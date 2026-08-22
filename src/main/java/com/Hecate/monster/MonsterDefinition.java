package com.Hecate.monster;

/**
 * 怪物数据定义：描述一种怪物类型的全部数值和行为配置。
 *
 * <p>取代过去"只有三个倍率"的 {@link MonsterVariant}——新增怪物类型只需要构造一份
 * {@code MonsterDefinition}（通常用 {@link Builder}），不需要改 {@code Monster}/
 * {@code MonsterManager} 的源码加分支。{@code MonsterVariant} 保留作为"预设名字"，
 * 内部持有一份 {@code MonsterDefinition}，波次系统等现有代码不受影响。
 */
public final class MonsterDefinition {

    public final String id;

    // 体型/血量/移速倍率（相对 Monster.BASE_* 常量）
    public final float sizeMultiplier;
    public final float healthMultiplier;
    public final float speedMultiplier;

    // 攻击数值
    public final float attackDamage;
    public final float attackCooldown;
    public final float attackRange;

    // 朝向相关
    public final boolean requiresFacing;     // 是否需要面朝目标才能发起攻击
    public final float facingToleranceDeg;   // 正面判定角度容差
    public final float turnSpeedDegPerSec;   // 转向速度；<=0 表示瞬间转向（现有怪物的默认行为）

    // 行为/武器
    public final MonsterAttackBehaviorFactory attackBehaviorFactory;

    private MonsterDefinition(Builder b) {
        this.id = b.id;
        this.sizeMultiplier = b.sizeMultiplier;
        this.healthMultiplier = b.healthMultiplier;
        this.speedMultiplier = b.speedMultiplier;
        this.attackDamage = b.attackDamage;
        this.attackCooldown = b.attackCooldown;
        this.attackRange = b.attackRange;
        this.requiresFacing = b.requiresFacing;
        this.facingToleranceDeg = b.facingToleranceDeg;
        this.turnSpeedDegPerSec = b.turnSpeedDegPerSec;
        this.attackBehaviorFactory = b.attackBehaviorFactory;
    }

    public static Builder builder(String id) {
        return new Builder(id);
    }

    public static final class Builder {
        private final String id;
        private float sizeMultiplier = 1.0f;
        private float healthMultiplier = 1.0f;
        private float speedMultiplier = 1.0f;

        private float attackDamage = Monster.ATTACK_DAMAGE;
        private float attackCooldown = Monster.ATTACK_COOLDOWN;
        private float attackRange = 0f; // 0表示纯接触判定（AABB相交），不做额外距离检测

        private boolean requiresFacing = false;
        private float facingToleranceDeg = 60f;
        private float turnSpeedDegPerSec = 0f; // 默认瞬间转向，保持现有怪物手感不变

        private MonsterAttackBehaviorFactory attackBehaviorFactory = ContactMeleeAttackBehavior::new;

        private Builder(String id) {
            this.id = id;
        }

        public Builder sizeMultiplier(float v) { this.sizeMultiplier = v; return this; }
        public Builder healthMultiplier(float v) { this.healthMultiplier = v; return this; }
        public Builder speedMultiplier(float v) { this.speedMultiplier = v; return this; }
        public Builder attackDamage(float v) { this.attackDamage = v; return this; }
        public Builder attackCooldown(float v) { this.attackCooldown = v; return this; }
        public Builder attackRange(float v) { this.attackRange = v; return this; }
        public Builder requiresFacing(boolean v) { this.requiresFacing = v; return this; }
        public Builder facingToleranceDeg(float v) { this.facingToleranceDeg = v; return this; }
        public Builder turnSpeedDegPerSec(float v) { this.turnSpeedDegPerSec = v; return this; }
        public Builder attackBehavior(MonsterAttackBehaviorFactory factory) { this.attackBehaviorFactory = factory; return this; }

        public MonsterDefinition build() {
            return new MonsterDefinition(this);
        }
    }
}
