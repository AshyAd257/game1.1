package com.Hecate.player;

/**
 * 精灵缩放管理器 (与现有系统兼容版本)
 * 专门管理2D精灵的缩放，不影响3D模型系统
 *
 * <p><b>依赖注入支持</b>：推荐作为 PlayerController 的实例字段使用。
 *
 * <h3>推荐用法（实例字段）</h3>
 * <pre>{@code
 * // 在 PlayerController 中创建实例字段
 * private SpriteScaleManager scaleManager = new SpriteScaleManager();
 * scaleManager.setSpriteScale(6.0f);
 * }</pre>
 *
 * <h3>向后兼容用法（已废弃）</h3>
 * <pre>{@code
 * // 旧代码仍可正常工作
 * SpriteScaleManager.getInstance().getSpriteScale();
 * }</pre>
 */
public class SpriteScaleManager {

    // 缩放配置
    private static final float DEFAULT_SPRITE_SCALE = 6.0f;
    private static final float MIN_SCALE = 1.0f;
    private static final float MAX_SCALE = 16.0f;
    private static final boolean PIXEL_PERFECT = true;

    // 当前设置
    private float spriteScale = DEFAULT_SPRITE_SCALE;
    private boolean enablePixelPerfect = PIXEL_PERFECT;
    private boolean spriteMode = false;

    // 实例管理
    private static SpriteScaleManager defaultInstance;

    /**
     * 构造函数 - 创建新的精灵缩放管理器实例
     * <p>推荐作为 PlayerController 的实例字段
     */
    public SpriteScaleManager() {
        // 公开构造函数，支持实例化
    }

    /**
     * 获取默认实例（向后兼容）
     *
     * @return 全局共享的精灵缩放管理器实例
     * @deprecated 推荐作为实例字段使用：{@code new SpriteScaleManager()}
     */
    @Deprecated
    public static SpriteScaleManager getInstance() {
        if (defaultInstance == null) {
            defaultInstance = new SpriteScaleManager();
        }
        return defaultInstance;
    }

    /**
     * 获取默认实例
     *
     * @return 默认精灵缩放管理器实例
     */
    public static SpriteScaleManager getDefaultInstance() {
        return getInstance();
    }

    /**
     * 创建新的独立实例
     *
     * @return 新的精灵缩放管理器实例
     */
    public static SpriteScaleManager createInstance() {
        return new SpriteScaleManager();
    }

    /**
     * 设置2D精灵缩放（不影响3D模型）
     */
    public void setSpriteScale(float scale) {
        float oldScale = this.spriteScale;
        this.spriteScale = clampScale(scale);

        if (oldScale != this.spriteScale) {
            notifySpriteScaleChanged();
        }
    }

    /**
     * 切换精灵模式（2D精灵 vs 3D模型）
     */
    public void setSpriteMode(boolean enable) {
        if (this.spriteMode != enable) {
            this.spriteMode = enable;
            notifySpriteModeChanged();
        }
    }

    /**
     * 获取精灵缩放值
     */
    public float getSpriteScale() {
        return spriteScale;
    }

    /**
     * 检查是否为精灵模式
     */
    public boolean isSpriteMode() {
        return spriteMode;
    }

    /**
     * 限制缩放值
     */
    private float clampScale(float scale) {
        return Math.max(MIN_SCALE, Math.min(MAX_SCALE, scale));
    }

    /**
     * 通知精灵缩放变化
     */
    private void notifySpriteScaleChanged() {
        // TODO: 通知精灵渲染器更新缩放
    }

    /**
     * 通知精灵模式变化
     */
    private void notifySpriteModeChanged() {
        // TODO: 通知PlayerController切换显示模式
    }

    // 便捷方法
    public void increaseSpriteScale() {
        setSpriteScale(spriteScale + 1.0f);
    }

    public void decreaseSpriteScale() {
        setSpriteScale(spriteScale - 1.0f);
    }

    public void resetSpriteScale() {
        setSpriteScale(DEFAULT_SPRITE_SCALE);
    }

    public void toggleSpriteMode() {
        setSpriteMode(!spriteMode);
    }

    /**
     * 获取精灵系统信息
     */
    public String getSpriteInfo() {
        return String.format("精灵模式: %s, 缩放: %.1fx",
                spriteMode ? "启用" : "禁用", spriteScale);
    }
}
