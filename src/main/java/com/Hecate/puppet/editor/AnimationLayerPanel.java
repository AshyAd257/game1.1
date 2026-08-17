package com.Hecate.puppet.editor;

import com.jme3.app.SimpleApplication;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Quad;
import com.Hecate.puppet.animation.AnimationLayer;
import com.Hecate.puppet.editor.animation.EditorAnimationPlayer;
import com.Hecate.puppet.animation.AnimationClip;
import com.Hecate.puppet.animation.BoneMask;

import java.util.ArrayList;
import java.util.List;

/**
 * 动画层管理面板
 * 紧凑版设计，适合放在左侧按钮列下方
 */
public class AnimationLayerPanel {

    private final SimpleApplication app;
    private final BitmapFont font;
    private final Node rootNode;
    private final int x, y, width, height;

    private EditorAnimationPlayer animationPlayer;

    // UI组件
    private BitmapText titleText;
    private Button modeToggleButton;
    private Button addLayerButton;

    // 层条目列表
    private List<LayerEntry> layerEntries;

    // 布局参数
    private final int buttonHeight = 25;
    private final int buttonSpacing = 5;
    private final int entryHeight = 85;  // 每个层条目的高度
    private final int scrollOffset = 0;  // 滚动偏移（未来可扩展）

    public AnimationLayerPanel(SimpleApplication app, BitmapFont font, int x, int y, int width, int height) {
        this.app = app;
        this.font = font;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        this.rootNode = new Node("AnimationLayerPanel");
        this.layerEntries = new ArrayList<>();

        initializePanel();
    }

    /**
     * 初始化面板
     */
    private void initializePanel() {
        // 创建半透明背景
        Quad bgQuad = new Quad(width, height);
        Geometry background = new Geometry("LayerPanelBackground", bgQuad);
        Material bgMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        bgMat.setColor("Color", new ColorRGBA(0.1f, 0.1f, 0.1f, 0.9f));
        bgMat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        background.setMaterial(bgMat);
        background.setLocalTranslation(x, y, 0);
        rootNode.attachChild(background);

        int currentY = y + height - 15;

        // 标题
        titleText = new BitmapText(font);
        titleText.setText("=== Layers ===");
        titleText.setSize(font.getCharSet().getRenderedSize() * 1.5f);
        titleText.setColor(ColorRGBA.Yellow);
        titleText.setLocalTranslation(x + 10, currentY, 1);
        rootNode.attachChild(titleText);
        currentY -= 30;

        // Mode Toggle 按钮
        modeToggleButton = new Button(app, font, "Mode: OFF", x + 10, currentY - buttonHeight, width - 20, buttonHeight);
        modeToggleButton.setClickListener(() -> {
            if (animationPlayer != null) {
                if (animationPlayer.isLayeredMode()) {
                    animationPlayer.disableLayeredMode();
                    modeToggleButton.setText("Mode: OFF");
                } else {
                    animationPlayer.enableLayeredMode();
                    modeToggleButton.setText("Mode: ON");
                }
            }
        });
        rootNode.attachChild(modeToggleButton.getRootNode());
        currentY -= (buttonHeight + buttonSpacing);

        // Add Layer 按钮
        addLayerButton = new Button(app, font, "+ Add Layer", x + 10, currentY - buttonHeight, width - 20, buttonHeight);
        addLayerButton.setClickListener(() -> {
            // TODO: 打开添加层对话框
        });
        rootNode.attachChild(addLayerButton.getRootNode());
        currentY -= (buttonHeight + buttonSpacing + 10);

        // 层列表区域从这里开始
        // 层条目会动态添加
    }

    /**
     * 刷新层列表显示
     */
    public void refreshLayers() {
        if (animationPlayer == null) {
            return;
        }

        // 清除旧的层条目UI
        for (LayerEntry entry : layerEntries) {
            entry.dispose();
        }
        layerEntries.clear();

        // 获取所有层
        List<AnimationLayer> layers = animationPlayer.getLayers();

        // 计算起始Y坐标（在Add Layer按钮下方）
        int startY = y + height - 15 - 30 - (buttonHeight + buttonSpacing) - (buttonHeight + buttonSpacing + 10);

        // 为每个层创建UI条目
        for (int i = 0; i < layers.size(); i++) {
            AnimationLayer layer = layers.get(i);
            int entryY = startY - (i * (entryHeight + 5));

            LayerEntry entry = new LayerEntry(layer, x + 10, entryY, width - 20);
            layerEntries.add(entry);
        }
    }

    /**
     * 层条目UI（紧凑版）
     */
    private class LayerEntry {
        private AnimationLayer layer;
        private Node entryNode;

        private BitmapText nameText;
        private BitmapText infoText;
        private Slider weightSlider;
        private Button enableButton;
        private Button upButton;
        private Button downButton;
        private Button deleteButton;

        public LayerEntry(AnimationLayer layer, int x, int y, int width) {
            this.layer = layer;
            this.entryNode = new Node("LayerEntry_" + layer.getName());

            // 背景
            Quad bgQuad = new Quad(width, entryHeight);
            Geometry bg = new Geometry("EntryBg", bgQuad);
            Material bgMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
            bgMat.setColor("Color", new ColorRGBA(0.15f, 0.15f, 0.2f, 0.8f));
            bgMat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
            bg.setMaterial(bgMat);
            bg.setLocalTranslation(x, y - entryHeight, 0);
            entryNode.attachChild(bg);

            int currentY = y - 5;

            // 第1行：层名称
            nameText = new BitmapText(font);
            nameText.setText("▼ " + layer.getName());
            nameText.setSize(font.getCharSet().getRenderedSize() * 1.2f);
            nameText.setColor(ColorRGBA.Cyan);
            nameText.setLocalTranslation(x + 5, currentY, 1);
            entryNode.attachChild(nameText);
            currentY -= 18;

            // 第2行：遮罩类型 + Priority
            infoText = new BitmapText(font);
            String maskName = (layer.getMask() != null) ? layer.getMask().getName() : "全身";
            infoText.setText(maskName + " P:" + layer.getPriority());
            infoText.setSize(font.getCharSet().getRenderedSize());
            infoText.setColor(ColorRGBA.LightGray);
            infoText.setLocalTranslation(x + 5, currentY, 1);
            entryNode.attachChild(infoText);
            currentY -= 18;

            // 第3行：Weight滑条
            weightSlider = new Slider(app, font, "W", 0f, 1f, layer.getWeight(), x + 5, currentY - 15);
            weightSlider.setChangeListener(value -> {
                layer.setWeight(value);
            });
            entryNode.attachChild(weightSlider.getRootNode());
            currentY -= 25;

            // 第4行：4个小按钮
            int buttonWidth = (width - 25) / 4;
            int buttonX = x + 5;

            // 启用/禁用按钮
            enableButton = new Button(app, font, layer.isEnabled() ? "✓" : " ",
                                     buttonX, currentY - 20, buttonWidth, 20);
            enableButton.setClickListener(() -> {
                layer.setEnabled(!layer.isEnabled());
                enableButton.setText(layer.isEnabled() ? "✓" : " ");
            });
            entryNode.attachChild(enableButton.getRootNode());
            buttonX += buttonWidth + 5;

            // 上移按钮
            upButton = new Button(app, font, "↑", buttonX, currentY - 20, buttonWidth, 20);
            upButton.setClickListener(() -> {
                layer.setPriority(layer.getPriority() + 1);
                refreshLayers();
            });
            entryNode.attachChild(upButton.getRootNode());
            buttonX += buttonWidth + 5;

            // 下移按钮
            downButton = new Button(app, font, "↓", buttonX, currentY - 20, buttonWidth, 20);
            downButton.setClickListener(() -> {
                layer.setPriority(layer.getPriority() - 1);
                refreshLayers();
            });
            entryNode.attachChild(downButton.getRootNode());
            buttonX += buttonWidth + 5;

            // 删除按钮
            deleteButton = new Button(app, font, "×", buttonX, currentY - 20, buttonWidth, 20);
            deleteButton.setClickListener(() -> {
                if (animationPlayer != null) {
                    animationPlayer.removeLayer(layer);
                    refreshLayers();
                }
            });
            entryNode.attachChild(deleteButton.getRootNode());

            rootNode.attachChild(entryNode);
        }

        public void dispose() {
            rootNode.detachChild(entryNode);
        }
    }

    /**
     * 更新
     */
    public void update(float tpf) {
        modeToggleButton.update(tpf);
        addLayerButton.update(tpf);

        for (LayerEntry entry : layerEntries) {
            entry.enableButton.update(tpf);
            entry.upButton.update(tpf);
            entry.downButton.update(tpf);
            entry.deleteButton.update(tpf);
        }
    }

    /**
     * 处理鼠标点击
     */
    public boolean handleMouseClick(int mouseX, int mouseY) {
        if (modeToggleButton.handleMouseClick(mouseX, mouseY)) return true;
        if (addLayerButton.handleMouseClick(mouseX, mouseY)) return true;

        for (LayerEntry entry : layerEntries) {
            if (entry.weightSlider.handleMouseClick(mouseX, mouseY)) return true;
            if (entry.enableButton.handleMouseClick(mouseX, mouseY)) return true;
            if (entry.upButton.handleMouseClick(mouseX, mouseY)) return true;
            if (entry.downButton.handleMouseClick(mouseX, mouseY)) return true;
            if (entry.deleteButton.handleMouseClick(mouseX, mouseY)) return true;
        }

        return false;
    }

    /**
     * 处理鼠标拖动
     */
    public void handleMouseDrag(int mouseX, int mouseY) {
        for (LayerEntry entry : layerEntries) {
            entry.weightSlider.handleMouseDrag(mouseX, mouseY);
        }
    }

    /**
     * 处理鼠标释放
     */
    public void handleMouseRelease() {
        for (LayerEntry entry : layerEntries) {
            entry.weightSlider.handleMouseRelease();
        }
    }

    // ========== Getters & Setters ==========

    public Node getRootNode() {
        return rootNode;
    }

    public void setAnimationPlayer(EditorAnimationPlayer animationPlayer) {
        this.animationPlayer = animationPlayer;
        refreshLayers();
    }
}
