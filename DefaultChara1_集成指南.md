# DefaultChara1 人物模型集成指南

## 概述
已创建完整的 puppet 人物模型 `defaultChara1.puppet`，包含面部、身体和脖子三个部件，使用旋转条状贴图实现 360° 视角。

## 文件结构

```
src/main/resources/puppets/defaultChara1/
├── defaultChara1.puppet          # 主配置文件（新建）
├── face/
│   ├── deafultface1.png         # 面部贴图（旋转条）
│   ├── deafultface2.png
│   ├── deafultface3.png
│   └── deafultface4.png
├── body/
│   ├── body1.png                # 身体贴图（旋转条）
│   ├── body2.png
│   └── body3.png
└── neck/
    └── neck1.png                # 脖子贴图
```

## 骨骼层级结构

```
Body (根骨骼)
  └── Neck (脖子)
      └── Head (头部/面部)
```

- **Body**: Y=0.0, 使用 body1.png (8x20px 帧，16帧旋转条)
- **Neck**: Y=0.4 (相对 Body), 使用 neck1.png
- **Head**: Y=0.25 (相对 Neck), 使用 deafultface1.png (12x13px 帧，16帧旋转条)

## 测试方法

### 方法1: 使用测试程序
```bash
# 编译并运行测试程序
javac -cp "target/classes;..." src/main/java/com/Hecate/puppet/TestDefaultChara.java
java -cp "target/classes;..." com.Hecate.puppet.TestDefaultChara
```

### 方法2: 在木偶编辑器中打开
1. 启动木偶编辑器
2. 点击"加载木偶"按钮
3. 选择 `puppets/defaultChara1/defaultChara1.puppet`
4. 使用鼠标旋转视角，查看 360° 旋转效果

## 集成到游戏玩家

### 选项A: 替换现有的 PuppetPlayerController

修改 `PuppetPlayerController.java` 第64行：
```java
// 修改前
private static final String PUPPET_PATH = "puppets/successv5.puppet";

// 修改后
private static final String PUPPET_PATH = "puppets/defaultChara1/defaultChara1.puppet";
```

### 选项B: 创建新的人物类

创建 `DefaultCharaPlayer.java`：
```java
public class DefaultCharaPlayer extends PuppetPlayerController {
    public DefaultCharaPlayer(SimpleApplication app, Vector3f startPosition) {
        super(app, startPosition);
    }
    
    @Override
    protected String getPuppetPath() {
        return "puppets/defaultChara1/defaultChara1.puppet";
    }
}
```

## 贴图配置说明

### 旋转条状贴图参数

**面部 (Head)**:
- 贴图文件: `deafultface1.png`
- 单帧尺寸: 12x13 像素
- 总帧数: 16 帧
- 校准偏移: 12 像素 (stripCalibrationOffsetPx)
- 显示尺寸: 0.375 x 0.40625 单位

**身体 (Body)**:
- 贴图文件: `body1.png`
- 单帧尺寸: 8x20 像素
- 总帧数: 16 帧
- 校准偏移: 14 像素
- 显示尺寸: 0.25 x 0.625 单位

### 如何切换不同表情/服装

修改 `defaultChara1.puppet` 文件中的贴图路径：

```json
// 切换面部表情
"stripTexturePath": "puppets/defaultChara1/face/deafultface2.png"

// 切换身体服装
"stripTexturePath": "puppets/defaultChara1/body/body2.png"
```

## 常见问题

### Q1: 贴图路径错误 (括号问题已修复)
**问题**: 路径包含括号 `game1(1)` 导致加载失败
**解决**: 已在 `PuppetEditorApp.convertToResourcePath()` 中修复，带特殊字符的路径会使用 BufferedImage 直接加载

### Q2: 人物显示太大或太小
**解决**: 调整 puppetNode 的缩放：
```java
puppetNode.setLocalScale(0.3f);  // 缩小到 30%
```

### Q3: 旋转条贴图不同步
**解决**: 调整 `stripCalibrationOffsetPx` 参数：
- 在木偶编辑器中打开人物
- 选择 Head 或 Body 部件
- 点击"旋转条:开"按钮
- 调整校准偏移滑条，使当前视角对准正确的帧

### Q4: 部件渲染顺序错误（遮挡问题）
**解决**: 调整 `priority` 值（数值越大越在前面）：
- Body: priority = 0
- Neck: priority = 1
- Head: priority = 2

## 下一步优化

1. **添加手臂和腿部**
   - 创建 ArmLeft, ArmRight, LegLeft, LegRight 骨骼
   - 连接到 Body，设置合适的父子关系

2. **添加动画**
   - 创建行走动画 (walk.anim)
   - 创建跳跃动画 (jump.anim)
   - 使用 AnimationPlayer 播放

3. **多套皮肤系统**
   - 在运行时切换不同的贴图路径
   - 实现换装系统

## 技术细节

### Billboard 模式
- **UNIFIED**: 整个人物像纸片人一样朝向摄像机（推荐）
- **DISABLED**: 部件保持固定朝向（适合 3D 立体模型）
- **INDEPENDENT**: 每个部件独立朝向（不推荐）

### 旋转条工作原理
旋转条贴图是一张横向排列的精灵图，包含角色从不同角度看的帧：
1. 引擎根据相机角度计算应该显示第几帧
2. 通过 UV 坐标偏移选取对应帧
3. 实现平滑的 360° 旋转效果

帧序列示例（16帧，每帧22.5°）：
```
[背面] [背左] [左侧] [左前] [正面] [右前] [右侧] [右后] ... (16帧)
  0°    22.5°   45°    67.5°   90°    112.5°  135°   157.5°
```

## 参考资料

- 木偶编辑器使用: `PUPPET_EDITOR_TUTORIAL.md`
- 旋转条贴图制作: `PUPPET_ROTATION_STRIP_PLAN.md`
- 路径问题修复: `贴图路径问题分析.md`
