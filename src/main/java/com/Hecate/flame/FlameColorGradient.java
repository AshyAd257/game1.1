package com.Hecate.flame;

import com.jme3.math.ColorRGBA;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;
import com.jme3.texture.image.ColorSpace;
import com.jme3.util.BufferUtils;

import java.nio.ByteBuffer;

/**
 * 火焰颜色渐变生成器
 * 生成一张 Nx1 的渐变纹理：深灰 → 灰橙 → 橙红 → 亮橙 → 金黄 → 白
 */
public class FlameColorGradient {

    /**
     * 创建火焰颜色渐变纹理
     * @param width 渐变宽度（推荐256或512）
     * @return 1D渐变纹理（实际是 width×1 的2D纹理）
     */
    public static Texture2D createGradientTexture(int width) {
        // 创建字节缓冲区（RGBA，每像素4字节）
        ByteBuffer buffer = BufferUtils.createByteBuffer(width * 4);

        // 定义颜色关键点（位置 → 颜色）
        ColorStop[] stops = new ColorStop[] {
            new ColorStop(0.00f, new ColorRGBA(0.3f, 0.3f, 0.3f, 1.0f)),  // 深灰
            new ColorStop(0.15f, new ColorRGBA(0.5f, 0.4f, 0.3f, 1.0f)),  // 灰橙
            new ColorStop(0.35f, new ColorRGBA(0.9f, 0.4f, 0.1f, 1.0f)),  // 橙红
            new ColorStop(0.60f, new ColorRGBA(1.0f, 0.6f, 0.2f, 1.0f)),  // 亮橙
            new ColorStop(0.85f, new ColorRGBA(1.0f, 0.9f, 0.3f, 1.0f)),  // 金黄
            new ColorStop(1.00f, new ColorRGBA(1.0f, 1.0f, 1.0f, 1.0f))   // 白色
        };

        // 生成每个像素的颜色
        for (int i = 0; i < width; i++) {
            float t = (float) i / (width - 1); // 归一化位置 0-1

            // 在颜色关键点之间插值
            ColorRGBA color = interpolateColor(t, stops);

            // 写入缓冲区（RGBA 8bit per channel）
            buffer.put((byte) (color.r * 255));
            buffer.put((byte) (color.g * 255));
            buffer.put((byte) (color.b * 255));
            buffer.put((byte) (color.a * 255));
        }

        buffer.flip();

        // 创建图像
        Image image = new Image(
            Image.Format.RGBA8,
            width,
            1,  // 高度为1
            buffer,
            ColorSpace.sRGB
        );

        // 创建纹理
        Texture2D texture = new Texture2D(image);
        texture.setMagFilter(Texture2D.MagFilter.Bilinear); // 平滑过渡
        texture.setMinFilter(Texture2D.MinFilter.BilinearNoMipMaps);
        texture.setWrap(Texture2D.WrapMode.EdgeClamp); // 边缘夹紧

        return texture;
    }

    /**
     * 在颜色关键点之间插值
     */
    private static ColorRGBA interpolateColor(float t, ColorStop[] stops) {
        // 找到 t 所在的两个关键点
        for (int i = 0; i < stops.length - 1; i++) {
            ColorStop stop1 = stops[i];
            ColorStop stop2 = stops[i + 1];

            if (t >= stop1.position && t <= stop2.position) {
                // 计算在这两个关键点之间的位置
                float localT = (t - stop1.position) / (stop2.position - stop1.position);

                // 线性插值
                return new ColorRGBA(
                    lerp(stop1.color.r, stop2.color.r, localT),
                    lerp(stop1.color.g, stop2.color.g, localT),
                    lerp(stop1.color.b, stop2.color.b, localT),
                    lerp(stop1.color.a, stop2.color.a, localT)
                );
            }
        }

        // 超出范围，返回最后一个颜色
        return stops[stops.length - 1].color;
    }

    /**
     * 线性插值
     */
    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    /**
     * 颜色停靠点
     */
    private static class ColorStop {
        float position;   // 0-1
        ColorRGBA color;

        ColorStop(float position, ColorRGBA color) {
            this.position = position;
            this.color = color;
        }
    }
}
