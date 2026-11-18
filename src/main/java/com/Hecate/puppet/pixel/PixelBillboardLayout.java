package com.Hecate.puppet.pixel;

import com.jme3.math.*;

/** Two bounded billboards meet along the same complete source-row boundary. No pose capture. */
public record PixelBillboardLayout(Vector3f right,Vector3f lowerUp,Vector3f upperUp,
                                   Vector3f lowerCentre,Vector3f upperCentre,float lowerPitch,float upperPitch) {
    public static PixelBillboardLayout create(BakedPixelDefinition d,float yaw,float pitch,
                                               PixelViewSelector.Selection selection,float pixelsPerUnit) {
        float low=d.elevation(selection.lowerBand()),high=d.elevation(selection.upperBand());
        float limit=(high-low-PixelViewSelector.transitionWidth(d,selection.lowerBand(),selection.upperBand()))*.5f;
        float lp=FastMath.clamp(pitch,low-limit,low+limit),hp=FastMath.clamp(pitch,high-limit,high+limit);
        Vector3f r=PixelViewSelector.right(yaw);
        Vector3f lu=PixelViewSelector.direction(yaw,lp).cross(r).normalizeLocal();
        Vector3f hu=PixelViewSelector.direction(yaw,hp).cross(r).normalizeLocal();
        float t=selection.revealedRows()/(float)selection.size();
        float cut=(selection.size()*.5f-selection.revealedRows())/pixelsPerUnit;
        Vector3f seam=PixelViewSelector.direction(yaw,lp+(hp-lp)*t).cross(r).normalizeLocal().multLocal(cut);
        return new PixelBillboardLayout(r,lu,hu,seam.subtract(lu.mult(cut)),seam.subtract(hu.mult(cut)),lp,hp);
    }
}
