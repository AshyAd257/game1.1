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
import com.jme3.scene.Node;
import com.jme3.scene.shape.Quad;
import com.Hecate.physics.CollisionManager;

import java.util.HashMap;
import java.util.Map;

/**
 * 网格调试渲染器
 * 可视化显示地面网格状态
 */
public class GridDebugRenderer {

    private final SimpleApplication app;
    private final AssetManager assetManager;
    private final Node rootNode;
    private final SparseGridManager gridManager;

    // 碰撞管理器（用于获取地形高度）
    private CollisionManager collisionManager;

    // 颜色解析器（用于根据观察者阵营计算颜色）
    private final ColorResolver colorResolver;

    // 观察者阵营ID（默认为暗属性）
    // TODO: 应该从玩家控制器获取，现在暂时硬编码
    private int observerFactionId = FactionRegistry.DARK_DEFAULT;

    // 观察者战斗状态（默认为true，用于测试）
    // TODO: 应该从玩家控制器获取
    private boolean observerInCombat = true;

    // 调试节点
    private final Node debugNode;

    // 网格几何体缓存
    private final Map<Long, Geometry> gridGeometries;

    // 是否启用调试渲染
    private boolean enabled = true;

    // 渲染高度偏移（稍微高于地面，避免Z-fighting和地形遮挡）
    // 注意：FlameParticle也使用相同的偏移量，确保涂墨位置与渲染位置一致
    private static final float RENDER_HEIGHT_OFFSET = 0.22f; // 增加到0.22，避免被地形起伏遮挡

    public GridDebugRenderer(SimpleApplication app, SparseGridManager gridManager) {
        this.app = app;
        this.assetManager = app.getAssetManager();
        this.rootNode = app.getRootNode();
        this.gridManager = gridManager;

        // 初始化颜色解析器
        this.colorResolver = new ColorResolver(gridManager.getFactionRegistry());

        this.debugNode = new Node("GridDebug");
        this.gridGeometries = new HashMap<>();

        rootNode.attachChild(debugNode);
    }

    /**
     * 设置碰撞管理器（用于获取地形高度）
     */
    public void setCollisionManager(CollisionManager collisionManager) {
        this.collisionManager = collisionManager;
    }

    /**
     * 设置观察者阵营ID
     * TODO: 应该由PlayerController自动更新
     */
    public void setObserverFactionId(int factionId) {
        this.observerFactionId = factionId;
    }

    /**
     * 设置观察者战斗状态
     * TODO: 应该由PlayerController自动更新
     */
    public void setObserverInCombat(boolean inCombat) {
        this.observerInCombat = inCombat;
    }

    // 调试计数器
    private int updateCounter = 0;
    private int lastRegionCount = 0;

    // 已渲染的区域集合（用于检测新区域）
    private final java.util.Set<GridRegion> renderedRegions = new java.util.HashSet<>();

    // 强制刷新计数器（定期重新渲染所有区域，应对墨水衰减）
    private int forceRefreshCounter = 0;
    private static final int FORCE_REFRESH_INTERVAL = 300; // 每300帧（约5秒）强制刷新一次（降低频率以提升性能）

    /**
     * 更新渲染（修复版：定期刷新所有区域以应对墨水衰减）
     */
    public void update() {
        updateCounter++;
        forceRefreshCounter++;

        if (!enabled) {
            debugNode.detachAllChildren();
            gridGeometries.clear();
            renderedRegions.clear();
            return;
        }

        // 检查CollisionManager是否已设置
        if (collisionManager == null) {
            return;
        }

        // 获取所有区域
        java.util.Collection<GridRegion> allRegions = gridManager.getAllRegions();
        int regionCount = allRegions.size();

        // 每30帧更新所有区域的材质（应对墨水衰减）
        boolean forceRefresh = forceRefreshCounter >= FORCE_REFRESH_INTERVAL;
        if (forceRefresh) {
            forceRefreshCounter = 0;
        }

        // 检测并渲染新区域或需要刷新的区域
        int removedRegions = 0;
        int updatedRegions = 0;
        int newRegions = 0;
        int dirtyRegions = 0;
        int skippedRegions = 0; // 跳过的区域（既不是新区域，也不脏，也不在强制刷新时）

        for (GridRegion region : allRegions) {
            if (!region.hasAnyInk()) {
                // 区域变空了，移除其几何体
                removeRegionGeometries(region);
                this.renderedRegions.remove(region);
                removedRegions++;
                continue;
            }

            boolean isNewRegion = !this.renderedRegions.contains(region);
            boolean isDirty = region.isDirty();

            if (isNewRegion) {
                // 新区域：创建几何体
                renderRegion(region);
                this.renderedRegions.add(region);
                region.clearDirty();
                newRegions++;
            } else if (isDirty || forceRefresh) {
                // 已有区域且脏了，或者强制刷新：只更新材质
                updateRegionMaterials(region);
                region.clearDirty();
                updatedRegions++;
                if (isDirty) dirtyRegions++;
            } else {
                skippedRegions++;
            }
        }
    }

    /**
     * 更新单个区域的渲染（完全重建）
     */
    private void updateRegion(GridRegion region) {
        // 先移除旧的几何体
        removeRegionGeometries(region);

        // 重新渲染区域
        renderRegion(region);
    }

    /**
     * 更新单个区域的材质（不移除几何体，只更新颜色和透明度）
     */
    private void updateRegionMaterials(GridRegion region) {
        Vector2f worldMin = region.getWorldMin();
        int regionX = region.getRegionX();
        int regionZ = region.getRegionZ();

        for (int localX = 0; localX < GridRegion.REGION_SIZE; localX++) {
            for (int localZ = 0; localZ < GridRegion.REGION_SIZE; localZ++) {
                GridCell cell = region.getCell(localX, localZ);
                long key = gridKey(regionX, regionZ, localX, localZ);
                Geometry geom = gridGeometries.get(key);

                // cell为null或为空时，移除几何体
                if (cell != null && !cell.isEmpty() && cell.getIntensity() > 0.001f) {
                    // 格子有墨水且强度足够
                    if (geom != null) {
                        // 更新现有几何体的材质
                        Material mat = geom.getMaterial();
                        // 【阶段1】暂时直接从 FactionRegistry 获取基础色相
                        ColorRGBA color = getCellColor(cell);
                        mat.setColor("Color", color);
                    } else {
                        // 几何体不存在，创建新的
                        float worldX = worldMin.x + localX * SparseGridManager.GRID_SIZE;
                        float worldZ = worldMin.y + localZ * SparseGridManager.GRID_SIZE;
                        Geometry newGeom = createGridGeometry(worldX, worldZ, cell);
                        if (newGeom != null) {
                            debugNode.attachChild(newGeom);
                            gridGeometries.put(key, newGeom);
                        }
                    }
                } else {
                    // 格子为null或为空，移除几何体
                    if (geom != null) {
                        geom.removeFromParent();
                        gridGeometries.remove(key);
                    }
                }
            }
        }
    }

    /**
     * 移除区域的所有几何体
     */
    private void removeRegionGeometries(GridRegion region) {
        int regionX = region.getRegionX();
        int regionZ = region.getRegionZ();

        for (int localX = 0; localX < GridRegion.REGION_SIZE; localX++) {
            for (int localZ = 0; localZ < GridRegion.REGION_SIZE; localZ++) {
                long key = gridKey(regionX, regionZ, localX, localZ);
                Geometry geom = gridGeometries.remove(key);
                if (geom != null) {
                    geom.removeFromParent();
                }
            }
        }
    }

    /**
     * 渲染单个区域
     */
    private void renderRegion(GridRegion region) {
        Vector2f worldMin = region.getWorldMin();

        for (int localX = 0; localX < GridRegion.REGION_SIZE; localX++) {
            for (int localZ = 0; localZ < GridRegion.REGION_SIZE; localZ++) {
                GridCell cell = region.getCell(localX, localZ);
                if (cell == null || cell.isEmpty()) {
                    continue;
                }

                // 计算世界坐标
                float worldX = worldMin.x + localX * SparseGridManager.GRID_SIZE;
                float worldZ = worldMin.y + localZ * SparseGridManager.GRID_SIZE;

                // 创建网格几何体
                Geometry geom = createGridGeometry(worldX, worldZ, cell);
                if (geom != null) {
                    debugNode.attachChild(geom);

                    // 缓存几何体
                    long key = gridKey(region.getRegionX(), region.getRegionZ(), localX, localZ);
                    gridGeometries.put(key, geom);
                }
            }
        }
    }

    /**
     * 创建单个网格几何体
     */
    private Geometry createGridGeometry(float worldX, float worldZ, GridCell cell) {
        // 获取该位置的地形高度
        float terrainHeight = getTerrainHeight(worldX, worldZ);

        // 检查地形高度是否有效
        if (Float.isNaN(terrainHeight) || Float.isInfinite(terrainHeight)) {
            return null;
        }

        // 检查cell状态
        if (cell.getIntensity() <= 0.001f) {
            return null;
        }

        // 创建四边形
        Quad quad = new Quad(SparseGridManager.GRID_SIZE, SparseGridManager.GRID_SIZE);
        Geometry geom = new Geometry("GridCell", quad);

        // 计算基于坐标的微小高度偏移，避免重叠网格的Z-fighting
        // 使用坐标的哈希值生成0.0001~0.0002的偏移
        int coordHash = (int)(worldX * 1000) ^ (int)(worldZ * 1000);
        float microOffset = 0.0001f + (Math.abs(coordHash % 100) / 100000.0f);

        // 设置位置（在地形表面上方一点点，加上微小偏移）
        float finalY = terrainHeight + RENDER_HEIGHT_OFFSET + microOffset;
        geom.setLocalTranslation(worldX, finalY, worldZ);

        // 旋转90度，让四边形平铺在地面上
        geom.rotate((float)Math.toRadians(-90), 0, 0);

        // 创建材质
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");

        // 根据阵营设置颜色
        ColorRGBA color = getCellColor(cell);
        mat.setColor("Color", color);

        // 使用标准Alpha混合
        mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        mat.setTransparent(true);
        mat.getAdditionalRenderState().setDepthWrite(false);
        mat.getAdditionalRenderState().setDepthTest(true);

        geom.setMaterial(mat);
        geom.setQueueBucket(RenderQueue.Bucket.Transparent);
        geom.updateModelBound();

        return geom;
    }

    /**
     * 获取格子的颜色
     * 使用 ColorResolver 根据观察者阵营动态计算颜色
     */
    private ColorRGBA getCellColor(GridCell cell) {
        // 使用 ColorResolver 计算最终颜色
        ColorRGBA color = colorResolver.resolve(
            cell.getFactionId(),
            cell.getIntensity(),
            observerFactionId,
            observerInCombat
        );

        return color;
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

    /**
     * 生成网格唯一键
     * 【修复6】正确处理负数坐标，避免位移操作导致的键值错误
     */
    private long gridKey(int regionX, int regionZ, int localX, int localZ) {
        // 将坐标转换为无符号值，避免负数导致的位移问题
        // 支持-32768到32767范围内的坐标
        long rx = (long)regionX & 0xFFFF;  // 取低16位
        long rz = (long)regionZ & 0xFFFF;  // 取低16位
        long lx = (long)localX & 0xFFFF;   // 取低16位
        long lz = (long)localZ & 0xFFFF;   // 取低16位
        return (rx << 48) | (rz << 32) | (lx << 16) | lz;
    }

    /**
     * 启用/禁用调试渲染
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            debugNode.detachAllChildren();
            gridGeometries.clear();
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 清理资源
     */
    public void cleanup() {
        debugNode.removeFromParent();
        gridGeometries.clear();
    }
}
