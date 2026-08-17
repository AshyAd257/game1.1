package com.Hecate.flame;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * 地面火焰管理器
 * 管理所有地面火焰点的生成、更新和销毁
 */
public class GroundFireManager {

    // 地面火焰列表
    private List<GroundFire> groundFires;

    // 粒子系统引用(用于发射粒子)
    private FlameParticleSystem particleSystem;

    // 地面火焰参数
    private static final float DEFAULT_GROUND_FIRE_LIFETIME = 8.0f; // 地面火焰持续8秒
    private static final float MIN_DISTANCE_BETWEEN_FIRES = 0.3f; // 地面火焰之间的最小距离

    public GroundFireManager(FlameParticleSystem particleSystem) {
        this.groundFires = new ArrayList<>();
        this.particleSystem = particleSystem;
    }

    /**
     * 在指定位置创建地面火焰
     */
    public void createGroundFire(Vector3f position) {
        // 检查是否已经有火焰在附近
        for (GroundFire fire : groundFires) {
            if (fire.isAlive() && fire.getPosition().distance(position) < MIN_DISTANCE_BETWEEN_FIRES) {
                // 附近已有火焰,不创建新的
                return;
            }
        }

        // 创建新的地面火焰
        GroundFire groundFire = new GroundFire(position, DEFAULT_GROUND_FIRE_LIFETIME);
        groundFires.add(groundFire);

    }

    /**
     * 更新所有地面火焰
     */
    public void update(float tpf) {
        // 更新所有地面火焰
        for (int i = groundFires.size() - 1; i >= 0; i--) {
            GroundFire fire = groundFires.get(i);
            fire.update(tpf);

            // 移除死亡的火焰
            if (!fire.isAlive()) {
                groundFires.remove(i);
                continue;
            }

            // 检查是否应该发射粒子
            if (fire.shouldEmit()) {
                emitGroundFireParticles(fire);
            }
        }
    }

    /**
     * 从地面火焰发射粒子(改进版:垂直拉长,横向衰减)
     */
    private void emitGroundFireParticles(GroundFire fire) {
        Vector3f firePos = fire.getPosition();
        int baseParticleCount = fire.getParticlesPerEmission(); // 基础粒子数量(横向)
        float intensity = fire.getIntensity();

        int verticalLayers = 3; // Y轴上的层数(垂直拉长)
        int totalParticles = baseParticleCount * verticalLayers;

        // 为每个基础位置创建垂直的火焰柱
        for (int i = 0; i < baseParticleCount; i++) {
            // 随机横向偏移(X和Z轴)
            float offsetX = (FastMath.nextRandomFloat() - 0.5f) * 0.15f;
            float offsetZ = (FastMath.nextRandomFloat() - 0.5f) * 0.15f;

            // 计算横向距离(用于寿命衰减)
            float horizontalDist = FastMath.sqrt(offsetX * offsetX + offsetZ * offsetZ);

            // 在Y轴上创建多层粒子(垂直拉长)
            for (int layer = 0; layer < verticalLayers; layer++) {
                // 每层高度间隔
                float layerHeight = layer * 0.12f; // 每层间隔0.12单位

                Vector3f pos = new Vector3f(
                    firePos.x + offsetX,
                    firePos.y + 0.05f + layerHeight, // 从地面向上堆叠
                    firePos.z + offsetZ
                );

                // 向上速度随层数减小(底部快,顶部慢)- 降低速度让火焰更稳定
                float baseUpwardSpeed = 0.5f - layer * 0.12f; // 0.5 → 0.38 → 0.26(降低速度)
                float upwardSpeed = baseUpwardSpeed + FastMath.nextRandomFloat() * 0.15f;
                float horizontalSpeed = (FastMath.nextRandomFloat() - 0.5f) * 0.05f; // 减少横向扰动
                Vector3f vel = new Vector3f(horizontalSpeed, upwardSpeed, horizontalSpeed);

                // 粒子大小随高度略微缩小
                float layerSizeMultiplier = 1.0f - layer * 0.1f; // 1.0 → 0.9 → 0.8
                float radius = (0.09f + FastMath.nextRandomFloat() * 0.02f) * layerSizeMultiplier;

                // 强度随高度衰减(顶部更透明)
                float layerIntensity = intensity * 0.9f * (1.0f - layer * 0.2f); // 90% → 72% → 54%

                // 寿命受横向距离影响(横向衰减效果)
                // 距离中心越远,寿命越短,火焰边缘更快消失
                float baseLifetime = 1.8f - horizontalDist * 2.0f; // 横向衰减系数(增加基础寿命,降低衰减速度)
                baseLifetime = FastMath.clamp(baseLifetime, 0.8f, 2.2f); // 限制范围(增加最小和最大寿命)

                // 高层粒子寿命也略短
                float layerLifetimeMultiplier = 1.0f - layer * 0.08f; // 减小垂直衰减(0.92 → 0.84)
                float lifetime = (baseLifetime * layerLifetimeMultiplier) + FastMath.nextRandomFloat() * 0.3f;

                // 创建粒子（使用粒子系统的阵营ID）
                FlameParticle particle = particleSystem.obtainParticle();
                particle.reset(pos, vel, radius, layerIntensity, 2.5f, lifetime, particleSystem.getFactionId());
                particleSystem.getParticles().add(particle);
            }
        }
    }

    /**
     * 清空所有地面火焰
     */
    public void clear() {
        groundFires.clear();
    }

    /**
     * 获取地面火焰数量
     */
    public int getGroundFireCount() {
        return groundFires.size();
    }

    /**
     * 获取所有地面火焰
     */
    public List<GroundFire> getGroundFires() {
        return groundFires;
    }
}
