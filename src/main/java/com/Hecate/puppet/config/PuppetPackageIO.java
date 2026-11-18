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

        savePackage(PuppetIO.createConfig(skeleton,renderer),renderer.getAssetManager(),packagePath);
    }

    /** Complete resource bundle; an incomplete export never replaces an existing good file. */
    public static void savePackage(PuppetConfig config,com.jme3.asset.AssetManager assets,String packagePath)throws IOException{
        Set<String> paths=collectTexturePaths(config);Map<String,String> mapping=new LinkedHashMap<>();int index=0;
        for(String path:paths)mapping.put(path,TEXTURE_DIR+"texture_"+(index++)+getFileExtension(path));
        PuppetConfig packaged=remapTexturePaths(config,mapping);
        Path destination=Paths.get(packagePath).toAbsolutePath();Path staging=Files.createTempFile(destination.getParent(),".puppet-export-",".tmp");
        try{
            try(ZipOutputStream zip=new ZipOutputStream(Files.newOutputStream(staging))){
                zip.putNextEntry(new ZipEntry(CONFIG_ENTRY));zip.write(gson.toJson(packaged).getBytes(java.nio.charset.StandardCharsets.UTF_8));zip.closeEntry();
                for(var item:mapping.entrySet())try(InputStream input=openResource(item.getKey(),assets)){
                    zip.putNextEntry(new ZipEntry(item.getValue()));input.transferTo(zip);zip.closeEntry();
                }
            }
            try{Files.move(staging,destination,java.nio.file.StandardCopyOption.ATOMIC_MOVE,java.nio.file.StandardCopyOption.REPLACE_EXISTING);}
            catch(java.nio.file.AtomicMoveNotSupportedException ex){Files.move(staging,destination,java.nio.file.StandardCopyOption.REPLACE_EXISTING);}
        }finally{Files.deleteIfExists(staging);}
    }
    private static InputStream openResource(String name,com.jme3.asset.AssetManager assets)throws IOException{
        try{
            Path file=name.startsWith("file:")?Paths.get(java.net.URI.create(name)):Paths.get(name);
            if(Files.isRegularFile(file))return Files.newInputStream(file);
            String resource=name.replace('\\','/');
            if(assets!=null){var info=assets.locateAsset(new com.jme3.asset.AssetKey<Object>(resource));if(info!=null)return info.openStream();}
            InputStream stream=PuppetPackageIO.class.getClassLoader().getResourceAsStream(resource);if(stream!=null)return stream;
        }catch(RuntimeException ex){throw new IOException("无法读取资源："+name,ex);}
        throw new IOException("导出失败，找不到资源："+name+"。请重新选择贴图后再导出。");
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

        PuppetIO.applyConfig(readPackageConfig(packagePath), skeleton, renderer);
    }

    /** Reads a portable package into a config usable by either renderer. */
    public static PuppetConfig readPackageConfig(String packagePath) throws IOException {
        // 1. 创建临时目录用于解压纹理
        Path tempDir = Files.createTempDirectory("puppet_package_");
        tempDir.toFile().deleteOnExit();

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
                        Path texturePath = tempDir.resolve(entryName).normalize();
                        if (!texturePath.startsWith(tempDir)) throw new IOException("非法资源路径: " + entryName);
                        Files.createDirectories(texturePath.getParent());

                        try (FileOutputStream fos = new FileOutputStream(texturePath.toFile())) {
                            byte[] buffer = new byte[8192];
                            int bytesRead;
                            while ((bytesRead = zis.read(buffer)) != -1) {
                                fos.write(buffer, 0, bytesRead);
                            }
                        }

                        Path directory=texturePath.getParent();java.util.List<Path> parents=new java.util.ArrayList<>();while(directory!=null&&directory.startsWith(tempDir)){parents.add(directory);directory=directory.getParent();}java.util.Collections.reverse(parents);for(Path parent:parents)parent.toFile().deleteOnExit();texturePath.toFile().deleteOnExit();
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

            for(String reference:collectTexturePaths(config))if(!pathMapping.containsKey(reference))throw new IOException("木偶文件缺少资源："+reference);
            PuppetConfig remappedConfig = remapTexturePaths(config, pathMapping);

            // 4. 应用配置到骨架和渲染器
            return remappedConfig;

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
        Set<String> paths = new LinkedHashSet<>();

        for (BoneConfig boneConfig : config.getBones()) {
            if (boneConfig.getVolumeDefinition() != null && boneConfig.getVolumeDefinition().getNormalTexturePath() != null) {
                paths.add(boneConfig.getVolumeDefinition().getNormalTexturePath());
            }
            if(boneConfig.getVolumeDefinition()!=null)for(var surface:com.Hecate.puppet.volume.Strip3DDefinition.Surface.values()){
                var layer=boneConfig.getVolumeDefinition().getStrip3D().layer(surface);
                if(layer.getColorPath()!=null)paths.add(layer.getColorPath());if(layer.getNormalPath()!=null)paths.add(layer.getNormalPath());
            }
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

                // 3D模型文件路径
                if (partConfig.getModelFilePath() != null && !partConfig.getModelFilePath().isEmpty()) {
                    paths.add(partConfig.getModelFilePath());
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
            if (boneConfig.getVolumeDefinition() != null) {
                String path = boneConfig.getVolumeDefinition().getNormalTexturePath();
                if (path != null) boneConfig.getVolumeDefinition().setNormalTexturePath(pathMapping.getOrDefault(path, path));
            }
            if(boneConfig.getVolumeDefinition()!=null)for(var surface:com.Hecate.puppet.volume.Strip3DDefinition.Surface.values()){
                var layer=boneConfig.getVolumeDefinition().getStrip3D().layer(surface);
                if(layer.getColorPath()!=null)layer.setColorPath(pathMapping.getOrDefault(layer.getColorPath(),layer.getColorPath()));
                if(layer.getNormalPath()!=null)layer.setNormalPath(pathMapping.getOrDefault(layer.getNormalPath(),layer.getNormalPath()));
            }
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

                // 3D模型文件路径（注意：目前只打包.obj本身，不会跟随收集.obj引用的.mtl
                // 材质文件和材质里引用的贴图——如果模型带贴图，打包后可能丢材质，
                // 这是已知限制，等真正用到带材质OBJ打包时再补）
                if (partConfig.getModelFilePath() != null && !partConfig.getModelFilePath().isEmpty()) {
                    String originalPath = partConfig.getModelFilePath();
                    String newPath = pathMapping.get(originalPath);
                    if (newPath != null) {
                        partConfig.setModelFilePath(newPath);
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
        if(filePath==null)return false;
        try(InputStream input=new FileInputStream(filePath)){return isPackageStream(input);}catch(IOException e){return false;}
    }

    static boolean isPackageStream(InputStream input)throws IOException{
        byte[] header=input.readNBytes(4);return header.length==4&&header[0]=='P'&&header[1]=='K'&&header[2]==3&&header[3]==4;
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
        PuppetIO.applyConfig(readPackageConfig(packagePath), editorSkeleton, editorRenderer);
    }
}
