package com.Hecate.character;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 角色配置加载器
 * 负责加载和保存部件定义、皮肤配置等JSON文件
 */
public class CharacterConfigLoader {

    private final Gson gson;
    private final String basePath;

    // 缓存已加载的部件定义
    private final Map<String, PuppetPartDefinition> partCache = new HashMap<>();

    // 缓存已加载的皮肤配置
    private final Map<String, CharacterSkin> skinCache = new HashMap<>();

    /**
     * 构造函数
     * @param basePath 配置文件基础路径（例如 "Characters/"）
     */
    public CharacterConfigLoader(String basePath) {
        this.basePath = basePath;
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();
    }

    /**
     * 加载部件定义
     * @param partId 部件ID
     * @return 部件定义，如果加载失败返回null
     */
    public PuppetPartDefinition loadPartDefinition(String partId) {
        // 先检查缓存
        if (partCache.containsKey(partId)) {
            return partCache.get(partId);
        }

        String filePath = basePath + "Puppets/Parts/" + partId + ".json";

        try {
            // 尝试从资源文件加载
            InputStream inputStream = getClass().getClassLoader()
                    .getResourceAsStream(filePath);

            if (inputStream != null) {
                try (InputStreamReader reader = new InputStreamReader(
                        inputStream, StandardCharsets.UTF_8)) {
                    PuppetPartDefinition part = gson.fromJson(
                            reader, PuppetPartDefinition.class);
                    partCache.put(partId, part);
                    return part;
                }
            }

            // 如果资源文件不存在，尝试从文件系统加载
            try (FileReader reader = new FileReader(filePath)) {
                PuppetPartDefinition part = gson.fromJson(
                        reader, PuppetPartDefinition.class);
                partCache.put(partId, part);
                return part;
            }

        } catch (IOException e) {
            System.err.println("Failed to load part definition: " + partId);
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 保存部件定义
     * @param part 部件定义
     * @return 是否保存成功
     */
    public boolean savePartDefinition(PuppetPartDefinition part) {
        String filePath = basePath + "Puppets/Parts/" + part.getPartId() + ".json";

        try (FileWriter writer = new FileWriter(filePath)) {
            gson.toJson(part, writer);
            partCache.put(part.getPartId(), part);
            return true;
        } catch (IOException e) {
            System.err.println("Failed to save part definition: " + part.getPartId());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 加载皮肤配置
     * @param skinId 皮肤ID
     * @return 皮肤配置，如果加载失败返回null
     */
    public CharacterSkin loadSkin(String skinId) {
        // 先检查缓存
        if (skinCache.containsKey(skinId)) {
            return skinCache.get(skinId);
        }

        String filePath = basePath + "Puppets/Skins/" + skinId + ".json";

        try {
            // 尝试从资源文件加载
            InputStream inputStream = getClass().getClassLoader()
                    .getResourceAsStream(filePath);

            if (inputStream != null) {
                try (InputStreamReader reader = new InputStreamReader(
                        inputStream, StandardCharsets.UTF_8)) {
                    CharacterSkin skin = gson.fromJson(reader, CharacterSkin.class);
                    skinCache.put(skinId, skin);
                    return skin;
                }
            }

            // 如果资源文件不存在，尝试从文件系统加载
            try (FileReader reader = new FileReader(filePath)) {
                CharacterSkin skin = gson.fromJson(reader, CharacterSkin.class);
                skinCache.put(skinId, skin);
                return skin;
            }

        } catch (IOException e) {
            System.err.println("Failed to load skin: " + skinId);
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 保存皮肤配置
     * @param skin 皮肤配置
     * @return 是否保存成功
     */
    public boolean saveSkin(CharacterSkin skin) {
        String filePath = basePath + "Puppets/Skins/" + skin.getSkinId() + ".json";

        try (FileWriter writer = new FileWriter(filePath)) {
            gson.toJson(skin, writer);
            skinCache.put(skin.getSkinId(), skin);
            return true;
        } catch (IOException e) {
            System.err.println("Failed to save skin: " + skin.getSkinId());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 清除缓存
     */
    public void clearCache() {
        partCache.clear();
        skinCache.clear();
    }

    /**
     * 重新加载部件定义（忽略缓存）
     */
    public PuppetPartDefinition reloadPartDefinition(String partId) {
        partCache.remove(partId);
        return loadPartDefinition(partId);
    }

    /**
     * 重新加载皮肤配置（忽略缓存）
     */
    public CharacterSkin reloadSkin(String skinId) {
        skinCache.remove(skinId);
        return loadSkin(skinId);
    }
}
