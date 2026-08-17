package com.Hecate.ui.common;

import com.jme3.app.SimpleApplication;
import com.jme3.input.KeyInput;
import com.jme3.input.RawInputListener;
import com.jme3.input.event.*;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Quad;

/**
 * 可编辑的文本输入框组件
 * 支持键盘输入、光标显示、文本编辑
 */
public class TextField {

    private final SimpleApplication app;
    private final TTFontLoader fontLoader;
    private final Node rootNode;

    private String text = "";
    private int cursorPosition = 0;
    private int maxLength = 100;
    private float maxWidth = -1;  // -1表示无限制

    private Geometry background;
    private Node textNode;
    private Geometry cursor;

    private int x, y;
    private final int width, height;

    // 保存光标的尺寸和Y偏移，用于updateCursorPosition
    private float cursorWidth;
    private float cursorHeight;
    private float cursorYOffset;

    // 颜色配置
    private ColorRGBA backgroundColor = new ColorRGBA(0.2f, 0.2f, 0.25f, 0.9f);
    private ColorRGBA focusedBackgroundColor = new ColorRGBA(0.25f, 0.25f, 0.3f, 0.9f);
    private ColorRGBA textColor = ColorRGBA.White;
    private ColorRGBA cursorColor = ColorRGBA.White;

    // 状态
    private boolean isFocused = false;
    private boolean cursorVisible = true;
    private float cursorBlinkTimer = 0f;
    private float cursorBlinkInterval = 0.5f;

    // 输入监听器
    private RawInputListener inputListener;

    // 值变化监听器
    private TextChangeListener changeListener;

    /**
     * 文本变化监听器接口
     */
    public interface TextChangeListener {
        void onTextChanged(String newText);
    }

    /**
     * 构造函数
     */
    public TextField(SimpleApplication app, TTFontLoader fontLoader, String initialText,
                     int x, int y, int width, int height) {
        this.app = app;
        this.fontLoader = fontLoader;
        this.text = initialText != null ? initialText : "";
        this.cursorPosition = this.text.length();
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        this.rootNode = new Node("TextField");
        initializeTextField();
        setupInputListener();
    }

    /**
     * 初始化文本框
     */
    private void initializeTextField() {
        // 创建背景
        Quad quad = new Quad(width, height);
        background = new Geometry("TextFieldBackground", quad);

        Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", backgroundColor);
        mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        background.setMaterial(mat);

        background.setLocalTranslation(x, y, 1004);
        rootNode.attachChild(background);

        // 创建光标（宽度为高度的1/3，更容易看到）
        cursorWidth = height / 3.0f;  // 搜索框高36px，光标宽12px
        cursorHeight = height * 0.8f;  // 光标高度为搜索框的80%

        Quad cursorQuad = new Quad(cursorWidth, cursorHeight);
        cursor = new Geometry("Cursor", cursorQuad);

        Material cursorMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        cursorMat.setColor("Color", cursorColor);
        cursorMat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        cursor.setMaterial(cursorMat);

        // 光标位置：左边距5px，垂直居中，下移8像素
        cursorYOffset = (height - cursorHeight) / 2.0f - 8;
        cursor.setLocalTranslation(x + 5, y + cursorYOffset, 1004.1f);

        rootNode.attachChild(cursor);

        // 初始化文本显示
        updateTextDisplay();
    }

    /**
     * 更新文本显示
     */
    private void updateTextDisplay() {
        // 移除旧的文本节点
        if (textNode != null) {
            rootNode.detachChild(textNode);
        }

        // 创建新的文本节点
        if (!text.isEmpty()) {
            textNode = fontLoader.createText(text, textColor);
            // 文本位置下移8像素
            textNode.setLocalTranslation(x + 5, y + height - 5 - 8, 1004.05f);
            rootNode.attachChild(textNode);
        }

        // 更新光标位置
        updateCursorPosition();
    }

    /**
     * 更新光标位置
     */
    private void updateCursorPosition() {
        if (cursor == null) return;

        // 计算光标位置
        String textBeforeCursor = text.substring(0, Math.min(cursorPosition, text.length()));
        float textWidth = fontLoader.getTextWidth(textBeforeCursor);

        // 使用保存的cursorYOffset和正确的z坐标
        cursor.setLocalTranslation(x + 5 + textWidth, y + cursorYOffset, 1004.1f);
    }

    /**
     * 设置输入监听器
     */
    private void setupInputListener() {
        inputListener = new RawInputListener() {
            @Override
            public void onKeyEvent(KeyInputEvent evt) {
                if (!isFocused) return;

                if (evt.isPressed()) {
                    handleKeyPress(evt.getKeyCode(), evt.getKeyChar());
                }
            }

            @Override
            public void onMouseButtonEvent(MouseButtonEvent evt) {}

            @Override
            public void onMouseMotionEvent(MouseMotionEvent evt) {}

            @Override
            public void onJoyAxisEvent(JoyAxisEvent evt) {}

            @Override
            public void onJoyButtonEvent(JoyButtonEvent evt) {}

            @Override
            public void beginInput() {}

            @Override
            public void endInput() {}

            @Override
            public void onTouchEvent(TouchEvent evt) {}
        };
    }

    /**
     * 处理按键输入
     */
    private void handleKeyPress(int keyCode, char keyChar) {
        switch (keyCode) {
            case KeyInput.KEY_BACK:
                // 退格键 - 删除光标前的字符
                deleteCharBeforeCursor();
                break;

            case KeyInput.KEY_DELETE:
                // Delete键 - 删除光标后的字符
                deleteCharAtCursor();
                break;

            case KeyInput.KEY_LEFT:
                // 左箭头 - 光标左移
                moveCursorLeft();
                break;

            case KeyInput.KEY_RIGHT:
                // 右箭头 - 光标右移
                moveCursorRight();
                break;

            case KeyInput.KEY_HOME:
                // Home键 - 光标移到开头
                cursorPosition = 0;
                updateCursorPosition();
                break;

            case KeyInput.KEY_END:
                // End键 - 光标移到末尾
                cursorPosition = text.length();
                updateCursorPosition();
                break;

            case KeyInput.KEY_RETURN:
            case KeyInput.KEY_NUMPADENTER:
                // 回车键 - 可以触发提交事件
                break;

            default:
                // 普通字符输入
                if (isPrintableChar(keyChar)) {
                    insertChar(keyChar);
                }
                break;
        }
    }

    /**
     * 插入字符
     */
    private void insertChar(char c) {
        if (text.length() >= maxLength) return;

        String before = text.substring(0, cursorPosition);
        String after = text.substring(cursorPosition);
        String newText = before + c + after;

        // 检查最大宽度限制
        if (maxWidth > 0) {
            float textWidth = fontLoader.getTextWidth(newText);
            if (textWidth > maxWidth) {
                return;  // 超过最大宽度，不插入
            }
        }

        text = newText;
        cursorPosition++;

        updateTextDisplay();
        notifyTextChanged();
    }

    /**
     * 删除光标前的字符（退格）
     */
    private void deleteCharBeforeCursor() {
        if (cursorPosition == 0) return;

        String before = text.substring(0, cursorPosition - 1);
        String after = text.substring(cursorPosition);
        text = before + after;
        cursorPosition--;

        updateTextDisplay();
        notifyTextChanged();
    }

    /**
     * 删除光标处的字符（Delete键）
     */
    private void deleteCharAtCursor() {
        if (cursorPosition >= text.length()) return;

        String before = text.substring(0, cursorPosition);
        String after = text.substring(cursorPosition + 1);
        text = before + after;

        updateTextDisplay();
        notifyTextChanged();
    }

    /**
     * 光标左移
     */
    private void moveCursorLeft() {
        if (cursorPosition > 0) {
            cursorPosition--;
            updateCursorPosition();
        }
    }

    /**
     * 光标右移
     */
    private void moveCursorRight() {
        if (cursorPosition < text.length()) {
            cursorPosition++;
            updateCursorPosition();
        }
    }

    /**
     * 判断是否为可打印字符
     */
    private boolean isPrintableChar(char c) {
        return c >= 32 && c < 127 || c >= 128; // ASCII可打印字符或Unicode字符
    }

    /**
     * 通知文本变化
     */
    private void notifyTextChanged() {
        if (changeListener != null) {
            changeListener.onTextChanged(text);
        }
    }

    /**
     * 更新方法（每帧调用）
     * 用于更新光标闪烁
     */
    public void update(float tpf) {
        if (!isFocused) {
            cursor.setCullHint(com.jme3.scene.Spatial.CullHint.Always);
            return;
        }

        // 光标闪烁逻辑
        cursorBlinkTimer += tpf;
        if (cursorBlinkTimer >= cursorBlinkInterval) {
            cursorBlinkTimer = 0;
            cursorVisible = !cursorVisible;
            cursor.setCullHint(cursorVisible ?
                com.jme3.scene.Spatial.CullHint.Never :
                com.jme3.scene.Spatial.CullHint.Always);
        }
    }

    // ==================== 公共API方法 ====================

    /**
     * 获取文本内容
     */
    public String getText() {
        return text;
    }

    /**
     * 设置文本内容
     */
    public void setText(String newText) {
        this.text = newText != null ? newText : "";
        this.cursorPosition = Math.min(cursorPosition, text.length());
        updateTextDisplay();
    }

    /**
     * 设置焦点状态
     */
    public void setFocused(boolean focused) {

        this.isFocused = focused;

        // 更新背景颜色
        Material mat = background.getMaterial();
        mat.setColor("Color", focused ? focusedBackgroundColor : backgroundColor);

        // 重置光标闪烁
        cursorBlinkTimer = 0;
        cursorVisible = true;

        if (focused) {
            // 获得焦点时，注册输入监听器
            app.getInputManager().addRawInputListener(inputListener);
            // 立即显示光标
            cursor.setCullHint(com.jme3.scene.Spatial.CullHint.Never);
        } else {
            // 失去焦点时，移除输入监听器
            app.getInputManager().removeRawInputListener(inputListener);
            cursor.setCullHint(com.jme3.scene.Spatial.CullHint.Always);
        }
    }

    /**
     * 获取焦点状态
     */
    public boolean isFocused() {
        return isFocused;
    }

    /**
     * 获取根节点
     */
    public Node getRootNode() {
        return rootNode;
    }

    /**
     * 设置文本变化监听器
     */
    public void setTextChangeListener(TextChangeListener listener) {
        this.changeListener = listener;
    }

    /**
     * 设置位置
     */
    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
        background.setLocalTranslation(x, y, 0);
        updateTextDisplay();
        updateCursorPosition();
    }

    /**
     * 设置最大长度
     */
    public void setMaxLength(int maxLength) {
        this.maxLength = maxLength;
    }

    /**
     * 设置最大宽度（像素）
     */
    public void setMaxWidth(float maxWidth) {
        this.maxWidth = maxWidth;
    }

    /**
     * 设置背景颜色
     */
    public void setBackgroundColor(ColorRGBA color) {
        this.backgroundColor = color;
        if (!isFocused) {
            background.getMaterial().setColor("Color", color);
        }
    }

    /**
     * 设置文本颜色
     */
    public void setTextColor(ColorRGBA color) {
        this.textColor = color;
        updateTextDisplay();
    }

    /**
     * 清理资源
     */
    public void cleanup() {
        if (isFocused) {
            app.getInputManager().removeRawInputListener(inputListener);
        }
        rootNode.removeFromParent();
    }
}
