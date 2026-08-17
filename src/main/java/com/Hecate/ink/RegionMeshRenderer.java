package com.Hecate.ink;

import com.jme3.app.SimpleApplication;
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
import java.util.HashMap;
import java.util.Map;

/**
 * 基于动态纹理的网格渲染器
 *
 * 性能优化方案：
 * - 1个 Region (16×16格子) = 1个 Geometry
 * - 颜色信息烘焙成 16×16 像素纹理
 * - 更新时只修改纹理像素，无需重建 Geometry
 *
 * 性能对比：
 * - 旧方案：256个 Geometry = 256次 Draw Call
 * - 新方案：1个 Geometry = 1次 Draw Call（256倍提升）
 */
public class RegionMeshRenderer {

    private final SimpleApplication app;
    private final AssetManager assetManager;
    private final Node rootNode;
    private final SparseGridManager gridManager;
    private final ColorResolver colorResolver;

    // 碰撞管理器（用于获取地形高度）
    private CollisionManager collisionManager;

    // 观察者阵营ID（默认为暗属性）
    private int observerFactionId = FactionRegistry.DARK_DEFAULT;

    // 观察者战斗状态
    private boolean observerInCombat = true;

    // 调试节点
    private final Node debugNode;

    // Region渲染缓存：regionKey -> RegionRenderer
    private final Map<Long, RegionRenderer> regionRenderers;

    // 光晕渲染器
    private final InkGlowRenderer glowRenderer;

    // 是否启用
    private boolean enabled = true;

    // 渲染高度偏移
    private static final float RENDER_HEIGHT_OFFSET = 0.22f;

    // 纹理分辨率（每个格子对应纹理的像素数）
    // 1 = 16×16像素，2 = 32×32像素（更精细）
    private static final int TEXTURE_SCALE = 1;
    private static final int TEXTURE_SIZE = GridRegion.REGION_SIZE * TEXTURE_SCALE;

    // 网格细分参数：sub×sub 网格，需要 (sub+1)² 个顶点
    private static final int MESH_SUBDIVISIONS = 8;

    public RegionMeshRenderer(SimpleApplication app, SparseGridManager gridManager) {
        this.app = app;
        this.assetManager = app.getAssetManager();
        this.rootNode = app.getRootNode();
        this.gridManager = gridManager;
        this.colorResolver = new ColorResolver(gridManager.getFactionRegistry());

        this.debugNode = new Node("RegionMeshDebug");
        this.regionRenderers = new HashMap<>();

        // 创建光晕渲染器
        this.glowRenderer = new InkGlowRenderer(assetManager, rootNode, gridManager);

        rootNode.attachChild(debugNode);
    }

    /**
     * 设置碰撞管理器
     */
    public void setCollisionManager(CollisionManager collisionManager) {
        this.collisionManager = collisionManager;
        glowRenderer.setCollisionManager(collisionManager);
    }

    /**
     * 设置观察者阵营ID
     */
    public void setObserverFactionId(int factionId) {
        this.observerFactionId = factionId;
        // 阵营变化时，标记所有 Region 需要刷新
        for (RegionRenderer renderer : regionRenderers.values()) {
            renderer.markDirty();
        }
        glowRenderer.setObserverFactionId(factionId);
    }

    /**
     * 设置观察者战斗状态
     */
    public void setObserverInCombat(boolean inCombat) {
        this.observerInCombat = inCombat;
        // 战斗状态变化时，标记所有 Region 需要刷新
        for (RegionRenderer renderer : regionRenderers.values()) {
            renderer.markDirty();
        }
        glowRenderer.setObserverInCombat(inCombat);
    }

    /**
     * 更新渲染
     */
    public void update() {
        if (!enabled) {
            debugNode.detachAllChildren();
            regionRenderers.clear();
            glowRenderer.setEnabled(false);
            return;
        }

        if (collisionManager == null) {
            return;
        }

        // 获取所有区域
        java.util.Collection<GridRegion> allRegions = gridManager.getAllRegions();

        // 更新或创建 Region 渲染器
        for (GridRegion region : allRegions) {
            if (!region.hasAnyInk()) {
                // 区域变空，移除渲染器
                removeRegionRenderer(region);
                continue;
            }

            long key = regionKey(region.getRegionX(), region.getRegionZ());
            RegionRenderer renderer = regionRenderers.get(key);

            if (renderer == null) {
                // 创建新的渲染器
                renderer = createRegionRenderer(region);
                regionRenderers.put(key, renderer);
            } else if (region.isDirty()) {
                // 区域脏了，更新纹理
                renderer.updateTexture(region);
                region.clearDirty();
            }
        }

        // 更新光晕渲染（复用相同的dirty机制）
        glowRenderer.update();
    }

    /**
     * 创建 Region 渲染器
     */
    private RegionRenderer createRegionRenderer(GridRegion region) {
        RegionRenderer renderer = new RegionRenderer(region);
        renderer.updateTexture(region);
        region.clearDirty();
        return renderer;
    }

    /**
     * 移除 Region 渲染器
     */
    private void removeRegionRenderer(GridRegion region) {
        long key = regionKey(region.getRegionX(), region.getRegionZ());
        RegionRenderer renderer = regionRenderers.remove(key);
        if (renderer != null) {
            renderer.cleanup();
        }
    }

    /**
     * 生成 Region 唯一键
     * 【修复】正确处理负数坐标，使用无符号掩码
     */
    private long regionKey(int regionX, int regionZ) {
        long rx = ((long)regionX) & 0xFFFFFFFFL;
        long rz = ((long)regionZ) & 0xFFFFFFFFL;
        return (rx << 32) | rz;
    }

    /**
     * 启用/禁用渲染
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        glowRenderer.setEnabled(enabled);
        if (!enabled) {
            debugNode.detachAllChildren();
            regionRenderers.clear();
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 清理资源
     */
    public void cleanup() {
        for (RegionRenderer renderer : regionRenderers.values()) {
            renderer.cleanup();
        }
        regionRenderers.clear();
        glowRenderer.cleanup();
        debugNode.removeFromParent();
    }

    /**
     * Region 渲染器
     * 管理单个 Region 的 Mesh + Texture
     */
    private class RegionRenderer {
        private final Geometry geometry;
        private final Texture2D texture;
        private final ByteBuffer textureBuffer;
        private final int regionX;
        private final int regionZ;
        private boolean dirty = false;

        // 缓存的网格顶点（只在创建时计算一次）
        private final float[] cachedVertexPositions;

        // 四个邻居Region的缓存（只在创建/更新时查询一次，不触发创建）
        private GridRegion neighborNorth; // z+1
        private GridRegion neighborSouth; // z-1
        private GridRegion neighborEast;  // x+1
        private GridRegion neighborWest;  // x-1

        public RegionRenderer(GridRegion region) {
            this.regionX = region.getRegionX();
            this.regionZ = region.getRegionZ();

            // 创建纹理
            textureBuffer = BufferUtils.createByteBuffer(TEXTURE_SIZE * TEXTURE_SIZE * 4); // RGBA
            Image image = new Image(Image.Format.RGBA8, TEXTURE_SIZE, TEXTURE_SIZE, textureBuffer, null, com.jme3.texture.image.ColorSpace.sRGB);
            texture = new Texture2D(image);
            texture.setMagFilter(Texture2D.MagFilter.Bilinear); // 双线性过滤，模糊边缘
            texture.setMinFilter(Texture2D.MinFilter.BilinearNoMipMaps);

            // 计算并缓存网格顶点高度
            this.cachedVertexPositions = calculateMeshVertices(region);

            // 创建平面 Mesh
            Mesh mesh = createRegionMesh();

            // 创建 Geometry
            geometry = new Geometry("Region_" + regionX + "_" + regionZ, mesh);

            // Region的左下角世界坐标（注意：不设置高度偏移，因为顶点Y已经包含地形高度）
            Vector2f worldMin = region.getWorldMin();
            geometry.setLocalTranslation(worldMin.x, 0, worldMin.y);

            // 创建材质
            Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
            mat.setTexture("ColorMap", texture);
            mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
            mat.setTransparent(true);
            mat.getAdditionalRenderState().setDepthWrite(false);
            mat.getAdditionalRenderState().setDepthTest(true);
            mat.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off); // 禁用背面剔除，双面渲染

            geometry.setMaterial(mat);
            geometry.setQueueBucket(RenderQueue.Bucket.Transparent);

            // 添加到场景
            debugNode.attachChild(geometry);
        }

        /**
         * 计算网格顶点位置（采样地形高度）
         * 网格：(sub+1) × (sub+1) 个顶点
         * @return float数组 [x0,y0,z0, x1,y1,z1, ...]
         */
        private float[] calculateMeshVertices(GridRegion region) {
            int vertexCount = (MESH_SUBDIVISIONS + 1) * (MESH_SUBDIVISIONS + 1);
            float[] positions = new float[vertexCount * 3];

            float regionSize = GridRegion.REGION_SIZE * SparseGridManager.GRID_SIZE;
            Vector2f worldMin = region.getWorldMin();

            int idx = 0;
            for (int iz = 0; iz <= MESH_SUBDIVISIONS; iz++) {
                for (int ix = 0; ix <= MESH_SUBDIVISIONS; ix++) {
                    // 顶点在Region内的相对位置 [0, regionSize]
                    float localX = (float)ix / MESH_SUBDIVISIONS * regionSize;
                    float localZ = (float)iz / MESH_SUBDIVISIONS * regionSize;

                    // 世界坐标
                    float worldX = worldMin.x + localX;
                    float worldZ = worldMin.y + localZ;

                    // 采样地形高度
                    float terrainHeight = getTerrainHeight(worldX, worldZ);
                    if (Float.isNaN(terrainHeight)) {
                        terrainHeight = 1.0f; // fallback
                    }

                    // 存储相对于Region原点的坐标（Geometry的translation会处理世界偏移）
                    positions[idx++] = localX;
                    positions[idx++] = terrainHeight + RENDER_HEIGHT_OFFSET;
                    positions[idx++] = localZ;
                }
            }

            return positions;
        }

        /**
         * 创建 Region 的网格 Mesh
         * 使用缓存的顶点位置
         */
        private Mesh createRegionMesh() {
            int vertexCount = (MESH_SUBDIVISIONS + 1) * (MESH_SUBDIVISIONS + 1);

            // UV 坐标
            float[] texCoords = new float[vertexCount * 2];
            int uvIdx = 0;
            for (int iz = 0; iz <= MESH_SUBDIVISIONS; iz++) {
                for (int ix = 0; ix <= MESH_SUBDIVISIONS; ix++) {
                    texCoords[uvIdx++] = (float)ix / MESH_SUBDIVISIONS;
                    texCoords[uvIdx++] = (float)iz / MESH_SUBDIVISIONS;
                }
            }

            // 法线（向上）
            float[] normals = new float[vertexCount * 3];
            for (int i = 0; i < vertexCount; i++) {
                normals[i * 3 + 0] = 0;
                normals[i * 3 + 1] = 1;
                normals[i * 3 + 2] = 0;
            }

            // 索引（生成 sub×sub 个格子，每个格子2个三角形）
            int quadCount = MESH_SUBDIVISIONS * MESH_SUBDIVISIONS;
            int[] indices = new int[quadCount * 6];
            int idxPos = 0;

            for (int iz = 0; iz < MESH_SUBDIVISIONS; iz++) {
                for (int ix = 0; ix < MESH_SUBDIVISIONS; ix++) {
                    int v0 = iz * (MESH_SUBDIVISIONS + 1) + ix;       // 左下
                    int v1 = v0 + 1;                                   // 右下
                    int v2 = v0 + (MESH_SUBDIVISIONS + 1);            // 左上
                    int v3 = v2 + 1;                                   // 右上

                    // 三角形1: v0, v1, v2
                    indices[idxPos++] = v0;
                    indices[idxPos++] = v1;
                    indices[idxPos++] = v2;

                    // 三角形2: v1, v3, v2
                    indices[idxPos++] = v1;
                    indices[idxPos++] = v3;
                    indices[idxPos++] = v2;
                }
            }

            Mesh mesh = new Mesh();
            mesh.setBuffer(VertexBuffer.Type.Position, 3, BufferUtils.createFloatBuffer(cachedVertexPositions));
            mesh.setBuffer(VertexBuffer.Type.TexCoord, 2, BufferUtils.createFloatBuffer(texCoords));
            mesh.setBuffer(VertexBuffer.Type.Normal, 3, BufferUtils.createFloatBuffer(normals));
            mesh.setBuffer(VertexBuffer.Type.Index, 3, BufferUtils.createIntBuffer(indices));
            mesh.updateBound();

            return mesh;
        }

        /**
         * 更新纹理（根据 GridRegion 的格子状态）
         */
        public void updateTexture(GridRegion region) {
            // 【优化】更新前重新查询邻居Region（只查询，不创建）
            cacheNeighborRegions();

            textureBuffer.rewind();

            for (int z = 0; z < GridRegion.REGION_SIZE; z++) {
                for (int x = 0; x < GridRegion.REGION_SIZE; x++) {
                    GridCell cell = region.getCell(x, z);
                    ColorRGBA color;

                    if (cell == null || cell.isEmpty() || cell.getIntensity() <= 0.001f) {
                        // 空格子：透明
                        color = new ColorRGBA(0, 0, 0, 0);
                    } else {
                        // 使用 ColorResolver 计算颜色
                        color = colorResolver.resolve(
                            cell.getFactionId(),
                            cell.getIntensity(),
                            observerFactionId,
                            observerInCombat
                        );

                        // 对光属性领地应用中心到边缘的渐变效果
                        color = applyLightFactionGradient(cell, x, z, color, region);
                    }

                    // 写入 RGBA（注意纹理坐标是从左下角开始）
                    for (int sy = 0; sy < TEXTURE_SCALE; sy++) {
                        for (int sx = 0; sx < TEXTURE_SCALE; sx++) {
                            textureBuffer.put((byte) (color.r * 255));
                            textureBuffer.put((byte) (color.g * 255));
                            textureBuffer.put((byte) (color.b * 255));
                            textureBuffer.put((byte) (color.a * 255));
                        }
                    }
                }
            }

            textureBuffer.rewind();

            // 通知 JME 纹理已更新
            texture.getImage().setData(textureBuffer);
        }

        /**
         * 缓存四个邻居Region的引用（只查询，不创建）
         */
        private void cacheNeighborRegions() {
            neighborNorth = gridManager.getRegionIfExists(regionX, regionZ + 1);
            neighborSouth = gridManager.getRegionIfExists(regionX, regionZ - 1);
            neighborEast  = gridManager.getRegionIfExists(regionX + 1, regionZ);
            neighborWest  = gridManager.getRegionIfExists(regionX - 1, regionZ);
        }

        /**
         * 快速获取格子（支持跨Region访问，包括对角线）
         * @param localX 当前Region的局部X坐标（可以是-1或16等越界值）
         * @param localZ 当前Region的局部Z坐标
         * @return GridCell 或 null
         */
        private GridCell getCellFast(int localX, int localZ, GridRegion currentRegion) {
            // 在当前Region范围内，直接数组访问
            if (localX >= 0 && localX < GridRegion.REGION_SIZE &&
                localZ >= 0 && localZ < GridRegion.REGION_SIZE) {
                return currentRegion.getCell(localX, localZ);
            }

            // 越界，确定目标Region和转换后的坐标
            GridRegion targetRegion = currentRegion;
            int targetX = localX;
            int targetZ = localZ;

            // X方向越界
            if (localX < 0) {
                targetRegion = neighborWest;
                targetX = GridRegion.REGION_SIZE + localX; // x=-1 -> x=15
            } else if (localX >= GridRegion.REGION_SIZE) {
                targetRegion = neighborEast;
                targetX = localX - GridRegion.REGION_SIZE; // x=16 -> x=0
            }

            // Z方向越界（可能在X越界基础上继续越界）
            if (localZ < 0) {
                // 需要查询南方邻居
                if (targetRegion == currentRegion) {
                    targetRegion = neighborSouth;
                } else if (targetRegion == neighborWest) {
                    // 西南对角线：需要查询西邻居的南邻居
                    targetRegion = (neighborWest != null)
                        ? gridManager.getRegionIfExists(neighborWest.getRegionX(), neighborWest.getRegionZ() - 1)
                        : null;
                } else if (targetRegion == neighborEast) {
                    // 东南对角线：需要查询东邻居的南邻居
                    targetRegion = (neighborEast != null)
                        ? gridManager.getRegionIfExists(neighborEast.getRegionX(), neighborEast.getRegionZ() - 1)
                        : null;
                }
                targetZ = GridRegion.REGION_SIZE + localZ; // z=-1 -> z=15
            } else if (localZ >= GridRegion.REGION_SIZE) {
                // 需要查询北方邻居
                if (targetRegion == currentRegion) {
                    targetRegion = neighborNorth;
                } else if (targetRegion == neighborWest) {
                    // 西北对角线：需要查询西邻居的北邻居
                    targetRegion = (neighborWest != null)
                        ? gridManager.getRegionIfExists(neighborWest.getRegionX(), neighborWest.getRegionZ() + 1)
                        : null;
                } else if (targetRegion == neighborEast) {
                    // 东北对角线：需要查询东邻居的北邻居
                    targetRegion = (neighborEast != null)
                        ? gridManager.getRegionIfExists(neighborEast.getRegionX(), neighborEast.getRegionZ() + 1)
                        : null;
                }
                targetZ = localZ - GridRegion.REGION_SIZE; // z=16 -> z=0
            }

            if (targetRegion == null) {
                return null; // 目标Region不存在
            }

            return targetRegion.getCell(targetX, targetZ);
        }

        /**
         * 为光属性领地应用中心到边缘的渐变效果
         * 中心：纯白色 #FFFFFF
         * 中间：金黄色 #FFC700（基础色）
         * 边缘：橘红色 #FF5C00（半透明）
         */
        private ColorRGBA applyLightFactionGradient(GridCell cell, int x, int z, ColorRGBA baseColor, GridRegion region) {
            FactionDef faction = gridManager.getFactionRegistry().get(cell.getFactionId());

            // 只对光属性领地应用此效果
            if (faction == null || faction.getVisualLineage() != FactionDef.VisualLineage.LIGHT) {
                return baseColor;
            }

            // 计算到区域边缘的距离（基于当前格子到最近空格的距离）
            float distanceToEdge = calculateDistanceToEdge(x, z, region);
            float centerRadius = GridRegion.REGION_SIZE / 4.0f; // 中心区域半径
            float edgeWidth = 2.0f; // 边缘渐变宽度

            ColorRGBA result = baseColor.clone();

            if (distanceToEdge <= edgeWidth) {
                // 边缘区域：从金黄色渐变到橘红色
                float edgeFactor = distanceToEdge / edgeWidth; // 0=最边缘, 1=远离边缘
                ColorRGBA edgeColor = new ColorRGBA(1.0f, 0.36f, 0.0f, 0.7f); // 橘红色 #FF5C00，半透明

                // 混合金黄色和橘红色
                result.r = edgeColor.r * (1 - edgeFactor) + baseColor.r * edgeFactor;
                result.g = edgeColor.g * (1 - edgeFactor) + baseColor.g * edgeFactor;
                result.b = edgeColor.b * (1 - edgeFactor) + baseColor.b * edgeFactor;
                result.a = edgeColor.a * (1 - edgeFactor) + baseColor.a * edgeFactor;
            } else {
                // 中心到中间区域：从纯白色渐变到金黄色
                float centerX = GridRegion.REGION_SIZE / 2.0f;
                float centerZ = GridRegion.REGION_SIZE / 2.0f;
                float distanceFromCenter = (float) Math.sqrt((x - centerX) * (x - centerX) + (z - centerZ) * (z - centerZ));

                if (distanceFromCenter < centerRadius) {
                    // 中心白色渐变
                    float centerFactor = distanceFromCenter / centerRadius; // 0=中心, 1=边缘
                    ColorRGBA centerColor = new ColorRGBA(1.0f, 1.0f, 1.0f, 1.0f); // 纯白色

                    result.r = centerColor.r * (1 - centerFactor) + baseColor.r * centerFactor;
                    result.g = centerColor.g * (1 - centerFactor) + baseColor.g * centerFactor;
                    result.b = centerColor.b * (1 - centerFactor) + baseColor.b * centerFactor;
                }
            }

            return result;
        }

        /**
         * 计算格子到最近边缘（空格）的距离
         * 【优化】使用缓存的邻居Region，支持跨Region查询
         */
        private float calculateDistanceToEdge(int x, int z, GridRegion region) {
            float minDistance = Float.MAX_VALUE;

            // 检查周围8个方向
            for (int dz = -1; dz <= 1; dz++) {
                for (int dx = -1; dx <= 1; dx++) {
                    if (dx == 0 && dz == 0) continue;

                    int checkX = x + dx;
                    int checkZ = z + dz;

                    // 【优化】使用快速访问器，自动处理跨Region情况
                    GridCell neighborCell = getCellFast(checkX, checkZ, region);

                    if (neighborCell == null || neighborCell.isEmpty()) {
                        float distance = (float) Math.sqrt(dx * dx + dz * dz);
                        minDistance = Math.min(minDistance, distance);
                    }
                }
            }

            // 如果周围都有格子，递归检查更远的距离（简化版：直接返回较大值）
            if (minDistance == Float.MAX_VALUE) {
                // 计算到区域边界的距离
                float distToBorder = Math.min(
                    Math.min(x, GridRegion.REGION_SIZE - 1 - x),
                    Math.min(z, GridRegion.REGION_SIZE - 1 - z)
                );

                return distToBorder + 2; // 偏移，避免边界效果
            }

            return minDistance;
        }

        public void markDirty() {
            dirty = true;
        }

        public void cleanup() {
            geometry.removeFromParent();
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
