package com.Hecate.camera;

import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.Node;
import com.Hecate.world.ChunkManager;
import com.Hecate.world.Chunk;
import com.Hecate.world.ChunkPosition;
import com.Hecate.utils.LogUtils;

import java.util.HashSet;
import java.util.Set;

/**
 * 摄像机方块剔除系统
 * 当摄像机穿过方块时，让周围圆形范围内的方块变透明
 */
public class CameraBlockCulling {

    private final Camera camera;
    private final ChunkManager chunkManager;

    // 透明化参数
    private static final float CULLING_RADIUS = 3.5f; // 透明化半径（方块单位）
    private static final float TRANSPARENT_ALPHA = 0.0f; // 透明度（0=全透明，1=不透明）

    // 当前透明化的方块集合
    private final Set<BlockPosition> transparentBlocks = new HashSet<>();

    public CameraBlockCulling(Camera camera, ChunkManager chunkManager) {
        this.camera = camera;
        this.chunkManager = chunkManager;
    }

    /**
     * 更新透明化状态
     * @param playerPosition 玩家位置
     */
    public void update(Vector3f playerPosition) {
        // 获取摄像机位置
        Vector3f cameraPos = camera.getLocation();

        // 检测摄像机到玩家之间的方块
        Set<BlockPosition> blocksInPath = getBlocksInPath(cameraPos, playerPosition);

        // 在路径上的方块周围应用透明化
        Set<BlockPosition> newTransparentBlocks = new HashSet<>();
        for (BlockPosition blockPos : blocksInPath) {
            // 添加周围圆形范围内的方块
            addBlocksInRadius(blockPos, CULLING_RADIUS, newTransparentBlocks);
        }

        // 检查是否有变化
        if (!transparentBlocks.equals(newTransparentBlocks)) {
            // 找出所有受影响的区块
            Set<ChunkPosition> affectedChunks = new HashSet<>();

            // 旧的透明方块所在区块
            for (BlockPosition pos : transparentBlocks) {
                affectedChunks.add(blockToChunkPosition(pos));
            }

            // 新的透明方块所在区块
            for (BlockPosition pos : newTransparentBlocks) {
                affectedChunks.add(blockToChunkPosition(pos));
            }

            // 标记这些区块为 dirty，强制重新渲染
            for (ChunkPosition chunkPos : affectedChunks) {
                Chunk chunk = chunkManager.getChunk(chunkPos);
                if (chunk != null) {
                    chunk.setDirty();
                }
            }
        }

        // 更新当前透明方块集合
        transparentBlocks.clear();
        transparentBlocks.addAll(newTransparentBlocks);
    }

    /**
     * 检查某个方块是否应该被剔除
     */
    public boolean shouldCullBlock(BlockPosition blockPos) {
        return transparentBlocks.contains(blockPos);
    }

    /**
     * 获取从摄像机到玩家路径上的所有方块
     */
    private Set<BlockPosition> getBlocksInPath(Vector3f start, Vector3f end) {
        Set<BlockPosition> blocks = new HashSet<>();

        // 使用DDA（数字微分分析器）算法进行体素遍历
        Vector3f direction = end.subtract(start);
        float distance = direction.length();
        direction.normalizeLocal();

        // 步进距离（0.5个方块单位，确保不会遗漏方块）
        float stepSize = 0.5f;
        int steps = (int) (distance / stepSize);

        for (int i = 0; i <= steps; i++) {
            Vector3f point = start.add(direction.mult(i * stepSize));
            BlockPosition blockPos = worldToBlockPosition(point);

            // 检查该位置是否有非空气方块
            if (isBlockSolid(blockPos)) {
                blocks.add(blockPos);
            }
        }

        return blocks;
    }

    /**
     * 添加指定方块周围圆形范围内的所有方块
     */
    private void addBlocksInRadius(BlockPosition center, float radius, Set<BlockPosition> result) {
        int radiusInt = (int) Math.ceil(radius);

        for (int dx = -radiusInt; dx <= radiusInt; dx++) {
            for (int dy = -radiusInt; dy <= radiusInt; dy++) {
                for (int dz = -radiusInt; dz <= radiusInt; dz++) {
                    // 检查是否在圆形范围内
                    float distSq = dx * dx + dy * dy + dz * dz;
                    if (distSq <= radius * radius) {
                        BlockPosition pos = new BlockPosition(
                            center.x + dx,
                            center.y + dy,
                            center.z + dz
                        );

                        // 只对非空气方块应用透明化
                        if (isBlockSolid(pos)) {
                            result.add(pos);
                        }
                    }
                }
            }
        }
    }

    /**
     * 检查方块是否为固体（非空气）
     */
    private boolean isBlockSolid(BlockPosition blockPos) {
        ChunkPosition chunkPos = blockToChunkPosition(blockPos);
        Chunk chunk = chunkManager.getChunk(chunkPos);

        if (chunk == null) {
            return false;
        }

        // 计算方块在区块内的相对位置
        int localX = Math.floorMod(blockPos.x, Chunk.SIZE);
        int localY = Math.floorMod(blockPos.y, Chunk.SIZE);
        int localZ = Math.floorMod(blockPos.z, Chunk.SIZE);

        String blockId = chunk.getBlockId(localX, localY, localZ);
        return blockId != null && !"air".equals(blockId);
    }

    /**
     * 世界坐标转换为方块坐标
     */
    private BlockPosition worldToBlockPosition(Vector3f worldPos) {
        return new BlockPosition(
            (int) Math.floor(worldPos.x),
            (int) Math.floor(worldPos.y),
            (int) Math.floor(worldPos.z)
        );
    }

    /**
     * 方块坐标转换为区块坐标
     */
    private ChunkPosition blockToChunkPosition(BlockPosition blockPos) {
        return new ChunkPosition(
            Math.floorDiv(blockPos.x, Chunk.SIZE),
            Math.floorDiv(blockPos.y, Chunk.SIZE),
            Math.floorDiv(blockPos.z, Chunk.SIZE)
        );
    }

    /**
     * 清理所有透明化效果
     */
    public void cleanup() {
        // 找出所有受影响的区块并标记为 dirty
        Set<ChunkPosition> affectedChunks = new HashSet<>();
        for (BlockPosition pos : transparentBlocks) {
            affectedChunks.add(blockToChunkPosition(pos));
        }

        for (ChunkPosition chunkPos : affectedChunks) {
            Chunk chunk = chunkManager.getChunk(chunkPos);
            if (chunk != null) {
                chunk.setDirty();
            }
        }

        transparentBlocks.clear();
    }

    /**
     * 方块位置类（公开以便外部使用）
     */
    public static class BlockPosition {
        final int x, y, z;

        public BlockPosition(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof BlockPosition)) return false;
            BlockPosition that = (BlockPosition) o;
            return x == that.x && y == that.y && z == that.z;
        }

        @Override
        public int hashCode() {
            int result = x;
            result = 31 * result + y;
            result = 31 * result + z;
            return result;
        }
    }
}
