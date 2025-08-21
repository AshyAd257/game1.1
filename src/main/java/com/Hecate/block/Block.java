package com.Hecate.block;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;

/**
 * 表示游戏中的基本方块
 */
public class Block {
    private final String id;
    private final String name;
    private final boolean solid;
    private final float hardness;
    private final BlockTexture texture;
    private final String modelPath;
    private final boolean isTransparent; // 新增：透明度属性

    /**
     * 创建一个新的方块类型（完整版本）
     */
    public Block(String id, String name, boolean solid, float hardness, BlockTexture texture, String modelPath, boolean isTransparent) {
        this.id = id;
        this.name = name;
        this.solid = solid;
        this.hardness = hardness;
        this.texture = texture;
        this.modelPath = modelPath;
        this.isTransparent = isTransparent;
    }

    /**
     * 创建一个使用默认立方体的方块类型（向后兼容）
     */
    public Block(String id, String name, boolean solid, float hardness, BlockTexture texture) {
        this(id, name, solid, hardness, texture, null, false);
    }

    /**
     * 🆕 新构造器：简化版本，不需要 BlockTexture 对象
     * 纹理由 BlockTextureManager 管理
     */
    public Block(String id, String name, boolean solid, float hardness, boolean isTransparent) {
        this.id = id;
        this.name = name;
        this.solid = solid;
        this.hardness = hardness;
        this.texture = null; // 纹理由外部管理
        this.modelPath = null;
        this.isTransparent = isTransparent;
        System.out.println("🧊 创建方块: " + id + " (透明:" + isTransparent + ")");
    }

    /**
     * 🆕 带自定义模型的构造器
     */
    public Block(String id, String name, boolean solid, float hardness, boolean isTransparent, String modelPath) {
        this.id = id;
        this.name = name;
        this.solid = solid;
        this.hardness = hardness;
        this.texture = null;
        this.modelPath = modelPath;
        this.isTransparent = isTransparent;
        System.out.println("🧊 创建自定义模型方块: " + id + " -> " + modelPath);
    }

    // Getter 方法
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean isSolid() {
        return solid;
    }

    public float getHardness() {
        return hardness;
    }

    public BlockTexture getTexture() {
        return texture;
    }

    public String getModelPath() {
        return modelPath;
    }

    public boolean isTransparent() {
        return isTransparent;
    }

    public boolean hasCustomModel() {
        return modelPath != null && !modelPath.isEmpty();
    }

    /**
     * 创建方块的几何体表示
     * @param position 方块在世界中的位置
     * @param assetManager 资源管理器
     * @return 方块的几何体
     */
    public Spatial createGeometry(Vector3f position, AssetManager assetManager) {
        Spatial blockSpatial;

        if (hasCustomModel()) {
            // 使用自定义OBJ模型
            try {
                blockSpatial = assetManager.loadModel(modelPath);
                blockSpatial.setName("Block_" + id + "_" + position.toString());

                // 应用纹理到模型
                if (texture != null) {
                    applyTextureToSpatial(blockSpatial, assetManager);
                }

                System.out.println("✅ 加载自定义模型: " + id + " -> " + modelPath);
            } catch (Exception e) {
                System.err.println("❌ 加载自定义模型失败: " + modelPath + ", 使用默认立方体");
                e.printStackTrace();
                blockSpatial = createDefaultCube(position, assetManager);
            }
        } else {
            // 使用默认立方体
            blockSpatial = createDefaultCube(position, assetManager);
        }

        // 设置位置
        blockSpatial.setLocalTranslation(position);
        return blockSpatial;
    }

    /**
     * 创建默认立方体
     */
    private Spatial createDefaultCube(Vector3f position, AssetManager assetManager) {
        com.jme3.scene.shape.Box box = new com.jme3.scene.shape.Box(0.5f, 0.5f, 0.5f);
        Geometry geometry = new Geometry("Block_" + id + "_" + position.toString(), box);

        // 应用纹理
        if (texture != null) {
            applyTextureToSpatial(geometry, assetManager);
        } else {
            // 使用默认材质
            Material material = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
            material.setBoolean("UseMaterialColors", true);
            material.setColor("Diffuse", com.jme3.math.ColorRGBA.Gray);
            geometry.setMaterial(material);
        }

        return geometry;
    }

    /**
     * 将纹理应用到空间对象
     */
    private void applyTextureToSpatial(Spatial spatial, AssetManager assetManager) {
        if (spatial instanceof Geometry) {
            Geometry geom = (Geometry) spatial;
            Material material = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");

            if (texture != null) {
                // 这里需要更新为使用新的纹理系统
                System.out.println("⚠️ 使用了旧的纹理应用方法，建议使用 BlockTextureManager");
            }

            geom.setMaterial(material);
        } else {
            // 如果是Node，递归应用到所有子几何体
            spatial.depthFirstTraversal(s -> {
                if (s instanceof Geometry) {
                    Geometry geom = (Geometry) s;
                    Material material = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");

                    if (texture != null) {
                        System.out.println("⚠️ 使用了旧的纹理应用方法，建议使用 BlockTextureManager");
                    }

                    geom.setMaterial(material);
                }
            });
        }
    }

    @Override
    public String toString() {
        return "Block{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", solid=" + solid +
                ", hardness=" + hardness +
                ", transparent=" + isTransparent +
                ", hasCustomModel=" + hasCustomModel() +
                '}';
    }
}
