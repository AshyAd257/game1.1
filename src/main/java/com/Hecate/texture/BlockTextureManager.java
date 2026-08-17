package com.Hecate.texture;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.texture.Texture;
import com.Hecate.utils.LogUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Block texture manager that owns texture definitions and cached materials.
 */
public class BlockTextureManager {
    private static final String MATERIAL_DEFINITION = "Common/MatDefs/Light/Lighting.j3md";

    private final AssetManager assetManager;
    private final Map<String, BlockTextureDefinition> textureDefinitions = new HashMap<>();
    private final Map<String, Material> materialCache = new HashMap<>();

    public BlockTextureManager() {
        this.assetManager = null;
        LogUtils.warning(BlockTextureManager.class, "BlockTextureManager created without AssetManager");
    }

    public BlockTextureManager(AssetManager assetManager) {
        this.assetManager = assetManager;
    }

    public void initializeDefaultTextures() {
        BlockTextureDefaults.registerAll(this);
    }

    /**
     * Register or update a texture definition.
     */
    public void defineBlockTexture(String blockId, BlockTextureDefinition definition) {
        textureDefinitions.put(blockId, definition);
        materialCache.remove(blockId); // invalidate cache when definition changes
    }

    /**
     * Create a material for the supplied block id.
     */
    public Material createBlockMaterial(String blockId) {
        Material cached = materialCache.get(blockId);
        if (cached != null) {
            return cached.clone();
        }

        if (assetManager == null) {
            LogUtils.error(BlockTextureManager.class, "AssetManager is not available. Unable to create material: " + blockId);
            return createFallbackMaterial();
        }

        BlockTextureDefinition definition = textureDefinitions.get(blockId);
        if (definition == null) {
            LogUtils.error(BlockTextureManager.class, "No texture definition registered for block: " + blockId);
            return createFallbackMaterial();
        }

        Material material = createLightingMaterial();
        if (material == null) {
            return null;
        }

        if (!applyDefinition(material, definition)) {
            applyFallbackColors(material);
        }

        materialCache.put(blockId, material);
        return material.clone();
    }

    private boolean applyDefinition(Material material, BlockTextureDefinition definition) {
        switch (definition.getType()) {
            case SINGLE:
                return applySingleTexture(material, definition.getSingleTexture());
            case THREE_TEXTURE:
                return applyThreeTexture(material, definition);
            default:
                LogUtils.error(BlockTextureManager.class, "Unsupported texture type: " + definition.getType());
                return false;
        }
    }

    /**
     * Apply a single texture to the material.
     */
    private boolean applySingleTexture(Material material, String texturePath) {
        return configureDiffuseTexture(material, texturePath);
    }

    /**
     * Apply the top face texture (current simplified behaviour).
     */
    private boolean applyThreeTexture(Material material, BlockTextureDefinition definition) {
        return configureDiffuseTexture(material, definition.getTopTexture());
    }

    private boolean configureDiffuseTexture(Material material, String texturePath) {
        if (texturePath == null || texturePath.isEmpty()) {
            LogUtils.error(BlockTextureManager.class, "Texture path is empty");
            return false;
        }

        // 【关键测试】完全复制Box测试地面的材质设置
        material.setBoolean("UseMaterialColors", true);
        material.setColor("Diffuse", new ColorRGBA(1.0f, 1.0f, 0.6f, 1.0f));
        material.setColor("Ambient", new ColorRGBA(1.0f, 1.0f, 0.6f, 1.0f));
        material.setColor("Specular", ColorRGBA.White.mult(0.1f));
        material.setFloat("Shininess", 8.0f);

        // 渲染状态 - 与Box测试地面完全一致
        material.getAdditionalRenderState().setDepthWrite(true);
        material.getAdditionalRenderState().setDepthTest(true);
        material.getAdditionalRenderState().setFaceCullMode(com.jme3.material.RenderState.FaceCullMode.Back);
        // 不设置BlendMode，让它保持默认值

        return true;

        /* 原始纹理加载代码
        Texture texture = loadTexture(texturePath);
        if (texture == null) {
            return false;
        }

        configureMaterialForTexture(material);
        material.setTexture("DiffuseMap", texture);
        return true;
        */
    }

    private Texture loadTexture(String texturePath) {
        if (assetManager == null) {
            return null;
        }

        try {
            Texture texture = assetManager.loadTexture(texturePath);
            if (texture == null) {
                LogUtils.error(BlockTextureManager.class, "AssetManager returned null texture: " + texturePath);
                return null;
            }

            configureTextureSampling(texture);
            return texture;
        } catch (Exception e) {
            LogUtils.error(BlockTextureManager.class, "Failed to load texture: " + texturePath, e);
            return null;
        }
    }

    private void configureTextureSampling(Texture texture) {
        texture.setMagFilter(Texture.MagFilter.Nearest);
        texture.setMinFilter(Texture.MinFilter.NearestNoMipMaps);
        texture.setWrap(Texture.WrapMode.Repeat);
        texture.setAnisotropicFilter(0);
    }

    private void configureMaterialForTexture(Material material) {
        material.setBoolean("UseMaterialColors", false);
        material.setColor("Diffuse", ColorRGBA.White);
        material.setColor("Ambient", ColorRGBA.White);
        material.setColor("Specular", ColorRGBA.White.mult(0.1f));
        material.setFloat("Shininess", 8.0f);

        // 【关键修复】确保材质接收光照和阴影
        // 这些设置与测试地面完全一致，确保体素地形能够接收阴影
        material.getAdditionalRenderState().setFaceCullMode(com.jme3.material.RenderState.FaceCullMode.Back);

        // 显式启用深度写入和深度测试（接收阴影的关键设置）
        material.getAdditionalRenderState().setDepthWrite(true);
        material.getAdditionalRenderState().setDepthTest(true);

        // 【新增】禁用混合模式，确保材质完全不透明（与测试地面一致）
        material.getAdditionalRenderState().setBlendMode(com.jme3.material.RenderState.BlendMode.Off);
    }

    private void applyFallbackColors(Material material) {
        material.setBoolean("UseMaterialColors", true);
        material.setColor("Diffuse", new ColorRGBA(1.0f, 0.0f, 1.0f, 1.0f));
        material.setColor("Ambient", new ColorRGBA(1.0f, 0.0f, 1.0f, 1.0f));
        material.setColor("Specular", ColorRGBA.White.mult(0.1f));
        material.setFloat("Shininess", 8.0f);

        // 【关键修复】确保材质接收光照和阴影（与测试地面一致）
        material.getAdditionalRenderState().setFaceCullMode(com.jme3.material.RenderState.FaceCullMode.Back);

        // 显式启用深度写入和深度测试（接收阴影的关键设置）
        material.getAdditionalRenderState().setDepthWrite(true);
        material.getAdditionalRenderState().setDepthTest(true);

        // 【新增】禁用混合模式，确保材质完全不透明（与测试地面一致）
        material.getAdditionalRenderState().setBlendMode(com.jme3.material.RenderState.BlendMode.Off);
    }

    private Material createFallbackMaterial() {
        Material material = createLightingMaterial();
        if (material != null) {
            applyFallbackColors(material);
        }
        return material;
    }

    private Material createLightingMaterial() {
        if (assetManager == null) {
            return null;
        }
        try {
            return new Material(assetManager, MATERIAL_DEFINITION);
        } catch (Exception e) {
            LogUtils.error(BlockTextureManager.class, "Failed to create material", e);
            return null;
        }
    }

    /**
     * Retrieve the registered definition for a block.
     */
    public BlockTextureDefinition getTextureDefinition(String blockId) {
        return textureDefinitions.get(blockId);
    }

    /**
     * Clear all cached materials.
     */
    public void clearCache() {
        materialCache.clear();
    }

    /**
     * Print simple statistics for debugging.
     */
    public void printStatistics() {
    }
}
