package com.Hecate.item.world;

import com.Hecate.item.ItemDef;
import com.Hecate.item.ItemRegistry;
import com.Hecate.item.ItemStack;
import com.jme3.asset.AssetManager;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;

import java.util.ArrayList;
import java.util.List;

/**
 * 管理场景里当前所有掉落物实体（{@link WorldItemEntity}）的生成/查询/销毁，
 * 结构上仿照 {@link com.Hecate.monster.MonsterManager} 管理怪物列表的方式。
 * <p>本类不做任何抬升/抛物线运动——落地即静止是当前设计（见任务要求），生成时传入的
 * 位置就是最终位置。若未来要支持"从空中抛出后落地"，那是调用方（如丢弃逻辑）的职责：
 * 算出抛物线终点后再调用 {@link #spawn}，不需要改这个类。
 */
public class WorldItemManager {
    private final AssetManager assetManager;
    private final ItemRegistry itemRegistry;
    private final List<WorldItemEntity> entities = new ArrayList<>();

    public WorldItemManager(AssetManager assetManager, ItemRegistry itemRegistry) {
        this.assetManager = assetManager;
        this.itemRegistry = itemRegistry;
    }

    /**
     * 在指定位置生成一个掉落物实体。
     * @param parentNode 掉落物视觉表现挂载的场景节点（应为当前活动世界的节点，
     *                    与MonsterManager.spawnMonster的parentNode参数同一约定）
     * @param itemId 物品ID（必须已在itemRegistry注册，否则抛异常——调用方应先校验）
     * @param count 数量
     * @param position 落地位置
     * @return 新生成的掉落物实体
     */
    public WorldItemEntity spawn(Node parentNode, String itemId, int count, Vector3f position) {
        ItemDef def = itemRegistry.getItemDef(itemId);
        if (def == null) {
            throw new IllegalArgumentException("未知物品ID: " + itemId);
        }

        ItemVisual visual = new IconPlaneItemVisual(assetManager, def);
        parentNode.attachChild(visual.getSpatial());

        WorldItemEntity entity = new WorldItemEntity(new ItemStack(itemId, count), position, visual);
        entities.add(entity);
        return entity;
    }

    /**
     * 查找离指定位置最近的掉落物实体（用于交互键拾取——先射线/距离找到目标，再决定拾取哪个）。
     * @param position 查询位置（通常是玩家当前位置或射线命中点）
     * @param maxDistance 最大搜索半径（超出此距离的掉落物不参与本次查找）
     * @return 最近的掉落物实体；搜索半径内没有任何掉落物则返回null
     */
    public WorldItemEntity findNearest(Vector3f position, float maxDistance) {
        WorldItemEntity nearest = null;
        float nearestDistSq = maxDistance * maxDistance;

        for (WorldItemEntity entity : entities) {
            float distSq = entity.getPosition().distanceSquared(position);
            if (distSq <= nearestDistSq) {
                nearest = entity;
                nearestDistSq = distSq;
            }
        }
        return nearest;
    }

    /**
     * 移除并销毁一个掉落物实体（拾取成功后调用）。传入不在列表中的实体是no-op。
     */
    public void remove(WorldItemEntity entity) {
        if (entities.remove(entity)) {
            entity.dispose();
        }
    }

    /**
     * 清空所有掉落物（世界切换/退出清理时调用，与MonsterManager.clear同一约定）。
     */
    public void clear() {
        for (WorldItemEntity entity : entities) {
            entity.dispose();
        }
        entities.clear();
    }

    public int getEntityCount() {
        return entities.size();
    }
}
