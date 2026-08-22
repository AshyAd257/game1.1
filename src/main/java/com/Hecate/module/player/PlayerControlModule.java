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
import com.Hecate.world.ChunkManager;
import com.Hecate.player.PlayerController;
import com.Hecate.player.inventory.PlayerStateManager;
import com.Hecate.player.inventory.PlayerHotbar;
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
    private PlayerController playerController;
    private PlayerStateManager playerStateManager;

    // 方块交互系统
    private BlockInteraction blockInteraction;
    private BlockBreaking blockBreaking;
    private ChunkManager chunkManager;
    private Node worldNode;

    public PlayerController getPlayerController() {
        return playerController;
    }

    public PlayerControlModule(SimpleApplication app) {
        this.app = app;
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
        BlockRegistry blockRegistry = BlockRegistry.getInstance();
        WeaponRegistry weaponRegistry = WeaponRegistry.getInstance();
        playerStateManager = new PlayerStateManager(blockRegistry, weaponRegistry);
        playerController.setPlayerStateManager(playerStateManager);

        // 初始化快捷栏并添加默认方块
        PlayerHotbar hotbar = playerStateManager.getEquipment().getHotbar();
        hotbar.setSlot(0, HeldItem.block("stone"));
        hotbar.setSlot(1, HeldItem.block("dirt"));
        hotbar.setSlot(2, HeldItem.block("grass"));
        hotbar.setSlot(3, HeldItem.block("glass"));
        hotbar.selectSlot(0); // 默认选中第一个槽位

        // 设置方块交互输入
        setupBlockInteractionInputs();
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
            blockInteraction = new BlockInteraction(app.getCamera(), worldNode, chunkManager);
            blockBreaking = new BlockBreaking(chunkManager);

        } else {

        }
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

        // 快捷栏选择（1-9键）
        for (int i = 0; i < 9; i++) {
            String mappingName = "SelectSlot" + i;
            app.getInputManager().addMapping(mappingName, new KeyTrigger(KeyInput.KEY_1 + i));
        }

        // 注册监听器
        app.getInputManager().addListener(this,
                "BreakBlock", "PlaceBlock",
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
    }

    public PlayerStateManager getPlayerStateManager() {
        return playerStateManager;
    }

    @Override
    public void onDisable() {

        // 清除输入映射
        app.getInputManager().deleteMapping("BreakBlock");
        app.getInputManager().deleteMapping("PlaceBlock");
        app.getInputManager().deleteMapping("SelectStone");
        app.getInputManager().deleteMapping("SelectDirt");
        app.getInputManager().deleteMapping("SelectGrass");
        app.getInputManager().deleteMapping("SelectGlass");

        // 删除监听器
        app.getInputManager().removeListener(this);
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
            // 右键用于恢复，不再用于放置方块
            if (playerController != null) {
                playerController.setRightButtonForRecovery(isPressed);
            }
        } else if (name.startsWith("SelectSlot") && isPressed) {
            // 动态处理快捷栏选择（1-9键）
            int slotIndex = Integer.parseInt(name.substring("SelectSlot".length()));
            PlayerHotbar hotbar = playerStateManager.getEquipment().getHotbar();
            hotbar.selectSlot(slotIndex);

            HeldItem selectedItem = hotbar.getCurrentItem();


        }
    }
}
