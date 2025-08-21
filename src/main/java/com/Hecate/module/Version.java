package com.Hecate.module;

/**
 * 表示模块的版本号
 */
public class Version implements Comparable<Version> {
    private final int major;
    private final int minor;
    private final int patch;

    /**
     * 创建一个版本对象
     * @param major 主版本号
     * @param minor 次版本号
     * @param patch 补丁版本号
     */
    public Version(int major, int minor, int patch) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
    }

    /**
     * 从字符串解析版本号 (格式: "1.2.3")
     */
    public static Version parse(String version) {
        String[] parts = version.split("\\.");
        int major = Integer.parseInt(parts[0]);
        int minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
        int patch = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
        return new Version(major, minor, patch);
    }

    /**
     * 检查此版本是否与另一版本兼容
     * 兼容规则: 主版本号相同且次版本号大于等于
     */
    public boolean isCompatibleWith(Version other) {
        return this.major == other.major && this.minor >= other.minor;
    }

    @Override
    public int compareTo(Version other) {
        if (this.major != other.major) {
            return Integer.compare(this.major, other.major);
        }
        if (this.minor != other.minor) {
            return Integer.compare(this.minor, other.minor);
        }
        return Integer.compare(this.patch, other.patch);
    }

    @Override
    public String toString() {
        return major + "." + minor + "." + patch;
    }

    // Getter方法
    public int getMajor() {
        return major;
    }

    public int getMinor() {
        return minor;
    }

    public int getPatch() {
        return patch;
    }
}