package com.Hecate.physics;

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
        System.out.println("✅ CollisionManager已连接到ChunkManager");
    }

    /**
     * ✅ PlayerController需要的方法 - 检测移动是否会发生碰撞
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
        
        // 调试信息：显示碰撞检测范围
        int minY = (int) Math.floor(min.y);
        int maxY = (int) Math.floor(max.y);
        
        // 只在玩家接近地面时打印
        if (minY <= 3 && maxY >= -1) {
            System.out.println("🔍 碰撞检测范围: Y=" + minY + " 到 Y=" + maxY + " (地面在Y=1)");
        }

        int minX = (int) Math.floor(min.x);
        int minZ = (int) Math.floor(min.z);
        int maxX = (int) Math.floor(max.x);
        int maxZ = (int) Math.floor(max.z);

        // 检查范围内的所有方块
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (isBlockSolid(x, y, z)) {
                        // 创建方块的AABB
                        AABB blockBox = new AABB(x, y, z, x + 1, y + 1, z + 1);

                        // 检测碰撞
                        if (newBox.intersects(blockBox)) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    /**
     * ✅ PlayerController需要的方法 - 获取滑动移动向量
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
     * 📐 获取部分移动向量
     */
    private Vector3f getPartialMovement(AABB playerBox, Vector3f currentMovement, Vector3f axisMovement) {
        Vector3f direction = axisMovement.normalize();
        float maxDistance = axisMovement.length();

        // 二分查找最大可移动距离
        float minDistance = 0;
        float testDistance = maxDistance;

        for (int i = 0; i < 10; i++) { // 最多10次迭代
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

        // 创建移动后的碰撞箱
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

        // 获取可能碰撞的方块范围
        int minX = (int) Math.floor(movedBox.getMinX());
        int maxX = (int) Math.floor(movedBox.getMaxX());
        int minY = (int) Math.floor(movedBox.getMinY());
        int maxY = (int) Math.floor(movedBox.getMaxY());
        int minZ = (int) Math.floor(movedBox.getMinZ());
        int maxZ = (int) Math.floor(movedBox.getMaxZ());

        // 检测范围内的所有方块
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (isBlockSolid(x, y, z)) {
                        AABB blockBox = new AABB(x, y, z, x + 1, y + 1, z + 1);
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
     * 检查指定位置的方块是否为固体
     * 使用你现有的ChunkManager系统
     */
    private boolean isBlockSolid(int x, int y, int z) {
        if (chunkManager == null) {
            System.out.println("⚠️ ChunkManager为null！位置(" + x + "," + y + "," + z + ")");
            // 没有ChunkManager时的简单测试逻辑
            return y <= 0; // 地面以下都是固体
        }

        try {
            // 计算区块坐标
            int chunkX = Math.floorDiv(x, Chunk.SIZE);
            int chunkY = Math.floorDiv(y, Chunk.SIZE);
            int chunkZ = Math.floorDiv(z, Chunk.SIZE);

            ChunkPosition chunkPos = new ChunkPosition(chunkX, chunkY, chunkZ);
            Chunk chunk = chunkManager.getChunk(chunkPos);

            if (chunk == null) {
                return false; // 未加载的区块视为空气
            }

            // 计算区块内坐标
            int localX = ((x % Chunk.SIZE) + Chunk.SIZE) % Chunk.SIZE;
            int localY = ((y % Chunk.SIZE) + Chunk.SIZE) % Chunk.SIZE;
            int localZ = ((z % Chunk.SIZE) + Chunk.SIZE) % Chunk.SIZE;

            // 使用你的Chunk.getBlockId方法
            String blockId = chunk.getBlockId(localX, localY, localZ);
            boolean isSolid = blockId != null && !blockId.equals("air");
            
            // 调试信息：打印玩家周围的方块检测
            if (x >= -1 && x <= 1 && y >= 0 && y <= 2 && z >= -1 && z <= 1) {
                System.out.println("🔍 检测方块(" + x + "," + y + "," + z + ") -> 区块(" + chunkX + "," + chunkY + "," + chunkZ + ") -> " + blockId + " (固体:" + isSolid + ")");
            }
            
            return isSolid;

        } catch (Exception e) {
            System.err.println("检查方块固体状态时出错: " + e.getMessage());
            return false;
        }
    }

    /**
     * 检查是否在地面上（用于跳跃检测）
     */
    public boolean isOnGround(AABB entityBox) {
        // 检查实体下方是否有固体方块
        AABB groundCheckBox = entityBox.offset(0, -0.1f, 0);

        int minX = (int) Math.floor(groundCheckBox.getMinX());
        int maxX = (int) Math.floor(groundCheckBox.getMaxX());
        int minY = (int) Math.floor(groundCheckBox.getMinY());
        int maxY = (int) Math.floor(groundCheckBox.getMaxY());
        int minZ = (int) Math.floor(groundCheckBox.getMinZ());
        int maxZ = (int) Math.floor(groundCheckBox.getMaxZ());

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (isBlockSolid(x, y, z)) {
                        AABB blockBox = new AABB(x, y, z, x + 1, y + 1, z + 1);
                        if (groundCheckBox.intersects(blockBox)) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }
}
