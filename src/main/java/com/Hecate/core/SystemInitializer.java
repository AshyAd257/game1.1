package com.Hecate.core;

import com.jme3.app.SimpleApplication;

/**
 * 系统初始化器 - 统一管理所有游戏系统的初始化流程
 *
 * <p>负责按正确的顺序初始化所有游戏系统，确保依赖关系正确。
 * 这个类从Main.java中提取出来，遵循单一职责原则。
 *
 * <h3>初始化顺序</h3>
 * <ol>
 *   <li>注册表系统（Registry）- 最先初始化，因为其他系统依赖它</li>
 *   <li>碰撞系统（Collision）- 物理检测基础</li>
 *   <li>光照系统（Lighting）- 场景光照</li>
 *   <li>游戏模块（Modules）- WorldModule, PlayerControlModule</li>
 *   <li>网格系统（Grid）- 涂墨系统</li>
 *   <li>火焰系统（Flame）- 粒子效果</li>
 *   <li>指针系统（Pointer）- 必须在PlayerController初始化后</li>
 *   <li>连接系统（Connect）- 建立各系统间的依赖关系</li>
 * </ol>
 *
 * <h3>设计原则</h3>
 * <ul>
 *   <li>单一职责：仅负责初始化流程的编排</li>
 *   <li>依赖注入：使用ApplicationContext管理依赖</li>
 *   <li>清晰的初始化顺序：按依赖关系顺序执行</li>
 *   <li>错误处理：每个步骤都有异常捕获</li>
 * </ul>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * // 在Main.java的simpleInitApp()中
 * ApplicationContext context = new ApplicationContext(this);
 * SystemInitializer.initialize(context);
 * }</pre>
 *
 * @author Hecate Team
 * @see ApplicationContext
 * @see LightingSystem
 */
public class SystemInitializer {

    /**
     * 初始化所有游戏系统
     *
     * @param context 应用程序上下文，包含所有系统实例
     */
    public static void initialize(ApplicationContext context) {

        try {
            // 第1步：初始化注册表系统
            initializeRegistries(context);

            // 第2步：初始化碰撞系统
            initializeCollisionSystem(context);

            // 第3步：初始化光照系统
            initializeLightingSystem(context);

            // 第4步：初始化游戏模块
            initializeModules(context);

            // 第5步：初始化网格系统（涂墨系统）
            initializeGridSystem(context);

            // 第6步：初始化怪物系统（必须在火焰系统之前，让FlameParticle能拿到MonsterManager引用）
            initializeMonsterSystem(context);

            // 第7步：初始化火焰系统
            initializeFlameSystem(context);

            // 第8步：初始化指针系统
            initializePointerSystem(context);

            // 第9步：初始化竞技场系统
            initializeArenaSystem(context);

            // 第10步：初始化面板系统（枪械仪表盘、说明面板）
            initializePanelSystem(context);

            // 第11步：连接各系统的依赖关系
            connectSystems(context);

        } catch (Exception e) {

            System.err.println("启动失败！");

            e.printStackTrace();
            throw new RuntimeException("系统初始化失败", e);
        }
    }

    /**
     * 第1步：初始化注册表系统
     * <p>必须最先初始化，因为其他系统依赖注册表
     */
    private static void initializeRegistries(ApplicationContext context) {
        try {
            context.initializeRegistries();
        } catch (Exception e) {
            throw new RuntimeException("注册表系统初始化失败", e);
        }
    }

    /**
     * 第2步：初始化碰撞系统
     */
    private static void initializeCollisionSystem(ApplicationContext context) {
        try {

            context.initializeCollisionSystem();

        } catch (Exception e) {
            throw new RuntimeException("碰撞系统初始化失败", e);
        }
    }

    /**
     * 第3步：初始化光照系统
     */
    private static void initializeLightingSystem(ApplicationContext context) {
        try {

            context.initializeLightingSystem();

        } catch (Exception e) {
            System.err.println("   光照系统初始化失败，继续执行...");
            e.printStackTrace();
            // 光照系统失败不应导致整个引擎崩溃
        }
    }

    /**
     * 第4步：初始化游戏模块
     * <p>依赖：注册表必须已初始化
     */
    private static void initializeModules(ApplicationContext context) {
        try {

            context.initializeModules();

        } catch (Exception e) {
            throw new RuntimeException("游戏模块初始化失败", e);
        }
    }

    /**
     * 第5步：初始化网格系统（涂墨系统）
     * <p>依赖：碰撞管理器
     */
    private static void initializeGridSystem(ApplicationContext context) {
        try {

            context.initializeGridSystem();

        } catch (Exception e) {
            System.err.println("   涂墨系统初始化失败，继续执行...");
            e.printStackTrace();
        }
    }

    /**
     * 第6步：初始化怪物系统
     * <p>无强制依赖，但必须在火焰系统之前初始化
     */
    private static void initializeMonsterSystem(ApplicationContext context) {
        try {

            context.initializeMonsterSystem();

        } catch (Exception e) {
            System.err.println("   怪物系统初始化失败，继续执行...");
            e.printStackTrace();
        }
    }

    /**
     * 第7步：初始化火焰系统
     * <p>依赖：碰撞管理器、网格管理器、玩家控制模块
     */
    private static void initializeFlameSystem(ApplicationContext context) {
        try {

            context.initializeFlameSystem();

        } catch (Exception e) {
            System.err.println("   火焰系统初始化失败，继续执行...");
            e.printStackTrace();
        }
    }

    /**
     * 第8步：初始化指针系统
     * <p>依赖：玩家控制模块（必须在PlayerController初始化后）
     */
    private static void initializePointerSystem(ApplicationContext context) {
        try {

            context.initializePointerSystem();

        } catch (Exception e) {
            System.err.println("   指针系统初始化失败，继续执行...");
            e.printStackTrace();
        }
    }

    /**
     * 第9步：初始化竞技场系统
     * <p>依赖：方块注册表、碰撞管理器
     */
    private static void initializeArenaSystem(ApplicationContext context) {
        try {

            context.initializeArenaSystem();

        } catch (Exception e) {
            System.err.println("   竞技场系统初始化失败，继续执行...");
            e.printStackTrace();
        }
    }

    /**
     * 第10步：初始化面板系统
     * <p>无强制依赖，应在连接系统之前完成，以便注入事件总线
     */
    private static void initializePanelSystem(ApplicationContext context) {
        try {

            context.initializePanelSystem();

        } catch (Exception e) {
            System.err.println("   面板系统初始化失败，继续执行...");
            e.printStackTrace();
        }
    }

    /**
     * 第11步：连接各系统的依赖关系
     * <p>必须在所有系统初始化完成后调用
     */
    private static void connectSystems(ApplicationContext context) {
        try {

            context.connectSystems();

        } catch (Exception e) {
            throw new RuntimeException("系统依赖连接失败", e);
        }
    }

    /**
     * 打印初始化配置信息
     *
     * @param context 应用程序上下文
     */
    public static void printConfiguration(ApplicationContext context) {

        // 注册表信息
        if (context.getBlockRegistry() != null) {

        }

        // 模块信息
        if (context.getWorldModule() != null) {

        }
        if (context.getPlayerControlModule() != null) {

        }

        // 系统信息
        if (context.getCollisionManager() != null) {

        }
        if (context.getLightingSystem() != null) {

        }
        if (context.getGridManager() != null) {

        }
        if (context.getFlameRenderer() != null) {

        }
        if (context.getPointerSystem() != null) {

        }

    }
}
