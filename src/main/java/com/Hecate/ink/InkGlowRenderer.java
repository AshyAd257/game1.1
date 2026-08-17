package com.Hecate.ink;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.VertexBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;
import com.jme3.util.BufferUtils;
import com.Hecate.physics.CollisionManager;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * 涂墨边界光晕渲染器
 *
 * 设计原则：
 * - 每个边界格子独立生成一小片光晕quad
 * - 不追求闭合多边形，自然拼接形成光晕带
 * - 朝外方向由8邻域非己方格子平均向量决定
 * - 只在region dirty时重新计算，复用现有dirty机制
 *
 * 光晕效果：
 * - 纵向渐变（靠近格子实色，远离格子透明）
 * - 双方都有光晕（光属性白色光晕，暗属性橙色光晕）
 * - 强度随墨水强度衰减
 */
public class InkGlowRenderer {

    private final AssetManager assetManager;
    private final Node rootNode;
    private final SparseGridManager gridManager;
    private final ColorResolver colorResolver;
    private CollisionManager collisionManager;

    // 观察者信息
    private int observerFactionId = FactionRegistry.DARK_DEFAULT;
    private boolean observerInCombat = true;

    // 光晕节点
    private final Node glowNode;

    // Region光晕缓存
    private final java.util.Map<Long, RegionGlow> regionGlows;

    // 光晕参数
    private static final float GLOW_WIDTH = SparseGridManager.GRID_SIZE;  // 光晕宽度 = 格子大小
    private static final float GLOW_HEIGHT = 0.15f;  // 光晕向外延伸高度（米）
    private static final float RENDER_HEIGHT_OFFSET = 0.24f;  // 比涂墨略高，避免z-fighting

    // 启用状态
    private boolean enabled = true;

    public InkGlowRenderer(AssetManager assetManager, Node rootNode, SparseGridManager gridManager) {
        this.assetManager = assetManager;
        this.rootNode = rootNode;
        this.gridManager = gridManager;
        this.colorResolver = new ColorResolver(gridManager.getFactionRegistry());

        this.glowNode = new Node("InkGlowNode");
        this.regionGlows = new java.util.HashMap<>();

        rootNode.attachChild(glowNode);
    }

    public void setCollisionManager(CollisionManager collisionManager) {
        this.collisionManager = collisionManager;
    }

    public void setObserverFactionId(int factionId) {
        this.observerFactionId = factionId;
        // 标记所有region需要刷新
        for (RegionGlow glow : regionGlows.values()) {
            glow.markDirty();
        }
    }

    public void setObserverInCombat(boolean inCombat) {
        this.observerInCombat = inCombat;
        for (RegionGlow glow : regionGlows.values()) {
            glow.markDirty();
        }
    }

    /**
     * 更新光晕渲染（在RegionMeshRenderer.update()中调用）
     */
    public void update() {
        if (!enabled) {
            glowNode.detachAllChildren();
            regionGlows.clear();
            return;
        }

        if (collisionManager == null) {
            return;
        }

        // 获取所有有墨水的region
        java.util.Collection<GridRegion> allRegions = gridManager.getAllRegions();

        for (GridRegion region : allRegions) {
            if (!region.hasAnyInk()) {
                // 移除空region的光晕
                removeRegionGlow(region);
                continue;
            }

            long key = regionKey(region.getRegionX(), region.getRegionZ());
            RegionGlow glow = regionGlows.get(key);

            if (glow == null) {
                // 创建新的光晕
                glow = new RegionGlow(region);
                regionGlows.put(key, glow);
            } else if (region.isDirty() || glow.needsUpdate()) {
                // region脏了，更新光晕
                glow.update(region);
            }
        }
    }

    private void removeRegionGlow(GridRegion region) {
        long key = regionKey(region.getRegionX(), region.getRegionZ());
        RegionGlow glow = regionGlows.remove(key);
        if (glow != null) {
            glow.cleanup();
        }
    }

    private long regionKey(int regionX, int regionZ) {
        long rx = ((long)regionX) & 0xFFFFFFFFL;
        long rz = ((long)regionZ) & 0xFFFFFFFFL;
        return (rx << 32) | rz;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            glowNode.detachAllChildren();
            regionGlows.clear();
        }
    }

    public void cleanup() {
        for (RegionGlow glow : regionGlows.values()) {
            glow.cleanup();
        }
        regionGlows.clear();
        glowNode.removeFromParent();
    }

    /**
     * Region光晕管理器
     * 每个region的所有边界格子光晕作为一个整体mesh
     */
    private class RegionGlow {
        private final int regionX;
        private final int regionZ;
        private Geometry geometry;
        private boolean dirty = true;

        public RegionGlow(GridRegion region) {
            this.regionX = region.getRegionX();
            this.regionZ = region.getRegionZ();
            createGeometry(region);
        }

        public boolean needsUpdate() {
            return dirty;
        }

        public void markDirty() {
            dirty = true;
        }

        public void update(GridRegion region) {
            // 重新构建mesh
            if (geometry != null) {
                geometry.removeFromParent();
            }
            createGeometry(region);
            dirty = false;
        }

        private void createGeometry(GridRegion region) {
            // 获取边界格子
            List<GridRegion.BoundaryCell> boundaries = region.getBoundaries();

            if (boundaries.isEmpty()) {
                return;
            }

            // 构建合并的mesh
            Mesh mesh = createGlowMesh(region, boundaries);

            geometry = new Geometry("RegionGlow_" + regionX + "_" + regionZ, mesh);

            // 设置位置
            Vector2f worldMin = region.getWorldMin();
            float terrainHeight = getTerrainHeight(worldMin.x, worldMin.y);
            geometry.setLocalTranslation(worldMin.x, terrainHeight + RENDER_HEIGHT_OFFSET, worldMin.y);

            // 创建材质（使用纵向渐变纹理）
            Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
            mat.setTexture("ColorMap", createGlowTexture());
            mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
            mat.setTransparent(true);
            mat.getAdditionalRenderState().setDepthWrite(false);
            mat.getAdditionalRenderState().setDepthTest(true);
            mat.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);

            geometry.setMaterial(mat);
            geometry.setQueueBucket(RenderQueue.Bucket.Transparent);

            glowNode.attachChild(geometry);
        }

        /**
         * 创建光晕mesh（合并所有边界格子的quad）
         */
        private Mesh createGlowMesh(GridRegion region, List<GridRegion.BoundaryCell> boundaries) {
            int quadCount = boundaries.size();
            int vertexCount = quadCount * 4;
            int indexCount = quadCount * 6;

            float[] positions = new float[vertexCount * 3];
            float[] texCoords = new float[vertexCount * 2];
            float[] colors = new float[vertexCount * 4];  // 每个顶点的颜色
            int[] indices = new int[indexCount];

            int vIdx = 0;
            int tIdx = 0;
            int cIdx = 0;
            int iIdx = 0;
            int quadIdx = 0;

            for (GridRegion.BoundaryCell boundary : boundaries) {
                // 格子中心世界坐标（相对于region原点）
                float cellCenterX = (boundary.localX + 0.5f) * SparseGridManager.GRID_SIZE;
                float cellCenterZ = (boundary.localZ + 0.5f) * SparseGridManager.GRID_SIZE;

                // 朝外方向
                Vector3f outwardDir = boundary.outwardDir;

                // 计算quad的4个顶点
                // 内边缘（靠近格子中心）和外边缘（向外延伸GLOW_HEIGHT）
                Vector3f perpDir = new Vector3f(-outwardDir.z, 0, outwardDir.x); // 垂直于朝外方向

                float halfWidth = GLOW_WIDTH * 0.5f;

                // 内边缘两个点（不透明）
                Vector3f inner1 = new Vector3f(
                    cellCenterX - perpDir.x * halfWidth,
                    0,
                    cellCenterZ - perpDir.z * halfWidth
                );
                Vector3f inner2 = new Vector3f(
                    cellCenterX + perpDir.x * halfWidth,
                    0,
                    cellCenterZ + perpDir.z * halfWidth
                );

                // 外边缘两个点（透明）
                Vector3f outer1 = new Vector3f(
                    inner1.x + outwardDir.x * GLOW_HEIGHT,
                    0,
                    inner1.z + outwardDir.z * GLOW_HEIGHT
                );
                Vector3f outer2 = new Vector3f(
                    inner2.x + outwardDir.x * GLOW_HEIGHT,
                    0,
                    inner2.z + outwardDir.z * GLOW_HEIGHT
                );

                // 填充顶点位置（逆时针顺序）
                // 0: inner1, 1: inner2, 2: outer2, 3: outer1
                positions[vIdx++] = inner1.x; positions[vIdx++] = inner1.y; positions[vIdx++] = inner1.z;
                positions[vIdx++] = inner2.x; positions[vIdx++] = inner2.y; positions[vIdx++] = inner2.z;
                positions[vIdx++] = outer2.x; positions[vIdx++] = outer2.y; positions[vIdx++] = outer2.z;
                positions[vIdx++] = outer1.x; positions[vIdx++] = outer1.y; positions[vIdx++] = outer1.z;

                // UV坐标（纵向渐变纹理）
                // U: 横向（不重要），V: 0=内边缘（不透明），1=外边缘（透明）
                texCoords[tIdx++] = 0; texCoords[tIdx++] = 0; // inner1
                texCoords[tIdx++] = 1; texCoords[tIdx++] = 0; // inner2
                texCoords[tIdx++] = 1; texCoords[tIdx++] = 1; // outer2
                texCoords[tIdx++] = 0; texCoords[tIdx++] = 1; // outer1

                // 顶点颜色（使用ColorResolver计算）
                ColorRGBA glowColor = colorResolver.resolve(
                    boundary.factionId,
                    boundary.intensity,
                    observerFactionId,
                    observerInCombat
                );

                // 内边缘：不透明
                colors[cIdx++] = glowColor.r; colors[cIdx++] = glowColor.g;
                colors[cIdx++] = glowColor.b; colors[cIdx++] = glowColor.a * 0.8f; // 内边缘稍微透明
                colors[cIdx++] = glowColor.r; colors[cIdx++] = glowColor.g;
                colors[cIdx++] = glowColor.b; colors[cIdx++] = glowColor.a * 0.8f;

                // 外边缘：完全透明
                colors[cIdx++] = glowColor.r; colors[cIdx++] = glowColor.g;
                colors[cIdx++] = glowColor.b; colors[cIdx++] = 0.0f;
                colors[cIdx++] = glowColor.r; colors[cIdx++] = glowColor.g;
                colors[cIdx++] = glowColor.b; colors[cIdx++] = 0.0f;

                // 索引（两个三角形）
                int baseVertex = quadIdx * 4;
                indices[iIdx++] = baseVertex + 0;
                indices[iIdx++] = baseVertex + 1;
                indices[iIdx++] = baseVertex + 2;

                indices[iIdx++] = baseVertex + 0;
                indices[iIdx++] = baseVertex + 2;
                indices[iIdx++] = baseVertex + 3;

                quadIdx++;
            }

            Mesh mesh = new Mesh();
            mesh.setBuffer(VertexBuffer.Type.Position, 3, BufferUtils.createFloatBuffer(positions));
            mesh.setBuffer(VertexBuffer.Type.TexCoord, 2, BufferUtils.createFloatBuffer(texCoords));
            mesh.setBuffer(VertexBuffer.Type.Color, 4, BufferUtils.createFloatBuffer(colors));
            mesh.setBuffer(VertexBuffer.Type.Index, 3, BufferUtils.createIntBuffer(indices));
            mesh.updateBound();

            return mesh;
        }

        /**
         * 创建纵向渐变纹理（简单白色渐变，颜色由顶点色控制）
         */
        private Texture2D createGlowTexture() {
            int width = 2;
            int height = 64;
            ByteBuffer buffer = BufferUtils.createByteBuffer(width * height * 4);

            for (int y = 0; y < height; y++) {
                // V坐标：0=不透明，1=透明
                float alpha = 1.0f - ((float) y / (height - 1));

                byte alphaByte = (byte) (alpha * 255);

                for (int x = 0; x < width; x++) {
                    buffer.put((byte) 255);  // R
                    buffer.put((byte) 255);  // G
                    buffer.put((byte) 255);  // B
                    buffer.put(alphaByte);   // A
                }
            }

            buffer.rewind();

            Image image = new Image(Image.Format.RGBA8, width, height, buffer, null, com.jme3.texture.image.ColorSpace.Linear);
            Texture2D texture = new Texture2D(image);
            texture.setMagFilter(Texture2D.MagFilter.Bilinear);
            texture.setMinFilter(Texture2D.MinFilter.BilinearNoMipMaps);
            texture.setWrap(Texture2D.WrapMode.Clamp);

            return texture;
        }

        public void cleanup() {
            if (geometry != null) {
                geometry.removeFromParent();
            }
        }
    }

    private float getTerrainHeight(float worldX, float worldZ) {
        if (collisionManager == null) {
            return 1.0f;
        }

        float terrainHeight = collisionManager.getTerrainHeightAt(worldX, worldZ);

        if (!Float.isNaN(terrainHeight)) {
            return terrainHeight;
        }

        return 1.0f;
    }
}