package com.Hecate.player;

import com.jme3.app.SimpleApplication;
import com.jme3.scene.Node;
import com.jme3.math.Vector3f;
import com.Hecate.utils.LogUtils;
import java.util.List;

/**
 * 玩家2D精灵控制器
 * 统一管理玩家的2D精灵显示、动画和渲染
 */
public class Player2DSpriteController {

    // 核心组件
    private final SimpleApplication app;
    private final Node playerNode;

    // 管理器组件
    private PlayerSpriteManager spriteManager;
    private SpriteAnimationSystem animationSystem;
    private DirectionalSpriteRenderer renderer;

    // 当前状态
    private AnimationState currentState;
    private Vector3f playerPosition;
    private Vector3f facingDirection;

    // 配置
    private boolean isEnabled = true;
    private boolean debugMode = false;

    public Player2DSpriteController(SimpleApplication app, Node playerNode) {
        this.app = app;
        this.playerNode = playerNode;
        this.currentState = AnimationState.IDLE;
        this.playerPosition = new Vector3f();
        this.facingDirection = new Vector3f(1, 0, 0); // 默认朝右

        initializeComponents();
        setupDefaultAnimations();
    }

    /**
     * 初始化所有组件
     */
    private void initializeComponents() {
        // 创建精灵资源管理器
        spriteManager = new PlayerSpriteManager(app);

        // 创建动画系统
        animationSystem = new SpriteAnimationSystem();

        // 创建渲染器
        renderer = new DirectionalSpriteRenderer(app, playerNode);
    }

    /**
     * 设置默认动画
     */
    private void setupDefaultAnimations() {
        // 加载标准动画
        spriteManager.loadStandardPlayerAnimations();

        // 建立方向感知的动画映射
        for (String animName : spriteManager.getLoadedAnimationNames()) {
            List<AnimationFrame> frames = spriteManager.getAnimationSequence(animName);
            if (frames != null && !frames.isEmpty()) {
                // 直接使用完整名称注册动画
                animationSystem.addAnimationFromFrames(animName, frames, true);
            }
        }

        // 启动默认的前方闲置动画
        animationSystem.playAnimation("back_idle");
    }

    /**
     * 更新控制器
     */
    public void update(float tpf) {
        if (!isEnabled) {
            return;
        }

        // 更新动画系统
        animationSystem.update(tpf, currentState);

        // 获取当前帧
        AnimationFrame currentFrame = animationSystem.getCurrentFrame();
        if (currentFrame != null) {
            renderer.setCurrentFrame(currentFrame);
        }

        // 更新渲染器
        renderer.update(tpf, playerPosition, facingDirection);

        // 调试信息
        if (debugMode) {
            printDebugInfo();
        }
    }

    /**
     * 设置玩家状态
     */
    public void setPlayerState(AnimationState newState) {
        if (newState != currentState) {
            AnimationState oldState = currentState;
            currentState = newState;
            LogUtils.debug(Player2DSpriteController.class,
                    "玩家状态变化: " + oldState + " -> " + newState);
        }
    }

    /**
     * 设置玩家位置
     */
    public void setPlayerPosition(Vector3f position) {
        this.playerPosition.set(position);
    }

    /**
     * 设置朝向方向
     */
    public void setFacingDirection(Vector3f direction) {
        this.facingDirection.set(direction.normalize());
    }

    /**
     * 手动播放动画
     */
    public boolean playAnimation(String animationName) {
        return animationSystem.playAnimation(animationName);
    }

    /**
     * 设置精灵缩放
     */
    public void setSpriteScale(float scale) {
        renderer.updateScale(scale);
    }

    /**
     * 设置精灵基础尺寸
     */
    public void setSpriteBaseSize(float width, float height) {
        renderer.setBaseSize(width, height);
    }

    /**
     * 设置精灵透明度
     */
    public void setSpriteAlpha(float alpha) {
        renderer.setAlpha(alpha);
    }

    /**
     * 设置精灵可见性
     */
    public void setSpriteVisible(boolean visible) {
        renderer.setVisible(visible);
    }

    /**
     * 启用/禁用方向翻转
     */
    public void setDirectionalFlipEnabled(boolean enabled) {
        renderer.setDirectionalFlipEnabled(enabled);
    }

    /**
     * 启用/禁用自动动画切换
     */
    public void setAutoAnimationSwitchEnabled(boolean enabled) {
        animationSystem.setAutoSwitchEnabled(enabled);
    }

    /**
     * 启用/禁用整个系统
     */
    public void setEnabled(boolean enabled) {
        this.isEnabled = enabled;
        setSpriteVisible(enabled);
    }

    /**
     * 启用/禁用调试模式
     */
    public void setDebugMode(boolean debug) {
        this.debugMode = debug;
    }

    /**
     * 加载自定义动画
     */
    public boolean loadCustomAnimation(String animationName, String folderPath, int frameCount) {
        boolean success = spriteManager.loadAnimationSequence(animationName, folderPath, frameCount);
        if (success) {
            List<AnimationFrame> frames = spriteManager.getAnimationSequence(animationName);
            if (frames != null) {
                animationSystem.addAnimationFromFrames(animationName, frames, true);
            }
        }
        return success;
    }

    /**
     * 设置状态到动画的映射
     */
    public void mapStateToAnimation(AnimationState state, String animationName) {
        animationSystem.setStateMapping(state, animationName);
    }

    // Getter方法
    public AnimationState getCurrentState() {
        return currentState;
    }

    public Vector3f getPlayerPosition() {
        return playerPosition.clone();
    }

    public Vector3f getFacingDirection() {
        return facingDirection.clone();
    }

    public String getCurrentAnimationName() {
        return animationSystem.getCurrentAnimationName();
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public boolean isDebugMode() {
        return debugMode;
    }

    /**
     * 获取系统状态
     */
    public String getSystemStatus() {
        return String.format("2D精灵控制器 - 状态: %s, 动画: %s, 位置: (%.1f,%.1f,%.1f), 启用: %s",
                currentState,
                getCurrentAnimationName(),
                playerPosition.x, playerPosition.y, playerPosition.z,
                isEnabled);
    }

    private int debugCounter = 0;

    private void printDebugInfo() {
        if (++debugCounter % 60 == 0) { // 每秒打印一次（假设60FPS）
            LogUtils.debug(Player2DSpriteController.class, getSystemStatus());
            LogUtils.debug(Player2DSpriteController.class, renderer.getRendererStatus());
            LogUtils.debug(Player2DSpriteController.class, animationSystem.getSystemStatus());
        }
    }

    /**
     * 清理资源
     */
    public void cleanup() {
        if (renderer != null) {
            renderer.cleanup();
        }

        if (animationSystem != null) {
            animationSystem.cleanup();
        }

        if (spriteManager != null) {
            spriteManager.cleanup();
        }
    }
}
