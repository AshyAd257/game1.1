package com.Hecate.puppet.editor.lemur;

import com.jme3.app.SimpleApplication;
import com.jme3.math.Vector3f;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.style.BaseStyles;
import com.Hecate.puppet.core.Bone;
import com.Hecate.puppet.core.Skeleton;
import com.Hecate.puppet.core.PuppetPartRenderer;

import java.util.List;

/**
 * Lemur UI 管理器
 * 负责初始化和管理所有 Lemur UI 面板
 * 作为旧 UI 系统和新 Lemur UI 之间的适配层
 */
public class LemurUIManager {

    private final SimpleApplication app;
    private boolean initialized = false;

    // UI 面板
    private LemurSliderPanel sliderPanel;
    private LemurButtonPanel buttonPanel;
    private LemurPartListPanel partListPanel;

    // 回调接口 - 连接到 PuppetEditorApp
    public interface EditorUICallbacks {
        // 滑条回调
        void onWidthChanged(float value);
        void onHeightChanged(float value);
        void onPriorityChanged(float value);
        void onPosXChanged(float value);
        void onPosYChanged(float value);
        void onPosZChanged(float value);
        void onRotXChanged(float value);
        void onRotZChanged(float value);
        void onGridSizeChanged(float value);

        // 按钮回调
        void onNewFile();
        void onOpenFile();
        void onSaveFile();
        void onSaveAsFile();
        void onExportFile();
        void onUndo();
        void onRedo();
        void onCopy();
        void onPaste();
        void onDelete();
        void onAddBone();
        void onRemoveBone();
        void onSetParent();
        void onClearParent();
        void onViewFront();
        void onViewBack();
        void onViewLeft();
        void onViewRight();
        void onHideMode();
        void onShowAll();
        void onPlay();
        void onStop();
        void onAddKeyframe();

        // 部件选择回调
        void onPartSelected(int boneIndex, String boneName);
    }

    private EditorUICallbacks callbacks;

    public LemurUIManager(SimpleApplication app) {
        this.app = app;
    }

    /**
     * 初始化 Lemur GUI 系统
     * 必须在 simpleInitApp() 中调用
     */
    public void initialize() {
        if (initialized) {
            return;
        }

        // 初始化 Lemur
        GuiGlobals.initialize(app);

        // 尝试加载 glass 样式（需要 Groovy），如果失败则使用默认样式
        try {
            BaseStyles.loadGlassStyle();
            GuiGlobals.getInstance().getStyles().setDefaultStyle("glass");
        } catch (Exception e) {
            // 使用默认样式，不设置特定样式
        }

        initialized = true;
    }

    /**
     * 创建所有 UI 面板
     */
    public void createPanels() {
        if (!initialized) {
            System.err.println("[LemurUIManager] Must call initialize() first!");
            return;
        }

        int screenWidth = app.getCamera().getWidth();
        int screenHeight = app.getCamera().getHeight();

        // 计算布局
        int topBarHeight = 70;
        int timelineHeight = 120;
        int mainAreaHeight = screenHeight - topBarHeight - timelineHeight;
        int mainAreaY = timelineHeight;  // 从底部算起

        int leftPanelWidth = 260;
        int rightPanelWidth = 290;
        int rightX = screenWidth - rightPanelWidth - 10;

        // 左侧按钮面板
        buttonPanel = new LemurButtonPanel(app, 10, mainAreaY, leftPanelWidth, mainAreaHeight);
        buttonPanel.setCallback(this::handleButtonClick);

        // 右侧上部：部件列表（占60%）
        int partListHeight = (int) (mainAreaHeight * 0.55);
        int partListY = mainAreaY + mainAreaHeight - partListHeight;
        partListPanel = new LemurPartListPanel(app, rightX, mainAreaY + (int)(mainAreaHeight * 0.45), rightPanelWidth, partListHeight);
        partListPanel.setCallback((boneIndex, boneName) -> {
            if (callbacks != null) {
                callbacks.onPartSelected(boneIndex, boneName);
            }
        });

        // 右侧下部：属性滑条（占45%）
        int sliderHeight = (int) (mainAreaHeight * 0.45);
        sliderPanel = new LemurSliderPanel(app, rightX, mainAreaY, rightPanelWidth, sliderHeight);
        sliderPanel.setCallback(this::handleSliderChange);

    }

    /**
     * 处理按钮点击
     */
    private void handleButtonClick(String buttonId) {
        if (callbacks == null) return;

        switch (buttonId) {
            case "new" -> callbacks.onNewFile();
            case "open" -> callbacks.onOpenFile();
            case "save" -> callbacks.onSaveFile();
            case "saveAs" -> callbacks.onSaveAsFile();
            case "export" -> callbacks.onExportFile();
            case "undo" -> callbacks.onUndo();
            case "redo" -> callbacks.onRedo();
            case "copy" -> callbacks.onCopy();
            case "paste" -> callbacks.onPaste();
            case "delete" -> callbacks.onDelete();
            case "addBone" -> callbacks.onAddBone();
            case "removeBone" -> callbacks.onRemoveBone();
            case "setParent" -> callbacks.onSetParent();
            case "clearParent" -> callbacks.onClearParent();
            case "front" -> callbacks.onViewFront();
            case "back" -> callbacks.onViewBack();
            case "left" -> callbacks.onViewLeft();
            case "right" -> callbacks.onViewRight();
            case "hideMode" -> callbacks.onHideMode();
            case "showAll" -> callbacks.onShowAll();
            case "play" -> callbacks.onPlay();
            case "stop" -> callbacks.onStop();
            case "addKey" -> callbacks.onAddKeyframe();
        }
    }

    /**
     * 处理滑条变化
     */
    private void handleSliderChange(String sliderId, float value) {
        if (callbacks == null) return;

        switch (sliderId) {
            case "width" -> callbacks.onWidthChanged(value);
            case "height" -> callbacks.onHeightChanged(value);
            case "priority" -> callbacks.onPriorityChanged(value);
            case "posX" -> callbacks.onPosXChanged(value);
            case "posY" -> callbacks.onPosYChanged(value);
            case "posZ" -> callbacks.onPosZChanged(value);
            case "rotX" -> callbacks.onRotXChanged(value);
            case "rotZ" -> callbacks.onRotZChanged(value);
            case "gridSize" -> callbacks.onGridSizeChanged(value);
        }
    }

    /**
     * 设置回调
     */
    public void setCallbacks(EditorUICallbacks callbacks) {
        this.callbacks = callbacks;
    }

    /**
     * 更新骨骼列表
     */
    public void updateBoneList(Skeleton skeleton) {
        if (partListPanel == null || skeleton == null) return;

        partListPanel.clearParts();

        // 遍历骨骼，添加到列表
        List<Bone> bones = skeleton.getAllBones();
        for (int i = 0; i < bones.size(); i++) {
            Bone bone = bones.get(i);
            int depth = calculateBoneDepth(bone);
            partListPanel.addPart(bone.getName(), i, true, depth);
        }
    }

    /**
     * 计算骨骼深度（用于缩进显示）
     */
    private int calculateBoneDepth(Bone bone) {
        int depth = 0;
        Bone parent = bone.getParent();
        while (parent != null) {
            depth++;
            parent = parent.getParent();
        }
        return depth;
    }

    /**
     * 设置选中的骨骼
     */
    public void setSelectedBone(int boneIndex) {
        if (partListPanel != null) {
            partListPanel.setSelectedByBoneIndex(boneIndex);
        }
    }

    /**
     * 更新滑条值（从选中的部件渲染器）
     */
    public void updateSliderValues(PuppetPartRenderer partRenderer) {
        if (sliderPanel == null || partRenderer == null) return;

        // 更新尺寸
        sliderPanel.setValue("width", partRenderer.getWidth());
        sliderPanel.setValue("height", partRenderer.getHeight());

        // 更新位置
        Vector3f offset = partRenderer.getOffset();
        sliderPanel.setValue("posX", offset.x);
        sliderPanel.setValue("posY", offset.y);
        sliderPanel.setValue("posZ", offset.z);

        // 更新旋转
        sliderPanel.setValue("rotX", partRenderer.getCustomRotationX());
        sliderPanel.setValue("rotZ", partRenderer.getCustomRotationZ());

    }

    /**
     * 更新滑条值（同时使用 PartRenderer 和 Bone）
     */
    public void updateSliderValues(PuppetPartRenderer partRenderer, Bone bone) {
        if (sliderPanel == null) return;

        if (partRenderer != null) {
            // 更新尺寸
            sliderPanel.setValue("width", partRenderer.getWidth());
            sliderPanel.setValue("height", partRenderer.getHeight());

            // 更新位置
            Vector3f offset = partRenderer.getOffset();
            sliderPanel.setValue("posX", offset.x);
            sliderPanel.setValue("posY", offset.y);
            sliderPanel.setValue("posZ", offset.z);

            // 更新旋转
            sliderPanel.setValue("rotX", partRenderer.getCustomRotationX());
            sliderPanel.setValue("rotZ", partRenderer.getCustomRotationZ());
        }

        if (bone != null) {
            // 优先级从Bone获取
            sliderPanel.setValue("priority", bone.getPriority());
        }
    }

    /**
     * 更新循环
     */
    public void update(float tpf) {
        if (sliderPanel != null) {
            sliderPanel.update(tpf);
        }
        if (partListPanel != null) {
            partListPanel.update(tpf);
        }
    }

    /**
     * 设置面板可见性
     */
    public void setVisible(boolean visible) {
        if (buttonPanel != null) buttonPanel.setVisible(visible);
        if (sliderPanel != null) sliderPanel.setVisible(visible);
        if (partListPanel != null) partListPanel.setVisible(visible);
    }

    /**
     * 清理资源
     */
    public void cleanup() {
        if (buttonPanel != null) buttonPanel.cleanup();
        if (sliderPanel != null) sliderPanel.cleanup();
        if (partListPanel != null) partListPanel.cleanup();
    }

    /**
     * 获取面板引用
     */
    public LemurSliderPanel getSliderPanel() { return sliderPanel; }
    public LemurButtonPanel getButtonPanel() { return buttonPanel; }
    public LemurPartListPanel getPartListPanel() { return partListPanel; }

    public boolean isInitialized() { return initialized; }
}
