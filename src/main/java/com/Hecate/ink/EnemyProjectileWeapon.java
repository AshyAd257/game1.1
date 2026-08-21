package com.Hecate.ink;

import com.jme3.math.Vector3f;

/**
 * 敌人弹道武器
 *
 * 【设计说明】
 * 复用 Gun2 的 Projectile 涂墨逻辑
 * 发射弹道，沿轨迹持续涂墨
 *
 * 【使用场景】
 * - 远程敌人
 * - 狙击敌人
 * - 导弹发射器
 *
 * @author Hecate Team
 * @since 2026-08-20
 */
public class EnemyProjectileWeapon extends BaseEnemyInkWeapon {

    // 弹道预设配置
    private InkHelper.ProjectilePreset projectilePreset;

    // 武器参数
    private float cooldown = 0.5f;    // 冷却时间
    private float range = 6.0f;       // 射程

    /**
     * 构造函数（使用默认配置）
     * @param factionId 阵营ID
     */
    public EnemyProjectileWeapon(int factionId) {
        super(factionId, "projectile");
        this.projectilePreset = new InkHelper.ProjectilePreset();
        this.inkRadius = 0.4f; // 弹道涂墨半径
    }

    /**
     * 构造函数（自定义配置）
     * @param factionId 阵营ID
     * @param preset 弹道预设
     */
    public EnemyProjectileWeapon(int factionId, InkHelper.ProjectilePreset preset) {
        super(factionId, "projectile");
        this.projectilePreset = preset;
        this.inkRadius = preset.radius;
    }

    @Override
    public void fire(Vector3f origin, Vector3f direction, SparseGridManager gridManager) {
        if (!inkEnabled || gridManager == null) {
            return;
        }

        // 计算终点位置
        Vector3f endPos = origin.add(direction.normalize().mult(range));

        // 复用 InkHelper 的弹道轨迹逻辑
        InkHelper.inkProjectileTrail(origin, endPos, factionId, gridManager, projectilePreset);
    }

    @Override
    public float getCooldown() {
        return cooldown;
    }

    @Override
    public float getRange() {
        return range;
    }

    // ===== Getters & Setters =====

    public void setCooldown(float cooldown) {
        this.cooldown = cooldown;
    }

    public void setRange(float range) {
        this.range = range;
    }

    public InkHelper.ProjectilePreset getProjectilePreset() {
        return projectilePreset;
    }

    public void setProjectilePreset(InkHelper.ProjectilePreset preset) {
        this.projectilePreset = preset;
        this.inkRadius = preset.radius;
    }
}
