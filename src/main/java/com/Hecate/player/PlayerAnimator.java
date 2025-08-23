package com.Hecate.player;

import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;

/**
 * 玩家模型动画控制器
 * 实现Minecraft风格的玩家动画
 */
public class PlayerAnimator {
    
    private final PlayerModel playerModel;
    
    // 动画状态
    public enum AnimationState {
        IDLE,       // 待机
        WALKING,    // 行走
        RUNNING,    // 奔跑
        JUMPING     // 跳跃
    }
    
    private AnimationState currentState = AnimationState.IDLE;
    private float animationTime = 0f;
    private boolean isMoving = false;
    private float movementSpeed = 0f;
    
    // 动画参数
    private static final float WALK_ARM_SWING = 30f * FastMath.DEG_TO_RAD;  // 手臂摆动角度（更自然）
    private static final float WALK_LEG_SWING = 35f * FastMath.DEG_TO_RAD;  // 腿部摆动角度（更自然）
    private static final float WALK_FREQUENCY = 3.5f;                       // 行走频率（稍慢）
    private static final float RUN_FREQUENCY = 5.5f;                        // 奔跑频率
    private static final float IDLE_FREQUENCY = 0.8f;                       // 待机呼吸频率（更慢）
    private static final float IDLE_AMPLITUDE = 1.5f * FastMath.DEG_TO_RAD; // 待机呼吸幅度（更细微）
    private static final float BODY_SWAY = 3f * FastMath.DEG_TO_RAD;        // 身体左右摇摆幅度
    
    // 当前旋转状态（用于平滑过渡）
    private float leftArmRotation = 0f;
    private float rightArmRotation = 0f;
    private float leftLegRotation = 0f;
    private float rightLegRotation = 0f;
    private float headBobbing = 0f;
    private float bodySway = 0f;
    
    // 眨眼动画
    private float blinkTimer = 0f;
    private float nextBlinkTime = 3f; // 3秒后眨眼
    private boolean isBlinking = false;
    private float blinkDuration = 0.15f; // 眨眼持续时间
    
    public PlayerAnimator(PlayerModel playerModel) {
        this.playerModel = playerModel;
        System.out.println("🎬 玩家动画系统初始化完成");
    }
    
    /**
     * 更新动画
     */
    public void update(float tpf, Vector3f velocity, boolean isPlayerMoving) {
        animationTime += tpf;
        
        // 直接使用传入的移动状态（更准确）
        isMoving = isPlayerMoving;
        
        // 设置一个默认的移动速度用于动画计算
        movementSpeed = isMoving ? 3.0f : 0.0f;
        
        // 确定当前动画状态
        updateAnimationState(velocity);
        
        // 根据状态执行相应动画
        switch (currentState) {
            case IDLE:
                animateIdle(tpf);
                break;
            case WALKING:
                animateWalking(tpf);
                break;
            case RUNNING:
                animateRunning(tpf);
                break;
            case JUMPING:
                animateJumping(tpf);
                break;
        }
        
        // 应用旋转到模型
        applyRotations();
    }
    
    /**
     * 更新动画状态
     */
    private void updateAnimationState(Vector3f velocity) {
        if (velocity.y > 0.5f) {
            // 跳跃状态
            currentState = AnimationState.JUMPING;
        } else if (isMoving) {
            // 移动状态
            if (movementSpeed > 3.0f) {
                currentState = AnimationState.RUNNING;
            } else {
                currentState = AnimationState.WALKING;
            }
        } else {
            // 待机状态
            currentState = AnimationState.IDLE;
        }
    }
    
    /**
     * 待机动画（轻微的呼吸效果）
     */
    private void animateIdle(float tpf) {
        float breathingCycle = FastMath.sin(animationTime * IDLE_FREQUENCY);
        
        // 轻微的头部上下移动（呼吸效果）
        headBobbing = breathingCycle * IDLE_AMPLITUDE;
        
        // 手臂和腿回到自然位置
        leftArmRotation = lerp(leftArmRotation, 0, tpf * 3f);
        rightArmRotation = lerp(rightArmRotation, 0, tpf * 3f);
        leftLegRotation = lerp(leftLegRotation, 0, tpf * 3f);
        rightLegRotation = lerp(rightLegRotation, 0, tpf * 3f);
        
        // 身体摇摆也回到中性位置
        bodySway = lerp(bodySway, 0, tpf * 4f);
        
        // 更新眨眼动画
        updateBlinkAnimation(tpf);
    }
    
    /**
     * 更新眨眼动画
     */
    private void updateBlinkAnimation(float tpf) {
        blinkTimer += tpf;
        
        if (!isBlinking && blinkTimer >= nextBlinkTime) {
            // 开始眨眼
            isBlinking = true;
            blinkTimer = 0f;
        } else if (isBlinking && blinkTimer >= blinkDuration) {
            // 眨眼结束
            isBlinking = false;
            blinkTimer = 0f;
            nextBlinkTime = 2f + FastMath.nextRandomFloat() * 4f; // 2-6秒随机间隔
        }
        
        // 这里可以后续实现眼睑缩放效果
    }
    
    /**
     * 行走动画
     */
    private void animateWalking(float tpf) {
        float walkCycle = FastMath.sin(animationTime * WALK_FREQUENCY);
        float walkCosine = FastMath.cos(animationTime * WALK_FREQUENCY);
        
        // 手臂摆动（与腿相反）
        leftArmRotation = walkCycle * WALK_ARM_SWING * 0.7f;
        rightArmRotation = -walkCycle * WALK_ARM_SWING * 0.7f;
        
        // 腿部摆动
        leftLegRotation = -walkCycle * WALK_LEG_SWING;
        rightLegRotation = walkCycle * WALK_LEG_SWING;
        
        // 轻微的头部上下摆动
        headBobbing = FastMath.abs(walkCosine) * IDLE_AMPLITUDE * 0.6f;
        
        // 身体左右轻微摇摆
        bodySway = walkCycle * BODY_SWAY * 0.3f;
    }
    
    /**
     * 奔跑动画
     */
    private void animateRunning(float tpf) {
        float runCycle = FastMath.sin(animationTime * RUN_FREQUENCY);
        float runCosine = FastMath.cos(animationTime * RUN_FREQUENCY);
        
        // 更大幅度的手臂摆动
        leftArmRotation = runCycle * WALK_ARM_SWING * 1.3f;
        rightArmRotation = -runCycle * WALK_ARM_SWING * 1.3f;
        
        // 更大幅度的腿部摆动
        leftLegRotation = -runCycle * WALK_LEG_SWING * 1.4f;
        rightLegRotation = runCycle * WALK_LEG_SWING * 1.4f;
        
        // 更明显的头部上下摆动
        headBobbing = FastMath.abs(runCosine) * IDLE_AMPLITUDE * 1.2f;
        
        // 更大的身体摇摆
        bodySway = runCycle * BODY_SWAY * 0.5f;
    }
    
    /**
     * 跳跃动画
     */
    private void animateJumping(float tpf) {
        // 手臂向上举起
        leftArmRotation = lerp(leftArmRotation, -45f * FastMath.DEG_TO_RAD, tpf * 6f);
        rightArmRotation = lerp(rightArmRotation, -45f * FastMath.DEG_TO_RAD, tpf * 6f);
        
        // 腿部略微弯曲
        leftLegRotation = lerp(leftLegRotation, 15f * FastMath.DEG_TO_RAD, tpf * 6f);
        rightLegRotation = lerp(rightLegRotation, 15f * FastMath.DEG_TO_RAD, tpf * 6f);
        
        // 头部保持稳定
        headBobbing = lerp(headBobbing, 0, tpf * 4f);
        
        // 身体前倾
        bodySway = lerp(bodySway, 5f * FastMath.DEG_TO_RAD, tpf * 5f);
    }
    
    /**
     * 应用旋转到模型各部位
     */
    private void applyRotations() {
        // 头部旋转（上下摆动）
        Quaternion headRotation = new Quaternion();
        headRotation.fromAngleAxis(headBobbing, Vector3f.UNIT_X);
        playerModel.getHeadNode().setLocalRotation(headRotation);
        
        // 身体旋转（左右轻微摇摆）
        Quaternion bodyRotation = new Quaternion();
        bodyRotation.fromAngleAxis(bodySway, Vector3f.UNIT_Z);
        playerModel.getBodyNode().setLocalRotation(bodyRotation);
        
        // 左臂旋转（绕X轴）
        Quaternion leftArmRot = new Quaternion();
        leftArmRot.fromAngleAxis(leftArmRotation, Vector3f.UNIT_X);
        playerModel.getLeftArmNode().setLocalRotation(leftArmRot);
        
        // 右臂旋转（绕X轴）
        Quaternion rightArmRot = new Quaternion();
        rightArmRot.fromAngleAxis(rightArmRotation, Vector3f.UNIT_X);
        playerModel.getRightArmNode().setLocalRotation(rightArmRot);
        
        // 左腿旋转（绕X轴）
        Quaternion leftLegRot = new Quaternion();
        leftLegRot.fromAngleAxis(leftLegRotation, Vector3f.UNIT_X);
        playerModel.getLeftLegNode().setLocalRotation(leftLegRot);
        
        // 右腿旋转（绕X轴）
        Quaternion rightLegRot = new Quaternion();
        rightLegRot.fromAngleAxis(rightLegRotation, Vector3f.UNIT_X);
        playerModel.getRightLegNode().setLocalRotation(rightLegRot);
    }
    
    /**
     * 线性插值辅助函数
     */
    private float lerp(float a, float b, float t) {
        return a + (b - a) * FastMath.clamp(t, 0f, 1f);
    }
    
    /**
     * 重置动画时间（用于状态切换时的平滑过渡）
     */
    public void resetAnimationTime() {
        animationTime = 0f;
    }
    
    /**
     * 获取当前动画状态
     */
    public AnimationState getCurrentState() {
        return currentState;
    }
    
    /**
     * 强制设置动画状态
     */
    public void setAnimationState(AnimationState state) {
        if (this.currentState != state) {
            this.currentState = state;
            resetAnimationTime();
            System.out.println("🎬 动画状态切换到: " + state);
        }
    }
}