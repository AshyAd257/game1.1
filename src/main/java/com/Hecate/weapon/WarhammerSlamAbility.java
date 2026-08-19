package com.Hecate.weapon;

/**
 * 战锤砸地技能骨架。
 * <p>战锤的核心机制（"蓄力后砸向地面，施加范围攻击，配合附魔可以范围控制，
 * 砸中敌人伤害很高；不蓄力砸中伤害-50%"）本质上是"从地面向外扩散的环形冲击波"，
 * 超出了单个飞行物的直线/抛物线飞行模型能描述的范畴——冲击波的传播方式是
 * 以玩家脚下为圆心向外扩张的圆环，不是从玩家身上"发射"出去的。
 * <p>因此战锤不继承 {@link MeleeWeapon}/{@link Weapon} 的开火管线，而是作为一个
 * 独立的技能对象，由 {@code PlayerController} 持有。本类目前只搭生命周期骨架，
 * 不做任何实际效果，也未被任何地方调用。
 */
public class WarhammerSlamAbility {

    private boolean charging;
    private boolean slamming;
    private float chargeTime;
    private float elapsedChargeTime;

    public WarhammerSlamAbility(float chargeTime) {
        this.chargeTime = chargeTime;
        this.charging = false;
        this.slamming = false;
        this.elapsedChargeTime = 0f;
    }

    /**
     * 开始蓄力
     */
    public void startCharge() {
        charging = true;
        elapsedChargeTime = 0f;
    }

    /**
     * 释放（松手触发砸地）。
     * @param wasFullyCharged 是否蓄满——影响伤害倍率（不蓄力命中伤害-50%，具体数值待实现）
     */
    public void release(boolean wasFullyCharged) {
        charging = false;
        slamming = true;
        // TODO: 触发以玩家脚下为圆心向外扩散的环形冲击波，扩散过程中检测命中，
        // 命中敌人造成范围伤害（wasFullyCharged影响伤害倍率），配合附魔可实现范围控制。
    }

    /**
     * 每帧更新（蓄力计时/冲击波扩散，具体效果待实现）
     * @param tpf 帧时间
     */
    public void update(float tpf) {
        if (charging) {
            elapsedChargeTime += tpf;
        }
        // TODO: slamming状态下的冲击波扩散半径更新与命中检测
    }

    public boolean isCharging() {
        return charging;
    }

    public boolean isSlamming() {
        return slamming;
    }

    /**
     * 取消当前蓄力（例如玩家死亡、切换武器时调用）
     */
    public void cancel() {
        charging = false;
        slamming = false;
        elapsedChargeTime = 0f;
    }
}
