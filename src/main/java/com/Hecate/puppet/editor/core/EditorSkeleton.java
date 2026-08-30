package com.Hecate.puppet.editor.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 编辑器专用骨骼树
 * 管理整个木偶的骨骼结构
 *
 * 注意：这是编辑器专用版本，与游戏运行时的Skeleton类独立
 */
public class EditorSkeleton {

    private final String name;
    private EditorBone rootBone;
    private final Map<String, EditorBone> boneMap;
    private final List<EditorBone> allBones;
    private final EditorGroupManager groupManager;  // 骨骼分组管理器

    public EditorSkeleton(String name) {
        this.name = name;
        this.boneMap = new HashMap<>();
        this.allBones = new ArrayList<>();
        this.groupManager = new EditorGroupManager(this);  // 创建分组管理器
    }

    /**
     * 设置根骨骼
     */
    public void setRootBone(EditorBone root) {
        this.rootBone = root;
        rebuildBoneList();
    }

    /**
     * 根据名称查找骨骼
     */
    public EditorBone findBone(String boneName) {
        return boneMap.get(boneName);
    }

    /**
     * 添加骨骼到索引
     */
    public void addBone(EditorBone bone) {
        boneMap.put(bone.getName(), bone);
        if (!allBones.contains(bone)) {
            allBones.add(bone);
        }
    }

    /**
     * 移除骨骼
     */
    public void removeBone(String boneName) {
        EditorBone bone = boneMap.get(boneName);
        if (bone == null) {
            return;
        }

        // 从父骨骼的子列表中移除
        EditorBone parent = bone.getParent();
        if (parent != null) {
            parent.removeChild(bone);
        }

        // 从索引中移除
        boneMap.remove(boneName);
        allBones.remove(bone);
    }

    /**
     * 清空所有骨骼
     */
    public void clear() {
        rootBone = null;
        boneMap.clear();
        allBones.clear();
    }

    /**
     * 重命名骨骼
     * 注意：这只更新索引，不会修改Bone对象本身的名称（因为name是final）
     * 需要创建新的Bone对象来真正重命名
     */
    public void updateBoneIndex(String oldName, String newName, EditorBone bone) {
        if (boneMap.containsKey(oldName)) {
            boneMap.remove(oldName);
            boneMap.put(newName, bone);
        }
    }

    /**
     * 直接更新根骨骼引用，不触发rebuildBoneList
     * 用于重命名等场景，避免丢失不在骨骼树中的独立骨骼
     */
    public void updateRootBoneReference(EditorBone newRoot) {
        this.rootBone = newRoot;
    }

    /**
     * 重建骨骼列表（深度优先遍历）
     */
    private void rebuildBoneList() {
        boneMap.clear();
        allBones.clear();

        if (rootBone != null) {
            collectBonesRecursive(rootBone);
        }
    }

    /**
     * 递归收集所有骨骼
     */
    private void collectBonesRecursive(EditorBone bone) {
        boneMap.put(bone.getName(), bone);
        allBones.add(bone);

        for (EditorBone child : bone.getChildren()) {
            collectBonesRecursive(child);
        }
    }

    /**
     * 重置所有骨骼到Rest姿势
     */
    public void resetToRestPose() {
        for (EditorBone bone : allBones) {
            bone.resetToRestPose();
        }
    }

    /**
     * 更新所有骨骼的变换矩阵
     * 注意：Bone使用延迟计算，世界变换在getWorldTransform()时按需计算
     */
    public void updateTransforms() {
        // Bone类使用延迟计算策略，无需显式更新
        // 世界变换会在需要时通过getWorldTransform()递归计算
    }

    /**
     * 获取所有骨骼列表（按层级顺序）
     */
    public List<EditorBone> getAllBones() {
        return new ArrayList<>(allBones);
    }

    /**
     * 获取骨骼数量
     */
    public int getBoneCount() {
        return allBones.size();
    }

    /**
     * 转换为基础 Skeleton 类型（用于导出）
     * 将 EditorSkeleton 转换为游戏运行时的 Skeleton
     */
    public com.Hecate.puppet.core.Skeleton getBaseSkeleton() {
        com.Hecate.puppet.core.Skeleton skeleton = new com.Hecate.puppet.core.Skeleton(this.name);

        if (rootBone != null) {
            // 递归转换骨骼树
            com.Hecate.puppet.core.Bone baseBone = convertBone(rootBone);
            skeleton.setRootBone(baseBone);
        }

        return skeleton;
    }

    /**
     * 递归转换 EditorBone 为 Bone
     */
    private com.Hecate.puppet.core.Bone convertBone(EditorBone editorBone) {
        com.Hecate.puppet.core.Bone bone = new com.Hecate.puppet.core.Bone(editorBone.getName());

        // 复制基本变换属性
        bone.setLocalPosition(editorBone.getLocalPosition().clone());
        bone.setLocalRotation(editorBone.getLocalRotation().clone());
        bone.setLocalScale(editorBone.getLocalScale().clone());

        // 复制Rest姿势
        bone.setRestPosition(editorBone.getRestPosition().clone());
        bone.setRestRotation(editorBone.getRestRotation().clone());
        bone.setRestScale(editorBone.getRestScale().clone());

        // 复制纹理路径
        if (editorBone.getTexturePath() != null) {
            bone.setTexturePath(editorBone.getTexturePath());
        }

        // 复制多方向贴图
        if (editorBone.getDirectionTextures() != null && !editorBone.getDirectionTextures().isEmpty()) {
            bone.setDirectionTextures(new HashMap<>(editorBone.getDirectionTextures()));
        }
        if (editorBone.getCurrentDirection() != null) {
            bone.setCurrentDirection(editorBone.getCurrentDirection());
        }

        // 复制优先级
        bone.setPriority(editorBone.getPriority());

        // 复制多方向优先级
        if (editorBone.getDirectionPriorities() != null && !editorBone.getDirectionPriorities().isEmpty()) {
            bone.setDirectionPriorities(new HashMap<>(editorBone.getDirectionPriorities()));
        }

        // 复制多方向UV坐标
        if (editorBone.getDirectionUVs() != null && !editorBone.getDirectionUVs().isEmpty()) {
            bone.setDirectionUVs(new HashMap<>(editorBone.getDirectionUVs()));
        }

        // 复制多方向尺寸
        if (editorBone.getDirectionWidths() != null && !editorBone.getDirectionWidths().isEmpty()) {
            bone.setDirectionWidths(new HashMap<>(editorBone.getDirectionWidths()));
        }
        if (editorBone.getDirectionHeights() != null && !editorBone.getDirectionHeights().isEmpty()) {
            bone.setDirectionHeights(new HashMap<>(editorBone.getDirectionHeights()));
        }

        // 复制多方向位置偏移
        if (editorBone.getDirectionOffsets() != null && !editorBone.getDirectionOffsets().isEmpty()) {
            bone.setDirectionOffsets(new HashMap<>(editorBone.getDirectionOffsets()));
        }

        // 复制多方向旋转
        if (editorBone.getDirectionRotations() != null && !editorBone.getDirectionRotations().isEmpty()) {
            bone.setDirectionRotations(new HashMap<>(editorBone.getDirectionRotations()));
        }

        // 复制多方向贴图旋转
        if (editorBone.getDirectionTextureRotations() != null && !editorBone.getDirectionTextureRotations().isEmpty()) {
            bone.setDirectionTextureRotations(new HashMap<>(editorBone.getDirectionTextureRotations()));
        }

        // 复制Billboard启用状态
        bone.setBillboardEnabled(editorBone.isBillboardEnabled());

        // 复制多方向贴图模式
        bone.setMultiDirectionTextureEnabled(editorBone.isMultiDirectionTextureEnabled());

        // 复制自由骨骼系统属性
        try {
            com.Hecate.puppet.core.Bone.BoneType boneType = com.Hecate.puppet.core.Bone.BoneType.valueOf(editorBone.getBoneType().name());
            bone.setBoneType(boneType);
        } catch (IllegalArgumentException e) {
            bone.setBoneType(com.Hecate.puppet.core.Bone.BoneType.CONNECTED);
        }

        try {
            com.Hecate.puppet.core.Bone.GravityDirection gravityDir = com.Hecate.puppet.core.Bone.GravityDirection.valueOf(editorBone.getGravityDirection().name());
            bone.setGravityDirection(gravityDir);
        } catch (IllegalArgumentException e) {
            bone.setGravityDirection(com.Hecate.puppet.core.Bone.GravityDirection.DOWN);
        }

        bone.setCustomGravityVector(editorBone.getCustomGravityVector().clone());
        bone.setFreedomValue(editorBone.getFreedomValue());

        // 复制旋转条状贴图系统属性（这里之前一直缺失——导出/预览为运行时Bone时
        // 旋转条状贴图部件会丢失所有条状贴图配置，改用6方向系统渲染。补上避免
        // 加3D模型骨骼系统时又留下同样的坑）
        bone.setRotationStripEnabled(editorBone.isRotationStripEnabled());
        if (editorBone.getStripTexturePath() != null) {
            bone.setStripTexturePath(editorBone.getStripTexturePath());
        }
        bone.setStripSteps(editorBone.getStripSteps());
        bone.setStripFrameWidthPx(editorBone.getStripFrameWidthPx());
        bone.setStripFrameHeightPx(editorBone.getStripFrameHeightPx());
        bone.setStripCalibrationOffsetPx(editorBone.getStripCalibrationOffsetPx());
        bone.setBillboardPitchClampUpDeg(editorBone.getBillboardPitchClampUpDeg());
        bone.setBillboardPitchClampDownDeg(editorBone.getBillboardPitchClampDownDeg());
        bone.setStripWidth(editorBone.getStripWidth());
        bone.setStripHeight(editorBone.getStripHeight());
        bone.setStripOffset(
            editorBone.getStripOffset().x,
            editorBone.getStripOffset().y,
            editorBone.getStripOffset().z
        );
        bone.setStripRotation(
            editorBone.getStripRotationX(),
            editorBone.getStripRotationY(),
            editorBone.getStripRotationZ()
        );
        bone.setStripPriority(editorBone.getStripPriority());

        // 复制3D模型骨骼系统属性（新增）
        bone.setModelEnabled(editorBone.isModelEnabled());
        if (editorBone.getModelFilePath() != null) {
            bone.setModelFilePath(editorBone.getModelFilePath());
        }
        bone.setModelRotation(
            editorBone.getModelRotationX(),
            editorBone.getModelRotationY(),
            editorBone.getModelRotationZ()
        );
        bone.setModelScale(editorBone.getModelScale());

        // 递归转换子骨骼
        for (EditorBone child : editorBone.getChildren()) {
            com.Hecate.puppet.core.Bone childBone = convertBone(child);
            bone.addChild(childBone);
        }

        return bone;
    }

    // ========== Getters ==========

    public String getName() {
        return name;
    }

    public EditorBone getRootBone() {
        return rootBone;
    }

    public EditorGroupManager getGroupManager() {
        return groupManager;
    }
}
