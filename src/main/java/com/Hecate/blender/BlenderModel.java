package com.Hecate.blender;

import com.Hecate.model.AbstractModel;
import com.Hecate.utils.LogUtils;
import com.jme3.animation.AnimChannel;
import com.jme3.animation.AnimControl;
import com.jme3.animation.LoopMode;
import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.texture.Texture;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 单个Blender模型的封装类
 * 支持模型、纹理、材质和动画的完整管理
 */
public class BlenderModel extends AbstractModel {
    private Map<String, String> texturePaths;
    private List<String> animationNames;
    private String materialType = "Common/MatDefs/Light/Lighting.j3md"; // 默认材质

    public BlenderModel(String modelId, String modelPath,
                        Map<String, String> texturePaths,
                        List<String> animationNames) {
        super(modelId, modelPath);
        this.texturePaths = texturePaths != null ? texturePaths : new HashMap<>();
        this.animationNames = animationNames != null ? animationNames : new ArrayList<>();
    }

    /**
     * 构造函数（只有模型路径）
     */
    public BlenderModel(String modelId, String modelPath) {
        this(modelId, modelPath, null, null);
    }

    @Override
    public String getTypeName() {
        return "Blender";
    }

    /**
     * 设置材质类型
     */
    public void setMaterialType(String materialType) {
        this.materialType = materialType;
    }

    /**
     * 添加纹理路径
     */
    public void addTexture(String textureType, String texturePath) {
        this.texturePaths.put(textureType, texturePath);
    }

    /**
     * 添加动画
     */
    public void addAnimation(String animationName) {
        if (!this.animationNames.contains(animationName)) {
            this.animationNames.add(animationName);
        }
    }

    /**
     * 加载模型
     */
    public boolean load(AssetManager assetManager) {
        try {
            // 加载基础模型
            spatial = assetManager.loadModel(modelPath);
            if (spatial == null) {
                LogUtils.error(getClass(), "无法加载模型文件: " + modelPath, null);
                return false;
            }

            // 应用纹理和材质
            if (texturePaths != null && !texturePaths.isEmpty()) {
                applyTextures(assetManager, spatial);
            }

            // 验证动画
            if (animationNames != null && !animationNames.isEmpty()) {
                validateAnimations();
            }

            loaded = true;

            return true;

        } catch (Exception e) {

            return false;
        }
    }

    /**
     * 应用纹理到模型
     */
    private void applyTextures(AssetManager assetManager, Spatial spatial) {
        if (spatial instanceof Geometry) {
            applyTexturesToGeometry(assetManager, (Geometry) spatial);
        } else if (spatial instanceof Node) {
            Node node = (Node) spatial;
            for (Spatial child : node.getChildren()) {
                applyTextures(assetManager, child);
            }
        }
    }

    /**
     * 应用纹理到几何体
     */
    private void applyTexturesToGeometry(AssetManager assetManager, Geometry geometry) {
        Material material = geometry.getMaterial();

        // 如果没有材质，创建新的
        if (material == null) {
            material = new Material(assetManager, materialType);
            geometry.setMaterial(material);
        }

        // 应用所有纹理
        for (Map.Entry<String, String> entry : texturePaths.entrySet()) {
            String textureType = entry.getKey();
            String texturePath = entry.getValue();

            try {
                Texture texture = assetManager.loadTexture(texturePath);
                if (texture != null) {
                    material.setTexture(textureType, texture);

                }
            } catch (Exception e) {

            }
        }

        // 设置材质渲染状态
        material.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
    }

    /**
     * 验证动画
     */
    private void validateAnimations() {
        if (spatial == null) {
            return;
        }

        AnimControl animControl = findAnimControl(spatial);
        if (animControl == null) {
            LogUtils.warning(getClass(), "模型没有动画控制器: " + id);
            return;
        }

        // 验证所有声明的动画是否存在
        for (String animName : animationNames) {
            if (animControl.getAnim(animName) == null) {
                LogUtils.warning(getClass(), "动画不存在: " + animName + " (模型: " + id + ")");
            }
        }
    }

    /**
     * 查找动画控制器
     */
    private AnimControl findAnimControl(Spatial spatial) {
        AnimControl control = spatial.getControl(AnimControl.class);
        if (control != null) {
            return control;
        }

        if (spatial instanceof Node) {
            Node node = (Node) spatial;
            for (Spatial child : node.getChildren()) {
                control = findAnimControl(child);
                if (control != null) {
                    return control;
                }
            }
        }

        return null;
    }

    /**
     * 创建模型实例
     */
    public Node createInstance() {
        if (!loaded || spatial == null) {
            LogUtils.error(getClass(), "模型未加载，无法创建实例: " + id, null);
            return null;
        }

        Node instance = new Node(id + "_instance");
        Spatial clonedModel = spatial.clone();
        instance.attachChild(clonedModel);

        return instance;
    }

    /**
     * 播放动画
     */
    public void playAnimation(Node modelInstance, String animationName) {
        if (modelInstance == null || animationNames == null || !animationNames.contains(animationName)) {
            return;
        }

        Spatial model = modelInstance.getChild(0);
        if (model != null) {
            AnimControl animControl = model.getControl(AnimControl.class);
            if (animControl != null) {
                AnimChannel channel = animControl.createChannel();
                channel.setAnim(animationName);
                channel.setLoopMode(LoopMode.Loop);
            }
        }
    }

    // Getters
    public Spatial getLoadedModel() { return spatial; }
    public List<String> getAnimationNames() { return animationNames; }
}
