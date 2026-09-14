package com.Hecate.util;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Filter;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * 日志去重过滤器
 * 防止相同的日志消息重复输出成千上万次
 *
 * 使用方式：
 * Logger logger = Logger.getLogger("com.jme3");
 * logger.setFilter(new DuplicateLogFilter());
 */
public class DuplicateLogFilter implements Filter {

    // 存储已经输出过的日志消息
    private final Set<String> seenMessages = ConcurrentHashMap.newKeySet();

    // 消息计数器（用于显示重复次数）
    private final ConcurrentHashMap<String, Integer> messageCounts = new ConcurrentHashMap<>();

    // 每隔多少次重复才输出一次提示（例如：每1000次重复输出一次"已重复1000次"）
    private final int notifyInterval;

    // 是否在首次出现时就添加提示
    private final boolean showFirstOccurrence;

    /**
     * 构造函数 - 使用默认配置
     */
    public DuplicateLogFilter() {
        this(1000, true);
    }

    /**
     * 构造函数 - 自定义配置
     *
     * @param notifyInterval 每隔多少次重复才输出一次提示
     * @param showFirstOccurrence 是否显示首次出现
     */
    public DuplicateLogFilter(int notifyInterval, boolean showFirstOccurrence) {
        this.notifyInterval = notifyInterval;
        this.showFirstOccurrence = showFirstOccurrence;
    }

    @Override
    public boolean isLoggable(LogRecord record) {
        if (record == null) {
            return true;
        }

        // 生成消息的唯一标识（包含级别和消息内容）
        String messageKey = record.getLevel() + ":" + record.getMessage();

        // 如果是首次出现
        if (!seenMessages.contains(messageKey)) {
            seenMessages.add(messageKey);
            messageCounts.put(messageKey, 1);

            if (showFirstOccurrence) {
                // 修改消息，添加提示
                record.setMessage(record.getMessage() + " [首次，后续重复将被过滤]");
            }
            return true; // 允许输出
        }

        // 已经出现过，增加计数
        int count = messageCounts.compute(messageKey, (k, v) -> v == null ? 1 : v + 1);

        // 每隔指定次数输出一次提示
        if (count % notifyInterval == 0) {
            record.setMessage(String.format("[已过滤 %d 次重复] %s", count, record.getMessage()));
            return true; // 允许输出
        }

        // 其他情况：过滤掉
        return false;
    }

    /**
     * 清空已记录的消息（重置过滤器）
     */
    public void reset() {
        seenMessages.clear();
        messageCounts.clear();
    }

    /**
     * 获取某条消息的重复次数
     *
     * @param level 日志级别
     * @param message 消息内容
     * @return 重复次数
     */
    public int getRepeatCount(String level, String message) {
        String messageKey = level + ":" + message;
        return messageCounts.getOrDefault(messageKey, 0);
    }

    /**
     * 打印统计信息
     */
    public void printStatistics() {
        System.out.println("\n=== 日志统计 ===");
        System.out.println("不同消息数量: " + seenMessages.size());

        // 找出重复次数最多的前10条
        messageCounts.entrySet().stream()
            .filter(e -> e.getValue() > 1)
            .sorted((e1, e2) -> Integer.compare(e2.getValue(), e1.getValue()))
            .limit(10)
            .forEach(entry -> {
                String[] parts = entry.getKey().split(":", 2);
                String level = parts.length > 0 ? parts[0] : "UNKNOWN";
                String message = parts.length > 1 ? parts[1] : entry.getKey();
                System.out.printf("%s [%s] - 重复 %d 次%n",
                    level,
                    message.length() > 80 ? message.substring(0, 77) + "..." : message,
                    entry.getValue());
            });
        System.out.println();
    }
}
