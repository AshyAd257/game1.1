package com.Hecate.block;

import com.Hecate.texture.BlockTextureDefinition;
import com.jme3.material.Material;

/**
 * 🎨 方块纹理类 - 使用新的纹理系统
 */
public class BlockTexture {
    private final String textureId;
    private final String texturePath;
    private final boolean isTransparent;
    private final BlockTextureDefinition definition;

    /**
     * 单一纹理构造器
     */
    public BlockTexture(String textureId, String texturePath) {
        this(textureId, texturePath, false);
    }

    /**
     * 单一纹理构造器（带透明度）
     */
    public BlockTexture(String textureId, String texturePath, boolean isTransparent) {
        this.textureId = textureId;
        this.texturePath = texturePath;
        this.isTransparent = isTransparent;
        this.definition = BlockTextureDefinition.singleTexture(texturePath);
        System.out.println("🎨 创建单一纹理: " + textureId + " -> " + texturePath);
    }

    /**
     * 🎨 多面纹理构造器
     */
    public BlockTexture(String textureId, String topTexture, String bottomTexture, String sideTexture) {
        this(textureId, topTexture, bottomTexture, sideTexture, false);
    }

    /**
     * 🎨 多面纹理构造器（带透明度）
     */
    public BlockTexture(String textureId, String topTexture, String bottomTexture, String sideTexture, boolean isTransparent) {
        this.textureId = textureId;
        this.texturePath = topTexture; // 主纹理路径
        this.isTransparent = isTransparent;
        this.definition = BlockTextureDefinition.threeTexture(topTexture, sideTexture, bottomTexture);
        System.out.println("🎨 创建多面纹理: " + textureId + " (顶:" + topTexture + ", 侧:" + sideTexture + ", 底:" + bottomTexture + ")");
    }

    /**
     * 🎨 兼容性方法：加载纹理
     * @deprecated 纹理现在由 BlockTextureManager 自动管理
     */
    @Deprecated
    public void load(TextureManager textureManager) {
        System.out.println("⚠️ BlockTexture.load() 已废弃，纹理由 BlockTextureManager 自动管理");
        System.out.println("🎨 纹理: " + textureId);
    }

    /**
     * 🎨 兼容性方法：应用材质
     * @deprecated 使用 BlockTextureManager.createBlockMaterial() 代替
     */
    @Deprecated
    public void applyToMaterial(Material material) {
        System.out.println("⚠️ applyToMaterial() 已废弃，请使用 BlockTextureManager.createBlockMaterial()");
        System.out.println("🎨 纹理ID: " + textureId);
        // 这里可以添加基本的纹理应用逻辑作为后备
    }

    // Getter方法
    public String getTextureId() {
        return textureId;
    }

    public String getTexturePath() {
        return texturePath;
    }

    public boolean isTransparent() {
        return isTransparent;
    }

    /**
     * 获取纹理定义（新系统）
     */
    public BlockTextureDefinition getDefinition() {
        return definition;
    }

    /**
     * 兼容性方法：获取配置
     * @deprecated 使用 getDefinition() 代替
     */
    @Deprecated
    public TextureManager.BlockTextureConfig getConfig() {
        System.out.println("⚠️ getConfig() 已废弃，请使用 getDefinition()");

        // 返回兼容的配置对象
        if (definition.getType() == BlockTextureDefinition.TextureType.SINGLE) {
            return new TextureManager.BlockTextureConfig(definition.getSingleTexture());
        } else {
            return new TextureManager.BlockTextureConfig(
                    definition.getTopTexture(),
                    definition.getBottomTexture(),
                    definition.getSideTexture()
            );
        }
    }

    @Override
    public String toString() {
        return "BlockTexture{" +
                "id='" + textureId + '\'' +
                ", path='" + texturePath + '\'' +
                ", transparent=" + isTransparent +
                ", definition=" + definition +
                '}';
    }
}
