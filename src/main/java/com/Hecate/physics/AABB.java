package com.Hecate.physics;

import com.jme3.math.Vector3f;

/**
 * 轴对齐包围盒 (AABB - Axis Aligned Bounding Box)
 * 使用 jMonkeyEngine 的 Vector3f
 */
public class AABB {
    private Vector3f min;
    private Vector3f max;

    public AABB(Vector3f min, Vector3f max) {
        this.min = min.clone();
        this.max = max.clone();
    }

    public AABB(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        this.min = new Vector3f(minX, minY, minZ);
        this.max = new Vector3f(maxX, maxY, maxZ);
    }

    /**
     * 🆕 从中心点和尺寸创建AABB
     */
    public AABB(Vector3f center, float width, float height, float depth) {
        float halfWidth = width / 2f;
        float halfHeight = height / 2f;
        float halfDepth = depth / 2f;

        this.min = new Vector3f(
                center.x - halfWidth,
                center.y - halfHeight,
                center.z - halfDepth
        );
        this.max = new Vector3f(
                center.x + halfWidth,
                center.y + halfHeight,
                center.z + halfDepth
        );
    }

    /**
     * 从底部位置（脚底）和尺寸创建AABB - 用于角色碰撞盒
     * @param bottomCenter 底部中心位置（角色脚底位置）
     * @param width 宽度
     * @param height 高度
     * @param depth 深度
     */
    public static AABB fromBottom(Vector3f bottomCenter, float width, float height, float depth) {
        float halfWidth = width / 2f;
        float halfDepth = depth / 2f;

        return new AABB(
                bottomCenter.x - halfWidth,
                bottomCenter.y,              // 底部从脚底开始
                bottomCenter.z - halfDepth,
                bottomCenter.x + halfWidth,
                bottomCenter.y + height,     // 顶部是底部+高度
                bottomCenter.z + halfDepth
        );
    }

    /**
     *  设置最小点 - PlayerController需要的方法
     */
    public void setMinPoint(Vector3f minPoint) {
        this.min = minPoint.clone();
    }

    /**
     *  设置最大点 - PlayerController需要的方法
     */
    public void setMaxPoint(Vector3f maxPoint) {
        this.max = maxPoint.clone();
    }

    /**
     *  获取最小点
     */
    public Vector3f getMinPoint() {
        return min.clone();
    }

    /**
     * 获取最大点
     */
    public Vector3f getMaxPoint() {
        return max.clone();
    }

    /**
     * 🔄 更新位置 - 保持尺寸不变，只改变位置（按中心点）
     */
    public void updatePosition(Vector3f newCenter) {
        Vector3f size = getSize();
        float halfWidth = size.x / 2f;
        float halfHeight = size.y / 2f;
        float halfDepth = size.z / 2f;

        this.min.set(
                newCenter.x - halfWidth,
                newCenter.y - halfHeight,
                newCenter.z - halfDepth
        );
        this.max.set(
                newCenter.x + halfWidth,
                newCenter.y + halfHeight,
                newCenter.z + halfDepth
        );
    }

    /**
     * 🔄 从底部位置更新 - 用于角色移动
     */
    public void updateFromBottom(Vector3f newBottomCenter) {
        Vector3f size = getSize();
        float halfWidth = size.x / 2f;
        float halfDepth = size.z / 2f;

        this.min.set(
                newBottomCenter.x - halfWidth,
                newBottomCenter.y,              // 底部从脚底开始
                newBottomCenter.z - halfDepth
        );
        this.max.set(
                newBottomCenter.x + halfWidth,
                newBottomCenter.y + size.y,     // 顶部是底部+高度
                newBottomCenter.z + halfDepth
        );
    }

    /**
     * 设置包围盒的边界
     */
    public void setBounds(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        this.min.set(minX, minY, minZ);
        this.max.set(maxX, maxY, maxZ);
    }

    /**
     * 检测两个AABB是否相交
     */
    public boolean intersects(AABB other) {
        return (this.min.x < other.max.x && this.max.x > other.min.x) &&
                (this.min.y < other.max.y && this.max.y > other.min.y) &&
                (this.min.z < other.max.z && this.max.z > other.min.z);
    }

    /**
     * 移动碰撞箱 - 返回新的AABB
     */
    public AABB translate(Vector3f offset) {
        return new AABB(
                min.x + offset.x, min.y + offset.y, min.z + offset.z,
                max.x + offset.x, max.y + offset.y, max.z + offset.z
        );
    }

    /**
     * 移动碰撞箱
     */
    public AABB offset(Vector3f offset) {
        return new AABB(
                min.x + offset.x, min.y + offset.y, min.z + offset.z,
                max.x + offset.x, max.y + offset.y, max.z + offset.z
        );
    }

    /**
     * 移动碰撞箱
     */
    public AABB offset(float x, float y, float z) {
        return new AABB(
                min.x + x, min.y + y, min.z + z,
                max.x + x, max.y + y, max.z + z
        );
    }

    /**
     * 扩展碰撞箱
     */
    public AABB expand(float x, float y, float z) {
        return new AABB(
                min.x - x, min.y - y, min.z - z,
                max.x + x, max.y + y, max.z + z
        );
    }

    /**
     * 获取碰撞箱的中心点
     */
    public Vector3f getCenter() {
        return new Vector3f(
                (min.x + max.x) / 2,
                (min.y + max.y) / 2,
                (min.z + max.z) / 2
        );
    }

    /**
     * 获取碰撞箱的尺寸
     */
    public Vector3f getSize() {
        return new Vector3f(
                max.x - min.x,
                max.y - min.y,
                max.z - min.z
        );
    }

    /**
     * 📋 复制AABB
     */
    public AABB clone() {
        return new AABB(min, max);
    }

    // Getters - 保持兼容性
    public Vector3f getMin() { return min.clone(); }
    public Vector3f getMax() { return max.clone(); }
    public float getMinX() { return min.x; }
    public float getMinY() { return min.y; }
    public float getMinZ() { return min.z; }
    public float getMaxX() { return max.x; }
    public float getMaxY() { return max.y; }
    public float getMaxZ() { return max.z; }

    @Override
    public String toString() {
        return "AABB{min=" + min + ", max=" + max + "}";
    }
}
