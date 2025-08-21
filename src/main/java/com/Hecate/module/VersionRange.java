package com.Hecate.module;

/**
 * 表示版本范围，例如 ">=1.0.0 <2.0.0"
 */
public class VersionRange {
    private final Version minVersion;
    private final Version maxVersion;
    private final boolean includeMin;
    private final boolean includeMax;

    /**
     * 创建版本范围
     */
    public VersionRange(Version minVersion, Version maxVersion, boolean includeMin, boolean includeMax) {
        this.minVersion = minVersion;
        this.maxVersion = maxVersion;
        this.includeMin = includeMin;
        this.includeMax = includeMax;
    }

    /**
     * 创建"大于等于"特定版本的范围
     */
    public static VersionRange atLeast(Version version) {
        return new VersionRange(version, null, true, false);
    }

    /**
     * 创建精确匹配特定版本的范围
     */
    public static VersionRange exactly(Version version) {
        return new VersionRange(version, version, true, true);
    }

    /**
     * 检查指定版本是否在范围内
     */
    public boolean contains(Version version) {
        if (minVersion != null) {
            int comparison = version.compareTo(minVersion);
            if (comparison < 0 || (comparison == 0 && !includeMin)) {
                return false;
            }
        }

        if (maxVersion != null) {
            int comparison = version.compareTo(maxVersion);
            if (comparison > 0 || (comparison == 0 && !includeMax)) {
                return false;
            }
        }

        return true;
    }

    // Getter方法
    public Version getMinVersion() {
        return minVersion;
    }

    public Version getMaxVersion() {
        return maxVersion;
    }
}