package com.Hecate.ui;

import com.Hecate.ui.font.*;
import com.jme3.asset.AssetManager;
import com.jme3.font.BitmapFont;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * 统一字体管理器
 * <p>支持多种字体渲染后端：
 * <ul>
 *   <li>JME3 默认 BitmapFont (.fnt)
 *   <li>jme-ttf (FreeType 包装)
 *   <li>LWJGL FreeType (直接调用)
 *   <li>LWJGL STB TrueType (轻量级)
 * </ul>
 */
public class FontManager {
    private static final Logger logger = LoggerFactory.getLogger(FontManager.class);

    private final AssetManager assetManager;
    private final Map<String, BitmapFont> fontCache = new HashMap<>();

    // 字体渲染后端
    public enum FontBackend {
        JME_BITMAP,      // JME3 预生成位图字体
        JME_TTF,         // jme-ttf (FreeType)
        LWJGL_FREETYPE,  // LWJGL FreeType 直接调用
        LWJGL_STB        // LWJGL STB TrueType
    }

    private FontBackend currentBackend = FontBackend.JME_BITMAP;
    private boolean diagnosticsRun = false;

    public FontManager(AssetManager assetManager) {
        this.assetManager = assetManager;
        detectAvailableBackends();
    }

    /**
     * 检测可用的字体渲染后端
     */
    private void detectAvailableBackends() {
        logger.info("========================================");
        logger.info("  Detecting Font Rendering Backends");
        logger.info("========================================");

        // 检查 jme-ttf
        if (JmeTtfFontLoader.isAvailable()) {
            logger.info("✓ jme-ttf: {} ", JmeTtfFontLoader.getVersion());
        } else {
            logger.warn("✗ jme-ttf: Not available");
        }

        // 检查 LWJGL FreeType
        try {
            Class.forName("org.lwjgl.util.freetype.FreeType");
            boolean ftInit = FreetypeFontLoader.initialize();
            if (ftInit) {
                logger.info("✓ LWJGL FreeType: Available and initialized");
            } else {
                logger.warn("✗ LWJGL FreeType: Library found but initialization failed");
            }
        } catch (ClassNotFoundException e) {
            logger.warn("✗ LWJGL FreeType: Not found in classpath");
        }

        // 检查 LWJGL STB
        try {
            Class.forName("org.lwjgl.stb.STBTruetype");
            logger.info("✓ LWJGL STB TrueType: Available");
        } catch (ClassNotFoundException e) {
            logger.warn("✗ LWJGL STB TrueType: Not found in classpath");
        }

        logger.info("========================================");
        logger.info("Current backend: {}", currentBackend);
        logger.info("========================================\n");
    }

    /**
     * 运行详细诊断（调试用）
     */
    public void runDiagnostics() {
        if (!diagnosticsRun) {
            FontSystemDiagnostics.runDiagnostics(assetManager);
            diagnosticsRun = true;
        }
    }

    /**
     * 加载字体（自动选择后端）
     * @param fontPath 字体路径（.fnt 或 .ttf）
     * @param size 字体大小（TTF字体有效）
     */
    public BitmapFont loadFont(String fontPath, int size) {
        String cacheKey = fontPath + "_" + size;

        if (fontCache.containsKey(cacheKey)) {
            return fontCache.get(cacheKey);
        }

        BitmapFont font = null;

        if (fontPath.endsWith(".fnt")) {
            // JME3 位图字体
            font = loadBitmapFont(fontPath);
        } else if (fontPath.endsWith(".ttf") || fontPath.endsWith(".otf")) {
            // TTF/OTF 字体
            font = loadTrueTypeFont(fontPath, size);
        }

        if (font != null) {
            fontCache.put(cacheKey, font);
        }

        return font;
    }

    /**
     * 加载 JME3 位图字体
     */
    private BitmapFont loadBitmapFont(String fontPath) {
        try {
            return assetManager.loadFont(fontPath);
        } catch (Exception e) {
            logger.error("Failed to load bitmap font: " + fontPath, e);
            return assetManager.loadFont("Interface/Fonts/Default.fnt");
        }
    }

    /**
     * 加载 TrueType 字体（使用当前后端）
     */
    private BitmapFont loadTrueTypeFont(String fontPath, int size) {
        switch (currentBackend) {
            case JME_TTF:
                return loadWithJmeTtf(fontPath, size);
            case LWJGL_FREETYPE:
                return loadWithLwjglFreetype(fontPath, size);
            case LWJGL_STB:
                return loadWithLwjglStb(fontPath, size);
            default:
                logger.warn("TTF fonts not supported, falling back to default bitmap font");
                return assetManager.loadFont("Interface/Fonts/Default.fnt");
        }
    }

    /**
     * 使用 jme-ttf 加载
     */
    private BitmapFont loadWithJmeTtf(String fontPath, int size) {
        BitmapFont font = JmeTtfFontLoader.loadFont(assetManager, fontPath, size);
        if (font != null) {
            return font;
        }
        logger.warn("jme-ttf loader failed, falling back to default font");
        return assetManager.loadFont("Interface/Fonts/Default.fnt");
    }

    /**
     * 使用 LWJGL FreeType 加载
     */
    private BitmapFont loadWithLwjglFreetype(String fontPath, int size) {
        BitmapFont font = FreetypeFontLoader.loadFont(assetManager, fontPath, size);
        if (font != null) {
            return font;
        }
        logger.warn("LWJGL FreeType loader failed, falling back to default font");
        return assetManager.loadFont("Interface/Fonts/Default.fnt");
    }

    /**
     * 使用 LWJGL STB 加载
     */
    private BitmapFont loadWithLwjglStb(String fontPath, int size) {
        BitmapFont font = STBFontLoader.loadFont(assetManager, fontPath, size);
        if (font != null) {
            return font;
        }
        logger.warn("LWJGL STB loader failed, falling back to default font");
        return assetManager.loadFont("Interface/Fonts/Default.fnt");
    }

    /**
     * 设置字体渲染后端
     */
    public void setBackend(FontBackend backend) {
        this.currentBackend = backend;
        logger.info("Font backend switched to: " + backend);
    }

    /**
     * 获取默认字体
     */
    public BitmapFont getDefaultFont() {
        return assetManager.loadFont("Interface/Fonts/Default.fnt");
    }

    /**
     * 清除字体缓存
     */
    public void clearCache() {
        fontCache.clear();
    }
}
