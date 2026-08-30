package com.Hecate.module.player;

import com.jme3.app.SimpleApplication;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.Hecate.module.AbstractGameModule;
import com.Hecate.module.Version;
import com.Hecate.block.BlockInteraction;
import com.Hecate.block.BlockBreaking;
import com.Hecate.block.BlockRegistry;
import com.Hecate.block.BlockPlacementOutline;
import com.Hecate.world.ChunkManager;
import com.Hecate.player.PlayerController;
import com.Hecate.player.inventory.PlayerStateManager;
import com.Hecate.player.inventory.HeldItem;
import com.Hecate.weapon.WeaponRegistry;
import com.Hecate.utils.LogUtils;

/**
 * 提供玩家移动和控制功能
 */
public class PlayerControlModule extends AbstractGameModule implements ActionListener {
    private static final String MODULE_ID = "player-control-module";
    private static final Version MODULE_VERSION = new Version(1, 0, 0);

    private final SimpleApplication app;
    private final BlockRegistry blockRegistry;
    private PlayerController playerController;
    private PlayerStateManager playerStateManager;

    // 方块交互系统
    private BlockInteraction blockInteraction;
    private BlockBreaking blockBreaking;
    private BlockPlacementOutline blockPlacementOutline;
    private ChunkManager chunkManager;
    private Node worldNode;

    // 半砖竖放模式：按住Alt时为true（左右前后朝向），松开为false（上下朝向，默认）。
    // Alt键目前完全未被占用（Shift=隐藏、Ctrl=普通模式），不会冲突。
    private boolean isSlabVerticalMode = false;

    public PlayerController getPlayerController() {
        return playerController;
    }

    public PlayerControlModule(SimpleApplication app, BlockRegistry blockRegistry) {
        this.app = app;
        this.blockRegistry = blockRegistry;
    }

    /**
     * @deprecated 推荐使用 {@link #PlayerControlModule(SimpleApplication, BlockRegistry)} 进行依赖注入
     */
    @Deprecated
    public PlayerControlModule(SimpleApplication app) {
        this.app = app;
        this.blockRegistry = BlockRegistry.getInstance();
    }

    @Override
    public String getId() {
        return MODULE_ID;
    }

    @Override
    public Version getVersion() {
        return MODULE_VERSION;
    }

    @Override
    public void onInitialize() {
        // 初始化玩家控制器
        playerController = new PlayerController(app);

        // 初始化玩家状态管理器（物品栏系统）
        WeaponRegistry weaponRegistry = WeaponRegistry.getInstance();
        playerStateManager = new PlayerStateManager(blockRegistry, weaponRegistry);
        playerController.setPlayerStateManager(playerStateManager);

        // 初始化快捷栏并添加默认方块（走PlayerEquipment而不是直接操作hotbar，
        // 确保currentBlock/currentWeapon缓存与槛位内容同步）
        com.Hecate.player.inventory.PlayerEquipment equipment = playerStateManager.getEquipment();
        equipment.setHotbarSlot(0, HeldItem.block("stone"));
        equipment.setHotbarSlot(1, HeldItem.block("dirt"));
        equipment.setHotbarSlot(2, HeldItem.block("grass"));
        equipment.setHotbarSlot(3, HeldItem.block("glass"));
        equipment.selectHotbarSlot(0); // 默认选中第一个槽位

        // 设置方块交互输入
        setupBlockInteractionInputs();

        // 初始化放置预览框（挂在根节点下，不依赖世界节点，可以立即创建）
        blockPlacementOutline = new BlockPlacementOutline(app.getAssetManager(), app.getRootNode());
    }

    @Override
    public void onPostInitialize() {
        // 在所有模块初始化完成后，获取世界模块的ChunkManager
        try {
            // 获取世界节点
            Node worldSpatial = (Node) app.getRootNode().getChild("WorldNode");
            if (worldSpatial != null) {
                worldNode = worldSpatial;
            } else {
                LogUtils.error(getClass(), "未找到世界节点");
                return;
            }

            // 初始化方块交互系统
            initializeBlockInteraction();
        } catch (Exception e) {
            LogUtils.error(getClass(), "初始化方块交互系统失败", e);
        }
    }

    /**
     * 初始化方块交互系统
     */
    private void initializeBlockInteraction() {
        if (chunkManager != null && worldNode != null) {
            blockInteraction = new BlockInteraction(app.getCamera(), worldNode, chunkManager, blockRegistry);
            // 交互距离用玩家本体位置计算（而不是可能被拉远到玩家身后8~15格的摄像机位置），
            // 否则第三人称镜头拉远时，即使玩家贴着方块也会被误判"太远"。射线本身仍从
            // 摄像机发出（BlockInteraction内部固定），保证"准星指哪"和"实际判定点"一致。
            if (playerController != null) {
                blockInteraction.setPlayerPositionSupplier(playerController::getPlayerPosition);
            }
            blockBreaking = new BlockBreaking(chunkManager, blockRegistry);

        } else {

        }
    }

    /**
     * 获取方块交互系统（用于 /give 等控制台命令直接放置方块）
     */
    public BlockInteraction getBlockInteraction() {
        return blockInteraction;
    }

    /**
     * 设置ChunkManager（由WorldModule调用）
     */
    public void setChunkManager(ChunkManager chunkManager) {
        this.chunkManager = chunkManager;

        // 如果worldNode也已经准备好，则初始化方块交互系统
        if (worldNode != null) {
            initializeBlockInteraction();
        }
    }

    /**
     * 设置世界节点（由WorldModule调用）
     */
    public void setWorldNode(Node worldNode) {
        this.worldNode = worldNode;

        // 传递给 PuppetPlayerController（用于阴影射线检测）
        if (playerController != null) {
            playerController.setWorldNode(worldNode);
        }

        // 如果chunkManager也已经准备好，则初始化方块交互系统
        if (chunkManager != null) {
            initializeBlockInteraction();
        }
    }

    /**
     * 设置网格管理器（由Main调用，用于墨水系统速度倍率）
     */
    public void setGridManager(com.Hecate.ink.SparseGridManager gridManager) {
        if (playerController != null) {
            playerController.setGridManager(gridManager);
            // 默认设置为B队（1=暗属性）
            playerController.setPlayerTeam(1);
        }
    }

    /**
     * 设置方块交互输入控制
     */
    private void setupBlockInteractionInputs() {
        // 方块交互控制
        app.getInputManager().addMapping("BreakBlock", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        app.getInputManager().addMapping("PlaceBlock", new MouseButtonTrigger(MouseInput.BUTTON_RIGHT));

        // 半砖竖放模式切换（按住Alt=竖放左右前后，松开=横放上下）。左右Alt都绑，
        // 参照PlayerController里NormalMode/NormalModeAlt绑定左右Ctrl的写法
        app.getInputManager().addMapping("SlabVerticalModeL", new KeyTrigger(KeyInput.KEY_LMENU));
        app.getInputManager().addMapping("SlabVerticalModeR", new KeyTrigger(KeyInput.KEY_RMENU));

        // 快捷栏选择（1-9键）
        for (int i = 0; i < 9; i++) {
            String mappingName = "SelectSlot" + i;
            app.getInputManager().addMapping(mappingName, new KeyTrigger(KeyInput.KEY_1 + i));
        }

        // 注册监听器
        app.getInputManager().addListener(this,
                "BreakBlock", "PlaceBlock", "SlabVerticalModeL", "SlabVerticalModeR",
                "SelectSlot0", "SelectSlot1", "SelectSlot2", "SelectSlot3",
                "SelectSlot4", "SelectSlot5", "SelectSlot6", "SelectSlot7", "SelectSlot8");
    }

    @Override
    public void onUpdate(float tpf) {
        // 更新玩家控制器
        if (playerController != null) {
            playerController.update(tpf);
        }

        // 更新方块破坏系统
        if (blockBreaking != null) {
            blockBreaking.updateBreaking(tpf);
        }

        // 更新放置预览框：只有手持方块且瞄准有效位置时才显示，其余情况隐藏
        updatePlacementOutline();
    }

    /**
     * 更新放置预览框（手持方块时显示将要放置的那一格，参考MC的方块选取框）
     */
    private void updatePlacementOutline() {
        if (blockPlacementOutline == null) {
            return;
        }

        if (blockInteraction == null || playerStateManager == null
                || !playerStateManager.getEquipment().isHoldingBlock()) {
            blockPlacementOutline.hide();
            return;
        }

        Vector3f previewPos = blockInteraction.previewPlacementPosition();
        blockPlacementOutline.update(previewPos);
    }

    public PlayerStateManager getPlayerStateManager() {
        return playerStateManager;
    }

    @Override
    public void onDisable() {

        // 清除输入映射
        app.getInputManager().deleteMapping("BreakBlock");
        app.getInputManager().deleteMapping("PlaceBlock");
        app.getInputManager().deleteMapping("SlabVerticalModeL");
        app.getInputManager().deleteMapping("SlabVerticalModeR");
        app.getInputManager().deleteMapping("SelectStone");
        app.getInputManager().deleteMapping("SelectDirt");
        app.getInputManager().deleteMapping("SelectGrass");
        app.getInputManager().deleteMapping("SelectGlass");

        // 删除监听器
        app.getInputManager().removeListener(this);

        // 清理放置预览框
        if (blockPlacementOutline != null) {
            blockPlacementOutline.cleanup();
        }
    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        // 方块交互
        if (name.equals("BreakBlock")) {
            if (blockInteraction != null && blockBreaking != null) {
                if (isPressed) {
                    BlockInteraction.BlockHitResult hitResult = blockInteraction.raycastBlock();
                    if (hitResult.isHit()) {
                        blockBreaking.startBreaking(hitResult.getBlockPosition());
                    }
                } else {
                    blockBreaking.stopBreaking();
                }
            }
        } else if (name.equals("PlaceBlock")) {
            // 右键行为按手持物品自动切换：手持方块时放置方块；空手/持武器时保留原有的"恢复"功能，
            // 不能同时触发——放置是瞬时动作（按下时判定一次），恢复是持续状态（跟着按住/松开）
            com.Hecate.player.inventory.PlayerEquipment equipment = playerStateManager.getEquipment();
            if (isPressed && equipment.isHoldingBlock() && blockInteraction != null) {
                blockInteraction.placeBlock(equipment.getCurrentBlock().getId(), isSlabVerticalMode);
            } else if (!equipment.isHoldingBlock() && playerController != null) {
                playerController.setRightButtonForRecovery(isPressed);
            }
        } else if (name.equals("SlabVerticalModeL") || name.equals("SlabVerticalModeR")) {
            isSlabVerticalMode = isPressed;
        } else if (name.startsWith("SelectSlot") && isPressed) {
            // 动态处理快捷栏选择（1-9键）：必须经过PlayerEquipment.selectHotbarSlot()而不是
            // 直接调用hotbar.selectSlot()，否则currentBlock/currentWeapon缓存不会刷新，
            // 导致isHoldingBlock()等查询读到切换前的旧数据

            // 反方向互斥：若正装备着Gun1/Gun2（快捷栏之外的独立武器系统），切换快捷栏
            // 槛位前先强制卸枪，确保"手持快捷栏物品"与"手持Gun1/Gun2"始终互斥
            if (playerController != null && playerController.isHoldingGunWeapon()) {
                playerController.forceUnequipGunWeapon();
            }

            int slotIndex = Integer.parseInt(name.substring("SelectSlot".length()));
            playerStateManager.getEquipment().selectHotbarSlot(slotIndex);
        }
    }
}
