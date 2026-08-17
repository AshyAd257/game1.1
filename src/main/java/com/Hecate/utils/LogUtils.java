package com.Hecate.utils;

import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * 统一的日志管理工具
 */
public class LogUtils {
    private static final boolean DEBUG_ENABLED = false; // 可以通过配置文件控制

    public static void debug(Class<?> clazz, String message) {
        if (DEBUG_ENABLED) {
            Logger.getLogger(clazz.getName()).log(Level.FINE, message);
        }
    }

    public static void info(Class<?> clazz, String message) {
        Logger.getLogger(clazz.getName()).log(Level.INFO, message);
    }

    public static void warning(Class<?> clazz, String message) {
        Logger.getLogger(clazz.getName()).log(Level.WARNING, message);
    }

    public static void error(Class<?> clazz, String message, Throwable throwable) {
        Logger.getLogger(clazz.getName()).log(Level.SEVERE, message, throwable);
    }

    public static void error(Class<?> clazz, String message) {
        Logger.getLogger(clazz.getName()).log(Level.SEVERE, message);
    }
}
