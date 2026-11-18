import fs from 'node:fs/promises';
import {Resvg} from '@resvg/resvg-js';
const root=new URL('../',import.meta.url);
function size(b){let p=2;while(p<b.length){if(b[p]!==255){p++;continue;}const m=b[p+1],n=b.readUInt16BE(p+2);if([192,193,194].includes(m))return{h:b.readUInt16BE(p+5),w:b.readUInt16BE(p+7)};p+=n+2;}throw Error('JPEG');}
for(const [id,y] of [['posture_tower',294],['paper_plane',294],['star',294],['angle_meter',282],['rudder_gauge',214],['block_sage',150]]){const b=await fs.readFile(new URL('evidence/model-'+id+'.jpg',root)),d=size(b),svg=`<svg xmlns="http://www.w3.org/2000/svg" width="655" height="440" viewBox="0 0 655 440"><image x="-34" y="-${y}" width="${d.w}" height="${d.h}" href="data:image/jpeg;base64,${b.toString('base64')}"/></svg>`;await fs.writeFile(new URL('evidence/thumb-'+id+'.png',root),new Resvg(svg).render().asPng());}
let page=await fs.readFile(new URL('preview/overview.html',root),'utf8');page=page.replace(/thumb-([a-z_]+)\.jpg/g,'thumb-$1.png');await fs.writeFile(new URL('preview/overview.html',root),page);console.log('Created six verified original-model screenshot crops.');
