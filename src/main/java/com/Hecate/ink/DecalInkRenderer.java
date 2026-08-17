package com.Hecate.ink;

import com.jme3.asset.AssetManager;
import com.jme3.collision.CollisionResults;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Ray;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.BufferUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 基于Decal投影的涂墨渲染器
 *
 * 原理：
 * - 为每个有墨水的格子创建一个小quad
 * - 射线检测地形，获取精确的高度和法线
 * - quad贴合地形表面，自动适应斜面
 *
 * 优势：
 * - 完美贴合斜面和复杂地形
 * - 每个格子独立，不受region整体高度限制
 *
 * 性能：
 * - 使用批量mesh合并，减少draw call
 * - 只在dirty时重新构建mesh
 */
public class DecalInkRenderer {

    private final AssetManager assetManager;
    private final Node rootNode;
    private final SparseGridManager gridManager;
    private final ColorResolver colorResolver;
    private Node worldNode; // 用于射线检测的世界节点

    // 观察者信息
    private int observerFactionId = FactionRegistry.DARK_DEFAULT;
    private boolean observerInCombat = true;

    // 渲染节点
    private final Node inkNode;

    // Region渲染缓存
    private final Map<Long, RegionDecal> regionDecals;

    // 格子大小
    private static final float GRID_SIZE = SparseGridManager.GRID_SIZE;
    private static final float DECAL_HEIGHT_OFFSET = 0.02f; // 贴花略微抬高，避免z-fighting

    // 启用状态
    private boolean enabled = true;

    public DecalInkRenderer(AssetManager assetManager, Node rootNode, SparseGridManager gridManager) {
        this.assetManager = assetManager;
        this.rootNode = rootNode;
        this.gridManager = gridManager;
        this.colorResolver = new ColorResolver(gridManager.getFactionRegistry());

        this.inkNode = new Node("DecalInkNode");
        this.regionDecals = new HashMap<>();

        rootNode.attachChild(inkNode);
    }

    public void setWorldNode(Node worldNode) {
        this.worldNode = worldNode;
    }

    public void setObserverFactionId(int factionId) {
        this.observerFactionId = factionId;
        for (RegionDecal decal : regionDecals.values()) {
            decal.markDirty();
        }
    }

    public void setObserverInCombat(boolean inCombat) {
        this.observerInCombat = inCombat;
        for (RegionDecal decal : regionDecals.values()) {
            decal.markDirty();
        }
    }

    public void update() {
        if (!enabled || worldNode == null) {
            inkNode.detachAllChildren();
            regionDecals.clear();
            return;
        }

        // 获取所有有墨水的region
        java.util.Collection<GridRegion> allRegions = gridManager.getAllRegions();

        int updatedCount = 0; // 性能统计

        for (GridRegion region : allRegions) {
            if (!region.hasAnyInk()) {
                removeRegionDecal(region);
                continue;
            }

            long key = regionKey(region.getRegionX(), region.getRegionZ());
            RegionDecal decal = regionDecals.get(key);

            if (decal == null) {
                // 新region，创建decal
                decal = new RegionDecal(region);
                regionDecals.put(key, decal);
                updatedCount++;
            } else if (region.isDirty()) {
                // region数据变化，更新decal
                decal.update(region);
                region.clearDirty(); // 清除dirty标记，避免重复更新
                updatedCount++;
            }
        }

        // 性能调试信息（可选）
        // if (updatedCount > 0) {
        //     System.out.println("[DecalInk] 本帧更新了 " + updatedCount + " 个region");
        // }
    }

    private void removeRegionDecal(GridRegion region) {
        long key = regionKey(region.getRegionX(), region.getRegionZ());
        RegionDecal decal = regionDecals.remove(key);
        if (decal != null) {
            decal.cleanup();
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
            inkNode.detachAllChildren();
            regionDecals.clear();
        }
    }

    public void cleanup() {
        for (RegionDecal decal : regionDecals.values()) {
            decal.cleanup();
        }
        regionDecals.clear();
        inkNode.removeFromParent();
    }

    /**
     * Region贴花管理器
     */
    private class RegionDecal {
        private final int regionX;
        private final int regionZ;
        private Geometry geometry;
        private boolean dirty = true;

        // 缓存射线检测结果：格子坐标 -> 地形高度
        private final java.util.Map<Integer, Float> heightCache = new java.util.HashMap<>();

        public RegionDecal(GridRegion region) {
            this.regionX = region.getRegionX();
            this.regionZ = region.getRegionZ();
            createGeometry(region);
        }

        public boolean needsUpdate() {
            return dirty;
        }

        public void markDirty() {
            dirty = true;
            heightCache.clear(); // 清除高度缓存
        }

        public void update(GridRegion region) {
            if (geometry != null) {
                geometry.removeFromParent();
            }
            createGeometry(region);
            dirty = false;
        }

        private void createGeometry(GridRegion region) {
            // 构建合并的mesh（所有格子的quad合并成一个mesh）
            Mesh mesh = createDecalMesh(region);

            if (mesh == null) {
                return; // 没有可渲染的格子
            }

            geometry = new Geometry("RegionDecal_" + regionX + "_" + regionZ, mesh);

            // 创建材质
            Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
            mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
            mat.setTransparent(true);
            mat.getAdditionalRenderState().setDepthWrite(false);
            mat.getAdditionalRenderState().setDepthTest(true);
            mat.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);

            // 使用顶点颜色
            mat.setBoolean("VertexColor", true);

            geometry.setMaterial(mat);
            geometry.setQueueBucket(RenderQueue.Bucket.Transparent);

            inkNode.attachChild(geometry);
        }

        /**
         * 创建贴花mesh
         * 为每个有墨水的格子射线检测地形，生成贴合地形的quad
         */
        private Mesh createDecalMesh(GridRegion region) {
            java.util.List<CellQuad> quads = new java.util.ArrayList<>();

            Vector2f regionWorldMin = region.getWorldMin();

            // 遍历region内所有格子
            for (int localX = 0; localX < GridRegion.REGION_SIZE; localX++) {
                for (int localZ = 0; localZ < GridRegion.REGION_SIZE; localZ++) {
                    GridCell cell = region.getCell(localX, localZ);

                    if (cell == null || cell.isEmpty()) {
                        continue;
                    }

                    // 格子中心世界坐标
                    float worldX = regionWorldMin.x + (localX + 0.5f) * GRID_SIZE;
                    float worldZ = regionWorldMin.y + (localZ + 0.5f) * GRID_SIZE;

                    // 射线检测地形（带缓存）
                    Vector3f hitPoint = raycastTerrain(worldX, worldZ, localX, localZ);

                    if (hitPoint == null) {
                        continue; // 没有命中地形
                    }

                    // 计算格子颜色
                    ColorRGBA color = colorResolver.resolve(
                        cell.getFactionId(),
                        cell.getIntensity(),
                        observerFactionId,
                        observerInCombat
                    );

                    // 创建quad
                    CellQuad quad = createCellQuad(worldX, worldZ, hitPoint.y, color);
                    quads.add(quad);
                }
            }

            if (quads.isEmpty()) {
                return null;
            }

            // 合并所有quad到一个mesh
            return mergeQuads(quads);
        }

        /**
         * 射线检测地形高度（带缓存）
         */
        private Vector3f raycastTerrain(float worldX, float worldZ, int localX, int localZ) {
            // 生成缓存key
            int cacheKey = (localX << 16) | (localZ & 0xFFFF);

            // 检查缓存
            Float cachedHeight = heightCache.get(cacheKey);
            if (cachedHeight != null) {
                return new Vector3f(worldX, cachedHeight, worldZ);
            }

            // 缓存未命中，执行射线检测
            if (worldNode == null) {
                return null;
            }

            Vector3f rayStart = new Vector3f(worldX, 1000f, worldZ);
            Vector3f rayDir = new Vector3f(0, -1, 0);
            Ray ray = new Ray(rayStart, rayDir);

            CollisionResults results = new CollisionResults();
            worldNode.collideWith(ray, results);

            if (results.size() > 0) {
                float height = results.getClosestCollision().getContactPoint().y;
                heightCache.put(cacheKey, height); // 缓存结果
                return new Vector3f(worldX, height, worldZ);
            }

            return null;
        }

        /**
         * 为单个格子创建quad
         */
        private CellQuad createCellQuad(float centerX, float centerZ, float height, ColorRGBA color) {
            float halfSize = GRID_SIZE * 0.5f;
            float y = height + DECAL_HEIGHT_OFFSET;

            // 4个顶点（XZ平面上的正方形）
            Vector3f[] vertices = {
                new Vector3f(centerX - halfSize, y, centerZ - halfSize), // 左下
                new Vector3f(centerX + halfSize, y, centerZ - halfSize), // 右下
                new Vector3f(centerX + halfSize, y, centerZ + halfSize), // 右上
                new Vector3f(centerX - halfSize, y, centerZ + halfSize)  // 左上
            };

            return new CellQuad(vertices, color);
        }

        /**
         * 合并多个quad到一个mesh
         */
        private Mesh mergeQuads(java.util.List<CellQuad> quads) {
            int quadCount = quads.size();
            int vertexCount = quadCount * 4;
            int indexCount = quadCount * 6;

            float[] positions = new float[vertexCount * 3];
            float[] colors = new float[vertexCount * 4];
            int[] indices = new int[indexCount];

            int vIdx = 0;
            int cIdx = 0;
            int iIdx = 0;

            for (int q = 0; q < quadCount; q++) {
                CellQuad quad = quads.get(q);

                // 填充顶点位置
                for (Vector3f v : quad.vertices) {
                    positions[vIdx++] = v.x;
                    positions[vIdx++] = v.y;
                    positions[vIdx++] = v.z;
                }

                // 填充顶点颜色
                for (int i = 0; i < 4; i++) {
                    colors[cIdx++] = quad.color.r;
                    colors[cIdx++] = quad.color.g;
                    colors[cIdx++] = quad.color.b;
                    colors[cIdx++] = quad.color.a;
                }

                // 填充索引（两个三角形）
                int baseVertex = q * 4;
                indices[iIdx++] = baseVertex + 0;
                indices[iIdx++] = baseVertex + 1;
                indices[iIdx++] = baseVertex + 2;

                indices[iIdx++] = baseVertex + 0;
                indices[iIdx++] = baseVertex + 2;
                indices[iIdx++] = baseVertex + 3;
            }

            Mesh mesh = new Mesh();
            mesh.setBuffer(VertexBuffer.Type.Position, 3, BufferUtils.createFloatBuffer(positions));
            mesh.setBuffer(VertexBuffer.Type.Color, 4, BufferUtils.createFloatBuffer(colors));
            mesh.setBuffer(VertexBuffer.Type.Index, 3, BufferUtils.createIntBuffer(indices));
            mesh.updateBound();

            return mesh;
        }

        public void cleanup() {
            if (geometry != null) {
                geometry.removeFromParent();
            }
        }
    }

    /**
     * 单个格子的quad数据
     */
    private static class CellQuad {
        final Vector3f[] vertices;
        final ColorRGBA color;

        CellQuad(Vector3f[] vertices, ColorRGBA color) {
            this.vertices = vertices;
            this.color = color;
        }
    }
}