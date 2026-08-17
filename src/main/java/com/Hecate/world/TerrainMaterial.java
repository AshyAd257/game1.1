package com.Hecate.world;

/**
 * 地形材质类型
 * 只有这些材质使用高度场系统，其他方块保持传统体素
 */
public enum TerrainMaterial {

    /**
     * 泥土 - 基础地形材质
     */
    DIRT("dirt"),

    /**
     * 沙子 - 沙漠/海滩地形
     */
    SAND("sand"),

    /**
     * 水 - 湖泊/河流/海洋
     */
    WATER("water"),

    /**
     * 无/空气 - 表示该格子没有地形材质
     */
    NONE("air");

    private final String blockId;

    TerrainMaterial(String blockId) {
        this.blockId = blockId;
    }

    /**
     * 获取对应的方块ID
     */
    public String getBlockId() {
        return blockId;
    }

    /**
     * 从方块ID获取地形材质
     */
    public static TerrainMaterial fromBlockId(String blockId) {
        if (blockId == null) {
            return NONE;
        }

        switch (blockId) {
            case "dirt":
            case "dirt_with_grass":
            case "dirt_light":
            case "dirt_normal":
                return DIRT;
            case "sand":
                return SAND;
            case "water":
                return WATER;
            case "air":
                return NONE;
            default:
                return NONE; // 其他方块类型不使用高度场
        }
    }

    /**
     * 检查方块ID是否是地形材质
     */
    public static boolean isTerrainBlock(String blockId) {
        return fromBlockId(blockId) != NONE;
    }

    /**
     * 检查是否是固体地形（非水）
     */
    public boolean isSolid() {
        return this == DIRT || this == SAND;
    }

    /**
     * 检查是否是液体
     */
    public boolean isLiquid() {
        return this == WATER;
    }
}
