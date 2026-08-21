package com.Hecate.player;

import com.Hecate.physics.CollisionManager;
import com.Hecate.utils.LogUtils;
import com.Hecate.world.Chunk;
import com.Hecate.world.ChunkManager;
import com.Hecate.world.ChunkPosition;
import com.Hecate.world.HeightMap;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;

import java.util.HashSet;
import java.util.Set;

/**
 * 玩家体素交互处理器
 * 负责地形挖掘、射线检测、跨Chunk边界同步等逻辑，从 PlayerController 中抽离
 */
public class PlayerVoxelInteraction {

    private final Camera camera;
    private final CollisionManager collisionManager;

    /**
     * 构造函数
     */
    public PlayerVoxelInteraction(Camera camera, CollisionManager collisionManager) {
        this.camera = camera;
        this.collisionManager = collisionManager;
    }

    /**
     * 执行地形挖掘 - 鼠标左键点击挖掘
     */
    public void performTerrainDig() {
        if (collisionManager == null) {
            return;
        }

        // 发射射线检测地形
        Ray ray = new Ray(camera.getLocation(), camera.getDirection());

        // 使用改进的射线检测算法
        // 最大检测距离20个单位
        float maxDistance = 20.0f;
        Vector3f hitPoint = null;
        float hitDistance = Float.MAX_VALUE;

        // 更精细的步进检测（0.1步长）
        for (float distance = 0.1f; distance < maxDistance; distance += 0.1f) {
            Vector3f testPoint = ray.getOrigin().add(ray.getDirection().mult(distance));
            float terrainHeight = collisionManager.getTerrainHeightAt(testPoint.x, testPoint.z);

            // 检查是否击中地形（允许一定误差范围）
            if (!Float.isNaN(terrainHeight)) {
                // 如果测试点在地形下方或非常接近地形表面
                if (testPoint.y <= terrainHeight + 0.2f && testPoint.y >= terrainHeight - 0.5f) {
                    if (distance < hitDistance) {
                        hitPoint = new Vector3f(testPoint.x, terrainHeight, testPoint.z);
                        hitDistance = distance;
                        break;
                    }
                }
            }
        }

        if (hitPoint == null) {
            LogUtils.debug(PlayerVoxelInteraction.class, "未检测到地形命中点");
            return;
        }

        // 计算chunk坐标
        int chunkX = (int) Math.floor(hitPoint.x / Chunk.SIZE);
        int chunkZ = (int) Math.floor(hitPoint.z / Chunk.SIZE);
        ChunkPosition chunkPos = new ChunkPosition(chunkX, 0, chunkZ);

        // 获取chunk
        ChunkManager chunkManager = collisionManager.getChunkManager();
        if (chunkManager == null) {
            LogUtils.debug(PlayerVoxelInteraction.class, "ChunkManager为null");
            return;
        }

        Chunk chunk = chunkManager.getChunk(chunkPos);
        if (chunk == null) {
            LogUtils.debug(PlayerVoxelInteraction.class, "Chunk不存在: " + chunkPos);
            return;
        }

        if (!chunk.hasTerrainData()) {
            LogUtils.debug(PlayerVoxelInteraction.class, "Chunk没有地形数据: " + chunkPos);
            return;
        }

        // 计算chunk内坐标
        float localX = hitPoint.x - (chunkX * Chunk.SIZE);
        float localZ = hitPoint.z - (chunkZ * Chunk.SIZE);

        // 边界检查
        if (localX < 0) localX = 0;
        if (localZ < 0) localZ = 0;
        if (localX >= Chunk.SIZE) localX = Chunk.SIZE - 0.01f;
        if (localZ >= Chunk.SIZE) localZ = Chunk.SIZE - 0.01f;

        // 找到最接近点击位置的顶点（而不是整个tile）
        // 顶点坐标范围是0-16
        int vertexX = Math.round(localX);
        int vertexZ = Math.round(localZ);

        // 确保顶点坐标在有效范围内
        vertexX = Math.max(0, Math.min(16, vertexX));
        vertexZ = Math.max(0, Math.min(16, vertexZ));

        // 获取高度图
        HeightMap heightMap = chunk.getSurfaceHeightMap();

        // 使用半径挖掘，避免单点极度拉伸
        int digRadius = 1; // 挖掘半径（顶点数）
        float centerDig = -0.25f; // 中心降低量

        // 记录需要标记为脏的区块
        Set<ChunkPosition> dirtyChunks = new HashSet<>();
        dirtyChunks.add(chunkPos);

        // 对半径范围内的顶点应用渐变降低
        for (int dx = -digRadius; dx <= digRadius; dx++) {
            for (int dz = -digRadius; dz <= digRadius; dz++) {
                int nx = vertexX + dx;
                int nz = vertexZ + dz;

                // 计算距离中心的距离
                float distance = (float) Math.sqrt(dx * dx + dz * dz);

                // 基于距离的衰减系数（中心为1.0，边缘为0.0）
                float falloff = Math.max(0, 1.0f - (distance / (digRadius + 1)));

                // 应用带衰减的降低
                float digAmount = centerDig * falloff;

                // 检查是否在当前区块范围内
                if (nx >= 0 && nx <= 16 && nz >= 0 && nz <= 16) {
                    heightMap.modifyHeight(nx, nz, digAmount);

                    // 检查是否在区块边界，需要同步相邻区块
                    if (nx == 0 && chunkX > 0) {
                        // 左边界，更新左侧区块
                        ChunkPosition leftChunk = new ChunkPosition(chunkX - 1, 0, chunkZ);
                        Chunk leftChunkObj = chunkManager.getChunk(leftChunk);
                        if (leftChunkObj != null && leftChunkObj.hasTerrainData()) {
                            leftChunkObj.getSurfaceHeightMap().modifyHeight(16, nz, digAmount);
                            dirtyChunks.add(leftChunk);
                        }
                    } else if (nx == 16) {
                        // 右边界，更新右侧区块
                        ChunkPosition rightChunk = new ChunkPosition(chunkX + 1, 0, chunkZ);
                        Chunk rightChunkObj = chunkManager.getChunk(rightChunk);
                        if (rightChunkObj != null && rightChunkObj.hasTerrainData()) {
                            rightChunkObj.getSurfaceHeightMap().modifyHeight(0, nz, digAmount);
                            dirtyChunks.add(rightChunk);
                        }
                    }

                    if (nz == 0 && chunkZ > 0) {
                        // 前边界，更新前侧区块
                        ChunkPosition frontChunk = new ChunkPosition(chunkX, 0, chunkZ - 1);
                        Chunk frontChunkObj = chunkManager.getChunk(frontChunk);
                        if (frontChunkObj != null && frontChunkObj.hasTerrainData()) {
                            frontChunkObj.getSurfaceHeightMap().modifyHeight(nx, 16, digAmount);
                            dirtyChunks.add(frontChunk);
                        }
                    } else if (nz == 16) {
                        // 后边界，更新后侧区块
                        ChunkPosition backChunk = new ChunkPosition(chunkX, 0, chunkZ + 1);
                        Chunk backChunkObj = chunkManager.getChunk(backChunk);
                        if (backChunkObj != null && backChunkObj.hasTerrainData()) {
                            backChunkObj.getSurfaceHeightMap().modifyHeight(nx, 0, digAmount);
                            dirtyChunks.add(backChunk);
                        }
                    }

                    // 处理角点（同时在两个边界上）
                    if (nx == 0 && nz == 0 && chunkX > 0 && chunkZ > 0) {
                        // 左前角
                        ChunkPosition cornerChunk = new ChunkPosition(chunkX - 1, 0, chunkZ - 1);
                        Chunk cornerChunkObj = chunkManager.getChunk(cornerChunk);
                        if (cornerChunkObj != null && cornerChunkObj.hasTerrainData()) {
                            cornerChunkObj.getSurfaceHeightMap().modifyHeight(16, 16, digAmount);
                            dirtyChunks.add(cornerChunk);
                        }
                    } else if (nx == 16 && nz == 0 && chunkZ > 0) {
                        // 右前角
                        ChunkPosition cornerChunk = new ChunkPosition(chunkX + 1, 0, chunkZ - 1);
                        Chunk cornerChunkObj = chunkManager.getChunk(cornerChunk);
                        if (cornerChunkObj != null && cornerChunkObj.hasTerrainData()) {
                            cornerChunkObj.getSurfaceHeightMap().modifyHeight(0, 16, digAmount);
                            dirtyChunks.add(cornerChunk);
                        }
                    } else if (nx == 0 && nz == 16 && chunkX > 0) {
                        // 左后角
                        ChunkPosition cornerChunk = new ChunkPosition(chunkX - 1, 0, chunkZ + 1);
                        Chunk cornerChunkObj = chunkManager.getChunk(cornerChunk);
                        if (cornerChunkObj != null && cornerChunkObj.hasTerrainData()) {
                            cornerChunkObj.getSurfaceHeightMap().modifyHeight(16, 0, digAmount);
                            dirtyChunks.add(cornerChunk);
                        }
                    } else if (nx == 16 && nz == 16) {
                        // 右后角
                        ChunkPosition cornerChunk = new ChunkPosition(chunkX + 1, 0, chunkZ + 1);
                        Chunk cornerChunkObj = chunkManager.getChunk(cornerChunk);
                        if (cornerChunkObj != null && cornerChunkObj.hasTerrainData()) {
                            cornerChunkObj.getSurfaceHeightMap().modifyHeight(0, 0, digAmount);
                            dirtyChunks.add(cornerChunk);
                        }
                    }
                }
            }
        }

        // 标记所有受影响的区块为脏
        for (ChunkPosition dirtyChunkPos : dirtyChunks) {
            Chunk dirtyChunk = chunkManager.getChunk(dirtyChunkPos);
            if (dirtyChunk != null) {
                dirtyChunk.setDirty();
            }
        }
    }
}
