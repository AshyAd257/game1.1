package com.Hecate.player;

/**
 * 波次结算后可供玩家选择的强化效果。
 * <p>具体数值效果由 {@link PlayerController} 应用（见其 onBuffSelected 方法），
 * 这里只负责定义种类和展示名称。
 */
public enum BuffType {

    FIRE_RATE_UP("射击速度 +50%"),
    EXTRA_PROJECTILE("攻击弹道 +1"),
    SPREAD_RANGE_UP("攻击范围变大"),
    RECOVERY_SPEED_UP("恢复速度变快"),
    MOVE_SPEED_UP("移动速度 +5%");

    public final String displayName;

    BuffType(String displayName) {
        this.displayName = displayName;
    }
}
