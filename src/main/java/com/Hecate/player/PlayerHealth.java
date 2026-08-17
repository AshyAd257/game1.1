package com.Hecate.player;

/**
 *  玩家血量管理器
 */
public class PlayerHealth {
    private float currentHealth;
    private final float maxHealth;
    private boolean isDead;

    private float timeSinceLastDamage = 0f;           // 距离上次受伤的时间
    private final float REGEN_DELAY = 5.0f;           // 开始恢复前的延迟（秒）
    private final float REGEN_RATE = 10.0f;           // 恢复速率（点/秒）
    private boolean isRegenerating = false;
    private boolean autoRegenEnabled = false;         // 自动恢复开关（默认禁用）

    // 血量变化监听器
    public interface HealthChangeListener {
        void onHealthChanged(float currentHealth, float maxHealth);
        void onPlayerDied();
        void onPlayerRevived();
    }

    private HealthChangeListener healthChangeListener;

    public PlayerHealth(float maxHealth) {
        this.maxHealth = maxHealth;
        this.currentHealth = maxHealth;
        this.isDead = false;

    }

    /**
     *  设置血量变化监听器
     */
    public void setHealthChangeListener(HealthChangeListener listener) {
        this.healthChangeListener = listener;
    }

    /**
     *  受到伤害
     */
    public void takeDamage(float damage) {
        if (isDead) return;

        float oldHealth = currentHealth;
        currentHealth = Math.max(0, currentHealth - damage);

        // 重置恢复计时器
        timeSinceLastDamage = 0f;
        isRegenerating = false;

        if (currentHealth <= 0 && !isDead) {
            isDead = true;

            if (healthChangeListener != null) {
                healthChangeListener.onPlayerDied();
            }
        }

        if (healthChangeListener != null) {
            healthChangeListener.onHealthChanged(currentHealth, maxHealth);
        }
    }
    /**
     * 更新血量系统（处理自动恢复）
     * @param tpf 帧时间
     */
    public void update(float tpf) {
        if (!autoRegenEnabled || isDead || isFullHealth()) {
            isRegenerating = false;
            return;
        }

        // 增加距离上次受伤的时间
        timeSinceLastDamage += tpf;

        // 检查是否可以开始恢复
        if (timeSinceLastDamage >= REGEN_DELAY) {
            if (!isRegenerating) {
                isRegenerating = true;

            }

            // 线性恢复：每帧恢复 REGEN_RATE * tpf
            float regenAmount = REGEN_RATE * tpf;
            float oldHealth = currentHealth;
            currentHealth = Math.min(maxHealth, currentHealth + regenAmount);

            // 只在血量实际变化时通知
            if (currentHealth != oldHealth && healthChangeListener != null) {
                healthChangeListener.onHealthChanged(currentHealth, maxHealth);
            }

            // 恢复满血时停止
            if (isFullHealth()) {
                isRegenerating = false;

            }
        }
    }

    /**
     * 手动恢复血量（基于百分比）
     * @param percentage 恢复百分比（0-1），例如0.05表示每秒恢复5%
     * @param tpf 时间增量
     */
    public void recoverByPercentage(float percentage, float tpf) {
        if (isDead || percentage <= 0) return;

        float recoverAmount = maxHealth * percentage * tpf;
        heal(recoverAmount);
    }

    /**
     * 设置自动恢复开关
     */
    public void setAutoRegenEnabled(boolean enabled) {
        this.autoRegenEnabled = enabled;
    }

    /**
     * 获取自动恢复开关状态
     */
    public boolean isAutoRegenEnabled() {
        return autoRegenEnabled;
    }
    /**
     * 恢复血量
     * @param amount 恢复量
     */
    public void heal(float amount) {
        if (isDead || amount <= 0) return;

        float oldHealth = currentHealth;
        currentHealth = Math.min(maxHealth, currentHealth + amount);

        // 恢复血量时重置恢复计时器（可选）
        // timeSinceLastDamage = 0f;
        // isRegenerating = false;

        if (currentHealth != oldHealth) {

            if (healthChangeListener != null) {
                healthChangeListener.onHealthChanged(currentHealth, maxHealth);
            }
        }
    }

    /**
     *  完全恢复血量
     */
    public void fullHeal() {
        heal(maxHealth - currentHealth);
    }

    /**
     *  复活玩家
     */
    public void revive() {
        if (isDead) {
            isDead = false;
            currentHealth = maxHealth;
            timeSinceLastDamage = 0f;
            isRegenerating = false;

            if (healthChangeListener != null) {
                healthChangeListener.onPlayerRevived();
                healthChangeListener.onHealthChanged(currentHealth, maxHealth);
            }
        }
    }
    /**
     *  复活玩家（指定血量）
     */
    public void revive(float health) {
        if (isDead) {
            isDead = false;
            currentHealth = Math.min(maxHealth, Math.max(1, health));

            if (healthChangeListener != null) {
                healthChangeListener.onPlayerRevived();
                healthChangeListener.onHealthChanged(currentHealth, maxHealth);
            }
        }
    }

    // Getter方法
    public float getCurrentHealth() {
        return currentHealth;
    }

    public float getMaxHealth() {
        return maxHealth;
    }

    public boolean isDead() {
        return isDead;
    }

    public boolean isAlive() {
        return !isDead;
    }

    /**
     * 获取血量百分比
     */
    public float getHealthPercentage() {
        return currentHealth / maxHealth;
    }

    /**
     * 检查血量是否满血
     */
    public boolean isFullHealth() {
        return currentHealth >= maxHealth;
    }

    /**
     * 检查血量是否危险（低于25%）
     */
    public boolean isLowHealth() {
        return getHealthPercentage() < 0.25f;
    }

    /**
     * 获取血量状态描述
     */
    public String getHealthStatus() {
        if (isDead) {
            return " 死亡";
        } else if (isFullHealth()) {
            return "满血";
        } else if (isLowHealth()) {
            return "危险";
        } else {
            return " 受伤";
        }
    }

    @Override
    public String toString() {
        return "PlayerHealth{" +
                "health=" + (int)currentHealth + "/" + (int)maxHealth +
                ", status=" + getHealthStatus() +
                '}';
    }
}
