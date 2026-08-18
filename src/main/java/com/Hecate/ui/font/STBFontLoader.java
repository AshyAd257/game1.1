package com.Hecate.ui.font;

import com.jme3.asset.AssetManager;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;
import com.jme3.texture.image.ColorSpace;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTruetype;
import org.lwjgl.system.MemoryStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

/**
 * LWJGL STB TrueType 字体加载器
 * <p>使用 stb_truetype 库直接加载 TTF 字体
 */
public class STBFontLoader {
    private static final Logger logger = LoggerFactory.getLogger(STBFontLoader.class);

    /**
     * 加载 TTF 字体并生成位图
     *
     * @param assetManager JME3 资源管理器
     * @param fontPath 字体文件路径（相对于 assets）
     * @param fontSize 字体大小（像素）
     * @return BitmapFont 对象，失败返回 null
     */
    public static BitmapFont loadFont(AssetManager assetManager, String fontPath, int fontSize) {
        try {
            // 读取 TTF 文件
            InputStream stream = assetManager.getClass().getResourceAsStream("/" + fontPath);
            if (stream == null) {
                logger.error("Font file not found: " + fontPath);
                return null;
            }

            byte[] fontData = stream.readAllBytes();
            ByteBuffer fontBuffer = BufferUtils.createByteBuffer(fontData.length);
            fontBuffer.put(fontData);
            fontBuffer.flip();

            // 初始化 STB 字体信息
            STBTTFontinfo fontInfo = STBTTFontinfo.create();
            if (!STBTruetype.stbtt_InitFont(fontInfo, fontBuffer)) {
                logger.error("Failed to initialize font: " + fontPath);
                return null;
            }

            logger.info("Successfully loaded STB font: {} (size: {})", fontPath, fontSize);

            // TODO: 生成字形纹理图集和字体度量数据
            // 这需要：
            // 1. 为每个字符生成位图
            // 2. 将所有字符打包到一个纹理图集中
            // 3. 创建 BitmapFont 和 BitmapCharacterSet
            // 4. 设置字符度量信息（宽度、高度、偏移等）

            logger.warn("STB font loading partially implemented, returning null");
            return null;

        } catch (Exception e) {
            logger.error("Error loading STB font: " + fontPath, e);
            return null;
        }
    }

    /**
     * 生成字形纹理图集
     *
     * @param fontInfo STB 字体信息
     * @param fontSize 字体大小
     * @param charRange 字符范围（例如：ASCII 32-126）
     * @return 纹理图集数据
     */
    private static Image generateFontAtlas(STBTTFontinfo fontInfo, int fontSize, int[] charRange) {
        // TODO: 实现字形图集生成
        // 1. 计算所需纹理大小
        // 2. 为每个字符渲染位图
        // 3. 打包到纹理图集
        // 4. 返回 JME3 Image 对象

        logger.warn("Font atlas generation not yet implemented");
        return null;
    }
}
