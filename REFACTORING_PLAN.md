# 模型与动画系统全面重构计划

## 重构目标
1. 引入ECS架构解决Bone类职责过重问题
2. 实现世界变换缓存提升性能
3. 添加动画状态机支持混合和过渡
4. 保持向后兼容（现有.puppet文件可加载）
5. 完全分离编辑器和运行时代码

## 重构策略
**新旧并存策略**：
- 保留旧的Bone/Skeleton类仅用于序列化（标记为@Deprecated）
- 创建新的ECS架构类用于运行时
- 提供转换层：加载旧格式 → 转换为新架构 → 运行
- 编辑器继续使用EditorBone，保存时转换为旧格式（兼容性）

---

## 阶段1: 核心ECS架构（第1-3天）

### 1.1 创建核心ECS基础设施

**新建文件**:
- `src/main/java/com/Hecate/puppet/ecs/Entity.java` - 实体基类
- `src/main/java/com/Hecate/puppet/ecs/Component.java` - 组件接口
- `src/main/java/com/Hecate/puppet/ecs/System.java` - 系统接口
- `src/main/java/com/Hecate/puppet/ecs/ComponentManager.java` - 组件管理器
- `src/main/java/com/Hecate/puppet/ecs/SystemManager.java` - 系统管理器

### 1.2 创建骨骼实体和核心组件

**新建文件**:
- `src/main/java/com/Hecate/puppet/ecs/entity/BoneEntity.java` - 骨骼实体
- `src/main/java/com/Hecate/puppet/ecs/component/TransformComponent.java` - 变换组件（含缓存）
- `src/main/java/com/Hecate/puppet/ecs/component/HierarchyComponent.java` - 层级关系组件
- `src/main/java/com/Hecate/puppet/ecs/component/RenderComponent.java` - 渲染基础组件

### 1.3 创建渲染策略组件

**新建文件**:
- `src/main/java/com/Hecate/puppet/ecs/component/render/SpriteRenderData.java`
- `src/main/java/com/Hecate/puppet/ecs/component/render/RotationStripRenderData.java`
- `src/main/java/com/Hecate/puppet/ecs/component/render/Model3DRenderData.java`

---

## 阶段2: 动画系统重构（第4-6天）

### 2.1 动画状态机

**新建文件**:
- `src/main/java/com/Hecate/puppet/animation2/AnimationStateMachine.java`
- `src/main/java/com/Hecate/puppet/animation2/AnimationState.java`
- `src/main/java/com/Hecate/puppet/animation2/StateTransition.java`
- `src/main/java/com/Hecate/puppet/animation2/TransitionCondition.java`

### 2.2 动画混合系统

**新建文件**:
- `src/main/java/com/Hecate/puppet/animation2/BlendTree.java`
- `src/main/java/com/Hecate/puppet/animation2/BlendNode.java`
- `src/main/java/com/Hecate/puppet/animation2/AnimationBlender.java`

### 2.3 保留旧动画类，添加转换

**修改文件**:
- `AnimationClip.java` - 添加@Deprecated，保留用于序列化
- `AnimationPlayer.java` - 标记为旧版本
- 创建 `AnimationConverter.java` - 旧格式转新格式

---

## 阶段3: 系统实现（第7-10天）

### 3.1 核心系统

**新建文件**:
- `src/main/java/com/Hecate/puppet/ecs/system/TransformSystem.java` - 变换更新（含缓存）
- `src/main/java/com/Hecate/puppet/ecs/system/HierarchySystem.java` - 层级管理
- `src/main/java/com/Hecate/puppet/ecs/system/AnimationSystem.java` - 动画播放

### 3.2 物理和特效系统

**新建文件**:
- `src/main/java/com/Hecate/puppet/ecs/component/PhysicsComponent.java`
- `src/main/java/com/Hecate/puppet/ecs/component/SwingComponent.java`
- `src/main/java/com/Hecate/puppet/ecs/system/PhysicsSystem.java`
- `src/main/java/com/Hecate/puppet/ecs/system/SwingSystem.java`

### 3.3 渲染系统

**新建文件**:
- `src/main/java/com/Hecate/puppet/ecs/system/RenderSystem.java`
- `src/main/java/com/Hecate/puppet/ecs/system/ShadowCasterSystem.java`

---

## 阶段4: 转换层和兼容性（第11-12天）

### 4.1 序列化兼容层

**新建文件**:
- `src/main/java/com/Hecate/puppet/compat/LegacyBoneAdapter.java`
- `src/main/java/com/Hecate/puppet/compat/BoneToEntityConverter.java`
- `src/main/java/com/Hecate/puppet/compat/EntityToBoneConverter.java`

### 4.2 修改加载器

**修改文件**:
- `PuppetIO.java` - 添加转换逻辑
- `PuppetPackageIO.java` - 支持新旧格式

---

## 阶段5: 迁移现有代码（第13-14天）

### 5.1 更新PuppetRenderer

**修改文件**:
- `PuppetRenderer.java` - 使用新的ECS系统
- `PuppetPartRenderer.java` - 简化为纯渲染职责

### 5.2 更新玩家控制器

**修改文件**:
- `PuppetPlayerController.java` - 使用BoneEntity

---

## 实现细节

### TransformComponent 设计（解决问题3：缓存）

```java
public class TransformComponent implements Component {
    // Local transform
    private Vector3f localPosition;
    private Quaternion localRotation;
    private Vector3f localScale;
    
    // Cached world transform
    private Vector3f worldPosition;
    private Quaternion worldRotation;
    private Vector3f worldScale;
    
    // Dirty flag
    private boolean worldTransformDirty = true;
    
    public void markDirty() {
        worldTransformDirty = true;
        // 通知所有子节点也变脏
    }
    
    public void getWorldTransform(Vector3f outPos, Quaternion outRot, Vector3f outScale) {
        if (!worldTransformDirty) {
            outPos.set(worldPosition);
            outRot.set(worldRotation);
            outScale.set(worldScale);
            return;
        }
        // 计算并缓存
        computeWorldTransform();
        worldTransformDirty = false;
        outPos.set(worldPosition);
        outRot.set(worldRotation);
        outScale.set(worldScale);
    }
}
```

### AnimationStateMachine 设计（解决问题2）

```java
public class AnimationStateMachine {
    private Map<String, AnimationState> states;
    private AnimationState currentState;
    private AnimationState targetState;
    private float transitionProgress;
    private float transitionDuration;
    
    public void transitionTo(String stateName, float duration) {
        targetState = states.get(stateName);
        transitionDuration = duration;
        transitionProgress = 0f;
    }
    
    public void update(float tpf) {
        if (targetState != null) {
            // 混合当前状态和目标状态
            transitionProgress += tpf / transitionDuration;
            if (transitionProgress >= 1.0f) {
                currentState = targetState;
                targetState = null;
            }
        }
        currentState.update(tpf);
    }
}
```

### RenderComponent 策略模式（解决问题1）

```java
public class RenderComponent implements Component {
    private RenderStrategy strategy;
    
    public interface RenderStrategy {
        void render(RenderContext context);
        String getTexturePath();
    }
}

// 不同的渲染策略作为独立类
public class SpriteRenderData implements RenderStrategy { ... }
public class RotationStripRenderData implements RenderStrategy { ... }
public class Model3DRenderData implements RenderStrategy { ... }
```

---

## 向后兼容保证

1. **序列化兼容**：
   - 旧的Bone/Skeleton类保留，仅用于JSON序列化
   - PuppetIO加载后自动转换为新的BoneEntity
   - 编辑器保存时转换回旧格式

2. **API兼容**：
   - 保留旧类的公共API，内部委托给新系统
   - 标记为@Deprecated，引导开发者使用新API

3. **渐进迁移**：
   - 新旧系统可并存
   - 可以逐个模块迁移，不需要一次性全部改完

---

## 性能改进预期

1. **世界变换计算**：从 O(n*m) 降至 O(n)
   - 10个骨骼，每帧5次查询：50次计算 → 10次计算
   - **预期提升**: 5倍性能

2. **动画采样**：从 O(n) 降至 O(log n)
   - 100个关键帧：100次比较 → 7次比较
   - **预期提升**: 14倍性能

3. **内存占用**：
   - 每个Bone减少约400字节（延迟初始化Map）
   - 100个骨骼节省约40KB

---

## 测试计划

### 单元测试
- TransformComponent缓存正确性
- AnimationStateMachine状态转换
- BoneToEntityConverter转换准确性

### 集成测试
- 加载旧.puppet文件验证兼容性
- 动画播放流畅度测试
- 性能基准测试（对比重构前后）

### 回归测试
- 所有现有游戏功能正常工作
- 编辑器保存/加载正常

---

## 风险和缓解

### 风险1：破坏现有功能
**缓解**：
- 新旧系统并存，逐步迁移
- 完整的单元测试和集成测试
- 代码审查

### 风险2：性能不如预期
**缓解**：
- 性能基准测试
- 可以回退到旧系统

### 风险3：工作量超出预期
**缓解**：
- 分阶段实施，每个阶段可独立发布
- 优先实现核心功能

---

## 时间表

- **第1-3天**: 核心ECS架构
- **第4-6天**: 动画系统
- **第7-10天**: 系统实现
- **第11-12天**: 转换层
- **第13-14天**: 迁移和测试

**总计**: 14个工作日（约3周）

---

## 成功标准

1. ✅ 所有旧.puppet文件可以加载
2. ✅ 世界变换计算性能提升5倍以上
3. ✅ 支持动画混合和状态转换
4. ✅ 代码行数减少30%以上
5. ✅ 所有现有功能正常工作
6. ✅ 编辑器保存/加载正常

---

**开始日期**: 2026-09-13  
**预计完成**: 2026-10-04
