package com.Hecate.block;

import com.Hecate.texture.BlockTextureDefaults;
import com.Hecate.texture.BlockTextureManager;
import com.Hecate.utils.LogUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Registry for block types that are available in the game.
 *
 * Refactored to support dependency injection while maintaining backward compatibility.
 *
 * Migration guide:
 * - Old code: BlockRegistry.getInstance()  (still works, returns default instance)
 * - New code: new BlockRegistry()  (for dependency injection)
 */
public class BlockRegistry {
    // Default instance for backward compatibility
    private static BlockRegistry defaultInstance;

    private final Map<String, Block> blocks = new HashMap<>();

    /**
     * Public constructor - allows creating independent instances for:
     * - Dependency injection
     * - Multiple worlds/save files
     * - Testing with isolated state
     * - Mod/plugin systems
     */
    public BlockRegistry() {
        // Intentionally public - allows multiple instances
    }

    /**
     * Retrieve the default instance (backward compatibility).
     *
     * @deprecated Use dependency injection instead: pass BlockRegistry through constructors.
     * This method is kept for gradual migration.
     */
    @Deprecated
    public static synchronized BlockRegistry getInstance() {
        if (defaultInstance == null) {
            defaultInstance = new BlockRegistry();
        }
        return defaultInstance;
    }

    /**
     * Get the default instance (preferred method name for clarity).
     * Returns the same instance as getInstance().
     */
    public static synchronized BlockRegistry getDefaultInstance() {
        return getInstance();
    }

    /**
     * Create a new independent instance.
     * Useful for testing, modding, or multi-world scenarios.
     */
    public static BlockRegistry createInstance() {
        return new BlockRegistry();
    }

    /**
     * Register a new block type.
     */
    public void registerBlock(Block block) {
        blocks.put(block.getId(), block);
    }

    /**
     * Look up a block by id.
     */
    public Block getBlock(String id) {
        return blocks.get(id);
    }

    /**
     * All registered block ids (for debugging or tooling).
     */
    public Set<String> getAllBlockIds() {
        Set<String> blockIds = blocks.keySet();
        LogUtils.debug(BlockRegistry.class, "getAllBlockIds(): " + blockIds);
        return blockIds;
    }

    /**
     * Register the built-in blocks and their texture definitions.
     */
    public void initializeDefaultBlocks(BlockTextureManager textureManager) {
        BlockTextureDefaults.registerAll(textureManager);

        registerBlock(new Block("air", "空气", false, 0.0f, true));
        registerBlock(new Block("dirt", "泥土1", true, 0.5f, false));
        registerBlock(new Block("dirt2", "泥土2", true, 0.5f, false));
        registerBlock(new Block("dirt3", "泥土3", true, 0.5f, false));
        registerBlock(new Block("dirt4", "泥土4", true, 0.5f, false));
        registerBlock(new Block("stone", "石头", true, 1.5f, false));
        registerBlock(new Block("grass", "草方块", true, 0.6f, false));
        registerBlock(new Block("glass", "玻璃", true, 0.3f, true));
        registerBlock(new Block("wood", "木头", true, 0.8f, false));
        registerBlock(new Block("cobblestone", "鹅卵石", true, 2.0f, false));
    }

    /**
     * Compatibility path for the legacy TextureManager.
     *
     * @deprecated Use {@link #initializeDefaultBlocks(BlockTextureManager)} instead.
     */
    @Deprecated
    public void initializeDefaultBlocks(TextureManager textureManager) {
        LogUtils.warning(BlockRegistry.class, "initializeDefaultBlocks(TextureManager) is deprecated.");
        LogUtils.warning(BlockRegistry.class, "Please migrate to BlockTextureManager.");

        registerBlock(new Block("air", "空气", false, 0.0f,
                new BlockTexture("air", "textures/blocks/air.png", true)));

        registerBlock(new Block("dirt", "泥土", true, 0.5f,
                new BlockTexture("dirt", "textures/blocks/dirt.png", false)));

        registerBlock(new Block("stone", "石头", true, 1.5f,
                new BlockTexture("stone", "textures/blocks/stone.png", false)));

        registerBlock(new Block("grass", "草方块", true, 0.6f,
                new BlockTexture("grass", "textures/blocks/grass_top.png", false)));

        registerBlock(new Block("glass", "玻璃", true, 0.3f,
                new BlockTexture("glass", "textures/blocks/glass.png", true)));

        for (Block block : blocks.values()) {
            if (block.getTexture() != null) {
                LogUtils.debug(BlockRegistry.class, "Configuring legacy texture for block: " + block.getId());
            }
        }
    }
}
