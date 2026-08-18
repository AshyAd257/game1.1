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
import com.Hecate.localization.Localization;

import java.util.List;
import java.util.function.Consumer;

/**
 * 波次结算后的 Buff 三选一界面。
 * <p>显示3个选项横排，包含标题和描述。鼠标滚轮上下切换（循环），
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

        // 增加背景面板高度，为多行文字（标题+描述）留出空间
        float panelHeight = 220f;
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

        // 使用 JME3 默认字体
        BitmapFont font = app.getAssetManager().loadFont("Interface/Fonts/Default.fnt");

        titleText = new BitmapText(font);
        titleText.setText(Localization.get("buff.select.title"));
        titleText.setSize(20);  // 设置字体大小
        titleText.setLocalTranslation(
                screenWidth / 2f - titleText.getLineWidth() / 2f,
                panelY + panelHeight - 20,
                0);
        titleText.setColor(ColorRGBA.White);
        uiNode.attachChild(titleText);

        // 3个横排选项，均分屏幕宽度
        optionTexts = new BitmapText[3];
        for (int i = 0; i < 3; i++) {
            BitmapText text = new BitmapText(font);
            text.setText("");
            text.setSize(18);  // 选项字体稍小
            text.setColor(NORMAL_COLOR);
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
        float panelHeight = 220f;
        float panelY = screenHeight / 2f - panelHeight / 2f;

        float slotWidth = screenWidth / 3f;
        for (int i = 0; i < 3; i++) {
            BitmapText text = optionTexts[i];
            BuffType buff = options.get(i);

            // 显示标题和描述（多行文本）
            String display = buff.getDisplayName() + "\n" + buff.getDescription();
            text.setText(display);

            // 水平居中，垂直在面板中间
            float textX = slotWidth * i + slotWidth / 2f - text.getLineWidth() / 2f;
            float textY = panelY + panelHeight / 2f + 20;  // 稍微上移，留出描述空间
            text.setLocalTranslation(textX, textY, 0);
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

            BuffType buff = currentOptions.get(i);
            String display = buff.getDisplayName() + "\n" + buff.getDescription();

            // 选中时不加括号，只用颜色高亮，避免文字宽度变化
            optionTexts[i].setText(display);

            // 重新居中
            int screenWidth = app.getCamera().getWidth();
            int screenHeight = app.getCamera().getHeight();
            float panelHeight = 220f;
            float panelY = screenHeight / 2f - panelHeight / 2f;
            float slotWidth = screenWidth / 3f;
            float textX = slotWidth * i + slotWidth / 2f - optionTexts[i].getLineWidth() / 2f;
            float textY = panelY + panelHeight / 2f + 20;
            optionTexts[i].setLocalTranslation(textX, textY, 0);
        }
    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if (!visible || !isPressed) {
            return;
        }

        if ("BuffSelectLeft".equals(name)) {
            selectedIndex = (selectedIndex + 2) % 3; // 向左切换（-1 用 +2 避免负数）
            refreshHighlight();
        } else if ("BuffSelectRight".equals(name)) {
            selectedIndex = (selectedIndex + 1) % 3; // 向右切换
            refreshHighlight();
        } else if ("BuffSelectConfirm".equals(name)) {
            BuffType chosen = currentOptions.get(selectedIndex);
            hide();
            if (onConfirm != null) {
                onConfirm.accept(chosen);
            }
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
