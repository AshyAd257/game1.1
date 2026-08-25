package com.Hecate.puppet.editor;

import javax.swing.*;
import java.awt.*;

/**
 * 添加棱柱部件对话框
 * 让用户选择棱柱边数（10或14）以及长宽高
 */
public class AddPrismDialog extends JDialog {

    private int sideCount = 10;
    private float width = 1.0f;
    private float depth = 1.0f;
    private float height = 2.0f;
    private boolean confirmed = false;

    public AddPrismDialog(Frame owner) {
        super(owner, "添加棱柱 / Add Prism", true);

        setLayout(new BorderLayout(10, 10));

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new GridLayout(5, 2, 10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // 边数选择
        JLabel sideLabel = new JLabel("边数 / Sides:");
        sideLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        JComboBox<String> sideCombo = new JComboBox<>(new String[]{"10棱柱 (Decagon)", "14棱柱 (Tetradecagon)"});
        sideCombo.setFont(new Font("Arial", Font.PLAIN, 14));

        // 宽（X方向直径）
        JLabel widthLabel = new JLabel("宽 Width (X):");
        widthLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        JTextField widthField = new JTextField(String.valueOf(width));
        widthField.setFont(new Font("Arial", Font.PLAIN, 14));

        // 长（Z方向直径）
        JLabel depthLabel = new JLabel("长 Depth (Z):");
        depthLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        JTextField depthField = new JTextField(String.valueOf(depth));
        depthField.setFont(new Font("Arial", Font.PLAIN, 14));

        // 高（竖直方向）
        JLabel heightLabel = new JLabel("高 Height (Y):");
        heightLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        JTextField heightField = new JTextField(String.valueOf(height));
        heightField.setFont(new Font("Arial", Font.PLAIN, 14));

        // 说明
        JLabel hintLabel = new JLabel("宽≠长时横截面为椭圆 / width≠depth gives elliptical cross-section");
        hintLabel.setFont(new Font("Arial", Font.ITALIC, 11));
        hintLabel.setForeground(Color.GRAY);

        mainPanel.add(sideLabel);
        mainPanel.add(sideCombo);
        mainPanel.add(widthLabel);
        mainPanel.add(widthField);
        mainPanel.add(depthLabel);
        mainPanel.add(depthField);
        mainPanel.add(heightLabel);
        mainPanel.add(heightField);
        mainPanel.add(hintLabel);
        mainPanel.add(new JLabel(""));

        add(mainPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT, 10, 10));

        JButton okButton = new JButton("OK");
        okButton.setPreferredSize(new Dimension(80, 30));
        okButton.addActionListener(e -> {
            try {
                float w = Float.parseFloat(widthField.getText());
                float d = Float.parseFloat(depthField.getText());
                float h = Float.parseFloat(heightField.getText());

                if (w <= 0 || d <= 0 || h <= 0) {
                    JOptionPane.showMessageDialog(this,
                        "尺寸必须大于0 / Dimensions must be positive",
                        "Invalid Input",
                        JOptionPane.ERROR_MESSAGE);
                    return;
                }

                width = w;
                depth = d;
                height = h;
                sideCount = sideCombo.getSelectedIndex() == 0 ? 10 : 14;

                confirmed = true;
                dispose();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this,
                    "请输入有效数字 / Please enter valid numbers",
                    "Invalid Input",
                    JOptionPane.ERROR_MESSAGE);
            }
        });

        JButton cancelButton = new JButton("Cancel");
        cancelButton.setPreferredSize(new Dimension(80, 30));
        cancelButton.addActionListener(e -> {
            confirmed = false;
            dispose();
        });

        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);

        add(buttonPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(owner);
        setResizable(false);
        setAlwaysOnTop(true);
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public int getSideCount() {
        return sideCount;
    }

    public float getPrismWidth() {
        return width;
    }

    public float getPrismDepth() {
        return depth;
    }

    public float getPrismHeight() {
        return height;
    }
}
