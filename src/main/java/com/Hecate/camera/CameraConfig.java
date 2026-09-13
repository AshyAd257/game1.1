package com.Hecate.camera;

import com.jme3.math.FastMath;

/**
 * 第三人称摄像机配置
 * 集中管理所有摄像机参数，避免魔法数字散落各处
 */
public class CameraConfig {

    // 距离配置
    public final float minDistance;
    public final float maxDistance;
    public final float defaultDistance;

    // 角度限制（弧度）
    public final float minPitch;
    public final float maxPitch;
    public final float defaultYaw;
    public final float defaultPitch;

    // 输入灵敏度
    public final float mouseSensitivity;
    public final float zoomSpeed;

    // 视角偏移（让角色出现在屏幕左侧，准星在屏幕中央）
    public final float lookAtHeightOffset;  // 向上偏移到头部位置
    public final float lookAtRightOffset;   // 向右偏移

    /**
     * 默认配置构造器
     */
    public CameraConfig() {
        this.minDistance = 2.0f;
        this.maxDistance = 15.0f;
        this.defaultDistance = 8.0f;

        this.minPitch = -80f * FastMath.DEG_TO_RAD;
        this.maxPitch = 60f * FastMath.DEG_TO_RAD;
        this.defaultYaw = FastMath.PI;  // 默认在角色背后（南方）
        this.defaultPitch = -20f * FastMath.DEG_TO_RAD;

        this.mouseSensitivity = 5.0f;  // 匹配原来的灵敏度
        this.zoomSpeed = 3.0f;

        this.lookAtHeightOffset = 1.7f;  // 头部高度
        this.lookAtRightOffset = 0.525f;
    }

    /**
     * 自定义配置构造器
     */
    public CameraConfig(float minDistance, float maxDistance, float defaultDistance,
                       float minPitch, float maxPitch, float defaultYaw, float defaultPitch,
                       float mouseSensitivity, float zoomSpeed,
                       float lookAtHeightOffset, float lookAtRightOffset) {
        this.minDistance = minDistance;
        this.maxDistance = maxDistance;
        this.defaultDistance = defaultDistance;
        this.minPitch = minPitch;
        this.maxPitch = maxPitch;
        this.defaultYaw = defaultYaw;
        this.defaultPitch = defaultPitch;
        this.mouseSensitivity = mouseSensitivity;
        this.zoomSpeed = zoomSpeed;
        this.lookAtHeightOffset = lookAtHeightOffset;
        this.lookAtRightOffset = lookAtRightOffset;
    }
}
