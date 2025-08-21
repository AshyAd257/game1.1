package com.Hecate.event;

import com.Hecate.module.GameModule;

/**
 * 当模块被加载时触发的事件
 */
public class ModuleLoadedEvent extends GameEvent {
    private final GameModule module;

    public ModuleLoadedEvent(GameModule module) {
        this.module = module;
    }

    /**
     * 获取被加载的模块
     */
    public GameModule getModule() {
        return module;
    }
}