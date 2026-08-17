package com.Hecate.puppet.editor.core;

/**
 * 方向配置类 - 将单个方向的所有属性合并到一个对象中
 * 优化：减少多次HashMap查找，一次查找获取所有属性
 */
public class DirectionConfig {

    // 贴图相关
    public String texture;
    public float[] uv;  // [offsetX, offsetY, scaleX, scaleY]
    public Float textureRotation;  // 贴图旋转角度（度）

    // 尺寸和位置
    public Float width;
    public Float height;
    public float[] offset;  // [offsetX, offsetY, offsetZ]
    public float[] rotation;  // [rotationX, rotationY, rotationZ]
    public float[] contentCenter;  // [centerX, centerY]

    // 渲染优先级
    public Integer priority;

    // 物理和动画
    public Float freedomValue;
    public Float damping;  // 注意：方法名是 getCurrentDirectionDamping()
    public Boolean swingEnabled;
    public Float swingAmplitude;
    public float[] swingAxis;  // [x, y, z] - 注意：存储为 float[] 而非 String

    /**
     * 默认构造函数 - 所有字段初始化为null
     */
    public DirectionConfig() {
    }

    /**
     * 拷贝构造函数 - 深拷贝另一个配置
     */
    public DirectionConfig(DirectionConfig source) {
        if (source == null) {
            return;
        }

        this.texture = source.texture;
        this.uv = source.uv != null ? source.uv.clone() : null;
        this.textureRotation = source.textureRotation;

        this.width = source.width;
        this.height = source.height;
        this.offset = source.offset != null ? source.offset.clone() : null;
        this.rotation = source.rotation != null ? source.rotation.clone() : null;
        this.contentCenter = source.contentCenter != null ? source.contentCenter.clone() : null;

        this.priority = source.priority;

        this.freedomValue = source.freedomValue;
        this.damping = source.damping;
        this.swingEnabled = source.swingEnabled;
        this.swingAmplitude = source.swingAmplitude;
        this.swingAxis = source.swingAxis != null ? source.swingAxis.clone() : null;
    }

    /**
     * 检查是否为空配置（所有属性都是null）
     */
    public boolean isEmpty() {
        return texture == null
            && uv == null
            && textureRotation == null
            && width == null
            && height == null
            && offset == null
            && rotation == null
            && contentCenter == null
            && priority == null
            && freedomValue == null
            && damping == null
            && swingEnabled == null
            && swingAmplitude == null
            && swingAxis == null;
    }

    /**
     * 从另一个配置继承缺失的属性
     * @param source 源配置
     */
    public void inheritFrom(DirectionConfig source) {
        if (source == null) {
            return;
        }

        if (this.texture == null && source.texture != null) {
            this.texture = source.texture;
        }
        if (this.uv == null && source.uv != null) {
            this.uv = source.uv.clone();
        }
        if (this.textureRotation == null && source.textureRotation != null) {
            this.textureRotation = source.textureRotation;
        }

        if (this.width == null && source.width != null) {
            this.width = source.width;
        }
        if (this.height == null && source.height != null) {
            this.height = source.height;
        }
        if (this.offset == null && source.offset != null) {
            this.offset = source.offset.clone();
        }
        if (this.rotation == null && source.rotation != null) {
            this.rotation = source.rotation.clone();
        }
        if (this.contentCenter == null && source.contentCenter != null) {
            this.contentCenter = source.contentCenter.clone();
        }

        if (this.priority == null && source.priority != null) {
            this.priority = source.priority;
        }

        if (this.freedomValue == null && source.freedomValue != null) {
            this.freedomValue = source.freedomValue;
        }
        if (this.damping == null && source.damping != null) {
            this.damping = source.damping;
        }
        if (this.swingEnabled == null && source.swingEnabled != null) {
            this.swingEnabled = source.swingEnabled;
        }
        if (this.swingAmplitude == null && source.swingAmplitude != null) {
            this.swingAmplitude = source.swingAmplitude;
        }
        if (this.swingAxis == null && source.swingAxis != null) {
            this.swingAxis = source.swingAxis.clone();
        }
    }
}
