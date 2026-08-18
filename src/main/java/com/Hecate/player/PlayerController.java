package com.Hecate.player;

import com.Hecate.block.CorpseBlock;
import com.Hecate.block.CorpseBlockManager;
import com.Hecate.physics.AABB;
import com.Hecate.physics.CollisionManager;
import com.Hecate.ui.BloodDripOverlay;
import com.Hecate.ui.GameConsole;
import com.Hecate.ui.InventoryUI;
import com.Hecate.utils.LogUtils;
import com.Hecate.weapon.Weapon;
import com.Hecate.weapon.BasicShooter;
import com.jme3.app.SimpleApplication;
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseAxisTrigger;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.BillboardControl;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Texture;

import java.util.List;

/**
 * 第三人称玩家控制器
 * 功能: 基于摄像机朝向的移动控制 + 血量系统 + 死亡效果 + 2D精灵动画
 */
public class PlayerController implements ActionListener, AnalogListener {

    // 核心组件
    private final SimpleApplication app;
    private final Camera camera;
    private final InputManager inputManager;

    // 常量定义
    private static final float PLAYER_WIDTH = 0.67f;  // 宽度缩短为原来的2/3
    private static final float PLAYER_HEIGHT = 1.5f;
    private static final float GROUND_LEVEL = 2.75f;
    private static final float SPRITE_HEIGHT_OFFSET = -0.1f; // 精灵底部稍微上浮，避免Z-fighting
    private static final float CAMERA_LOOK_AT_OFFSET = 1.0f;
    private static final float CORPSE_HEIGHT = 1.6f;
    private static final float TOP_VIEW_ANGLE_THRESHOLD = 25f;

    // 移动参数
    private static final float WALK_SPEED = 3.25f;  // 普通步行速度
    private static final float SPRINT_SPEED_MULTIPLIER = 1.5f;  // 疾跑速度倍率
    private static final float FAST_MOVE_SPEED = 7f;  // 快速移动速度（2倍速度加成）
    private static final float JUMP_SPEED = 8.0f;
    private static final float GRAVITY = -20.0f;

    // 速度倍数（用于/speed指令）
    private float speedMultiplier = 1.0f;

    // 摄像机参数
    private static final float CAMERA_HEIGHT = 6.0f;
    private static final float DEFAULT_CAMERA_DISTANCE = 8.0f;
    private static final float DEFAULT_CAMERA_ANGLE_X = 0f;
    private static final float DEFAULT_CAMERA_ANGLE_Y = -20f;
    private static final float CAMERA_MIN_DISTANCE = 2.0f;
    private static final float CAMERA_MAX_DISTANCE = 15.0f;
    private static final float ZOOM_SPEED = 3.0f;
    private static final float MOUSE_SENSITIVITY = 5.0f;
    private static final float RESET_SPEED = 5.0f;

    // 独立的摄像机基准朝向（不受Q/E影响）
    // 初始值为PI（180度），让摄像机在玩家背后
    private float cameraBaseAngle = FastMath.PI;

    // 动画参数
    private static final float FACING_SMOOTH_SPEED = 8.0f;
    private static final float AUTO_REVIVE_TIME = 5.0f;
    private static final float DEATH_ANIMATION_DURATION = 0.5f;

    // 虚空死亡阈值：低于此Y坐标视为坠入虚空，判定摔死。
    // 主世界地形可挖掘到Y=-300（地形边缘填充网格的下探深度），留出足够安全边界，
    // 确保只有真正掉出任何已知地形之外才会触发，不会误伤挖掘到底部的玩家。
    private static final float VOID_DEATH_Y = -350f;

    // 玩家状态
    private Vector3f playerPosition = new Vector3f(0, GROUND_LEVEL, 0);
    private Vector3f velocity = new Vector3f();
    private Vector3f spawnPosition = new Vector3f(0, GROUND_LEVEL, 0);
    private Vector3f deathPosition = new Vector3f();

    private float playerFacing = 0f;
    private Vector3f lastMovementDirection = new Vector3f();
    private String currentSpriteDirection = "back"; // 当前精灵显示方向

    private boolean[] moveDirection = new boolean[6]; // W, S, A, D, Q, E
    private boolean isJumping = false;
    private boolean isMoving = false;
    private boolean isOnGround = false;
    private boolean isDead = false;
    // 一旦查到过一次有效地形数据就永久置true：用于区分"世界刚启动、还没生成好
    // 地形"（此时GROUND_LEVEL兜底是安全网）和"已经站在过真实地面上、现在查不到
    // 地形就是真的在虚空里"（此时不应再被隐形地板接住，应继续下落）。
    private boolean hasLandedOnTerrain = false;
    private boolean isSprinting = false;  // 疾跑状态

    // Buff选择界面弹出期间锁定玩家操作
    private boolean inputLocked = false;
    private com.Hecate.ui.BuffSelectUI buffSelectUI;

    // "攻击弹道+1"buff的叠加计数：额外发射的偏移弹道数量（FlameWeapon读取）
    private int extraProjectiles = 0;

    // 双击检测（用于疾跑）
    private float[] lastKeyPressTime = new float[4]; // W, A, S, D
    private static final float DOUBLE_TAP_THRESHOLD = 0.3f; // 300ms内算双击
    private float sprintTimer = 0f;  // 疾跑持续时间
    private static final float SPRINT_DURATION = 3.0f;  // 疾跑持续3秒

    // 碰撞系统
    private CollisionManager collisionManager;
    private AABB playerBox;
    private com.Hecate.physics.CollisionBoxRenderer collisionBoxRenderer;  // 碰撞箱可视化渲染器

    // 血量系统
    private PlayerHealth playerHealth;
    private BloodDripOverlay bloodDripOverlay;

    // 弹药系统
    private PlayerAmmo playerAmmo;

    // 恢复管理器
    private PlayerRecoveryManager recoveryManager;

    // 武器系统
    private Weapon currentWeapon;

    // 玩家阵营（默认暗属性）
    private int playerFactionId = com.Hecate.ink.FactionRegistry.DARK_DEFAULT;

    // 玩家状态系统（三选二规则）
    private boolean isRecovering = false;     // 恢复状态
    private boolean isHidingOnInk = false;    // 在涂墨地面的隐藏状态
    private boolean isHidingOnEmpty = false;  // 在无墨地面的隐藏状态
    private boolean isFastMoving = false;     // 快速移动状态
    private java.util.LinkedList<String> stateHistory = new java.util.LinkedList<>(); // 状态历史（用于"丢失最早状态"）

    // 按键状态
    private boolean isShiftPressed = false;   // Shift键是否按下
    private boolean isRightButtonPressed = false; // 右键是否按下

    // 地面类型枚举
    private enum GroundType {
        NONE,      // 无涂墨
        FRIENDLY,  // 己方涂墨
        IGNITED    // 点燃
    }
    private GroundType currentGroundType = GroundType.NONE;

    // 恢复速率（每秒5%）——非final：波次buff系统（"恢复速度变快"）需要在运行时提高这个值
    private float recoveryPercentage = 0.05f;

    // 死亡系统
    private float deathAnimationProgress = 0f;
    private float deathTimer = 0f;
    private Node deadPlayerNode;

    // 屏幕振动
    private boolean isShaking = false;
    private float shakeTimer = 0f;
    private float shakeDuration = 1.0f;
    private float shakeIntensity = 0.3f;
    private Vector3f originalCameraLocation;

    // 摄像机控制
    private float cameraDistance = DEFAULT_CAMERA_DISTANCE;
    private float cameraAngleX = DEFAULT_CAMERA_ANGLE_X;
    private float cameraAngleY = DEFAULT_CAMERA_ANGLE_Y;
    private boolean isResettingCamera = false;
    private float resetProgress = 0f;

    // 2D精灵系统
    private boolean useSpriteMode = false;
    private Node spriteNode;
    private PlayerSpriteManager spriteManager;
    private SpriteAnimationSystem spriteAnimationSystem;
    private DirectionalSpriteRenderer spriteRenderer;
    private SpriteScaleManager spriteScaleManager;

    // Puppet 动画系统
    private boolean usePuppetMode = true;  // 默认使用 puppet 模式
    private PuppetPlayerController puppetPlayerController;

    // 墨水网格系统
    private com.Hecate.ink.SparseGridManager gridManager;

    // 火焰渲染器（用于子弹粒子效果）
    private com.Hecate.flame.SimpleFlameRenderer flameRenderer;

    // UI系统
    private InventoryUI inventoryUI;
    private GameConsole gameConsole;

    // 竞技场系统（世界切换由 WorldSwitcher 统一处理）
    private com.Hecate.arena.WorldSwitcher worldSwitcher;

    // 怪物系统
    private com.Hecate.monster.MonsterManager monsterManager;
    // 当前活动世界的场景节点（随setWorldNode同步更新），用于/mob1命令生成怪物到正确的世界
    private Node currentWorldNode;

    // Gun1武器系统
    private Node gun1WeaponNode = null;  // Gun1武器模型节点
    private boolean isGun1Equipped = false;  // Gun1是否装备

    // 持枪状态系统
    private boolean isHoldingGun = false;  // 是否处于持枪状态
    private boolean isLeftButtonPressed = false;  // 左键是否按下（用于连发/蓄力）
    private float continuousFireTimer = 0f;  // 连发计时器

    // 调试计数器
    private float debugTimer = 0f;
    private boolean wasTopView = false;
    private int frameCount = 0; // 帧计数器，用于控制debug输出频率

    /**
     * 构造函数
     */
    public PlayerController(SimpleApplication app) {
        this.app = app;
        this.camera = app.getCamera();
        this.inputManager = app.getInputManager();

        // 初始化血量系统
        initializeHealthSystem();

        // 初始化玩家
        initializePlayer();

        // 设置输入控制
        setupInput();

        // 初始化2D精灵系统（但不启用）
        initializeSpriteSystem();

        // 初始化 Puppet 系统
        if (usePuppetMode) {
            initializePuppetSystem();
        } else {
            // 只有在不使用 puppet 时才启用精灵模式
            enableSpriteMode();
        }

        // 更新摄像机
        updateCameraPosition();

        // 初始化游戏控制台
        initializeGameConsole();

        // 初始化背包UI
        initializeInventoryUI();

        // 初始化碰撞箱渲染器
        initializeCollisionBoxRenderer();
    }

    /**
     * 设置碰撞管理器
     */
    public void setCollisionManager(CollisionManager collisionManager) {
        this.collisionManager = collisionManager;
    }

    /**
     * 设置世界切换器（用于进入/离开竞技场）
     */
    public void setWorldSwitcher(com.Hecate.arena.WorldSwitcher worldSwitcher) {
        this.worldSwitcher = worldSwitcher;
    }

    /**
     * 初始化碰撞箱渲染器
     */
    private void initializeCollisionBoxRenderer() {
        collisionBoxRenderer = new com.Hecate.physics.CollisionBoxRenderer(
            app.getAssetManager(),
            app.getRootNode()
        );
        // 默认显示碰撞箱（可以通过按键切换）
        collisionBoxRenderer.setVisible(true);
    }

    /**
     * 初始化血量系统
     */
    private void initializeHealthSystem() {
        playerHealth = new PlayerHealth(100.0f);

        playerHealth.setHealthChangeListener(new PlayerHealth.HealthChangeListener() {
            @Override
            public void onHealthChanged(float currentHealth, float maxHealth) {
                app.enqueue(() -> {
                    if (bloodDripOverlay != null) {
                        float healthPercentage = currentHealth / maxHealth;
                        bloodDripOverlay.updateHealth(healthPercentage);
                    }
                    return null;
                });
            }

            @Override
            public void onPlayerDied() {
                app.enqueue(() -> {
                    handlePlayerDeath();
                    return null;
                });
            }

            @Override
            public void onPlayerRevived() {
                app.enqueue(() -> {
                    handlePlayerRevive();
                    if (bloodDripOverlay != null) {
                        bloodDripOverlay.startSlideOutAnimation();
                    }
                    return null;
                });
            }
        });

        // 初始化弹药系统
        playerAmmo = new PlayerAmmo();
        playerAmmo.setAmmoChangeListener(new PlayerAmmo.AmmoChangeListener() {
            @Override
            public void onAmmoChanged(float currentAmmo, float maxAmmo) {
                // TODO: 更新UI显示弹药
                LogUtils.debug(PlayerController.class, String.format(
                    "弹药: %.0f/%.0f (%.1f%%)",
                    currentAmmo, maxAmmo, (currentAmmo/maxAmmo)*100
                ));
            }

            @Override
            public void onAmmoEmpty() {

            }
        });

        // 初始化武器系统（默认使用BasicShooter，可通过setFlameRenderer切换到FlameWeapon）
        currentWeapon = BasicShooter.createDefault();

        // 初始化恢复管理器
        recoveryManager = new PlayerRecoveryManager(playerHealth, playerAmmo);

    }

    /**
     * 设置火焰渲染器并切换到火焰武器
     */
    public void setFlameRenderer(com.Hecate.flame.SimpleFlameRenderer flameRenderer) {
        // 保存火焰渲染器引用
        this.flameRenderer = flameRenderer;

        // 创建火焰武器并设置为默认武器
        com.Hecate.weapon.FlameWeapon flameWeapon = com.Hecate.weapon.FlameWeapon.createDefault(flameRenderer, camera);
        flameWeapon.setPlayerController(this);
        this.currentWeapon = flameWeapon;
    }

    /**
     * 初始化玩家模型
     */
    private void initializePlayer() {
        updatePlayerBox();
    }

    /**
     * 启用精灵模式
     */
    private void enableSpriteMode() {
        useSpriteMode = true;
        if (spriteScaleManager != null) {
            spriteScaleManager.setSpriteMode(true);
        }
        if (spriteNode != null) {
            spriteNode.setCullHint(Node.CullHint.Never);
        }
    }

    /**
     * 初始化 Puppet 动画系统
     */
    private void initializePuppetSystem() {
        try {
            puppetPlayerController = new PuppetPlayerController(app, playerPosition.clone());
            // 设置玩家阵营
            puppetPlayerController.setPlayerFactionId(playerFactionId);

        } catch (Exception e) {

            e.printStackTrace();
            // 失败时回退到精灵模式
            usePuppetMode = false;
            enableSpriteMode();
        }
    }

    /**
     * 初始化游戏控制台
     */
    private void initializeGameConsole() {
        gameConsole = new GameConsole(app);
        gameConsole.setPlayerController(this);

        // 初始化Buff选择界面（波次结算后弹出）
        buffSelectUI = new com.Hecate.ui.BuffSelectUI(app);

        // 注册Gun1命令 - 显示steampunkgun.glb模型
        registerGun1Command();

        // 注册Mob1命令 - 在玩家前方生成一只怪物
        registerMob1Command();

        // 注册Wave1命令 - 开始三波递进的刷怪遭遇战
        registerWave1Command();
    }

    /**
     * 注册Wave1命令 - 开始一次三波递进的刷怪遭遇战
     * （第1波3只慢速怪 -> 第2波6只普通怪 -> 第3波1只小Boss，杀光当前波自动进下一波）
     */
    private void registerWave1Command() {
        gameConsole.registerCommand("wave1", new GameConsole.CommandHandler() {
            @Override
            public void execute(String[] args) {
                app.enqueue(() -> {
                    try {
                        if (monsterManager == null) {
                            gameConsole.addHistory("错误: 怪物系统未初始化");
                            return null;
                        }
                        if (currentWorldNode == null) {
                            gameConsole.addHistory("错误: 当前世界节点未就绪");
                            return null;
                        }

                        boolean started = monsterManager.startWaveEncounter(currentWorldNode);
                        if (started) {
                            gameConsole.addHistory("遭遇战开始：第1波（3只慢速怪）");
                        } else {
                            gameConsole.addHistory("遭遇战已在进行中");
                        }
                    } catch (Exception e) {
                        LogUtils.error(PlayerController.class, "处理Wave1命令失败", e);
                        gameConsole.addHistory("错误: " + e.getMessage());
                    }
                    return null;
                });
            }

            @Override
            public String getDescription() {
                return "开始三波递进的刷怪遭遇战";
            }
        });
    }

    /**
     * 注册Mob1命令 - 在玩家面前生成一只怪物（最初的怪物原型：1x1红色方块）
     */
    private void registerMob1Command() {
        gameConsole.registerCommand("mob1", new GameConsole.CommandHandler() {
            @Override
            public void execute(String[] args) {
                app.enqueue(() -> {
                    try {
                        if (monsterManager == null) {
                            gameConsole.addHistory("错误: 怪物系统未初始化");
                            return null;
                        }
                        if (currentWorldNode == null) {
                            gameConsole.addHistory("错误: 当前世界节点未就绪");
                            return null;
                        }

                        // 玩家前方3米处生成，脚底高度取当前玩家所在的地形高度
                        Vector3f forward = camera.getDirection().clone();
                        forward.y = 0;
                        if (forward.lengthSquared() < 0.001f) {
                            forward.set(0, 0, 1);
                        } else {
                            forward.normalizeLocal();
                        }

                        Vector3f spawnPos = playerPosition.clone().addLocal(forward.mult(3f));

                        if (collisionManager != null) {
                            float terrainHeight = collisionManager.getTerrainHeightAt(spawnPos.x, spawnPos.z);
                            if (!Float.isNaN(terrainHeight)) {
                                spawnPos.y = terrainHeight;
                            }
                        }

                        monsterManager.spawnMonster(currentWorldNode, spawnPos);
                        gameConsole.addHistory("已在玩家前方生成一只怪物");
                    } catch (Exception e) {
                        LogUtils.error(PlayerController.class, "处理Mob1命令失败", e);
                        gameConsole.addHistory("错误: " + e.getMessage());
                    }
                    return null;
                });
            }

            @Override
            public String getDescription() {
                return "在玩家前方生成一只怪物";
            }
        });
    }

    /**
     * 注册Gun1命令 - 装备/卸下蒸汽朋克枪模型
     */
    private void registerGun1Command() {
        gameConsole.registerCommand("gun1", new GameConsole.CommandHandler() {
            @Override
            public void execute(String[] args) {
                app.enqueue(() -> {
                    try {
                        if (isGun1Equipped) {
                            // 卸下武器
                            if (gun1WeaponNode != null && gun1WeaponNode.getParent() != null) {
                                gun1WeaponNode.removeFromParent();
                            }
                            isGun1Equipped = false;

                            // 退出持枪状态
                            isHoldingGun = false;
                            isLeftButtonPressed = false;
                            continuousFireTimer = 0f;

                            // 恢复默认武器
                            setCurrentWeapon(BasicShooter.createDefault());

                            gameConsole.addHistory("蒸汽朋克枪已卸下");
                            gameConsole.addHistory("已切换回默认武器");
                            gameConsole.addHistory("已退出持枪状态");

                        } else {
                            // 装备武器
                            if (gun1WeaponNode == null) {
                                // 首次加载模型
                                Spatial weaponModel = app.getAssetManager().loadModel("weapons/steampunkgun.glb");

                                if (weaponModel == null) {
                                    gameConsole.addHistory("错误: 无法加载模型 weapons/steampunkgun.glb");

                                    return null;
                                }

                                // 创建节点容器
                                gun1WeaponNode = new Node("Gun1_SteampunkGun");
                                gun1WeaponNode.attachChild(weaponModel);

                                // 调整模型缩放
                                gun1WeaponNode.setLocalScale(0.3f);  // 缩小模型，适合持枪


                            }

                            // 添加到场景（位置会在update中更新）
                            app.getRootNode().attachChild(gun1WeaponNode);
                            isGun1Equipped = true;

                            // 进入持枪状态
                            isHoldingGun = true;

                            // 切换到蒸汽朋克枪武器
                            com.Hecate.weapon.SteampunkGun steampunkGun = com.Hecate.weapon.SteampunkGun.create();

                            // 设置武器依赖项
                            steampunkGun.setFlameRenderer(flameRenderer);  // 设置火焰渲染器（用于子弹效果）
                            steampunkGun.setGridManager(gridManager);
                            steampunkGun.setWorldNode(app.getRootNode());
                            steampunkGun.setPlayerTeam(getPlayerTeam());

                            setCurrentWeapon(steampunkGun);

                            gameConsole.addHistory("蒸汽朋克枪已装备");
                            gameConsole.addHistory(steampunkGun.getInfo());
                            gameConsole.addHistory("━━━━━━━━━━━━━━━━");
                            gameConsole.addHistory("已进入持枪状态");
                            gameConsole.addHistory("左键: 攻击（长按连发）");
                            gameConsole.addHistory("武器将跟随玩家移动");
                            gameConsole.addHistory("再次输入 /gun1 可以卸下武器");


                        }
                    } catch (Exception e) {
                        LogUtils.error(PlayerController.class, "处理Gun1命令失败", e);
                        gameConsole.addHistory("错误: " + e.getMessage());
                    }
                    return null;
                });
            }

            @Override
            public String getDescription() {
                return "装备/卸下蒸汽朋克枪（武器会跟随玩家）";
            }
        });


    }

    /**
     * 初始化背包UI
     */
    private void initializeInventoryUI() {
        inventoryUI = new InventoryUI(app);
    }

    /**
     * 处理玩家死亡
     */
    private void handlePlayerDeath() {
        isDead = true;
        deathTimer = 0f;
        deathPosition.set(playerPosition);
        deathAnimationProgress = 0f;

        // 开始屏幕振动
        startScreenShake(0.5f, 0.3f);

        // 隐藏精灵
        if (spriteNode != null) {
            spriteNode.setCullHint(Node.CullHint.Always);
        }

        // 创建死亡效果
        if (useSpriteMode) {
            createDeadSprite();
        }
    }

    /**
     * 创建死亡精灵
     */
    private void createDeadSprite() {
        Node corpseNode = new Node("PlayerCorpse");

        Quad corpseQuad = new Quad(2.0f, 2.0f);
        Geometry corpseGeometry = new Geometry("CorpseSprite", corpseQuad);
        corpseGeometry.setLocalTranslation(-1.0f, -1.0f, 0);

        Material corpseMaterial = new Material(app.getAssetManager(),
                "Common/MatDefs/Misc/Unshaded.j3md");

        // 加载尸体纹理
        try {
            Texture corpseTexture = app.getAssetManager().loadTexture(
                    "textures/player/back/idle/idle_01.png");

            if (corpseTexture != null) {
                corpseTexture.setMagFilter(Texture.MagFilter.Nearest);
                corpseTexture.setMinFilter(Texture.MinFilter.NearestNoMipMaps);
                corpseMaterial.setTexture("ColorMap", corpseTexture);
                LogUtils.debug(PlayerController.class, "尸体纹理加载成功");
            } else {
                LogUtils.warning(PlayerController.class, "尸体纹理为null，使用红色占位符");
                corpseMaterial.setColor("Color", ColorRGBA.Red);
            }
        } catch (Exception e) {
            LogUtils.error(PlayerController.class, "加载尸体纹理失败", e);
            corpseMaterial.setColor("Color", ColorRGBA.Red);
        }

        // 设置透明度
        corpseMaterial.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        corpseMaterial.setTransparent(true);
        corpseMaterial.getAdditionalRenderState().setDepthWrite(false);

        corpseGeometry.setMaterial(corpseMaterial);
        corpseGeometry.setQueueBucket(RenderQueue.Bucket.Transparent);

        corpseNode.attachChild(corpseGeometry);

        // 设置尸体位置
        Vector3f corpsePosition = playerPosition.clone();
        corpsePosition.y = CORPSE_HEIGHT;
        corpseNode.setLocalTranslation(corpsePosition);

        corpseNode.setCullHint(Spatial.CullHint.Never);
        corpseGeometry.setCullHint(Spatial.CullHint.Never);

        app.getRootNode().attachChild(corpseNode);
        this.deadPlayerNode = corpseNode;

        LogUtils.debug(PlayerController.class, "2D精灵尸体创建完成 - 位置: " + corpsePosition);
    }

    /**
     * 更新尸体朝向 - 让尸体躺着并面向摄像机
     */
    private void updateCorpseOrientation() {
        if (deadPlayerNode == null) return;

        Vector3f camPos = camera.getLocation();
        Vector3f corpsePos = deadPlayerNode.getLocalTranslation();

        Vector3f direction = camPos.subtract(corpsePos);
        direction.y = 0;

        if (direction.lengthSquared() < 0.01f) {
            return;
        }

        direction.normalizeLocal();

        float angle = FastMath.atan2(direction.x, direction.z);
        Quaternion yRotation = new Quaternion();
        yRotation.fromAngleAxis(angle, Vector3f.UNIT_Y);

        Quaternion xRotation = new Quaternion();
        xRotation.fromAngleAxis(-FastMath.HALF_PI, Vector3f.UNIT_X);

        Quaternion finalRotation = yRotation.mult(xRotation);
        deadPlayerNode.setLocalRotation(finalRotation);
    }

    /**
     * 处理玩家复活
     */
    private void handlePlayerRevive() {
        isDead = false;

        // 移除死亡节点
        if (deadPlayerNode != null) {
            deadPlayerNode.removeFromParent();
            deadPlayerNode = null;
        }

        // 重置玩家位置到出生点
        playerPosition.set(spawnPosition);
        velocity.set(0, 0, 0);
        isJumping = false;
        isOnGround = true;

        // 恢复2D精灵显示
        if (spriteNode != null) {
            spriteNode.setCullHint(Node.CullHint.Never);
        }
    }

    /**
     * 开始屏幕振动
     */
    private void startScreenShake(float duration, float intensity) {
        this.isShaking = true;
        this.shakeTimer = 0f;
        this.shakeDuration = duration;
        this.shakeIntensity = intensity;

        if (originalCameraLocation == null) {
            originalCameraLocation = new Vector3f();
        }
        originalCameraLocation.set(camera.getLocation());

        LogUtils.debug(PlayerController.class, "屏幕振动开始");
    }

    /**
     * 更新屏幕振动效果
     */
    private void updateScreenShake(float tpf) {
        if (!isShaking) {
            return;
        }

        shakeTimer += tpf;

        if (shakeTimer >= shakeDuration) {
            isShaking = false;
            shakeTimer = 0f;
            LogUtils.debug(PlayerController.class, "屏幕振动结束");
        }
    }

    /**
     * 应用屏幕振动到摄像机 (在updateCameraPosition中调用)
     * @return 振动偏移向量
     */
    private Vector3f getShakeOffset() {
        if (!isShaking) {
            return Vector3f.ZERO;
        }

        float progress = shakeTimer / shakeDuration;
        float currentIntensity = shakeIntensity * (1.0f - progress);

        float shakeX = (FastMath.nextRandomFloat() - 0.5f) * 2.0f * currentIntensity;
        float shakeY = (FastMath.nextRandomFloat() - 0.5f) * 2.0f * currentIntensity;
        float shakeZ = (FastMath.nextRandomFloat() - 0.5f) * 2.0f * currentIntensity;

        return new Vector3f(shakeX, shakeY, shakeZ);
    }

    /**
     * 更新死亡动画
     */
    private void updateDeathAnimation(float tpf) {
        if (!isDead) return;

        if (deathAnimationProgress < DEATH_ANIMATION_DURATION) {
            deathAnimationProgress += tpf;
        }
    }

    /**
     * 获取玩家正面方向向量
     */
    public Vector3f getPlayerFacingDirection() {
        return new Vector3f(
                FastMath.sin(playerFacing),
                0,
                FastMath.cos(playerFacing)
        );
    }

    /**
     * 获取摄像机朝向方向（水平投影）- 用于WASD移动
     */
    private Vector3f getCameraFacingDirection() {
        // 使用独立的cameraBaseAngle
        float totalAngleX = cameraBaseAngle + (cameraAngleX * FastMath.DEG_TO_RAD);

        return new Vector3f(
                -FastMath.sin(totalAngleX),
                0,
                -FastMath.cos(totalAngleX)
        );
    }

    /**
     * 获取摄像机右方向向量
     */
    private Vector3f getCameraRightDirection() {
        Vector3f forward = getCameraFacingDirection();
        return forward.cross(Vector3f.UNIT_Y).normalizeLocal();
    }

    /**
     * 获取玩家朝向角度（弧度）
     */
    public float getPlayerFacing() {
        return playerFacing;
    }

    /**
     * 获取指针射线起点
     */
    public Vector3f getPointerOrigin() {
        return camera.getLocation().clone();
    }

    /**
     * 获取指针射线方向
     */
    public Vector3f getPointerDirection() {
        return camera.getDirection().clone();
    }

    /**
     * 设置输入控制
     */
    private void setupInput() {
        inputManager.addMapping("MoveForward", new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping("MoveBackward", new KeyTrigger(KeyInput.KEY_S));
        inputManager.addMapping("StrafeLeft", new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping("StrafeRight", new KeyTrigger(KeyInput.KEY_D));
        inputManager.addMapping("RotateLeft", new KeyTrigger(KeyInput.KEY_Q));
        inputManager.addMapping("RotateRight", new KeyTrigger(KeyInput.KEY_E));
        inputManager.addMapping("Jump", new KeyTrigger(KeyInput.KEY_SPACE));

        // 状态切换按键（左Shift隐藏）
        inputManager.addMapping("ToggleHiding", new KeyTrigger(KeyInput.KEY_LSHIFT));

        // 【新增】Ctrl键切换到普通模式
        inputManager.addMapping("NormalMode", new KeyTrigger(KeyInput.KEY_LCONTROL));
        inputManager.addMapping("NormalModeAlt", new KeyTrigger(KeyInput.KEY_RCONTROL));

        inputManager.addMapping("MouseLook", new MouseAxisTrigger(MouseInput.AXIS_X, false));
        inputManager.addMapping("MouseLookNeg", new MouseAxisTrigger(MouseInput.AXIS_X, true));
        inputManager.addMapping("MouseLookUp", new MouseAxisTrigger(MouseInput.AXIS_Y, false));
        inputManager.addMapping("MouseLookDown", new MouseAxisTrigger(MouseInput.AXIS_Y, true));
        inputManager.addMapping("ZoomIn", new MouseAxisTrigger(MouseInput.AXIS_WHEEL, false));
        inputManager.addMapping("ZoomOut", new MouseAxisTrigger(MouseInput.AXIS_WHEEL, true));
        inputManager.addMapping("ResetCamera", new KeyTrigger(KeyInput.KEY_R));

        // 背包UI
        inputManager.addMapping("ToggleInventory", new KeyTrigger(KeyInput.KEY_G));

        // 竞技场系统
        inputManager.addMapping("EnterArena", new KeyTrigger(KeyInput.KEY_M));

        // 碰撞箱显示切换（F3键）
        inputManager.addMapping("ToggleCollisionBox", new KeyTrigger(KeyInput.KEY_F3));

        // 地形挖掘
        inputManager.addMapping("DigTerrain", new com.jme3.input.controls.MouseButtonTrigger(MouseInput.BUTTON_LEFT));

        // 武器攻击
        inputManager.addMapping("FireWeapon", new com.jme3.input.controls.MouseButtonTrigger(MouseInput.BUTTON_MIDDLE));

        inputManager.addListener(this,
                "MoveForward", "MoveBackward", "StrafeLeft", "StrafeRight",
                "RotateLeft", "RotateRight", "Jump", "MouseLook", "MouseLookNeg",
                "MouseLookUp", "MouseLookDown", "ZoomIn", "ZoomOut", "ResetCamera",
                "DigTerrain", "ToggleInventory", "NormalMode", "NormalModeAlt", "FireWeapon",
                "ToggleHiding", "ToggleCollisionBox", "EnterArena");
    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        // 控制台打开时忽略玩家输入
        if (gameConsole != null && gameConsole.isVisible()) {
            return;
        }

        // TextField有焦点时忽略玩家输入
        if (inventoryUI != null && inventoryUI.isTextFieldFocused()) {
            return;
        }

        // Buff选择界面弹出时锁定玩家操作（WASD/开火等），直到选完
        if (inputLocked) {
            return;
        }

        if (isDead) return;

        switch (name) {
            case "MoveForward":
                if (isPressed) {
                    // 检测双击（用于疾跑）
                    float currentTime = app.getTimer().getTimeInSeconds();
                    if (currentTime - lastKeyPressTime[0] < DOUBLE_TAP_THRESHOLD) {
                        onDoubleTapDetected();
                    }
                    lastKeyPressTime[0] = currentTime;
                }
                moveDirection[0] = isPressed;
                break;
            case "MoveBackward":
                if (isPressed) {
                    float currentTime = app.getTimer().getTimeInSeconds();
                    if (currentTime - lastKeyPressTime[2] < DOUBLE_TAP_THRESHOLD) {
                        onDoubleTapDetected();
                    }
                    lastKeyPressTime[2] = currentTime;
                }
                moveDirection[1] = isPressed;
                break;
            case "StrafeLeft":
                if (isPressed) {
                    float currentTime = app.getTimer().getTimeInSeconds();
                    if (currentTime - lastKeyPressTime[1] < DOUBLE_TAP_THRESHOLD) {
                        onDoubleTapDetected();
                    }
                    lastKeyPressTime[1] = currentTime;
                }
                moveDirection[2] = isPressed;
                break;
            case "StrafeRight":
                if (isPressed) {
                    float currentTime = app.getTimer().getTimeInSeconds();
                    if (currentTime - lastKeyPressTime[3] < DOUBLE_TAP_THRESHOLD) {
                        onDoubleTapDetected();
                    }
                    lastKeyPressTime[3] = currentTime;
                }
                moveDirection[3] = isPressed;
                break;
            case "RotateLeft":
                if (isPressed) {
                    float oldFacing = playerFacing;
                    // 按一次立刻转90度（逆时针）
                    playerFacing -= FastMath.HALF_PI;
                    // 标准化角度
                    while (playerFacing < 0) playerFacing += FastMath.TWO_PI;
                }
                break;
            case "RotateRight":
                if (isPressed) {
                    float oldFacing = playerFacing;
                    // 按一次立刻转90度（顺时针）
                    playerFacing += FastMath.HALF_PI;
                    // 标准化角度
                    while (playerFacing >= FastMath.TWO_PI) playerFacing -= FastMath.TWO_PI;
                }
                break;
            case "Jump":
                if (isPressed && isOnGround && !isJumping) {
                    velocity.y = JUMP_SPEED;
                    isJumping = true;
                    isOnGround = false;
                }
                break;
            case "ToggleHiding":
                // 记录Shift键状态
                isShiftPressed = isPressed;
                break;
            case "ResetCamera":
                if (isPressed) {
                    startCameraReset();
                }
                break;
            case "DigTerrain":
                // 根据持枪状态决定左键行为
                if (isPressed) {

                    isLeftButtonPressed = true;

                    if (isHoldingGun) {
                        // 持枪状态：开始攻击或蓄力

                        performGunAttack();
                    } else {
                        // 无枪状态：挖掘地形（原始行为）

                        performTerrainDig();
                    }
                } else {
                    // 按键松开
                    isLeftButtonPressed = false;

                    // 如果正在蓄力，释放蓄力攻击
                    if (isHoldingGun && currentWeapon != null && currentWeapon.isCharging()) {
                        releaseChargedAttack();
                    }
                }
                break;
            case "ToggleInventory":
                if (isPressed && inventoryUI != null) {
                    inventoryUI.toggle();
                }
                break;
            case "ToggleCollisionBox":
                if (isPressed && collisionBoxRenderer != null) {
                    collisionBoxRenderer.toggleVisibility();
                    System.out.println("碰撞箱显示: " + (collisionBoxRenderer.isVisible() ? "开启" : "关闭"));
                }
                break;
            case "EnterArena":
                if (isPressed && worldSwitcher != null) {
                    // 双向切换：主世界 <-> 竞技场
                    worldSwitcher.toggle();
                }
                break;
            case "NormalMode":
            case "NormalModeAlt":
                // 【新增】按住Ctrl切换到普通模式，松开回到圆盘模式
                if (puppetPlayerController != null) {
                    puppetPlayerController.setNormalMode(isPressed);
                }
                break;
            case "FireWeapon":
                if (isPressed) {
                    performWeaponFire();
                }
                break;
        }
    }

    @Override
    public void onAnalog(String name, float value, float tpf) {
        // 控制台打开时忽略鼠标输入
        if (gameConsole != null && gameConsole.isVisible()) {
            return;
        }

        // TextField有焦点时忽略鼠标输入
        if (inventoryUI != null && inventoryUI.isTextFieldFocused()) {
            return;
        }

        // 如果正在重置摄像机，忽略鼠标输入
        if (isResettingCamera) {
            return;
        }

        switch (name) {
            case "MouseLook":
                cameraAngleX += value * MOUSE_SENSITIVITY * 50f;
                break;
            case "MouseLookNeg":
                cameraAngleX -= value * MOUSE_SENSITIVITY * 50f;
                break;
            case "MouseLookUp":
                cameraAngleY += value * MOUSE_SENSITIVITY * 30f;
                cameraAngleY = FastMath.clamp(cameraAngleY, -80f, 60f);
                break;
            case "MouseLookDown":
                cameraAngleY -= value * MOUSE_SENSITIVITY * 30f;
                cameraAngleY = FastMath.clamp(cameraAngleY, -80f, 60f);
                break;
            case "ZoomIn":
                cameraDistance -= value * ZOOM_SPEED;
                cameraDistance = FastMath.clamp(cameraDistance, CAMERA_MIN_DISTANCE, CAMERA_MAX_DISTANCE);
                break;
            case "ZoomOut":
                cameraDistance += value * ZOOM_SPEED;
                cameraDistance = FastMath.clamp(cameraDistance, CAMERA_MIN_DISTANCE, CAMERA_MAX_DISTANCE);
                break;
        }
    }

    /**
     * 开始摄像机重置 - 重置到玩家当前朝向的正后方
     */
    private void startCameraReset() {
        // 立刻重置摄像机到玩家当前朝向的正后方
        // 玩家朝向 + 180度 = 玩家背后
        cameraBaseAngle = playerFacing + FastMath.PI;
        cameraAngleX = DEFAULT_CAMERA_ANGLE_X;  // 无偏移
        cameraAngleY = DEFAULT_CAMERA_ANGLE_Y;  // 水平视角

        // 标准化角度
        while (cameraBaseAngle >= FastMath.TWO_PI) cameraBaseAngle -= FastMath.TWO_PI;
        while (cameraBaseAngle < 0) cameraBaseAngle += FastMath.TWO_PI;

        // 保留距离的平滑重置
        isResettingCamera = true;
        resetProgress = 0f;
    }

    /**
     * 更新摄像机重置动画（仅重置距离，角度已瞬间重置）
     */
    private void updateCameraReset(float tpf) {
        if (!isResettingCamera) {
            return;
        }

        resetProgress += RESET_SPEED * tpf;

        if (resetProgress >= 1.0f) {
            resetProgress = 1.0f;
            isResettingCamera = false;
            LogUtils.debug(PlayerController.class, "摄像机距离重置完成");
        }

        // 只平滑插值距离，角度已经在startCameraReset中瞬间重置了
        cameraDistance = lerp(cameraDistance, DEFAULT_CAMERA_DISTANCE, resetProgress);
    }

    /**
     * 线性插值
     */
    private float lerp(float a, float b, float t) {
        return a + (b - a) * FastMath.clamp(t, 0f, 1f);
    }

    /**
     * 角度插值（处理360度环绕）
     */
    private float lerpAngle(float a, float b, float t) {
        float diff = b - a;
        while (diff > 180f) diff -= 360f;
        while (diff < -180f) diff += 360f;
        return a + diff * FastMath.clamp(t, 0f, 1f);
    }

    /**
     * 更新摄像机位置
     */
    private void updateCameraPosition() {
        // 更新摄像机重置进度
        updateCameraReset(app.getTimer().getTimePerFrame());

        // 使用独立的cameraBaseAngle，不受Q/E旋转影响
        float totalAngleX = cameraBaseAngle + (cameraAngleX * FastMath.DEG_TO_RAD);
        float angleY = cameraAngleY * FastMath.DEG_TO_RAD;

        float horizontalDistance = cameraDistance * FastMath.cos(angleY);
        float verticalDistance = cameraDistance * FastMath.sin(angleY);

        float camX = playerPosition.x + FastMath.sin(totalAngleX) * horizontalDistance;
        float camZ = playerPosition.z + FastMath.cos(totalAngleX) * horizontalDistance;
        float camY = playerPosition.y + CAMERA_HEIGHT + verticalDistance;

        Vector3f cameraPosition = new Vector3f(camX, camY, camZ);

        Vector3f shakeOffset = getShakeOffset();
        cameraPosition.addLocal(shakeOffset);

        camera.setLocation(cameraPosition);

        // 【修复】让相机看向玩家右侧的点，使人物出现在屏幕左侧
        // 计算相机的右方向向量
        Vector3f cameraRight = getCameraRightDirection();

        // lookAt目标点：玩家位置 + 向右偏移 + 向上偏移到头部
        Vector3f lookAtTarget = playerPosition.clone();
        lookAtTarget.y += CAMERA_LOOK_AT_OFFSET + 0.7f;  // 额外向上偏移0.5单位到头部位置
        lookAtTarget.addLocal(cameraRight.mult(0.525f));  // 向右偏移
        Vector3f direction = lookAtTarget.subtract(cameraPosition).normalizeLocal();
        camera.lookAtDirection(direction, Vector3f.UNIT_Y);
    }

    /**
     * 更新玩家碰撞盒
     */
    private void updatePlayerBox() {
        if (playerBox == null) {
            // 使用fromBottom方法，playerPosition是脚底位置
            playerBox = AABB.fromBottom(
                    playerPosition.clone(),
                    PLAYER_WIDTH, PLAYER_HEIGHT, PLAYER_WIDTH
            );
        } else {
            // 使用updateFromBottom方法更新位置
            playerBox.updateFromBottom(playerPosition.clone());
        }

        // 更新碰撞箱可视化
        if (collisionBoxRenderer != null && playerBox != null) {
            collisionBoxRenderer.updateCollisionBox(playerBox);
        }
    }

    /**
     * 将玩家传送到指定世界坐标
     * <p>统一处理传送时需要同步的所有状态：玩家位置、速度清零、相机、碰撞盒、Puppet位置。
     * 世界切换（worldNode/chunkManager 等）由调用方（如 {@link com.Hecate.arena.WorldSwitcher}）
     * 负责，此方法只处理"同一个世界坐标系内的瞬间移动"。
     *
     * @param targetPosition 目标世界坐标（脚底位置）
     */
    public void teleportTo(Vector3f targetPosition) {
        playerPosition.set(targetPosition);
        velocity.set(0, 0, 0);
        isJumping = false;
        isOnGround = false; // 交给下一帧的地形碰撞检测重新判定

        // 更新相机位置
        if (camera != null) {
            camera.setLocation(playerPosition.clone());
        }

        // 更新玩家碰撞盒
        updatePlayerBox();

        // 同步Puppet位置
        if (puppetPlayerController != null) {
            puppetPlayerController.setPosition(playerPosition.clone());
        }

        LogUtils.debug(PlayerController.class, "玩家已传送到: " + targetPosition);
    }

    /**
     * 主更新循环
     */
    public void update(float tpf) {
        frameCount++; // 递增帧计数器

        if (bloodDripOverlay == null) {
            app.enqueue(() -> {
                if (bloodDripOverlay == null) {
                    bloodDripOverlay = new BloodDripOverlay(app);
                    bloodDripOverlay.updateHealth(playerHealth.getHealthPercentage());
                }
                return null;
            });
            return;
        }

        if (bloodDripOverlay != null) {
            bloodDripOverlay.update(tpf);
        }

        // 更新背包UI（用于TextField光标闪烁等动画效果）
        if (inventoryUI != null) {
            inventoryUI.update(tpf);
        }

        if (isDead) {
            updateDeathAnimation(tpf);

            if (deadPlayerNode != null) {
                updateCorpseOrientation();
            }

            deathTimer += tpf;
            if (deathTimer >= AUTO_REVIVE_TIME) {
                app.enqueue(() -> {
                    if (playerHealth != null && playerHealth.isDead()) {
                        playerHealth.revive();
                    }
                    return null;
                });
                return;
            }

            updateScreenShake(tpf);
            updateCameraPosition();
            return;
        }

        // Buff选择界面弹出期间：锁定移动/武器/恢复等逐帧逻辑，只保留相机/振动，
        // 避免画面完全静止显得卡死，但玩家无法借这段时间移动或攻击。
        if (inputLocked) {
            updateScreenShake(tpf);
            updateCameraPosition();
            return;
        }

        if (playerHealth != null) {
            playerHealth.update(tpf);
        }

        // 更新恢复管理器
        if (recoveryManager != null) {
            recoveryManager.update(tpf, playerPosition);
        }

        // 更新武器系统
        if (currentWeapon != null) {
            currentWeapon.update(tpf);
        }

        // 【地面类型检测】检测玩家脚下的墨水类型
        updateGroundType();

        // 【三选二状态管理】在友方墨水上根据按键决定激活哪两种状态
        // 三种状态：恢复、隐藏、加速
        // 按键组合：
        // - 无按键：无效果
        // - 仅右键：恢复 + 加速
        // - 仅Shift：隐藏 + 加速
        // - Shift + 右键：隐藏 + 恢复（无加速，无法移动）
        boolean hasRecovery = false;
        boolean hasHiding = false;
        boolean hasSpeed = false;

        if (currentGroundType == GroundType.FRIENDLY) {
            boolean bothPressed = isShiftPressed && isRightButtonPressed;

            if (bothPressed) {
                // 同时按下：隐藏 + 恢复
                hasHiding = true;
                hasRecovery = true;
                hasSpeed = false;
            } else if (isRightButtonPressed) {
                // 仅右键：恢复 + 加速
                hasRecovery = true;
                hasSpeed = true;
                hasHiding = false;
            } else if (isShiftPressed) {
                // 仅Shift：隐藏 + 加速
                hasHiding = true;
                hasSpeed = true;
                hasRecovery = false;
            } else {
                // 无按键：无特殊效果
                hasSpeed = false;
                hasRecovery = false;
                hasHiding = false;
            }
        } else {
            // 非友方墨水：无特殊状态
            hasRecovery = false;
            hasHiding = false;
            hasSpeed = false;
        }

        // 持枪状态下的连发机制
        if (isHoldingGun && isLeftButtonPressed && currentWeapon != null && playerAmmo != null) {
            // 只对不支持蓄力的武器（连发武器）生效
            if (!currentWeapon.getStats().hasCharge()) {
                continuousFireTimer += tpf;

                // 根据武器射速持续开火
                float fireRate = currentWeapon.getStats().getFireRate();
                if (continuousFireTimer >= fireRate) {
                    continuousFireTimer = 0f;

                    // 尝试开火
                    Vector3f fireOrigin = playerPosition.clone();
                    fireOrigin.y += 1.0f;
                    Vector3f fireDirection = camera.getDirection().clone();

                    boolean fired = currentWeapon.tryFire(playerAmmo, fireOrigin, fireDirection);
                    if (fired) {
                        LogUtils.debug(PlayerController.class, "连发攻击");
                    }
                }
            }
        } else {
            // 重置连发计时器
            continuousFireTimer = 0f;
        }

        // 应用恢复状态（三选二规则）
        if (hasRecovery && currentGroundType == GroundType.FRIENDLY) {
            // 恢复血量
            if (playerHealth != null && !playerHealth.isFullHealth()) {
                playerHealth.recoverByPercentage(recoveryPercentage, tpf);
            }
            // 恢复弹药
            if (playerAmmo != null && !playerAmmo.isFull()) {
                playerAmmo.recoverByPercentage(recoveryPercentage, tpf);
            }
        }

        // 旧的状态恢复系统（已被新系统替代，保留以防其他地方使用）
        if (isRecovering && currentGroundType == GroundType.FRIENDLY) {
            // 恢复血量
            if (playerHealth != null && !playerHealth.isFullHealth()) {
                playerHealth.recoverByPercentage(recoveryPercentage, tpf);
            }
            // 恢复弹药
            if (playerAmmo != null && !playerAmmo.isFull()) {
                playerAmmo.recoverByPercentage(recoveryPercentage, tpf);
            }
        }

        // 更新疾跑计时器
        if (isSprinting) {
            sprintTimer += tpf;
            if (sprintTimer >= SPRINT_DURATION) {
                isSprinting = false;
                sprintTimer = 0f;

            }
        }

        // Q/E键旋转已改为瞬间旋转，在onAction中处理，此处不再需要

        // 【移动控制】Shift + 右键时禁止移动
        boolean isBothPressed = isShiftPressed && isRightButtonPressed && currentGroundType == GroundType.FRIENDLY;
        if (isBothPressed) {
            // 隐藏 + 恢复模式：无法移动
            isMoving = false;
        } else {
            // 基于摄像机方向计算移动向量
            Vector3f forward = getCameraFacingDirection();
            Vector3f right = getCameraRightDirection();
            Vector3f movement = new Vector3f();

            if (moveDirection[0]) {
                movement.addLocal(forward);
            }
            if (moveDirection[1]) {
                movement.addLocal(forward.negate());
            }
            if (moveDirection[2]) {
                movement.addLocal(right.negate());
            }
            if (moveDirection[3]) {
                movement.addLocal(right);
            }

            isMoving = movement.lengthSquared() > 0;

            // 【隐藏状态】如果正在移动，自动脱离隐藏状态
            if (isMoving && (isHidingOnInk || isHidingOnEmpty)) {
                if (isHidingOnInk) {
                    isHidingOnInk = false;
                    stateHistory.remove("hidingOnInk");

                }
                if (isHidingOnEmpty) {
                    isHidingOnEmpty = false;
                    stateHistory.remove("hidingOnEmpty");

                }
                updateFastMovingState();
            }

            if (isMoving) {
                movement.normalizeLocal();

                // 【墨水系统】应用基于地面状态的速度倍率
                // 规则：
                // - 敌方减速：始终生效
                // - 友方加速：根据hasSpeed状态（三选二规则）
                float inkSpeedMultiplier = 1.0f;
                if (usePuppetMode && puppetPlayerController != null) {
                    float rawMultiplier = puppetPlayerController.getSpeedMultiplier();

                    if (rawMultiplier < 1.0f) {
                        // 敌方减速：始终生效
                        inkSpeedMultiplier = rawMultiplier;
                    } else if (rawMultiplier > 1.0f && hasSpeed) {
                        // 友方加速：根据三选二规则的hasSpeed状态
                        inkSpeedMultiplier = rawMultiplier;
                    }
                    // else: 保持1.0f（普通地面或无加速状态）
                }

                // 根据快速移动状态选择速度
                float baseSpeed = isFastMoving ? FAST_MOVE_SPEED : WALK_SPEED;

                // 应用疾跑速度倍率
                float sprintMultiplier = isSprinting ? SPRINT_SPEED_MULTIPLIER : 1.0f;

                movement.multLocal(baseSpeed * speedMultiplier * inkSpeedMultiplier * sprintMultiplier * tpf);

                // 暂时禁用体素碰撞检测，使用地形碰撞系统
                // TODO: 未来需要整合体素碰撞和地形碰撞
                playerPosition.addLocal(movement);

                lastMovementDirection.set(movement.clone().normalizeLocal());
            }
        }

        velocity.y += GRAVITY * tpf;

        // 应用重力（先更新Y坐标）
        playerPosition.y += velocity.y * tpf;

        updatePlayerBox();

        // 地形表面碰撞检测（替代硬编码的GROUND_LEVEL）
        if (collisionManager != null) {
            float terrainHeight = collisionManager.getTerrainHeightAt(playerPosition.x, playerPosition.z);

            // 如果查询到有效的地形高度
            if (!Float.isNaN(terrainHeight)) {
                // 玩家脚底应该在terrainHeight位置
                if (playerPosition.y < terrainHeight) {
                    // 将玩家放在地形表面上
                    playerPosition.y = terrainHeight;

                    // 如果玩家之前在下落，现在停止下落
                    if (velocity.y < 0) {
                        velocity.y = 0;
                        isJumping = false;
                        isOnGround = true;
                    }
                } else if (playerPosition.y > terrainHeight + 0.1f) {
                    // 玩家在空中
                    isOnGround = false;
                }
            } else {
                // 查不到地形数据：初始状态下（游戏刚启动，世界尚未生成完毕）用
                // GROUND_LEVEL兜底防止掉出初始地面；一旦玩家已经真正立足过地面
                // （hasLandedOnTerrain=true），说明"查不到地形"就是真的在虚空里，
                // 应该持续下落而不是被隐形地板接住——否则玩家能"走在虚空上"。
                if (!hasLandedOnTerrain && playerPosition.y <= GROUND_LEVEL && velocity.y <= 0) {
                    playerPosition.y = GROUND_LEVEL;
                    velocity.y = 0;
                    isJumping = false;
                    isOnGround = true;
                } else {
                    isOnGround = false;
                }
            }
        } else {
            // 没有collisionManager（系统尚未接入），使用备用的GROUND_LEVEL
            if (playerPosition.y <= GROUND_LEVEL && velocity.y <= 0) {
                playerPosition.y = GROUND_LEVEL;
                velocity.y = 0;
                isJumping = false;
                isOnGround = true;
            } else {
                isOnGround = false;
            }
        }

        if (isOnGround) {
            hasLandedOnTerrain = true;
        }

        // 虚空死亡检测：掉出任何已知地形范围之外，判定坠落致死
        if (!isDead && playerPosition.y < VOID_DEATH_Y) {
            if (playerHealth != null) {
                playerHealth.kill();
            }
        }

        if (useSpriteMode && spriteAnimationSystem != null && spriteRenderer != null) {
            updateSpriteSystem(tpf);
        }

        // 更新 Puppet 系统
        if (usePuppetMode && puppetPlayerController != null) {
            updatePuppetSystem(tpf);
        }

        updateScreenShake(tpf);
        updateCameraPosition();

        // 更新Gun1武器位置（如果已装备）
        if (isGun1Equipped && gun1WeaponNode != null) {
            updateGun1WeaponPosition();
        }

    }

    /**
     * 更新Gun1武器位置
     * 使武器跟随玩家，出现在玩家面前
     */
    private void updateGun1WeaponPosition() {
        // 获取摄像机朝向（玩家面向的方向）
        Vector3f forward = getCameraFacingDirection();
        Vector3f right = getCameraRightDirection();

        // 计算武器位置：玩家位置 + 前方偏移 + 右侧偏移 + 高度偏移
        Vector3f weaponPos = playerPosition.clone();

        // 使用水平方向的前向向量（减少垂直移动）
        Vector3f horizontalForward = forward.clone();
        horizontalForward.y = 0;  // 移除垂直分量
        horizontalForward.normalizeLocal();

        // 前方距离（武器在玩家前方0.8个单位，只在水平方向）
        weaponPos.addLocal(horizontalForward.mult(0.8f));

        // 右侧偏移（武器在玩家右侧0.3个单位，模拟右手持枪）
        weaponPos.addLocal(right.mult(0.3f));

        // 固定高度偏移（武器在玩家视线高度，稍微下方）
        // 添加微小的垂直跟随（减少到原来的20%）
        weaponPos.y += 0.6f + (forward.y * 0f);

        gun1WeaponNode.setLocalTranslation(weaponPos);

        // 设置武器旋转，使其朝向与玩家相同
        // 使用水平朝向计算旋转
        Quaternion rotation = new Quaternion();
        rotation.lookAt(horizontalForward, Vector3f.UNIT_Y);
        gun1WeaponNode.setLocalRotation(rotation);
    }

    /**
     * 更新2D精灵系统
     */
    private void updateSpriteSystem(float tpf) {
        Vector3f cameraDir = camera.getDirection().clone();
        boolean isTopView = cameraAngleY > TOP_VIEW_ANGLE_THRESHOLD;

        // 定期显示当前相机角度（已禁用，减少日志输出）
        // debugTimer += tpf;
        // if (debugTimer >= 2.0f) {
        //     String status;
        //     if (isTopView) {
        //         status = "✓ 俯视模式已激活";
        //     } else if (cameraAngleY > TOP_VIEW_ANGLE_THRESHOLD - 5f) {
        //         status = "接近阈值，继续向上移动鼠标";
        //     } else if (cameraAngleY > 0) {
        //         status = "相机朝上，需要角度 > " + TOP_VIEW_ANGLE_THRESHOLD + "°";
        //     } else {
        //         status = "相机朝下，需要向上移动鼠标";
        //     }
        //     LogUtils.info(PlayerController.class, String.format("相机角度: %.1f° (阈值: %.1f°) - %s",
        //         cameraAngleY, TOP_VIEW_ANGLE_THRESHOLD, status));
        //     debugTimer = 0f;
        // }

        // 模式切换（静默处理）
        if (isTopView != wasTopView) {
            wasTopView = isTopView;
        }

        String direction;
        if (isTopView) {
            direction = "top";
        } else {
            // 计算摄像机相对于玩家朝向的角度
            cameraDir.y = 0;
            cameraDir.normalizeLocal();

            // 摄像机的世界角度
            float cameraWorldAngle = FastMath.atan2(cameraDir.x, cameraDir.z);

            // 玩家朝向角度（Q/E控制）
            float playerWorldAngle = playerFacing;

            // 计算相对角度（摄像机相对于玩家的角度）
            float relativeAngle = cameraWorldAngle - playerWorldAngle;

            // 标准化到0-360度
            float relativeDegrees = relativeAngle * FastMath.RAD_TO_DEG;
            while (relativeDegrees < 0) relativeDegrees += 360;
            while (relativeDegrees >= 360) relativeDegrees -= 360;

            // 根据相对角度决定精灵显示方向
            // 0° = 摄像机在玩家正前方（背面） → back
            // 180° = 摄像机在玩家正后方（正面） → front
            // 90° = 摄像机在玩家右侧（左侧面） → left
            // 270° = 摄像机在玩家左侧（右侧面） → right
            if (relativeDegrees >= 315 || relativeDegrees < 45) {
                direction = "back";  // 玩家背对摄像机
            } else if (relativeDegrees >= 45 && relativeDegrees < 135) {
                direction = "left";  // 玩家左侧面向摄像机
            } else if (relativeDegrees >= 135 && relativeDegrees < 225) {
                direction = "front"; // 玩家面对摄像机
            } else {
                direction = "right"; // 玩家右侧面向摄像机
            }
        }

        // 保存当前精灵方向（用于火球发射等功能）
        currentSpriteDirection = direction;

        String action;
        if (isTopView) {
            action = isMoving ? "run" : "idle";
        } else {
            if (!isOnGround && velocity.y > 0) {
                action = "jump";
            } else if (isMoving) {
                action = "run";
            } else {
                action = "idle";
            }
        }

        String animationName = direction + "_" + action;

        // 移除了重复的动画日志，在实际播放时已经有日志了

        if (!spriteAnimationSystem.hasAnimation(animationName)) {
            LogUtils.warning(PlayerController.class, "动画不存在: " + animationName);
            if (isTopView && action.equals("jump")) {
                animationName = "top_idle";
                LogUtils.debug(PlayerController.class, "使用备用动画: " + animationName);
            }
        }

        if (spriteAnimationSystem.hasAnimation(animationName)) {
            if (!animationName.equals(spriteAnimationSystem.getCurrentAnimationName())) {
                spriteAnimationSystem.playAnimation(animationName);
                LogUtils.debug(PlayerController.class, "播放动画: " + animationName);
            }
        } else {
            LogUtils.warning(PlayerController.class, "最终动画也不存在: " + animationName);
        }

        AnimationState spriteState = AnimationState.IDLE;
        if (isMoving) spriteState = AnimationState.WALKING;
        if (isJumping) spriteState = AnimationState.JUMPING;

        spriteAnimationSystem.update(tpf, spriteState);

        AnimationFrame currentFrame = spriteAnimationSystem.getCurrentFrame();
        if (currentFrame != null) {
            spriteRenderer.setCurrentFrame(currentFrame);

            Vector3f spritePos = playerPosition.clone();
            spritePos.y += SPRITE_HEIGHT_OFFSET;

            // 在俯视模式下，精灵应该平放并面向摄像机
            if (isTopView) {
                spriteRenderer.setTopViewMode(true, camera);
            } else {
                spriteRenderer.setTopViewMode(false, null);
            }

            Vector3f spriteFacing = Vector3f.UNIT_Z;
            spriteRenderer.update(tpf, spritePos, spriteFacing);
        }
    }

    /**
     * 更新 Puppet 动画系统
     */
    private void updatePuppetSystem(float tpf) {
        // 更新 puppet 位置
        puppetPlayerController.setPosition(playerPosition);

        // 更新 puppet 旋转（根据玩家朝向）
        float oldPuppetRotation = puppetPlayerController.getRotation();
        puppetPlayerController.setRotation(playerFacing);

        // 每60帧输出一次旋转同步信息
        // (已禁用日志输出)

        // 根据移动状态播放动画
        // 使用 isMoving 标志而不是 velocity，因为 velocity 可能在碰撞后被重置
        puppetPlayerController.setWalking(isMoving);

        // Debug: 每60帧输出一次状态
        frameCount++;
        // (已禁用日志输出)

        // 跳跃动画
        if (isJumping && !wasJumping) {
            puppetPlayerController.jump();
        }
        wasJumping = isJumping;

        // 更新 puppet 系统
        puppetPlayerController.update(tpf);
    }

    private boolean wasJumping = false;

    /**
     * 更新地面类型检测
     * 检测玩家脚下的墨水类型，更新 currentGroundType
     */
    private void updateGroundType() {
        if (gridManager == null) {

            return;
        }

        // 查询玩家脚下的网格单元
        com.Hecate.ink.GridCell cell = gridManager.getCellAt(playerPosition);

        if (cell == null || cell.isEmpty()) {
            currentGroundType = GroundType.NONE;
            return;
        }

        // 获取玩家阵营ID
        int playerFactionId = getPlayerFactionId();

        // 根据网格状态和玩家阵营判断地面类型
        GroundType oldGroundType = currentGroundType;

        if (cell.isEmpty()) {
            // 空地
            currentGroundType = GroundType.NONE;
        } else {
            // 判断阵营关系
            com.Hecate.ink.FactionRegistry registry = gridManager.getFactionRegistry();
            com.Hecate.ink.Relation relation = registry.getRelation(cell.getFactionId(), playerFactionId);

            if (relation == com.Hecate.ink.Relation.SELF) {
                // 己方领地
                currentGroundType = cell.isIgnited() ? GroundType.IGNITED : GroundType.FRIENDLY;
            } else {
                // 敌方或中立领地
                currentGroundType = GroundType.NONE;
            }
        }

        // 地面类型改变时输出日志

    }

    /**
     * 初始化2D精灵系统（不自动启用）
     */
    private void initializeSpriteSystem() {
        spriteScaleManager = SpriteScaleManager.getInstance();

        spriteNode = new Node("PlayerSprite");
        spriteNode.setCullHint(Node.CullHint.Always);  // 默认隐藏
        app.getRootNode().attachChild(spriteNode);

        spriteManager = new PlayerSpriteManager(app);
        spriteManager.loadStandardPlayerAnimations();

        spriteAnimationSystem = new SpriteAnimationSystem();
        spriteRenderer = new DirectionalSpriteRenderer(app, spriteNode);

        setupSpriteAnimations();

        // 不再自动启用精灵模式，由 enableSpriteMode() 方法控制
    }

    /**
     * 设置精灵动画
     */
    private void setupSpriteAnimations() {
        int totalAnimations = 0;
        for (String animName : spriteManager.getLoadedAnimationNames()) {
            List<AnimationFrame> frames = spriteManager.getAnimationSequence(animName);
            if (frames != null && !frames.isEmpty()) {
                spriteAnimationSystem.addAnimationFromFrames(animName, frames, true);

                totalAnimations++;
            }
        }

        // 检查top动画是否存在
        if (spriteAnimationSystem.hasAnimation("top_idle")) {
        } else {
        }

        if (spriteAnimationSystem.hasAnimation("top_run")) {
        } else {
        }

        if (spriteAnimationSystem.hasAnimation("front_idle")) {
            spriteAnimationSystem.playAnimation("front_idle");
        }
    }

    public Vector3f getPlayerPosition() {
        return playerPosition.clone();
    }

    public AABB getPlayerBox() {
        return playerBox;
    }

    public boolean isJumping() {
        return isJumping;
    }

    public boolean isFastMoving() {
        return isFastMoving;
    }

    public boolean isOnGround() {
        return isOnGround;
    }

    public float getCurrentMoveSpeed() {
        return isFastMoving ? FAST_MOVE_SPEED : WALK_SPEED;
    }

    public float getCameraDistance() {
        return cameraDistance;
    }

    public boolean isResettingCamera() {
        return isResettingCamera;
    }

    public PlayerHealth getPlayerHealth() {
        return playerHealth;
    }

    public float getCurrentHealth() {
        return playerHealth.getCurrentHealth();
    }

    public float getMaxHealth() {
        return playerHealth.getMaxHealth();
    }

    public boolean isPlayerAlive() {
        return playerHealth.isAlive();
    }

    public float getHealthPercentage() {
        return playerHealth.getHealthPercentage();
    }

    public boolean isLowHealth() {
        return playerHealth.isLowHealth();
    }

    public boolean isDead() {
        return isDead;
    }

    public Node getDeadPlayerNode() {
        return deadPlayerNode;
    }

    public boolean isShaking() {
        return isShaking;
    }

    public Vector3f getSpawnPosition() {
        return spawnPosition.clone();
    }

    public void setSpawnPosition(Vector3f newSpawnPosition) {
        this.spawnPosition.set(newSpawnPosition);
        LogUtils.debug(PlayerController.class, "出生位置已更新: " + spawnPosition);
    }

    /**
     * 获取速度倍数
     */
    public float getSpeedMultiplier() {
        return speedMultiplier;
    }

    /**
     * 设置速度倍数
     */
    public void setSpeedMultiplier(float multiplier) {
        this.speedMultiplier = Math.max(0.1f, multiplier); // 最小0.1倍，防止设置为0或负数
    }

    /**
     * 获取当前精灵显示方向
     * @return "back", "front", "left", "right" 或 "top"
     */
    public String getCurrentSpriteDirection() {
        return currentSpriteDirection;
    }

    /**
     * 设置世界节点（用于阴影射线检测）
     */
    public void setWorldNode(Node worldNode) {
        this.currentWorldNode = worldNode;

        if (puppetPlayerController != null) {
            puppetPlayerController.setWorldNode(worldNode);
        }

        // 同步当前武器的瞄准射线检测节点（如FlameWeapon），避免世界切换后
        // 瞄准射线仍打在已从场景图摘除的旧世界节点上
        if (currentWeapon instanceof com.Hecate.weapon.FlameWeapon) {
            ((com.Hecate.weapon.FlameWeapon) currentWeapon).setWorldNode(worldNode);
        }
    }

    /**
     * 设置怪物管理器（用于/mob1命令生成怪物）
     */
    public void setMonsterManager(com.Hecate.monster.MonsterManager monsterManager) {
        this.monsterManager = monsterManager;
    }

    /**
     * "攻击弹道+1"buff的当前叠加数量（FlameWeapon开火时读取）
     */
    public int getExtraProjectiles() {
        return extraProjectiles;
    }

    /**
     * 弹出Buff三选一界面，锁定玩家操作直到选完。
     * @param options 恰好3个候选buff
     * @param onComplete 玩家选完并应用效果之后的回调（例如波次系统据此生成下一波怪物）
     */
    public void showBuffSelection(java.util.List<BuffType> options, Runnable onComplete) {
        if (buffSelectUI == null) {
            // 界面未就绪，直接跳过选择、立即执行后续动作，避免流程卡死
            if (onComplete != null) {
                onComplete.run();
            }
            return;
        }

        inputLocked = true;
        buffSelectUI.show(options, (BuffType chosen) -> {
            onBuffSelected(chosen);
            inputLocked = false;
            if (onComplete != null) {
                onComplete.run();
            }
        });
    }

    /**
     * 应用玩家选中的buff效果
     */
    private void onBuffSelected(BuffType type) {
        switch (type) {
            case FIRE_RATE_UP:
                if (currentWeapon != null) {
                    currentWeapon.getStats().multiplyFireRate(1f / 1.5f); // 间隔缩短=打得更快
                }
                break;
            case EXTRA_PROJECTILE:
                extraProjectiles++;
                break;
            case SPREAD_RANGE_UP:
                if (currentWeapon != null) {
                    currentWeapon.getStats().multiplySpreadAngle(1.5f);
                }
                break;
            case RECOVERY_SPEED_UP:
                recoveryPercentage *= 1.5f;
                break;
            case MOVE_SPEED_UP:
                setSpeedMultiplier(getSpeedMultiplier() * 1.05f);
                break;
        }

        LogUtils.debug(PlayerController.class, "已应用Buff: " + type.displayName);
    }

    /**
     * 设置网格管理器（用于墨水系统速度倍率和地面类型检测）
     */
    public void setGridManager(com.Hecate.ink.SparseGridManager gridManager) {
        this.gridManager = gridManager;
        if (puppetPlayerController != null) {
            puppetPlayerController.setGridManager(gridManager);
        }
        if (recoveryManager != null) {
            recoveryManager.setGridManager(gridManager);
        }
    }

    /**
     * 设置玩家队伍（用于墨水系统速度倍率）
     * @param team 队伍编号（0=A队，1=B队）
     */
    public void setPlayerTeam(int team) {
        if (puppetPlayerController != null) {
            puppetPlayerController.setPlayerTeam(team);
        }
        if (recoveryManager != null) {
            recoveryManager.setPlayerTeam(team);
        }
    }

    /**
     * 获取玩家队伍
     */
    public int getPlayerTeam() {
        if (puppetPlayerController != null) {
            return puppetPlayerController.getPlayerTeam();
        }
        return 1; // 默认B队（暗属性）
    }

    /**
     * 获取 PuppetPlayerController（用于外部访问）
     */
    public PuppetPlayerController getPuppetPlayerController() {
        return puppetPlayerController;
    }

    /**
     * 获取弹药系统
     */
    public PlayerAmmo getPlayerAmmo() {
        return playerAmmo;
    }

    /**
     * 获取当前武器
     */
    public Weapon getCurrentWeapon() {
        return currentWeapon;
    }

    /**
     * 设置当前武器
     */
    public void setCurrentWeapon(Weapon weapon) {
        this.currentWeapon = weapon;

    }

    /**
     * 获取玩家阵营ID
     */
    public int getPlayerFactionId() {
        return playerFactionId;
    }

    /**
     * 设置玩家阵营ID
     */
    public void setPlayerFactionId(int factionId) {
        this.playerFactionId = factionId;

    }

    /**
     * 执行武器开火
     */
    private void performWeaponFire() {
        // 攻击时清除所有状态
        clearAllStates();

        // 检查游戏状态是否允许攻击（现在总是允许，因为攻击会清除状态）
        // if (!gameState.canAttack()) {
        //     LogUtils.debug(PlayerController.class, "当前状态不允许攻击: " + gameState.getDescription());
        //     return;
        // }

        // 尝试开火
        Vector3f fireOrigin = playerPosition.clone();
        fireOrigin.y += 1.0f; // 从玩家胸部位置发射

        Vector3f fireDirection = camera.getDirection().clone();

        // 调用武器的tryFire方法
        if (currentWeapon != null && playerAmmo != null) {
            boolean fired = currentWeapon.tryFire(playerAmmo, fireOrigin, fireDirection);
            if (fired) {

            } else {

            }
        }
    }

    /**
     * 持枪状态下的攻击处理
     * 根据武器类型决定是连发还是蓄力
     */
    private void performGunAttack() {
        if (currentWeapon == null || playerAmmo == null) {
            return;
        }

        // 清除所有状态（与原始攻击行为一致）
        clearAllStates();

        // 检查武器是否支持蓄力
        if (currentWeapon.getStats().hasCharge()) {
            // 支持蓄力：开始蓄力
            currentWeapon.startCharge();
        } else {
            // 不支持蓄力：立即开火（连发武器）
            Vector3f fireOrigin = playerPosition.clone();
            fireOrigin.y += 1.0f; // 从玩家胸部位置发射

            Vector3f fireDirection = camera.getDirection().clone();

            boolean fired = currentWeapon.tryFire(playerAmmo, fireOrigin, fireDirection);
            if (fired) {
                // 重置连发计时器，从首次开火开始计算连发间隔
                continuousFireTimer = 0f;
            }
        }
    }

    /**
     * 释放蓄力攻击
     */
    private void releaseChargedAttack() {
        if (currentWeapon == null || playerAmmo == null) {
            return;
        }

        Vector3f fireOrigin = playerPosition.clone();
        fireOrigin.y += 1.0f;

        Vector3f fireDirection = camera.getDirection().clone();

        // 释放蓄力攻击
        currentWeapon.releaseCharge(playerAmmo, fireOrigin, fireDirection);
    }

    /**
     * 双击检测回调（用于疾跑）
     */
    private void onDoubleTapDetected() {
        // 启动疾跑状态
        isSprinting = true;
        sprintTimer = 0f;

    }

    /**
     * 切换状态
     * @param stateName "recovery", "hidingOnInk", 或 "hidingOnEmpty"
     */
    private void toggleState(String stateName) {
        boolean currentState;
        if (stateName.equals("recovery")) {
            currentState = isRecovering;
        } else if (stateName.equals("hidingOnInk")) {
            currentState = isHidingOnInk;
        } else if (stateName.equals("hidingOnEmpty")) {
            currentState = isHidingOnEmpty;
        } else {
            return;
        }

        // 如果当前状态已激活，则关闭它
        if (currentState) {
            if (stateName.equals("recovery")) {
                isRecovering = false;
                stateHistory.remove("recovery");
            } else if (stateName.equals("hidingOnInk")) {
                isHidingOnInk = false;
                stateHistory.remove("hidingOnInk");
            } else if (stateName.equals("hidingOnEmpty")) {
                isHidingOnEmpty = false;
                stateHistory.remove("hidingOnEmpty");
            }
            LogUtils.debug(PlayerController.class, "关闭状态: " + stateName);
            updateFastMovingState();
            return;
        }

        // 计算当前激活的状态数量
        int activeStates = 0;
        if (isRecovering) activeStates++;
        if (isHidingOnInk) activeStates++;
        if (isHidingOnEmpty) activeStates++;

        // 根据地面类型确定最大状态数
        int maxStates;
        switch (currentGroundType) {
            case NONE:
                maxStates = 1;
                break;
            case FRIENDLY:
                maxStates = 2;
                break;
            case IGNITED:
                maxStates = 3;
                break;
            default:
                maxStates = 1;
        }

        // 如果已达到最大状态数，移除最早的状态
        if (activeStates >= maxStates) {
            String oldestState = stateHistory.poll();
            if (oldestState != null) {
                if (oldestState.equals("recovery")) {
                    isRecovering = false;
                } else if (oldestState.equals("hidingOnInk")) {
                    isHidingOnInk = false;
                } else if (oldestState.equals("hidingOnEmpty")) {
                    isHidingOnEmpty = false;
                }
                LogUtils.debug(PlayerController.class, "移除最早状态: " + oldestState);
            }
        }

        // 激活新状态
        if (stateName.equals("recovery")) {
            isRecovering = true;
        } else if (stateName.equals("hidingOnInk")) {
            isHidingOnInk = true;
        } else if (stateName.equals("hidingOnEmpty")) {
            isHidingOnEmpty = true;
        }
        stateHistory.add(stateName);
        LogUtils.debug(PlayerController.class, "激活状态: " + stateName);

        // 更新快速移动状态
        updateFastMovingState();
    }

    /**
     * 更新快速移动状态（基于地面类型和其他状态）
     */
    private void updateFastMovingState() {
        boolean oldFastMoving = isFastMoving;

        switch (currentGroundType) {
            case NONE:
                // 无墨地面：只能通过双击触发，这里不改变
                LogUtils.debug(PlayerController.class, "[快速移动] 无墨地面，保持当前状态: " + isFastMoving);
                break;
            case FRIENDLY:
                // 友方墨水：一个状态激活=快速移动，两个状态激活=无快速移动
                int activeStates = 0;
                if (isRecovering) activeStates++;
                if (isHidingOnInk) activeStates++;
                if (isHidingOnEmpty) activeStates++;
                isFastMoving = (activeStates == 1);

                break;
            case IGNITED:
                // 点燃墨水：总是快速移动
                isFastMoving = true;
                break;
        }

    }

    /**
     * 清除所有状态（攻击时调用）
     */
    private void clearAllStates() {
        if (isRecovering || isHidingOnInk || isHidingOnEmpty || isFastMoving) {
            isRecovering = false;
            isHidingOnInk = false;
            isHidingOnEmpty = false;
            isFastMoving = false;
            stateHistory.clear();
            LogUtils.debug(PlayerController.class, "攻击：清除所有状态");
        }
    }

    /**
     * 获取恢复管理器
     */
    public PlayerRecoveryManager getRecoveryManager() {
        return recoveryManager;
    }

    /**
     * 设置右键用于恢复（由PlayerControlModule调用）
     */
    public void setRightButtonForRecovery(boolean pressed) {
        isRightButtonPressed = pressed;
        if (pressed) {

        } else {

        }
        if (recoveryManager != null) {
            recoveryManager.setLeftButtonPressed(pressed);
        }
    }

    /**
     * 执行地形挖掘 - 鼠标左键点击挖掘
     */
    private void performTerrainDig() {
        // 攻击时清除所有状态
        clearAllStates();

        if (collisionManager == null) {
            return;
        }

        // 发射射线检测地形
        com.jme3.math.Ray ray = new com.jme3.math.Ray(camera.getLocation(), camera.getDirection());

        // 使用改进的射线检测算法
        // 最大检测距离20个单位
        float maxDistance = 20.0f;
        Vector3f hitPoint = null;
        float hitDistance = Float.MAX_VALUE;

        // 更精细的步进检测（0.1步长）
        for (float distance = 0.1f; distance < maxDistance; distance += 0.1f) {
            Vector3f testPoint = ray.getOrigin().add(ray.getDirection().mult(distance));
            float terrainHeight = collisionManager.getTerrainHeightAt(testPoint.x, testPoint.z);

            // 检查是否击中地形（允许一定误差范围）
            if (!Float.isNaN(terrainHeight)) {
                // 如果测试点在地形下方或非常接近地形表面
                if (testPoint.y <= terrainHeight + 0.2f && testPoint.y >= terrainHeight - 0.5f) {
                    if (distance < hitDistance) {
                        hitPoint = new Vector3f(testPoint.x, terrainHeight, testPoint.z);
                        hitDistance = distance;
                        break;
                    }
                }
            }
        }

        if (hitPoint == null) {
            LogUtils.debug(PlayerController.class, "未检测到地形命中点");
            return;
        }

        // 计算chunk坐标
        int chunkX = (int) Math.floor(hitPoint.x / com.Hecate.world.Chunk.SIZE);
        int chunkZ = (int) Math.floor(hitPoint.z / com.Hecate.world.Chunk.SIZE);
        com.Hecate.world.ChunkPosition chunkPos = new com.Hecate.world.ChunkPosition(chunkX, 0, chunkZ);

        // 获取chunk
        com.Hecate.world.ChunkManager chunkManager = collisionManager.getChunkManager();
        if (chunkManager == null) {
            LogUtils.debug(PlayerController.class, "ChunkManager为null");
            return;
        }

        com.Hecate.world.Chunk chunk = chunkManager.getChunk(chunkPos);
        if (chunk == null) {
            LogUtils.debug(PlayerController.class, "Chunk不存在: " + chunkPos);
            return;
        }

        if (!chunk.hasTerrainData()) {
            LogUtils.debug(PlayerController.class, "Chunk没有地形数据: " + chunkPos);
            return;
        }

        // 计算chunk内坐标
        float localX = hitPoint.x - (chunkX * com.Hecate.world.Chunk.SIZE);
        float localZ = hitPoint.z - (chunkZ * com.Hecate.world.Chunk.SIZE);

        // 边界检查
        if (localX < 0) localX = 0;
        if (localZ < 0) localZ = 0;
        if (localX >= com.Hecate.world.Chunk.SIZE) localX = com.Hecate.world.Chunk.SIZE - 0.01f;
        if (localZ >= com.Hecate.world.Chunk.SIZE) localZ = com.Hecate.world.Chunk.SIZE - 0.01f;

        // 找到最接近点击位置的顶点（而不是整个tile）
        // 顶点坐标范围是0-16
        int vertexX = Math.round(localX);
        int vertexZ = Math.round(localZ);

        // 确保顶点坐标在有效范围内
        vertexX = Math.max(0, Math.min(16, vertexX));
        vertexZ = Math.max(0, Math.min(16, vertexZ));

        // 获取高度图
        com.Hecate.world.HeightMap heightMap = chunk.getSurfaceHeightMap();

        // 使用半径挖掘，避免单点极度拉伸
        int digRadius = 1; // 挖掘半径（顶点数）
        float centerDig = -0.25f; // 中心降低量

        // 记录需要标记为脏的区块
        java.util.Set<com.Hecate.world.ChunkPosition> dirtyChunks = new java.util.HashSet<>();
        dirtyChunks.add(chunkPos);

        // 对半径范围内的顶点应用渐变降低
        for (int dx = -digRadius; dx <= digRadius; dx++) {
            for (int dz = -digRadius; dz <= digRadius; dz++) {
                int nx = vertexX + dx;
                int nz = vertexZ + dz;

                // 计算距离中心的距离
                float distance = (float) Math.sqrt(dx * dx + dz * dz);

                // 基于距离的衰减系数（中心为1.0，边缘为0.0）
                float falloff = Math.max(0, 1.0f - (distance / (digRadius + 1)));

                // 应用带衰减的降低
                float digAmount = centerDig * falloff;

                // 检查是否在当前区块范围内
                if (nx >= 0 && nx <= 16 && nz >= 0 && nz <= 16) {
                    heightMap.modifyHeight(nx, nz, digAmount);

                    // 检查是否在区块边界，需要同步相邻区块
                    if (nx == 0 && chunkX > 0) {
                        // 左边界，更新左侧区块
                        com.Hecate.world.ChunkPosition leftChunk = new com.Hecate.world.ChunkPosition(chunkX - 1, 0, chunkZ);
                        com.Hecate.world.Chunk leftChunkObj = chunkManager.getChunk(leftChunk);
                        if (leftChunkObj != null && leftChunkObj.hasTerrainData()) {
                            leftChunkObj.getSurfaceHeightMap().modifyHeight(16, nz, digAmount);
                            dirtyChunks.add(leftChunk);
                        }
                    } else if (nx == 16) {
                        // 右边界，更新右侧区块
                        com.Hecate.world.ChunkPosition rightChunk = new com.Hecate.world.ChunkPosition(chunkX + 1, 0, chunkZ);
                        com.Hecate.world.Chunk rightChunkObj = chunkManager.getChunk(rightChunk);
                        if (rightChunkObj != null && rightChunkObj.hasTerrainData()) {
                            rightChunkObj.getSurfaceHeightMap().modifyHeight(0, nz, digAmount);
                            dirtyChunks.add(rightChunk);
                        }
                    }

                    if (nz == 0 && chunkZ > 0) {
                        // 前边界，更新前侧区块
                        com.Hecate.world.ChunkPosition frontChunk = new com.Hecate.world.ChunkPosition(chunkX, 0, chunkZ - 1);
                        com.Hecate.world.Chunk frontChunkObj = chunkManager.getChunk(frontChunk);
                        if (frontChunkObj != null && frontChunkObj.hasTerrainData()) {
                            frontChunkObj.getSurfaceHeightMap().modifyHeight(nx, 16, digAmount);
                            dirtyChunks.add(frontChunk);
                        }
                    } else if (nz == 16) {
                        // 后边界，更新后侧区块
                        com.Hecate.world.ChunkPosition backChunk = new com.Hecate.world.ChunkPosition(chunkX, 0, chunkZ + 1);
                        com.Hecate.world.Chunk backChunkObj = chunkManager.getChunk(backChunk);
                        if (backChunkObj != null && backChunkObj.hasTerrainData()) {
                            backChunkObj.getSurfaceHeightMap().modifyHeight(nx, 0, digAmount);
                            dirtyChunks.add(backChunk);
                        }
                    }

                    // 处理角点（同时在两个边界上）
                    if (nx == 0 && nz == 0 && chunkX > 0 && chunkZ > 0) {
                        // 左前角
                        com.Hecate.world.ChunkPosition cornerChunk = new com.Hecate.world.ChunkPosition(chunkX - 1, 0, chunkZ - 1);
                        com.Hecate.world.Chunk cornerChunkObj = chunkManager.getChunk(cornerChunk);
                        if (cornerChunkObj != null && cornerChunkObj.hasTerrainData()) {
                            cornerChunkObj.getSurfaceHeightMap().modifyHeight(16, 16, digAmount);
                            dirtyChunks.add(cornerChunk);
                        }
                    } else if (nx == 16 && nz == 0 && chunkZ > 0) {
                        // 右前角
                        com.Hecate.world.ChunkPosition cornerChunk = new com.Hecate.world.ChunkPosition(chunkX + 1, 0, chunkZ - 1);
                        com.Hecate.world.Chunk cornerChunkObj = chunkManager.getChunk(cornerChunk);
                        if (cornerChunkObj != null && cornerChunkObj.hasTerrainData()) {
                            cornerChunkObj.getSurfaceHeightMap().modifyHeight(0, 16, digAmount);
                            dirtyChunks.add(cornerChunk);
                        }
                    } else if (nx == 0 && nz == 16 && chunkX > 0) {
                        // 左后角
                        com.Hecate.world.ChunkPosition cornerChunk = new com.Hecate.world.ChunkPosition(chunkX - 1, 0, chunkZ + 1);
                        com.Hecate.world.Chunk cornerChunkObj = chunkManager.getChunk(cornerChunk);
                        if (cornerChunkObj != null && cornerChunkObj.hasTerrainData()) {
                            cornerChunkObj.getSurfaceHeightMap().modifyHeight(16, 0, digAmount);
                            dirtyChunks.add(cornerChunk);
                        }
                    } else if (nx == 16 && nz == 16) {
                        // 右后角
                        com.Hecate.world.ChunkPosition cornerChunk = new com.Hecate.world.ChunkPosition(chunkX + 1, 0, chunkZ + 1);
                        com.Hecate.world.Chunk cornerChunkObj = chunkManager.getChunk(cornerChunk);
                        if (cornerChunkObj != null && cornerChunkObj.hasTerrainData()) {
                            cornerChunkObj.getSurfaceHeightMap().modifyHeight(0, 0, digAmount);
                            dirtyChunks.add(cornerChunk);
                        }
                    }
                }
            }
        }

        // 标记所有受影响的区块为脏
        for (com.Hecate.world.ChunkPosition dirtyChunkPos : dirtyChunks) {
            com.Hecate.world.Chunk dirtyChunk = chunkManager.getChunk(dirtyChunkPos);
            if (dirtyChunk != null) {
                dirtyChunk.setDirty();
            }
        }
    }
}
