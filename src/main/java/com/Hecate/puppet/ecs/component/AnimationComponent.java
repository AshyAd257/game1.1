package com.Hecate.puppet.ecs.component;

import com.Hecate.puppet.ecs.Component;
import com.Hecate.puppet.animation.AnimationClip;

import java.util.HashMap;
import java.util.Map;

/**
 * 动画组件
 * 存储实体的动画数据和播放状态
 *
 * 使用AnimationStateMachine来管理复杂的动画状态转换
 */
public class AnimationComponent implements Component {

    // 动画片段库
    private final Map<String, AnimationClip> animations;

    // 当前播放的动画片段
    private AnimationClip currentClip;
    private String currentAnimationName;

    // 播放状态
    private float currentTime;
    private boolean playing;
    private boolean looping;
    private float playbackSpeed;

    // 混合状态（用于动画过渡）
    private AnimationClip targetClip;
    private String targetAnimationName;
    private float blendWeight; // 0.0 = 完全播放current, 1.0 = 完全播放target
    private float blendDuration; // 混合持续时间
    private float blendProgress; // 混合进度

    private boolean enabled;

    /**
     * 构造函数
     */
    public AnimationComponent() {
        this.animations = new HashMap<>();
        this.currentClip = null;
        this.currentAnimationName = null;
        this.currentTime = 0f;
        this.playing = false;
        this.looping = true;
        this.playbackSpeed = 1.0f;
        this.targetClip = null;
        this.targetAnimationName = null;
        this.blendWeight = 0f;
        this.blendDuration = 0.3f; // 默认300ms过渡
        this.blendProgress = 0f;
        this.enabled = true;
    }

    /**
     * 添加动画片段
     *
     * @param name 动画名称
     * @param clip 动画片段
     */
    public void addAnimation(String name, AnimationClip clip) {
        animations.put(name, clip);
    }

    /**
     * 移除动画片段
     *
     * @param name 动画名称
     */
    public void removeAnimation(String name) {
        animations.remove(name);
    }

    /**
     * 获取动画片段
     *
     * @param name 动画名称
     * @return 动画片段，如果不存在返回null
     */
    public AnimationClip getAnimation(String name) {
        return animations.get(name);
    }

    /**
     * 检查是否有指定动画
     *
     * @param name 动画名称
     * @return true如果存在
     */
    public boolean hasAnimation(String name) {
        return animations.containsKey(name);
    }

    /**
     * 播放动画（立即切换）
     *
     * @param name 动画名称
     */
    public void play(String name) {
        play(name, false);
    }

    /**
     * 播放动画
     *
     * @param name 动画名称
     * @param blend 是否使用混合过渡
     */
    public void play(String name, boolean blend) {
        AnimationClip clip = animations.get(name);
        if (clip == null) {
            java.lang.System.err.println("[AnimationComponent] Animation not found: " + name);
            return;
        }

        if (blend && currentClip != null) {
            // 启动混合过渡
            targetClip = clip;
            targetAnimationName = name;
            blendProgress = 0f;
            blendWeight = 0f;
        } else {
            // 立即切换
            currentClip = clip;
            currentAnimationName = name;
            currentTime = 0f;
            targetClip = null;
            targetAnimationName = null;
            blendWeight = 0f;
            blendProgress = 0f;
        }

        playing = true;
        looping = clip.isLooping();
    }

    /**
     * 停止播放
     */
    public void stop() {
        playing = false;
        currentTime = 0f;
    }

    /**
     * 暂停播放
     */
    public void pause() {
        playing = false;
    }

    /**
     * 恢复播放
     */
    public void resume() {
        playing = true;
    }

    /**
     * 更新动画时间
     *
     * @param tpf 时间步长
     */
    public void updateTime(float tpf) {
        if (!playing) {
            return;
        }

        // 更新混合进度
        if (targetClip != null) {
            blendProgress += tpf / blendDuration;
            blendWeight = Math.min(1.0f, blendProgress);

            if (blendProgress >= 1.0f) {
                // 混合完成，切换到目标动画
                currentClip = targetClip;
                currentAnimationName = targetAnimationName;
                currentTime = 0f;
                targetClip = null;
                targetAnimationName = null;
                blendWeight = 0f;
                blendProgress = 0f;
            }
        }

        // 更新当前动画时间
        currentTime += tpf * playbackSpeed;

        if (currentClip != null) {
            float duration = currentClip.getDuration();
            if (currentTime >= duration) {
                if (looping) {
                    currentTime = currentTime % duration;
                } else {
                    currentTime = duration;
                    playing = false;
                }
            }
        }
    }

    /**
     * 检查是否正在混合
     *
     * @return true如果正在混合过渡
     */
    public boolean isBlending() {
        return targetClip != null;
    }

    // ========== Getters/Setters ==========

    public Map<String, AnimationClip> getAnimations() {
        return animations;
    }

    public AnimationClip getCurrentClip() {
        return currentClip;
    }

    public String getCurrentAnimationName() {
        return currentAnimationName;
    }

    public float getCurrentTime() {
        return currentTime;
    }

    public void setCurrentTime(float time) {
        this.currentTime = time;
    }

    public boolean isPlaying() {
        return playing;
    }

    public void setPlaying(boolean playing) {
        this.playing = playing;
    }

    public boolean isLooping() {
        return looping;
    }

    public void setLooping(boolean looping) {
        this.looping = looping;
    }

    public float getPlaybackSpeed() {
        return playbackSpeed;
    }

    public void setPlaybackSpeed(float speed) {
        this.playbackSpeed = speed;
    }

    public AnimationClip getTargetClip() {
        return targetClip;
    }

    public String getTargetAnimationName() {
        return targetAnimationName;
    }

    public float getBlendWeight() {
        return blendWeight;
    }

    public float getBlendDuration() {
        return blendDuration;
    }

    public void setBlendDuration(float duration) {
        this.blendDuration = Math.max(0.01f, duration);
    }

    // ========== Component Interface ==========

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public Component clone() {
        AnimationComponent cloned = new AnimationComponent();
        cloned.animations.putAll(this.animations);
        cloned.playbackSpeed = this.playbackSpeed;
        cloned.blendDuration = this.blendDuration;
        cloned.looping = this.looping;
        return cloned;
    }

    @Override
    public String toString() {
        return String.format("AnimationComponent[current=%s, playing=%s, time=%.2f, blending=%s]",
                currentAnimationName, playing, currentTime, isBlending());
    }
}
