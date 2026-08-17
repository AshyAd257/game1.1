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
import com.Hecate.puppet.editor.core.EditorPuppetPartRenderer;

/**
 * 右侧滑条列面板
 * 包含所有滑条控件
 */
public class SliderColumnPanel {

    private final SimpleApplication app;
    private final BitmapFont font;
    private final TTFontLoader ttfLoader;  // 新增：TTF字体加载器
    private final Node rootNode;
    private int x, y;  // 改为可变，支持拖动

    // 拖动相关
    private boolean isDragging = false;
    private int dragStartX = 0;
    private int dragStartY = 0;
    private int panelStartX = 0;
    private int panelStartY = 0;
    private final int titleBarHeight = 30;
    private Geometry backgroundGeometry;
    private Geometry titleBarGeometry;  // 标题栏背景（可拖动区域提示）
    private BitmapText titleText;

    // 调整大小相关
    private boolean isResizing = false;
    private ResizeDirection resizeDirection = ResizeDirection.NONE;
    private int resizeStartX = 0;
    private int resizeStartY = 0;
    private int panelStartWidth = 0;
    private int panelStartHeight = 0;
    private int width;   // 可变宽度
    private int height;  // 可变高度
    private final int RESIZE_BORDER = 8;  // 调整大小的边框宽度
    private final int MIN_WIDTH = 200;
    private final int MIN_HEIGHT = 300;

    // 调整大小方向枚举
    public enum ResizeDirection {
        NONE, LEFT, RIGHT, TOP, BOTTOM,
        TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
    }

    // 所有滑条
    private Slider widthSlider;
    private Slider heightSlider;
    private Slider prioritySlider;  // 优先级滑条
    private Slider posXSlider;
    private Slider posYSlider;
    private Slider posZSlider;
    private Slider rotXSlider;
    private Slider rotYSlider;
    private Slider rotZSlider;
    private Slider textureRotationSlider;  // 新增：贴图旋转滑条
    private Slider gridSizeSlider;
    private Slider freedomValueSlider;  // 新增：自由度滑条
    private Slider swingAmplitudeSlider;  // 新增：摇摆幅度滑条
    private Slider swingFrequencySlider;  // 新增：摇摆频率滑条
    private Slider swingPhaseSlider;  // 新增：摇摆相位滑条

    // UV纹理预览
    private TexturePreviewPanel texturePreviewPanel;
    private EnlargedTexturePreviewPanel enlargedPreviewPanel;
    private Button openEnlargedPreviewButton;

    // 存储当前部件的尺寸（用于放大预览的宽高比）
    private float currentPartWidth = 1.0f;
    private float currentPartHeight = 1.0f;

    // 存储当前选中的部件渲染器（用于网格配置）
    private EditorPuppetPartRenderer currentPartRenderer;

    // 回调接口
    public interface SliderCallbacks {
        void onWidthChanged(float value);
        void onHeightChanged(float value);
        void onPriorityChanged(int value);  // 优先级回调
        void onPosXChanged(float value);
        void onPosYChanged(float value);
        void onPosZChanged(float value);
        void onRotXChanged(float value);
        void onRotYChanged(float value);
        void onRotZChanged(float value);
        void onTextureRotationChanged(float value);  // 新增：贴图旋转回调
        void onGridSizeChanged(float value);
        void onFreedomValueChanged(float value);  // 新增：自由度回调
        void onSwingAmplitudeChanged(float value);  // 新增：摇摆幅度回调
        void onSwingFrequencyChanged(float value);  // 新增：摇摆频率回调
        void onSwingPhaseChanged(float value);  // 新增：摇摆相位回调
        void onUVChanged(float uvOffsetX, float uvOffsetY, float uvScaleX, float uvScaleY);
    }
    private SliderCallbacks callbacks;

    // 原构造函数 - 只使用BitmapFont
    public SliderColumnPanel(SimpleApplication app, BitmapFont font, int x, int y, int width, int height) {
        this.app = app;
        this.font = font;
        this.ttfLoader = null;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        this.rootNode = new Node("SliderColumnPanel");
        initializePanel();
    }

    // 新构造函数 - 支持TTFontLoader
    public SliderColumnPanel(SimpleApplication app, BitmapFont font, TTFontLoader ttfLoader, int x, int y, int width, int height) {
        this.app = app;
        this.font = font;
        this.ttfLoader = ttfLoader;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        this.rootNode = new Node("SliderColumnPanel");
        initializePanel();
    }

    private void initializePanel() {
        // 创建半透明背景
        Quad bgQuad = new Quad(width, height);
        backgroundGeometry = new Geometry("SliderColumnBackground", bgQuad);
        Material bgMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        bgMat.setColor("Color", new ColorRGBA(0.1f, 0.1f, 0.1f, 0.85f));
        bgMat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        backgroundGeometry.setMaterial(bgMat);
        backgroundGeometry.setLocalTranslation(x, y, -1);  // z=-1 让背景在最后面
        rootNode.attachChild(backgroundGeometry);

        // 创建标题栏背景（浅色，提示可拖动）
        Quad titleBarQuad = new Quad(width, titleBarHeight);
        titleBarGeometry = new Geometry("SliderColumnTitleBar", titleBarQuad);
        Material titleBarMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        titleBarMat.setColor("Color", new ColorRGBA(0.05f, 0.05f, 0.08f, 0.95f));  // 深色背景
        titleBarMat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        titleBarGeometry.setMaterial(titleBarMat);
        titleBarGeometry.setLocalTranslation(x, y + height - titleBarHeight, -0.5f);
        rootNode.attachChild(titleBarGeometry);

        // 添加标题
        titleText = new BitmapText(font);

        titleText.setSize(font.getCharSet().getRenderedSize() * 1.8f);
        titleText.setColor(ColorRGBA.Yellow);
        titleText.setLocalTranslation(x + 10, y + height - 20, 1);
        rootNode.attachChild(titleText);

        // 创建所有滑条
        int currentY = y + height - 60;
        int startX = x + 10;

        // Width
        widthSlider = createSliderWithChinese("宽度", "Width", 1, 1000, 100, startX, currentY);
        widthSlider.setChangeListener((value) -> {
            currentPartWidth = value;  // 更新当前部件宽度
            if (callbacks != null) callbacks.onWidthChanged(value);
            // 禁用自动UV调整（用户明确要求"禁止uv框试图吻合纹理"）
            // adjustUVToMatchPartAspectRatioInternal();
        });
        rootNode.attachChild(widthSlider.getRootNode());
        currentY -= 50;

        // Height
        heightSlider = createSliderWithChinese("高度", "Height", 1, 1000, 100, startX, currentY);
        heightSlider.setChangeListener((value) -> {
            currentPartHeight = value;  // 更新当前部件高度
            if (callbacks != null) callbacks.onHeightChanged(value);
            // 禁用自动UV调整（用户明确要求"禁止uv框试图吻合纹理"）
            // adjustUVToMatchPartAspectRatioInternal();
        });
        rootNode.attachChild(heightSlider.getRootNode());
        currentY -= 50;

        // Priority（优先级，用于控制部件的视觉层叠顺序）
        prioritySlider = createSliderWithChinese("优先级", "Priority", -100, 100, 0, startX, currentY);
        prioritySlider.setChangeListener((value) -> {
            if (callbacks != null) callbacks.onPriorityChanged((int)value);
        });
        rootNode.attachChild(prioritySlider.getRootNode());
        currentY -= 50;

        // Texture Rotation（贴图旋转，仅旋转UV坐标，支持多圈旋转：-1800到+1800度，即-5圈到+5圈）
        textureRotationSlider = createSliderWithChinese("贴图旋转", "Tex Rot", -1800, 1800, 0, startX, currentY);
        textureRotationSlider.setChangeListener((value) -> {
            if (callbacks != null) callbacks.onTextureRotationChanged(value);
        });
        rootNode.attachChild(textureRotationSlider.getRootNode());
        currentY -= 50;

        // Position X（降低灵敏度）
        posXSlider = createSliderWithChinese("位置X", "Pos X", -50, 50, 0, startX, currentY);
        posXSlider.setSensitivity(0.1f);  // 10%灵敏度
        posXSlider.setChangeListener((value) -> {
            if (callbacks != null) callbacks.onPosXChanged(value);
        });
        rootNode.attachChild(posXSlider.getRootNode());
        currentY -= 50;

        // Position Y（降低灵敏度）
        posYSlider = createSliderWithChinese("位置Y", "Pos Y", -50, 50, 0, startX, currentY);
        posYSlider.setSensitivity(0.1f);  // 10%灵敏度
        posYSlider.setChangeListener((value) -> {
            if (callbacks != null) callbacks.onPosYChanged(value);
        });
        rootNode.attachChild(posYSlider.getRootNode());
        currentY -= 50;

        // Position Z（降低灵敏度）
        posZSlider = createSliderWithChinese("位置Z", "Pos Z", -50, 50, 0, startX, currentY);
        posZSlider.setSensitivity(0.01f);  // 1%灵敏度
        posZSlider.setChangeListener((value) -> {
            if (callbacks != null) callbacks.onPosZChanged(value);
        });
        rootNode.attachChild(posZSlider.getRootNode());
        currentY -= 50;

        // Rotation X
        rotXSlider = createSliderWithChinese("旋转X", "Rot X", -180, 180, 0, startX, currentY);
        rotXSlider.setChangeListener((value) -> {
            if (callbacks != null) callbacks.onRotXChanged(value);
        });
        rootNode.attachChild(rotXSlider.getRootNode());
        currentY -= 50;

        // Rotation Y
        rotYSlider = createSliderWithChinese("旋转Y", "Rot Y", -180, 180, 0, startX, currentY);
        rotYSlider.setChangeListener((value) -> {
            if (callbacks != null) callbacks.onRotYChanged(value);
        });
        rootNode.attachChild(rotYSlider.getRootNode());
        currentY -= 50;

        // Rotation Z
        rotZSlider = createSliderWithChinese("旋转Z", "Rot Z", -180, 180, 0, startX, currentY);
        rotZSlider.setChangeListener((value) -> {
            if (callbacks != null) callbacks.onRotZChanged(value);
        });
        rootNode.attachChild(rotZSlider.getRootNode());
        currentY -= 50;

        // Grid Size
        gridSizeSlider = createSliderWithChinese("网格大小", "Grid Size", 1, 100, 10, startX, currentY);
        gridSizeSlider.setChangeListener((value) -> {
            if (callbacks != null) callbacks.onGridSizeChanged(value);
        });
        rootNode.attachChild(gridSizeSlider.getRootNode());
        currentY -= 50;

        // Freedom Value（自由骨骼的自由度，0-1范围）
        freedomValueSlider = createSliderWithChinese("自由度", "Freedom", 0f, 1f, 0.5f, startX, currentY);
        freedomValueSlider.setSensitivity(0.1f);  // 10%灵敏度
        freedomValueSlider.setChangeListener((value) -> {
            if (callbacks != null) callbacks.onFreedomValueChanged(value);
        });
        rootNode.attachChild(freedomValueSlider.getRootNode());
        currentY -= 50;

        // Swing Amplitude（摇摆幅度，0-90度）
        swingAmplitudeSlider = createSliderWithChinese("摇摆幅度", "Swing Amp", 0f, 90f, 15f, startX, currentY);
        swingAmplitudeSlider.setChangeListener((value) -> {
            if (callbacks != null) callbacks.onSwingAmplitudeChanged(value);
        });
        rootNode.attachChild(swingAmplitudeSlider.getRootNode());
        currentY -= 50;

        // Swing Frequency（摇摆频率，0-5Hz）
        swingFrequencySlider = createSliderWithChinese("摇摆频率", "Swing Freq", 0f, 5f, 0.5f, startX, currentY);
        swingFrequencySlider.setSensitivity(0.1f);  // 10%灵敏度
        swingFrequencySlider.setChangeListener((value) -> {
            if (callbacks != null) callbacks.onSwingFrequencyChanged(value);
        });
        rootNode.attachChild(swingFrequencySlider.getRootNode());
        currentY -= 50;

        // Swing Phase（摇摆相位，0-2π弧度）
        swingPhaseSlider = createSliderWithChinese("摇摆相位", "Swing Phase", 0f, 6.28318f, 0f, startX, currentY);
        swingPhaseSlider.setSensitivity(0.1f);  // 10%灵敏度
        swingPhaseSlider.setChangeListener((value) -> {
            if (callbacks != null) callbacks.onSwingPhaseChanged(value);
        });
        rootNode.attachChild(swingPhaseSlider.getRootNode());
        currentY -= 50;

        // UV纹理预览面板（在所有滑条下方）
        texturePreviewPanel = new TexturePreviewPanel(app, font, x + 10, currentY - 150, width - 20, 150);
        rootNode.attachChild(texturePreviewPanel.getRootNode());

        // 在TexturePreviewPanel下方添加打开UV编辑器按钮
        int buttonYPos = currentY - 160;  // texturePreview下方10像素
        openEnlargedPreviewButton = new Button(app, font, "Open UV Editor", x + 10, buttonYPos - 30, width - 20, 30);
        openEnlargedPreviewButton.setClickListener(() -> {
            if (enlargedPreviewPanel != null && texturePreviewPanel.getCurrentTexture() != null) {
                // 将当前部件渲染器传递给放大预览（用于网格配置）
                enlargedPreviewPanel.setCurrentPart(currentPartRenderer);

                // 将当前纹理和UV坐标同步到放大预览
                enlargedPreviewPanel.setTexture(texturePreviewPanel.getCurrentTexture());
                enlargedPreviewPanel.setUV(
                    texturePreviewPanel.getUvOffsetX(),
                    texturePreviewPanel.getUvOffsetY(),
                    texturePreviewPanel.getUvScaleX(),
                    texturePreviewPanel.getUvScaleY()
                );
                // 只保存宽高比，不调用setPartDimensions()以避免重置UV坐标
                // setPartDimensions()会将UV重置到固定位置，这不是我们想要的
                // 我们只需要在SCALE模式下使用的宽高比
                enlargedPreviewPanel.show();
            }
        });
        rootNode.attachChild(openEnlargedPreviewButton.getRootNode());

        // 调整背景高度以覆盖所有内容（包括UV预览和按钮）
        int contentBottomY = buttonYPos - 30;  // 按钮的底部位置
        int actualContentHeight = (y + height) - contentBottomY;  // 从顶部到按钮底部的实际高度
        if (actualContentHeight > height) {
            // 需要扩展背景 - 直接更新网格而不是重新创建
            Quad newBgQuad = new Quad(width, actualContentHeight);
            backgroundGeometry.setMesh(newBgQuad);

            // 更新实际高度
            this.height = actualContentHeight;
        }

        // 设置UV变化监听器
        texturePreviewPanel.setChangeListener((uvOffsetX, uvOffsetY, uvScaleX, uvScaleY) -> {
            if (callbacks != null) {
                callbacks.onUVChanged(uvOffsetX, uvOffsetY, uvScaleX, uvScaleY);
            }
        });

        // 放大预览面板（初始不可见）
        enlargedPreviewPanel = new EnlargedTexturePreviewPanel(app, font);
        rootNode.attachChild(enlargedPreviewPanel.getRootNode());
        enlargedPreviewPanel.hide();  // 初始隐藏

        // 设置放大预览的UV变化监听器，同步回小预览和回调
        enlargedPreviewPanel.setChangeListener((uvOffsetX, uvOffsetY, uvScaleX, uvScaleY) -> {
            // 同步到小预览面板
            if (texturePreviewPanel != null) {
                texturePreviewPanel.setUV(uvOffsetX, uvOffsetY, uvScaleX, uvScaleY);
            }
            // 触发回调，更新到渲染器
            if (callbacks != null) {
                callbacks.onUVChanged(uvOffsetX, uvOffsetY, uvScaleX, uvScaleY);
            }
            //                  ") Scale: (" + uvScaleX + ", " + uvScaleY + ")");
        });

        // 设置双击监听器，打开放大预览
        texturePreviewPanel.setDoubleClickListener(() -> {
            if (enlargedPreviewPanel != null) {
                // 将当前纹理和UV坐标同步到放大预览
                enlargedPreviewPanel.setTexture(texturePreviewPanel.getCurrentTexture());
                enlargedPreviewPanel.setUV(
                    texturePreviewPanel.getUvOffsetX(),
                    texturePreviewPanel.getUvOffsetY(),
                    texturePreviewPanel.getUvScaleX(),
                    texturePreviewPanel.getUvScaleY()
                );
                // 注意：不要调用setPartDimensions，因为它会修改UV坐标
                // enlargedPreviewPanel.setPartDimensions(currentPartWidth, currentPartHeight);
                enlargedPreviewPanel.show();
                //     (texturePreviewPanel.getCurrentTexture() != null ?
                //      texturePreviewPanel.getCurrentTexture().getName() : "null"));
            }
        });
    }

    public boolean handleMouseClick(int mouseX, int mouseY) {
        // 检查放大预览面板（优先级最高，因为它是全屏的）
        if (enlargedPreviewPanel != null && enlargedPreviewPanel.isVisible()) {
            if (enlargedPreviewPanel.handleMousePress(mouseX, mouseY)) {
                return true;
            }
        }

        // 检查"Open UV Editor"按钮
        if (openEnlargedPreviewButton != null && openEnlargedPreviewButton.handleMouseClick(mouseX, mouseY)) {
            return true;
        }

        // 检查纹理预览面板
        if (texturePreviewPanel != null && texturePreviewPanel.handleMousePress(mouseX, mouseY)) {
            return true;
        }

        // 检查滑条
        if (widthSlider.handleMouseClick(mouseX, mouseY)) return true;
        if (heightSlider.handleMouseClick(mouseX, mouseY)) return true;
        if (prioritySlider.handleMouseClick(mouseX, mouseY)) return true;
        if (textureRotationSlider.handleMouseClick(mouseX, mouseY)) return true;
        if (posXSlider.handleMouseClick(mouseX, mouseY)) return true;
        if (posYSlider.handleMouseClick(mouseX, mouseY)) return true;
        if (posZSlider.handleMouseClick(mouseX, mouseY)) return true;
        if (rotXSlider.handleMouseClick(mouseX, mouseY)) return true;
        if (rotYSlider.handleMouseClick(mouseX, mouseY)) return true;
        if (rotZSlider.handleMouseClick(mouseX, mouseY)) return true;
        if (gridSizeSlider.handleMouseClick(mouseX, mouseY)) return true;
        if (freedomValueSlider.handleMouseClick(mouseX, mouseY)) return true;
        if (swingAmplitudeSlider.handleMouseClick(mouseX, mouseY)) return true;
        if (swingFrequencySlider.handleMouseClick(mouseX, mouseY)) return true;
        if (swingPhaseSlider.handleMouseClick(mouseX, mouseY)) return true;
        return false;
    }

    public Node getRootNode() {
        return rootNode;
    }

    public void setCallbacks(SliderCallbacks callbacks) {
        this.callbacks = callbacks;
    }

    // Getter methods for sliders
    public Slider getWidthSlider() { return widthSlider; }
    public Slider getHeightSlider() { return heightSlider; }
    public Slider getPrioritySlider() { return prioritySlider; }
    public Slider getPosXSlider() { return posXSlider; }
    public Slider getPosYSlider() { return posYSlider; }
    public Slider getPosZSlider() { return posZSlider; }
    public Slider getRotXSlider() { return rotXSlider; }
    public Slider getRotYSlider() { return rotYSlider; }
    public Slider getRotZSlider() { return rotZSlider; }
    public Slider getTextureRotationSlider() { return textureRotationSlider; }
    public Slider getGridSizeSlider() { return gridSizeSlider; }
    public Slider getFreedomValueSlider() { return freedomValueSlider; }

    /**
     * 检查是否点击在标题栏（用于拖动）
     */
    public boolean handleTitleBarClick(int mouseX, int mouseY) {
        // 检查X范围
        if (mouseX < x || mouseX > x + width) {
            return false;
        }

        // 检查Y范围（标题栏区域）
        if (mouseY >= y + height - titleBarHeight && mouseY <= y + height) {
            isDragging = true;
            dragStartX = mouseX;
            dragStartY = mouseY;
            panelStartX = x;
            panelStartY = y;
            return true;
        }

        return false;
    }

    /**
     * 处理鼠标拖动（同时处理面板拖动、调整大小和滑条拖动）
     */
    public void handleMouseDrag(int mouseX, int mouseY) {
        // 如果正在调整大小
        if (isResizing) {
            handleResizeDrag(mouseX, mouseY);
            return;
        }

        // 如果正在拖动面板
        if (isDragging) {
            int deltaX = mouseX - dragStartX;
            int deltaY = mouseY - dragStartY;

            x = panelStartX + deltaX;
            y = panelStartY + deltaY;

            // 限制在屏幕范围内
            x = Math.max(0, Math.min(x, app.getCamera().getWidth() - width));
            y = Math.max(0, Math.min(y, app.getCamera().getHeight() - height));

            // 更新所有UI元素的位置
            updatePanelPosition();
        } else {
            // 优先处理放大预览的拖动
            if (enlargedPreviewPanel != null && enlargedPreviewPanel.isVisible()) {
                enlargedPreviewPanel.handleMouseDrag(mouseX, mouseY);
                return;
            }

            // 否则处理纹理预览和滑条拖动
            if (texturePreviewPanel != null) {
                texturePreviewPanel.handleMouseDrag(mouseX, mouseY);
            }
            widthSlider.handleMouseDrag(mouseX, mouseY);
            heightSlider.handleMouseDrag(mouseX, mouseY);
            prioritySlider.handleMouseDrag(mouseX, mouseY);
            textureRotationSlider.handleMouseDrag(mouseX, mouseY);
            posXSlider.handleMouseDrag(mouseX, mouseY);
            posYSlider.handleMouseDrag(mouseX, mouseY);
            posZSlider.handleMouseDrag(mouseX, mouseY);
            rotXSlider.handleMouseDrag(mouseX, mouseY);
            rotYSlider.handleMouseDrag(mouseX, mouseY);
            rotZSlider.handleMouseDrag(mouseX, mouseY);
            gridSizeSlider.handleMouseDrag(mouseX, mouseY);
            freedomValueSlider.handleMouseDrag(mouseX, mouseY);
            swingAmplitudeSlider.handleMouseDrag(mouseX, mouseY);
            swingFrequencySlider.handleMouseDrag(mouseX, mouseY);
            swingPhaseSlider.handleMouseDrag(mouseX, mouseY);
        }
    }

    /**
     * 处理鼠标释放（同时处理面板拖动释放、调整大小释放和滑条释放）
     */
    public void handleMouseRelease() {
        // 处理面板拖动释放
        if (isDragging) {
            isDragging = false;
        }

        // 处理调整大小释放
        if (isResizing) {
            isResizing = false;
            resizeDirection = ResizeDirection.NONE;
        }

        // 处理放大预览释放（需要传入鼠标坐标，但这里我们不追踪，传0,0）
        if (enlargedPreviewPanel != null && enlargedPreviewPanel.isVisible()) {
            enlargedPreviewPanel.handleMouseRelease(0, 0);
        }

        // 处理纹理预览释放（需要传入鼠标坐标，但这里我们不追踪，传0,0）
        if (texturePreviewPanel != null) {
            texturePreviewPanel.handleMouseRelease(0, 0);
        }

        // 处理滑条释放
        widthSlider.handleMouseRelease();
        heightSlider.handleMouseRelease();
        prioritySlider.handleMouseRelease();
        textureRotationSlider.handleMouseRelease();
        posXSlider.handleMouseRelease();
        posYSlider.handleMouseRelease();
        posZSlider.handleMouseRelease();
        rotXSlider.handleMouseRelease();
        rotYSlider.handleMouseRelease();
        rotZSlider.handleMouseRelease();
        gridSizeSlider.handleMouseRelease();
        freedomValueSlider.handleMouseRelease();
        swingAmplitudeSlider.handleMouseRelease();
        swingFrequencySlider.handleMouseRelease();
        swingPhaseSlider.handleMouseRelease();
    }

    /**
     * 更新面板位置（用于拖动时）
     */
    private void updatePanelPosition() {
        // 更新背景位置
        if (backgroundGeometry != null) {
            backgroundGeometry.setLocalTranslation(x, y, 0);
        }

        // 更新标题栏位置
        if (titleBarGeometry != null) {
            titleBarGeometry.setLocalTranslation(x, y + height - titleBarHeight, -0.5f);
        }

        // 更新标题
        if (titleText != null) {
            titleText.setLocalTranslation(x + 10, y + height - 20, 1);
        }

        // 更新所有滑条位置
        int currentY = y + height - 60;
        int startX = x + 10;

        if (widthSlider != null) {
            widthSlider.setPosition(startX, currentY);
            currentY -= 50;
        }
        if (heightSlider != null) {
            heightSlider.setPosition(startX, currentY);
            currentY -= 50;
        }
        if (prioritySlider != null) {
            prioritySlider.setPosition(startX, currentY);
            currentY -= 50;
        }
        if (textureRotationSlider != null) {
            textureRotationSlider.setPosition(startX, currentY);
            currentY -= 50;
        }
        if (posXSlider != null) {
            posXSlider.setPosition(startX, currentY);
            currentY -= 50;
        }
        if (posYSlider != null) {
            posYSlider.setPosition(startX, currentY);
            currentY -= 50;
        }
        if (posZSlider != null) {
            posZSlider.setPosition(startX, currentY);
            currentY -= 50;
        }
        if (rotXSlider != null) {
            rotXSlider.setPosition(startX, currentY);
            currentY -= 50;
        }
        if (rotYSlider != null) {
            rotYSlider.setPosition(startX, currentY);
            currentY -= 50;
        }
        if (rotZSlider != null) {
            rotZSlider.setPosition(startX, currentY);
            currentY -= 50;
        }
        if (gridSizeSlider != null) {
            gridSizeSlider.setPosition(startX, currentY);
            currentY -= 50;
        }
        if (freedomValueSlider != null) {
            freedomValueSlider.setPosition(startX, currentY);
            currentY -= 50;
        }
        if (swingAmplitudeSlider != null) {
            swingAmplitudeSlider.setPosition(startX, currentY);
            currentY -= 50;
        }
        if (swingFrequencySlider != null) {
            swingFrequencySlider.setPosition(startX, currentY);
            currentY -= 50;
        }
        if (swingPhaseSlider != null) {
            swingPhaseSlider.setPosition(startX, currentY);
            currentY -= 50;
        }

        // 更新纹理预览面板位置
        if (texturePreviewPanel != null) {
            texturePreviewPanel.setPosition(x + 10, currentY - 150);
        }

        // 更新"Open UV Editor"按钮位置
        if (openEnlargedPreviewButton != null) {
            int buttonYPos = currentY - 160;
            openEnlargedPreviewButton.setPosition(x + 10, buttonYPos - 30);
        }
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public TexturePreviewPanel getTexturePreviewPanel() {
        return texturePreviewPanel;
    }

    public EnlargedTexturePreviewPanel getEnlargedPreviewPanel() {
        return enlargedPreviewPanel;
    }

    /**
     * 获取调整大小方向（检查鼠标是否在边缘）
     */
    public ResizeDirection getResizeDirection(int mouseX, int mouseY) {
        // 检查是否在面板范围内
        if (mouseX < x || mouseX > x + width || mouseY < y || mouseY > y + height) {
            return ResizeDirection.NONE;
        }

        boolean onLeft = mouseX >= x && mouseX <= x + RESIZE_BORDER;
        boolean onRight = mouseX >= x + width - RESIZE_BORDER && mouseX <= x + width;
        boolean onTop = mouseY >= y + height - RESIZE_BORDER && mouseY <= y + height;
        boolean onBottom = mouseY >= y && mouseY <= y + RESIZE_BORDER;

        // 角落优先
        if (onLeft && onTop) return ResizeDirection.TOP_LEFT;
        if (onRight && onTop) return ResizeDirection.TOP_RIGHT;
        if (onLeft && onBottom) return ResizeDirection.BOTTOM_LEFT;
        if (onRight && onBottom) return ResizeDirection.BOTTOM_RIGHT;

        // 边缘
        if (onLeft) return ResizeDirection.LEFT;
        if (onRight) return ResizeDirection.RIGHT;
        if (onTop) return ResizeDirection.TOP;
        if (onBottom) return ResizeDirection.BOTTOM;

        return ResizeDirection.NONE;
    }

    /**
     * 检查是否点击在边缘（用于调整大小）
     * 注意：调整大小功能已禁用，因为滑条宽度调整存在问题
     */
    public boolean handleResizeBorderClick(int mouseX, int mouseY) {
        // 禁用调整大小功能
        return false;
    }

    /**
     * 处理调整大小的拖动
     */
    public void handleResizeDrag(int mouseX, int mouseY) {
        if (!isResizing || resizeDirection == ResizeDirection.NONE) return;

        int deltaX = mouseX - resizeStartX;
        int deltaY = mouseY - resizeStartY;

        int newX = panelStartX;
        int newY = panelStartY;
        int newWidth = panelStartWidth;
        int newHeight = panelStartHeight;

        switch (resizeDirection) {
            case LEFT:
                newX = panelStartX + deltaX;
                newWidth = panelStartWidth - deltaX;
                break;
            case RIGHT:
                newWidth = panelStartWidth + deltaX;
                break;
            case TOP:
                newHeight = panelStartHeight + deltaY;
                break;
            case BOTTOM:
                newY = panelStartY + deltaY;
                newHeight = panelStartHeight - deltaY;
                break;
            case TOP_LEFT:
                newX = panelStartX + deltaX;
                newWidth = panelStartWidth - deltaX;
                newHeight = panelStartHeight + deltaY;
                break;
            case TOP_RIGHT:
                newWidth = panelStartWidth + deltaX;
                newHeight = panelStartHeight + deltaY;
                break;
            case BOTTOM_LEFT:
                newX = panelStartX + deltaX;
                newWidth = panelStartWidth - deltaX;
                newY = panelStartY + deltaY;
                newHeight = panelStartHeight - deltaY;
                break;
            case BOTTOM_RIGHT:
                newWidth = panelStartWidth + deltaX;
                newY = panelStartY + deltaY;
                newHeight = panelStartHeight - deltaY;
                break;
            default:
                break;
        }

        // 应用最小尺寸限制
        if (newWidth >= MIN_WIDTH && newHeight >= MIN_HEIGHT) {
            this.x = newX;
            this.y = newY;
            this.width = newWidth;
            this.height = newHeight;

            // 重建背景
            rebuildBackground();

            // 更新所有UI元素位置
            updatePanelPosition();

            // 更新所有滑条的轨道宽度（面板宽度 - 边距 - 数值文本区域）
            int newTrackWidth = newWidth - 120;  // 留出边距和数值文本空间
            updateAllSliderWidths(newTrackWidth);
        }
    }

    /**
     * 更新所有滑条的轨道宽度（调整大小时调用）
     */
    private void updateAllSliderWidths(int newTrackWidth) {
        if (widthSlider != null) widthSlider.updateTrackWidth(newTrackWidth);
        if (heightSlider != null) heightSlider.updateTrackWidth(newTrackWidth);
        if (prioritySlider != null) prioritySlider.updateTrackWidth(newTrackWidth);
        if (textureRotationSlider != null) textureRotationSlider.updateTrackWidth(newTrackWidth);
        if (posXSlider != null) posXSlider.updateTrackWidth(newTrackWidth);
        if (posYSlider != null) posYSlider.updateTrackWidth(newTrackWidth);
        if (posZSlider != null) posZSlider.updateTrackWidth(newTrackWidth);
        if (rotXSlider != null) rotXSlider.updateTrackWidth(newTrackWidth);
        if (rotYSlider != null) rotYSlider.updateTrackWidth(newTrackWidth);
        if (rotZSlider != null) rotZSlider.updateTrackWidth(newTrackWidth);
        if (gridSizeSlider != null) gridSizeSlider.updateTrackWidth(newTrackWidth);
        if (freedomValueSlider != null) freedomValueSlider.updateTrackWidth(newTrackWidth);
        if (swingAmplitudeSlider != null) swingAmplitudeSlider.updateTrackWidth(newTrackWidth);
        if (swingFrequencySlider != null) swingFrequencySlider.updateTrackWidth(newTrackWidth);
        if (swingPhaseSlider != null) swingPhaseSlider.updateTrackWidth(newTrackWidth);
    }

    /**
     * 重建背景几何体（调整大小后需要）
     */
    private void rebuildBackground() {
        if (backgroundGeometry != null) {
            backgroundGeometry.removeFromParent();
        }

        Quad bgQuad = new Quad(width, height);
        backgroundGeometry = new Geometry("SliderColumnBackground", bgQuad);
        Material bgMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        bgMat.setColor("Color", new ColorRGBA(0.1f, 0.1f, 0.1f, 0.85f));
        bgMat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        backgroundGeometry.setMaterial(bgMat);
        backgroundGeometry.setLocalTranslation(x, y, 0);
        rootNode.attachChild(backgroundGeometry);

        // 重建标题栏背景
        if (titleBarGeometry != null) {
            titleBarGeometry.removeFromParent();
        }

        Quad titleBarQuad = new Quad(width, titleBarHeight);
        titleBarGeometry = new Geometry("SliderColumnTitleBar", titleBarQuad);
        Material titleBarMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        titleBarMat.setColor("Color", new ColorRGBA(0.05f, 0.05f, 0.08f, 0.95f));
        titleBarMat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        titleBarGeometry.setMaterial(titleBarMat);
        titleBarGeometry.setLocalTranslation(x, y + height - titleBarHeight, -0.5f);
        rootNode.attachChild(titleBarGeometry);
    }

    /**
     * 检查是否正在调整大小
     */
    public boolean isResizing() {
        return isResizing;
    }

    /**
     * 获取当前调整大小方向
     */
    public ResizeDirection getCurrentResizeDirection() {
        return resizeDirection;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    /**
     * 公开方法：手动触发UV调整
     * 用于在部件选择或初始化时调整UV比例
     */
    public void adjustUVToMatchPartAspectRatio() {
        adjustUVToMatchPartAspectRatioInternal();
    }

    /**
     * 手动设置当前部件尺寸（用于外部更新）
     */
    public void setCurrentPartDimensions(float width, float height) {
        this.currentPartWidth = width;
        this.currentPartHeight = height;
    }

    /**
     * 设置当前选中的部件渲染器（用于网格配置）
     */
    public void setCurrentPartRenderer(EditorPuppetPartRenderer partRenderer) {
        this.currentPartRenderer = partRenderer;
    }

    /**
     * 自动调整UV比例以匹配部件的宽高比，防止纹理拉伸
     * 当部件的宽度或高度改变时调用此方法
     */
    private void adjustUVToMatchPartAspectRatioInternal() {
        if (texturePreviewPanel == null || currentPartWidth <= 0 || currentPartHeight <= 0) {
            return;
        }

        // 获取当前纹理
        com.jme3.texture.Texture texture = texturePreviewPanel.getCurrentTexture();
        if (texture == null) {
            return;
        }

        // 计算部件的宽高比
        float partAspectRatio = currentPartWidth / currentPartHeight;

        // 计算纹理的宽高比
        int texWidth = texture.getImage().getWidth();
        int texHeight = texture.getImage().getHeight();
        float textureAspectRatio = (float) texWidth / texHeight;

        // 获取当前UV坐标
        float currentUvOffsetX = texturePreviewPanel.getUvOffsetX();
        float currentUvOffsetY = texturePreviewPanel.getUvOffsetY();
        float currentUvScaleX = texturePreviewPanel.getUvScaleX();
        float currentUvScaleY = texturePreviewPanel.getUvScaleY();

        // 保持UV区域的中心点不变
        float uvCenterX = currentUvOffsetX + currentUvScaleX / 2.0f;
        float uvCenterY = currentUvOffsetY + currentUvScaleY / 2.0f;

        // 计算新的UV比例，使得UV选择的纹理区域的实际宽高比与部件宽高比匹配
        // realAspectRatio = (uvScaleX * texWidth) / (uvScaleY * texHeight)
        // 我们想要 realAspectRatio = partAspectRatio
        // 所以: (uvScaleX * texWidth) / (uvScaleY * texHeight) = partAspectRatio
        // 即: uvScaleX / uvScaleY = partAspectRatio * (texHeight / texWidth)
        // 即: uvScaleX / uvScaleY = partAspectRatio / textureAspectRatio

        float targetUvAspectRatio = partAspectRatio / textureAspectRatio;
        float currentUvAspectRatio = currentUvScaleX / currentUvScaleY;

        float newUvScaleX, newUvScaleY;

        if (targetUvAspectRatio > currentUvAspectRatio) {
            // 需要增加X或减少Y
            newUvScaleX = currentUvScaleY * targetUvAspectRatio;
            newUvScaleY = currentUvScaleY;
        } else {
            // 需要增加Y或减少X
            newUvScaleX = currentUvScaleX;
            newUvScaleY = currentUvScaleX / targetUvAspectRatio;
        }

        // 限制UV范围
        newUvScaleX = Math.max(0.01f, newUvScaleX);
        newUvScaleY = Math.max(0.01f, newUvScaleY);

        // 根据新的比例重新计算起点，保持中心点不变
        float newUvOffsetX = uvCenterX - newUvScaleX / 2.0f;
        float newUvOffsetY = uvCenterY - newUvScaleY / 2.0f;

        // 确保不会出现负偏移（但允许超出1.0）
        newUvOffsetX = Math.max(0.0f, newUvOffsetX);
        newUvOffsetY = Math.max(0.0f, newUvOffsetY);

        // 更新UV坐标到预览面板
        texturePreviewPanel.setUV(newUvOffsetX, newUvOffsetY, newUvScaleX, newUvScaleY);

        // 触发回调，更新到渲染器
        if (callbacks != null) {
            callbacks.onUVChanged(newUvOffsetX, newUvOffsetY, newUvScaleX, newUvScaleY);
        }
    }

    /**
     * 辅助方法：创建滑条（支持中文）
     * 如果有TTF字体加载器，使用中文标签；否则使用英文标签
     */
    private Slider createSliderWithChinese(String chineseLabel, String englishLabel,
                                           float minValue, float maxValue, float initialValue,
                                           int x, int y) {
        if (ttfLoader != null) {
            return new Slider(app, ttfLoader, chineseLabel, minValue, maxValue, initialValue, x, y);
        } else {
            return new Slider(app, font, englishLabel, minValue, maxValue, initialValue, x, y);
        }
    }

    /**
     * 更新UI语言
     */
    public void updateLanguage() {
        LanguageManager langMgr = LanguageManager.getInstance();

        // 更新所有滑条标签
        if (widthSlider != null) {
            widthSlider.setText(langMgr.getText("width"));
        }
        if (heightSlider != null) {
            heightSlider.setText(langMgr.getText("height"));
        }
        if (prioritySlider != null) {
            prioritySlider.setText(langMgr.getText("priority"));
        }
        if (textureRotationSlider != null) {
            textureRotationSlider.setText(langMgr.getText("tex_rot"));
        }
        if (posXSlider != null) {
            posXSlider.setText(langMgr.getText("pos_x"));
        }
        if (posYSlider != null) {
            posYSlider.setText(langMgr.getText("pos_y"));
        }
        if (posZSlider != null) {
            posZSlider.setText(langMgr.getText("pos_z"));
        }
        if (rotXSlider != null) {
            rotXSlider.setText(langMgr.getText("rot_x"));
        }
        if (rotYSlider != null) {
            rotYSlider.setText(langMgr.getText("rot_y"));
        }
        if (rotZSlider != null) {
            rotZSlider.setText(langMgr.getText("rot_z"));
        }
        if (gridSizeSlider != null) {
            gridSizeSlider.setText(langMgr.getText("grid_size"));
        }
        if (freedomValueSlider != null) {
            freedomValueSlider.setText(langMgr.getText("freedom"));
        }

        // 更新按钮文本
        if (openEnlargedPreviewButton != null) {
            openEnlargedPreviewButton.setText(langMgr.getText("open_uv_editor"));
        }
    }
}
