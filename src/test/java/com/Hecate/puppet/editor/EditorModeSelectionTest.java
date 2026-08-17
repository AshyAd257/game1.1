package com.Hecate.puppet.editor;

/**
 * 测试模式选择界面
 */
public class EditorModeSelectionTest {

    public static void main(String[] args) {
        System.out.println("显示模式选择界面...");

        EditorModeSelectionScreen.EditorMode mode = EditorModeSelectionScreen.showModeSelection();

        if (mode == null) {
            System.out.println("用户取消了选择");
        } else {
            System.out.println("用户选择了模式: " + mode);
        }
    }
}
