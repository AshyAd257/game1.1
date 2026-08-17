package com.Hecate.puppet.editor;

import javax.swing.*;
import java.awt.*;

/**
 * 相机跟随自由度设置对话框
 */
public class CameraFollowDialog extends JDialog {

    private float cameraFollowX = 0f;
    private float cameraFollowY = 0f;
    private boolean confirmed = false;

    public CameraFollowDialog(Frame owner, float initialX, float initialY) {
        super(owner, "Camera Follow Freedom", true);

        setLayout(new BorderLayout(10, 10));

        // 创建主面板
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new GridLayout(3, 2, 10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // X 标签和输入框
        JLabel xLabel = new JLabel("Horizontal (X):");
        xLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        JTextField xField = new JTextField(String.valueOf(initialX));
        xField.setFont(new Font("Arial", Font.PLAIN, 14));

        // Y 标签和输入框
        JLabel yLabel = new JLabel("Vertical (Y):");
        yLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        JTextField yField = new JTextField(String.valueOf(initialY));
        yField.setFont(new Font("Arial", Font.PLAIN, 14));

        // 说明标签
        JLabel hintLabel = new JLabel("Range: 0.0 - 1.0");
        hintLabel.setFont(new Font("Arial", Font.ITALIC, 12));
        hintLabel.setForeground(Color.GRAY);

        mainPanel.add(xLabel);
        mainPanel.add(xField);
        mainPanel.add(yLabel);
        mainPanel.add(yField);
        mainPanel.add(hintLabel);
        mainPanel.add(new JLabel("")); // 空白占位

        add(mainPanel, BorderLayout.CENTER);

        // 按钮面板
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT, 10, 10));

        JButton okButton = new JButton("OK");
        okButton.setPreferredSize(new Dimension(80, 30));
        okButton.addActionListener(e -> {
            try {
                float x = Float.parseFloat(xField.getText());
                float y = Float.parseFloat(yField.getText());

                // 限制范围在 0-1
                cameraFollowX = Math.max(0f, Math.min(1f, x));
                cameraFollowY = Math.max(0f, Math.min(1f, y));

                confirmed = true;
                dispose();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this,
                    "Please enter valid numbers (0.0 - 1.0)",
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

    /**
     * 用户是否确认了输入
     */
    public boolean isConfirmed() {
        return confirmed;
    }

    /**
     * 获取水平方向的相机跟随自由度
     */
    public float getCameraFollowX() {
        return cameraFollowX;
    }

    /**
     * 获取垂直方向的相机跟随自由度
     */
    public float getCameraFollowY() {
        return cameraFollowY;
    }
}
