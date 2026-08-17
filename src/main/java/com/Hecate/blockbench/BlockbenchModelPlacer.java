package com.Hecate.blockbench;

import com.Hecate.placer.AbstractModelPlacer;
import com.Hecate.utils.LogUtils;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;

/**
 * Blockbench模型放置器
 */
public class BlockbenchModelPlacer extends AbstractModelPlacer<BlockbenchModel, BlockbenchModelRegistry> {

    public BlockbenchModelPlacer(com.jme3.scene.Node rootNode) {
        super(rootNode, BlockbenchModelRegistry.getInstance());
    }

    @Override
    public Boolean placeModel(String modelId, Vector3f position) {
        BlockbenchModel model = getAndValidateModel(modelId);
        if (model == null) {
            return false;
        }

        try {
            // 克隆模型以避免多次使用同一个实例
            Spatial modelInstance = model.getSpatial().clone();

            // 设置位置
            modelInstance.setLocalTranslation(position);

            // 设置名称以便识别
            modelInstance.setName("blockbench_" + modelId + "_" + position.toString());

            // 添加到场景
            worldNode.attachChild(modelInstance);

            return true;

        } catch (Exception e) {

            return false;
        }
    }

    @Override
    protected boolean isModelLoaded(BlockbenchModel model) {
        return model.isLoaded();
    }

    /**
     * 移除指定位置的模型
     */
    public boolean removeModelAt(Vector3f position) {
        try {
            // 查找并移除指定位置的模型
            String targetName = "blockbench_.*_" + position.toString();

            for (int i = worldNode.getQuantity() - 1; i >= 0; i--) {
                Spatial child = worldNode.getChild(i);
                if (child.getName() != null && child.getName().matches(targetName)) {
                    worldNode.detachChild(child);

                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            LogUtils.error(getClass(), "移除模型失败在位置: " + position, e);
            return false;
        }
    }

    /**
     * 清除所有Blockbench模型
     */
    public void clearAllModels() {
        try {
            for (int i = worldNode.getQuantity() - 1; i >= 0; i--) {
                Spatial child = worldNode.getChild(i);
                if (child.getName() != null && child.getName().startsWith("blockbench_")) {
                    worldNode.detachChild(child);
                }
            }
        } catch (Exception e) {
            LogUtils.error(getClass(), "清除模型失败", e);
        }
    }
}
