package com.Hecate.weapon;

import com.jme3.math.Vector3f;

/**
 * 子弹配置 - 弹道和物理参数
 *
 * 定义子弹的飞行方式（直线、抛物线、导弹）
 * 和命中效果（伤害、涂墨、爆炸）
 */
public class ProjectileProfile {

    // 基础信息
    private final String id;                    // 配置ID（唯一标识）
    private final String displayName;           // 显示名称

    // 弹道类型
    private final ArcType arcType;              // 弹道类型：直线/抛物线/追踪

    // 物理参数
    private final float velocity;               // 初速度（米/秒）
    private final float gravity;                // 重力加速度（米/秒²，负数向下）
    private final float drag;                   // 空气阻力系数（0-1）
    private final float maxLifetime;            // 最大存活时间（秒）
    private final float maxRange;               // 最大射程（米）

    // 命中效果
    private final HitEffect hitEffect;          // 命中时效果
    private final ExpireEffect expireEffect;    // 超时效果

    // 视觉效果
    private final VisualConfig visualConfig;    // 渲染配置

    /**
     * 弹道类型
     */
    public enum ArcType {
        LINEAR,         // 直线（忽略重力）
        BALLISTIC,      // 抛物线（受重力影响）
        HOMING          // 追踪导弹（自动瞄准）
    }

    /**
     * 命中效果配置
     */
    public static class HitEffect {
        public final float damage;              // 伤害值
        public final float inkRadius;           // 涂墨半径（米）
        public final int inkTeam;               // 涂墨队伍（0=A, 1=B）
        public final boolean ignite;            // 是否点燃墨水
        public final boolean explode;           // 是否爆炸
        public final float explosionRadius;     // 爆炸半径（米）

        public HitEffect(float damage, float inkRadius, int inkTeam,
                        boolean ignite, boolean explode, float explosionRadius) {
            this.damage = damage;
            this.inkRadius = inkRadius;
            this.inkTeam = inkTeam;
            this.ignite = ignite;
            this.explode = explode;
            this.explosionRadius = explosionRadius;
        }

        // 简化构造（仅伤害和涂墨）
        public static HitEffect simple(float damage, float inkRadius, int inkTeam) {
            return new HitEffect(damage, inkRadius, inkTeam, false, false, 0);
        }
    }

    /**
     * 超时效果配置（子弹消失时）
     */
    public static class ExpireEffect {
        public final boolean dropToGround;      // 是否落地涂墨
        public final boolean explode;           // 是否爆炸

        public ExpireEffect(boolean dropToGround, boolean explode) {
            this.dropToGround = dropToGround;
            this.explode = explode;
        }

        public static ExpireEffect none() {
            return new ExpireEffect(false, false);
        }

        public static ExpireEffect dropInk() {
            return new ExpireEffect(true, false);
        }
    }

    /**
     * 视觉配置（渲染相关）
     */
    public static class VisualConfig {
        public final String particleType;       // 粒子类型：flame, bullet, laser
        public final Vector3f color;            // 颜色（RGB）
        public final float scale;               // 尺寸缩放
        public final boolean trail;             // 是否有拖尾

        public VisualConfig(String particleType, Vector3f color, float scale, boolean trail) {
            this.particleType = particleType;
            this.color = color;
            this.scale = scale;
            this.trail = trail;
        }

        public static VisualConfig flame() {
            return new VisualConfig("flame", new Vector3f(1, 0.5f, 0), 1.0f, true);
        }

        public static VisualConfig bullet() {
            return new VisualConfig("bullet", new Vector3f(1, 1, 0), 0.5f, false);
        }
    }

    /**
     * 构造函数（使用Builder创建）
     */
    private ProjectileProfile(Builder builder) {
        this.id = builder.id;
        this.displayName = builder.displayName;
        this.arcType = builder.arcType;
        this.velocity = builder.velocity;
        this.gravity = builder.gravity;
        this.drag = builder.drag;
        this.maxLifetime = builder.maxLifetime;
        this.maxRange = builder.maxRange;
        this.hitEffect = builder.hitEffect;
        this.expireEffect = builder.expireEffect;
        this.visualConfig = builder.visualConfig;
    }

    // Getters
    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public ArcType getArcType() { return arcType; }
    public float getVelocity() { return velocity; }
    public float getGravity() { return gravity; }
    public float getDrag() { return drag; }
    public float getMaxLifetime() { return maxLifetime; }
    public float getMaxRange() { return maxRange; }
    public HitEffect getHitEffect() { return hitEffect; }
    public ExpireEffect getExpireEffect() { return expireEffect; }
    public VisualConfig getVisualConfig() { return visualConfig; }

    /**
     * Builder模式
     */
    public static class Builder {
        // 必需参数
        private final String id;
        private final String displayName;

        // 可选参数（默认值）
        private ArcType arcType = ArcType.LINEAR;
        private float velocity = 20.0f;
        private float gravity = 0.0f;
        private float drag = 0.98f;
        private float maxLifetime = 5.0f;
        private float maxRange = 100.0f;
        private HitEffect hitEffect = HitEffect.simple(10, 1, 0);
        private ExpireEffect expireEffect = ExpireEffect.none();
        private VisualConfig visualConfig = VisualConfig.bullet();

        public Builder(String id, String displayName) {
            this.id = id;
            this.displayName = displayName;
        }

        public Builder arcType(ArcType val) { arcType = val; return this; }
        public Builder velocity(float val) { velocity = val; return this; }
        public Builder gravity(float val) { gravity = val; return this; }
        public Builder drag(float val) { drag = val; return this; }
        public Builder maxLifetime(float val) { maxLifetime = val; return this; }
        public Builder maxRange(float val) { maxRange = val; return this; }
        public Builder hitEffect(HitEffect val) { hitEffect = val; return this; }
        public Builder expireEffect(ExpireEffect val) { expireEffect = val; return this; }
        public Builder visualConfig(VisualConfig val) { visualConfig = val; return this; }

        public ProjectileProfile build() {
            return new ProjectileProfile(this);
        }
    }

    @Override
    public String toString() {
        return String.format("ProjectileProfile[%s: %s, arc=%s, vel=%.1f, range=%.1f]",
                id, displayName, arcType, velocity, maxRange);
    }
}
