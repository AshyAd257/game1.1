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
import com.Hecate.world.ChunkManager;
import com.Hecate.player.PlayerController;

/**
 * 提供玩家移动和控制功能
 */
public class PlayerControlModule extends AbstractGameModule implements ActionListener {
    private static final String MODULE_ID = "player-control-module";
    private static final Version MODULE_VERSION = new Version(1, 0, 0);

    private final SimpleApplication app;
    private PlayerController playerController;

    // 方块交互系统
    private BlockInteraction blockInteraction;
    private BlockBreaking blockBreaking;
    private ChunkManager chunkManager;
    private Node worldNode;

    public PlayerController getPlayerController() {
        return playerController;
    }

    // 当前选中的方块类型
    private String selectedBlockType = "stone";

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
        System.out.println("玩家控制模块: 初始化中");

        // 初始化玩家控制器
        playerController = new PlayerController(app);

        // 设置方块交互输入
        setupBlockInteractionInputs();

        System.out.println("玩家控制模块: 初始化完成");
    }

    @Override
    public void onPostInitialize() {
        // 在所有模块初始化完成后，获取世界模块的ChunkManager
        try {
            // 获取世界节点
            Node worldSpatial = (Node) app.getRootNode().getChild("WorldNode");
            if (worldSpatial != null) {
                worldNode = worldSpatial;
                System.out.println("成功获取世界节点");
            } else {
                System.err.println("未找到世界节点");
                return;
            }

            // 初始化方块交互系统
            initializeBlockInteraction();
        } catch (Exception e) {
            System.err.println("初始化方块交互系统失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 初始化方块交互系统
     */
    private void initializeBlockInteraction() {
        if (chunkManager != null && worldNode != null) {
            blockInteraction = new BlockInteraction(app.getCamera(), worldNode, chunkManager);
            blockBreaking = new BlockBreaking(chunkManager);
            System.out.println("方块交互系统初始化完成");
        } else {
            System.out.println("等待ChunkManager和WorldNode初始化...");
        }
    }

    /**
     * 设置ChunkManager（由WorldModule调用）
     */
    public void setChunkManager(ChunkManager chunkManager) {
        this.chunkManager = chunkManager;
        System.out.println("PlayerControlModule: 接收到ChunkManager");

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
        System.out.println("PlayerControlModule: 接收到WorldNode");

        // 如果chunkManager也已经准备好，则初始化方块交互系统
        if (chunkManager != null) {
            initializeBlockInteraction();
        }
    }

    /**
     * 设置方块交互输入控制
     */
    private void setupBlockInteractionInputs() {
        // 方块交互控制
        app.getInputManager().addMapping("BreakBlock", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        app.getInputManager().addMapping("PlaceBlock", new MouseButtonTrigger(MouseInput.BUTTON_RIGHT));

        // 方块选择
        app.getInputManager().addMapping("SelectStone", new KeyTrigger(KeyInput.KEY_1));
        app.getInputManager().addMapping("SelectDirt", new KeyTrigger(KeyInput.KEY_2));
        app.getInputManager().addMapping("SelectGrass", new KeyTrigger(KeyInput.KEY_3));
        app.getInputManager().addMapping("SelectGlass", new KeyTrigger(KeyInput.KEY_4));

        // 注册监听器
        app.getInputManager().addListener(this,
                "BreakBlock", "PlaceBlock",
                "SelectStone", "SelectDirt", "SelectGrass", "SelectGlass");
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

    @Override
    public void onDisable() {
        System.out.println("玩家控制模块: 正在禁用");
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
        switch (name) {
            case "BreakBlock":
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
                break;

            case "PlaceBlock":
                if (isPressed && blockInteraction != null) {
                    blockInteraction.placeBlock(selectedBlockType);
                }
                break;

            // 方块选择
            case "SelectStone":
                if (isPressed) {
                    selectedBlockType = "stone";
                    System.out.println("选择方块: 石头");
                }
                break;
            case "SelectDirt":
                if (isPressed) {
                    selectedBlockType = "dirt";
                    System.out.println("选择方块: 泥土");
                }
                break;
            case "SelectGrass":
                if (isPressed) {
                    selectedBlockType = "grass";
                    System.out.println("选择方块: 草方块");
                }
                break;
            case "SelectGlass":
                if (isPressed) {
                    selectedBlockType = "glass";
                    System.out.println("选择方块: 玻璃");
                }
                break;
        }
    }
}
