package com.Hecate.physics;

import com.Hecate.block.BlockShapeRegistry;
import com.Hecate.world.ChunkManager;
import com.Hecate.world.ChunkPosition;
import com.Hecate.world.Chunk;
import com.jme3.math.Vector3f;

/**
 * 碰撞检测管理器
 * 与现有的ChunkManager系统完全兼容
 */
public class CollisionManager {
    private ChunkManager chunkManager;
    // 各方块ID的实际碰撞盒尺寸（如wood1这种细柱子远小于满格）。为null时退化为
    // 所有方块都是满格1x1x1实心立方体（向后兼容未注入时的行为）。
    private BlockShapeRegistry shapeRegistry;

    // 碰撞检测参数
    private static final float COLLISION_TOLERANCE = 0.001f;

    public CollisionManager() {
        this.chunkManager = null;
    }

    public CollisionManager(ChunkManager chunkManager) {
        this.chunkManager = chunkManager;
    }

    /**
     * 设置ChunkManager
     */
    public void setChunkManager(ChunkManager chunkManager) {
        this.chunkManager = chunkManager;

    }

    /**
     * 获取ChunkManager
     */
    public ChunkManager getChunkManager() {
        return chunkManager;
    }

    /**
     * 设置方块碰撞尺寸管理器（用于查询各方块实际碰撞盒大小，而不是一律满格）
     */
    public void setShapeRegistry(BlockShapeRegistry shapeRegistry) {
        this.shapeRegistry = shapeRegistry;
    }

    /**
     * PlayerController需要的方法 - 检测移动是否会发生碰撞
     */
    public boolean wouldCollide(AABB playerBox, Vector3f movement) {
        if (chunkManager == null) {
            return false;
        }

        // 计算移动后的包围盒
        AABB newBox = playerBox.translate(movement);

        // 获取需要检查的方块范围
        Vector3f min = newBox.getMinPoint();
        Vector3f max = newBox.getMaxPoint();

        int minX = (int) Math.floor(min.x);
        int minZ = (int) Math.floor(min.z);
        int maxX = (int) Math.floor(max.x);
        int maxZ = (int) Math.floor(max.z);

        return false;
    }

    /**
     * PlayerController需要的方法 - 获取滑动移动向量
     */
    public Vector3f getSlideMovement(AABB playerBox, Vector3f intendedMovement) {
        if (chunkManager == null) {
            return Vector3f.ZERO;
        }

        Vector3f finalMovement = Vector3f.ZERO.clone();

        // 分别处理 X, Y, Z 轴的移动
        Vector3f[] axisMovements = {
                new Vector3f(intendedMovement.x, 0, 0),  // X轴移动
                new Vector3f(0, intendedMovement.y, 0),  // Y轴移动
                new Vector3f(0, 0, intendedMovement.z)   // Z轴移动
        };

        Vector3f currentMovement = Vector3f.ZERO.clone();

        for (Vector3f axisMovement : axisMovements) {
            if (axisMovement.lengthSquared() > 0) {
                Vector3f testMovement = currentMovement.add(axisMovement);

                if (!wouldCollide(playerBox, testMovement)) {
                    currentMovement = testMovement;
                } else {
                    // 尝试部分移动
                    Vector3f partialMovement = getPartialMovement(playerBox, currentMovement, axisMovement);
                    currentMovement = currentMovement.add(partialMovement);
                }
            }
        }

        return currentMovement;
    }

    /**
     * 获取部分移动向量
     */
    private Vector3f getPartialMovement(AABB playerBox, Vector3f currentMovement, Vector3f axisMovement) {
        Vector3f direction = axisMovement.normalize();
        float maxDistance = axisMovement.length();

        // 二分查找最大可移动距离
        float minDistance = 0;
        float testDistance = maxDistance;

        for (int i = 0; i < 10; i++) { // 最大10次迭代
            Vector3f testMovement = currentMovement.add(direction.mult(testDistance));

            if (wouldCollide(playerBox, testMovement)) {
                maxDistance = testDistance;
                testDistance = (minDistance + testDistance) / 2;
            } else {
                minDistance = testDistance;
                testDistance = (testDistance + maxDistance) / 2;
            }

            if (maxDistance - minDistance < COLLISION_TOLERANCE) {
                break;
            }
        }

        return direction.mult(minDistance);
    }

    /**
     * 检测移动是否会发生碰撞，返回修正后的移动向量
     */
    public Vector3f checkCollision(AABB entityBox, Vector3f movement) {
        Vector3f correctedMovement = movement.clone();

        // 分别检测X、Y、Z轴的移动
        correctedMovement.x = checkAxisCollision(entityBox, movement.x, 0);

        // 更新实体位置后检测Y轴
        AABB movedBoxX = entityBox.offset(correctedMovement.x, 0, 0);
        correctedMovement.y = checkAxisCollision(movedBoxX, movement.y, 1);

        // 更新实体位置后检测Z轴
        AABB movedBoxXY = movedBoxX.offset(0, correctedMovement.y, 0);
        correctedMovement.z = checkAxisCollision(movedBoxXY, movement.z, 2);

        return correctedMovement;
    }

    /**
     * 检测单轴移动的碰撞
     */
    private float checkAxisCollision(AABB entityBox, float movement, int axis) {
        if (Math.abs(movement) < 0.001f) {
            return movement;
        }

        // 创建移动后的碰撞盒
        AABB movedBox;
        switch (axis) {
            case 0: // X轴
                movedBox = entityBox.offset(movement, 0, 0);
                break;
            case 1: // Y轴
                movedBox = entityBox.offset(0, movement, 0);
                break;
            case 2: // Z轴
                movedBox = entityBox.offset(0, 0, movement);
                break;
            default:
                return movement;
        }

        // 获取可能碰撞的方块范围。方块坐标n占据[n-0.5,n+0.5]（坐标本身是中心不是角点），
        // 所以查询范围要各往外扩0.5再取整，否则贴近格子边界时会漏检相邻格子
        // （例如movedBox.getMaxX()=3.7，真正该检查的格子中心是4，因为它占据[3.5,4.5]
        // 与3.7有重叠，但floor(3.7)=3会漏掉它）。多检查几个格子无害，intersects()会精确过滤。
        int minX = (int) Math.floor(movedBox.getMinX() - 0.5f);
        int maxX = (int) Math.floor(movedBox.getMaxX() + 0.5f);
        int minY = (int) Math.floor(movedBox.getMinY() - 0.5f);
        int maxY = (int) Math.floor(movedBox.getMaxY() + 0.5f);
        int minZ = (int) Math.floor(movedBox.getMinZ() - 0.5f);
        int maxZ = (int) Math.floor(movedBox.getMaxZ() + 0.5f);

        // 检测范围内的所有方块
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    String blockId = getBlockIdAt(x, y, z);
                    if (blockId != null && !blockId.equals("air")) {
                        AABB blockBox = buildBlockAABB(x, y, z, blockId);
                        if (movedBox.intersects(blockBox)) {
                            // 发生碰撞，计算修正后的移动距离
                            return calculateCorrectedMovement(entityBox, blockBox, movement, axis);
                        }
                    }
                }
            }
        }

        return movement; // 没有碰撞
    }

    /**
     * 计算修正后的移动距离
     */
    private float calculateCorrectedMovement(AABB entityBox, AABB blockBox, float movement, int axis) {
        switch (axis) {
            case 0: // X轴
                if (movement > 0) {
                    return blockBox.getMinX() - entityBox.getMaxX() - 0.001f;
                } else {
                    return blockBox.getMaxX() - entityBox.getMinX() + 0.001f;
                }
            case 1: // Y轴
                if (movement > 0) {
                    return blockBox.getMinY() - entityBox.getMaxY() - 0.001f;
                } else {
                    return blockBox.getMaxY() - entityBox.getMinY() + 0.001f;
                }
            case 2: // Z轴
                if (movement > 0) {
                    return blockBox.getMinZ() - entityBox.getMaxZ() - 0.001f;
                } else {
                    return blockBox.getMaxZ() - entityBox.getMinZ() + 0.001f;
                }
        }
        return 0;
    }

    /**
     * 公共方法：检查指定位置的方块是否为固体（用于火焰粒子等）
     */
    public boolean isBlockSolidAt(int x, int y, int z) {
        return isBlockSolid(x, y, z);
    }

    /**
     * 检查指定位置的方块是否为固体
     * 使用你现有的ChunkManager系统
     */
    private boolean isBlockSolid(int x, int y, int z) {
        String blockId = getBlockIdAt(x, y, z);
        return blockId != null && !blockId.equals("air");
    }

    /**
     * 获取指定整数格子坐标的方块ID；查不到（区块未加载/越界）时返回null（视为空气）
     */
    private String getBlockIdAt(int x, int y, int z) {
        if (chunkManager == null) {
            // 没有ChunkManager时的简单测试逻辑：地面以下都是固体
            return y <= 0 ? "stone" : null;
        }

        try {
            // 计算区块坐标（使用正确的地板除法）
            int chunkX = (x >= 0) ? (x / Chunk.SIZE) : ((x + 1) / Chunk.SIZE - 1);
            int chunkY = (y >= 0) ? (y / Chunk.SIZE) : ((y + 1) / Chunk.SIZE - 1);
            int chunkZ = (z >= 0) ? (z / Chunk.SIZE) : ((z + 1) / Chunk.SIZE - 1);

            ChunkPosition chunkPos = new ChunkPosition(chunkX, chunkY, chunkZ);
            Chunk chunk = chunkManager.getChunk(chunkPos);

            if (chunk == null) {
                return null; // 未加载的区块视为空气
            }

            // 计算区块内坐标（确保在0-15范围内）
            int localX = x - (chunkX * Chunk.SIZE);
            int localY = y - (chunkY * Chunk.SIZE);
            int localZ = z - (chunkZ * Chunk.SIZE);

            // 边界检查（防止数组越界）
            if (localX < 0 || localX >= Chunk.SIZE ||
                localY < 0 || localY >= Chunk.SIZE ||
                localZ < 0 || localZ >= Chunk.SIZE) {
                return null;
            }

            return chunk.getBlockId(localX, localY, localZ);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 构造指定整数格子坐标处方块的碰撞盒。方块渲染约定"整数坐标=格子中心本身"（如坐标5的
     * 方块用Box(0.5,0.5,0.5)直接摆在Vector3f(5,y,z)上，占据[4.5,5.5]），不是把整数坐标当
     * "角点"——之前的写法new AABB(x,y,z,x+1,y+1,z+1)正是把整数坐标当成了角点，构造出的
     * 盒子跨度是[x,x+1]，跟真实渲染位置整整错位了半格。这里以(x,y,z)本身为中心构造，
     * 尺寸从BlockShapeRegistry查询该方块的实际登记尺寸（默认满格1x1x1；wood1这类细柱子会
     * 小得多），而不是一律满格。
     */
    private AABB buildBlockAABB(int x, int y, int z, String blockId) {
        Vector3f half = shapeRegistry != null
                ? shapeRegistry.getHalfExtents(blockId)
                : new Vector3f(0.5f, 0.5f, 0.5f);
        return new AABB(
                x - half.x, y - half.y, z - half.z,
                x + half.x, y + half.y, z + half.z
        );
    }

    /**
     * 检查是否在地面上（用于跳跃检测）
     */
    public boolean isOnGround(AABB entityBox) {
        // 检查实体下方是否有固体方块
        AABB groundCheckBox = entityBox.offset(0, -0.1f, 0);

        // 同checkAxisCollision：方块坐标n占据[n-0.5,n+0.5]，查询范围要各往外扩0.5再取整
        int minX = (int) Math.floor(groundCheckBox.getMinX() - 0.5f);
        int maxX = (int) Math.floor(groundCheckBox.getMaxX() + 0.5f);
        int minY = (int) Math.floor(groundCheckBox.getMinY() - 0.5f);
        int maxY = (int) Math.floor(groundCheckBox.getMaxY() + 0.5f);
        int minZ = (int) Math.floor(groundCheckBox.getMinZ() - 0.5f);
        int maxZ = (int) Math.floor(groundCheckBox.getMaxZ() + 0.5f);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    String blockId = getBlockIdAt(x, y, z);
                    if (blockId != null && !blockId.equals("air")) {
                        AABB blockBox = buildBlockAABB(x, y, z, blockId);
                        if (groundCheckBox.intersects(blockBox)) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    /**
     * 获取地形表面在指定世界坐标的高度
     * @param worldX 世界X坐标
     * @param worldZ 世界Z坐标
     * @return 地形表面高度，如果没有地形数据则返回Float.NaN
     */
    // 调试计数器
    private int heightQueryCounter = 0;
    private int chunkMissCounter = 0;
    private int boundaryQueryCounter = 0;

    public float getTerrainHeightAt(float worldX, float worldZ) {
        heightQueryCounter++;

        if (chunkManager == null) {

        }

        try {
            // 计算chunk坐标（只处理Y=0的地表chunk）
            int chunkX = (int) Math.floor(worldX / Chunk.SIZE);
            int chunkZ = (int) Math.floor(worldZ / Chunk.SIZE);
            ChunkPosition chunkPos = new ChunkPosition(chunkX, 0, chunkZ);

            // 计算chunk内坐标
            float localX = worldX - (chunkX * Chunk.SIZE);
            float localZ = worldZ - (chunkZ * Chunk.SIZE);

            // 检测是否在chunk边界附近（距离边界0.5格以内）
            boolean nearBoundary = (localX < 0.5f || localX > 15.5f || localZ < 0.5f || localZ > 15.5f);
            if (nearBoundary) {
                boundaryQueryCounter++;
                // 移除边界查询日志 - 太多且无用
            }

            Chunk chunk = chunkManager.getChunk(chunkPos);
            if (chunk == null || !chunk.hasTerrainData()) {
                chunkMissCounter++;



                return Float.NaN; // 无地形数据
            }

            // 【关键修复】hasTerrainData() 只检查该区块内"是否存在任意一个"有地形的格子，
            // 不代表玩家脚下具体这一格也有地形（例如竞技场：区块内部分格子是DIRT地板，
            // 部分格子在圆形范围外是NONE）。必须再检查具体格子的材质，否则区块内任意
            // 位置都会被判定为"有地面"，玩家会被悬空的NONE格子稳稳托住。
            int cellX = (int) Math.floor(localX);
            int cellZ = (int) Math.floor(localZ);
            cellX = Math.max(0, Math.min(Chunk.SIZE - 1, cellX));
            cellZ = Math.max(0, Math.min(Chunk.SIZE - 1, cellZ));
            if (chunk.getSurfaceMaterial(cellX, cellZ) == com.Hecate.world.TerrainMaterial.NONE) {
                return Float.NaN; // 该格子本身没有地形（例如竞技场圆形范围外）
            }

            // 获取heightMap（17x17顶点）
            com.Hecate.world.HeightMap heightMap = chunk.getSurfaceHeightMap();

            // 双线性插值获取精确高度
            // localX和localZ在[0, 16]范围内，heightMap索引在[0, 16]
            int x0 = (int) Math.floor(localX);
            int z0 = (int) Math.floor(localZ);
            int x1 = Math.min(x0 + 1, 16);
            int z1 = Math.min(z0 + 1, 16);

            float fx = localX - x0; // 小数部分
            float fz = localZ - z0;

            // 四个顶点的高度
            float h00 = heightMap.getHeight(x0, z0);
            float h10 = heightMap.getHeight(x1, z0);
            float h01 = heightMap.getHeight(x0, z1);
            float h11 = heightMap.getHeight(x1, z1);

            // 使用三角形精确碰撞而不是双线性插值
            // 顶点布局: 0=(x,z), 1=(x+1,z), 2=(x,z+1), 3=(x+1,z+1)
            // 对应高度: h00, h10, h01, h11
            // 三角形A (0,1,3): h00, h10, h11 - 对角线右上方
            // 三角形B (0,3,2): h00, h11, h01 - 对角线左下方

            float height;
            if (fz < fx) {
                // 在右上三角形 (0,1,3): 包含h00, h10, h11
                // 平面方程插值
                height = h00 + (h10 - h00) * fx + (h11 - h10) * fz;
            } else {
                // 在左下三角形 (0,3,2): 包含h00, h11, h01
                // 平面方程插值
                height = h00 + (h11 - h01) * fx + (h01 - h00) * fz;
            }

            return height;

        } catch (Exception e) {
            e.printStackTrace();
            return Float.NaN;
        }
    }
}
