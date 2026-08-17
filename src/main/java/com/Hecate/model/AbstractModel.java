package com.Hecate.model;

import com.jme3.scene.Spatial;

/**
 * 抽象模型基类
 * 提供所有模型类型的通用字段和方法
 */
public abstract class AbstractModel {
    protected final String id;
    protected final String modelPath;
    protected Spatial spatial;
    protected boolean loaded;

    /**
     * 构造函数
     * @param id 模型ID
     * @param modelPath 模型文件路径
     */
    public AbstractModel(String id, String modelPath) {
        this.id = id;
        this.modelPath = modelPath;
        this.loaded = false;
    }

    /**
     * 获取模型ID
     * @return 模型ID
     */
    public String getId() {
        return id;
    }

    /**
     * 获取模型ID（兼容方法）
     * @return 模型ID
     */
    public String getModelId() {
        return id;
    }

    /**
     * 获取模型文件路径
     * @return 模型路径
     */
    public String getModelPath() {
        return modelPath;
    }

    /**
     * 获取加载的Spatial对象
     * @return Spatial对象
     */
    public Spatial getSpatial() {
        return spatial;
    }

    /**
     * 设置Spatial对象
     * @param spatial Spatial对象
     */
    public void setSpatial(Spatial spatial) {
        this.spatial = spatial;
        this.loaded = (spatial != null);
    }

    /**
     * 判断模型是否已加载
     * @return 已加载返回true
     */
    public boolean isLoaded() {
        return loaded;
    }

    /**
     * 获取模型类型名称
     * @return 模型类型名称
     */
    public abstract String getTypeName();

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "id='" + id + '\'' +
                ", modelPath='" + modelPath + '\'' +
                ", loaded=" + loaded +
                '}';
    }
}
