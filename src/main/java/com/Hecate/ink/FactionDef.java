package com.Hecate.ink;

import com.jme3.math.ColorRGBA;

/**
 * 阵营定义
 * 运行时查表用，定义一个子阵营的视觉属性
 */
public class FactionDef {

    /**
     * 视觉血统：光/暗
     * 只有这两个值，不做开放扩展
     */
    public enum VisualLineage {
        LIGHT((byte) 0),  // 光属性
        DARK((byte) 1);   // 暗属性

        private final byte id;

        VisualLineage(byte id) {
            this.id = id;
        }

        public byte getId() {
            return id;
        }

        public static VisualLineage fromId(byte id) {
            return id == 0 ? LIGHT : DARK;
        }
    }

    // 阵营ID（客观归属，查表用的key）
    private final int factionId;

    // 视觉血统（光/暗）
    private final VisualLineage visualLineage;

    // 基础色相（ColorResolver 从这里取，不写死）
    private final ColorRGBA baseHue;

    // 阵营名称（可选，用于调试）
    private final String name;

    public FactionDef(int factionId, VisualLineage visualLineage, ColorRGBA baseHue, String name) {
        this.factionId = factionId;
        this.visualLineage = visualLineage;
        this.baseHue = baseHue.clone();
        this.name = name;
    }

    // Getters
    public int getFactionId() {
        return factionId;
    }

    public VisualLineage getVisualLineage() {
        return visualLineage;
    }

    public ColorRGBA getBaseHue() {
        return baseHue.clone();
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return String.format("FactionDef[id=%d, lineage=%s, name=%s]", factionId, visualLineage, name);
    }
}
