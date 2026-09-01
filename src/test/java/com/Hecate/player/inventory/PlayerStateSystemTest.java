package com.Hecate.player.inventory;

import com.Hecate.block.BlockRegistry;
import com.Hecate.item.ItemRegistry;
import com.Hecate.weapon.WeaponRegistry;
import com.Hecate.player.effect.EffectRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 玩家状态系统测试
 * 测试装备切换、效果应用、状态查询等功能
 */
public class PlayerStateSystemTest {
    private BlockRegistry blockRegistry;
    private WeaponRegistry weaponRegistry;
    private PlayerStateManager stateManager;

    @BeforeEach
    public void setup() {
        blockRegistry = BlockRegistry.createInstance();
        weaponRegistry = WeaponRegistry.getInstance();

        // 初始化测试用的注册表——必须传真正的BlockTextureManager实例（无参构造即可，
        // 不需要真实AssetManager）：传字面null会导致BlockTextureDefaults.registerAll内部
        // manager::defineBlockTexture直接NPE（根因在ItemRegistryAutoRegistrationTest里
        // 复现并确认过，不是框架bug，是调用方传参错误）。
        blockRegistry.initializeDefaultBlocks(new com.Hecate.texture.BlockTextureManager());

        // 必须在构造PlayerStateManager之前完成——PlayerStateManager构造函数会调用
        // PlayerEquipment.resetToDefault()往背包塞入"stone"等默认物品，若ItemRegistry
        // 查不到对应ItemDef会直接抛异常（与ApplicationContext.initializeModules()里
        // 的真实启动顺序保持一致，见该方法的注释）。这里用单例ItemRegistry.getInstance()
        // 而不是createInstance()，因为PlayerStateManager/Inventory内部固定查询单例。
        ItemRegistry.getInstance().registerFromBlocks(blockRegistry, new com.Hecate.texture.BlockTextureManager());

        stateManager = new PlayerStateManager(blockRegistry, weaponRegistry);
    }

    @Test
    public void testSlotSelection() {
        PlayerEquipment equipment = stateManager.getEquipment();

        // 测试槛位切换
        equipment.selectSlot(0);
        assertEquals(0, equipment.getSelectedSlot());

        equipment.selectSlot(4);
        assertEquals(4, equipment.getSelectedSlot());
    }

    @Test
    public void testScrollSlot() {
        PlayerEquipment equipment = stateManager.getEquipment();
        equipment.selectSlot(3);

        equipment.scrollSlot(1);
        assertEquals(4, equipment.getSelectedSlot());

        equipment.scrollSlot(-1);
        assertEquals(3, equipment.getSelectedSlot());
    }

    @Test
    public void testScrollSlotClampsAtBoundary() {
        PlayerEquipment equipment = stateManager.getEquipment();
        equipment.selectSlot(0);

        equipment.scrollSlot(-1); // 已经在第0格，往前滚不应该报错或回绕
        assertEquals(0, equipment.getSelectedSlot());
    }

    @Test
    public void testBlockEquipment() {
        PlayerEquipment equipment = stateManager.getEquipment();

        // resetToDefault()已经把stone放进了槛位0（见PlayerEquipment.resetToDefault）
        equipment.selectSlot(0);

        assertTrue(equipment.isHoldingBlock());
        assertFalse(equipment.isHoldingWeapon());
        assertFalse(equipment.isEmpty());

        assertEquals("stone", equipment.getCurrentBlock().getId());
    }

    @Test
    public void testWeaponEquipment() {
        PlayerEquipment equipment = stateManager.getEquipment();

        // 把steampunk_gun放进槛位1并选中——武器物品需要先在ItemRegistry里有对应的
        // obtainable武器定义（本测试的registerFromWeapons还没跑，直接用背包底层API
        // setSlot绕过校验放入即可，测的是isHoldingWeapon()的判定逻辑，不是完整的
        // 注册流程）
        stateManager.getBackpack().setSlot(1,
                new com.Hecate.item.ItemStack("scrap_metal", 1)); // 占位：先验证非武器物品的行为
        equipment.selectSlot(1);
        assertFalse(equipment.isHoldingWeapon());

        // 完整验证武器物品：注册steampunk_gun的武器定义后再放入
        ItemRegistry.getInstance().registerFromWeapons(WeaponRegistry.getInstance());
        stateManager.getBackpack().setSlot(2,
                new com.Hecate.item.ItemStack("steampunk_gun", 1));
        equipment.selectSlot(2);

        assertTrue(equipment.isHoldingWeapon());
        assertFalse(equipment.isHoldingBlock());
        assertFalse(equipment.isEmpty());
    }

    @Test
    public void testEmptyHand() {
        PlayerEquipment equipment = stateManager.getEquipment();

        stateManager.getBackpack().clearSlot(5);
        equipment.selectSlot(5);

        assertTrue(equipment.isEmpty());
        assertFalse(equipment.isHoldingWeapon());
        assertFalse(equipment.isHoldingBlock());
    }

    @Test
    public void testRemoveFromCurrentSlotConsumesCount() {
        PlayerEquipment equipment = stateManager.getEquipment();
        equipment.selectSlot(0); // stone x64（resetToDefault默认值）

        int removed = equipment.removeFromCurrentSlot(1);

        assertEquals(1, removed);
        assertEquals(63, stateManager.getBackpack().getSlot(0).getCount());
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
