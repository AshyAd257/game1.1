package com.Hecate.flame;

import com.jme3.math.Vector3f;
import com.Hecate.physics.CollisionManager;
import com.Hecate.ink.SparseGridManager;
import com.Hecate.monster.MonsterManager;

/**
 * 火焰粒子(3D世界空间)
 * 表示一个圆形的火焰强度贡献源
 */
public class FlameParticle {

    // 位置(3D世界空间)
    private Vector3f position;

    // 速度(3D)
    private Vector3f velocity;

    // 半径(像素或单位)
    private float radius;

    // 强度(0~1)
    private float intensity;

    // 软边系数(推荐2-3)
    private float softness;

    // 生命周期
    private float lifetime;
    private float maxLifetime;

    // 是否存活
    private boolean alive;

    // 是否刚触地(用于触发地面火焰)
    private boolean justHitGround;

    // 发射者阵营ID（实例字段，每个粒子携带自己的发射者信息）
    private int factionId;

    // 本粒子所携带的伤害值，命中怪物时生效（不影响落地涂墨）
    private float damage;

    // 本粒子所属的"这一发子弹"的唯一标识：一次开火发射的一整团粒子共享同一shotId，
    // 用于让MonsterManager把它们当作同一发子弹去重伤害，而不是每个粒子单独计伤
    private long shotId;

    // 碰撞检测管理器
    private static CollisionManager collisionManager;

    // 涂墨网格管理器
    private static SparseGridManager gridManager;

    // 怪物管理器（用于弹道命中检测），世界切换时WorldSwitcher会重新指向
    private static MonsterManager monsterManager;

    // 物理参数
    private static final float GRAVITY = -15f; // 向下的重力加速度(3D世界单位)
    private static final float AIR_RESISTANCE = 0.98f; // 空气阻力系数(每帧)
    private float turbulenceTime = 0f; // 扰动时间累加器

    /**
     * 构造函数（无伤害，用于环境火焰视觉粒子，如地面火焰扩散产生的粒子）
     * @param position 初始位置
     * @param velocity 初始速度
     * @param radius 粒子半径
     * @param intensity 强度
     * @param softness 软边系数
     * @param lifetime 生命周期
     * @param factionId 发射者阵营ID
     */
    public FlameParticle(Vector3f position, Vector3f velocity, float radius, float intensity, float softness, float lifetime, int factionId) {
        this(position, velocity, radius, intensity, softness, lifetime, factionId, 0f, -1L);
    }

    /**
     * 构造函数（带伤害，用于武器直接发射的子弹粒子）
     * @param damage 命中怪物时造成的伤害
     * @param shotId 本粒子所属的"这一发子弹"的唯一标识，用于伤害去重（见类字段注释）
     */
    public FlameParticle(Vector3f position, Vector3f velocity, float radius, float intensity, float softness,
                          float lifetime, int factionId, float damage, long shotId) {
        this.position = position.clone();
        this.velocity = velocity.clone();
        this.radius = radius;
        this.intensity = intensity;
        this.softness = softness;
        this.lifetime = lifetime;
        this.maxLifetime = lifetime;
        this.alive = true;
        this.factionId = factionId;
        this.damage = damage;
        this.shotId = shotId;
    }

    /**
     * 更新粒子(包含物理效果和方块碰撞)
     */
    public void update(float tpf) {
        if (!alive) return;

        // 重置触地标志
        justHitGround = false;

        // 应用重力(向下加速)
        velocity.y += GRAVITY * tpf;

        // 应用空气阻力(速度衰减)
        velocity.multLocal(AIR_RESISTANCE);

        // 添加轻微的随机扰动(模拟气流)
        turbulenceTime += tpf * 3.0f;
        float turbulenceX = (float) Math.sin(turbulenceTime) * 0.05f;
        float turbulenceZ = (float) Math.cos(turbulenceTime * 1.3f) * 0.03f;

        // 计算新位置(包含扰动)
        Vector3f newPosition = position.clone();
        newPosition.x += (velocity.x + turbulenceX) * tpf;
        newPosition.y += velocity.y * tpf;
        newPosition.z += (velocity.z + turbulenceZ) * tpf;

        // 怪物命中检测（先于地形碰撞检测：子弹应该打中怪物本身，而不是先撞到怪物脚下的地面）
        // 只有带伤害的粒子（damage>0，武器直接发射的子弹）才参与命中判定，
        // 环境视觉粒子（如地面火焰扩散产生的粒子）不应该对怪物造成伤害。
        if (damage > 0f && monsterManager != null) {
            Vector3f hitPoint = monsterManager.checkHit(position, newPosition, damage, shotId);
            if (hitPoint != null) {
                // 命中怪物：粒子消失，不再落地涂墨
                alive = false;
                return;
            }
        }

        // 方块碰撞检测（改进版：找到实际地面表面）
        if (collisionManager != null) {
            // 检测从旧位置到新位置的路径上是否碰撞
            Vector3f surfacePos = findGroundSurface(position, newPosition);

            if (surfacePos != null) {
                // 找到了地面表面，在此位置涂墨
                position.set(surfacePos);

                // 在落地点涂墨（圆形区域）
                if (gridManager != null) {
                    // 使用粒子携带的发射者阵营涂墨
                    gridManager.inkCircle(surfacePos, 0.5f, this.factionId);

                    // 移除落地涂墨日志 - 太频繁且无用
                } else {

                }

                // 标记为触地
                justHitGround = true;

                // 粒子消失
                alive = false;

                return;
            }
        }

        // 没有碰撞,更新位置
        position.set(newPosition);

        // 更新生命周期
        lifetime -= tpf;
        if (lifetime <= 0) {
            alive = false;
        }

        // 粒子随时间缩小
        float lifeRatio = lifetime / maxLifetime;
        // radius 会在渲染时乘以(0.5 + lifeRatio * 0.5) 来实现缩小效果
    }

    /**
     * 检测指定方块位置是否为固体
     */
    private boolean isBlockSolidAt(int x, int y, int z) {
        if (collisionManager == null) {
            // 没有碰撞管理器时,使用简单的地面检测
            return y <= 1;
        }
        return collisionManager.isBlockSolidAt(x, y, z);
    }

    /**
     * 找到从起点到终点路径上的地面表面位置
     * @param oldPos 起点（上一帧位置）
     * @param newPos 终点（当前帧位置）
     * @return 地面表面位置，如果没有碰撞返回 null
     */
    private Vector3f findGroundSurface(Vector3f oldPos, Vector3f newPos) {
        // 计算移动方向和距离
        Vector3f direction = newPos.subtract(oldPos);
        float distance = direction.length();

        if (distance < 0.001f) {
            return null; // 几乎没有移动
        }

        direction.normalizeLocal();

        // 沿着移动路径逐步检测（步长0.1米）
        float stepSize = 0.1f;
        int steps = (int) Math.ceil(distance / stepSize);

        for (int i = 0; i <= steps; i++) {
            float t = Math.min(i * stepSize, distance);
            Vector3f checkPos = oldPos.add(direction.mult(t));

            // 使用CollisionManager的getTerrainHeightAt方法获取精确地形高度
            if (collisionManager != null) {
                float terrainHeight = collisionManager.getTerrainHeightAt(checkPos.x, checkPos.z);

                // 如果有地形数据，且粒子已经低于地形表面
                if (!Float.isNaN(terrainHeight) && checkPos.y <= terrainHeight) {

                    // 在地形表面上方0.22格，与GridDebugRenderer的RENDER_HEIGHT_OFFSET一致
                    return new Vector3f(checkPos.x, terrainHeight + 0.22f, checkPos.z);
                }
            } else {
                // 没有CollisionManager，使用简单的方块检测
                int blockX = (int) Math.floor(checkPos.x);
                int blockY = (int) Math.floor(checkPos.y);
                int blockZ = (int) Math.floor(checkPos.z);

                if (isBlockSolidAt(blockX, blockY, blockZ)) {
                    float surfaceY = blockY + 1.0f;
                    return new Vector3f(checkPos.x, surfaceY, checkPos.z);
                }
            }
        }

        return null; // 没有碰撞
    }

    /**
     * 重置粒子(用于对象池)——无伤害版本，用于环境火焰视觉粒子
     */
    public void reset(Vector3f position, Vector3f velocity, float radius, float intensity, float softness, float lifetime, int factionId) {
        reset(position, velocity, radius, intensity, softness, lifetime, factionId, 0f, -1L);
    }

    /**
     * 重置粒子(用于对象池)——带伤害版本，用于武器直接发射的子弹粒子
     */
    public void reset(Vector3f position, Vector3f velocity, float radius, float intensity, float softness,
                       float lifetime, int factionId, float damage, long shotId) {
        this.position.set(position);
        this.velocity.set(velocity);
        this.radius = radius;
        this.intensity = intensity;
        this.softness = softness;
        this.lifetime = lifetime;
        this.maxLifetime = lifetime;
        this.alive = true;
        this.turbulenceTime = 0f;
        this.justHitGround = false;
        this.factionId = factionId;
        this.damage = damage;
        this.shotId = shotId;
    }

    // Getters
    public Vector3f getPosition() { return position; }
    public Vector3f getVelocity() { return velocity; }
    public float getRadius() { return radius; }
    public float getIntensity() {
        // 返回衰减后的强度
        float lifeRatio = lifetime / maxLifetime;
        return intensity * lifeRatio;
    }
    public float getRawIntensity() { return intensity; }
    public float getSoftness() { return softness; }
    public float getLifetime() { return lifetime; }
    public float getLifeRatio() { return lifetime / maxLifetime; }
    public boolean isAlive() { return alive; }
    public boolean justHitGround() { return justHitGround; }

    // Setters
    public void setPosition(Vector3f position) { this.position.set(position); }
    public void setVelocity(Vector3f velocity) { this.velocity.set(velocity); }
    public void setRadius(float radius) { this.radius = radius; }
    public void setIntensity(float intensity) { this.intensity = intensity; }
    public void setSoftness(float softness) { this.softness = softness; }
    public void kill() { this.alive = false; }

    /**
     * 设置碰撞管理器(静态方法,所有粒子共享)
     */
    public static void setCollisionManager(CollisionManager manager) {
        collisionManager = manager;
    }

    /**
     * 设置涂墨网格管理器(静态方法,所有粒子共享)
     */
    public static void setGridManager(SparseGridManager manager) {
        gridManager = manager;
    }

    /**
     * 设置怪物管理器(静态方法,所有粒子共享)，用于弹道命中检测
     */
    public static void setMonsterManager(MonsterManager manager) {
        monsterManager = manager;
    }
}
