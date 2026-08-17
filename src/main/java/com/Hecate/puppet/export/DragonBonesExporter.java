package com.Hecate.puppet.export;

import com.Hecate.puppet.core.Skeleton;
import com.Hecate.puppet.core.PuppetRenderer;
import com.Hecate.puppet.core.Bone;
import com.Hecate.puppet.core.PuppetPartRenderer;
import com.Hecate.puppet.animation.Keyframe;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.jme3.math.Vector3f;
import com.jme3.math.Quaternion;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.io.*;
import java.util.*;

/**
 * DragonBones格式导出器
 * 导出为DragonBones 5.5格式
 * 生成三个文件：
 * 1. 骨架JSON (xxx.json)
 * 2. 贴图集JSON (xxx_tex.json)
 * 3. 纹理图片 (xxx_tex.png)
 */
public class DragonBonesExporter implements IExporter {

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

    @Override
    public void export(Skeleton skeleton, PuppetRenderer renderer, String filePath) throws IOException {
        // 确保文件路径以.json结尾
        if (!filePath.toLowerCase().endsWith(".json")) {
            filePath += ".json";
        }

        File file = new File(filePath);
        String baseName = file.getName().replace(".json", "");
        String dir = file.getParent();

        // 生成骨架数据
        Map<String, Object> dragonBonesData = createDragonBonesData(skeleton, renderer, baseName);

        // 保存骨架JSON
        try (Writer writer = new FileWriter(filePath)) {
            gson.toJson(dragonBonesData, writer);
        }

        // 生成贴图集数据
        Map<String, Object> textureAtlasData = createTextureAtlasData(skeleton, renderer, baseName);

        // 保存贴图集JSON
        String texJsonPath = new File(dir, baseName + "_tex.json").getAbsolutePath();
        try (Writer writer = new FileWriter(texJsonPath)) {
            gson.toJson(textureAtlasData, writer);
        }

        // 生成并保存合并的纹理（暂时跳过实际图片合并，只创建占位符）
        // TODO: 实现真正的贴图合并功能
    }

    /**
     * 创建DragonBones骨架数据
     */
    private Map<String, Object> createDragonBonesData(Skeleton skeleton, PuppetRenderer renderer, String name) {
        Map<String, Object> root = new HashMap<>();
        root.put("name", name);
        root.put("version", "5.5");
        root.put("compatibleVersion", "5.5");
        root.put("frameRate", 24);

        List<Map<String, Object>> armatures = new ArrayList<>();
        Map<String, Object> armature = new HashMap<>();
        armature.put("name", skeleton.getName());
        armature.put("type", "Armature");

        // 骨骼列表
        List<Map<String, Object>> bones = new ArrayList<>();
        List<Bone> allBones = skeleton.getAllBones();
        for (Bone bone : allBones) {
            Map<String, Object> boneData = new HashMap<>();
            boneData.put("name", bone.getName());

            if (bone.getParent() != null) {
                boneData.put("parent", bone.getParent().getName());
            }

            // Transform
            Map<String, Object> transform = new HashMap<>();
            Vector3f pos = bone.getRestPosition();
            transform.put("x", pos.x);
            transform.put("y", pos.y);

            // 旋转（从Quaternion转换为欧拉角）
            Quaternion rot = bone.getRestRotation();
            float[] angles = rot.toAngles(null);
            transform.put("skX", Math.toDegrees(angles[2])); // Z轴旋转
            transform.put("skY", Math.toDegrees(angles[2]));

            // 缩放
            Vector3f scale = bone.getRestScale();
            transform.put("scX", scale.x);
            transform.put("scY", scale.y);

            boneData.put("transform", transform);
            bones.add(boneData);
        }
        armature.put("bone", bones);

        // Slot列表（每个bone对应一个slot）
        List<Map<String, Object>> slots = new ArrayList<>();
        for (Bone bone : allBones) {
            Map<String, Object> slot = new HashMap<>();
            slot.put("name", bone.getName() + "_slot");
            slot.put("parent", bone.getName());
            slot.put("displayIndex", 0);
            slots.add(slot);
        }
        armature.put("slot", slots);

        // Skin（皮肤/材质）
        List<Map<String, Object>> skins = new ArrayList<>();
        Map<String, Object> defaultSkin = new HashMap<>();
        defaultSkin.put("name", "default");

        List<Map<String, Object>> skinSlots = new ArrayList<>();
        for (Bone bone : allBones) {
            PuppetPartRenderer partRenderer = renderer.getPartRenderer(bone.getName());
            if (partRenderer != null && partRenderer.getTexturePath() != null) {
                Map<String, Object> skinSlot = new HashMap<>();
                skinSlot.put("name", bone.getName() + "_slot");

                List<Map<String, Object>> displays = new ArrayList<>();
                Map<String, Object> display = new HashMap<>();
                display.put("name", bone.getName());
                display.put("type", "image");

                Map<String, Object> displayTransform = new HashMap<>();
                displayTransform.put("x", 0);
                displayTransform.put("y", 0);
                displayTransform.put("skX", 0);
                displayTransform.put("skY", 0);
                displayTransform.put("scX", 1);
                displayTransform.put("scY", 1);
                display.put("transform", displayTransform);

                displays.add(display);
                skinSlot.put("display", displays);
                skinSlots.add(skinSlot);
            }
        }
        defaultSkin.put("slot", skinSlots);
        skins.add(defaultSkin);
        armature.put("skin", skins);

        armatures.add(armature);
        root.put("armature", armatures);

        return root;
    }

    /**
     * 创建贴图集数据
     */
    private Map<String, Object> createTextureAtlasData(Skeleton skeleton, PuppetRenderer renderer, String name) {
        Map<String, Object> root = new HashMap<>();
        root.put("name", name);
        root.put("imagePath", name + "_tex.png");
        root.put("width", 1024);
        root.put("height", 1024);

        List<Map<String, Object>> subTextures = new ArrayList<>();
        List<Bone> allBones = skeleton.getAllBones();

        int currentX = 0;
        int currentY = 0;
        int rowHeight = 0;

        for (Bone bone : allBones) {
            PuppetPartRenderer partRenderer = renderer.getPartRenderer(bone.getName());
            if (partRenderer != null && partRenderer.getTexturePath() != null) {
                Map<String, Object> subTexture = new HashMap<>();
                subTexture.put("name", bone.getName());
                subTexture.put("x", currentX);
                subTexture.put("y", currentY);

                int width = (int) (partRenderer.getWidth() * 100);
                int height = (int) (partRenderer.getHeight() * 100);

                subTexture.put("width", width);
                subTexture.put("height", height);

                subTextures.add(subTexture);

                // 简单的排列算法
                currentX += width + 2; // 2px间距
                rowHeight = Math.max(rowHeight, height);

                if (currentX > 1024) {
                    currentX = 0;
                    currentY += rowHeight + 2;
                    rowHeight = 0;
                }
            }
        }

        root.put("SubTexture", subTextures);
        return root;
    }

    @Override
    public ExportFormat getFormat() {
        return ExportFormat.DRAGONBONES;
    }
}
