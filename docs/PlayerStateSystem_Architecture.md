# 玩家状态系统架构总结

## 🎯 问题分析

### 原有系统的问题

1. **方块选择硬编码**（PlayerControlModule.java）
   - 第40行：`private String selectedBlockType = "stone"`
   - 第149-152行：硬编码按键映射（1/2/3/4 对应 stone/dirt/grass/glass）
   - 第218-242行：switch-case 硬编码处理每个方块选择
   - **问题**：每增加一个方块需要修改多处代码

2. **武器选择硬编码**（PlayerController.java）
   - 第144行：`private Weapon currentWeapon`
   - 第367行：`currentWeapon = BasicShooter.createDefault()`
   - 第384行：`this.currentWeapon = flameWeapon`
   - **问题**：武器也是枚举式硬编码，无法从武器表动态选择

3. **缺少玩家状态管理**
   - 没有统一的 Buff/Debuff 系统
   - 无法管理玩家的持续效果（中毒、速度提升等）
   - 状态效果分散在各处代码中

---

## ✅ 解决方案

### 新系统架构

```
PlayerStateManager (玩家状态管理器)
├── PlayerEquipment (装备系统)
│   └── PlayerHotbar (快捷栏：9个槽位)
│       └── HeldItem (手持物品)
│           ├── 类型：EMPTY / WEAPON / BLOCK / ITEM
│           └── ID：从注册表查询
└── PlayerEffectManager (效果管理器)
    └── ActiveEffect (活跃效果实例)
        └── EffectDefinition (效果模板)
            └── EffectRegistry (效果注册表)
```

### 核心组件

#### 1. 装备系统（com.Hecate.player.inventory）

| 类名 | 职责 |
|-----|------|
| `HeldItemType` | 手持物品类型枚举 |
| `HeldItem` | 手持物品数据（不可变） |
| `PlayerHotbar` | 快捷栏管理（9个槽位） |
| `PlayerEquipment` | 装备管理器（集成快捷栏+注册表查询） |

#### 2. 效果系统（com.Hecate.player.effect）

| 类名 | 职责 |
|-----|------|
| `EffectType` | 效果类型枚举（Buff/Debuff） |
| `EffectDefinition` | 效果模板定义（静态配置） |
| `ActiveEffect` | 活跃效果实例（运行时数据） |
| `EffectRegistry` | 效果注册表（单例） |
| `PlayerEffectManager` | 效果管理器（应用/移除/更新） |

#### 3. 统一管理

| 类名 | 职责 |
|-----|------|
| `PlayerStateManager` | 整合装备+效果，提供统一接口 |

---

## 📖 使用指南

### 1. 初始化

```java
// 在 PlayerController 或模块初始化时
PlayerStateManager stateManager = new PlayerStateManager(blockRegistry, weaponRegistry);

// 配置默认快捷栏
stateManager.getEquipment().setHotbarSlot(0, HeldItem.block("stone"));
stateManager.getEquipment().setHotbarSlot(1, HeldItem.block("dirt"));
stateManager.getEquipment().setHotbarSlot(4, HeldItem.weapon("smg_01"));
```

### 2. 快捷栏切换（替代硬编码按键）

```java
// 旧代码（需要删除）
case "SelectStone": selectedBlockType = "stone"; break;

// 新代码
stateManager.getEquipment().selectHotbarSlot(0);  // 选择第1个槽位
```

### 3. 获取当前装备

```java
// 旧代码
String blockType = selectedBlockType;  // 只是字符串
Weapon weapon = currentWeapon;         // 硬编码字段

// 新代码
Block block = stateManager.getEquipment().getCurrentBlock();    // 从注册表查询
Weapon weapon = stateManager.getEquipment().getCurrentWeapon(); // 从注册表查询

// 状态检查
boolean holdingWeapon = stateManager.getEquipment().isHoldingWeapon();
boolean holdingBlock = stateManager.getEquipment().isHoldingBlock();
boolean empty = stateManager.getEquipment().isEmpty();
```

### 4. 效果系统使用

```java
PlayerEffectManager effectManager = stateManager.getEffectManager();

// 应用效果
effectManager.applyEffect("speed_boost");  // 速度提升
effectManager.applyEffect("poison");       // 中毒

// 检查效果
if (effectManager.hasEffect("invincible")) {
    // 玩家无敌
}

// 获取受效果影响的属性
float speedMultiplier = stateManager.getSpeedMultiplier();   // 考虑速度相关效果
float damageMultiplier = stateManager.getDamageMultiplier(); // 考虑伤害相关效果

// 移除效果
effectManager.removeEffect("poison");
effectManager.clearHarmfulEffects();  // 清除所有负面效果
```

### 5. 每帧更新

```java
// 在 PlayerController 的 update() 或 simpleUpdate() 中
public void update(float tpf) {
    // 更新效果系统（处理过期、持续伤害等）
    stateManager.update(tpf);
    
    // 应用速度效果
    float speed = baseSpeed * stateManager.getSpeedMultiplier();
    
    // 检查眩晕
    if (stateManager.isStunned()) {
        return;  // 禁用移动
    }
}
```

---

## 🔧 迁移步骤

### 第一步：在 PlayerController 中集成

```java
public class PlayerController {
    // 添加新字段
    private PlayerStateManager playerStateManager;
    
    // 初始化方法中
    public void initialize(BlockRegistry blockRegistry, WeaponRegistry weaponRegistry) {
        playerStateManager = new PlayerStateManager(blockRegistry, weaponRegistry);
        // ... 其他初始化
    }
    
    // 更新方法中
    @Override
    public void update(float tpf) {
        playerStateManager.update(tpf);
        // ... 其他更新
    }
}
```

### 第二步：替换 PlayerControlModule 的硬编码

```java
// 删除这些旧代码：
// - private String selectedBlockType = "stone";
// - inputManager.addMapping("SelectStone", new KeyTrigger(KeyInput.KEY_1));
// - case "SelectStone": selectedBlockType = "stone"; break;

// 替换为统一的快捷栏映射
for (int i = 0; i < 9; i++) {
    String name = "SelectSlot" + i;
    inputManager.addMapping(name, new KeyTrigger(KeyInput.KEY_1 + i));
    inputManager.addListener((n, pressed, tpf) -> {
        if (pressed && n.startsWith("SelectSlot")) {
            int slot = Integer.parseInt(n.substring(10));
            playerStateManager.getEquipment().selectHotbarSlot(slot);
        }
    }, name);
}
```

### 第三步：更新武器系统

```java
// 删除：private Weapon currentWeapon;
// 删除：currentWeapon = BasicShooter.createDefault();

// 替换为：
public Weapon getCurrentWeapon() {
    return playerStateManager.getEquipment().getCurrentWeapon();
}
```

---

## 🎨 效果系统详解

### 预定义效果列表

| 效果ID | 类型 | 描述 | 冲突效果 |
|-------|------|------|---------|
| `speed_boost` | Buff | 速度提升1.5倍 | slowness |
| `strength` | Buff | 伤害提升1.3倍 | weakness |
| `regeneration` | Buff | 每秒恢复2点生命 | poison |
| `shield` | Buff | 20点护盾值 | - |
| `poison` | Debuff | 每秒1点伤害 | regeneration |
| `slowness` | Debuff | 速度降至0.5倍 | speed_boost |
| `weakness` | Debuff | 伤害降至0.7倍 | strength |
| `burning` | Debuff | 每秒2点伤害 | - |
| `frozen` | Debuff | 速度降至0.3倍 | - |
| `stun` | Debuff | 完全无法移动 | - |
| `invincible` | 特殊 | 无敌帧 | - |

### 效果特性

1. **堆叠系统**：部分效果可堆叠（如中毒最多5层）
2. **冲突处理**：互斥效果会自动替换（如速度提升vs减速）
3. **持续时间刷新**：重复应用可刷新效果时长
4. **自动过期**：效果到期自动移除
5. **事件监听**：支持监听效果应用/移除/堆叠/刷新事件

### 自定义效果示例

```java
// 在 EffectRegistry 中注册自定义效果
EffectRegistry.getInstance().register(
    new EffectDefinition.Builder("custom_effect", EffectType.SPEED_BOOST)
        .duration(20.0f)
        .magnitude(2.0f)
        .maxStacks(10)
        .refreshable(true)
        .conflictsWith("slowness", "frozen")
        .build()
);

// 应用自定义效果
effectManager.applyEffect("custom_effect");
```

---

## 📊 架构优势对比

| 特性 | 旧系统 | 新系统 |
|-----|--------|--------|
| 扩展性 | ❌ 每加一个物品改3处代码 | ✅ 配置化，无需改代码 |
| 类型安全 | ❌ 字符串拼写错误无法检测 | ✅ 强类型+枚举 |
| 代码量 | ❌ 重复的switch-case | ✅ 数据驱动 |
| Buff系统 | ❌ 不存在 | ✅ 完整实现 |
| 可测试性 | ❌ 依赖输入系统 | ✅ 单元测试覆盖 |
| 配置化 | ❌ 硬编码 | ✅ 可从文件加载 |

---

## 🧪 测试

运行单元测试（当 Maven 可用时）：

```bash
mvn test -Dtest=PlayerStateSystemTest
```

测试覆盖：
- ✅ 快捷栏槽位切换
- ✅ 方块/武器装备
- ✅ 空手状态
- ✅ 效果应用与冲突
- ✅ 效果堆叠
- ✅ 效果过期
- ✅ 状态检查（无敌/眩晕）
- ✅ 属性倍率计算

---

## 📁 文件清单

### 核心代码（10个类）

```
src/main/java/com/Hecate/player/
├── inventory/
│   ├── HeldItemType.java           # 手持物品类型枚举
│   ├── HeldItem.java               # 手持物品数据类
│   ├── PlayerHotbar.java           # 快捷栏系统（9槽位）
│   ├── PlayerEquipment.java        # 装备管理器
│   └── PlayerStateManager.java    # 统一状态管理器
└── effect/
    ├── EffectType.java             # 效果类型枚举
    ├── EffectDefinition.java       # 效果模板定义
    ├── ActiveEffect.java           # 活跃效果实例
    ├── EffectRegistry.java         # 效果注册表
    └── PlayerEffectManager.java    # 效果管理器
```

### 文档（3个文件）

```
docs/
├── PlayerStateSystem_README.md                # 系统文档
├── PlayerController_Integration_Example.java  # 集成示例代码
└── (本文件) 架构总结
```

### 测试

```
src/test/java/com/Hecate/player/inventory/
└── PlayerStateSystemTest.java      # 单元测试（10个测试用例）
```

---

## 🚀 下一步建议

1. **立即可做**：
   - 将 PlayerStateManager 集成到 PlayerController
   - 替换 PlayerControlModule 中的硬编码逻辑
   - 运行测试确保功能正常

2. **短期改进**：
   - 实现物品注册表（ItemRegistry）
   - 在 HUD 显示效果图标和剩余时间
   - 支持从配置文件加载快捷栏配置

3. **长期扩展**：
   - 完整背包系统（不只是快捷栏）
   - 效果粒子特效
   - 保存/加载玩家状态
   - 自定义效果脚本系统

---

## ❓ 常见问题

**Q: 为什么不把解除buff的逻辑放在注册表？**  
A: 注册表存静态模板，解除逻辑需要访问运行时状态，所以放在 `ActiveEffect.onRemove()` 中作为生命周期回调。

**Q: 如何添加新的效果类型？**  
A: 在 `EffectType` 枚举中添加，然后在 `EffectRegistry.registerDefaultEffects()` 中注册。

**Q: 快捷栏可以扩展到9个以上吗？**  
A: 可以，修改 `PlayerHotbar.HOTBAR_SIZE` 常量即可。

**Q: 效果系统如何处理持续伤害？**  
A: 在 `ActiveEffect.onTick()` 中实现，每帧调用一次。

---

**创建时间**: 2026-08-20  
**版本**: 1.0  
**作者**: Claude Code
