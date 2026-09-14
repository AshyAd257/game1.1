package com.Hecate.puppet.animation2;

/**
 * 动画事件
 * 在动画播放到特定时间点时触发的事件
 *
 * 用途：
 * - 脚步声（在脚落地时播放音效）
 * - 武器挥舞声（在挥动到特定位置时播放音效）
 * - 特效触发（在特定帧触发粒子效果）
 * - 伤害判定（在攻击动画的特定帧触发伤害检测）
 */
public class AnimationEvent {

    private final float time;           // 触发时间（秒）
    private final String eventName;     // 事件名称
    private final Object parameter;     // 事件参数（可选）

    /**
     * 构造函数（无参数）
     *
     * @param time 触发时间（秒）
     * @param eventName 事件名称
     */
    public AnimationEvent(float time, String eventName) {
        this(time, eventName, null);
    }

    /**
     * 构造函数（带参数）
     *
     * @param time 触发时间（秒）
     * @param eventName 事件名称
     * @param parameter 事件参数
     */
    public AnimationEvent(float time, String eventName, Object parameter) {
        this.time = time;
        this.eventName = eventName;
        this.parameter = parameter;
    }

    public float getTime() {
        return time;
    }

    public String getEventName() {
        return eventName;
    }

    public Object getParameter() {
        return parameter;
    }

    public boolean hasParameter() {
        return parameter != null;
    }

    @Override
    public String toString() {
        return String.format("AnimationEvent[time=%.2fs, name=%s, param=%s]",
                time, eventName, parameter != null ? parameter.toString() : "null");
    }
}
