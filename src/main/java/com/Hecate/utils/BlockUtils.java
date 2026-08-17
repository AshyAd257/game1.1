package com.Hecate.utils;

import com.Hecate.world.Chunk;
import com.Hecate.world.ChunkManager;
import com.Hecate.world.ChunkPosition;
import com.jme3.math.Vector3f;

import java.util.Random;

/**
 * 方块操作工具类 - 提供通用的方块操作方法
 */
public class BlockUtils {
    private static final String[] DIRT_VARIANTS = {"dirt", "dirt2", "dirt3", "dirt4"};
    private static final Random RANDOM = new Random();

    /**
     * 世界坐标转区块坐标
     */
    public static ChunkPosition worldToChunkPosition(Vector3f worldPos) {
        int chunkX = (int) Math.floor(worldPos.x / Chunk.SIZE);
        int chunkY = (int) Math.floor(worldPos.y / Chunk.SIZE);
        int chunkZ = (int) Math.floor(worldPos.z / Chunk.SIZE);
        return new ChunkPosition(chunkX, chunkY, chunkZ);
    }

    /**
     * 获取指定位置的方块
     */
    public static String getBlockAt(Vector3f worldPos, ChunkManager chunkManager) {
        ChunkPosition chunkPos = worldToChunkPosition(worldPos);
        Chunk chunk = chunkManager.getChunk(chunkPos);

        if (chunk != null) {
            int localX = (int) (worldPos.x - chunkPos.getX() * Chunk.SIZE);
            int localY = (int) (worldPos.y - chunkPos.getY() * Chunk.SIZE);
            int localZ = (int) (worldPos.z - chunkPos.getZ() * Chunk.SIZE);

            if (isValidLocalCoordinate(localX, localY, localZ)) {
                return chunk.getBlockId(localX, localY, localZ);
            }
        }

        return "air";
    }

    /**
     * 设置指定位置的方块
     */
    public static boolean setBlockAt(Vector3f worldPos, String blockId, ChunkManager chunkManager) {
        ChunkPosition chunkPos = worldToChunkPosition(worldPos);
        Chunk chunk = chunkManager.getChunk(chunkPos);

        if (chunk != null) {
            int localX = (int) (worldPos.x - chunkPos.getX() * Chunk.SIZE);
            int localY = (int) (worldPos.y - chunkPos.getY() * Chunk.SIZE);
            int localZ = (int) (worldPos.z - chunkPos.getZ() * Chunk.SIZE);

            if (isValidLocalCoordinate(localX, localY, localZ)) {
                chunk.setBlock(localX, localY, localZ, blockId);
                return true;
            }
        }

        return false;
    }

    /**
     * 检查局部坐标是否有效
     */
    private static boolean isValidLocalCoordinate(int x, int y, int z) {
        return x >= 0 && x < Chunk.SIZE &&
                y >= 0 && y < Chunk.SIZE &&
                z >= 0 && z < Chunk.SIZE;
    }

    /**
     * 获取随机泥土方块ID（用于多样化泥土材质�?
     */
    public static String getRandomDirtVariant() {
        return DIRT_VARIANTS[RANDOM.nextInt(DIRT_VARIANTS.length)];
    }
}
