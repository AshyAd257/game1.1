package com.Hecate.ink;

import com.jme3.math.ColorRGBA;

import java.util.*;

/**
 * 阵营注册表
 * 管理所有阵营定义，提供查询接口
 */
public class FactionRegistry {

    // 特殊阵营ID
    public static final int NONE = 0; // 空地，无归属

    // 默认阵营ID（现在只有两个）
    public static final int LIGHT_DEFAULT = 1;  // 光属性默认阵营
    public static final int DARK_DEFAULT = 2;   // 暗属性默认阵营

    // 阵营定义表
    private final Map<Integer, FactionDef> factions;

    // 按血统分组的阵营列表
    private final Map<FactionDef.VisualLineage, List<FactionDef>> factionsByLineage;

    public FactionRegistry() {
        this.factions = new HashMap<>();
        this.factionsByLineage = new EnumMap<>(FactionDef.VisualLineage.class);

        // 初始化空列表
        for (FactionDef.VisualLineage lineage : FactionDef.VisualLineage.values()) {
            factionsByLineage.put(lineage, new ArrayList<>());
        }

        // 注册默认阵营
        registerDefaultFactions();
    }

    /**
     * 注册默认阵营
     */
    private void registerDefaultFactions() {
        // 光属性默认阵营（金黄色 #FFC700，发光）
        FactionDef lightFaction = new FactionDef(
            LIGHT_DEFAULT,
            FactionDef.VisualLineage.LIGHT,
            new ColorRGBA(1.0f, 0.78f, 0.0f, 1.0f), // 金黄色 #FFC700
            "Light Default"
        );
        register(lightFaction);

        // 暗属性默认阵营（橙色）
        FactionDef darkFaction = new FactionDef(
            DARK_DEFAULT,
            FactionDef.VisualLineage.DARK,
            new ColorRGBA(1.0f, 0.5f, 0.0f, 1.0f), // 橙色
            "Dark Default"
        );
        register(darkFaction);
    }

    /**
     * 注册阵营
     */
    public void register(FactionDef faction) {
        factions.put(faction.getFactionId(), faction);
        factionsByLineage.get(faction.getVisualLineage()).add(faction);
    }

    /**
     * 获取阵营定义
     * @param factionId 阵营ID
     * @return 阵营定义，如果不存在返回null
     */
    public FactionDef get(int factionId) {
        return factions.get(factionId);
    }

    /**
     * 获取指定血统下的所有阵营
     */
    public List<FactionDef> getFactionsByLineage(FactionDef.VisualLineage lineage) {
        return Collections.unmodifiableList(factionsByLineage.get(lineage));
    }

    /**
     * 为玩家分配阵营
     * 当前实现：直接返回该血统下的第一个阵营（因为现在只有一个）
     * 未来：可以改成随机抽取
     *
     * @param playerLineage 玩家血统
     * @return 分配的阵营
     */
    public FactionDef assignFaction(FactionDef.VisualLineage playerLineage) {
        List<FactionDef> available = factionsByLineage.get(playerLineage);
        if (available.isEmpty()) {
            throw new IllegalStateException("No factions available for lineage: " + playerLineage);
        }
        // 现在只有一个，直接返回；以后换成随机抽取
        return available.get(0);
    }

    /**
     * 判断两个阵营的关系
     */
    public Relation getRelation(int factionA, int factionB) {
        // 空地视为中立
        if (factionA == NONE || factionB == NONE) {
            return Relation.NEUTRAL;
        }

        // 相同阵营
        if (factionA == factionB) {
            return Relation.SELF;
        }

        // 现在简单粗暴：不同阵营就是敌人
        // 以后可以改成查关系表，支持同盟
        return Relation.ENEMY;
    }

    /**
     * 判断两个阵营是否同血统（视觉上是否同侧）
     */
    public boolean isSameLineage(int factionA, int factionB) {
        FactionDef defA = get(factionA);
        FactionDef defB = get(factionB);

        if (defA == null || defB == null) {
            return false;
        }

        return defA.getVisualLineage() == defB.getVisualLineage();
    }

    /**
     * 获取所有已注册的阵营
     */
    public Collection<FactionDef> getAllFactions() {
        return Collections.unmodifiableCollection(factions.values());
    }
}
