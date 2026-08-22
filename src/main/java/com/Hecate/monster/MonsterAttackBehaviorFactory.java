package com.Hecate.monster;

/**
 * {@link MonsterAttackBehavior} 的工厂：每只 {@link Monster} 实例持有自己的行为对象
 * （而不是共享单例），因为部分行为实现（如 {@link RangedAttackBehavior}）需要持有
 * 每只怪物独立的武器冷却状态。
 */
@FunctionalInterface
public interface MonsterAttackBehaviorFactory {
    MonsterAttackBehavior create();
}
