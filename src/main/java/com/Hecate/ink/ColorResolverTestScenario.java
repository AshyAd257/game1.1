package com.Hecate.ink;

import com.jme3.math.Vector3f;

/**
 * ColorResolver 测试场景生成器
 * 用于快速创建测试场景：铺满光属性墨水的世界
 *
 * 使用场景：
 * - 玩家默认是暗属性
 * - 世界铺满光属性墨水
 * - 暗属性玩家看光属性领地应该是刺眼/bloom效果
 */
public class ColorResolverTestScenario {

    /**
     * 在指定位置周围铺满光属性墨水
     *
     * @param gridManager 网格管理器
     * @param center 中心位置
     * @param radius 半径（米）
     */
    public static void fillLightInkAround(SparseGridManager gridManager, Vector3f center, float radius) {

        // 使用稀疏铺设：每5米打一个大范围涂墨点
        float inkSpacing = 5.0f;  // 每5米一个点（降低密度）
        float inkRadius = 3.5f;   // 每个点的涂墨半径3.5米（确保覆盖）
        int steps = (int) (radius / inkSpacing);

        int paintedCells = 0;

        for (int dx = -steps; dx <= steps; dx++) {
            for (int dz = -steps; dz <= steps; dz++) {
                float worldX = center.x + dx * inkSpacing;
                float worldZ = center.z + dz * inkSpacing;

                // 检查是否在圆形范围内
                float distSq = dx * dx * inkSpacing * inkSpacing + dz * dz * inkSpacing * inkSpacing;
                if (distSq <= radius * radius) {
                    Vector3f inkPos = new Vector3f(worldX, center.y, worldZ);

                    // 打一个较大半径的涂墨圆
                    gridManager.inkCircle(inkPos, inkRadius, FactionRegistry.LIGHT_DEFAULT);
                    paintedCells++;
                }
            }
        }

        System.out.println("  提示：按F5切换视角，按F6切换战斗状态");
    }

    /**
     * 快速测试：在玩家出生点周围铺设光属性墨水
     *
     * @param gridManager 网格管理器
     * @param playerPos 玩家位置
     */
    public static void setupDefaultTestScene(SparseGridManager gridManager, Vector3f playerPos) {
        // 在玩家周围50米范围铺满光属性墨水
        fillLightInkAround(gridManager, playerPos, 50.0f);
    }
}
