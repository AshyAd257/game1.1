package com.Hecate.player;

import com.Hecate.puppet.core.PuppetRenderer;
import com.Hecate.puppet.PlanarShadow;
import com.Hecate.puppet.config.AnimationConfig;
import com.Hecate.puppet.config.AnimationIO;
import com.Hecate.puppet.config.PuppetConfig;
import com.Hecate.puppet.config.PuppetIO;
import com.Hecate.puppet.animation.AnimationPlayer;
import com.Hecate.puppet.animation.AnimationClip;
import com.Hecate.ink.SparseGridManager;
import com.Hecate.ink.GridCell;
import com.jme3.app.SimpleApplication;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.math.Quaternion;
import com.jme3.scene.Node;

/**
 * 基于 Puppet 动画系统的玩家控制器
 * 使用骨骼动画替代精灵序列帧
 */
public class PuppetPlayerController {

    private final SimpleApplication app;
    private final Node rootNode;

    // Puppet 系统组件
    private PuppetRenderer puppetRenderer;
    private AnimationPlayer animationPlayer;
    private Node puppetNode;
    private PlanarShadow planarShadow;  // 平面阴影系统

    // 墨水系统组件
    private SparseGridManager gridManager;
    private int playerFactionId = com.Hecate.ink.FactionRegistry.DARK_DEFAULT;  // 玩家阵营ID（默认暗属性）

    // 动画剪辑
    private AnimationClip walkAnimation;
    private AnimationClip jumpAnimation;

    // 调试计数器
    private int debugFrameCounter = 0;

    // 玩家状态
    private Vector3f position;
    private float rotation = 0f;  // 保留用于兼容性，实际使用charYaw
    private boolean isWalking = false;
    private boolean isJumping = false;

    // 【新架构】两个独立的旋转状态
    private float charYaw = 0f;      // 人物真实朝向（Q/E改变）
    private float cameraYaw = 0f;    // 镜头朝向（鼠标改变）

    // 上一次的旋转角度（用于检测旋转变化）
    private float lastCharYaw = 0f;
    private float lastCameraYaw = 0f;
    private String currentDirection = "back"; // 当前显示的方向（圆盘模式默认显示back）

    // 【模式控制】
    private boolean isNormalMode = false;  // false=圆盘模式（默认），true=普通模式（按住Ctrl）

    // 动画文件路径（使用classpath资源）
    private static final String PUPPET_PATH = "puppets/successv5.puppet";
    private static final String WALK_ANIM_PATH = "puppets/walknew.anim";
    private static final String JUMP_ANIM_PATH = "puppets/jump.anim";

    // Puppet 渲染偏移（防止穿模）
    private static final float PUPPET_Y_OFFSET = 0.8f;  // 向上偏移，防止沉入地面
    private static final float PUPPET_SCREEN_OFFSET = -0.8f; // 屏幕空间偏移（相对于相机左侧）

    public PuppetPlayerController(SimpleApplication app, Vector3f startPosition) {
        this.app = app;
        this.rootNode = app.getRootNode();
        this.position = startPosition.clone();

        initializePuppetSystem();
    }

    /**
     * 初始化 Puppet 动画系统
     */
    private void initializePuppetSystem() {
        try {
            // 加载 puppet 配置（从 classpath 资源）
            PuppetConfig puppetConfig = PuppetIO.loadFromResource(PUPPET_PATH);

            // 创建空的 Skeleton 和 PuppetRenderer
            com.Hecate.puppet.core.Skeleton skeleton = new com.Hecate.puppet.core.Skeleton(puppetConfig.getName());
            puppetRenderer = new PuppetRenderer(app, skeleton);

            // 应用配置（这会创建所有骨骼和渲染器）
            PuppetIO.applyConfig(puppetConfig, skeleton, puppetRenderer);

            // 创建节点并添加到场景
            puppetNode = new Node("PuppetPlayer");
            puppetNode.setLocalTranslation(position);

            // 缩小puppet到合适的大小（原始大小太大了）
            puppetNode.setLocalScale(0.3f);  // 缩小到30%

            puppetNode.attachChild(puppetRenderer.getPuppetNode());
            rootNode.attachChild(puppetNode);

            // 【新增】创建平面阴影系统 - 暂时禁用以测试DirectionalLightShadowRenderer
            /*
            planarShadow = new PlanarShadow(app, puppetRenderer);
            // 地面高度：人物正好在地面上时，阴影也应该在地面
            // 尝试更大的偏移量来让阴影降到地面
            planarShadow.setGroundY(position.y - 2.0f);
            planarShadow.setShadowOpacity(0.5f);  // 阴影不透明度
            planarShadow.initialize(rootNode);
            */

            // 创建动画播放器
            animationPlayer = new AnimationPlayer(skeleton, puppetRenderer);

            // 【关键】禁用自动方向切换，使用手动控制（通过updatePuppetDirection）
            // 如果启用自动切换，会完全根据相机位置决定方向，忽略Q/E键的旋转
            puppetRenderer.setAutoDirectionSwitch(false);

            // 保持统一billboard模式，让部件始终面向摄像机
            puppetRenderer.setBillboardMode(PuppetRenderer.BillboardMode.UNIFIED);

            // 【圆盘模式初始化】设置初始方向为back（背对相机）
            setInitialDirection("back");

            // 加载动画
            AnimationConfig walkConfig = AnimationIO.loadAnimation(WALK_ANIM_PATH);
            com.Hecate.puppet.config.BoneMappingConfig walkMapping = createIdentityMapping(walkConfig, skeleton);
            walkAnimation = com.Hecate.puppet.config.AnimationIO.applyAnimation(walkConfig, walkMapping);

            AnimationConfig jumpConfig = AnimationIO.loadAnimation(JUMP_ANIM_PATH);
            com.Hecate.puppet.config.BoneMappingConfig jumpMapping = createIdentityMapping(jumpConfig, skeleton);
            jumpAnimation = com.Hecate.puppet.config.AnimationIO.applyAnimation(jumpConfig, jumpMapping);

        } catch (Exception e) {
            System.err.println("Puppet 系统初始化失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 创建身份映射（动画骨骼名 = 木偶骨骼名）
     */
    private com.Hecate.puppet.config.BoneMappingConfig createIdentityMapping(
            AnimationConfig animConfig, com.Hecate.puppet.core.Skeleton skeleton) {
        com.Hecate.puppet.config.BoneMappingConfig mapping =
            new com.Hecate.puppet.config.BoneMappingConfig(animConfig.getName(), skeleton.getName());

        // 为每个动画骨骼创建同名映射
        for (String boneName : animConfig.getAllBoneNames()) {
            mapping.addMapping(boneName, boneName);
        }

        return mapping;
    }

    /**
     * 更新玩家状态和动画
     */
    public void update(float tpf) {
        if (puppetRenderer == null || animationPlayer == null) {
            return;
        }

        // 【关键修复】先更新位置，再更新渲染器
        // 这样bone的世界变换才能基于正确的puppetNode位置计算
        Vector3f adjustedPosition = position.clone();
        adjustedPosition.y += PUPPET_Y_OFFSET;

        // 【修复】移除屏幕空间偏移，避免旋转镜头时人物围绕中心点旋转
        // 人物应该以自己为轴心旋转，而不是跟随相机偏移
        // Vector3f cameraLeft = app.getCamera().getLeft().normalize();
        // adjustedPosition.addLocal(cameraLeft.mult(PUPPET_SCREEN_OFFSET));

        puppetNode.setLocalTranslation(adjustedPosition);

        // 【两状态系统】同步人物朝向和镜头朝向
        float oldCharYaw = charYaw;

        // 计算相机位置和朝向（两种模式都需要）
        Vector3f camPos = app.getCamera().getLocation();
        Vector3f playerPos = position.clone();
        Vector3f toCamera = camPos.subtract(playerPos);
        toCamera.y = 0;

        if (toCamera.lengthSquared() >= 0.0001f) {
            toCamera.normalizeLocal();
            // 计算相机的世界角度
            float newCameraYaw = (float)Math.atan2(toCamera.x, toCamera.z);
            cameraYaw = normalizeSignedAngle(newCameraYaw);
        }

        // 【圆盘模式 vs 普通模式】
        if (isNormalMode) {
            // 普通模式：人物朝向由Q/E控制
            charYaw = rotation;
        } else {
            // 圆盘模式：人物朝向跟随相机，始终背对相机
            charYaw = normalizeSignedAngle(cameraYaw + FastMath.PI);
        }

        // 【关键修复】检测charYaw或cameraYaw的变化，任一改变都需要更新方向
        // 使用deltaAngle避免跨边界时的巨大误差
        boolean charYawChanged = Math.abs(deltaAngle(charYaw, lastCharYaw)) > 0.01f;
        boolean cameraChanged = Math.abs(deltaAngle(cameraYaw, lastCameraYaw)) > 0.01f;

        // 【模式判断】只有在普通模式下才根据相机位置切换方向
        if (isNormalMode && (charYawChanged || cameraChanged)) {
            updatePuppetDirection();  // 根据观察角判断显示哪一面
            lastCharYaw = charYaw;
            lastCameraYaw = cameraYaw;
        } else if (!isNormalMode) {
            // 【圆盘模式】固定显示back方向，不随相机切换
            // 只在第一次或模式切换时设置
            if (!currentDirection.equals("back")) {
                setFixedDirection("back");
            }
        }

        // 【关键修复】设置骨骼旋转角度，让骨骼随着角色朝向旋转
        // 这样贴图的"上下左右"方向才是正确的
        // charYaw 是角色的真实朝向（Q/E键控制）
        puppetRenderer.setManualRotationAngle(charYaw);

        // 更新动画播放器
        animationPlayer.update(tpf);

        // 更新 puppet 渲染
        puppetRenderer.update(tpf);

        // 更新平面阴影（传递当前人物位置，让阴影跟随人物高度）- 暂时禁用
        /*
        if (planarShadow != null) {
            planarShadow.update(tpf, position.y);
        }
        */

        // 统一billboard会让所有部件始终面向摄像机，始终显示同一个面
    }

    /**
     * 设置初始方向（用于初始化）
     */
    private void setInitialDirection(String direction) {
        if (puppetRenderer == null || puppetRenderer.getSkeleton() == null) {
            return;
        }

        currentDirection = direction;
        com.Hecate.puppet.core.Skeleton skeleton = puppetRenderer.getSkeleton();

        for (com.Hecate.puppet.core.Bone bone : skeleton.getAllBones()) {
            bone.setCurrentDirection(direction);
            com.Hecate.puppet.core.PuppetPartRenderer partRenderer =
                puppetRenderer.getPartRenderer(bone.getName());
            if (partRenderer != null) {
                partRenderer.updateTextureFromBone();
            }
        }
    }

    /**
     * 设置固定方向（用于圆盘模式）
     */
    private void setFixedDirection(String direction) {
        if (puppetRenderer == null || puppetRenderer.getSkeleton() == null) {
            return;
        }

        if (direction.equals(currentDirection)) {
            return;  // 方向没有改变，不需要更新
        }

        currentDirection = direction;
        com.Hecate.puppet.core.Skeleton skeleton = puppetRenderer.getSkeleton();

        for (com.Hecate.puppet.core.Bone bone : skeleton.getAllBones()) {
            bone.setCurrentDirection(direction);
            com.Hecate.puppet.core.PuppetPartRenderer partRenderer =
                puppetRenderer.getPartRenderer(bone.getName());
            if (partRenderer != null) {
                partRenderer.updateTextureFromBone();
            }
        }
    }

    /**
     * 切换到普通模式（按住Ctrl时调用）
     */
    public void setNormalMode(boolean enabled) {
        if (isNormalMode != enabled) {
            isNormalMode = enabled;

            if (!enabled) {
                // 切换回圆盘模式时，立即设置为back方向
                setFixedDirection("back");
            }
        }
    }

    /**
     * 根据摄像机相对于玩家的位置更新puppet的方向贴图
     * 【最终方案】使用局部坐标系投影法，避免世界角度相减的混乱
     * 核心思路：将相机方向投影到玩家的局部forward/right向量，用atan2得到局部角
     */
    private void updatePuppetDirection() {
        if (puppetRenderer == null || puppetRenderer.getSkeleton() == null) {
            return;
        }

        // 获取相机位置和玩家位置
        Vector3f camPos = app.getCamera().getLocation();
        Vector3f playerPos = position.clone();

        // 相机在玩家周围的方位：玩家 -> 相机
        Vector3f toCamera = camPos.subtract(playerPos);
        toCamera.y = 0;  // 清零y分量，只考虑水平方向

        if (toCamera.lengthSquared() < 0.0001f) {
            return;  // 相机和玩家重合
        }

        toCamera.normalizeLocal();

        // 玩家真实朝向（使用charYaw，已归一化）
        float yaw = charYaw;

        // 玩家局部基向量
        // forward: 玩家面朝方向
        // right: 玩家右手方向
        Vector3f forward = new Vector3f((float)Math.sin(yaw), 0, (float)Math.cos(yaw));
        Vector3f right   = new Vector3f((float)Math.cos(yaw), 0, -(float)Math.sin(yaw));

        // 投影到玩家局部坐标：相机在玩家局部坐标系中的位置
        float forwardDot = toCamera.dot(forward);  // 正值=相机在前方，负值=在后方
        float rightDot   = toCamera.dot(right);    // 正值=相机在右侧，负值=在左侧

        // 关键：局部角 theta ∈ (-π, π]
        // theta=0°表示相机在玩家正前方，+90°表示在右侧，-90°表示在左侧，±180°表示在后方
        float theta = (float)Math.atan2(rightDot, forwardDot);
        float deg = (float)Math.toDegrees(theta);

        // 更新cameraYaw用于变化检测（使用世界角，不是局部角theta）
        // cameraYaw 永远表示"玩家→相机的世界角"
        cameraYaw = (float)Math.atan2(toCamera.x, toCamera.z);
        cameraYaw = normalizeSignedAngle(cameraYaw);

        // 分四象限（语义："相机在玩家右侧 → 显示right面"）
        // -45° 到 +45°：相机在玩家前方 → 显示front
        // +45° 到 +135°：相机在玩家右侧 → 显示right
        // -135° 到 -45°：相机在玩家左侧 → 显示left
        // +135° 到 +180° 或 -180° 到 -135°：相机在玩家后方 → 显示back
        String direction;

        if (deg >= -45 && deg < 45) {
            direction = "front";

        } else if (deg >= 45 && deg < 135) {
            direction = "right";  // 【修复】相机在右侧 → 显示right（之前错误地显示left）

        } else if (deg >= -135 && deg < -45) {
            direction = "left";   // 【修复】相机在左侧 → 显示left（之前错误地显示right）

        } else {
            direction = "back";

        }

        // 如果方向没有改变，不需要更新
        if (direction.equals(currentDirection)) {

            return;
        }

        currentDirection = direction;

        // 更新所有骨骼的当前方向
        com.Hecate.puppet.core.Skeleton skeleton = puppetRenderer.getSkeleton();
        int boneCount = 0;
        int successCount = 0;

        for (com.Hecate.puppet.core.Bone bone : skeleton.getAllBones()) {
            boneCount++;

            // 直接设置方向，不做任何镜像处理
            bone.setCurrentDirection(direction);

            // 获取部件渲染器并更新贴图
            com.Hecate.puppet.core.PuppetPartRenderer partRenderer =
                puppetRenderer.getPartRenderer(bone.getName());
            if (partRenderer != null) {
                partRenderer.updateTextureFromBone();
                successCount++;
            }
        }

    }

    /**
     * 将角度归一化到 -π 到 +π 范围（-180° 到 +180°）
     */
    private float normalizeSignedAngle(float angle) {
        while (angle > Math.PI) {
            angle -= 2 * Math.PI;
        }
        while (angle < -Math.PI) {
            angle += 2 * Math.PI;
        }
        return angle;
    }

    /**
     * 计算两个角度之间的最短角差（避免跨边界时的巨大误差）
     */
    private float deltaAngle(float a, float b) {
        float d = a - b;
        while (d > Math.PI) {
            d -= 2f * (float)Math.PI;
        }
        while (d < -Math.PI) {
            d += 2f * (float)Math.PI;
        }
        return d;
    }

    /**
     * 设置行走状态
     */
    public void setWalking(boolean walking) {
        if (walking != isWalking) {
            isWalking = walking;

            if (walking && !isJumping) {
                // 播放走路动画
                animationPlayer.play(walkAnimation);

            } else if (!walking && !isJumping) {
                // 停止动画
                animationPlayer.stop();

            }
        }
    }

    /**
     * 触发跳跃
     */
    public void jump() {
        if (!isJumping) {
            isJumping = true;

            // 播放跳跃动画
            animationPlayer.play(jumpAnimation);

            // 1秒后结束跳跃状态
            app.enqueue(() -> {
                new Thread(() -> {
                    try {
                        Thread.sleep(1000);
                        isJumping = false;

                        // 如果还在走路，恢复走路动画
                        if (isWalking) {
                            animationPlayer.play(walkAnimation);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();
                return null;
            });
        }
    }

    /**
     * 设置玩家位置
     */
    public void setPosition(Vector3f position) {
        this.position.set(position);
    }

    /**
     * 获取玩家位置
     */
    public Vector3f getPosition() {
        return position.clone();
    }

    /**
     * 设置玩家旋转（弧度）
     */
    public void setRotation(float rotation) {
        float oldRotation = this.rotation;
        this.rotation = normalizeSignedAngle(rotation);

        // 只在旋转真正改变时输出日志
        if (Math.abs(this.rotation - oldRotation) > 0.01f) {
        }
    }

    /**
     * 获取玩家旋转（弧度）
     */
    public float getRotation() {
        return rotation;
    }

    /**
     * 设置世界节点（用于阴影射线检测）
     */
    public void setWorldNode(Node worldNode) {
        if (planarShadow != null) {
            planarShadow.setWorldNode(worldNode);
        }
    }

    /**
     * 设置网格管理器（用于速度倍率计算）
     */
    public void setGridManager(SparseGridManager gridManager) {
        this.gridManager = gridManager;
    }

    /**
     * 设置玩家阵营
     * @param factionId 阵营ID
     */
    public void setPlayerFactionId(int factionId) {
        this.playerFactionId = factionId;
    }

    /**
     * 获取玩家阵营ID
     */
    public int getPlayerFactionId() {
        return playerFactionId;
    }

    // 向后兼容方法（旧API）
    @Deprecated
    public void setPlayerTeam(int team) {
        // 将旧的 team (0/1) 映射到新的 factionId
        this.playerFactionId = (team == 0)
            ? com.Hecate.ink.FactionRegistry.LIGHT_DEFAULT
            : com.Hecate.ink.FactionRegistry.DARK_DEFAULT;
    }

    @Deprecated
    public int getPlayerTeam() {
        // 将 factionId 映射回 team (0/1)
        return (playerFactionId == com.Hecate.ink.FactionRegistry.LIGHT_DEFAULT) ? 0 : 1;
    }

    /**
     * 获取当前位置的移动速度倍率
     * 根据脚下网格状态返回速度倍率：
     * - 敌方点燃/涂墨: 0.3x（极慢）
     * - 普通地面: 1.0x（正常）
     * - 己方涂墨: 1.6x（快速）
     * - 己方点燃: 2.0x（极快）
     *
     * @return 速度倍率（0.3 ~ 2.0）
     */
    public float getSpeedMultiplier() {
        if (gridManager == null) {
            return 1.0f;  // 没有网格管理器，返回正常速度
        }

        // 获取玩家脚下的网格单元
        GridCell cell = gridManager.getCellAt(position);

        if (cell == null || cell.isEmpty()) {
            return 1.0f;  // 空地面，正常速度
        }

        // 使用GridCell的getSpeedMultiplier方法
        float multiplier = cell.getSpeedMultiplier(playerFactionId, gridManager.getFactionRegistry());

        return multiplier;
    }

    /**
     * 清理资源
     */
    public void cleanup() {
        if (planarShadow != null) {
            planarShadow.cleanup();
        }
        if (puppetNode != null) {
            puppetNode.removeFromParent();
        }
    }
}
