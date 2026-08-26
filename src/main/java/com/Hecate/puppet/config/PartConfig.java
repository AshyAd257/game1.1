package com.Hecate.puppet.config;

import java.util.Map;

/**
 * 部件渲染器配置
 * 存储单个部件的尺寸、偏移、旋转等信息
 */
public class PartConfig {

    // 部件尺寸
    private float width;
    private float height;

    // 位置偏移
    private Vec3Config offset;

    // 自定义旋转
    private float customRotationX;  // X轴旋转（跷跷板）
    private float customRotationZ;  // Z轴旋转（平面内）

    // 中心点（旋转中心）
    private Vec3Config pivotPoint;

    // 纹理路径（向后兼容）
    private String texturePath;

    // 多方向贴图（新增）
    private Map<String, String> directionTextures;  // 方向key -> 贴图路径
    private String currentDirection;  // 当前方向

    // 调试颜色（如果没有纹理）
    private ColorConfig debugColor;

    // 渲染优先级
    private int priority;

    // UV纹理坐标（用于纹理图集切片）- 向后兼容
    private float uvOffsetX = 0.0f;  // UV起始X坐标（0.0-1.0）
    private float uvOffsetY = 0.0f;  // UV起始Y坐标（0.0-1.0）
    private float uvScaleX = 1.0f;   // UV缩放X（区域宽度，0.0-1.0）
    private float uvScaleY = 1.0f;   // UV缩放Y（区域高度，0.0-1.0）

    // 多方向UV坐标（新增 - 每个方向独立的UV设置）
    private Map<String, float[]> directionUVs;  // 方向key -> UV数组[offsetX, offsetY, scaleX, scaleY]

    // 多方向优先级（新增 - 每个方向独立的优先级设置）
    private Map<String, Integer> directionPriorities;  // 方向key -> 优先级值

    // 多方向尺寸（新增 - 每个方向独立的宽度和高度）
    private Map<String, Float> directionWidths;  // 方向key -> 宽度值
    private Map<String, Float> directionHeights;  // 方向key -> 高度值

    // 多方向位置偏移（新增 - 每个方向独立的位置偏移）
    private Map<String, float[]> directionOffsets;  // 方向key -> [offsetX, offsetY, offsetZ]

    // 多方向旋转（新增 - 每个方向独立的旋转角度）
    private Map<String, float[]> directionRotations;  // 方向key -> [rotationX, rotationZ]

    // 多方向贴图旋转（新增 - 每个方向独立的贴图旋转角度）
    private Map<String, Float> directionTextureRotations;  // 方向key -> 旋转角度（度）

    // Billboard控制 - 该部件是否启用billboard（始终面向摄像机）
    // true = 2D纸片模式，false = 3D固定朝向模式
    private boolean billboardEnabled = true;  // 默认启用，向后兼容

    // 旋转条状贴图系统（伪3D棱柱效果，与6方向系统互斥）
    private boolean rotationStripEnabled = false;
    private String stripTexturePath;
    private int stripSteps = 16;
    private int stripFrameWidthPx = 32;
    private int stripFrameHeightPx = 32;

    // Billboard俯仰角平滑过渡阈值（度）
    private float billboardPitchFullRangeDeg = 60f;
    private float billboardPitchLockDeg = 80f;

    // 旋转条状贴图专用变换数据（单一值，不按方向分槎）
    private float stripWidth = 1.0f;
    private float stripHeight = 1.0f;
    private Vec3Config stripOffset;
    private float stripRotationX;
    private float stripRotationY;
    private float stripRotationZ;
    private int stripPriority;
    private int stripCalibrationOffsetPx;

    // 3D模型骨骼系统（与Bone/EditorBone保持一致）
    private boolean modelEnabled = false;
    private String modelFilePath;
    private float modelRotationX;
    private float modelRotationY;
    private float modelRotationZ;
    private float modelScale = 1.0f;

    public PartConfig() {
    }

    // ========== Getters and Setters ==========

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

    public Vec3Config getOffset() {
        return offset;
    }

    public void setOffset(Vec3Config offset) {
        this.offset = offset;
    }

    public float getCustomRotationX() {
        return customRotationX;
    }

    public void setCustomRotationX(float customRotationX) {
        this.customRotationX = customRotationX;
    }

    public float getCustomRotationZ() {
        return customRotationZ;
    }

    public void setCustomRotationZ(float customRotationZ) {
        this.customRotationZ = customRotationZ;
    }

    public Vec3Config getPivotPoint() {
        return pivotPoint;
    }

    public void setPivotPoint(Vec3Config pivotPoint) {
        this.pivotPoint = pivotPoint;
    }

    public String getTexturePath() {
        return texturePath;
    }

    public void setTexturePath(String texturePath) {
        this.texturePath = texturePath;
    }

    public ColorConfig getDebugColor() {
        return debugColor;
    }

    public void setDebugColor(ColorConfig debugColor) {
        this.debugColor = debugColor;
    }

    public Map<String, String> getDirectionTextures() {
        return directionTextures;
    }

    public void setDirectionTextures(Map<String, String> directionTextures) {
        this.directionTextures = directionTextures;
    }

    public String getCurrentDirection() {
        return currentDirection;
    }

    public void setCurrentDirection(String currentDirection) {
        this.currentDirection = currentDirection;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public float getUvOffsetX() {
        return uvOffsetX;
    }

    public void setUvOffsetX(float uvOffsetX) {
        this.uvOffsetX = uvOffsetX;
    }

    public float getUvOffsetY() {
        return uvOffsetY;
    }

    public void setUvOffsetY(float uvOffsetY) {
        this.uvOffsetY = uvOffsetY;
    }

    public float getUvScaleX() {
        return uvScaleX;
    }

    public void setUvScaleX(float uvScaleX) {
        this.uvScaleX = uvScaleX;
    }

    public float getUvScaleY() {
        return uvScaleY;
    }

    public void setUvScaleY(float uvScaleY) {
        this.uvScaleY = uvScaleY;
    }

    public Map<String, float[]> getDirectionUVs() {
        return directionUVs;
    }

    public void setDirectionUVs(Map<String, float[]> directionUVs) {
        this.directionUVs = directionUVs;
    }

    public Map<String, Integer> getDirectionPriorities() {
        return directionPriorities;
    }

    public void setDirectionPriorities(Map<String, Integer> directionPriorities) {
        this.directionPriorities = directionPriorities;
    }

    public Map<String, Float> getDirectionWidths() {
        return directionWidths;
    }

    public void setDirectionWidths(Map<String, Float> directionWidths) {
        this.directionWidths = directionWidths;
    }

    public Map<String, Float> getDirectionHeights() {
        return directionHeights;
    }

    public void setDirectionHeights(Map<String, Float> directionHeights) {
        this.directionHeights = directionHeights;
    }

    public Map<String, float[]> getDirectionOffsets() {
        return directionOffsets;
    }

    public void setDirectionOffsets(Map<String, float[]> directionOffsets) {
        this.directionOffsets = directionOffsets;
    }

    public Map<String, float[]> getDirectionRotations() {
        return directionRotations;
    }

    public void setDirectionRotations(Map<String, float[]> directionRotations) {
        this.directionRotations = directionRotations;
    }

    public Map<String, Float> getDirectionTextureRotations() {
        return directionTextureRotations;
    }

    public void setDirectionTextureRotations(Map<String, Float> directionTextureRotations) {
        this.directionTextureRotations = directionTextureRotations;
    }

    public boolean isBillboardEnabled() {
        return billboardEnabled;
    }

    public void setBillboardEnabled(boolean billboardEnabled) {
        this.billboardEnabled = billboardEnabled;
    }

    public boolean isRotationStripEnabled() {
        return rotationStripEnabled;
    }

    public void setRotationStripEnabled(boolean rotationStripEnabled) {
        this.rotationStripEnabled = rotationStripEnabled;
    }

    public String getStripTexturePath() {
        return stripTexturePath;
    }

    public void setStripTexturePath(String stripTexturePath) {
        this.stripTexturePath = stripTexturePath;
    }

    public int getStripSteps() {
        return stripSteps;
    }

    public void setStripSteps(int stripSteps) {
        this.stripSteps = stripSteps;
    }

    public int getStripFrameWidthPx() {
        return stripFrameWidthPx;
    }

    public void setStripFrameWidthPx(int stripFrameWidthPx) {
        this.stripFrameWidthPx = stripFrameWidthPx;
    }

    public int getStripFrameHeightPx() {
        return stripFrameHeightPx;
    }

    public void setStripFrameHeightPx(int stripFrameHeightPx) {
        this.stripFrameHeightPx = stripFrameHeightPx;
    }

    public float getBillboardPitchFullRangeDeg() {
        return billboardPitchFullRangeDeg;
    }

    public void setBillboardPitchFullRangeDeg(float billboardPitchFullRangeDeg) {
        this.billboardPitchFullRangeDeg = billboardPitchFullRangeDeg;
    }

    public float getBillboardPitchLockDeg() {
        return billboardPitchLockDeg;
    }

    public void setBillboardPitchLockDeg(float billboardPitchLockDeg) {
        this.billboardPitchLockDeg = billboardPitchLockDeg;
    }

    public float getStripWidth() {
        return stripWidth;
    }

    public void setStripWidth(float stripWidth) {
        this.stripWidth = stripWidth;
    }

    public float getStripHeight() {
        return stripHeight;
    }

    public void setStripHeight(float stripHeight) {
        this.stripHeight = stripHeight;
    }

    public Vec3Config getStripOffset() {
        return stripOffset;
    }

    public void setStripOffset(Vec3Config stripOffset) {
        this.stripOffset = stripOffset;
    }

    public float getStripRotationX() {
        return stripRotationX;
    }

    public void setStripRotationX(float stripRotationX) {
        this.stripRotationX = stripRotationX;
    }

    public float getStripRotationY() {
        return stripRotationY;
    }

    public void setStripRotationY(float stripRotationY) {
        this.stripRotationY = stripRotationY;
    }

    public float getStripRotationZ() {
        return stripRotationZ;
    }

    public void setStripRotationZ(float stripRotationZ) {
        this.stripRotationZ = stripRotationZ;
    }

    public int getStripPriority() {
        return stripPriority;
    }

    public void setStripPriority(int stripPriority) {
        this.stripPriority = stripPriority;
    }

    public int getStripCalibrationOffsetPx() {
        return stripCalibrationOffsetPx;
    }

    public void setStripCalibrationOffsetPx(int stripCalibrationOffsetPx) {
        this.stripCalibrationOffsetPx = stripCalibrationOffsetPx;
    }

    public boolean isModelEnabled() {
        return modelEnabled;
    }

    public void setModelEnabled(boolean modelEnabled) {
        this.modelEnabled = modelEnabled;
    }

    public String getModelFilePath() {
        return modelFilePath;
    }

    public void setModelFilePath(String modelFilePath) {
        this.modelFilePath = modelFilePath;
    }

    public float getModelRotationX() {
        return modelRotationX;
    }

    public void setModelRotationX(float modelRotationX) {
        this.modelRotationX = modelRotationX;
    }

    public float getModelRotationY() {
        return modelRotationY;
    }

    public void setModelRotationY(float modelRotationY) {
        this.modelRotationY = modelRotationY;
    }

    public float getModelRotationZ() {
        return modelRotationZ;
    }

    public void setModelRotationZ(float modelRotationZ) {
        this.modelRotationZ = modelRotationZ;
    }

    public float getModelScale() {
        return modelScale;
    }

    public void setModelScale(float modelScale) {
        this.modelScale = modelScale;
    }
}
