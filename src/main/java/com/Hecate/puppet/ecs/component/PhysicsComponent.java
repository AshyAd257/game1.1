package com.Hecate.puppet.ecs.component;

import com.Hecate.puppet.ecs.Component;
import com.jme3.math.Vector3f;

/**
 * 物理组件
 * 存储实体的物理属性，用于模拟自由骨骼的摆动效果
 *
 * 【改进的物理模拟】
 * 旧实现（FreeBonePhysics）:
 * - 使用显式欧拉积分（不稳定）
 * - 容易产生振荡
 *
 * 新实现:
 * - 使用半隐式欧拉积分（更稳定）
 * - 更好的能量守恒
 * - 可调节的物理参数
 *
 * 【使用场景】
 * - 头发摆动
 * - 尾巴摇摆
 * - 布料模拟
 * - 柔性部件
 */
public class PhysicsComponent implements Component {

    // 物理状态
    private Vector3f velocity;          // 当前速度
    private Vector3f acceleration;      // 当前加速度
    private Vector3f previousPosition;  // 上一帧位置（用于检测运动）

    // 物理参数
    private float mass;                 // 质量
    private float damping;              // 阻尼系数（0-1，越小阻尼越大）
    private float stiffness;            // 刚度系数（回弹力强度）
    private float gravityStrength;      // 重力强度

    // 重力方向
    private Vector3f gravityDirection;

    // 约束参数
    private float maxSwingAngle;        // 最大摆动角度（度）
    private float maxVelocity;          // 最大速度（限制）
    private float maxAcceleration;      // 最大加速度（限制）

    // 风力影响
    private boolean windAffected;
    private float windInfluence;        // 风力影响系数（0-1）

    private boolean enabled;

    /**
     * 构造函数（使用默认参数）
     */
    public PhysicsComponent() {
        this.velocity = new Vector3f(0, 0, 0);
        this.acceleration = new Vector3f(0, 0, 0);
        this.previousPosition = new Vector3f(0, 0, 0);

        // 默认物理参数（适合头发）
        this.mass = 1.0f;
        this.damping = 0.95f;           // 5% 阻尼
        this.stiffness = 50.0f;
        this.gravityStrength = 9.8f;
        this.gravityDirection = new Vector3f(0, -1, 0);

        // 默认约束
        this.maxSwingAngle = 45.0f;
        this.maxVelocity = 10.0f;
        this.maxAcceleration = 50.0f;

        // 风力
        this.windAffected = false;
        this.windInfluence = 0.5f;

        this.enabled = true;
    }

    /**
     * 重置物理状态
     */
    public void reset() {
        velocity.set(0, 0, 0);
        acceleration.set(0, 0, 0);
    }

    /**
     * 应用力
     *
     * @param force 力向量
     */
    public void applyForce(Vector3f force) {
        // F = ma, a = F/m
        acceleration.addLocal(force.divide(mass));
    }

    /**
     * 应用冲量（瞬间改变速度）
     *
     * @param impulse 冲量向量
     */
    public void applyImpulse(Vector3f impulse) {
        // p = mv, v = p/m
        velocity.addLocal(impulse.divide(mass));
    }

    /**
     * 限制速度
     */
    public void clampVelocity() {
        float speed = velocity.length();
        if (speed > maxVelocity) {
            velocity.normalizeLocal().multLocal(maxVelocity);
        }
    }

    /**
     * 限制加速度
     */
    public void clampAcceleration() {
        float acc = acceleration.length();
        if (acc > maxAcceleration) {
            acceleration.normalizeLocal().multLocal(maxAcceleration);
        }
    }

    // ========== 预设配置 ==========

    /**
     * 配置为头发物理
     */
    public void configureForHair() {
        mass = 0.5f;
        damping = 0.92f;
        stiffness = 40.0f;
        gravityStrength = 5.0f;
        maxSwingAngle = 60.0f;
        windAffected = true;
        windInfluence = 0.7f;
    }

    /**
     * 配置为尾巴物理
     */
    public void configureForTail() {
        mass = 1.5f;
        damping = 0.95f;
        stiffness = 60.0f;
        gravityStrength = 9.8f;
        maxSwingAngle = 45.0f;
        windAffected = false;
    }

    /**
     * 配置为布料物理
     */
    public void configureForCloth() {
        mass = 0.3f;
        damping = 0.90f;
        stiffness = 30.0f;
        gravityStrength = 9.8f;
        maxSwingAngle = 90.0f;
        windAffected = true;
        windInfluence = 0.8f;
    }

    // ========== Getters/Setters ==========

    public Vector3f getVelocity() {
        return velocity;
    }

    public void setVelocity(Vector3f velocity) {
        this.velocity.set(velocity);
    }

    public Vector3f getAcceleration() {
        return acceleration;
    }

    public void setAcceleration(Vector3f acceleration) {
        this.acceleration.set(acceleration);
    }

    public Vector3f getPreviousPosition() {
        return previousPosition;
    }

    public void setPreviousPosition(Vector3f position) {
        this.previousPosition.set(position);
    }

    public float getMass() {
        return mass;
    }

    public void setMass(float mass) {
        this.mass = Math.max(0.01f, mass); // 避免除零
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

    public Vector3f getGravityDirection() {
        return gravityDirection;
    }

    public void setGravityDirection(Vector3f direction) {
        this.gravityDirection.set(direction).normalizeLocal();
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

    public float getMaxAcceleration() {
        return maxAcceleration;
    }

    public void setMaxAcceleration(float maxAcceleration) {
        this.maxAcceleration = Math.max(0.1f, maxAcceleration);
    }

    public boolean isWindAffected() {
        return windAffected;
    }

    public void setWindAffected(boolean windAffected) {
        this.windAffected = windAffected;
    }

    public float getWindInfluence() {
        return windInfluence;
    }

    public void setWindInfluence(float windInfluence) {
        this.windInfluence = Math.max(0f, Math.min(1f, windInfluence));
    }

    // ========== Component Interface ==========

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            reset();
        }
    }

    @Override
    public Component clone() {
        PhysicsComponent cloned = new PhysicsComponent();
        cloned.velocity.set(this.velocity);
        cloned.acceleration.set(this.acceleration);
        cloned.previousPosition.set(this.previousPosition);
        cloned.mass = this.mass;
        cloned.damping = this.damping;
        cloned.stiffness = this.stiffness;
        cloned.gravityStrength = this.gravityStrength;
        cloned.gravityDirection.set(this.gravityDirection);
        cloned.maxSwingAngle = this.maxSwingAngle;
        cloned.maxVelocity = this.maxVelocity;
        cloned.maxAcceleration = this.maxAcceleration;
        cloned.windAffected = this.windAffected;
        cloned.windInfluence = this.windInfluence;
        return cloned;
    }

    @Override
    public String toString() {
        return String.format("PhysicsComponent[mass=%.2f, damping=%.2f, stiffness=%.2f, velocity=(%.2f,%.2f,%.2f)]",
                mass, damping, stiffness, velocity.x, velocity.y, velocity.z);
    }
}
