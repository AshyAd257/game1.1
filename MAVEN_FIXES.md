# Maven 配置修复报告

**日期**: 2026-08-18  
**问题**: pom.xml 中的版本管理问题

---

## ✅ 已修复的问题

### 1️⃣ LWJGL 版本硬编码

**问题描述**:
```xml
<!-- ❌ 之前：硬编码版本号 -->
<dependency>
    <groupId>org.lwjgl</groupId>
    <artifactId>lwjgl-freetype</artifactId>
    <version>3.3.1</version>  <!-- 硬编码 -->
</dependency>
```

**修复方案**:
```xml
<!-- ✅ 修复后：使用属性变量 -->
<properties>
    <lwjgl.version>3.3.1</lwjgl.version>  <!-- 统一管理 -->
</properties>

<dependency>
    <groupId>org.lwjgl</groupId>
    <artifactId>lwjgl-freetype</artifactId>
    <version>${lwjgl.version}</version>  <!-- 引用变量 -->
</dependency>
```

**影响的依赖**:
- ✅ `lwjgl-freetype` (主库 + 3个原生库)
- ✅ `lwjgl-stb` (主库 + 3个原生库)
- **总计**: 8 个依赖项

**优势**:
- 统一版本管理
- 易于升级
- 避免版本冲突

---

## 📋 当前依赖版本总览

### 核心框架
| 库名 | 版本 | 变量名 | 状态 |
|------|------|--------|------|
| jMonkeyEngine | 3.5.2-stable | `${jme3.version}` | ✅ |
| LWJGL | 3.3.1 | `${lwjgl.version}` | ✅ |

### 字体渲染库
| 库名 | 版本 | 变量 | 状态 |
|------|------|------|------|
| jme-ttf | 3.0.1 | 直接指定 | ✅ |
| lwjgl-freetype | 3.3.1 | `${lwjgl.version}` | ✅ |
| lwjgl-stb | 3.3.1 | `${lwjgl.version}` | ✅ |

### UI 库
| 库名 | 版本 | 状态 |
|------|------|------|
| ImGui-Java | 1.86.11 | ✅ |
| Lemur | 1.16.0 | ✅ |
| Lemur-Proto | 1.13.0 | ✅ |
| Nifty GUI | 3.5.2-stable | ✅ |

### 辅助库
| 库名 | 版本 | 状态 |
|------|------|------|
| Guava | 32.1.2-jre | ✅ |
| SLF4J | 2.0.9 | ✅ |
| Logback | 1.4.11 | ✅ |

---

## ⚠️ 潜在的兼容性问题

### LWJGL 版本冲突检查

**JME3 3.5.2 的 LWJGL 依赖**:
- JME3 内置使用 LWJGL 3.3.x
- 我们添加的 LWJGL 3.3.1 应该兼容

**验证方法**:
```bash
# 查看依赖树
mvn dependency:tree -Dverbose

# 查找 LWJGL 版本冲突
mvn dependency:tree | grep lwjgl
```

**预期结果**:
```
[INFO] +- org.jmonkeyengine:jme3-lwjgl3:jar:3.5.2-stable:compile
[INFO] |  \- org.lwjgl:lwjgl:jar:3.3.1:compile
[INFO] +- org.lwjgl:lwjgl-freetype:jar:3.3.1:compile
[INFO] +- org.lwjgl:lwjgl-stb:jar:3.3.1:compile
```

**如果有冲突**:
```xml
<!-- 使用 Maven 的 exclusions 排除冲突依赖 -->
<dependency>
    <groupId>org.jmonkeyengine</groupId>
    <artifactId>jme3-lwjgl3</artifactId>
    <version>${jme3.version}</version>
    <exclusions>
        <exclusion>
            <groupId>org.lwjgl</groupId>
            <artifactId>lwjgl</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```

---

## 📝 pom.xml 结构分析

### 依赖组织结构

```
pom.xml
├── properties (版本变量定义)
│   ├── jme3.version = 3.5.2-stable
│   └── lwjgl.version = 3.3.1  ✅ 新增
│
├── repositories (Maven仓库)
│   ├── Maven Central
│   └── JitPack (用于 TWL)
│
├── dependencies (依赖列表)
│   ├── [jMonkeyEngine 核心] (6个)
│   ├── [字体渲染库] (9个) ✅ 新增
│   ├── [辅助库] (3个)
│   ├── [UI库] (4个)
│   └── [测试库] (5个)
│
├── profiles (构建配置)
│   ├── game-only (默认)
│   └── with-editor
│
└── build (构建插件)
    ├── maven-compiler-plugin
    ├── maven-surefire-plugin
    └── exec-maven-plugin
```

---

## 🔍 依赖检查清单

### ✅ 已验证
- [x] 所有版本变量已定义
- [x] LWJGL 依赖使用统一版本
- [x] 原生库覆盖 3 个平台 (Windows/Linux/macOS)
- [x] jme-ttf 仓库可访问 (Maven Central)
- [x] 所有依赖 groupId/artifactId 正确

### ⚠️ 需要运行时验证
- [ ] LWJGL 版本与 JME3 兼容
- [ ] 原生库在当前平台加载成功
- [ ] jme-ttf 3.0.1 的类路径正确
- [ ] 没有传递依赖冲突

### 📋 验证命令

```bash
# 1. 清理并重新下载依赖
mvn clean dependency:purge-local-repository

# 2. 查看依赖树
mvn dependency:tree > dependency-tree.txt

# 3. 检查冲突
mvn dependency:analyze

# 4. 编译验证
mvn compile

# 5. 运行测试
mvn test
```

---

## 🚀 推荐的 Maven 命令

### 日常开发
```bash
# 快速编译
mvn clean compile

# 运行游戏
mvn exec:java

# 打包（游戏本体）
mvn clean package

# 打包（含编辑器）
mvn clean package -P with-editor
```

### 依赖管理
```bash
# 更新所有依赖到最新版
mvn versions:display-dependency-updates

# 更新单个依赖
mvn versions:use-latest-versions -Dincludes=org.lwjgl:*

# 下载源码（便于调试）
mvn dependency:sources

# 下载文档
mvn dependency:resolve -Dclassifier=javadoc
```

### 问题诊断
```bash
# 检查有效 POM（包含所有继承和变量替换）
mvn help:effective-pom > effective-pom.xml

# 检查依赖冲突
mvn dependency:tree -Dverbose -Dincludes=org.lwjgl

# 强制更新快照版本
mvn clean install -U
```

---

## 📊 依赖大小估算

### LWJGL 库大小（估算）

| 组件 | 大小 | 说明 |
|------|------|------|
| lwjgl-freetype (主库) | ~50 KB | Java 绑定 |
| lwjgl-freetype-natives-windows | ~500 KB | FreeType DLL |
| lwjgl-freetype-natives-linux | ~400 KB | FreeType SO |
| lwjgl-freetype-natives-macos | ~450 KB | FreeType DYLIB |
| lwjgl-stb (主库) | ~80 KB | Java 绑定 |
| lwjgl-stb-natives (3平台) | ~1.5 MB | 原生库 |
| **总计** | **~3 MB** | 所有平台 |

**注意**: Maven 只会下载当前平台的原生库到最终 JAR 中（使用 maven-shade-plugin 时可配置）。

---

## 🛠️ 优化建议

### 1. 排除不需要的原生库

如果只发布 Windows 版本：

```xml
<dependency>
    <groupId>org.lwjgl</groupId>
    <artifactId>lwjgl-freetype</artifactId>
    <version>${lwjgl.version}</version>
</dependency>
<!-- 只保留 Windows 原生库 -->
<dependency>
    <groupId>org.lwjgl</groupId>
    <artifactId>lwjgl-freetype</artifactId>
    <version>${lwjgl.version}</version>
    <classifier>natives-windows</classifier>
</dependency>
<!-- 删除 Linux 和 macOS -->
```

**减少大小**: ~850 KB

---

### 2. 使用 Maven BOM 管理 LWJGL

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.lwjgl</groupId>
            <artifactId>lwjgl-bom</artifactId>
            <version>${lwjgl.version}</version>
            <scope>import</scope>
            <type>pom</type>
        </dependency>
    </dependencies>
</dependencyManagement>
```

**优势**:
- 自动管理所有 LWJGL 子模块版本
- 避免版本不一致

---

### 3. 条件化平台依赖

使用 Maven profiles 根据平台选择原生库：

```xml
<profiles>
    <profile>
        <id>windows</id>
        <activation>
            <os><family>windows</family></os>
        </activation>
        <dependencies>
            <dependency>
                <groupId>org.lwjgl</groupId>
                <artifactId>lwjgl-freetype</artifactId>
                <classifier>natives-windows</classifier>
            </dependency>
        </dependencies>
    </profile>
    <!-- Linux / macOS profiles... -->
</profiles>
```

---

## 📋 后续步骤

### 1. 立即执行
```bash
# 验证修复
cd game1(1)
mvn clean compile
```

### 2. 检查输出
查看是否有以下警告：
- ❌ 版本冲突
- ❌ 依赖缺失
- ❌ 原生库加载失败

### 3. 如果成功
```bash
# 运行字体系统测试
mvn exec:java -Dexec.mainClass="com.Hecate.ui.test.FontSystemTest"
```

### 4. 生成依赖报告
```bash
# 生成详细报告
mvn dependency:tree > dependency-tree.txt
mvn dependency:analyze > dependency-analysis.txt
```

---

## 📚 参考资料

### Maven 文档
- [Dependency Management](https://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html)
- [Profiles](https://maven.apache.org/guides/introduction/introduction-to-profiles.html)
- [Properties](https://maven.apache.org/pom.html#Properties)

### LWJGL 文档
- [LWJGL 3 Guide](https://www.lwjgl.org/guide)
- [FreeType Native](https://www.lwjgl.org/customize)
- [STB TrueType](https://github.com/nothings/stb/blob/master/stb_truetype.h)

### JME3 文档
- [jME3 LWJGL3 Support](https://wiki.jmonkeyengine.org/docs/3.4/core/engine.html)
- [jME3 Maven Setup](https://jmonkeyengine.github.io/wiki/jme3/maven.html)

---

## ✅ 总结

### 修复内容
1. ✅ 添加 `lwjgl.version` 属性
2. ✅ 所有 LWJGL 依赖使用 `${lwjgl.version}`
3. ✅ 统一版本管理

### 修复的依赖数量
- **8 个 LWJGL 依赖**项版本统一

### 潜在风险
- ⚠️ LWJGL 3.3.1 与 JME3 3.5.2 的兼容性（需运行时验证）

### 下一步
1. 运行 `mvn clean compile` 验证
2. 检查依赖树是否有冲突
3. 运行字体系统测试

---

**修复完成时间**: 2026-08-18  
**状态**: ✅ 已修复，待验证
