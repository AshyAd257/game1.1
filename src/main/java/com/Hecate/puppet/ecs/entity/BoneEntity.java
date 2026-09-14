package com.Hecate.puppet.ecs.entity;

import com.Hecate.puppet.ecs.Entity;
import com.Hecate.puppet.ecs.component.HierarchyComponent;
import com.Hecate.puppet.ecs.component.TransformComponent;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;

/**
 * 骨骼实体
 * ECS架构中代表一个骨骼节点
 *
 * 这是新架构的核心类，替代旧的Bone类
 * 通过组件组合实现功能，而不是继承和字段膨胀
 */
public class BoneEntity extends Entity {

    /**
     * 创建骨骼实体
     *
     * @param name 骨骼名称
     */
    public BoneEntity(String name) {
        super(name);

        // 添加核心组件
        addComponent(new TransformComponent());
        addComponent(new HierarchyComponent());
    }

    /**
     * 创建骨骼实体（指定初始变换）
     *
     * @param name 骨骼名称
     * @param position 初始位置
     * @param rotation 初始旋转
     * @param scale 初始缩放
     */
    public BoneEntity(String name, Vector3f position, Quaternion rotation, Vector3f scale) {
        super(name);

        // 添加核心组件
        addComponent(new TransformComponent(position, rotation, scale));
        addComponent(new HierarchyComponent());
    }

    // ========== 便捷方法（委托给组件） ==========

    /**
     * 获取Transform组件
     *
     * @return Transform组件
     */
    public TransformComponent getTransform() {
        return getComponent(TransformComponent.class);
    }

    /**
     * 获取Hierarchy组件
     *
     * @return Hierarchy组件
     */
    public HierarchyComponent getHierarchy() {
        return getComponent(HierarchyComponent.class);
    }

    /**
     * 设置父骨骼
     *
     * @param parent 父骨骼实体
     */
    public void setParent(BoneEntity parent) {
        HierarchyComponent hierarchy = getHierarchy();
        if (hierarchy != null) {
            hierarchy.setParent(parent);
        }
    }

    /**
     * 获取父骨骼
     *
     * @return 父骨骼实体，如果是根节点返回null
     */
    public BoneEntity getParent() {
        HierarchyComponent hierarchy = getHierarchy();
        if (hierarchy != null) {
            Entity parent = hierarchy.getParent();
            return (parent instanceof BoneEntity) ? (BoneEntity) parent : null;
        }
        return null;
    }

    /**
     * 添加子骨骼
     *
     * @param child 子骨骼实体
     */
    public void addChild(BoneEntity child) {
        HierarchyComponent hierarchy = getHierarchy();
        if (hierarchy != null) {
            hierarchy.addChild(child);
        }
    }

    /**
     * 移除子骨骼
     *
     * @param child 子骨骼实体
     */
    public void removeChild(BoneEntity child) {
        HierarchyComponent hierarchy = getHierarchy();
        if (hierarchy != null) {
            hierarchy.removeChild(child);
        }
    }

    /**
     * 获取局部位置
     *
     * @return 局部位置
     */
    public Vector3f getLocalPosition() {
        TransformComponent transform = getTransform();
        return transform != null ? transform.getLocalPosition() : new Vector3f();
    }

    /**
     * 设置局部位置
     *
     * @param position 局部位置
     */
    public void setLocalPosition(Vector3f position) {
        TransformComponent transform = getTransform();
        if (transform != null) {
            transform.setLocalPosition(position);
        }
    }

    /**
     * 获取局部旋转
     *
     * @return 局部旋转
     */
    public Quaternion getLocalRotation() {
        TransformComponent transform = getTransform();
        return transform != null ? transform.getLocalRotation() : new Quaternion();
    }

    /**
     * 设置局部旋转
     *
     * @param rotation 局部旋转
     */
    public void setLocalRotation(Quaternion rotation) {
        TransformComponent transform = getTransform();
        if (transform != null) {
            transform.setLocalRotation(rotation);
        }
    }

    /**
     * 获取局部缩放
     *
     * @return 局部缩放
     */
    public Vector3f getLocalScale() {
        TransformComponent transform = getTransform();
        return transform != null ? transform.getLocalScale() : new Vector3f(1, 1, 1);
    }

    /**
     * 设置局部缩放
     *
     * @param scale 局部缩放
     */
    public void setLocalScale(Vector3f scale) {
        TransformComponent transform = getTransform();
        if (transform != null) {
            transform.setLocalScale(scale);
        }
    }

    /**
     * 获取世界位置（从缓存）
     * 注意：需要TransformSystem先更新缓存
     *
     * @return 世界位置
     */
    public Vector3f getWorldPosition() {
        TransformComponent transform = getTransform();
        return transform != null ? transform.getCachedWorldPosition() : new Vector3f();
    }

    /**
     * 获取世界旋转（从缓存）
     *
     * @return 世界旋转
     */
    public Quaternion getWorldRotation() {
        TransformComponent transform = getTransform();
        return transform != null ? transform.getCachedWorldRotation() : new Quaternion();
    }

    /**
     * 获取世界缩放（从缓存）
     *
     * @return 世界缩放
     */
    public Vector3f getWorldScale() {
        TransformComponent transform = getTransform();
        return transform != null ? transform.getCachedWorldScale() : new Vector3f(1, 1, 1);
    }

    /**
     * 重置到Rest姿势
     */
    public void resetToRestPose() {
        TransformComponent transform = getTransform();
        if (transform != null) {
            transform.resetToRestPose();
        }
    }

    @Override
    public String toString() {
        return String.format("BoneEntity[name=%s, id=%d, components=%d]",
                getName(), getId(), getAllComponents().size());
    }
}
