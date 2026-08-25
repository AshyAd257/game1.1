# 旋转条状贴图 + Billboard俯仰过渡 —实施计划

## 0. 背景约定（调研确认）

- 编辑器（`editor.core.*`）和运行时（`core.*`）是**两套完全独立、手写镜像的代码**，没有共享基类。`EditorPuppetPartRenderer.updateTransform()` 和 `PuppetPartRenderer.updateTransform()` 的billboard分支逐行对应但各自维护。**任何新逻辑要在两个文件里各写一遍。**
- 顺带发现一个既有bug：编辑器版本的UP/DOWN billboard分支缺少核心版本里`upDotToCam > 0.999f`（摄像机几乎垂直看时的退化情况）的回退逻辑。趁着改这块顺手把editor版本补齐，否则新功能在两个预览里会不一致。
- 编辑器UI是**jME场景图部件**（Quad/Geometry手工拼出来的面板），不是Swing。Swing只在数字输入框（`JOptionPane`）里出现。`TexturePreviewPanel`已经有一套"鼠标拖拽→选区框"的实现，可以照着改成像素单位版本。
- 贴图像素尺寸统一通过 `texture.getImage().getWidth()/getHeight()`（jME Image）读取，不用`BufferedImage`。
- 序列化用Gson反射，`PartConfig`新增字段自动生效，不需要写TypeAdapter。但因为`PuppetIO.java`里一个导出方法+四个导入方法（base/editor × full-load/additive-load）都各自手写字段搬运，新字段要在全部6处（含`PuppetPackageIO`的贴图路径收集/重映射）手动加。
- 内存里从BufferedImage构造jME Texture2D已有现成模板：`TTFontLoader.convertToTexture()`（`editor/TTFontLoader.java:187-210`），直接照抄这个模式做贴图padding。

---

## 1. 数据结构

### 1.1 `Bone.java` 新增字段（跟`billboardEnabled`同一风格，紧邻多方向贴图那一段）

```java
private boolean rotationStripEnabled = false;
private String stripTexturePath;
private int stripSteps = 16;          // 0 = 连续逐像素模式
private int stripFrameWidthPx = 32;
private int stripFrameHeightPx = 32;
// billboard俯仰过渡阈值（第4部分要用，但数据结构一起加）
private float billboardPitchFullRangeDeg = 60f;
private float billboardPitchLockDeg = 80f;
```

对应getter/setter照抄`isBillboardEnabled/setBillboardEnabled`的写法。

### 1.2 `EditorBone.java` 原样镜像一份（字段名、getter/setter完全一致，这是本项目的既有约定）。

### 1.3 `PartConfig.java` 新增对应字段 + getter/setter（Gson自动反射，不用改Gson配置）。

---

## 2. 序列化（`PuppetIO.java` + `PuppetPackageIO.java`）

按调研结果，逐一确认要改的6个位置：

1. `createConfig(Skeleton, PuppetRenderer)` 写入块（~104-155行）：新增 `partConfig.setRotationStripEnabled(bone.isRotationStripEnabled())` 等6行。
2. `applyConfig(PuppetConfig, Skeleton, PuppetRenderer)` 读取块（~372-431）+ 应用到渲染器块（~484-525）。
3. `addConfig(PuppetConfig, Skeleton, PuppetRenderer)`（~649-708，附加合并模式）。
4. `applyConfig(PuppetConfig, EditorSkeleton, EditorPuppetRenderer)`（~840-899 + ~957-985）。
5. `addConfig(PuppetConfig, EditorSkeleton, EditorPuppetRenderer)`（~1080-1139 + ~1198-1231）。
6. `PuppetPackageIO.java` 的 `collectTexturePaths`（~242-253）和 `remapTexturePaths`（~273-291）：加入 `stripTexturePath` 的收集与重映射，否则打包`.puppet`时旋转条贴图会被漏掉。

---

## 3. 运行时贴图padding工具（新类）

新建 `com.Hecate.puppet.core.RotationStripTextureUtil`（或放`support`子包），职责：

```java
Texture2D getOrCreatePaddedStrip(AssetManager am, String texturePath, int requiredSteps)
```

- 用`AssetManager`加载原始贴图，读`getImage().getWidth()/getHeight()`。
- 若原宽度 >= requiredSteps，直接返回原贴图。
- 否则：仿照`TTFontLoader.convertToTexture()`的模式——分配一个`requiredSteps`宽（高度不变）的`ByteBuffer`，把原图像素数据拷贝到左侧，右侧留空（alpha=0，即透明，不是白色），构造`new Image(Format.RGBA8, ...)` → `new Texture2D(image)`，设置`Nearest`/`NearestNoMipMaps`过滤。
- 按 `texturePath + "#" + requiredSteps` 做key，用一个`Map`缓存结果，避免每帧重建（这是本类唯一的缓存，PuppetPartRenderer本身不做通用贴图缓存，调研已确认项目里没有现成的TextureManager）。

---

## 4. 运行时取样算法

### 4.1 `Bone`/`EditorBone`（已在第1步加完字段，这里只是取用）

### 4.2 `PuppetPartRenderer.updateTransform()` / `updateTextureFromBone()` 新增分支（`EditorPuppetPartRenderer`同步照抄一份）

在现有方向判断之前插入短路：

```java
if (bone.isRotationStripEnabled()) {
    applyRotationStripUV(worldRot); // 新增私有方法
} else {
    // 原有 front/back/left/right/up/down 逻辑不变
}
```

`applyRotationStripUV`逻辑：
1. 算相机相对骨骼的水平yaw（复用现有`calculateAndSetCameraDirection`里那套"camDir水平投影 → atan2"算法，抽成可复用的静态工具方法，避免第三次复制这段数学）。
2. `stripSteps > 0`：`stepIndex = ((int)Math.round(yawDeg / 360f * stripSteps)) % stripSteps`（注意负数取模要处理，Java的`%`对负数结果为负，需要`((x % n) + n) % n`归一化）。
   `stripSteps == 0`：`pixelOffset = ((int)Math.round(yawDeg / 360f * paddedTexW)) 归一化到[0, paddedTexW)`。
3. 通过`RotationStripTextureUtil.getOrCreatePaddedStrip(...)`拿到padding后的贴图和其宽度`paddedTexW`。
4. `frameStep = paddedTexW / stripSteps`（N档模式）；`pixelStart = stepIndex * frameStep`（N档）或直接用`pixelOffset`（连续模式）。
5. 换算UV：`u0 = pixelStart / (float)paddedTexW`，`u1 = (pixelStart + stripFrameWidthPx) / (float)paddedTexW`，`v0/v1`按`stripFrameHeightPx`和贴图高度算（假定纸带贴图高度就是一帧高度，不额外分段）。
6. 直接写入`partQuad`的texcoord buffer（复用现有`updateTexCoords()`的写法，贴图旋转参数在这个模式下直接跳过，旋转条贴图不支持`textureRotation`）。
7. 材质纹理过滤保持`Nearest`/`NearestNoMipMaps`（`loadTexture()`里已经这么设，不用改）。

---

## 5. 编辑器选区框工具（新jME部件）

新建 `com.Hecate.puppet.editor.RotationStripSelectorPanel`，仿照`EnlargedTexturePreviewPanel`的结构（构造签名`(app, font, x, y, width, height)`，`getRootNode()`，`Callbacks`回调接口），但做两处关键简化/修改：

1. **整数倍缩放**：不用`EnlargedTexturePreviewPanel`现成的`textureZoom`（那是任意浮点0.5-4.0），新增一个`int zoomLevel`（1/2/3/4...），改鼠标滚轮的步进为整数递增/递减，quad显示尺寸=`texPixelWidth * zoomLevel`。
2. **像素吸附选区框**：复用`TexturePreviewPanel`里"鼠标像素坐标 → 归一化UV → 拖拽画框"的整套math，但把归一化UV换成整数像素索引：
   ```
   pixelX = floor((mouseScreenX - panelOriginX) / zoomLevel)
   pixelX = clamp(pixelX, 0, texWidth)
   ```
   拖拽时实时显示`宽度Npx / 高度Npx`文本（复用已有`BitmapText`用法）。
3. 面板上加一个数字输入（复用`Slider`的双击弹`JOptionPane`模式）填N档数，实时算并提示：`"贴图宽X px，需要至少N px，将用透明填充${N-X}px"`（当X<N时）。
4. 选区结果通过`onStripFrameSelected(int px, int py, int width, int height)`回调传出，调用方（`InspectorPanel`）负责写回`EditorBone.setStripFrameWidthPx/Height`和`EditorPuppetPartRenderer`对应setter——遵循现有`TexturePreviewPanel.UVChangeListener`那种"面板不直接碰EditorBone"的解耦约定。
5. `InspectorPanel`里新增一个入口按钮（"旋转条设置"）打开这个面板，参考现有"Open UV Editor"按钮打开`EnlargedTexturePreviewPanel`的接线方式（拉取当前值→塞入面板→注册listener写回）。

---

## 6. Billboard俯仰角平滑过渡

在`PuppetPartRenderer.updateTransform()`的UNIFIED billboard分支里，现有UP/DOWN判断是**硬阈值**（`"up".equals(currentDirection)`），这次改造思路不同：不再依赖离散的`currentDirection`字符串，而是**直接算连续俯仰角**，因为旋转条纸带模式下`currentDirection`概念本身就不适用（它没有up/down这些key）。

1. 新增私有方法`calculatePitchAdjustedBillboardRotation(Vector3f partPos, Quaternion baseBillboardRot, Quaternion worldRot, float fullRangeDeg, float lockDeg)`：
   - 算摄像机相对部件的俯仰角 `pitchDeg = asin(toCam.y) 转角度`（`toCam`已经是归一化的部件→摄像机方向）。
   - `|pitch| <= fullRangeDeg`：直接返回`baseBillboardRot`。
   - `|pitch| >= lockDeg`：返回"自然竖直"旋转——用`worldRot`的Y轴保持世界UP，只绕世界Y轴对齐视线水平分量（等价于jME的`BillboardControl.Alignment.AxialY`效果，手写一个`lookAt`用水平投影的`toCam`+固定`Vector3f.UNIT_Y`up）。
   - 中间：`t = (|pitch| - fullRangeDeg) / (lockDeg - fullRangeDeg)`，`Quaternion.slerp(baseBillboardRot, uprightRot, t, result)`。
2. 这个方法替换掉现有UNIFIED分支里`"up"/"down"`那段特判代码（第628-690行），原来的水平方向分支（`else`分支，走`baseBillboardRot`）自然被新方法的`fullRangeDeg`阈值涵盖，不用单独保留。
3. **同步把`upDotToCam > 0.999f`极端角度回退逻辑保留**（原来核心版本有、编辑器版本没有的那部分）——这段仍然需要，用在算"自然竖直"旋转时camera几乎垂直向上/向下看的退化情况。
4. `EditorPuppetPartRenderer`同步实现一份（顺带修掉调研发现的那个bug）。
5. 阈值`billboardPitchFullRangeDeg`/`billboardPitchLockDeg`已在第1步加到`Bone`/`EditorBone`，`PartConfig`+`PuppetIO`一并加上序列化（同第2部分清单一起改，不单独再走一轮5个方法）。
6. `InspectorPanel`加两个数值输入（复用`Slider`或`JOptionPane`模式）暴露这两个阈值给用户调。

---

## 7. 实施顺序

1. 第1步数据结构（Bone/EditorBone/PartConfig字段）——小改动，快速做完方便后续验证编译。
2. 第2步序列化6处——跟着第1步一起做，保证存读不丢字段。
3. 第3步padding工具类——独立可单测的一小块。
4. 第4步运行时取样（PuppetPartRenderer + EditorPuppetPartRenderer）——核心效果在这一步就能在编辑器里肉眼看到（先手动在`.puppet`json里填字段测试，选区工具还没做）。
5. 第5步编辑器选区框——补齐调参体验。
6. 第6步billboard俯仰过渡——相对独立，最后做。

每步做完跑一次 `mvn compile` 确认无编译错误；核心渲染逻辑改完后用现有的`launch-puppet-editor.bat`手动跑一遍编辑器肉眼验证效果（这类jME交互渲染没有自动化测试覆盖，跟项目现状一致，需要你在编辑器里实际转动视角检查纸带切换和billboard过渡是否符合预期）。
