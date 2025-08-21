package com.Hecate.player;

import com.jme3.app.SimpleApplication;
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseAxisTrigger;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;

// 导入物理系统
import com.Hecate.physics.AABB;
import com.Hecate.physics.CollisionManager;

/**
 * 第三人称玩家控制器（带朝向系统和固定高空俯视视角）
 */
public class PlayerController implements ActionListener, AnalogListener {
    private final SimpleApplication app;
    private final Camera camera;
    private final InputManager inputManager;

    // 玩家方块
    private Geometry playerBlock;
    private Node playerNode;

    // 🧭 玩家朝向系统
    private float playerFacing = 0f; // 玩家朝向角度（弧度）0=北方(-Z), π/2=东方(+X), π=南方(+Z), 3π/2=西方(-X)
    private Vector3f lastMovementDirection = new Vector3f(); // 上一帧的移动方向
    private final float FACING_SMOOTH_SPEED = 8.0f; // 朝向平滑转换速度

    // 玩家移动和物理
    private Vector3f playerPosition = new Vector3f(0, 2.75f, 0); // 站在地面上(地面Y=1到Y=2，玩家中心Y=2.75)
    private Vector3f velocity = new Vector3f();
    private final float moveSpeed = 5.0f;
    private final float jumpSpeed = 8.0f;
    private final float gravity = -20.0f;
    private boolean[] moveDirection = new boolean[4]; // W, A, S, D
    private boolean isJumping = false;

    // 碰撞相关
    private static final float PLAYER_WIDTH = 1;
    private static final float PLAYER_HEIGHT = 1.5f;

    private CollisionManager collisionManager;
    private AABB playerBox;

    public PlayerController(SimpleApplication app) {
        this.app = app;
        this.camera = app.getCamera();
        this.inputManager = app.getInputManager();

        this.collisionManager = new CollisionManager();

        initializePlayer();
        setupInput();
        updateCameraPosition();

        System.out.println("📹 摄像机设置: 跟随玩家高度的俯视视角（背后5格，高度偏移3格）");
    }

    public void setCollisionManager(CollisionManager collisionManager) {
        this.collisionManager = collisionManager;
        System.out.println("✅ 玩家碰撞系统已启用");
    }

    /**
     * 🎨 初始化玩家方块（带朝向标识）
     */
    private void initializePlayer() {
        System.out.println("🔍 正在创建带朝向的玩家方块...");

        Node existingPlayer = (Node) app.getRootNode().getChild("Player");
        if (existingPlayer != null) {
            existingPlayer.removeFromParent();
            System.out.println("🗑️ 移除了现有的玩家节点");
        }

        playerNode = new Node("Player");

        // 创建玩家方块
        Box playerBoxGeom = new Box(PLAYER_WIDTH/2, PLAYER_HEIGHT/2, PLAYER_WIDTH/2);
        playerBlock = new Geometry("PlayerBlock", playerBoxGeom);

        // 🎨 创建多面材质（正面不同颜色）
        createPlayerMaterials();

        playerNode.setLocalTranslation(playerPosition);
        playerNode.attachChild(playerBlock);
        app.getRootNode().attachChild(playerNode);

        updatePlayerBox();

        System.out.println("✅ 带朝向的玩家方块已创建，位置: " + playerPosition);
        System.out.println("🧭 玩家初始朝向: 北方 (0°)");
    }

    /**
     * 🎨 创建玩家材质（正面高亮）
     */
    private void createPlayerMaterials() {
        // 基础材质（身体颜色）
        Material bodyMat = new Material(app.getAssetManager(), "Common/MatDefs/Light/Lighting.j3md");
        bodyMat.setColor("Diffuse", new ColorRGBA(0.2f, 0.5f, 1.0f, 1.0f)); // 蓝色身体
        bodyMat.setColor("Specular", ColorRGBA.White);
        bodyMat.setFloat("Shininess", 16f);

        // 暂时使用单一材质，后续可以改为多材质方块
        playerBlock.setMaterial(bodyMat);

        // TODO: 未来可以使用 MatParam 或多个 Geometry 来实现不同面的颜色
        System.out.println("🎨 玩家材质已设置（正面标识待优化）");
    }

    /**
     * 🧭 更新玩家朝向（根据移动方向）
     */
    private void updatePlayerFacing(float tpf) {
        // 计算当前移动方向
        Vector3f currentMovement = new Vector3f();
        if (moveDirection[0]) currentMovement.z -= 1; // W - 向北
        if (moveDirection[1]) currentMovement.z += 1; // S - 向南
        if (moveDirection[2]) currentMovement.x -= 1; // A - 向西
        if (moveDirection[3]) currentMovement.x += 1; // D - 向东

        // 如果有移动，更新朝向
        if (currentMovement.lengthSquared() > 0) {
            currentMovement.normalizeLocal();

            // 计算目标朝向角度
            float targetFacing = FastMath.atan2(currentMovement.x, -currentMovement.z);

            // 平滑转向目标角度
            float angleDiff = targetFacing - playerFacing;

            // 处理角度跨越问题（-π 到 π）
            while (angleDiff > FastMath.PI) angleDiff -= FastMath.TWO_PI;
            while (angleDiff < -FastMath.PI) angleDiff += FastMath.TWO_PI;

            // 平滑插值
            playerFacing += angleDiff * FACING_SMOOTH_SPEED * tpf;

            // 标准化角度
            while (playerFacing > FastMath.TWO_PI) playerFacing -= FastMath.TWO_PI;
            while (playerFacing < 0) playerFacing += FastMath.TWO_PI;

            lastMovementDirection.set(currentMovement);
        }

        // 🔄 更新玩家方块的旋转
        playerNode.setLocalRotation(playerNode.getLocalRotation().fromAngleAxis(playerFacing, Vector3f.UNIT_Y));
    }

    /**
     * 🧭 获取玩家正面方向向量
     */
    public Vector3f getPlayerFacingDirection() {
        return new Vector3f(
                FastMath.sin(playerFacing),  // X分量
                0,                           // Y分量（水平朝向）
                -FastMath.cos(playerFacing)  // Z分量
        );
    }

    /**
     * 🧭 获取玩家朝向角度（弧度）
     */
    public float getPlayerFacing() {
        return playerFacing;
    }

    /**
     * 🎯 获取指针射线起点（从摄像机位置发出）
     */
    public Vector3f getPointerOrigin() {
        // 射线从摄像机位置发出，这样十字线瞄准就是从高位置向下
        return app.getCamera().getLocation().clone();
    }

    /**
     * 🎯 获取指针射线方向（摄像机朝向）
     */
    public Vector3f getPointerDirection() {
        // 射线方向就是摄像机的朝向
        return app.getCamera().getDirection().clone();
    }

    private void setupInput() {
        System.out.println("设置玩家输入控制...");

        inputManager.addMapping("MoveForward", new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping("MoveBackward", new KeyTrigger(KeyInput.KEY_S));
        inputManager.addMapping("MoveLeft", new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping("MoveRight", new KeyTrigger(KeyInput.KEY_D));
        inputManager.addMapping("Jump", new KeyTrigger(KeyInput.KEY_SPACE));

        inputManager.addListener(this,
                "MoveForward", "MoveBackward", "MoveLeft", "MoveRight", "Jump");

        System.out.println("✅ 玩家输入控制设置完成（已移除摄像机控制）");
        System.out.println("🧭 WASD移动将自动调整玩家朝向");
    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        switch (name) {
            case "MoveForward":
                moveDirection[0] = isPressed;
                break;
            case "MoveBackward":
                moveDirection[1] = isPressed;
                break;
            case "MoveLeft":
                moveDirection[2] = isPressed;
                break;
            case "MoveRight":
                moveDirection[3] = isPressed;
                break;
            case "Jump":
                if (isPressed && !isJumping && collisionManager != null && collisionManager.isOnGround(playerBox)) {
                    velocity.y = jumpSpeed;
                    isJumping = true;
                    System.out.println("🦘 玩家跳跃！");
                }
                break;
        }
    }

    @Override
    public void onAnalog(String name, float value, float tpf) {
        // 固定视角模式下不需要摄像机控制
    }

    /**
     * 📹 更新摄像机位置（固定高空俯视角度）
     */
    private void updateCameraPosition() {
        // 🎯 固定摄像机参数
        final float CAMERA_DISTANCE_BACK = 5.0f;  // 向后5格
        final float CAMERA_HEIGHT = 6.0f;         // 距离地面6格高

        // 获取玩家朝向的反方向（背后）
        Vector3f facingDir = getPlayerFacingDirection();
        Vector3f backwardDir = facingDir.negate(); // 反向 = 背后

        // 计算摄像机位置
        Vector3f cameraPos = playerPosition.clone();
        cameraPos.addLocal(backwardDir.mult(CAMERA_DISTANCE_BACK)); // 向后5格
        cameraPos.y = CAMERA_HEIGHT; // 固定高度6格

        // 计算摄像机看向的目标点（玩家前方）
        Vector3f lookAtTarget = playerPosition.clone();
        lookAtTarget.addLocal(facingDir.mult(2.0f)); // 看向玩家前方2格的位置
        lookAtTarget.y += 1.0f; // 稍微向上看一点

        // 设置摄像机
        camera.setLocation(cameraPos);
        camera.lookAt(lookAtTarget, Vector3f.UNIT_Y);
    }

    /**
     * 📦 更新玩家碰撞盒
     */
    private void updatePlayerBox() {
        Vector3f minPoint = new Vector3f(
                playerPosition.x - PLAYER_WIDTH / 2,
                playerPosition.y - PLAYER_HEIGHT / 2,
                playerPosition.z - PLAYER_WIDTH / 2
        );
        Vector3f maxPoint = new Vector3f(
                playerPosition.x + PLAYER_WIDTH / 2,
                playerPosition.y + PLAYER_HEIGHT / 2,
                playerPosition.z + PLAYER_WIDTH / 2
        );

        if (playerBox == null) {
            playerBox = new AABB(minPoint, maxPoint);
        } else {
            playerBox.setMinPoint(minPoint);
            playerBox.setMaxPoint(maxPoint);
        }
    }

    /**
     * ⏱️ 主更新循环
     */
    public void update(float tpf) {
        // 🧭 更新玩家朝向
        updatePlayerFacing(tpf);

        Vector3f movement = new Vector3f();
        if (moveDirection[0]) movement.z -= 1; // W
        if (moveDirection[1]) movement.z += 1; // S
        if (moveDirection[2]) movement.x -= 1; // A
        if (moveDirection[3]) movement.x += 1; // D

        if (movement.lengthSquared() > 0) {
            movement.normalizeLocal();
            movement.multLocal(moveSpeed * tpf);

            if (collisionManager != null) {
                Vector3f originalPosition = playerPosition.clone();
                Vector3f newPosition = originalPosition.add(movement);

                updatePlayerBox();

                if (!collisionManager.wouldCollide(playerBox, movement)) {
                    playerPosition.set(newPosition);
                } else {
                    Vector3f slideMovement = collisionManager.getSlideMovement(playerBox, movement);
                    if (slideMovement.lengthSquared() > 0.01f) {
                        playerPosition.addLocal(slideMovement);
                    }
                }
            } else {
                playerPosition.addLocal(movement);
            }
        }

        velocity.y += gravity * tpf;

        // 🚨 临时修复：简单地面检测
        float groundLevel = 2.75f; // 地面方块Y=1到Y=2，玩家站在Y=2，中心在Y=2.75
        
        if (playerPosition.y <= groundLevel) {
            // 玩家触地
            playerPosition.y = groundLevel;
            velocity.y = 0;
            isJumping = false;
            
            // 调试信息
            if (System.currentTimeMillis() % 1000 < 16) {
                System.out.println("🏠 玩家站在简单地面上，Y=" + playerPosition.y);
            }
        } else {
            // 正常碰撞检测（暂时禁用，因为有bug）
            playerPosition.y += velocity.y * tpf;
            
            // 调试信息：每秒打印一次玩家状态
            if (System.currentTimeMillis() % 1000 < 16) {
                System.out.println("🧍 玩家下落中: " + playerPosition + ", 速度Y: " + velocity.y);
            }
        }

        updatePlayerBox();
        playerNode.setLocalTranslation(playerPosition);
        updateCameraPosition();
    }

    // Getter方法
    public Vector3f getPlayerPosition() {
        return playerPosition.clone();
    }

    public AABB getPlayerBox() {
        return playerBox;
    }
}
