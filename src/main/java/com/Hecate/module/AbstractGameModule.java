package com.Hecate.module;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 游戏模块的抽象基类
 * 提供了大多数方法的默认实现，简化了具体模块的创建
 */
public abstract class AbstractGameModule implements GameModule {

    /**
     * 获取模块依赖列表
     * 默认情况下，没有依赖
     */
    @Override
    public List<ModuleDependency> getDependencies() {
        return Collections.emptyList();
    }

    /**
     * 模块加载时调用
     * 默认实现为空
     */
    @Override
    public void onLoad() {
        // 默认实现为空
    }

    /**
     * 所有模块初始化后调用
     * 默认实现为空
     */
    @Override
    public void onPostInitialize() {
        // 默认实现为空
    }

    /**
     * 游戏循环中每帧调用
     * 默认实现为空
     */
    @Override
    public void onUpdate(float tpf) {
        // 默认实现为空
    }

    /**
     * 模块被禁用时调用
     * 默认实现为空
     */
    @Override
    public void onDisable() {
        // 默认实现为空
    }

    /**
     * 获取模块提供的能力
     * 默认情况下，不提供任何能力
     */
    @Override
    public Set<ModuleCapability> providesCapabilities() {
        return new HashSet<>();
    }

    /**
     * 获取模块需要的能力
     * 默认情况下，不需要任何能力
     */
    @Override
    public Set<ModuleCapability> requiresCapabilities() {
        return new HashSet<>();
    }

    /**
     * 获取模块声明的冲突
     * 默认情况下，没有冲突
     */
    @Override
    public List<ModuleConflict> getDeclaredConflicts() {
        return new ArrayList<>();
    }
}