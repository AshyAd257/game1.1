package com.Hecate.puppet.pixel;

import com.Hecate.puppet.volume.*;
import com.google.gson.Gson;
import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class PixelVerticalSamplingTest {
    static VolumeDefinition definition(){var d=PixelPhotoAndRingTest.imported(new com.jme3.scene.shape.Box(1,1,1));d.getBakedPixels().setVerticalTextureEnabled(true);d.getBakedPixels().setVerticalFrameHeight(50);d.getStrip3D().setImportanceEnabled(false);d.getStrip3D().setOutlineEnabled(false);for(var s:Strip3DDefinition.Surface.values())d.getStrip3D().layer(s).setColorPath("strip.png");return d;}
    static BufferedImage[] images(){var image=new BufferedImage(37,150,BufferedImage.TYPE_INT_ARGB);for(int y=0;y<150;y++)for(int x=0;x<37;x++)image.setRGB(x,y,0xff000000|y<<16|x<<8|40);return new BufferedImage[]{image,image,image};}
    @Test void halfAndFullTurnsInElevationMoveHalfAndFullFrameHeightsSymmetrically(){var d=definition();var a=PixelAtlasBaker.bake(d);try{
        for(int pitch:new int[]{-90,-45,0,45,90})assertEquals(pitch*50/90,PixelVerticalSampling.shift(a,d,pitch));
        d.getBakedPixels().setVerticalFrameHeight(53);assertEquals(27,PixelVerticalSampling.shift(a,d,45));assertEquals(-27,PixelVerticalSampling.shift(a,d,-45));
    }finally{a.dispose();}}
    @Test void sourceRowsSlideFromOneFlatAnchorAtEveryWholePixelStep(){var d=definition();var a=PixelAtlasBaker.bake(d);try{
        var sources=images();for(int pitch=-90;pitch<=90;pitch++){
            var choice=PixelViewSelector.select(d.getBakedPixels(),0,pitch,a.size());var rows=PixelVerticalSampling.build(a,d,choice,pitch,sources);
            for(int y=0;y<a.size();y++)for(int layer=0;layer<3;layer++){
                int raw=y-a.sampleCentreY(choice.viewForRow(y))+75-PixelVerticalSampling.shift(a,d,pitch);
                assertEquals(raw<0||raw>=150?-1:raw,rows.sourceY(layer,y));
            }
        }
    }finally{a.dispose();}}
    @Test void importanceThreeSurvivesWhenTheSlidingWindowWouldAlreadyHaveDroppedIt(){var d=definition();var a=PixelAtlasBaker.bake(d);try{
        var sources=images();var choice=PixelViewSelector.select(d.getBakedPixels(),0,45,a.size());var natural=PixelVerticalSampling.build(a,d,choice,45,sources);
        boolean originallyVisible=false;for(int y=0;y<a.size();y++)if((a.rowLayers(choice.viewForRow(y),y)&2)!=0&&natural.sourceY(1,y)==95)originallyVisible=true;assertFalse(originallyVisible);
        d.getStrip3D().setImportanceEnabled(true);d.getBakedPixels().setRowImportance(1,95,150,3);for(int y=50;y<100;y++)if(y!=95)d.getBakedPixels().setRowImportance(1,y,150,1);
        var preferred=PixelVerticalSampling.build(a,d,choice,45,sources);boolean retained=false;
        for(int y=0;y<a.size();y++)if((a.rowLayers(choice.viewForRow(y),y)&2)!=0&&preferred.sourceY(1,y)==95)retained=true;
        assertTrue(retained);String key=PixelAtlasBaker.key(d);d.getBakedPixels().setRowImportance(1,95,150,2);assertEquals(key,PixelAtlasBaker.key(d));
    }finally{a.dispose();}}
    @Test void prioritySelectionNeverReordersRowsOrChangesTheCachedSilhouetteOrDepth(){var d=definition();var a=PixelAtlasBaker.bake(d);try{
        d.getStrip3D().setImportanceEnabled(true);String key=a.key();long bytes=a.bytes();float z=a.depth(20,a.size()/2,a.size()/2);
        for(int row=0;row<150;row++)for(int layer=0;layer<3;layer++)d.getBakedPixels().setRowImportance(layer,row,150,row%3+1);
        for(int pitch=-90;pitch<=90;pitch++){
            var choice=PixelViewSelector.select(d.getBakedPixels(),0,pitch,a.size());var selected=PixelVerticalSampling.build(a,d,choice,pitch,images());
            for(int layer=0;layer<3;layer++){int previous=-1;for(int y=0;y<a.size();y++)if((a.rowLayers(choice.viewForRow(y),y)&1<<layer)!=0){int value=selected.sourceY(layer,y);if(value<0)continue;assertTrue(value>=previous);previous=value;}}
        }
        assertEquals(key,PixelAtlasBaker.key(d));assertEquals(bytes,a.bytes());assertEquals(z,a.depth(20,a.size()/2,a.size()/2));
    }finally{a.dispose();}}
    @Test void settingsAndActualSourceRowNumbersRemainIsolatedAndSaveWithoutChangingCaptureKey(){var d=definition();String key=PixelAtlasBaker.key(d);d.getBakedPixels().setRowImportance(1,95,150,3);
        var copy=d.copy();copy.getBakedPixels().setRowImportance(1,95,150,1);assertEquals(3,d.getBakedPixels().rowImportance(1,95));
        var saved=new Gson().fromJson(new Gson().toJson(d),VolumeDefinition.class);assertTrue(saved.getBakedPixels().isVerticalTextureEnabled());assertEquals(50,saved.getBakedPixels().getVerticalFrameHeight());assertEquals(3,saved.getBakedPixels().rowImportance(1,95));assertEquals(key,PixelAtlasBaker.key(saved));
    }
}
