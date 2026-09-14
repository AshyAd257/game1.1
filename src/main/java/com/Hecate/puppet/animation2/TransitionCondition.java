package com.Hecate.puppet.animation2;

/**
 * 转换条件接口
 * 定义状态转换的触发条件
 *
 * 使用函数式接口，可以用Lambda表达式简化代码
 *
 * 示例：
 * <pre>
 * // 简单条件
 * TransitionCondition condition = () -> player.isGrounded();
 *
 * // 复杂条件
 * TransitionCondition condition = () -> {
 *     return player.isGrounded() && player.getSpeed() > 5.0f;
 * };
 * </pre>
 */
@FunctionalInterface
public interface TransitionCondition {

    /**
     * 测试条件是否满足
     *
     * @return true如果条件满足，应该触发转换
     */
    boolean test();

    /**
     * 创建一个始终返回true的条件
     *
     * @return 始终满足的条件
     */
    static TransitionCondition always() {
        return () -> true;
    }

    /**
     * 创建一个始终返回false的条件
     *
     * @return 始终不满足的条件
     */
    static TransitionCondition never() {
        return () -> false;
    }

    /**
     * 组合多个条件（AND逻辑）
     *
     * @param other 另一个条件
     * @return 组合后的条件
     */
    default TransitionCondition and(TransitionCondition other) {
        return () -> this.test() && other.test();
    }

    /**
     * 组合多个条件（OR逻辑）
     *
     * @param other 另一个条件
     * @return 组合后的条件
     */
    default TransitionCondition or(TransitionCondition other) {
        return () -> this.test() || other.test();
    }

    /**
     * 取反条件（NOT逻辑）
     *
     * @return 取反后的条件
     */
    default TransitionCondition negate() {
        return () -> !this.test();
    }
}
