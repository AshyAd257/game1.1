package com.Hecate.puppet.editor.lemur;

import com.jme3.app.SimpleApplication;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Box;
import com.jme3.system.AppSettings;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.style.BaseStyles;

/**
 * Lemur UI 编辑器测试
 *
 * 演示如何在木偶编辑器中使用 Lemur GUI
 * 运行此类来测试新的 UI 面板
 */
public class LemurEditorTest extends SimpleApplication {

    private LemurSliderPanel sliderPanel;
    private LemurButtonPanel buttonPanel;
    private LemurPartListPanel partListPanel;

    // 测试用的方块
    private Geometry testBox;
    private float testWidth = 1f;
    private float testHeight = 1f;
    private float testPosX = 0f;
    private float testPosY = 0f;
    private float testRotZ = 0f;

    public static void main(String[] args) {
        LemurEditorTest app = new LemurEditorTest();

        AppSettings settings = new AppSettings(true);
        settings.setTitle("Lemur UI Editor Test");
        settings.setWidth(1600);
        settings.setHeight(900);
        settings.setVSync(true);
        settings.setSamples(4);

        app.setSettings(settings);
        app.setShowSettings(false);
        app.start();
    }

    @Override
    public void simpleInitApp() {
        // 初始化 Lemur
        GuiGlobals.initialize(this);
        BaseStyles.loadGlassStyle();
        GuiGlobals.getInstance().getStyles().setDefaultStyle("glass");

        // 设置背景色
        viewPort.setBackgroundColor(new ColorRGBA(0.2f, 0.2f, 0.25f, 1f));

        // 禁用 FlyCam，启用鼠标
        flyCam.setDragToRotate(true);
        inputManager.setCursorVisible(true);

        // 设置相机
        cam.setLocation(new Vector3f(0, 5, 15));
        cam.lookAt(Vector3f.ZERO, Vector3f.UNIT_Y);

        // 添加光照
        setupLights();

        // 创建测试场景
        createTestScene();

        // 创建 Lemur UI 面板
        createLemurPanels();

    }

    private void setupLights() {
        // 环境光
        AmbientLight ambient = new AmbientLight();
        ambient.setColor(ColorRGBA.White.mult(0.4f));
        rootNode.addLight(ambient);

        // 方向光
        DirectionalLight sun = new DirectionalLight();
        sun.setDirection(new Vector3f(-0.5f, -1f, -0.5f).normalizeLocal());
        sun.setColor(ColorRGBA.White.mult(0.8f));
        rootNode.addLight(sun);
    }

    private void createTestScene() {
        // 创建一个测试方块（模拟木偶部件）
        Box box = new Box(1, 1, 0.2f);
        testBox = new Geometry("TestPart", box);

        Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        mat.setBoolean("UseMaterialColors", true);
        mat.setColor("Diffuse", ColorRGBA.Blue);
        mat.setColor("Ambient", ColorRGBA.Blue.mult(0.3f));
        testBox.setMaterial(mat);

        rootNode.attachChild(testBox);

        // 创建地面参考
        Box ground = new Box(10, 0.1f, 10);
        Geometry groundGeom = new Geometry("Ground", ground);
        Material groundMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        groundMat.setColor("Color", new ColorRGBA(0.3f, 0.3f, 0.3f, 1f));
        groundGeom.setMaterial(groundMat);
        groundGeom.setLocalTranslation(0, -2, 0);
        rootNode.attachChild(groundGeom);
    }

    private void createLemurPanels() {
        int screenWidth = cam.getWidth();
        int screenHeight = cam.getHeight();

        // 左侧按钮面板
        buttonPanel = new LemurButtonPanel(this, 10, 100, 250, screenHeight - 200);
        buttonPanel.setCallback(buttonId -> {
            handleButtonClick(buttonId);
        });

        // 右侧上部：部件列表
        int rightX = screenWidth - 290;
        partListPanel = new LemurPartListPanel(this, rightX, screenHeight / 2, 280, screenHeight / 2 - 50);
        partListPanel.setCallback((index, partName) -> {
        });

        // 添加测试部件数据
        partListPanel.addPart("Body", 0, true, 0);
        partListPanel.addPart("Head", 1, true, 1);
        partListPanel.addPart("Left Arm", 2, true, 1);
        partListPanel.addPart("Left Hand", 3, true, 2);
        partListPanel.addPart("Right Arm", 4, true, 1);
        partListPanel.addPart("Right Hand", 5, true, 2);
        partListPanel.addPart("Left Leg", 6, true, 1);
        partListPanel.addPart("Left Foot", 7, true, 2);
        partListPanel.addPart("Right Leg", 8, true, 1);
        partListPanel.addPart("Right Foot", 9, true, 2);

        // 右侧下部：属性滑条
        sliderPanel = new LemurSliderPanel(this, rightX, 50, 280, screenHeight / 2 - 100);
        sliderPanel.setCallback((sliderId, value) -> {
            handleSliderChange(sliderId, value);
        });
    }

    private void handleButtonClick(String buttonId) {
        switch (buttonId) {
            case "new":
                break;
            case "open":
                break;
            case "save":
                break;
            case "undo":
                break;
            case "redo":
                break;
            case "front":
                cam.setLocation(new Vector3f(0, 5, 15));
                cam.lookAt(Vector3f.ZERO, Vector3f.UNIT_Y);
                break;
            case "back":
                cam.setLocation(new Vector3f(0, 5, -15));
                cam.lookAt(Vector3f.ZERO, Vector3f.UNIT_Y);
                break;
            case "left":
                cam.setLocation(new Vector3f(-15, 5, 0));
                cam.lookAt(Vector3f.ZERO, Vector3f.UNIT_Y);
                break;
            case "right":
                cam.setLocation(new Vector3f(15, 5, 0));
                cam.lookAt(Vector3f.ZERO, Vector3f.UNIT_Y);
                break;
            case "play":
                break;
            case "stop":
                break;
            default:
        }
    }

    private void handleSliderChange(String sliderId, float value) {
        switch (sliderId) {
            case "width":
                testWidth = value / 100f;
                updateTestBox();
                break;
            case "height":
                testHeight = value / 100f;
                updateTestBox();
                break;
            case "posX":
                testPosX = value / 10f;
                updateTestBox();
                break;
            case "posY":
                testPosY = value / 10f;
                updateTestBox();
                break;
            case "rotZ":
                testRotZ = value;
                updateTestBox();
                break;
            default:
        }
    }

    private void updateTestBox() {
        if (testBox != null) {
            testBox.setLocalTranslation(testPosX, testPosY, 0);
            testBox.setLocalScale(testWidth, testHeight, 1);
            testBox.rotate(0, 0, (float) Math.toRadians(testRotZ * 0.01f));
        }
    }

    @Override
    public void simpleUpdate(float tpf) {
        // 更新 Lemur 面板
        if (sliderPanel != null) {
            sliderPanel.update(tpf);
        }
        if (partListPanel != null) {
            partListPanel.update(tpf);
        }
    }

    @Override
    public void destroy() {
        // 清理
        if (sliderPanel != null) sliderPanel.cleanup();
        if (buttonPanel != null) buttonPanel.cleanup();
        if (partListPanel != null) partListPanel.cleanup();
        super.destroy();
    }
}
