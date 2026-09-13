package com.Hecate.weapon;

/**
 * 射弹模式枚举
 *
 * 玩家的射弹可以在光明/黑暗两种模式之间切换
 * - 光明模式：造成光明/点燃效果，视觉上明亮、温暖
 * - 黑暗模式：造成黑暗/雾效果，视觉上幽暗、神秘
 *
 * 切换限制：
 * - 需要冷却时间（约3秒）
 * - 切换时播放动画和音效
 * - 不能在射击过程中切换
 */
public enum ProjectileMode {

    /**
     * 光明模式
     * - 造成 LIGHT 或 IGNITED 地块状态
     * - 视觉：金黄色、发光粒子
     * - 适合进攻和占领区域
     */
    LIGHT("光明模式"),

    /**
     * 黑暗模式
     * - 造成 DARK 或 FOG 地块状态
     * - 视觉：紫色、暗影粒子
     * - 适合隐蔽和机动
     */
    DARK("黑暗模式");

    private final String displayName;

    ProjectileMode(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * 切换到另一个模式
     * @return 切换后的模式
     */
    public ProjectileMode toggle() {
        return this == LIGHT ? DARK : LIGHT;
    }

    /**
     * 获取对应的基础地块状态
     * @return 射弹命中后造成的地块状态
     */
    public com.Hecate.ink.TileState getBaseTileState() {
        return this == LIGHT ? com.Hecate.ink.TileState.LIGHT : com.Hecate.ink.TileState.DARK;
    }

    /**
     * 获取对应的点燃/加强状态
     * @return 特殊条件下造成的加强状态
     */
    public com.Hecate.ink.TileState getEnhancedTileState() {
        return this == LIGHT ? com.Hecate.ink.TileState.IGNITED : com.Hecate.ink.TileState.FOG;
    }

    /**
     * 获取射弹颜色（用于渲染）
     * @return RGB颜色
     */
    public com.jme3.math.ColorRGBA getProjectileColor() {
        if (this == LIGHT) {
            return new com.jme3.math.ColorRGBA(1.0f, 0.9f, 0.3f, 1.0f); // 金黄色
        } else {
            return new com.jme3.math.ColorRGBA(0.6f, 0.2f, 0.8f, 1.0f); // 紫色
        }
    }

    /**
     * 判断是否与地块状态匹配
     * @param state 地块状态
     * @return true 如果模式与状态类型一致
     */
    public boolean matchesTileState(com.Hecate.ink.TileState state) {
        if (this == LIGHT) {
            return state.isLightType();
        } else {
            return state.isDarkType();
        }
    }
}
