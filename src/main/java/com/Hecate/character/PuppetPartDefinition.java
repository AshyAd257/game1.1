package com.Hecate.character;

import com.Hecate.puppet.config.Vec3Config;
import java.util.HashMap;
import java.util.Map;

/**
 * 木偶部件定义
 * 描述一个可挂载的部件（身体部位、武器、装饰品等）
 */
public class PuppetPartDefinition {

    /** 部件唯一ID */
    private String partId;

    /** 部件名称 */
    private String name;

    /** 部件类型 */
    public enum PartType {
        BODY_HEAD,      // 头部
        BODY_NECK,      // 脖子
        BODY_TORSO,     // 躯干
        BODY_ARM_LEFT,  // 左臂
        BODY_ARM_RIGHT, // 右臂
        BODY_LEG_LEFT,  // 左腿
        BODY_LEG_RIGHT, // 右腿
        WEAPON_MAIN,    // 主武器
        WEAPON_OFF,     // 副武器
        ACCESSORY_BACK, // 背部装饰（背包、翅膀等）
        ACCESSORY_HEAD, // 头部装饰（帽子、头盔等）
        ACCESSORY_FACE  // 面部装饰（眼镜、面具等）
    }
    private PartType partType;

    /** 要绑定的3D骨骼名称 */
    private String targetBoneName;

    /** 部件尺寸（世界单位） */
    private float width;
    private float height;

    /** 相对骨骼的偏移 */
    private Vec3Config attachmentOffset;

    /** 相对骨骼的旋转（欧拉角，度数） */
    private Vec3Config attachmentRotation;

    /** 部件缩放（相对于部件原始尺寸） */
    private float scale = 1.0f;

    /** 多方向贴图路径 */
    private Map<String, String> directionTextures = new HashMap<>();

    /** 渲染优先级（用于遮挡排序） */
    private int renderPriority;

    /** 是否启用Billboard渲染 */
    private boolean billboardEnabled = true;

    /** 是否跟随相机方向切换贴图 */
    private boolean autoDirectionSwitch = true;

    public PuppetPartDefinition() {
    }

    public PuppetPartDefinition(String partId) {
        this.partId = partId;
    }

    // ========== Getters and Setters ==========

    public String getPartId() {
        return partId;
    }

    public void setPartId(String partId) {
        this.partId = partId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public PartType getPartType() {
        return partType;
    }

    public void setPartType(PartType partType) {
        this.partType = partType;
    }

    public String getTargetBoneName() {
        return targetBoneName;
    }

    public void setTargetBoneName(String targetBoneName) {
        this.targetBoneName = targetBoneName;
    }

    public float getWidth() {
        return width;
    }

    public void setWidth(float width) {
        this.width = width;
    }

    public float getHeight() {
        return height;
    }

    public void setHeight(float height) {
        this.height = height;
    }

    public Vec3Config getAttachmentOffset() {
        return attachmentOffset;
    }

    public void setAttachmentOffset(Vec3Config attachmentOffset) {
        this.attachmentOffset = attachmentOffset;
    }

    public Vec3Config getAttachmentRotation() {
        return attachmentRotation;
    }

    public void setAttachmentRotation(Vec3Config attachmentRotation) {
        this.attachmentRotation = attachmentRotation;
    }

    public float getScale() {
        return scale;
    }

    public void setScale(float scale) {
        this.scale = scale;
    }

    public Map<String, String> getDirectionTextures() {
        return directionTextures;
    }

    public void setDirectionTextures(Map<String, String> directionTextures) {
        this.directionTextures = directionTextures;
    }

    public int getRenderPriority() {
        return renderPriority;
    }

    public void setRenderPriority(int renderPriority) {
        this.renderPriority = renderPriority;
    }

    public boolean isBillboardEnabled() {
        return billboardEnabled;
    }

    public void setBillboardEnabled(boolean billboardEnabled) {
        this.billboardEnabled = billboardEnabled;
    }

    public boolean isAutoDirectionSwitch() {
        return autoDirectionSwitch;
    }

    public void setAutoDirectionSwitch(boolean autoDirectionSwitch) {
        this.autoDirectionSwitch = autoDirectionSwitch;
    }
}
