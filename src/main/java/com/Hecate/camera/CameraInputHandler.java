package com.Hecate.camera;

/**
 * 摄像机输入处理器
 *
 * 职责：
 * - 将原始输入值转换为摄像机旋转/缩放指令
 * - 应用灵敏度和速度配置
 *
 * 不负责：
 * - 实际的摄像机数学计算（由 ThirdPersonCamera 负责）
 * - 输入事件监听（由 PlayerController 负责）
 */
public class CameraInputHandler {

    private final ThirdPersonCamera camera;
    private final CameraConfig config;

    public CameraInputHandler(ThirdPersonCamera camera) {
        this.camera = camera;
        this.config = camera.getConfig();
    }

    /**
     * 处理鼠标横向移动（左右旋转）
     * @param mouseDelta 鼠标移动增量（InputManager提供的原始值）
     */
    public void handleMouseHorizontal(float mouseDelta) {
        // 转换为yaw旋转增量（弧度）
        // 匹配原来的灵敏度：mouseDelta * MOUSE_SENSITIVITY * 50f * DEG_TO_RAD
        float deltaYaw = mouseDelta * config.mouseSensitivity * 50f * 0.017453292f;
        camera.rotate(deltaYaw, 0);
    }

    /**
     * 处理鼠标纵向移动（上下旋转）
     * @param mouseDelta 鼠标移动增量
     */
    public void handleMouseVertical(float mouseDelta) {
        // 转换为pitch旋转增量（弧度）
        // 垂直灵敏度使用 30f 倍数（原系统中垂直和水平不同）
        float deltaPitch = mouseDelta * config.mouseSensitivity * 30f * 0.017453292f;
        camera.rotate(0, deltaPitch);
    }

    /**
     * 处理缩放输入（按住+键）
     * @param tpf 帧时间（秒）
     */
    public void handleZoomIn(float tpf) {
        float delta = -config.zoomSpeed * tpf;
        camera.zoom(delta);
    }

    /**
     * 处理缩放输入（按住-键）
     * @param tpf 帧时间（秒）
     */
    public void handleZoomOut(float tpf) {
        float delta = config.zoomSpeed * tpf;
        camera.zoom(delta);
    }

    /**
     * 处理重置指令（R键）
     */
    public void handleReset() {
        camera.reset();
    }
}
