package com.Hecate.weapon;

/**
 * 近战武器基类。
 * <p>剑类（迅捷剑/大剑）复用 {@link Weapon#tryFire}/{@code fire()} 的开火管线——
 * 挥砍在数值上被抽象成"发射一个短程慢速的 {@link Projectile}"
 * （{@link ProjectileProfile.ArcType#MELEE_SWING}），弹药消耗可以配置为0来表示
 * 近战不消耗弹药。骑枪（冲锋接管移动）和战锤（地面环形冲击波）行为超出单个
 * 飞行物能描述的范畴，不继承本类，见 {@link LanceChargeAbility}/{@link WarhammerSlamAbility}。
 * <p>本类只负责搭好"弹反/格挡查询"的钩子——具体的挥砍判定、弹反时机、伤害减免
 * 数值都留空，待后续填数值时实现。
 */
public abstract class MeleeWeapon extends Weapon {

    public MeleeWeapon(WeaponStats stats, WeaponKind kind) {
        super(stats, kind);
    }

    /**
     * 当前是否处于"格挡/弹反生效"窗口。
     * <p>预留给伤害链路的查询钩子：{@code MonsterManager.checkContactDamage()}
     * 未来可以在调用 {@code playerHealth.takeDamage()} 前先查询这个方法，
     * 决定是否要减免/完全弹反这次伤害。目前默认返回false，不改变现有伤害逻辑
     * （怪物接触伤害依然直接结算，行为与改动前完全一致）。
     * <p>迅捷剑/大剑："挥舞过程如果恰到好处"触发的格挡窗口；大剑弹反成功后
     * 窗口会持续一段时间。具体的"恰到好处"判定（例如挥砍动作的某个时间区间）
     * 留给子类实现。
     */
    public boolean isBlockingActive() {
        return false;
    }

    /**
     * 格挡/弹反生效时的伤害减免比例（0=无减免，1=完全格挡）。
     * <p>仅在 {@link #isBlockingActive()} 为true时才有意义。默认1.0（占位为完全格挡），
     * 具体数值（例如"迅捷剑弹反是完全格挡还是部分格挡"）由子类覆写决定。
     */
    public float getBlockDamageReduction() {
        return 1.0f;
    }
}
