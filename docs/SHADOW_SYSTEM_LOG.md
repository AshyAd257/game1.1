# 阴影系统开发日志

## 文档信息
- **创建日期**: 2026-05-21
- **系统版本**: Hecate Engine v0.1.0
- **jMonkeyEngine**: 3.5.2
- **开发者**: Claude & User

---

## 1. 今日完成的修改 (2026-05-21)

### 1.1 光源对齐阴影系统 (Light-Aligned Shadow System)

#### 问题背景
- 原始的billboard阴影在旋转时会"散开"，各个部件的阴影方向不一致
- 需要一个统一的、朝向光源的"影子替身"来投射整体阴影

#### 实现方案
在 `PuppetRenderer.java` 中为每个puppet部件创建对应的shadowCaster几何体：

```java
// 核心代码位置: PuppetRenderer.java 行882-920
private void createShadowCaster(PuppetPartRenderer part, Geometry partGeometry) {
    // 1. 克隆几何体形状作为阴影投射器
    Mesh clonedMesh = partGeometry.getMesh().deepClone();
    Geometry shadowCaster = new Geometry("ShadowCaster_" + part.getPartName(), clonedMesh);

    // 2. 设置材质为完全透明（不影响颜色和深度缓冲）
    Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
    mat.setColor("Color", new ColorRGBA(0, 0, 0, 0));
    mat.getAdditionalRenderState().setColorWrite(false);  // 不写入颜色
    mat.getAdditionalRenderState().setDepthWrite(false);  // 不写入深度
    shadowCaster.setMaterial(mat);

    // 3. 设置为仅投射阴影模式
    shadowCaster.setShadowMode(ShadowMode.Cast);

    // 4. 存储引用
    shadowCasters.put(part.getPartName(), shadowCaster);
    puppetNode.attachChild(shadowCaster);
}
```

**关键技术点**:
- `ColorWrite(false)` - 防止shadowCaster在屏幕上可见
- `DepthWrite(false)` - 防止shadowCaster遮挡puppet本体（修复"镜面效应"）
- `ShadowMode.Cast` - 只投射阴影，不接收阴影

---

### 1.2 光源方向追踪与反向旋转补偿

#### 问题背景
shadowCaster需要始终面向光源，以产生正确的阴影轮廓。

#### 实现代码 (PuppetRenderer.java 行669-858)

```java
/**
 * 更新光源对齐阴影系统
 * 让shadowCaster旋转朝向光源，产生统一的阴影效果
 */
private void updateLightAlignedShadows(float tpf) {
    // === 步骤1: 光源查找 ===
    if (cachedDirectionalLight == null || lightCacheTimer > LIGHT_CACHE_DURATION) {
        findPrimaryLight();
        lightCacheTimer = 0f;
    }
    lightCacheTimer += tpf;

    // === 步骤2: 计算光源Yaw角 ===
    Vector3f lightDir = cachedDirectionalLight.getDirection().normalize();
    float lightYaw = (float) Math.atan2(lightDir.x, lightDir.z);

    // === 步骤3: 反向旋转补偿 ===
    // shadowCaster需要抵消以下旋转:
    // - sceneNodeRotation: 场景节点本身的旋转
    // - manualRotationAngle: puppet手动旋转
    // - cameraYaw: 相机跟随

    float sceneNodeYaw = extractYawFromNode(puppetNode.getParent());
    float manualYaw = rotationAngle * FastMath.DEG_TO_RAD;
    float cameraYaw = 0f; // 如果有相机跟随系统，从这里读取

    float ghostRotationY = lightYaw - sceneNodeYaw - manualYaw - cameraYaw;

    Quaternion ghostRotation = new Quaternion();
    ghostRotation.fromAngleAxis(ghostRotationY, Vector3f.UNIT_Y);

    // === 步骤4: 应用旋转到所有shadowCaster ===
    for (Geometry shadowCaster : shadowCasters.values()) {
        shadowCaster.setLocalRotation(ghostRotation);

        // 统一Z轴深度，避免"散开"效果
        Vector3f localPos = shadowCaster.getLocalTranslation();
        localPos.z = 0f;
        shadowCaster.setLocalTranslation(localPos);
    }
}
```

**公式解释**:
```
ghostRotationY = lightYaw - sceneNodeYaw - manualYaw - cameraYaw
                 ↑          ↑              ↑            ↑
                 光源方向    场景节点旋转    角色手动旋转  相机跟随
```

---

### 1.3 Z轴统一修复

#### 问题
当shadowCaster旋转后，原本用于表现部件前后遮挡的Z值差异会暴露，导致阴影"散开"。

#### 解决方案 (PuppetRenderer.java 行789-794)
```java
// 【修复Z轴散开】统一所有shadowCaster的Z值为0，消除深度差异
// 当shadowCaster偏转角度时，原本用于表现遮挡关系的Z值差异会暴露出来
// 将Z值统一为0，让所有部件在同一平面上，避免"散开"效果
if (useLightAlignedShadow) {
    localPos.z = 0f;
}
```

---

### 1.4 已修复的问题

#### Bug #1: "镜面效应"
- **症状**: shadowCaster虽然不可见，但会遮挡puppet本体渲染
- **原因**: `ColorWrite(false)` 只禁用颜色写入，深度缓冲仍然写入
- **修复**: 添加 `DepthWrite(false)`
- **文件**: PuppetRenderer.java 行907-908

#### Bug #2: 地面阴影消失
- **症状**: 尝试分离阴影时，地面阴影消失
- **原因**: 将shadowCaster设为 `ShadowMode.Off`
- **修复**: 恢复为 `ShadowMode.Cast`
- **文件**: PuppetRenderer.java 行914

#### Bug #3: 椭圆形阴影轨迹（已回退）
- **症状**: 阴影距离脚部太远，且随相机旋转呈椭圆运动
- **原因**: 尝试用 `localPos.z = -2.0f` 推远shadowCaster，产生透视畸变
- **修复**: 回退该方案，恢复Z轴统一
- **文件**: PuppetRenderer.java 行789-794 (已回退)

---

## 2. 当前未解决的核心问题

### 问题描述
**shadowCaster会同时在地面和puppet身上投射阴影，但我们只希望它在地面上投射阴影。**

#### 期望效果
1. **第一部分**: shadowCaster → 只投射到地面（强阴影）
2. **第二部分**: puppet部件 → 彼此互相投射阴影（软阴影）

#### 当前状态
- shadowCaster同时投射到地面和puppet上
- puppet部件设为 `ShadowMode.Receive`，只接收不投射

#### 技术限制
jME3的 `DirectionalLightShadowRenderer` 不支持：
- 按对象过滤阴影接收器
- 阴影投射器-接收器配对
- 多个独立的阴影组

---

## 3. 未来规划与待实现功能

### 3.1 光源查找系统优化

#### 当前实现 (PuppetRenderer.java 行730-765)
```java
private void findPrimaryLight() {
    // 从场景根节点递归查找DirectionalLight
    Node rootNode = getRootNode();
    cachedDirectionalLight = searchForDirectionalLight(rootNode);
}

private DirectionalLight searchForDirectionalLight(Spatial spatial) {
    // 递归遍历场景图
    if (spatial instanceof Node) {
        Node node = (Node) spatial;

        // 检查当前节点的光源列表
        for (Light light : node.getLocalLightList()) {
            if (light instanceof DirectionalLight) {
                return (DirectionalLight) light;
            }
        }

        // 递归检查子节点
        for (Spatial child : node.getChildren()) {
            DirectionalLight found = searchForDirectionalLight(child);
            if (found != null) return found;
        }
    }
    return null;
}
```

#### 优化方向

**A. 缓存策略**
- ✅ 已实现: 每5秒重新查找一次光源（避免每帧遍历场景图）
- 🔲 待实现: 缓存光源Yaw角（太阳光方向通常不变，整局游戏可能只需计算一次）

```java
// 建议添加字段
private float cachedLightYaw = 0f;
private boolean lightYawDirty = true;
private static final float LIGHT_YAW_THRESHOLD = 0.01f; // 1度变化才更新

// 优化后的更新逻辑
private void updateLightYaw() {
    Vector3f lightDir = cachedDirectionalLight.getDirection().normalize();
    float newYaw = (float) Math.atan2(lightDir.x, lightDir.z);

    if (Math.abs(newYaw - cachedLightYaw) > LIGHT_YAW_THRESHOLD) {
        cachedLightYaw = newYaw;
        lightYawDirty = true;
    }
}
```

**B. 光源查找范围**
- 当前: 从根节点递归查找
- 风险: 大型场景可能有数百个节点
- 建议:
  - 限制递归深度（3-5层）
  - 或者从 `Main.java` 直接传入光源引用

---

### 3.2 多光源优先级系统

#### 场景需求
某些场景可能有多个方向光：
- 主太阳光（天空）
- 次级月光（夜晚）
- 区域定向光（洞穴入口）

#### 建议配置 (待实现)

```java
public enum LightPriorityMode {
    /**
     * 只使用最强的光源（默认）
     * 适用场景: 我的世界风格游戏
     * 理由: 太阳光压倒性强，火把等弱光源的阴影可忽略
     */
    PRIMARY_ONLY,

    /**
     * 自动压缩多光源为最强光
     * 适用场景: 室内/多光源密集场景
     * 理由: 多个方向光需要手动切换
     */
    AUTO_COMPRESS,

    /**
     * 使用所有光源（性能开销大）
     * 适用场景: 电影级渲染
     */
    ALL_LIGHTS
}

// 添加到PuppetRenderer字段
private LightPriorityMode lightPriorityMode = LightPriorityMode.PRIMARY_ONLY;
```

#### 多光源查找算法 (伪代码)

```java
private DirectionalLight findPrimaryLight(LightPriorityMode mode) {
    List<DirectionalLight> allLights = findAllDirectionalLights();

    switch (mode) {
        case PRIMARY_ONLY:
            // 按光源强度排序，返回最强的
            return allLights.stream()
                .max(Comparator.comparing(this::getLightIntensity))
                .orElse(null);

        case AUTO_COMPRESS:
            // 可以在这里实现光源融合逻辑
            // 例如：计算加权平均方向
            return compressLights(allLights);

        case ALL_LIGHTS:
            // 为每个光源创建独立shadowCaster
            // （需要重大架构改动）
            return null;
    }
}

private float getLightIntensity(DirectionalLight light) {
    ColorRGBA color = light.getColor();
    return (color.r + color.g + color.b) / 3f;
}
```

**性能建议**:
- **默认值**: `PRIMARY_ONLY`
- **理由**:
  - 我的世界风格游戏通常是强太阳光 + 若干弱点光源
  - 弱光源的阴影贡献视觉上可忽略
  - 玩家几乎注意不到火把旁边没有第二个影子
- **例外场景**: 室内多灯场景手动切换到 `AUTO_COMPRESS`

---

### 3.3 角色基础旋转补偿

#### 当前公式
```java
float ghostRotationY = cachedLightYaw
                     - sceneNodeRotation
                     - manualRotationAngle
                     - cameraYaw;
```

#### 重要说明

**动画旋转不需要补偿**
- 原因: 动画旋转作用在子骨骼上，不影响根节点朝向
- shadowCaster和puppet共享动画但根节点独立，动画会自动同步
- 真正需要补偿的只是根节点的世界空间Yaw

**sceneNodeRotation是最容易漏掉的**
- 场景: 角色所在的场景节点本身有旋转（例如跟随地形倾斜）
- 后果: 不计入会导致shadowCaster偏转
- 解决: 必须从父节点提取Yaw

#### 实现细节 (PuppetRenderer.java 行712-728)

```java
/**
 * 从节点提取Yaw角（绕Y轴旋转）
 */
private float extractYawFromNode(Spatial node) {
    if (node == null) return 0f;

    Quaternion rotation = node.getWorldRotation();
    float[] angles = new float[3];
    rotation.toAngles(angles);

    return angles[1]; // Yaw is the second component (Y-axis rotation)
}
```

**注意事项**:
- 使用 `getWorldRotation()` 而不是 `getLocalRotation()`
- 如果有多层父节点嵌套，世界旋转会累积

---

### 3.4 光源对齐阴影开关

#### 功能需求
用户应该能够在游戏中关闭光源对齐阴影，回到简单的billboard阴影。

#### 预留接口设计

```java
// 在 PuppetRenderer.java 中添加
private boolean useLightAlignedShadow = true; // 默认启用

/**
 * 设置是否启用光源对齐阴影系统
 * @param enabled true=启用统一阴影, false=使用传统billboard阴影
 */
public void setLightAlignedShadowEnabled(boolean enabled) {
    this.useLightAlignedShadow = enabled;

    if (!enabled) {
        // 禁用时，隐藏所有shadowCaster
        for (Geometry shadowCaster : shadowCasters.values()) {
            shadowCaster.setCullHint(Spatial.CullHint.Always);
        }
    } else {
        // 启用时，恢复shadowCaster
        for (Geometry shadowCaster : shadowCasters.values()) {
            shadowCaster.setCullHint(Spatial.CullHint.Dynamic);
        }
    }
}

public boolean isLightAlignedShadowEnabled() {
    return useLightAlignedShadow;
}
```

#### 游戏UI集成（占位）

```java
// 未来在设置菜单中添加选项
// 位置: src/main/java/com/Hecate/ui/SettingsMenu.java (待创建)

public class GraphicsSettings {
    // 阴影质量
    public enum ShadowQuality {
        OFF,           // 无阴影
        BILLBOARD,     // 传统billboard阴影（快）
        LIGHT_ALIGNED, // 光源对齐阴影（推荐）
        FULL_3D        // 完整3D阴影（慢）
    }

    private ShadowQuality shadowQuality = ShadowQuality.LIGHT_ALIGNED;

    // 当用户更改设置时调用
    public void applyShadowSetting(PuppetRenderer renderer) {
        switch (shadowQuality) {
            case OFF:
                renderer.setShadowMode(ShadowMode.Off);
                break;
            case BILLBOARD:
                renderer.setLightAlignedShadowEnabled(false);
                break;
            case LIGHT_ALIGNED:
                renderer.setLightAlignedShadowEnabled(true);
                break;
            case FULL_3D:
                // 未来功能：启用完整体积阴影
                break;
        }
    }
}
```

**UI控件建议**:
- 类型: 下拉菜单 (Dropdown)
- 标签: "阴影质量" / "Shadow Quality"
- 选项:
  - 关闭（性能最优）
  - 简单阴影（billboard）
  - 光源对齐（推荐）✓
  - 高质量（实验性）

---

## 4. 阴影分离方案探索

### 核心问题回顾
**shadowCaster同时在地面和puppet上投射阴影，但我们只希望地面有阴影。**

### 方案对比

#### 方案A: 双阴影渲染器 + 场景图分离

**原理**:
- 创建两个独立的 `DirectionalLightShadowRenderer`
- 第一个渲染器: 只处理shadowCaster几何体（投射到地面）
- 第二个渲染器: 只处理puppet部件（部件互相投射）

**实现难点**:
```java
// Main.java 伪代码
DirectionalLightShadowRenderer groundShadowRenderer;
DirectionalLightShadowRenderer partShadowRenderer;

// 问题: jME3的ShadowRenderer会处理整个ViewPort的所有几何体
// 无法通过API限制"只处理某些对象"
```

**技术限制**: ❌
- jME3不支持按对象过滤阴影
- 两个渲染器会重复处理所有几何体
- 性能开销翻倍且无法达到目标

---

#### 方案B: 自定义阴影材质 (Shader修改)

**原理**:
- 为puppet部件创建自定义材质
- 在fragment shader中检测阴影是否来自shadowCaster
- 如果是，将阴影强度设为0

**实现步骤**:
1. 复制 `Common/MatDefs/Light/Lighting.j3md` 为 `Custom/PuppetLighting.j3md`
2. 修改fragment shader:

```glsl
// Custom/Shaders/PuppetLighting.frag
uniform sampler2D m_ShadowMap0;
uniform vec4 m_ShadowCasterBounds; // 传入shadowCaster的AABB范围

void main() {
    // ... 原有光照计算 ...

    // 阴影采样
    float shadow = textureProj(m_ShadowMap0, shadowCoord);

    // 检测是否在shadowCaster范围内
    if (isInShadowCasterBounds(worldPos, m_ShadowCasterBounds)) {
        shadow = 0.0; // 忽略shadowCaster的阴影
    }

    // 应用阴影
    finalColor *= (1.0 - shadow * shadowIntensity);
}
```

**技术难度**: ⚠️ 中等
- 需要深入了解jME3的阴影管线
- 需要实时传递shadowCaster的边界信息
- 可能影响其他光照计算

---

#### 方案C: PlanarShadow替代shadowCaster（推荐）★

**原理**:
- shadowCaster不再使用DirectionalLightShadowRenderer
- 改用简单的平面投影阴影（直接投影到Y=0平面）
- puppet部件继续使用正常的阴影渲染器

**实现代码框架**:

```java
// 新增类: PlanarShadow.java
public class PlanarShadow {
    private Geometry shadowQuad;
    private Vector3f groundNormal = new Vector3f(0, 1, 0); // 地面法线
    private float groundY = 0f; // 地面高度

    public void update(Vector3f puppetPos, Vector3f lightDir) {
        // 计算投影矩阵
        Matrix4f projectionMatrix = createShadowProjectionMatrix(
            groundNormal, groundY, lightDir
        );

        // 应用投影到shadowQuad
        shadowQuad.setLocalTransform(projectionMatrix);
    }

    private Matrix4f createShadowProjectionMatrix(
        Vector3f planeNormal, float planeD, Vector3f lightDir
    ) {
        // 平面投影公式: P' = P - (P·N + D) / (L·N) * L
        // 其中P是顶点，N是平面法线，D是平面距离，L是光方向

        float dot = planeNormal.dot(lightDir);

        // 构建4x4投影矩阵
        Matrix4f mat = new Matrix4f();
        mat.m00 = dot - lightDir.x * planeNormal.x;
        mat.m01 = -lightDir.x * planeNormal.y;
        mat.m02 = -lightDir.x * planeNormal.z;
        mat.m03 = -lightDir.x * planeD;

        mat.m10 = -lightDir.y * planeNormal.x;
        mat.m11 = dot - lightDir.y * planeNormal.y;
        mat.m12 = -lightDir.y * planeNormal.z;
        mat.m13 = -lightDir.y * planeD;

        mat.m20 = -lightDir.z * planeNormal.x;
        mat.m21 = -lightDir.z * planeNormal.y;
        mat.m22 = dot - lightDir.z * planeNormal.z;
        mat.m23 = -lightDir.z * planeD;

        mat.m30 = 0;
        mat.m31 = 0;
        mat.m32 = 0;
        mat.m33 = dot;

        return mat;
    }
}
```

**优点**:
- ✅ 完全绕过阴影渲染器，shadowCaster不参与阴影计算
- ✅ 地面阴影100%可控
- ✅ 性能优秀（无需额外阴影贴图）

**缺点**:
- ⚠️ 平面投影阴影不支持阴影软化
- ⚠️ 无法投射到不平整的地形
- ⚠️ 边缘可能产生锯齿

**适用场景**:
- 平坦地面（我的世界风格）
- 不需要极致阴影质量的游戏

---

#### 方案D: 节点分层 + 自定义RenderQueue

**原理**:
- 将shadowCaster放入独立的RenderQueue.Bucket
- 为地面创建专用ViewPort
- 第一个ViewPort只渲染shadowCaster
- 第二个ViewPort只渲染puppet部件

**实现难度**: ⚠️⚠️ 高
- 需要多个ViewPort和Camera
- 需要深入理解jME3的渲染管线
- 可能引入新的深度缓冲问题

---

### 推荐方案

**短期方案（1-2天）**: 方案C - PlanarShadow
- 快速实现
- 解决80%的视觉问题
- 性能友好

**长期方案（1-2周）**: 方案B - 自定义Shader
- 完全控制阴影效果
- 支持高质量渲染
- 需要shader编程经验

---

## 5. 技术债务与注意事项

### 5.1 FreeBonePhysics与shadowCaster的交互

**当前状态**:
- `FreeBonePhysics` 会修改骨骼的 `localPosition`
- shadowCaster通过 `bone.setTargetLocalPosition()` 保存非物理位置
- 地面阴影使用targetPosition，避免物理摆动影响阴影

**代码位置**: FreeBonePhysics.java 行73-82

```java
// 保存目标局部位置（用于shadowCaster，避免物理摆动可见）
Vector3f targetLocalPos = targetWorldPos.subtract(parentWorldPos);
if (parentWorldRot.norm() > 0.0001f) {
    targetLocalPos = parentWorldRot.inverse().mult(targetLocalPos);
}
if (parentWorldScale.x != 0 && parentWorldScale.y != 0 && parentWorldScale.z != 0) {
    targetLocalPos = targetLocalPos.divide(parentWorldScale);
}
bone.setTargetLocalPosition(targetLocalPos);
```

**注意**:
- 如果未来删除物理系统，需要移除 `targetLocalPosition` 逻辑
- puppet部件应该直接使用 `localPosition`

---

### 5.2 性能监控建议

```java
// 添加性能统计字段
private long shadowUpdateTime = 0L;
private int shadowUpdateCount = 0;

private void updateLightAlignedShadows(float tpf) {
    long startTime = System.nanoTime();

    // ... 阴影更新逻辑 ...

    long endTime = System.nanoTime();
    shadowUpdateTime += (endTime - startTime);
    shadowUpdateCount++;

    // 每秒输出一次性能报告
    if (shadowUpdateCount >= 60) {
        float avgTime = shadowUpdateTime / (float) shadowUpdateCount / 1_000_000f;
        System.out.println("Shadow Update Avg: " + avgTime + "ms");
        shadowUpdateTime = 0L;
        shadowUpdateCount = 0;
    }
}
```

**性能目标**:
- shadowCaster更新: < 0.5ms per frame
- 光源查找（缓存）: < 0.1ms per 5 seconds

---

## 6. 相关文件清单

### 核心文件
1. **PuppetRenderer.java**
   - 路径: `src/main/java/com/Hecate/puppet/core/PuppetRenderer.java`
   - 行数: 1200+ 行
   - 职责: 管理puppet的整体渲染、shadowCaster创建、光源对齐逻辑

2. **PuppetPartRenderer.java**
   - 路径: `src/main/java/com/Hecate/puppet/core/PuppetPartRenderer.java`
   - 行数: 400+ 行
   - 职责: 单个puppet部件的billboard渲染

3. **FreeBonePhysics.java**
   - 路径: `src/main/java/com/Hecate/puppet/core/FreeBonePhysics.java`
   - 行数: 250 行
   - 职责: 骨骼物理模拟、targetLocalPosition保存

4. **Main.java**
   - 路径: `src/main/java/com/Hecate/Main.java`
   - 行数: 800+ 行
   - 职责: 游戏主循环、阴影渲染器初始化

### 待创建文件
5. **PlanarShadow.java** (方案C)
   - 路径: `src/main/java/com/Hecate/puppet/PlanarShadow.java`
   - 职责: 平面投影阴影实现

6. **GraphicsSettings.java** (UI配置)
   - 路径: `src/main/java/com/Hecate/ui/GraphicsSettings.java`
   - 职责: 图形设置菜单

---

## 7. 测试检查清单

### 功能测试
- [ ] shadowCaster在常规渲染中不可见
- [ ] shadowCaster不会遮挡puppet本体
- [ ] 地面阴影正确投射
- [ ] 阴影跟随光源方向旋转
- [ ] 相机旋转时阴影方向保持稳定
- [ ] 角色手动旋转时阴影不偏移
- [ ] 多个puppet同时存在时性能正常

### 边缘情况测试
- [ ] 场景图有多层父节点时旋转补偿正确
- [ ] 没有光源时不崩溃（优雅降级）
- [ ] 光源方向突变时阴影平滑过渡
- [ ] 动画旋转不影响shadowCaster方向
- [ ] 物理骨骼摆动不影响地面阴影

### 性能测试
- [ ] 10个puppet时帧率 > 60fps
- [ ] 100个puppet时帧率 > 30fps
- [ ] 光源查找缓存正常工作（不每帧遍历）

---

## 8. 参考资料

### jMonkeyEngine文档
- [Shadow Rendering](https://wiki.jmonkeyengine.org/docs/3.4/core/light/light_and_shadow.html)
- [RenderState API](https://javadoc.jmonkeyengine.org/v3.5.2-stable/com/jme3/material/RenderState.html)
- [ShadowMode Enum](https://javadoc.jmonkeyengine.org/v3.5.2-stable/com/jme3/renderer/queue/RenderQueue.ShadowMode.html)

### 相关技术
- [Planar Shadow Projection (PDF)](http://developer.download.nvidia.com/books/gpugems/gamedev.net/GpuGems.pdf) - Chapter 9
- [Shadow Mapping in OpenGL](https://learnopengl.com/Advanced-Lighting/Shadows/Shadow-Mapping)

---

## 9. 变更历史

| 日期 | 版本 | 作者 | 变更内容 |
|------|------|------|----------|
| 2026-05-21 | 1.0.0 | Claude + User | 初始版本，实现光源对齐阴影系统 |
| 2026-05-21 | 1.0.1 | Claude | 修复镜面效应（添加DepthWrite false） |
| 2026-05-21 | 1.0.2 | Claude | 回退Y=0地面定位方案 |

---

## 10. 待办事项 (按优先级排序)

### 高优先级
- [ ] **实现阴影分离方案**（推荐方案C: PlanarShadow）
- [ ] **添加光源对齐开关接口**（预留UI集成）
- [ ] **优化光源Yaw缓存**（减少三角函数计算）

### 中优先级
- [ ] 实现多光源优先级系统
- [ ] 添加性能监控统计
- [ ] 编写单元测试

### 低优先级
- [ ] 创建图形设置UI菜单
- [ ] 实现阴影质量级别配置
- [ ] 编写用户使用文档

---

## 11. 联系与反馈

如需讨论阴影系统改进或报告问题，请：
1. 查看本文档的"未来规划"章节
2. 检查"待办事项"清单
3. 参考"方案对比"选择合适的技术路线

**文档维护**: 每次重大修改后更新本文档的"变更历史"表格。

---

**文档结束**
