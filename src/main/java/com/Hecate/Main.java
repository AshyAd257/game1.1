package com.Hecate;

import com.jme3.app.SimpleApplication;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Box;
import com.jme3.system.AppSettings;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;

// 导入模块系统
import com.Hecate.module.world.WorldModule;
import com.Hecate.module.player.PlayerControlModule;

// 导入碰撞系统
import com.Hecate.physics.CollisionManager;

// 🎯 导入指针系统
import com.Hecate.pointer.PointerSystem;

/**
 * Hecate游戏主类
 * 集成了世界系统、玩家控制、碰撞检测和指针系统
 */
public class Main extends SimpleApplication {

    // 模块系统
    private WorldModule worldModule;
    private PlayerControlModule playerControlModule;

    // 碰撞系统
    private CollisionManager collisionManager;

    // 🎯 指针系统
    private PointerSystem pointerSystem;

    public static void main(String[] args) {
        Main app = new Main();

        // 配置应用设置
        AppSettings settings = new AppSettings(true);
        settings.setTitle("Hecate - 模块化体素游戏 [指针系统已集成]");
        settings.setResolution(1280, 720);
        settings.setVSync(true);
        settings.setSamples(4); // 抗锯齿
        app.setSettings(settings);

        // 显示设置对话框
        app.setShowSettings(false);

        // 启动应用
        app.start();
    }

    @Override
    public void simpleInitApp() {
        System.out.println("🚀 Hecate游戏启动中...");

        // 设置相机初始位置
        cam.setLocation(new Vector3f(0, 15, 15));
        cam.lookAt(new Vector3f(0, 1, 0), Vector3f.UNIT_Y);

        // 设置光照
        setupLighting();

        // 隐藏统计信息（可选）
        setDisplayStatView(false);
        setDisplayFps(true);

        // 初始化碰撞系统
        initializeCollisionSystem();

        // 初始化模块系统
        initializeModules();

        // 🎯 初始化指针系统
        initializePointerSystem();

        // 连接系统
        connectSystems();

        System.out.println("✅ Hecate游戏启动完成！");
        System.out.println("📋 控制说明:");
        System.out.println("   WASD - 移动 (自动调整朝向)");
        System.out.println("   空格 - 跳跃");
        System.out.println("   鼠标 - 控制相机");
        System.out.println("   滚轮 - 缩放相机");
        System.out.println("   R键 - 重置相机");
        System.out.println("   左键 - 破坏方块");
        System.out.println("   右键 - 放置方块");
        System.out.println("   1234 - 选择方块类型");
        System.out.println("🎯 指针系统:");
        System.out.println("   十字线 - 显示瞄准点");
        System.out.println("   黄色框 - 高亮目标方块");
        System.out.println("   左上角 - 显示朝向和目标信息");
    }

    /**
     * 初始化碰撞系统
     */
    private void initializeCollisionSystem() {
        System.out.println("🔧 初始化碰撞系统...");
        collisionManager = new CollisionManager();
        System.out.println("✅ 碰撞系统初始化完成");
    }

    /**
     * 初始化模块系统
     */
    private void initializeModules() {
        System.out.println("🔧 初始化模块系统...");

        try {
            // 初始化世界模块
            worldModule = new WorldModule(this);
            worldModule.onInitialize();
            System.out.println("✅ 世界模块初始化完成");

            // 初始化玩家控制模块
            playerControlModule = new PlayerControlModule(this);
            playerControlModule.onInitialize();
            System.out.println("✅ 玩家控制模块初始化完成");

            // 后初始化阶段
            worldModule.onPostInitialize();
            playerControlModule.onPostInitialize();
            System.out.println("✅ 模块后初始化完成");

        } catch (Exception e) {
            System.err.println("❌ 模块初始化失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 🎯 初始化指针系统
     */
    private void initializePointerSystem() {
        System.out.println("🎯 初始化指针系统...");

        try {
            // 确保玩家控制器已经创建
            if (playerControlModule != null && playerControlModule.getPlayerController() != null) {
                pointerSystem = new PointerSystem(this, playerControlModule.getPlayerController());
                System.out.println("✅ 指针系统初始化完成");
                System.out.println("🧭 玩家朝向控制: WASD自动调整");
                System.out.println("🎯 射线起点: 玩家中心向前");
                System.out.println("🔫 武器联动: 已预留接口");
            } else {
                System.err.println("❌ 指针系统初始化失败: 玩家控制器未就绪");
            }
        } catch (Exception e) {
            System.err.println("❌ 指针系统初始化失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 连接各个系统
     */
    private void connectSystems() {
        System.out.println("🔗 连接系统组件...");

        try {
            // 将ChunkManager连接到碰撞系统
            if (worldModule != null && worldModule.getChunkManager() != null) {
                collisionManager.setChunkManager(worldModule.getChunkManager());
                System.out.println("✅ 碰撞系统已连接到世界系统");
            }

            // 连接玩家控制模块到世界模块
            if (playerControlModule != null && worldModule != null) {
                playerControlModule.setChunkManager(worldModule.getChunkManager());
                playerControlModule.setWorldNode(worldModule.getWorldNode());

                // 将碰撞系统连接到PlayerControlModule的PlayerController
                if (playerControlModule.getPlayerController() != null) {
                    playerControlModule.getPlayerController().setCollisionManager(collisionManager);
                    System.out.println("✅ 玩家控制器已连接到碰撞系统");
                }

                System.out.println("✅ 玩家控制模块已连接到世界模块");
            }

            // 🎯 验证指针系统连接
            if (pointerSystem != null) {
                System.out.println("✅ 指针系统已就绪，等待武器系统连接");
            }

        } catch (Exception e) {
            System.err.println("❌ 系统连接失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void simpleUpdate(float tpf) {
        // 更新模块
        if (worldModule != null) {
            worldModule.onUpdate(tpf);
        }

        if (playerControlModule != null) {
            playerControlModule.onUpdate(tpf);
        }

        // 🎯 更新指针系统
        if (pointerSystem != null) {
            pointerSystem.update(tpf);
        }
    }

    @Override
    public void simpleRender(RenderManager rm) {
        // 渲染逻辑（如果需要）
    }

    @Override
    public void destroy() {
        System.out.println("🛑 Hecate游戏正在关闭...");

        // 🎯 清理指针系统
        if (pointerSystem != null) {
            System.out.println("🎯 指针系统已清理");
        }

        // 清理模块
        if (playerControlModule != null) {
            playerControlModule.onDisable();
        }

        if (worldModule != null) {
            worldModule.onDisable();
        }

        System.out.println("✅ Hecate游戏已安全关闭");
        super.destroy();
    }

    // 🎯 公共访问器方法（为未来的武器系统预留）

    /**
     * 获取指针系统实例
     * @return PointerSystem 指针系统
     */
    public PointerSystem getPointerSystem() {
        return pointerSystem;
    }

    /**
     * 获取玩家控制模块
     * @return PlayerControlModule 玩家控制模块
     */
    public PlayerControlModule getPlayerControlModule() {
        return playerControlModule;
    }

    /**
     * 获取世界模块
     * @return WorldModule 世界模块
     */
    public WorldModule getWorldModule() {
        return worldModule;
    }

    /**
     * 设置光照系统
     */
    private void setupLighting() {
        // 环境光（整体亮度）
        AmbientLight ambient = new AmbientLight();
        ambient.setColor(ColorRGBA.White.mult(0.3f)); // 30%环境光
        rootNode.addLight(ambient);

        // 方向光（太阳光）
        DirectionalLight sun = new DirectionalLight();
        sun.setDirection(new Vector3f(-1f, -1f, -1f).normalizeLocal());
        sun.setColor(ColorRGBA.White.mult(0.8f)); // 80%方向光
        rootNode.addLight(sun);

        System.out.println("💡 光照系统已设置: 环境光30% + 方向光80%");
    }

    /**
     * 获取碰撞管理器
     * @return CollisionManager 碰撞管理器
     */
    public CollisionManager getCollisionManager() {
        return collisionManager;
    }
}