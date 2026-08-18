package com.Hecate.ui.font;

import com.jme3.asset.AssetManager;
import com.jme3.font.BitmapFont;
import org.lwjgl.PointerBuffer;
import org.lwjgl.util.freetype.FT_Face;
import org.lwjgl.util.freetype.FreeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.ByteBuffer;

import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.util.freetype.FreeType.*;

/**
 * LWJGL FreeType 字体加载器
 * <p>直接使用 FreeType 库加载和渲染 TrueType 字体
 */
public class FreetypeFontLoader {
    private static final Logger logger = LoggerFactory.getLogger(FreetypeFontLoader.class);

    private static long ftLibrary = 0;

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

            // TODO: 生成字形图集
            // 1. 遍历字符集
            // 2. 使用 FT_Load_Char 加载每个字符
            // 3. 使用 FT_Render_Glyph 渲染为位图
            // 4. 打包到纹理图集
            // 5. 创建 BitmapFont

            // 清理
            FT_Done_Face(face);
            memFree(fontBuffer);

            logger.warn("FreeType font loading partially implemented");
            return null;

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
