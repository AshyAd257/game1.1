package com.Hecate.player;

import java.util.Random;

/**
 * 高级动画控制器 - 处理复杂的动画逻辑
 * 包括变速动画、随机眨眼、跳跃状态保持等
 */
public class AdvancedAnimationController {

    // 动画状态枚举
    public enum AnimationState {
        IDLE,           // 站立
        WALKING,        // 走路
        RUNNING,        // 跑步
        JUMPING,        // 跳跃
        BLINKING        // 眨眼
    }

    // 眨眼状态枚举
    public enum BlinkState {
        NOT_BLINKING,   // 不在眨眼
        FIRST_BLINK,    // 第一次眨眼 (3帧/秒)
        SECOND_BLINK,   // 第二次眨眼 (6帧/秒)
        THIRD_BLINK     // 第三次眨眼 (6帧/秒)
    }

    // 动画帧率常量
    private static final float WALK_FPS = 5.0f;
    private static final float RUN_FPS = 7.0f;
    private static final float JUMP_FPS = 5.0f;
    private static final float BLINK_FIRST_FPS = 3.0f;
    private static final float BLINK_FAST_FPS = 6.0f;

    // 眨眼随机参数
    private static final float MIN_BLINK_INTERVAL = 3.0f;  // 最小眨眼间隔(秒)
    private static final float MAX_BLINK_INTERVAL = 8.0f;  // 最大眨眼间隔(秒)

    // 状态变量
    private AnimationState currentState = AnimationState.IDLE;
    private BlinkState blinkState = BlinkState.NOT_BLINKING;
    private String currentDirection = "front";

    // 时间控制
    private float animationTime = 0.0f;
    private float blinkTimer = 0.0f;
    private float nextBlinkTime;
    private int currentFrame = 0;
    private int blinkCycleCount = 0;

    // 跳跃状态
    private boolean isGrounded = true;
    private boolean jumpAnimationStarted = false;

    // 随机数生成器
    private Random random = new Random();

    // 精灵管理器引用
    private PlayerSpriteManager spriteManager;

    public AdvancedAnimationController(PlayerSpriteManager spriteManager) {
        this.spriteManager = spriteManager;
        scheduleNextBlink();
    }

    /**
     * 更新动画系统
     */
    public void update(float deltaTime, PlayerState playerState) {
        // 更新基础动画时间
        animationTime += deltaTime;

        // 更新眨眼计时器
        updateBlinkSystem(deltaTime, playerState);

        // 根据玩家状态确定动画
        determineAnimationState(playerState);

        // 更新当前动画帧
        updateCurrentFrame();

        // 应用动画到精灵管理器
        applyAnimation();
    }

    /**
     * 更新眨眼系统
     */
    private void updateBlinkSystem(float deltaTime, PlayerState playerState) {
        // 只有在站立且面向前方时才能眨眼
        if (playerState.isIdle() && "front".equals(currentDirection)) {
            blinkTimer += deltaTime;

            // 检查是否到了眨眼时间
            if (blinkState == BlinkState.NOT_BLINKING && blinkTimer >= nextBlinkTime) {
                startBlinkSequence();
            }

            // 处理眨眼序列
            if (blinkState != BlinkState.NOT_BLINKING) {
                updateBlinkSequence();
            }
        } else {
            // 非站立状态时重置眨眼
            if (blinkState != BlinkState.NOT_BLINKING) {
                resetBlinkState();
            }
        }
    }

    /**
     * 开始眨眼序列
     */
    private void startBlinkSequence() {
        blinkState = BlinkState.FIRST_BLINK;
        currentState = AnimationState.BLINKING;
        blinkCycleCount = 0;
        animationTime = 0.0f;
        currentFrame = 0;

        System.out.println("开始眨眼序列 - 第一次眨眼 (3帧/秒)");
    }

    /**
     * 更新眨眼序列
     */
    private void updateBlinkSequence() {
        float currentFPS = (blinkState == BlinkState.FIRST_BLINK) ?
                BLINK_FIRST_FPS : BLINK_FAST_FPS;

        // 检查是否完成当前眨眼循环
        if (hasCompletedBlinkCycle(currentFPS)) {
            blinkCycleCount++;
            animationTime = 0.0f;
            currentFrame = 0;

            switch (blinkState) {
                case FIRST_BLINK:
                    blinkState = BlinkState.SECOND_BLINK;
                    System.out.println("第二次眨眼 (6帧/秒)");
                    break;
                case SECOND_BLINK:
                    blinkState = BlinkState.THIRD_BLINK;
                    System.out.println("第三次眨眼 (6帧/秒)");
                    break;
                case THIRD_BLINK:
                    // 完成所有眨眼，回到正常状态
                    resetBlinkState();
                    scheduleNextBlink();
                    System.out.println("眨眼序列完成，安排下次眨眼");
                    break;
            }
        }
    }

    /**
     * 检查是否完成了一个眨眼循环
     */
    private boolean hasCompletedBlinkCycle(float fps) {
        // 眨眼动画有2帧 (idle_01.png, idle_02.png)
        int totalFrames = 2;
        float cycleDuration = totalFrames / fps;
        return animationTime >= cycleDuration;
    }

    /**
     * 重置眨眼状态
     */
    private void resetBlinkState() {
        blinkState = BlinkState.NOT_BLINKING;
        if (currentState == AnimationState.BLINKING) {
            currentState = AnimationState.IDLE;
        }
        blinkCycleCount = 0;
    }

    /**
     * 安排下次眨眼时间
     */
    private void scheduleNextBlink() {
        nextBlinkTime = MIN_BLINK_INTERVAL +
                random.nextFloat() * (MAX_BLINK_INTERVAL - MIN_BLINK_INTERVAL);
        blinkTimer = 0.0f;

        System.out.println("下次眨眼将在 " + String.format("%.1f", nextBlinkTime) + " 秒后");
    }

    /**
     * 根据玩家状态确定动画状态
     */
    private void determineAnimationState(PlayerState playerState) {
        // 如果正在眨眼，不改变状态
        if (blinkState != BlinkState.NOT_BLINKING) {
            return;
        }

        // 更新方向
        currentDirection = playerState.getDirection();

        // 跳跃状态处理
        if (!playerState.isGrounded()) {
            if (!jumpAnimationStarted) {
                currentState = AnimationState.JUMPING;
                jumpAnimationStarted = true;
                animationTime = 0.0f;
                currentFrame = 0;
                System.out.println("开始跳跃动画");
            }
            // 跳跃中保持当前状态
            return;
        } else {
            // 着陆了
            if (jumpAnimationStarted) {
                jumpAnimationStarted = false;
                animationTime = 0.0f;
                currentFrame = 0;
                System.out.println("着陆，重置动画");
            }
        }

        // 地面移动状态
        if (playerState.isMoving()) {
            AnimationState newState = playerState.isRunning() ?
                    AnimationState.RUNNING : AnimationState.WALKING;

            if (currentState != newState) {
                currentState = newState;
                animationTime = 0.0f;
                currentFrame = 0;
                System.out.println("切换到 " + (newState == AnimationState.RUNNING ? "跑步" : "走路"));
            }
        } else {
            if (currentState != AnimationState.IDLE) {
                currentState = AnimationState.IDLE;
                animationTime = 0.0f;
                currentFrame = 0;
                System.out.println("切换到站立");
            }
        }
    }

    /**
     * 更新当前动画帧
     */
    private void updateCurrentFrame() {
        float fps = getCurrentFPS();
        int totalFrames = getTotalFrames();

        if (totalFrames <= 1) {
            currentFrame = 0;
            return;
        }

        // 特殊处理跳跃动画
        if (currentState == AnimationState.JUMPING && !isGrounded) {
            // 计算应该在第几帧
            int calculatedFrame = (int)(animationTime * fps) % totalFrames;

            // 如果超过了jump_02 (第2帧，索引1)，就停在jump_02
            if (calculatedFrame >= 2) {
                currentFrame = 1; // jump_02.png
            } else {
                currentFrame = calculatedFrame;
            }
        } else {
            // 正常循环动画
            currentFrame = (int)(animationTime * fps) % totalFrames;
        }
    }

    /**
     * 获取当前动画的FPS
     */
    private float getCurrentFPS() {
        switch (currentState) {
            case WALKING:
                return WALK_FPS;
            case RUNNING:
                return RUN_FPS;
            case JUMPING:
                return JUMP_FPS;
            case BLINKING:
                return (blinkState == BlinkState.FIRST_BLINK) ?
                        BLINK_FIRST_FPS : BLINK_FAST_FPS;
            case IDLE:
            default:
                return 1.0f; // 站立动画很慢
        }
    }

    /**
     * 获取当前动画的总帧数
     */
    private int getTotalFrames() {
        String animationType = getAnimationType();
        return spriteManager.getFrameCount(currentDirection, animationType);
    }

    /**
     * 获取动画类型字符串
     */
    private String getAnimationType() {
        switch (currentState) {
            case WALKING:
            case RUNNING:
                return "run";
            case JUMPING:
                return "jump";
            case BLINKING:
            case IDLE:
            default:
                return "idle";
        }
    }

    /**
     * 应用动画到精灵管理器
     */
    private void applyAnimation() {
        String animationType = getAnimationType();
        spriteManager.setCurrentAnimation(currentDirection, animationType, currentFrame);
    }

    /**
     * 设置玩家是否在地面上
     */
    public void setGrounded(boolean grounded) {
        this.isGrounded = grounded;
    }

    /**
     * 强制触发眨眼（用于测试）
     */
    public void triggerBlink() {
        if (blinkState == BlinkState.NOT_BLINKING) {
            startBlinkSequence();
        }
    }

    // Getters
    public AnimationState getCurrentState() { return currentState; }
    public BlinkState getBlinkState() { return blinkState; }
    public int getCurrentFrame() { return currentFrame; }
    public String getCurrentDirection() { return currentDirection; }
}
