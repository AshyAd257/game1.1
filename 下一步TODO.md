# 🎯 下一步 TODO 清单

## ✅ 已完成
- ✅ 所有核心类已创建（TileState, ProjectileMode, ElementType, GridCellNew, DecalManager 等）
- ✅ 武器系统已集成射弹模式（Weapon, SteampunkGun, SniperRifle）
- ✅ V 键切换输入已添加（PlayerControlModule）
- ✅ 地表装饰系统已实现（ElementalSurfaceDecal, DecalManager）
- ✅ 移除了元素克制系统

---

## 🔨 需要手动完成的集成

### 1️⃣ 测试基础功能
```bash
# 在 IDEA 中或者找到 Maven 路径后运行
mvn compile
mvn exec:java -Dexec.mainClass="com.Hecate.Main"
```
**测试项**：
- 游戏能否正常启动
- 按 V 键是否在控制台显示切换消息
- 是否有 3 秒冷却时间

---

### 2️⃣ 集成 SparseGridManager（重要！）

**文件**：`src/main/java/com/Hecate/ink/SparseGridManager.java`

**添加方法**：
```java
import com.Hecate.weapon.ProjectileMode;
import com.Hecate.element.ElementType;

// 应用射弹命中
public void applyProjectile(Vector3f worldPos, ProjectileMode mode, ElementType element) {
    GridCellNew cell = getOrCreateCell(worldPos);
    cell.applyHit(mode, element);
}

// 查询地块状态
public TileState getTileState(Vector3f worldPos) {
    GridCellNew cell = getCell(worldPos);
    return cell != null ? cell.getCurrentState() : TileState.NONE;
}

// 查询 buff
public float getSpeedMultiplier(Vector3f worldPos) {
    GridCellNew cell = getCell(worldPos);
    return cell != null ? cell.getSpeedMultiplier() : 1.0f;
}

public boolean isInvisible(Vector3f worldPos) {
    GridCellNew cell = getCell(worldPos);
    return cell != null && cell.isInvisible();
}

public float getHealthRegenRate(Vector3f worldPos) {
    GridCellNew cell = getCell(worldPos);
    return cell != null ? cell.getHealthRegenRate() : 0.0f;
}
```

---

### 3️⃣ 射弹命中时调用

**文件**：找到射弹的碰撞检测代码（可能在 `Projectile.java` 或武器类中）

**添加逻辑**：
```java
// 在射弹命中地面时
public void onHitGround(Vector3f hitPosition) {
    // 应用到地块系统
    sparseGridManager.applyProjectile(hitPosition, this.projectileMode, this.element);
    
    // 生成地表装饰
    decalManager.addDecal(hitPosition, this.element);
    
    // ... 原有的命中逻辑
}
```

---

### 4️⃣ 初始化 DecalManager

**文件**：`src/main/java/com/Hecate/Main.java`（或主游戏类）

**添加代码**：
```java
private DecalManager decalManager;

@Override
public void simpleInitApp() {
    // ... 其他初始化
    
    // 初始化装饰管理器
    decalManager = new DecalManager(assetManager, rootNode);
    
    // 将 decalManager 传递给需要的系统（如武器管理器）
}

@Override
public void simpleUpdate(float tpf) {
    // ... 其他更新
    
    // 更新装饰物（淡出、过期清理）
    decalManager.update(tpf);
}
```

---

### 5️⃣ 玩家移动时应用 buff

**文件**：`src/main/java/com/Hecate/player/PlayerController.java`

**添加逻辑**：
```java
// 在玩家更新方法中（每帧调用）
public void update(float tpf) {
    Vector3f playerPos = spatial.getWorldTranslation();
    
    // 查询当前脚下地块的 buff
    float speedMult = sparseGridManager.getSpeedMultiplier(playerPos);
    boolean invisible = sparseGridManager.isInvisible(playerPos);
    float regenRate = sparseGridManager.getHealthRegenRate(playerPos);
    
    // 应用速度 buff
    this.walkSpeed = BASE_WALK_SPEED * speedMult;
    
    // 应用隐身效果（可能需要修改渲染或 AI 检测）
    if (invisible) {
        // 例如：设置半透明或隐藏玩家模型
    }
    
    // 应用生命恢复
    if (regenRate > 0 && playerHealth != null) {
        playerHealth.heal(regenRate * tpf);
    }
    
    // ... 原有的移动逻辑
}
```

---

### 6️⃣ 为玩家添加元素选择（可选，暂时可硬编码）

**临时方案**：在玩家初始化时硬编码元素
```java
// 在创建武器或玩家时
weapon.setPlayerElement(ElementType.GRASS); // 暂时都用草元素
```

**完整方案**：创建 UI 界面让玩家选择元素
- 游戏开始时弹出选择界面
- 5 个按钮对应 5 种元素
- 选择后设置到所有武器

---

## 📋 集成优先级

1. **测试 V 键切换**（5分钟）- 验证基础功能
2. **集成 SparseGridManager**（30分钟）- 核心玩法
3. **射弹命中调用**（20分钟）- 连接射弹和地块
4. **初始化 DecalManager**（10分钟）- 地表装饰
5. **玩家 buff 应用**（30分钟）- 实际游戏效果
6. **元素选择 UI**（1-2小时）- 可延后

---

## 🔍 需要找到的文件

如果不确定在哪里修改，搜索这些关键字：
```bash
# 找到射弹命中逻辑
grep -r "onCollision\|onHit\|projectile" src/

# 找到玩家移动更新
grep -r "simpleUpdate\|update.*tpf" src/main/java/com/Hecate/player/

# 找到主游戏初始化
grep -r "simpleInitApp" src/
```

---

## 🎮 测试流程

完成集成后的测试步骤：
1. 启动游戏
2. 按 V 键，看控制台是否显示切换消息
3. 射击地面，观察是否生成地表装饰
4. 站在涂过的地块上，观察是否有速度变化
5. 切换到黑暗模式，涂地后站上去看是否隐身

---

## 📖 参考文档

- `新玩法集成完成报告.md` - 详细的完成状态和架构说明
- `新玩法系统设计.md` - 完整设计文档
- `新玩法实施指南.md` - 分步骤指南
- `新玩法快速参考.md` - API 速查表

---

## 💡 提示

- 所有新类都在 `src/main/java/com/Hecate/` 下，按功能分在不同包中
- 如果模型文件缺失不影响功能测试，会使用占位方块
- 可以在 `GridCellNew` 等类的常量中调整参数（覆盖次数、buff倍率等）

---

生成时间：2026-09-13
