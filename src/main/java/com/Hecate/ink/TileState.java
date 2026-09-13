package com.Hecate.ink;

/**
 * 地块状态枚举
 *
 * 【核心变更】
 * 替代原有的 factionId 系统，地块状态与阵营解耦
 * 地块可以是光明、黑暗、雾、点燃或无状态
 *
 * 设计理念：
 * - 光明系：加速、恢复效果
 * - 黑暗系：隐藏、机动效果
 * - 中立：无特殊效果
 */
public enum TileState {

    /**
     * 无状态（中立地块）
     * 未被任何效果覆盖，普通移动速度
     */
    NONE("无状态"),

    /**
     * 光明笼罩
     * 提供加速 + 恢复 buff
     * 己方：1.5倍速度
     * 敌方：0.8倍速度
     */
    LIGHT("光明"),

    /**
     * 黑暗笼罩
     * 提供隐藏 buff
     * 己方：1.2倍速度 + 隐身效果
     * 敌方：0.7倍速度
     */
    DARK("黑暗"),

    /**
     * 雾笼罩（黑暗的变体）
     * 类似黑暗但效果稍弱，视觉上更朦胧
     * 己方：1.1倍速度
     * 敌方：0.8倍速度
     */
    FOG("雾"),

    /**
     * 点燃（光明的加强版）
     * 更强的加速 + 恢复效果
     * 己方：2.0倍速度
     * 敌方：0.5倍速度
     */
    IGNITED("点燃");

    private final String displayName;

    TileState(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * 判断是否是光明系状态
     * @return true 如果是光明或点燃
     */
    public boolean isLightType() {
        return this == LIGHT || this == IGNITED;
    }

    /**
     * 判断是否是黑暗系状态
     * @return true 如果是黑暗或雾
     */
    public boolean isDarkType() {
        return this == DARK || this == FOG;
    }

    /**
     * 获取状态覆盖所需的命中次数阈值
     * 不同状态有不同的抗性
     *
     * @return 需要多少次命中才能被覆盖
     */
    public int getCoverageThreshold() {
        switch (this) {
            case IGNITED:
                return 10; // 点燃状态抗性最强
            case LIGHT:
            case DARK:
                return 5;  // 普通状态
            case FOG:
                return 3;  // 雾状态抗性最弱
            case NONE:
                return 1;  // 空地立即被覆盖
            default:
                return 5;
        }
    }

    /**
     * 获取己方速度倍率
     * @return 速度倍率
     */
    public float getAllySpeedMultiplier() {
        switch (this) {
            case LIGHT:
                return 1.5f;
            case IGNITED:
                return 2.0f;
            case DARK:
                return 1.2f;
            case FOG:
                return 1.1f;
            case NONE:
            default:
                return 1.0f;
        }
    }

    /**
     * 获取敌方速度倍率
     * @return 速度倍率
     */
    public float getEnemySpeedMultiplier() {
        switch (this) {
            case LIGHT:
                return 0.8f;
            case IGNITED:
                return 0.5f;
            case DARK:
                return 0.7f;
            case FOG:
                return 0.8f;
            case NONE:
            default:
                return 1.0f;
        }
    }

    /**
     * 判断是否提供隐身效果
     * @return true 如果提供隐身
     */
    public boolean providesStealthBuff() {
        return this == DARK || this == FOG;
    }

    /**
     * 判断是否提供恢复效果
     * @return true 如果提供恢复
     */
    public boolean providesHealingBuff() {
        return this == LIGHT || this == IGNITED;
    }
}
