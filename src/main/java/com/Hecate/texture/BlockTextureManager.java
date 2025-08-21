package com.Hecate.texture;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.texture.Texture;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;
import com.jme3.math.ColorRGBA;

import java.util.HashMap;
import java.util.Map;
import java.nio.ByteBuffer;

/**
 * 🎨 方块纹理管理器 - 新的纹理系统
 */
public class BlockTextureManager {
    private final AssetManager assetManager;
    private final Map<String, BlockTextureDefinition> textureDefinitions = new HashMap<>();
    private final Map<String, Material> materialCache = new HashMap<>();

    public BlockTextureManager() {
        this.assetManager = null;
        System.out.println("🎨 BlockTextureManager 创建（无 AssetManager）");
    }

    public BlockTextureManager(AssetManager assetManager) {
        this.assetManager = assetManager;
        System.out.println("🎨 BlockTextureManager 创建完成");
    }

    /**
     * 🎨 初始化默认纹理定义
     */
    public void initializeDefaultTextures() {
        System.out.println("🎨 定义方块纹理: stone -> SINGLE");
        defineBlockTexture("stone", BlockTextureDefinition.singleTexture("Textures/blocks/dirt.png"));

        System.out.println("🎨 定义方块纹理: dirt -> SINGLE");
        defineBlockTexture("dirt", BlockTextureDefinition.singleTexture("Textures/blocks/dirt.png"));

        System.out.println("🎨 定义方块纹理: glass -> SINGLE");
        defineBlockTexture("glass", BlockTextureDefinition.singleTexture("Textures/blocks/dirt.png"));

        System.out.println("🎨 定义方块纹理: cobblestone -> SINGLE");
        defineBlockTexture("cobblestone", BlockTextureDefinition.singleTexture("Textures/blocks/dirt.png"));

        System.out.println("🎨 定义方块纹理: wood -> SINGLE");
        defineBlockTexture("wood", BlockTextureDefinition.singleTexture("Textures/blocks/dirt.png"));

        System.out.println("🎨 定义方块纹理: grass -> THREE_TEXTURE");
        defineBlockTexture("grass", BlockTextureDefinition.threeTexture(
                "Textures/blocks/dirt.png",  // 顶部
                "Textures/blocks/dirt.png",  // 侧面
                "Textures/blocks/dirt.png"   // 底部
        ));

        System.out.println("🎨 定义方块纹理: air -> SINGLE");
        defineBlockTexture("air", BlockTextureDefinition.singleTexture("Textures/blocks/dirt.png"));
    }

    /**
     * 🎨 定义方块纹理
     */
    public void defineBlockTexture(String blockId, BlockTextureDefinition definition) {
        textureDefinitions.put(blockId, definition);
        System.out.println("🎨 定义方块纹理: " + blockId + " -> " + definition.getType());
    }

    /**
     * 🎨 为方块创建材质
     */
    public Material createBlockMaterial(String blockId) {
        // 检查缓存
        if (materialCache.containsKey(blockId)) {
            return materialCache.get(blockId).clone();
        }

        if (assetManager == null) {
            System.err.println("❌ AssetManager 未初始化，无法创建材质");
            return createFallbackMaterial();
        }

        BlockTextureDefinition definition = textureDefinitions.get(blockId);
        if (definition == null) {
            System.err.println("❌ 未找到方块纹理定义: " + blockId);
            return createFallbackMaterial();
        }

        Material material = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");

        try {
            switch (definition.getType()) {
                case SINGLE:
                    applySingleTexture(material, definition);
                    break;
                case THREE_TEXTURE:
                    applyThreeTexture(material, definition);
                    break;
                default:
                    System.err.println("❌ 未知的纹理类型: " + definition.getType());
                    return createFallbackMaterial();
            }

            // 缓存材质
            materialCache.put(blockId, material);
            System.out.println("✅ 创建方块材质: " + blockId);
            return material.clone();

        } catch (Exception e) {
            System.err.println("❌ 创建材质失败: " + blockId + " - " + e.getMessage());
            return createFallbackMaterial();
        }
    }

    /**
     * 🎨 应用单一纹理
     */
    private void applySingleTexture(Material material, BlockTextureDefinition definition) {
        String texturePath = definition.getSingleTexture();
        System.out.println("🔍 尝试加载纹理: " + texturePath);

        if (texturePath != null && !texturePath.isEmpty()) {
            try {
                Texture texture = assetManager.loadTexture(texturePath);
                if (texture != null) {
                    material.setTexture("DiffuseMap", texture);
                    System.out.println("✅ 成功加载纹理: " + texturePath);
                    System.out.println("   纹理尺寸: " + texture.getImage().getWidth() + "x" + texture.getImage().getHeight());
                } else {
                    System.err.println("❌ 纹理加载返回 null: " + texturePath);
                    createMissingTexture(material);
                }
            } catch (Exception e) {
                System.err.println("❌ 加载纹理失败: " + texturePath + " - " + e.getMessage());
                e.printStackTrace(); // 打印完整错误堆栈
                createMissingTexture(material);
            }
        } else {
            System.err.println("❌ 纹理路径为空");
            createMissingTexture(material);
        }
    }

    /**
     * 🎨 应用三面纹理（简化版本 - 使用顶部纹理）
     */
    private void applyThreeTexture(Material material, BlockTextureDefinition definition) {
        // 目前简化实现：使用顶部纹理作为主纹理
        // 未来可以扩展为真正的多面纹理支持
        String topTexture = definition.getTopTexture();
        if (topTexture != null && !topTexture.isEmpty()) {
            try {
                Texture texture = assetManager.loadTexture(topTexture);
                material.setTexture("DiffuseMap", texture);
                System.out.println("🎨 应用三面纹理（顶部）: " + topTexture);
            } catch (Exception e) {
                System.err.println("❌ 加载顶部纹理失败: " + topTexture + " - " + e.getMessage());
                createMissingTexture(material);
            }
        } else {
            createMissingTexture(material);
        }
    }

    /**
     * 🎨 创建缺失纹理（粉红色）
     */
    private void createMissingTexture(Material material) {
        try {
            // 创建一个简单的粉红色纹理表示缺失的纹理
            material.setBoolean("UseMaterialColors", true);
            material.setColor("Diffuse", ColorRGBA.Magenta);
            System.out.println("🎨 使用缺失纹理（粉红色）");
        } catch (Exception e) {
            System.err.println("❌ 创建缺失纹理失败: " + e.getMessage());
        }
    }

    /**
     * 🎨 创建后备材质
     */
    private Material createFallbackMaterial() {
        if (assetManager != null) {
            try {
                Material material = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
                material.setBoolean("UseMaterialColors", true);
                material.setColor("Diffuse", ColorRGBA.Gray);
                return material;
            } catch (Exception e) {
                System.err.println("❌ 创建后备材质失败: " + e.getMessage());
            }
        }
        return null;
    }

    /**
     * 🎨 获取纹理定义
     */
    public BlockTextureDefinition getTextureDefinition(String blockId) {
        return textureDefinitions.get(blockId);
    }

    /**
     * 🎨 清理缓存
     */
    public void clearCache() {
        materialCache.clear();
        System.out.println("🎨 材质缓存已清理");
    }

    /**
     * 🎨 获取统计信息
     */
    public void printStatistics() {
        System.out.println("🎨 纹理管理器统计:");
        System.out.println("   纹理定义数量: " + textureDefinitions.size());
        System.out.println("   材质缓存数量: " + materialCache.size());
    }
}
