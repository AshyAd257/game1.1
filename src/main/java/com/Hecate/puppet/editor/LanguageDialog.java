package com.Hecate.puppet.editor;

import javax.swing.*;
import java.awt.*;

/**
 * 语言选择对话框
 */
public class LanguageDialog extends JDialog {

    private String selectedLanguage = null;

    public LanguageDialog(Frame owner) {
        super(owner, "选择语言 / Select Language", true);

        setLayout(new BorderLayout(10, 10));

        // 创建主面板
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new GridLayout(2, 1, 10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // 中文按钮
        JButton chineseButton = new JButton("中文");
        chineseButton.setFont(new Font("Microsoft YaHei", Font.PLAIN, 16));
        chineseButton.setPreferredSize(new Dimension(200, 50));
        chineseButton.addActionListener(e -> {
            selectedLanguage = "zh";
            dispose();
        });

        // 英文按钮
        JButton englishButton = new JButton("English");
        englishButton.setFont(new Font("Arial", Font.PLAIN, 16));
        englishButton.setPreferredSize(new Dimension(200, 50));
        englishButton.addActionListener(e -> {
            selectedLanguage = "en";
            dispose();
        });

        mainPanel.add(chineseButton);
        mainPanel.add(englishButton);

        add(mainPanel, BorderLayout.CENTER);

        pack();
        setLocationRelativeTo(owner);
        setResizable(false);
        setAlwaysOnTop(true);  // 确保对话框始终显示在最前面
    }

    /**
     * 获取用户选择的语言
     * @return "zh" 表示中文，"en" 表示英文，null 表示取消
     */
    public String getSelectedLanguage() {
        return selectedLanguage;
    }
}
