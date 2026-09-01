package com.Hecate.item;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 物品注册表：集中管理所有 {@link ItemDef}，运行时通过id查询。
 * <p>与 {@code BlockRegistry}/{@code WeaponRegistry} 同一种"表格文件"模式：
 * 支持单例（{@link #getInstance()}，向后兼容/主游戏默认用法）与独立实例
 * （{@link #createInstance()}，用于测试隔离）。
 */
public class ItemRegistry {
    private static ItemRegistry defaultInstance;

    private final Map<String, ItemDef> items = new HashMap<>();

    /**
     * 公开构造器——允许创建独立实例（测试隔离/未来的mod系统）。
     */
    public ItemRegistry() {
    }

    public static synchronized ItemRegistry getInstance() {
        if (defaultInstance == null) {
            defaultInstance = new ItemRegistry();
            defaultInstance.registerDefaults();
        }
        return defaultInstance;
    }

    public static ItemRegistry createInstance() {
        return new ItemRegistry();
    }

    public void registerItem(ItemDef itemDef) {
        items.put(itemDef.getId(), itemDef);
    }

    public ItemDef getItemDef(String id) {
        return items.get(id);
    }

    public boolean isValidItem(String id) {
        return items.containsKey(id);
    }

    public Set<String> getAllItemIds() {
        return items.keySet();
    }

    /**
     * 注册占位测试物品，供 /giveitem 调试命令与早期背包UI联调使用。
     * <p>正式的物品图鉴（战利品、镶嵌件等）应在此基础上按需扩展，而不是替换这个方法——
     * 这里只保留"能跑起来的最小集合"。
     */
    public void registerDefaults() {
        registerItem(new ItemDef("scrap_metal", "碎金属", "textures/ui/inventory_grid.png", 64));
        registerItem(new ItemDef("herb_common", "常见草药", "textures/ui/inventory_grid.png", 20));
        registerItem(new ItemDef("gem_shard", "晶体碎片", "textures/ui/inventory_grid.png", 10));

        // Gun1（蒸汽朋克枪）：装备/gun1时自动放入背包，图标横向占2格（贴图44x22，2:1比例），
        // 不可堆叠（同时只能装备一把）。见PlayerCombatController.equipGun1/unequipGun1。
        // 注意：这是Gun1独立老路径专用的物品id，与下面registerFromWeapons()按
        // WeaponRegistry自动生成的"steampunk_gun"是两个不同的id，本次不合并（断开
        // Gun1/Gun2独立路径是后续阶段的工作，这里保持现状零行为变化）。
        registerItem(new ItemDef("gun1", "蒸汽朋克枪", "textures/ui/Gun1.png", 1, 2, 1));
    }

    /**
     * 遍历 {@link com.Hecate.block.BlockRegistry} 里所有已注册且
     * {@link com.Hecate.block.Block#isObtainable()} 为true的方块，自动生成对应ItemDef
     * ——不需要为每个方块手写一条ItemDef。已存在同id的ItemDef会被覆盖（若某个方块id
     * 同时也被手写注册过，以自动生成的为准，因为它反映的是方块的最新真实数据）。
     * <p>图标路径取方块纹理的代表面：单一纹理直接用；三面纹理（如grass）取顶面纹理。
     * 没有纹理定义（罕见）的方块图标留空，不阻断整体注册流程。
     * @param textureManager 纹理定义查询源。注意：方块的纹理定义存在
     *        {@link com.Hecate.texture.BlockTextureManager} 内部按id索引的表里，
     *        不在 {@link com.Hecate.block.Block} 对象本身——{@code Block.getTexture()}
     *        对所有通过 {@code BlockRegistry.initializeDefaultBlocks(BlockTextureManager)}
     *        注册的方块永远返回null，只有旧的 {@code initializeDefaultBlocks(TextureManager)}
     *        兼容路径才会把BlockTexture塞进Block对象——不能查Block本身。
     */
    public void registerFromBlocks(com.Hecate.block.BlockRegistry blockRegistry,
                                    com.Hecate.texture.BlockTextureManager textureManager) {
        for (String blockId : blockRegistry.getAllBlockIds()) {
            com.Hecate.block.Block block = blockRegistry.getBlock(blockId);
            if (block == null || !block.isObtainable()) {
                continue;
            }
            String iconPath = resolveBlockIconPath(blockId, textureManager);
            registerItem(new ItemDef(blockId, block.getName(), iconPath, 64)
                    .setBlockId(blockId));
        }
    }

    private String resolveBlockIconPath(String blockId, com.Hecate.texture.BlockTextureManager textureManager) {
        if (textureManager == null) {
            return null;
        }
        com.Hecate.texture.BlockTextureDefinition definition = textureManager.getTextureDefinition(blockId);
        if (definition == null) {
            return null;
        }
        switch (definition.getType()) {
            case THREE_TEXTURE:
                return definition.getTopTexture();
            case SINGLE:
            default:
                return definition.getSingleTexture();
        }
    }

    /**
     * 遍历 {@link WeaponRegistry} 里所有已注册且
     * {@link com.Hecate.weapon.WeaponDefinition#isObtainable()} 为true的武器定义，
     * 自动生成对应ItemDef。obtainable=false的武器（如smg_01/flame_thrower，目前还没有
     * 对应的Weapon子类实现，见WeaponFactory）不会被注册——避免玩家拿到一把"看起来是枪
     * 但左键没反应"的哑物品。
     * <p>图标路径目前统一留空（武器图标美术资源不在本次范围内）。
     */
    public void registerFromWeapons(com.Hecate.weapon.WeaponRegistry weaponRegistry) {
        for (String weaponId : weaponRegistry.getAllWeaponIds()) {
            com.Hecate.weapon.WeaponDefinition def = weaponRegistry.getWeaponDef(weaponId);
            if (def == null || !def.isObtainable()) {
                continue;
            }
            registerItem(new ItemDef(weaponId, def.getDisplayName(), null, 1)
                    .setWeaponId(weaponId));
        }
    }
}
