# Billboard模式使用指南

## 问题说明

在木偶编辑器中制作立体图形(如立方体)时,如果使用默认的Billboard渲染模式,旋转视角时各个部件会**独立旋转**来面对摄像机,导致立体结构散架,呈现出诡异的旋转效果。

## 解决方案

### Billboard渲染模式介绍

木偶编辑器现在支持三种Billboard渲染模式:

#### 1. **DISABLED (禁用Billboard)** - 适合3D立体模型
- **效果**: 部件保持固定的相对位置和朝向
- **用途**: 制作立方体、金字塔等3D立体图形
- **特点**: 旋转视角时,整个模型像真实3D物体一样旋转

#### 2. **UNIFIED (统一Billboard)** - 适合2D纸片人(默认)
- **效果**: 整个木偶像纸人一样整体朝向摄像机
- **用途**: 传统2D精灵动画角色
- **特点**: 所有部件使用统一旋转,保持相对位置关系

#### 3. **INDEPENDENT (独立Billboard)** - 不推荐
- **效果**: 每个部件独立朝向摄像机
- **用途**: 特殊效果(通常不推荐使用)
- **特点**: 会导致立体结构散架,部件各自旋转

## 使用方法

### 方法1: 快捷键切换(推荐)

在编辑器中按 **B键** 可以循环切换三种模式:

```
DISABLED → UNIFIED → INDEPENDENT → DISABLED → ...
```

每次切换时,控制台会显示当前模式。

### 方法2: 代码设置

在`PuppetEditorApp.java`的初始化代码中设置:

```java
// 设置为禁用Billboard模式(适合3D立体模型)
puppetTestScene.getPuppetRenderer().setBillboardMode(PuppetRenderer.BillboardMode.DISABLED);

// 设置为统一Billboard模式(适合2D精灵)
puppetTestScene.getPuppetRenderer().setBillboardMode(PuppetRenderer.BillboardMode.UNIFIED);

// 设置为独立Billboard模式(不推荐)
puppetTestScene.getPuppetRenderer().setBillboardMode(PuppetRenderer.BillboardMode.INDEPENDENT);
```

## 导出和加载

### 自动保存

Billboard模式会自动保存到木偶配置文件(`.json`)中:

```json
{
  "name": "MyPuppet",
  "version": "1.0",
  "billboardMode": "DISABLED",
  "bones": [...]
}
```

### 自动加载

在游戏中加载木偶时,Billboard模式会自动从配置文件恢复:

```java
// 加载配置
PuppetConfig config = PuppetIO.loadFromFile("path/to/puppet.json");

// 应用配置(Billboard模式会自动设置)
PuppetIO.applyConfig(config, skeleton, renderer);
```

控制台会显示:
```
[PuppetIO] 已设置Billboard模式: DISABLED
```

## 推荐使用场景

| 模型类型 | 推荐模式 | 说明 |
|---------|---------|------|
| 立方体、金字塔等3D图形 | **DISABLED** | 保持立体结构 |
| 2D角色精灵 | **UNIFIED** | 纸人效果 |
| 粒子效果、UI元素 | **INDEPENDENT** | 特殊用途 |

## 注意事项

1. **向后兼容**: 旧的配置文件会默认使用`UNIFIED`模式
2. **实时切换**: 可以在编辑器运行时使用B键实时切换查看效果
3. **游戏中使用**: 确保游戏代码使用`PuppetIO.applyConfig()`来正确加载配置

## 技术细节

- Billboard模式存储在`PuppetConfig`类的`billboardMode`字段中
- 使用字符串存储,便于JSON序列化
- 加载时会验证有效性,无效值会回退到`UNIFIED`模式
