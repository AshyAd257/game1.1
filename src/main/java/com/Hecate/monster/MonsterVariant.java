package com.Hecate.monster;

/**
 * 怪物变体：在基础数值（{@code Monster.BASE_SIZE}/{@code BASE_MAX_HEALTH}/
 * {@code BASE_MOVE_SPEED}）上应用的倍率，用于波次系统区分"慢速怪/普通怪/小Boss"。
 */
public enum MonsterVariant {

    /** 慢速怪：体型/血量不变，移速明显更慢（用于第1波） */
    SLOW(1.0f, 1.0f, 0.6f),

    /** 普通怪：即当前默认数值（用于第2波，以及 /mob1 命令） */
    NORMAL(1.0f, 1.0f, 1.0f),

    /** 小Boss：体型翻倍、血量翻倍、移速略慢于普通怪（用于第3波） */
    MINI_BOSS(2.0f, 2.0f, 0.85f);

    public final float sizeMultiplier;
    public final float healthMultiplier;
    public final float speedMultiplier;

    MonsterVariant(float sizeMultiplier, float healthMultiplier, float speedMultiplier) {
        this.sizeMultiplier = sizeMultiplier;
        this.healthMultiplier = healthMultiplier;
        this.speedMultiplier = speedMultiplier;
    }
}
