# 🎮 Hecate - 模块化体素游戏引擎

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![jMonkeyEngine](https://img.shields.io/badge/jMonkeyEngine-3.5.2-blue.svg)](https://jmonkeyengine.org/)
[![License](https://img.shields.io/badge/license-MIT-green.svg)](LICENSE)
[![Tests](https://img.shields.io/badge/tests-27%20passing-brightgreen.svg)](TEST_README.md)

一个基于 **jMonkeyEngine 3.5.2** 的现代化、模块化体素游戏引擎，专为可扩展性和可维护性而设计。

## ✨ 特性

### 🏗️ 核心系统
- **模块化架构** - 插件式模块系统，支持动态加载和依赖管理
- **体素世界** - 基于区块(Chunk)的无限世界生成
- **多模型支持** - 支持 Blender 和 Blockbench 模型格式
- **物理引擎** - AABB 碰撞检测和物理模拟
- **玩家系统** - 第三人称控制、动画、血量系统

### 🎨 渲染系统
- 区块优化渲染
- 动态光照系统
- 纹理管理和UV映射
- 2D精灵动画系统

### 🔧 开发者友好
- 统一的日志系统
- 完整的单元测试 (27个测试用例)
- 清晰的API文档
- 抽象基类支持快速扩展

---

## 📦 快速开始

### 前置要求
- Java 17 或更高版本
- Maven 3.6+
- 2GB+ RAM

### 安装

```bash
# 克隆项目
git clone <repository-url>
cd game1(1)

# 编译项目
mvn clean compile

# 运行游戏
mvn exec:java -Dexec.mainClass="com.Hecate.Main"

# 运行测试
mvn test
```

### 打包

```bash
# 打包成可执行JAR
mvn clean package

# 运行打包后的JAR
java -jar target/modularHecate-1.0-SNAPSHOT.jar
```

---

## 🎮 游戏控制

| 按键 | 功能 |
|------|------|
| **W/A/S/D** | 玩家移动 |
| **鼠标** | 视角控制 |
| **左键** | 破坏方块 |
| **右键** | 放置方块 |
| **1/2/3/4** | 选择方块类型 |
| **R** | 重置相机 |
| **Shift** | 冲刺 |
| **Space** | 跳跃 |

---

## 🏛️ 项目架构

```
com.Hecate/
├── registry/          # 模型注册表系统
│   └── AbstractModelRegistry
├── loader/            # 资源加载器系统
│   └── AbstractAssetLoader
├── placer/            # 模型放置器系统
│   └── AbstractModelPlacer
├── model/             # 模型数据结构
│   └── AbstractModel
├── module/            # 模块系统
│   ├── blender/       # Blender模型支持
│   ├── blockbench/    # Blockbench模型支持
│   ├── player/        # 玩家控制模块
│   └── world/         # 世界管理模块
├── block/             # 方块系统
├── world/             # 世界生成和管理
├── player/            # 玩家相关
├── physics/           # 物理引擎
├── ui/                # 用户界面
└── utils/             # 工具类
```

详细架构说明请参阅 [ARCHITECTURE.md](ARCHITECTURE.md)

---

## 📚 文档

| 文档 | 说明 |
|------|------|
| [ARCHITECTURE.md](ARCHITECTURE.md) | 架构设计和技术选型 |
| [REFACTORING.md](REFACTORING.md) | 重构历史和改进记录 |
| [API.md](API.md) | API使用指南 |
| [TEST_README.md](TEST_README.md) | 测试文档 |
| [CONTRIBUTING.md](CONTRIBUTING.md) | 贡献指南 |
| [CHANGELOG.md](CHANGELOG.md) | 变更日志 |
| [CLAUDE.md](CLAUDE.md) | Claude Code 集成指南 |

---

## 🧪 测试

本项目包含完整的单元测试套件，覆盖所有核心抽象基类。

```bash
# 运行所有测试
mvn test

# 运行特定测试
mvn test -Dtest=AbstractModelRegistryTest

# 查看测试报告
# 报告位置: target/surefire-reports/
```

**测试统计**: 27个测试用例，覆盖4个抽象基类

详细测试说明请参阅 [TEST_README.md](TEST_README.md)

---

## 🚀 技术栈

### 核心依赖
- **jMonkeyEngine 3.5.2** - 3D游戏引擎
- **jBullet** - 物理引擎
- **Google Guava** - 工具库
- **SLF4J + Logback** - 日志系统

### 开发工具
- **JUnit 5** - 单元测试框架
- **Mockito** - Mock框架
- **Maven** - 构建工具

---

## 🎯 主要特性演示

### 1. 模块化架构

轻松添加新模型类型：

```java
public class FBXModelRegistry extends AbstractModelRegistry<FBXModel> {
    @Override
    protected boolean isModelLoaded(FBXModel model) {
        return model.isLoaded();
    }

    @Override
    protected String getModelTypeName() {
        return "FBX";
    }
}
```

### 2. 统一的资源加载

```java
// 加载Blender模型
BlenderAssetLoader loader = new BlenderAssetLoader(assetManager);
BlenderModel model = loader.loadSimpleModel("tree", "Models/tree.j3o");

// 加载带纹理的模型
Map<String, String> textures = BlenderAssetLoader.createTextureMap(
    "bark", "Textures/tree_bark.png",
    "leaves", "Textures/tree_leaves.png"
);
loader.loadModelWithTextures("oak_tree", "Models/oak.j3o", textures);
```

### 3. 智能日志系统

```java
// 统一的日志管理


---

## 🔄 最近更新

### 2025-10-13 - 重大重构
- ✅ 创建4个抽象基类统一系统架构
- ✅ 消除300+行重复代码
- ✅ 统一日志管理系统
- ✅ 添加27个单元测试
- ✅ 完善项目文档

详细变更请查看 [CHANGELOG.md](CHANGELOG.md)

---

## 🤝 贡献

欢迎贡献代码、报告问题或提出建议！

1. Fork 本项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

详细贡献指南请参阅 [CONTRIBUTING.md](CONTRIBUTING.md)

---

## 📊 项目统计

| 指标 | 数值 |
|------|------|
| 代码行数 | ~15,000+ |
| Java文件 | 65+ |
| 抽象基类 | 4 |
| 模块数量 | 6 |
| 测试用例 | 27 |
| 测试覆盖 | 核心类100% |

---

## 🛠️ 开发路线图

### 近期计划
- [ ] 添加更多方块类型
- [ ] 实现物品栏系统
- [ ] 添加生物系统
- [ ] 优化区块渲染性能
- [ ] 添加音效系统

### 长期计划
- [ ] 多人游戏支持
- [ ] 服务器/客户端架构
- [ ] 模组API
- [ ] 关卡编辑器
- [ ] 移动平台支持

---

## 📄 许可证

本项目采用 MIT 许可证 - 详见 [LICENSE](LICENSE) 文件

---

## 🙏 致谢

- [jMonkeyEngine](https://jmonkeyengine.org/) - 强大的开源3D游戏引擎
- [Google Guava](https://github.com/google/guava) - 优秀的Java工具库
- [JUnit](https://junit.org/) - 可靠的测试框架

---

## 📞 联系方式

- 项目主页: [GitHub Repository](#)
- 问题反馈: [Issues](#)
- 讨论区: [Discussions](#)

---

## 🌟 Star History

如果这个项目对你有帮助，请给个 Star ⭐️

---

**最后更新**: 2025-10-13
**版本**: 1.0-SNAPSHOT
**状态**: 🟢 活跃开发中
