package com.Hecate.ink;

import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;

/**
 * 涂墨工具类
 *
 * 【设计目标】
 * - 为敌人AI、环境机关、特殊效果提供便捷的涂墨接口
 * - 复用玩家武器的涂墨逻辑（FlameParticle、Projectile）
 * - 支持自定义涂墨策略和效果
 *
 * 【核心功能】
 * 1. 静态涂墨方法（无需创建对象）
 * 2. 预设涂墨配置（快速应用常见效果）
 * 3. 涂墨效果组合（支持多种模式叠加）
 *
 * 【使用示例】
 * <pre>
 * // 敌人发射火焰涂墨
 * InkHelper.inkFlamePattern(enemyPos, enemyDirection, enemyFactionId, gridManager);
 *
 * // 陷阱触发区域涂墨
 * InkHelper.inkArea(trapPos, 2.0f, FactionRegistry.DARK_DEFAULT, gridManager);
 *
 * // 敌人发射抛物线墨水弹
 * InkHelper.inkProjectileTrail(startPos, endPos, enemyFactionId, gridManager);
 * </pre>
 *
 * @author Hecate Team
 * @since 2026-08-20
 */
public class InkHelper {

    /**
     * 预设配置：火焰涂墨模式
     */
    public static class FlamePreset {
        public float radius = 0.5f;        // 落点半径
        public int particleCount = 5;      // 粒子数量
        public float spread = 0.3f;        // 散布范围
        public float distance = 2.0f;      // 飞行距离
    }

    /**
     * 预设配置：弹道涂墨模式
     */
    public static class ProjectilePreset {
        public float radius = 0.4f;        // 涂墨半径
        public float interval = 0.3f;      // 涂墨间隔（米）
        public float maxDistance = 6.0f;   // 最大距离
    }

    /**
     * 预设配置：区域涂墨模式
     */
    public static class AreaPreset {
        public float radius = 1.0f;        // 涂墨半径
        public boolean ignite = false;     // 是否点燃
    }

    // ===== 基础涂墨方法 =====

    /**
     * 单点涂墨
     * @param position 位置
     * @param radius 半径
     * @param factionId 阵营ID
     * @param gridManager 网格管理器
     */
    public static void inkPoint(Vector3f position, float radius, int factionId, SparseGridManager gridManager) {
        if (gridManager == null || position == null) {
            return;
        }
        gridManager.inkCircle(position, radius, factionId);
    }

    /**
     * 区域涂墨（圆形）
     * @param position 中心位置
     * @param radius 半径
     * @param factionId 阵营ID
     * @param gridManager 网格管理器
     */
    public static void inkArea(Vector3f position, float radius, int factionId, SparseGridManager gridManager) {
        inkPoint(position, radius, factionId, gridManager);
    }

    /**
     * 区域涂墨 + 点燃
     * @param position 中心位置
     * @param radius 半径
     * @param factionId 阵营ID
     * @param gridManager 网格管理器
     * @return 成功点燃的格子数量
     */
    public static int inkAndIgniteArea(Vector3f position, float radius, int factionId, SparseGridManager gridManager) {
        if (gridManager == null || position == null) {
            return 0;
        }
        gridManager.inkCircle(position, radius, factionId);
        return gridManager.igniteCircle(position, radius);
    }

    // ===== 火焰模式涂墨 =====

    /**
     * 火焰散射涂墨（模拟 Gun1 的 FlameParticle 效果）
     *
     * 【使用场景】
     * - 敌人火焰喷射器
     * - 火焰陷阱
     * - 火焰环境效果
     *
     * @param origin 发射起点
     * @param direction 发射方向（归一化向量）
     * @param factionId 阵营ID
     * @param gridManager 网格管理器
     */
    public static void inkFlamePattern(Vector3f origin, Vector3f direction, int factionId, SparseGridManager gridManager) {
        inkFlamePattern(origin, direction, factionId, gridManager, new FlamePreset());
    }

    /**
     * 火焰散射涂墨（自定义配置）
     * @param origin 发射起点
     * @param direction 发射方向
     * @param factionId 阵营ID
     * @param gridManager 网格管理器
     * @param preset 预设配置
     */
    public static void inkFlamePattern(Vector3f origin, Vector3f direction, int factionId,
                                       SparseGridManager gridManager, FlamePreset preset) {
        if (gridManager == null || origin == null || direction == null) {
            return;
        }

        // 模拟 Gun1 的粒子散射逻辑
        // 在发射方向前方生成多个落点
        Vector3f normalizedDir = direction.normalize();

        for (int i = 0; i < preset.particleCount; i++) {
            // 计算落点位置（带随机偏移）
            float forwardDist = preset.distance + (float)(Math.random() * 0.5f - 0.25f);
            float sideDist = (float)(Math.random() * preset.spread * 2 - preset.spread);
            float vertDist = (float)(Math.random() * preset.spread * 2 - preset.spread);

            Vector3f landPos = origin.clone()
                .add(normalizedDir.mult(forwardDist))
                .add(sideDist, 0, vertDist);

            // 涂墨
            gridManager.inkCircle(landPos, preset.radius, factionId);
        }
    }

    // ===== 弹道模式涂墨 =====

    /**
     * 弹道轨迹涂墨（模拟 Gun2 的 Projectile 效果）
     *
     * 【使用场景】
     * - 敌人远程武器
     * - 飞行道具
     * - 弹道导弹
     *
     * @param startPos 起点
     * @param endPos 终点
     * @param factionId 阵营ID
     * @param gridManager 网格管理器
     */
    public static void inkProjectileTrail(Vector3f startPos, Vector3f endPos, int factionId, SparseGridManager gridManager) {
        inkProjectileTrail(startPos, endPos, factionId, gridManager, new ProjectilePreset());
    }

    /**
     * 弹道轨迹涂墨（自定义配置）
     * @param startPos 起点
     * @param endPos 终点
     * @param factionId 阵营ID
     * @param gridManager 网格管理器
     * @param preset 预设配置
     */
    public static void inkProjectileTrail(Vector3f startPos, Vector3f endPos, int factionId,
                                          SparseGridManager gridManager, ProjectilePreset preset) {
        if (gridManager == null || startPos == null || endPos == null) {
            return;
        }

        // 计算轨迹方向和距离
        Vector3f direction = endPos.subtract(startPos);
        float totalDistance = direction.length();
        direction.normalizeLocal();

        // 限制最大距离
        float actualDistance = Math.min(totalDistance, preset.maxDistance);

        // 沿轨迹涂墨
        float traveled = 0f;
        while (traveled < actualDistance) {
            Vector3f inkPos = startPos.add(direction.mult(traveled));
            gridManager.inkCircle(inkPos, preset.radius, factionId);

            traveled += preset.interval;
        }

        // 终点涂墨
        if (totalDistance <= preset.maxDistance) {
            gridManager.inkCircle(endPos, preset.radius, factionId);
        }
    }

    // ===== 特殊效果涂墨 =====

    /**
     * 环形涂墨（爆炸效果）
     *
     * 【使用场景】
     * - 炸弹爆炸
     * - 技能AOE
     * - 冲击波效果
     *
     * @param center 中心位置
     * @param innerRadius 内圈半径（不涂墨）
     * @param outerRadius 外圈半径
     * @param factionId 阵营ID
     * @param gridManager 网格管理器
     */
    public static void inkRing(Vector3f center, float innerRadius, float outerRadius,
                               int factionId, SparseGridManager gridManager) {
        if (gridManager == null || center == null) {
            return;
        }

        // 计算需要涂墨的网格范围
        float gridSize = SparseGridManager.GRID_SIZE;
        int gridRadius = (int)Math.ceil(outerRadius / gridSize);

        Vector2f centerGrid = SparseGridManager.worldToGrid(center);
        int centerX = (int)centerGrid.x;
        int centerZ = (int)centerGrid.y;

        for (int dx = -gridRadius; dx <= gridRadius; dx++) {
            for (int dz = -gridRadius; dz <= gridRadius; dz++) {
                float dist = (float)Math.sqrt(dx * dx + dz * dz) * gridSize;

                // 只涂墨在环形范围内的格子
                if (dist >= innerRadius && dist <= outerRadius) {
                    Vector3f inkPos = SparseGridManager.gridToWorld(centerX + dx, centerZ + dz);
                    gridManager.inkCircle(inkPos, gridSize * 0.8f, factionId);
                }
            }
        }
    }

    /**
     * 线性涂墨（墙壁、路径）
     *
     * 【使用场景】
     * - 涂墨墙壁
     * - 涂墨轨道
     * - 激光扫射
     *
     * @param startPos 起点
     * @param endPos 终点
     * @param width 宽度
     * @param factionId 阵营ID
     * @param gridManager 网格管理器
     */
    public static void inkLine(Vector3f startPos, Vector3f endPos, float width,
                               int factionId, SparseGridManager gridManager) {
        if (gridManager == null || startPos == null || endPos == null) {
            return;
        }

        Vector3f direction = endPos.subtract(startPos);
        float length = direction.length();
        direction.normalizeLocal();

        // 沿线段涂墨
        float step = width * 0.5f; // 间隔为宽度的一半，确保连续
        float traveled = 0f;

        while (traveled <= length) {
            Vector3f inkPos = startPos.add(direction.mult(traveled));
            gridManager.inkCircle(inkPos, width, factionId);
            traveled += step;
        }
    }

    // ===== 工具方法 =====

    /**
     * 检查位置是否有墨水
     * @param position 位置
     * @param gridManager 网格管理器
     * @return true = 有墨水，false = 空地
     */
    public static boolean hasInkAt(Vector3f position, SparseGridManager gridManager) {
        if (gridManager == null || position == null) {
            return false;
        }
        GridCell cell = gridManager.getCellAt(position);
        return cell != null && !cell.isEmpty();
    }

    /**
     * 获取位置的墨水阵营
     * @param position 位置
     * @param gridManager 网格管理器
     * @return 阵营ID，如果无墨水则返回 FactionRegistry.NONE
     */
    public static int getInkFactionAt(Vector3f position, SparseGridManager gridManager) {
        if (gridManager == null || position == null) {
            return FactionRegistry.NONE;
        }
        GridCell cell = gridManager.getCellAt(position);
        return (cell != null) ? cell.getFactionId() : FactionRegistry.NONE;
    }

    /**
     * 检查位置是否为敌方墨水
     * @param position 位置
     * @param myFactionId 我方阵营ID
     * @param gridManager 网格管理器
     * @return true = 敌方墨水，false = 非敌方（空地、己方、盟友）
     */
    public static boolean isEnemyInkAt(Vector3f position, int myFactionId, SparseGridManager gridManager) {
        int inkFactionId = getInkFactionAt(position, gridManager);
        if (inkFactionId == FactionRegistry.NONE) {
            return false;
        }

        FactionRegistry registry = gridManager.getFactionRegistry();
        Relation relation = registry.getRelation(inkFactionId, myFactionId);
        return relation == Relation.ENEMY;
    }

    /**
     * 清除区域墨水（炸弹、净化效果）
     *
     * 【实现说明】
     * 当前系统不支持直接清除墨水，需要用己方墨水覆盖敌方墨水
     * 此方法用中性阵营覆盖目标区域
     *
     * @param position 中心位置
     * @param radius 半径
     * @param gridManager 网格管理器
     */
    public static void clearInkArea(Vector3f position, float radius, SparseGridManager gridManager) {
        // 未来可扩展为真正的清除逻辑
        // 当前实现：用 NONE 阵营覆盖（需要系统支持）
        // inkArea(position, radius, FactionRegistry.NONE, gridManager);

        // 临时方案：暂不支持清除，留作扩展
        System.out.println("[InkHelper] clearInkArea: 当前系统不支持清除墨水，请用覆盖涂墨代替");
    }
}
