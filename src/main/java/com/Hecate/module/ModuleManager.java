package com.Hecate.module;

import com.jme3.app.SimpleApplication;
import com.Hecate.event.EventBus;
import com.Hecate.event.ModuleLoadedEvent;

import java.util.*;

/**
 * 管理游戏模块的加载、初始化和运行
 */
public class ModuleManager {
    private final Map<String, GameModule> loadedModules = new LinkedHashMap<>();
    private final List<GameModule> updateOrder = new ArrayList<>();
    private final Map<String, Set<GameModule>> capabilityProviders = new HashMap<>();
    private final SimpleApplication app;
    private final EventBus eventBus;

    /**
     * 创建模块管理器
     * @param app 游戏应用实例
     * @param eventBus 事件总线
     */
    public ModuleManager(SimpleApplication app, EventBus eventBus) {
        this.app = app;
        this.eventBus = eventBus;
    }

    /**
     * 加载单个模块
     * @param module 要加载的模块
     */
    public void loadModule(GameModule module) {
        String moduleId = module.getId();

        // 检查模块是否已加载
        if (loadedModules.containsKey(moduleId)) {
            System.out.println("模块已加载: " + moduleId);
            return;
        }

        System.out.println("加载模块: " + moduleId + " v" + module.getVersion());

        // 检查冲突
        checkConflicts(module);

        // 检查依赖
        checkDependencies(module);

        // 检查能力需求
        checkCapabilityRequirements(module);

        try {
            // 加载模块
            module.onLoad();
            loadedModules.put(moduleId, module);

            // 注册提供的能力
            registerCapabilities(module);

            // 发布模块加载事件
            eventBus.publish(new ModuleLoadedEvent(module));

            System.out.println("模块加载成功: " + moduleId);
        } catch (Exception e) {
            System.err.println("加载模块失败: " + moduleId);
            e.printStackTrace();
            throw new RuntimeException("模块加载失败: " + moduleId, e);
        }
    }

    /**
     * 初始化所有已加载的模块
     */
    public void initializeAll() {
        System.out.println("开始初始化所有模块...");

        // 按依赖顺序排序
        List<GameModule> sortedModules = sortModulesByDependencies();

        // 初始化模块
        for (GameModule module : sortedModules) {
            try {
                System.out.println("初始化模块: " + module.getId());
                module.onInitialize();
                updateOrder.add(module);
            } catch (Exception e) {
                System.err.println("初始化模块失败: " + module.getId());
                e.printStackTrace();
            }
        }

        System.out.println("所有模块初始化完成");
    }

    /**
     * 调用所有已加载模块的postInitialize方法
     * 在所有模块加载完成后调用
     */
    public void postInitializeAll() {
        System.out.println("开始执行模块后初始化...");

        for (GameModule module : updateOrder) {
            try {
                System.out.println("后初始化模块: " + module.getId());
                module.onPostInitialize();
            } catch (Exception e) {
                System.err.println("模块后初始化异常: " + module.getId());
                e.printStackTrace();
            }
        }

        System.out.println("所有模块后初始化完成");
    }

    /**
     * 更新所有已加载的模块
     * 在游戏循环中调用
     * @param tpf 时间增量（秒）
     */
    public void update(float tpf) {
        for (GameModule module : updateOrder) {
            try {
                if (module.isEnabled()) {
                    module.onUpdate(tpf);
                }
            } catch (Exception e) {
                System.err.println("模块更新异常: " + module.getId());
                e.printStackTrace();
            }
        }
    }

    /**
     * 检查模块冲突
     */
    private void checkConflicts(GameModule module) {
        for (ModuleConflict conflict : module.getDeclaredConflicts()) {
            GameModule conflictingModule = loadedModules.get(conflict.getConflictingModuleId());
            if (conflictingModule != null &&
                    conflict.getConflictingVersions().contains(conflictingModule.getVersion())) {
                throw new IllegalStateException(
                        "模块冲突: " + module.getId() + " 与 " + conflict.getConflictingModuleId() +
                                " v" + conflictingModule.getVersion() + " 冲突 - " + conflict.getReason()
                );
            }
        }

        // 检查其他模块是否与当前模块冲突
        for (GameModule existingModule : loadedModules.values()) {
            for (ModuleConflict conflict : existingModule.getDeclaredConflicts()) {
                if (conflict.getConflictingModuleId().equals(module.getId()) &&
                        conflict.getConflictingVersions().contains(module.getVersion())) {
                    throw new IllegalStateException(
                            "模块冲突: " + existingModule.getId() + " 与 " + module.getId() +
                                    " v" + module.getVersion() + " 冲突 - " + conflict.getReason()
                    );
                }
            }
        }
    }

    /**
     * 检查依赖
     */
    private void checkDependencies(GameModule module) {
        for (ModuleDependency dependency : module.getDependencies()) {
            GameModule dependencyModule = loadedModules.get(dependency.getModuleId());

            if (dependencyModule == null) {
                if (!dependency.isOptional()) {
                    throw new IllegalStateException(
                            "模块 " + module.getId() + " 缺少必需依赖: " + dependency.getModuleId()
                    );
                } else {
                    System.out.println("模块 " + module.getId() + " 的可选依赖未找到: " + dependency.getModuleId());
                }
                continue;
            }

            // 检查版本兼容性
            if (!dependency.getCompatibleVersions().contains(dependencyModule.getVersion())) {
                throw new IllegalStateException(
                        "模块 " + module.getId() + " 依赖版本不兼容: " + dependency.getModuleId() +
                                " (需要: " + dependency.getCompatibleVersions() +
                                ", 实际: " + dependencyModule.getVersion() + ")"
                );
            }
        }
    }

    /**
     * 检查能力需求
     */
    private void checkCapabilityRequirements(GameModule module) {
        for (ModuleCapability requiredCapability : module.requiresCapabilities()) {
            Set<GameModule> providers = capabilityProviders.get(requiredCapability.getCapabilityId());

            if (providers == null || providers.isEmpty()) {
                throw new IllegalStateException(
                        "模块 " + module.getId() + " 需要的能力未找到: " + requiredCapability
                );
            }

            // 检查版本兼容性
            boolean compatible = false;
            for (GameModule provider : providers) {
                for (ModuleCapability providedCapability : provider.providesCapabilities()) {
                    if (providedCapability.getCapabilityId().equals(requiredCapability.getCapabilityId()) &&
                            providedCapability.getVersion().compareTo(requiredCapability.getVersion()) >= 0) {
                        compatible = true;
                        break;
                    }
                }
                if (compatible) break;
            }

            if (!compatible) {
                throw new IllegalStateException(
                        "模块 " + module.getId() + " 需要的能力版本不兼容: " + requiredCapability
                );
            }
        }
    }

    /**
     * 注册模块提供的能力
     */
    private void registerCapabilities(GameModule module) {
        for (ModuleCapability capability : module.providesCapabilities()) {
            capabilityProviders.computeIfAbsent(capability.getCapabilityId(), k -> new HashSet<>()).add(module);
        }
    }

    /**
     * 按依赖关系排序模块
     */
    private List<GameModule> sortModulesByDependencies() {
        List<GameModule> sorted = new ArrayList<>();
        Set<String> processed = new HashSet<>();
        Set<String> processing = new HashSet<>();

        for (GameModule module : loadedModules.values()) {
            sortModule(module, sorted, processed, processing);
        }

        return sorted;
    }

    private void sortModule(GameModule module, List<GameModule> sorted,
                            Set<String> processed, Set<String> processing) {
        String moduleId = module.getId();

        if (processed.contains(moduleId)) {
            return;
        }

        if (processing.contains(moduleId)) {
            throw new IllegalStateException("检测到循环依赖: " + moduleId);
        }

        processing.add(moduleId);

        // 先处理依赖
        for (ModuleDependency dependency : module.getDependencies()) {
            GameModule depModule = loadedModules.get(dependency.getModuleId());
            if (depModule != null) {
                sortModule(depModule, sorted, processed, processing);
            }
        }

        processing.remove(moduleId);
        processed.add(moduleId);
        sorted.add(module);
    }

    /**
     * 根据ID获取已加载的模块
     */
    public GameModule getModule(String id) {
        return loadedModules.get(id);
    }

    /**
     * 获取指定类型的模块
     */
    @SuppressWarnings("unchecked")
    public <T extends GameModule> T getModule(String id, Class<T> moduleClass) {
        GameModule module = loadedModules.get(id);
        if (module != null && moduleClass.isInstance(module)) {
            return (T) module;
        }
        return null;
    }

    /**
     * 获取所有已加载的模块
     */
    public Collection<GameModule> getAllModules() {
        return loadedModules.values();
    }

    /**
     * 获取提供指定能力的模块
     */
    public Set<GameModule> getCapabilityProviders(String capabilityId) {
        return capabilityProviders.getOrDefault(capabilityId, Set.of());
    }

    /**
     * 禁用并卸载指定ID的模块
     */
    public void disableModule(String id) {
        GameModule module = loadedModules.get(id);
        if (module != null) {
            try {
                module.onDisable();
                loadedModules.remove(id);
                updateOrder.remove(module);

                // 移除能力注册
                for (ModuleCapability capability : module.providesCapabilities()) {
                    Set<GameModule> providers = capabilityProviders.get(capability.getCapabilityId());
                    if (providers != null) {
                        providers.remove(module);
                        if (providers.isEmpty()) {
                            capabilityProviders.remove(capability.getCapabilityId());
                        }
                    }
                }

                System.out.println("模块已禁用: " + id);
            } catch (Exception e) {
                System.err.println("禁用模块失败: " + id);
                e.printStackTrace();
            }
        }
    }
}
