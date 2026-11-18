# 颈椎卫士 · 原第二阶段素材设计记录

历史记录：以下保留第二阶段素材交付时的设计约定及当时状态，不代表当前开发进度。当前1.6版已完成开发与验收，图标、模型和38段音频均已集成；最新运行行为、构建及验证说明以根目录README、docs/ARCHITECTURE.md和docs/VERIFICATION.md为准。早期证据、试听样本与已拒绝音色不纳入公开仓库。

## 文件入口

- `preview/index.html`：实际 GLB、UI/图标与声音集中预览。用本地 HTTP 服务打开；浏览器拖动旋转，点击播放真实倾倒动画。
- `model-catalog.json`：9个模型的名称、节点、米制尺寸、碰撞建议、动画与文件路径。
- `ui-catalog.json`：23组可复用SVG/PNG素材。
- `ui/screen-catalog.json`：14类界面的中英文组合参考，共28张800×600 PNG。
- `audio-catalog.json`：当前采用的语音与音效；不包含已拒绝的旧音色。
- `asset-manifest.json`：最终文件、用途和SHA-256摘要。

## 3D模型

所有模型为glTF 2.0二进制GLB，米制、右手坐标、Y向上。PBR材质随GLB内嵌，不依赖网络纹理。当前实际浏览器加载通过；PICO SDK导入仍待第三阶段验证。

| 文件 | 用途和节点约定 |
|---|---|
| models/block_sage.glb | 鼠尾草积木，0.07×0.035×0.045m，底部中心原点 |
| models/block_peach.glb | 杏桃积木，同尺寸 |
| models/block_cream.glb | 奶油黄积木，同尺寸 |
| models/posture_tower.glb | 一列六块独立积木；节点tower_block_01至06；含桌面 |
| models/tabletop.glb | 圆角虚拟桌面；上表面Y=0，原点为台面中心 |
| models/paper_plane.glb | 暖纸色飞机，浅绿折面，机首朝局部-Z；面材质双面 |
| models/star.glb | 圆角五角星，实体厚度和柔和自发光材质 |
| models/angle_meter.glb | 13个独立分段，节点meter_segment_00至60，每5°一段 |
| models/rudder_gauge.glb | 弧形刻度与独立needle_pivot节点，绕局部Z轴驱动 |

高塔初始是竖直单列，六个节点可以由应用在宽容期内按头部倾角联动，并恢复初始变换。GLB另含 `topple_physics`，4秒、30Hz关键帧，来自240Hz刚体模拟，逐块运动；结束后停止动画、恢复GLB初始节点变换即可复位。它不是运行时持续的IMU/物理系统，第三阶段实现触发和生命周期。

为给真实倾倒留出落点，成品桌面采用0.9×0.4m展示面，较概念图中的初始约0.55m宽度留出更多余量。应用可将整个场景按比例适配窗口，不改变模型相对比例。烘焙动画中的最大水平位移约0.302m；数值求解器的最低包围盒角点约-0.00061m，属于亚毫米接触误差。具体数据见 `evidence/tower-physics-report.json`。

星星的自发光属于GLB真实材质；柔光扩散由随附 `ui/star_halo.png` 提供，应用可用面向观察者的透明图形实现。浏览器审阅已组合该贴图。不能把浏览器光照和阴影当作模型文件中的运行环境。

所有轴心的文字说明使用 `extras.originDescription`。`needle_pivot`本身是实际变换节点，不能把轴心描述字符串当作几何坐标。

## 图标

`icon/app_icon_v1.svg` 为第一版批准方向的源图，无内嵌语言文字。PNG尺寸：48、72、96、144、192、512、1024。另有透明适配前景、背景和Android VectorDrawable/AdaptiveIcon XML。

第三阶段首次debug APK必须使用本目录第一版图标，并验证安装包资源与系统启动器显示；当前并未声称图标已装入APK。中文名称仅“颈椎卫士”，英文/default名称仅“NeckGuardian”，名称由本地化资源提供。

## UI

`ui/`提供圆角卡片、按钮视觉状态、问卷单选状态、进度条、角度分段、针形、星星柔光、结果状态和模块插画。每项同时提供SVG和PNG；细节见索引。

`ui/screens/zh/`与`ui/screens/en/`分别是启动、免责声明、主菜单、问卷、训练选择、两个训练设置、两个运行场景、后仰、侧伸、结果、历史、自评结果的组合参考。

完整页面PNG仅用于核对布局和文案，不作为可点击的整页贴图。动态文字、角度、计时、答题和按钮交互须由SpatialUI原生组件实现；窗口材质使用系统能力，不将参考图中的模拟玻璃烘焙为实际窗口背景。主题和间距见 `ui/theme.json`。

## 声音

用户已拒绝初版系统音色，旧文件移入 `archive/voice-v1/`，不再用于当前预览或最终ZIP。旧生成器也已停用，后续使用 `tools/build-male-voice.py`。

新版选择专业男声：中文Microsoft神经语音Yunyang，英文Guy；语速轻缓，音高微调，文案简洁、停顿清楚。新版完整库已重制为24段专业男声，见 `audio-catalog.json`；另保留6段试听样本在 `audio/samples-male/`。新版男声音色已获用户确认；全部24段中英文引导已按该音色替换。仅合成应用通用提示文案，不处理用户健康数据。

正式音频库为12段中文男声、12段英文男声和6类无语言交互音，另有6段重复用途的试听样本。运行音频为单声道24kHz、16-bit PCM WAV；峰值上限-3dBFS，效果音约-10dBFS；播放遵循系统音量，应用内不新增音量设置。中文环境只播放中文，英文环境只播放英文。语音可被停止/超限等更高优先级事件打断，不能在训练结束后继续排队播报旧提示。

## 工具与验证

素材由本地几何/矢量工具与独立语音合成制作，没有调用PICO CLI生成素材。作者脚本保留在工作区production/tools/，版本由package-lock.json记录；最终ZIP不包含开发依赖、临时文件或旧版归档。重建UI的作者脚本还会使用工作区已确认的.scratch矢量设计稿。浏览器预览携带Three.js运行文件与MIT许可，不依赖外部CDN。

检查记录：`evidence/model-validation.json`、`image-validation.json`、`audio-validation.json`、`browser-model-review.json`。这些证据不替代PICO头显上的SDK兼容性、交互、IMU与性能验证；第三阶段必须完成这些验证后才能交付可运行APK。
