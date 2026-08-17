package com.Hecate.blender;

import com.Hecate.placer.AbstractModelPlacer;
import com.Hecate.utils.LogUtils;
import com.jme3.math.Vector3f;
import com.jme3.math.Quaternion;
import com.jme3.scene.Node;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Blender模型放置工具
 * 用于在世界中放置和管理Blender模型实例
 */
public class BlenderModulePlacer extends AbstractModelPlacer<BlenderModel, BlenderModelRegistry> {
    private final Map<String, Node> placedModels; // 实例ID -> 模型节点

    public BlenderModulePlacer(Node worldNode) {
        super(worldNode, BlenderModelRegistry.getInstance());
        this.placedModels = new HashMap<>();
    }

    @Override
    public Node placeModel(String modelId, Vector3f position) {
        return placeModel(modelId, position, 1.0f);
    }

    @Override
    protected boolean isModelLoaded(BlenderModel model) {
        return model.isLoaded();
    }

    /**
     * 在指定位置放置模型
     * @param modelId 模型ID
     * @param position 世界位置
     * @param scale 缩放比例
     * @return 放置的模型节点，如果失败返回null
     */
    public Node placeModel(String modelId, Vector3f position, float scale) {
        BlenderModel model = getAndValidateModel(modelId);
        if (model == null) {
            return null;
        }

        Node modelInstance = model.createInstance();
        if (modelInstance != null) {
            // 生成唯一实例ID
            String instanceId = modelId + "_" + UUID.randomUUID().toString().substring(0, 8);
            modelInstance.setName(instanceId);

            // 设置位置和缩放
            modelInstance.setLocalTranslation(position);
            modelInstance.setLocalScale(scale);

            // 添加到世界
            worldNode.attachChild(modelInstance);
            placedModels.put(instanceId, modelInstance);

            return modelInstance;
        }

        return null;
    }

    /**
     * 在指定位置放置模型并设置旋转
     */
    public Node placeModel(String modelId, Vector3f position, float scale, Quaternion rotation) {
        Node modelNode = placeModel(modelId, position, scale);
        if (modelNode != null && rotation != null) {
            modelNode.setLocalRotation(rotation);
        }
        return modelNode;
    }

    /**
     * 在指定位置放置模型并设置Y轴旋转角度
     */
    public Node placeModel(String modelId, Vector3f position, float scale, float yRotationDegrees) {
        Quaternion rotation = new Quaternion();
        rotation.fromAngleAxis((float) Math.toRadians(yRotationDegrees), Vector3f.UNIT_Y);
        return placeModel(modelId, position, scale, rotation);
    }

    /**
     * 移除模型实例
     */
    public void removeModel(Node modelNode) {
        if (modelNode != null) {
            worldNode.detachChild(modelNode);
            placedModels.remove(modelNode.getName());
        }
    }

    /**
     * 根据实例ID移除模型
     */
    public void removeModelById(String instanceId) {
        Node modelNode = placedModels.get(instanceId);
        if (modelNode != null) {
            removeModel(modelNode);
        }
    }

    /**
     * 移除指定模型类型的所有实例
     */
    public void removeAllModelsOfType(String modelId) {
        placedModels.entrySet().removeIf(entry -> {
            if (entry.getKey().startsWith(modelId + "_")) {
                worldNode.detachChild(entry.getValue());
                return true;
            }
            return false;
        });
    }

    /**
     * 获取已放置的模型数量
     */
    public int getPlacedModelCount() {
        return placedModels.size();
    }

    /**
     * 批量放置模型 - 在区域内随机放置
     */
    public void placeModelsInArea(String modelId, Vector3f centerPosition, float radius,
                                  int count, float minScale, float maxScale) {
        for (int i = 0; i < count; i++) {
            // 在圆形区域内生成随机位置
            float angle = (float) (Math.random() * 2 * Math.PI);
            float distance = (float) (Math.random() * radius);

            float x = centerPosition.x + distance * (float) Math.cos(angle);
            float z = centerPosition.z + distance * (float) Math.sin(angle);
            float y = centerPosition.y; // 保持相同高度

            Vector3f position = new Vector3f(x, y, z);
            float scale = minScale + (float) Math.random() * (maxScale - minScale);
            float rotation = (float) (Math.random() * 360); // 随机旋转

            placeModel(modelId, position, scale, rotation);
        }
    }
}
