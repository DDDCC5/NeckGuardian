"""Non-destructive Tripo foliage repair. Original trunk vertex/normal/UV buffers and atlas remain byte-identical."""
from pathlib import Path
import json,struct,math,random,hashlib,collections,subprocess
ROOT=Path(__file__).resolve().parents[2]
SOURCE=ROOT/'production/environments/v2/models/natural-tree-v3/tripo-out/neckguardian-natural-tree-v3-1e29b9f5/model.glb'
OUT=ROOT/'production/environments/v2/foliage-rebuild'
OUT.mkdir(exist_ok=True)
raw=SOURCE.read_bytes();jl=struct.unpack_from('<I',raw,12)[0];doc=json.loads(raw[20:20+jl]);original=raw[28+jl:28+jl+doc['buffers'][0]['byteLength']];data=bytearray(original)
# Decode the original embedded JPEG for classification. No raster artwork is modified.
analysis_dir=ROOT/'.scratch/foliage-rebuild';analysis_dir.mkdir(parents=True,exist_ok=True)
image_view=doc['bufferViews'][doc['images'][0]['bufferView']];image_start=image_view.get('byteOffset',0);atlas=original[image_start:image_start+image_view['byteLength']]
(analysis_dir/'atlas.jpg').write_bytes(atlas)
subprocess.run(['sips','-s','format','bmp',str(analysis_dir/'atlas.jpg'),'--out',str(analysis_dir/'atlas.bmp')],check=True,stdout=subprocess.DEVNULL)
bmp=(analysis_dir/'atlas.bmp').read_bytes();offset=struct.unpack_from('<I',bmp,10)[0];bw,bh=struct.unpack_from('<ii',bmp,18);stride=(bw*3+3)//4*4

def pixel(u,v):
 x=max(0,min(bw-1,round(u*(bw-1))));y=max(0,min(abs(bh)-1,round(v*(abs(bh)-1))))
 if bh>0:y=bh-1-y
 b,g,r=bmp[offset+y*stride+x*3:offset+y*stride+x*3+3];return r,g,b

def accessor(index):
 a=doc['accessors'][index];v=doc['bufferViews'][a['bufferView']];n={'SCALAR':1,'VEC2':2,'VEC3':3,'VEC4':4}[a['type']];fmt={5126:'f',5125:'I',5123:'H'}[a['componentType']];step=v.get('byteStride',struct.calcsize(fmt)*n);start=v.get('byteOffset',0)+a.get('byteOffset',0)
 return [struct.unpack_from('<'+fmt*n,original,start+i*step) for i in range(a['count'])]

prim=doc['meshes'][0]['primitives'][0];positions=accessor(prim['attributes']['POSITION']);uv=accessor(prim['attributes']['TEXCOORD_0']);indices=[i[0] for i in accessor(prim['indices'])];tris=[indices[i:i+3] for i in range(0,len(indices),3)];height=max(p[1] for p in positions)-min(p[1] for p in positions);bottom=min(p[1] for p in positions)
def green(c):
 r,g,b=c;return g>r*.93 and g>b*1.15

def center(t):return tuple(sum(positions[i][k] for i in t)/3 for k in range(3))
def area(t):
 a,b,c=[positions[i] for i in t];u=[b[k]-a[k] for k in range(3)];v=[c[k]-a[k] for k in range(3)];return math.sqrt(sum(x*x for x in [u[1]*v[2]-u[2]*v[1],u[2]*v[0]-u[0]*v[2],u[0]*v[1]-u[1]*v[0]]))*.5
mask=[]
for t in tris:
 colors=[pixel(*uv[i]) for i in t];m=tuple(sum(uv[i][k] for i in t)/3 for k in range(2));colors.append(pixel(*m));mask.append(center(t)[1]>bottom+height*.20 and sum(green(c) for c in colors)>=2)
# Weld only for analysis; retained source vertices/UV seams are never changed.
keys=[tuple(round(c,5) for c in p) for p in positions];incident=collections.defaultdict(list)
for ti,t in enumerate(tris):
 for i in t:incident[keys[i]].append(ti)
# Fill small baked-color holes within a leafy surface, protecting brown branch centers.
for _ in range(2):
 updated=mask[:]
 for ti,t in enumerate(tris):
  if mask[ti] or center(t)[1]<bottom+height*.2:continue
  neighbors=set(n for i in t for n in incident[keys[i]])-{ti}
  if neighbors and sum(mask[n] for n in neighbors)/len(neighbors)>.65:updated[ti]=True
 mask=updated
# Keep the connected trunk/branch body; remove disconnected remnants of baked leaf triangles.
seen=set();wood_components=[]
for ti in range(len(tris)):
 if mask[ti] or ti in seen:continue
 stack=[ti];seen.add(ti);component=[]
 while stack:
  n=stack.pop();component.append(n)
  for v in tris[n]:
   for other in incident[keys[v]]:
    if not mask[other] and other not in seen:seen.add(other);stack.append(other)
 wood_components.append(component)
wood_components.sort(key=len,reverse=True)
extra_wood=[]
for component in wood_components[1:]:
 pts=[positions[i] for ti in component for i in tris[ti]]
 mean=[sum(p[k] for p in pts)/len(pts) for k in range(3)]
 cov=[[sum((p[i]-mean[i])*(p[j]-mean[j]) for p in pts)/len(pts) for j in range(3)] for i in range(3)]
 axis=[1.,.3,.2]
 for _ in range(12):
  vec=[sum(cov[i][j]*axis[j] for j in range(3)) for i in range(3)];norm=math.sqrt(sum(x*x for x in vec))
  if norm>1e-12:axis=[x/norm for x in vec]
 trace=sum(cov[i][i] for i in range(3));elongation=sum(axis[i]*cov[i][j]*axis[j] for i in range(3) for j in range(3))/max(trace,1e-12)
 colors=[pixel(*uv[i]) for ti in component for i in tris[ti]];ratio=sum(c[1] for c in colors)/max(1,sum(c[0] for c in colors))
 if len(component)>=25 and ratio<.91 and elongation>.82:extra_wood.append(component)
 else:
  for ti in component:mask[ti]=True
visited=set();clusters=[]
for ti in range(len(tris)):
 if not mask[ti] or ti in visited:continue
 stack=[ti];visited.add(ti);component=[]
 while stack:
  n=stack.pop();component.append(n)
  for v in tris[n]:
   for other in incident[keys[v]]:
    if mask[other] and other not in visited:visited.add(other);stack.append(other)
 clusters.append(component)
clusters.sort(key=len,reverse=True)
retained=[i for ti,t in enumerate(tris) if not mask[ti] for i in t]
def append_accessor(values,n,component=5126,kind=None):
 while len(data)%4:data.append(0)
 offset=len(data);fmt={5126:'f',5125:'I'}[component];flat=[c for v in values for c in (v if isinstance(v,(tuple,list)) else [v])];payload=struct.pack('<'+fmt*len(flat),*flat);data.extend(payload);doc['bufferViews'].append({'buffer':0,'byteOffset':offset,'byteLength':len(payload)})
 a={'bufferView':len(doc['bufferViews'])-1,'componentType':component,'count':len(values),'type':kind or {1:'SCALAR',2:'VEC2',3:'VEC3',4:'VEC4'}[n]}
 if n==3 and component==5126:a.update(min=[min(v[k] for v in values) for k in range(3)],max=[max(v[k] for v in values) for k in range(3)])
 doc['accessors'].append(a);return len(doc['accessors'])-1

def save(name):
 doc['buffers'][0]['byteLength']=len(data);j=json.dumps(doc,separators=(',',':')).encode();j+=b' '*((-len(j))%4);binary=bytes(data);binary+=b'\0'*((-len(binary))%4);out=struct.pack('<III',0x46546c67,2,28+len(j)+len(binary))+struct.pack('<II',len(j),0x4e4f534a)+j+struct.pack('<II',len(binary),0x004e4942)+binary;(OUT/name).write_bytes(out)
prim['indices']=append_accessor(retained,1,5125);doc['nodes'][0]['name']='RetainedTripoBranches';save('trunk-only.glb')
report={'sourceSha256':hashlib.sha256(raw).hexdigest(),'originalTriangles':len(tris),'removedFoliageTriangles':sum(mask),'retainedTrunkTriangles':len(retained)//3,'clusterCount':len(clusters),'largestClusters':[len(c) for c in clusters[:30]],'heightSourceUnits':height,'originalBufferPrefixUnchanged':bytes(data[:len(original)])==original,'trunkVertexNormalUvAccessorsUnchanged':True}
(OUT/'segmentation.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report,indent=2))
# Detailed samples are temporary analysis input, not app assets.
(ROOT/'.scratch/foliage-rebuild/clusters.json').write_text(json.dumps([{'triangles':c,'centers':[center(tris[t]) for t in c],'areas':[area(tris[t]) for t in c]} for c in clusters]))

# Split large connected canopy patches by measured 3D cells; discard tiny disconnected AI debris.
cell=height*.075;groups=[]
for comp in clusters:
 if len(comp)<8 or sum(area(tris[t]) for t in comp)<height*height*.00010:continue
 buckets=collections.defaultdict(list)
 for ti in comp:
  c=center(tris[ti]);buckets[tuple(math.floor(x/cell) for x in c)].append(ti)
 if len(comp)<380:groups.append(comp)
 else:groups.extend(b for b in buckets.values() if len(b)>=8)

rng=random.Random(20261127)
def add(a,b):return tuple(a[i]+b[i] for i in range(3))
def sub(a,b):return tuple(a[i]-b[i] for i in range(3))
def mul(a,s):return tuple(x*s for x in a)
def dot(a,b):return sum(a[i]*b[i] for i in range(3))
def cross(a,b):return (a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0])
def length(a):return math.sqrt(dot(a,a))
def unit(a):
 n=length(a);return mul(a,1/n) if n>1e-10 else (0.,1.,0.)
def basis(d):
 w=unit(cross(d,(0.,1.,0.) if abs(d[1])<.95 else (1.,0.,0.)));return w,unit(cross(w,d))
wood={'p':[],'n':[],'uv':[],'i':[]};leaves={'p':[],'n':[],'uv':[],'i':[],'color':[]}
def tube(a,b,radius,sides=5):
 d=unit(sub(b,a));w,n=basis(d);start=len(wood['p'])
 for j,p in enumerate([a,b]):
  for i in range(sides):
   radial=add(mul(w,math.cos(i*math.tau/sides)),mul(n,math.sin(i*math.tau/sides)));wood['p'].append(add(p,mul(radial,radius*(1 if j==0 else .45))));wood['n'].append(radial);wood['uv'].append((i/sides,j))
 for i in range(sides):
  k=(i+1)%sides;wood['i'].extend([start+i,start+sides+i,start+k,start+k,start+sides+i,start+sides+k])

def leaf(base,d,L,roll):
 d=unit(d);w,n=basis(d);w=add(mul(w,math.cos(roll)),mul(n,math.sin(roll)));n=unit(cross(w,d));start=len(leaves['p']);tint=rng.uniform(.82,1.)
 for row in range(3):
  t=row/2
  for col in range(3):
   x=col-1
   # Physically zero-thickness curved surface. Alpha texture provides the natural elliptical silhouette.
   bend=(.045*math.sin(math.pi*t)-.028*x*x*math.sin(math.pi*t))*L
   p=add(add(add(base,mul(d,L*t)),mul(w,x*L/3)),mul(n,bend));leaves['p'].append(p);leaves['n'].append(n);leaves['uv'].append((col/2,1-t));leaves['color'].append((tint,rng.uniform(.94,1.)*tint,.90*tint))
 for row in range(2):
  for col in range(2):
   a=start+row*3+col;b=a+1;c=a+3;e=c+1;leaves['i'].extend([a,b,c,b,e,c])


brown_indices=sorted(set(retained));brown=[positions[i] for i in brown_indices]
def kd_build(points,depth=0):
 if not points:return None
 axis=depth%3;points=sorted(points,key=lambda p:p[axis]);mid=len(points)//2;return (points[mid],axis,kd_build(points[:mid],depth+1),kd_build(points[mid+1:],depth+1))
def nearest(tree,p):
 best=[float('inf'),None]
 def visit(node):
  if node is None:return
  q,axis,left,right=node;distance=sum((p[k]-q[k])**2 for k in range(3))
  if distance<best[0]:best[:]=[distance,q]
  delta=p[axis]-q[axis];near,far=(left,right) if delta<0 else (right,left);visit(near)
  if delta*delta<best[0]:visit(far)
 visit(tree);return best
brown_tree=kd_build(list(set(brown)))
main_points=list(set(positions[i] for ti in wood_components[0] for i in tris[ti]));main_tree=kd_build(main_points);branch_repairs=[]
for component in extra_wood:
 candidate_points=set(positions[i] for ti in component for i in tris[ti]);distance,start,end=min((nearest(main_tree,p)[0],nearest(main_tree,p)[1],p) for p in candidate_points)
 tube(start,end,height*.0016);branch_repairs.append(math.sqrt(distance))
max_join=0.;leaf_count=0;shoot_count=0
for group in groups:
 weights=[area(tris[t]) for t in group];total=sum(weights)
 if total<=0:continue
 c=tuple(sum(center(tris[ti])[k]*a for ti,a in zip(group,weights))/total for k in range(3))
 if c[1]<bottom+height*.20:continue
 anchor=nearest(brown_tree,c)[1];gap=length(sub(c,anchor))
 if gap>height*.12:continue
 max_join=max(max_join,gap)
 axis=unit(sub(c,anchor));radial=unit((c[0],rng.uniform(-.12,.25),c[2]));w,n=basis(axis)
 # Main support starts at an actual retained branch vertex; leaflets grow from this connected shoot.
 end=add(c,mul(axis,height*.018));tube(anchor,end,height*.0011);shoot_count+=1
 number=16
 for i in range(number):
  t=.35+.62*i/(number-1);stem=add(anchor,mul(sub(end,anchor),t));side=-1 if i%2 else 1
  spread=unit(add(add(mul(w,side),mul(n,rng.uniform(-.75,.75))),mul(radial,.45)))
  petiole=height*rng.uniform(.010,.023);base=add(stem,mul(spread,petiole))
  d=unit(add(mul(spread,.8),add(mul(axis,.35), (0.,rng.uniform(-.45,.35),0.))))
  L=height*rng.uniform(.045,.067);tube(stem,add(base,mul(d,L*.05)),height*.0004,sides=3);leaf(base,d,L,rng.uniform(-.95,.95));leaf_count+=1

# Add all new content as separate objects; never rewrite the retained bark attributes or texture.
leaf_image=(OUT/'leaf-texture.png').read_bytes()
while len(data)%4:data.append(0)
image_offset=len(data);data.extend(leaf_image);doc['bufferViews'].append({'buffer':0,'byteOffset':image_offset,'byteLength':len(leaf_image)});doc.setdefault('images',[]).append({'name':'NaturalThinLeafTexture','mimeType':'image/png','bufferView':len(doc['bufferViews'])-1});doc.setdefault('samplers',[]).append({'magFilter':9729,'minFilter':9987,'wrapS':33071,'wrapT':33071});doc.setdefault('textures',[]).append({'source':len(doc['images'])-1,'sampler':len(doc['samplers'])-1})
leaf_material=len(doc['materials']);doc['materials'].append({'name':'NaturalThinLeaves','pbrMetallicRoughness':{'baseColorFactor':[1,1,1,1],'baseColorTexture':{'index':len(doc['textures'])-1},'metallicFactor':0,'roughnessFactor':.86},'alphaMode':'MASK','alphaCutoff':.7,'doubleSided':True})
wood_material=len(doc['materials']);doc['materials'].append({'name':'ConnectedLeafTwigs','pbrMetallicRoughness':{'baseColorFactor':[.085,.052,.021,1],'metallicFactor':0,'roughnessFactor':.95}})
for name,geom,material in [('ConnectedLeafTwigs',wood,wood_material),('NaturalThinLeaves',leaves,leaf_material)]:
 attrs={'POSITION':append_accessor(geom['p'],3),'NORMAL':append_accessor(geom['n'],3),'TEXCOORD_0':append_accessor(geom['uv'],2)}
 if 'color' in geom:attrs['COLOR_0']=append_accessor(geom['color'],3)
 primitive={'attributes':attrs,'indices':append_accessor(geom['i'],1,5125),'material':material};doc['meshes'].append({'name':name,'primitives':[primitive]});doc['nodes'].append({'name':name,'mesh':len(doc['meshes'])-1});doc['scenes'][0]['nodes'].append(len(doc['nodes'])-1)
save('natural-tree-thin-foliage-v4.glb')
report.update({'sprayCount':shoot_count,'leafCount':leaf_count,'leafTriangles':len(leaves['i'])//3,'newTwigTriangles':len(wood['i'])//3,'totalTriangles':len(retained)//3+(len(leaves['i'])+len(wood['i']))//3,'maximumSupportLengthSourceUnits':max_join,'supportRootGap':0,'originalBufferPrefixUnchanged':bytes(data[:len(original)])==original,'leafGeometryThickness':0,'leafAlphaCutoff':.7,'newPaidTripoTasks':0,'retainedBranchRepairs':branch_repairs})
(OUT/'rebuild-report.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report,indent=2))
