package com.Hecate.blender;

import com.Hecate.registry.AbstractModelRegistry;
import com.Hecate.utils.LogUtils;
import com.jme3.asset.AssetManager;

/**
 * Blender模型注册表
 *
 * 重构为支持依赖注入，同时保持向后兼容。
 *
 * 迁移指南：
 * - 旧代码: BlenderModelRegistry.getInstance() (仍可用)
 * - 新代码: new BlenderModelRegistry() (推荐，用于依赖注入)
 */
public class BlenderModelRegistry extends AbstractModelRegistry<BlenderModel> {
    // 默认实例（向后兼容）
    private static BlenderModelRegistry defaultInstance;

    /**
     * Public constructor - 支持创建独立实例
     */
    public BlenderModelRegistry() {
        // 允许创建多个实例
    }

    /**
     * 获取默认实例（向后兼容）
     *
     * @deprecated 推荐使用依赖注入：通过构造器传递 BlenderModelRegistry
     */
    @Deprecated
    public static synchronized BlenderModelRegistry getInstance() {
        if (defaultInstance == null) {
            defaultInstance = new BlenderModelRegistry();
        }
        return defaultInstance;
    }

    /**
     * 获取默认实例（语义更清晰的方法名）
     */
    public static synchronized BlenderModelRegistry getDefaultInstance() {
        return getInstance();
    }

    /**
     * 创建新的独立实例（用于测试、编辑器、多世界等场景）
     */
    public static BlenderModelRegistry createInstance() {
        return new BlenderModelRegistry();
    }

    /**
     * 注册Blender模型（便捷方法）
     */
    public void registerModel(BlenderModel model) {
        registerModel(model.getModelId(), model);
    }

    /**
     * 加载所有注册的模型
     */
    public void loadAllModels(AssetManager assetManager) {

        int successCount = 0;
        int totalCount = models.size();

        for (BlenderModel model : models.values()) {
            if (model.load(assetManager)) {
                successCount++;
            }
        }

    }

    @Override
    protected boolean isModelLoaded(BlenderModel model) {
        return model.isLoaded();
    }

    @Override
    protected String getModelTypeName() {
        return "Blender";
    }
}
