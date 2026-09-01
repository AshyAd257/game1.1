package com.Hecate.item;

import com.Hecate.block.BlockRegistry;
import com.Hecate.texture.BlockTextureManager;
import com.Hecate.weapon.WeaponRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证ItemRegistry按BlockRegistry/WeaponRegistry自动生成ItemDef的行为（阶段1：
 * 纯数据管线，不涉及任何运行时手持/开火路径）。
 */
public class ItemRegistryAutoRegistrationTest {
    private ItemRegistry itemRegistry;
    private BlockRegistry blockRegistry;
    private BlockTextureManager textureManager;

    @BeforeEach
    public void setup() {
        itemRegistry = ItemRegistry.createInstance();
        blockRegistry = BlockRegistry.createInstance();
        // 传入真正的BlockTextureManager实例（无参构造，测试不需要真实AssetManager）——
        // 之前误传字面null会导致BlockTextureDefaults.registerAll内部manager::defineBlockTexture
        // 直接NPE，这不是框架的bug，是调用方传参错误。
        textureManager = new BlockTextureManager();
        blockRegistry.initializeDefaultBlocks(textureManager);
    }

    @Test
    public void testObtainableBlockGetsRegistered() {
        itemRegistry.registerFromBlocks(blockRegistry, textureManager);

        assertTrue(itemRegistry.isValidItem("stone"));
        ItemDef def = itemRegistry.getItemDef("stone");
        assertEquals("石头", def.getName());
        assertTrue(def.hasBlockId());
        assertEquals("stone", def.getBlockId());
        assertFalse(def.hasWeaponId());
    }

    @Test
    public void testAirIsExcludedFromAutoRegistration() {
        itemRegistry.registerFromBlocks(blockRegistry, textureManager);

        assertFalse(itemRegistry.isValidItem("air"));
    }

    @Test
    public void testGrassIconResolvesToTopTexture() {
        itemRegistry.registerFromBlocks(blockRegistry, textureManager);

        ItemDef def = itemRegistry.getItemDef("grass");
        assertNotNull(def);
        assertEquals("textures/blocks/grass_top.png", def.getIconPath());
    }

    @Test
    public void testSingleTextureBlockIconResolves() {
        itemRegistry.registerFromBlocks(blockRegistry, textureManager);

        ItemDef def = itemRegistry.getItemDef("stone");
        assertEquals("textures/blocks/stone.png", def.getIconPath());
    }

    @Test
    public void testObtainableWeaponGetsRegistered() {
        WeaponRegistry weaponRegistry = WeaponRegistry.getInstance();
        itemRegistry.registerFromWeapons(weaponRegistry);

        assertTrue(itemRegistry.isValidItem("steampunk_gun"));
        ItemDef def = itemRegistry.getItemDef("steampunk_gun");
        assertEquals("蒸汽朋克枪", def.getName());
        assertTrue(def.hasWeaponId());
        assertEquals("steampunk_gun", def.getWeaponId());
        assertFalse(def.hasBlockId());

        assertTrue(itemRegistry.isValidItem("sniper_rifle"));
    }

    @Test
    public void testUnimplementedWeaponsAreExcluded() {
        WeaponRegistry weaponRegistry = WeaponRegistry.getInstance();
        itemRegistry.registerFromWeapons(weaponRegistry);

        // smg_01/flame_thrower目前没有对应的Weapon子类实现（WeaponFactory查不到），
        // obtainable默认false，不该被自动注册——否则玩家会拿到一把打不出子弹的哑武器
        assertFalse(itemRegistry.isValidItem("smg_01"));
        assertFalse(itemRegistry.isValidItem("flame_thrower"));
    }

    @Test
    public void testBlockObtainableFlagDefaultsTrue() {
        com.Hecate.block.Block customBlock = new com.Hecate.block.Block("test_block", "测试方块", true, 1.0f, false);
        assertTrue(customBlock.isObtainable());
    }

    @Test
    public void testBlockSetObtainableFalseExcludesFromRegistration() {
        BlockRegistry customRegistry = BlockRegistry.createInstance();
        customRegistry.registerBlock(
                new com.Hecate.block.Block("bedrock", "基岩", true, 100f, false).setObtainable(false));

        ItemRegistry freshItemRegistry = ItemRegistry.createInstance();
        freshItemRegistry.registerFromBlocks(customRegistry, textureManager);

        assertFalse(freshItemRegistry.isValidItem("bedrock"));
    }
}
