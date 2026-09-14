# Puppet 部件绑定到 3D 骨骼系统 - 完整指南

## 概述

已创建完整的系统，将 2D puppet 部件（面部、身体、脖子）绑定到 3D 人物模型的骨骼上。puppet 部件会跟随 3D 骨骼的位置和旋转，实现完美同步。

## 系统架构

### 核心类

1. **PuppetBoneAttachment.java**
   - 单个 puppet 部件到 3D 骨骼的绑定
   - 每帧计算骨骼的世界坐标和旋转
   - 将 puppet 部件同步到骨骼位置
   - 支持跟随旋转或保持朝向摄像机

2. **PuppetBoneAttachmentManager.java**
   - 管理多个 puppet 部件的绑定
   - 加载 puppet 配置文件
   - 根据映射表将部件绑定到骨骼
   - 统一更新所有绑定

3. **SkeletalPlayerController.java**（已修改）
   - 集成 puppet 绑定系统
   - 初始化时创建绑定
   - 每帧更新绑定
   - 清理时释放资源

## 工作流程

### 1. 查看模型骨骼结构

运行工具查看 3D 模型的骨骼名称：

```bash
cd C:\Users\29232\OneDrive\Desktop\game1(1)
list_model_bones.bat
```

输出示例：
```
骨骼列表（按层级显示）：
├─ Armature (索引: 0, 子骨骼: 1)
  ├─ Spine (索引: 1, 子骨骼: 2)
    ├─ Neck (索引: 2, 子骨骼: 1)
      ├─ Head (索引: 3, 子骨骼: 0)
    ├─ ArmLeft (索引: 4, 子骨骼: 1)
```

### 2. 配置骨骼映射

在 `SkeletalPlayerController.initializePuppetAttachments()` 中配置映射：

```java
java.util.Map<String, String> boneMapping = new java.util.HashMap<>();

// Puppet 部件名 -> 3D 骨骼名
boneMapping.put("Head", "Head");     // 面部 -> 头部骨骼
boneMapping.put("Neck", "Neck");     // 脖子 -> 脖子骨骼
boneMapping.put("Body", "Spine");    // 身体 -> 脊柱骨骼
```

**注意**：骨骼名称必须与模型中的实际名称完全匹配（区分大小写）！

### 3. 调整缩放比例

在 `initializePuppetAttachments()` 中调整 `puppetScale`：

```java
float puppetScale = 2.0f;  // 增大数值放大部件，减小数值缩小部件
```

### 4. 运行游戏测试

```bash
run_game_with_puppet.bat
```

## 关键特性

### ✅ 位置同步
- puppet 部件精确跟随 3D 骨骼的世界坐标
- 考虑了角色节点的缩放和旋转
- 支持局部偏移（可在创建绑定时指定）

### ✅ 旋转同步
- puppet 部件跟随骨骼的旋转
- 面部会随着头部骨骼转动
- 身体会随着脊柱骨骼弯曲

### ✅ 动画同步
- 3D 骨骼动画播放时，puppet 部件自动跟随
- 支持所有动画：待机、行走、跳跃等

### ✅ Billboard 模式
- 可选择是否让部件始终朝向摄像机
- `followRotation = true`: 跟随骨骼旋转
- `followRotation = false`: 只跟随位置，朝向摄像机

## 常见问题

### Q1: Puppet 部件不显示

**检查清单**：
1. 控制台是否有 "Puppet 绑定完成" 的消息？
2. 控制台是否有骨骼映射错误？
3. 骨骼名称是否正确（运行 `list_model_bones.bat` 确认）

**解决方案**：
```java
// 如果找不到骨骼，会输出：
// [PuppetBoneAttachment] 错误：未找到骨骼 'Spine'

// 检查模型中的实际名称，可能是：
boneMapping.put("Body", "Spine.001");  // Blender 可能添加数字后缀
boneMapping.put("Body", "Chest");      // 或使用其他名称
boneMapping.put("Body", "spine");      // 小写
```

### Q2: Puppet 部件太大或太小

**解决方案**：
```java
// 在 SkeletalPlayerController.initializePuppetAttachments() 中调整
float puppetScale = 1.0f;   // 太小了
float puppetScale = 3.0f;   // 试试这个
float puppetScale = 5.0f;   // 更大
```

### Q3: Puppet 部件位置偏移

**解决方案**：
在 `PuppetBoneAttachmentManager.loadAndAttachPuppet()` 中添加偏移：

```java
// 创建绑定时指定偏移
Vector3f offset = new Vector3f(0, 0.5f, 0);  // 向上偏移 0.5 单位
PuppetBoneAttachment attachment = new PuppetBoneAttachment(
    targetJoint, partNode, partRenderer, offset, scale, followRotation
);
```

### Q4: Puppet 部件朝向不对

**问题**：部件跟随骨骼旋转了，但朝向反了或歪了

**解决方案**：
1. 检查 Blender 中骨骼的朝向是否正确
2. 调整 puppet 贴图的旋转校准：
   ```java
   // 在木偶编辑器中打开 defaultChara1.puppet
   // 选择 Head 部件
   // 调整 "校准偏移" 参数
   ```

### Q5: 部件跟随旋转导致显示错误

**解决方案**：
改为 Billboard 模式（不跟随旋转）：

```java
// 在 PuppetBoneAttachmentManager.loadAndAttachPuppet() 中
boolean followRotation = false;  // 改为 false
```

## 调试技巧

### 1. 查看控制台输出

```
[PuppetBoneAttachment] 找到骨架，骨骼数量: 10
[PuppetBoneAttachment] 骨骼 0: Armature
[PuppetBoneAttachment] 骨骼 1: Spine
[PuppetBoneAttachment] 骨骼 2: Neck
[PuppetBoneAttachment] 骨骼 3: Head
[PuppetBoneAttachment] 绑定成功: Head -> Head
[PuppetBoneAttachment] 绑定成功: Neck -> Neck
[PuppetBoneAttachment] 绑定成功: Body -> Spine
[PuppetBoneAttachment] 所有部件绑定完成，总数: 3
```

### 2. 临时禁用 Puppet 部件

```java
skeletalPlayerController.setPuppetVisible(false);  // 隐藏
skeletalPlayerController.setPuppetVisible(true);   // 显示
```

### 3. 查看骨骼变换

在 `PuppetBoneAttachment.update()` 中添加调试输出：

```java
System.out.println("骨骼 " + targetJoint.getName() +
    " 位置: " + boneWorldPos +
    " 旋转: " + boneWorldRot);
```

## 下一步优化

### 1. 添加更多部件
- 手臂、腿部、武器等
- 在 `defaultChara1.puppet` 中添加更多骨骼
- 在 `boneMapping` 中添加映射

### 2. 动态换装
- 运行时切换不同的 puppet 配置
- 加载不同的贴图（表情、服装）

### 3. 细节调整
- 为每个部件单独设置偏移
- 调整部件的渲染优先级
- 添加部件之间的父子关系

## 技术细节

### 坐标变换链

```
骨骼本地坐标 (Joint.getLocalTransform())
    ↓ 应用父骨骼变换
骨骼模型坐标 (Joint.getModelTransform())
    ↓ 应用角色节点缩放
中间坐标
    ↓ 应用角色节点旋转
    ↓ 应用角色节点位置
世界坐标 (最终 puppet 部件位置)
```

### 旋转同步原理

1. 获取骨骼的世界旋转（Quaternion）
2. 应用到 puppet 部件节点
3. puppet 渲染器根据旋转选择正确的贴图帧（旋转条模式）

### Billboard 模式

- `DISABLED`: 部件跟随骨骼旋转（用于绑定）
- `UNIFIED`: 整个 puppet 朝向摄像机（不适合绑定）
- `INDEPENDENT`: 每个部件独立朝向（不适合绑定）

## 文件清单

| 文件 | 描述 |
|------|------|
| `PuppetBoneAttachment.java` | 单个部件绑定逻辑 |
| `PuppetBoneAttachmentManager.java` | 绑定管理器 |
| `SkeletalPlayerController.java` | 集成绑定系统 |
| `ListModelBones.java` | 骨骼查看工具 |
| `list_model_bones.bat` | 骨骼查看脚本 |
| `defaultChara1.puppet` | Puppet 配置文件 |

## 总结

现在你有了一个完整的系统，可以将 2D puppet 部件绑定到 3D 骨骼上。关键步骤：

1. ✅ 查看模型骨骼名称
2. ✅ 配置正确的映射
3. ✅ 调整缩放比例
4. ✅ 运行游戏测试

Puppet 部件会自动跟随 3D 骨骼移动和旋转，实现完美的同步效果！
