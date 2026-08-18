package com.Hecate.ui.font;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.AssetManager;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Node;
import jmettf.TrueTypeFont;
import jmettf.TrueTypeMesh;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 使用 jme-ttf 加载和渲染 TrueType 字体的示例。
 * <p>
 * jme-ttf 提供两种渲染模式：
 * 1. BitmapFont 模式 (TrueTypeFont): 预渲染字形到纹理图集
 * 2. 矢量模式 (TrueTypeMesh): 动态生成网格，支持任意大小
 */
public class TrueTypeFontExample {
    private static final Logger logger = LoggerFactory.getLogger(TrueTypeFontExample.class);

    /**
     * 加载 TTF 字体并转换为 BitmapFont（适合游戏UI文本）
     *
     * @param assetManager JME3 资源管理器
     * @param fontPath     字体文件路径（如 "Fonts/NotoSansCJK-Regular.ttf"）
     * @param fontSize     字体大小（像素）
     * @return BitmapFont 对象
     */
    public static BitmapFont loadTTFFont(AssetManager assetManager, String fontPath, int fontSize) {
        try {
            // 使用 jme-ttf 加载 TTF 字体
            TrueTypeFont ttf = TrueTypeFont.create(assetManager, fontPath);

            // 设置字体大小和DPI
            ttf.setStyle(fontSize);

            // 指定要预渲染的字符集（可选，默认为 ASCII）
            // ttf.setCharacterSet("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%");

            // 转换为 BitmapFont
            BitmapFont bitmapFont = ttf.getBitmapFont();

            logger.info("✓ TTF font loaded: {} (size: {})", fontPath, fontSize);
            return bitmapFont;

        } catch (Exception e) {
            logger.error("Failed to load TTF font: {}", fontPath, e);
            return null;
        }
    }

    /**
     * 创建文本节点（使用 BitmapFont）
     */
    public static BitmapText createText(BitmapFont font, String text, float x, float y) {
        BitmapText textNode = new BitmapText(font);
        textNode.setText(text);
        textNode.setSize(font.getCharSet().getRenderedSize());
        textNode.setColor(ColorRGBA.White);
        textNode.setLocalTranslation(x, y, 0);
        return textNode;
    }

    /**
     * 使用示例
     */
    public static void example(SimpleApplication app) {
        // 1. 加载字体
        BitmapFont font = loadTTFFont(app.getAssetManager(), "Fonts/Arial.ttf", 24);

        if (font != null) {
            // 2. 创建文本
            BitmapText text = createText(font, "Hello World! 你好世界!", 100, 300);

            // 3. 添加到场景
            app.getGuiNode().attachChild(text);
        }
    }
}
