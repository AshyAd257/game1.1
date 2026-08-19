package com.Hecate.weapon;

import com.Hecate.localization.Localization;

/**
 * 武器种类（步枪/狙击枪/... /战锤）。
 * <p>每个种类可以对应多个具体变体（{@link WeaponDefinition} 实例），
 * 例如"步枪"下可以注册多把数值不同的具体步枪，都共享 {@code RIFLE} 这个 kind。
 * <p>本地化文案 key 规则与 {@link com.Hecate.player.BuffType} 保持一致：
 * {@code keyPrefix + ".name"} / {@code keyPrefix + ".desc"}。
 */
public enum WeaponKind {

    // ==================== 远程 ====================

    /** 步枪：中庸+涂墨，可选散射，一次发射数枚射弹，射速较快 */
    RIFLE(WeaponCategory.RANGED, "weapon.rifle"),

    /** 狙击枪：瞄准蓄力后发射单发高伤害子弹，可穿透，沿途涂墨 */
    SNIPER(WeaponCategory.RANGED, "weapon.sniper"),

    /** 加特林：蓄力后发射多枚高伤害子弹，射程越远伤害越高、子弹越慢 */
    GATLING(WeaponCategory.RANGED, "weapon.gatling"),

    /** 散弹枪：单次发射一片子弹，间隔略长，群攻，对单个小体型敌人伤害较低 */
    SHOTGUN(WeaponCategory.RANGED, "weapon.shotgun"),

    // ==================== 近战 ====================

    /** 迅捷剑：体积/射程/范围/伤害小，挥舞快，可弹反 */
    QUICK_SWORD(WeaponCategory.MELEE, "weapon.quick_sword"),

    /** 大剑：体积/射程/范围/伤害大，挥舞极慢，弹反后有一段时间的伤害格挡窗口 */
    GREATSWORD(WeaponCategory.MELEE, "weapon.greatsword"),

    /** 骑枪：蓄力后向前冲锋，无视减益地块，命中控制敌人，非冲锋时数值都很弱 */
    LANCE(WeaponCategory.MELEE, "weapon.lance"),

    /** 镰刀：蓄力后周身360度持续攻击甩墨，可弹开伤害，转完后有虚弱期，伤害较低但灵活 */
    SCYTHE(WeaponCategory.MELEE, "weapon.scythe"),

    /** 战锤：蓄力后砸向地面造成范围攻击，攻速慢，不蓄力命中伤害-50% */
    WARHAMMER(WeaponCategory.MELEE, "weapon.warhammer");

    private final WeaponCategory category;
    private final String keyPrefix;

    WeaponKind(WeaponCategory category, String keyPrefix) {
        this.category = category;
        this.keyPrefix = keyPrefix;
    }

    public WeaponCategory getCategory() {
        return category;
    }

    public boolean isRanged() {
        return category == WeaponCategory.RANGED;
    }

    public boolean isMelee() {
        return category == WeaponCategory.MELEE;
    }

    /**
     * 获取本地化的显示名称
     */
    public String getDisplayName() {
        return Localization.get(keyPrefix + ".name");
    }

    /**
     * 获取本地化的描述文本
     */
    public String getDescription() {
        return Localization.get(keyPrefix + ".desc");
    }
}
