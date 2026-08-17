package com.Hecate.blockbench;

import com.Hecate.loader.AbstractAssetLoader;
import com.Hecate.utils.LogUtils;
import com.jme3.app.SimpleApplication;
import com.jme3.scene.Spatial;

/**
 * Blockbench资源加载器
 */
public class BlockbenchAssetLoader extends AbstractAssetLoader<BlockbenchModel, BlockbenchModelRegistry> {

    /**
     * 构造函数（依赖注入）
     *
     * @param app 应用实例
     * @param registry Blockbench模型注册表（通过依赖注入传入）
     */
    public BlockbenchAssetLoader(SimpleApplication app, BlockbenchModelRegistry registry) {
        super(app.getAssetManager(), registry);
    }

    /**
     * 构造函数（向后兼容）
     *
     * @param app 应用实例
     * @deprecated 推荐使用 {@link #BlockbenchAssetLoader(SimpleApplication, BlockbenchModelRegistry)} 进行依赖注入
     */
    @Deprecated
    public BlockbenchAssetLoader(SimpleApplication app) {
        super(app.getAssetManager(), BlockbenchModelRegistry.getInstance());
    }

    @Override
    protected String getModelTypeName() {
        return "Blockbench";
    }

    @Override
    protected void loadModelsImpl() {
        // 注册土方块模型（目前只有这一种方块）
        registerAndLoadModel("block_dirt", "土方块模型", "Models/blocks/dirt_block.obj");
    }

    @Override
    protected boolean loadModelFile(BlockbenchModel model) {
        try {
            // 尝试加载OBJ文件
            Spatial spatial = assetManager.loadModel(model.getModelPath());

            if (spatial != null) {
                model.setSpatial(spatial);
                return true;
            } else {
                LogUtils.error(getClass(), "模型文件为空: " + model.getModelPath(), null);
                return false;
            }

        } catch (Exception e) {
            LogUtils.error(getClass(), "加载模型失败: " + model.getId(), e);
            return false;
        }
    }

    /**
     * 注册并加载一个简单模型
     */
    private void registerAndLoadModel(String id, String name, String modelPath) {
        // 检查是否已存在
        if (checkExistingModel(id) != null) {
            return;
        }

        // 创建模型对象
        BlockbenchModel model = new BlockbenchModel(id, name, modelPath);

        // 注册到注册表
        registry.registerModel(model);

        // 尝试加载模型文件
        loadModelFile(model);
    }

    /**
     * 获取加载统计信息
     */
    public void printLoadingStats() {
        int loaded = registry.getLoadedModelCount();
        int total = registry.getTotalModelCount();

    }
}
