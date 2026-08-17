package com.Hecate.weapon;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;

/**
 * 基础射击武器（示例实现）
 * 发射直线飞行的子弹
 */
public class BasicShooter extends Weapon {

    /**
     * 构造函数
     */
    public BasicShooter(WeaponStats stats) {
        super(stats);
    }

    /**
     * 执行普通开火
     */
    @Override
    protected void fire(Vector3f origin, Vector3f direction) {
        // 计算散射
        Vector3f finalDirection = applySpread(direction.clone());

        // TODO: 创建子弹实体
        // 这里需要集成到游戏的子弹系统中
    }

    /**
     * 执行蓄力攻击
     */
    @Override
    protected void fireCharged(Vector3f origin, Vector3f direction, float damageMultiplier) {
        // 蓄力攻击通常散射更小
        Vector3f finalDirection = applySpread(direction.clone(), 0.5f);

        // TODO: 创建强化子弹实体
    }

    /**
     * 应用散射
     * @param direction 原始方向
     * @return 应用散射后的方向
     */
    private Vector3f applySpread(Vector3f direction) {
        return applySpread(direction, 1.0f);
    }

    /**
     * 应用散射
     * @param direction 原始方向
     * @param spreadMultiplier 散射倍率（0-1，用于蓄力攻击减少散射）
     * @return 应用散射后的方向
     */
    private Vector3f applySpread(Vector3f direction, float spreadMultiplier) {
        if (stats.getSpreadAngle() <= 0) {
            return direction.normalizeLocal();
        }

        // 计算实际散射角度
        float actualSpread = stats.getSpreadAngle() * spreadMultiplier;

        // 在散射范围内随机偏移
        float randomAngleX = (FastMath.nextRandomFloat() - 0.5f) * 2.0f * actualSpread * FastMath.DEG_TO_RAD;
        float randomAngleY = (FastMath.nextRandomFloat() - 0.5f) * 2.0f * actualSpread * FastMath.DEG_TO_RAD;

        // 应用旋转
        Vector3f result = direction.clone();

        // 绕Y轴旋转（水平散射）
        float cosY = FastMath.cos(randomAngleY);
        float sinY = FastMath.sin(randomAngleY);
        float newX = result.x * cosY - result.z * sinY;
        float newZ = result.x * sinY + result.z * cosY;
        result.x = newX;
        result.z = newZ;

        // 绕X轴旋转（垂直散射）
        float cosX = FastMath.cos(randomAngleX);
        float sinX = FastMath.sin(randomAngleX);
        float newY = result.y * cosX - result.z * sinX;
        newZ = result.y * sinX + result.z * cosX;
        result.y = newY;
        result.z = newZ;

        return result.normalizeLocal();
    }

    /**
     * 创建默认武器配置
     */
    public static BasicShooter createDefault() {
        WeaponStats stats = new WeaponStats.Builder("basic_shooter", "基础射击器")
                .fireRate(0.5f)              // 0.5秒攻击间隔
                .projectileVelocity(20.0f)   // 20米/秒子弹速度
                .spreadAngle(5.0f)           // 5度散射角
                .maxRange(50.0f)             // 50米射程
                .ammoCost(100.0f)            // 消耗100弹药（可射10发）
                .baseDamage(10.0f)           // 10点伤害
                .inkRadius(1.0f)             // 1米涂墨半径
                .build();

        return new BasicShooter(stats);
    }
}
