package com.Hecate.ink;

/**
 * 阵营关系枚举
 * 统一的敌我判断入口
 */
public enum Relation {
    SELF,     // 自己
    ALLY,     // 盟友（暂未使用，预留）
    ENEMY,    // 敌人
    NEUTRAL   // 中立（如空地）
}
