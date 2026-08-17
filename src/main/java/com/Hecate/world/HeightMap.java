package com.Hecate.world;

/**
 * 高度场 - 存储地形表面的顶点高度
 * 用于土/沙/水的自然起伏地形
 *
 * 17×17 顶点网格用于 16×16 方块区域
 * 边界顶点与相邻区块共享
 */
public class HeightMap {

    // 顶点数量：16个方块需要17个顶点（0到16）
    public static final int VERTEX_COUNT = 17; // Chunk.SIZE + 1

    // 高度数据：[x][z]，值为相对于方块Y坐标的偏移（0.0到1.0）
    private final float[][] heights;

    public HeightMap() {
        this.heights = new float[VERTEX_COUNT][VERTEX_COUNT];
        // 默认全部初始化为5.0（地表高度为5格）
        initializeFlat(5.0f);
    }

    /**
     * 从已有数据创建（用于反序列化）
     */
    public HeightMap(float[][] heightData) {
        if (heightData.length != VERTEX_COUNT || heightData[0].length != VERTEX_COUNT) {
            throw new IllegalArgumentException("高度数据尺寸必须是 " + VERTEX_COUNT + "×" + VERTEX_COUNT);
        }
        this.heights = heightData;
    }

    /**
     * 初始化为平坦高度
     */
    public void initializeFlat(float height) {
        for (int x = 0; x < VERTEX_COUNT; x++) {
            for (int z = 0; z < VERTEX_COUNT; z++) {
                heights[x][z] = height;
            }
        }
    }

    /**
     * 获取指定顶点的高度
     * @param x 顶点X坐标 (0-16)
     * @param z 顶点Z坐标 (0-16)
     * @return 高度值
     */
    public float getHeight(int x, int z) {
        if (!isValidVertex(x, z)) {
            return 0.0f;
        }
        return heights[x][z];
    }

    /**
     * 设置指定顶点的高度
     * @param x 顶点X坐标 (0-16)
     * @param z 顶点Z坐标 (0-16)
     * @param height 高度值
     */
    public void setHeight(int x, int z, float height) {
        if (isValidVertex(x, z)) {
            heights[x][z] = height;
        }
    }

    /**
     * 检查顶点坐标是否有效
     */
    public boolean isValidVertex(int x, int z) {
        return x >= 0 && x < VERTEX_COUNT && z >= 0 && z < VERTEX_COUNT;
    }

    /**
     * 获取指定位置的插值高度（用于碰撞检测）
     * @param localX 区块内X坐标 (0.0-16.0)
     * @param localZ 区块内Z坐标 (0.0-16.0)
     * @return 插值后的高度
     */
    public float getInterpolatedHeight(float localX, float localZ) {
        // 限制在有效范围内
        localX = Math.max(0, Math.min(16 - 0.001f, localX)); // Chunk.SIZE = 16
        localZ = Math.max(0, Math.min(16 - 0.001f, localZ));

        // 找到四个角顶点
        int x0 = (int) Math.floor(localX);
        int z0 = (int) Math.floor(localZ);
        int x1 = Math.min(x0 + 1, VERTEX_COUNT - 1);
        int z1 = Math.min(z0 + 1, VERTEX_COUNT - 1);

        // 计算插值系数
        float fx = localX - x0;
        float fz = localZ - z0;

        // 双线性插值
        float h00 = heights[x0][z0];
        float h10 = heights[x1][z0];
        float h01 = heights[x0][z1];
        float h11 = heights[x1][z1];

        float h0 = h00 * (1 - fx) + h10 * fx;
        float h1 = h01 * (1 - fx) + h11 * fx;

        return h0 * (1 - fz) + h1 * fz;
    }

    /**
     * 修改顶点高度并返回高度变化量
     * @param x 顶点X坐标
     * @param z 顶点Z坐标
     * @param delta 高度变化量
     * @return 实际的高度变化量
     */
    public float modifyHeight(int x, int z, float delta) {
        if (!isValidVertex(x, z)) {
            return 0.0f;
        }

        float oldHeight = heights[x][z];
        float newHeight = oldHeight + delta;

        // 限制高度范围（-300.0到100.0，允许深度挖掘和高度建造）
        newHeight = Math.max(-300.0f, Math.min(100.0f, newHeight));

        heights[x][z] = newHeight;
        return newHeight - oldHeight; // 返回实际变化量
    }

    /**
     * 获取原始高度数据（用于序列化）
     */
    public float[][] getHeightData() {
        return heights;
    }

    /**
     * 计算单个顶点修改的体积变化
     * 一个顶点影响4个相邻格子，每个格子贡献1/4
     * @param deltaHeight 高度变化量
     * @return 体积变化（立方方块单位）
     */
    public static float calculateVolumeChange(float deltaHeight) {
        // 一个顶点影响周围4个格子，每格1/4面积
        // 体积 = 高度 × 面积 = deltaHeight × (1.0 × 4 × 0.25) = deltaHeight
        return deltaHeight;
    }

    /**
     * 复制高度图
     */
    public HeightMap copy() {
        float[][] newHeights = new float[VERTEX_COUNT][VERTEX_COUNT];
        for (int x = 0; x < VERTEX_COUNT; x++) {
            System.arraycopy(heights[x], 0, newHeights[x], 0, VERTEX_COUNT);
        }
        return new HeightMap(newHeights);
    }
}
