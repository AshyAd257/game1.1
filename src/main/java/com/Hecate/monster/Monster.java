package com.Hecate.monster;

import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import com.jme3.asset.AssetManager;
import com.Hecate.physics.AABB;
import com.Hecate.physics.CollisionManager;

import java.util.HashSet;
import java.util.Set;

/**
 * 最初的怪物原型：一个 1x1x1 的红色方块。
 *
 * <p>行为约定：
 * <ul>
 *   <li>受击瞬间闪白 {@link #FLASH_DURATION} 秒</li>
 *   <li>命中瞬间自身极短暂暂停（hit-stop）{@link #HITSTOP_FRAMES} 帧，只影响这只怪物
 *       的位置/动画更新，不影响其他系统</li>
 *   <li>死亡后在原地留下一滩玩家友方涂色（由 {@link MonsterManager} 负责触发涂墨，
 *       因为涂墨要用到当前世界的 SparseGridManager，怪物本身不持有这个引用）</li>
 * </ul>
 */
public class Monster {

    // 基础数值：MonsterDefinition 的倍率相对这些基准值应用
    private static final float BASE_SIZE = 1.0f;
    private static final float BASE_MAX_HEALTH = 30.0f;
    // 基础移动速度：略慢于玩家步行速度（PlayerController.WALK_SPEED = 3.25），
    // 保证玩家正常走路时能甩开怪物，但不能一直跑（疾跑速度更快）。
    private static final float BASE_MOVE_SPEED = 2.6f;

    private static final float FLASH_DURATION = 0.05f;
    private static final int HITSTOP_FRAMES = 3; // 命中瞬间暂停的帧数（2~3帧取上限）

    // 保留作为向后兼容的默认值（MonsterDefinition.Builder 的默认攻击数值取自这里）
    public static final float ATTACK_COOLDOWN = 1.0f;
    public static final float ATTACK_DAMAGE = 10.0f;

    private static final ColorRGBA BASE_COLOR = new ColorRGBA(0.8f, 0.1f, 0.1f, 1.0f);
    private static final ColorRGBA FLASH_COLOR = ColorRGBA.White;

    private final Geometry geometry;
    private final Material material;

    // 本怪物的数据定义（数值+行为配置），取代过去"只有倍率"的 MonsterVariant
    private final MonsterDefinition definition;

    // 本实例的实际数值（基础值 x 对应MonsterDefinition的倍率）
    private final float size;
    private final float moveSpeed;

    private Vector3f position;
    private float health;
    private boolean alive = true;

    // 水平朝向角（弧度）。定义域按需wrap到[-PI, PI]，0表示朝+X方向。
    private float facingYaw = 0f;

    private float flashTimer = 0f;
    private int hitStopFramesRemaining = 0;

    // 距离上次成功命中玩家的时间，用于攻击冷却
    private float attackCooldownRemaining = 0f;

    // 本怪物的攻击行为（由definition.attackBehaviorFactory创建，每只怪物独立一份，
    // 因为部分行为实现需要持有各自独立的武器冷却状态）
    private final MonsterAttackBehavior attackBehavior;

    // 本怪物所属的波次编号（由MonsterManager的波次系统设置）。-1表示不属于任何波次
    // （例如 /mob1 命令生成的测试怪物），波次完成判定不会将其计入。
    private int waveNumber = -1;

    // 一次开火（如gun1按一次左键）会发射上百个共享同一shotId的粒子（视觉密集效果），
    // 但这本质上是同一发"子弹"。记录已经生效过的shotId，避免同一发子弹的多个粒子
    // 几乎同时命中时被重复计算伤害。集合大小有限（近期几发），定期清理防止无限增长。
    private final Set<Long> consumedShotIds = new HashSet<>();
    private static final int MAX_TRACKED_SHOT_IDS = 32;

    /**
     * @param spawnFootPosition 出生点（脚底位置，世界坐标）。构造函数内部会按本怪物的
     *                          实际体型（受variant影响）把它转换为几何中心坐标。
     * @param variant 怪物变体，决定体型/血量/移速的倍率以及攻击行为
     */
    public Monster(AssetManager assetManager, Node parentNode, Vector3f spawnFootPosition, MonsterVariant variant) {
        this(assetManager, parentNode, spawnFootPosition, variant.definition);
    }

    /**
     * @param spawnFootPosition 出生点（脚底位置，世界坐标）。构造函数内部会按本怪物的
     *                          实际体型（受definition影响）把它转换为几何中心坐标。
     * @param definition 怪物数据定义，决定体型/血量/移速/攻击数值/攻击行为
     */
    public Monster(AssetManager assetManager, Node parentNode, Vector3f spawnFootPosition, MonsterDefinition definition) {
        this.definition = definition;
        this.size = BASE_SIZE * definition.sizeMultiplier;
        this.moveSpeed = BASE_MOVE_SPEED * definition.speedMultiplier;
        this.health = BASE_MAX_HEALTH * definition.healthMultiplier;
        this.attackBehavior = definition.attackBehaviorFactory.create();

        this.position = spawnFootPosition.clone();
        this.position.y += size / 2f;

        Box box = new Box(size / 2f, size / 2f, size / 2f);
        geometry = new Geometry("Monster", box);

        material = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        material.setColor("Color", BASE_COLOR);
        material.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);
        geometry.setMaterial(material);
        geometry.setQueueBucket(RenderQueue.Bucket.Opaque);
        geometry.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);

        // Box的几何中心即position：已在上方将脚底坐标转换为几何中心坐标（+size/2）
        geometry.setLocalTranslation(position);

        parentNode.attachChild(geometry);
    }

    /**
     * 每帧更新（受击闪白计时、hit-stop倒计时、朝玩家移动、攻击冷却）
     * @param tpf 帧时间
     * @param playerPosition 玩家当前位置（世界坐标）
     * @param collisionManager 当前活动世界的碰撞管理器，用于查询地形高度（判断脚下是否为虚空）
     */
    public void update(float tpf, Vector3f playerPosition, CollisionManager collisionManager) {
        if (!alive) return;

        if (hitStopFramesRemaining > 0) {
            // hit-stop期间跳过移动，但闪白计时仍正常流逝——
            // hit-stop只是"被打了一下愣住"，不是全局暂停
            hitStopFramesRemaining--;
        } else {
            moveTowardPlayer(tpf, playerPosition, collisionManager);
        }

        if (flashTimer > 0f) {
            flashTimer -= tpf;
            if (flashTimer <= 0f) {
                flashTimer = 0f;
                material.setColor("Color", BASE_COLOR);
            }
        }
    }

    /**
     * 固定步长更新（由 {@link com.Hecate.core.FixedTickScheduler} 驱动，默认20Hz）。
     * <p>攻击冷却判定挪到固定刻，保证在任意渲染帧率下"多久能再打一次"的判定时序一致，
     * 不受帧率波动影响。移动/视觉表现（闪白、hit-stop）仍走可变帧率的 {@link #update}。
     */
    public void fixedUpdate(float dt, com.Hecate.player.PlayerController player, com.Hecate.ink.SparseGridManager gridManager) {
        if (!alive) return;

        if (attackCooldownRemaining > 0f) {
            attackCooldownRemaining -= dt;
        }

        attackBehavior.fixedUpdate(dt, this, player, gridManager);
    }

    /**
     * 朝玩家水平移动，避免走入虚空（脚下查不到地形数据的地方）。
     * <p>先尝试沿玩家方向的完整对角线移动；若目标点是虚空，则退化为分轴尝试
     * （只走X轴或只走Z轴），与 {@code CollisionManager.checkCollision} 处理实体碰撞
     * 时的分轴思路一致，只是这里判断的是"地形是否存在"而不是"是否有实体挡路"。
     * 两个轴都会掉进虚空时，原地不动。
     */
    private void moveTowardPlayer(float tpf, Vector3f playerPosition, CollisionManager collisionManager) {
        Vector3f toPlayer = playerPosition.subtract(position);
        toPlayer.y = 0;

        if (toPlayer.lengthSquared() < 0.01f) {
            return; // 已经贴近玩家，不需要再移动（避免原地抖动）
        }

        toPlayer.normalizeLocal();
        updateFacing(toPlayer, tpf);

        float moveDist = moveSpeed * tpf;
        Vector3f delta = toPlayer.mult(moveDist);

        Vector3f allowedDelta = resolveSafeMovement(delta, collisionManager);
        position.x += allowedDelta.x;
        position.z += allowedDelta.z;

        // 移动后贴合新位置的地形高度（脚底站在地表上）
        if (collisionManager != null) {
            float terrainHeight = collisionManager.getTerrainHeightAt(position.x, position.z);
            if (!Float.isNaN(terrainHeight)) {
                position.y = terrainHeight + size / 2f;
            }
        }

        geometry.setLocalTranslation(position);
    }

    /**
     * 更新水平朝向角，朝targetDirection（已归一化的XZ方向）转动。
     * <p>{@code definition.turnSpeedDegPerSec <= 0} 表示瞬间转向——这是所有现有怪物
     * （SLOW/NORMAL/MINI_BOSS）的默认行为，保证本次改动不影响它们原有的手感。
     * 需要"转身有延迟"的精英怪可以设置正的转向速度。
     */
    private void updateFacing(Vector3f targetDirection, float tpf) {
        float targetYaw = FastMath.atan2(targetDirection.z, targetDirection.x);

        if (definition.turnSpeedDegPerSec <= 0f) {
            facingYaw = targetYaw;
            return;
        }

        float maxDelta = definition.turnSpeedDegPerSec * FastMath.DEG_TO_RAD * tpf;
        float diff = shortestAngleDiff(targetYaw, facingYaw);

        if (Math.abs(diff) <= maxDelta) {
            facingYaw = targetYaw;
        } else {
            facingYaw += Math.signum(diff) * maxDelta;
        }
    }

    /**
     * 计算从fromAngle转到toAngle的最短角度差，结果落在(-PI, PI]。
     * 不依赖FastMath.normalizeAngle的具体值域约定，手写wrap避免版本差异。
     */
    private static float shortestAngleDiff(float toAngle, float fromAngle) {
        float diff = (toAngle - fromAngle) % FastMath.TWO_PI;
        if (diff > FastMath.PI) {
            diff -= FastMath.TWO_PI;
        } else if (diff < -FastMath.PI) {
            diff += FastMath.TWO_PI;
        }
        return diff;
    }

    /**
     * 是否正朝向目标位置（水平面），用于"需要正面命中/正面攻击"类怪物的判定。
     * @param targetPosition 目标位置（世界坐标）
     */
    public boolean isFacingTarget(Vector3f targetPosition) {
        Vector3f toTarget = targetPosition.subtract(position);
        toTarget.y = 0;
        if (toTarget.lengthSquared() < 0.0001f) {
            return true; // 目标就在脚下，视为已朝向
        }

        float targetYaw = FastMath.atan2(toTarget.z, toTarget.x);
        float diff = shortestAngleDiff(targetYaw, facingYaw);

        return Math.abs(diff) <= definition.facingToleranceDeg * FastMath.DEG_TO_RAD;
    }

    public float getFacingYaw() {
        return facingYaw;
    }

    public MonsterDefinition getDefinition() {
        return definition;
    }

    /**
     * 检查移动增量是否会导致怪物落入虚空，返回实际允许的移动增量
     */
    private Vector3f resolveSafeMovement(Vector3f delta, CollisionManager collisionManager) {
        if (collisionManager == null) {
            return delta; // 没有碰撞系统可查询，直接允许移动
        }

        float targetX = position.x + delta.x;
        float targetZ = position.z + delta.z;

        // 优先尝试完整的对角线移动
        if (!Float.isNaN(collisionManager.getTerrainHeightAt(targetX, targetZ))) {
            return delta;
        }

        // 对角线目标点是虚空：退化为只走X轴
        if (!Float.isNaN(collisionManager.getTerrainHeightAt(targetX, position.z))) {
            return new Vector3f(delta.x, 0, 0);
        }

        // 只走Z轴
        if (!Float.isNaN(collisionManager.getTerrainHeightAt(position.x, targetZ))) {
            return new Vector3f(0, 0, delta.z);
        }

        // 两个轴都会掉入虚空，原地不动
        return new Vector3f(0, 0, 0);
    }

    /**
     * 是否可以发起新一次接触攻击（冷却已结束）
     */
    public boolean isAttackReady() {
        return attackCooldownRemaining <= 0f;
    }

    /**
     * 重置攻击冷却（成功命中玩家后调用）
     */
    public void resetAttackCooldown() {
        attackCooldownRemaining = ATTACK_COOLDOWN;
    }

    /**
     * 受到伤害
     * @param damage 伤害值
     * @param shotId 发射本次伤害的"这一发子弹"的唯一标识。同一shotId重复调用只会在
     *               第一次生效——一次开火发射的一整团粒子本质上是同一发子弹，不应
     *               因为多个粒子几乎同时命中就叠加伤害。传入-1表示不做去重（每次必然生效）。
     * @return 是否因此死亡
     */
    public boolean takeDamage(float damage, long shotId) {
        if (!alive) return false;

        if (shotId >= 0 && !consumedShotIds.add(shotId)) {
            // 这一发子弹已经命中过本怪物一次，本次命中不重复计伤
            return false;
        }

        health -= damage;

        // 受击闪白
        flashTimer = FLASH_DURATION;
        material.setColor("Color", FLASH_COLOR);

        // hit-stop：命中瞬间自身极短暂暂停
        hitStopFramesRemaining = HITSTOP_FRAMES;

        if (consumedShotIds.size() > MAX_TRACKED_SHOT_IDS) {
            consumedShotIds.clear();
        }

        if (health <= 0f) {
            alive = false;
            return true;
        }
        return false;
    }

    /**
     * 获取用于命中检测的包围盒（世界坐标）
     */
    public AABB getBoundingBox() {
        return new AABB(position, size, size, size);
    }

    public Vector3f getPosition() {
        return position.clone();
    }

    public boolean isAlive() {
        return alive;
    }

    /**
     * 本怪物所属的波次编号，-1表示不属于任何波次
     */
    public int getWaveNumber() {
        return waveNumber;
    }

    public void setWaveNumber(int waveNumber) {
        this.waveNumber = waveNumber;
    }

    /**
     * 从场景中移除（死亡清理）
     */
    public void removeFromScene() {
        geometry.removeFromParent();
    }
}
