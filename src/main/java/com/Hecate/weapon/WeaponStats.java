package com.Hecate.weapon;

import com.Hecate.player.inventory.PlayerStateManager;

/**
 * 武器属性配置类
 * 定义武器的各项参数
 */
public class WeaponStats {

    // 基础属性
    private final String weaponId;           // 武器ID
    private final String weaponName;         // 武器名称

    // 攻击属性（现在改为保存基础值，通过getter动态计算buff加成）
    private final float baseFireRate;        // 基础攻击间隔（秒）
    private final float projectileSpawnRate; // 发射子弹的速度（每次攻击发射的子弹数量/秒）
    private final float projectileVelocity;  // 射出的子弹的速度（米/秒）- 子弹飞行速度
    private final float baseSpreadAngle;     // 基础散射角度（度）
    private final float maxRange;            // 攻击距离（米）- 子弹最大飞行距离
    private final float ammoCost;            // 发射消耗的弹药（每次攻击）

    // 蓄力属性
    private final boolean hasCharge;         // 是否支持蓄力
    private final float maxChargeTime;       // 最大蓄力时间（秒）
    private final float chargeMultiplier;    // 蓄力伤害倍率（满蓄力时）

    // 伤害属性
    private final float baseDamage;          // 基础伤害
    private final float inkRadius;           // 涂墨半径（米）

    // 效果系统引用（用于动态计算buff加成）
    private PlayerStateManager playerStateManager;

    /**
     * 构造函数 - 使用Builder模式创建
     */
    private WeaponStats(Builder builder) {
        this.weaponId = builder.weaponId;
        this.weaponName = builder.weaponName;
        this.baseFireRate = builder.fireRate;
        this.projectileSpawnRate = builder.projectileSpawnRate;
        this.projectileVelocity = builder.projectileVelocity;
        this.baseSpreadAngle = builder.spreadAngle;
        this.maxRange = builder.maxRange;
        this.ammoCost = builder.ammoCost;
        this.hasCharge = builder.hasCharge;
        this.maxChargeTime = builder.maxChargeTime;
        this.chargeMultiplier = builder.chargeMultiplier;
        this.baseDamage = builder.baseDamage;
        this.inkRadius = builder.inkRadius;
        this.playerStateManager = null; // 默认为null，通过setPlayerStateManager设置
    }

    /**
     * 设置PlayerStateManager引用（用于动态计算buff加成）
     */
    public void setPlayerStateManager(PlayerStateManager manager) {
        this.playerStateManager = manager;
    }

    // Getters（动态计算buff加成）
    public String getWeaponId() { return weaponId; }
    public String getWeaponName() { return weaponName; }

    /**
     * 获取实际射速（基础值 * buff加成）
     */
    public float getFireRate() {
        float rate = baseFireRate;
        if (playerStateManager != null) {
            int stacks = playerStateManager.getEffectStacks("fire_rate_boost");
            float buffMultiplier = (float) Math.pow(1f / 1.5f, stacks); // 每层缩短到2/3，可叠加
            rate *= buffMultiplier;
        }
        return rate;
    }

    public float getProjectileSpawnRate() { return projectileSpawnRate; }
    public float getProjectileVelocity() { return projectileVelocity; }

    /**
     * 获取实际散射角度（基础值 * buff加成）
     */
    public float getSpreadAngle() {
        float angle = baseSpreadAngle;
        if (playerStateManager != null) {
            int stacks = playerStateManager.getEffectStacks("spread_range_boost");
            float buffMultiplier = (float) Math.pow(1.5f, stacks); // 每层1.5倍，可叠加
            angle *= buffMultiplier;
        }
        return angle;
    }

    public float getMaxRange() { return maxRange; }
    public float getAmmoCost() { return ammoCost; }
    public boolean hasCharge() { return hasCharge; }
    public float getMaxChargeTime() { return maxChargeTime; }
    public float getChargeMultiplier() { return chargeMultiplier; }
    public float getBaseDamage() { return baseDamage; }
    public float getInkRadius() { return inkRadius; }

    /**
     * @deprecated 不再需要直接修改值，现在通过效果系统动态计算
     */
    @Deprecated
    public void multiplyFireRate(float factor) {
        // 空实现，保留接口兼容性
    }

    /**
     * @deprecated 不再需要直接修改值，现在通过效果系统动态计算
     */
    @Deprecated
    public void multiplySpreadAngle(float factor) {
        // 空实现，保留接口兼容性
    }

    /**
     * Builder类 - 用于构建WeaponStats
     */
    public static class Builder {
        // 必需参数
        private final String weaponId;
        private final String weaponName;

        // 可选参数 - 设置默认值
        private float fireRate = 0.5f;              // 默认0.5秒攻击间隔
        private float projectileSpawnRate = 1.0f;   // 默认每次发射1个子弹
        private float projectileVelocity = 20.0f;   // 默认20米/秒
        private float spreadAngle = 0.0f;           // 默认无散射
        private float maxRange = 50.0f;             // 默认50米射程
        private float ammoCost = 100.0f;            // 默认消耗100弹药
        private boolean hasCharge = false;          // 默认不支持蓄力
        private float maxChargeTime = 0.0f;         // 默认无蓄力时间
        private float chargeMultiplier = 1.0f;      // 默认无蓄力倍率
        private float baseDamage = 10.0f;           // 默认10点伤害
        private float inkRadius = 1.0f;             // 默认1米涂墨半径

        public Builder(String weaponId, String weaponName) {
            this.weaponId = weaponId;
            this.weaponName = weaponName;
        }

        public Builder fireRate(float val) { fireRate = val; return this; }
        public Builder projectileSpawnRate(float val) { projectileSpawnRate = val; return this; }
        public Builder projectileVelocity(float val) { projectileVelocity = val; return this; }
        public Builder spreadAngle(float val) { spreadAngle = val; return this; }
        public Builder maxRange(float val) { maxRange = val; return this; }
        public Builder ammoCost(float val) { ammoCost = val; return this; }
        public Builder hasCharge(boolean val) { hasCharge = val; return this; }
        public Builder maxChargeTime(float val) { maxChargeTime = val; return this; }
        public Builder chargeMultiplier(float val) { chargeMultiplier = val; return this; }
        public Builder baseDamage(float val) { baseDamage = val; return this; }
        public Builder inkRadius(float val) { inkRadius = val; return this; }

        public WeaponStats build() {
            return new WeaponStats(this);
        }
    }

    @Override
    public String toString() {
        return String.format("Weapon[%s] - FireRate: %.2fs, Velocity: %.1fm/s, Range: %.1fm, AmmoCost: %.0f",
                weaponName, baseFireRate, projectileVelocity, maxRange, ammoCost);
    }
}
