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
 * 可拖拽、可调整大小的面板基类
 * 提供标题栏拖拽和边缘调整大小功能
 */
public class DraggablePanel {

    protected final SimpleApplication app;
    protected final Node rootNode;
    protected final Node contentNode;  // 子类的内容放在这里

    // 位置和大小
    protected int x, y, width, height;
    protected int minWidth = 150;
    protected int minHeight = 100;

    // 标题栏
    protected static final int TITLE_BAR_HEIGHT = 25;
    protected static final int RESIZE_BORDER = 6;  // 调整大小的边框宽度

    protected String title;
    protected Geometry backgroundGeom;
    protected Geometry titleBarGeom;
    protected BitmapText titleText;
    protected BitmapFont font;

    // 拖拽状态
    protected boolean isDragging = false;
    protected boolean isResizing = false;
    protected int dragOffsetX, dragOffsetY;
    protected ResizeDirection resizeDirection = ResizeDirection.NONE;

    // 颜色配置
    protected ColorRGBA backgroundColor = new ColorRGBA(0.15f, 0.15f, 0.18f, 0.95f);
    protected ColorRGBA titleBarColor = new ColorRGBA(0.25f, 0.25f, 0.3f, 1f);
    protected ColorRGBA titleBarHoverColor = new ColorRGBA(0.3f, 0.3f, 0.38f, 1f);
    protected ColorRGBA borderColor = new ColorRGBA(0.4f, 0.4f, 0.5f, 1f);

    // 回调
    public interface PanelResizeCallback {
        void onPanelResized(int newWidth, int newHeight);
        void onPanelMoved(int newX, int newY);
    }
    protected PanelResizeCallback resizeCallback;

    public enum ResizeDirection {
        NONE, LEFT, RIGHT, TOP, BOTTOM,
        TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
    }

    public DraggablePanel(SimpleApplication app, String title, int x, int y, int width, int height) {
        this.app = app;
        this.title = title;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        this.rootNode = new Node("DraggablePanel_" + title);
        this.contentNode = new Node("Content_" + title);

        // 加载字体
        this.font = app.getAssetManager().loadFont("Interface/Fonts/Default.fnt");

        initializePanel();
    }

    protected void initializePanel() {
        // 创建背景
        createBackground();

        // 创建标题栏
        createTitleBar();

        // 添加内容节点
        rootNode.attachChild(contentNode);

        // 设置位置
        updatePosition();
    }

    protected void createBackground() {
        Quad backgroundQuad = new Quad(width, height);
        backgroundGeom = new Geometry("PanelBackground", backgroundQuad);

        Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", backgroundColor);
        mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        backgroundGeom.setMaterial(mat);

        backgroundGeom.setLocalTranslation(0, 0, 0);
        rootNode.attachChild(backgroundGeom);
    }

    protected void createTitleBar() {
        // 标题栏背景
        Quad titleQuad = new Quad(width, TITLE_BAR_HEIGHT);
        titleBarGeom = new Geometry("TitleBar", titleQuad);

        Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", titleBarColor);
        titleBarGeom.setMaterial(mat);

        titleBarGeom.setLocalTranslation(0, height - TITLE_BAR_HEIGHT, 1);
        rootNode.attachChild(titleBarGeom);

        // 标题文字
        titleText = new BitmapText(font);
        titleText.setSize(14);
        titleText.setColor(ColorRGBA.White);
        titleText.setText(title);
        titleText.setLocalTranslation(8, height - 5, 2);
        rootNode.attachChild(titleText);
    }

    /**
     * 更新面板位置（基于 y 从底部算起）
     */
    protected void updatePosition() {
        rootNode.setLocalTranslation(x, y, 0);
    }

    /**
     * 更新面板大小（需要重建几何体）
     */
    public void updateSize(int newWidth, int newHeight) {
        this.width = Math.max(newWidth, minWidth);
        this.height = Math.max(newHeight, minHeight);

        // 重建背景
        if (backgroundGeom != null) {
            backgroundGeom.removeFromParent();
        }
        createBackground();

        // 重建标题栏
        if (titleBarGeom != null) {
            titleBarGeom.removeFromParent();
        }
        if (titleText != null) {
            titleText.removeFromParent();
        }
        createTitleBar();

        // 通知回调
        if (resizeCallback != null) {
            resizeCallback.onPanelResized(width, height);
        }
    }

    /**
     * 检查点击位置是否在标题栏上（用于开始拖拽）
     */
    public boolean isOnTitleBar(int mouseX, int mouseY) {
        int panelTop = y + height;
        int panelBottom = panelTop - TITLE_BAR_HEIGHT;

        return mouseX >= x && mouseX <= x + width &&
               mouseY >= panelBottom && mouseY <= panelTop;
    }

    /**
     * 检查点击位置是否在面板边缘（用于调整大小）
     * 返回调整方向
     */
    public ResizeDirection getResizeDirection(int mouseX, int mouseY) {
        boolean onLeft = mouseX >= x && mouseX <= x + RESIZE_BORDER;
        boolean onRight = mouseX >= x + width - RESIZE_BORDER && mouseX <= x + width;
        boolean onTop = mouseY >= y + height - RESIZE_BORDER && mouseY <= y + height;
        boolean onBottom = mouseY >= y && mouseY <= y + RESIZE_BORDER;

        // 检查是否在面板范围内
        if (mouseX < x || mouseX > x + width || mouseY < y || mouseY > y + height) {
            return ResizeDirection.NONE;
        }

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
     * 检查点击位置是否在面板内部
     */
    public boolean contains(int mouseX, int mouseY) {
        return mouseX >= x && mouseX <= x + width &&
               mouseY >= y && mouseY <= y + height;
    }

    /**
     * 开始拖拽
     */
    public void startDrag(int mouseX, int mouseY) {
        isDragging = true;
        dragOffsetX = mouseX - x;
        dragOffsetY = mouseY - y;
    }

    /**
     * 更新拖拽位置
     */
    public void updateDrag(int mouseX, int mouseY, int screenWidth, int screenHeight) {
        if (!isDragging) return;

        int newX = mouseX - dragOffsetX;
        int newY = mouseY - dragOffsetY;

        // 边界检查
        newX = Math.max(0, Math.min(newX, screenWidth - width));
        newY = Math.max(0, Math.min(newY, screenHeight - height));

        this.x = newX;
        this.y = newY;
        updatePosition();

        if (resizeCallback != null) {
            resizeCallback.onPanelMoved(x, y);
        }
    }

    /**
     * 结束拖拽
     */
    public void endDrag() {
        isDragging = false;
    }

    /**
     * 开始调整大小
     */
    public void startResize(int mouseX, int mouseY, ResizeDirection direction) {
        isResizing = true;
        resizeDirection = direction;
        dragOffsetX = mouseX;
        dragOffsetY = mouseY;
    }

    /**
     * 更新调整大小
     */
    public void updateResize(int mouseX, int mouseY) {
        if (!isResizing || resizeDirection == ResizeDirection.NONE) return;

        int deltaX = mouseX - dragOffsetX;
        int deltaY = mouseY - dragOffsetY;

        int newX = x, newY = y, newWidth = width, newHeight = height;

        switch (resizeDirection) {
            case LEFT:
                newX = x + deltaX;
                newWidth = width - deltaX;
                break;
            case RIGHT:
                newWidth = width + deltaX;
                break;
            case TOP:
                newHeight = height + deltaY;
                break;
            case BOTTOM:
                newY = y + deltaY;
                newHeight = height - deltaY;
                break;
            case TOP_LEFT:
                newX = x + deltaX;
                newWidth = width - deltaX;
                newHeight = height + deltaY;
                break;
            case TOP_RIGHT:
                newWidth = width + deltaX;
                newHeight = height + deltaY;
                break;
            case BOTTOM_LEFT:
                newX = x + deltaX;
                newWidth = width - deltaX;
                newY = y + deltaY;
                newHeight = height - deltaY;
                break;
            case BOTTOM_RIGHT:
                newWidth = width + deltaX;
                newY = y + deltaY;
                newHeight = height - deltaY;
                break;
        }

        // 应用最小尺寸限制
        if (newWidth >= minWidth && newHeight >= minHeight) {
            this.x = newX;
            this.y = newY;
            updateSize(newWidth, newHeight);
            updatePosition();

            dragOffsetX = mouseX;
            dragOffsetY = mouseY;
        }
    }

    /**
     * 结束调整大小
     */
    public void endResize() {
        isResizing = false;
        resizeDirection = ResizeDirection.NONE;
    }

    // Getters
    public Node getRootNode() { return rootNode; }
    public Node getContentNode() { return contentNode; }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int getContentHeight() { return height - TITLE_BAR_HEIGHT; }
    public boolean isDragging() { return isDragging; }
    public boolean isResizing() { return isResizing; }

    // Setters
    public void setMinSize(int minWidth, int minHeight) {
        this.minWidth = minWidth;
        this.minHeight = minHeight;
    }

    public void setResizeCallback(PanelResizeCallback callback) {
        this.resizeCallback = callback;
    }

    public void setTitle(String title) {
        this.title = title;
        if (titleText != null) {
            titleText.setText(title);
        }
    }

    public void setBackgroundColor(ColorRGBA color) {
        this.backgroundColor = color;
        if (backgroundGeom != null) {
            backgroundGeom.getMaterial().setColor("Color", color);
        }
    }

    public void setTitleBarColor(ColorRGBA color) {
        this.titleBarColor = color;
        if (titleBarGeom != null) {
            titleBarGeom.getMaterial().setColor("Color", color);
        }
    }

    /**
     * 清理资源
     */
    public void cleanup() {
        if (rootNode.getParent() != null) {
            rootNode.removeFromParent();
        }
    }
}
