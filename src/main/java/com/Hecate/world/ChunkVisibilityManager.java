package com.Hecate.world;

import com.jme3.bounding.BoundingBox;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 管理区块的可见性和加载/卸载
 * 实现视锥体剔除优化
 */
public class ChunkVisibilityManager {
    private final Camera camera;
    private final ChunkManager chunkManager;

    // 可见距离（以区块为单位）
    private int viewDistance = 8;

    // 当前可见的区块
    private final Set<ChunkPosition> visibleChunks = ConcurrentHashMap.newKeySet();

    // 上一帧相机的区块位置
    private ChunkPosition lastCameraChunkPos = null;

    public ChunkVisibilityManager(Camera camera, ChunkManager chunkManager) {
        this.camera = camera;
        this.chunkManager = chunkManager;
    }

    /**
     * 更新可见区块
     * 应该每帧调用
     */
    public void update() {
        // 获取相机当前位置
        Vector3f cameraPos = camera.getLocation();
        ChunkPosition currentCameraChunkPos = worldToChunkPosition(cameraPos);

        // 如果相机没有移动到新的区块，不需要更新
        if (currentCameraChunkPos.equals(lastCameraChunkPos)) {
            return;
        }

        lastCameraChunkPos = currentCameraChunkPos;

        // 计算新的可见区块集合
        Set<ChunkPosition> newVisibleChunks = new HashSet<>();

        // 遍历视距内的所有区块
        for (int dx = -viewDistance; dx <= viewDistance; dx++) {
            for (int dy = -viewDistance; dy <= viewDistance; dy++) {
                for (int dz = -viewDistance; dz <= viewDistance; dz++) {
                    ChunkPosition chunkPos = new ChunkPosition(
                            currentCameraChunkPos.getX() + dx,
                            currentCameraChunkPos.getY() + dy,
                            currentCameraChunkPos.getZ() + dz
                    );

                    // 检查区块是否在视锥体内
                    if (isChunkInFrustum(chunkPos)) {
                        newVisibleChunks.add(chunkPos);
                    }
                }
            }
        }

        // 找出需要隐藏的区块（之前可见但现在不可见）
        Set<ChunkPosition> chunksToHide = new HashSet<>(visibleChunks);
        chunksToHide.removeAll(newVisibleChunks);

        // 找出需要显示的区块（现在可见但之前不可见）
        Set<ChunkPosition> chunksToShow = new HashSet<>(newVisibleChunks);
        chunksToShow.removeAll(visibleChunks);

        // 隐藏不再可见的区块
        for (ChunkPosition pos : chunksToHide) {
            hideChunk(pos);
        }

        // 显示新可见的区块
        for (ChunkPosition pos : chunksToShow) {
            showChunk(pos);
        }

        // 更新可见区块集合
        visibleChunks.clear();
        visibleChunks.addAll(newVisibleChunks);
    }

    /**
     * 检查区块是否在视锥体内
     */
    private boolean isChunkInFrustum(ChunkPosition chunkPos) {
        // 创建区块的边界盒
        Vector3f min = new Vector3f(
                chunkPos.getX() * Chunk.SIZE,
                chunkPos.getY() * Chunk.SIZE,
                chunkPos.getZ() * Chunk.SIZE
        );
        Vector3f max = min.add(new Vector3f(Chunk.SIZE, Chunk.SIZE, Chunk.SIZE));

        BoundingBox chunkBounds = new BoundingBox(min, max);

        // 使用JME3的视锥体剔除
        return camera.contains(chunkBounds) != Camera.FrustumIntersect.Outside;
    }

    /**
     * 显示区块
     */
    private void showChunk(ChunkPosition pos) {
        Chunk chunk = chunkManager.getChunk(pos);
        if (chunk != null) {
            Node chunkNode = chunk.getChunkNode();
            if (chunkNode != null && chunkNode.getCullHint() != Node.CullHint.Dynamic) {
                chunkNode.setCullHint(Node.CullHint.Dynamic);
            }
        }
    }

    /**
     * 隐藏区块
     */
    private void hideChunk(ChunkPosition pos) {
        Chunk chunk = chunkManager.getChunk(pos);
        if (chunk != null) {
            Node chunkNode = chunk.getChunkNode();
            if (chunkNode != null && chunkNode.getCullHint() != Node.CullHint.Always) {
                chunkNode.setCullHint(Node.CullHint.Always);
            }
        }
    }

    /**
     * 当方块改变时调用
     * 检查是否需要更新相邻区块
     */
    public void onBlockChanged(Vector3f worldPosition) {
        // 获取方块在区块内的局部坐标
        int localX = ((int) Math.floor(worldPosition.x)) % Chunk.SIZE;
        int localY = ((int) Math.floor(worldPosition.y)) % Chunk.SIZE;
        int localZ = ((int) Math.floor(worldPosition.z)) % Chunk.SIZE;

        // 如果方块在区块边界上，可能需要更新相邻区块
        ChunkPosition chunkPos = worldToChunkPosition(worldPosition);

        // 检查是否在区块边界
        if (localX == 0 || localX == Chunk.SIZE - 1 ||
                localY == 0 || localY == Chunk.SIZE - 1 ||
                localZ == 0 || localZ == Chunk.SIZE - 1) {

            // 标记相邻区块需要重新渲染
            updateNeighborChunks(chunkPos, localX, localY, localZ);
        }
    }

    /**
     * 更新相邻区块
     */
    private void updateNeighborChunks(ChunkPosition pos, int localX, int localY, int localZ) {
        // 检查每个轴
        if (localX == 0) {
            markChunkDirty(new ChunkPosition(pos.getX() - 1, pos.getY(), pos.getZ()));
        }
        if (localX == Chunk.SIZE - 1) {
            markChunkDirty(new ChunkPosition(pos.getX() + 1, pos.getY(), pos.getZ()));
        }

        if (localY == 0) {
            markChunkDirty(new ChunkPosition(pos.getX(), pos.getY() - 1, pos.getZ()));
        }
        if (localY == Chunk.SIZE - 1) {
            markChunkDirty(new ChunkPosition(pos.getX(), pos.getY() + 1, pos.getZ()));
        }

        if (localZ == 0) {
            markChunkDirty(new ChunkPosition(pos.getX(), pos.getY(), pos.getZ() - 1));
        }
        if (localZ == Chunk.SIZE - 1) {
            markChunkDirty(new ChunkPosition(pos.getX(), pos.getY(), pos.getZ() + 1));
        }
    }

    /**
     * 标记区块需要重新渲染
     */
    private void markChunkDirty(ChunkPosition pos) {
        Chunk chunk = chunkManager.getChunk(pos);
        if (chunk != null) {
            chunk.setDirty();
        }
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

    // Getter/Setter
    public int getViewDistance() {
        return viewDistance;
    }

    public void setViewDistance(int viewDistance) {
        this.viewDistance = viewDistance;
        // 触发重新计算可见区块
        lastCameraChunkPos = null;
    }
}