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
import com.Hecate.puppet.editor.core.EditorBone;
import com.Hecate.puppet.editor.core.EditorPuppetPartRenderer;
import com.Hecate.puppet.editor.core.EditorPuppetRenderer;
import com.Hecate.puppet.editor.animation.EditorAnimationPlayer;
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
    private EditorBone currentBone;
    private EditorPuppetPartRenderer currentPartRenderer;
    private CommandManager commandManager;
    private EditorPuppetRenderer puppetRenderer;
    private EditorAnimationPlayer animationPlayer;

    // 多选骨骼集合的引用（从PuppetEditorApp传入）
    private java.util.Set<EditorBone> selectedBones;

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
    private Button boneTypeButton;
    private Button gravityDirButton;
    private Button billboardModeButton;
    private Button textureModeButton;
    private Button swingEnableButton;
    private Button swingAxisButton;

    // 滑条列（右边）
    private Slider widthSlider;
    private Slider heightSlider;
    private Slider prioritySlider;
    private Slider positionXSlider;
    private Slider positionYSlider;
    private Slider positionZSlider;
    private Slider rotationXSlider;
    private Slider rotationYSlider;
    private Slider rotationZSlider;
    private Slider gridSizeSlider;
    private Slider contentCenterXSlider;
    private Slider contentCenterYSlider;
    private Slider freedomValueSlider;
    private Button cameraFollowButton;
    private Slider textureRotationSlider;

    // 摇摆系统滑条
    private Slider swingAmplitudeSlider;
    private Slider swingFrequencySlider;
    private Slider swingPhaseSlider;

    // 纹理预览
    private TexturePreviewPanel texturePreviewPanel;
    private EnlargedTexturePreviewPanel enlargedPreviewPanel;
    private Button openEnlargedPreviewButton;
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
        texturePreviewPanel = new TexturePreviewPanel(app, font, sliderX, previewY, sliderColumnWidth, 250);
        rootNode.attachChild(texturePreviewPanel.getRootNode());

        // 在TexturePreviewPanel下方添加打开总览按钮
        int buttonYPos = previewY - 10;
        openEnlargedPreviewButton = new Button(app, font, "Open UV Editor", sliderX, buttonYPos - 30, sliderColumnWidth, 30);
        openEnlargedPreviewButton.setClickListener(() -> {
            if (enlargedPreviewPanel != null && currentPartRenderer != null) {
                // 同步当前的纹理和UV数据到放大预览
                enlargedPreviewPanel.setTexture(currentPartRenderer.getTexture());
                enlargedPreviewPanel.setUV(
                    currentPartRenderer.getUvOffsetX(),
                    currentPartRenderer.getUvOffsetY(),
                    currentPartRenderer.getUvScaleX(),
                    currentPartRenderer.getUvScaleY()
                );
                // 设置部件宽高比，让UV框初始形状匹配部件
                enlargedPreviewPanel.setPartDimensions(
                    currentPartRenderer.getWidth(),
                    currentPartRenderer.getHeight()
                );
                enlargedPreviewPanel.show();
            }
        });
        rootNode.attachChild(openEnlargedPreviewButton.getRootNode());

        // 创建EnlargedTexturePreviewPanel（覆盖全屏）
        enlargedPreviewPanel = new EnlargedTexturePreviewPanel(app, font);
        rootNode.attachChild(enlargedPreviewPanel.getRootNode());

        // 设置UV变化监听器
        enlargedPreviewPanel.setChangeListener((uvOffsetX, uvOffsetY, uvScaleX, uvScaleY) -> {
            if (currentPartRenderer != null) {
                currentPartRenderer.setUV(uvOffsetX, uvOffsetY, uvScaleX, uvScaleY);
                // 同时更新小预览
                texturePreviewPanel.setUV(uvOffsetX, uvOffsetY, uvScaleX, uvScaleY);
            }
        });

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
            transformModeButton.setText(boneTransformMode ? "Transform: EditorBone" : "Transform: Part");
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

        // Grid Snap 按钮（控制UV选框吸附）
        gridSnapButton = new Button(app, font, "Snap: OFF", startX, currentY - buttonHeight, buttonColumnWidth, buttonHeight);
        gridSnapButton.setClickListener(() -> {
            gridSnapEnabled = !gridSnapEnabled;
            gridSnapButton.setText(gridSnapEnabled ? "Snap: ON" : "Snap: OFF");
            if (texturePreviewPanel != null) {
                texturePreviewPanel.setSnapToGrid(gridSnapEnabled);
            }
            if (enlargedPreviewPanel != null) {
                enlargedPreviewPanel.setSnapToGrid(gridSnapEnabled);
            }
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
        currentY -= (buttonHeight + buttonSpacing);

        // EditorBone Type 按钮（切换连接骨骼/自由骨骼）
        boneTypeButton = new Button(app, font, "Type: Connected", startX, currentY - buttonHeight, buttonColumnWidth, buttonHeight);
        boneTypeButton.setClickListener(() -> {
            if (currentBone != null) {
                // 切换骨骼类型
                if (currentBone.getBoneType() == EditorBone.BoneType.CONNECTED) {
                    currentBone.setBoneType(EditorBone.BoneType.FREE);
                    boneTypeButton.setText("Type: Free");
                    // 创建物理系统
                    if (puppetRenderer != null) {
                        puppetRenderer.refreshBoneConnections(); // 刷新连接线颜色
                    }
                } else {
                    currentBone.setBoneType(EditorBone.BoneType.CONNECTED);
                    boneTypeButton.setText("Type: Connected");
                    // 移除物理系统
                    if (puppetRenderer != null) {
                        puppetRenderer.refreshBoneConnections(); // 刷新连接线颜色
                    }
                }
                // 更新UI显示
                updateFreeBoneControlsVisibility();
            }
        });
        rootNode.attachChild(boneTypeButton.getRootNode());
        currentY -= (buttonHeight + buttonSpacing);

        // Gravity Direction 按钮（循环切换重力方向）
        gravityDirButton = new Button(app, font, "Gravity: Down", startX, currentY - buttonHeight, buttonColumnWidth, buttonHeight);
        gravityDirButton.setClickListener(() -> {
            if (currentBone != null && currentBone.isFreeBone()) {
                // 循环切换重力方向：Down -> Up -> Left -> Right -> Front -> Back -> Down
                EditorBone.GravityDirection current = currentBone.getGravityDirection();
                EditorBone.GravityDirection next;
                switch (current) {
                    case DOWN:
                        next = EditorBone.GravityDirection.UP;
                        break;
                    case UP:
                        next = EditorBone.GravityDirection.LEFT;
                        break;
                    case LEFT:
                        next = EditorBone.GravityDirection.RIGHT;
                        break;
                    case RIGHT:
                        next = EditorBone.GravityDirection.FRONT;
                        break;
                    case FRONT:
                        next = EditorBone.GravityDirection.BACK;
                        break;
                    case BACK:
                    case CUSTOM:
                    default:
                        next = EditorBone.GravityDirection.DOWN;
                        break;
                }
                currentBone.setGravityDirection(next);
                gravityDirButton.setText("Gravity: " + next.getDisplayName());
            }
        });
        rootNode.attachChild(gravityDirButton.getRootNode());
        currentY -= (buttonHeight + buttonSpacing);

        // Billboard Mode 按钮（切换2D/3D模式）
        billboardModeButton = new Button(app, font, "Mode: 2D", startX, currentY - buttonHeight, buttonColumnWidth, buttonHeight);
        billboardModeButton.setClickListener(() -> {
            if (currentBone != null && currentPartRenderer != null) {
                boolean currentMode = currentPartRenderer.isBillboardEnabled();
                currentPartRenderer.setBillboardEnabled(!currentMode);
                billboardModeButton.setText(!currentMode ? "Mode: 2D" : "Mode: 3D");
            }
        });
        rootNode.attachChild(billboardModeButton.getRootNode());
        currentY -= (buttonHeight + buttonSpacing);

        // Camera Follow 按钮（设置相机跟随自由度）
        cameraFollowButton = new Button(app, font, "Cam Follow", startX, currentY - buttonHeight, buttonColumnWidth, buttonHeight);
        cameraFollowButton.setClickListener(() -> {
            if (currentBone != null) {
                // 打开输入对话框
                CameraFollowDialog dialog = new CameraFollowDialog(
                    null,
                    currentBone.getCameraFollowFreedomX(),
                    currentBone.getCameraFollowFreedomY()
                );
                dialog.setVisible(true);

                // 如果用户确认了输入
                if (dialog.isConfirmed()) {
                    currentBone.setCameraFollowFreedomX(dialog.getCameraFollowX());
                    currentBone.setCameraFollowFreedomY(dialog.getCameraFollowY());
                    // 更新按钮文本显示当前值
                    updateCameraFollowButtonText();
                }
            }
        });
        rootNode.attachChild(cameraFollowButton.getRootNode());
        currentY -= (buttonHeight + buttonSpacing);

        // Texture Mode 按钮（切换单贴图/多贴图模式）
        textureModeButton = new Button(app, font, "Tex: Multi", startX, currentY - buttonHeight, buttonColumnWidth, buttonHeight);
        textureModeButton.setClickListener(() -> {
            if (currentBone != null) {
                boolean currentMode = currentBone.isMultiDirectionTextureEnabled();
                currentBone.setMultiDirectionTextureEnabled(!currentMode);
                textureModeButton.setText(!currentMode ? "Tex: Multi" : "Tex: Single");
                // 刷新贴图显示
                if (currentPartRenderer != null) {
                    currentPartRenderer.updateTextureFromBone();
                }
            }
        });
        rootNode.attachChild(textureModeButton.getRootNode());
        currentY -= (buttonHeight + buttonSpacing);

        // Swing Enable 按钮（启用/禁用摇摆系统）
        swingEnableButton = new Button(app, font, "Swing: OFF", startX, currentY - buttonHeight, buttonColumnWidth, buttonHeight);
        swingEnableButton.setClickListener(() -> {
            if (currentBone != null) {
                // 获取当前方向的摇摆开关状态
                boolean currentEnabled = currentBone.getCurrentDirectionSwingEnabled();
                // 设置当前方向的摇摆开关
                currentBone.setDirectionSwingEnabled(currentBone.getCurrentDirection(), !currentEnabled);
                swingEnableButton.setText(!currentEnabled ? "Swing: ON" : "Swing: OFF");
            }
        });
        rootNode.attachChild(swingEnableButton.getRootNode());
        currentY -= (buttonHeight + buttonSpacing);

        // Swing Axis 按钮（切换摇摆轴方向：X/Y/Z）
        swingAxisButton = new Button(app, font, "Axis: Z", startX, currentY - buttonHeight, buttonColumnWidth, buttonHeight);
        swingAxisButton.setClickListener(() -> {
            if (currentBone != null) {
                Vector3f currentAxis = currentBone.getSwingAxis();
                Vector3f newAxis;
                String axisName;

                // 循环切换：Z轴（左右摆） -> X轴（前后摆） -> Y轴（扭转） -> Z轴
                if (Math.abs(currentAxis.z - 1f) < 0.01f) {
                    // 当前是Z轴，切换到X轴
                    newAxis = new Vector3f(1, 0, 0);
                    axisName = "X";
                } else if (Math.abs(currentAxis.x - 1f) < 0.01f) {
                    // 当前是X轴，切换到Y轴
                    newAxis = new Vector3f(0, 1, 0);
                    axisName = "Y";
                } else {
                    // 当前是Y轴或其他，切换到Z轴
                    newAxis = new Vector3f(0, 0, 1);
                    axisName = "Z";
                }

                currentBone.setSwingAxis(newAxis);
                swingAxisButton.setText("Axis: " + axisName);
            }
        });
        rootNode.attachChild(swingAxisButton.getRootNode());
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
                currentPartRenderer.setWidth(value);
            }
        });
        rootNode.attachChild(widthSlider.getRootNode());
        currentY -= sliderSpacing;

        // Height 滑条
        heightSlider = new Slider(app, font, "Height", 10f, 500f, 100f, startX, currentY - 30);
        heightSlider.setChangeListener(value -> {
            if (currentPartRenderer != null) {
                currentPartRenderer.setHeight(value);
            }
        });
        rootNode.attachChild(heightSlider.getRootNode());
        currentY -= sliderSpacing;

        // Priority 滑条
        prioritySlider = new Slider(app, font, "Priority", -100f, 100f, 0f, startX, currentY - 30);
        prioritySlider.setChangeListener(value -> {
            if (currentBone != null) {
                int priorityValue = (int)value;
                // 同时更新全局优先度和所有方向的优先度
                currentBone.setPriority(priorityValue);
                // 更新所有方向的优先度，确保不会被后备机制覆盖
                for (String dir : new String[]{"front", "back", "left", "right", "up", "down"}) {
                    currentBone.setDirectionPriority(dir, priorityValue);
                }
                // 立即重新排序，让用户立即看到效果
                if (puppetRenderer != null) {
                    puppetRenderer.updateRenderOrder();
                }
            }
        });
        rootNode.attachChild(prioritySlider.getRootNode());
        currentY -= sliderSpacing;

        // Content Center X 滑条（贴图内容中心X偏移，范围 -0.5 到 +0.5）
        contentCenterXSlider = new Slider(app, font, "Center X", -0.5f, 0.5f, 0f, startX, currentY - 30);
        contentCenterXSlider.setChangeListener(value -> {
            if (currentBone != null) {
                // 获取当前内容中心偏移，如果没有则使用[0, 0]
                float[] currentCenter = currentBone.getCurrentDirectionContentCenter();
                float centerY = (currentCenter != null) ? currentCenter[1] : 0f;
                // 更新X值，保持Y值不变
                currentBone.setDirectionContentCenter(currentBone.getCurrentDirection(), value, centerY);
            }
        });
        rootNode.attachChild(contentCenterXSlider.getRootNode());
        currentY -= sliderSpacing;

        // Content Center Y 滑条（贴图内容中心Y偏移，范围 -0.5 到 +0.5）
        contentCenterYSlider = new Slider(app, font, "Center Y", -0.5f, 0.5f, 0f, startX, currentY - 30);
        contentCenterYSlider.setChangeListener(value -> {
            if (currentBone != null) {
                // 获取当前内容中心偏移，如果没有则使用[0, 0]
                float[] currentCenter = currentBone.getCurrentDirectionContentCenter();
                float centerX = (currentCenter != null) ? currentCenter[0] : 0f;
                // 更新Y值，保持X值不变
                currentBone.setDirectionContentCenter(currentBone.getCurrentDirection(), centerX, value);
            }
        });
        rootNode.attachChild(contentCenterYSlider.getRootNode());
        currentY -= sliderSpacing;

        // Position X 滑条
        positionXSlider = new Slider(app, font, "Pos X", -500f, 500f, 0f, startX, currentY - 30);
        positionXSlider.setChangeListener(value -> {
            // 进入编辑模式，防止动画覆盖手动编辑
            if (animationPlayer != null) {
                animationPlayer.setEditMode(true);
            }
            // 应用到所有选中的骨骼
            if (selectedBones != null && !selectedBones.isEmpty()) {
                for (EditorBone bone : selectedBones) {
                    Vector3f pos = bone.getLocalPosition();
                    bone.setLocalPosition(new Vector3f(value, pos.y, pos.z));
                }
            } else if (currentBone != null) {
                Vector3f pos = currentBone.getLocalPosition();
                currentBone.setLocalPosition(new Vector3f(value, pos.y, pos.z));
            }
        });
        rootNode.attachChild(positionXSlider.getRootNode());
        currentY -= sliderSpacing;

        // Position Y 滑条
        positionYSlider = new Slider(app, font, "Pos Y", -500f, 500f, 0f, startX, currentY - 30);
        positionYSlider.setChangeListener(value -> {
            // 进入编辑模式，防止动画覆盖手动编辑
            if (animationPlayer != null) {
                animationPlayer.setEditMode(true);
            }
            // 应用到所有选中的骨骼
            if (selectedBones != null && !selectedBones.isEmpty()) {
                for (EditorBone bone : selectedBones) {
                    Vector3f pos = bone.getLocalPosition();
                    bone.setLocalPosition(new Vector3f(pos.x, value, pos.z));
                }
            } else if (currentBone != null) {
                Vector3f pos = currentBone.getLocalPosition();
                currentBone.setLocalPosition(new Vector3f(pos.x, value, pos.z));
            }
        });
        rootNode.attachChild(positionYSlider.getRootNode());
        currentY -= sliderSpacing;

        // Position Z 滑条
        positionZSlider = new Slider(app, font, "Pos Z", -500f, 500f, 0f, startX, currentY - 30);
        positionZSlider.setChangeListener(value -> {
            // 进入编辑模式，防止动画覆盖手动编辑
            if (animationPlayer != null) {
                animationPlayer.setEditMode(true);
            }
            // 应用到所有选中的骨骼
            if (selectedBones != null && !selectedBones.isEmpty()) {
                for (EditorBone bone : selectedBones) {
                    Vector3f pos = bone.getLocalPosition();
                    bone.setLocalPosition(new Vector3f(pos.x, pos.y, value));
                }
            } else if (currentBone != null) {
                Vector3f pos = currentBone.getLocalPosition();
                currentBone.setLocalPosition(new Vector3f(pos.x, pos.y, value));
            }
        });
        rootNode.attachChild(positionZSlider.getRootNode());
        currentY -= sliderSpacing;

        // Rotation X 滑条
        rotationXSlider = new Slider(app, font, "Rot X", -180f, 180f, 0f, startX, currentY - 30);
        rotationXSlider.setChangeListener(value -> {
            // 应用到所有选中的骨骼
            if (selectedBones != null && !selectedBones.isEmpty()) {
                for (EditorBone bone : selectedBones) {
                    Quaternion rot = bone.getLocalRotation();
                    float[] angles = rot.toAngles(new float[3]);
                    bone.setLocalRotation(new Quaternion().fromAngles(
                        (float)Math.toRadians(value), angles[1], angles[2]
                    ));
                }
            } else if (currentBone != null) {
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
            // 应用到所有选中的骨骼
            if (selectedBones != null && !selectedBones.isEmpty()) {
                for (EditorBone bone : selectedBones) {
                    Quaternion rot = bone.getLocalRotation();
                    float[] angles = rot.toAngles(new float[3]);
                    bone.setLocalRotation(new Quaternion().fromAngles(
                        angles[0], angles[1], (float)Math.toRadians(value)
                    ));
                }
            } else if (currentBone != null) {
                Quaternion rot = currentBone.getLocalRotation();
                float[] angles = rot.toAngles(new float[3]);
                currentBone.setLocalRotation(new Quaternion().fromAngles(
                    angles[0], angles[1], (float)Math.toRadians(value)
                ));
            }
        });
        rootNode.attachChild(rotationZSlider.getRootNode());
        currentY -= sliderSpacing;

        // Grid Size 滑条（控制网格大小，范围 8-128 像素）
        // 这个滑条可以在小预览面板使用，放大窗口有自己的滑条
        gridSizeSlider = new Slider(app, font, "Grid Size", 8f, 128f, 32f, startX, currentY - 30);
        gridSizeSlider.setChangeListener(value -> {
            gridSize = value;
            // 只更新小预览面板的网格大小
            if (texturePreviewPanel != null) {
                texturePreviewPanel.setGridSize(value);
            }
        });
        rootNode.attachChild(gridSizeSlider.getRootNode());
        currentY -= sliderSpacing;

        // Freedom Value 滑条（自由骨骼的自由度，0-1范围）
        freedomValueSlider = new Slider(app, font, "Freedom", 0f, 1f, 0.5f, startX, currentY - 30);
        freedomValueSlider.setChangeListener(value -> {
            if (currentBone != null && currentBone.isFreeBone()) {
                // 设置当前方向的自由度
                currentBone.setDirectionFreedomValue(currentBone.getCurrentDirection(), value);
            }
        });
        rootNode.attachChild(freedomValueSlider.getRootNode());
        currentY -= sliderSpacing;

        // Texture Rotation 滑条（贴图旋转，0-360度）
        textureRotationSlider = new Slider(app, font, "Tex Rot", 0f, 360f, 0f, startX, currentY - 30);
        textureRotationSlider.setChangeListener(value -> {
            if (currentBone != null) {
                // 设置当前方向的贴图旋转角度
                currentBone.setDirectionTextureRotation(currentBone.getCurrentDirection(), value);
                // 刷新贴图显示
                if (currentPartRenderer != null) {
                    // 清除动画旋转标志，让UI滑块重新获得控制权
                    currentPartRenderer.setUseAnimationRotation(false);
                    currentPartRenderer.updateTextureFromBone();
                }
            }
        });
        rootNode.attachChild(textureRotationSlider.getRootNode());
        currentY -= sliderSpacing;

        // ==================== 摇摆系统控制 ====================

        // Swing Amplitude 滑条（摇摆幅度，0-90度）
        swingAmplitudeSlider = new Slider(app, font, "Swing Amp", 0f, 90f, 15f, startX, currentY - 30);
        swingAmplitudeSlider.setChangeListener(value -> {
            if (currentBone != null) {
                // 设置当前方向的摇摆幅度
                currentBone.setDirectionSwingAmplitude(currentBone.getCurrentDirection(), value);
            }
        });
        rootNode.attachChild(swingAmplitudeSlider.getRootNode());
        currentY -= sliderSpacing;

        // Swing Frequency 滑条（摇摆频率，0-5Hz）
        swingFrequencySlider = new Slider(app, font, "Swing Freq", 0f, 5f, 0.5f, startX, currentY - 30);
        swingFrequencySlider.setChangeListener(value -> {
            if (currentBone != null) {
                currentBone.setSwingFrequency(value);
            }
        });
        rootNode.attachChild(swingFrequencySlider.getRootNode());
        currentY -= sliderSpacing;

        // Swing Phase Offset 滑条（摇摆相位偏移，0-2π）
        swingPhaseSlider = new Slider(app, font, "Swing Phase", 0f, (float)(2 * Math.PI), 0f, startX, currentY - 30);
        swingPhaseSlider.setChangeListener(value -> {
            if (currentBone != null) {
                currentBone.setSwingPhaseOffset(value);
            }
        });
        rootNode.attachChild(swingPhaseSlider.getRootNode());
    }

    /**
     * 设置当前骨骼
     */
    public void setBone(EditorBone bone, EditorPuppetPartRenderer partRenderer) {
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

        // 显示当前方向的优先度，而不是全局优先度
        prioritySlider.setValue(currentBone.getCurrentDirectionPriority());

        Vector3f pos = currentBone.getLocalPosition();
        positionXSlider.setValue(pos.x);
        positionYSlider.setValue(pos.y);
        positionZSlider.setValue(pos.z);

        Quaternion rot = currentBone.getLocalRotation();
        float[] angles = rot.toAngles(new float[3]);
        rotationXSlider.setValue((float)Math.toDegrees(angles[0]));
        rotationZSlider.setValue((float)Math.toDegrees(angles[2]));

        // 更新内容中心偏移滑条
        float[] contentCenter = currentBone.getCurrentDirectionContentCenter();
        if (contentCenter != null) {
            contentCenterXSlider.setValue(contentCenter[0]);
            contentCenterYSlider.setValue(contentCenter[1]);
        } else {
            contentCenterXSlider.setValue(0f);
            contentCenterYSlider.setValue(0f);
        }

        // 更新骨骼类型按钮
        if (currentBone.isFreeBone()) {
            boneTypeButton.setText("Type: Free");
        } else {
            boneTypeButton.setText("Type: Connected");
        }

        // 更新重力方向按钮
        gravityDirButton.setText("Gravity: " + currentBone.getGravityDirection().getDisplayName());

        // 更新Billboard模式按钮
        if (currentPartRenderer != null) {
            billboardModeButton.setText(currentPartRenderer.isBillboardEnabled() ? "Mode: 2D" : "Mode: 3D");
        }

        // 更新贴图模式按钮
        textureModeButton.setText(currentBone.isMultiDirectionTextureEnabled() ? "Tex: Multi" : "Tex: Single");

        // 更新摇摆系统按钮（使用当前方向的值）
        swingEnableButton.setText(currentBone.getCurrentDirectionSwingEnabled() ? "Swing: ON" : "Swing: OFF");

        // 更新摇摆轴按钮
        Vector3f currentAxis = currentBone.getSwingAxis();
        String axisName;
        if (Math.abs(currentAxis.z - 1f) < 0.01f) {
            axisName = "Z";
        } else if (Math.abs(currentAxis.x - 1f) < 0.01f) {
            axisName = "X";
        } else {
            axisName = "Y";
        }
        swingAxisButton.setText("Axis: " + axisName);

        // 更新自由度滑条（使用当前方向的值）
        freedomValueSlider.setValue(currentBone.getCurrentDirectionFreedomValue());

        // 更新相机跟随按钮文本
        updateCameraFollowButtonText();

        // 更新贴图旋转滑条
        Float textureRotation = currentBone.getDirectionTextureRotation(currentBone.getCurrentDirection());
        textureRotationSlider.setValue(textureRotation != null ? textureRotation : 0f);

        // 更新摇摆系统滑条（使用当前方向的值）
        swingAmplitudeSlider.setValue(currentBone.getCurrentDirectionSwingAmplitude());
        swingFrequencySlider.setValue(currentBone.getSwingFrequency());
        swingPhaseSlider.setValue(currentBone.getSwingPhaseOffset());

        // 更新自由骨骼控件的可见性
        updateFreeBoneControlsVisibility();

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
     * 更新自由骨骼控件的可见性
     * 只有当前骨骼是自由骨骼时，才显示重力方向和自由度控件
     */
    private void updateFreeBoneControlsVisibility() {
        if (currentBone != null && currentBone.isFreeBone()) {
            // 显示自由骨骼控件
            gravityDirButton.getRootNode().setCullHint(com.jme3.scene.Spatial.CullHint.Never);
            freedomValueSlider.getRootNode().setCullHint(com.jme3.scene.Spatial.CullHint.Never);
        } else {
            // 隐藏自由骨骼控件
            gravityDirButton.getRootNode().setCullHint(com.jme3.scene.Spatial.CullHint.Always);
            freedomValueSlider.getRootNode().setCullHint(com.jme3.scene.Spatial.CullHint.Always);
        }
    }

    /**
     * 更新相机跟随按钮文本
     */
    private void updateCameraFollowButtonText() {
        if (currentBone != null && cameraFollowButton != null) {
            float x = currentBone.getCameraFollowFreedomX();
            float y = currentBone.getCameraFollowFreedomY();
            cameraFollowButton.setText(String.format("Cam: %.2f,%.2f", x, y));
        }
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
        boneTypeButton.update(tpf);
        gravityDirButton.update(tpf);
        billboardModeButton.update(tpf);
        cameraFollowButton.update(tpf);
        textureModeButton.update(tpf);
        swingEnableButton.update(tpf);
        swingAxisButton.update(tpf);

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
        // 优先检查EnlargedPreviewPanel（如果可见）
        if (enlargedPreviewPanel != null && enlargedPreviewPanel.isVisible()) {
            return enlargedPreviewPanel.handleMousePress(mouseX, mouseY);
        }

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
        if (openEnlargedPreviewButton.handleMouseClick(mouseX, mouseY)) return true;
        if (boneTypeButton.handleMouseClick(mouseX, mouseY)) return true;
        if (gravityDirButton.handleMouseClick(mouseX, mouseY)) return true;
        if (billboardModeButton.handleMouseClick(mouseX, mouseY)) return true;
        if (cameraFollowButton.handleMouseClick(mouseX, mouseY)) return true;
        if (textureModeButton.handleMouseClick(mouseX, mouseY)) return true;
        if (swingEnableButton.handleMouseClick(mouseX, mouseY)) return true;
        if (swingAxisButton.handleMouseClick(mouseX, mouseY)) return true;

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
        if (swingAmplitudeSlider.handleMouseClick(mouseX, mouseY)) return true;
        if (swingFrequencySlider.handleMouseClick(mouseX, mouseY)) return true;
        if (swingPhaseSlider.handleMouseClick(mouseX, mouseY)) return true;

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
        // 优先处理EnlargedPreviewPanel（如果可见）
        if (enlargedPreviewPanel != null && enlargedPreviewPanel.isVisible()) {
            enlargedPreviewPanel.handleMouseDrag(mouseX, mouseY);
            return;
        }

        widthSlider.handleMouseDrag(mouseX, mouseY);
        heightSlider.handleMouseDrag(mouseX, mouseY);
        prioritySlider.handleMouseDrag(mouseX, mouseY);
        positionXSlider.handleMouseDrag(mouseX, mouseY);
        positionYSlider.handleMouseDrag(mouseX, mouseY);
        positionZSlider.handleMouseDrag(mouseX, mouseY);
        rotationXSlider.handleMouseDrag(mouseX, mouseY);
        rotationZSlider.handleMouseDrag(mouseX, mouseY);
        gridSizeSlider.handleMouseDrag(mouseX, mouseY);
        swingAmplitudeSlider.handleMouseDrag(mouseX, mouseY);
        swingFrequencySlider.handleMouseDrag(mouseX, mouseY);
        swingPhaseSlider.handleMouseDrag(mouseX, mouseY);

        // 处理纹理预览拖动
        if (texturePreviewPanel != null) {
            texturePreviewPanel.handleMouseDrag(mouseX, mouseY);
        }
    }

    /**
     * 处理鼠标释放
     */
    public void handleMouseRelease() {
        // 优先处理EnlargedPreviewPanel（如果可见）
        if (enlargedPreviewPanel != null && enlargedPreviewPanel.isVisible()) {
            enlargedPreviewPanel.handleMouseRelease(0, 0); // EnlargedPreviewPanel需要坐标参数
            return;
        }

        widthSlider.handleMouseRelease();
        heightSlider.handleMouseRelease();
        prioritySlider.handleMouseRelease();
        positionXSlider.handleMouseRelease();
        positionYSlider.handleMouseRelease();
        positionZSlider.handleMouseRelease();
        rotationXSlider.handleMouseRelease();
        rotationZSlider.handleMouseRelease();
        gridSizeSlider.handleMouseRelease();
        swingAmplitudeSlider.handleMouseRelease();
        swingFrequencySlider.handleMouseRelease();
        swingPhaseSlider.handleMouseRelease();

        // 处理纹理预览释放
        if (texturePreviewPanel != null) {
            texturePreviewPanel.handleMouseRelease(0, 0);
        }
    }

    // ========== Getters and Setters ==========

    public Node getRootNode() {
        return rootNode;
    }

    public EditorBone getCurrentBone() {
        return currentBone;
    }

    public EditorPuppetPartRenderer getCurrentPartRenderer() {
        return currentPartRenderer;
    }

    public void setCallbacks(PanelCallbacks callbacks) {
        this.callbacks = callbacks;
    }

    public void setCommandManager(CommandManager commandManager) {
        this.commandManager = commandManager;
    }

    public void setPuppetRenderer(EditorPuppetRenderer puppetRenderer) {
        this.puppetRenderer = puppetRenderer;
    }

    public void setAnimationPlayer(EditorAnimationPlayer animationPlayer) {
        this.animationPlayer = animationPlayer;
    }

    public void setSelectedBones(java.util.Set<EditorBone> selectedBones) {
        this.selectedBones = selectedBones;
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
