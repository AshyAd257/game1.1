package com.Hecate.player.inventory;

import com.Hecate.block.BlockRegistry;
import com.Hecate.weapon.WeaponRegistry;
import com.Hecate.weapon.Weapon;
import com.Hecate.weapon.WeaponDefinition;
import com.Hecate.block.Block;

/**
 * 玩家装备管理器
 * 统一管理玩家当前手持/装备的物品，并集成快捷栏系统
 */
public class PlayerEquipment {
    private final PlayerHotbar hotbar;
    private final BlockRegistry blockRegistry;
    private final WeaponRegistry weaponRegistry;

    // 缓存当前装备的实例（从注册表查询）
    private WeaponDefinition currentWeapon;
    private Block currentBlock;

    public PlayerEquipment(BlockRegistry blockRegistry, WeaponRegistry weaponRegistry) {
        this.hotbar = new PlayerHotbar();
        this.blockRegistry = blockRegistry;
        this.weaponRegistry = weaponRegistry;

        // 初始化默认快捷栏
        hotbar.initializeDefault();

        // 更新当前装备
        updateCurrentEquipment();
    }

    /**
     * 选择快捷栏槽位（通过数字键 1-9）
     */
    public void selectHotbarSlot(int slotIndex) {
        hotbar.selectSlot(slotIndex);
        updateCurrentEquipment();
    }

    /**
     * 获取当前选中的槽位索引
     */
    public int getSelectedSlot() {
        return hotbar.getSelectedSlot();
    }

    /**
     * 获取当前手持物品
     */
    public HeldItem getCurrentHeldItem() {
        return hotbar.getCurrentItem();
    }

    /**
     * 更新当前装备的缓存
     */
    private void updateCurrentEquipment() {
        HeldItem current = hotbar.getCurrentItem();

        // 清空缓存
        currentWeapon = null;
        currentBlock = null;

        // 根据类型从注册表查询
        if (current.isWeapon()) {
            WeaponDefinition weaponDef = weaponRegistry.getWeaponDef(current.getItemId());
            if (weaponDef != null) {
                currentWeapon = weaponDef;
            }
        } else if (current.isBlock()) {
            currentBlock = blockRegistry.getBlock(current.getItemId());
        }
    }

    /**
     * 获取当前装备的武器定义
     */
    public WeaponDefinition getCurrentWeapon() {
        return currentWeapon;
    }

    /**
     * 获取当前选中的方块类型
     */
    public Block getCurrentBlock() {
        return currentBlock;
    }

    /**
     * 检查当前是否持有武器
     */
    public boolean isHoldingWeapon() {
        return currentWeapon != null;
    }

    /**
     * 检查当前是否持有方块
     */
    public boolean isHoldingBlock() {
        return currentBlock != null;
    }

    /**
     * 检查当前是否空手
     */
    public boolean isEmpty() {
        return getCurrentHeldItem().isEmpty();
    }

    /**
     * 设置快捷栏槽位
     */
    public void setHotbarSlot(int slotIndex, HeldItem item) {
        hotbar.setSlot(slotIndex, item);
        // 如果修改的是当前选中槽位，需要更新装备
        if (slotIndex == hotbar.getSelectedSlot()) {
            updateCurrentEquipment();
        }
    }

    /**
     * 获取快捷栏
     */
    public PlayerHotbar getHotbar() {
        return hotbar;
    }

    /**
     * 清空快捷栏
     */
    public void clearHotbar() {
        hotbar.clearAll();
        updateCurrentEquipment();
    }

    /**
     * 重置为默认配置
     */
    public void resetToDefault() {
        hotbar.initializeDefault();
        hotbar.selectSlot(0);
        updateCurrentEquipment();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("PlayerEquipment{\n");
        sb.append("  Current: ").append(getCurrentHeldItem()).append("\n");
        if (currentWeapon != null) {
            sb.append("  Weapon: ").append(currentWeapon.getId()).append("\n");
        }
        if (currentBlock != null) {
            sb.append("  Block: ").append(currentBlock.getId()).append("\n");
        }
        sb.append(hotbar.toString());
        sb.append("\n}");
        return sb.toString();
    }
}
