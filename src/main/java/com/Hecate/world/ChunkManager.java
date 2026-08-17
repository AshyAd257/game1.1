package com.Hecate.world;

import com.jme3.scene.Node;
// import com.Hecate.utils.LogUtils;
import java.util.HashMap;
import java.util.Map;

/**
 * 管理世界中的所有区块
 */
public class ChunkManager {
    private final Map<ChunkPosition, Chunk> loadedChunks = new HashMap<>();
    private final Node worldNode; // 包含所有区块的根节点
    private final ChunkSerializer serializer; // 区块序列化器

    public ChunkManager(Node worldNode) {
        this(worldNode, "world"); // 默认世界名为"world"
    }

    public ChunkManager(Node worldNode, String worldName) {
        this.worldNode = worldNode;
        this.serializer = new ChunkSerializer(worldName);
    }

    /**
     * 加载指定位置的区块
     * @param position 区块位置
     * @return 加载的区块
     */
    public Chunk loadChunk(ChunkPosition position) {
        // 如果区块已加载，直接返回
        if (loadedChunks.containsKey(position)) {
            return loadedChunks.get(position);
        }

        Chunk chunk;

        // 1. 尝试从文件加载
        chunk = serializer.loadChunk(position);

        // 2. 如果文件不存在，生成新区块
        if (chunk == null) {
            chunk = new Chunk(position);
            chunk.fillWithTestPattern(); // 生成默认地形
            // LogUtils.debug(ChunkManager.class, "生成新区块: " + position);
        } else {
            // LogUtils.debug(ChunkManager.class, "从文件加载区块: " + position);
        }

        loadedChunks.put(position, chunk);
        return chunk;
    }

    /**
     * 卸载指定区块
     * @param position 区块位置
     */
    public void unloadChunk(ChunkPosition position) {
        Chunk chunk = loadedChunks.remove(position);
        if (chunk != null) {
            // 如果区块被修改过，保存到文件
            if (chunk.isModified()) {
                serializer.saveChunk(chunk);
                // LogUtils.debug(ChunkManager.class, "卸载并保存区块: " + position);
            } else {
                // LogUtils.debug(ChunkManager.class, "卸载区块（未修改）: " + position);
            }

            // 从场景图移除
            if (chunk.getChunkNode() != null) {
                worldNode.detachChild(chunk.getChunkNode());
            }
        }
    }

    /**
     * 获取所有已加载的区块
     */
    public Map<ChunkPosition, Chunk> getLoadedChunks() {
        return loadedChunks;
    }

    /**
     * 获取指定位置的区块（如果未加载则返回null）
     */
    public Chunk getChunk(ChunkPosition position) {
        return loadedChunks.get(position);
    }

    /**
     * 获取或加载指定位置的区块
     */
    public Chunk getOrLoadChunk(ChunkPosition position) {
        Chunk chunk = getChunk(position);
        if (chunk == null) {
            chunk = loadChunk(position);
        }
        return chunk;
    }

    /**
     * 保存所有修改过的区块（用于游戏退出时）
     */
    public void saveAllModifiedChunks() {
        int savedCount = 0;
        for (Chunk chunk : loadedChunks.values()) {
            if (chunk.isModified()) {
                serializer.saveChunk(chunk);
                savedCount++;
            }
        }
        if (savedCount > 0) {
            // LogUtils.info(ChunkManager.class, "已保存 " + savedCount + " 个修改过的区块");
        }
    }

    /**
     * 获取序列化器（用于其他需要访问的地方）
     */
    public ChunkSerializer getSerializer() {
        return serializer;
    }
}