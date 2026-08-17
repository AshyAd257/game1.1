# 字体文件说明

## 如何添加自定义TTF字体

将你的TTF字体文件放在这个目录下，支持的文件名：

1. **CustomFont.ttf** （优先级最高）
2. **ChineseFont.ttf**

## 字体加载优先级

程序会按以下顺序尝试加载字体：

1. **resources/Fonts/CustomFont.ttf** （从这个目录加载）
2. **resources/Fonts/ChineseFont.ttf** （从这个目录加载）
3. **C:/Windows/Fonts/msyh.ttc** （Windows系统微软雅黑）
4. **C:/Windows/Fonts/simhei.ttf** （Windows系统黑体）
5. **C:/Windows/Fonts/simsun.ttc** （Windows系统宋体）
6. **Interface/Fonts/ChineseFont.fnt** （BitmapFont备用）
7. **Interface/Fonts/Default.fnt** （默认英文字体）

## 推荐字体

### 免费中文字体：
- **思源黑体** (Source Han Sans): https://github.com/adobe-fonts/source-han-sans
- **文泉驿微米黑**: http://wenq.org/wqy2/index.cgi?MicroHei
- **站酷字体**: https://www.zcool.com.cn/special/zcoolfonts/

### 像素风格字体：
- **zpix**: https://github.com/SolidZORO/zpix-pixel-font
- **fusion-pixel**: https://github.com/TakWolf/fusion-pixel-font

## 使用步骤

1. 下载你喜欢的TTF字体文件
2. 重命名为 `CustomFont.ttf` 或 `ChineseFont.ttf`
3. 复制到这个目录：`src/main/resources/Fonts/`
4. 重新编译并运行游戏：`mvn compile && mvn exec:java`

## 测试

运行游戏后按 `I` 键打开编辑器，查看帮助面板中的中文是否正确显示。

## 注意事项

- TTF/OTF字体文件都支持
- 字体文件会被打包到JAR中，确保跨平台运行
- 如果字体文件很大（>5MB），可能会增加JAR包体积
- 首次渲染新字符时会有轻微延迟（正常现象）
