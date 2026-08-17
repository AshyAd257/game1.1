package com.Hecate.puppet.editor;

import com.jme3.app.SimpleApplication;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Quad;
import com.jme3.input.InputManager;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

/**
 * 可拖动的滑条UI组件
 * 用于调整数值参数
 */
public class Slider {

    private final SimpleApplication app;
    private final BitmapFont font;
    private final TTFontLoader ttfLoader;  // 新增：TTF字体加载器
    private final Node rootNode;

    private String label;
    private float minValue;
    private float maxValue;
    private float currentValue;

    // UI组件
    private BitmapText labelText;
    private BitmapText valueText;
    private Node ttfLabelNode;  // 新增：TTF标签节点
    private Node ttfValueNode;  // 新增：TTF值节点
    private Geometry trackGeometry;
    private Geometry handleGeometry;

    // 布局参数
    private int x, y;  // 改为可变，支持位置更新
    private int width, height;  // 改为可变，支持大小更新
    private int trackWidth = 280;  // 改为可变，支持宽度更新
    private final int trackHeight = 4;
    private final int handleWidth = 12;
    private final int handleHeight = 20;

    // 交互状态
    private boolean isDragging = false;
    private SliderChangeListener changeListener;

    // 灵敏度控制
    private float sensitivity = 1.0f;  // 默认灵敏度为1.0
    private int lastMouseX = 0;  // 记录上一次鼠标位置

    // 双击检测
    private long lastClickTime = 0;
    private static final long DOUBLE_CLICK_INTERVAL = 300; // 300毫秒内的两次点击算双击

    /**
     * 滑条变化监听器接口
     */
    public interface SliderChangeListener {
        void onValueChanged(float newValue);
    }

    // 原构造函数 - 使用BitmapFont
    public Slider(SimpleApplication app, BitmapFont font, String label,
                  float minValue, float maxValue, float initialValue,
                  int x, int y) {
        this.app = app;
        this.font = font;
        this.ttfLoader = null;
        this.label = label;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.currentValue = initialValue;
        this.x = x;
        this.y = y;
        this.width = trackWidth + 100;
        this.height = 30;

        this.rootNode = new Node("Slider_" + label);
        initializeSlider();
    }

    // 新构造函数 - 使用TTFontLoader
    public Slider(SimpleApplication app, TTFontLoader ttfLoader, String label,
                  float minValue, float maxValue, float initialValue,
                  int x, int y) {
        this.app = app;
        this.font = null;
        this.ttfLoader = ttfLoader;
        this.label = label;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.currentValue = initialValue;
        this.x = x;
        this.y = y;
        this.width = trackWidth + 100;
        this.height = 30;

        this.rootNode = new Node("Slider_" + label);
        initializeSlider();
    }

    /**
     * 初始化滑条
     */
    private void initializeSlider() {
        // 标签文本（放在滑条上方）
        if (ttfLoader != null) {
            // 使用TTFontLoader
            ttfLabelNode = ttfLoader.createText(label, ColorRGBA.White);
            ttfLabelNode.setLocalTranslation(x, y + 30, 0);
            rootNode.attachChild(ttfLabelNode);
        } else {
            // 使用BitmapFont
            labelText = new BitmapText(font);
            labelText.setText(label);
            labelText.setSize(font.getCharSet().getRenderedSize() * 1.6f);
            labelText.setColor(ColorRGBA.White);
            labelText.setLocalTranslation(x, y + 30, 0);
            rootNode.attachChild(labelText);
        }

        // 轨道（在标签下方）
        createTrack();

        // 滑块
        createHandle();

        // 数值文本（在滑条右侧）
        if (ttfLoader != null) {
            // 使用TTFontLoader
            updateValueTextTTF();
        } else {
            // 使用BitmapFont
            valueText = new BitmapText(font);
            updateValueText();
            valueText.setSize(font.getCharSet().getRenderedSize() * 1.5f);
            valueText.setColor(ColorRGBA.Cyan);
            valueText.setLocalTranslation(x + trackWidth + 20, y - 10, 0);
            rootNode.attachChild(valueText);
        }
    }

    /**
     * 创建轨道
     */
    private void createTrack() {
        Quad trackQuad = new Quad(trackWidth, trackHeight);
        trackGeometry = new Geometry("SliderTrack", trackQuad);

        Material trackMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        trackMat.setColor("Color", new ColorRGBA(0.3f, 0.3f, 0.3f, 1.0f));
        trackGeometry.setMaterial(trackMat);

        trackGeometry.setLocalTranslation(x, y - trackHeight - 5, 0);
        rootNode.attachChild(trackGeometry);
    }

    /**
     * 创建滑块
     */
    private void createHandle() {
        Quad handleQuad = new Quad(handleWidth, handleHeight);
        handleGeometry = new Geometry("SliderHandle", handleQuad);

        Material handleMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        handleMat.setColor("Color", new ColorRGBA(0.2f, 0.6f, 1.0f, 1.0f));
        handleGeometry.setMaterial(handleMat);

        updateHandlePosition();
        rootNode.attachChild(handleGeometry);
    }

    /**
     * 更新滑块位置
     */
    private void updateHandlePosition() {
        float normalizedValue = (currentValue - minValue) / (maxValue - minValue);
        float handleX = x + normalizedValue * trackWidth - handleWidth / 2;
        float handleY = y - handleHeight / 2 - 5;
        handleGeometry.setLocalTranslation(handleX, handleY, 1);
    }

    /**
     * 更新数值文本
     */
    private void updateValueText() {
        if (valueText != null) {
            valueText.setText(String.format("%.2f", currentValue));
        } else if (ttfLoader != null) {
            updateValueTextTTF();
        }
    }

    /**
     * 更新数值文本（TTF版本）
     */
    private void updateValueTextTTF() {
        if (ttfValueNode != null) {
            rootNode.detachChild(ttfValueNode);
        }
        ttfValueNode = ttfLoader.createText(String.format("%.2f", currentValue), ColorRGBA.Cyan);
        ttfValueNode.setLocalTranslation(x + trackWidth + 20, y - 10, 0);
        rootNode.attachChild(ttfValueNode);
    }

    /**
     * 处理鼠标点击
     */
    public boolean handleMouseClick(int mouseX, int mouseY) {
        // 检查是否点击在数值文本上（用于键盘输入）
        if (isPointInValueText(mouseX, mouseY)) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastClickTime < DOUBLE_CLICK_INTERVAL) {
                // 双击 - 打开输入对话框
                openInputDialog();
                lastClickTime = 0; // 重置双击计时
            } else {
                lastClickTime = currentTime;
            }
            return true;
        }

        // 检查是否点击在滑块或轨道上
        if (isPointInSlider(mouseX, mouseY)) {
            isDragging = true;
            lastMouseX = mouseX;  // 记录起始位置
            updateValueFromMouseX(mouseX);
            return true;
        }
        return false;
    }

    /**
     * 处理鼠标拖动
     * @return 如果正在拖动滑条返回true，否则返回false
     */
    public boolean handleMouseDrag(int mouseX, int mouseY) {
        if (isDragging) {
            // 使用增量更新模式，考虑灵敏度
            int deltaX = mouseX - lastMouseX;
            float deltaValue = (deltaX / (float)trackWidth) * (maxValue - minValue) * sensitivity;

            float newValue = currentValue + deltaValue;
            newValue = Math.max(minValue, Math.min(maxValue, newValue));

            if (Math.abs(newValue - currentValue) > 0.01f) {
                currentValue = newValue;
                updateHandlePosition();
                updateValueText();

                if (changeListener != null) {
                    changeListener.onValueChanged(currentValue);
                }
            }

            lastMouseX = mouseX;  // 更新上次位置
            return true;
        }
        return false;
    }

    /**
     * 处理鼠标释放
     */
    public void handleMouseRelease() {
        isDragging = false;
    }

    /**
     * 根据鼠标X坐标更新数值
     */
    private void updateValueFromMouseX(int mouseX) {
        float relativeX = mouseX - x;
        float normalizedValue = Math.max(0, Math.min(1, relativeX / trackWidth));
        float newValue = minValue + normalizedValue * (maxValue - minValue);

        if (Math.abs(newValue - currentValue) > 0.01f) {
            currentValue = newValue;
            updateHandlePosition();
            updateValueText();

            if (changeListener != null) {
                changeListener.onValueChanged(currentValue);
            }
        }
    }

    /**
     * 检查点是否在滑条区域内
     */
    private boolean isPointInSlider(int mouseX, int mouseY) {
        // guiNode 和鼠标事件都使用底部为原点的坐标系，这里不再转换
        float sliderLeft = x;
        float sliderRight = x + trackWidth;

        float handleBottom = handleGeometry.getLocalTranslation().y;
        float handleTop = handleBottom + handleHeight;
        float trackBottom = trackGeometry.getLocalTranslation().y;

        float sliderBottom = Math.min(handleBottom, trackBottom) - 6f; // 留出容错
        float sliderTop = handleTop + 6f;

        boolean xInRange = mouseX >= sliderLeft && mouseX <= sliderRight;
        boolean yInRange = mouseY >= sliderBottom && mouseY <= sliderTop;
        boolean result = xInRange && yInRange;

        return result;
    }

    /**
     * 检查点是否在数值文本区域内
     */
    private boolean isPointInValueText(int mouseX, int mouseY) {
        if (valueText != null) {
            // 使用BitmapFont
            float textX = valueText.getLocalTranslation().x;
            float textY = valueText.getLocalTranslation().y;
            float textWidth = valueText.getLineWidth();
            float textHeight = valueText.getLineHeight();

            // 扩大点击区域，方便点击
            float padding = 5f;

            boolean xInRange = mouseX >= textX - padding && mouseX <= textX + textWidth + padding;
            boolean yInRange = mouseY >= textY - textHeight - padding && mouseY <= textY + padding;

            return xInRange && yInRange;
        } else if (ttfValueNode != null) {
            // 使用TTF - 简化的边界检测
            float textX = x + trackWidth + 20;
            float textY = y - 10;
            float textWidth = 60;  // 估计宽度
            float textHeight = 20; // 估计高度

            float padding = 5f;

            boolean xInRange = mouseX >= textX - padding && mouseX <= textX + textWidth + padding;
            boolean yInRange = mouseY >= textY - textHeight - padding && mouseY <= textY + padding;

            return xInRange && yInRange;
        }
        return false;
    }

    /**
     * 打开输入对话框让用户输入数值
     */
    private void openInputDialog() {
        // 在独立线程中显示输入对话框
        new Thread(() -> {
            // 创建置顶的父窗口
            JFrame parentFrame = new JFrame();
            parentFrame.setAlwaysOnTop(true);
            parentFrame.setUndecorated(true);
            parentFrame.setSize(0, 0);
            parentFrame.setVisible(false);

            String input = JOptionPane.showInputDialog(
                parentFrame,
                label + " (" + minValue + " ~ " + maxValue + "):",
                String.format("%.2f", currentValue)
            );

            parentFrame.dispose();

            if (input != null && !input.trim().isEmpty()) {
                try {
                    float newValue = Float.parseFloat(input.trim());

                    // 在渲染线程中更新滑条
                    app.enqueue(() -> {
                        setValue(newValue);
                        if (changeListener != null) {
                            changeListener.onValueChanged(currentValue);
                        }
                        return null;
                    });
                } catch (NumberFormatException e) {
                    // 输入无效，显示错误提示
                    JFrame errorFrame = new JFrame();
                    errorFrame.setAlwaysOnTop(true);
                    errorFrame.setUndecorated(true);
                    errorFrame.setSize(0, 0);
                    errorFrame.setVisible(false);

                    JOptionPane.showMessageDialog(
                        errorFrame,
                        "Invalid number format: " + input,
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                    );

                    errorFrame.dispose();
                }
            }
        }).start();
    }

    /**
     * 设置数值
     */
    public void setValue(float value) {
        this.currentValue = Math.max(minValue, Math.min(maxValue, value));
        updateHandlePosition();
        updateValueText();
    }

    /**
     * 获取当前数值
     */
    public float getValue() {
        return currentValue;
    }

    /**
     * 设置变化监听器
     */
    public void setChangeListener(SliderChangeListener listener) {
        this.changeListener = listener;
    }

    /**
     * 设置灵敏度（0.1 = 很慢，1.0 = 正常，2.0 = 很快）
     */
    public void setSensitivity(float sensitivity) {
        this.sensitivity = Math.max(0.01f, sensitivity);
    }

    /**
     * 获取根节点
     */
    public Node getRootNode() {
        return rootNode;
    }

    /**
     * 检查是否正在拖动
     */
    public boolean isDragging() {
        return isDragging;
    }

    /**
     * 设置滑条位置（用于拖动面板时更新滑条位置）
     */
    public void setPosition(int newX, int newY) {
        this.x = newX;
        this.y = newY;

        // 更新标签位置（与初始化保持一致）
        if (labelText != null) {
            labelText.setLocalTranslation(x, y + 30, 0);
        } else if (ttfLabelNode != null) {
            ttfLabelNode.setLocalTranslation(x, y + 30, 0);
        }

        // 更新数值文本位置（与初始化保持一致）
        if (valueText != null) {
            valueText.setLocalTranslation(x + trackWidth + 20, y - 10, 0);
        } else if (ttfValueNode != null) {
            ttfValueNode.setLocalTranslation(x + trackWidth + 20, y - 10, 0);
        }

        // 更新滑条轨道位置（与初始化保持一致）
        if (trackGeometry != null) {
            trackGeometry.setLocalTranslation(x, y - trackHeight - 5, 0);
        }

        // 更新滑块位置
        updateHandlePosition();
    }

    /**
     * 设置滑条的值范围
     * @param min 最小值
     * @param max 最大值
     */
    public void setRange(float min, float max) {
        this.minValue = min;
        this.maxValue = max;

        // 确保当前值在新范围内
        this.currentValue = Math.max(min, Math.min(max, this.currentValue));

        // 更新显示
        updateHandlePosition();
        if (valueText != null) {
            valueText.setText(String.format("%.2f", currentValue));
        }
    }

    /**
     * 更新滑条轨道宽度（用于面板调整大小时更新）
     * @param newTrackWidth 新的轨道宽度
     */
    public void updateTrackWidth(int newTrackWidth) {
        if (newTrackWidth < 50) {
            newTrackWidth = 50;  // 最小宽度
        }
        this.trackWidth = newTrackWidth;
        this.width = trackWidth + 100;

        // 重建轨道几何体
        if (trackGeometry != null) {
            trackGeometry.removeFromParent();
        }
        createTrack();

        // 更新数值文本位置
        if (valueText != null) {
            valueText.setLocalTranslation(x + trackWidth + 20, y - 10, 0);
        }

        // 更新滑块位置
        updateHandlePosition();
    }

    /**
     * 获取当前轨道宽度
     */
    public int getTrackWidth() {
        return trackWidth;
    }

    /**
     * 更新标签文本（用于语言切换）
     */
    public void setText(String newLabel) {
        this.label = newLabel;

        if (ttfLoader != null) {
            // 使用TTF：移除旧节点，创建新节点
            if (ttfLabelNode != null) {
                ttfLabelNode.removeFromParent();
            }
            ttfLabelNode = ttfLoader.createText(newLabel, ColorRGBA.White);
            ttfLabelNode.setLocalTranslation(x, y + 30, 0);
            rootNode.attachChild(ttfLabelNode);
        } else {
            // 使用BitmapFont：直接更新文本
            if (labelText != null) {
                labelText.setText(newLabel);
            }
        }
    }
}
