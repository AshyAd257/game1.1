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
    // 自定义模型（modelPath非空时）外挂的贴图路径（classpath相对路径，如"Textures/blocks/xx.png"）。
    // 与wood1一类"贴图直接烘焙进.glb内部"的旧模型不同：这个路径指向一张独立的png文件，
    // 加载模型后会在其上覆盖设置DiffuseMap，换贴图只需要换这个png，不需要重新导出模型。
    // null表示沿用模型自带的材质（wood1等旧模型的行为不变）。
    private final String modelTexturePath;
    // true=模型几何体已经按世界单位精确建模（如cube.glb/wedge.glb/halfbrick.glb这类由
    // generate_block_shapes.py生成的形状原型，halfbrick的Y方向故意就是0.5），加载后不能再
    // 做"按高度自动归一化缩放到1.0"的处理——那是给wood1一类"用Blender随便建模、原始尺寸
    // 不确定"的旧模型设计的兜底逻辑，如果套在已经精确建模的halfbrick上会被强行拉伸回1.0
    // 高度，且scale()是三轴等比缩放，宽/深会跟着被放大到2倍，导致方块比整格还大。
    // false=沿用旧的自动缩放行为（wood1等模型的行为不变）。
    private final boolean skipAutoScale;
    private final boolean isTransparent;
    private final Axis axis; // 摆放朝向轴（默认Y=竖直），仅对方向性方块（如原木）有意义
    private final String orientationGroup; // 同一族朝向变体共享的分组key（如"wood1"），null表示非方向性方块
    // 半砖族标识：同一材质的slabFamily相同（如"xx"），slabOrientation区分该变体是
    // BOTTOM/TOP/LEFT/RIGHT/FRONT/BACK/DOUBLE中的哪一个。两者都为null表示不是半砖，
    // 与原木的orientationGroup/axis是完全独立的另一套朝向机制（原木只有3个变体且
    // 不支持"叠放合并"，半砖有7个变体且合并逻辑不同，故不复用同一套字段）。
    private final String slabFamily;
    private final SlabOrientation slabOrientation;
    // 是否可以被玩家实际获得（挖掘/丢弃/拾取/物品栏自动注册）。默认true；
    // "air"这类技术方块（没有实体、不该出现在物品栏或掉落物里）显式调用setObtainable(false)。
    // 用可变字段+setter而不是塞进已经有7个重载的构造函数——那样要在每个重载链上都加一层参数，
    // 改动面远大于收益，而这个标记本身与"如何建模几何体/朝向"无关，加在构造之后完全合理。
    private boolean obtainable = true;

    /**
     * 创建一个新的方块类型（完整版本）
     */
    public Block(String id, String name, boolean solid, float hardness, BlockTexture texture, String modelPath, boolean isTransparent) {
        this(id, name, solid, hardness, texture, modelPath, null, isTransparent, Axis.Y, null, null, null);
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
        this(id, name, solid, hardness, null, null, null, isTransparent, Axis.Y, null, null, null);
    }

    /**
     * 带自定义模型的构造器（模型贴图烘焙在模型文件内部，如wood1.glb）
     */
    public Block(String id, String name, boolean solid, float hardness, boolean isTransparent, String modelPath) {
        this(id, name, solid, hardness, null, modelPath, null, isTransparent, Axis.Y, null, null, null);
    }

    /**
     * 带自定义模型+独立贴图路径的构造器：模型本身只提供几何形状/UV（如cube.glb/wedge.glb/
     * halfbrick.glb这类形状原型），贴图是外部单独的png文件，加载模型后另外覆盖设置。
     */
    public Block(String id, String name, boolean solid, float hardness, boolean isTransparent, String modelPath,
                 String modelTexturePath) {
        this(id, name, solid, hardness, null, modelPath, modelTexturePath, isTransparent, Axis.Y, null, null, null);
    }

    /**
     * 带朝向轴的构造器（用于原木一类方向性方块的某个具体朝向变体）
     *
     * @param axis 该变体摆放时长轴所沿的方向
     * @param orientationGroup 同一族朝向变体共享的分组key（如"wood1"），放置时用它找到"另外两个朝向的兄弟方块"
     */
    public Block(String id, String name, boolean solid, float hardness, boolean isTransparent, String modelPath,
                 Axis axis, String orientationGroup) {
        this(id, name, solid, hardness, null, modelPath, null, isTransparent, axis, orientationGroup, null, null);
    }

    /**
     * 带朝向轴+独立贴图路径的构造器（方向性方块使用形状原型模型+外部贴图的场景）
     */
    public Block(String id, String name, boolean solid, float hardness, boolean isTransparent, String modelPath,
                 String modelTexturePath, Axis axis, String orientationGroup) {
        this(id, name, solid, hardness, null, modelPath, modelTexturePath, isTransparent, axis, orientationGroup, null, null);
    }

    /**
     * 半砖族变体的构造器：模型是generate_block_shapes.py生成的形状原型
     * （slab_bottom.glb等6个单朝向，或叠满态直接复用cube.glb），贴图统一是同一张
     * 完整方块贴图（美术只画一张，模型UV各自采样其中一半区域，叠满态则采样整张）。
     *
     * @param slabFamily 同一材质的所有变体共享的分组key（如"xx"），用于放置时查找同族的其他朝向
     * @param slabOrientation 该变体的具体朝向
     */
    public Block(String id, String name, boolean solid, float hardness, String modelPath, String modelTexturePath,
                 String slabFamily, SlabOrientation slabOrientation) {
        this(id, name, solid, hardness, null, modelPath, modelTexturePath, false, Axis.Y, null, slabFamily, slabOrientation);
    }

    private Block(String id, String name, boolean solid, float hardness, BlockTexture texture, String modelPath,
                   String modelTexturePath, boolean isTransparent, Axis axis, String orientationGroup,
                   String slabFamily, SlabOrientation slabOrientation) {
        this.id = id;
        this.name = name;
        this.solid = solid;
        this.hardness = hardness;
        this.texture = texture;
        this.modelPath = modelPath;
        this.modelTexturePath = modelTexturePath;
        // 目前只有"形状原型+外部贴图"这条新路径（modelTexturePath非空）在用精确建模的模型
        // （cube/wedge/halfbrick/slab_*），wood1一类旧模型走的是"贴图烘焙进glb内部"
        // （modelTexturePath为null）+需要自动缩放。用modelTexturePath是否存在来推断是否
        // 跳过自动缩放，两条路径目前完全对应，不需要再单独加一个构造器参数。
        this.skipAutoScale = modelTexturePath != null && !modelTexturePath.isEmpty();
        this.isTransparent = isTransparent;
        this.axis = axis;
        this.orientationGroup = orientationGroup;
        this.slabFamily = slabFamily;
        this.slabOrientation = slabOrientation;
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

    public String getModelTexturePath() {
        return modelTexturePath;
    }

    public boolean hasModelTexture() {
        return modelTexturePath != null && !modelTexturePath.isEmpty();
    }

    public boolean isSkipAutoScale() {
        return skipAutoScale;
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

    public String getSlabFamily() {
        return slabFamily;
    }

    public SlabOrientation getSlabOrientation() {
        return slabOrientation;
    }

    public boolean isSlabPart() {
        return slabFamily != null;
    }

    public boolean isObtainable() {
        return obtainable;
    }

    /**
     * 标记该方块是否可以被玩家实际获得（用于排除air等技术方块，不参与物品栏自动注册）。
     */
    public Block setObtainable(boolean obtainable) {
        this.obtainable = obtainable;
        return this;
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
