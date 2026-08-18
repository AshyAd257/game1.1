package com.Hecate.ui;

import com.jme3.app.SimpleApplication;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Node;
import com.jme3.ui.Picture;
import com.Hecate.player.BuffType;

import java.util.List;
import java.util.function.Consumer;

/**
 * 波次结算后的 Buff 三选一界面。
 * <p>纯文字粗糙按钮：3个选项横排显示，高亮当前选中项。A/D 左右切换（循环），
 * Enter 确认。显示期间使用独立的输入映射名，不与 {@code PlayerController}
 * 的移动键位冲突——具体的"锁玩家操作"由调用方（{@code PlayerController}）负责，
 * 本类只负责选择交互本身。
 */
public class BuffSelectUI implements ActionListener {

    private static final ColorRGBA NORMAL_COLOR = new ColorRGBA(0.7f, 0.7f, 0.7f, 1f);
    private static final ColorRGBA HIGHLIGHT_COLOR = ColorRGBA.Yellow;

    private final SimpleApplication app;
    private final Node rootNode;

    private Node uiNode;
    private Picture background;
    private BitmapText[] optionTexts;
    private BitmapText titleText;

    private List<BuffType> currentOptions;
    private int selectedIndex = 0;
    private Consumer<BuffType> onConfirm;
    private boolean visible = false;

    public BuffSelectUI(SimpleApplication app) {
        this.app = app;
        this.rootNode = app.getGuiNode();
        initializeUI();
        registerInputMappings();
    }

    private void initializeUI() {
        uiNode = new Node("BuffSelectUI");

        int screenWidth = app.getCamera().getWidth();
        int screenHeight = app.getCamera().getHeight();

        // 半透明黑色背景，覆盖屏幕中间一条横带，突出"这是个需要注意的弹窗"
        float panelHeight = 140f;
        float panelY = screenHeight / 2f - panelHeight / 2f;

        background = new Picture("BuffSelectBackground");
        background.setWidth(screenWidth);
        background.setHeight(panelHeight);
        background.setPosition(0, panelY);

        Material bgMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        bgMat.setColor("Color", new ColorRGBA(0, 0, 0, 0.85f));
        bgMat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        background.setMaterial(bgMat);

        uiNode.attachChild(background);

        BitmapFont font = app.getAssetManager().loadFont("Interface/Fonts/Default.fnt");

        titleText = new BitmapText(font);
        titleText.setSize(20);
        titleText.setColor(ColorRGBA.White);
        titleText.setText("选择一项强化 [A/D 切换   Enter 确认]");
        titleText.setLocalTranslation(
                screenWidth / 2f - titleText.getLineWidth() / 2f,
                panelY + panelHeight - 20,
                0);
        uiNode.attachChild(titleText);

        // 3个横排选项，均分屏幕宽度
        optionTexts = new BitmapText[3];
        for (int i = 0; i < 3; i++) {
            BitmapText text = new BitmapText(font);
            text.setSize(24);
            text.setColor(NORMAL_COLOR);
            text.setText("");
            optionTexts[i] = text;
            uiNode.attachChild(text);
        }

        uiNode.setCullHint(com.jme3.scene.Spatial.CullHint.Always);
        rootNode.attachChild(uiNode);
    }

    private void registerInputMappings() {
        app.getInputManager().addMapping("BuffSelectLeft", new KeyTrigger(KeyInput.KEY_A));
        app.getInputManager().addMapping("BuffSelectRight", new KeyTrigger(KeyInput.KEY_D));
        app.getInputManager().addMapping("BuffSelectConfirm", new KeyTrigger(KeyInput.KEY_RETURN));
        // 常驻注册监听器，但onAction内部通过visible短路——避免show/hide时反复增删监听器
        app.getInputManager().addListener(this, "BuffSelectLeft", "BuffSelectRight", "BuffSelectConfirm");
    }

    /**
     * 显示选择界面
     * @param options 恰好3个候选buff
     * @param onConfirm 玩家确认选择后的回调，参数为被选中的buff
     */
    public void show(List<BuffType> options, Consumer<BuffType> onConfirm) {
        this.currentOptions = options;
        this.onConfirm = onConfirm;
        this.selectedIndex = 0;

        int screenWidth = app.getCamera().getWidth();
        int screenHeight = app.getCamera().getHeight();
        float panelHeight = 140f;
        float panelY = screenHeight / 2f - panelHeight / 2f;

        float slotWidth = screenWidth / 3f;
        for (int i = 0; i < 3; i++) {
            BitmapText text = optionTexts[i];
            text.setText(options.get(i).displayName);
            float textX = slotWidth * i + slotWidth / 2f - text.getLineWidth() / 2f;
            text.setLocalTranslation(textX, panelY + panelHeight / 2f, 0);
        }

        refreshHighlight();

        uiNode.setCullHint(com.jme3.scene.Spatial.CullHint.Never);
        visible = true;
    }

    public void hide() {
        visible = false;
        uiNode.setCullHint(com.jme3.scene.Spatial.CullHint.Always);
    }

    public boolean isVisible() {
        return visible;
    }

    private void refreshHighlight() {
        for (int i = 0; i < optionTexts.length; i++) {
            boolean selected = (i == selectedIndex);
            optionTexts[i].setColor(selected ? HIGHLIGHT_COLOR : NORMAL_COLOR);
            String base = currentOptions.get(i).displayName;
            optionTexts[i].setText(selected ? ("[ " + base + " ]") : ("  " + base + "  "));

            // 重新居中（文字加了括号后宽度变化，需要重新计算位置）
            int screenWidth = app.getCamera().getWidth();
            int screenHeight = app.getCamera().getHeight();
            float panelHeight = 140f;
            float panelY = screenHeight / 2f - panelHeight / 2f;
            float slotWidth = screenWidth / 3f;
            float textX = slotWidth * i + slotWidth / 2f - optionTexts[i].getLineWidth() / 2f;
            optionTexts[i].setLocalTranslation(textX, panelY + panelHeight / 2f, 0);
        }
    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if (!visible || !isPressed) {
            return;
        }

        switch (name) {
            case "BuffSelectLeft":
                selectedIndex = (selectedIndex + 2) % 3; // -1，用+2避免负数取模
                refreshHighlight();
                break;
            case "BuffSelectRight":
                selectedIndex = (selectedIndex + 1) % 3;
                refreshHighlight();
                break;
            case "BuffSelectConfirm":
                BuffType chosen = currentOptions.get(selectedIndex);
                hide();
                if (onConfirm != null) {
                    onConfirm.accept(chosen);
                }
                break;
        }
    }

    /**
     * 清理资源
     */
    public void cleanup() {
        app.getInputManager().deleteMapping("BuffSelectLeft");
        app.getInputManager().deleteMapping("BuffSelectRight");
        app.getInputManager().deleteMapping("BuffSelectConfirm");
        app.getInputManager().removeListener(this);

        if (uiNode != null) {
            uiNode.removeFromParent();
        }
    }
}
