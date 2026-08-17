package com.Hecate.block;

import com.jme3.math.Vector3f;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

/**
 * 💀 尸体方块管理器 - 管理世界中的所有尸体方块
 */
public class CorpseBlockManager {
    private static CorpseBlockManager instance;
    private final Map<String, CorpseBlock> corpseBlocks = new HashMap<>();

    private CorpseBlockManager() {}

    public static synchronized CorpseBlockManager getInstance() {
        if (instance == null) {
            instance = new CorpseBlockManager();
        }
        return instance;
    }

    /**
     * 💀 创建尸体方块
     */
    public CorpseBlock createCorpseBlock(Vector3f position, String playerName, float playerFacing) {
        CorpseBlock corpse = new CorpseBlock(position, playerName, playerFacing);
        corpseBlocks.put(corpse.getCorpseId(), corpse);

        System.out.println("💀 尸体方块已注册: " + corpse.getCorpseId());
        return corpse;
    }

    /**
     * 🔍 根据位置查找尸体方块
     */
    public CorpseBlock getCorpseAtPosition(Vector3f position) {
        for (CorpseBlock corpse : corpseBlocks.values()) {
            if (corpse.getPosition().distance(position) < 1.0f) { // 1格范围内
                return corpse;
            }
        }
        return null;
    }

    /**
     * 🎒 搜刮指定位置的尸体
     */
    public List<String> lootCorpseAtPosition(Vector3f position) {
        CorpseBlock corpse = getCorpseAtPosition(position);
        if (corpse != null) {
            return corpse.lootCorpse();
        }
        return new ArrayList<>();
    }

    /**
     * 🗑️ 移除尸体方块
     */
    public boolean removeCorpse(String corpseId) {
        CorpseBlock removed = corpseBlocks.remove(corpseId);
        if (removed != null) {
            System.out.println("🗑️ 移除尸体方块: " + corpseId);
            return true;
        }
        return false;
    }

    /**
     * 📊 获取统计信息
     */
    public void printStatistics() {
        System.out.println("💀 尸体方块统计:");
        System.out.println("   总数量: " + corpseBlocks.size());

        int lootedCount = 0;
        for (CorpseBlock corpse : corpseBlocks.values()) {
            if (corpse.isLooted()) {
                lootedCount++;
            }
        }

        System.out.println("   已搜刮: " + lootedCount);
        System.out.println("   未搜刮: " + (corpseBlocks.size() - lootedCount));
    }

    /**
     * 📋 获取所有尸体方块
     */
    public Map<String, CorpseBlock> getAllCorpses() {
        return new HashMap<>(corpseBlocks);
    }

    /**
     * 🧹 清理过期尸体（可选功能）
     */
    public void cleanupExpiredCorpses(long maxAgeMillis) {
        long currentTime = System.currentTimeMillis();
        List<String> toRemove = new ArrayList<>();

        for (CorpseBlock corpse : corpseBlocks.values()) {
            if (currentTime - corpse.getDeathTime() > maxAgeMillis) {
                toRemove.add(corpse.getCorpseId());
            }
        }

        for (String corpseId : toRemove) {
            removeCorpse(corpseId);
        }

        if (!toRemove.isEmpty()) {
            System.out.println("🧹 清理了 " + toRemove.size() + " 个过期尸体");
        }
    }
}
