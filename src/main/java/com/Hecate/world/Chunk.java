package com.Hecate.world;

// import com.Hecate.block.Block;
// import com.Hecate.block.BlockRegistry;
// import com.Hecate.utils.BlockUtils;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import java.util.Arrays;

/**
 * 表示世界中的一个16x16x16方块区块
 */
public class Chunk {
    public static final int SIZE = 16;

    private final ChunkPosition position;
    private final String[][][] blockIds;
    private Node chunkNode;
    private boolean dirty;
    private boolean modified; // 标记区块是否被玩家修改过（用于持久化）

    // 地形系统：用于土/沙/水的自然起伏
    private HeightMap surfaceHeightMap; // 地表高度场 (17×17 顶点)
    private TerrainMaterial[][] surfaceMaterials; // 地表材质 (16×16 格子)

    /**
     * 创建一个新区块
     *
     * @param position 区块在世界中的位置
     */
    public Chunk(ChunkPosition position) {
        this.position = position;
        this.blockIds = new String[SIZE][SIZE][SIZE];
        this.dirty = true;
        this.modified = false;

        // 默认填充为空气方块
        for (String[][] xz : blockIds) {
            for (String[] z : xz) {
                Arrays.fill(z, "air");
            }
        }

        // 初始化地形系统
        this.surfaceHeightMap = new HeightMap();
        this.surfaceMaterials = new TerrainMaterial[SIZE][SIZE];
        for (int x = 0; x < SIZE; x++) {
            for (int z = 0; z < SIZE; z++) {
                surfaceMaterials[x][z] = TerrainMaterial.NONE;
            }
        }
    }

    /**
     * 创建区块（用于从文件加载）
     */
    public Chunk(ChunkPosition position, String[][][] blockData) {
        this.position = position;
        this.blockIds = blockData;
        this.dirty = true;
        this.modified = false; // 从文件加载的区块初始不算修改

        // 初始化地形系统（将在反序列化时设置）
        this.surfaceHeightMap = new HeightMap();
        this.surfaceMaterials = new TerrainMaterial[SIZE][SIZE];
        for (int x = 0; x < SIZE; x++) {
            for (int z = 0; z < SIZE; z++) {
                surfaceMaterials[x][z] = TerrainMaterial.NONE;
            }
        }
    }

    /**
     * 创建区块（用于从文件加载，包含地形数据）
     */
    public Chunk(ChunkPosition position, String[][][] blockData,
                 HeightMap heightMap, TerrainMaterial[][] materials) {
        this.position = position;
        this.blockIds = blockData;
        this.dirty = true;
        this.modified = false;
        this.surfaceHeightMap = heightMap;
        this.surfaceMaterials = materials;
    }

    /**
     * 获取区块位置
     */
    public ChunkPosition getPosition() {
        return position;
    }

    /**
     * 获取指定位置的方块ID
     *
     * @param x 区块内X坐标 (0-15)
     * @param y 区块内Y坐标 (0-15)
     * @param z 区块内Z坐标 (0-15)
     * @return 方块ID
     */
    public String getBlockId(int x, int y, int z) {
        if (isValidPosition(x, y, z)) {
            return blockIds[x][y][z];
        }
        return "air";
    }

    /**
     * 设置指定位置的方块
     *
     * @param x       区块内X坐标 (0-15)
     * @param y       区块内Y坐标 (0-15)
     * @param z       区块内Z坐标 (0-15)
     * @param blockId 方块ID
     * @return 是否成功设置
     */
    public boolean setBlock(int x, int y, int z, String blockId) {
        if (isValidPosition(x, y, z)) {
            // 只有方块变化时才标记为dirty和modified
            if (!blockIds[x][y][z].equals(blockId)) {
                blockIds[x][y][z] = blockId;
                dirty = true;
                modified = true; // 标记为已修改，需要保存
                return true;
            }
        }
        return false;
    }

    /**
     * 检查坐标是否在区块内
     */
    private boolean isValidPosition(int x, int y, int z) {
        return x >= 0 && x < SIZE && y >= 0 && y < SIZE && z >= 0 && z < SIZE;
    }

    /**
     * 获取区块的3D渲染节点
     */
    public Node getChunkNode() {
        return chunkNode;
    }

    /**
     * 设置区块的3D渲染节点
     */
    public void setChunkNode(Node chunkNode) {
        this.chunkNode = chunkNode;
    }

    /**
     * 检查区块是否需要重新渲染
     */
    public boolean isDirty() {
        return dirty;
    }

    /**
     * 标记区块已经被渲染
     */
    public void setClean() {
        this.dirty = false;
    }

    /**
     * 标记区块需要重新渲染
     */
    public void setDirty() {
        this.dirty = true;
    }

    /**
     * 获取区块在世界中的实际位置（方块坐标）
     */
    public Vector3f getWorldPosition() {
        return new Vector3f(
                position.getX() * SIZE,
                position.getY() * SIZE,
                position.getZ() * SIZE
        );
    }

    /**
     * 🎯 填充整个区块 - 使用高度场系统生成地表
     */
    public void fillWithTestPattern() {
        // 根据区块的Y位置决定填充内容
        int chunkY = position.getY();

        if (chunkY == 0) {
            // 地表区块 - 使用高度场系统生成噪声地形

            // 使用Perlin噪声生成起伏地形
            for (int x = 0; x < HeightMap.VERTEX_COUNT; x++) {
                for (int z = 0; z < HeightMap.VERTEX_COUNT; z++) {
                    // 世界坐标
                    float worldX = position.getX() * SIZE + x;
                    float worldZ = position.getZ() * SIZE + z;

                    // 使用多层噪声生成自然起伏
                    float height = generateTerrainHeight(worldX, worldZ);
                    surfaceHeightMap.setHeight(x, z, height);
                }
            }

            // 设置所有格子材质为土
            for (int x = 0; x < SIZE; x++) {
                for (int z = 0; z < SIZE; z++) {
                    surfaceMaterials[x][z] = TerrainMaterial.DIRT;
                }
            }

            // 在Y=0生成碰撞层（不渲染）
            for (int x = 0; x < SIZE; x++) {
                for (int z = 0; z < SIZE; z++) {
                    blockIds[x][0][z] = "dirt"; // 临时硬编码，避免依赖 BlockUtils
                }
            }
        }
        // 其他Y层的区块保持空气

        dirty = true;
        // 注意：fillWithTestPattern不标记为modified，因为这是自动生成的
    }

    /**
     * 检查区块是否被修改过（需要保存）
     */
    public boolean isModified() {
        return modified;
    }

    /**
     * 重置modified标记（保存后调用）
     */
    public void setUnmodified() {
        this.modified = false;
    }

    /**
     * 获取方块数据（用于序列化）
     */
    public String[][][] getBlockData() {
        return blockIds;
    }

    // ========== 地形系统方法 ==========

    /**
     * 获取地表高度场
     */
    public HeightMap getSurfaceHeightMap() {
        return surfaceHeightMap;
    }

    /**
     * 设置地表高度场（用于反序列化）
     */
    public void setSurfaceHeightMap(HeightMap heightMap) {
        this.surfaceHeightMap = heightMap;
        this.dirty = true;
        this.modified = true;
    }

    /**
     * 获取地表材质
     */
    public TerrainMaterial[][] getSurfaceMaterials() {
        return surfaceMaterials;
    }

    /**
     * 获取指定格子的地表材质
     */
    public TerrainMaterial getSurfaceMaterial(int x, int z) {
        if (x >= 0 && x < SIZE && z >= 0 && z < SIZE) {
            return surfaceMaterials[x][z];
        }
        return TerrainMaterial.NONE;
    }

    /**
     * 设置指定格子的地表材质
     */
    public void setSurfaceMaterial(int x, int z, TerrainMaterial material) {
        if (x >= 0 && x < SIZE && z >= 0 && z < SIZE) {
            surfaceMaterials[x][z] = material;
            this.dirty = true;
            this.modified = true;
        }
    }

    /**
     * 检查是否有地形数据（是否使用高度场系统）
     */
    public boolean hasTerrainData() {
        for (int x = 0; x < SIZE; x++) {
            for (int z = 0; z < SIZE; z++) {
                if (surfaceMaterials[x][z] != TerrainMaterial.NONE) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 生成地形高度（使用Perlin噪声）
     */
    private float generateTerrainHeight(float worldX, float worldZ) {
        // 使用简单的正弦波模拟噪声（增大起伏）
        float scale1 = 0.03f;  // 大范围起伏
        float scale2 = 0.1f;   // 中等起伏
        float scale3 = 0.2f;   // 细节起伏

        float height = 0.0f;

        // 大范围起伏（增大振幅）
        height += Math.sin(worldX * scale1) * Math.cos(worldZ * scale1) * 2.0f;

        // 中等起伏（增大振幅）
        height += Math.sin(worldX * scale2 + 100) * Math.cos(worldZ * scale2 + 100) * 1.0f;

        // 细节起伏（增大振幅）
        height += Math.sin(worldX * scale3 + 50) * Math.cos(worldZ * scale3 + 50) * 0.5f;

        // 基准高度 + 噪声起伏
        float rawHeight = 1.0f + height;

        // 量化高度：四舍五入到最近的0.5，创造明显的低多边形台阶效果
        float quantizationStep = 0.5f;
        return Math.round(rawHeight / quantizationStep) * quantizationStep;
    }
}
