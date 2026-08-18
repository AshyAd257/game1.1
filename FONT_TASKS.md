# 字体系统实现任务清单

## 🎯 总览

本文档列出了完成字体渲染系统所需的具体任务。每个任务都标明了优先级、难度和预期工作量。

---

## 📋 任务列表

### ✅ 已完成

- [x] 添加所有字体库依赖到 pom.xml
- [x] 创建 FontManager 统一管理器
- [x] 创建各个加载器的骨架代码
- [x] 实现字体系统诊断工具
- [x] 创建测试应用

---

### 🔴 高优先级（核心功能）

#### 1. 完成 jme-ttf 集成
**文件**: `JmeTtfFontLoader.java`  
**优先级**: 🔴 HIGH  
**难度**: ⭐⭐ (中等)  
**预期时间**: 2-4 小时

**任务**:
```java
// 需要实现的方法：
public static BitmapFont loadFont(AssetManager assetManager, String fontPath, int fontSize)
```

**步骤**:
1. 研究 jme-ttf 3.0.1 的 API 文档
2. 检查正确的包名和类名（可能是 `com.jme3x.jfx.injme.TrueTypeFont`）
3. 实现字体加载逻辑：
   ```java
   // 伪代码示例
   TrueTypeFont ttfFont = new TrueTypeFont(assetManager, fontPath, fontSize);
   BitmapFont bitmapFont = ttfFont.getBitmapFont();
   return bitmapFont;
   ```
4. 测试中文字体（`ZLabsBitmap_12px_CN（简体中文）.ttf`）
5. 处理异常情况

**验收标准**:
- ✓ 能够加载项目中的 TTF 字体文件
- ✓ 生成的 BitmapFont 可以显示文本
- ✓ 支持中文字符显示
- ✓ 无内存泄漏

**参考资料**:
- https://github.com/stephengold/jme-ttf
- https://github.com/stephengold/jme-ttf/blob/master/README.md

---

#### 2. 实现 LWJGL FreeType 字形图集生成
**文件**: `FreetypeFontLoader.java`  
**优先级**: 🔴 HIGH  
**难度**: ⭐⭐⭐⭐ (困难)  
**预期时间**: 6-8 小时

**任务**:
在 `loadFont()` 方法中添加字形渲染和图集生成。

**步骤**:

##### 2.1 渲染单个字符
```java
// 1. 加载字符
int error = FT_Load_Char(face, charCode, FT_LOAD_RENDER);

// 2. 获取字形槽
FT_GlyphSlot slot = FT_Face_glyph(face);

// 3. 获取位图数据
FT_Bitmap bitmap = FT_GlyphSlot_bitmap(slot);
int width = bitmap.width();
int height = bitmap.rows();
ByteBuffer buffer = bitmap.buffer(width * height);

// 4. 获取字符度量
int advanceX = (int)(FT_GlyphSlot_advance(slot) >> 6);
int bearingX = FT_GlyphSlot_bitmap_left(slot);
int bearingY = FT_GlyphSlot_bitmap_top(slot);
```

##### 2.2 生成纹理图集
```java
// 1. 计算所需纹理大小
int atlasWidth = 512;  // 或动态计算
int atlasHeight = 512;

// 2. 创建空白纹理
ByteBuffer atlasBuffer = BufferUtils.createByteBuffer(atlasWidth * atlasHeight);

// 3. 打包字形到图集（使用矩形装箱算法）
class CharInfo {
    int x, y, width, height;
    int advanceX, bearingX, bearingY;
}
Map<Character, CharInfo> charMap = new HashMap<>();

// 4. 将每个字形复制到图集
for (char c : characterSet) {
    // 渲染字符...
    // 找到空位置...
    // 复制位图数据...
    charMap.put(c, charInfo);
}
```

##### 2.3 创建 JME3 BitmapFont
```java
// 1. 创建纹理
Image image = new Image(Format.Luminance8, atlasWidth, atlasHeight, atlasBuffer);
Texture2D texture = new Texture2D(image);

// 2. 创建 BitmapCharacterSet
// 注意：这部分可能需要反射或继承，因为 BitmapFont 的内部结构较复杂

// 3. 设置字符度量信息
for (Map.Entry<Character, CharInfo> entry : charMap.entrySet()) {
    // 设置 UV 坐标
    // 设置字符宽度、高度
    // 设置 kerning（字距）
}

// 4. 返回 BitmapFont
return bitmapFont;
```

**字符集建议**:
- **ASCII**: 32-126 (基础拉丁字符)
- **CJK 常用汉字**: GB2312 一级字库 (3755 个汉字)
- **或使用动态加载**: 仅渲染实际使用的字符

**验收标准**:
- ✓ 能渲染 ASCII 字符
- ✓ 能渲染中文字符
- ✓ 字符间距正确
- ✓ 纹理图集大小合理（不浪费显存）
- ✓ 性能可接受（加载时间 < 500ms）

**参考资料**:
- https://learnopengl.com/In-Practice/Text-Rendering
- https://freetype.org/freetype2/docs/tutorial/step1.html

---

#### 3. 实现 LWJGL STB 字形图集生成
**文件**: `STBFontLoader.java`  
**优先级**: 🔴 HIGH  
**难度**: ⭐⭐⭐ (中等偏难)  
**预期时间**: 4-6 小时

**任务**:
使用 STB TrueType 渲染字形并生成图集。

**步骤**:

##### 3.1 获取字体度量
```java
try (MemoryStack stack = MemoryStack.stackPush()) {
    IntBuffer ascent = stack.mallocInt(1);
    IntBuffer descent = stack.mallocInt(1);
    IntBuffer lineGap = stack.mallocInt(1);
    
    STBTruetype.stbtt_GetFontVMetrics(fontInfo, ascent, descent, lineGap);
    
    float scale = STBTruetype.stbtt_ScaleForPixelHeight(fontInfo, fontSize);
    int baseline = (int)(ascent.get(0) * scale);
}
```

##### 3.2 渲染字符位图
```java
try (MemoryStack stack = MemoryStack.stackPush()) {
    IntBuffer width = stack.mallocInt(1);
    IntBuffer height = stack.mallocInt(1);
    IntBuffer xoff = stack.mallocInt(1);
    IntBuffer yoff = stack.mallocInt(1);
    
    ByteBuffer bitmap = STBTruetype.stbtt_GetCodepointBitmap(
        fontInfo, scale, scale, codepoint, width, height, xoff, yoff
    );
    
    // bitmap 现在包含字符的灰度位图
    // 需要复制到图集中
    
    STBTruetype.stbtt_FreeBitmap(bitmap);
}
```

##### 3.3 装箱算法（矩形打包）
```java
// 简单的行装箱算法
class Packer {
    int currentX = 0;
    int currentY = 0;
    int rowHeight = 0;
    int atlasWidth;
    int atlasHeight;
    
    Rectangle pack(int width, int height) {
        if (currentX + width > atlasWidth) {
            // 换行
            currentX = 0;
            currentY += rowHeight;
            rowHeight = 0;
        }
        
        Rectangle rect = new Rectangle(currentX, currentY, width, height);
        currentX += width;
        rowHeight = Math.max(rowHeight, height);
        
        return rect;
    }
}
```

**验收标准**:
- ✓ 与 FreeType 加载器相同
- ✓ 启动速度比 FreeType 快（STB 应该更快）

**参考资料**:
- https://github.com/nothings/stb/blob/master/stb_truetype.h
- LWJGL STB example: https://github.com/LWJGL/lwjgl3/tree/master/modules/samples/src/test/java/org/lwjgl/demo/stb

---

### 🟡 中优先级（增强功能）

#### 4. 实现字体缓存系统
**文件**: 新建 `FontCache.java`  
**优先级**: 🟡 MEDIUM  
**难度**: ⭐⭐ (中等)  
**预期时间**: 3-4 小时

**任务**:
- 磁盘缓存：预生成的字形图集存储为文件
- 内存缓存：LRU 缓存，避免重复加载
- 异步加载：后台线程加载字体，不阻塞主线程

**步骤**:
```java
public class FontCache {
    private LRUCache<String, BitmapFont> memoryCache;
    private Path cacheDir = Paths.get("cache/fonts/");
    
    public BitmapFont getOrLoad(String fontPath, int size) {
        String cacheKey = fontPath + "_" + size;
        
        // 1. 检查内存缓存
        if (memoryCache.contains(cacheKey)) {
            return memoryCache.get(cacheKey);
        }
        
        // 2. 检查磁盘缓存
        Path cachedFile = cacheDir.resolve(cacheKey + ".cache");
        if (Files.exists(cachedFile)) {
            BitmapFont font = loadFromDiskCache(cachedFile);
            memoryCache.put(cacheKey, font);
            return font;
        }
        
        // 3. 生成新字体并缓存
        BitmapFont font = generateFont(fontPath, size);
        saveToDiskCache(cachedFile, font);
        memoryCache.put(cacheKey, font);
        return font;
    }
}
```

---

#### 5. 添加 MSDF 字体渲染支持
**文件**: 新建 `MSDFFontLoader.java`  
**优先级**: 🟡 MEDIUM  
**难度**: ⭐⭐⭐⭐⭐ (非常困难)  
**预期时间**: 10-15 小时

**说明**:
MSDF (Multi-channel Signed Distance Field) 字体可以无损缩放，质量远超普通位图字体。

**步骤**:
1. 集成 msdfgen 库（可能需要 JNI）
2. 为每个字符生成 MSDF 纹理
3. 编写 MSDF shader（GLSL）
4. 修改 JME3 渲染管线

**参考**:
- https://github.com/Chlumsky/msdfgen
- https://github.com/Chlumsky/msdf-atlas-gen

---

### 🟢 低优先级（可选功能）

#### 6. 高级字体特性
**优先级**: 🟢 LOW  
**难度**: ⭐⭐⭐⭐⭐

- OpenType 特性（连字、替代字形）
- 复杂文本布局（阿拉伯语、印地语）
- 字距调整（kerning pairs）
- 文本整形（text shaping with HarfBuzz）

---

## 🧪 测试清单

### 单元测试
- [ ] 测试各个加载器的初始化
- [ ] 测试字体文件解析
- [ ] 测试字符渲染
- [ ] 测试图集生成

### 集成测试
- [ ] 运行 `FontSystemTest`
- [ ] 检查控制台诊断输出
- [ ] 验证屏幕显示文本

### 性能测试
- [ ] 字体加载时间 < 500ms
- [ ] 内存占用合理（< 50MB per font）
- [ ] 无内存泄漏

### 兼容性测试
- [ ] Windows 10/11
- [ ] Linux (Ubuntu)
- [ ] macOS

---

## 📊 进度跟踪

| 任务 | 状态 | 负责人 | 预计完成 |
|------|------|--------|----------|
| 基础架构 | ✅ 完成 | - | 2026-08-18 |
| jme-ttf 集成 | ⏳ 待做 | - | TBD |
| FreeType 图集 | ⏳ 待做 | - | TBD |
| STB 图集 | ⏳ 待做 | - | TBD |
| 字体缓存 | ⏸️ 暂缓 | - | TBD |
| MSDF 支持 | ⏸️ 暂缓 | - | TBD |

---

## 🛠️ 开发建议

### 推荐开发顺序
1. **先完成 jme-ttf** - 最简单，最快见效
2. **再做 LWJGL STB** - 相对简单，无外部依赖
3. **最后做 FreeType** - 功能最强，但最复杂

### 调试技巧
```java
// 保存渲染的字形图集到文件，方便调试
ImageIO.write(atlasBufferedImage, "PNG", new File("debug_atlas.png"));

// 打印字符度量信息
logger.debug("Char '{}': width={}, height={}, advanceX={}", 
    c, width, height, advanceX);
```

### 常见问题
1. **字符显示为方块**: 字体文件不包含该字符
2. **字符间距错误**: advanceX 计算有误
3. **字符偏移错误**: bearingX/bearingY 未正确应用
4. **纹理模糊**: 字体大小太小或图集分辨率不足

---

## 📞 需要帮助？

遇到问题时的排查步骤：
1. 运行 `FontSystemDiagnostics`
2. 检查日志输出
3. 保存中间结果（图集图片）
4. 参考 LearnOpenGL 的字体渲染教程

---

**最后更新**: 2026-08-18  
**文档版本**: 1.0
