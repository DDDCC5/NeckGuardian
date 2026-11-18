from pathlib import Path
import struct,zlib,json
R=Path(__file__).resolve().parents[1];items=[]
for folder in ['ui','icon']:
 for p in sorted((R/folder).rglob('*.png')):
  b=p.read_bytes();assert b[:8]==b'\x89PNG\r\n\x1a\n',p
  off=8;compressed=b'';w=h=ctype=depth=0
  while off<len(b):
   n=struct.unpack_from('>I',b,off)[0];kind=b[off+4:off+8];data=b[off+8:off+8+n];crc=struct.unpack_from('>I',b,off+8+n)[0];assert zlib.crc32(kind+data)&0xffffffff==crc,p
   if kind==b'IHDR':w,h,depth,ctype=struct.unpack_from('>IIBB',data)
   if kind==b'IDAT':compressed+=data
   off+=12+n
  alpha=[]
  if ctype==6 and depth==8 and folder=='icon':
   raw=zlib.decompress(compressed);stride=w*4;rows=[];prev=bytearray(stride);pos=0
   for y in range(h):
    f=raw[pos];pos+=1;cur=bytearray(raw[pos:pos+stride]);pos+=stride
    for x in range(stride):
     a=cur[x-4] if x>=4 else 0;bb=prev[x];c=prev[x-4] if x>=4 else 0
     if f==1:v=a
     elif f==2:v=bb
     elif f==3:v=(a+bb)//2
     elif f==4:
      pp=a+bb-c;pa=abs(pp-a);pb=abs(pp-bb);pc=abs(pp-c);v=a if pa<=pb and pa<=pc else bb if pb<=pc else c
     else:v=0
     cur[x]=(cur[x]+v)&255
    if y in [0,h-1]:alpha.extend([cur[3],cur[-1]])
    prev=cur
  items.append({'file':str(p.relative_to(R)),'width':w,'height':h,'bitDepth':depth,'colorType':ctype,'crcValid':True,'cornerAlpha':alpha})
for x in items:
 if x['file'].endswith('adaptive_foreground_432.png'):assert all(a==0 for a in x['cornerAlpha'])
 if '/screens/' in x['file']:assert (x['width'],x['height'])==(800,600)
(R/'evidence/image-validation.json').write_text(json.dumps({'files':len(items),'errors':0,'items':items},indent=2));print('Validated',len(items),'PNG files, zero CRC/dimension errors')
