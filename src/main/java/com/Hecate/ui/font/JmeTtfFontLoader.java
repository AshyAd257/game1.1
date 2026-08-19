package com.Hecate.ui.font;

import com.jme3.asset.AssetManager;
import com.jme3.font.BitmapFont;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * jme-ttf 字体加载器包装器
 * <p>jme-ttf 是 Stephen Gold 开发的 JME3 TTF 插件，基于 FreeType
 * <p>GitHub: https://github.com/stephengold/jme-ttf
 */
public class JmeTtfFontLoader {
    private static final Logger logger = LoggerFactory.getLogger(JmeTtfFontLoader.class);

    /**
     * 检查 jme-ttf 是否可用
     */
    public static boolean isAvailable() {
        try {
            // jme-ttf 3.0.1 使用 com.atr.jme.font.TrueTypeFont
            Class.forName("com.atr.jme.font.TrueTypeFont");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * 加载 TTF 字体
     *
     * @param assetManager JME3 资源管理器
     * @param fontPath 字体文件路径
     * @param fontSize 字体大小
     * @return BitmapFont 对象
     */
    public static BitmapFont loadFont(AssetManager assetManager, String fontPath, int fontSize) {
        if (!isAvailable()) {
            logger.error("jme-ttf library not found in classpath");
            return null;
        }

        try {
            // 注册 jme-ttf 加载器
            assetManager.registerLoader(com.atr.jme.font.asset.TrueTypeLoader.class, "ttf");

            // 创建 TrueTypeKeyBMP 用于位图字体渲染
            com.atr.jme.font.asset.TrueTypeKeyBMP key =
                new com.atr.jme.font.asset.TrueTypeKeyBMP(fontPath,
                    com.atr.jme.font.util.Style.Plain, fontSize);

            // 加载 TrueTypeBMP（位图渲染模式）
            com.atr.jme.font.TrueTypeBMP ttfBitmap =
                (com.atr.jme.font.TrueTypeBMP) assetManager.loadAsset(key);

            // jme-ttf 使用 TrueTypeText/TrueTypeNode，不直接返回 BitmapFont
            // 我们需要另一种方法
            logger.info("jme-ttf font loaded: {}", fontPath);

            // jme-ttf 不返回 BitmapFont，返回 null 使用回退方案
            return null;

        } catch (Exception e) {
            logger.error("Error loading jme-ttf font: " + fontPath, e);
            return null;
        }
    }

    /**
     * 获取 jme-ttf 版本信息
     */
    public static String getVersion() {
        if (!isAvailable()) {
            return "Not installed";
        }
        return "3.0.1 (Stephen Gold)";
    }
}
