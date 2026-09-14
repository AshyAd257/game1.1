package com.Hecate.player;

import com.Hecate.puppet.config.PuppetConfig;
import com.Hecate.puppet.config.PuppetIO;
import com.Hecate.puppet.core.Bone;
import com.Hecate.puppet.core.PuppetPartRenderer;
import com.Hecate.puppet.core.PuppetRenderer;
import com.Hecate.puppet.core.Skeleton;
import com.jme3.anim.Armature;
import com.jme3.anim.Joint;
import com.jme3.anim.SkinningControl;
import com.jme3.app.SimpleApplication;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 加载一份 .puppet 配置（生成独立的 {@link Skeleton}/{@link PuppetRenderer}），
 * 按调用方传入的 boneMapping（puppet部件名 -> 3D骨骼Joint名）把每个 puppet 部件的
 * Geometry 摘出来，分别包一层 {@link PuppetBoneAttachment} 挂到 {@link SkinningControl}
 * 暴露的 {@link Armature} 对应 Joint 上。只被 {@link SkeletalPlayerController} 使用。
 *
 * 骨骼名匹配失败（boneMapping里的名字在Armature中找不到）只会打印错误日志、跳过
 * 该部件，不会抛异常中断——若部件缺失或位置不对，先检查控制台的
 * "[PuppetBoneAttachment] 错误：未找到骨骼" 日志，用 com.Hecate.tools.ListModelBones
 * （list_model_bones.bat）核对模型的真实骨骼名。
 */
public class PuppetBoneAttachmentManager {

    private final SimpleApplication app;
    private final Node characterNode;
    private final Spatial characterModel;
    private final float modelScale;

    private PuppetRenderer puppetRenderer;
    private Skeleton puppetSkeleton;
    private Armature modelArmature;

    private final List<PuppetBoneAttachment> attachments = new ArrayList<>();
    private final Map<String, Joint> jointMap = new HashMap<>();

    /**
     * 创建管理器
     *
     * @param app 应用程序
     * @param characterNode 角色根节点
     * @param characterModel 角色模型
     * @param modelScale 模型缩放比例
     */
    public PuppetBoneAttachmentManager(SimpleApplication app, Node characterNode,
                                      Spatial characterModel, float modelScale) {
        this.app = app;
        this.characterNode = characterNode;
        this.characterModel = characterModel;
        this.modelScale = modelScale;

        initializeArmature();
    }

    /**
     * 初始化骨架系统
     */
    private void initializeArmature() {
        // 查找 SkinningControl
        SkinningControl skinningControl = findControlRecursive(characterModel, SkinningControl.class);
        if (skinningControl == null) {
            System.err.println("[PuppetBoneAttachment] 错误：未找到 SkinningControl");
            return;
        }

        modelArmature = skinningControl.getArmature();
        System.out.println("[PuppetBoneAttachment] 找到骨架，骨骼数量: " + modelArmature.getJointCount());

        // 构建骨骼映射表
        buildJointMap(modelArmature);
    }

    /**
     * 构建骨骼名称到 Joint 对象的映射
     */
    private void buildJointMap(Armature armature) {
        for (int i = 0; i < armature.getJointCount(); i++) {
            Joint joint = armature.getJoint(i);
            jointMap.put(joint.getName(), joint);
            System.out.println("[PuppetBoneAttachment] 骨骼 " + i + ": " + joint.getName());
        }
    }

    /**
     * 加载 puppet 配置并绑定到 3D 骨骼
     *
     * @param puppetConfigPath puppet 配置文件路径
     * @param boneMapping puppet 部件名到 3D 骨骼名的映射
     * @param scale puppet 缩放比例
     */
    public void loadAndAttachPuppet(String puppetConfigPath,
                                   Map<String, String> boneMapping,
                                   float scale) {
        try {
            System.out.println("[PuppetBoneAttachment] 加载 puppet 配置: " + puppetConfigPath);

            // 加载 puppet 配置
            PuppetConfig config = PuppetIO.loadFromResource(puppetConfigPath);

            // 创建 puppet 骨架和渲染器
            puppetSkeleton = new Skeleton(config.getName());
            puppetRenderer = new PuppetRenderer(app, puppetSkeleton);

            // 应用配置
            PuppetIO.applyConfig(config, puppetSkeleton, puppetRenderer);

            // 设置 Billboard 模式为 DISABLED（让部件跟随骨骼旋转）
            puppetRenderer.setBillboardMode(PuppetRenderer.BillboardMode.DISABLED);

            System.out.println("[PuppetBoneAttachment] Puppet 加载成功，部件数量: "
                + puppetSkeleton.getAllBones().size());

            // 为每个 puppet 部件创建绑定
            for (Bone puppetBone : puppetSkeleton.getAllBones()) {
                String puppetPartName = puppetBone.getName();
                System.out.println("[PuppetBoneAttachment] 处理 puppet 部件: " + puppetPartName);

                String targetJointName = boneMapping.get(puppetPartName);

                if (targetJointName == null) {
                    System.out.println("[PuppetBoneAttachment] 跳过未映射的部件: " + puppetPartName);
                    continue;
                }

                Joint targetJoint = jointMap.get(targetJointName);
                if (targetJoint == null) {
                    System.err.println("[PuppetBoneAttachment] 错误：未找到骨骼 '" + targetJointName
                        + "' (映射自 puppet 部件 '" + puppetPartName + "')");
                    continue;
                }

                // 获取 puppet 部件的渲染器
                PuppetPartRenderer partRenderer = puppetRenderer.getPartRenderer(puppetPartName);
                if (partRenderer == null) {
                    System.err.println("[PuppetBoneAttachment] 错误：未找到部件渲染器: " + puppetPartName);
                    continue;
                }

                // 创建一个包装节点来包含部件的 Geometry
                Node partNode = new Node(puppetPartName + "_AttachmentNode");

                // 获取部件的 Geometry
                Geometry partGeometry = partRenderer.getGeometry();
                if (partGeometry == null) {
                    System.err.println("[PuppetBoneAttachment] 错误：部件 Geometry 为空: " + puppetPartName);
                    continue;
                }

                // 从原父节点中分离
                if (partGeometry.getParent() != null) {
                    partGeometry.removeFromParent();
                }

                // 附加到包装节点
                partNode.attachChild(partGeometry);

                // 附加到场景根节点（独立管理）
                app.getRootNode().attachChild(partNode);

                // 创建绑定（默认无偏移，跟随旋转）
                Vector3f offset = Vector3f.ZERO;
                boolean followRotation = true;

                PuppetBoneAttachment attachment = new PuppetBoneAttachment(
                    targetJoint, partNode, partRenderer, offset, scale, followRotation
                );

                attachments.add(attachment);

                System.out.println("[PuppetBoneAttachment] 绑定成功: " + puppetPartName
                    + " -> " + targetJointName);
            }

            System.out.println("[PuppetBoneAttachment] 所有部件绑定完成，总数: " + attachments.size());

        } catch (Exception e) {
            System.err.println("[PuppetBoneAttachment] 加载 puppet 失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 更新所有绑定（每帧调用）
     */
    public void update(float tpf) {
        // 注意：这里不能调用 puppetRenderer.update(tpf)。PuppetRenderer.update()
        // 会驱动内部PuppetPartRenderer.updateTransform()，按puppet自己的Skeleton/Bone
        // （.puppet配置的restPosition层级，如Body->Neck偏移+0.4->Head再偏移+0.25）
        // 重新设置partGeometry的localTranslation——而partGeometry已经被
        // loadAndAttachPuppet()摘出来挂到了我们自己创建的partNode下面，
        // partNode的世界坐标由下面的attachment.update()按3D骨骼位置精确计算。
        // 两者同时生效会导致partGeometry在partNode正确位置基础上，又叠加一层
        // puppet自身骨架的局部偏移，表现为puppet部件整体比3D骨骼位置更高/漂浮。

        // 更新所有绑定
        for (PuppetBoneAttachment attachment : attachments) {
            attachment.update(characterNode, modelScale);
        }
    }

    /**
     * 设置所有 puppet 部件的可见性
     */
    public void setVisible(boolean visible) {
        for (PuppetBoneAttachment attachment : attachments) {
            Node partNode = attachment.getPuppetPartNode();
            if (partNode != null) {
                partNode.setCullHint(visible ? Spatial.CullHint.Dynamic : Spatial.CullHint.Always);
            }
        }
    }

    /**
     * 清理资源
     */
    public void cleanup() {
        for (PuppetBoneAttachment attachment : attachments) {
            Node partNode = attachment.getPuppetPartNode();
            if (partNode != null && partNode.getParent() != null) {
                partNode.removeFromParent();
            }
        }
        attachments.clear();
    }

    /**
     * 递归查找 Control
     */
    @SuppressWarnings("unchecked")
    private <T> T findControlRecursive(Spatial spatial, Class<T> controlClass) {
        Object control = spatial.getControl((Class) controlClass);
        if (control != null) {
            return (T) control;
        }
        if (spatial instanceof Node) {
            for (Spatial child : ((Node) spatial).getChildren()) {
                T found = findControlRecursive(child, controlClass);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    /**
     * 获取 puppet 渲染器
     */
    public PuppetRenderer getPuppetRenderer() {
        return puppetRenderer;
    }

    /**
     * 获取绑定列表
     */
    public List<PuppetBoneAttachment> getAttachments() {
        return attachments;
    }
}
