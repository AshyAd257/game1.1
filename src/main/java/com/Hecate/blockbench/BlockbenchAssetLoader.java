package com.Hecate.blockbench;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.AssetManager;
import com.jme3.scene.Spatial;

/**
 * Blockbench资源加载器
 */
public class BlockbenchAssetLoader {
    private final AssetManager assetManager;
    private final BlockbenchModelRegistry registry;

    public BlockbenchAssetLoader(SimpleApplication app) {
        this.assetManager = app.getAssetManager();
        this.registry = BlockbenchModelRegistry.getInstance();
    }

    /**
     * 加载默认的Blockbench模型
     */
    public void loadDefaultModels() {
        System.out.println("开始加载默认Blockbench模型...");

        try {
            // 注册土方块模型（目前你只有这一种方块）
            registerModel("block_dirt", "土方块模型", "Models/blocks/dirt_block.obj");

            System.out.println("默认Blockbench模型加载完成");
        } catch (Exception e) {
            System.err.println("加载默认Blockbench模型时出现错误: ");
            e.printStackTrace();
        }
    }

    /**
     * 注册并加载一个简单模型
     */
    private void registerModel(String id, String name, String modelPath) {
        // 创建模型对象
        BlockbenchModel model = new BlockbenchModel(id, name, modelPath);

        // 注册到注册表
        registry.registerModel(model);

        // 尝试加载模型文件
        loadModelFile(model);
    }

    /**
     * 加载模型文件
     */
    private void loadModelFile(BlockbenchModel model) {
        try {
            System.out.println("加载模型文件: " + model.getModelPath());

            // 尝试加载OBJ文件
            Spatial spatial = assetManager.loadModel(model.getModelPath());

            if (spatial != null) {
                model.setSpatial(spatial);
                System.out.println("✅ 模型加载成功: " + model.getId());
            } else {
                System.err.println("❌ 模型文件为空: " + model.getModelPath());
            }

        } catch (Exception e) {
            System.err.println("❌ 加载模型失败: " + model.getId() + " - " + e.getMessage());
            // 不抛出异常，允许游戏继续运行
        }
    }

    /**
     * 重新加载指定模型
     */
    public boolean reloadModel(String modelId) {
        BlockbenchModel model = registry.getModel(modelId);
        if (model != null) {
            loadModelFile(model);
            return model.isLoaded();
        }
        return false;
    }

    /**
     * 获取加载统计信息
     */
    public void printLoadingStats() {
        int loaded = registry.getLoadedModelCount();
        int total = registry.getTotalModelCount();
        System.out.println("Blockbench模型加载统计: " + loaded + "/" + total + " 个模型已加载");
    }
}
