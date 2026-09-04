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

    // 事件总线（武器装备/弹药变化等事件通知PanelManager等UI系统）
    private com.Hecate.event.EventBus eventBus;

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
    private boolean isCameraZoomingIn = false;  // +键是否按住（摄像机缩近，原滚轮功能）
    private boolean isCameraZoomingOut = false; // -键是否按住（摄像机缩远，原滚轮功能）
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

    // 摄像机角度平滑过渡（按WASD自动对齐/按R重置时都会用到）
    private boolean isAligningCamera = false;
    private float targetCameraBaseAngle = 0f;
    private static final float CAMERA_AUTO_ALIGN_SPEED = 6f; // 指数平滑速度，越大过渡越快（不是瞬间闪现）

    // 2D精灵系统
    private boolean useSpriteMode = false;
    private Node spriteNode;
    private PlayerSpriteManager spriteManager;
    private SpriteAnimationSystem spriteAnimationSystem;
    private DirectionalSpriteRenderer spriteRenderer;
    private SpriteScaleManager spriteScaleManager;

    // Puppet 动画系统（已废弃，替换为3D骨骼模型）
    // private boolean usePuppetMode = true;
    // private PuppetPlayerController puppetPlayerController;

    // 3D骨骼模型系统（新）
    private SkeletalPlayerController skeletalPlayerController;

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

    // 世界掉落物系统（F键交互拾取）
    private com.Hecate.item.world.WorldItemManager worldItemManager;
    private static final float PICKUP_INTERACTION_DISTANCE = 3.0f; // 拾取交互距离（米），与方块交互距离(5米)同量级但略近

    // 调试计数器
    private float debugTimer = 0f;
    private boolean wasTopView = false;
    private int frameCount = 0; // 帧计数器，用于控制debug输出频率

    // 旋转偏移测试
    private int rotationOffsetIndex = 1; // 当前使用的偏移索引（默认1 = -90度）
    private static final float[] ROTATION_OFFSETS = {
        0f,                      // 0度
        -FastMath.HALF_PI,      // -90度
        FastMath.PI,            // 180度
        FastMath.HALF_PI        // 90度
    };

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

        // 初始化 3D 骨骼模型系统（替换旧的Puppet系统）
        initializeSkeletalSystem();

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
        // 默认不显示碰撞箱（可以通过按键切换）
        collisionBoxRenderer.setVisible(false);
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
                LogUtils.debug(PlayerController.class, String.format(
                    "弹药: %.0f/%.0f (%.1f%%)",
                    currentAmmo, maxAmmo, (currentAmmo/maxAmmo)*100
                ));

                if (eventBus != null) {
                    eventBus.publish(new com.Hecate.event.AmmoChangedEvent(currentAmmo, maxAmmo));
                }
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
     * 初始化 3D 骨骼模型系统
     */
    private void initializeSkeletalSystem() {
        try {
            skeletalPlayerController = new SkeletalPlayerController(app, playerPosition.clone());
            System.out.println("[PlayerController] Skeletal player model initialized");

        } catch (Exception e) {
            System.err.println("[PlayerController] Failed to initialize skeletal model: " + e.getMessage());
            e.printStackTrace();
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
                FastMath.sin(totalAngleX),
                0,
                FastMath.cos(totalAngleX)
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
        // 滚轮改为切换背包选中槛位（不再控制摄像机缩放，见PlayerEquipment.scrollSlot）
        inputManager.addMapping("ScrollSlotNext", new MouseAxisTrigger(MouseInput.AXIS_WHEEL, false));
        inputManager.addMapping("ScrollSlotPrev", new MouseAxisTrigger(MouseInput.AXIS_WHEEL, true));
        // 摄像机缩放改绑+/-键（原本完全未被占用）
        inputManager.addMapping("CameraZoomIn", new KeyTrigger(KeyInput.KEY_EQUALS));
        inputManager.addMapping("CameraZoomOut", new KeyTrigger(KeyInput.KEY_MINUS));
        inputManager.addMapping("ResetCamera", new KeyTrigger(KeyInput.KEY_R));

        // 背包UI
        inputManager.addMapping("ToggleInventory", new KeyTrigger(KeyInput.KEY_G));

        // 竞技场系统
        inputManager.addMapping("EnterArena", new KeyTrigger(KeyInput.KEY_M));

        // 碰撞箱显示切换（F3键）
        inputManager.addMapping("ToggleCollisionBox", new KeyTrigger(KeyInput.KEY_F3));

        // 拾取地面掉落物（F键，独立于左键挖方块/攻击）
        inputManager.addMapping("PickupItem", new KeyTrigger(KeyInput.KEY_F));

        // 地形挖掘
        inputManager.addMapping("DigTerrain", new com.jme3.input.controls.MouseButtonTrigger(MouseInput.BUTTON_LEFT));

        // 武器攻击
        inputManager.addMapping("FireWeapon", new com.jme3.input.controls.MouseButtonTrigger(MouseInput.BUTTON_MIDDLE));

        inputManager.addListener(this,
                "MoveForward", "MoveBackward", "StrafeLeft", "StrafeRight",
                "RotateLeft", "RotateRight", "Jump", "MouseLook", "MouseLookNeg",
                "MouseLookUp", "MouseLookDown", "ScrollSlotNext", "ScrollSlotPrev",
                "CameraZoomIn", "CameraZoomOut", "ResetCamera",
                "DigTerrain", "ToggleInventory", "NormalMode", "NormalModeAlt", "FireWeapon",
                "ToggleHiding", "ToggleCollisionBox", "EnterArena", "PickupItem");

        // 隐藏系统鼠标光标，切换到相对移动模式。默认可见状态下，OS光标移动到屏幕
        // 边缘后就不会再产生新的位移增量，导致鼠标转镜头"转到一定角度突然转不动"
        // （卡住的角度还和光标初始位置有关，所以有时一圈半、有时半圈，表现不一致）。
        // 隐藏后引擎使用相对输入，增量不再受屏幕边界限制，可以无限旋转。
        // 背包/控制台/Buff选择这些需要可见光标的界面会在打开时自行调用setCursorVisible(true)。
        inputManager.setCursorVisible(false);
    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        // 左键"松开"事件必须无条件放行，不能被下面几道守卫拦截：否则如果玩家在
        // 按住左键开火期间打开了控制台/背包，松开事件会被提前return吞掉，
        // combatController.isLeftButtonPressed()永久卡在true，导致开火状态判断
        // （见updateGroundType()附近的isFiring）误判为"一直在开火"，堵死墨水上的
        // 恢复/加速能力。按下事件仍走下面的正常拦截逻辑，只放行松开。
        if (name.equals("DigTerrain") && !isPressed && combatController != null) {
            combatController.setLeftButtonPressed(false);
        }

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
                    // 只在"从完全没按任何移动键，到按下第一个移动键"这一瞬间触发
                    // 摄像机自动对齐，不能看cameraAutoAligned/isAligningCamera这类
                    // "对齐动画是否还在播"的状态——那种写法在按住W走路途中稍微动一下
                    // 鼠标（几乎不可避免）就会被鼠标事件清掉，导致此时再按A/D会被
                    // 误判成"新的一次按键"重新触发对齐，表现为斜着走镜头突然甩一下。
                    if (!isAnyMoveKeyPressed()) {
                        startCameraAutoAlign();
                    }
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
                    if (!isAnyMoveKeyPressed()) {
                        startCameraAutoAlign();
                    }
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
                    if (!isAnyMoveKeyPressed()) {
                        startCameraAutoAlign();
                    }
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
                    if (!isAnyMoveKeyPressed()) {
                        startCameraAutoAlign();
                    }
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
            case "CameraZoomIn":
                isCameraZoomingIn = isPressed;
                break;
            case "CameraZoomOut":
                isCameraZoomingOut = isPressed;
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
                // TODO: 如果需要，在 SkeletalPlayerController 中实现 setNormalMode
                // if (skeletalPlayerController != null) {
                //     skeletalPlayerController.setNormalMode(isPressed);
                // }
                break;
            case "FireWeapon":
                if (isPressed) {
                    Vector3f fireOrigin = playerPosition.clone();
                    fireOrigin.y += 1.0f;
                    Vector3f fireDirection = camera.getDirection().clone();
                    combatController.performGunAttack(fireOrigin, fireDirection);
                }
                break;
            case "PickupItem":
                if (isPressed) {
                    tryPickupNearestItem();
                }
                break;
        }
    }

    /**
     * 拾取交互（F键）：找玩家附近最近的掉落物，命中就放进背包并从世界移除。
     * 找不到（超出PICKUP_INTERACTION_DISTANCE范围内没有任何掉落物）则什么都不做。
     */
    private void tryPickupNearestItem() {
        if (worldItemManager == null || playerStateManager == null) {
            return;
        }

        com.Hecate.item.world.WorldItemEntity entity =
                worldItemManager.findNearest(playerPosition, PICKUP_INTERACTION_DISTANCE);
        if (entity == null) {
            return;
        }

        com.Hecate.item.ItemStack stack = entity.getItemStack();
        int remaining = playerStateManager.getBackpack().addItem(stack.getItemId(), stack.getCount());
        if (remaining >= stack.getCount()) {
            // 背包已满，一个都放不进去：不移除掉落物，留在地上给玩家清理背包后再来拾取
            return;
        }
        // 部分或全部放入成功：掉落物直接整体消失（不支持"拾取一部分、地上留一部分"，
        // 与Inventory.addItem本身"部分放入+返回剩余数量"的语义相比，这里选择更简单的
        // "全有或全没有"体验——剩余数量当前只是被丢弃，不重新生成一个数量更少的掉落物）
        worldItemManager.remove(entity);
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
                cameraAngleX = wrapAngleDeg(cameraAngleX);
                // 鼠标移动时打断正在播放的自动对齐/重置过渡动画，把镜头交还给玩家。
                // 注意：这不影响下一次触发对齐的判断——判断依据是isAnyMoveKeyPressed()
                // （见onAction），不是这个正在播放的动画状态，所以走路途中稍微
                // 动一下鼠标不会导致后续按其他移动键重新触发对齐。
                isAligningCamera = false;
                break;
            case "MouseLookNeg":
                cameraAngleX -= value * MOUSE_SENSITIVITY * 50f;
                cameraAngleX = wrapAngleDeg(cameraAngleX);
                isAligningCamera = false;
                break;
            case "MouseLookUp":
                cameraAngleY += value * MOUSE_SENSITIVITY * 30f;
                cameraAngleY = FastMath.clamp(cameraAngleY, -80f, 60f);
                break;
            case "MouseLookDown":
                cameraAngleY -= value * MOUSE_SENSITIVITY * 30f;
                cameraAngleY = FastMath.clamp(cameraAngleY, -80f, 60f);
                break;
            case "ScrollSlotNext":
                scrollSelectedSlot(1);
                break;
            case "ScrollSlotPrev":
                scrollSelectedSlot(-1);
                break;
        }
    }

    /**
     * 滚轮切换背包选中槛位（往前/往后移动一格）。控制台/背包UI/文本框有焦点、
     * 摄像机重置中这些情况已经在onAnalog开头统一拦截，这里不需要重复判断。
     */
    private void scrollSelectedSlot(int direction) {
        if (playerStateManager != null) {
            playerStateManager.getEquipment().scrollSlot(direction);
        }
    }

    /**
     * 是否有任意一个移动键（W/S/A/D）正在按住。用于判断"这次按键是从静止状态
     * 开始走路，还是已经在走路途中新增/切换方向"——只有前一种情况才应该触发
     * 摄像机自动对齐，否则斜着走（同时按住两个方向键）会在第二个键按下时
     * 被误判成"新的一次移动"而重新对齐镜头。
     */
    private boolean isAnyMoveKeyPressed() {
        return moveDirection[0] || moveDirection[1] || moveDirection[2] || moveDirection[3];
    }

    /**
     * 开始摄像机自动对齐 - 从静止开始按WASD时将摄像机平滑移动到角色背后（不再瞬间闪现）
     */
    private void startCameraAutoAlign() {
        targetCameraBaseAngle = normalizeAngleRad(playerFacing + FastMath.PI);
        isAligningCamera = true;
    }

    /**
     * 开始摄像机重置 - 按R键重置到默认位置和距离（角度平滑过渡，距离另外平滑插值）
     */
    private void startCameraReset() {
        targetCameraBaseAngle = normalizeAngleRad(playerFacing + FastMath.PI);
        isAligningCamera = true;

        // 保留距离的平滑重置
        isResettingCamera = true;
        resetProgress = 0f;
    }

    /**
     * 更新摄像机重置动画（仅重置距离，角度由updateCameraAutoAlign平滑过渡）
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

        // 只平滑插值距离，角度由updateCameraAutoAlign单独平滑过渡
        cameraDistance = lerp(cameraDistance, DEFAULT_CAMERA_DISTANCE, resetProgress);
    }

    /**
     * 平滑地将摄像机角度过渡到角色背后（targetCameraBaseAngle），
     * 同时让偏移角度回到默认值。按WASD自动对齐、按R重置都会触发。
     * 使用指数衰减插值，避免瞬间闪现的突兀感，也不会像固定时长插值那样在
     * 快速连续触发时出现卡顿或跳变。
     */
    private void updateCameraAutoAlign(float tpf) {
        if (!isAligningCamera) {
            return;
        }

        float t = 1f - FastMath.exp(-CAMERA_AUTO_ALIGN_SPEED * tpf);

        float diff = shortestAngleDiffRad(cameraBaseAngle, targetCameraBaseAngle);
        cameraBaseAngle = normalizeAngleRad(cameraBaseAngle + diff * t);

        cameraAngleX += (DEFAULT_CAMERA_ANGLE_X - cameraAngleX) * t;
        cameraAngleY += (DEFAULT_CAMERA_ANGLE_Y - cameraAngleY) * t;

        // 足够接近目标时结束过渡，避免指数衰减永远差一点点
        if (Math.abs(diff) < 0.001f
                && Math.abs(cameraAngleX - DEFAULT_CAMERA_ANGLE_X) < 0.01f
                && Math.abs(cameraAngleY - DEFAULT_CAMERA_ANGLE_Y) < 0.01f) {
            cameraBaseAngle = targetCameraBaseAngle;
            cameraAngleX = DEFAULT_CAMERA_ANGLE_X;
            cameraAngleY = DEFAULT_CAMERA_ANGLE_Y;
            isAligningCamera = false;
        }
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
     * 把角度（度）归一化到[-180, 180)。仅用于防止cameraAngleX长时间游玩后无限增长
     * 导致float精度下降——取模环绕不会造成视觉跳变（sin/cos按周期运算），
     * 不会限制旋转圈数。
     */
    private float wrapAngleDeg(float angleDeg) {
        angleDeg = angleDeg % 360f;
        if (angleDeg >= 180f) {
            angleDeg -= 360f;
        } else if (angleDeg < -180f) {
            angleDeg += 360f;
        }
        return angleDeg;
    }

    /**
     * 把角度（弧度）归一化到[0, 2π)
     */
    private float normalizeAngleRad(float angleRad) {
        while (angleRad >= FastMath.TWO_PI) angleRad -= FastMath.TWO_PI;
        while (angleRad < 0) angleRad += FastMath.TWO_PI;
        return angleRad;
    }

    /**
     * 计算从a到b的最短夹角差（弧度，范围(-π, π]），用于平滑过渡角度时不绕远路
     */
    private float shortestAngleDiffRad(float a, float b) {
        float diff = (b - a) % FastMath.TWO_PI;
        if (diff > FastMath.PI) {
            diff -= FastMath.TWO_PI;
        } else if (diff < -FastMath.PI) {
            diff += FastMath.TWO_PI;
        }
        return diff;
    }

    /**
     * 摄像机缩放（+/-键持续按住时每帧连续调整cameraDistance），取代原来滚轮的
     * 单次事件缩放——按键是持续触发的action而不是滚轮的单次analog事件，所以要
     * 按tpf连续累加，不能直接套用原ZoomIn/ZoomOut里"value*ZOOM_SPEED"那种
     * 单次增量写法。
     */
    private void updateCameraZoom(float tpf) {
        if (isCameraZoomingIn) {
            cameraDistance -= ZOOM_SPEED * tpf;
            cameraDistance = FastMath.clamp(cameraDistance, CAMERA_MIN_DISTANCE, CAMERA_MAX_DISTANCE);
        }
        if (isCameraZoomingOut) {
            cameraDistance += ZOOM_SPEED * tpf;
            cameraDistance = FastMath.clamp(cameraDistance, CAMERA_MIN_DISTANCE, CAMERA_MAX_DISTANCE);
        }
    }

    /**
     * 更新摄像机位置
     */
    private void updateCameraPosition() {
        // 更新摄像机重置进度
        updateCameraReset(app.getTimer().getTimePerFrame());

        // 平滑过渡摄像机角度到角色背后（按WASD自动对齐/按R重置时触发）
        updateCameraAutoAlign(app.getTimer().getTimePerFrame());

        // 使用独立的cameraBaseAngle，不受Q/E旋转影响
        float totalAngleX = cameraBaseAngle + (cameraAngleX * FastMath.DEG_TO_RAD);
        float angleY = cameraAngleY * FastMath.DEG_TO_RAD;

        float horizontalDistance = cameraDistance * FastMath.cos(angleY);
        float verticalDistance = cameraDistance * FastMath.sin(angleY);

        // 修复：摄像机位置计算
        // totalAngleX = 0 时，摄像机应该在 +Z 方向（北）
        // totalAngleX = PI 时，摄像机应该在 -Z 方向（南，角色背后）
        float camX = playerPosition.x - FastMath.sin(totalAngleX) * horizontalDistance;
        float camZ = playerPosition.z - FastMath.cos(totalAngleX) * horizontalDistance;
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

        // 同步角色模型位置
        if (skeletalPlayerController != null) {
            skeletalPlayerController.setPosition(playerPosition.clone());
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

        // 每帧强制同步光标可见性。窗口失去/重新获得焦点时(alt-tab等)，
        // 底层GLFW会把光标状态重置为可见，单次setCursorVisible(false)调用
        // （只在setupInput()里执行过一次）无法覆盖后续的重置，导致"光标偶尔
        // 冒出来"。这里按当前是否有需要可见光标的界面打开，每帧显式同步一次，
        // 就不会被外部事件悄悄改回默认状态。
        updateCursorVisibility();

        // 摄像机缩放（+/-键持续按住时每帧连续缩放，与原滚轮的连续手感一致）
        updateCameraZoom(tpf);

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

        // 【三选二状态管理】在友方墨水上根据按键决定激活哪两种状态。
        // 开火期间（左键按住/正在射击）禁止进入任何三选二状态——否则右键恢复
        // （50%maxAmmo/秒量级）会盖过开火消耗（大多数武器仅个位数/秒），弹药条
        // 表现为"打不完"，实质是漏了开火与恢复互斥的判断。
        boolean isFiring = combatController.isHoldingWeapon() && combatController.isLeftButtonPressed();
        if (currentGroundType == GroundType.FRIENDLY && !isFiring) {
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

                // 调试输出：显示移动向量和摄像机角度
                if (frameCount % 60 == 0) { // 每秒输出一次
                    float totalAngleX = cameraBaseAngle + (cameraAngleX * FastMath.DEG_TO_RAD);
                    System.out.println("==========================================");
                    System.out.println("移动调试信息:");
                    System.out.println("按键: W=" + moveDirection[0] + " S=" + moveDirection[1] + " A=" + moveDirection[2] + " D=" + moveDirection[3]);
                    System.out.println("摄像机角度: " + (totalAngleX * FastMath.RAD_TO_DEG) + "°");
                    System.out.println("移动向量: x=" + String.format("%.2f", movement.x) + " z=" + String.format("%.2f", movement.z));
                    System.out.println("playerFacing (旧): " + (playerFacing * FastMath.RAD_TO_DEG) + "°");
                    System.out.println("==========================================");
                }

                // 【墨水系统】应用基于地面状态的速度倍率
                // 规则：
                // - 敌方减速：始终生效
                // - 友方加速：根据hasSpeedState状态（三选二规则）
                float inkSpeedMultiplier = 1.0f;
                // TODO: 墨水系统速度倍数，暂时禁用
                // if (skeletalPlayerController != null) {
                //     float rawMultiplier = skeletalPlayerController.getSpeedMultiplier();
                //
                //     if (rawMultiplier < 1.0f) {
                //         // 敌方减速：始终生效
                //         inkSpeedMultiplier = rawMultiplier;
                //     } else if (rawMultiplier > 1.0f && hasSpeedState) {
                //         // 友方加速：根据三选二规则的hasSpeedState状态
                //         inkSpeedMultiplier = rawMultiplier;
                //     }
                //     // else: 保持1.0f（普通地面或无加速状态）
                // }

                // 基础速度始终是步行速度：己方墨水加速完全由inkSpeedMultiplier（1.6/2.0倍）
                // 体现，不再叠加FAST_MOVE_SPEED换挡——此前两者同时生效会导致重复叠加
                // （7 * 1.6 = 11.2，相当于步行速度的3.4倍，远超GridCell里设计的1.6倍）
                float baseSpeed = WALK_SPEED;

                // 应用疾跑速度倍率
                float sprintMultiplier = isSprinting ? SPRINT_SPEED_MULTIPLIER : 1.0f;

                movement.multLocal(baseSpeed * speedMultiplier * inkSpeedMultiplier * sprintMultiplier * tpf);

                // 体素方块碰撞检测（水平方向）：用移动前的碰撞盒对水平位移做修正，
                // 避免玩家直接穿过stone/dirt/wood1等体素方块。updatePlayerBox()
                // 此时反映的是移动前的位置，正好符合checkCollision需要的"当前碰撞盒"语义。
                if (collisionManager != null) {
                    updatePlayerBox();
                    Vector3f horizontalMovement = new Vector3f(movement.x, 0, movement.z);
                    Vector3f corrected = collisionManager.checkCollision(playerBox, horizontalMovement);
                    playerPosition.x += corrected.x;
                    playerPosition.z += corrected.z;
                } else {
                    playerPosition.addLocal(movement);
                }

                Vector3f normalizedMovement = movement.clone().normalizeLocal();
                lastMovementDirection.set(normalizedMovement);

                // 【修复】模型朝向始终跟随摄像机水平朝向，不跟随移动方向。
                // 之前playerFacing = atan2(移动方向)，导致单独按A/D时移动向量指向侧方，
                // 模型会"转身"去面朝侧面——看起来像原地旋转而不是横向走步（strafe）。
                // 现在移动向量只影响位移（见上面right/forward的叠加），朝向和摄像机
                // 保持一致，按A/D时模型侧身平移、脸始终朝前，符合"往旁边走"的预期。
                float cameraForwardAngle = cameraBaseAngle + (cameraAngleX * FastMath.DEG_TO_RAD);
                playerFacing = normalizeAngleRad(cameraForwardAngle);
            }
        }

        velocity.y += GRAVITY * tpf;

        // 应用重力前先用移动前的碰撞盒检测竖直方向的体素碰撞（站在方块顶上/撞天花板），
        // 再运行下面的地形高度贴合逻辑（处理连续地形曲面）。两者互补：
        // 体素碰撞管"一格一格摆出来的方块"，地形碰撞管"高度场生成的连续曲面"，
        // 缺一都会出现"能穿过方块"或"能穿过地形"的问题。
        boolean landedOnVoxelBlockThisFrame = false;
        if (collisionManager != null) {
            updatePlayerBox();
            Vector3f verticalMovement = new Vector3f(0, velocity.y * tpf, 0);
            Vector3f correctedVertical = collisionManager.checkCollision(playerBox, verticalMovement);
            playerPosition.y += correctedVertical.y;

            // 竖直方向被体素方块挡住（correctedVertical.y与原本想要的位移不一致）时停止下落/上升，
            // 避免撞到方块顶部/底部后仍持续累积速度
            if (Math.abs(correctedVertical.y - verticalMovement.y) > 0.0001f) {
                if (velocity.y < 0) {
                    isJumping = false;
                    isOnGround = true;
                    landedOnVoxelBlockThisFrame = true; // 标记：这一帧是站在体素方块上，
                    // 下面的地形高度贴合逻辑不应该因为"离地形曲面很远"而把这个状态覆盖掉
                    // （悬空的wood1平台离地形曲面本来就隔着一段距离，这是正常情况不是悬空）
                }
                velocity.y = 0;
            }
        } else {
            // 应用重力（先更新Y坐标）
            playerPosition.y += velocity.y * tpf;
        }

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
                } else if (playerPosition.y > terrainHeight + 0.1f && !landedOnVoxelBlockThisFrame) {
                    // 玩家在空中（且这一帧不是刚站到体素方块上——那种情况离地形曲面
                    // 隔着一段距离是正常的，不代表悬空）
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

        // 更新 3D 骨骼模型系统
        updateSkeletalSystem(tpf);

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
     * 更新 3D 骨骼模型系统
     */
    private void updateSkeletalSystem(float tpf) {
        if (skeletalPlayerController == null) {
            return;
        }

        // 更新角色位置
        skeletalPlayerController.setPosition(playerPosition);

        // 更新角色旋转（根据玩家朝向）
        skeletalPlayerController.setYaw(playerFacing);

        // 根据移动状态设置动画
        skeletalPlayerController.setWalking(isMoving);

        // 跳跃动画
        if (isJumping && !wasJumping) {
            skeletalPlayerController.setJumping(true);
        } else if (!isJumping && wasJumping) {
            skeletalPlayerController.setJumping(false);
        }
        wasJumping = isJumping;

        // 更新骨骼模型系统
        skeletalPlayerController.update(tpf);
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

    /**
     * 水平方向瞬间位移玩家（不影响Y轴/速度），用于怪物碰撞箱把玩家推开。
     * @param offset 位移量，只使用x/z分量
     */
    public void pushHorizontal(Vector3f offset) {
        playerPosition.x += offset.x;
        playerPosition.z += offset.z;
        updatePlayerBox();
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

        // TODO: 如果需要墨水绘制功能，在 SkeletalPlayerController 中实现
        // if (skeletalPlayerController != null) {
        //     skeletalPlayerController.setWorldNode(worldNode);
        // }

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
     * 设置世界掉落物管理器（F键拾取用，同时转发给调试命令处理器供/spawnitem使用）
     */
    public void setWorldItemManager(com.Hecate.item.world.WorldItemManager worldItemManager) {
        this.worldItemManager = worldItemManager;
        debugCommands.setWorldItemManager(worldItemManager);
    }

    /**
     * 设置方块交互系统的获取方式（用于 /give 调试命令）
     * 传入Supplier而不是具体实例，确保切换世界（如竞技场）后 /give 操作的是当前激活的世界
     */
    public void setBlockInteractionSupplier(java.util.function.Supplier<com.Hecate.block.BlockInteraction> blockInteractionSupplier) {
        debugCommands.setBlockInteractionSupplier(blockInteractionSupplier);
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

        // TODO: 墨水系统相关，暂不需要
        // if (skeletalPlayerController != null) {
        //     skeletalPlayerController.setGridManager(gridManager);
        // }
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

        // TODO: 墨水系统相关，暂不需要
        // if (skeletalPlayerController != null) {
        //     skeletalPlayerController.setPlayerTeam(team);
        // }
    }

    /**
     * 获取玩家队伍
     */
    public int getPlayerTeam() {
        // TODO: 墨水系统相关，暂时返回默认值
        // if (skeletalPlayerController != null) {
        //     return skeletalPlayerController.getPlayerTeam();
        // }
        return 1; // 默认B队（暗属性）
    }

    /**
     * 设置玩家状态管理器（物品栏系统）
     */
    public void setPlayerStateManager(PlayerStateManager playerStateManager) {
        this.playerStateManager = playerStateManager;

        // debugCommands在构造函数中创建，此时playerStateManager还不存在，
        // 这里补上/give命令需要的装备系统引用（把方块放入当前选中的快捷栏槽位）
        if (debugCommands != null && playerStateManager != null) {
            debugCommands.setPlayerEquipment(playerStateManager.getEquipment());
            debugCommands.setBackpack(playerStateManager.getBackpack());
        }

        // inventoryUI在构造函数中创建（initializeInventoryUI），此时playerStateManager还不存在，
        // 这里补上背包格子面板需要的数据引用
        if (inventoryUI != null && playerStateManager != null) {
            inventoryUI.setBackpack(playerStateManager.getBackpack(), com.Hecate.item.ItemRegistry.getInstance());
        }

        // combatController在构造函数中创建，此时playerStateManager还不存在，
        // 这里补上PlayerEquipment的武器装备/卸下监听——背包选中槛位切到带weaponId的
        // 物品时，PlayerEquipment会自动调用combatController.onWeaponEquipped/
        // onWeaponUnequipped，不再需要combatController自己持有backpack引用手动同步
        // （此前equipGun1/unequipGun1里的backpack.addItem/removeItem正是要消灭的
        // "两个真相源"问题——物品本来就已经在背包里，装备只是"选中哪一格"）。
        if (combatController != null && playerStateManager != null) {
            playerStateManager.getEquipment().setWeaponEquipListener(combatController);
        }
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
        // TODO: 墨水系统相关，暂时返回默认值
        // if (skeletalPlayerController != null) {
        //     return skeletalPlayerController.getPlayerFactionId();
        // }
        return com.Hecate.ink.FactionRegistry.DARK_DEFAULT; // 默认暗属性阵营
    }

    /**
     * 获取 SkeletalPlayerController（用于外部访问）
     */
    public SkeletalPlayerController getSkeletalPlayerController() {
        return skeletalPlayerController;
    }

    /**
     * 获取 PuppetPlayerController（已废弃，返回null）
     * @deprecated 使用 getSkeletalPlayerController() 代替
     */
    @Deprecated
    public PuppetPlayerController getPuppetPlayerController() {
        return null;
    }

    /**
     * 获取弹药系统
     */
    public PlayerAmmo getPlayerAmmo() {
        return playerAmmo;
    }

    /**
     * 设置事件总线（武器装备/卸下/弹药变化事件的发布源），同时转发给CombatController。
     * <p>并订阅武器装备/卸下事件，同步更新PlayerEquipment的外部武器覆盖标记——
     * Gun1/Gun2（PlayerCombatController，独立于快捷栏的老系统）与快捷栏方块/武器槛位
     * 原本是两套互不知情的"手持物"状态，导致装备Gun1/Gun2后右键依然能放置方块。
     * 见PlayerEquipment.setExternalWeaponOverride()。
     */
    public void setEventBus(com.Hecate.event.EventBus eventBus) {
        this.eventBus = eventBus;
        if (combatController != null) {
            combatController.setEventBus(eventBus);
        }
        if (eventBus != null) {
            eventBus.subscribe(com.Hecate.event.WeaponEquippedEvent.class,
                    event -> setExternalWeaponOverride(true));
            eventBus.subscribe(com.Hecate.event.WeaponUnequippedEvent.class,
                    event -> setExternalWeaponOverride(false));
        }
    }

    /**
     * 同步Gun1/Gun2的装备状态到PlayerEquipment的外部武器覆盖标记
     */
    private void setExternalWeaponOverride(boolean active) {
        if (playerStateManager != null) {
            playerStateManager.getEquipment().setExternalWeaponOverride(active);
        }
    }

    /**
     * 设置面板管理器（转发给InventoryUI，用于鼠标悬停背包物品时显示说明面板）
     */
    public void setPanelManager(com.Hecate.ui.PanelManager panelManager) {
        if (inventoryUI != null) {
            inventoryUI.setPanelManager(panelManager);
        }
    }

    /**
     * 每帧同步系统鼠标光标可见性。只有背包UI需要可见光标去点击/拖拽物品，
     * 其他覆盖层（GameConsole、BuffSelectUI）都是纯键盘操作，不需要光标。
     * 之所以要每帧调用而不是只在背包toggle时调用一次：窗口失焦/重新聚焦
     * （比如alt-tab、点击任务栏）会让底层GLFW把光标状态改回可见，如果只在
     * toggle时设置一次，之后就会被这类外部事件悄悄覆盖掉，表现为光标偶尔
     * 无缘无故冒出来。每帧强制同步一次就不会被覆盖。
     */
    private void updateCursorVisibility() {
        boolean shouldBeVisible = inventoryUI != null && inventoryUI.isVisible();
        if (inputManager.isCursorVisible() != shouldBeVisible) {
            inputManager.setCursorVisible(shouldBeVisible);
        }
    }

    /**
     * 鼠标是否处于"解放"状态（可自由移动指向物品，而非锁定控制镜头朝向）。
     * 目前唯一会解除鼠标锁定的界面是背包UI，其他覆盖层（如GameConsole）不影响此状态。
     */
    public boolean isCursorFree() {
        return inventoryUI != null && inventoryUI.isVisible();
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
     * 扔掉当前手持物品：从背包选中槛位取出物品堆，清空该槛位（若有weaponId，
     * PlayerEquipment.removeFromCurrentSlot内部会自动触发卸下武器——不需要
     * 额外调用combatController，装备状态永远精确匹配槛位内容），再在玩家前方
     * 按抛物线算出落地点生成一个世界掉落物。
     * <p>抛物线只是"算一次落点"，不需要真的接入Projectile的逐帧更新循环——
     * 掉落物落地后是静止的，不需要飞行过程的中间帧。
     */
    private void dropCurrentItem() {
        if (playerStateManager == null || worldItemManager == null || currentWorldNode == null) {
            return;
        }

        com.Hecate.item.ItemStack stack = playerStateManager.getEquipment().getCurrentHeldItem();
        if (stack.isEmpty()) {
            return;
        }

        int selectedSlot = playerStateManager.getEquipment().getSelectedSlot();
        int count = stack.getCount();
        playerStateManager.getEquipment().removeFromCurrentSlot(count);

        Vector3f landingPos = computeDropLandingPosition();
        worldItemManager.spawn(currentWorldNode, stack.getItemId(), count, landingPos);
    }

    // 丢弃抛物线参数：初速度朝正前方偏上一点，重力用与SteampunkGun同量级的手感数值，
    // 不需要精确物理，只要"抛出去往前落地"的视觉效果合理
    private static final float DROP_THROW_SPEED = 4.0f;
    private static final float DROP_THROW_UP_ANGLE = 0.35f; // 弧度，约20度仰角
    private static final float DROP_GRAVITY = -15.0f;
    private static final float DROP_MAX_TIME = 3.0f; // 抛物线最长模拟时长（秒），防止极端情况下死循环

    /**
     * 按抛物线公式（初速度+重力）一次性算出丢弃物品的落地世界坐标：从玩家当前位置、
     * 沿摄像机水平朝向，以固定仰角和初速度抛出，逐小步模拟直到穿过地形表面为止。
     * 与{@link com.Hecate.weapon.Projectile}的BALLISTIC弧线是同一套加速度模型
     * （见Projectile.updateBallistic），但这里不需要生成Projectile实例走每帧更新——
     * 落地位置在丢弃的这一帧就能一次性算完。
     */
    private Vector3f computeDropLandingPosition() {
        Vector3f forward = camera.getDirection().clone();
        forward.y = 0;
        if (forward.lengthSquared() < 0.0001f) {
            forward.set(0, 0, 1);
        } else {
            forward.normalizeLocal();
        }

        Vector3f velocity = forward.mult(DROP_THROW_SPEED * FastMath.cos(DROP_THROW_UP_ANGLE));
        velocity.y = DROP_THROW_SPEED * FastMath.sin(DROP_THROW_UP_ANGLE);

        Vector3f position = playerPosition.clone();
        position.y += 1.0f; // 从玩家胸口高度附近抛出，与其他武器发射起点一致

        float dt = 0.05f;
        float elapsed = 0f;
        while (elapsed < DROP_MAX_TIME) {
            Vector3f nextPosition = position.add(velocity.mult(dt));
            float terrainHeight = collisionManager != null
                    ? collisionManager.getTerrainHeightAt(nextPosition.x, nextPosition.z) : Float.NaN;

            if (!Float.isNaN(terrainHeight) && nextPosition.y <= terrainHeight) {
                return new Vector3f(nextPosition.x, terrainHeight, nextPosition.z);
            }

            position = nextPosition;
            velocity.y += DROP_GRAVITY * dt;
            elapsed += dt;
        }

        // 模拟超时仍未落地（例如上方悬空太高），直接用最后位置——总比不生成掉落物好
        return position;
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
