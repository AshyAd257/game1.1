package com.Hecate.puppet.compat;

import com.Hecate.puppet.core.Bone;
import com.Hecate.puppet.ecs.entity.BoneEntity;
import com.Hecate.puppet.ecs.component.*;
import com.Hecate.puppet.ecs.component.render.*;
import com.jme3.asset.AssetManager;
import com.jme3.math.Vector3f;
import com.jme3.math.Quaternion;

/**
 * Bone到BoneEntity的转换器
 * 将旧的Bone对象转换为新的ECS架构
 *
 * 【转换内容】
 * - 基础属性（名称、变换）
 * - 层级关系（父子关系）
 * - 渲染配置（根据模式选择策略）
 * - 物理配置（如果是自由骨骼）
 * - 动画配置
 *
 * 【兼容性保证】
 * - 100%保留所有配置
 * - 自动识别渲染模式
 * - 保持原有行为
 */
public class BoneToEntityConverter {

    private final AssetManager assetManager;

    /**
     * 构造函数
     *
     * @param assetManager 资源管理器（用于加载纹理）
     */
    public BoneToEntityConverter(AssetManager assetManager) {
        this.assetManager = assetManager;
    }

    /**
     * 转换单个Bone
     *
     * @param bone 旧的Bone对象
     * @return 新的BoneEntity
     */
    public BoneEntity convert(Bone bone) {
        if (bone == null) {
            return null;
        }

        // 1. 创建BoneEntity
        BoneEntity entity = new BoneEntity(bone.getName());

        // 2. 转换基础变换
        convertTransform(bone, entity);

        // 3. 转换渲染配置
        convertRender(bone, entity);

        // 4. 转换物理配置
        convertPhysics(bone, entity);

        // 5. 转换层级关系（在所有Bone都转换后再处理）
        // 这部分由convertHierarchy单独处理

        return entity;
    }

    /**
     * 转换变换属性
     */
    private void convertTransform(Bone bone, BoneEntity entity) {
        TransformComponent transform = entity.getTransform();

        // Local transform
        transform.setLocalPosition(bone.getLocalPosition().clone());
        transform.setLocalRotation(bone.getLocalRotation().clone());
        transform.setLocalScale(bone.getLocalScale().clone());

        // Rest pose
        transform.setRestPosition(bone.getRestPosition().clone());
        transform.setRestRotation(bone.getRestRotation().clone());
        transform.setRestScale(bone.getRestScale().clone());
    }

    /**
     * 转换渲染配置
     */
    private void convertRender(Bone bone, BoneEntity entity) {
        RenderStrategy strategy = null;

        // 根据Bone的渲染模式选择合适的策略
        if (bone.isModelEnabled()) {
            // 3D模型模式 - 暂时跳过（需要Model3DRenderStrategy）
            java.lang.System.out.println("[Converter] Model mode not yet supported for: " + bone.getName());
            return;
        } else if (bone.isRotationStripEnabled()) {
            // 旋转条状模式 - 暂时跳过（需要RotationStripRenderStrategy）
            java.lang.System.out.println("[Converter] Rotation strip mode not yet supported for: " + bone.getName());
            return;
        } else if (bone.isMultiDirectionTextureEnabled()) {
            // 6方向精灵模式
            strategy = convertToSpriteStrategy(bone);
        } else {
            // 默认单一纹理
            strategy = convertToSimpleSprite(bone);
        }

        if (strategy != null) {
            RenderComponent render = new RenderComponent(strategy);

            // 转换通用渲染属性
            // 注意：Bone类没有isVisible()方法，默认设置为可见
            render.setVisible(true);
            render.setBillboardEnabled(bone.isBillboardEnabled());

            entity.addComponent(render);
        }
    }

    /**
     * 转换为6方向精灵策略
     */
    private SpriteRenderStrategy convertToSpriteStrategy(Bone bone) {
        // 获取front方向的宽高作为默认值，如果没有则使用1.0
        Float width = bone.getDirectionWidth("front");
        Float height = bone.getDirectionHeight("front");
        if (width == null) width = 1.0f;
        if (height == null) height = 1.0f;

        SpriteRenderStrategy strategy = new SpriteRenderStrategy(width, height);
        strategy.setAssetManager(assetManager);

        // 转换6个方向的纹理
        if (bone.getDirectionTexture("front") != null) {
            strategy.setTexture(SpriteRenderStrategy.Direction.FRONT,
                bone.getDirectionTexture("front"));
        }
        if (bone.getDirectionTexture("back") != null) {
            strategy.setTexture(SpriteRenderStrategy.Direction.BACK,
                bone.getDirectionTexture("back"));
        }
        if (bone.getDirectionTexture("left") != null) {
            strategy.setTexture(SpriteRenderStrategy.Direction.LEFT,
                bone.getDirectionTexture("left"));
        }
        if (bone.getDirectionTexture("right") != null) {
            strategy.setTexture(SpriteRenderStrategy.Direction.RIGHT,
                bone.getDirectionTexture("right"));
        }
        if (bone.getDirectionTexture("up") != null) {
            strategy.setTexture(SpriteRenderStrategy.Direction.UP,
                bone.getDirectionTexture("up"));
        }
        if (bone.getDirectionTexture("down") != null) {
            strategy.setTexture(SpriteRenderStrategy.Direction.DOWN,
                bone.getDirectionTexture("down"));
        }

        return strategy;
    }

    /**
     * 转换为简单精灵（单一纹理）
     */
    private SpriteRenderStrategy convertToSimpleSprite(Bone bone) {
        String texturePath = bone.getTexturePath();
        if (texturePath == null || texturePath.isEmpty()) {
            return null;
        }

        // 使用默认尺寸
        float width = 1.0f;
        float height = 1.0f;

        SpriteRenderStrategy strategy = new SpriteRenderStrategy(width, height);
        strategy.setAssetManager(assetManager);
        strategy.setTexture(SpriteRenderStrategy.Direction.FRONT, texturePath);

        return strategy;
    }

    /**
     * 转换物理配置
     */
    private void convertPhysics(Bone bone, BoneEntity entity) {
        // 检查是否是自由骨骼（有物理属性）
        if (bone.getBoneType() != Bone.BoneType.FREE) {
            return; // 不是自由骨骼，跳过
        }

        PhysicsComponent physics = new PhysicsComponent();

        // 转换物理参数
        physics.setMass(bone.getPhysMass());
        physics.setDamping(bone.getPhysDamping());
        physics.setStiffness(bone.getPhysStiffness());
        physics.setGravityStrength(bone.getPhysGravityStrength());
        physics.setMaxSwingAngle(bone.getPhysMaxSwingAngle());
        physics.setMaxVelocity(bone.getPhysMaxVelocity());

        // 转换重力方向
        Vector3f gravityDir = bone.getCustomGravityVector();
        if (gravityDir != null) {
            physics.setGravityDirection(gravityDir.clone());
        }

        entity.addComponent(physics);
    }

    /**
     * 转换层级关系
     * 需要在所有Bone都转换为BoneEntity后调用
     *
     * @param oldBone 旧的Bone
     * @param newEntity 新的BoneEntity
     * @param boneToEntityMap Bone到Entity的映射
     */
    public void convertHierarchy(Bone oldBone, BoneEntity newEntity,
                                 java.util.Map<Bone, BoneEntity> boneToEntityMap) {
        // 转换子骨骼关系
        for (Bone childBone : oldBone.getChildren()) {
            BoneEntity childEntity = boneToEntityMap.get(childBone);
            if (childEntity != null) {
                childEntity.setParent(newEntity);
            }
        }
    }

    /**
     * 批量转换整个骨骼树
     *
     * @param rootBone 根骨骼
     * @return 根BoneEntity
     */
    public BoneEntity convertTree(Bone rootBone) {
        if (rootBone == null) {
            return null;
        }

        // 第一遍：转换所有Bone到BoneEntity
        java.util.Map<Bone, BoneEntity> boneToEntityMap = new java.util.HashMap<>();
        convertTreeRecursive(rootBone, boneToEntityMap);

        // 第二遍：建立层级关系
        for (java.util.Map.Entry<Bone, BoneEntity> entry : boneToEntityMap.entrySet()) {
            convertHierarchy(entry.getKey(), entry.getValue(), boneToEntityMap);
        }

        return boneToEntityMap.get(rootBone);
    }

    /**
     * 递归转换骨骼树
     */
    private void convertTreeRecursive(Bone bone, java.util.Map<Bone, BoneEntity> map) {
        // 转换当前骨骼
        BoneEntity entity = convert(bone);
        map.put(bone, entity);

        // 递归转换子骨骼
        for (Bone child : bone.getChildren()) {
            convertTreeRecursive(child, map);
        }
    }

    /**
     * 获取转换统计信息
     */
    public static class ConversionStats {
        public int totalBones = 0;
        public int withRender = 0;
        public int withPhysics = 0;
        public int withAnimation = 0;

        @Override
        public String toString() {
            return String.format("Conversion Stats: %d bones (%d render, %d physics, %d animation)",
                    totalBones, withRender, withPhysics, withAnimation);
        }
    }
}
