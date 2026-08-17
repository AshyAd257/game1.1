package com.Hecate.player;

import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;

/**
 * 摄像机方向检测器
 * 检测玩家相对于摄像机的朝向，用于精灵翻转
 *
 * <p><b>依赖注入支持</b>：推荐作为 PlayerController 的实例字段使用。
 *
 * <h3>推荐用法（实例字段）</h3>
 * <pre>{@code
 * // 在 PlayerController 中创建实例字段
 * private CameraDirectionDetector directionDetector = new CameraDirectionDetector();
 * directionDetector.initialize(camera);
 * }</pre>
 *
 * <h3>向后兼容用法（已废弃）</h3>
 * <pre>{@code
 * // 旧代码仍可正常工作
 * CameraDirectionDetector.getInstance().update(playerPos);
 * }</pre>
 */
public class CameraDirectionDetector {

    private static CameraDirectionDetector defaultInstance;

    private Camera camera;
    private Vector3f lastPlayerPosition = new Vector3f();
    private Vector3f lastCameraPosition = new Vector3f();

    // 方向检测参数
    private float directionThreshold = 0.1f;
    private boolean facingRight = true;

    // 调试信息
    private boolean debugMode = false;

    /**
     * 构造函数 - 创建新的方向检测器实例
     * <p>推荐作为 PlayerController 或 AnimationFrame 的实例字段
     */
    public CameraDirectionDetector() {
        // 公开构造函数，支持实例化
    }

    /**
     * 获取默认实例（向后兼容）
     *
     * @return 全局共享的方向检测器实例
     * @deprecated 推荐作为实例字段使用：{@code new CameraDirectionDetector()}
     */
    @Deprecated
    public static CameraDirectionDetector getInstance() {
        if (defaultInstance == null) {
            defaultInstance = new CameraDirectionDetector();
        }
        return defaultInstance;
    }

    /**
     * 获取默认实例
     *
     * @return 默认方向检测器实例
     */
    public static CameraDirectionDetector getDefaultInstance() {
        return getInstance();
    }

    /**
     * 创建新的独立实例
     *
     * @return 新的方向检测器实例
     */
    public static CameraDirectionDetector createInstance() {
        return new CameraDirectionDetector();
    }

    /**
     * 初始化摄像机引用
     */
    public void initialize(Camera camera) {
        this.camera = camera;
        this.lastCameraPosition.set(camera.getLocation());
    }

    /**
     * 更新方向检测
     */
    public void update(Vector3f playerPosition) {
        if (camera == null) {
            return;
        }

        Vector3f currentCameraPos = camera.getLocation();

        // 计算玩家相对于摄像机的方向
        Vector3f playerToCamera = currentCameraPos.subtract(playerPosition);
        Vector3f lastPlayerToCamera = lastCameraPosition.subtract(lastPlayerPosition);

        // 检测水平方向变化
        float currentDirection = playerToCamera.x;
        float lastDirection = lastPlayerToCamera.x;

        // 如果方向变化超过阈值，更新朝向
        if (Math.abs(currentDirection - lastDirection) > directionThreshold) {
            boolean newFacingRight = currentDirection > 0;

            if (newFacingRight != facingRight) {
                facingRight = newFacingRight;
            }
        }

        // 更新位置记录
        lastPlayerPosition.set(playerPosition);
        lastCameraPosition.set(currentCameraPos);
    }

    /**
     * 基于移动方向检测朝向
     */
    public void updateByMovement(Vector3f velocity) {
        if (Math.abs(velocity.x) > directionThreshold) {
            boolean newFacingRight = velocity.x > 0;

            if (newFacingRight != facingRight) {
                facingRight = newFacingRight;
            }
        }
    }

    /**
     * 手动设置朝向
     */
    public void setFacingDirection(boolean facingRight) {
        if (this.facingRight != facingRight) {
            this.facingRight = facingRight;
        }
    }

    /**
     * 是否朝右
     */
    public boolean isFacingRight() {
        return facingRight;
    }

    /**
     * 是否朝左
     */
    public boolean isFacingLeft() {
        return !facingRight;
    }

    /**
     * 设置方向检测阈值
     */
    public void setDirectionThreshold(float threshold) {
        this.directionThreshold = threshold;
    }

    /**
     * 设置调试模式
     */
    public void setDebugMode(boolean debug) {
        this.debugMode = debug;
    }

    /**
     * 获取方向信息
     */
    public String getDirectionInfo() {
        return String.format("朝向: %s, 阈值: %.2f",
                facingRight ? "右" : "左", directionThreshold);
    }

    /**
     * 重置检测器
     */
    public void reset() {
        facingRight = true;
        lastPlayerPosition.set(Vector3f.ZERO);
        lastCameraPosition.set(Vector3f.ZERO);
    }
}
