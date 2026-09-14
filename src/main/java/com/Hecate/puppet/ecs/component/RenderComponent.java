package com.Hecate.puppet.ecs.component;

import com.Hecate.puppet.ecs.Component;
import com.Hecate.puppet.ecs.component.render.RenderStrategy;
import com.jme3.math.ColorRGBA;

/**
 * 渲染组件
 * 存储实体的渲染数据和渲染策略
 *
 * 【设计原则】
 * - 使用策略模式替代继承
 * - 每个实体只有一个RenderStrategy
 * - 渲染相关的所有参数都在这里
 *
 * 【优势】
 * - 内存高效：只存储实际使用的渲染策略
 * - 易于扩展：添加新策略无需修改核心类
 * - 易于切换：运行时可以更换渲染策略
 */
public class RenderComponent implements Component {

    // 渲染策略
    private RenderStrategy strategy;

    // 通用渲染参数
    private boolean visible;
    private float opacity;          // 不透明度（0-1）
    private ColorRGBA tintColor;    // 着色
    private int renderOrder;        // 渲染顺序（用于排序）

    // Billboard相关
    private boolean billboardEnabled;

    // 阴影相关
    private boolean castShadow;
    private boolean receiveShadow;

    private boolean enabled;

    /**
     * 构造函数
     *
     * @param strategy 渲染策略
     */
    public RenderComponent(RenderStrategy strategy) {
        this.strategy = strategy;
        this.visible = true;
        this.opacity = 1.0f;
        this.tintColor = ColorRGBA.White.clone();
        this.renderOrder = 0;
        this.billboardEnabled = false;
        this.castShadow = true;
        this.receiveShadow = true;
        this.enabled = true;
    }

    /**
     * 设置渲染策略
     * 注意：切换策略时应先清理旧策略
     *
     * @param strategy 新的渲染策略
     */
    public void setStrategy(RenderStrategy strategy) {
        if (this.strategy != null) {
            this.strategy.cleanup();
        }
        this.strategy = strategy;
    }

    /**
     * 获取渲染策略
     *
     * @return 渲染策略
     */
    public RenderStrategy getStrategy() {
        return strategy;
    }

    /**
     * 获取策略类型
     *
     * @return 策略类型
     */
    public RenderStrategy.Type getStrategyType() {
        return strategy != null ? strategy.getType() : null;
    }

    // ========== 通用渲染参数 ==========

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
        if (strategy != null) {
            strategy.setVisible(visible);
        }
    }

    public float getOpacity() {
        return opacity;
    }

    public void setOpacity(float opacity) {
        this.opacity = Math.max(0f, Math.min(1f, opacity));
    }

    public ColorRGBA getTintColor() {
        return tintColor;
    }

    public void setTintColor(ColorRGBA color) {
        this.tintColor.set(color);
    }

    public int getRenderOrder() {
        return renderOrder;
    }

    public void setRenderOrder(int order) {
        this.renderOrder = order;
    }

    public boolean isBillboardEnabled() {
        return billboardEnabled;
    }

    public void setBillboardEnabled(boolean enabled) {
        this.billboardEnabled = enabled;
    }

    public boolean isCastShadow() {
        return castShadow;
    }

    public void setCastShadow(boolean castShadow) {
        this.castShadow = castShadow;
    }

    public boolean isReceiveShadow() {
        return receiveShadow;
    }

    public void setReceiveShadow(boolean receiveShadow) {
        this.receiveShadow = receiveShadow;
    }

    // ========== Component Interface ==========

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled && strategy != null) {
            strategy.setVisible(false);
        } else if (enabled && strategy != null) {
            strategy.setVisible(visible);
        }
    }

    @Override
    public Component clone() {
        RenderComponent cloned = new RenderComponent(
            strategy != null ? strategy.clone() : null
        );
        cloned.visible = this.visible;
        cloned.opacity = this.opacity;
        cloned.tintColor = this.tintColor.clone();
        cloned.renderOrder = this.renderOrder;
        cloned.billboardEnabled = this.billboardEnabled;
        cloned.castShadow = this.castShadow;
        cloned.receiveShadow = this.receiveShadow;
        return cloned;
    }

    @Override
    public String toString() {
        return String.format("RenderComponent[strategy=%s, visible=%s, opacity=%.2f, order=%d]",
                strategy != null ? strategy.getType() : "null",
                visible, opacity, renderOrder);
    }
}
