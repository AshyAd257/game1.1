package com.Hecate.puppet.ecs;

import java.util.List;

/**
 * 系统接口
 * ECS架构中的系统（System）- 处理具有特定组件的实体
 *
 * 系统包含逻辑，但不包含数据
 */
public interface System {

    /**
     * 系统初始化
     * 在系统第一次被添加到SystemManager时调用
     */
    default void initialize() {
        // 默认空实现
    }

    /**
     * 更新系统
     * 每帧调用一次，处理所有符合条件的实体
     *
     * @param entities 所有实体列表
     * @param tpf 时间步长（秒）
     */
    void update(List<Entity> entities, float tpf);

    /**
     * 获取系统需要的组件类型
     * 系统只会处理包含所有这些组件的实体
     *
     * @return 所需组件类型的数组
     */
    Class<? extends Component>[] getRequiredComponents();

    /**
     * 系统的执行优先级
     * 数值越小，越早执行
     *
     * @return 优先级（默认0）
     */
    default int getPriority() {
        return 0;
    }

    /**
     * 系统是否启用
     *
     * @return true如果系统启用
     */
    default boolean isEnabled() {
        return true;
    }

    /**
     * 设置系统启用状态
     *
     * @param enabled true启用，false禁用
     */
    default void setEnabled(boolean enabled) {
        // 子类可以重写此方法来支持启用/禁用
    }

    /**
     * 系统清理
     * 在系统被移除时调用，用于释放资源
     */
    default void cleanup() {
        // 默认空实现
    }
}
