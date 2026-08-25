package com.Hecate.player;

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
     * 注册所有调试命令
     */
    public void registerAllCommands() {
        registerGun1Command();
        registerGun2Command();
        registerMob1Command();
        registerWave1Command();
        registerDataCommand();
    }

    /**
     * 注册Gun1命令 - 装备/卸下蒸汽朋克枪模型
     */
    private void registerGun1Command() {
        gameConsole.registerCommand("gun1", new GameConsole.CommandHandler() {
            @Override
            public void execute(String[] args) {
                app.enqueue(() -> {
                    try {
                        if (combatController.isHoldingGun() && combatController.getCurrentWeapon() instanceof com.Hecate.weapon.SteampunkGun) {
                            // 卸下武器
                            if (combatController.unequipGun1()) {
                                gameConsole.addHistory("蒸汽朋克枪已卸下");
                                gameConsole.addHistory("已切换回默认武器");
                                gameConsole.addHistory("已退出持枪状态");
                            }
                        } else {
                            // 装备武器
                            if (combatController.equipGun1()) {
                                gameConsole.addHistory("蒸汽朋克枪已装备");
                                gameConsole.addHistory("左键: 开火");
                                gameConsole.addHistory("再次输入 /gun1 可卸下武器");
                            } else {
                                gameConsole.addHistory("装备Gun1失败");
                            }
                        }
                    } catch (Exception e) {
                        gameConsole.addHistory("Gun1命令执行失败: " + e.getMessage());
                        e.printStackTrace();
                    }
                    return null;
                });
            }

            @Override
            public String getDescription() {
                return "装备/卸下蒸汽朋克枪";
            }
        });
    }

    /**
     * 注册Gun2命令 - 装备/卸下狙击枪
     */
    private void registerGun2Command() {
        gameConsole.registerCommand("gun2", new GameConsole.CommandHandler() {
            @Override
            public void execute(String[] args) {
                app.enqueue(() -> {
                    try {
                        if (combatController.isHoldingGun() && combatController.getCurrentWeapon() instanceof com.Hecate.weapon.SniperRifle) {
                            // 卸下武器
                            if (combatController.unequipGun2()) {
                                gameConsole.addHistory("狙击枪已卸下");
                                gameConsole.addHistory("已切换回默认武器");
                                gameConsole.addHistory("已退出持枪状态");
                            }
                        } else {
                            // 装备武器
                            if (combatController.equipGun2()) {
                                gameConsole.addHistory("狙击枪已装备（占位方块模型）");
                                gameConsole.addHistory("左键: 发射子弹");
                                gameConsole.addHistory("子弹命中地面会留下涂墨");
                                gameConsole.addHistory("再次输入 /gun2 可卸下武器");
                            } else {
                                gameConsole.addHistory("装备Gun2失败");
                            }
                        }
                    } catch (Exception e) {
                        gameConsole.addHistory("Gun2命令执行失败: " + e.getMessage());
                        e.printStackTrace();
                    }
                    return null;
                });
            }

            @Override
            public String getDescription() {
                return "装备/卸下狙击枪（占位方块）";
            }
        });
    }

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
