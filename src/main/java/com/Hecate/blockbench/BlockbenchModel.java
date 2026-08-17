package com.Hecate.blockbench;

import com.Hecate.model.AbstractModel;

/**
 * 表示一个Blockbench模型
 */
public class BlockbenchModel extends AbstractModel {
    private final String name;

    public BlockbenchModel(String id, String name, String modelPath) {
        super(id, modelPath);
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public String getTypeName() {
        return "Blockbench";
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
