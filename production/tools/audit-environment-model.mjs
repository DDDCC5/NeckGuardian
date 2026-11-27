import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import validator from 'gltf-validator';
const [source,output]=process.argv.slice(2);
if(!source||!output)throw Error('Usage: node audit-environment-model.mjs model.glb report.json');
const bytes=fs.readFileSync(source);
if(bytes.readUInt32LE(0)!==0x46546c67||bytes.readUInt32LE(4)!==2)throw Error('Not a GLB2 asset');
const doc=JSON.parse(bytes.subarray(20,20+bytes.readUInt32LE(12)).toString('utf8'));
let bin;
for(let offset=12;offset<bytes.length;){const length=bytes.readUInt32LE(offset);if(bytes.readUInt32LE(offset+4)===0x004e4942)bin=bytes.subarray(offset+8,offset+8+length);offset+=8+length;}
const report=await validator.validateBytes(new Uint8Array(bytes),{uri:path.basename(source),maxIssues:100});
let triangles=0,vertices=0;const modes=[];
for(const mesh of doc.meshes??[])for(const primitive of mesh.primitives){const mode=primitive.mode??4;const count=doc.accessors[primitive.indices??primitive.attributes.POSITION].count;vertices+=doc.accessors[primitive.attributes.POSITION].count;if(mode===4)triangles+=count/3;else if(mode===5||mode===6)triangles+=Math.max(0,count-2);modes.push(mode);}
function imageSize(b){
 if(b.length>24&&b.subarray(0,8).equals(Buffer.from([137,80,78,71,13,10,26,10])))return {width:b.readUInt32BE(16),height:b.readUInt32BE(20),encoding:'PNG'};
 if(b[0]===255&&b[1]===216){let i=2;while(i+8<b.length){if(b[i]!==255){i++;continue}const marker=b[i+1];if(marker===216||marker===217){i+=2;continue}const n=b.readUInt16BE(i+2);if([192,193,194].includes(marker))return {width:b.readUInt16BE(i+7),height:b.readUInt16BE(i+5),encoding:'JPEG'};if(n<2)break;i+=2+n;}}
 return {encoding:'unparsed'};
}
const images=(doc.images??[]).map((image,index)=>{const view=doc.bufferViews?.[image.bufferView];const data=view&&bin?bin.subarray(view.byteOffset??0,(view.byteOffset??0)+view.byteLength):null;return {index,mimeType:image.mimeType,embedded:!!data,externalUri:!!image.uri,...(data?imageSize(data):{})};});
const stats={source:path.resolve(source),sha256:crypto.createHash('sha256').update(bytes).digest('hex'),bytes:bytes.length,meshCount:doc.meshes?.length??0,primitiveCount:modes.length,triangleCount:triangles,vertices,materialCount:doc.materials?.length??0,images,externalResources:[...(doc.buffers??[]),...(doc.images??[])].filter(v=>v.uri&&!v.uri.startsWith('data:')).map(v=>v.uri),materials:(doc.materials??[]).map(m=>({name:m.name,alphaMode:m.alphaMode??'OPAQUE',doubleSided:m.doubleSided??false,hasBaseColorTexture:!!m.pbrMetallicRoughness?.baseColorTexture,hasNormalTexture:!!m.normalTexture})),validator:report.issues,limitations:['Triangle count sums unique mesh primitives, not all scene instances.','Asset validation does not establish headset sharpness, performance, scale, or placement acceptance.']};
fs.writeFileSync(output,JSON.stringify(stats,null,2)+'\n');console.log(JSON.stringify({...stats,materials:undefined,validator:{errors:report.issues.numErrors,warnings:report.issues.numWarnings}},null,2));
