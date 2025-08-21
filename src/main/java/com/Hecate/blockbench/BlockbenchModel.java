package com.Hecate.blockbench;

import com.jme3.scene.Spatial;

/**
 * 表示一个Blockbench模型
 */
public class BlockbenchModel {
    private final String id;
    private final String name;
    private final String modelPath;
    private Spatial spatial;
    private boolean loaded;

    public BlockbenchModel(String id, String name, String modelPath) {
        this.id = id;
        this.name = name;
        this.modelPath = modelPath;
        this.loaded = false;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getModelPath() {
        return modelPath;
    }

    public Spatial getSpatial() {
        return spatial;
    }

    public void setSpatial(Spatial spatial) {
        this.spatial = spatial;
        this.loaded = (spatial != null);
    }

    public boolean isLoaded() {
        return loaded;
    }

    @Override
    public String toString() {
        return "BlockbenchModel{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", loaded=" + loaded +
                '}';
    }
}
