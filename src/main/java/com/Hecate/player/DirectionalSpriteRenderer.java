package com.Hecate.player;

import com.jme3.app.SimpleApplication;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.Vector3f;
import com.jme3.math.Vector2f;
import com.jme3.math.Quaternion;
import com.jme3.renderer.Camera;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Quad;
import com.jme3.scene.control.BillboardControl;
import com.jme3.texture.Texture;
import com.jme3.math.FastMath;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Spatial;
import com.Hecate.utils.LogUtils;

/**
 * 方向性精灵渲染器
 * 负责渲染2D精灵并处理朝向、缩放等效果
 */
public class DirectionalSpriteRenderer {

    private final SimpleApplication app;
    private final Node spriteNode;

    // 渲染组件
    private Geometry spriteGeometry;
    private Material spriteMaterial;
    private Quad spriteQuad;
    private BillboardControl billboardControl;

    // 尺寸和缩放
    private float baseWidth = 2.0f;
    private float baseHeight = 2.0f;
    private float currentScale = 1.0f;
    private boolean maintainAspectRatio = true;

    // 朝向控制
    private boolean enableDirectionalFlip = true;
    private Vector3f lastFacingDirection = new Vector3f();
    private boolean isFlippedX = false;

    // 俯视模式
    private boolean isTopViewMode = false;
    private boolean wasTopViewMode = false;
    private Camera currentCamera = null;
    private boolean rotationLogged = false;

    // 动画相关
    private AnimationFrame currentFrame;
    private Texture currentTexture;

    // 渲染设置
    private boolean enableTransparency = true;
    private boolean enablePixelPerfect = true;
    private float alphaThreshold = 0.1f;

    public DirectionalSpriteRenderer(SimpleApplication app, Node spriteNode) {
        this.app = app;
        this.spriteNode = spriteNode;
        initializeRenderer();
    }

    /**
     * 初始化渲染器
     */
    private void initializeRenderer() {
        // 创建四边形几何体 - 使用标准Quad
        spriteQuad = new Quad(baseWidth, baseHeight);
        spriteGeometry = new Geometry("PlayerSprite", spriteQuad);

        // 创建材质
        createSpriteMaterial();

        // 设置渲染属性
        setupRenderingProperties();

        // 修改广告牌控制设置
        billboardControl = new BillboardControl();
        billboardControl.setAlignment(BillboardControl.Alignment.AxialY);
        spriteGeometry.addControl(billboardControl);

        // 添加到节点
        spriteNode.attachChild(spriteGeometry);
    }

    /**
     * 创建精灵材质 - 完全兼容JME3 3.5.2版本
     */
    private void createSpriteMaterial() {
        spriteMaterial = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");

        if (enableTransparency) {
            spriteMaterial.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
            spriteMaterial.setTransparent(true);
        }

        spriteMaterial.setColor("Color", ColorRGBA.White);
        spriteGeometry.setMaterial(spriteMaterial);
    }

    /**
     * 设置渲染属性
     */
    private void setupRenderingProperties() {
        // 设置渲染队列（透明物体）
        spriteGeometry.setQueueBucket(RenderQueue.Bucket.Transparent);

        // 禁用深度写入（避免透明问题）
        spriteMaterial.getAdditionalRenderState().setDepthWrite(false);

        // 启用背面剔除
        spriteMaterial.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Back);
    }

    /**
     * 更新渲染器
     */
    public void update(float tpf, Vector3f playerPosition, Vector3f facingDirection) {
        // 更新位置
        updatePosition(playerPosition);

        // 俯视模式和普通模式都不需要手动旋转
        // BillboardControl会自动让精灵面向摄像机
        // 只需要根据角度切换不同的纹理即可

        // 更新当前帧显示
        updateFrameDisplay();
    }

    /**
     * 设置俯视模式
     */
    public void setTopViewMode(boolean topViewMode, Camera camera) {
        // 只在模式真正改变时才执行切换逻辑
        if (topViewMode == wasTopViewMode) {
            this.currentCamera = camera;
            return;
        }

        this.isTopViewMode = topViewMode;
        this.currentCamera = camera;
        this.wasTopViewMode = topViewMode;

        // 俯视模式和普通模式都使用BillboardControl
        // 区别只是显示的纹理不同（top_idle vs front_idle等）
    }

    /**
     * 更新俯视模式下的旋转
     */
    private void updateTopViewRotation() {
        // 在俯视模式下，精灵需要平躺在地面上
        // Quad默认在XY平面，面向+Z方向
        // 绕X轴旋转90度，让它平躺在XZ平面（面向+Y方向）

        Quaternion rotation = new Quaternion();
        rotation.fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_X);
        spriteGeometry.setLocalRotation(rotation);

    }

    /**
     * 更新精灵位置
     */
    private void updatePosition(Vector3f playerPosition) {
        // 直接使用传入的位置（已经在PlayerController中设置好偏移）
        spriteGeometry.setLocalTranslation(playerPosition);
    }

    /**
     * 更新方向翻转
     */
    private void updateDirectionalFlip(Vector3f facingDirection) {
        if (facingDirection.equals(lastFacingDirection)) {
            return;
        }

        // 判断是否需要翻转（基于X轴方向）
        boolean shouldFlipX = facingDirection.x < 0;

        if (shouldFlipX != isFlippedX) {
            flipSpriteX(shouldFlipX);
            isFlippedX = shouldFlipX;
        }

        lastFacingDirection.set(facingDirection);
    }

    /**
     * X轴翻转精灵
     */
    private void flipSpriteX(boolean flip) {
        // 通过修改纹理坐标实现翻转
        spriteQuad = new Quad(baseWidth * currentScale, baseHeight * currentScale);

        if (flip) {
            // 翻转纹理坐标
            spriteQuad.scaleTextureCoordinates(new Vector2f(-1, 1));
        }

        // 更新几何体
        spriteGeometry.setMesh(spriteQuad);

        // 如果有当前纹理，重新应用
        if (currentTexture != null) {
            spriteMaterial.setTexture("ColorMap", currentTexture);
        }
    }

    /**
     * 更新帧显示
     */
    private void updateFrameDisplay() {
        // 这个方法将在设置新帧时被调用
    }

    /**
     * 设置当前显示帧 - 修复版
     */
    public void setCurrentFrame(AnimationFrame frame) {
        if (frame == null) {
            LogUtils.warning(DirectionalSpriteRenderer.class, "setCurrentFrame: frame为null");
            return;
        }

        currentFrame = frame;
        Texture newTexture = frame.getTexture();

        if (newTexture != null) {
            setTexture(newTexture);
            currentTexture = newTexture;

            // 确保精灵可见
            if (spriteGeometry.getCullHint() != Spatial.CullHint.Never) {
                spriteGeometry.setCullHint(Spatial.CullHint.Never);
            }

            // 俯视模式下静默设置帧
        } else {
            LogUtils.warning(DirectionalSpriteRenderer.class,
                "setCurrentFrame: 纹理为null, 帧名: " + frame.getFrameName());
            if (spriteMaterial != null) {
                // 设置为品红色表示错误
                spriteMaterial.setColor("Color", ColorRGBA.Magenta);
                // 清除纹理参数
                spriteMaterial.clearParam("ColorMap");
            }
        }
    }

    /**
     * 设置纹理 - 修复版
     */
    private void setTexture(Texture texture) {
        if (texture == null) {
            return;
        }

        // 设置像素完美过滤
        if (enablePixelPerfect) {
            texture.setMagFilter(Texture.MagFilter.Nearest);
            texture.setMinFilter(Texture.MinFilter.NearestNoMipMaps);
        }

        // 应用纹理到材质
        spriteMaterial.setTexture("ColorMap", texture);

        // 设置白色以确保纹理显示正确
        spriteMaterial.setColor("Color", ColorRGBA.White);

        // 确保使用正确的混合模式
        if (enableTransparency) {
            spriteMaterial.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
            spriteMaterial.setTransparent(true);
        }

        // 强制更新几何体的材质
        spriteGeometry.setMaterial(spriteMaterial);

        // 根据纹理调整精灵尺寸（可选）
        if (maintainAspectRatio) {
            adjustSizeToTexture(texture);
        }
    }

    public void setTextureFromPath(String texturePath) {
        try {
            Texture texture = app.getAssetManager().loadTexture(texturePath);
            setTexture(texture);
            currentTexture = texture;
        } catch (Exception e) {
            LogUtils.error(DirectionalSpriteRenderer.class, "加载纹理失败: " + texturePath, e);
        }
    }

    /**
     * 根据纹理调整尺寸
     */
    private void adjustSizeToTexture(Texture texture) {
        if (texture.getImage() != null) {
            int texWidth = texture.getImage().getWidth();
            int texHeight = texture.getImage().getHeight();

            // 计算宽高比
            float aspectRatio = (float) texWidth / texHeight;

            // 调整精灵尺寸
            float newWidth = baseHeight * aspectRatio * currentScale;
            float newHeight = baseHeight * currentScale;

            // 更新四边形尺寸
            spriteQuad = new Quad(newWidth, newHeight);
            spriteGeometry.setMesh(spriteQuad);
        }
    }

    /**
     * 更新缩放
     */
    public void updateScale(float scale) {
        if (scale <= 0) {
            LogUtils.warning(DirectionalSpriteRenderer.class, "无效的缩放值: " + scale);
            return;
        }

        currentScale = scale;

        // 重新计算尺寸
        float newWidth = baseWidth * currentScale;
        float newHeight = baseHeight * currentScale;

        // 如果有纹理且保持宽高比，重新调整
        if (currentTexture != null && maintainAspectRatio) {
            adjustSizeToTexture(currentTexture);
        } else {
            // 直接应用缩放
            spriteQuad = new Quad(newWidth, newHeight);
            spriteGeometry.setMesh(spriteQuad);
        }
    }

    /**
     * 设置基础尺寸
     */
    public void setBaseSize(float width, float height) {
        this.baseWidth = width;
        this.baseHeight = height;
        updateScale(currentScale); // 重新应用缩放
    }

    /**
     * 设置透明度 - 安全版本
     */
    public void setAlpha(float alpha) {
        alpha = FastMath.clamp(alpha, 0f, 1f);

        // 安全获取当前颜色
        ColorRGBA currentColor = ColorRGBA.White; // 默认白色
        try {
            Object colorParam = spriteMaterial.getParam("Color");
            if (colorParam != null) {
                currentColor = (ColorRGBA) spriteMaterial.getParam("Color").getValue();
            }
        } catch (Exception e) {
            // 使用默认颜色
        }

        if (currentColor == null) {
            currentColor = ColorRGBA.White;
        }

        spriteMaterial.setColor("Color", new ColorRGBA(
                currentColor.r, currentColor.g, currentColor.b, alpha));
    }

    /**
     * 设置可见性
     */
    public void setVisible(boolean visible) {
        spriteGeometry.setCullHint(visible ?
                Spatial.CullHint.Never :
                Spatial.CullHint.Always);
    }

    /**
     * 启用/禁用方向翻转
     */
    public void setDirectionalFlipEnabled(boolean enabled) {
        this.enableDirectionalFlip = enabled;
    }

    /**
     * 启用/禁用保持宽高比
     */
    public void setMaintainAspectRatio(boolean maintain) {
        this.maintainAspectRatio = maintain;
        if (currentTexture != null) {
            updateScale(currentScale);
        }
    }

    /**
     * 启用/禁用像素完美
     */
    public void setPixelPerfectEnabled(boolean enabled) {
        this.enablePixelPerfect = enabled;
        if (currentTexture != null) {
            setTexture(currentTexture);
        }
    }

    // Getter方法
    public float getCurrentScale() {
        return currentScale;
    }

    public Vector3f getBaseSize() {
        return new Vector3f(baseWidth, baseHeight, 0);
    }

    public AnimationFrame getCurrentFrame() {
        return currentFrame;
    }

    public boolean isDirectionalFlipEnabled() {
        return enableDirectionalFlip;
    }

    public boolean isMaintainAspectRatio() {
        return maintainAspectRatio;
    }

    public boolean isPixelPerfectEnabled() {
        return enablePixelPerfect;
    }

    public boolean isFlippedX() {
        return isFlippedX;
    }

    /**
     * 获取渲染器状态信息
     */
    public String getRendererStatus() {
        return String.format("精灵渲染器 - 缩放: %.1fx, 翻转: %s, 当前帧: %s",
                currentScale,
                isFlippedX ? "是" : "否",
                currentFrame != null ? currentFrame.getFrameName() : "无");
    }

    /**
     * 清理资源
     */
    public void cleanup() {
        if (spriteGeometry != null) {
            spriteGeometry.removeFromParent();
        }
        currentFrame = null;
        currentTexture = null;
    }
}
