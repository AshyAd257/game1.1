package com.Hecate.module;

import java.util.List;
import java.util.Set;

/**
 * 游戏模块接口 - 所有模块必须实现此接口
 */
public interface GameModule {
    /**
     * 获取模块唯一标识符
     */
    String getId();

    /**
     * 获取模块版本
     */
    Version getVersion();

    /**
     * 获取模块依赖列表
     */
    default List<ModuleDependency> getDependencies() {
        return List.of();
    }

    /**
     * 模块加载时调用
     */
    default void onLoad() {}

    /**
     * 模块初始化时调用
     */
    default void onInitialize() {}

    /**
     * 所有模块初始化后调用
     */
    default void onPostInitialize() {}

    /**
     * 游戏循环中每帧调用
     * @param tpf 上一帧到这一帧的时间（秒）
     */
    default void onUpdate(float tpf) {}

    /**
     * 模块被禁用时调用
     */
    default void onDisable() {}

    /**
     * 获取模块提供的能力
     */
    default Set<ModuleCapability> providesCapabilities() {
        return Set.of();
    }

    /**
     * 获取模块需要的能力
     */
    default Set<ModuleCapability> requiresCapabilities() {
        return Set.of();
    }

    /**
     * 获取模块声明的冲突
     */
    default List<ModuleConflict> getDeclaredConflicts() {
        return List.of();
    }

    /**
     * 检查模块是否已启用
     */
    default boolean isEnabled() {
        return true;
    }
}
