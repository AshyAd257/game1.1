package com.Hecate.item;

/**
 * 物品堆：物品id + 数量的不可变值对象。
 * <p>不直接持有 {@link ItemDef} 引用（避免序列化/存档时把整张表格数据一起存下来），
 * 查名称/图标/堆叠上限时通过 {@link ItemRegistry#getItemDef(String)} 按id查询。
 */
public final class ItemStack {
    private final String itemId;
    private final int count;

    /**
     * 创建一个物品堆。
     * @param itemId 物品注册表中的物品ID
     * @param count 数量，必须大于0（表示"空槛位"请用 {@link #EMPTY}，不要用count=0的ItemStack）
     */
    public ItemStack(String itemId, int count) {
        if (itemId == null || itemId.isEmpty()) {
            throw new IllegalArgumentException("itemId 不能为空");
        }
        if (count < 1) {
            throw new IllegalArgumentException("count 必须至少为1");
        }
        this.itemId = itemId;
        this.count = count;
    }

    /** 表示槛位为空的哨兵值，没有itemId/count概念。 */
    public static final ItemStack EMPTY = new ItemStack();

    private ItemStack() {
        this.itemId = null;
        this.count = 0;
    }

    public boolean isEmpty() {
        return itemId == null;
    }

    public String getItemId() {
        return itemId;
    }

    public int getCount() {
        return count;
    }

    /**
     * 是否可以与另一个堆合并（同种物品，且未指向 {@link #EMPTY}）。
     */
    public boolean canMergeWith(ItemStack other) {
        if (isEmpty() || other == null || other.isEmpty()) {
            return false;
        }
        return itemId.equals(other.itemId);
    }

    /**
     * 返回数量变为newCount的新堆（this不变，符合不可变值对象约定）。
     * @param newCount 新数量，必须大于0
     */
    public ItemStack withCount(int newCount) {
        return new ItemStack(itemId, newCount);
    }

    @Override
    public String toString() {
        if (isEmpty()) {
            return "ItemStack{EMPTY}";
        }
        return "ItemStack{" + itemId + " x" + count + '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ItemStack)) return false;
        ItemStack other = (ItemStack) obj;
        if (isEmpty() || other.isEmpty()) {
            return isEmpty() == other.isEmpty();
        }
        return count == other.count && itemId.equals(other.itemId);
    }

    @Override
    public int hashCode() {
        if (isEmpty()) {
            return 0;
        }
        return 31 * itemId.hashCode() + count;
    }
}
