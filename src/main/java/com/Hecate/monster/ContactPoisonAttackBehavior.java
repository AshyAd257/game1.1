package com.Hecate.monster;

import com.Hecate.ink.SparseGridManager;
import com.Hecate.physics.AABB;
import com.Hecate.player.PlayerController;
import com.Hecate.player.effect.PoisonEffect;

/**
 * 接触攻击 + 中毒：命中判定与 {@link ContactMeleeAttackBehavior} 完全相同，
 * 但命中时额外对玩家施加一层 {@link PoisonEffect}（DOT），而不是直接扣血。
 *
 * <p>用于验证"怪物攻击 → 施加DOT → 固定逻辑刻驱动扣血"这条链路——本类命中时不再
 * 直接调用 {@code takeDamage}，改为把伤害转换成持续时间内的中毒效果。
 * 作为怪物数据框架接入DOT系统的示范实现，未来的毒怪/流血怪可参照此类编写。
 */
public class ContactPoisonAttackBehavior implements MonsterAttackBehavior {

    private final String poisonEffectId;

    public ContactPoisonAttackBehavior(String poisonEffectId) {
        this.poisonEffectId = poisonEffectId;
    }

    @Override
    public void fixedUpdate(float dt, Monster self, PlayerController player, SparseGridManager gridManager) {
        if (player == null || !self.isAttackReady()) {
            return;
        }

        AABB playerBox = player.getPlayerBox();
        if (playerBox == null) {
            return;
        }

        if (!self.getBoundingBox().intersects(playerBox)) {
            return;
        }

        if (player.getPlayerStateManager() != null) {
            PoisonEffect poison = PoisonEffect.create(poisonEffectId, player.getPlayerHealth());
            player.getPlayerStateManager().getEffectManager().applyEffect(poison);
        }

        self.resetAttackCooldown();
    }
}
