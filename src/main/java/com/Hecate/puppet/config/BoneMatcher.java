package com.Hecate.puppet.config;

import com.Hecate.puppet.core.Bone;
import java.util.*;

/**
 * 骨骼自动匹配算法
 * 提供多种策略来匹配动画骨骼到木偶骨骼
 */
public class BoneMatcher {

    /**
     * 匹配结果
     */
    public static class MatchResult {
        public enum Confidence {
            EXACT,      // 精确匹配（绿色）
            FUZZY,      // 模糊匹配（黄色）
            SEMANTIC,   // 语义匹配（橙色）
            HIERARCHY,  // 层级匹配（浅橙）
            NONE        // 无匹配（红色）
        }

        private String animBone;
        private String puppetBone;
        private Confidence confidence;
        private float score;  // 0.0-1.0

        public MatchResult(String animBone, String puppetBone, Confidence confidence, float score) {
            this.animBone = animBone;
            this.puppetBone = puppetBone;
            this.confidence = confidence;
            this.score = score;
        }

        public String getAnimBone() {
            return animBone;
        }

        public String getPuppetBone() {
            return puppetBone;
        }

        public Confidence getConfidence() {
            return confidence;
        }

        public float getScore() {
            return score;
        }

        @Override
        public String toString() {
            return String.format("%s -> %s [%s, %.2f]", animBone, puppetBone, confidence, score);
        }
    }

    // 语义词典：标准化不同命名风格
    private static final Map<String, String> SEMANTIC_DICT = new HashMap<>();
    static {
        // 左右
        SEMANTIC_DICT.put("left", "left");
        SEMANTIC_DICT.put("l", "left");
        SEMANTIC_DICT.put("left_", "left");
        SEMANTIC_DICT.put("_l", "left");

        SEMANTIC_DICT.put("right", "right");
        SEMANTIC_DICT.put("r", "right");
        SEMANTIC_DICT.put("right_", "right");
        SEMANTIC_DICT.put("_r", "right");

        // 部位
        SEMANTIC_DICT.put("arm", "arm");
        SEMANTIC_DICT.put("hand", "hand");
        SEMANTIC_DICT.put("leg", "leg");
        SEMANTIC_DICT.put("foot", "foot");
        SEMANTIC_DICT.put("head", "head");
        SEMANTIC_DICT.put("body", "body");
        SEMANTIC_DICT.put("torso", "body");
        SEMANTIC_DICT.put("chest", "body");

        // 上下
        SEMANTIC_DICT.put("upper", "upper");
        SEMANTIC_DICT.put("up", "upper");
        SEMANTIC_DICT.put("lower", "lower");
        SEMANTIC_DICT.put("low", "lower");
    }

    /**
     * 自动匹配所有骨骼
     */
    public static BoneMappingConfig autoMatch(Set<String> animBoneNames, List<Bone> puppetBones) {
        BoneMappingConfig mapping = new BoneMappingConfig();

        // 创建木偶骨骼名称集合
        Set<String> puppetBoneNames = new HashSet<>();
        for (Bone bone : puppetBones) {
            puppetBoneNames.add(bone.getName());
        }

        // 对每个动画骨骼尝试匹配
        for (String animBone : animBoneNames) {
            MatchResult best = findBestMatch(animBone, puppetBoneNames, puppetBones);
            if (best != null && best.getConfidence() != MatchResult.Confidence.NONE) {
                mapping.addMapping(animBone, best.getPuppetBone());
            }
        }

        return mapping;
    }

    /**
     * 为单个动画骨骼找到最佳匹配
     */
    public static MatchResult findBestMatch(String animBone, Set<String> puppetBoneNames, List<Bone> puppetBones) {
        List<MatchResult> candidates = new ArrayList<>();

        // 策略1: 精确匹配
        if (puppetBoneNames.contains(animBone)) {
            return new MatchResult(animBone, animBone, MatchResult.Confidence.EXACT, 1.0f);
        }

        // 策略2: 模糊匹配（忽略大小写、下划线、空格）
        String normalizedAnimBone = normalizeName(animBone);
        for (String puppetBone : puppetBoneNames) {
            if (normalizedAnimBone.equals(normalizeName(puppetBone))) {
                candidates.add(new MatchResult(animBone, puppetBone, MatchResult.Confidence.FUZZY, 0.9f));
            }
        }

        // 策略3: 语义匹配
        Set<String> animTokens = extractSemanticTokens(animBone);
        for (String puppetBone : puppetBoneNames) {
            Set<String> puppetTokens = extractSemanticTokens(puppetBone);
            float similarity = calculateTokenSimilarity(animTokens, puppetTokens);
            if (similarity > 0.5f) {
                candidates.add(new MatchResult(animBone, puppetBone, MatchResult.Confidence.SEMANTIC, similarity * 0.8f));
            }
        }

        // 策略4: 层级匹配（如果提供了层级信息）
        // TODO: 需要AnimationConfig中的层级信息

        // 选择得分最高的匹配
        if (!candidates.isEmpty()) {
            candidates.sort((a, b) -> Float.compare(b.getScore(), a.getScore()));
            return candidates.get(0);
        }

        return new MatchResult(animBone, null, MatchResult.Confidence.NONE, 0.0f);
    }

    /**
     * 标准化名称：小写 + 移除下划线和空格
     */
    private static String normalizeName(String name) {
        return name.toLowerCase()
                   .replace("_", "")
                   .replace(" ", "")
                   .replace("-", "");
    }

    /**
     * 提取语义标记
     */
    private static Set<String> extractSemanticTokens(String name) {
        Set<String> tokens = new HashSet<>();
        String normalized = name.toLowerCase();

        // 分割驼峰命名
        String[] parts = normalized.split("(?=[A-Z])|_| |-");

        for (String part : parts) {
            part = part.toLowerCase().trim();
            if (part.isEmpty()) continue;

            // 查找语义词典
            String semantic = SEMANTIC_DICT.get(part);
            if (semantic != null) {
                tokens.add(semantic);
            } else {
                tokens.add(part);
            }
        }

        return tokens;
    }

    /**
     * 计算标记相似度（Jaccard相似度）
     */
    private static float calculateTokenSimilarity(Set<String> tokens1, Set<String> tokens2) {
        if (tokens1.isEmpty() && tokens2.isEmpty()) {
            return 1.0f;
        }

        Set<String> intersection = new HashSet<>(tokens1);
        intersection.retainAll(tokens2);

        Set<String> union = new HashSet<>(tokens1);
        union.addAll(tokens2);

        return (float) intersection.size() / union.size();
    }

    /**
     * 获取所有可能的匹配（用于UI显示）
     */
    public static List<MatchResult> getAllPossibleMatches(String animBone, Set<String> puppetBoneNames, List<Bone> puppetBones) {
        List<MatchResult> results = new ArrayList<>();

        // 精确匹配
        if (puppetBoneNames.contains(animBone)) {
            results.add(new MatchResult(animBone, animBone, MatchResult.Confidence.EXACT, 1.0f));
            return results; // 精确匹配直接返回
        }

        // 模糊和语义匹配
        String normalizedAnimBone = normalizeName(animBone);
        Set<String> animTokens = extractSemanticTokens(animBone);

        for (String puppetBone : puppetBoneNames) {
            // 模糊匹配
            if (normalizedAnimBone.equals(normalizeName(puppetBone))) {
                results.add(new MatchResult(animBone, puppetBone, MatchResult.Confidence.FUZZY, 0.9f));
                continue;
            }

            // 语义匹配
            Set<String> puppetTokens = extractSemanticTokens(puppetBone);
            float similarity = calculateTokenSimilarity(animTokens, puppetTokens);
            if (similarity > 0.3f) {
                results.add(new MatchResult(animBone, puppetBone, MatchResult.Confidence.SEMANTIC, similarity * 0.8f));
            }
        }

        // 按得分排序
        results.sort((a, b) -> Float.compare(b.getScore(), a.getScore()));

        return results;
    }

    /**
     * 自动匹配所有骨骼（编辑器版本）
     * 接受 EditorBone 列表
     */
    public static BoneMappingConfig autoMatchEditorBones(Set<String> animBoneNames,
                                                         List<com.Hecate.puppet.editor.core.EditorBone> editorBones) {
        BoneMappingConfig mapping = new BoneMappingConfig();

        // 创建木偶骨骼名称集合
        Set<String> puppetBoneNames = new HashSet<>();
        for (com.Hecate.puppet.editor.core.EditorBone bone : editorBones) {
            puppetBoneNames.add(bone.getName());
        }

        // 对每个动画骨骼尝试匹配
        for (String animBone : animBoneNames) {
            // 使用名称集合进行匹配（不需要完整的Bone对象）
            MatchResult best = findBestMatchByName(animBone, puppetBoneNames);
            if (best != null && best.getConfidence() != MatchResult.Confidence.NONE) {
                mapping.addMapping(animBone, best.getPuppetBone());
            }
        }

        return mapping;
    }

    /**
     * 为单个动画骨骼找到最佳匹配（仅使用名称）
     */
    private static MatchResult findBestMatchByName(String animBone, Set<String> puppetBoneNames) {
        List<MatchResult> candidates = new ArrayList<>();

        // 策略1: 精确匹配
        if (puppetBoneNames.contains(animBone)) {
            return new MatchResult(animBone, animBone, MatchResult.Confidence.EXACT, 1.0f);
        }

        // 策略2: 模糊匹配（忽略大小写、下划线、空格）
        String normalizedAnimBone = normalizeName(animBone);
        for (String puppetBone : puppetBoneNames) {
            if (normalizedAnimBone.equals(normalizeName(puppetBone))) {
                candidates.add(new MatchResult(animBone, puppetBone, MatchResult.Confidence.FUZZY, 0.9f));
            }
        }

        // 策略3: 语义匹配
        Set<String> animTokens = extractSemanticTokens(animBone);
        for (String puppetBone : puppetBoneNames) {
            Set<String> puppetTokens = extractSemanticTokens(puppetBone);
            float similarity = calculateTokenSimilarity(animTokens, puppetTokens);
            if (similarity > 0.5f) {
                candidates.add(new MatchResult(animBone, puppetBone, MatchResult.Confidence.SEMANTIC, similarity * 0.8f));
            }
        }

        // 选择得分最高的匹配
        if (!candidates.isEmpty()) {
            candidates.sort((a, b) -> Float.compare(b.getScore(), a.getScore()));
            return candidates.get(0);
        }

        return new MatchResult(animBone, null, MatchResult.Confidence.NONE, 0.0f);
    }

    /**
     * 获取所有可能的匹配（用于UI显示，编辑器版本）
     */
    public static List<MatchResult> getAllPossibleMatchesForEditor(String animBone,
                                                                    Set<String> puppetBoneNames,
                                                                    List<com.Hecate.puppet.editor.core.EditorBone> editorBones) {
        // 实际上不需要使用 editorBones 参数，只需要名称集合
        return getAllPossibleMatches(animBone, puppetBoneNames, null);
    }
}
