package com.Hecate.block;

/**
 * 半砖（slab）的朝向。BOTTOM/TOP是横放（沿Y轴砍半），LEFT/RIGHT是沿X轴砍半的竖放，
 * FRONT/BACK是沿Z轴砍半的竖放。DOUBLE代表两个互补朝向叠满整格后的合并态
 * （渲染上直接复用cube.glb+同一张贴图，效果等价于一个完整方块）。
 */
public enum SlabOrientation {
    BOTTOM, TOP, LEFT, RIGHT, FRONT, BACK, DOUBLE;

    /**
     * 判断两个朝向是否互补（叠满整格）：BOTTOM+TOP、LEFT+RIGHT、FRONT+BACK。
     * DOUBLE不参与互补判断（它本身已经是叠满态，不能再叠加）。
     */
    public boolean isComplementOf(SlabOrientation other) {
        if (this == DOUBLE || other == DOUBLE) {
            return false;
        }
        switch (this) {
            case BOTTOM: return other == TOP;
            case TOP: return other == BOTTOM;
            case LEFT: return other == RIGHT;
            case RIGHT: return other == LEFT;
            case FRONT: return other == BACK;
            case BACK: return other == FRONT;
            default: return false;
        }
    }

    /**
     * 方块id后缀（小写），用于拼接 baseId + "_" + suffix 得到实际注册的方块id。
     */
    public String idSuffix() {
        return name().toLowerCase();
    }
}
