package com.Hecate.player.inventory;

/**
 * 手持物品数据类
 * 表示玩家当前手持的物品（武器/方块/物品/空手）
 */
public class HeldItem {
    private final HeldItemType type;
    private final String itemId;  // 对应注册表中的ID

    /**
     * 创建空手状态
     */
    public static HeldItem empty() {
        return new HeldItem(HeldItemType.EMPTY, null);
    }

    /**
     * 创建手持武器
     * @param weaponId 武器注册表中的武器ID
     */
    public static HeldItem weapon(String weaponId) {
        if (weaponId == null || weaponId.isEmpty()) {
            throw new IllegalArgumentException("weaponId 不能为空");
        }
        return new HeldItem(HeldItemType.WEAPON, weaponId);
    }

    /**
     * 创建手持方块
     * @param blockId 方块注册表中的方块ID
     */
    public static HeldItem block(String blockId) {
        if (blockId == null || blockId.isEmpty()) {
            throw new IllegalArgumentException("blockId 不能为空");
        }
        return new HeldItem(HeldItemType.BLOCK, blockId);
    }

    /**
     * 创建手持物品
     * @param itemId 物品注册表中的物品ID
     */
    public static HeldItem item(String itemId) {
        if (itemId == null || itemId.isEmpty()) {
            throw new IllegalArgumentException("itemId 不能为空");
        }
        return new HeldItem(HeldItemType.ITEM, itemId);
    }

    private HeldItem(HeldItemType type, String itemId) {
        this.type = type;
        this.itemId = itemId;
    }

    public HeldItemType getType() {
        return type;
    }

    public String getItemId() {
        return itemId;
    }

    public boolean isEmpty() {
        return type == HeldItemType.EMPTY;
    }

    public boolean isWeapon() {
        return type == HeldItemType.WEAPON;
    }

    public boolean isBlock() {
        return type == HeldItemType.BLOCK;
    }

    public boolean isItem() {
        return type == HeldItemType.ITEM;
    }

    @Override
    public String toString() {
        if (isEmpty()) {
            return "HeldItem{EMPTY}";
        }
        return String.format("HeldItem{%s: %s}", type, itemId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof HeldItem)) return false;
        HeldItem other = (HeldItem) obj;
        if (type != other.type) return false;
        if (itemId == null) return other.itemId == null;
        return itemId.equals(other.itemId);
    }

    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + (itemId != null ? itemId.hashCode() : 0);
        return result;
    }
}
