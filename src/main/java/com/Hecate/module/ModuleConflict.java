package com.Hecate.module;

/**
 * 表示模块与其他模块的冲突
 */
public class ModuleConflict {
    private final String conflictingModuleId;
    private final VersionRange conflictingVersions;
    private final String reason;

    /**
     * 创建模块冲突
     * @param conflictingModuleId 冲突模块ID
     * @param conflictingVersions 冲突的版本范围
     * @param reason 冲突原因
     */
    public ModuleConflict(String conflictingModuleId, VersionRange conflictingVersions, String reason) {
        this.conflictingModuleId = conflictingModuleId;
        this.conflictingVersions = conflictingVersions;
        this.reason = reason;
    }

    public String getConflictingModuleId() {
        return conflictingModuleId;
    }

    public VersionRange getConflictingVersions() {
        return conflictingVersions;
    }

    public String getReason() {
        return reason;
    }

    @Override
    public String toString() {
        return "与模块 " + conflictingModuleId + " " + conflictingVersions + " 冲突: " + reason;
    }
}
