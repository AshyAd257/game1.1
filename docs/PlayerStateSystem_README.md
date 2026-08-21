# 玩家状态系统重构说明

## 概述

本次重构将硬编码的玩家手持物品和武器选择系统替换为基于注册表的灵活架构，并新增了完整的 Buff/Debuff 效果系统。

## 新增组件

### 1. 装备与物品栏系统

#### `HeldItemType.java`
- 定义手持物品类型枚举：空手、武器、方块、物品

#### `HeldItem.java`
- 手持物品数据类，通过工厂方法创建：
  - `HeldItem.empty()` - 空手
  - `HeldItem.weapon(String weaponId)` - 手持武器
  - `HeldItem.block(String blockId)` - 手持方块
  - `HeldItem.item(String itemId)` - 手持物品（暂未实现）

#### `PlayerHotbar.java`
- 管理 9 个快捷栏槽位（对应数字键 1-9）
- 支持槽位选择、物品设置、默认配置初始化
- 提供 `initializeDefault()` 方法设置默认快捷栏

#### `PlayerEquipment.java`
- 统一装备管理器，集成快捷栏系统
- 自动从注册表查询当前装备的实例（武器/方块）
- 提供便捷的状态检查方法：`isHoldingWeapon()`, `isHoldingBlock()`, `isEmpty()`

### 2. 效果系统（Buff/Debuff）

#### `EffectType.java`
- 定义所有效果类型：
  - **Buff**: 速度提升、力量、生命恢复、护盾、跳跃提升、隐身、抗性
  - **Debuff**: 中毒、减速、虚弱、失明、燃烧、冰冻、眩晕
  - **特殊**: 墨水着色、无敌帧

#### `EffectDefinition.java`
- 效果模板定义（Builder 模式）
- 配置：持续时间、堆叠上限、可刷新性、冲突效果、效果强度

#### `ActiveEffect.java`
- 活跃效果实例，包含运行时数据：
  - 剩余时间、当前堆叠层数、效果来源
  - 生命周期回调：`onApply()`, `onTick()`, `onRemove()`

#### `EffectRegistry.java`
- 效果注册表（单例）
- 预注册所有默认效果及其配置
- 提供效果实例工厂方法

#### `PlayerEffectManager.java`
- 玩家效果管理器
- 功能：
  - 应用/移除效果
  - 自动处理效果冲突（互斥效果会相互替换）
  - 效果堆叠和持续时间刷新
  - 效果过期自动清理
  - 事件监听器机制

### 3. 统一状态管理

#### `PlayerStateManager.java`
- 整合装备系统和效果系统
- 提供计算属性：
  - `getSpeedMultiplier()` - 速度倍率（受效果影响）
  - `getDamageMultiplier()` - 伤害倍率（受效果影响）
  - `isInvincible()`, `isStunned()`, `isInvisible()` - 状态检查
- 统一的 `update(float deltaTime)` 方法

## 架构优势

### 相比旧系统的改进

**旧系统问题**：
```java
// PlayerControlModule.java 旧代码
private String selectedBlockType = "stone";  // 硬编码字符串

inputManager.addMapping("SelectStone", new KeyTrigger(KeyInput.KEY_1));
inputManager.addMapping("SelectDirt", new KeyTrigger(KeyInput.KEY_2));
inputManager.addMapping("SelectGrass", new KeyTrigger(KeyInput.KEY_3));
inputManager.addMapping("SelectGlass", new KeyTrigger(KeyInput.KEY_4));

case "SelectStone": selectedBlockType = "stone"; break;
case "SelectDirt": selectedBlockType = "dirt"; break;
// ... 每增加一个方块都要改三处代码
```

**新系统优势**：
```java
// 新代码：通过注册表动态管理
PlayerStateManager stateManager = new PlayerStateManager(blockRegistry, weaponRegistry);

// 配置快捷栏（可从配置文件加载）
stateManager.getEquipment().setHotbarSlot(0, HeldItem.block("stone"));
stateManager.getEquipment().setHotbarSlot(4, HeldItem.weapon("smg_01"));

// 切换槽位（按键映射统一处理）
stateManager.getEquipment().selectHotbarSlot(0);

// 获取当前装备（自动从注册表查询实例）
Block currentBlock = stateManager.getEquipment().getCurrentBlock();
Weapon currentWeapon = stateManager.getEquipment().getCurrentWeapon();
```

### 关键特性

1. **数据驱动**：通过注册表管理，无需修改代码即可添加新物品
2. **类型安全**：使用枚举和强类型，避免字符串拼写错误
3. **解耦设计**：装备系统、效果系统、注册表分离，职责清晰
4. **扩展性强**：快捷栏可扩展为完整背包系统
5. **效果系统完整**：支持堆叠、冲突、持续时间、监听器

## 效果系统使用示例

```java
PlayerEffectManager effectManager = stateManager.getEffectManager();

// 应用效果
effectManager.applyEffect("speed_boost");
effectManager.applyEffect("poison");

// 检查效果
if (effectManager.hasEffect("invincible")) {
    // 玩家无敌，免疫伤害
}

// 获取受效果影响的属性
float speedMultiplier = stateManager.getSpeedMultiplier();  // 考虑速度提升/减速
float damageMultiplier = stateManager.getDamageMultiplier(); // 考虑力量/虚弱

// 清除负面效果
effectManager.clearHarmfulEffects();

// 自定义效果监听器
effectManager.addListener(new PlayerEffectManager.EffectListener() {
    @Override
    public void onEffectApplied(ActiveEffect effect) {
        System.out.println("效果已应用: " + effect.getType().getDisplayName());
    }
});
```

## 迁移指南

### 替换 PlayerControlModule 中的硬编码

**需要修改的位置**：
- `PlayerController.java:144` - `private Weapon currentWeapon`
- `PlayerController.java:367` - `currentWeapon = BasicShooter.createDefault()`
- `PlayerControlModule.java:40` - `private String selectedBlockType`
- `PlayerControlModule.java:149-152` - 硬编码的按键映射
- `PlayerControlModule.java:218-242` - switch-case 方块选择

**迁移步骤**：
1. 在 `PlayerController` 中添加 `PlayerStateManager` 实例
2. 用 `stateManager.getEquipment().selectHotbarSlot(index)` 替换硬编码的选择逻辑
3. 用 `stateManager.getEquipment().getCurrentWeapon()` 替换 `currentWeapon` 访问
4. 移除 `selectedBlockType` 字段和相关 switch-case

## 测试

已提供完整的单元测试 `PlayerStateSystemTest.java`，覆盖：
- 快捷栏槽位切换
- 方块/武器装备
- 空手状态
- 效果应用与冲突
- 效果堆叠
- 效果过期
- 无敌/眩晕状态检查
- 速度/伤害倍率计算

运行测试：
```bash
mvn test -Dtest=PlayerStateSystemTest
```

## 下一步建议

1. **集成到 PlayerController**：将 `PlayerStateManager` 实例化并替换现有硬编码
2. **实现物品注册表**：补充 `ItemRegistry`（目前物品系统预留但未实现）
3. **效果可视化**：在 HUD 上显示当前效果图标和剩余时间
4. **配置文件支持**：快捷栏配置可从 JSON/YAML 加载
5. **保存/加载系统**：序列化玩家装备和效果状态

## 设计决策

### 为什么 "解除 buff 逻辑" 不放在注册表？

**注册表职责**：存储静态模板数据（效果类型、基础属性）  
**实例职责**：管理运行时状态（剩余时间、叠加层数）  
**解除逻辑**：放在 `ActiveEffect.onRemove()` 中作为生命周期回调

这样设计的好处：
- 注册表保持简洁，只管数据定义
- 解除逻辑可以访问实例状态（如移除时触发爆炸、治疗等）
- 便于继承 `ActiveEffect` 实现自定义效果

---

**文件清单**：
- `com.Hecate.player.inventory.*` - 装备与物品栏系统（5个类）
- `com.Hecate.player.effect.*` - 效果系统（5个类）
- `PlayerStateSystemTest.java` - 单元测试
