package com.Hecate.puppet.animation;

import com.jme3.math.Vector3f;
import com.jme3.math.Quaternion;

/**
 * 关键帧
 * 存储某个时间点骨骼的变换数据
 */
public class Keyframe {

    /**
     * 关键帧类型
     */
    public enum KeyframeType {
        INTERPOLATED,  // 插值关键帧：会在两个关键帧之间平滑过渡
        SNAPSHOT       // 快照关键帧：立即切换到该状态，无中间过渡
    }

    private float time;              // 时间点（秒）
    private String boneName;         // 骨骼名称
    private KeyframeType type;       // 关键帧类型

    // 变换数据
    private Vector3f position;       // 位置
    private Quaternion rotation;     // 旋转
    private Vector3f scale;          // 缩放

    // 部件尺寸（用于PuppetPartRenderer）
    private float width;
    private float height;

    // 自定义旋转（用于PuppetPartRenderer）
    private float customRotationX;  // X轴旋转（度数）
    private float customRotationY;  // Y轴旋转（度数）
    private float customRotationZ;  // Z轴旋转（度数）

    // 纹理路径（用于快照关键帧）
    private String texturePath;     // 纹理贴图路径

    // 贴图旋转（度数，支持多圈旋转）
    private float textureRotation;  // 贴图旋转角度（已废弃，保留用于向后兼容）

    // 多方向贴图旋转（新功能）
    private java.util.Map<String, Float> directionTextureRotations;  // 每个方向的贴图旋转角度

    public Keyframe(float time, String boneName) {
        this(time, boneName, KeyframeType.INTERPOLATED);  // 默认为插值关键帧
    }

    public Keyframe(float time, String boneName, KeyframeType type) {
        this.time = time;
        this.boneName = boneName;
        this.type = type;
        this.position = new Vector3f();
        this.rotation = new Quaternion();
        this.scale = new Vector3f(1, 1, 1);
        this.width = 1.0f;
        this.height = 1.0f;
        this.customRotationX = 0f;
        this.customRotationY = 0f;
        this.customRotationZ = 0f;
        this.texturePath = null;
        this.textureRotation = 0f;
        this.directionTextureRotations = new java.util.HashMap<>();
    }

    /**
     * 完整构造函数
     */
    public Keyframe(float time, String boneName, Vector3f position, Quaternion rotation,
                    Vector3f scale, float width, float height) {
        this(time, boneName, KeyframeType.INTERPOLATED);
        this.position = position.clone();
        this.rotation = rotation.clone();
        this.scale = scale.clone();
        this.width = width;
        this.height = height;
    }

    /**
     * 复制构造函数
     */
    public Keyframe(Keyframe other) {
        this.time = other.time;
        this.boneName = other.boneName;
        this.type = other.type;
        this.position = other.position.clone();
        this.rotation = other.rotation.clone();
        this.scale = other.scale.clone();
        this.width = other.width;
        this.height = other.height;
        this.customRotationX = other.customRotationX;
        this.customRotationY = other.customRotationY;
        this.customRotationZ = other.customRotationZ;
        this.texturePath = other.texturePath;
        this.textureRotation = other.textureRotation;
        this.directionTextureRotations = new java.util.HashMap<>(other.directionTextureRotations);
    }

    /**
     * 线性插值到另一个关键帧
     * 注意：快照关键帧不参与插值
     */
    public Keyframe interpolate(Keyframe target, float t) {
        // t = 0 返回this，t = 1 返回target
        t = Math.max(0, Math.min(1, t)); // 限制在[0,1]

        Keyframe result = new Keyframe(
            time + (target.time - time) * t,
            boneName,
            this.type  // 保持原关键帧类型
        );

        // 位置插值
        result.position.set(
            position.x + (target.position.x - position.x) * t,
            position.y + (target.position.y - position.y) * t,
            position.z + (target.position.z - position.z) * t
        );

        // 旋转插值（球面线性插值）
        result.rotation.slerp(rotation, target.rotation, t);

        // 缩放插值
        result.scale.set(
            scale.x + (target.scale.x - scale.x) * t,
            scale.y + (target.scale.y - scale.y) * t,
            scale.z + (target.scale.z - scale.z) * t
        );

        // 尺寸插值
        result.width = width + (target.width - width) * t;
        result.height = height + (target.height - height) * t;

        // 自定义旋转插值
        result.customRotationX = customRotationX + (target.customRotationX - customRotationX) * t;
        result.customRotationY = customRotationY + (target.customRotationY - customRotationY) * t;
        result.customRotationZ = customRotationZ + (target.customRotationZ - customRotationZ) * t;

        // 贴图旋转插值（向后兼容）
        result.textureRotation = textureRotation + (target.textureRotation - textureRotation) * t;

        // 多方向贴图旋转插值
        result.directionTextureRotations = new java.util.HashMap<>();
        // 合并两个关键帧的所有方向
        java.util.Set<String> allDirections = new java.util.HashSet<>();
        allDirections.addAll(this.directionTextureRotations.keySet());
        allDirections.addAll(target.directionTextureRotations.keySet());

        for (String direction : allDirections) {
            float startRot = this.directionTextureRotations.getOrDefault(direction, 0f);
            float endRot = target.directionTextureRotations.getOrDefault(direction, 0f);
            result.directionTextureRotations.put(direction, startRot + (endRot - startRot) * t);
        }

        // 纹理路径：取起始关键帧的纹理（不插值）
        result.texturePath = this.texturePath;

        return result;
    }

    // ========== Getters and Setters ==========

    public float getTime() {
        return time;
    }

    public void setTime(float time) {
        this.time = time;
    }

    public String getBoneName() {
        return boneName;
    }

    public Vector3f getPosition() {
        return position;
    }

    public void setPosition(Vector3f position) {
        this.position.set(position);
    }

    public Quaternion getRotation() {
        return rotation;
    }

    public void setRotation(Quaternion rotation) {
        this.rotation.set(rotation);
    }

    public Vector3f getScale() {
        return scale;
    }

    public void setScale(Vector3f scale) {
        this.scale.set(scale);
    }

    public float getWidth() {
        return width;
    }

    public void setWidth(float width) {
        this.width = width;
    }

    public float getHeight() {
        return height;
    }

    public void setHeight(float height) {
        this.height = height;
    }

    public float getCustomRotationX() {
        return customRotationX;
    }

    public void setCustomRotationX(float customRotationX) {
        this.customRotationX = customRotationX;
    }

    public float getCustomRotationY() {
        return customRotationY;
    }

    public void setCustomRotationY(float customRotationY) {
        this.customRotationY = customRotationY;
    }

    public float getCustomRotationZ() {
        return customRotationZ;
    }

    public void setCustomRotationZ(float customRotationZ) {
        this.customRotationZ = customRotationZ;
    }

    public KeyframeType getType() {
        return type;
    }

    public void setType(KeyframeType type) {
        this.type = type;
    }

    public String getTexturePath() {
        return texturePath;
    }

    public void setTexturePath(String texturePath) {
        this.texturePath = texturePath;
    }

    public float getTextureRotation() {
        return textureRotation;
    }

    public void setTextureRotation(float textureRotation) {
        this.textureRotation = textureRotation;
    }

    /**
     * 获取多方向贴图旋转映射
     * @return 方向到旋转角度的映射
     */
    public java.util.Map<String, Float> getDirectionTextureRotations() {
        return directionTextureRotations;
    }

    /**
     * 设置多方向贴图旋转映射
     * @param directionTextureRotations 方向到旋转角度的映射
     */
    public void setDirectionTextureRotations(java.util.Map<String, Float> directionTextureRotations) {
        this.directionTextureRotations = directionTextureRotations != null ?
            new java.util.HashMap<>(directionTextureRotations) : new java.util.HashMap<>();
    }

    /**
     * 设置指定方向的贴图旋转
     * @param direction 方向 (front/back/left/right)
     * @param rotation 旋转角度（度）
     */
    public void setDirectionTextureRotation(String direction, float rotation) {
        this.directionTextureRotations.put(direction, rotation);
    }

    /**
     * 获取指定方向的贴图旋转
     * @param direction 方向 (front/back/left/right)
     * @return 旋转角度（度），如果不存在返回null
     */
    public Float getDirectionTextureRotation(String direction) {
        return this.directionTextureRotations.get(direction);
    }

    @Override
    public String toString() {
        return String.format("Keyframe[time=%.2f, bone=%s, type=%s, pos=(%.2f,%.2f,%.2f), size=%.2fx%.2f, tex=%s]",
                time, boneName, type, position.x, position.y, position.z, width, height,
                texturePath != null ? texturePath : "null");
    }
}
