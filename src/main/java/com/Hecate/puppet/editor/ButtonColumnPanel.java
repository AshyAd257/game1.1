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

/**
 * 左侧按钮列面板
 * 包含所有控制按钮
 */
public class ButtonColumnPanel {

    private final SimpleApplication app;
    private final BitmapFont font;
    private final TTFontLoader ttfLoader;  // 新增：TTF字体加载器
    private final Node rootNode;
    private final int x, y, width, height;

    // 所有按钮
    private Button addPartButton;
    private Button hideModeButton;
    private Button showAllButton;
    private Button loadPuppetButton;
    private Button addPuppetButton;  // 新增：添加木偶（不清除现有）
    private Button savePuppetButton;
    private Button exportAnimButton;
    private Button importAnimButton;
    private Button loadTextureButton;
    private Button setParentButton;
    private Button addFreeBoneButton;
    private Button clearParentButton;
    private Button deletePartButton;
    private Button transformModeButton;
    private Button toggleBoneLinesButton;
    private Button addKeyframeButton;
    private Button addSnapshotButton;  // 新增快照关键帧按钮
    private Button deleteKeyframeButton;  // 新增删除关键帧按钮
    private Button copyKeyframeButton;  // 新增：复制关键帧按钮
    private Button pasteKeyframeButton;  // 新增：粘贴关键帧按钮
    private Button undoButton;
    private Button redoButton;
    private Button copyButton;
    private Button pasteButton;
    private Button pasteMirrorButton;
    private Button gridSnapButton;
    private Button togglePreviewButton;
    private Button toggleBillboardButton;  // 新增：Billboard开关按钮
    private Button textureModeButton;  // 新增：纹理模式按钮（单贴图/多贴图）

    // 播放控制按钮
    private Button playPauseButton;
    private Button resetButton;

    private Button gravityDirectionButton;  // 新增：重力方向按钮
    private Button cameraFollowButton;  // 新增：相机跟随按钮
    private Button swingEnableButton;  // 新增：摇摆开关按钮
    private Button swingAxisButton;  // 新增：摇摆轴按钮

    // 状态
    private boolean hideModeEnabled = false;
    private boolean boneTransformMode = false;
    private boolean boneLinesVisible = true;
    private boolean gridSnapEnabled = false;
    private boolean previewEnabled = false;
    private boolean isPlaying = false;
    private boolean billboardEnabled = true;  // 新增：当前选中部件的billboard状态
    private boolean multiTextureEnabled = false;  // 新增：多向纹理模式状态
    private boolean swingEnabled = false;  // 新增：摇摆开关状态

    // 回调接口
    public interface ButtonCallbacks {
        void onAddPart();
        void onHideModeToggle(boolean enabled);
        void onShowAllParts();
        void onLoadPuppet();
        void onAddPuppet();  // 新增：添加木偶（不清除现有）
        void onSavePuppet();
        void onExportAnimation();
        void onImportAnimation();
        void onLoadTexture();
        void onSetParent();
        void onAddFreeBone();
        void onClearParent();
        void onDeletePart();
        void onToggleBoneLines(boolean enabled);
        void onAddKeyframe();
        void onAddSnapshot();  // 新增快照关键帧回调
        void onDeleteKeyframe();  // 新增删除关键帧回调
        void onCopyKeyframe();  // 新增：复制关键帧回调
        void onPasteKeyframe();  // 新增：粘贴关键帧回调
        void onUndo();
        void onRedo();
        void onCopyBone();
        void onPasteBone();
        void onPasteBoneMirrored();
        void onGridSnapToggle(boolean enabled);
        void onPreviewToggle(boolean enabled);
        void onBillboardToggle(boolean enabled);  // 新增：Billboard开关回调
        void onTextureModeToggle(boolean multiTextureEnabled);  // 新增：纹理模式切换回调
        void onPlayPauseToggle(boolean playing);
        void onReset();
        void onDirectionChanged(String direction);
        void onGravityDirectionChanged();  // 新增：重力方向切换回调
        void onCameraFollowClicked();  // 新增：相机跟随按钮回调
        void onSwingEnableToggle(boolean enabled);  // 新增：摇摆开关回调
        void onSwingAxisChanged();  // 新增：摇摆轴切换回调
    }
    private ButtonCallbacks callbacks;

    // 原构造函数 - 只使用BitmapFont
    public ButtonColumnPanel(SimpleApplication app, BitmapFont font, int x, int y, int width, int height) {
        this.app = app;
        this.font = font;
        this.ttfLoader = null;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        this.rootNode = new Node("ButtonColumnPanel");
        initializePanel();
    }

    // 新构造函数 - 支持TTFontLoader
    public ButtonColumnPanel(SimpleApplication app, BitmapFont font, TTFontLoader ttfLoader, int x, int y, int width, int height) {
        this.app = app;
        this.font = font;
        this.ttfLoader = ttfLoader;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        this.rootNode = new Node("ButtonColumnPanel");
        initializePanel();
    }

    private void initializePanel() {
        // 创建半透明背景
        Quad bgQuad = new Quad(width, height);
        Geometry background = new Geometry("ButtonColumnBackground", bgQuad);
        Material bgMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        bgMat.setColor("Color", new ColorRGBA(0.1f, 0.1f, 0.1f, 0.85f));
        bgMat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        background.setMaterial(bgMat);
        background.setLocalTranslation(x, y, 0);
        rootNode.attachChild(background);

        // 标题
        BitmapText titleText = new BitmapText(font);
        titleText.setText("=== Controls ===");
        titleText.setSize(font.getCharSet().getRenderedSize() * 1.8f);
        titleText.setColor(ColorRGBA.Yellow);
        titleText.setLocalTranslation(x + 10, y + height - 20, 1);
        rootNode.attachChild(titleText);

        // 两列布局配置
        int currentY = y + height - 60;
        int buttonHeight = 34;
        int buttonSpacing = 5;
        int columnSpacing = 5;
        int buttonWidth = (width - columnSpacing - 20) / 2;  // 两列，中间留5px间距
        int leftColumnX = x + 10;
        int rightColumnX = leftColumnX + buttonWidth + columnSpacing;

        // 左列计数器和右列计数器
        int leftY = currentY;
        int rightY = currentY;

        // ========== 左列按钮 ==========
        // Add Part
        addPartButton = createButtonWithChinese("添加部件", "Add Part", leftColumnX, leftY - buttonHeight, buttonWidth, buttonHeight,
            () -> { if (callbacks != null) callbacks.onAddPart(); });
        leftY -= (buttonHeight + buttonSpacing);

        // Hide Mode
        hideModeButton = createButtonWithChinese("隐藏部件", "Hide: OFF", leftColumnX, leftY - buttonHeight, buttonWidth, buttonHeight,
            () -> {
                hideModeEnabled = !hideModeEnabled;
                if (ttfLoader != null) {
                    hideModeButton.setText(hideModeEnabled ? "隐藏部件:开" : "隐藏部件");
                } else {
                    hideModeButton.setText(hideModeEnabled ? "Hide: ON" : "Hide: OFF");
                }
                if (callbacks != null) callbacks.onHideModeToggle(hideModeEnabled);
            });
        leftY -= (buttonHeight + buttonSpacing);

        // Show All
        showAllButton = createButtonWithChinese("显示全部", "Show All", leftColumnX, leftY - buttonHeight, buttonWidth, buttonHeight,
            () -> { if (callbacks != null) callbacks.onShowAllParts(); });
        leftY -= (buttonHeight + buttonSpacing);

        // Load Puppet
        loadPuppetButton = createButtonWithChinese("加载木偶", "Load Puppet", leftColumnX, leftY - buttonHeight, buttonWidth, buttonHeight,
            () -> { if (callbacks != null) callbacks.onLoadPuppet(); });
        leftY -= (buttonHeight + buttonSpacing);

        // Add Puppet (新增: 添加木偶不清除现有)
        addPuppetButton = createButtonWithChinese("添加木偶", "Add Puppet", leftColumnX, leftY - buttonHeight, buttonWidth, buttonHeight,
            () -> { if (callbacks != null) callbacks.onAddPuppet(); });
        leftY -= (buttonHeight + buttonSpacing);

        // Save Puppet
        savePuppetButton = createButtonWithChinese("保存木偶", "Save Puppet", leftColumnX, leftY - buttonHeight, buttonWidth, buttonHeight,
            () -> { if (callbacks != null) callbacks.onSavePuppet(); });
        leftY -= (buttonHeight + buttonSpacing);

        // Export Animation
        exportAnimButton = createButtonWithChinese("导出动画", "Export Anim", leftColumnX, leftY - buttonHeight, buttonWidth, buttonHeight,
            () -> { if (callbacks != null) callbacks.onExportAnimation(); });
        leftY -= (buttonHeight + buttonSpacing);

        // Import Animation
        importAnimButton = createButtonWithChinese("导入动画", "Import Anim", leftColumnX, leftY - buttonHeight, buttonWidth, buttonHeight,
            () -> { if (callbacks != null) callbacks.onImportAnimation(); });
        leftY -= (buttonHeight + buttonSpacing);

        // Load Texture
        loadTextureButton = createButtonWithChinese("加载纹理", "Load Texture", leftColumnX, leftY - buttonHeight, buttonWidth, buttonHeight,
            () -> { if (callbacks != null) callbacks.onLoadTexture(); });
        leftY -= (buttonHeight + buttonSpacing);

        // Set Parent
        setParentButton = createButtonWithChinese("添加刚性骨骼", "Set Parent", leftColumnX, leftY - buttonHeight, buttonWidth, buttonHeight,
            () -> { if (callbacks != null) callbacks.onSetParent(); });
        leftY -= (buttonHeight + buttonSpacing);

        // Add Free EditorBone
        addFreeBoneButton = createButtonWithChinese("添加自由骨骼", "Add Free EditorBone", leftColumnX, leftY - buttonHeight, buttonWidth, buttonHeight,
            () -> { if (callbacks != null) callbacks.onAddFreeBone(); });
        leftY -= (buttonHeight + buttonSpacing);

        // Clear Parent
        clearParentButton = createButtonWithChinese("清除骨骼", "Clear Parent", leftColumnX, leftY - buttonHeight, buttonWidth, buttonHeight,
            () -> { if (callbacks != null) callbacks.onClearParent(); });
        leftY -= (buttonHeight + buttonSpacing);

        // Delete Part
        deletePartButton = createButtonWithChinese("删除部件", "Delete Part", leftColumnX, leftY - buttonHeight, buttonWidth, buttonHeight,
            () -> { if (callbacks != null) callbacks.onDeletePart(); });
        leftY -= (buttonHeight + buttonSpacing);

        // Transform Mode
        transformModeButton = createButtonWithChinese("模式:部件", "Mode: Part", leftColumnX, leftY - buttonHeight, buttonWidth, buttonHeight,
            () -> {
                boneTransformMode = !boneTransformMode;
                if (ttfLoader != null) {
                    transformModeButton.setText(boneTransformMode ? "模式:骨骼" : "模式:部件");
                } else {
                    transformModeButton.setText(boneTransformMode ? "Mode: EditorBone" : "Mode: Part");
                }
            });
        leftY -= (buttonHeight + buttonSpacing);

        // Toggle EditorBone Lines
        toggleBoneLinesButton = createButtonWithChinese("骨线:开", "Lines: ON", leftColumnX, leftY - buttonHeight, buttonWidth, buttonHeight,
            () -> {
                boneLinesVisible = !boneLinesVisible;
                if (ttfLoader != null) {
                    toggleBoneLinesButton.setText(boneLinesVisible ? "骨线:开" : "骨线:关");
                } else {
                    toggleBoneLinesButton.setText(boneLinesVisible ? "Lines: ON" : "Lines: OFF");
                }
                if (callbacks != null) callbacks.onToggleBoneLines(boneLinesVisible);
            });
        leftY -= (buttonHeight + buttonSpacing);

        // Play/Pause
        playPauseButton = createButtonWithChinese("播放动画", "Play", leftColumnX, leftY - buttonHeight, buttonWidth, buttonHeight,
            () -> {
                isPlaying = !isPlaying;
                if (ttfLoader != null) {
                    playPauseButton.setText(isPlaying ? "暂停" : "播放动画");
                } else {
                    playPauseButton.setText(isPlaying ? "Pause" : "Play");
                }
                if (callbacks != null) callbacks.onPlayPauseToggle(isPlaying);
            });
        leftY -= (buttonHeight + buttonSpacing);

        // Reset
        resetButton = createButtonWithChinese("重置时间轴", "Reset", leftColumnX, leftY - buttonHeight, buttonWidth, buttonHeight,
            () -> { if (callbacks != null) callbacks.onReset(); });

        // ========== 右列按钮 ==========
        // Add Keyframe (插值关键帧)
        addKeyframeButton = createButtonWithChinese("添加关键帧", "Add Key", rightColumnX, rightY - buttonHeight, buttonWidth, buttonHeight,
            () -> { if (callbacks != null) callbacks.onAddKeyframe(); });
        rightY -= (buttonHeight + buttonSpacing);

        // Add Snapshot (快照关键帧) - 蓝色标记
        addSnapshotButton = createButtonWithChinese("添加快照帧", "Add Snap", rightColumnX, rightY - buttonHeight, buttonWidth, buttonHeight,
            () -> { if (callbacks != null) callbacks.onAddSnapshot(); });
        rightY -= (buttonHeight + buttonSpacing);

        // Delete Keyframe (新增!)
        deleteKeyframeButton = createButtonWithChinese("删除帧", "Del Key", rightColumnX, rightY - buttonHeight, buttonWidth, buttonHeight,
            () -> { if (callbacks != null) callbacks.onDeleteKeyframe(); });
        rightY -= (buttonHeight + buttonSpacing);

        // Copy Keyframe (新增!)
        copyKeyframeButton = createButtonWithChinese("复制帧", "Copy Key", rightColumnX, rightY - buttonHeight, buttonWidth, buttonHeight,
            () -> { if (callbacks != null) callbacks.onCopyKeyframe(); });
        rightY -= (buttonHeight + buttonSpacing);

        // Paste Keyframe (新增!)
        pasteKeyframeButton = createButtonWithChinese("粘贴帧", "Paste Key", rightColumnX, rightY - buttonHeight, buttonWidth, buttonHeight,
            () -> { if (callbacks != null) callbacks.onPasteKeyframe(); });
        rightY -= (buttonHeight + buttonSpacing);

        // Undo
        undoButton = createButtonWithChinese("撤销", "Undo", rightColumnX, rightY - buttonHeight, buttonWidth, buttonHeight,
            () -> { if (callbacks != null) callbacks.onUndo(); });
        rightY -= (buttonHeight + buttonSpacing);

        // Redo
        redoButton = createButtonWithChinese("重做", "Redo", rightColumnX, rightY - buttonHeight, buttonWidth, buttonHeight,
            () -> { if (callbacks != null) callbacks.onRedo(); });
        rightY -= (buttonHeight + buttonSpacing);

        // Copy
        copyButton = createButtonWithChinese("复制", "Copy", rightColumnX, rightY - buttonHeight, buttonWidth, buttonHeight,
            () -> { if (callbacks != null) callbacks.onCopyBone(); });
        rightY -= (buttonHeight + buttonSpacing);

        // Paste
        pasteButton = createButtonWithChinese("粘贴", "Paste", rightColumnX, rightY - buttonHeight, buttonWidth, buttonHeight,
            () -> { if (callbacks != null) callbacks.onPasteBone(); });
        rightY -= (buttonHeight + buttonSpacing);

        // Paste Mirror
        pasteMirrorButton = createButtonWithChinese("镜像粘贴", "Paste Mirror", rightColumnX, rightY - buttonHeight, buttonWidth, buttonHeight,
            () -> { if (callbacks != null) callbacks.onPasteBoneMirrored(); });
        rightY -= (buttonHeight + buttonSpacing);

        // Grid Snap
        gridSnapButton = createButtonWithChinese("网格:关", "Grid: OFF", rightColumnX, rightY - buttonHeight, buttonWidth, buttonHeight,
            () -> {
                gridSnapEnabled = !gridSnapEnabled;
                if (ttfLoader != null) {
                    gridSnapButton.setText(gridSnapEnabled ? "网格:开" : "网格:关");
                } else {
                    gridSnapButton.setText(gridSnapEnabled ? "Grid: ON" : "Grid: OFF");
                }
                if (callbacks != null) callbacks.onGridSnapToggle(gridSnapEnabled);
            });
        rightY -= (buttonHeight + buttonSpacing);

        // Toggle Preview
        togglePreviewButton = createButtonWithChinese("预览:关", "Preview: OFF", rightColumnX, rightY - buttonHeight, buttonWidth, buttonHeight,
            () -> {
                previewEnabled = !previewEnabled;
                if (ttfLoader != null) {
                    togglePreviewButton.setText(previewEnabled ? "预览:开" : "预览:关");
                } else {
                    togglePreviewButton.setText(previewEnabled ? "Preview: ON" : "Preview: OFF");
                }
                if (callbacks != null) callbacks.onPreviewToggle(previewEnabled);
            });
        rightY -= (buttonHeight + buttonSpacing);

        // Toggle Billboard（新增：当前部件的2D/3D模式切换）
        toggleBillboardButton = createButtonWithChinese("2D模式", "2D Mode", rightColumnX, rightY - buttonHeight, buttonWidth, buttonHeight,
            () -> {
                billboardEnabled = !billboardEnabled;
                if (ttfLoader != null) {
                    toggleBillboardButton.setText(billboardEnabled ? "2D模式" : "3D模式");
                } else {
                    toggleBillboardButton.setText(billboardEnabled ? "2D Mode" : "3D Mode");
                }
                if (callbacks != null) callbacks.onBillboardToggle(billboardEnabled);
            });
        rightY -= (buttonHeight + buttonSpacing);

        // Texture Mode（新增：纹理模式切换 - 单贴图/多向贴图）
        textureModeButton = createButtonWithChinese("纹理:单向", "Tex: Single", rightColumnX, rightY - buttonHeight, buttonWidth, buttonHeight,
            () -> {
                multiTextureEnabled = !multiTextureEnabled;
                if (ttfLoader != null) {
                    textureModeButton.setText(multiTextureEnabled ? "纹理:多向" : "纹理:单向");
                } else {
                    textureModeButton.setText(multiTextureEnabled ? "Tex: Multi" : "Tex: Single");
                }
                if (callbacks != null) callbacks.onTextureModeToggle(multiTextureEnabled);
            });
        rightY -= (buttonHeight + buttonSpacing);

        // Gravity Direction（新增：重力方向切换按钮）
        gravityDirectionButton = createButtonWithChinese("重力:下", "Gravity: Down", rightColumnX, rightY - buttonHeight, buttonWidth, buttonHeight,
            () -> {
                if (callbacks != null) callbacks.onGravityDirectionChanged();
            });
        rightY -= (buttonHeight + buttonSpacing);

        // Camera Follow（新增：相机跟随按钮）
        cameraFollowButton = createButtonWithChinese("相机跟随", "Cam Follow", rightColumnX, rightY - buttonHeight, buttonWidth, buttonHeight,
            () -> {
                if (callbacks != null) callbacks.onCameraFollowClicked();
            });
        rightY -= (buttonHeight + buttonSpacing);

        // Swing Enable（新增：摇摆开关按钮）
        swingEnableButton = createButtonWithChinese("摇摆:关", "Swing: OFF", rightColumnX, rightY - buttonHeight, buttonWidth, buttonHeight,
            () -> {
                swingEnabled = !swingEnabled;
                if (ttfLoader != null) {
                    swingEnableButton.setText(swingEnabled ? "摇摆:开" : "摇摆:关");
                } else {
                    swingEnableButton.setText(swingEnabled ? "Swing: ON" : "Swing: OFF");
                }
                if (callbacks != null) callbacks.onSwingEnableToggle(swingEnabled);
            });
        rightY -= (buttonHeight + buttonSpacing);

        // Swing Axis（新增：摇摆轴按钮）
        swingAxisButton = createButtonWithChinese("轴:Z", "Axis: Z", rightColumnX, rightY - buttonHeight, buttonWidth, buttonHeight,
            () -> {
                if (callbacks != null) callbacks.onSwingAxisChanged();
            });
    }

    /**
     * 辅助方法：创建按钮并添加到场景
     */
    private Button createButton(String text, int x, int y, int width, int height, Runnable clickAction) {
        Button button = new Button(app, font, text, x, y, width, height);
        button.setClickListener(new Button.ButtonClickListener() {
            @Override
            public void onClick() {
                clickAction.run();
            }
        });
        rootNode.attachChild(button.getRootNode());
        return button;
    }

    /**
     * 辅助方法：创建按钮（支持中文）
     * 如果有TTF字体加载器，使用中文文本；否则使用英文文本
     */
    private Button createButtonWithChinese(String chineseText, String englishText, int x, int y, int width, int height, Runnable clickAction) {
        Button button;
        if (ttfLoader != null) {
            button = new Button(app, ttfLoader, chineseText, x, y, width, height);
        } else {
            button = new Button(app, font, englishText, x, y, width, height);
        }
        button.setClickListener(new Button.ButtonClickListener() {
            @Override
            public void onClick() {
                clickAction.run();
            }
        });
        rootNode.attachChild(button.getRootNode());
        return button;
    }

    public void update(float tpf) {
        addPartButton.update(tpf);
        hideModeButton.update(tpf);
        showAllButton.update(tpf);
        loadPuppetButton.update(tpf);
        addPuppetButton.update(tpf);
        savePuppetButton.update(tpf);
        loadTextureButton.update(tpf);
        setParentButton.update(tpf);
        addFreeBoneButton.update(tpf);
        clearParentButton.update(tpf);
        deletePartButton.update(tpf);
        transformModeButton.update(tpf);
        toggleBoneLinesButton.update(tpf);
        addKeyframeButton.update(tpf);
        addSnapshotButton.update(tpf);
        deleteKeyframeButton.update(tpf);  // 新增
        copyKeyframeButton.update(tpf);  // 新增
        pasteKeyframeButton.update(tpf);  // 新增
        undoButton.update(tpf);
        redoButton.update(tpf);
        copyButton.update(tpf);
        pasteButton.update(tpf);
        pasteMirrorButton.update(tpf);
        gridSnapButton.update(tpf);
        togglePreviewButton.update(tpf);
        playPauseButton.update(tpf);
        resetButton.update(tpf);
        gravityDirectionButton.update(tpf);  // 新增
        toggleBillboardButton.update(tpf);
        exportAnimButton.update(tpf);
        importAnimButton.update(tpf);
        textureModeButton.update(tpf);  // 新增
        cameraFollowButton.update(tpf);  // 新增
        swingEnableButton.update(tpf);  // 新增
        swingAxisButton.update(tpf);  // 新增
    }

    public boolean handleMouseClick(int mouseX, int mouseY) {
        if (addPartButton.handleMouseClick(mouseX, mouseY)) return true;
        if (hideModeButton.handleMouseClick(mouseX, mouseY)) return true;
        if (showAllButton.handleMouseClick(mouseX, mouseY)) return true;
        if (loadPuppetButton.handleMouseClick(mouseX, mouseY)) return true;
        if (addPuppetButton.handleMouseClick(mouseX, mouseY)) return true;
        if (savePuppetButton.handleMouseClick(mouseX, mouseY)) return true;
        if (exportAnimButton.handleMouseClick(mouseX, mouseY)) return true;
        if (importAnimButton.handleMouseClick(mouseX, mouseY)) return true;
        if (loadTextureButton.handleMouseClick(mouseX, mouseY)) return true;
        if (setParentButton.handleMouseClick(mouseX, mouseY)) return true;
        if (addFreeBoneButton.handleMouseClick(mouseX, mouseY)) return true;
        if (clearParentButton.handleMouseClick(mouseX, mouseY)) return true;
        if (deletePartButton.handleMouseClick(mouseX, mouseY)) return true;
        if (transformModeButton.handleMouseClick(mouseX, mouseY)) return true;
        if (toggleBoneLinesButton.handleMouseClick(mouseX, mouseY)) return true;
        if (addKeyframeButton.handleMouseClick(mouseX, mouseY)) return true;
        if (addSnapshotButton.handleMouseClick(mouseX, mouseY)) return true;
        if (deleteKeyframeButton.handleMouseClick(mouseX, mouseY)) return true;  // 新增
        if (copyKeyframeButton.handleMouseClick(mouseX, mouseY)) return true;  // 新增
        if (pasteKeyframeButton.handleMouseClick(mouseX, mouseY)) return true;  // 新增
        if (undoButton.handleMouseClick(mouseX, mouseY)) return true;
        if (redoButton.handleMouseClick(mouseX, mouseY)) return true;
        if (copyButton.handleMouseClick(mouseX, mouseY)) return true;
        if (pasteButton.handleMouseClick(mouseX, mouseY)) return true;
        if (pasteMirrorButton.handleMouseClick(mouseX, mouseY)) return true;
        if (gridSnapButton.handleMouseClick(mouseX, mouseY)) return true;
        if (togglePreviewButton.handleMouseClick(mouseX, mouseY)) return true;
        if (playPauseButton.handleMouseClick(mouseX, mouseY)) return true;
        if (resetButton.handleMouseClick(mouseX, mouseY)) return true;
        if (gravityDirectionButton.handleMouseClick(mouseX, mouseY)) return true;  // 新增
        if (toggleBillboardButton.handleMouseClick(mouseX, mouseY)) return true;
        if (textureModeButton.handleMouseClick(mouseX, mouseY)) return true;  // 新增：纹理模式按钮
        if (cameraFollowButton.handleMouseClick(mouseX, mouseY)) return true;  // 新增：相机跟随按钮
        if (swingEnableButton.handleMouseClick(mouseX, mouseY)) return true;  // 新增：摇摆开关按钮
        if (swingAxisButton.handleMouseClick(mouseX, mouseY)) return true;  // 新增：摇摆轴按钮
        return false;
    }

    public Node getRootNode() {
        return rootNode;
    }

    public void setCallbacks(ButtonCallbacks callbacks) {
        this.callbacks = callbacks;
    }

    public Button getSetParentButton() {
        return setParentButton;
    }

    public Button getGravityDirectionButton() {
        return gravityDirectionButton;
    }

    public boolean isBoneTransformMode() {
        return boneTransformMode;
    }

    public boolean isGridSnapEnabled() {
        return gridSnapEnabled;
    }

    /**
     * 设置激活的方向按钮（同时取消其他方向按钮的激活状态）
     * 支持6个方向：前后左右上下
     */

    public boolean isPlaying() {
        return isPlaying;
    }

    public void setPlaying(boolean playing) {
        this.isPlaying = playing;
        playPauseButton.setText(playing ? "Pause" : "Play");
    }

    /**
     * 更新Billboard按钮显示状态（当切换选中部件时调用）
     */
    public void updateBillboardButton(boolean enabled) {
        this.billboardEnabled = enabled;
        if (toggleBillboardButton != null) {
            toggleBillboardButton.setText(enabled ? "2D Mode" : "3D Mode");
        }
    }

    /**
     * 更新纹理模式按钮显示状态（当切换选中部件时调用）
     */
    public void updateTextureModeButton(boolean multiTextureEnabled) {
        this.multiTextureEnabled = multiTextureEnabled;
        if (textureModeButton != null) {
            if (ttfLoader != null) {
                textureModeButton.setText(multiTextureEnabled ? "纹理:多向" : "纹理:单向");
            } else {
                textureModeButton.setText(multiTextureEnabled ? "Tex: Multi" : "Tex: Single");
            }
        }
    }

    /**
     * 更新重力按钮文本（当切换选中自由骨骼时调用）
     */
    public void updateGravityButtonText(String text) {
        if (gravityDirectionButton != null) {
            gravityDirectionButton.setText(text);
        }
    }

    /**
     * 更新相机跟随按钮文本（显示当前X/Y值）
     */
    public void updateCameraFollowButtonText(float x, float y) {
        if (cameraFollowButton != null) {
            if (ttfLoader != null) {
                cameraFollowButton.setText(String.format("相机:%.2f,%.2f", x, y));
            } else {
                cameraFollowButton.setText(String.format("Cam:%.2f,%.2f", x, y));
            }
        }
    }

    /**
     * 更新摇摆轴按钮文本（当切换摇摆轴时调用）
     */
    public void updateSwingAxisButtonText(String axisName) {
        if (swingAxisButton != null) {
            if (ttfLoader != null) {
                swingAxisButton.setText("轴:" + axisName);
            } else {
                swingAxisButton.setText("Axis: " + axisName);
            }
        }
    }

    /**
     * 更新UI语言
     */
    public void updateLanguage() {
        LanguageManager langMgr = LanguageManager.getInstance();

        // 左列按钮
        if (addPartButton != null) {
            addPartButton.setText(langMgr.getText("add_part"));
        }
        if (hideModeButton != null) {
            hideModeButton.setText(hideModeEnabled ? langMgr.getText("hide_on") : langMgr.getText("hide_off"));
        }
        if (showAllButton != null) {
            showAllButton.setText(langMgr.getText("show_all"));
        }
        if (loadPuppetButton != null) {
            loadPuppetButton.setText(langMgr.getText("load_puppet"));
        }
        if (addPuppetButton != null) {
            addPuppetButton.setText(langMgr.getText("add_puppet"));
        }
        if (savePuppetButton != null) {
            savePuppetButton.setText(langMgr.getText("save_puppet"));
        }
        if (exportAnimButton != null) {
            exportAnimButton.setText(langMgr.getText("export_anim"));
        }
        if (importAnimButton != null) {
            importAnimButton.setText(langMgr.getText("import_anim"));
        }
        if (loadTextureButton != null) {
            loadTextureButton.setText(langMgr.getText("load_texture"));
        }
        if (setParentButton != null) {
            setParentButton.setText(langMgr.getText("set_parent"));
        }
        if (addFreeBoneButton != null) {
            addFreeBoneButton.setText(langMgr.getText("add_free_bone"));
        }
        if (clearParentButton != null) {
            clearParentButton.setText(langMgr.getText("clear_parent"));
        }
        if (deletePartButton != null) {
            deletePartButton.setText(langMgr.getText("delete_part"));
        }
        if (transformModeButton != null) {
            transformModeButton.setText(boneTransformMode ? langMgr.getText("transform_mode_bone") : langMgr.getText("transform_mode_part"));
        }
        if (toggleBoneLinesButton != null) {
            toggleBoneLinesButton.setText(boneLinesVisible ? langMgr.getText("bone_lines_on") : langMgr.getText("bone_lines_off"));
        }
        if (playPauseButton != null) {
            playPauseButton.setText(isPlaying ? langMgr.getText("pause") : langMgr.getText("play"));
        }
        if (resetButton != null) {
            resetButton.setText(langMgr.getText("reset_timeline"));
        }

        // 右列按钮
        if (addKeyframeButton != null) {
            addKeyframeButton.setText(langMgr.getText("add_keyframe"));
        }
        if (addSnapshotButton != null) {
            addSnapshotButton.setText(langMgr.getText("add_snapshot"));
        }
        if (deleteKeyframeButton != null) {
            deleteKeyframeButton.setText(langMgr.getText("delete_keyframe"));
        }
        if (undoButton != null) {
            undoButton.setText(langMgr.getText("undo"));
        }
        if (redoButton != null) {
            redoButton.setText(langMgr.getText("redo"));
        }
        if (copyButton != null) {
            copyButton.setText(langMgr.getText("copy"));
        }
        if (pasteButton != null) {
            pasteButton.setText(langMgr.getText("paste"));
        }
        if (pasteMirrorButton != null) {
            pasteMirrorButton.setText(langMgr.getText("paste_mirror"));
        }
        if (gridSnapButton != null) {
            gridSnapButton.setText(gridSnapEnabled ? langMgr.getText("grid_on") : langMgr.getText("grid_off"));
        }
        if (togglePreviewButton != null) {
            togglePreviewButton.setText(previewEnabled ? langMgr.getText("preview_on") : langMgr.getText("preview_off"));
        }
        if (toggleBillboardButton != null) {
            toggleBillboardButton.setText(billboardEnabled ? langMgr.getText("billboard_2d") : langMgr.getText("billboard_3d"));
        }
        if (textureModeButton != null) {
            textureModeButton.setText(multiTextureEnabled ? langMgr.getText("texture_multi") : langMgr.getText("texture_single"));
        }
        if (gravityDirectionButton != null) {
            gravityDirectionButton.setText(langMgr.getText("gravity_down"));
        }
    }
}
