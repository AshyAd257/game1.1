package com.Hecate.ink;

import com.jme3.math.Vector3f;

/**
 * 敌人火焰武器
 *
 * 【设计说明】
 * 复用 Gun1 的 FlameParticle 涂墨逻辑
 * 发射多个火焰粒子，在落地点涂墨
 *
 * 【使用场景】
 * - 火焰敌人
 * - 火焰炮塔
 * - 火焰陷阱
 *
 * @author Hecate Team
 * @since 2026-08-20
 */
public class EnemyFlameWeapon extends BaseEnemyInkWeapon {

    // 火焰预设配置
    private InkHelper.FlamePreset flamePreset;

    // 武器参数
    private float cooldown = 1.0f;    // 冷却时间
    private float range = 3.0f;       // 射程

    /**
     * 构造函数（使用默认配置）
     * @param factionId 阵营ID
     */
    public EnemyFlameWeapon(int factionId) {
        super(factionId, "flame");
        this.flamePreset = new InkHelper.FlamePreset();
        this.inkRadius = 0.5f; // 火焰落点半径
    }

    /**
     * 构造函数（自定义配置）
     * @param factionId 阵营ID
     * @param preset 火焰预设
     */
    public EnemyFlameWeapon(int factionId, InkHelper.FlamePreset preset) {
        super(factionId, "flame");
        this.flamePreset = preset;
        this.inkRadius = preset.radius;
    }

    @Override
    public void fire(Vector3f origin, Vector3f direction, SparseGridManager gridManager) {
        if (!inkEnabled || gridManager == null) {
            return;
        }

        // 复用 InkHelper 的火焰散射逻辑
        InkHelper.inkFlamePattern(origin, direction, factionId, gridManager, flamePreset);
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

    public InkHelper.FlamePreset getFlamePreset() {
        return flamePreset;
    }

    public void setFlamePreset(InkHelper.FlamePreset preset) {
        this.flamePreset = preset;
        this.inkRadius = preset.radius;
    }
}
