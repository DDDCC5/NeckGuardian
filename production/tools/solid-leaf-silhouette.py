"""Convert the existing leaf card grids into alpha-silhouette geometry; preserve source files and PNG bytes.
No raster editing or generation: alpha values are read solely to derive mesh UV contour positions.
"""
import json, struct, zlib, math, hashlib
from pathlib import Path
png=Path('production/environments/v2/foliage-rebuild/leaf-texture.png').read_bytes()
i=8;data=b''
while i<len(png):
 n=struct.unpack_from('>I',png,i)[0];kind=png[i+4:i+8];chunk=png[i+8:i+8+n]
 if kind==b'IHDR':W,H,depth,color,*_=struct.unpack('>IIBBBBB',chunk)
 if kind==b'IDAT':data+=chunk
 i+=n+12
assert depth==8 and color==6,(depth,color)
raw=zlib.decompress(data);rows=[];stride=W*4;offset=0
for y in range(H):
 f=raw[offset];offset+=1;row=bytearray(raw[offset:offset+stride]);offset+=stride;previous=rows[-1] if rows else bytearray(stride)
 for x in range(stride):
  a=row[x-4] if x>=4 else 0;b=previous[x];c=previous[x-4] if x>=4 else 0
  if f==1:v=a
  elif f==2:v=b
  elif f==3:v=(a+b)//2
  elif f==4:
   p=a+b-c;da,db,dc=abs(p-a),abs(p-b),abs(p-c);v=a if da<=db and da<=dc else b if db<=dc else c
  else:v=0
  row[x]=(row[x]+v)&255
 rows.append(row)
def contour(y):
 a=[x for x in range(W) if rows[y][x*4+3]>=210]
 return (min(a)+1,max(a)-1) if len(a)>4 else None
occupied=[y for y in range(H) if contour(y)]
y0,y1=min(occupied),max(occupied)
# Base-to-tip rows include the stem and natural tapered tip. Insets avoid sampling transparent halo.
samples=[]
for k in range(7):
 y=round(y1-(y1-y0)*k/6);left,right=contour(y);samples.append((1-y/(H-1),left/(W-1),right/(W-1),y/(H-1)))
print('alpha contour',W,H,y0,y1,'opaque fraction',sum(sum(r[x*4+3]>=210 for x in range(W)) for r in rows)/(W*H))
def convert(source,target):
 b=source.read_bytes();n=struct.unpack_from('<I',b,12)[0];d=json.loads(b[20:20+n]);binary=bytearray(b[28+n:]);replaced=0;leaf_count=0
 def read(index):
  a=d['accessors'][index];v=d['bufferViews'][a['bufferView']];components={'SCALAR':1,'VEC2':2,'VEC3':3,'VEC4':4}[a['type']];code,size={5126:('f',4),5125:('I',4),5123:('H',2)}[a['componentType']];base=v.get('byteOffset',0)+a.get('byteOffset',0);step=v.get('byteStride',components*size)
  return [struct.unpack_from('<'+code*components,binary,base+j*step) for j in range(a['count'])]
 def append(values,components,index=False):
  while len(binary)%4:binary.append(0)
  offset=len(binary);flat=[x for row in values for x in row];binary.extend(struct.pack('<'+('I' if index else 'f')*len(flat),*flat));view=len(d['bufferViews']);d['bufferViews'].append({'buffer':0,'byteOffset':offset,'byteLength':len(binary)-offset,'target':34963 if index else 34962});a={'bufferView':view,'componentType':5125 if index else 5126,'count':len(values),'type':{1:'SCALAR',2:'VEC2',3:'VEC3'}[components]}
  if components==3 and not index:a.update(min=[min(v[k] for v in values) for k in range(3)],max=[max(v[k] for v in values) for k in range(3)])
  d['accessors'].append(a);return len(d['accessors'])-1
 for mesh in d['meshes']:
  for prim in mesh['primitives']:
   mat=d['materials'][prim['material']]
   if mat.get('name')!='NaturalThinLeaves':continue
   old=read(prim['attributes']['POSITION']);normals=read(prim['attributes']['NORMAL']);colors=read(prim['attributes']['COLOR_0']) if 'COLOR_0' in prim['attributes'] else [(1,1,1)]*len(old);old_indices=read(prim['indices']);assert len(old)%9==0, 'Run the scene builder first; input must have original 9-vertex cards';used=sorted(set(v[0]//9 for v in old_indices));pos=[];nor=[];uv=[];col=[];ind=[]
   for leaf in used:
    base=leaf*9;start=len(pos)
    def interpolate(values,u,t):
     x=min(u*2,1.999999);y=min(t*2,1.999999);c=int(x);r=int(y);fx=x-c;fy=y-r
     return tuple((1-fy)*((1-fx)*values[base+r*3+c][k]+fx*values[base+r*3+c+1][k])+fy*((1-fx)*values[base+(r+1)*3+c][k]+fx*values[base+(r+1)*3+c+1][k]) for k in range(3))
    for t,left,right,v in samples:
     for u in [left,right]:
      pos.append(interpolate(old,u,t));normal=interpolate(normals,u,t);length=math.sqrt(sum(x*x for x in normal));nor.append(tuple(x/length for x in normal));uv.append((u,v));col.append(interpolate(colors,u,t))
    for r in range(6):
     a=start+r*2;ind.extend([(a,),(a+1,),(a+2,),(a+1,),(a+3,),(a+2,)])
   prim['attributes']={'POSITION':append(pos,3),'NORMAL':append(nor,3),'TEXCOORD_0':append(uv,2),'COLOR_0':append(col,3)};prim['indices']=append(ind,1,True);mat['alphaMode']='OPAQUE';mat.pop('alphaCutoff',None);mat['extras']={'leafSilhouette':'Geometry traced from unchanged original PNG alpha>=210; no cutout/mipmap dependency'};replaced+=1;leaf_count+=len(used)
 d['buffers'][0]['byteLength']=len(binary);d.setdefault('extras',{})['solidLeafGeometry']={'sourceSha256':hashlib.sha256(b).hexdigest(),'leafGridPrimitives':replaced,'uniqueMeshLeaves':leaf_count,'contourRows':7,'alphaCutoutRequired':False,'rasterBytesChanged':False}
 j=json.dumps(d,separators=(',',':')).encode();j+=b' '*((-len(j))%4);binary+=b'\0'*((-len(binary))%4);target.write_bytes(struct.pack('<III',0x46546c67,2,28+len(j)+len(binary))+struct.pack('<II',len(j),0x4e4f534a)+j+struct.pack('<II',len(binary),0x004e4942)+binary);print(target,replaced,leaf_count)
for theme in ['lake','forest','study']:
 p=Path('production/environments/v6')/(theme+'-v6.glb');convert(p,p)
