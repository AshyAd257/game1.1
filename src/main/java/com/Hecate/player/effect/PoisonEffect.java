package com.Hecate.player.effect;

import com.Hecate.player.PlayerHealth;

/**
 * 中毒/流血类持续伤害效果（DOT）：每帧按 magnitude（每秒伤害量）× 堆叠层数 × deltaTime 扣血。
 *
 * <p>用于验证"怪物攻击 → 施加DOT → 固定逻辑刻驱动扣血"这条链路——{@link PlayerEffectManager}
 * 之前已经写好但从未被接入主循环，现在通过 {@link com.Hecate.core.FixedTickScheduler}
 * 每20Hz调用一次 {@code update}，本类的 {@link #onTick} 就是这条链路的最终落地点。
 *
 * <p>用法：{@code playerController.getPlayerStateManager().getEffectManager()
 * .applyEffect(PoisonEffect.create("poison", playerController.getPlayerHealth()))}
 */
public class PoisonEffect extends ActiveEffect {

    private final PlayerHealth playerHealth;

    public PoisonEffect(EffectDefinition definition, PlayerHealth playerHealth) {
        super(definition);
        this.playerHealth = playerHealth;
    }

    public PoisonEffect(EffectDefinition definition, float duration, int stacks, Object source, PlayerHealth playerHealth) {
        super(definition, duration, stacks, source);
        this.playerHealth = playerHealth;
    }

    /**
     * 便捷工厂：按 {@link EffectRegistry} 里已注册的效果ID（如 "poison"）创建实例。
     */
    public static PoisonEffect create(String effectId, PlayerHealth playerHealth) {
        EffectDefinition def = EffectRegistry.getInstance().getDefinition(effectId);
        if (def == null) {
            throw new IllegalArgumentException("Unknown effect ID: " + effectId);
        }
        return new PoisonEffect(def, playerHealth);
    }

    @Override
    public void onTick(float deltaTime) {
        if (playerHealth == null || playerHealth.isDead()) {
            return;
        }
        float damage = getEffectiveMagnitude() * deltaTime;
        if (damage > 0f) {
            playerHealth.takeDamage(damage);
        }
    }
}
