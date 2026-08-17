package com.Hecate.puppet.editor.lemur;

import com.jme3.app.SimpleApplication;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.simsilica.lemur.*;
import com.simsilica.lemur.component.SpringGridLayout;
import com.simsilica.lemur.core.VersionedReference;
import com.simsilica.lemur.style.ElementId;

import java.util.HashMap;
import java.util.Map;

/**
 * 使用 Lemur GUI 重写的滑条面板
 * 替代原有的 SliderColumnPanel
 */
public class LemurSliderPanel {

    private final SimpleApplication app;
    private final Container mainContainer;
    private final Node guiNode;

    // 滑条映射
    private final Map<String, Slider> sliders = new HashMap<>();
    private final Map<String, Label> valueLabels = new HashMap<>();
    private final Map<String, VersionedReference<Double>> sliderRefs = new HashMap<>();

    // 回调接口
    public interface SliderChangeCallback {
        void onValueChanged(String sliderId, float value);
    }
    private SliderChangeCallback callback;

    // 面板位置和大小
    private int panelX, panelY;
    private final int panelWidth;
    private final int panelHeight;

    public LemurSliderPanel(SimpleApplication app, int x, int y, int width, int height) {
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
        Label titleLabel = new Label("Properties Panel");
        titleLabel.setFontSize(18);
        titleLabel.setColor(ColorRGBA.Yellow);
        mainContainer.addChild(titleLabel);

        // 分隔线
        mainContainer.addChild(createSeparator());

        // === Transform 组 ===
        mainContainer.addChild(createGroupLabel("Transform"));

        // Position 滑条
        createSliderRow("posX", "Pos X", -50, 50, 0);
        createSliderRow("posY", "Pos Y", -50, 50, 0);
        createSliderRow("posZ", "Pos Z", -50, 50, 0);

        // Rotation 滑条
        createSliderRow("rotX", "Rot X", -180, 180, 0);
        createSliderRow("rotZ", "Rot Z", -180, 180, 0);

        mainContainer.addChild(createSeparator());

        // === Size 组 ===
        mainContainer.addChild(createGroupLabel("Size"));

        createSliderRow("width", "Width", 0.01f, 1000, 100);
        createSliderRow("height", "Height", 0.01f, 1000, 100);

        mainContainer.addChild(createSeparator());

        // === Render 组 ===
        mainContainer.addChild(createGroupLabel("Render"));

        createSliderRow("priority", "Priority", -100, 100, 0);
        createSliderRow("gridSize", "Grid Size", 1, 100, 10);
    }

    /**
     * 创建滑条行（标签 + 滑条 + 数值显示）
     */
    private void createSliderRow(String id, String label, float min, float max, float defaultValue) {
        Container rowContainer = new Container(new SpringGridLayout(Axis.X, Axis.Y, FillMode.Last, FillMode.None));
        rowContainer.setPreferredSize(new Vector3f(panelWidth - 20, 30, 0));

        // 标签
        Label nameLabel = new Label(label);
        nameLabel.setPreferredSize(new Vector3f(70, 25, 0));
        nameLabel.setFontSize(14);
        rowContainer.addChild(nameLabel);

        // 滑条
        Slider slider = new Slider(Axis.X);
        slider.setPreferredSize(new Vector3f(120, 20, 0));

        // 设置范围
        DefaultRangedValueModel model = new DefaultRangedValueModel(min, max, defaultValue);
        slider.setModel(model);

        rowContainer.addChild(slider);

        // 数值标签
        Label valueLabel = new Label(String.format("%.1f", defaultValue));
        valueLabel.setPreferredSize(new Vector3f(50, 25, 0));
        valueLabel.setFontSize(12);
        valueLabel.setColor(ColorRGBA.Cyan);
        rowContainer.addChild(valueLabel);

        // 保存引用
        sliders.put(id, slider);
        valueLabels.put(id, valueLabel);
        sliderRefs.put(id, slider.getModel().createReference());

        mainContainer.addChild(rowContainer);
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
        sep.setPreferredSize(new Vector3f(panelWidth - 20, 5, 0));
        sep.setBackground(null);
        return sep;
    }

    /**
     * 更新面板位置
     */
    private void updatePosition() {
        // Lemur 坐标系：左下角为原点，Y轴向上
        // 转换：screenY = panelY (从底部算起)
        mainContainer.setLocalTranslation(panelX, panelY + panelHeight, 0);
    }

    /**
     * 在 update 循环中调用，检查滑条值变化
     */
    public void update(float tpf) {
        for (Map.Entry<String, VersionedReference<Double>> entry : sliderRefs.entrySet()) {
            String id = entry.getKey();
            VersionedReference<Double> ref = entry.getValue();

            if (ref.update()) {
                float value = ref.get().floatValue();

                // 更新数值标签
                Label valueLabel = valueLabels.get(id);
                if (valueLabel != null) {
                    valueLabel.setText(String.format("%.1f", value));
                }

                // 触发回调
                if (callback != null) {
                    callback.onValueChanged(id, value);
                }
            }
        }
    }

    /**
     * 设置回调
     */
    public void setCallback(SliderChangeCallback callback) {
        this.callback = callback;
    }

    /**
     * 设置滑条值（不触发回调）
     */
    public void setValue(String id, float value) {
        Slider slider = sliders.get(id);
        if (slider != null) {
            slider.getModel().setValue(value);

            // 更新数值标签
            Label valueLabel = valueLabels.get(id);
            if (valueLabel != null) {
                valueLabel.setText(String.format("%.1f", value));
            }

            // 更新引用以避免触发回调
            VersionedReference<Double> ref = sliderRefs.get(id);
            if (ref != null) {
                ref.update();
            }
        }
    }

    /**
     * 获取滑条值
     */
    public float getValue(String id) {
        Slider slider = sliders.get(id);
        if (slider != null) {
            return (float) slider.getModel().getValue();
        }
        return 0;
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
     * 获取主容器（用于附加到场景）
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
