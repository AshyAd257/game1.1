package com.Hecate.puppet.ecs.system;

import com.Hecate.puppet.ecs.Component;
import com.Hecate.puppet.ecs.Entity;
import com.Hecate.puppet.ecs.System;
import com.Hecate.puppet.ecs.component.HierarchyComponent;
import com.Hecate.puppet.ecs.component.PhysicsComponent;
import com.Hecate.puppet.ecs.component.TransformComponent;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;

import java.util.List;

/**
 * 物理系统
 * 负责更新PhysicsComponent，实现自由骨骼的摆动效果
 *
 * 【改进的积分方法】
 * 旧实现（FreeBonePhysics）: 显式欧拉法
 *   v(t+dt) = v(t) + a(t) * dt
 *   x(t+dt) = x(t) + v(t) * dt  // 使用旧速度，不稳定
 *
 * 新实现: 半隐式欧拉法（Symplectic Euler）
 *   v(t+dt) = v(t) + a(t) * dt
 *   x(t+dt) = x(t) + v(t+dt) * dt  // 使用新速度，更稳定
 *
 * 优势：
 * - 能量守恒更好
 * - 在大时间步长下更稳定
 * - 不会发散
 */
public class PhysicsSystem implements System {

    private boolean enabled = true;

    // 全局风力
    private Vector3f windForce = new Vector3f(0, 0, 0);

    // 临时变量（复用，避免GC）
    private final Vector3f tempPos = new Vector3f();
    private final Vector3f tempVec = new Vector3f();
    private final Vector3f tempForce = new Vector3f();
    private final Quaternion tempRot = new Quaternion();

    @Override
    public void initialize() {
        java.lang.System.out.println("[PhysicsSystem] Initialized (Semi-implicit Euler)");
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<? extends Component>[] getRequiredComponents() {
        return new Class[]{
            PhysicsComponent.class,
            TransformComponent.class,
            HierarchyComponent.class
        };
    }

    @Override
    public int getPriority() {
        return -40; // 在TransformSystem之后，AnimationSystem之后
    }

    @Override
    public void update(List<Entity> entities, float tpf) {
        for (Entity entity : entities) {
            PhysicsComponent physics = entity.getComponent(PhysicsComponent.class);
            TransformComponent transform = entity.getComponent(TransformComponent.class);
            HierarchyComponent hierarchy = entity.getComponent(HierarchyComponent.class);

            if (physics == null || transform == null || hierarchy == null) {
                continue;
            }

            updatePhysics(physics, transform, hierarchy, tpf);
        }
    }

    /**
     * 更新单个实体的物理
     */
    private void updatePhysics(PhysicsComponent physics, TransformComponent transform,
                              HierarchyComponent hierarchy, float tpf) {
        // 获取当前世界位置
        Vector3f currentWorldPos = transform.getCachedWorldPosition();

        // 获取Rest位置（目标位置）
        Vector3f restPos = transform.getRestPosition();

        // 计算受力
        tempForce.set(0, 0, 0);

        // 1. 重力
        tempVec.set(physics.getGravityDirection()).multLocal(physics.getGravityStrength() * physics.getMass());
        tempForce.addLocal(tempVec);

        // 2. 弹簧回复力（指向Rest位置）
        tempVec.set(restPos).subtractLocal(transform.getLocalPosition()).multLocal(physics.getStiffness());
        tempForce.addLocal(tempVec);

        // 3. 风力（如果启用）
        if (physics.isWindAffected() && windForce.lengthSquared() > 0) {
            tempVec.set(windForce).multLocal(physics.getWindInfluence());
            tempForce.addLocal(tempVec);
        }

        // 4. 检测父骨骼移动产生的惯性力
        if (hierarchy.getParent() != null) {
            TransformComponent parentTransform = hierarchy.getParent().getComponent(TransformComponent.class);
            if (parentTransform != null) {
                Vector3f parentWorldPos = parentTransform.getCachedWorldPosition();
                tempVec.set(physics.getPreviousPosition()).subtractLocal(parentWorldPos);

                if (tempVec.lengthSquared() > 0.0001f) {
                    // 父骨骼移动了，产生惯性力
                    tempVec.multLocal(physics.getMass() * 20.0f / tpf); // 惯性力
                    tempForce.addLocal(tempVec);
                }

                physics.getPreviousPosition().set(parentWorldPos);
            }
        }

        // 应用力
        physics.applyForce(tempForce);

        // 限制加速度
        physics.clampAcceleration();

        // 【半隐式欧拉积分】
        // 1. 更新速度（v_new = v_old + a * dt）
        Vector3f velocity = physics.getVelocity();
        velocity.addLocal(physics.getAcceleration().mult(tpf));

        // 2. 应用阻尼
        velocity.multLocal(physics.getDamping());

        // 3. 限制速度
        physics.clampVelocity();

        // 4. 更新位置（使用新速度 - 这是半隐式的关键）
        tempVec.set(velocity).multLocal(tpf);
        transform.setLocalPosition(transform.getLocalPosition().add(tempVec));

        // 5. 应用角度约束
        applyAngleConstraint(transform, hierarchy, physics);

        // 6. 重置加速度
        physics.getAcceleration().set(0, 0, 0);

        // 标记Transform为脏
        transform.markDirty();
    }

    /**
     * 应用角度约束
     * 限制骨骼相对于父骨骼的摆动角度
     */
    private void applyAngleConstraint(TransformComponent transform, HierarchyComponent hierarchy,
                                     PhysicsComponent physics) {
        if (hierarchy.getParent() == null) {
            return;
        }

        TransformComponent parentTransform = hierarchy.getParent().getComponent(TransformComponent.class);
        if (parentTransform == null) {
            return;
        }

        // 计算相对父骨骼的方向
        tempPos.set(transform.getLocalPosition());
        float length = tempPos.length();

        if (length < 0.001f) {
            return; // 太短，跳过
        }

        tempPos.normalizeLocal();

        // 假设骨骼初始方向是(0,1,0)（向上）
        Vector3f restDirection = new Vector3f(0, 1, 0);

        // 计算角度
        float angle = restDirection.angleBetween(tempPos);
        float maxAngleRad = physics.getMaxSwingAngle() * FastMath.DEG_TO_RAD;

        if (angle > maxAngleRad) {
            // 超过最大角度，限制
            tempRot.fromAngleAxis(maxAngleRad, restDirection.cross(tempPos).normalizeLocal());
            tempVec.set(restDirection);
            tempRot.mult(tempVec, tempVec);
            tempVec.multLocal(length);
            transform.setLocalPosition(tempVec);

            // 同时减小速度（模拟碰撞）
            physics.getVelocity().multLocal(0.5f);
        }
    }

    /**
     * 设置全局风力
     *
     * @param force 风力向量
     */
    public void setWindForce(Vector3f force) {
        this.windForce.set(force);
    }

    /**
     * 获取全局风力
     *
     * @return 风力向量
     */
    public Vector3f getWindForce() {
        return windForce.clone();
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
        java.lang.System.out.println("[PhysicsSystem] Cleaned up");
    }
}
