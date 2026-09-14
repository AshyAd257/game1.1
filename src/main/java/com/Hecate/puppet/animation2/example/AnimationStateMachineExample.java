package com.Hecate.puppet.animation2.example;

import com.Hecate.puppet.animation.AnimationClip;
import com.Hecate.puppet.animation.Keyframe;
import com.Hecate.puppet.animation2.*;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;

/**
 * 动画状态机使用示例
 * 演示如何使用新的动画系统
 */
public class AnimationStateMachineExample {

    public static void main(String[] args) {
        java.lang.System.out.println("=== Animation State Machine Example ===\n");

        // 1. 创建动画片段
        AnimationClip idleClip = createIdleAnimation();
        AnimationClip walkClip = createWalkAnimation();
        AnimationClip runClip = createRunAnimation();
        AnimationClip jumpClip = createJumpAnimation();

        // 2. 创建动画状态
        AnimationState idleState = new AnimationState("Idle", idleClip);
        AnimationState walkState = new AnimationState("Walk", walkClip);
        AnimationState runState = new AnimationState("Run", runClip);
        AnimationState jumpState = new AnimationState("Jump", jumpClip);
        jumpState.setLooping(false); // 跳跃不循环

        // 3. 添加动画事件
        walkState.addEvent(0.25f, "footstep_left");
        walkState.addEvent(0.75f, "footstep_right");
        runState.addEvent(0.2f, "footstep_left");
        runState.addEvent(0.6f, "footstep_right");
        jumpState.addEvent(0.5f, "jump_peak");

        // 4. 创建状态机
        AnimationStateMachine stateMachine = new AnimationStateMachine();
        stateMachine.addState(idleState);
        stateMachine.addState(walkState);
        stateMachine.addState(runState);
        stateMachine.addState(jumpState);
        stateMachine.setDefaultState("Idle");

        // 5. 设置状态转换规则
        setupTransitions(stateMachine);

        // 6. 添加事件监听器
        stateMachine.addEventListener("footstep_left", event -> {
            java.lang.System.out.println("  [Event] Left foot down at " + event.getTime() + "s");
        });
        stateMachine.addEventListener("footstep_right", event -> {
            java.lang.System.out.println("  [Event] Right foot down at " + event.getTime() + "s");
        });
        stateMachine.addEventListener("jump_peak", event -> {
            java.lang.System.out.println("  [Event] Jump peak reached!");
        });

        // 7. 设置转换回调
        stateMachine.setOnTransitionCallback(transition -> {
            java.lang.System.out.println("  [Transition] " + transition);
        });

        // 8. 模拟游戏循环
        java.lang.System.out.println("Starting animation simulation...\n");

        PlayerState player = new PlayerState();
        float tpf = 0.016f; // 60 FPS
        int frameCount = 0;

        for (int i = 0; i < 600; i++) { // 模拟10秒
            frameCount++;

            // 模拟玩家输入
            if (i == 60) {
                player.startWalking();
                java.lang.System.out.println("\n[Frame " + frameCount + "] Player starts walking");
            } else if (i == 180) {
                player.startRunning();
                java.lang.System.out.println("\n[Frame " + frameCount + "] Player starts running");
            } else if (i == 300) {
                player.jump();
                java.lang.System.out.println("\n[Frame " + frameCount + "] Player jumps");
            } else if (i == 420) {
                player.stopMoving();
                java.lang.System.out.println("\n[Frame " + frameCount + "] Player stops moving");
            }

            // 更新状态机
            stateMachine.update(tpf);

            // 每60帧打印一次状态
            if (i % 60 == 0) {
                java.lang.System.out.println("[Frame " + frameCount + "] Current state: " +
                    stateMachine.getCurrentState().getName() +
                    ", Time: " + String.format("%.2f", stateMachine.getCurrentTime()) + "s");
            }
        }

        java.lang.System.out.println("\n=== Simulation Complete ===");
    }

    /**
     * 设置状态转换规则
     */
    private static void setupTransitions(AnimationStateMachine sm) {
        // Idle -> Walk（当玩家开始移动且速度较慢）
        sm.addTransition(new StateTransition(
            "Idle", "Walk",
            () -> PlayerState.isMoving && PlayerState.speed > 0 && PlayerState.speed < 5,
            0.2f, 0, true, 0f
        ));

        // Walk -> Idle（当玩家停止移动）
        sm.addTransition(new StateTransition(
            "Walk", "Idle",
            () -> !PlayerState.isMoving,
            0.3f, 0, true, 0f
        ));

        // Walk -> Run（当玩家加速）
        sm.addTransition(new StateTransition(
            "Walk", "Run",
            () -> PlayerState.isMoving && PlayerState.speed >= 5,
            0.15f, 0, true, 0f
        ));

        // Run -> Walk（当玩家减速）
        sm.addTransition(new StateTransition(
            "Run", "Walk",
            () -> PlayerState.isMoving && PlayerState.speed < 5 && PlayerState.speed > 0,
            0.15f, 0, true, 0f
        ));

        // Run -> Idle（当玩家停止）
        sm.addTransition(new StateTransition(
            "Run", "Idle",
            () -> !PlayerState.isMoving,
            0.3f, 0, true, 0f
        ));

        // Any -> Jump（当玩家按下跳跃键且在地面上）
        sm.addTransition(new StateTransition(
            null, "Jump", // null表示从任意状态
            () -> PlayerState.jumpPressed && PlayerState.isGrounded,
            0.1f, 100, false, 0f // 高优先级，不能中断自己
        ));

        // Jump -> Idle（当跳跃完成且玩家不移动）
        sm.addTransition(new StateTransition(
            "Jump", "Idle",
            () -> PlayerState.isGrounded && !PlayerState.isMoving,
            0.2f, 0, true, 0.9f // 需要播放到90%才能转换
        ));

        // Jump -> Walk（当跳跃完成且玩家移动）
        sm.addTransition(new StateTransition(
            "Jump", "Walk",
            () -> PlayerState.isGrounded && PlayerState.isMoving && PlayerState.speed < 5,
            0.2f, 0, true, 0.9f
        ));
    }

    /**
     * 创建待机动画
     */
    private static AnimationClip createIdleAnimation() {
        AnimationClip clip = new AnimationClip("Idle");

        // 简单的上下浮动
        Keyframe kf1 = new Keyframe(0f, "body", Keyframe.KeyframeType.INTERPOLATED);
        kf1.setPosition(new Vector3f(0, 0, 0));
        kf1.setRotation(new Quaternion());
        kf1.setScale(new Vector3f(1, 1, 1));

        Keyframe kf2 = new Keyframe(1f, "body", Keyframe.KeyframeType.INTERPOLATED);
        kf2.setPosition(new Vector3f(0, 0.05f, 0));
        kf2.setRotation(new Quaternion());
        kf2.setScale(new Vector3f(1, 1, 1));

        Keyframe kf3 = new Keyframe(2f, "body", Keyframe.KeyframeType.INTERPOLATED);
        kf3.setPosition(new Vector3f(0, 0, 0));
        kf3.setRotation(new Quaternion());
        kf3.setScale(new Vector3f(1, 1, 1));

        clip.addKeyframe(kf1);
        clip.addKeyframe(kf2);
        clip.addKeyframe(kf3);
        clip.setDuration(2f);
        clip.setLooping(true);

        return clip;
    }

    /**
     * 创建行走动画
     */
    private static AnimationClip createWalkAnimation() {
        AnimationClip clip = new AnimationClip("Walk");
        // 简化版，实际应该有更多关键帧
        clip.setDuration(1f);
        clip.setLooping(true);
        return clip;
    }

    /**
     * 创建奔跑动画
     */
    private static AnimationClip createRunAnimation() {
        AnimationClip clip = new AnimationClip("Run");
        clip.setDuration(0.6f);
        clip.setLooping(true);
        return clip;
    }

    /**
     * 创建跳跃动画
     */
    private static AnimationClip createJumpAnimation() {
        AnimationClip clip = new AnimationClip("Jump");
        clip.setDuration(1.2f);
        clip.setLooping(false);
        return clip;
    }

    /**
     * 模拟玩家状态（用于测试转换条件）
     */
    static class PlayerState {
        static boolean isMoving = false;
        static float speed = 0f;
        static boolean jumpPressed = false;
        static boolean isGrounded = true;

        static void startWalking() {
            isMoving = true;
            speed = 2f;
        }

        static void startRunning() {
            isMoving = true;
            speed = 8f;
        }

        static void stopMoving() {
            isMoving = false;
            speed = 0f;
        }

        static void jump() {
            jumpPressed = true;
            isGrounded = false;
            // 模拟跳跃后落地
            new Thread(() -> {
                try {
                    Thread.sleep(800);
                    isGrounded = true;
                    jumpPressed = false;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }
}
