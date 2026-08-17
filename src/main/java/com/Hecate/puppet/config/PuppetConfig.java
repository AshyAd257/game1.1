package com.Hecate.puppet.config;

import java.util.ArrayList;
import java.util.List;

/**
 * 木偶配置文件
 * 存储整个木偶的骨骼层级、部件设置和动画数据
 */
public class PuppetConfig {

    private String name;
    private String version = "1.0";
    private List<BoneConfig> bones;
    private List<GroupConfig> groups;  // 骨骼分组配置（新增）

    // Billboard渲染模式 (DISABLED/UNIFIED/INDEPENDENT)
    // 默认UNIFIED（向后兼容）
    private String billboardMode = "UNIFIED";

    public PuppetConfig() {
        this.bones = new ArrayList<>();
        this.groups = new ArrayList<>();
    }

    public PuppetConfig(String name) {
        this.name = name;
        this.bones = new ArrayList<>();
        this.groups = new ArrayList<>();
    }

    // ========== Getters and Setters ==========

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public List<BoneConfig> getBones() {
        return bones;
    }

    public void setBones(List<BoneConfig> bones) {
        this.bones = bones;
    }

    public void addBone(BoneConfig bone) {
        this.bones.add(bone);
    }

    public String getBillboardMode() {
        return billboardMode;
    }

    public void setBillboardMode(String billboardMode) {
        this.billboardMode = billboardMode;
    }

    public List<GroupConfig> getGroups() {
        return groups;
    }

    public void setGroups(List<GroupConfig> groups) {
        this.groups = groups;
    }

    public void addGroup(GroupConfig group) {
        this.groups.add(group);
    }
}
