package com.Hecate.weapon;

import com.Hecate.ink.SparseGridManager;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;

/**
 * 蒸汽朋克枪（Gun1）
 * 中距离武器，每次开火产生一发方块抛体，沿抛物线飞行（重力+阻力独立于其他步枪配置），
 * 命中判定/地形碰撞/沿途涂墨均由外部的 {@link ProjectileManager} 驱动。
 * 方向带有小幅随机偏移，模拟原先散射手感。
 */
public class SteampunkGun extends Weapon {

    // 子弹配置（弹道+命中效果+视觉）——每把步枪自己的抛物线参数
    private final ProjectileProfile projectileProfile;

    // 依赖项（子弹的实际飞行/碰撞/涂墨由外部的子弹更新循环驱动，这里只负责生成Projectile）
    private SparseGridManager gridManager;      // 墨水系统
    private Node worldNode;                     // 世界节点
    private int playerFactionId = com.Hecate.ink.FactionRegistry.DARK_DEFAULT;  // 玩家阵营ID

    // 每次开火产生的子弹交给外部监听器处理（PlayerController里的子弹更新循环）
    private ProjectileSpawnListener spawnListener;

    /**
     * 构造函数
     */
    public SteampunkGun(WeaponStats stats, ProjectileProfile projectileProfile) {
        super(stats, WeaponKind.RIFLE);
        this.projectileProfile = projectileProfile;
    }

    /**
     * 设置墨水系统
     */
    public void setGridManager(SparseGridManager gridManager) {
        this.gridManager = gridManager;
    }

    /**
     * 设置世界节点（用于射线检测）
     */
    public void setWorldNode(Node worldNode) {
        this.worldNode = worldNode;
    }

    /**
     * 设置玩家阵营ID
     */
    public void setPlayerFactionId(int factionId) {
        this.playerFactionId = factionId;
    }

    @Deprecated
    public void setPlayerTeam(int team) {
        // 向后兼容：将 team 映射到 factionId
        this.playerFactionId = (team == 0)
            ? com.Hecate.ink.FactionRegistry.LIGHT_DEFAULT
            : com.Hecate.ink.FactionRegistry.DARK_DEFAULT;
    }

    public void setSpawnListener(ProjectileSpawnListener listener) {
        this.spawnListener = listener;
    }

    /**
     * 执行普通开火：产生一发方块抛体，沿本武器自己的抛物线飞行。
     */
    @Override
    protected void fire(Vector3f origin, Vector3f direction) {
        Vector3f finalDirection = applySpread(direction.clone());

        ProjectileProfile.HitEffect hitEffect = ProjectileProfile.HitEffect.simple(
                stats.getBaseDamage(), stats.getInkRadius(), playerFactionId);

        // 注意：这里不读取stats.getMaxRange()。WeaponStats.maxRange在旧的火焰粒子系统里
        // 只是纯展示数值（仅getInfo()用），从未参与运动学计算；子弹何时消失完全由
        // 撞地形/撞怪物/存活时间耗尽决定。projectileProfile.getMaxRange()是子弹自己的
        // 物理射程上限（在create()里配置得足够大，保证重力能先把子弹拉到地面），
        // 与WeaponStats.maxRange是两个不同语义的字段，不能混用（混用会导致子弹在
        // 触地前就被射程截断静默消失，不涂墨）。
        ProjectileProfile shotProfile = new ProjectileProfile.Builder(projectileProfile.getId(), projectileProfile.getDisplayName())
                .arcType(projectileProfile.getArcType())
                .velocity(stats.getProjectileVelocity())
                .gravity(projectileProfile.getGravity())
                .drag(projectileProfile.getDrag())
                .maxLifetime(projectileProfile.getMaxLifetime())
                .maxRange(projectileProfile.getMaxRange())
                .hitEffect(hitEffect)
                .expireEffect(projectileProfile.getExpireEffect())
                .visualConfig(projectileProfile.getVisualConfig())
                .build();

        Projectile projectile = new Projectile(shotProfile, origin, finalDirection, 1.0f, playerFactionId);

        if (spawnListener != null) {
            spawnListener.onProjectileSpawned(projectile);
        }
    }

    /**
     * 执行蓄力攻击（蒸汽朋克枪不支持蓄力）
     */
    @Override
    protected void fireCharged(Vector3f origin, Vector3f direction, float damageMultiplier) {
        // 蒸汽朋克枪不支持蓄力，直接调用普通攻击
        fire(origin, direction);
    }

    /**
     * 应用小幅随机方向偏移（保留原散射手感，但现在只影响单发子弹的方向，
     * 而不是像之前的火焰粒子那样让一团粒子各自散开）
     * @param direction 原始方向
     * @return 应用偏移后的方向
     */
    private Vector3f applySpread(Vector3f direction) {
        float spreadAngle = stats.getSpreadAngle();

        if (spreadAngle <= 0) {
            return direction.normalizeLocal();
        }

        // 水平方向：-half 到 +half
        float horizontalAngle = (FastMath.nextRandomFloat() - 0.5f) * spreadAngle * FastMath.DEG_TO_RAD;

        // 垂直方向：较小的散射（保持在合理范围）
        float verticalAngle = (FastMath.nextRandomFloat() - 0.5f) * (spreadAngle * 0.5f) * FastMath.DEG_TO_RAD;

        // 应用旋转
        Vector3f result = direction.clone();

        // 绕Y轴旋转（水平散射）
        float cosY = FastMath.cos(horizontalAngle);
        float sinY = FastMath.sin(horizontalAngle);
        float newX = result.x * cosY - result.z * sinY;
        float newZ = result.x * sinY + result.z * cosY;
        result.x = newX;
        result.z = newZ;

        // 绕X轴旋转（垂直散射）
        float cosX = FastMath.cos(verticalAngle);
        float sinX = FastMath.sin(verticalAngle);
        float newY = result.y * cosX - result.z * sinX;
        newZ = result.y * sinX + result.z * cosX;
        result.y = newY;
        result.z = newZ;

        return result.normalizeLocal();
    }

    /**
     * 创建蒸汽朋克枪实例
     *
     * 属性配置：
     * - 子弹消耗：3点/发
     * - 发射速度：0.5秒/发（可连发）
     * - 射程：2个格子（2米最远）
     * - 散布：60度扇形（方向随机偏移）
     * <p>抛物线参数（重力/阻力）沿用原火焰粒子的手感数值，保持接入方块抛体后
     * 弹道弧度大致不变。
     */
    public static SteampunkGun create() {
        WeaponStats stats = new WeaponStats.Builder("steampunk_gun", "蒸汽朋克枪")
                .fireRate(0.5f)              // 0.5秒攻击间隔（可连发）
                .projectileVelocity(15.0f)   // 15米/秒子弹速度（中等速度）
                .spreadAngle(60.0f)          // 60度散射角（方向随机偏移）
                .maxRange(2.0f)              // 2格子射程（2米最远）
                .ammoCost(3.0f)              // 每发消耗3点弹药
                .baseDamage(8.0f)            // 8点基础伤害
                .inkRadius(0.8f)             // 0.8米涂墨半径
                .hasCharge(false)            // 不支持蓄力
                .build();

        ProjectileProfile profile = new ProjectileProfile.Builder("steampunk_bullet", "蒸汽枪弹")
                .arcType(ProjectileProfile.ArcType.BALLISTIC)
                .velocity(stats.getProjectileVelocity())
                .gravity(-15.0f)              // 与原FlameParticle重力手感一致
                .drag(0.98f)                  // 与原FlameParticle空气阻力手感一致
                .maxLifetime(2.0f)
                // 这里的maxRange是子弹自己的物理射程上限（够大，保证不会在触地前被截断），
                // 与stats.getMaxRange()（面板展示用的"射程1.5-2格子"）是两个不同语义的
                // 字段，故意不取同一个值——见fire()里的详细说明。
                .maxRange(50.0f)
                .hitEffect(ProjectileProfile.HitEffect.simple(stats.getBaseDamage(), stats.getInkRadius(), 0))
                .expireEffect(ProjectileProfile.ExpireEffect.dropInk())
                .visualConfig(ProjectileProfile.VisualConfig.texturedBox(null)) // 模型先空着，纯色方块占位
                .build();

        return new SteampunkGun(stats, profile);
    }

    /**
     * 获取武器信息描述
     */
    public String getInfo() {
        return String.format(
            "蒸汽朋克枪\n" +
            "━━━━━━━━━━━━━━━━\n" +
            "弹药消耗: %.0f/发\n" +
            "发射速度: %.1f秒/发\n" +
            "射程范围: 1.5-%.1f格子\n" +
            "散布角度: %.0f度扇形\n" +
            "伤害: %.0f\n" +
            "━━━━━━━━━━━━━━━━",
            stats.getAmmoCost(),
            stats.getFireRate(),
            stats.getMaxRange(),
            stats.getSpreadAngle(),
            stats.getBaseDamage()
        );
    }

    /**
     * 子弹生成监听器：SteampunkGun 本身不管理子弹的飞行/碰撞/涂墨，
     * 只负责在开火时创建 Projectile 并通知外部（PlayerController里的子弹更新循环）接管。
     */
    public interface ProjectileSpawnListener {
        void onProjectileSpawned(Projectile projectile);
    }
}
