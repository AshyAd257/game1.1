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
            // jme-ttf 3.0+ 使用 TrueTypeFont 类
            Class.forName("com.jme3x.jfx.injme.TrueTypeFont");
            return true;
        } catch (ClassNotFoundException e) {
            // 尝试旧版本的类名
            try {
                Class.forName("jmettf.TrueTypeFont");
                return true;
            } catch (ClassNotFoundException ex) {
                return false;
            }
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
            // jme-ttf 的典型用法：
            // TrueTypeFont font = TrueTypeLoader.loadFont(assetManager, fontPath, fontSize);
            // return font.getBitmapFont();

            logger.warn("jme-ttf integration needs proper implementation");
            logger.info("jme-ttf is available but loader not fully integrated");

            // TODO: 实现完整的 jme-ttf 集成
            // 问题：jme-ttf 3.0.1 的 API 可能已更改
            // 需要参考最新文档进行集成

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
