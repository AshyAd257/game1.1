package com.Hecate.core;

import com.jme3.app.SimpleApplication;

// 注册表系统
import com.Hecate.block.BlockRegistry;
import com.Hecate.blender.BlenderModelRegistry;
import com.Hecate.blockbench.BlockbenchModelRegistry;

// 核心系统
import com.Hecate.physics.CollisionManager;
import com.Hecate.pointer.PointerSystem;
import com.Hecate.flame.SimpleFlameRenderer;
import com.Hecate.ink.SparseGridManager;
import com.Hecate.ink.GridDebugRenderer;
import com.Hecate.event.EventBus;
import com.Hecate.ui.PanelManager;

// 模块系统
import com.Hecate.module.world.WorldModule;
import com.Hecate.module.player.PlayerControlModule;

/**
 * 应用程序上下文 - 中央依赖注入容器
 *
 * <p>负责创建、管理和提供所有核心系统实例。
 * 这是一个轻量级的依赖注入容器，避免了Main.java职责过重的问题。
 *
 * <h3>设计原则</h3>
 * <ul>
 *   <li>单一职责：仅负责依赖管理，不包含业务逻辑</li>
 *   <li>依赖注入：所有组件通过构造函数创建</li>
 *   <li>延迟初始化：部分系统延迟创建（如PointerSystem需要PlayerController）</li>
 *   <li>清晰的生命周期：提供cleanup方法统一清理资源</li>
 * </ul>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * ApplicationContext context = new ApplicationContext(app);
 * context.initializeRegistries();
 * context.initializeCoreSystems();
 *
 * BlockRegistry blockRegistry = context.getBlockRegistry();
 * WorldModule worldModule = context.getWorldModule();
 * }</pre>
 *
 * @see SystemInitializer
 */
public class ApplicationContext {

    private final SimpleApplication app;

    // ==================== 注册表系统 ====================
    private BlockRegistry blockRegistry;
    private BlenderModelRegistry blenderModelRegistry;
    private BlockbenchModelRegistry blockbenchModelRegistry;

    // ==================== 核心系统 ====================
    private CollisionManager collisionManager;
    private PointerSystem pointerSystem;
    private SimpleFlameRenderer flameRenderer;
    private SparseGridManager gridManager;
    private GridDebugRenderer gridDebugRenderer; // 旧渲染器（已弃用）
    private com.Hecate.ink.RegionMeshRenderer regionMeshRenderer; // 新渲染器（基于动态纹理）
    private com.Hecate.ink.DecalInkRenderer decalInkRenderer; // Decal渲染器（自适应地形）

    // 渲染器切换标志（true=Decal, false=RegionMesh）
    private static final boolean USE_DECAL_RENDERER = false;

    // ==================== 模块系统 ====================
    private WorldModule worldModule;
    private PlayerControlModule playerControlModule;

    // ==================== 光照系统 ====================
    private LightingSystem lightingSystem;

    // ==================== 竞技场系统 ====================
    private com.Hecate.arena.WorldSwitcher worldSwitcher;

    // ==================== 怪物系统 ====================
    private com.Hecate.monster.MonsterManager monsterManager;

    // ==================== 事件系统 ====================
    private final EventBus eventBus = new EventBus();

    // ==================== 面板系统 ====================
    private PanelManager panelManager;

    // ==================== 固定逻辑刻 ====================
    // 战斗判定（怪物攻击冷却、未来的buff/DOT结算、技能计时）挂在这里，
    // 保证判定时序在任意渲染帧率下一致。详见 FixedTickScheduler。
    private final FixedTickScheduler fixedTickScheduler = new FixedTickScheduler();

    // ==================== 游戏内时间调度器 ====================
    // 植物生长、环境演化等粗粒度的延迟/周期任务。详见 GameScheduler。
    private final GameScheduler gameScheduler = new GameScheduler();

    /**
     * 构造函数
     *
     * @param app SimpleApplication实例
     */
    public ApplicationContext(SimpleApplication app) {
        this.app = app;
    }

    // ==================== 初始化方法 ====================

    /**
     * 初始化所有注册表
     * <p>必须最先调用，因为模块依赖注册表实例
     */
    public void initializeRegistries() {
        blockRegistry = new BlockRegistry();
        blenderModelRegistry = new BlenderModelRegistry();
        blockbenchModelRegistry = new BlockbenchModelRegistry();
    }

    /**
     * 初始化碰撞检测系统
     */
    public void initializeCollisionSystem() {
        collisionManager = new CollisionManager();
        // 注入方块碰撞尺寸管理器：initializeRegistries()已在此之前创建了blockRegistry，
        // 让碰撞系统能查到wood1这类方块的实际（非满格）碰撞盒尺寸
        if (blockRegistry != null) {
            collisionManager.setShapeRegistry(blockRegistry.getShapeRegistry());
        }
    }

    /**
     * 初始化光照系统
     */
    public void initializeLightingSystem() {
        lightingSystem = new LightingSystem(app);
        lightingSystem.setupLighting();
    }

    /**
     * 初始化游戏模块
     * <p>依赖：注册表必须已初始化
     */
    public void initializeModules() {
        // 初始化世界模块（通过构造函数注入 BlockRegistry）
        worldModule = new WorldModule(app, blockRegistry);
        worldModule.onInitialize();

        // 初始化玩家控制模块
        playerControlModule = new PlayerControlModule(app, blockRegistry);
        playerControlModule.onInitialize();

        // 玩家效果系统（buff/debuff剩余时长倒计时、持续伤害等）挂在固定逻辑刻上，
        // 保证在任意渲染帧率下的判定时序一致。此前这套系统一直存在但未被任何地方调用。
        if (playerControlModule.getPlayerStateManager() != null) {
            fixedTickScheduler.register(
                    dt -> playerControlModule.getPlayerStateManager().getEffectManager().update(dt));
        }

        // 后初始化阶段
        worldModule.onPostInitialize();
        playerControlModule.onPostInitialize();
    }

    /**
     * 初始化涂墨网格系统
     * <p>依赖：碰撞管理器
     */
    public void initializeGridSystem() {
        // 创建阵营注册表
        com.Hecate.ink.FactionRegistry factionRegistry = new com.Hecate.ink.FactionRegistry();

        // 创建网格管理器
        gridManager = new SparseGridManager(factionRegistry);

        // 设置墨水参数
        gridManager.setInkDecayTime(60.0f);    // 墨水60秒后消退
        gridManager.setIgniteDecayTime(10.0f); // 点燃10秒后降级为涂墨

        // 创建涂墨渲染器（支持切换）
        if (USE_DECAL_RENDERER) {
            // 使用Decal渲染器（自适应地形，完美贴合斜面）
            decalInkRenderer = new com.Hecate.ink.DecalInkRenderer(app.getAssetManager(), app.getRootNode(), gridManager);
            decalInkRenderer.setWorldNode(worldModule.getWorldNode()); // 设置世界节点用于射线检测
            decalInkRenderer.setEnabled(true);

            // ColorResolver调试输入
            com.Hecate.ink.ColorResolverDebugInput colorDebugInput =
                new com.Hecate.ink.ColorResolverDebugInput(decalInkRenderer);
            colorDebugInput.registerInputs(app);
        } else {
            // 使用原有的RegionMesh渲染器（基于纹理）
            regionMeshRenderer = new com.Hecate.ink.RegionMeshRenderer(app, gridManager);
            regionMeshRenderer.setEnabled(true);

            // 设置碰撞管理器（用于获取地形高度）
            if (collisionManager != null) {
                regionMeshRenderer.setCollisionManager(collisionManager);
            }

            // ColorResolver调试输入
            com.Hecate.ink.ColorResolverDebugInput colorDebugInput =
                new com.Hecate.ink.ColorResolverDebugInput(regionMeshRenderer);
            colorDebugInput.registerInputs(app);
        }

        // 生成测试场景：在原点周围铺满光属性墨水
        com.Hecate.ink.ColorResolverTestScenario.fillLightInkAround(
            gridManager,
            new com.jme3.math.Vector3f(0, 0, 0),
            50.0f  // 50米范围测试
        );
    }

    /**
     * 初始化怪物系统
     * <p>依赖：碰撞管理器（已在initializeCollisionSystem中创建）、
     * 玩家控制模块（已在initializeModules中创建）。
     * 应在初始化火焰系统之前调用，以便 FlameParticle 能立即拿到 MonsterManager 引用用于命中检测。
     */
    public void initializeMonsterSystem() {
        monsterManager = new com.Hecate.monster.MonsterManager(app.getAssetManager());
        com.Hecate.flame.FlameParticle.setMonsterManager(monsterManager);

        // 怪物的攻击冷却判定挂在固定逻辑刻上（详见 FixedTickScheduler）
        fixedTickScheduler.register(monsterManager::fixedUpdate);

        // 碰撞管理器：用于怪物AI移动时查询地形高度、避免走入虚空
        if (collisionManager != null) {
            monsterManager.setCollisionManager(collisionManager);
        }

        // 玩家控制器：怪物的索敌目标、接触伤害的施加对象
        if (playerControlModule != null && playerControlModule.getPlayerController() != null) {
            monsterManager.setPlayerController(playerControlModule.getPlayerController());
        }
    }

    /**
     * 初始化火焰系统
     * <p>依赖：碰撞管理器、网格管理器、玩家控制模块
     */
    public void initializeFlameSystem() {
        // 设置依赖到火焰粒子系统
        if (collisionManager != null) {
            com.Hecate.flame.FlameParticle.setCollisionManager(collisionManager);
        }

        if (gridManager != null) {
            com.Hecate.flame.FlameParticle.setGridManager(gridManager);
            if (monsterManager != null) {
                monsterManager.setGridManager(gridManager);
            }
        }

        // 创建火焰渲染器
        flameRenderer = new SimpleFlameRenderer(app);

        // 设置玩家阵营到火焰粒子系统（用于涂墨）
        if (playerControlModule != null && playerControlModule.getPlayerController() != null) {
            int playerFactionId = playerControlModule.getPlayerController().getPlayerFactionId();
            flameRenderer.getParticleSystem().setFactionId(playerFactionId);
        }

        // 【整合火焰武器到PlayerController】
        if (playerControlModule != null && playerControlModule.getPlayerController() != null) {
            playerControlModule.getPlayerController().setFlameRenderer(flameRenderer);

            // 设置worldNode用于射线检测
            if (worldModule != null && worldModule.getWorldNode() != null) {
                com.Hecate.weapon.FlameWeapon flameWeapon =
                    (com.Hecate.weapon.FlameWeapon) playerControlModule.getPlayerController().getCurrentWeapon();
                if (flameWeapon != null) {
                    flameWeapon.setWorldNode(worldModule.getWorldNode());
                }
            }
        }
    }

    /**
     * 初始化指针系统
     * <p>依赖：玩家控制模块（必须在PlayerController初始化后）
     */
    public void initializePointerSystem() {
        if (playerControlModule != null && playerControlModule.getPlayerController() != null) {
            pointerSystem = new PointerSystem(app, playerControlModule.getPlayerController());
        }
    }

    /**
     * 初始化竞技场系统
     * <p>依赖：WorldModule（区块管理器/世界节点已创建）、CollisionManager、PlayerControlModule（PlayerController已创建）
     */
    public void initializeArenaSystem() {
        if (worldModule != null && worldModule.getChunkManager() != null &&
            collisionManager != null &&
            playerControlModule != null && playerControlModule.getPlayerController() != null &&
            regionMeshRenderer != null && gridManager != null) {
            worldSwitcher = new com.Hecate.arena.WorldSwitcher(
                    worldModule, collisionManager, playerControlModule,
                    regionMeshRenderer, gridManager, app.getRootNode(), monsterManager);
        }
    }

    /**
     * 初始化面板系统（枪械仪表盘、说明面板等HUD表盘）
     * <p>依赖：无强制依赖，但应在connectSystems()之前创建，以便注入事件总线
     */
    public void initializePanelSystem() {
        panelManager = new PanelManager(app, eventBus);
    }

    /**
     * 连接各个系统的依赖关系
     * <p>必须在所有系统初始化完成后调用
     */
    public void connectSystems() {
        // 【面板系统】将事件总线注入玩家控制模块，武器装备/卸下/弹药变化时通知PanelManager
        if (playerControlModule != null && playerControlModule.getPlayerController() != null) {
            playerControlModule.getPlayerController().setEventBus(eventBus);
        }

        // 将ChunkManager连接到碰撞检测系统
        if (worldModule != null && worldModule.getChunkManager() != null) {
            collisionManager.setChunkManager(worldModule.getChunkManager());
        }

        // 连接玩家控制模块到世界模块
        if (playerControlModule != null && worldModule != null) {
            playerControlModule.setChunkManager(worldModule.getChunkManager());
            playerControlModule.setWorldNode(worldModule.getWorldNode());

            // 将碰撞管理器注入到PlayerControlModule的PlayerController
            if (playerControlModule.getPlayerController() != null) {
                playerControlModule.getPlayerController().setCollisionManager(collisionManager);

                // 将PlayerController连接到WorldModule（用于方块放置等交互）
                worldModule.setPlayerController(playerControlModule.getPlayerController());

                // 【方块系统】连接方块交互系统到调试命令（/give 命令用）
                // 传方法引用而不是当前值：世界切换（竞技场）时playerControlModule内部会
                // 重新创建BlockInteraction，method reference每次调用都会取到最新的那个
                playerControlModule.getPlayerController().setBlockInteractionSupplier(playerControlModule::getBlockInteraction);
            }
        }

        // 【墨水系统】连接网格管理器到玩家控制模块（用于速度倍率）
        if (playerControlModule != null && gridManager != null) {
            playerControlModule.setGridManager(gridManager);
        }

        // 【竞技场系统】连接世界切换器到玩家控制模块（M键进入/离开竞技场）
        if (playerControlModule != null && playerControlModule.getPlayerController() != null && worldSwitcher != null) {
            playerControlModule.getPlayerController().setWorldSwitcher(worldSwitcher);
        }

        // 【怪物系统】连接怪物管理器到玩家控制模块（/mob1 命令生成怪物）
        if (playerControlModule != null && playerControlModule.getPlayerController() != null && monsterManager != null) {
            playerControlModule.getPlayerController().setMonsterManager(monsterManager);
        }
    }

    /**
     * 更新所有系统（在游戏循环中调用）
     *
     * @param tpf 每帧时间（秒）
     */
    public void update(float tpf) {
        // 更新模块
        if (worldModule != null) {
            worldModule.onUpdate(tpf);
        }

        if (playerControlModule != null) {
            playerControlModule.onUpdate(tpf);
        }

        // 更新指针系统
        if (pointerSystem != null) {
            pointerSystem.update(tpf);
        }

        // 更新火焰系统
        if (flameRenderer != null) {
            flameRenderer.update(tpf);
        }

        // 更新怪物系统（受击闪白计时、hit-stop倒计时、死亡清理+死亡涂墨）
        if (monsterManager != null) {
            monsterManager.update(tpf);
        }

        // 固定逻辑刻：战斗判定（攻击冷却到期判断等），与渲染帧率解耦
        fixedTickScheduler.update(tpf);

        // 游戏内时间调度器：植物生长/环境演化等延迟/周期任务
        gameScheduler.update(tpf);

        // 更新涂墨网格系统
        // 若WorldSwitcher存在，由它统一更新主世界+竞技场两份独立的墨水网格
        // （确保未激活的那一份墨水也按真实时间衰减，不会被"冻结"）；
        // 否则（竞技场系统未初始化成功）退化为只更新主世界的这一份。
        if (worldSwitcher != null) {
            worldSwitcher.update(tpf);
        } else if (gridManager != null) {
            gridManager.update(tpf);
        }

        // 更新涂墨渲染器
        if (USE_DECAL_RENDERER && decalInkRenderer != null) {
            decalInkRenderer.update();
        } else if (regionMeshRenderer != null && regionMeshRenderer.isEnabled()) {
            regionMeshRenderer.update();
        }
    }

    /**
     * 清理所有系统资源
     */
    public void cleanup() {
        // 清理面板系统
        if (panelManager != null) {
            panelManager.cleanup();
        }

        // 清理怪物系统
        if (monsterManager != null) {
            monsterManager.clear();
        }

        // 清理火焰系统
        if (flameRenderer != null) {
            flameRenderer.cleanup();
        }

        // 清理涂墨渲染系统
        if (USE_DECAL_RENDERER && decalInkRenderer != null) {
            decalInkRenderer.cleanup();
        } else if (regionMeshRenderer != null) {
            regionMeshRenderer.cleanup();
        }

        if (gridManager != null) {
            gridManager.clear();
        }

        // 清理竞技场系统：保存主世界和竞技场两边所有被修改过的区块
        // （worldModule.onDisable() 只会保存当前绑定的那一个ChunkManager，
        //  这里确保无论玩家退出时身处哪个世界，两边的存档都不会丢）
        if (worldSwitcher != null) {
            worldSwitcher.saveAll();
        }

        // 清理所有模块
        if (playerControlModule != null) {
            playerControlModule.onDisable();
        }

        if (worldModule != null) {
            worldModule.onDisable();
        }
    }

    // ==================== Getter方法 ====================

    public BlockRegistry getBlockRegistry() {
        return blockRegistry;
    }

    public BlenderModelRegistry getBlenderModelRegistry() {
        return blenderModelRegistry;
    }

    public BlockbenchModelRegistry getBlockbenchModelRegistry() {
        return blockbenchModelRegistry;
    }

    public CollisionManager getCollisionManager() {
        return collisionManager;
    }

    public PointerSystem getPointerSystem() {
        return pointerSystem;
    }

    public SimpleFlameRenderer getFlameRenderer() {
        return flameRenderer;
    }

    public SparseGridManager getGridManager() {
        return gridManager;
    }

    public GridDebugRenderer getGridDebugRenderer() {
        return gridDebugRenderer;
    }

    public WorldModule getWorldModule() {
        return worldModule;
    }

    public PlayerControlModule getPlayerControlModule() {
        return playerControlModule;
    }

    public LightingSystem getLightingSystem() {
        return lightingSystem;
    }

    public com.Hecate.arena.WorldSwitcher getWorldSwitcher() {
        return worldSwitcher;
    }

    public com.Hecate.monster.MonsterManager getMonsterManager() {
        return monsterManager;
    }

    public FixedTickScheduler getFixedTickScheduler() {
        return fixedTickScheduler;
    }

    public GameScheduler getGameScheduler() {
        return gameScheduler;
    }

    public EventBus getEventBus() {
        return eventBus;
    }

    public PanelManager getPanelManager() {
        return panelManager;
    }
}
