package com.Hecate.puppet.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.Hecate.puppet.core.Bone;
import com.Hecate.puppet.core.PuppetRenderer;
import com.Hecate.puppet.core.Skeleton;
import com.Hecate.puppet.animation.Keyframe;
import com.Hecate.puppet.editor.core.EditorSkeleton;
import com.Hecate.puppet.editor.core.EditorPuppetRenderer;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * 木偶打包格式输入/输出工具
 * 将木偶配置和所有纹理图片打包成一个.puppet文件（实际上是ZIP格式）
 *
 * 文件结构：
 * puppet.puppet (ZIP)
 * ├── puppet.json          # 木偶配置
 * └── textures/            # 纹理文件夹
 *     ├── texture1.png
 *     ├── texture2.png
 *     └── ...
 *
 * 优点：
 * - 完全独立，包含所有资源
 * - 方便分享和移植
 * - 标准ZIP格式，可以手动解压查看
 * - 自动压缩，节省空间
 */
public class PuppetPackageIO {

    private static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(Keyframe.KeyframeType.class, new TypeAdapter<Keyframe.KeyframeType>() {
                @Override
                public void write(JsonWriter out, Keyframe.KeyframeType value) throws IOException {
                    if (value == null) {
                        out.nullValue();
                    } else {
                        out.value(value.name());
                    }
                }

                @Override
                public Keyframe.KeyframeType read(JsonReader in) throws IOException {
                    String value = in.nextString();
                    try {
                        return Keyframe.KeyframeType.valueOf(value);
                    } catch (IllegalArgumentException e) {
                        return Keyframe.KeyframeType.INTERPOLATED;
                    }
                }
            })
            .create();

    private static final String CONFIG_ENTRY = "puppet.json";
    private static final String TEXTURE_DIR = "textures/";

    /**
     * 保存木偶为打包格式（.puppet文件）
     *
     * @param skeleton 骨架
     * @param renderer 渲染器
     * @param packagePath 打包文件路径（.puppet）
     * @throws IOException 如果保存失败
     */
    public static void savePackage(Skeleton skeleton, PuppetRenderer renderer, String packagePath) throws IOException {

        // 1. 创建配置
        PuppetConfig config = PuppetIO.createConfig(skeleton, renderer);

        // 2. 收集所有纹理路径
        Set<String> texturePaths = collectTexturePaths(config);

        // 3. 创建纹理路径映射（原始路径 -> 打包内路径）
        Map<String, String> pathMapping = new HashMap<>();
        int textureIndex = 0;
        for (String originalPath : texturePaths) {
            if (originalPath != null && !originalPath.isEmpty()) {
                // 获取文件扩展名
                String extension = getFileExtension(originalPath);
                // 生成打包内的路径
                String packagedPath = TEXTURE_DIR + "texture_" + textureIndex + extension;
                pathMapping.put(originalPath, packagedPath);
                textureIndex++;
            }
        }

        // 4. 修改配置中的纹理路径为打包内路径
        PuppetConfig packagedConfig = remapTexturePaths(config, pathMapping);

        // 5. 创建ZIP文件
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(packagePath))) {
            // 5.1 写入配置JSON
            ZipEntry configEntry = new ZipEntry(CONFIG_ENTRY);
            zos.putNextEntry(configEntry);
            String jsonString = gson.toJson(packagedConfig);
            zos.write(jsonString.getBytes("UTF-8"));
            zos.closeEntry();

            // 5.2 写入所有纹理文件
            for (Map.Entry<String, String> entry : pathMapping.entrySet()) {
                String originalPath = entry.getKey();
                String packagedPath = entry.getValue();

                File textureFile = new File(originalPath);
                if (textureFile.exists() && textureFile.isFile()) {
                    ZipEntry textureEntry = new ZipEntry(packagedPath);
                    zos.putNextEntry(textureEntry);

                    // 复制文件内容
                    try (FileInputStream fis = new FileInputStream(textureFile)) {
                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        while ((bytesRead = fis.read(buffer)) != -1) {
                            zos.write(buffer, 0, bytesRead);
                        }
                    }

                    zos.closeEntry();
                } else {
                    System.err.println("[PuppetPackageIO] 警告：纹理文件不存在: " + originalPath);
                }
            }
        }

    }

    /**
     * 从打包格式加载木偶（.puppet文件）
     *
     * @param packagePath 打包文件路径
     * @param skeleton 目标骨架
     * @param renderer 目标渲染器
     * @param assetManager jME资源管理器（用于加载纹理）
     * @throws IOException 如果加载失败
     */
    public static void loadPackage(String packagePath, Skeleton skeleton, PuppetRenderer renderer,
                                   com.jme3.asset.AssetManager assetManager) throws IOException {

        // 1. 创建临时目录用于解压纹理
        Path tempDir = Files.createTempDirectory("puppet_package_");

        try {
            PuppetConfig config = null;
            Map<String, Path> extractedTextures = new HashMap<>();

            // 优先尝试从classpath加载
            InputStream resourceStream = PuppetPackageIO.class.getClassLoader().getResourceAsStream(packagePath);
            InputStream zipInputStream;

            if (resourceStream != null) {
                // 从classpath资源加载
                zipInputStream = resourceStream;
            } else {
                // 回退到文件系统路径
                zipInputStream = new FileInputStream(packagePath);
            }

            // 2. 解压ZIP文件
            try (ZipInputStream zis = new ZipInputStream(zipInputStream)) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    String entryName = entry.getName();

                    if (entryName.equals(CONFIG_ENTRY)) {
                        // 读取配置JSON
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        while ((bytesRead = zis.read(buffer)) != -1) {
                            baos.write(buffer, 0, bytesRead);
                        }
                        String jsonString = baos.toString("UTF-8");
                        config = gson.fromJson(jsonString, PuppetConfig.class);

                    } else if (entryName.startsWith(TEXTURE_DIR) && !entry.isDirectory()) {
                        // 解压纹理文件
                        Path texturePath = tempDir.resolve(entryName);
                        Files.createDirectories(texturePath.getParent());

                        try (FileOutputStream fos = new FileOutputStream(texturePath.toFile())) {
                            byte[] buffer = new byte[8192];
                            int bytesRead;
                            while ((bytesRead = zis.read(buffer)) != -1) {
                                fos.write(buffer, 0, bytesRead);
                            }
                        }

                        extractedTextures.put(entryName, texturePath);
                    }

                    zis.closeEntry();
                }
            }

            if (config == null) {
                throw new IOException("打包文件中没有找到配置文件: " + CONFIG_ENTRY);
            }

            // 3. 将打包内路径映射回实际文件路径
            Map<String, String> pathMapping = new HashMap<>();
            for (Map.Entry<String, Path> entry : extractedTextures.entrySet()) {
                pathMapping.put(entry.getKey(), entry.getValue().toString());
            }

            PuppetConfig remappedConfig = remapTexturePaths(config, pathMapping);

            // 4. 应用配置到骨架和渲染器
            PuppetIO.applyConfig(remappedConfig, skeleton, renderer);

        } finally {
            // 注意：临时文件在程序退出时会被删除
            // 如果需要立即删除，可以在这里添加清理代码
            // 但由于纹理可能还在使用中，建议保留到程序退出
            tempDir.toFile().deleteOnExit();
        }
    }

    /**
     * 收集配置中的所有纹理路径
     */
    private static Set<String> collectTexturePaths(PuppetConfig config) {
        Set<String> paths = new HashSet<>();

        for (BoneConfig boneConfig : config.getBones()) {
            PartConfig partConfig = boneConfig.getPartConfig();
            if (partConfig != null) {
                // 单一纹理路径
                if (partConfig.getTexturePath() != null && !partConfig.getTexturePath().isEmpty()) {
                    paths.add(partConfig.getTexturePath());
                }

                // 多方向纹理路径
                if (partConfig.getDirectionTextures() != null) {
                    for (String texturePath : partConfig.getDirectionTextures().values()) {
                        if (texturePath != null && !texturePath.isEmpty()) {
                            paths.add(texturePath);
                        }
                    }
                }

                // 旋转条状贴图路径
                if (partConfig.getStripTexturePath() != null && !partConfig.getStripTexturePath().isEmpty()) {
                    paths.add(partConfig.getStripTexturePath());
                }
            }
        }

        return paths;
    }

    /**
     * 重新映射配置中的纹理路径
     */
    private static PuppetConfig remapTexturePaths(PuppetConfig config, Map<String, String> pathMapping) {
        // 深拷贝配置（通过JSON序列化/反序列化）
        String jsonString = gson.toJson(config);
        PuppetConfig newConfig = gson.fromJson(jsonString, PuppetConfig.class);

        // 重新映射所有纹理路径
        for (BoneConfig boneConfig : newConfig.getBones()) {
            PartConfig partConfig = boneConfig.getPartConfig();
            if (partConfig != null) {
                // 单一纹理路径
                if (partConfig.getTexturePath() != null && !partConfig.getTexturePath().isEmpty()) {
                    String originalPath = partConfig.getTexturePath();
                    String newPath = pathMapping.get(originalPath);
                    if (newPath != null) {
                        partConfig.setTexturePath(newPath);
                    }
                }

                // 多方向纹理路径
                if (partConfig.getDirectionTextures() != null) {
                    Map<String, String> newDirectionTextures = new HashMap<>();
                    for (Map.Entry<String, String> entry : partConfig.getDirectionTextures().entrySet()) {
                        String direction = entry.getKey();
                        String originalPath = entry.getValue();
                        String newPath = pathMapping.getOrDefault(originalPath, originalPath);
                        newDirectionTextures.put(direction, newPath);
                    }
                    partConfig.setDirectionTextures(newDirectionTextures);
                }

                // 旋转条状贴图路径
                if (partConfig.getStripTexturePath() != null && !partConfig.getStripTexturePath().isEmpty()) {
                    String originalPath = partConfig.getStripTexturePath();
                    String newPath = pathMapping.get(originalPath);
                    if (newPath != null) {
                        partConfig.setStripTexturePath(newPath);
                    }
                }
            }
        }

        return newConfig;
    }

    /**
     * 获取文件扩展名（包括点号）
     */
    private static String getFileExtension(String filePath) {
        int lastDotIndex = filePath.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < filePath.length() - 1) {
            return filePath.substring(lastDotIndex);
        }
        return ".png"; // 默认扩展名
    }

    /**
     * 检查文件是否为打包格式
     */
    public static boolean isPackageFile(String filePath) {
        return filePath != null && filePath.toLowerCase().endsWith(".puppet");
    }

    /**
     * 检查文件是否为JSON格式
     */
    public static boolean isJsonFile(String filePath) {
        return filePath != null && filePath.toLowerCase().endsWith(".json");
    }

    /**
     * 加载打包格式（编辑器版本）
     * 将 EditorSkeleton 和 EditorPuppetRenderer 转换为基础类型后加载
     */
    public static void loadPackage(String packagePath, EditorSkeleton editorSkeleton,
                                   EditorPuppetRenderer editorRenderer,
                                   com.jme3.asset.AssetManager assetManager) throws IOException {
        // 转换为基础类型
        Skeleton skeleton = editorSkeleton.getBaseSkeleton();
        PuppetRenderer renderer = editorRenderer.getBaseRenderer();

        // 调用原始加载方法
        loadPackage(packagePath, skeleton, renderer, assetManager);

        // TODO: 需要将加载的数据同步回 EditorSkeleton 和 EditorPuppetRenderer
        // 这需要实现反向转换逻辑
    }
}
