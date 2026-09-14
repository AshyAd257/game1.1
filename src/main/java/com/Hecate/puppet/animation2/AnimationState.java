package com.Hecate.puppet.animation2;

import com.Hecate.puppet.animation.AnimationClip;

import java.util.ArrayList;
import java.util.List;

/**
 * 动画状态
 * 代表动画状态机中的一个状态
 *
 * 每个状态包含一个动画片段和相关的播放参数
 */
public class AnimationState {

    private final String name;
    private AnimationClip clip;

    // 播放参数
    private float playbackSpeed;
    private boolean looping;

    // 状态权重（用于混合）
    private float weight;

    // 动画事件
    private final List<AnimationEvent> events;

    // 标签（用于分类和查询）
    private final List<String> tags;

    /**
     * 构造函数
     *
     * @param name 状态名称
     * @param clip 动画片段
     */
    public AnimationState(String name, AnimationClip clip) {
        this.name = name;
        this.clip = clip;
        this.playbackSpeed = 1.0f;
        this.looping = clip != null && clip.isLooping();
        this.weight = 1.0f;
        this.events = new ArrayList<>();
        this.tags = new ArrayList<>();
    }

    /**
     * 添加动画事件
     *
     * @param time 触发时间（秒）
     * @param eventName 事件名称
     */
    public void addEvent(float time, String eventName) {
        events.add(new AnimationEvent(time, eventName));
    }

    /**
     * 添加动画事件（带参数）
     *
     * @param time 触发时间（秒）
     * @param eventName 事件名称
     * @param parameter 事件参数
     */
    public void addEvent(float time, String eventName, Object parameter) {
        events.add(new AnimationEvent(time, eventName, parameter));
    }

    /**
     * 获取指定时间范围内的事件
     *
     * @param startTime 起始时间
     * @param endTime 结束时间
     * @return 该时间范围内的事件列表
     */
    public List<AnimationEvent> getEventsInRange(float startTime, float endTime) {
        List<AnimationEvent> result = new ArrayList<>();
        for (AnimationEvent event : events) {
            float eventTime = event.getTime();
            if (eventTime >= startTime && eventTime <= endTime) {
                result.add(event);
            }
        }
        return result;
    }

    /**
     * 添加标签
     *
     * @param tag 标签名称
     */
    public void addTag(String tag) {
        if (!tags.contains(tag)) {
            tags.add(tag);
        }
    }

    /**
     * 移除标签
     *
     * @param tag 标签名称
     */
    public void removeTag(String tag) {
        tags.remove(tag);
    }

    /**
     * 检查是否有指定标签
     *
     * @param tag 标签名称
     * @return true如果有该标签
     */
    public boolean hasTag(String tag) {
        return tags.contains(tag);
    }

    // ========== Getters/Setters ==========

    public String getName() {
        return name;
    }

    public AnimationClip getClip() {
        return clip;
    }

    public void setClip(AnimationClip clip) {
        this.clip = clip;
        if (clip != null) {
            this.looping = clip.isLooping();
        }
    }

    public float getPlaybackSpeed() {
        return playbackSpeed;
    }

    public void setPlaybackSpeed(float speed) {
        this.playbackSpeed = speed;
    }

    public boolean isLooping() {
        return looping;
    }

    public void setLooping(boolean looping) {
        this.looping = looping;
    }

    public float getWeight() {
        return weight;
    }

    public void setWeight(float weight) {
        this.weight = Math.max(0f, Math.min(1f, weight));
    }

    public List<AnimationEvent> getEvents() {
        return new ArrayList<>(events);
    }

    public List<String> getTags() {
        return new ArrayList<>(tags);
    }

    public float getDuration() {
        return clip != null ? clip.getDuration() : 0f;
    }

    @Override
    public String toString() {
        return String.format("AnimationState[name=%s, clip=%s, speed=%.2f, weight=%.2f, events=%d]",
                name, clip != null ? clip.getName() : "null", playbackSpeed, weight, events.size());
    }
}
