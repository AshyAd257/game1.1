package com.Hecate.puppet.ecs;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 系统管理器
 * 管理和执行所有系统
 *
 * 负责按优先级顺序执行系统，并过滤符合条件的实体
 */
public class SystemManager {

    private final List<System> systems;
    private final ComponentManager componentManager;
    private boolean needsSort;

    public SystemManager(ComponentManager componentManager) {
        this.systems = new ArrayList<>();
        this.componentManager = componentManager;
        this.needsSort = false;
    }

    /**
     * 添加系统
     *
     * @param system 要添加的系统
     */
    public void addSystem(System system) {
        if (system == null) {
            throw new IllegalArgumentException("System cannot be null");
        }

        if (systems.contains(system)) {
            return; // 已存在
        }

        systems.add(system);
        system.initialize();
        needsSort = true;
    }

    /**
     * 移除系统
     *
     * @param system 要移除的系统
     */
    public void removeSystem(System system) {
        if (systems.remove(system)) {
            system.cleanup();
        }
    }

    /**
     * 移除指定类型的系统
     *
     * @param systemClass 系统类型
     */
    public void removeSystem(Class<? extends System> systemClass) {
        systems.removeIf(system -> {
            if (system.getClass().equals(systemClass)) {
                system.cleanup();
                return true;
            }
            return false;
        });
    }

    /**
     * 获取指定类型的系统
     *
     * @param systemClass 系统类型
     * @param <T> 系统类型
     * @return 系统实例，如果不存在返回null
     */
    @SuppressWarnings("unchecked")
    public <T extends System> T getSystem(Class<T> systemClass) {
        for (System system : systems) {
            if (system.getClass().equals(systemClass)) {
                return (T) system;
            }
        }
        return null;
    }

    /**
     * 更新所有系统
     * 按优先级顺序执行每个系统
     *
     * @param tpf 时间步长（秒）
     */
    public void update(float tpf) {
        // 如果需要，按优先级排序
        if (needsSort) {
            sortSystems();
            needsSort = false;
        }

        // 获取所有激活的实体
        List<Entity> allEntities = componentManager.getActiveEntities();

        // 执行每个启用的系统
        for (System system : systems) {
            if (!system.isEnabled()) {
                continue;
            }

            // 过滤出符合系统要求的实体
            List<Entity> matchingEntities = filterEntities(allEntities, system);

            // 执行系统
            try {
                system.update(matchingEntities, tpf);
            } catch (Exception e) {
                java.lang.System.err.println("[SystemManager] System " + system.getClass().getSimpleName() +
                        " threw exception: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * 过滤符合系统要求的实体
     *
     * @param entities 所有实体
     * @param system 系统
     * @return 符合要求的实体列表
     */
    private List<Entity> filterEntities(List<Entity> entities, System system) {
        Class<? extends Component>[] requiredComponents = system.getRequiredComponents();

        if (requiredComponents == null || requiredComponents.length == 0) {
            return entities; // 系统不需要特定组件，处理所有实体
        }

        // 优化：如果只需要一个组件，直接使用组件索引
        if (requiredComponents.length == 1) {
            return componentManager.getEntitiesWithComponent(requiredComponents[0]);
        }

        // 需要多个组件，使用优化的查询
        return componentManager.getEntitiesWithComponents(requiredComponents);
    }

    /**
     * 按优先级排序系统
     * 优先级数值越小，越早执行
     */
    private void sortSystems() {
        systems.sort(Comparator.comparingInt(System::getPriority));
    }

    /**
     * 获取所有系统
     *
     * @return 系统列表
     */
    public List<System> getAllSystems() {
        return new ArrayList<>(systems);
    }

    /**
     * 清空所有系统
     */
    public void clear() {
        for (System system : systems) {
            system.cleanup();
        }
        systems.clear();
    }

    /**
     * 获取系统数量
     *
     * @return 系统总数
     */
    public int getSystemCount() {
        return systems.size();
    }
}
