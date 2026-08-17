package com.Hecate.puppet.animation;

import java.util.*;

/**
 * 动画片段
 * 管理一组关键帧，支持播放和采样
 */
public class AnimationClip {

    private String name;
    private float duration;  // 总时长（秒）
    private boolean looping; // 是否循环播放

    // 按骨骼名称组织的关键帧
    // Map<骨骼名称, List<关键帧>>
    private Map<String, List<Keyframe>> keyframesByBone;

    public AnimationClip(String name) {
        this.name = name;
        this.duration = 0f;
        this.looping = true;
        this.keyframesByBone = new HashMap<>();
    }

    /**
     * 添加关键帧
     */
    public void addKeyframe(Keyframe keyframe) {
        String boneName = keyframe.getBoneName();

        // 获取或创建该骨骼的关键帧列表
        List<Keyframe> boneKeyframes = keyframesByBone.get(boneName);
        if (boneKeyframes == null) {
            boneKeyframes = new ArrayList<>();
            keyframesByBone.put(boneName, boneKeyframes);
        }

        // 插入关键帧（保持时间顺序）
        int insertIndex = 0;
        for (int i = 0; i < boneKeyframes.size(); i++) {
            if (boneKeyframes.get(i).getTime() > keyframe.getTime()) {
                insertIndex = i;
                break;
            }
            insertIndex = i + 1;
        }
        boneKeyframes.add(insertIndex, keyframe);

        // 更新总时长
        if (keyframe.getTime() > duration) {
            duration = keyframe.getTime();
        }

    }

    /**
     * 移除关键帧
     */
    public boolean removeKeyframe(String boneName, float time) {
        List<Keyframe> boneKeyframes = keyframesByBone.get(boneName);
        if (boneKeyframes == null) {
            return false;
        }

        for (int i = 0; i < boneKeyframes.size(); i++) {
            if (Math.abs(boneKeyframes.get(i).getTime() - time) < 0.01f) {
                boneKeyframes.remove(i);
                return true;
            }
        }
        return false;
    }

    /**
     * 移除指定时间的所有骨骼关键帧
     */
    public int removeKeyframesAtTime(float time) {
        int removedCount = 0;
        for (String boneName : new HashSet<>(keyframesByBone.keySet())) {
            if (removeKeyframe(boneName, time)) {
                removedCount++;
            }
        }

        // 重新计算duration
        recalculateDuration();

        return removedCount;
    }

    /**
     * 重新计算动画时长
     */
    private void recalculateDuration() {
        duration = 0f;
        for (List<Keyframe> boneKeyframes : keyframesByBone.values()) {
            for (Keyframe kf : boneKeyframes) {
                if (kf.getTime() > duration) {
                    duration = kf.getTime();
                }
            }
        }
    }

    /**
     * 采样指定时间点的关键帧
     * 如果没有精确匹配，则在相邻关键帧之间插值
     * 快照关键帧会立即切换，不进行插值
     */
    public Keyframe sample(String boneName, float time) {
        List<Keyframe> boneKeyframes = keyframesByBone.get(boneName);
        if (boneKeyframes == null || boneKeyframes.isEmpty()) {
            return null;
        }

        // 处理循环
        if (looping && duration > 0) {
            time = time % duration;
            if (time < 0) time += duration;
        }

        // 只有一个关键帧
        if (boneKeyframes.size() == 1) {
            return new Keyframe(boneKeyframes.get(0));
        }

        // 查找相邻的两个关键帧
        Keyframe before = null;
        Keyframe after = null;

        for (Keyframe kf : boneKeyframes) {
            if (kf.getTime() <= time) {
                before = kf;
            }
            if (kf.getTime() >= time && after == null) {
                after = kf;
            }
        }

        // 时间在第一个关键帧之前
        if (before == null) {
            return new Keyframe(boneKeyframes.get(0));
        }

        // 时间在最后一个关键帧之后
        if (after == null) {
            if (looping && boneKeyframes.size() > 1) {
                // 循环：从最后一帧插值到第一帧
                after = boneKeyframes.get(0);

                // 如果最后一帧或第一帧是快照关键帧，不插值
                if (before.getType() == Keyframe.KeyframeType.SNAPSHOT ||
                    after.getType() == Keyframe.KeyframeType.SNAPSHOT) {
                    return new Keyframe(before);
                }

                float t = (time - before.getTime()) / (duration - before.getTime() + after.getTime());
                return before.interpolate(after, t);
            } else {
                return new Keyframe(boneKeyframes.get(boneKeyframes.size() - 1));
            }
        }

        // 精确匹配
        if (Math.abs(before.getTime() - time) < 0.001f) {
            return new Keyframe(before);
        }
        if (Math.abs(after.getTime() - time) < 0.001f) {
            return new Keyframe(after);
        }

        // 检查是否有快照关键帧
        // 如果起始帧是快照关键帧，保持在起始帧状态直到到达下一个关键帧
        if (before.getType() == Keyframe.KeyframeType.SNAPSHOT) {
            // 快照关键帧：不插值，保持原状态直到下一个关键帧
            Keyframe snapshot = new Keyframe(before);
            snapshot.setTime(time);  // 更新时间为当前时间
            return snapshot;
        }

        // 如果目标帧是快照关键帧，也不插值
        if (after.getType() == Keyframe.KeyframeType.SNAPSHOT) {
            // 保持在before状态，直到精确到达after的时间
            Keyframe snapshot = new Keyframe(before);
            snapshot.setTime(time);
            return snapshot;
        }

        // 两个都是插值关键帧，进行正常插值
        float t = (time - before.getTime()) / (after.getTime() - before.getTime());
        return before.interpolate(after, t);
    }

    /**
     * 获取所有骨骼名称
     */
    public Set<String> getBoneNames() {
        return keyframesByBone.keySet();
    }

    /**
     * 获取指定骨骼的所有关键帧
     */
    public List<Keyframe> getKeyframes(String boneName) {
        List<Keyframe> keyframes = keyframesByBone.get(boneName);
        return keyframes != null ? new ArrayList<>(keyframes) : new ArrayList<>();
    }

    /**
     * 获取所有关键帧
     */
    public List<Keyframe> getAllKeyframes() {
        List<Keyframe> allKeyframes = new ArrayList<>();
        for (List<Keyframe> boneKeyframes : keyframesByBone.values()) {
            allKeyframes.addAll(boneKeyframes);
        }
        // 按时间排序
        allKeyframes.sort(Comparator.comparing(Keyframe::getTime));
        return allKeyframes;
    }

    /**
     * 清空所有关键帧
     */
    public void clear() {
        keyframesByBone.clear();
        duration = 0f;
    }

    // ========== Getters and Setters ==========

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public float getDuration() {
        return duration;
    }

    public void setDuration(float duration) {
        this.duration = duration;
    }

    public boolean isLooping() {
        return looping;
    }

    public void setLooping(boolean looping) {
        this.looping = looping;
    }

    @Override
    public String toString() {
        return String.format("AnimationClip[name=%s, duration=%.2fs, bones=%d, keyframes=%d]",
                name, duration, keyframesByBone.size(), getAllKeyframes().size());
    }
}
