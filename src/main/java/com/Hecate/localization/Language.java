package com.Hecate.localization;

/**
 * 支持的语言列表
 */
public enum Language {
    EN_US("en_US", "English"),
    ZH_CN("zh_CN", "简体中文");

    public final String code;
    public final String displayName;

    Language(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public static Language fromCode(String code) {
        for (Language lang : values()) {
            if (lang.code.equals(code)) {
                return lang;
            }
        }
        return EN_US; // 默认英文
    }
}
