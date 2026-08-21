package com.Hecate.player.inventory;

/**
 * 手持物品类型枚举
 */
public enum HeldItemType {
    /** 空手 */
    EMPTY,

    /** 武器（从武器注册表选择） */
    WEAPON,

    /** 方块（从方块注册表选择） */
    BLOCK,

    /** 物品（从物品注册表选择，暂未实现） */
    ITEM
}
