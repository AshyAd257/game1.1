package com.Hecate.monster;

/**
 * 怪物变体：命名预设，内部持有一份 {@link MonsterDefinition}。
 * <p>用于波次系统按名字引用怪物类型（避免波次配置里直接散落一堆裸的MonsterDefinition
 * 实例）。新增怪物类型不强制要求进这个枚举——{@link MonsterManager#spawnMonster}
 * 也接受直接传入 {@link MonsterDefinition}，供未来数据/JSON驱动的怪物图鉴使用。
 */
public enum MonsterVariant {

    /** 慢速怪：体型/血量不变，移速明显更慢（用于第1波） */
    SLOW(MonsterDefinition.builder("slow")
            .speedMultiplier(0.6f)
            .build()),

    /** 普通怪：即当前默认数值（用于第2波，以及 /mob1 命令） */
    NORMAL(MonsterDefinition.builder("normal")
            .build()),

    /** 小Boss：体型翻倍、血量翻倍、移速略慢于普通怪（用于第3波） */
    MINI_BOSS(MonsterDefinition.builder("mini_boss")
            .sizeMultiplier(2.0f)
            .healthMultiplier(2.0f)
            .speedMultiplier(0.85f)
            .build()),

    /**
     * 毒怪示例：接触攻击不直接扣血，改为施加中毒DOT（复用EffectRegistry里已注册的
     * "poison"效果：8秒内每秒1点伤害）。演示数据驱动框架下新增"攻击方式"只需换一个
     * attackBehavior，不需要改Monster/MonsterManager源码。
     */
    POISON_SLIME(MonsterDefinition.builder("poison_slime")
            .speedMultiplier(0.8f)
            .attackBehavior(() -> new ContactPoisonAttackBehavior("poison"))
            .build()),

    /**
     * 远程怪示例：不做接触伤害，进入射程后用 {@link com.Hecate.ink.EnemyProjectileWeapon}
     * 开火（该武器类此前已经写好但从未被任何代码实例化，通过RangedAttackBehavior首次接入怪物AI）。
     */
    RANGED_SPITTER(MonsterDefinition.builder("ranged_spitter")
            .speedMultiplier(0.7f)
            .attackBehavior(RangedAttackBehavior.factory(
                    () -> new com.Hecate.ink.EnemyProjectileWeapon(com.Hecate.ink.FactionRegistry.DARK_DEFAULT)))
            .build());

    public final MonsterDefinition definition;

    MonsterVariant(MonsterDefinition definition) {
        this.definition = definition;
    }
}
