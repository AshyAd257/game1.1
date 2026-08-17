package com.Hecate.blender;

import com.Hecate.utils.LogUtils;
import com.jme3.asset.AssetManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Blender模型导入工具
 * 提供命令行和程序化接口来导入Blender模型
 */
public class BlenderImportTool {

    private final BlenderAssetLoader loader;
    private final AssetManager assetManager;

    public BlenderImportTool(AssetManager assetManager) {
        this.assetManager = assetManager;
        this.loader = new BlenderAssetLoader(assetManager);
    }

    /**
     * 从路径字符串导入单个模型
     */
    public BlenderModel importModel(String sourcePath, String modelId) {
        File sourceFile = new File(sourcePath);
        if (!sourceFile.exists()) {
            LogUtils.error(getClass(), "文件不存在: " + sourcePath, null);
            return null;
        }

        return loader.importModelFile(sourceFile, modelId);
    }

    /**
     * 从路径字符串批量导入文件夹
     */
    public int importFolder(String folderPath, String modelIdPrefix) {
        File folder = new File(folderPath);
        if (!folder.exists() || !folder.isDirectory()) {
            LogUtils.error(getClass(), "文件夹不存在: " + folderPath, null);
            return 0;
        }

        return loader.importModelFolder(folder, modelIdPrefix);
    }

    /**
     * 列出支持的文件格式
     */
    public static String[] getSupportedFormats() {
        return new String[]{
            "OBJ - Wavefront Object (.obj)",
            "FBX - Autodesk FBX (.fbx)",
            "GLTF - GL Transmission Format (.gltf)",
            "GLB - Binary GLTF (.glb)",
            "J3O - jMonkeyEngine Binary (.j3o)"
        };
    }

    /**
     * 打印使用说明
     */
    public static void printUsage() {
        for (String format : getSupportedFormats()) {
        }
    }

    /**
     * 获取资源目录信息
     */
    public static class ResourceInfo {
        public final String modelsDir;
        public final String texturesDir;

        public ResourceInfo() {
            String userDir = System.getProperty("user.dir");
            this.modelsDir = userDir + "/src/main/resources/Models/Blender/";
            this.texturesDir = userDir + "/src/main/resources/Textures/Blender/";
        }

        @Override
        public String toString() {
            return "模型目录: " + modelsDir + "\n纹理目录: " + texturesDir;
        }
    }

    /**
     * 列出已导入的模型
     */
    public List<String> listImportedModels() {
        List<String> models = new ArrayList<>();
        BlenderModelRegistry registry = BlenderModelRegistry.getInstance();

        for (String modelId : registry.getAllModelIds()) {
            BlenderModel model = registry.getModel(modelId);
            if (model != null) {
                models.add(modelId + " - " + model.getModelPath());
            }
        }

        return models;
    }

    /**
     * 获取模型详细信息
     */
    public String getModelInfo(String modelId) {
        BlenderModelRegistry registry = BlenderModelRegistry.getInstance();
        BlenderModel model = registry.getModel(modelId);

        if (model == null) {
            return "模型不存在: " + modelId;
        }

        StringBuilder info = new StringBuilder();
        info.append("=== 模型信息 ===\n");
        info.append("ID: ").append(model.getId()).append("\n");
        info.append("路径: ").append(model.getModelPath()).append("\n");
        info.append("已加载: ").append(model.isLoaded() ? "是" : "否").append("\n");

        List<String> animations = model.getAnimationNames();
        if (animations != null && !animations.isEmpty()) {
            info.append("动画数量: ").append(animations.size()).append("\n");
            for (String anim : animations) {
                info.append("  - ").append(anim).append("\n");
            }
        }

        return info.toString();
    }

    /**
     * 示例：如何使用导入工具
     */
    public static void example(AssetManager assetManager) {
        BlenderImportTool tool = new BlenderImportTool(assetManager);

        // 打印使用说明
        printUsage();

        // 示例1：导入单个模型

        // 示例2：批量导入

        // 示例3：列出已导入的模型

        // 示例4：查看模型信息
    }
}
