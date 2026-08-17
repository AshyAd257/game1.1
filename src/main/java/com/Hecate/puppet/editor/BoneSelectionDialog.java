package com.Hecate.puppet.editor;

import com.jme3.app.SimpleApplication;
import com.jme3.font.BitmapFont;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Node;
import com.Hecate.puppet.editor.core.EditorBone;
import com.Hecate.puppet.editor.core.EditorSkeleton;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 骨骼多选对话框
 * 显示所有骨骼，支持复选框多选
 */
public class BoneSelectionDialog {

    private final SimpleApplication app;
    private final BitmapFont guiFont;
    private final TTFontLoader ttfLoader;
    private final Node rootNode;

    // 布局参数
    private final int x;
    private final int y;
    private final int width;
    private final int height;

    // 数据
    private EditorSkeleton skeleton;
    private Map<EditorBone, Boolean> selectionStates;  // 骨骼 -> 是否选中
    private List<Button> checkboxButtons;  // 复选框按钮列表

    // UI组件
    private Button okButton;
    private Button cancelButton;
    private Button selectAllButton;
    private Button deselectAllButton;
    private Node boneListNode;  // 骨骼列表容器

    // 回调
    private DialogResultListener resultListener;
    private boolean isVisible;

    /**
     * 创建骨骼选择对话框
     */
    public BoneSelectionDialog(SimpleApplication app, BitmapFont font, TTFontLoader ttfLoader,
                               int x, int y, int width, int height) {
        this.app = app;
        this.guiFont = font;
        this.ttfLoader = ttfLoader;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.rootNode = new Node("BoneSelectionDialog");
        this.selectionStates = new HashMap<>();
        this.checkboxButtons = new ArrayList<>();
        this.boneListNode = new Node("BoneList");
        this.isVisible = false;

        initializeUI();
    }

    /**
     * 初始化UI
     */
    private void initializeUI() {
        // 背景面板（简单的半透明背景）
        // TODO: 可以用Quad添加实际的背景矩形

        // 标题文本
        com.jme3.font.BitmapText titleText = new com.jme3.font.BitmapText(guiFont);
        titleText.setText("选择骨骼 (Select Bones)");
        titleText.setColor(ColorRGBA.White);
        titleText.setLocalTranslation(x + 10, y + height - 10, 1);
        rootNode.attachChild(titleText);

        // 按钮区域（底部）
        int buttonAreaY = y + 50;
        int buttonWidth = (width - 40) / 2;

        // OK按钮
        if (ttfLoader != null) {
            okButton = new Button(app, ttfLoader,
                "确定 (OK)", x + 10, buttonAreaY, buttonWidth, 35);
        } else {
            okButton = new Button(app, guiFont,
                "确定 (OK)", x + 10, buttonAreaY, buttonWidth, 35);
        }
        okButton.setClickListener(() -> onOkClicked());
        rootNode.attachChild(okButton.getRootNode());

        // Cancel按钮
        if (ttfLoader != null) {
            cancelButton = new Button(app, ttfLoader,
                "取消 (Cancel)", x + 15 + buttonWidth, buttonAreaY, buttonWidth, 35);
        } else {
            cancelButton = new Button(app, guiFont,
                "取消 (Cancel)", x + 15 + buttonWidth, buttonAreaY, buttonWidth, 35);
        }
        cancelButton.setClickListener(() -> onCancelClicked());
        rootNode.attachChild(cancelButton.getRootNode());

        // 全选/反选按钮（顶部下方）
        int toggleButtonY = y + height - 50;
        int toggleButtonWidth = (width - 30) / 2;

        if (ttfLoader != null) {
            selectAllButton = new Button(app, ttfLoader,
                "全选 (All)", x + 10, toggleButtonY, toggleButtonWidth, 30);
        } else {
            selectAllButton = new Button(app, guiFont,
                "全选 (All)", x + 10, toggleButtonY, toggleButtonWidth, 30);
        }
        selectAllButton.setClickListener(() -> selectAll());
        rootNode.attachChild(selectAllButton.getRootNode());

        if (ttfLoader != null) {
            deselectAllButton = new Button(app, ttfLoader,
                "取消全选 (None)", x + 15 + toggleButtonWidth, toggleButtonY, toggleButtonWidth, 30);
        } else {
            deselectAllButton = new Button(app, guiFont,
                "取消全选 (None)", x + 15 + toggleButtonWidth, toggleButtonY, toggleButtonWidth, 30);
        }
        deselectAllButton.setClickListener(() -> deselectAll());
        rootNode.attachChild(deselectAllButton.getRootNode());

        // 骨骼列表区域
        rootNode.attachChild(boneListNode);
    }

    /**
     * 设置骨骼系统
     */
    public void setSkeleton(EditorSkeleton skeleton) {
        this.skeleton = skeleton;
        rebuildBoneList();
    }

    /**
     * 重建骨骼列表
     */
    private void rebuildBoneList() {
        // 清空现有列表
        boneListNode.detachAllChildren();
        checkboxButtons.clear();
        selectionStates.clear();

        if (skeleton == null) {
            return;
        }

        List<EditorBone> bones = skeleton.getAllBones();
        if (bones.isEmpty()) {
            return;
        }

        // 计算可用高度
        int listStartY = y + height - 90;  // 标题下方
        int listEndY = y + 90;  // 按钮上方
        int availableHeight = listStartY - listEndY;
        int itemHeight = 30;
        int maxVisibleItems = availableHeight / itemHeight;

        // 创建复选框按钮
        int currentY = listStartY;
        int itemIndex = 0;

        for (EditorBone bone : bones) {
            if (itemIndex >= maxVisibleItems) {
                break;  // 超出可显示区域
            }

            // 初始化选择状态
            selectionStates.put(bone, false);

            // 创建复选框按钮（初始文本为 "☐ 骨骼名"）
            String buttonText = "☐ " + bone.getName();
            Button checkboxButton;

            if (ttfLoader != null) {
                checkboxButton = new Button(app, ttfLoader,
                    buttonText, x + 10, currentY, width - 20, itemHeight - 2);
            } else {
                checkboxButton = new Button(app, guiFont,
                    buttonText, x + 10, currentY, width - 20, itemHeight - 2);
            }

            // 设置点击监听器（切换选中状态）
            final EditorBone currentBone = bone;
            final Button currentButton = checkboxButton;
            checkboxButton.setClickListener(() -> toggleBoneSelection(currentBone, currentButton));

            checkboxButtons.add(checkboxButton);
            boneListNode.attachChild(checkboxButton.getRootNode());

            currentY -= itemHeight;
            itemIndex++;
        }
    }

    /**
     * 切换骨骼选中状态
     */
    private void toggleBoneSelection(EditorBone bone, Button button) {
        boolean currentState = selectionStates.getOrDefault(bone, false);
        boolean newState = !currentState;
        selectionStates.put(bone, newState);

        // 更新按钮文本
        String prefix = newState ? "☑ " : "☐ ";
        button.setText(prefix + bone.getName());
    }

    /**
     * 全选
     */
    private void selectAll() {
        if (skeleton == null) {
            return;
        }

        List<EditorBone> bones = skeleton.getAllBones();
        for (int i = 0; i < bones.size() && i < checkboxButtons.size(); i++) {
            EditorBone bone = bones.get(i);
            Button button = checkboxButtons.get(i);

            selectionStates.put(bone, true);
            button.setText("☑ " + bone.getName());
        }
    }

    /**
     * 取消全选
     */
    private void deselectAll() {
        if (skeleton == null) {
            return;
        }

        List<EditorBone> bones = skeleton.getAllBones();
        for (int i = 0; i < bones.size() && i < checkboxButtons.size(); i++) {
            EditorBone bone = bones.get(i);
            Button button = checkboxButtons.get(i);

            selectionStates.put(bone, false);
            button.setText("☐ " + bone.getName());
        }
    }

    /**
     * 确定按钮点击
     */
    private void onOkClicked() {
        List<EditorBone> selectedBones = getSelectedBones();

        if (resultListener != null) {
            resultListener.onOk(selectedBones);
        }

        hide();
    }

    /**
     * 取消按钮点击
     */
    private void onCancelClicked() {
        if (resultListener != null) {
            resultListener.onCancel();
        }

        hide();
    }

    /**
     * 获取选中的骨骼列表
     */
    public List<EditorBone> getSelectedBones() {
        List<EditorBone> selected = new ArrayList<>();

        for (Map.Entry<EditorBone, Boolean> entry : selectionStates.entrySet()) {
            if (entry.getValue()) {
                selected.add(entry.getKey());
            }
        }

        return selected;
    }

    /**
     * 显示对话框
     */
    public void show() {
        if (!isVisible) {
            app.getGuiNode().attachChild(rootNode);
            isVisible = true;
        }
    }

    /**
     * 隐藏对话框
     */
    public void hide() {
        if (isVisible) {
            app.getGuiNode().detachChild(rootNode);
            isVisible = false;
        }
    }

    /**
     * 设置结果监听器
     */
    public void setResultListener(DialogResultListener listener) {
        this.resultListener = listener;
    }

    /**
     * 获取根节点
     */
    public Node getRootNode() {
        return rootNode;
    }

    /**
     * 是否可见
     */
    public boolean isVisible() {
        return isVisible;
    }

    /**
     * 对话框结果回调接口
     */
    public interface DialogResultListener {
        void onOk(List<EditorBone> selectedBones);
        void onCancel();
    }
}
