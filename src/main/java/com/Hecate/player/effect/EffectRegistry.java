package com.Hecate.player.effect;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 效果注册表
 * 管理所有效果定义（模板）
 */
public class EffectRegistry {
    private static EffectRegistry instance;

    private final Map<String, EffectDefinition> effectDefinitions;

    private EffectRegistry() {
        effectDefinitions = new HashMap<>();
        registerDefaultEffects();
    }

    public static EffectRegistry getInstance() {
        if (instance == null) {
            instance = new EffectRegistry();
        }
        return instance;
    }

    /**
     * 注册默认效果定义
     */
    private void registerDefaultEffects() {
        // ==================== Buff ====================

        // 速度提升
        register(new EffectDefinition.Builder("speed_boost", EffectType.SPEED_BOOST)
                .duration(10.0f)
                .magnitude(1.5f)  // 1.5倍速度
                .maxStacks(3)
                .conflictsWith("slowness")
                .build());

        // 力量提升
        register(new EffectDefinition.Builder("strength", EffectType.STRENGTH)
                .duration(15.0f)
                .magnitude(1.3f)  // 1.3倍伤害
                .maxStacks(3)
                .conflictsWith("weakness")
                .build());

        // 生命恢复
        register(new EffectDefinition.Builder("regeneration", EffectType.REGENERATION)
                .duration(10.0f)
                .magnitude(2.0f)  // 每秒恢复2点生命
                .maxStacks(5)
                .conflictsWith("poison")
                .build());

        // 护盾
        register(new EffectDefinition.Builder("shield", EffectType.SHIELD)
                .duration(30.0f)
                .magnitude(20.0f)  // 20点护盾值
                .maxStacks(1)
                .refreshable(false)
                .build());

        // 跳跃提升
        register(new EffectDefinition.Builder("jump_boost", EffectType.JUMP_BOOST)
                .duration(15.0f)
                .magnitude(1.5f)  // 1.5倍跳跃高度
                .maxStacks(2)
                .build());

        // ==================== Debuff ====================

        // 中毒
        register(new EffectDefinition.Builder("poison", EffectType.POISON)
                .duration(8.0f)
                .magnitude(1.0f)  // 每秒1点伤害
                .maxStacks(5)
                .conflictsWith("regeneration")
                .build());

        // 减速
        register(new EffectDefinition.Builder("slowness", EffectType.SLOWNESS)
                .duration(6.0f)
                .magnitude(0.5f)  // 0.5倍速度
                .maxStacks(3)
                .conflictsWith("speed_boost")
                .build());

        // 虚弱
        register(new EffectDefinition.Builder("weakness", EffectType.WEAKNESS)
                .duration(10.0f)
                .magnitude(0.7f)  // 0.7倍伤害
                .maxStacks(2)
                .conflictsWith("strength")
                .build());

        // 燃烧
        register(new EffectDefinition.Builder("burning", EffectType.BURNING)
                .duration(5.0f)
                .magnitude(2.0f)  // 每秒2点伤害
                .maxStacks(1)
                .refreshable(true)
                .build());

        // 冰冻
        register(new EffectDefinition.Builder("frozen", EffectType.FROZEN)
                .duration(3.0f)
                .magnitude(0.3f)  // 0.3倍速度
                .maxStacks(1)
                .refreshable(false)
                .build());

        // 眩晕
        register(new EffectDefinition.Builder("stun", EffectType.STUN)
                .duration(2.0f)
                .magnitude(0.0f)  // 完全无法移动
                .maxStacks(1)
                .refreshable(false)
                .build());

        // ==================== 特殊效果 ====================

        // 无敌帧
        register(new EffectDefinition.Builder("invincible", EffectType.INVINCIBLE)
                .duration(1.0f)
                .maxStacks(1)
                .refreshable(false)
                .build());

        // ==================== 游戏专属永久Buff（Wave奖励） ====================

        // 射速提升（永久）- 对应 FIRE_RATE_UP
        register(new EffectDefinition.Builder("fire_rate_boost", EffectType.CUSTOM)
                .duration(Float.POSITIVE_INFINITY)  // 永久效果
                .magnitude(1.5f)  // 射速提升1.5倍
                .maxStacks(99)  // 可叠加
                .refreshable(false)
                .build());

        // 额外弹道（永久）- 对应 EXTRA_PROJECTILE
        register(new EffectDefinition.Builder("extra_projectile", EffectType.CUSTOM)
                .duration(Float.POSITIVE_INFINITY)
                .magnitude(1.0f)  // 每层+1弹道
                .maxStacks(99)
                .refreshable(false)
                .build());

        // 散射范围提升（永久）- 对应 SPREAD_RANGE_UP
        register(new EffectDefinition.Builder("spread_range_boost", EffectType.CUSTOM)
                .duration(Float.POSITIVE_INFINITY)
                .magnitude(1.5f)  // 散射角度1.5倍
                .maxStacks(99)
                .refreshable(false)
                .build());

        // 恢复速度提升（永久）- 对应 RECOVERY_SPEED_UP
        register(new EffectDefinition.Builder("recovery_boost", EffectType.CUSTOM)
                .duration(Float.POSITIVE_INFINITY)
                .magnitude(1.5f)  // 恢复速度1.5倍
                .maxStacks(99)
                .refreshable(false)
                .build());

        // 移动速度提升（永久）- 对应 MOVE_SPEED_UP
        register(new EffectDefinition.Builder("move_speed_boost", EffectType.CUSTOM)
                .duration(Float.POSITIVE_INFINITY)
                .magnitude(1.05f)  // 移动速度1.05倍
                .maxStacks(99)
                .refreshable(false)
                .build());
    }

    /**
     * 注册效果定义
     */
    public void register(EffectDefinition definition) {
        effectDefinitions.put(definition.getId(), definition);
    }

    /**
     * 获取效果定义
     */
    public EffectDefinition getDefinition(String id) {
        return effectDefinitions.get(id);
    }

    /**
     * 创建效果实例（工厂方法）
     */
    public ActiveEffect createEffect(String effectId) {
        EffectDefinition def = getDefinition(effectId);
        if (def == null) {
            throw new IllegalArgumentException("Unknown effect ID: " + effectId);
        }
        return new ActiveEffect(def);
    }

    /**
     * 创建效果实例（带自定义参数）
     */
    public ActiveEffect createEffect(String effectId, float duration, int stacks, Object source) {
        EffectDefinition def = getDefinition(effectId);
        if (def == null) {
            throw new IllegalArgumentException("Unknown effect ID: " + effectId);
        }
        return new ActiveEffect(def, duration, stacks, source);
    }

    /**
     * 获取所有效果ID
     */
    public Set<String> getAllEffectIds() {
        return effectDefinitions.keySet();
    }
}
