package com.Hecate.blockbench;

import com.Hecate.registry.AbstractModelRegistry;

/**
 * Blockbench模型注册表
 *
 * 重构为支持依赖注入，同时保持向后兼容。
 *
 * 迁移指南：
 * - 旧代码: BlockbenchModelRegistry.getInstance() (仍可用)
 * - 新代码: new BlockbenchModelRegistry() (推荐，用于依赖注入)
 */
public class BlockbenchModelRegistry extends AbstractModelRegistry<BlockbenchModel> {
    // 默认实例（向后兼容）
    private static BlockbenchModelRegistry defaultInstance;

    /**
     * Public constructor - 支持创建独立实例
     */
    public BlockbenchModelRegistry() {
        // 允许创建多个实例
    }

    /**
     * 获取默认实例（向后兼容）
     *
     * @deprecated 推荐使用依赖注入：通过构造器传递 BlockbenchModelRegistry
     */
    @Deprecated
    public static synchronized BlockbenchModelRegistry getInstance() {
        if (defaultInstance == null) {
            defaultInstance = new BlockbenchModelRegistry();
        }
        return defaultInstance;
    }

    /**
     * 获取默认实例（语义更清晰的方法名）
     */
    public static synchronized BlockbenchModelRegistry getDefaultInstance() {
        return getInstance();
    }

    /**
     * 创建新的独立实例（用于测试、编辑器、多世界等场景）
     */
    public static BlockbenchModelRegistry createInstance() {
        return new BlockbenchModelRegistry();
    }

    /**
     * 注册Blockbench模型（便捷方法）
     */
    public void registerModel(BlockbenchModel model) {
        registerModel(model.getId(), model);
    }

    @Override
    protected boolean isModelLoaded(BlockbenchModel model) {
        return model.isLoaded();
    }

    @Override
    protected String getModelTypeName() {
        return "Blockbench";
    }
}
