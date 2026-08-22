package com.Hecate.player;

import com.Hecate.block.CorpseBlock;
import com.Hecate.block.CorpseBlockManager;
import com.Hecate.physics.AABB;
import com.Hecate.physics.CollisionManager;
import com.Hecate.player.inventory.PlayerStateManager;
import com.Hecate.player.effect.ActiveEffect;
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

    // 战斗控制器（武器、攻击逻辑）
    private PlayerCombatController combatController;

    // 体素交互控制器（地形挖掘）
    private PlayerVoxelInteraction voxelInteraction;

    // 调试命令处理器
    private PlayerDebugCommands debugCommands;

    // 子弹管理器
    private com.Hecate.weapon.ProjectileManager projectileManager;

    // 玩家阵营（默认暗属性）
    private int playerFactionId = com.Hecate.ink.FactionRegistry.DARK_DEFAULT;

    // 按键状态
    private boolean isShiftPressed = false;   // Shift键是否按下
    private boolean isCtrlPressed = false;    // Ctrl键是否按下
    private boolean isRightButtonPressed = false; // 右键是否按下

    // 地面类型枚举
    private enum GroundType {
        NONE,      // 无涂墨
        FRIENDLY,  // 己方涂墨
        IGNITED    // 点燃
    }
    private GroundType currentGroundType = GroundType.NONE;

    // 【三选二状态机】友方墨水上可激活的三种能力，最多同时激活两种
    // 按键组合决定激活哪两种：
    // - 仅右键：恢复 + 加速
    // - 仅Shift：隐藏 + 加速
    // - Shift + 右键：隐藏 + 恢复（无加速，无法移动）
    private boolean hasRecoveryState = false;   // 恢复状态：回血回弹药
    private boolean hasHidingState = false;     // 隐藏状态：不可见（暂未实现视觉效果）
    private boolean hasSpeedState = false;      // 加速状态：移动速度提升
    private boolean isFastMoving = false;       // 当前是否在快速移动（通过加速状态触发）

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

    // 玩家状态管理器（物品栏系统）
    private PlayerStateManager playerStateManager;

    // 火焰渲染器（用于子弹粒子效果）
    private com.Hecate.flame.SimpleFlameRenderer flameRenderer;

    // UI系统
    private InventoryUI inventoryUI;
    private GameConsole gameConsole;

    // 竞技场系统（世界切换由 WorldSwitcher 统一处理）
    private com.Hecate.arena.WorldSwitcher worldSwitcher;

    // 怪物系统
    private com.Hecate.monster.MonsterManager monsterManager;
    // 当前活动世界的场景节点（随setWorldNode同步更新）
    private Node currentWorldNode;

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

        // 初始化战斗控制器
        combatController = new PlayerCombatController(app, playerAmmo);
        combatController.setPositionProvider(() -> playerPosition);

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

        // 初始化调试命令处理器
        debugCommands = new PlayerDebugCommands(app, gameConsole, combatController);
        debugCommands.setPlayerInfoProvider(new PlayerDebugCommands.PlayerInfoProvider() {
            @Override
            public Vector3f getPosition() {
                return playerPosition;
            }

            @Override
            public float getFacing() {
                return playerFacing;
            }
        });
        debugCommands.registerAllCommands();

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

        // 初始化体素交互控制器
        voxelInteraction = new PlayerVoxelInteraction(camera, collisionManager);

        // 子弹的地形碰撞检测也依赖同一个碰撞管理器
        refreshProjectileManager();
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
    }

    /**
     * 设置火焰渲染器并切换到火焰武器
     */
    public void setFlameRenderer(com.Hecate.flame.SimpleFlameRenderer flameRenderer) {
        // 保存火焰渲染器引用
        this.flameRenderer = flameRenderer;

        // 传递给战斗控制器
        combatController.setFlameRenderer(flameRenderer);

        // 创建火焰武器并设置为默认武器
        com.Hecate.weapon.FlameWeapon flameWeapon = com.Hecate.weapon.FlameWeapon.createDefault(flameRenderer, camera);
        flameWeapon.setPlayerController(this);
        combatController.setCurrentWeapon(flameWeapon);
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

        // 死亡期间 onAction 会因 isDead==true 而整体忽略输入事件，
        // 若玩家死亡瞬间正按着移动键/鼠标键，松开事件也会被吞掉，
        // 导致这些按键状态永久卡在"按下"——复活后必须显式清空，否则会自动移动/开火。
        resetInputStates();

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
                    LogUtils.debug(PlayerController.class, "按下Q键，isCtrlPressed=" + isCtrlPressed);
                    if (isCtrlPressed) {
                        // Ctrl + Q：旋转模型（原功能）
                        float oldFacing = playerFacing;
                        // 按一次立刻转90度（逆时针）
                        playerFacing -= FastMath.HALF_PI;
                        // 标准化角度
                        while (playerFacing < 0) playerFacing += FastMath.TWO_PI;
                        LogUtils.debug(PlayerController.class, "执行旋转模型");
                    } else {
                        // 单独按Q：扔掉当前手持物品
                        LogUtils.debug(PlayerController.class, "尝试扔掉物品");
                        dropCurrentItem();
                    }
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
                // 左键行为：委托给战斗控制器或体素交互
                if (isPressed) {
                    if (combatController.isHoldingWeapon()) {
                        // 持枪状态：记录按下状态（驱动连发），并立即开火/开始蓄力
                        combatController.setLeftButtonPressed(true);
                        Vector3f fireOrigin = playerPosition.clone();
                        fireOrigin.y += 1.0f;
                        Vector3f fireDirection = camera.getDirection().clone();
                        combatController.performGunAttack(fireOrigin, fireDirection);
                    } else {
                        // 无枪状态：挖掘地形（委托给体素交互组件）
                        voxelInteraction.performTerrainDig();
                    }
                } else {
                    // 按键松开：停止连发，如果正在蓄力则释放蓄力攻击
                    if (combatController.isHoldingWeapon()) {
                        combatController.setLeftButtonPressed(false);
                        Vector3f fireOrigin = playerPosition.clone();
                        fireOrigin.y += 1.0f;
                        Vector3f fireDirection = camera.getDirection().clone();
                        combatController.releaseChargedAttack(fireOrigin, fireDirection);
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
                isCtrlPressed = isPressed;
                if (puppetPlayerController != null) {
                    puppetPlayerController.setNormalMode(isPressed);
                }
                break;
            case "FireWeapon":
                if (isPressed) {
                    Vector3f fireOrigin = playerPosition.clone();
                    fireOrigin.y += 1.0f;
                    Vector3f fireDirection = camera.getDirection().clone();
                    combatController.performGunAttack(fireOrigin, fireDirection);
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

        // 更新战斗控制器
        combatController.update(tpf);

        // 【地面类型检测】检测玩家脚下的墨水类型
        updateGroundType();

        // 【三选二状态管理】在友方墨水上根据按键决定激活哪两种状态
        if (currentGroundType == GroundType.FRIENDLY) {
            boolean bothPressed = isShiftPressed && isRightButtonPressed;

            if (bothPressed) {
                // 同时按下：隐藏 + 恢复
                hasHidingState = true;
                hasRecoveryState = true;
                hasSpeedState = false;
            } else if (isRightButtonPressed) {
                // 仅右键：恢复 + 加速
                hasRecoveryState = true;
                hasSpeedState = true;
                hasHidingState = false;
            } else if (isShiftPressed) {
                // 仅Shift：隐藏 + 加速
                hasHidingState = true;
                hasSpeedState = true;
                hasRecoveryState = false;
            } else {
                // 无按键：无特殊效果
                hasSpeedState = false;
                hasRecoveryState = false;
                hasHidingState = false;
            }
        } else {
            // 非友方墨水：无特殊状态
            hasRecoveryState = false;
            hasHidingState = false;
            hasSpeedState = false;
        }

        // 应用恢复状态（三选二规则）
        if (hasRecoveryState && currentGroundType == GroundType.FRIENDLY) {
            // 计算实际恢复速率（基础值 * buff叠加）
            float actualRecoveryRate = getActualRecoveryRate();

            // 恢复血量
            if (playerHealth != null && !playerHealth.isFullHealth()) {
                playerHealth.recoverByPercentage(actualRecoveryRate, tpf);
            }
            // 恢复弹药
            if (playerAmmo != null && !playerAmmo.isFull()) {
                playerAmmo.recoverByPercentage(actualRecoveryRate, tpf);
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

            if (isMoving) {
                movement.normalizeLocal();

                // 【墨水系统】应用基于地面状态的速度倍率
                // 规则：
                // - 敌方减速：始终生效
                // - 友方加速：根据hasSpeedState状态（三选二规则）
                float inkSpeedMultiplier = 1.0f;
                if (usePuppetMode && puppetPlayerController != null) {
                    float rawMultiplier = puppetPlayerController.getSpeedMultiplier();

                    if (rawMultiplier < 1.0f) {
                        // 敌方减速：始终生效
                        inkSpeedMultiplier = rawMultiplier;
                    } else if (rawMultiplier > 1.0f && hasSpeedState) {
                        // 友方加速：根据三选二规则的hasSpeedState状态
                        inkSpeedMultiplier = rawMultiplier;
                    }
                    // else: 保持1.0f（普通地面或无加速状态）
                }

                // 基础速度始终是步行速度：己方墨水加速完全由inkSpeedMultiplier（1.6/2.0倍）
                // 体现，不再叠加FAST_MOVE_SPEED换挡——此前两者同时生效会导致重复叠加
                // （7 * 1.6 = 11.2，相当于步行速度的3.4倍，远超GridCell里设计的1.6倍）
                float baseSpeed = WALK_SPEED;

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

    }

    /**
     * 更新手持武器模型位置（Gun1/Gun2共用），使武器跟随玩家，出现在玩家面前
     */
    private void updateHeldWeaponPosition(Node weaponNode) {
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

        weaponNode.setLocalTranslation(weaponPos);

        // 设置武器旋转，使其朝向与玩家相同
        // 使用水平朝向计算旋转
        Quaternion rotation = new Quaternion();
        rotation.lookAt(horizontalForward, Vector3f.UNIT_Y);
        weaponNode.setLocalRotation(rotation);
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

    // 【三选二状态机】状态查询接口
    /**
     * 查询当前是否处于恢复状态（回血回弹药）
     */
    public boolean hasRecoveryState() {
        return hasRecoveryState;
    }

    /**
     * 查询当前是否处于隐藏状态（不可见）
     */
    public boolean hasHidingState() {
        return hasHidingState;
    }

    /**
     * 查询当前是否处于加速状态（移动速度提升）
     */
    public boolean hasSpeedState() {
        return hasSpeedState;
    }

    /**
     * 查询当前地面类型（NONE/FRIENDLY/IGNITED）
     */
    public String getCurrentGroundType() {
        return currentGroundType.name();
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
     * 现在从效果系统读取叠加的移动速度buff
     */
    public float getSpeedMultiplier() {
        float baseMultiplier = speedMultiplier; // /speed指令设置的基础倍数

        if (playerStateManager == null) {
            return baseMultiplier;
        }

        // 使用PlayerStateManager提供的速度倍率（已经整合了所有速度相关效果）
        return baseMultiplier * playerStateManager.getSpeedMultiplier();
    }

    /**
     * 获取实际恢复速率（基础值 * buff叠加）
     */
    private float getActualRecoveryRate() {
        float baseRate = 0.05f; // 基础恢复速率 5%每秒

        if (playerStateManager == null) {
            return baseRate;
        }

        // 使用PlayerStateManager提供的恢复速率倍率
        return baseRate * playerStateManager.getRecoveryMultiplier();
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

        // 同步调试命令的世界节点
        if (debugCommands != null) {
            debugCommands.setCurrentWorldNode(worldNode);
        }

        // 传递世界节点给战斗控制器（用于 SteampunkGun 等武器）
        if (combatController != null) {
            combatController.setWorldNode(worldNode);
            combatController.updateWeaponWorldNode(worldNode);
        }

        // 世界切换时Gun2的子弹更新循环也要重新指向新世界节点
        refreshProjectileManager();
    }

    /**
     * (重新)构建武器共享的子弹更新循环。gridManager/monsterManager/collisionManager/
     * currentWorldNode 四者的setter调用顺序不固定（ApplicationContext.connectSystems()里
     * setWorldNode先于setGridManager/setMonsterManager/setCollisionManager），所以每个
     * setter都调用本方法，以最新的四个依赖重建，避免捕获到尚未注入的null依赖。
     * <p>重建前会清空旧的子弹更新循环，防止旧世界节点下的子弹方块残留。
     */
    private void refreshProjectileManager() {
        if (projectileManager != null) {
            projectileManager.clear();
        }
        if (currentWorldNode != null) {
            projectileManager = new com.Hecate.weapon.ProjectileManager(
                    app.getAssetManager(), currentWorldNode, monsterManager, gridManager, collisionManager);
            combatController.setProjectileManager(projectileManager);
        } else {
            projectileManager = null;
            combatController.setProjectileManager(null);
        }
    }

    /**
     * 设置怪物管理器（用于调试命令）
     */
    public void setMonsterManager(com.Hecate.monster.MonsterManager monsterManager) {
        this.monsterManager = monsterManager;
        combatController.setMonsterManager(monsterManager);
        debugCommands.setMonsterManager(monsterManager);
        refreshProjectileManager();
    }

    /**
     * "攻击弹道+1"buff的当前叠加数量（FlameWeapon开火时读取）
     * 现在从效果系统读取
     */
    public int getExtraProjectiles() {
        if (playerStateManager == null) {
            return 0;
        }
        ActiveEffect effect = playerStateManager.getEffectManager().getEffect("extra_projectile");
        return effect != null ? effect.getStacks() : 0;
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
            resetInputStates();
            inputLocked = false;
            if (onComplete != null) {
                onComplete.run();
            }
        });
    }

    /**
     * 重置所有输入状态，防止按键卡住
     */
    private void resetInputStates() {
        // 重置移动方向
        moveDirection[0] = false;
        moveDirection[1] = false;
        moveDirection[2] = false;
        moveDirection[3] = false;

        // 重置按键状态
        isShiftPressed = false;
        isCtrlPressed = false;
        isRightButtonPressed = false;
    }

    /**
     * 应用玩家选中的buff效果
     * 现在使用效果系统，而非硬编码字段修改
     */
    private void onBuffSelected(BuffType type) {
        if (playerStateManager == null) {
            LogUtils.warning(PlayerController.class, "PlayerStateManager未初始化，无法应用Buff");
            return;
        }

        String effectId = null;
        switch (type) {
            case FIRE_RATE_UP:
                effectId = "fire_rate_boost";
                break;
            case EXTRA_PROJECTILE:
                effectId = "extra_projectile";
                break;
            case SPREAD_RANGE_UP:
                effectId = "spread_range_boost";
                break;
            case RECOVERY_SPEED_UP:
                effectId = "recovery_boost";
                break;
            case MOVE_SPEED_UP:
                effectId = "move_speed_boost";
                break;
        }

        if (effectId != null) {
            playerStateManager.getEffectManager().applyEffect(effectId);
            LogUtils.debug(PlayerController.class, "已应用Buff效果: " + type.getDisplayName() + " -> " + effectId);
        }
    }

    /**
     * 设置网格管理器（用于墨水系统速度倍率和地面类型检测）
     */
    public void setGridManager(com.Hecate.ink.SparseGridManager gridManager) {
        this.gridManager = gridManager;

        // 传递给战斗控制器
        combatController.setGridManager(gridManager);

        if (puppetPlayerController != null) {
            puppetPlayerController.setGridManager(gridManager);
        }
        refreshProjectileManager();
    }

    /**
     * 设置玩家队伍（用于墨水系统速度倍率）
     * @param team 队伍编号（0=A队，1=B队）
     */
    public void setPlayerTeam(int team) {
        // team(0/1) 需要映射为阵营ID后再传给战斗控制器，否则墨水颜色会查错
        // （FactionRegistry里 LIGHT_DEFAULT=1、DARK_DEFAULT=2，与team编号并不相等）
        int factionId = (team == 0)
                ? com.Hecate.ink.FactionRegistry.LIGHT_DEFAULT
                : com.Hecate.ink.FactionRegistry.DARK_DEFAULT;
        combatController.setPlayerFactionId(factionId);

        if (puppetPlayerController != null) {
            puppetPlayerController.setPlayerTeam(team);
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
     * 设置玩家状态管理器（物品栏系统）
     */
    public void setPlayerStateManager(PlayerStateManager playerStateManager) {
        this.playerStateManager = playerStateManager;
    }

    /**
     * 获取玩家状态管理器（装备系统+效果系统），可能为null（未初始化完成前）。
     * 供怪物攻击行为（如中毒/流血DOT）施加效果到玩家身上。
     */
    public PlayerStateManager getPlayerStateManager() {
        return playerStateManager;
    }

    /**
     * 获取玩家阵营ID（用于墨水系统）
     */
    public int getPlayerFactionId() {
        if (puppetPlayerController != null) {
            return puppetPlayerController.getPlayerFactionId();
        }
        return com.Hecate.ink.FactionRegistry.DARK_DEFAULT; // 默认暗属性阵营
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
     * 获取当前武器（委托给 CombatController）
     */
    public Weapon getCurrentWeapon() {
        return combatController != null ? combatController.getCurrentWeapon() : null;
    }

    /**
     * 设置当前武器（委托给 CombatController）
     * 自动将PlayerStateManager传递给武器的WeaponStats，以便动态计算buff加成
     */
    public void setCurrentWeapon(Weapon weapon) {
        if (combatController != null) {
            combatController.setCurrentWeapon(weapon);

            // 将PlayerStateManager传递给武器属性，使其能动态计算buff
            if (weapon != null && weapon.getStats() != null && playerStateManager != null) {
                weapon.getStats().setPlayerStateManager(playerStateManager);
            }
        }
    }


    /**
     * 设置玩家阵营ID
     */
    public void setPlayerFactionId(int factionId) {
        this.playerFactionId = factionId;

    }

    /**
     * 扔掉当前手持物品（委托给 CombatController）
     */
    private void dropCurrentItem() {
        if (combatController != null) {
            combatController.dropCurrentItem();
        }
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
     * 设置右键用于恢复（由PlayerControlModule调用）
     */
    public void setRightButtonForRecovery(boolean pressed) {
        isRightButtonPressed = pressed;
        if (pressed) {

        } else {

        }
    }

}
