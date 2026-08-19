package com.Hecate.ui;

import com.jme3.app.SimpleApplication;
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
import com.Hecate.ui.common.TTFontLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger logger = LoggerFactory.getLogger(BuffSelectUI.class);
    private static final ColorRGBA NORMAL_COLOR = new ColorRGBA(0.7f, 0.7f, 0.7f, 1f);
    private static final ColorRGBA HIGHLIGHT_COLOR = ColorRGBA.Yellow;
    private static final String FONT_PATH = "Interface/Fonts/ZLabsBitmap_12px_CN（简体中文）.ttf";

    private final SimpleApplication app;
    private final Node rootNode;

    private Node uiNode;
    private Picture background;
    private Node[] optionTextNodes;
    private Node titleTextNode;
    private TTFontLoader titleFontLoader;
    private TTFontLoader optionFontLoader;

    private List<BuffType> currentOptions;
    private int selectedIndex = 0;
    private Consumer<BuffType> onConfirm;
    private boolean visible = false;

    public BuffSelectUI(SimpleApplication app) {
        this.app = app;
        this.rootNode = app.getGuiNode();
        this.titleFontLoader = TTFontLoader.loadFontFromResource(app.getAssetManager(), FONT_PATH, 24f);
        this.optionFontLoader = TTFontLoader.loadFontFromResource(app.getAssetManager(), FONT_PATH, 18f);
        if (titleFontLoader == null || optionFontLoader == null) {
            logger.error("加载TTF字体失败: {}", FONT_PATH);
        }
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

        // 使用 TTF 像素字体创建标题
        titleTextNode = titleFontLoader.createText(
                Localization.get("buff.select.title"),
                ColorRGBA.White
        );
        titleTextNode.setLocalTranslation(
                screenWidth / 2f - 100,  // 临时位置，稍后调整
                panelY + panelHeight - 30,
                0);
        uiNode.attachChild(titleTextNode);

        // 3个横排选项（占位节点，实际内容在 show()/refreshHighlight() 中重建）
        optionTextNodes = new Node[3];
        for (int i = 0; i < 3; i++) {
            Node textNode = optionFontLoader.createText("", NORMAL_COLOR);
            optionTextNodes[i] = textNode;
            uiNode.attachChild(textNode);
        }

        uiNode.setCullHint(com.jme3.scene.Spatial.CullHint.Always);
        rootNode.attachChild(uiNode);
    }

    /**
     * 用新内容替换指定下标的选项文本节点（TTFontLoader 不支持原地更新文本，需重建节点）
     */
    private void replaceOptionText(int index, String display, ColorRGBA color, float x, float y) {
        Node oldNode = optionTextNodes[index];
        if (oldNode != null) {
            oldNode.removeFromParent();
        }
        Node newNode = optionFontLoader.createText(display, color);
        newNode.setLocalTranslation(x, y, 0);
        uiNode.attachChild(newNode);
        optionTextNodes[index] = newNode;
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
        int screenWidth = app.getCamera().getWidth();
        int screenHeight = app.getCamera().getHeight();
        float panelHeight = 220f;
        float panelY = screenHeight / 2f - panelHeight / 2f;
        float slotWidth = screenWidth / 3f;

        for (int i = 0; i < optionTextNodes.length; i++) {
            boolean selected = (i == selectedIndex);
            ColorRGBA color = selected ? HIGHLIGHT_COLOR : NORMAL_COLOR;

            BuffType buff = currentOptions.get(i);
            String display = buff.getDisplayName() + "\n" + buff.getDescription();

            // 重新居中并用新颜色重建文本节点
            float textX = slotWidth * i + slotWidth / 2f - 80;
            float textY = panelY + panelHeight / 2f + 20;
            replaceOptionText(i, display, color, textX, textY);
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
