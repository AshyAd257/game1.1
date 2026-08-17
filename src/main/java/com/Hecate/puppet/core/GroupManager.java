package com.Hecate.puppet.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 骨骼分组管理器
 * 统一管理木偶的所有骨骼组
 *
 * 职责：
 * - 创建、删除、查找骨骼组
 * - 维护骨骼与组的关联关系
 * - 批量操作（如整体旋转）
 */
public class GroupManager {

    private Map<String, BoneGroup> groups;  // 组ID -> 组对象
    private Skeleton skeleton;  // 关联的骨骼系统

    /**
     * 创建分组管理器
     * @param skeleton 关联的骨骼系统
     */
    public GroupManager(Skeleton skeleton) {
        this.skeleton = skeleton;
        this.groups = new HashMap<>();
    }

    /**
     * 创建新的骨骼组
     * @param name 组名
     * @return 新创建的组，如果名称已存在返回null
     */
    public BoneGroup createGroup(String name) {
        // 检查名称是否已存在
        for (BoneGroup group : groups.values()) {
            if (group.getName().equals(name)) {
                return null;  // 名称冲突
            }
        }

        // 生成唯一ID
        String groupId = UUID.randomUUID().toString();
        BoneGroup group = new BoneGroup(name);
        groups.put(groupId, group);
        return group;
    }

    /**
     * 创建新的骨骼组（带指定ID）
     * @param groupId 组ID
     * @param name 组名
     * @return 新创建的组，如果ID已存在返回null
     */
    public BoneGroup createGroup(String groupId, String name) {
        if (groups.containsKey(groupId)) {
            return null;  // ID已存在
        }

        BoneGroup group = new BoneGroup(name);
        groups.put(groupId, group);
        return group;
    }

    /**
     * 删除骨骼组
     * @param groupId 组ID
     * @return true如果成功删除
     */
    public boolean deleteGroup(String groupId) {
        BoneGroup group = groups.remove(groupId);
        if (group != null) {
            // 清除所有成员骨骼的组ID
            for (Bone bone : group.getMembers()) {
                bone.setGroupId(null);
            }
            return true;
        }
        return false;
    }

    /**
     * 获取指定ID的组
     * @param groupId 组ID
     * @return 骨骼组，如果不存在返回null
     */
    public BoneGroup getGroup(String groupId) {
        return groups.get(groupId);
    }

    /**
     * 根据名称查找组
     * @param name 组名
     * @return 骨骼组，如果不存在返回null
     */
    public BoneGroup getGroupByName(String name) {
        for (BoneGroup group : groups.values()) {
            if (group.getName().equals(name)) {
                return group;
            }
        }
        return null;
    }

    /**
     * 获取骨骼所属的组
     * @param bone 骨骼
     * @return 骨骼组，如果骨骼不属于任何组返回null
     */
    public BoneGroup getGroupOf(Bone bone) {
        if (bone == null || !bone.hasGroup()) {
            return null;
        }
        return groups.get(bone.getGroupId());
    }

    /**
     * 获取所有组
     * @return 所有组的列表
     */
    public List<BoneGroup> getAllGroups() {
        return new ArrayList<>(groups.values());
    }

    /**
     * 获取组的数量
     * @return 组数量
     */
    public int getGroupCount() {
        return groups.size();
    }

    /**
     * 添加骨骼到组
     * @param groupId 组ID
     * @param bone 骨骼
     * @return true如果成功添加
     */
    public boolean addBoneToGroup(String groupId, Bone bone) {
        BoneGroup group = groups.get(groupId);
        if (group == null || bone == null) {
            return false;
        }

        // 如果骨骼已在其他组中，先移除
        if (bone.hasGroup()) {
            removeBoneFromGroup(bone);
        }

        // 添加到新组
        if (group.addMember(bone)) {
            bone.setGroupId(groupId);
            return true;
        }
        return false;
    }

    /**
     * 从组中移除骨骼
     * @param bone 骨骼
     * @return true如果成功移除
     */
    public boolean removeBoneFromGroup(Bone bone) {
        if (bone == null || !bone.hasGroup()) {
            return false;
        }

        BoneGroup group = groups.get(bone.getGroupId());
        if (group != null && group.removeMember(bone)) {
            bone.setGroupId(null);
            return true;
        }
        return false;
    }

    /**
     * 批量添加骨骼到组
     * @param groupId 组ID
     * @param bones 骨骼列表
     * @return 成功添加的数量
     */
    public int addBonesToGroup(String groupId, List<Bone> bones) {
        int count = 0;
        for (Bone bone : bones) {
            if (addBoneToGroup(groupId, bone)) {
                count++;
            }
        }
        return count;
    }

    /**
     * 清空所有组
     */
    public void clearAllGroups() {
        // 清除所有骨骼的组ID
        for (BoneGroup group : groups.values()) {
            for (Bone bone : group.getMembers()) {
                bone.setGroupId(null);
            }
        }
        groups.clear();
    }

    /**
     * 获取组ID映射表（用于序列化）
     * @return 组ID -> 组对象的映射
     */
    public Map<String, BoneGroup> getGroupsMap() {
        return new HashMap<>(groups);
    }

    /**
     * 设置组映射表（用于反序列化）
     * @param groupsMap 组ID -> 组对象的映射
     */
    public void setGroupsMap(Map<String, BoneGroup> groupsMap) {
        this.groups.clear();
        if (groupsMap != null) {
            this.groups.putAll(groupsMap);
        }
    }

    /**
     * 根据骨骼名称查找骨骼
     * @param boneName 骨骼名称
     * @return 骨骼对象，如果不存在返回null
     */
    private Bone findBoneByName(String boneName) {
        if (skeleton == null) {
            return null;
        }
        return findBoneRecursive(skeleton.getRootBone(), boneName);
    }

    /**
     * 递归查找骨骼
     * @param current 当前骨骼
     * @param name 目标名称
     * @return 找到的骨骼，如果不存在返回null
     */
    private Bone findBoneRecursive(Bone current, String name) {
        if (current == null) {
            return null;
        }
        if (current.getName().equals(name)) {
            return current;
        }
        for (Bone child : current.getChildren()) {
            Bone found = findBoneRecursive(child, name);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    /**
     * 验证组的完整性
     * 检查所有组成员是否正确设置了groupId
     * @return true如果所有组都完整
     */
    public boolean validateGroups() {
        for (Map.Entry<String, BoneGroup> entry : groups.entrySet()) {
            String groupId = entry.getKey();
            BoneGroup group = entry.getValue();

            for (Bone bone : group.getMembers()) {
                if (!groupId.equals(bone.getGroupId())) {
                    return false;  // 骨骼的groupId与实际所属组不一致
                }
            }
        }
        return true;
    }

    /**
     * 修复组的完整性
     * 同步组成员和骨骼的groupId
     */
    public void repairGroups() {
        for (Map.Entry<String, BoneGroup> entry : groups.entrySet()) {
            String groupId = entry.getKey();
            BoneGroup group = entry.getValue();

            // 为所有成员设置正确的groupId
            for (Bone bone : group.getMembers()) {
                bone.setGroupId(groupId);
            }
        }
    }

    @Override
    public String toString() {
        return String.format("GroupManager[groups=%d]", groups.size());
    }
}
