# 训练环境与素材来源

1.8.1在舒颈飞行开始前选择真实空间、林间晨光、湖畔薄雾或暖阳书房。`TrainingEnvironment`只允许固定枚举和固定资源路径，未知偏好回到真实空间。环境音偏好独立保存，不改变历史记录结构。

虚拟环境从`app/src/main/assets/environments/{forest,lake,study}-v6.glb`加载，菜单与设置保持原生窗口。图片仅用于SRGB环境照明，非可见全景。模型与原始训练物件分别归属于Stage根节点；真实头姿校准后保持世界固定，显式重新定位才改变场景锚点。退出时先销毁场景，再释放保留的照明资源。所有实时角度仍来自官方追踪，不模拟缺失角度。

模型制作源文件、实际浏览器渲染预览、glTF校验及SHA256在`production/environments/v6/`。湖畔/林间/书房分别约606350/551792/46800实例三角形。几何预算不是帧率证据，尚需设备性能与合批评估。

树干/枝条来自用户批准的Tripo任务，薄叶和连接枝条在本地重建；叶片轮廓依据本项目生成的原PNG透明通道计算，最终使用不依赖透明裁切的双面几何薄片。地形、水面、草、蕨类、平台及书房家具由本地脚本制作。草地和叶片颜色贴图使用内置图像生成工具，木纹复用本项目素材；环境音由本地脚本合成。并非所有模型都由Tripo生成，也没有Spatial Editor bundle。

关键脚本：

- `production/tools/rebuild-thin-foliage.py`：保留Tripo枝干并重建薄叶。
- `production/tools/build-environments-v6.mjs`：组装三套环境。
- `production/tools/solid-leaf-silhouette.py`：读取原PNG轮廓，生成不依赖alpha cutout的叶片几何；不改写PNG。
- `production/tools/check-environment-layout.mjs`：选定道具的源世界包围盒/训练通道检查。
- `production/tools/audit-environment-model.mjs`：glTF结构、内嵌资源及校验。

先运行builder，再运行solid-leaf-silhouette，后者不能直接对已转换的模型重复执行。使用已提交模型构建Android应用不需要重新生成素材或访问Tripo账号。原始资产和旧候选保留用于追溯，不代表全部旧候选已获视觉验收。

浏览器预览入口：`production/preview/environments-v6.html`，从仓库根目录启动静态HTTP服务后打开。预览直接加载与APK相同的GLB，但照明/阴影/雾等运行环境不同，不能替代头显验证；预览中的飞机运动只是构图演示，不产生训练数据。

当前验证边界见[1.8.1版本说明](releases/1.8.1.md)。
