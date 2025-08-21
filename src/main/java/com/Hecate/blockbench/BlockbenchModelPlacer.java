package com.Hecate.blockbench;

import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

/**
 * Blockbench模型放置器
 */
public class BlockbenchModelPlacer {
    private final Node rootNode;
    private final BlockbenchModelRegistry registry;

    public BlockbenchModelPlacer(Node rootNode) {
        this.rootNode = rootNode;
        this.registry = BlockbenchModelRegistry.getInstance();
    }

    /**
     * 在指定位置放置Blockbench模型
     */
    public boolean placeModel(String modelId, Vector3f position) {
        BlockbenchModel model = registry.getModel(modelId);

        if (model == null) {
            System.err.println("模型不存在: " + modelId);
            return false;
        }

        if (!model.isLoaded()) {
            System.err.println("模型未加载: " + modelId);
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
            rootNode.attachChild(modelInstance);

            System.out.println("放置Blockbench模型: " + modelId + " 在位置 " + position);
            return true;

        } catch (Exception e) {
            System.err.println("放置模型失败: " + modelId + " - " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 移除指定位置的模型
     */
    public boolean removeModelAt(Vector3f position) {
        try {
            // 查找并移除指定位置的模型
            String targetName = "blockbench_.*_" + position.toString();

            for (int i = rootNode.getQuantity() - 1; i >= 0; i--) {
                Spatial child = rootNode.getChild(i);
                if (child.getName() != null && child.getName().matches(targetName)) {
                    rootNode.detachChild(child);
                    System.out.println("移除Blockbench模型在位置: " + position);
                    return true;
                }
            }

            return false;
        } catch (Exception e) {
            System.err.println("移除模型失败在位置: " + position + " - " + e.getMessage());
            return false;
        }
    }

    /**
     * 清除所有Blockbench模型
     */
    public void clearAllModels() {
        try {
            for (int i = rootNode.getQuantity() - 1; i >= 0; i--) {
                Spatial child = rootNode.getChild(i);
                if (child.getName() != null && child.getName().startsWith("blockbench_")) {
                    rootNode.detachChild(child);
                }
            }
            System.out.println("清除所有Blockbench模型");
        } catch (Exception e) {
            System.err.println("清除模型失败: " + e.getMessage());
        }
    }
}
