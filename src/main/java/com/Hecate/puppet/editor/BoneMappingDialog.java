package com.Hecate.puppet.editor;

import com.Hecate.puppet.editor.core.EditorBone;
import com.Hecate.puppet.config.AnimationConfig;
import com.Hecate.puppet.config.BoneMappingConfig;
import com.Hecate.puppet.config.BoneMatcher;
import com.Hecate.puppet.config.AnimationIO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.*;
import java.util.List;

/**
 * 骨骼映射编辑器对话框
 * 允许用户可视化地查看和调整动画骨骼到木偶骨骼的映射关系
 */
public class BoneMappingDialog extends JDialog {

    private final AnimationConfig animConfig;
    private final List<EditorBone> puppetBones;
    private BoneMappingConfig mapping;
    private final BoneMappingConfig originalMapping; // 保存原始自动匹配结果

    private boolean confirmed = false;

    // UI组件
    private JPanel mappingPanel;
    private JLabel coverageLabel;
    private Map<String, JComboBox<String>> boneComboBoxes;
    private Map<String, JLabel> confidenceLabels;
    private Map<String, BoneMatcher.MatchResult> matchResults;

    // 木偶骨骼名称列表（用于下拉框）
    private String[] puppetBoneNames;

    public BoneMappingDialog(Frame owner, AnimationConfig animConfig,
                            BoneMappingConfig autoMapping, List<EditorBone> puppetBones) {
        super(owner, "骨骼映射编辑器", true);
        this.animConfig = animConfig;
        this.puppetBones = puppetBones;
        this.mapping = autoMapping;
        this.originalMapping = copyMapping(autoMapping);

        this.boneComboBoxes = new HashMap<>();
        this.confidenceLabels = new HashMap<>();
        this.matchResults = new HashMap<>();

        // 准备木偶骨骼名称列表
        puppetBoneNames = new String[puppetBones.size() + 1];
        puppetBoneNames[0] = "<未映射>";
        for (int i = 0; i < puppetBones.size(); i++) {
            puppetBoneNames[i + 1] = puppetBones.get(i).getName();
        }

        // 预先计算所有可能的匹配结果（用于显示置信度）
        Set<String> animBoneNames = animConfig.getAllBoneNames();
        Set<String> puppetBoneNamesSet = new HashSet<>();
        for (EditorBone bone : puppetBones) {
            puppetBoneNamesSet.add(bone.getName());
        }

        for (String animBone : animBoneNames) {
            List<BoneMatcher.MatchResult> allMatches =
                BoneMatcher.getAllPossibleMatchesForEditor(animBone, puppetBoneNamesSet, puppetBones);
            if (!allMatches.isEmpty()) {
                matchResults.put(animBone, allMatches.get(0)); // 保存最佳匹配
            }
        }

        initUI();
        pack();
        setLocationRelativeTo(owner);
    }

    private void initUI() {
        setLayout(new BorderLayout(10, 10));

        // 顶部信息栏
        JPanel topPanel = createTopPanel();
        add(topPanel, BorderLayout.NORTH);

        // 中间映射表格
        JScrollPane scrollPane = createMappingTable();
        add(scrollPane, BorderLayout.CENTER);

        // 底部按钮栏
        JPanel bottomPanel = createBottomPanel();
        add(bottomPanel, BorderLayout.SOUTH);

        setPreferredSize(new Dimension(800, 600));
    }

    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // 标题
        JLabel titleLabel = new JLabel("骨骼映射编辑器 - " + animConfig.getName());
        titleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        panel.add(titleLabel, BorderLayout.WEST);

        // 统计信息和操作按钮
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));

        coverageLabel = new JLabel();
        updateCoverageLabel();
        coverageLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        rightPanel.add(coverageLabel);

        JButton saveMappingButton = new JButton("保存映射");
        saveMappingButton.addActionListener(e -> saveMappingToFile());
        rightPanel.add(saveMappingButton);

        JButton loadMappingButton = new JButton("加载映射");
        loadMappingButton.addActionListener(e -> loadMappingFromFile());
        rightPanel.add(loadMappingButton);

        panel.add(rightPanel, BorderLayout.EAST);

        return panel;
    }

    private JScrollPane createMappingTable() {
        mappingPanel = new JPanel();
        mappingPanel.setLayout(new BoxLayout(mappingPanel, BoxLayout.Y_AXIS));
        mappingPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // 表头
        JPanel headerPanel = new JPanel(new GridLayout(1, 3, 10, 0));
        headerPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        headerPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, Color.GRAY));

        JLabel animBoneHeader = new JLabel("动画骨骼");
        animBoneHeader.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        headerPanel.add(animBoneHeader);

        JLabel puppetBoneHeader = new JLabel("映射到木偶骨骼");
        puppetBoneHeader.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        headerPanel.add(puppetBoneHeader);

        JLabel confidenceHeader = new JLabel("匹配置信度");
        confidenceHeader.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        headerPanel.add(confidenceHeader);

        mappingPanel.add(headerPanel);
        mappingPanel.add(Box.createVerticalStrut(10));

        // 映射行
        Set<String> animBoneNames = animConfig.getAllBoneNames();
        List<String> sortedAnimBones = new ArrayList<>(animBoneNames);
        Collections.sort(sortedAnimBones);

        for (String animBone : sortedAnimBones) {
            JPanel rowPanel = createMappingRow(animBone);
            mappingPanel.add(rowPanel);
            mappingPanel.add(Box.createVerticalStrut(5));
        }

        JScrollPane scrollPane = new JScrollPane(mappingPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        return scrollPane;
    }

    private JPanel createMappingRow(String animBone) {
        JPanel rowPanel = new JPanel(new GridLayout(1, 3, 10, 0));
        rowPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        rowPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // 左侧：动画骨骼名（带状态图标）
        String mappedPuppetBone = mapping.getMappedBoneName(animBone);
        String statusIcon = mappedPuppetBone != null ? "●" : "○";
        JLabel animBoneLabel = new JLabel(statusIcon + " " + animBone);
        animBoneLabel.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        rowPanel.add(animBoneLabel);

        // 中间：木偶骨骼下拉框
        JComboBox<String> comboBox = new JComboBox<>(puppetBoneNames);
        if (mappedPuppetBone != null) {
            comboBox.setSelectedItem(mappedPuppetBone);
        } else {
            comboBox.setSelectedIndex(0); // 未映射
        }
        comboBox.addActionListener(e -> {
            String selected = (String) comboBox.getSelectedItem();
            if ("<未映射>".equals(selected)) {
                mapping.removeMapping(animBone);
            } else {
                mapping.addMapping(animBone, selected);
            }
            updateCoverageLabel();
            updateConfidenceLabel(animBone);
        });
        boneComboBoxes.put(animBone, comboBox);
        rowPanel.add(comboBox);

        // 右侧：置信度标签
        JLabel confidenceLabel = new JLabel();
        updateConfidenceLabelContent(animBone, confidenceLabel);
        confidenceLabels.put(animBone, confidenceLabel);
        rowPanel.add(confidenceLabel);

        return rowPanel;
    }

    private void updateConfidenceLabel(String animBone) {
        JLabel label = confidenceLabels.get(animBone);
        if (label != null) {
            updateConfidenceLabelContent(animBone, label);
        }
    }

    private void updateConfidenceLabelContent(String animBone, JLabel label) {
        String mappedPuppetBone = mapping.getMappedBoneName(animBone);

        if (mappedPuppetBone == null) {
            label.setText("✗ 未映射");
            label.setForeground(Color.RED);
            label.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
            return;
        }

        // 获取匹配结果
        BoneMatcher.MatchResult result = matchResults.get(animBone);
        if (result != null && result.getPuppetBone() != null &&
            result.getPuppetBone().equals(mappedPuppetBone)) {
            // 使用自动匹配的结果
            switch (result.getConfidence()) {
                case EXACT:
                    label.setText("✓ 精确匹配");
                    label.setForeground(new Color(0, 150, 0));
                    break;
                case FUZZY:
                    label.setText("~ 模糊匹配");
                    label.setForeground(new Color(200, 150, 0));
                    break;
                case SEMANTIC:
                    label.setText("≈ 语义匹配");
                    label.setForeground(new Color(255, 100, 0));
                    break;
                case HIERARCHY:
                    label.setText("⊕ 层级匹配");
                    label.setForeground(new Color(255, 150, 50));
                    break;
                default:
                    label.setText("? 未知");
                    label.setForeground(Color.GRAY);
            }
        } else {
            // 手动映射
            label.setText("✎ 手动映射");
            label.setForeground(new Color(100, 100, 255));
        }
        label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
    }

    private void updateCoverageLabel() {
        Set<String> animBoneNames = animConfig.getAllBoneNames();
        float coverage = mapping.getMappingCoverage(animBoneNames);
        int mapped = 0;
        for (String animBone : animBoneNames) {
            if (mapping.hasMapping(animBone)) {
                mapped++;
            }
        }
        coverageLabel.setText(String.format("映射覆盖率: %d/%d (%.1f%%)",
            mapped, animBoneNames.size(), coverage));

        // 根据覆盖率设置颜色
        if (coverage >= 90) {
            coverageLabel.setForeground(new Color(0, 150, 0));
        } else if (coverage >= 70) {
            coverageLabel.setForeground(new Color(200, 150, 0));
        } else {
            coverageLabel.setForeground(Color.RED);
        }
    }

    private JPanel createBottomPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JButton acceptAllButton = new JButton("全部接受");
        acceptAllButton.setToolTipText("接受所有自动匹配的映射");
        acceptAllButton.addActionListener(e -> acceptAllAutoMappings());

        JButton resetButton = new JButton("重置");
        resetButton.setToolTipText("恢复到原始自动匹配状态");
        resetButton.addActionListener(e -> resetToOriginal());

        JButton cancelButton = new JButton("取消");
        cancelButton.addActionListener(e -> {
            confirmed = false;
            dispose();
        });

        JButton okButton = new JButton("确定");
        okButton.addActionListener(e -> {
            confirmed = true;
            dispose();
        });

        panel.add(acceptAllButton);
        panel.add(resetButton);
        panel.add(cancelButton);
        panel.add(okButton);

        return panel;
    }

    private void acceptAllAutoMappings() {
        // 使用原始自动匹配结果
        mapping = copyMapping(originalMapping);

        // 更新UI
        for (Map.Entry<String, JComboBox<String>> entry : boneComboBoxes.entrySet()) {
            String animBone = entry.getKey();
            JComboBox<String> comboBox = entry.getValue();
            String mappedBone = mapping.getMappedBoneName(animBone);
            if (mappedBone != null) {
                comboBox.setSelectedItem(mappedBone);
            } else {
                comboBox.setSelectedIndex(0);
            }
        }

        updateCoverageLabel();
        for (String animBone : boneComboBoxes.keySet()) {
            updateConfidenceLabel(animBone);
        }
    }

    private void resetToOriginal() {
        acceptAllAutoMappings();
    }

    private void saveMappingToFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("保存骨骼映射配置");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Mapping Files (*.mapping)", "mapping"));
        fileChooser.setSelectedFile(new File(animConfig.getName() + ".mapping"));

        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            String path = file.getAbsolutePath();
            if (!path.toLowerCase().endsWith(".mapping")) {
                path += ".mapping";
            }

            try {
                AnimationIO.saveMappingConfig(mapping, path);
                JOptionPane.showMessageDialog(this,
                    "映射配置保存成功！\n文件: " + path,
                    "保存成功", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                    "保存映射配置失败：\n" + ex.getMessage(),
                    "保存错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void loadMappingFromFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("加载骨骼映射配置");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Mapping Files (*.mapping)", "mapping"));

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            String path = file.getAbsolutePath();

            try {
                BoneMappingConfig loadedMapping = AnimationIO.loadMappingConfig(path);

                // 验证加载的映射是否适用于当前动画和木偶
                Set<String> animBoneNames = animConfig.getAllBoneNames();
                boolean valid = true;

                // 应用加载的映射
                for (String animBone : animBoneNames) {
                    String mappedBone = loadedMapping.getMappedBoneName(animBone);
                    if (mappedBone != null) {
                        mapping.addMapping(animBone, mappedBone);
                    }
                }

                // 更新UI
                for (Map.Entry<String, JComboBox<String>> entry : boneComboBoxes.entrySet()) {
                    String animBone = entry.getKey();
                    JComboBox<String> comboBox = entry.getValue();
                    String mappedBone = mapping.getMappedBoneName(animBone);
                    if (mappedBone != null) {
                        comboBox.setSelectedItem(mappedBone);
                    } else {
                        comboBox.setSelectedIndex(0);
                    }
                }

                updateCoverageLabel();
                for (String animBone : boneComboBoxes.keySet()) {
                    updateConfidenceLabel(animBone);
                }

                JOptionPane.showMessageDialog(this,
                    "映射配置加载成功！",
                    "加载成功", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                    "加载映射配置失败：\n" + ex.getMessage(),
                    "加载错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private BoneMappingConfig copyMapping(BoneMappingConfig source) {
        BoneMappingConfig copy = new BoneMappingConfig(
            source.getAnimationName(),
            source.getPuppetName()
        );
        for (Map.Entry<String, String> entry : source.getMapping().entrySet()) {
            copy.addMapping(entry.getKey(), entry.getValue());
        }
        return copy;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public BoneMappingConfig getMapping() {
        return mapping;
    }

    /**
     * 显示映射对话框
     * @return 如果用户点击确定返回映射配置，否则返回null
     */
    public static BoneMappingConfig showDialog(Frame owner, AnimationConfig animConfig,
                                              BoneMappingConfig autoMapping, List<EditorBone> puppetBones) {
        BoneMappingDialog dialog = new BoneMappingDialog(owner, animConfig, autoMapping, puppetBones);
        dialog.setVisible(true);

        if (dialog.isConfirmed()) {
            return dialog.getMapping();
        }
        return null;
    }
}
