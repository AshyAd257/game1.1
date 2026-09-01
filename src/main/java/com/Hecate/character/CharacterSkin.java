package com.Hecate.character;

import com.Hecate.puppet.config.Vec3Config;
import java.util.ArrayList;
import java.util.List;

/**
 * 角色皮肤配置
 * 定义一套完整的角色外观（多个部件的组合）
 */
public class CharacterSkin {

    /** 皮肤ID */
    private String skinId;

    /** 皮肤名称 */
    private String name;

    /** 基础3D模型路径 */
    private String baseModelPath;

    /** 模型全局缩放（用于调整3D模型与2D部件的比例） */
    private float modelScale = 1.0f;

    /** 包含的部件列表 */
    private List<SkinPartSlot> partSlots;

    public CharacterSkin() {
        this.partSlots = new ArrayList<>();
    }

    public CharacterSkin(String skinId) {
        this.skinId = skinId;
        this.partSlots = new ArrayList<>();
    }

    /**
     * 皮肤部件槽位
     * 描述该皮肤在某个槽位上使用哪个部件
     */
    public static class SkinPartSlot {
        /** 部件槽位类型 */
        private PuppetPartDefinition.PartType slotType;

        /** 使用的部件ID */
        private String partId;

        /** 该部件的个性化缩放（覆盖部件定义中的scale） */
        private Float customScale; // null表示使用部件默认缩放

        /** 该部件的个性化偏移（覆盖部件定义中的offset） */
        private Vec3Config customOffset; // null表示使用部件默认偏移

        public SkinPartSlot() {
        }

        public SkinPartSlot(PuppetPartDefinition.PartType slotType, String partId) {
            this.slotType = slotType;
            this.partId = partId;
        }

        // ========== Getters and Setters ==========

        public PuppetPartDefinition.PartType getSlotType() {
            return slotType;
        }

        public void setSlotType(PuppetPartDefinition.PartType slotType) {
            this.slotType = slotType;
        }

        public String getPartId() {
            return partId;
        }

        public void setPartId(String partId) {
            this.partId = partId;
        }

        public Float getCustomScale() {
            return customScale;
        }

        public void setCustomScale(Float customScale) {
            this.customScale = customScale;
        }

        public Vec3Config getCustomOffset() {
            return customOffset;
        }

        public void setCustomOffset(Vec3Config customOffset) {
            this.customOffset = customOffset;
        }
    }

    // ========== Getters and Setters ==========

    public String getSkinId() {
        return skinId;
    }

    public void setSkinId(String skinId) {
        this.skinId = skinId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBaseModelPath() {
        return baseModelPath;
    }

    public void setBaseModelPath(String baseModelPath) {
        this.baseModelPath = baseModelPath;
    }

    public float getModelScale() {
        return modelScale;
    }

    public void setModelScale(float modelScale) {
        this.modelScale = modelScale;
    }

    public List<SkinPartSlot> getPartSlots() {
        return partSlots;
    }

    public void setPartSlots(List<SkinPartSlot> partSlots) {
        this.partSlots = partSlots;
    }

    public void addPartSlot(SkinPartSlot slot) {
        this.partSlots.add(slot);
    }
}
