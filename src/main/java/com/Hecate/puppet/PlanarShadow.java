package com.Hecate.puppet;

import com.Hecate.puppet.core.Bone;
import com.Hecate.puppet.core.PuppetPartRenderer;
import com.Hecate.puppet.core.PuppetRenderer;
import com.Hecate.puppet.core.Skeleton;
import com.jme3.app.SimpleApplication;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Texture;
import com.jme3.collision.CollisionResults;

import java.util.HashMap;
import java.util.Map;

/**
 * 平面阴影系统 - 为Puppet创建投影到地面的阴影
 * 通过复制puppet的贴图并将其渲染为半透明黑色来实现
 */
public class PlanarShadow {

    private final SimpleApplication app;
    private final PuppetRenderer puppetRenderer;
    private final Node shadowNode;
    private Node worldNode;  // 世界节点，用于射线检测

    // 阴影部件映射 (boneName -> shadowGeometry)
    private final Map<String, Geometry> shadowParts = new HashMap<>();

    // 阴影配置
    private float shadowOpacity = 0.5f;  // 阴影不透明度
    private float shadowScale = 1.0f;    // 阴影缩放
    private float groundY = 0.0f;        // 地面Y坐标
    private Vector3f shadowOffset = new Vector3f(0, 0.01f, 0);  // 阴影偏移（避免Z-fighting，抬高0.01单位）

    // 调试模式：使用白色阴影以确认可见性
    private boolean debugMode = false;  // 改为false使用黑色阴影

    public PlanarShadow(SimpleApplication app, PuppetRenderer puppetRenderer) {
        this.app = app;
        this.puppetRenderer = puppetRenderer;
        this.shadowNode = new Node("PuppetShadow");
    }

    /**
     * 初始化阴影系统
     */
    public void initialize(Node parentNode) {
        // 将阴影节点添加到场景
        parentNode.attachChild(shadowNode);

        // 为每个puppet部件创建阴影
        createShadowParts();
    }

    /**
     * 为所有puppet部件创建阴影几何体
     */
    private void createShadowParts() {
        Skeleton skeleton = puppetRenderer.getSkeleton();

        for (Bone bone : skeleton.getAllBones()) {
            PuppetPartRenderer partRenderer = puppetRenderer.getPartRenderer(bone.getName());
            if (partRenderer != null && partRenderer.isInitialized()) {
                createShadowForPart(bone, partRenderer);
            }
        }
    }

    /**
     * 为单个部件创建阴影
     */
    private void createShadowForPart(Bone bone, PuppetPartRenderer partRenderer) {
        // 创建与部件相同大小的quad
        float width = partRenderer.getWidth();
        float height = partRenderer.getHeight();

        Quad shadowQuad = new Quad(width, height);

        // 设置顶点位置（XZ平面，Y=0）
        shadowQuad.setBuffer(com.jme3.scene.VertexBuffer.Type.Position, 3, new float[]{
            -width/2, 0, -height/2,  // 左下
            width/2, 0, -height/2,   // 右下
            width/2, 0, height/2,    // 右上
            -width/2, 0, height/2    // 左上
        });

        // 设置法线（朝上）
        shadowQuad.setBuffer(com.jme3.scene.VertexBuffer.Type.Normal, 3, new float[]{
            0, 1, 0,
            0, 1, 0,
            0, 1, 0,
            0, 1, 0
        });

        Geometry shadowGeom = new Geometry(bone.getName() + "_Shadow", shadowQuad);

        // 创建阴影材质
        Material shadowMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");

        // 加载原始贴图
        Texture texture = partRenderer.getTexture();
        if (texture != null) {
            shadowMat.setTexture("ColorMap", texture);
        }

        // 调试模式：使用白色以确认阴影可见
        // 正常模式：使用半透明黑色
        if (debugMode) {
            shadowMat.setColor("Color", new ColorRGBA(1, 1, 1, 1.0f)); // 白色，完全不透明
        } else {
            shadowMat.setColor("Color", new ColorRGBA(0, 0, 0, shadowOpacity));
        }

        shadowMat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        shadowMat.setTransparent(true);

        // 禁用背面剔除（确保从任何角度都能看到）
        shadowMat.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);

        // 禁用深度写入，避免阴影遮挡其他物体
        shadowMat.getAdditionalRenderState().setDepthWrite(false);

        // 确保深度测试启用（默认应该是启用的）
        shadowMat.getAdditionalRenderState().setDepthTest(true);

        // 启用alpha测试 - 让光线穿过透明部分！
        // 透明度小于0.1的像素不会渲染，实现透明阴影投射
        shadowMat.setFloat("AlphaDiscardThreshold", 0.1f);

        shadowGeom.setMaterial(shadowMat);

        // 设置渲染队列为Transparent（确保正确渲染透明物体）
        shadowGeom.setQueueBucket(com.jme3.renderer.queue.RenderQueue.Bucket.Transparent);

        // 设置UV坐标（与原部件相同）
        float[] uvCoords = new float[]{
            partRenderer.getUvOffsetX(), partRenderer.getUvOffsetY(),
            partRenderer.getUvOffsetX() + partRenderer.getUvScaleX(), partRenderer.getUvOffsetY(),
            partRenderer.getUvOffsetX() + partRenderer.getUvScaleX(), partRenderer.getUvOffsetY() + partRenderer.getUvScaleY(),
            partRenderer.getUvOffsetX(), partRenderer.getUvOffsetY() + partRenderer.getUvScaleY()
        };
        shadowQuad.setBuffer(com.jme3.scene.VertexBuffer.Type.TexCoord, 2, uvCoords);

        // 添加到阴影节点
        shadowNode.attachChild(shadowGeom);
        shadowParts.put(bone.getName(), shadowGeom);
    }

    /**
     * 更新阴影位置和变换
     * @param puppetBaseY 人物的当前基础Y坐标（用于射线检测起点）
     */
    public void update(float tpf, float puppetBaseY) {
        Skeleton skeleton = puppetRenderer.getSkeleton();

        for (Bone bone : skeleton.getAllBones()) {
            Geometry shadowGeom = shadowParts.get(bone.getName());
            if (shadowGeom == null) {
                continue;
            }

            PuppetPartRenderer partRenderer = puppetRenderer.getPartRenderer(bone.getName());
            if (partRenderer == null || !partRenderer.isInitialized()) {
                continue;
            }

            // 获取部件的世界位置
            Vector3f partWorldPos = partRenderer.getFinalWorldPosition();

            // 使用射线检测找到脚下的地面高度
            float groundHeight = findGroundHeight(partWorldPos.x, puppetBaseY, partWorldPos.z);

            // 将阴影投影到地面继续我们在game1(1) 里的工作，我们之前在构建光影系统，但是地面始终不显示人物的投影，然后我们创建了一个用于测试的纯
            //白地面，事实证明投影系统是正常工作的，但在游戏地面上无法显示影子
            Vector3f shadowPos = new Vector3f(
                partWorldPos.x,
                groundHeight + shadowOffset.y,  // 使用检测到的地面高度
                partWorldPos.z
            );

            shadowGeom.setLocalTranslation(shadowPos);

            // 阴影始终水平朝上（不旋转）
            shadowGeom.setLocalRotation(com.jme3.math.Quaternion.IDENTITY);

            // 应用缩放
            shadowGeom.setLocalScale(shadowScale);
        }
    }

    /**
     * 使用射线检测找到指定位置下方的地面高度
     */
    private float findGroundHeight(float x, float startY, float z) {
        if (worldNode == null) {
            // 如果没有世界节点，使用默认地面高度
            return groundY;
        }

        // 从人物位置向下发射射线
        Vector3f rayStart = new Vector3f(x, startY + 10f, z);  // 从人物上方开始
        Vector3f rayDirection = new Vector3f(0, -1, 0);  // 向下
        Ray ray = new Ray(rayStart, rayDirection);

        // 检测碰撞
        CollisionResults results = new CollisionResults();
        worldNode.collideWith(ray, results);

        if (results.size() > 0) {
            // 找到地面，返回碰撞点的Y坐标
            return results.getClosestCollision().getContactPoint().y;
        }

        // 没有找到地面，使用默认高度
        return groundY;
    }

    /**
     * 设置世界节点（用于射线检测）
     */
    public void setWorldNode(Node worldNode) {
        this.worldNode = worldNode;
    }

    /**
     * 更新阴影位置和变换（向后兼容的重载方法）
     */
    public void update(float tpf) {
        update(tpf, groundY + 2.0f);  // 使用初始groundY作为默认值
    }

    /**
     * 设置阴影不透明度
     */
    public void setShadowOpacity(float opacity) {
        this.shadowOpacity = Math.max(0f, Math.min(1f, opacity));

        // 更新所有阴影材质
        for (Geometry shadowGeom : shadowParts.values()) {
            Material mat = shadowGeom.getMaterial();
            mat.setColor("Color", new ColorRGBA(0, 0, 0, shadowOpacity));
        }
    }

    /**
     * 设置阴影缩放
     */
    public void setShadowScale(float scale) {
        this.shadowScale = scale;
    }

    /**
     * 设置地面高度
     */
    public void setGroundY(float y) {
        this.groundY = y;
    }

    /**
     * 设置阴影可见性
     */
    public void setVisible(boolean visible) {
        shadowNode.setCullHint(visible ?
            com.jme3.scene.Spatial.CullHint.Never :
            com.jme3.scene.Spatial.CullHint.Always);
    }

    /**
     * 清理资源
     */
    public void cleanup() {
        shadowNode.removeFromParent();
        shadowParts.clear();
    }
}
