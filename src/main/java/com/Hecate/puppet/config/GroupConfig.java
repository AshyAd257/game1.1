package com.Hecate.puppet.config;

import java.util.ArrayList;
import java.util.List;

/**
 * 骨骼组配置
 * 用于序列化/反序列化BoneGroup
 */
public class GroupConfig {

    private String groupId;  // 组的唯一ID
    private String name;  // 组名
    private List<String> memberBoneNames;  // 成员骨骼的名称列表
    private int currentRotation;  // 当前旋转角度

    public GroupConfig() {
        this.memberBoneNames = new ArrayList<>();
        this.currentRotation = 0;
    }

    public GroupConfig(String groupId, String name) {
        this.groupId = groupId;
        this.name = name;
        this.memberBoneNames = new ArrayList<>();
        this.currentRotation = 0;
    }

    // ========== Getters and Setters ==========

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getMemberBoneNames() {
        return memberBoneNames;
    }

    public void setMemberBoneNames(List<String> memberBoneNames) {
        this.memberBoneNames = memberBoneNames;
    }

    public void addMemberBoneName(String boneName) {
        if (!memberBoneNames.contains(boneName)) {
            memberBoneNames.add(boneName);
        }
    }

    public int getCurrentRotation() {
        return currentRotation;
    }

    public void setCurrentRotation(int currentRotation) {
        this.currentRotation = currentRotation;
    }

    @Override
    public String toString() {
        return String.format("GroupConfig[id=%s, name=%s, members=%d, rotation=%d°]",
            groupId, name, memberBoneNames.size(), currentRotation);
    }
}
