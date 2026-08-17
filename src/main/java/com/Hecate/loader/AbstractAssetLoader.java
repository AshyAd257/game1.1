package com.Hecate.loader;

import com.Hecate.registry.AbstractModelRegistry;
import com.Hecate.utils.LogUtils;
import com.jme3.asset.AssetManager;

/**
 * 抽象的资源加载器基础类，提供通用的加载流程与日志。
 *
 * @param <M> 模型类型
 * @param <R> 模型注册表类型
 */
public abstract class AbstractAssetLoader<M, R extends AbstractModelRegistry<M>> {
    protected final AssetManager assetManager;
    protected final R registry;

    /**
     * @param assetManager JME 资源管理器
     * @param registry     模型注册表
     */
    protected AbstractAssetLoader(AssetManager assetManager, R registry) {
        this.assetManager = assetManager;
        this.registry = registry;
    }

    /**
     * 加载默认模型集合，子类通过 {@link #loadModelsImpl()} 注入具体实现。
     */
    public void loadDefaultModels() {
        try {
            loadModelsImpl();
        } catch (Exception e) {

        }
    }

    /**
     * 根据 ID 重新加载模型配置。
     *
     * @param modelId 模型 ID
     * @return 加载成功返回 true
     */
    public boolean reloadModel(String modelId) {
        M model = registry.getModel(modelId);
        if (model != null) {
            return loadModelFile(model);
        }
        LogUtils.error(getClass(), "模型不存在: " + modelId, null);
        return false;
    }

    public AssetManager getAssetManager() {
        return assetManager;
    }

    public R getRegistry() {
        return registry;
    }

    /**
     * 如果注册表中已存在指定模型，则返回该模型并记录日志。
     */
    protected M checkExistingModel(String modelId) {
        if (registry.hasModel(modelId)) {
            return registry.getModel(modelId);
        }
        return null;
    }

    /**
     * @return 模型类型名称，用于日志输出。
     */
    protected abstract String getModelTypeName();

    /**
     * 子类实现：加载默认模型集合。
     */
    protected abstract void loadModelsImpl();

    /**
     * 子类实现：加载单个模型文件。
     *
     * @param model 模型实例
     * @return 加载成功返回 true
     */
    protected abstract boolean loadModelFile(M model);
}
