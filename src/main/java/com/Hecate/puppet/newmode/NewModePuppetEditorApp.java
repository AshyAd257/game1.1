package com.Hecate.puppet.newmode;

import com.jme3.app.SimpleApplication;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.RawInputListener;
import com.jme3.input.controls.*;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.light.DirectionalLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.system.AppSettings;

/**
 * 新模式木偶编辑器 - 八棱柱卡片系统
 *
 * 简化版编辑器，用于演示和测试新模式的核心功能
 */
public class NewModePuppetEditorApp extends SimpleApplication {

    private NewModeSkeleton skeleton;
    private NewModePuppetRenderer renderer;
    private NewModeEditorUI editorUI;  // UI系统

    // 相机控制
    private float cameraDistance = 5f;
    private float cameraYaw = 0f;
    private float cameraPitch = 0f;
    private Vector3f focusPoint = new Vector3f(0, 0, 0);

    // 鼠标拖拽状态
    private boolean isDragging = false;
    private float lastMouseX = 0f;
    private float lastMouseY = 0f;

    // 鼠标输入监听器
    private RawInputListener mouseListener;

    public static void main(String[] args) {
        NewModePuppetEditorApp app = new NewModePuppetEditorApp();

        AppSettings settings = new AppSettings(true);
        settings.setTitle("New Mode Puppet Editor");

        // 获取屏幕尺寸并使用最大化窗口（与旧模式一致）
        java.awt.GraphicsDevice gd = java.awt.GraphicsEnvironment
                .getLocalGraphicsEnvironment().getDefaultScreenDevice();
        java.awt.DisplayMode dm = gd.getDisplayMode();
        settings.setWidth(dm.getWidth());
        settings.setHeight(dm.getHeight());
        settings.setFullscreen(false);  // 不使用全屏模式，避免黑屏
        settings.setResizable(true);
        settings.setVSync(true);
        settings.setSamples(4); // 抗锯齿

        app.setSettings(settings);
        app.setShowSettings(false);

        app.start();
    }

    @Override
    public void simpleInitApp() {
        // 设置背景颜色为 #95959F (149, 149, 159)
        viewPort.setBackgroundColor(new ColorRGBA(149f/255f, 149f/255f, 159f/255f, 1.0f));

        // 设置光照
        DirectionalLight sun = new DirectionalLight();
        sun.setDirection(new Vector3f(-0.3f, -1f, -0.3f).normalizeLocal());
        sun.setColor(ColorRGBA.White);
        rootNode.addLight(sun);

        // 创建示例骨骼系统
        createTestSkeleton();

        // 创建渲染器
        renderer = new NewModePuppetRenderer(this, skeleton);
        renderer.initialize();
        renderer.attachToScene(rootNode);

        // 初始化相机
        setupCamera();

        // 设置输入控制
        setupInput();

        // 创建UI系统
        editorUI = new NewModeEditorUI(this);
        editorUI.setSkeleton(skeleton);
        editorUI.setRenderer(renderer);  // 传递渲染器引用

        // 默认选择根骨骼
        if (skeleton.getRootBone() != null) {
            editorUI.selectBone(skeleton.getRootBone());
        }

        editorUI.setVisible(true);  // 默认显示UI

        // 设置UI回调
        editorUI.setEditorCallbacks(new NewModeEditorUI.EditorCallbacks() {
            @Override
            public void onEditorOpened() {
                // 编辑器打开时重新设置回调
                setupUICallbacks();
            }

            @Override
            public void onBackButtonClicked() {
                // 返回按钮 - 隐藏UI
                editorUI.setVisible(false);
            }

            @Override
            public void onExitButtonClicked() {
                // 退出按钮 - 关闭应用
                stop();
            }
        });

        // 设置UI面板回调
        setupUICallbacks();
    }

    /**
     * 设置UI面板回调
     */
    private void setupUICallbacks() {
        // 设置卡片列表回调
        if (editorUI.getPartListPanel() != null) {
            editorUI.getPartListPanel().setCallbacks((bone, cardIndex) -> {
                editorUI.selectCard(bone, cardIndex);
            });
        }

        // 设置属性检查器回调
        if (editorUI.getInspectorPanel() != null) {
            editorUI.getInspectorPanel().setCallbacks(new NewModeInspectorPanel.InspectorCallbacks() {
                @Override
                public void onLoadTexture(NewModeBone bone, int cardIndex) {
                    // 打开文件选择器加载贴图
                    javax.swing.JFileChooser fileChooser = new javax.swing.JFileChooser();
                    fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                        "Image files", "png", "jpg", "jpeg", "bmp"));

                    int result = fileChooser.showOpenDialog(null);
                    if (result == javax.swing.JFileChooser.APPROVE_OPTION) {
                        java.io.File selectedFile = fileChooser.getSelectedFile();
                        String texturePath = "Textures/" + selectedFile.getName();

                        // 复制文件到assets目录（实际项目中需要实现）
                        // 这里暂时只设置路径
                        bone.setCardTexture(cardIndex, texturePath);

                        // 刷新卡片列表显示
                        if (editorUI.getPartListPanel() != null) {
                            editorUI.getPartListPanel().refreshCardList();
                        }
                    }
                }

                @Override
                public void onClearTexture(NewModeBone bone, int cardIndex) {
                    bone.setCardTexture(cardIndex, null);

                    // 刷新卡片列表显示
                    if (editorUI.getPartListPanel() != null) {
                        editorUI.getPartListPanel().refreshCardList();
                    }
                }
            });
        }
    }

    /**
     * 创建测试骨骼
     */
    private void createTestSkeleton() {
        skeleton = new NewModeSkeleton("TestPuppet");

        // 创建根骨骼
        NewModeBone root = new NewModeBone("Root");
        root.setLocalPosition(0, 0, 0);
        root.setRingRadius(0.5f);
        root.setPerspective(false); // 使用正交投影

        // 8张卡片已经在构造函数中初始化，默认值：width=1.0, height=2.0, zOffset=0
        // 用户可以通过 setCardTexture 设置贴图路径
        // 例如: root.setCardTexture(0, "Textures/card0.png");

        skeleton.addBone(root);
        skeleton.setRootBone(root);
    }

    /**
     * 设置相机
     */
    private void setupCamera() {
        flyCam.setEnabled(false); // 禁用默认飞行相机
        updateCameraPosition();
    }

    /**
     * 设置输入控制
     */
    private void setupInput() {
        // 鼠标滚轮缩放
        inputManager.addMapping("ZoomIn", new MouseAxisTrigger(MouseInput.AXIS_WHEEL, false));
        inputManager.addMapping("ZoomOut", new MouseAxisTrigger(MouseInput.AXIS_WHEEL, true));
        inputManager.addListener(analogListener, "ZoomIn", "ZoomOut");

        // I键切换UI
        inputManager.addMapping("ToggleUI", new KeyTrigger(KeyInput.KEY_I));
        inputManager.addListener(actionListener, "ToggleUI");

        // ESC退出
        inputManager.addMapping("Exit", new KeyTrigger(KeyInput.KEY_ESCAPE));
        inputManager.addListener(actionListener, "Exit");

        // 添加RawInputListener处理鼠标事件
        setupMouseListener();
    }

    /**
     * 设置鼠标监听器
     */
    private void setupMouseListener() {
        mouseListener = new RawInputListener() {
            @Override
            public void beginInput() {}

            @Override
            public void endInput() {}

            @Override
            public void onJoyAxisEvent(com.jme3.input.event.JoyAxisEvent evt) {}

            @Override
            public void onJoyButtonEvent(com.jme3.input.event.JoyButtonEvent evt) {}

            @Override
            public void onMouseMotionEvent(MouseMotionEvent evt) {
                int mouseX = evt.getX();
                int mouseY = evt.getY();

                // 如果正在拖动
                if (isDragging) {
                    // 先检查是否在拖动UI面板（面板或滑条）
                    boolean uiHandledDrag = false;
                    if (editorUI != null && editorUI.isVisible()) {
                        uiHandledDrag = editorUI.handleMouseDrag(mouseX, mouseY);
                    }

                    // 只有当UI没有处理拖动时，才旋转相机
                    if (!uiHandledDrag) {
                        float deltaX = mouseX - lastMouseX;
                        float deltaY = mouseY - lastMouseY;

                        cameraYaw += deltaX * 0.01f;
                        cameraPitch -= deltaY * 0.01f;

                        // 限制俯仰角
                        cameraPitch = Math.max(-1.5f, Math.min(1.5f, cameraPitch));

                        updateCameraPosition();
                    }
                }

                lastMouseX = mouseX;
                lastMouseY = mouseY;
            }

            @Override
            public void onMouseButtonEvent(MouseButtonEvent evt) {
                int mouseX = evt.getX();
                int mouseY = evt.getY();
                int button = evt.getButtonIndex();

                if (button == MouseInput.BUTTON_LEFT) {
                    if (evt.isPressed()) {
                        // 按下左键
                        if (editorUI != null && editorUI.isVisible()) {
                            // 先检查是否点击了面板标题栏（用于拖动）
                            if (editorUI.getPartListPanel() != null &&
                                editorUI.getPartListPanel().handleTitleBarClick(mouseX, mouseY)) {
                                isDragging = true;
                                return;
                            }

                            if (editorUI.getInspectorPanel() != null &&
                                editorUI.getInspectorPanel().handleTitleBarClick(mouseX, mouseY)) {
                                isDragging = true;
                                return;
                            }

                            // 然后检查是否点击了UI元素
                            if (editorUI.handleMouseClick(mouseX, mouseY)) {
                                return;
                            }
                        }

                        // 开始拖动相机
                        isDragging = true;
                        lastMouseX = mouseX;
                        lastMouseY = mouseY;
                    } else {
                        // 释放左键
                        if (editorUI != null && editorUI.isVisible()) {
                            editorUI.handleMouseRelease();
                        }
                        isDragging = false;
                    }
                }
            }

            @Override
            public void onKeyEvent(com.jme3.input.event.KeyInputEvent evt) {}

            @Override
            public void onTouchEvent(com.jme3.input.event.TouchEvent evt) {}
        };

        inputManager.addRawInputListener(mouseListener);
    }

    /**
     * 动作监听器
     */
    private final ActionListener actionListener = new ActionListener() {
        @Override
        public void onAction(String name, boolean isPressed, float tpf) {
            if (name.equals("ToggleUI")) {
                if (!isPressed && editorUI != null) {
                    editorUI.toggleVisibility();
                }
            } else if (name.equals("Exit") && !isPressed) {
                stop();
            }
        }
    };

    /**
     * 模拟监听器
     */
    private final AnalogListener analogListener = new AnalogListener() {
        @Override
        public void onAnalog(String name, float value, float tpf) {
            if (name.equals("ZoomIn")) {
                cameraDistance = Math.max(2f, cameraDistance - 0.5f);
                updateCameraPosition();
            } else if (name.equals("ZoomOut")) {
                cameraDistance = Math.min(20f, cameraDistance + 0.5f);
                updateCameraPosition();
            }
        }
    };

    /**
     * 更新相机位置
     */
    private void updateCameraPosition() {
        // 球面坐标转换
        float x = cameraDistance * (float) (Math.cos(cameraPitch) * Math.sin(cameraYaw));
        float y = cameraDistance * (float) Math.sin(cameraPitch);
        float z = cameraDistance * (float) (Math.cos(cameraPitch) * Math.cos(cameraYaw));

        Vector3f cameraPos = focusPoint.add(x, y, z);
        cam.setLocation(cameraPos);
        cam.lookAt(focusPoint, Vector3f.UNIT_Y);
    }

    @Override
    public void simpleUpdate(float tpf) {
        // 更新渲染器
        if (renderer != null) {
            renderer.update(tpf);
        }

        // 更新UI
        if (editorUI != null) {
            editorUI.update(tpf);
        }
    }

    @Override
    public void simpleRender(com.jme3.renderer.RenderManager rm) {
        // 可以在这里添加自定义渲染逻辑
    }

    @Override
    public void destroy() {
        if (renderer != null) {
            renderer.cleanup();
        }
        if (editorUI != null) {
            editorUI.cleanup();
        }
        super.destroy();
    }
}
