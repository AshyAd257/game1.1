package com.Hecate.block;

import com.jme3.math.Vector3f;

/**
 * 方块的朝向轴（用于原木一类的方向性方块：竖直摆放还是横放，横放又分两个方向）
 */
public enum Axis {
    X, Y, Z;

    /**
     * 根据被点击面的法线判断新方块应沿哪个轴摆放（参考MC原木朝向规则）：
     * 点击的是顶面/底面（法线Y分量占主导） -> 竖直摆放（Y轴）
     * 点击的是东/西侧面（法线X分量占主导） -> 沿X轴横放
     * 点击的是南/北侧面（法线Z分量占主导） -> 沿Z轴横放
     */
    public static Axis fromFaceNormal(Vector3f normal) {
        float ax = Math.abs(normal.x);
        float ay = Math.abs(normal.y);
        float az = Math.abs(normal.z);

        if (ay >= ax && ay >= az) {
            return Y;
        } else if (ax >= az) {
            return X;
        } else {
            return Z;
        }
    }
}
