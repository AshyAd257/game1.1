package com.Hecate.puppet.ecs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 实体类
 * ECS架构中的实体（Entity）- 组件的容器
 *
 * 实体本身不包含数据和逻辑，只是一个ID和组件集合
 */
public class Entity {

    private static long nextId = 1;

    private final long id;
    private final String name;
    private final Map<Class<? extends Component>, Component> components;
    private boolean active;

    /**
     * 创建匿名实体
     */
    public Entity() {
        this("Entity_" + nextId);
    }

    /**
     * 创建命名实体
     *
     * @param name 实体名称
     */
    public Entity(String name) {
        this.id = nextId++;
        this.name = name;
        this.components = new HashMap<>();
        this.active = true;
    }

    /**
     * 添加组件
     *
     * @param component 要添加的组件
     * @param <T> 组件类型
     * @return 实体自身（支持链式调用）
     */
    public <T extends Component> Entity addComponent(T component) {
        if (component == null) {
            throw new IllegalArgumentException("Component cannot be null");
        }
        components.put(component.getComponentType(), component);
        return this;
    }

    /**
     * 获取组件
     *
     * @param componentClass 组件类型
     * @param <T> 组件类型
     * @return 组件实例，如果不存在返回null
     */
    @SuppressWarnings("unchecked")
    public <T extends Component> T getComponent(Class<T> componentClass) {
        return (T) components.get(componentClass);
    }

    /**
     * 移除组件
     *
     * @param componentClass 组件类型
     * @param <T> 组件类型
     * @return 被移除的组件，如果不存在返回null
     */
    @SuppressWarnings("unchecked")
    public <T extends Component> T removeComponent(Class<T> componentClass) {
        return (T) components.remove(componentClass);
    }

    /**
     * 检查是否有指定组件
     *
     * @param componentClass 组件类型
     * @return true如果实体有该组件
     */
    public boolean hasComponent(Class<? extends Component> componentClass) {
        return components.containsKey(componentClass);
    }

    /**
     * 检查是否有所有指定的组件
     *
     * @param componentClasses 组件类型列表
     * @return true如果实体有所有指定的组件
     */
    @SafeVarargs
    public final boolean hasAllComponents(Class<? extends Component>... componentClasses) {
        for (Class<? extends Component> componentClass : componentClasses) {
            if (!hasComponent(componentClass)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 获取所有组件
     *
     * @return 所有组件的列表
     */
    public List<Component> getAllComponents() {
        return new ArrayList<>(components.values());
    }

    /**
     * 清空所有组件
     */
    public void clearComponents() {
        components.clear();
    }

    // ========== Getters ==========

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public String toString() {
        return String.format("Entity[id=%d, name=%s, components=%d, active=%s]",
                id, name, components.size(), active);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Entity entity = (Entity) obj;
        return id == entity.id;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(id);
    }
}
