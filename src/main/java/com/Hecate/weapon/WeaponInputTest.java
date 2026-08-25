package com.Hecate.weapon;

/**
 * 输入系统详细测试
 * 验证FireMode翻译逻辑：同一个物理按键，不同模式不同行为
 */
public class WeaponInputTest {

    public static void main(String[] args) {


        // 测试1：AUTO模式（按住持续开火）
        testAutoMode();

        // 测试2：SINGLE模式（点击一次发射一次）
        testSingleMode();

        // 测试3：CHARGE模式（蓄力松手即发，蓄多少放多少）
        testChargeMode();

        // 测试4：BURST模式（点击触发三连发）
        testBurstMode();


    }

    /**
     * 测试AUTO模式
     * 预期：按住持续开火，受射速限制（600RPM = 每0.1秒一发）
     */
    private static void testAutoMode() {


        WeaponRegistry registry = WeaponRegistry.getInstance();
        WeaponInstance smg = registry.createWeaponInstance("smg_01");
        WeaponInputHandler input = new WeaponInputHandler();

        float currentTime = 0.0f;
        float deltaTime = 0.016f;  // 60fps

        System.out.println("武器: " + smg.getDef().getDisplayName());
        System.out.println("模式: " + smg.getDef().getFireMode());
        System.out.println("射速: 每0.1秒一发\n");

        System.out.println("模拟按住10帧:");
        for (int frame = 0; frame < 10; frame++) {
            currentTime += deltaTime;
            input.update(true, deltaTime);  // 按住

            WeaponInputHandler.FireRequest request = input.shouldFire(smg, currentTime, deltaTime);
            if (request != null) {
                System.out.println("  帧" + frame + ": 开火! " + request);
                smg.consumeAmmo();
                smg.recordFireTime(currentTime);
            } else {
                System.out.println("  帧" + frame + ": 冷却中...");
            }
        }

        System.out.println("\n弹药消耗: " + (100 - (int)smg.getAmmo()) + "发\n");
    }

    /**
     * 测试SINGLE模式
     * 预期：每次点击发射一次，连续点击会受射速限制
     */
    private static void testSingleMode() {
        System.out.println("========== 测试 SINGLE 模式 ==========");

        WeaponRegistry registry = WeaponRegistry.getInstance();
        WeaponInstance pistol = registry.createWeaponInstance("steampunk_gun");
        WeaponInputHandler input = new WeaponInputHandler();

        float currentTime = 0.0f;
        float deltaTime = 0.016f;

        System.out.println("武器: " + pistol.getDef().getDisplayName());
        System.out.println("模式: " + pistol.getDef().getFireMode());
        System.out.println("射速: 每0.8秒一发\n");

        // 模拟快速点击5次
        System.out.println("模拟快速点击5次:");
        for (int click = 0; click < 5; click++) {
            currentTime += deltaTime;

            // 点击：按下1帧，松开1帧
            input.update(true, deltaTime);
            WeaponInputHandler.FireRequest request1 = input.shouldFire(pistol, currentTime, deltaTime);

            currentTime += deltaTime;
            input.update(false, deltaTime);
            WeaponInputHandler.FireRequest request2 = input.shouldFire(pistol, currentTime, deltaTime);

            if (request1 != null) {
                System.out.println("  点击" + click + ": 开火! " + request1);
                pistol.consumeAmmo();
                pistol.recordFireTime(currentTime);
            } else {
                System.out.println("  点击" + click + ": 冷却中，无法开火");
            }
        }

        System.out.println("\n弹药消耗: " + (24 - (int)pistol.getAmmo()) + "发\n");
    }

    /**
     * 测试CHARGE模式
     * 预期：
     * 1. 按住0.5秒松开 → 蓄力33%
     * 2. 按住1.5秒松开 → 蓄力100%
     * 3. 按住0.1秒松开 → 蓄力6.7%（也能发射！无minCharge限制）
     */
    private static void testChargeMode() {
        System.out.println("========== 测试 CHARGE 模式 ==========");

        // 创建一个蓄力武器
        WeaponDefinition chargeDef = new WeaponDefinition.Builder("charge_cannon", "蓄力炮")
                .fireMode(WeaponDefinition.FireMode.CHARGE)
                .ammoMax(50)
                .projectileProfile("pellet_short")
                .param("chargeTime", 1.5f)  // 1.5秒满蓄力
                .build();
        WeaponInstance cannon = new WeaponInstance(chargeDef);
        WeaponInputHandler input = new WeaponInputHandler();

        float currentTime = 0.0f;
        float deltaTime = 0.016f;

        System.out.println("武器: " + cannon.getDef().getDisplayName());
        System.out.println("模式: " + cannon.getDef().getFireMode());
        System.out.println("满蓄力时间: 1.5秒\n");

        // 测试1：蓄力0.5秒
        System.out.println("测试1: 按住0.5秒（约31帧）");
        testChargeDuration(input, cannon, 31, currentTime, deltaTime);

        // 重置
        currentTime += 1.0f;
        input.reset();
        cannon.releaseCharge();

        // 测试2：蓄力1.5秒（满蓄力）
        System.out.println("\n测试2: 按住1.5秒（约93帧）- 满蓄力");
        testChargeDuration(input, cannon, 93, currentTime, deltaTime);

        // 重置
        currentTime += 1.0f;
        input.reset();
        cannon.releaseCharge();

        // 测试3：蓄力0.1秒（极短蓄力，验证无minCharge限制）
        System.out.println("\n测试3: 按住0.1秒（约6帧）- 极短蓄力");
        testChargeDuration(input, cannon, 6, currentTime, deltaTime);

        System.out.println();
    }

    private static void testChargeDuration(WeaponInputHandler input, WeaponInstance weapon,
                                          int holdFrames, float currentTime, float deltaTime) {
        // 按住蓄力
        for (int frame = 0; frame < holdFrames; frame++) {
            currentTime += deltaTime;
            input.update(true, deltaTime);
            input.shouldFire(weapon, currentTime, deltaTime);
        }

        System.out.println("  蓄力进度: " + String.format("%.1f%%", weapon.getCharge() * 100));

        // 松开
        currentTime += deltaTime;
        input.update(false, deltaTime);
        WeaponInputHandler.FireRequest request = input.shouldFire(weapon, currentTime, deltaTime);

        if (request != null) {
            System.out.println("  松开开火: " + request);
            System.out.println("  ✓ 蓄多少放多少，无最小限制！");
        } else {
            System.out.println("  ✗ 未触发开火（错误！）");
        }
    }

    /**
     * 测试BURST模式
     * 预期：点击一次触发三连发，连发期间自动发射
     */
    private static void testBurstMode() {
        System.out.println("========== 测试 BURST 模式 ==========");

        // 创建三连发武器
        WeaponDefinition burstDef = new WeaponDefinition.Builder("burst_rifle", "三连发步枪")
                .fireMode(WeaponDefinition.FireMode.BURST)
                .ammoMax(30)
                .projectileProfile("pellet_short")
                .param("fireRate", 0.5f)           // 两轮连发间隔0.5秒
                .param("burstCount", 3)            // 每轮3发
                .param("burstInterval", 0.1f)      // 连发间隔0.1秒
                .build();
        WeaponInstance rifle = new WeaponInstance(burstDef);
        WeaponInputHandler input = new WeaponInputHandler();

        float currentTime = 0.0f;
        float deltaTime = 0.016f;

        System.out.println("武器: " + rifle.getDef().getDisplayName());
        System.out.println("模式: " + rifle.getDef().getFireMode());
        System.out.println("配置: 每轮3发，连发间隔0.1秒\n");

        // 点击一次
        System.out.println("点击一次，触发三连发:");
        input.update(true, deltaTime);
        currentTime += deltaTime;
        input.update(false, deltaTime);
        currentTime += deltaTime;

        // 模拟20帧，观察连发过程
        int shotCount = 0;
        for (int frame = 0; frame < 20; frame++) {
            currentTime += deltaTime;
            input.update(false, deltaTime);  // 松开状态

            WeaponInputHandler.FireRequest request = input.shouldFire(rifle, currentTime, deltaTime);
            if (request != null) {
                shotCount++;
                System.out.println("  帧" + frame + ": 连发第" + shotCount + "发! " + request);
                rifle.consumeAmmo();
            }
        }

        System.out.println("\n连发总数: " + shotCount + "发");
        System.out.println("剩余连发: " + input.getBurstShotsRemaining() + "发\n");
    }
}
