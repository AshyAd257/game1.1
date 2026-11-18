package com.Hecate.puppet.pixel;

import com.jme3.math.*;

/** Pure lookup. Exactly 20 yaw windows; neighbouring elevation masks meet at integer rows. */
public final class PixelViewSelector {
    private PixelViewSelector() {}
    public record Selection(int yawStep, int lowerBand, int upperBand, int revealedRows, int size) {
        public int viewForRow(int topDownRow) { return (topDownRow < revealedRows ? upperBand : lowerBand) * PixelAtlas.YAW_VIEWS + yawStep; }
    }
    public static Selection select(BakedPixelDefinition d, float yaw, float pitch, int size) {
        if(!Float.isFinite(yaw)||!Float.isFinite(pitch))throw new IllegalArgumentException("观察角度无效");
        int step = Math.floorMod((int)Math.floor((yaw + PixelAtlas.YAW_DEGREES/2) / PixelAtlas.YAW_DEGREES),PixelAtlas.YAW_VIEWS);
        pitch=FastMath.clamp(pitch,-90,90);
        int pair=0;
        while(pair<3 && pitch>d.elevation(BakedPixelDefinition.orderedBand(pair+1)))pair++;
        int low=BakedPixelDefinition.orderedBand(pair),high=BakedPixelDefinition.orderedBand(pair+1);
        float boundary = (d.elevation(low)+d.elevation(high))*.5f;
        float t = (pitch-boundary)/transitionWidth(d,low,high)+.5f;
        // Camera quaternion round-off must not send an exact half-row to the other image.
        int rows = Math.max(0,Math.min(size,Math.round(t*size+.0001f)));
        return new Selection(step,low,high,rows,size);
    }
    static float transitionWidth(BakedPixelDefinition d,int low,int high) {
        return Math.min(d.getTransitionDegrees(),d.elevation(high)-d.elevation(low));
    }
    public static float localYaw(Vector3f toward,Vector3f cameraRight) {
        return (toward.x*toward.x+toward.z*toward.z<1e-7f
                ?FastMath.atan2(-cameraRight.z,cameraRight.x):FastMath.atan2(toward.x,toward.z))*FastMath.RAD_TO_DEG;
    }
    public static Vector3f direction(float yaw, float pitch) {
        float y=yaw*FastMath.DEG_TO_RAD,p=pitch*FastMath.DEG_TO_RAD;
        if(pitch==90)return new Vector3f(0,1,0);
        if(pitch==-90)return new Vector3f(0,-1,0);
        return new Vector3f(FastMath.sin(y)*FastMath.cos(p),FastMath.sin(p),FastMath.cos(y)*FastMath.cos(p));
    }
    public static Vector3f right(float yaw) {
        float a=yaw*FastMath.DEG_TO_RAD; return new Vector3f(FastMath.cos(a),0,-FastMath.sin(a));
    }
}
