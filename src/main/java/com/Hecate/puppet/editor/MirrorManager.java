package com.Hecate.puppet.editor;

import com.Hecate.puppet.editor.core.EditorBone;
import com.jme3.math.ColorRGBA;
import java.util.*;

/**
 * 镜像管理器
 * 管理骨骼之间的镜像关系，支持多对镜像配对
 */
public class MirrorManager {

    /**
     * 镜像轴枚举
     */
    public enum MirrorAxis {
        X, Y, Z
    }

    /**
     * 镜像对数据结构
     */
    public static class MirrorPair {
        public EditorBone bone1;
        public EditorBone bone2;
        public MirrorAxis axis;
        public ColorRGBA color;

        public MirrorPair(EditorBone bone1, EditorBone bone2, MirrorAxis axis, ColorRGBA color) {
            this.bone1 = bone1;
            this.bone2 = bone2;
            this.axis = axis;
            this.color = color;
        }

        /**
         * 检查给定的骨骼是否在这个镜像对中
         */
        public boolean contains(EditorBone bone) {
            return bone == bone1 || bone == bone2;
        }

        /**
         * 获取给定骨骼的镜像对象
         */
        public EditorBone getMirror(EditorBone bone) {
            if (bone == bone1) return bone2;
            if (bone == bone2) return bone1;
            return null;
        }
    }

    // 10种镜像颜色池（柔和且易于区分的颜色）
    private static final ColorRGBA[] MIRROR_COLORS = {
        new ColorRGBA(0.4f, 0.8f, 1.0f, 1.0f),   // 天蓝色
        new ColorRGBA(1.0f, 0.7f, 0.4f, 1.0f),   // 橙色
        new ColorRGBA(0.6f, 1.0f, 0.6f, 1.0f),   // 浅绿色
        new ColorRGBA(1.0f, 0.5f, 0.8f, 1.0f),   // 粉色
        new ColorRGBA(0.8f, 0.8f, 0.4f, 1.0f),   // 黄色
        new ColorRGBA(0.7f, 0.5f, 1.0f, 1.0f),   // 紫色
        new ColorRGBA(0.4f, 1.0f, 1.0f, 1.0f),   // 青色
        new ColorRGBA(1.0f, 0.6f, 0.6f, 1.0f),   // 浅红色
        new ColorRGBA(0.6f, 0.9f, 0.4f, 1.0f),   // 草绿色
        new ColorRGBA(0.9f, 0.6f, 1.0f, 1.0f)    // 淡紫色
    };

    // 存储所有镜像对
    private List<MirrorPair> mirrorPairs = new ArrayList<>();

    // 用于快速查找骨骼所属的镜像对
    private Map<EditorBone, MirrorPair> boneToMirrorMap = new HashMap<>();

    // 当前可用的颜色索引
    private int nextColorIndex = 0;

    /**
     * 添加镜像对
     * @param bone1 第一个骨骼
     * @param bone2 第二个骨骼
     * @param axis 镜像轴（默认X轴）
     * @return 是否成功添加
     */
    public boolean addMirrorPair(EditorBone bone1, EditorBone bone2, MirrorAxis axis) {
        if (bone1 == null || bone2 == null || bone1 == bone2) {
            return false;
        }

        // 检查这两个骨骼是否已经在其他镜像对中
        if (boneToMirrorMap.containsKey(bone1) || boneToMirrorMap.containsKey(bone2)) {
            return false;
        }

        // 获取下一个颜色
        ColorRGBA color = MIRROR_COLORS[nextColorIndex % MIRROR_COLORS.length];
        nextColorIndex++;

        // 创建镜像对
        MirrorPair pair = new MirrorPair(bone1, bone2, axis, color);
        mirrorPairs.add(pair);

        // 更新映射
        boneToMirrorMap.put(bone1, pair);
        boneToMirrorMap.put(bone2, pair);

        return true;
    }

    /**
     * 移除镜像对
     * @param bone 镜像对中的任意一个骨骼
     * @return 是否成功移除
     */
    public boolean removeMirrorPair(EditorBone bone) {
        MirrorPair pair = boneToMirrorMap.get(bone);
        if (pair == null) {
            return false;
        }

        // 从映射中移除
        boneToMirrorMap.remove(pair.bone1);
        boneToMirrorMap.remove(pair.bone2);

        // 从列表中移除
        mirrorPairs.remove(pair);

        return true;
    }

    /**
     * 获取骨骼的镜像对象
     */
    public EditorBone getMirrorBone(EditorBone bone) {
        MirrorPair pair = boneToMirrorMap.get(bone);
        if (pair == null) {
            return null;
        }
        return pair.getMirror(bone);
    }

    /**
     * 获取骨骼所属的镜像对
     */
    public MirrorPair getMirrorPair(EditorBone bone) {
        return boneToMirrorMap.get(bone);
    }

    /**
     * 检查骨骼是否在某个镜像对中
     */
    public boolean hasMirror(EditorBone bone) {
        return boneToMirrorMap.containsKey(bone);
    }

    /**
     * 获取骨骼的镜像颜色
     */
    public ColorRGBA getMirrorColor(EditorBone bone) {
        MirrorPair pair = boneToMirrorMap.get(bone);
        if (pair == null) {
            return null;
        }
        return pair.color;
    }

    /**
     * 设置镜像对的镜像轴
     */
    public boolean setMirrorAxis(EditorBone bone, MirrorAxis axis) {
        MirrorPair pair = boneToMirrorMap.get(bone);
        if (pair == null) {
            return false;
        }
        pair.axis = axis;
        return true;
    }

    /**
     * 获取所有镜像对
     */
    public List<MirrorPair> getAllMirrorPairs() {
        return new ArrayList<>(mirrorPairs);
    }

    /**
     * 清除所有镜像关系
     */
    public void clearAll() {
        mirrorPairs.clear();
        boneToMirrorMap.clear();
        nextColorIndex = 0;
    }

    /**
     * 获取镜像对的数量
     */
    public int getMirrorPairCount() {
        return mirrorPairs.size();
    }
}
