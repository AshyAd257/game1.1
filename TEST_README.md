# Hecate游戏引擎 - 单元测试文档

## 📋 测试概览

本项目包含针对重构后抽象基类的完整单元测试套件。

### 测试覆盖的类

1. **AbstractModelRegistry** - 模型注册表基类
2. **AbstractModel** - 模型数据基类
3. **AbstractAssetLoader** - 资源加载器基类
4. **AbstractModelPlacer** - 模型放置器基类

## 🚀 运行测试

### 方式1: 运行所有测试
```bash
mvn test
```

### 方式2: 运行特定测试类
```bash
# 测试注册表
mvn test -Dtest=AbstractModelRegistryTest

# 测试模型
mvn test -Dtest=AbstractModelTest

# 测试加载器
mvn test -Dtest=AbstractAssetLoaderTest

# 测试放置器
mvn test -Dtest=AbstractModelPlacerTest
```

### 方式3: 运行测试套件
```bash
mvn test -Dtest=RefactoringTestSuite
```

## 📊 测试统计

| 测试类 | 测试用例数 | 覆盖功能 |
|--------|-----------|---------|
| **AbstractModelRegistryTest** | 8 | 注册、查询、移除、统计 |
| **AbstractModelTest** | 7 | 初始化、Spatial管理、状态 |
| **AbstractAssetLoaderTest** | 6 | 加载、重载、错误处理 |
| **AbstractModelPlacerTest** | 6 | 验证、放置、坐标转换 |
| **总计** | **27** | - |

## 🧪 测试详情

### AbstractModelRegistryTest

测试模型注册表的核心功能：

- ✅ 注册模型
- ✅ 获取模型
- ✅ 移除模型
- ✅ 检查模型是否存在
- ✅ 统计已加载模型
- ✅ 获取所有模型ID
- ✅ 清空所有模型
- ✅ 处理不存在的模型

**关键测试场景**:
```java
@Test
void shouldRegisterModel() {
    TestModel model = new TestModel("test1", true);
    registry.registerModel("test1", model);

    assertTrue(registry.hasModel("test1"));
    assertEquals(model, registry.getModel("test1"));
}
```

---

### AbstractModelTest

测试模型数据类的基础功能：

- ✅ 正确初始化属性
- ✅ Spatial设置与状态同步
- ✅ 加载状态管理
- ✅ ID和路径getter
- ✅ toString方法
- ✅ 类型名称

**关键测试场景**:
```java
@Test
void shouldMarkAsLoadedWhenSpatialIsSet() {
    Spatial spatial = new Geometry("test", new Box(1, 1, 1));
    model.setSpatial(spatial);

    assertTrue(model.isLoaded());
    assertSame(spatial, model.getSpatial());
}
```

---

### AbstractAssetLoaderTest

测试资源加载器的加载逻辑：

- ✅ AssetManager访问
- ✅ 注册表访问
- ✅ 检查已存在模型
- ✅ 重新加载模型
- ✅ 默认模型加载
- ✅ 异常处理

**关键测试场景**:
```java
@Test
void shouldCallLoadModelFileWhenReloadingExistingModel() {
    TestModel model = new TestModel("test", true);
    registry.registerModel("test", model);

    boolean result = loader.reloadModel("test");

    assertTrue(result);
    assertTrue(loader.loadModelFileCalled);
}
```

---

### AbstractModelPlacerTest

测试模型放置器的验证和放置功能：

- ✅ 世界节点访问
- ✅ 注册表访问
- ✅ 模型验证
- ✅ 坐标转换
- ✅ 放置成功/失败
- ✅ 错误情况处理

**关键测试场景**:
```java
@Test
void shouldCallVectorPlaceModelFromCoordinates() {
    TestModel model = new TestModel("test", true);
    registry.registerModel("test", model);

    Object result = placer.placeModel("test", 1.0f, 2.0f, 3.0f);

    assertTrue((Boolean) result);
    assertEquals(1.0f, placer.lastPlacedPosition.x, 0.001f);
}
```

## 🛠️ 技术栈

- **JUnit 5** (5.10.0) - 测试框架
- **Mockito** (5.5.0) - Mock框架
- **Maven Surefire** (3.1.2) - 测试运行器

## 📝 编写新测试

### 测试命名规范

```java
@Test
@DisplayName("应该[期望行为]")
void should[ExpectedBehavior]() {
    // Given - 准备测试数据

    // When - 执行被测方法

    // Then - 验证结果
}
```

### 示例

```java
@Test
@DisplayName("应该在模型不存在时返回null")
void shouldReturnNullWhenModelDoesNotExist() {
    // Given
    // (无需准备，测试空状态)

    // When
    TestModel result = registry.getModel("nonexistent");

    // Then
    assertNull(result);
}
```

## 🔍 测试覆盖率

要生成测试覆盖率报告，添加JaCoCo插件到pom.xml：

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.10</version>
    <executions>
        <execution>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

然后运行：
```bash
mvn clean test jacoco:report
```

报告位置: `target/site/jacoco/index.html`

## ✅ 测试通过标准

所有测试必须：
1. ✅ 测试通过率 100%
2. ✅ 无编译错误
3. ✅ 无运行时异常
4. ✅ 断言全部通过

## 🐛 故障排除

### 常见问题

**问题1: 找不到JUnit**
```bash
# 解决方案: 确保依赖已下载
mvn clean install
```

**问题2: jMonkeyEngine相关错误**
```bash
# 解决方案: 某些测试需要jME3环境，使用Mock代替
```

**问题3: 测试失败**
```bash
# 查看详细错误信息
mvn test -X
```

## 📚 参考资料

- [JUnit 5 用户指南](https://junit.org/junit5/docs/current/user-guide/)
- [Mockito 文档](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)
- [Maven Surefire 插件](https://maven.apache.org/surefire/maven-surefire-plugin/)

## 🎯 持续改进

未来可以添加的测试：

- [ ] BlenderModelRegistry集成测试
- [ ] BlockbenchModelRegistry集成测试
- [ ] 完整的资源加载流程测试
- [ ] 性能基准测试
- [ ] 并发测试

---

**测试编写日期**: 2025-10-13
**最后更新**: 2025-10-13
