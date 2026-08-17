# 🏛️ Hecate 架构设计文档

> 详细描述Hecate游戏引擎的架构设计、技术选型和设计模式

**版本**: 1.0
**最后更新**: 2025-10-13
**作者**: Hecate开发团队

---

## 📋 目录

1. [架构概览](#架构概览)
2. [核心设计原则](#核心设计原则)
3. [系统架构](#系统架构)
4. [模块详解](#模块详解)
5. [数据流](#数据流)
6. [设计模式](#设计模式)
7. [性能优化](#性能优化)
8. [扩展性设计](#扩展性设计)

---

## 🎯 架构概览

Hecate采用**分层模块化架构**，将游戏引擎分为多个独立但协作的模块。

### 架构图

```
┌─────────────────────────────────────────────────────────┐
│                    应用层 (Main)                        │
├─────────────────────────────────────────────────────────┤
│                    模块层 (Modules)                     │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐             │
│  │  World   │  │  Player  │  │  Block   │    ...      │
│  │  Module  │  │  Module  │  │  Module  │             │
│  └──────────┘  └──────────┘  └──────────┘             │
├─────────────────────────────────────────────────────────┤
│                    核心层 (Core)                        │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐             │
│  │ Registry │  │  Loader  │  │  Placer  │             │
│  └──────────┘  └──────────┘  └──────────┘             │
├─────────────────────────────────────────────────────────┤
│                   基础层 (Foundation)                   │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐             │
│  │  Model   │  │ Physics  │  │   Utils  │             │
│  └──────────┘  └──────────┘  └──────────┘             │
├─────────────────────────────────────────────────────────┤
│                jMonkeyEngine 3.5.2                      │
└─────────────────────────────────────────────────────────┘
```

---

## 💡 核心设计原则

### 1. **SOLID 原则**

#### 单一职责原则 (SRP)
每个类只负责一个功能：
- `AbstractModelRegistry` 只负责模型注册管理
- `AbstractAssetLoader` 只负责资源加载
- `AbstractModelPlacer` 只负责模型放置

#### 开闭原则 (OCP)
对扩展开放，对修改封闭：
- 抽象基类定义接口，子类实现具体逻辑
- 新增模型类型无需修改现有代码

#### 里氏替换原则 (LSP)
子类可以替换父类：
- 所有 `AbstractModelRegistry` 的子类都可互换使用

#### 接口隔离原则 (ISP)
接口细粒度分离：
- 模块系统提供 `GameModule` 接口
- 各个模块只实现需要的方法

#### 依赖倒置原则 (DIP)
依赖抽象而非具体实现：
- 使用 `AbstractModelRegistry<T>` 而非具体注册表
- 使用接口而非具体类

### 2. **DRY 原则 (Don't Repeat Yourself)**

通过抽象基类消除重复代码：
- 4个抽象基类消除了300+行重复代码
- 统一的日志系统避免重复的日志代码

### 3. **关注点分离**

- **模型数据** (`AbstractModel`) - 只关心数据结构
- **模型注册** (`AbstractModelRegistry`) - 只关心注册管理
- **资源加载** (`AbstractAssetLoader`) - 只关心加载逻辑
- **模型放置** (`AbstractModelPlacer`) - 只关心场景放置

---

## 🏗️ 系统架构

### 1. 分层架构

#### 应用层 (Application Layer)
**职责**: 初始化和协调各个模块

**核心类**:
- `Main.java` - 应用入口，负责初始化和生命周期管理

**关键代码**:
```java
public class Main extends SimpleApplication {
    private WorldModule worldModule;
    private PlayerControlModule playerControlModule;

    @Override
    public void simpleInitApp() {
        initializeModules();
        connectSystems();
    }
}
```

---

#### 模块层 (Module Layer)
**职责**: 提供独立的游戏功能模块

**核心模块**:

| 模块 | 职责 | 依赖 |
|------|------|------|
| **WorldModule** | 世界生成和管理 | ChunkManager, TerrainGenerator |
| **PlayerControlModule** | 玩家控制 | PlayerController, PlayerModel |
| **BlenderModule** | Blender模型支持 | BlenderAssetLoader, BlenderModelRegistry |
| **BlockbenchModule** | Blockbench模型支持 | BlockbenchAssetLoader |

**模块生命周期**:
```
onLoad() → onInitialize() → onPostInitialize() → onUpdate() → onDisable()
```

---

#### 核心层 (Core Layer)
**职责**: 提供通用的抽象功能

**核心抽象类**:

1. **AbstractModelRegistry<T>**
   ```java
   public abstract class AbstractModelRegistry<T> {
       protected final Map<String, T> models = new HashMap<>();

       public void registerModel(String id, T model);
       public T getModel(String id);
       protected abstract boolean isModelLoaded(T model);
   }
   ```

2. **AbstractAssetLoader<M, R>**
   ```java
   public abstract class AbstractAssetLoader<M, R extends AbstractModelRegistry<M>> {
       protected final AssetManager assetManager;
       protected final R registry;

       public void loadDefaultModels();
       protected abstract void loadModelsImpl();
   }
   ```

3. **AbstractModelPlacer<M, R>**
   ```java
   public abstract class AbstractModelPlacer<M, R extends AbstractModelRegistry<M>> {
       protected final Node worldNode;

       public abstract Object placeModel(String modelId, Vector3f position);
       protected abstract boolean isModelLoaded(M model);
   }
   ```

4. **AbstractModel**
   ```java
   public abstract class AbstractModel {
       protected final String id;
       protected final String modelPath;
       protected Spatial spatial;
       protected boolean loaded;
   }
   ```

---

#### 基础层 (Foundation Layer)
**职责**: 提供最底层的工具和数据结构

**核心组件**:
- **Physics** - AABB碰撞检测、CollisionManager
- **Utils** - LogUtils、BlockUtils
- **Block** - 方块定义、纹理管理
- **World** - Chunk、ChunkPosition

---

### 2. 模块系统架构

```
┌─────────────────────────────────────────┐
│         ModuleManager                    │
│  ┌─────────────────────────────────┐   │
│  │  依赖解析 (Dependency Resolution) │   │
│  │  版本控制 (Version Control)        │   │
│  │  冲突检测 (Conflict Detection)     │   │
│  └─────────────────────────────────┘   │
└─────────────────────────────────────────┘
                   │
        ┌──────────┼──────────┐
        ▼          ▼          ▼
   ┌────────┐ ┌────────┐ ┌────────┐
   │ Module │ │ Module │ │ Module │
   │   A    │ │   B    │ │   C    │
   └────────┘ └────────┘ └────────┘
```

**模块接口**:
```java
public interface GameModule {
    String getId();
    Version getVersion();
    void onLoad();
    void onInitialize();
    void onPostInitialize();
    void onUpdate(float tpf);
    void onDisable();
}
```

---

## 📦 模块详解

### 1. World Module (世界模块)

**职责**: 管理游戏世界的生成、渲染和更新

**核心组件**:

```
WorldModule
├── ChunkManager         # 区块管理器
│   ├── loadChunk()
│   ├── unloadChunk()
│   └── getChunk()
├── TerrainGenerator     # 地形生成器
│   └── generateChunk()
├── ChunkRenderer        # 区块渲染器
│   └── renderChunk()
└── ChunkVisibilityManager  # 可见性管理
    └── updateVisibleChunks()
```

**数据结构**:
```java
public class Chunk {
    private static final int SIZE = 16;
    private final int[][][] blocks = new int[SIZE][SIZE][SIZE];
    private final ChunkPosition position;
    private Node chunkNode;
}
```

**渲染优化**:
- 只渲染可见面
- 区块批处理
- 视锥体裁剪

---

### 2. Player Module (玩家模块)

**职责**: 处理玩家输入、移动、动画和状态

**核心组件**:

```
PlayerControlModule
├── PlayerController         # 控制器
│   ├── handleInput()
│   ├── updateMovement()
│   └── handleCollision()
├── PlayerModel             # 模型
├── PlayerAnimator          # 动画
│   └── SpriteAnimationSystem
├── PlayerHealth            # 血量
└── PlayerSpriteManager     # 精灵管理
```

**状态机**:
```
        ┌─────┐
        │IDLE │◄──────┐
        └─────┘       │
           │          │
      [Move]│         │[Stop]
           ▼          │
        ┌─────┐       │
        │WALK │───────┘
        └─────┘
           │
     [Sprint]│
           ▼
        ┌─────┐
        │RUN  │
        └─────┘
```

---

### 3. Block Module (方块模块)

**职责**: 方块类型管理、交互和渲染

**核心组件**:

```
BlockModule
├── BlockRegistry           # 方块注册表
├── BlockInteraction        # 交互系统
│   ├── breakBlock()
│   └── placeBlock()
├── BlockBreaking           # 破坏系统
├── TextureManager          # 纹理管理
└── ProceduralBlockGenerator  # 程序化生成
```

**方块定义**:
```java
public class Block {
    private final String id;
    private final BlockTexture texture;
    private final float hardness;
    private final boolean solid;
}
```

---

### 4. Physics Module (物理模块)

**职责**: 碰撞检测和物理模拟

**核心组件**:

```
PhysicsModule
├── CollisionManager        # 碰撞管理器
│   ├── checkCollision()
│   └── resolveCollision()
├── AABB                    # 包围盒
│   ├── intersects()
│   └── contains()
└── PointerSystem           # 射线投射
    └── raycast()
```

**碰撞检测算法**:
```java
public boolean intersects(AABB other) {
    return (this.min.x <= other.max.x && this.max.x >= other.min.x) &&
           (this.min.y <= other.max.y && this.max.y >= other.min.y) &&
           (this.min.z <= other.max.z && this.max.z >= other.min.z);
}
```

---

## 🔄 数据流

### 1. 模型加载流程

```
用户请求
    │
    ▼
AbstractAssetLoader.loadDefaultModels()
    │
    ├──► loadModelsImpl()  [子类实现]
    │
    ├──► loadModelFile(model)
    │       │
    │       ├──► AssetManager.loadModel()
    │       │
    │       └──► model.setSpatial(spatial)
    │
    └──► registry.registerModel(id, model)
```

### 2. 模型放置流程

```
用户调用placeModel()
    │
    ▼
AbstractModelPlacer.getAndValidateModel()
    │
    ├──► registry.getModel(id)
    │
    ├──► isModelLoaded(model) ?
    │       YES │         NO │
    │           ▼            ▼
    │    placeModelImpl()  返回null/false
    │           │
    │           ├──► model.getSpatial().clone()
    │           │
    │           ├──► spatial.setLocalTranslation(position)
    │           │
    │           └──► worldNode.attachChild(spatial)
```

### 3. 区块渲染流程

```
ChunkManager.loadChunk(position)
    │
    ├──► TerrainGenerator.generateChunk()
    │       │
    │       └──► Chunk.fillWithBlocks()
    │
    ├──► ChunkRenderer.renderChunk(chunk)
    │       │
    │       ├──► 遍历所有方块
    │       │
    │       ├──► 只渲染可见面
    │       │
    │       └──► 创建Mesh并应用纹理
    │
    └──► worldNode.attachChild(chunkNode)
```

---

## 🎨 设计模式

### 1. **模板方法模式**

在 `AbstractAssetLoader` 中使用：

```java
public void loadDefaultModels() {
 
    try {
        loadModelsImpl();  // 子类实现具体逻辑
      
    } catch (Exception e) {
      
    }
}
```

**优点**:
- 统一的错误处理
- 统一的日志输出
- 子类只需实现核心逻辑

---

### 2. **注册表模式**

`AbstractModelRegistry` 实现：

```java
public class BlenderModelRegistry extends AbstractModelRegistry<BlenderModel> {
    private static BlenderModelRegistry instance;

    public static synchronized BlenderModelRegistry getInstance() {
        if (instance == null) {
            instance = new BlenderModelRegistry();
        }
        return instance;
    }
}
```

**优点**:
- 集中管理所有模型
- 避免重复加载
- 便于查询和统计

---

### 3. **策略模式**

不同的模型加载器实现不同的加载策略：

```java
// Blender加载器 - 支持动画和纹理
public class BlenderAssetLoader extends AbstractAssetLoader {
    protected void loadModelsImpl() {
        loadAnimatedModel("character", ...);
    }
}

// Blockbench加载器 - 简单OBJ加载
public class BlockbenchAssetLoader extends AbstractAssetLoader {
    protected void loadModelsImpl() {
        loadSimpleModel("block", ...);
    }
}
```

---

### 4. **观察者模式**

事件总线系统：

```java
public class EventBus {
    private final Map<Class<?>, List<Consumer<?>>> listeners = new HashMap<>();

    public <T extends GameEvent> void subscribe(Class<T> eventType, Consumer<T> listener) {
        listeners.computeIfAbsent(eventType, k -> new ArrayList<>()).add(listener);
    }

    public void publish(GameEvent event) {
        // 通知所有监听器
    }
}
```

---

### 5. **工厂模式**

方块生成：

```java
public class ProceduralBlockGenerator {
    public static Geometry createBlock(BlockSize size, ColorRGBA color) {
        Box box = new Box(size.getSize(), size.getSize(), size.getSize());
        Geometry geom = new Geometry("Block", box);
        Material mat = createMaterial(color);
        geom.setMaterial(mat);
        return geom;
    }
}
```

---

## ⚡ 性能优化

### 1. **区块批处理**

将多个方块合并为一个Mesh：

```java
public Mesh createChunkMesh(Chunk chunk) {
    List<Vector3f> vertices = new ArrayList<>();
    List<Integer> indices = new ArrayList<>();

    // 遍历所有方块，合并顶点
    for (int x = 0; x < SIZE; x++) {
        for (int y = 0; y < SIZE; y++) {
            for (int z = 0; z < SIZE; z++) {
                if (isVisible(x, y, z)) {
                    addBlockVertices(vertices, indices, x, y, z);
                }
            }
        }
    }

    return createMesh(vertices, indices);
}
```

**性能提升**: 从16³=4096个Geometry减少到1个Mesh

---

### 2. **视锥体裁剪**

只渲染摄像机视野内的区块：

```java
public void updateVisibleChunks(Camera camera) {
    visibleChunks.clear();
    for (Chunk chunk : loadedChunks) {
        if (isInFrustum(chunk.getBounds(), camera)) {
            visibleChunks.add(chunk);
        }
    }
}
```

---

### 3. **延迟加载**

只在需要时加载资源：

```java
public Spatial getSpatial() {
    if (spatial == null && !loading) {
        loading = true;
        CompletableFuture.runAsync(() -> loadSpatial());
    }
    return spatial;
}
```

---

### 4. **对象池**

复用频繁创建的对象：

```java
public class Vector3fPool {
    private final Queue<Vector3f> pool = new ConcurrentLinkedQueue<>();

    public Vector3f obtain() {
        Vector3f v = pool.poll();
        return v != null ? v : new Vector3f();
    }

    public void free(Vector3f v) {
        v.set(0, 0, 0);
        pool.offer(v);
    }
}
```

---

## 🔌 扩展性设计

### 1. **添加新模型类型**

只需3步：

```java
// 1. 创建模型类
public class FBXModel extends AbstractModel {
    @Override
    public String getTypeName() { return "FBX"; }
}

// 2. 创建注册表
public class FBXModelRegistry extends AbstractModelRegistry<FBXModel> {
    @Override
    protected boolean isModelLoaded(FBXModel model) {
        return model.isLoaded();
    }
}

// 3. 创建加载器
public class FBXAssetLoader extends AbstractAssetLoader<FBXModel, FBXModelRegistry> {
    @Override
    protected void loadModelsImpl() {
        // 加载FBX模型
    }
}
```

---

### 2. **添加新模块**

实现 `GameModule` 接口：

```java
public class NPCModule extends AbstractGameModule {
    @Override
    public String getId() { return "npc"; }

    @Override
    public void onInitialize() {
        // 初始化NPC系统
    }

    @Override
    public void onUpdate(float tpf) {
        // 更新NPC逻辑
    }
}
```

---

### 3. **模块间通信**

使用事件总线：

```java
// 发布事件
eventBus.publish(new PlayerDamagedEvent(player, damage));

// 订阅事件
eventBus.subscribe(PlayerDamagedEvent.class, event -> {
    // 处理玩家受伤事件
    updateHealthUI(event.getNewHealth());
});
```

---

## 📊 性能指标

### 目标性能

| 指标 | 目标值 | 当前值 |
|------|--------|--------|
| FPS | 60 | ~55-60 |
| 内存占用 | <2GB | ~1.5GB |
| 区块加载时间 | <100ms | ~80ms |
| 启动时间 | <5s | ~4s |

---

## 🔮 未来架构改进

### 近期计划
- [ ] 实现ECS (Entity Component System) 架构
- [ ] 添加多线程区块加载
- [ ] 实现网络层架构

### 长期计划
- [ ] 微服务化架构 (服务器/客户端分离)
- [ ] 插件系统 (热加载)
- [ ] 脚本支持 (Lua/JavaScript)

---

**文档维护**: 请在架构变更时及时更新本文档

**反馈**: 如有架构相关问题，请提交Issue
