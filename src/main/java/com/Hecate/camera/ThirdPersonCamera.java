package com.Hecate.camera;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;

/**
 * 第三人称摄像机控制器
 *
 * 职责：
 * - 管理摄像机的位置和朝向
 * - 提供干净的旋转/缩放接口
 * - 处理摄像机数学计算
 *
 * 不负责：
 * - 输入处理（由 CameraInputHandler 负责）
 * - 玩家移动逻辑（由 PlayerController 负责）
 *
 * 坐标系：
 * - +X: 东, +Z: 北, +Y: 上
 * - yaw = 0: 摄像机在目标正北
 * - yaw = π: 摄像机在目标正南（角色背后）
 * - pitch = 0: 水平, pitch > 0: 俯视, pitch < 0: 仰视
 */
public class ThirdPersonCamera {

    // 摄像机状态（全部使用弧度）
    private float yaw;        // 水平角度 [0, 2π)
    private float pitch;      // 垂直角度 [minPitch, maxPitch]
    private float distance;   // 摄像机距离目标的距离

    // 跟随目标
    private final Vector3f target = new Vector3f();

    // 配置
    private final CameraConfig config;

    // JME摄像机引用
    private final Camera jmeCamera;

    /**
     * 构造函数
     * @param jmeCamera JME3的摄像机对象
     * @param config 摄像机配置
     */
    public ThirdPersonCamera(Camera jmeCamera, CameraConfig config) {
        this.jmeCamera = jmeCamera;
        this.config = config;

        // 初始化为默认值
        this.yaw = config.defaultYaw;
        this.pitch = config.defaultPitch;
        this.distance = config.defaultDistance;
    }

    /**
     * 使用默认配置的构造函数
     */
    public ThirdPersonCamera(Camera jmeCamera) {
        this(jmeCamera, new CameraConfig());
    }

    /**
     * 设置跟随目标位置（通常是玩家脚底位置）
     */
    public void setTarget(Vector3f targetPosition) {
        this.target.set(targetPosition);
    }

    /**
     * 旋转摄像机
     * @param deltaYaw 水平旋转增量（弧度）
     * @param deltaPitch 垂直旋转增量（弧度）
     */
    public void rotate(float deltaYaw, float deltaPitch) {
        // 更新yaw并归一化到 [0, 2π)
        this.yaw = normalizeAngle(this.yaw + deltaYaw);

        // 更新pitch并限制范围
        this.pitch = FastMath.clamp(
            this.pitch + deltaPitch,
            config.minPitch,
            config.maxPitch
        );
    }

    /**
     * 缩放摄像机距离
     * @param delta 距离变化量（正数=远离，负数=靠近）
     */
    public void zoom(float delta) {
        this.distance = FastMath.clamp(
            this.distance + delta,
            config.minDistance,
            config.maxDistance
        );
    }

    /**
     * 重置摄像机到默认状态
     */
    public void reset() {
        this.yaw = config.defaultYaw;
        this.pitch = config.defaultPitch;
        this.distance = config.defaultDistance;
    }

    /**
     * 更新JME摄像机的位置和朝向
     * 每帧调用一次
     */
    public void update() {
        Vector3f cameraPosition = calculateCameraPosition();
        Vector3f lookAtTarget = calculateLookAtTarget();

        jmeCamera.setLocation(cameraPosition);

        Vector3f direction = lookAtTarget.subtract(cameraPosition).normalizeLocal();
        jmeCamera.lookAtDirection(direction, Vector3f.UNIT_Y);
    }

    /**
     * 计算摄像机世界坐标位置
     */
    private Vector3f calculateCameraPosition() {
        // 球坐标转笛卡尔坐标
        // x = r * sin(yaw) * cos(pitch)
        // y = r * sin(pitch)
        // z = r * cos(yaw) * cos(pitch)

        float horizontalDistance = distance * FastMath.cos(pitch);
        float verticalOffset = distance * FastMath.sin(pitch);

        float camX = target.x + horizontalDistance * FastMath.sin(yaw);
        float camY = target.y + config.lookAtHeightOffset + verticalOffset;
        float camZ = target.z + horizontalDistance * FastMath.cos(yaw);

        return new Vector3f(camX, camY, camZ);
    }

    /**
     * 计算摄像机应该看向的目标点
     * 偏移到角色右侧，让角色出现在屏幕左侧
     */
    private Vector3f calculateLookAtTarget() {
        // 计算摄像机的右方向向量
        Vector3f cameraRight = getCameraRightDirection();

        // 目标点 = 角色位置 + 向上偏移 + 向右偏移
        Vector3f lookAt = target.clone();
        lookAt.y += config.lookAtHeightOffset;
        lookAt.addLocal(cameraRight.mult(config.lookAtRightOffset));

        return lookAt;
    }

    /**
     * 获取摄像机的右方向向量（水平面投影）
     */
    private Vector3f getCameraRightDirection() {
        // 摄像机前方向（水平投影）
        Vector3f forward = new Vector3f(
            FastMath.sin(yaw),
            0,
            FastMath.cos(yaw)
        );

        // 右方向 = 前方向 × 上方向
        return forward.cross(Vector3f.UNIT_Y).normalizeLocal();
    }

    /**
     * 角度归一化到 [0, 2π)
     */
    private float normalizeAngle(float angle) {
        angle = angle % FastMath.TWO_PI;
        if (angle < 0) {
            angle += FastMath.TWO_PI;
        }
        return angle;
    }

    // ==================== Getters ====================

    /**
     * 获取当前yaw角度（弧度）
     */
    public float getYaw() {
        return yaw;
    }

    /**
     * 直接设置yaw角度（弧度）
     * 用于摄像机自动归位等需要精确控制的场景
     */
    public void setYaw(float yaw) {
        this.yaw = normalizeAngle(yaw);
    }

    /**
     * 获取当前pitch角度（弧度）
     */
    public float getPitch() {
        return pitch;
    }

    /**
     * 获取当前距离
     */
    public float getDistance() {
        return distance;
    }

    /**
     * 获取摄像机前方向（用于计算移动方向）
     * 水平投影，归一化
     */
    public Vector3f getForwardDirection() {
        return new Vector3f(
            FastMath.sin(yaw),
            0,
            FastMath.cos(yaw)
        ).normalizeLocal();
    }

    /**
     * 获取摄像机右方向（用于计算移动方向）
     * 水平投影，归一化
     */
    public Vector3f getRightDirection() {
        return getForwardDirection().cross(Vector3f.UNIT_Y).normalizeLocal();
    }

    /**
     * 获取配置对象
     */
    public CameraConfig getConfig() {
        return config;
    }

    /**
     * 获取当前位置（用于调试）
     */
    public Vector3f getPosition() {
        return jmeCamera.getLocation().clone();
    }
}
