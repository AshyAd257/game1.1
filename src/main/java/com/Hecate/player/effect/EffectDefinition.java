package com.Hecate.player.effect;

/**
 * 效果定义（模板）
 * 存储在 EffectRegistry 中的静态配置数据
 */
public class EffectDefinition {
    private final String id;
    private final EffectType type;
    private final String displayName;

    // 效果属性
    private final float baseDuration;  // 基础持续时间（秒）
    private final int maxStacks;       // 最大堆叠层数（0=不可堆叠，-1=无限堆叠）
    private final boolean refreshable; // 是否可以刷新持续时间

    // 冲突与互斥
    private final String[] conflictsWith;  // 与哪些效果冲突（互斥）

    // 效果强度
    private final float magnitude;  // 效果强度（如速度倍率、伤害量等）

    private EffectDefinition(Builder builder) {
        this.id = builder.id;
        this.type = builder.type;
        this.displayName = builder.displayName;
        this.baseDuration = builder.baseDuration;
        this.maxStacks = builder.maxStacks;
        this.refreshable = builder.refreshable;
        this.conflictsWith = builder.conflictsWith;
        this.magnitude = builder.magnitude;
    }

    // Getters
    public String getId() { return id; }
    public EffectType getType() { return type; }
    public String getDisplayName() { return displayName; }
    public float getBaseDuration() { return baseDuration; }
    public int getMaxStacks() { return maxStacks; }
    public boolean isRefreshable() { return refreshable; }
    public String[] getConflictsWith() { return conflictsWith; }
    public float getMagnitude() { return magnitude; }

    /**
     * 检查是否与另一个效果冲突
     */
    public boolean conflictsWith(String otherId) {
        if (conflictsWith == null) return false;
        for (String conflictId : conflictsWith) {
            if (conflictId.equals(otherId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Builder 模式
     */
    public static class Builder {
        private final String id;
        private final EffectType type;
        private String displayName;
        private float baseDuration = 10.0f;
        private int maxStacks = 1;
        private boolean refreshable = true;
        private String[] conflictsWith = new String[0];
        private float magnitude = 1.0f;

        public Builder(String id, EffectType type) {
            this.id = id;
            this.type = type;
            this.displayName = type.getDisplayName();
        }

        public Builder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public Builder duration(float seconds) {
            this.baseDuration = seconds;
            return this;
        }

        public Builder maxStacks(int stacks) {
            this.maxStacks = stacks;
            return this;
        }

        public Builder refreshable(boolean refreshable) {
            this.refreshable = refreshable;
            return this;
        }

        public Builder conflictsWith(String... effectIds) {
            this.conflictsWith = effectIds;
            return this;
        }

        public Builder magnitude(float magnitude) {
            this.magnitude = magnitude;
            return this;
        }

        public EffectDefinition build() {
            return new EffectDefinition(this);
        }
    }

    @Override
    public String toString() {
        return String.format("EffectDef{%s, type=%s, duration=%.1fs, stacks=%d}",
            id, type, baseDuration, maxStacks);
    }
}
