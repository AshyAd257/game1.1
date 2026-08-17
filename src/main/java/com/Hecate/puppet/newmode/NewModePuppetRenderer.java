package com.Hecate.puppet.newmode;

import com.jme3.app.SimpleApplication;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.texture.Texture;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 新模式渲染器 - 八棱柱卡片渲染系统
 *
 * 核心功能：
 * 1. 使用 EdgeLinkedCardRing 计算卡片的投影位置和宽度
 * 2. 根据计算结果动态调整每张卡片的几何体
 * 3. 自动处理背面剔除和深度排序
 */
public class NewModePuppetRenderer {

    private final SimpleApplication app;
    private final NewModeSkeleton skeleton;
    private final Node puppetNode;

    // 每个骨骼对应一个卡片环渲染器
    private final Map<String, BoneCardRenderer> boneRenderers = new HashMap<>();

    private boolean initialized = false;

    public NewModePuppetRenderer(SimpleApplication app, NewModeSkeleton skeleton) {
        this.app = app;
        this.skeleton = skeleton;
        this.puppetNode = new Node(skeleton.getName() + "_Node");
    }

    /**
     * 初始化渲染器
     */
    public void initialize() {
        if (initialized) {
            return;
        }

        // 为每个骨骼创建渲染器
        for (NewModeBone bone : skeleton.getAllBones()) {
            BoneCardRenderer renderer = new BoneCardRenderer(app, bone, puppetNode);
            renderer.initialize();
            boneRenderers.put(bone.getName(), renderer);
        }

        initialized = true;
    }

    /**
     * 更新渲染（每帧调用）
     */
    public void update(float tpf) {
        if (!initialized) {
            return;
        }

        // 获取相机位置和方向
        Vector3f cameraPos = app.getCamera().getLocation();
        Vector3f cameraDir = app.getCamera().getDirection();

        // 更新每个骨骼的卡片
        for (BoneCardRenderer renderer : boneRenderers.values()) {
            renderer.update(cameraPos, cameraDir);
        }
    }

    /**
     * 附加到场景
     */
    public void attachToScene(Node sceneNode) {
        sceneNode.attachChild(puppetNode);
    }

    /**
     * 从场景移除
     */
    public void detachFromScene() {
        puppetNode.removeFromParent();
    }

    /**
     * 清理资源
     */
    public void cleanup() {
        for (BoneCardRenderer renderer : boneRenderers.values()) {
            renderer.cleanup();
        }
        boneRenderers.clear();
        puppetNode.removeFromParent();
        initialized = false;
    }

    /**
     * 刷新指定骨骼的几何（Ring Radius, Card Height 变化时调用）
     */
    public void refreshBoneGeometry(NewModeBone bone) {
        BoneCardRenderer renderer = boneRenderers.get(bone.getName());
        if (renderer != null) {
            renderer.refreshGeometry();
        }
    }

    /**
     * 刷新指定卡片的几何（Card Position, Rotation 变化时调用）
     */
    public void refreshCardGeometry(NewModeBone bone, int cardIndex) {
        BoneCardRenderer renderer = boneRenderers.get(bone.getName());
        if (renderer != null) {
            renderer.refreshCardGeometry(cardIndex);
        }
    }

    /**
     * 刷新指定卡片的贴图
     */
    public void refreshCardTexture(NewModeBone bone, int cardIndex) {
        BoneCardRenderer renderer = boneRenderers.get(bone.getName());
        if (renderer != null) {
            renderer.refreshCardTexture(cardIndex);
        }
    }

    /**
     * 设置选中的卡片（显示高亮边框）
     */
    public void setSelectedCard(NewModeBone bone, int cardIndex) {
        // 先取消所有卡片的选中状态
        for (BoneCardRenderer renderer : boneRenderers.values()) {
            renderer.clearAllSelection();
        }

        // 设置指定卡片为选中状态
        BoneCardRenderer renderer = boneRenderers.get(bone.getName());
        if (renderer != null) {
            renderer.setCardSelected(cardIndex);
        }
    }

    // ========== Getters ==========

    public Node getPuppetNode() {
        return puppetNode;
    }

    public NewModeSkeleton getSkeleton() {
        return skeleton;
    }

    public boolean isInitialized() {
        return initialized;
    }

    /**
     * 单个骨骼的卡片环渲染器
     */
    private static class BoneCardRenderer {

        private final SimpleApplication app;
        private final NewModeBone bone;
        private final Node parentNode;
        private final Node boneNode;
        private final Node cardContainerNode; // 卡片容器节点，挂Billboard

        // 核心数学系统
        private EdgeLinkedCardRing cardRing;

        // 8张卡片的几何体
        private final List<CardGeometry> cards = new ArrayList<>(8);

        private boolean initialized = false;

        public BoneCardRenderer(SimpleApplication app, NewModeBone bone, Node parentNode) {
            this.app = app;
            this.bone = bone;
            this.parentNode = parentNode;
            this.boneNode = new Node(bone.getName() + "_Bone");
            this.cardContainerNode = new Node(bone.getName() + "_Cards");
        }

        public void initialize() {
            if (initialized) {
                return;
            }

            // 创建卡片环数学系统（使用骨骼级别的 ringRadius）
            cardRing = new EdgeLinkedCardRing(
                    8,  // 8张卡片
                    bone.getRingRadius(),
                    bone.isPerspective(),
                    bone.getFov(),
                    bone.getCameraZ()
            );

            // 给卡片容器节点添加Billboard控制（整体面向相机）
            com.jme3.scene.control.BillboardControl billboard =
                new com.jme3.scene.control.BillboardControl();
            billboard.setAlignment(com.jme3.scene.control.BillboardControl.Alignment.Screen);
            cardContainerNode.addControl(billboard);

            // 创建8张卡片几何体
            for (int i = 0; i < 8; i++) {
                CardGeometry card = new CardGeometry(app, i, bone);
                card.initialize();
                cards.add(card);
                cardContainerNode.attachChild(card.getGeometry());
                // 添加高亮边框到cardContainerNode（跟随卡片一起Billboard）
                if (card.getHighlightBorder() != null) {
                    cardContainerNode.attachChild(card.getHighlightBorder());
                }
            }

            // 层级结构：parentNode -> boneNode -> cardContainerNode -> cards
            boneNode.attachChild(cardContainerNode);
            parentNode.attachChild(boneNode);
            initialized = true;
        }

        public void update(Vector3f cameraPos, Vector3f cameraDir) {
            if (!initialized) {
                return;
            }

            // 计算骨骼的世界变换
            Vector3f worldPos = new Vector3f();
            Quaternion worldRot = new Quaternion();
            Vector3f worldScale = new Vector3f();
            bone.getWorldTransform(worldPos, worldRot, worldScale);

            // 设置骨骼节点的位置和旋转
            boneNode.setLocalTranslation(worldPos);
            boneNode.setLocalRotation(worldRot);

            // 计算相机相对于骨骼的偏航角
            // toCamera 是从骨骼指向相机的向量
            Vector3f toCamera = cameraPos.subtract(worldPos);
            if (toCamera.lengthSquared() < 0.0001f) {
                return; // 相机和骨骼重合
            }
            toCamera.normalizeLocal();

            // EdgeLinkedCardRing的约定：x=sin(angle), z=cos(angle)
            // 所以 angle = atan2(x, z)
            // 这里toCamera.x是相对于世界坐标的X，toCamera.z是相对于世界坐标的Z
            // 但我们需要相对于骨骼的局部坐标系

            // 将toCamera转换到骨骼的局部坐标系
            Quaternion invBoneRot = worldRot.inverse();
            Vector3f localToCamera = invBoneRot.mult(toCamera);

            // 计算相对偏航角（相对于骨骼的局部Z轴）
            float relativeYaw = (float) Math.atan2(localToCamera.x, localToCamera.z);

            // 求解卡片位置（输出的是相机对齐坐标系）
            List<EdgeLinkedCardRing.CardSpan> spans = cardRing.solve(relativeYaw);

            // 更新每张卡片的几何体
            // 因为cardContainerNode有BillboardControl，已经自动转向相机
            // EdgeLinkedCardRing的输出本身就是billboard前提下的布局
            for (EdgeLinkedCardRing.CardSpan span : spans) {
                if (span.index < cards.size()) {
                    CardGeometry card = cards.get(span.index);
                    card.update(span);
                }
            }
        }

        /**
         * 刷新几何（Ring Radius 变化时调用）
         */
        public void refreshGeometry() {
            if (!initialized) {
                return;
            }

            // 重新创建卡片环数学系统
            cardRing = new EdgeLinkedCardRing(
                    8,
                    bone.getRingRadius(),
                    bone.isPerspective(),
                    bone.getFov(),
                    bone.getCameraZ()
            );

            // 每张卡片的高度是独立的，在 CardGeometry.update() 中处理
        }

        /**
         * 刷新指定卡片的几何（位置、旋转、宽度、高度变化时调用）
         */
        public void refreshCardGeometry(int cardIndex) {
            if (!initialized || cardIndex < 0 || cardIndex >= cards.size()) {
                return;
            }

            CardGeometry card = cards.get(cardIndex);
            NewModeBone.CardData cardData = bone.getCard(cardIndex);

            if (card != null && cardData != null) {
                // 更新卡片高度（重新创建mesh）
                card.setHeight(cardData.height);
            }
        }

        /**
         * 刷新指定卡片的贴图
         */
        public void refreshCardTexture(int cardIndex) {
            if (!initialized || cardIndex < 0 || cardIndex >= cards.size()) {
                return;
            }
            cards.get(cardIndex).refreshTexture();
        }

        /**
         * 取消所有卡片的选中状态
         */
        public void clearAllSelection() {
            for (CardGeometry card : cards) {
                card.setSelected(false);
            }
        }

        /**
         * 设置指定卡片为选中状态
         */
        public void setCardSelected(int cardIndex) {
            if (!initialized || cardIndex < 0 || cardIndex >= cards.size()) {
                return;
            }
            cards.get(cardIndex).setSelected(true);
        }

        public void cleanup() {
            for (CardGeometry card : cards) {
                card.cleanup();
            }
            cards.clear();
            cardContainerNode.removeFromParent();
            boneNode.removeFromParent();
        }
    }

    /**
     * 单张卡片的几何体
     */
    private static class CardGeometry {

        private final SimpleApplication app;
        private final int index;
        private final NewModeBone bone;

        private Geometry geometry;
        private Material material;

        // 高亮边框
        private Geometry highlightBorder;
        private Material highlightMaterial;
        private boolean isSelected = false;

        public CardGeometry(SimpleApplication app, int index, NewModeBone bone) {
            this.app = app;
            this.index = index;
            this.bone = bone;
        }

        public void initialize() {
            // 获取卡片数据
            NewModeBone.CardData cardData = bone.getCard(index);
            float cardHeight = (cardData != null) ? cardData.height : 2.0f;

            // 创建初始mesh
            CenteredQuad quad = new CenteredQuad(1f, cardHeight);
            geometry = new Geometry("Card_" + index, quad);

            // 不添加BillboardControl！Billboard在父节点上

            // 创建材质
            material = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
            material.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
            material.setTransparent(true);
            material.setColor("Color", ColorRGBA.White);
            material.setFloat("AlphaDiscardThreshold", 0.1f);

            // 启用深度测试和深度写入，让Z-buffer正确处理遮挡
            material.getAdditionalRenderState().setDepthTest(true);
            material.getAdditionalRenderState().setDepthWrite(true);

            // 加载贴图
            String texturePath = bone.getCardTexture(index);
            if (texturePath != null && !texturePath.isEmpty()) {
                try {
                    Texture texture = app.getAssetManager().loadTexture(texturePath);
                    texture.setMagFilter(Texture.MagFilter.Nearest);
                    texture.setMinFilter(Texture.MinFilter.NearestNoMipMaps);
                    material.setTexture("ColorMap", texture);
                } catch (Exception e) {
                    // 贴图加载失败，使用颜色
                    material.setColor("Color", getDebugColor(index));
                }
            } else {
                // 没有贴图，使用调试颜色
                material.setColor("Color", getDebugColor(index));
            }

            geometry.setMaterial(material);
            geometry.setQueueBucket(RenderQueue.Bucket.Transparent);
            geometry.setShadowMode(RenderQueue.ShadowMode.Off);

            // 创建高亮边框
            createHighlight();
        }

        /**
         * 创建高亮边框（沿着卡片四周描边）
         */
        private void createHighlight() {
            // 获取卡片数据
            NewModeBone.CardData cardData = bone.getCard(index);
            float cardHeight = (cardData != null) ? cardData.height : 2.0f;

            float w = 0.5f; // 初始半宽
            float h = cardHeight / 2f; // 初始半高

            // 创建线框mesh（闭合的矩形）
            com.jme3.scene.Mesh lineMesh = new com.jme3.scene.Mesh();
            lineMesh.setMode(com.jme3.scene.Mesh.Mode.LineLoop);

            // 定义矩形的4个顶点（逆时针）
            float[] vertices = new float[] {
                -w, -h, 0,  // 左下
                 w, -h, 0,  // 右下
                 w,  h, 0,  // 右上
                -w,  h, 0   // 左上
            };

            // 索引（LineLoop会自动闭合）
            short[] indices = new short[] {0, 1, 2, 3};

            lineMesh.setBuffer(com.jme3.scene.VertexBuffer.Type.Position, 3, vertices);
            lineMesh.setBuffer(com.jme3.scene.VertexBuffer.Type.Index, 1, indices);
            lineMesh.updateBound();

            highlightBorder = new Geometry("CardHighlight_" + index, lineMesh);

            highlightMaterial = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
            highlightMaterial.setColor("Color", new ColorRGBA(1f, 1f, 0f, 1.0f)); // 黄色
            highlightMaterial.getAdditionalRenderState().setLineWidth(3f); // 线宽
            highlightMaterial.getAdditionalRenderState().setDepthWrite(false);
            highlightMaterial.getAdditionalRenderState().setDepthTest(false);

            highlightBorder.setMaterial(highlightMaterial);
            highlightBorder.setQueueBucket(RenderQueue.Bucket.Transparent);

            // 默认隐藏
            highlightBorder.setCullHint(com.jme3.scene.Spatial.CullHint.Always);
        }

        public void update(EdgeLinkedCardRing.CardSpan span) {
            // 不剔除任何卡片！即使背面的也显示
            // 让深度排序和Z-buffer自然处理遮挡关系
            geometry.setCullHint(Geometry.CullHint.Never);

            // EdgeLinkedCardRing输出的坐标已经是"billboard前提下"的布局
            // centerX是水平位置，depth是深度（正值=靠近相机）
            float centerX = (span.leftX + span.rightX) / 2f;

            // 获取卡片的独立属性
            NewModeBone.CardData card = bone.getCard(index);
            Vector3f cardOffset = new Vector3f(0, 0, 0);
            Quaternion cardRotation = new Quaternion();
            float cardWidth = 1.0f;
            float cardZOffset = 0f;

            if (card != null) {
                cardOffset = card.localPosition;
                cardZOffset = card.zOffset;
                cardWidth = card.width;

                // 将欧拉角转换为四元数
                cardRotation.fromAngles(
                    (float) Math.toRadians(card.rotationX),
                    (float) Math.toRadians(card.rotationY),
                    (float) Math.toRadians(card.rotationZ)
                );
            }

            // Z坐标用负depth，因为JME的Z轴正方向是"向外"
            // depth越大表示越靠近相机，所以要用负值
            // 再加上卡片独立的Z离心值（相对八棱柱中心）
            // 再加上卡片独立的位置偏移
            geometry.setLocalTranslation(
                centerX + cardOffset.x,
                cardOffset.y,
                -span.depth - cardZOffset + cardOffset.z
            );

            // 应用卡片独立的旋转
            geometry.setLocalRotation(cardRotation);

            // 更新宽度（span.width * 卡片独立的宽度系数）
            geometry.setLocalScale(span.width * cardWidth, 1f, 1f);

            // 使用用户数据存储深度，供渲染队列排序使用
            geometry.setUserData("depth", span.depth);

            // 同步更新高亮边框位置和缩放
            if (highlightBorder != null) {
                highlightBorder.setLocalTranslation(
                    centerX + cardOffset.x,
                    cardOffset.y,
                    -span.depth - cardZOffset + cardOffset.z + 0.001f  // 稍微靠前一点
                );
                highlightBorder.setLocalRotation(cardRotation);
                highlightBorder.setLocalScale(span.width * cardWidth, 1f, 1f);
            }
        }

        /**
         * 更新高亮边框大小（当卡片高度改变时）
         */
        private void updateHighlightBorderSize() {
            if (highlightBorder == null) {
                return;
            }

            // 获取卡片数据
            NewModeBone.CardData cardData = bone.getCard(index);
            float cardHeight = (cardData != null) ? cardData.height : 2.0f;

            float w = 0.5f; // 半宽
            float h = cardHeight / 2f; // 半高

            // 更新顶点位置
            float[] vertices = new float[] {
                -w, -h, 0,  // 左下
                 w, -h, 0,  // 右下
                 w,  h, 0,  // 右上
                -w,  h, 0   // 左上
            };

            highlightBorder.getMesh().setBuffer(com.jme3.scene.VertexBuffer.Type.Position, 3, vertices);
            highlightBorder.getMesh().updateBound();
            highlightBorder.getMesh().getBuffer(com.jme3.scene.VertexBuffer.Type.Position).setUpdateNeeded();
        }

        /**
         * 设置卡片高度（当卡片高度改变时调用）
         */
        public void setHeight(float height) {
            if (geometry != null) {
                CenteredQuad quad = new CenteredQuad(1f, height);
                geometry.setMesh(quad);
                // 同时更新边框大小
                updateHighlightBorderSize();
            }
        }

        /**
         * 设置是否选中（显示/隐藏高亮边框）
         */
        public void setSelected(boolean selected) {
            this.isSelected = selected;
            if (highlightBorder != null) {
                highlightBorder.setCullHint(selected ?
                    com.jme3.scene.Spatial.CullHint.Never :
                    com.jme3.scene.Spatial.CullHint.Always);
            }
        }

        /**
         * 获取高亮边框几何体
         */
        public Geometry getHighlightBorder() {
            return highlightBorder;
        }

        /**
         * 刷新贴图
         */
        public void refreshTexture() {
            if (material == null) {
                return;
            }

            String texturePath = bone.getCardTexture(index);
            if (texturePath != null && !texturePath.isEmpty()) {
                try {
                    Texture texture = app.getAssetManager().loadTexture(texturePath);
                    texture.setMagFilter(Texture.MagFilter.Nearest);
                    texture.setMinFilter(Texture.MinFilter.NearestNoMipMaps);
                    material.setTexture("ColorMap", texture);
                    material.setColor("Color", ColorRGBA.White);
                } catch (Exception e) {
                    // 贴图加载失败，使用调试颜色
                    material.clearParam("ColorMap");
                    material.setColor("Color", getDebugColor(index));
                }
            } else {
                // 没有贴图，使用调试颜色
                material.clearParam("ColorMap");
                material.setColor("Color", getDebugColor(index));
            }
        }

        public void cleanup() {
            if (geometry != null) {
                geometry.removeFromParent();
                geometry = null;
            }
        }

        public Geometry getGeometry() {
            return geometry;
        }

        /**
         * 获取调试颜色（当没有贴图时使用）
         */
        private ColorRGBA getDebugColor(int index) {
            ColorRGBA[] colors = {
                    new ColorRGBA(1, 0, 0, 1),     // 0: 红
                    new ColorRGBA(1, 0.5f, 0, 1),  // 1: 橙
                    new ColorRGBA(1, 1, 0, 1),     // 2: 黄
                    new ColorRGBA(0, 1, 0, 1),     // 3: 绿
                    new ColorRGBA(0, 1, 1, 1),     // 4: 青
                    new ColorRGBA(0, 0, 1, 1),     // 5: 蓝
                    new ColorRGBA(0.5f, 0, 1, 1),  // 6: 紫
                    new ColorRGBA(1, 0, 1, 1)      // 7: 品红
            };
            return colors[index % colors.length];
        }
    }

    /**
     * 居中的四边形mesh
     * 顶点顺序：逆时针（从正面看）= 法线朝外
     */
    private static class CenteredQuad extends com.jme3.scene.Mesh {
        public CenteredQuad(float width, float height) {
            float w = width / 2f;
            float h = height / 2f;

            // 顶点位置：逆时针顺序，从正面（+Z）看
            float[] positions = new float[]{
                    -w, -h, 0,  // 0: 左下
                    w, -h, 0,   // 1: 右下
                    w, h, 0,    // 2: 右上
                    -w, h, 0    // 3: 左上
            };
            setBuffer(com.jme3.scene.VertexBuffer.Type.Position, 3, positions);

            // UV坐标
            float[] texCoords = new float[]{
                    0, 0,  // 左下
                    1, 0,  // 右下
                    1, 1,  // 右上
                    0, 1   // 左上
            };
            setBuffer(com.jme3.scene.VertexBuffer.Type.TexCoord, 2, texCoords);

            // 法线：全部指向+Z（朝外）
            float[] normals = new float[]{
                    0, 0, 1,
                    0, 0, 1,
                    0, 0, 1,
                    0, 0, 1
            };
            setBuffer(com.jme3.scene.VertexBuffer.Type.Normal, 3, normals);

            // 索引：逆时针三角形
            short[] indices = new short[]{
                    0, 1, 2,  // 第一个三角形
                    0, 2, 3   // 第二个三角形
            };
            setBuffer(com.jme3.scene.VertexBuffer.Type.Index, 3, indices);

            updateBound();
        }
    }
}
