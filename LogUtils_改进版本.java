package com.Hecate.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 统一的日志管理工具 (使用SLF4J)
 */
public class LogUtils {
    private static final boolean DEBUG_ENABLED = true; // 可以通过配置文件控制

    public static void debug(Class<?> clazz, String message) {
        if (DEBUG_ENABLED) {
            Logger logger = LoggerFactory.getLogger(clazz);
            logger.debug(message);
        }
    }

    public static void info(Class<?> clazz, String message) {
        Logger logger = LoggerFactory.getLogger(clazz);
        logger.info(message);
    }

    public static void warning(Class<?> clazz, String message) {
        Logger logger = LoggerFactory.getLogger(clazz);
        logger.warn(message);
    }

    public static void error(Class<?> clazz, String message, Throwable throwable) {
        Logger logger = LoggerFactory.getLogger(clazz);
        logger.error(message, throwable);
    }

    public static void error(Class<?> clazz, String message) {
        Logger logger = LoggerFactory.getLogger(clazz);
        logger.error(message);
    }
}
