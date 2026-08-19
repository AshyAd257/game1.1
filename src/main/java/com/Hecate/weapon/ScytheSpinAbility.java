package com.Hecate.weapon;

/**
 * 镰刀旋转技能骨架。
 * <p>镰刀的核心机制（"蓄力一会后在周身360度持续攻击甩墨，可以弹开附近的伤害，
 * 转完后有一段时间的虚弱期"）是一个持续环绕玩家身周的旋转攻击判定，
 * 不是"发射出去"的飞行物，因此和骑枪/战锤一样单独设计，不继承
 * {@link MeleeWeapon}/{@link Weapon} 的开火管线。
 * <p>本类目前只搭生命周期骨架（蓄力→旋转攻击→虚弱期三段状态），不做任何实际
 * 效果，也未被任何地方调用。
 */
public class ScytheSpinAbility {

    public enum State {
        IDLE,
        CHARGING,
        SPINNING,
        WEAKENED
    }

    private State state = State.IDLE;
    private float chargeTime;
    private float spinDuration;
    private float weaknessDuration;
    private float elapsed;

    public ScytheSpinAbility(float chargeTime, float spinDuration, float weaknessDuration) {
        this.chargeTime = chargeTime;
        this.spinDuration = spinDuration;
        this.weaknessDuration = weaknessDuration;
    }

    /**
     * 开始蓄力
     */
    public void startCharge() {
        state = State.CHARGING;
        elapsed = 0f;
    }

    /**
     * 每帧更新，按当前状态推进到下一阶段（具体判定/甩墨效果待实现）
     * @param tpf 帧时间
     */
    public void update(float tpf) {
        elapsed += tpf;
        switch (state) {
            case CHARGING:
                // TODO: 蓄力满后自动进入旋转攻击阶段
                break;
            case SPINNING:
                // TODO: 环绕玩家身周的旋转攻击判定+甩墨，期间可弹开附近伤害；
                // 也可配合附魔冲进敌人堆里进行大规模控制
                break;
            case WEAKENED:
                // TODO: 虚弱期效果（例如伤害减免降低/移速降低）
                break;
            case IDLE:
            default:
                break;
        }
    }

    public State getState() {
        return state;
    }

    /**
     * 取消当前技能状态（例如玩家死亡、切换武器时调用）
     */
    public void cancel() {
        state = State.IDLE;
        elapsed = 0f;
    }
}
