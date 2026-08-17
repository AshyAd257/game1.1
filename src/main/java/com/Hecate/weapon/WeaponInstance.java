package com.Hecate.weapon;

/**
 * 武器实例 - 运行时状态
 * 每个玩家/NPC持有的武器都是一个独立实例
 *
 * 职责：
 * 1. 存储会变化的状态（弹药、蓄力、冷却）
 * 2. 引用不可变的定义（def）
 * 3. 不包含行为逻辑（逻辑由WeaponSystem处理）
 */
public class WeaponInstance {

    // 武器定义（不可变引用）
    private final WeaponDefinition def;

    // 运行时状态（可变）
    private float ammo;                 // 当前弹药量
    private float charge;               // 蓄力进度（0-1）
    private float lastFireTime;         // 上次开火时间戳（秒）
    private boolean scoped;             // 是否在瞄准状态

    // 装备状态
    private boolean equipped;           // 是否被装备
    private String ownerEntityId;       // 所有者ID（玩家/NPC）

    // 修饰符（运行时属性增益/减益）
    private WeaponModifiers modifiers;  // 临时修饰符（buff/debuff）

    /**
     * 构造函数
     * @param def 武器定义（静态数据）
     */
    public WeaponInstance(WeaponDefinition def) {
        this.def = def;
        this.ammo = def.getAmmoMax();       // 初始满弹
        this.charge = 0.0f;
        this.lastFireTime = 0.0f;
        this.scoped = false;
        this.equipped = false;
        this.ownerEntityId = null;
        this.modifiers = new WeaponModifiers();
    }

    /**
     * 检查是否可以开火
     * @param currentTime 当前时间
     * @param fireRateMod 射速修饰符（1.0=正常，2.0=两倍速）
     * @return true如果可以开火
     */
    public boolean canFire(float currentTime, float fireRateMod) {
        // 检查弹药
        if (ammo < def.getAmmoPerShot()) {
            return false;
        }

        // 检查冷却时间
        float fireRate = getEffectiveFireRate(fireRateMod);
        float timeSinceLastFire = currentTime - lastFireTime;
        return timeSinceLastFire >= fireRate;
    }

    /**
     * 消耗弹药
     * @return 是否成功消耗
     */
    public boolean consumeAmmo() {
        if (ammo >= def.getAmmoPerShot()) {
            ammo -= def.getAmmoPerShot();
            return true;
        }
        return false;
    }

    /**
     * 装填弹药
     * @param amount 装填数量
     */
    public void reload(float amount) {
        ammo = Math.min(ammo + amount, def.getAmmoMax());
    }

    /**
     * 更新蓄力进度
     * @param deltaTime 时间增量
     * @param chargeRate 蓄力速度（秒）
     */
    public void updateCharge(float deltaTime, float chargeRate) {
        if (def.getFireMode() == WeaponDefinition.FireMode.CHARGE) {
            charge = Math.min(charge + deltaTime / chargeRate, 1.0f);
        }
    }

    /**
     * 释放蓄力（重置为0）
     */
    public void releaseCharge() {
        charge = 0.0f;
    }

    /**
     * 记录开火时间
     * @param time 当前时间
     */
    public void recordFireTime(float time) {
        this.lastFireTime = time;
    }

    /**
     * 获取有效射速（应用修饰符后）
     */
    private float getEffectiveFireRate(float fireRateMod) {
        // 从params获取基础射速
        Float baseFireRate = def.getParam("fireRate", 0.5f);
        return baseFireRate / fireRateMod;  // 修饰符越大，射速越快
    }

    // ==================== Getters & Setters ====================

    public WeaponDefinition getDef() { return def; }
    public float getAmmo() { return ammo; }
    public float getAmmoRatio() { return ammo / def.getAmmoMax(); }
    public float getCharge() { return charge; }
    public float getLastFireTime() { return lastFireTime; }
    public boolean isScoped() { return scoped; }
    public void setScoped(boolean scoped) { this.scoped = scoped; }
    public boolean isEquipped() { return equipped; }
    public void setEquipped(boolean equipped) { this.equipped = equipped; }
    public String getOwnerEntityId() { return ownerEntityId; }
    public void setOwnerEntityId(String ownerEntityId) { this.ownerEntityId = ownerEntityId; }
    public WeaponModifiers getModifiers() { return modifiers; }

    /**
     * 武器修饰符（临时属性增益）
     */
    public static class WeaponModifiers {
        public float fireRateMultiplier = 1.0f;    // 射速倍率
        public float damageMultiplier = 1.0f;      // 伤害倍率
        public float spreadMultiplier = 1.0f;      // 散射倍率
        public float rangeMultiplier = 1.0f;       // 射程倍率

        public void reset() {
            fireRateMultiplier = 1.0f;
            damageMultiplier = 1.0f;
            spreadMultiplier = 1.0f;
            rangeMultiplier = 1.0f;
        }
    }

    @Override
    public String toString() {
        return String.format("WeaponInstance[%s, ammo=%.0f/%.0f, charge=%.2f, equipped=%s]",
                def.getDisplayName(), ammo, (float)def.getAmmoMax(), charge, equipped);
    }
}
