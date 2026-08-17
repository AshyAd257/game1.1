package com.Hecate.puppet.newmode;

import com.jme3.app.SimpleApplication;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Quad;
import com.Hecate.puppet.editor.Button;

/**
 * 新模式编辑器UI - 八棱柱卡片系统
 *
 * 核心功能：
 * 1. 卡片列表（显示8张卡片）
 * 2. 属性编辑器（长宽高、旋转、缩放、UV、镜像）
 * 3. 可拖动面板背景
 */
public class NewModeEditorUI {

    private final SimpleApplication app;
    private final Node guiNode;
    private final BitmapFont guiFont;

    // UI根节点
    private Node editorRootNode;

    // UI面板
    private NewModePartListPanel partListPanel;  // 8张卡片列表
    private NewModeInspectorPanel inspectorPanel;  // 属性检查器
    private NewModeButtonPanel buttonPanel;  // 左侧按钮面板

    // 渲染器引用
    private NewModePuppetRenderer renderer;

    // 顶栏按钮
    private Button backButton;
    private Button exitButton;

    // 编辑器状态
    private NewModeSkeleton currentSkeleton;
    private NewModeBone selectedBone;
    private int selectedCardIndex = -1;  // 当前选中的卡片索引（0-7）
    private boolean visible = false;

    // 布局参数
    private int screenWidth;
    private int screenHeight;
    private final int topBarHeight = 70;
    private final int partListWidth = 250;   // 左侧卡片列表宽度（和旧模式一致）
    private final int inspectorWidth = 280;  // 右侧属性检查器宽度（和旧模式一致）

    public NewModeEditorUI(SimpleApplication app) {
        this.app = app;
        this.guiNode = app.getGuiNode();
        this.guiFont = app.getAssetManager().loadFont("Interface/Fonts/Default.fnt");
        this.screenWidth = app.getCamera().getWidth();
        this.screenHeight = app.getCamera().getHeight();

        initializeUI();
    }

    /**
     * 初始化UI
     */
    private void initializeUI() {
        editorRootNode = new Node("NewModeEditorUI");

        // 创建顶栏
        createTopBar();

        // 计算主区域参数
        int mainAreaHeight = screenHeight / 2;  // 栏目高度为屏幕高度的一半

        // 设置默认位置（根据用户拖动后确定的位置）
        int leftPanelX = 1879;
        int leftPanelY = 369;

        int rightPanelX = 2153;
        int rightPanelY = 371;

        // 创建左侧按钮面板
        int buttonPanelWidth = 150;
        int buttonPanelHeight = 200;
        buttonPanel = new NewModeButtonPanel(app, guiFont, 10, screenHeight - buttonPanelHeight - 10, buttonPanelWidth, buttonPanelHeight);
        editorRootNode.attachChild(buttonPanel.getRootNode());

        // 创建左侧卡片列表
        partListPanel = new NewModePartListPanel(app, guiFont, leftPanelX, leftPanelY, partListWidth, mainAreaHeight);
        editorRootNode.attachChild(partListPanel.getRootNode());

        // 创建右侧属性检查器
        inspectorPanel = new NewModeInspectorPanel(app, guiFont, rightPanelX, rightPanelY, inspectorWidth, mainAreaHeight);
        editorRootNode.attachChild(inspectorPanel.getRootNode());

        // 设置按钮面板回调
        setupButtonPanelCallbacks();

        // 默认隐藏
        editorRootNode.setCullHint(com.jme3.scene.Spatial.CullHint.Always);
    }

    /**
     * 创建顶栏
     */
    private void createTopBar() {
        // 创建顶栏背景
        Quad bgQuad = new Quad(screenWidth, topBarHeight);
        Geometry topBarBg = new Geometry("TopBarBackground", bgQuad);
        Material bgMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        bgMat.setColor("Color", new ColorRGBA(0.15f, 0.15f, 0.15f, 0.95f));
        bgMat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        topBarBg.setMaterial(bgMat);
        topBarBg.setLocalTranslation(0, screenHeight - topBarHeight, -2);
        editorRootNode.attachChild(topBarBg);

        // 标题文本
        BitmapText titleText = new BitmapText(guiFont);
        titleText.setText("New Mode - 8-Card Ring Editor");
        titleText.setSize(guiFont.getCharSet().getRenderedSize() * 2.5f);
        titleText.setColor(ColorRGBA.Yellow);
        titleText.setLocalTranslation(10, screenHeight - 15, 0);
        editorRootNode.attachChild(titleText);

        // Back 按钮
        int buttonY = screenHeight - 35;
        int backX = screenWidth - 180;
        backButton = new Button(app, guiFont, "Back", backX, buttonY, 80, 40);
        backButton.setClickListener(() -> {
            if (editorCallbacks != null) {
                editorCallbacks.onBackButtonClicked();
            }
        });
        editorRootNode.attachChild(backButton.getRootNode());

        // Exit 按钮
        int exitX = screenWidth - 90;
        exitButton = new Button(app, guiFont, "Exit", exitX, buttonY, 80, 40);
        exitButton.setClickListener(() -> {
            if (editorCallbacks != null) {
                editorCallbacks.onExitButtonClicked();
            }
        });
        editorRootNode.attachChild(exitButton.getRootNode());
    }

    /**
     * 设置按钮面板回调
     */
    private void setupButtonPanelCallbacks() {
        if (buttonPanel == null) return;

        buttonPanel.setCallbacks(new NewModeButtonPanel.ButtonCallbacks() {
            @Override
            public void onSavePuppet() {
                System.out.println("[NewModeEditorUI] Save Puppet clicked");
                // 打开文件选择器保存木偶
                javax.swing.JFileChooser fileChooser = new javax.swing.JFileChooser();
                fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                    "Puppet files", "json"));
                fileChooser.setDialogTitle("保存木偶");

                int result = fileChooser.showSaveDialog(null);
                if (result == javax.swing.JFileChooser.APPROVE_OPTION) {
                    java.io.File selectedFile = fileChooser.getSelectedFile();
                    String filePath = selectedFile.getAbsolutePath();

                    // 确保文件有.json扩展名
                    if (!filePath.endsWith(".json")) {
                        filePath += ".json";
                    }

                    // 保存当前骨架
                    if (currentSkeleton != null) {
                        // TODO: 实现保存功能，调用PuppetIO或类似的保存方法
                        System.out.println("[NewModeEditorUI] Saving puppet to: " + filePath);
                    }
                }
            }

            @Override
            public void onLoadPuppet() {
                System.out.println("[NewModeEditorUI] Load Puppet clicked");
                // 打开文件选择器加载木偶
                javax.swing.JFileChooser fileChooser = new javax.swing.JFileChooser();
                fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                    "Puppet files", "json"));
                fileChooser.setDialogTitle("加载木偶");

                int result = fileChooser.showOpenDialog(null);
                if (result == javax.swing.JFileChooser.APPROVE_OPTION) {
                    java.io.File selectedFile = fileChooser.getSelectedFile();
                    String filePath = selectedFile.getAbsolutePath();

                    // TODO: 实现加载功能，调用PuppetIO或类似的加载方法
                    System.out.println("[NewModeEditorUI] Loading puppet from: " + filePath);
                }
            }

            @Override
            public void onAddCard() {
                System.out.println("[NewModeEditorUI] Add Card clicked - Not implemented yet");
                // TODO: 实现添加卡片功能
            }

            @Override
            public void onDeleteCard() {
                System.out.println("[NewModeEditorUI] Delete Card clicked - Not implemented yet");
                // TODO: 实现删除卡片功能
            }

            @Override
            public void onLoadTexture() {
                if (selectedBone != null && selectedCardIndex >= 0) {
                    // 打开文件选择器加载贴图
                    javax.swing.JFileChooser fileChooser = new javax.swing.JFileChooser();
                    fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                        "Image files", "png", "jpg", "jpeg", "bmp"));
                    fileChooser.setDialogTitle("加载贴图");

                    int result = fileChooser.showOpenDialog(null);
                    if (result == javax.swing.JFileChooser.APPROVE_OPTION) {
                        java.io.File selectedFile = fileChooser.getSelectedFile();
                        String texturePath = "Textures/" + selectedFile.getName();

                        // 设置贴图路径
                        selectedBone.setCardTexture(selectedCardIndex, texturePath);

                        // 刷新渲染
                        if (renderer != null) {
                            renderer.refreshCardTexture(selectedBone, selectedCardIndex);
                        }

                        // 刷新卡片列表显示
                        if (partListPanel != null) {
                            partListPanel.refreshCardList();
                        }

                        System.out.println("[NewModeEditorUI] Loaded texture: " + texturePath);
                    }
                } else {
                    System.out.println("[NewModeEditorUI] No card selected");
                }
            }
        });
    }

    /**
     * 设置当前编辑的骨架
     */
    public void setSkeleton(NewModeSkeleton skeleton) {
        this.currentSkeleton = skeleton;
        this.selectedBone = null;
        this.selectedCardIndex = -1;

        // 更新卡片列表
        if (partListPanel != null) {
            partListPanel.setSkeleton(skeleton);
        }
    }

    /**
     * 选择骨骼
     */
    public void selectBone(NewModeBone bone) {
        this.selectedBone = bone;
        this.selectedCardIndex = 0;  // 默认选择第一张卡片
        updateInspector();

        // 更新卡片列表高亮
        if (partListPanel != null) {
            partListPanel.setSelectedBone(bone);
        }

        // 在3D场景中显示选中框
        if (renderer != null) {
            renderer.setSelectedCard(bone, 0);
        }
    }

    /**
     * 选择卡片
     */
    public void selectCard(NewModeBone bone, int cardIndex) {
        this.selectedBone = bone;
        this.selectedCardIndex = cardIndex;
        updateInspector();

        // 更新卡片列表高亮
        if (partListPanel != null) {
            partListPanel.setSelectedCard(bone, cardIndex);
        }

        // 在3D场景中显示选中框
        if (renderer != null) {
            renderer.setSelectedCard(bone, cardIndex);
        }
    }

    /**
     * 更新属性检查器
     */
    private void updateInspector() {
        if (inspectorPanel == null || selectedBone == null) return;

        inspectorPanel.setBone(selectedBone, selectedCardIndex);
    }

    /**
     * 更新UI（每帧调用）
     */
    public void update(float tpf) {
        if (!visible) return;

        // 更新顶栏按钮
        if (backButton != null) {
            backButton.update(tpf);
        }
        if (exitButton != null) {
            exitButton.update(tpf);
        }
    }

    /**
     * 处理鼠标点击
     */
    public boolean handleMouseClick(int mouseX, int mouseY) {
        if (!visible) return false;

        // 检查Back按钮
        if (backButton != null && backButton.handleMouseClick(mouseX, mouseY)) {
            return true;
        }

        // 检查Exit按钮
        if (exitButton != null && exitButton.handleMouseClick(mouseX, mouseY)) {
            return true;
        }

        // 检查卡片列表
        if (partListPanel != null && partListPanel.handleMouseClick(mouseX, mouseY)) {
            return true;
        }

        // 检查属性检查器
        if (inspectorPanel != null && inspectorPanel.handleMouseClick(mouseX, mouseY)) {
            return true;
        }

        return false;
    }

    /**
     * 处理鼠标拖动
     * @return 如果UI正在处理拖动返回true，否则返回false
     */
    public boolean handleMouseDrag(int mouseX, int mouseY) {
        if (!visible) return false;

        // 处理卡片列表拖动
        if (partListPanel != null && partListPanel.handleMouseDrag(mouseX, mouseY)) {
            return true;
        }

        // 处理属性检查器拖动
        if (inspectorPanel != null && inspectorPanel.handleMouseDrag(mouseX, mouseY)) {
            return true;
        }

        return false;
    }

    /**
     * 处理鼠标释放
     */
    public void handleMouseRelease() {
        if (!visible) return;

        // 处理卡片列表释放
        if (partListPanel != null) {
            partListPanel.handleMouseRelease();
        }

        // 处理属性检查器释放
        if (inspectorPanel != null) {
            inspectorPanel.handleMouseRelease();
        }
    }

    /**
     * 设置渲染器引用
     */
    public void setRenderer(NewModePuppetRenderer renderer) {
        this.renderer = renderer;
        // 传递给 InspectorPanel
        if (inspectorPanel != null) {
            inspectorPanel.setRenderer(renderer);
        }
    }

    /**
     * 切换显示/隐藏
     */
    public void toggleVisibility() {
        visible = !visible;

        if (visible) {
            editorRootNode.setCullHint(com.jme3.scene.Spatial.CullHint.Never);
            guiNode.attachChild(editorRootNode);

            if (editorCallbacks != null) {
                editorCallbacks.onEditorOpened();
            }
        } else {
            editorRootNode.setCullHint(com.jme3.scene.Spatial.CullHint.Always);
            editorRootNode.removeFromParent();
        }
    }

    /**
     * 设置可见性
     */
    public void setVisible(boolean visible) {
        if (this.visible != visible) {
            toggleVisibility();
        }
    }

    /**
     * 清理资源
     */
    public void cleanup() {
        if (editorRootNode != null) {
            editorRootNode.removeFromParent();
        }
    }

    // ========== 回调接口 ==========

    public interface EditorCallbacks {
        void onEditorOpened();
        void onBackButtonClicked();
        void onExitButtonClicked();
    }

    private EditorCallbacks editorCallbacks;

    public void setEditorCallbacks(EditorCallbacks callbacks) {
        this.editorCallbacks = callbacks;
    }

    // ========== Getters ==========

    public boolean isVisible() {
        return visible;
    }

    public NewModeSkeleton getCurrentSkeleton() {
        return currentSkeleton;
    }

    public NewModeBone getSelectedBone() {
        return selectedBone;
    }

    public int getSelectedCardIndex() {
        return selectedCardIndex;
    }

    public NewModePartListPanel getPartListPanel() {
        return partListPanel;
    }

    public NewModeInspectorPanel getInspectorPanel() {
        return inspectorPanel;
    }
}
