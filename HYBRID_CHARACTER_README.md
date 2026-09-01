# 混合角色系统 - 快速入门

## 🎯 系统概述

混合角色系统允许你将 **Blender 导出的3D骨骼模型** 与 **木偶编辑器制作的2D部件** 结合，创建独特的角色。

### 核心优势

1. **灵活的换装系统** - 运行时替换任意部件
2. **尺寸可调** - 支持部件尺寸和模型缩放
3. **完整的骨骼动画** - 保留3D模型的所有动画
4. **多方向贴图** - 2D部件自动跟随相机切换方向
5. **模块化资源** - 贴图、模型、动画分离存储

## 📁 文件结构

```
src/main/
├── java/com/Hecate/character/
│   ├── PuppetPartDefinition.java      # 部件定义类
│   ├── CharacterSkin.java             # 皮肤配置类
│   ├── CharacterConfigLoader.java     # 配置加载器
│   ├── HybridCharacterRenderer.java   # 核心渲染器
│   └── HybridCharacterExample.java    # 使用示例
│
└── resources/Characters/
    ├── Models/                        # 3D骨骼模型 (.j3o)
    ├── Puppets/
    │   ├── Parts/                     # 部件定义 (.json)
    │   └── Skins/                     # 皮肤配置 (.json)
    ├── Textures/                      # 所有贴图
    │   ├── Body/
    │   ├── Weapons/
    │   └── Accessories/
    └── Animations/                    # 动画文件（包含在.j3o中）
```

## 🚀 快速开始

### 1. 准备 Blender 模型

#### 骨骼命名示例

```
Root
├── Hips
│   ├── Spine
│   │   ├── Spine1
│   │   │   ├── Spine2
│   │   │   │   ├── Neck
│   │   │   │   │   └── Head         # 头部挂点
│   │   │   │   ├── Shoulder.R
│   │   │   │   │   ├── Arm.R
│   │   │   │   │   │   └── Hand.R  # 右手挂点（武器）
│   │   │   │   └── Shoulder.L
│   │   │   │       └── ...
│   ├── Leg.L
│   └── Leg.R
└── Socket_Backpack                  # 自定义背包挂点
```

#### 导出设置

1. **导出为 GLTF/GLB**
   - File → Export → glTF 2.0 (.glb)
   - ✅ Include: Animation, Skinning
   - ✅ Transform: +Y Up

2. **转换为 J3O**（推荐）
   - 使用 jMonkeyEngine SDK 转换
   - 更好的性能和加载速度

### 2. 创建部件定义

创建文件：`Characters/Puppets/Parts/head_default.json`

```json
{
  "partId": "head_default",
  "name": "Default Head",
  "partType": "BODY_HEAD",
  "targetBoneName": "Head",
  "width": 0.3,
  "height": 0.4,
  "attachmentOffset": {"x": 0.0, "y": 0.0, "z": 0.0},
  "attachmentRotation": {"x": 0.0, "y": 0.0, "z": 0.0},
  "scale": 1.0,
  "directionTextures": {
    "front": "Textures/Body/head_front.png",
    "back": "Textures/Body/head_back.png",
    "left": "Textures/Body/head_left.png",
    "right": "Textures/Body/head_right.png"
  },
  "renderPriority": 100,
  "billboardEnabled": true,
  "autoDirectionSwitch": true
}
```

### 3. 创建皮肤配置

创建文件：`Characters/Puppets/Skins/skin_default.json`

```json
{
  "skinId": "skin_default",
  "name": "Default Character",
  "baseModelPath": "Models/base_human.j3o",
  "modelScale": 1.0,
  "partSlots": [
    {
      "slotType": "BODY_HEAD",
      "partId": "head_default"
    },
    {
      "slotType": "WEAPON_MAIN",
      "partId": "weapon_sword"
    }
  ]
}
```

### 4. 使用代码

```java
// 创建渲染器
HybridCharacterRenderer character = new HybridCharacterRenderer(app, "Characters/");

// 加载皮肤
character.loadSkin("skin_default");

// 附加到场景
character.attachToScene(rootNode);

// 播放动画
character.playAnimation("walk", true);

// 在 simpleUpdate 中更新
@Override
public void simpleUpdate(float tpf) {
    character.update(tpf);
}
```

## 🎨 尺寸调整工作流

### 问题：2D部件与3D模型尺寸不匹配

#### 解决方案（按顺序尝试）：

1. **调整模型全局缩放**
   ```java
   character.setModelScale(1.5f); // 放大模型
   ```

2. **调整部件尺寸**（在部件定义中）
   ```json
   {
     "width": 0.4,   // 增大宽度
     "height": 0.5,  // 增大高度
     "scale": 1.2    // 整体放大20%
   }
   ```

3. **调整部件偏移**（微调位置）
   ```json
   {
     "attachmentOffset": {
       "x": 0.05,   // 向右移动
       "y": 0.1,    // 向上移动
       "z": -0.02   // 向前移动
     }
   }
   ```

4. **在皮肤配置中个性化调整**
   ```json
   {
     "slotType": "BODY_HEAD",
     "partId": "head_default",
     "customScale": 1.3,
     "customOffset": {"x": 0.0, "y": 0.05, "z": 0.0}
   }
   ```

### 推荐尺寸参考

| 部位 | 宽度 | 高度 | 说明 |
|------|------|------|------|
| 头部 | 0.3 | 0.4 | 标准人类头部 |
| 脖子 | 0.15 | 0.2 | 较细 |
| 躯干 | 0.5 | 0.7 | 上半身 |
| 手臂 | 0.15 | 0.5 | 单条手臂 |
| 腿部 | 0.2 | 0.6 | 单条腿 |
| 武器（剑） | 0.1 | 0.8 | 中型单手武器 |
| 武器（长矛） | 0.08 | 1.5 | 长型武器 |
| 背包 | 0.3 | 0.4 | 中型背包 |

## 🔧 换装系统

### 运行时更换部件

```java
// 更换武器
character.replacePart(
    PuppetPartDefinition.PartType.WEAPON_MAIN,
    "weapon_magic_staff"
);

// 更换头盔
character.replacePart(
    PuppetPartDefinition.PartType.ACCESSORY_HEAD,
    "helmet_dragon"
);

// 移除背包
character.removePart(
    PuppetPartDefinition.PartType.ACCESSORY_BACK
);
```

### 部件类型

```java
// 身体部位
BODY_HEAD, BODY_NECK, BODY_TORSO
BODY_ARM_LEFT, BODY_ARM_RIGHT
BODY_LEG_LEFT, BODY_LEG_RIGHT

// 武器
WEAPON_MAIN, WEAPON_OFF

// 装饰品
ACCESSORY_BACK   // 背包、翅膀等
ACCESSORY_HEAD   // 帽子、头盔等
ACCESSORY_FACE   // 眼镜、面具等
```

## 🎬 动画控制

```java
// 获取所有可用动画
Collection<String> animations = character.getAvailableAnimations();

// 播放循环动画
character.playAnimation("walk", true);

// 播放单次动画
character.playAnimation("attack", false);
```

## 🐛 调试技巧

### 1. 显示3D模型骨骼

```java
// 显示3D模型+2D部件（混合模式）
character.setHideModel(false);

// 只显示2D部件（默认）
character.setHideModel(true);
```

### 2. 打印骨骼信息

```java
// 打印所有可用骨骼名称
// 在 createAttachmentNodes() 方法中会自动打印

// 打印所有动画名称
System.out.println("Animations: " + character.getAvailableAnimations());
```

### 3. 手动调整部件

```java
// 获取部件渲染器
PuppetPartRenderer headRenderer = character.getPartRenderer("head_default");

// 手动调整（如果需要）
if (headRenderer != null) {
    // 访问底层的 Bone 对象
    com.Hecate.puppet.core.Bone bone = headRenderer.getBone();
    // 进行调整...
}
```

## 📊 性能优化

1. **使用 J3O 格式** - 比 GLTF 加载更快
2. **合并贴图** - 使用贴图集减少纹理切换
3. **缓存配置** - CharacterConfigLoader 自动缓存
4. **LOD 系统** - 远距离使用简化模型

## 📚 完整文档

- [详细文档](docs/HYBRID_CHARACTER_SYSTEM.md)
- [使用示例](src/main/java/com/Hecate/character/HybridCharacterExample.java)
- [木偶编辑器教程](PUPPET_EDITOR_TUTORIAL.md)
- [Blender 导入指南](BLENDER_IMPORT_README.md)

## ❓ 常见问题

### Q: 部件不显示？
**A:** 检查：
1. `targetBoneName` 是否与 Blender 中的骨骼名称完全一致（区分大小写）
2. 贴图路径是否正确
3. 部件尺寸是否设置（width, height）

### Q: 部件位置偏移很大？
**A:** 尝试：
1. 调整 `modelScale`（通常在 0.5 - 2.0 之间）
2. 检查 Blender 导出时的单位设置
3. 调整 `attachmentOffset`

### Q: 换装时程序崩溃？
**A:** 确保：
1. 新部件的配置文件存在
2. 贴图文件路径正确
3. `targetBoneName` 对应的骨骼存在

## 🎉 开始创作！

1. 在 Blender 中准备你的角色骨骼
2. 在木偶编辑器中绘制2D部件
3. 创建配置文件连接它们
4. 运行示例程序查看效果

祝你创作愉快！有问题请查看详细文档或提交 Issue。
