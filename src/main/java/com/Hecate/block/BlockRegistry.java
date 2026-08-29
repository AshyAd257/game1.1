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
    // 碰撞尺寸单独管理，与模型/纹理/硬度分开——不是所有方块都是满格实心立方体（如wood1这种细柱子）
    private final BlockShapeRegistry shapeRegistry = new BlockShapeRegistry();

    /**
     * 获取碰撞尺寸管理器（供CollisionManager查询方块实际碰撞盒尺寸）
     */
    public BlockShapeRegistry getShapeRegistry() {
        return shapeRegistry;
    }

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
     * 注册一族朝向性方块（如原木）：竖直变体使用baseId本身，另外两个水平朝向变体
     * 分别命名为 baseId_x / baseId_z（沿X轴/Z轴横放）。三者共享同一个模型文件，
     * 仅在放置时依据点击面法线（{@link Axis#fromFaceNormal}）选择使用哪一个。
     */
    public void registerDirectionalBlockFamily(String baseId, String name, boolean solid, float hardness, String modelPath) {
        registerBlock(new Block(baseId, name, solid, hardness, false, modelPath, Axis.Y, baseId));
        registerBlock(new Block(baseId + "_x", name, solid, hardness, false, modelPath, Axis.X, baseId));
        registerBlock(new Block(baseId + "_z", name, solid, hardness, false, modelPath, Axis.Z, baseId));
    }

    /**
     * 在某个朝向族里查找沿指定轴摆放的变体方块。
     */
    public Block getVariantForAxis(String orientationGroup, Axis axis) {
        for (Block block : blocks.values()) {
            if (axis == block.getAxis() && orientationGroup.equals(block.getOrientationGroup())) {
                return block;
            }
        }
        return null;
    }

    /**
     * 根据玩家手持的方块ID和放置时点击的面法线，解析出实际应该写入世界的方块ID。
     * 非方向性方块（orientationGroup为null）直接原样返回；方向性方块（如wood1）
     * 会依据法线换算出目标朝向轴，再在同一族里找到对应变体的ID。
     */
    public String resolvePlacementVariant(String heldBlockId, com.jme3.math.Vector3f faceNormal) {
        Block held = getBlock(heldBlockId);
        if (held == null || !held.isDirectional()) {
            return heldBlockId;
        }

        Axis targetAxis = Axis.fromFaceNormal(faceNormal);
        Block variant = getVariantForAxis(held.getOrientationGroup(), targetAxis);
        return variant != null ? variant.getId() : heldBlockId;
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
        registerDirectionalBlockFamily("wood1", "木柱1", true, 0.8f, "blocks/wood1.glb");
        shapeRegistry.registerDirectionalShape("wood1", 0.2f, 1.0f);
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
