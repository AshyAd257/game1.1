package com.Hecate.puppet.newmode;

import java.util.*;

/**
 * 新模式骨骼系统
 * 管理一个木偶的所有骨骼
 */
public class NewModeSkeleton {

    private final String name;
    private final Map<String, NewModeBone> bones = new LinkedHashMap<>();
    private NewModeBone rootBone;

    public NewModeSkeleton(String name) {
        this.name = name;
    }

    // ========== 基础属性 ==========

    public String getName() {
        return name;
    }

    // ========== 骨骼管理 ==========

    /**
     * 添加骨骼
     */
    public void addBone(NewModeBone bone) {
        bones.put(bone.getName(), bone);
        if (rootBone == null && bone.getParent() == null) {
            rootBone = bone;
        }
    }

    /**
     * 移除骨骼
     */
    public void removeBone(String boneName) {
        NewModeBone bone = bones.remove(boneName);
        if (bone == rootBone) {
            rootBone = null;
        }
    }

    /**
     * 根据名称查找骨骼
     */
    public NewModeBone findBone(String boneName) {
        return bones.get(boneName);
    }

    /**
     * 获取所有骨骼
     */
    public Collection<NewModeBone> getAllBones() {
        return bones.values();
    }

    /**
     * 获取根骨骼
     */
    public NewModeBone getRootBone() {
        return rootBone;
    }

    /**
     * 设置根骨骼
     */
    public void setRootBone(NewModeBone rootBone) {
        this.rootBone = rootBone;
        if (!bones.containsKey(rootBone.getName())) {
            addBone(rootBone);
        }
    }

    /**
     * 获取骨骼数量
     */
    public int getBoneCount() {
        return bones.size();
    }

    /**
     * 获取某个骨骼的所有子骨骼
     */
    public List<NewModeBone> getChildren(NewModeBone parent) {
        List<NewModeBone> children = new ArrayList<>();
        for (NewModeBone bone : bones.values()) {
            if (bone.getParent() == parent) {
                children.add(bone);
            }
        }
        return children;
    }

    /**
     * 按照父子关系层级遍历所有骨骼
     */
    public List<NewModeBone> getBonesByHierarchy() {
        List<NewModeBone> result = new ArrayList<>();
        if (rootBone != null) {
            traverseHierarchy(rootBone, result);
        }
        return result;
    }

    private void traverseHierarchy(NewModeBone bone, List<NewModeBone> result) {
        result.add(bone);
        for (NewModeBone child : getChildren(bone)) {
            traverseHierarchy(child, result);
        }
    }

    @Override
    public String toString() {
        return "NewModeSkeleton{" +
                "name='" + name + '\'' +
                ", boneCount=" + bones.size() +
                ", rootBone=" + (rootBone != null ? rootBone.getName() : "null") +
                '}';
    }
}
