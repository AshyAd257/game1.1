package com.Hecate.ink;

import com.Hecate.element.ElementType;
import com.Hecate.weapon.ProjectileMode;

/**
 * 网格单元（重构版 - 2026-09-13）
 *
 * 【重大变更】
 * - factionId 现在代表元素类型（水/草/火/冰/雷），不再是光/暗阵营
 * - 增加 TileState 表示地块状态（光明/黑暗/雾/点燃/无）
 * - 增加累积命中系统，实现元素对抗博弈
 * - ignited 字段废弃，由 TileState.IGNITED 替代
 *
 * 核心机制：
 * - 地块有状态（光明/黑暗等）+ 元素归属（谁涂的）
 * - 不同元素玩家涂同一地块需要累积命中才能覆盖
 */
public class GridCell {

    // ========== 新系统字段 ==========

    // 地块状态（光明/黑暗/雾/点燃/无）
    private TileState state;

    // 元素类型（哪个元素的玩家占领的）
    // 注意：这里用 ElementType 枚举，不再用 int factionId
    private ElementType elementType;

    // 累积命中数（用于元素对抗博弈）
    private int accumulatedHits;

    // ========== 原有字段（保留兼容） ==========

    // 时间戳（用于状态消退）
    private float timestamp;

    // 状态强度（0.0-1.0，用于渐变效果）
    private float intensity;

    // ========== 兼容性字段（废弃但保留） ==========

    // 旧的 factionId，现在映射到 ElementType
    // 保留此字段是为了不破坏现有的 getFactionId() 调用
    @Deprecated
    private int legacyFactionId;

    // 旧的 ignited 标志，现在由 state == IGNITED 替代
    @Deprecated
    private boolean legacyIgnited;

    /**
     * 构造函数
     */
    public GridCell() {
        this.state = TileState.NONE;
        this.elementType = null;
        this.timestamp = 0f;
        this.intensity = 0f;
        this.accumulatedHits = 0;
        this.legacyFactionId = FactionRegistry.NONE;
        this.legacyIgnited = false;
    }

    // ========== 新系统接口 ==========

    /**
     * 射弹命中地块（新接口）
     *
     * @param mode 射弹模式（光明/黑暗）
     * @param element 射击者的元素类型
     * @param currentTime 当前时间
     * @return 是否触发了状态变化
     */
    public boolean onProjectileHit(ProjectileMode mode, ElementType element, float currentTime) {
        // 情况1：相同元素且状态匹配 → 刷新时间
        if (this.elementType == element && stateMatchesMode(mode)) {
            this.timestamp = currentTime;
            this.intensity = 1.0f;
            this.accumulatedHits = 0;
            return false; // 状态未改变
        }

        // 情况2：不同元素或不同状态 → 累积命中数
        accumulatedHits++;

        // 获取覆盖阈值
        int threshold = state.getCoverageThreshold();

        // 情况3：达到阈值 → 改变状态
        if (accumulatedHits >= threshold) {
            applyState(mode, element, currentTime);
            accumulatedHits = 0;
            updateLegacyFields(); // 同步到旧字段
            return true; // 状态已改变
        }

        return false; // 正在累积
    }

    /**
     * 点燃地块（光明 → 点燃）
     */
    public boolean ignite(float currentTime) {
        if (state == TileState.LIGHT) {
            state = TileState.IGNITED;
            timestamp = currentTime;
            intensity = 1.0f;
            accumulatedHits = 0;
            updateLegacyFields();
            return true;
        }
        return false;
    }

    /**
     * 转化为雾状态（黑暗 → 雾）
     */
    public boolean convertToFog(float currentTime) {
        if (state == TileState.DARK) {
            state = TileState.FOG;
            timestamp = currentTime;
            accumulatedHits = 0;
            updateLegacyFields();
            return true;
        }
        return false;
    }

    // ========== 旧系统接口（兼容层） ==========

    /**
     * 涂墨（旧接口，保留兼容）
     *
     * @deprecated 使用 onProjectileHit(ProjectileMode, ElementType, float) 代替
     */
    @Deprecated
    public void ink(int factionId, float currentTime) {
        // 映射 factionId 到新系统
        if (factionId == FactionRegistry.NONE) {
            clear();
            return;
        }

        // 简单映射：LIGHT_DEFAULT → 光明模式 + 默认元素
        // DARK_DEFAULT → 黑暗模式 + 默认元素
        ProjectileMode mode = (factionId == FactionRegistry.LIGHT_DEFAULT)
            ? ProjectileMode.LIGHT
            : ProjectileMode.DARK;
        ElementType element = ElementType.GRASS; // 默认草元素

        applyState(mode, element, currentTime);
        updateLegacyFields();
    }

    /**
     * 点燃（旧接口，保留兼容）
     *
     * @deprecated 使用 ignite(float) 代替
     */
    @Deprecated
    public boolean ignite(float currentTime, boolean unused) {
        return ignite(currentTime);
    }

    // ========== 内部方法 ==========

    /**
     * 应用新状态
     */
    private void applyState(ProjectileMode mode, ElementType element, float currentTime) {
        this.state = mode.getBaseTileState();
        this.elementType = element;
        this.timestamp = currentTime;
        this.intensity = 1.0f;
    }

    /**
     * 判断当前状态是否匹配射弹模式
     */
    private boolean stateMatchesMode(ProjectileMode mode) {
        return mode.matchesTileState(state);
    }

    /**
     * 同步新字段到旧字段（兼容性）
     */
    private void updateLegacyFields() {
        // 映射 ElementType → factionId
        if (elementType == null) {
            legacyFactionId = FactionRegistry.NONE;
        } else {
            // 简化映射：根据状态决定
            legacyFactionId = state.isLightType()
                ? FactionRegistry.LIGHT_DEFAULT
                : FactionRegistry.DARK_DEFAULT;
        }

        // 映射 TileState → ignited
        legacyIgnited = (state == TileState.IGNITED);
    }

    /**
     * 清空网格
     */
    public void clear() {
        this.state = TileState.NONE;
        this.elementType = null;
        this.timestamp = 0f;
        this.intensity = 0f;
        this.accumulatedHits = 0;
        this.legacyFactionId = FactionRegistry.NONE;
        this.legacyIgnited = false;
    }

    /**
     * 更新网格（处理状态消退）
     */
    public void update(float currentTime, float inkDecayTime, float igniteDecayTime, float fadeStartTime) {
        float elapsed = currentTime - timestamp;

        if (elapsed < 0) {
            timestamp = currentTime;
            elapsed = 0;
        }

        // 空地不需要更新
        if (state == TileState.NONE) {
            return;
        }

        // 点燃状态：持续一段时间后降级
        if (state == TileState.IGNITED) {
            if (elapsed > igniteDecayTime) {
                state = TileState.LIGHT; // 降级为光明
                timestamp = currentTime;
                intensity = 0.8f;
                updateLegacyFields();
            }
        } else if (state == TileState.FOG) {
            // 雾状态：比普通黑暗消退更快
            float fogDecayTime = inkDecayTime * 0.7f; // 雾持续时间是普通墨水的70%
            if (elapsed > fogDecayTime) {
                clear();
            } else {
                float fadeThreshold = fogDecayTime - fadeStartTime;
                if (elapsed < fadeThreshold) {
                    intensity = 1.0f;
                } else {
                    float fadeProgress = (elapsed - fadeThreshold) / fadeStartTime;
                    intensity = 1.0f - fadeProgress;
                }
            }
        } else {
            // 普通状态：正常消退
            if (elapsed > inkDecayTime) {
                clear();
            } else {
                float fadeThreshold = inkDecayTime - fadeStartTime;
                if (elapsed < fadeThreshold) {
                    intensity = 1.0f;
                } else {
                    float fadeProgress = (elapsed - fadeThreshold) / fadeStartTime;
                    intensity = 1.0f - fadeProgress;
                }
            }
        }
    }

    // ========== Getters（新系统） ==========

    public TileState getState() {
        return state;
    }

    public ElementType getElementType() {
        return elementType;
    }

    public int getAccumulatedHits() {
        return accumulatedHits;
    }

    public boolean isEmpty() {
        return state == TileState.NONE;
    }

    /**
     * 获取移动速度倍率（新接口）
     */
    public float getSpeedMultiplier(ElementType playerElement) {
        if (isEmpty()) {
            return 1.0f;
        }

        boolean isAlly = (this.elementType == playerElement);
        return isAlly ? state.getAllySpeedMultiplier() : state.getEnemySpeedMultiplier();
    }

    /**
     * 判断是否提供隐身buff
     */
    public boolean providesStealthBuff(ElementType playerElement) {
        if (isEmpty() || elementType != playerElement) {
            return false;
        }
        return state.providesStealthBuff();
    }

    /**
     * 判断是否提供恢复buff
     */
    public boolean providesHealingBuff(ElementType playerElement) {
        if (isEmpty() || elementType != playerElement) {
            return false;
        }
        return state.providesHealingBuff();
    }

    /**
     * 获取争夺进度（0.0 - 1.0）
     */
    public float getContestProgress() {
        if (accumulatedHits == 0) {
            return 0.0f;
        }
        int threshold = state.getCoverageThreshold();
        return Math.min(1.0f, (float) accumulatedHits / threshold);
    }

    // ========== Getters（旧系统兼容） ==========

    public float getTimestamp() {
        return timestamp;
    }

    public float getIntensity() {
        return intensity;
    }

    /**
     * @deprecated 使用 getElementType() 代替
     */
    @Deprecated
    public int getFactionId() {
        return legacyFactionId;
    }

    /**
     * @deprecated 使用 getState() == TileState.IGNITED 代替
     */
    @Deprecated
    public boolean isIgnited() {
        return legacyIgnited;
    }

    /**
     * 获取移动速度倍率（旧接口，保留兼容）
     *
     * @deprecated 使用 getSpeedMultiplier(ElementType) 代替
     */
    @Deprecated
    public float getSpeedMultiplier(int playerFactionId, FactionRegistry registry) {
        // 简单映射到新接口
        ElementType playerElement = (playerFactionId == FactionRegistry.LIGHT_DEFAULT)
            ? ElementType.FIRE  // 光阵营 → 火元素
            : ElementType.WATER; // 暗阵营 → 水元素
        return getSpeedMultiplier(playerElement);
    }
}
