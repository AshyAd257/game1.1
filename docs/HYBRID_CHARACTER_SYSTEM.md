# 混合角色系统 (Hybrid Character System)

## 概述

混合角色系统允许你将 **3D 骨骼动画模型** 与 **2D 木偶部件** 结合，创建独特的角色外观。

### 核心功能

- ✅ 从 Blender 导入3D骨骼模型
- ✅ 将2D木偶部件挂载到3D骨骼上
- ✅ 支持换装系统（运行时替换部件）
- ✅ 支持模型缩放调整
- ✅ 支持多方向贴图自动切换
- ✅ 支持完整的骨骼动画播放

## 文件组织结构

```
resources/Characters/
├── Models/           # 3D骨骼模型
│   ├── base_human.j3o
│   ├── base_human_male.j3o
│   └── base_human_female.j3o
│
├── Puppets/          # 木偶部件配置
│   ├── Parts/        # 部件定义
│   │   ├── head_default.json      # 头部部件
│   │   ├── neck_default.json      # 脖子部件
│   │   ├── torso_default.json     # 躯干部件
│   │   ├── weapon_sword.json      # 武器部件
│   │   └── backpack_leather.json  # 背包部件
│   │
│   └── Skins/        # 皮肤配置（组合多个部件）
│       ├── skin_default.json
│       ├── skin_knight.json
│       └── skin_mage.json
│
├── Textures/         # 所有贴图资源
│   ├── Body/
│   │   ├── head_front.png
│   │   ├── head_back.png
│   │   ├── neck_front.png
│   │   └── torso_front.png
│   │
│   ├── Weapons/
│   │   ├── sword_iron.png
│   │   └── sword_gold.png
│   │
│   └── Accessories/
│       ├── backpack_leather.png
│       └── backpack_gold.png
│
└── Animations/       # 动画文件（包含在.j3o模型中）
    ├── idle.anim
    ├── walk.anim
    ├── run.anim
    └── attack.anim
```

## 配置文件格式

### 1. 部件定义 (Part Definition)

文件位置：`Characters/Puppets/Parts/{partId}.json`

```json
{
  "partId": "head_default",
  "name": "Default Head",
  "partType": "BODY_HEAD",
  "targetBoneName": "Head",
  "width": 0.3,
  "height": 0.4,
  "attachmentOffset": {
    "x": 0.0,
    "y": 0.0,
    "z": 0.0
  },
  "attachmentRotation": {
    "x": 0.0,
    "y": 0.0,
    "z": 0.0
  },
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

#### 字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| `partId` | String | 部件唯一ID |
| `name` | String | 部件显示名称 |
| `partType` | Enum | 部件类型（见下表） |
| `targetBoneName` | String | 要绑定的3D骨骼名称（必须与Blender中的骨骼名称一致） |
| `width` | Float | 部件宽度（世界单位） |
| `height` | Float | 部件高度（世界单位） |
| `attachmentOffset` | Vec3 | 相对骨骼的位置偏移 |
| `attachmentRotation` | Vec3 | 相对骨骼的旋转（欧拉角，度数） |
| `scale` | Float | 部件缩放（相对于原始尺寸） |
| `directionTextures` | Map | 多方向贴图路径 |
| `renderPriority` | Int | 渲染优先级（越大越靠前） |
| `billboardEnabled` | Boolean | 是否启用Billboard渲染 |
| `autoDirectionSwitch` | Boolean | 是否跟随相机自动切换方向 |

#### 部件类型 (PartType)

| 类型 | 说明 |
|------|------|
| `BODY_HEAD` | 头部 |
| `BODY_NECK` | 脖子 |
| `BODY_TORSO` | 躯干 |
| `BODY_ARM_LEFT` | 左臂 |
| `BODY_ARM_RIGHT` | 右臂 |
| `BODY_LEG_LEFT` | 左腿 |
| `BODY_LEG_RIGHT` | 右腿 |
| `WEAPON_MAIN` | 主武器 |
| `WEAPON_OFF` | 副武器 |
| `ACCESSORY_BACK` | 背部装饰（背包、翅膀等） |
| `ACCESSORY_HEAD` | 头部装饰（帽子、头盔等） |
| `ACCESSORY_FACE` | 面部装饰（眼镜、面具等） |

### 2. 皮肤配置 (Skin Configuration)

文件位置：`Characters/Puppets/Skins/{skinId}.json`

```json
{
  "skinId": "knight_heavy",
  "name": "Heavy Knight",
  "baseModelPath": "Models/base_human.j3o",
  "modelScale": 1.2,
  "partSlots": [
    {
      "slotType": "BODY_HEAD",
      "partId": "head_knight",
      "customScale": 1.1
    },
    {
      "slotType": "BODY_NECK",
      "partId": "neck_armored"
    },
    {
      "slotType": "BODY_TORSO",
      "partId": "torso_plate_armor",
      "customScale": 1.3
    },
    {
      "slotType": "WEAPON_MAIN",
      "partId": "weapon_longsword",
      "customOffset": {
        "x": 0.1,
        "y": 0.0,
        "z": 0.0
      }
    },
    {
      "slotType": "ACCESSORY_BACK",
      "partId": "backpack_leather"
    }
  ]
}
```

#### 字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| `skinId` | String | 皮肤唯一ID |
| `name` | String | 皮肤显示名称 |
| `baseModelPath` | String | 3D模型路径 |
| `modelScale` | Float | 模型全局缩放（用于调整3D模型与2D部件的比例） |
| `partSlots` | Array | 部件槽位列表 |

#### 部件槽位 (PartSlot)

| 字段 | 类型 | 说明 |
|------|------|------|
| `slotType` | Enum | 部件槽位类型 |
| `partId` | String | 使用的部件ID |
| `customScale` | Float? | 该部件的个性化缩放（覆盖部件定义中的scale） |
| `customOffset` | Vec3? | 该部件的个性化偏移（覆盖部件定义中的offset） |

## 使用方法

### 1. 基本使用

```java
// 创建混合角色渲染器
HybridCharacterRenderer character = new HybridCharacterRenderer(app, "Characters/");

// 加载皮肤
character.loadSkin("knight_heavy");

// 附加到场景
character.attachToScene(rootNode);

// 在主循环中更新
@Override
public void simpleUpdate(float tpf) {
    character.update(tpf);
}

// 播放动画
character.playAnimation("walk", true);
```

### 2. 运行时换装

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
character.removePart(PuppetPartDefinition.PartType.ACCESSORY_BACK);
```

### 3. 调整模型缩放

```java
// 设置模型缩放（用于调整3D模型与2D部件的比例）
character.setModelScale(1.5f);

// 获取当前缩放
float currentScale = character.getModelScale();
```

### 4. 控制3D模型可见性

```java
// 隐藏3D模型，只显示2D部件
character.setHideModel(true);

// 显示3D模型+2D部件（混合渲染）
character.setHideModel(false);
```

### 5. 动画控制

```java
// 获取所有可用动画
Collection<String> animations = character.getAvailableAnimations();

// 播放动画（循环）
character.playAnimation("walk", true);

// 播放动画（单次）
character.playAnimation("attack", false);
```

## Blender 模型准备

### 1. 骨骼命名规范

在 Blender 中为骨骼命名时，使用清晰的名称：

```
Root
├── Hips
│   ├── Spine
│   │   ├── Spine1
│   │   │   ├── Spine2
│   │   │   │   ├── Neck
│   │   │   │   │   └── Head
│   │   │   │   ├── Shoulder.L
│   │   │   │   │   ├── Arm.L
│   │   │   │   │   │   └── Hand.L
│   │   │   │   └── Shoulder.R
│   │   │   │       ├── Arm.R
│   │   │   │       │   └── Hand.R
│   ├── Leg.L
│   │   └── Foot.L
│   └── Leg.R
│       └── Foot.R
├── Socket_Weapon_Main    # 主武器挂点
├── Socket_Weapon_Off     # 副武器挂点
├── Socket_Backpack       # 背包挂点
└── Socket_Hat            # 帽子挂点
```

### 2. 创建挂点骨骼

在 Blender 中添加"挂点骨骼"（Socket Bones）：

1. 选择要添加挂点的父骨骼（如右手）
2. 添加新骨骼：`Shift + A` → Bone
3. 命名为 `Socket_Weapon_Main`
4. 调整位置到合适的挂载位置
5. 可以设置为零长度骨骼（只是位置标记）

### 3. 导出设置

**导出为 GLTF/GLB：**
- File → Export → glTF 2.0 (.glb/.gltf)
- ✅ Include: Selected Objects
- ✅ Transform: +Y Up
- ✅ Geometry: Apply Modifiers
- ✅ Animation: Animation
- ✅ Skinning

**转换为 J3O（推荐）：**
使用 jMonkeyEngine SDK 将 GLTF 转换为 J3O 格式以获得更好的性能。

## 尺寸调整指南

### 1. 部件尺寸设置

不同部位的推荐尺寸（世界单位）：

| 部位 | 宽度 | 高度 | 说明 |
|------|------|------|------|
| 头部 | 0.3 | 0.4 | 标准人类头部 |
| 脖子 | 0.15 | 0.2 | 较细 |
| 躯干 | 0.5 | 0.7 | 上半身 |
| 手臂 | 0.15 | 0.5 | 单条手臂 |
| 腿部 | 0.2 | 0.6 | 单条腿 |
| 武器 | 0.1-0.3 | 0.5-1.5 | 根据武器类型 |

### 2. 模型缩放调整

如果2D部件与3D模型不匹配：

1. **先调整模型全局缩放**：`modelScale` (0.5 - 2.0)
2. **再调整个别部件缩放**：`customScale` 或 `scale`
3. **最后微调偏移**：`attachmentOffset`

### 3. 调试技巧

```java
// 显示3D模型骨骼，帮助定位
character.setHideModel(false);

// 打印所有骨骼名称
Collection<String> animations = character.getAvailableAnimations();
System.out.println("Available animations: " + animations);

// 获取部件渲染器，手动调整
PuppetPartRenderer headRenderer = character.getPartRenderer("head_default");
if (headRenderer != null) {
    // 调整部件位置、旋转等
}
```

## 性能优化

1. **使用 J3O 格式**：将 GLTF 转换为 J3O 以获得更快的加载速度
2. **合并贴图**：将多个小贴图合并为贴图集
3. **LOD 系统**：远距离使用低精度模型
4. **批处理**：相同材质的部件可以批处理渲染

## 常见问题

### Q: 部件位置不对？
**A:** 检查以下几点：
1. `targetBoneName` 是否正确（区分大小写）
2. `attachmentOffset` 是否设置正确
3. `modelScale` 是否合适

### Q: 贴图方向错误？
**A:** 检查：
1. `directionTextures` 映射是否正确
2. `autoDirectionSwitch` 是否启用

### Q: 动画播放不流畅？
**A:** 确保：
1. 动画帧率足够高（至少 30 FPS）
2. 使用 `LoopMode.Loop` 进行循环动画

### Q: 部件遮挡顺序错误？
**A:** 调整 `renderPriority` 值，数值越大越靠前显示。

## 示例项目

完整示例请查看：
- `src/main/java/com/Hecate/character/HybridCharacterExample.java`
- `resources/Characters/` 示例配置文件

## 相关文档

- [木偶编辑器教程](PUPPET_EDITOR_TUTORIAL.md)
- [Blender 导入指南](BLENDER_IMPORT_GUIDE.md)
- [动画系统文档](ANIMATION_SYSTEM_COMPLETE.md)
