package com.Hecate.weapon;

/**
 * 武器大类：远程 / 近战。
 * <p>玩家的持有状态仍然只有二态（持有武器 / 空手），近战和远程的区分发生在
 * "持有武器"内部——由当前武器自身的 {@link WeaponKind#getCategory()} 决定
 * {@code PlayerController} 该走开火分支还是挥砍分支，而不是在输入层新增第三态。
 */
public enum WeaponCategory {
    RANGED,
    MELEE
}
