package com.Hecate.ui.test;

import com.jme3.app.SimpleApplication;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Box;
import com.jme3.system.AppSettings;

// Lemur imports
import com.simsilica.lemur.*;
import com.simsilica.lemur.style.BaseStyles;

/**
 * UI库测试应用 - 测试 Lemur GUI
 *
 * 运行方式: 在IDE中直接运行此类的main方法
 */
public class UILibraryTestApp extends SimpleApplication {

    public static void main(String[] args) {
        UILibraryTestApp app = new UILibraryTestApp();

        AppSettings settings = new AppSettings(true);
        settings.setTitle("UI Library Test - Lemur");
        settings.setWidth(1280);
        settings.setHeight(720);
        settings.setVSync(true);

        app.setSettings(settings);
        app.setShowSettings(false);
        app.start();
    }

    @Override
    public void simpleInitApp() {
        // 初始化 Lemur GUI
        GuiGlobals.initialize(this);

        // 加载默认样式
        BaseStyles.loadGlassStyle();

        // 设置默认样式
        GuiGlobals.getInstance().getStyles().setDefaultStyle("glass");

        // 创建一个简单的3D场景作为背景
        createScene();

        // 创建 Lemur UI
        createLemurUI();

        // 启用鼠标光标
        flyCam.setDragToRotate(true);
        inputManager.setCursorVisible(true);
    }

    private void createScene() {
        // 创建一个旋转的方块作为背景
        Box box = new Box(1, 1, 1);
        Geometry geom = new Geometry("Box", box);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.Blue);
        geom.setMaterial(mat);
        geom.setLocalTranslation(0, 0, -5);
        rootNode.attachChild(geom);
    }

    private void createLemurUI() {
        // 创建主容器
        Container mainWindow = new Container();
        guiNode.attachChild(mainWindow);
        mainWindow.setLocalTranslation(50, cam.getHeight() - 50, 0);

        // 标题
        Label title = new Label("Lemur GUI 测试面板");
        title.setFontSize(24);
        mainWindow.addChild(title);

        // 分隔
        mainWindow.addChild(new Label(""));

        // 按钮测试
        Button testButton = new Button("点击测试按钮");
        testButton.addClickCommands(source -> {

        });
        mainWindow.addChild(testButton);

        // 复选框测试
        Checkbox checkbox = new Checkbox("启用某功能");
        mainWindow.addChild(checkbox);

        // 滑块测试
        Container sliderContainer = new Container();
        sliderContainer.addChild(new Label("音量:"));
        Slider slider = new Slider();
        slider.setPreferredSize(new Vector3f(200, 20, 0));
        slider.getModel().setPercent(0.5);
        sliderContainer.addChild(slider);
        mainWindow.addChild(sliderContainer);

        // 文本输入测试
        mainWindow.addChild(new Label("输入框:"));
        TextField textField = new TextField("在此输入文本...");
        textField.setPreferredWidth(200);
        mainWindow.addChild(textField);

        // 下拉选择器
        mainWindow.addChild(new Label(""));
        mainWindow.addChild(new Label("选择列表:"));
        ListBox<String> listBox = new ListBox<>();
        listBox.getModel().add("选项 1");
        listBox.getModel().add("选项 2");
        listBox.getModel().add("选项 3");
        listBox.setPreferredSize(new Vector3f(200, 80, 0));
        mainWindow.addChild(listBox);

        // 关闭按钮
        mainWindow.addChild(new Label(""));
        Button closeButton = new Button("关闭应用");
        closeButton.addClickCommands(source -> {
            stop();
        });
        mainWindow.addChild(closeButton);

        // 创建第二个窗口展示更多功能
        createSecondWindow();
    }

    private void createSecondWindow() {
        Container infoWindow = new Container();
        guiNode.attachChild(infoWindow);
        infoWindow.setLocalTranslation(350, cam.getHeight() - 50, 0);

        infoWindow.addChild(new Label("系统信息"));
        infoWindow.addChild(new Label(""));
        infoWindow.addChild(new Label("UI库: Lemur 1.16.0"));
        infoWindow.addChild(new Label("引擎: jMonkeyEngine 3.5.2"));
        infoWindow.addChild(new Label("Java: " + System.getProperty("java.version")));
        infoWindow.addChild(new Label(""));

        // 进度条模拟
        ProgressBar progressBar = new ProgressBar();
        progressBar.setPreferredSize(new Vector3f(200, 20, 0));
        progressBar.setProgressPercent(0.75);
        progressBar.setMessage("加载进度: 75%");
        infoWindow.addChild(progressBar);
    }

    private float angle = 0;

    @Override
    public void simpleUpdate(float tpf) {
        // 旋转背景方块
        angle += tpf;
        Geometry box = (Geometry) rootNode.getChild("Box");
        if (box != null) {
            box.rotate(0, tpf, 0);
        }
    }
}
