package com.Hecate.player;

import com.Hecate.puppet.core.PuppetPartRenderer;
import com.jme3.anim.Joint;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;

/**
 * 单个"puppet部件 <-> 3D骨骼Joint"绑定的最小单元，每帧把 {@link #targetJoint}
 * 的世界变换（位置+旋转）换算后写入 {@link #puppetPartNode}。由
 * {@link PuppetBoneAttachmentManager} 批量创建和持有，不单独实例化使用。
 *
 * update() 里的坐标换算依赖调用方传入的 characterNode 当前的
 * localTranslation/localRotation/localScale，这三者由 {@link SkeletalPlayerController}
 * 每帧在别处设置——本类只读不写这些量，若部件位置/朝向整体出错，先确认
 * characterNode 的这三个量当时的取值是否符合预期，而不是先怀疑本类的换算逻辑。
 */
public class PuppetBoneAttachment {

    private final Joint targetJoint;           // 目标骨骼
    private final Node puppetPartNode;         // puppet 部件节点
    private final PuppetPartRenderer partRenderer; // puppet 部件渲染器（可选，用于更新方向）
    private final Vector3f localOffset;        // 相对骨骼的偏移
    private final float scale;                 // 缩放比例

    // 是否跟随骨骼旋转（如果为 false，部件只跟随位置，始终朝向摄像机）
    private final boolean followRotation;

    // 调试计数器（每个实例独立）
    private int debugCounter = 0;

    /**
     * 创建 puppet 部件绑定
     *
     * @param targetJoint 目标骨骼
     * @param puppetPartNode puppet 部件节点
     * @param partRenderer puppet 部件渲染器（可选）
     * @param localOffset 相对骨骼的偏移
     * @param scale 缩放比例
     * @param followRotation 是否跟随骨骼旋转
     */
    public PuppetBoneAttachment(Joint targetJoint, Node puppetPartNode,
                                PuppetPartRenderer partRenderer,
                                Vector3f localOffset, float scale,
                                boolean followRotation) {
        this.targetJoint = targetJoint;
        this.puppetPartNode = puppetPartNode;
        this.partRenderer = partRenderer;
        this.localOffset = localOffset.clone();
        this.scale = scale;
        this.followRotation = followRotation;
    }

    /**
     * 更新 puppet 部件的位置和旋转，使其跟随骨骼
     *
     * @param characterNode 角色根节点
     * @param modelScale 模型的缩放比例
     */
    public void update(Node characterNode, float modelScale) {
        if (targetJoint == null || puppetPartNode == null) {
            return;
        }

        // 获取骨骼的世界变换
        Vector3f boneWorldPos = new Vector3f();
        Quaternion boneWorldRot = new Quaternion();
        getJointWorldTransform(targetJoint, characterNode, modelScale, boneWorldPos, boneWorldRot);

        // 应用局部偏移（考虑骨骼旋转）
        Vector3f offsetWorld = boneWorldRot.mult(localOffset.mult(scale));
        Vector3f finalPos = boneWorldPos.add(offsetWorld);

        // 更新 puppet 部件位置
        puppetPartNode.setLocalTranslation(finalPos);

        // 更新旋转
        if (followRotation) {
            // 跟随骨骼旋转
            puppetPartNode.setLocalRotation(boneWorldRot);

            // 如果有渲染器，通知它更新方向
            if (partRenderer != null) {
                // 根据骨骼朝向计算应该显示的方向
                updatePartDirection(boneWorldRot);
            }
        } else {
            // 不跟随旋转，保持默认朝向（billboard 模式会自动朝向摄像机）
            puppetPartNode.setLocalRotation(Quaternion.IDENTITY);
        }

        // 更新缩放
        puppetPartNode.setLocalScale(scale);

        // 驱动条带贴图按观察角度切换UV（不影响partGeometry的位置/旋转，
        // 那些已经由上面的puppetPartNode.setLocalTranslation/setLocalRotation决定）。
        // 之前这里依赖PuppetBoneAttachmentManager调用puppetRenderer.update(tpf)来刷新，
        // 但那会连带驱动PuppetPartRenderer.updateTransform()按puppet自己的Skeleton/Bone
        // （.puppet配置的层级偏移）重新设置partGeometry坐标，与这里的3D骨骼坐标冲突、
        // 产生叠加偏移，因此被移除——这里改为只调用条带UV刷新，两者互不影响。
        if (partRenderer != null) {
            partRenderer.updateRotationStripUV(finalPos);
        }

        debugCounter++;
    }

    /**
     * 获取骨骼的世界变换（位置和旋转）
     */
    private void getJointWorldTransform(Joint joint, Node characterNode, float modelScale,
                                       Vector3f outPosition, Quaternion outRotation) {
        // 获取骨骼的模型空间变换
        Vector3f modelPos = joint.getModelTransform().getTranslation();
        Quaternion modelRot = joint.getModelTransform().getRotation();

        // 转换到世界空间（考虑角色节点的变换）
        Vector3f charPos = characterNode.getLocalTranslation();
        Quaternion charRot = characterNode.getLocalRotation();
        float charScale = characterNode.getLocalScale().x; // 假设等比缩放

        // 应用角色节点的缩放和旋转。注意：characterNode 自身的缩放（charScale）
        // 已经等于调用方传入的 modelScale（两者都来自 SkeletalPlayerController 的
        // MODEL_SCALE），这里不能再乘一次 modelScale，否则骨骼的模型空间坐标会被
        // 缩放两次（相当于 scale^2），导致不同骨骼之间的Y方向间距被过度压缩，
        // 所有puppet部件挤在角色中心附近、看起来"整体偏高/叠在一起"。
        outPosition.set(modelPos).multLocal(charScale);
        outPosition.set(charRot.mult(outPosition));
        outPosition.addLocal(charPos);

        // 组合旋转
        outRotation.set(charRot.mult(modelRot));
    }

    /**
     * 根据骨骼旋转更新 puppet 部件的显示方向
     * （如果使用旋转条贴图，这里会选择正确的帧）
     */
    private void updatePartDirection(Quaternion boneRotation) {
        if (partRenderer == null) {
            return;
        }

        // 获取骨骼的前向向量（本地 Z 轴）
        Vector3f forward = boneRotation.mult(Vector3f.UNIT_Z);

        // 计算水平朝向角度（忽略俯仰）
        float yaw = (float) Math.atan2(forward.x, forward.z);

        // 归一化到 0-2π
        if (yaw < 0) {
            yaw += Math.PI * 2;
        }

        // 根据角度确定方向（8方向或16方向）
        String direction = getDirectionFromYaw(yaw);

        // 通知渲染器更新方向（如果渲染器支持）
        // 注意：这需要 PuppetPartRenderer 有设置方向的方法
        // partRenderer.setDirection(direction);
    }

    /**
     * 根据 yaw 角度获取方向字符串
     * 支持 8 方向：front, front-right, right, back-right, back, back-left, left, front-left
     */
    private String getDirectionFromYaw(float yaw) {
        float pi = (float) Math.PI;
        float angle = yaw;

        // 8个方向，每个占 45° (π/4)
        if (angle < pi / 8 || angle >= 15 * pi / 8) {
            return "front";
        } else if (angle >= pi / 8 && angle < 3 * pi / 8) {
            return "front-right";
        } else if (angle >= 3 * pi / 8 && angle < 5 * pi / 8) {
            return "right";
        } else if (angle >= 5 * pi / 8 && angle < 7 * pi / 8) {
            return "back-right";
        } else if (angle >= 7 * pi / 8 && angle < 9 * pi / 8) {
            return "back";
        } else if (angle >= 9 * pi / 8 && angle < 11 * pi / 8) {
            return "back-left";
        } else if (angle >= 11 * pi / 8 && angle < 13 * pi / 8) {
            return "left";
        } else {
            return "front-left";
        }
    }

    /**
     * 获取绑定的骨骼
     */
    public Joint getTargetJoint() {
        return targetJoint;
    }

    /**
     * 获取 puppet 部件节点
     */
    public Node getPuppetPartNode() {
        return puppetPartNode;
    }
}
