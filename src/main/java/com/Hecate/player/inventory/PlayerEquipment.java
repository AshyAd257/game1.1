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

    // 外部武器覆盖：Gun1/Gun2（PlayerCombatController，独立于快捷栏的老系统）装备时置true。
    // 快捷栏本身的选中槛位不受影响（仍可能是stone等方块），但isHoldingBlock()/isHoldingWeapon()
    // 需要如实反映"玩家实际手上拿的是枪"，否则右键会在持枪时依然触发方块放置。
    private boolean externalWeaponOverride = false;

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
     * 检查当前是否持有武器（快捷栏武器槛位，或外部覆盖——见{@link #setExternalWeaponOverride}）
     */
    public boolean isHoldingWeapon() {
        return externalWeaponOverride || currentWeapon != null;
    }

    /**
     * 检查当前是否持有方块。外部武器覆盖生效时（Gun1/Gun2已装备），即使快捷栏
     * 选中的是方块槛位，也视为"手上没有方块"，避免持枪状态下右键依然放置方块。
     */
    public boolean isHoldingBlock() {
        return !externalWeaponOverride && currentBlock != null;
    }

    /**
     * 设置外部武器覆盖状态（由PlayerController在Gun1/Gun2装备/卸下时调用）。
     * 该覆盖不改变快捷栏本身选中的槛位，只影响isHoldingWeapon()/isHoldingBlock()的
     * 判定结果，确保"手持方块"与"手持Gun1/Gun2"始终互斥。
     */
    public void setExternalWeaponOverride(boolean active) {
        this.externalWeaponOverride = active;
    }

    /**
     * 查询外部武器覆盖是否生效（Gun1/Gun2是否已装备）
     */
    public boolean isExternalWeaponOverrideActive() {
        return externalWeaponOverride;
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
