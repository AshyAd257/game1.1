package com.Hecate.puppet.export;

/**
 * 导出格式枚举
 */
public enum ExportFormat {
    PUPPET("Puppet文件 (JSON)", "puppet", "Puppet Files (*.puppet)"),
    PUPPET_PACKAGE("Puppet打包文件 (含图片)", "ppkg", "Puppet Package Files (*.ppkg)"),
    JSON("纯JSON格式", "json", "JSON Files (*.json)"),
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
