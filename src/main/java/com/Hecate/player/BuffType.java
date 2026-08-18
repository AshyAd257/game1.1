package com.Hecate.player;

import com.Hecate.localization.Localization;

/**
 * 波次结算后可供玩家选择的强化效果。
 * <p>具体数值效果由 {@link PlayerController} 应用（见其 onBuffSelected 方法），
 * 这里只负责定义种类和展示名称。
 */
public enum BuffType {

    FIRE_RATE_UP("buff.fire_rate"),
    EXTRA_PROJECTILE("buff.multi_shot"),
    SPREAD_RANGE_UP("buff.wide_spread"),
    RECOVERY_SPEED_UP("buff.quick_recovery"),
    MOVE_SPEED_UP("buff.swift_move");

    private final String keyPrefix;

    BuffType(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }

    /**
     * 获取本地化的显示名称
     */
    public String getDisplayName() {
        return Localization.get(keyPrefix + ".name");
    }

    /**
     * 获取本地化的描述文本
     */
    public String getDescription() {
        return Localization.get(keyPrefix + ".desc");
    }
}
