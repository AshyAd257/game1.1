package com.Hecate.physics;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.BufferUtils;

/**
 * 碰撞箱渲染器 - 用于可视化调试胶囊形碰撞箱
 * 功能：将AABB碰撞箱渲染为胶囊形线框
 */
public class CollisionBoxRenderer {

    private final AssetManager assetManager;
    private final Node rootNode;

    // 线框几何体
    private Geometry capsuleGeometry;
    private Material wireframeMaterial;

    // 可见性控制
    private boolean isVisible = true;

    /**
     * 构造碰撞箱渲染器
     * @param assetManager 资源管理器
     * @param rootNode 根节点
     */
    public CollisionBoxRenderer(AssetManager assetManager, Node rootNode) {
        this.assetManager = assetManager;
        this.rootNode = rootNode;

        // 初始化线框材质
        initializeMaterial();
    }

    /**
     * 初始化线框材质
     */
    private void initializeMaterial() {
        wireframeMaterial = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        wireframeMaterial.setColor("Color", new ColorRGBA(0.0f, 1.0f, 0.0f, 1.0f)); // 绿色
        wireframeMaterial.getAdditionalRenderState().setWireframe(true);
        wireframeMaterial.getAdditionalRenderState().setDepthTest(false); // 始终显示在最前面
        wireframeMaterial.getAdditionalRenderState().setDepthWrite(false);
    }

    /**
     * 创建胶囊形线框网格
     * @param radius 半径（基于宽度）
     * @param height 总高度
     * @return 胶囊形网格
     */
    private Mesh createCapsuleMesh(float radius, float height) {
        // 胶囊 = 圆柱体 + 两个半球
        // 圆柱体高度 = 总高度 - 2*半径
        float cylinderHeight = height - 2 * radius;

        // 确保圆柱体高度为正
        if (cylinderHeight < 0) {
            cylinderHeight = 0;
        }

        int segments = 16; // 圆周分段数
        int hemisphereRings = 8; // 半球的环数

        // 重新计算顶点数量：
        // 圆柱体：segments * 2 （顶部和底部圆）
        // 上半球：segments * hemisphereRings + 1（极点）
        // 下半球：segments * hemisphereRings + 1（极点）
        int vertexCount = segments * 2 + segments * hemisphereRings * 2 + 2;

        // 重新计算索引数量：
        // 圆柱体顶部圆：segments条线 = segments * 2 索引
        // 圆柱体底部圆：segments条线 = segments * 2 索引
        // 圆柱体竖线：segments条线 = segments * 2 索引
        // 上半球纬线：segments * hemisphereRings条线 = segments * hemisphereRings * 2 索引
        // 上半球经线：segments * hemisphereRings条线 = segments * hemisphereRings * 2 索引
        // 上半球到极点：segments条线 = segments * 2 索引
        // 下半球纬线：segments * hemisphereRings条线 = segments * hemisphereRings * 2 索引
        // 下半球经线：segments * hemisphereRings条线 = segments * hemisphereRings * 2 索引
        // 下半球到极点：segments条线 = segments * 2 索引
        int indexCount = segments * 2 * 3 + segments * hemisphereRings * 2 * 4 + segments * 2 * 2;

        Vector3f[] vertices = new Vector3f[vertexCount];
        int[] indices = new int[indexCount];

        int vertexIndex = 0;
        int indexIndex = 0;

        float cylinderTop = cylinderHeight / 2;
        float cylinderBottom = -cylinderHeight / 2;

        // ===== 1. 圆柱体部分 =====
        // 顶部圆和底部圆
        int cylinderTopStart = vertexIndex;
        for (int i = 0; i < segments; i++) {
            float angle = (float) i / segments * FastMath.TWO_PI;
            float x = FastMath.cos(angle) * radius;
            float z = FastMath.sin(angle) * radius;

            // 顶部圆
            vertices[vertexIndex++] = new Vector3f(x, cylinderTop, z);
        }

        int cylinderBottomStart = vertexIndex;
        for (int i = 0; i < segments; i++) {
            float angle = (float) i / segments * FastMath.TWO_PI;
            float x = FastMath.cos(angle) * radius;
            float z = FastMath.sin(angle) * radius;

            // 底部圆
            vertices[vertexIndex++] = new Vector3f(x, cylinderBottom, z);
        }

        // 圆柱体顶部圆的线段
        for (int i = 0; i < segments; i++) {
            indices[indexIndex++] = cylinderTopStart + i;
            indices[indexIndex++] = cylinderTopStart + (i + 1) % segments;
        }

        // 圆柱体底部圆的线段
        for (int i = 0; i < segments; i++) {
            indices[indexIndex++] = cylinderBottomStart + i;
            indices[indexIndex++] = cylinderBottomStart + (i + 1) % segments;
        }

        // 圆柱体竖线
        for (int i = 0; i < segments; i++) {
            indices[indexIndex++] = cylinderTopStart + i;
            indices[indexIndex++] = cylinderBottomStart + i;
        }

        // ===== 2. 上半球 =====
        int upperHemisphereStart = vertexIndex;
        for (int ring = 1; ring <= hemisphereRings; ring++) {
            float phi = (float) ring / (hemisphereRings + 1) * FastMath.HALF_PI;
            float y = cylinderTop + FastMath.sin(phi) * radius;
            float ringRadius = FastMath.cos(phi) * radius;

            for (int i = 0; i < segments; i++) {
                float theta = (float) i / segments * FastMath.TWO_PI;
                float x = FastMath.cos(theta) * ringRadius;
                float z = FastMath.sin(theta) * ringRadius;
                vertices[vertexIndex++] = new Vector3f(x, y, z);
            }
        }

        // 上半球纬线和经线
        int prevRingStart = cylinderTopStart;
        for (int ring = 0; ring < hemisphereRings; ring++) {
            int currentRingStart = upperHemisphereStart + ring * segments;

            for (int i = 0; i < segments; i++) {
                // 纬线（同一环内相邻顶点）
                indices[indexIndex++] = currentRingStart + i;
                indices[indexIndex++] = currentRingStart + (i + 1) % segments;

                // 经线（连接上一环到当前环）
                indices[indexIndex++] = prevRingStart + i;
                indices[indexIndex++] = currentRingStart + i;
            }

            prevRingStart = currentRingStart;
        }

        // 上半球顶点（极点）
        int topPoleIndex = vertexIndex++;
        vertices[topPoleIndex] = new Vector3f(0, cylinderTop + radius, 0);

        // 连接最后一环到极点
        int lastUpperRing = upperHemisphereStart + (hemisphereRings - 1) * segments;
        for (int i = 0; i < segments; i++) {
            indices[indexIndex++] = lastUpperRing + i;
            indices[indexIndex++] = topPoleIndex;
        }

        // ===== 3. 下半球 =====
        int lowerHemisphereStart = vertexIndex;
        for (int ring = 1; ring <= hemisphereRings; ring++) {
            float phi = (float) ring / (hemisphereRings + 1) * FastMath.HALF_PI;
            float y = cylinderBottom - FastMath.sin(phi) * radius;
            float ringRadius = FastMath.cos(phi) * radius;

            for (int i = 0; i < segments; i++) {
                float theta = (float) i / segments * FastMath.TWO_PI;
                float x = FastMath.cos(theta) * ringRadius;
                float z = FastMath.sin(theta) * ringRadius;
                vertices[vertexIndex++] = new Vector3f(x, y, z);
            }
        }

        // 下半球纬线和经线
        prevRingStart = cylinderBottomStart;
        for (int ring = 0; ring < hemisphereRings; ring++) {
            int currentRingStart = lowerHemisphereStart + ring * segments;

            for (int i = 0; i < segments; i++) {
                // 纬线（同一环内相邻顶点）
                indices[indexIndex++] = currentRingStart + i;
                indices[indexIndex++] = currentRingStart + (i + 1) % segments;

                // 经线（连接上一环到当前环）
                indices[indexIndex++] = prevRingStart + i;
                indices[indexIndex++] = currentRingStart + i;
            }

            prevRingStart = currentRingStart;
        }

        // 下半球底点（极点）
        int bottomPoleIndex = vertexIndex++;
        vertices[bottomPoleIndex] = new Vector3f(0, cylinderBottom - radius, 0);

        // 连接最后一环到极点
        int lastLowerRing = lowerHemisphereStart + (hemisphereRings - 1) * segments;
        for (int i = 0; i < segments; i++) {
            indices[indexIndex++] = lastLowerRing + i;
            indices[indexIndex++] = bottomPoleIndex;
        }

        // 创建网格
        Mesh mesh = new Mesh();
        mesh.setMode(Mesh.Mode.Lines);
        mesh.setBuffer(VertexBuffer.Type.Position, 3, BufferUtils.createFloatBuffer(vertices));
        mesh.setBuffer(VertexBuffer.Type.Index, 2, BufferUtils.createIntBuffer(indices));
        mesh.updateBound();

        return mesh;
    }

    /**
     * 创建或更新碰撞箱可视化
     * @param aabb 要渲染的碰撞箱
     */
    public void updateCollisionBox(AABB aabb) {
        if (!isVisible) {
            return;
        }

        Vector3f center = aabb.getCenter();
        Vector3f size = aabb.getSize();

        // 计算胶囊参数
        float radius = Math.max(size.x, size.z) / 2; // 使用较大的水平尺寸作为半径
        float height = size.y;

        // 如果还没有创建几何体，则创建
        if (capsuleGeometry == null) {
            Mesh capsuleMesh = createCapsuleMesh(radius, height);
            capsuleGeometry = new Geometry("CollisionCapsule", capsuleMesh);
            capsuleGeometry.setMaterial(wireframeMaterial);
            capsuleGeometry.setQueueBucket(RenderQueue.Bucket.Translucent);

            // 添加到场景
            rootNode.attachChild(capsuleGeometry);
        } else {
            // 重新创建网格（尺寸变化时）
            Mesh capsuleMesh = createCapsuleMesh(radius, height);
            capsuleGeometry.setMesh(capsuleMesh);
        }

        // 设置位置到AABB中心
        capsuleGeometry.setLocalTranslation(center);
    }

    /**
     * 设置碰撞箱颜色
     * @param color 颜色
     */
    public void setColor(ColorRGBA color) {
        if (wireframeMaterial != null) {
            wireframeMaterial.setColor("Color", color);
        }
    }

    /**
     * 设置可见性
     * @param visible 是否可见
     */
    public void setVisible(boolean visible) {
        this.isVisible = visible;

        if (capsuleGeometry != null) {
            if (visible) {
                if (capsuleGeometry.getParent() == null) {
                    rootNode.attachChild(capsuleGeometry);
                }
            } else {
                capsuleGeometry.removeFromParent();
            }
        }
    }

    /**
     * 获取可见性状态
     * @return 是否可见
     */
    public boolean isVisible() {
        return isVisible;
    }

    /**
     * 切换可见性
     */
    public void toggleVisibility() {
        setVisible(!isVisible);
    }

    /**
     * 清理资源
     */
    public void cleanup() {
        if (capsuleGeometry != null) {
            capsuleGeometry.removeFromParent();
            capsuleGeometry = null;
        }
        wireframeMaterial = null;
    }
}
