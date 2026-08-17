package com.Hecate.puppet.config;

import com.jme3.math.ColorRGBA;

/**
 * 颜色配置
 * 用于序列化 ColorRGBA
 */
public class ColorConfig {

    private float r;
    private float g;
    private float b;
    private float a;

    public ColorConfig() {
    }

    public ColorConfig(float r, float g, float b, float a) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
    }

    public ColorConfig(ColorRGBA color) {
        this.r = color.r;
        this.g = color.g;
        this.b = color.b;
        this.a = color.a;
    }

    public ColorRGBA toColorRGBA() {
        return new ColorRGBA(r, g, b, a);
    }

    // ========== Getters and Setters ==========

    public float getR() {
        return r;
    }

    public void setR(float r) {
        this.r = r;
    }

    public float getG() {
        return g;
    }

    public void setG(float g) {
        this.g = g;
    }

    public float getB() {
        return b;
    }

    public void setB(float b) {
        this.b = b;
    }

    public float getA() {
        return a;
    }

    public void setA(float a) {
        this.a = a;
    }
}
