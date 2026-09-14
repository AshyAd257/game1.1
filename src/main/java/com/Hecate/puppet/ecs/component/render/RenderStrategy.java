package com.Hecate.puppet.ecs.component.render;

import com.jme3.scene.Node;

/**
 * 渲染策略接口
 * 定义不同渲染方式的统一接口
 *
 * 【策略模式】
 * 解决旧Bone类职责过重的问题：
 * - 旧实现：Bone类包含8种互斥的渲染模式，所有字段都存在
 * - 新实现：使用策略模式，每个实体只有一个RenderStrategy
 *
 * 支持的渲染策略：
 * - SpriteRenderStrategy: 6方向精灵（front/back/left/right/up/down）
 * - RotationStripRenderStrategy: 旋转条状贴图
 * - Model3DRenderStrategy: 3D模型
 * - BillboardRenderStrategy: 广告牌模式
 */
public interface RenderStrategy {

    /**
     * 渲染策略类型
     */
    enum Type {
        SPRITE_6DIR,        // 6方向精灵
        ROTATION_STRIP,     // 旋转条状
        MODEL_3D,           // 3D模型
        BILLBOARD           // 广告牌
    }

    /**
     * 获取策略类型
     *
     * @return 策略类型
     */
    Type getType();

    /**
     * 初始化渲染资源
     * 加载纹理、创建几何体等
     *
     * @param parentNode 父节点
     */
    void initialize(Node parentNode);

    /**
     * 更新渲染
     * 每帧调用，更新位置、旋转、纹理等
     *
     * @param tpf 时间步长
     */
    void update(float tpf);

    /**
     * 清理渲染资源
     * 移除几何体、释放纹理等
     */
    void cleanup();

    /**
     * 设置可见性
     *
     * @param visible 是否可见
     */
    void setVisible(boolean visible);

    /**
     * 获取可见性
     *
     * @return 是否可见
     */
    boolean isVisible();

    /**
     * 克隆策略
     *
     * @return 策略的深拷贝
     */
    RenderStrategy clone();
}
