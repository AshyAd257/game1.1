package com.Hecate.puppet.animation;

/**
 * 动画层 - 将动画片段与骨骼遮罩结合
 * 用于实现动画分层和部分骨骼动画
 */
public class AnimationLayer {

    private String name;
    private AnimationClip clip;
    private BoneMask mask;
    private int priority;  // 优先级，数值越大越优先（后渲染）
    private float weight;  // 权重，0-1范围，用于混合
    private boolean enabled;  // 是否启用

    /**
     * 创建动画层
     * @param name 层名称
     * @param clip 动画片段
     * @param mask 骨骼遮罩（null表示影响所有骨骼）
     * @param priority 优先级（数值越大越优先）
     */
    public AnimationLayer(String name, AnimationClip clip, BoneMask mask, int priority) {
        this.name = name;
        this.clip = clip;
        this.mask = mask;
        this.priority = priority;
        this.weight = 1.0f;  // 默认全权重
        this.enabled = true;  // 默认启用
    }

    /**
     * 创建动画层（简化构造函数，默认优先级为0）
     */
    public AnimationLayer(String name, AnimationClip clip, BoneMask mask) {
        this(name, clip, mask, 0);
    }

    /**
     * 创建全身动画层（影响所有骨骼）
     */
    public AnimationLayer(String name, AnimationClip clip) {
        this(name, clip, null, 0);
    }

    /**
     * 检查指定骨骼是否受此层影响
     * @param boneName 骨骼名称
     * @return true如果受影响
     */
    public boolean affects(String boneName) {
        // 如果没有遮罩，影响所有骨骼
        if (mask == null || mask.isEmpty()) {
            return true;
        }
        return mask.affects(boneName);
    }

    /**
     * 获取有效权重（考虑启用状态）
     * @return 有效权重（0-1）
     */
    public float getEffectiveWeight() {
        return enabled ? weight : 0f;
    }

    // ==================== Getters & Setters ====================

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public AnimationClip getClip() {
        return clip;
    }

    public void setClip(AnimationClip clip) {
        this.clip = clip;
    }

    public BoneMask getMask() {
        return mask;
    }

    public void setMask(BoneMask mask) {
        this.mask = mask;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public float getWeight() {
        return weight;
    }

    /**
     * 设置权重（自动限制在0-1范围内）
     */
    public void setWeight(float weight) {
        this.weight = Math.max(0f, Math.min(1f, weight));
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * 淡入（逐渐增加权重）
     * @param deltaWeight 每次增加的权重
     */
    public void fadeIn(float deltaWeight) {
        setWeight(weight + deltaWeight);
    }

    /**
     * 淡出（逐渐减少权重）
     * @param deltaWeight 每次减少的权重
     */
    public void fadeOut(float deltaWeight) {
        setWeight(weight - deltaWeight);
    }

    @Override
    public String toString() {
        String maskInfo = (mask != null) ? mask.getName() : "全身";
        String statusInfo = enabled ? "启用" : "禁用";
        return String.format("%s [%s] (优先级:%d, 权重:%.2f, %s)",
                           name, maskInfo, priority, weight, statusInfo);
    }
}
