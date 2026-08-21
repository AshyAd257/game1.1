package com.Hecate.ink;

import com.jme3.math.Vector3f;

/**
 * 涂墨武器接口
 *
 * 【设计目标】
 * - 为玩家武器和敌人涂墨武器提供统一接口
 * - 支持复用现有的涂墨系统代码（FlameParticle、Projectile）
 * - 解耦武器实现和涂墨系统
 *
 * 【使用场景】
 * 1. 玩家武器：Gun1 (FlameParticle)、Gun2 (Projectile)
 * 2. 敌人武器：EnemyFlameWeapon、EnemyProjectileWeapon
 * 3. 特殊机关：陷阱、喷泉、环境涂墨器
 *
 * @author Hecate Team
 * @since 2026-08-20
 */
public interface InkWeaponInterface {

    /**
     * 获取涂墨阵营ID
     * @return 阵营ID（对应 FactionRegistry 中的注册）
     */
    int getFactionId();

    /**
     * 设置涂墨阵营ID
     * @param factionId 阵营ID
     */
    void setFactionId(int factionId);

    /**
     * 获取涂墨半径
     * @return 半径（世界单位，米）
     */
    float getInkRadius();

    /**
     * 设置涂墨半径
     * @param radius 半径（世界单位，米）
     */
    void setInkRadius(float radius);

    /**
     * 触发涂墨
     * @param position 涂墨位置（世界坐标）
     * @param gridManager 网格管理器
     */
    void triggerInk(Vector3f position, SparseGridManager gridManager);

    /**
     * 获取武器类型标识
     * @return 武器类型（如 "flame", "projectile", "trap"）
     */
    String getWeaponType();

    /**
     * 是否启用涂墨功能
     * @return true = 启用，false = 禁用
     */
    boolean isInkEnabled();

    /**
     * 设置是否启用涂墨功能
     * @param enabled true = 启用，false = 禁用
     */
    void setInkEnabled(boolean enabled);
}
