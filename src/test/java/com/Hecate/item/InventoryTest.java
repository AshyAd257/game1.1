package com.Hecate.item;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 物品栏系统测试：ItemStack合并规则 + Inventory堆叠/换位/移除行为
 */
public class InventoryTest {
    private ItemRegistry itemRegistry;
    private Inventory inventory;

    @BeforeEach
    public void setup() {
        itemRegistry = ItemRegistry.createInstance();
        itemRegistry.registerItem(new ItemDef("scrap_metal", "碎金属", "icon.png", 64));
        itemRegistry.registerItem(new ItemDef("gem_shard", "晶体碎片", "icon.png", 10));
        itemRegistry.registerItem(new ItemDef("gun1", "蒸汽朋克枪", "icon.png", 1, 2, 1));
        inventory = new Inventory(9, itemRegistry);
    }

    @Test
    public void testEmptySlotsInitially() {
        for (int i = 0; i < inventory.getSize(); i++) {
            assertTrue(inventory.isEmpty(i));
        }
    }

    @Test
    public void testAddItemFillsEmptySlot() {
        int remaining = inventory.addItem("scrap_metal", 10);
        assertEquals(0, remaining);
        assertEquals(10, inventory.countItem("scrap_metal"));
    }

    @Test
    public void testAddItemStacksIntoExistingSlot() {
        inventory.addItem("scrap_metal", 10);
        inventory.addItem("scrap_metal", 5);

        // 应该都堆在同一个槛位里，而不是散落到第二个空槛位
        assertEquals(15, inventory.getSlot(0).getCount());
        assertEquals(15, inventory.countItem("scrap_metal"));
    }

    @Test
    public void testAddItemOverflowsToNextSlotAtMaxStack() {
        inventory.addItem("scrap_metal", 64);
        inventory.addItem("scrap_metal", 10);

        assertEquals(64, inventory.getSlot(0).getCount());
        assertEquals(10, inventory.getSlot(1).getCount());
    }

    @Test
    public void testAddItemReturnsRemainingWhenInventoryFull() {
        Inventory tiny = new Inventory(1, itemRegistry);
        int remaining = tiny.addItem("gem_shard", 15); // maxStack=10

        assertEquals(5, remaining);
        assertEquals(10, tiny.getSlot(0).getCount());
    }

    @Test
    public void testAddItemUnknownIdThrows() {
        assertThrows(IllegalArgumentException.class, () -> inventory.addItem("unknown_item", 1));
    }

    @Test
    public void testSwapSlots() {
        inventory.setSlot(0, new ItemStack("scrap_metal", 5));
        inventory.setSlot(1, new ItemStack("gem_shard", 2));

        inventory.swapSlots(0, 1);

        assertEquals("gem_shard", inventory.getSlot(0).getItemId());
        assertEquals("scrap_metal", inventory.getSlot(1).getItemId());
    }

    @Test
    public void testRemoveFromSlotPartial() {
        inventory.setSlot(0, new ItemStack("scrap_metal", 10));

        int removed = inventory.removeFromSlot(0, 4);

        assertEquals(4, removed);
        assertEquals(6, inventory.getSlot(0).getCount());
    }

    @Test
    public void testRemoveFromSlotMoreThanAvailableClearsSlot() {
        inventory.setSlot(0, new ItemStack("scrap_metal", 3));

        int removed = inventory.removeFromSlot(0, 10);

        assertEquals(3, removed);
        assertTrue(inventory.isEmpty(0));
    }

    @Test
    public void testIsFull() {
        Inventory tiny = new Inventory(2, itemRegistry);
        assertFalse(tiny.isFull());

        tiny.addItem("gem_shard", 10);
        tiny.addItem("gem_shard", 10);

        assertTrue(tiny.isFull());
    }

    @Test
    public void testItemStackCanMergeWith() {
        ItemStack a = new ItemStack("scrap_metal", 5);
        ItemStack b = new ItemStack("scrap_metal", 3);
        ItemStack c = new ItemStack("gem_shard", 1);

        assertTrue(a.canMergeWith(b));
        assertFalse(a.canMergeWith(c));
        assertFalse(a.canMergeWith(ItemStack.EMPTY));
    }

    @Test
    public void testInvalidIndexThrows() {
        assertThrows(IndexOutOfBoundsException.class, () -> inventory.getSlot(99));
    }

    // ==================== 跨格物品（如Gun1横向占2格）====================

    @Test
    public void testAddMultiCellItemOccupiesFootprint() {
        Inventory grid = new Inventory(16, 4, itemRegistry); // 4x4，与backpack.png布局一致
        int remaining = grid.addItem("gun1", 1);

        assertEquals(0, remaining);
        // 锚点格(0)持有数据，被占用的格子(1)标记为该锚点占用但自身不持有数据
        assertEquals("gun1", grid.getSlot(0).getItemId());
        assertTrue(grid.getSlot(1).isEmpty());
        assertEquals(0, grid.getAnchorIndex(1));
        assertFalse(grid.isEmpty(1));
    }

    @Test
    public void testMultiCellItemCannotWrapAcrossRowBoundary() {
        Inventory grid = new Inventory(16, 4, itemRegistry);
        // 占住格0和格2：格0-1、格2-3都放不下gun1（各剩一个空格但不连续），
        // 格3自身横向占2格会越过行边界(3+2>4)也不合法——逼迫gun1只能落到第二行开头
        grid.setSlot(0, new ItemStack("gem_shard", 1));
        grid.setSlot(2, new ItemStack("gem_shard", 1));
        int remaining = grid.addItem("gun1", 1);

        assertEquals(0, remaining);
        assertEquals("gun1", grid.getSlot(4).getItemId()); // 第二行第一格
    }

    @Test
    public void testMultiCellItemRejectsOverlap() {
        Inventory grid = new Inventory(16, 4, itemRegistry);
        grid.setSlot(0, new ItemStack("gun1", 1)); // 占用格0,1

        assertFalse(grid.canPlaceAt(1, 2, 1)); // 与已有的gun1重叠
        assertTrue(grid.canPlaceAt(2, 2, 1));  // 格2,3空闲，可以放
    }

    @Test
    public void testClearSlotOnOccupiedCellClearsWholeFootprint() {
        Inventory grid = new Inventory(16, 4, itemRegistry);
        grid.setSlot(0, new ItemStack("gun1", 1));

        grid.clearSlot(1); // 点击被占用格（非锚点），应清空整个footprint

        assertTrue(grid.isEmpty(0));
        assertTrue(grid.isEmpty(1));
    }

    @Test
    public void testRemoveItemByIdRemovesMultiCellFootprint() {
        Inventory grid = new Inventory(16, 4, itemRegistry);
        grid.addItem("gun1", 1);

        int removed = grid.removeItem("gun1", 1);

        assertEquals(1, removed);
        assertTrue(grid.isEmpty(0));
        assertTrue(grid.isEmpty(1));
        assertEquals(0, grid.countItem("gun1"));
    }

    @Test
    public void testSwapSlotsRejectsMultiCellItem() {
        Inventory grid = new Inventory(16, 4, itemRegistry);
        grid.addItem("gun1", 1);
        grid.setSlot(4, new ItemStack("gem_shard", 1));

        assertThrows(IllegalStateException.class, () -> grid.swapSlots(0, 4));
    }

    // ==================== moveItem（拖拽落位）====================

    @Test
    public void testMoveItemToEmptySlot() {
        inventory.setSlot(0, new ItemStack("scrap_metal", 5));

        boolean moved = inventory.moveItem(0, 3);

        assertTrue(moved);
        assertTrue(inventory.isEmpty(0));
        assertEquals("scrap_metal", inventory.getSlot(3).getItemId());
        assertEquals(5, inventory.getSlot(3).getCount());
    }

    @Test
    public void testMoveItemSwapsTwoSingleCellItems() {
        inventory.setSlot(0, new ItemStack("scrap_metal", 5));
        inventory.setSlot(1, new ItemStack("gem_shard", 2));

        boolean moved = inventory.moveItem(0, 1);

        assertTrue(moved);
        assertEquals("gem_shard", inventory.getSlot(0).getItemId());
        assertEquals("scrap_metal", inventory.getSlot(1).getItemId());
    }

    @Test
    public void testMoveItemFromEmptySlotFails() {
        boolean moved = inventory.moveItem(0, 1);
        assertFalse(moved);
    }

    @Test
    public void testMoveItemOntoSelfIsNoOp() {
        Inventory grid = new Inventory(16, 4, itemRegistry);
        grid.setSlot(0, new ItemStack("gun1", 1)); // 占用格0,1

        boolean moved = grid.moveItem(0, 1); // 拖到自己占用的另一格

        assertTrue(moved);
        assertEquals("gun1", grid.getSlot(0).getItemId()); // 位置完全不变
        assertFalse(grid.isEmpty(1)); // 格1仍被gun1的footprint占用，不是空闲
        assertEquals(0, grid.getAnchorIndex(1));
    }

    @Test
    public void testMoveItemResolvesNonAnchorFromIndex() {
        Inventory grid = new Inventory(16, 4, itemRegistry);
        grid.setSlot(0, new ItemStack("gun1", 1)); // 占用格0,1

        // 从被占用格(1)拖动而不是锚点格(0)本身，应该整体搬移
        boolean moved = grid.moveItem(1, 8);

        assertTrue(moved);
        assertTrue(grid.isEmpty(0));
        assertTrue(grid.isEmpty(1));
        assertEquals("gun1", grid.getSlot(8).getItemId());
        assertEquals(8, grid.getAnchorIndex(9));
    }

    @Test
    public void testMoveMultiCellItemToEmptyArea() {
        Inventory grid = new Inventory(16, 4, itemRegistry);
        grid.setSlot(0, new ItemStack("gun1", 1)); // 占用格0,1

        boolean moved = grid.moveItem(0, 8); // 搬到第三行开头，占用8,9

        assertTrue(moved);
        assertTrue(grid.isEmpty(0));
        assertTrue(grid.isEmpty(1));
        assertEquals("gun1", grid.getSlot(8).getItemId());
        assertEquals(8, grid.getAnchorIndex(9));
    }

    @Test
    public void testMoveMultiCellItemToInsufficientSpaceFailsAndRollsBack() {
        Inventory grid = new Inventory(16, 4, itemRegistry);
        grid.setSlot(0, new ItemStack("gun1", 1)); // 占用格0,1
        grid.setSlot(2, new ItemStack("gem_shard", 1)); // 挡住格2，格3单独放不下横向2格的gun1

        boolean moved = grid.moveItem(0, 3);

        assertFalse(moved);
        // 原状必须完全不变
        assertEquals("gun1", grid.getSlot(0).getItemId());
        assertFalse(grid.isEmpty(1)); // 格1仍被gun1的footprint占用，不是空闲
        assertEquals(0, grid.getAnchorIndex(1));
        assertEquals("gem_shard", grid.getSlot(2).getItemId());
    }

    @Test
    public void testMoveMultiCellItemSwapsWithSingleCellItem() {
        Inventory grid = new Inventory(16, 4, itemRegistry);
        grid.setSlot(0, new ItemStack("gun1", 1)); // 占用格0,1
        grid.setSlot(8, new ItemStack("gem_shard", 3)); // 第三行开头，两侧(9)都空着

        boolean moved = grid.moveItem(0, 8);

        assertTrue(moved);
        assertEquals("gem_shard", grid.getSlot(0).getItemId());
        assertEquals(3, grid.getSlot(0).getCount());
        assertEquals("gun1", grid.getSlot(8).getItemId());
        assertEquals(8, grid.getAnchorIndex(9));
    }

    @Test
    public void testMoveMultiCellItemSwapFailsWhenTargetBoundaryTooNarrowAndRollsBack() {
        Inventory grid = new Inventory(16, 4, itemRegistry);
        grid.setSlot(0, new ItemStack("gun1", 1)); // 占用格0,1
        grid.setSlot(3, new ItemStack("gem_shard", 1)); // 行末最后一格，横向2格的gun1放不进这个位置

        boolean moved = grid.moveItem(0, 3);

        assertFalse(moved);
        // 原状必须完全不变（包括之前换位过程中临时写入又回滚的格子0）
        assertEquals("gun1", grid.getSlot(0).getItemId());
        assertEquals(0, grid.getAnchorIndex(1));
        assertEquals("gem_shard", grid.getSlot(3).getItemId());
    }
}
