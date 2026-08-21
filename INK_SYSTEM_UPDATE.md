# 墨水系统更新总结

## 更新日期：2026-08-20

## 主要改进

### 1. 墨水持续时间延长到75秒
**修改文件：** `SparseGridManager.java`
- 将 `inkDecayTime` 从 60 秒延长到 **75 秒**
- 墨水在地面上的存在时间更长，增强涂地战略价值

### 2. 墨水渐变消退效果
**修改文件：** `SparseGridManager.java`, `GridRegion.java`, `GridCell.java`
- 新增 `fadeStartTime = 15.0f` 参数
- 墨水在消失前的 **最后15秒** 开始线性淡化
- 强度从 1.0 逐渐降低到 0.0
- `GridCell.update()` 实现分阶段消退逻辑：
  - 前60秒：保持满强度（intensity = 1.0）
  - 最后15秒：线性淡化（intensity = 1.0 → 0.0）
  - 超过75秒：清除墨水

**淡化公式：**
```java
float fadeThreshold = inkDecayTime - fadeStartTime;  // 75 - 15 = 60秒
if (elapsed < fadeThreshold) {
    intensity = 1.0f;  // 前60秒满强度
} else {
    float fadeProgress = (elapsed - fadeThreshold) / fadeStartTime;
    intensity = 1.0f - fadeProgress;  // 60-75秒线性淡化
}
```

### 3. 敌人涂墨接口系统
**新增文件：**
1. **`InkWeaponInterface.java`** - 涂墨武器统一接口
2. **`InkHelper.java`** - 静态涂墨工具类
3. **`BaseEnemyInkWeapon.java`** - 敌人武器抽象基类
4. **`EnemyFlameWeapon.java`** - 火焰武器实现
5. **`EnemyProjectileWeapon.java`** - 弹道武器实现
6. **`EnemyAreaWeapon.java`** - 区域涂墨武器实现
7. **`EnemyInkExamples.java`** - 完整使用示例

## 接口设计特点

### InkWeaponInterface（统一接口）
```java
public interface InkWeaponInterface {
    int getFactionId();
    void setFactionId(int factionId);
    float getInkRadius();
    void setInkRadius(float radius);
    void triggerInk(Vector3f position, SparseGridManager gridManager);
    String getWeaponType();
    boolean isInkEnabled();
    void setInkEnabled(boolean enabled);
}
```

### InkHelper（静态工具类）
**基础涂墨方法：**
- `inkPoint()` - 单点涂墨
- `inkArea()` - 区域涂墨
- `inkAndIgniteArea()` - 区域涂墨 + 点燃

**复用玩家武器逻辑：**
- `inkFlamePattern()` - 复用 Gun1 的火焰散射效果
- `inkProjectileTrail()` - 复用 Gun2 的弹道轨迹效果

**特殊效果：**
- `inkRing()` - 环形涂墨（爆炸、冲击波）
- `inkLine()` - 线性涂墨（墙壁、激光）

**状态检查：**
- `hasInkAt()` - 检查是否有墨水
- `getInkFactionAt()` - 获取墨水阵营
- `isEnemyInkAt()` - 检查是否为敌方墨水

### 敌人武器类层次
```
InkWeaponInterface (接口)
    └── BaseEnemyInkWeapon (抽象基类)
            ├── EnemyFlameWeapon (火焰武器)
            ├── EnemyProjectileWeapon (弹道武器)
            └── EnemyAreaWeapon (区域武器)
```

## 使用示例

### 创建敌人火焰武器
```java
EnemyFlameWeapon weapon = new EnemyFlameWeapon(FactionRegistry.DARK_DEFAULT);
weapon.setInkRadius(0.6f);
weapon.setCooldown(1.5f);

// 敌人攻击时
weapon.fire(enemyPos, targetDirection, gridManager);
```

### 创建敌人弹道武器
```java
EnemyProjectileWeapon weapon = new EnemyProjectileWeapon(FactionRegistry.DARK_DEFAULT);

// 自定义弹道参数
InkHelper.ProjectilePreset preset = new InkHelper.ProjectilePreset();
preset.radius = 0.5f;
preset.interval = 0.2f;
preset.maxDistance = 8.0f;
weapon.setProjectilePreset(preset);

// 敌人攻击时
weapon.fire(enemyPos, targetDirection, gridManager);
```

### 自爆敌人
```java
EnemyAreaWeapon weapon = new EnemyAreaWeapon(FactionRegistry.DARK_DEFAULT);
weapon.setInkRadius(2.0f);
weapon.setShouldIgnite(true);

// 敌人死亡时
weapon.fireAt(deathPos, gridManager);
```

### 使用静态方法
```java
// 火焰散射
InkHelper.inkFlamePattern(origin, direction, factionId, gridManager);

// 弹道轨迹
InkHelper.inkProjectileTrail(startPos, endPos, factionId, gridManager);

// 环形冲击波
InkHelper.inkRing(center, 1.0f, 3.0f, factionId, gridManager);

// 线性涂墨
InkHelper.inkLine(startPos, endPos, 0.5f, factionId, gridManager);
```

### 简单AI集成
```java
public class SimpleEnemyAI {
    private BaseEnemyInkWeapon weapon;
    private float cooldownTimer = 0f;

    public void update(float tpf, Vector3f enemyPos, Vector3f targetPos) {
        cooldownTimer -= tpf;
        
        if (cooldownTimer <= 0) {
            float distance = enemyPos.distance(targetPos);
            
            if (distance <= weapon.getRange()) {
                Vector3f direction = targetPos.subtract(enemyPos).normalize();
                weapon.fire(enemyPos, direction, gridManager);
                cooldownTimer = weapon.getCooldown();
            }
        }
    }
}
```

## 扩展性设计

### 完整的钩子系统
敌人武器系统提供了全面的扩展点：

1. **自定义武器类型**
   - 继承 `BaseEnemyInkWeapon`
   - 实现 `fire()` 方法
   - 定义 `getCooldown()` 和 `getRange()`

2. **预设配置系统**
   - `FlamePreset` - 火焰参数
   - `ProjectilePreset` - 弹道参数
   - `AreaPreset` - 区域参数

3. **复用玩家武器代码**
   - `InkHelper.inkFlamePattern()` 复用 Gun1 逻辑
   - `InkHelper.inkProjectileTrail()` 复用 Gun2 逻辑
   - 保持行为一致性

## 技术细节

### 墨水生命周期
```
涂墨时刻 (t=0)
    ↓
满强度阶段 (0-60秒)
    intensity = 1.0
    ↓
淡化阶段 (60-75秒)
    intensity = 1.0 → 0.0 (线性)
    ↓
清除 (t=75秒)
    cell.clear()
```

### 性能考虑
- 所有敌人武器使用相同的 `SparseGridManager`
- 涂墨操作复用现有网格系统
- 无需额外内存分配
- 区域更新仅在有墨水时触发

### 阵营系统集成
- 敌人武器使用 `FactionRegistry` 管理阵营关系
- 支持多阵营敌人（不同颜色）
- 自动处理速度倍率（己方加速、敌方减速）

## 后续扩展建议

1. **可视化增强**
   - 根据 `intensity` 值调整墨水透明度
   - 淡化时的颜色过渡效果

2. **更多武器类型**
   - 激光扫射武器
   - 迫击炮武器
   - 毒雾扩散武器

3. **AI行为树集成**
   - 将武器系统整合到敌人行为树
   - 支持智能选择武器类型
   - 基于环境的战术决策

4. **特殊机关**
   - 自动炮塔
   - 涂墨喷泉
   - 环境陷阱

## 文件清单

### 修改的文件
- `src/main/java/com/Hecate/ink/SparseGridManager.java`
- `src/main/java/com/Hecate/ink/GridRegion.java`
- `src/main/java/com/Hecate/ink/GridCell.java`

### 新增的文件
- `src/main/java/com/Hecate/ink/InkWeaponInterface.java`
- `src/main/java/com/Hecate/ink/InkHelper.java`
- `src/main/java/com/Hecate/ink/BaseEnemyInkWeapon.java`
- `src/main/java/com/Hecate/ink/EnemyFlameWeapon.java`
- `src/main/java/com/Hecate/ink/EnemyProjectileWeapon.java`
- `src/main/java/com/Hecate/ink/EnemyAreaWeapon.java`
- `src/main/java/com/Hecate/ink/EnemyInkExamples.java`

## 测试建议

1. **墨水持续时间测试**
   - 涂一块地，观察是否在75秒后消失
   - 检查60-75秒之间的淡化效果

2. **敌人武器测试**
   - 创建测试敌人，装备不同武器类型
   - 验证涂墨效果与玩家武器一致

3. **性能测试**
   - 生成大量敌人同时涂墨
   - 监控帧率和内存占用

4. **阵营关系测试**
   - 多阵营敌人互相涂墨
   - 验证玩家在不同阵营墨水上的速度

---
**更新完成！** 🎮
