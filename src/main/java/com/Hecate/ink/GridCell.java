package com.Hecate.ink;

/**
 * 网格单元
 * 表示地面上0.2m x 0.2m的一个格子
 *
 * 【重构说明】
 * - 移除 State 枚举，使用 factionId 表示客观归属
 * - 颜色由 ColorResolver 根据观察者决定，不在此绑定
 */
public class GridCell {

    // 阵营ID（客观归属，查表用的key）
    // NONE(0) = 空地，其他值对应 FactionRegistry 中的阵营
    private int factionId;

    // 时间戳（用于墨水消退、点燃持续时间等）
    private float timestamp;

    // 墨水强度（0.0-1.0，用于渐变效果和衰减）
    private float intensity;

    // 点燃状态（点燃后10秒降级为普通涂墨）
    private boolean ignited;

    public GridCell() {
        this.factionId = FactionRegistry.NONE;
        this.timestamp = 0f;
        this.intensity = 0f;
        this.ignited = false;
    }

    /**
     * 涂墨
     * @param factionId 阵营ID
     * @param currentTime 当前时间
     */
    public void ink(int factionId, float currentTime) {
        this.factionId = factionId;
        this.timestamp = currentTime;
        this.intensity = 1.0f;
        this.ignited = false; // 重新涂墨时清除点燃状态
    }

    /**
     * 点燃
     * @param currentTime 当前时间
     * @return 是否成功点燃（只有已涂墨的格子才能点燃）
     */
    public boolean ignite(float currentTime) {
        // 只有已涂墨的格子（非空地）才能点燃
        if (factionId != FactionRegistry.NONE && !ignited) {
            ignited = true;
            timestamp = currentTime;
            intensity = 1.0f;
            return true;
        }
        return false;
    }

    /**
     * 清空网格
     */
    public void clear() {
        this.factionId = FactionRegistry.NONE;
        this.timestamp = 0f;
        this.intensity = 0f;
        this.ignited = false;
    }

    /**
     * 更新网格（处理墨水消退、点燃持续时间等）
     * @param currentTime 当前时间
     * @param inkDecayTime 墨水消退时间（秒）
     * @param igniteDecayTime 点燃持续时间（秒）
     */
    public void update(float currentTime, float inkDecayTime, float igniteDecayTime) {
        float elapsed = currentTime - timestamp;

        if (elapsed < 0) {
            timestamp = currentTime;
            elapsed = 0;
        }

        // 空地不需要更新
        if (factionId == FactionRegistry.NONE) {
            return;
        }

        if (ignited) {
            // 点燃状态：持续一段时间后降级为普通涂墨
            if (elapsed > igniteDecayTime) {
                ignited = false;
                timestamp = currentTime;
                intensity = 0.8f; // 降级后强度降低
            }
        } else {
            // 普通涂墨：逐渐消退
            if (elapsed > inkDecayTime) {
                clear();
            } else {
                intensity = 1.0f - (elapsed / inkDecayTime);
            }
        }
    }

    // Getters
    public int getFactionId() {
        return factionId;
    }

    public float getTimestamp() {
        return timestamp;
    }

    public float getIntensity() {
        return intensity;
    }

    public boolean isIgnited() {
        return ignited;
    }

    public boolean isEmpty() {
        return factionId == FactionRegistry.NONE;
    }

    /**
     * 获取移动速度倍率
     * @param playerFactionId 玩家阵营ID
     * @param registry 阵营注册表
     * @return 速度倍率
     */
    public float getSpeedMultiplier(int playerFactionId, FactionRegistry registry) {
        // 空地，普通速度
        if (isEmpty()) {
            return 1.0f;
        }

        // 判断阵营关系
        Relation relation = registry.getRelation(factionId, playerFactionId);

        // 调试日志
        float result;
        switch (relation) {
            case SELF:
                // 己方领地：加速
                result = ignited ? 2.0f : 1.6f;
                break;

            case ENEMY:
                // 敌方领地：减速
                result = 0.3f;
                break;

            case ALLY:
                // 盟友领地：轻微加速（暂未使用）
                result = 1.3f;
                break;

            case NEUTRAL:
            default:
                result = 1.0f;
                break;
        }

        return result;
    }
}
