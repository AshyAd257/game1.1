package com.Hecate.event;

/**
 * 武器卸下事件 - 玩家卸下当前手持武器时发出，UI系统（PanelManager）订阅以隐藏枪械仪表盘
 */
public class WeaponUnequippedEvent extends GameEvent {

    public WeaponUnequippedEvent() {
        super();
    }
}
