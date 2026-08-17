package com.Hecate.block;

import com.jme3.math.Vector3f;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 尸体方块 - 玩家死亡时生成的特殊方块
 * 包含玩家死亡时携带的所有物品
 */
public class CorpseBlock {
    private final String corpseId;           // 尸体唯一ID
    private final Vector3f position;         // 尸体位置
    private final String playerName;         // 死亡玩家名称
    private final long deathTime;            // 死亡时间戳
    private final List<String> items;        // 死亡时携带的物品（目前用字符串模拟）
    private boolean isLooted;                // 是否已被搜刮

    //  尸体外观相关
    private final float playerFacing;        // 玩家死亡时的朝向

    public CorpseBlock(Vector3f position, String playerName, float playerFacing) {
        this.corpseId = UUID.randomUUID().toString();
        this.position = position.clone();
        this.playerName = playerName;
        this.playerFacing = playerFacing;
        this.deathTime = System.currentTimeMillis();
        this.items = new ArrayList<>();
        this.isLooted = false;

        // 🎒 模拟死亡时携带的物品（未来会从真实物品栏获取）
        simulateDeathItems();

    }

    /**
     * 🎒 模拟死亡时的物品掉落（临时实现）
     */
    private void simulateDeathItems() {
        // 模拟玩家死亡时携带的物品
        items.add("stone:64");      // 64个石头
        items.add("dirt:32");       // 32个泥土
        items.add("wood:16");       // 16个木头
        items.add("iron_sword:1");  // 1把铁剑
        items.add("bread:8");       // 8个面包

    }

    /**
     * 🔍 搜刮尸体（获取所有物品）
     */
    public List<String> lootCorpse() {
        if (isLooted) {

            return new ArrayList<>();
        }

        List<String> lootedItems = new ArrayList<>(items);
        isLooted = true;

        return lootedItems;
    }

    // Getter方法
    public String getCorpseId() { return corpseId; }
    public Vector3f getPosition() { return position.clone(); }
    public String getPlayerName() { return playerName; }
    public long getDeathTime() { return deathTime; }
    public List<String> getItems() { return new ArrayList<>(items); }
    public boolean isLooted() { return isLooted; }
    public float getPlayerFacing() { return playerFacing; }

    @Override
    public String toString() {
        return "CorpseBlock{" +
                "id='" + corpseId + '\'' +
                ", player='" + playerName + '\'' +
                ", position=" + position +
                ", items=" + items.size() +
                ", looted=" + isLooted +
                '}';
    }
}
