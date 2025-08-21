package com.Hecate.block;

import com.Hecate.texture.BlockTextureManager;
import com.Hecate.texture.BlockTextureDefinition;
import com.jme3.asset.AssetManager;
import com.jme3.material.Material;

/**
 * 🔄 TextureManager 兼容性适配器
 * 将旧的 TextureManager 调用重定向到新的 BlockTextureManager
 *
 * @deprecated 请使用 com.Hecate.texture.BlockTextureManager
 */
@Deprecated
public class TextureManager {
    private final BlockTextureManager blockTextureManager;

    public TextureManager(AssetManager assetManager) {
        this.blockTextureManager = new BlockTextureManager(assetManager);
        System.out.println("⚠️ 使用了已废弃的 TextureManager，请迁移到 BlockTextureManager");
    }

    /**
     * 兼容性方法：配置方块纹理
     */
    public void configureBlockTexture(String blockId, BlockTextureConfig config) {
        if (config.isSingleTexture()) {
            blockTextureManager.defineBlockTexture(blockId,
                    BlockTextureDefinition.singleTexture(config.getSingleTexture()));
        } else {
            blockTextureManager.defineBlockTexture(blockId,
                    BlockTextureDefinition.threeTexture(
                            config.getTopTexture(),
                            config.getSideTexture(),
                            config.getBottomTexture()));
        }
    }

    /**
     * 兼容性方法：为方块创建材质
     */
    public Material createMaterialForBlock(String blockId) {
        return blockTextureManager.createBlockMaterial(blockId);
    }

    /**
     * 兼容性配置类
     */
    public static class BlockTextureConfig {
        private final String singleTexture;
        private final String topTexture;
        private final String bottomTexture;
        private final String sideTexture;
        private final boolean isSingle;

        // 单一纹理构造器
        public BlockTextureConfig(String singleTexture) {
            this.singleTexture = singleTexture;
            this.topTexture = null;
            this.bottomTexture = null;
            this.sideTexture = null;
            this.isSingle = true;
        }

        // 三面纹理构造器
        public BlockTextureConfig(String topTexture, String bottomTexture, String sideTexture) {
            this.singleTexture = null;
            this.topTexture = topTexture;
            this.bottomTexture = bottomTexture;
            this.sideTexture = sideTexture;
            this.isSingle = false;
        }

        public boolean isSingleTexture() { return isSingle; }
        public String getSingleTexture() { return singleTexture; }
        public String getTopTexture() { return topTexture; }
        public String getBottomTexture() { return bottomTexture; }
        public String getSideTexture() { return sideTexture; }
    }
}
