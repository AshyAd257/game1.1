package com.Hecate.weapon;

/**
 * 输入系统 - 处理玩家开火输入
 *
 * 职责：
 * 1. 检测按键状态（按下/按住/松开）
 * 2. 根据FireMode判断"现在该开火"
 * 3. 输出开火请求（FireRequest）
 *
 * 设计理念：
 * - 输入系统不发射子弹，只回答"这一帧该不该触发开火"
 * - 同一个物理按键，不同fireMode解释成不同行为
 * - 蓄力模式：松手即发，蓄多少放多少（无minCharge限制）
 */
public class WeaponInputHandler {

    // 输入状态
    private boolean fireButtonPressed;      // 开火键是否按下（边沿触发：这一帧刚按下）
    private boolean fireButtonReleased;     // 开火键是否松开（边沿触发：这一帧刚松开）
    private boolean fireButtonHeld;         // 开火键是否持续按住

    // 上一帧状态（用于检测边沿）
    private boolean lastFramePressed;

    // 连发状态（BURST模式）
    private int burstShotsRemaining;        // 剩余连发次数
    private float burstCooldown;            // 连发间隔计时器

    public WeaponInputHandler() {
        this.fireButtonPressed = false;
        this.fireButtonReleased = false;
        this.fireButtonHeld = false;
        this.lastFramePressed = false;
        this.burstShotsRemaining = 0;
        this.burstCooldown = 0.0f;
    }

    /**
     * 每帧更新输入状态
     * @param fireInput 当前帧开火键是否按下
     * @param deltaTime 时间增量
     */
    public void update(boolean fireInput, float deltaTime) {
        // 检测边沿（按下瞬间和松开瞬间）
        fireButtonPressed = fireInput && !lastFramePressed;     // 上升沿
        fireButtonReleased = !fireInput && lastFramePressed;    // 下降沿

        // 更新持续按住状态
        fireButtonHeld = fireInput;

        // 更新连发冷却
        if (burstCooldown > 0) {
            burstCooldown -= deltaTime;
        }

        // 记录状态
        lastFramePressed = fireInput;
    }

    /**
     * 判断是否应该开火
     * @param weapon 武器实例
     * @param currentTime 当前时间
     * @param deltaTime 时间增量（用于蓄力）
     * @return FireRequest 如果应该开火，返回开火请求；否则返回null
     */
    public FireRequest shouldFire(WeaponInstance weapon, float currentTime, float deltaTime) {
        WeaponDefinition.FireMode mode = weapon.getDef().getFireMode();

        switch (mode) {
            case AUTO:
                return handleAutoMode(weapon, currentTime);

            case SINGLE:
                return handleSingleMode(weapon, currentTime);

            case CHARGE:
                return handleChargeMode(weapon, currentTime, deltaTime);

            case BURST:
                return handleBurstMode(weapon, currentTime, deltaTime);
        }

        return null;
    }

    /**
     * 处理全自动模式
     * 按住持续开火，受射速限制
     */
    private FireRequest handleAutoMode(WeaponInstance weapon, float currentTime) {
        if (fireButtonHeld && weapon.canFire(currentTime, 1.0f)) {
            return new FireRequest(FireRequest.Type.NORMAL, 1.0f);
        }
        return null;
    }

    /**
     * 处理单发模式
     * 每次点击发射一次，受射速限制
     */
    private FireRequest handleSingleMode(WeaponInstance weapon, float currentTime) {
        if (fireButtonPressed && weapon.canFire(currentTime, 1.0f)) {
            return new FireRequest(FireRequest.Type.NORMAL, 1.0f);
        }
        return null;
    }

    /**
     * 处理蓄力模式
     * 按住蓄力，松手即发，蓄多少放多少（无最小蓄力限制）
     */
    private FireRequest handleChargeMode(WeaponInstance weapon, float currentTime, float deltaTime) {
        Float chargeTime = weapon.getDef().getParam("chargeTime", 1.5f);

        // 按住：持续蓄力
        if (fireButtonHeld) {
            weapon.updateCharge(deltaTime, chargeTime);
            return null;  // 还在蓄力，不开火
        }

        // 松开：释放蓄力攻击
        if (fireButtonReleased && weapon.getCharge() > 0.0f) {
            float chargeRatio = weapon.getCharge();
            weapon.releaseCharge();

            // 蓄多少放多少，无最小限制
            return new FireRequest(FireRequest.Type.CHARGED, chargeRatio);
        }

        return null;
    }

    /**
     * 处理连发模式
     * 每次点击触发一轮连发，连发期间自动发射
     */
    private FireRequest handleBurstMode(WeaponInstance weapon, float currentTime, float deltaTime) {
        Integer burstCount = weapon.getDef().getParam("burstCount", 3);
        Float burstInterval = weapon.getDef().getParam("burstInterval", 0.1f);

        // 点击触发新一轮连发
        if (fireButtonPressed && weapon.canFire(currentTime, 1.0f) && burstShotsRemaining == 0) {
            burstShotsRemaining = burstCount;
            burstCooldown = 0.0f;  // 第一发立即发射
        }

        // 连发进行中
        if (burstShotsRemaining > 0 && burstCooldown <= 0) {
            burstShotsRemaining--;
            burstCooldown = burstInterval;
            return new FireRequest(FireRequest.Type.BURST, (float)burstShotsRemaining);
        }

        return null;
    }

    /**
     * 重置输入状态（切换武器时调用）
     */
    public void reset() {
        fireButtonPressed = false;
        fireButtonReleased = false;
        fireButtonHeld = false;
        lastFramePressed = false;
        burstShotsRemaining = 0;
        burstCooldown = 0.0f;
    }

    // Getters
    public boolean isFireButtonPressed() { return fireButtonPressed; }
    public boolean isFireButtonReleased() { return fireButtonReleased; }
    public boolean isFireButtonHeld() { return fireButtonHeld; }
    public int getBurstShotsRemaining() { return burstShotsRemaining; }

    /**
     * 开火请求 - 输入系统的输出
     */
    public static class FireRequest {
        public enum Type {
            NORMAL,     // 普通开火
            CHARGED,    // 蓄力攻击（intensity=蓄力比例0-1）
            BURST       // 连发（intensity=剩余连发数）
        }

        public final Type type;
        public final float intensity;  // 强度/倍率（蓄力进度、剩余连发数）

        public FireRequest(Type type, float intensity) {
            this.type = type;
            this.intensity = intensity;
        }

        @Override
        public String toString() {
            return String.format("FireRequest[%s, intensity=%.2f]", type, intensity);
        }
    }
}
