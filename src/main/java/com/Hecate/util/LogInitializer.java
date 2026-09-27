package com.Hecate.util;

import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 日志初始化工具
 * 配置日志系统，防止重复消息泛滥
 *
 * 使用方式：
 * 在Main类的开始处调用：
 * LogInitializer.initialize();
 */
public class LogInitializer {

    private static boolean initialized = false;
    private static DuplicateLogFilter duplicateFilter;

    /**
     * 初始化日志系统（使用默认配置）
     */
    public static void initialize() {
        initialize(Level.WARNING, true);
    }

    /**
     * 初始化日志系统（自定义配置）
     *
     * @param minLevel 最低日志级别
     * @param filterDuplicates 是否过滤重复日志
     */
    public static void initialize(Level minLevel, boolean filterDuplicates) {
        if (initialized) {
            return;
        }


        // 1. 配置JME3日志（最常见的重复日志来源）
        setupJME3Logging(minLevel, filterDuplicates);

        // 2. 配置根日志器
        setupRootLogger(minLevel, filterDuplicates);

        // 3. 配置其他常见的日志来源
        setupCommonLoggers(minLevel, filterDuplicates);

        initialized = true;
    }

    /**
     * 配置JME3相关日志
     */
    private static void setupJME3Logging(Level minLevel, boolean filterDuplicates) {
        // JME3核心
        Logger jme3Logger = Logger.getLogger("com.jme3");
        jme3Logger.setLevel(minLevel);

        // JME3资源加载器（最容易产生重复警告）
        Logger gltfLogger = Logger.getLogger("com.jme3.scene.plugins.gltf");
        gltfLogger.setLevel(minLevel);

        Logger objLogger = Logger.getLogger("com.jme3.scene.plugins.obj");
        objLogger.setLevel(minLevel);

        // JME3材质系统
        Logger materialLogger = Logger.getLogger("com.jme3.material");
        materialLogger.setLevel(minLevel);

        // JME3纹理系统
        Logger textureLogger = Logger.getLogger("com.jme3.texture");
        textureLogger.setLevel(minLevel);

        if (filterDuplicates) {
            duplicateFilter = new DuplicateLogFilter(1000, true);
            jme3Logger.setFilter(duplicateFilter);
            gltfLogger.setFilter(duplicateFilter);
            objLogger.setFilter(duplicateFilter);
            materialLogger.setFilter(duplicateFilter);
            textureLogger.setFilter(duplicateFilter);
        }
    }

    /**
     * 配置根日志器
     */
    private static void setupRootLogger(Level minLevel, boolean filterDuplicates) {
        Logger rootLogger = Logger.getLogger("");
        rootLogger.setLevel(minLevel);

        // 移除默认的ConsoleHandler，添加自定义的
        for (var handler : rootLogger.getHandlers()) {
            rootLogger.removeHandler(handler);
        }

        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(minLevel);
        if (filterDuplicates && duplicateFilter != null) {
            handler.setFilter(duplicateFilter);
        }
        rootLogger.addHandler(handler);
    }

    /**
     * 配置常见的日志来源
     */
    private static void setupCommonLoggers(Level minLevel, boolean filterDuplicates) {
        // 配置项目自己的日志
        Logger hecateLogger = Logger.getLogger("com.Hecate");
        hecateLogger.setLevel(Level.INFO); // 项目日志保持INFO级别

        if (filterDuplicates && duplicateFilter != null) {
            hecateLogger.setFilter(duplicateFilter);
        }
    }

    /**
     * 静默特定的日志消息
     * 用于完全屏蔽某些已知的、无害的重复警告
     *
     * @param loggerName 日志器名称
     * @param messagePattern 消息模式（正则表达式）
     */
    public static void silenceMessage(String loggerName, String messagePattern) {
        Logger logger = Logger.getLogger(loggerName);
        logger.setFilter(record -> {
            if (record.getMessage() != null && record.getMessage().matches(messagePattern)) {
                return false; // 过滤掉
            }
            return true; // 允许输出
        });
    }

    /**
     * 静默JME3的常见警告
     */
    public static void silenceCommonJME3Warnings() {
        // 静默GLTF动画插值警告（这个警告在每次加载动画时都会出现）
        silenceMessage("com.jme3.scene.plugins.gltf.GltfLoader",
            ".*only supports linear interpolation.*");

    }

    /**
     * 打印日志统计信息
     */
    public static void printStatistics() {
        if (duplicateFilter != null) {
            duplicateFilter.printStatistics();
        } else {
        }
    }

    /**
     * 重置日志过滤器
     */
    public static void reset() {
        if (duplicateFilter != null) {
            duplicateFilter.reset();
        }
        initialized = false;
    }

    /**
     * 设置日志级别（运行时调整）
     *
     * @param loggerName 日志器名称
     * @param level 日志级别
     */
    public static void setLogLevel(String loggerName, Level level) {
        Logger logger = Logger.getLogger(loggerName);
        logger.setLevel(level);
    }
}
