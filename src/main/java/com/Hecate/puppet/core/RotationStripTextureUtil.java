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
     * 获取（必要时生成）补齐到至少requiredSteps像素宽的条状贴图（左侧不额外留白，向后兼容旧调用）
     */
    public static PaddedStrip getOrCreatePaddedStrip(AssetManager assetManager, String texturePath, int requiredSteps) {
        return getOrCreatePaddedStrip(assetManager, texturePath, 0, requiredSteps);
    }

    /**
     * 获取（必要时生成）补齐后的条状贴图，支持在左侧预留一段透明像素的"校准余量"。
     *
     * 校准偏移允许为负数（比如摄像机朝向对应的采样步数比用户手动选定的取景框像素还大），
     * 这时候取景框要往贴图左边界以外取样。原贴图内容不能真的往左边移，所以改用在内存里
     * 生成的临时贴图左侧插入leftMarginPx像素的透明留白，调用方后续把逻辑像素坐标一律
     * 加上leftMarginPx就能落在这张贴图的有效范围内，取景框仍能按固定1像素/步连续滑动，
     * 不会因为越界被硬夹在边界上产生"卡住不动"的问题。
     *
     * @param assetManager 资源管理器
     * @param texturePath 原始条状贴图路径
     * @param leftMarginPx 左侧预留的透明留白像素数（>=0，通常是一个固定常量，与校准偏移的
     *                     理论最小负值相匹配，比如STEPS_PER_REVOLUTION-1）
     * @param requiredWidthPx 除左侧留白外，右侧（原图内容+右侧补齐）总共需要的最小像素宽度
     * @return 补齐后的贴图信息（paddedWidthPx已包含左侧留白），如果加载失败返回null
     */
    public static PaddedStrip getOrCreatePaddedStrip(AssetManager assetManager, String texturePath, int leftMarginPx, int requiredWidthPx) {
        if (texturePath == null || texturePath.isEmpty() || assetManager == null) {
            return null;
        }

        int effectiveLeftMargin = Math.max(0, leftMarginPx);
        int effectiveRequiredWidth = Math.max(1, requiredWidthPx);
        String cacheKey = texturePath + "#" + effectiveLeftMargin + "#" + effectiveRequiredWidth;

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
            if (effectiveLeftMargin == 0 && width >= effectiveRequiredWidth) {
                // 不需要左侧留白，且原贴图已足够宽，直接复用（不生成新贴图，不额外占用内存）
                Texture2D texture2D = (rawTexture instanceof Texture2D)
                        ? (Texture2D) rawTexture
                        : new Texture2D(sourceImage);
                texture2D.setMagFilter(Texture.MagFilter.Nearest);
                texture2D.setMinFilter(Texture.MinFilter.NearestNoMipMaps);
                result = new PaddedStrip(texture2D, width, height);
            } else {
                int totalWidth = effectiveLeftMargin + Math.max(width, effectiveRequiredWidth);
                result = createPaddedTexture(sourceImage, width, height, effectiveLeftMargin, totalWidth);
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
     * 生成一张两侧补齐透明像素的加宽贴图
     * 左侧leftMargin像素透明留白，中间保留原始像素内容，右侧补齐部分也完全透明（alpha=0，不是白色）
     */
    private static PaddedStrip createPaddedTexture(Image sourceImage, int srcWidth, int srcHeight, int leftMargin, int paddedWidth) {
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
                int srcX = x - leftMargin;
                if (srcX >= 0 && srcX < srcWidth) {
                    int srcIndex = (y * srcWidth + srcX) * bytesPerPixel;
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
                    // 左侧留白或右侧补齐区域：完全透明（不是白色）
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
