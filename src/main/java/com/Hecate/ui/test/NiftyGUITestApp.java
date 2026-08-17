package com.Hecate.ui.test;

import com.jme3.app.SimpleApplication;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.niftygui.NiftyJmeDisplay;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Box;
import com.jme3.system.AppSettings;
import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.builder.*;
import de.lessvoid.nifty.controls.button.builder.ButtonBuilder;
import de.lessvoid.nifty.controls.label.builder.LabelBuilder;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.nifty.screen.ScreenController;

/**
 * Nifty GUI 测试应用
 *
 * 运行方式: 在IDE中直接运行此类的main方法
 */
public class NiftyGUITestApp extends SimpleApplication implements ScreenController {

    private Nifty nifty;

    public static void main(String[] args) {
        NiftyGUITestApp app = new NiftyGUITestApp();

        AppSettings settings = new AppSettings(true);
        settings.setTitle("UI Library Test - Nifty GUI");
        settings.setWidth(1280);
        settings.setHeight(720);
        settings.setVSync(true);

        app.setSettings(settings);
        app.setShowSettings(false);
        app.start();
    }

    @Override
    public void simpleInitApp() {
        // 创建3D场景
        createScene();

        // 初始化 Nifty GUI
        NiftyJmeDisplay niftyDisplay = NiftyJmeDisplay.newNiftyJmeDisplay(
                assetManager, inputManager, audioRenderer, guiViewPort);
        nifty = niftyDisplay.getNifty();

        // 使用 Java Builder API 创建 UI (不需要XML文件)
        createNiftyUI();

        guiViewPort.addProcessor(niftyDisplay);

        // 启用鼠标
        flyCam.setDragToRotate(true);
        inputManager.setCursorVisible(true);
    }

    private void createScene() {
        Box box = new Box(1, 1, 1);
        Geometry geom = new Geometry("Box", box);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.Green);
        geom.setMaterial(mat);
        geom.setLocalTranslation(0, 0, -5);
        rootNode.attachChild(geom);
    }

    private void createNiftyUI() {
        nifty.loadStyleFile("nifty-default-styles.xml");
        nifty.loadControlFile("nifty-default-controls.xml");

        // 使用 Builder API 创建界面
        new ScreenBuilder("start") {{
            controller(NiftyGUITestApp.this);

            layer(new LayerBuilder("background") {{
                childLayoutCenter();
                backgroundColor("#000a");
            }});

            layer(new LayerBuilder("foreground") {{
                childLayoutVertical();
                padding("50px");

                // 标题面板
                panel(new PanelBuilder("titlePanel") {{
                    childLayoutHorizontal();
                    alignCenter();
                    height("50px");
                    width("100%");
                    backgroundColor("#444a");
                    padding("10px");

                    control(new LabelBuilder("titleLabel") {{
                        text("Nifty GUI 测试面板");
                        font("aurulent-sans-16.fnt");
                        color("#fff");
                        width("100%");
                        alignCenter();
                    }});
                }});

                // 主内容面板
                panel(new PanelBuilder("contentPanel") {{
                    childLayoutVertical();
                    alignCenter();
                    valignCenter();
                    height("*");
                    width("400px");
                    backgroundColor("#333a");
                    padding("20px");
                    marginTop("20px");

                    // 信息标签
                    control(new LabelBuilder("infoLabel") {{
                        text("这是 Nifty GUI 的 Java Builder API 示例");
                        font("aurulent-sans-16.fnt");
                        color("#ccc");
                        width("100%");
                        marginBottom("20px");
                    }});

                    // 按钮1
                    control(new ButtonBuilder("testButton", "点击测试") {{
                        width("200px");
                        height("40px");
                        alignCenter();
                        marginBottom("10px");
                        interactOnClick("onTestButtonClick()");
                    }});

                    // 按钮2
                    control(new ButtonBuilder("actionButton", "执行操作") {{
                        width("200px");
                        height("40px");
                        alignCenter();
                        marginBottom("10px");
                        interactOnClick("onActionButtonClick()");
                    }});

                    // 关闭按钮
                    control(new ButtonBuilder("closeButton", "关闭应用") {{
                        width("200px");
                        height("40px");
                        alignCenter();
                        marginTop("30px");
                        interactOnClick("onCloseButtonClick()");
                    }});
                }});

                // 底部状态栏
                panel(new PanelBuilder("statusBar") {{
                    childLayoutHorizontal();
                    height("30px");
                    width("100%");
                    backgroundColor("#222a");
                    padding("5px");
                    marginTop("auto");

                    control(new LabelBuilder("statusLabel") {{
                        text("状态: 就绪 | Nifty GUI 1.4.3 | jME 3.5.2");
                        font("aurulent-sans-16.fnt");
                        color("#888");
                    }});
                }});
            }});
        }}.build(nifty);

        nifty.gotoScreen("start");
    }

    // ScreenController 方法
    @Override
    public void bind(Nifty nifty, Screen screen) {
    }

    @Override
    public void onStartScreen() {
    }

    @Override
    public void onEndScreen() {
    }

    // 按钮回调方法
    public void onTestButtonClick() {

    }

    public void onActionButtonClick() {
    }

    public void onCloseButtonClick() {
        stop();
    }

    @Override
    public void simpleUpdate(float tpf) {
        Geometry box = (Geometry) rootNode.getChild("Box");
        if (box != null) {
            box.rotate(0, tpf * 0.5f, 0);
        }
    }
}
