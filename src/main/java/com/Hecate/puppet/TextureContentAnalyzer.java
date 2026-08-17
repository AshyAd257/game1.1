package com.Hecate.puppet;

import com.jme3.asset.AssetManager;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;

import java.nio.ByteBuffer;

/**
 * 贴图内容分析器
 * 用于分析透明贴图的实际内容中心位置
 */
public class TextureContentAnalyzer {

    /**
     * 分析贴图的内容中心偏移
     * 通过检测Alpha通道确定实际可见内容的质心
     *
     * @param assetManager 资源管理器
     * @param texturePath 贴图路径
     * @return 内容中心偏移数组[centerX, centerY]，范围 -0.5 到 +0.5，如果分析失败返回[0, 0]
     */
    public static float[] analyzeContentCenter(AssetManager assetManager, String texturePath) {
        if (texturePath == null || texturePath.isEmpty()) {
            return new float[]{0.0f, 0.0f};
        }

        try {
            // 加载贴图
            Texture2D texture = (Texture2D) assetManager.loadTexture(texturePath);
            if (texture == null) {
                System.err.println("[内容分析] 无法加载贴图: " + texturePath);
                return new float[]{0.0f, 0.0f};
            }

            Image image = texture.getImage();
            if (image == null) {
                System.err.println("[内容分析] 贴图没有图像数据: " + texturePath);
                return new float[]{0.0f, 0.0f};
            }

            // 获取图像尺寸
            int width = image.getWidth();
            int height = image.getHeight();

            // 获取像素数据
            ByteBuffer data = image.getData(0);
            if (data == null) {
                System.err.println("[内容分析] 无法获取图像数据: " + texturePath);
                return new float[]{0.0f, 0.0f};
            }

            // 计算每像素的字节数（假设RGBA格式，4字节）
            Image.Format format = image.getFormat();
            int bytesPerPixel = getBytesPerPixel(format);

            if (bytesPerPixel == 0) {
                System.err.println("[内容分析] 不支持的图像格式: " + format);
                return new float[]{0.0f, 0.0f};
            }

            // 计算Alpha通道的偏移量（通常是第4个字节，索引为3）
            int alphaOffset = getAlphaOffset(format);
            if (alphaOffset == -1) {
                System.err.println("[内容分析] 图像格式没有Alpha通道: " + format);
                return new float[]{0.0f, 0.0f};
            }

            // 统计所有非透明像素的质心
            long sumX = 0;
            long sumY = 0;
            long totalWeight = 0;

            data.rewind(); // 重置buffer位置

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int pixelIndex = (y * width + x) * bytesPerPixel;

                    // 读取alpha值（0-255）
                    int alpha = data.get(pixelIndex + alphaOffset) & 0xFF;

                    // 只计算非透明像素（alpha > 阈值）
                    if (alpha > 10) { // 阈值：几乎完全透明的像素不参与计算
                        sumX += x * alpha;
                        sumY += y * alpha;
                        totalWeight += alpha;
                    }
                }
            }

            if (totalWeight == 0) {
                return new float[]{0.0f, 0.0f};
            }

            // 计算加权质心
            float centroidX = (float) sumX / totalWeight;
            float centroidY = (float) sumY / totalWeight;

            // 转换为相对于中心的偏移（范围 -0.5 到 +0.5）
            // centroid在图像坐标系中，原点在左上角
            // 我们需要转换为以中心为原点的坐标系
            float offsetX = (centroidX - width / 2.0f) / width;
            float offsetY = (centroidY - height / 2.0f) / height;

            // 注意：Y轴方向需要翻转，因为图像坐标系Y轴向下，而我们的坐标系Y轴向上
            offsetY = -offsetY;

            return new float[]{offsetX, offsetY};

        } catch (Exception e) {
            System.err.println("[内容分析] 分析贴图时发生异常: " + texturePath);
            e.printStackTrace();
            return new float[]{0.0f, 0.0f};
        }
    }

    /**
     * 获取图像格式对应的每像素字节数
     */
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
                return 0; // 不支持的格式
        }
    }

    /**
     * 获取Alpha通道在像素数据中的偏移量
     * @return Alpha通道偏移，如果没有Alpha通道返回-1
     */
    private static int getAlphaOffset(Image.Format format) {
        switch (format) {
            case RGBA8:
                return 3; // RGBA顺序，Alpha在第4个字节
            case ABGR8:
                return 0; // ABGR顺序，Alpha在第1个字节
            case Luminance8Alpha8:
                return 1; // LA顺序，Alpha在第2个字节
            case Alpha8:
                return 0; // 纯Alpha通道
            default:
                return -1; // 没有Alpha通道
        }
    }
}
