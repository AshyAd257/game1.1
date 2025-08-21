package com.Hecate.texture;

/**
 * 🎨 方块纹理定义 - 描述方块的纹理配置
 */
public class BlockTextureDefinition {

    public enum TextureType {
        SINGLE,         // 单一纹理（所有面相同）
        THREE_TEXTURE   // 三面纹理（顶部、底部、侧面）
    }

    private final TextureType type;
    private final String singleTexture;
    private final String topTexture;
    private final String bottomTexture;
    private final String sideTexture;

    // 私有构造器
    private BlockTextureDefinition(TextureType type, String singleTexture,
                                   String topTexture, String bottomTexture, String sideTexture) {
        this.type = type;
        this.singleTexture = singleTexture;
        this.topTexture = topTexture;
        this.bottomTexture = bottomTexture;
        this.sideTexture = sideTexture;
    }

    /**
     * 🎨 创建单一纹理定义
     */
    public static BlockTextureDefinition singleTexture(String texturePath) {
        return new BlockTextureDefinition(TextureType.SINGLE, texturePath, null, null, null);
    }

    /**
     * 🎨 创建三面纹理定义
     */
    public static BlockTextureDefinition threeTexture(String topTexture, String sideTexture, String bottomTexture) {
        return new BlockTextureDefinition(TextureType.THREE_TEXTURE, null, topTexture, bottomTexture, sideTexture);
    }

    // Getter 方法
    public TextureType getType() {
        return type;
    }

    public String getSingleTexture() {
        return singleTexture;
    }

    public String getTopTexture() {
        return topTexture;
    }

    public String getBottomTexture() {
        return bottomTexture;
    }

    public String getSideTexture() {
        return sideTexture;
    }

    @Override
    public String toString() {
        switch (type) {
            case SINGLE:
                return "SingleTexture{" + singleTexture + "}";
            case THREE_TEXTURE:
                return "ThreeTexture{top=" + topTexture + ", side=" + sideTexture + ", bottom=" + bottomTexture + "}";
            default:
                return "UnknownTexture{}";
        }
    }
}
