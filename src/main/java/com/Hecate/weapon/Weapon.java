package com.Hecate.weapon;

import com.Hecate.player.PlayerAmmo;
import com.jme3.math.Vector3f;

/**
 * 武器基类
 * 所有武器的抽象基类，定义武器的通用行为
 */
public abstract class Weapon {

    // 武器属性
    protected final WeaponStats stats;

    // 武器种类（步枪/狙击枪/.../战锤）。null表示未分类——现有的BasicShooter/
    // SteampunkGun/FlameWeapon都是历史遗留武器，通过单参数构造函数创建，kind为null。
    // PlayerController据此字段区分远程/近战分支（而不是instanceof判断），
    // kind为null时按远程武器处理，与现状完全一致。
    protected final WeaponKind kind;

    // 武器状态
    protected float timeSinceLastFire;       // 距离上次开火的时间
    protected float currentChargeTime = 0f;  // 当前蓄力时间
    protected boolean isCharging = false;    // 是否正在蓄力

    /**
     * 构造函数（不指定种类，兼容现有远程武器子类）
     */
    public Weapon(WeaponStats stats) {
        this(stats, null);
    }

    /**
     * 构造函数（指定武器种类）
     */
    public Weapon(WeaponStats stats, WeaponKind kind) {
        this.stats = stats;
        this.kind = kind;
        // 初始化为已冷却状态，允许立即开火
        this.timeSinceLastFire = stats.getFireRate();
    }

    /**
     * 更新武器状态
     * @param tpf 时间增量
     */
    public void update(float tpf) {
        // 更新开火冷却
        if (timeSinceLastFire < stats.getFireRate()) {
            timeSinceLastFire += tpf;
        }

        // 更新蓄力
        if (isCharging && stats.hasCharge()) {
            currentChargeTime += tpf;
            if (currentChargeTime > stats.getMaxChargeTime()) {
                currentChargeTime = stats.getMaxChargeTime();
            }
        }
    }

    /**
     * 尝试开火
     * @param ammo 弹药系统
     * @param origin 发射起点
     * @param direction 发射方向
     * @return true 如果成功开火
     */
    public boolean tryFire(PlayerAmmo ammo, Vector3f origin, Vector3f direction) {
        // 检查冷却时间
        if (timeSinceLastFire < stats.getFireRate()) {
            return false;
        }

        // 检查弹药
        if (!ammo.hasAmmo(stats.getAmmoCost())) {
            return false;
        }

        // 消耗弹药
        if (!ammo.consume(stats.getAmmoCost())) {
            return false;
        }

        // 执行开火
        fire(origin, direction);

        // 重置冷却
        timeSinceLastFire = 0f;

        return true;
    }

    /**
     * 开始蓄力
     */
    public void startCharge() {
        if (stats.hasCharge()) {
            isCharging = true;
            currentChargeTime = 0f;
        }
    }

    /**
     * 释放蓄力攻击
     * @param ammo 弹药系统
     * @param origin 发射起点
     * @param direction 发射方向
     * @return true 如果成功释放
     */
    public boolean releaseCharge(PlayerAmmo ammo, Vector3f origin, Vector3f direction) {
        if (!stats.hasCharge() || !isCharging) {
            return false;
        }

        // 检查弹药
        if (!ammo.hasAmmo(stats.getAmmoCost())) {
            isCharging = false;
            currentChargeTime = 0f;
            return false;
        }

        // 消耗弹药
        if (!ammo.consume(stats.getAmmoCost())) {
            isCharging = false;
            currentChargeTime = 0f;
            return false;
        }

        // 计算蓄力倍率
        float chargeRatio = currentChargeTime / stats.getMaxChargeTime();
        float damageMultiplier = 1.0f + (stats.getChargeMultiplier() - 1.0f) * chargeRatio;

        // 执行蓄力攻击
        fireCharged(origin, direction, damageMultiplier);

        // 重置状态
        isCharging = false;
        currentChargeTime = 0f;
        timeSinceLastFire = 0f;

        return true;
    }

    /**
     * 取消蓄力
     */
    public void cancelCharge() {
        isCharging = false;
        currentChargeTime = 0f;
    }

    /**
     * 执行普通开火（由子类实现）
     * @param origin 发射起点
     * @param direction 发射方向
     */
    protected abstract void fire(Vector3f origin, Vector3f direction);

    /**
     * 执行蓄力攻击（由子类实现）
     * @param origin 发射起点
     * @param direction 发射方向
     * @param damageMultiplier 伤害倍率
     */
    protected abstract void fireCharged(Vector3f origin, Vector3f direction, float damageMultiplier);

    // Getters
    public WeaponStats getStats() { return stats; }
    public WeaponKind getKind() { return kind; }

    /**
     * 是否为近战武器。kind未设置（历史遗留远程武器）时返回false，
     * 与"远程/近战二分、默认远程"的现状保持一致。
     */
    public boolean isMelee() {
        return kind != null && kind.isMelee();
    }

    public boolean canFire() { return timeSinceLastFire >= stats.getFireRate(); }
    public boolean isCharging() { return isCharging; }
    public float getChargeProgress() {
        return stats.hasCharge() ? (currentChargeTime / stats.getMaxChargeTime()) : 0f;
    }
}
