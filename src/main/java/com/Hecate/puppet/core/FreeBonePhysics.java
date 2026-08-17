package com.Hecate.puppet.core;

import com.jme3.math.Vector3f;
import com.jme3.math.Quaternion;

/**
 * 自由骨骼物理系统
 * 实现基于物理的摆动效果，模拟头发、尾巴、布料等柔性部件
 */
public class FreeBonePhysics {

    private final Bone bone;

    // 物理状态
    private Vector3f velocity;           // 当前速度
    private Vector3f acceleration;       // 当前加速度
    private Vector3f previousParentPos;  // 上一帧父骨骼位置（用于检测父骨骼移动）

    // 物理参数
    private float mass = 1.0f;           // 质量
    private float damping = 0.95f;       // 阻尼系数（0-1，越小阻尼越大）
    private float stiffness = 50.0f;     // 刚度系数（回弹力强度）
    private float gravityStrength = 9.8f; // 重力强度

    // 约束参数
    private float maxSwingAngle = 45.0f; // 最大摆动角度（度）
    private float maxVelocity = 10.0f;   // 最大速度限制

    // 目标位置（rest位置，相对于父骨骼）
    private Vector3f restOffset;

    /**
     * 构造函数
     * @param bone 要应用物理的骨骼
     */
    public FreeBonePhysics(Bone bone) {
        this.bone = bone;
        this.velocity = new Vector3f(0, 0, 0);
        this.acceleration = new Vector3f(0, 0, 0);
        this.previousParentPos = new Vector3f(0, 0, 0);
        this.restOffset = bone.getLocalPosition().clone();
    }

    /**
     * 更新物理模拟
     * @param tpf 时间步长（秒）
     */
    public void update(float tpf) {
        if (bone.getParent() == null) {
            // 根骨骼不需要物理模拟
            return;
        }

        // 获取父骨骼的世界位置
        Vector3f parentWorldPos = new Vector3f();
        Quaternion parentWorldRot = new Quaternion();
        Vector3f parentWorldScale = new Vector3f();
        bone.getParent().getWorldTransform(parentWorldPos, parentWorldRot, parentWorldScale);

        // 计算父骨骼的移动速度（用于施加惯性力）
        Vector3f parentVelocity = parentWorldPos.subtract(previousParentPos).divide(tpf);
        previousParentPos.set(parentWorldPos);

        // 获取当前骨骼的世界位置
        Vector3f currentWorldPos = new Vector3f();
        Quaternion currentWorldRot = new Quaternion();
        Vector3f currentWorldScale = new Vector3f();
        bone.getWorldTransform(currentWorldPos, currentWorldRot, currentWorldScale);

        // 计算目标位置（父骨骼位置 + rest偏移）
        Vector3f targetWorldPos = parentWorldPos.add(parentWorldRot.mult(restOffset.mult(parentWorldScale)));

        // 【新增】保存目标局部位置（用于shadowCaster，避免物理摆动可见）
        // 将目标世界位置转换为局部坐标
        Vector3f targetLocalPos = targetWorldPos.subtract(parentWorldPos);
        if (parentWorldRot.norm() > 0.0001f) {
            targetLocalPos = parentWorldRot.inverse().mult(targetLocalPos);
        }
        if (parentWorldScale.x != 0 && parentWorldScale.y != 0 && parentWorldScale.z != 0) {
            targetLocalPos = targetLocalPos.divide(parentWorldScale);
        }
        bone.setTargetLocalPosition(targetLocalPos);

        // === 力的计算 ===

        // 1. 回弹力（弹簧力）- 将骨骼拉回目标位置
        Vector3f displacement = targetWorldPos.subtract(currentWorldPos);
        float freedomFactor = bone.getFreedomValue(); // 0-1范围
        float effectiveStiffness = stiffness * (1.0f - freedomFactor * 0.8f); // 自由度越高，刚度越低
        Vector3f springForce = displacement.mult(effectiveStiffness);

        // 2. 重力力
        Vector3f gravityVector = bone.getEffectiveGravityVector();
        Vector3f gravityForce = gravityVector.mult(gravityStrength * mass * freedomFactor);

        // 3. 惯性力（父骨骼移动产生的拖拽）
        Vector3f inertiaForce = parentVelocity.mult(-mass * freedomFactor * 2.0f);

        // 4. 阻尼力（与速度相反）
        Vector3f dampingForce = velocity.mult(-damping * 0.5f);

        // 合力
        Vector3f totalForce = springForce.add(gravityForce).add(inertiaForce).add(dampingForce);

        // === 运动学积分 ===

        // 加速度 = 力 / 质量
        acceleration.set(totalForce.divide(mass));

        // 速度 += 加速度 * 时间
        velocity.addLocal(acceleration.mult(tpf));

        // 速度限制
        float velocityMag = velocity.length();
        if (velocityMag > maxVelocity) {
            velocity.normalizeLocal().multLocal(maxVelocity);
        }

        // 应用阻尼
        float effectiveDamping = 1.0f - (1.0f - damping) * freedomFactor;
        velocity.multLocal(effectiveDamping);

        // 位置 += 速度 * 时间
        Vector3f newWorldPos = currentWorldPos.add(velocity.mult(tpf));

        // === 约束处理 ===

        // 限制摆动角度
        Vector3f toNew = newWorldPos.subtract(parentWorldPos);
        Vector3f toTarget = targetWorldPos.subtract(parentWorldPos);

        if (toNew.lengthSquared() > 0.0001f && toTarget.lengthSquared() > 0.0001f) {
            float angle = toNew.angleBetween(toTarget);
            float maxAngleRad = (float) Math.toRadians(maxSwingAngle * freedomFactor);

            if (angle > maxAngleRad) {
                // 限制在最大角度内
                Vector3f axis = toTarget.cross(toNew).normalizeLocal();
                Quaternion limitRot = new Quaternion().fromAngleAxis(maxAngleRad, axis);
                toNew.set(limitRot.mult(toTarget));
                newWorldPos.set(parentWorldPos.add(toNew));

                // 减少速度（碰到约束边界）
                velocity.multLocal(0.5f);
            }
        }

        // === 更新骨骼位置 ===

        // 将世界位置转换回局部位置
        Vector3f newLocalPos = newWorldPos.subtract(parentWorldPos);
        if (parentWorldRot.norm() > 0.0001f) {
            newLocalPos = parentWorldRot.inverse().mult(newLocalPos);
        }
        if (parentWorldScale.x != 0 && parentWorldScale.y != 0 && parentWorldScale.z != 0) {
            newLocalPos.divideLocal(parentWorldScale);
        }

        bone.setLocalPosition(newLocalPos);
    }

    /**
     * 重置物理状态
     */
    public void reset() {
        velocity.set(0, 0, 0);
        acceleration.set(0, 0, 0);
        bone.setLocalPosition(restOffset.clone());
    }

    /**
     * 设置rest偏移（目标位置）
     * @param restOffset rest偏移向量
     */
    public void setRestOffset(Vector3f restOffset) {
        this.restOffset.set(restOffset);
    }

    /**
     * 初始化父骨骼位置（在第一次更新前调用）
     */
    public void initializeParentPosition() {
        if (bone.getParent() != null) {
            Vector3f parentWorldPos = new Vector3f();
            Quaternion parentWorldRot = new Quaternion();
            Vector3f parentWorldScale = new Vector3f();
            bone.getParent().getWorldTransform(parentWorldPos, parentWorldRot, parentWorldScale);
            previousParentPos.set(parentWorldPos);
        }
    }

    // ========== Getters & Setters ==========

    public float getMass() {
        return mass;
    }

    public void setMass(float mass) {
        this.mass = Math.max(0.1f, mass);
    }

    public float getDamping() {
        return damping;
    }

    public void setDamping(float damping) {
        this.damping = Math.max(0f, Math.min(1f, damping));
    }

    public float getStiffness() {
        return stiffness;
    }

    public void setStiffness(float stiffness) {
        this.stiffness = Math.max(0f, stiffness);
    }

    public float getGravityStrength() {
        return gravityStrength;
    }

    public void setGravityStrength(float gravityStrength) {
        this.gravityStrength = gravityStrength;
    }

    public float getMaxSwingAngle() {
        return maxSwingAngle;
    }

    public void setMaxSwingAngle(float maxSwingAngle) {
        this.maxSwingAngle = Math.max(0f, Math.min(180f, maxSwingAngle));
    }

    public float getMaxVelocity() {
        return maxVelocity;
    }

    public void setMaxVelocity(float maxVelocity) {
        this.maxVelocity = Math.max(0.1f, maxVelocity);
    }

    public Vector3f getVelocity() {
        return velocity.clone();
    }

    public Vector3f getAcceleration() {
        return acceleration.clone();
    }
}
