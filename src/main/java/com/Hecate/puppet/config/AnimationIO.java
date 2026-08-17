package com.Hecate.puppet.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.Hecate.puppet.core.Bone;
import com.Hecate.puppet.core.Skeleton;
import com.Hecate.puppet.core.PuppetPartRenderer;
import com.Hecate.puppet.animation.AnimationClip;
import com.Hecate.puppet.animation.Keyframe;

import java.io.*;
import java.util.*;

/**
 * 动画文件输入/输出工具
 * 负责导出和导入纯动画数据（不包含纹理）
 */
public class AnimationIO {

    private static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(Keyframe.KeyframeType.class, new TypeAdapter<Keyframe.KeyframeType>() {
                @Override
                public void write(JsonWriter out, Keyframe.KeyframeType value) throws IOException {
                    if (value == null) {
                        out.nullValue();
                    } else {
                        out.value(value.name());
                    }
                }

                @Override
                public Keyframe.KeyframeType read(JsonReader in) throws IOException {
                    String value = in.nextString();
                    try {
                        return Keyframe.KeyframeType.valueOf(value);
                    } catch (IllegalArgumentException e) {
                        return Keyframe.KeyframeType.INTERPOLATED; // 默认值
                    }
                }
            })
            .create();

    /**
     * 从AnimationClip和Skeleton导出动画配置
     */
    public static AnimationConfig exportAnimation(AnimationClip clip, Skeleton skeleton) {
        AnimationConfig config = new AnimationConfig(clip.getName());
        config.setDuration(clip.getDuration());
        config.setLooping(clip.isLooping());

        // 导出骨骼层级结构
        List<Bone> allBones = skeleton.getAllBones();
        int index = 0;
        for (Bone bone : allBones) {
            AnimationConfig.BoneHierarchy boneHierarchy = new AnimationConfig.BoneHierarchy(
                bone.getName(),
                bone.getParent() != null ? bone.getParent().getName() : null,
                index++
            );
            config.getBones().add(boneHierarchy);
        }

        // 导出所有关键帧
        List<Keyframe> allKeyframes = clip.getAllKeyframes();
        for (Keyframe kf : allKeyframes) {
            AnimationConfig.KeyframeData kfData = new AnimationConfig.KeyframeData();
            kfData.setTime(kf.getTime());
            kfData.setBoneName(kf.getBoneName());
            kfData.setPosition(new Vec3Config(kf.getPosition()));
            kfData.setRotation(new QuatConfig(kf.getRotation()));
            kfData.setScale(new Vec3Config(kf.getScale()));
            kfData.setWidth(kf.getWidth());
            kfData.setHeight(kf.getHeight());
            kfData.setCustomRotationX(kf.getCustomRotationX());
            kfData.setCustomRotationZ(kf.getCustomRotationZ());
            kfData.setTextureRotation(kf.getTextureRotation());
            kfData.setDirectionTextureRotations(kf.getDirectionTextureRotations());  // Export direction rotations
            kfData.setType(kf.getType().name());  // Export KeyframeType as string

            config.getKeyframes().add(kfData);
        }

        return config;
    }

    /**
     * 保存动画配置到文件
     */
    public static void saveAnimation(AnimationConfig config, String filePath) throws IOException {
        try (Writer writer = new FileWriter(filePath)) {
            gson.toJson(config, writer);
        }
    }

    /**
     * 从文件加载动画配置
     */
    public static AnimationConfig loadAnimation(String filePath) throws IOException {
        try (Reader reader = new FileReader(filePath)) {
            return gson.fromJson(reader, AnimationConfig.class);
        }
    }

    /**
     * 将动画配置应用到AnimationClip（使用映射）
     */
    public static AnimationClip applyAnimation(AnimationConfig config, BoneMappingConfig mapping) {
        AnimationClip clip = new AnimationClip(config.getName());
        clip.setDuration(config.getDuration());
        clip.setLooping(config.isLooping());

        // 应用关键帧（使用映射）
        for (AnimationConfig.KeyframeData kfData : config.getKeyframes()) {
            String animBoneName = kfData.getBoneName();
            String puppetBoneName = mapping.getMappedBoneName(animBoneName);

            if (puppetBoneName != null) {
                // 创建关键帧并使用映射后的骨骼名
                Keyframe kf = new Keyframe(kfData.getTime(), puppetBoneName);
                kf.setPosition(kfData.getPosition().toVector3f());
                kf.setRotation(kfData.getRotation().toQuaternion());
                kf.setScale(kfData.getScale().toVector3f());
                kf.setWidth(kfData.getWidth());
                kf.setHeight(kfData.getHeight());
                kf.setCustomRotationX(kfData.getCustomRotationX());
                kf.setCustomRotationZ(kfData.getCustomRotationZ());
                kf.setTextureRotation(kfData.getTextureRotation());

                // Load direction texture rotations (new feature)
                if (kfData.getDirectionTextureRotations() != null) {
                    kf.setDirectionTextureRotations(kfData.getDirectionTextureRotations());
                }

                // Restore KeyframeType (default to INTERPOLATED for backward compatibility)
                if (kfData.getType() != null) {
                    try {
                        Keyframe.KeyframeType type = Keyframe.KeyframeType.valueOf(kfData.getType());
                        kf.setType(type);
                    } catch (IllegalArgumentException e) {
                        // Invalid type string, default to INTERPOLATED
                        kf.setType(Keyframe.KeyframeType.INTERPOLATED);
                    }
                } else {
                    // No type specified, default to INTERPOLATED
                    kf.setType(Keyframe.KeyframeType.INTERPOLATED);
                }

                clip.addKeyframe(kf);
            }
        }

        return clip;
    }

    /**
     * 保存映射配置到文件
     */
    public static void saveMappingConfig(BoneMappingConfig config, String filePath) throws IOException {
        try (Writer writer = new FileWriter(filePath)) {
            gson.toJson(config, writer);
        }
    }

    /**
     * 从文件加载映射配置
     */
    public static BoneMappingConfig loadMappingConfig(String filePath) throws IOException {
        try (Reader reader = new FileReader(filePath)) {
            return gson.fromJson(reader, BoneMappingConfig.class);
        }
    }

    /**
     * 验证映射完整性
     */
    public static boolean validateMapping(AnimationConfig animConfig, BoneMappingConfig mapping, Skeleton skeleton) {
        Set<String> animBones = animConfig.getAllBoneNames();
        Set<String> puppetBones = new HashSet<>();
        for (Bone bone : skeleton.getAllBones()) {
            puppetBones.add(bone.getName());
        }

        // 检查所有映射的骨骼是否存在
        for (String animBone : animBones) {
            String puppetBone = mapping.getMappedBoneName(animBone);
            if (puppetBone != null && !puppetBones.contains(puppetBone)) {
                System.err.println("[AnimationIO] 映射错误：木偶中不存在骨骼 '" + puppetBone + "'");
                return false;
            }
        }

        return true;
    }

    /**
     * 获取映射统计信息
     */
    public static String getMappingStatistics(AnimationConfig animConfig, BoneMappingConfig mapping) {
        Set<String> animBones = animConfig.getAllBoneNames();
        int totalBones = animBones.size();
        int mappedBones = 0;

        for (String animBone : animBones) {
            if (mapping.hasMapping(animBone)) {
                mappedBones++;
            }
        }

        float coverage = mapping.getMappingCoverage(animBones);
        return String.format("映射覆盖率: %d/%d (%.1f%%)", mappedBones, totalBones, coverage);
    }

    /**
     * 从AnimationClip和EditorSkeleton导出动画配置（编辑器版本）
     */
    public static AnimationConfig exportAnimation(AnimationClip clip,
                                                  com.Hecate.puppet.editor.core.EditorSkeleton editorSkeleton) {
        // 转换为基础类型
        Skeleton skeleton = editorSkeleton.getBaseSkeleton();

        // 调用原始方法
        return exportAnimation(clip, skeleton);
    }

    /**
     * 验证映射完整性（编辑器版本）
     */
    public static boolean validateMapping(AnimationConfig animConfig,
                                          BoneMappingConfig mapping,
                                          com.Hecate.puppet.editor.core.EditorSkeleton editorSkeleton) {
        // 转换为基础类型
        Skeleton skeleton = editorSkeleton.getBaseSkeleton();

        // 调用原始方法
        return validateMapping(animConfig, mapping, skeleton);
    }
}
