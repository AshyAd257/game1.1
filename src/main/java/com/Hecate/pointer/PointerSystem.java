package com.Hecate.pointer;

import com.jme3.app.SimpleApplication;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Texture;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.Hecate.player.PlayerController;
import com.Hecate.utils.LogUtils;

/**
 * 指针系统 - 使用自定义光标图片
 */
public class PointerSystem {
    private final SimpleApplication app;
    private final PlayerController playerController;

    // 光标相关
    private Geometry crosshairGeometry;
    private Material normalCrosshairMaterial;  // 实心光标材质
    private Material miningCrosshairMaterial;  // 空心光标材质（预留）

    // 光标大小（可以调整）
    private static final float CROSSHAIR_SIZE = 48f;

    // 信息文本
    private BitmapText infoText;

    // 状态
    private PointerState currentState = PointerState.NORMAL;  // 改为 NORMAL

    public PointerSystem(SimpleApplication app, PlayerController playerController) {
        this.app = app;
        this.playerController = playerController;

        initializeCrosshair();
        initializeInfoText();

        LogUtils.debug(PointerSystem.class, "指针系统初始化完成");
    }

    /**
     * 初始化光标
     */
    private void initializeCrosshair() {
        try {
            // 获取屏幕中心位置
            int screenWidth = app.getCamera().getWidth();
            int screenHeight = app.getCamera().getHeight();
            float centerX = screenWidth / 2f;
            float centerY = screenHeight / 2f;

            // 创建光标几何体
            Quad crosshairQuad = new Quad(CROSSHAIR_SIZE, CROSSHAIR_SIZE);
            crosshairGeometry = new Geometry("Crosshair", crosshairQuad);

            // 创建实心光标材质
            normalCrosshairMaterial = new Material(app.getAssetManager(),
                    "Common/MatDefs/Misc/Unshaded.j3md");

            // 加载实心光标纹理
            Texture normalTexture = app.getAssetManager()
                    .loadTexture("Interface/crosshair_normal.png");  // 你的实心光标图片
            normalTexture.setMagFilter(Texture.MagFilter.Nearest);
            normalTexture.setMinFilter(Texture.MinFilter.NearestNoMipMaps);

            normalCrosshairMaterial.setTexture("ColorMap", normalTexture);
            normalCrosshairMaterial.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
            normalCrosshairMaterial.setTransparent(true);

            // 设置默认材质（实心光标）
            crosshairGeometry.setMaterial(normalCrosshairMaterial);

            // 设置光标位置（屏幕中心）
            crosshairGeometry.setLocalTranslation(
                    centerX - CROSSHAIR_SIZE / 2,
                    centerY - CROSSHAIR_SIZE / 2,
                    1000  // Z值确保在其他UI元素之上
            );

            // 添加到GUI节点
            app.getGuiNode().attachChild(crosshairGeometry);

            LogUtils.debug(PointerSystem.class, "自定义光标加载成功");

        } catch (Exception e) {
            LogUtils.error(PointerSystem.class, "加载自定义光标失败，使用备用方案", e);
            createFallbackCrosshair();
        }
    }

    /**
     * 创建备用光标（如果图片加载失败）
     */
    private void createFallbackCrosshair() {
        // 获取屏幕中心位置
        int screenWidth = app.getCamera().getWidth();
        int screenHeight = app.getCamera().getHeight();
        float centerX = screenWidth / 2f;
        float centerY = screenHeight / 2f;

        // 创建简单的十字光标
        Node crosshairNode = new Node("CrosshairNode");

        // 水平线
        Quad hLine = new Quad(20, 2);
        Geometry hGeom = new Geometry("HLine", hLine);
        Material hMat = new Material(app.getAssetManager(),
                "Common/MatDefs/Misc/Unshaded.j3md");
        hMat.setColor("Color", ColorRGBA.White);
        hGeom.setMaterial(hMat);
        hGeom.setLocalTranslation(-10, -1, 0);

        // 垂直线
        Quad vLine = new Quad(2, 20);
        Geometry vGeom = new Geometry("VLine", vLine);
        Material vMat = new Material(app.getAssetManager(),
                "Common/MatDefs/Misc/Unshaded.j3md");
        vMat.setColor("Color", ColorRGBA.White);
        vGeom.setMaterial(vMat);
        vGeom.setLocalTranslation(-1, -10, 0);

        crosshairNode.attachChild(hGeom);
        crosshairNode.attachChild(vGeom);
        crosshairNode.setLocalTranslation(centerX, centerY, 1000);

        app.getGuiNode().attachChild(crosshairNode);
    }

    /**
     * 初始化信息文本
     */
    private void initializeInfoText() {
        BitmapFont font = app.getAssetManager().loadFont("Interface/Fonts/Default.fnt");
        infoText = new BitmapText(font, false);
        infoText.setSize(font.getCharSet().getRenderedSize());
        infoText.setColor(ColorRGBA.White);
        infoText.setText("");
        infoText.setLocalTranslation(10, app.getCamera().getHeight() - 10, 1001);

        app.getGuiNode().attachChild(infoText);
    }

    /**
     * 更新指针系统
     */
    public void update(float tpf) {
        // 更新信息文本
        updateInfoText();
    }

    /**
     * 更新信息文本
     */
    private void updateInfoText() {
        Vector3f playerPos = playerController.getPlayerPosition();
        String info = String.format("位置: (%.1f, %.1f, %.1f)",
                playerPos.x, playerPos.y, playerPos.z);

        infoText.setText(info);
    }

    /**
     * 获取当前状态
     */
    public PointerState getCurrentState() {
        return currentState;
    }

    /**
     * 设置状态
     */
    public void setState(PointerState state) {
        this.currentState = state;
    }

    /**
     * 清理资源
     */
    public void cleanup() {
        if (crosshairGeometry != null) {
            crosshairGeometry.removeFromParent();
        }
        if (infoText != null) {
            infoText.removeFromParent();
        }
    }
}
