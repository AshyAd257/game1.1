package com.Hecate.puppet.ecs.system;

import com.Hecate.puppet.ecs.Component;
import com.Hecate.puppet.ecs.Entity;
import com.Hecate.puppet.ecs.System;
import com.Hecate.puppet.ecs.component.AnimationComponent;
import com.Hecate.puppet.ecs.component.TransformComponent;
import com.Hecate.puppet.animation.AnimationClip;
import com.Hecate.puppet.animation.Keyframe;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;

import java.util.List;

/**
 * 动画系统
 * 负责更新AnimationComponent并应用动画到Transform
 *
 * 【核心功能】
 * - 更新动画时间
 * - 采样关键帧
 * - 插值计算
 * - 应用到Transform
 * - 支持动画混合
 *
 * 【性能优化】
 * - 使用二分查找采样关键帧（O(log n)而非O(n)）
 * - 复用临时对象避免GC
 */
public class AnimationSystem implements System {

    private boolean enabled = true;

    // 临时变量（复用，避免GC）
    private final Vector3f tempPos = new Vector3f();
    private final Quaternion tempRot = new Quaternion();
    private final Vector3f tempScale = new Vector3f();
    private final Vector3f tempPos2 = new Vector3f();
    private final Quaternion tempRot2 = new Quaternion();
    private final Vector3f tempScale2 = new Vector3f();

    @Override
    public void initialize() {
        java.lang.System.out.println("[AnimationSystem] Initialized");
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<? extends Component>[] getRequiredComponents() {
        return new Class[]{AnimationComponent.class, TransformComponent.class};
    }

    @Override
    public int getPriority() {
        return -50; // 在TransformSystem之后，但在渲染之前
    }

    @Override
    public void update(List<Entity> entities, float tpf) {
        for (Entity entity : entities) {
            AnimationComponent animation = entity.getComponent(AnimationComponent.class);
            TransformComponent transform = entity.getComponent(TransformComponent.class);

            if (animation == null || transform == null) {
                continue;
            }

            // 更新动画时间
            animation.updateTime(tpf);

            // 应用动画到Transform
            applyAnimation(animation, transform);
        }
    }

    /**
     * 应用动画到Transform
     */
    private void applyAnimation(AnimationComponent animation, TransformComponent transform) {
        if (!animation.isPlaying()) {
            return;
        }

        AnimationClip currentClip = animation.getCurrentClip();
        if (currentClip == null) {
            return;
        }

        if (animation.isBlending()) {
            // 混合两个动画
            AnimationClip targetClip = animation.getTargetClip();
            if (targetClip != null) {
                applyBlendedAnimation(animation, transform, currentClip, targetClip);
            } else {
                applySingleAnimation(currentClip, animation.getCurrentTime(), transform);
            }
        } else {
            // 播放单个动画
            applySingleAnimation(currentClip, animation.getCurrentTime(), transform);
        }
    }

    /**
     * 应用单个动画
     */
    private void applySingleAnimation(AnimationClip clip, float time, TransformComponent transform) {
        // 采样动画
        Keyframe result = sampleAnimation(clip, time);
        if (result == null) {
            return;
        }

        // 应用到Transform
        if (result.getPosition() != null) {
            transform.setLocalPosition(result.getPosition());
        }
        if (result.getRotation() != null) {
            transform.setLocalRotation(result.getRotation());
        }
        if (result.getScale() != null) {
            transform.setLocalScale(result.getScale());
        }
    }

    /**
     * 应用混合动画
     */
    private void applyBlendedAnimation(AnimationComponent animation, TransformComponent transform,
                                      AnimationClip clip1, AnimationClip clip2) {
        float time1 = animation.getCurrentTime();
        float time2 = 0f; // 目标动画从头开始
        float weight = animation.getBlendWeight(); // 0=完全播放clip1, 1=完全播放clip2

        // 采样两个动画
        Keyframe result1 = sampleAnimation(clip1, time1);
        Keyframe result2 = sampleAnimation(clip2, time2);

        if (result1 == null && result2 == null) {
            return;
        }

        // 混合
        if (result1 != null && result2 != null) {
            // 位置混合
            tempPos.set(result1.getPosition()).multLocal(1 - weight);
            tempPos2.set(result2.getPosition()).multLocal(weight);
            tempPos.addLocal(tempPos2);
            transform.setLocalPosition(tempPos);

            // 旋转混合（使用slerp）
            tempRot.set(result1.getRotation());
            tempRot2.set(result2.getRotation());
            tempRot.slerp(tempRot2, weight);
            transform.setLocalRotation(tempRot);

            // 缩放混合
            tempScale.set(result1.getScale()).multLocal(1 - weight);
            tempScale2.set(result2.getScale()).multLocal(weight);
            tempScale.addLocal(tempScale2);
            transform.setLocalScale(tempScale);
        } else if (result1 != null) {
            applySingleAnimation(clip1, time1, transform);
        } else {
            applySingleAnimation(clip2, time2, transform);
        }
    }

    /**
     * 采样动画（使用优化的二分查找）
     */
    private Keyframe sampleAnimation(AnimationClip clip, float time) {
        if (clip == null) {
            return null;
        }

        // 注意：这里简化处理，实际应该对每个骨骼分别采样
        // 这里假设clip有一个主要的关键帧序列
        List<Keyframe> keyframes = clip.getAllKeyframes();
        if (keyframes.isEmpty()) {
            return null;
        }

        // 使用二分查找找到时间区间
        int index = binarySearchKeyframe(keyframes, time);

        if (index < 0) {
            // 在第一帧之前
            return keyframes.get(0);
        } else if (index >= keyframes.size() - 1) {
            // 在最后一帧之后
            return keyframes.get(keyframes.size() - 1);
        } else {
            // 在两帧之间，进行插值
            Keyframe before = keyframes.get(index);
            Keyframe after = keyframes.get(index + 1);
            return interpolateKeyframes(before, after, time);
        }
    }

    /**
     * 二分查找关键帧
     * 返回time之前或等于的最后一个关键帧的索引
     */
    private int binarySearchKeyframe(List<Keyframe> keyframes, float time) {
        int low = 0;
        int high = keyframes.size() - 1;
        int result = -1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            float midTime = keyframes.get(mid).getTime();

            if (midTime <= time) {
                result = mid;
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }

        return result;
    }

    /**
     * 插值两个关键帧
     */
    private Keyframe interpolateKeyframes(Keyframe before, Keyframe after, float time) {
        float t1 = before.getTime();
        float t2 = after.getTime();

        if (t2 <= t1) {
            return after; // 避免除零
        }

        float t = (time - t1) / (t2 - t1);
        t = Math.max(0f, Math.min(1f, t)); // 限制在[0,1]

        // 检查是否是快照关键帧（立即切换，无插值）
        if (before.getType() == Keyframe.KeyframeType.SNAPSHOT) {
            return before;
        }
        if (after.getType() == Keyframe.KeyframeType.SNAPSHOT && t >= 0.5f) {
            return after;
        }

        // 创建插值结果
        Keyframe result = new Keyframe(time, before.getBoneName(), Keyframe.KeyframeType.INTERPOLATED);

        // 位置插值
        tempPos.set(before.getPosition()).multLocal(1 - t);
        tempPos2.set(after.getPosition()).multLocal(t);
        result.setPosition(tempPos.add(tempPos2));

        // 旋转插值（slerp）
        tempRot.set(before.getRotation());
        tempRot2.set(after.getRotation());
        tempRot.slerp(tempRot2, t);
        result.setRotation(tempRot);

        // 缩放插值
        tempScale.set(before.getScale()).multLocal(1 - t);
        tempScale2.set(after.getScale()).multLocal(t);
        result.setScale(tempScale.add(tempScale2));

        return result;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public void cleanup() {
        java.lang.System.out.println("[AnimationSystem] Cleaned up");
    }
}
