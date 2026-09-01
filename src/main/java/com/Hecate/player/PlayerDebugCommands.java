package com.Hecate.player;

import com.Hecate.block.BlockInteraction;
import com.Hecate.ui.GameConsole;
import com.jme3.app.SimpleApplication;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;

/**
 * 玩家调试命令处理器
 * 负责注册和处理所有玩家相关的控制台调试命令，从 PlayerController 中抽离
 */
public class PlayerDebugCommands {

    private final SimpleApplication app;
    private final GameConsole gameConsole;
    private final PlayerCombatController combatController;
    private com.Hecate.monster.MonsterManager monsterManager;
    private Node currentWorldNode;
    // 用Supplier而不是缓存单个实例：世界切换（如进入/离开竞技场）时
    // PlayerControlModule会重新创建绑定新世界的BlockInteraction，
    // 缓存旧引用会导致/give命令永远操作切换前的那个世界
    private java.util.function.Supplier<BlockInteraction> blockInteractionSupplier;
    private com.Hecate.player.inventory.PlayerEquipment playerEquipment;
    private com.Hecate.item.Inventory backpack;
    private com.Hecate.item.world.WorldItemManager worldItemManager;

    /**
     * 位置和朝向提供者接口
     */
    public interface PlayerInfoProvider {
        Vector3f getPosition();
        float getFacing();
    }

    private PlayerInfoProvider playerInfoProvider;

    /**
     * 构造函数
     */
    public PlayerDebugCommands(SimpleApplication app, GameConsole gameConsole, PlayerCombatController combatController) {
        this.app = app;
        this.gameConsole = gameConsole;
        this.combatController = combatController;
    }

    /**
     * 设置玩家信息提供者
     */
    public void setPlayerInfoProvider(PlayerInfoProvider provider) {
        this.playerInfoProvider = provider;
    }

    /**
     * 设置怪物管理器
     */
    public void setMonsterManager(com.Hecate.monster.MonsterManager monsterManager) {
        this.monsterManager = monsterManager;
    }

    /**
     * 设置当前世界节点
     */
    public void setCurrentWorldNode(Node worldNode) {
        this.currentWorldNode = worldNode;
    }

    /**
     * 设置方块交互系统的获取方式（用于 /give 命令校验方块ID是否存在）
     * 传入的是Supplier而不是具体实例，确保世界切换后总能拿到当前激活世界的最新交互系统
     */
    public void setBlockInteractionSupplier(java.util.function.Supplier<BlockInteraction> blockInteractionSupplier) {
        this.blockInteractionSupplier = blockInteractionSupplier;
    }

    /**
     * 设置玩家装备管理器（用于 /give 命令把方块放进当前选中的快捷栏槽位）
     */
    public void setPlayerEquipment(com.Hecate.player.inventory.PlayerEquipment playerEquipment) {
        this.playerEquipment = playerEquipment;
    }

    /**
     * 设置玩家背包（用于 /giveitem 命令把物品放进背包格子容器）
     */
    public void setBackpack(com.Hecate.item.Inventory backpack) {
        this.backpack = backpack;
    }

    /**
     * 设置世界掉落物管理器（用于 /spawnitem 命令在玩家面前生成掉落物）
     */
    public void setWorldItemManager(com.Hecate.item.world.WorldItemManager worldItemManager) {
        this.worldItemManager = worldItemManager;
    }

    /**
     * 注册所有调试命令
     */
    public void registerAllCommands() {
        registerMob1Command();
        registerWave1Command();
        registerDataCommand();
        registerGiveCommand();
        registerGiveItemCommand();
        registerSpawnItemCommand();
    }

    /**
     * 注册Give命令 - 把指定方块放入玩家当前选中的快捷栏槽位
     * 用法: /give <blockId>，例如 /give wood1
     * 放入后可用左键破坏方块的同款瞄准方式，右键放置（手持方块时右键=放置，空手/持武器时右键=原恢复功能）
     */
    private void registerGiveCommand() {
        gameConsole.registerCommand("give", new GameConsole.CommandHandler() {
            @Override
            public void execute(String[] args) {
                app.enqueue(() -> {
                    try {
                        if (args.length < 1) {
                            gameConsole.addHistory("用法: /give <方块ID>");
                            gameConsole.addHistory("示例: /give wood1");
                            return null;
                        }

                        BlockInteraction blockInteraction = blockInteractionSupplier != null
                                ? blockInteractionSupplier.get() : null;
                        if (blockInteraction == null) {
                            gameConsole.addHistory("错误: 方块交互系统未初始化");
                            return null;
                        }
                        if (playerEquipment == null) {
                            gameConsole.addHistory("错误: 玩家装备系统未初始化");
                            return null;
                        }

                        String blockId = args[0];
                        if (!blockInteraction.isBlockValid(blockId)) {
                            gameConsole.addHistory("[FAIL] unknown block: " + blockId);
                            return null;
                        }
                        if (backpack == null) {
                            gameConsole.addHistory("错误: 背包系统未初始化");
                            return null;
                        }

                        // 方块本身就是物品（ItemRegistry.registerFromBlocks自动把每个可获得
                        // 方块注册成同id的ItemDef），直接把这个物品堆放进当前选中槛位——
                        // 不再需要HeldItem这层区分"手持方块"和"背包物品"，两者是同一份数据。
                        com.Hecate.item.ItemDef def = com.Hecate.item.ItemRegistry.getInstance().getItemDef(blockId);
                        int maxStack = def != null ? def.getMaxStackSize() : 64;
                        int slot = playerEquipment.getSelectedSlot();
                        backpack.setSlot(slot, new com.Hecate.item.ItemStack(blockId, maxStack));
                        gameConsole.addHistory("[OK] slot " + (slot + 1) + " = " + blockId);
                    } catch (Exception e) {
                        gameConsole.addHistory("Give命令执行失败: " + e.getMessage());
                        e.printStackTrace();
                    }
                    return null;
                });
            }

            @Override
            public String getDescription() {
                return "将指定方块放入当前选中的快捷栏槽位";
            }
        });
    }

    /**
     * 注册GiveItem命令 - 把指定物品放入玩家背包（通用格子容器，与快捷栏方块/武器是两套独立系统）
     * 用法: /giveitem <itemId> [数量]，例如 /giveitem scrap_metal 10（不填数量默认为1）
     */
    private void registerGiveItemCommand() {
        gameConsole.registerCommand("giveitem", new GameConsole.CommandHandler() {
            @Override
            public void execute(String[] args) {
                app.enqueue(() -> {
                    try {
                        if (args.length < 1) {
                            gameConsole.addHistory("用法: /giveitem <物品ID> [数量]");
                            gameConsole.addHistory("示例: /giveitem scrap_metal 10");
                            return null;
                        }
                        if (backpack == null) {
                            gameConsole.addHistory("错误: 背包系统未初始化");
                            return null;
                        }

                        String itemId = args[0];
                        int count = 1;
                        if (args.length >= 2) {
                            try {
                                count = Integer.parseInt(args[1]);
                            } catch (NumberFormatException e) {
                                gameConsole.addHistory("[FAIL] 数量必须是整数: " + args[1]);
                                return null;
                            }
                            if (count < 1) {
                                gameConsole.addHistory("[FAIL] 数量必须至少为1");
                                return null;
                            }
                        }

                        if (!com.Hecate.item.ItemRegistry.getInstance().isValidItem(itemId)) {
                            gameConsole.addHistory("[FAIL] unknown item: " + itemId);
                            return null;
                        }

                        int remaining = backpack.addItem(itemId, count);
                        int added = count - remaining;
                        if (remaining > 0) {
                            gameConsole.addHistory("[OK] 放入 " + added + " 个 " + itemId + "（背包已满，剩余 "
                                    + remaining + " 个未能放入）");
                        } else {
                            gameConsole.addHistory("[OK] 放入 " + added + " 个 " + itemId);
                        }
                    } catch (Exception e) {
                        gameConsole.addHistory("GiveItem命令执行失败: " + e.getMessage());
                        e.printStackTrace();
                    }
                    return null;
                });
            }

            @Override
            public String getDescription() {
                return "将指定物品放入背包";
            }
        });
    }

    /**
     * 注册SpawnItem命令 - 在玩家面前生成一个掉落物实体（世界里的物品，不是放进背包）
     * 用法: /spawnitem <物品ID> [数量]，例如 /spawnitem scrap_metal 5
     * 用于测试"世界掉落物→F键拾取→进背包"这条完整数据流，不依赖丢弃/死亡掉落等
     * 尚未实现的生成来源。
     */
    private void registerSpawnItemCommand() {
        gameConsole.registerCommand("spawnitem", new GameConsole.CommandHandler() {
            @Override
            public void execute(String[] args) {
                app.enqueue(() -> {
                    try {
                        if (args.length < 1) {
                            gameConsole.addHistory("用法: /spawnitem <物品ID> [数量]");
                            gameConsole.addHistory("示例: /spawnitem scrap_metal 5");
                            return null;
                        }
                        if (worldItemManager == null) {
                            gameConsole.addHistory("错误: 世界掉落物系统未初始化");
                            return null;
                        }
                        if (currentWorldNode == null) {
                            gameConsole.addHistory("错误: 当前世界节点未设置");
                            return null;
                        }
                        if (playerInfoProvider == null) {
                            gameConsole.addHistory("错误: 玩家信息提供者未设置");
                            return null;
                        }

                        String itemId = args[0];
                        int count = 1;
                        if (args.length >= 2) {
                            try {
                                count = Integer.parseInt(args[1]);
                            } catch (NumberFormatException e) {
                                gameConsole.addHistory("[FAIL] 数量必须是整数: " + args[1]);
                                return null;
                            }
                            if (count < 1) {
                                gameConsole.addHistory("[FAIL] 数量必须至少为1");
                                return null;
                            }
                        }

                        if (!com.Hecate.item.ItemRegistry.getInstance().isValidItem(itemId)) {
                            gameConsole.addHistory("[FAIL] unknown item: " + itemId);
                            return null;
                        }

                        Vector3f playerPos = playerInfoProvider.getPosition();
                        float playerFacing = playerInfoProvider.getFacing();

                        // 在玩家前方2个单位生成，与拾取交互距离(3米)相比留出一点余量，
                        // 生成后玩家不需要移动就能直接按F拾取，方便测试
                        float spawnDistance = 2.0f;
                        Vector3f spawnPos = new Vector3f(
                            playerPos.x + spawnDistance * (float) Math.sin(playerFacing),
                            playerPos.y,
                            playerPos.z + spawnDistance * (float) Math.cos(playerFacing)
                        );

                        worldItemManager.spawn(currentWorldNode, itemId, count, spawnPos);
                        gameConsole.addHistory("[OK] 在玩家前方生成 " + count + " 个 " + itemId);

                    } catch (Exception e) {
                        gameConsole.addHistory("SpawnItem命令执行失败: " + e.getMessage());
                        e.printStackTrace();
                    }
                    return null;
                });
            }

            @Override
            public String getDescription() {
                return "在玩家前方生成一个世界掉落物";
            }
        });
    }

    // /gun1、/gun2命令已移除：武器装备不再由专属命令硬编码触发，而是背包选中槛位
    // 自动同步（见PlayerEquipment.syncWeaponEquipState）。测试装备武器的方式改为
    // /giveitem steampunk_gun 1（或 sniper_rifle），放进背包后用数字键/滚轮选中该槛位
    // 即可自动装备，选到别的槛位自动卸下。

    /**
     * 注册Mob1命令 - 在玩家面前生成一只怪物
     */
    private void registerMob1Command() {
        gameConsole.registerCommand("mob1", new GameConsole.CommandHandler() {
            @Override
            public void execute(String[] args) {
                app.enqueue(() -> {
                    try {
                        if (monsterManager == null) {
                            gameConsole.addHistory("错误: 怪物系统未初始化");
                            return null;
                        }
                        if (currentWorldNode == null) {
                            gameConsole.addHistory("错误: 当前世界节点未设置");
                            return null;
                        }
                        if (playerInfoProvider == null) {
                            gameConsole.addHistory("错误: 玩家信息提供者未设置");
                            return null;
                        }

                        Vector3f playerPos = playerInfoProvider.getPosition();
                        float playerFacing = playerInfoProvider.getFacing();

                        // 在玩家前方5个单位生成怪物
                        float spawnDistance = 5.0f;
                        Vector3f spawnPos = new Vector3f(
                            playerPos.x + spawnDistance * (float) Math.sin(playerFacing),
                            playerPos.y,
                            playerPos.z + spawnDistance * (float) Math.cos(playerFacing)
                        );

                        monsterManager.spawnMonster(currentWorldNode, spawnPos);
                        gameConsole.addHistory("已在玩家前方生成一只怪物");

                    } catch (Exception e) {
                        gameConsole.addHistory("Mob1命令执行失败: " + e.getMessage());
                        e.printStackTrace();
                    }
                    return null;
                });
            }

            @Override
            public String getDescription() {
                return "在玩家前方生成一只怪物";
            }
        });
    }

    /**
     * 注册Wave1命令 - 开始三波递进的刷怪遭遇战
     */
    private void registerWave1Command() {
        gameConsole.registerCommand("wave1", new GameConsole.CommandHandler() {
            @Override
            public void execute(String[] args) {
                app.enqueue(() -> {
                    try {
                        if (monsterManager == null) {
                            gameConsole.addHistory("错误: 怪物系统未初始化");
                            return null;
                        }
                        if (currentWorldNode == null) {
                            gameConsole.addHistory("错误: 当前世界节点未设置");
                            return null;
                        }
                        if (playerInfoProvider == null) {
                            gameConsole.addHistory("错误: 玩家信息提供者未设置");
                            return null;
                        }

                        Vector3f playerPos = playerInfoProvider.getPosition();

                        // 开始三波遭遇战
                        monsterManager.startWaveEncounter(currentWorldNode);
                        gameConsole.addHistory("=== 波次战斗开始 ===");
                        gameConsole.addHistory("第1波: 3只慢速怪物");
                        gameConsole.addHistory("击杀所有怪物后自动进入下一波");

                    } catch (Exception e) {
                        gameConsole.addHistory("Wave1命令执行失败: " + e.getMessage());
                        e.printStackTrace();
                    }
                    return null;
                });
            }

            @Override
            public String getDescription() {
                return "开始三波递进的刷怪遭遇战";
            }
        });
    }

    /**
     * 注册Data命令 - 显示玩家当前所在方位（世界坐标+朝向），用于排查场景中
     * 特定物体（如某个突兀的地形/结构）相对出生点的位置
     */
    private void registerDataCommand() {
        gameConsole.registerCommand("data", new GameConsole.CommandHandler() {
            @Override
            public void execute(String[] args) {
                app.enqueue(() -> {
                    try {
                        if (playerInfoProvider == null) {
                            gameConsole.addHistory("错误: 玩家信息提供者未设置");
                            return null;
                        }

                        Vector3f pos = playerInfoProvider.getPosition();
                        float facingRad = playerInfoProvider.getFacing();
                        float facingDeg = facingRad * FastMath.RAD_TO_DEG;
                        while (facingDeg < 0) facingDeg += 360f;
                        while (facingDeg >= 360f) facingDeg -= 360f;

                        gameConsole.addHistory(String.format(
                                "坐标: X=%.1f Y=%.1f Z=%.1f", pos.x, pos.y, pos.z));
                        gameConsole.addHistory(String.format(
                                "朝向: %.1f°", facingDeg));

                    } catch (Exception e) {
                        gameConsole.addHistory("Data命令执行失败: " + e.getMessage());
                        e.printStackTrace();
                    }
                    return null;
                });
            }

            @Override
            public String getDescription() {
                return "显示玩家当前坐标和朝向";
            }
        });
    }
}
