package com.Hecate.blender;

import com.Hecate.utils.LogUtils;
import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.texture.Texture;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Blender模型导入器
 * 支持导入Blender导出的OBJ、FBX、GLTF等格式模型
 * 自动处理纹理文件的复制和加载
 */
public class BlenderImporter {

    private final AssetManager assetManager;
    private final String modelBaseDir = "Models/Blender/";
    private final String textureBaseDir = "Textures/Blender/";

    // 支持的模型格式
    private static final String[] SUPPORTED_MODEL_FORMATS = {
        ".obj", ".OBJ",
        ".fbx", ".FBX",
        ".gltf", ".GLTF",
        ".glb", ".GLB",
        ".j3o", ".J3O"
    };

    // 支持的纹理格式
    private static final String[] SUPPORTED_TEXTURE_FORMATS = {
        ".png", ".PNG",
        ".jpg", ".JPG",
        ".jpeg", ".JPEG",
        ".tga", ".TGA",
        ".bmp", ".BMP",
        ".dds", ".DDS"
    };

    public BlenderImporter(AssetManager assetManager) {
        this.assetManager = assetManager;
    }

    /**
     * 导入单个模型文件（自动检测并复制相关纹理）
     *
     * @param sourceFile 源模型文件路径
     * @param modelId 模型ID
     * @return 导入结果
     */
    public ImportResult importModel(File sourceFile, String modelId) {
        return importModel(sourceFile, modelId, true);
    }

    /**
     * 导入单个模型文件
     *
     * @param sourceFile 源模型文件路径
     * @param modelId 模型ID
     * @param autoDetectTextures 是否自动检测并复制同目录下的纹理文件
     * @return 导入结果
     */
    public ImportResult importModel(File sourceFile, String modelId, boolean autoDetectTextures) {
        ImportResult result = new ImportResult();
        result.modelId = modelId;

        if (!sourceFile.exists()) {
            result.success = false;
            result.errorMessage = "模型文件不存在: " + sourceFile.getAbsolutePath();
            LogUtils.error(getClass(), result.errorMessage, null);
            return result;
        }

        // 验证文件格式
        if (!isSupportedModelFormat(sourceFile.getName())) {
            result.success = false;
            result.errorMessage = "不支持的模型格式: " + sourceFile.getName();
            LogUtils.error(getClass(), result.errorMessage, null);
            return result;
        }

        try {
            // 获取资源根目录
            String resourcesPath = getResourcesPath();

            // 创建目标目录
            File modelDir = new File(resourcesPath, modelBaseDir);
            File textureDir = new File(resourcesPath, textureBaseDir);
            modelDir.mkdirs();
            textureDir.mkdirs();

            // 复制模型文件
            String modelFileName = modelId + getFileExtension(sourceFile.getName());
            File targetModelFile = new File(modelDir, modelFileName);
            Files.copy(sourceFile.toPath(), targetModelFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            result.modelPath = modelBaseDir + modelFileName;


            // 自动检测并复制纹理文件
            if (autoDetectTextures) {
                File sourceDir = sourceFile.getParentFile();
                result.texturePaths = detectAndCopyTextures(sourceDir, textureDir, modelId);
            }

            // 加载模型验证
            try {
                Spatial spatial = assetManager.loadModel(result.modelPath);
                if (spatial != null) {
                    result.success = true;
                    result.loadedSpatial = spatial;

                    // 自动应用纹理（如果有）
                    if (!result.texturePaths.isEmpty()) {
                        applyTexturesToModel(spatial, result.texturePaths);
                    }


                } else {
                    result.success = false;

                }
            } catch (Exception e) {
                result.success = false;
                result.errorMessage = "模型加载验证失败: " + e.getMessage();
                LogUtils.error(getClass(), result.errorMessage, e);
            }

        } catch (Exception e) {
            result.success = false;
            result.errorMessage = "导入过程发生错误: " + e.getMessage();
            LogUtils.error(getClass(), result.errorMessage, e);
        }

        return result;
    }

    /**
     * 批量导入模型文件夹（包含模型和纹理）
     *
     * @param sourceDir 源文件夹
     * @param modelIdPrefix 模型ID前缀
     * @return 导入结果列表
     */
    public List<ImportResult> importModelFolder(File sourceDir, String modelIdPrefix) {
        List<ImportResult> results = new ArrayList<>();

        if (!sourceDir.exists() || !sourceDir.isDirectory()) {
            ImportResult errorResult = new ImportResult();
            errorResult.success = false;
            errorResult.errorMessage = "源目录不存在或不是目录: " + sourceDir.getAbsolutePath();
            results.add(errorResult);
            return results;
        }

        File[] files = sourceDir.listFiles();
        if (files == null) {
            return results;
        }

        int modelCount = 0;
        for (File file : files) {
            if (file.isFile() && isSupportedModelFormat(file.getName())) {
                String baseName = getBaseName(file.getName());
                String modelId = modelIdPrefix + "_" + baseName;

                ImportResult result = importModel(file, modelId, true);
                results.add(result);

                if (result.success) {
                    modelCount++;
                }
            }
        }


        return results;
    }

    /**
     * 检测并复制纹理文件
     */
    private Map<String, String> detectAndCopyTextures(File sourceDir, File targetDir, String modelId) {
        Map<String, String> texturePaths = new HashMap<>();

        File[] files = sourceDir.listFiles();
        if (files == null) {
            return texturePaths;
        }

        for (File file : files) {
            if (file.isFile() && isSupportedTextureFormat(file.getName())) {
                try {
                    String textureName = modelId + "_" + file.getName();
                    File targetFile = new File(targetDir, textureName);
                    Files.copy(file.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                    String texturePath = textureBaseDir + textureName;

                    // 根据文件名推测纹理类型
                    String textureType = guessTextureType(file.getName());
                    texturePaths.put(textureType, texturePath);



                } catch (Exception e) {

                }
            }
        }

        return texturePaths;
    }

    /**
     * 根据文件名猜测纹理类型
     */
    private String guessTextureType(String fileName) {
        String lowerName = fileName.toLowerCase();

        if (lowerName.contains("diffuse") || lowerName.contains("color") || lowerName.contains("albedo")) {
            return "DiffuseMap";
        } else if (lowerName.contains("normal") || lowerName.contains("norm")) {
            return "NormalMap";
        } else if (lowerName.contains("specular") || lowerName.contains("spec")) {
            return "SpecularMap";
        } else if (lowerName.contains("metallic") || lowerName.contains("metal")) {
            return "MetallicMap";
        } else if (lowerName.contains("roughness") || lowerName.contains("rough")) {
            return "RoughnessMap";
        } else if (lowerName.contains("ao") || lowerName.contains("ambient")) {
            return "LightMap";
        } else if (lowerName.contains("emission") || lowerName.contains("emissive")) {
            return "GlowMap";
        } else if (lowerName.contains("alpha") || lowerName.contains("opacity")) {
            return "AlphaMap";
        } else {
            // 默认作为漫反射贴图
            return "DiffuseMap";
        }
    }

    /**
     * 将纹理应用到模型上
     */
    private void applyTexturesToModel(Spatial spatial, Map<String, String> texturePaths) {
        if (spatial instanceof Geometry) {
            applyTexturesToGeometry((Geometry) spatial, texturePaths);
        } else if (spatial instanceof Node) {
            Node node = (Node) spatial;
            for (Spatial child : node.getChildren()) {
                applyTexturesToModel(child, texturePaths);
            }
        }
    }

    /**
     * 将纹理应用到几何体
     */
    private void applyTexturesToGeometry(Geometry geometry, Map<String, String> texturePaths) {
        Material material = geometry.getMaterial();
        if (material == null) {
            // 创建新材质
            material = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
            geometry.setMaterial(material);
        }

        for (Map.Entry<String, String> entry : texturePaths.entrySet()) {
            String textureType = entry.getKey();
            String texturePath = entry.getValue();

            try {
                Texture texture = assetManager.loadTexture(texturePath);
                if (texture != null) {
                    material.setTexture(textureType, texture);

                }
            } catch (Exception e) {

            }
        }

        // 设置材质属性
        material.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
    }

    /**
     * 获取资源根目录路径
     */
    private String getResourcesPath() {
        // 获取项目的resources目录
        String userDir = System.getProperty("user.dir");
        return userDir + "/src/main/resources";
    }

    /**
     * 检查是否为支持的模型格式
     */
    private boolean isSupportedModelFormat(String fileName) {
        for (String ext : SUPPORTED_MODEL_FORMATS) {
            if (fileName.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查是否为支持的纹理格式
     */
    private boolean isSupportedTextureFormat(String fileName) {
        for (String ext : SUPPORTED_TEXTURE_FORMATS) {
            if (fileName.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取文件扩展名（包含点）
     */
    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0) {
            return fileName.substring(lastDot);
        }
        return "";
    }

    /**
     * 获取文件基本名（不含扩展名）
     */
    private String getBaseName(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0) {
            return fileName.substring(0, lastDot);
        }
        return fileName;
    }

    /**
     * 导入结果类
     */
    public static class ImportResult {
        public boolean success;
        public String modelId;
        public String modelPath;
        public Map<String, String> texturePaths = new HashMap<>();
        public String errorMessage;
        public Spatial loadedSpatial;

        public boolean isSuccess() {
            return success;
        }

        public String getModelId() {
            return modelId;
        }

        public String getModelPath() {
            return modelPath;
        }

        public Map<String, String> getTexturePaths() {
            return texturePaths;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public Spatial getLoadedSpatial() {
            return loadedSpatial;
        }

        @Override
        public String toString() {
            if (success) {
                return "导入成功: " + modelId + " (" + texturePaths.size() + " 个纹理)";
            } else {
                return "导入失败: " + errorMessage;
            }
        }
    }
}
