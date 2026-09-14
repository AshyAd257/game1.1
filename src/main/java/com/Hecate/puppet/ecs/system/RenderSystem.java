package com.Hecate.puppet.ecs.system;

import com.Hecate.puppet.ecs.Component;
import com.Hecate.puppet.ecs.Entity;
import com.Hecate.puppet.ecs.System;
import com.Hecate.puppet.ecs.component.RenderComponent;
import com.Hecate.puppet.ecs.component.TransformComponent;
import com.Hecate.puppet.ecs.component.render.RenderStrategy;

import java.util.List;

/**
 * 渲染系统
 * 负责更新和渲染所有RenderComponent
 *
 * 【职责】
 * - 初始化渲染策略
 * - 更新渲染状态
 * - 按渲染顺序排序
 * - 应用Transform到渲染对象
 * - 处理可见性
 */
public class RenderSystem implements System {

    private boolean enabled = true;

    @Override
    public void initialize() {
        java.lang.System.out.println("[RenderSystem] Initialized");
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<? extends Component>[] getRequiredComponents() {
        return new Class[]{RenderComponent.class, TransformComponent.class};
    }

    @Override
    public int getPriority() {
        return 100; // 低优先级，在所有逻辑系统之后执行
    }

    @Override
    public void update(List<Entity> entities, float tpf) {
        // 按渲染顺序排序
        entities.sort((e1, e2) -> {
            RenderComponent r1 = e1.getComponent(RenderComponent.class);
            RenderComponent r2 = e2.getComponent(RenderComponent.class);
            return Integer.compare(r1.getRenderOrder(), r2.getRenderOrder());
        });

        // 更新每个实体的渲染
        for (Entity entity : entities) {
            RenderComponent render = entity.getComponent(RenderComponent.class);
            TransformComponent transform = entity.getComponent(TransformComponent.class);

            if (render == null || transform == null) {
                continue;
            }

            updateRender(render, transform, tpf);
        }
    }

    /**
     * 更新单个实体的渲染
     */
    private void updateRender(RenderComponent render, TransformComponent transform, float tpf) {
        if (!render.isVisible() || !render.isEnabled()) {
            return;
        }

        RenderStrategy strategy = render.getStrategy();
        if (strategy == null) {
            return;
        }

        // 更新渲染策略
        strategy.update(tpf);

        // 这里可以添加更多渲染相关的逻辑：
        // - Billboard朝向相机
        // - 应用Transform到渲染节点
        // - 应用不透明度
        // - 应用着色
        // 等等...
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public void cleanup() {
        java.lang.System.out.println("[RenderSystem] Cleaned up");
    }
}
