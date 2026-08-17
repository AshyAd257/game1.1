# 单例模式重构记录

## 概述

本次重构消除了项目中过度使用单例模式的问题，将核心注册表类从单例模式重构为支持依赖注入，同时保持向后兼容性。

## 重构日期

2026-06-02

## 重构目标

- 消除单例模式带来的问题（测试困难、多世界支持受限、模块化受阻）
- 支持依赖注入，提高代码的可测试性和灵活性
- 保持向后兼容性，允许渐进式迁移

## 已完成的重构

### 1. BlockRegistry.java

**位置**: `src/main/java/com/Hecate/block/BlockRegistry.java`

**改动**:
- 将 `private` 构造函数改为 `public`，允许创建独立实例
- 将 `instance` 字段重命名为 `defaultInstance`
- `getInstance()` 方法标记为 `@Deprecated`，但仍保持功能
- 新增 `getDefaultInstance()` 方法，语义更清晰
- 新增 `createInstance()` 静态工厂方法，用于创建独立实例
- 添加详细的迁移指南文档

**迁移指南**:
```java
// 旧代码（仍可用，但不推荐）
BlockRegistry registry = BlockRegistry.getInstance();

// 新代码（推荐 - 通过构造器创建）
BlockRegistry registry = new BlockRegistry();

// 新代码（获取默认实例）
BlockRegistry registry = BlockRegistry.getDefaultInstance();

// 新代码（创建独立实例用于测试/多世界）
BlockRegistry testRegistry = BlockRegistry.createInstance();
```

### 2. BlenderModelRegistry.java

**位置**: `src/main/java/com/Hecate/blender/BlenderModelRegistry.java`

**改动**: 与 BlockRegistry 相同的重构模式

**迁移指南**:
```java
// 旧代码
BlenderModelRegistry registry = BlenderModelRegistry.getInstance();

// 新代码（推荐）
BlenderModelRegistry registry = new BlenderModelRegistry();
```

### 3. BlockbenchModelRegistry.java

**位置**: `src/main/java/com/Hecate/blockbench/BlockbenchModelRegistry.java`

**改动**: 与 BlockRegistry 相同的重构模式

**迁移指南**:
```java
// 旧代码
BlockbenchModelRegistry registry = BlockbenchModelRegistry.getInstance();

// 新代码（推荐）
BlockbenchModelRegistry registry = new BlockbenchModelRegistry();
```

### 4. Main.java - 依赖注入实现

**位置**: `src/main/java/com/Hecate/Main.java`

**改动**:
1. 添加注册表字段:
   ```java
   private BlockRegistry blockRegistry;
   private BlenderModelRegistry blenderModelRegistry;
   private BlockbenchModelRegistry blockbenchModelRegistry;
   ```

2. 新增 `initializeRegistries()` 方法:
   ```java
   private void initializeRegistries() {
       blockRegistry = new BlockRegistry();
       blenderModelRegistry = new BlenderModelRegistry();
       blockbenchModelRegistry = new BlockbenchModelRegistry();
   }
   ```

3. 在 `simpleInitApp()` 中调用注册表初始化（在模块初始化之前）

4. 添加公共 getter 方法供模块访问:
   ```java
   public BlockRegistry getBlockRegistry()
   public BlenderModelRegistry getBlenderModelRegistry()
   public BlockbenchModelRegistry getBlockbenchModelRegistry()
   ```

5. 修改模块初始化，传递注册表实例:
   ```java
   worldModule = new WorldModule(this, blockRegistry);
   ```

### 5. WorldModule.java - 接收依赖注入

**位置**: `src/main/java/com/Hecate/module/world/WorldModule.java`

**改动**:
1. 添加支持依赖注入的构造函数:
   ```java
   public WorldModule(SimpleApplication app, BlockRegistry blockRegistry) {
       this.app = app;
       this.blockRegistry = blockRegistry;
   }
   ```

2. 保留旧构造函数并标记为 `@Deprecated`:
   ```java
   @Deprecated
   public WorldModule(SimpleApplication app) {
       this.app = app;
       this.blockRegistry = null; // 将在 onInitialize 中初始化为默认实例
   }
   ```

3. 修改 `onInitialize()` 方法，检查注册表是否已注入:
   ```java
   if (blockRegistry == null) {
       blockRegistry = BlockRegistry.getInstance(); // 向后兼容
   }
   blockRegistry.initializeDefaultBlocks(textureManager);
   ```

## 重构优势

### 1. 支持多世界/多存档
```java
// 为不同的世界创建独立的注册表实例
BlockRegistry world1Registry = new BlockRegistry();
BlockRegistry world2Registry = new BlockRegistry();
```

### 2. 支持单元测试
```java
@Test
public void testBlockRegistration() {
    // 每个测试使用独立的注册表实例，互不干扰
    BlockRegistry testRegistry = new BlockRegistry();
    testRegistry.registerBlock(new Block("test_block", "测试方块", true, 1.0f, false));
    assertEquals(1, testRegistry.getAllBlockIds().size());
}
```

### 3. 支持模组/插件系统
```java
// 模组可以创建自己的注册表实例
public class MyMod {
    private final BlockRegistry modRegistry;

    public MyMod() {
        this.modRegistry = new BlockRegistry();
        registerCustomBlocks();
    }
}
```

### 4. 支持编辑器集成
```java
// 编辑器可以创建独立的注册表实例，不影响游戏主世界
public class PuppetEditor {
    private final BlockRegistry editorRegistry = new BlockRegistry();
}
```

## 向后兼容性

所有旧代码仍然可以正常工作：
```java
// 这些代码仍然可以运行，只是会看到 @Deprecated 警告
BlockRegistry registry = BlockRegistry.getInstance();
BlenderModelRegistry blenderRegistry = BlenderModelRegistry.getInstance();
```

IDE 会显示这些方法已被弃用，并建议使用新的构造器方式。

## 未来工作

### 待重构的低优先级单例类

1. **CorpseBlockManager** - 尸块管理器（优先级：低）
2. **CameraDirectionDetector** - 相机方向检测器（优先级：低）
3. **SpriteScaleManager** - 精灵缩放管理器（优先级：低）
4. **LanguageManager** - 语言管理器（优先级：低）

### 推荐的渐进式迁移路径

1. **第一阶段**（已完成）: 重构核心注册表类
   - ✅ BlockRegistry
   - ✅ BlenderModelRegistry
   - ✅ BlockbenchModelRegistry

2. **第二阶段**: 更新所有模块使用依赖注入
   - ✅ Main.java
   - ✅ WorldModule
   - ⏳ PlayerControlModule（如果需要访问注册表）
   - ⏳ 其他自定义模块

3. **第三阶段**: 逐步移除 `getInstance()` 调用
   - 在代码库中搜索所有 `.getInstance()` 调用
   - 逐个替换为依赖注入方式
   - 可以使用 IDE 的"查找用法"功能辅助

4. **第四阶段**: 重构剩余的单例类（如有需要）

## 测试验证

### 编译验证
```bash
mvn clean compile
```

### 运行验证
```bash
mvn exec:java -Dexec.mainClass="com.Hecate.Main"
```

### 预期结果
- 编译无错误
- 运行时行为与之前完全一致
- IDE 显示 `getInstance()` 方法已弃用的警告

## 相关文档

- [架构设计文档](ARCHITECTURE.md)
- [重构日志](REFACTORING_LOG.md)
- [Claude 指导文档](CLAUDE.md)

## 总结

本次重构成功将三个核心注册表类从强制单例模式转换为支持依赖注入的灵活设计，同时保持了完全的向后兼容性。这为未来的功能扩展（多世界、模组系统、单元测试等）奠定了坚实的基础。

重构遵循了以下原则：
1. **渐进式改进** - 不破坏现有代码
2. **清晰的文档** - 提供详细的迁移指南
3. **实用主义** - 保留向后兼容性
4. **长远考虑** - 为未来扩展打下基础
