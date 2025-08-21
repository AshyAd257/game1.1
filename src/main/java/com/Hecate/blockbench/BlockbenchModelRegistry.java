package com.Hecate.blockbench;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Blockbench模型注册表
 */
public class BlockbenchModelRegistry {
    private static BlockbenchModelRegistry instance;
    private final Map<String, BlockbenchModel> models = new HashMap<>();

    private BlockbenchModelRegistry() {}

    public static synchronized BlockbenchModelRegistry getInstance() {
        if (instance == null) {
            instance = new BlockbenchModelRegistry();
        }
        return instance;
    }

    /**
     * 注册一个Blockbench模型
     */
    public void registerModel(BlockbenchModel model) {
        models.put(model.getId(), model);
        System.out.println("注册Blockbench模型: " + model.getId() + " - " + model.getName());
    }

    /**
     * 根据ID获取模型
     */
    public BlockbenchModel getModel(String id) {
        return models.get(id);
    }

    /**
     * 获取所有注册的模型ID
     */
    public Set<String> getAllModelIds() {
        return models.keySet();
    }

    /**
     * 检查模型是否存在
     */
    public boolean hasModel(String id) {
        return models.containsKey(id);
    }

    /**
     * 获取已加载的模型数量
     */
    public int getLoadedModelCount() {
        return (int) models.values().stream().filter(BlockbenchModel::isLoaded).count();
    }

    /**
     * 获取总模型数量
     */
    public int getTotalModelCount() {
        return models.size();
    }
}
