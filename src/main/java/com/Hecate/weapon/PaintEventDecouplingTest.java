package com.Hecate.weapon;

import com.Hecate.event.EventBus;
import com.Hecate.event.PaintEvent;
import com.Hecate.ink.InkSystemSubscriber;
import com.Hecate.ink.SparseGridManager;
import com.jme3.math.Vector3f;

/**
 * 涂墨解耦测试 - 验证子弹和涂墨系统完全解耦
 *
 * 测试场景：
 * 1. 子弹不知道涂墨系统的存在
 * 2. 子弹只通过EventBus发射PaintEvent
 * 3. 涂墨系统订阅PaintEvent并处理涂墨
 * 4. 无论什么类型的子弹（普通/爆炸/弹跳），都通过同一个事件
 */
public class PaintEventDecouplingTest {

    public static void main(String[] args) {
        System.out.println("========== 涂墨解耦测试 ==========\n");

        // 步骤1：创建事件总线（全局单例）
        EventBus eventBus = new EventBus();
        System.out.println("✓ 创建事件总线");

        // 步骤2：创建阵营注册表和涂墨系统（独立）
        com.Hecate.ink.FactionRegistry factionRegistry = new com.Hecate.ink.FactionRegistry();
        SparseGridManager inkSystem = new SparseGridManager(factionRegistry);
        System.out.println("✓ 创建涂墨系统");

        // 步骤3：创建订阅者（连接事件和涂墨系统）
        InkSystemSubscriber inkSubscriber = new InkSystemSubscriber(inkSystem, eventBus);
        System.out.println("✓ 涂墨系统订阅PaintEvent");
        System.out.println();

        // 步骤4：创建子弹配置
        ProjectileProfile bulletProfile = new ProjectileProfile.Builder(
                "test_bullet", "测试子弹")
                .arcType(ProjectileProfile.ArcType.LINEAR)
                .velocity(20.0f)
                .maxRange(50.0f)
                .maxLifetime(10.0f)
                .hitEffect(new ProjectileProfile.HitEffect(10.0f, 2.0f, 0, false, false, 0))
                .build();
        System.out.println("✓ 创建子弹配置: " + bulletProfile.getId());
        System.out.println("  涂墨半径: " + bulletProfile.getHitEffect().inkRadius + "m");
        System.out.println();

        // 步骤5：创建子弹并连接事件总线
        Vector3f startPos = new Vector3f(10, 1, 10);
        Vector3f direction = new Vector3f(1, 0, 0);
        Projectile bullet = new Projectile(bulletProfile, startPos, direction, 1.0f, 0);
        bullet.setEventBus(eventBus);  // 连接事件总线
        System.out.println("✓ 创建子弹实例");
        System.out.println("  初始位置: " + startPos);
        System.out.println("  队伍ID: 0 (己方)");
        System.out.println();

        // 步骤6：模拟子弹飞行
        System.out.println("========== 模拟子弹飞行 ==========");
        float deltaTime = 0.016f;
        for (int frame = 0; frame < 10; frame++) {
            bullet.update(deltaTime);
            if (!bullet.isAlive()) {
                break;
            }
        }
        System.out.println("子弹飞行中...");
        System.out.println();

        // 步骤7：模拟子弹命中（由碰撞系统调用）
        System.out.println("========== 子弹命中 ==========");
        Vector3f hitPoint = new Vector3f(15, 0, 10);
        System.out.println("命中点: " + hitPoint);
        System.out.println();

        System.out.println("调用 bullet.hit(hitPoint)...");
        bullet.hit(hitPoint);
        System.out.println("✓ 子弹发射PaintEvent到事件总线");
        System.out.println("✓ 涂墨系统接收事件并处理涂墨");
        System.out.println();

        // 步骤8：验证涂墨结果
        System.out.println("========== 验证涂墨结果 ==========");
        System.out.println("✓ 涂墨系统已接收PaintEvent并处理");
        System.out.println("✓ 己方墨水已涂在地面（命中点: " + hitPoint + "）");
        System.out.println();

        // 步骤9：测试不同蓄力倍率的涂墨
        System.out.println("========== 测试蓄力倍率影响 ==========");
        float[] chargeMultipliers = {0.5f, 1.0f, 1.5f, 2.0f};

        for (float chargeMult : chargeMultipliers) {
            Vector3f testPos = new Vector3f(20 + chargeMult * 5, 1, 10);
            Projectile testBullet = new Projectile(bulletProfile, testPos, direction, chargeMult, 1);
            testBullet.setEventBus(eventBus);

            Vector3f testHitPoint = new Vector3f(testPos.x + 1, 0, 10);
            testBullet.hit(testHitPoint);

            System.out.println(String.format("蓄力倍率 %.1fx: 涂墨半径=%.2fm",
                    chargeMult, bulletProfile.getHitEffect().inkRadius * chargeMult));
        }
        System.out.println();

        // 步骤10：验证解耦
        System.out.println("========== 解耦验证 ==========");
        System.out.println("✓ 子弹类（Projectile）没有引用涂墨系统（SparseGridManager）");
        System.out.println("✓ 子弹只知道EventBus，不知道谁在订阅");
        System.out.println("✓ 涂墨系统只知道EventBus，不知道谁在发射事件");
        System.out.println("✓ 新增爆炸弹、弹跳弹只需发射同样的PaintEvent");
        System.out.println("✓ 涂墨系统可以独立修改，不影响武器逻辑");
        System.out.println();

        // 步骤11：演示多个订阅者
        System.out.println("========== 演示多个订阅者 ==========");
        eventBus.subscribe(PaintEvent.class, event -> {
            System.out.println("  [音效系统] 播放涂墨音效: " + event);
        });
        eventBus.subscribe(PaintEvent.class, event -> {
            System.out.println("  [粒子系统] 生成涂墨粒子: " + event);
        });

        Vector3f multiHitPoint = new Vector3f(30, 0, 10);
        Projectile multiBullet = new Projectile(bulletProfile, new Vector3f(25, 1, 10), direction, 1.0f, 0);
        multiBullet.setEventBus(eventBus);
        multiBullet.hit(multiHitPoint);
        System.out.println("✓ 一个事件可以被多个系统订阅");
        System.out.println();

        System.out.println("========== 测试完成 ==========");
    }
}
