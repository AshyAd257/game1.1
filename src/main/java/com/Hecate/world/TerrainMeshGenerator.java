package com.Hecate.world;

import com.jme3.math.Vector3f;
import com.jme3.math.Vector2f;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.BufferUtils;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * 地形网格生成器
 * 根据高度图生成三角网格
 */
public class TerrainMeshGenerator {

    /**
     * 生成地表网格（所有材质）
     * @param chunk 区块
     * @param chunkWorldPos 区块的世界坐标
     * @return 生成的网格
     */
    public static Mesh generateSurfaceMesh(Chunk chunk, Vector3f chunkWorldPos) {
        return generateSurfaceMesh(chunk, chunkWorldPos, null);
    }

    /**
     * 生成地表网格（指定材质）
     * @param chunk 区块
     * @param chunkWorldPos 区块的世界坐标
     * @param filterMaterial 只生成该材质的网格，null表示生成所有材质
     * @return 生成的网格
     */
    public static Mesh generateSurfaceMesh(Chunk chunk, Vector3f chunkWorldPos, TerrainMaterial filterMaterial) {
        HeightMap heightMap = chunk.getSurfaceHeightMap();
        TerrainMaterial[][] materials = chunk.getSurfaceMaterials();

        List<Vector3f> vertices = new ArrayList<>();
        List<Vector3f> normals = new ArrayList<>();
        List<Vector2f> uvs = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();

        // 方形大小（每个方形占据的单位数）
        float QUAD_SIZE = 1.0f; // 改回1x1，避免区块重叠
        // ========== 直接使用原始高度：无空隙优先 ==========
        // 允许tile轻微扭曲（四角不严格共面），但保证相邻tile完全共享顶点
        // 这样在chunk边界处也不会有空隙
        // 统计信息
        int totalCells = 0;
        int skippedCells = 0;

        // 每个格子独立生成4个顶点，但使用共享的高度值
        for (int x = 0; x < Chunk.SIZE; x++) {
            for (int z = 0; z < Chunk.SIZE; z++) {
                totalCells++;

                // 检查该格子是否有材质
                TerrainMaterial material = materials[x][z];
                if (material == TerrainMaterial.NONE) {
                    skippedCells++;
                    continue; // 跳过空格子
                }

                // 如果指定了过滤材质，只生成匹配的格子
                if (filterMaterial != null && material != filterMaterial) {
                    continue;
                }

                // 获取格子四个角的量化高度
                // 使用heightMap的原始量化值，确保相邻tile共享顶点（无空隙）
                float h00 = heightMap.getHeight(x, z);
                float h10 = heightMap.getHeight(x + 1, z);
                float h01 = heightMap.getHeight(x, z + 1);
                float h11 = heightMap.getHeight(x + 1, z + 1);

                // 注意：由于高度已量化，quad可能不严格共面，但几何法线计算会让视觉上呈现平面效果

                // 为这个格子生成4个顶点（每个tile独立顶点，保证严格共面）
                int baseIndex = vertices.size();

                // 四个角的世界坐标
                vertices.add(new Vector3f(
                    chunkWorldPos.x + x * QUAD_SIZE,
                    chunkWorldPos.y + h00,
                    chunkWorldPos.z + z * QUAD_SIZE));
                vertices.add(new Vector3f(
                    chunkWorldPos.x + (x + 1) * QUAD_SIZE,
                    chunkWorldPos.y + h10,
                    chunkWorldPos.z + z * QUAD_SIZE));
                vertices.add(new Vector3f(
                    chunkWorldPos.x + x * QUAD_SIZE,
                    chunkWorldPos.y + h01,
                    chunkWorldPos.z + (z + 1) * QUAD_SIZE));
                vertices.add(new Vector3f(
                    chunkWorldPos.x + (x + 1) * QUAD_SIZE,
                    chunkWorldPos.y + h11,
                    chunkWorldPos.z + (z + 1) * QUAD_SIZE));

                // UV坐标
                uvs.add(new Vector2f(x * QUAD_SIZE, z * QUAD_SIZE));
                uvs.add(new Vector2f((x + 1) * QUAD_SIZE, z * QUAD_SIZE));
                uvs.add(new Vector2f(x * QUAD_SIZE, (z + 1) * QUAD_SIZE));
                uvs.add(new Vector2f((x + 1) * QUAD_SIZE, (z + 1) * QUAD_SIZE));

                // 为每个三角形单独计算法线（使用固定对角线方向）
                Vector3f v0 = vertices.get(baseIndex + 0);  // 左上
                Vector3f v1 = vertices.get(baseIndex + 1);  // 右上
                Vector3f v2 = vertices.get(baseIndex + 2);  // 左下
                Vector3f v3 = vertices.get(baseIndex + 3);  // 右下

                // 固定使用v0-v3对角线（左上到右下），确保所有quad一致
                // 第一个三角形 (0, 1, 3)
                Vector3f edge1_t1 = v1.subtract(v0);
                Vector3f edge2_t1 = v3.subtract(v0);
                Vector3f normal_t1 = edge1_t1.cross(edge2_t1);

                if (normal_t1.lengthSquared() < 0.001f) {
                    normal_t1.set(0, 1, 0);
                } else {
                    normal_t1.normalizeLocal();
                    if (normal_t1.y < 0) normal_t1.negateLocal();
                }

                // 第二个三角形 (0, 3, 2)
                Vector3f edge1_t2 = v3.subtract(v0);
                Vector3f edge2_t2 = v2.subtract(v0);
                Vector3f normal_t2 = edge1_t2.cross(edge2_t2);

                if (normal_t2.lengthSquared() < 0.001f) {
                    normal_t2.set(0, 1, 0);
                } else {
                    normal_t2.normalizeLocal();
                    if (normal_t2.y < 0) normal_t2.negateLocal();
                }

                // v0和v3属于两个三角形，v1和v2各属于一个
                Vector3f normal_v0 = normal_t1.add(normal_t2);
                if (normal_v0.lengthSquared() < 0.001f) {
                    normal_v0.set(0, 1, 0);
                } else {
                    normal_v0.normalizeLocal();
                }

                Vector3f normal_v1 = normal_t1.clone();
                Vector3f normal_v2 = normal_t2.clone();

                Vector3f normal_v3 = normal_t1.add(normal_t2);
                if (normal_v3.lengthSquared() < 0.001f) {
                    normal_v3.set(0, 1, 0);
                } else {
                    normal_v3.normalizeLocal();
                }

                normals.add(normal_v0);
                normals.add(normal_v1);
                normals.add(normal_v2);
                normals.add(normal_v3);

                // 【修复】使用逆时针顶点顺序（从上往下看）以正确接收阴影
                // 第一个三角形 (0, 3, 1) - 逆时针：左上 -> 右下 -> 右上
                indices.add(baseIndex + 0);
                indices.add(baseIndex + 3);
                indices.add(baseIndex + 1);

                // 第二个三角形 (0, 2, 3) - 逆时针：左上 -> 左下 -> 右下
                indices.add(baseIndex + 0);
                indices.add(baseIndex + 2);
                indices.add(baseIndex + 3);
            }
        }

        // 调试：输出跳过的格子数量
        if (skippedCells > 0) {
        }

        // 如果没有有效的三角形，返回空网格
        if (indices.isEmpty()) {
            return null;
        }

        // 法线已经在生成quad时计算好了，无需再次计算

        // 创建网格
        Mesh mesh = new Mesh();

        // 转换为缓冲区
        FloatBuffer vertexBuffer = BufferUtils.createFloatBuffer(vertices.size() * 3);
        for (Vector3f v : vertices) {
            vertexBuffer.put(v.x).put(v.y).put(v.z);
        }
        vertexBuffer.flip();

        FloatBuffer normalBuffer = BufferUtils.createFloatBuffer(normals.size() * 3);
        for (Vector3f n : normals) {
            normalBuffer.put(n.x).put(n.y).put(n.z);
        }
        normalBuffer.flip();

        FloatBuffer uvBuffer = BufferUtils.createFloatBuffer(uvs.size() * 2);
        for (Vector2f uv : uvs) {
            uvBuffer.put(uv.x).put(uv.y);
        }
        uvBuffer.flip();

        IntBuffer indexBuffer = BufferUtils.createIntBuffer(indices.size());
        for (Integer i : indices) {
            indexBuffer.put(i);
        }
        indexBuffer.flip();

        // 设置缓冲区
        mesh.setBuffer(VertexBuffer.Type.Position, 3, vertexBuffer);
        mesh.setBuffer(VertexBuffer.Type.Normal, 3, normalBuffer);
        mesh.setBuffer(VertexBuffer.Type.TexCoord, 2, uvBuffer);
        mesh.setBuffer(VertexBuffer.Type.Index, 1, indexBuffer); // 索引是单个整数，不是3个组件
        mesh.updateBound();

        return mesh;
    }

    /**
     * 计算顶点法线（Flat Shading - 每个方形面一个法线）
     */
    private static void calculateNormals(List<Vector3f> vertices, List<Integer> indices, List<Vector3f> normals) {
        // 使用flat shading：每个三角形的所有顶点使用相同的面法线
        // 这样每个方形会有明显的平面感

        for (int i = 0; i < indices.size(); i += 3) {
            int i0 = indices.get(i);
            int i1 = indices.get(i + 1);
            int i2 = indices.get(i + 2);

            Vector3f v0 = vertices.get(i0);
            Vector3f v1 = vertices.get(i1);
            Vector3f v2 = vertices.get(i2);

            // 计算面法线
            Vector3f edge1 = v1.subtract(v0);
            Vector3f edge2 = v2.subtract(v0);
            Vector3f faceNormal = edge1.cross(edge2).normalize();

            // 防止零向量
            if (faceNormal.lengthSquared() < 0.001f) {
                faceNormal.set(0, 1, 0);
            }

            // 直接设置为面法线（不累加，实现flat shading）
            normals.set(i0, faceNormal.clone());
            normals.set(i1, faceNormal.clone());
            normals.set(i2, faceNormal.clone());
        }
    }

    /**
     * 生成边缘填充网格（智能检测开放边缘并生成垂直连接面）
     * 只在没有相邻格子的边缘生成垂直面，避免穿帮
     */
    public static Mesh generateEdgeFillMesh(Chunk chunk, Vector3f chunkWorldPos) {
        HeightMap heightMap = chunk.getSurfaceHeightMap();
        TerrainMaterial[][] materials = chunk.getSurfaceMaterials();

        List<Vector3f> vertices = new ArrayList<>();
        List<Vector3f> normals = new ArrayList<>();
        List<Vector2f> uvs = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();

        float QUAD_SIZE = 1.0f;
        float bottomY = -300.0f; // 垂直面延伸到的底部高度（可以向下挖300格）

        int edgesGenerated = 0;

        // 遍历每个格子，检测开放边缘
        for (int x = 0; x < Chunk.SIZE; x++) {
            for (int z = 0; z < Chunk.SIZE; z++) {
                // 只处理有材质的格子
                TerrainMaterial material = materials[x][z];
                if (material == TerrainMaterial.NONE) {
                    continue;
                }

                // 获取当前格子四个角的高度
                float h00 = heightMap.getHeight(x, z);
                float h10 = heightMap.getHeight(x + 1, z);
                float h01 = heightMap.getHeight(x, z + 1);
                float h11 = heightMap.getHeight(x + 1, z + 1);

                // 检查4个方向的邻居，生成需要的垂直面
                // 左边缘(-X方向, x=0边)
                if (x == 0 || materials[x - 1][z] == TerrainMaterial.NONE) {
                    generateVerticalEdge(vertices, normals, uvs, indices,
                        chunkWorldPos, x, z, h00, h01, bottomY, QUAD_SIZE,
                        new Vector3f(-1, 0, 0)); // 法线朝左
                    edgesGenerated++;
                }

                // 右边缘(+X方向, x+1边)
                if (x == Chunk.SIZE - 1 || materials[x + 1][z] == TerrainMaterial.NONE) {
                    generateVerticalEdge(vertices, normals, uvs, indices,
                        chunkWorldPos, x + 1, z, h10, h11, bottomY, QUAD_SIZE,
                        new Vector3f(1, 0, 0)); // 法线朝右
                    edgesGenerated++;
                }

                // 前边缘(-Z方向, z=0边)
                if (z == 0 || materials[x][z - 1] == TerrainMaterial.NONE) {
                    generateVerticalEdge(vertices, normals, uvs, indices,
                        chunkWorldPos, x, z, h00, h10, bottomY, QUAD_SIZE,
                        new Vector3f(0, 0, -1)); // 法线朝前
                    edgesGenerated++;
                }

                // 后边缘(+Z方向, z+1边)
                if (z == Chunk.SIZE - 1 || materials[x][z + 1] == TerrainMaterial.NONE) {
                    generateVerticalEdge(vertices, normals, uvs, indices,
                        chunkWorldPos, x, z + 1, h01, h11, bottomY, QUAD_SIZE,
                        new Vector3f(0, 0, 1)); // 法线朝后
                    edgesGenerated++;
                }
            }
        }

        // 如果没有需要填充的边缘，返回null
        if (indices.isEmpty()) {
            return null;
        }

        // 创建网格
        Mesh mesh = new Mesh();

        FloatBuffer vertexBuffer = BufferUtils.createFloatBuffer(vertices.size() * 3);
        for (Vector3f v : vertices) {
            vertexBuffer.put(v.x).put(v.y).put(v.z);
        }
        vertexBuffer.flip();

        FloatBuffer normalBuffer = BufferUtils.createFloatBuffer(normals.size() * 3);
        for (Vector3f n : normals) {
            normalBuffer.put(n.x).put(n.y).put(n.z);
        }
        normalBuffer.flip();

        FloatBuffer uvBuffer = BufferUtils.createFloatBuffer(uvs.size() * 2);
        for (Vector2f uv : uvs) {
            uvBuffer.put(uv.x).put(uv.y);
        }
        uvBuffer.flip();

        IntBuffer indexBuffer = BufferUtils.createIntBuffer(indices.size());
        for (Integer i : indices) {
            indexBuffer.put(i);
        }
        indexBuffer.flip();

        mesh.setBuffer(VertexBuffer.Type.Position, 3, vertexBuffer);
        mesh.setBuffer(VertexBuffer.Type.Normal, 3, normalBuffer);
        mesh.setBuffer(VertexBuffer.Type.TexCoord, 2, uvBuffer);
        mesh.setBuffer(VertexBuffer.Type.Index, 1, indexBuffer);

        mesh.updateBound();

        return mesh;
    }

    /**
     * 生成单条垂直边缘（矩形面，2个三角形）
     * 用于填充开放的边缘，防止看到天空
     */
    private static void generateVerticalEdge(
        List<Vector3f> vertices, List<Vector3f> normals,
        List<Vector2f> uvs, List<Integer> indices,
        Vector3f chunkWorldPos, float edgeX, float edgeZ,
        float height1, float height2, float bottomY, float quadSize,
        Vector3f faceNormal) {

        int baseIndex = vertices.size();

        // 垂直矩形的4个顶点
        // 顶部两个顶点使用地表高度，底部两个顶点使用bottomY

        // 根据法线方向确定顶点顺序，确保正面朝外
        if (Math.abs(faceNormal.x) > 0.5f) {
            // X方向的边（前后两个顶点）
            // 顶部
            vertices.add(new Vector3f(
                chunkWorldPos.x + edgeX * quadSize,
                chunkWorldPos.y + height1,
                chunkWorldPos.z + edgeZ * quadSize));
            vertices.add(new Vector3f(
                chunkWorldPos.x + edgeX * quadSize,
                chunkWorldPos.y + height2,
                chunkWorldPos.z + (edgeZ + 1) * quadSize));
            // 底部
            vertices.add(new Vector3f(
                chunkWorldPos.x + edgeX * quadSize,
                bottomY,
                chunkWorldPos.z + edgeZ * quadSize));
            vertices.add(new Vector3f(
                chunkWorldPos.x + edgeX * quadSize,
                bottomY,
                chunkWorldPos.z + (edgeZ + 1) * quadSize));
        } else {
            // Z方向的边（左右两个顶点）
            // 顶部
            vertices.add(new Vector3f(
                chunkWorldPos.x + edgeX * quadSize,
                chunkWorldPos.y + height1,
                chunkWorldPos.z + edgeZ * quadSize));
            vertices.add(new Vector3f(
                chunkWorldPos.x + (edgeX + 1) * quadSize,
                chunkWorldPos.y + height2,
                chunkWorldPos.z + edgeZ * quadSize));
            // 底部
            vertices.add(new Vector3f(
                chunkWorldPos.x + edgeX * quadSize,
                bottomY,
                chunkWorldPos.z + edgeZ * quadSize));
            vertices.add(new Vector3f(
                chunkWorldPos.x + (edgeX + 1) * quadSize,
                bottomY,
                chunkWorldPos.z + edgeZ * quadSize));
        }

        // UV坐标（简单拉伸）
        uvs.add(new Vector2f(0, 1));
        uvs.add(new Vector2f(1, 1));
        uvs.add(new Vector2f(0, 0));
        uvs.add(new Vector2f(1, 0));

        // 所有顶点使用相同的法线
        normals.add(faceNormal.clone());
        normals.add(faceNormal.clone());
        normals.add(faceNormal.clone());
        normals.add(faceNormal.clone());

        // 两个三角形（确保正面朝外）
        // 第一个三角形
        indices.add(baseIndex + 0);
        indices.add(baseIndex + 1);
        indices.add(baseIndex + 2);

        // 第二个三角形
        indices.add(baseIndex + 1);
        indices.add(baseIndex + 3);
        indices.add(baseIndex + 2);
    }

    /**
     * 生成裙边网格（将地表边缘连接到底部）
     * TODO: 后续实现
     */
    public static Mesh generateSkirtMesh(Chunk chunk, Vector3f chunkWorldPos) {
        // 暂未实现，返回null
        return null;
    }
}
