package com.Hecate.puppet.newmode;

import com.jme3.app.SimpleApplication;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Quad;
import com.Hecate.puppet.editor.Slider;
import com.Hecate.puppet.editor.Button;

/**
 * 新模式属性检查器 - 编辑卡片属性
 *
 * 核心功能：
 * 1. Ring Radius (环半径)
 * 2. Card Height (卡片高度)
 * 3. Card Rotation (卡片旋转)
 * 4. UV Offset X/Y (UV偏移)
 * 5. UV Scale X/Y (UV缩放)
 * 6. Load Texture (加载贴图)
 * 7. Mirror Card (镜像卡片)
 */
public class NewModeInspectorPanel {

    private final SimpleApplication app;
    private final BitmapFont font;
    private final Node rootNode;
    private final Node guiNode;  // 直接访问guiNode用于滑条和按钮
    private int x, y;  // 可变，支持拖动
    private final int width, height;

    // 当前编辑的骨骼和卡片
    private NewModeBone currentBone;
    private int currentCardIndex = -1;

    // 渲染器引用
    private NewModePuppetRenderer renderer;

    // 布局参数
    private final int titleBarHeight = 50;
    private final int sliderSpacing = 45;

    // UI组件
    private BitmapText titleText;
    private BitmapText boneNameText;
    private BitmapText cardIndexText;

    // 滑条
    private Slider widthSlider;      // 卡片宽度
    private Slider heightSlider;     // 卡片高度
    private Slider zOffsetSlider;    // Z轴离心值（相对八棱柱中心）
    private Slider positionXSlider;
    private Slider positionYSlider;
    private Slider positionZSlider;
    private Slider rotationXSlider;
    private Slider rotationYSlider;
    private Slider rotationZSlider;

    // 按钮
    private Button loadTextureButton;
    private Button clearTextureButton;

    // 背景
    private Geometry backgroundGeometry;
    private Geometry titleBarGeometry;

    // 拖动相关
    private boolean isDragging = false;
    private int dragStartX = 0;
    private int dragStartY = 0;
    private int panelStartX = 0;
    private int panelStartY = 0;

    // 回调接口
    public interface InspectorCallbacks {
        void onLoadTexture(NewModeBone bone, int cardIndex);
        void onClearTexture(NewModeBone bone, int cardIndex);
    }
    private InspectorCallbacks callbacks;

    public NewModeInspectorPanel(SimpleApplication app, BitmapFont font, int x, int y, int width, int height) {
        this.app = app;
        this.font = font;
        this.guiNode = app.getGuiNode();
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        this.rootNode = new Node("NewModeInspectorPanel");
        initializePanel();
    }

    /**
     * 初始化面板 - 所有子元素使用相对坐标
     */
    private void initializePanel() {
        // 创建背景（相对坐标）
        Quad bgQuad = new Quad(width, height);
        backgroundGeometry = new Geometry("InspectorBackground", bgQuad);
        Material bgMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        bgMat.setColor("Color", new ColorRGBA(0.15f, 0.15f, 0.15f, 0.9f));
        bgMat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        backgroundGeometry.setMaterial(bgMat);
        backgroundGeometry.setLocalTranslation(0, 0, -1);
        rootNode.attachChild(backgroundGeometry);

        // 创建标题栏背景（相对坐标）
        Quad titleBarQuad = new Quad(width, titleBarHeight);
        titleBarGeometry = new Geometry("InspectorTitleBar", titleBarQuad);
        Material titleBarMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        titleBarMat.setColor("Color", new ColorRGBA(0.08f, 0.08f, 0.1f, 0.95f));
        titleBarMat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        titleBarGeometry.setMaterial(titleBarMat);
        titleBarGeometry.setLocalTranslation(0, height - titleBarHeight, -0.5f);
        rootNode.attachChild(titleBarGeometry);

        float currentY = height - 20;

        // 标题（相对坐标）
        titleText = new BitmapText(font);
        titleText.setText("=== Inspector ===");
        titleText.setSize(font.getCharSet().getRenderedSize() * 2.0f);
        titleText.setColor(ColorRGBA.Yellow);
        titleText.setLocalTranslation(10, currentY, 1);
        rootNode.attachChild(titleText);
        currentY -= 35;

        // 骨骼名称（相对坐标）
        boneNameText = new BitmapText(font);
        boneNameText.setText("No bone selected");
        boneNameText.setSize(font.getCharSet().getRenderedSize() * 1.5f);
        boneNameText.setColor(ColorRGBA.Cyan);
        boneNameText.setLocalTranslation(10, currentY, 1);
        rootNode.attachChild(boneNameText);
        currentY -= 25;

        // 卡片索引（相对坐标）
        cardIndexText = new BitmapText(font);
        cardIndexText.setText("Card: -");
        cardIndexText.setSize(font.getCharSet().getRenderedSize() * 1.2f);
        cardIndexText.setColor(ColorRGBA.Gray);
        cardIndexText.setLocalTranslation(10, currentY, 1);
        rootNode.attachChild(cardIndexText);
        currentY -= 40;

        createSliders(10, currentY);
        createButtons(10, currentY - 400);

        // 设置 rootNode 的屏幕绝对位置（唯一的坐标转换点）
        updateRootNodePosition();
    }

    /**
     * 创建滑条
     */
    private void createSliders(float startX, float startY) {
        float currentY = startY;

        // 计算绝对坐标（rootNode的位置 + 相对位置）
        int screenHeight = app.getCamera().getHeight();
        float panelBottomGuiY = screenHeight - y - height;
        int absoluteX = (int)(x + startX);

        // Width 滑条 - 绑定到选中卡片
        int sliderAbsoluteY = (int)(panelBottomGuiY + currentY - 30);
        widthSlider = new Slider(app, font, "Width", 0.1f, 5.0f, 1.0f, absoluteX, sliderAbsoluteY);
        widthSlider.setChangeListener(value -> {
            if (currentBone != null && currentCardIndex >= 0) {
                NewModeBone.CardData card = currentBone.getCard(currentCardIndex);
                if (card != null) {
                    card.width = value;
                    if (renderer != null) {
                        renderer.refreshCardGeometry(currentBone, currentCardIndex);
                    }
                }
            }
        });
        guiNode.attachChild(widthSlider.getRootNode());
        currentY -= sliderSpacing;

        // Height 滑条 - 绑定到选中卡片
        sliderAbsoluteY = (int)(panelBottomGuiY + currentY - 30);
        heightSlider = new Slider(app, font, "Height", 0.1f, 10.0f, 2.0f, absoluteX, sliderAbsoluteY);
        heightSlider.setChangeListener(value -> {
            if (currentBone != null && currentCardIndex >= 0) {
                NewModeBone.CardData card = currentBone.getCard(currentCardIndex);
                if (card != null) {
                    card.height = value;
                    if (renderer != null) {
                        renderer.refreshCardGeometry(currentBone, currentCardIndex);
                    }
                }
            }
        });
        guiNode.attachChild(heightSlider.getRootNode());
        currentY -= sliderSpacing;

        // Z Offset 滑条 - 绑定到选中卡片（相对八棱柱中心的距离）
        sliderAbsoluteY = (int)(panelBottomGuiY + currentY - 30);
        zOffsetSlider = new Slider(app, font, "Z Offset", -5.0f, 5.0f, 0f, absoluteX, sliderAbsoluteY);
        zOffsetSlider.setChangeListener(value -> {
            if (currentBone != null && currentCardIndex >= 0) {
                NewModeBone.CardData card = currentBone.getCard(currentCardIndex);
                if (card != null) {
                    card.zOffset = value;
                    if (renderer != null) {
                        renderer.refreshCardGeometry(currentBone, currentCardIndex);
                    }
                }
            }
        });
        guiNode.attachChild(zOffsetSlider.getRootNode());
        currentY -= sliderSpacing;

        // Position X 滑条 - 绑定到选中卡片
        sliderAbsoluteY = (int)(panelBottomGuiY + currentY - 30);
        positionXSlider = new Slider(app, font, "Pos X", -5f, 5f, 0f, absoluteX, sliderAbsoluteY);
        positionXSlider.setChangeListener(value -> {
            if (currentBone != null && currentCardIndex >= 0) {
                NewModeBone.CardData card = currentBone.getCard(currentCardIndex);
                if (card != null) {
                    card.localPosition.x = value;
                    if (renderer != null) {
                        renderer.refreshCardGeometry(currentBone, currentCardIndex);
                    }
                }
            }
        });
        guiNode.attachChild(positionXSlider.getRootNode());
        currentY -= sliderSpacing;

        // Position Y 滑条 - 绑定到选中卡片
        sliderAbsoluteY = (int)(panelBottomGuiY + currentY - 30);
        positionYSlider = new Slider(app, font, "Pos Y", -5f, 5f, 0f, absoluteX, sliderAbsoluteY);
        positionYSlider.setChangeListener(value -> {
            if (currentBone != null && currentCardIndex >= 0) {
                NewModeBone.CardData card = currentBone.getCard(currentCardIndex);
                if (card != null) {
                    card.localPosition.y = value;
                    if (renderer != null) {
                        renderer.refreshCardGeometry(currentBone, currentCardIndex);
                    }
                }
            }
        });
        guiNode.attachChild(positionYSlider.getRootNode());
        currentY -= sliderSpacing;

        // Position Z 滑条 - 绑定到选中卡片
        sliderAbsoluteY = (int)(panelBottomGuiY + currentY - 30);
        positionZSlider = new Slider(app, font, "Pos Z", -5f, 5f, 0f, absoluteX, sliderAbsoluteY);
        positionZSlider.setChangeListener(value -> {
            if (currentBone != null && currentCardIndex >= 0) {
                NewModeBone.CardData card = currentBone.getCard(currentCardIndex);
                if (card != null) {
                    card.localPosition.z = value;
                    if (renderer != null) {
                        renderer.refreshCardGeometry(currentBone, currentCardIndex);
                    }
                }
            }
        });
        guiNode.attachChild(positionZSlider.getRootNode());
        currentY -= sliderSpacing;

        // Rotation X 滑条 - 绑定到选中卡片
        sliderAbsoluteY = (int)(panelBottomGuiY + currentY - 30);
        rotationXSlider = new Slider(app, font, "Rot X", -180f, 180f, 0f, absoluteX, sliderAbsoluteY);
        rotationXSlider.setChangeListener(value -> {
            if (currentBone != null && currentCardIndex >= 0) {
                NewModeBone.CardData card = currentBone.getCard(currentCardIndex);
                if (card != null) {
                    card.rotationX = value;
                    if (renderer != null) {
                        renderer.refreshCardGeometry(currentBone, currentCardIndex);
                    }
                }
            }
        });
        guiNode.attachChild(rotationXSlider.getRootNode());
        currentY -= sliderSpacing;

        // Rotation Y 滑条 - 绑定到选中卡片
        sliderAbsoluteY = (int)(panelBottomGuiY + currentY - 30);
        rotationYSlider = new Slider(app, font, "Rot Y", -180f, 180f, 0f, absoluteX, sliderAbsoluteY);
        rotationYSlider.setChangeListener(value -> {
            if (currentBone != null && currentCardIndex >= 0) {
                NewModeBone.CardData card = currentBone.getCard(currentCardIndex);
                if (card != null) {
                    card.rotationY = value;
                    if (renderer != null) {
                        renderer.refreshCardGeometry(currentBone, currentCardIndex);
                    }
                }
            }
        });
        guiNode.attachChild(rotationYSlider.getRootNode());
        currentY -= sliderSpacing;

        // Rotation Z 滑条 - 绑定到选中卡片
        sliderAbsoluteY = (int)(panelBottomGuiY + currentY - 30);
        rotationZSlider = new Slider(app, font, "Rot Z", -180f, 180f, 0f, absoluteX, sliderAbsoluteY);
        rotationZSlider.setChangeListener(value -> {
            if (currentBone != null && currentCardIndex >= 0) {
                NewModeBone.CardData card = currentBone.getCard(currentCardIndex);
                if (card != null) {
                    card.rotationZ = value;
                    if (renderer != null) {
                        renderer.refreshCardGeometry(currentBone, currentCardIndex);
                    }
                }
            }
        });
        guiNode.attachChild(rotationZSlider.getRootNode());
    }

    /**
     * 创建按钮
     */
    private void createButtons(float startX, float startY) {
        float currentY = startY;
        int buttonHeight = 30;
        int buttonSpacing = 8;

        // 计算绝对坐标
        int screenHeight = app.getCamera().getHeight();
        float panelBottomGuiY = screenHeight - y - height;
        int absoluteX = (int)(x + startX);

        // Load Texture 按钮
        int buttonAbsoluteY = (int)(panelBottomGuiY + currentY - buttonHeight);
        loadTextureButton = new Button(app, font, "Load Texture", absoluteX, buttonAbsoluteY, width - 20, buttonHeight);
        loadTextureButton.setClickListener(() -> {
            if (callbacks != null && currentBone != null && currentCardIndex >= 0) {
                callbacks.onLoadTexture(currentBone, currentCardIndex);
                // 加载完贴图后刷新渲染
                if (renderer != null) {
                    renderer.refreshCardTexture(currentBone, currentCardIndex);
                }
            }
        });
        guiNode.attachChild(loadTextureButton.getRootNode());
        currentY -= (buttonHeight + buttonSpacing);

        // Clear Texture 按钮
        buttonAbsoluteY = (int)(panelBottomGuiY + currentY - buttonHeight);
        clearTextureButton = new Button(app, font, "Clear Texture", absoluteX, buttonAbsoluteY, width - 20, buttonHeight);
        clearTextureButton.setClickListener(() -> {
            if (callbacks != null && currentBone != null && currentCardIndex >= 0) {
                callbacks.onClearTexture(currentBone, currentCardIndex);
                // 清除贴图后刷新渲染
                if (renderer != null) {
                    renderer.refreshCardTexture(currentBone, currentCardIndex);
                }
            }
        });
        guiNode.attachChild(clearTextureButton.getRootNode());
    }

    /**
     * 设置当前骨骼和卡片
     */
    public void setBone(NewModeBone bone, int cardIndex) {
        this.currentBone = bone;
        this.currentCardIndex = cardIndex;
        updateDisplay();
    }

    /**
     * 更新显示
     */
    private void updateDisplay() {
        if (currentBone == null || currentCardIndex < 0) {
            boneNameText.setText("No bone selected");
            cardIndexText.setText("Card: -");
            return;
        }

        boneNameText.setText(currentBone.getName());
        cardIndexText.setText("Card: " + currentCardIndex);

        // 更新卡片级别滑条（所有参数都是每张卡片独立的）
        NewModeBone.CardData card = currentBone.getCard(currentCardIndex);
        if (card != null) {
            widthSlider.setValue(card.width);
            heightSlider.setValue(card.height);
            zOffsetSlider.setValue(card.zOffset);

            positionXSlider.setValue(card.localPosition.x);
            positionYSlider.setValue(card.localPosition.y);
            positionZSlider.setValue(card.localPosition.z);

            rotationXSlider.setValue(card.rotationX);
            rotationYSlider.setValue(card.rotationY);
            rotationZSlider.setValue(card.rotationZ);
        }
    }

    /**
     * 处理鼠标点击
     */
    public boolean handleMouseClick(int mouseX, int mouseY) {
        // 检查按钮
        if (loadTextureButton.handleMouseClick(mouseX, mouseY)) return true;
        if (clearTextureButton.handleMouseClick(mouseX, mouseY)) return true;

        // 检查滑条
        if (widthSlider.handleMouseClick(mouseX, mouseY)) return true;
        if (heightSlider.handleMouseClick(mouseX, mouseY)) return true;
        if (zOffsetSlider.handleMouseClick(mouseX, mouseY)) return true;
        if (positionXSlider.handleMouseClick(mouseX, mouseY)) return true;
        if (positionYSlider.handleMouseClick(mouseX, mouseY)) return true;
        if (positionZSlider.handleMouseClick(mouseX, mouseY)) return true;
        if (rotationXSlider.handleMouseClick(mouseX, mouseY)) return true;
        if (rotationYSlider.handleMouseClick(mouseX, mouseY)) return true;
        if (rotationZSlider.handleMouseClick(mouseX, mouseY)) return true;

        return false;
    }

    /**
     * 处理鼠标拖动 - 只更新 x, y 并移动 rootNode
     * @return 如果正在处理拖动（面板或滑条）返回true，否则返回false
     */
    public boolean handleMouseDrag(int mouseX, int mouseY) {
        // 优先处理面板拖动
        if (isDragging) {
            int deltaX = mouseX - dragStartX;
            int deltaY = mouseY - dragStartY;

            x = panelStartX + deltaX;
            y = panelStartY - deltaY;

            // 增大拖动范围15%
            int screenWidth = app.getCamera().getWidth();
            int screenHeight = app.getCamera().getHeight();
            int extraRange = (int)(Math.max(width, height) * 0.15);

            // 限制在屏幕范围内（允许15%超出）
            x = Math.max(-extraRange, Math.min(x, screenWidth - width + extraRange));
            y = Math.max(-extraRange, Math.min(y, screenHeight - height + extraRange));

            updateRootNodePosition();
            updateSlidersAndButtonsPosition();  // 更新滑条和按钮位置
            return true;
        }

        // 处理滑条拖动 - 检查是否任何滑条正在被拖动
        boolean anySliderDragging = false;
        anySliderDragging |= widthSlider.handleMouseDrag(mouseX, mouseY);
        anySliderDragging |= heightSlider.handleMouseDrag(mouseX, mouseY);
        anySliderDragging |= zOffsetSlider.handleMouseDrag(mouseX, mouseY);
        anySliderDragging |= positionXSlider.handleMouseDrag(mouseX, mouseY);
        anySliderDragging |= positionYSlider.handleMouseDrag(mouseX, mouseY);
        anySliderDragging |= positionZSlider.handleMouseDrag(mouseX, mouseY);
        anySliderDragging |= rotationXSlider.handleMouseDrag(mouseX, mouseY);
        anySliderDragging |= rotationYSlider.handleMouseDrag(mouseX, mouseY);
        anySliderDragging |= rotationZSlider.handleMouseDrag(mouseX, mouseY);

        return anySliderDragging;
    }

    /**
     * 处理鼠标释放（滑条释放 + 面板拖动结束）
     */
    public void handleMouseRelease() {
        // 结束面板拖动
        if (isDragging) {
            isDragging = false;
        }

        // 处理滑条释放
        widthSlider.handleMouseRelease();
        heightSlider.handleMouseRelease();
        zOffsetSlider.handleMouseRelease();
        positionXSlider.handleMouseRelease();
        positionYSlider.handleMouseRelease();
        positionZSlider.handleMouseRelease();
        rotationXSlider.handleMouseRelease();
        rotationYSlider.handleMouseRelease();
        rotationZSlider.handleMouseRelease();
    }

    /**
     * 检查是否点击在标题栏（用于拖动）
     */
    public boolean handleTitleBarClick(int mouseX, int mouseY) {
        int screenHeight = app.getCamera().getHeight();

        // 检查X范围
        if (mouseX < x || mouseX > x + width) {
            return false;
        }

        // 计算标题栏在GUI坐标系中的Y范围
        int titleBarTopGuiY = screenHeight - y;
        int titleBarBottomGuiY = screenHeight - y - titleBarHeight;

        // 检查Y范围（标题栏区域）
        if (mouseY >= titleBarBottomGuiY && mouseY <= titleBarTopGuiY) {
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
     * 更新 rootNode 位置 - 唯一的坐标转换点
     */
    private void updateRootNodePosition() {
        int screenHeight = app.getCamera().getHeight();
        float panelBottomGuiY = screenHeight - y - height;
        rootNode.setLocalTranslation(x, panelBottomGuiY, 0);
    }

    /**
     * 更新滑条和按钮位置（当面板拖动时调用）
     */
    private void updateSlidersAndButtonsPosition() {
        int screenHeight = app.getCamera().getHeight();
        float panelBottomGuiY = screenHeight - y - height;
        int absoluteX = x + 10;

        float currentY = height - titleBarHeight - 10 - 35 - 25 - 40;  // 从标题后开始

        // 更新所有滑条位置
        if (widthSlider != null) {
            int sliderY = (int)(panelBottomGuiY + currentY - 30);
            widthSlider.setPosition(absoluteX, sliderY);
            currentY -= sliderSpacing;
        }

        if (heightSlider != null) {
            int sliderY = (int)(panelBottomGuiY + currentY - 30);
            heightSlider.setPosition(absoluteX, sliderY);
            currentY -= sliderSpacing;
        }

        if (zOffsetSlider != null) {
            int sliderY = (int)(panelBottomGuiY + currentY - 30);
            zOffsetSlider.setPosition(absoluteX, sliderY);
            currentY -= sliderSpacing;
        }

        if (positionXSlider != null) {
            int sliderY = (int)(panelBottomGuiY + currentY - 30);
            positionXSlider.setPosition(absoluteX, sliderY);
            currentY -= sliderSpacing;
        }

        if (positionYSlider != null) {
            int sliderY = (int)(panelBottomGuiY + currentY - 30);
            positionYSlider.setPosition(absoluteX, sliderY);
            currentY -= sliderSpacing;
        }

        if (positionZSlider != null) {
            int sliderY = (int)(panelBottomGuiY + currentY - 30);
            positionZSlider.setPosition(absoluteX, sliderY);
            currentY -= sliderSpacing;
        }

        if (rotationXSlider != null) {
            int sliderY = (int)(panelBottomGuiY + currentY - 30);
            rotationXSlider.setPosition(absoluteX, sliderY);
            currentY -= sliderSpacing;
        }

        if (rotationYSlider != null) {
            int sliderY = (int)(panelBottomGuiY + currentY - 30);
            rotationYSlider.setPosition(absoluteX, sliderY);
            currentY -= sliderSpacing;
        }

        if (rotationZSlider != null) {
            int sliderY = (int)(panelBottomGuiY + currentY - 30);
            rotationZSlider.setPosition(absoluteX, sliderY);
        }

        // 更新按钮位置
        currentY = height - titleBarHeight - 10 - 35 - 25 - 40 - 400;  // 按钮起始位置
        int buttonHeight = 30;
        int buttonSpacing = 8;

        if (loadTextureButton != null) {
            int buttonY = (int)(panelBottomGuiY + currentY - buttonHeight);
            loadTextureButton.setPosition(absoluteX, buttonY);
            currentY -= (buttonHeight + buttonSpacing);
        }

        if (clearTextureButton != null) {
            int buttonY = (int)(panelBottomGuiY + currentY - buttonHeight);
            clearTextureButton.setPosition(absoluteX, buttonY);
        }
    }

    // ========== Getters and Setters ==========

    public Node getRootNode() {
        return rootNode;
    }

    public void setCallbacks(InspectorCallbacks callbacks) {
        this.callbacks = callbacks;
    }

    public void setRenderer(NewModePuppetRenderer renderer) {
        this.renderer = renderer;
    }

    public NewModeBone getCurrentBone() {
        return currentBone;
    }

    public int getCurrentCardIndex() {
        return currentCardIndex;
    }
}
