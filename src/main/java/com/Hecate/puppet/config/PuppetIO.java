package com.Hecate.puppet.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.Hecate.puppet.core.Bone;
import com.Hecate.puppet.core.Skeleton;
import com.Hecate.puppet.core.PuppetRenderer;
import com.Hecate.puppet.core.PuppetPartRenderer;
import com.Hecate.puppet.animation.Keyframe;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 木偶配置文件输入/输出工具
 * 负责保存和加载木偶配置
 */
public class PuppetIO {

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
                        return Keyframe.KeyframeType.INTERPOLATED;
                    }
                }
            })
            .create();

    /**
     * 从骨架和渲染器创建配置
     */
    public static PuppetConfig createConfig(Skeleton skeleton, com.Hecate.puppet.core.PuppetRenderer renderer) {
        PuppetConfig config = new PuppetConfig(skeleton.getName());

        // 保存Billboard渲染模式
        config.setBillboardMode(renderer.getBillboardMode().name());

        // 收集所有骨骼
        List<Bone> allBones = skeleton.getAllBones();

        for (Bone bone : allBones) {
            BoneConfig boneConfig = new BoneConfig(bone.getName());

            // 设置父骨骼名称
            if (bone.getParent() != null) {
                boneConfig.setParentName(bone.getParent().getName());
            }

            // Rest Pose
            boneConfig.setRestPosition(new Vec3Config(bone.getRestPosition()));
            boneConfig.setRestRotation(new QuatConfig(bone.getRestRotation()));
            boneConfig.setRestScale(new Vec3Config(bone.getRestScale()));

            // Current Pose
            boneConfig.setCurrentPosition(new Vec3Config(bone.getLocalPosition()));
            boneConfig.setCurrentRotation(new QuatConfig(bone.getLocalRotation()));
            boneConfig.setCurrentScale(new Vec3Config(bone.getLocalScale()));

            // 自由骨骼系统属性
            boneConfig.setBoneType(bone.getBoneType().name());
            boneConfig.setGravityDirection(bone.getGravityDirection().name());
            boneConfig.setCustomGravityVector(new Vec3Config(bone.getCustomGravityVector()));
            boneConfig.setFreedomValue(bone.getFreedomValue());

            // 保存Camera Follow相机跟随自由度（Live2D风格效果）
            boneConfig.setCameraFollowFreedomX(bone.getCameraFollowFreedomX());
            boneConfig.setCameraFollowFreedomY(bone.getCameraFollowFreedomY());

            // 保存FreeBonePhysics物理参数
            boneConfig.setMass(bone.getPhysMass());
            boneConfig.setDamping(bone.getPhysDamping());
            boneConfig.setStiffness(bone.getPhysStiffness());
            boneConfig.setGravityStrength(bone.getPhysGravityStrength());
            boneConfig.setMaxSwingAngle(bone.getPhysMaxSwingAngle());
            boneConfig.setMaxVelocity(bone.getPhysMaxVelocity());

            // 贴图模式配置
            boneConfig.setMultiDirectionTextureEnabled(bone.isMultiDirectionTextureEnabled());

            // 部件配置
            PuppetPartRenderer partRenderer = renderer.getPartRenderer(bone.getName());
            if (partRenderer != null) {
                PartConfig partConfig = new PartConfig();
                partConfig.setWidth(partRenderer.getWidth());
                partConfig.setHeight(partRenderer.getHeight());
                partConfig.setOffset(new Vec3Config(partRenderer.getOffset()));
                partConfig.setCustomRotationX(partRenderer.getCustomRotationX());
                partConfig.setCustomRotationZ(partRenderer.getCustomRotationZ());
                partConfig.setPivotPoint(new Vec3Config(partRenderer.getPivotPoint()));

                // 保存纹理路径
                String texturePath = partRenderer.getTexturePath();
                partConfig.setTexturePath(texturePath);

                // 保存多方向贴图（原样保存，不做任何自动修正）
                Map<String, String> directionTextures = bone.getDirectionTextures();
                partConfig.setDirectionTextures(directionTextures);

                partConfig.setCurrentDirection(bone.getCurrentDirection());

                // 保存优先级
                partConfig.setPriority(bone.getPriority());

                // 保存多方向优先级（新增）
                partConfig.setDirectionPriorities(bone.getDirectionPriorities());

                // 保存多方向尺寸（新增）
                partConfig.setDirectionWidths(bone.getDirectionWidths());
                partConfig.setDirectionHeights(bone.getDirectionHeights());

                // 保存多方向位置偏移（新增）
                partConfig.setDirectionOffsets(bone.getDirectionOffsets());

                // 保存多方向旋转（新增）
                partConfig.setDirectionRotations(bone.getDirectionRotations());

                // 保存多方向贴图旋转（新增）
                partConfig.setDirectionTextureRotations(bone.getDirectionTextureRotations());

                // 保存UV坐标（向后兼容）
                partConfig.setUvOffsetX(partRenderer.getUvOffsetX());
                partConfig.setUvOffsetY(partRenderer.getUvOffsetY());
                partConfig.setUvScaleX(partRenderer.getUvScaleX());
                partConfig.setUvScaleY(partRenderer.getUvScaleY());

                // 保存多方向UV坐标（新增）
                partConfig.setDirectionUVs(bone.getDirectionUVs());

                // 保存Billboard启用状态（新增 - 每个部件独立控制2D/3D模式）
                partConfig.setBillboardEnabled(bone.isBillboardEnabled());

                // 保存旋转条状贴图配置（新增）
                partConfig.setRotationStripEnabled(bone.isRotationStripEnabled());
                partConfig.setStripTexturePath(bone.getStripTexturePath());
                partConfig.setStripSteps(bone.getStripSteps());
                partConfig.setStripFrameWidthPx(bone.getStripFrameWidthPx());
                partConfig.setStripFrameHeightPx(bone.getStripFrameHeightPx());

                // 保存Billboard俯仰角平滑过渡阈值（新增）
                partConfig.setBillboardPitchFullRangeDeg(bone.getBillboardPitchFullRangeDeg());
                partConfig.setBillboardPitchLockDeg(bone.getBillboardPitchLockDeg());

                // 保存旋转条状贴图专用变换数据（新增 - 单一值，不按方向分槎）
                partConfig.setStripWidth(bone.getStripWidth());
                partConfig.setStripHeight(bone.getStripHeight());
                partConfig.setStripOffset(new Vec3Config(bone.getStripOffset()));
                partConfig.setStripRotationX(bone.getStripRotationX());
                partConfig.setStripRotationY(bone.getStripRotationY());
                partConfig.setStripRotationZ(bone.getStripRotationZ());
                partConfig.setStripPriority(bone.getStripPriority());
                partConfig.setStripCalibrationOffsetPx(bone.getStripCalibrationOffsetPx());

                // 保存3D模型骨骼配置（新增）
                partConfig.setModelEnabled(bone.isModelEnabled());
                partConfig.setModelFilePath(bone.getModelFilePath());
                partConfig.setModelRotationX(bone.getModelRotationX());
                partConfig.setModelRotationY(bone.getModelRotationY());
                partConfig.setModelRotationZ(bone.getModelRotationZ());
                partConfig.setModelScale(bone.getModelScale());

                // TODO: 添加调试颜色支持

                boneConfig.setPartConfig(partConfig);
            }

            // 保存骨骼分组ID（新增）
            boneConfig.setGroupId(bone.getGroupId());

            config.addBone(boneConfig);
        }

        // 保存分组配置（新增）
        com.Hecate.puppet.core.GroupManager groupManager = skeleton.getGroupManager();
        if (groupManager != null) {
            for (java.util.Map.Entry<String, com.Hecate.puppet.core.BoneGroup> entry : groupManager.getGroupsMap().entrySet()) {
                String groupId = entry.getKey();
                com.Hecate.puppet.core.BoneGroup group = entry.getValue();

                GroupConfig groupConfig = new GroupConfig(groupId, group.getName());

                // 保存成员骨骼名称
                for (Bone member : group.getMembers()) {
                    groupConfig.addMemberBoneName(member.getName());
                }

                // 保存当前旋转角度
                groupConfig.setCurrentRotation(group.getCurrentRotation());

                config.addGroup(groupConfig);
            }
        }

        return config;
    }

    /**
     * 保存配置到文件
     */
    public static void saveToFile(PuppetConfig config, String filePath) throws IOException {
        try (Writer writer = new FileWriter(filePath)) {
            gson.toJson(config, writer);
        }
    }

    /**
     * 从文件加载配置
     */
    public static PuppetConfig loadFromFile(String filePath) throws IOException {
        try (Reader reader = new FileReader(filePath)) {
            return gson.fromJson(reader, PuppetConfig.class);
        }
    }

    /**
     * 从 classpath 资源加载 Puppet 配置
     * @param resourcePath 相对于 classpath 的资源路径，例如 "puppets/successv5.puppet"
     */
    public static PuppetConfig loadFromResource(String resourcePath) throws IOException {
        InputStream inputStream = PuppetIO.class.getClassLoader().getResourceAsStream(resourcePath);
        if (inputStream == null) {
            throw new IOException("资源未找到: " + resourcePath);
        }
        try (Reader reader = new InputStreamReader(inputStream)) {
            return gson.fromJson(reader, PuppetConfig.class);
        }
    }

    /**
     * 转换纹理路径为jME3兼容格式
     * 支持相对路径、绝对路径和file://协议
     */
    private static String convertTexturePath(String texturePath) {
        if (texturePath == null || texturePath.isEmpty()) {
            return texturePath;
        }

        // 如果已经是file://协议，直接返回
        if (texturePath.startsWith("file://")) {

            return texturePath;
        }

        // 标准化路径（统一使用正斜杠）
        String normalized = texturePath.replace('\\', '/');

        // 尝试找到resources目录
        int resourcesIndex = normalized.indexOf("/resources/");
        if (resourcesIndex != -1) {
            // 提取resources之后的路径
            String resourcePath = normalized.substring(resourcesIndex + "/resources/".length());

            return resourcePath;
        }

        // 检查是否为绝对路径
        boolean isAbsolutePath = normalized.matches("^[A-Za-z]:.*") || normalized.startsWith("/");
        if (isAbsolutePath) {
            // 使用file://协议支持绝对路径
            String fileProtocolPath;
            if (normalized.matches("^[A-Za-z]:.*")) {
                // Windows路径
                fileProtocolPath = "file:///" + normalized;
            } else {
                // Unix路径
                fileProtocolPath = "file://" + normalized;
            }

            return fileProtocolPath;
        }

        // 相对路径，假定从resources目录加载

        return normalized;
    }

    /**
     * 将配置应用到骨架和渲染器
     * 完全重建骨架结构，而不是更新现有骨骼
     */
    public static void applyConfig(PuppetConfig config, com.Hecate.puppet.core.Skeleton skeleton, com.Hecate.puppet.core.PuppetRenderer renderer) {

        if (config == null || skeleton == null || renderer == null) {

            return;
        }

        // 步骤0：应用Billboard渲染模式
        if (config.getBillboardMode() != null) {
            try {
                PuppetRenderer.BillboardMode mode = PuppetRenderer.BillboardMode.valueOf(config.getBillboardMode());
                renderer.setBillboardMode(mode);

            } catch (IllegalArgumentException e) {

                renderer.setBillboardMode(PuppetRenderer.BillboardMode.UNIFIED);
            }
        }

        // 步骤1：保存渲染器的场景附着状态
        com.jme3.scene.Node parentNode = renderer.getPuppetNode().getParent();
        boolean wasAttached = (parentNode != null);

        // 步骤2：清空现有渲染器
        renderer.cleanup();

        // 步骤3：清空现有骨架
        skeleton.clear();

        // 步骤3：从配置创建骨骼（不建立父子关系）

        Map<String, Bone> boneMap = new HashMap<>();
        for (BoneConfig boneConfig : config.getBones()) {
            Bone bone = new Bone(boneConfig.getName());

            // 设置 Rest Pose
            if (boneConfig.getRestPosition() != null) {
                bone.setRestPosition(boneConfig.getRestPosition().toVector3f());
            }
            if (boneConfig.getRestRotation() != null) {
                bone.setRestRotation(boneConfig.getRestRotation().toQuaternion());
            }
            if (boneConfig.getRestScale() != null) {
                bone.setRestScale(boneConfig.getRestScale().toVector3f());
            }

            // 设置 Current Pose
            if (boneConfig.getCurrentPosition() != null) {
                bone.setLocalPosition(boneConfig.getCurrentPosition().toVector3f());
            }
            if (boneConfig.getCurrentRotation() != null) {
                bone.setLocalRotation(boneConfig.getCurrentRotation().toQuaternion());
            }
            if (boneConfig.getCurrentScale() != null) {
                bone.setLocalScale(boneConfig.getCurrentScale().toVector3f());
            }

            // 设置自由骨骼系统属性
            if (boneConfig.getBoneType() != null) {
                try {
                    Bone.BoneType boneType = Bone.BoneType.valueOf(boneConfig.getBoneType());
                    bone.setBoneType(boneType);
                } catch (IllegalArgumentException e) {

                    bone.setBoneType(Bone.BoneType.CONNECTED);
                }
            }

            if (boneConfig.getGravityDirection() != null) {
                try {
                    Bone.GravityDirection gravityDir = Bone.GravityDirection.valueOf(boneConfig.getGravityDirection());
                    bone.setGravityDirection(gravityDir);
                } catch (IllegalArgumentException e) {
                    System.err.println("[PuppetIO] 无效的重力方向: " + boneConfig.getGravityDirection() + "，使用默认值DOWN");
                    bone.setGravityDirection(Bone.GravityDirection.DOWN);
                }
            }

            if (boneConfig.getCustomGravityVector() != null) {
                bone.setCustomGravityVector(boneConfig.getCustomGravityVector().toVector3f());
            }

            bone.setFreedomValue(boneConfig.getFreedomValue());

            // 加载Camera Follow相机跟随自由度（Live2D风格效果）
            bone.setCameraFollowFreedomX(boneConfig.getCameraFollowFreedomX());
            bone.setCameraFollowFreedomY(boneConfig.getCameraFollowFreedomY());

            // 加载FreeBonePhysics物理参数
            bone.setPhysMass(boneConfig.getMass());
            bone.setPhysDamping(boneConfig.getDamping());
            bone.setPhysStiffness(boneConfig.getStiffness());
            bone.setPhysGravityStrength(boneConfig.getGravityStrength());
            bone.setPhysMaxSwingAngle(boneConfig.getMaxSwingAngle());
            bone.setPhysMaxVelocity(boneConfig.getMaxVelocity());

            // 加载贴图模式配置
            bone.setMultiDirectionTextureEnabled(boneConfig.isMultiDirectionTextureEnabled());

            // 设置纹理和UV信息
            PartConfig partConfig = boneConfig.getPartConfig();
            if (partConfig != null) {
                // 设置多方向贴图（转换路径）
                if (partConfig.getDirectionTextures() != null) {
                    Map<String, String> convertedTextures = new HashMap<>();
                    for (Map.Entry<String, String> entry : partConfig.getDirectionTextures().entrySet()) {
                        convertedTextures.put(entry.getKey(), convertTexturePath(entry.getValue()));
                    }
                    bone.setDirectionTextures(convertedTextures);
                }
                if (partConfig.getCurrentDirection() != null) {
                    bone.setCurrentDirection(partConfig.getCurrentDirection());
                }

                // 设置纹理路径（向后兼容，转换路径）
                if (partConfig.getTexturePath() != null && !partConfig.getTexturePath().isEmpty()) {
                    String originalPath = partConfig.getTexturePath();
                    String convertedPath = convertTexturePath(originalPath);

                    bone.setTexturePath(convertedPath);
                }

                // 设置多方向UV坐标
                if (partConfig.getDirectionUVs() != null) {
                    bone.setDirectionUVs(partConfig.getDirectionUVs());
                }

                // 设置优先级
                bone.setPriority(partConfig.getPriority());

                // 设置多方向优先级（新增）
                if (partConfig.getDirectionPriorities() != null) {
                    bone.setDirectionPriorities(partConfig.getDirectionPriorities());
                }

                // 设置多方向尺寸（新增）
                if (partConfig.getDirectionWidths() != null) {
                    bone.setDirectionWidths(partConfig.getDirectionWidths());
                }
                if (partConfig.getDirectionHeights() != null) {
                    bone.setDirectionHeights(partConfig.getDirectionHeights());
                }

                // 设置多方向位置偏移（新增）
                if (partConfig.getDirectionOffsets() != null) {
                    bone.setDirectionOffsets(partConfig.getDirectionOffsets());
                }

                // 设置多方向旋转（新增）
                if (partConfig.getDirectionRotations() != null) {
                    bone.setDirectionRotations(partConfig.getDirectionRotations());
                }

                // 设置多方向贴图旋转（新增）
                if (partConfig.getDirectionTextureRotations() != null) {
                    bone.setDirectionTextureRotations(partConfig.getDirectionTextureRotations());
                }

                // 设置Billboard启用状态（新增 - 每个部件独立控制2D/3D模式）
                bone.setBillboardEnabled(partConfig.isBillboardEnabled());

                // 设置旋转条状贴图配置（新增）
                bone.setRotationStripEnabled(partConfig.isRotationStripEnabled());
                if (partConfig.getStripTexturePath() != null && !partConfig.getStripTexturePath().isEmpty()) {
                    bone.setStripTexturePath(convertTexturePath(partConfig.getStripTexturePath()));
                }
                bone.setStripSteps(partConfig.getStripSteps());
                bone.setStripFrameWidthPx(partConfig.getStripFrameWidthPx());
                bone.setStripFrameHeightPx(partConfig.getStripFrameHeightPx());

                // 设置Billboard俯仰角平滑过渡阈值（新增）
                bone.setBillboardPitchFullRangeDeg(partConfig.getBillboardPitchFullRangeDeg());
                bone.setBillboardPitchLockDeg(partConfig.getBillboardPitchLockDeg());

                // 设置旋转条状贴图专用变换数据（新增 - 单一值，不按方向分槎）
                bone.setStripWidth(partConfig.getStripWidth());
                bone.setStripHeight(partConfig.getStripHeight());
                if (partConfig.getStripOffset() != null) {
                    Vector3f so = partConfig.getStripOffset().toVector3f();
                    bone.setStripOffset(so.x, so.y, so.z);
                }
                bone.setStripRotation(
                    partConfig.getStripRotationX(),
                    partConfig.getStripRotationY(),
                    partConfig.getStripRotationZ()
                );
                bone.setStripPriority(partConfig.getStripPriority());
                bone.setStripCalibrationOffsetPx(partConfig.getStripCalibrationOffsetPx());

                // 加载3D模型骨骼配置（新增）
                bone.setModelEnabled(partConfig.isModelEnabled());
                bone.setModelFilePath(partConfig.getModelFilePath());
                bone.setModelRotation(
                    partConfig.getModelRotationX(),
                    partConfig.getModelRotationY(),
                    partConfig.getModelRotationZ()
                );
                bone.setModelScale(partConfig.getModelScale());
            }

            // 加载骨骼分组ID（新增）
            if (boneConfig.getGroupId() != null) {
                bone.setGroupId(boneConfig.getGroupId());
            }

            boneMap.put(bone.getName(), bone);
            skeleton.addBone(bone);
        }

        // 步骤4：建立父子关系
        Bone rootBone = null;
        int rootBoneCount = 0;
        for (BoneConfig boneConfig : config.getBones()) {
            Bone bone = boneMap.get(boneConfig.getName());
            String parentName = boneConfig.getParentName();

            if (parentName == null || parentName.isEmpty()) {
                // 这是根骨骼
                if (rootBone == null) {
                    rootBone = bone;
                } else {
                    // 多个根骨骼！将后续的"根骨骼"作为第一个根骨骼的子骨骼
                    rootBone.addChild(bone);
                }
                rootBoneCount++;
            } else {
                // 找到父骨骼并建立关系
                Bone parent = boneMap.get(parentName);
                if (parent != null) {
                    parent.addChild(bone);
                } else {
                
                    if (rootBone != null) {
                        rootBone.addChild(bone);
                    }
                }
            }
        }

        // 设置根骨骼
        if (rootBone != null) {
            skeleton.setRootBone(rootBone);
        } else {

        }

        // 步骤5：为每个骨骼创建渲染器

        for (BoneConfig boneConfig : config.getBones()) {
            Bone bone = boneMap.get(boneConfig.getName());
            PartConfig partConfig = boneConfig.getPartConfig();

            if (bone != null && partConfig != null) {

                // 创建部件渲染器
                PuppetPartRenderer partRenderer = renderer.addPartRenderer(
                    bone,
                    partConfig.getWidth(),
                    partConfig.getHeight()
                );

                // 设置偏移
                if (partConfig.getOffset() != null) {
                    partRenderer.setOffset(
                        partConfig.getOffset().getX(),
                        partConfig.getOffset().getY()
                    );
                    partRenderer.setOffsetZ(partConfig.getOffset().getZ());
                }

                // 设置自定义旋转
                partRenderer.setCustomRotationX(partConfig.getCustomRotationX());
                partRenderer.setCustomRotationZ(partConfig.getCustomRotationZ());

                // 设置中心点
                if (partConfig.getPivotPoint() != null) {
                    partRenderer.setPivotPoint(partConfig.getPivotPoint().toVector3f());
                }

                // 加载纹理和UV

                partRenderer.updateTextureFromBone();

                // 应用旧格式UV（向后兼容）
                if (partConfig.getDirectionUVs() == null || partConfig.getDirectionUVs().isEmpty()) {
                    partRenderer.setUV(
                        partConfig.getUvOffsetX(),
                        partConfig.getUvOffsetY(),
                        partConfig.getUvScaleX(),
                        partConfig.getUvScaleY()
                    );
                }
            }
        }

        // 请求渲染器重新排序
        renderer.requestPrioritySort();

        // 步骤6：恢复骨骼分组（新增）
        if (config.getGroups() != null && !config.getGroups().isEmpty()) {
            com.Hecate.puppet.core.GroupManager groupManager = skeleton.getGroupManager();
            if (groupManager != null) {
                for (GroupConfig groupConfig : config.getGroups()) {
                    // 创建分组（使用保存的groupId）
                    com.Hecate.puppet.core.BoneGroup group = groupManager.createGroup(
                        groupConfig.getGroupId(),
                        groupConfig.getName()
                    );

                    if (group != null) {
                        // 添加成员骨骼
                        for (String boneName : groupConfig.getMemberBoneNames()) {
                            Bone bone = boneMap.get(boneName);
                            if (bone != null) {
                                groupManager.addBoneToGroup(groupConfig.getGroupId(), bone);
                            }
                        }

                        // 恢复旋转角度
                        group.setCurrentRotation(groupConfig.getCurrentRotation());
                    }
                }
            }
        }

        // 步骤7：重新附加到场景（如果之前附加过）
        if (wasAttached && parentNode != null) {
            renderer.attachToScene(parentNode);
        }

        // 步骤8：重新激活渲染器
        renderer.setInitialized(true);

    }

    /**
     * 添加配置到骨架和渲染器（不清除现有模型）
     * 将新的骨骼添加到现有骨架中
     */
    public static void addConfig(PuppetConfig config, com.Hecate.puppet.core.Skeleton skeleton, com.Hecate.puppet.core.PuppetRenderer renderer) {
        if (config == null || skeleton == null || renderer == null) {
            System.err.println("[PuppetIO.addConfig] 参数为空！config=" + config + ", skeleton=" + skeleton + ", renderer=" + renderer);
            return;
        }

        // 步骤1：从配置创建骨骼（不建立父子关系）
        Map<String, Bone> boneMap = new HashMap<>();
        for (BoneConfig boneConfig : config.getBones()) {
            Bone bone = new Bone(boneConfig.getName());

            // 设置 Rest Pose
            if (boneConfig.getRestPosition() != null) {
                bone.setRestPosition(boneConfig.getRestPosition().toVector3f());
            }
            if (boneConfig.getRestRotation() != null) {
                bone.setRestRotation(boneConfig.getRestRotation().toQuaternion());
            }
            if (boneConfig.getRestScale() != null) {
                bone.setRestScale(boneConfig.getRestScale().toVector3f());
            }

            // 设置 Current Pose
            if (boneConfig.getCurrentPosition() != null) {
                bone.setLocalPosition(boneConfig.getCurrentPosition().toVector3f());
            }
            if (boneConfig.getCurrentRotation() != null) {
                bone.setLocalRotation(boneConfig.getCurrentRotation().toQuaternion());
            }
            if (boneConfig.getCurrentScale() != null) {
                bone.setLocalScale(boneConfig.getCurrentScale().toVector3f());
            }

            // 设置自由骨骼系统属性
            if (boneConfig.getBoneType() != null) {
                try {
                    Bone.BoneType boneType = Bone.BoneType.valueOf(boneConfig.getBoneType());
                    bone.setBoneType(boneType);
                } catch (IllegalArgumentException e) {
                    System.err.println("[PuppetIO] 无效的骨骼类型: " + boneConfig.getBoneType() + "，使用默认值CONNECTED");
                    bone.setBoneType(Bone.BoneType.CONNECTED);
                }
            }

            if (boneConfig.getGravityDirection() != null) {
                try {
                    Bone.GravityDirection gravityDir = Bone.GravityDirection.valueOf(boneConfig.getGravityDirection());
                    bone.setGravityDirection(gravityDir);
                } catch (IllegalArgumentException e) {
                    System.err.println("[PuppetIO] 无效的重力方向: " + boneConfig.getGravityDirection() + "，使用默认值DOWN");
                    bone.setGravityDirection(Bone.GravityDirection.DOWN);
                }
            }

            if (boneConfig.getCustomGravityVector() != null) {
                bone.setCustomGravityVector(boneConfig.getCustomGravityVector().toVector3f());
            }

            bone.setFreedomValue(boneConfig.getFreedomValue());

            // 加载Camera Follow相机跟随自由度（Live2D风格效果）
            bone.setCameraFollowFreedomX(boneConfig.getCameraFollowFreedomX());
            bone.setCameraFollowFreedomY(boneConfig.getCameraFollowFreedomY());

            // 加载FreeBonePhysics物理参数
            bone.setPhysMass(boneConfig.getMass());
            bone.setPhysDamping(boneConfig.getDamping());
            bone.setPhysStiffness(boneConfig.getStiffness());
            bone.setPhysGravityStrength(boneConfig.getGravityStrength());
            bone.setPhysMaxSwingAngle(boneConfig.getMaxSwingAngle());
            bone.setPhysMaxVelocity(boneConfig.getMaxVelocity());

            // 加载贴图模式配置
            bone.setMultiDirectionTextureEnabled(boneConfig.isMultiDirectionTextureEnabled());

            // 设置纹理和UV信息
            PartConfig partConfig = boneConfig.getPartConfig();
            if (partConfig != null) {
                // 设置多方向贴图（转换路径）
                if (partConfig.getDirectionTextures() != null) {
                    Map<String, String> convertedTextures = new HashMap<>();
                    for (Map.Entry<String, String> entry : partConfig.getDirectionTextures().entrySet()) {
                        convertedTextures.put(entry.getKey(), convertTexturePath(entry.getValue()));
                    }
                    bone.setDirectionTextures(convertedTextures);
                }
                if (partConfig.getCurrentDirection() != null) {
                    bone.setCurrentDirection(partConfig.getCurrentDirection());
                }

                // 设置纹理路径（向后兼容，转换路径）
                if (partConfig.getTexturePath() != null && !partConfig.getTexturePath().isEmpty()) {
                    String originalPath = partConfig.getTexturePath();
                    String convertedPath = convertTexturePath(originalPath);
                    bone.setTexturePath(convertedPath);
                }

                // 设置多方向UV坐标
                if (partConfig.getDirectionUVs() != null) {
                    bone.setDirectionUVs(partConfig.getDirectionUVs());
                }

                // 设置优先级
                bone.setPriority(partConfig.getPriority());

                // 设置多方向优先级（新增）
                if (partConfig.getDirectionPriorities() != null) {
                    bone.setDirectionPriorities(partConfig.getDirectionPriorities());
                }

                // 设置多方向尺寸（新增）
                if (partConfig.getDirectionWidths() != null) {
                    bone.setDirectionWidths(partConfig.getDirectionWidths());
                }
                if (partConfig.getDirectionHeights() != null) {
                    bone.setDirectionHeights(partConfig.getDirectionHeights());
                }

                // 设置多方向位置偏移（新增）
                if (partConfig.getDirectionOffsets() != null) {
                    bone.setDirectionOffsets(partConfig.getDirectionOffsets());
                }

                // 设置多方向旋转（新增）
                if (partConfig.getDirectionRotations() != null) {
                    bone.setDirectionRotations(partConfig.getDirectionRotations());
                }

                // 设置多方向贴图旋转（新增）
                if (partConfig.getDirectionTextureRotations() != null) {
                    bone.setDirectionTextureRotations(partConfig.getDirectionTextureRotations());
                }

                // 设置Billboard启用状态（新增 - 每个部件独立控制2D/3D模式）
                bone.setBillboardEnabled(partConfig.isBillboardEnabled());

                // 设置旋转条状贴图配置（新增）
                bone.setRotationStripEnabled(partConfig.isRotationStripEnabled());
                if (partConfig.getStripTexturePath() != null && !partConfig.getStripTexturePath().isEmpty()) {
                    bone.setStripTexturePath(convertTexturePath(partConfig.getStripTexturePath()));
                }
                bone.setStripSteps(partConfig.getStripSteps());
                bone.setStripFrameWidthPx(partConfig.getStripFrameWidthPx());
                bone.setStripFrameHeightPx(partConfig.getStripFrameHeightPx());

                // 设置Billboard俯仰角平滑过渡阈值（新增）
                bone.setBillboardPitchFullRangeDeg(partConfig.getBillboardPitchFullRangeDeg());
                bone.setBillboardPitchLockDeg(partConfig.getBillboardPitchLockDeg());

                // 设置旋转条状贴图专用变换数据（新增 - 单一值，不按方向分槎）
                bone.setStripWidth(partConfig.getStripWidth());
                bone.setStripHeight(partConfig.getStripHeight());
                if (partConfig.getStripOffset() != null) {
                    Vector3f so = partConfig.getStripOffset().toVector3f();
                    bone.setStripOffset(so.x, so.y, so.z);
                }
                bone.setStripRotation(
                    partConfig.getStripRotationX(),
                    partConfig.getStripRotationY(),
                    partConfig.getStripRotationZ()
                );
                bone.setStripPriority(partConfig.getStripPriority());
                bone.setStripCalibrationOffsetPx(partConfig.getStripCalibrationOffsetPx());

                // 加载3D模型骨骼配置（新增）
                bone.setModelEnabled(partConfig.isModelEnabled());
                bone.setModelFilePath(partConfig.getModelFilePath());
                bone.setModelRotation(
                    partConfig.getModelRotationX(),
                    partConfig.getModelRotationY(),
                    partConfig.getModelRotationZ()
                );
                bone.setModelScale(partConfig.getModelScale());
            }

            boneMap.put(bone.getName(), bone);
        }

        // 步骤2：建立骨骼父子关系
        for (BoneConfig boneConfig : config.getBones()) {
            Bone bone = boneMap.get(boneConfig.getName());
            if (bone != null && boneConfig.getParentName() != null) {
                Bone parent = boneMap.get(boneConfig.getParentName());
                if (parent != null) {
                    parent.addChild(bone);
                }
            }
        }

        // 步骤3：将新骨骼添加到骨架
        for (Bone bone : boneMap.values()) {
            skeleton.addBone(bone);
        }

        // 步骤4：为新骨骼创建渲染器
        for (Bone bone : boneMap.values()) {
            renderer.addPartRenderer(bone, 1.0f, 1.0f);  // 使用默认尺寸，后续会从配置加载

            // 加载纹理
            if (bone.getTexturePath() != null && !bone.getTexturePath().isEmpty()) {
                com.Hecate.puppet.core.PuppetPartRenderer partRenderer = renderer.getPartRenderer(bone.getName());
                if (partRenderer != null) {
                    partRenderer.loadTexture(bone.getTexturePath());
                }
            }
        }

    }

    /**
     * 应用配置到骨架和渲染器（编辑器版本）
     * 直接操作 EditorSkeleton 和 EditorBone，避免类型转换问题
     */
    public static void applyConfig(PuppetConfig config,
                                   com.Hecate.puppet.editor.core.EditorSkeleton editorSkeleton,
                                   com.Hecate.puppet.editor.core.EditorPuppetRenderer editorRenderer) {

        if (config == null || editorSkeleton == null || editorRenderer == null) {

            return;
        }

        // 步骤1：保存渲染器的场景附着状态
        com.jme3.scene.Node parentNode = editorRenderer.getPuppetNode().getParent();
        boolean wasAttached = (parentNode != null);

        // 步骤2：清空现有渲染器

        editorRenderer.cleanup();

        // 步骤3：清空现有骨架

        editorSkeleton.clear();

        // 步骤4：从配置创建 EditorBone（不建立父子关系）

        Map<String, com.Hecate.puppet.editor.core.EditorBone> boneMap = new HashMap<>();
        for (BoneConfig boneConfig : config.getBones()) {
            com.Hecate.puppet.editor.core.EditorBone bone = new com.Hecate.puppet.editor.core.EditorBone(boneConfig.getName());

            // 设置 Rest Pose
            if (boneConfig.getRestPosition() != null) {
                bone.setRestPosition(boneConfig.getRestPosition().toVector3f());
            }
            if (boneConfig.getRestRotation() != null) {
                bone.setRestRotation(boneConfig.getRestRotation().toQuaternion());
            }
            if (boneConfig.getRestScale() != null) {
                bone.setRestScale(boneConfig.getRestScale().toVector3f());
            }

            // 设置 Current Pose
            if (boneConfig.getCurrentPosition() != null) {
                bone.setLocalPosition(boneConfig.getCurrentPosition().toVector3f());
            }
            if (boneConfig.getCurrentRotation() != null) {
                bone.setLocalRotation(boneConfig.getCurrentRotation().toQuaternion());
            }
            if (boneConfig.getCurrentScale() != null) {
                bone.setLocalScale(boneConfig.getCurrentScale().toVector3f());
            }

            // 设置自由骨骼系统属性
            if (boneConfig.getBoneType() != null) {
                try {
                    com.Hecate.puppet.editor.core.EditorBone.BoneType boneType = com.Hecate.puppet.editor.core.EditorBone.BoneType.valueOf(boneConfig.getBoneType());
                    bone.setBoneType(boneType);
                } catch (IllegalArgumentException e) {
                    System.err.println("[PuppetIO] 无效的骨骼类型: " + boneConfig.getBoneType());
                    bone.setBoneType(com.Hecate.puppet.editor.core.EditorBone.BoneType.CONNECTED);
                }
            }

            if (boneConfig.getGravityDirection() != null) {
                try {
                    com.Hecate.puppet.editor.core.EditorBone.GravityDirection gravityDir = com.Hecate.puppet.editor.core.EditorBone.GravityDirection.valueOf(boneConfig.getGravityDirection());
                    bone.setGravityDirection(gravityDir);
                } catch (IllegalArgumentException e) {
                    System.err.println("[PuppetIO] 无效的重力方向: " + boneConfig.getGravityDirection());
                    bone.setGravityDirection(com.Hecate.puppet.editor.core.EditorBone.GravityDirection.DOWN);
                }
            }

            if (boneConfig.getCustomGravityVector() != null) {
                bone.setCustomGravityVector(boneConfig.getCustomGravityVector().toVector3f());
            }

            bone.setFreedomValue(boneConfig.getFreedomValue());

            // 加载Camera Follow相机跟随自由度（Live2D风格效果）
            bone.setCameraFollowFreedomX(boneConfig.getCameraFollowFreedomX());
            bone.setCameraFollowFreedomY(boneConfig.getCameraFollowFreedomY());

            // 加载FreeBonePhysics物理参数
            bone.setPhysMass(boneConfig.getMass());
            bone.setPhysDamping(boneConfig.getDamping());
            bone.setPhysStiffness(boneConfig.getStiffness());
            bone.setPhysGravityStrength(boneConfig.getGravityStrength());
            bone.setPhysMaxSwingAngle(boneConfig.getMaxSwingAngle());
            bone.setPhysMaxVelocity(boneConfig.getMaxVelocity());

            // 加载贴图模式配置
            bone.setMultiDirectionTextureEnabled(boneConfig.isMultiDirectionTextureEnabled());

            // 设置纹理和UV信息
            PartConfig partConfig = boneConfig.getPartConfig();
            if (partConfig != null) {
                // 设置多方向贴图（转换路径）
                if (partConfig.getDirectionTextures() != null) {
                    Map<String, String> convertedTextures = new HashMap<>();
                    for (Map.Entry<String, String> entry : partConfig.getDirectionTextures().entrySet()) {
                        convertedTextures.put(entry.getKey(), convertTexturePath(entry.getValue()));
                    }
                    bone.setDirectionTextures(convertedTextures);
                }
                if (partConfig.getCurrentDirection() != null) {
                    bone.setCurrentDirection(partConfig.getCurrentDirection());
                }

                // 设置纹理路径（向后兼容，转换路径）
                if (partConfig.getTexturePath() != null && !partConfig.getTexturePath().isEmpty()) {
                    String originalPath = partConfig.getTexturePath();
                    String convertedPath = convertTexturePath(originalPath);

                    bone.setTexturePath(convertedPath);
                }

                // 设置多方向UV坐标
                if (partConfig.getDirectionUVs() != null) {
                    bone.setDirectionUVs(partConfig.getDirectionUVs());
                }

                // 设置优先级
                bone.setPriority(partConfig.getPriority());

                // 设置多方向优先级
                if (partConfig.getDirectionPriorities() != null) {
                    bone.setDirectionPriorities(partConfig.getDirectionPriorities());
                }

                // 设置多方向尺寸
                if (partConfig.getDirectionWidths() != null) {
                    bone.setDirectionWidths(partConfig.getDirectionWidths());
                }
                if (partConfig.getDirectionHeights() != null) {
                    bone.setDirectionHeights(partConfig.getDirectionHeights());
                }

                // 设置多方向位置偏移
                if (partConfig.getDirectionOffsets() != null) {
                    bone.setDirectionOffsets(partConfig.getDirectionOffsets());
                }

                // 设置多方向旋转
                if (partConfig.getDirectionRotations() != null) {
                    bone.setDirectionRotations(partConfig.getDirectionRotations());
                }

                // 设置多方向贴图旋转
                if (partConfig.getDirectionTextureRotations() != null) {
                    bone.setDirectionTextureRotations(partConfig.getDirectionTextureRotations());
                }

                // 设置Billboard启用状态
                bone.setBillboardEnabled(partConfig.isBillboardEnabled());

                // 设置旋转条状贴图配置（新增）
                bone.setRotationStripEnabled(partConfig.isRotationStripEnabled());
                if (partConfig.getStripTexturePath() != null && !partConfig.getStripTexturePath().isEmpty()) {
                    bone.setStripTexturePath(convertTexturePath(partConfig.getStripTexturePath()));
                }
                bone.setStripSteps(partConfig.getStripSteps());
                bone.setStripFrameWidthPx(partConfig.getStripFrameWidthPx());
                bone.setStripFrameHeightPx(partConfig.getStripFrameHeightPx());

                // 设置Billboard俯仰角平滑过渡阈值（新增）
                bone.setBillboardPitchFullRangeDeg(partConfig.getBillboardPitchFullRangeDeg());
                bone.setBillboardPitchLockDeg(partConfig.getBillboardPitchLockDeg());

                // 设置旋转条状贴图专用变换数据（新增 - 单一值，不按方向分槎）
                bone.setStripWidth(partConfig.getStripWidth());
                bone.setStripHeight(partConfig.getStripHeight());
                if (partConfig.getStripOffset() != null) {
                    Vector3f so = partConfig.getStripOffset().toVector3f();
                    bone.setStripOffset(so.x, so.y, so.z);
                }
                bone.setStripRotation(
                    partConfig.getStripRotationX(),
                    partConfig.getStripRotationY(),
                    partConfig.getStripRotationZ()
                );
                bone.setStripPriority(partConfig.getStripPriority());
                bone.setStripCalibrationOffsetPx(partConfig.getStripCalibrationOffsetPx());

                // 加载3D模型骨骼配置（新增）
                bone.setModelEnabled(partConfig.isModelEnabled());
                bone.setModelFilePath(partConfig.getModelFilePath());
                bone.setModelRotation(
                    partConfig.getModelRotationX(),
                    partConfig.getModelRotationY(),
                    partConfig.getModelRotationZ()
                );
                bone.setModelScale(partConfig.getModelScale());
            }

            boneMap.put(bone.getName(), bone);
            editorSkeleton.addBone(bone);
        }

        // 步骤3：建立父子关系
        com.Hecate.puppet.editor.core.EditorBone rootBone = null;
        for (BoneConfig boneConfig : config.getBones()) {
            com.Hecate.puppet.editor.core.EditorBone bone = boneMap.get(boneConfig.getName());
            String parentName = boneConfig.getParentName();

            if (parentName == null || parentName.isEmpty()) {
                // 这是根骨骼
                if (rootBone == null) {
                    rootBone = bone;
                } else {
                    // 多个根骨骼！将后续的"根骨骼"作为第一个根骨骼的子骨骼
                    rootBone.addChild(bone);
                }
            } else {
                // 找到父骨骼并建立关系
                com.Hecate.puppet.editor.core.EditorBone parent = boneMap.get(parentName);
                if (parent != null) {
                    parent.addChild(bone);
                } else {

                    if (rootBone != null) {
                        rootBone.addChild(bone);
                    }
                }
            }
        }

        // 设置根骨骼
        if (rootBone != null) {
            editorSkeleton.setRootBone(rootBone);
        } else {

        }

        // 步骤4：为每个骨骼创建渲染器部件

        for (BoneConfig boneConfig : config.getBones()) {
            com.Hecate.puppet.editor.core.EditorBone bone = boneMap.get(boneConfig.getName());
            PartConfig partConfig = boneConfig.getPartConfig();

            if (bone != null && partConfig != null) {

                // 创建部件渲染器
                com.Hecate.puppet.editor.core.EditorPuppetPartRenderer partRenderer = editorRenderer.addPartRenderer(
                    bone,
                    partConfig.getWidth(),
                    partConfig.getHeight()
                );

                // 设置偏移
                if (partConfig.getOffset() != null) {
                    partRenderer.setOffset(
                        partConfig.getOffset().getX(),
                        partConfig.getOffset().getY()
                    );
                    partRenderer.setOffsetZ(partConfig.getOffset().getZ());
                }

                // 设置自定义旋转
                partRenderer.setCustomRotationX(partConfig.getCustomRotationX());
                partRenderer.setCustomRotationZ(partConfig.getCustomRotationZ());

                // 设置中心点
                if (partConfig.getPivotPoint() != null) {
                    partRenderer.setPivotPoint(partConfig.getPivotPoint().toVector3f());
                }

                // 加载纹理和UV

                partRenderer.updateTextureFromBone();

                // 应用旧格式UV（向后兼容）
                if (partConfig.getDirectionUVs() == null || partConfig.getDirectionUVs().isEmpty()) {
                    partRenderer.setUV(
                        partConfig.getUvOffsetX(),
                        partConfig.getUvOffsetY(),
                        partConfig.getUvScaleX(),
                        partConfig.getUvScaleY()
                    );
                }
            }
        }

        // 请求渲染器重新排序
        editorRenderer.requestPrioritySort();

        // 步骤5：重新附加到场景（如果之前附加过）
        if (wasAttached && parentNode != null) {

            editorRenderer.attachToScene(parentNode);
        }

        // 步骤6：重新激活渲染器

        editorRenderer.setInitialized(true);

    }

    /**
     * 添加配置到骨架和渲染器（编辑器版本）
     * 专门为编辑器设计，正确处理 EditorBone 和 EditorPuppetPartRenderer
     */
    public static void addConfig(PuppetConfig config,
                                 com.Hecate.puppet.editor.core.EditorSkeleton editorSkeleton,
                                 com.Hecate.puppet.editor.core.EditorPuppetRenderer editorRenderer) {

        if (config == null || editorSkeleton == null || editorRenderer == null) {
            System.err.println("[PuppetIO.addConfig] 参数为空！config=" + config + ", editorSkeleton=" + editorSkeleton + ", editorRenderer=" + editorRenderer);
            return;
        }

        // 步骤1：从配置创建 EditorBone（不建立父子关系）

        Map<String, com.Hecate.puppet.editor.core.EditorBone> boneMap = new HashMap<>();
        for (BoneConfig boneConfig : config.getBones()) {
            com.Hecate.puppet.editor.core.EditorBone bone = new com.Hecate.puppet.editor.core.EditorBone(boneConfig.getName());

            // 设置 Rest Pose
            if (boneConfig.getRestPosition() != null) {
                bone.setRestPosition(boneConfig.getRestPosition().toVector3f());
            }
            if (boneConfig.getRestRotation() != null) {
                bone.setRestRotation(boneConfig.getRestRotation().toQuaternion());
            }
            if (boneConfig.getRestScale() != null) {
                bone.setRestScale(boneConfig.getRestScale().toVector3f());
            }

            // 设置 Current Pose
            if (boneConfig.getCurrentPosition() != null) {
                bone.setLocalPosition(boneConfig.getCurrentPosition().toVector3f());
            }
            if (boneConfig.getCurrentRotation() != null) {
                bone.setLocalRotation(boneConfig.getCurrentRotation().toQuaternion());
            }
            if (boneConfig.getCurrentScale() != null) {
                bone.setLocalScale(boneConfig.getCurrentScale().toVector3f());
            }

            // 设置自由骨骼系统属性
            if (boneConfig.getBoneType() != null) {
                try {
                    com.Hecate.puppet.editor.core.EditorBone.BoneType boneType = com.Hecate.puppet.editor.core.EditorBone.BoneType.valueOf(boneConfig.getBoneType());
                    bone.setBoneType(boneType);
                } catch (IllegalArgumentException e) {
                    System.err.println("[PuppetIO] 无效的骨骼类型: " + boneConfig.getBoneType());
                    bone.setBoneType(com.Hecate.puppet.editor.core.EditorBone.BoneType.CONNECTED);
                }
            }

            if (boneConfig.getGravityDirection() != null) {
                try {
                    com.Hecate.puppet.editor.core.EditorBone.GravityDirection gravityDir = com.Hecate.puppet.editor.core.EditorBone.GravityDirection.valueOf(boneConfig.getGravityDirection());
                    bone.setGravityDirection(gravityDir);
                } catch (IllegalArgumentException e) {
                    System.err.println("[PuppetIO] 无效的重力方向: " + boneConfig.getGravityDirection());
                    bone.setGravityDirection(com.Hecate.puppet.editor.core.EditorBone.GravityDirection.DOWN);
                }
            }

            if (boneConfig.getCustomGravityVector() != null) {
                bone.setCustomGravityVector(boneConfig.getCustomGravityVector().toVector3f());
            }

            bone.setFreedomValue(boneConfig.getFreedomValue());

            // 加载Camera Follow相机跟随自由度（Live2D风格效果）
            bone.setCameraFollowFreedomX(boneConfig.getCameraFollowFreedomX());
            bone.setCameraFollowFreedomY(boneConfig.getCameraFollowFreedomY());

            bone.setMultiDirectionTextureEnabled(boneConfig.isMultiDirectionTextureEnabled());

            // 设置纹理和UV信息
            PartConfig partConfig = boneConfig.getPartConfig();
            if (partConfig != null) {
                // 设置多方向贴图（转换路径）
                if (partConfig.getDirectionTextures() != null) {
                    Map<String, String> convertedTextures = new HashMap<>();
                    for (Map.Entry<String, String> entry : partConfig.getDirectionTextures().entrySet()) {
                        convertedTextures.put(entry.getKey(), convertTexturePath(entry.getValue()));
                    }
                    bone.setDirectionTextures(convertedTextures);
                }
                if (partConfig.getCurrentDirection() != null) {
                    bone.setCurrentDirection(partConfig.getCurrentDirection());
                }

                // 设置纹理路径（向后兼容，转换路径）
                if (partConfig.getTexturePath() != null && !partConfig.getTexturePath().isEmpty()) {
                    String originalPath = partConfig.getTexturePath();
                    String convertedPath = convertTexturePath(originalPath);
                    bone.setTexturePath(convertedPath);
                }

                // 设置多方向UV坐标
                if (partConfig.getDirectionUVs() != null) {
                    bone.setDirectionUVs(partConfig.getDirectionUVs());
                }

                // 设置优先级
                bone.setPriority(partConfig.getPriority());

                // 设置多方向优先级
                if (partConfig.getDirectionPriorities() != null) {
                    bone.setDirectionPriorities(partConfig.getDirectionPriorities());
                }

                // 设置多方向尺寸
                if (partConfig.getDirectionWidths() != null) {
                    bone.setDirectionWidths(partConfig.getDirectionWidths());
                }
                if (partConfig.getDirectionHeights() != null) {
                    bone.setDirectionHeights(partConfig.getDirectionHeights());
                }

                // 设置多方向位置偏移
                if (partConfig.getDirectionOffsets() != null) {
                    bone.setDirectionOffsets(partConfig.getDirectionOffsets());
                }

                // 设置多方向旋转
                if (partConfig.getDirectionRotations() != null) {
                    bone.setDirectionRotations(partConfig.getDirectionRotations());
                }

                // 设置多方向贴图旋转
                if (partConfig.getDirectionTextureRotations() != null) {
                    bone.setDirectionTextureRotations(partConfig.getDirectionTextureRotations());
                }

                // 设置Billboard启用状态
                bone.setBillboardEnabled(partConfig.isBillboardEnabled());

                // 设置旋转条状贴图配置（新增）
                bone.setRotationStripEnabled(partConfig.isRotationStripEnabled());
                if (partConfig.getStripTexturePath() != null && !partConfig.getStripTexturePath().isEmpty()) {
                    bone.setStripTexturePath(convertTexturePath(partConfig.getStripTexturePath()));
                }
                bone.setStripSteps(partConfig.getStripSteps());
                bone.setStripFrameWidthPx(partConfig.getStripFrameWidthPx());
                bone.setStripFrameHeightPx(partConfig.getStripFrameHeightPx());

                // 设置Billboard俯仰角平滑过渡阈值（新增）
                bone.setBillboardPitchFullRangeDeg(partConfig.getBillboardPitchFullRangeDeg());
                bone.setBillboardPitchLockDeg(partConfig.getBillboardPitchLockDeg());

                // 设置旋转条状贴图专用变换数据（新增 - 单一值，不按方向分槎）
                bone.setStripWidth(partConfig.getStripWidth());
                bone.setStripHeight(partConfig.getStripHeight());
                if (partConfig.getStripOffset() != null) {
                    Vector3f so = partConfig.getStripOffset().toVector3f();
                    bone.setStripOffset(so.x, so.y, so.z);
                }
                bone.setStripRotation(
                    partConfig.getStripRotationX(),
                    partConfig.getStripRotationY(),
                    partConfig.getStripRotationZ()
                );
                bone.setStripPriority(partConfig.getStripPriority());
                bone.setStripCalibrationOffsetPx(partConfig.getStripCalibrationOffsetPx());

                // 加载3D模型骨骼配置（新增）
                bone.setModelEnabled(partConfig.isModelEnabled());
                bone.setModelFilePath(partConfig.getModelFilePath());
                bone.setModelRotation(
                    partConfig.getModelRotationX(),
                    partConfig.getModelRotationY(),
                    partConfig.getModelRotationZ()
                );
                bone.setModelScale(partConfig.getModelScale());
            }

            boneMap.put(bone.getName(), bone);
        }

        // 步骤2：建立骨骼父子关系

        for (BoneConfig boneConfig : config.getBones()) {
            com.Hecate.puppet.editor.core.EditorBone bone = boneMap.get(boneConfig.getName());
            if (bone != null && boneConfig.getParentName() != null) {
                // 首先在新添加的骨骼中查找父骨骼
                com.Hecate.puppet.editor.core.EditorBone parent = boneMap.get(boneConfig.getParentName());

                // 如果新骨骼中没有，在现有骨架中查找
                if (parent == null) {
                    parent = editorSkeleton.findBone(boneConfig.getParentName());
                }

                if (parent != null) {
                    parent.addChild(bone);

                } else {

            }
            }
        }

        // 步骤3：将新骨骼添加到骨架

        for (com.Hecate.puppet.editor.core.EditorBone bone : boneMap.values()) {
            editorSkeleton.addBone(bone);

        }

        // 步骤4：为新骨骼创建编辑器渲染器

        for (com.Hecate.puppet.editor.core.EditorBone bone : boneMap.values()) {

            // 获取部件配置中的尺寸
            float width = 1.0f;
            float height = 1.0f;

            BoneConfig boneConfig = null;
            for (BoneConfig bc : config.getBones()) {
                if (bc.getName().equals(bone.getName())) {
                    boneConfig = bc;
                    break;
                }
            }

            if (boneConfig != null && boneConfig.getPartConfig() != null) {
                width = boneConfig.getPartConfig().getWidth();
                height = boneConfig.getPartConfig().getHeight();
            }

            com.Hecate.puppet.editor.core.EditorPuppetPartRenderer partRenderer =
                editorRenderer.addPartRenderer(bone, width, height);

            // 应用部件配置
            if (boneConfig != null && boneConfig.getPartConfig() != null) {
                PartConfig partConfig = boneConfig.getPartConfig();

                // 设置偏移
                if (partConfig.getOffset() != null) {
                    partRenderer.setOffset(
                        partConfig.getOffset().getX(),
                        partConfig.getOffset().getY()
                    );
                    partRenderer.setOffsetZ(partConfig.getOffset().getZ());
                }

                // 设置自定义旋转
                partRenderer.setCustomRotationX(partConfig.getCustomRotationX());
                partRenderer.setCustomRotationZ(partConfig.getCustomRotationZ());

                // 设置枢轴点
                if (partConfig.getPivotPoint() != null) {
                    partRenderer.setPivotPoint(partConfig.getPivotPoint().toVector3f());
                }

                // 加载纹理和UV（使用 updateTextureFromBone 来正确处理多方向贴图）

                partRenderer.updateTextureFromBone();

                // 应用旧格式UV（向后兼容）
                if (partConfig.getDirectionUVs() == null || partConfig.getDirectionUVs().isEmpty()) {
                    partRenderer.setUV(
                        partConfig.getUvOffsetX(),
                        partConfig.getUvOffsetY(),
                        partConfig.getUvScaleX(),
                        partConfig.getUvScaleY()
                    );
                }
            }
        }

        // 请求渲染器重新排序
        editorRenderer.requestPrioritySort();

    }
}
