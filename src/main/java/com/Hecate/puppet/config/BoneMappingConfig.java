package com.Hecate.puppet.config;

import java.util.*;

/**
 * 骨骼映射配置
 * 用于保存和加载动画骨骼到木偶骨骼的映射关系
 */
public class BoneMappingConfig {

    private String animationName;       // 动画名称
    private String puppetName;          // 木偶名称
    private Map<String, String> mapping; // 映射表：动画骨骼名 -> 木偶骨骼名

    public BoneMappingConfig() {
        this.mapping = new HashMap<>();
    }

    public BoneMappingConfig(String animationName, String puppetName) {
        this();
        this.animationName = animationName;
        this.puppetName = puppetName;
    }

    /**
     * 添加映射
     */
    public void addMapping(String animBoneName, String puppetBoneName) {
        mapping.put(animBoneName, puppetBoneName);
    }

    /**
     * 移除映射
     */
    public void removeMapping(String animBoneName) {
        mapping.remove(animBoneName);
    }

    /**
     * 获取映射的目标骨骼名
     */
    public String getMappedBoneName(String animBoneName) {
        return mapping.get(animBoneName);
    }

    /**
     * 检查是否有映射
     */
    public boolean hasMapping(String animBoneName) {
        return mapping.containsKey(animBoneName);
    }

    /**
     * 获取未映射的骨骼列表
     */
    public Set<String> getUnmappedBones(Set<String> animBoneNames) {
        Set<String> unmapped = new HashSet<>(animBoneNames);
        unmapped.removeAll(mapping.keySet());
        return unmapped;
    }

    /**
     * 获取映射覆盖率（百分比）
     */
    public float getMappingCoverage(Set<String> animBoneNames) {
        if (animBoneNames.isEmpty()) {
            return 100f;
        }
        int mappedCount = 0;
        for (String animBone : animBoneNames) {
            if (hasMapping(animBone)) {
                mappedCount++;
            }
        }
        return (float) mappedCount / animBoneNames.size() * 100f;
    }

    /**
     * 清空所有映射
     */
    public void clear() {
        mapping.clear();
    }

    // ========== Getters and Setters ==========

    public String getAnimationName() {
        return animationName;
    }

    public void setAnimationName(String animationName) {
        this.animationName = animationName;
    }

    public String getPuppetName() {
        return puppetName;
    }

    public void setPuppetName(String puppetName) {
        this.puppetName = puppetName;
    }

    public Map<String, String> getMapping() {
        return mapping;
    }

    public void setMapping(Map<String, String> mapping) {
        this.mapping = mapping;
    }

    @Override
    public String toString() {
        return String.format("BoneMappingConfig[anim=%s, puppet=%s, mappings=%d]",
                animationName, puppetName, mapping.size());
    }
}
