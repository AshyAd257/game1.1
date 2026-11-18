package com.Hecate.puppet.pixel;

import com.Hecate.puppet.volume.*;
import com.jme3.math.*;
import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class PixelPerspectiveTest {
    @Test void mildPerspectiveMakesTheNearMiddleOfACubeWiderThanItsFarEdges(){
        var d=PixelPhotoAndRingTest.imported(new com.jme3.scene.shape.Box(1,1,1));
        d.getBakedPixels().setPerspectiveStrength(.1f);var a=PixelAtlasBaker.bake(d);
        try{
            int first=a.size(),last=-1,max=0,edge=0;
            for(int y=0;y<a.size();y++){
                int count=0;for(int x=0;x<a.size();x++)if(a.occupied(40,x,y))count++;
                if(count>0){if(first==a.size()){first=y;edge=count;}last=y;max=Math.max(max,count);}
            }
            assertTrue(max>=edge+2,"The near seam must be visibly wider than the far upper edge");
            assertTrue(last>first);assertTrue(a.photoPerspective(40).w>0);
        }finally{a.dispose();}
    }
    @Test void perspectiveCorrectDepthReconstructsTheOriginalBoxSurfaceAtEveryCapturedPixel(){
        var d=PixelPhotoAndRingTest.imported(new com.jme3.scene.shape.Box(.5f,.8f,.2f));
        d.getBakedPixels().setPerspectiveStrength(.2f);var a=PixelAtlasBaker.bake(d);
        try{
            for(int view=0;view<PixelAtlas.VIEWS;view++)for(int y=0;y<a.size();y++)for(int x=0;x<a.size();x++)if(a.occupied(view,x,y)){
                var p=a.point(view,x,y);assertTrue(Math.abs(p.x)<=.50002&&Math.abs(p.y)<=.80002&&Math.abs(p.z)<=.20002,"Reconstructed point left original geometry: "+p);
                float surface=Math.min(Math.abs(Math.abs(p.x)-.5f),Math.min(Math.abs(Math.abs(p.y)-.8f),Math.abs(Math.abs(p.z)-.2f)));
                assertEquals(0,surface,.00002f,"Point must remain on a real triangle surface");
            }
        }finally{a.dispose();}
    }
    @Test void aOneMetreFrontSquareOnTheReferencePlaneStillHasTwentyFivePixels(){
        var d=PixelPhotoAndRingTest.imported(PixelPhotoAndRingTest.plane(1,1));d.getBakedPixels().setPerspectiveStrength(.1f);var a=PixelAtlasBaker.bake(d);
        try{assertEquals(25,PixelPhotoAndRingTest.width(a,20));assertEquals(25,PixelFiveDirectionsTest.height(a,20));}finally{a.dispose();}
    }
    @Test void projectionSettingsSaveAndOldMasksBecomeStaleWhenPerspectiveChanges(){
        var d=new VolumeDefinition();d.getBakedPixels().setEnabled(true);d.getBakedPixels().setPerspectiveStrength(0);
        String old=PixelAtlasBaker.key(d),v4=old;
        d.getBakedPixels().setEdit(20,new BakedPixelDefinition.MaskEdit(1,new boolean[]{true},v4));assertEquals(0,d.getBakedPixels().staleEdits(old));
        d.getBakedPixels().setPerspectiveStrength(.1f);assertNotEquals(old,PixelAtlasBaker.key(d));assertEquals(1,d.getBakedPixels().staleEdits(PixelAtlasBaker.key(d)));
        var saved=new Gson().fromJson(new Gson().toJson(d),VolumeDefinition.class);assertEquals(.1f,saved.getBakedPixels().getPerspectiveStrength());assertEquals(.1f,d.copy().getBakedPixels().getPerspectiveStrength());
        assertThrows(IllegalArgumentException.class,()->d.getBakedPixels().setPerspectiveStrength(.21f));
    }
    @Test void offCentreBlenderJointRetainsItsOriginalSurfacePointsUnderPerspective(){
        var node=new com.jme3.scene.Node("Arm");var geometry=new com.jme3.scene.Geometry("mesh",new com.jme3.scene.shape.Box(.2f,.6f,.2f));geometry.setLocalTranslation(0,-.6f,0);node.attachChild(geometry);
        var d=BlenderPixelImporter.fromScene(node).get(0).definition();var a=PixelAtlasBaker.bake(d);
        try{for(int view=0;view<PixelAtlas.VIEWS;view++)for(int y=0;y<a.size();y++)for(int x=0;x<a.size();x++)if(a.occupied(view,x,y)){var p=a.point(view,x,y);assertTrue(p.y>=-1.20002&&p.y<=.00002&&Math.abs(p.x)<=.20002&&Math.abs(p.z)<=.20002);}}finally{a.dispose();}
    }
}
