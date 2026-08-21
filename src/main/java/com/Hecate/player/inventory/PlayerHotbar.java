package com.Hecate.player.inventory;

import java.util.ArrayList;
import java.util.List;

/**
 * 玩家快捷栏系统
 * 管理 9 个快捷栏槽位，通过数字键 1-9 快速切换
 */
public class PlayerHotbar {
    private static final int HOTBAR_SIZE = 9;

    private final List<HeldItem> slots;
    private int selectedSlot = 0;  // 当前选中的槽位索引 (0-8)

    public PlayerHotbar() {
        slots = new ArrayList<>(HOTBAR_SIZE);
        // 初始化所有槽位为空
        for (int i = 0; i < HOTBAR_SIZE; i++) {
            slots.add(HeldItem.empty());
        }
    }

    /**
     * 获取当前选中槽位的物品
     */
    public HeldItem getCurrentItem() {
        return slots.get(selectedSlot);
    }

    /**
     * 获取当前选中的槽位索引
     */
    public int getSelectedSlot() {
        return selectedSlot;
    }

    /**
     * 选择指定槽位（通过数字键 1-9）
     * @param slotIndex 槽位索引 (0-8)
     */
    public void selectSlot(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= HOTBAR_SIZE) {
            throw new IllegalArgumentException("槽位索引必须在 0-8 之间");
        }
        this.selectedSlot = slotIndex;
    }

    /**
     * 设置指定槽位的物品
     */
    public void setSlot(int slotIndex, HeldItem item) {
        if (slotIndex < 0 || slotIndex >= HOTBAR_SIZE) {
            throw new IllegalArgumentException("槽位索引必须在 0-8 之间");
        }
        if (item == null) {
            item = HeldItem.empty();
        }
        slots.set(slotIndex, item);
    }

    /**
     * 获取指定槽位的物品
     */
    public HeldItem getSlot(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= HOTBAR_SIZE) {
            throw new IllegalArgumentException("槽位索引必须在 0-8 之间");
        }
        return slots.get(slotIndex);
    }

    /**
     * 清空指定槽位
     */
    public void clearSlot(int slotIndex) {
        setSlot(slotIndex, HeldItem.empty());
    }

    /**
     * 清空所有槽位
     */
    public void clearAll() {
        for (int i = 0; i < HOTBAR_SIZE; i++) {
            slots.set(i, HeldItem.empty());
        }
        selectedSlot = 0;
    }

    /**
     * 获取快捷栏大小
     */
    public int getSize() {
        return HOTBAR_SIZE;
    }

    /**
     * 初始化默认快捷栏配置
     * 示例：1=石头, 2=泥土, 3=草方块, 4=玻璃, 5=武器
     */
    public void initializeDefault() {
        setSlot(0, HeldItem.block("stone"));
        setSlot(1, HeldItem.block("dirt"));
        setSlot(2, HeldItem.block("grass"));
        setSlot(3, HeldItem.block("glass"));
        setSlot(4, HeldItem.weapon("smg_01"));  // 示例武器
        // 其余槽位保持为空
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("PlayerHotbar{\n");
        for (int i = 0; i < HOTBAR_SIZE; i++) {
            sb.append(String.format("  [%d] %s%s\n",
                i + 1,
                slots.get(i),
                i == selectedSlot ? " <--" : ""));
        }
        sb.append("}");
        return sb.toString();
    }
}
