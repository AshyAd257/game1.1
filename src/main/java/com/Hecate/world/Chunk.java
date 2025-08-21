package com.Hecate.world;

import com.Hecate.block.Block;
import com.Hecate.block.BlockRegistry;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import java.util.Arrays;

/**
 * 表示世界中的一个16x16x16方块区块
 * 🎯 修改为只在Y=1生成土方块
 */
public class Chunk {
    public static final int SIZE = 16; // 区块大小

    private final ChunkPosition position;
    private final String[][][] blockIds; // 存储方块ID
    private Node chunkNode; // 区块的3D表示
    private boolean dirty; // 标记是否需要重新渲染

    /**
     * 创建一个新区块
     *
     * @param position 区块在世界中的位置
     */
    public Chunk(ChunkPosition position) {
        this.position = position;
        this.blockIds = new String[SIZE][SIZE][SIZE];
        this.dirty = true;

        // 默认填充为空气方块
        for (String[][] xz : blockIds) {
            for (String[] z : xz) {
                Arrays.fill(z, "air");
            }
        }
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
            // 只有方块变化时才标记为dirty
            if (!blockIds[x][y][z].equals(blockId)) {
                blockIds[x][y][z] = blockId;
                dirty = true;
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
     * 🎯 填充整个区块 - 只在Y=1生成一层土方块
     */
    public void fillWithTestPattern() {
        // 根据区块的Y位置决定填充内容
        int chunkY = position.getY();

        if (chunkY == 0) {
            // 地表区块 - 只在Y=1填充一层土方块
            System.out.println("🌍 在区块 " + position + " 的Y=1层生成土方块");
            for (int x = 0; x < SIZE; x++) {
                for (int z = 0; z < SIZE; z++) {
                    blockIds[x][1][z] = "dirt"; // 只在Y=1放置土方块
                }
            }
        }
        // 其他Y层的区块保持空气

        dirty = true;
    }
}
