package com.Hecate.puppet.ecs.component;

import com.Hecate.puppet.ecs.Component;
import com.Hecate.puppet.ecs.Entity;

import java.util.ArrayList;
import java.util.List;

/**
 * 层级组件
 * 管理实体之间的父子关系
 *
 * 用于构建骨骼树结构
 */
public class HierarchyComponent implements Component {

    private Entity parent;
    private final List<Entity> children;
    private boolean enabled;

    public HierarchyComponent() {
        this.parent = null;
        this.children = new ArrayList<>();
        this.enabled = true;
    }

    /**
     * 设置父实体
     *
     * @param parent 父实体（null表示根节点）
     */
    public void setParent(Entity parent) {
        // 从旧父节点移除
        if (this.parent != null) {
            HierarchyComponent parentHierarchy = this.parent.getComponent(HierarchyComponent.class);
            if (parentHierarchy != null) {
                parentHierarchy.removeChild(getOwnerEntity());
            }
        }

        // 设置新父节点
        this.parent = parent;

        // 添加到新父节点的子列表
        if (parent != null) {
            HierarchyComponent parentHierarchy = parent.getComponent(HierarchyComponent.class);
            if (parentHierarchy != null) {
                parentHierarchy.addChild(getOwnerEntity());
            }
        }

        // 标记transform为脏（父节点改变会影响世界变换）
        markTransformDirty();
    }

    /**
     * 获取父实体
     *
     * @return 父实体，如果是根节点返回null
     */
    public Entity getParent() {
        return parent;
    }

    /**
     * 添加子实体
     *
     * @param child 子实体
     */
    public void addChild(Entity child) {
        if (child == null || children.contains(child)) {
            return;
        }

        // 检查是否会形成循环
        if (isAncestor(child)) {
            java.lang.System.err.println("[HierarchyComponent] Cannot add ancestor as child - would create cycle");
            return;
        }

        children.add(child);

        // 设置子实体的父节点
        HierarchyComponent childHierarchy = child.getComponent(HierarchyComponent.class);
        if (childHierarchy != null && childHierarchy.getParent() != getOwnerEntity()) {
            childHierarchy.parent = getOwnerEntity(); // 直接设置，避免递归
        }
    }

    /**
     * 移除子实体
     *
     * @param child 子实体
     */
    public void removeChild(Entity child) {
        if (children.remove(child)) {
            HierarchyComponent childHierarchy = child.getComponent(HierarchyComponent.class);
            if (childHierarchy != null) {
                childHierarchy.parent = null;
            }
        }
    }

    /**
     * 获取所有子实体
     *
     * @return 子实体列表（副本）
     */
    public List<Entity> getChildren() {
        return new ArrayList<>(children);
    }

    /**
     * 获取子实体数量
     *
     * @return 子实体数量
     */
    public int getChildCount() {
        return children.size();
    }

    /**
     * 检查是否有子实体
     *
     * @return true如果有子实体
     */
    public boolean hasChildren() {
        return !children.isEmpty();
    }

    /**
     * 检查是否是根节点
     *
     * @return true如果没有父节点
     */
    public boolean isRoot() {
        return parent == null;
    }

    /**
     * 检查指定实体是否是祖先
     *
     * @param entity 要检查的实体
     * @return true如果是祖先
     */
    public boolean isAncestor(Entity entity) {
        Entity current = parent;
        while (current != null) {
            if (current.equals(entity)) {
                return true;
            }
            HierarchyComponent hierarchy = current.getComponent(HierarchyComponent.class);
            current = (hierarchy != null) ? hierarchy.getParent() : null;
        }
        return false;
    }

    /**
     * 获取所有祖先实体（从根到直接父节点）
     *
     * @return 祖先实体列表
     */
    public List<Entity> getAncestors() {
        List<Entity> ancestors = new ArrayList<>();
        Entity current = parent;
        while (current != null) {
            ancestors.add(0, current); // 插入到开头，使根节点在最前
            HierarchyComponent hierarchy = current.getComponent(HierarchyComponent.class);
            current = (hierarchy != null) ? hierarchy.getParent() : null;
        }
        return ancestors;
    }

    /**
     * 获取层级深度（根节点深度为0）
     *
     * @return 层级深度
     */
    public int getDepth() {
        int depth = 0;
        Entity current = parent;
        while (current != null) {
            depth++;
            HierarchyComponent hierarchy = current.getComponent(HierarchyComponent.class);
            current = (hierarchy != null) ? hierarchy.getParent() : null;
        }
        return depth;
    }

    /**
     * 获取拥有此组件的实体
     * 注意：这需要在组件被添加到实体后才能正常工作
     *
     * @return 拥有此组件的实体
     */
    private Entity getOwnerEntity() {
        // 这是一个简化实现，实际应该通过Entity -> Component的反向引用来获取
        // 或者在设置parent/children时已经知道owner
        // 这里先返回null，实际使用时需要改进
        return null;
    }

    /**
     * 标记transform组件为脏
     * 当层级关系改变时，需要重新计算世界变换
     */
    private void markTransformDirty() {
        Entity owner = getOwnerEntity();
        if (owner != null) {
            TransformComponent transform = owner.getComponent(TransformComponent.class);
            if (transform != null) {
                transform.markDirty();
            }

            // 递归标记所有子节点
            for (Entity child : children) {
                HierarchyComponent childHierarchy = child.getComponent(HierarchyComponent.class);
                if (childHierarchy != null) {
                    childHierarchy.markTransformDirty();
                }
            }
        }
    }

    // ========== Component Interface ==========

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public Component clone() {
        HierarchyComponent cloned = new HierarchyComponent();
        // 注意：克隆时不复制父子关系，需要在克隆实体后重新建立
        return cloned;
    }

    @Override
    public String toString() {
        return String.format("HierarchyComponent[parent=%s, children=%d, depth=%d]",
                parent != null ? parent.getName() : "null", children.size(), getDepth());
    }
}
