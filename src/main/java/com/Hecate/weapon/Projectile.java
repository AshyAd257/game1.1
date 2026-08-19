package com.Hecate.weapon;

import com.jme3.math.Vector3f;
import com.Hecate.event.EventBus;
import com.Hecate.event.PaintEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * 子弹实体 - 独立的飞行对象
 *
 * 职责：
 * 1. 存储位置、速度、弹道配置
 * 2. 每帧更新位置（根据arc类型）
 * 3. 检测生命周期（traveled vs maxRange）
 * 4. 触发命中/超时事件
 *
 * 设计理念：
 * - 子弹是纯数据对象，不包含渲染逻辑
 * - arc类型决定update()中的物理分支
 * - 事件驱动：onHit/onExpire触发回调
 */
public class Projectile {

    // 子弹配置（不可变引用）
    private final ProjectileProfile profile;

    // 物理状态（可变）
    private Vector3f position;          // 当前位置（3D世界坐标）
    private Vector3f velocity;          // 当前速度（米/秒）
    private float traveled;             // 已飞行距离（米）
    private float lifetime;             // 已存活时间（秒）

    // 状态标志
    private boolean alive;              // 是否存活

    // 穿透剩余次数：初始等于profile.getHitEffect().pierceCount。
    // 注意：Projectile自身不记录"已经命中过哪些目标"，同一发子弹是否会对
    // 同一个目标重复计伤，需要调用hit()的一方（碰撞检测系统）自行去重
    // （参考Monster.takeDamage的shotId去重机制）。
    private int remainingPierces;

    // 沿途涂墨计时器：仅profile.isPaintAlongPath()==true时使用
    private float pathPaintTimer;

    // 发射信息（用于命中效果）
    private final float chargeMult;     // 蓄力倍率（影响伤害/射程）
    private final int teamId;           // 发射者队伍

    // 事件监听器
    private final List<ProjectileEventListener> listeners;

    // 事件总线（可选，用于发射全局事件）
    private EventBus eventBus;

    /**
     * 构造函数
     * @param profile 子弹配置
     * @param startPos 发射起点
     * @param direction 发射方向（单位向量）
     * @param chargeMult 蓄力倍率（1.0=普通，>1.0=蓄力增强）
     * @param teamId 发射者队伍
     */
    public Projectile(ProjectileProfile profile, Vector3f startPos, Vector3f direction,
                     float chargeMult, int teamId) {
        this.profile = profile;
        this.position = startPos.clone();

        // 根据蓄力倍率计算初速度
        float speed = profile.getVelocity() * chargeMult;
        this.velocity = direction.normalize().mult(speed);

        this.traveled = 0.0f;
        this.lifetime = 0.0f;
        this.alive = true;
        this.chargeMult = chargeMult;
        this.teamId = teamId;
        this.listeners = new ArrayList<>();
        this.eventBus = null;  // 默认不发射全局事件
        this.remainingPierces = profile.getHitEffect() != null ? profile.getHitEffect().pierceCount : 0;
        this.pathPaintTimer = 0.0f;
    }

    /**
     * 每帧更新子弹物理状态
     * @param deltaTime 时间增量（秒）
     */
    public void update(float deltaTime) {
        if (!alive) return;

        // 根据弹道类型应用物理
        switch (profile.getArcType()) {
            case LINEAR:
                updateLinear(deltaTime);
                break;

            case BALLISTIC:
                updateBallistic(deltaTime);
                break;

            case HOMING:
                updateHoming(deltaTime);
                break;

            case MELEE_SWING:
                // TODO: 近战挥砍弧线（短程、慢速、沿弧形轨迹），占位阶段先落回直线
                updateLinear(deltaTime);
                break;
        }

        // 更新飞行距离
        float frameDistance = velocity.length() * deltaTime;
        traveled += frameDistance;

        // 更新存活时间
        lifetime += deltaTime;

        // 沿途涂墨（狙击枪弹道痕迹、镰刀甩墨等）
        if (profile.isPaintAlongPath()) {
            updatePathPaint(deltaTime);
        }

        // 检查生命周期结束条件
        checkLifecycle();
    }

    /**
     * 沿飞行路径按固定间隔发出涂墨事件，与命中/超时时的单点涂墨互不影响。
     * 半径复用hitEffect.inkRadius，强度固定为1（不受蓄力倍率影响，避免和命中涂墨的
     * 强度语义混淆）。
     */
    private void updatePathPaint(float deltaTime) {
        pathPaintTimer += deltaTime;
        float interval = profile.getPathPaintInterval();
        if (interval <= 0f || pathPaintTimer < interval) {
            return;
        }
        pathPaintTimer = 0f;

        if (eventBus != null && profile.getHitEffect() != null) {
            PaintEvent pathPaintEvent = new PaintEvent(
                    position, profile.getHitEffect().inkRadius, teamId, 1.0f);
            eventBus.publish(pathPaintEvent);
        }
    }

    /**
     * 直线弹道：不受重力影响，匀速直线飞行
     */
    private void updateLinear(float deltaTime) {
        position.addLocal(velocity.mult(deltaTime));
    }

    /**
     * 抛物线弹道：受重力影响
     */
    private void updateBallistic(float deltaTime) {
        // 应用重力加速度
        velocity.y += profile.getGravity() * deltaTime;

        // 应用空气阻力（速度衰减）
        velocity.multLocal(profile.getDrag());

        // 更新位置
        position.addLocal(velocity.mult(deltaTime));
    }

    /**
     * 追踪弹道：自动瞄准目标（待实现）
     */
    private void updateHoming(float deltaTime) {
        // TODO: 实现追踪逻辑
        // 1. 查找最近目标
        // 2. 计算转向力
        // 3. 限制最大转向角
        updateLinear(deltaTime);  // 暂时使用直线
    }

    /**
     * 检查生命周期结束条件
     */
    private void checkLifecycle() {
        // 条件1：超过最大射程
        if (traveled >= profile.getMaxRange() * chargeMult) {
            expire();
            return;
        }

        // 条件2：超过最大存活时间
        if (lifetime >= profile.getMaxLifetime()) {
            expire();
            return;
        }

        // 条件3：击中物体（由外部碰撞系统调用hit()）
    }

    /**
     * 命中目标（由碰撞系统调用）
     * @param hitPoint 命中点
     */
    public void hit(Vector3f hitPoint) {
        if (!alive) return;

        // 穿透：还有剩余穿透次数时，子弹命中后继续存活（不发expire/不清alive），
        // 只消耗一次穿透次数。伤害结算由调用方（碰撞检测系统）自行处理，
        // Projectile本身不关心具体命中的是谁。
        if (remainingPierces > 0) {
            remainingPierces--;
        } else {
            alive = false;
        }

        // 发射PaintEvent到事件总线（如果已连接）
        if (eventBus != null && profile.getHitEffect() != null) {
            float paintRadius = profile.getHitEffect().inkRadius;
            float paintIntensity = chargeMult;  // 蓄力倍率影响涂墨强度

            PaintEvent paintEvent = new PaintEvent(hitPoint, paintRadius, teamId, paintIntensity);
            eventBus.publish(paintEvent);
        }

        // 触发onHit事件（本地监听器）
        ProjectileEvent event = new ProjectileEvent(
                ProjectileEvent.Type.HIT,
                hitPoint,
                this
        );
        notifyListeners(event);
    }

    /**
     * 超时消失
     */
    private void expire() {
        if (!alive) return;

        alive = false;

        // 超时不涂墨（或根据ExpireEffect决定）
        // 如果有ExpireEffect.explode，可以在这里发射爆炸涂墨事件

        // 触发onExpire事件
        ProjectileEvent event = new ProjectileEvent(
                ProjectileEvent.Type.EXPIRE,
                position.clone(),
                this
        );
        notifyListeners(event);
    }

    /**
     * 添加事件监听器
     */
    public void addListener(ProjectileEventListener listener) {
        listeners.add(listener);
    }

    /**
     * 设置事件总线（用于发射全局事件）
     */
    public void setEventBus(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    /**
     * 通知所有监听器
     */
    private void notifyListeners(ProjectileEvent event) {
        for (ProjectileEventListener listener : listeners) {
            listener.onProjectileEvent(event);
        }
    }

    // Getters
    public ProjectileProfile getProfile() { return profile; }
    public Vector3f getPosition() { return position.clone(); }
    public Vector3f getVelocity() { return velocity.clone(); }
    public float getTraveled() { return traveled; }
    public float getLifetime() { return lifetime; }
    public boolean isAlive() { return alive; }
    public float getChargeMult() { return chargeMult; }
    public int getTeamId() { return teamId; }
    public int getRemainingPierces() { return remainingPierces; }

    /**
     * 子弹事件
     */
    public static class ProjectileEvent {
        public enum Type {
            HIT,        // 命中目标
            EXPIRE      // 超时消失
        }

        public final Type type;
        public final Vector3f position;     // 事件发生位置
        public final Projectile projectile; // 子弹本体

        public ProjectileEvent(Type type, Vector3f position, Projectile projectile) {
            this.type = type;
            this.position = position;
            this.projectile = projectile;
        }

        @Override
        public String toString() {
            return String.format("ProjectileEvent[%s, pos=(%.2f,%.2f,%.2f)]",
                    type, position.x, position.y, position.z);
        }
    }

    /**
     * 事件监听器接口
     */
    public interface ProjectileEventListener {
        void onProjectileEvent(ProjectileEvent event);
    }

    @Override
    public String toString() {
        return String.format("Projectile[%s, pos=(%.2f,%.2f,%.2f), vel=(%.2f,%.2f,%.2f), traveled=%.2f, alive=%s]",
                profile.getId(), position.x, position.y, position.z,
                velocity.x, velocity.y, velocity.z, traveled, alive);
    }
}
