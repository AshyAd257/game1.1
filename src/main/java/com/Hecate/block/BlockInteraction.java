package com.Hecate.block;

import com.jme3.collision.CollisionResults;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;
import com.Hecate.world.ChunkManager;
import com.Hecate.utils.BlockUtils;

/**
 * 方块交互系统 - 处理方块的放置、破坏和查询
 */
public class BlockInteraction {
    private final Camera camera;
    private final Node worldNode;
    private final ChunkManager chunkManager;
    private final BlockRegistry blockRegistry;

    // 交互距离
    private static final float INTERACTION_DISTANCE = 5.0f;

    /**
     * 构造函数（依赖注入）
     *
     * @param camera 相机
     * @param worldNode 世界节点
     * @param chunkManager 区块管理器
     * @param blockRegistry 方块注册表（通过依赖注入传入）
     */
    public BlockInteraction(Camera camera, Node worldNode, ChunkManager chunkManager, BlockRegistry blockRegistry) {
        this.camera = camera;
        this.worldNode = worldNode;
        this.chunkManager = chunkManager;
        this.blockRegistry = blockRegistry;
    }

    /**
     * 构造函数（向后兼容）
     *
     * @param camera 相机
     * @param worldNode 世界节点
     * @param chunkManager 区块管理器
     * @deprecated 推荐使用 {@link #BlockInteraction(Camera, Node, ChunkManager, BlockRegistry)} 进行依赖注入
     */
    @Deprecated
    public BlockInteraction(Camera camera, Node worldNode, ChunkManager chunkManager) {
        this.camera = camera;
        this.worldNode = worldNode;
        this.chunkManager = chunkManager;
        this.blockRegistry = BlockRegistry.getInstance();
    }

    /**
     * 射线检测获取玩家正在看的方块
     */
    public BlockHitResult raycastBlock() {
        // 创建从相机位置发出的射线
        Ray ray = new Ray(camera.getLocation(), camera.getDirection());

        // 检测碰撞
        CollisionResults results = new CollisionResults();
        worldNode.collideWith(ray, results);

        if (results.size() > 0) {
            Vector3f hitPoint = results.getClosestCollision().getContactPoint();
            Vector3f normal = results.getClosestCollision().getContactNormal();

            // 检查距离
            float distance = camera.getLocation().distance(hitPoint);
            if (distance <= INTERACTION_DISTANCE) {
                // 计算方块坐标
                Vector3f blockPos = getBlockPosition(hitPoint, normal, false);
                return new BlockHitResult(true, blockPos, hitPoint, normal, distance);
            }
        }

        return new BlockHitResult(false, null, null, null, 0);
    }

    /**
     * 破坏方块
     */
    public boolean breakBlock() {
        BlockHitResult hitResult = raycastBlock();
        if (!hitResult.isHit()) {
            return false;
        }

        Vector3f blockPos = hitResult.getBlockPosition();
        String currentBlock = BlockUtils.getBlockAt(blockPos, chunkManager);

        if (!"air".equals(currentBlock)) {
            return BlockUtils.setBlockAt(blockPos, "air", chunkManager);
        }

        return false;
    }

    /**
     * 放置方块
     */
    public boolean placeBlock(String blockId) {
        BlockHitResult hitResult = raycastBlock();
        if (!hitResult.isHit()) {
            return false;
        }

        // 获取放置位置（在被击中面的外侧）
        Vector3f placePos = getBlockPosition(hitResult.getHitPoint(), hitResult.getNormal(), true);
        String currentBlock = BlockUtils.getBlockAt(placePos, chunkManager);

        if ("air".equals(currentBlock) && blockRegistry.getBlock(blockId) != null) {
            return BlockUtils.setBlockAt(placePos, blockId, chunkManager);
        }

        return false;
    }

    /**
     * 获取方块在世界中的位置
     */
    private Vector3f getBlockPosition(Vector3f hitPoint, Vector3f normal, boolean placeMode) {
        Vector3f blockPos = hitPoint.clone();

        if (placeMode) {
            // 放置模式：在被击中面的外侧
            blockPos.addLocal(normal.mult(0.1f));
        } else {
            // 破坏模式：在被击中面的内侧
            blockPos.subtractLocal(normal.mult(0.1f));
        }

        // 转换为方块坐标
        return new Vector3f(
                (float) Math.floor(blockPos.x),
                (float) Math.floor(blockPos.y),
                (float) Math.floor(blockPos.z)
        );
    }

    /**
     * 方块射线检测结果
     */
    public static class BlockHitResult {
        private final boolean hit;
        private final Vector3f blockPosition;
        private final Vector3f hitPoint;
        private final Vector3f normal;
        private final float distance;

        public BlockHitResult(boolean hit, Vector3f blockPosition, Vector3f hitPoint, Vector3f normal, float distance) {
            this.hit = hit;
            this.blockPosition = blockPosition;
            this.hitPoint = hitPoint;
            this.normal = normal;
            this.distance = distance;
        }

        public boolean isHit() { return hit; }
        public Vector3f getBlockPosition() { return blockPosition; }
        public Vector3f getHitPoint() { return hitPoint; }
        public Vector3f getNormal() { return normal; }
        public float getDistance() { return distance; }
    }
}
