package com.Hecate.module;

/**
 * 表示模块的依赖关系
 */
public class ModuleDependency {
    private final String moduleId;
    private final VersionRange compatibleVersions;
    private final boolean optional;

    /**
     * 创建模块依赖
     * @param moduleId 依赖模块的ID
     * @param compatibleVersions 兼容的版本范围
     * @param optional 是否为可选依赖
     */
    public ModuleDependency(String moduleId, VersionRange compatibleVersions, boolean optional) {
        this.moduleId = moduleId;
        this.compatibleVersions = compatibleVersions;
        this.optional = optional;
    }

    /**
     * 创建必需依赖（非可选）
     */
    public ModuleDependency(String moduleId, VersionRange compatibleVersions) {
        this(moduleId, compatibleVersions, false);
    }

    /**
     * 创建精确版本的必需依赖
     */
    public ModuleDependency(String moduleId, Version version) {
        this(moduleId, VersionRange.exactly(version), false);
    }

    public String getModuleId() {
        return moduleId;
    }

    public VersionRange getCompatibleVersions() {
        return compatibleVersions;
    }

    public boolean isOptional() {
        return optional;
    }

    @Override
    public String toString() {
        return moduleId + " " + compatibleVersions + (optional ? " (可选)" : "");
    }
}
