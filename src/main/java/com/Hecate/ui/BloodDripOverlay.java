package com.Hecate.ui;

import com.jme3.app.SimpleApplication;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Texture;

/**
 * 血滴覆盖层效果
 * 根据玩家血量动态调整血滴覆盖层的显示
 */
public class BloodDripOverlay {
    private final SimpleApplication app;
    private Geometry bloodQuad;
    private Material bloodMaterial;
    private boolean isInitialized = false;

    // 滑出动画相关
    private boolean isSlideOutAnimating = false;
    private float slideOutProgress = 0f;
    private final float SLIDE_OUT_DURATION = 1.0f;
    private float currentY = 0f; // 当前Y位置

    public BloodDripOverlay(SimpleApplication app) {
        this.app = app;
        createBloodEffect();
    }

    private void createBloodEffect() {
        try {
            // 获取屏幕尺寸
            int screenWidth = app.getCamera().getWidth();
            int screenHeight = app.getCamera().getHeight();

            // 创建覆盖整个屏幕的四边形
            Quad quad = new Quad(screenWidth, screenHeight);
            bloodQuad = new Geometry("BloodOverlay", quad);

            // 创建材质并加载血液贴图
            bloodMaterial = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");

            // 加载血液滴落贴图
            Texture bloodTexture = app.getAssetManager().loadTexture("Interface/blood_drip.png");
            bloodTexture.setWrap(Texture.WrapMode.EdgeClamp);

            bloodMaterial.setTexture("ColorMap", bloodTexture);

            // 设置80%透明度(alpha = 0.8)
            ColorRGBA colorWithAlpha = new ColorRGBA(1f, 1f, 1f, 0.8f);
            bloodMaterial.setColor("Color", colorWithAlpha);

            // 设置混合模式和透明属性
            bloodMaterial.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
            bloodMaterial.setTransparent(true);

            bloodQuad.setMaterial(bloodMaterial);

            // 初始位置：完全在屏幕上方（满血时不可见）
            bloodQuad.setLocalTranslation(0, screenHeight, 999); // Z=999确保在前面

            // 添加到GUI节点
            app.getGuiNode().attachChild(bloodQuad);
            isInitialized = true;

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 更新血量显示
     * @param healthPercentage 血量百分比 (0.0 - 1.0)
     */
    public void updateHealth(float healthPercentage) {
        if (!isInitialized || bloodQuad == null) {
            return;
        }

        // 限制范围 0-1
        healthPercentage = Math.max(0.0f, Math.min(1.0f, healthPercentage));

        int screenHeight = app.getCamera().getHeight();

        // 计算Y位置
        // 满血(1.0): Y = screenHeight (完全在屏幕外，看不见)
        // 空血(0.0): Y = 0 (完全覆盖屏幕)
        // 血量越低，贴图越往下移
        final float yPosition = screenHeight * healthPercentage;
        final float finalHealthPercentage = healthPercentage;

        // 使用enqueue确保线程安全
        app.enqueue(() -> {
            bloodQuad.setLocalTranslation(0, yPosition, 999);
            return null;
        });
    }

    /**
     * 动态调整透明度的方法（可选）
     */
    public void setOpacity(float opacity) {
        if (!isInitialized || bloodMaterial == null) {
            return;
        }

        // 限制透明度范围 0-1
        final float finalOpacity = Math.max(0.0f, Math.min(1.0f, opacity));

        app.enqueue(() -> {
            ColorRGBA color = bloodMaterial.getParamValue("Color");
            if (color == null) {
                color = new ColorRGBA(1f, 1f, 1f, finalOpacity);
            } else {
                color = new ColorRGBA(color.r, color.g, color.b, finalOpacity);
            }
            bloodMaterial.setColor("Color", color);
            return null;
        });
    }

    /**
     * 强制显示方法（用于调试）
     */
    public void forceShow() {
        if (!isInitialized) return;

        app.enqueue(() -> {
            if (bloodQuad != null) {
                // 直接显示在屏幕上
                bloodQuad.setLocalTranslation(0, 0, 999);
            }
            return null;
        });
    }

    /**
     * 清理资源
     */
    public void cleanup() {
        if (!isInitialized) return;

        app.enqueue(() -> {
            if (bloodQuad != null && bloodQuad.getParent() != null) {
                bloodQuad.removeFromParent();
            }
            isInitialized = false;
            return null;
        });
    }

    public boolean isInitialized() {
        return isInitialized;
    }

    /**
     * 开始滑出动画
     */
    public void startSlideOutAnimation() {
        if (!isInitialized || bloodQuad == null) {
            return;
        }

        app.enqueue(() -> {
            // 记录当前位置作为起始点
            currentY = bloodQuad.getLocalTranslation().y;
            isSlideOutAnimating = true;
            slideOutProgress = 0f;
            return null;
        });
    }

    /**
     * 更新动画（需要在游戏循环中调用）
     */
    public void update(float tpf) {
        if (!isSlideOutAnimating || !isInitialized || bloodQuad == null) {
            return;
        }

        slideOutProgress += tpf / SLIDE_OUT_DURATION;

        if (slideOutProgress >= 1.0f) {
            // 动画完成
            slideOutProgress = 1.0f;
            isSlideOutAnimating = false;

            app.enqueue(() -> {
                int screenHeight = app.getCamera().getHeight();
                bloodQuad.setLocalTranslation(0, screenHeight, 999);
                return null;
            });
            return;
        }

        // 计算当前位置（从currentY滑到屏幕上方）
        int screenHeight = app.getCamera().getHeight();
        float targetY = screenHeight + 100; // 多滑出一点确保完全消失
        float newY = currentY + (targetY - currentY) * slideOutProgress;

        // 计算淡出透明度
        float alpha = 0.8f * (1.0f - slideOutProgress);

        app.enqueue(() -> {
            bloodQuad.setLocalTranslation(0, newY, 999);

            // 更新透明度
            ColorRGBA currentColor = bloodMaterial.getParamValue("Color");
            if (currentColor != null) {
                bloodMaterial.setColor("Color", new ColorRGBA(
                        currentColor.r, currentColor.g, currentColor.b, alpha));
            }
            return null;
        });
    }
}
