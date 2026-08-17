package com.Hecate.puppet.config;

import com.jme3.math.Quaternion;

/**
 * 四元数配置
 * 用于序列化 Quaternion
 */
public class QuatConfig {

    private float x;
    private float y;
    private float z;
    private float w;

    public QuatConfig() {
    }

    public QuatConfig(float x, float y, float z, float w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }

    public QuatConfig(Quaternion quat) {
        this.x = quat.getX();
        this.y = quat.getY();
        this.z = quat.getZ();
        this.w = quat.getW();
    }

    public Quaternion toQuaternion() {
        return new Quaternion(x, y, z, w);
    }

    // ========== Getters and Setters ==========

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public float getZ() {
        return z;
    }

    public void setZ(float z) {
        this.z = z;
    }

    public float getW() {
        return w;
    }

    public void setW(float w) {
        this.w = w;
    }
}
