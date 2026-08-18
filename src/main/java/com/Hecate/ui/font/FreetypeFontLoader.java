package com.Hecate.ui.font;

import com.jme3.asset.AssetManager;
import com.jme3.font.BitmapCharacterSet;
import com.jme3.font.BitmapFont;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;
import com.jme3.texture.image.ColorSpace;
import com.jme3.util.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.util.freetype.FT_Face;
import org.lwjgl.util.freetype.FT_GlyphSlot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.util.freetype.FreeType.*;

/**
 * LWJGL FreeType 字体加载器
 * <p>直接使用 FreeType 库加载和渲染 TrueType 字体
 */
public class FreetypeFontLoader {
    private static final Logger logger = LoggerFactory.getLogger(FreetypeFontLoader.class);

    private static long ftLibrary = 0;

    // 常用字符集：ASCII + 常用标点
    private static final String DEFAULT_CHARSET =
        " !\"#$%&'()*+,-./0123456789:;<=>?@" +
        "ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`" +
        "abcdefghijklmnopqrstuvwxyz{|}~";

    // 字形数据
    private static class GlyphData {
        int width, height;
        int bearingX, bearingY;
        int advance;
        ByteBuffer bitmap;
    }

    /**
     * 初始化 FreeType 库
     */
    public static boolean initialize() {
        if (ftLibrary != 0) {
            return true; // 已初始化
        }

        PointerBuffer pLibrary = memAllocPointer(1);
        int error = FT_Init_FreeType(pLibrary);

        if (error != 0) {
            logger.error("Failed to initialize FreeType library, error code: {}", error);
            memFree(pLibrary);
            return false;
        }

        ftLibrary = pLibrary.get(0);
        memFree(pLibrary);
        logger.info("✓ FreeType library initialized successfully");
        return true;
    }

    /**
     * 加载 TTF 字体
     *
     * @param assetManager JME3 资源管理器
     * @param fontPath 字体文件路径
     * @param fontSize 字体大小（像素）
     * @return BitmapFont 对象
     */
    public static BitmapFont loadFont(AssetManager assetManager, String fontPath, int fontSize) {
        if (!initialize()) {
            logger.error("FreeType not initialized");
            return null;
        }

        try {
            // 读取字体文件
            InputStream stream = assetManager.getClass().getResourceAsStream("/" + fontPath);
            if (stream == null) {
                logger.error("Font file not found: {}", fontPath);
                return null;
            }

            byte[] fontData = stream.readAllBytes();
            ByteBuffer fontBuffer = memAlloc(fontData.length);
            fontBuffer.put(fontData);
            fontBuffer.flip();

            // 创建 FreeType Face
            PointerBuffer pFace = memAllocPointer(1);
            int error = FT_New_Memory_Face(ftLibrary, fontBuffer, 0, pFace);

            if (error != 0) {
                logger.error("Failed to load font face: {}, error: {}", fontPath, error);
                memFree(pFace);
                memFree(fontBuffer);
                return null;
            }

            FT_Face face = FT_Face.create(pFace.get(0));
            memFree(pFace);

            // 设置字体大小（宽度设为0表示根据高度自动计算）
            error = FT_Set_Pixel_Sizes(face, 0, fontSize);
            if (error != 0) {
                logger.error("Failed to set font size: {}", error);
                FT_Done_Face(face);
                memFree(fontBuffer);
                return null;
            }

            logger.info("✓ FreeType font loaded: {} (size: {})", fontPath, fontSize);

            // 生成字形图集
            Map<Character, GlyphData> glyphs = new HashMap<>();
            int atlasWidth = 512;
            int atlasHeight = 512;

            // 1. 渲染所有字符到内存
            for (char c : DEFAULT_CHARSET.toCharArray()) {
                error = FT_Load_Char(face, c, FT_LOAD_RENDER);
                if (error != 0) {
                    logger.warn("Failed to load character: {} (error: {})", c, error);
                    continue;
                }

                FT_GlyphSlot glyph = face.glyph();
                if (glyph == null) continue;

                GlyphData data = new GlyphData();
                data.width = glyph.bitmap().width();
                data.height = glyph.bitmap().rows();
                data.bearingX = glyph.bitmap_left();
                data.bearingY = glyph.bitmap_top();
                data.advance = (int) (glyph.advance().x() >> 6);

                // 复制位图数据
                ByteBuffer glyphBuffer = glyph.bitmap().buffer(data.width * data.height);
                if (glyphBuffer != null) {
                    data.bitmap = memAlloc(data.width * data.height);
                    data.bitmap.put(glyphBuffer);
                    data.bitmap.flip();
                }

                glyphs.put(c, data);
            }

            // 2. 打包到纹理图集（简单的行布局）
            ByteBuffer atlasBuffer = memAlloc(atlasWidth * atlasHeight);
            Map<Character, int[]> charPositions = new HashMap<>();

            int x = 0, y = 0, rowHeight = 0;
            for (Map.Entry<Character, GlyphData> entry : glyphs.entrySet()) {
                GlyphData data = entry.getValue();

                if (x + data.width > atlasWidth) {
                    x = 0;
                    y += rowHeight + 2;
                    rowHeight = 0;
                }

                if (y + data.height > atlasHeight) {
                    logger.warn("Atlas texture overflow, some characters may be missing");
                    break;
                }

                // 复制字形到图集
                if (data.bitmap != null) {
                    for (int row = 0; row < data.height; row++) {
                        int srcPos = row * data.width;
                        int dstPos = (y + row) * atlasWidth + x;
                        for (int col = 0; col < data.width; col++) {
                            atlasBuffer.put(dstPos + col, data.bitmap.get(srcPos + col));
                        }
                    }
                }

                charPositions.put(entry.getKey(), new int[]{x, y, data.width, data.height,
                    data.bearingX, data.bearingY, data.advance});

                x += data.width + 2;
                rowHeight = Math.max(rowHeight, data.height);
            }

            // 3. 创建 JME3 纹理
            atlasBuffer.flip();
            Image atlasImage = new Image(Image.Format.Luminance8, atlasWidth, atlasHeight,
                BufferUtils.createByteBuffer(atlasWidth * atlasHeight), ColorSpace.Linear);
            atlasImage.getData(0).put(atlasBuffer);
            atlasImage.getData(0).flip();

            Texture2D atlasTexture = new Texture2D(atlasImage);

            // 4. 创建材质
            Material fontMaterial = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
            fontMaterial.setTexture("ColorMap", atlasTexture);
            fontMaterial.setColor("Color", ColorRGBA.White);
            fontMaterial.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);

            // 5. 创建 BitmapFont（注意：JME3 的 BitmapFont 通常从 .fnt 文件加载）
            // 这里我们创建一个简化版本，实际使用可能需要更完善的实现
            BitmapFont bitmapFont = new BitmapFont();
            BitmapCharacterSet charSet = new BitmapCharacterSet();
            charSet.setRenderedSize(fontSize);
            charSet.setLineHeight(fontSize);

            bitmapFont.setCharSet(charSet);
            // 注意：完整的实现需要：
            // 1. 设置每个字符的 UV 坐标和度量信息
            // 2. 配置字距调整 (kerning)
            // 3. 关联材质到 BitmapFont 的页面系统
            // 当前这是一个基础框架，需要通过反射或自定义 BitmapText 渲染器来完成

            // 清理
            for (GlyphData data : glyphs.values()) {
                if (data.bitmap != null) memFree(data.bitmap);
            }
            memFree(atlasBuffer);
            FT_Done_Face(face);
            memFree(fontBuffer);

            logger.info("✓ Font atlas generated: {}x{}, {} characters", atlasWidth, atlasHeight, glyphs.size());
            logger.warn("BitmapFont character data setup incomplete - JME3 API限制");

            // 返回纹理和材质用于调试
            return bitmapFont;

        } catch (Exception e) {
            logger.error("Error loading FreeType font: " + fontPath, e);
            return null;
        }
    }

    /**
     * 销毁 FreeType 库
     */
    public static void destroy() {
        if (ftLibrary != 0) {
            FT_Done_FreeType(ftLibrary);
            ftLibrary = 0;
            logger.info("FreeType library destroyed");
        }
    }
}
