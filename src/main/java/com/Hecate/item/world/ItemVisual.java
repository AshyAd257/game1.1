package com.Hecate.item.world;

import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;

/**
 * 掉落物的视觉表现接口——{@link WorldItemEntity} 只持有一个 {@link ItemVisual}，
 * 不关心具体渲染的是图标平面(billboard)、3D模型还是别的东西。
 * <p>这一层存在的唯一目的：以后要把"图标平面"换成"真实3D模型"时，只需要新写一个
 * 实现类（如 {@code ModelItemVisual}）替换 {@link WorldItemManager} 里创建视觉表现的
 * 那一行，不需要改动 {@link WorldItemEntity}/{@link WorldItemManager} 任何拾取/生命周期逻辑。
 */
public interface ItemVisual {
    /**
     * 视觉表现挂载的根节点/几何体，由调用方负责attach到场景图。
     */
    Spatial getSpatial();

    /**
     * 设置世界坐标位置（掉落物落地后不会移动，这个方法只在生成时调用一次；
     * 若未来支持"物品在地上有漂浮/旋转动画"，也是这一层的实现细节，不影响外部接口）。
     */
    void setPosition(Vector3f position);

    /**
     * 从场景图上摘除并释放资源（拾取/清理时调用）。
     */
    void dispose();
}
