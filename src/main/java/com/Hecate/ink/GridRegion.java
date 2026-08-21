package com.Hecate.ink;

import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * 网格区域
 * 管理一个16x16的网格块（3.2m x 3.2m）
 * 用于分块管理，提高性能
 */
public class GridRegion {

    // 区域大小（16x16个网格）
    public static final int REGION_SIZE = 16;
    public static final float REGION_WORLD_SIZE = REGION_SIZE * SparseGridManager.GRID_SIZE; // 3.2m

    // 区域坐标（区域空间，不是世界空间）
    private final int regionX;
    private final int regionZ;

    // 网格数组（16x16）
    private final GridCell[][] cells;

    // 是否有任何墨水（快速跳过空区域）
    private boolean hasAnyInk;

    // 脏标记（需要更新）
    private boolean dirty;

    // 边界缓存（只在dirty时重新计算）
    private List<BoundaryCell> cachedBoundaries = null;

    // 最后更新时间
    private float lastUpdateTime;

    public GridRegion(int regionX, int regionZ) {
        this.regionX = regionX;
        this.regionZ = regionZ;
        this.cells = new GridCell[REGION_SIZE][REGION_SIZE];
        this.hasAnyInk = false;
        this.dirty = false;
        this.lastUpdateTime = 0f;

        // 初始化所有格子
        for (int x = 0; x < REGION_SIZE; x++) {
            for (int z = 0; z < REGION_SIZE; z++) {
                cells[x][z] = new GridCell();
            }
        }
    }

    /**
     * 获取网格单元（区域内坐标）
     * @param localX 区域内X坐标（0-15）
     * @param localZ 区域内Z坐标（0-15）
     */
    public GridCell getCell(int localX, int localZ) {
        if (localX < 0 || localX >= REGION_SIZE || localZ < 0 || localZ >= REGION_SIZE) {
            return null;
        }
        return cells[localX][localZ];
    }

    /**
     * 涂墨
     * @param localX 区域内X坐标
     * @param localZ 区域内Z坐标
     * @param team 队伍
     * @param currentTime 当前时间
     */
    // 调试计数器
    private static int inkCounter = 0;

    public void ink(int localX, int localZ, int team, float currentTime) {
        GridCell cell = getCell(localX, localZ);
        if (cell != null) {
            cell.ink(team, currentTime);
            hasAnyInk = true;
            dirty = true;
        }
    }

    /**
     * 点燃
     * @param localX 区域内X坐标
     * @param localZ 区域内Z坐标
     * @param currentTime 当前时间
     * @return 是否成功点燃
     */
    public boolean ignite(int localX, int localZ, float currentTime) {
        GridCell cell = getCell(localX, localZ);
        if (cell != null && cell.ignite(currentTime)) {
            dirty = true;
            return true;
        }
        return false;
    }

    /**
     * 更新区域（处理墨水消退等）
     * @param currentTime 当前时间
     * @param inkDecayTime 墨水消退时间
     * @param igniteDecayTime 点燃持续时间
     * @param fadeStartTime 墨水开始淡化的时间
     */
    public void update(float currentTime, float inkDecayTime, float igniteDecayTime, float fadeStartTime) {
        boolean stillHasInk = false;
        boolean anyChanged = false;

        for (int x = 0; x < REGION_SIZE; x++) {
            for (int z = 0; z < REGION_SIZE; z++) {
                GridCell cell = cells[x][z];
                if (!cell.isEmpty()) {
                    float oldIntensity = cell.getIntensity();

                    cell.update(currentTime, inkDecayTime, igniteDecayTime, fadeStartTime);

                    if (cell.isEmpty() || Math.abs(cell.getIntensity() - oldIntensity) > 0.001f) {
                        anyChanged = true;
                    }

                    if (!cell.isEmpty()) {
                        stillHasInk = true;
                    }
                }
            }
        }

        hasAnyInk = stillHasInk;
        lastUpdateTime = currentTime;

        if (anyChanged) {
            dirty = true;
        }
    }

    /**
     * 获取区域中心世界坐标
     */
    public Vector2f getWorldCenter() {
        float worldX = (regionX + 0.5f) * REGION_WORLD_SIZE;
        float worldZ = (regionZ + 0.5f) * REGION_WORLD_SIZE;
        return new Vector2f(worldX, worldZ);
    }

    /**
     * 获取区域左下角世界坐标
     */
    public Vector2f getWorldMin() {
        float worldX = regionX * REGION_WORLD_SIZE;
        float worldZ = regionZ * REGION_WORLD_SIZE;
        return new Vector2f(worldX, worldZ);
    }

    // Getters
    public int getRegionX() {
        return regionX;
    }

    public int getRegionZ() {
        return regionZ;
    }

    public boolean hasAnyInk() {
        return hasAnyInk;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void clearDirty() {
        dirty = false;
        cachedBoundaries = null; // 清除缓存，下次重新计算
    }

    public float getLastUpdateTime() {
        return lastUpdateTime;
    }

    /**
     * 获取区域内所有非空格子的数量
     */
    public int getInkCount() {
        int count = 0;
        for (int x = 0; x < REGION_SIZE; x++) {
            for (int z = 0; z < REGION_SIZE; z++) {
                if (!cells[x][z].isEmpty()) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * 边界格子信息
     * 用于光晕渲染
     */
    public static class BoundaryCell {
        public final int localX;        // 区域内坐标
        public final int localZ;
        public final int factionId;     // 阵营ID
        public final Vector3f outwardDir; // 朝外方向（归一化）
        public final float intensity;   // 墨水强度

        public BoundaryCell(int localX, int localZ, int factionId, Vector3f outwardDir, float intensity) {
            this.localX = localX;
            this.localZ = localZ;
            this.factionId = factionId;
            this.outwardDir = outwardDir;
            this.intensity = intensity;
        }
    }

    /**
     * 获取边界格子列表（缓存，只在dirty时重新计算）
     * 边界定义：至少有一个8邻域格子是非己方（空地或敌方）
     */
    public List<BoundaryCell> getBoundaries() {
        if (cachedBoundaries != null && !dirty) {
            return cachedBoundaries;
        }

        cachedBoundaries = new ArrayList<>();

        for (int x = 0; x < REGION_SIZE; x++) {
            for (int z = 0; z < REGION_SIZE; z++) {
                GridCell cell = cells[x][z];

                // 只有有墨水的格子才可能是边界
                if (cell.isEmpty()) {
                    continue;
                }

                // 检查8邻域，看是否有非己方格子
                Vector3f outwardDir = computeOutwardDirection(x, z, cell.getFactionId());

                if (outwardDir != null) {
                    // 这是一个边界格子
                    cachedBoundaries.add(new BoundaryCell(
                        x, z,
                        cell.getFactionId(),
                        outwardDir,
                        cell.getIntensity()
                    ));
                }
            }
        }

        return cachedBoundaries;
    }

    /**
     * 计算格子的朝外方向
     * 原理：检查8邻域，找到所有非己方方向，取平均向量
     * @return 朝外方向（归一化），如果不是边界格子返回null
     */
    private Vector3f computeOutwardDirection(int x, int z, int selfFactionId) {
        // 8方向偏移
        int[][] offsets = {
            {-1, -1}, {0, -1}, {1, -1},
            {-1,  0},          {1,  0},
            {-1,  1}, {0,  1}, {1,  1}
        };

        Vector3f sumOutward = new Vector3f(0, 0, 0);
        int nonSelfCount = 0;

        for (int[] offset : offsets) {
            int nx = x + offset[0];
            int nz = z + offset[1];

            // 检查邻居是否非己方（包括空地、敌方、区域外）
            boolean isNonSelf = false;

            if (nx < 0 || nx >= REGION_SIZE || nz < 0 || nz >= REGION_SIZE) {
                // 区域边界外，视为非己方
                isNonSelf = true;
            } else {
                GridCell neighbor = cells[nx][nz];
                if (neighbor.isEmpty() || neighbor.getFactionId() != selfFactionId) {
                    isNonSelf = true;
                }
            }

            if (isNonSelf) {
                // 累加朝外方向（XZ平面）
                sumOutward.x += offset[0];
                sumOutward.z += offset[1];
                nonSelfCount++;
            }
        }

        // 如果没有非己方邻居，说明不是边界格子
        if (nonSelfCount == 0) {
            return null;
        }

        // 归一化
        sumOutward.normalizeLocal();
        return sumOutward;
    }
}
