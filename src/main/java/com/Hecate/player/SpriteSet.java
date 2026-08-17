package com.Hecate.player;

import com.jme3.texture.Texture;
import java.util.ArrayList;
import java.util.List;

/**
 * 🎨 精灵集合 - 存储一组动画帧（简化版）
 */
public class SpriteSet {
    private String name;
    private List<Texture> frames;

    public SpriteSet(String name) {
        this.name = name;
        this.frames = new ArrayList<>();
    }

    public void addFrame(Texture texture) {
        frames.add(texture);
    }

    public void setFrames(List<Texture> frames) {
        this.frames = new ArrayList<>(frames);
    }

    public int getFrameCount() {
        return frames.size();
    }

    public Texture getFrame(int index) {
        if (index >= 0 && index < frames.size()) {
            return frames.get(index);
        }
        return frames.isEmpty() ? null : frames.get(0);
    }

    public String getName() {
        return name;
    }

    public boolean isEmpty() {
        return frames.isEmpty();
    }

    public List<Texture> getAllFrames() {
        return new ArrayList<>(frames);
    }
}
