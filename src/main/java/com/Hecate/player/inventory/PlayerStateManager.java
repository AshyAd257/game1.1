package com.Hecate.player.inventory;

import com.Hecate.player.effect.PlayerEffectManager;
import com.Hecate.block.BlockRegistry;
import com.Hecate.weapon.WeaponRegistry;
import com.Hecate.item.Inventory;
import com.Hecate.item.ItemRegistry;

/**
 * 玩家状态管理器
 * 整合装备系统、效果系统、背包系统，提供统一的玩家状态访问接口
 */
public class PlayerStateManager {
    // 背包格数：4x4=16格，与backpack.png背景贴图的4x4格布局一一对应
    // （暂定固定值，不受装备影响——装备格数联动留给未来的据点/装备系统）
    private static final int BACKPACK_COLUMNS = 4;
    private static final int BACKPACK_SIZE = 16;

    private final PlayerEquipment equipment;
    private final PlayerEffectManager effectManager;
    private final Inventory backpack;

    public PlayerStateManager(BlockRegistry blockRegistry, WeaponRegistry weaponRegistry) {
        this.backpack = new Inventory(BACKPACK_SIZE, BACKPACK_COLUMNS, ItemRegistry.getInstance());
        this.equipment = new PlayerEquipment(blockRegistry, weaponRegistry);
        this.equipment.setBackpack(this.backpack);
        this.equipment.resetToDefault();
        this.effectManager = new PlayerEffectManager();
    }

    /**
     * 每帧更新
     */
    public void update(float deltaTime) {
        // 更新效果系统
        effectManager.update(deltaTime);
    }

    /**
     * 获取装备管理器
     */
    public PlayerEquipment getEquipment() {
        return equipment;
    }

    /**
     * 获取效果管理器
     */
    public PlayerEffectManager getEffectManager() {
        return effectManager;
    }

    /**
     * 获取玩家背包（通用格子容器，物品/战利品存放处，与快捷栏的方块/武器引用是两套独立的东西）
     */
    public Inventory getBackpack() {
        return backpack;
    }

    /**
     * 获取玩家当前移动速度倍率（受效果影响）
     */
    public float getSpeedMultiplier() {
        float multiplier = 1.0f;

        // 速度提升效果
        if (effectManager.hasEffectType(com.Hecate.player.effect.EffectType.SPEED_BOOST)) {
            multiplier *= effectManager.getTotalMagnitude(com.Hecate.player.effect.EffectType.SPEED_BOOST);
        }

        // 减速效果
        if (effectManager.hasEffectType(com.Hecate.player.effect.EffectType.SLOWNESS)) {
            multiplier *= effectManager.getTotalMagnitude(com.Hecate.player.effect.EffectType.SLOWNESS);
        }

        // 冰冻效果
        if (effectManager.hasEffectType(com.Hecate.player.effect.EffectType.FROZEN)) {
            multiplier *= effectManager.getTotalMagnitude(com.Hecate.player.effect.EffectType.FROZEN);
        }

        // 眩晕效果（完全无法移动）
        if (effectManager.hasEffectType(com.Hecate.player.effect.EffectType.STUN)) {
            multiplier = 0.0f;
        }

        return multiplier;
    }

    /**
     * 获取玩家当前伤害倍率（受效果影响）
     */
    public float getDamageMultiplier() {
        float multiplier = 1.0f;

        // 力量效果
        if (effectManager.hasEffectType(com.Hecate.player.effect.EffectType.STRENGTH)) {
            multiplier *= effectManager.getTotalMagnitude(com.Hecate.player.effect.EffectType.STRENGTH);
        }

        // 虚弱效果
        if (effectManager.hasEffectType(com.Hecate.player.effect.EffectType.WEAKNESS)) {
            multiplier *= effectManager.getTotalMagnitude(com.Hecate.player.effect.EffectType.WEAKNESS);
        }

        return multiplier;
    }

    /**
     * 检查玩家是否无敌
     */
    public boolean isInvincible() {
        return effectManager.hasEffectType(com.Hecate.player.effect.EffectType.INVINCIBLE);
    }

    /**
     * 检查玩家是否被眩晕
     */
    public boolean isStunned() {
        return effectManager.hasEffectType(com.Hecate.player.effect.EffectType.STUN);
    }

    /**
     * 检查玩家是否隐身
     */
    public boolean isInvisible() {
        return effectManager.hasEffectType(com.Hecate.player.effect.EffectType.INVISIBILITY);
    }

    /**
     * 获取指定效果的堆叠层数
     * @param effectId 效果ID
     * @return 堆叠层数，如果效果不存在返回0
     */
    public int getEffectStacks(String effectId) {
        var effect = effectManager.getEffect(effectId);
        return effect != null ? effect.getStacks() : 0;
    }

    /**
     * 获取玩家当前恢复速度倍率（受效果影响）
     * @return 恢复速度倍率
     */
    public float getRecoveryMultiplier() {
        float multiplier = 1.0f;

        // 再生效果
        if (effectManager.hasEffectType(com.Hecate.player.effect.EffectType.REGENERATION)) {
            multiplier *= effectManager.getTotalMagnitude(com.Hecate.player.effect.EffectType.REGENERATION);
        }

        return multiplier;
    }

    /**
     * 重置所有状态
     */
    public void reset() {
        equipment.resetToDefault();
        effectManager.clearAllEffects();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("PlayerState{\n");
        sb.append(equipment.toString()).append("\n");
        sb.append("  Active Effects: ").append(effectManager.getActiveEffects().size()).append("\n");
        for (var effect : effectManager.getActiveEffects()) {
            sb.append("    - ").append(effect).append("\n");
        }
        sb.append("}");
        return sb.toString();
    }
}
