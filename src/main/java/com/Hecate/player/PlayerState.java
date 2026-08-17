package com.Hecate.player;

/**
 * 🎮 玩家状态类 - 包含所有玩家状态信息
 */
public class PlayerState {

    // 基础状态
    private boolean moving = false;
    private boolean running = false;
    private boolean grounded = true;
    private boolean idle = true;

    // 输入状态
    private boolean shiftPressed = false;
    private boolean jumpPressed = false;

    // 方向状态
    private String direction = "front";

    // 速度信息
    private float velocity = 0.0f;
    private float horizontalVelocity = 0.0f;
    private float verticalVelocity = 0.0f;

    // 构造函数
    public PlayerState() {
        // 默认状态
    }

    /**
     * 判断玩家是否处于战斗状态
     * TODO: 占位函数，目前死返回 true 用于测试视觉效果
     * 未来接入武器持有判定 + 仇恨系统后替换实现
     * @return true 表示在战斗中
     */
    public boolean isInCombat() {
        return true; // 测试阶段永远显示战斗态的视觉偏移效果
    }

    // 基础状态方法
    public boolean isMoving() {
        return moving;
    }

    public void setMoving(boolean moving) {
        this.moving = moving;
        this.idle = !moving;
    }

    public boolean isRunning() {
        return running && moving;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public boolean isGrounded() {
        return grounded;
    }

    public void setGrounded(boolean grounded) {
        this.grounded = grounded;
    }

    public boolean isIdle() {
        return idle && grounded;
    }

    public void setIdle(boolean idle) {
        this.idle = idle;
    }

    // 输入状态方法
    public boolean isShiftPressed() {
        return shiftPressed;
    }

    public void setShiftPressed(boolean shiftPressed) {
        this.shiftPressed = shiftPressed;
        // Shift键控制跑步
        if (moving) {
            this.running = shiftPressed;
        }
    }

    public boolean isJumpPressed() {
        return jumpPressed;
    }

    public void setJumpPressed(boolean jumpPressed) {
        this.jumpPressed = jumpPressed;
    }

    // 方向方法
    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        if (direction != null &&
                (direction.equals("front") || direction.equals("back") ||
                        direction.equals("left") || direction.equals("right"))) {
            this.direction = direction;
        }
    }

    // 速度方法
    public float getVelocity() {
        return velocity;
    }

    public void setVelocity(float velocity) {
        this.velocity = velocity;
    }

    public float getHorizontalVelocity() {
        return horizontalVelocity;
    }

    public void setHorizontalVelocity(float horizontalVelocity) {
        this.horizontalVelocity = horizontalVelocity;
    }

    public float getVerticalVelocity() {
        return verticalVelocity;
    }

    public void setVerticalVelocity(float verticalVelocity) {
        this.verticalVelocity = verticalVelocity;
    }

    // 便捷方法
    public boolean isWalking() {
        return moving && !running;
    }

    public boolean isJumping() {
        return !grounded && verticalVelocity > 0;
    }

    public boolean isFalling() {
        return !grounded && verticalVelocity < 0;
    }

    // 更新方法
    public void update(float deltaTime) {
        // 根据速度更新移动状态
        boolean wasMoving = moving;
        moving = Math.abs(horizontalVelocity) > 0.1f;

        if (moving != wasMoving) {
            idle = !moving;
        }

        // 更新跑步状态
        if (moving && shiftPressed) {
            running = true;
        } else if (!moving) {
            running = false;
        }
    }

    @Override
    public String toString() {
        return String.format("PlayerState{moving=%s, running=%s, grounded=%s, idle=%s, direction='%s'}",
                moving, running, grounded, idle, direction);
    }
}
