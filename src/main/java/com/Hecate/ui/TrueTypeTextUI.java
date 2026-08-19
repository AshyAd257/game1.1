package com.Hecate.ui;

import com.Hecate.ui.common.TTFontLoader;
import com.jme3.app.SimpleApplication;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Node;

/**
 * TTF字体文本UI组件
 * 使用TTFontLoader渲染TrueType字体（支持中英文像素字体）
 */
public class TrueTypeTextUI {

    private final SimpleApplication app;
    private TTFontLoader fontLoader;
    private Node textNode;

    public TrueTypeTextUI(SimpleApplication app) {
        this.app = app;
    }

    /**
     * 加载字体
     * @param fontPath 字体文件路径（相对于 resources）
     * @param size 字体大小
     */
    public void loadFont(String fontPath, int size) {
        try {
            // 使用TTFontLoader加载字体
            fontLoader = TTFontLoader.loadFontFromResource(
                app.getAssetManager(),
                fontPath,
                (float) size
            );

            if (fontLoader == null) {
                System.err.println("字体加载失败：找不到字体文件 " + fontPath);
            }
        } catch (Exception e) {
            System.err.println("字体加载失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 创建文本节点
     */
    public Node createText(String text, ColorRGBA color, float x, float y) {
        if (fontLoader == null) {
            System.err.println("字体未初始化");
            return null;
        }

        // 创建文本节点
        Node node = fontLoader.createText(text, color);
        node.setLocalTranslation(x, y, 1000);
        return node;
    }

    /**
     * 在屏幕上显示文本
     */
    public void showText(String text, ColorRGBA color, float x, float y) {
        if (fontLoader == null) {
            System.err.println("字体未初始化");
            return;
        }

        // 移除旧的文本节点
        if (textNode != null) {
            textNode.removeFromParent();
        }

        // 创建新的文本节点
        textNode = fontLoader.createText(text, color);
        textNode.setLocalTranslation(x, y, 1000);

        // 添加到GUI节点
        app.getGuiNode().attachChild(textNode);
    }

    /**
     * 移除文本显示
     */
    public void hideText() {
        if (textNode != null) {
            textNode.removeFromParent();
            textNode = null;
        }
    }

    /**
     * 清理资源
     */
    public void cleanup() {
        hideText();
    }
}
