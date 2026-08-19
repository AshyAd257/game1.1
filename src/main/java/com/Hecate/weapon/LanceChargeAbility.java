package com.Hecate.weapon;

/**
 * 骑枪冲锋技能骨架。
 * <p>骑枪的核心机制（"蓄力一段时间后往前冲去，无视路上的减益地块，击中后
 * 对方会被控制"）超出了单个飞行物（{@link Projectile}）能描述的范畴——
 * 冲锋要接管玩家的移动（玩家被"绑定"在冲锋路径上），停下来时冲锋即消失，
 * 这和"发射一个独立飞行的子弹"是两种不同的控制关系。
 * <p>因此骑枪不继承 {@link MeleeWeapon}/{@link Weapon} 的开火管线，而是作为一个
 * 独立的技能对象，由 {@code PlayerController} 持有并在冲锋期间接管移动逻辑
 * （具体接入方式留待后续实现，本类目前只搭生命周期骨架，不做任何实际效果，
 * 也未被任何地方调用）。
 */
public class LanceChargeAbility {

    private boolean active;
    private float chargeTime;
    private float elapsedChargeTime;

    public LanceChargeAbility(float chargeTime) {
        this.chargeTime = chargeTime;
        this.active = false;
        this.elapsedChargeTime = 0f;
    }

    /**
     * 开始蓄力（对应"蓄力一段时间"阶段，尚未真正冲出去）
     */
    public void startCharge() {
        // TODO: 蓄力阶段的具体表现（例如玩家无法移动/武器动作）留待实现
        active = true;
        elapsedChargeTime = 0f;
    }

    /**
     * 每帧更新（蓄力计时/冲锋位移，具体效果待实现）
     * @param tpf 帧时间
     */
    public void update(float tpf) {
        if (!active) {
            return;
        }
        elapsedChargeTime += tpf;
        // TODO: 蓄力满后触发冲锋：接管玩家移动，沿当前朝向匀速位移，
        // 无视路上的减益地块（GridCell的速度倍率），命中敌人后触发控制效果，
        // 玩家停下（或冲锋距离耗尽）时冲锋消失。
    }

    /**
     * 是否正在蓄力或冲锋中
     */
    public boolean isActive() {
        return active;
    }

    /**
     * 取消当前蓄力/冲锋（例如玩家死亡、切换武器时调用）
     */
    public void cancel() {
        active = false;
        elapsedChargeTime = 0f;
    }
}
