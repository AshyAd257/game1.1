package com.Hecate.ink;

import com.jme3.math.ColorRGBA;

/**
 * ColorResolver 测试脚本
 * 脱离游戏环境，快速验证颜色计算逻辑
 *
 * 使用方式：
 * 1. 直接运行这个类的 main 方法
 * 2. 观察控制台输出的各种情况下的颜色值
 * 3. 根据输出调整 ColorResolver 中的参数
 */
public class ColorResolverTest {

    public static void main(String[] args) {
        // 初始化注册表
        FactionRegistry registry = new FactionRegistry();
        ColorResolver resolver = new ColorResolver(registry);

        System.out.println("========== ColorResolver 测试 ==========\n");

        // 测试场景 1：空地
        testEmptyGround(resolver);

        // 测试场景 2：光属性玩家视角
        testLightPlayerView(resolver, registry);

        // 测试场景 3：暗属性玩家视角
        testDarkPlayerView(resolver, registry);

        // 测试场景 4：强度衰减效果
        testIntensityDecay(resolver, registry);

        System.out.println("\n========== 测试完成 ==========");
    }

    /**
     * 测试场景 1：空地
     */
    private static void testEmptyGround(ColorResolver resolver) {
        System.out.println("【场景 1】空地");
        ColorRGBA color = resolver.resolve(FactionRegistry.NONE, 1.0f, FactionRegistry.LIGHT_DEFAULT, true);
        printColor("空地（任意观察者）", color);
        System.out.println();
    }

    /**
     * 测试场景 2：光属性玩家视角
     */
    private static void testLightPlayerView(ColorResolver resolver, FactionRegistry registry) {
        System.out.println("【场景 2】光属性玩家视角");

        // 光看光（己方）
        ColorRGBA lightSeesLight = resolver.resolve(
            FactionRegistry.LIGHT_DEFAULT,
            1.0f,
            FactionRegistry.LIGHT_DEFAULT,
            true
        );
        printColor("光看光领地（己方，不偏移）", lightSeesLight);

        // 光看暗（敌方）
        ColorRGBA lightSeesDark = resolver.resolve(
            FactionRegistry.DARK_DEFAULT,
            1.0f,
            FactionRegistry.LIGHT_DEFAULT,
            true
        );
        printColor("光看暗领地（敌方，应该变暗）", lightSeesDark);

        // 非战斗状态对比
        ColorRGBA lightSeesDarkNoCombat = resolver.resolve(
            FactionRegistry.DARK_DEFAULT,
            1.0f,
            FactionRegistry.LIGHT_DEFAULT,
            false
        );
        printColor("光看暗领地（非战斗，中性态）", lightSeesDarkNoCombat);

        System.out.println();
    }

    /**
     * 测试场景 3：暗属性玩家视角
     */
    private static void testDarkPlayerView(ColorResolver resolver, FactionRegistry registry) {
        System.out.println("【场景 3】暗属性玩家视角");

        // 暗看暗（己方）
        ColorRGBA darkSeesDark = resolver.resolve(
            FactionRegistry.DARK_DEFAULT,
            1.0f,
            FactionRegistry.DARK_DEFAULT,
            true
        );
        printColor("暗看暗领地（己方，不偏移）", darkSeesDark);

        // 暗看光（敌方）
        ColorRGBA darkSeesLight = resolver.resolve(
            FactionRegistry.LIGHT_DEFAULT,
            1.0f,
            FactionRegistry.DARK_DEFAULT,
            true
        );
        printColor("暗看光领地（敌方，应该发光刺眼）", darkSeesLight);

        // 非战斗状态对比
        ColorRGBA darkSeesLightNoCombat = resolver.resolve(
            FactionRegistry.LIGHT_DEFAULT,
            1.0f,
            FactionRegistry.DARK_DEFAULT,
            false
        );
        printColor("暗看光领地（非战斗，中性态）", darkSeesLightNoCombat);

        System.out.println();
    }

    /**
     * 测试场景 4：强度衰减效果
     */
    private static void testIntensityDecay(ColorResolver resolver, FactionRegistry registry) {
        System.out.println("【场景 4】强度衰减效果（光看暗领地）");

        float[] intensities = {1.0f, 0.75f, 0.5f, 0.25f, 0.1f};
        for (float intensity : intensities) {
            ColorRGBA color = resolver.resolve(
                FactionRegistry.DARK_DEFAULT,
                intensity,
                FactionRegistry.LIGHT_DEFAULT,
                true
            );
            printColor(String.format("强度 %.2f", intensity), color);
        }

        System.out.println();
    }

    /**
     * 打印颜色信息（格式化输出）
     */
    private static void printColor(String label, ColorRGBA color) {
        System.out.printf("  %-30s R=%.3f G=%.3f B=%.3f A=%.3f  |  亮度=%.3f\n",
            label + ":",
            color.r,
            color.g,
            color.b,
            color.a,
            calculateBrightness(color)
        );
    }

    /**
     * 计算感知亮度（简化公式）
     */
    private static float calculateBrightness(ColorRGBA color) {
        return 0.299f * color.r + 0.587f * color.g + 0.114f * color.b;
    }
}
