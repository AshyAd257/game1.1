package com.Hecate.module.world;

import com.jme3.app.SimpleApplication;
import com.jme3.scene.Node;
import com.jme3.scene.Geometry;
import com.jme3.material.Material;
import com.jme3.math.Vector3f;
import com.jme3.scene.shape.Box;
import com.jme3.scene.BatchNode;
import com.Hecate.module.AbstractGameModule;
import com.Hecate.module.Version;
import com.Hecate.world.ChunkManager;
import com.Hecate.world.ChunkPosition;
import com.Hecate.world.Chunk;
import com.Hecate.world.TerrainMaterialFactory;
import com.Hecate.texture.BlockTextureManager;
import com.Hecate.block.BlockRegistry;
import com.Hecate.utils.LogUtils;

/**
 * 世界模块 - 管理纯净土方块世界
 */
public class WorldModule extends AbstractGameModule {
    private static final String MODULE_ID = "world-module";
    private static final Version MODULE_VERSION = new Version(1, 0, 0);

    private final SimpleApplication app;
    private Node worldNode;
    private ChunkManager chunkManager;
    private BlockTextureManager textureManager;
    private BlockRegistry blockRegistry;
    private TerrainMaterialFactory terrainMaterialFactory;

    // 动态世界加载参数
    private static final int RENDER_DISTANCE = 4; // 渲染距离（区块数）- 扩大到±4覆盖50米测试区域
    private static final int UNLOAD_DISTANCE = 6; // 卸载距离（区块数）
    private static final int DIRT_LAYER_Y = 1; // 土方块层的Y坐标
    private static final int MAX_CHUNKS_PER_FRAME = 8; // 每帧最多加载区块数
    private static final float UPDATE_INTERVAL = 0.2f; // 更新间隔（秒）
    private static final int INK_EXPAND_RADIUS = 2; // 墨水扩展预加载半径（区块数）

    // 玩家位置追踪
    private Vector3f lastPlayerChunkPos = null;
    private com.Hecate.player.PlayerController playerController;
    private float updateTimer = 0f; // 更新计时器
    private boolean initialLoadComplete = false; // 初始加载是否完成
    private float gameTime = 0f; // 游戏时间（用于着色器动画）

    // 摄像机方块剔除系统
    private com.Hecate.camera.CameraBlockCulling cameraBlockCulling;

    // 地形边缘填充网格下探到的绝对世界Y坐标。主世界地形可被挖掘任意深度，
    // 需要下探很深（-300）避免看穿地表；竞技场是悬浮平台，边缘外是虚空，
    // 用较浅的深度（刚好遮住台面底部）避免呈现"深坑/峡谷"的视觉效果。
    private static final float DEFAULT_EDGE_FILL_DEPTH = -300.0f;
    private float edgeFillDepth = DEFAULT_EDGE_FILL_DEPTH;

    /**
     * 构造函数（依赖注入）
     *
     * @param app SimpleApplication 实例
     * @param blockRegistry 方块注册表实例（通过依赖注入传入）
     */
    public WorldModule(SimpleApplication app, BlockRegistry blockRegistry) {
        this.app = app;
        this.blockRegistry = blockRegistry;
    }

    /**
     * 构造函数（向后兼容 - 使用默认注册表）
     *
     * @param app SimpleApplication 实例
     * @deprecated 推荐使用 {@link #WorldModule(SimpleApplication, BlockRegistry)} 进行依赖注入
     */
    @Deprecated
    public WorldModule(SimpleApplication app) {
        this.app = app;
        this.blockRegistry = null; // 将在 onInitialize 中初始化为默认实例
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
        // 创建世界节点
        worldNode = new Node("WorldNode");
        app.getRootNode().attachChild(worldNode);

        // 初始化纹理管理器
        textureManager = new BlockTextureManager(app.getAssetManager());

        // 初始化默认纹理（包括十字纹理）
        textureManager.initializeDefaultTextures();

        // 初始化方块注册表（如果未通过构造函数注入，则使用默认实例）
        if (blockRegistry == null) {
            blockRegistry = BlockRegistry.getInstance();
        }
        blockRegistry.initializeDefaultBlocks(textureManager);

        // 初始化区块管理器
        chunkManager = new ChunkManager(worldNode);

        // 初始化地形材质工厂
        terrainMaterialFactory = new TerrainMaterialFactory(app.getAssetManager());

    }

    @Override
    public void onPostInitialize() {
        // 不再预生成世界，等待onUpdate()中根据玩家位置动态加载

        // 初始化摄像机方块剔除系统
        cameraBlockCulling = new com.Hecate.camera.CameraBlockCulling(app.getCamera(), chunkManager);
    }

    /**
     * 渲染区块（优化版 - 使用网格合并 + 地形网格）
     */
    private void renderChunk(Chunk chunk) {
        Node chunkNode = new Node("Chunk_" + chunk.getPosition().toString());
        Vector3f chunkWorldPos = chunk.getWorldPosition();

        // === 地形网格渲染 ===
        if (chunk.hasTerrainData()) {
            com.jme3.scene.Mesh terrainMesh = com.Hecate.world.TerrainMeshGenerator.generateSurfaceMesh(chunk, chunkWorldPos);
            if (terrainMesh != null) {
                Geometry terrainGeom = new Geometry("Terrain_" + chunk.getPosition().toString(), terrainMesh);

                // 使用自定义着色器材质
                Material terrainMat = terrainMaterialFactory.getMaterial(com.Hecate.world.TerrainMaterial.DIRT);
                terrainGeom.setMaterial(terrainMat);
                terrainGeom.setQueueBucket(com.jme3.renderer.queue.RenderQueue.Bucket.Opaque);

                // 【关键修复】启用阴影接收
                terrainGeom.setShadowMode(com.jme3.renderer.queue.RenderQueue.ShadowMode.CastAndReceive);

                chunkNode.attachChild(terrainGeom);
            }

            // 生成边缘填充网格（智能检测开放边缘，防止看到天空）
            com.jme3.scene.Mesh edgeFillMesh = com.Hecate.world.TerrainMeshGenerator.generateEdgeFillMesh(chunk, chunkWorldPos, edgeFillDepth);
            if (edgeFillMesh != null) {
                Geometry edgeFillGeom = new Geometry("TerrainEdgeFill_" + chunk.getPosition().toString(), edgeFillMesh);

                // 使用与地表相同的材质
                Material edgeFillMat = terrainMaterialFactory.getMaterial(com.Hecate.world.TerrainMaterial.DIRT);
                edgeFillGeom.setMaterial(edgeFillMat);
                edgeFillGeom.setQueueBucket(com.jme3.renderer.queue.RenderQueue.Bucket.Opaque);

                // 【关键修复】启用阴影接收
                edgeFillGeom.setShadowMode(com.jme3.renderer.queue.RenderQueue.ShadowMode.CastAndReceive);

                chunkNode.attachChild(edgeFillGeom);
            }
        }

        // === 体素方块渲染（地下方块和非地形方块）===
        // 使用网格合并：为每种方块类型创建一个合并的Geometry
        java.util.Map<String, java.util.List<Vector3f>> blockPositions = new java.util.HashMap<>();

        // 收集所有非空气方块的位置，按类型分组
        for (int x = 0; x < Chunk.SIZE; x++) {
            for (int y = 0; y < Chunk.SIZE; y++) {
                for (int z = 0; z < Chunk.SIZE; z++) {
                    String blockId = chunk.getBlockId(x, y, z);

                    // 只处理非空气方块
                    if (!"air".equals(blockId)) {
                        // 如果是地形区块，跳过Y=0碰撞层（不渲染）
                        if (chunk.hasTerrainData() && y == 0) {
                            continue;
                        }

                        // 检查是否应该被摄像机剔除
                        int worldX = (int) (chunkWorldPos.x + x);
                        int worldY = (int) (chunkWorldPos.y + y);
                        int worldZ = (int) (chunkWorldPos.z + z);

                        if (cameraBlockCulling != null &&
                            cameraBlockCulling.shouldCullBlock(
                                new com.Hecate.camera.CameraBlockCulling.BlockPosition(worldX, worldY, worldZ))) {
                            continue; // 跳过需要剔除的方块
                        }

                        // 面剔除：只有暴露在外的方块才需要渲染
                        if (isBlockExposed(chunk, x, y, z)) {
                            blockPositions.computeIfAbsent(blockId, k -> new java.util.ArrayList<>())
                                    .add(new Vector3f(x, y, z));
                        }
                    }
                }
            }
        }

        // 为每种方块类型创建一个批量渲染的批次
        int totalBlocks = 0;
        for (java.util.Map.Entry<String, java.util.List<Vector3f>> entry : blockPositions.entrySet()) {
            String blockId = entry.getKey();
            java.util.List<Vector3f> positions = entry.getValue();

            if (!positions.isEmpty()) {
                com.jme3.scene.Spatial batchSpatial = createBatchGeometry(blockId, positions, chunkWorldPos);
                if (batchSpatial != null) {
                    chunkNode.attachChild(batchSpatial);
                    totalBlocks += positions.size();
                }
            }
        }

        // 将区块节点添加到世界
        worldNode.attachChild(chunkNode);
        chunk.setChunkNode(chunkNode);
        chunk.setClean();

        if (totalBlocks > 0) {
            LogUtils.debug(getClass(),
                String.format("区块 %s 已渲染 %d 个方块（合并为 %d 个批次）",
                    chunk.getPosition(), totalBlocks, blockPositions.size()));
        }
    }

    /**
     * 检查方块是否暴露在外（是否有相邻的空气方块）
     */
    private boolean isBlockExposed(Chunk chunk, int x, int y, int z) {
        // 检查六个面是否有任何一面暴露
        return isAirOrOutside(chunk, x + 1, y, z) ||
               isAirOrOutside(chunk, x - 1, y, z) ||
               isAirOrOutside(chunk, x, y + 1, z) ||
               isAirOrOutside(chunk, x, y - 1, z) ||
               isAirOrOutside(chunk, x, y, z + 1) ||
               isAirOrOutside(chunk, x, y, z - 1);
    }

    /**
     * 检查位置是否为空气或超出区块边界
     */
    private boolean isAirOrOutside(Chunk chunk, int x, int y, int z) {
        // 超出区块边界视为暴露（简化处理，更完善的版本需要检查相邻区块）
        if (x < 0 || x >= Chunk.SIZE || y < 0 || y >= Chunk.SIZE || z < 0 || z >= Chunk.SIZE) {
            return true;
        }
        return "air".equals(chunk.getBlockId(x, y, z));
    }

    /**
     * 创建批量渲染的方块Node
     */
    private com.jme3.scene.Spatial createBatchGeometry(String blockId, java.util.List<Vector3f> positions, Vector3f chunkWorldPos) {
        if (positions.isEmpty()) {
            return null;
        }

        // 使用 BatchNode 进行自动批处理
        BatchNode batchNode = new BatchNode("Batch_" + blockId);

        for (Vector3f localPos : positions) {
            Vector3f worldPos = new Vector3f(
                chunkWorldPos.x + localPos.x,
                chunkWorldPos.y + localPos.y,
                chunkWorldPos.z + localPos.z
            );

            Geometry blockGeom = createBlockGeometry(blockId, worldPos);
            if (blockGeom != null) {
                batchNode.attachChild(blockGeom);
            }
        }

        // 执行批处理：合并所有Geometry为一个，大幅减少绘制调用
        batchNode.batch();

        return batchNode;
    }

    /**
     * 创建方块几何体
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
        // 更新游戏时间和水流动画
        gameTime += tpf;
        if (terrainMaterialFactory != null) {
            terrainMaterialFactory.updateWaterAnimation(gameTime);
        }

        // 如果没有玩家控制器，不进行区块加载
        if (playerController == null) {
            return;
        }

        // 累积时间，减少更新频率
        updateTimer += tpf;
        if (updateTimer < UPDATE_INTERVAL) {
            return; // 未到更新时间
        }
        updateTimer = 0f;

        // 获取玩家当前所在区块位置
        Vector3f playerPos = playerController.getPlayerPosition();
        int playerChunkX = (int) Math.floor(playerPos.x / Chunk.SIZE);
        int playerChunkZ = (int) Math.floor(playerPos.z / Chunk.SIZE);

        Vector3f currentChunkPos = new Vector3f(playerChunkX, 0, playerChunkZ);

        // 检查是否移动到新区块
        boolean movedToNewChunk = lastPlayerChunkPos != null &&
                                 !currentChunkPos.equals(lastPlayerChunkPos);

        // 移动到新区块时重置加载标志
        if (movedToNewChunk) {
            initialLoadComplete = false;
        }

        // 初始加载或玩家移动到新区块时加载
        boolean needsLoad = lastPlayerChunkPos == null ||
                           movedToNewChunk ||
                           !initialLoadComplete;

        if (needsLoad) {
            // 加载玩家周围的区块（限制每次加载数量）
            boolean hasMoreToLoad = loadChunksAroundPlayer(playerChunkX, playerChunkZ);

            // 如果没有更多区块需要加载，标记初始加载完成
            if (!hasMoreToLoad) {
                if (!initialLoadComplete) {
                    initialLoadComplete = true;
                }
            }

            // 卸载远离玩家的区块
            if (lastPlayerChunkPos != null) {
                unloadDistantChunks(playerChunkX, playerChunkZ);
            }

            lastPlayerChunkPos = currentChunkPos;
        }

        // 更新摄像机方块剔除系统
        if (cameraBlockCulling != null && playerController != null) {
            cameraBlockCulling.update(playerController.getPlayerPosition());
        }

        // 检查并重新渲染 dirty 的区块
        for (Chunk chunk : chunkManager.getLoadedChunks().values()) {
            if (chunk.isDirty()) {
                // 移除旧的渲染节点
                if (chunk.getChunkNode() != null) {
                    worldNode.detachChild(chunk.getChunkNode());
                }
                // 重新渲染
                renderChunk(chunk);
            }
        }
    }

    @Override
    public void onDisable() {
        // 清理摄像机方块剔除系统
        if (cameraBlockCulling != null) {
            cameraBlockCulling.cleanup();
        }

        // 保存所有修改过的区块
        if (chunkManager != null) {
            chunkManager.saveAllModifiedChunks();
        }

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

    /**
     * 切换渲染/更新循环所驱动的活动世界（用于世界切换，如竞技场）
     * <p>复用现有的 onUpdate 区块加载/渲染管线，只是重新指向新的 ChunkManager 和场景节点。
     * 不会改变 textureManager/terrainMaterialFactory/blockRegistry，这些与具体世界无关。
     *
     * @param newChunkManager 新的活动区块管理器
     * @param newWorldNode 新的活动世界场景节点
     */
    public void bindActiveWorld(ChunkManager newChunkManager, Node newWorldNode) {
        bindActiveWorld(newChunkManager, newWorldNode, DEFAULT_EDGE_FILL_DEPTH);
    }

    /**
     * 切换渲染/更新循环所驱动的活动世界，并指定该世界地形边缘填充网格下探的深度
     * @param edgeFillDepth 边缘填充网格下探到的绝对世界Y坐标
     */
    public void bindActiveWorld(ChunkManager newChunkManager, Node newWorldNode, float edgeFillDepth) {
        // 切换前先清理摄像机方块剔除系统（绑定了旧的chunkManager）
        if (cameraBlockCulling != null) {
            cameraBlockCulling.cleanup();
        }

        this.chunkManager = newChunkManager;
        this.worldNode = newWorldNode;
        this.edgeFillDepth = edgeFillDepth;

        // 重建摄像机方块剔除系统，绑定新的chunkManager
        cameraBlockCulling = new com.Hecate.camera.CameraBlockCulling(app.getCamera(), chunkManager);

        // 重置区块加载状态，强制在新世界中围绕玩家重新加载
        lastPlayerChunkPos = null;
        initialLoadComplete = false;
        updateTimer = 0f;
    }

    public BlockTextureManager getTextureManager() {
        return textureManager;
    }

    /**
     * 设置玩家控制器
     */
    public void setPlayerController(com.Hecate.player.PlayerController playerController) {
        this.playerController = playerController;
    }

    /**
     * 加载玩家周围的区块（限制每次加载数量）
     * @return 是否还有更多区块需要加载
     */
    private boolean loadChunksAroundPlayer(int playerChunkX, int playerChunkZ) {
        int chunksLoaded = 0;
        boolean hasMoreToLoad = false;

        // 优先加载距离玩家最近的区块
        // 使用螺旋式加载顺序
        outerLoop:
        for (int radius = 0; radius <= RENDER_DISTANCE; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    // 只处理当前半径的边缘
                    if (Math.abs(dx) != radius && Math.abs(dz) != radius) {
                        continue;
                    }

                    int chunkX = playerChunkX + dx;
                    int chunkZ = playerChunkZ + dz;

                    ChunkPosition position = new ChunkPosition(chunkX, 0, chunkZ);

                    // 如果区块未加载，则加载并渲染
                    if (chunkManager.getChunk(position) == null) {
                        // 达到本次加载上限，标记还有更多需要加载
                        if (chunksLoaded >= MAX_CHUNKS_PER_FRAME) {
                            hasMoreToLoad = true;
                            break outerLoop;
                        }

                        Chunk chunk = chunkManager.loadChunk(position);
                        if (chunk != null) {
                            renderChunk(chunk);
                            chunksLoaded++;
                        }
                    }
                }
            }
        }

        if (chunksLoaded > 0) {
            LogUtils.debug(getClass(),
                String.format("加载了 %d 个区块 (玩家区块: %d, %d)%s",
                    chunksLoaded, playerChunkX, playerChunkZ,
                    hasMoreToLoad ? " [还有更多...]" : ""));
        }

        return hasMoreToLoad;
    }

    /**
     * 卸载远离玩家的区块
     */
    private void unloadDistantChunks(int playerChunkX, int playerChunkZ) {
        java.util.List<ChunkPosition> chunksToUnload = new java.util.ArrayList<>();

        // 检查所有已加载的区块
        for (ChunkPosition pos : chunkManager.getLoadedChunks().keySet()) {
            int dx = Math.abs(pos.getX() - playerChunkX);
            int dz = Math.abs(pos.getZ() - playerChunkZ);

            // 如果区块距离超过卸载距离，标记为待卸载
            if (dx > UNLOAD_DISTANCE || dz > UNLOAD_DISTANCE) {
                chunksToUnload.add(pos);
            }
        }

        // 卸载标记的区块
        for (ChunkPosition pos : chunksToUnload) {
            chunkManager.unloadChunk(pos);
        }
    }

    /**
     * 基于墨水范围按需加载chunk
     * 当墨水系统尝试访问某个世界坐标时，确保对应chunk已加载
     * @param worldX 世界X坐标
     * @param worldZ 世界Z坐标
     */
    public void ensureChunkLoadedForInk(float worldX, float worldZ) {
        // 计算chunk坐标
        int chunkX = (int) Math.floor(worldX / Chunk.SIZE);
        int chunkZ = (int) Math.floor(worldZ / Chunk.SIZE);

        // 预加载周围的chunk（墨水可能会扩散）
        for (int dx = -INK_EXPAND_RADIUS; dx <= INK_EXPAND_RADIUS; dx++) {
            for (int dz = -INK_EXPAND_RADIUS; dz <= INK_EXPAND_RADIUS; dz++) {
                ChunkPosition pos = new ChunkPosition(chunkX + dx, 0, chunkZ + dz);

                // 如果chunk未加载，异步加载
                if (chunkManager.getChunk(pos) == null) {
                    Chunk chunk = chunkManager.loadChunk(pos);
                    if (chunk != null) {
                        renderChunk(chunk);
                    }
                }
            }
        }
    }
}
