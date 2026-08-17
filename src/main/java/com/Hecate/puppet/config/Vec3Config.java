package com.Hecate.puppet.config;

import com.jme3.math.Vector3f;

/**
 * 3D向量配置
 * 用于序列化 Vector3f
 */
public class Vec3Config {

    private float x;
    private float y;
    private float z;

    public Vec3Config() {
    }

    public Vec3Config(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vec3Config(Vector3f vec) {
        this.x = vec.x;
        this.y = vec.y;
        this.z = vec.z;
    }

    public Vector3f toVector3f() {
        return new Vector3f(x, y, z);
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
}
