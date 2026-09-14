package com.Hecate.puppet.animation2;

import java.util.function.Supplier;

/**
 * 状态转换
 * 定义从一个状态到另一个状态的转换规则
 *
 * 转换包含：
 * - 源状态和目标状态
 * - 转换条件（Condition）
 * - 转换持续时间（混合时间）
 */
public class StateTransition {

    private final String fromState;     // 源状态名称（null表示任意状态）
    private final String toState;       // 目标状态名称
    private final TransitionCondition condition; // 转换条件
    private final float transitionDuration; // 转换持续时间（秒）
    private final int priority;         // 优先级（数值越大越优先）

    // 转换选项
    private final boolean canInterruptSelf; // 是否可以中断自己（重新播放同一动画）
    private final float exitTime;       // 退出时间（0-1，相对于动画长度）

    /**
     * 构造函数（简单版本）
     *
     * @param fromState 源状态名称（null表示任意状态）
     * @param toState 目标状态名称
     * @param condition 转换条件
     */
    public StateTransition(String fromState, String toState, TransitionCondition condition) {
        this(fromState, toState, condition, 0.3f, 0, true, 0f);
    }

    /**
     * 构造函数（完整版本）
     *
     * @param fromState 源状态名称（null表示任意状态）
     * @param toState 目标状态名称
     * @param condition 转换条件
     * @param transitionDuration 转换持续时间
     * @param priority 优先级
     * @param canInterruptSelf 是否可以中断自己
     * @param exitTime 退出时间（0-1）
     */
    public StateTransition(String fromState, String toState, TransitionCondition condition,
                          float transitionDuration, int priority,
                          boolean canInterruptSelf, float exitTime) {
        this.fromState = fromState;
        this.toState = toState;
        this.condition = condition;
        this.transitionDuration = Math.max(0f, transitionDuration);
        this.priority = priority;
        this.canInterruptSelf = canInterruptSelf;
        this.exitTime = Math.max(0f, Math.min(1f, exitTime));
    }

    /**
     * 检查是否可以从指定状态转换
     *
     * @param currentState 当前状态名称
     * @return true如果可以转换
     */
    public boolean canTransitionFrom(String currentState) {
        if (fromState == null) {
            return true; // 任意状态都可以转换
        }

        if (!canInterruptSelf && toState.equals(currentState)) {
            return false; // 不允许中断自己
        }

        return fromState.equals(currentState);
    }

    /**
     * 检查转换条件是否满足
     *
     * @return true如果条件满足
     */
    public boolean checkCondition() {
        return condition != null && condition.test();
    }

    /**
     * 检查是否满足退出时间条件
     *
     * @param normalizedTime 归一化时间（0-1）
     * @return true如果满足退出时间
     */
    public boolean checkExitTime(float normalizedTime) {
        return normalizedTime >= exitTime;
    }

    // ========== Getters ==========

    public String getFromState() {
        return fromState;
    }

    public String getToState() {
        return toState;
    }

    public TransitionCondition getCondition() {
        return condition;
    }

    public float getTransitionDuration() {
        return transitionDuration;
    }

    public int getPriority() {
        return priority;
    }

    public boolean canInterruptSelf() {
        return canInterruptSelf;
    }

    public float getExitTime() {
        return exitTime;
    }

    @Override
    public String toString() {
        return String.format("StateTransition[%s -> %s, duration=%.2fs, priority=%d]",
                fromState != null ? fromState : "Any", toState, transitionDuration, priority);
    }
}
