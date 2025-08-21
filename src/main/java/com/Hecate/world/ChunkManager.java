package com.Hecate.world;

import com.jme3.scene.Node;
import java.util.HashMap;
import java.util.Map;

/**
 * 管理世界中的所有区块
 */
public class ChunkManager {
    private final Map<ChunkPosition, Chunk> loadedChunks = new HashMap<>();
    private final Node worldNode; // 包含所有区块的根节点

    public ChunkManager(Node worldNode) {
        this.worldNode = worldNode;
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

        // 创建新区块
        Chunk chunk = new Chunk(position);
        loadedChunks.put(position, chunk);

        // 为测试目的，填充区块
        chunk.fillWithTestPattern();

        return chunk;
    }

    /**
     * 卸载指定区块
     * @param position 区块位置
     */
    public void unloadChunk(ChunkPosition position) {
        Chunk chunk = loadedChunks.remove(position);
        if (chunk != null && chunk.getChunkNode() != null) {
            worldNode.detachChild(chunk.getChunkNode());
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
}