package com.Hecate.event;

import com.Hecate.weapon.WeaponKind;

/**
 * 武器装备事件 - 玩家装备 Gun1/Gun2 时发出，UI系统（PanelManager）订阅以显示枪械仪表盘
 */
public class WeaponEquippedEvent extends GameEvent {

    private final WeaponKind kind;      // 装备的武器种类（可能为null，历史遗留武器无分类）
    private final float ammoCurrent;    // 当前弹药
    private final float ammoMax;        // 最大弹药

    public WeaponEquippedEvent(WeaponKind kind, float ammoCurrent, float ammoMax) {
        super();
        this.kind = kind;
        this.ammoCurrent = ammoCurrent;
        this.ammoMax = ammoMax;
    }

    public WeaponKind getKind() { return kind; }
    public float getAmmoCurrent() { return ammoCurrent; }
    public float getAmmoMax() { return ammoMax; }
}
