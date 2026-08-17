package com.Hecate.ink;

import com.Hecate.event.EventBus;
import com.Hecate.event.PaintEvent;

/**
 * 涂墨系统订阅者 - 订阅PaintEvent并处理涂墨
 *
 * 设计理念：
 * - 枪不知道涂墨系统的存在
 * - 子弹只发射PaintEvent到事件总线
 * - 涂墨系统在这里订阅PaintEvent，独立处理涂墨逻辑
 * - 彻底解耦：爆炸弹、弹跳弹、火焰弹都发射同一个事件
 *
 * 职责：
 * 1. 订阅PaintEvent
 * 2. 将事件参数转换为涂墨调用
 * 3. 处理涂墨强度、半径等参数
 */
public class InkSystemSubscriber {

    private final SparseGridManager gridManager;
    private final EventBus eventBus;

    /**
     * 构造函数
     * @param gridManager 涂墨网格管理器
     * @param eventBus 事件总线
     */
    public InkSystemSubscriber(SparseGridManager gridManager, EventBus eventBus) {
        this.gridManager = gridManager;
        this.eventBus = eventBus;

        // 订阅PaintEvent
        eventBus.subscribe(PaintEvent.class, this::onPaintEvent);
    }

    /**
     * 处理涂墨事件
     * @param event 涂墨事件
     */
    private void onPaintEvent(PaintEvent event) {
        // 获取事件参数
        float radius = event.getRadius();
        int factionId = event.getTeamId(); // 保持 PaintEvent 的 teamId 命名，但在这里视为 factionId
        float intensity = event.getIntensity();

        // 根据强度调整半径（蓄力倍率影响涂墨范围）
        float effectiveRadius = radius * intensity;

        // 调用涂墨系统
        gridManager.inkCircle(event.getPosition(), effectiveRadius, factionId);

        // 可选：打印调试信息
        // System.out.println("涂墨: " + event);
    }

    /**
     * 取消订阅（清理时调用）
     */
    public void unsubscribe() {
        eventBus.unsubscribe(PaintEvent.class, this::onPaintEvent);
    }
}
