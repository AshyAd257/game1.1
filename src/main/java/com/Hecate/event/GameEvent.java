package com.Hecate.event;

/**
 * 所有游戏事件的基类
 */
public abstract class GameEvent {
    private boolean cancelled = false;

    /**
     * 检查事件是否被取消
     */
    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * 设置事件是否被取消
     * 被取消的事件将不会继续传播
     */
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}