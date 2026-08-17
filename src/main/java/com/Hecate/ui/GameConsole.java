package com.Hecate.ui;

import com.jme3.app.SimpleApplication;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.RawInputListener;
import com.jme3.input.event.*;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Node;
import com.jme3.ui.Picture;
import com.Hecate.utils.LogUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 游戏内控制台系统
 * 按 / 键打开/关闭
 */
public class GameConsole {

    private final SimpleApplication app;
    private final Node guiNode;

    // UI组件
    private Node consoleNode;
    private Picture background;
    private BitmapText inputText;
    private BitmapText historyText;

    // 状态
    private boolean isVisible = false;
    private StringBuilder currentInput = new StringBuilder();
    private List<String> commandHistory = new ArrayList<>();
    private int maxHistoryLines = 10;

    // 命令处理器
    private Map<String, CommandHandler> commands = new HashMap<>();

    // 输入监听器
    private RawInputListener inputListener;

    // 玩家控制器引用（用于执行玩家相关命令）
    private com.Hecate.player.PlayerController playerController;

    // 命令处理器接口
    public interface CommandHandler {
        void execute(String[] args);
        String getDescription();
    }

    public GameConsole(SimpleApplication app) {
        this.app = app;
        this.guiNode = app.getGuiNode();

        initializeUI();
        setupInput();
        registerDefaultCommands();
    }

    /**
     * 初始化UI
     */
    private void initializeUI() {
        consoleNode = new Node("ConsoleUI");

        int screenWidth = app.getCamera().getWidth();
        int screenHeight = app.getCamera().getHeight();

        // 半透明黑色背景
        background = new Picture("ConsoleBackground");
        background.setWidth(screenWidth);
        background.setHeight(200);
        background.setPosition(0, screenHeight - 200);

        // 创建简单的黑色材质
        Material bgMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        bgMat.setColor("Color", new ColorRGBA(0, 0, 0, 0.8f));
        bgMat.getAdditionalRenderState().setBlendMode(com.jme3.material.RenderState.BlendMode.Alpha);
        background.setMaterial(bgMat);

        consoleNode.attachChild(background);

        // 字体
        BitmapFont font = app.getAssetManager().loadFont("Interface/Fonts/Default.fnt");

        // 输入文本
        inputText = new BitmapText(font);
        inputText.setSize(16);
        inputText.setColor(ColorRGBA.White);
        inputText.setLocalTranslation(10, screenHeight - 10, 0);
        inputText.setText("/");
        consoleNode.attachChild(inputText);

        // 历史文本
        historyText = new BitmapText(font);
        historyText.setSize(14);
        historyText.setColor(new ColorRGBA(0.8f, 0.8f, 0.8f, 1f));
        historyText.setLocalTranslation(10, screenHeight - 30, 0);
        historyText.setText("");
        consoleNode.attachChild(historyText);

        // 初始隐藏
        consoleNode.setCullHint(com.jme3.scene.Spatial.CullHint.Always);
    }

    /**
     * 设置输入监听
     */
    private void setupInput() {
        app.getInputManager().addMapping("ToggleConsole", new KeyTrigger(KeyInput.KEY_SLASH));
        app.getInputManager().addMapping("ConsoleEnter", new KeyTrigger(KeyInput.KEY_RETURN));
        app.getInputManager().addMapping("ConsoleBackspace", new KeyTrigger(KeyInput.KEY_BACK));

        app.getInputManager().addListener(new ActionListener() {
            @Override
            public void onAction(String name, boolean isPressed, float tpf) {
                if (!isPressed) return;

                switch (name) {
                    case "ToggleConsole":
                        toggle();
                        break;
                    case "ConsoleEnter":
                        if (isVisible) {
                            executeCommand();
                        }
                        break;
                    case "ConsoleBackspace":
                        if (isVisible && currentInput.length() > 0) {
                            currentInput.deleteCharAt(currentInput.length() - 1);
                            updateInputDisplay();
                        }
                        break;
                }
            }
        }, "ToggleConsole", "ConsoleEnter", "ConsoleBackspace");

        // 添加原始输入监听器来捕获字符输入
        inputListener = new RawInputListener() {
            @Override
            public void beginInput() {}

            @Override
            public void endInput() {}

            @Override
            public void onJoyAxisEvent(JoyAxisEvent evt) {}

            @Override
            public void onJoyButtonEvent(JoyButtonEvent evt) {}

            @Override
            public void onMouseMotionEvent(MouseMotionEvent evt) {}

            @Override
            public void onMouseButtonEvent(MouseButtonEvent evt) {}

            @Override
            public void onKeyEvent(KeyInputEvent evt) {
                // 只在控制台可见且按键按下时处理
                if (!isVisible || !evt.isPressed()) {
                    return;
                }

                char keyChar = evt.getKeyChar();
                int keyCode = evt.getKeyCode();

                // 忽略特殊键（已经通过ActionListener处理）
                if (keyCode == KeyInput.KEY_SLASH ||
                    keyCode == KeyInput.KEY_RETURN ||
                    keyCode == KeyInput.KEY_BACK ||
                    keyCode == KeyInput.KEY_ESCAPE) {
                    return;
                }

                // 处理字符输入
                if (keyChar != 0 && !Character.isISOControl(keyChar)) {
                    addChar(keyChar);
                }
            }

            @Override
            public void onTouchEvent(TouchEvent evt) {}
        };

        app.getInputManager().addRawInputListener(inputListener);
    }

    /**
     * 注册默认命令
     */
    private void registerDefaultCommands() {
        // help命令
        registerCommand("help", new CommandHandler() {
            @Override
            public void execute(String[] args) {
                addHistory("可用命令:");
                for (Map.Entry<String, CommandHandler> entry : commands.entrySet()) {
                    addHistory("  /" + entry.getKey() + " - " + entry.getValue().getDescription());
                }
            }

            @Override
            public String getDescription() {
                return "显示所有可用命令";
            }
        });

        // clear命令
        registerCommand("clear", new CommandHandler() {
            @Override
            public void execute(String[] args) {
                commandHistory.clear();
                updateHistoryDisplay();
            }

            @Override
            public String getDescription() {
                return "清除控制台历史";
            }
        });

        // kill命令 - 杀死玩家
        registerCommand("kill", new CommandHandler() {
            @Override
            public void execute(String[] args) {
                if (playerController == null) {
                    addHistory("错误: 玩家控制器未初始化");
                    return;
                }

                if (playerController.getPlayerHealth() == null) {
                    addHistory("错误: 玩家血量系统未初始化");
                    return;
                }

                // 造成9999点伤害，触发死亡
                app.enqueue(() -> {
                    playerController.getPlayerHealth().takeDamage(9999.0f);
                    addHistory("玩家已被杀死");
                    return null;
                });
            }

            @Override
            public String getDescription() {
                return "杀死玩家（触发死亡特效）";
            }
        });

        // hurt命令 - 造成当前血量50%的伤害
        registerCommand("hurt", new CommandHandler() {
            @Override
            public void execute(String[] args) {
                if (playerController == null) {
                    addHistory("错误: 玩家控制器未初始化");
                    return;
                }

                if (playerController.getPlayerHealth() == null) {
                    addHistory("错误: 玩家血量系统未初始化");
                    return;
                }

                app.enqueue(() -> {
                    float currentHealth = playerController.getCurrentHealth();
                    float damage = currentHealth * 0.5f;
                    playerController.getPlayerHealth().takeDamage(damage);
                    addHistory(String.format("造成 %.1f 点伤害（当前血量的50%%）", damage));
                    addHistory(String.format("剩余血量: %.1f / %.1f",
                        playerController.getCurrentHealth(),
                        playerController.getMaxHealth()));
                    return null;
                });
            }

            @Override
            public String getDescription() {
                return "造成当前血量50%的伤害";
            }
        });

        // speed命令 - 设置玩家移动速度倍数
        registerCommand("speed", new CommandHandler() {
            @Override
            public void execute(String[] args) {
                if (playerController == null) {
                    addHistory("错误: 玩家控制器未初始化");
                    return;
                }

                // 没有参数时显示当前速度
                if (args.length == 0) {
                    float currentMultiplier = playerController.getSpeedMultiplier();
                    addHistory(String.format("当前速度倍数: %.2fx", currentMultiplier));
                    addHistory("用法: /speed <倍数>");
                    addHistory("示例: /speed 1 (正常速度)");
                    addHistory("      /speed 100 (100倍速度)");
                    addHistory("      /speed 0.5 (半速)");
                    return;
                }

                // 解析速度倍数
                try {
                    float multiplier = Float.parseFloat(args[0]);

                    if (multiplier < 0.1f) {
                        addHistory("错误: 速度倍数不能小于0.1");
                        return;
                    }

                    if (multiplier > 1000f) {
                        addHistory("错误: 速度倍数不能大于1000");
                        return;
                    }

                    app.enqueue(() -> {
                        playerController.setSpeedMultiplier(multiplier);
                        addHistory(String.format("速度倍数已设置为: %.2fx", multiplier));
                        if (multiplier == 1.0f) {
                            addHistory("(正常速度)");
                        } else if (multiplier > 1.0f) {
                            addHistory(String.format("(%.0f倍正常速度)", multiplier));
                        } else {
                            addHistory(String.format("(%.0f%%正常速度)", multiplier * 100));
                        }
                        return null;
                    });

                } catch (NumberFormatException e) {
                    addHistory("错误: 无效的数字格式");
                    addHistory("用法: /speed <倍数>");
                }
            }

            @Override
            public String getDescription() {
                return "设置移动速度倍数 (如: /speed 100)";
            }
        });
    }

    /**
     * 注册命令
     */
    public void registerCommand(String command, CommandHandler handler) {
        commands.put(command.toLowerCase(), handler);
        LogUtils.debug(GameConsole.class, "注册命令: /" + command);
    }

    /**
     * 设置玩家控制器（用于玩家相关命令）
     */
    public void setPlayerController(com.Hecate.player.PlayerController playerController) {
        this.playerController = playerController;
        LogUtils.debug(GameConsole.class, "玩家控制器已设置");
    }

    /**
     * 切换控制台显示
     */
    public void toggle() {
        isVisible = !isVisible;

        if (isVisible) {
            consoleNode.setCullHint(com.jme3.scene.Spatial.CullHint.Never);
            guiNode.attachChild(consoleNode);
            currentInput.setLength(0);
            updateInputDisplay();
        } else {
            consoleNode.setCullHint(com.jme3.scene.Spatial.CullHint.Always);
            consoleNode.removeFromParent();
        }
    }

    /**
     * 添加字符到输入
     */
    public void addChar(char c) {
        if (!isVisible) return;

        if (Character.isLetterOrDigit(c) || c == ' ' || c == '-' || c == '_') {
            currentInput.append(c);
            updateInputDisplay();
        }
    }

    /**
     * 执行命令
     */
    private void executeCommand() {
        String input = currentInput.toString().trim();

        if (input.isEmpty()) {
            toggle();
            return;
        }

        addHistory("> /" + input);

        // 解析命令
        String[] parts = input.split("\\s+");
        String command = parts[0].toLowerCase();
        String[] args = new String[parts.length - 1];
        System.arraycopy(parts, 1, args, 0, args.length);

        // 执行命令
        CommandHandler handler = commands.get(command);
        if (handler != null) {
            try {
                handler.execute(args);
            } catch (Exception e) {
                addHistory("错误: " + e.getMessage());
                LogUtils.error(GameConsole.class, "命令执行失败: " + command, e);
            }
        } else {
            addHistory("未知命令: /" + command + " (输入 /help 查看可用命令)");
        }

        // 清空输入
        currentInput.setLength(0);
        updateInputDisplay();
    }

    /**
     * 添加历史记录
     */
    public void addHistory(String message) {
        commandHistory.add(message);

        // 限制历史记录行数
        while (commandHistory.size() > maxHistoryLines) {
            commandHistory.remove(0);
        }

        updateHistoryDisplay();
    }

    /**
     * 更新输入显示
     */
    private void updateInputDisplay() {
        inputText.setText("/ " + currentInput.toString() + "_");
    }

    /**
     * 更新历史显示
     */
    private void updateHistoryDisplay() {
        StringBuilder sb = new StringBuilder();
        for (int i = commandHistory.size() - 1; i >= 0; i--) {
            sb.append(commandHistory.get(i)).append("\n");
        }
        historyText.setText(sb.toString());
    }

    /**
     * 清理资源
     */
    public void cleanup() {
        if (consoleNode != null) {
            consoleNode.removeFromParent();
        }
        if (inputListener != null) {
            app.getInputManager().removeRawInputListener(inputListener);
        }
    }

    public boolean isVisible() {
        return isVisible;
    }
}
