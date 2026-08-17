package com.Hecate.puppet.config;

import java.util.*;

/**
 * 动画配置文件格式
 * 只包含骨骼层级和关键帧数据，不包含纹理和渲染信息
 */
public class AnimationConfig {

    private String name;                    // 动画名称
    private float duration;                 // 总时长（秒）
    private boolean looping;                // 是否循环
    private List<BoneHierarchy> bones;      // 骨骼层级结构
    private List<KeyframeData> keyframes;   // 所有关键帧数据

    public AnimationConfig() {
        this.bones = new ArrayList<>();
        this.keyframes = new ArrayList<>();
        this.looping = true;
    }

    public AnimationConfig(String name) {
        this();
        this.name = name;
    }

    /**
     * 骨骼层级信息
     * 用于验证骨骼结构
     */
    public static class BoneHierarchy {
        private String name;
        private String parentName;  // null表示根骨骼
        private int index;          // 骨骼在层级中的索引

        public BoneHierarchy() {}

        public BoneHierarchy(String name, String parentName, int index) {
            this.name = name;
            this.parentName = parentName;
            this.index = index;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getParentName() {
            return parentName;
        }

        public void setParentName(String parentName) {
            this.parentName = parentName;
        }

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }
    }

    /**
     * 关键帧数据
     */
    public static class KeyframeData {
        private float time;
        private String boneName;
        private Vec3Config position;
        private QuatConfig rotation;
        private Vec3Config scale;
        private float width;
        private float height;
        private float customRotationX;
        private float customRotationZ;
        private float textureRotation;  // 贴图旋转角度（度数，向后兼容）
        private java.util.Map<String, Float> directionTextureRotations;  // 多方向贴图旋转
        private String type;  // KeyframeType: "INTERPOLATED" or "SNAPSHOT"

        public KeyframeData() {}

        public KeyframeData(float time, String boneName) {
            this.time = time;
            this.boneName = boneName;
        }

        // Getters and Setters
        public float getTime() {
            return time;
        }

        public void setTime(float time) {
            this.time = time;
        }

        public String getBoneName() {
            return boneName;
        }

        public void setBoneName(String boneName) {
            this.boneName = boneName;
        }

        public Vec3Config getPosition() {
            return position;
        }

        public void setPosition(Vec3Config position) {
            this.position = position;
        }

        public QuatConfig getRotation() {
            return rotation;
        }

        public void setRotation(QuatConfig rotation) {
            this.rotation = rotation;
        }

        public Vec3Config getScale() {
            return scale;
        }

        public void setScale(Vec3Config scale) {
            this.scale = scale;
        }

        public float getWidth() {
            return width;
        }

        public void setWidth(float width) {
            this.width = width;
        }

        public float getHeight() {
            return height;
        }

        public void setHeight(float height) {
            this.height = height;
        }

        public float getCustomRotationX() {
            return customRotationX;
        }

        public void setCustomRotationX(float customRotationX) {
            this.customRotationX = customRotationX;
        }

        public float getCustomRotationZ() {
            return customRotationZ;
        }

        public void setCustomRotationZ(float customRotationZ) {
            this.customRotationZ = customRotationZ;
        }

        public float getTextureRotation() {
            return textureRotation;
        }

        public void setTextureRotation(float textureRotation) {
            this.textureRotation = textureRotation;
        }

        public java.util.Map<String, Float> getDirectionTextureRotations() {
            return directionTextureRotations;
        }

        public void setDirectionTextureRotations(java.util.Map<String, Float> directionTextureRotations) {
            this.directionTextureRotations = directionTextureRotations;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }

    // ========== Getters and Setters ==========

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public float getDuration() {
        return duration;
    }

    public void setDuration(float duration) {
        this.duration = duration;
    }

    public boolean isLooping() {
        return looping;
    }

    public void setLooping(boolean looping) {
        this.looping = looping;
    }

    public List<BoneHierarchy> getBones() {
        return bones;
    }

    public void setBones(List<BoneHierarchy> bones) {
        this.bones = bones;
    }

    public List<KeyframeData> getKeyframes() {
        return keyframes;
    }

    public void setKeyframes(List<KeyframeData> keyframes) {
        this.keyframes = keyframes;
    }

    /**
     * 获取所有涉及的骨骼名称
     */
    public Set<String> getAllBoneNames() {
        Set<String> names = new HashSet<>();
        for (BoneHierarchy bone : bones) {
            names.add(bone.getName());
        }
        return names;
    }

    @Override
    public String toString() {
        return String.format("AnimationConfig[name=%s, duration=%.2fs, bones=%d, keyframes=%d]",
                name, duration, bones.size(), keyframes.size());
    }
}
