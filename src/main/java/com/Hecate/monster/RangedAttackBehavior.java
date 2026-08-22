package com.Hecate.monster;

import com.jme3.math.Vector3f;
import com.Hecate.ink.BaseEnemyInkWeapon;
import com.Hecate.ink.SparseGridManager;
import com.Hecate.player.PlayerController;

import java.util.function.Supplier;

/**
 * 远程/AOE攻击：持有一个 {@link BaseEnemyInkWeapon}（如 EnemyFlameWeapon/
 * EnemyProjectileWeapon/EnemyAreaWeapon），玩家进入武器射程且冷却结束时开火。
 * <p>这是敌人涂墨武器框架（{@code com.Hecate.ink.Enemy*Weapon}）第一次被怪物AI实际
 * 使用——此前这些类已经写好但没有任何代码实例化它们。
 * <p>武器实例通过 {@code Supplier} 而不是直接传对象创建，因为每只怪物需要自己独立的
 * 武器冷却状态，不能多只怪物共享同一个weapon实例。
 */
public class RangedAttackBehavior implements MonsterAttackBehavior {

    private final BaseEnemyInkWeapon weapon;
    private float cooldownRemaining = 0f;

    public RangedAttackBehavior(Supplier<BaseEnemyInkWeapon> weaponFactory) {
        this.weapon = weaponFactory.get();
    }

    public static MonsterAttackBehaviorFactory factory(Supplier<BaseEnemyInkWeapon> weaponFactory) {
        return () -> new RangedAttackBehavior(weaponFactory);
    }

    @Override
    public void fixedUpdate(float dt, Monster self, PlayerController player, SparseGridManager gridManager) {
        if (cooldownRemaining > 0f) {
            cooldownRemaining -= dt;
        }

        if (player == null || gridManager == null || cooldownRemaining > 0f) {
            return;
        }

        Vector3f selfPos = self.getPosition();
        Vector3f playerPos = player.getPlayerPosition();
        Vector3f toPlayer = playerPos.subtract(selfPos);

        float distance = toPlayer.length();
        if (distance < 0.0001f || distance > weapon.getRange()) {
            return; // 太近（方向未定义）或超出射程
        }

        weapon.fire(selfPos, toPlayer.normalizeLocal(), gridManager);
        cooldownRemaining = weapon.getCooldown();
    }

    public BaseEnemyInkWeapon getWeapon() {
        return weapon;
    }
}
