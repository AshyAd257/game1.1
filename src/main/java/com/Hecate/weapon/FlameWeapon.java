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

        float damage = stats.getBaseDamage();

        if (targetPos != null) {
            // 计算发射速度
            Vector3f velocity = calculateShootVelocity(handPos3D, targetPos);
            emitWithExtraProjectiles(handPos3D, velocity, 150, damage);
        } else {
            // 没有目标，使用默认发射
            emitWithExtraProjectiles(handPos3D, null, 150, damage);
        }

    }

    @Override
    protected void fireCharged(Vector3f origin, Vector3f direction, float damageMultiplier) {
        // 蓄力攻击：发射更多粒子，伤害按蓄力倍率提升
        Vector3f handPos3D = getHandPosition3D(origin);
        Vector3f targetPos = getTargetPosition();

        float damage = stats.getBaseDamage() * damageMultiplier;

        if (targetPos != null) {
            Vector3f velocity = calculateShootVelocity(handPos3D, targetPos);
            int particleCount = (int)(150 * damageMultiplier);
            emitWithExtraProjectiles(handPos3D, velocity, particleCount, damage);
        }
    }

    /**
     * 发射主弹道，并按"攻击弹道+1"buff的叠加数量额外发射偏移角度的弹道。
     * <p>额外弹道朝主方向的左右交替偏移（+15°, -15°, +30°, -30°...），
     * 在水平面上像扇形一样展开；不改变垂直速度分量，保持每条弹道各自的
     * 抛物线落点距离感一致，只是发射方向不同。
     *
     * @param baseVelocity 主弹道初始速度（null表示没有目标，emitFlame内部会用默认速度，
     *                     此时额外弹道也跳过，因为没有方向可供旋转偏移）
     */
    private void emitWithExtraProjectiles(Vector3f handPos3D, Vector3f baseVelocity, int particleCount, float damage) {
        flameRenderer.emitFlame(handPos3D, particleCount, baseVelocity, damage);

        if (baseVelocity == null || playerController == null) {
            return;
        }

        int extra = playerController.getExtraProjectiles();
        for (int i = 1; i <= extra; i++) {
            float angleDeg = 15f * ((i + 1) / 2); // 1,1,2,2,3,3... -> 15,15,30,30,45,45
            float signedAngle = (i % 2 == 1) ? angleDeg : -angleDeg;
            Vector3f offsetVelocity = rotateAroundY(baseVelocity, signedAngle);
            flameRenderer.emitFlame(handPos3D, particleCount, offsetVelocity, damage);
        }
    }

    /**
     * 绕世界Y轴旋转速度向量的水平分量（改变发射方向），保留垂直速度分量不变
     */
    private Vector3f rotateAroundY(Vector3f velocity, float degrees) {
        float rad = degrees * FastMath.DEG_TO_RAD;
        float cos = FastMath.cos(rad);
        float sin = FastMath.sin(rad);
        float newX = velocity.x * cos - velocity.z * sin;
        float newZ = velocity.x * sin + velocity.z * cos;
        return new Vector3f(newX, velocity.y, newZ);
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
