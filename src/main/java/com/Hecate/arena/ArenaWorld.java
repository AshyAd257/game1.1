package com.Hecate.arena;

import com.Hecate.world.Chunk;
import com.Hecate.world.ChunkManager;
import com.Hecate.world.ChunkPosition;
import com.Hecate.world.HeightMap;
import com.Hecate.world.TerrainMaterial;
import com.Hecate.utils.LogUtils;

/**
 * 竞技场世界生成器
 * 生成一个完全平坦的圆形竞技场
 *
 * <p>地面使用地形高度场系统（与主世界地表相同的渲染/碰撞路径），而不是逐格体素方块。
 * 这样 {@link com.Hecate.physics.CollisionManager#getTerrainHeightAt} 能直接读到正确的
 * 平坦高度，且不会与体素方块渲染叠加。
 */
public class ArenaWorld {

    private final ChunkManager chunkManager;

    // 地面高度（高度场数值，与 HeightMap 的默认基准一致）
    public static final float FLOOR_HEIGHT = 5.0f;

    /**
     * 构造竞技场世界生成器
     * @param chunkManager 区块管理器
     */
    public ArenaWorld(ChunkManager chunkManager) {
        this.chunkManager = chunkManager;
    }

    /**
     * 生成圆形竞技场（平坦地形，使用高度场系统）
     * @param centerX 中心X坐标（世界坐标）
     * @param centerZ 中心Z坐标（世界坐标）
     * @param diameter 直径（方块数量）
     */
    public void generateArena(float centerX, float centerZ, int diameter) {


        float radius = diameter / 2.0f;

        // 计算需要生成的区块范围
        int chunkRadius = (int) Math.ceil(radius / Chunk.SIZE) + 1;
        int centerChunkX = (int) Math.floor(centerX / Chunk.SIZE);
        int centerChunkZ = (int) Math.floor(centerZ / Chunk.SIZE);

        int floorCells = 0;

        // 遍历所有可能包含圆形的区块
        for (int chunkX = centerChunkX - chunkRadius; chunkX <= centerChunkX + chunkRadius; chunkX++) {
            for (int chunkZ = centerChunkZ - chunkRadius; chunkZ <= centerChunkZ + chunkRadius; chunkZ++) {
                // 只在Y=0的区块生成地面
                ChunkPosition chunkPos = new ChunkPosition(chunkX, 0, chunkZ);
                // 注意：如果该区块此前未加载过，getOrLoadChunk 会通过
                // ChunkManager.loadChunk -> Chunk.fillWithTestPattern 生成噪声地形。
                // 下面的逻辑会完全重写高度场和材质，清除这层噪声污染。
                Chunk chunk = chunkManager.getOrLoadChunk(chunkPos);

                // 清空区块体素方块（填充为空气）
                for (int x = 0; x < Chunk.SIZE; x++) {
                    for (int y = 0; y < Chunk.SIZE; y++) {
                        for (int z = 0; z < Chunk.SIZE; z++) {
                            chunk.setBlock(x, y, z, "air");
                        }
                    }
                }

                // 重置高度场为完全平坦（覆盖 fillWithTestPattern 留下的噪声起伏）
                HeightMap heightMap = chunk.getSurfaceHeightMap();
                heightMap.initializeFlat(FLOOR_HEIGHT);

                // 按圆形范围逐格设置地表材质：圆内=DIRT（生成平坦地形），圆外=NONE（无地形）
                TerrainMaterial[][] materials = chunk.getSurfaceMaterials();
                for (int localX = 0; localX < Chunk.SIZE; localX++) {
                    for (int localZ = 0; localZ < Chunk.SIZE; localZ++) {
                        // 使用格子中心点判断是否在圆形范围内
                        float worldX = chunkX * Chunk.SIZE + localX + 0.5f;
                        float worldZ = chunkZ * Chunk.SIZE + localZ + 0.5f;

                        float dx = worldX - centerX;
                        float dz = worldZ - centerZ;
                        float distance = (float) Math.sqrt(dx * dx + dz * dz);

                        if (distance <= radius) {
                            materials[localX][localZ] = TerrainMaterial.DIRT;
                            floorCells++;
                        } else {
                            materials[localX][localZ] = TerrainMaterial.NONE;
                        }
                    }
                }

                // 标记区块需要重新渲染
                chunk.setDirty();
            }
        }


    }

    /**
     * 清空指定区域的所有方块和地形数据
     * @param centerX 中心X坐标
     * @param centerZ 中心Z坐标
     * @param diameter 直径
     */
    public void clearArena(float centerX, float centerZ, int diameter) {
        float radius = diameter / 2.0f;

        int chunkRadius = (int) Math.ceil(radius / Chunk.SIZE) + 1;
        int centerChunkX = (int) Math.floor(centerX / Chunk.SIZE);
        int centerChunkZ = (int) Math.floor(centerZ / Chunk.SIZE);

        for (int chunkX = centerChunkX - chunkRadius; chunkX <= centerChunkX + chunkRadius; chunkX++) {
            for (int chunkZ = centerChunkZ - chunkRadius; chunkZ <= centerChunkZ + chunkRadius; chunkZ++) {
                ChunkPosition chunkPos = new ChunkPosition(chunkX, 0, chunkZ);
                Chunk chunk = chunkManager.getChunk(chunkPos);

                if (chunk != null) {
                    // 清空区块（填充为空气）
                    for (int x = 0; x < Chunk.SIZE; x++) {
                        for (int y = 0; y < Chunk.SIZE; y++) {
                            for (int z = 0; z < Chunk.SIZE; z++) {
                                chunk.setBlock(x, y, z, "air");
                            }
                        }
                    }

                    // 清空地形数据（移除高度场地表，避免残留旧地形）
                    TerrainMaterial[][] materials = chunk.getSurfaceMaterials();
                    for (int x = 0; x < Chunk.SIZE; x++) {
                        for (int z = 0; z < Chunk.SIZE; z++) {
                            materials[x][z] = TerrainMaterial.NONE;
                        }
                    }

                    chunk.setDirty();
                }
            }
        }


    }

    /**
     * 获取竞技场的生成点（圆心上方）
     * @param centerX 圆心X
     * @param centerZ 圆心Z
     * @return 生成点的Y坐标（地面上方）
     */
    public float getSpawnHeight(float centerX, float centerZ) {
        return FLOOR_HEIGHT + 0.5f; // 地面上方0.5格
    }
}
