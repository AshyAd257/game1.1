package com.Hecate.puppet.editor;

import com.Hecate.puppet.editor.lemur.HybridPuppetEditorApp;
import javax.swing.*;
import java.awt.*;

/**
 * 木偶编辑器启动器
 * 用于打包成独立可执行程序
 * 支持选择编辑器模式（传统模式/新模式）和UI模式（Classic/Lemur）
 */
public class PuppetEditorLauncher {

    // UI 模式
    public enum UIMode {
        CLASSIC,  // 原有的自定义 UI
        LEMUR     // 新的 Lemur GUI
    }

    // 默认使用经典模式
    private static UIMode currentMode = UIMode.CLASSIC;

    public static void main(String[] args) {
        // 先显示模式选择界面（传统模式 vs 新模式）
        EditorModeSelectionScreen.EditorMode editorMode = EditorModeSelectionScreen.showModeSelection();

        // 如果用户取消，则退出
        if (editorMode == null) {
            System.out.println("用户取消启动编辑器");
            return;
        }

        // 检查命令行参数来决定UI模式
        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("--lemur") || args[0].equalsIgnoreCase("-l")) {
                currentMode = UIMode.LEMUR;
            } else if (args[0].equalsIgnoreCase("--classic") || args[0].equalsIgnoreCase("-c")) {
                currentMode = UIMode.CLASSIC;
            } else if (args[0].equalsIgnoreCase("--select") || args[0].equalsIgnoreCase("-s")) {
                // 显示UI模式选择对话框
                currentMode = showModeSelectionDialog();
            }
        }

        // 根据选择的编辑器模式启动
        switch (editorMode) {
            case LEGACY:
                // 启动传统模式编辑器
                launchEditor(currentMode);
                break;
            case NEW:
                // 启动新模式编辑器
                launchNewModeEditor();
                break;
        }
    }

    /**
     * 启动新模式编辑器
     */
    private static void launchNewModeEditor() {
        try {
            // 使用反射调用新模式编辑器的main方法
            Class<?> newEditorClass = Class.forName("com.Hecate.puppet.newmode.NewModePuppetEditorApp");
            java.lang.reflect.Method mainMethod = newEditorClass.getMethod("main", String[].class);
            String[] args = new String[0];
            mainMethod.invoke(null, (Object) args);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(
                null,
                "启动新模式编辑器失败: " + e.getMessage(),
                "错误",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }

    /**
     * 显示UI模式选择对话框
     */
    private static UIMode showModeSelectionDialog() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // ignore
        }

        String[] options = {"Classic UI (经典)", "Lemur UI (新版)"};
        int choice = JOptionPane.showOptionDialog(
            null,
            "请选择编辑器 UI 模式:\n\n" +
            "Classic UI - 原有的自定义界面\n" +
            "Lemur UI - 新版 Lemur GUI 界面 (实验性)",
            "Puppet Editor - UI 模式选择",
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            options,
            options[0]
        );

        return (choice == 1) ? UIMode.LEMUR : UIMode.CLASSIC;
    }

    /**
     * 启动编辑器
     */
    public static void launchEditor(UIMode mode) {

        switch (mode) {
            case LEMUR -> {
                HybridPuppetEditorApp.main(new String[]{});
            }
            case CLASSIC -> {
                PuppetEditorApp editor = PuppetEditorApp.createEditor();
                editor.start();
            }
        }
    }

    /**
     * 直接启动经典模式
     */
    public static void launchClassic() {
        launchEditor(UIMode.CLASSIC);
    }

    /**
     * 直接启动 Lemur 模式
     */
    public static void launchLemur() {
        launchEditor(UIMode.LEMUR);
    }
}
