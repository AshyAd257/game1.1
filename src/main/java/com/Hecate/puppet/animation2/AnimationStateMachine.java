package com.Hecate.puppet.animation2;

import java.util.*;
import java.util.function.Consumer;

/**
 * 动画状态机
 * 管理动画状态和状态之间的转换
 *
 * 【核心功能】
 * - 状态管理：添加、删除、查询状态
 * - 转换管理：定义状态之间的转换规则
 * - 自动转换：根据条件自动切换状态
 * - 事件系统：触发动画事件回调
 *
 * 【解决的问题】
 * 旧AnimationPlayer缺少状态机，导致：
 * - 无法实现复杂的动画逻辑（走→跑→跳）
 * - 无法平滑过渡（生硬切换）
 * - 无法定义转换条件
 * 新AnimationStateMachine完全解决这些问题
 */
public class AnimationStateMachine {

    // 状态管理
    private final Map<String, AnimationState> states;
    private AnimationState currentState;
    private AnimationState defaultState;

    // 转换管理
    private final List<StateTransition> transitions;

    // 混合状态
    private AnimationState targetState;
    private float blendProgress;
    private float blendDuration;

    // 播放状态
    private float currentTime;
    private float lastEventCheckTime;

    // 事件回调
    private final Map<String, List<Consumer<AnimationEvent>>> eventListeners;
    private Consumer<StateTransition> onTransitionCallback;

    /**
     * 构造函数
     */
    public AnimationStateMachine() {
        this.states = new HashMap<>();
        this.currentState = null;
        this.defaultState = null;
        this.transitions = new ArrayList<>();
        this.targetState = null;
        this.blendProgress = 0f;
        this.blendDuration = 0.3f;
        this.currentTime = 0f;
        this.lastEventCheckTime = 0f;
        this.eventListeners = new HashMap<>();
        this.onTransitionCallback = null;
    }

    // ==================== 状态管理 ====================

    /**
     * 添加状态
     *
     * @param state 动画状态
     */
    public void addState(AnimationState state) {
        states.put(state.getName(), state);
    }

    /**
     * 移除状态
     *
     * @param stateName 状态名称
     */
    public void removeState(String stateName) {
        states.remove(stateName);
        if (currentState != null && currentState.getName().equals(stateName)) {
            currentState = defaultState;
        }
    }

    /**
     * 获取状态
     *
     * @param stateName 状态名称
     * @return 动画状态，如果不存在返回null
     */
    public AnimationState getState(String stateName) {
        return states.get(stateName);
    }

    /**
     * 设置默认状态
     *
     * @param stateName 状态名称
     */
    public void setDefaultState(String stateName) {
        AnimationState state = states.get(stateName);
        if (state != null) {
            this.defaultState = state;
            if (currentState == null) {
                currentState = state;
            }
        }
    }

    /**
     * 强制切换到指定状态（不检查转换条件）
     *
     * @param stateName 状态名称
     * @param blendDuration 混合持续时间（0表示立即切换）
     */
    public void forceState(String stateName, float blendDuration) {
        AnimationState state = states.get(stateName);
        if (state == null) {
            java.lang.System.err.println("[AnimationStateMachine] State not found: " + stateName);
            return;
        }

        if (blendDuration > 0f && currentState != null) {
            // 混合过渡
            targetState = state;
            this.blendDuration = blendDuration;
            blendProgress = 0f;
        } else {
            // 立即切换
            currentState = state;
            currentTime = 0f;
            targetState = null;
            blendProgress = 0f;
        }
    }

    // ==================== 转换管理 ====================

    /**
     * 添加转换
     *
     * @param transition 状态转换
     */
    public void addTransition(StateTransition transition) {
        transitions.add(transition);
        // 按优先级排序（优先级高的在前）
        transitions.sort((t1, t2) -> Integer.compare(t2.getPriority(), t1.getPriority()));
    }

    /**
     * 移除转换
     *
     * @param fromState 源状态
     * @param toState 目标状态
     */
    public void removeTransition(String fromState, String toState) {
        transitions.removeIf(t ->
            (t.getFromState() == null || t.getFromState().equals(fromState)) &&
            t.getToState().equals(toState)
        );
    }

    /**
     * 创建并添加简单转换
     *
     * @param fromState 源状态名称（null表示任意状态）
     * @param toState 目标状态名称
     * @param condition 转换条件
     */
    public void addSimpleTransition(String fromState, String toState, TransitionCondition condition) {
        addTransition(new StateTransition(fromState, toState, condition));
    }

    // ==================== 更新逻辑 ====================

    /**
     * 更新状态机
     *
     * @param tpf 时间步长（秒）
     */
    public void update(float tpf) {
        if (currentState == null) {
            return;
        }

        // 1. 检查状态转换
        if (targetState == null) {
            checkTransitions();
        }

        // 2. 更新混合进度
        if (targetState != null) {
            blendProgress += tpf / blendDuration;
            if (blendProgress >= 1.0f) {
                // 混合完成，切换到目标状态
                currentState = targetState;
                currentTime = 0f;
                targetState = null;
                blendProgress = 0f;

                java.lang.System.out.println("[AnimationStateMachine] Transitioned to: " + currentState.getName());
            }
        }

        // 3. 更新当前动画时间
        float oldTime = currentTime;
        if (currentState.getClip() != null) {
            currentTime += tpf * currentState.getPlaybackSpeed();

            float duration = currentState.getDuration();
            if (currentTime >= duration) {
                if (currentState.isLooping()) {
                    currentTime = currentTime % duration;
                    lastEventCheckTime = 0f; // 重置事件检查时间
                } else {
                    currentTime = duration;
                }
            }
        }

        // 4. 检查并触发动画事件
        checkAndFireEvents(oldTime, currentTime);
    }

    /**
     * 检查并执行状态转换
     */
    private void checkTransitions() {
        if (currentState == null) {
            return;
        }

        String currentStateName = currentState.getName();
        float normalizedTime = currentState.getDuration() > 0 ?
            currentTime / currentState.getDuration() : 0f;

        // 遍历所有转换，找到第一个满足条件的
        for (StateTransition transition : transitions) {
            if (!transition.canTransitionFrom(currentStateName)) {
                continue;
            }

            if (!transition.checkExitTime(normalizedTime)) {
                continue;
            }

            if (!transition.checkCondition()) {
                continue;
            }

            // 满足所有条件，执行转换
            AnimationState toState = states.get(transition.getToState());
            if (toState != null) {
                targetState = toState;
                blendDuration = transition.getTransitionDuration();
                blendProgress = 0f;

                // 触发转换回调
                if (onTransitionCallback != null) {
                    onTransitionCallback.accept(transition);
                }

                java.lang.System.out.println("[AnimationStateMachine] Transitioning: " +
                    currentStateName + " -> " + toState.getName());
                break; // 只执行第一个满足条件的转换
            }
        }
    }

    /**
     * 检查并触发动画事件
     */
    private void checkAndFireEvents(float oldTime, float newTime) {
        if (currentState == null || currentState.getClip() == null) {
            return;
        }

        // 处理循环情况
        if (newTime < oldTime) {
            // 动画循环了，检查两个区间
            List<AnimationEvent> events1 = currentState.getEventsInRange(oldTime, currentState.getDuration());
            List<AnimationEvent> events2 = currentState.getEventsInRange(0f, newTime);
            events1.forEach(this::fireEvent);
            events2.forEach(this::fireEvent);
        } else {
            // 正常情况
            List<AnimationEvent> events = currentState.getEventsInRange(oldTime, newTime);
            events.forEach(this::fireEvent);
        }
    }

    /**
     * 触发动画事件
     */
    private void fireEvent(AnimationEvent event) {
        String eventName = event.getEventName();
        List<Consumer<AnimationEvent>> listeners = eventListeners.get(eventName);
        if (listeners != null) {
            for (Consumer<AnimationEvent> listener : listeners) {
                try {
                    listener.accept(event);
                } catch (Exception e) {
                    java.lang.System.err.println("[AnimationStateMachine] Event listener error: " + e.getMessage());
                }
            }
        }
    }

    // ==================== 事件系统 ====================

    /**
     * 添加事件监听器
     *
     * @param eventName 事件名称
     * @param listener 监听器
     */
    public void addEventListener(String eventName, Consumer<AnimationEvent> listener) {
        eventListeners.computeIfAbsent(eventName, k -> new ArrayList<>()).add(listener);
    }

    /**
     * 移除事件监听器
     *
     * @param eventName 事件名称
     * @param listener 监听器
     */
    public void removeEventListener(String eventName, Consumer<AnimationEvent> listener) {
        List<Consumer<AnimationEvent>> listeners = eventListeners.get(eventName);
        if (listeners != null) {
            listeners.remove(listener);
        }
    }

    /**
     * 设置转换回调
     *
     * @param callback 转换回调
     */
    public void setOnTransitionCallback(Consumer<StateTransition> callback) {
        this.onTransitionCallback = callback;
    }

    // ==================== Getters ====================

    public AnimationState getCurrentState() {
        return currentState;
    }

    public AnimationState getTargetState() {
        return targetState;
    }

    public float getBlendProgress() {
        return blendProgress;
    }

    public float getCurrentTime() {
        return currentTime;
    }

    public boolean isTransitioning() {
        return targetState != null;
    }

    public Map<String, AnimationState> getStates() {
        return new HashMap<>(states);
    }

    public List<StateTransition> getTransitions() {
        return new ArrayList<>(transitions);
    }

    @Override
    public String toString() {
        return String.format("AnimationStateMachine[current=%s, states=%d, transitions=%d, transitioning=%s]",
                currentState != null ? currentState.getName() : "null",
                states.size(), transitions.size(), isTransitioning());
    }
}
