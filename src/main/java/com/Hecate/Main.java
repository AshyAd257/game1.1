package com.Hecate;

import com.jme3.app.SimpleApplication;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.system.AppSettings;
import java.awt.DisplayMode;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;

// 导入核心系统
import com.Hecate.core.ApplicationContext;
import com.Hecate.core.SystemInitializer;

// 导入输入系统
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;

/**
 * Hecate游戏引擎主类 - 应用程序入口点
 *
 * <p><b>重构版本</b>：本类职责已大幅精简，遵循单一职责原则。
 *
 * <h3>核心职责</h3>
 * <ul>
 *   <li>应用程序配置（窗口设置、全屏模式）</li>
 *   <li>初始化流程编排（委托给 {@link SystemInitializer}）</li>
 *   <li>游戏循环（simpleUpdate）</li>
 *   <li>资源清理（destroy）</li>
 * </ul>
 *
 * <h3>重构成果</h3>
 * <p>所有系统管理职责已提取到：
 * <ul>
 *   <li>{@link ApplicationContext} - 依赖注入容器，管理所有系统实例</li>
 *   <li>{@link SystemInitializer} - 初始化流程管理，按正确顺序初始化系统</li>
 *   <li>{@link com.Hecate.core.LightingSystem} - 光照系统，封装光照和阴影逻辑</li>
 * </ul>
 *
 * <h3>代码行数对比</h3>
 * <ul>
 *   <li>重构前：920行（职责过重）</li>
 *   <li>重构后：~250行（职责清晰）</li>
 * </ul>
 *
 * @author Hecate Team
 * @version 2.0 (重构版本 - 2026-06-02)
 * @see ApplicationContext
 * @see SystemInitializer
 */
public class Main extends SimpleApplication {

    // ==================== 核心系统 ====================

    /** 应用程序上下文（统一管理所有系统依赖） */
    private ApplicationContext context;

    // ==================== 木偶编辑器 ====================

    /** 木偶动画编辑器应用（独立窗口）- 使用Object避免直接依赖 */
    private Object puppetEditorApp;

    // ==================== 临时标志 ====================

    /** 区块重新渲染标志（临时解决方案） */
    private boolean chunksReloaded = false;

    // ==================== 应用程序入口点 ====================

    /**
     * 应用程序入口点
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        Main app = new Main();

        // 创建应用程序设置
        AppSettings settings = new AppSettings(true);

        // 获取默认显示设备信息
        GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        DisplayMode displayMode = device.getDisplayMode();

        // 配置全屏模式
        settings.setFullscreen(true);
        settings.setWidth(displayMode.getWidth());
        settings.setHeight(displayMode.getHeight());
        settings.setFrequency(displayMode.getRefreshRate());
        settings.setBitsPerPixel(displayMode.getBitDepth());

        // 其他设置
        settings.setTitle("Hecate Game");
        settings.setVSync(true);  // 垂直同步，防止画面撕裂
        settings.setSamples(4);    // 抗锯齿

        // 跳过显示设置对话框
        app.setShowSettings(false);

        // 应用设置并启动
        app.setSettings(settings);
        app.start();
    }

    // ==================== 应用程序生命周期 ====================

    /**
     * 应用程序初始化入口
     *
     * <p>职责已精简为：
     * <ul>
     *   <li>注册资源路径</li>
     *   <li>配置相机和视口</li>
     *   <li>创建应用上下文</li>
     *   <li>委托给SystemInitializer初始化所有系统</li>
     *   <li>初始化木偶编辑器启动器</li>
     *   <li>清理调试UI</li>
     * </ul>
     */
    @Override
    public void simpleInitApp() {
        // 注册资源路径（让AssetManager能找到textures目录）
        assetManager.registerLocator("target/classes/", com.jme3.asset.plugins.FileLocator.class);

        // 配置相机初始位置和朝向
        cam.setLocation(new Vector3f(0, 15, 15));
        cam.lookAt(new Vector3f(0, 1, 0), Vector3f.UNIT_Y);

        // 配置视口
        viewPort.setBackgroundColor(new ColorRGBA(0.53f, 0.81f, 0.92f, 1.0f)); // 天蓝色
        setDisplayStatView(false);
        setDisplayFps(false);

        // 创建应用上下文（依赖注入容器）
        context = new ApplicationContext(this);

        // 使用SystemInitializer初始化所有系统
        SystemInitializer.initialize(context);

        // 打印系统配置信息
        SystemInitializer.printConfiguration(context);

        // 初始化木偶编辑器启动器（按I键打开独立窗口）
        initializePuppetEditorLauncher();

        // 清除调试UI元素
        removeDebugTexts();

        // 显示欢迎屏幕（TTF 字体测试）
        // showWelcomeScreen();
    }

    /**
     * 游戏循环更新
     *
     * <p>职责已精简为：
     * <ul>
     *   <li>清除调试UI（确保无调试文本显示）</li>
     *   <li>区块重新渲染机制（临时解决方案）</li>
     *   <li>委托给ApplicationContext更新所有系统</li>
     * </ul>
     *
     * @param tpf 每帧时间（秒）
     */
    @Override
    public void simpleUpdate(float tpf) {
        // 持续清除所有调试文本
        removeAllDebugTexts();

        // 【临时解决方案】区块重新渲染机制
        if (!chunksReloaded && context != null && context.getWorldModule() != null) {
            if (context.getWorldModule().getChunkManager() != null &&
                !context.getWorldModule().getChunkManager().getLoadedChunks().isEmpty()) {
                forceReloadAllChunks();
                chunksReloaded = true;
            }
        }

        // 委托给ApplicationContext更新所有系统
        if (context != null) {
            context.update(tpf);
        }
    }

    /**
     * 渲染更新（目前未使用）
     *
     * @param rm 渲染管理器
     */
    @Override
    public void simpleRender(RenderManager rm) {
        // 目前无需自定义渲染逻辑
    }

    /**
     * 应用程序销毁/清理
     */
    @Override
    public void destroy() {
        // 清理木偶编辑器独立窗口（使用反射）
        if (puppetEditorApp != null) {
            try {
                java.lang.reflect.Method stopMethod = puppetEditorApp.getClass().getMethod("stop");
                stopMethod.invoke(puppetEditorApp);
            } catch (Exception e) {
                // 忽略清理错误
            }
            puppetEditorApp = null;
        }

        // 委托给ApplicationContext清理所有系统
        if (context != null) {
            context.cleanup();
        }

        super.destroy();
    }

    // ==================== 木偶编辑器相关 ====================

    /**
     * 初始化木偶编辑器启动器
     * <p>绑定I键打开木偶编辑器独立窗口
     */
    private void initializePuppetEditorLauncher() {
        inputManager.addMapping("OpenPuppetEditor", new KeyTrigger(KeyInput.KEY_I));
        inputManager.addListener(puppetEditorActionListener, "OpenPuppetEditor");
    }

    /**
     * 木偶编辑器输入监听器
     */
    private final ActionListener puppetEditorActionListener = new ActionListener() {
        @Override
        public void onAction(String name, boolean isPressed, float tpf) {
            if (name.equals("OpenPuppetEditor") && !isPressed) {
                openPuppetEditorWindow();
            }
        }
    };

    /**
     * 打开木偶编辑器独立窗口（在新线程中启动）
     * <p>使用反射加载编辑器，避免游戏主程序直接依赖编辑器代码
     */
    private void openPuppetEditorWindow() {
        // 如果编辑器已经打开，则不重复创建
        if (puppetEditorApp != null) {
            return;
        }

        // 在新线程中启动编辑器独立窗口
        new Thread(() -> {
            try {
                // 先显示模式选择界面
                Class<?> selectionScreenClass = Class.forName("com.Hecate.puppet.editor.EditorModeSelectionScreen");
                java.lang.reflect.Method showSelectionMethod = selectionScreenClass.getMethod("showModeSelection");
                Object editorMode = showSelectionMethod.invoke(null);

                // 如果用户取消，则退出
                if (editorMode == null) {
                    System.out.println("[木偶编辑器] 用户取消启动编辑器");
                    return;
                }

                // 获取选择的模式枚举值
                String modeName = editorMode.toString();

                if ("LEGACY".equals(modeName)) {
                    // 启动传统模式编辑器
                    Class<?> editorClass = Class.forName("com.Hecate.puppet.editor.PuppetEditorApp");
                    java.lang.reflect.Method createMethod = editorClass.getMethod("createEditor");
                    puppetEditorApp = createMethod.invoke(null);

                    // 调用start方法
                    java.lang.reflect.Method startMethod = editorClass.getMethod("start");
                    startMethod.invoke(puppetEditorApp);

                    // 当编辑器窗口关闭后，清理引用（允许重新打开）
                    puppetEditorApp = null;

                } else if ("NEW".equals(modeName)) {
                    // 启动新模式编辑器
                    Class<?> newEditorClass = Class.forName("com.Hecate.puppet.newmode.NewModePuppetEditorApp");
                    java.lang.reflect.Method mainMethod = newEditorClass.getMethod("main", String[].class);
                    String[] args = new String[0];
                    mainMethod.invoke(null, (Object) args);

                    // 新模式编辑器是独立应用，不需要保存引用
                }

            } catch (ClassNotFoundException e) {
                System.err.println("[木偶编辑器] 未找到编辑器类（可能未编译）");
                e.printStackTrace();
                puppetEditorApp = null;
            } catch (Exception e) {
                System.err.println("[木偶编辑器] 启动失败: " + e.getMessage());
                e.printStackTrace();
                puppetEditorApp = null;
            }
        }).start();
    }

    // ==================== UI清理相关 ====================

    /**
     * 显示欢迎屏幕（TTF 字体测试）
     */
    private void showWelcomeScreen() {
        try {
            com.Hecate.ui.WelcomeScreen welcomeScreen = new com.Hecate.ui.WelcomeScreen(this);
            welcomeScreen.show();
        } catch (Exception e) {
            System.err.println("[Main] 欢迎屏幕显示失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 清除调试文本（仅在初始化时调用一次）
     */
    private void removeDebugTexts() {
        try {
            for (int i = guiNode.getChildren().size() - 1; i >= 0; i--) {
                com.jme3.scene.Spatial child = guiNode.getChild(i);
                String name = child.getName();

                // 清除FPS显示和统计信息
                if (name != null && (name.contains("Statistics") ||
                                    name.contains("FPS") ||
                                    name.contains("Stats") ||
                                    name.equals("DebugText"))) {
                    child.removeFromParent();
                }

                // 清除BitmapText调试元素
                if (child instanceof com.jme3.font.BitmapText) {
                    child.removeFromParent();
                }
            }
        } catch (Exception e) {
            // 忽略异常
        }
    }

    /**
     * 清除所有调试文本（在每帧update中调用）
     */
    private void removeAllDebugTexts() {
        try {
            for (int i = guiNode.getChildren().size() - 1; i >= 0; i--) {
                com.jme3.scene.Spatial child = guiNode.getChild(i);
                if (child instanceof com.jme3.font.BitmapText) {
                    child.removeFromParent();
                }
            }
        } catch (Exception e) {
            // 忽略异常
        }
    }

    // ==================== 临时解决方案 ====================

    /**
     * 强制重新渲染所有区块（临时解决方案）
     * <p>用于应用材质和网格修复
     */
    private void forceReloadAllChunks() {
        if (context != null && context.getWorldModule() != null &&
            context.getWorldModule().getChunkManager() != null) {
            try {
                com.Hecate.world.ChunkManager chunkManager = context.getWorldModule().getChunkManager();
                java.util.Map<com.Hecate.world.ChunkPosition, com.Hecate.world.Chunk> chunks =
                    chunkManager.getLoadedChunks();

                // 标记所有区块为dirty并清除渲染节点
                for (com.Hecate.world.Chunk chunk : chunks.values()) {
                    chunk.setDirty();

                    // 移除旧的渲染节点
                    com.jme3.scene.Node oldNode = chunk.getChunkNode();
                    if (oldNode != null) {
                        oldNode.removeFromParent();
                        chunk.setChunkNode(null);
                    }
                }
            } catch (Exception e) {
                System.err.println("[Main] 强制重新渲染失败: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    // ==================== Getter方法（向后兼容） ====================

    /**
     * 获取碰撞管理器实例
     * @return CollisionManager 碰撞检测管理器
     */
    public com.Hecate.physics.CollisionManager getCollisionManager() {
        return context != null ? context.getCollisionManager() : null;
    }

    /**
     * 获取指针系统实例
     * @return PointerSystem 指针系统
     */
    public com.Hecate.pointer.PointerSystem getPointerSystem() {
        return context != null ? context.getPointerSystem() : null;
    }

    /**
     * 获取玩家控制模块
     * @return PlayerControlModule 玩家控制模块
     */
    public com.Hecate.module.player.PlayerControlModule getPlayerControlModule() {
        return context != null ? context.getPlayerControlModule() : null;
    }

    /**
     * 获取世界模块
     * @return WorldModule 世界模块
     */
    public com.Hecate.module.world.WorldModule getWorldModule() {
        return context != null ? context.getWorldModule() : null;
    }

    /**
     * 获取涂墨网格管理器实例
     * @return SparseGridManager 涂墨网格管理器
     */
    public com.Hecate.ink.SparseGridManager getGridManager() {
        return context != null ? context.getGridManager() : null;
    }

    /**
     * 获取方块注册表实例（依赖注入）
     * @return BlockRegistry 方块注册表
     */
    public com.Hecate.block.BlockRegistry getBlockRegistry() {
        return context != null ? context.getBlockRegistry() : null;
    }

    /**
     * 获取Blender模型注册表实例（依赖注入）
     * @return BlenderModelRegistry Blender模型注册表
     */
    public com.Hecate.blender.BlenderModelRegistry getBlenderModelRegistry() {
        return context != null ? context.getBlenderModelRegistry() : null;
    }

    /**
     * 获取Blockbench模型注册表实例（依赖注入）
     * @return BlockbenchModelRegistry Blockbench模型注册表
     */
    public com.Hecate.blockbench.BlockbenchModelRegistry getBlockbenchModelRegistry() {
        return context != null ? context.getBlockbenchModelRegistry() : null;
    }

    /**
     * 获取应用上下文（提供对所有系统的访问）
     * @return ApplicationContext 应用上下文
     */
    public ApplicationContext getApplicationContext() {
        return context;
    }
}
