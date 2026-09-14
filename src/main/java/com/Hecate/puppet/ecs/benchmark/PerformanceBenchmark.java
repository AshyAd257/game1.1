package com.Hecate.puppet.ecs.benchmark;

import com.Hecate.puppet.core.Bone;
import com.Hecate.puppet.ecs.ComponentManager;
import com.Hecate.puppet.ecs.SystemManager;
import com.Hecate.puppet.ecs.entity.BoneEntity;
import com.Hecate.puppet.ecs.system.TransformSystem;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;

/**
 * 性能基准测试
 * 对比旧Bone类和新ECS架构的性能差异
 *
 * 测试场景：
 * 1. 创建10个骨骼的层级结构
 * 2. 每帧查询5次世界变换
 * 3. 运行1000帧
 */
public class PerformanceBenchmark {

    private static final int BONE_COUNT = 10;
    private static final int QUERIES_PER_FRAME = 5;
    private static final int FRAME_COUNT = 1000;

    public static void main(String[] args) {
        System.out.println("=== Performance Benchmark: Old Bone vs New ECS ===\n");
        System.out.println("Test scenario:");
        System.out.println("  - Bones: " + BONE_COUNT);
        System.out.println("  - Queries per frame: " + QUERIES_PER_FRAME);
        System.out.println("  - Frames: " + FRAME_COUNT);
        System.out.println("  - Total queries: " + (BONE_COUNT * QUERIES_PER_FRAME * FRAME_COUNT));
        System.out.println();

        // 预热JVM
        System.out.println("Warming up JVM...");
        warmup();
        System.out.println();

        // 测试旧Bone实现
        long oldTime = benchmarkOldBone();

        // 测试新ECS实现
        long newTime = benchmarkNewECS();

        // 打印结果
        System.out.println("\n=== Results ===");
        System.out.println("Old Bone implementation: " + (oldTime / 1_000_000) + " ms");
        System.out.println("New ECS implementation:  " + (newTime / 1_000_000) + " ms");
        System.out.println();

        double speedup = (double) oldTime / newTime;
        System.out.printf("Speedup: %.2fx faster%n", speedup);
        System.out.printf("Performance improvement: %.1f%%%n", (speedup - 1) * 100);
        System.out.println();

        if (speedup >= 5.0) {
            System.out.println("✓ Excellent! Achieved 5x+ speedup as expected.");
        } else if (speedup >= 3.0) {
            System.out.println("✓ Good! Significant performance improvement.");
        } else if (speedup >= 1.5) {
            System.out.println("✓ Moderate performance improvement.");
        } else {
            System.out.println("⚠ Performance improvement is less than expected.");
        }
    }

    /**
     * 预热JVM
     */
    private static void warmup() {
        for (int i = 0; i < 100; i++) {
            benchmarkOldBoneInternal(10, 10, 10);
            benchmarkNewECSInternal(10, 10, 10);
        }
    }

    /**
     * 测试旧Bone实现
     */
    private static long benchmarkOldBone() {
        System.out.println("Testing old Bone implementation...");
        long startTime = System.nanoTime();

        benchmarkOldBoneInternal(BONE_COUNT, QUERIES_PER_FRAME, FRAME_COUNT);

        long endTime = System.nanoTime();
        return endTime - startTime;
    }

    /**
     * 旧Bone实现内部逻辑
     */
    private static void benchmarkOldBoneInternal(int boneCount, int queriesPerFrame, int frameCount) {
        // 创建骨骼层级
        Bone root = new Bone("root");
        Bone current = root;

        for (int i = 1; i < boneCount; i++) {
            Bone child = new Bone("bone_" + i);
            child.setLocalPosition(new Vector3f(0, 0.1f, 0));
            current.addChild(child);
            current = child;
        }

        // 临时变量
        Vector3f tempPos = new Vector3f();
        Quaternion tempRot = new Quaternion();
        Vector3f tempScale = new Vector3f();

        // 模拟帧循环
        for (int frame = 0; frame < frameCount; frame++) {
            // 每帧查询多次世界变换（模拟实际使用）
            for (int query = 0; query < queriesPerFrame; query++) {
                // 查询所有骨骼的世界变换
                Bone bone = root;
                while (bone != null) {
                    // 【关键】每次调用都会递归计算，没有缓存
                    bone.getWorldTransform(tempPos, tempRot, tempScale);

                    // 获取下一个骨骼（遍历子骨骼）
                    if (!bone.getChildren().isEmpty()) {
                        bone = bone.getChildren().get(0);
                    } else {
                        bone = null;
                    }
                }
            }

            // 偶尔修改一下根骨骼位置（模拟动画）
            if (frame % 100 == 0) {
                root.setLocalPosition(new Vector3f(frame * 0.001f, 0, 0));
            }
        }
    }

    /**
     * 测试新ECS实现
     */
    private static long benchmarkNewECS() {
        System.out.println("Testing new ECS implementation...");
        long startTime = System.nanoTime();

        benchmarkNewECSInternal(BONE_COUNT, QUERIES_PER_FRAME, FRAME_COUNT);

        long endTime = System.nanoTime();
        return endTime - startTime;
    }

    /**
     * 新ECS实现内部逻辑
     */
    private static void benchmarkNewECSInternal(int boneCount, int queriesPerFrame, int frameCount) {
        // 创建管理器
        ComponentManager componentManager = new ComponentManager();
        SystemManager systemManager = new SystemManager(componentManager);
        systemManager.addSystem(new TransformSystem());

        // 创建骨骼层级
        BoneEntity root = new BoneEntity("root");
        componentManager.addEntity(root);

        BoneEntity current = root;
        for (int i = 1; i < boneCount; i++) {
            BoneEntity child = new BoneEntity("bone_" + i);
            child.setLocalPosition(new Vector3f(0, 0.1f, 0));
            child.setParent(current);
            componentManager.addEntity(child);
            current = child;
        }

        // 模拟帧循环
        for (int frame = 0; frame < frameCount; frame++) {
            // 更新系统（只在变换改变时重新计算）
            systemManager.update(0.016f);

            // 每帧查询多次世界变换（模拟实际使用）
            for (int query = 0; query < queriesPerFrame; query++) {
                // 查询所有骨骼的世界变换
                BoneEntity bone = root;
                while (bone != null) {
                    // 【关键】使用缓存，无需重新计算
                    Vector3f worldPos = bone.getWorldPosition();
                    Quaternion worldRot = bone.getWorldRotation();
                    Vector3f worldScale = bone.getWorldScale();

                    // 获取下一个骨骼
                    if (bone.getHierarchy().hasChildren()) {
                        bone = (BoneEntity) bone.getHierarchy().getChildren().get(0);
                    } else {
                        bone = null;
                    }
                }
            }

            // 偶尔修改一下根骨骼位置（模拟动画）
            if (frame % 100 == 0) {
                root.setLocalPosition(new Vector3f(frame * 0.001f, 0, 0));
            }
        }

        // 清理
        systemManager.clear();
        componentManager.clear();
    }
}
