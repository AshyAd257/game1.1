package com.Hecate.element;

import com.jme3.asset.AssetManager;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 元素地表装饰管理器
 *
 * 统一管理所有元素地表装饰的生命周期
 * 负责生成、更新、移除装饰物
 *
 * 性能优化：
 * - 限制最大装饰数量（先进先出）
 * - 批量更新减少遍历开销
 * - 自动清理过期装饰
 */
public class DecalManager {

    private final List<ElementalSurfaceDecal> decals;
    private final Node rootNode;
    private final AssetManager assetManager;

    // 最大装饰数量（防止性能问题）
    private static final int MAX_DECALS = 500;

    // 最小生成间隔（同一位置不会频繁生成）
    private static final float MIN_SPAWN_DISTANCE = 0.3f;

    /**
     * 构造函数
     *
     * @param rootNode 根节点
     * @param assetManager 资源管理器
     */
    public DecalManager(Node rootNode, AssetManager assetManager) {
        this.decals = new ArrayList<>();
        this.rootNode = rootNode;
        this.assetManager = assetManager;
    }

    /**
     * 生成装饰物
     *
     * 检查附近是否已有装饰物，避免过度密集
     *
     * @param element 元素类型
     * @param position 位置
     * @return 是否成功生成
     */
    public boolean spawnDecal(ElementType element, Vector3f position) {
        // 检查是否超过最大数量
        if (decals.size() >= MAX_DECALS) {
            // 移除最老的装饰物
            removeOldestDecal();
        }

        // 检查附近是否已有装饰物（防止过度密集）
        if (hasNearbyDecal(position, MIN_SPAWN_DISTANCE)) {
            return false;
        }

        // 创建新装饰物
        ElementalSurfaceDecal decal = new ElementalSurfaceDecal(
            element, position, rootNode, assetManager
        );
        decals.add(decal);

        return true;
    }

    /**
     * 检查附近是否已有装饰物
     *
     * @param position 检查位置
     * @param radius 检查半径
     * @return true 如果附近有装饰物
     */
    private boolean hasNearbyDecal(Vector3f position, float radius) {
        float radiusSquared = radius * radius;

        for (ElementalSurfaceDecal decal : decals) {
            float distSquared = decal.getPosition().distanceSquared(position);
            if (distSquared < radiusSquared) {
                return true;
            }
        }

        return false;
    }

    /**
     * 移除最老的装饰物
     */
    private void removeOldestDecal() {
        if (decals.isEmpty()) {
            return;
        }

        // 找到生命周期最短的（最老的）
        ElementalSurfaceDecal oldest = decals.get(0);
        for (ElementalSurfaceDecal decal : decals) {
            if (decal.getLifetime() < oldest.getLifetime()) {
                oldest = decal;
            }
        }

        oldest.remove();
        decals.remove(oldest);
    }

    /**
     * 更新所有装饰物
     *
     * @param tpf 时间增量（秒）
     */
    public void update(float tpf) {
        Iterator<ElementalSurfaceDecal> iterator = decals.iterator();

        while (iterator.hasNext()) {
            ElementalSurfaceDecal decal = iterator.next();

            // 更新装饰物，如果应该被移除则从列表中删除
            if (decal.update(tpf)) {
                iterator.remove();
            }
        }
    }

    /**
     * 清理所有装饰物
     */
    public void clear() {
        for (ElementalSurfaceDecal decal : decals) {
            decal.remove();
        }
        decals.clear();
    }

    /**
     * 清理指定区域内的装饰物
     *
     * @param center 中心位置
     * @param radius 清理半径
     * @return 清理的装饰物数量
     */
    public int clearInRadius(Vector3f center, float radius) {
        float radiusSquared = radius * radius;
        int count = 0;

        Iterator<ElementalSurfaceDecal> iterator = decals.iterator();
        while (iterator.hasNext()) {
            ElementalSurfaceDecal decal = iterator.next();

            float distSquared = decal.getPosition().distanceSquared(center);
            if (distSquared < radiusSquared) {
                decal.remove();
                iterator.remove();
                count++;
            }
        }

        return count;
    }

    /**
     * 清理指定元素的所有装饰物
     *
     * @param element 元素类型
     * @return 清理的装饰物数量
     */
    public int clearByElement(ElementType element) {
        int count = 0;

        Iterator<ElementalSurfaceDecal> iterator = decals.iterator();
        while (iterator.hasNext()) {
            ElementalSurfaceDecal decal = iterator.next();

            if (decal.getElement() == element) {
                decal.remove();
                iterator.remove();
                count++;
            }
        }

        return count;
    }

    /**
     * 获取指定区域内的装饰物数量
     *
     * @param center 中心位置
     * @param radius 检查半径
     * @return 装饰物数量
     */
    public int countInRadius(Vector3f center, float radius) {
        float radiusSquared = radius * radius;
        int count = 0;

        for (ElementalSurfaceDecal decal : decals) {
            float distSquared = decal.getPosition().distanceSquared(center);
            if (distSquared < radiusSquared) {
                count++;
            }
        }

        return count;
    }

    /**
     * 获取指定元素的装饰物数量
     *
     * @param element 元素类型
     * @return 装饰物数量
     */
    public int countByElement(ElementType element) {
        int count = 0;

        for (ElementalSurfaceDecal decal : decals) {
            if (decal.getElement() == element) {
                count++;
            }
        }

        return count;
    }

    // Getters

    /**
     * 获取当前装饰物数量
     * @return 装饰物数量
     */
    public int getDecalCount() {
        return decals.size();
    }

    /**
     * 获取最大装饰物数量
     * @return 最大数量
     */
    public int getMaxDecals() {
        return MAX_DECALS;
    }

    /**
     * 判断是否接近容量上限
     * @return true 如果装饰物数量超过80%
     */
    public boolean isNearCapacity() {
        return decals.size() > (MAX_DECALS * 0.8f);
    }

    /**
     * 获取所有装饰物（只读）
     * @return 装饰物列表的副本
     */
    public List<ElementalSurfaceDecal> getDecals() {
        return new ArrayList<>(decals);
    }
}
