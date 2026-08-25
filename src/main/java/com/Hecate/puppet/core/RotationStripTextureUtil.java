package com.Hecate.puppet.core;

import com.jme3.asset.AssetManager;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.texture.image.ColorSpace;
import com.jme3.util.BufferUtils;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * 旋转条状贴图工具类
 *
 * 负责加载"环绕360°的条状贴图"，并在贴图实际像素宽度不足以容纳所需档数时，
 * 在内存中生成一张右侧补齐透明像素的加宽版本（不修改原始贴图文件）。
 *
 * 补齐后的贴图会按 (贴图路径 + 所需档数) 缓存，避免每帧重新生成。
 */
public class RotationStripTextureUtil {

    /**
     * 缓存的补齐贴图信息
     */
    public static class PaddedStrip {
        public final Texture2D texture;
        public final int paddedWidthPx;
        public final int heightPx;

        PaddedStrip(Texture2D texture, int paddedWidthPx, int heightPx) {
            this.texture = texture;
            this.paddedWidthPx = paddedWidthPx;
            this.heightPx = heightPx;
        }
    }

    // 缓存key: 贴图路径 + "#" + 所需档数 -> 补齐后的贴图
    private static final Map<String, PaddedStrip> cache = new HashMap<>();

    /**
     * 获取（必要时生成）补齐到至少requiredSteps像素宽的条状贴图
     *
     * @param assetManager 资源管理器
     * @param texturePath 原始条状贴图路径
     * @param requiredSteps 所需的最小像素宽度（对应方向档数，档数<=0时不需要补齐）
     * @return 补齐后的贴图信息，如果加载失败返回null
     */
    public static PaddedStrip getOrCreatePaddedStrip(AssetManager assetManager, String texturePath, int requiredSteps) {
        if (texturePath == null || texturePath.isEmpty() || assetManager == null) {
            return null;
        }

        int effectiveRequiredSteps = Math.max(1, requiredSteps);
        String cacheKey = texturePath + "#" + effectiveRequiredSteps;

        PaddedStrip cached = cache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        try {
            Texture rawTexture = assetManager.loadTexture(texturePath);
            Image sourceImage = rawTexture.getImage();
            if (sourceImage == null) {
                return null;
            }

            int width = sourceImage.getWidth();
            int height = sourceImage.getHeight();

            PaddedStrip result;
            if (width >= effectiveRequiredSteps) {
                // 原贴图已足够宽，直接复用（不生成新贴图，不额外占用内存）
                Texture2D texture2D = (rawTexture instanceof Texture2D)
                        ? (Texture2D) rawTexture
                        : new Texture2D(sourceImage);
                texture2D.setMagFilter(Texture.MagFilter.Nearest);
                texture2D.setMinFilter(Texture.MinFilter.NearestNoMipMaps);
                result = new PaddedStrip(texture2D, width, height);
            } else {
                result = createPaddedTexture(sourceImage, width, height, effectiveRequiredSteps);
            }

            cache.put(cacheKey, result);
            return result;

        } catch (Exception e) {
            System.err.println("[RotationStripTextureUtil] 加载条状贴图失败: " + texturePath);
            System.err.println("[RotationStripTextureUtil] 错误信息: " + e.getMessage());
            return null;
        }
    }

    /**
     * 生成一张右侧补齐透明像素的加宽贴图
     * 左侧保留原始像素内容，右侧补齐部分完全透明（alpha=0），不是白色
     */
    private static PaddedStrip createPaddedTexture(Image sourceImage, int srcWidth, int srcHeight, int paddedWidth) {
        ByteBuffer srcData = sourceImage.getData(0);
        Image.Format format = sourceImage.getFormat();
        int bytesPerPixel = getBytesPerPixel(format);

        if (bytesPerPixel == 0 || srcData == null) {
            System.err.println("[RotationStripTextureUtil] 不支持的图像格式，无法补齐: " + format);
            // 无法读取像素数据时，仍返回一张全透明的占位贴图，保证渲染不崩
            ByteBuffer blankBuffer = BufferUtils.createByteBuffer(paddedWidth * srcHeight * 4);
            for (int i = 0; i < paddedWidth * srcHeight; i++) {
                blankBuffer.put((byte) 0).put((byte) 0).put((byte) 0).put((byte) 0);
            }
            blankBuffer.flip();
            Image blankImage = new Image(Image.Format.RGBA8, paddedWidth, srcHeight, blankBuffer, ColorSpace.Linear);
            Texture2D blankTexture = new Texture2D(blankImage);
            blankTexture.setMagFilter(Texture.MagFilter.Nearest);
            blankTexture.setMinFilter(Texture.MinFilter.NearestNoMipMaps);
            return new PaddedStrip(blankTexture, paddedWidth, srcHeight);
        }

        int alphaOffset = getAlphaOffset(format);
        boolean hasAlpha = alphaOffset != -1;

        ByteBuffer destBuffer = BufferUtils.createByteBuffer(paddedWidth * srcHeight * 4);
        srcData.rewind();

        for (int y = 0; y < srcHeight; y++) {
            for (int x = 0; x < paddedWidth; x++) {
                if (x < srcWidth) {
                    int srcIndex = (y * srcWidth + x) * bytesPerPixel;
                    int r, g, b, a;

                    switch (format) {
                        case RGBA8:
                            r = srcData.get(srcIndex) & 0xFF;
                            g = srcData.get(srcIndex + 1) & 0xFF;
                            b = srcData.get(srcIndex + 2) & 0xFF;
                            a = srcData.get(srcIndex + 3) & 0xFF;
                            break;
                        case ABGR8:
                            a = srcData.get(srcIndex) & 0xFF;
                            b = srcData.get(srcIndex + 1) & 0xFF;
                            g = srcData.get(srcIndex + 2) & 0xFF;
                            r = srcData.get(srcIndex + 3) & 0xFF;
                            break;
                        case RGB8:
                            r = srcData.get(srcIndex) & 0xFF;
                            g = srcData.get(srcIndex + 1) & 0xFF;
                            b = srcData.get(srcIndex + 2) & 0xFF;
                            a = 255;
                            break;
                        case BGR8:
                            b = srcData.get(srcIndex) & 0xFF;
                            g = srcData.get(srcIndex + 1) & 0xFF;
                            r = srcData.get(srcIndex + 2) & 0xFF;
                            a = 255;
                            break;
                        default:
                            r = g = b = 0;
                            a = hasAlpha ? (srcData.get(srcIndex + alphaOffset) & 0xFF) : 255;
                            break;
                    }

                    destBuffer.put((byte) r).put((byte) g).put((byte) b).put((byte) a);
                } else {
                    // 补齐区域：完全透明（不是白色）
                    destBuffer.put((byte) 0).put((byte) 0).put((byte) 0).put((byte) 0);
                }
            }
        }
        destBuffer.flip();

        Image paddedImage = new Image(Image.Format.RGBA8, paddedWidth, srcHeight, destBuffer, ColorSpace.Linear);
        Texture2D paddedTexture = new Texture2D(paddedImage);
        paddedTexture.setMagFilter(Texture.MagFilter.Nearest);
        paddedTexture.setMinFilter(Texture.MinFilter.NearestNoMipMaps);

        return new PaddedStrip(paddedTexture, paddedWidth, srcHeight);
    }

    private static int getBytesPerPixel(Image.Format format) {
        switch (format) {
            case RGBA8:
            case ABGR8:
                return 4;
            case RGB8:
            case BGR8:
                return 3;
            case Luminance8Alpha8:
                return 2;
            case Luminance8:
            case Alpha8:
                return 1;
            default:
                return 0;
        }
    }

    private static int getAlphaOffset(Image.Format format) {
        switch (format) {
            case RGBA8:
                return 3;
            case ABGR8:
                return 0;
            case Luminance8Alpha8:
                return 1;
            case Alpha8:
                return 0;
            default:
                return -1;
        }
    }

    /**
     * 清空缓存（贴图热重载时可调用）
     */
    public static void clearCache() {
        cache.clear();
    }
}
