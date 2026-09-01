package com.Hecate.item.world;

import com.Hecate.item.ItemStack;
import com.jme3.math.Vector3f;

/**
 * 世界里的一个掉落物：一份 {@link ItemStack} 数据 + 位置 + 视觉表现（{@link ItemVisual}）。
 * 落地后静止不动，不需要任何动画（旋转/漂浮等特效留给未来，不是本次范围）。
 * <p>本类不关心自己是怎么生成的（丢弃/调试命令/怪物死亡掉落等），也不关心自己会怎么被
 * 拾取——生成和销毁都由 {@link WorldItemManager} 负责。
 */
public class WorldItemEntity {
    private final ItemStack itemStack;
    private final Vector3f position;
    private final ItemVisual visual;

    public WorldItemEntity(ItemStack itemStack, Vector3f position, ItemVisual visual) {
        this.itemStack = itemStack;
        this.position = position.clone();
        this.visual = visual;
        visual.setPosition(this.position);
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public Vector3f getPosition() {
        return position.clone();
    }

    public ItemVisual getVisual() {
        return visual;
    }

    /**
     * 从场景图摘除视觉表现（拾取/清理时调用，调用后本实体不应再被使用）。
     */
    public void dispose() {
        visual.dispose();
    }
}
