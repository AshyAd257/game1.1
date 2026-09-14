package com.Hecate.puppet.ecs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 组件管理器
 * 管理所有实体和它们的组件
 *
 * 提供高效的组件查询和实体管理功能
 */
public class ComponentManager {

    private final Map<Long, Entity> entities;
    private final List<Entity> entityList;

    // 组件索引：按组件类型快速查找拥有该组件的实体
    private final Map<Class<? extends Component>, List<Entity>> componentIndex;

    public ComponentManager() {
        this.entities = new HashMap<>();
        this.entityList = new ArrayList<>();
        this.componentIndex = new HashMap<>();
    }

    /**
     * 添加实体
     *
     * @param entity 要添加的实体
     */
    public void addEntity(Entity entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Entity cannot be null");
        }

        if (entities.containsKey(entity.getId())) {
            return; // 已存在
        }

        entities.put(entity.getId(), entity);
        entityList.add(entity);

        // 更新组件索引
        updateComponentIndex(entity);
    }

    /**
     * 移除实体
     *
     * @param entity 要移除的实体
     */
    public void removeEntity(Entity entity) {
        if (entity == null) {
            return;
        }

        entities.remove(entity.getId());
        entityList.remove(entity);

        // 从组件索引中移除
        for (List<Entity> list : componentIndex.values()) {
            list.remove(entity);
        }
    }

    /**
     * 根据ID获取实体
     *
     * @param id 实体ID
     * @return 实体，如果不存在返回null
     */
    public Entity getEntity(long id) {
        return entities.get(id);
    }

    /**
     * 根据名称获取实体
     *
     * @param name 实体名称
     * @return 实体，如果不存在返回null
     */
    public Entity getEntityByName(String name) {
        for (Entity entity : entityList) {
            if (entity.getName().equals(name)) {
                return entity;
            }
        }
        return null;
    }

    /**
     * 获取所有实体
     *
     * @return 所有实体的列表
     */
    public List<Entity> getAllEntities() {
        return new ArrayList<>(entityList);
    }

    /**
     * 获取所有激活的实体
     *
     * @return 所有激活实体的列表
     */
    public List<Entity> getActiveEntities() {
        List<Entity> activeEntities = new ArrayList<>();
        for (Entity entity : entityList) {
            if (entity.isActive()) {
                activeEntities.add(entity);
            }
        }
        return activeEntities;
    }

    /**
     * 获取拥有指定组件的所有实体
     *
     * @param componentClass 组件类型
     * @return 拥有该组件的实体列表
     */
    public List<Entity> getEntitiesWithComponent(Class<? extends Component> componentClass) {
        List<Entity> result = componentIndex.get(componentClass);
        return result != null ? new ArrayList<>(result) : new ArrayList<>();
    }

    /**
     * 获取拥有所有指定组件的实体
     *
     * @param componentClasses 组件类型列表
     * @return 拥有所有指定组件的实体列表
     */
    @SafeVarargs
    public final List<Entity> getEntitiesWithComponents(Class<? extends Component>... componentClasses) {
        if (componentClasses.length == 0) {
            return new ArrayList<>();
        }

        // 从拥有第一个组件的实体开始
        List<Entity> result = getEntitiesWithComponent(componentClasses[0]);

        // 过滤出同时拥有其他所有组件的实体
        for (int i = 1; i < componentClasses.length; i++) {
            Class<? extends Component> componentClass = componentClasses[i];
            result.removeIf(entity -> !entity.hasComponent(componentClass));
        }

        return result;
    }

    /**
     * 更新组件索引
     * 当实体添加或移除组件时调用
     *
     * @param entity 要更新索引的实体
     */
    public void updateComponentIndex(Entity entity) {
        // 先从所有索引中移除
        for (List<Entity> list : componentIndex.values()) {
            list.remove(entity);
        }

        // 重新添加到对应的组件索引
        for (Component component : entity.getAllComponents()) {
            Class<? extends Component> componentClass = component.getComponentType();
            List<Entity> list = componentIndex.computeIfAbsent(
                componentClass, k -> new ArrayList<>()
            );
            if (!list.contains(entity)) {
                list.add(entity);
            }
        }
    }

    /**
     * 清空所有实体
     */
    public void clear() {
        entities.clear();
        entityList.clear();
        componentIndex.clear();
    }

    /**
     * 获取实体数量
     *
     * @return 实体总数
     */
    public int getEntityCount() {
        return entityList.size();
    }

    /**
     * 获取激活实体数量
     *
     * @return 激活实体数量
     */
    public int getActiveEntityCount() {
        int count = 0;
        for (Entity entity : entityList) {
            if (entity.isActive()) {
                count++;
            }
        }
        return count;
    }
}
