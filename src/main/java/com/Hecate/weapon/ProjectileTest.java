package com.Hecate.weapon;

import com.jme3.math.Vector3f;

/**
 * 弹道测试 - 验证子弹飞行物理
 *
 * 测试场景：
 * 1. LINEAR（直线）：匀速直线飞行
 * 2. BALLISTIC（抛物线）：受重力影响，轨迹弯曲
 * 3. 生命周期：traveled vs maxRange
 * 4. 事件系统：onHit/onExpire触发
 */
public class ProjectileTest {

    public static void main(String[] args) {
        System.out.println("========== 子弹弹道测试 ==========\n");

        // 测试1：直线弹道
        testLinearTrajectory();

        // 测试2：抛物线弹道
        testBallisticTrajectory();

        // 测试3：生命周期管理
        testLifecycle();

        // 测试4：蓄力影响
        testChargeEffect();

        System.out.println("\n========== 测试完成 ==========");
    }

    /**
     * 测试直线弹道
     * 预期：子弹沿初始方向匀速飞行，不受重力影响
     */
    private static void testLinearTrajectory() {
        System.out.println("========== 测试 LINEAR 弹道 ==========");

        // 创建直线子弹配置
        ProjectileProfile linearProfile = new ProjectileProfile.Builder(
                "test_linear", "测试直线弹")
                .arcType(ProjectileProfile.ArcType.LINEAR)
                .velocity(20.0f)            // 20米/秒
                .maxRange(100.0f)           // 100米射程
                .maxLifetime(10.0f)
                .build();

        // 发射：从(0,5,0)向前(1,0,0)
        Vector3f startPos = new Vector3f(0, 5, 0);
        Vector3f direction = new Vector3f(1, 0, 0);
        Projectile bullet = new Projectile(linearProfile, startPos, direction, 1.0f, 0);

        System.out.println("初始位置: " + startPos);
        System.out.println("发射方向: " + direction);
        System.out.println("初速度: " + bullet.getVelocity());
        System.out.println();

        // 模拟5秒飞行（每帧0.016秒）
        float deltaTime = 0.016f;
        System.out.println("飞行轨迹（每秒采样）:");
        for (int frame = 0; frame < 5 * 60; frame++) {
            bullet.update(deltaTime);

            // 每60帧（约1秒）输出一次
            if (frame % 60 == 0) {
                Vector3f pos = bullet.getPosition();
                System.out.println(String.format("  %.1f秒: pos=(%.2f, %.2f, %.2f), traveled=%.2fm",
                        frame / 60.0f, pos.x, pos.y, pos.z, bullet.getTraveled()));

                // 验证y坐标不变（不受重力影响）
                if (Math.abs(pos.y - 5.0f) > 0.01f) {
                    System.out.println("    ✗ 错误：y坐标改变！应该保持5.0");
                }
            }
        }

        System.out.println("\n最终距离: " + String.format("%.2fm", bullet.getTraveled()));
        System.out.println("理论距离: " + String.format("%.2fm", 20.0f * 5.0f));
        System.out.println();
    }

    /**
     * 测试抛物线弹道
     * 预期：子弹受重力影响，y轴速度逐渐下降，形成抛物线
     */
    private static void testBallisticTrajectory() {
        System.out.println("========== 测试 BALLISTIC 弹道（抛物线） ==========");

        // 创建抛物线子弹配置
        ProjectileProfile ballisticProfile = new ProjectileProfile.Builder(
                "test_ballistic", "测试抛物线")
                .arcType(ProjectileProfile.ArcType.BALLISTIC)
                .velocity(15.0f)            // 15米/秒
                .gravity(-9.8f)             // 标准重力加速度
                .drag(0.99f)                // 轻微空气阻力
                .maxRange(50.0f)
                .maxLifetime(10.0f)
                .build();

        // 发射：从(0,2,0)向斜上方45度
        Vector3f startPos = new Vector3f(0, 2, 0);
        float angle = (float)Math.toRadians(45);  // 45度仰角
        Vector3f direction = new Vector3f(
                (float)Math.cos(angle),
                (float)Math.sin(angle),
                0
        );
        Projectile bullet = new Projectile(ballisticProfile, startPos, direction, 1.0f, 0);

        System.out.println("初始位置: " + startPos);
        System.out.println("发射角度: 45度");
        System.out.println("初速度: " + bullet.getVelocity());
        System.out.println();

        // 模拟飞行直到落地或超时
        float deltaTime = 0.016f;
        float maxHeight = 2.0f;
        boolean reachedPeak = false;

        System.out.println("飞行轨迹:");
        for (int frame = 0; frame < 500; frame++) {
            Vector3f oldPos = bullet.getPosition();
            Vector3f oldVel = bullet.getVelocity();

            bullet.update(deltaTime);

            Vector3f pos = bullet.getPosition();
            Vector3f vel = bullet.getVelocity();

            // 记录最高点
            if (pos.y > maxHeight) {
                maxHeight = pos.y;
            }

            // 检测最高点（y轴速度从正变负）
            if (oldVel.y > 0 && vel.y <= 0 && !reachedPeak) {
                reachedPeak = true;
                System.out.println(String.format("  最高点: %.1f秒, y=%.2fm, x=%.2fm",
                        frame * deltaTime, pos.y, pos.x));
            }

            // 每0.5秒输出一次
            if (frame % 30 == 0) {
                System.out.println(String.format("  %.2f秒: pos=(%.2f, %.2f, %.2f), vel=(%.2f, %.2f, %.2f)",
                        frame * deltaTime, pos.x, pos.y, pos.z, vel.x, vel.y, vel.z));
            }

            // 落地检测
            if (pos.y <= 0) {
                System.out.println(String.format("  ✓ 落地: %.2f秒, x=%.2fm",
                        frame * deltaTime, pos.x));
                break;
            }

            if (!bullet.isAlive()) {
                System.out.println("  子弹消失");
                break;
            }
        }

        System.out.println();
    }

    /**
     * 测试生命周期管理
     * 预期：子弹在达到maxRange或maxLifetime时自动消失
     */
    private static void testLifecycle() {
        System.out.println("========== 测试生命周期管理 ==========");

        // 测试1：射程限制
        System.out.println("测试1: 射程限制（maxRange=10m）");
        ProjectileProfile shortRange = new ProjectileProfile.Builder(
                "test_short", "短射程")
                .arcType(ProjectileProfile.ArcType.LINEAR)
                .velocity(20.0f)
                .maxRange(10.0f)            // 短射程
                .maxLifetime(100.0f)
                .build();

        Projectile bullet1 = new Projectile(shortRange,
                new Vector3f(0, 0, 0), new Vector3f(1, 0, 0), 1.0f, 0);

        // 添加事件监听
        bullet1.addListener(event -> {
            System.out.println("  事件触发: " + event);
        });

        // 模拟到消失
        float deltaTime = 0.016f;
        int frameCount = 0;
        while (bullet1.isAlive() && frameCount < 1000) {
            bullet1.update(deltaTime);
            frameCount++;
        }

        System.out.println("  飞行时间: " + String.format("%.2f秒", frameCount * deltaTime));
        System.out.println("  飞行距离: " + String.format("%.2fm", bullet1.getTraveled()));
        System.out.println();

        // 测试2：时间限制
        System.out.println("测试2: 时间限制（maxLifetime=1s）");
        ProjectileProfile shortLife = new ProjectileProfile.Builder(
                "test_life", "短生命")
                .arcType(ProjectileProfile.ArcType.LINEAR)
                .velocity(20.0f)
                .maxRange(1000.0f)
                .maxLifetime(1.0f)          // 短生命
                .build();

        Projectile bullet2 = new Projectile(shortLife,
                new Vector3f(0, 0, 0), new Vector3f(1, 0, 0), 1.0f, 0);

        bullet2.addListener(event -> {
            System.out.println("  事件触发: " + event);
        });

        frameCount = 0;
        while (bullet2.isAlive() && frameCount < 1000) {
            bullet2.update(deltaTime);
            frameCount++;
        }

        System.out.println("  存活时间: " + String.format("%.2f秒", bullet2.getLifetime()));
        System.out.println("  飞行距离: " + String.format("%.2fm", bullet2.getTraveled()));
        System.out.println();
    }

    /**
     * 测试蓄力影响
     * 预期：蓄力倍率影响速度和射程
     */
    private static void testChargeEffect() {
        System.out.println("========== 测试蓄力影响 ==========");

        ProjectileProfile profile = new ProjectileProfile.Builder(
                "test_charge", "蓄力测试")
                .arcType(ProjectileProfile.ArcType.LINEAR)
                .velocity(10.0f)            // 基础速度
                .maxRange(20.0f)            // 基础射程
                .maxLifetime(10.0f)
                .build();

        Vector3f startPos = new Vector3f(0, 0, 0);
        Vector3f direction = new Vector3f(1, 0, 0);

        // 测试不同蓄力倍率
        float[] chargeMultipliers = {0.5f, 1.0f, 1.5f, 2.0f};

        for (float chargeMult : chargeMultipliers) {
            Projectile bullet = new Projectile(profile, startPos, direction, chargeMult, 0);

            System.out.println(String.format("蓄力倍率: %.1fx", chargeMult));
            System.out.println("  初速度: " + String.format("%.2fm/s", bullet.getVelocity().length()));
            System.out.println("  最大射程: " + String.format("%.2fm", profile.getMaxRange() * chargeMult));

            // 模拟到消失
            float deltaTime = 0.016f;
            while (bullet.isAlive()) {
                bullet.update(deltaTime);
            }

            System.out.println("  实际飞行: " + String.format("%.2fm", bullet.getTraveled()));
            System.out.println();
        }
    }
}
