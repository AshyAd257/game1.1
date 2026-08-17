package com.Hecate.puppet.editor;

import com.jme3.app.SimpleApplication;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Texture;
import com.Hecate.puppet.editor.core.EditorPuppetPartRenderer;

/**
 * 放大的纹理预览窗口
 * 用于更精确地选择UV区域
 */
public class EnlargedTexturePreviewPanel {

    private final SimpleApplication app;
    private final BitmapFont font;
    private final Node rootNode;

    // 窗口尺寸（占据大部分屏幕）
    private final int width = 1000;
    private final int height = 800;
    private int x, y;

    // 纹理预览
    private Geometry texturePreview;
    private Texture currentTexture;

    // 当前编辑的部件（用于读取和保存网格配置）
    private EditorPuppetPartRenderer currentPart;

    // 实际纹理显示区域（保持宽高比）
    private float actualDisplayWidth;
    private float actualDisplayHeight;
    private float actualDisplayX;
    private float actualDisplayY;

    // 选择框
    private Node selectionBox;
    private Geometry topLine, bottomLine, leftLine, rightLine;

    // 角点拖动手柄
    private Geometry topLeftHandle, topRightHandle, bottomLeftHandle, bottomRightHandle;

    // UV坐标（初始化为居中的较大方框）
    private float uvOffsetX = 0.25f;
    private float uvOffsetY = 0.25f;
    private float uvScaleX = 0.5f;
    private float uvScaleY = 0.5f;

    // 视口偏移（用于平移查看大图）
    private float viewportOffsetX = 0.0f;
    private float viewportOffsetY = 0.0f;

    // UV显示范围（可以>1.0以支持大纹理图集）
    private float maxUvRange = 2.0f; // 默认显示2倍范围

    // 缩放功能
    private float textureZoom = 1.0f; // 1.0 = 100%, 2.0 = 200%
    private static final float MIN_ZOOM = 0.5f;
    private static final float MAX_ZOOM = 4.0f;
    private static final float ZOOM_SPEED = 0.1f;

    // 右键平移功能
    private boolean isPanning = false;
    private int panStartX, panStartY;
    private float panStartViewportOffsetX, panStartViewportOffsetY;

    // 鼠标状态
    private boolean isDragging = false;
    private boolean isResizing = false;
    private int dragStartX, dragStartY;
    private float dragStartUvX, dragStartUvY;
    private float dragStartOffsetX, dragStartOffsetY;
    private float dragStartScaleX, dragStartScaleY;

    // 调整大小时的角点标识
    private enum ResizeHandle {
        NONE, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
    }
    private ResizeHandle currentHandle = ResizeHandle.NONE;

    // 角点拖动框大小（像素）
    private static final int HANDLE_SIZE = 12;

    // 编辑模式
    private enum EditMode {
        MOVE,    // 移动模式
        RESIZE,  // 调整大小模式（可改变形状）
        SCALE    // 缩放模式（保持宽高比）
    }
    private EditMode editMode = EditMode.RESIZE; // 默认调整大小模式

    // 用于保持宽高比
    private float aspectRatio = 1.0f;

    // UI元素
    private BitmapText uvInfoText;
    private BitmapText titleText;
    private BitmapText modeText;
    private Button closeButton;
    private Button modeToggleButton;
    private Geometry background;
    private Geometry modalOverlay;

    // 网格控制按钮
    private Button gridEnableButton;
    private Button gridHorizontalButton;
    private Button gridVerticalButton;
    private Button snapToGridButton;
    private Slider gridSizeSlider;

    // 滚动条
    private Slider horizontalScrollbar;
    private Slider verticalScrollbar;
    private boolean isScrolling = false;

    // 网格系统
    private boolean gridEnabled = false;
    private boolean gridHorizontal = true;
    private boolean gridVertical = true;
    private float gridSize = 32f;
    private boolean snapToGrid = false;
    private Node gridLinesNode;

    // UV精确控制按钮（点击输入数字）
    private Button uvOffsetXButton;
    private Button uvOffsetYButton;
    private Button uvScaleXButton;
    private Button uvScaleYButton;

    // 镜像按钮
    private Button flipHorizontalButton;
    private Button flipVerticalButton;

    // 回调
    private TexturePreviewPanel.UVChangeListener changeListener;

    public EnlargedTexturePreviewPanel(SimpleApplication app, BitmapFont font) {
        this.app = app;
        this.font = font;
        this.rootNode = new Node("EnlargedTexturePreviewPanel");

        // 窗口居中
        this.x = (app.getCamera().getWidth() - width) / 2;
        this.y = (app.getCamera().getHeight() - height) / 2;

        initialize();
        rootNode.setCullHint(Spatial.CullHint.Always); // 默认隐藏
    }

    private void initialize() {
        // 创建模态遮罩层（半透明黑色背景）
        Quad fullScreenQuad = new Quad(app.getCamera().getWidth(), app.getCamera().getHeight());
        modalOverlay = new Geometry("ModalOverlay", fullScreenQuad);
        Material overlayMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        overlayMat.setColor("Color", new ColorRGBA(0.0f, 0.0f, 0.0f, 0.7f));
        overlayMat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        modalOverlay.setMaterial(overlayMat);
        modalOverlay.setLocalTranslation(0, 0, 5.0f);
        rootNode.attachChild(modalOverlay);

        // 创建窗口背景
        Quad bgQuad = new Quad(width, height);
        background = new Geometry("EnlargedBackground", bgQuad);
        Material bgMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        bgMat.setColor("Color", new ColorRGBA(0.15f, 0.15f, 0.15f, 1.0f));
        background.setMaterial(bgMat);
        background.setLocalTranslation(x, y, 5.1f);
        rootNode.attachChild(background);

        // 创建标题栏
        titleText = new BitmapText(font);
        titleText.setText("UV Texture Atlas Selector - Double Click or Press Close to Exit");
        titleText.setSize(font.getCharSet().getRenderedSize());
        titleText.setColor(ColorRGBA.White);
        titleText.setLocalTranslation(x + 10, y + height - 10, 5.3f);
        rootNode.attachChild(titleText);

        // 创建关闭按钮
        closeButton = new Button(app, font, "Close", x + width - 110, y + height - 40, 100, 30);
        closeButton.setClickListener(new Button.ButtonClickListener() {
            @Override
            public void onClick() {
                hide();
            }
        });
        closeButton.getRootNode().setLocalTranslation(0, 0, 5.3f);
        rootNode.attachChild(closeButton.getRootNode());

        // 创建模式切换按钮
        modeToggleButton = new Button(app, font, "Mode: Resize", x + width - 230, y + height - 40, 110, 30);
        modeToggleButton.setClickListener(new Button.ButtonClickListener() {
            @Override
            public void onClick() {
                toggleEditMode();
            }
        });
        modeToggleButton.getRootNode().setLocalTranslation(0, 0, 5.3f);
        rootNode.attachChild(modeToggleButton.getRootNode());

        // 创建网格控制按钮（在模式按钮左边）
        int buttonWidth = 85;
        int buttonHeight = 30;
        int buttonSpacing = 10;
        int buttonY = y + height - 40;
        int buttonStartX = x + 10;

        // Grid Enable 按钮
        gridEnableButton = new Button(app, font, "Grid: OFF", buttonStartX, buttonY, buttonWidth, buttonHeight);
        gridEnableButton.setClickListener(new Button.ButtonClickListener() {
            @Override
            public void onClick() {
                if (currentPart != null) {
                    currentPart.setGridEnabled(!currentPart.isGridEnabled());
                    gridEnableButton.setText(currentPart.isGridEnabled() ? "Grid: ON" : "Grid: OFF");
                    updateGridLines();
                }
            }
        });
        gridEnableButton.getRootNode().setLocalTranslation(0, 0, 5.3f);
        rootNode.attachChild(gridEnableButton.getRootNode());
        buttonStartX += buttonWidth + buttonSpacing;

        // Horizontal Grid 按钮
        gridHorizontalButton = new Button(app, font, "H-Grid: ON", buttonStartX, buttonY, buttonWidth, buttonHeight);
        gridHorizontalButton.setClickListener(new Button.ButtonClickListener() {
            @Override
            public void onClick() {
                if (currentPart != null) {
                    currentPart.setGridHorizontal(!currentPart.isGridHorizontal());
                    gridHorizontalButton.setText(currentPart.isGridHorizontal() ? "H-Grid: ON" : "H-Grid: OFF");
                    updateGridLines();
                }
            }
        });
        gridHorizontalButton.getRootNode().setLocalTranslation(0, 0, 5.3f);
        rootNode.attachChild(gridHorizontalButton.getRootNode());
        buttonStartX += buttonWidth + buttonSpacing;

        // Vertical Grid 按钮
        gridVerticalButton = new Button(app, font, "V-Grid: ON", buttonStartX, buttonY, buttonWidth, buttonHeight);
        gridVerticalButton.setClickListener(new Button.ButtonClickListener() {
            @Override
            public void onClick() {
                if (currentPart != null) {
                    currentPart.setGridVertical(!currentPart.isGridVertical());
                    gridVerticalButton.setText(currentPart.isGridVertical() ? "V-Grid: ON" : "V-Grid: OFF");
                    updateGridLines();
                }
            }
        });
        gridVerticalButton.getRootNode().setLocalTranslation(0, 0, 5.3f);
        rootNode.attachChild(gridVerticalButton.getRootNode());
        buttonStartX += buttonWidth + buttonSpacing;

        // Snap to Grid 按钮
        snapToGridButton = new Button(app, font, "Snap: OFF", buttonStartX, buttonY, buttonWidth, buttonHeight);
        snapToGridButton.setClickListener(new Button.ButtonClickListener() {
            @Override
            public void onClick() {
                if (currentPart != null) {
                    currentPart.setSnapToGrid(!currentPart.isSnapToGrid());
                    snapToGridButton.setText(currentPart.isSnapToGrid() ? "Snap: ON" : "Snap: OFF");
                }
            }
        });
        snapToGridButton.getRootNode().setLocalTranslation(0, 0, 5.3f);
        rootNode.attachChild(snapToGridButton.getRootNode());
        buttonStartX += buttonWidth + buttonSpacing;

        // Grid Size 滑条（在按钮右边）
        gridSizeSlider = new Slider(app, font, "Size", 8f, 128f, 32f, buttonStartX, buttonY);
        gridSizeSlider.setChangeListener(new Slider.SliderChangeListener() {
            @Override
            public void onValueChanged(float value) {
                if (currentPart != null) {
                    currentPart.setGridSize(value);
                    updateGridLines();
                }
            }
        });
        gridSizeSlider.getRootNode().setLocalTranslation(0, 0, 5.3f);
        rootNode.attachChild(gridSizeSlider.getRootNode());

        // 镜像按钮（在按钮行的最右边）
        int mirrorButtonY = y + height - 80;
        int mirrorButtonWidth = 85;

        // 水平镜像按钮
        flipHorizontalButton = new Button(app, font, "Flip H", x + width - 190, mirrorButtonY, mirrorButtonWidth, buttonHeight);
        flipHorizontalButton.setClickListener(new Button.ButtonClickListener() {
            @Override
            public void onClick() {
                flipHorizontal();
            }
        });
        flipHorizontalButton.getRootNode().setLocalTranslation(0, 0, 5.3f);
        rootNode.attachChild(flipHorizontalButton.getRootNode());

        // 垂直镜像按钮
        flipVerticalButton = new Button(app, font, "Flip V", x + width - 95, mirrorButtonY, mirrorButtonWidth, buttonHeight);
        flipVerticalButton.setClickListener(new Button.ButtonClickListener() {
            @Override
            public void onClick() {
                flipVertical();
            }
        });
        flipVerticalButton.getRootNode().setLocalTranslation(0, 0, 5.3f);
        rootNode.attachChild(flipVerticalButton.getRootNode());

        // 创建模式提示文本
        modeText = new BitmapText(font);
        modeText.setText("[RESIZE MODE] Click corners to resize | Click inside to move");
        modeText.setSize(font.getCharSet().getRenderedSize() * 0.85f);
        modeText.setColor(new ColorRGBA(1.0f, 0.8f, 0.2f, 1.0f)); // 黄色
        modeText.setLocalTranslation(x + 10, y + height - 90, 5.3f);
        rootNode.attachChild(modeText);

        // 创建纹理预览区域（留出标题栏和底部信息栏）
        int previewWidth = width - 20;
        int previewHeight = height - 80;
        int previewX = x + 10;
        int previewY = y + 40;

        Quad texQuad = new Quad(previewWidth, previewHeight);
        texturePreview = new Geometry("EnlargedTexturePreview", texQuad);
        Material texMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        texMat.setColor("Color", ColorRGBA.White);
        texMat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        texturePreview.setMaterial(texMat);
        texturePreview.setLocalTranslation(previewX, previewY, 5.2f);

        // 设置默认纹理坐标
        texQuad.setBuffer(com.jme3.scene.VertexBuffer.Type.TexCoord, 2, new float[]{
            0, 0,  // 左下
            1, 0,  // 右下
            1, 1,  // 右上
            0, 1   // 左上
        });
        rootNode.attachChild(texturePreview);

        // 创建网格线容器
        gridLinesNode = new Node("GridLines");
        gridLinesNode.setLocalTranslation(0, 0, 5.21f);
        rootNode.attachChild(gridLinesNode);
        updateGridLines();

        // 创建选择框
        selectionBox = createBoxOutline(previewWidth, previewHeight);
        selectionBox.setLocalTranslation(0, 0, 5.25f);
        rootNode.attachChild(selectionBox);

        // 创建角点手柄
        createResizeHandles();

        // 创建UV信息文本
        uvInfoText = new BitmapText(font);
        uvInfoText.setText("UV: 0.00, 0.00 | 1.00 x 1.00");
        uvInfoText.setSize(font.getCharSet().getRenderedSize() * 0.9f);
        uvInfoText.setColor(ColorRGBA.White);
        uvInfoText.setLocalTranslation(x + 10, y + 30, 5.3f);
        rootNode.attachChild(uvInfoText);

        // 创建滚动条
        createScrollbars(previewX, previewY, previewWidth, previewHeight);

        updateSelectionBox();
    }

    private void createScrollbars(int previewX, int previewY, int previewWidth, int previewHeight) {
        // 水平滚动条（底部）
        horizontalScrollbar = new Slider(app, font, "H-Scroll",
            0.0f, maxUvRange - 1.0f, viewportOffsetX,
            previewX, previewY - 30);
        horizontalScrollbar.updateTrackWidth(previewWidth);
        horizontalScrollbar.setChangeListener(new Slider.SliderChangeListener() {
            @Override
            public void onValueChanged(float value) {
                viewportOffsetX = value;
                updateTextureViewport();
                updateSelectionBox();
            }
        });
        horizontalScrollbar.getRootNode().setLocalTranslation(0, 0, 5.3f);
        rootNode.attachChild(horizontalScrollbar.getRootNode());

        // 垂直滚动条（右侧）
        int scrollbarWidth = 20;
        verticalScrollbar = new Slider(app, font, "V-Scroll",
            0.0f, maxUvRange - 1.0f, viewportOffsetY,
            previewX + previewWidth + 10, previewY);
        verticalScrollbar.updateTrackWidth(scrollbarWidth);
        verticalScrollbar.setChangeListener(new Slider.SliderChangeListener() {
            @Override
            public void onValueChanged(float value) {
                viewportOffsetY = value;
                updateTextureViewport();
                updateSelectionBox();
            }
        });
        verticalScrollbar.getRootNode().setLocalTranslation(0, 0, 5.3f);
        rootNode.attachChild(verticalScrollbar.getRootNode());

        // UV精确控制按钮（点击输入数字）
        int buttonWidth = (previewWidth - 40) / 4;  // 4个按钮平分宽度
        int buttonHeight = 30;
        int buttonY = previewY - 70;  // 在水平滚动条下方

        // UV Offset X 按钮
        uvOffsetXButton = new Button(app, font,
            String.format("UVX: %.3f", uvOffsetX),
            previewX, buttonY, buttonWidth, buttonHeight);
        uvOffsetXButton.setClickListener(new Button.ButtonClickListener() {
            @Override
            public void onClick() {
                openNumericInputDialog("UV Offset X", uvOffsetX, 0.0f, 2.0f, (value) -> {
                    uvOffsetX = value;
                    uvOffsetXButton.setText(String.format("UVX: %.3f", uvOffsetX));
                    updateSelectionBox();
                    if (changeListener != null) {
                        changeListener.onUVChanged(uvOffsetX, uvOffsetY, uvScaleX, uvScaleY);
                    }
                });
            }
        });
        uvOffsetXButton.getRootNode().setLocalTranslation(0, 0, 5.3f);
        rootNode.attachChild(uvOffsetXButton.getRootNode());

        // UV Offset Y 按钮
        uvOffsetYButton = new Button(app, font,
            String.format("UVY: %.3f", uvOffsetY),
            previewX + buttonWidth + 10, buttonY, buttonWidth, buttonHeight);
        uvOffsetYButton.setClickListener(new Button.ButtonClickListener() {
            @Override
            public void onClick() {
                openNumericInputDialog("UV Offset Y", uvOffsetY, 0.0f, 2.0f, (value) -> {
                    uvOffsetY = value;
                    uvOffsetYButton.setText(String.format("UVY: %.3f", uvOffsetY));
                    updateSelectionBox();
                    if (changeListener != null) {
                        changeListener.onUVChanged(uvOffsetX, uvOffsetY, uvScaleX, uvScaleY);
                    }
                });
            }
        });
        uvOffsetYButton.getRootNode().setLocalTranslation(0, 0, 5.3f);
        rootNode.attachChild(uvOffsetYButton.getRootNode());

        // UV Scale X 按钮
        uvScaleXButton = new Button(app, font,
            String.format("ScaleX: %.3f", uvScaleX),
            previewX + (buttonWidth + 10) * 2, buttonY, buttonWidth, buttonHeight);
        uvScaleXButton.setClickListener(new Button.ButtonClickListener() {
            @Override
            public void onClick() {
                openNumericInputDialog("UV Scale X", uvScaleX, 0.01f, 2.0f, (value) -> {
                    uvScaleX = Math.max(0.01f, value);
                    uvScaleXButton.setText(String.format("ScaleX: %.3f", uvScaleX));
                    updateSelectionBox();
                    if (changeListener != null) {
                        changeListener.onUVChanged(uvOffsetX, uvOffsetY, uvScaleX, uvScaleY);
                    }
                });
            }
        });
        uvScaleXButton.getRootNode().setLocalTranslation(0, 0, 5.3f);
        rootNode.attachChild(uvScaleXButton.getRootNode());

        // UV Scale Y 按钮
        uvScaleYButton = new Button(app, font,
            String.format("ScaleY: %.3f", uvScaleY),
            previewX + (buttonWidth + 10) * 3, buttonY, buttonWidth, buttonHeight);
        uvScaleYButton.setClickListener(new Button.ButtonClickListener() {
            @Override
            public void onClick() {
                openNumericInputDialog("UV Scale Y", uvScaleY, 0.01f, 2.0f, (value) -> {
                    uvScaleY = Math.max(0.01f, value);
                    uvScaleYButton.setText(String.format("ScaleY: %.3f", uvScaleY));
                    updateSelectionBox();
                    if (changeListener != null) {
                        changeListener.onUVChanged(uvOffsetX, uvOffsetY, uvScaleX, uvScaleY);
                    }
                });
            }
        });
        uvScaleYButton.getRootNode().setLocalTranslation(0, 0, 5.3f);
        rootNode.attachChild(uvScaleYButton.getRootNode());
    }

    private void updateTextureViewport() {
        if (texturePreview == null) {
            return;
        }

        // 更新纹理显示的UV范围（考虑缩放）
        Quad texQuad = (Quad) texturePreview.getMesh();
        float u0 = viewportOffsetX;
        float v0 = viewportOffsetY;
        float u1 = viewportOffsetX + (1.0f / textureZoom);
        float v1 = viewportOffsetY + (1.0f / textureZoom);

        texQuad.setBuffer(com.jme3.scene.VertexBuffer.Type.TexCoord, 2, new float[]{
            u0, v0,  // 左下
            u1, v0,  // 右下
            u1, v1,  // 右上
            u0, v1   // 左上
        });
    }

    private Node createBoxOutline(int maxWidth, int maxHeight) {
        Node boxNode = new Node("EnlargedSelectionBox");

        Material lineMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        lineMat.setColor("Color", new ColorRGBA(1.0f, 1.0f, 1.0f, 1.0f));

        int lineWidth = 2;

        topLine = new Geometry("TopLine", new Quad(maxWidth, lineWidth));
        topLine.setMaterial(lineMat);
        boxNode.attachChild(topLine);

        bottomLine = new Geometry("BottomLine", new Quad(maxWidth, lineWidth));
        bottomLine.setMaterial(lineMat);
        boxNode.attachChild(bottomLine);

        leftLine = new Geometry("LeftLine", new Quad(lineWidth, maxHeight));
        leftLine.setMaterial(lineMat);
        boxNode.attachChild(leftLine);

        rightLine = new Geometry("RightLine", new Quad(lineWidth, maxHeight));
        rightLine.setMaterial(lineMat);
        boxNode.attachChild(rightLine);

        return boxNode;
    }

    private void createResizeHandles() {
        Material handleMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        handleMat.setColor("Color", new ColorRGBA(1.0f, 0.5f, 0.0f, 1.0f)); // 橙色手柄
        handleMat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);

        Quad handleQuad = new Quad(HANDLE_SIZE, HANDLE_SIZE);

        topLeftHandle = new Geometry("TopLeftHandle", handleQuad);
        topLeftHandle.setMaterial(handleMat.clone());
        topLeftHandle.setLocalTranslation(0, 0, 5.3f);
        rootNode.attachChild(topLeftHandle);

        topRightHandle = new Geometry("TopRightHandle", handleQuad);
        topRightHandle.setMaterial(handleMat.clone());
        topRightHandle.setLocalTranslation(0, 0, 5.3f);
        rootNode.attachChild(topRightHandle);

        bottomLeftHandle = new Geometry("BottomLeftHandle", handleQuad);
        bottomLeftHandle.setMaterial(handleMat.clone());
        bottomLeftHandle.setLocalTranslation(0, 0, 5.3f);
        rootNode.attachChild(bottomLeftHandle);

        bottomRightHandle = new Geometry("BottomRightHandle", handleQuad);
        bottomRightHandle.setMaterial(handleMat.clone());
        bottomRightHandle.setLocalTranslation(0, 0, 5.3f);
        rootNode.attachChild(bottomRightHandle);
    }

    /**
     * 设置当前编辑的部件
     * @param part 部件渲染器
     */
    public void setCurrentPart(EditorPuppetPartRenderer part) {
        this.currentPart = part;
        // 从部件加载网格配置
        if (part != null) {
            // 更新按钮状态以反映部件的网格配置
            updateGridButtonsFromPart();
        }
    }

    /**
     * 从当前部件更新网格按钮状态
     */
    private void updateGridButtonsFromPart() {
        if (currentPart == null) return;

        // 更新按钮文本以反映当前状态
        if (gridEnableButton != null) {
            gridEnableButton.setText("Grid: " + (currentPart.isGridEnabled() ? "ON" : "OFF"));
        }
        if (gridHorizontalButton != null) {
            gridHorizontalButton.setText("H-Grid: " + (currentPart.isGridHorizontal() ? "ON" : "OFF"));
        }
        if (gridVerticalButton != null) {
            gridVerticalButton.setText("V-Grid: " + (currentPart.isGridVertical() ? "ON" : "OFF"));
        }
        if (snapToGridButton != null) {
            snapToGridButton.setText("Snap: " + (currentPart.isSnapToGrid() ? "ON" : "OFF"));
        }
        if (gridSizeSlider != null) {
            gridSizeSlider.setValue(currentPart.getGridSize());
        }

        // 更新网格显示
        updateGridLines();
    }

    public void setTexture(Texture texture) {
        this.currentTexture = texture;
        if (texture != null) {
            Material mat = texturePreview.getMaterial();
            mat.setTexture("ColorMap", texture);
            mat.setColor("Color", ColorRGBA.White);

            // 根据纹理的宽高比调整显示区域，保持纹理不拉伸
            int texWidth = texture.getImage().getWidth();
            int texHeight = texture.getImage().getHeight();
            float textureAspectRatio = (float) texWidth / texHeight;

            // 可用的预览区域
            int previewWidth = width - 20;
            int previewHeight = height - 80;
            float previewAspectRatio = (float) previewWidth / previewHeight;

            // 计算实际显示尺寸（保持纹理宽高比）
            float displayWidth, displayHeight;
            if (textureAspectRatio > previewAspectRatio) {
                // 纹理更宽，以宽度为准
                displayWidth = previewWidth;
                displayHeight = previewWidth / textureAspectRatio;
            } else {
                // 纹理更高，以高度为准
                displayHeight = previewHeight;
                displayWidth = previewHeight * textureAspectRatio;
            }

            // 居中显示
            int previewX = x + 10;
            int previewY = y + 40;
            float offsetX = (previewWidth - displayWidth) / 2;
            float offsetY = (previewHeight - displayHeight) / 2;

            // 保存实际显示区域
            this.actualDisplayWidth = displayWidth;
            this.actualDisplayHeight = displayHeight;
            this.actualDisplayX = previewX + offsetX;
            this.actualDisplayY = previewY + offsetY;

            // 更新Quad的大小和位置
            Quad newQuad = new Quad(displayWidth, displayHeight);

            // 设置纹理坐标（在setMesh之前）
            float u0 = viewportOffsetX;
            float v0 = viewportOffsetY;
            float u1 = viewportOffsetX + 1.0f;
            float v1 = viewportOffsetY + 1.0f;
            newQuad.setBuffer(com.jme3.scene.VertexBuffer.Type.TexCoord, 2, new float[]{
                u0, v0,  // 左下
                u1, v0,  // 右下
                u1, v1,  // 右上
                u0, v1   // 左上
            });

            texturePreview.setMesh(newQuad);
            texturePreview.setLocalTranslation(actualDisplayX, actualDisplayY, 5.2f);

            // 更新选择框
            updateSelectionBox();
        }
    }

    public void setUV(float offsetX, float offsetY, float scaleX, float scaleY) {
        this.uvOffsetX = Math.max(0.0f, Math.min(1.0f, offsetX));
        this.uvOffsetY = Math.max(0.0f, Math.min(1.0f, offsetY));
        this.uvScaleX = Math.max(0.01f, Math.min(1.0f, scaleX));
        this.uvScaleY = Math.max(0.01f, Math.min(1.0f, scaleY));

        if (this.uvOffsetX + this.uvScaleX > 1.0f) {
            this.uvOffsetX = 1.0f - this.uvScaleX;
        }
        if (this.uvOffsetY + this.uvScaleY > 1.0f) {
            this.uvOffsetY = 1.0f - this.uvScaleY;
        }

        updateSelectionBox();
    }

    /**
     * 根据部件的实际宽高设置UV选框，使其匹配部件的宽高比
     * @param partWidth 部件宽度
     * @param partHeight 部件高度
     */
    public void setPartDimensions(float partWidth, float partHeight) {
        // 计算部件的宽高比
        float partAspectRatio = partWidth / partHeight;

        // 根据部件宽高比调整UV选框
        // 保持居中，调整宽高使其匹配部件比例
        if (partAspectRatio > 1.0f) {
            // 宽度大于高度（横向矩形）
            uvScaleX = 0.5f;
            uvScaleY = 0.5f / partAspectRatio;
        } else {
            // 高度大于宽度（竖向矩形）
            uvScaleX = 0.5f * partAspectRatio;
            uvScaleY = 0.5f;
        }

        // 居中显示
        uvOffsetX = 0.5f - uvScaleX / 2.0f;
        uvOffsetY = 0.5f - uvScaleY / 2.0f;

        // 保存宽高比以供SCALE模式使用
        aspectRatio = partAspectRatio;

        updateSelectionBox();
    }

    /**
     * 获取实际的纹理显示区域尺寸和位置
     * @return [displayX, displayY, displayWidth, displayHeight]
     */
    private float[] getDisplayBounds() {
        float displayWidth = actualDisplayWidth > 0 ? actualDisplayWidth : width - 20;
        float displayHeight = actualDisplayHeight > 0 ? actualDisplayHeight : height - 80;
        float displayX = actualDisplayX > 0 ? actualDisplayX : x + 10;
        float displayY = actualDisplayY > 0 ? actualDisplayY : y + 40;
        return new float[]{displayX, displayY, displayWidth, displayHeight};
    }

    private void updateSelectionBox() {
        float[] bounds = getDisplayBounds();
        float displayX = bounds[0];
        float displayY = bounds[1];
        float displayWidth = bounds[2];
        float displayHeight = bounds[3];

        // 获取纹理的实际宽高比
        float textureAspectRatio = 1.0f;
        int texWidth = 1;
        int texHeight = 1;
        if (currentTexture != null && currentTexture.getImage() != null) {
            texWidth = currentTexture.getImage().getWidth();
            texHeight = currentTexture.getImage().getHeight();
            textureAspectRatio = (float) texWidth / texHeight;
        }

        // 考虑视口偏移和缩放
        float relativeUvX = uvOffsetX - viewportOffsetX;
        float relativeUvY = uvOffsetY - viewportOffsetY;

        // **修复：需要考虑缩放级别**
        // 缩放时，显示的UV范围变小（1/zoom），所以屏幕空间的UV要乘以zoom
        float effectiveDisplayWidth = displayWidth * textureZoom;
        float effectiveDisplayHeight = displayHeight * textureZoom;

        float boxX = displayX + relativeUvX * effectiveDisplayWidth;
        float boxY = displayY + relativeUvY * effectiveDisplayHeight;
        float boxWidth = uvScaleX * effectiveDisplayWidth;
        float boxHeight = uvScaleY * effectiveDisplayHeight;

        // 计算实际像素尺寸（用于调试）
        float actualPixelsWidth = uvScaleX * texWidth;
        float actualPixelsHeight = uvScaleY * texHeight;
        float pixelAspectRatio = actualPixelsHeight > 0 ? actualPixelsWidth / actualPixelsHeight : 1.0f;

        int lineWidth = 2;

        topLine.setMesh(new Quad(boxWidth, lineWidth));
        topLine.setLocalTranslation(boxX, boxY + boxHeight - lineWidth, 0);

        bottomLine.setMesh(new Quad(boxWidth, lineWidth));
        bottomLine.setLocalTranslation(boxX, boxY, 0);

        leftLine.setMesh(new Quad(lineWidth, boxHeight));
        leftLine.setLocalTranslation(boxX, boxY, 0);

        rightLine.setMesh(new Quad(lineWidth, boxHeight));
        rightLine.setLocalTranslation(boxX + boxWidth - lineWidth, boxY, 0);

        String uvText = String.format("UV: %.3f, %.3f | %.3f x %.3f  |  Pixels: %.0fx%.0f (%.2f:1)  |  Texture: %dx%d",
            uvOffsetX, uvOffsetY, uvScaleX, uvScaleY,
            actualPixelsWidth, actualPixelsHeight, pixelAspectRatio,
            texWidth, texHeight);
        uvInfoText.setText(uvText);

        // 更新手动输入按钮的文本
        if (uvOffsetXButton != null) {
            uvOffsetXButton.setText(String.format("UVX: %.3f", uvOffsetX));
        }
        if (uvOffsetYButton != null) {
            uvOffsetYButton.setText(String.format("UVY: %.3f", uvOffsetY));
        }
        if (uvScaleXButton != null) {
            uvScaleXButton.setText(String.format("ScaleX: %.3f", uvScaleX));
        }
        if (uvScaleYButton != null) {
            uvScaleYButton.setText(String.format("ScaleY: %.3f", uvScaleY));
        }

        // 更新角点手柄位置
        updateHandlePositions(boxX, boxY, boxWidth, boxHeight);
    }

    private void updateHandlePositions(float boxX, float boxY, float boxWidth, float boxHeight) {
        int halfHandle = HANDLE_SIZE / 2;

        // 左上角
        topLeftHandle.setLocalTranslation(boxX - halfHandle, boxY + boxHeight - halfHandle, 5.3f);

        // 右上角
        topRightHandle.setLocalTranslation(boxX + boxWidth - halfHandle, boxY + boxHeight - halfHandle, 5.3f);

        // 左下角
        bottomLeftHandle.setLocalTranslation(boxX - halfHandle, boxY - halfHandle, 5.3f);

        // 右下角
        bottomRightHandle.setLocalTranslation(boxX + boxWidth - halfHandle, boxY - halfHandle, 5.3f);
    }

    public boolean handleMousePress(int mouseX, int mouseY) {
        return handleMousePress(mouseX, mouseY, 0); // 默认左键
    }

    public boolean handleMousePress(int mouseX, int mouseY, int button) {
        // button: 0 = 左键, 1 = 右键, 2 = 中键

        // 检查是否右键点击（用于平移）
        if (button == 1) {
            // 检查是否在预览区域内
            float[] bounds = getDisplayBounds();
            float displayX = bounds[0];
            float displayY = bounds[1];
            float displayWidth = bounds[2];
            float displayHeight = bounds[3];

            if (mouseX >= displayX && mouseX <= displayX + displayWidth &&
                mouseY >= displayY && mouseY <= displayY + displayHeight) {
                isPanning = true;
                panStartX = mouseX;
                panStartY = mouseY;
                panStartViewportOffsetX = viewportOffsetX;
                panStartViewportOffsetY = viewportOffsetY;
                return true;
            }
        }

        // 检查是否点击关闭按钮
        if (closeButton.handleMouseClick(mouseX, mouseY)) {
            return true;
        }

        // 检查是否点击模式切换按钮
        if (modeToggleButton.handleMouseClick(mouseX, mouseY)) {
            return true;
        }

        // 检查网格控制按钮
        if (gridEnableButton != null && gridEnableButton.handleMouseClick(mouseX, mouseY)) {
            return true;
        }
        if (gridHorizontalButton != null && gridHorizontalButton.handleMouseClick(mouseX, mouseY)) {
            return true;
        }
        if (gridVerticalButton != null && gridVerticalButton.handleMouseClick(mouseX, mouseY)) {
            return true;
        }
        if (snapToGridButton != null && snapToGridButton.handleMouseClick(mouseX, mouseY)) {
            return true;
        }

        // 检查网格大小滑条
        if (gridSizeSlider != null && gridSizeSlider.handleMouseClick(mouseX, mouseY)) {
            return true;
        }

        // 检查滚动条交互
        if (horizontalScrollbar != null && horizontalScrollbar.handleMouseClick(mouseX, mouseY)) {
            isScrolling = true;
            return true;
        }
        if (verticalScrollbar != null && verticalScrollbar.handleMouseClick(mouseX, mouseY)) {
            isScrolling = true;
            return true;
        }

        // 检查UV控制按钮交互
        if (uvOffsetXButton != null && uvOffsetXButton.handleMouseClick(mouseX, mouseY)) {
            return true;
        }
        if (uvOffsetYButton != null && uvOffsetYButton.handleMouseClick(mouseX, mouseY)) {
            return true;
        }
        if (uvScaleXButton != null && uvScaleXButton.handleMouseClick(mouseX, mouseY)) {
            return true;
        }
        if (uvScaleYButton != null && uvScaleYButton.handleMouseClick(mouseX, mouseY)) {
            return true;
        }

        // 检查是否在预览区域内
        float[] bounds = getDisplayBounds();
        float displayX = bounds[0];
        float displayY = bounds[1];
        float displayWidth = bounds[2];
        float displayHeight = bounds[3];

        if (mouseX >= displayX && mouseX <= displayX + displayWidth &&
            mouseY >= displayY && mouseY <= displayY + displayHeight) {

            // 计算当前选择框的屏幕坐标（考虑视口偏移和缩放）
            float relativeUvX = uvOffsetX - viewportOffsetX;
            float relativeUvY = uvOffsetY - viewportOffsetY;

            // **修复：需要考虑缩放级别**
            float effectiveDisplayWidth = displayWidth * textureZoom;
            float effectiveDisplayHeight = displayHeight * textureZoom;

            float boxX = displayX + relativeUvX * effectiveDisplayWidth;
            float boxY = displayY + relativeUvY * effectiveDisplayHeight;
            float boxWidth = uvScaleX * effectiveDisplayWidth;
            float boxHeight = uvScaleY * effectiveDisplayHeight;

            if (editMode == EditMode.RESIZE || editMode == EditMode.SCALE) {
                // ===== RESIZE 或 SCALE 模式 =====
                // 检查是否点击了角点手柄
                currentHandle = getHandleAtPosition(mouseX, mouseY, boxX, boxY, boxWidth, boxHeight);

                if (currentHandle != ResizeHandle.NONE) {
                    // 开始调整大小
                    isResizing = true;
                    dragStartX = mouseX;
                    dragStartY = mouseY;
                    dragStartOffsetX = uvOffsetX;
                    dragStartOffsetY = uvOffsetY;
                    dragStartScaleX = uvScaleX;
                    dragStartScaleY = uvScaleY;
                    // 在SCALE模式下，保存当前宽高比
                    if (editMode == EditMode.SCALE) {
                        aspectRatio = uvScaleX / uvScaleY;
                    }
                    return true;
                }

                // 检查是否点击在选择框内部（拖动）
                if (mouseX >= boxX && mouseX <= boxX + boxWidth &&
                    mouseY >= boxY && mouseY <= boxY + boxHeight) {
                    isDragging = true;
                    dragStartX = mouseX;
                    dragStartY = mouseY;
                    dragStartOffsetX = uvOffsetX;
                    dragStartOffsetY = uvOffsetY;
                    return true;
                }

            } else if (editMode == EditMode.MOVE) {
                // ===== MOVE 模式 =====
                // 只检查是否点击在选择框内部（只能拖动）
                if (mouseX >= boxX && mouseX <= boxX + boxWidth &&
                    mouseY >= boxY && mouseY <= boxY + boxHeight) {
                    isDragging = true;
                    dragStartX = mouseX;
                    dragStartY = mouseY;
                    dragStartOffsetX = uvOffsetX;
                    dragStartOffsetY = uvOffsetY;
                    return true;
                }
            }

            // 在空白处点击不创建新选择框，保持当前选择
            return true;
        }

        return false;
    }

    private ResizeHandle getHandleAtPosition(int mouseX, int mouseY, float boxX, float boxY, float boxWidth, float boxHeight) {
        int halfHandle = HANDLE_SIZE / 2;
        int tolerance = HANDLE_SIZE;

        // 检查左上角
        if (isPointNear(mouseX, mouseY, boxX - halfHandle + halfHandle, boxY + boxHeight - halfHandle + halfHandle, tolerance)) {
            return ResizeHandle.TOP_LEFT;
        }

        // 检查右上角
        if (isPointNear(mouseX, mouseY, boxX + boxWidth - halfHandle + halfHandle, boxY + boxHeight - halfHandle + halfHandle, tolerance)) {
            return ResizeHandle.TOP_RIGHT;
        }

        // 检查左下角
        if (isPointNear(mouseX, mouseY, boxX - halfHandle + halfHandle, boxY - halfHandle + halfHandle, tolerance)) {
            return ResizeHandle.BOTTOM_LEFT;
        }

        // 检查右下角
        if (isPointNear(mouseX, mouseY, boxX + boxWidth - halfHandle + halfHandle, boxY - halfHandle + halfHandle, tolerance)) {
            return ResizeHandle.BOTTOM_RIGHT;
        }

        return ResizeHandle.NONE;
    }

    private boolean isPointNear(int px, int py, float cx, float cy, int tolerance) {
        return Math.abs(px - cx) <= tolerance && Math.abs(py - cy) <= tolerance;
    }

    private void toggleEditMode() {
        // 三种模式循环切换: RESIZE -> SCALE -> MOVE -> RESIZE
        switch (editMode) {
            case RESIZE:
                editMode = EditMode.SCALE;
                modeToggleButton.setText("Mode: Scale");
                modeText.setText("[SCALE MODE] Drag corners to scale proportionally | Drag inside to move");
                // 计算并保存当前宽高比
                aspectRatio = uvScaleX / uvScaleY;
                // 显示手柄
                setHandlesVisible(true);
                break;

            case SCALE:
                editMode = EditMode.MOVE;
                modeToggleButton.setText("Mode: Move");
                modeText.setText("[MOVE MODE] Click and drag to move the selection box");
                // 隐藏手柄
                setHandlesVisible(false);
                break;

            case MOVE:
                editMode = EditMode.RESIZE;
                modeToggleButton.setText("Mode: Resize");
                modeText.setText("[RESIZE MODE] Click corners to resize freely | Click inside to move");
                // 显示手柄
                setHandlesVisible(true);
                break;
        }
    }

    private void setHandlesVisible(boolean visible) {
        Spatial.CullHint hint = visible ? Spatial.CullHint.Never : Spatial.CullHint.Always;
        topLeftHandle.setCullHint(hint);
        topRightHandle.setCullHint(hint);
        bottomLeftHandle.setCullHint(hint);
        bottomRightHandle.setCullHint(hint);
    }

    public boolean handleMouseDrag(int mouseX, int mouseY) {
        // 处理右键平移
        if (isPanning) {
            int deltaX = mouseX - panStartX;
            int deltaY = mouseY - panStartY;

            float[] bounds = getDisplayBounds();
            float displayWidth = bounds[2];
            float displayHeight = bounds[3];

            // 将像素偏移转换为UV偏移（考虑缩放）
            float uvDeltaX = -(deltaX / displayWidth) / textureZoom;
            float uvDeltaY = -(deltaY / displayHeight) / textureZoom;

            viewportOffsetX = panStartViewportOffsetX + uvDeltaX;
            viewportOffsetY = panStartViewportOffsetY + uvDeltaY;

            // 限制平移范围
            viewportOffsetX = Math.max(-maxUvRange, Math.min(maxUvRange, viewportOffsetX));
            viewportOffsetY = Math.max(-maxUvRange, Math.min(maxUvRange, viewportOffsetY));

            updateTextureViewport();
            updateSelectionBox();
            updateGridLines();
            return true;
        }

        // 处理网格大小滑条拖动
        if (gridSizeSlider != null && gridSizeSlider.isDragging()) {
            gridSizeSlider.handleMouseDrag(mouseX, mouseY);
            return true;
        }

        // 优先处理滚动条拖动
        if (isScrolling) {
            if (horizontalScrollbar != null && horizontalScrollbar.isDragging()) {
                horizontalScrollbar.handleMouseDrag(mouseX, mouseY);
                return true;
            }
            if (verticalScrollbar != null && verticalScrollbar.isDragging()) {
                verticalScrollbar.handleMouseDrag(mouseX, mouseY);
                return true;
            }
            return false;
        }

        float[] bounds = getDisplayBounds();
        float displayX = bounds[0];
        float displayY = bounds[1];
        float displayWidth = bounds[2];
        float displayHeight = bounds[3];

        if (isResizing) {
            int deltaX = mouseX - dragStartX;
            int deltaY = mouseY - dragStartY;

            // **修复：需要考虑缩放级别**
            float effectiveDisplayWidth = displayWidth * textureZoom;
            float effectiveDisplayHeight = displayHeight * textureZoom;

            float deltaUvX = deltaX / effectiveDisplayWidth;
            float deltaUvY = deltaY / effectiveDisplayHeight;

            if (editMode == EditMode.SCALE) {
                // ===== SCALE 模式：保持宽高比缩放 =====
                // 使用对角线距离来计算缩放因子
                float scaleFactor = 1.0f;

                switch (currentHandle) {
                    case TOP_LEFT:
                        // 左上角：向内缩放
                        scaleFactor = 1.0f - Math.max(deltaUvX / dragStartScaleX, deltaUvY / dragStartScaleY);
                        uvScaleX = Math.max(0.01f, dragStartScaleX * scaleFactor);
                        uvScaleY = uvScaleX / aspectRatio;
                        uvOffsetX = dragStartOffsetX + dragStartScaleX - uvScaleX;
                        uvOffsetY = dragStartOffsetY + dragStartScaleY - uvScaleY;
                        break;

                    case TOP_RIGHT:
                        // 右上角：右侧和上侧缩放
                        scaleFactor = 1.0f + Math.max(deltaUvX / dragStartScaleX, deltaUvY / dragStartScaleY);
                        uvScaleX = Math.max(0.01f, dragStartScaleX * scaleFactor);
                        uvScaleY = uvScaleX / aspectRatio;
                        uvOffsetX = dragStartOffsetX;
                        uvOffsetY = dragStartOffsetY + dragStartScaleY - uvScaleY;
                        break;

                    case BOTTOM_LEFT:
                        // 左下角：左侧和下侧缩放
                        scaleFactor = 1.0f - Math.max(deltaUvX / dragStartScaleX, -deltaUvY / dragStartScaleY);
                        uvScaleX = Math.max(0.01f, dragStartScaleX * scaleFactor);
                        uvScaleY = uvScaleX / aspectRatio;
                        uvOffsetX = dragStartOffsetX + dragStartScaleX - uvScaleX;
                        uvOffsetY = dragStartOffsetY;
                        break;

                    case BOTTOM_RIGHT:
                        // 右下角：整体放大缩小
                        scaleFactor = 1.0f + Math.max(deltaUvX / dragStartScaleX, -deltaUvY / dragStartScaleY);
                        uvScaleX = Math.max(0.01f, dragStartScaleX * scaleFactor);
                        uvScaleY = uvScaleX / aspectRatio;
                        uvOffsetX = dragStartOffsetX;
                        uvOffsetY = dragStartOffsetY;
                        break;
                }

                // 确保不超出边界（SCALE模式下需要保持宽高比）
                // 先检查哪个维度会先超出边界
                float maxAllowedScaleX = 1.0f - uvOffsetX;
                float maxAllowedScaleY = 1.0f - uvOffsetY;

                // 如果 uvScaleX 超出，按 X 限制并重新计算 Y
                if (uvScaleX > maxAllowedScaleX) {
                    uvScaleX = maxAllowedScaleX;
                    uvScaleY = uvScaleX / aspectRatio;
                }

                // 如果 uvScaleY 超出，按 Y 限制并重新计算 X
                if (uvScaleY > maxAllowedScaleY) {
                    uvScaleY = maxAllowedScaleY;
                    uvScaleX = uvScaleY * aspectRatio;
                }

                // 最后确保最小值和偏移范围
                uvScaleX = Math.max(0.01f, uvScaleX);
                uvScaleY = Math.max(0.01f, uvScaleY);
                uvOffsetX = Math.max(0.0f, Math.min(1.0f - uvScaleX, uvOffsetX));
                uvOffsetY = Math.max(0.0f, Math.min(1.0f - uvScaleY, uvOffsetY));

            } else {
                // ===== RESIZE 模式：自由调整大小 =====
                switch (currentHandle) {
                    case TOP_LEFT:
                        uvOffsetX = Math.max(0.0f, Math.min(dragStartOffsetX + dragStartScaleX - 0.01f, dragStartOffsetX + deltaUvX));
                        uvOffsetY = dragStartOffsetY;
                        uvScaleX = dragStartScaleX + (dragStartOffsetX - uvOffsetX);
                        uvScaleY = Math.max(0.01f, Math.min(1.0f - uvOffsetY, dragStartScaleY + deltaUvY));
                        break;

                    case TOP_RIGHT:
                        uvOffsetX = dragStartOffsetX;
                        uvOffsetY = dragStartOffsetY;
                        uvScaleX = Math.max(0.01f, Math.min(1.0f - uvOffsetX, dragStartScaleX + deltaUvX));
                        uvScaleY = Math.max(0.01f, Math.min(1.0f - uvOffsetY, dragStartScaleY + deltaUvY));
                        break;

                    case BOTTOM_LEFT:
                        uvOffsetX = Math.max(0.0f, Math.min(dragStartOffsetX + dragStartScaleX - 0.01f, dragStartOffsetX + deltaUvX));
                        uvOffsetY = Math.max(0.0f, Math.min(dragStartOffsetY + dragStartScaleY - 0.01f, dragStartOffsetY + deltaUvY));
                        uvScaleX = dragStartScaleX + (dragStartOffsetX - uvOffsetX);
                        uvScaleY = dragStartScaleY + (dragStartOffsetY - uvOffsetY);
                        break;

                    case BOTTOM_RIGHT:
                        uvOffsetX = dragStartOffsetX;
                        uvOffsetY = Math.max(0.0f, Math.min(dragStartOffsetY + dragStartScaleY - 0.01f, dragStartOffsetY + deltaUvY));
                        uvScaleX = Math.max(0.01f, Math.min(1.0f - uvOffsetX, dragStartScaleX + deltaUvX));
                        uvScaleY = dragStartScaleY + (dragStartOffsetY - uvOffsetY);
                        break;
                }

                // 确保UV在有效范围内
                uvScaleX = Math.max(0.01f, Math.min(1.0f - uvOffsetX, uvScaleX));
                uvScaleY = Math.max(0.01f, Math.min(1.0f - uvOffsetY, uvScaleY));
            }

            updateSelectionBox();
            return true;

        } else if (isDragging) {
            // 拖动模式
            int deltaX = mouseX - dragStartX;
            int deltaY = mouseY - dragStartY;

            // **修复：需要考虑缩放级别**
            float effectiveDisplayWidth = displayWidth * textureZoom;
            float effectiveDisplayHeight = displayHeight * textureZoom;

            float deltaUvX = deltaX / effectiveDisplayWidth;
            float deltaUvY = deltaY / effectiveDisplayHeight;

            // 支持超出1.0的UV范围
            uvOffsetX = Math.max(0.0f, Math.min(maxUvRange - uvScaleX, dragStartOffsetX + deltaUvX));
            uvOffsetY = Math.max(0.0f, Math.min(maxUvRange - uvScaleY, dragStartOffsetY + deltaUvY));

            updateSelectionBox();
            return true;
        }

        return false;
    }

    public boolean handleMouseRelease(int mouseX, int mouseY) {
        // 处理右键平移释放
        if (isPanning) {
            isPanning = false;
            return true;
        }

        // 处理网格大小滑条释放
        if (gridSizeSlider != null && gridSizeSlider.isDragging()) {
            gridSizeSlider.handleMouseRelease();
            return true;
        }

        // 处理滚动条释放
        if (isScrolling) {
            isScrolling = false;
            if (horizontalScrollbar != null) {
                horizontalScrollbar.handleMouseRelease();
            }
            if (verticalScrollbar != null) {
                verticalScrollbar.handleMouseRelease();
            }
            return true;
        }

        if (isDragging || isResizing) {
            isDragging = false;
            isResizing = false;
            currentHandle = ResizeHandle.NONE;

            // 应用网格吸附
            applyGridSnap();

            // 通知监听器
            if (changeListener != null) {
                changeListener.onUVChanged(uvOffsetX, uvOffsetY, uvScaleX, uvScaleY);
            }

            return true;
        }
        return false;
    }

    public void show() {
        rootNode.setCullHint(Spatial.CullHint.Never);
        // 重置缩放和平移状态
        textureZoom = 1.0f;
        viewportOffsetX = 0.0f;
        viewportOffsetY = 0.0f;
        isPanning = false;
        updateTextureViewport();
        updateSelectionBox();
    }

    public void hide() {
        rootNode.setCullHint(Spatial.CullHint.Always);
        // 重置缩放和平移状态
        textureZoom = 1.0f;
        viewportOffsetX = 0.0f;
        viewportOffsetY = 0.0f;
        isPanning = false;
        isDragging = false;
        isResizing = false;
        currentHandle = ResizeHandle.NONE;
        updateTextureViewport();
        updateSelectionBox();
    }

    public boolean isVisible() {
        return rootNode.getCullHint() == Spatial.CullHint.Never;
    }

    /**
     * 处理鼠标滚轮事件（用于缩放）
     * @param mouseX 鼠标X坐标
     * @param mouseY 鼠标Y坐标
     * @param scrollAmount 滚动量（负数=放大，正数=缩小）
     * @return 是否处理了事件
     */
    public boolean handleMouseScroll(int mouseX, int mouseY, float scrollAmount) {
        // 检查是否在预览区域内
        float[] bounds = getDisplayBounds();
        float displayX = bounds[0];
        float displayY = bounds[1];
        float displayWidth = bounds[2];
        float displayHeight = bounds[3];

        if (mouseX >= displayX && mouseX <= displayX + displayWidth &&
            mouseY >= displayY && mouseY <= displayY + displayHeight) {

            // 计算缩放前鼠标在UV空间的位置
            float relativeMouseX = (mouseX - displayX) / displayWidth;
            float relativeMouseY = (mouseY - displayY) / displayHeight;

            float oldZoom = textureZoom;

            // 更新缩放级别
            if (scrollAmount < 0) {
                // 向上滚动 = 放大
                textureZoom += ZOOM_SPEED;
            } else {
                // 向下滚动 = 缩小
                textureZoom -= ZOOM_SPEED;
            }

            // 限制缩放范围
            textureZoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, textureZoom));

            // 调整视口偏移，使缩放以鼠标位置为中心
            if (textureZoom != oldZoom) {
                float zoomRatio = textureZoom / oldZoom;

                // 计算鼠标在当前视口的UV位置
                float mouseUvX = viewportOffsetX + relativeMouseX / oldZoom;
                float mouseUvY = viewportOffsetY + relativeMouseY / oldZoom;

                // 调整视口偏移，使鼠标位置保持不变
                viewportOffsetX = mouseUvX - relativeMouseX / textureZoom;
                viewportOffsetY = mouseUvY - relativeMouseY / textureZoom;

                // 限制平移范围
                viewportOffsetX = Math.max(-maxUvRange, Math.min(maxUvRange, viewportOffsetX));
                viewportOffsetY = Math.max(-maxUvRange, Math.min(maxUvRange, viewportOffsetY));

                updateTextureViewport();
                updateSelectionBox();
                updateGridLines();
            }

            return true;
        }

        return false;
    }

    // ========== Getters and Setters ==========

    public Node getRootNode() {
        return rootNode;
    }

    public void setChangeListener(TexturePreviewPanel.UVChangeListener listener) {
        this.changeListener = listener;
    }

    public float getUvOffsetX() {
        return uvOffsetX;
    }

    public float getUvOffsetY() {
        return uvOffsetY;
    }

    public float getUvScaleX() {
        return uvScaleX;
    }

    public float getUvScaleY() {
        return uvScaleY;
    }

    /**
     * 设置UV显示范围（用于支持大纹理图集）
     * @param maxRange 最大UV范围（默认2.0，即可显示2倍大小的纹理）
     */
    public void setMaxUvRange(float maxRange) {
        this.maxUvRange = Math.max(1.0f, maxRange);

        // 更新滚动条范围
        if (horizontalScrollbar != null) {
            horizontalScrollbar.setRange(0.0f, maxUvRange - 1.0f);
        }
        if (verticalScrollbar != null) {
            verticalScrollbar.setRange(0.0f, maxUvRange - 1.0f);
        }
    }

    /**
     * 数值输入回调接口
     */
    private interface NumericInputCallback {
        void onValueEntered(float value);
    }

    /**
     * 打开数值输入对话框
     * @param title 对话框标题
     * @param currentValue 当前值
     * @param minValue 最小值
     * @param maxValue 最大值
     * @param callback 回调接口
     */
    private void openNumericInputDialog(String title, float currentValue, float minValue, float maxValue, NumericInputCallback callback) {
        app.enqueue(() -> {
            try {
                String input = javax.swing.JOptionPane.showInputDialog(
                    null,
                    String.format("Enter value (%.2f - %.2f):", minValue, maxValue),
                    title,
                    javax.swing.JOptionPane.PLAIN_MESSAGE
                );

                if (input != null && !input.trim().isEmpty()) {
                    try {
                        float value = Float.parseFloat(input.trim());
                        // 限制在有效范围内
                        value = Math.max(minValue, Math.min(maxValue, value));
                        final float finalValue = value;

                        // 在主线程中执行回调
                        app.enqueue(() -> {
                            callback.onValueEntered(finalValue);
                            return null;
                        });
                    } catch (NumberFormatException e) {
                        javax.swing.JOptionPane.showMessageDialog(
                            null,
                            "Invalid number format. Please enter a valid decimal number.",
                            "Error",
                            javax.swing.JOptionPane.ERROR_MESSAGE
                        );
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        });
    }

    // ========== 网格功能 ==========

    /**
     * 更新网格线
     */
    private void updateGridLines() {
        // 清除旧的网格线
        gridLinesNode.detachAllChildren();

        // 如果没有当前部件，或者网格未启用，则不显示网格
        if (currentPart == null || !currentPart.isGridEnabled()) {
            return;
        }

        // 获取预览区域尺寸
        float[] bounds = getDisplayBounds();
        float displayX = bounds[0];
        float displayY = bounds[1];
        float displayWidth = bounds[2];
        float displayHeight = bounds[3];

        Material gridMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        gridMat.setColor("Color", new ColorRGBA(0.5f, 0.5f, 0.5f, 0.5f));
        gridMat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);

        int lineThickness = 1;

        // 从当前部件读取网格配置
        float partGridSize = currentPart.getGridSize();
        boolean partGridHorizontal = currentPart.isGridHorizontal();
        boolean partGridVertical = currentPart.isGridVertical();

        // 计算UV空间的网格间距（基于像素gridSize转换为UV空间）
        // 关键：网格应该是正方形的，所以需要使用较小的显示尺寸作为参考
        // 这样可以确保网格在UV空间中是正方形，而不会被拉伸
        float minDisplaySize = Math.min(displayWidth, displayHeight);

        // UV空间中每个像素对应的UV大小（使用最小尺寸保持正方形）
        // 在UV空间中，1.0f / textureZoom 的范围会被映射到 minDisplaySize 像素
        float uvPerPixel = (1.0f / textureZoom) / minDisplaySize;

        // 网格间距在UV空间中（保持正方形网格）
        float uvGridSpacing = partGridSize * uvPerPixel;

        // 计算当前视口在UV空间的范围
        float uvVisibleWidth = 1.0f / textureZoom;
        float uvVisibleHeight = 1.0f / textureZoom;

        float uvStartX = viewportOffsetX;
        float uvEndX = viewportOffsetX + uvVisibleWidth;
        float uvStartY = viewportOffsetY;
        float uvEndY = viewportOffsetY + uvVisibleHeight;

        // 绘制纵向网格线（固定在UV空间的绝对位置，从UV=0开始）
        if (partGridVertical && partGridSize > 0 && uvGridSpacing > 0) {
            // 计算可见范围内的网格线索引（基于UV空间原点）
            int startLineIndex = (int)Math.floor(uvStartX / uvGridSpacing);
            int endLineIndex = (int)Math.ceil(uvEndX / uvGridSpacing);

            for (int i = startLineIndex; i <= endLineIndex; i++) {
                // UV空间中的绝对位置（相对于UV原点0,0）
                float uvX = i * uvGridSpacing;

                // 检查是否在视口范围内
                if (uvX < uvStartX || uvX > uvEndX) continue;

                // 转换UV绝对坐标到屏幕坐标
                // UV坐标相对于UV原点，减去viewportOffset得到相对于当前视口的位置
                float relativeUvX = uvX - viewportOffsetX;
                // 将UV空间坐标转换为屏幕像素坐标
                // 使用 minDisplaySize 保持正方形网格
                float screenX = displayX + relativeUvX * minDisplaySize * textureZoom;

                // 竖线从显示区域顶部延伸到底部
                Geometry line = new Geometry("VGridLine_" + i, new Quad(lineThickness, displayHeight));
                line.setMaterial(gridMat);
                line.setLocalTranslation(screenX, displayY, 0);
                gridLinesNode.attachChild(line);
            }
        }

        // 绘制横向网格线（固定在UV空间的绝对位置，从UV=0开始）
        if (partGridHorizontal && partGridSize > 0 && uvGridSpacing > 0) {
            // 计算可见范围内的网格线索引（基于UV空间原点）
            int startLineIndex = (int)Math.floor(uvStartY / uvGridSpacing);
            int endLineIndex = (int)Math.ceil(uvEndY / uvGridSpacing);

            for (int i = startLineIndex; i <= endLineIndex; i++) {
                // UV空间中的绝对位置（相对于UV原点0,0）
                float uvY = i * uvGridSpacing;

                // 检查是否在视口范围内
                if (uvY < uvStartY || uvY > uvEndY) continue;

                // 转换UV绝对坐标到屏幕坐标
                // UV坐标相对于UV原点，减去viewportOffset得到相对于当前视口的位置
                float relativeUvY = uvY - viewportOffsetY;
                // 使用minDisplaySize保持正方形网格（白纸比喻）
                float screenY = displayY + relativeUvY * minDisplaySize * textureZoom;

                // 横线从显示区域左侧延伸到右侧
                Geometry line = new Geometry("HGridLine_" + i, new Quad(displayWidth, lineThickness));
                line.setMaterial(gridMat);
                line.setLocalTranslation(displayX, screenY, 0);
                gridLinesNode.attachChild(line);
            }
        }
    }

    /**
     * 应用网格吸附
     */
    private void applyGridSnap() {
        // 如果没有当前部件，或者吸附未启用，则不吸附
        if (currentPart == null || !currentPart.isSnapToGrid() || !currentPart.isGridEnabled()) {
            return;
        }

        float partGridSize = currentPart.getGridSize();
        if (partGridSize <= 0) {
            return;
        }

        // 获取预览区域尺寸
        int previewWidth = width - 20;
        int previewHeight = height - 80;

        // 将UV坐标转换为像素坐标
        float pixelX = uvOffsetX * previewWidth;
        float pixelY = uvOffsetY * previewHeight;
        float pixelWidth = uvScaleX * previewWidth;
        float pixelHeight = uvScaleY * previewHeight;

        // 对齐到网格
        float snappedX = Math.round(pixelX / partGridSize) * partGridSize;
        float snappedY = Math.round(pixelY / partGridSize) * partGridSize;
        float snappedWidth = Math.round(pixelWidth / partGridSize) * partGridSize;
        float snappedHeight = Math.round(pixelHeight / partGridSize) * partGridSize;

        // 确保最小尺寸为一个网格
        if (snappedWidth < partGridSize) snappedWidth = partGridSize;
        if (snappedHeight < partGridSize) snappedHeight = partGridSize;

        // 转换回UV坐标
        uvOffsetX = snappedX / previewWidth;
        uvOffsetY = snappedY / previewHeight;
        uvScaleX = snappedWidth / previewWidth;
        uvScaleY = snappedHeight / previewHeight;

        // 确保不超出范围
        uvOffsetX = Math.max(0.0f, Math.min(maxUvRange - uvScaleX, uvOffsetX));
        uvOffsetY = Math.max(0.0f, Math.min(maxUvRange - uvScaleY, uvOffsetY));

        updateSelectionBox();
    }

    // ========== 网格控制接口 ==========

    public void setGridEnabled(boolean enabled) {
        this.gridEnabled = enabled;
        updateGridLines();
    }

    public void setGridHorizontal(boolean enabled) {
        this.gridHorizontal = enabled;
        updateGridLines();
    }

    public void setGridVertical(boolean enabled) {
        this.gridVertical = enabled;
        updateGridLines();
    }

    public void setGridSize(float size) {
        this.gridSize = Math.max(4f, Math.min(128f, size));
        updateGridLines();
    }

    public void setSnapToGrid(boolean enabled) {
        this.snapToGrid = enabled;
    }

    public boolean isGridEnabled() {
        return gridEnabled;
    }

    public boolean isGridHorizontal() {
        return gridHorizontal;
    }

    public boolean isGridVertical() {
        return gridVertical;
    }

    public float getGridSize() {
        return gridSize;
    }

    public boolean isSnapToGrid() {
        return snapToGrid;
    }

    // ========== 镜像功能 ==========

    /**
     * 水平镜像UV坐标
     */
    private void flipHorizontal() {
        if (currentTexture == null) {
            return;
        }

        // 水平镜像：新的offsetX = 1.0 - (offsetX + scaleX)
        // scaleX保持不变
        float newOffsetX = 1.0f - (uvOffsetX + uvScaleX);

        setUV(newOffsetX, uvOffsetY, uvScaleX, uvScaleY);

        // 通知监听器
        if (changeListener != null) {
            changeListener.onUVChanged(uvOffsetX, uvOffsetY, uvScaleX, uvScaleY);
        }
    }

    /**
     * 垂直镜像UV坐标
     */
    private void flipVertical() {
        if (currentTexture == null) {
            return;
        }

        // 垂直镜像：新的offsetY = 1.0 - (offsetY + scaleY)
        // scaleY保持不变
        float newOffsetY = 1.0f - (uvOffsetY + uvScaleY);

        setUV(uvOffsetX, newOffsetY, uvScaleX, uvScaleY);

        // 通知监听器
        if (changeListener != null) {
            changeListener.onUVChanged(uvOffsetX, uvOffsetY, uvScaleX, uvScaleY);
        }
    }
}
