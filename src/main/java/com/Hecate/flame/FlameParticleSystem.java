package com.Hecate.flame;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import java.util.ArrayList;
import java.util.List;

/**
 * 火焰粒子系统
 * 管理火焰粒子的生成、更新和回收
 */
public class FlameParticleSystem {

    // 粒子列表
    private List<FlameParticle> particles;

    // 粒子池（对象复用）
    private List<FlameParticle> particlePool;

    // 粒子上限
    private int maxParticles = 5000;

    // 地面火焰管理器（用于触地通知）
    private GroundFireManager groundFireManager;

    // 发射者阵营ID（每个粒子系统绑定到一个发射者）
    private int factionId;

    // 发射器参数
    private Vector3f emitterPosition;
    private float emitterWidth = 0.5f;  // 发射带宽度（3D单位）
    private float emissionRate = 0f;   // 每秒发射粒子数（默认关闭）
    private float emissionAccumulator = 0f;

    // 粒子默认参数
    private float defaultRadius = 0.08f;  // 粒子半径（3D单位）
    private float defaultIntensity = 0.4f;
    private float defaultSoftness = 2.5f;
    private float defaultLifetime = 1.5f;  // 生命周期（秒）
    private Vector3f defaultVelocity = new Vector3f(0, 2f, 0); // 向上漂浮（火焰效果）

    // 随机变化范围
    private float radiusVariation = 0.03f;  // 3D单位
    private float intensityVariation = 0.2f;
    private float velocityVariation = 0.5f;  // 3D单位

    public FlameParticleSystem() {
        particles = new ArrayList<>();
        particlePool = new ArrayList<>();
        emitterPosition = new Vector3f(0, 1, 0); // 默认发射位置（3D）
        this.factionId = com.Hecate.ink.FactionRegistry.DARK_DEFAULT; // 默认暗属性
    }

    /**
     * 设置发射者阵营ID
     */
    public void setFactionId(int factionId) {
        this.factionId = factionId;
    }

    /**
     * 获取发射者阵营ID
     */
    public int getFactionId() {
        return factionId;
    }

    /**
     * 更新粒子系统
     */
    public void update(float tpf) {
        // 更新所有粒子
        for (int i = particles.size() - 1; i >= 0; i--) {
            FlameParticle p = particles.get(i);
            p.update(tpf);

            // 检测触地粒子，通知地面火焰管理器
            if (p.justHitGround() && groundFireManager != null) {
                groundFireManager.createGroundFire(p.getPosition());
            }

            // 回收死亡粒子
            if (!p.isAlive()) {
                particles.remove(i);
                recycleParticle(p);
            }
        }

        // 自动发射（如果启用）
        if (emissionRate > 0) {
            emissionAccumulator += tpf * emissionRate;
            while (emissionAccumulator >= 1.0f && particles.size() < maxParticles) {
                emitParticle();
                emissionAccumulator -= 1.0f;
            }
        }
    }

    /**
     * 发射一个粒子（3D空间）
     */
    public void emitParticle() {
        // 在发射带内随机位置（高斯分布，中心密集）
        float offsetX = gaussianRandom() * emitterWidth * 0.5f;
        float offsetZ = gaussianRandom() * emitterWidth * 0.5f;
        Vector3f pos = new Vector3f(
            emitterPosition.x + offsetX,
            emitterPosition.y,
            emitterPosition.z + offsetZ
        );

        // 随机速度（主要向上，少量横向）
        Vector3f vel = new Vector3f(
            defaultVelocity.x + (FastMath.nextRandomFloat() - 0.5f) * velocityVariation,
            defaultVelocity.y + (FastMath.nextRandomFloat() - 0.5f) * velocityVariation * 0.3f,
            defaultVelocity.z + (FastMath.nextRandomFloat() - 0.5f) * velocityVariation
        );

        // 随机半径（中心大，两侧小）
        float centerDist = FastMath.sqrt(offsetX * offsetX + offsetZ * offsetZ);
        float centerFactor = 1.0f - FastMath.clamp(centerDist / (emitterWidth * 0.5f), 0f, 1f);
        float radius = defaultRadius + (FastMath.nextRandomFloat() - 0.5f) * radiusVariation;
        radius *= (0.7f + 0.3f * centerFactor); // 中心粒子更大

        // 随机强度
        float intensity = defaultIntensity + (FastMath.nextRandomFloat() - 0.5f) * intensityVariation;
        intensity *= (0.8f + 0.2f * centerFactor); // 中心粒子更亮

        // 创建或复用粒子
        FlameParticle particle = obtainParticle();
        particle.reset(pos, vel, radius, intensity, defaultSoftness, defaultLifetime, this.factionId);
        particles.add(particle);
    }

    // 每次调用burst()生成的一组粒子共享的shotId计数器（用于武器命中伤害去重）
    private static long nextShotId = 0L;

    /**
     * 在指定位置爆发式发射粒子（改进版，3D空间）——无伤害版本，用于环境视觉效果
     */
    public void burst(Vector3f position, int count, float spreadRadius, float initialIntensity) {
        burst(position, count, spreadRadius, initialIntensity, null);
    }

    /**
     * 在指定位置爆发式发射粒子，朝向目标3D位置（用于瞄准射击）——无伤害版本
     * @param position 发射位置（3D世界坐标）
     * @param count 粒子数量
     * @param spreadRadius 扩散半径（3D单位）
     * @param initialIntensity 初始强度
     * @param targetVelocity 目标速度方向（3D），如果为null则使用默认向上速度
     */
    public void burst(Vector3f position, int count, float spreadRadius, float initialIntensity, Vector3f targetVelocity) {
        burst(position, count, spreadRadius, initialIntensity, targetVelocity, 0f);
    }

    /**
     * 在指定位置爆发式发射粒子，朝向目标3D位置，带伤害（用于武器开火）
     * <p>本次调用发射的所有粒子共享同一个新生成的shotId：它们在概念上是"同一发子弹"，
     * 命中怪物时只会计算一次伤害（见 {@link FlameParticle} 类注释）。
     *
     * @param damage 命中怪物时造成的伤害（<=0表示不参与命中判定，视为环境粒子）
     */
    public void burst(Vector3f position, int count, float spreadRadius, float initialIntensity,
                       Vector3f targetVelocity, float damage) {
        long shotId = damage > 0f ? nextShotId++ : -1L;
        int beforeCount = particles.size();
        for (int i = 0; i < count && particles.size() < maxParticles; i++) {
            // 随机方向（球形扩散）
            float theta = FastMath.nextRandomFloat() * FastMath.TWO_PI; // 水平角度
            float phi = (FastMath.nextRandomFloat() - 0.5f) * FastMath.PI * 0.5f; // 垂直角度
            float distance = FastMath.nextRandomFloat() * spreadRadius * 0.3f;

            Vector3f pos = new Vector3f(
                position.x + FastMath.cos(theta) * FastMath.cos(phi) * distance,
                position.y + FastMath.sin(phi) * distance,
                position.z + FastMath.sin(theta) * FastMath.cos(phi) * distance
            );

            Vector3f vel;
            if (targetVelocity != null) {
                // 使用目标速度，加上随机扩散
                float spreadX = (FastMath.nextRandomFloat() - 0.5f) * 0.5f;
                float spreadY = (FastMath.nextRandomFloat() - 0.5f) * 0.3f;
                float spreadZ = (FastMath.nextRandomFloat() - 0.5f) * 0.5f;
                vel = new Vector3f(
                    targetVelocity.x + spreadX,
                    targetVelocity.y + spreadY,
                    targetVelocity.z + spreadZ
                );
            } else {
                // 默认向上速度
                float upwardSpeed = 2f + FastMath.nextRandomFloat() * 1f; // 2-3 向上
                float horizontalSpeed = (FastMath.nextRandomFloat() - 0.5f) * 0.5f; // -0.25到0.25 横向
                vel = new Vector3f(horizontalSpeed, upwardSpeed, horizontalSpeed);
            }

            // 随机大小变化
            float radius = defaultRadius + (FastMath.nextRandomFloat() - 0.5f) * radiusVariation * 1.5f;
            float intensity = initialIntensity + (FastMath.nextRandomFloat() - 0.5f) * intensityVariation;

            // 随机生命周期
            float lifetime = defaultLifetime * (0.7f + FastMath.nextRandomFloat() * 0.6f); // 70%-130%

            FlameParticle particle = obtainParticle();
            particle.reset(pos, vel, radius, intensity, defaultSoftness, lifetime, this.factionId, damage, shotId);
            particles.add(particle);
        }
    }

    /**
     * 高斯随机数（均值0，标准差1）
     */
    private float gaussianRandom() {
        // Box-Muller 变换
        float u1 = FastMath.nextRandomFloat();
        float u2 = FastMath.nextRandomFloat();
        return FastMath.sqrt(-2f * FastMath.log(u1)) * FastMath.cos(FastMath.TWO_PI * u2);
    }

    /**
     * 从对象池获取粒子（公开给地面火焰管理器使用）
     */
    public FlameParticle obtainParticle() {
        if (particlePool.isEmpty()) {
            return new FlameParticle(
                new Vector3f(), new Vector3f(),
                defaultRadius, defaultIntensity, defaultSoftness, defaultLifetime,
                this.factionId  // 使用粒子系统的阵营ID
            );
        }
        return particlePool.remove(particlePool.size() - 1);
    }

    /**
     * 回收粒子到对象池
     */
    private void recycleParticle(FlameParticle particle) {
        if (particlePool.size() < 1000) { // 限制池大小
            particlePool.add(particle);
        }
    }

    /**
     * 清空所有粒子
     */
    public void clear() {
        for (FlameParticle p : particles) {
            recycleParticle(p);
        }
        particles.clear();
    }

    // ========== Getters & Setters ==========

    public List<FlameParticle> getParticles() { return particles; }
    public int getParticleCount() { return particles.size(); }

    public void setEmitterPosition(Vector3f pos) { this.emitterPosition.set(pos); }
    public Vector3f getEmitterPosition() { return emitterPosition; }

    public void setEmitterWidth(float width) { this.emitterWidth = width; }
    public float getEmitterWidth() { return emitterWidth; }

    public void setEmissionRate(float rate) { this.emissionRate = rate; }
    public float getEmissionRate() { return emissionRate; }

    public void setDefaultRadius(float radius) { this.defaultRadius = radius; }
    public void setDefaultIntensity(float intensity) { this.defaultIntensity = intensity; }
    public void setDefaultSoftness(float softness) { this.defaultSoftness = softness; }
    public void setDefaultLifetime(float lifetime) { this.defaultLifetime = lifetime; }
    public void setDefaultVelocity(Vector3f velocity) { this.defaultVelocity.set(velocity); }

    public void setRadiusVariation(float variation) { this.radiusVariation = variation; }
    public void setIntensityVariation(float variation) { this.intensityVariation = variation; }
    public void setVelocityVariation(float variation) { this.velocityVariation = variation; }

    public void setMaxParticles(int max) { this.maxParticles = max; }
    public int getMaxParticles() { return maxParticles; }

    public void setGroundFireManager(GroundFireManager groundFireManager) {
        this.groundFireManager = groundFireManager;
    }
}
