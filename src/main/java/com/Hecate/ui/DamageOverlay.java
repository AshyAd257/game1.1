package com.Hecate.ui;

import com.jme3.app.SimpleApplication;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.ui.Picture;

/**
 * 血量受伤覆盖层效果
 * 根据玩家血量动态调整红色覆盖层的透明度
 */
public class DamageOverlay {
    private final SimpleApplication app;
    private Picture damageOverlay;
    private boolean isInitialized = false;

    // 效果参数
    private static final float MAX_ALPHA = 0.6f; // 最大透明度60%
    private static final float MIN_ALPHA = 0.0f; // 最小透明度0%（完全透明）

    public DamageOverlay(SimpleApplication app) {
        this.app = app;
        initialize();
    }

    private void initialize() {
        try {
            // 创建Picture对象
            damageOverlay = new Picture("DamageOverlay");

            // 加载血量覆盖贴图
            Texture texture = app.getAssetManager().loadTexture("Interface/blood_drip.png");

            // 确保是Texture2D类型
            if (texture instanceof Texture2D) {
                damageOverlay.setTexture(app.getAssetManager(), (Texture2D) texture, true);
            } else {
                // 如果不是Texture2D，创建一个新的Texture2D
                Texture2D texture2D = new Texture2D();
                texture2D.setImage(texture.getImage());
                damageOverlay.setTexture(app.getAssetManager(), texture2D, true);
            }

            // 设置贴图大小为屏幕大小
            int screenWidth = app.getCamera().getWidth();
            int screenHeight = app.getCamera().getHeight();
            damageOverlay.setWidth(screenWidth);
            damageOverlay.setHeight(screenHeight);

            // 位置固定在屏幕左下角
            damageOverlay.setPosition(0, 0);

            // 获取材质并设置混合模式
            Material mat = damageOverlay.getMaterial();
            mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);

            // 初始时完全透明（满血状态）
            mat.setColor("Color", new ColorRGBA(1, 1, 1, MIN_ALPHA));

            // 添加到GUI节点
            app.getGuiNode().attachChild(damageOverlay);

            isInitialized = true;

            // 测试：显示一下效果
            updateHealth(0.3f); // 30%血量测试

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 更新血量显示
     * @param healthPercentage 血量百分比 (0.0 - 1.0)
     */
    public void updateHealth(float healthPercentage) {
        if (!isInitialized || damageOverlay == null) {
            return;
        }

        // 确保血量百分比在有效范围内
        healthPercentage = Math.max(0.0f, Math.min(1.0f, healthPercentage));

        // 计算透明度：血量越低，透明度越高（红色越明显）
        float damageLevel = 1.0f - healthPercentage;
        float alpha = MIN_ALPHA + (damageLevel * (MAX_ALPHA - MIN_ALPHA));

        // 更新材质透明度
        Material mat = damageOverlay.getMaterial();
        mat.setColor("Color", new ColorRGBA(1, 1, 1, alpha));
    }

    /**
     * 显示受伤闪烁效果
     * @param duration 闪烁持续时间（毫秒）
     */
    public void showDamageFlash(long duration) {
        if (!isInitialized || damageOverlay == null) {
            return;
        }

        // 创建一个新线程来处理闪烁效果
        new Thread(() -> {
            try {
                // 快速显示红色
                Material mat = damageOverlay.getMaterial();
                mat.setColor("Color", new ColorRGBA(1, 1, 1, 0.8f));

                // 等待一段时间
                Thread.sleep(duration);

                // 恢复原来的透明度
                // 这里应该根据当前血量来设置，暂时设为透明
                mat.setColor("Color", new ColorRGBA(1, 1, 1, MIN_ALPHA));

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * 清理资源
     */
    public void cleanup() {
        if (damageOverlay != null && damageOverlay.getParent() != null) {
            damageOverlay.removeFromParent();
            damageOverlay = null;
        }
        isInitialized = false;
    }

    /**
     * 显示/隐藏覆盖层
     */
    public void setVisible(boolean visible) {
        if (damageOverlay != null) {
            damageOverlay.setCullHint(visible ?
                    com.jme3.scene.Spatial.CullHint.Never :
                    com.jme3.scene.Spatial.CullHint.Always);
        }
    }

    /**
     * 检查是否已初始化
     */
    public boolean isInitialized() {
        return isInitialized;
    }

    /**
     * 测试效果
     */
    public void test() {

        // 模拟血量变化
        new Thread(() -> {
            try {
                for (float health = 1.0f; health >= 0; health -= 0.1f) {
                    updateHealth(health);
                    Thread.sleep(500);
                }

                // 恢复满血
                Thread.sleep(1000);
                updateHealth(1.0f);

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }
}
