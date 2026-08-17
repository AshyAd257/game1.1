package com.Hecate.core;

import com.jme3.app.SimpleApplication;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.shadow.DirectionalLightShadowRenderer;
import com.jme3.shadow.EdgeFilteringMode;

/**
 * 光照系统 - 管理游戏世界的光照和阴影
 *
 * <p>负责配置和管理场景的光照效果，包括：
 * <ul>
 *   <li>环境光照（AmbientLight）- 提供基础亮度</li>
 *   <li>方向光照（DirectionalLight）- 模拟太阳光照</li>
 *   <li>阴影渲染（Shadow Rendering）- 实时阴影效果</li>
 *   <li>后处理效果（可选）- SSAO、Bloom、体积光等</li>
 * </ul>
 *
 * <h3>设计原则</h3>
 * <ul>
 *   <li>单一职责：仅负责光照相关功能</li>
 *   <li>可配置性：支持调整光照参数</li>
 *   <li>分离关注点：从Main.java提取出来</li>
 * </ul>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * LightingSystem lighting = new LightingSystem(app);
 * lighting.setupLighting();
 *
 * // 可选：调整光照参数
 * lighting.setAmbientIntensity(0.4f);
 * lighting.setSunIntensity(1.8f);
 * }</pre>
 *
 * @author Hecate Team
 * @see ApplicationContext
 */
public class LightingSystem {

    private final SimpleApplication app;

    // 光源
    private DirectionalLight sun;
    private AmbientLight ambientLight;

    // 阴影渲染器
    private DirectionalLightShadowRenderer shadowRenderer;

    // 光照参数配置
    private static final float DEFAULT_AMBIENT_INTENSITY = 0.3f;
    private static final ColorRGBA DEFAULT_AMBIENT_COLOR = new ColorRGBA(0.3f, 0.3f, 0.3f, 1.0f);
    private static final ColorRGBA DEFAULT_SUN_COLOR = new ColorRGBA(1.5f, 1.4f, 1.2f, 1.0f);
    private static final Vector3f DEFAULT_SUN_DIRECTION = new Vector3f(-1f, -1.5f, -1f).normalizeLocal();

    // 阴影参数配置
    private static final int SHADOWMAP_SIZE = 2048;
    private static final float SHADOW_INTENSITY = 0.9f;
    private static final float SHADOW_Z_EXTEND = 200f;
    private static final float SHADOW_Z_FADE_LENGTH = 20f;

    /**
     * 构造函数
     *
     * @param app SimpleApplication实例
     */
    public LightingSystem(SimpleApplication app) {
        this.app = app;
    }

    /**
     * 设置光照系统（主入口方法）
     * <p>配置环境光、太阳光和阴影效果
     */
    public void setupLighting() {
        setupAmbientLight();
        setupSunLight();
        setupShadows();
    }

    /**
     * 设置环境光照
     * <p>提供基础亮度，避免完全黑暗
     */
    private void setupAmbientLight() {
        ambientLight = new AmbientLight();
        ambientLight.setColor(DEFAULT_AMBIENT_COLOR);
        app.getRootNode().addLight(ambientLight);
    }

    /**
     * 设置方向光照（太阳光）
     * <p>模拟太阳光照效果，从西北上方照射
     */
    private void setupSunLight() {
        sun = new DirectionalLight();
        sun.setDirection(DEFAULT_SUN_DIRECTION);
        sun.setColor(DEFAULT_SUN_COLOR);
        app.getRootNode().addLight(sun);
    }

    /**
     * 设置阴影系统
     * <p>配置高质量实时阴影效果
     */
    private void setupShadows() {
        try {
            // 创建阴影渲染器 - 使用更高分辨率获得更清晰的阴影
            shadowRenderer = new DirectionalLightShadowRenderer(
                app.getAssetManager(),
                SHADOWMAP_SIZE,
                4  // 分割数量
            );
            shadowRenderer.setLight(sun);

            // 设置阴影强度 - 更明显的阴影（0.9 = 90%黑暗）
            shadowRenderer.setShadowIntensity(SHADOW_INTENSITY);

            // 设置边缘过滤模式 - 使用PCF4获得更清晰的阴影边缘
            shadowRenderer.setEdgeFilteringMode(EdgeFilteringMode.PCF4);

            // 设置阴影渲染距离
            shadowRenderer.setShadowZExtend(SHADOW_Z_EXTEND);
            shadowRenderer.setShadowZFadeLength(SHADOW_Z_FADE_LENGTH);

            // 将阴影渲染器添加到视口
            app.getViewPort().addProcessor(shadowRenderer);

        } catch (Exception e) {
            System.err.println("[光照系统] ✗ 阴影系统配置失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 设置后处理效果（SSAO、Bloom、体积光等）
     * <p>注意：需要jme3-effects依赖
     */
    public void setupPostProcessing() {
        // 预留接口，未来可以实现后处理效果
        // 需要添加 jme3-effects 依赖才能启用
    }

    // ==================== 参数调整方法 ====================

    /**
     * 设置环境光强度
     *
     * @param intensity 强度值（0.0 - 1.0）
     */
    public void setAmbientIntensity(float intensity) {
        if (ambientLight != null) {
            float clampedIntensity = Math.max(0.0f, Math.min(1.0f, intensity));
            ambientLight.setColor(new ColorRGBA(
                clampedIntensity,
                clampedIntensity,
                clampedIntensity,
                1.0f
            ));
        }
    }

    /**
     * 设置太阳光强度
     *
     * @param intensity 强度倍数（建议范围：0.5 - 2.0）
     */
    public void setSunIntensity(float intensity) {
        if (sun != null) {
            sun.setColor(new ColorRGBA(
                1.5f * intensity,
                1.4f * intensity,
                1.2f * intensity,
                1.0f
            ));
        }
    }

    /**
     * 设置太阳光方向
     *
     * @param direction 光照方向向量（会自动归一化）
     */
    public void setSunDirection(Vector3f direction) {
        if (sun != null) {
            sun.setDirection(direction.normalizeLocal());
        }
    }

    /**
     * 设置阴影强度
     *
     * @param intensity 强度值（0.0 - 1.0）
     */
    public void setShadowIntensity(float intensity) {
        if (shadowRenderer != null) {
            float clampedIntensity = Math.max(0.0f, Math.min(1.0f, intensity));
            shadowRenderer.setShadowIntensity(clampedIntensity);
        }
    }

    /**
     * 启用或禁用阴影
     *
     * @param enabled true启用，false禁用
     */
    public void setShadowEnabled(boolean enabled) {
        if (shadowRenderer != null) {
            if (enabled) {
                if (!app.getViewPort().getProcessors().contains(shadowRenderer)) {
                    app.getViewPort().addProcessor(shadowRenderer);
                }
            } else {
                app.getViewPort().removeProcessor(shadowRenderer);
            }
        }
    }

    // ==================== Getter方法 ====================

    /**
     * 获取太阳光源
     *
     * @return DirectionalLight 太阳光源对象
     */
    public DirectionalLight getSun() {
        return sun;
    }

    /**
     * 获取环境光源
     *
     * @return AmbientLight 环境光源对象
     */
    public AmbientLight getAmbientLight() {
        return ambientLight;
    }

    /**
     * 获取阴影渲染器
     *
     * @return DirectionalLightShadowRenderer 阴影渲染器对象
     */
    public DirectionalLightShadowRenderer getShadowRenderer() {
        return shadowRenderer;
    }

    // ==================== 调试信息 ====================

    /**
     * 打印光照系统配置信息
     */
    public void printConfiguration() {
    }
}
