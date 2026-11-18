package com.Hecate.puppet.pixel;

import com.Hecate.puppet.volume.*;
import com.jme3.math.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class PixelFiveDirectionsTest {
    @Test void exactCaptureAnglesSelectTheirOwnGroupIncludingBothPoles(){
        var d=new BakedPixelDefinition();
        for(int band=0;band<5;band++)for(int yaw=0;yaw<PixelAtlas.YAW_VIEWS;yaw++){
            var s=PixelViewSelector.select(d,yaw*PixelAtlas.YAW_DEGREES,d.elevation(band),64);
            for(int row=0;row<64;row++)assertEquals(band*PixelAtlas.YAW_VIEWS+yaw,s.viewForRow(row));
        }
        d.setElevations(-35,55);assertEquals(90,d.elevation(3));assertEquals(-90,d.elevation(4));
    }
    @Test void allFourTransitionsAdvanceOnlyWholeRowsInTheCorrectOrder(){
        var d=new BakedPixelDefinition();
        for(int i=0;i<4;i++){
            int low=BakedPixelDefinition.orderedBand(i),high=BakedPixelDefinition.orderedBand(i+1),previous=0;
            float mid=(d.elevation(low)+d.elevation(high))*.5f;
            for(int k=0;k<=64;k++){
                var s=PixelViewSelector.select(d,36,mid-4+k/8f,64);
                assertEquals(low,s.lowerBand());assertEquals(high,s.upperBand());assertEquals(k,s.revealedRows());
                assertTrue(s.revealedRows()>=previous);previous=s.revealedRows();
                for(int row=0;row<64;row++)assertEquals((row<k?high:low)*PixelAtlas.YAW_VIEWS+2,s.viewForRow(row));
            }
        }
    }
    @Test void truePolePhotosUseRealCapGeometryAndDepthInsteadOfTheSlantedPhoto(){
        var d=PixelPhotoAndRingTest.imported(new com.jme3.scene.shape.Box(.5f,.8f,.2f));
        var a=PixelAtlasBaker.bake(d);
        try{
            assertEquals(100,PixelAtlas.VIEWS);
            for(int band:new int[]{BakedPixelDefinition.TOP,BakedPixelDefinition.BOTTOM})for(int yaw=0;yaw<PixelAtlas.YAW_VIEWS;yaw++){
                int view=band*PixelAtlas.YAW_VIEWS+yaw,count=0;
                for(int y=0;y<a.size();y++)for(int x=0;x<a.size();x++)if(a.occupied(view,x,y)){
                    count++;assertEquals(band==3?1:3,a.layer(view,x,y));
                    assertEquals(band==3?.8f:-.8f,a.point(view,x,y).y,.00001f);
                }
                assertTrue(count>0);if(yaw==0)assertEquals(250,count);
            }
            assertEquals(10,height(a,60));assertEquals(35,height(a,40));
        }finally{a.dispose();}
    }
    static int height(PixelAtlas a,int view){int first=a.size(),last=-1;for(int y=0;y<a.size();y++)for(int x=0;x<a.size();x++)if(a.occupied(view,x,y)){first=Math.min(first,y);last=Math.max(last,y);}return last-first+1;}
    @Test void finiteBillboardsStopTurningInTheTransitionAndMeetAtOneSharedRow(){
        var d=new BakedPixelDefinition();
        for(int i=0;i<4;i++){
            int low=BakedPixelDefinition.orderedBand(i),high=BakedPixelDefinition.orderedBand(i+1);
            float mid=(d.elevation(low)+d.elevation(high))*.5f;
            for(int k=0;k<=64;k++){
                float pitch=mid-4+k/8f;var s=PixelViewSelector.select(d,15,pitch,64);
                var b=PixelBillboardLayout.create(d,15,pitch,s,25);float cut=(32-k)/25f;
                assertEquals(mid-4,b.lowerPitch(),.00001f);assertEquals(mid+4,b.upperPitch(),.00001f);
                assertEquals(0,b.lowerCentre().add(b.lowerUp().mult(cut)).distance(b.upperCentre().add(b.upperUp().mult(cut))),.00001f);
                if(k==0)assertEquals(0,b.lowerCentre().length(),.00001f);
                if(k==64)assertEquals(0,b.upperCentre().length(),.00001f);
            }
        }
        var s=PixelViewSelector.select(d,0,10,64);var b=PixelBillboardLayout.create(d,0,10,s,25);assertEquals(10,b.lowerPitch());
    }
    @Test void poleYawUsesCameraRightSoTopAndBottomDoNotFlipTextureByHalfATurn(){
        for(int step=0;step<PixelAtlas.YAW_VIEWS;step++)for(int pitch:new int[]{-90,90}){
            float yaw=PixelViewSelector.localYaw(PixelViewSelector.direction(step*PixelAtlas.YAW_DEGREES,pitch),PixelViewSelector.right(step*PixelAtlas.YAW_DEGREES));
            assertEquals(step,PixelViewSelector.select(new BakedPixelDefinition(),yaw,pitch,64).yawStep());
        }
    }
    @Test void oldTwentyFourDirectionEditsAreRetainedSeparatelyAndDoNotOccupyNewDirections(){
        var d=new VolumeDefinition();d.setDimensions(25,40,10);d.getBakedPixels().setEnabled(true);
        String key=PixelAtlasBaker.key(d),old=key.replace("pixel-v6/","pixel-v5/");
        var gson=new com.google.gson.Gson();var json=gson.toJsonTree(d).getAsJsonObject();
        var saved=new com.google.gson.JsonObject();saved.add("119",gson.toJsonTree(new BakedPixelDefinition.MaskEdit(1,new boolean[]{true},old)));
        saved.add("48",gson.toJsonTree(new BakedPixelDefinition.MaskEdit(1,new boolean[]{true},old)));
        json.getAsJsonObject("bakedPixels").add("viewEdits",saved);
        d=gson.fromJson(json,VolumeDefinition.class);assertNull(d.getBakedPixels().getEdit(48));assertEquals(2,d.getBakedPixels().staleEdits(key));
        var restored=gson.fromJson(gson.toJson(d.copy()),VolumeDefinition.class);
        assertTrue(restored.getBakedPixels().getLegacyEdit(119).mask()[0]);assertNotNull(restored.getBakedPixels().getLegacyEdit(48));
        restored.getBakedPixels().setEdit(99,new BakedPixelDefinition.MaskEdit(1,new boolean[]{true},key));
        assertNotNull(restored.copy().getBakedPixels().getEdit(99));assertNotNull(restored.getBakedPixels().getLegacyEdit(119));
        assertThrows(IllegalArgumentException.class,()->restored.getBakedPixels().setEdit(100,new BakedPixelDefinition.MaskEdit(1,new boolean[]{true},key)));
    }
}
