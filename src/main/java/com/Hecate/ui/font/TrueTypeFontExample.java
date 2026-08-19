package com.Hecate.ui.font;

import com.atr.jme.font.TrueTypeBMP;
import com.atr.jme.font.asset.TrueTypeLoader;
import com.atr.jme.font.asset.TrueTypeKeyBMP;
import com.atr.jme.font.shape.TrueTypeText;
import com.atr.jme.font.util.Style;
import com.jme3.app.SimpleApplication;
import com.jme3.asset.AssetManager;
import com.jme3.math.ColorRGBA;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TTF 字体加载示例
 * 使用 jme-ttf 库的正确方式：通过 AssetManager 和 TrueTypeKeyBMP
 */
public class TrueTypeFontExample {

    private static final Logger logger = LoggerFactory.getLogger(TrueTypeFontExample.class);
    private static boolean loaderRegistered = false;

    /**
     * 注册 TTF 字体加载器（整个应用只需注册一次）
     */
    public static void registerLoader(AssetManager assetManager) {
        if (!loaderRegistered) {
            assetManager.registerLoader(TrueTypeLoader.class, "ttf");
            loaderRegistered = true;
            logger.info("✓ TrueTypeLoader registered for .ttf files");
        }
    }

    /**
     * 加载并创建 TTF 字体文本
     * @param app 应用实例
     * @param text 要显示的文字
     * @param fontPath TTF 字体文件路径（相对于 assets 根目录）
     * @param fontSize 字体大小（单位：点）
     * @return TrueTypeText 节点（可直接添加到 guiNode）
     */
    public static TrueTypeText createText(SimpleApplication app, String text, String fontPath, int fontSize) {
        AssetManager assetManager = app.getAssetManager();

        try {
            // 1. 确保加载器已注册
            registerLoader(assetManager);

            // 2. 创建字体加载 Key
            TrueTypeKeyBMP fontKey = new TrueTypeKeyBMP(fontPath, Style.Plain, fontSize);

            // 3. 加载字体
            TrueTypeBMP font = (TrueTypeBMP) assetManager.loadAsset(fontKey);

            // 4. 创建文本节点（需要两个颜色参数：前景色和背景色）
            TrueTypeText textNode = font.getText(text, 0, ColorRGBA.White, ColorRGBA.BlackNoAlpha);

            logger.info("✓ TTF text created: \"{}\" using font: {} (size: {}pt)", text, fontPath, fontSize);
            return textNode;

        } catch (Exception e) {
            logger.error("Failed to create TTF text: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 创建带样式和颜色的 TTF 文本
     */
    public static TrueTypeText createStyledText(
            SimpleApplication app,
            String text,
            String fontPath,
            int fontSize,
            Style style,
            ColorRGBA color
    ) {
        AssetManager assetManager = app.getAssetManager();

        try {
            registerLoader(assetManager);

            TrueTypeKeyBMP fontKey = new TrueTypeKeyBMP(fontPath, style, fontSize);
            TrueTypeBMP font = (TrueTypeBMP) assetManager.loadAsset(fontKey);
            TrueTypeText textNode = font.getText(text, 0, color, ColorRGBA.BlackNoAlpha);

            logger.info("✓ Styled TTF text created: \"{}\" (style: {}, color: {})", text, style, color);
            return textNode;

        } catch (Exception e) {
            logger.error("Failed to create styled TTF text: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 创建缩放的高质量文本（推荐用于小字号）
     * 官方文档推荐：对于小于 53pt 的字体，使用更大的字号渲染后缩小，可以获得更清晰的效果
     */
    public static TrueTypeText createScaledText(
            SimpleApplication app,
            String text,
            String fontPath,
            int targetSize,
            ColorRGBA color
    ) {
        AssetManager assetManager = app.getAssetManager();

        try {
            registerLoader(assetManager);

            // 使用较大的渲染尺寸
            int renderSize = (int) (targetSize * 1.5f);
            float scale = (float) targetSize / renderSize;

            TrueTypeKeyBMP fontKey = new TrueTypeKeyBMP(fontPath, Style.Plain, renderSize);
            TrueTypeBMP font = (TrueTypeBMP) assetManager.loadAsset(fontKey);
            font.setScale(scale);

            TrueTypeText textNode = font.getText(text, 0, color, ColorRGBA.BlackNoAlpha);

            logger.info("✓ Scaled TTF text created: \"{}\" (target: {}pt, render: {}pt, scale: {})",
                       text, targetSize, renderSize, scale);
            return textNode;

        } catch (Exception e) {
            logger.error("Failed to create scaled TTF text: {}", e.getMessage(), e);
            return null;
        }
    }
}
