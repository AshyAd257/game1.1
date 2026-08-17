package com.Hecate.registry;

import com.Hecate.utils.LogUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 抽象模型注册表基类
 * 提供通用的模型注册、查询和管理功能
 *
 * @param <T> 模型类型
 */
public abstract class AbstractModelRegistry<T> {
    protected final Map<String, T> models = new HashMap<>();

    /**
     * 注册模型
     * @param id 模型ID
     * @param model 模型实例
     */
    public void registerModel(String id, T model) {
        models.put(id, model);
        onModelRegistered(id, model);

    }

    /**
     * 获取模型
     * @param id 模型ID
     * @return 模型实例，不存在时返回null
     */
    public T getModel(String id) {
        return models.get(id);
    }

    /**
     * 获取所有模型ID
     * @return 模型ID集合
     */
    public Set<String> getAllModelIds() {
        return models.keySet();
    }

    /**
     * 检查模型是否存在
     * @param id 模型ID
     * @return 存在返回true
     */
    public boolean hasModel(String id) {
        return models.containsKey(id);
    }

    /**
     * 移除模型
     * @param id 模型ID
     * @return 被移除的模型，不存在返回null
     */
    public T removeModel(String id) {
        T removed = models.remove(id);
        if (removed != null) {
            onModelRemoved(id, removed);

        }
        return removed;
    }

    /**
     * 获取已加载的模型数量
     * @return 已加载模型数量
     */
    public int getLoadedModelCount() {
        return (int) models.values().stream()
                .filter(this::isModelLoaded)
                .count();
    }

    /**
     * 获取总模型数量
     * @return 总模型数量
     */
    public int getTotalModelCount() {
        return models.size();
    }

    /**
     * 清空所有模型
     */
    public void clear() {
        models.clear();

    }

    /**
     * 判断模型是否已加载
     * 子类需要实现此方法来定义加载状态的判断逻辑
     *
     * @param model 模型实例
     * @return 已加载返回true
     */
    protected abstract boolean isModelLoaded(T model);

    /**
     * 获取模型类型名称（用于日志输出）
     *
     * @return 模型类型名称
     */
    protected abstract String getModelTypeName();

    /**
     * 模型注册完成后的回调
     * 子类可以重写此方法来执行额外操作
     *
     * @param id 模型ID
     * @param model 模型实例
     */
    protected void onModelRegistered(String id, T model) {
        // 默认空实现
    }

    /**
     * 模型移除后的回调
     * 子类可以重写此方法来执行清理操作
     *
     * @param id 模型ID
     * @param model 模型实例
     */
    protected void onModelRemoved(String id, T model) {
        // 默认空实现
    }
}
