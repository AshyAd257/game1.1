package com.Hecate.puppet.editor.core;

import com.Hecate.puppet.core.DirectionRotation;
import java.util.ArrayList;
import java.util.List;

/**
 * 编辑器骨骼分组类
 * 用于将多个EditorBone组合成一个逻辑单元，支持整体旋转等操作
 *
 * 注意：这是编辑器专用版本，与游戏运行时的BoneGroup类独立
 */
public class EditorBoneGroup {

    private String name;  // 组名
    private List<EditorBone> members;  // 组成员
    private int currentRotation;  // 当前旋转角度（0, 90, 180, 270）
    private boolean collapsed = false;  // UI折叠状态（PartListPanel拖拽分组用，不参与序列化）

    /**
     * 创建一个骨骼组
     * @param name 组名
     */
    public EditorBoneGroup(String name) {
        this.name = name;
        this.members = new ArrayList<>();
        this.currentRotation = 0;  // 初始无旋转
    }

    /**
     * 添加骨骼到组
     * @param bone 要添加的骨骼
     * @return true如果成功添加（骨骼不在组中）
     */
    public boolean addMember(EditorBone bone) {
        if (bone == null || members.contains(bone)) {
            return false;
        }
        members.add(bone);
        return true;
    }

    /**
     * 从组中移除骨骼
     * @param bone 要移除的骨骼
     * @return true如果成功移除（骨骼在组中）
     */
    public boolean removeMember(EditorBone bone) {
        return members.remove(bone);
    }

    /**
     * 检查骨骼是否在组中
     * @param bone 骨骼
     * @return true如果骨骼在组中
     */
    public boolean containsMember(EditorBone bone) {
        return members.contains(bone);
    }

    /**
     * 获取所有成员
     * @return 成员列表的副本
     */
    public List<EditorBone> getMembers() {
        return new ArrayList<>(members);
    }

    /**
     * 获取成员数量
     * @return 成员数量
     */
    public int getMemberCount() {
        return members.size();
    }

    /**
     * 清空所有成员
     */
    public void clearMembers() {
        members.clear();
    }

    /**
     * 检查组是否为空
     * @return true如果组中没有成员
     */
    public boolean isEmpty() {
        return members.isEmpty();
    }

    /**
     * 获取组名
     * @return 组名
     */
    public String getName() {
        return name;
    }

    /**
     * 设置组名
     * @param name 新组名
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 获取当前旋转角度
     * @return 旋转角度（0, 90, 180, 270）
     */
    public int getCurrentRotation() {
        return currentRotation;
    }

    /**
     * 设置当前旋转角度
     * @param rotation 旋转角度（会被规范化到0-360范围）
     */
    public void setCurrentRotation(int rotation) {
        // 规范化到0-360，并对齐到90度的倍数
        rotation = rotation % 360;
        if (rotation < 0) rotation += 360;
        this.currentRotation = (rotation / 90) * 90;
    }

    /**
     * 向左旋转90度
     * 应用方向重映射到所有成员
     */
    public void rotateLeft90() {
        for (EditorBone bone : members) {
            DirectionRotation.rotateDirectionsLeft90(bone);
        }
        currentRotation = (currentRotation + 90) % 360;
    }

    /**
     * 向右旋转90度
     * 应用方向重映射到所有成员
     */
    public void rotateRight90() {
        for (EditorBone bone : members) {
            DirectionRotation.rotateDirectionsRight90(bone);
        }
        currentRotation = (currentRotation - 90 + 360) % 360;
    }

    /**
     * 转身180度
     * 应用方向重映射到所有成员
     */
    public void rotate180() {
        for (EditorBone bone : members) {
            DirectionRotation.rotateDirections180(bone);
        }
        currentRotation = (currentRotation + 180) % 360;
    }

    /**
     * 重置旋转到0度
     * 根据当前旋转角度，反向旋转回到初始状态
     */
    public void resetRotation() {
        while (currentRotation != 0) {
            rotateRight90();
        }
    }

    /**
     * 是否折叠（PartListPanel里的UI状态，不参与序列化）
     */
    public boolean isCollapsed() {
        return collapsed;
    }

    public void setCollapsed(boolean collapsed) {
        this.collapsed = collapsed;
    }

    /**
     * 整体平移组内所有成员（相对偏移，直接叠加到每个成员当前的localPosition/restPosition）
     * @param dx X偏移
     * @param dy Y偏移
     * @param dz Z偏移
     */
    public void translateAll(float dx, float dy, float dz) {
        com.jme3.math.Vector3f delta = new com.jme3.math.Vector3f(dx, dy, dz);
        for (EditorBone bone : members) {
            com.jme3.math.Vector3f pos = bone.getLocalPosition().add(delta);
            bone.setLocalPosition(pos);
            bone.setRestPosition(pos.clone());
        }
    }

    @Override
    public String toString() {
        return String.format("EditorBoneGroup[name=%s, members=%d, rotation=%d°]",
            name, members.size(), currentRotation);
    }
}
