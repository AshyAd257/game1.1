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
import com.jme3.texture.Texture;

/**
 * 纹理预览和UV区域选择面板
 * 支持鼠标拖拽选择纹理图集中的区域
 */
public class TexturePreviewPanel {

    private final SimpleApplication app;
    private final BitmapFont font;
    private final Node rootNode;

    private int x, y;  // 改为可变，支持拖动
    private final int width, height;

    // 纹理预览
    private Geometry background;
    private Geometry texturePreview;
    private Texture currentTexture;

    // 选择框
    private Node selectionBox;
    private Geometry selectionFill;
    private Geometry topLine, bottomLine, leftLine, rightLine;

    // UV坐标（归一化 0.0-1.0）
    private float uvOffsetX = 0.0f;
    private float uvOffsetY = 0.0f;
    private float uvScaleX = 1.0f;
    private float uvScaleY = 1.0f;

    // 网格系统
    private boolean gridEnabled = false;
    private boolean gridHorizontal = true;
    private boolean gridVertical = true;
    private float gridSize = 32f;  // 网格大小（像素）
    private boolean snapToGrid = false;
    private Node gridLinesNode;

    // 鼠标状态
    private boolean isDragging = false;
    private int dragStartX, dragStartY;
    private float dragStartUvX, dragStartUvY;

    // 面板拖拽状态
    private boolean isPanelDragging = false;
    private int panelDragStartX, panelDragStartY;

    // 双击检测
    private long lastClickTime = 0;
    private static final long DOUBLE_CLICK_INTERVAL = 300; // 毫秒

    // UI文本
    private BitmapText uvInfoText;

    // 镜像按钮
    private Button flipHorizontalButton;
    private Button flipVerticalButton;

    // 回调接口
    private UVChangeListener changeListener;
    private DoubleClickListener doubleClickListener;

    /**
     * UV变化监听器
     */
    public interface UVChangeListener {
        void onUVChanged(float uvOffsetX, float uvOffsetY, float uvScaleX, float uvScaleY);
    }

    /**
     * 双击监听器
     */
    public interface DoubleClickListener {
        void onDoubleClick();
    }

    public TexturePreviewPanel(SimpleApplication app, BitmapFont font, int x, int y, int width, int height) {
        this.app = app;
        this.font = font;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        this.rootNode = new Node("TexturePreviewPanel");
        initialize();
    }

    /**
     * 初始化面板
     */
    private void initialize() {
        // 创建背景
        Quad bgQuad = new Quad(width, height);
        background = new Geometry("PreviewBackground", bgQuad);

        Material bgMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        bgMat.setColor("Color", new ColorRGBA(0.2f, 0.2f, 0.2f, 1.0f));
        background.setMaterial(bgMat);
        background.setLocalTranslation(x, y, 0);
        rootNode.attachChild(background);

        // 创建纹理预览（初始为空）
        Quad texQuad = new Quad(width, height);
        texturePreview = new Geometry("TexturePreview", texQuad);

        Material texMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        texMat.setColor("Color", ColorRGBA.White);
        texMat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        texturePreview.setMaterial(texMat);
        texturePreview.setLocalTranslation(x, y, 0.1f);

        // 设置默认纹理坐标（0,0到1,1覆盖整个quad）
        texQuad.setBuffer(com.jme3.scene.VertexBuffer.Type.TexCoord, 2, new float[]{
            0, 0,  // 左下
            1, 0,  // 右下
            1, 1,  // 右上
            0, 1   // 左上
        });

        rootNode.attachChild(texturePreview);

        // 创建选择框填充（隐藏，不再使用）
        selectionFill = new Geometry("SelectionFill", new Quad(1, 1));
        Material fillMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        fillMat.setColor("Color", new ColorRGBA(0.3f, 0.6f, 1.0f, 0.0f)); // 完全透明
        fillMat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        selectionFill.setMaterial(fillMat);
        selectionFill.setLocalTranslation(x, y, 0.2f);
        // 不添加到场景中，避免干扰视觉
        // rootNode.attachChild(selectionFill);

        // 创建网格线容器
        gridLinesNode = new Node("GridLines");
        rootNode.attachChild(gridLinesNode);
        updateGridLines();

        // 创建选择框边框
        selectionBox = createBoxOutline();
        rootNode.attachChild(selectionBox);

        // 创建UV信息文本
        uvInfoText = new BitmapText(font);
        uvInfoText.setText("UV: 0.00, 0.00 | 1.00 x 1.00");
        uvInfoText.setSize(font.getCharSet().getRenderedSize() * 0.8f);
        uvInfoText.setColor(ColorRGBA.White);
        uvInfoText.setLocalTranslation(x + 5, y - 5, 0.3f);
        rootNode.attachChild(uvInfoText);

        // 创建镜像按钮（放在预览面板内部底部）
        int buttonWidth = (width - 15) / 2;
        int buttonHeight = 25;
        int buttonY = y + 5;  // 放在面板内部底部

        // 水平镜像按钮
        flipHorizontalButton = new Button(app, font, "Flip H", x + 5, buttonY, buttonWidth, buttonHeight);
        flipHorizontalButton.setClickListener(() -> {
            flipHorizontal();
        });
        rootNode.attachChild(flipHorizontalButton.getRootNode());

        // 垂直镜像按钮
        flipVerticalButton = new Button(app, font, "Flip V", x + buttonWidth + 10, buttonY, buttonWidth, buttonHeight);
        flipVerticalButton.setClickListener(() -> {
            flipVertical();
        });
        rootNode.attachChild(flipVerticalButton.getRootNode());

        updateSelectionBox();
    }

    /**
     * 创建选择框边框（4条线）
     */
    private Node createBoxOutline() {
        Node boxNode = new Node("SelectionBox");

        Material lineMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        lineMat.setColor("Color", new ColorRGBA(1.0f, 1.0f, 1.0f, 1.0f)); // 白色边框

        int lineWidth = 2;

        // 顶边
        topLine = new Geometry("TopLine", new Quad(width, lineWidth));
        topLine.setMaterial(lineMat);
        boxNode.attachChild(topLine);

        // 底边
        bottomLine = new Geometry("BottomLine", new Quad(width, lineWidth));
        bottomLine.setMaterial(lineMat);
        boxNode.attachChild(bottomLine);

        // 左边
        leftLine = new Geometry("LeftLine", new Quad(lineWidth, height));
        leftLine.setMaterial(lineMat);
        boxNode.attachChild(leftLine);

        // 右边
        rightLine = new Geometry("RightLine", new Quad(lineWidth, height));
        rightLine.setMaterial(lineMat);
        boxNode.attachChild(rightLine);

        return boxNode;
    }

    /**
     * 设置纹理
     */
    public void setTexture(Texture texture) {
        this.currentTexture = texture;

        if (texture != null) {
            Material mat = texturePreview.getMaterial();
            mat.setTexture("ColorMap", texture);
            // 确保颜色不会影响纹理显示
            mat.setColor("Color", ColorRGBA.White);
            // 确保纹理可见
            texturePreview.setCullHint(com.jme3.scene.Spatial.CullHint.Never);
        } else {
            // 清除纹理
            Material mat = texturePreview.getMaterial();
            mat.clearParam("ColorMap");

        }
    }

    /**
     * 设置UV坐标
     */
    public void setUV(float offsetX, float offsetY, float scaleX, float scaleY) {
        // 允许负数缩放（用于镜像）
        this.uvOffsetX = offsetX;
        this.uvOffsetY = offsetY;
        this.uvScaleX = scaleX;
        this.uvScaleY = scaleY;

        // 对于正常的UV范围，进行限制
        if (scaleX > 0) {
            this.uvOffsetX = Math.max(0.0f, Math.min(1.0f, offsetX));
            this.uvScaleX = Math.max(0.01f, Math.min(1.0f, scaleX));
            // 确保不超出范围
            if (this.uvOffsetX + this.uvScaleX > 1.0f) {
                this.uvOffsetX = 1.0f - this.uvScaleX;
            }
        } else {
            // 负数缩放（镜像）：offsetX是右边界，scaleX是负的宽度
            this.uvOffsetX = Math.max(0.0f, Math.min(1.0f, offsetX));
            this.uvScaleX = Math.max(-1.0f, Math.min(-0.01f, scaleX));
        }

        if (scaleY > 0) {
            this.uvOffsetY = Math.max(0.0f, Math.min(1.0f, offsetY));
            this.uvScaleY = Math.max(0.01f, Math.min(1.0f, scaleY));
            // 确保不超出范围
            if (this.uvOffsetY + this.uvScaleY > 1.0f) {
                this.uvOffsetY = 1.0f - this.uvScaleY;
            }
        } else {
            // 负数缩放（镜像）：offsetY是上边界，scaleY是负的高度
            this.uvOffsetY = Math.max(0.0f, Math.min(1.0f, offsetY));
            this.uvScaleY = Math.max(-1.0f, Math.min(-0.01f, scaleY));
        }

        updateSelectionBox();
    }

    /**
     * 更新网格线
     */
    private void updateGridLines() {
        // 清除旧的网格线
        gridLinesNode.detachAllChildren();

        if (!gridEnabled) {
            return;
        }

        Material gridMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        gridMat.setColor("Color", new ColorRGBA(0.5f, 0.5f, 0.5f, 0.5f)); // 半透明灰色
        gridMat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);

        int lineThickness = 1;
        float zDepth = 0.15f; // 在背景之上，纹理之下

        // 绘制纵向网格线
        if (gridVertical && gridSize > 0) {
            int numLines = (int)(width / gridSize);
            for (int i = 0; i <= numLines; i++) {
                float lineX = x + i * gridSize;
                if (lineX > x + width) break;

                Geometry line = new Geometry("VGridLine_" + i, new Quad(lineThickness, height));
                line.setMaterial(gridMat);
                line.setLocalTranslation(lineX, y, zDepth);
                gridLinesNode.attachChild(line);
            }
        }

        // 绘制横向网格线
        if (gridHorizontal && gridSize > 0) {
            int numLines = (int)(height / gridSize);
            for (int i = 0; i <= numLines; i++) {
                float lineY = y + i * gridSize;
                if (lineY > y + height) break;

                Geometry line = new Geometry("HGridLine_" + i, new Quad(width, lineThickness));
                line.setMaterial(gridMat);
                line.setLocalTranslation(x, lineY, zDepth);
                gridLinesNode.attachChild(line);
            }
        }
    }

    /**
     * 更新选择框显示
     */
    private void updateSelectionBox() {
        // Map UV to screen space using panel width/height
        // 处理负数缩放（镜像）
        float boxX = x + uvOffsetX * width;
        float boxY = y + uvOffsetY * height;
        float boxWidth = uvScaleX * width;
        float boxHeight = uvScaleY * height;

        // 如果是负数缩放，需要调整起点
        float actualX = boxWidth < 0 ? boxX + boxWidth : boxX;
        float actualY = boxHeight < 0 ? boxY + boxHeight : boxY;
        float actualWidth = Math.abs(boxWidth);
        float actualHeight = Math.abs(boxHeight);

        int lineWidth = 2;
        float zDepth = 0.25f;

        // 更新顶边
        topLine.setMesh(new Quad(actualWidth, lineWidth));
        topLine.setLocalTranslation(actualX, actualY + actualHeight - lineWidth, zDepth);

        // 更新底边
        bottomLine.setMesh(new Quad(actualWidth, lineWidth));
        bottomLine.setLocalTranslation(actualX, actualY, zDepth);

        // 更新左边
        leftLine.setMesh(new Quad(lineWidth, actualHeight));
        leftLine.setLocalTranslation(actualX, actualY, zDepth);

        // 更新右边
        rightLine.setMesh(new Quad(lineWidth, actualHeight));
        rightLine.setLocalTranslation(actualX + actualWidth - lineWidth, actualY, zDepth);

        // 更新文本
        String uvText = String.format("UV: %.2f, %.2f | %.2f x %.2f",
            uvOffsetX, uvOffsetY, uvScaleX, uvScaleY);
        uvInfoText.setText(uvText);
    }

    /**
     * 处理鼠标按下（兼容旧版本，默认不按Shift）
     */
    public boolean handleMousePress(int mouseX, int mouseY) {
        return handleMousePress(mouseX, mouseY, false);
    }

    /**
     * 处理鼠标按下
     */
    public boolean handleMousePress(int mouseX, int mouseY, boolean shiftPressed) {
        // 优先检查按钮点击
        if (flipHorizontalButton != null && flipHorizontalButton.handleMouseClick(mouseX, mouseY)) {
            return true;
        }
        if (flipVerticalButton != null && flipVerticalButton.handleMouseClick(mouseX, mouseY)) {
            return true;
        }

        if (isPointInPanel(mouseX, mouseY)) {
            // 检测双击
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastClickTime < DOUBLE_CLICK_INTERVAL) {
                // 双击事件
                if (doubleClickListener != null) {
                    doubleClickListener.onDoubleClick();
                }
                lastClickTime = 0; // 重置双击计时
                return true;
            }
            lastClickTime = currentTime;

            // 如果按住Shift键，则拖拽整个面板
            if (shiftPressed) {
                isPanelDragging = true;
                panelDragStartX = mouseX;
                panelDragStartY = mouseY;
            } else {
                // 否则拖拽UV选择框
                isDragging = true;
                dragStartX = mouseX;
                dragStartY = mouseY;

                // Convert mouse position to UV using panel width/height
                dragStartUvX = (float)(mouseX - x) / width;
                dragStartUvY = (float)(mouseY - y) / height;

                // 开始新的选择
                uvOffsetX = dragStartUvX;
                uvOffsetY = dragStartUvY;
                uvScaleX = 0.01f;
                uvScaleY = 0.01f;
                updateSelectionBox();
            }

            return true;
        }
        return false;
    }

    /**
     * 处理鼠标拖拽
     */
    public boolean handleMouseDrag(int mouseX, int mouseY) {
        if (isPanelDragging) {
            // 拖拽整个面板
            int deltaX = mouseX - panelDragStartX;
            int deltaY = mouseY - panelDragStartY;

            setPosition(x + deltaX, y + deltaY);

            panelDragStartX = mouseX;
            panelDragStartY = mouseY;
            return true;
        } else if (isDragging) {
            // 拖拽UV选择框
            // 将鼠标坐标限制在面板范围内
            int clampedX = Math.max(x, Math.min(x + width, mouseX));
            int clampedY = Math.max(y, Math.min(y + height, mouseY));

            // Convert drag delta per axis to keep UV accurate
            float currentUvX = dragStartUvX + (float)(clampedX - dragStartX) / width;
            float currentUvY = dragStartUvY + (float)(clampedY - dragStartY) / height;

            // 计算选择框的起点和大小
            float minUvX = Math.min(dragStartUvX, currentUvX);
            float minUvY = Math.min(dragStartUvY, currentUvY);
            float maxUvX = Math.max(dragStartUvX, currentUvX);
            float maxUvY = Math.max(dragStartUvY, currentUvY);

            uvOffsetX = minUvX;
            uvOffsetY = minUvY;
            uvScaleX = maxUvX - minUvX;
            uvScaleY = maxUvY - minUvY;

            updateSelectionBox();
            return true;
        }
        return false;
    }

    /**
     * 处理鼠标释放
     */
    public boolean handleMouseRelease(int mouseX, int mouseY) {
        if (isPanelDragging) {
            isPanelDragging = false;
            return true;
        } else if (isDragging) {
            isDragging = false;

            // 应用网格吸附
            if (snapToGrid && gridEnabled && gridSize > 0) {
                applyGridSnap();
            }

            // 通知监听器
            if (changeListener != null) {
                changeListener.onUVChanged(uvOffsetX, uvOffsetY, uvScaleX, uvScaleY);
            }

            return true;
        }
        return false;
    }

    /**
     * 应用网格吸附（将UV坐标对齐到最近的网格）
     */
    private void applyGridSnap() {
        // 将UV坐标转换为像素坐标
        float pixelX = uvOffsetX * width;
        float pixelY = uvOffsetY * height;
        float pixelWidth = uvScaleX * width;
        float pixelHeight = uvScaleY * height;

        // 对齐到网格
        float snappedX = Math.round(pixelX / gridSize) * gridSize;
        float snappedY = Math.round(pixelY / gridSize) * gridSize;
        float snappedWidth = Math.round(pixelWidth / gridSize) * gridSize;
        float snappedHeight = Math.round(pixelHeight / gridSize) * gridSize;

        // 确保最小尺寸为一个网格
        if (snappedWidth < gridSize) snappedWidth = gridSize;
        if (snappedHeight < gridSize) snappedHeight = gridSize;

        // 转换回UV坐标
        uvOffsetX = snappedX / width;
        uvOffsetY = snappedY / height;
        uvScaleX = snappedWidth / width;
        uvScaleY = snappedHeight / height;

        // 确保不超出范围
        uvOffsetX = Math.max(0.0f, Math.min(1.0f - uvScaleX, uvOffsetX));
        uvOffsetY = Math.max(0.0f, Math.min(1.0f - uvScaleY, uvOffsetY));

        updateSelectionBox();
    }

    /**
     * 检查点是否在面板内
     */
    private boolean isPointInPanel(int mouseX, int mouseY) {
        return mouseX >= x && mouseX <= x + width &&
               mouseY >= y && mouseY <= y + height;
    }

    /**
     * 更新（用于动画等）
     */
    public void update(float tpf) {
        // 更新镜像按钮
        if (flipHorizontalButton != null) {
            flipHorizontalButton.update(tpf);
        }
        if (flipVerticalButton != null) {
            flipVerticalButton.update(tpf);
        }
    }

    // ========== Getters and Setters ==========

    public Node getRootNode() {
        return rootNode;
    }

    public void setChangeListener(UVChangeListener listener) {
        this.changeListener = listener;
    }

    public void setDoubleClickListener(DoubleClickListener listener) {
        this.doubleClickListener = listener;
    }

    public Texture getCurrentTexture() {
        return currentTexture;
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
     * 设置面板位置（用于拖动父面板时）
     */
    public void setPosition(int newX, int newY) {
        this.x = newX;
        this.y = newY;

        // 更新背景位置
        if (background != null) {
            background.setLocalTranslation(x, y, 0);
        }

        // 更新纹理预览位置
        if (texturePreview != null) {
            texturePreview.setLocalTranslation(x, y, 0.1f);
        }

        // 更新选择框填充位置
        if (selectionFill != null) {
            selectionFill.setLocalTranslation(x, y, 0.2f);
        }

        // 更新UV信息文本位置
        if (uvInfoText != null) {
            uvInfoText.setLocalTranslation(x + 5, y - 5, 0.3f);
        }

        // 更新镜像按钮位置
        int buttonWidth = (width - 15) / 2;
        int buttonHeight = 25;
        int buttonY = y + 5;
        if (flipHorizontalButton != null) {
            flipHorizontalButton.setPosition(x + 5, buttonY);
        }
        if (flipVerticalButton != null) {
            flipVerticalButton.setPosition(x + buttonWidth + 10, buttonY);
        }

        // 更新选择框位置和网格线
        updateSelectionBox();
        updateGridLines();
    }

    // ========== 网格控制接口 ==========

    /**
     * 设置网格开关
     */
    public void setGridEnabled(boolean enabled) {
        this.gridEnabled = enabled;
        updateGridLines();
    }

    /**
     * 设置横向网格
     */
    public void setGridHorizontal(boolean enabled) {
        this.gridHorizontal = enabled;
        updateGridLines();
    }

    /**
     * 设置纵向网格
     */
    public void setGridVertical(boolean enabled) {
        this.gridVertical = enabled;
        updateGridLines();
    }

    /**
     * 设置网格大小
     */
    public void setGridSize(float size) {
        this.gridSize = Math.max(4f, Math.min(128f, size));
        updateGridLines();
    }

    /**
     * 设置吸附开关
     */
    public void setSnapToGrid(boolean enabled) {
        this.snapToGrid = enabled;
    }

    /**
     * 获取网格状态
     */
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

        // 水平镜像：反转scaleX的符号，并调整offsetX
        // 如果scaleX > 0，镜像后：offsetX = offsetX + scaleX, scaleX = -scaleX
        // 如果scaleX < 0，镜像后：offsetX = offsetX + scaleX, scaleX = -scaleX
        float newOffsetX = uvOffsetX + uvScaleX;
        float newScaleX = -uvScaleX;

        setUV(newOffsetX, uvOffsetY, newScaleX, uvScaleY);

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

        // 垂直镜像：反转scaleY的符号，并调整offsetY
        // 如果scaleY > 0，镜像后：offsetY = offsetY + scaleY, scaleY = -scaleY
        // 如果scaleY < 0，镜像后：offsetY = offsetY + scaleY, scaleY = -scaleY
        float newOffsetY = uvOffsetY + uvScaleY;
        float newScaleY = -uvScaleY;

        setUV(uvOffsetX, newOffsetY, uvScaleX, newScaleY);

        // 通知监听器
        if (changeListener != null) {
            changeListener.onUVChanged(uvOffsetX, uvOffsetY, uvScaleX, uvScaleY);
        }
    }
}
