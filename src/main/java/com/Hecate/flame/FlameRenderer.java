package com.Hecate.flame;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.Hecate.utils.LogUtils;

/**
 * 火焰渲染器
 * 实现两Pass渲染：
 * Pass 1: 将粒子渲染到强度图（加法混合）
 * Pass 2: 将强度图转换为彩色火焰（采样 + 噪声扰动）
 */
public class FlameRenderer {

    private final SimpleApplication app;
    private final AssetManager assetManager;
    private final Camera camera;

    // Pass 1: 强度场渲染
    private FrameBuffer intensityFBO;
    private Texture2D intensityTexture;
    private ViewPort intensityViewPort;
    private Geometry fullscreenQuad;

    // Pass 2: 火焰渲染
    private Geometry flameQuad;
    private Material flameMaterial;
    private Texture2D colorGradient;

    // 粒子系统
    private FlameParticleSystem particleSystem;

    // 渲染参数
    private float smokeThreshold = 0.25f;
    private float exposure = 1.2f;
    private float noiseFreq = 1.2f;
    private float noiseSpeed = 0.3f;
    private float noiseAmp = 0.01f;
    private float time = 0f;

    // 屏幕尺寸
    private int screenWidth;
    private int screenHeight;

    public FlameRenderer(SimpleApplication app) {
        this.app = app;
        this.assetManager = app.getAssetManager();
        this.camera = app.getCamera();
        this.screenWidth = camera.getWidth();
        this.screenHeight = camera.getHeight();

        // 创建粒子系统
        particleSystem = new FlameParticleSystem();

        // 初始化渲染管线
        initializeIntensityPass();
        initializeFlamePass();
    }

    /**
     * Pass 1: 初始化强度场渲染
     */
    private void initializeIntensityPass() {
        // 创建强度图纹理（单通道灰度，用于累加强度值）
        intensityTexture = new Texture2D(screenWidth, screenHeight, Image.Format.RGBA8);
        intensityTexture.setMinFilter(Texture.MinFilter.BilinearNoMipMaps);
        intensityTexture.setMagFilter(Texture.MagFilter.Bilinear);

        // 创建离屏渲染缓冲区
        intensityFBO = new FrameBuffer(screenWidth, screenHeight, 1);
        intensityFBO.setDepthBuffer(Image.Format.Depth);
        intensityFBO.setColorTexture(intensityTexture);

        // 创建离屏渲染视口
        intensityViewPort = app.getRenderManager().createPreView("IntensityPass", camera);
        intensityViewPort.setClearFlags(true, true, true);
        intensityViewPort.setBackgroundColor(ColorRGBA.Black); // 清零强度
        intensityViewPort.setOutputFrameBuffer(intensityFBO);

        // 创建全屏四边形（用于渲染每个粒子圆）
        fullscreenQuad = new Geometry("IntensityQuad", new Quad(2, 2));
        fullscreenQuad.setLocalTranslation(-1, -1, 0); // NDC坐标 [-1, 1]
    }

    /**
     * Pass 2: 初始化火焰渲染（临时测试版）
     */
    private void initializeFlamePass() {
        // 临时测试：使用简单的红色半透明材质来验证渲染
        flameMaterial = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        flameMaterial.setColor("Color", new ColorRGBA(1.0f, 0.3f, 0.0f, 0.5f)); // 橙红色半透明
        flameMaterial.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        flameMaterial.setTransparent(true);

        // 创建火焰渲染四边形 - 设置为屏幕中心的小方块用于测试
        float testSize = 200f;
        flameQuad = new Geometry("FlameQuad", new Quad(testSize, testSize));
        flameQuad.setMaterial(flameMaterial);
        flameQuad.setLocalTranslation(screenWidth/2 - testSize/2, screenHeight/2 - testSize/2, 0);
    }

    /**
     * 更新渲染器
     */
    public void update(float tpf) {
        // 更新粒子系统
        particleSystem.update(tpf);

        // 更新时间（用于噪声动画）
        time += tpf;
        // TODO: 恢复完整着色器后取消注释
        // flameMaterial.setFloat("Time", time);
    }

    /**
     * 渲染火焰
     */
    public void render(RenderManager renderManager) {
        // Pass 1: 渲染粒子到强度图
        renderIntensityField(renderManager);

        // Pass 2: 将强度图转换为彩色火焰（由外部调用时渲染到屏幕）
        // flameQuad 需要被添加到场景的 GUI 节点或透明队列
    }

    /**
     * Pass 1: 渲染强度场（临时简化版 - 直接渲染测试圆点）
     */
    private void renderIntensityField(RenderManager renderManager) {
        // 临时方案：直接在flameQuad上绘制简单的测试图案
        // 让火焰材质使用一个简单的白色测试纹理来验证渲染是否工作

        // 暂时跳过复杂的离屏渲染，等基础渲染工作后再实现
        // TODO: 实现正确的离屏渲染到intensityFBO
    }

    /**
     * 将3D世界坐标转换为屏幕UV坐标 (0-1)
     * （此方法暂时未使用，保留用于未来的2D渲染模式）
     */
    private Vector3f worldToScreen(Vector3f worldPos) {
        // 使用摄像机将3D坐标投影到屏幕
        Vector3f screenCoords = camera.getScreenCoordinates(worldPos);
        return new Vector3f(
            screenCoords.x / screenWidth,
            screenCoords.y / screenHeight,
            screenCoords.z
        );
    }

    /**
     * 获取火焰渲染几何体（用于添加到场景）
     */
    public Geometry getFlameQuad() {
        return flameQuad;
    }

    /**
     * 获取粒子系统
     */
    public FlameParticleSystem getParticleSystem() {
        return particleSystem;
    }

    /**
     * 在指定位置发射火焰（3D世界空间）
     */
    public void emitFlame(Vector3f worldPosition, int particleCount) {
        particleSystem.burst(worldPosition, particleCount, 0.3f, 0.6f);
    }

    /**
     * 设置发射器位置（3D世界空间）
     */
    public void setEmitterPosition(Vector3f position) {
        particleSystem.setEmitterPosition(position);
    }

    /**
     * 启用/禁用连续发射
     */
    public void setEmissionRate(float rate) {
        particleSystem.setEmissionRate(rate);
    }

    // ========== 参数调节方法（临时禁用） ==========

    public void setSmokeThreshold(float threshold) {
        this.smokeThreshold = threshold;
        // TODO: 恢复完整着色器后取消注释
        // flameMaterial.setFloat("SmokeThreshold", threshold);
    }

    public void setExposure(float exposure) {
        this.exposure = exposure;
        // TODO: 恢复完整着色器后取消注释
        // flameMaterial.setFloat("Exposure", exposure);
    }

    public void setNoiseParameters(float freq, float speed, float amp) {
        this.noiseFreq = freq;
        this.noiseSpeed = speed;
        this.noiseAmp = amp;
        // TODO: 恢复完整着色器后取消注释
        // flameMaterial.setFloat("NoiseFreq", freq);
        // flameMaterial.setFloat("NoiseSpeed", speed);
        // flameMaterial.setFloat("NoiseAmp", amp);
    }

    /**
     * 清理资源
     */
    public void cleanup() {
        particleSystem.clear();
        if (intensityViewPort != null) {
            app.getRenderManager().removePreView(intensityViewPort);
        }
    }
}
