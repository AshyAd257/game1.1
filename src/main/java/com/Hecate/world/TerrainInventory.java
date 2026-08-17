package com.Hecate.world;

import com.Hecate.utils.LogUtils;

/**
 * 地形背包系统
 * 跟踪玩家拥有的土方体积
 */
public class TerrainInventory {

    // 土方体积（立方方块单位）
    private float dirtVolume;

    // 初始土方数量
    private static final float INITIAL_DIRT_VOLUME = 100.0f;

    public TerrainInventory() {
        this.dirtVolume = INITIAL_DIRT_VOLUME;
    }

    /**
     * 获取当前土方体积
     */
    public float getDirtVolume() {
        return dirtVolume;
    }

    /**
     * 尝试消耗土方（填土时）
     * @param volume 需要消耗的体积
     * @return 是否成功消耗
     */
    public boolean tryConsumeDirt(float volume) {
        if (volume < 0) {
            throw new IllegalArgumentException("体积不能为负数");
        }

        if (dirtVolume >= volume) {
            dirtVolume -= volume;
            LogUtils.debug(TerrainInventory.class,
                String.format("消耗土方 %.3f，剩余 %.3f", volume, dirtVolume));
            return true;
        } else {
            LogUtils.debug(TerrainInventory.class,
                String.format("土方不足：需要 %.3f，但只有 %.3f", volume, dirtVolume));
            return false;
        }
    }

    /**
     * 添加土方（挖掘时）
     * @param volume 添加的体积
     */
    public void addDirt(float volume) {
        if (volume < 0) {
            throw new IllegalArgumentException("体积不能为负数");
        }

        dirtVolume += volume;
        LogUtils.debug(TerrainInventory.class,
            String.format("获得土方 %.3f，总计 %.3f", volume, dirtVolume));
    }

    /**
     * 尝试执行地形编辑
     * @param volumeChange 体积变化（正数=填土，负数=挖土）
     * @return 是否允许执行
     */
    public boolean canPerformEdit(float volumeChange) {
        if (volumeChange > 0) {
            // 填土：检查是否有足够的土方
            return dirtVolume >= volumeChange;
        } else {
            // 挖土：总是允许
            return true;
        }
    }

    /**
     * 执行地形编辑后更新背包
     * @param volumeChange 体积变化（正数=填土，负数=挖土）
     */
    public void applyVolumeChange(float volumeChange) {
        if (volumeChange > 0) {
            // 填土：消耗土方
            tryConsumeDirt(volumeChange);
        } else if (volumeChange < 0) {
            // 挖土：获得土方
            addDirt(-volumeChange);
        }
    }

    /**
     * 设置土方体积（用于测试）
     */
    public void setDirtVolume(float volume) {
        this.dirtVolume = Math.max(0, volume);
    }

    /**
     * 获取格式化的体积字符串
     */
    public String getFormattedVolume() {
        return String.format("%.1f m³", dirtVolume);
    }
}
