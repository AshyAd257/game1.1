package com.Hecate.ink;

import com.Hecate.element.ElementType;
import com.Hecate.weapon.ProjectileMode;

/**
 * 网格单元（重构版）
 *
 * 【重大变更 - 2026-09-13】
 * - 移除 factionId，改用 TileState 表示地块状态
 * - 增加 elementType 表示涂抹者的元素类型
 * - 增加 accumulatedHits 实现累积命中博弈机制
 * - 状态转换需要达到阈值，不同元素玩家的射弹会产生对抗
 *
 * 核心机制：
 * 1. 地块状态：光明/黑暗/雾/点燃/无
 * 2. 元素归属：记录是哪个元素类型的玩家占领的
 * 3. 累积博弈：不同元素需要多次命中才能覆盖
 */
public class GridCellNew {

    // 地块状态（光明/黑暗/雾/点燃/无）
    private TileState state;

    // 涂抹者的元素类型（水/草/火等）
    // 用于判断同一地块被不同元素玩家涂抹时的博弈
    private ElementType elementType;

    // 时间戳（用于状态消退）
    private float timestamp;

    // 状态强度（0.0-1.0，用于渐变效果）
    private float intensity;

    // 累积命中数（不同元素或不同状态的射弹命中时累积）
    private int accumulatedHits;

    /**
     * 构造函数
     */
    public GridCellNew() {
        this.state = TileState.NONE;
        this.elementType = null;
        this.timestamp = 0f;
        this.intensity = 0f;
        this.accumulatedHits = 0;
    }

    /**
     * 射弹命中地块
     *
     * 核心博弈逻辑：
     * - 相同元素 + 匹配状态：刷新时间
     * - 不同元素或不同状态：累积命中数
     * - 达到阈值：改变状态
     *
     * @param mode 射弹模式（光明/黑暗）
     * @param element 射击者的元素类型
     * @param currentTime 当前时间
     * @return 是否触发了状态变化
     */
    public boolean onProjectileHit(ProjectileMode mode, ElementType element, float currentTime) {
        // 情况1：相同元素且状态匹配 → 刷新时间，重置累积
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
            return true; // 状态已改变
        }

        return false; // 正在累积，状态未改变
    }

    /**
     * 应用新状态
     *
     * @param mode 射弹模式
     * @param element 元素类型
     * @param currentTime 当前时间
     */
    private void applyState(ProjectileMode mode, ElementType element, float currentTime) {
        this.state = mode.getBaseTileState();
        this.elementType = element;
        this.timestamp = currentTime;
        this.intensity = 1.0f;
    }

    /**
     * 判断当前状态是否匹配射弹模式
     *
     * @param mode 射弹模式
     * @return true 如果匹配
     */
    private boolean stateMatchesMode(ProjectileMode mode) {
        return mode.matchesTileState(state);
    }

    /**
     * 点燃地块（光明 → 点燃）
     *
     * @param currentTime 当前时间
     * @return 是否成功点燃
     */
    public boolean ignite(float currentTime) {
        if (state == TileState.LIGHT) {
            state = TileState.IGNITED;
            timestamp = currentTime;
            intensity = 1.0f;
            accumulatedHits = 0;
            return true;
        }
        return false;
    }

    /**
     * 转化为雾状态（黑暗 → 雾）
     *
     * @param currentTime 当前时间
     * @return 是否成功转化
     */
    public boolean convertToFog(float currentTime) {
        if (state == TileState.DARK) {
            state = TileState.FOG;
            timestamp = currentTime;
            accumulatedHits = 0;
            return true;
        }
        return false;
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
    }

    /**
     * 更新网格（处理状态消退）
     *
     * @param currentTime 当前时间
     * @param stateDecayTime 状态消退时间（秒）
     * @param fadeStartTime 开始淡化的时间（距离消退前多少秒）
     */
    public void update(float currentTime, float stateDecayTime, float fadeStartTime) {
        float elapsed = currentTime - timestamp;

        if (elapsed < 0) {
            timestamp = currentTime;
            elapsed = 0;
        }

        // 空地不需要更新
        if (state == TileState.NONE) {
            return;
        }

        // 处理状态消退
        if (elapsed > stateDecayTime) {
            // 超时，清除状态
            clear();
        } else {
            float fadeThreshold = stateDecayTime - fadeStartTime;

            if (elapsed < fadeThreshold) {
                // 前期：保持满强度
                intensity = 1.0f;
            } else {
                // 后期（最后 fadeStartTime 秒）：线性淡化
                float fadeProgress = (elapsed - fadeThreshold) / fadeStartTime;
                intensity = 1.0f - fadeProgress; // 1.0 → 0.0
            }
        }
    }

    // Getters

    public TileState getState() {
        return state;
    }

    public ElementType getElementType() {
        return elementType;
    }

    public float getTimestamp() {
        return timestamp;
    }

    public float getIntensity() {
        return intensity;
    }

    public int getAccumulatedHits() {
        return accumulatedHits;
    }

    public boolean isEmpty() {
        return state == TileState.NONE;
    }

    /**
     * 判断是否与指定元素匹配
     *
     * @param element 元素类型
     * @return true 如果匹配或者是空地
     */
    public boolean isOwnedBy(ElementType element) {
        if (isEmpty()) {
            return false; // 空地不属于任何人
        }
        return this.elementType == element;
    }

    /**
     * 判断是否是敌对元素
     *
     * @param playerElement 玩家元素类型
     * @return true 如果是敌对元素占领的地块
     */
    public boolean isEnemyTerritory(ElementType playerElement) {
        if (isEmpty() || elementType == null) {
            return false;
        }
        return this.elementType != playerElement;
    }

    /**
     * 获取移动速度倍率
     *
     * @param playerElement 玩家元素类型
     * @return 速度倍率
     */
    public float getSpeedMultiplier(ElementType playerElement) {
        if (isEmpty()) {
            return 1.0f; // 中立地块，普通速度
        }

        // 判断是己方还是敌方
        boolean isAlly = (this.elementType == playerElement);

        if (isAlly) {
            return state.getAllySpeedMultiplier();
        } else {
            return state.getEnemySpeedMultiplier();
        }
    }

    /**
     * 判断是否提供隐身buff（仅己方）
     *
     * @param playerElement 玩家元素类型
     * @return true 如果提供隐身
     */
    public boolean providesStealthBuff(ElementType playerElement) {
        if (isEmpty() || elementType != playerElement) {
            return false;
        }
        return state.providesStealthBuff();
    }

    /**
     * 判断是否提供恢复buff（仅己方）
     *
     * @param playerElement 玩家元素类型
     * @return true 如果提供恢复
     */
    public boolean providesHealingBuff(ElementType playerElement) {
        if (isEmpty() || elementType != playerElement) {
            return false;
        }
        return state.providesHealingBuff();
    }

    /**
     * 获取争夺进度（0.0 - 1.0）
     * 用于UI显示
     *
     * @return 争夺进度百分比
     */
    public float getContestProgress() {
        if (accumulatedHits == 0) {
            return 0.0f;
        }

        int threshold = state.getCoverageThreshold();
        return Math.min(1.0f, (float) accumulatedHits / threshold);
    }
}
