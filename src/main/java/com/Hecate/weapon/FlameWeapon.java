package com.Hecate.weapon;

import com.Hecate.flame.SimpleFlameRenderer;
import com.Hecate.player.PlayerController;
import com.Hecate.utils.LogUtils;
import com.jme3.collision.CollisionResults;
import com.jme3.math.FastMath;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;

/**
 * 火焰武器
 * 整合 FlameTestController 的弹道计算和火焰粒子系统
 */
public class FlameWeapon extends Weapon {

    private final SimpleFlameRenderer flameRenderer;
    private final Camera camera;
    private PlayerController playerController;
    private Node worldNode;

    // 火焰参数
    private static final float MAX_RANGE = 20.0f;
    private static final float GRAVITY = -15f;

    public FlameWeapon(WeaponStats stats, SimpleFlameRenderer flameRenderer, Camera camera) {
        super(stats);
        this.flameRenderer = flameRenderer;
        this.camera = camera;
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

    @Override
    protected void fire(Vector3f origin, Vector3f direction) {
        // 获取手部位置
        Vector3f handPos3D = getHandPosition3D(origin);

        // 获取准星目标位置
        Vector3f targetPos = getTargetPosition();

        if (targetPos != null) {
            // 计算发射速度
            Vector3f velocity = calculateShootVelocity(handPos3D, targetPos);

            // 发射火焰粒子
            flameRenderer.emitFlame(handPos3D, 150, velocity);
        } else {
            // 没有目标，使用默认发射
            flameRenderer.emitFlame(handPos3D, 150);
        }

    }

    @Override
    protected void fireCharged(Vector3f origin, Vector3f direction, float damageMultiplier) {
        // 蓄力攻击：发射更多粒子
        Vector3f handPos3D = getHandPosition3D(origin);
        Vector3f targetPos = getTargetPosition();

        if (targetPos != null) {
            Vector3f velocity = calculateShootVelocity(handPos3D, targetPos);
            int particleCount = (int)(150 * damageMultiplier);
            flameRenderer.emitFlame(handPos3D, particleCount, velocity);

        }
    }

    /**
     * 获取手部3D位置
     */
    private Vector3f getHandPosition3D(Vector3f origin) {
        if (playerController == null) {
            return origin.clone();
        }

        Vector3f playerPos = playerController.getPlayerPosition();

        // 获取摄像机方向（水平）
        Vector3f cameraDir = camera.getDirection().clone();
        cameraDir.y = 0;
        cameraDir.normalizeLocal();

        // 获取摄像机右方向
        Vector3f cameraRight = camera.getLeft().negate();
        cameraRight.y = 0;
        cameraRight.normalizeLocal();

        // 计算手部位置
        Vector3f handPos3D = playerPos.clone();
        handPos3D.y += 0.5f;
        handPos3D.addLocal(cameraDir.mult(0.3f));
        handPos3D.addLocal(cameraRight.mult(0.5f));

        return handPos3D;
    }

    /**
     * 使用射线检测获取准星指向的目标位置
     */
    private Vector3f getTargetPosition() {
        if (worldNode == null) {
            return camera.getLocation().add(camera.getDirection().mult(MAX_RANGE));
        }

        Ray ray = new Ray(camera.getLocation(), camera.getDirection());
        CollisionResults results = new CollisionResults();
        worldNode.collideWith(ray, results);

        if (results.size() > 0) {
            Vector3f hitPoint = results.getClosestCollision().getContactPoint();
            float distance = camera.getLocation().distance(hitPoint);

            if (distance <= MAX_RANGE) {
                return hitPoint;
            }
        }

        return camera.getLocation().add(camera.getDirection().mult(MAX_RANGE));
    }

    /**
     * 根据玩家朝向计算发射速度
     */
    private Vector3f calculateShootVelocity(Vector3f handPos, Vector3f targetPos) {
        if (playerController == null) {
            return calculateVelocity3D(handPos, targetPos);
        }

        String spriteDirection = playerController.getCurrentSpriteDirection();

        if ("back".equals(spriteDirection)) {
            // 背对镜头：朝准星目标发射
            return calculateVelocity3D(handPos, targetPos);
        } else {
            // 其他情况：朝玩家正面方向发射
            float targetDistance = handPos.distance(targetPos);

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

            Vector3f virtualTarget = handPos.add(shootDirection.mult(targetDistance));
            virtualTarget.y = targetPos.y;

            return calculateVelocity3D(handPos, virtualTarget);
        }
    }

    /**
     * 计算抛物线初始速度
     */
    private Vector3f calculateVelocity3D(Vector3f start, Vector3f target) {
        Vector3f displacement = target.subtract(start);

        float horizontalDist = new Vector3f(displacement.x, 0, displacement.z).length();
        float verticalDist = displacement.y;

        float flightTime = FastMath.sqrt(2 * horizontalDist / 10f);
        flightTime = FastMath.clamp(flightTime, 0.3f, 2.0f);

        float horizontalVel = horizontalDist / flightTime;
        float verticalVel = (verticalDist - 0.5f * GRAVITY * flightTime * flightTime) / flightTime;

        Vector3f horizontalDir = new Vector3f(displacement.x, 0, displacement.z);
        if (horizontalDir.lengthSquared() > 0.001f) {
            horizontalDir.normalizeLocal();
        } else {
            horizontalDir = camera.getDirection().clone();
            horizontalDir.y = 0;
            horizontalDir.normalizeLocal();
        }

        return new Vector3f(
            horizontalDir.x * horizontalVel,
            verticalVel,
            horizontalDir.z * horizontalVel
        );
    }

    /**
     * 创建默认火焰武器
     */
    public static FlameWeapon createDefault(SimpleFlameRenderer flameRenderer, Camera camera) {
        WeaponStats stats = new WeaponStats.Builder("flame_weapon", "火焰喷射器")
                .fireRate(0.3f)              // 0.3秒攻击间隔（比BasicShooter快）
                .projectileVelocity(15.0f)   // 15米/秒
                .spreadAngle(0f)             // 无散射（粒子系统自带散射）
                .maxRange(20.0f)             // 20米射程
                .ammoCost(100.0f)            // 消耗100弹药（1000弹药可发射10次）
                .baseDamage(5.0f)            // 5点伤害（多粒子累积）
                .inkRadius(2.0f)             // 2米涂墨半径
                .build();

        return new FlameWeapon(stats, flameRenderer, camera);
    }
}
