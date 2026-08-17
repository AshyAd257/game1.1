package com.Hecate.blender;

import com.Hecate.loader.AbstractAssetLoader;
import com.Hecate.utils.LogUtils;
import com.jme3.asset.AssetManager;
import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

/**
 * Blender资源加载器
 * 提供便捷的方法来创建和注册Blender模型
 * 支持OBJ、FBX、GLTF、GLB、J3O等多种格式
 */
public class BlenderAssetLoader extends AbstractAssetLoader<BlenderModel, BlenderModelRegistry> {

    private final BlenderImporter importer;

    /**
     * 构造函数（依赖注入）
     *
     * @param assetManager 资产管理器
     * @param registry Blender模型注册表（通过依赖注入传入）
     */
    public BlenderAssetLoader(AssetManager assetManager, BlenderModelRegistry registry) {
        super(assetManager, registry);
        this.importer = new BlenderImporter(assetManager);
    }

    /**
     * 构造函数（向后兼容）
     *
     * @param assetManager 资产管理器
     * @deprecated 推荐使用 {@link #BlenderAssetLoader(AssetManager, BlenderModelRegistry)} 进行依赖注入
     */
    @Deprecated
    public BlenderAssetLoader(AssetManager assetManager) {
        super(assetManager, BlenderModelRegistry.getInstance());
        this.importer = new BlenderImporter(assetManager);
    }

    @Override
    protected String getModelTypeName() {
        return "Blender";
    }

    @Override
    protected void loadModelsImpl() {
        // 示例：加载一些常用模型
        // 注意：这些路径需要根据您的实际资源文件调整

        // 加载简单的树模型
        loadSimpleModel("tree_oak", "Models/Blender/tree_oak.j3o");

        // 加载带纹理的房屋模型
        Map<String, String> houseTextures = new HashMap<>();
        houseTextures.put("walls", "Textures/Blender/house_walls.png");
        houseTextures.put("roof", "Textures/Blender/house_roof.png");
        loadModelWithTextures("house_basic", "Models/Blender/house_basic.j3o", houseTextures);

        // 加载带动画的角色模型
        loadAnimatedModel("character_player", "Models/Blender/character.j3o",
                "idle", "walk", "run", "jump");
    }

    @Override
    protected boolean loadModelFile(BlenderModel model) {
        return model.load(assetManager);
    }

    /**
     * 加载简单模型（无动画，无纹理）
     */
    public BlenderModel loadSimpleModel(String modelId, String modelPath) {
        return loadModel(modelId, modelPath, null, null);
    }

    /**
     * 加载带纹理的模型
     */
    public BlenderModel loadModelWithTextures(String modelId, String modelPath,
                                              Map<String, String> texturePaths) {
        return loadModel(modelId, modelPath, texturePaths, null);
    }

    /**
     * 加载带动画的模型
     */
    public BlenderModel loadAnimatedModel(String modelId, String modelPath,
                                          String... animationNames) {
        return loadModel(modelId, modelPath, null, Arrays.asList(animationNames));
    }

    /**
     * 加载完整模型（纹理+动画）
     */
    public BlenderModel loadCompleteModel(String modelId, String modelPath,
                                          Map<String, String> texturePaths,
                                          String... animationNames) {
        return loadModel(modelId, modelPath, texturePaths, Arrays.asList(animationNames));
    }

    /**
     * 加载完整模型（纹理+动画）
     */
    public BlenderModel loadModel(String modelId, String modelPath,
                                  Map<String, String> texturePaths,
                                  List<String> animationNames) {

        // 检查模型是否已存在
        BlenderModel existing = checkExistingModel(modelId);
        if (existing != null) {
            return existing;
        }

        BlenderModel model = new BlenderModel(modelId, modelPath, texturePaths, animationNames);

        // 立即加载模型
        if (loadModelFile(model)) {
            registry.registerModel(model);
            return model;
        } else {
            LogUtils.error(getClass(), "模型加载失败: " + modelId, null);
            return null;
        }
    }

    /**
     * 创建纹理映射的辅助方法
     */
    public static Map<String, String> createTextureMap(String... pairs) {
        Map<String, String> textureMap = new HashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            if (i + 1 < pairs.length) {
                textureMap.put(pairs[i], pairs[i + 1]);
            }
        }
        return textureMap;
    }

    // ==================== 导入功能 ====================

    /**
     * 从外部文件导入Blender模型（自动检测并复制纹理）
     *
     * @param sourceFile 源模型文件
     * @param modelId 模型ID
     * @return 导入的模型，失败返回null
     */
    public BlenderModel importModelFile(File sourceFile, String modelId) {
        BlenderImporter.ImportResult result = importer.importModel(sourceFile, modelId, true);

        if (result.isSuccess()) {
            BlenderModel model = new BlenderModel(
                result.getModelId(),
                result.getModelPath(),
                result.getTexturePaths(),
                null
            );

            // 模型已经在导入时加载，直接设置spatial
            model.setSpatial(result.getLoadedSpatial());

            // 注册到registry
            registry.registerModel(model);


            return model;
        } else {

            return null;
        }
    }

    /**
     * 从外部文件导入Blender模型（不自动检测纹理）
     *
     * @param sourceFile 源模型文件
     * @param modelId 模型ID
     * @param autoDetectTextures 是否自动检测纹理
     * @return 导入的模型，失败返回null
     */
    public BlenderModel importModelFile(File sourceFile, String modelId, boolean autoDetectTextures) {
        BlenderImporter.ImportResult result = importer.importModel(sourceFile, modelId, autoDetectTextures);

        if (result.isSuccess()) {
            BlenderModel model = new BlenderModel(
                result.getModelId(),
                result.getModelPath(),
                result.getTexturePaths(),
                null
            );

            model.setSpatial(result.getLoadedSpatial());
            registry.registerModel(model);


            return model;
        } else {

            return null;
        }
    }

    /**
     * 批量导入文件夹中的所有Blender模型
     *
     * @param sourceDir 源文件夹
     * @param modelIdPrefix 模型ID前缀
     * @return 成功导入的模型数量
     */
    public int importModelFolder(File sourceDir, String modelIdPrefix) {
        List<BlenderImporter.ImportResult> results = importer.importModelFolder(sourceDir, modelIdPrefix);

        int successCount = 0;
        for (BlenderImporter.ImportResult result : results) {
            if (result.isSuccess()) {
                BlenderModel model = new BlenderModel(
                    result.getModelId(),
                    result.getModelPath(),
                    result.getTexturePaths(),
                    null
                );

                model.setSpatial(result.getLoadedSpatial());
                registry.registerModel(model);
                successCount++;
            }
        }
        return successCount;
    }

    /**
     * 获取导入器实例
     */
    public BlenderImporter getImporter() {
        return importer;
    }
}
