package com.Hecate.flame;

import com.jme3.app.SimpleApplication;
import com.jme3.collision.CollisionResults;
import com.jme3.input.InputManager;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.math.FastMath;
import com.jme3.math.Ray;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;
import com.Hecate.utils.LogUtils;
import com.Hecate.player.PlayerController;

/**
 * 火焰测试控制器
 * 按鼠标中键在鼠标位置喷射火焰
 */
public class FlameTestController implements ActionListener {

    private final SimpleApplication app;
    private final SimpleFlameRenderer flameRenderer;
    private final InputManager inputManager;
    private final Camera camera;
    private PlayerController playerController;
    private Node worldNode;

    // 火焰参数
    private static final float MAX_RANGE = 20.0f; // 最大射程
    private static final float GRAVITY = -15f; // 与FlameParticle中的重力保持一致

    // 发射模式
    private boolean continuousEmission = false;

    public FlameTestController(SimpleApplication app, SimpleFlameRenderer flameRenderer) {
        this.app = app;
        this.flameRenderer = flameRenderer;
        this.inputManager = app.getInputManager();
        this.camera = app.getCamera();

        setupInput();
    }

    /**
     * 设置玩家控制器引用
     */
    public void setPlayerController(PlayerController playerController) {
        this.playerController = playerController;
    }

    /**
     * 设置世界节点引用（用于射线检测）
     */
    public void setWorldNode(Node worldNode) {
        this.worldNode = worldNode;
    }

    /**
     * 设置输入映射
     */
    private void setupInput() {
        inputManager.addMapping("FlameShoot", new MouseButtonTrigger(MouseInput.BUTTON_MIDDLE));
        inputManager.addListener(this, "FlameShoot");
    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if ("FlameShoot".equals(name)) {
            if (isPressed) {
                // 获取角色手部3D位置（发射起点）
                Vector3f handPos3D = getHandPosition3D();

                // 获取准星指向的目标位置（用于判断发射方向）
                Vector3f targetPos = getTargetPosition();

                if (targetPos != null) {
                    // 根据玩家朝向计算实际发射速度
                    Vector3f velocity = calculateShootVelocity(handPos3D, targetPos);

                    // 发射火焰，带目标速度（3D世界空间）
                    // 增加粒子数量以提高涂墨覆盖率（从50增加到150）
                    flameRenderer.emitFlame(handPos3D, 150, velocity);
                } else {
                    // 没有目标，使用默认发射
                    flameRenderer.emitFlame(handPos3D, 150);
                }

            }
        }
    }

    /**
     * 获取手部3D位置
     * 基于摄像机视角计算，保证在不同精灵方向下位置一致
     */
    private Vector3f getHandPosition3D() {
        if (playerController == null) {
            return camera.getLocation().clone();
        }

        // 获取玩家位置（3D世界坐标）
        Vector3f playerPos = playerController.getPlayerPosition();

        // 获取摄像机方向（水平），而不是玩家朝向
        Vector3f cameraDir = camera.getDirection().clone();
        cameraDir.y = 0;
        cameraDir.normalizeLocal();

        // 获取摄像机右方向
        Vector3f cameraRight = camera.getLeft().negate();
        cameraRight.y = 0;
        cameraRight.normalizeLocal();

        // 计算手部3D位置
        // 基于摄像机视角：向前一点，向右一点
        Vector3f handPos3D = playerPos.clone();
        handPos3D.y += 0.5f; // 身体中部高度（2.75 + 0.5 = 3.25）
        handPos3D.addLocal(cameraDir.mult(0.3f)); // 向摄像机前方
        handPos3D.addLocal(cameraRight.mult(0.5f)); // 向摄像机右方

        return handPos3D;
    }

    /**
     * 使用射线检测获取准星指向的目标位置
     * 始终使用摄像机方向检测准星位置
     */
    private Vector3f getTargetPosition() {
        if (worldNode == null) {
            // 如果没有世界节点，返回摄像机前方的默认位置
            return camera.getLocation().add(camera.getDirection().mult(MAX_RANGE));
        }

        // 始终使用摄像机位置和方向做射线检测（找准星指向的位置）
        Ray ray = new Ray(camera.getLocation(), camera.getDirection());

        // 检测碰撞
        CollisionResults results = new CollisionResults();
        worldNode.collideWith(ray, results);

        if (results.size() > 0) {
            Vector3f hitPoint = results.getClosestCollision().getContactPoint();
            float distance = camera.getLocation().distance(hitPoint);

            // 检查是否在射程内
            if (distance <= MAX_RANGE) {
                return hitPoint;
            }
        }

        // 没有碰撞或超出射程，返回射线上的最大射程点
        Vector3f defaultTarget = camera.getLocation().add(camera.getDirection().mult(MAX_RANGE));
        return defaultTarget;
    }

    /**
     * 根据玩家模型朝向计算实际发射方向的速度
     * - back: 朝目标点发射（能打到准星位置）
     * - 其他: 朝玩家正面发射，使用准星检测到的距离
     */
    private Vector3f calculateShootVelocity(Vector3f handPos, Vector3f targetPos) {
        if (playerController == null) {
            // 没有玩家控制器，直接朝目标发射
            return calculateVelocity3D(handPos, targetPos);
        }

        String spriteDirection = playerController.getCurrentSpriteDirection();

        if ("back".equals(spriteDirection)) {
            // 背对镜头：朝准星目标点发
            return calculateVelocity3D(handPos, targetPos);
        } else {
            // 其他情况：朝玩家模型正面方向发射，使用准星距离

            // 计算准星目标的实际距离
            float targetDistance = handPos.distance(targetPos);

            // 获取摄像机方向（水平）
            Vector3f cameraDir = camera.getDirection().clone();
            cameraDir.y = 0;
            cameraDir.normalizeLocal();

            Vector3f shootDirection;
            if ("front".equals(spriteDirection)) {
                shootDirection = cameraDir.negate();

            } else if ("left".equals(spriteDirection)) {
                shootDirection = new Vector3f(cameraDir.z, 0, -cameraDir.x);

            } else if ("right".equals(spriteDirection)) {
                shootDirection = new Vector3f(-cameraDir.z, 0, cameraDir.x);

            } else {
                shootDirection = cameraDir.clone();

            }

            // 使用准星距离在玩家正面方向上创建虚拟目标点
            Vector3f virtualTarget = handPos.add(shootDirection.mult(targetDistance));

            // 确保虚拟目标点的Y坐标和准星目标点一致（保持同样的高度差）
            virtualTarget.y = targetPos.y;

            return calculateVelocity3D(handPos, virtualTarget);
        }
    }

    /**
     * 计算从起点到终点的抛物线初始速度（3D世界空间）
     *
     * 物理公式：
     * x = v_x * t
     * y = v_y * t + 0.5 * g * t^2
     * z = v_z * t
     *
     * @param start 起点（3D世界坐标）
     * @param target 终点（3D世界坐标）
     * @return 初始速度（3D世界空间）
     */
    private Vector3f calculateVelocity3D(Vector3f start, Vector3f target) {
        // 计算3D位移
        Vector3f displacement = target.subtract(start);

        // 计算水平距离（忽略Y轴）
        float horizontalDist = new Vector3f(displacement.x, 0, displacement.z).length();
        float verticalDist = displacement.y;

        // 根据距离估算飞行时间
        float flightTime = FastMath.sqrt(2 * horizontalDist / 10f); // 经验公式
        flightTime = FastMath.clamp(flightTime, 0.3f, 2.0f); // 限制在合理范围

        // 计算水平速度（XZ平面）
        float horizontalVel = horizontalDist / flightTime;

        // 计算垂直速度（考虑重力）
        // v_y = (y - 0.5 * g * t^2) / t
        float verticalVel = (verticalDist - 0.5f * GRAVITY * flightTime * flightTime) / flightTime;

        // 计算水平方向（归一化）
        Vector3f horizontalDir = new Vector3f(displacement.x, 0, displacement.z);
        if (horizontalDir.lengthSquared() > 0.001f) {
            horizontalDir.normalizeLocal();
        } else {
            // 目标正上方/正下方，使用默认向前方向
            horizontalDir = camera.getDirection().clone();
            horizontalDir.y = 0;
            horizontalDir.normalizeLocal();
        }

        // 返回3D速度
        return new Vector3f(
            horizontalDir.x * horizontalVel,
            verticalVel,
            horizontalDir.z * horizontalVel
        );
    }

    /**
     * 获取鼠标屏幕坐标（保留备用）
     */
    private Vector2f getMousePosition() {
        Vector2f cursorPos = inputManager.getCursorPosition();
        return new Vector2f(cursorPos.x, cursorPos.y);
    }

    /**
     * 设置连续发射模式（暂不支持）
     */
    public void setContinuousEmission(boolean enabled, Vector2f position) {
        this.continuousEmission = enabled;
        // TODO: SimpleFlameRenderer暂不支持连续发射
    }

    /**
     * 清理资源
     */
    public void cleanup() {
        inputManager.deleteMapping("FlameShoot");
        inputManager.removeListener(this);
    }
}
