package com.Hecate.player;

import java.util.*;
import java.util.logging.Logger;

/**
 * 精灵动画系统
 * 管理2D精灵动画的播放、切换和特殊逻辑
 */
public class SpriteAnimationSystem {

    private static final Logger logger = Logger.getLogger(SpriteAnimationSystem.class.getName());

    // 特殊空闲逻辑配置
    private static final float MIN_IDLE_INTERVAL = 3.0f;
    private static final float MAX_IDLE_INTERVAL = 7.0f;
    private static final float DOUBLE_PLAY_CHANCE = 0.5f;
    private static final float IDLE_TRANSITION_TIME = 0.1f;

    // 动画存储
    private final Map<String, SpriteAnimation> animations;
    private final Map<AnimationState, String> stateToAnimationMap;

    // 当前状态
    private SpriteAnimation currentAnimation;
    private String currentAnimationName;
    private AnimationState lastAnimationState;

    // 系统配置
    private boolean autoSwitchEnabled;
    private float transitionTime;

    // 特殊空闲逻辑状态
    private boolean specialFrontIdleLogicEnabled;
    private float idleTimer;
    private float nextIdleTriggerTime;
    private boolean isPlayingSpecialIdle;
    private int specialIdlePlayCount;
    private float transitionBackTimer;

    // 随机数生成器
    private final Random random;

    public SpriteAnimationSystem() {
        this.animations = new HashMap<>();
        this.stateToAnimationMap = new HashMap<>();
        this.autoSwitchEnabled = true;
        this.transitionTime = 0.1f;
        this.specialFrontIdleLogicEnabled = false;
        this.random = new Random();

        resetSpecialIdleState();
    }

    /**
     * 添加动画到系统
     */
    public void addAnimation(String name, SpriteAnimation animation) {
        if (name == null || name.trim().isEmpty()) {
            logger.warning("动画名称不能为空");
            return;
        }

        if (animation == null) {
            logger.warning("动画对象不能为null");
            return;
        }

        animations.put(name, animation);
    }

    /**
     * 从帧列表创建并添加动画
     */
    public void addAnimationFromFrames(String name, List<AnimationFrame> frames, boolean loop) {
        if (frames == null || frames.isEmpty()) {
            logger.warning("帧列表不能为空");
            return;
        }

        SpriteAnimation animation = new SpriteAnimation(name, loop);
        for (AnimationFrame frame : frames) {
            animation.addFrame(frame);
        }

        addAnimation(name, animation);
    }

    /**
     * 移除动画
     */
    public boolean removeAnimation(String name) {
        if (animations.containsKey(name)) {
            if (name.equals(currentAnimationName)) {
                stopCurrentAnimation();
            }
            animations.remove(name);
            return true;
        }
        return false;
    }

    /**
     * 设置动画状态映射
     */
    public void setStateMapping(AnimationState state, String animationName) {
        if (state == null) {
            logger.warning("动画状态不能为null");
            return;
        }

        stateToAnimationMap.put(state, animationName);
    }

    /**
     * 移除状态映射
     */
    public void removeStateMapping(AnimationState state) {
        stateToAnimationMap.remove(state);
    }

    /**
     * 播放指定动画
     */
    public boolean playAnimation(String animationName) {
        SpriteAnimation animation = animations.get(animationName);

        if (animation != null) {
            if (currentAnimation != null && !currentAnimationName.equals(animationName)) {
                currentAnimation.stop();
            }

            currentAnimation = animation;
            currentAnimationName = animationName;
            currentAnimation.play();

            if (!animationName.equals("idle_01") && !animationName.equals("idle_02")) {
                resetSpecialIdleState();
            }

            return true;
        } else {
            logger.warning("未找到动画: " + animationName);
            return false;
        }
    }

    /**
     * 根据3D动画状态切换动画
     */
    public void switchToAnimation(AnimationState state) {
        String animationName = stateToAnimationMap.get(state);
        if (animationName != null && !animationName.equals(currentAnimationName)) {
            playAnimation(animationName);
        }
    }

    /**
     * 更新动画系统
     */
    public void update(float tpf, AnimationState currentState) {
        if (autoSwitchEnabled && currentState != lastAnimationState) {
            switchToAnimation(currentState);
            lastAnimationState = currentState;
        }

        if (currentAnimation != null) {
            currentAnimation.update(tpf, currentState);
        }

        if (specialFrontIdleLogicEnabled && currentState == AnimationState.IDLE) {
            updateSpecialIdleLogic(tpf);
        }
    }

    /**
     * 更新特殊空闲逻辑
     */
    private void updateSpecialIdleLogic(float tpf) {
        if (isPlayingSpecialIdle) {
            if (currentAnimationName.equals("idle_02") &&
                    currentAnimation != null &&
                    !currentAnimation.isPlaying()) {

                specialIdlePlayCount--;

                if (specialIdlePlayCount > 0) {
                    playAnimation("idle_01");
                    transitionBackTimer = IDLE_TRANSITION_TIME;
                } else {
                    playAnimation("idle_01");
                    isPlayingSpecialIdle = false;
                    resetIdleTimer();
                }
            }

            if (transitionBackTimer > 0) {
                transitionBackTimer -= tpf;
                if (transitionBackTimer <= 0) {
                    playAnimation("idle_02");
                }
            }
        } else {
            idleTimer += tpf;

            if (idleTimer >= nextIdleTriggerTime) {
                triggerSpecialIdle();
            }
        }
    }

    /**
     * 触发特殊空闲动画
     */
    private void triggerSpecialIdle() {
        if (!animations.containsKey("idle_02")) {
            logger.warning("未找到 idle_02 动画");
            resetIdleTimer();
            return;
        }

        specialIdlePlayCount = random.nextFloat() < DOUBLE_PLAY_CHANCE ? 2 : 1;
        playAnimation("idle_02");
        isPlayingSpecialIdle = true;
    }

    /**
     * 重置空闲计时器
     */
    private void resetIdleTimer() {
        idleTimer = 0f;
        nextIdleTriggerTime = MIN_IDLE_INTERVAL +
                random.nextFloat() * (MAX_IDLE_INTERVAL - MIN_IDLE_INTERVAL);
    }

    /**
     * 重置特殊空闲状态
     */
    private void resetSpecialIdleState() {
        isPlayingSpecialIdle = false;
        specialIdlePlayCount = 0;
        transitionBackTimer = 0f;
        resetIdleTimer();
    }

    /**
     * 启用/禁用特殊前置空闲逻辑
     */
    public void setSpecialFrontIdleLogic(boolean enabled) {
        this.specialFrontIdleLogicEnabled = enabled;
        if (enabled) {
            resetIdleTimer();
        } else {
            resetSpecialIdleState();
        }
    }

    public boolean isSpecialFrontIdleLogicEnabled() {
        return specialFrontIdleLogicEnabled;
    }

    public void pauseCurrentAnimation() {
        if (currentAnimation != null) {
            currentAnimation.pause();
        }
    }

    public void resumeCurrentAnimation() {
        if (currentAnimation != null && currentAnimation.isPaused()) {
            currentAnimation.play();
        }
    }

    public void stopCurrentAnimation() {
        if (currentAnimation != null) {
            currentAnimation.stop();
            currentAnimation = null;
            currentAnimationName = null;
        }
    }

    public void resetCurrentAnimation() {
        if (currentAnimation != null) {
            currentAnimation.reset();
        }
    }

    public AnimationFrame getCurrentFrame() {
        if (currentAnimation != null) {
            return currentAnimation.getCurrentFrame();
        }
        return null;
    }

    public SpriteAnimation getCurrentAnimation() {
        return currentAnimation;
    }

    public String getCurrentAnimationName() {
        return currentAnimationName;
    }

    public void setAutoSwitchEnabled(boolean enabled) {
        this.autoSwitchEnabled = enabled;
    }

    public boolean isAutoSwitchEnabled() {
        return autoSwitchEnabled;
    }

    public void setTransitionTime(float time) {
        this.transitionTime = Math.max(0, time);
    }

    public String[] getAnimationNames() {
        return animations.keySet().toArray(new String[0]);
    }

    public boolean hasAnimation(String animationName) {
        return animations.containsKey(animationName);
    }

    public String getSystemStatus() {
        String status = String.format("动画系统 - 当前: %s, 总数: %d, 自动切换: %s",
                currentAnimationName != null ? currentAnimationName : "无",
                animations.size(),
                autoSwitchEnabled ? "开" : "关");

        if (specialFrontIdleLogicEnabled) {
            status += String.format("\n特殊空闲: 启用, 计时: %.1f/%.1f秒",
                    idleTimer, nextIdleTriggerTime);
        }

        return status;
    }

    public void printAnimationInfo() {
        // 静默方法，不进行任何输出
    }

    public String getAnimationInfo() {
        StringBuilder info = new StringBuilder();
        info.append("精灵动画系统状态:\n");
        info.append("   ").append(getSystemStatus()).append("\n");
        info.append("   状态映射:\n");

        for (Map.Entry<AnimationState, String> entry : stateToAnimationMap.entrySet()) {
            String status = animations.containsKey(entry.getValue()) ? "存在" : "缺失";
            info.append("     ").append(entry.getKey()).append(" -> ")
                    .append(entry.getValue()).append(" ").append(status).append("\n");
        }

        if (specialFrontIdleLogicEnabled) {
            info.append("   特殊空闲逻辑: 启用\n");
            info.append("     - 触发间隔: ").append(MIN_IDLE_INTERVAL)
                    .append("-").append(MAX_IDLE_INTERVAL).append("秒\n");
            info.append("     - 双重播放概率: ").append((int)(DOUBLE_PLAY_CHANCE * 100)).append("%\n");
            info.append("     - 过渡时间: ").append(IDLE_TRANSITION_TIME).append("秒\n");
        }

        return info.toString();
    }

    public void cleanup() {
        stopCurrentAnimation();
        animations.clear();
        stateToAnimationMap.clear();
        resetSpecialIdleState();
    }
}
