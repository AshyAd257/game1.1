package com.Hecate.ui.test;

import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.app.Application;
import imgui.app.Configuration;
import imgui.flag.ImGuiConfigFlags;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;
import imgui.type.ImFloat;
import imgui.type.ImInt;
import imgui.type.ImString;

/**
 * ImGui-Java 测试应用
 *
 * 注意: ImGui-Java 使用自己的窗口系统，不直接集成到 jME3
 * 适合用于独立的编辑器工具、调试面板等
 *
 * 运行方式: 在IDE中直接运行此类的main方法
 */
public class ImGuiTestApp extends Application {

    // UI 状态变量
    private final ImString textInput = new ImString(256);
    private final ImFloat sliderValue = new ImFloat(0.5f);
    private final ImInt comboSelection = new ImInt(0);
    private final ImBoolean checkboxValue = new ImBoolean(true);
    private final ImBoolean showDemoWindow = new ImBoolean(false);
    private final ImBoolean showMetricsWindow = new ImBoolean(false);

    private int clickCount = 0;
    private float[] colorPicker = new float[]{0.5f, 0.5f, 0.5f, 1.0f};

    public static void main(String[] args) {
        launch(new ImGuiTestApp());
    }

    @Override
    protected void configure(Configuration config) {
        config.setTitle("ImGui-Java 测试应用");
        config.setWidth(1280);
        config.setHeight(720);
    }

    @Override
    protected void initImGui(Configuration config) {
        super.initImGui(config);

        final ImGuiIO io = ImGui.getIO();
        io.addConfigFlags(ImGuiConfigFlags.NavEnableKeyboard);
        io.addConfigFlags(ImGuiConfigFlags.DockingEnable);

        // 设置中文字体 (如果有的话)
        // io.getFonts().addFontFromFileTTF("path/to/chinese-font.ttf", 16.0f);
    }

    @Override
    public void process() {
        // 主控制面板
        createMainPanel();

        // 工具面板
        createToolsPanel();

        // 属性检查器
        createInspectorPanel();

        // 日志窗口
        createLogPanel();

        // 可选窗口
        if (showDemoWindow.get()) {
            ImGui.showDemoWindow(showDemoWindow);
        }
        if (showMetricsWindow.get()) {
            ImGui.showMetricsWindow(showMetricsWindow);
        }
    }

    private void createMainPanel() {
        ImGui.setNextWindowPos(20, 20, imgui.flag.ImGuiCond.FirstUseEver);
        ImGui.setNextWindowSize(350, 400, imgui.flag.ImGuiCond.FirstUseEver);

        if (ImGui.begin("ImGui 主控制面板", ImGuiWindowFlags.MenuBar)) {
            // 菜单栏
            if (ImGui.beginMenuBar()) {
                if (ImGui.beginMenu("File")) {
                    if (ImGui.menuItem("New")) {
                    }
                    if (ImGui.menuItem("Open")) {
                    }
                    if (ImGui.menuItem("Save")) {
                    }
                    ImGui.separator();
                    if (ImGui.menuItem("Exit")) {
                        System.exit(0);
                    }
                    ImGui.endMenu();
                }
                if (ImGui.beginMenu("View")) {
                    ImGui.menuItem("Show Demo Window", "", showDemoWindow);
                    ImGui.menuItem("Show Metrics", "", showMetricsWindow);
                    ImGui.endMenu();
                }
                ImGui.endMenuBar();
            }

            // 标题
            ImGui.text("ImGui-Java UI Library Test");
            ImGui.separator();

            // 按钮
            if (ImGui.button("Click Me! (" + clickCount + ")")) {
                clickCount++;
            }
            ImGui.sameLine();
            if (ImGui.button("Reset")) {
                clickCount = 0;
            }

            ImGui.spacing();

            // 文本输入
            ImGui.text("Text Input:");
            ImGui.inputText("##textInput", textInput);

            // 滑块
            ImGui.text("Slider:");
            ImGui.sliderFloat("##slider", sliderValue.getData(), 0.0f, 1.0f, "%.2f");

            // 下拉框
            String[] items = {"Option 1", "Option 2", "Option 3", "Option 4"};
            ImGui.text("Combo Box:");
            ImGui.combo("##combo", comboSelection, items);

            // 复选框
            ImGui.checkbox("Enable Feature", checkboxValue);

            ImGui.spacing();
            ImGui.separator();

            // 颜色选择器
            ImGui.text("Color Picker:");
            ImGui.colorEdit4("##colorPicker", colorPicker);

            ImGui.spacing();

            // 显示当前值
            ImGui.text("Current Values:");
            ImGui.bulletText("Text: " + textInput.get());
            ImGui.bulletText("Slider: " + String.format("%.2f", sliderValue.get()));
            ImGui.bulletText("Combo: " + items[comboSelection.get()]);
            ImGui.bulletText("Checkbox: " + checkboxValue.get());
        }
        ImGui.end();
    }

    private void createToolsPanel() {
        ImGui.setNextWindowPos(390, 20, imgui.flag.ImGuiCond.FirstUseEver);
        ImGui.setNextWindowSize(250, 200, imgui.flag.ImGuiCond.FirstUseEver);

        if (ImGui.begin("Tools")) {
            if (ImGui.button("Tool 1", 100, 30)) {
            }
            if (ImGui.button("Tool 2", 100, 30)) {
            }
            if (ImGui.button("Tool 3", 100, 30)) {
            }

            ImGui.separator();

            // 树形视图
            if (ImGui.treeNode("Tree Node")) {
                ImGui.text("Child Item 1");
                ImGui.text("Child Item 2");
                if (ImGui.treeNode("Nested")) {
                    ImGui.text("Nested Item");
                    ImGui.treePop();
                }
                ImGui.treePop();
            }
        }
        ImGui.end();
    }

    private void createInspectorPanel() {
        ImGui.setNextWindowPos(660, 20, imgui.flag.ImGuiCond.FirstUseEver);
        ImGui.setNextWindowSize(300, 300, imgui.flag.ImGuiCond.FirstUseEver);

        if (ImGui.begin("Inspector")) {
            ImGui.text("Selected Object: None");
            ImGui.separator();

            // 模拟属性编辑器
            if (ImGui.collapsingHeader("Transform", imgui.flag.ImGuiTreeNodeFlags.DefaultOpen)) {
                float[] position = {0, 0, 0};
                float[] rotation = {0, 0, 0};
                float[] scale = {1, 1, 1};

                ImGui.dragFloat3("Position", position, 0.1f);
                ImGui.dragFloat3("Rotation", rotation, 1.0f);
                ImGui.dragFloat3("Scale", scale, 0.01f);
            }

            if (ImGui.collapsingHeader("Material")) {
                ImGui.colorEdit3("Diffuse", new float[]{1, 1, 1});
                ImGui.sliderFloat("Roughness", new float[]{0.5f}, 0, 1);
                ImGui.sliderFloat("Metallic", new float[]{0.0f}, 0, 1);
            }
        }
        ImGui.end();
    }

    private void createLogPanel() {
        ImGui.setNextWindowPos(20, 440, imgui.flag.ImGuiCond.FirstUseEver);
        ImGui.setNextWindowSize(620, 200, imgui.flag.ImGuiCond.FirstUseEver);

        if (ImGui.begin("Log Console")) {
            ImGui.textColored(0.0f, 1.0f, 0.0f, 1.0f, "[INFO] ImGui-Java initialized successfully");
            ImGui.textColored(0.0f, 1.0f, 0.0f, 1.0f, "[INFO] Version: 1.90.0");
            ImGui.textColored(1.0f, 1.0f, 0.0f, 1.0f, "[WARN] This is a standalone test window");
            ImGui.text("[LOG] Click count: " + clickCount);

            ImGui.separator();
            if (ImGui.button("Clear Log")) {
            }
            ImGui.sameLine();
            if (ImGui.button("Copy Log")) {
            }
        }
        ImGui.end();
    }
}
