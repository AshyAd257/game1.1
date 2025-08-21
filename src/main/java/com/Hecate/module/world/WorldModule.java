package com.Hecate.module.world;

import com.jme3.app.SimpleApplication;
import com.jme3.scene.Node;
import com.jme3.scene.Geometry;
import com.jme3.material.Material;
import com.jme3.math.Vector3f;
import com.jme3.scene.shape.Box;
import com.Hecate.module.AbstractGameModule;
import com.Hecate.module.Version;
import com.Hecate.world.ChunkManager;
import com.Hecate.world.ChunkPosition;
import com.Hecate.world.Chunk;
import com.Hecate.texture.BlockTextureManager;
import com.Hecate.block.BlockRegistry;

/**
 * 🌍 世界模块 - 管理纯净土方块世界
 */
public class WorldModule extends AbstractGameModule {
    private static final String MODULE_ID = "world-module";
    private static final Version MODULE_VERSION = new Version(1, 0, 0);

    private final SimpleApplication app;
    private Node worldNode;
    private ChunkManager chunkManager;
    private BlockTextureManager textureManager;
    private BlockRegistry blockRegistry;

    // 🎯 世界生成参数
    private static final int WORLD_SIZE = 10; // 10x10 区块的世界
    private static final int DIRT_LAYER_Y = 1; // 土方块层的Y坐标

    public WorldModule(SimpleApplication app) {
        this.app = app;
    }

    @Override
    public String getId() {
        return MODULE_ID;
    }

    @Override
    public Version getVersion() {
        return MODULE_VERSION;
    }

    @Override
    public void onInitialize() {
        System.out.println("🌍 世界模块: 初始化中...");

        // 创建世界节点
        worldNode = new Node("WorldNode");
        app.getRootNode().attachChild(worldNode);

        // 初始化纹理管理器
        textureManager = new BlockTextureManager(app.getAssetManager());

        // 🎯 初始化默认纹理（包括十字纹理）
        textureManager.initializeDefaultTextures();

        // 初始化方块注册表
        blockRegistry = BlockRegistry.getInstance();
        blockRegistry.initializeDefaultBlocks(textureManager);

        // 初始化区块管理器
        chunkManager = new ChunkManager(worldNode);

        System.out.println("✅ 世界模块: 基础组件初始化完成");
    }


    @Override
    public void onPostInitialize() {
        System.out.println("🌍 世界模块: 开始生成纯净土方块世界...");

        // 生成纯净的土方块世界
        generatePureDirtWorld();

        System.out.println("✅ 世界模块: 纯净土方块世界生成完成");
    }

    /**
     * 🎨 初始化十字纹理
     */
    private void initializeCrossTexture() {
        System.out.println("🎨 配置十字纹理...");

        // 将所有方块都配置为使用十字纹理
        String crossTexturePath = "Textures/blocks/cross.png"; // 你需要将十字图片放在这个路径

        textureManager.defineBlockTexture("dirt",
                com.Hecate.texture.BlockTextureDefinition.singleTexture(crossTexturePath));
        textureManager.defineBlockTexture("stone",
                com.Hecate.texture.BlockTextureDefinition.singleTexture(crossTexturePath));
        textureManager.defineBlockTexture("grass",
                com.Hecate.texture.BlockTextureDefinition.singleTexture(crossTexturePath));
        textureManager.defineBlockTexture("glass",
                com.Hecate.texture.BlockTextureDefinition.singleTexture(crossTexturePath));
        textureManager.defineBlockTexture("wood",
                com.Hecate.texture.BlockTextureDefinition.singleTexture(crossTexturePath));
        textureManager.defineBlockTexture("cobblestone",
                com.Hecate.texture.BlockTextureDefinition.singleTexture(crossTexturePath));

        System.out.println("✅ 十字纹理配置完成");
    }

    /**
     * 🌍 生成纯净的土方块世界
     */
    private void generatePureDirtWorld() {
        System.out.println("🌍 开始生成 " + WORLD_SIZE + "x" + WORLD_SIZE + " 的纯净土方块世界...");

        int blocksGenerated = 0;

        // 只在Y=0的区块层生成（因为我们只要Y=1的土方块）
        for (int chunkX = -WORLD_SIZE/2; chunkX < WORLD_SIZE/2; chunkX++) {
            for (int chunkZ = -WORLD_SIZE/2; chunkZ < WORLD_SIZE/2; chunkZ++) {
                // 创建区块位置（Y=0，因为土方块在Y=1）
                ChunkPosition position = new ChunkPosition(chunkX, 0, chunkZ);

                // 加载区块（会自动调用 fillWithTestPattern）
                Chunk chunk = chunkManager.loadChunk(position);

                // 渲染区块
                renderChunk(chunk);

                blocksGenerated += 16 * 16; // 每个区块16x16个土方块
            }
        }

        System.out.println("✅ 纯净土方块世界生成完成！");
        System.out.println("📊 统计信息:");
        System.out.println("   区块数量: " + (WORLD_SIZE * WORLD_SIZE));
        System.out.println("   土方块数量: " + blocksGenerated);
        System.out.println("   世界尺寸: " + (WORLD_SIZE * 16) + "x1x" + (WORLD_SIZE * 16) + " 方块");
    }

    /**
     * 🎨 渲染区块
     */
    private void renderChunk(Chunk chunk) {
        Node chunkNode = new Node("Chunk_" + chunk.getPosition().toString());

        Vector3f chunkWorldPos = chunk.getWorldPosition();

        // 遍历区块中的所有方块
        for (int x = 0; x < Chunk.SIZE; x++) {
            for (int y = 0; y < Chunk.SIZE; y++) {
                for (int z = 0; z < Chunk.SIZE; z++) {
                    String blockId = chunk.getBlockId(x, y, z);

                    // 只渲染非空气方块
                    if (!"air".equals(blockId)) {
                        Vector3f blockPos = new Vector3f(
                                chunkWorldPos.x + x,
                                chunkWorldPos.y + y,
                                chunkWorldPos.z + z
                        );

                        Geometry blockGeometry = createBlockGeometry(blockId, blockPos);
                        chunkNode.attachChild(blockGeometry);
                    }
                }
            }
        }

        // 将区块节点添加到世界
        worldNode.attachChild(chunkNode);
        chunk.setChunkNode(chunkNode);
        chunk.setClean();
    }

    /**
     * 🧊 创建方块几何体
     */
    private Geometry createBlockGeometry(String blockId, Vector3f position) {
        // 创建1x1x1的立方体
        Box box = new Box(0.5f, 0.5f, 0.5f);
        Geometry geometry = new Geometry("Block_" + blockId + "_" + position.toString(), box);

        // 应用十字纹理材质
        Material material = textureManager.createBlockMaterial(blockId);
        geometry.setMaterial(material);

        // 设置位置
        geometry.setLocalTranslation(position);

        return geometry;
    }

    @Override
    public void onUpdate(float tpf) {
        // 世界更新逻辑（如果需要）
    }

    @Override
    public void onDisable() {
        System.out.println("🌍 世界模块: 正在禁用");
        if (worldNode != null) {
            app.getRootNode().detachChild(worldNode);
        }
    }

    // 公共访问器方法
    public ChunkManager getChunkManager() {
        return chunkManager;
    }

    public Node getWorldNode() {
        return worldNode;
    }

    public BlockTextureManager getTextureManager() {
        return textureManager;
    }
}
