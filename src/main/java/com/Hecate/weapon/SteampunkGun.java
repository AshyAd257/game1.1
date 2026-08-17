package com.Hecate.weapon;

import com.Hecate.flame.SimpleFlameRenderer;
import com.Hecate.ink.SparseGridManager;
import com.jme3.math.FastMath;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.collision.CollisionResults;

/**
 * 蒸汽朋克枪
 * 中距离散射武器，每次攻击消耗3点弹药
 * 在60度扇形范围内散布子弹
 */
public class SteampunkGun extends Weapon {

    // 依赖项
    private SimpleFlameRenderer flameRenderer;  // 火焰渲染器（用于发射子弹视觉效果）
    private SparseGridManager gridManager;      // 墨水系统
    private Node worldNode;                     // 世界节点（用于射线检测）
    private int playerTeam = 0;                 // 玩家队伍

    /**
     * 构造函数
     */
    public SteampunkGun(WeaponStats stats) {
        super(stats);
    }

    /**
     * 设置火焰渲染器（用于子弹效果）
     */
    public void setFlameRenderer(SimpleFlameRenderer flameRenderer) {
        this.flameRenderer = flameRenderer;
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
     * 设置玩家队伍
     */
    public void setPlayerTeam(int team) {
        this.playerTeam = team;
    }

    /**
     * 执行普通开火
     */
    @Override
    protected void fire(Vector3f origin, Vector3f direction) {
        // 应用60度扇形散射
        Vector3f finalDirection = applySpread(direction.clone());

        // 发射视觉效果（墨水子弹粒子）
        if (flameRenderer != null) {
            // 计算子弹速度
            float bulletSpeed = stats.getProjectileVelocity();
            Vector3f bulletVelocity = finalDirection.mult(bulletSpeed);

            // 发射少量粒子作为子弹（类似火焰，但更少的粒子）
            int particleCount = 5; // 少量粒子代表一发子弹
            flameRenderer.emitFlame(origin, particleCount, bulletVelocity);
        }

        // 粒子落地后会自动触发涂墨（FlameParticle系统自带）
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
     * 应用60度扇形散射
     * @param direction 原始方向
     * @return 应用散射后的方向
     */
    private Vector3f applySpread(Vector3f direction) {
        float spreadAngle = stats.getSpreadAngle();

        if (spreadAngle <= 0) {
            return direction.normalizeLocal();
        }

        // 在60度扇形范围内随机散射
        // 水平方向：-30度 到 +30度
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
     * - 射程：1.5-2个格子（每个格子按1米计算）
     * - 散布：60度扇形
     */
    public static SteampunkGun create() {
        WeaponStats stats = new WeaponStats.Builder("steampunk_gun", "蒸汽朋克枪")
                .fireRate(0.5f)              // 0.5秒攻击间隔（可连发）
                .projectileVelocity(15.0f)   // 15米/秒子弹速度（中等速度）
                .spreadAngle(60.0f)          // 60度散射角（扇形散布）
                .maxRange(2.0f)              // 2格子射程（2米最远）
                .ammoCost(3.0f)              // 每发消耗3点弹药
                .baseDamage(8.0f)            // 8点基础伤害
                .inkRadius(0.8f)             // 0.8米涂墨半径
                .hasCharge(false)            // 不支持蓄力
                .build();

        return new SteampunkGun(stats);
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
}
