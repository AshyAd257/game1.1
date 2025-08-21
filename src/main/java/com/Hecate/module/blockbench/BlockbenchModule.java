package com.Hecate.module.blockbench;

import com.Hecate.blockbench.BlockbenchAssetLoader;
import com.Hecate.blockbench.BlockbenchModelPlacer;
import com.Hecate.blockbench.BlockbenchModelRegistry;
import com.Hecate.module.AbstractGameModule;
import com.Hecate.module.Version;
import com.jme3.app.SimpleApplication;

/**
 * Blockbench模块 - 管理Blockbench模型的加载和使用
 */
public class BlockbenchModule extends AbstractGameModule {
    private final SimpleApplication app;
    private BlockbenchAssetLoader assetLoader;
    private BlockbenchModelPlacer modelPlacer;

    public BlockbenchModule(SimpleApplication app) {
        this.app = app;
    }

    @Override
    public String getId() {
        return "blockbench";
    }

    @Override
    public Version getVersion() {
        return new Version(1, 0, 0);
    }

    @Override
    public void onInitialize() {
        System.out.println("初始化Blockbench模块...");

        try {
            // 初始化资源加载器
            assetLoader = new BlockbenchAssetLoader(app);

            // 初始化模型放置器
            modelPlacer = new BlockbenchModelPlacer(app.getRootNode());

            // 加载默认模型
            assetLoader.loadDefaultModels();

            System.out.println("✅ Blockbench模块初始化完成");

        } catch (Exception e) {
            System.err.println("❌ Blockbench模块初始化失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onPostInitialize() {
        // 打印加载统计
        if (assetLoader != null) {
            assetLoader.printLoadingStats();
        }
    }

    @Override
    public void onDisable() {
        System.out.println("禁用Blockbench模块...");

        // 清理资源
        if (modelPlacer != null) {
            modelPlacer.clearAllModels();
        }
    }

    // Getter方法供其他模块使用
    public BlockbenchAssetLoader getAssetLoader() {
        return assetLoader;
    }

    public BlockbenchModelPlacer getModelPlacer() {
        return modelPlacer;
    }

    public BlockbenchModelRegistry getModelRegistry() {
        return BlockbenchModelRegistry.getInstance();
    }
}
