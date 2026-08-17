package com.Hecate.player;

/**
 * 玩家游戏状态枚举
 * 四种状态及其能力组合（在己方涂墨上只能在"高速移动/恢复/隐藏"中三选二）：
 *
 * 1. NORMAL（普通）: 可走路 + 可攻击 + 可见 + 不可恢复
 *    - 在普通地面或敌方地面上的状态
 *
 * 2. FAST_MOVING（快速移动）: 不可攻击 + 可快速移动 + 可见 + 可恢复
 *    - 在己方涂墨上：选择了"高速移动"和"恢复"
 *
 * 3. HIDING_STILL（完全隐藏）: 不可攻击 + 不可见 + 可恢复 + 不可移动
 *    - 在己方涂墨上：选择了"隐藏"和"恢复"
 *
 * 4. HIDING_FAST（隐藏快速移动）: 不可攻击 + 可快速移动 + 不可见 + 不可恢复
 *    - 在己方涂墨上：选择了"高速移动"和"隐藏"
 */
public enum PlayerGameState {
    /**
     * 普通状态 - 可走路、可攻击、可见、不可恢复
     * 在普通地面或敌方地面上
     */
    NORMAL,

    /**
     * 快速移动状态 - 不可攻击、可快速移动、可见、可恢复
     * 在己方涂墨上：高速移动 + 恢复
     */
    FAST_MOVING,

    /**
     * 完全隐藏状态 - 不可攻击、不可见、可恢复、不可移动
     * 在己方涂墨上：隐藏 + 恢复
     */
    HIDING_STILL,

    /**
     * 隐藏快速移动状态 - 不可攻击、可快速移动、不可见、不可恢复
     * 在己方涂墨上：高速移动 + 隐藏
     */
    HIDING_FAST;

    /**
     * 判断当前状态下玩家是否可以移动
     * @return true 如果可以移动
     */
    public boolean canMove() {
        switch (this) {
            case NORMAL:
            case FAST_MOVING:
            case HIDING_FAST:
                return true;
            case HIDING_STILL:
                return false;
            default:
                return false;
        }
    }

    /**
     * 判断当前状态下玩家是否可以攻击
     * @return true 如果可以攻击
     */
    public boolean canAttack() {
        switch (this) {
            case NORMAL:
                return true;
            case FAST_MOVING:
            case HIDING_STILL:
            case HIDING_FAST:
                return false;
            default:
                return false;
        }
    }

    /**
     * 判断当前状态下玩家是否可见
     * @return true 如果可见
     */
    public boolean isVisible() {
        switch (this) {
            case NORMAL:
            case FAST_MOVING:
                return true;
            case HIDING_STILL:
            case HIDING_FAST:
                return false;
            default:
                return true;
        }
    }

    /**
     * 判断当前状态下玩家是否可以恢复（血量+弹药）
     * @return true 如果可以恢复
     */
    public boolean canRecover() {
        switch (this) {
            case FAST_MOVING:
            case HIDING_STILL:
                return true;
            case NORMAL:
            case HIDING_FAST:
                return false;
            default:
                return false;
        }
    }

    /**
     * 判断当前状态下玩家是否快速移动
     * @return true 如果快速移动
     */
    public boolean isFastMoving() {
        switch (this) {
            case FAST_MOVING:
            case HIDING_FAST:
                return true;
            case NORMAL:
            case HIDING_STILL:
                return false;
            default:
                return false;
        }
    }

    /**
     * 获取状态的中文描述
     * @return 状态描述
     */
    public String getDescription() {
        switch (this) {
            case NORMAL:
                return "普通状态";
            case FAST_MOVING:
                return "快速移动状态";
            case HIDING_STILL:
                return "完全隐藏状态";
            case HIDING_FAST:
                return "隐藏快速移动状态";
            default:
                return "未知状态";
        }
    }
}
