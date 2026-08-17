package com.Hecate.weapon;

/**
 * 数据流测试 - 验证从输入到实例化的完整链路
 *
 * 测试步骤：
 * 1. 从注册表获取武器定义
 * 2. 创建武器实例
 * 3. 模拟输入系统判断
 * 4. 验证状态变化
 */
public class WeaponDataFlowTest {

    public static void main(String[] args) {
        System.out.println("========== 武器数据流测试 ==========\n");

        // 步骤1：获取注册表
        WeaponRegistry registry = WeaponRegistry.getInstance();
        System.out.println("已加载武器定义数量: " + registry.getAllWeaponIds().size());
        System.out.println("已加载子弹配置数量: " + registry.getAllProjectileIds().size());
        System.out.println();

        // 步骤2：创建3种武器实例
        WeaponInstance smg = registry.createWeaponInstance("smg_01");
        WeaponInstance flameThrower = registry.createWeaponInstance("flame_thrower");
        WeaponInstance steampunk = registry.createWeaponInstance("steampunk_gun");

        System.out.println("创建的武器实例:");
        System.out.println("  - " + smg);
        System.out.println("  - " + flameThrower);
        System.out.println("  - " + steampunk);
        System.out.println();

        // 步骤3：获取子弹配置
        ProjectileProfile pelletShort = registry.getProjectileProfile("pellet_short");
        ProjectileProfile flameParticle = registry.getProjectileProfile("flame_particle");
        ProjectileProfile pelletScatter = registry.getProjectileProfile("pellet_scatter");

        System.out.println("子弹配置:");
        System.out.println("  - " + pelletShort);
        System.out.println("  - " + flameParticle);
        System.out.println("  - " + pelletScatter);
        System.out.println();

        // 步骤4：测试输入系统（AUTO模式）
        System.out.println("========== 测试AUTO模式（速射手） ==========");
        testAutoMode(smg);

        // 步骤5：测试输入系统（CHARGE模式）
        System.out.println("\n========== 测试CHARGE模式（假设武器） ==========");
        WeaponDefinition chargeDef = new WeaponDefinition.Builder("test_charge", "测试蓄力武器")
                .fireMode(WeaponDefinition.FireMode.CHARGE)
                .ammoMax(50)
                .projectileProfile("pellet_short")
                .param("chargeTime", 1.5f)
                .build();
        WeaponInstance chargeWeapon = new WeaponInstance(chargeDef);
        testChargeMode(chargeWeapon);

        // 步骤6：测试弹药消耗
        System.out.println("\n========== 测试弹药消耗 ==========");
        testAmmoConsumption(smg);

        System.out.println("\n========== 测试完成 ==========");
    }

    /**
     * 测试AUTO模式
     */
    private static void testAutoMode(WeaponInstance weapon) {
        WeaponInputHandler input = new WeaponInputHandler();
        float currentTime = 0.0f;

        System.out.println("初始状态: " + weapon);
        System.out.println("FireMode: " + weapon.getDef().getFireMode());

        // 模拟按住开火键
        System.out.println("\n模拟按住开火键5帧:");
        for (int frame = 0; frame < 5; frame++) {
            currentTime += 0.016f;  // 60fps
            input.update(true, 0.016f);

            WeaponInputHandler.FireRequest request = input.shouldFire(weapon, currentTime, 0.016f);
            if (request != null) {
                System.out.println("  帧" + frame + ": 触发开火 - " + request);
                weapon.consumeAmmo();
                weapon.recordFireTime(currentTime);
            } else {
                System.out.println("  帧" + frame + ": 冷却中...");
            }
        }

        System.out.println("最终弹药: " + weapon.getAmmo() + "/" + weapon.getDef().getAmmoMax());
    }

    /**
     * 测试CHARGE模式
     */
    private static void testChargeMode(WeaponInstance weapon) {
        WeaponInputHandler input = new WeaponInputHandler();
        float currentTime = 0.0f;

        System.out.println("初始状态: " + weapon);
        System.out.println("FireMode: " + weapon.getDef().getFireMode());

        // 模拟按住10帧（蓄力）
        System.out.println("\n模拟按住蓄力10帧:");
        for (int frame = 0; frame < 10; frame++) {
            currentTime += 0.016f;
            input.update(true, 0.016f);

            WeaponInputHandler.FireRequest request = input.shouldFire(weapon, currentTime, 0.016f);
            System.out.println("  帧" + frame + ": 蓄力进度=" + String.format("%.2f", weapon.getCharge()));
        }

        // 模拟松开（释放）
        System.out.println("\n模拟松开按键:");
        currentTime += 0.016f;
        input.update(false, 0.016f);

        WeaponInputHandler.FireRequest request = input.shouldFire(weapon, currentTime, 0.016f);
        if (request != null) {
            System.out.println("  触发蓄力攻击 - " + request);
            weapon.consumeAmmo();
        }

        System.out.println("蓄力进度已重置: " + weapon.getCharge());
    }

    /**
     * 测试弹药消耗
     */
    private static void testAmmoConsumption(WeaponInstance weapon) {
        System.out.println("初始弹药: " + weapon.getAmmo());

        // 消耗到空
        int shotCount = 0;
        while (weapon.consumeAmmo()) {
            shotCount++;
        }

        System.out.println("发射次数: " + shotCount);
        System.out.println("剩余弹药: " + weapon.getAmmo());
        System.out.println("无法继续开火: " + !weapon.consumeAmmo());

        // 装填测试
        weapon.reload(50);
        System.out.println("装填50发后: " + weapon.getAmmo());
    }
}
