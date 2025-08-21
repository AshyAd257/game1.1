package com.Hecate.blender;

import com.jme3.animation.AnimChannel;
import com.jme3.animation.AnimControl;
import com.jme3.animation.LoopMode;
import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.texture.Texture;

import java.util.List;
import java.util.Map;

/**
 * 单个Blender模型的封装类
 */
public class BlenderModel {
    private final String modelId;
    private final String modelPath;
    private final Map<String, String> texturePaths;
    private final List<String> animationNames;

    private Spatial loadedModel;
    private boolean isLoaded = false;

    public BlenderModel(String modelId, String modelPath,
                        Map<String, String> texturePaths,
                        List<String> animationNames) {
        this.modelId = modelId;
        this.modelPath = modelPath;
        this.texturePaths = texturePaths;
        this.animationNames = animationNames;
    }

    /**
     * 加载模型
     */
    public boolean load(AssetManager assetManager) {
        try {
            System.out.println("加载Blender模型: " + modelId + " 从路径: " + modelPath);

            // 加载基础模型
            loadedModel = assetManager.loadModel(modelPath);
            if (loadedModel == null) {
                System.err.println("无法加载模型文件: " + modelPath);
                return false;
            }

            // 应用纹理
            if (texturePaths != null && !texturePaths.isEmpty()) {
                applyTextures(assetManager);
            }

            // 验证动画
            if (animationNames != null && !animationNames.isEmpty()) {
                validateAnimations();
            }

            isLoaded = true;
            System.out.println("模型加载成功: " + modelId);
            return true;

        } catch (Exception e) {
            System.err.println("加载模型 " + modelId + " 时发生错误: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 应用纹理
     */
    private void applyTextures(AssetManager assetManager) {
        for (Map.Entry<String, String> entry : texturePaths.entrySet()) {
            String materialName = entry.getKey();
            String texturePath = entry.getValue();

            try {
                Texture texture = assetManager.loadTexture(texturePath);
                System.out.println("应用纹理: " + materialName + " -> " + texturePath);
            } catch (Exception e) {
                System.err.println("加载纹理失败: " + texturePath + " - " + e.getMessage());
            }
        }
    }

    /**
     * 验证动画
     */
    private void validateAnimations() {
        AnimControl animControl = loadedModel.getControl(AnimControl.class);
        if (animControl != null) {
            for (String animName : animationNames) {
                if (animControl.getAnimationNames().contains(animName)) {
                    System.out.println("发现动画: " + animName);
                } else {
                    System.err.println("动画不存在: " + animName);
                }
            }
        } else {
            System.err.println("模型没有动画控制器: " + modelId);
        }
    }

    /**
     * 创建模型实例
     */
    public Node createInstance() {
        if (!isLoaded || loadedModel == null) {
            System.err.println("模型未加载，无法创建实例: " + modelId);
            return null;
        }

        Node instance = new Node(modelId + "_instance");
        Spatial clonedModel = loadedModel.clone();
        instance.attachChild(clonedModel);

        return instance;
    }

    /**
     * 播放动画
     */
    public void playAnimation(Node modelInstance, String animationName) {
        if (modelInstance == null || animationNames == null || !animationNames.contains(animationName)) {
            return;
        }

        Spatial model = modelInstance.getChild(0);
        if (model != null) {
            AnimControl animControl = model.getControl(AnimControl.class);
            if (animControl != null) {
                AnimChannel channel = animControl.createChannel();
                channel.setAnim(animationName);
                channel.setLoopMode(LoopMode.Loop);
            }
        }
    }

    // Getters
    public String getModelId() { return modelId; }
    public String getModelPath() { return modelPath; }
    public boolean isLoaded() { return isLoaded; }
    public Spatial getLoadedModel() { return loadedModel; }
    public List<String> getAnimationNames() { return animationNames; }
}
