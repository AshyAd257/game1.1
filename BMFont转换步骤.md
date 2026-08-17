# BMFont字体转换步骤

## 你已有的文件
`C:\Users\29232\OneDrive\Desktop\Z Labs Bitmap 12px_100font\ttf\ZLabsBitmap_12px_CN（简体中文）.ttf`

## 步骤1：下载并安装BMFont

1. 下载地址：http://www.angelcode.com/products/bmfont/
2. 下载 `bmfont64.zip` 或 `bmfont.zip`
3. 解压到任意文件夹（例如：`C:\Tools\BMFont`）
4. 运行 `bmfont.exe`（无需安装）

## 步骤2：配置字体设置

1. 点击菜单 **Options → Font Settings**
2. 配置如下：
   - **Font**: 点击下拉菜单，选择 "**Add Font File...**"
   - 浏览到：`C:\Users\29232\OneDrive\Desktop\Z Labs Bitmap 12px_100font\ttf\ZLabsBitmap_12px_CN（简体中文）.ttf`
   - 选择后，字体会显示在列表中
   - **Size**: 选择 **12** (这是像素字体，使用原始大小)
   - **Match char height**: 勾选
   - 其他保持默认
3. 点击 **OK**

## 步骤3：配置导出选项

1. 点击菜单 **Options → Export Options**
2. 配置如下：
   - **Padding**:
     - Top: 0
     - Right: 0
     - Bottom: 0
     - Left: 0
   - **Spacing**:
     - Horiz: 1
     - Vert: 1
   - **Equalize the cell heights**: 勾选
   - **Bit depth**: 选择 **32**
   - **Preset**: Texture size presets 选择 **512x512** 或 **1024x1024**
   - **Font descriptor**: 选择 **Text**
   - **Textures**: 选择 **png**
3. 点击 **OK**

## 步骤4：选择要包含的字符

这是关键步骤！只包含你需要的字符可以大大减小文件大小。

### 方法A：从文件导入（推荐）

1. 创建一个文本文件 `chars.txt`，包含所有需要的字符：
```
快捷键显示隐藏编辑器退出全屏切换选择骨骼调整宽度高度录制关键帧空格播放暂停鼠标左键拖拽旋转视角滚轮缩放滑条尺寸
位置缩放部件名称属性值面板时间帧数当前已加载测试场景木偶渲染更新
Body Head Torso Arm Leg Foot Hand
0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz
+-*/=()[]{},.;:!?<>|&@#$%^~_\'"
```

2. 在BMFont中，点击菜单 **Edit → Select chars from file**
3. 选择刚才创建的 `chars.txt` 文件

### 方法B：手动选择常用字符

1. 点击菜单 **Edit → Select subset**
2. 勾选：
   - **Latin + Latin supplement** (英文和基本符号)
3. 点击菜单 **Edit → Select chars from file** 并选择包含中文字符的文本文件

## 步骤5：生成字体文件

1. 点击菜单 **Options → Save bitmap font as...**
2. 保存位置和文件名：
   - 保存到：`C:\Users\29232\OneDrive\Desktop\ChineseFont.fnt`
3. 点击 **保存**

**生成的文件：**
- `ChineseFont.fnt` (字体描述文件)
- `ChineseFont_0.png` (字体纹理图片)

如果字符很多，可能会生成多个纹理：
- `ChineseFont_0.png`
- `ChineseFont_1.png`
- ...

## 步骤6：复制到项目

1. 将生成的所有文件复制到项目目录：
```
C:\Users\29232\OneDrive\Desktop\game1(1)\src\main\resources\Interface\Fonts\
```

复制后的文件结构应该是：
```
src/main/resources/Interface/Fonts/
    ChineseFont.fnt
    ChineseFont_0.png
    ChineseFont_1.png (如果有)
    ...
```

## 步骤7：测试

1. 运行游戏：`mvn compile && mvn exec:java -Dexec.mainClass="com.Hecate.Main"`
2. 查看控制台输出：
   - ✅ `>>> Loaded Chinese font` - 成功！
   - ❌ `>>> Using default font` - 文件路径不对或文件损坏

## 常见问题

### 问题1：纹理太小，字符显示不全
**解决**：在 Export Options 中增加 Texture size，使用 1024x1024 或 2048x2048

### 问题2：字符太多，生成失败
**解决**：减少选择的字符数量，只包含必需的字符

### 问题3：字体显示为方块
**解决**：确保选择了包含这些字符的字符集

### 问题4：中文显示为方块，英文正常
**解决**：检查是否用 "Select chars from file" 包含了中文字符

## 快速测试命令

```bash
# 编译并运行
cd C:\Users\29232\OneDrive\Desktop\game1(1)
mvn compile && mvn exec:java -Dexec.mainClass="com.Hecate.Main"
```
