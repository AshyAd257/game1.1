package com.Hecate.world;

import com.Hecate.world.Chunk;
import com.Hecate.world.ChunkManager;
import com.Hecate.world.ChunkPosition;

/**
 * 地形生成器 - 生成完整的地表
 */
public class TerrainGenerator {
    private final ChunkManager chunkManager;

    public TerrainGenerator(ChunkManager chunkManager) {
        this.chunkManager = chunkManager;
    }

    /**
     * 为指定区块生成地形
     */
    public void generateTerrain(ChunkPosition chunkPos) {
        Chunk chunk = chunkManager.getOrLoadChunk(chunkPos);

        // 只在Y=0的区块生成地表
        if (chunkPos.getY() == 0) {
            generateSurfaceTerrain(chunk);
        }
        // 其他Y层保持空气
    }

    /**
     * 生成地表地形 - 只在Y=1生成一层土方块
     */
    private void generateSurfaceTerrain(Chunk chunk) {
        System.out.println("生成地表区块: " + chunk.getPosition());

        for (int x = 0; x < Chunk.SIZE; x++) {
            for (int z = 0; z < Chunk.SIZE; z++) {
                chunk.setBlock(x, 1, z, "dirt"); // 只在Y=1放置土方块
            }
        }
    }

    /**
     * 生成大面积地形 - 只生成地表层
     */
    public void generateLargeTerrain(int centerX, int centerZ, int radius) {
        System.out.println("开始生成大面积地形，中心: (" + centerX + ", " + centerZ + ")，半径: " + radius);

        int generatedChunks = 0;

        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                // 只生成Y=0的区块（包含Y=1的地表）
                ChunkPosition pos = new ChunkPosition(x, 0, z);
                generateTerrain(pos);
                generatedChunks++;
            }
        }

        System.out.println("地形生成完成，共生成 " + generatedChunks + " 个区块");
    }
}
