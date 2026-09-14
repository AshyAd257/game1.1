package com.Hecate.puppet.ecs.system;

import com.Hecate.puppet.ecs.Component;
import com.Hecate.puppet.ecs.Entity;
import com.Hecate.puppet.ecs.System;
import com.Hecate.puppet.ecs.component.HierarchyComponent;
import com.Hecate.puppet.ecs.component.TransformComponent;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;

import java.util.List;

/**
 * 变换系统
 * 负责更新实体的世界变换，实现高效的缓存机制
 *
 * 【性能优化核心】
 * 解决原Bone类的性能问题：
 * - 旧实现：每次调用getWorldTransform()都递归计算，O(n*m)复杂度
 * - 新实现：脏标记+缓存，只在变化时重新计算，O(n)复杂度
 *
 * 对于10个骨骼，每帧5次查询：
 * - 旧实现：50次递归计算
 * - 新实现：10次计算（首次）+ 5*10次缓存读取
 * - 性能提升：约5倍
 */
public class TransformSystem implements System {

    private boolean enabled = true;

    // 临时变量（复用，避免GC）
    private final Vector3f tempPos = new Vector3f();
    private final Quaternion tempRot = new Quaternion();
    private final Vector3f tempScale = new Vector3f();

    @Override
    public void initialize() {
        java.lang.System.out.println("[TransformSystem] Initialized");
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<? extends Component>[] getRequiredComponents() {
        return new Class[]{TransformComponent.class, HierarchyComponent.class};
    }

    @Override
    public int getPriority() {
        return -100; // 高优先级，在其他系统之前执行
    }

    @Override
    public void update(List<Entity> entities, float tpf) {
        if (entities.isEmpty()) {
            return;
        }

        // 按层级深度排序，确保父节点先于子节点更新
        // 这样子节点可以直接使用父节点的缓存
        entities.sort((e1, e2) -> {
            HierarchyComponent h1 = e1.getComponent(HierarchyComponent.class);
            HierarchyComponent h2 = e2.getComponent(HierarchyComponent.class);
            int depth1 = h1 != null ? h1.getDepth() : 0;
            int depth2 = h2 != null ? h2.getDepth() : 0;
            return Integer.compare(depth1, depth2);
        });

        // 更新所有实体的世界变换
        for (Entity entity : entities) {
            updateWorldTransform(entity);
        }
    }

    /**
     * 更新单个实体的世界变换
     *
     * @param entity 要更新的实体
     */
    private void updateWorldTransform(Entity entity) {
        TransformComponent transform = entity.getComponent(TransformComponent.class);
        HierarchyComponent hierarchy = entity.getComponent(HierarchyComponent.class);

        if (transform == null) {
            return;
        }

        // 如果不脏，使用缓存（性能优化的核心）
        if (!transform.isDirty()) {
            return;
        }

        // 根节点：世界变换 = 局部变换
        if (hierarchy == null || hierarchy.isRoot()) {
            transform.setCachedWorldTransform(
                transform.getLocalPosition(),
                transform.getLocalRotation(),
                transform.getLocalScale()
            );
            return;
        }

        // 子节点：世界变换 = 父世界变换 * 局部变换
        Entity parent = hierarchy.getParent();
        if (parent == null) {
            // 没有父节点，当作根节点处理
            transform.setCachedWorldTransform(
                transform.getLocalPosition(),
                transform.getLocalRotation(),
                transform.getLocalScale()
            );
            return;
        }

        TransformComponent parentTransform = parent.getComponent(TransformComponent.class);
        if (parentTransform == null) {
            // 父节点没有Transform组件，使用局部变换
            transform.setCachedWorldTransform(
                transform.getLocalPosition(),
                transform.getLocalRotation(),
                transform.getLocalScale()
            );
            return;
        }

        // 【关键优化】直接使用父节点的缓存，不需要递归计算
        Vector3f parentWorldPos = parentTransform.getCachedWorldPosition();
        Quaternion parentWorldRot = parentTransform.getCachedWorldRotation();
        Vector3f parentWorldScale = parentTransform.getCachedWorldScale();

        // 计算世界变换
        // 位置 = 父位置 + 父旋转 * (父缩放 * 局部位置)
        tempPos.set(transform.getLocalPosition());
        tempPos.multLocal(parentWorldScale); // 先缩放
        parentWorldRot.mult(tempPos, tempPos); // 再旋转
        tempPos.addLocal(parentWorldPos); // 最后平移

        // 旋转 = 父旋转 * 局部旋转
        tempRot.set(parentWorldRot);
        tempRot.multLocal(transform.getLocalRotation());

        // 缩放 = 父缩放 * 局部缩放
        tempScale.set(parentWorldScale);
        tempScale.multLocal(transform.getLocalScale());

        // 缓存计算结果
        transform.setCachedWorldTransform(tempPos, tempRot, tempScale);

        // 标记所有子节点为脏（级联更新）
        markChildrenDirty(entity, hierarchy);
    }

    /**
     * 递归标记所有子节点的transform为脏
     *
     * @param entity 父实体
     * @param hierarchy 父实体的层级组件
     */
    private void markChildrenDirty(Entity entity, HierarchyComponent hierarchy) {
        if (hierarchy == null || !hierarchy.hasChildren()) {
            return;
        }

        for (Entity child : hierarchy.getChildren()) {
            TransformComponent childTransform = child.getComponent(TransformComponent.class);
            if (childTransform != null) {
                childTransform.markDirty();
            }

            // 递归标记子节点的子节点
            HierarchyComponent childHierarchy = child.getComponent(HierarchyComponent.class);
            if (childHierarchy != null) {
                markChildrenDirty(child, childHierarchy);
            }
        }
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public void cleanup() {
        java.lang.System.out.println("[TransformSystem] Cleaned up");
    }
}
