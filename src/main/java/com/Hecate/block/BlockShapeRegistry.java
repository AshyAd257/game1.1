package com.Hecate.block;

import com.jme3.math.Vector3f;

import java.util.HashMap;
import java.util.Map;

/**
 * 方块碰撞尺寸管理器 - 专门管理各方块ID的物理碰撞盒尺寸，与BlockRegistry（管理模型/纹理/硬度）分开。
 * 默认方块是满格1x1x1的实心立方体；像wood1这种细长柱子需要单独登记一个比整格小得多的碰撞盒，
 * 否则玩家会被"看起来很细的柱子"像整块方块一样挡住。
 */
public class BlockShapeRegistry {

    // 未登记的方块ID默认视为满格1x1x1的实心立方体，中心与方块位置重合（无偏移）
    private static final Vector3f DEFAULT_FULL_SIZE = new Vector3f(1f, 1f, 1f);
    private static final Vector3f DEFAULT_CENTER_OFFSET = Vector3f.ZERO;

    // 每个方块ID对应的碰撞盒完整尺寸（宽/高/深，单位=格子，1.0=占满一整格）
    private final Map<String, Vector3f> fullSizes = new HashMap<>();
    // 每个方块ID对应的碰撞盒中心相对方块位置的偏移（格子单位）。大多数方块（如wood1这种
    // 关于中心对称的柱子）不需要偏移，默认(0,0,0)；但像半砖这种贴着格子下沿放置、几何范围
    // 是[-0.5,0.0]而不是对称的[-0.25,0.25]的方块，碰撞盒中心必须跟着往下偏移0.25，否则
    // 碰撞盒会悬浮在视觉模型上半段，出现"贴图贴地但碰撞体悬空"的错位。
    private final Map<String, Vector3f> centerOffsets = new HashMap<>();

    /**
     * 登记某个方块ID的碰撞盒完整尺寸（碰撞盒中心与方块位置重合，无偏移）
     * @param width  X方向尺寸（格子单位，1.0=占满一整格）
     * @param height Y方向尺寸
     * @param depth  Z方向尺寸
     */
    public void registerShape(String blockId, float width, float height, float depth) {
        fullSizes.put(blockId, new Vector3f(width, height, depth));
    }

    /**
     * 登记某个方块ID的碰撞盒完整尺寸+中心偏移，用于几何体本身不是关于方块位置居中对称的
     * 方块（如半砖：Y方向范围[-0.5,0.0]，中心需要相对方块位置往下偏移0.25）。
     * @param offsetX/Y/Z 碰撞盒中心相对方块位置的偏移（格子单位）
     */
    public void registerShapeWithOffset(String blockId, float width, float height, float depth,
                                          float offsetX, float offsetY, float offsetZ) {
        fullSizes.put(blockId, new Vector3f(width, height, depth));
        centerOffsets.put(blockId, new Vector3f(offsetX, offsetY, offsetZ));
    }

    /**
     * 登记一族朝向性方块（如原木）的碰撞盒尺寸，与BlockRegistry.registerDirectionalBlockFamily
     * 的方块ID/朝向轴约定保持一致（baseId=竖直Y轴，baseId_x=横放X轴，baseId_z=横放Z轴）：
     * 长轴对应的那个维度用length，另外两个短维度都用thickness。
     *
     * @param thickness 柱子的粗细（两个短边的尺寸，格子单位）
     * @param length    柱子的长度（长轴方向的尺寸，通常是1.0，即占满一整格的长度）
     */
    public void registerDirectionalShape(String baseId, float thickness, float length) {
        registerShape(baseId, thickness, length, thickness);           // Y轴：长边在高度方向
        registerShape(baseId + "_x", length, thickness, thickness);    // X轴：长边在宽度方向
        registerShape(baseId + "_z", thickness, thickness, length);    // Z轴：长边在深度方向
    }

    /**
     * 获取某方块ID的碰撞盒完整尺寸；未登记过的方块返回默认满格尺寸(1,1,1)
     */
    public Vector3f getFullSize(String blockId) {
        return fullSizes.getOrDefault(blockId, DEFAULT_FULL_SIZE);
    }

    /**
     * 获取碰撞盒半尺寸（用于以"碰撞盒中心"为基准构造AABB：中心±半尺寸；
     * 中心本身可能相对方块位置有偏移，见{@link #getCenterOffset}）
     */
    public Vector3f getHalfExtents(String blockId) {
        return getFullSize(blockId).mult(0.5f);
    }

    /**
     * 获取某方块ID的碰撞盒中心相对方块位置的偏移；未登记过偏移的方块返回(0,0,0)
     * （即碰撞盒中心与方块位置重合，绝大多数方块都是这种情况）
     */
    public Vector3f getCenterOffset(String blockId) {
        return centerOffsets.getOrDefault(blockId, DEFAULT_CENTER_OFFSET);
    }
}
