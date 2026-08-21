package com.Hecate.player.effect;

import java.util.*;

/**
 * 玩家效果管理器
 * 管理玩家身上所有活跃的 Buff/Debuff
 */
public class PlayerEffectManager {
    private final Map<String, ActiveEffect> activeEffects;
    private final List<EffectListener> listeners;

    public PlayerEffectManager() {
        this.activeEffects = new HashMap<>();
        this.listeners = new ArrayList<>();
    }

    /**
     * 添加效果监听器
     */
    public void addListener(EffectListener listener) {
        listeners.add(listener);
    }

    /**
     * 移除效果监听器
     */
    public void removeListener(EffectListener listener) {
        listeners.remove(listener);
    }

    /**
     * 应用效果到玩家
     */
    public boolean applyEffect(String effectId) {
        return applyEffect(EffectRegistry.getInstance().createEffect(effectId));
    }

    /**
     * 应用效果实例到玩家
     */
    public boolean applyEffect(ActiveEffect effect) {
        String effectId = effect.getId();
        EffectDefinition def = effect.getDefinition();

        // 检查冲突效果
        for (String conflictId : def.getConflictsWith()) {
            if (activeEffects.containsKey(conflictId)) {
                removeEffect(conflictId);  // 移除冲突效果
            }
        }

        // 如果效果已存在
        if (activeEffects.containsKey(effectId)) {
            ActiveEffect existing = activeEffects.get(effectId);

            // 尝试堆叠
            if (existing.addStack(1)) {
                notifyEffectStacked(existing);
                return true;
            }

            // 尝试刷新持续时间
            if (def.isRefreshable()) {
                existing.refresh();
                notifyEffectRefreshed(existing);
                return true;
            }

            return false;  // 无法堆叠也无法刷新
        }

        // 添加新效果
        activeEffects.put(effectId, effect);
        effect.onApply();
        notifyEffectApplied(effect);
        return true;
    }

    /**
     * 移除效果
     */
    public boolean removeEffect(String effectId) {
        ActiveEffect effect = activeEffects.remove(effectId);
        if (effect != null) {
            effect.onRemove();
            notifyEffectRemoved(effect);
            return true;
        }
        return false;
    }

    /**
     * 清除所有效果
     */
    public void clearAllEffects() {
        for (ActiveEffect effect : new ArrayList<>(activeEffects.values())) {
            removeEffect(effect.getId());
        }
    }

    /**
     * 清除所有负面效果
     */
    public void clearHarmfulEffects() {
        List<String> toRemove = new ArrayList<>();
        for (ActiveEffect effect : activeEffects.values()) {
            if (effect.getType().isHarmful()) {
                toRemove.add(effect.getId());
            }
        }
        for (String effectId : toRemove) {
            removeEffect(effectId);
        }
    }

    /**
     * 每帧更新所有效果
     */
    public void update(float deltaTime) {
        List<String> expired = new ArrayList<>();

        for (ActiveEffect effect : activeEffects.values()) {
            // 调用效果的tick方法
            effect.onTick(deltaTime);

            // 更新剩余时间
            if (effect.tick(deltaTime)) {
                expired.add(effect.getId());
            }
        }

        // 移除过期效果
        for (String effectId : expired) {
            removeEffect(effectId);
        }
    }

    /**
     * 检查是否有某个效果
     */
    public boolean hasEffect(String effectId) {
        return activeEffects.containsKey(effectId);
    }

    /**
     * 检查是否有某类型的效果
     */
    public boolean hasEffectType(EffectType type) {
        for (ActiveEffect effect : activeEffects.values()) {
            if (effect.getType() == type) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取效果实例
     */
    public ActiveEffect getEffect(String effectId) {
        return activeEffects.get(effectId);
    }

    /**
     * 获取所有活跃效果
     */
    public Collection<ActiveEffect> getActiveEffects() {
        return Collections.unmodifiableCollection(activeEffects.values());
    }

    /**
     * 获取所有正面效果
     */
    public List<ActiveEffect> getBeneficialEffects() {
        List<ActiveEffect> beneficial = new ArrayList<>();
        for (ActiveEffect effect : activeEffects.values()) {
            if (effect.getType().isBeneficial()) {
                beneficial.add(effect);
            }
        }
        return beneficial;
    }

    /**
     * 获取所有负面效果
     */
    public List<ActiveEffect> getHarmfulEffects() {
        List<ActiveEffect> harmful = new ArrayList<>();
        for (ActiveEffect effect : activeEffects.values()) {
            if (effect.getType().isHarmful()) {
                harmful.add(effect);
            }
        }
        return harmful;
    }

    /**
     * 获取某类型效果的总强度（所有同类效果的叠加）
     */
    public float getTotalMagnitude(EffectType type) {
        float total = 0;
        for (ActiveEffect effect : activeEffects.values()) {
            if (effect.getType() == type) {
                total += effect.getEffectiveMagnitude();
            }
        }
        return total;
    }

    // ==================== 通知监听器 ====================

    private void notifyEffectApplied(ActiveEffect effect) {
        for (EffectListener listener : listeners) {
            listener.onEffectApplied(effect);
        }
    }

    private void notifyEffectRemoved(ActiveEffect effect) {
        for (EffectListener listener : listeners) {
            listener.onEffectRemoved(effect);
        }
    }

    private void notifyEffectStacked(ActiveEffect effect) {
        for (EffectListener listener : listeners) {
            listener.onEffectStacked(effect);
        }
    }

    private void notifyEffectRefreshed(ActiveEffect effect) {
        for (EffectListener listener : listeners) {
            listener.onEffectRefreshed(effect);
        }
    }

    /**
     * 效果监听器接口
     */
    public interface EffectListener {
        default void onEffectApplied(ActiveEffect effect) {}
        default void onEffectRemoved(ActiveEffect effect) {}
        default void onEffectStacked(ActiveEffect effect) {}
        default void onEffectRefreshed(ActiveEffect effect) {}
    }
}
