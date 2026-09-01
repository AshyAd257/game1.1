package com.Hecate.character;

import com.jme3.animation.*;
import com.jme3.app.SimpleApplication;
import com.jme3.math.*;
import com.jme3.scene.*;
import com.Hecate.puppet.core.*;
import com.Hecate.puppet.config.*;
import java.util.*;

/**
 * 混合角色渲染器
 * 结合3D骨骼模型和2D木偶部件
 *
 * 功能特性：
 * - 支持从Blender导入的3D骨骼动画模型
 * - 将2D木偶部件挂载到3D骨骼上
 * - 支持换装系统（运行时替换部件）
 * - 支持模型缩放调整
 * - 支持多方向贴图自动切换
 */
public class HybridCharacterRenderer {

    private final SimpleApplication app;
    private final Node rootNode;

    // 3D模型部分
    private Node modelNode;
    private AnimControl animControl;
    private SkeletonControl skeletonControl;
    private AnimChannel animChannel;

    // 木偶部件部分
    private final Map<String, PuppetPartRenderer> partRenderers;
    private final Map<String, Node> attachmentNodes; // 骨骼挂点节点

    // 配置加载器
    private final CharacterConfigLoader configLoader;

    // 当前皮肤
    private CharacterSkin currentSkin;
    private float modelScale = 1.0f;

    // 是否隐藏3D模型（只显示2D部件）
    private boolean hideModel = true;

    /**
     * 构造函数
     * @param app SimpleApplication实例
     */
    public HybridCharacterRenderer(SimpleApplication app) {
        this(app, "");
    }

    /**
     * 构造函数
     * @param app SimpleApplication实例
     * @param configBasePath 配置文件基础路径（例如 "Characters/"）
     */
    public HybridCharacterRenderer(SimpleApplication app, String configBasePath) {
        this.app = app;
        this.rootNode = new Node("HybridCharacter");
        this.partRenderers = new HashMap<>();
        this.attachmentNodes = new HashMap<>();
        this.configLoader = new CharacterConfigLoader(configBasePath);
    }

    /**
     * 加载角色皮肤
     * @param skinId 皮肤ID
     * @return 是否加载成功
     */
    public boolean loadSkin(String skinId) {
        CharacterSkin skin = configLoader.loadSkin(skinId);
        if (skin == null) {
            System.err.println("Failed to load skin: " + skinId);
            return false;
        }
        return loadSkin(skin);
    }

    /**
     * 加载角色皮肤
     * @param skin 皮肤配置对象
     * @return 是否加载成功
     */
    public boolean loadSkin(CharacterSkin skin) {
        // 清理旧皮肤
        cleanup();

        this.currentSkin = skin;
        this.modelScale = skin.getModelScale();

        // 1. 加载3D基础模型
        if (!loadBaseModel(skin.getBaseModelPath())) {
            System.err.println("Failed to load base model: " + skin.getBaseModelPath());
            return false;
        }

        // 2. 创建所有部件的挂点
        createAttachmentNodes();

        // 3. 加载并挂载所有部件
        for (CharacterSkin.SkinPartSlot slot : skin.getPartSlots()) {
            attachPart(slot);
        }

        return true;
    }

    /**
     * 加载3D基础模型
     * @param modelPath 模型路径
     * @return 是否加载成功
     */
    private boolean loadBaseModel(String modelPath) {
        // 清理旧模型
        if (modelNode != null) {
            modelNode.removeFromParent();
        }

        try {
            // 加载新模型
            modelNode = (Node) app.getAssetManager().loadModel(modelPath);
            modelNode.setLocalScale(modelScale);

            // 获取动画控制
            animControl = modelNode.getControl(AnimControl.class);
            skeletonControl = modelNode.getControl(SkeletonControl.class);

            if (animControl != null) {
                animChannel = animControl.createChannel();
            }

            // 根据配置决定是否隐藏3D模型
            if (hideModel) {
                makeModelInvisible(modelNode);
            }

            rootNode.attachChild(modelNode);
            return true;

        } catch (Exception e) {
            System.err.println("Failed to load model: " + modelPath);
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 将模型设为不可见（保留骨骼功能）
     * 只隐藏几何体，保留骨骼和动画
     */
    private void makeModelInvisible(Node node) {
        for (Spatial child : node.getChildren()) {
            if (child instanceof Geometry) {
                child.setCullHint(Spatial.CullHint.Always);
            } else if (child instanceof Node) {
                makeModelInvisible((Node) child);
            }
        }
    }

    /**
     * 创建所有骨骼的挂点节点
     */
    private void createAttachmentNodes() {
        if (skeletonControl == null) {
            System.err.println("No SkeletonControl found on model");
            return;
        }

        Skeleton skeleton = skeletonControl.getSkeleton();

        // 为每个骨骼创建挂点
        for (int i = 0; i < skeleton.getBoneCount(); i++) {
            Bone bone = skeleton.getBone(i);
            String boneName = bone.getName();

            // 创建挂点节点
            Node attachNode = skeletonControl.getAttachmentsNode(boneName);
            attachmentNodes.put(boneName, attachNode);
        }

        System.out.println("Created " + attachmentNodes.size() + " attachment nodes");
    }

    /**
     * 挂载一个部件到指定骨骼
     * @param slot 皮肤部件槽位
     */
    private void attachPart(CharacterSkin.SkinPartSlot slot) {
        // 加载部件定义
        PuppetPartDefinition partDef = configLoader.loadPartDefinition(slot.getPartId());
        if (partDef == null) {
            System.err.println("Failed to load part: " + slot.getPartId());
            return;
        }

        // 获取目标骨骼的挂点节点
        Node attachNode = attachmentNodes.get(partDef.getTargetBoneName());
        if (attachNode == null) {
            System.err.println("Bone not found: " + partDef.getTargetBoneName());
            System.err.println("Available bones: " + attachmentNodes.keySet());
            return;
        }

        // 创建部件的容器节点
        Node partContainer = new Node(slot.getPartId() + "_Container");

        // 应用偏移
        Vec3Config offset = slot.getCustomOffset() != null
                ? slot.getCustomOffset()
                : partDef.getAttachmentOffset();
        if (offset != null) {
            partContainer.setLocalTranslation(
                    offset.getX(),
                    offset.getY(),
                    offset.getZ()
            );
        }

        // 应用旋转
        Vec3Config rotation = partDef.getAttachmentRotation();
        if (rotation != null) {
            Quaternion rot = new Quaternion();
            rot.fromAngles(
                    rotation.getX() * FastMath.DEG_TO_RAD,
                    rotation.getY() * FastMath.DEG_TO_RAD,
                    rotation.getZ() * FastMath.DEG_TO_RAD
            );
            partContainer.setLocalRotation(rot);
        }

        // 应用缩放
        float scale = slot.getCustomScale() != null
                ? slot.getCustomScale()
                : partDef.getScale();
        partContainer.setLocalScale(scale);

        // 创建木偶部件渲染器
        PuppetPartRenderer partRenderer = createPartRenderer(partDef, partContainer);
        partRenderers.put(slot.getPartId(), partRenderer);

        // 挂载到骨骼节点
        attachNode.attachChild(partContainer);

        System.out.println("Attached part: " + slot.getPartId() + " to bone: " + partDef.getTargetBoneName());
    }

    /**
     * 创建木偶部件渲染器
     * @param partDef 部件定义
     * @param parentNode 父节点
     * @return 部件渲染器
     */
    private PuppetPartRenderer createPartRenderer(
            PuppetPartDefinition partDef,
            Node parentNode) {

        // 创建临时Bone对象（适配现有的PuppetPartRenderer）
        com.Hecate.puppet.core.Bone bone = new com.Hecate.puppet.core.Bone(
                partDef.getPartId()
        );

        // 设置尺寸
        bone.setWidth(partDef.getWidth());
        bone.setHeight(partDef.getHeight());

        // 设置多方向贴图
        for (Map.Entry<String, String> entry : partDef.getDirectionTextures().entrySet()) {
            bone.setDirectionTexture(entry.getKey(), entry.getValue());
        }

        // 设置优先级
        bone.setPriority(partDef.getRenderPriority());

        // 创建渲染器
        PuppetPartRenderer renderer = new PuppetPartRenderer(
                app,
                bone,
                parentNode,
                partDef.getWidth(),
                partDef.getHeight()
        );

        renderer.initialize();

        return renderer;
    }

    /**
     * 更新（每帧调用）
     * @param tpf Time per frame
     */
    public void update(float tpf) {
        // 更新所有部件渲染器
        for (PuppetPartRenderer renderer : partRenderers.values()) {
            renderer.updateTransform();
        }
    }

    /**
     * 播放动画
     * @param animName 动画名称
     * @param loop 是否循环播放
     */
    public void playAnimation(String animName, boolean loop) {
        if (animChannel != null && animControl != null) {
            if (animControl.getAnimationNames().contains(animName)) {
                animChannel.setAnim(animName);
                animChannel.setLoopMode(loop ? LoopMode.Loop : LoopMode.DontLoop);
            } else {
                System.err.println("Animation not found: " + animName);
                System.err.println("Available animations: " + animControl.getAnimationNames());
            }
        }
    }

    /**
     * 获取所有可用的动画名称
     * @return 动画名称集合
     */
    public Collection<String> getAvailableAnimations() {
        if (animControl != null) {
            return animControl.getAnimationNames();
        }
        return new ArrayList<>();
    }

    /**
     * 更换部件（换装）
     * @param slotType 部件槽位类型
     * @param newPartId 新部件ID
     * @return 是否更换成功
     */
    public boolean replacePart(PuppetPartDefinition.PartType slotType, String newPartId) {
        if (currentSkin == null) {
            System.err.println("No skin loaded");
            return false;
        }

        // 1. 找到对应槽位的旧部件
        CharacterSkin.SkinPartSlot oldSlot = currentSkin.getPartSlots().stream()
                .filter(slot -> slot.getSlotType() == slotType)
                .findFirst()
                .orElse(null);

        if (oldSlot != null) {
            // 2. 移除旧部件
            PuppetPartRenderer oldRenderer = partRenderers.remove(oldSlot.getPartId());
            if (oldRenderer != null) {
                oldRenderer.cleanup();
            }

            // 3. 从皮肤配置中移除旧槽位
            currentSkin.getPartSlots().remove(oldSlot);
        }

        // 4. 创建新槽位
        CharacterSkin.SkinPartSlot newSlot = new CharacterSkin.SkinPartSlot();
        newSlot.setSlotType(slotType);
        newSlot.setPartId(newPartId);

        // 5. 挂载新部件
        attachPart(newSlot);

        // 6. 添加到皮肤配置
        currentSkin.getPartSlots().add(newSlot);

        return true;
    }

    /**
     * 移除指定槽位的部件
     * @param slotType 部件槽位类型
     * @return 是否移除成功
     */
    public boolean removePart(PuppetPartDefinition.PartType slotType) {
        if (currentSkin == null) {
            return false;
        }

        // 找到对应槽位的部件
        CharacterSkin.SkinPartSlot slot = currentSkin.getPartSlots().stream()
                .filter(s -> s.getSlotType() == slotType)
                .findFirst()
                .orElse(null);

        if (slot != null) {
            // 移除渲染器
            PuppetPartRenderer renderer = partRenderers.remove(slot.getPartId());
            if (renderer != null) {
                renderer.cleanup();
            }

            // 从皮肤配置中移除
            currentSkin.getPartSlots().remove(slot);
            return true;
        }

        return false;
    }

    /**
     * 设置模型缩放（用于调整3D模型与2D部件的比例）
     * @param scale 缩放值
     */
    public void setModelScale(float scale) {
        this.modelScale = scale;
        if (modelNode != null) {
            modelNode.setLocalScale(scale);
        }
        if (currentSkin != null) {
            currentSkin.setModelScale(scale);
        }
    }

    /**
     * 设置是否隐藏3D模型（只显示骨骼和2D部件）
     * @param hide true=隐藏3D模型，false=显示3D模型
     */
    public void setHideModel(boolean hide) {
        this.hideModel = hide;
        if (modelNode != null) {
            if (hide) {
                makeModelInvisible(modelNode);
            } else {
                makeModelVisible(modelNode);
            }
        }
    }

    /**
     * 将模型设为可见
     */
    private void makeModelVisible(Node node) {
        for (Spatial child : node.getChildren()) {
            if (child instanceof Geometry) {
                child.setCullHint(Spatial.CullHint.Inherit);
            } else if (child instanceof Node) {
                makeModelVisible((Node) child);
            }
        }
    }

    /**
     * 附加到场景
     * @param sceneNode 场景节点
     */
    public void attachToScene(Node sceneNode) {
        sceneNode.attachChild(rootNode);
    }

    /**
     * 从场景移除
     */
    public void detachFromScene() {
        rootNode.removeFromParent();
    }

    /**
     * 清理资源
     */
    public void cleanup() {
        // 清理所有部件渲染器
        for (PuppetPartRenderer renderer : partRenderers.values()) {
            renderer.cleanup();
        }
        partRenderers.clear();

        // 清理挂点节点
        attachmentNodes.clear();

        // 移除模型节点
        if (modelNode != null) {
            modelNode.removeFromParent();
            modelNode = null;
        }

        animControl = null;
        skeletonControl = null;
        animChannel = null;
    }

    // ========== Getters ==========

    public Node getRootNode() {
        return rootNode;
    }

    public CharacterSkin getCurrentSkin() {
        return currentSkin;
    }

    public float getModelScale() {
        return modelScale;
    }

    public boolean isHideModel() {
        return hideModel;
    }

    public CharacterConfigLoader getConfigLoader() {
        return configLoader;
    }

    public Map<String, PuppetPartRenderer> getPartRenderers() {
        return new HashMap<>(partRenderers);
    }

    public PuppetPartRenderer getPartRenderer(String partId) {
        return partRenderers.get(partId);
    }

    /**
     * 设置角色位置
     */
    public void setPosition(Vector3f position) {
        rootNode.setLocalTranslation(position);
    }

    /**
     * 获取角色位置
     */
    public Vector3f getPosition() {
        return rootNode.getLocalTranslation();
    }

    /**
     * 设置角色旋转
     */
    public void setRotation(Quaternion rotation) {
        rootNode.setLocalRotation(rotation);
    }

    /**
     * 获取角色旋转
     */
    public Quaternion getRotation() {
        return rootNode.getLocalRotation();
    }
}
