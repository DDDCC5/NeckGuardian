"""Original synthesized ambience candidates. No speech, recording or copyrighted music.
24kHz mono PCM; repeatable noise beds with a one-second circular crossfade.
"""
from pathlib import Path
import array,math,random,wave,json,hashlib
OUT=Path(__file__).resolve().parents[1]/'environments/v1'
RATE=24000;DURATION=24;OVERLAP=RATE
report=[]
for kind in ['forest','lake','study']:
 rng=random.Random({'forest':1701,'lake':1702,'study':1703}[kind]);samples=[];low=0.;slow=0.;mid=0.
 # Quiet leaf/water/room beds: no strong beat, sharp clicks or abrupt bird calls.
 for i in range((DURATION+1)*RATE):
  t=i/RATE;white=rng.uniform(-1,1);low+=.012*(white-low);slow+=.0012*(white-slow);mid+=.17*(white-mid)
  if kind=='forest':
   envelope=.60+.17*math.sin(t*.63)+.12*math.sin(t*.23)
   x=(mid-low)*.17*envelope+low*.40
   for at in (4.3,10.7,17.2,21.1):
    dt=t-at
    if 0<dt<.33:
     x+=.007*math.sin(math.pi*dt/.33)**2*math.sin(2*math.pi*(1750*dt+520*dt*dt))
  elif kind=='lake':
   wave_env=(.45+.4*math.sin(t*.74)**2)
   x=low*.52+(mid-low)*.11*wave_env+slow*.32
  else:x=low*.38+slow*.6+(mid-low)*.018
  samples.append(x)
 # Mix the final one-second tail into the beginning. End/start share the same noise trajectory.
 output=samples[OVERLAP:DURATION*RATE]
 blend=[]
 for i in range(OVERLAP):
  k=i/OVERLAP;blend.append(samples[DURATION*RATE+i]*(1-k)+samples[i]*k)
 output+=blend
 mean=sum(output)/len(output);output=[x-mean for x in output]
 peak=max(abs(x) for x in output);target={'forest':.075,'lake':.07,'study':.045}[kind]
 output=[x*target/peak for x in output]
 pcm=array.array('h',(max(-32768,min(32767,round(x*32767))) for x in output))
 p=OUT/f'{kind}.wav'
 with wave.open(str(p),'wb') as w:w.setnchannels(1);w.setsampwidth(2);w.setframerate(RATE);w.writeframes(pcm.tobytes())
 rms=math.sqrt(sum(x*x for x in output)/len(output));report.append({'id':kind,'file':p.name,'durationSeconds':len(output)/RATE,'sampleRate':RATE,'channels':1,'bits':16,'peakDbFS':round(20*math.log10(target),2),'rmsDbFS':round(20*math.log10(rms),2),'seamStep':round(abs(output[-1]-output[0]),7),'sha256':hashlib.sha256(p.read_bytes()).hexdigest(),'source':'Deterministic original synthesis; candidate for listening review'})
(OUT/'audio-validation.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
print(json.dumps(report,ensure_ascii=False,indent=2))
