package com.Hecate.puppet.ecs;

/**
 * 组件接口
 * ECS架构中的组件（Component）- 纯数据容器，不包含逻辑
 *
 * 所有组件必须实现此接口，以便被ComponentManager管理
 */
public interface Component {

    /**
     * 获取组件类型
     * 用于快速识别组件类型，避免使用instanceof
     *
     * @return 组件类型的唯一标识符
     */
    default Class<? extends Component> getComponentType() {
        return this.getClass();
    }

    /**
     * 组件是否启用
     * 禁用的组件不会被系统处理
     *
     * @return true如果组件启用
     */
    default boolean isEnabled() {
        return true;
    }

    /**
     * 设置组件启用状态
     *
     * @param enabled true启用，false禁用
     */
    default void setEnabled(boolean enabled) {
        // 子类可以重写此方法来支持启用/禁用
    }

    /**
     * 克隆组件
     * 用于复制实体时复制组件数据
     *
     * @return 组件的深拷贝
     */
    default Component clone() {
        throw new UnsupportedOperationException(
            "Component " + getClass().getName() + " does not support cloning");
    }
}
