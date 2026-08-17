package com.Hecate.player;

import com.jme3.texture.Texture;

/**
 * 2D精灵动画帧
 * 表示单个动画帧的所有信息
 */
public class AnimationFrame {

    // 基本信息
    private final String frameName;
    private final String texturePath;
    private final float duration;
    private final int frameIndex;

    // 纹理和渲染
    private Texture texture;
    private boolean textureLoaded = false;

    // 精灵特定属性
    private float scaleMultiplier = 1.0f;
    private float offsetX = 0.0f;
    private float offsetY = 0.0f;
    private boolean flipHorizontal = false;
    private boolean flipVertical = false;

    // 动画状态映射
    private AnimationState mappedState;

    // 帧特效
    private boolean hasSpecialEffect = false;
    private String effectType = "";

    /**
     * 构造函数
     */
    public AnimationFrame(String frameName, String texturePath, float duration, int frameIndex) {
        this.frameName = frameName;
        this.texturePath = texturePath;
        this.duration = duration;
        this.frameIndex = frameIndex;
    }

    /**
     * 完整构造函数
     */
    public AnimationFrame(String frameName, String texturePath, float duration, int frameIndex,
                          AnimationState mappedState) {
        this(frameName, texturePath, duration, frameIndex);
        this.mappedState = mappedState;
    }

    // ========== 核心方法 ==========

    /**
     * 判断相等性
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        AnimationFrame that = (AnimationFrame) obj;
        return frameName.equals(that.frameName) &&
                texturePath.equals(that.texturePath) &&
                frameIndex == that.frameIndex;
    }

    /**
     * 哈希码
     */
    @Override
    public int hashCode() {
        return frameName.hashCode() + texturePath.hashCode() + frameIndex;
    }

    /**
     * 创建翻转副本
     */
    public AnimationFrame createFlippedCopy() {
        AnimationFrame flipped = new AnimationFrame(
                frameName + "_flipped",
                texturePath,
                duration,
                frameIndex,
                mappedState
        );
        flipped.scaleMultiplier = this.scaleMultiplier;
        flipped.offsetX = -this.offsetX; // 翻转X偏移
        flipped.offsetY = this.offsetY;
        flipped.flipHorizontal = !this.flipHorizontal;
        flipped.flipVertical = this.flipVertical;
        flipped.texture = this.texture; // 共享纹理
        flipped.textureLoaded = this.textureLoaded;
        return flipped;
    }

    /**
     * 获取最终缩放值
     */
    public float getFinalSpriteScale() {
        // 如果 SpriteScaleManager 存在，使用它；否则使用默认值
        try {
            return SpriteScaleManager.getInstance().getSpriteScale() * scaleMultiplier;
        } catch (Exception e) {
            return 1.0f * scaleMultiplier; // 默认缩放
        }
    }

    /**
     * 应用帧效果
     */
    public void applyFrameEffects() {
        if (hasSpecialEffect) {
            // 这里可以添加特殊效果逻辑
        }
    }

    /**
     * 重置帧状态
     */
    public void resetFrame() {
        // 重置所有可变状态
        hasSpecialEffect = false;
        effectType = "";
    }

    // ========== Getter和Setter方法 ==========

    public String getFrameName() {
        return frameName;
    }

    public String getTexturePath() {
        return texturePath;
    }

    public float getDuration() {
        return duration;
    }

    public int getFrameIndex() {
        return frameIndex;
    }

    public Texture getTexture() {
        return texture;
    }

    public void setTexture(Texture texture) {
        this.texture = texture;
        this.textureLoaded = (texture != null);
    }

    public boolean isTextureLoaded() {
        return textureLoaded && texture != null;
    }

    public boolean isLoaded() {
        return isTextureLoaded();
    }

    public AnimationState getMappedAnimationState() {
        return mappedState;
    }

    public void setMappedAnimationState(AnimationState mappedState) {
        this.mappedState = mappedState;
    }

    public float getScaleMultiplier() {
        return scaleMultiplier;
    }

    public void setScaleMultiplier(float scaleMultiplier) {
        this.scaleMultiplier = Math.max(0.1f, scaleMultiplier); // 最小缩放限制
    }

    public float getOffsetX() {
        return offsetX;
    }

    public float getOffsetY() {
        return offsetY;
    }

    public void setOffset(float offsetX, float offsetY) {
        this.offsetX = offsetX;
        this.offsetY = offsetY;
    }

    public boolean isFlipHorizontal() {
        return flipHorizontal;
    }

    public boolean isFlipVertical() {
        return flipVertical;
    }

    public void setFlip(boolean flipHorizontal, boolean flipVertical) {
        this.flipHorizontal = flipHorizontal;
        this.flipVertical = flipVertical;
    }

    public boolean hasSpecialEffect() {
        return hasSpecialEffect;
    }

    public void setSpecialEffect(String effectType) {
        this.effectType = effectType;
        this.hasSpecialEffect = (effectType != null && !effectType.isEmpty());
    }

    public String getEffectType() {
        return effectType;
    }

    // ========== 信息方法 ==========

    /**
     * 获取纹理尺寸信息
     */
    public String getTextureSizeInfo() {
        if (texture != null && texture.getImage() != null) {
            int width = texture.getImage().getWidth();
            int height = texture.getImage().getHeight();
            return width + "x" + height;
        }
        return "未加载";
    }

    /**
     * 获取帧详细信息
     */
    public String getFrameInfo() {
        return String.format("帧 %s [%d] - 时长: %.3fs, 缩放: %.1fx, 状态: %s, 纹理: %s",
                frameName, frameIndex, duration, getFinalSpriteScale(),
                mappedState != null ? mappedState.getAnimationName() : "未设置",
                isTextureLoaded() ? "已加载(" + getTextureSizeInfo() + ")" : "未加载");
    }

    /**
     * 获取帧状态摘要
     */
    public String getFrameStatus() {
        StringBuilder status = new StringBuilder();
        status.append("帧状态: ");
        if (isTextureLoaded()) {
            status.append("已加载 ");
        } else {
            status.append("未加载 ");
        }

        if (flipHorizontal) status.append("↔翻转 ");
        if (hasSpecialEffect) status.append("效果 ");
        if (scaleMultiplier != 1.0f) status.append(String.format("%.1fx ", scaleMultiplier));

        return status.toString().trim();
    }

    /**
     * 调试信息
     */
    @Override
    public String toString() {
        return String.format("AnimationFrame{name='%s', index=%d, duration=%.2fs, state=%s, loaded=%s}",
                frameName, frameIndex, duration,
                mappedState != null ? mappedState : "null",
                isTextureLoaded());
    }
    
    }

