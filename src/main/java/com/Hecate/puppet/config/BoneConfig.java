package com.Hecate.puppet.config;

import java.util.ArrayList;
import java.util.List;

/**
 * 骨骼配置
 * 存储单个骨骼的变换信息和子骨骼
 */
public class BoneConfig {

    private String name;
    private String parentName;  // 父骨骼名称（null 表示根骨骼）

    // Rest Pose（默认姿势）
    private Vec3Config restPosition;
    private QuatConfig restRotation;
    private Vec3Config restScale;

    // Current Pose（当前姿势）
    private Vec3Config currentPosition;
    private QuatConfig currentRotation;
    private Vec3Config currentScale;

    // 部件渲染器配置
    private PartConfig partConfig;

    // 自由骨骼系统配置
    private String boneType;  // "CONNECTED" 或 "FREE"
    private String gravityDirection;  // 重力方向预设名称
    private Vec3Config customGravityVector;  // 自定义重力向量
    private float freedomValue = 0.5f;  // 自由度（0-1）

    // FreeBonePhysics 物理参数（用于摇晃效果）
    private float mass = 1.0f;           // 质量
    private float damping = 0.95f;       // 阻尼系数（0-1）
    private float stiffness = 50.0f;     // 刚度系数
    private float gravityStrength = 9.8f; // 重力强度
    private float maxSwingAngle = 45.0f; // 最大摆动角度（度）
    private float maxVelocity = 10.0f;   // 最大速度限制

    // 贴图模式配置
    private boolean multiDirectionTextureEnabled = true;  // 是否启用多方向贴图模式（默认true）

    // Camera Follow 相机跟随自由度（Live2D风格效果）
    private float cameraFollowFreedomX = 0.0f;  // 水平方向相机跟随自由度
    private float cameraFollowFreedomY = 0.0f;  // 垂直方向相机跟随自由度

    // 骨骼分组ID（新增）
    private String groupId;  // 所属分组ID（null表示不属于任何分组）

    public BoneConfig() {
    }

    public BoneConfig(String name) {
        this.name = name;
    }

    // ========== Getters and Setters ==========

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getParentName() {
        return parentName;
    }

    public void setParentName(String parentName) {
        this.parentName = parentName;
    }

    public Vec3Config getRestPosition() {
        return restPosition;
    }

    public void setRestPosition(Vec3Config restPosition) {
        this.restPosition = restPosition;
    }

    public QuatConfig getRestRotation() {
        return restRotation;
    }

    public void setRestRotation(QuatConfig restRotation) {
        this.restRotation = restRotation;
    }

    public Vec3Config getRestScale() {
        return restScale;
    }

    public void setRestScale(Vec3Config restScale) {
        this.restScale = restScale;
    }

    public Vec3Config getCurrentPosition() {
        return currentPosition;
    }

    public void setCurrentPosition(Vec3Config currentPosition) {
        this.currentPosition = currentPosition;
    }

    public QuatConfig getCurrentRotation() {
        return currentRotation;
    }

    public void setCurrentRotation(QuatConfig currentRotation) {
        this.currentRotation = currentRotation;
    }

    public Vec3Config getCurrentScale() {
        return currentScale;
    }

    public void setCurrentScale(Vec3Config currentScale) {
        this.currentScale = currentScale;
    }

    public PartConfig getPartConfig() {
        return partConfig;
    }

    public void setPartConfig(PartConfig partConfig) {
        this.partConfig = partConfig;
    }

    // ========== 自由骨骼系统 Getters and Setters ==========

    public String getBoneType() {
        return boneType;
    }

    public void setBoneType(String boneType) {
        this.boneType = boneType;
    }

    public String getGravityDirection() {
        return gravityDirection;
    }

    public void setGravityDirection(String gravityDirection) {
        this.gravityDirection = gravityDirection;
    }

    public Vec3Config getCustomGravityVector() {
        return customGravityVector;
    }

    public void setCustomGravityVector(Vec3Config customGravityVector) {
        this.customGravityVector = customGravityVector;
    }

    public float getFreedomValue() {
        return freedomValue;
    }

    public void setFreedomValue(float freedomValue) {
        this.freedomValue = freedomValue;
    }

    // ========== FreeBonePhysics 物理参数 Getters and Setters ==========

    public float getMass() {
        return mass;
    }

    public void setMass(float mass) {
        this.mass = mass;
    }

    public float getDamping() {
        return damping;
    }

    public void setDamping(float damping) {
        this.damping = damping;
    }

    public float getStiffness() {
        return stiffness;
    }

    public void setStiffness(float stiffness) {
        this.stiffness = stiffness;
    }

    public float getGravityStrength() {
        return gravityStrength;
    }

    public void setGravityStrength(float gravityStrength) {
        this.gravityStrength = gravityStrength;
    }

    public float getMaxSwingAngle() {
        return maxSwingAngle;
    }

    public void setMaxSwingAngle(float maxSwingAngle) {
        this.maxSwingAngle = maxSwingAngle;
    }

    public float getMaxVelocity() {
        return maxVelocity;
    }

    public void setMaxVelocity(float maxVelocity) {
        this.maxVelocity = maxVelocity;
    }

    // ========== 贴图模式 Getters and Setters ==========

    public boolean isMultiDirectionTextureEnabled() {
        return multiDirectionTextureEnabled;
    }

    public void setMultiDirectionTextureEnabled(boolean multiDirectionTextureEnabled) {
        this.multiDirectionTextureEnabled = multiDirectionTextureEnabled;
    }

    // ========== Camera Follow 相机跟随自由度 Getters and Setters ==========

    public float getCameraFollowFreedomX() {
        return cameraFollowFreedomX;
    }

    public void setCameraFollowFreedomX(float cameraFollowFreedomX) {
        this.cameraFollowFreedomX = cameraFollowFreedomX;
    }

    public float getCameraFollowFreedomY() {
        return cameraFollowFreedomY;
    }

    public void setCameraFollowFreedomY(float cameraFollowFreedomY) {
        this.cameraFollowFreedomY = cameraFollowFreedomY;
    }

    // ========== 骨骼分组 Getters and Setters ==========

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }
}
