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
    private final boolean isTransparent;
    private final Axis axis; // 摆放朝向轴（默认Y=竖直），仅对方向性方块（如原木）有意义
    private final String orientationGroup; // 同一族朝向变体共享的分组key（如"wood1"），null表示非方向性方块

    /**
     * 创建一个新的方块类型（完整版本）
     */
    public Block(String id, String name, boolean solid, float hardness, BlockTexture texture, String modelPath, boolean isTransparent) {
        this(id, name, solid, hardness, texture, modelPath, isTransparent, Axis.Y, null);
    }

    /**
     * 创建一个使用默认立方体的方块类型（向后兼容）
     */
    public Block(String id, String name, boolean solid, float hardness, BlockTexture texture) {
        this(id, name, solid, hardness, texture, null, false);
    }

    /**
     * 新构造器：简化版本，不需要 BlockTexture 对象
     * 纹理由 BlockTextureManager 管理
     */
    public Block(String id, String name, boolean solid, float hardness, boolean isTransparent) {
        this(id, name, solid, hardness, null, null, isTransparent, Axis.Y, null);
    }

    /**
     * 带自定义模型的构造器
     */
    public Block(String id, String name, boolean solid, float hardness, boolean isTransparent, String modelPath) {
        this(id, name, solid, hardness, null, modelPath, isTransparent, Axis.Y, null);
    }

    /**
     * 带朝向轴的构造器（用于原木一类方向性方块的某个具体朝向变体）
     *
     * @param axis 该变体摆放时长轴所沿的方向
     * @param orientationGroup 同一族朝向变体共享的分组key（如"wood1"），放置时用它找到"另外两个朝向的兄弟方块"
     */
    public Block(String id, String name, boolean solid, float hardness, boolean isTransparent, String modelPath,
                 Axis axis, String orientationGroup) {
        this(id, name, solid, hardness, null, modelPath, isTransparent, axis, orientationGroup);
    }

    private Block(String id, String name, boolean solid, float hardness, BlockTexture texture, String modelPath,
                   boolean isTransparent, Axis axis, String orientationGroup) {
        this.id = id;
        this.name = name;
        this.solid = solid;
        this.hardness = hardness;
        this.texture = texture;
        this.modelPath = modelPath;
        this.isTransparent = isTransparent;
        this.axis = axis;
        this.orientationGroup = orientationGroup;
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

    public Axis getAxis() {
        return axis;
    }

    public String getOrientationGroup() {
        return orientationGroup;
    }

    public boolean isDirectional() {
        return orientationGroup != null;
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
            } catch (Exception e) {
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
        // 递归遍历所有几何体并应用材质
        spatial.depthFirstTraversal(s -> {
            if (s instanceof Geometry) {
                Geometry geom = (Geometry) s;
                Material material = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
                geom.setMaterial(material);
            }
        });
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
