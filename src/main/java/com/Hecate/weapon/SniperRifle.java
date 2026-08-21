package com.Hecate.weapon;

import com.Hecate.ink.SparseGridManager;
import com.Hecate.monster.MonsterManager;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;

/**
 * 狙击枪
 * 蓄力后单发发射，子弹沿几乎平直的抛物线飞行，射程内可穿透任意数量目标，
 * 沿途在地面留下墨水痕迹。不蓄力时伤害最低（80），蓄力越久伤害越高，
 * 满蓄力（2秒）时伤害最高（180）。蓄力时间达到上限后会安全封顶，
 * 持续按住左键不会出错，但仍需松开鼠标才会真正发射（继承父类Weapon的蓄力/释放语义）。
 */
public class SniperRifle extends Weapon {

    // 不蓄力时的伤害（chargeRatio=0）
    private static final float MIN_DAMAGE = 80.0f;
    // 满蓄力时的伤害（chargeRatio=1）
    private static final float MAX_DAMAGE = 180.0f;

    // 子弹配置（弹道+命中效果+视觉）
    private final ProjectileProfile projectileProfile;

    // 依赖项（子弹的实际飞行/碰撞/涂墨由外部的子弹更新循环驱动，这里只负责生成Projectile）
    private SparseGridManager gridManager;
    private MonsterManager monsterManager;
    private Node worldNode;
    private int playerFactionId = com.Hecate.ink.FactionRegistry.DARK_DEFAULT;  // 玩家阵营ID

    // 每次开火产生的子弹交给外部监听器处理（PlayerController里的子弹更新循环）
    private ProjectileSpawnListener spawnListener;

    public SniperRifle(WeaponStats stats, ProjectileProfile projectileProfile) {
        super(stats, WeaponKind.SNIPER);
        this.projectileProfile = projectileProfile;
    }

    public void setGridManager(SparseGridManager gridManager) {
        this.gridManager = gridManager;
    }

    public void setMonsterManager(MonsterManager monsterManager) {
        this.monsterManager = monsterManager;
    }

    public void setWorldNode(Node worldNode) {
        this.worldNode = worldNode;
    }

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
     * 不支持不蓄力的普通开火（狙击枪必须蓄力才能开火）。
     * PlayerController.performGunAttack() 已经保证 hasCharge()==true 时只会走
     * startCharge()/releaseCharge() 路径，不会调用到这里；此方法仅用于满足抽象方法签名。
     */
    @Override
    protected void fire(Vector3f origin, Vector3f direction) {
        fireBullet(origin, direction, 0f);
    }

    /**
     * 蓄力攻击：damageMultiplier 由父类 Weapon.releaseCharge() 按线性插值算出，
     * 但父类的插值公式（1 + (chargeMultiplier-1)*chargeRatio）不满足"基础伤害-100"
     * 的需求，所以这里不使用传入的 damageMultiplier，而是直接用 getChargeProgress()
     * 重新按 MIN_DAMAGE~MAX_DAMAGE 计算伤害。
     */
    @Override
    protected void fireCharged(Vector3f origin, Vector3f direction, float damageMultiplier) {
        fireBullet(origin, direction, getChargeProgress());
    }

    private void fireBullet(Vector3f origin, Vector3f direction, float chargeRatio) {
        float damage = MIN_DAMAGE + (MAX_DAMAGE - MIN_DAMAGE) * chargeRatio;

        ProjectileProfile.HitEffect scaledHitEffect = ProjectileProfile.HitEffect.piercing(
                damage,
                projectileProfile.getHitEffect().inkRadius,
                playerFactionId,  // 使用玩家阵营ID而非team
                projectileProfile.getHitEffect().pierceCount
        );
        ProjectileProfile shotProfile = new ProjectileProfile.Builder(projectileProfile.getId(), projectileProfile.getDisplayName())
                .arcType(projectileProfile.getArcType())
                .velocity(projectileProfile.getVelocity())
                .gravity(projectileProfile.getGravity())
                .drag(projectileProfile.getDrag())
                .maxLifetime(projectileProfile.getMaxLifetime())
                .maxRange(projectileProfile.getMaxRange())
                .hitEffect(scaledHitEffect)
                .expireEffect(projectileProfile.getExpireEffect())
                .paintAlongPath(projectileProfile.isPaintAlongPath())
                .pathPaintInterval(projectileProfile.getPathPaintInterval())
                .visualConfig(projectileProfile.getVisualConfig())
                .build();

        Projectile projectile = new Projectile(shotProfile, origin, direction, 1.0f, playerFactionId);

        if (spawnListener != null) {
            spawnListener.onProjectileSpawned(projectile);
        }
    }

    /**
     * 创建默认的狙击枪实例（gun2命令用）。
     * <p>数值：不蓄力开火间隔0.7秒，最长蓄力2秒，射程6格（6米），
     * 沿途每0.15秒在飞行路径上涂一次墨。
     */
    public static SniperRifle create() {
        WeaponStats stats = new WeaponStats.Builder("sniper_rifle", "狙击枪")
                .fireRate(0.7f)              // 不蓄力时最低开火间隔0.7秒
                .projectileVelocity(40.0f)   // 40米/秒初速度（配合小重力实现近似平直的抛物线）
                .maxRange(6.0f)              // 6格射程（6米）
                .ammoCost(10.0f)             // 每发消耗10点弹药
                .hasCharge(true)
                .maxChargeTime(2.0f)         // 最长蓄力2秒
                .chargeMultiplier(MAX_DAMAGE / MIN_DAMAGE) // 仅用于getChargeProgress的父类逻辑一致性，实际伤害由fireBullet重新计算
                .baseDamage(MIN_DAMAGE)
                .inkRadius(0.4f)             // 沿途/命中涂墨半径
                .build();

        ProjectileProfile profile = new ProjectileProfile.Builder("sniper_bullet", "狙击枪弹")
                .arcType(ProjectileProfile.ArcType.BALLISTIC)
                .velocity(stats.getProjectileVelocity())
                .gravity(-2.0f)              // 极小重力，6格射程内几乎看不出下坠，但仍是真正的抛物线
                .drag(1.0f)                  // 不额外衰减速度（保持平直）
                .maxLifetime(3.0f)
                .maxRange(stats.getMaxRange())
                // 穿透次数用一个足够大的值代表"射程内能穿多少个目标都行"，
                // 子弹真正的终止条件是maxRange/maxLifetime，而不是穿透次数耗尽。
                .hitEffect(ProjectileProfile.HitEffect.piercing(MIN_DAMAGE, stats.getInkRadius(), 0, 999))
                .expireEffect(ProjectileProfile.ExpireEffect.none())
                .paintAlongPath(true)
                .pathPaintInterval(0.15f)    // 沿途每0.15秒留一次墨痕
                .visualConfig(ProjectileProfile.VisualConfig.texturedBox(null)) // 模型先空着，纯色方块占位
                .build();

        return new SniperRifle(stats, profile);
    }

    /**
     * 子弹生成监听器：SniperRifle 本身不管理子弹的飞行/碰撞/涂墨，
     * 只负责在开火时创建 Projectile 并通知外部（PlayerController里的子弹更新循环）接管。
     */
    public interface ProjectileSpawnListener {
        void onProjectileSpawned(Projectile projectile);
    }
}
