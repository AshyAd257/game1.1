package com.Hecate.puppet.animation;

import java.util.HashSet;
import java.util.Set;
import java.util.Arrays;

/**
 * 骨骼遮罩 - 定义动画影响哪些骨骼
 * 用于实现部分骨骼动画和动画分层
 */
public class BoneMask {

    private Set<String> affectedBones;
    private String name;

    /**
     * 创建空的骨骼遮罩
     */
    public BoneMask(String name) {
        this.name = name;
        this.affectedBones = new HashSet<>();
    }

    /**
     * 创建包含指定骨骼的遮罩
     */
    public BoneMask(String name, String... boneNames) {
        this.name = name;
        this.affectedBones = new HashSet<>(Arrays.asList(boneNames));
    }

    /**
     * 创建包含指定骨骼集合的遮罩
     */
    public BoneMask(String name, Set<String> boneNames) {
        this.name = name;
        this.affectedBones = new HashSet<>(boneNames);
    }

    /**
     * 添加受影响的骨骼
     */
    public void addBone(String boneName) {
        affectedBones.add(boneName);
    }

    /**
     * 移除骨骼
     */
    public void removeBone(String boneName) {
        affectedBones.remove(boneName);
    }

    /**
     * 检查指定骨骼是否受此遮罩影响
     */
    public boolean affects(String boneName) {
        return affectedBones.contains(boneName);
    }

    /**
     * 获取所有受影响的骨骼名称
     */
    public Set<String> getAffectedBones() {
        return new HashSet<>(affectedBones);
    }

    /**
     * 获取遮罩名称
     */
    public String getName() {
        return name;
    }

    /**
     * 设置遮罩名称
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 检查遮罩是否为空
     */
    public boolean isEmpty() {
        return affectedBones.isEmpty();
    }

    /**
     * 获取受影响骨骼数量
     */
    public int size() {
        return affectedBones.size();
    }

    /**
     * 清空遮罩
     */
    public void clear() {
        affectedBones.clear();
    }

    // ==================== 预设遮罩工厂方法 ====================

    /**
     * 创建上半身遮罩（头部、躯干、手臂）
     */
    public static BoneMask createUpperBody() {
        BoneMask mask = new BoneMask("上半身");
        mask.addBone("Head");
        mask.addBone("Body");
        mask.addBone("LeftUpperArm");
        mask.addBone("LeftLowerArm");
        mask.addBone("LeftHand");
        mask.addBone("RightUpperArm");
        mask.addBone("RightLowerArm");
        mask.addBone("RightHand");
        return mask;
    }

    /**
     * 创建下半身遮罩（腿部）
     */
    public static BoneMask createLowerBody() {
        BoneMask mask = new BoneMask("下半身");
        mask.addBone("LeftUpperLeg");
        mask.addBone("LeftLowerLeg");
        mask.addBone("LeftFoot");
        mask.addBone("RightUpperLeg");
        mask.addBone("RightLowerLeg");
        mask.addBone("RightFoot");
        return mask;
    }

    /**
     * 创建左侧遮罩（左手臂、左腿）
     */
    public static BoneMask createLeftSide() {
        BoneMask mask = new BoneMask("左侧");
        mask.addBone("LeftUpperArm");
        mask.addBone("LeftLowerArm");
        mask.addBone("LeftHand");
        mask.addBone("LeftUpperLeg");
        mask.addBone("LeftLowerLeg");
        mask.addBone("LeftFoot");
        return mask;
    }

    /**
     * 创建右侧遮罩（右手臂、右腿）
     */
    public static BoneMask createRightSide() {
        BoneMask mask = new BoneMask("右侧");
        mask.addBone("RightUpperArm");
        mask.addBone("RightLowerArm");
        mask.addBone("RightHand");
        mask.addBone("RightUpperLeg");
        mask.addBone("RightLowerLeg");
        mask.addBone("RightFoot");
        return mask;
    }

    /**
     * 创建全身遮罩（影响所有骨骼）
     */
    public static BoneMask createAll() {
        BoneMask mask = new BoneMask("全身");
        // 空遮罩在 AnimationLayer 中会被特殊处理为"影响所有骨骼"
        return mask;
    }

    /**
     * 创建自定义遮罩（用户手动选择骨骼）
     */
    public static BoneMask createCustom(String name) {
        return new BoneMask(name);
    }

    @Override
    public String toString() {
        return name + " (" + affectedBones.size() + " bones)";
    }
}
