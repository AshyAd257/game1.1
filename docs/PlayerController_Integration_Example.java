package com.Hecate.player;

import com.Hecate.player.inventory.PlayerStateManager;
import com.Hecate.player.inventory.HeldItem;
import com.Hecate.block.BlockRegistry;
import com.Hecate.weapon.WeaponRegistry;
import com.Hecate.weapon.Weapon;
import com.Hecate.block.Block;

/**
 * PlayerController 集成新状态系统的示例代码
 *
 * 使用说明：
 * 1. 将此文件的代码片段复制到现有的 PlayerController.java 中
 * 2. 移除旧的硬编码字段和方法
 * 3. 更新按键映射逻辑使用快捷栏系统
 */
public class PlayerControllerIntegrationExample {

    // ==================== 在 PlayerController 类中添加以下字段 ====================

    /**
     * 玩家状态管理器（替代硬编码的 currentWeapon 和 selectedBlockType）
     */
    private PlayerStateManager playerStateManager;

    // ==================== 在构造函数/初始化方法中添加 ====================

    public void initializePlayerState(BlockRegistry blockRegistry, WeaponRegistry weaponRegistry) {
        // 创建状态管理器
        playerStateManager = new PlayerStateManager(blockRegistry, weaponRegistry);

        // 配置默认快捷栏（可选，已有默认配置）
        playerStateManager.getEquipment().setHotbarSlot(0, HeldItem.block("stone"));
        playerStateManager.getEquipment().setHotbarSlot(1, HeldItem.block("dirt"));
        playerStateManager.getEquipment().setHotbarSlot(2, HeldItem.block("grass"));
        playerStateManager.getEquipment().setHotbarSlot(3, HeldItem.block("glass"));
        playerStateManager.getEquipment().setHotbarSlot(4, HeldItem.weapon("smg_01"));
        // 槽位 5-9 默认为空手

        // 选中第一个槽位
        playerStateManager.getEquipment().selectHotbarSlot(0);
    }

    // ==================== 在 update() 方法中添加 ====================

    public void updatePlayerState(float tpf) {
        // 更新效果系统（Buff/Debuff 过期检查）
        playerStateManager.update(tpf);

        // 应用速度效果到移动
        float speedMultiplier = playerStateManager.getSpeedMultiplier();
        // 将 speedMultiplier 应用到移动速度计算中

        // 检查眩晕状态（眩晕时禁用移动）
        if (playerStateManager.isStunned()) {
            // 禁用移动输入
            return;
        }
    }

    // ==================== 替换旧的按键映射逻辑 ====================

    /**
     * 旧代码（需要删除）：
     *
     * inputManager.addMapping("SelectStone", new KeyTrigger(KeyInput.KEY_1));
     * inputManager.addMapping("SelectDirt", new KeyTrigger(KeyInput.KEY_2));
     * ...
     *
     * case "SelectStone": selectedBlockType = "stone"; break;
     * case "SelectDirt": selectedBlockType = "dirt"; break;
     * ...
     */

    /**
     * 新代码：统一的快捷栏选择
     */
    public void setupHotbarInput(com.jme3.input.InputManager inputManager) {
        // 注册数字键 1-9 映射
        for (int i = 0; i < 9; i++) {
            String mappingName = "SelectSlot" + i;
            int keyCode = com.jme3.input.KeyInput.KEY_1 + i;
            inputManager.addMapping(mappingName, new com.jme3.input.controls.KeyTrigger(keyCode));
            inputManager.addListener(actionListener, mappingName);
        }
    }

    /**
     * 新的输入监听器逻辑
     */
    private final com.jme3.input.controls.ActionListener actionListener = new com.jme3.input.controls.ActionListener() {
        @Override
        public void onAction(String name, boolean isPressed, float tpf) {
            if (!isPressed) return;

            // 处理快捷栏选择
            if (name.startsWith("SelectSlot")) {
                int slotIndex = Integer.parseInt(name.substring(10));  // 提取数字
                playerStateManager.getEquipment().selectHotbarSlot(slotIndex);
                onHotbarSlotChanged(slotIndex);
            }
        }
    };

    // ==================== 获取当前装备的方法（替换旧的访问方式） ====================

    /**
     * 获取当前武器（替代直接访问 currentWeapon 字段）
     */
    public Weapon getCurrentWeapon() {
        return playerStateManager.getEquipment().getCurrentWeapon();
    }

    /**
     * 获取当前方块（替代 selectedBlockType 字符串）
     */
    public Block getCurrentBlock() {
        return playerStateManager.getEquipment().getCurrentBlock();
    }

    /**
     * 检查是否持有武器
     */
    public boolean isHoldingWeapon() {
        return playerStateManager.getEquipment().isHoldingWeapon();
    }

    /**
     * 检查是否持有方块
     */
    public boolean isHoldingBlock() {
        return playerStateManager.getEquipment().isHoldingBlock();
    }

    // ==================== 快捷栏切换事件处理 ====================

    /**
     * 快捷栏槽位切换时触发（用于更新手持物品模型）
     */
    private void onHotbarSlotChanged(int newSlot) {
        // 移除旧的手持物品模型
        if (currentHeldItemNode != null) {
            currentHeldItemNode.removeFromParent();
            currentHeldItemNode = null;
        }

        // 根据新装备类型加载模型
        if (playerStateManager.getEquipment().isHoldingWeapon()) {
            // 加载武器模型
            Weapon weapon = getCurrentWeapon();
            // loadWeaponModel(weapon);
        } else if (playerStateManager.getEquipment().isHoldingBlock()) {
            // 加载方块预览模型
            Block block = getCurrentBlock();
            // loadBlockPreviewModel(block);
        }
        // 空手时不显示模型
    }

    // ==================== 效果系统使用示例 ====================

    /**
     * 应用速度提升效果
     */
    public void applySpeedBoost(float duration) {
        var effectManager = playerStateManager.getEffectManager();
        effectManager.applyEffect("speed_boost");
    }

    /**
     * 应用中毒效果
     */
    public void applyPoison(int stacks) {
        var effectManager = playerStateManager.getEffectManager();
        for (int i = 0; i < stacks; i++) {
            effectManager.applyEffect("poison");
        }
    }

    /**
     * 玩家受到伤害时检查无敌状态
     */
    public void takeDamage(float damage) {
        if (playerStateManager.isInvincible()) {
            return;  // 无敌时免疫伤害
        }

        // 应用伤害倍率（抗性效果）
        float actualDamage = damage;  // 这里可以加入抗性计算

        // 扣除生命值...

        // 受伤后给予短暂无敌帧
        playerStateManager.getEffectManager().applyEffect("invincible");
    }

    /**
     * 玩家攻击时应用伤害倍率
     */
    public float calculateAttackDamage(float baseDamage) {
        float damageMultiplier = playerStateManager.getDamageMultiplier();
        return baseDamage * damageMultiplier;
    }

    // ==================== 需要删除的旧代码标记 ====================

    /*
     * 以下字段和方法应该被删除：
     *
     * - private Weapon currentWeapon;  (PlayerController.java:144)
     * - currentWeapon = BasicShooter.createDefault();  (PlayerController.java:367)
     * - private String selectedBlockType = "stone";  (PlayerControlModule.java:40)
     * - case "SelectStone": selectedBlockType = "stone"; break;  (PlayerControlModule.java:218-242)
     * - inputManager.addMapping("SelectStone", ...);  (PlayerControlModule.java:149-152)
     */
}
