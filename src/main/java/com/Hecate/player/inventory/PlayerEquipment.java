package com.Hecate.player.inventory;

import com.Hecate.block.Block;
import com.Hecate.block.BlockRegistry;
import com.Hecate.item.ItemDef;
import com.Hecate.item.ItemRegistry;
import com.Hecate.item.ItemStack;
import com.Hecate.item.Inventory;
import com.Hecate.weapon.Weapon;
import com.Hecate.weapon.WeaponFactory;

/**
 * 玩家装备管理器：不再维护独立的快捷栏数组——玩家"手上拿着什么"就是背包
 * ({@link Inventory})里某一格的 {@link ItemStack}，通过 {@link #selectedSlotIndex}
 * 指向哪一格。这样背包/手持是同一份数据的两个视图，不存在"背包里的图标"和
 * "手上真正拿的东西"不同步的问题（此前Gun1/Gun2走的是完全独立的
 * backpack.addItem/removeItem手动同步路径，这正是要消灭的那种不一致）。
 * <p>选中槛位切换（滚轮/数字键）时：若新槛位物品有 {@link ItemDef#hasWeaponId()}，
 * 自动用 {@link WeaponFactory} 装备对应武器；切出时自动卸下。方块类物品
 * （{@link ItemDef#hasBlockId()}）不需要"装备"这个动作，右键放置时直接查当前槛位。
 */
public class PlayerEquipment {
    private final BlockRegistry blockRegistry;
    private final ItemRegistry itemRegistry;
    private Inventory backpack;

    // 当前选中的槛位（对应背包前9格，数字键1-9/滚轮都作用于这个范围）
    private int selectedSlotIndex = 0;
    private static final int SELECTABLE_SLOT_COUNT = 9;

    // 外部武器覆盖：兼容极少数尚未迁移到本类武器装备路径的旧调用（当前应始终为false，
    // 保留字段是因为PlayerController.setExternalWeaponOverride的调用点还没有全部清理，
    // 见WeaponEquippedEvent/WeaponUnequippedEvent订阅逻辑）。
    private boolean externalWeaponOverride = false;

    // 武器装备/卸下的回调——PlayerCombatController通过这个接口接收"当前选中槛位的
    // 武器变了"的通知，自己决定如何构造/清理Weapon实例、挂载手持模型等（这些是战斗
    // 系统的职责，不应该让PlayerEquipment直接持有Node/AssetManager等依赖）。
    public interface WeaponEquipListener {
        void onWeaponEquipped(String weaponId, Weapon weapon);
        void onWeaponUnequipped();
    }
    private WeaponEquipListener weaponEquipListener;

    public PlayerEquipment(BlockRegistry blockRegistry, com.Hecate.weapon.WeaponRegistry weaponRegistry) {
        this.blockRegistry = blockRegistry;
        this.itemRegistry = ItemRegistry.getInstance();
        // weaponRegistry参数保留仅为兼容现有调用签名（PlayerStateManager构造函数）——
        // 武器装备现在完全通过WeaponFactory.create(weaponId)按id构造，不需要经过
        // WeaponRegistry查WeaponDefinition这一步（那是数值表，不参与实际构造）。
    }

    /**
     * 设置背包引用。必须在构造后、首次查询手持物品之前调用——PlayerStateManager
     * 先构造Inventory，再构造PlayerEquipment时传入，避免循环依赖。
     */
    public void setBackpack(Inventory backpack) {
        this.backpack = backpack;
    }

    public void setWeaponEquipListener(WeaponEquipListener listener) {
        this.weaponEquipListener = listener;
    }

    /**
     * 选中指定槛位（滚轮/数字键触发）。若新旧槛位的武器不同，自动卸下旧武器/
     * 装备新武器——装备状态永远与"选中槛位当前指向的武器物品"保持一致，不需要
     * 任何手动同步。
     */
    public void selectSlot(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= SELECTABLE_SLOT_COUNT) {
            throw new IllegalArgumentException("槛位索引必须在 0-" + (SELECTABLE_SLOT_COUNT - 1) + " 之间");
        }
        if (slotIndex == selectedSlotIndex) {
            return;
        }
        this.selectedSlotIndex = slotIndex;
        syncWeaponEquipState();
    }

    /**
     * 滚轮切换：往前/往后移动一格，越界不回绕（到头就停在0或8，与MC快捷栏滚轮手感一致）。
     * @param direction 正数=下一格，负数=上一格
     */
    public void scrollSlot(int direction) {
        int next = selectedSlotIndex + (direction > 0 ? 1 : -1);
        next = Math.max(0, Math.min(SELECTABLE_SLOT_COUNT - 1, next));
        selectSlot(next);
    }

    public int getSelectedSlot() {
        return selectedSlotIndex;
    }

    /**
     * 获取当前手持物品堆（背包选中格的数据，可能是EMPTY）。
     */
    public ItemStack getCurrentHeldItem() {
        return backpack.getSlot(selectedSlotIndex);
    }

    private ItemDef getCurrentItemDef() {
        ItemStack stack = getCurrentHeldItem();
        if (stack.isEmpty()) {
            return null;
        }
        return itemRegistry.getItemDef(stack.getItemId());
    }

    /**
     * 获取当前装备的武器id（当前槛位物品有weaponId时返回，否则null）。
     * 具体的Weapon实例由WeaponEquipListener的实现（PlayerCombatController）持有，
     * 本类不持有任何Weapon/Node实例。
     */
    public String getCurrentWeaponId() {
        ItemDef def = getCurrentItemDef();
        return def != null && def.hasWeaponId() ? def.getWeaponId() : null;
    }

    /**
     * 获取当前选中的方块类型（当前槛位物品有blockId时返回，否则null）。
     */
    public Block getCurrentBlock() {
        ItemDef def = getCurrentItemDef();
        if (def == null || !def.hasBlockId()) {
            return null;
        }
        return blockRegistry.getBlock(def.getBlockId());
    }

    /**
     * 检查当前是否持有武器（当前槛位物品有weaponId，或外部覆盖生效）。
     */
    public boolean isHoldingWeapon() {
        return externalWeaponOverride || getCurrentWeaponId() != null;
    }

    /**
     * 检查当前是否持有方块。外部武器覆盖生效时视为"手上没有方块"，
     * 避免持枪状态下右键依然放置方块。
     */
    public boolean isHoldingBlock() {
        return !externalWeaponOverride && getCurrentBlock() != null;
    }

    public void setExternalWeaponOverride(boolean active) {
        this.externalWeaponOverride = active;
    }

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
     * 从当前选中槛位扣减数量（方块放置消耗、丢弃等场景调用）。
     * @return 实际扣减的数量
     */
    public int removeFromCurrentSlot(int count) {
        int removed = backpack.removeFromSlot(selectedSlotIndex, count);
        // 扣完后该格可能变空，若之前是武器物品需要同步卸下装备状态
        syncWeaponEquipState();
        return removed;
    }

    /**
     * 重置为默认配置：清空前9格并放入初始方块（保留原有的"1=石头,2=泥土,3=草方块,
     * 4=玻璃"默认配置的等价物），供PlayerStateManager.reset()调用。
     */
    public void resetToDefault() {
        for (int i = 0; i < SELECTABLE_SLOT_COUNT && i < backpack.getSize(); i++) {
            backpack.clearSlot(i);
        }
        backpack.setSlot(0, new ItemStack("stone", 64));
        backpack.setSlot(1, new ItemStack("dirt", 64));
        backpack.setSlot(2, new ItemStack("grass", 64));
        backpack.setSlot(3, new ItemStack("glass", 64));
        selectedSlotIndex = 0;
        syncWeaponEquipState();
    }

    /**
     * 按当前选中槛位的物品，向 {@link WeaponEquipListener} 发出装备/卸下通知。
     * 每次selectSlot/removeFromCurrentSlot改变了"当前槛位指向什么"之后调用，
     * 保证武器装备状态永远精确匹配槛位内容，不需要任何调用方手动同步。
     */
    private void syncWeaponEquipState() {
        if (weaponEquipListener == null) {
            return;
        }
        String weaponId = getCurrentWeaponId();
        if (weaponId == null) {
            weaponEquipListener.onWeaponUnequipped();
            return;
        }
        Weapon weapon = WeaponFactory.create(weaponId);
        if (weapon == null) {
            // obtainable=true的武器理论上都应该有WeaponFactory实现（见ItemRegistry.
            // registerFromWeapons的注册前提），走到这里说明数据不一致，保守起见按
            // "无法装备"处理，不留一个假装备着但打不出子弹的状态
            weaponEquipListener.onWeaponUnequipped();
            return;
        }
        weaponEquipListener.onWeaponEquipped(weaponId, weapon);
    }

    @Override
    public String toString() {
        return "PlayerEquipment{selectedSlot=" + selectedSlotIndex +
                ", current=" + getCurrentHeldItem() + '}';
    }
}
