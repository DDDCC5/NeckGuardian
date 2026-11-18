from pathlib import Path
import json,hashlib,zipfile,wave,struct,xml.etree.ElementTree as ET
R=Path(__file__).resolve().parents[1];files=[]
# Whitelist resources. No authoring tools, voice environments, temp audio, old voices or archives ship.
for folder in ['models','ui','icon','preview']:
 files.extend(p for p in (R/folder).rglob('*') if p.is_file())
files.extend(p for p in (R/'audio').rglob('*.wav'))
for name in ['ASSET_GUIDE.md','model-catalog.json','ui-catalog.json','audio-catalog.json','male-voice-samples.json']:files.append(R/name)
for name in ['model-validation.json','image-validation.json','audio-validation.json','tower-physics-report.json','browser-model-review.json','browser-media-review.json','animation-playback.json']:files.append(R/'evidence'/name)
# Static model thumbnails make the extracted preview usable even before WebGL is enabled.
files.extend((R/'evidence').glob('thumb-*.png'))
files=list(sorted(set(files)))
for p in (R/'icon').glob('*.xml'):ET.parse(p)
models=json.loads((R/'model-catalog.json').read_text());audios=json.loads((R/'audio-catalog.json').read_text());uis=json.loads((R/'ui-catalog.json').read_text());screens=json.loads((R/'ui/screen-catalog.json').read_text())
assert len(models)==9 and len(uis)==23 and len(screens)==28
voices=[x for x in audios if x['category']=='voice'];assert len(voices)==24 and all(x['gender']=='Male' for x in voices)
assert {x['voice'] for x in voices}=={'zh-CN-YunyangNeural','en-US-GuyNeural'}
assert len([x for x in audios if x['category']=='sfx'])==6
for x in audios:
 with wave.open(str(R/x['file']),'rb') as w:assert w.getnchannels()==1 and w.getsampwidth()==2 and w.getframerate()==24000
for x in models:
 b=(R/x['file']).read_bytes();assert b[:4]==b'glTF' and struct.unpack_from('<I',b,4)[0]==2
for x in screens:assert (R/x['file']).is_file()
rejected={hashlib.sha256(p.read_bytes()).hexdigest() for p in (R/'archive/voice-v1').rglob('*.wav')}
entries=[]
for p in files:
 rel=p.relative_to(R).as_posix();h=hashlib.sha256(p.read_bytes()).hexdigest();assert not any(x in rel for x in ['archive/','audio-tmp/','node_modules/','voice-env/']);assert h not in rejected,'Rejected voice matched '+rel
 entries.append({'file':rel,'bytes':p.stat().st_size,'sha256':h})
manifest={'application':'com.example.neckguardian','version':'assets-v1-male-neural','status':'ready_for_full_asset_review','approvals':{'designV2':True,'professionalMaleVoice':True,'completeAssetPackage':False},'counts':{'glbModels':9,'uiSvgPngPairs':23,'screenReferences':28,'launcherPngSizes':7,'guidanceVoices':24,'interactionSounds':6,'extraAuditionSamples':6},'picoCliUsedForAssetGeneration':False,'androidApplicationCompiled':False,'files':entries}
(R/'asset-manifest.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2))
zip_path=R/'NeckGuardian-assets-v1.zip'
with zipfile.ZipFile(zip_path,'w',compression=zipfile.ZIP_DEFLATED,compresslevel=7) as z:
 for p in files+[R/'asset-manifest.json']:z.write(p,p.relative_to(R))
with zipfile.ZipFile(zip_path) as z:
 assert z.testzip() is None
 for x in entries:assert hashlib.sha256(z.read(x['file'])).hexdigest()==x['sha256']
summary={'files':len(entries)+1,'zipBytes':zip_path.stat().st_size,'zipSha256':hashlib.sha256(zip_path.read_bytes()).hexdigest(),'rejectedVoiceHashesChecked':len(rejected),'rejectedVoiceMatches':0,'crcValid':True,'allEntryHashesMatch':True}
(R/'evidence/package-validation.json').write_text(json.dumps(summary,indent=2));print(json.dumps(summary,indent=2))
