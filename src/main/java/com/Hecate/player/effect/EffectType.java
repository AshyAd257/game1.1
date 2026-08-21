package com.Hecate.player.effect;

/**
 * 效果类型枚举
 * 定义所有可能的 Buff 和 Debuff 类型
 */
public enum EffectType {
    // ==================== Buff（正面效果） ====================
    /** 速度提升 */
    SPEED_BOOST("速度提升", true),

    /** 力量提升 */
    STRENGTH("力量提升", true),

    /** 生命恢复 */
    REGENERATION("生命恢复", true),

    /** 护盾/伤害吸收 */
    SHIELD("护盾", true),

    /** 跳跃提升 */
    JUMP_BOOST("跳跃提升", true),

    /** 隐身 */
    INVISIBILITY("隐身", true),

    /** 抗性提升 */
    RESISTANCE("抗性提升", true),

    // ==================== Debuff（负面效果） ====================
    /** 中毒（持续伤害） */
    POISON("中毒", false),

    /** 减速 */
    SLOWNESS("减速", false),

    /** 虚弱 */
    WEAKNESS("虚弱", false),

    /** 失明 */
    BLINDNESS("失明", false),

    /** 燃烧 */
    BURNING("燃烧", false),

    /** 冰冻/减速 */
    FROZEN("冰冻", false),

    /** 眩晕 */
    STUN("眩晕", false),

    // ==================== 特殊效果 ====================
    /** 墨水着色（Splatoon风格） */
    INK_COLOR("墨水着色", false),

    /** 无敌帧 */
    INVINCIBLE("无敌", true),

    /** 自定义效果（用于游戏专属的永久buff等特殊情况） */
    CUSTOM("自定义", true);

    private final String displayName;
    private final boolean beneficial;  // 是否为正面效果

    EffectType(String displayName, boolean beneficial) {
        this.displayName = displayName;
        this.beneficial = beneficial;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isBeneficial() {
        return beneficial;
    }

    public boolean isHarmful() {
        return !beneficial;
    }
}
