package com.Hecate.world;

import java.util.Objects;

/**
 * 表示区块在世界中的位置
 */
public class ChunkPosition {
    private final int x;
    private final int y;
    private final int z;

    public ChunkPosition(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChunkPosition that = (ChunkPosition) o;
        return x == that.x && y == that.y && z == that.z;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z);
    }

    @Override
    public String toString() {
        return "ChunkPosition{" +
                "x=" + x +
                ", y=" + y +
                ", z=" + z +
                '}';
    }
}