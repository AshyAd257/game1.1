package com.Hecate.puppet.ecs.component.render;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Texture;

import java.util.HashMap;
import java.util.Map;

/**
 * 6方向精灵渲染策略
 * 根据相机方向自动选择合适的精灵纹理
 *
 * 支持6个方向：
 * - front (前)
 * - back (后)
 * - left (左)
 * - right (右)
 * - up (上)
 * - down (下)
 *
 * 【使用场景】
 * - 2D角色在3D世界中
 * - 降低渲染开销（相比3D模型）
 * - 保持像素艺术风格
 */
public class SpriteRenderStrategy implements RenderStrategy {

    // 6个方向的纹理路径
    private final Map<Direction, String> texturePaths;

    // 加载的纹理
    private final Map<Direction, Texture> textures;

    // 渲染几何体
    private Geometry geometry;
    private Quad quad;
    private Material material;

    // 父节点
    private Node parentNode;

    // 精灵尺寸
    private float width;
    private float height;

    // 当前方向
    private Direction currentDirection;

    // 可见性
    private boolean visible;

    // AssetManager（需要外部传入）
    private AssetManager assetManager;

    /**
     * 方向枚举
     */
    public enum Direction {
        FRONT, BACK, LEFT, RIGHT, UP, DOWN
    }

    /**
     * 构造函数
     *
     * @param width 精灵宽度
     * @param height 精灵高度
     */
    public SpriteRenderStrategy(float width, float height) {
        this.texturePaths = new HashMap<>();
        this.textures = new HashMap<>();
        this.width = width;
        this.height = height;
        this.currentDirection = Direction.FRONT;
        this.visible = true;
    }

    /**
     * 设置方向纹理
     *
     * @param direction 方向
     * @param texturePath 纹理路径
     */
    public void setTexture(Direction direction, String texturePath) {
        texturePaths.put(direction, texturePath);
    }

    /**
     * 设置AssetManager
     *
     * @param assetManager 资源管理器
     */
    public void setAssetManager(AssetManager assetManager) {
        this.assetManager = assetManager;
    }

    @Override
    public Type getType() {
        return Type.SPRITE_6DIR;
    }

    @Override
    public void initialize(Node parentNode) {
        this.parentNode = parentNode;

        if (assetManager == null) {
            java.lang.System.err.println("[SpriteRenderStrategy] AssetManager not set!");
            return;
        }

        // 加载所有纹理
        for (Map.Entry<Direction, String> entry : texturePaths.entrySet()) {
            try {
                Texture texture = assetManager.loadTexture(entry.getValue());
                textures.put(entry.getKey(), texture);
            } catch (Exception e) {
                java.lang.System.err.println("[SpriteRenderStrategy] Failed to load texture: " +
                    entry.getValue() + " - " + e.getMessage());
            }
        }

        // 创建Quad
        quad = new Quad(width, height);
        geometry = new Geometry("Sprite", quad);

        // 创建材质
        material = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");

        // 设置当前方向的纹理
        updateTexture(currentDirection);

        geometry.setMaterial(material);

        // 居中精灵
        geometry.setLocalTranslation(-width / 2, 0, 0);

        // 添加到父节点
        if (parentNode != null) {
            parentNode.attachChild(geometry);
        }
    }

    @Override
    public void update(float tpf) {
        // 这里可以添加自动选择方向的逻辑
        // 例如根据相机位置计算最佳方向
    }

    /**
     * 更新纹理
     *
     * @param direction 方向
     */
    private void updateTexture(Direction direction) {
        Texture texture = textures.get(direction);
        if (texture != null && material != null) {
            material.setTexture("ColorMap", texture);
            currentDirection = direction;
        }
    }

    /**
     * 设置当前方向
     *
     * @param direction 方向
     */
    public void setCurrentDirection(Direction direction) {
        if (direction != currentDirection) {
            updateTexture(direction);
        }
    }

    /**
     * 根据观察方向自动选择纹理
     *
     * @param viewDirection 观察方向（从相机到物体的方向）
     */
    public void setDirectionFromView(Vector3f viewDirection) {
        // 归一化
        Vector3f dir = viewDirection.normalize();

        // 判断主要方向
        float absX = Math.abs(dir.x);
        float absY = Math.abs(dir.y);
        float absZ = Math.abs(dir.z);

        Direction newDirection;

        if (absY > absX && absY > absZ) {
            // Y轴占主导
            newDirection = dir.y > 0 ? Direction.UP : Direction.DOWN;
        } else if (absX > absZ) {
            // X轴占主导
            newDirection = dir.x > 0 ? Direction.RIGHT : Direction.LEFT;
        } else {
            // Z轴占主导
            newDirection = dir.z > 0 ? Direction.FRONT : Direction.BACK;
        }

        setCurrentDirection(newDirection);
    }

    @Override
    public void cleanup() {
        if (geometry != null && parentNode != null) {
            parentNode.detachChild(geometry);
        }
        textures.clear();
    }

    @Override
    public void setVisible(boolean visible) {
        this.visible = visible;
        if (geometry != null) {
            if (visible) {
                geometry.setCullHint(com.jme3.scene.Spatial.CullHint.Dynamic);
            } else {
                geometry.setCullHint(com.jme3.scene.Spatial.CullHint.Always);
            }
        }
    }

    @Override
    public boolean isVisible() {
        return visible;
    }

    @Override
    public RenderStrategy clone() {
        SpriteRenderStrategy cloned = new SpriteRenderStrategy(width, height);
        cloned.texturePaths.putAll(this.texturePaths);
        cloned.currentDirection = this.currentDirection;
        cloned.assetManager = this.assetManager;
        return cloned;
    }

    // ========== Getters/Setters ==========

    public float getWidth() {
        return width;
    }

    public void setWidth(float width) {
        this.width = width;
        if (quad != null) {
            quad.updateGeometry(width, height);
            geometry.setLocalTranslation(-width / 2, 0, 0);
        }
    }

    public float getHeight() {
        return height;
    }

    public void setHeight(float height) {
        this.height = height;
        if (quad != null) {
            quad.updateGeometry(width, height);
        }
    }

    public Direction getCurrentDirection() {
        return currentDirection;
    }

    public Map<Direction, String> getTexturePaths() {
        return new HashMap<>(texturePaths);
    }
}
