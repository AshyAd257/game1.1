package com.Hecate.ink;

import com.jme3.math.Vector3f;

/**
 * 敌人区域涂墨武器
 *
 * 【设计说明】
 * 在指定位置生成区域涂墨效果
 * 可用于爆炸、陷阱、技能AOE等
 *
 * 【使用场景】
 * - 自爆敌人（死亡时涂墨）
 * - 地雷陷阱
 * - 毒雾效果
 * - Boss技能
 *
 * @author Hecate Team
 * @since 2026-08-20
 */
public class EnemyAreaWeapon extends BaseEnemyInkWeapon {

    // 区域预设配置
    private InkHelper.AreaPreset areaPreset;

    // 武器参数
    private float cooldown = 3.0f;    // 冷却时间
    private float range = 0f;         // 射程（0表示自身位置）

    // 是否点燃
    private boolean shouldIgnite = false;

    /**
     * 构造函数（使用默认配置）
     * @param factionId 阵营ID
     */
    public EnemyAreaWeapon(int factionId) {
        super(factionId, "area");
        this.areaPreset = new InkHelper.AreaPreset();
        this.inkRadius = 1.0f; // 区域半径
    }

    /**
     * 构造函数（自定义配置）
     * @param factionId 阵营ID
     * @param preset 区域预设
     */
    public EnemyAreaWeapon(int factionId, InkHelper.AreaPreset preset) {
        super(factionId, "area");
        this.areaPreset = preset;
        this.inkRadius = preset.radius;
        this.shouldIgnite = preset.ignite;
    }

    @Override
    public void fire(Vector3f origin, Vector3f direction, SparseGridManager gridManager) {
        if (!inkEnabled || gridManager == null) {
            return;
        }

        // 区域涂墨
        if (shouldIgnite) {
            InkHelper.inkAndIgniteArea(origin, inkRadius, factionId, gridManager);
        } else {
            InkHelper.inkArea(origin, inkRadius, factionId, gridManager);
        }
    }

    /**
     * 触发区域涂墨（无需方向参数）
     * @param position 位置
     * @param gridManager 网格管理器
     */
    public void fireAt(Vector3f position, SparseGridManager gridManager) {
        fire(position, Vector3f.UNIT_Y, gridManager);
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

    public boolean isShouldIgnite() {
        return shouldIgnite;
    }

    public void setShouldIgnite(boolean shouldIgnite) {
        this.shouldIgnite = shouldIgnite;
    }

    public InkHelper.AreaPreset getAreaPreset() {
        return areaPreset;
    }

    public void setAreaPreset(InkHelper.AreaPreset preset) {
        this.areaPreset = preset;
        this.inkRadius = preset.radius;
        this.shouldIgnite = preset.ignite;
    }
}
