package com.Hecate.core;

import java.util.Comparator;
import java.util.PriorityQueue;

/**
 * 游戏内时间调度器：管理"过一段时间后触发一次"或"每隔一段时间反复触发"的粗粒度任务。
 *
 * <p>用于植物生长、环境演化这类不需要逐帧精度的延迟/周期逻辑。与 {@link FixedTickScheduler}
 * 的区别：这里的时间源是每帧累积的游戏内时间（暂停/冻结时不流逝），粒度是"秒"级而不是
 * "50ms"级，任务数量可能很多但触发不频繁，用优先队列只处理已到期的任务，不逐个轮询。
 */
public class GameScheduler {

    private double gameTime = 0.0;

    private final PriorityQueue<ScheduledTask> queue =
            new PriorityQueue<>(Comparator.comparingDouble(t -> t.fireTime));

    /**
     * 每帧调用一次，推进游戏内时间并执行所有已到期的任务。
     * @param tpf 本帧的时间增量（秒）
     */
    public void update(float tpf) {
        gameTime += tpf;

        while (!queue.isEmpty() && queue.peek().fireTime <= gameTime) {
            ScheduledTask task = queue.poll();
            if (task.cancelled) {
                continue;
            }

            task.action.run();

            if (task.intervalIfRepeating > 0) {
                task.fireTime = gameTime + task.intervalIfRepeating;
                queue.add(task);
            }
        }
    }

    /**
     * 延迟 delaySeconds 秒后执行一次 action。
     * @return 任务句柄，可用于在触发前取消
     */
    public TaskHandle schedule(double delaySeconds, Runnable action) {
        ScheduledTask task = new ScheduledTask(gameTime + delaySeconds, -1, action);
        queue.add(task);
        return () -> task.cancelled = true;
    }

    /**
     * 每隔 intervalSeconds 秒重复执行一次 action（首次触发也在 intervalSeconds 后，不是立即执行）。
     * @return 任务句柄，可用于停止后续触发
     */
    public TaskHandle scheduleRepeating(double intervalSeconds, Runnable action) {
        ScheduledTask task = new ScheduledTask(gameTime + intervalSeconds, intervalSeconds, action);
        queue.add(task);
        return () -> task.cancelled = true;
    }

    /** 当前累积的游戏内时间（秒） */
    public double getGameTime() {
        return gameTime;
    }

    @FunctionalInterface
    public interface TaskHandle {
        void cancel();
    }

    private static final class ScheduledTask {
        double fireTime;
        final double intervalIfRepeating; // <=0 表示一次性任务
        final Runnable action;
        boolean cancelled = false;

        ScheduledTask(double fireTime, double intervalIfRepeating, Runnable action) {
            this.fireTime = fireTime;
            this.intervalIfRepeating = intervalIfRepeating;
            this.action = action;
        }
    }
}
