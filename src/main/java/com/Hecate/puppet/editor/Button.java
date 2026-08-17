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
 * 简单的UI按钮组件
 */
public class Button {

    private final SimpleApplication app;
    private final BitmapFont font;
    private final TTFontLoader ttfLoader;  // 新增: TTF字体加载器
    private final Node rootNode;

    private String text;
    private Geometry background;
    private BitmapText labelText;  // 用于BitmapFont
    private Node ttfTextNode;      // 新增: 用于TTFontLoader
    private ButtonClickListener clickListener;

    private int x, y;  // 改为可变，支持位置更新
    private final int width, height;

    // 颜色
    private ColorRGBA normalColor = new ColorRGBA(0.3f, 0.3f, 0.4f, 0.9f);
    private ColorRGBA hoverColor = new ColorRGBA(0.4f, 0.4f, 0.5f, 0.9f);
    private ColorRGBA activeColor = new ColorRGBA(0.2f, 0.6f, 0.3f, 0.9f);
    private ColorRGBA pressedColor = new ColorRGBA(0.6f, 0.6f, 0.7f, 0.9f);
    private ColorRGBA textColor = ColorRGBA.White;

    private boolean isActive = false;
    private boolean isHovered = false;
    private boolean isPressed = false;
    private float pressedTimer = 0f;

    /**
     * 按钮点击监听器接口
     */
    public interface ButtonClickListener {
        void onClick();
    }

    // 原构造函数 - 使用BitmapFont
    public Button(SimpleApplication app, BitmapFont font, String text, int x, int y, int width, int height) {
        this.app = app;
        this.font = font;
        this.ttfLoader = null;
        this.text = text;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        this.rootNode = new Node("Button_" + text);
        initializeButton();
    }

    // 新构造函数 - 使用TTFontLoader
    public Button(SimpleApplication app, TTFontLoader ttfLoader, String text, int x, int y, int width, int height) {
        this.app = app;
        this.font = null;
        this.ttfLoader = ttfLoader;
        this.text = text;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        this.rootNode = new Node("Button_" + text);
        initializeButton();
    }

    /**
     * 初始化按钮
     */
    private void initializeButton() {
        // 创建背景
        Quad quad = new Quad(width, height);
        background = new Geometry("ButtonBackground", quad);

        Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", normalColor);
        mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        background.setMaterial(mat);

        background.setLocalTranslation(x, y, 0);
        rootNode.attachChild(background);

        // 创建文本 - 根据字体类型选择
        if (ttfLoader != null) {
            // 使用TTFontLoader
            ttfTextNode = ttfLoader.createText(text, textColor);

            // 居中文本，并上移10像素以适应TTF字体渲染
            float textWidth = ttfLoader.getTextWidth(text);
            float textHeight = ttfLoader.getLineHeight();
            float textX = x + (width - textWidth) / 2;
            float textY = y + (height - textHeight) / 2 + 10;  // 上移10像素

            ttfTextNode.setLocalTranslation(textX, textY, 1);
            rootNode.attachChild(ttfTextNode);
        } else {
            // 使用BitmapFont
            labelText = new BitmapText(font);
            labelText.setText(text);
            labelText.setSize(font.getCharSet().getRenderedSize() * 1.05f);
            labelText.setColor(textColor);

            // 居中文本
            float textWidth = labelText.getLineWidth();
            float textHeight = labelText.getLineHeight();
            float textX = x + (width - textWidth) / 2;
            float textY = y + (height + textHeight) / 2;

            labelText.setLocalTranslation(textX, textY, 1);
            rootNode.attachChild(labelText);
        }
    }

    /**
     * 处理鼠标点击
     */
    public boolean handleMouseClick(int mouseX, int mouseY) {
        if (isPointInButton(mouseX, mouseY)) {
            // 触发按下效果
            isPressed = true;
            pressedTimer = 0.5f; // 500ms按下效果
            updateColor();

            if (clickListener != null) {
                clickListener.onClick();
            }
            return true;
        }
        return false;
    }

    /**
     * 更新按钮状态（需要在update循环中调用）
     */
    public void update(float tpf) {
        if (isPressed) {
            pressedTimer -= tpf;
            if (pressedTimer <= 0) {
                isPressed = false;
                updateColor();
            }
        }
    }

    /**
     * 更新悬停状态
     */
    public void updateHover(int mouseX, int mouseY) {
        boolean wasHovered = isHovered;
        isHovered = isPointInButton(mouseX, mouseY);

        if (wasHovered != isHovered) {
            updateColor();
        }
    }

    /**
     * 检查点是否在按钮内
     */
    private boolean isPointInButton(int mouseX, int mouseY) {
        return mouseX >= x && mouseX <= x + width &&
               mouseY >= y && mouseY <= y + height;
    }

    /**
     * 更新颜色
     */
    private void updateColor() {
        ColorRGBA color;
        if (isPressed) {
            color = pressedColor;
        } else if (isActive) {
            color = activeColor;
        } else if (isHovered) {
            color = hoverColor;
        } else {
            color = normalColor;
        }

        Material mat = background.getMaterial();
        mat.setColor("Color", color);
    }

    /**
     * 设置激活状态
     */
    public void setActive(boolean active) {
        this.isActive = active;
        updateColor();
    }

    /**
     * 获取激活状态
     */
    public boolean isActive() {
        return isActive;
    }

    /**
     * 设置点击监听器
     */
    public void setClickListener(ButtonClickListener listener) {
        this.clickListener = listener;
    }

    /**
     * 设置文本
     */
    public void setText(String text) {
        this.text = text;

        if (labelText != null) {
            // 使用BitmapFont
            labelText.setText(text);

            // 重新居中
            float textWidth = labelText.getLineWidth();
            float textHeight = labelText.getLineHeight();
            float textX = x + (width - textWidth) / 2;
            float textY = y + (height + textHeight) / 2;
            labelText.setLocalTranslation(textX, textY, 1);
        } else if (ttfTextNode != null && ttfLoader != null) {
            // 使用TTFontLoader - 需要重新创建文本节点
            rootNode.detachChild(ttfTextNode);
            ttfTextNode = ttfLoader.createText(text, textColor);

            // 居中文本，并上移10像素
            float textWidth = ttfLoader.getTextWidth(text);
            float textHeight = ttfLoader.getLineHeight();
            float textX = x + (width - textWidth) / 2;
            float textY = y + (height - textHeight) / 2 + 10;  // 上移10像素
            ttfTextNode.setLocalTranslation(textX, textY, 1);
            rootNode.attachChild(ttfTextNode);
        }
    }

    /**
     * 设置颜色方案
     */
    public void setColors(ColorRGBA normal, ColorRGBA hover, ColorRGBA active) {
        this.normalColor = normal;
        this.hoverColor = hover;
        this.activeColor = active;
        updateColor();
    }

    /**
     * 获取根节点
     */
    public Node getRootNode() {
        return rootNode;
    }

    /**
     * 设置可见性
     */
    public void setVisible(boolean visible) {
        rootNode.setCullHint(visible ?
            com.jme3.scene.Spatial.CullHint.Never :
            com.jme3.scene.Spatial.CullHint.Always);
    }

    /**
     * 设置按钮位置（用于拖动面板时更新按钮位置）
     */
    public void setPosition(int newX, int newY) {
        this.x = newX;
        this.y = newY;

        // 更新背景位置
        if (background != null) {
            background.setLocalTranslation(x, y, 0);
        }

        // 更新文本位置
        if (labelText != null) {
            float textWidth = labelText.getLineWidth();
            float textHeight = labelText.getLineHeight();
            float textX = x + (width - textWidth) / 2;
            float textY = y + (height + textHeight) / 2;
            labelText.setLocalTranslation(textX, textY, 1);
        }
    }
}
