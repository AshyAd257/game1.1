package com.Hecate.texture;

/**
 * 🎨 方块面枚举 - 定义方块的六个面
 */
public enum BlockFace {
    TOP("top", "顶面"),
    BOTTOM("bottom", "底面"),
    NORTH("north", "北面"),
    SOUTH("south", "南面"),
    EAST("east", "东面"),
    WEST("west", "西面");

    private final String id;
    private final String displayName;

    BlockFace(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * 获取相对的面
     */
    public BlockFace getOpposite() {
        switch (this) {
            case TOP: return BOTTOM;
            case BOTTOM: return TOP;
            case NORTH: return SOUTH;
            case SOUTH: return NORTH;
            case EAST: return WEST;
            case WEST: return EAST;
            default: return this;
        }
    }

    /**
     * 是否为垂直面（顶部或底部）
     */
    public boolean isVertical() {
        return this == TOP || this == BOTTOM;
    }

    /**
     * 是否为水平面（侧面）
     */
    public boolean isHorizontal() {
        return !isVertical();
    }

    /**
     * 调试方法：打印所有面
     */
    public static void printAllFaces() {
        System.out.println("🔍 BlockFace 枚举值: " + java.util.Arrays.toString(values()));
    }

    @Override
    public String toString() {
        return displayName + "(" + id + ")";
    }
}
