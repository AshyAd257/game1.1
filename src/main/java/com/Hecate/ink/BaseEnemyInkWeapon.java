package com.Hecate.ink;

import com.jme3.math.Vector3f;

/**
 * 敌人涂墨武器基类
 *
 * 【设计目标】
 * - 为敌人AI提供开箱即用的涂墨武器实现
 * - 复用玩家武器逻辑（FlameParticle、Projectile）
 * - 支持自定义涂墨策略
 *
 * 【继承关系】
 * BaseEnemyInkWeapon (抽象基类)
 *   ├── EnemyFlameWeapon (火焰喷射器)
 *   ├── EnemyProjectileWeapon (远程弹道武器)
 *   └── EnemyAreaWeapon (区域涂墨器)
 *
 * 【使用示例】
 * <pre>
 * // 创建敌人火焰武器
 * EnemyFlameWeapon weapon = new EnemyFlameWeapon(FactionRegistry.DARK_DEFAULT);
 * weapon.setInkRadius(0.6f);
 *
 * // 敌人攻击时触发涂墨
 * weapon.fire(enemyPosition, targetDirection, gridManager);
 * </pre>
 *
 * @author Hecate Team
 * @since 2026-08-20
 */
public abstract class BaseEnemyInkWeapon implements InkWeaponInterface {

    // 涂墨阵营ID
    protected int factionId;

    // 涂墨半径
    protected float inkRadius;

    // 是否启用涂墨
    protected boolean inkEnabled;

    // 武器类型标识
    protected String weaponType;

    /**
     * 构造函数
     * @param factionId 阵营ID
     * @param weaponType 武器类型
     */
    public BaseEnemyInkWeapon(int factionId, String weaponType) {
        this.factionId = factionId;
        this.weaponType = weaponType;
        this.inkRadius = 0.5f; // 默认半径
        this.inkEnabled = true;
    }

    // ===== InkWeaponInterface 实现 =====

    @Override
    public int getFactionId() {
        return factionId;
    }

    @Override
    public void setFactionId(int factionId) {
        this.factionId = factionId;
    }

    @Override
    public float getInkRadius() {
        return inkRadius;
    }

    @Override
    public void setInkRadius(float radius) {
        this.inkRadius = radius;
    }

    @Override
    public String getWeaponType() {
        return weaponType;
    }

    @Override
    public boolean isInkEnabled() {
        return inkEnabled;
    }

    @Override
    public void setInkEnabled(boolean enabled) {
        this.inkEnabled = enabled;
    }

    @Override
    public void triggerInk(Vector3f position, SparseGridManager gridManager) {
        if (!inkEnabled || gridManager == null) {
            return;
        }
        InkHelper.inkPoint(position, inkRadius, factionId, gridManager);
    }

    // ===== 抽象方法 =====

    /**
     * 开火（子类实现具体逻辑）
     * @param origin 发射起点
     * @param direction 发射方向
     * @param gridManager 网格管理器
     */
    public abstract void fire(Vector3f origin, Vector3f direction, SparseGridManager gridManager);

    /**
     * 获取武器冷却时间（秒）
     * @return 冷却时间
     */
    public abstract float getCooldown();

    /**
     * 获取武器射程
     * @return 射程（米）
     */
    public abstract float getRange();
}
