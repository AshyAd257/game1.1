package com.Hecate.ink;

import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;

import java.util.*;

/**
 * 稀疏网格管理器
 * 高性能地面涂墨系统
 *
 * 特性：
 * - 稀疏存储：只存储有墨水的区域
 * - 分块管理：16x16网格为一个区域
 * - 时间切片：每帧只更新部分区域
 * - 脏标记：只更新变化的区域
 *
 * 【阶段1重构】
 * - 涂墨接口从 team(int) 改为 factionId(int)
 * - 增加 FactionRegistry 引用，用于阵营查询
 */
public class SparseGridManager {

    // 网格大小（世界单位）
    public static final float GRID_SIZE = 0.2f; // 0.2m x 0.2m

    // 区域管理
    private final HashMap<Long, GridRegion> regions;

    // 脏区域队列（需要更新的区域）
    private final LinkedList<GridRegion> dirtyRegions;

    // 阵营注册表
    private final FactionRegistry factionRegistry;

    // 性能参数
    private static final int MAX_UPDATES_PER_FRAME = 10; // 每帧最多更新10个区域

    // 墨水参数
    private float inkDecayTime = 60.0f;    // 墨水60秒后消退
    private float igniteDecayTime = 10.0f; // 点燃10秒后降级为涂墨

    // 当前时间
    private float currentTime = 0f;

    // 统计信息
    private int totalRegions = 0;
    private int totalInkCells = 0;
    private int updatesThisFrame = 0;
    private int updateCounter = 0; // 帧计数器

    public SparseGridManager(FactionRegistry factionRegistry) {
        this.factionRegistry = factionRegistry;
        this.regions = new HashMap<>();
        this.dirtyRegions = new LinkedList<>();
    }

    /**
     * 获取阵营注册表
     */
    public FactionRegistry getFactionRegistry() {
        return factionRegistry;
    }

    /**
     * 更新管理器
     * @param tpf 时间增量
     */
    public void update(float tpf) {
        currentTime += tpf;
        updatesThisFrame = 0;
        updateCounter++;

        // 【修复】更新所有有墨水的区域，而不仅仅是脏区域
        // 这确保墨水衰减在所有区域中持续进行，防止出现"冻结"的死区
        int regionsWithInk = 0;
        int emptyRegions = 0;

        for (GridRegion region : regions.values()) {
            if (region.hasAnyInk()) {
                region.update(currentTime, inkDecayTime, igniteDecayTime);
                updatesThisFrame++;
                regionsWithInk++;
            } else {
                emptyRegions++;
            }
        }

        // 清空脏区域队列（因为我们已经更新了所有区域）
        dirtyRegions.clear();
    }

    /**
     * 世界坐标转网格坐标
     */
    public static Vector2f worldToGrid(Vector3f worldPos) {
        return new Vector2f(
            (float)Math.floor(worldPos.x / GRID_SIZE),
            (float)Math.floor(worldPos.z / GRID_SIZE)
        );
    }

    /**
     * 网格坐标转世界坐标（网格中心）
     */
    public static Vector3f gridToWorld(int gridX, int gridZ) {
        return new Vector3f(
            (gridX + 0.5f) * GRID_SIZE,
            0f,
            (gridZ + 0.5f) * GRID_SIZE
        );
    }

    /**
     * 网格坐标转区域坐标
     */
    private static int gridToRegion(int gridCoord) {
        return (int)Math.floor((double)gridCoord / GridRegion.REGION_SIZE);
    }

    /**
     * 网格坐标转区域内坐标
     */
    private static int gridToLocal(int gridCoord) {
        int local = gridCoord % GridRegion.REGION_SIZE;
        return local < 0 ? local + GridRegion.REGION_SIZE : local;
    }

    /**
     * 区域坐标转唯一键
     * 【修复】正确处理负数坐标，使用无符号掩码
     */
    private static long regionKey(int regionX, int regionZ) {
        // 使用掩码清除符号扩展，支持负数坐标
        long rx = ((long)regionX) & 0xFFFFFFFFL;  // 取低32位
        long rz = ((long)regionZ) & 0xFFFFFFFFL;  // 取低32位
        return (rx << 32) | rz;
    }

    /**
     * 获取或创建区域
     */
    private GridRegion getOrCreateRegion(int regionX, int regionZ) {
        long key = regionKey(regionX, regionZ);
        GridRegion region = regions.get(key);
        if (region == null) {
            region = new GridRegion(regionX, regionZ);
            regions.put(key, region);
            totalRegions++;
        }
        return region;
    }

    /**
     * 获取区域（如果存在）
     * 只读查询，不会创建新区域
     * @param regionX 区域X坐标
     * @param regionZ 区域Z坐标
     * @return GridRegion 或 null（如果不存在）
     */
    public GridRegion getRegionIfExists(int regionX, int regionZ) {
        long key = regionKey(regionX, regionZ);
        return regions.get(key);
    }

    /**
     * 获取网格单元
     */
    public GridCell getCell(int gridX, int gridZ) {
        int regionX = gridToRegion(gridX);
        int regionZ = gridToRegion(gridZ);
        long key = regionKey(regionX, regionZ);

        GridRegion region = regions.get(key);
        if (region == null) {
            return null; // 区域不存在，返回null表示空格子
        }

        int localX = gridToLocal(gridX);
        int localZ = gridToLocal(gridZ);
        return region.getCell(localX, localZ);
    }

    /**
     * 获取世界坐标处的网格单元
     */
    public GridCell getCellAt(Vector3f worldPos) {
        Vector2f gridPos = worldToGrid(worldPos);
        return getCell((int)gridPos.x, (int)gridPos.y);
    }

    /**
     * 涂墨（单个格子）
     * @param gridX 网格X坐标
     * @param gridZ 网格Z坐标
     * @param factionId 阵营ID
     */
    public void ink(int gridX, int gridZ, int factionId) {
        int regionX = gridToRegion(gridX);
        int regionZ = gridToRegion(gridZ);
        GridRegion region = getOrCreateRegion(regionX, regionZ);

        int localX = gridToLocal(gridX);
        int localZ = gridToLocal(gridZ);
        region.ink(localX, localZ, factionId, currentTime);

        // 标记为脏区域
        if (!dirtyRegions.contains(region)) {
            dirtyRegions.offer(region);
        }

        totalInkCells++;
    }

    /**
     * 涂墨（圆形区域）
     * @param worldPos 世界坐标
     * @param radius 半径（世界单位）
     * @param factionId 阵营ID
     */
    public void inkCircle(Vector3f worldPos, float radius, int factionId) {
        Vector2f centerGrid = worldToGrid(worldPos);
        int centerX = (int)centerGrid.x;
        int centerZ = (int)centerGrid.y;

        // 计算需要涂墨的网格范围
        int gridRadius = (int)Math.ceil(radius / GRID_SIZE);

        int inkedCount = 0;
        for (int dx = -gridRadius; dx <= gridRadius; dx++) {
            for (int dz = -gridRadius; dz <= gridRadius; dz++) {
                // 检查是否在圆形范围内
                float dist = (float)Math.sqrt(dx * dx + dz * dz) * GRID_SIZE;
                if (dist <= radius) {
                    ink(centerX + dx, centerZ + dz, factionId);
                    inkedCount++;
                }
            }
        }
    }

    /**
     * 点燃（单个格子）
     */
    public boolean ignite(int gridX, int gridZ) {
        int regionX = gridToRegion(gridX);
        int regionZ = gridToRegion(gridZ);
        long key = regionKey(regionX, regionZ);

        GridRegion region = regions.get(key);
        if (region == null) {
            return false; // 区域不存在
        }

        int localX = gridToLocal(gridX);
        int localZ = gridToLocal(gridZ);
        boolean success = region.ignite(localX, localZ, currentTime);

        if (success && !dirtyRegions.contains(region)) {
            dirtyRegions.offer(region);
        }

        return success;
    }

    /**
     * 点燃（圆形区域）
     */
    public int igniteCircle(Vector3f worldPos, float radius) {
        Vector2f centerGrid = worldToGrid(worldPos);
        int centerX = (int)centerGrid.x;
        int centerZ = (int)centerGrid.y;

        int gridRadius = (int)Math.ceil(radius / GRID_SIZE);
        int count = 0;

        for (int dx = -gridRadius; dx <= gridRadius; dx++) {
            for (int dz = -gridRadius; dz <= gridRadius; dz++) {
                float dist = (float)Math.sqrt(dx * dx + dz * dz) * GRID_SIZE;
                if (dist <= radius) {
                    if (ignite(centerX + dx, centerZ + dz)) {
                        count++;
                    }
                }
            }
        }

        return count;
    }

    /**
     * 获取玩家脚下的移动速度倍率
     * @param playerPos 玩家位置
     * @param playerFactionId 玩家阵营ID
     * @return 速度倍率
     */
    public float getSpeedMultiplier(Vector3f playerPos, int playerFactionId) {
        GridCell cell = getCellAt(playerPos);
        if (cell == null || cell.isEmpty()) {
            return 1.0f; // 空格子，普通速度
        }
        return cell.getSpeedMultiplier(playerFactionId, factionRegistry);
    }

    /**
     * 清空所有网格
     */
    public void clear() {
        regions.clear();
        dirtyRegions.clear();
        totalRegions = 0;
        totalInkCells = 0;
    }

    /**
     * 获取所有区域（用于渲染）
     */
    public Collection<GridRegion> getAllRegions() {
        return regions.values();
    }

    /**
     * 获取统计信息
     */
    public String getStats() {
        return "Grid Stats: " + regions.size() + " regions";
    }

    // Getters & Setters
    public float getInkDecayTime() {
        return inkDecayTime;
    }

    public void setInkDecayTime(float inkDecayTime) {
        this.inkDecayTime = inkDecayTime;
    }

    public float getIgniteDecayTime() {
        return igniteDecayTime;
    }

    public void setIgniteDecayTime(float igniteDecayTime) {
        this.igniteDecayTime = igniteDecayTime;
    }

    public float getCurrentTime() {
        return currentTime;
    }

    public int getTotalRegions() {
        return totalRegions;
    }

    public int getTotalInkCells() {
        return totalInkCells;
    }
}
