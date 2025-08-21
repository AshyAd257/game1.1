package com.Hecate.blender;

import com.jme3.asset.AssetManager;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Blender模型注册表
 */
public class BlenderModelRegistry {
    private static BlenderModelRegistry instance;
    private final Map<String, BlenderModel> models = new HashMap<>();

    private BlenderModelRegistry() {}

    public static synchronized BlenderModelRegistry getInstance() {
        if (instance == null) {
            instance = new BlenderModelRegistry();
        }
        return instance;
    }

    /**
     * 注册Blender模型
     */
    public void registerModel(BlenderModel model) {
        models.put(model.getModelId(), model);
        System.out.println("注册Blender模型: " + model.getModelId());
    }

    /**
     * 获取模型
     */
    public BlenderModel getModel(String modelId) {
        return models.get(modelId);
    }

    /**
     * 获取所有模型ID
     */
    public Set<String> getAllModelIds() {
        return models.keySet();
    }

    /**
     * 检查模型是否存在
     */
    public boolean hasModel(String modelId) {
        return models.containsKey(modelId);
    }

    /**
     * 移除模型
     */
    public void removeModel(String modelId) {
        BlenderModel removed = models.remove(modelId);
        if (removed != null) {
            System.out.println("移除Blender模型: " + modelId);
        }
    }

    /**
     * 加载所有注册的模型
     */
    public void loadAllModels(AssetManager assetManager) {
        System.out.println("开始加载所有Blender模型...");
        int successCount = 0;
        int totalCount = models.size();

        for (BlenderModel model : models.values()) {
            if (model.load(assetManager)) {
                successCount++;
            }
        }

        System.out.println("Blender模型加载完成: " + successCount + "/" + totalCount + " 成功");
    }

    /**
     * 获取已加载的模型数量
     */
    public int getLoadedModelCount() {
        int count = 0;
        for (BlenderModel model : models.values()) {
            if (model.isLoaded()) {
                count++;
            }
        }
        return count;
    }

    /**
     * 清空所有模型
     */
    public void clear() {
        models.clear();
        System.out.println("清空所有Blender模型");
    }
}
