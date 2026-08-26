package com.Hecate.puppet.editor.core;

import com.jme3.math.Vector3f;
import com.jme3.math.Quaternion;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

/**
 * 编辑器专用骨骼节点
 * 代表木偶的一个刚性部件（头、躯干、手臂等）
 *
 * 注意：这是编辑器专用版本，与游戏运行时的Bone类独立
 */
public class EditorBone {

    /**
     * 骨骼类型枚举
     */
    public enum BoneType {
        /** 连接骨骼 - 刚性连接，跟随父骨骼移动 */
        CONNECTED,
        /** 自由骨骼 - 带物理摆动效果 */
        FREE
    }

    /**
     * 重力方向预设枚举
     */
    public enum GravityDirection {
        UP(0, 1, 0, "上"),
        DOWN(0, -1, 0, "下"),
        LEFT(-1, 0, 0, "左"),
        RIGHT(1, 0, 0, "右"),
        FRONT(0, 0, 1, "前"),
        BACK(0, 0, -1, "后"),
        CUSTOM(0, 0, 0, "自定义");

        private final float x, y, z;
        private final String displayName;

        GravityDirection(float x, float y, float z, String displayName) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.displayName = displayName;
        }

        public Vector3f toVector() {
            return new Vector3f(x, y, z);
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * 方向枚举（用于多方向贴图支持）
     * 支持6个方向：前后左右上下
     */
    public enum Direction {
        FRONT("front"),   // 正面（相机在前）
        BACK("back"),     // 背面（相机在后）
        LEFT("left"),     // 左侧
        RIGHT("right"),   // 右侧
        UP("up"),         // 上方（相机在上）
        DOWN("down");     // 下方（相机在下）

        private final String key;

        Direction(String key) {
            this.key = key;
        }

        public String getKey() {
            return key;
        }

        public static Direction fromKey(String key) {
            for (Direction dir : values()) {
                if (dir.key.equals(key)) {
                    return dir;
                }
            }
            return FRONT; // 默认正面
        }
    }

    private String name;  // 修改为可变，支持重命名
    private EditorBone parent;
    private final List<EditorBone> children;

    // Rest姿势（初始姿势）
    private Vector3f restPosition;
    private Quaternion restRotation;
    private Vector3f restScale;

    // 当前姿势（动画后的姿势）
    private Vector3f localPosition;
    private Quaternion localRotation;
    private Vector3f localScale;

    // 可选的Z偏移（用于排序）
    private float zOffset;

    // 渲染优先级（数值越大，渲染越靠后，会覆盖在其他部件上面）
    private int priority;

    // 部件纹理/材质引用（可选，向后兼容）
    private String texturePath;

    // 多方向贴图支持
    private Map<String, String> directionTextures;  // 方向key -> 贴图路径
    private String currentDirection;  // 当前方向（默认"front"）

    // 多方向UV坐标支持（每个方向独立的UV设置）
    private Map<String, float[]> directionUVs;  // 方向key -> UV数组[offsetX, offsetY, scaleX, scaleY]

    // 多方向优先级支持（每个方向独立的优先级设置）
    private Map<String, Integer> directionPriorities;  // 方向key -> 优先级值

    // 多方向尺寸支持（每个方向独立的宽度和高度）
    private Map<String, Float> directionWidths;  // 方向key -> 宽度值
    private Map<String, Float> directionHeights;  // 方向key -> 高度值

    // 多方向位置偏移支持（每个方向独立的位置偏移）
    private Map<String, float[]> directionOffsets;  // 方向key -> [offsetX, offsetY, offsetZ]

    // 多方向旋转支持（每个方向独立的旋转角度）
    private Map<String, float[]> directionRotations;  // 方向key -> [rotationX, rotationY, rotationZ]

    // 多方向内容中心偏移支持（用于对齐贴图中非居中的实际内容）
    // 偏移值相对于quad中心，范围 -0.5 到 +0.5
    private Map<String, float[]> directionContentCenters;  // 方向key -> [centerX, centerY]

    // 多方向贴图旋转支持（每个方向独立的贴图旋转角度）
    // 旋转角度以度为单位，顺时针为正
    private Map<String, Float> directionTextureRotations;  // 方向key -> 旋转角度（度）

    // Billboard控制 - 控制该部件是否使用billboard（始终面向摄像机）
    // true = 2D纸片模式（面向摄像机），false = 3D模式（固定朝向）
    private boolean billboardEnabled = true;  // 默认启用，向后兼容

    // 多方向贴图模式控制
    // true = 每个方向独立贴图（2D精灵模式），false = 所有方向共用一个贴图（3D模型模式）
    private boolean multiDirectionTextureEnabled = true;  // 默认启用多方向贴图

    // ==================== 旋转条状贴图系统（伪3D棱柱效果） ====================
    // 与上面的6方向系统（front/back/left/right/up/down）互斥，按骨骼独立开关
    // 启用后该骨骼忽略方向key，改用"一张环绕360°的条状贴图 + 相机yaw取样"

    // 是否启用旋转条状贴图模式
    private boolean rotationStripEnabled = false;

    // 条状贴图路径
    private String stripTexturePath;

    // 转一圈对应的档数（离散取样点数）。0 = 逐像素连续取样（最细腻，无级）
    private int stripSteps = 16;

    // 取景框宽度（像素），即每一档显示的贴图宽度
    private int stripFrameWidthPx = 32;

    // 取景框高度（像素）
    private int stripFrameHeightPx = 32;

    // Billboard俯仰角范围（度）：|pitch| <= 此值时完全billboard（面向摄像机）
    private float billboardPitchFullRangeDeg = 60f;

    // Billboard俯仰角锁定阈值（度）：|pitch| >= 此值时完全锁定竖直朝向，不再billboard
    // fullRangeDeg与此值之间做平滑插值过渡
    private float billboardPitchLockDeg = 80f;

    // ==================== 旋转条状贴图专用变换数据（单一值，不按方向分槎） ====================
    // 旋转条状贴图部件转身时不切换方向key，所以宽高/偏移/旋转/优先级只需要一份数据，
    // 不能像6方向系统那样按currentDirection存成Map——否则会出现"同一个部件在不同视角
    // 显示不同宽高"的错乱。这些字段只在rotationStripEnabled=true时被读写。

    private float stripWidth = 1.0f;
    private float stripHeight = 1.0f;
    private final Vector3f stripOffset = new Vector3f(0f, 0f, 0f);
    private float stripRotationX = 0f;
    private float stripRotationY = 0f;
    private float stripRotationZ = 0f;
    private int stripPriority = 0;

    // 取景框校准偏移（像素），与core.Bone保持一致
    private int stripCalibrationOffsetPx = 0;

    // ==================== 3D模型骨骼系统（与core.Bone保持一致） ====================
    // 扩展点说明见core.Bone.java同名字段上方的注释（逐帧模型动画的接入方式）
    private boolean modelEnabled = false;
    private String modelFilePath;
    private float modelRotationX = 0f;
    private float modelRotationY = 0f;
    private float modelRotationZ = 0f;
    private float modelScale = 1.0f;

    // ==================== 骨骼分组系统 ====================

    // 骨骼组ID（所属分组的唯一标识）
    private String groupId;  // null表示不属于任何分组

    // ==================== 自由骨骼系统 ====================

    // 骨骼类型（连接骨骼 or 自由骨骼）
    private BoneType boneType = BoneType.CONNECTED;  // 默认为连接骨骼

    // 重力方向（用于自由骨骼）
    private GravityDirection gravityDirection = GravityDirection.DOWN;  // 默认向下

    // 自定义重力向量（当gravityDirection为CUSTOM时使用）
    private Vector3f customGravityVector = new Vector3f(0, -1, 0);

    // 自由度数值（0-1范围，决定摆动的剧烈程度）
    // 0 = 完全刚性，1 = 最大摆动
    private float freedomValue = 0.5f;  // 默认中等自由度（向后兼容）

    // 多方向自由度支持（每个方向独立的自由度设置）
    private Map<String, Float> directionFreedomValues;  // 方向key -> 自由度值

    // FreeBonePhysics 物理参数（用于摇晃效果）
    private float physMass = 1.0f;           // 质量
    private float physDamping = 0.95f;       // 阻尼系数（0-1）（向后兼容）
    private float physStiffness = 50.0f;     // 刚度系数
    private float physGravityStrength = 9.8f; // 重力强度
    private float physMaxSwingAngle = 45.0f; // 最大摆动角度（度）
    private float physMaxVelocity = 10.0f;   // 最大速度限制

    // 多方向阻尼系数支持（每个方向独立的阻尼系数设置）
    private Map<String, Float> directionDampingValues;  // 方向key -> 阻尼系数值

    // ==================== Live2D风格相机跟随系统 ====================

    // 相机跟随自由度 - 水平方向（左右晃动）
    // 0 = 不晃动，1 = 最大晃动幅度
    private float cameraFollowFreedomX = 0.0f;  // 默认不晃动

    // 相机跟随自由度 - 垂直方向（上下晃动）
    // 0 = 不晃动，1 = 最大晃动幅度
    private float cameraFollowFreedomY = 0.0f;  // 默认不晃动

    // ==================== 摇摆系统 (Swing/Sway System) ====================

    // 摇摆开关 - 是否启用摇摆效果
    private boolean swingEnabled = false;  // 默认关闭（向后兼容）

    // 多方向摇摆开关支持（每个方向独立的摇摆开关）
    private Map<String, Boolean> directionSwingEnabled;  // 方向key -> 是否启用摇摆

    // 摇摆轴 - 摇摆旋转的轴向（归一化向量）
    // 例如：(0, 0, 1) 表示绕Z轴摇摆（左右摆动）
    //       (1, 0, 0) 表示绕X轴摇摆（前后摆动）
    //       (0, 1, 0) 表示绕Y轴摇摆（扭转）
    private Vector3f swingAxis = new Vector3f(0, 0, 1);  // 默认绕Z轴（左右摆动）（向后兼容）

    // 多方向摇摆轴支持（每个方向独立的摇摆轴）
    private Map<String, float[]> directionSwingAxes;  // 方向key -> [x, y, z]

    // 摇摆幅度 - 最大摇摆角度（度）
    // 例如：30.0f 表示最大摆动±30度
    private float swingAmplitude = 15.0f;  // 默认±15度（向后兼容）

    // 多方向摇摆幅度支持（每个方向独立的摇摆幅度）
    private Map<String, Float> directionSwingAmplitudes;  // 方向key -> 摇摆幅度值

    // 摇摆频率 - 摇摆的频率（Hz，每秒摆动次数）
    // 例如：0.5f 表示每秒摆动0.5次（2秒一个完整周期）
    private float swingFrequency = 0.5f;  // 默认0.5Hz

    // 摇摆相位偏移 - 初始相位偏移（弧度）
    // 用于让多个部件的摇摆不同步，产生更自然的效果
    // 例如：0.0f 表示从中心位置开始，FastMath.HALF_PI 表示从最大位置开始
    private float swingPhaseOffset = 0.0f;  // 默认无偏移

    public EditorBone(String name) {
        this.name = name;
        this.children = new ArrayList<>();

        // 默认Rest姿势
        this.restPosition = new Vector3f(0, 0, 0);
        this.restRotation = new Quaternion();
        this.restScale = new Vector3f(1, 1, 1);

        // 默认当前姿势等于Rest姿势
        this.localPosition = restPosition.clone();
        this.localRotation = restRotation.clone();
        this.localScale = restScale.clone();

        this.zOffset = 0f;
        this.priority = 0;  // 默认优先级为0

        // 初始化多方向贴图
        this.directionTextures = new HashMap<>();
        this.currentDirection = Direction.FRONT.getKey();

        // 初始化多方向UV坐标（默认全图：0,0,1,1）
        this.directionUVs = new HashMap<>();

        // 初始化多方向优先级
        this.directionPriorities = new HashMap<>();

        // 初始化多方向尺寸
        this.directionWidths = new HashMap<>();
        this.directionHeights = new HashMap<>();

        // 初始化多方向位置偏移
        this.directionOffsets = new HashMap<>();

        // 初始化多方向旋转
        this.directionRotations = new HashMap<>();

        // 初始化多方向内容中心偏移
        this.directionContentCenters = new HashMap<>();

        // 初始化多方向贴图旋转
        this.directionTextureRotations = new HashMap<>();

        // 初始化多方向自由度
        this.directionFreedomValues = new HashMap<>();

        // 初始化多方向阻尼系数
        this.directionDampingValues = new HashMap<>();

        // 初始化多方向摇摆开关
        this.directionSwingEnabled = new HashMap<>();

        // 初始化多方向摇摆轴
        this.directionSwingAxes = new HashMap<>();

        // 初始化多方向摇摆幅度
        this.directionSwingAmplitudes = new HashMap<>();
    }

    /**
     * 添加子骨骼
     */
    public void addChild(EditorBone child) {
        // 检查是否试图添加自己
        if (child == this) {
            return;
        }

        // 检查是否会形成循环（child是否是this的祖先）
        EditorBone ancestor = this.parent;
        while (ancestor != null) {
            if (ancestor == child) {
                return;
            }
            ancestor = ancestor.parent;
        }

        // 从旧父节点移除
        if (child.parent != null) {
            child.parent.children.remove(child);
        }

        // 设置新的父子关系
        child.parent = this;
        this.children.add(child);
    }

    /**
     * 移除子骨骼
     */
    public void removeChild(EditorBone child) {
        if (this.children.remove(child)) {
            child.parent = null;
        }
    }

    /**
     * 重置到Rest姿势
     */
    public void resetToRestPose() {
        this.localPosition.set(restPosition);
        this.localRotation.set(restRotation);
        this.localScale.set(restScale);
    }

    /**
     * 计算世界空间变换矩阵
     */
    public void getWorldTransform(Vector3f outPosition, Quaternion outRotation, Vector3f outScale) {
        if (parent == null) {
            // 根骨骼，直接使用局部变换
            outPosition.set(localPosition);
            outRotation.set(localRotation);
            outScale.set(localScale);
        } else {
            // 递归计算父骨骼的世界变换
            Vector3f parentWorldPos = new Vector3f();
            Quaternion parentWorldRot = new Quaternion();
            Vector3f parentWorldScale = new Vector3f();
            parent.getWorldTransform(parentWorldPos, parentWorldRot, parentWorldScale);

            // 组合变换
            // 位置 = 父位置 + 父旋转 * (父缩放 * 局部位置)
            Vector3f scaledPos = localPosition.mult(parentWorldScale);
            Vector3f rotatedPos = parentWorldRot.mult(scaledPos);
            outPosition.set(parentWorldPos.add(rotatedPos));

            // 旋转 = 父旋转 * 局部旋转
            outRotation.set(parentWorldRot.mult(localRotation));

            // 缩放 = 父缩放 * 局部缩放
            outScale.set(parentWorldScale.mult(localScale));
        }
    }

    // ========== Getters & Setters ==========

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public EditorBone getParent() {
        return parent;
    }

    public List<EditorBone> getChildren() {
        return new ArrayList<>(children);
    }

    public Vector3f getRestPosition() {
        return restPosition;
    }

    public void setRestPosition(Vector3f restPosition) {
        this.restPosition = restPosition;
    }

    public Quaternion getRestRotation() {
        return restRotation;
    }

    public void setRestRotation(Quaternion restRotation) {
        this.restRotation = restRotation;
    }

    public Vector3f getRestScale() {
        return restScale;
    }

    public void setRestScale(Vector3f restScale) {
        this.restScale = restScale;
    }

    public Vector3f getLocalPosition() {
        return localPosition;
    }

    public void setLocalPosition(Vector3f localPosition) {
        this.localPosition = localPosition;
    }

    public Quaternion getLocalRotation() {
        return localRotation;
    }

    public void setLocalRotation(Quaternion localRotation) {
        this.localRotation = localRotation;
    }

    public Vector3f getLocalScale() {
        return localScale;
    }

    public void setLocalScale(Vector3f localScale) {
        this.localScale = localScale;
    }

    public float getZOffset() {
        return zOffset;
    }

    public void setZOffset(float zOffset) {
        this.zOffset = zOffset;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public String getTexturePath() {
        return texturePath;
    }

    public void setTexturePath(String texturePath) {
        this.texturePath = texturePath;
    }

    /**
     * 检查是否启用Billboard
     */
    public boolean isBillboardEnabled() {
        return billboardEnabled;
    }

    /**
     * 设置是否启用Billboard
     * @param enabled true=2D纸片模式（面向摄像机），false=3D模式（固定朝向）
     */
    public void setBillboardEnabled(boolean enabled) {
        this.billboardEnabled = enabled;
    }

    /**
     * 检查是否启用多方向贴图模式
     */
    public boolean isMultiDirectionTextureEnabled() {
        return multiDirectionTextureEnabled;
    }

    /**
     * 设置是否启用多方向贴图模式
     * @param enabled true=每个方向独立贴图（2D精灵），false=所有方向共用一个贴图（3D模型）
     */
    public void setMultiDirectionTextureEnabled(boolean enabled) {
        this.multiDirectionTextureEnabled = enabled;
    }

    // ==================== 旋转条状贴图系统 Getter & Setter ====================

    public boolean isRotationStripEnabled() {
        return rotationStripEnabled;
    }

    public void setRotationStripEnabled(boolean enabled) {
        this.rotationStripEnabled = enabled;
    }

    public String getStripTexturePath() {
        return stripTexturePath;
    }

    public void setStripTexturePath(String stripTexturePath) {
        this.stripTexturePath = stripTexturePath;
    }

    public int getStripSteps() {
        return stripSteps;
    }

    public void setStripSteps(int stripSteps) {
        this.stripSteps = Math.max(0, stripSteps);
    }

    // 取景框像素 -> 世界单位的固定换算比例（32px = 1单位），与core.Bone保持一致
    public static final float STRIP_PIXELS_PER_UNIT = 32f;

    public int getStripFrameWidthPx() {
        return stripFrameWidthPx;
    }

    public void setStripFrameWidthPx(int stripFrameWidthPx) {
        this.stripFrameWidthPx = Math.max(1, stripFrameWidthPx);
        syncStripSizeFromFramePixels();
    }

    public int getStripFrameHeightPx() {
        return stripFrameHeightPx;
    }

    public void setStripFrameHeightPx(int stripFrameHeightPx) {
        this.stripFrameHeightPx = Math.max(1, stripFrameHeightPx);
        syncStripSizeFromFramePixels();
    }

    /**
     * 根据取景框像素宽高，按固定比例重算部件世界尺寸，保证显示形状与取景框像素比例始终一致。
     */
    private void syncStripSizeFromFramePixels() {
        this.stripWidth = stripFrameWidthPx / STRIP_PIXELS_PER_UNIT;
        this.stripHeight = stripFrameHeightPx / STRIP_PIXELS_PER_UNIT;
    }

    public float getBillboardPitchFullRangeDeg() {
        return billboardPitchFullRangeDeg;
    }

    public void setBillboardPitchFullRangeDeg(float degrees) {
        this.billboardPitchFullRangeDeg = degrees;
    }

    public float getBillboardPitchLockDeg() {
        return billboardPitchLockDeg;
    }

    public void setBillboardPitchLockDeg(float degrees) {
        this.billboardPitchLockDeg = degrees;
    }

    // ==================== 旋转条状贴图专用变换数据 Getter & Setter ====================

    public float getStripWidth() {
        return stripWidth;
    }

    public void setStripWidth(float stripWidth) {
        this.stripWidth = stripWidth;
    }

    public float getStripHeight() {
        return stripHeight;
    }

    public void setStripHeight(float stripHeight) {
        this.stripHeight = stripHeight;
    }

    public Vector3f getStripOffset() {
        return stripOffset.clone();
    }

    public void setStripOffset(float offsetX, float offsetY, float offsetZ) {
        this.stripOffset.set(offsetX, offsetY, offsetZ);
    }

    public float getStripRotationX() {
        return stripRotationX;
    }

    public float getStripRotationY() {
        return stripRotationY;
    }

    public float getStripRotationZ() {
        return stripRotationZ;
    }

    public void setStripRotation(float rotationX, float rotationY, float rotationZ) {
        this.stripRotationX = rotationX;
        this.stripRotationY = rotationY;
        this.stripRotationZ = rotationZ;
    }

    public int getStripPriority() {
        return stripPriority;
    }

    public void setStripPriority(int stripPriority) {
        this.stripPriority = stripPriority;
    }

    public int getStripCalibrationOffsetPx() {
        return stripCalibrationOffsetPx;
    }

    public void setStripCalibrationOffsetPx(int stripCalibrationOffsetPx) {
        this.stripCalibrationOffsetPx = stripCalibrationOffsetPx;
    }

    // ==================== 3D模型骨骼系统 Getter & Setter ====================

    public boolean isModelEnabled() {
        return modelEnabled;
    }

    public void setModelEnabled(boolean modelEnabled) {
        this.modelEnabled = modelEnabled;
    }

    public String getModelFilePath() {
        return modelFilePath;
    }

    public void setModelFilePath(String modelFilePath) {
        this.modelFilePath = modelFilePath;
    }

    public float getModelRotationX() {
        return modelRotationX;
    }

    public float getModelRotationY() {
        return modelRotationY;
    }

    public float getModelRotationZ() {
        return modelRotationZ;
    }

    public void setModelRotation(float rotationX, float rotationY, float rotationZ) {
        this.modelRotationX = rotationX;
        this.modelRotationY = rotationY;
        this.modelRotationZ = rotationZ;
    }

    public float getModelScale() {
        return modelScale;
    }

    public void setModelScale(float modelScale) {
        this.modelScale = modelScale;
    }

    // ==================== 骨骼分组系统方法 ====================

    /**
     * 获取骨骼组ID
     * @return 组ID，null表示不属于任何分组
     */
    public String getGroupId() {
        return groupId;
    }

    /**
     * 设置骨骼组ID
     * @param groupId 组ID，null表示移出分组
     */
    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    /**
     * 检查骨骼是否属于某个分组
     * @return true表示属于某个分组
     */
    public boolean hasGroup() {
        return groupId != null;
    }

    // ==================== 方向贴图方法 ====================

    /**
     * 获取方向贴图映射
     */
    public Map<String, String> getDirectionTextures() {
        return directionTextures;
    }

    /**
     * 设置方向贴图映射
     */
    public void setDirectionTextures(Map<String, String> directionTextures) {
        this.directionTextures = directionTextures;
    }

    /**
     * 获取当前方向
     */
    public String getCurrentDirection() {
        return currentDirection;
    }

    /**
     * 设置当前方向
     */
    public void setCurrentDirection(String direction) {

        this.currentDirection = direction;
    }

    /**
     * 【性能优化】获取当前方向的完整配置
     * 一次查找获取所有属性，避免多次HashMap查找
     *
     * @return 当前方向的配置对象（已应用继承逻辑）
     */
    public DirectionConfig getCurrentDirectionConfig() {
        DirectionConfig config = new DirectionConfig();
        String dir = getCurrentDirection();

        // 单贴图模式：忽略方向
        if (!multiDirectionTextureEnabled) {
            // 获取任意可用贴图
            for (String texture : directionTextures.values()) {
                if (texture != null && !texture.isEmpty()) {
                    config.texture = texture;
                    break;
                }
            }
            if (config.texture == null) {
                config.texture = texturePath;
            }
        } else {
            // 多方向贴图模式：先尝试当前方向
            config.texture = directionTextures.get(dir);
            if (config.texture == null || config.texture.isEmpty()) {
                // 继承策略：从第一个有贴图的方向继承
                String sourceDir = getSourceDirectionForInheritance();
                if (sourceDir != null) {
                    config.texture = directionTextures.get(sourceDir);
                }
                if (config.texture == null || config.texture.isEmpty()) {
                    config.texture = texturePath;
                }
            }
        }

        // 获取所有其他属性（应用相同的继承逻辑）
        config.uv = getCurrentDirectionUV();
        config.textureRotation = getCurrentDirectionTextureRotation();
        config.width = getCurrentDirectionWidth();
        config.height = getCurrentDirectionHeight();
        config.offset = getCurrentDirectionOffset();
        config.rotation = getCurrentDirectionRotation();
        config.contentCenter = getCurrentDirectionContentCenter();
        config.priority = getCurrentDirectionPriority();
        config.freedomValue = getCurrentDirectionFreedomValue();
        config.damping = getCurrentDirectionDamping();  // 注意：方法名是 Damping 不是 DampingValue
        config.swingEnabled = getCurrentDirectionSwingEnabled();
        config.swingAmplitude = getCurrentDirectionSwingAmplitude();

        // SwingAxis 目前没有 getCurrentDirection 版本的方法，暂时设为 null
        // TODO: 如果需要支持多方向的 swingAxis，需要添加对应的方法
        config.swingAxis = null;

        return config;
    }

    /**
     * 设置指定方向的贴图路径
     */
    public void setDirectionTexture(Direction direction, String texturePath) {
        directionTextures.put(direction.getKey(), texturePath);
    }

    /**
     * 设置指定方向的贴图路径（字符串key）
     */
    public void setDirectionTexture(String directionKey, String texturePath) {
        directionTextures.put(directionKey, texturePath);
    }

    /**
     * 获取指定方向的贴图路径
     */
    public String getDirectionTexture(Direction direction) {
        return directionTextures.get(direction.getKey());
    }

    /**
     * 获取指定方向的贴图路径（字符串key）
     */
    public String getDirectionTexture(String directionKey) {
        return directionTextures.get(directionKey);
    }

    /**
     * 获取当前方向的贴图路径（使用继承策略）
     * 如果启用多方向贴图模式：
     *   优先级：
     *   1. 当前方向的贴图
     *   2. 继承源方向的贴图（第一个有贴图的方向）
     *   3. 向后兼容的texturePath
     * 如果禁用多方向贴图模式（单贴图模式）：
     *   直接返回任意可用贴图，不考虑方向
     */
    public String getCurrentDirectionTexture() {
        // 单贴图模式：忽略方向，直接返回任意可用贴图
        if (!multiDirectionTextureEnabled) {
            // 尝试获取任意已设置的方向贴图
            for (String texture : directionTextures.values()) {
                if (texture != null && !texture.isEmpty()) {
                    return texture;
                }
            }
            // 向后兼容：如果没有方向贴图，使用旧的texturePath
            return texturePath;
        }

        // 多方向贴图模式：根据当前方向选择贴图
        // 1. 尝试获取当前方向的贴图
        String dirTexture = directionTextures.get(currentDirection);
        if (dirTexture != null && !dirTexture.isEmpty()) {
            return dirTexture;
        }

        // 2. 使用继承策略：从第一个有贴图的方向继承
        String sourceDirection = getSourceDirectionForInheritance();
        if (sourceDirection != null) {
            String sourceTexture = directionTextures.get(sourceDirection);
            if (sourceTexture != null && !sourceTexture.isEmpty()) {
                return sourceTexture;
            }
        }

        // 3. 向后兼容：如果没有方向贴图，使用旧的texturePath
        return texturePath;
    }

    /**
     * 获取对面方向
     * FRONT ↔ BACK
     * LEFT ↔ RIGHT
     */
    private String getOppositeDirection(String direction) {
        if (direction == null) return null;

        switch (direction) {
            case "front":
                return "back";
            case "back":
                return "front";
            case "left":
                return "right";
            case "right":
                return "left";
            default:
                return null;
        }
    }

    /**
     * 获取用于继承的源方向（基于贴图）
     * 查找第一个拥有贴图的方向，用作其他方向的继承源
     * 优先级顺序：front -> back -> left -> right -> up -> down
     * @return 第一个拥有贴图的方向key，如果都没有返回null
     */
    private String getSourceDirectionForInheritance() {
        // 优先顺序：front -> back -> left -> right -> up -> down
        String[] priorities = {"front", "back", "left", "right", "up", "down"};

        for (String dir : priorities) {
            String texture = directionTextures.get(dir);
            if (texture != null && !texture.isEmpty()) {
                return dir;
            }
        }

        return null;
    }

    /**
     * 获取用于继承特定属性的源方向
     * 查找第一个拥有该属性的方向，用作其他方向的继承源
     * 优先级顺序：front -> back -> left -> right -> up -> down
     * @param attributeMap 属性映射（例如directionWidths, directionHeights等）
     * @return 第一个拥有该属性的方向key，如果都没有返回null
     */
    private <T> String getSourceDirectionForAttribute(Map<String, T> attributeMap) {
        // 优先顺序：front -> back -> left -> right -> up -> down
        String[] priorities = {"front", "back", "left", "right", "up", "down"};

        for (String dir : priorities) {
            T value = attributeMap.get(dir);
            if (value != null) {
                return dir;
            }
        }

        return null;
    }

    /**
     * 检查是否有指定方向的贴图
     */
    public boolean hasDirectionTexture(Direction direction) {
        String path = directionTextures.get(direction.getKey());
        return path != null && !path.isEmpty();
    }

    /**
     * 设置指定方向的UV坐标
     * @param direction 方向key (front/back/left/right)
     * @param offsetX UV偏移X
     * @param offsetY UV偏移Y
     * @param scaleX UV缩放X
     * @param scaleY UV缩放Y
     */
    public void setDirectionUV(String direction, float offsetX, float offsetY, float scaleX, float scaleY) {
        float[] uv = new float[]{offsetX, offsetY, scaleX, scaleY};
        directionUVs.put(direction, uv);
    }

    /**
     * 获取指定方向的UV坐标
     * @param direction 方向key (front/back/left/right)
     * @return UV数组[offsetX, offsetY, scaleX, scaleY]，如果不存在返回null
     */
    public float[] getDirectionUV(String direction) {
        return directionUVs.get(direction);
    }

    /**
     * 获取当前方向的UV坐标（使用继承策略）
     * 优先级：
     * 1. 当前方向的UV
     * 2. 继承源方向的UV（第一个有该属性的方向）
     * 3. 默认UV (0, 0, 1, 1)
     * @return UV数组[offsetX, offsetY, scaleX, scaleY]
     */
    public float[] getCurrentDirectionUV() {
        // 1. 尝试获取当前方向的UV
        float[] uv = directionUVs.get(currentDirection);
        if (uv != null) {
            return uv;
        }

        // 2. 使用继承策略：从第一个有该属性的方向继承
        String sourceDirection = getSourceDirectionForAttribute(directionUVs);
        if (sourceDirection != null) {
            float[] sourceUV = directionUVs.get(sourceDirection);
            if (sourceUV != null) {
                return sourceUV;
            }
        }

        // 3. 如果所有方向都没有UV设置，返回默认UV（全图）
        return new float[]{0.0f, 0.0f, 1.0f, 1.0f};
    }

    /**
     * 检查是否有指定方向的UV坐标
     * @param direction 方向key (front/back/left/right)
     * @return true如果该方向有UV设置
     */
    public boolean hasDirectionUV(String direction) {
        return directionUVs.containsKey(direction) && directionUVs.get(direction) != null;
    }

    /**
     * 获取方向UV映射（用于序列化）
     * @return 方向UV映射的副本
     */
    public Map<String, float[]> getDirectionUVs() {
        return new HashMap<>(directionUVs);
    }

    /**
     * 设置方向UV映射（用于反序列化）
     * @param directionUVs 方向UV映射
     */
    public void setDirectionUVs(Map<String, float[]> directionUVs) {
        if (directionUVs != null) {
            this.directionUVs = new HashMap<>(directionUVs);
        }
    }

    /**
     * 设置指定方向的优先级
     * @param direction 方向key (front/back/left/right)
     * @param priority 优先级值
     */
    public void setDirectionPriority(String direction, int priority) {
        directionPriorities.put(direction, priority);
    }

    /**
     * 获取指定方向的优先级
     * @param direction 方向key (front/back/left/right)
     * @return 优先级值，如果不存在返回null
     */
    public Integer getDirectionPriority(String direction) {
        return directionPriorities.get(direction);
    }

    /**
     * 获取当前方向的优先级（使用继承策略）
     * 优先级：
     * 1. 当前方向的优先级
     * 2. 继承源方向的优先级（第一个有该属性的方向）
     * 3. 全局priority值（向后兼容）
     * @return 优先级值
     */
    public int getCurrentDirectionPriority() {
        // 1. 尝试获取当前方向的优先级
        Integer dirPriority = directionPriorities.get(currentDirection);
        if (dirPriority != null) {
            return dirPriority;
        }

        // 2. 使用继承策略：从第一个有该属性的方向继承
        String sourceDirection = getSourceDirectionForAttribute(directionPriorities);
        if (sourceDirection != null) {
            Integer sourcePriority = directionPriorities.get(sourceDirection);
            if (sourcePriority != null) {
                return sourcePriority;
            }
        }

        // 3. 使用全局priority（向后兼容）
        return priority;
    }

    /**
     * 检查是否有指定方向的优先级
     * @param direction 方向key (front/back/left/right)
     * @return true如果该方向有优先级设置
     */
    public boolean hasDirectionPriority(String direction) {
        return directionPriorities.containsKey(direction) && directionPriorities.get(direction) != null;
    }

    /**
     * 获取方向优先级映射（用于序列化）
     * @return 方向优先级映射的副本
     */
    public Map<String, Integer> getDirectionPriorities() {
        return new HashMap<>(directionPriorities);
    }

    /**
     * 设置方向优先级映射（用于反序列化）
     * @param directionPriorities 方向优先级映射
     */
    public void setDirectionPriorities(Map<String, Integer> directionPriorities) {
        if (directionPriorities != null) {
            this.directionPriorities = new HashMap<>(directionPriorities);
        }
    }

    // ==================== 多方向尺寸支持 ====================

    /**
     * 设置指定方向的宽度
     * @param direction 方向key (front/back/left/right)
     * @param width 宽度值
     */
    public void setDirectionWidth(String direction, float width) {
        directionWidths.put(direction, width);
    }

    /**
     * 获取指定方向的宽度
     * @param direction 方向key (front/back/left/right)
     * @return 宽度值，如果不存在返回null
     */
    public Float getDirectionWidth(String direction) {
        return directionWidths.get(direction);
    }

    /**
     * 获取当前方向的宽度（使用继承策略）
     * 优先级：
     * 1. 当前方向的宽度
     * 2. 继承源方向的宽度（第一个有宽度属性的方向）
     * 3. 默认值null（由PuppetPartRenderer提供）
     * @return 宽度值，如果不存在返回null（表示使用渲染器默认值）
     */
    public Float getCurrentDirectionWidth() {
        // 1. 尝试获取当前方向的宽度
        Float width = directionWidths.get(currentDirection);
        if (width != null) {
            return width;
        }

        // 2. 使用继承策略：从第一个有宽度属性的方向继承
        String sourceDirection = getSourceDirectionForAttribute(directionWidths);
        if (sourceDirection != null) {
            Float sourceWidth = directionWidths.get(sourceDirection);
            if (sourceWidth != null) {
                return sourceWidth;
            }
        }

        // 3. 所有方向都没有，返回null（使用渲染器默认值）
        return null;
    }

    /**
     * 设置指定方向的高度
     * @param direction 方向key (front/back/left/right)
     * @param height 高度值
     */
    public void setDirectionHeight(String direction, float height) {
        directionHeights.put(direction, height);
    }

    /**
     * 获取指定方向的高度
     * @param direction 方向key (front/back/left/right)
     * @return 高度值，如果不存在返回null
     */
    public Float getDirectionHeight(String direction) {
        return directionHeights.get(direction);
    }

    /**
     * 获取当前方向的高度（使用继承策略）
     * 优先级：
     * 1. 当前方向的高度
     * 2. 继承源方向的高度（第一个有高度属性的方向）
     * 3. 默认值null（由PuppetPartRenderer提供）
     * @return 高度值，如果不存在返回null（表示使用渲染器默认值）
     */
    public Float getCurrentDirectionHeight() {
        // 1. 尝试获取当前方向的高度
        Float height = directionHeights.get(currentDirection);
        if (height != null) {
            return height;
        }

        // 2. 使用继承策略：从第一个有高度属性的方向继承
        String sourceDirection = getSourceDirectionForAttribute(directionHeights);
        if (sourceDirection != null) {
            Float sourceHeight = directionHeights.get(sourceDirection);
            if (sourceHeight != null) {
                return sourceHeight;
            }
        }

        // 3. 所有方向都没有，返回null（使用渲染器默认值）
        return null;
    }

    /**
     * 获取方向尺寸映射（用于序列化）
     * @return 方向尺寸映射的副本
     */
    public Map<String, Float> getDirectionWidths() {
        return new HashMap<>(directionWidths);
    }

    public Map<String, Float> getDirectionHeights() {
        return new HashMap<>(directionHeights);
    }

    /**
     * 设置方向尺寸映射（用于反序列化）
     * @param directionWidths 方向宽度映射
     */
    public void setDirectionWidths(Map<String, Float> directionWidths) {
        if (directionWidths != null) {
            this.directionWidths = new HashMap<>(directionWidths);
        }
    }

    public void setDirectionHeights(Map<String, Float> directionHeights) {
        if (directionHeights != null) {
            this.directionHeights = new HashMap<>(directionHeights);
        }
    }

    // ==================== 多方向位置偏移支持 ====================

    /**
     * 设置指定方向的位置偏移
     * @param direction 方向key (front/back/left/right)
     * @param offsetX X偏移
     * @param offsetY Y偏移
     * @param offsetZ Z偏移
     */
    public void setDirectionOffset(String direction, float offsetX, float offsetY, float offsetZ) {
        directionOffsets.put(direction, new float[]{offsetX, offsetY, offsetZ});
    }

    /**
     * 获取指定方向的位置偏移
     * @param direction 方向key (front/back/left/right)
     * @return 偏移数组[offsetX, offsetY, offsetZ]，如果不存在返回null
     */
    public float[] getDirectionOffset(String direction) {
        return directionOffsets.get(direction);
    }

    /**
     * 获取当前方向的位置偏移（使用继承策略）
     * 优先级：
     * 1. 当前方向的位置偏移
     * 2. 继承源方向的位置偏移（第一个有该属性的方向）
     * 3. 默认值null（由PuppetPartRenderer提供）
     * @return 偏移数组[offsetX, offsetY, offsetZ]，如果不存在返回null（表示使用渲染器默认值）
     */
    public float[] getCurrentDirectionOffset() {
        // 1. 尝试获取当前方向的位置偏移
        float[] offset = directionOffsets.get(currentDirection);
        if (offset != null) {
            return offset;
        }

        // 2. 使用继承策略：从第一个有该属性的方向继承
        String sourceDirection = getSourceDirectionForAttribute(directionOffsets);
        if (sourceDirection != null) {
            float[] sourceOffset = directionOffsets.get(sourceDirection);
            if (sourceOffset != null) {
                return sourceOffset;
            }
        }

        // 3. 所有方向都没有，返回null（使用渲染器默认值）
        return null;
    }

    /**
     * 获取方向位置偏移映射（用于序列化）
     * @return 方向位置偏移映射的副本
     */
    public Map<String, float[]> getDirectionOffsets() {
        return new HashMap<>(directionOffsets);
    }

    /**
     * 设置方向位置偏移映射（用于反序列化）
     * @param directionOffsets 方向位置偏移映射
     */
    public void setDirectionOffsets(Map<String, float[]> directionOffsets) {
        if (directionOffsets != null) {
            this.directionOffsets = new HashMap<>(directionOffsets);
        }
    }

    // ==================== 多方向旋转支持 ====================

    /**
     * 设置指定方向的旋转
     * @param direction 方向key (front/back/left/right)
     * @param rotationX X轴旋转角度
     * @param rotationY Y轴旋转角度
     * @param rotationZ Z轴旋转角度
     */
    public void setDirectionRotation(String direction, float rotationX, float rotationY, float rotationZ) {
        directionRotations.put(direction, new float[]{rotationX, rotationY, rotationZ});
    }

    /**
     * 获取指定方向的旋转
     * @param direction 方向key (front/back/left/right)
     * @return 旋转数组[rotationX, rotationY, rotationZ]，如果不存在返回null
     */
    public float[] getDirectionRotation(String direction) {
        return directionRotations.get(direction);
    }

    /**
     * 获取当前方向的旋转（使用继承策略）
     * 优先级：
     * 1. 当前方向的旋转
     * 2. 继承源方向的旋转（第一个有该属性的方向）
     * 3. 默认值null（由PuppetPartRenderer提供）
     * @return 旋转数组[rotationX, rotationY, rotationZ]，如果不存在返回null（表示使用渲染器默认值）
     */
    public float[] getCurrentDirectionRotation() {
        // 1. 尝试获取当前方向的旋转
        float[] rotation = directionRotations.get(currentDirection);
        if (rotation != null) {
            return rotation;
        }

        // 2. 使用继承策略：从第一个有该属性的方向继承
        String sourceDirection = getSourceDirectionForAttribute(directionRotations);
        if (sourceDirection != null) {
            float[] sourceRotation = directionRotations.get(sourceDirection);
            if (sourceRotation != null) {
                return sourceRotation;
            }
        }

        // 3. 所有方向都没有，返回null（使用渲染器默认值）
        return null;
    }

    /**
     * 获取方向旋转映射（用于序列化）
     * @return 方向旋转映射的副本
     */
    public Map<String, float[]> getDirectionRotations() {
        return new HashMap<>(directionRotations);
    }

    /**
     * 设置方向旋转映射（用于反序列化）
     * @param directionRotations 方向旋转映射
     */
    public void setDirectionRotations(Map<String, float[]> directionRotations) {
        if (directionRotations != null) {
            this.directionRotations = new HashMap<>(directionRotations);
        }
    }

    // ==================== 多方向内容中心偏移支持 ====================

    /**
     * 设置指定方向的内容中心偏移
     * @param direction 方向key (front/back/left/right)
     * @param centerX X轴中心偏移（相对于quad中心，范围 -0.5 到 +0.5）
     * @param centerY Y轴中心偏移（相对于quad中心，范围 -0.5 到 +0.5）
     */
    public void setDirectionContentCenter(String direction, float centerX, float centerY) {
        directionContentCenters.put(direction, new float[]{centerX, centerY});
    }

    /**
     * 获取指定方向的内容中心偏移
     * @param direction 方向key (front/back/left/right)
     * @return 内容中心偏移数组[centerX, centerY]，如果不存在返回null
     */
    public float[] getDirectionContentCenter(String direction) {
        return directionContentCenters.get(direction);
    }

    /**
     * 获取当前方向的内容中心偏移（使用继承策略）
     * 优先级：
     * 1. 当前方向的内容中心偏移
     * 2. 继承源方向的内容中心偏移（第一个有该属性的方向）
     * 3. 默认值[0, 0]（表示内容已居中，无需额外偏移）
     * @return 内容中心偏移数组[centerX, centerY]
     */
    public float[] getCurrentDirectionContentCenter() {
        // 1. 尝试获取当前方向的内容中心偏移
        float[] contentCenter = directionContentCenters.get(currentDirection);
        if (contentCenter != null) {
            return contentCenter;
        }

        // 2. 使用继承策略：从第一个有该属性的方向继承
        String sourceDirection = getSourceDirectionForAttribute(directionContentCenters);
        if (sourceDirection != null) {
            float[] sourceContentCenter = directionContentCenters.get(sourceDirection);
            if (sourceContentCenter != null) {
                return sourceContentCenter;
            }
        }

        // 3. 所有方向都没有，返回默认值[0, 0]（表示内容已居中）
        return new float[]{0.0f, 0.0f};
    }

    /**
     * 获取内容中心偏移映射（用于序列化）
     * @return 内容中心偏移映射的副本
     */
    public Map<String, float[]> getDirectionContentCenters() {
        return new HashMap<>(directionContentCenters);
    }

    /**
     * 设置内容中心偏移映射（用于反序列化）
     * @param directionContentCenters 内容中心偏移映射
     */
    public void setDirectionContentCenters(Map<String, float[]> directionContentCenters) {
        if (directionContentCenters != null) {
            this.directionContentCenters = new HashMap<>(directionContentCenters);
        }
    }

    // ==================== 多方向贴图旋转支持 ====================

    /**
     * 设置指定方向的贴图旋转角度
     * @param direction 方向key (front/back/left/right)
     * @param rotation 旋转角度（度，顺时针为正）
     */
    public void setDirectionTextureRotation(String direction, float rotation) {
        directionTextureRotations.put(direction, rotation);
    }

    /**
     * 获取指定方向的贴图旋转角度
     * @param direction 方向key (front/back/left/right)
     * @return 旋转角度（度），如果不存在返回null
     */
    public Float getDirectionTextureRotation(String direction) {
        return directionTextureRotations.get(direction);
    }

    /**
     * 获取当前方向的贴图旋转角度（使用继承策略）
     * 优先级：
     * 1. 当前方向的旋转角度
     * 2. 继承源方向的旋转角度（第一个有该属性的方向）
     * 3. 默认值0.0f（无旋转）
     * @return 旋转角度（度）
     */
    public float getCurrentDirectionTextureRotation() {
        // 1. 尝试获取当前方向的旋转角度
        Float rotation = directionTextureRotations.get(currentDirection);
        if (rotation != null) {
            return rotation;
        }

        // 2. 使用继承策略：从第一个有该属性的方向继承
        String sourceDirection = getSourceDirectionForAttribute(directionTextureRotations);
        if (sourceDirection != null) {
            Float sourceRotation = directionTextureRotations.get(sourceDirection);
            if (sourceRotation != null) {
                return sourceRotation;
            }
        }

        // 3. 所有方向都没有，返回默认值0.0f（无旋转）
        return 0.0f;
    }

    /**
     * 检查是否有指定方向的贴图旋转
     * @param direction 方向key (front/back/left/right)
     * @return true如果该方向有旋转设置
     */
    public boolean hasDirectionTextureRotation(String direction) {
        return directionTextureRotations.containsKey(direction) && directionTextureRotations.get(direction) != null;
    }

    /**
     * 获取贴图旋转映射（用于序列化）
     * @return 贴图旋转映射的副本
     */
    public Map<String, Float> getDirectionTextureRotations() {
        return new HashMap<>(directionTextureRotations);
    }

    /**
     * 设置贴图旋转映射（用于反序列化）
     * @param directionTextureRotations 贴图旋转映射
     */
    public void setDirectionTextureRotations(Map<String, Float> directionTextureRotations) {
        if (directionTextureRotations != null) {
            this.directionTextureRotations = new HashMap<>(directionTextureRotations);
        }
    }

    // ==================== 自由骨骼系统 Getter & Setter ====================

    /**
     * 获取骨骼类型
     * @return 骨骼类型（CONNECTED 或 FREE）
     */
    public BoneType getBoneType() {
        return boneType;
    }

    /**
     * 设置骨骼类型
     * @param boneType 骨骼类型（CONNECTED 或 FREE）
     */
    public void setBoneType(BoneType boneType) {
        this.boneType = boneType;
    }

    /**
     * 检查是否为自由骨骼
     * @return true如果是自由骨骼
     */
    public boolean isFreeBone() {
        return boneType == BoneType.FREE;
    }

    /**
     * 获取重力方向预设
     * @return 重力方向枚举
     */
    public GravityDirection getGravityDirection() {
        return gravityDirection;
    }

    /**
     * 设置重力方向预设
     * @param gravityDirection 重力方向枚举
     */
    public void setGravityDirection(GravityDirection gravityDirection) {
        this.gravityDirection = gravityDirection;
        // 如果不是自定义方向，更新自定义向量
        if (gravityDirection != GravityDirection.CUSTOM) {
            this.customGravityVector.set(gravityDirection.toVector());
        }
    }

    /**
     * 获取自定义重力向量
     * @return 重力向量
     */
    public Vector3f getCustomGravityVector() {
        return customGravityVector;
    }

    /**
     * 设置自定义重力向量
     * @param customGravityVector 重力向量
     */
    public void setCustomGravityVector(Vector3f customGravityVector) {
        this.customGravityVector = customGravityVector;
        this.gravityDirection = GravityDirection.CUSTOM;
    }

    /**
     * 设置自定义重力向量（通过xyz分量）
     * @param x X分量
     * @param y Y分量
     * @param z Z分量
     */
    public void setCustomGravityVector(float x, float y, float z) {
        this.customGravityVector.set(x, y, z);
        this.gravityDirection = GravityDirection.CUSTOM;
    }

    /**
     * 获取当前有效的重力向量
     * @return 重力向量（归一化）
     */
    public Vector3f getEffectiveGravityVector() {
        if (gravityDirection == GravityDirection.CUSTOM) {
            return customGravityVector.normalize();
        } else {
            return gravityDirection.toVector();
        }
    }

    /**
     * 获取自由度数值（向后兼容方法）
     * @return 自由度（0-1范围）
     */
    public float getFreedomValue() {
        return freedomValue;
    }

    /**
     * 设置自由度数值（向后兼容方法）
     * @param freedomValue 自由度（0-1范围，0=完全刚性，1=最大摆动）
     */
    public void setFreedomValue(float freedomValue) {
        // 限制在0-1范围内
        this.freedomValue = Math.max(0f, Math.min(1f, freedomValue));
    }

    /**
     * 设置指定方向的自由度
     * @param direction 方向key (front/back/left/right/up/down)
     * @param freedomValue 自由度值（0-1范围）
     */
    public void setDirectionFreedomValue(String direction, float freedomValue) {
        directionFreedomValues.put(direction, Math.max(0f, Math.min(1f, freedomValue)));
    }

    /**
     * 获取指定方向的自由度
     * @param direction 方向key (front/back/left/right/up/down)
     * @return 自由度值，如果不存在返回null
     */
    public Float getDirectionFreedomValue(String direction) {
        return directionFreedomValues.get(direction);
    }

    /**
     * 获取当前方向的自由度（使用继承策略）
     * 优先级：
     * 1. 当前方向的自由度
     * 2. 继承源方向的自由度（第一个有贴图的方向）
     * 3. 默认值freedomValue（向后兼容）
     * @return 自由度值（0-1范围）
     */
    public float getCurrentDirectionFreedomValue() {
        // 1. 尝试获取当前方向的自由度
        Float freedom = directionFreedomValues.get(currentDirection);
        if (freedom != null) {
            return freedom;
        }

        // 2. 使用继承策略：从第一个有贴图的方向继承
        String sourceDirection = getSourceDirectionForInheritance();
        if (sourceDirection != null) {
            Float sourceFreedom = directionFreedomValues.get(sourceDirection);
            if (sourceFreedom != null) {
                return sourceFreedom;
            }
        }

        // 3. 返回默认值（向后兼容）
        return freedomValue;
    }

    /**
     * 获取方向自由度映射（用于序列化）
     * @return 方向自由度映射的副本
     */
    public Map<String, Float> getDirectionFreedomValues() {
        return new HashMap<>(directionFreedomValues);
    }

    /**
     * 设置方向自由度映射（用于反序列化）
     * @param directionFreedomValues 方向自由度映射
     */
    public void setDirectionFreedomValues(Map<String, Float> directionFreedomValues) {
        if (directionFreedomValues != null) {
            this.directionFreedomValues = new HashMap<>(directionFreedomValues);
        }
    }

    // ==================== FreeBonePhysics 物理参数 Getter & Setter ====================

    public float getPhysMass() {
        return physMass;
    }

    public void setPhysMass(float physMass) {
        this.physMass = physMass;
    }

    public float getPhysDamping() {
        return physDamping;
    }

    public void setPhysDamping(float physDamping) {
        this.physDamping = physDamping;
    }

    /**
     * 设置指定方向的阻尼系数
     * @param direction 方向key (front/back/left/right/up/down)
     * @param damping 阻尼系数值（0-1范围）
     */
    public void setDirectionDamping(String direction, float damping) {
        directionDampingValues.put(direction, Math.max(0f, Math.min(1f, damping)));
    }

    /**
     * 获取指定方向的阻尼系数
     * @param direction 方向key (front/back/left/right/up/down)
     * @return 阻尼系数值，如果不存在返回null
     */
    public Float getDirectionDamping(String direction) {
        return directionDampingValues.get(direction);
    }

    /**
     * 获取当前方向的阻尼系数（使用继承策略）
     * 优先级：
     * 1. 当前方向的阻尼系数
     * 2. 继承源方向的阻尼系数（第一个有贴图的方向）
     * 3. 默认值physDamping（向后兼容）
     * @return 阻尼系数值（0-1范围）
     */
    public float getCurrentDirectionDamping() {
        // 1. 尝试获取当前方向的阻尼系数
        Float damping = directionDampingValues.get(currentDirection);
        if (damping != null) {
            return damping;
        }

        // 2. 使用继承策略：从第一个有贴图的方向继承
        String sourceDirection = getSourceDirectionForInheritance();
        if (sourceDirection != null) {
            Float sourceDamping = directionDampingValues.get(sourceDirection);
            if (sourceDamping != null) {
                return sourceDamping;
            }
        }

        // 3. 返回默认值（向后兼容）
        return physDamping;
    }

    /**
     * 获取方向阻尼系数映射（用于序列化）
     * @return 方向阻尼系数映射的副本
     */
    public Map<String, Float> getDirectionDampingValues() {
        return new HashMap<>(directionDampingValues);
    }

    /**
     * 设置方向阻尼系数映射（用于反序列化）
     * @param directionDampingValues 方向阻尼系数映射
     */
    public void setDirectionDampingValues(Map<String, Float> directionDampingValues) {
        if (directionDampingValues != null) {
            this.directionDampingValues = new HashMap<>(directionDampingValues);
        }
    }

    public float getPhysStiffness() {
        return physStiffness;
    }

    public void setPhysStiffness(float physStiffness) {
        this.physStiffness = physStiffness;
    }

    public float getPhysGravityStrength() {
        return physGravityStrength;
    }

    public void setPhysGravityStrength(float physGravityStrength) {
        this.physGravityStrength = physGravityStrength;
    }

    public float getPhysMaxSwingAngle() {
        return physMaxSwingAngle;
    }

    public void setPhysMaxSwingAngle(float physMaxSwingAngle) {
        this.physMaxSwingAngle = physMaxSwingAngle;
    }

    public float getPhysMaxVelocity() {
        return physMaxVelocity;
    }

    public void setPhysMaxVelocity(float physMaxVelocity) {
        this.physMaxVelocity = physMaxVelocity;
    }

    // ==================== Live2D风格相机跟随系统 Getters/Setters ====================

    /**
     * 获取水平方向的相机跟随自由度
     * @return 自由度（0-1范围，0=不晃动，1=最大晃动）
     */
    public float getCameraFollowFreedomX() {
        return cameraFollowFreedomX;
    }

    /**
     * 设置水平方向的相机跟随自由度
     * @param freedom 自由度（0-1范围，0=不晃动，1=最大晃动）
     */
    public void setCameraFollowFreedomX(float freedom) {
        this.cameraFollowFreedomX = Math.max(0f, Math.min(1f, freedom));
    }

    /**
     * 获取垂直方向的相机跟随自由度
     * @return 自由度（0-1范围，0=不晃动，1=最大晃动）
     */
    public float getCameraFollowFreedomY() {
        return cameraFollowFreedomY;
    }

    /**
     * 设置垂直方向的相机跟随自由度
     * @param freedom 自由度（0-1范围，0=不晃动，1=最大晃动）
     */
    public void setCameraFollowFreedomY(float freedom) {
        this.cameraFollowFreedomY = Math.max(0f, Math.min(1f, freedom));
    }

    // ==================== 摇摆系统 Getters/Setters ====================

    /**
     * 检查是否启用摇摆效果（向后兼容方法）
     * @return true如果启用摇摆
     */
    public boolean isSwingEnabled() {
        return swingEnabled;
    }

    /**
     * 设置是否启用摇摆效果（向后兼容方法）
     * @param enabled true=启用摇摆，false=禁用摇摆
     */
    public void setSwingEnabled(boolean enabled) {
        this.swingEnabled = enabled;
    }

    /**
     * 设置指定方向的摇摆开关
     * @param direction 方向key (front/back/left/right/up/down)
     * @param enabled 是否启用摇摆
     */
    public void setDirectionSwingEnabled(String direction, boolean enabled) {
        directionSwingEnabled.put(direction, enabled);
    }

    /**
     * 获取指定方向的摇摆开关
     * @param direction 方向key (front/back/left/right/up/down)
     * @return 是否启用摇摆，如果不存在返回null
     */
    public Boolean getDirectionSwingEnabled(String direction) {
        return directionSwingEnabled.get(direction);
    }

    /**
     * 获取当前方向的摇摆开关（使用继承策略）
     * 优先级：
     * 1. 当前方向的摇摆开关
     * 2. 继承源方向的摇摆开关（第一个有贴图的方向）
     * 3. 默认值swingEnabled（向后兼容）
     * @return 是否启用摇摆
     */
    public boolean getCurrentDirectionSwingEnabled() {
        // 1. 尝试获取当前方向的摇摆开关
        Boolean enabled = directionSwingEnabled.get(currentDirection);
        if (enabled != null) {
            return enabled;
        }

        // 2. 使用继承策略：从第一个有贴图的方向继承
        String sourceDirection = getSourceDirectionForInheritance();
        if (sourceDirection != null) {
            Boolean sourceEnabled = directionSwingEnabled.get(sourceDirection);
            if (sourceEnabled != null) {
                return sourceEnabled;
            }
        }

        // 3. 返回默认值（向后兼容）
        return swingEnabled;
    }

    /**
     * 获取方向摇摆开关映射（用于序列化）
     * @return 方向摇摆开关映射的副本
     */
    public Map<String, Boolean> getDirectionSwingEnabled() {
        return new HashMap<>(directionSwingEnabled);
    }

    /**
     * 设置方向摇摆开关映射（用于反序列化）
     * @param directionSwingEnabled 方向摇摆开关映射
     */
    public void setDirectionSwingEnabled(Map<String, Boolean> directionSwingEnabled) {
        if (directionSwingEnabled != null) {
            this.directionSwingEnabled = new HashMap<>(directionSwingEnabled);
        }
    }

    /**
     * 获取摇摆轴向量
     * @return 摇摆轴（归一化向量）
     */
    public Vector3f getSwingAxis() {
        return swingAxis.clone();
    }

    /**
     * 设置摇摆轴向量
     * @param axis 摇摆轴向量（将被自动归一化）
     */
    public void setSwingAxis(Vector3f axis) {
        if (axis != null && axis.lengthSquared() > 0.0001f) {
            this.swingAxis.set(axis).normalizeLocal();
        }
    }

    /**
     * 获取摇摆幅度（向后兼容方法）
     * @return 摇摆幅度（度）
     */
    public float getSwingAmplitude() {
        return swingAmplitude;
    }

    /**
     * 设置摇摆幅度（向后兼容方法）
     * @param amplitude 摇摆幅度（度，必须 >= 0）
     */
    public void setSwingAmplitude(float amplitude) {
        this.swingAmplitude = Math.max(0f, amplitude);
    }

    /**
     * 设置指定方向的摇摆幅度
     * @param direction 方向key (front/back/left/right/up/down)
     * @param amplitude 摇摆幅度值（度）
     */
    public void setDirectionSwingAmplitude(String direction, float amplitude) {
        directionSwingAmplitudes.put(direction, Math.max(0f, amplitude));
    }

    /**
     * 获取指定方向的摇摆幅度
     * @param direction 方向key (front/back/left/right/up/down)
     * @return 摇摆幅度值，如果不存在返回null
     */
    public Float getDirectionSwingAmplitude(String direction) {
        return directionSwingAmplitudes.get(direction);
    }

    /**
     * 获取当前方向的摇摆幅度（使用继承策略）
     * 优先级：
     * 1. 当前方向的摇摆幅度
     * 2. 继承源方向的摇摆幅度（第一个有贴图的方向）
     * 3. 默认值swingAmplitude（向后兼容）
     * @return 摇摆幅度值（度）
     */
    public float getCurrentDirectionSwingAmplitude() {
        // 1. 尝试获取当前方向的摇摆幅度
        Float amplitude = directionSwingAmplitudes.get(currentDirection);
        if (amplitude != null) {
            return amplitude;
        }

        // 2. 使用继承策略：从第一个有贴图的方向继承
        String sourceDirection = getSourceDirectionForInheritance();
        if (sourceDirection != null) {
            Float sourceAmplitude = directionSwingAmplitudes.get(sourceDirection);
            if (sourceAmplitude != null) {
                return sourceAmplitude;
            }
        }

        // 3. 返回默认值（向后兼容）
        return swingAmplitude;
    }

    /**
     * 获取方向摇摆幅度映射（用于序列化）
     * @return 方向摇摆幅度映射的副本
     */
    public Map<String, Float> getDirectionSwingAmplitudes() {
        return new HashMap<>(directionSwingAmplitudes);
    }

    /**
     * 设置方向摇摆幅度映射（用于反序列化）
     * @param directionSwingAmplitudes 方向摇摆幅度映射
     */
    public void setDirectionSwingAmplitudes(Map<String, Float> directionSwingAmplitudes) {
        if (directionSwingAmplitudes != null) {
            this.directionSwingAmplitudes = new HashMap<>(directionSwingAmplitudes);
        }
    }

    /**
     * 获取摇摆频率
     * @return 摇摆频率（Hz）
     */
    public float getSwingFrequency() {
        return swingFrequency;
    }

    /**
     * 设置摇摆频率
     * @param frequency 摇摆频率（Hz，必须 >= 0）
     */
    public void setSwingFrequency(float frequency) {
        this.swingFrequency = Math.max(0f, frequency);
    }

    /**
     * 获取摇摆相位偏移
     * @return 摇摆相位偏移（弧度）
     */
    public float getSwingPhaseOffset() {
        return swingPhaseOffset;
    }

    /**
     * 设置摇摆相位偏移
     * @param phaseOffset 摇摆相位偏移（弧度）
     */
    public void setSwingPhaseOffset(float phaseOffset) {
        this.swingPhaseOffset = phaseOffset;
    }

    /**
     * 计算当前摇摆角度
     * 使用正弦波公式：angle = amplitude × sin(2π × frequency × time + phaseOffset)
     * @param time 当前时间（秒）
     * @return 摇摆角度（弧度）
     */
    public float calculateSwingAngle(float time) {
        if (!swingEnabled) {
            return 0f;
        }
        // angle = amplitude × sin(2π × frequency × time + phaseOffset)
        float angleInDegrees = swingAmplitude * com.jme3.math.FastMath.sin(
            com.jme3.math.FastMath.TWO_PI * swingFrequency * time + swingPhaseOffset
        );
        return angleInDegrees * com.jme3.math.FastMath.DEG_TO_RAD;  // 转换为弧度
    }
}
