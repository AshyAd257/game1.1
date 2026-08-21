package com.Hecate.player.effect;

/**
 * 活跃效果实例
 * 表示玩家身上正在生效的一个 Buff/Debuff
 */
public class ActiveEffect {
    private final EffectDefinition definition;
    private float remainingTime;  // 剩余持续时间（秒）
    private int stacks;           // 当前堆叠层数
    private Object source;        // 效果来源（可选，如施加者、物品等）

    public ActiveEffect(EffectDefinition definition) {
        this(definition, definition.getBaseDuration(), 1, null);
    }

    public ActiveEffect(EffectDefinition definition, float duration, int stacks, Object source) {
        this.definition = definition;
        this.remainingTime = duration;
        this.stacks = Math.min(stacks, definition.getMaxStacks() > 0 ? definition.getMaxStacks() : stacks);
        this.source = source;
    }

    /**
     * 每帧更新效果
     * @param deltaTime 时间增量（秒）
     * @return 是否已过期
     */
    public boolean tick(float deltaTime) {
        remainingTime -= deltaTime;
        return remainingTime <= 0;
    }

    /**
     * 应用效果时调用（首次施加）
     */
    public void onApply() {
        // 子类可覆盖此方法实现具体效果
    }

    /**
     * 每帧效果生效时调用
     */
    public void onTick(float deltaTime) {
        // 子类可覆盖此方法实现持续效果（如中毒伤害）
    }

    /**
     * 移除效果时调用
     */
    public void onRemove() {
        // 子类可覆盖此方法实现清理逻辑
    }

    /**
     * 刷新效果持续时间
     */
    public void refresh() {
        if (definition.isRefreshable()) {
            remainingTime = definition.getBaseDuration();
        }
    }

    /**
     * 增加堆叠层数
     * @return 是否成功增加
     */
    public boolean addStack(int amount) {
        int maxStacks = definition.getMaxStacks();
        if (maxStacks == 0) {
            return false;  // 不可堆叠
        }
        if (maxStacks > 0 && stacks >= maxStacks) {
            return false;  // 已达最大层数
        }
        stacks += amount;
        if (maxStacks > 0) {
            stacks = Math.min(stacks, maxStacks);
        }
        return true;
    }

    // Getters
    public EffectDefinition getDefinition() { return definition; }
    public String getId() { return definition.getId(); }
    public EffectType getType() { return definition.getType(); }
    public float getRemainingTime() { return remainingTime; }
    public int getStacks() { return stacks; }
    public Object getSource() { return source; }

    /**
     * 获取效果强度（基础强度 × 堆叠层数）
     */
    public float getEffectiveMagnitude() {
        return definition.getMagnitude() * stacks;
    }

    @Override
    public String toString() {
        return String.format("ActiveEffect{%s, %.1fs, stacks=%d}",
            definition.getId(), remainingTime, stacks);
    }
}
