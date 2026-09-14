package com.Hecate.puppet.compat;

import com.Hecate.puppet.core.Bone;
import com.Hecate.puppet.core.Skeleton;
import com.Hecate.puppet.ecs.ComponentManager;
import com.Hecate.puppet.ecs.SystemManager;
import com.Hecate.puppet.ecs.entity.BoneEntity;
import com.Hecate.puppet.ecs.system.*;
import com.jme3.asset.AssetManager;

import java.util.HashMap;
import java.util.Map;

/**
 * Puppet兼容适配器
 * 提供新旧系统之间的无缝集成
 *
 * 【功能】
 * - 自动转换Skeleton到ECS
 * - 提供统一的API
 * - 透明的向后兼容
 * - 性能优化的ECS后端
 *
 * 【使用方式】
 * <pre>
 * // 加载旧格式配置并应用到骨架/渲染器
 * PuppetConfig config = PuppetIO.loadFromFile("character.puppet");
 * Skeleton oldSkeleton = new Skeleton(config.getName());
 * PuppetRenderer renderer = new PuppetRenderer(app, oldSkeleton);
 * PuppetIO.applyConfig(config, oldSkeleton, renderer);
 *
 * // 创建适配器（自动转换）
 * PuppetAdapter adapter = new PuppetAdapter(oldSkeleton, assetManager);
 *
 * // 使用ECS系统运行
 * adapter.update(tpf);
 *
 * // 保存回旧格式
 * Skeleton saved = adapter.toSkeleton();
 * PuppetConfig savedConfig = PuppetIO.createConfig(saved, renderer);
 * PuppetIO.saveToFile(savedConfig, "character.puppet");
 * </pre>
 */
public class PuppetAdapter {

    private final ComponentManager componentManager;
    private final SystemManager systemManager;
    private final BoneEntity rootEntity;
    private final AssetManager assetManager;

    // 转换器
    private final BoneToEntityConverter boneToEntity;
    private final EntityToBoneConverter entityToBone;

    // 原始Skeleton（用于保存）
    private Skeleton originalSkeleton;

    /**
     * 从旧Skeleton创建适配器
     *
     * @param skeleton 旧的Skeleton
     * @param assetManager 资源管理器
     */
    public PuppetAdapter(Skeleton skeleton, AssetManager assetManager) {
        this.assetManager = assetManager;
        this.originalSkeleton = skeleton;

        // 创建转换器
        this.boneToEntity = new BoneToEntityConverter(assetManager);
        this.entityToBone = new EntityToBoneConverter();

        // 创建ECS系统
        this.componentManager = new ComponentManager();
        this.systemManager = new SystemManager(componentManager);

        // 注册所有系统
        setupSystems();

        // 转换Skeleton到ECS
        this.rootEntity = convertSkeletonToECS(skeleton);

        java.lang.System.out.println("[PuppetAdapter] Created with " +
            componentManager.getEntityCount() + " entities");
    }

    /**
     * 从ECS创建适配器（用于新创建的Puppet）
     *
     * @param rootEntity 根实体
     * @param assetManager 资源管理器
     */
    public PuppetAdapter(BoneEntity rootEntity, AssetManager assetManager) {
        this.assetManager = assetManager;
        this.rootEntity = rootEntity;

        // 创建转换器
        this.boneToEntity = new BoneToEntityConverter(assetManager);
        this.entityToBone = new EntityToBoneConverter();

        // 创建ECS系统
        this.componentManager = new ComponentManager();
        this.systemManager = new SystemManager(componentManager);

        // 注册所有系统
        setupSystems();

        // 添加实体到管理器
        addEntityTreeToManager(rootEntity);
    }

    /**
     * 设置ECS系统
     */
    private void setupSystems() {
        systemManager.addSystem(new TransformSystem());     // -100: 变换缓存
        systemManager.addSystem(new AnimationSystem());     // -50:  动画
        systemManager.addSystem(new PhysicsSystem());       // -40:  物理
        systemManager.addSystem(new RenderSystem());        // 100:  渲染
    }

    /**
     * 转换Skeleton到ECS
     */
    private BoneEntity convertSkeletonToECS(Skeleton skeleton) {
        if (skeleton == null || skeleton.getRootBone() == null) {
            java.lang.System.err.println("[PuppetAdapter] Invalid skeleton");
            return null;
        }

        // 转换整个骨骼树
        BoneEntity root = boneToEntity.convertTree(skeleton.getRootBone());

        // 添加所有实体到ComponentManager
        addEntityTreeToManager(root);

        return root;
    }

    /**
     * 递归添加实体树到管理器
     */
    private void addEntityTreeToManager(BoneEntity entity) {
        if (entity == null) {
            return;
        }

        componentManager.addEntity(entity);

        // 递归添加子实体
        if (entity.getHierarchy() != null) {
            for (com.Hecate.puppet.ecs.Entity child : entity.getHierarchy().getChildren()) {
                if (child instanceof BoneEntity) {
                    addEntityTreeToManager((BoneEntity) child);
                }
            }
        }
    }

    /**
     * 更新Puppet（每帧调用）
     *
     * @param tpf 时间步长
     */
    public void update(float tpf) {
        systemManager.update(tpf);
    }

    /**
     * 转换回Skeleton（用于保存）
     *
     * @return Skeleton对象
     */
    public Skeleton toSkeleton() {
        if (rootEntity == null) {
            return null;
        }

        // 转换根实体为Bone
        Bone rootBone = entityToBone.convertTree(rootEntity);

        // 创建Skeleton
        Skeleton skeleton = new Skeleton(originalSkeleton != null ?
            originalSkeleton.getName() : "Skeleton");
        skeleton.setRootBone(rootBone);

        return skeleton;
    }

    /**
     * 根据名称查找实体
     *
     * @param name 实体名称
     * @return BoneEntity，如果不存在返回null
     */
    public BoneEntity findEntity(String name) {
        return (BoneEntity) componentManager.getEntityByName(name);
    }

    /**
     * 获取根实体
     *
     * @return 根BoneEntity
     */
    public BoneEntity getRootEntity() {
        return rootEntity;
    }

    /**
     * 获取ComponentManager
     *
     * @return ComponentManager
     */
    public ComponentManager getComponentManager() {
        return componentManager;
    }

    /**
     * 获取SystemManager
     *
     * @return SystemManager
     */
    public SystemManager getSystemManager() {
        return systemManager;
    }

    /**
     * 获取指定系统
     *
     * @param systemClass 系统类
     * @param <T> 系统类型
     * @return 系统实例
     */
    public <T extends com.Hecate.puppet.ecs.System> T getSystem(Class<T> systemClass) {
        return systemManager.getSystem(systemClass);
    }

    /**
     * 获取统计信息
     *
     * @return 统计字符串
     */
    public String getStats() {
        return String.format("Entities: %d, Systems: %d",
            componentManager.getEntityCount(),
            systemManager.getSystemCount());
    }

    /**
     * 清理资源
     */
    public void cleanup() {
        systemManager.clear();
        componentManager.clear();
    }

    @Override
    public String toString() {
        return String.format("PuppetAdapter[%s]", getStats());
    }
}
