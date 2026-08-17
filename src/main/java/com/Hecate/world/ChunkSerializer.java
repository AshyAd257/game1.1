package com.Hecate.world;

// import com.Hecate.utils.LogUtils;
import java.io.*;
import java.nio.file.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * 区块序列化器 - 负责区块的保存和加载
 * 使用简单的二进制格式 + Gzip压缩
 */
public class ChunkSerializer {

    // 文件格式版本
    private static final int FILE_VERSION_1 = 1; // 原始版本（只有方块数据）
    private static final int FILE_VERSION_2 = 2; // 添加地形系统（高度场+材质）
    private static final int CURRENT_VERSION = FILE_VERSION_2;

    private final Path worldSaveDirectory;

    public ChunkSerializer(String worldName) {
        // 保存目录：当前目录/saves/[世界名]/chunks/
        this.worldSaveDirectory = Paths.get("saves", worldName, "chunks");

        // 创建保存目录
        try {
            Files.createDirectories(worldSaveDirectory);
        } catch (IOException e) {
            // LogUtils.error(ChunkSerializer.class, "创建保存目录失败", e);
            e.printStackTrace();
        }
    }

    /**
     * 保存区块到文件
     */
    public void saveChunk(Chunk chunk) {
        if (!chunk.isModified()) {
            return; // 未修改的区块不需要保存
        }

        ChunkPosition pos = chunk.getPosition();
        Path chunkFile = getChunkFilePath(pos);

        try {
            // 创建父目录
            Files.createDirectories(chunkFile.getParent());

            // 使用Gzip压缩流写入
            try (DataOutputStream out = new DataOutputStream(
                    new GZIPOutputStream(
                            new BufferedOutputStream(
                                    Files.newOutputStream(chunkFile))))) {

                // 写入文件版本号
                out.writeInt(CURRENT_VERSION);

                // 写入区块位置信息
                out.writeInt(pos.getX());
                out.writeInt(pos.getY());
                out.writeInt(pos.getZ());

                // 写入方块数据
                String[][][] blockData = chunk.getBlockData();
                for (int x = 0; x < Chunk.SIZE; x++) {
                    for (int y = 0; y < Chunk.SIZE; y++) {
                        for (int z = 0; z < Chunk.SIZE; z++) {
                            String blockId = blockData[x][y][z];
                            // 使用UTF编码写入方块ID
                            out.writeUTF(blockId != null ? blockId : "air");
                        }
                    }
                }

                // 写入地形数据（版本2新增）
                HeightMap heightMap = chunk.getSurfaceHeightMap();
                float[][] heights = heightMap.getHeightData();
                for (int x = 0; x < HeightMap.VERTEX_COUNT; x++) {
                    for (int z = 0; z < HeightMap.VERTEX_COUNT; z++) {
                        out.writeFloat(heights[x][z]);
                    }
                }

                // 写入地表材质
                TerrainMaterial[][] materials = chunk.getSurfaceMaterials();
                for (int x = 0; x < Chunk.SIZE; x++) {
                    for (int z = 0; z < Chunk.SIZE; z++) {
                        out.writeUTF(materials[x][z].name());
                    }
                }
            }

            chunk.setUnmodified(); // 保存后重置modified标记
            // LogUtils.debug(ChunkSerializer.class, "区块已保存: " + pos);

        } catch (IOException e) {
            // LogUtils.error(ChunkSerializer.class, "保存区块失败: " + pos, e);
            e.printStackTrace();
        }
    }

    /**
     * 从文件加载区块
     * @return 加载的区块，如果文件不存在返回null
     */
    public Chunk loadChunk(ChunkPosition pos) {
        Path chunkFile = getChunkFilePath(pos);

        // 检查文件是否存在
        if (!Files.exists(chunkFile)) {
            return null; // 文件不存在，返回null让系统生成新区块
        }

        try {
            // 使用Gzip解压缩流读取
            try (DataInputStream in = new DataInputStream(
                    new GZIPInputStream(
                            new BufferedInputStream(
                                    Files.newInputStream(chunkFile))))) {

                // 读取文件版本号
                int version = FILE_VERSION_1; // 默认版本1（向后兼容）
                in.mark(4);
                try {
                    int firstInt = in.readInt();
                    // 如果第一个int是合理的版本号，使用它
                    if (firstInt >= FILE_VERSION_1 && firstInt <= CURRENT_VERSION) {
                        version = firstInt;
                    } else {
                        // 否则这是旧格式，第一个int是X坐标，回退
                        in.reset();
                    }
                } catch (IOException e) {
                    in.reset();
                }

                // 读取区块位置信息（用于验证）
                int x = in.readInt();
                int y = in.readInt();
                int z = in.readInt();

                // 验证位置是否匹配
                if (x != pos.getX() || y != pos.getY() || z != pos.getZ()) {
                    // LogUtils.warning(ChunkSerializer.class,
                    //     "区块位置不匹配，文件可能损坏: " + pos);
                    return null;
                }

                // 读取方块数据
                String[][][] blockData = new String[Chunk.SIZE][Chunk.SIZE][Chunk.SIZE];
                for (int bx = 0; bx < Chunk.SIZE; bx++) {
                    for (int by = 0; by < Chunk.SIZE; by++) {
                        for (int bz = 0; bz < Chunk.SIZE; bz++) {
                            blockData[bx][by][bz] = in.readUTF();
                        }
                    }
                }

                // 读取地形数据（版本2）
                HeightMap heightMap = null;
                TerrainMaterial[][] materials = null;

                if (version >= FILE_VERSION_2) {
                    // 读取高度场
                    float[][] heights = new float[HeightMap.VERTEX_COUNT][HeightMap.VERTEX_COUNT];
                    for (int hx = 0; hx < HeightMap.VERTEX_COUNT; hx++) {
                        for (int hz = 0; hz < HeightMap.VERTEX_COUNT; hz++) {
                            heights[hx][hz] = in.readFloat();
                        }
                    }
                    heightMap = new HeightMap(heights);

                    // 读取地表材质
                    materials = new TerrainMaterial[Chunk.SIZE][Chunk.SIZE];
                    for (int mx = 0; mx < Chunk.SIZE; mx++) {
                        for (int mz = 0; mz < Chunk.SIZE; mz++) {
                            String materialName = in.readUTF();
                            materials[mx][mz] = TerrainMaterial.valueOf(materialName);
                        }
                    }
                }

                // LogUtils.debug(ChunkSerializer.class, "区块已加载 (版本" + version + "): " + pos);

                // 根据是否有地形数据选择构造函数
                if (heightMap != null && materials != null) {
                    return new Chunk(pos, blockData, heightMap, materials);
                } else {
                    return new Chunk(pos, blockData);
                }
            }

        } catch (IOException e) {
            // LogUtils.error(ChunkSerializer.class, "加载区块失败: " + pos, e);
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 获取区块文件路径
     * 格式: saves/[世界名]/chunks/[chunkX]/[chunkZ]/chunk_[chunkX]_[chunkY]_[chunkZ].dat
     */
    private Path getChunkFilePath(ChunkPosition pos) {
        String filename = String.format("chunk_%d_%d_%d.dat",
            pos.getX(), pos.getY(), pos.getZ());

        // 使用chunkX和chunkZ创建子目录，减少单个目录下的文件数量
        return worldSaveDirectory
                .resolve(String.valueOf(pos.getX()))
                .resolve(String.valueOf(pos.getZ()))
                .resolve(filename);
    }

    /**
     * 删除区块文件（如果需要）
     */
    public void deleteChunk(ChunkPosition pos) {
        Path chunkFile = getChunkFilePath(pos);
        try {
            Files.deleteIfExists(chunkFile);
            // LogUtils.debug(ChunkSerializer.class, "区块文件已删除: " + pos);
        } catch (IOException e) {
            // LogUtils.error(ChunkSerializer.class, "删除区块文件失败: " + pos, e);
            e.printStackTrace();
        }
    }

    /**
     * 检查区块是否已保存
     */
    public boolean chunkExists(ChunkPosition pos) {
        return Files.exists(getChunkFilePath(pos));
    }
}
