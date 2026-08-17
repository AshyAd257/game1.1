package com.Hecate.puppet.editor;

import com.jme3.math.Vector3f;
import com.jme3.math.Quaternion;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 骨骼剪贴板数据
 * 用于复制/粘贴部件
 */
public class BoneClipboardData {

    /**
     * 单个骨骼的数据
     */
    public static class BoneData {
        public String name;
        public Vector3f localPosition;
        public Quaternion localRotation;
        public Vector3f localScale;
        public int priority;  // 骨骼优先级（影响渲染顺序）

        // 渲染器数据
        public float width;
        public float height;
        public Vector3f offset;
        public float customRotationX;
        public float customRotationY;  // Y轴旋转
        public float customRotationZ;
        public String texturePath;

        // UV坐标数据
        public float uvOffsetX;
        public float uvOffsetY;
        public float uvScaleX;
        public float uvScaleY;

        // 方向纹理映射 (direction key -> texture path)
        public Map<String, String> directionTextures;

        // 方向尺寸映射 (direction key -> width/height)
        public Map<String, Float> directionWidths;
        public Map<String, Float> directionHeights;

        // 子骨骼
        public List<BoneData> children;

        public BoneData() {
            children = new ArrayList<>();
            directionTextures = new HashMap<>();
            directionWidths = new HashMap<>();
            directionHeights = new HashMap<>();
        }

        /**
         * 克隆骨骼数据（深拷贝）
         */
        public BoneData clone() {
            BoneData copy = new BoneData();
            copy.name = this.name;
            copy.localPosition = this.localPosition.clone();
            copy.localRotation = this.localRotation.clone();
            copy.localScale = this.localScale.clone();
            copy.priority = this.priority;
            copy.width = this.width;
            copy.height = this.height;
            copy.offset = this.offset.clone();
            copy.customRotationX = this.customRotationX;
            copy.customRotationY = this.customRotationY;
            copy.customRotationZ = this.customRotationZ;
            copy.texturePath = this.texturePath;

            // 复制UV坐标
            copy.uvOffsetX = this.uvOffsetX;
            copy.uvOffsetY = this.uvOffsetY;
            copy.uvScaleX = this.uvScaleX;
            copy.uvScaleY = this.uvScaleY;

            // 复制方向纹理映射
            copy.directionTextures = new HashMap<>(this.directionTextures);

            // 复制方向尺寸映射
            if (this.directionWidths != null) {
                copy.directionWidths = new HashMap<>(this.directionWidths);
            }
            if (this.directionHeights != null) {
                copy.directionHeights = new HashMap<>(this.directionHeights);
            }

            // 递归克隆子骨骼
            for (BoneData child : this.children) {
                copy.children.add(child.clone());
            }

            return copy;
        }

        /**
         * 镜像骨骼数据（翻转X坐标）
         */
        public BoneData mirror() {
            BoneData mirrored = clone();

            // 翻转X位置
            mirrored.localPosition.x = -mirrored.localPosition.x;

            // 翻转X偏移
            mirrored.offset.x = -mirrored.offset.x;

            // 翻转Y和Z旋转（保持X旋转）
            float[] angles = mirrored.localRotation.toAngles(null);
            mirrored.localRotation.fromAngles(angles[0], -angles[1], -angles[2]);

            // 翻转自定义旋转Z
            mirrored.customRotationZ = -mirrored.customRotationZ;

            // 尝试智能重命名（Left <-> Right）
            if (mirrored.name.contains("Left")) {
                mirrored.name = mirrored.name.replace("Left", "Right");
            } else if (mirrored.name.contains("Right")) {
                mirrored.name = mirrored.name.replace("Right", "Left");
            } else if (mirrored.name.contains("L")) {
                mirrored.name = mirrored.name.replace("L", "R");
            } else if (mirrored.name.contains("R")) {
                mirrored.name = mirrored.name.replace("R", "L");
            } else {
                // 如果没有Left/Right标记，添加_Mirror后缀
                mirrored.name = mirrored.name + "_Mirror";
            }

            // 递归镜像子骨骼
            mirrored.children.clear();
            for (BoneData child : this.children) {
                mirrored.children.add(child.mirror());
            }

            return mirrored;
        }
    }

    private BoneData rootBoneData;

    public BoneClipboardData(BoneData rootBoneData) {
        this.rootBoneData = rootBoneData;
    }

    public BoneData getRootBoneData() {
        return rootBoneData;
    }

    /**
     * 获取镜像的剪贴板数据
     */
    public BoneClipboardData getMirroredData() {
        return new BoneClipboardData(rootBoneData.mirror());
    }
}
