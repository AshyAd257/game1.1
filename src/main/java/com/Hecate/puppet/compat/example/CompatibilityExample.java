package com.Hecate.puppet.compat.example;

import com.Hecate.puppet.core.Bone;
import com.Hecate.puppet.core.Skeleton;
import com.Hecate.puppet.compat.*;
import com.Hecate.puppet.ecs.entity.BoneEntity;
import com.Hecate.puppet.ecs.component.*;
import com.jme3.math.Vector3f;
import com.jme3.math.Quaternion;

/**
 * 转换器使用示例
 * 演示新旧系统之间的无缝转换
 */
public class CompatibilityExample {

    public static void main(String[] args) {
        java.lang.System.out.println("=== Compatibility Example ===\n");

        // 1. 创建旧格式的Skeleton
        java.lang.System.out.println("[Step 1] Creating old Skeleton...");
        Skeleton oldSkeleton = createOldSkeleton();
        java.lang.System.out.println("  Created skeleton with " +
            oldSkeleton.getAllBones().size() + " bones");

        // 2. 创建PuppetAdapter（自动转换）
        java.lang.System.out.println("\n[Step 2] Converting to ECS...");
        PuppetAdapter adapter = new PuppetAdapter(oldSkeleton, null);
        java.lang.System.out.println("  " + adapter.getStats());

        // 3. 使用ECS系统运行
        java.lang.System.out.println("\n[Step 3] Running ECS simulation...");
        for (int i = 0; i < 5; i++) {
            adapter.update(0.016f); // 60 FPS

            if (i == 0 || i == 4) {
                BoneEntity head = adapter.findEntity("head");
                if (head != null) {
                    Vector3f pos = head.getWorldPosition();
                    java.lang.System.out.printf("  Frame %d - Head position: (%.2f, %.2f, %.2f)%n",
                        i, pos.x, pos.y, pos.z);
                }
            }
        }

        // 4. 转换回旧格式
        java.lang.System.out.println("\n[Step 4] Converting back to Skeleton...");
        Skeleton newSkeleton = adapter.toSkeleton();
        java.lang.System.out.println("  Converted skeleton with " +
            newSkeleton.getAllBones().size() + " bones");

        // 5. 验证转换结果
        java.lang.System.out.println("\n[Step 5] Validating conversion...");
        boolean valid = validateConversion(oldSkeleton, newSkeleton);
        if (valid) {
            java.lang.System.out.println("  ✓ Conversion validated successfully!");
        } else {
            java.lang.System.out.println("  ✗ Validation failed");
        }

        // 6. 演示直接转换器使用
        java.lang.System.out.println("\n[Step 6] Testing direct converters...");
        testDirectConverters();

        // 7. 清理
        adapter.cleanup();
        java.lang.System.out.println("\n=== Example Complete ===");
    }

    /**
     * 创建旧格式的Skeleton用于测试
     */
    private static Skeleton createOldSkeleton() {
        // 创建骨骼层级
        Bone torso = new Bone("torso");
        torso.setLocalPosition(new Vector3f(0, 1, 0));
        torso.setLocalRotation(new Quaternion());
        torso.setLocalScale(new Vector3f(1, 1, 1));
        torso.setDirectionWidth("front", 1.0f);
        torso.setDirectionHeight("front", 1.5f);
        torso.setTexturePath("textures/torso.png");

        Bone head = new Bone("head");
        head.setLocalPosition(new Vector3f(0, 0.5f, 0));
        head.setDirectionWidth("front", 0.8f);
        head.setDirectionHeight("front", 0.8f);
        head.setTexturePath("textures/head.png");
        torso.addChild(head);

        Bone leftArm = new Bone("leftArm");
        leftArm.setLocalPosition(new Vector3f(-0.3f, 0.2f, 0));
        leftArm.setDirectionWidth("front", 0.3f);
        leftArm.setDirectionHeight("front", 1.0f);
        leftArm.setTexturePath("textures/arm.png");
        torso.addChild(leftArm);

        Bone rightArm = new Bone("rightArm");
        rightArm.setLocalPosition(new Vector3f(0.3f, 0.2f, 0));
        rightArm.setDirectionWidth("front", 0.3f);
        rightArm.setDirectionHeight("front", 1.0f);
        rightArm.setTexturePath("textures/arm.png");
        torso.addChild(rightArm);

        // 创建自由骨骼（头发）
        Bone hair = new Bone("hair");
        hair.setLocalPosition(new Vector3f(0, 0.4f, 0));
        hair.setBoneType(Bone.BoneType.FREE);
        hair.setPhysMass(0.5f);
        hair.setPhysDamping(0.92f);
        hair.setPhysStiffness(40.0f);
        hair.setPhysMaxSwingAngle(60.0f);
        head.addChild(hair);

        // 创建Skeleton
        Skeleton skeleton = new Skeleton("TestCharacter");
        skeleton.setRootBone(torso);

        return skeleton;
    }

    /**
     * 验证转换结果
     */
    private static boolean validateConversion(Skeleton original, Skeleton converted) {
        // 检查骨骼数量
        if (original.getAllBones().size() != converted.getAllBones().size()) {
            java.lang.System.out.println("  Bone count mismatch: " +
                original.getAllBones().size() + " vs " + converted.getAllBones().size());
            return false;
        }

        // 检查根骨骼
        Bone origRoot = original.getRootBone();
        Bone convRoot = converted.getRootBone();

        if (!origRoot.getName().equals(convRoot.getName())) {
            java.lang.System.out.println("  Root bone name mismatch");
            return false;
        }

        // 检查位置
        if (!origRoot.getLocalPosition().equals(convRoot.getLocalPosition())) {
            java.lang.System.out.println("  Root position mismatch");
            return false;
        }

        // 检查子骨骼数量
        if (origRoot.getChildren().size() != convRoot.getChildren().size()) {
            java.lang.System.out.println("  Child count mismatch");
            return false;
        }

        return true;
    }

    /**
     * 测试直接转换器
     */
    private static void testDirectConverters() {
        // 创建一个简单的Bone
        Bone oldBone = new Bone("test");
        oldBone.setLocalPosition(new Vector3f(1, 2, 3));
        oldBone.setDirectionWidth("front", 1.5f);
        oldBone.setDirectionHeight("front", 2.0f);
        oldBone.setBoneType(Bone.BoneType.FREE);
        oldBone.setPhysMass(1.0f);

        // 转换为Entity
        BoneToEntityConverter toEntity = new BoneToEntityConverter(null);
        BoneEntity entity = toEntity.convert(oldBone);

        java.lang.System.out.println("  Bone -> Entity: " + entity.getName());
        java.lang.System.out.println("    Position: " + entity.getLocalPosition());
        java.lang.System.out.println("    Has Physics: " +
            (entity.getComponent(PhysicsComponent.class) != null));

        // 转换回Bone
        EntityToBoneConverter toBone = new EntityToBoneConverter();
        Bone newBone = toBone.convert(entity);

        java.lang.System.out.println("  Entity -> Bone: " + newBone.getName());
        java.lang.System.out.println("    Position: " + newBone.getLocalPosition());
        java.lang.System.out.println("    Type: " + newBone.getBoneType());

        // 验证
        boolean matches = EntityToBoneConverter.validate(oldBone, newBone);
        java.lang.System.out.println("  Validation: " + (matches ? "✓ Pass" : "✗ Fail"));
    }
}
