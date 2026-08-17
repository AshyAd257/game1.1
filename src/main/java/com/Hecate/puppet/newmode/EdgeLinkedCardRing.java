package com.Hecate.puppet.newmode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 边贴边卡片环（Edge-Linked Card Ring）
 *
 * 核心思想：不投影"卡片中心"，而是投影正N边形的N个顶点。
 * 卡片 i 横跨顶点 i 和顶点 i+1，因此相邻卡片字面上共享同一个
 * 投影顶点 —— 接缝在数学上是零宽度的，不存在浮点缝隙问题。
 *
 * 卡片的屏幕宽度是旋转的自然产物：
 *   正对镜头 → 接近满宽
 *   侧向     → 被压缩成窄条
 *   过侧棱   → 宽度归零后翻到背面（被剔除）
 * 宽度本身就是"该视角有多正对镜头"的度量，可直接当象限权重用。
 *
 * 该类是纯数学层，不依赖任何引擎。每帧调用 solve() 后，
 * 用返回的 CardSpan 列表去驱动 JME 场景图：
 *   - offsetX  → 卡片quad沿"摄像机右方向"的水平位置
 *   - width    → quad宽度（或用于crossfade权重）
 *   - depth    → 写回你现有的 Z-offset 优先级排序系统
 *   - facing   → false 时直接 setCullHint(Always)
 */
public class EdgeLinkedCardRing {

    /** 象限/卡片数量。4 = 前后左右，8 = 含斜角 */
    private final int segmentCount;

    /** 环半径，决定卡片在深度方向上的分离程度（影响遮挡排序的余量） */
    private final float radius;

    /**
     * 透视强度。demo里是 FOV/(CAMZ - z)。
     * 如果你的puppet是正交渲染，把 perspective 设为 false，
     * scale 恒为 1，只保留 offsetX 和 depth。
     */
    private final boolean perspective;
    private final float fov;      // 仅 perspective=true 时使用
    private final float cameraZ;  // 仅 perspective=true 时使用，必须 > radius

    public EdgeLinkedCardRing(int segmentCount, float radius,
                               boolean perspective, float fov, float cameraZ) {
        if (perspective && cameraZ <= radius) {
            throw new IllegalArgumentException("cameraZ 必须大于 radius，否则顶点会穿过相机平面");
        }
        this.segmentCount = segmentCount;
        this.radius = radius;
        this.perspective = perspective;
        this.fov = fov;
        this.cameraZ = cameraZ;
    }

    /** 单个投影顶点：x 为沿摄像机右方向的偏移，z 为朝向摄像机的深度 */
    private static final class ProjectedVertex {
        final float screenX; // 已含透视缩放的水平位置
        final float scale;   // 透视缩放因子（正交时恒为1）
        final float z;       // 世界深度，越大越靠近镜头

        ProjectedVertex(float screenX, float scale, float z) {
            this.screenX = screenX;
            this.scale = scale;
            this.z = z;
        }
    }

    /** 每帧求解结果：一张卡片的屏幕跨度 */
    public static final class CardSpan {
        public final int index;      // 卡片编号（对应哪套象限图）
        public final float leftX;    // 左边缘（摄像机右方向坐标）
        public final float rightX;   // 右边缘
        public final float width;    // rightX - leftX，恒为正
        public final float scale;    // 两端顶点缩放的平均值，用于高度
        public final float depth;    // 中点深度，喂给你的Z-offset排序
        public final boolean facing; // 是否朝向镜头（背面剔除用）

        CardSpan(int index, float leftX, float rightX,
                 float scale, float depth, boolean facing) {
            this.index = index;
            this.leftX = Math.min(leftX, rightX);
            this.rightX = Math.max(leftX, rightX);
            this.width = this.rightX - this.leftX;
            this.scale = scale;
            this.depth = depth;
            this.facing = facing;
        }
    }

    /**
     * @param relativeYaw 摄像机相对角色的水平角（弧度）。
     *                    在JME里通常是 cameraYaw - puppetYaw，
     *                    可以复用你 ghost 系统里已经在算的那个量。
     * @return 按深度升序排列的卡片列表（先画远的，后画近的；
     *         如果用Z-offset排序系统则直接取 depth 字段即可）
     */
    public List<CardSpan> solve(float relativeYaw) {
        float step = (float) (Math.PI * 2.0 / segmentCount);

        // 1. 投影 N 个共享顶点。
        //    偏移 -step/2 让 0 号卡片在 relativeYaw=0 时正对镜头。
        ProjectedVertex[] verts = new ProjectedVertex[segmentCount];
        for (int j = 0; j < segmentCount; j++) {
            float angle = j * step - step / 2f + relativeYaw;
            float x = (float) Math.sin(angle) * radius;
            float z = (float) Math.cos(angle) * radius;
            float s = perspective ? fov / (cameraZ - z) : 1f;
            verts[j] = new ProjectedVertex(x * s, s, z);
        }

        // 2. 组装卡片：卡片 i 横跨顶点 i 和 (i+1)%N。
        //    朝向判定：右顶点的投影位置在左顶点右侧 → 面向镜头。
        List<CardSpan> cards = new ArrayList<>(segmentCount);
        for (int i = 0; i < segmentCount; i++) {
            ProjectedVertex l = verts[i];
            ProjectedVertex r = verts[(i + 1) % segmentCount];
            boolean facing = r.screenX > l.screenX;
            cards.add(new CardSpan(
                    i,
                    l.screenX, r.screenX,
                    (l.scale + r.scale) / 2f,
                    (l.z + r.z) / 2f,
                    facing));
        }

        // 3. 深度升序（远 → 近）。接你的优先级排序系统时，
        //    建议只在排序结果真的变化时才触发 sortByPriority()
        //    （dirty-flag），顺序交换只发生在过侧棱的瞬间。
        cards.sort(Comparator.comparingDouble(c -> c.depth));
        return cards;
    }

    /**
     * 可选工具：把宽度归一化成 0~1 的"正对度"权重。
     * 满宽 = 2 * radius * sin(PI / N)（正对镜头时的弦长投影上限附近）。
     * 可用于：crossfade透明度、或作为你纸片位移系统的输入。
     */
    public float facingWeight(CardSpan card) {
        float maxWidth = 2f * radius * (float) Math.sin(Math.PI / segmentCount);
        if (perspective) maxWidth *= fov / (cameraZ - radius);
        return Math.min(1f, card.width / maxWidth);
    }
}
