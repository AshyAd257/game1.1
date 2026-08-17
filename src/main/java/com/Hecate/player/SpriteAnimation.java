package com.Hecate.player;

import java.util.ArrayList;
import java.util.List;

/**
 * 2D精灵动画序列管理器
 * 与现有PlayerAnimator系统协同工作
 */
public class SpriteAnimation {

    private final String animationName;
    private final List<AnimationFrame> frames;
    private final boolean loop;

    // 播放状态
    private int currentFrameIndex = 0;
    private float currentTime = 0f;
    private boolean isPlaying = false;
    private boolean isPaused = false;

    // 与3D动画的同步
    private AnimationState linkedAnimationState;

    public SpriteAnimation(String animationName, boolean loop) {
        this.animationName = animationName;
        this.frames = new ArrayList<>();
        this.loop = loop;
    }

    /**
     * 添加动画帧
     */
    public void addFrame(AnimationFrame frame) {
        frames.add(frame);
    }

    /**
     * 链接到3D动画状态
     */
    public void linkToAnimationState(AnimationState state) {
        this.linkedAnimationState = state;
        // 为所有帧设置映射状态
        for (AnimationFrame frame : frames) {
            frame.setMappedAnimationState(state);
        }
    }

    /**
     * 开始播放动画
     */
    public void play() {
        if (!isPlaying) {
            isPlaying = true;
            isPaused = false;
        }
    }

    /**
     * 暂停动画
     */
    public void pause() {
        isPaused = true;
    }

    /**
     * 停止动画
     */
    public void stop() {
        isPlaying = false;
        isPaused = false;
        currentTime = 0f;
        currentFrameIndex = 0;
    }

    /**
     * 重置动画到开始
     */
    public void reset() {
        currentTime = 0f;
        currentFrameIndex = 0;
    }

    /**
     * 更新动画（与3D动画同步）
     */
    public void update(float tpf, AnimationState currentState) {
        if (!isPlaying || isPaused || frames.isEmpty()) {
            return;
        }

        currentTime += tpf;

        // 计算总持续时间
        float totalDuration = calculateTotalDuration();

        if (totalDuration <= 0) {
            return; // 防止除零错误
        }

        // 如果超过总时长，循环动画
        if (currentTime >= totalDuration) {
            if (loop) {
                currentTime = currentTime % totalDuration;
            } else {
                currentTime = totalDuration;
                isPlaying = false;
                return;
            }
        }

        // 计算当前帧
        float accumulatedTime = 0f;
        int newFrameIndex = 0;

        for (int i = 0; i < frames.size(); i++) {
            accumulatedTime += frames.get(i).getDuration();
            if (currentTime <= accumulatedTime) {
                newFrameIndex = i;
                break;
            }
        }

        // 确保帧索引在有效范围内
        currentFrameIndex = Math.max(0, Math.min(newFrameIndex, frames.size() - 1));
    }

    /**
     * 获取当前帧
     */
    public AnimationFrame getCurrentFrame() {
        if (frames.isEmpty()) {
            return null;
        }
        return frames.get(Math.min(currentFrameIndex, frames.size() - 1));
    }

    /**
     * 计算总持续时间
     */
    public float calculateTotalDuration() {
        float total = 0f;
        for (AnimationFrame frame : frames) {
            total += frame.getDuration();
        }
        return total;
    }

    /**
     * 跳转到指定帧
     */
    public boolean jumpToFrame(int frameIndex) {
        if (frameIndex >= 0 && frameIndex < frames.size()) {
            currentFrameIndex = frameIndex;
            currentTime = 0f;
            // 计算到该帧的累积时间
            for (int i = 0; i < frameIndex; i++) {
                currentTime += frames.get(i).getDuration();
            }
            return true;
        }
        return false;
    }

    /**
     * 获取播放进度 (0.0 - 1.0)
     */
    public float getPlayProgress() {
        float totalDuration = calculateTotalDuration();
        if (totalDuration <= 0) return 0f;
        return Math.min(1.0f, currentTime / totalDuration);
    }

    /**
     * 获取动画状态信息
     */
    public String getAnimationInfo() {
        return String.format("动画: %s - 帧数: %d, 时长: %.2fs, 循环: %s, 状态: %s, 进度: %.1f%%",
                animationName, frames.size(), calculateTotalDuration(),
                loop ? "是" : "否",
                isPlaying ? (isPaused ? "暂停" : "播放") : "停止",
                getPlayProgress() * 100);
    }

    // Getter方法
    public String getAnimationName() {
        return animationName;
    }

    public List<AnimationFrame> getFrames() {
        return new ArrayList<>(frames);
    }

    public boolean isLoop() {
        return loop;
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public boolean isPaused() {
        return isPaused;
    }

    public int getCurrentFrameIndex() {
        return currentFrameIndex;
    }

    public float getCurrentTime() {
        return currentTime;
    }

    public AnimationState getLinkedAnimationState() {
        return linkedAnimationState;
    }

    public int getFrameCount() {
        return frames.size();
    }

    @Override
    public String toString() {
        return String.format("SpriteAnimation{name='%s', frames=%d, playing=%s, linked=%s}",
                animationName, frames.size(), isPlaying, linkedAnimationState);
    }
}

