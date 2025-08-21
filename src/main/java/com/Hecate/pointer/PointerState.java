package com.Hecate.pointer;

/**
 * 🎯 指针状态枚举
 */
public enum PointerState {
    NORMAL,      // 普通十字线
    CHARGING,    // 武器蓄力中（动画效果）
    READY,       // 蓄力完成（特殊样式）
    COOLDOWN     // 冷却中（灰色或闪烁）
}

