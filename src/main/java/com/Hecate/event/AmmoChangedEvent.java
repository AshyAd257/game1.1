package com.Hecate.event;

/**
 * 弹药变化事件 - 玩家弹药消耗/恢复时发出，UI系统（PanelManager）订阅以更新枪械仪表盘的弹药条
 */
public class AmmoChangedEvent extends GameEvent {

    private final float ammoCurrent;
    private final float ammoMax;

    public AmmoChangedEvent(float ammoCurrent, float ammoMax) {
        super();
        this.ammoCurrent = ammoCurrent;
        this.ammoMax = ammoMax;
    }

    public float getAmmoCurrent() { return ammoCurrent; }
    public float getAmmoMax() { return ammoMax; }
}
