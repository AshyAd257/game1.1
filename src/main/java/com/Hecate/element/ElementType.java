package com.Hecate.element;

import com.jme3.math.ColorRGBA;

/**
 * 元素类型枚举
 *
 * 用于区分玩家阵营，替代原有的颜色系统
 * 每个玩家选择一种元素属性，决定：
 * - 射弹和地块的视觉表现
 * - 地表装饰的样式（半砖形状、纹理）
 * - 粒子效果和音效
 *
 * 设计理念：
 * 通过元素而非颜色区分阵营，提供更丰富的视觉多样性
 * 不同元素的射弹在同一地块上会产生博弈和对抗
 */
public enum ElementType {

    /**
     * 水元素
     * 视觉：蓝色波纹、水花粒子、流动感
     * 地表：水波纹半砖，半透明
     * 特点：流畅、柔和
     */
    WATER(
        "水元素",
        new ColorRGBA(0.2f, 0.6f, 1.0f, 1.0f),      // 主色：天蓝
        new ColorRGBA(0.4f, 0.8f, 1.0f, 1.0f),      // 副色：浅蓝
        "Models/decals/water_half_brick.j3o",
        "Effects/water_splash.j3p"
    ),

    /**
     * 草元素
     * 视觉：绿色藤蔓、叶子粒子、自然感
     * 地表：三叶草半砖，有机纹理
     * 特点：生机、成长
     */
    GRASS(
        "草元素",
        new ColorRGBA(0.3f, 0.8f, 0.3f, 1.0f),      // 主色：翠绿
        new ColorRGBA(0.5f, 1.0f, 0.5f, 1.0f),      // 副色：浅绿
        "Models/decals/clover_half_brick.j3o",
        "Effects/grass_growth.j3p"
    ),

    /**
     * 火元素
     * 视觉：橙红火焰、灰烬粒子、热力感
     * 地表：灰烬堆半砖，炭黑纹理
     * 特点：狂野、爆发
     */
    FIRE(
        "火元素",
        new ColorRGBA(1.0f, 0.4f, 0.1f, 1.0f),      // 主色：橙红
        new ColorRGBA(1.0f, 0.7f, 0.2f, 1.0f),      // 副色：明黄
        "Models/decals/ash_half_brick.j3o",
        "Effects/fire_ember.j3p"
    ),

    /**
     * 冰元素
     * 视觉：冰蓝晶体、冰晶粒子、寒冷感
     * 地表：冰霜半砖，结晶纹理
     * 特点：凝固、精确
     */
    ICE(
        "冰元素",
        new ColorRGBA(0.6f, 0.9f, 1.0f, 1.0f),      // 主色：冰蓝
        new ColorRGBA(0.8f, 1.0f, 1.0f, 1.0f),      // 副色：白霜
        "Models/decals/ice_half_brick.j3o",
        "Effects/ice_crystal.j3p"
    ),

    /**
     * 雷元素
     * 视觉：金色电弧、电火花粒子、闪电感
     * 地表：充能半砖，电纹理
     * 特点：迅捷、爆裂
     */
    THUNDER(
        "雷元素",
        new ColorRGBA(0.9f, 0.8f, 0.2f, 1.0f),      // 主色：金黄
        new ColorRGBA(1.0f, 1.0f, 0.6f, 1.0f),      // 副色：亮黄
        "Models/decals/thunder_half_brick.j3o",
        "Effects/thunder_spark.j3p"
    );

    private final String displayName;
    private final ColorRGBA primaryColor;    // 主色（射弹、粒子）
    private final ColorRGBA secondaryColor;  // 副色（辅助效果）
    private final String decalModelPath;     // 地表装饰模型路径
    private final String particleEffectPath; // 粒子效果路径

    ElementType(String displayName, ColorRGBA primaryColor, ColorRGBA secondaryColor,
                String decalModelPath, String particleEffectPath) {
        this.displayName = displayName;
        this.primaryColor = primaryColor;
        this.secondaryColor = secondaryColor;
        this.decalModelPath = decalModelPath;
        this.particleEffectPath = particleEffectPath;
    }

    // Getters

    public String getDisplayName() {
        return displayName;
    }

    public ColorRGBA getPrimaryColor() {
        return primaryColor;
    }

    public ColorRGBA getSecondaryColor() {
        return secondaryColor;
    }

    public String getDecalModelPath() {
        return decalModelPath;
    }

    public String getParticleEffectPath() {
        return particleEffectPath;
    }

    /**
     * 获取混合后的颜色（用于光明/黑暗模式的颜色调整）
     * @param mode 射弹模式
     * @return 调整后的颜色
     */
    public ColorRGBA getColorForMode(com.Hecate.weapon.ProjectileMode mode) {
        ColorRGBA base = primaryColor.clone();

        if (mode == com.Hecate.weapon.ProjectileMode.LIGHT) {
            // 光明模式：增加亮度
            base.r = Math.min(1.0f, base.r * 1.3f);
            base.g = Math.min(1.0f, base.g * 1.3f);
            base.b = Math.min(1.0f, base.b * 1.3f);
        } else {
            // 黑暗模式：降低亮度，增加对比
            base.r *= 0.7f;
            base.g *= 0.7f;
            base.b *= 0.7f;
        }

        return base;
    }

}
