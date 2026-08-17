package com.Hecate.puppet;

import com.jme3.app.SimpleApplication;
import com.jme3.light.DirectionalLight;
import com.jme3.light.Light;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.VertexBuffer;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * 椭圆形阴影系统 - 基于光源方向生成动态椭圆阴影
 *
 * 特性：
 * - 支持多个光源（主光源为太阳光）
 * - 椭圆的大小、拉伸和方向根据光源位置自动计算
 * - 阴影随光照角度动态变化
 * - 比复制贴图的方法更高效、更实用
 */
public class EllipticalShadow {

    private final SimpleApplication app;
    private final Node shadowNode;
    private Geometry shadowGeometry;

    // 阴影配置
    private float baseRadius = 0.5f;           // 基础半径（人物脚下的阴影大小）
    private float shadowOpacity = 0.4f;        // 阴影不透明度
    private float maxStretchFactor = 3.0f;     // 最大拉伸倍数（光源接近水平时）
    private float shadowYOffset = 0.02f;       // 阴影高度偏移（避免Z-fighting）

    // 光源信息
    private DirectionalLight mainLight;        // 主光源（太阳光）
    private final List<DirectionalLight> additionalLights = new ArrayList<>();

    // 人物位置信息
    private Vector3f characterPosition = new Vector3f();
    private float characterHeight = 1.8f;      // 人物高度（用于计算阴影投影）

    // 调试模式
    private boolean debugMode = false;

    public EllipticalShadow(SimpleApplication app) {
        this.app = app;
        this.shadowNode = new Node("EllipticalShadow");
    }

    /**
     * 初始化阴影系统
     */
    public void initialize(Node parentNode) {
        // 创建椭圆形网格
        createEllipseMesh();

        // 将阴影节点添加到场景
        parentNode.attachChild(shadowNode);

        // 自动检测场景中的光源
        detectLights();
    }

    /**
     * 自动检测场景中的方向光
     */
    private void detectLights() {
        if (app.getRootNode() == null) {
            return;
        }

        // 遍历场景中的所有光源
        for (Light light : app.getRootNode().getLocalLightList()) {
            if (light instanceof DirectionalLight) {
                DirectionalLight dirLight = (DirectionalLight) light;

                // 第一个光源作为主光源
                if (mainLight == null) {
                    mainLight = dirLight;
                } else {
                    additionalLights.add(dirLight);
                }
            }
        }

        if (mainLight == null) {
            // 创建一个默认的向下光源
            mainLight = new DirectionalLight();
            mainLight.setDirection(new Vector3f(0.3f, -1f, 0.3f).normalizeLocal());
        }
    }

    /**
     * 创建椭圆形网格
     */
    private void createEllipseMesh() {
        Mesh ellipseMesh = createEllipse(32); // 32个顶点的椭圆

        shadowGeometry = new Geometry("EllipseShadow", ellipseMesh);

        // 创建阴影材质
        Material shadowMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");

        // 【改进质感】创建软边缘渐变纹理
        com.jme3.texture.Texture2D shadowTexture = createSoftShadowTexture();
        shadowMat.setTexture("ColorMap", shadowTexture);

        // 设置半透明黑色（与贴图相乘）
        shadowMat.setColor("Color", new ColorRGBA(0, 0, 0, shadowOpacity));

        // 启用透明度混合
        shadowMat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        shadowMat.setTransparent(true);

        // 禁用背面剔除
        shadowMat.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);

        // 禁用深度写入，避免阴影遮挡其他物体
        shadowMat.getAdditionalRenderState().setDepthWrite(false);

        // 启用深度测试
        shadowMat.getAdditionalRenderState().setDepthTest(true);

        shadowGeometry.setMaterial(shadowMat);

        // 设置渲染队列为Transparent
        shadowGeometry.setQueueBucket(com.jme3.renderer.queue.RenderQueue.Bucket.Transparent);

        shadowNode.attachChild(shadowGeometry);
    }

    /**
     * 创建软边缘的阴影纹理（径向渐变，边缘柔和）
     */
    private com.jme3.texture.Texture2D createSoftShadowTexture() {
        int size = 128;
        com.jme3.texture.Image.Format format = com.jme3.texture.Image.Format.RGBA8;

        java.nio.ByteBuffer data = java.nio.ByteBuffer.allocateDirect(size * size * 4);

        float centerX = size / 2f;
        float centerY = size / 2f;

        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                // 计算到中心的距离（归一化到0-1）
                float dx = (x - centerX) / centerX;
                float dy = (y - centerY) / centerY;
                float distance = (float) Math.sqrt(dx * dx + dy * dy);

                // 径向渐变：中心=1.0（不透明），边缘=0.0（完全透明）
                // 使用平滑曲线让边缘更柔和
                float alpha = 1.0f - FastMath.clamp(distance, 0f, 1f);
                alpha = alpha * alpha; // 平方曲线，让渐变更自然

                // RGBA：白色，alpha通道控制透明度
                data.put((byte) 255);  // R
                data.put((byte) 255);  // G
                data.put((byte) 255);  // B
                data.put((byte) (alpha * 255));  // A
            }
        }

        data.rewind();

        com.jme3.texture.Image image = new com.jme3.texture.Image(
            format, size, size, data, com.jme3.texture.image.ColorSpace.Linear
        );

        com.jme3.texture.Texture2D texture = new com.jme3.texture.Texture2D(image);
        texture.setMinFilter(com.jme3.texture.Texture.MinFilter.BilinearNoMipMaps);
        texture.setMagFilter(com.jme3.texture.Texture.MagFilter.Bilinear);
        texture.setWrap(com.jme3.texture.Texture.WrapMode.Clamp);

        return texture;
    }

    /**
     * 创建椭圆形网格（在XZ平面上）
     */
    private Mesh createEllipse(int segments) {
        Mesh mesh = new Mesh();

        // 顶点数量：中心点 + 圆周上的点
        int vertexCount = segments + 1;

        // 顶点位置
        float[] positions = new float[vertexCount * 3];

        // 中心点 (0, 0, 0)
        positions[0] = 0;
        positions[1] = 0;
        positions[2] = 0;

        // 圆周上的点（初始为圆形，后面会根据光源拉伸成椭圆）
        for (int i = 0; i < segments; i++) {
            float angle = (float) (2 * Math.PI * i / segments);
            float x = (float) Math.cos(angle);
            float z = (float) Math.sin(angle);

            positions[(i + 1) * 3 + 0] = x;
            positions[(i + 1) * 3 + 1] = 0;
            positions[(i + 1) * 3 + 2] = z;
        }

        // 法线（全部朝上）
        float[] normals = new float[vertexCount * 3];
        for (int i = 0; i < vertexCount; i++) {
            normals[i * 3 + 0] = 0;
            normals[i * 3 + 1] = 1;
            normals[i * 3 + 2] = 0;
        }

        // 三角形索引（扇形）
        int[] indices = new int[segments * 3];
        for (int i = 0; i < segments; i++) {
            indices[i * 3 + 0] = 0;              // 中心点
            indices[i * 3 + 1] = i + 1;          // 当前圆周点
            indices[i * 3 + 2] = (i + 1) % segments + 1; // 下一个圆周点
        }

        mesh.setBuffer(VertexBuffer.Type.Position, 3, positions);
        mesh.setBuffer(VertexBuffer.Type.Normal, 3, normals);
        mesh.setBuffer(VertexBuffer.Type.Index, 3, indices);

        mesh.updateBound();

        return mesh;
    }

    /**
     * 更新阴影位置和形状
     */
    public void update(Vector3f characterPos) {
        if (shadowGeometry == null || mainLight == null) {
            return;
        }

        this.characterPosition.set(characterPos);

        // 计算阴影的位置、大小和方向
        updateShadowTransform();

        // 如果有多个光源，可以创建多个阴影（这里先只处理主光源）
        // TODO: 多光源支持 - 可以叠加多个半透明阴影
    }

    /**
     * 更新阴影的变换（位置、旋转、缩放）
     */
    private void updateShadowTransform() {
        // 获取光源方向
        Vector3f lightDir = mainLight.getDirection().normalize();

        // 如果光源从下方照射，不显示阴影
        if (lightDir.y >= 0) {
            shadowNode.setCullHint(com.jme3.scene.Spatial.CullHint.Always);
            return;
        }
        shadowNode.setCullHint(com.jme3.scene.Spatial.CullHint.Never);

        // 计算光源的水平方向和角度
        Vector3f lightHorizontal = new Vector3f(lightDir.x, 0, lightDir.z);
        float horizontalLength = lightHorizontal.length();

        if (horizontalLength < 0.001f) {
            // 光源正上方，使用圆形阴影
            shadowGeometry.setLocalTranslation(
                characterPosition.x,
                characterPosition.y + shadowYOffset,
                characterPosition.z
            );
            shadowGeometry.setLocalScale(baseRadius);
            shadowGeometry.setLocalRotation(com.jme3.math.Quaternion.IDENTITY);
            return;
        }

        lightHorizontal.normalizeLocal();

        // 计算光源的仰角（光源越接近水平，阴影越长）
        float elevation = FastMath.asin(-lightDir.y); // 仰角（0到π/2）

        // 计算拉伸因子：光源越接近水平，拉伸越大
        // elevation = π/2（正上方）→ stretch = 1.0（圆形）
        // elevation = 0（水平）→ stretch = maxStretchFactor（最长椭圆）
        float stretchFactor = 1.0f + (maxStretchFactor - 1.0f) * (1.0f - elevation / (FastMath.PI / 2));

        // 计算阴影偏移：光源越斜，阴影中心越远离人物脚下
        float shadowOffset = characterHeight * FastMath.tan(FastMath.PI / 2 - elevation) * 0.5f;

        // 阴影中心位置（沿光源水平方向偏移）
        Vector3f shadowCenter = characterPosition.add(
            lightHorizontal.x * shadowOffset,
            shadowYOffset,
            lightHorizontal.z * shadowOffset
        );

        shadowGeometry.setLocalTranslation(shadowCenter);

        // 计算旋转角度（椭圆的长轴指向光源方向）
        // 【修正】旋转90度，让椭圆的长轴（X轴）与光源水平方向对齐
        float angle = FastMath.atan2(lightHorizontal.x, lightHorizontal.z) + FastMath.HALF_PI;
        com.jme3.math.Quaternion rotation = new com.jme3.math.Quaternion();
        rotation.fromAngleAxis(angle, Vector3f.UNIT_Y);

        shadowGeometry.setLocalRotation(rotation);

        // 设置缩放（x方向拉伸，z方向保持）
        shadowGeometry.setLocalScale(
            baseRadius * stretchFactor,  // 沿光源方向拉伸
            1.0f,
            baseRadius                   // 垂直于光源方向保持
        );

        if (debugMode) {
            // Debug mode: shadow parameters logged
        }
    }

    /**
     * 手动设置主光源
     */
    public void setMainLight(DirectionalLight light) {
        this.mainLight = light;
    }

    /**
     * 添加额外光源
     */
    public void addLight(DirectionalLight light) {
        if (!additionalLights.contains(light)) {
            additionalLights.add(light);
        }
    }

    /**
     * 设置人物高度
     */
    public void setCharacterHeight(float height) {
        this.characterHeight = height;
    }

    /**
     * 设置阴影基础半径
     */
    public void setBaseRadius(float radius) {
        this.baseRadius = radius;
    }

    /**
     * 设置阴影不透明度
     */
    public void setShadowOpacity(float opacity) {
        this.shadowOpacity = Math.max(0f, Math.min(1f, opacity));

        if (shadowGeometry != null) {
            Material mat = shadowGeometry.getMaterial();
            mat.setColor("Color", new ColorRGBA(0, 0, 0, shadowOpacity));
        }
    }

    /**
     * 设置最大拉伸倍数
     */
    public void setMaxStretchFactor(float factor) {
        this.maxStretchFactor = Math.max(1.0f, factor);
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
     * 启用/禁用调试模式
     */
    public void setDebugMode(boolean enabled) {
        this.debugMode = enabled;
    }

    /**
     * 清理资源
     */
    public void cleanup() {
        shadowNode.removeFromParent();
        additionalLights.clear();
    }
}
