# 摄像机系统重构设计

## 设计原则

### 1. 单一职责
- **ThirdPersonCamera**: 纯粹的摄像机数学和位置计算
- **CameraInputHandler**: 处理输入（鼠标、键盘）
- **PlayerController**: 只负责玩家状态，委托摄像机控制

### 2. 统一角度系统
- **全部使用弧度**
- **统一范围**: `[0, 2π)` for yaw, `[-π/2, π/2]` for pitch
- **避免度数/弧度混用**

### 3. 清晰的状态管理
- 使用状态机模式
- 明确的状态转换条件
- 无隐式状态

## 架构设计

```
┌─────────────────────────────────────┐
│       PlayerController              │
│  - 玩家位置/朝向                      │
│  - 移动逻辑                          │
└──────────────┬──────────────────────┘
               │ delegates
               ↓
┌─────────────────────────────────────┐
│    ThirdPersonCamera                │
│  - 摄像机位置计算                     │
│  - 角度管理 (yaw/pitch)              │
│  - 距离管理                          │
│  - 碰撞检测                          │
└──────────────┬──────────────────────┘
               │ uses
               ↓
┌─────────────────────────────────────┐
│    CameraInputHandler               │
│  - 鼠标输入                          │
│  - 键盘输入 (+/- zoom, R reset)     │
└─────────────────────────────────────┘
```

## 核心类设计

### ThirdPersonCamera

```java
public class ThirdPersonCamera {
    // 状态
    private float yaw;           // 水平角度 [0, 2π)
    private float pitch;         // 垂直角度 [-π/2, π/2]
    private float distance;      // 摄像机距离
    private Vector3f target;     // 跟随目标位置
    
    // 配置
    private CameraConfig config; // 距离范围、灵敏度等
    
    // 公开接口
    public void setTarget(Vector3f target);
    public void rotate(float deltaYaw, float deltaPitch);
    public void zoom(float delta);
    public void reset();
    public void update(Camera jmeCamera);
    
    // 内部计算
    private Vector3f calculatePosition();
    private Vector3f calculateLookAt();
}
```

### CameraConfig (配置对象)

```java
public class CameraConfig {
    public float minDistance = 2.0f;
    public float maxDistance = 15.0f;
    public float defaultDistance = 8.0f;
    public float minPitch = -80f * DEG_TO_RAD;
    public float maxPitch = 60f * DEG_TO_RAD;
    public float mouseSensitivity = 0.005f;
    public float zoomSpeed = 3.0f;
    public float lookAtHeightOffset = 1.0f;
    public float lookAtRightOffset = 0.525f;
}
```

### CameraInputHandler

```java
public class CameraInputHandler {
    private ThirdPersonCamera camera;
    
    public void handleMouseMove(float deltaX, float deltaY);
    public void handleZoomIn(float tpf);
    public void handleZoomOut(float tpf);
    public void handleReset();
}
```

## 移除的混乱功能

### ❌ 删除
1. **自动对齐**: 按WASD自动转到背后
2. **cameraBaseAngle**: 多余的第三套角度系统
3. **isAligningCamera**: 平滑过渡状态
4. **playerFacing与摄像机耦合**: 移动中鼠标改playerFacing

### ✅ 保留
1. **R键重置**: 简化为直接设置目标角度/距离
2. **+/- 缩放**: 保持按住连续缩放
3. **角色朝向**: 独立于摄像机，由移动方向决定

## 角色朝向策略

### 新的分离逻辑

```
摄像机角度 (yaw/pitch) - 玩家可自由旋转360度
    ↓ 完全独立
玩家朝向 (playerFacing) - 由最后移动方向决定
```

**规则**：
- 玩家朝向 = 最后一次移动的方向
- 静止时保持上次朝向
- 摄像机可以自由环绕，不影响角色朝向
- 移动时根据WASD计算世界方向，不再依赖摄像机

## 数学统一

### 坐标系定义
- **+X**: 东
- **+Z**: 北  
- **+Y**: 上

### 角度定义
- **yaw = 0**: 摄像机在目标正北 (+Z)
- **yaw = π/2**: 摄像机在目标正东 (+X)
- **yaw = π**: 摄像机在目标正南 (-Z)
- **pitch = 0**: 水平
- **pitch > 0**: 俯视
- **pitch < 0**: 仰视

### 位置计算公式

```java
camX = target.x + distance * sin(yaw) * cos(pitch)
camY = target.y + heightOffset + distance * sin(pitch)
camZ = target.z + distance * cos(yaw) * cos(pitch)
```

## 测试计划

1. ✅ 摄像机可以360度水平旋转
2. ✅ 垂直角度限制在 [-80°, 60°]
3. ✅ +/- 键缩放平滑
4. ✅ R键重置到默认位置
5. ✅ 角色移动时朝向正确
6. ✅ 静止时摄像机可以自由环绕
7. ✅ 窗口失焦/恢复不影响光标状态

## 迁移步骤

1. 创建新的 ThirdPersonCamera 类
2. 创建 CameraConfig 配置类
3. 创建 CameraInputHandler 输入处理类
4. 在 PlayerController 中集成新系统
5. 删除旧的摄像机逻辑
6. 删除死代码（CameraDirectionDetector、2D精灵）
7. 测试验证
