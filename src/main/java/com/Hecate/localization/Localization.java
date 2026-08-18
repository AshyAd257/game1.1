package com.Hecate.localization;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * 游戏文本本地化管理器。
 * <p>从 resources/localization/ 目录加载对应语言的 .properties 文件。
 * 使用方式：Localization.get("key.name")
 */
public class Localization {

    private static Language currentLanguage = Language.EN_US;
    private static Properties texts = new Properties();

    static {
        loadLanguage(currentLanguage);
    }

    /**
     * 切换语言并重新加载文本
     */
    public static void setLanguage(Language language) {
        currentLanguage = language;
        loadLanguage(language);
    }

    public static Language getCurrentLanguage() {
        return currentLanguage;
    }

    /**
     * 获取本地化文本
     * @param key 文本键名
     * @return 对应语言的文本，找不到则返回键名本身
     */
    public static String get(String key) {
        return texts.getProperty(key, key);
    }

    /**
     * 获取带参数替换的本地化文本
     * @param key 文本键名
     * @param args 替换参数，按 {0}, {1}, {2}... 顺序替换
     */
    public static String get(String key, Object... args) {
        String text = get(key);
        for (int i = 0; i < args.length; i++) {
            text = text.replace("{" + i + "}", String.valueOf(args[i]));
        }
        return text;
    }

    private static void loadLanguage(Language language) {
        texts.clear();
        String path = "/localization/" + language.code + ".properties";

        try (InputStream in = Localization.class.getResourceAsStream(path)) {
            if (in == null) {
                System.err.println("语言文件不存在: " + path);
                return;
            }
            // 使用 UTF-8 编码读取
            texts.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            System.out.println("已加载语言: " + language.displayName);
        } catch (IOException e) {
            System.err.println("加载语言文件失败: " + path);
            e.printStackTrace();
        }
    }
}
