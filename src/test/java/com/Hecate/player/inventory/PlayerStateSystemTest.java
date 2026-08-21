package com.Hecate.player.inventory;

import com.Hecate.block.BlockRegistry;
import com.Hecate.weapon.WeaponRegistry;
import com.Hecate.player.effect.EffectRegistry;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * 玩家状态系统测试
 * 测试装备切换、效果应用、状态查询等功能
 */
public class PlayerStateSystemTest {
    private BlockRegistry blockRegistry;
    private WeaponRegistry weaponRegistry;
    private PlayerStateManager stateManager;

    @Before
    public void setup() {
        blockRegistry = BlockRegistry.createInstance();
        weaponRegistry = WeaponRegistry.getInstance();
        stateManager = new PlayerStateManager(blockRegistry, weaponRegistry);

        // 初始化测试用的注册表
        blockRegistry.initializeDefaultBlocks(null);
    }

    @Test
    public void testHotbarSlotSelection() {
        PlayerEquipment equipment = stateManager.getEquipment();

        // 测试槽位切换
        equipment.selectHotbarSlot(0);
        assertEquals(0, equipment.getSelectedSlot());

        equipment.selectHotbarSlot(4);
        assertEquals(4, equipment.getSelectedSlot());
    }

    @Test
    public void testBlockEquipment() {
        PlayerEquipment equipment = stateManager.getEquipment();

        // 设置方块到槽位并选中
        equipment.setHotbarSlot(0, HeldItem.block("stone"));
        equipment.selectHotbarSlot(0);

        assertTrue(equipment.isHoldingBlock());
        assertFalse(equipment.isHoldingWeapon());
        assertFalse(equipment.isEmpty());

        assertEquals("stone", equipment.getCurrentBlock().getId());
    }

    @Test
    public void testWeaponEquipment() {
        PlayerEquipment equipment = stateManager.getEquipment();

        // 设置武器到槽位并选中
        equipment.setHotbarSlot(1, HeldItem.weapon("smg_01"));
        equipment.selectHotbarSlot(1);

        assertTrue(equipment.isHoldingWeapon());
        assertFalse(equipment.isHoldingBlock());
        assertFalse(equipment.isEmpty());
    }

    @Test
    public void testEmptyHand() {
        PlayerEquipment equipment = stateManager.getEquipment();

        // 设置空手
        equipment.setHotbarSlot(2, HeldItem.empty());
        equipment.selectHotbarSlot(2);

        assertTrue(equipment.isEmpty());
        assertFalse(equipment.isHoldingWeapon());
        assertFalse(equipment.isHoldingBlock());
    }

    @Test
    public void testEffectApplication() {
        var effectManager = stateManager.getEffectManager();

        // 应用速度提升效果
        assertTrue(effectManager.applyEffect("speed_boost"));
        assertTrue(effectManager.hasEffect("speed_boost"));

        // 检查速度倍率
        float speedMultiplier = stateManager.getSpeedMultiplier();
        assertTrue(speedMultiplier > 1.0f);
    }

    @Test
    public void testConflictingEffects() {
        var effectManager = stateManager.getEffectManager();

        // 应用速度提升
        effectManager.applyEffect("speed_boost");
        assertTrue(effectManager.hasEffect("speed_boost"));

        // 应用减速（应该移除速度提升）
        effectManager.applyEffect("slowness");
        assertFalse(effectManager.hasEffect("speed_boost"));
        assertTrue(effectManager.hasEffect("slowness"));
    }

    @Test
    public void testEffectStacking() {
        var effectManager = stateManager.getEffectManager();

        // 应用多次中毒效果
        effectManager.applyEffect("poison");
        var effect1 = effectManager.getEffect("poison");
        int stacks1 = effect1.getStacks();

        effectManager.applyEffect("poison");
        var effect2 = effectManager.getEffect("poison");
        int stacks2 = effect2.getStacks();

        assertEquals(stacks1 + 1, stacks2);
    }

    @Test
    public void testEffectExpiration() throws InterruptedException {
        var effectManager = stateManager.getEffectManager();

        // 应用短时效果
        effectManager.applyEffect(
            EffectRegistry.getInstance().createEffect("stun", 0.1f, 1, null)
        );
        assertTrue(effectManager.hasEffect("stun"));

        // 模拟时间流逝
        stateManager.update(0.2f);

        // 效果应该过期
        assertFalse(effectManager.hasEffect("stun"));
    }

    @Test
    public void testInvincibilityCheck() {
        var effectManager = stateManager.getEffectManager();

        assertFalse(stateManager.isInvincible());

        effectManager.applyEffect("invincible");
        assertTrue(stateManager.isInvincible());
    }

    @Test
    public void testDamageMultiplier() {
        var effectManager = stateManager.getEffectManager();

        // 初始倍率为 1.0
        assertEquals(1.0f, stateManager.getDamageMultiplier(), 0.01f);

        // 应用力量效果
        effectManager.applyEffect("strength");
        assertTrue(stateManager.getDamageMultiplier() > 1.0f);

        // 应用虚弱效果（应该移除力量）
        effectManager.applyEffect("weakness");
        assertTrue(stateManager.getDamageMultiplier() < 1.0f);
    }

    @Test
    public void testStateReset() {
        var effectManager = stateManager.getEffectManager();

        // 应用一些效果
        effectManager.applyEffect("speed_boost");
        effectManager.applyEffect("poison");

        // 重置状态
        stateManager.reset();

        // 所有效果应该被清除
        assertEquals(0, effectManager.getActiveEffects().size());
    }
}
