package com.Hecate.puppet.export;

/**
 * 导出格式枚举
 */
public enum ExportFormat {
    PUPPET("Puppet完整文件（贴图＋行权重）", "puppet", "Puppet Files (*.puppet)"),
    PUPPET_PACKAGE("Puppet打包文件 (含图片)", "ppkg", "Puppet Package Files (*.ppkg)"),
    JSON("JSON参数文件（不含图片）", "json", "JSON Files (*.json)"),
    DRAGONBONES("DragonBones格式", "json", "DragonBones Files (*.json)");

    private final String displayName;
    private final String extension;
    private final String description;

    ExportFormat(String displayName, String extension, String description) {
        this.displayName = displayName;
        this.extension = extension;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getExtension() {
        return extension;
    }

    public String getDescription() {
        return description;
    }
}
