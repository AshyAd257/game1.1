package com.Hecate.module.blender;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.Hecate.module.AbstractGameModule;
import com.Hecate.module.Version;

import java.util.*;

/**
 * Blender模块 - 负责3D模型的加载和管理
 * 支持从Blender导出的.j3o文件，以及内置几何体
 */
public class BlenderModule extends AbstractGameModule {

    // 土方块模型缓存
    private Map<String, Spatial> dirtBlockModels;
    // 土方块模型路径列表
    private List<String> dirtModelPaths;
    // 随机数生成器
    private Random random;
    // 应用程序引用
    private SimpleApplication app;
    // 资源管理器
    private AssetManager assetManager;

    public BlenderModule(SimpleApplication app) {
        this.app = app;
        this.assetManager = app.getAssetManager();
        this.dirtBlockModels = new HashMap<>();
        this.dirtModelPaths = new ArrayList<>();
        this.random = new Random();
    }

    @Override
    public String getId() {
        return "blender-module";
    }

    @Override
    public Version getVersion() {
        return new Version(1, 0, 0);
    }

    @Override
    public void onInitialize() {
        System.out.println("Blender模块: 开始初始化...");

        // 初始化模型路径
        initializeDirtModelPaths();

        // 预加载所有土方块模型
        loadDirtBlockModels();

        System.out.println("Blender模块: 初始化完成");
    }

    @Override
    public void onPostInitialize() {
        System.out.println("Blender模块: 后初始化完成");
    }

    @Override
    public void onUpdate(float tpf) {
        // 更新逻辑（如果需要）
    }

    @Override
    public void onDisable() {
        System.out.println("Blender模块: 正在禁用...");

        // 清理资源
        if (dirtBlockModels != null) {
            dirtBlockModels.clear();
        }
        if (dirtModelPaths != null) {
            dirtModelPaths.clear();
        }

        System.out.println("Blender模块: 已禁用");
    }

    /**
     * 初始化土方块模型路径
     */
    private void initializeDirtModelPaths() {
        // 外部模型文件路径 - 你的项目中有 .obj 文件，但JME需要 .j3o 文件
        String[] externalPaths = {
                "Models/dirt1.j3o",
                "Models/dirt2.j3o",
                "Models/dirt3.j3o",
                "Models/dirt4.j3o"
        };

        // 添加外部模型路径
        dirtModelPaths.addAll(Arrays.asList(externalPaths));

        System.out.println("土方块模型路径初始化完成，共 " + dirtModelPaths.size() + " 个路径");
    }

    /**
     * 预加载所有土方块模型（带详细调试信息）
     */
    private void loadDirtBlockModels() {
        System.out.println("开始加载土方块模型...");
        System.out.println("AssetManager: " + (assetManager != null ? "已初始化" : "未初始化"));

        int loadedCount = 0;

        // 尝试加载外部模型文件
        for (String modelPath : new ArrayList<>(dirtModelPaths)) {
            try {
                System.out.println("尝试加载模型: " + modelPath);

                // 检查文件是否存在
                try {
                    AssetInfo info = assetManager.locateAsset(new AssetKey<>(modelPath));
                    if (info == null) {
                        System.err.println("❌ 文件不存在: " + modelPath);
                        continue;
                    }
                    System.out.println("✅ 文件存在: " + modelPath);
                } catch (Exception e) {
                    System.err.println("❌ 无法定位文件 " + modelPath + ": " + e.getMessage());
                    continue;
                }

                // 加载模型
                Spatial model = assetManager.loadModel(modelPath);
                if (model != null) {
                    model.setLocalScale(1.0f);
                    dirtBlockModels.put(modelPath, model);
                    loadedCount++;
                    System.out.println("✅ 成功加载模型: " + modelPath);
                } else {
                    System.err.println("❌ 加载模型返回null: " + modelPath);
                }
            } catch (Exception e) {
                System.err.println("❌ 加载模型异常 " + modelPath + ": " + e.getMessage());
            }
        }

        System.out.println("外部模型加载完成，成功加载 " + loadedCount + " 个模型");

        // 如果没有加载到外部模型，创建内置几何体
        if (loadedCount == 0) {
            System.out.println("⚠️  没有找到外部模型文件，创建内置几何体...");
            createBuiltInDirtBlocks();
        }

        System.out.println("土方块模型加载完成，总共可用 " + dirtBlockModels.size() + " 个模型");
    }

    /**
     * 创建内置几何体土方块
     */
    private void createBuiltInDirtBlocks() {
        System.out.println("创建内置几何体土方块...");

        // 创建4种不同颜色的方块
        ColorRGBA[] colors = {
                ColorRGBA.Brown,                           // 标准棕色
                new ColorRGBA(0.6f, 0.4f, 0.2f, 1.0f),   // 深棕色
                new ColorRGBA(0.8f, 0.6f, 0.3f, 1.0f),   // 浅棕色
                new ColorRGBA(0.5f, 0.3f, 0.1f, 1.0f)    // 暗棕色
        };

        String[] names = {"标准土块", "深色土块", "浅色土块", "暗色土块"};

        // 清空原有路径，使用内置路径
        dirtModelPaths.clear();

        for (int i = 0; i < colors.length; i++) {
            String modelKey = "built-in-dirt-" + i;

            // 创建立方体几何体
            Box box = new Box(0.5f, 0.5f, 0.5f);
            Geometry geom = new Geometry("DirtBlock" + i, box);

            // 设置材质
            Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
            mat.setColor("Color", colors[i]);
            geom.setMaterial(mat);

            // 缓存模型
            dirtBlockModels.put(modelKey, geom);
            dirtModelPaths.add(modelKey);

            System.out.println("✅ 创建内置土方块: " + names[i] + " (" + modelKey + ")");
        }

        System.out.println("内置土方块创建完成，共创建 " + dirtBlockModels.size() + " 个方块");
    }

    /**
     * 获取随机的土方块模型
     * @return 克隆的土方块模型
     */
    public Spatial getRandomDirtBlock() {
        if (dirtBlockModels.isEmpty()) {
            System.err.println("❌ 没有可用的土方块模型！");
            return createEmergencyDirtBlock();
        }

        // 随机选择一个模型路径
        String randomPath = dirtModelPaths.get(random.nextInt(dirtModelPaths.size()));
        Spatial originalModel = dirtBlockModels.get(randomPath);

        if (originalModel != null) {
            // 克隆模型以避免共享状态
            Spatial clonedModel = originalModel.clone();
            clonedModel.setLocalScale(1.0f);
            return clonedModel;
        }

        System.err.println("❌ 无法获取模型: " + randomPath);
        return createEmergencyDirtBlock();
    }

    /**
     * 创建紧急备用土方块（当所有其他方法都失败时）
     */
    private Spatial createEmergencyDirtBlock() {
        System.out.println("创建紧急备用土方块...");

        // 创建一个简单的红色立方体作为错误指示
        Box box = new Box(0.5f, 0.5f, 0.5f);
        Geometry geom = new Geometry("EmergencyDirtBlock", box);

        // 设置红色材质表示错误
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.Red);
        geom.setMaterial(mat);

        return geom;
    }

    /**
     * 在指定位置创建随机土方块场景
     * @param centerX 中心X坐标
     * @param centerZ 中心Z坐标
     * @param width 宽度
     * @param height 高度
     * @param density 密度 (0-100)
     * @return 创建的方块数量
     */
    public int createRandomDirtField(float centerX, float centerZ, int width, int height, int density) {
        System.out.println("创建随机土方块场景...");
        System.out.println("中心位置: (" + centerX + ", " + centerZ + ")");
        System.out.println("区域大小: " + width + "x" + height);
        System.out.println("密度: " + density + "%");

        int createdCount = 0;

        // 计算起始位置
        float startX = centerX - width / 2.0f;
        float startZ = centerZ - height / 2.0f;

        // 遍历指定区域
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < height; z++) {
                // 根据密度随机决定是否放置方块
                if (random.nextInt(100) < density) {
                    // 获取随机土方块
                    Spatial dirtBlock = getRandomDirtBlock();

                    if (dirtBlock != null) {
                        // 设置位置
                        float posX = startX + x;
                        float posZ = startZ + z;
                        float posY = 0; // 地面高度

                        dirtBlock.setLocalTranslation(posX, posY, posZ);

                        // 随机旋转 (0, 90, 180, 270度)
                        float rotation = (random.nextInt(4) * 90) * (float) Math.PI / 180f;
                        dirtBlock.setLocalRotation(dirtBlock.getLocalRotation().fromAngleAxis(rotation, Vector3f.UNIT_Y));

                        // 添加到场景
                        app.getRootNode().attachChild(dirtBlock);
                        createdCount++;
                    }
                }
            }
        }

        System.out.println("装饰性土方块场景创建完成，包含 " + createdCount + " 个方块");
        return createdCount;
    }

    /**
     * 重新加载所有模型
     */
    public void reloadModels() {
        System.out.println("重新加载所有模型...");

        // 清理现有模型
        dirtBlockModels.clear();

        // 重新加载
        loadDirtBlockModels();

        System.out.println("模型重新加载完成");
    }

    /**
     * 获取可用模型数量
     */
    public int getAvailableModelCount() {
        return dirtBlockModels.size();
    }

    /**
     * 获取所有模型路径
     */
    public List<String> getModelPaths() {
        return new ArrayList<>(dirtModelPaths);
    }
}
