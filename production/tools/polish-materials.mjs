// Decorative material variants: original geometry, hierarchy and animation buffers are retained.
import fs from 'node:fs';
import path from 'node:path';
import {Resvg} from '@resvg/resvg-js';
const root=path.resolve(import.meta.dirname,'..');
const out=path.join(root,'models/polished');fs.mkdirSync(out,{recursive:true});
const grain=Array.from({length:72},(_,i)=>{const y=i*3.7;const a=Math.sin(i*1.3)*3;return `<path d="M-10 ${y} Q60 ${y+a-4} 126 ${y+a} T270 ${y-1}" fill="none" stroke="${i%3?'#ffffff':'#655c42'}" stroke-opacity="${i%3?'.10':'.16'}" stroke-width="${i%4===0?'.7':'.35'}"/>`;}).join('');
const svg=`<svg xmlns="http://www.w3.org/2000/svg" width="256" height="256"><rect width="256" height="256" fill="#f3ecdc"/>${grain}</svg>`;
fs.writeFileSync(path.join(out,'wood-grain.svg'),svg);
const png=new Resvg(svg).render().asPng();fs.writeFileSync(path.join(out,'wood-grain.png'),png);
const align=b=>Buffer.concat([b,Buffer.alloc((4-b.length%4)%4)]);
function variant(name){
 const original=fs.readFileSync(path.join(root,`models/${name}.glb`));const jlen=original.readUInt32LE(12);const doc=JSON.parse(original.subarray(20,20+jlen));const bin=original.subarray(28+jlen,28+jlen+original.readUInt32LE(20+jlen));
 const originalAnimation=JSON.stringify(doc.animations);let parts=[align(bin)],length=parts[0].length;
 function append(bytes){const offset=length;const a=align(bytes);parts.push(a);length+=a.length;doc.bufferViews??=[];doc.bufferViews.push({buffer:0,byteOffset:offset,byteLength:bytes.length});return doc.bufferViews.length-1;}
 const textureView=append(png);doc.images??=[];doc.images.push({bufferView:textureView,mimeType:'image/png'});doc.samplers??=[];doc.samplers.push({magFilter:9729,minFilter:9987,wrapS:10497,wrapT:10497});doc.textures??=[];doc.textures.push({sampler:doc.samplers.length-1,source:doc.images.length-1});const texture=doc.textures.length-1;
 for(const mesh of doc.meshes)for(const p of mesh.primitives){
  if(p.attributes.TEXCOORD_0==null){const a=doc.accessors[p.attributes.POSITION];const v=doc.bufferViews[a.bufferView];const uv=Buffer.alloc(a.count*8);for(let i=0;i<a.count;i++){const start=(v.byteOffset??0)+(a.byteOffset??0)+i*(v.byteStride??12);uv.writeFloatLE(bin.readFloatLE(start)*4+.5,i*8);uv.writeFloatLE(bin.readFloatLE(start+8)*4+.5,i*8+4);}const view=append(uv);doc.accessors.push({bufferView:view,componentType:5126,count:a.count,type:'VEC2'});p.attributes.TEXCOORD_0=doc.accessors.length-1;}
 }
 for(const m of doc.materials){m.pbrMetallicRoughness??={};m.pbrMetallicRoughness.baseColorTexture={index:texture};m.pbrMetallicRoughness.roughnessFactor=name==='paper_plane'?.78:.68;}
 const joined=Buffer.concat(parts);doc.buffers[0].byteLength=joined.length;
 let json=Buffer.from(JSON.stringify(doc));json=Buffer.concat([json,Buffer.alloc((4-json.length%4)%4,32)]);
 const header=Buffer.alloc(20);header.writeUInt32LE(0x46546c67,0);header.writeUInt32LE(2,4);header.writeUInt32LE(28+json.length+joined.length,8);header.writeUInt32LE(json.length,12);header.writeUInt32LE(0x4e4f534a,16);const bh=Buffer.alloc(8);bh.writeUInt32LE(joined.length,0);bh.writeUInt32LE(0x004e4942,4);
 if(JSON.stringify(doc.animations)!==originalAnimation||!joined.subarray(0,bin.length).equals(bin))throw Error('Original animation/geometry changed');
 const bytes=Buffer.concat([header,json,bh,joined]);fs.writeFileSync(path.join(out,`${name}.glb`),bytes);fs.mkdirSync(path.resolve(root,'../app/src/main/assets/models/polished'),{recursive:true});fs.writeFileSync(path.resolve(root,`../app/src/main/assets/models/polished/${name}.glb`),bytes);
 console.log(name,bytes.length,'bytes; original binary and animation tracks retained');
}
variant('posture_tower');variant('paper_plane');
