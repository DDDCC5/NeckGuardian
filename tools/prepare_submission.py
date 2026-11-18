from pathlib import Path
import json, xml.etree.ElementTree as ET, base64
ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'submission/v1.6'
OUT.mkdir(parents=True,exist_ok=True)
NS='{http://schemas.android.com/apk/res/android}'
def vector(name):
    def convert(e):
        if e.tag=='path':
            attrs={'d':e.get(NS+'pathData','')}
            mapping={'fillColor':'fill','strokeColor':'stroke','strokeWidth':'stroke-width','strokeLineCap':'stroke-linecap','strokeLineJoin':'stroke-linejoin'}
            for a,b in mapping.items():
                v=e.get(NS+a)
                if v: attrs[b]='none' if v=='@android:color/transparent' else v
            return '<path '+' '.join(f'{k}="{v}"' for k,v in attrs.items())+'/>'
        if e.tag=='group':
            x=e.get(NS+'translateX','0');y=e.get(NS+'translateY','0');sx=e.get(NS+'scaleX','1');sy=e.get(NS+'scaleY','1')
            return f'<g transform="translate({x} {y}) scale({sx} {sy})">'+''.join(map(convert,e))+'</g>'
        return ''.join(map(convert,e))
    return convert(ET.parse(ROOT/f'app/src/main/res/drawable/{name}.xml').getroot())
# Submission artwork retains the approved symbol and fills all four corners.
icon_svg=(ROOT/'production/icon/app_icon_v1.svg').read_text().replace('<rect x="8" y="8" width="240" height="240" rx="60"', '<rect x="0" y="0" width="256" height="256"')
(OUT/'01-icon-source.svg').write_text(icon_svg)
import subprocess
subprocess.run(['node','--input-type=module','-e', '''import {Resvg} from "./production/tools/node_modules/@resvg/resvg-js/index.js"; import fs from "node:fs"; fs.writeFileSync("submission/v1.6/01-icon-1024.png",new Resvg(fs.readFileSync("submission/v1.6/01-icon-source.svg"),{fitTo:{mode:"width",value:1024}}).render().asPng());'''],cwd=ROOT,check=True)
icon=base64.b64encode((OUT/'01-icon-1024.png').read_bytes()).decode()
cards=[('ic_health_check','颈部感受自评',['六道问题，回顾近期感受','以图形与文字提供分组参考'],'六题自评','#F5F0E4'),('ic_training','颈椎挑战训练',['让高塔随姿态保持平衡','侧倾操控飞机，收集沿途星星'],'稳姿高塔 · 舒颈飞行','#F4E9DF'),('ic_routine','活动角度记录',['后仰与左右侧伸','查看当前角度与本次最大值'],'前方标尺 · 左右指针','#E9EEE2')]
parts=[f'''<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" width="2560" height="1440" viewBox="0 0 2560 1440"><rect width="2560" height="1440" fill="#FAF7EF"/><circle cx="2460" cy="80" r="430" fill="#E5ECDC"/><circle cx="80" cy="1470" r="420" fill="#F0E2CF"/><g font-family="PingFang SC,Heiti SC,sans-serif" fill="#354B40"><image x="140" y="138" width="184" height="184" xlink:href="data:image/png;base64,{icon}"/><text x="382" y="266" font-size="120" font-weight="600">颈椎卫士</text><text x="140" y="423" font-size="52" fill="#557665">让每一次轻轻回正，都有清晰反馈</text><text x="2420" y="248" text-anchor="end" font-size="36" fill="#557665">PICO Spatial</text>''']
for i,(name,title,body,tag,fill) in enumerate(cards):
    x=140+i*774
    parts+=[f'<rect x="{x}" y="540" width="732" height="630" rx="44" fill="{fill}"/>',f'<g transform="translate({x+56} 598) scale(7)">{vector(name)}</g>',f'<text x="{x+56}" y="856" font-size="56" font-weight="600">{title}</text>']
    for j,line in enumerate(body):parts.append(f'<text x="{x+56}" y="{938+j*54}" font-size="32">{line}</text>')
    parts.append(f'<text x="{x+56}" y="1100" font-size="30" fill="#557665">{tag}</text>')
parts+=['<text x="140" y="1255" font-size="34" fill="#557665">中英文随系统切换 · 专业男声讲解 · 最近 30 条本地记录</text>','<text x="140" y="1320" font-size="34">仅作健康参考，不构成医疗诊断或治疗建议</text>','<text x="140" y="1390" font-size="22" fill="#78857A">DDDCC5 · V1.6</text>','<text x="2420" y="1390" text-anchor="end" font-size="22" fill="#78857A">宣传设计图 · 非运行截图</text></g></svg>']
(OUT/'02-poster-2560x1440.svg').write_text(''.join(parts))
short='用头部姿态平衡高塔、操控纸飞机，在柔和的空间互动中完成颈部自评、活动角度记录与舒缓训练。'
intro='''颈椎卫士是一款面向 PICO Spatial 的颈部感受自评与姿态训练应用，用温和的配色、可视化反馈和专业男声引导，让每一次轻轻回正都有清晰回应。

先了解自己的日常感受
通过六道自评问题回顾近期颈部感受，结果用不同颜色与形状的图标提示关注程度。自评只是健康参考，不是经过临床验证的诊断量表。

两项挑战，让动作有反馈
稳姿高塔支持设置低头角度上限、挑战时长和宽容时间。高塔随低头角度倾斜，及时回正即可恢复；持续超限则倾倒结束。舒颈飞行通过持续左右侧倾控制纸飞机转向，碰到星星即可收集，并统计收集、错过及完成率。

记录舒适范围内的活动角度
后仰测试以前方角度条反馈当前角度，侧伸测试以指针反馈左右倾斜，分别记录最大后仰和左右侧伸。无需追求更大的角度，动作以舒适为限。

轻量而完整的使用流程
设置页提供动态模型预览、内容讲解与重听入口；结果保留最近30条本地记录。界面与专业男声随系统语言切换为简体中文或英文，音量跟随系统。

菜单和自评使用原生窗口，涉及真实头姿的训练进入透视训练空间，结束后返回窗口。本应用不提供跨应用后台姿态监测。

本应用仅作健康参考，不构成医疗诊断或治疗建议。如有不适请立即停止，必要时就医。'''
version='''V1.6 体验版（Debug）
1. 提供六题颈部感受自评、稳姿高塔、舒颈飞行、后仰及侧伸角度记录。
2. 四个设置页统一“重听讲解／返回选择／进入”操作，内置动态模型预览与中英文专业男声内容讲解。
3. 舒颈飞行使用半尺寸飞机与同步缩小的碰撞轮廓，记录收集、错过及完成率，并保存结果。
4. 统一字体与图标，精简空间模式、存储状态等冗余说明，自评三档结果使用不同颜色与形状的图标。
5. 支持简体中文及英文随系统切换，保留最近30条本地记录。
采用真实头姿追踪，训练期间进入透视全空间；Shared Space后台不读取头姿。仅作健康参考，不构成医疗诊断。'''
ai='''使用 Codex 辅助需求拆解、交互与UI迭代、Kotlin/SpatialUI Compose实现、模型与界面整合、测试及设备日志排查。主要AI模型按创作者此前确认填写 GPT-6.1 Sol。模型与图标主要由程序化几何和SVG/矢量制作，倾倒动画使用物理模拟采样；交互音效为确定性合成。专业男声由神经语音合成生成，非开发者本人录音。PICO CLI用于环境配置、项目初始化、构建辅助、设备安装与截图验证，未用于生成本项目素材。'''
english='''NeckGuardian is a PICO Spatial app for neck wellbeing awareness and gentle posture challenges. Answer six self-report questions, balance a tower with your posture, steer a paper plane to collect stars, and record comfortable extension and side-bend angles. It includes animated setup previews, professional voice guidance, collected/missed star statistics, and the latest 30 local records. UI and speech follow the system language in Simplified Chinese or English. Real-pose activities use passthrough Full Space and return to a native window afterward. This app does not monitor posture across other apps in the background. For wellbeing reference only, not medical diagnosis or treatment. Stop if you feel discomfort.'''
for file,text in [('一句话简介.txt',short),('作品简介.txt',intro),('版本说明.txt',version),('AI辅助开发说明.txt',ai),('Introduction-English.txt',english)]: (OUT/file).write_text(text+'\n')
assert len(short)<=80 and len(version)<=1000
fields={'GitHub仓库地址':'https://github.com/DDDCC5/NeckGuardian','GitHub Commit ID':json.loads((OUT/'填写字段.json').read_text()).get('GitHub Commit ID','待填写') if (OUT/'填写字段.json').exists() else '待填写','技术栈':'PICO Spatial SDK / Android原生 / Kotlin / SpatialUI Compose','内容类型':'应用','特性标签建议':['混合现实','手势交互／手势追踪（按实际选项）'],'内容标签建议':'健康生活／运动／生活方式，按表单实际选项匹配','版本名称':'V1.6 体验版（Debug）','APK versionName':'1.6','APK versionCode':7,'包名':'com.example.neckguardian','AI IDE':'Codex','主要 AI 模型':'GPT-6.1 Sol','队友':'无实际人类队友则留空','作品名称':'颈椎卫士','英文名称':'NeckGuardian','一句话简介':short,'版本说明':version,'作品简介':intro,'AI辅助开发说明':ai,'Release':'https://github.com/DDDCC5/NeckGuardian/releases/tag/v1.6'}
(OUT/'填写字段.json').write_text(json.dumps(fields,ensure_ascii=False,indent=2)+'\n')
print(json.dumps({'shortCharacters':len(short),'versionCharacters':len(version)},ensure_ascii=False))
