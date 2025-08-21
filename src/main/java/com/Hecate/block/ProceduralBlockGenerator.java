package com.Hecate.block;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.BufferUtils;
import com.Hecate.texture.BlockTextureManager;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * 🧊 程序化方块生成器 - 支持不同尺寸和纹理
 */
public class ProceduralBlockGenerator {
    private final AssetManager assetManager;
    private final BlockTextureManager textureManager;

    public enum BlockSize {
        SMALL(0.75f),
        NORMAL(1.0f),
        LARGE(1.5f);

        private final float scale;
        BlockSize(float scale) { this.scale = scale; }
        public float getScale() { return scale; }
    }

    public ProceduralBlockGenerator(AssetManager assetManager, BlockTextureManager textureManager) {
        this.assetManager = assetManager;
        this.textureManager = textureManager;
    }

    /**
     * 🧊 创建方块几何体
     */
    public Geometry createBlock(String blockId, BlockSize size, Vector3f position) {
        float scale = size.getScale();

        // 创建立方体网格
        Mesh mesh = createCubeMesh(scale);

        // 创建几何体
        Geometry blockGeometry = new Geometry("Block_" + blockId + "_" + size.name(), mesh);

        // 应用材质
        Material material = textureManager.createBlockMaterial(blockId);
        blockGeometry.setMaterial(material);

        // 设置位置
        blockGeometry.setLocalTranslation(position);

        return blockGeometry;
    }

    /**
     * 🧊 创建立方体网格（带正确的UV坐标）
     */
    private Mesh createCubeMesh(float size) {
        float half = size / 2f;

        // 顶点坐标（24个顶点，每面4个）
        float[] vertices = {
                // 前面 (Z+)
                -half, -half,  half,   half, -half,  half,   half,  half,  half,  -half,  half,  half,
                // 后面 (Z-)
                half, -half, -half,  -half, -half, -half,  -half,  half, -half,   half,  half, -half,
                // 左面 (X-)
                -half, -half, -half,  -half, -half,  half,  -half,  half,  half,  -half,  half, -half,
                // 右面 (X+)
                half, -half,  half,   half, -half, -half,   half,  half, -half,   half,  half,  half,
                // 顶面 (Y+)
                -half,  half,  half,   half,  half,  half,   half,  half, -half,  -half,  half, -half,
                // 底面 (Y-)
                -half, -half, -half,   half, -half, -half,   half, -half,  half,  -half, -half,  half
        };

        // 纹理坐标（每面都是标准的0-1映射）
        float[] texCoords = {
                // 前面
                0, 0,  1, 0,  1, 1,  0, 1,
                // 后面
                0, 0,  1, 0,  1, 1,  0, 1,
                // 左面
                0, 0,  1, 0,  1, 1,  0, 1,
                // 右面
                0, 0,  1, 0,  1, 1,  0, 1,
                // 顶面
                0, 0,  1, 0,  1, 1,  0, 1,
                // 底面
                0, 0,  1, 0,  1, 1,  0, 1
        };

        // 法线向量
        float[] normals = {
                // 前面
                0, 0, 1,  0, 0, 1,  0, 0, 1,  0, 0, 1,
                // 后面
                0, 0, -1,  0, 0, -1,  0, 0, -1,  0, 0, -1,
                // 左面
                -1, 0, 0,  -1, 0, 0,  -1, 0, 0,  -1, 0, 0,
                // 右面
                1, 0, 0,  1, 0, 0,  1, 0, 0,  1, 0, 0,
                // 顶面
                0, 1, 0,  0, 1, 0,  0, 1, 0,  0, 1, 0,
                // 底面
                0, -1, 0,  0, -1, 0,  0, -1, 0,  0, -1, 0
        };

        // 索引（每面两个三角形）
        int[] indices = {
                // 前面
                0, 1, 2,  0, 2, 3,
                // 后面
                4, 5, 6,  4, 6, 7,
                // 左面
                8, 9, 10,  8, 10, 11,
                // 右面
                12, 13, 14,  12, 14, 15,
                // 顶面
                16, 17, 18,  16, 18, 19,
                // 底面
                20, 21, 22,  20, 22, 23
        };

        // 创建网格
        Mesh mesh = new Mesh();

        // 设置顶点缓冲区
        FloatBuffer vertexBuffer = BufferUtils.createFloatBuffer(vertices);
        mesh.setBuffer(VertexBuffer.Type.Position, 3, vertexBuffer);

        // 设置纹理坐标缓冲区
        FloatBuffer texCoordBuffer = BufferUtils.createFloatBuffer(texCoords);
        mesh.setBuffer(VertexBuffer.Type.TexCoord, 2, texCoordBuffer);

        // 设置法线缓冲区
        FloatBuffer normalBuffer = BufferUtils.createFloatBuffer(normals);
        mesh.setBuffer(VertexBuffer.Type.Normal, 3, normalBuffer);

        // 设置索引缓冲区
        IntBuffer indexBuffer = BufferUtils.createIntBuffer(indices);
        mesh.setBuffer(VertexBuffer.Type.Index, 3, indexBuffer);

        // 更新边界
        mesh.updateBound();

        return mesh;
    }
}
