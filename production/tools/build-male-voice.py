from pathlib import Path
import os,asyncio,subprocess,wave,array,math,json,sys
import edge_tts
ROOT=Path(__file__).resolve().parents[1]
SCRIPTS=[
('start','请坐稳，放松肩膀。准备好后，我们开始。',"Sit comfortably and relax your shoulders. When you're ready, let's begin."),
('calibrate','请保持头部正中。校准完成。','Keep your head centered. Calibration complete.'),
('countdown_3','三','Three'),('countdown_2','二','Two'),('countdown_1','一','One'),('begin','开始','Begin'),
('over_limit','请缓慢回正，回到舒适的姿态。','Slowly return to a comfortable, neutral posture.'),
('extend','请坐稳，缓慢向后抬头。动作以舒适为限。','Stay seated and slowly tilt your head back. Move only within your comfort.'),
('lateral','保持肩部放松。缓慢向左，或向右侧倾。','Relax your shoulders. Slowly tilt your head to the left, or to the right.'),
('finish_success','本次训练已完成。做得很好，请放松休息。','Your session is complete. Well done. Relax and take a short rest.'),
('finish_ended','本次训练已结束。请放松颈部，稍作休息。','Your session has ended. Relax your neck and take a short rest.'),
('tracking_lost','暂时无法读取头部姿态。本次训练已结束。','Head tracking is currently unavailable. This session has ended.')]
SCRIPTS += [
('intro_tower','欢迎来到稳姿高塔。请坐稳并放松肩膀。开始前，设置低头角度上限、挑战时长和宽容时间，再将高塔放到合适的位置。目视前方，校准后开始。训练中，高塔会随低头角度倾斜，回正后恢复。连续超限超过宽容时间，高塔会倾倒，挑战结束。挑战不可暂停，可以随时终止。如有不适，请立即停止。',
 'Welcome to Posture Tower. Sit comfortably and relax your shoulders. Set the downward angle limit, duration, and grace period, then place the tower where you want it. Look straight ahead, calibrate, and begin. The tower leans as you look down and straightens when you return upright. Staying over the limit beyond the grace period topples the tower and ends the challenge. You can end the session at any time, but cannot pause it. Stop if you feel discomfort.'),
('intro_flight','欢迎来到舒颈飞行。请坐稳并放松肩膀。目视前方，校准后开始。缓慢向左或向右倾斜头部，保持约半秒，控制纸飞机转向。碰到星星即可收集。快速抬头可以显示停止飞行按钮，也可以随时点击停止训练。动作以舒适为限，如有不适，请立即停止。',
 'Welcome to Gentle Glide. Sit comfortably and relax your shoulders. Look straight ahead, calibrate, and begin. Slowly tilt your head left or right and hold for about half a second to steer the paper plane. Touch stars to collect them. A quick upward look reveals a Stop Flight button. You can also use Stop Training at any time. Move only within your comfort, and stop if you feel discomfort.'),
('intro_extension','接下来进行后仰角度测试。请笔直坐稳，放松肩膀，目视前方并校准坐姿。开始后，缓慢向后仰头，前方角度条会显示当前角度，并记录本次最大后仰角度。无需追求更大的角度，动作以舒适为限。完成后，点击结束并记录。如有疼痛或头晕，请立即停止。',
 'This is the neck extension check. Sit upright, relax your shoulders, and look straight ahead to calibrate. Once started, slowly tilt your head back. The bar in front shows your current angle and records your maximum extension. There is no need to aim for a larger angle. Stay within your comfortable range. Select Finish and Save when done. Stop immediately if you feel pain or dizziness.'),
('intro_lateral','接下来进行侧伸角度测试。请笔直坐稳，保持肩部放松，目视前方并校准坐姿。开始后，缓慢向左或向右倾斜头部，不要转动身体。前方指针会显示侧伸角度，并分别记录左右两侧的最大角度。无需追求更大的角度，动作以舒适为限。完成后，点击结束并记录。如有不适，请立即停止。',
 'This is the side bend check. Sit upright with relaxed shoulders and look straight ahead to calibrate. Once started, slowly tilt your head left or right without turning your body. The dial in front shows your side bend angle and records the maximum for each side. There is no need to aim for a larger angle. Stay within your comfortable range. Select Finish and Save when done. Stop if you feel discomfort.')]
INTRO_ONLY='--introductions' in sys.argv
SR=24000
sample_mode='--samples' in sys.argv
sample_keys={'start','over_limit','finish_success'}
report=[]
def normalize(source,out):
 raw=source.with_suffix('.raw.wav');subprocess.run(['afconvert','-f','WAVE','-d','LEI16@24000','-c','1',str(source),str(raw)],check=True,stdout=subprocess.DEVNULL)
 with wave.open(str(raw),'rb') as w:
  assert w.getsampwidth()==2 and w.getnchannels()==1 and w.getframerate()==SR
  a=array.array('h');a.frombytes(w.readframes(w.getnframes()));v=[x/32768 for x in a]
 active=[i for i in range(0,len(v),240) if math.sqrt(sum(x*x for x in v[i:i+240])/max(1,len(v[i:i+240])))>.002]
 if not active:raise RuntimeError('Silent audio')
 v=v[max(0,active[0]-1920):min(len(v),active[-1]+240+2400)];rms=math.sqrt(sum(x*x for x in v)/len(v));peak=max(map(abs,v));gain=min(10**(-3/20)/peak,10**(-20/20)/rms);v=[x*gain for x in v]
 for i in range(min(120,len(v)//2)):v[i]*=i/120;v[-i-1]*=i/120
 data=array.array('h',(round(max(-.999,min(.999,x))*32767) for x in v))
 with wave.open(str(out),'wb') as w:w.setparams((1,2,SR,len(data),'NONE','not compressed'));w.writeframes(data.tobytes())
 raw.unlink()
 return {'duration':len(v)/SR,'peakDbFS':20*math.log10(max(map(abs,v))),'rmsDbFS':20*math.log10(math.sqrt(sum(x*x for x in v)/len(v))),'clippedSamples':sum(abs(x)>=.999 for x in v)}
async def main():
 proxy=os.environ.get('HTTPS_PROXY') or os.environ.get('https_proxy')
 for locale,voice,idx,rate in [('zh-CN','zh-CN-YunyangNeural',1,'-8%'),('en','en-US-GuyNeural',2,'-6%')]:
  folder=ROOT/'audio'/('samples-male/'+locale if sample_mode else locale);folder.mkdir(parents=True,exist_ok=True)
  for cue in SCRIPTS:
   key,text=cue[0],cue[idx]
   if sample_mode and key not in sample_keys:continue
   if INTRO_ONLY and not key.startswith('intro_'):continue
   mp3=folder/(key+'.mp3');out=folder/(key+'.wav')
   for attempt in range(2):
    try:
     await edge_tts.Communicate(text,voice,rate=rate,pitch='-2Hz',proxy=proxy,connect_timeout=20,receive_timeout=45).save(str(mp3));break
    except Exception:
     if attempt:raise
   info=normalize(mp3,out)
   report.append({'id':key,'file':str(out.relative_to(ROOT)),'category':'voice','locale':locale,'text':text,'voice':voice,'gender':'Male','rate':rate,'pitch':'-2Hz','sampleRate':SR,'channels':1,'bitDepth':16,'source':'Microsoft neural speech via edge-tts','offlineSynthesis':False,**info})
   print(locale,key,round(info['duration'],2),'seconds',flush=True)
 if sample_mode:
  (ROOT/'male-voice-samples.json').write_text(json.dumps(report,ensure_ascii=False,indent=2))
 else:
  old=json.loads((ROOT/'audio-catalog.json').read_text());replaced={(x['id'],x['locale']) for x in report};all_items=[x for x in old if (x['id'],x['locale']) not in replaced]+report
  (ROOT/'audio-catalog.json').write_text(json.dumps(all_items,ensure_ascii=False,indent=2))
  (ROOT/'evidence/audio-validation.json').write_text(json.dumps({'files':len(all_items),'voiceRevision':'male-neural-v3-introductions','silent':sum(x['peakDbFS']<-80 for x in all_items),'clipped':sum(x['clippedSamples'] for x in all_items),'items':all_items},ensure_ascii=False,indent=2))
 print('Complete',len(report),'male voice files',flush=True)
asyncio.run(main())
