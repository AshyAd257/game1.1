package com.Hecate.blender;

import com.jme3.app.SimpleApplication;
import com.jme3.light.DirectionalLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;

import java.io.File;

/**
 * Blender模型导入示例
 * 演示如何使用BlenderImporter导入和显示Blender模型
 */
public class BlenderImportExample extends SimpleApplication {

    private BlenderAssetLoader loader;
    private BlenderImportTool tool;

    public static void main(String[] args) {
        BlenderImportExample app = new BlenderImportExample();
        app.start();
    }

    @Override
    public void simpleInitApp() {
        // 初始化导入工具
        loader = new BlenderAssetLoader(assetManager);
        tool = new BlenderImportTool(assetManager);

        // 设置相机
        cam.setLocation(new Vector3f(0, 5, 10));
        cam.lookAt(Vector3f.ZERO, Vector3f.UNIT_Y);

        // 添加光照
        DirectionalLight sun = new DirectionalLight();
        sun.setDirection(new Vector3f(-0.5f, -0.5f, -0.5f).normalizeLocal());
        sun.setColor(ColorRGBA.White);
        rootNode.addLight(sun);

        // 打印使用说明
        BlenderImportTool.printUsage();

        // 示例1：如果你有一个OBJ文件，可以这样导入
        exampleImportSingleFile();

        // 示例2：批量导入文件夹
        exampleImportFolder();

        // 示例3：列出已导入的模型
        exampleListModels();
    }

    /**
     * 示例1：导入单个模型文件
     */
    private void exampleImportSingleFile() {

        // 假设你有一个模型文件
        String modelPath = "C:/path/to/your/model.obj";  // 修改为你的实际路径
        File modelFile = new File(modelPath);

        if (modelFile.exists()) {

            // 导入模型
            BlenderModel model = loader.importModelFile(modelFile, "my_model");

            if (model != null && model.isLoaded()) {
                // 创建实例并添加到场景
                Node instance = model.createInstance();
                if (instance != null) {
                    rootNode.attachChild(instance);
                }
            }
        } else {
        }
    }

    /**
     * 示例2：批量导入文件夹
     */
    private void exampleImportFolder() {

        // 假设你有一个包含多个模型的文件夹
        String folderPath = "C:/path/to/your/models/";  // 修改为你的实际路径
        File folder = new File(folderPath);

        if (folder.exists() && folder.isDirectory()) {

            // 批量导入
            int count = loader.importModelFolder(folder, "imported");

            // 显示所有导入的模型
            displayImportedModels();
        } else {
        }
    }

    /**
     * 示例3：列出已导入的模型
     */
    private void exampleListModels() {

        BlenderModelRegistry registry = BlenderModelRegistry.getInstance();
        int modelCount = registry.getAllModelIds().size();

        for (String modelId : registry.getAllModelIds()) {
        }
    }

    /**
     * 显示所有导入的模型
     */
    private void displayImportedModels() {
        BlenderModelRegistry registry = BlenderModelRegistry.getInstance();

        float x = 0;
        for (String modelId : registry.getAllModelIds()) {
            BlenderModel model = registry.getModel(modelId);
            if (model != null && model.isLoaded()) {
                Node instance = model.createInstance();
                if (instance != null) {
                    instance.setLocalTranslation(x, 0, 0);
                    rootNode.attachChild(instance);
                    x += 3; // 间隔3个单位
                }
            }
        }
    }

    /**
     * 程序化使用示例（不需要运行游戏）
     */
    public static class ProgrammaticExample {

        public static void demonstrateUsage() {

        }

        public static void main(String[] args) {
            demonstrateUsage();
        }
    }
}
