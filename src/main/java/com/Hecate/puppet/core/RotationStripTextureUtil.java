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
 * 负责加载"环绕360°的条状贴图"，并保证贴图内容宽度正好等于一圈的步数
 * （framesPerRevolution，一步一像素），这样才能配合GPU的Repeat环绕模式实现
 * 真正无缝的360°环形取样：转到第0步和转到第framesPerRevolution步（即绕回
 * 第0步）之间，硬件采样会自然衔接，不会有整段跳变。
 *
 * 如果原始贴图宽度不等于framesPerRevolution，会在内存中生成一张宽度恰好为
 * framesPerRevolution的版本（不修改原始贴图文件）：
 * - 原图更窄：右侧用透明像素（alpha=0，不是白色）补齐
 * - 原图更宽：只使用最前面framesPerRevolution列，多出的部分不会被采样到
 * 两种情况都会打印警告，提醒贴图内容宽度应该恰好匹配一圈步数。
 *
 * 补齐/裁剪后的贴图会按 (贴图路径 + 所需步数) 缓存，避免每帧重新生成。
 */
public class RotationStripTextureUtil {

    /**
     * 环形条状贴图信息
     */
    public static class RingStrip {
        public final Texture2D texture;
        public final int widthPx;  // 始终等于framesPerRevolution
        public final int heightPx;

        RingStrip(Texture2D texture, int widthPx, int heightPx) {
            this.texture = texture;
            this.widthPx = widthPx;
            this.heightPx = heightPx;
        }
    }

    // 缓存key: 贴图路径 + "#" + 一圈步数 -> 环形贴图
    private static final Map<String, RingStrip> cache = new HashMap<>();

    /**
     * 获取（必要时生成）宽度恰好为framesPerRevolution像素、且开启了S轴Repeat环绕的条状贴图。
     *
     * @param assetManager 资源管理器
     * @param texturePath 原始条状贴图路径
     * @param framesPerRevolution 一圈的步数（贴图内容的周期宽度，单位像素）
     * @return 环形贴图信息，如果加载失败返回null
     */
    public static RingStrip getOrCreateRingStrip(AssetManager assetManager, String texturePath, int framesPerRevolution) {
        if (texturePath == null || texturePath.isEmpty() || assetManager == null) {
            return null;
        }

        int targetWidth = Math.max(1, framesPerRevolution);
        String cacheKey = texturePath + "#ring#" + targetWidth;

        RingStrip cached = cache.get(cacheKey);
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

            RingStrip result;
            if (width == targetWidth) {
                // 宽度正好匹配一圈步数，直接复用（不生成新贴图，不额外占用内存）
                Texture2D texture2D = (rawTexture instanceof Texture2D)
                        ? (Texture2D) rawTexture
                        : new Texture2D(sourceImage);
                configureRingTexture(texture2D);
                result = new RingStrip(texture2D, width, height);
            } else {
                if (width > targetWidth) {
                    System.err.println("[RotationStripTextureUtil] 贴图宽度 " + width
                            + "px 超过一圈所需的 " + targetWidth
                            + "px，多出的列不会被使用（贴图内容周期必须正好等于一圈步数才能无缝环绕）: " + texturePath);
                } else {
                    System.err.println("[RotationStripTextureUtil] 贴图宽度 " + width
                            + "px 小于一圈所需的 " + targetWidth
                            + "px，右侧将用透明像素补齐到 " + targetWidth + "px: " + texturePath);
                }
                result = createRingTexture(sourceImage, width, height, targetWidth);
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
     * 给环形贴图配置最近邻过滤（不产生半像素糊边）和S轴Repeat环绕（支持360°无缝取样，
     * 也支持任意正负U坐标——GPU的Repeat会对U坐标自动取小数部分，等价于取模，
     * 越界坐标不需要在CPU侧夹紧或做留白）。T轴使用EdgeClamp，因为取景框高度方向
     * 不需要环绕，用EdgeClamp避免边缘因为过滤或极端UV值意外取到对面的像素。
     */
    private static void configureRingTexture(Texture2D texture) {
        texture.setMagFilter(Texture.MagFilter.Nearest);
        texture.setMinFilter(Texture.MinFilter.NearestNoMipMaps);
        texture.setWrap(Texture.WrapAxis.S, Texture.WrapMode.Repeat);
        texture.setWrap(Texture.WrapAxis.T, Texture.WrapMode.EdgeClamp);
    }

    /**
     * 生成一张宽度恰好为targetWidth的贴图：原图内容从左对齐拷贝
     * min(srcWidth, targetWidth)列，多出的宽度（仅当srcWidth<targetWidth时）
     * 用透明像素补齐（alpha=0，不是白色）；原图更宽的部分直接丢弃不拷贝。
     */
    private static RingStrip createRingTexture(Image sourceImage, int srcWidth, int srcHeight, int targetWidth) {
        ByteBuffer srcData = sourceImage.getData(0);
        Image.Format format = sourceImage.getFormat();
        int bytesPerPixel = getBytesPerPixel(format);

        if (bytesPerPixel == 0 || srcData == null) {
            System.err.println("[RotationStripTextureUtil] 不支持的图像格式，无法处理: " + format);
            // 无法读取像素数据时，仍返回一张全透明的占位贴图，保证渲染不崩
            ByteBuffer blankBuffer = BufferUtils.createByteBuffer(targetWidth * srcHeight * 4);
            for (int i = 0; i < targetWidth * srcHeight; i++) {
                blankBuffer.put((byte) 0).put((byte) 0).put((byte) 0).put((byte) 0);
            }
            blankBuffer.flip();
            Image blankImage = new Image(Image.Format.RGBA8, targetWidth, srcHeight, blankBuffer, ColorSpace.Linear);
            Texture2D blankTexture = new Texture2D(blankImage);
            configureRingTexture(blankTexture);
            return new RingStrip(blankTexture, targetWidth, srcHeight);
        }

        int alphaOffset = getAlphaOffset(format);
        boolean hasAlpha = alphaOffset != -1;
        int copyWidth = Math.min(srcWidth, targetWidth);

        ByteBuffer destBuffer = BufferUtils.createByteBuffer(targetWidth * srcHeight * 4);
        srcData.rewind();

        for (int y = 0; y < srcHeight; y++) {
            for (int x = 0; x < targetWidth; x++) {
                if (x < copyWidth) {
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
                    // 右侧补齐区域（仅当原图比目标窄时才会走到这里）：完全透明（不是白色）
                    destBuffer.put((byte) 0).put((byte) 0).put((byte) 0).put((byte) 0);
                }
            }
        }
        destBuffer.flip();

        Image ringImage = new Image(Image.Format.RGBA8, targetWidth, srcHeight, destBuffer, ColorSpace.Linear);
        Texture2D ringTexture = new Texture2D(ringImage);
        configureRingTexture(ringTexture);

        return new RingStrip(ringTexture, targetWidth, srcHeight);
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
