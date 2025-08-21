package com.Hecate.module;

import java.util.Objects;

/**
 * 表示模块提供的能力
 */
public class ModuleCapability {
    private final String capabilityId;
    private final Version version;

    /**
     * 创建一个模块能力
     * @param capabilityId 能力ID
     * @param version 能力版本
     */
    public ModuleCapability(String capabilityId, Version version) {
        this.capabilityId = capabilityId;
        this.version = version;
    }

    public String getCapabilityId() {
        return capabilityId;
    }

    public Version getVersion() {
        return version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ModuleCapability that = (ModuleCapability) o;
        return Objects.equals(capabilityId, that.capabilityId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(capabilityId);
    }

    @Override
    public String toString() {
        return capabilityId + " v" + version;
    }
}
