package com.Hecate.puppet.editor.lemur;

import com.jme3.app.SimpleApplication;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.simsilica.lemur.*;
import com.simsilica.lemur.component.SpringGridLayout;

import java.util.HashMap;
import java.util.Map;

/**
 * 使用 Lemur GUI 重写的按钮面板
 * 替代原有的 ButtonColumnPanel
 */
public class LemurButtonPanel {

    private final SimpleApplication app;
    private final Container mainContainer;
    private final Node guiNode;

    // 按钮映射
    private final Map<String, Button> buttons = new HashMap<>();

    // 回调接口
    public interface ButtonClickCallback {
        void onButtonClicked(String buttonId);
    }
    private ButtonClickCallback callback;

    // 面板位置和大小
    private int panelX, panelY;
    private final int panelWidth;
    private final int panelHeight;

    public LemurButtonPanel(SimpleApplication app, int x, int y, int width, int height) {
        this.app = app;
        this.guiNode = app.getGuiNode();
        this.panelX = x;
        this.panelY = y;
        this.panelWidth = width;
        this.panelHeight = height;

        // 创建主容器
        mainContainer = new Container();
        mainContainer.setPreferredSize(new Vector3f(width, height, 0));

        // 初始化面板
        initializePanel();

        // 设置位置
        updatePosition();

        // 添加到GUI节点
        guiNode.attachChild(mainContainer);
    }

    private void initializePanel() {
        // 标题
        Label titleLabel = new Label("Tools");
        titleLabel.setFontSize(18);
        titleLabel.setColor(ColorRGBA.Yellow);
        mainContainer.addChild(titleLabel);

        // 分隔
        mainContainer.addChild(createSeparator());

        // === 文件操作组 ===
        mainContainer.addChild(createGroupLabel("File"));

        Container fileRow = createButtonRow();
        createButton(fileRow, "new", "New", 70);
        createButton(fileRow, "open", "Open", 70);
        createButton(fileRow, "save", "Save", 70);
        mainContainer.addChild(fileRow);

        Container fileRow2 = createButtonRow();
        createButton(fileRow2, "saveAs", "Save As", 100);
        createButton(fileRow2, "export", "Export", 100);
        mainContainer.addChild(fileRow2);

        mainContainer.addChild(createSeparator());

        // === 编辑操作组 ===
        mainContainer.addChild(createGroupLabel("Edit"));

        Container editRow = createButtonRow();
        createButton(editRow, "undo", "Undo", 70);
        createButton(editRow, "redo", "Redo", 70);
        mainContainer.addChild(editRow);

        Container editRow2 = createButtonRow();
        createButton(editRow2, "copy", "Copy", 70);
        createButton(editRow2, "paste", "Paste", 70);
        createButton(editRow2, "delete", "Delete", 70);
        mainContainer.addChild(editRow2);

        mainContainer.addChild(createSeparator());

        // === 骨骼操作组 ===
        mainContainer.addChild(createGroupLabel("Bone"));

        Container boneRow = createButtonRow();
        createButton(boneRow, "addBone", "Add Bone", 100);
        createButton(boneRow, "removeBone", "Remove", 100);
        mainContainer.addChild(boneRow);

        Container boneRow2 = createButtonRow();
        createButton(boneRow2, "setParent", "Set Parent", 100);
        createButton(boneRow2, "clearParent", "Clear Parent", 100);
        mainContainer.addChild(boneRow2);

        mainContainer.addChild(createSeparator());

        // === 视图操作组 ===
        mainContainer.addChild(createGroupLabel("View"));

        Container viewRow = createButtonRow();
        createButton(viewRow, "front", "Front", 60);
        createButton(viewRow, "back", "Back", 60);
        createButton(viewRow, "left", "Left", 60);
        createButton(viewRow, "right", "Right", 60);
        mainContainer.addChild(viewRow);

        Container viewRow2 = createButtonRow();
        createButton(viewRow2, "hideMode", "Hide Mode", 100);
        createButton(viewRow2, "showAll", "Show All", 100);
        mainContainer.addChild(viewRow2);

        mainContainer.addChild(createSeparator());

        // === 动画组 ===
        mainContainer.addChild(createGroupLabel("Animation"));

        Container animRow = createButtonRow();
        createButton(animRow, "play", "Play", 70);
        createButton(animRow, "stop", "Stop", 70);
        createButton(animRow, "addKey", "Add Key", 80);
        mainContainer.addChild(animRow);
    }

    /**
     * 创建按钮行容器
     */
    private Container createButtonRow() {
        Container row = new Container(new SpringGridLayout(Axis.X, Axis.Y, FillMode.None, FillMode.None));
        return row;
    }

    /**
     * 创建按钮
     */
    private void createButton(Container parent, String id, String label, int width) {
        Button button = new Button(label);
        button.setPreferredSize(new Vector3f(width, 30, 0));
        button.setFontSize(12);

        // 点击事件
        button.addClickCommands(source -> {
            if (callback != null) {
                callback.onButtonClicked(id);
            }
        });

        buttons.put(id, button);
        parent.addChild(button);
    }

    /**
     * 创建分组标签
     */
    private Label createGroupLabel(String text) {
        Label label = new Label("-- " + text + " --");
        label.setFontSize(14);
        label.setColor(new ColorRGBA(0.7f, 0.7f, 0.7f, 1f));
        return label;
    }

    /**
     * 创建分隔线
     */
    private Container createSeparator() {
        Container sep = new Container();
        sep.setPreferredSize(new Vector3f(panelWidth - 20, 8, 0));
        sep.setBackground(null);
        return sep;
    }

    /**
     * 更新面板位置
     */
    private void updatePosition() {
        mainContainer.setLocalTranslation(panelX, panelY + panelHeight, 0);
    }

    /**
     * 设置按钮激活状态（高亮）
     */
    public void setButtonActive(String id, boolean active) {
        Button button = buttons.get(id);
        if (button != null) {
            if (active) {
                button.setColor(ColorRGBA.Green);
            } else {
                button.setColor(ColorRGBA.White);
            }
        }
    }

    /**
     * 设置按钮文本
     */
    public void setButtonText(String id, String text) {
        Button button = buttons.get(id);
        if (button != null) {
            button.setText(text);
        }
    }

    /**
     * 设置按钮启用/禁用
     */
    public void setButtonEnabled(String id, boolean enabled) {
        Button button = buttons.get(id);
        if (button != null) {
            button.setEnabled(enabled);
            if (!enabled) {
                button.setColor(new ColorRGBA(0.5f, 0.5f, 0.5f, 1f));
            } else {
                button.setColor(ColorRGBA.White);
            }
        }
    }

    /**
     * 设置回调
     */
    public void setCallback(ButtonClickCallback callback) {
        this.callback = callback;
    }

    /**
     * 设置可见性
     */
    public void setVisible(boolean visible) {
        if (visible) {
            mainContainer.setCullHint(com.jme3.scene.Spatial.CullHint.Never);
        } else {
            mainContainer.setCullHint(com.jme3.scene.Spatial.CullHint.Always);
        }
    }

    /**
     * 获取主容器
     */
    public Container getContainer() {
        return mainContainer;
    }

    /**
     * 移除面板
     */
    public void cleanup() {
        if (mainContainer.getParent() != null) {
            mainContainer.removeFromParent();
        }
    }

    /**
     * 设置位置
     */
    public void setPosition(int x, int y) {
        this.panelX = x;
        this.panelY = y;
        updatePosition();
    }
}
