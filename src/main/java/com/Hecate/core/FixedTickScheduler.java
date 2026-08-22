package com.Hecate.core;

import java.util.ArrayList;
import java.util.List;

/**
 * 固定逻辑刻调度器：把可变帧率的 tpf 转换为固定步长（默认20Hz/50ms）的调用序列。
 *
 * <p>用于战斗相关的判定逻辑（怪物AI决策、攻击/技能冷却到期判断、buff结算），
 * 使其在任意帧率下的判定时序保持一致，不受渲染帧率波动影响。
 *
 * <p>怪物移动插值、受击闪白等纯视觉表现仍然走原有的 update(tpf)，不受此调度器影响。
 */
public class FixedTickScheduler {

    /** 固定步长（秒）。20Hz = 50ms一次。 */
    public static final float FIXED_DT = 1f / 20f;

    /** 单帧最多补跑几次固定步，避免卡顿/断点后疯狂追帧导致死亡螺旋。 */
    private static final int MAX_STEPS_PER_FRAME = 5;

    private final List<FixedTickListener> listeners = new ArrayList<>();
    private float accumulator = 0f;

    public interface FixedTickListener {
        /** @param dt 固定步长时间（秒），恒等于 {@link #FIXED_DT} */
        void onFixedTick(float dt);
    }

    public void register(FixedTickListener listener) {
        listeners.add(listener);
    }

    public void unregister(FixedTickListener listener) {
        listeners.remove(listener);
    }

    /**
     * 每帧调用一次，内部按累积时间跑0~N次固定步。
     * @param tpf 本帧的可变帧时间（秒）
     */
    public void update(float tpf) {
        accumulator += tpf;

        int steps = 0;
        while (accumulator >= FIXED_DT && steps < MAX_STEPS_PER_FRAME) {
            for (FixedTickListener listener : listeners) {
                listener.onFixedTick(FIXED_DT);
            }
            accumulator -= FIXED_DT;
            steps++;
        }

        if (steps == MAX_STEPS_PER_FRAME) {
            // 长时间卡顿后丢弃剩余的累积时间，避免下一帧继续疯狂追帧
            accumulator = 0f;
        }
    }
}
