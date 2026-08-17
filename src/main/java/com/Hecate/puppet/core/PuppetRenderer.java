package com.Hecate.puppet.core;

import com.jme3.app.SimpleApplication;
import com.jme3.math.Vector3f;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.scene.Node;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Line;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.light.DirectionalLight;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 木偶渲染器
 * 管理整个木偶的渲染，包括所有骨骼部件
 */
public class PuppetRenderer {

    /**
     * Billboard渲染模式
     */
    public enum BillboardMode {
        /** 禁用Billboard - 部件保持固定朝向，适合3D立体模型 */
        DISABLED,
        /** 统一Billboard - 整个木偶像纸人一样整体朝向摄像机 */
        UNIFIED,
        /** 独立Billboard - 每个部件独立朝向摄像机（不推荐，会导致诡异效果） */
        INDEPENDENT
    }

    private final SimpleApplication app;
    private final Skeleton skeleton;
    private final Node puppetNode;

    private final Map<String, PuppetPartRenderer> partRenderers;
    private final List<PuppetPartRenderer> allRenderers;

    /**
     * 多光源处理模式
     */
    public enum MultiLightMode {
        /** 只使用最强光源（默认，适合我的世界风格的室外场景：强太阳光+若干弱点光源） */
        PRIMARY_ONLY,
        /** 多光源时垂直压缩为脚底投影（适合室内/多光源密集场景） */
        AUTO_COMPRESS
    }

    // 阴影投射系统（固定朝向的阴影投射几何体）
    private final Node shadowCasterNode;
    private final Map<String, Geometry> shadowCasters;  // 每个部件对应的阴影投射几何体
    private final Map<String, String> shadowCasterDirections;  // 跟踪每个shadow caster的当前方向

    // 【新增】光源缓存系统（性能优化：太阳光基本不动，cachedLightYaw可能整局游戏只算一次）
    private DirectionalLight cachedPrimaryLight = null;
    private float cachedLightYaw = 0f;
    private boolean lightCacheDirty = true;
    private MultiLightMode multiLightMode = MultiLightMode.PRIMARY_ONLY;

    // 【预留接口】控制面板可以通过此开关关闭光源对齐功能
    // 默认启用，使用反向旋转补偿让阴影始终基于"不会穿帮的初始姿势"
    private boolean useLightAlignedShadow = true;

    private boolean initialized = false;

    // 默认部件尺寸
    private float defaultPartWidth = 1.0f;
    private float defaultPartHeight = 1.0f;

    // 骨骼连接线
    private final Node boneConnectionsNode;
    private final Map<String, Geometry> boneConnectionLines;
    private boolean showBoneConnections = false; // 默认隐藏骨骼连接线

    // 自由骨骼物理系统
    private final Map<String, FreeBonePhysics> freeBonePhysics;

    // 优先级排序标志
    private boolean needsPrioritySort = false;

    // Billboard渲染模式
    private BillboardMode billboardMode = BillboardMode.UNIFIED;

    // 统一Billboard模式 - 让整个木偶像纸人一样整体朝向相机（向后兼容）
    @Deprecated
    private boolean unifiedBillboard = true;
    private Quaternion unifiedBillboardRotation = new Quaternion();

    // 调试帧计数器（每60帧打印一次调试信息）
    private int debugFrameCounter = 0;

    // 自动方向切换开关（默认启用，游戏中可以禁用以使用手动控制）
    private boolean autoDirectionSwitch = true;

    // 手动旋转角度（弧度）- 用于"圆盘旋转"效果
    private float manualRotationAngle = 0f;

    public PuppetRenderer(SimpleApplication app, Skeleton skeleton) {
        this.app = app;
        this.skeleton = skeleton;
        this.puppetNode = new Node(skeleton.getName() + "_Node");
        this.partRenderers = new HashMap<>();
        this.allRenderers = new ArrayList<>();
        this.boneConnectionsNode = new Node(skeleton.getName() + "_Connections");
        this.boneConnectionLines = new HashMap<>();
        this.freeBonePhysics = new HashMap<>();

        // 初始化阴影投射系统
        this.shadowCasterNode = new Node(skeleton.getName() + "_ShadowCasters");
        this.shadowCasters = new HashMap<>();
        this.shadowCasterDirections = new HashMap<>();

        // 注意：boneConnectionsNode不附加到puppetNode，而是在attachToScene时单独附加
        // 这样可以直接使用世界坐标创建连接线
    }

    /**
     * 初始化渲染器
     * 为每个骨骼创建对应的部件渲染器
     */
    public void initialize() {
        if (initialized) {

            // 即使已经初始化，也要确保 shadow casters 存在
            if (shadowCasters.isEmpty() && !allRenderers.isEmpty()) {

                createShadowCasters();
            } else {

            }
            return;
        }

        // 为每个骨骼创建渲染器
        for (Bone bone : skeleton.getAllBones()) {
            PuppetPartRenderer partRenderer = new PuppetPartRenderer(
                    app,
                    bone,
                    puppetNode,
                    defaultPartWidth,
                    defaultPartHeight
            );

            // partRenderer.setEditorMode(editorMode); // 设置编辑器模式 - REMOVED
            partRenderer.initialize();
            partRenderer.setParentRenderer(this); // 设置父渲染器引用，用于统一billboard
            partRenderers.put(bone.getName(), partRenderer);
            allRenderers.add(partRenderer);

            // 如果是自由骨骼，创建物理系统
            if (bone.isFreeBone()) {
                FreeBonePhysics physics = new FreeBonePhysics(bone);
                // 应用骨骼中保存的物理参数
                physics.setMass(bone.getPhysMass());
                physics.setDamping(bone.getPhysDamping());
                physics.setStiffness(bone.getPhysStiffness());
                physics.setGravityStrength(bone.getPhysGravityStrength());
                physics.setMaxSwingAngle(bone.getPhysMaxSwingAngle());
                physics.setMaxVelocity(bone.getPhysMaxVelocity());
                physics.initializeParentPosition();
                freeBonePhysics.put(bone.getName(), physics);
            }
        }

        // 按优先级排序并附加节点
        sortByPriority();

        // 创建阴影投射几何体
        createShadowCasters();

        initialized = true;
    }

    /**
     * 更新所有部件的变换
     */
    public void update(float tpf) {
        if (!initialized) {
            return;
        }

        // 【紧急修复】如果 shadowCasters 为空但 allRenderers 不为空，立即创建
        if (shadowCasters.isEmpty() && !allRenderers.isEmpty()) {

            createShadowCasters();
        }

        // 如果需要重新排序，先执行排序
        if (needsPrioritySort) {
            sortByPriority();
            needsPrioritySort = false;
        }

        // 如果启用统一Billboard，计算整体朝向相机的旋转
        if (unifiedBillboard) {
            updateUnifiedBillboardRotation();
        }

        // 计算相机方向并更新所有骨骼的方向贴图（仅在启用自动方向切换时）
        if (autoDirectionSwitch) {
            calculateAndSetCameraDirection();
        }

        // 更新自由骨骼的物理模拟
        for (FreeBonePhysics physics : freeBonePhysics.values()) {
            physics.update(tpf);
        }

        // 更新摇摆效果（应用正弦波旋转）
        float currentTime = app.getTimer().getTimeInSeconds();
        for (Bone bone : skeleton.getAllBones()) {
            if (bone.isSwingEnabled()) {
                // 计算当前摇摆角度
                float swingAngle = bone.calculateSwingAngle(currentTime);

                // 创建绕摇摆轴的旋转
                Quaternion swingRotation = new Quaternion();
                swingRotation.fromAngleAxis(swingAngle, bone.getSwingAxis());

                // 应用摇摆旋转到骨骼的局部旋转
                // 注意：这里是乘法组合，不是替换
                Quaternion currentRotation = bone.getLocalRotation();
                bone.setLocalRotation(currentRotation.mult(swingRotation));
            }
        }

        // 更新所有部件的世界变换
        for (PuppetPartRenderer renderer : allRenderers) {
            renderer.updateTransform();
        }

        // 更新阴影投射几何体的位置（跟随部件位置，但保持固定朝向）
        updateShadowCasterPositions();

        // 更新骨骼连接线位置（需要每帧更新，因为骨骼会移动）
        if (showBoneConnections) {
            updateBoneConnectionPositions();
        }
    }

    /**
     * 更新统一billboard旋转
     * 让整个木偶像纸人一样整体朝向相机
     */
    private void updateUnifiedBillboardRotation() {
        // 获取相机位置和木偶位置
        Vector3f camPos = app.getCamera().getLocation();
        Vector3f puppetPos = puppetNode.getWorldTranslation();

        // 计算从木偶指向相机的方向（完整3D方向，不忽略Y轴）
        Vector3f camDir = camPos.subtract(puppetPos);
        if (camDir.lengthSquared() < 0.0001f) {
            // 相机和木偶重合，使用默认朝向
            unifiedBillboardRotation.loadIdentity();
            return;
        }
        camDir.normalizeLocal();

        // 计算旋转：让Z轴（纸人的正面）朝向相机
        // 使用完整的3D朝向，支持垂直视角

        // 选择一个合适的"上"方向
        Vector3f up;
        if (Math.abs(camDir.y) > 0.99f) {
            // 相机几乎在正上方或正下方，使用X轴作为"上"方向
            up = Vector3f.UNIT_X;
        } else {
            // 其他情况使用Y轴作为"上"方向
            up = Vector3f.UNIT_Y;
        }

        // 计算局部坐标系
        Vector3f left = up.cross(camDir).normalizeLocal();
        Vector3f realUp = camDir.cross(left).normalizeLocal();

        // 构建旋转矩阵并转换为四元数
        // left = 局部X轴, realUp = 局部Y轴, camDir = 局部Z轴（指向相机）
        unifiedBillboardRotation.fromAxes(left, realUp, camDir);
    }

    /**
     * 计算相机方向并更新所有骨骼的方向贴图
     * 根据相机位置自动切换骨骼的显示方向（前后左右上下）
     */
    private void calculateAndSetCameraDirection() {
        if (skeleton == null || app == null || app.getCamera() == null) {
            return;
        }

        // 获取相机位置和木偶位置
        Vector3f camPos = app.getCamera().getLocation();
        Vector3f puppetPos = puppetNode.getWorldTranslation();

        // 计算从木偶指向相机的方向向量
        Vector3f camDir = camPos.subtract(puppetPos);
        if (camDir.lengthSquared() < 0.0001f) {
            return; // 相机和木偶重合，不更新
        }
        camDir.normalizeLocal();

        // 判断当前相机方向
        Bone.Direction newDirection;

        // 首先检查垂直角度（优先级更高）
        float verticalThreshold = 0.7f; // cos(45°) ≈ 0.707

        if (camDir.y > verticalThreshold) {
            // 从上往下看 → 看到木偶的顶部
            newDirection = Bone.Direction.UP;
        } else if (camDir.y < -verticalThreshold) {
            // 从下往上看 → 看到木偶的底部
            newDirection = Bone.Direction.DOWN;
        } else {
            // 水平视角：检查水平方向
            // 计算水平方向向量（忽略Y轴）
            Vector3f horizontalDir = new Vector3f(camDir.x, 0, camDir.z);
            if (horizontalDir.lengthSquared() < 0.0001f) {
                return; // 完全垂直，无法判断水平方向
            }
            horizontalDir.normalizeLocal();

            // 使用点积判断方向
            // Z轴正方向 = FRONT (0, 0, 1)
            // Z轴负方向 = BACK (0, 0, -1)
            // X轴正方向 = RIGHT (1, 0, 0)
            // X轴负方向 = LEFT (-1, 0, 0)

            float dotZ = horizontalDir.z;
            float dotX = horizontalDir.x;

            // 判断主要方向
            if (Math.abs(dotZ) > Math.abs(dotX)) {
                // Z轴方向占主导
                if (dotZ > 0) {
                    newDirection = Bone.Direction.FRONT;
                } else {
                    newDirection = Bone.Direction.BACK;
                }
            } else {
                // X轴方向占主导
                if (dotX > 0) {
                    newDirection = Bone.Direction.RIGHT;
                } else {
                    newDirection = Bone.Direction.LEFT;
                }
            }
        }

        // 更新所有骨骼的当前方向
        for (Bone bone : skeleton.getAllBones()) {
            // 设置骨骼的当前方向
            bone.setCurrentDirection(newDirection.getKey());

            // 获取部件渲染器并更新贴图
            PuppetPartRenderer partRenderer = partRenderers.get(bone.getName());
            if (partRenderer != null) {
                partRenderer.updateTextureFromBone();
            }
        }

        // 方向改变后，需要重新排序（因为不同方向的优先级可能不同）
        // 例如：鼻子在正面优先级高，在侧面优先级低；耳朵则相反
        requestPrioritySort();
    }

    /**
     * 设置木偶的世界位置
     */
    public void setWorldPosition(Vector3f position) {
        puppetNode.setLocalTranslation(position);
    }

    /**
     * 获取指定骨骼的部件渲染器
     */
    public PuppetPartRenderer getPartRenderer(String boneName) {
        return partRenderers.get(boneName);
    }

    /**
     * 设置所有部件的可见性
     */
    public void setVisible(boolean visible) {
        for (PuppetPartRenderer renderer : allRenderers) {
            renderer.setVisible(visible);
        }
    }

    /**
     * 设置默认部件尺寸
     */
    public void setDefaultPartSize(float width, float height) {
        this.defaultPartWidth = width;
        this.defaultPartHeight = height;
    }

    /**
     * 将木偶节点附加到场景
     */
    public void attachToScene(Node sceneNode) {

        sceneNode.attachChild(puppetNode);
        sceneNode.attachChild(boneConnectionsNode); // 单独附加连接线节点到场景根
        sceneNode.attachChild(shadowCasterNode); // 附加阴影投射节点到场景根

    }

    /**
     * 从场景移除木偶节点
     */
    public void detachFromScene() {
        puppetNode.removeFromParent();
        boneConnectionsNode.removeFromParent();
        shadowCasterNode.removeFromParent();
    }

    /**
     * 添加部件渲染器
     */
    public void addPartRenderer(Bone bone) {
        addPartRenderer(bone, defaultPartWidth, defaultPartHeight);
    }

    /**
     * 添加部件渲染器（指定尺寸）
     */
    public PuppetPartRenderer addPartRenderer(Bone bone, float width, float height) {
        if (partRenderers.containsKey(bone.getName())) {
            return partRenderers.get(bone.getName());
        }

        PuppetPartRenderer partRenderer = new PuppetPartRenderer(
                app,
                bone,
                puppetNode,
                width,
                height
        );

        // partRenderer.setEditorMode(editorMode); // 设置编辑器模式 - REMOVED
        partRenderer.initialize();
        partRenderer.setParentRenderer(this); // 设置父渲染器引用，用于统一billboard
        partRenderers.put(bone.getName(), partRenderer);
        allRenderers.add(partRenderer);
        return partRenderer;
    }

    /**
     * 移除部件渲染器
     */
    public void removePartRenderer(String boneName) {
        PuppetPartRenderer renderer = partRenderers.get(boneName);
        if (renderer == null) {
            return;
        }

        renderer.cleanup();
        partRenderers.remove(boneName);
        allRenderers.remove(renderer);
    }

    /**
     * 创建骨骼连接线（只在初始化或开关显示时调用）
     */
    private void createBoneConnections() {
        // 清除旧的连接线
        boneConnectionsNode.detachAllChildren();
        boneConnectionLines.clear();

        int count = 0;
        // 为每个有父骨骼的骨骼创建连接线
        for (Bone bone : skeleton.getAllBones()) {
            Bone parent = bone.getParent();
            if (parent != null) {
                createBoneConnectionLine(bone, parent);
                count++;
            }
        }
    }

    /**
     * 更新骨骼连接线的位置（每帧调用，因为骨骼会移动）
     */
    private void updateBoneConnectionPositions() {
        if (boneConnectionLines.isEmpty()) {
            // 第一次调用，需要先创建连接线
            createBoneConnections();
            return;
        }

        // 更新每条连接线的位置
        for (Bone bone : skeleton.getAllBones()) {
            Bone parent = bone.getParent();
            if (parent != null) {
                Geometry lineGeometry = boneConnectionLines.get(bone.getName());
                if (lineGeometry != null) {
                    // 从PuppetPartRenderer获取最终世界位置（包括offset和自定义旋转）
                    PuppetPartRenderer childRenderer = partRenderers.get(bone.getName());
                    PuppetPartRenderer parentRenderer = partRenderers.get(parent.getName());

                    if (childRenderer != null && parentRenderer != null) {
                        Vector3f childWorldPos = childRenderer.getFinalWorldPosition();
                        Vector3f parentWorldPos = parentRenderer.getFinalWorldPosition();

                        // 更新线条的顶点位置
                        Line line = new Line(parentWorldPos, childWorldPos);
                        lineGeometry.setMesh(line);
                    }
                }
            }
        }
    }

    /**
     * 创建骨骼连接线
     */
    private void createBoneConnectionLine(Bone child, Bone parent) {
        // 从PuppetPartRenderer获取最终世界位置（包括offset和自定义旋转）
        PuppetPartRenderer childRenderer = partRenderers.get(child.getName());
        PuppetPartRenderer parentRenderer = partRenderers.get(parent.getName());

        if (childRenderer == null || parentRenderer == null) {
            return;
        }

        Vector3f childWorldPos = childRenderer.getFinalWorldPosition();
        Vector3f parentWorldPos = parentRenderer.getFinalWorldPosition();

        // 创建连接线
        Line line = new Line(parentWorldPos, childWorldPos);
        Geometry lineGeometry = new Geometry(child.getName() + "_Connection", line);

        // 创建材质 - 根据骨骼类型选择颜色
        Material lineMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");

        // 自由骨骼使用蓝色，连接骨骼使用红色
        ColorRGBA lineColor;
        if (child.isFreeBone()) {
            lineColor = new ColorRGBA(0f, 0.5f, 1f, 1.0f); // 蓝色
        } else {
            lineColor = new ColorRGBA(1f, 0f, 0f, 1.0f); // 红色
        }

        lineMat.setColor("Color", lineColor);
        lineMat.getAdditionalRenderState().setLineWidth(5f); // 更粗的线宽便于观察
        lineMat.getAdditionalRenderState().setDepthTest(false); // 禁用深度测试，始终可见
        lineMat.getAdditionalRenderState().setDepthWrite(false); // 禁用深度写入
        lineGeometry.setMaterial(lineMat);

        // 设置渲染队列为Translucent，确保在所有不透明物体之后渲染
        lineGeometry.setQueueBucket(com.jme3.renderer.queue.RenderQueue.Bucket.Translucent);

        // 添加到连接线节点（直接附加到场景根）
        boneConnectionsNode.attachChild(lineGeometry);
        boneConnectionLines.put(child.getName(), lineGeometry);
    }

    /**
     * 设置是否显示骨骼连接线
     */
    public void setShowBoneConnections(boolean show) {
        this.showBoneConnections = show;
        if (show) {
            createBoneConnections();
        } else {
            boneConnectionsNode.detachAllChildren();
            boneConnectionLines.clear();
        }
    }

    /**
     * 获取是否显示骨骼连接线
     */
    public boolean isShowBoneConnections() {
        return showBoneConnections;
    }

    /**
     * 刷新骨骼连接线（当骨骼层级关系改变时调用）
     */
    public void refreshBoneConnections() {
        if (showBoneConnections) {
            createBoneConnections();
        }
    }

    /**
     * 为骨骼添加自由骨骼物理系统（运行时动态添加）
     * @param bone 要添加物理的骨骼
     */
    public void addFreeBonePhysics(Bone bone) {
        if (bone == null || !bone.isFreeBone()) {
            return;
        }

        // 如果已经存在物理系统，先移除
        if (freeBonePhysics.containsKey(bone.getName())) {
            freeBonePhysics.remove(bone.getName());
        }

        // 创建新的物理系统
        FreeBonePhysics physics = new FreeBonePhysics(bone);
        // 应用骨骼中保存的物理参数
        physics.setMass(bone.getPhysMass());
        physics.setDamping(bone.getPhysDamping());
        physics.setStiffness(bone.getPhysStiffness());
        physics.setGravityStrength(bone.getPhysGravityStrength());
        physics.setMaxSwingAngle(bone.getPhysMaxSwingAngle());
        physics.setMaxVelocity(bone.getPhysMaxVelocity());
        physics.initializeParentPosition();
        freeBonePhysics.put(bone.getName(), physics);
    }

    /**
     * 移除骨骼的自由骨骼物理系统
     * @param bone 要移除物理的骨骼
     */
    public void removeFreeBonePhysics(Bone bone) {
        if (bone == null) {
            return;
        }
        freeBonePhysics.remove(bone.getName());
    }

    /**
     * 按优先级排序部件渲染器
     * 优先级越高，渲染越靠后（会覆盖在其他部件上面）
     */
    private void sortByPriority() {
        // 调试输出：排序前的优先度

        for (PuppetPartRenderer renderer : allRenderers) {
            Bone bone = renderer.getBone();

        }

        // 按照当前方向的priority从小到大排序
        // 使用getCurrentDirectionPriority()而不是getPriority()，这样会考虑方向优先度
        allRenderers.sort(Comparator.comparingInt(renderer -> renderer.getBone().getCurrentDirectionPriority()));

        // 调试输出：排序后的顺序

        // 重新附加所有geometry到puppetNode，确保渲染顺序正确
        // 先detach所有
        for (PuppetPartRenderer renderer : allRenderers) {
            if (renderer.getGeometry() != null) {
                renderer.getGeometry().removeFromParent();
            }
        }

        // 按排序后的顺序重新attach（优先级低的先attach，优先级高的后attach）
        // 同时设置Z偏移，确保分层效果
        float zOffsetPerPriority = 0.01f; // 每个优先级单位的Z偏移量

        for (int i = 0; i < allRenderers.size(); i++) {
            PuppetPartRenderer renderer = allRenderers.get(i);
            if (renderer.getGeometry() != null) {
                puppetNode.attachChild(renderer.getGeometry());

                // 获取当前优先度
                int priority = renderer.getBone().getCurrentDirectionPriority();

                // 计算Z偏移：优先度越高，Z值越大（越靠近摄像机）
                // 使用priority直接作为偏移，每个优先度单位对应zOffsetPerPriority的Z偏移
                float zOffsetValue = priority * zOffsetPerPriority;

                // 设置渲染器的Z偏移（会在updateTransform()中应用）
                renderer.setPriorityZOffset(zOffsetValue);
            }
        }
    }

    /**
     * 更新阴影投射几何体的位置和朝向
     * 让shadow caster跟随部件位置移动，并朝向光源（而不是摄像机）
     * 这样投射出的阴影是木偶"正对光源"时的形状，不会随视角变化而"解体"
     */
    protected void updateShadowCasterPositions() {
        // 每60帧打印一次调试信息
        debugFrameCounter++;
        boolean shouldDebug = (debugFrameCounter % 60 == 0);

        if (shouldDebug) {
           
        }

        // 获取光源方向（假设使用主方向光）
        // 从场景中获取DirectionalLight
        DirectionalLight sun = null;

        // 尝试从多个位置查找光源
        if (puppetNode.getParent() != null) {
            // 先从父节点查找
            for (com.jme3.light.Light light : puppetNode.getParent().getLocalLightList()) {
                if (light instanceof DirectionalLight) {
                    sun = (DirectionalLight) light;
                    break;
                }
            }

            // 如果父节点没有，尝试从根节点查找
            if (sun == null) {
                Node rootNode = puppetNode.getParent();
                while (rootNode.getParent() != null) {
                    rootNode = rootNode.getParent();
                }
                for (com.jme3.light.Light light : rootNode.getLocalLightList()) {
                    if (light instanceof DirectionalLight) {
                        sun = (DirectionalLight) light;
                        break;
                    }
                }
            }
        }

        if (sun == null) {
            // 如果没有找到光源，使用默认方向（从上往下）
            Vector3f defaultLightDir = new Vector3f(0.3f, -1f, 0.3f).normalizeLocal();
            updateShadowCastersWithLightDir(defaultLightDir);
            return;
        }

        // 光源照射方向（从光源指向物体）
        Vector3f lightDir = sun.getDirection();
        updateShadowCastersWithLightDir(lightDir);
    }

    /**
     * 使用指定的光源方向更新shadow casters
     */
    private void updateShadowCastersWithLightDir(Vector3f lightDir) {
        boolean shouldDebug = (debugFrameCounter % 60 == 0);

        if (shouldDebug) {

        }

        for (PuppetPartRenderer renderer : allRenderers) {
            Bone bone = renderer.getBone();
            Geometry shadowCaster = shadowCasters.get(bone.getName());

            if (shouldDebug) {

            }

            if (shadowCaster != null) {
                // 【新增】检查方向是否改变，如果改变则更新贴图
                String currentDirection = bone.getCurrentDirection();
                String lastDirection = shadowCasterDirections.get(bone.getName());

                if (!currentDirection.equals(lastDirection)) {
                    // 方向改变了，更新贴图
                    String newTexturePath = bone.getCurrentDirectionTexture();
                    if (newTexturePath != null && !newTexturePath.isEmpty()) {
                        try {
                            com.jme3.texture.Texture newTexture = app.getAssetManager().loadTexture(newTexturePath);
                            newTexture.setMagFilter(com.jme3.texture.Texture.MagFilter.Nearest);
                            newTexture.setMinFilter(com.jme3.texture.Texture.MinFilter.NearestNoMipMaps);

                            Material mat = shadowCaster.getMaterial();
                            mat.setTexture("DiffuseMap", newTexture);

                            // 更新记录的方向
                            shadowCasterDirections.put(bone.getName(), currentDirection);

                        } catch (Exception e) {
                            System.err.println("[Shadow Caster] 切换贴图失败: " + newTexturePath);
                        }
                    }
                }

                // 获取部件的最终世界位置
                Vector3f partWorldPos = renderer.getFinalWorldPosition();

                if (shouldDebug) {

                }

                // 【修复】将世界坐标转换为相对于shadowCasterNode父节点的局部坐标
                // 这样即使父节点有变换，shadow caster也能正确跟随部件
                Node parent = shadowCasterNode.getParent();
                Vector3f localPos;
                if (parent != null) {
                    // 将世界坐标转换为父节点的局部坐标
                    localPos = parent.worldToLocal(partWorldPos, null);
                } else {
                    // 如果没有父节点，直接使用世界坐标
                    localPos = partWorldPos;
                }

                // 【修复Z轴散开】统一所有shadowCaster的Z值为0，消除深度差异
                // 当shadowCaster偏转角度时，原本用于表现遮挡关系的Z值差异会暴露出来
                // 将Z值统一为0，让所有部件在同一平面上，避免"散开"效果
                if (useLightAlignedShadow) {
                    localPos.z = 0f;
                }

                // 更新shadow caster的位置（跟随部件）
                shadowCaster.setLocalTranslation(localPos);

                // 【新增】根据配置选择旋转模式
                if (useLightAlignedShadow) {
                    // 【光源对齐模式】使用反向旋转补偿，让shadowCaster始终面向光源
                    // 抵消相机旋转、场景旋转和角色手动旋转，使阴影始终基于"不会穿帮的初始姿势"
                    shadowCaster.setLocalRotation(calculateLightAlignedRotation());
                } else {
                    // 【旧模式】使用统一billboard旋转，跟随摄像机（向后兼容）
                    shadowCaster.setLocalRotation(unifiedBillboardRotation);
                }
            }
        }

        // 【新增】应用多光源压缩（如果启用）
        if (useLightAlignedShadow) {
            applyShadowCompression();
        }
    }

    /**
     * 创建阴影投射几何体
     * 为每个部件创建一个固定朝向的shadow caster，只用于投射阴影
     */
    protected void createShadowCasters() {

        for (PuppetPartRenderer renderer : allRenderers) {
            Bone bone = renderer.getBone();

            // 创建与部件相同尺寸的四边形
            float width = renderer.getWidth();
            float height = renderer.getHeight();

            // 创建居中的四边形mesh
            com.jme3.scene.shape.Quad quad = new com.jme3.scene.shape.Quad(width, height);

            // 调整顶点使其居中
            com.jme3.scene.Mesh mesh = new com.jme3.scene.Mesh();
            float[] positions = new float[]{
                -width/2, -height/2, 0,  // 左下
                width/2, -height/2, 0,   // 右下
                width/2, height/2, 0,    // 右上
                -width/2, height/2, 0    // 左上
            };
            mesh.setBuffer(com.jme3.scene.VertexBuffer.Type.Position, 3, positions);

            // 设置纹理坐标（标准UV，后续会根据方向更新）
            float[] texCoords = new float[]{
                0, 0,  // 左下
                1, 0,  // 右下
                1, 1,  // 右上
                0, 1   // 左上
            };
            mesh.setBuffer(com.jme3.scene.VertexBuffer.Type.TexCoord, 2, texCoords);

            float[] normals = new float[]{
                0, 0, 1,
                0, 0, 1,
                0, 0, 1,
                0, 0, 1
            };
            mesh.setBuffer(com.jme3.scene.VertexBuffer.Type.Normal, 3, normals);

            short[] indices = new short[]{
                0, 1, 2,
                0, 2, 3
            };
            mesh.setBuffer(com.jme3.scene.VertexBuffer.Type.Index, 3, indices);
            mesh.updateBound();

            Geometry shadowCaster = new Geometry(bone.getName() + "_ShadowCaster", mesh);

            // 【新方案】使用部件的贴图，让阴影有正确的轮廓
            Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Light/Lighting.j3md");

            // 尝试加载部件的当前方向贴图
            String texturePath = bone.getCurrentDirectionTexture();
            if (texturePath != null && !texturePath.isEmpty()) {
                try {
                    com.jme3.texture.Texture texture = app.getAssetManager().loadTexture(texturePath);
                    texture.setMagFilter(com.jme3.texture.Texture.MagFilter.Nearest);
                    texture.setMinFilter(com.jme3.texture.Texture.MinFilter.NearestNoMipMaps);
                    mat.setTexture("DiffuseMap", texture);

                    // 设置为白色，让贴图原色显示
                    mat.setBoolean("UseMaterialColors", true);
                    mat.setColor("Diffuse", ColorRGBA.White);
                    mat.setColor("Ambient", ColorRGBA.White);

                    // 设置alpha测试阈值，让透明部分不投射阴影
                    mat.setFloat("AlphaDiscardThreshold", 0.5f);

                } catch (Exception e) {
                    System.err.println("[Shadow Caster] 加载贴图失败: " + texturePath);
                    // 失败时使用蓝色作为后备
                    mat.setBoolean("UseMaterialColors", true);
                    mat.setColor("Diffuse", ColorRGBA.Blue);
                    mat.setColor("Ambient", ColorRGBA.Blue);
                }
            } else {
                // 没有贴图时使用蓝色
                mat.setBoolean("UseMaterialColors", true);
                mat.setColor("Diffuse", ColorRGBA.Blue);
                mat.setColor("Ambient", ColorRGBA.Blue);
            }

            // 【隐身模式】使shadowCaster完全透明但继续投射阴影
            // 禁用颜色写入和深度写入，让shadowCaster不影响常规渲染，但仍参与阴影计算
            mat.getAdditionalRenderState().setColorWrite(false);
            mat.getAdditionalRenderState().setDepthWrite(false);

            shadowCaster.setMaterial(mat);

            // 【阴影投射】shadowCaster投射统一的地面阴影
            // puppet部件通过CastAndReceive模式实现相互遮挡阴影
            shadowCaster.setShadowMode(com.jme3.renderer.queue.RenderQueue.ShadowMode.Cast);

            // 设置渲染队列为Opaque（不透明物体）
            shadowCaster.setQueueBucket(com.jme3.renderer.queue.RenderQueue.Bucket.Opaque);

            // 添加到shadow caster节点
            shadowCasterNode.attachChild(shadowCaster);
            shadowCasters.put(bone.getName(), shadowCaster);

            // 记录当前方向
            shadowCasterDirections.put(bone.getName(), bone.getCurrentDirection());

        }

        // 【关键修复】自动将shadowCasterNode附加到场景
        // 如果puppetNode已经有父节点（已附加到场景），则将shadowCasterNode也附加到同一个父节点
        if (puppetNode.getParent() != null && shadowCasterNode.getParent() == null) {

            puppetNode.getParent().attachChild(shadowCasterNode);

        } else if (shadowCasterNode.getParent() == null) {

        } else {

        }

    }

    /**
     * 请求在下一帧重新排序
     * 当部件的优先级改变时调用
     */
    public void requestPrioritySort() {
        this.needsPrioritySort = true;
    }

    /**
     * 立即更新渲染顺序
     * 当部件的优先级改变时调用，立即重新排序
     */
    public void updateRenderOrder() {
        sortByPriority();
    }

    /**
     * 清理资源
     */
    public void cleanup() {
        for (PuppetPartRenderer renderer : allRenderers) {
            renderer.cleanup();
        }
        partRenderers.clear();
        allRenderers.clear();
        boneConnectionsNode.detachAllChildren();
        boneConnectionLines.clear();

        // 清理阴影投射几何体
        shadowCasterNode.detachAllChildren();
        shadowCasters.clear();
        shadowCasterDirections.clear();

        puppetNode.removeFromParent();
        initialized = false;
    }

    // ========== Getters ==========

    public Skeleton getSkeleton() {
        return skeleton;
    }

    public Node getPuppetNode() {
        return puppetNode;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public List<PuppetPartRenderer> getAllPartRenderers() {
        return new ArrayList<>(allRenderers);
    }

    /**
     * 设置初始化状态
     * 用于在清空和重建后重新激活渲染器
     */
    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }

    /**
     * 获取Billboard渲染模式
     */
    public BillboardMode getBillboardMode() {
        return billboardMode;
    }

    /**
     * 设置Billboard渲染模式
     * @param mode Billboard模式
     */
    public void setBillboardMode(BillboardMode mode) {
        this.billboardMode = mode;

        // 同步旧的unifiedBillboard字段（向后兼容）
        if (mode == BillboardMode.UNIFIED) {
            this.unifiedBillboard = true;
        } else {
            this.unifiedBillboard = false;
        }
    }

    /**
     * 获取统一Billboard开关状态（向后兼容）
     * @deprecated 使用 getBillboardMode() 代替
     */
    @Deprecated
    public boolean isUnifiedBillboard() {
        return billboardMode == BillboardMode.UNIFIED;
    }

    /**
     * 设置统一Billboard开关（向后兼容）
     * @param enabled true=启用统一billboard（整体朝向相机），false=各部件独立billboard
     * @deprecated 使用 setBillboardMode() 代替
     */
    @Deprecated
    public void setUnifiedBillboard(boolean enabled) {
        this.billboardMode = enabled ? BillboardMode.UNIFIED : BillboardMode.INDEPENDENT;
        this.unifiedBillboard = enabled;
    }

    /**
     * 获取统一Billboard旋转（供PuppetPartRenderer使用）
     */
    public Quaternion getUnifiedBillboardRotation() {
        return unifiedBillboardRotation;
    }

    /**
     * 设置是否启用自动方向切换
     * @param enabled true=根据相机位置自动切换方向，false=手动控制方向
     */
    public void setAutoDirectionSwitch(boolean enabled) {
        this.autoDirectionSwitch = enabled;
    }

    /**
     * 获取是否启用自动方向切换
     */
    public boolean isAutoDirectionSwitch() {
        return autoDirectionSwitch;
    }

    /**
     * 设置手动旋转角度（用于"圆盘旋转"效果）
     * @param angle 旋转角度（弧度）
     */
    public void setManualRotationAngle(float angle) {
        this.manualRotationAngle = angle;
    }

    /**
     * 获取手动旋转角度
     * @return 旋转角度（弧度）
     */
    public float getManualRotationAngle() {
        return manualRotationAngle;
    }

    // ========== 光源对齐阴影系统 ==========

    /**
     * 设置是否启用光源对齐阴影（反向旋转补偿）
     * @param enabled true=启用（默认），false=使用旧的billboard跟随模式
     */
    public void setUseLightAlignedShadow(boolean enabled) {
        this.useLightAlignedShadow = enabled;
        if (enabled) {
            markLightCacheDirty(); // 切换到新模式时强制重新查找光源
        }
    }

    /**
     * 获取是否启用光源对齐阴影
     */
    public boolean isUseLightAlignedShadow() {
        return useLightAlignedShadow;
    }

    /**
     * 设置多光源处理模式
     * @param mode PRIMARY_ONLY（默认）或 AUTO_COMPRESS
     */
    public void setMultiLightMode(MultiLightMode mode) {
        if (this.multiLightMode != mode) {
            this.multiLightMode = mode;
            markLightCacheDirty(); // 模式改变时重新评估光源
        }
    }

    /**
     * 获取多光源处理模式
     */
    public MultiLightMode getMultiLightMode() {
        return multiLightMode;
    }

    /**
     * 标记光源缓存为脏（当场景光源改变时调用）
     * 例如：进入新区域、时间变化导致光源切换等
     */
    public void markLightCacheDirty() {
        this.lightCacheDirty = true;
    }

    /**
     * 从根节点递归查找所有DirectionalLight
     * 【性能优化】只在初始化或lightCacheDirty时调用
     */
    private List<DirectionalLight> findAllDirectionalLights(Node rootNode) {
        List<DirectionalLight> lights = new ArrayList<>();
        collectLightsRecursive(rootNode, lights);
        return lights;
    }

    /**
     * 递归收集所有DirectionalLight
     */
    private void collectLightsRecursive(Node node, List<DirectionalLight> lights) {
        // 检查当前节点的光源
        for (com.jme3.light.Light light : node.getLocalLightList()) {
            if (light instanceof DirectionalLight) {
                lights.add((DirectionalLight) light);
            }
        }

        // 递归检查子节点
        for (com.jme3.scene.Spatial child : node.getChildren()) {
            if (child instanceof Node) {
                collectLightsRecursive((Node) child, lights);
            }
        }
    }

    /**
     * 选择主光源（根据multiLightMode）
     */
    private DirectionalLight selectPrimaryLight(List<DirectionalLight> lights) {
        if (lights.isEmpty()) {
            return null;
        }

        if (lights.size() == 1) {
            return lights.get(0);
        }

        switch (multiLightMode) {
            case PRIMARY_ONLY:
                // 选择最强光源（颜色分量总和最大 = intensity最高）
                return lights.stream()
                    .max((a, b) -> {
                        float aIntensity = a.getColor().r + a.getColor().g + a.getColor().b;
                        float bIntensity = b.getColor().r + b.getColor().g + b.getColor().b;
                        return Float.compare(aIntensity, bIntensity);
                    })
                    .orElse(lights.get(0));

            case AUTO_COMPRESS:
                // 多光源压缩模式：随便选一个光源（反正会被压缩成平面）
                // 选择第一个作为参考方向
                return lights.get(0);

            default:
                return lights.get(0);
        }
    }

    /**
     * 更新光源缓存
     * 【性能关键】只在lightCacheDirty=true时执行，太阳光基本不动，可能整局游戏只算一次
     */
    private void updateLightCache() {
        if (!lightCacheDirty) {
            return; // 缓存有效，直接返回
        }

        // 从根节点查找所有DirectionalLight
        Node rootNode = puppetNode;
        while (rootNode.getParent() != null) {
            rootNode = rootNode.getParent();
        }

        List<DirectionalLight> lights = findAllDirectionalLights(rootNode);

        if (lights.isEmpty()) {
            cachedPrimaryLight = null;
            cachedLightYaw = 0f;
            lightCacheDirty = false;
            return;
        }

        // 选择主光源
        cachedPrimaryLight = selectPrimaryLight(lights);

        // 计算并缓存光源yaw（水平方向角）
        if (cachedPrimaryLight != null) {
            Vector3f lightDir = cachedPrimaryLight.getDirection();
            // 光源方向是"从光源指向物体"，我们需要"从物体指向光源"的反方向
            Vector3f toLightSource = lightDir.negate();
            toLightSource.y = 0; // 只考虑水平方向

            if (toLightSource.lengthSquared() > 0.0001f) {
                toLightSource.normalizeLocal();
                cachedLightYaw = (float)Math.atan2(toLightSource.x, toLightSource.z);

            } else {
                // 光源完全垂直，使用默认yaw
                cachedLightYaw = 0f;
            }
        }

        lightCacheDirty = false;
    }

    /**
     * 计算光源对齐旋转（反向补偿）
     * 【核心修正】让shadowCaster的法线(+Z轴)指向光源方向
     */
    private Quaternion calculateLightAlignedRotation() {
        // 更新光源缓存（如果需要）
        updateLightCache();

        if (cachedPrimaryLight == null) {
            // 无光源，使用默认朝向（不旋转）
            return Quaternion.IDENTITY;
        }

        // 获取光源方向（从光源指向物体）
        Vector3f lightDir = cachedPrimaryLight.getDirection();

        // 我们需要"从物体指向光源"的方向，即光源方向的反向
        Vector3f toLightSource = lightDir.negate().normalize();

        // 【关键修正】shadowCaster的法线是+Z轴(0,0,1)，需要旋转使其对齐toLightSource
        // 使用lookAt让quad的+Z轴指向光源
        Quaternion rotation = new Quaternion();
        rotation.lookAt(toLightSource, Vector3f.UNIT_Y);

        return rotation;
    }

    /**
     * 应用垂直压缩（多光源AUTO_COMPRESS模式）
     */
    private void applyShadowCompression() {
        if (multiLightMode == MultiLightMode.AUTO_COMPRESS && cachedPrimaryLight != null) {
            for (Geometry shadowCaster : shadowCasters.values()) {
                // 垂直压缩为接近平面的quad（退化为脚底投影）
                Vector3f scale = shadowCaster.getLocalScale().clone();
                scale.y = 0.01f; // 压缩到1%高度
                shadowCaster.setLocalScale(scale);
            }
        } else {
            // 恢复正常缩放
            for (Geometry shadowCaster : shadowCasters.values()) {
                Vector3f scale = shadowCaster.getLocalScale().clone();
                scale.y = 1.0f; // 恢复100%高度
                shadowCaster.setLocalScale(scale);
            }
        }
    }
}
