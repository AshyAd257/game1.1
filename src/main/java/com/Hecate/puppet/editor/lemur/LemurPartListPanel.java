package com.Hecate.puppet.editor.lemur;

import com.jme3.app.SimpleApplication;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.simsilica.lemur.*;
import com.simsilica.lemur.component.SpringGridLayout;
import com.simsilica.lemur.core.VersionedList;
import com.simsilica.lemur.list.SelectionModel;

import java.util.ArrayList;
import java.util.List;

/**
 * 使用 Lemur GUI 重写的部件层级面板
 * 替代原有的 PartListPanel
 */
public class LemurPartListPanel {

    private final SimpleApplication app;
    private final Container mainContainer;
    private final Node guiNode;

    // 部件列表
    private ListBox<String> partListBox;
    private VersionedList<String> partList;
    private SelectionModel selectionModel;

    // 当前选中的部件索引
    private int selectedIndex = -1;

    // 回调接口
    public interface PartSelectionCallback {
        void onPartSelected(int index, String partName);
    }
    private PartSelectionCallback callback;

    // 面板位置和大小
    private int panelX, panelY;
    private final int panelWidth;
    private final int panelHeight;

    // 部件数据
    private final List<PartInfo> parts = new ArrayList<>();

    /**
     * 部件信息
     */
    public static class PartInfo {
        public String name;
        public int boneIndex;
        public boolean isVisible;
        public int depth; // 层级深度（用于缩进显示）

        public PartInfo(String name, int boneIndex, boolean isVisible, int depth) {
            this.name = name;
            this.boneIndex = boneIndex;
            this.isVisible = isVisible;
            this.depth = depth;
        }
    }

    public LemurPartListPanel(SimpleApplication app, int x, int y, int width, int height) {
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
        // 标题栏
        Container titleBar = new Container(new SpringGridLayout(Axis.X, Axis.Y, FillMode.Last, FillMode.None));
        titleBar.setPreferredSize(new Vector3f(panelWidth - 10, 30, 0));

        Label titleLabel = new Label("Part Hierarchy");
        titleLabel.setFontSize(16);
        titleLabel.setColor(ColorRGBA.Yellow);
        titleBar.addChild(titleLabel);

        // 搜索/过滤按钮（可选）
        Button refreshButton = new Button("Refresh");
        refreshButton.setFontSize(12);
        refreshButton.setPreferredSize(new Vector3f(60, 25, 0));
        refreshButton.addClickCommands(source -> {

        });
        titleBar.addChild(refreshButton);

        mainContainer.addChild(titleBar);

        // 分隔
        mainContainer.addChild(createSeparator());

        // 创建列表
        partList = new VersionedList<>();
        partListBox = new ListBox<>(partList);
        partListBox.setPreferredSize(new Vector3f(panelWidth - 20, panelHeight - 100, 0));
        partListBox.setVisibleItems(15);

        // 选择监听
        selectionModel = partListBox.getSelectionModel();

        mainContainer.addChild(partListBox);

        // 底部操作按钮
        mainContainer.addChild(createSeparator());

        Container buttonRow = new Container(new SpringGridLayout(Axis.X, Axis.Y, FillMode.None, FillMode.None));

        Button selectButton = new Button("Select");
        selectButton.setFontSize(12);
        selectButton.setPreferredSize(new Vector3f(70, 28, 0));
        selectButton.addClickCommands(source -> {
            Integer sel = selectionModel.getSelection();
            if (sel != null && sel >= 0 && sel < parts.size()) {
                selectedIndex = sel;
                PartInfo part = parts.get(sel);
                if (callback != null) {
                    callback.onPartSelected(part.boneIndex, part.name);
                }
            }
        });
        buttonRow.addChild(selectButton);

        Button toggleVisButton = new Button("Toggle Vis");
        toggleVisButton.setFontSize(12);
        toggleVisButton.setPreferredSize(new Vector3f(80, 28, 0));
        toggleVisButton.addClickCommands(source -> {
            Integer sel = selectionModel.getSelection();
            if (sel != null && sel >= 0 && sel < parts.size()) {
                PartInfo part = parts.get(sel);
                part.isVisible = !part.isVisible;
                updateListItem(sel, part);
            }
        });
        buttonRow.addChild(toggleVisButton);

        Button expandButton = new Button("Expand All");
        expandButton.setFontSize(12);
        expandButton.setPreferredSize(new Vector3f(80, 28, 0));
        buttonRow.addChild(expandButton);

        mainContainer.addChild(buttonRow);
    }

    /**
     * 创建分隔线
     */
    private Container createSeparator() {
        Container sep = new Container();
        sep.setPreferredSize(new Vector3f(panelWidth - 20, 5, 0));
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
     * 设置部件列表
     */
    public void setParts(List<PartInfo> newParts) {
        parts.clear();
        parts.addAll(newParts);

        partList.clear();
        for (PartInfo part : parts) {
            String indent = "  ".repeat(part.depth);
            String visIcon = part.isVisible ? "[O]" : "[X]";
            partList.add(indent + visIcon + " " + part.name);
        }
    }

    /**
     * 添加部件
     */
    public void addPart(String name, int boneIndex, boolean isVisible, int depth) {
        PartInfo part = new PartInfo(name, boneIndex, isVisible, depth);
        parts.add(part);

        String indent = "  ".repeat(depth);
        String visIcon = isVisible ? "[O]" : "[X]";
        partList.add(indent + visIcon + " " + name);
    }

    /**
     * 清空列表
     */
    public void clearParts() {
        parts.clear();
        partList.clear();
    }

    /**
     * 更新单个列表项
     */
    private void updateListItem(int index, PartInfo part) {
        if (index >= 0 && index < partList.size()) {
            String indent = "  ".repeat(part.depth);
            String visIcon = part.isVisible ? "[O]" : "[X]";
            partList.set(index, indent + visIcon + " " + part.name);
        }
    }

    /**
     * 设置选中项
     */
    public void setSelectedIndex(int index) {
        if (index >= 0 && index < parts.size()) {
            selectedIndex = index;
            selectionModel.setSelection(index);
        }
    }

    /**
     * 通过骨骼索引设置选中项
     */
    public void setSelectedByBoneIndex(int boneIndex) {
        for (int i = 0; i < parts.size(); i++) {
            if (parts.get(i).boneIndex == boneIndex) {
                setSelectedIndex(i);
                return;
            }
        }
    }

    /**
     * 获取选中的部件索引
     */
    public int getSelectedIndex() {
        return selectedIndex;
    }

    /**
     * 获取选中的骨骼索引
     */
    public int getSelectedBoneIndex() {
        if (selectedIndex >= 0 && selectedIndex < parts.size()) {
            return parts.get(selectedIndex).boneIndex;
        }
        return -1;
    }

    /**
     * 设置回调
     */
    public void setCallback(PartSelectionCallback callback) {
        this.callback = callback;
    }

    /**
     * 更新方法（检查选择变化）
     */
    public void update(float tpf) {
        // 检查选择变化
        Integer sel = selectionModel.getSelection();
        if (sel != null && sel != selectedIndex && sel >= 0 && sel < parts.size()) {
            selectedIndex = sel;
            PartInfo part = parts.get(sel);
            if (callback != null) {
                callback.onPartSelected(part.boneIndex, part.name);
            }
        }
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
