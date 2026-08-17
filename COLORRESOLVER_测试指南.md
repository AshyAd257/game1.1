# ColorResolver 测试指南

## 测试场景设置

### 默认配置
- **玩家阵营**: 暗属性 (DARK_DEFAULT)
- **测试场景**: 世界铺满光属性墨水（原点周围50米范围）
- **预期效果**: 暗属性玩家看光属性领地应该是**刺眼/bloom效果**（蓝色变亮发光）

## 已完成的实现

### 核心系统
- ✅ **ColorResolver** - 颜色解析器
  - 中性基准色（非战斗态）
  - 战斗态偏移（bloom 刺眼效果 / darken 昏暗效果）
  - 可调参数预留

- ✅ **GridDebugRenderer** - 网格渲染器集成
  - 调用 ColorResolver 动态计算颜色
  - 支持切换观察者阵营
  - 支持切换战斗状态

- ✅ **PlayerState.isInCombat()** - 战斗状态占位
  - 目前死返回 `true`（便于测试战斗态效果）
  - 未来接入武器/仇恨系统后替换

## 测试操作

### 游戏内快捷键
- **F5**: 切换观察者阵营（光属性 ↔ 暗属性）
- **F6**: 切换战斗状态（开启 ↔ 关闭）

### 测试步骤

1. **启动游戏**
   - 运行 `Main.java` 或使用启动脚本
   - 控制台会显示：
     ```
     [ColorResolver] 调试按键已注册: F5=切换阵营 F6=切换战斗状态
     [ColorResolver测试场景] 开始铺设光属性墨水...
     [ColorResolver测试场景] 场景生成完成！
     ```

2. **观察初始效果**
   - **你是暗属性玩家**
   - 地面应该铺满**刺眼的蓝色墨迹**（bloom效果）
   - 如果看起来不够刺眼，说明需要调整 `BLOOM_INTENSITY_MAX` 参数

3. **切换到光属性视角（F5）**
   - 按下 **F5**，切换到光属性视角
   - 观察墨迹颜色变化：
     - 光属性墨迹（蓝色）应该**变回正常**（己方领地，不刺眼）
   - 再按 **F5** 切回暗属性视角，应该再次变刺眼

4. **切换战斗状态（F6）**
   - 按下 **F6**，关闭战斗状态
   - 观察墨迹颜色变化：
     - 所有墨迹应该回到**中性基准色**（无视觉偏移，正常蓝色）
   - 再次按 **F6** 重新开启战斗状态，bloom效果应该回来

5. **用武器涂抹暗属性墨水**
   - 用武器向地面射击（如果武器有阵营支持）
   - 涂抹暗属性墨迹（橙色）
   - 观察对比：
     - **暗看暗** = 正常橙色（己方领地）
     - **暗看光** = 刺眼蓝色（敌方领地）
     - 按 **F5** 切换视角再次对比

## 预留的调参位置

在 `ColorResolver.java` 中，以下参数可以实机调整：

```java
// 第 23-24 行：中性态亮度
private static final float NEUTRAL_LIGHT_BRIGHTNESS = 0.85f;  // 光领地基准亮度
private static final float NEUTRAL_DARK_BRIGHTNESS = 0.75f;   // 暗领地基准亮度

// 第 27-28 行：战斗态偏移上限
private static final float BLOOM_INTENSITY_MAX = 0.6f;        // bloom 最大增亮幅度
private static final float DARKEN_INTENSITY_MAX = 0.5f;       // darken 最大降暗幅度
```

### 调参建议
1. **NEUTRAL_LIGHT_BRIGHTNESS** (0.85f)
   - 数值越高，光领地越亮
   - 范围建议：0.7 ~ 1.0

2. **NEUTRAL_DARK_BRIGHTNESS** (0.75f)
   - 数值越高，暗领地越亮（"暗但看得清"的程度）
   - 范围建议：0.6 ~ 0.9

3. **BLOOM_INTENSITY_MAX** (0.6f)
   - 暗属性玩家看光领地时的刺眼程度
   - 数值越高越刺眼，但不能让玩家完全看不见
   - 范围建议：0.4 ~ 0.8

4. **DARKEN_INTENSITY_MAX** (0.5f)
   - 光属性玩家看暗领地时的昏暗程度
   - 数值越高越暗
   - 范围建议：0.3 ~ 0.7

## 命令行测试工具

如果想快速验证颜色计算逻辑，无需进入游戏：

```bash
cd "C:\Users\29232\OneDrive\Desktop\game1(1)"
java -cp "target/classes;C:\Users\29232\.m2\repository\org\jmonkeyengine\jme3-core\3.5.2-stable\jme3-core-3.5.2-stable.jar" com.Hecate.ink.ColorResolverTest
```

输出示例：
```
【场景 2】光属性玩家视角
  光看光领地（己方，不偏移）:           R=0.000 G=0.425 B=0.850 A=1.000  |  亮度=0.346
  光看暗领地（敌方，应该变暗）:         R=0.375 G=0.188 B=0.100 A=1.000  |  亮度=0.234
  光看暗领地（非战斗，中性态）:         R=0.750 G=0.375 B=0.000 A=1.000  |  亮度=0.444
```

## 已知限制（TODO）

1. **玩家阵营写死为光属性**
   - 目前 `GridDebugRenderer` 默认观察者阵营为 `LIGHT_DEFAULT`
   - 未来需要从 `PlayerController` 读取实际阵营

2. **战斗状态写死为 true**
   - `PlayerState.isInCombat()` 目前死返回 `true`
   - 未来需要接入武器持有/仇恨系统

3. **点燃效果未集成**
   - `GridCell.isIgnited()` 状态还未在 ColorResolver 中体现
   - 可能需要在 `computeNeutralColor()` 中加强点燃格子的亮度

4. **环境基础层未实现**
   - 树荫/向阳等天然光暗区域还未实现
   - 预留在 GridCell 的 `baseAttribute` 字段（目前未添加）

## 下一步工作

根据 `landandlight.md` 文档第 185-191 行的实现顺序：

- ✅ 1. GridCell 字段改名拆分
- ✅ 2. FactionDef / FactionRegistry / getRelation()
- ✅ 3. ColorResolver 核心算法
- ⏸️ 4. 子弹改为同时调用 inkCircle + spawnDecal（墨迹独立实体化）
- ⏸️ 5. 曝光/gamma 微调（纯数值调参）

当前阶段重点是**实机验证视觉效果**，根据你的感受调整参数。
