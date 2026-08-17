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
import com.jme3.texture.Texture;
import com.Hecate.module.AbstractGameModule;
import com.Hecate.module.Version;

import java.util.*;

/**
 * 改进的Blender模块 - 支持OBJ+MTL文件加载
 * 正确处理几何体、材质和贴图的关联
 */
public class BlenderModule extends AbstractGameModule {

    // 模型配置类
    public static class ModelConfig {
        public final String modelId;
        public final String objPath;
        public final String mtlPath;
        public final Map<String, String> textureOverrides;

        public ModelConfig(String modelId, String objPath, String mtlPath) {
            this.modelId = modelId;
            this.objPath = objPath;
            this.mtlPath = mtlPath;
            this.textureOverrides = new HashMap<>();
        }

        public ModelConfig(String modelId, String objPath, String mtlPath, Map<String, String> textureOverrides) {
            this.modelId = modelId;
            this.objPath = objPath;
            this.mtlPath = mtlPath;
            this.textureOverrides = textureOverrides != null ? textureOverrides : new HashMap<>();
        }
    }

    // 土方块模型缓存
    private Map<String, Spatial> dirtBlockModels;
    // 模型配置列表
    private List<ModelConfig> modelConfigs;
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
        this.modelConfigs = new ArrayList<>();
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

        // 初始化模型配置
        initializeModelConfigs();

        // 预加载所有土方块模型
        loadDirtBlockModels();

    }

    @Override
    public void onPostInitialize() {

    }

    @Override
    public void onUpdate(float tpf) {
        // 更新逻辑（如果需要）
    }

    @Override
    public void onDisable() {

        if (dirtBlockModels != null) {
            dirtBlockModels.clear();
        }
        if (modelConfigs != null) {
            modelConfigs.clear();
        }

    }

    /**
     * 初始化模型配置
     */
    private void initializeModelConfigs() {
        // 方案1: 使用现有的OBJ文件（需要创建对应的MTL文件）
        modelConfigs.add(new ModelConfig(
                "dirt1",
                "Models/blocks/drt1.obj",
                "Models/blocks/drt1.mtl"
        ));

        // 方案2: 如果你有更多模型文件
        // modelConfigs.add(new ModelConfig(
        //     "dirt2",
        //     "Models/blocks/drt2.obj",
        //     "Models/blocks/drt2.mtl"
        // ));

        // 方案3: 使用纹理覆盖（当MTL文件不存在或需要自定义材质时）
        Map<String, String> customTextures = new HashMap<>();
        customTextures.put("diffuse", "textures/blocks/dirt.png");
        modelConfigs.add(new ModelConfig(
                "dirt_custom",
                "Models/blocks/drt1.obj",
                null, // 没有MTL文件
                customTextures
        ));

    }

    /**
     * 加载土方块模型（支持OBJ+MTL）
     */
    private void loadDirtBlockModels() {

        int loadedCount = 0;

        for (ModelConfig config : modelConfigs) {
            try {

                Spatial model = loadModelFromConfig(config);
                if (model != null) {
                    model.setLocalScale(1.0f);
                    dirtBlockModels.put(config.modelId, model);
                    loadedCount++;

                } else {
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // 如果没有加载到外部模型，创建内置几何体
        if (loadedCount == 0) {
            createBuiltInDirtBlocks();
        }

    }

    /**
     * 从配置加载模型
     */
    private Spatial loadModelFromConfig(ModelConfig config) {
        // 检查OBJ文件是否存在
        if (!checkFileExists(config.objPath)) {
            return null;
        }

        try {
            // 加载OBJ文件
            Spatial model = assetManager.loadModel(config.objPath);
            if (model == null) {
                return null;
            }

            // 处理材质
            if (config.mtlPath != null && checkFileExists(config.mtlPath)) {
                // MTL文件存在，JME3应该自动加载材质

                // 注意：JME3通常会自动查找与OBJ同名的MTL文件
            } else {
                // MTL文件不存在或为null，应用自定义材质

                applyCustomMaterial(model, config);
            }

            return model;

        } catch (Exception e) {

            e.printStackTrace();
            return null;
        }
    }

    /**
     * 应用自定义材质
     */
    private void applyCustomMaterial(Spatial model, ModelConfig config) {
        if (model instanceof Geometry) {
            applyMaterialToGeometry((Geometry) model, config);
        } else if (model instanceof com.jme3.scene.Node) {
            // 递归处理节点中的所有几何体
            com.jme3.scene.Node node = (com.jme3.scene.Node) model;
            for (Spatial child : node.getChildren()) {
                applyCustomMaterial(child, config);
            }
        }
    }

    /**
     * 为几何体应用材质
     */
    private void applyMaterialToGeometry(Geometry geometry, ModelConfig config) {
        try {
            Material material = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");

            // 应用漫反射贴图
            if (config.textureOverrides.containsKey("diffuse")) {
                String texturePath = config.textureOverrides.get("diffuse");
                if (checkFileExists(texturePath)) {
                    Texture diffuseTexture = assetManager.loadTexture(texturePath);
                    material.setTexture("DiffuseMap", diffuseTexture);

                } else {

                    // 使用默认颜色
                    material.setColor("Diffuse", ColorRGBA.Brown);
                }
            } else {
                // 使用默认土色
                material.setColor("Diffuse", ColorRGBA.Brown);
            }

            // 设置环境光和镜面反射
            material.setColor("Ambient", ColorRGBA.Brown.mult(0.3f));
            material.setColor("Specular", ColorRGBA.White);
            material.setFloat("Shininess", 32f);

            geometry.setMaterial(material);

        } catch (Exception e) {

            // 回退到基本材质
            Material fallback = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
            fallback.setColor("Color", ColorRGBA.Brown);
            geometry.setMaterial(fallback);
        }
    }

    /**
     * 检查文件是否存在
     */
    private boolean checkFileExists(String path) {
        try {
            AssetInfo info = assetManager.locateAsset(new AssetKey<>(path));
            return info != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 创建内置几何体土方块（保持原有逻辑）
     */
    private void createBuiltInDirtBlocks() {

        ColorRGBA[] colors = {
                ColorRGBA.Brown,
                new ColorRGBA(0.6f, 0.4f, 0.2f, 1.0f),
                new ColorRGBA(0.8f, 0.6f, 0.3f, 1.0f),
                new ColorRGBA(0.5f, 0.3f, 0.1f, 1.0f)
        };

        String[] names = {"标准土块", "深色土块", "浅色土块", "暗色土块"};

        for (int i = 0; i < colors.length; i++) {
            String modelKey = "built-in-dirt-" + i;

            Box box = new Box(0.5f, 0.5f, 0.5f);
            Geometry geom = new Geometry("DirtBlock" + i, box);

            Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
            mat.setColor("Color", colors[i]);
            geom.setMaterial(mat);

            dirtBlockModels.put(modelKey, geom);
        }

    }

    /**
     * 获取随机的土方块模型
     */
    public Spatial getRandomDirtBlock() {
        if (dirtBlockModels.isEmpty()) {
            return createEmergencyDirtBlock();
        }

        String[] modelIds = dirtBlockModels.keySet().toArray(new String[0]);
        String randomId = modelIds[random.nextInt(modelIds.length)];
        Spatial originalModel = dirtBlockModels.get(randomId);

        if (originalModel != null) {
            Spatial clonedModel = originalModel.clone();
            clonedModel.setLocalScale(1.0f);
            return clonedModel;
        }

        return createEmergencyDirtBlock();
    }

    /**
     * 创建紧急备用土方块
     */
    private Spatial createEmergencyDirtBlock() {

        Box box = new Box(0.5f, 0.5f, 0.5f);
        Geometry geom = new Geometry("EmergencyDirtBlock", box);

        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.Red);
        geom.setMaterial(mat);

        return geom;
    }

    // 其他方法保持不变...
    public int createRandomDirtField(float centerX, float centerZ, int width, int height, int density) {

        int createdCount = 0;
        float startX = centerX - width / 2.0f;
        float startZ = centerZ - height / 2.0f;

        for (int x = 0; x < width; x++) {
            for (int z = 0; z < height; z++) {
                if (random.nextInt(100) < density) {
                    Spatial dirtBlock = getRandomDirtBlock();

                    if (dirtBlock != null) {
                        float posX = startX + x;
                        float posZ = startZ + z;
                        float posY = 0;

                        dirtBlock.setLocalTranslation(posX, posY, posZ);

                        float rotation = (random.nextInt(4) * 90) * (float) Math.PI / 180f;
                        dirtBlock.setLocalRotation(dirtBlock.getLocalRotation().fromAngleAxis(rotation, Vector3f.UNIT_Y));

                        app.getRootNode().attachChild(dirtBlock);
                        createdCount++;
                    }
                }
            }
        }

        return createdCount;
    }

    public void reloadModels() {
        dirtBlockModels.clear();
        loadDirtBlockModels();
    }

    public int getAvailableModelCount() {
        return dirtBlockModels.size();
    }

    public List<String> getModelIds() {
        return new ArrayList<>(dirtBlockModels.keySet());
    }
}
