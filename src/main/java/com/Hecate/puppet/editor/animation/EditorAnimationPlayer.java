package com.Hecate.puppet.editor.animation;

import com.Hecate.puppet.editor.core.EditorSkeleton;
import com.Hecate.puppet.editor.core.EditorBone;
import com.Hecate.puppet.editor.core.EditorPuppetRenderer;
import com.Hecate.puppet.animation.AnimationClip;
import com.Hecate.puppet.animation.AnimationLayer;
import com.Hecate.puppet.animation.BoneMask;
import com.Hecate.puppet.animation.Keyframe;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;

/**
 * 动画播放器
 * 负责播放AnimationClip并应用到Skeleton和PuppetRenderer
 * 支持多层动画混合
 */
public class EditorAnimationPlayer {

    private EditorSkeleton skeleton;
    private EditorPuppetRenderer puppetRenderer;

    private AnimationClip currentClip;
    private float currentTime;
    private boolean playing;
    private float playbackSpeed;

    // 编辑模式标志 - 当用户手动编辑部件时，防止动画覆盖
    private boolean editMode;

    // ==================== 动画分层系统 ====================

    // 动画层列表（按优先级排序）
    private List<AnimationLayer> layers;

    // 是否启用分层模式（false时使用传统单动画模式）
    private boolean layeredMode;

    public EditorAnimationPlayer(EditorSkeleton skeleton, EditorPuppetRenderer puppetRenderer) {
        this.skeleton = skeleton;
        this.puppetRenderer = puppetRenderer;
        this.currentTime = 0f;
        this.playing = false;
        this.playbackSpeed = 1.0f;
        this.editMode = false;  // 默认不是编辑模式
        this.layers = new ArrayList<>();
        this.layeredMode = false;  // 默认使用传统单动画模式
    }

    /**
     * 播放动画片段
     */
    public void play(AnimationClip clip) {
        this.currentClip = clip;
        this.currentTime = 0f;
        this.playing = true;

        // 重新启用所有部件的动画旋转控制
        enableAnimationRotationForAllParts();
    }

    /**
     * 播放动画片段（从指定时间开始）
     */
    public void play(AnimationClip clip, float startTime) {
        this.currentClip = clip;
        this.currentTime = startTime;
        this.playing = true;

        // 重新启用所有部件的动画旋转控制
        enableAnimationRotationForAllParts();
    }

    /**
     * 暂停播放
     */
    public void pause() {
        this.playing = false;
    }

    /**
     * 继续播放
     */
    public void resume() {
        if (currentClip != null) {
            this.playing = true;
        }
    }

    /**
     * 停止播放
     */
    public void stop() {
        this.playing = false;
        this.currentTime = 0f;

        // 重置所有部件的动画旋转标志
        if (puppetRenderer != null && skeleton != null) {
            for (EditorBone bone : skeleton.getAllBones()) {
                var partRenderer = puppetRenderer.getPartRenderer(bone.getName());
                if (partRenderer != null) {
                    partRenderer.resetAnimationRotation();
                }
            }
        }

    }

    /**
     * 跳转到指定时间
     */
    public void seek(float time) {
        if (currentClip != null) {
            this.currentTime = time;
            applyAnimation(time);
        }
    }

    /**
     * 更新动画（每帧调用）
     */
    public void update(float tpf) {
        if (!playing || currentClip == null) {
            return;
        }

        float duration = currentClip.getDuration();

        // 如果duration是0（没有关键帧），仍然更新时间以便编辑
        if (duration <= 0.001f) {
            // 检查是否真的没有关键帧
            if (currentClip.getAllKeyframes().isEmpty()) {
                // 真的没有关键帧时，简单地累加时间
                currentTime += tpf * playbackSpeed;
                return;
            }
            // 有关键帧但duration为0（单个关键帧在0.00s），仍然应用动画
            currentTime += tpf * playbackSpeed;
            applyAnimation(currentTime);
            return;
        }

        // 更新时间
        currentTime += tpf * playbackSpeed;

        // 处理循环
        if (currentClip.isLooping()) {
            if (currentTime >= duration) {
                currentTime = currentTime % duration;
            }
        } else {
            // 非循环：到达结尾时停止
            if (currentTime >= duration) {
                currentTime = duration;
                playing = false;
            }
        }

        // 应用动画
        applyAnimation(currentTime);
    }

    /**
     * 应用动画到指定时间点
     */
    private void applyAnimation(float time) {
        if (layeredMode) {
            // 分层模式：应用所有启用的层
            applyLayeredAnimation(time);
        } else {
            // 传统模式：应用单个动画片段
            applySingleAnimation(time);
        }
    }

    /**
     * 应用单个动画片段（传统模式）
     */
    private void applySingleAnimation(float time) {
        if (currentClip == null) {
            return;
        }

        // 【重要】如果在编辑模式，不应用动画，防止覆盖用户的手动编辑
        if (editMode) {
            return;
        }

        // 【新增】在非播放模式下，限制时间不超过duration，保持在最后一帧
        float duration = currentClip.getDuration();
        if (!playing && time > duration && duration > 0) {
            time = duration - 0.001f;
        }

        try {
            // 对每个骨骼采样并应用
            for (String boneName : currentClip.getBoneNames()) {
                Keyframe kf = currentClip.sample(boneName, time);
                if (kf == null) {
                    continue;
                }

                // 应用到骨骼
                EditorBone bone = skeleton.findBone(boneName);
                if (bone != null) {
                    bone.setLocalPosition(kf.getPosition());
                    bone.setLocalRotation(kf.getRotation());
                    bone.setLocalScale(kf.getScale());
                }

                // 应用到渲染器
                if (puppetRenderer != null) {
                    var partRenderer = puppetRenderer.getPartRenderer(boneName);
                    if (partRenderer != null) {
                        partRenderer.setSize(kf.getWidth(), kf.getHeight());
                        partRenderer.setCustomRotationX(kf.getCustomRotationX());
                        partRenderer.setCustomRotationY(kf.getCustomRotationY());
                        partRenderer.setCustomRotationZ(kf.getCustomRotationZ());

                        // 设置贴图旋转
                        float texRot = kf.getTextureRotation();

                        // 应用多方向贴图旋转（新功能）
                        java.util.Map<String, Float> dirRotations = kf.getDirectionTextureRotations();
                        if (dirRotations != null && !dirRotations.isEmpty()) {
                            // 使用多方向贴图旋转
                            for (java.util.Map.Entry<String, Float> entry : dirRotations.entrySet()) {
                                bone.setDirectionTextureRotation(entry.getKey(), entry.getValue());
                            }
                            // 禁用动画旋转标志，让渲染器使用方向旋转
                            partRenderer.setUseAnimationRotation(false);
                        } else if (texRot != 0f) {
                            // 向后兼容：如果没有多方向旋转但有单一旋转值，应用到渲染器
                            // 区分播放和跳转两种情况：
                            // - 播放时：使用setTextureRotation()锁定动画控制
                            // - 跳转时：使用setTextureRotationInternal()保持UI控制权
                            if (playing) {
                                // 播放模式：只有在渲染器允许动画控制时才应用
                                if (partRenderer.isUsingAnimationRotation()) {
                                    partRenderer.setTextureRotation(texRot);
                                }
                            } else {
                                // 跳转/seek模式：应用值但不锁定控制权
                                partRenderer.setTextureRotationInternal(texRot);
                            }
                        }

                        if (kf.getType() == Keyframe.KeyframeType.SNAPSHOT && kf.getTexturePath() != null) {
                            String currentTexPath = partRenderer.getTexturePath();
                            if (currentTexPath == null || !currentTexPath.equals(kf.getTexturePath())) {
                                partRenderer.loadTexture(kf.getTexturePath());
                            }
                        }
                    }
                }
            }

            // 应用摇摆动画（在关键帧动画之后，叠加在关键帧动画上）
            int swingEnabledCount = 0;
            for (EditorBone bone : skeleton.getAllBones()) {
                if (bone.isSwingEnabled()) {
                    swingEnabledCount++;
                    // 获取摇摆参数
                    float amplitude = bone.getSwingAmplitude();  // 幅度（度）
                    float frequency = bone.getSwingFrequency();  // 频率（Hz）
                    float phase = bone.getSwingPhaseOffset();    // 相位偏移（弧度）

                    // 计算摇摆角度：angle = amplitude × sin(2π × frequency × time + phase)
                    float swingAngle = amplitude * (float)Math.sin(2 * Math.PI * frequency * time + phase);

                    // 获取摇摆轴
                    Vector3f swingAxis = bone.getSwingAxis();

                    // 创建摇摆旋转四元数（将角度从度转换为弧度）
                    Quaternion swingRotation = new Quaternion();
                    swingRotation.fromAngleAxis(swingAngle * (float)Math.PI / 180f, swingAxis);

                    // 将摇摆旋转叠加到当前旋转上（乘法组合）
                    Quaternion currentRotation = bone.getLocalRotation();
                    Quaternion newRotation = currentRotation.mult(swingRotation);
                    bone.setLocalRotation(newRotation);
                }
            }

            // 更新骨骼变换
            skeleton.updateTransforms();

            // 更新渲染器
            if (puppetRenderer != null) {
                puppetRenderer.update(0);
            }
        } catch (Exception e) {
            System.err.println("[EditorAnimationPlayer] 动画应用失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 应用分层动画（新功能）
     * 按优先级顺序应用所有启用的层，高优先级层覆盖低优先级层
     */
    private void applyLayeredAnimation(float time) {
        if (layers.isEmpty()) {
            return;
        }

        // 【重要】如果在编辑模式，不应用动画，防止覆盖用户的手动编辑
        if (editMode) {
            return;
        }

        try {
            // 按优先级排序层（低优先级先应用）
            List<AnimationLayer> sortedLayers = new ArrayList<>(layers);
            sortedLayers.sort(Comparator.comparingInt(AnimationLayer::getPriority));

            // 记录每个骨骼是否已被处理（用于优先级覆盖）
            java.util.Set<String> processedBones = new java.util.HashSet<>();

            // 从低优先级到高优先级应用层
            for (AnimationLayer layer : sortedLayers) {
                if (!layer.isEnabled() || layer.getClip() == null) {
                    continue;
                }

                AnimationClip clip = layer.getClip();
                float effectiveWeight = layer.getEffectiveWeight();

                if (effectiveWeight <= 0.001f) {
                    continue;  // 权重太小，跳过
                }

                // 对每个骨骼采样并应用
                for (String boneName : clip.getBoneNames()) {
                    // 检查此层是否影响该骨骼
                    if (!layer.affects(boneName)) {
                        continue;
                    }

                    Keyframe kf = clip.sample(boneName, time);
                    if (kf == null) {
                        continue;
                    }

                    EditorBone bone = skeleton.findBone(boneName);
                    if (bone == null) {
                        continue;
                    }

                    // 如果权重为1.0，直接覆盖
                    if (effectiveWeight >= 0.999f) {
                        bone.setLocalPosition(kf.getPosition());
                        bone.setLocalRotation(kf.getRotation());
                        bone.setLocalScale(kf.getScale());
                    } else {
                        // 权重混合（与当前值混合）
                        bone.setLocalPosition(
                            bone.getLocalPosition().mult(1f - effectiveWeight)
                                .add(kf.getPosition().mult(effectiveWeight))
                        );
                        // 旋转混合：创建新的四元数进行插值
                        Quaternion blendedRotation = new Quaternion();
                        blendedRotation.slerp(bone.getLocalRotation(), kf.getRotation(), effectiveWeight);
                        bone.setLocalRotation(blendedRotation);
                        bone.setLocalScale(
                            bone.getLocalScale().mult(1f - effectiveWeight)
                                .add(kf.getScale().mult(effectiveWeight))
                        );
                    }

                    // 应用到渲染器
                    if (puppetRenderer != null) {
                        var partRenderer = puppetRenderer.getPartRenderer(boneName);
                        if (partRenderer != null) {
                            if (effectiveWeight >= 0.999f) {
                                partRenderer.setSize(kf.getWidth(), kf.getHeight());
                                partRenderer.setCustomRotationX(kf.getCustomRotationX());
                                partRenderer.setCustomRotationY(kf.getCustomRotationY());
                                partRenderer.setCustomRotationZ(kf.getCustomRotationZ());
                            } else {
                                // 权重混合尺寸和旋转
                                float currentWidth = partRenderer.getWidth();
                                float currentHeight = partRenderer.getHeight();
                                partRenderer.setSize(
                                    currentWidth * (1f - effectiveWeight) + kf.getWidth() * effectiveWeight,
                                    currentHeight * (1f - effectiveWeight) + kf.getHeight() * effectiveWeight
                                );
                            }

                            // 处理贴图旋转
                            float texRot = kf.getTextureRotation();
                            java.util.Map<String, Float> dirRotations = kf.getDirectionTextureRotations();
                            if (dirRotations != null && !dirRotations.isEmpty()) {
                                for (java.util.Map.Entry<String, Float> entry : dirRotations.entrySet()) {
                                    bone.setDirectionTextureRotation(entry.getKey(), entry.getValue());
                                }
                                partRenderer.setUseAnimationRotation(false);
                            } else if (texRot != 0f) {
                                if (playing && partRenderer.isUsingAnimationRotation()) {
                                    partRenderer.setTextureRotation(texRot);
                                } else if (!playing) {
                                    partRenderer.setTextureRotationInternal(texRot);
                                }
                            }

                            // 处理快照纹理
                            if (kf.getType() == Keyframe.KeyframeType.SNAPSHOT && kf.getTexturePath() != null) {
                                String currentTexPath = partRenderer.getTexturePath();
                                if (currentTexPath == null || !currentTexPath.equals(kf.getTexturePath())) {
                                    partRenderer.loadTexture(kf.getTexturePath());
                                }
                            }
                        }
                    }

                    processedBones.add(boneName);
                }
            }

            // 应用摇摆动画（在关键帧动画之后，叠加在关键帧动画上）
            int swingEnabledCount = 0;
            for (EditorBone bone : skeleton.getAllBones()) {
                if (bone.isSwingEnabled()) {
                    swingEnabledCount++;
                    // 获取摇摆参数
                    float amplitude = bone.getSwingAmplitude();  // 幅度（度）
                    float frequency = bone.getSwingFrequency();  // 频率（Hz）
                    float phase = bone.getSwingPhaseOffset();    // 相位偏移（弧度）

                    // 计算摇摆角度：angle = amplitude × sin(2π × frequency × time + phase)
                    float swingAngle = amplitude * (float)Math.sin(2 * Math.PI * frequency * time + phase);

                    // 获取摇摆轴
                    Vector3f swingAxis = bone.getSwingAxis();

                    // 创建摇摆旋转四元数（将角度从度转换为弧度）
                    Quaternion swingRotation = new Quaternion();
                    swingRotation.fromAngleAxis(swingAngle * (float)Math.PI / 180f, swingAxis);

                    // 将摇摆旋转叠加到当前旋转上（乘法组合）
                    Quaternion currentRotation = bone.getLocalRotation();
                    Quaternion newRotation = currentRotation.mult(swingRotation);
                    bone.setLocalRotation(newRotation);
                }
            }

            // 更新骨骼变换
            skeleton.updateTransforms();

            // 更新渲染器
            if (puppetRenderer != null) {
                puppetRenderer.update(0);
            }
        } catch (Exception e) {
            System.err.println("[EditorAnimationPlayer] 分层动画应用失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ==================== 层管理方法 ====================

    /**
     * 添加动画层
     */
    public void addLayer(AnimationLayer layer) {
        if (layer != null && !layers.contains(layer)) {
            layers.add(layer);
            sortLayers();
        }
    }

    /**
     * 移除动画层
     */
    public void removeLayer(AnimationLayer layer) {
        layers.remove(layer);
    }

    /**
     * 移除指定名称的层
     */
    public void removeLayer(String layerName) {
        layers.removeIf(layer -> layer.getName().equals(layerName));
    }

    /**
     * 获取指定名称的层
     */
    public AnimationLayer getLayer(String layerName) {
        for (AnimationLayer layer : layers) {
            if (layer.getName().equals(layerName)) {
                return layer;
            }
        }
        return null;
    }

    /**
     * 获取所有层
     */
    public List<AnimationLayer> getLayers() {
        return new ArrayList<>(layers);
    }

    /**
     * 清空所有层
     */
    public void clearLayers() {
        layers.clear();
    }

    /**
     * 按优先级排序层
     */
    private void sortLayers() {
        layers.sort(Comparator.comparingInt(AnimationLayer::getPriority));
    }

    /**
     * 启用分层模式
     */
    public void enableLayeredMode() {
        this.layeredMode = true;
    }

    /**
     * 禁用分层模式（回到传统单动画模式）
     */
    public void disableLayeredMode() {
        this.layeredMode = false;
    }

    /**
     * 检查是否启用分层模式
     */
    public boolean isLayeredMode() {
        return layeredMode;
    }

    /**
     * 播放分层动画（启用分层模式并开始播放）
     */
    public void playLayered() {
        this.layeredMode = true;
        this.playing = true;
        this.currentTime = 0f;

        // 重新启用所有部件的动画旋转控制
        enableAnimationRotationForAllParts();
    }

    /**
     * 为所有部件启用动画旋转控制
     * 当开始播放动画时调用，让动画系统重新接管旋转控制
     */
    private void enableAnimationRotationForAllParts() {
        if (puppetRenderer == null || currentClip == null) {
            return;
        }

        // 遍历动画中的所有骨骼，重新启用动画旋转
        for (String boneName : currentClip.getBoneNames()) {
            var partRenderer = puppetRenderer.getPartRenderer(boneName);
            if (partRenderer != null) {
                partRenderer.setUseAnimationRotation(true);
            }
        }
    }

    /**
     * 从当前状态录制关键帧
     */
    public Keyframe recordKeyframe(String boneName, float time) {
        return recordKeyframe(boneName, time, Keyframe.KeyframeType.INTERPOLATED);
    }

    /**
     * 从当前状态录制关键帧（指定类型）
     */
    public Keyframe recordKeyframe(String boneName, float time, Keyframe.KeyframeType type) {
        EditorBone bone = skeleton.findBone(boneName);
        if (bone == null) {
            return null;
        }

        Keyframe kf = new Keyframe(
            time,
            boneName,
            type
        );

        // 设置骨骼变换
        kf.setPosition(bone.getLocalPosition().clone());
        kf.setRotation(bone.getLocalRotation().clone());
        kf.setScale(bone.getLocalScale().clone());

        // 获取渲染器尺寸和自定义旋转
        if (puppetRenderer != null) {
            var partRenderer = puppetRenderer.getPartRenderer(boneName);
            if (partRenderer != null) {
                kf.setWidth(partRenderer.getWidth());
                kf.setHeight(partRenderer.getHeight());
                kf.setCustomRotationX(partRenderer.getCustomRotationX());
                kf.setCustomRotationY(partRenderer.getCustomRotationY());
                kf.setCustomRotationZ(partRenderer.getCustomRotationZ());

                // 获取当前实际的贴图旋转值
                // 如果正在使用动画旋转，使用textureRotation；否则使用方向旋转
                float currentRotation = partRenderer.getTextureRotation();
                if (currentRotation == 0f) {
                    // 可能是使用UI滑块设置的方向旋转
                    currentRotation = bone.getCurrentDirectionTextureRotation();
                }
                kf.setTextureRotation(currentRotation);

                // 记录所有方向的贴图旋转（新功能）
                java.util.Map<String, Float> dirRotations = bone.getDirectionTextureRotations();
                if (dirRotations != null && !dirRotations.isEmpty()) {
                    kf.setDirectionTextureRotations(dirRotations);
                }

                // 如果是快照关键帧，记录纹理路径
                if (type == Keyframe.KeyframeType.SNAPSHOT) {
                    kf.setTexturePath(partRenderer.getTexturePath());
                }
            }
        }

        return kf;
    }

    /**
     * 录制所有骨骼的关键帧
     */
    public void recordAllKeyframes(AnimationClip clip, float time) {
        recordAllKeyframes(clip, time, Keyframe.KeyframeType.INTERPOLATED);
    }

    /**
     * 录制所有骨骼的关键帧（指定类型）
     */
    public void recordAllKeyframes(AnimationClip clip, float time, Keyframe.KeyframeType type) {
        int successCount = 0;
        int failCount = 0;
        java.util.List<String> failedBones = new java.util.ArrayList<>();

        for (EditorBone bone : skeleton.getAllBones()) {
            Keyframe kf = recordKeyframe(bone.getName(), time, type);
            if (kf != null) {
                clip.addKeyframe(kf);
                successCount++;
            } else {
                failCount++;
                failedBones.add(bone.getName());
            }
        }

        if (failCount > 0) {
        }
    }

    // ========== Getters and Setters ==========

    public AnimationClip getCurrentClip() {
        return currentClip;
    }

    public void setCurrentClip(AnimationClip clip) {
        this.currentClip = clip;
    }

    public float getCurrentTime() {
        return currentTime;
    }

    public void setCurrentTime(float time) {
        this.currentTime = time;
    }

    public boolean isPlaying() {
        return playing;
    }

    public void setPlaying(boolean playing) {
        this.playing = playing;
    }

    public float getPlaybackSpeed() {
        return playbackSpeed;
    }

    public void setPlaybackSpeed(float speed) {
        this.playbackSpeed = speed;
    }

    public EditorSkeleton getSkeleton() {
        return skeleton;
    }

    public EditorPuppetRenderer getPuppetRenderer() {
        return puppetRenderer;
    }

    /**
     * 获取编辑模式状态
     */
    public boolean isEditMode() {
        return editMode;
    }

    /**
     * 设置编辑模式
     * 编辑模式下，动画不会应用，防止覆盖用户的手动编辑
     */
    public void setEditMode(boolean editMode) {
        this.editMode = editMode;
        if (editMode) {

        } else {

        }
    }

    /**
     * 退出编辑模式并重新应用当前时间的动画
     */
    public void exitEditModeAndApply() {
        this.editMode = false;
        applyAnimation(currentTime);
    }
}
