package com.Hecate.world;

import com.Hecate.block.Block;
import com.Hecate.block.BlockRegistry;
import com.Hecate.texture.BlockFace;
import com.Hecate.texture.BlockTextureManager;
import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import java.util.HashMap;
import java.util.Map;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.BufferUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 🎨 区块渲染器 - 简化版本
 */
public class ChunkRenderer {
    private final AssetManager assetManager;
    private final BlockTextureManager textureManager;

    public ChunkRenderer(AssetManager assetManager, BlockTextureManager textureManager) {
        this.assetManager = assetManager;
        this.textureManager = textureManager;

    }

    /**
     * 渲染区块为3D网格
     * 【诊断模式】为每个面创建独立几何体以测试阴影
     */
    public Node renderChunk(Chunk chunk) {

        // 【临时禁用缓存】强制重新渲染以测试新材质
        // if (!chunk.isDirty() && chunk.getChunkNode() != null) {
        //     return chunk.getChunkNode();
        // }

        Node chunkNode = new Node("Chunk_" + chunk.getPosition());
        Vector3f chunkWorldPos = chunk.getWorldPosition();

        // 【诊断模式】为每个面创建独立的几何体
        // 遍历区块中的所有方块
        for (int x = 0; x < Chunk.SIZE; x++) {
            for (int y = 0; y < Chunk.SIZE; y++) {
                for (int z = 0; z < Chunk.SIZE; z++) {
                    String blockId = chunk.getBlockId(x, y, z);

                    // 只渲染非空气方块
                    if (!blockId.equals("air")) {
                        // 检查每个面是否需要渲染
                        for (BlockFace face : BlockFace.values()) {
                            if (shouldRenderFace(chunk, x, y, z, face)) {
                                // 为每个面创建独立的MeshData
                                MeshData meshData = new MeshData();

                                addFaceToMesh(
                                        x + chunkWorldPos.x,
                                        y + chunkWorldPos.y,
                                        z + chunkWorldPos.z,
                                        face,
                                        meshData.vertices,
                                        meshData.normals,
                                        meshData.texCoords,
                                        meshData.indices
                                );

                                // 为这个面创建独立的几何体
                                Geometry faceGeom = createBlockGeometry(
                                        meshData.vertices,
                                        meshData.normals,
                                        meshData.texCoords,
                                        meshData.indices
                                );

                                if (faceGeom != null) {
                                    Material mat = textureManager.createBlockMaterial(blockId);
                                    faceGeom.setMaterial(mat);
                                    faceGeom.setName("Face_" + blockId + "_" + x + "_" + y + "_" + z + "_" + face);
                                    chunkNode.attachChild(faceGeom);
                                }
                            }
                        }
                    }
                }
            }
        }

        chunk.setChunkNode(chunkNode);
        chunk.setClean();
        return chunkNode;
    }

    // 添加内部类来存储网格数据
    private static class MeshData {
        List<Float> vertices = new ArrayList<>();
        List<Float> normals = new ArrayList<>();
        List<Float> texCoords = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();
    }

    /**
     * 判断某个面是否需要渲染
     */
    private boolean shouldRenderFace(Chunk chunk, int x, int y, int z, BlockFace face) {
        // 获取相邻方块的坐标
        int adjX = x, adjY = y, adjZ = z;

        switch (face) {
            case TOP: adjY++; break;
            case BOTTOM: adjY--; break;
            case NORTH: adjZ--; break;
            case SOUTH: adjZ++; break;
            case EAST: adjX++; break;
            case WEST: adjX--; break;
        }

        // 如果相邻位置超出区块边界，需要渲染
        if (adjX < 0 || adjX >= Chunk.SIZE ||
                adjY < 0 || adjY >= Chunk.SIZE ||
                adjZ < 0 || adjZ >= Chunk.SIZE) {
            return true;
        }

        // 如果相邻方块是空气，需要渲染这个面
        String adjBlockId = chunk.getBlockId(adjX, adjY, adjZ);
        return adjBlockId.equals("air");
    }

    /**
     * 将一个方块面添加到网格数据中
     */
    private void addFaceToMesh(float x, float y, float z, BlockFace face,
                               List<Float> vertices, List<Float> normals,
                               List<Float> texCoords, List<Integer> indices) {

        int startIndex = vertices.size() / 3;

        // 根据面的方向添加顶点
        switch (face) {
            case TOP:
                // 顶面 - 修正为逆时针顶点顺序（从上往下看）
                addVertex(vertices, x, y + 1, z);           // 0: back-left
                addVertex(vertices, x, y + 1, z + 1);       // 1: front-left
                addVertex(vertices, x + 1, y + 1, z + 1);   // 2: front-right
                addVertex(vertices, x + 1, y + 1, z);       // 3: back-right
                addNormal(normals, 0, 1, 0);
                break;

            case BOTTOM:
                // 底面
                addVertex(vertices, x, y, z + 1);
                addVertex(vertices, x + 1, y, z + 1);
                addVertex(vertices, x + 1, y, z);
                addVertex(vertices, x, y, z);
                addNormal(normals, 0, -1, 0);
                break;

            case NORTH:
                // 北面
                addVertex(vertices, x + 1, y, z);
                addVertex(vertices, x, y, z);
                addVertex(vertices, x, y + 1, z);
                addVertex(vertices, x + 1, y + 1, z);
                addNormal(normals, 0, 0, -1);
                break;

            case SOUTH:
                // 南面
                addVertex(vertices, x, y, z + 1);
                addVertex(vertices, x + 1, y, z + 1);
                addVertex(vertices, x + 1, y + 1, z + 1);
                addVertex(vertices, x, y + 1, z + 1);
                addNormal(normals, 0, 0, 1);
                break;

            case EAST:
                // 东面
                addVertex(vertices, x + 1, y, z + 1);
                addVertex(vertices, x + 1, y, z);
                addVertex(vertices, x + 1, y + 1, z);
                addVertex(vertices, x + 1, y + 1, z + 1);
                addNormal(normals, 1, 0, 0);
                break;

            case WEST:
                // 西面
                addVertex(vertices, x, y, z);
                addVertex(vertices, x, y, z + 1);
                addVertex(vertices, x, y + 1, z + 1);
                addVertex(vertices, x, y + 1, z);
                addNormal(normals, -1, 0, 0);
                break;
        }

        // 添加纹理坐标
        addTexCoords(texCoords);

        // 添加索引（两个三角形组成一个四边形）
        addQuadIndices(indices, startIndex);
    }

    private void addVertex(List<Float> vertices, float x, float y, float z) {
        vertices.add(x);
        vertices.add(y);
        vertices.add(z);
    }

    private void addNormal(List<Float> normals, float x, float y, float z) {
        // 为四个顶点添加相同的法向量
        for (int i = 0; i < 4; i++) {
            normals.add(x);
            normals.add(y);
            normals.add(z);
        }
    }

    private void addTexCoords(List<Float> texCoords) {
        // 四个角的纹理坐标
        texCoords.add(0.0f); texCoords.add(0.0f);
        texCoords.add(1.0f); texCoords.add(0.0f);
        texCoords.add(1.0f); texCoords.add(1.0f);
        texCoords.add(0.0f); texCoords.add(1.0f);
    }

    private void addQuadIndices(List<Integer> indices, int startIndex) {
        // 第一个三角形
        indices.add(startIndex);
        indices.add(startIndex + 1);
        indices.add(startIndex + 2);

        // 第二个三角形
        indices.add(startIndex);
        indices.add(startIndex + 2);
        indices.add(startIndex + 3);
    }

    /**
     * 创建方块几何体
     */
    private Geometry createBlockGeometry(List<Float> vertices, List<Float> normals,
                                         List<Float> texCoords, List<Integer> indices) {

        if (vertices.isEmpty()) {
            return null;
        }

        Mesh mesh = new Mesh();

        // 【关键】显式设置网格模式为Triangles
        mesh.setMode(Mesh.Mode.Triangles);

        // 转换为数组
        float[] vertexArray = listToFloatArray(vertices);
        float[] normalArray = listToFloatArray(normals);
        float[] texCoordArray = listToFloatArray(texCoords);
        int[] indexArray = listToIntArray(indices);

        // 设置网格数据
        mesh.setBuffer(VertexBuffer.Type.Position, 3, BufferUtils.createFloatBuffer(vertexArray));
        mesh.setBuffer(VertexBuffer.Type.Normal, 3, BufferUtils.createFloatBuffer(normalArray));
        mesh.setBuffer(VertexBuffer.Type.TexCoord, 2, BufferUtils.createFloatBuffer(texCoordArray));
        mesh.setBuffer(VertexBuffer.Type.Index, 3, BufferUtils.createIntBuffer(indexArray));

        // 【关键修复】生成切线数据（阴影渲染需要）
        // TangentBinormalGenerator会自动计算切线和副切线
        com.jme3.util.TangentBinormalGenerator.generate(mesh);

        mesh.updateBound();

        Geometry geometry = new Geometry("ChunkMesh", mesh);

        // 启用阴影投射和接收（让DirectionalLightShadowRenderer能够在地面上渲染阴影）
        geometry.setShadowMode(com.jme3.renderer.queue.RenderQueue.ShadowMode.CastAndReceive);

        // 显式设置为Opaque渲染队列（确保正确接收阴影）
        geometry.setQueueBucket(com.jme3.renderer.queue.RenderQueue.Bucket.Opaque);

        return geometry;
    }

    private float[] listToFloatArray(List<Float> list) {
        float[] array = new float[list.size()];
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i);
        }
        return array;
    }

    private int[] listToIntArray(List<Integer> list) {
        int[] array = new int[list.size()];
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i);
        }
        return array;
    }
}
