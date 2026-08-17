package com.Hecate.block;

import com.jme3.math.Vector3f;
import com.Hecate.world.ChunkManager;
import com.Hecate.utils.BlockUtils;

/**
 * 方块破坏系统 - 处理方块破坏的进度、效果和掉落物
 */
public class BlockBreaking {
    private final ChunkManager chunkManager;
    private final BlockRegistry blockRegistry;

    // 破坏进度跟踪
    private Vector3f currentBreakingBlock = null;
    private float breakingProgress = 0.0f;
    private float breakingTime = 0.0f;

    /**
     * 构造函数（依赖注入）
     *
     * @param chunkManager 区块管理器
     * @param blockRegistry 方块注册表（通过依赖注入传入）
     */
    public BlockBreaking(ChunkManager chunkManager, BlockRegistry blockRegistry) {
        this.chunkManager = chunkManager;
        this.blockRegistry = blockRegistry;
    }

    /**
     * 构造函数（向后兼容）
     *
     * @param chunkManager 区块管理器
     * @deprecated 推荐使用 {@link #BlockBreaking(ChunkManager, BlockRegistry)} 进行依赖注入
     */
    @Deprecated
    public BlockBreaking(ChunkManager chunkManager) {
        this.chunkManager = chunkManager;
        this.blockRegistry = BlockRegistry.getInstance();
    }

    /**
     * 开始破坏方块
     */
    public void startBreaking(Vector3f blockPosition) {
        if (!blockPosition.equals(currentBreakingBlock)) {
            currentBreakingBlock = blockPosition.clone();
            breakingProgress = 0.0f;
            breakingTime = 0.0f;
        }
    }

    /**
     * 停止破坏方块
     */
    public void stopBreaking() {
        if (currentBreakingBlock != null) {
            currentBreakingBlock = null;
            breakingProgress = 0.0f;
            breakingTime = 0.0f;
        }
    }

    /**
     * 更新破坏进度
     */
    public void updateBreaking(float tpf) {
        if (currentBreakingBlock == null) {
            return;
        }

        // 使用工具类获取方块信息
        String blockId = BlockUtils.getBlockAt(currentBreakingBlock, chunkManager);
        if ("air".equals(blockId)) {
            stopBreaking();
            return;
        }

        Block block = blockRegistry.getBlock(blockId);
        if (block == null) {
            stopBreaking();
            return;
        }

        // 计算破坏时间（基于方块硬度）
        float hardness = getBlockHardness(block);
        float requiredTime = hardness * 1.5f;

        // 更新进度
        breakingTime += tpf;
        breakingProgress = Math.min(breakingTime / requiredTime, 1.0f);

        // 检查是否完成破坏
        if (breakingProgress >= 1.0f) {
            completeBreaking();
        }
    }

    /**
     * 完成方块破坏
     */
    private void completeBreaking() {
        if (currentBreakingBlock == null) {
            return;
        }

        String blockId = BlockUtils.getBlockAt(currentBreakingBlock, chunkManager);
        Block block = blockRegistry.getBlock(blockId);

        if (block != null) {
            // 使用工具类设置方块
            BlockUtils.setBlockAt(currentBreakingBlock, "air", chunkManager);
            createBlockDrop(currentBreakingBlock, block);
        }
        stopBreaking();
    }

    /**
     * 获取方块硬度
     */
    private float getBlockHardness(Block block) {
        // 直接使用Block类中的hardness属性
        return block.getHardness();
    }

    /**
     * 创建方块掉落物
     */
    private void createBlockDrop(Vector3f position, Block block) {
        // 这里将来会实现掉落物系统
        // 目前只是占位
    }

    // Getter方法
    public Vector3f getCurrentBreakingBlock() { return currentBreakingBlock; }
    public float getBreakingProgress() { return breakingProgress; }
    public boolean isBreaking() { return currentBreakingBlock != null; }
}

