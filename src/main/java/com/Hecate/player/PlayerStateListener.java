package com.Hecate.player;

/**
 * 👂 玩家状态监听器接口
 * 用于监听玩家状态变化事件
 */
public interface PlayerStateListener {

    /**
     * 🔄 状态改变时调用
     * @param oldState 旧状态
     * @param newState 新状态
     * @param reason 状态改变的原因
     */
    void onStateChanged(PlayerState oldState, PlayerState newState, String reason);

    /**
     * ⏰ 状态持续时调用（可选实现）
     * @param currentState 当前状态
     * @param duration 状态持续时间
     */
    default void onStateUpdate(PlayerState currentState, float duration) {
        // 默认空实现
    }

    /**
     * 🎯 进入特定状态时调用（可选实现）
     * @param state 进入的状态
     * @param fromState 来源状态
     */
    default void onStateEntered(PlayerState state, PlayerState fromState) {
        // 默认空实现
    }

    /**
     * 🚪 离开特定状态时调用（可选实现）
     * @param state 离开的状态
     * @param toState 目标状态
     */
    default void onStateExited(PlayerState state, PlayerState toState) {
        // 默认空实现
    }

    /**
     * ⚠️ 状态转换被拒绝时调用（可选实现）
     * @param currentState 当前状态
     * @param requestedState 请求的状态
     * @param reason 拒绝原因
     */
    default void onStateTransitionRejected(PlayerState currentState, PlayerState requestedState, String reason) {
        // 默认空实现
    }
}
