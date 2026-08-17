package com.Hecate.puppet.editor;

import com.jme3.app.SimpleApplication;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Node;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Quad;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.Hecate.puppet.editor.core.EditorSkeleton;
import com.Hecate.puppet.editor.core.EditorBone;
import com.Hecate.puppet.editor.core.EditorPuppetPartRenderer;
import java.io.File;

/**
 * 木偶编辑器UI主界面
 * 管理Timeline、Inspector等面板
 */
public class PuppetEditorUI {

    private final SimpleApplication app;
    private final Node guiNode;
    private BitmapFont guiFont;
    private TTFontLoader ttfLoader; // TTF字体加载器

    // UI组件
    private Node editorRootNode;
    private ButtonColumnPanel buttonColumnPanel;
    private AnimationLayerPanel animationLayerPanel;  // 动画层管理面板
    private GroupControlPanel groupControlPanel;  // 骨骼分组控制面板
    private SliderColumnPanel sliderColumnPanel;
    private PartListPanel partListPanel;  // 部件列表面板
    private TimelinePanel timelinePanel;  // 时间轴面板
    private BitmapText currentDirectionText;
    private BitmapText keyframeTypeText;  // 关键帧类型显示

    // 编辑器状态
    private EditorSkeleton currentSkeleton;
    private EditorBone selectedBone;
    private EditorPuppetPartRenderer currentPartRenderer;
    private float currentTime = 0f;
    private boolean visible = false;

    // 布局参数
    private int screenWidth;
    private int screenHeight;
    private final int buttonColumnWidth = 250;  // 左侧按钮列宽度
    private final int rightColumnWidth = 280;  // 右侧列宽度（层级+属性）
    private final int topBarHeight = 70;  // 顶栏高度（增加以容纳按钮间距）
    private final int timelineHeight = 120;  // 底部时间轴高度

    // 顶栏按钮
    private Button backButton;
    private Button exitButton;
    private Button languageButton;  // 语言切换按钮

    public PuppetEditorUI(SimpleApplication app) {
        this.app = app;
        this.guiNode = app.getGuiNode();

        // 尝试加载TTF字体
        // 优先级1: 从resources加载自定义字体
        String[] resourceFonts = {
            "Interface/Fonts/ZLabsBitmap_12px_CN（简体中文）.ttf",  // ZLabs像素字体
            "Fonts/CustomFont.ttf",
            "Fonts/ChineseFont.ttf"
        };

        for (String resourcePath : resourceFonts) {
            this.ttfLoader = TTFontLoader.loadFontFromResource(app.getAssetManager(), resourcePath, 16f);
            if (this.ttfLoader != null) {
                break;
            }
        }

        // 优先级2: 从系统字体目录加载（Windows）
        if (this.ttfLoader == null) {
            String[] systemFontPaths = {
                "C:/Windows/Fonts/msyh.ttc",      // 微软雅黑
                "C:/Windows/Fonts/simhei.ttf",    // 黑体
                "C:/Windows/Fonts/simsun.ttc"     // 宋体
            };

            for (String fontPath : systemFontPaths) {
                File fontFile = new File(fontPath);
                if (fontFile.exists()) {
                    this.ttfLoader = TTFontLoader.loadFont(app.getAssetManager(), fontPath, 16f);
                    if (this.ttfLoader != null) {
                        break;
                    }
                }
            }
        }

        // 如果TTF字体加载成功，打印信息
        if (this.ttfLoader != null) {

        } else {

        }

        // 加载默认BitmapFont作为后备
        this.guiFont = app.getAssetManager().loadFont("Interface/Fonts/Default.fnt");

        this.screenWidth = app.getCamera().getWidth();
        this.screenHeight = app.getCamera().getHeight();

        initializeUI();
    }

    /**
     * 初始化UI
     */
    private void initializeUI() {
        // 创建编辑器根节点
        editorRootNode = new Node("PuppetEditorUI");

        // 创建顶栏
        createTopBar();

        // 创建方向显示
        createDirectionDisplay();

        // 计算主区域高度（扣除顶栏和底部时间轴）
        int mainAreaHeight = screenHeight - topBarHeight - timelineHeight;
        int mainAreaY = topBarHeight;

        // 创建左侧按钮列（传递TTF字体加载器）
        // 按钮列占左侧上半部分（60%）
        int buttonColumnHeight = (int)(mainAreaHeight * 0.6);
        buttonColumnPanel = new ButtonColumnPanel(app, guiFont, ttfLoader, 0, mainAreaY + mainAreaHeight - buttonColumnHeight, buttonColumnWidth, buttonColumnHeight);
        editorRootNode.attachChild(buttonColumnPanel.getRootNode());

        // 创建动画层管理面板（放在按钮列下方，占左侧下半部分20%）
        int layerPanelHeight = (int)(mainAreaHeight * 0.2);
        int layerPanelY = mainAreaY + (int)(mainAreaHeight * 0.2);  // 在分组面板上方
        animationLayerPanel = new AnimationLayerPanel(app, guiFont, 0, layerPanelY, buttonColumnWidth, layerPanelHeight);
        editorRootNode.attachChild(animationLayerPanel.getRootNode());

        // 创建骨骼分组控制面板（放在动画层面板下方，占左侧下半部分20%）
        int groupPanelHeight = (int)(mainAreaHeight * 0.2);
        int groupPanelY = mainAreaY;
        groupControlPanel = new GroupControlPanel(app, guiFont, ttfLoader, 0, groupPanelY, buttonColumnWidth, groupPanelHeight);
        editorRootNode.attachChild(groupControlPanel.getRootNode());

        // 右侧列分为两部分：上半部分是层级列表，下半部分是属性滑条
        int rightX = screenWidth - rightColumnWidth;

        // 上半部分：部件层级列表（占右侧的60%）
        int partListHeight = (int)(mainAreaHeight * 0.6);
        int partListY = mainAreaY + mainAreaHeight - partListHeight;  // 从上往下
        partListPanel = new PartListPanel(app, guiFont, rightX, partListY, rightColumnWidth, partListHeight);
        editorRootNode.attachChild(partListPanel.getRootNode());

        // 下半部分：属性滑条（占右侧的80%，增加高度以容纳所有滑条包括贴图旋转）
        int sliderHeight = (int)(mainAreaHeight * 0.8);
        int sliderY = mainAreaY;
        sliderColumnPanel = new SliderColumnPanel(app, guiFont, ttfLoader, rightX, sliderY, rightColumnWidth, sliderHeight);
        editorRootNode.attachChild(sliderColumnPanel.getRootNode());

        // 底部时间轴
        timelinePanel = new TimelinePanel(app, guiFont, 0, 0, screenWidth, timelineHeight);
        editorRootNode.attachChild(timelinePanel.getRootNode());

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
        bgMat.setColor("Color", new ColorRGBA(0.15f, 0.15f, 0.2f, 0.95f));
        bgMat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        topBarBg.setMaterial(bgMat);
        topBarBg.setLocalTranslation(0, screenHeight - topBarHeight, -2);
        editorRootNode.attachChild(topBarBg);

        // 标题文本
        BitmapText titleText = new BitmapText(guiFont);
        titleText.setText("木偶编辑器 - Puppet Editor");
        titleText.setSize(guiFont.getCharSet().getRenderedSize() * 2.5f);
        titleText.setColor(ColorRGBA.White);
        titleText.setLocalTranslation(10, screenHeight - 15, 0);
        editorRootNode.attachChild(titleText);

        // Back 按钮（放在顶栏里面，上移以适应TTF字体）
        int buttonY = screenHeight - 35;  // 再上移15像素

        // 语言切换按钮 (最右边往左数第三个)
        int langX = screenWidth - 270;
        if (ttfLoader != null) {
            languageButton = new Button(app, ttfLoader, "语言", langX, buttonY, 80, 40);
        } else {
            languageButton = new Button(app, guiFont, "Lang", langX, buttonY, 80, 40);
        }
        languageButton.setClickListener(() -> {
            // 显示语言选择对话框
            java.awt.EventQueue.invokeLater(() -> {
                LanguageDialog dialog = new LanguageDialog(null);
                dialog.setVisible(true);
                String selectedLang = dialog.getSelectedLanguage();
                if (selectedLang != null) {
                    // 使用 app.enqueue() 在jME3主线程上执行语言切换
                    final String langToSet = selectedLang;
                    app.enqueue(() -> {
                        LanguageManager.getInstance().setLanguage(langToSet);
                        return null;
                    });
                }
            });
        });
        editorRootNode.attachChild(languageButton.getRootNode());

        // Back 按钮
        int backX = screenWidth - 180;
        if (ttfLoader != null) {
            backButton = new Button(app, ttfLoader, "返回", backX, buttonY, 80, 40);
        } else {
            backButton = new Button(app, guiFont, "Back", backX, buttonY, 80, 40);
        }
        backButton.setClickListener(() -> {
            if (editorCallbacks != null) {
                editorCallbacks.onBackButtonClicked();
            }
        });
        editorRootNode.attachChild(backButton.getRootNode());

        // Exit 按钮
        int exitX = screenWidth - 90;
        if (ttfLoader != null) {
            exitButton = new Button(app, ttfLoader, "退出", exitX, buttonY, 80, 40);
        } else {
            exitButton = new Button(app, guiFont, "Exit", exitX, buttonY, 80, 40);
        }
        exitButton.setClickListener(() -> {
            if (editorCallbacks != null) {
                editorCallbacks.onExitButtonClicked();
            }
        });
        editorRootNode.attachChild(exitButton.getRootNode());
    }

    /**
     * 创建方向显示
     */
    private void createDirectionDisplay() {
        currentDirectionText = new BitmapText(guiFont);
        currentDirectionText.setText("Current View: FRONT");
        currentDirectionText.setSize(guiFont.getCharSet().getRenderedSize() * 2.5f);
        currentDirectionText.setColor(new ColorRGBA(1.0f, 0.8f, 0.2f, 1.0f)); // 金黄色
        // 放在屏幕顶部中央，z坐标设置得高一些确保显示在最上层
        currentDirectionText.setLocalTranslation(screenWidth / 2 - 180, screenHeight - 50, 10);
        editorRootNode.attachChild(currentDirectionText);

        // 创建关键帧类型显示
        keyframeTypeText = new BitmapText(guiFont);
        keyframeTypeText.setText("模式: 插值 (T切换)");
        keyframeTypeText.setSize(guiFont.getCharSet().getRenderedSize() * 2.0f);
        keyframeTypeText.setColor(ColorRGBA.Yellow); // 黄色表示插值模式
        // 放在方向文本下方
        keyframeTypeText.setLocalTranslation(screenWidth / 2 - 150, screenHeight - 90, 10);
        editorRootNode.attachChild(keyframeTypeText);

        // 注册语言变化监听器
        LanguageManager.getInstance().addListener(newLanguage -> {
            updateLanguage();
        });
    }

    /**
     * 更新UI语言
     */
    private void updateLanguage() {
        LanguageManager langMgr = LanguageManager.getInstance();

        // 更新顶部按钮
        if (ttfLoader != null) {
            languageButton.setText(langMgr.getText("language"));
            backButton.setText(langMgr.getText("back"));
            exitButton.setText(langMgr.getText("exit"));
        } else {
            languageButton.setText(langMgr.getText("language"));
            backButton.setText(langMgr.getText("back"));
            exitButton.setText(langMgr.getText("exit"));
        }

        // 通知ButtonColumnPanel更新
        if (buttonColumnPanel != null) {
            buttonColumnPanel.updateLanguage();
        }

        // 通知SliderColumnPanel更新
        if (sliderColumnPanel != null) {
            sliderColumnPanel.updateLanguage();
        }
    }

    /**
     * 设置当前编辑的骨架
     */
    public void setSkeleton(EditorSkeleton skeleton) {
        this.currentSkeleton = skeleton;
        this.selectedBone = null;
        this.currentPartRenderer = null;

        // 更新部件列表
        if (partListPanel != null) {
            partListPanel.setSkeleton(skeleton);
        }

        // 更新骨骼分组控制面板
        if (groupControlPanel != null) {
            groupControlPanel.setSkeleton(skeleton);
        }
    }

    /**
     * 选择骨骼
     */
    public void selectBone(EditorBone bone) {
        this.selectedBone = bone;
        this.currentPartRenderer = null;
        updateSliders();

        // 更新部件列表高亮
        if (partListPanel != null) {
            partListPanel.setSelectedBone(bone);
        }

        // 更新骨骼分组控制面板
        if (groupControlPanel != null) {
            groupControlPanel.setSelectedBone(bone);
        }
    }

    /**
     * 选择骨骼（带渲染器）
     */
    public void selectBone(EditorBone bone, EditorPuppetPartRenderer partRenderer) {
        this.selectedBone = bone;
        this.currentPartRenderer = partRenderer;
        updateSliders();

        // 更新SliderColumnPanel的当前部件渲染器（用于网格配置）
        if (sliderColumnPanel != null) {
            sliderColumnPanel.setCurrentPartRenderer(partRenderer);
        }

        // 更新部件列表高亮
        if (partListPanel != null) {
            partListPanel.setSelectedBone(bone);
        }

        // 更新骨骼分组控制面板
        if (groupControlPanel != null) {
            groupControlPanel.setSelectedBone(bone);
        }

        // 更新Billboard按钮状态
        if (buttonColumnPanel != null && bone != null) {
            buttonColumnPanel.updateBillboardButton(bone.isBillboardEnabled());
            buttonColumnPanel.updateTextureModeButton(bone.isMultiDirectionTextureEnabled());
            buttonColumnPanel.updateCameraFollowButtonText(
                bone.getCameraFollowFreedomX(),
                bone.getCameraFollowFreedomY()
            );
        }
    }

    /**
     * 更新滑条显示
     */
    private void updateSliders() {
        if (sliderColumnPanel == null || selectedBone == null) return;

        // Update sliders with current bone values
        if (currentPartRenderer != null) {
            sliderColumnPanel.getWidthSlider().setValue(currentPartRenderer.getWidth());
            sliderColumnPanel.getHeightSlider().setValue(currentPartRenderer.getHeight());

            // 更新优先级滑条
            if (selectedBone != null) {
                sliderColumnPanel.getPrioritySlider().setValue(selectedBone.getPriority());
            }

            // 手动更新 currentPartWidth 和 currentPartHeight
            // 因为 setValue() 不会触发监听器
            sliderColumnPanel.setCurrentPartDimensions(currentPartRenderer.getWidth(), currentPartRenderer.getHeight());

            // 更新纹理预览
            if (sliderColumnPanel.getTexturePreviewPanel() != null) {
                com.jme3.texture.Texture texture = currentPartRenderer.getTexture();
                sliderColumnPanel.getTexturePreviewPanel().setTexture(texture);

                // 同步UV坐标到预览面板
                sliderColumnPanel.getTexturePreviewPanel().setUV(
                    currentPartRenderer.getUvOffsetX(),
                    currentPartRenderer.getUvOffsetY(),
                    currentPartRenderer.getUvScaleX(),
                    currentPartRenderer.getUvScaleY()
                );
                //     currentPartRenderer.getUvOffsetY() + ") Scale: (" +
                //     currentPartRenderer.getUvScaleX() + ", " + currentPartRenderer.getUvScaleY() + ")");
            }
        }

        sliderColumnPanel.getPosXSlider().setValue(selectedBone.getLocalPosition().x);
        sliderColumnPanel.getPosYSlider().setValue(selectedBone.getLocalPosition().y);
        sliderColumnPanel.getPosZSlider().setValue(selectedBone.getLocalPosition().z);

        float[] angles = new float[3];
        selectedBone.getLocalRotation().toAngles(angles);
        sliderColumnPanel.getRotXSlider().setValue((float)Math.toDegrees(angles[0]));
        sliderColumnPanel.getRotYSlider().setValue((float)Math.toDegrees(angles[1]));
        sliderColumnPanel.getRotZSlider().setValue((float)Math.toDegrees(angles[2]));

        // 更新贴图旋转滑条（读取当前方向的贴图旋转角度）
        float textureRotation = selectedBone.getCurrentDirectionTextureRotation();
        sliderColumnPanel.getTextureRotationSlider().setValue(textureRotation);

        // 更新网格大小滑条（读取当前部件的值）
        if (currentPartRenderer != null) {
            sliderColumnPanel.getGridSizeSlider().setValue(currentPartRenderer.getGridSize());
        }

        // 更新自由度滑条（仅对自由骨骼有效）
        if (selectedBone.isFreeBone()) {
            sliderColumnPanel.getFreedomValueSlider().setValue(selectedBone.getFreedomValue());
        }

        // 更新重力按钮文本（仅对自由骨骼有效）
        if (buttonColumnPanel != null && selectedBone.isFreeBone()) {
            String gravityText = "重力:" + selectedBone.getGravityDirection().getDisplayName();
            buttonColumnPanel.updateGravityButtonText(gravityText);
        }

        // 禁用自动UV调整，以保持用户或复制粘贴设置的UV坐标
        // 如果启用此功能，会导致：
        // 1. 复制粘贴后UV坐标被自动调整，丢失原始UV设置
        // 2. 每次选择部件时UV都会被重置
        // 用户明确要求"禁止uv框试图吻合纹理"
        // if (currentPartRenderer != null) {
        //     sliderColumnPanel.adjustUVToMatchPartAspectRatio();
        // }
    }

    /**
     * 更新Inspector显示
     */
    public void updateInspector() {
        updateSliders();
    }

    /**
     * 更新编辑器
     */
    public void update(float tpf) {
        if (!visible) return;

        currentTime += tpf;

        // 更新按钮列
        if (buttonColumnPanel != null) {
            buttonColumnPanel.update(tpf);
        }

        // 更新动画层面板
        if (animationLayerPanel != null) {
            animationLayerPanel.update(tpf);
        }

        // 更新顶栏按钮
        if (backButton != null) {
            backButton.update(tpf);
        }
        if (exitButton != null) {
            exitButton.update(tpf);
        }
        if (languageButton != null) {
            languageButton.update(tpf);
        }

        // 更新时间轴（如果启用）
        if (timelinePanel != null) {
            timelinePanel.update(tpf);
        }
    }

    /**
     * 更新按钮状态（独立于时间轴）
     */
    public void updateButtons(float tpf) {
        if (!visible) return;

        // 更新按钮列
        if (buttonColumnPanel != null) {
            buttonColumnPanel.update(tpf);
        }

        // 更新顶栏按钮
        if (backButton != null) {
            backButton.update(tpf);
        }
        if (exitButton != null) {
            exitButton.update(tpf);
        }
        if (languageButton != null) {
            languageButton.update(tpf);
        }

        // 更新时间轴（重要：包括解锁播放切换）
        if (timelinePanel != null) {
            timelinePanel.update(tpf);
        }
    }

    /**
     * 回调接口，用于通知编辑器状态变化
     */
    public interface EditorCallbacks {
        void onEditorOpened();
        void onBackButtonClicked();
        void onExitButtonClicked();
    }
    private EditorCallbacks editorCallbacks;

    public void setEditorCallbacks(EditorCallbacks callbacks) {
        this.editorCallbacks = callbacks;
    }

    /**
     * 切换显示/隐藏
     */
    public void toggleVisibility() {
        visible = !visible;

        if (visible) {
            // 打开编辑器时，检查窗口大小是否变化（例如全屏切换）
            int currentWidth = app.getCamera().getWidth();
            int currentHeight = app.getCamera().getHeight();
            if (currentWidth != screenWidth || currentHeight != screenHeight) {
                reshapeUI(currentWidth, currentHeight);
            }

            editorRootNode.setCullHint(com.jme3.scene.Spatial.CullHint.Never);
            guiNode.attachChild(editorRootNode);

            // 通知编辑器已打开（用于重新设置回调）
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
     * 响应窗口大小变化
     */
    public void reshapeUI(int newWidth, int newHeight) {
        this.screenWidth = newWidth;
        this.screenHeight = newHeight;

        // 保存当前选中的骨骼和渲染器
        EditorBone oldSelectedBone = selectedBone;
        EditorPuppetPartRenderer oldPartRenderer = currentPartRenderer;

        // 保存旧的AnimationClip引用（如果TimelinePanel存在）
        com.Hecate.puppet.animation.AnimationClip oldAnimationClip = null;
        if (timelinePanel != null) {
            oldAnimationClip = timelinePanel.getAnimationClip();
        }

        // 清除旧的UI
        editorRootNode.detachAllChildren();

        // 重新创建UI
        createTopBar();
        createDirectionDisplay();

        // 计算主区域高度（扣除顶栏和底部时间轴）
        int mainAreaHeight = screenHeight - topBarHeight - timelineHeight;
        int mainAreaY = topBarHeight;

        // 创建左侧按钮列（传递TTF字体加载器）
        // 按钮列占左侧上半部分（60%）
        int buttonColumnHeight = (int)(mainAreaHeight * 0.6);
        buttonColumnPanel = new ButtonColumnPanel(app, guiFont, ttfLoader, 0, mainAreaY + mainAreaHeight - buttonColumnHeight, buttonColumnWidth, buttonColumnHeight);
        editorRootNode.attachChild(buttonColumnPanel.getRootNode());

        // 创建动画层管理面板（放在按钮列下方，占左侧下半部分40%）
        int layerPanelHeight = (int)(mainAreaHeight * 0.4);
        int layerPanelY = mainAreaY;
        animationLayerPanel = new AnimationLayerPanel(app, guiFont, 0, layerPanelY, buttonColumnWidth, layerPanelHeight);
        editorRootNode.attachChild(animationLayerPanel.getRootNode());

        // 右侧列分为两部分
        int rightX = screenWidth - rightColumnWidth;

        // 上半部分：部件层级列表
        int partListHeight = (int)(mainAreaHeight * 0.6);
        int partListY = mainAreaY + mainAreaHeight - partListHeight;
        partListPanel = new PartListPanel(app, guiFont, rightX, partListY, rightColumnWidth, partListHeight);
        editorRootNode.attachChild(partListPanel.getRootNode());

        // 下半部分：属性滑条
        int sliderHeight = (int)(mainAreaHeight * 0.4);
        int sliderY = mainAreaY;
        sliderColumnPanel = new SliderColumnPanel(app, guiFont, ttfLoader, rightX, sliderY, rightColumnWidth, sliderHeight);
        editorRootNode.attachChild(sliderColumnPanel.getRootNode());

        // 底部时间轴
        timelinePanel = new TimelinePanel(app, guiFont, 0, 0, screenWidth, timelineHeight);
        editorRootNode.attachChild(timelinePanel.getRootNode());

        // 恢复骨架和选中的骨骼
        if (currentSkeleton != null) {
            partListPanel.setSkeleton(currentSkeleton);
        }
        if (oldSelectedBone != null) {
            selectBone(oldSelectedBone, oldPartRenderer);
        }

        // 恢复AnimationClip（在重新创建TimelinePanel之后）
        if (oldAnimationClip != null && timelinePanel != null) {
            timelinePanel.setAnimationClip(oldAnimationClip);
        }

        // 重新设置回调
        if (editorCallbacks != null) {
            editorCallbacks.onEditorOpened();
        }
    }

    /**
     * 处理鼠标点击事件
     */
    public boolean handleMouseClick(int mouseX, int mouseY) {
        if (!visible) return false;

        // 调试输出

        // 检查语言按钮
        if (languageButton != null && languageButton.handleMouseClick(mouseX, mouseY)) {
            return true;
        }

        // 检查Back按钮
        if (backButton != null && backButton.handleMouseClick(mouseX, mouseY)) {
            return true;
        }

        // 检查Exit按钮
        if (exitButton != null && exitButton.handleMouseClick(mouseX, mouseY)) {
            return true;
        }

        // 检查动画层面板
        if (animationLayerPanel != null && animationLayerPanel.handleMouseClick(mouseX, mouseY)) {
            return true;
        }

        return false;
    }

    /**
     * 清理资源
     */
    public void cleanup() {
        if (editorRootNode != null) {
            editorRootNode.removeFromParent();
        }
    }

    // ========== Getters ==========

    public boolean isVisible() {
        return visible;
    }

    public EditorSkeleton getCurrentSkeleton() {
        return currentSkeleton;
    }

    public EditorBone getSelectedBone() {
        return selectedBone;
    }

    public float getCurrentTime() {
        return currentTime;
    }

    public void setCurrentTime(float time) {
        this.currentTime = time;
    }

    public ButtonColumnPanel getButtonColumnPanel() {
        return buttonColumnPanel;
    }

    public SliderColumnPanel getSliderColumnPanel() {
        return sliderColumnPanel;
    }

    public EditorPuppetPartRenderer getCurrentPartRenderer() {
        return currentPartRenderer;
    }

    public PartListPanel getPartListPanel() {
        return partListPanel;
    }

    public TimelinePanel getTimelinePanel() {
        return timelinePanel;
    }

    public AnimationLayerPanel getAnimationLayerPanel() {
        return animationLayerPanel;
    }

    /**
     * 更新当前方向显示
     */
    public void updateCurrentDirection(EditorBone.Direction direction) {
        if (currentDirectionText != null) {
            currentDirectionText.setText("Current View: " + direction.name());
        }
    }

    /**
     * 更新关键帧类型显示
     */
    public void updateKeyframeTypeDisplay(com.Hecate.puppet.animation.Keyframe.KeyframeType type) {
        if (keyframeTypeText != null) {
            if (type == com.Hecate.puppet.animation.Keyframe.KeyframeType.SNAPSHOT) {
                keyframeTypeText.setText("模式: 快照 (T切换)");
                keyframeTypeText.setColor(new ColorRGBA(0.2f, 0.5f, 1.0f, 1.0f)); // 蓝色表示快照模式
            } else {
                keyframeTypeText.setText("模式: 插值 (T切换)");
                keyframeTypeText.setColor(ColorRGBA.Yellow); // 黄色表示插值模式
            }
        }
    }
}
