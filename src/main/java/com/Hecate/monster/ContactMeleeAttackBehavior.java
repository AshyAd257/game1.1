package com.Hecate.monster;

import com.Hecate.ink.SparseGridManager;
import com.Hecate.physics.AABB;
import com.Hecate.player.PlayerController;
import com.Hecate.weapon.MeleeWeapon;
import com.Hecate.weapon.Weapon;

/**
 * 接触近战攻击：玩家进入怪物攻击范围（{@link Monster#isPlayerInAttackRange}，默认是
 * 套在方块上的外接球，无死角）且攻击冷却已结束时造成一次伤害。
 * <p>不要求朝向——原地转圈的怪物也能咬到玩家。这是所有现有怪物（SLOW/NORMAL/MINI_BOSS）
 * 的默认行为。
 */
public class ContactMeleeAttackBehavior implements MonsterAttackBehavior {

    @Override
    public void fixedUpdate(float dt, Monster self, PlayerController player, SparseGridManager gridManager) {
        if (player == null || !self.isAttackReady()) {
            return;
        }

        AABB playerBox = player.getPlayerBox();
        if (playerBox == null) {
            return;
        }

        if (self.isPlayerInAttackRange(playerBox)) {
            float damage = self.getDefinition().attackDamage * (1.0f - getMeleeBlockReduction(player));
            if (damage > 0f) {
                player.getPlayerHealth().takeDamage(damage);
            }
            self.resetAttackCooldown();
        }
    }

    /**
     * 查询玩家当前武器（如果是近战武器）是否处于格挡/弹反窗口，返回对应的伤害减免比例
     * （0=无减免，1=完全格挡）。目前 {@link MeleeWeapon#isBlockingActive()} 始终返回false，
     * 本方法恒返回0，预留给迅捷剑/大剑弹反数值填充时使用。
     */
    private float getMeleeBlockReduction(PlayerController player) {
        Weapon weapon = player.getCurrentWeapon();
        if (weapon instanceof MeleeWeapon) {
            MeleeWeapon meleeWeapon = (MeleeWeapon) weapon;
            if (meleeWeapon.isBlockingActive()) {
                return meleeWeapon.getBlockDamageReduction();
            }
        }
        return 0f;
    }
}
