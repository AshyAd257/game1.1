package com.Hecate.world;

import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.Hecate.utils.LogUtils;

/**
 * 地形编辑器
 * 处理地形高度修改、体积计算等
 */
public class TerrainEditor {

    private final ChunkManager chunkManager;
    private final Node worldNode;

    // 编辑参数
    private static final float MAX_EDIT_DISTANCE = 10.0f; // 最大编辑距离
    private static final float VERTEX_DETECT_RADIUS = 0.5f; // 顶点检测半径

    // 铲子工具参数
    private float shovelRadius = 2.0f; // 铲子影响半径（顶点数）
    private boolean shovelSnapEnabled = false; // 是否启用0.5步进对齐

    public TerrainEditor(ChunkManager chunkManager, Node worldNode) {
        this.chunkManager = chunkManager;
        this.worldNode = worldNode;
    }

    /**
     * 射线检测结果
     */
    public static class TerrainHitResult {
        public final Vector3f hitPoint; // 击中点世界坐标
        public final ChunkPosition chunkPos; // 所在区块
        public final int vertexX, vertexZ; // 最近的顶点（区块内坐标）
        public final float distance; // 距离

        public TerrainHitResult(Vector3f hitPoint, ChunkPosition chunkPos,
                               int vertexX, int vertexZ, float distance) {
            this.hitPoint = hitPoint;
            this.chunkPos = chunkPos;
            this.vertexX = vertexX;
            this.vertexZ = vertexZ;
            this.distance = distance;
        }
    }

    /**
     * 射线检测到地形
     * @param ray 射线
     * @return 击中结果，如果未击中返回 null
     */
    public TerrainHitResult raycastTerrain(Ray ray) {
        CollisionResults results = new CollisionResults();
        worldNode.collideWith(ray, results);

        // 查找最近的地形碰撞
        for (CollisionResult result : results) {
            if (result.getGeometry().getName().startsWith("Terrain_")) {
                Vector3f hitPoint = result.getContactPoint();
                float distance = result.getDistance();

                if (distance > MAX_EDIT_DISTANCE) {
                    continue; // 超出编辑距离
                }

                // 计算击中点所在的区块
                ChunkPosition chunkPos = worldToChunkPosition(hitPoint);
                Chunk chunk = chunkManager.getChunk(chunkPos);

                if (chunk == null || !chunk.hasTerrainData()) {
                    continue;
                }

                // 计算击中点在区块内的坐标
                Vector3f chunkWorldPos = chunk.getWorldPosition();
                float localX = hitPoint.x - chunkWorldPos.x;
                float localZ = hitPoint.z - chunkWorldPos.z;

                // 找到最近的顶点
                int vertexX = Math.round(localX);
                int vertexZ = Math.round(localZ);

                // 限制在有效范围内
                vertexX = Math.max(0, Math.min(HeightMap.VERTEX_COUNT - 1, vertexX));
                vertexZ = Math.max(0, Math.min(HeightMap.VERTEX_COUNT - 1, vertexZ));

                return new TerrainHitResult(hitPoint, chunkPos, vertexX, vertexZ, distance);
            }
        }

        return null;
    }

    /**
     * 修改顶点高度（徒手工具）
     * @param hitResult 射线检测结果
     * @param deltaHeight 高度变化量
     * @return 实际消耗的体积（如果是挖掘返回负值）
     */
    public float modifyVertexHeight(TerrainHitResult hitResult, float deltaHeight) {
        if (hitResult == null) {
            return 0.0f;
        }

        Chunk chunk = chunkManager.getChunk(hitResult.chunkPos);
        if (chunk == null || !chunk.hasTerrainData()) {
            return 0.0f;
        }

        HeightMap heightMap = chunk.getSurfaceHeightMap();

        // 修改主顶点高度
        float actualDelta = heightMap.modifyHeight(hitResult.vertexX, hitResult.vertexZ, deltaHeight);

        if (Math.abs(actualDelta) < 0.001f) {
            return 0.0f; // 没有实际变化
        }

        // 标记区块为 dirty，触发重新渲染
        chunk.setDirty();

        // 检查是否需要更新相邻区块（边界顶点）
        updateAdjacentChunks(hitResult.chunkPos, hitResult.vertexX, hitResult.vertexZ, actualDelta);

        // 计算体积变化
        float volumeChange = HeightMap.calculateVolumeChange(actualDelta);

        LogUtils.debug(TerrainEditor.class,
            String.format("修改顶点 (%d, %d) 高度 %.3f，体积变化 %.3f",
                hitResult.vertexX, hitResult.vertexZ, actualDelta, volumeChange));

        return volumeChange;
    }

    /**
     * 更新相邻区块的共享顶点
     */
    private void updateAdjacentChunks(ChunkPosition centerChunk, int vertexX, int vertexZ, float deltaHeight) {
        // 如果顶点在边界上，需要更新相邻区块
        boolean onLeftEdge = (vertexX == 0);
        boolean onRightEdge = (vertexX == HeightMap.VERTEX_COUNT - 1);
        boolean onFrontEdge = (vertexZ == 0);
        boolean onBackEdge = (vertexZ == HeightMap.VERTEX_COUNT - 1);

        if (!onLeftEdge && !onRightEdge && !onFrontEdge && !onBackEdge) {
            return; // 不在边界，无需更新相邻区块
        }

        // 左边相邻区块
        if (onLeftEdge) {
            updateAdjacentVertex(centerChunk, -1, 0, HeightMap.VERTEX_COUNT - 1, vertexZ, deltaHeight);
        }

        // 右边相邻区块
        if (onRightEdge) {
            updateAdjacentVertex(centerChunk, 1, 0, 0, vertexZ, deltaHeight);
        }

        // 前边相邻区块
        if (onFrontEdge) {
            updateAdjacentVertex(centerChunk, 0, -1, vertexX, HeightMap.VERTEX_COUNT - 1, deltaHeight);
        }

        // 后边相邻区块
        if (onBackEdge) {
            updateAdjacentVertex(centerChunk, 0, 1, vertexX, 0, deltaHeight);
        }

        // 角落顶点（需要更新对角相邻区块）
        if (onLeftEdge && onFrontEdge) {
            updateAdjacentVertex(centerChunk, -1, -1, HeightMap.VERTEX_COUNT - 1, HeightMap.VERTEX_COUNT - 1, deltaHeight);
        }
        if (onRightEdge && onFrontEdge) {
            updateAdjacentVertex(centerChunk, 1, -1, 0, HeightMap.VERTEX_COUNT - 1, deltaHeight);
        }
        if (onLeftEdge && onBackEdge) {
            updateAdjacentVertex(centerChunk, -1, 1, HeightMap.VERTEX_COUNT - 1, 0, deltaHeight);
        }
        if (onRightEdge && onBackEdge) {
            updateAdjacentVertex(centerChunk, 1, 1, 0, 0, deltaHeight);
        }
    }

    /**
     * 更新相邻区块的指定顶点
     */
    private void updateAdjacentVertex(ChunkPosition centerChunk, int dx, int dz,
                                     int targetVertexX, int targetVertexZ, float deltaHeight) {
        ChunkPosition adjacentPos = new ChunkPosition(
            centerChunk.getX() + dx,
            centerChunk.getY(),
            centerChunk.getZ() + dz
        );

        Chunk adjacentChunk = chunkManager.getChunk(adjacentPos);
        if (adjacentChunk != null && adjacentChunk.hasTerrainData()) {
            HeightMap adjacentHeightMap = adjacentChunk.getSurfaceHeightMap();
            adjacentHeightMap.modifyHeight(targetVertexX, targetVertexZ, deltaHeight);
            adjacentChunk.setDirty();
        }
    }

    /**
     * 世界坐标转区块坐标
     */
    private ChunkPosition worldToChunkPosition(Vector3f worldPos) {
        return new ChunkPosition(
            Math.floorDiv((int) Math.floor(worldPos.x), Chunk.SIZE),
            Math.floorDiv((int) Math.floor(worldPos.y), Chunk.SIZE),
            Math.floorDiv((int) Math.floor(worldPos.z), Chunk.SIZE)
        );
    }

    /**
     * 获取顶点世界坐标
     */
    public Vector3f getVertexWorldPosition(ChunkPosition chunkPos, int vertexX, int vertexZ) {
        Chunk chunk = chunkManager.getChunk(chunkPos);
        if (chunk == null || !chunk.hasTerrainData()) {
            return null;
        }

        Vector3f chunkWorldPos = chunk.getWorldPosition();
        float height = chunk.getSurfaceHeightMap().getHeight(vertexX, vertexZ);

        return new Vector3f(
            chunkWorldPos.x + vertexX,
            chunkWorldPos.y + height,
            chunkWorldPos.z + vertexZ
        );
    }

    // ========== 铲子工具 ==========

    /**
     * 使用铲子工具平整区域
     * @param hitResult 射线检测结果（中心点）
     * @return 总体积变化
     */
    public float flattenArea(TerrainHitResult hitResult) {
        if (hitResult == null) {
            return 0.0f;
        }

        Chunk centerChunk = chunkManager.getChunk(hitResult.chunkPos);
        if (centerChunk == null || !centerChunk.hasTerrainData()) {
            return 0.0f;
        }

        // 获取中心顶点的高度作为目标高度
        float targetHeight = centerChunk.getSurfaceHeightMap().getHeight(
            hitResult.vertexX, hitResult.vertexZ);

        // 可选：对齐到0.5步进
        if (shovelSnapEnabled) {
            targetHeight = Math.round(targetHeight * 2.0f) / 2.0f;
        }

        // 计算中心顶点的世界坐标
        Vector3f centerWorldPos = new Vector3f(
            hitResult.chunkPos.getX() * Chunk.SIZE + hitResult.vertexX,
            0,
            hitResult.chunkPos.getZ() * Chunk.SIZE + hitResult.vertexZ
        );

        float totalVolumeChange = 0.0f;
        java.util.Set<ChunkPosition> affectedChunks = new java.util.HashSet<>();

        // 计算影响范围（世界坐标）
        int minWorldX = (int) Math.floor(centerWorldPos.x - shovelRadius);
        int maxWorldX = (int) Math.ceil(centerWorldPos.x + shovelRadius);
        int minWorldZ = (int) Math.floor(centerWorldPos.z - shovelRadius);
        int maxWorldZ = (int) Math.ceil(centerWorldPos.z + shovelRadius);

        // 遍历所有受影响的顶点
        for (int worldX = minWorldX; worldX <= maxWorldX; worldX++) {
            for (int worldZ = minWorldZ; worldZ <= maxWorldZ; worldZ++) {
                // 检查是否在圆形半径内
                float dx = worldX - centerWorldPos.x;
                float dz = worldZ - centerWorldPos.z;
                float distSq = dx * dx + dz * dz;

                if (distSq > shovelRadius * shovelRadius) {
                    continue; // 超出半径
                }

                // 计算该顶点所在的区块和局部坐标
                int chunkX = Math.floorDiv(worldX, Chunk.SIZE);
                int chunkZ = Math.floorDiv(worldZ, Chunk.SIZE);
                int localX = Math.floorMod(worldX, HeightMap.VERTEX_COUNT);
                int localZ = Math.floorMod(worldZ, HeightMap.VERTEX_COUNT);

                ChunkPosition chunkPos = new ChunkPosition(chunkX, 0, chunkZ);
                Chunk chunk = chunkManager.getChunk(chunkPos);

                if (chunk == null || !chunk.hasTerrainData()) {
                    continue;
                }

                // 获取当前高度
                HeightMap heightMap = chunk.getSurfaceHeightMap();
                float currentHeight = heightMap.getHeight(localX, localZ);

                // 计算高度变化
                float deltaHeight = targetHeight - currentHeight;

                if (Math.abs(deltaHeight) < 0.001f) {
                    continue; // 高度无变化
                }

                // 直接设置高度（而不是增量修改）
                heightMap.setHeight(localX, localZ, targetHeight);

                // 累积体积变化
                totalVolumeChange += HeightMap.calculateVolumeChange(deltaHeight);

                // 标记区块为dirty
                affectedChunks.add(chunkPos);
            }
        }

        // 标记所有受影响的区块重新渲染
        for (ChunkPosition pos : affectedChunks) {
            Chunk chunk = chunkManager.getChunk(pos);
            if (chunk != null) {
                chunk.setDirty();
            }
        }

        return totalVolumeChange;
    }

    /**
     * 设置铲子半径
     */
    public void setShovelRadius(float radius) {
        this.shovelRadius = Math.max(0.5f, Math.min(8.0f, radius));
    }

    /**
     * 获取铲子半径
     */
    public float getShovelRadius() {
        return shovelRadius;
    }

    /**
     * 切换铲子对齐模式
     */
    public void toggleShovelSnap() {
        this.shovelSnapEnabled = !this.shovelSnapEnabled;
    }

    /**
     * 设置铲子对齐模式
     */
    public void setShovelSnapEnabled(boolean enabled) {
        this.shovelSnapEnabled = enabled;
    }

    /**
     * 获取铲子对齐模式
     */
    public boolean isShovelSnapEnabled() {
        return shovelSnapEnabled;
    }
}
