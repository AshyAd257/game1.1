package com.Hecate.flame;

import com.jme3.math.Vector3f;

/**
 * 地面火焰点
 * 在地面上持续燃烧的火焰，会持续发射粒子
 */
public class GroundFire {

    // 位置
    private Vector3f position;

    // 生命周期
    private float lifetime;
    private float maxLifetime;

    // 是否存活
    private boolean alive;

    // 发射参数
    private float emissionTimer = 0f;
    private float emissionInterval = 0.15f; // 每0.15秒发射一次（降低频率，减少闪烁）
    private int particlesPerEmission = 3; // 每次发射3个基础粒子（每个会在Y轴上创建3层，共9个粒子）

    // 火焰强度（随时间衰减）
    private float intensity = 1.0f;

    /**
     * 构造函数
     */
    public GroundFire(Vector3f position, float lifetime) {
        this.position = position.clone();
        this.lifetime = lifetime;
        this.maxLifetime = lifetime;
        this.alive = true;
    }

    /**
     * 更新地面火焰
     */
    public void update(float tpf) {
        if (!alive) return;

        // 更新生命周期
        lifetime -= tpf;
        if (lifetime <= 0) {
            alive = false;
            return;
        }

        // 更新强度（随生命周期衰减）
        intensity = lifetime / maxLifetime;

        // 更新发射计时器
        emissionTimer += tpf;
    }

    /**
     * 检查是否应该发射粒子
     */
    public boolean shouldEmit() {
        if (!alive) return false;

        if (emissionTimer >= emissionInterval) {
            emissionTimer -= emissionInterval;
            return true;
        }
        return false;
    }

    // Getters
    public Vector3f getPosition() { return position; }
    public float getLifetime() { return lifetime; }
    public float getLifeRatio() { return lifetime / maxLifetime; }
    public boolean isAlive() { return alive; }
    public int getParticlesPerEmission() { return particlesPerEmission; }
    public float getIntensity() { return intensity; }

    public void kill() { this.alive = false; }
}
