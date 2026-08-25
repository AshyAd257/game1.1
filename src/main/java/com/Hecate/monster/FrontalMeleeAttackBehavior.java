package com.Hecate.monster;

import com.Hecate.ink.SparseGridManager;
import com.Hecate.physics.AABB;
import com.Hecate.player.PlayerController;

/**
 * 正面近战攻击：与 {@link ContactMeleeAttackBehavior} 一样要求玩家进入攻击范围+冷却结束，
 * 但额外要求怪物正朝向玩家（{@link Monster#isFacingTarget}）才会造成伤害——
 * 用于"背刺无伤、需要正面拍才受伤"的重装/精英怪。
 */
public class FrontalMeleeAttackBehavior implements MonsterAttackBehavior {

    @Override
    public void fixedUpdate(float dt, Monster self, PlayerController player, SparseGridManager gridManager) {
        if (player == null || !self.isAttackReady()) {
            return;
        }

        AABB playerBox = player.getPlayerBox();
        if (playerBox == null) {
            return;
        }

        if (!self.isPlayerInAttackRange(playerBox)) {
            return;
        }

        if (!self.isFacingTarget(player.getPlayerPosition())) {
            return;
        }

        float damage = self.getDefinition().attackDamage;
        if (damage > 0f) {
            player.getPlayerHealth().takeDamage(damage);
        }
        self.resetAttackCooldown();
    }
}
