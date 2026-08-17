package com.Hecate.ink;

import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;

/**
 * 颜色解析器
 * 核心职责：把客观的地块归属数据，翻译成主观的视觉体验
 *
 * 设计思路：
 * - 有一个"中性"的默认世界（非战斗状态），所有人看到的一致
 * - 战斗状态下，根据观察者血统对敌方领地施加视觉偏移：
 *   - 暗属性玩家看光领地：加 bloom（刺眼）
 *   - 光属性玩家看暗领地：darken（昏暗）
 */
public class ColorResolver {

    private final FactionRegistry registry;

    // 空地基准色（中性灰）
    private static final ColorRGBA BASE_GROUND_COLOR = new ColorRGBA(0.5f, 0.5f, 0.5f, 1.0f);

    // 可调参数：中性态亮度调整
    private static final float NEUTRAL_LIGHT_BRIGHTNESS = 1.0f; // 光领地在中性态的亮度倍率（白色已经很亮）
    private static final float NEUTRAL_DARK_BRIGHTNESS = 0.8f;  // 暗领地在中性态的亮度倍率

    // 可调参数：战斗态偏移强度上限（防止完全看不见）
    private static final float BLOOM_INTENSITY_MAX = 1.2f;   // bloom 最大增亮幅度（增大到1.2，更刺眼）
    private static final float DARKEN_INTENSITY_MAX = 0.7f;  // darken 最大降暗幅度（增大到0.7，更昏暗）

    public ColorResolver(FactionRegistry registry) {
        this.registry = registry;
    }

    /**
     * 主入口：解析网格单元的最终渲染颜色
     *
     * @param factionId 格子的客观归属
     * @param intensity 墨水强度（0.0-1.0）
     * @param observerFactionId 观察者的阵营ID
     * @param inCombat 观察者是否处于战斗状态
     * @return 最终渲染颜色
     */
    public ColorRGBA resolve(int factionId, float intensity, int observerFactionId, boolean inCombat) {
        // 空地直接返回基准色
        if (factionId == FactionRegistry.NONE) {
            return BASE_GROUND_COLOR.clone();
        }

        // 获取阵营定义
        FactionDef cellFaction = registry.get(factionId);
        FactionDef observerFaction = registry.get(observerFactionId);

        if (cellFaction == null || observerFaction == null) {
            // 容错：阵营不存在时返回基准色
            return BASE_GROUND_COLOR.clone();
        }

        // 计算中性基准色（非战斗态，所有人看到的一致）
        ColorRGBA neutral = computeNeutralColor(cellFaction, intensity);

        // 非战斗状态：直接返回中性色
        if (!inCombat) {
            return neutral;
        }

        // 战斗状态：根据血统关系施加偏移
        return computeCombatBiasedColor(
            neutral,
            cellFaction.getVisualLineage(),
            observerFaction.getVisualLineage(),
            intensity
        );
    }

    /**
     * 计算中性基准色（非战斗状态，所有人看到的版本一致）
     *
     * 设计目标：
     * - 光领地：用 baseHue，正常亮度，能看清，不刺眼
     * - 暗领地：用 baseHue，适度调高亮度/降低对比，保证"暗但看得清"，不是纯黑
     *
     * @param cellFaction 格子所属阵营定义
     * @param intensity 墨水强度（影响最终颜色的透明度/强度）
     * @return 中性基准色
     */
    private ColorRGBA computeNeutralColor(FactionDef cellFaction, float intensity) {
        ColorRGBA baseHue = cellFaction.getBaseHue();

        // 根据血统调整亮度
        float brightnessMult;
        if (cellFaction.getVisualLineage() == FactionDef.VisualLineage.LIGHT) {
            brightnessMult = NEUTRAL_LIGHT_BRIGHTNESS;
        } else {
            // 暗领地：适度提亮，保证"暗但看得清"
            brightnessMult = NEUTRAL_DARK_BRIGHTNESS;
        }

        // 应用亮度倍率和强度衰减
        ColorRGBA result = baseHue.clone();
        result.r *= brightnessMult * intensity;
        result.g *= brightnessMult * intensity;
        result.b *= brightnessMult * intensity;
        result.a = intensity; // 透明度由强度控制

        return result;
    }

    /**
     * 计算战斗态偏移色
     *
     * 规则：
     * - 看己方领地：基本不变（或后续加微弱的"自己人"暖光）
     * - 暗看光：加 bloom（刺眼）
     * - 光看暗：darken（昏暗）
     *
     * @param neutral 中性基准色
     * @param cellLineage 格子血统
     * @param observerLineage 观察者血统
     * @param intensity 墨水强度（控制偏移幅度）
     * @return 战斗态最终颜色
     */
    private ColorRGBA computeCombatBiasedColor(
            ColorRGBA neutral,
            FactionDef.VisualLineage cellLineage,
            FactionDef.VisualLineage observerLineage,
            float intensity) {

        // 看己方领地：不施加偏移，直接返回中性色
        if (cellLineage == observerLineage) {
            return neutral;
        }

        // 看敌方领地：根据观察者血统决定偏移方向
        if (cellLineage == FactionDef.VisualLineage.LIGHT) {
            // 暗属性玩家看光领地 → 加 bloom（刺眼）
            return applyBloom(neutral, intensity);
        } else {
            // 光属性玩家看暗领地 → darken（昏暗）
            return applyDarken(neutral, intensity);
        }
    }

    /**
     * 应用 Bloom 特效（模拟刺眼感）
     *
     * 实现方式：
     * - 整体提亮颜色，增加白色分量
     * - 保留原有色相的一部分（不完全变成纯白）
     * - 有上限，确保不会刺眼到完全看不见
     *
     * @param color 基础颜色
     * @param intensity 强度（影响偏移幅度）
     * @return 应用 bloom 后的颜色
     */
    private ColorRGBA applyBloom(ColorRGBA color, float intensity) {
        // TODO: 这些数值需要实机调整
        float bloomStrength = BLOOM_INTENSITY_MAX * intensity;

        ColorRGBA result = color.clone();

        // 向白色插值，但保留部分原色
        result.r = FastMath.clamp(result.r + bloomStrength, 0f, 1f);
        result.g = FastMath.clamp(result.g + bloomStrength, 0f, 1f);
        result.b = FastMath.clamp(result.b + bloomStrength, 0f, 1f);

        return result;
    }

    /**
     * 应用 Darken 特效（模拟昏暗感）
     *
     * 实现方式：
     * - 降低整体亮度
     * - 可选：降低饱和度（让颜色发灰）
     * - 有上限，确保暗部仍然能看清轮廓
     *
     * @param color 基础颜色
     * @param intensity 强度（影响偏移幅度）
     * @return 应用 darken 后的颜色
     */
    private ColorRGBA applyDarken(ColorRGBA color, float intensity) {
        // TODO: 这些数值需要实机调整
        float darkenStrength = DARKEN_INTENSITY_MAX * intensity;

        ColorRGBA result = color.clone();

        // 降低亮度，但确保不会变成纯黑
        result.r = FastMath.clamp(result.r * (1f - darkenStrength), 0.1f, 1f);
        result.g = FastMath.clamp(result.g * (1f - darkenStrength), 0.1f, 1f);
        result.b = FastMath.clamp(result.b * (1f - darkenStrength), 0.1f, 1f);

        return result;
    }
}
