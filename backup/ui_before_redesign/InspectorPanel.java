package com.Hecate.puppet.editor;

import com.jme3.app.SimpleApplication;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Quad;
import com.Hecate.puppet.Bone;
import com.Hecate.puppet.PuppetPartRenderer;
import com.Hecate.puppet.PuppetRenderer;
import com.Hecate.puppet.editor.command.CommandManager;

/**
 * Inspector面板 - 左边按钮，右边滑条
 */
public class InspectorPanel {

    private final SimpleApplication app;
    private final BitmapFont font;
    private final Node rootNode;
    private final int x, y, width, height;

    // 当前编辑的骨骼和渲染器
    private Bone currentBone;
    private PuppetPartRenderer currentPartRenderer;
    private CommandManager commandManager;
    private PuppetRenderer puppetRenderer;

    // 编辑状态
    private Vector3f editStartPosition;
    private Quaternion editStartRotation;
    private Vector3f editStartScale;
    private boolean isEditing = false;

    // 布局参数
    private final int buttonColumnWidth = 180;  // 左边按钮列宽度
    private final int sliderColumnWidth = 250;  // 右边滑条列宽度
    private final int columnSpacing = 20;       // 列间距
    private final int buttonHeight = 30;
    private final int buttonSpacing = 8;

    // UI组件 - 标题和信息
    private BitmapText titleText;
    private BitmapText boneNameText;
    private BitmapText parentNameText;

    // 按钮列（左边）
    private Button hideModeButton;
    private Button showAllButton;
    private Button loadTextureButton;
    private Button setParentButton;
    private Button clearParentButton;
    private Button transformModeButton;
    private Button undoButton;
    private Button redoButton;
    private Button copyButton;
    private Button pasteButton;
    private Button pasteMirrorButton;
    private Button gridSnapButton;
    private Button togglePreviewButton;

    // 滑条列（右边）
    private Slider widthSlider;
    private Slider heightSlider;
    private Slider prioritySlider;
    private Slider positionXSlider;
    private Slider positionYSlider;
    private Slider positionZSlider;
    private Slider rotationXSlider;
    private Slider rotationZSlider;
    private Slider gridSizeSlider;

    // 纹理预览
    private TexturePreviewPanel texturePreviewPanel;
    private EnlargedTexturePreviewPanel enlargedPreviewPanel;
    private boolean showTexturePreview = false;

    // 状态标志
    private boolean boneTransformMode = false;
    private boolean gridSnapEnabled = false;
    private boolean hideModeEnabled = false;
    private float gridSize = 10f;

    // 回调接口
    public interface PanelCallbacks {
        void onHideModeToggle(boolean enabled);
        void onShowAllParts();
        void onDeletePart();
        void onToggleBoneLines(boolean enabled);
        void onSetParent();
        void onClearParent();
        void onLoadPuppet();
        void onLoadTexture();
        void onLoadDirectionTexture(String direction);
        void onClearDirectionTexture(String direction);
        void onAddKeyframe();
        void onUndo();
        void onRedo();
        void onCopyBone();
        void onPasteBone();
        void onPasteBoneMirrored();
        void onGridSnapToggle(boolean enabled);
    }
    private PanelCallbacks callbacks;

    public InspectorPanel(SimpleApplication app, BitmapFont font, int x, int y, int width, int height) {
        this.app = app;
        this.font = font;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        this.rootNode = new Node("InspectorPanel");
        initializePanel();
    }

    /**
     * 初始化面板
     */
    private void initializePanel() {
        // 创建半透明背景
        Quad bgQuad = new Quad(width, height);
        Geometry background = new Geometry("InspectorBackground", bgQuad);
        Material bgMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        bgMat.setColor("Color", new ColorRGBA(0.15f, 0.15f, 0.15f, 0.9f));
        bgMat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        background.setMaterial(bgMat);
        background.setLocalTranslation(x, y, 0);
        rootNode.attachChild(background);

        int currentY = y + height - 20;

        // 标题
        titleText = new BitmapText(font);
        titleText.setText("=== Inspector ===");
        titleText.setSize(font.getCharSet().getRenderedSize() * 2.0f);
        titleText.setColor(ColorRGBA.Yellow);
        titleText.setLocalTranslation(x + 10, currentY, 1);
        rootNode.attachChild(titleText);
        currentY -= 35;

        // 骨骼名称
        boneNameText = new BitmapText(font);
        boneNameText.setText("No bone selected");
        boneNameText.setSize(font.getCharSet().getRenderedSize() * 1.5f);
        boneNameText.setColor(ColorRGBA.Cyan);
        boneNameText.setLocalTranslation(x + 10, currentY, 1);
        rootNode.attachChild(boneNameText);
        currentY -= 25;

        // 父骨骼名称
        parentNameText = new BitmapText(font);
        parentNameText.setText("Parent: None");
        parentNameText.setSize(font.getCharSet().getRenderedSize() * 1.2f);
        parentNameText.setColor(ColorRGBA.Gray);
        parentNameText.setLocalTranslation(x + 10, currentY, 1);
        rootNode.attachChild(parentNameText);
        currentY -= 40;

        // 左列：按钮区域
        int buttonX = x + 10;
        int buttonY = currentY;
        createButtonColumn(buttonX, buttonY);

        // 右列：滑条区域
        int sliderX = x + buttonColumnWidth + columnSpacing + 10;
        int sliderY = currentY;
        createSliderColumn(sliderX, sliderY);

        // 纹理预览面板（在滑条下方）
        int previewY = y + 100;
        texturePreviewPanel = new TexturePreviewPanel(app, font, sliderX, previewY, sliderColumnWidth, 150);
        rootNode.attachChild(texturePreviewPanel.getRootNode());
        // Note: TexturePreviewPanel doesn't have setVisible, will control via CullHint if needed
    }

    /**
     * 创建按钮列（左边）
     */
    private void createButtonColumn(int startX, int startY) {
        int currentY = startY;

        // Hide Mode 按钮
        hideModeButton = new Button(app, font, "Hide Mode: OFF", startX, currentY - buttonHeight, buttonColumnWidth, buttonHeight);
        hideModeButton.setClickListener(() -> {
            hideModeEnabled = !hideModeEnabled;
            hideModeButton.setText(hideModeEnabled ? "Hide Mode: ON" : "Hide Mode: OFF");
            if (callbacks != null) callbacks.onHideModeToggle(hideModeEnabled);
        });
        rootNode.attachChild(hideModeButton.getRootNode());
        currentY -= (buttonHeight + buttonSpacing);

        // Show All 按钮
        showAllButton = new Button(app, font, "Show All", startX, currentY - buttonHeight, buttonColumnWidth, buttonHeight);
        showAllButton.setClickListener(() -> {
            if (callbacks != null) callbacks.onShowAllParts();
        });
        rootNode.attachChild(showAllButton.getRootNode());
        currentY -= (buttonHeight + buttonSpacing);

        // Load Texture 按钮
        loadTextureButton = new Button(app, font, "Load Texture", startX, currentY - buttonHeight, buttonColumnWidth, buttonHeight);
        loadTextureButton.setClickListener(() -> {
            if (callbacks != null) callbacks.onLoadTexture();
        });
        rootNode.attachChild(loadTextureButton.getRootNode());
        currentY -= (buttonHeight + buttonSpacing);

        // Set Parent 按钮
        setParentButton = new Button(app, font, "Set Parent", startX, currentY - buttonHeight, buttonColumnWidth, buttonHeight);
        setParentButton.setClickListener(() -> {
            if (callbacks != null) callbacks.onSetParent();
        });
        rootNode.attachChild(setParentButton.getRootNode());
        currentY -= (buttonHeight + buttonSpacing);

        // Clear Parent 按钮
        clearParentButton = new Button(app, font, "Clear Parent", startX, currentY - buttonHeight, buttonColumnWidth, buttonHeight);
        clearParentButton.setClickListener(() -> {
            if (callbacks != null) callbacks.onClearParent();
        });
        rootNode.attachChild(clearParentButton.getRootNode());
        currentY -= (buttonHeight + buttonSpacing);

        // Transform Mode 按钮
        transformModeButton = new Button(app, font, "Transform: Part", startX, currentY - buttonHeight, buttonColumnWidth, buttonHeight);
        transformModeButton.setClickListener(() -> {
            boneTransformMode = !boneTransformMode;
            transformModeButton.setText(boneTransformMode ? "Transform: Bone" : "Transform: Part");
        });
        rootNode.attachChild(transformModeButton.getRootNode());
        currentY -= (buttonHeight + buttonSpacing);

        // Undo 按钮
        undoButton = new Button(app, font, "Undo", startX, currentY - buttonHeight, buttonColumnWidth, buttonHeight);
        undoButton.setClickListener(() -> {
            if (callbacks != null) callbacks.onUndo();
        });
        rootNode.attachChild(undoButton.getRootNode());
        currentY -= (buttonHeight + buttonSpacing);

        // Redo 按钮
        redoButton = new Button(app, font, "Redo", startX, currentY - buttonHeight, buttonColumnWidth, buttonHeight);
        redoButton.setClickListener(() -> {
            if (callbacks != null) callbacks.onRedo();
        });
        rootNode.attachChild(redoButton.getRootNode());
        currentY -= (buttonHeight + buttonSpacing);

        // Copy 按钮
        copyButton = new Button(app, font, "Copy", startX, currentY - buttonHeight, buttonColumnWidth, buttonHeight);
        copyButton.setClickListener(() -> {
            if (callbacks != null) callbacks.onCopyBone();
        });
        rootNode.attachChild(copyButton.getRootNode());
        currentY -= (buttonHeight + buttonSpacing);

        // Paste 按钮
        pasteButton = new Button(app, font, "Paste", startX, currentY - buttonHeight, buttonColumnWidth, buttonHeight);
        pasteButton.setClickListener(() -> {
            if (callbacks != null) callbacks.onPasteBone();
        });
        rootNode.attachChild(pasteButton.getRootNode());
        currentY -= (buttonHeight + buttonSpacing);

        // Paste Mirror 按钮
        pasteMirrorButton = new Button(app, font, "Paste Mirror", startX, currentY - buttonHeight, buttonColumnWidth, buttonHeight);
        pasteMirrorButton.setClickListener(() -> {
            if (callbacks != null) callbacks.onPasteBoneMirrored();
        });
        rootNode.attachChild(pasteMirrorButton.getRootNode());
        currentY -= (buttonHeight + buttonSpacing);

        // Grid Snap 按钮
        gridSnapButton = new Button(app, font, "Grid: OFF", startX, currentY - buttonHeight, buttonColumnWidth, buttonHeight);
        gridSnapButton.setClickListener(() -> {
            gridSnapEnabled = !gridSnapEnabled;
            gridSnapButton.setText(gridSnapEnabled ? "Grid: ON" : "Grid: OFF");
            if (callbacks != null) callbacks.onGridSnapToggle(gridSnapEnabled);
        });
        rootNode.attachChild(gridSnapButton.getRootNode());
        currentY -= (buttonHeight + buttonSpacing);

        // Toggle Preview 按钮
        togglePreviewButton = new Button(app, font, "Preview: OFF", startX, currentY - buttonHeight, buttonColumnWidth, buttonHeight);
        togglePreviewButton.setClickListener(() -> {
            showTexturePreview = !showTexturePreview;
            togglePreviewButton.setText(showTexturePreview ? "Preview: ON" : "Preview: OFF");
            // Control visibility via CullHint
            if (texturePreviewPanel != null) {
                texturePreviewPanel.getRootNode().setCullHint(
                    showTexturePreview ? com.jme3.scene.Spatial.CullHint.Never : com.jme3.scene.Spatial.CullHint.Always
                );
            }
        });
        rootNode.attachChild(togglePreviewButton.getRootNode());
    }

    /**
     * 创建滑条列（右边）
     */
    private void createSliderColumn(int startX, int startY) {
        int currentY = startY;
        int sliderSpacing = 45;

        // Width 滑条
        widthSlider = new Slider(app, font, "Width", 10f, 500f, 100f, startX, currentY - 30);
        widthSlider.setChangeListener(value -> {
            if (currentPartRenderer != null) {
                currentPartRenderer.adjustWidth(value);
            }
        });
        rootNode.attachChild(widthSlider.getRootNode());
        currentY -= sliderSpacing;

        // Height 滑条
        heightSlider = new Slider(app, font, "Height", 10f, 500f, 100f, startX, currentY - 30);
        heightSlider.setChangeListener(value -> {
            if (currentPartRenderer != null) {
                currentPartRenderer.adjustHeight(value);
            }
        });
        rootNode.attachChild(heightSlider.getRootNode());
        currentY -= sliderSpacing;

        // Priority 滑条
        prioritySlider = new Slider(app, font, "Priority", -100f, 100f, 0f, startX, currentY - 30);
        prioritySlider.setChangeListener(value -> {
            if (currentBone != null) {
                currentBone.setPriority((int)value);
            }
        });
        rootNode.attachChild(prioritySlider.getRootNode());
        currentY -= sliderSpacing;

        // Position X 滑条
        positionXSlider = new Slider(app, font, "Pos X", -500f, 500f, 0f, startX, currentY - 30);
        positionXSlider.setChangeListener(value -> {
            if (currentBone != null) {
                Vector3f pos = currentBone.getLocalPosition();
                currentBone.setLocalPosition(new Vector3f(value, pos.y, pos.z));
            }
        });
        rootNode.attachChild(positionXSlider.getRootNode());
        currentY -= sliderSpacing;

        // Position Y 滑条
        positionYSlider = new Slider(app, font, "Pos Y", -500f, 500f, 0f, startX, currentY - 30);
        positionYSlider.setChangeListener(value -> {
            if (currentBone != null) {
                Vector3f pos = currentBone.getLocalPosition();
                currentBone.setLocalPosition(new Vector3f(pos.x, value, pos.z));
            }
        });
        rootNode.attachChild(positionYSlider.getRootNode());
        currentY -= sliderSpacing;

        // Position Z 滑条
        positionZSlider = new Slider(app, font, "Pos Z", -500f, 500f, 0f, startX, currentY - 30);
        positionZSlider.setChangeListener(value -> {
            if (currentBone != null) {
                Vector3f pos = currentBone.getLocalPosition();
                currentBone.setLocalPosition(new Vector3f(pos.x, pos.y, value));
            }
        });
        rootNode.attachChild(positionZSlider.getRootNode());
        currentY -= sliderSpacing;

        // Rotation X 滑条
        rotationXSlider = new Slider(app, font, "Rot X", -180f, 180f, 0f, startX, currentY - 30);
        rotationXSlider.setChangeListener(value -> {
            if (currentBone != null) {
                Quaternion rot = currentBone.getLocalRotation();
                float[] angles = rot.toAngles(new float[3]);
                currentBone.setLocalRotation(new Quaternion().fromAngles(
                    (float)Math.toRadians(value), angles[1], angles[2]
                ));
            }
        });
        rootNode.attachChild(rotationXSlider.getRootNode());
        currentY -= sliderSpacing;

        // Rotation Z 滑条
        rotationZSlider = new Slider(app, font, "Rot Z", -180f, 180f, 0f, startX, currentY - 30);
        rotationZSlider.setChangeListener(value -> {
            if (currentBone != null) {
                Quaternion rot = currentBone.getLocalRotation();
                float[] angles = rot.toAngles(new float[3]);
                currentBone.setLocalRotation(new Quaternion().fromAngles(
                    angles[0], angles[1], (float)Math.toRadians(value)
                ));
            }
        });
        rootNode.attachChild(rotationZSlider.getRootNode());
        currentY -= sliderSpacing;

        // Grid Size 滑条
        gridSizeSlider = new Slider(app, font, "Grid", 1f, 50f, 10f, startX, currentY - 30);
        gridSizeSlider.setChangeListener(value -> {
            gridSize = value;
        });
        rootNode.attachChild(gridSizeSlider.getRootNode());
    }

    /**
     * 设置当前骨骼
     */
    public void setBone(Bone bone, PuppetPartRenderer partRenderer) {
        this.currentBone = bone;
        this.currentPartRenderer = partRenderer;
        updateDisplay();
    }

    /**
     * 更新显示
     */
    public void updateDisplay() {
        if (currentBone == null) {
            boneNameText.setText("No bone selected");
            parentNameText.setText("Parent: None");
            return;
        }

        // 更新骨骼名称
        boneNameText.setText(currentBone.getName());

        // 更新父骨骼
        if (currentBone.getParent() != null) {
            parentNameText.setText("Parent: " + currentBone.getParent().getName());
        } else {
            parentNameText.setText("Parent: None");
        }

        // 更新滑条值
        if (currentPartRenderer != null) {
            widthSlider.setValue(currentPartRenderer.getWidth());
            heightSlider.setValue(currentPartRenderer.getHeight());
        }

        prioritySlider.setValue(currentBone.getPriority());

        Vector3f pos = currentBone.getLocalPosition();
        positionXSlider.setValue(pos.x);
        positionYSlider.setValue(pos.y);
        positionZSlider.setValue(pos.z);

        Quaternion rot = currentBone.getLocalRotation();
        float[] angles = rot.toAngles(new float[3]);
        rotationXSlider.setValue((float)Math.toDegrees(angles[0]));
        rotationZSlider.setValue((float)Math.toDegrees(angles[2]));

        // 更新纹理预览
        if (currentPartRenderer != null && currentPartRenderer.getTexture() != null) {
            texturePreviewPanel.setTexture(currentPartRenderer.getTexture());
        } else {
            texturePreviewPanel.setTexture(null);
        }
    }

    /**
     * 清空显示
     */
    public void clear() {
        currentBone = null;
        currentPartRenderer = null;
        boneNameText.setText("No bone selected");
        parentNameText.setText("Parent: None");
    }

    /**
     * 更新
     */
    public void update(float tpf) {
        // 更新所有按钮
        hideModeButton.update(tpf);
        showAllButton.update(tpf);
        loadTextureButton.update(tpf);
        setParentButton.update(tpf);
        clearParentButton.update(tpf);
        transformModeButton.update(tpf);
        undoButton.update(tpf);
        redoButton.update(tpf);
        copyButton.update(tpf);
        pasteButton.update(tpf);
        pasteMirrorButton.update(tpf);
        gridSnapButton.update(tpf);
        togglePreviewButton.update(tpf);

        // 滑条不需要更新（没有update方法）

        // 更新纹理预览
        if (texturePreviewPanel != null) {
            texturePreviewPanel.update(tpf);
        }
    }

    /**
     * 处理鼠标点击
     */
    public boolean handleMouseClick(int mouseX, int mouseY) {
        // 检查按钮
        if (hideModeButton.handleMouseClick(mouseX, mouseY)) return true;
        if (showAllButton.handleMouseClick(mouseX, mouseY)) return true;
        if (loadTextureButton.handleMouseClick(mouseX, mouseY)) return true;
        if (setParentButton.handleMouseClick(mouseX, mouseY)) return true;
        if (clearParentButton.handleMouseClick(mouseX, mouseY)) return true;
        if (transformModeButton.handleMouseClick(mouseX, mouseY)) return true;
        if (undoButton.handleMouseClick(mouseX, mouseY)) return true;
        if (redoButton.handleMouseClick(mouseX, mouseY)) return true;
        if (copyButton.handleMouseClick(mouseX, mouseY)) return true;
        if (pasteButton.handleMouseClick(mouseX, mouseY)) return true;
        if (pasteMirrorButton.handleMouseClick(mouseX, mouseY)) return true;
        if (gridSnapButton.handleMouseClick(mouseX, mouseY)) return true;
        if (togglePreviewButton.handleMouseClick(mouseX, mouseY)) return true;

        // 检查滑条
        if (widthSlider.handleMouseClick(mouseX, mouseY)) return true;
        if (heightSlider.handleMouseClick(mouseX, mouseY)) return true;
        if (prioritySlider.handleMouseClick(mouseX, mouseY)) return true;
        if (positionXSlider.handleMouseClick(mouseX, mouseY)) return true;
        if (positionYSlider.handleMouseClick(mouseX, mouseY)) return true;
        if (positionZSlider.handleMouseClick(mouseX, mouseY)) return true;
        if (rotationXSlider.handleMouseClick(mouseX, mouseY)) return true;
        if (rotationZSlider.handleMouseClick(mouseX, mouseY)) return true;
        if (gridSizeSlider.handleMouseClick(mouseX, mouseY)) return true;

        // 检查纹理预览
        if (texturePreviewPanel != null && texturePreviewPanel.handleMousePress(mouseX, mouseY)) {
            return true;
        }

        return false;
    }

    /**
     * 处理鼠标拖动
     */
    public void handleMouseDrag(int mouseX, int mouseY) {
        widthSlider.handleMouseDrag(mouseX, mouseY);
        heightSlider.handleMouseDrag(mouseX, mouseY);
        prioritySlider.handleMouseDrag(mouseX, mouseY);
        positionXSlider.handleMouseDrag(mouseX, mouseY);
        positionYSlider.handleMouseDrag(mouseX, mouseY);
        positionZSlider.handleMouseDrag(mouseX, mouseY);
        rotationXSlider.handleMouseDrag(mouseX, mouseY);
        rotationZSlider.handleMouseDrag(mouseX, mouseY);
        gridSizeSlider.handleMouseDrag(mouseX, mouseY);
    }

    /**
     * 处理鼠标释放
     */
    public void handleMouseRelease() {
        widthSlider.handleMouseRelease();
        heightSlider.handleMouseRelease();
        prioritySlider.handleMouseRelease();
        positionXSlider.handleMouseRelease();
        positionYSlider.handleMouseRelease();
        positionZSlider.handleMouseRelease();
        rotationXSlider.handleMouseRelease();
        rotationZSlider.handleMouseRelease();
        gridSizeSlider.handleMouseRelease();
    }

    // ========== Getters and Setters ==========

    public Node getRootNode() {
        return rootNode;
    }

    public Bone getCurrentBone() {
        return currentBone;
    }

    public PuppetPartRenderer getCurrentPartRenderer() {
        return currentPartRenderer;
    }

    public void setCallbacks(PanelCallbacks callbacks) {
        this.callbacks = callbacks;
    }

    public void setCommandManager(CommandManager commandManager) {
        this.commandManager = commandManager;
    }

    public void setPuppetRenderer(PuppetRenderer puppetRenderer) {
        this.puppetRenderer = puppetRenderer;
    }

    public Button getSetParentButton() {
        return setParentButton;
    }

    public boolean isBoneTransformMode() {
        return boneTransformMode;
    }

    public boolean isGridSnapEnabled() {
        return gridSnapEnabled;
    }

    public float getGridSize() {
        return gridSize;
    }
}
