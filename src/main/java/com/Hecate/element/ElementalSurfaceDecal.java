package com.Hecate.element;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;

/**
 * 元素地表装饰
 *
 * 射弹落地后生成的视觉半砖，无碰撞体积
 * 提供视觉反馈，让玩家能看到自己的元素在地面上留下的痕迹
 *
 * 特性：
 * - 无碰撞体积（类似雾气）
 * - 有生命周期（30秒后消失）
 * - 最后5秒淡出效果
 * - 不同元素有不同的视觉样式
 */
public class ElementalSurfaceDecal {

    private final ElementType element;
    private final Vector3f position;
    private final Node visualNode;
    private float lifetime;

    // 装饰物存在时间（秒）
    private static final float MAX_LIFETIME = 30.0f;
    private static final float FADE_START_TIME = 5.0f;

    /**
     * 构造函数
     *
     * @param element 元素类型
     * @param position 位置（世界坐标）
     * @param rootNode 根节点（用于附加视觉）
     * @param assetManager 资源管理器
     */
    public ElementalSurfaceDecal(ElementType element, Vector3f position,
                                  Node rootNode, AssetManager assetManager) {
        this.element = element;
        this.position = position.clone();
        this.lifetime = MAX_LIFETIME;

        // 创建视觉节点
        this.visualNode = createVisualNode(assetManager);
        this.visualNode.setLocalTranslation(position);

        // 附加到根节点（无碰撞）
        rootNode.attachChild(visualNode);
    }

    /**
     * 根据元素类型创建视觉节点
     *
     * 尝试加载模型文件，如果失败则使用简单的方块占位
     *
     * @param assetManager 资源管理器
     * @return 视觉节点
     */
    private Node createVisualNode(AssetManager assetManager) {
        Node node = new Node("Decal_" + element.name() + "_" + System.currentTimeMillis());

        Spatial model = null;

        // 尝试加载元素专属模型
        try {
            model = assetManager.loadModel(element.getDecalModelPath());
        } catch (Exception e) {
            // 模型文件不存在，使用简单的占位方块
            model = createPlaceholderDecal(assetManager);
        }

        node.attachChild(model);

        // 半砖尺寸：0.5x0.25x0.5（宽x高x深）
        node.setLocalScale(0.5f, 0.25f, 0.5f);

        return node;
    }

    /**
     * 创建占位装饰（当模型文件不存在时）
     *
     * @param assetManager 资源管理器
     * @return 占位几何体
     */
    private Spatial createPlaceholderDecal(AssetManager assetManager) {
        Box box = new Box(0.5f, 0.125f, 0.5f);
        Geometry geo = new Geometry("PlaceholderDecal", box);

        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", element.getPrimaryColor());

        geo.setMaterial(mat);
        return geo;
    }

    /**
     * 更新装饰物（处理生命周期和淡出）
     *
     * @param tpf 时间增量（秒）
     * @return true 如果应该被移除
     */
    public boolean update(float tpf) {
        lifetime -= tpf;

        // 最后5秒淡出效果
        if (lifetime < FADE_START_TIME && lifetime > 0) {
            float alpha = lifetime / FADE_START_TIME;

            // 缩放淡出
            float scale = 0.5f + (alpha * 0.5f); // 从1.0缩放到0.5
            visualNode.setLocalScale(scale, 0.25f * alpha, scale);

            // 透明度淡出（如果材质支持）
            applyAlpha(visualNode, alpha);
        }

        // 生命周期结束
        if (lifetime <= 0) {
            if (visualNode.getParent() != null) {
                visualNode.removeFromParent();
            }
            return true;
        }

        return false;
    }

    /**
     * 应用透明度到节点的所有材质
     *
     * @param node 节点
     * @param alpha 透明度 (0.0 - 1.0)
     */
    private void applyAlpha(Node node, float alpha) {
        // 递归处理所有子节点
        for (Spatial child : node.getChildren()) {
            if (child instanceof Node) {
                applyAlpha((Node) child, alpha);
            } else if (child instanceof Geometry) {
                Geometry geo = (Geometry) child;
                Material mat = geo.getMaterial();
                if (mat != null) {
                    try {
                        ColorRGBA color = (ColorRGBA) mat.getParam("Color").getValue();
                        if (color != null) {
                            color.a = alpha;
                            mat.setColor("Color", color);
                        }
                    } catch (Exception e) {
                        // 材质不支持颜色参数，忽略
                    }
                }
            }
        }
    }

    /**
     * 强制移除装饰物
     */
    public void remove() {
        if (visualNode != null && visualNode.getParent() != null) {
            visualNode.removeFromParent();
        }
    }

    // Getters

    public ElementType getElement() {
        return element;
    }

    public Vector3f getPosition() {
        return position;
    }

    public float getLifetime() {
        return lifetime;
    }

    public Node getVisualNode() {
        return visualNode;
    }

    /**
     * 判断是否正在淡出
     * @return true 如果正在淡出
     */
    public boolean isFading() {
        return lifetime < FADE_START_TIME;
    }
}
