package com.Hecate.player;

/**
 * 玩家弹药系统
 * 管理玩家的弹药容量、消耗和恢复
 */
public class PlayerAmmo {

    // 弹药容量
    private static final float DEFAULT_MAX_AMMO = 1000.0f;
    private float maxAmmo;
    private float currentAmmo;

    // 恢复速度（每秒恢复量）
    private static final float RECOVERY_RATE = 50.0f; // 每秒恢复50弹药

    // 监听器
    private AmmoChangeListener ammoChangeListener;

    /**
     * 弹药变化监听器接口
     */
    public interface AmmoChangeListener {
        /**
         * 弹药数量变化时调用
         * @param currentAmmo 当前弹药
         * @param maxAmmo 最大弹药
         */
        void onAmmoChanged(float currentAmmo, float maxAmmo);

        /**
         * 弹药耗尽时调用
         */
        void onAmmoEmpty();
    }

    /**
     * 构造函数 - 使用默认最大弹药量
     */
    public PlayerAmmo() {
        this(DEFAULT_MAX_AMMO);
    }

    /**
     * 构造函数 - 指定最大弹药量
     */
    public PlayerAmmo(float maxAmmo) {
        this.maxAmmo = maxAmmo;
        this.currentAmmo = maxAmmo; // 初始满弹药
    }

    /**
     * 设置弹药变化监听器
     */
    public void setAmmoChangeListener(AmmoChangeListener listener) {
        this.ammoChangeListener = listener;
    }

    /**
     * 消耗弹药
     * @param amount 消耗量
     * @return true 如果成功消耗，false 如果弹药不足
     */
    public boolean consume(float amount) {
        if (currentAmmo >= amount) {
            currentAmmo -= amount;
            if (currentAmmo < 0) currentAmmo = 0;

            if (ammoChangeListener != null) {
                ammoChangeListener.onAmmoChanged(currentAmmo, maxAmmo);
            }

            if (currentAmmo <= 0 && ammoChangeListener != null) {
                ammoChangeListener.onAmmoEmpty();
            }

            return true;
        }
        return false;
    }

    /**
     * 恢复弹药（在己方涂墨上且处于恢复状态时调用）
     * @param tpf 时间增量
     */
    public void recover(float tpf) {
        if (currentAmmo < maxAmmo) {
            currentAmmo += RECOVERY_RATE * tpf;
            if (currentAmmo > maxAmmo) {
                currentAmmo = maxAmmo;
            }

            if (ammoChangeListener != null) {
                ammoChangeListener.onAmmoChanged(currentAmmo, maxAmmo);
            }
        }
    }

    /**
     * 基于百分比恢复弹药（每秒恢复最大弹药的指定百分比）
     * @param percentage 恢复百分比（0-1），例如0.05表示每秒恢复5%
     * @param tpf 时间增量
     */
    public void recoverByPercentage(float percentage, float tpf) {
        if (currentAmmo >= maxAmmo || percentage <= 0) {
            return;
        }

        float recoverAmount = maxAmmo * percentage * tpf;
        currentAmmo = Math.min(maxAmmo, currentAmmo + recoverAmount);

        if (ammoChangeListener != null) {
            ammoChangeListener.onAmmoChanged(currentAmmo, maxAmmo);
        }
    }

    /**
     * 立即补满弹药
     */
    public void refill() {
        currentAmmo = maxAmmo;
        if (ammoChangeListener != null) {
            ammoChangeListener.onAmmoChanged(currentAmmo, maxAmmo);
        }
    }

    /**
     * 获取当前弹药
     */
    public float getCurrentAmmo() {
        return currentAmmo;
    }

    /**
     * 获取最大弹药
     */
    public float getMaxAmmo() {
        return maxAmmo;
    }

    /**
     * 获取弹药百分比
     */
    public float getAmmoPercentage() {
        return currentAmmo / maxAmmo;
    }

    /**
     * 检查是否有足够弹药
     */
    public boolean hasAmmo(float amount) {
        return currentAmmo >= amount;
    }

    /**
     * 检查弹药是否为空
     */
    public boolean isEmpty() {
        return currentAmmo <= 0;
    }

    /**
     * 检查弹药是否已满
     */
    public boolean isFull() {
        return currentAmmo >= maxAmmo;
    }
}
