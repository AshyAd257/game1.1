package com.Hecate.puppet.editor;

import com.jme3.app.SimpleApplication;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Node;
import com.Hecate.puppet.editor.core.EditorBone;
import com.Hecate.puppet.editor.core.EditorBoneGroup;
import com.Hecate.puppet.editor.core.EditorGroupManager;
import com.Hecate.puppet.editor.core.EditorSkeleton;
import java.util.ArrayList;
import java.util.List;

/**
 * 骨骼分组控制面板
 * 提供分组管理界面：创建组、删除组、添加/移除成员、旋转组
 */
public class GroupControlPanel {

    private final SimpleApplication app;
    private final BitmapFont guiFont;
    private final TTFontLoader ttfLoader;
    private final Node rootNode;

    // 布局参数
    private final int x;
    private final int y;
    private final int width;
    private final int height;

    // UI组件
    private Button createGroupButton;
    private Button deleteGroupButton;
    private Button addToGroupButton;
    private Button removeFromGroupButton;
    private Button rotateLeft90Button;
    private Button rotateRight90Button;
    private Button rotate180Button;

    // 组整体XYZ位移微调按钮（用于PartListPanel拖拽分组功能）
    private Button moveXMinusButton, moveXPlusButton;
    private Button moveYMinusButton, moveYPlusButton;
    private Button moveZMinusButton, moveZPlusButton;
    private static final float MOVE_STEP = 0.5f;

    private TextField groupNameField;
    private BitmapText groupListText;
    private BitmapText currentGroupText;

    // 数据
    private EditorGroupManager groupManager;
    private EditorSkeleton skeleton;
    private String selectedGroupId;  // 当前选中的组ID
    private EditorBone selectedBone;  // 当前选中的骨骼

    // 骨骼选择对话框
    private BoneSelectionDialog boneSelectionDialog;

    // 回调接口
    private GroupActionListener actionListener;

    /**
     * 创建分组控制面板
     */
    public GroupControlPanel(SimpleApplication app, BitmapFont font, TTFontLoader ttfLoader,
                           int x, int y, int width, int height) {
        this.app = app;
        this.guiFont = font;
        this.ttfLoader = ttfLoader;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.rootNode = new Node("GroupControlPanel");

        initializeUI();
        initializeBoneSelectionDialog();
    }

    /**
     * 初始化UI
     */
    private void initializeUI() {
        // 标题
        BitmapText titleText = new BitmapText(guiFont);
        titleText.setText("骨骼分组 (Bone Groups)");
        titleText.setColor(ColorRGBA.White);
        titleText.setLocalTranslation(x + 10, y + height - 10, 0);
        rootNode.attachChild(titleText);

        // 组名输入框（如果有TTF字体加载器）
        if (ttfLoader != null) {
            groupNameField = new TextField(app, ttfLoader, "", x + 10, y + height - 45, width - 20, 30);
            rootNode.attachChild(groupNameField.getRootNode());
        }

        // 创建组按钮
        int buttonWidth = (width - 30) / 2;
        if (ttfLoader != null) {
            createGroupButton = new Button(app, ttfLoader,
                "创建组 (Create)", x + 10, y + height - 85, buttonWidth, 35);
        } else {
            createGroupButton = new Button(app, guiFont,
                "创建组 (Create)", x + 10, y + height - 85, buttonWidth, 35);
        }
        createGroupButton.setClickListener(() -> createGroup());
        rootNode.attachChild(createGroupButton.getRootNode());

        // 删除组按钮
        if (ttfLoader != null) {
            deleteGroupButton = new Button(app, ttfLoader,
                "删除组 (Delete)", x + 15 + buttonWidth, y + height - 85, buttonWidth, 35);
        } else {
            deleteGroupButton = new Button(app, guiFont,
                "删除组 (Delete)", x + 15 + buttonWidth, y + height - 85, buttonWidth, 35);
        }
        deleteGroupButton.setClickListener(() -> deleteGroup());
        rootNode.attachChild(deleteGroupButton.getRootNode());

        // 当前选中组显示
        currentGroupText = new BitmapText(guiFont);
        currentGroupText.setText("当前组: 无");
        currentGroupText.setColor(ColorRGBA.Yellow);
        currentGroupText.setLocalTranslation(x + 10, y + height - 95, 0);
        rootNode.attachChild(currentGroupText);

        // 添加到组按钮
        if (ttfLoader != null) {
            addToGroupButton = new Button(app, ttfLoader,
                "添加骨骼 (Add)", x + 10, y + height - 135, buttonWidth, 35);
        } else {
            addToGroupButton = new Button(app, guiFont,
                "添加骨骼 (Add)", x + 10, y + height - 135, buttonWidth, 35);
        }
        addToGroupButton.setClickListener(() -> addBoneToGroup());
        rootNode.attachChild(addToGroupButton.getRootNode());

        // 从组移除按钮
        if (ttfLoader != null) {
            removeFromGroupButton = new Button(app, ttfLoader,
                "移除骨骼 (Remove)", x + 15 + buttonWidth, y + height - 135, buttonWidth, 35);
        } else {
            removeFromGroupButton = new Button(app, guiFont,
                "移除骨骼 (Remove)", x + 15 + buttonWidth, y + height - 135, buttonWidth, 35);
        }
        removeFromGroupButton.setClickListener(() -> removeBoneFromGroup());
        rootNode.attachChild(removeFromGroupButton.getRootNode());

        // 旋转控制按钮 - 分隔线
        BitmapText rotateLabel = new BitmapText(guiFont);
        rotateLabel.setText("旋转控制 (Rotation)");
        rotateLabel.setColor(ColorRGBA.White);
        rotateLabel.setLocalTranslation(x + 10, y + height - 150, 0);
        rootNode.attachChild(rotateLabel);

        // 左转90度按钮
        int rotateButtonWidth = (width - 40) / 3;
        if (ttfLoader != null) {
            rotateLeft90Button = new Button(app, ttfLoader,
                "← 90°", x + 10, y + height - 185, rotateButtonWidth, 35);
        } else {
            rotateLeft90Button = new Button(app, guiFont,
                "← 90°", x + 10, y + height - 185, rotateButtonWidth, 35);
        }
        rotateLeft90Button.setClickListener(() -> rotateGroupLeft90());
        rootNode.attachChild(rotateLeft90Button.getRootNode());

        // 转身180度按钮
        if (ttfLoader != null) {
            rotate180Button = new Button(app, ttfLoader,
                "↑ 180°", x + 15 + rotateButtonWidth, y + height - 185, rotateButtonWidth, 35);
        } else {
            rotate180Button = new Button(app, guiFont,
                "↑ 180°", x + 15 + rotateButtonWidth, y + height - 185, rotateButtonWidth, 35);
        }
        rotate180Button.setClickListener(() -> rotateGroup180());
        rootNode.attachChild(rotate180Button.getRootNode());

        // 右转90度按钮
        if (ttfLoader != null) {
            rotateRight90Button = new Button(app, ttfLoader,
                "→ 90°", x + 20 + rotateButtonWidth * 2, y + height - 185, rotateButtonWidth, 35);
        } else {
            rotateRight90Button = new Button(app, guiFont,
                "→ 90°", x + 20 + rotateButtonWidth * 2, y + height - 185, rotateButtonWidth, 35);
        }
        rotateRight90Button.setClickListener(() -> rotateGroupRight90());
        rootNode.attachChild(rotateRight90Button.getRootNode());

        // 整体移动控制 - 分隔线
        BitmapText moveLabel = new BitmapText(guiFont);
        moveLabel.setText("整体移动 (Move All)");
        moveLabel.setColor(ColorRGBA.White);
        moveLabel.setLocalTranslation(x + 10, y + height - 220, 0);
        rootNode.attachChild(moveLabel);

        // X/Y/Z 三组 -/+ 按钮，一行放两个（负/正）
        int moveButtonWidth = (width - 40) / 3;
        int moveButtonY = y + height - 255;

        if (ttfLoader != null) {
            moveXMinusButton = new Button(app, ttfLoader, "X-", x + 10, moveButtonY, moveButtonWidth / 2 - 2, 30);
            moveXPlusButton = new Button(app, ttfLoader, "X+", x + 10 + moveButtonWidth / 2, moveButtonY, moveButtonWidth / 2 - 2, 30);
            moveYMinusButton = new Button(app, ttfLoader, "Y-", x + 15 + moveButtonWidth, moveButtonY, moveButtonWidth / 2 - 2, 30);
            moveYPlusButton = new Button(app, ttfLoader, "Y+", x + 15 + moveButtonWidth + moveButtonWidth / 2, moveButtonY, moveButtonWidth / 2 - 2, 30);
            moveZMinusButton = new Button(app, ttfLoader, "Z-", x + 20 + moveButtonWidth * 2, moveButtonY, moveButtonWidth / 2 - 2, 30);
            moveZPlusButton = new Button(app, ttfLoader, "Z+", x + 20 + moveButtonWidth * 2 + moveButtonWidth / 2, moveButtonY, moveButtonWidth / 2 - 2, 30);
        } else {
            moveXMinusButton = new Button(app, guiFont, "X-", x + 10, moveButtonY, moveButtonWidth / 2 - 2, 30);
            moveXPlusButton = new Button(app, guiFont, "X+", x + 10 + moveButtonWidth / 2, moveButtonY, moveButtonWidth / 2 - 2, 30);
            moveYMinusButton = new Button(app, guiFont, "Y-", x + 15 + moveButtonWidth, moveButtonY, moveButtonWidth / 2 - 2, 30);
            moveYPlusButton = new Button(app, guiFont, "Y+", x + 15 + moveButtonWidth + moveButtonWidth / 2, moveButtonY, moveButtonWidth / 2 - 2, 30);
            moveZMinusButton = new Button(app, guiFont, "Z-", x + 20 + moveButtonWidth * 2, moveButtonY, moveButtonWidth / 2 - 2, 30);
            moveZPlusButton = new Button(app, guiFont, "Z+", x + 20 + moveButtonWidth * 2 + moveButtonWidth / 2, moveButtonY, moveButtonWidth / 2 - 2, 30);
        }
        moveXMinusButton.setClickListener(() -> moveSelectedGroup(-MOVE_STEP, 0, 0));
        moveXPlusButton.setClickListener(() -> moveSelectedGroup(MOVE_STEP, 0, 0));
        moveYMinusButton.setClickListener(() -> moveSelectedGroup(0, -MOVE_STEP, 0));
        moveYPlusButton.setClickListener(() -> moveSelectedGroup(0, MOVE_STEP, 0));
        moveZMinusButton.setClickListener(() -> moveSelectedGroup(0, 0, -MOVE_STEP));
        moveZPlusButton.setClickListener(() -> moveSelectedGroup(0, 0, MOVE_STEP));
        rootNode.attachChild(moveXMinusButton.getRootNode());
        rootNode.attachChild(moveXPlusButton.getRootNode());
        rootNode.attachChild(moveYMinusButton.getRootNode());
        rootNode.attachChild(moveYPlusButton.getRootNode());
        rootNode.attachChild(moveZMinusButton.getRootNode());
        rootNode.attachChild(moveZPlusButton.getRootNode());

        // 组列表显示
        groupListText = new BitmapText(guiFont);
        groupListText.setText("组列表:\n(暂无)");
        groupListText.setColor(ColorRGBA.LightGray);
        groupListText.setLocalTranslation(x + 10, y + height - 285, 0);
        rootNode.attachChild(groupListText);
    }

    /**
     * 整体移动当前选中的组（每次点击移动MOVE_STEP个单位）
     */
    private void moveSelectedGroup(float dx, float dy, float dz) {
        if (groupManager == null || selectedGroupId == null) {
            showMessage("请先选择一个组");
            return;
        }

        EditorBoneGroup group = groupManager.getGroup(selectedGroupId);
        if (group == null) {
            showMessage("组不存在");
            return;
        }

        group.translateAll(dx, dy, dz);

        if (actionListener != null) {
            actionListener.onGroupMoved(group, dx, dy, dz);
        }
    }

    /**
     * 初始化骨骼选择对话框
     */
    private void initializeBoneSelectionDialog() {
        // 对话框居中显示，占据屏幕的60%
        int screenWidth = app.getCamera().getWidth();
        int screenHeight = app.getCamera().getHeight();
        int dialogWidth = (int)(screenWidth * 0.6);
        int dialogHeight = (int)(screenHeight * 0.7);
        int dialogX = (screenWidth - dialogWidth) / 2;
        int dialogY = (screenHeight - dialogHeight) / 2;

        boneSelectionDialog = new BoneSelectionDialog(
            app, guiFont, ttfLoader,
            dialogX, dialogY, dialogWidth, dialogHeight
        );

        // 设置对话框结果监听器
        boneSelectionDialog.setResultListener(new BoneSelectionDialog.DialogResultListener() {
            @Override
            public void onOk(List<EditorBone> selectedBones) {
                createGroupWithBones(selectedBones);
            }

            @Override
            public void onCancel() {
                showMessage("取消创建组");
            }
        });
    }

    /**
     * 创建新组（使用对话框选择骨骼）
     */
    private void createGroup() {
        if (groupManager == null) {
            showMessage("错误: GroupManager未初始化");
            return;
        }

        if (skeleton == null) {
            showMessage("错误: Skeleton未初始化");
            return;
        }

        // 设置骨骼系统到对话框
        boneSelectionDialog.setSkeleton(skeleton);

        // 显示对话框
        boneSelectionDialog.show();
    }

    /**
     * 使用选中的骨骼创建组
     */
    private void createGroupWithBones(List<EditorBone> selectedBones) {
        if (selectedBones.isEmpty()) {
            showMessage("请至少选择一个骨骼");
            return;
        }

        // 获取组名
        String groupName = "";
        if (groupNameField != null) {
            groupName = groupNameField.getText().trim();
        }

        if (groupName.isEmpty()) {
            // 如果没有输入，使用默认名称
            groupName = "Group_" + (groupManager.getGroupCount() + 1);
        }

        // 创建组
        EditorBoneGroup group = groupManager.createGroup(groupName);
        if (group == null) {
            showMessage("组名已存在: " + groupName);
            return;
        }

        // 获取组ID
        String groupId = groupManager.getGroupId(group);
        if (groupId == null) {
            showMessage("错误: 无法获取组ID");
            return;
        }

        // 检测哪些骨骼已属于其他组（将被移动）
        List<String> movedBones = new ArrayList<>();
        for (EditorBone bone : selectedBones) {
            if (bone.hasGroup()) {
                EditorBoneGroup oldGroup = groupManager.getGroupOf(bone);
                if (oldGroup != null) {
                    movedBones.add(bone.getName() + " (从 " + oldGroup.getName() + ")");
                }
            }
        }

        // 批量添加骨骼到组
        int addedCount = groupManager.addBonesToGroup(groupId, selectedBones);

        // 清空输入框
        if (groupNameField != null) {
            groupNameField.setText("");
        }

        updateGroupList();

        // 显示详细信息（包括被移动的骨骼）
        if (!movedBones.isEmpty()) {
            StringBuilder message = new StringBuilder();
            message.append(String.format("创建组成功: %s，已添加 %d 个骨骼\n", groupName, addedCount));
            message.append("以下骨骼已从旧组移除：\n");
            for (String boneName : movedBones) {
                message.append("  - " + boneName + "\n");
            }
            showMessage(message.toString());
        } else {
            showMessage(String.format("创建组成功: %s，已添加 %d 个骨骼", groupName, addedCount));
        }

        if (actionListener != null) {
            actionListener.onGroupCreated(group);
        }
    }

    /**
     * 删除选中的组
     */
    private void deleteGroup() {
        if (groupManager == null || selectedGroupId == null) {
            showMessage("请先选择一个组");
            return;
        }

        EditorBoneGroup group = groupManager.getGroup(selectedGroupId);
        if (group == null) {
            showMessage("组不存在");
            return;
        }

        String groupName = group.getName();
        if (groupManager.deleteGroup(selectedGroupId)) {
            selectedGroupId = null;
            updateGroupList();
            updateCurrentGroupDisplay();
            showMessage("删除组成功: " + groupName);

            if (actionListener != null) {
                actionListener.onGroupDeleted(selectedGroupId);
            }
        } else {
            showMessage("删除组失败");
        }
    }

    /**
     * 将当前选中的骨骼添加到选中的组
     */
    private void addBoneToGroup() {
        if (groupManager == null || selectedGroupId == null) {
            showMessage("请先选择一个组");
            return;
        }

        if (selectedBone == null) {
            showMessage("请先选择一个骨骼");
            return;
        }

        // 检测骨骼是否已属于其他组
        String oldGroupInfo = "";
        if (selectedBone.hasGroup()) {
            EditorBoneGroup oldGroup = groupManager.getGroupOf(selectedBone);
            if (oldGroup != null) {
                oldGroupInfo = " (已从 " + oldGroup.getName() + " 移除)";
            }
        }

        if (groupManager.addBoneToGroup(selectedGroupId, selectedBone)) {
            EditorBoneGroup group = groupManager.getGroup(selectedGroupId);
            showMessage("已添加 " + selectedBone.getName() + " 到组 " + group.getName() + oldGroupInfo);

            if (actionListener != null) {
                actionListener.onBoneAddedToGroup(selectedBone, group);
            }
        } else {
            showMessage("添加失败");
        }
    }

    /**
     * 从组中移除当前选中的骨骼
     */
    private void removeBoneFromGroup() {
        if (groupManager == null || selectedBone == null) {
            showMessage("请先选择一个骨骼");
            return;
        }

        if (!selectedBone.hasGroup()) {
            showMessage("骨骼不属于任何组");
            return;
        }

        EditorBoneGroup group = groupManager.getGroupOf(selectedBone);
        String groupName = (group != null) ? group.getName() : "未知";

        if (groupManager.removeBoneFromGroup(selectedBone)) {
            showMessage("已从组 " + groupName + " 移除 " + selectedBone.getName());

            if (actionListener != null) {
                actionListener.onBoneRemovedFromGroup(selectedBone, group);
            }
        } else {
            showMessage("移除失败");
        }
    }

    /**
     * 左转90度
     */
    private void rotateGroupLeft90() {
        if (groupManager == null || selectedGroupId == null) {
            showMessage("请先选择一个组");
            return;
        }

        EditorBoneGroup group = groupManager.getGroup(selectedGroupId);
        if (group == null) {
            showMessage("组不存在");
            return;
        }

        group.rotateLeft90();
        showMessage("组 " + group.getName() + " 已左转90°");

        if (actionListener != null) {
            actionListener.onGroupRotated(group, -90);
        }
    }

    /**
     * 右转90度
     */
    private void rotateGroupRight90() {
        if (groupManager == null || selectedGroupId == null) {
            showMessage("请先选择一个组");
            return;
        }

        EditorBoneGroup group = groupManager.getGroup(selectedGroupId);
        if (group == null) {
            showMessage("组不存在");
            return;
        }

        group.rotateRight90();
        showMessage("组 " + group.getName() + " 已右转90°");

        if (actionListener != null) {
            actionListener.onGroupRotated(group, 90);
        }
    }

    /**
     * 转身180度
     */
    private void rotateGroup180() {
        if (groupManager == null || selectedGroupId == null) {
            showMessage("请先选择一个组");
            return;
        }

        EditorBoneGroup group = groupManager.getGroup(selectedGroupId);
        if (group == null) {
            showMessage("组不存在");
            return;
        }

        group.rotate180();
        showMessage("组 " + group.getName() + " 已转身180°");

        if (actionListener != null) {
            actionListener.onGroupRotated(group, 180);
        }
    }

    /**
     * 更新组列表显示
     */
    private void updateGroupList() {
        if (groupManager == null) {
            groupListText.setText("组列表:\n(未初始化)");
            return;
        }

        List<EditorBoneGroup> groups = groupManager.getAllGroups();
        if (groups.isEmpty()) {
            groupListText.setText("组列表:\n(暂无)");
            return;
        }

        StringBuilder sb = new StringBuilder("组列表:\n");
        for (EditorBoneGroup group : groups) {
            sb.append("• ").append(group.getName())
              .append(" (").append(group.getMemberCount()).append("骨骼)\n");
        }
        groupListText.setText(sb.toString());
    }

    /**
     * 更新当前组显示
     */
    private void updateCurrentGroupDisplay() {
        if (selectedGroupId == null) {
            currentGroupText.setText("当前组: 无");
            return;
        }

        EditorBoneGroup group = groupManager.getGroup(selectedGroupId);
        if (group == null) {
            currentGroupText.setText("当前组: 无");
            selectedGroupId = null;
            return;
        }

        currentGroupText.setText("当前组: " + group.getName() +
            " (" + group.getCurrentRotation() + "°)");
    }

    /**
     * 显示消息（临时实现，可以改为更美观的提示）
     */
    private void showMessage(String message) {
        System.out.println("[GroupControlPanel] " + message);
        // TODO: 在UI上显示提示消息
    }

    /**
     * 设置GroupManager
     */
    public void setGroupManager(EditorGroupManager groupManager, EditorSkeleton skeleton) {
        this.groupManager = groupManager;
        this.skeleton = skeleton;
        updateGroupList();
        updateCurrentGroupDisplay();
    }

    /**
     * 设置Skeleton（便捷方法）
     */
    public void setSkeleton(EditorSkeleton skeleton) {
        this.skeleton = skeleton;
        if (skeleton != null) {
            this.groupManager = skeleton.getGroupManager();
            updateGroupList();
            updateCurrentGroupDisplay();
        }
    }

    /**
     * 设置当前选中的骨骼
     */
    public void setSelectedBone(EditorBone bone) {
        this.selectedBone = bone;

        // 如果骨骼属于某个组，自动选中该组
        if (bone != null && bone.hasGroup()) {
            this.selectedGroupId = bone.getGroupId();
            updateCurrentGroupDisplay();
        }
    }

    /**
     * 设置当前选中的组
     */
    public void setSelectedGroup(String groupId) {
        this.selectedGroupId = groupId;
        updateCurrentGroupDisplay();
    }

    /**
     * 设置动作监听器
     */
    public void setActionListener(GroupActionListener listener) {
        this.actionListener = listener;
    }

    /**
     * 获取根节点
     */
    public Node getRootNode() {
        return rootNode;
    }

    /**
     * 显示/隐藏面板
     */
    public void setVisible(boolean visible) {
        if (visible) {
            if (!app.getGuiNode().hasChild(rootNode)) {
                app.getGuiNode().attachChild(rootNode);
            }
        } else {
            app.getGuiNode().detachChild(rootNode);
        }
    }

    /**
     * 更新面板（每帧调用）
     */
    public void update(float tpf) {
        // 更新UI组件
        if (groupNameField != null) {
            groupNameField.update(tpf);
        }
        if (moveXMinusButton != null) {
            moveXMinusButton.update(tpf);
            moveXPlusButton.update(tpf);
            moveYMinusButton.update(tpf);
            moveYPlusButton.update(tpf);
            moveZMinusButton.update(tpf);
            moveZPlusButton.update(tpf);
        }
    }

    /**
     * 处理鼠标点击（所有按钮）
     * @return true如果点击命中了本面板的某个按钮
     */
    public boolean handleMouseClick(int mouseX, int mouseY) {
        if (createGroupButton.handleMouseClick(mouseX, mouseY)) return true;
        if (deleteGroupButton.handleMouseClick(mouseX, mouseY)) return true;
        if (addToGroupButton.handleMouseClick(mouseX, mouseY)) return true;
        if (removeFromGroupButton.handleMouseClick(mouseX, mouseY)) return true;
        if (rotateLeft90Button.handleMouseClick(mouseX, mouseY)) return true;
        if (rotateRight90Button.handleMouseClick(mouseX, mouseY)) return true;
        if (rotate180Button.handleMouseClick(mouseX, mouseY)) return true;
        if (moveXMinusButton.handleMouseClick(mouseX, mouseY)) return true;
        if (moveXPlusButton.handleMouseClick(mouseX, mouseY)) return true;
        if (moveYMinusButton.handleMouseClick(mouseX, mouseY)) return true;
        if (moveYPlusButton.handleMouseClick(mouseX, mouseY)) return true;
        if (moveZMinusButton.handleMouseClick(mouseX, mouseY)) return true;
        if (moveZPlusButton.handleMouseClick(mouseX, mouseY)) return true;
        return false;
    }

    /**
     * 组操作回调接口
     */
    public interface GroupActionListener {
        void onGroupCreated(EditorBoneGroup group);
        void onGroupDeleted(String groupId);
        void onBoneAddedToGroup(EditorBone bone, EditorBoneGroup group);
        void onBoneRemovedFromGroup(EditorBone bone, EditorBoneGroup group);
        void onGroupRotated(EditorBoneGroup group, int degrees);
        void onGroupMoved(EditorBoneGroup group, float dx, float dy, float dz);
    }
}
