package com.Hecate.puppet.compat;

import com.Hecate.puppet.core.Bone;
import com.Hecate.puppet.ecs.entity.BoneEntity;
import com.Hecate.puppet.ecs.component.*;
import com.Hecate.puppet.ecs.component.render.*;

import java.util.HashMap;
import java.util.Map;

/**
 * BoneEntity到Bone的转换器
 * 将新的ECS架构转换回旧的Bone对象
 *
 * 【用途】
 * - 保存.puppet文件（保持旧格式兼容）
 * - 与旧代码交互
 * - 导出到其他工具
 *
 * 【转换策略】
 * - 识别组件类型并转换为对应的Bone配置
 * - RenderComponent → 渲染模式配置
 * - PhysicsComponent → 自由骨骼配置
 * - AnimationComponent → 动画配置
 */
public class EntityToBoneConverter {

    /**
     * 转换单个BoneEntity
     *
     * @param entity BoneEntity
     * @return Bone对象
     */
    public Bone convert(BoneEntity entity) {
        if (entity == null) {
            return null;
        }

        // 1. 创建Bone
        Bone bone = new Bone(entity.getName());

        // 2. 转换变换
        convertTransform(entity, bone);

        // 3. 转换渲染配置
        convertRender(entity, bone);

        // 4. 转换物理配置
        convertPhysics(entity, bone);

        return bone;
    }

    /**
     * 转换变换属性
     */
    private void convertTransform(BoneEntity entity, Bone bone) {
        TransformComponent transform = entity.getTransform();
        if (transform == null) {
            return;
        }

        // Local transform
        bone.setLocalPosition(transform.getLocalPosition().clone());
        bone.setLocalRotation(transform.getLocalRotation().clone());
        bone.setLocalScale(transform.getLocalScale().clone());

        // Rest pose
        bone.setRestPosition(transform.getRestPosition().clone());
        bone.setRestRotation(transform.getRestRotation().clone());
        bone.setRestScale(transform.getRestScale().clone());
    }

    /**
     * 转换渲染配置
     */
    private void convertRender(BoneEntity entity, Bone bone) {
        RenderComponent render = entity.getComponent(RenderComponent.class);
        if (render == null) {
            return;
        }

        // 转换通用属性
        // 注意：Bone类没有setVisible()方法，跳过
        bone.setBillboardEnabled(render.isBillboardEnabled());

        // 根据渲染策略类型转换
        RenderStrategy strategy = render.getStrategy();
        if (strategy == null) {
            return;
        }

        switch (strategy.getType()) {
            case SPRITE_6DIR:
                convertSpriteStrategy(strategy, bone);
                break;
            case ROTATION_STRIP:
                // 暂不支持
                java.lang.System.out.println("[Converter] Rotation strip not yet supported");
                break;
            case MODEL_3D:
                // 暂不支持
                java.lang.System.out.println("[Converter] 3D model not yet supported");
                break;
            case BILLBOARD:
                // 暂不支持
                java.lang.System.out.println("[Converter] Billboard not yet supported");
                break;
        }
    }

    /**
     * 转换精灵策略
     */
    private void convertSpriteStrategy(RenderStrategy strategy, Bone bone) {
        if (!(strategy instanceof SpriteRenderStrategy)) {
            return;
        }

        SpriteRenderStrategy sprite = (SpriteRenderStrategy) strategy;

        // 设置尺寸到front方向
        bone.setDirectionWidth("front", sprite.getWidth());
        bone.setDirectionHeight("front", sprite.getHeight());

        // 获取纹理路径
        Map<SpriteRenderStrategy.Direction, String> textures = sprite.getTexturePaths();

        if (textures.size() > 1) {
            // 多方向模式
            bone.setMultiDirectionTextureEnabled(true);

            // 转换各方向纹理
            for (Map.Entry<SpriteRenderStrategy.Direction, String> entry : textures.entrySet()) {
                String direction = convertDirectionName(entry.getKey());
                bone.setDirectionTexture(direction, entry.getValue());
            }
        } else if (textures.size() == 1) {
            // 单一纹理模式
            bone.setMultiDirectionTextureEnabled(false);
            String texturePath = textures.values().iterator().next();
            bone.setTexturePath(texturePath);
        }
    }

    /**
     * 转换方向名称
     */
    private String convertDirectionName(SpriteRenderStrategy.Direction direction) {
        switch (direction) {
            case FRONT: return "front";
            case BACK: return "back";
            case LEFT: return "left";
            case RIGHT: return "right";
            case UP: return "up";
            case DOWN: return "down";
            default: return "front";
        }
    }

    /**
     * 转换物理配置
     */
    private void convertPhysics(BoneEntity entity, Bone bone) {
        PhysicsComponent physics = entity.getComponent(PhysicsComponent.class);
        if (physics == null) {
            // 没有物理组件，设置为连接骨骼
            bone.setBoneType(Bone.BoneType.CONNECTED);
            return;
        }

        // 有物理组件，设置为自由骨骼
        bone.setBoneType(Bone.BoneType.FREE);

        // 转换物理参数
        bone.setPhysMass(physics.getMass());
        bone.setPhysDamping(physics.getDamping());
        bone.setPhysStiffness(physics.getStiffness());
        bone.setPhysGravityStrength(physics.getGravityStrength());
        bone.setPhysMaxSwingAngle(physics.getMaxSwingAngle());
        bone.setPhysMaxVelocity(physics.getMaxVelocity());

        // 转换重力方向
        bone.setCustomGravityVector(physics.getGravityDirection().clone());
    }

    /**
     * 转换层级关系
     */
    public void convertHierarchy(BoneEntity entity, Bone bone,
                                 Map<BoneEntity, Bone> entityToBoneMap) {
        HierarchyComponent hierarchy = entity.getHierarchy();
        if (hierarchy == null) {
            return;
        }

        // 转换子节点
        for (com.Hecate.puppet.ecs.Entity child : hierarchy.getChildren()) {
            if (child instanceof BoneEntity) {
                Bone childBone = entityToBoneMap.get(child);
                if (childBone != null) {
                    bone.addChild(childBone);
                }
            }
        }
    }

    /**
     * 批量转换整个实体树
     *
     * @param rootEntity 根实体
     * @return 根Bone
     */
    public Bone convertTree(BoneEntity rootEntity) {
        if (rootEntity == null) {
            return null;
        }

        // 第一遍：转换所有BoneEntity到Bone
        Map<BoneEntity, Bone> entityToBoneMap = new HashMap<>();
        convertTreeRecursive(rootEntity, entityToBoneMap);

        // 第二遍：建立层级关系
        for (Map.Entry<BoneEntity, Bone> entry : entityToBoneMap.entrySet()) {
            convertHierarchy(entry.getKey(), entry.getValue(), entityToBoneMap);
        }

        return entityToBoneMap.get(rootEntity);
    }

    /**
     * 递归转换实体树
     */
    private void convertTreeRecursive(BoneEntity entity, Map<BoneEntity, Bone> map) {
        // 转换当前实体
        Bone bone = convert(entity);
        map.put(entity, bone);

        // 递归转换子实体
        HierarchyComponent hierarchy = entity.getHierarchy();
        if (hierarchy != null) {
            for (com.Hecate.puppet.ecs.Entity child : hierarchy.getChildren()) {
                if (child instanceof BoneEntity) {
                    convertTreeRecursive((BoneEntity) child, map);
                }
            }
        }
    }

    /**
     * 验证转换结果
     * 检查转换前后是否一致
     *
     * @param original 原始Bone
     * @param converted 转换后的Bone
     * @return true如果一致
     */
    public static boolean validate(Bone original, Bone converted) {
        if (original == null || converted == null) {
            return original == converted;
        }

        // 检查基础属性
        if (!original.getName().equals(converted.getName())) {
            return false;
        }

        // 检查变换
        if (!original.getLocalPosition().equals(converted.getLocalPosition())) {
            return false;
        }

        // 检查子节点数量
        if (original.getChildren().size() != converted.getChildren().size()) {
            return false;
        }

        return true;
    }
}
