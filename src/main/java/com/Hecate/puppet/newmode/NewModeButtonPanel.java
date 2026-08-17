package com.Hecate.puppet.newmode;

import com.jme3.app.SimpleApplication;
import com.jme3.font.BitmapFont;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Quad;
import com.Hecate.puppet.editor.Button;

/**
 * 新模式按钮面板 - 左侧控制按钮
 *
 * 功能：
 * 1. 添加部件（卡片）
 * 2. 删除部件
 * 3. 加载贴图
 */
public class NewModeButtonPanel {

    private final SimpleApplication app;
    private final BitmapFont font;
    private final Node rootNode;
    private final Node guiNode;
    private int x, y;  // 可变，支持拖动
    private final int width, height;

    // 按钮
    private Button savePuppetButton;      // 保存木偶
    private Button loadPuppetButton;      // 导入木偶
    private Button addCardButton;         // 添加卡片
    private Button deleteCardButton;      // 删除卡片
    private Button loadTextureButton;     // 加载贴图

    // 背景
    private Geometry backgroundGeometry;

    // 回调接口
    public interface ButtonCallbacks {
        void onSavePuppet();     // 保存木偶
        void onLoadPuppet();     // 导入木偶
        void onAddCard();        // 添加卡片
        void onDeleteCard();     // 删除卡片
        void onLoadTexture();    // 加载贴图
    }
    private ButtonCallbacks callbacks;

    public NewModeButtonPanel(SimpleApplication app, BitmapFont font, int x, int y, int width, int height) {
        this.app = app;
        this.font = font;
        this.guiNode = app.getGuiNode();
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        this.rootNode = new Node("NewModeButtonPanel");
        initializePanel();
    }

    private void initializePanel() {
        // 创建背景
        Quad bgQuad = new Quad(width, height);
        backgroundGeometry = new Geometry("ButtonPanelBackground", bgQuad);
        Material bgMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        bgMat.setColor("Color", new ColorRGBA(0.15f, 0.15f, 0.15f, 0.9f));
        bgMat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        backgroundGeometry.setMaterial(bgMat);
        backgroundGeometry.setLocalTranslation(0, 0, -1);
        rootNode.attachChild(backgroundGeometry);

        // 创建按钮
        createButtons();

        // 设置rootNode的屏幕位置
        updateRootNodePosition();
    }

    private void createButtons() {
        int screenHeight = app.getCamera().getHeight();
        float panelBottomGuiY = screenHeight - y - height;
        int absoluteX = x + 10;

        int buttonWidth = width - 20;
        int buttonHeight = 40;
        int buttonSpacing = 10;

        float currentY = height - 20 - buttonHeight;  // 从顶部开始

        // 保存木偶按钮
        int buttonAbsoluteY = (int)(panelBottomGuiY + currentY);
        savePuppetButton = new Button(app, font, "Save Puppet", absoluteX, buttonAbsoluteY, buttonWidth, buttonHeight);
        savePuppetButton.setClickListener(() -> {
            if (callbacks != null) {
                callbacks.onSavePuppet();
            }
        });
        guiNode.attachChild(savePuppetButton.getRootNode());
        currentY -= (buttonHeight + buttonSpacing);

        // 导入木偶按钮
        buttonAbsoluteY = (int)(panelBottomGuiY + currentY);
        loadPuppetButton = new Button(app, font, "Load Puppet", absoluteX, buttonAbsoluteY, buttonWidth, buttonHeight);
        loadPuppetButton.setClickListener(() -> {
            if (callbacks != null) {
                callbacks.onLoadPuppet();
            }
        });
        guiNode.attachChild(loadPuppetButton.getRootNode());
        currentY -= (buttonHeight + buttonSpacing);

        // 加载贴图按钮
        buttonAbsoluteY = (int)(panelBottomGuiY + currentY);
        loadTextureButton = new Button(app, font, "Load Texture", absoluteX, buttonAbsoluteY, buttonWidth, buttonHeight);
        loadTextureButton.setClickListener(() -> {
            if (callbacks != null) {
                callbacks.onLoadTexture();
            }
        });
        guiNode.attachChild(loadTextureButton.getRootNode());
        currentY -= (buttonHeight + buttonSpacing);

        // 添加卡片按钮
        buttonAbsoluteY = (int)(panelBottomGuiY + currentY);
        addCardButton = new Button(app, font, "Add Card", absoluteX, buttonAbsoluteY, buttonWidth, buttonHeight);
        addCardButton.setClickListener(() -> {
            if (callbacks != null) {
                callbacks.onAddCard();
            }
        });
        guiNode.attachChild(addCardButton.getRootNode());
        currentY -= (buttonHeight + buttonSpacing);

        // 删除卡片按钮
        buttonAbsoluteY = (int)(panelBottomGuiY + currentY);
        deleteCardButton = new Button(app, font, "Delete Card", absoluteX, buttonAbsoluteY, buttonWidth, buttonHeight);
        deleteCardButton.setClickListener(() -> {
            if (callbacks != null) {
                callbacks.onDeleteCard();
            }
        });
        guiNode.attachChild(deleteCardButton.getRootNode());
    }

    /**
     * 处理鼠标点击
     */
    public boolean handleMouseClick(int mouseX, int mouseY) {
        // 检查所有按钮
        if (savePuppetButton != null && savePuppetButton.handleMouseClick(mouseX, mouseY)) return true;
        if (loadPuppetButton != null && loadPuppetButton.handleMouseClick(mouseX, mouseY)) return true;
        if (loadTextureButton != null && loadTextureButton.handleMouseClick(mouseX, mouseY)) return true;
        if (addCardButton != null && addCardButton.handleMouseClick(mouseX, mouseY)) return true;
        if (deleteCardButton != null && deleteCardButton.handleMouseClick(mouseX, mouseY)) return true;

        return false;
    }

    /**
     * 更新 rootNode 位置
     */
    private void updateRootNodePosition() {
        int screenHeight = app.getCamera().getHeight();
        float panelBottomGuiY = screenHeight - y - height;
        rootNode.setLocalTranslation(x, panelBottomGuiY, 0);
    }

    // ========== Getters and Setters ==========

    public Node getRootNode() {
        return rootNode;
    }

    public void setCallbacks(ButtonCallbacks callbacks) {
        this.callbacks = callbacks;
    }
}
