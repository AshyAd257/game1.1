package com.Hecate.block;

import com.Hecate.texture.BlockTextureManager;
import com.Hecate.texture.BlockTextureDefinition;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 管理游戏中所有方块类型的注册表
 */
public class BlockRegistry {
    private static BlockRegistry instance;
    private final Map<String, Block> blocks = new HashMap<>();

    // 私有构造函数，使用单例模式
    private BlockRegistry() {}

    /**
     * 获取注册表实例
     */
    public static synchronized BlockRegistry getInstance() {
        if (instance == null) {
            instance = new BlockRegistry();
        }
        return instance;
    }

    /**
     * 注册一个新方块类型
     */
    public void registerBlock(Block block) {
        blocks.put(block.getId(), block);
        System.out.println("🎮 注册方块: " + block.getId() + " (" + block.getName() + ")");
    }

    /**
     * 根据ID获取方块
     */
    public Block getBlock(String id) {
        return blocks.get(id);
    }

    /**
     * 获取所有注册的方块ID
     */
    public Set<String> getAllBlockIds() {
        Set<String> blockIds = blocks.keySet();
        System.out.println("🔍 BlockRegistry.getAllBlockIds() 返回: " + blockIds);
        return blockIds;
    }

    /**
     * 初始化默认方块 - 使用新的纹理系统
     * 这将在游戏启动时调用，注册所有基本方块类型
     */
    public void initializeDefaultBlocks(BlockTextureManager textureManager) {
        System.out.println("🎮 初始化默认方块...");

        // 定义方块纹理
        textureManager.defineBlockTexture("air",
                BlockTextureDefinition.singleTexture("textures/blocks/air.png"));

        textureManager.defineBlockTexture("dirt",
                BlockTextureDefinition.singleTexture("textures/blocks/dirt.png"));

        textureManager.defineBlockTexture("stone",
                BlockTextureDefinition.singleTexture("textures/blocks/stone.png"));

        // 草方块使用多面纹理
        textureManager.defineBlockTexture("grass",
                BlockTextureDefinition.threeTexture(
                        "textures/blocks/grass_top.png",    // 顶部
                        "textures/blocks/grass_side.png",   // 侧面
                        "textures/blocks/dirt.png"          // 底部
                ));

        textureManager.defineBlockTexture("glass",
                BlockTextureDefinition.singleTexture("textures/blocks/glass.png"));

        textureManager.defineBlockTexture("wood",
                BlockTextureDefinition.singleTexture("textures/blocks/wood.png"));

        textureManager.defineBlockTexture("cobblestone",
                BlockTextureDefinition.singleTexture("textures/blocks/cobblestone.png"));

        // 注册方块（使用简化的构造器）
        registerBlock(new Block("air", "空气", false, 0.0f, true));
        registerBlock(new Block("dirt", "泥土", true, 0.5f, false));
        registerBlock(new Block("stone", "石头", true, 1.5f, false));
        registerBlock(new Block("grass", "草方块", true, 0.6f, false));
        registerBlock(new Block("glass", "玻璃", true, 0.3f, true));
        registerBlock(new Block("wood", "木头", true, 0.8f, false));
        registerBlock(new Block("cobblestone", "鹅卵石", true, 2.0f, false));

        System.out.println("✅ 已注册 " + blocks.size() + " 种方块类型");
    }

    /**
     * 兼容性方法：使用旧的TextureManager
     * @deprecated 使用 initializeDefaultBlocks(BlockTextureManager) 代替
     */
    @Deprecated
    public void initializeDefaultBlocks(TextureManager textureManager) {
        System.out.println("⚠️ 使用了已废弃的 initializeDefaultBlocks(TextureManager)");
        System.out.println("🔄 请更新代码使用 BlockTextureManager");

        // 添加空气方块（特殊方块，通常是透明的）
        registerBlock(new Block("air", "空气", false, 0.0f,
                new BlockTexture("air", "textures/blocks/air.png", true)));

        // 注册基本方块类型
        registerBlock(new Block("dirt", "泥土", true, 0.5f,
                new BlockTexture("dirt", "textures/blocks/dirt.png", false)));

        registerBlock(new Block("stone", "石头", true, 1.5f,
                new BlockTexture("stone", "textures/blocks/stone.png", false)));

        registerBlock(new Block("grass", "草方块", true, 0.6f,
                new BlockTexture("grass", "textures/blocks/grass_top.png", false)));

        registerBlock(new Block("glass", "玻璃", true, 0.3f,
                new BlockTexture("glass", "textures/blocks/glass.png", true)));

        // 配置纹理（如果需要的话）
        for (Block block : blocks.values()) {
            if (block.getTexture() != null) {
                // 这里可以添加纹理加载逻辑
                System.out.println("🎨 配置方块纹理: " + block.getId());
            }
        }
    }
}
