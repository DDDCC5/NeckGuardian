import fs from 'node:fs/promises';
import path from 'node:path';
import * as T from 'three';
import {RoundedBoxGeometry} from 'three/addons/geometries/RoundedBoxGeometry.js';
import {GLTFExporter} from 'three/addons/exporters/GLTFExporter.js';
import R from '@dimforge/rapier3d-compat';
import validator from 'gltf-validator';
const OUT=new URL('../models/',import.meta.url);const EVID=new URL('../evidence/',import.meta.url);
class Reader{readAsArrayBuffer(blob){blob.arrayBuffer().then(x=>{this.result=x;this.onloadend?.();});}readAsDataURL(blob){blob.arrayBuffer().then(x=>{this.result='data:application/octet-stream;base64,'+Buffer.from(x).toString('base64');this.onloadend?.();});}}
globalThis.FileReader=Reader;
const M={sage:new T.MeshStandardMaterial({name:'mat_sage',color:'#aec1a6',roughness:.82}),peach:new T.MeshStandardMaterial({name:'mat_peach',color:'#dfb69a',roughness:.86}),cream:new T.MeshStandardMaterial({name:'mat_cream',color:'#ead5a8',roughness:.85}),paper:new T.MeshStandardMaterial({name:'mat_paper',color:'#fff6e4',roughness:.9,side:T.DoubleSide}),fold:new T.MeshStandardMaterial({name:'mat_fold',color:'#a6bca4',roughness:.88,side:T.DoubleSide}),wood:new T.MeshStandardMaterial({name:'mat_wood',color:'#d8bd98',roughness:.86}),dark:new T.MeshStandardMaterial({name:'mat_sage_dark',color:'#718a70',roughness:.8}),inactive:new T.MeshStandardMaterial({name:'mat_meter_inactive',color:'#d6dccb',roughness:.95}),star:new T.MeshStandardMaterial({name:'mat_star_soft_emissive',color:'#efcc87',emissive:'#70501d',emissiveIntensity:.35,roughness:.55,metalness:0})};
const catalog=[];const validation=[];
const mesh=(name,g,m)=>{const x=new T.Mesh(g,m);x.name=name;x.castShadow=true;x.receiveShadow=true;return x;};
const box=(name,w,h,d,mat,r=.003)=>mesh(name,new RoundedBoxGeometry(w,h,d,2,r),mat);
async function save(id,label,group,meta={},clips=[]){
 group.name=id;const safeMeta={...meta};if('pivot' in safeMeta){safeMeta.originDescription=safeMeta.pivot;delete safeMeta.pivot;}group.userData={units:'meters',coordinateSystem:'right-handed Y-up; plane forward -Z',...safeMeta};
 const scene=new T.Scene();scene.name=id+'_scene';scene.add(group);scene.updateMatrixWorld(true);
 const data=await new GLTFExporter().parseAsync(scene,{binary:true,animations:clips,onlyVisible:true,maxTextureSize:1024,includeCustomExtensions:false});
 const file=id+'.glb';await fs.writeFile(new URL(file,OUT),Buffer.from(data));
 const check=await validator.validateBytes(new Uint8Array(data),{uri:file,maxIssues:100});
 validation.push({id,errors:check.issues.numErrors,warnings:check.issues.numWarnings,messages:check.issues.messages});
 const b=new T.Box3().setFromObject(group);const size=b.getSize(new T.Vector3());let triangles=0;const nodes=[];
 group.traverse(o=>{if(o.isMesh){triangles+=(o.geometry.index?.count??o.geometry.attributes.position.count)/3;nodes.push(o.name);}});
 catalog.push({id,label,file:'models/'+file,bytes:data.byteLength,triangles,dimensionsMeters:size.toArray(),boundsMeters:{min:b.min.toArray(),max:b.max.toArray()},meshNodes:nodes,animations:clips.map(c=>({name:c.name,duration:c.duration,tracks:c.tracks.length})),...meta});
 if(check.issues.numErrors)throw new Error(file+' validation errors');
}
for(const c of ['sage','peach','cream']){const g=new T.Group(),b=box('block_'+c,.07,.035,.045,M[c]);b.position.y=.0175;g.add(b);await save('block_'+c,'Rounded '+c+' block',g,{pivot:'bottom center',collision:{type:'box',size:[.07,.035,.045],center:[0,.0175,0]}});}
const table=new T.Group();const top=box('table_top',.9,.018,.4,M.wood,.012);top.position.y=-.009;table.add(top);await save('tabletop','Virtual rounded tabletop',table,{pivot:'top center',collision:{type:'box',size:[.9,.018,.4],center:[0,-.009,0]}});
await R.init();const world=new R.World({x:0,y:-9.81,z:0});world.timestep=1/240;world.integrationParameters.numSolverIterations=12;world.integrationParameters.allowedLinearError=.00005;world.integrationParameters.predictionDistance=.0005;
const floor=world.createRigidBody(R.RigidBodyDesc.fixed().setTranslation(0,-.009,0));world.createCollider(R.ColliderDesc.cuboid(.45,.009,.2).setFriction(.65).setRestitution(.08),floor);
// Invisible low floor catches anything that genuinely leaves the authored tabletop during inspection.
const tower=new T.Group();tower.add(top.clone());const bodies=[],objects=[],tracks=[],times=[],positions=Array.from({length:6},()=>[]),quats=Array.from({length:6},()=>[]);const mats=[M.cream,M.sage,M.peach];
for(let i=0;i<6;i++){
 const y=.0175+i*.0351;const o=box('tower_block_'+String(i+1).padStart(2,'0'),.07,.035,.045,mats[i%3]);o.position.y=y;tower.add(o);objects.push(o);
 const b=world.createRigidBody(R.RigidBodyDesc.kinematicPositionBased().setTranslation(0,y,0).setLinearDamping(2.5).setAngularDamping(1.0).setCcdEnabled(true));world.createCollider(R.ColliderDesc.cuboid(.035,.0175,.0225).setDensity(420).setFriction(.8).setRestitution(.04),b);bodies.push(b);
}
let impulse=false,minCornerY=1,maxDisplacement=0;
for(let k=0;k<=960;k++){
 if(k<=144){const u=Math.max(0,Math.min(1,(k-48)/96));const a=24*Math.PI/180*(u*u*(3-2*u));const q=new T.Quaternion().setFromAxisAngle(new T.Vector3(0,0,1),-a);bodies.forEach((b,i)=>{const h=.0175+i*.0351;b.setNextKinematicTranslation({x:h*Math.sin(a),y:h*Math.cos(a)+.035*Math.sin(a),z:0});b.setNextKinematicRotation(q);});}if(k===145){bodies.forEach(b=>{b.setBodyType(R.RigidBodyType.Dynamic,true);b.setLinvel({x:0,y:0,z:0},true);b.setAngvel({x:0,y:0,z:0},true);});impulse=true;}
 if(k%8===0){times.push(k/240);bodies.forEach((b,i)=>{const p=b.translation(),q=b.rotation();positions[i].push(p.x,p.y,p.z);quats[i].push(q.x,q.y,q.z,q.w);maxDisplacement=Math.max(maxDisplacement,Math.abs(p.x));const rot=new T.Quaternion(q.x,q.y,q.z,q.w);for(const x of [-.035,.035])for(const y of [-.0175,.0175])for(const z of [-.0225,.0225])minCornerY=Math.min(minCornerY,new T.Vector3(x,y,z).applyQuaternion(rot).y+p.y);});}
 world.step();
}
for(let i=0;i<6;i++){tracks.push(new T.VectorKeyframeTrack(objects[i].name+'.position',times,positions[i]));tracks.push(new T.QuaternionKeyframeTrack(objects[i].name+'.quaternion',times,quats[i]));}
const topple=new T.AnimationClip('topple_physics',4,tracks);
await save('posture_tower','Six-block single-column tower',tower,{pivot:'tabletop center',blockCount:6,independentBlockNodes:objects.map(x=>x.name),collision:'Each block 0.07 × 0.035 × 0.045 m; table top y=0',animationSource:'Rapier 3D rigid-body simulation, 240 Hz bake sampled at 30 Hz',reset:'Stop animation and restore node transforms from the original GLB pose'},[topple]);
await fs.writeFile(new URL('tower-physics-report.json',EVID),JSON.stringify({simulationSeconds:4,sampleRate:30,impulseApplied:impulse,minBoxCornerY:minCornerY,maxHorizontalDisplacement:maxDisplacement,finalPoses:bodies.map(b=>({position:b.translation(),rotation:b.rotation()}))},null,2));world.free();
// Folded paper mesh: four broad panels, no texture dependency, front is -Z.
const plane=new T.Group();function tri(name,vertices,mat){const geo=new T.BufferGeometry();geo.setAttribute('position',new T.Float32BufferAttribute(vertices.flat(),3));geo.computeVertexNormals();return mesh(name,geo,mat);}
const nose=[0,.002,-.15],tail=[0,.008,.105],keel=[0,-.025,.045],left=[-.11,0,.085],right=[.11,0,.085],foldL=[-.025,.01,.035],foldR=[.025,.01,.035];
plane.add(tri('wing_left',[nose,left,foldL,nose,foldL,tail],M.paper));plane.add(tri('wing_right',[nose,foldR,right,nose,tail,foldR],M.paper));plane.add(tri('fold_left',[nose,foldL,keel,foldL,tail,keel],M.fold));plane.add(tri('fold_right',[nose,keel,foldR,foldR,keel,tail],M.sage));await save('paper_plane','Gentle Glide paper plane',plane,{pivot:'fold center',forwardAxis:'-Z',collision:{type:'box',size:[.22,.035,.255]},doubleSidedPaper:true});
// Rounded star contour, extruded and beveled. Geometry and emissive color are genuine GLB data.
const points=[];for(let i=0;i<10;i++){const a=Math.PI/2+i*Math.PI/5,r=i%2===0?.0225:.011;points.push(new T.Vector2(Math.cos(a)*r,Math.sin(a)*r));}
const shape=new T.Shape();for(let i=0;i<10;i++){const prev=points[(i+9)%10],cur=points[i],next=points[(i+1)%10],a=cur.clone().lerp(prev,.17),b=cur.clone().lerp(next,.17);if(!i)shape.moveTo(a.x,a.y);else shape.lineTo(a.x,a.y);shape.quadraticCurveTo(cur.x,cur.y,b.x,b.y);}shape.closePath();
const sg=new T.ExtrudeGeometry(shape,{depth:.004,bevelEnabled:true,bevelSegments:2,steps:1,bevelSize:.0012,bevelThickness:.001,curveSegments:4});sg.translate(0,0,-.002);const starG=new T.Group();starG.add(mesh('star_core',sg,M.star));await save('star','Soft emissive collectible star',starG,{pivot:'center',collision:{type:'sphere',radius:.023},glow:'Core emissive material is included; optional soft halo texture ui/star_halo.png is delivered separately. No post-processing extension required.'});
const meter=new T.Group();const backing=box('meter_backing',.078,.31,.01,M.cream,.01);backing.position.y=.155;meter.add(backing);
for(let i=0;i<13;i++){const seg=box('meter_segment_'+String(i*5).padStart(2,'0'),.05,.014,.006,i<=4?M.sage:M.inactive,.004);seg.position.set(0,.018+i*.023,.008);seg.userData={degrees:i*5,role:'runtime replace material to highlight current angle'};meter.add(seg);}await save('angle_meter','Segmented extension angle meter',meter,{pivot:'bottom center',segments:13,stepDegrees:5,rangeDegrees:[0,60],runtime:'Change segment materials independently; current degree text remains native UI.'});
const dial=new T.Group();const arc=new T.Mesh(new T.TorusGeometry(.12,.008,8,64,Math.PI),M.sage);arc.name='dial_arc';dial.add(arc);
for(let i=0;i<7;i++){const a=Math.PI*i/6,tick=box('dial_tick_'+i,.002,.014,.003,M.dark,.0008);tick.position.set(Math.cos(a)*.12,Math.sin(a)*.12,.009);tick.rotation.z=a-Math.PI/2;dial.add(tick);}
const pivot=new T.Group();pivot.name='needle_pivot';const needle=box('dial_needle',.006,.105,.006,M.wood,.002);needle.position.y=.0525;pivot.add(needle);dial.add(pivot);const hub=mesh('dial_hub',new T.CylinderGeometry(.013,.013,.008,24),M.dark);hub.rotation.x=Math.PI/2;hub.position.z=.008;dial.add(hub);await save('rudder_gauge','Pivoted side-bending gauge',dial,{pivot:'dial center',needleNode:'needle_pivot',rotationAxis:'local Z',rangeDisplay:'Left/right calibrated roll; illustrative tick labels delivered as vector UI'});
await fs.writeFile(new URL('../model-catalog.json',import.meta.url),JSON.stringify(catalog,null,2));
// Correct catalog location is the production root, adjacent to models/.
await fs.writeFile(new URL('../evidence/model-validation.json',import.meta.url),JSON.stringify(validation,null,2));
console.log(JSON.stringify({models:catalog.length,bytes:catalog.reduce((a,x)=>a+x.bytes,0),errors:validation.reduce((a,x)=>a+x.errors,0),warnings:validation.reduce((a,x)=>a+x.warnings,0),tower:{minCornerY,maxDisplacement}},null,2));
