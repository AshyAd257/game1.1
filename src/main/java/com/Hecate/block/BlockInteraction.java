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
    private java.util.function.Supplier<Vector3f> playerPositionSupplier;

    // 交互距离。射线本身必须从摄像机发出、沿摄像机朝向（否则"准星指哪"和"实际判定点"
    // 会因为第三人称镜头与玩家身体的空间错位而不一致——曾经改成从玩家身体发射线，
    // 结果射线方向仍是镜头朝向，等于两条平行但错位的线，导致判定位置随镜头拉远/旋转
    // 随机漂移）。但"够不够近"这个距离判断要用玩家本体位置计算，不能用摄像机位置，
    // 否则第三人称镜头被拉远到玩家身后时，即使玩家贴着方块也会被误判"太远"。
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
     * 设置玩家本体位置提供者，仅用于计算交互距离（不影响射线本身，射线始终从摄像机发出）
     */
    public void setPlayerPositionSupplier(java.util.function.Supplier<Vector3f> playerPositionSupplier) {
        this.playerPositionSupplier = playerPositionSupplier;
    }

    private Vector3f getDistanceReferencePosition() {
        if (playerPositionSupplier != null) {
            Vector3f pos = playerPositionSupplier.get();
            if (pos != null) {
                return pos;
            }
        }
        // 未设置时回退到摄像机位置（向后兼容第一人称场景，此时摄像机=玩家眼睛位置）
        return camera.getLocation();
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
        // 射线必须从摄像机发出、沿摄像机朝向，这样"准星指哪"和"实际判定点"才能保证一致
        Ray ray = new Ray(camera.getLocation(), camera.getDirection());

        // 检测碰撞
        CollisionResults results = new CollisionResults();
        worldNode.collideWith(ray, results);

        if (results.size() > 0) {
            Vector3f hitPoint = results.getClosestCollision().getContactPoint();
            Vector3f normal = results.getClosestCollision().getContactNormal();

            // 距离判断用玩家本体位置（而不是摄像机位置），避免第三人称镜头拉远时
            // 玩家明明贴着方块却被误判"太远"
            float distance = getDistanceReferencePosition().distance(hitPoint);
            if (distance <= INTERACTION_DISTANCE) {
                // 计算被瞄准的方块坐标（供挖方块使用）
                Vector3f blockPos = getHitBlockPosition(hitPoint, normal);
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

        Vector3f placePos = computePlacementPosition(hitResult);
        String currentBlock = BlockUtils.getBlockAt(placePos, chunkManager);

        if ("air".equals(currentBlock) && blockRegistry.getBlock(blockId) != null) {
            // 方向性方块（如原木）依据被点击面的法线换算出目标朝向的具体变体ID，
            // 非方向性方块原样返回blockId，两者用同一套逻辑无需分支
            String resolvedBlockId = blockRegistry.resolvePlacementVariant(blockId, hitResult.getNormal());
            return BlockUtils.setBlockAt(placePos, resolvedBlockId, chunkManager);
        }

        return false;
    }

    /**
     * 预览"如果现在放置方块会落在哪一格"，不实际修改世界（供UI画放置预览线框用）。
     * 只有目标格子确实是空气（真的能放）时才返回坐标，否则返回null（不显示预览框）。
     */
    public Vector3f previewPlacementPosition() {
        BlockHitResult hitResult = raycastBlock();
        if (!hitResult.isHit()) {
            return null;
        }

        Vector3f placePos = computePlacementPosition(hitResult);
        String currentBlock = BlockUtils.getBlockAt(placePos, chunkManager);
        return "air".equals(currentBlock) ? placePos : null;
    }

    /**
     * 放置位置 = round(命中点 + 法线 * 0.5)，逐分量四舍五入（Minecraft同款公式）。
     * 这个公式同时兼顾了两种命中面：
     * - 贴着已有方块摆放时，命中点正好在方块表面的半整数边界上，round()稳定跳到隔壁格子中心，
     *   不受射线-三角形碰撞在边界附近的浮点误差影响（之前用floor+微小偏移再整数步进的方式，
     *   在命中点各别情况下会向下多跳一格，导致方块深深嵌入地面——本质是floor()对"命中点恰好
     *   比整数高出一点点"这种情况有系统性的向下偏差，偏差最坏能到完整的1格）。
     * - 贴着连续地形摆放时，地形高度可以是任意小数，round()保证取到最近的格子中心，
     *   最坏误差被压缩到±0.5格（这是格子分辨率带来的固有量化误差，无法完全消除），
     *   不会再出现"随机沉入地面"的系统性偏差。
     */
    private Vector3f computePlacementPosition(BlockHitResult hitResult) {
        Vector3f hitPoint = hitResult.getHitPoint();
        Vector3f normal = hitResult.getNormal();
        return new Vector3f(
                (float) Math.round(hitPoint.x + normal.x * 0.5f),
                (float) Math.round(hitPoint.y + normal.y * 0.5f),
                (float) Math.round(hitPoint.z + normal.z * 0.5f)
        );
    }

    /**
     * 检查方块ID是否已在注册表中注册（供 /give 等命令校验，避免把不存在的方块塞进快捷栏）
     */
    public boolean isBlockValid(String blockId) {
        return blockRegistry.getBlock(blockId) != null;
    }

    /**
     * 获取被瞄准的方块坐标（供挖方块直接使用，也是放置方块时"贴着哪个方块摆"的基准点）。
     * 命中点落在方块表面（半整数边界），往内侧挪一点点确保落在被瞄准的那一格而不是隔壁。
     * 这里只需要小幅内推（不需要跨越整个格子），命中点本身的浮点误差通常远小于0.1，
     * 不会导致像放置逻辑那样在格子边界附近的不稳定取整问题。
     */
    private Vector3f getHitBlockPosition(Vector3f hitPoint, Vector3f normal) {
        Vector3f blockPos = hitPoint.clone();
        blockPos.subtractLocal(normal.mult(0.1f));

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
