package com.Hecate.player;

import com.Hecate.ink.SparseGridManager;
import com.Hecate.ink.GridCell;
import com.jme3.math.Vector3f;

/**
 * 玩家恢复管理器
 * 管理玩家血量和弹药的联动恢复
 *
 * 恢复规则：
 * - 在地面上没有涂墨的情况下，长按左键2秒后开始恢复
 * - 每秒恢复5%的血量和弹药
 * - 血量上限100点，弹药上限1000点
 */
public class PlayerRecoveryManager {

    private final PlayerHealth playerHealth;
    private final PlayerAmmo playerAmmo;
    private SparseGridManager gridManager;
    private int playerTeam = 0;

    // 恢复状态
    private boolean isRecovering = false;
    private float recoveryHoldTime = 0f;
    private boolean isLeftButtonPressed = false;

    // 恢复参数
    private static final float RECOVERY_DELAY = 2.0f;  // 长按2秒后开始恢复
    private static final float RECOVERY_RATE = 0.05f;  // 每秒恢复5%

    /**
     * 构造函数
     */
    public PlayerRecoveryManager(PlayerHealth playerHealth, PlayerAmmo playerAmmo) {
        this.playerHealth = playerHealth;
        this.playerAmmo = playerAmmo;
    }

    /**
     * 设置网格管理器
     */
    public void setGridManager(SparseGridManager gridManager) {
        this.gridManager = gridManager;
    }

    /**
     * 设置玩家队伍
     */
    public void setPlayerTeam(int team) {
        this.playerTeam = team;
    }

    /**
     * 设置左键按下状态
     */
    public void setLeftButtonPressed(boolean pressed) {
        this.isLeftButtonPressed = pressed;

        // 松开左键时重置恢复状态
        if (!pressed) {
            isRecovering = false;
            recoveryHoldTime = 0f;
        }
    }

    /**
     * 检查当前位置是否可以恢复（地面上没有涂墨）
     */
    private boolean canRecoverAtPosition(Vector3f position) {
        if (gridManager == null) {
            return true;  // 没有网格管理器，默认可以恢复
        }

        GridCell cell = gridManager.getCellAt(position);

        // 只有在空地面上才能恢复
        return cell == null || cell.isEmpty();
    }

    /**
     * 更新恢复系统
     */
    public void update(float tpf, Vector3f playerPosition) {
        // 如果没有按下左键，不处理
        if (!isLeftButtonPressed) {
            return;
        }

        // 检查是否可以在当前位置恢复
        if (!canRecoverAtPosition(playerPosition)) {
            // 在涂墨上，重置恢复状态
            isRecovering = false;
            recoveryHoldTime = 0f;
            return;
        }

        // 检查是否已经满血满弹药
        if (playerHealth.isFullHealth() && playerAmmo.isFull()) {
            // 已经满了，不需要恢复
            isRecovering = false;
            recoveryHoldTime = 0f;
            return;
        }

        // 累计长按时间
        recoveryHoldTime += tpf;

        // 检查是否达到恢复延迟
        if (recoveryHoldTime >= RECOVERY_DELAY) {
            if (!isRecovering) {
                isRecovering = true;
            }

            // 同时恢复血量和弹药（每秒5%）
            playerHealth.recoverByPercentage(RECOVERY_RATE, tpf);
            playerAmmo.recoverByPercentage(RECOVERY_RATE, tpf);
        }
    }

    /**
     * 获取恢复进度（0-1）
     * 用于UI显示
     */
    public float getRecoveryProgress() {
        if (!isLeftButtonPressed) {
            return 0f;
        }
        return Math.min(1.0f, recoveryHoldTime / RECOVERY_DELAY);
    }

    /**
     * 是否正在恢复
     */
    public boolean isRecovering() {
        return isRecovering;
    }

    /**
     * 获取恢复延迟时间
     */
    public float getRecoveryDelay() {
        return RECOVERY_DELAY;
    }

    /**
     * 获取恢复速率
     */
    public float getRecoveryRate() {
        return RECOVERY_RATE;
    }
}
