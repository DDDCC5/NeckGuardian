# 三套训练环境 v6

已确认并接入颈椎卫士1.8.1（versionCode10）。林间晨光、湖畔薄雾、暖阳书房均为实际GLB，文件与APK内资源校验值相同。原始资产、阶段模型与制作脚本保留用于追溯。

- lake-v6.glb：湖畔、木平台、岸线、树木、弯曲渐细草簇。
- forest-v6.glb：24棵树、起伏地形、开阔空地、步道和14组蕨类。
- study-v6.glb：完整房间、暖木书架、书籍、桌椅、台灯、沙发、落地窗和庭院。

树叶从原PNG透明轮廓构造为双面几何薄片（7行边界、每叶12三角形），使用OPAQUE材质，移除透明裁切依赖；原PNG未改写。树干来自既有Tripo模型，草和场景结构由本地脚本制作。不是全套Tripo新生成，也不是Spatial Editor bundle。

米制、Y向上、前方−Z；眼点(0,1.25,1.8)m。三个GLB均经glTF校验0错误/0警告，无外链资源。*-layout.json只证明选定道具的源世界AABB未进入中央通道，不等于设备碰撞或佩戴验收。manifest.json记录哈希和预算。

从仓库根目录启动静态HTTP服务后，打开production/preview/environments-v6.html。页面直接加载本目录最终GLB；*-preview.png是真实浏览器渲染，不是头显截图。飞机和星星独立于环境文件，预览不产生训练统计。原生光照、阴影和水波不保证等效。

制作顺序：

```sh
node production/tools/build-environments-v6.mjs
python3 production/tools/solid-leaf-silhouette.py
node production/tools/check-environment-layout.mjs
```

叶片转换必须紧接builder，不能对已转换模型重复单独执行。只构建Android应用无需运行这些脚本、连接Tripo或重新付费。当前实例三角形：湖畔606350、林间551792、书房46800；尚未完成头显性能验收。

原生叶片缺失的底层原因尚未确认，不能宣称glTF MASK普遍不受支持；本版通过几何轮廓方案避免该依赖。三场景视觉与舒适度仍待完整验收。详见docs/releases/1.8.1.md和docs/ENVIRONMENTS.md。
