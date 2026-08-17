package com.Hecate.placer;

import com.Hecate.registry.AbstractModelRegistry;
import com.Hecate.utils.LogUtils;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;

/**
 * 抽象模型放置器基类
 * 提供通用的模型放置和验证功能
 *
 * @param <M> 模型类型
 * @param <R> 模型注册表类型
 */
public abstract class AbstractModelPlacer<M, R extends AbstractModelRegistry<M>> {
    protected final Node worldNode;
    protected final R registry;

    /**
     * 构造函数
     * @param worldNode 世界节点（用于附加模型）
     * @param registry 模型注册表
     */
    public AbstractModelPlacer(Node worldNode, R registry) {
        this.worldNode = worldNode;
        this.registry = registry;
    }

    /**
     * 在指定位置放置模型（带验证）
     * @param modelId 模型ID
     * @param position 世界位置
     * @return 放置成功返回true/Node（根据子类实现）
     */
    protected M getAndValidateModel(String modelId) {
        M model = registry.getModel(modelId);

        if (model == null) {
            LogUtils.error(getClass(), "模型不存在: " + modelId, null);
            return null;
        }

        if (!isModelLoaded(model)) {
            LogUtils.error(getClass(), "模型未加载: " + modelId, null);
            return null;
        }

        return model;
    }

    /**
     * 便捷方法：在指定坐标放置模型
     * @param modelId 模型ID
     * @param x X坐标
     * @param y Y坐标
     * @param z Z坐标
     */
    public Object placeModel(String modelId, float x, float y, float z) {
        return placeModel(modelId, new Vector3f(x, y, z));
    }

    /**
     * 在指定位置放置模型
     * 子类需要实现此方法
     *
     * @param modelId 模型ID
     * @param position 世界位置
     * @return 放置结果（Node或boolean，根据子类实现）
     */
    public abstract Object placeModel(String modelId, Vector3f position);

    /**
     * 判断模型是否已加载
     * 子类需要实现此方法
     *
     * @param model 模型实例
     * @return 已加载返回true
     */
    protected abstract boolean isModelLoaded(M model);

    /**
     * 获取世界节点
     * @return 世界节点
     */
    public Node getWorldNode() {
        return worldNode;
    }

    /**
     * 获取注册表
     * @return 模型注册表
     */
    public R getRegistry() {
        return registry;
    }
}
