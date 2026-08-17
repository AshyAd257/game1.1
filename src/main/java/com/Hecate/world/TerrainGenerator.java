package com.Hecate.world;

import com.Hecate.utils.BlockUtils;
import com.Hecate.utils.LogUtils;

/**
 * 地形生成器 - 生成完整的地表
 */
public class TerrainGenerator {
    private final ChunkManager chunkManager;

    public TerrainGenerator(ChunkManager chunkManager) {
        this.chunkManager = chunkManager;
    }

    /**
     * 为指定区块生成地�?
     */
    public void generateTerrain(ChunkPosition chunkPos) {
        Chunk chunk = chunkManager.getOrLoadChunk(chunkPos);

        // 只在Y=0的区块生成地�?
        if (chunkPos.getY() == 0) {
            generateSurfaceTerrain(chunk);
        }
        // 其他Y层保持空�?
    }

    /**
     * 生成地表地形 - 生成5层厚的地面 (Y=1到Y=5)
     */
    private void generateSurfaceTerrain(Chunk chunk) {
        final int TERRAIN_THICKNESS = 5;

        for (int x = 0; x < Chunk.SIZE; x++) {
            for (int z = 0; z < Chunk.SIZE; z++) {
                // 生成5层厚的地面
                for (int y = 1; y <= TERRAIN_THICKNESS; y++) {
                    chunk.setBlock(x, y, z, BlockUtils.getRandomDirtVariant());
                }
            }
        }
    }

    /**
     * 生成大面积地�?- 只生成地表层
     */
    public void generateLargeTerrain(int centerX, int centerZ, int radius) {
        int generatedChunks = 0;

        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                // 只生成Y=0的区块（包含Y=1的地表）
                ChunkPosition pos = new ChunkPosition(x, 0, z);
                generateTerrain(pos);
                generatedChunks++;
            }
        }
    }
}
