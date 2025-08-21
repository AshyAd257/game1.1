package com.Hecate.blender;

import com.jme3.asset.AssetManager;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

/**
 * Blender资源加载器
 * 提供便捷的方法来创建和注册Blender模型
 */
public class BlenderAssetLoader {
    private final AssetManager assetManager;
    private final BlenderModelRegistry registry;

    public BlenderAssetLoader(AssetManager assetManager) {
        this.assetManager = assetManager;
        this.registry = BlenderModelRegistry.getInstance();
    }

    /**
     * 加载简单模型（无动画，无纹理）
     */
    public BlenderModel loadSimpleModel(String modelId, String modelPath) {
        return loadModel(modelId, modelPath, null, null);
    }

    /**
     * 加载带纹理的模型
     */
    public BlenderModel loadModelWithTextures(String modelId, String modelPath,
                                              Map<String, String> texturePaths) {
        return loadModel(modelId, modelPath, texturePaths, null);
    }

    /**
     * 加载带动画的模型
     */
    public BlenderModel loadAnimatedModel(String modelId, String modelPath,
                                          String... animationNames) {
        return loadModel(modelId, modelPath, null, Arrays.asList(animationNames));
    }

    /**
     * 加载完整模型（纹理+动画）
     */
    public BlenderModel loadCompleteModel(String modelId, String modelPath,
                                          Map<String, String> texturePaths,
                                          String... animationNames) {
        return loadModel(modelId, modelPath, texturePaths, Arrays.asList(animationNames));
    }

    /**
     * 加载完整模型（纹理+动画）
     */
    public BlenderModel loadModel(String modelId, String modelPath,
                                  Map<String, String> texturePaths,
                                  List<String> animationNames) {

        // 检查模型是否已存在
        if (registry.hasModel(modelId)) {
            System.out.println("模型已存在: " + modelId);
            return registry.getModel(modelId);
        }

        BlenderModel model = new BlenderModel(modelId, modelPath, texturePaths, animationNames);

        // 立即加载模型
        if (model.load(assetManager)) {
            registry.registerModel(model);
            return model;
        } else {
            System.err.println("模型加载失败: " + modelId);
            return null;
        }
    }

    /**
     * 批量加载预定义的模型
     */
    public void loadDefaultModels() {
        System.out.println("加载默认Blender模型...");

        try {
            // 示例：加载一些常用模型
            // 注意：这些路径需要根据您的实际资源文件调整

            // 加载简单的树模型
            loadSimpleModel("tree_oak", "Models/Blender/tree_oak.j3o");

            // 加载带纹理的房屋模型
            Map<String, String> houseTextures = new HashMap<>();
            houseTextures.put("walls", "Textures/Blender/house_walls.png");
            houseTextures.put("roof", "Textures/Blender/house_roof.png");
            loadModelWithTextures("house_basic", "Models/Blender/house_basic.j3o", houseTextures);

            // 加载带动画的角色模型
            loadAnimatedModel("character_player", "Models/Blender/character.j3o",
                    "idle", "walk", "run", "jump");

        } catch (Exception e) {
            System.err.println("加载默认模型时出现错误: ");
            e.printStackTrace();
        }

        System.out.println("默认Blender模型加载完成");
    }

    /**
     * 创建纹理映射的辅助方法
     */
    public static Map<String, String> createTextureMap(String... pairs) {
        Map<String, String> textureMap = new HashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            if (i + 1 < pairs.length) {
                textureMap.put(pairs[i], pairs[i + 1]);
            }
        }
        return textureMap;
    }

    /**
     * 重新加载指定模型
     */
    public boolean reloadModel(String modelId) {
        BlenderModel model = registry.getModel(modelId);
        if (model != null) {
            return model.load(assetManager);
        }
        return false;
    }

    /**
     * 获取资源管理器
     */
    public AssetManager getAssetManager() {
        return assetManager;
    }
}
