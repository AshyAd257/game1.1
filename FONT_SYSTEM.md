# 字体渲染系统文档

## 📚 概述

Hecate 游戏引擎集成了多种字体渲染库，支持高质量的文本显示和多语言字符集。

## 🔧 已集成的字体渲染库

### 1. **JME3 Bitmap Font**（默认）
- **状态**: ✅ 完全可用
- **说明**: JME3 自带的位图字体系统
- **优点**: 性能好，稳定
- **缺点**: 需要预生成 .fnt 文件
- **使用场景**: 固定大小的 UI 文本

### 2. **jme-ttf** (Stephen Gold)
- **状态**: ⚠️ 已添加但未完全集成
- **版本**: 3.0.1
- **Maven 坐标**: `com.github.stephengold:jme-ttf:3.0.1`
- **GitHub**: https://github.com/stephengold/jme-ttf
- **说明**: 基于 FreeType 的 JME3 TTF 字体加载器
- **优点**: 
  - 直接加载 TTF 文件
  - 运行时动态调整字体大小
  - 与 JME3 深度集成
- **待完成**: 需要实现 `JmeTtfFontLoader.loadFont()` 的完整逻辑

### 3. **LWJGL FreeType**
- **状态**: ⚠️ 已添加但部分实现
- **版本**: 3.3.1
- **Maven 坐标**: `org.lwjgl:lwjgl-freetype:3.3.1`
- **官网**: https://www.freetype.org/
- **说明**: FreeType 库的 LWJGL 绑定
- **优点**:
  - 行业标准的字体渲染引擎
  - 支持各种字体格式（TTF, OTF, Type1）
  - 精细的字形控制和提示（hinting）
- **已实现**:
  - FreeType 库初始化
  - 字体文件加载
  - 字体大小设置
- **待完成**:
  - 字形图集生成
  - 转换为 JME3 BitmapFont

### 4. **LWJGL STB TrueType**
- **状态**: ⚠️ 已添加但部分实现
- **版本**: 3.3.1
- **Maven 坐标**: `org.lwjgl:lwjgl-stb:3.3.1`
- **说明**: stb_truetype 单头文件库的 LWJGL 绑定
- **优点**:
  - 轻量级（无外部依赖）
  - 公共领域授权（Public Domain）
  - 启动快速
- **缺点**:
  - 功能少于 FreeType
  - 不支持高级字体特性（连字、复杂字形）
- **已实现**:
  - STB 字体信息初始化
- **待完成**:
  - 字形位图生成
  - 纹理图集打包

### 5. **Lemur GUI 字体系统**
- **状态**: ✅ 可用（已安装 Lemur）
- **版本**: 1.16.0
- **说明**: Lemur 自带的字体渲染系统
- **特点**: 如果使用 Lemur 做 UI，可以直接使用其字体 API

## 📦 依赖配置

所有依赖已添加到 `pom.xml`：

```xml
<!-- jme-ttf -->
<dependency>
    <groupId>com.github.stephengold</groupId>
    <artifactId>jme-ttf</artifactId>
    <version>3.0.1</version>
</dependency>

<!-- LWJGL FreeType -->
<dependency>
    <groupId>org.lwjgl</groupId>
    <artifactId>lwjgl-freetype</artifactId>
    <version>3.3.1</version>
</dependency>
<!-- 原生库（Windows/Linux/macOS） -->
...

<!-- LWJGL STB -->
<dependency>
    <groupId>org.lwjgl</groupId>
    <artifactId>lwjgl-stb</artifactId>
    <version>3.3.1</version>
</dependency>
...
```

## 🎯 使用方法

### 基础用法

```java
// 创建字体管理器
FontManager fontManager = new FontManager(assetManager);

// 加载默认字体
BitmapFont defaultFont = fontManager.getDefaultFont();

// 加载 TTF 字体（未来支持）
BitmapFont customFont = fontManager.loadFont("Interface/Fonts/MyFont.ttf", 24);

// 切换字体渲染后端
fontManager.setBackend(FontManager.FontBackend.LWJGL_FREETYPE);
```

### 运行诊断

```java
// 检测所有字体库状态
fontManager.runDiagnostics();

// 输出示例：
// ========================================
//   Font System Diagnostics
// ========================================
//   [✓] JME3 Bitmap Font
//       Status: Default font loaded successfully
//   [✗] jme-ttf (Stephen Gold) (3.0.1+)
//       Status: Library found but not integrated
//       Issues:
//         - Loader implementation incomplete
//   [✓] LWJGL FreeType (3.3.1)
//       Status: Library available, loader partially implemented
//   ...
```

## 📂 已有字体文件

```
src/main/resources/Interface/Fonts/
├── ZLabsBitmap_12px_CN（简体中文）.ttf
├── ZLabsBitmap_12px_HC（香港繁体）.ttf
├── ZLabsBitmap_12px_JP（日文）.ttf
├── ZLabsBitmap_12px_HC_FALLBACK.ttf
└── ZLabsBitmap_12px_JP_FALLBACK.ttf
```

## 🚧 待完成工作

### 高优先级
1. **完成 jme-ttf 集成**
   - 研究 jme-ttf 3.0.1 API
   - 实现 `JmeTtfFontLoader.loadFont()`
   - 测试中文字体加载

2. **字形图集生成**
   - 为 FreeType 和 STB 实现图集生成
   - 支持 ASCII + CJK 字符集
   - 优化纹理打包算法

3. **BitmapFont 转换**
   - 将渲染的字形转换为 JME3 BitmapFont
   - 设置字符度量信息（宽度、高度、kerning）
   - 生成纹理坐标

### 中优先级
4. **MSDF 字体渲染**
   - 研究 msdfgen 集成方案
   - 实现 MSDF 纹理生成
   - 创建 MSDF shader

5. **字体缓存系统**
   - 实现磁盘缓存（预生成图集）
   - LRU 内存缓存
   - 异步加载

### 低优先级
6. **高级字体特性**
   - 连字（ligatures）
   - 复杂字形（OpenType features）
   - 右到左语言支持

## 🔍 诊断工具

### FontSystemDiagnostics

运行完整检查：
```java
List<DiagnosticResult> results = FontSystemDiagnostics.runDiagnostics(assetManager);
String report = FontSystemDiagnostics.generateReport(results);
System.out.println(report);
```

输出示例：
```
╔════════════════════════════════════════╗
║   Font System Diagnostic Report       ║
╚════════════════════════════════════════╝

  [✓] JME3 Bitmap Font
      Status: Default font loaded successfully

  [✗] jme-ttf (Stephen Gold) (3.0.1+)
      Status: Library found but not integrated
      Issues:
        - Loader implementation incomplete

  [✓] LWJGL FreeType (3.3.1)
      Status: Library available, loader partially implemented
      Issues:
        - Native library loaded successfully

  [✓] LWJGL STB TrueType (3.3.1)
      Status: Library available, loader partially implemented

  [✓] Lemur GUI Font System (1.16.0)
      Status: Lemur GUI available
      Issues:
        - Lemur has its own font rendering system

  [✓] Font Files
      Status: 3/4 font files found
      Issues:
        - Missing: Interface/Fonts/Default.fnt

═══════════════════════════════════════
Summary: 4/6 components available
═══════════════════════════════════════
```

## 📖 参考资料

- **jme-ttf**: https://github.com/stephengold/jme-ttf
- **FreeType**: https://www.freetype.org/freetype2/docs/tutorial/step1.html
- **LWJGL**: https://www.lwjgl.org/guide
- **stb_truetype**: https://github.com/nothings/stb/blob/master/stb_truetype.h
- **MSDF**: https://github.com/Chlumsky/msdfgen

## 🤝 贡献指南

如需完善字体系统实现，请参考：
1. `com.Hecate.ui.font` 包中的加载器类
2. 每个加载器都标注了 TODO 注释
3. 运行诊断工具确定缺失组件

---

**当前状态**: 🟡 部分完成
- 基础框架 ✅
- 依赖配置 ✅
- 诊断工具 ✅
- 加载器实现 ⚠️ 进行中
