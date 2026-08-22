package com.Hecate.monster;

import com.Hecate.ink.SparseGridManager;
import com.Hecate.player.PlayerController;

/**
 * 怪物攻击行为：每种怪物类型通过 {@link MonsterDefinition#attackBehaviorFactory} 挂载
 * 一个具体实现，决定"这只怪具体怎么打人"。{@link MonsterManager} 不再关心具体打法，
 * 只在固定逻辑刻里对每只存活怪物调用 {@link #fixedUpdate}。
 */
public interface MonsterAttackBehavior {

    /**
     * 固定步长调用（20Hz），判定本tick是否发起攻击并结算。
     * @param dt 固定步长时间（秒）
     * @param self 怪物自身
     * @param player 玩家控制器（索敌目标，可能为null）
     * @param gridManager 当前活动世界的墨水网格（远程/AOE武器涂墨用，可能为null）
     */
    void fixedUpdate(float dt, Monster self, PlayerController player, SparseGridManager gridManager);
}
