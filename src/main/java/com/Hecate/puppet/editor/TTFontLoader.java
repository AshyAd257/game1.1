package com.Hecate.puppet.editor;

import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapCharacter;
import com.jme3.font.BitmapText;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.texture.Image;
import com.jme3.texture.image.ColorSpace;
import com.jme3.util.BufferUtils;
import com.jme3.asset.AssetManager;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * TTF字体加载器 - 直接使用TrueType字体，无需BMFont
 */
public class TTFontLoader {

    private Font awtFont;
    private AssetManager assetManager;
    private Map<Character, CharInfo> charCache = new HashMap<>();
    private float fontSize = 14f;

    public static class CharInfo {
        public Texture texture;
        public int width;
        public int height;
        public int xOffset;
        public int yOffset;
    }

    /**
     * 从TTF文件路径加载字体（文件系统）
     */
    public static TTFontLoader loadFont(AssetManager assetManager, String ttfPath, float size) {
        try {
            File fontFile = new File(ttfPath);
            Font baseFont = Font.createFont(Font.TRUETYPE_FONT, fontFile);
            Font font = baseFont.deriveFont(size);

            TTFontLoader loader = new TTFontLoader();
            loader.awtFont = font;
            loader.assetManager = assetManager;
            loader.fontSize = size;

            return loader;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 从resources加载字体（JAR内部资源）
     * @param assetManager jME AssetManager
     * @param resourcePath 资源路径，例如"Fonts/MyFont.ttf"
     * @param size 字体大小
     */
    public static TTFontLoader loadFontFromResource(AssetManager assetManager, String resourcePath, float size) {
        try {
            // 尝试从AssetManager加载
            java.io.InputStream fontStream = assetManager.getClass()
                .getClassLoader()
                .getResourceAsStream(resourcePath);

            if (fontStream == null) {
                return null;
            }

            Font baseFont = Font.createFont(Font.TRUETYPE_FONT, fontStream);
            Font font = baseFont.deriveFont(size);
            fontStream.close();

            TTFontLoader loader = new TTFontLoader();
            loader.awtFont = font;
            loader.assetManager = assetManager;
            loader.fontSize = size;

            return loader;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 创建文本节点
     */
    public Node createText(String text, ColorRGBA color) {
        Node textNode = new Node("TTFText");

        float xPos = 0;
        float yPos = 0;

        for (char c : text.toCharArray()) {
            if (c == '\n') {
                yPos -= fontSize * 1.5f;
                xPos = 0;
                continue;
            }

            CharInfo charInfo = getCharInfo(c);
            if (charInfo == null) continue;

            // 创建字符几何体
            Geometry charGeom = createCharGeometry(charInfo, color);
            charGeom.setLocalTranslation(xPos + charInfo.xOffset, yPos - charInfo.yOffset, 0);
            textNode.attachChild(charGeom);

            xPos += charInfo.width;
        }

        return textNode;
    }

    /**
     * 获取字符信息（带缓存）
     */
    private CharInfo getCharInfo(char c) {
        if (charCache.containsKey(c)) {
            return charCache.get(c);
        }

        CharInfo info = renderChar(c);
        charCache.put(c, info);
        return info;
    }

    /**
     * 渲染单个字符到纹理
     */
    private CharInfo renderChar(char c) {
        // 测量字符大小
        BufferedImage tempImg = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D tempG = tempImg.createGraphics();
        tempG.setFont(awtFont);
        FontMetrics fm = tempG.getFontMetrics();
        FontRenderContext frc = tempG.getFontRenderContext();
        Rectangle2D bounds = awtFont.getStringBounds(String.valueOf(c), frc);
        tempG.dispose();

        int width = (int) Math.ceil(bounds.getWidth()) + 4;
        int height = fm.getHeight() + 4;

        if (width <= 0) width = 1;
        if (height <= 0) height = 1;

        // 渲染字符
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(awtFont);
        g.setColor(Color.WHITE);
        g.drawString(String.valueOf(c), 2, fm.getAscent() + 2);
        g.dispose();

        // 垂直翻转图像（修复OpenGL坐标系问题）
        img = flipVertical(img);

        // 转换为JME纹理
        CharInfo info = new CharInfo();
        info.texture = convertToTexture(img);
        info.width = width;
        info.height = height;
        info.xOffset = 0;
        info.yOffset = fm.getAscent() + 2;

        return info;
    }

    /**
     * 将BufferedImage转换为JME纹理
     */
    private Texture convertToTexture(BufferedImage img) {
        int width = img.getWidth();
        int height = img.getHeight();

        ByteBuffer buffer = BufferUtils.createByteBuffer(width * height * 4);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = img.getRGB(x, y);
                buffer.put((byte) ((pixel >> 16) & 0xFF)); // R
                buffer.put((byte) ((pixel >> 8) & 0xFF));  // G
                buffer.put((byte) (pixel & 0xFF));         // B
                buffer.put((byte) ((pixel >> 24) & 0xFF)); // A
            }
        }
        buffer.flip();

        Image jmeImage = new Image(Image.Format.RGBA8, width, height, buffer, ColorSpace.Linear);
        Texture texture = new Texture2D(jmeImage);
        texture.setMinFilter(Texture.MinFilter.BilinearNoMipMaps);
        texture.setMagFilter(Texture.MagFilter.Bilinear);

        return texture;
    }

    /**
     * 创建字符几何体
     */
    private Geometry createCharGeometry(CharInfo info, ColorRGBA color) {
        // 使用简单的四边形
        com.jme3.scene.shape.Quad quad = new com.jme3.scene.shape.Quad(info.width, info.height);
        Geometry geom = new Geometry("char", quad);

        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setTexture("ColorMap", info.texture);
        mat.setColor("Color", color);
        mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);

        geom.setMaterial(mat);
        geom.setQueueBucket(RenderQueue.Bucket.Gui);

        return geom;
    }

    /**
     * 计算文本宽度
     */
    public float getTextWidth(String text) {
        float width = 0;
        for (char c : text.toCharArray()) {
            CharInfo info = getCharInfo(c);
            if (info != null) {
                width += info.width;
            }
        }
        return width;
    }

    /**
     * 获取字体高度
     */
    public float getLineHeight() {
        return fontSize * 1.5f;
    }

    /**
     * 垂直翻转BufferedImage（修复OpenGL Y轴反向问题）
     */
    private BufferedImage flipVertical(BufferedImage src) {
        int width = src.getWidth();
        int height = src.getHeight();
        BufferedImage flipped = new BufferedImage(width, height, src.getType());

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                flipped.setRGB(x, height - 1 - y, src.getRGB(x, y));
            }
        }

        return flipped;
    }
}
