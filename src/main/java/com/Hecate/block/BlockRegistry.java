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
     * 注册一族半砖（slab）方块：一次调用自动生成该材质的全部7个变体
     * （BOTTOM/TOP/LEFT/RIGHT/FRONT/BACK六个单朝向 + 叠满整格的DOUBLE合并态）及其碰撞盒。
     *
     * 约定（保证"叠满整格后外观等价于完整方块"这个规则对任何新半砖材质自动成立，
     * 不依赖人工每次注册时小心复制）：
     * - 六个单朝向模型是generate_block_shapes.py生成的slab_bottom.glb等形状原型，
     *   其UV是在cube.glb同一套画布上按半格裁剪出来的（bottom/top、left/right、
     *   front/back三对分别在纵向/横向上正好互补拼满[0,1]，不重叠不留缝）。
     * - DOUBLE变体直接复用cube.glb（不是另外拼接两个半砖模型）+同一张texturePath，
     *   因为两者在数学上完全等价（同一张贴图，只是被裁成两半分别显示 vs 整张直接显示）,
     *   这样省去处理拼接缝隙/浮点误差的风险。
     * - texturePath只需要美术画一张完整方块贴图（和cube_uv_template.png画法一致），
     *   六个朝向和DOUBLE变体全部共用同一张图，不需要为半砖单独画缩小/裁切版本。
     *
     * @param baseId 材质分组key（如"xx"），最终注册的方块id是 baseId+"_bottom"等
     * @param name 显示名（六个朝向+DOUBLE共用同一个显示名，不单独区分）
     * @param texturePath 完整方块贴图路径（classpath相对路径）
     */
    public void registerSlabFamily(String baseId, String name, boolean solid, float hardness, String texturePath) {
        for (SlabOrientation orientation : new SlabOrientation[]{
                SlabOrientation.BOTTOM, SlabOrientation.TOP,
                SlabOrientation.LEFT, SlabOrientation.RIGHT,
                SlabOrientation.FRONT, SlabOrientation.BACK}) {
            String variantId = baseId + "_" + orientation.idSuffix();
            String modelPath = "blocks/slab_" + orientation.idSuffix() + ".glb";
            registerBlock(new Block(variantId, name, solid, hardness, modelPath, texturePath, baseId, orientation));
        }
        registerBlock(new Block(baseId + "_double", name, solid, hardness, "blocks/cube.glb", texturePath,
                baseId, SlabOrientation.DOUBLE));

        // 碰撞盒：单朝向是半格厚度+相应方向的偏移，DOUBLE是满格无偏移
        shapeRegistry.registerShapeWithOffset(baseId + "_bottom", 1.0f, 0.5f, 1.0f, 0f, -0.25f, 0f);
        shapeRegistry.registerShapeWithOffset(baseId + "_top", 1.0f, 0.5f, 1.0f, 0f, 0.25f, 0f);
        shapeRegistry.registerShapeWithOffset(baseId + "_left", 0.5f, 1.0f, 1.0f, -0.25f, 0f, 0f);
        shapeRegistry.registerShapeWithOffset(baseId + "_right", 0.5f, 1.0f, 1.0f, 0.25f, 0f, 0f);
        shapeRegistry.registerShapeWithOffset(baseId + "_back", 1.0f, 1.0f, 0.5f, 0f, 0f, -0.25f);
        shapeRegistry.registerShapeWithOffset(baseId + "_front", 1.0f, 1.0f, 0.5f, 0f, 0f, 0.25f);
        shapeRegistry.registerShape(baseId + "_double", 1.0f, 1.0f, 1.0f);
    }

    /**
     * 解析半砖的放置结果：目标格子为空气时返回对应朝向的单变体id；目标格子已有同族的
     * 互补朝向（如已有BOTTOM，本次又对着它放TOP）时返回DOUBLE合并态id；其他情况
     * （目标格已有非同族内容、或已是相同朝向重复放置、或已是DOUBLE）拒绝放置返回null。
     *
     * @param slabFamily 手持半砖的材质分组key（Block.getSlabFamily()）
     * @param requested 根据点击位置推断出的目标朝向
     * @param currentBlockId 目标格子当前的方块id
     */
    public String resolveSlabPlacement(String slabFamily, SlabOrientation requested, String currentBlockId) {
        if ("air".equals(currentBlockId)) {
            return slabFamily + "_" + requested.idSuffix();
        }

        Block current = getBlock(currentBlockId);
        if (current != null && current.isSlabPart() && slabFamily.equals(current.getSlabFamily())
                && current.getSlabOrientation().isComplementOf(requested)) {
            return slabFamily + "_double";
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

        registerBlock(new Block("air", "空气", false, 0.0f, true).setObtainable(false));
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
        // 半砖族"xx"：一次调用自动注册全部7个变体（6个单朝向+叠满整格的合并态）及碰撞盒，
        // 贴图路径实际文件在resources/blocks/下（不是Textures/blocks/）
        registerSlabFamily("xx", "半砖xx", true, 0.8f, "blocks/bluetest.png");
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
                new BlockTexture("air", "textures/blocks/air.png", true)).setObtainable(false));

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
