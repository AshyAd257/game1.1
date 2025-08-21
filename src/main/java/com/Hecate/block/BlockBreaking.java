package com.Hecate.block;

import com.jme3.math.Vector3f;
import com.Hecate.world.Chunk;
import com.Hecate.world.ChunkManager;
import com.Hecate.world.ChunkPosition;

/**
 * 方块破坏系统 - 处理方块破坏的进度、效果和掉落物
 */
public class BlockBreaking {
    private final ChunkManager chunkManager;
    private final BlockRegistry blockRegistry;

    // 破坏进度跟踪
    private Vector3f currentBreakingBlock = null;
    private float breakingProgress = 0.0f;
    private float breakingTime = 0.0f;

    public BlockBreaking(ChunkManager chunkManager) {
        this.chunkManager = chunkManager;
        this.blockRegistry = BlockRegistry.getInstance();
    }

    /**
     * 开始破坏方块
     */
    public void startBreaking(Vector3f blockPosition) {
        if (!blockPosition.equals(currentBreakingBlock)) {
            // 重置破坏进度
            currentBreakingBlock = blockPosition.clone();
            breakingProgress = 0.0f;
            breakingTime = 0.0f;

            System.out.println("开始破坏方块: " + blockPosition);
        }
    }

    /**
     * 停止破坏方块
     */
    public void stopBreaking() {
        if (currentBreakingBlock != null) {
            System.out.println("停止破坏方块: " + currentBreakingBlock);
            currentBreakingBlock = null;
            breakingProgress = 0.0f;
            breakingTime = 0.0f;
        }
    }

    /**
     * 更新破坏进度
     */
    public void updateBreaking(float tpf) {
        if (currentBreakingBlock == null) {
            return;
        }

        // 获取方块信息
        String blockId = getBlockAt(currentBreakingBlock);
        if ("air".equals(blockId)) {
            stopBreaking();
            return;
        }

        Block block = blockRegistry.getBlock(blockId);
        if (block == null) {
            stopBreaking();
            return;
        }

        // 计算破坏时间（基于方块硬度）
        float hardness = getBlockHardness(block);
        float requiredTime = hardness * 1.5f; // 基础破坏时间

        // 更新进度
        breakingTime += tpf;
        breakingProgress = Math.min(breakingTime / requiredTime, 1.0f);

        // 检查是否完成破坏
        if (breakingProgress >= 1.0f) {
            completeBreaking();
        }
    }

    /**
     * 完成方块破坏
     */
    private void completeBreaking() {
        if (currentBreakingBlock == null) {
            return;
        }

        String blockId = getBlockAt(currentBreakingBlock);
        Block block = blockRegistry.getBlock(blockId);

        if (block != null) {
            // 设置为空气方块
            setBlockAt(currentBreakingBlock, "air");

            // 创建掉落物
            createBlockDrop(currentBreakingBlock, block);

            System.out.println("完成破坏方块: " + blockId + " 在位置 " + currentBreakingBlock);
        }

        stopBreaking();
    }

    /**
     * 获取指定位置的方块
     */
    private String getBlockAt(Vector3f worldPos) {
        ChunkPosition chunkPos = worldToChunkPosition(worldPos);
        Chunk chunk = chunkManager.getChunk(chunkPos);

        if (chunk != null) {
            int localX = (int) (worldPos.x - chunkPos.getX() * Chunk.SIZE);
            int localY = (int) (worldPos.y - chunkPos.getY() * Chunk.SIZE);
            int localZ = (int) (worldPos.z - chunkPos.getZ() * Chunk.SIZE);

            if (localX >= 0 && localX < Chunk.SIZE &&
                    localY >= 0 && localY < Chunk.SIZE &&
                    localZ >= 0 && localZ < Chunk.SIZE) {
                return chunk.getBlockId(localX, localY, localZ);
            }
        }

        return "air";
    }

    /**
     * 设置指定位置的方块
     */
    private void setBlockAt(Vector3f worldPos, String blockId) {
        ChunkPosition chunkPos = worldToChunkPosition(worldPos);
        Chunk chunk = chunkManager.getChunk(chunkPos);

        if (chunk != null) {
            int localX = (int) (worldPos.x - chunkPos.getX() * Chunk.SIZE);
            int localY = (int) (worldPos.y - chunkPos.getY() * Chunk.SIZE);
            int localZ = (int) (worldPos.z - chunkPos.getZ() * Chunk.SIZE);

            if (localX >= 0 && localX < Chunk.SIZE &&
                    localY >= 0 && localY < Chunk.SIZE &&
                    localZ >= 0 && localZ < Chunk.SIZE) {
                chunk.setBlock(localX, localY, localZ, blockId);
            }
        }
    }

    /**
     * 获取方块硬度
     */
    private float getBlockHardness(Block block) {
        // 根据方块类型返回不同的硬度
        switch (block.getId()) {
            case "stone": return 3.0f;
            case "dirt": return 1.0f;
            case "grass": return 1.2f;
            case "glass": return 0.5f;
            default: return 2.0f;
        }
    }

    /**
     * 创建方块掉落物
     */
    private void createBlockDrop(Vector3f position, Block block) {
        // 这里将来会实现掉落物系统
        // 目前只是打印信息
        System.out.println("创建掉落物: " + block.getId() + " 在位置 " + position);
    }

    /**
     * 世界坐标转区块坐标
     */
    private ChunkPosition worldToChunkPosition(Vector3f worldPos) {
        int chunkX = (int) Math.floor(worldPos.x / Chunk.SIZE);
        int chunkY = (int) Math.floor(worldPos.y / Chunk.SIZE);
        int chunkZ = (int) Math.floor(worldPos.z / Chunk.SIZE);
        return new ChunkPosition(chunkX, chunkY, chunkZ);
    }

    // Getter方法
    public Vector3f getCurrentBreakingBlock() { return currentBreakingBlock; }
    public float getBreakingProgress() { return breakingProgress; }
    public boolean isBreaking() { return currentBreakingBlock != null; }
}
