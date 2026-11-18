package com.Hecate.puppet.pixel;

import com.Hecate.puppet.volume.*;
import com.jme3.math.*;
import com.jme3.scene.*;
import org.junit.jupiter.api.Test;
import java.awt.image.BufferedImage;
import static org.junit.jupiter.api.Assertions.*;

final class PixelPhotoAndRingTest {
    static Mesh plane(float width,float height){Mesh mesh=new Mesh();mesh.setBuffer(VertexBuffer.Type.Position,3,new float[]{-width/2,-height/2,0,width/2,-height/2,0,width/2,height/2,0,-width/2,height/2,0});mesh.setBuffer(VertexBuffer.Type.Index,3,new int[]{0,1,2,0,2,3});mesh.updateBound();return mesh;}
    static VolumeDefinition imported(Mesh mesh){Node n=new Node("Irregular");n.attachChild(new Geometry("Mesh",mesh));var d=BlenderPixelImporter.fromScene(n).get(0).definition();d.getBakedPixels().setPerspectiveStrength(0);return d;}
    static int width(PixelAtlas a,int view){int lo=a.size(),hi=-1;for(int y=0;y<a.size();y++)for(int x=0;x<a.size();x++)if(a.occupied(view,x,y)){lo=Math.min(lo,x);hi=Math.max(hi,x);}return hi-lo+1;}
    @Test void photosRoundProjectedSizesWithoutModifyingOriginalVertices(){for(float w:new float[]{.9696f,.9804f,.992f,1.012f,1.024f}){var d=imported(plane(w,.996f));var mesh=VolumeMeshFactory.create(d);float[] before=new float[mesh.getVertexCount()*3];for(int i=0;i<before.length;i++)before[i]=mesh.getFloatBuffer(VertexBuffer.Type.Position).get(i);var a=PixelAtlasBaker.bakeMesh(mesh,d);assertEquals(Math.round(w*25),width(a,20));int occupied=0;for(int y=0;y<a.size();y++)for(int x=0;x<a.size();x++)if(a.occupied(20,x,y)){occupied++;var p=a.point(20,x,y);assertTrue(Math.abs(p.x)<=w/2+.0001f);assertTrue(Math.abs(p.y)<=.498f+.0001f);}assertEquals(Math.round(w*25)*25,occupied);float[] after=new float[before.length];for(int i=0;i<after.length;i++)after[i]=mesh.getFloatBuffer(VertexBuffer.Type.Position).get(i);assertArrayEquals(before,after);a.dispose();}}
    @Test void eachAngleRoundsItsOwnPhotoExtent(){var d=imported(new com.jme3.scene.shape.Box(.5f,.5f,.2f));var a=PixelAtlasBaker.bake(d);assertEquals(25,width(a,20));assertEquals(26,width(a,22));assertEquals(10,width(a,25));a.dispose();}
    @Test void requiredRingWidthDoesNotResizeModelOrMask(){
        var d=imported(plane(.992f,.996f));d.getBakedPixels().setTextureOrigin(-30,0);
        String key=PixelAtlasBaker.key(d);int window=d.getStrip3D().getSampleWidth(),height=d.getStrip3D().getFrameHeight();
        var cache=PixelAtlasCache.SHARED;long builds=cache.builds();
        try(var lease=cache.acquire(d)){
            var r=PixelRingTexture.requirement(lease.atlas());assertEquals(25,r.silhouetteWidth());assertEquals(20,r.width());assertEquals(1,r.columnsPerStep());
            BufferedImage source=new BufferedImage(r.width(),13,BufferedImage.TYPE_INT_ARGB);
            for(int y=0;y<13;y++)for(int x=0;x<source.getWidth();x++)source.setRGB(x,y,0xff000000|x%256<<16|y);
            for(int width:new int[]{1,7,37,113,2051}){
                var wrong=new BufferedImage(width,13,BufferedImage.TYPE_INT_ARGB);
                PixelRingTexture.assign(d,Strip3DDefinition.Surface.SIDE,"free.png",wrong);
                assertEquals("free.png",d.getStrip3D().layer(Strip3DDefinition.Surface.SIDE).getColorPath());
            }
            PixelRingTexture.assign(d,Strip3DDefinition.Surface.SIDE,"ring.png",source);
            assertEquals(window,d.getStrip3D().getSampleWidth());assertEquals(height,d.getStrip3D().getFrameHeight());assertEquals(key,PixelAtlasBaker.key(d));
            try(var again=cache.acquire(d)){assertSame(lease.atlas(),again.atlas());}
            for(int view:new int[]{20,21,39}){
                var image=PixelArtwork.view(lease.atlas(),d,view,new BufferedImage[]{source,source,source});int size=lease.atlas().size();
                for(int y=0;y<size;y++)for(int x=0;x<size;x++)if(lease.atlas().occupied(view,x,y)){
                    int sx=PixelRingTexture.column(x-size/2-30+PixelRingTexture.originX(source.getWidth())+PixelRingTexture.phase(view%PixelAtlas.YAW_VIEWS,r.width()),r.width());
                    int sy=Math.floorMod(y-size/2+PixelRingTexture.originY(source.getHeight()),13);assertEquals(source.getRGB(sx,sy),image.getRGB(x,y));
                }
            }
            assertTrue(cache.builds()<=builds+1);
        }
    }
    @Test void entireRingClosesInTwentyWholePixelSteps(){
        for(int width:new int[]{20,40,100,200,24,288,37}){
            assertEquals(0,PixelRingTexture.phase(0,width));assertEquals(0,PixelRingTexture.phase(20,width));
            for(int i=0;i<20;i++){
                assertEquals(i*width/20,PixelRingTexture.phase(i,width));
                assertEquals(PixelRingTexture.phase(i,width),PixelRingTexture.phase(i-20,width));
                assertEquals(PixelRingTexture.phase(i,width),PixelRingTexture.phase(i+20,width));
            }
            if(width%20==0)for(int i=0;i<19;i++)assertEquals(width/20,PixelRingTexture.phase(i+1,width)-PixelRingTexture.phase(i,width));
        }
    }
    @Test void modelWidthDoesNotRestrictTextureRingLength(){
        var small=PixelAtlasBaker.bake(imported(new com.jme3.scene.shape.Box(.5f,.5f,.5f)));
        var large=PixelAtlasBaker.bake(imported(new com.jme3.scene.shape.Box(1,1,1)));
        var s=PixelRingTexture.requirement(small);var l=PixelRingTexture.requirement(large);
        assertEquals(s.width(),l.width());assertTrue(l.recommendedHeight()>s.recommendedHeight());
        for(var r:new PixelRingTexture.Requirement[]{s,l}){assertEquals(0,r.width()%20);assertEquals(20,r.width());assertEquals(r.width()/20,r.columnsPerStep());}
        assertEquals(20,l.width());assertEquals(1,l.columnsPerStep());small.dispose();large.dispose();
    }
    @Test void ringEndsConnectForBothDirectionsAndWindowCanBeWiderThanRing(){assertEquals(36,PixelRingTexture.column(-1,37));assertEquals(0,PixelRingTexture.column(37,37));assertEquals(1,PixelRingTexture.column(38,37));for(int x=-100;x<100;x++)assertEquals(PixelRingTexture.column(x,7),PixelRingTexture.column(x+7,7));}
    @Test void noTextureStillShowsEveryCapturedPixel(){var d=imported(plane(.992f,.996f));var a=PixelAtlasBaker.bake(d);var blank=PixelRingTexture.silhouette();var image=PixelArtwork.view(a,d,20,new BufferedImage[]{blank,blank,blank});int count=0;for(int y=0;y<a.size();y++)for(int x=0;x<a.size();x++)if((image.getRGB(x,y)>>>24)>0)count++;assertEquals(625,count);a.dispose();}
}
