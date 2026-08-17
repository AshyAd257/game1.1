package com.Hecate.puppet.editor;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.KeyEvent;

/**
 * 木偶编辑器模式选择界面（复古风）
 *
 * 与旧版的区别：
 * - JDialog + setModal(true)：setVisible(true) 自然阻塞到窗口关闭，
 *   删除了原来的 while(isVisible()) Thread.sleep(100) 忙等待
 * - 所有子元素颜色/字体集中在顶部常量，改风格只动一处
 */
public class EditorModeSelectionScreen extends JDialog {

    public enum EditorMode { LEGACY, NEW }

    // ==== 风格常量：和编辑器主界面保持一致 ====
    private static final Color BG        = new Color(0x95959F);
    private static final Color TITLE_BG  = new Color(0x3A3A42);
    private static final Color TITLE_FG  = new Color(0xE8E840);
    private static final Color BTN_BG    = new Color(0x5A5A64);
    private static final Color BTN_HOVER = new Color(0x6A6A74);
    private static final Color BTN_EDGE  = new Color(0x46464E);
    private static final Color FG        = Color.WHITE;
    private static final Color FG_DIM    = new Color(0xC8C8D0);
    private static final Font  MONO      = new Font(Font.MONOSPACED, Font.PLAIN, 16);
    private static final Font  MONO_SM   = new Font(Font.MONOSPACED, Font.PLAIN, 13);

    private EditorMode selectedMode = null;

    private EditorModeSelectionScreen() {
        super((Frame) null, true);  // 无父窗口的模态对话框
        setModal(true);
        setUndecorated(true);
        setResizable(false);
        setAlwaysOnTop(true);  // 确保在所有窗口之上

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG);

        // ---- 标题栏 ----
        JPanel titleBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 10));
        titleBar.setBackground(TITLE_BG);
        JLabel title = new JLabel("=== Puppet Editor ===");
        title.setFont(MONO.deriveFont(Font.BOLD, 15f));
        title.setForeground(TITLE_FG);
        titleBar.add(title);
        root.add(titleBar, BorderLayout.NORTH);

        // ---- 中央按钮区 ----
        JPanel center = new JPanel();
        center.setBackground(BG);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setBorder(BorderFactory.createEmptyBorder(70, 0, 80, 0));

        JLabel prompt = new JLabel("Select Mode");
        prompt.setFont(MONO.deriveFont(14f));
        prompt.setForeground(FG);
        prompt.setAlignmentX(Component.CENTER_ALIGNMENT);
        center.add(prompt);
        center.add(Box.createVerticalStrut(30));

        center.add(makeButton("传统模式", FG, () -> choose(EditorMode.LEGACY)));
        center.add(Box.createVerticalStrut(14));
        center.add(makeButton("新模式", FG, () -> choose(EditorMode.NEW)));
        center.add(Box.createVerticalStrut(34));
        center.add(makeButton("退出 (ESC)", FG_DIM, this::cancel));
        center.add(Box.createVerticalStrut(24));

        JLabel note = new JLabel("两种模式数据不兼容");
        note.setFont(MONO_SM);
        note.setForeground(FG_DIM);
        note.setAlignmentX(Component.CENTER_ALIGNMENT);
        center.add(note);

        root.add(center, BorderLayout.CENTER);
        setContentPane(root);

        // ---- ESC 退出 ----
        getRootPane().registerKeyboardAction(
                e -> cancel(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        // 全屏
        setSize(Toolkit.getDefaultToolkit().getScreenSize());
        setLocation(0, 0);
    }

    private JButton makeButton(String text, Color fg, Runnable onClick) {
        JButton b = new JButton(text);
        b.setFont(MONO.deriveFont(15f));
        b.setForeground(fg);
        b.setBackground(BTN_BG);
        b.setBorder(new LineBorder(BTN_EDGE, 1));
        b.setFocusPainted(false);
        b.setContentAreaFilled(true);
        b.setOpaque(true);
        b.setAlignmentX(Component.CENTER_ALIGNMENT);
        Dimension size = new Dimension(260, 44);
        b.setPreferredSize(size);
        b.setMaximumSize(size);
        b.setMinimumSize(size);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) { b.setBackground(BTN_HOVER); }
            @Override public void mouseExited (java.awt.event.MouseEvent e) { b.setBackground(BTN_BG); }
            @Override public void mousePressed(java.awt.event.MouseEvent e) { b.setBackground(BTN_EDGE); }
            @Override public void mouseReleased(java.awt.event.MouseEvent e) { b.setBackground(BTN_HOVER); }
        });
        b.addActionListener(e -> onClick.run());
        return b;
    }

    private void choose(EditorMode mode) {
        selectedMode = mode;
        dispose();
    }

    private void cancel() {
        selectedMode = null;
        dispose();
    }

    /**
     * 显示模式选择界面并阻塞等待。
     * @return 选中的模式；用户取消/ESC 时返回 null
     */
    public static EditorMode showModeSelection() {
        final EditorModeSelectionScreen[] screenHolder = new EditorModeSelectionScreen[1];

        // 确保在 EDT 线程中创建和显示对话框
        try {
            SwingUtilities.invokeAndWait(() -> {
                screenHolder[0] = new EditorModeSelectionScreen();
                screenHolder[0].setVisible(true);  // 模态：这里自然阻塞在 EDT 线程
            });
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        return screenHolder[0] != null ? screenHolder[0].selectedMode : null;
    }
}
