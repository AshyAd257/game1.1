package com.Hecate.player;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.AssetManager;
import com.jme3.texture.Texture;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 玩家精灵资源管理器 - 修复版
 * 负责加载和管理PNG序列帧资源
 */
public class PlayerSpriteManager {

    private final SimpleApplication app;
    private final AssetManager assetManager;

    // 资源存储
    private final Map<String, List<Texture>> spriteSets = new HashMap<>();
    private final Map<String, Texture> textureCache = new HashMap<>();

    // 动画序列存储
    private final Map<String, List<AnimationFrame>> animationSequences = new HashMap<>();

    // 当前显示的精灵
    private Texture currentSprite;

    // 资源路径配置
    private static final String SPRITE_BASE_PATH = "textures/player/";

    public PlayerSpriteManager(SimpleApplication app) {
        this.app = app;
        this.assetManager = app.getAssetManager();

        // 加载所有玩家精灵
        loadAllPlayerSprites();
    }

    /**
     * 加载所有玩家精灵 - 根据实际文件结构修复
     */
    private void loadAllPlayerSprites() {

        // 定义实际存在的精灵文件
        Map<String, Integer> spriteConfig = new HashMap<>();

        // Front sprites
        spriteConfig.put("front_idle", 2);  // idle_01.png, idle_02.png
        spriteConfig.put("front_run", 4);   // run_01.png to run_04.png
        spriteConfig.put("front_jump", 4);  // jump_01.png to jump_04.png

        // Back sprites
        spriteConfig.put("back_idle", 1);   // idle_01.png
        spriteConfig.put("back_run", 4);    // run_01.png to run_04.png
        spriteConfig.put("back_jump", 1);   // jump_01.png

        // Left sprites
        spriteConfig.put("left_idle", 1);   // idle_01.png
        spriteConfig.put("left_run", 2);    // run_01.png, run_02.png
        spriteConfig.put("left_jump", 2);   // jump_01.png, jump_02.png

        // Right sprites
        spriteConfig.put("right_idle", 1);  // idle_01.png
        spriteConfig.put("right_run", 2);   // run_01.png, run_02.png
        spriteConfig.put("right_jump", 2);  // jump_01.png, jump_02.png

        // Top sprites (俯视角度) - 简化版
        spriteConfig.put("top_idle", 1);    // 只有一个站立贴图
        spriteConfig.put("top_run", 2);     // 假设有2帧走路动画，根据实际调整

        // 加载每个精灵集
        for (Map.Entry<String, Integer> entry : spriteConfig.entrySet()) {
            String key = entry.getKey();
            int frameCount = entry.getValue();
            String[] parts = key.split("_");
            String direction = parts[0];
            String action = parts[1];

            loadSpriteSetWithCount(direction, action, frameCount);
        }

        // 特殊处理：让 top_jump 使用 top_idle 的贴图
        if (spriteSets.containsKey("top_idle") && !spriteSets.containsKey("top_jump")) {
            spriteSets.put("top_jump", spriteSets.get("top_idle"));
        }

        // 如果没有加载到任何精灵，创建默认精灵
        if (spriteSets.isEmpty()) {
            createDefaultSprite();
        }

    }

    /**
     * 加载指定数量的精灵帧
     */
    private void loadSpriteSetWithCount(String direction, String action, int frameCount) {
        String key = direction + "_" + action;
        String folderPath = SPRITE_BASE_PATH + direction + "/" + action + "/";

        List<Texture> frames = new ArrayList<>();

        // 加载实际存在的帧
        for (int i = 1; i <= frameCount; i++) {
            String fileName = action + "_" + String.format("%02d", i) + ".png";
            String fullPath = folderPath + fileName;

            try {
                Texture texture = assetManager.loadTexture(fullPath);
                if (texture != null) {
                    // 设置像素完美过滤
                    texture.setMagFilter(Texture.MagFilter.Nearest);
                    texture.setMinFilter(Texture.MinFilter.NearestNoMipMaps);
                    frames.add(texture);
                    textureCache.put(fullPath, texture);
                }
            } catch (Exception e) {
                // 继续尝试加载其他帧
            }
        }

        // 如果成功加载了帧，保存精灵集
        if (!frames.isEmpty()) {
            spriteSets.put(key, frames);
        } else {

            // 尝试使用备用纹理
            loadFallbackTexture(key);
        }
    }

    /**
     * 加载备用纹理
     */
    private void loadFallbackTexture(String key) {
        try {
            // 首先尝试加载dirt纹理作为备用
            Texture fallbackTexture = assetManager.loadTexture("textures/blocks/dirt.png");
            if (fallbackTexture != null) {
                fallbackTexture.setMagFilter(Texture.MagFilter.Nearest);
                fallbackTexture.setMinFilter(Texture.MinFilter.NearestNoMipMaps);

                List<Texture> frames = new ArrayList<>();
                frames.add(fallbackTexture);
                spriteSets.put(key, frames);

            }
        } catch (Exception e) {
        }
    }

    /**
     * 创建默认精灵（纯色）
     */
    private void createDefaultSprite() {

        // 这里应该创建一个程序化的默认纹理
        // 由于JME3的限制，我们至少需要一个有效的纹理
        try {
            Texture defaultTexture = assetManager.loadTexture("textures/blocks/dirt.png");
            if (defaultTexture != null) {
                for (String key : new String[]{"front_idle", "back_idle", "left_idle", "right_idle"}) {
                    List<Texture> frames = new ArrayList<>();
                    frames.add(defaultTexture);
                    spriteSets.put(key, frames);
                }

            }
        } catch (Exception e) {
        }
    }

    /**
     * 加载标准玩家动画
     */
    public void loadStandardPlayerAnimations() {

        // 为每个精灵集创建动画序列
        for (Map.Entry<String, List<Texture>> entry : spriteSets.entrySet()) {
            String animationName = entry.getKey();
            List<Texture> textures = entry.getValue();

            if (textures == null || textures.isEmpty()) {

                continue;
            }

            List<AnimationFrame> frames = new ArrayList<>();
            for (int i = 0; i < textures.size(); i++) {
                Texture texture = textures.get(i);
                String frameName = animationName + "_frame_" + (i + 1);
                float duration = 0.2f; // 默认每帧0.2秒

                AnimationFrame frame = new AnimationFrame(frameName, "", duration, i);
                frame.setTexture(texture);
                frames.add(frame);
            }

            animationSequences.put(animationName, frames);

        }

        // 确保至少有一个默认动画
        if (animationSequences.isEmpty()) {
        } else {

        }
    }

    /**
     * 获取已加载的动画名称
     */
    public Set<String> getLoadedAnimationNames() {
        return animationSequences.keySet();
    }

    /**
     * 获取动画序列
     */
    public List<AnimationFrame> getAnimationSequence(String animationName) {
        return animationSequences.get(animationName);
    }

    /**
     * 获取指定方向和动作的帧数
     */
    public int getFrameCount(String direction, String action) {
        String key = direction + "_" + action;
        List<Texture> frames = spriteSets.get(key);
        if (frames != null) {
            return frames.size();
        }
        return 0;
    }

    /**
     * 设置当前动画
     */
    public void setCurrentAnimation(String direction, String action, int frame) {
        String key = direction + "_" + action;
        List<Texture> frames = spriteSets.get(key);

        if (frames != null && frame >= 0 && frame < frames.size()) {
            currentSprite = frames.get(frame);
        } else if (frames != null && !frames.isEmpty()) {
            currentSprite = frames.get(0);
        }
    }

    /**
     * 获取当前精灵纹理
     */
    public Texture getCurrentSprite() {
        return currentSprite;
    }

    /**
     * 获取特定帧的纹理
     */
    public Texture getFrame(String direction, String action, int frame) {
        String key = direction + "_" + action;
        List<Texture> frames = spriteSets.get(key);

        if (frames != null && frame >= 0 && frame < frames.size()) {
            return frames.get(frame);
        } else if (frames != null && !frames.isEmpty()) {
            return frames.get(0);
        }

        return null;
    }

    /**
     * 检查是否有指定的动画
     */
    public boolean hasAnimation(String direction, String action) {
        String key = direction + "_" + action;
        List<Texture> frames = spriteSets.get(key);
        return frames != null && !frames.isEmpty();
    }

    /**
     * 加载动画序列（自定义路径）
     */
    public boolean loadAnimationSequence(String animationName, String folderPath, int frameCount) {
        List<AnimationFrame> frames = new ArrayList<>();

        for (int i = 1; i <= frameCount; i++) {
            String fileName = String.format("%02d.png", i);
            String fullPath = folderPath + "/" + fileName;

            try {
                Texture texture = assetManager.loadTexture(fullPath);
                if (texture != null) {
                    texture.setMagFilter(Texture.MagFilter.Nearest);
                    texture.setMinFilter(Texture.MinFilter.NearestNoMipMaps);

                    String frameName = animationName + "_frame_" + i;
                    AnimationFrame frame = new AnimationFrame(frameName, fullPath, 0.2f, i - 1);
                    frame.setTexture(texture);
                    frames.add(frame);
                }
            } catch (Exception e) {
            }
        }

        if (!frames.isEmpty()) {
            animationSequences.put(animationName, frames);

            return true;
        }

        return false;
    }

    /**
     * 清理资源
     */
    public void cleanup() {

        spriteSets.clear();
        textureCache.clear();
        animationSequences.clear();
        currentSprite = null;
    }

    /**
     * 获取资源统计信息
     */
    public void printResourceStats() {

        for (Map.Entry<String, List<Texture>> entry : spriteSets.entrySet()) {
        }
    }
}
