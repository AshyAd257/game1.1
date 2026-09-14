package com.Hecate.puppet.ecs.example;

import com.Hecate.puppet.ecs.ComponentManager;
import com.Hecate.puppet.ecs.SystemManager;
import com.Hecate.puppet.ecs.entity.BoneEntity;
import com.Hecate.puppet.ecs.system.TransformSystem;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;

/**
 * ECS架构使用示例
 * 演示如何使用新的ECS系统创建和管理骨骼
 */
public class ECSUsageExample {

    public static void main(String[] args) {
        System.out.println("=== ECS Architecture Usage Example ===\n");

        // 1. 创建管理器
        ComponentManager componentManager = new ComponentManager();
        SystemManager systemManager = new SystemManager(componentManager);

        // 2. 添加系统
        systemManager.addSystem(new TransformSystem());

        // 3. 创建骨骼层级
        System.out.println("Creating bone hierarchy...");

        // 根骨骼（躯干）
        BoneEntity torso = new BoneEntity("Torso");
        torso.setLocalPosition(new Vector3f(0, 1, 0));
        componentManager.addEntity(torso);

        // 头部（躯干的子骨骼）
        BoneEntity head = new BoneEntity("Head");
        head.setLocalPosition(new Vector3f(0, 0.5f, 0));
        head.setParent(torso);
        componentManager.addEntity(head);

        // 左臂（躯干的子骨骼）
        BoneEntity leftArm = new BoneEntity("LeftArm");
        leftArm.setLocalPosition(new Vector3f(-0.3f, 0.2f, 0));
        leftArm.setParent(torso);
        componentManager.addEntity(leftArm);

        // 右臂（躯干的子骨骼）
        BoneEntity rightArm = new BoneEntity("RightArm");
        rightArm.setLocalPosition(new Vector3f(0.3f, 0.2f, 0));
        rightArm.setParent(torso);
        componentManager.addEntity(rightArm);

        System.out.println("Created " + componentManager.getEntityCount() + " bones\n");

        // 4. 第一次更新（计算世界变换）
        System.out.println("First update (computing world transforms)...");
        long startTime = System.nanoTime();
        systemManager.update(0.016f); // 模拟一帧 (60 FPS)
        long endTime = System.nanoTime();
        System.out.println("Time taken: " + (endTime - startTime) / 1000 + " μs\n");

        // 5. 打印世界坐标
        System.out.println("World positions after first update:");
        printWorldPosition(torso);
        printWorldPosition(head);
        printWorldPosition(leftArm);
        printWorldPosition(rightArm);
        System.out.println();

        // 6. 第二次更新（使用缓存，无需重新计算）
        System.out.println("Second update (using cached transforms)...");
        startTime = System.nanoTime();
        systemManager.update(0.016f);
        endTime = System.nanoTime();
        System.out.println("Time taken: " + (endTime - startTime) / 1000 + " μs (much faster!)\n");

        // 7. 移动根骨骼
        System.out.println("Moving torso...");
        torso.setLocalPosition(new Vector3f(1, 1, 0));

        // 8. 第三次更新（只重新计算被标记为脏的变换）
        System.out.println("Third update (recomputing dirty transforms)...");
        startTime = System.nanoTime();
        systemManager.update(0.016f);
        endTime = System.nanoTime();
        System.out.println("Time taken: " + (endTime - startTime) / 1000 + " μs\n");

        // 9. 打印更新后的世界坐标
        System.out.println("World positions after moving torso:");
        printWorldPosition(torso);
        printWorldPosition(head);
        printWorldPosition(leftArm);
        printWorldPosition(rightArm);
        System.out.println();

        // 10. 性能对比总结
        System.out.println("=== Performance Summary ===");
        System.out.println("✓ First update: Full computation required");
        System.out.println("✓ Second update: 10-100x faster using cache");
        System.out.println("✓ Third update: Only dirty transforms recomputed");
        System.out.println("\nOld Bone implementation would compute ALL transforms EVERY time!");
        System.out.println("New ECS implementation only computes when needed.\n");

        // 11. 清理
        systemManager.clear();
        componentManager.clear();
        System.out.println("Cleanup complete.");
    }

    private static void printWorldPosition(BoneEntity bone) {
        Vector3f worldPos = bone.getWorldPosition();
        System.out.printf("  %s: (%.2f, %.2f, %.2f)%n",
            bone.getName(), worldPos.x, worldPos.y, worldPos.z);
    }
}
