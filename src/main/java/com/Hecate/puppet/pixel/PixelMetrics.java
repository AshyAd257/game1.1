package com.Hecate.puppet.pixel;

import com.Hecate.puppet.volume.VolumeDefinition;

/** Artwork density is independent of the 20 viewing directions. */
public final class PixelMetrics {
    public static final int PIXELS_PER_METRE = 25;
    private PixelMetrics() {}
    /** Called only at the appearance preparation boundary, including loading previous pixel saves. */
    public static void normalize(VolumeDefinition d) {
        if(!d.isBakedPixelsEnabled() || d.getPixelsPerUnit()==PIXELS_PER_METRE)return;
        float ratio=PIXELS_PER_METRE/d.getPixelsPerUnit();
        int width=Math.max(1,Math.round(d.getWidthPx()*ratio)),height=Math.max(1,Math.round(d.getHeightPx()*ratio)),depth=Math.max(1,Math.round(d.getDepthPx()*ratio));
        var strip=d.getStrip3D();boolean blank=true;for(var surface:com.Hecate.puppet.volume.Strip3DDefinition.Surface.values())blank&=strip.layer(surface).getColorPath()==null;
        boolean defaultWindow=!strip.hasSelectionMask()&&strip.getSampleWidth()==strip.getFrameWidth()&&(blank||strip.getFrameWidth()==d.getWidthPx()&&strip.getFrameHeight()==d.getHeightPx());
        d.setDimensions(width,height,depth);
        if(d.getImportedMesh()!=null)d.setImportedMesh(d.getImportedMesh().atCurrentDensity());
        d.setPixelsPerUnit(PIXELS_PER_METRE);
        if(defaultWindow){strip.setFrameSize(width,height);strip.setSampleWidth(width);}
    }
}
