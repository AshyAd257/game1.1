package com.Hecate.puppet.volume;

import com.jme3.math.*;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;
import com.jme3.asset.DesktopAssetManager;
import com.jme3.collision.CollisionResults;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;

class SurfaceProjectionTest {
    @TempDir Path temp;
    @Test void perspectivePartsShareViewingDirectionAndPanningDoesNotSpinArtwork()throws Exception{
        var art=new BufferedImage(32,8,BufferedImage.TYPE_INT_ARGB);for(int y=0;y<8;y++)for(int x=0;x<32;x++)art.setRGB(x,y,0xff000000|x*7<<16|y*25<<8|53);
        var path=temp.resolve("direction.png");ImageIO.write(art,"png",path.toFile());var d=new VolumeDefinition();d.getStrip3D().setEnabled(true);d.getStrip3D().setFrameSize(8,8);d.getStrip3D().setOutlineEnabled(false);
        for(var face:Strip3DDefinition.Surface.values())d.getStrip3D().layer(face).setColorPath(path.toString());
        var cam=camera(new Vector3f(1,2,6),Vector3f.UNIT_Y);cam.setParallelProjection(false);cam.setFrustumPerspective(45,1,.1f,100);
        var parent=new Node();var assets=new DesktopAssetManager(true);var a=new Strip3DView(assets,parent,"left");var b=new Strip3DView(assets,parent,"right");
        a.update(d,new Vector3f(-3,-1,0),Quaternion.IDENTITY,Vector3f.UNIT_XYZ,cam,true,false);b.update(d,new Vector3f(3,2,1),Quaternion.IDENTITY,Vector3f.UNIT_XYZ,cam,true,false);
        byte[] original=uploadedPixels(a);assertArrayEquals(original,uploadedPixels(b),"Position alone must not change a part's sampled facing");
        cam.setLocation(cam.getLocation().add(2,1,0));a.update(d,new Vector3f(-3,-1,0),Quaternion.IDENTITY,Vector3f.UNIT_XYZ,cam,true,false);assertArrayEquals(original,uploadedPixels(a),"Panning keeps the same viewing direction");
        b.update(d,new Vector3f(3,2,1),new Quaternion().fromAngleAxis(FastMath.HALF_PI,Vector3f.UNIT_Y),Vector3f.UNIT_XYZ,cam,true,false);assertFalse(java.util.Arrays.equals(original,uploadedPixels(b)),"Real bone rotation still changes its facing");a.cleanup();b.cleanup();
    }
    private byte[] uploadedPixels(Strip3DView view){
        var t=(com.jme3.texture.Texture)view.getGeometry().getMaterial().getParam("ColorMap").getValue();var data=t.getImage().getData(0).duplicate();data.rewind();byte[] pixels=new byte[data.remaining()];data.get(pixels);return pixels;
    }
    @Test void roundedSilhouetteKeepsWholeCellsAndPreservesInteriorSurfaceDepth(){
        var d=new VolumeDefinition();d.setDimensions(64,64,64);d.getStrip3D().setFrameSize(12,12);
        var cam=camera(new Vector3f(.7f,3f,5),Vector3f.UNIT_Y);var p=projection(d,cam);
        var layout=TriStripSampler.layout(d.getStrip3D(),0,0);int width=layout.canvasWidth(),height=layout.canvasHeight();
        var toScreen=p.screenToArt().invert();int extended=0,trimmed=0,interior=0;
        for(int y=0;y<height;y++)for(int x=0;x<width;x++){
            var centre=toScreen.mult(new Vector3f((x+.5f)/width,(y+.5f)/height,1));
            boolean keep=SurfaceProjection.hit(p,d,centre.x,centre.y)!=null;
            for(float fy:new float[]{.08f,.5f,.92f})for(float fx:new float[]{.08f,.5f,.92f}){
                var point=toScreen.mult(new Vector3f((x+fx)/width,(y+fy)/height,1));
                var rounded=SurfaceProjection.hitArtwork(p,d,point.x,point.y,width,height);
                var physical=SurfaceProjection.hit(p,d,point.x,point.y);
                assertEquals(keep,rounded!=null,"A source texel must have one silhouette decision");
                if(rounded!=null&&physical==null)extended++;
                if(rounded==null&&physical!=null)trimmed++;
                if(rounded!=null&&physical!=null){assertEquals(physical.depth(),rounded.depth(),1e-7);interior++;}
            }
        }
        assertTrue(extended>0,"Expected rounded-out cells");assertTrue(trimmed>0,"Expected rounded-in cells");assertTrue(interior>100,"Expected interior depth comparisons");
    }
    @Test void contourRoundingDefaultsOnAndCopiesAndSavesBothStates(){
        var gson=new com.google.gson.Gson();var d=gson.fromJson("{}",VolumeDefinition.class);
        assertTrue(d.isSurfaceContourSnapped());assertTrue(d.copy().isSurfaceContourSnapped());
        d.setSurfaceContourSnapped(false);assertFalse(d.copy().isSurfaceContourSnapped());
        assertFalse(gson.fromJson(gson.toJson(d),VolumeDefinition.class).isSurfaceContourSnapped());
    }
    private Camera camera(Vector3f eye,Vector3f up){
        var c=new Camera(720,720);c.setParallelProjection(true);c.setFrustum(.1f,100,-3,3,3,-3);c.setLocation(eye);c.lookAt(Vector3f.ZERO,up);return c;
    }
    private SurfaceProjection.Result projection(VolumeDefinition d,Camera c){return SurfaceProjection.create(d,new Transform(),c,c.getLeft().negate(),TriStripSampler.layout(d.getStrip3D(),0,0));}
    @Test void nearPlaneInsideVolumeUsesCutFaceInsteadOfBackSurface(){
        for(var shape:VolumeDefinition.Shape.values())for(boolean parallel:new boolean[]{true,false}){
            var d=new VolumeDefinition();d.setDimensions(64,64,64);d.setShape(shape);d.setTopRatio(.6f);
            var c=camera(new Vector3f(0,0,.3f),Vector3f.UNIT_Y);
            if(!parallel){c.setParallelProjection(false);c.setFrustumPerspective(45,1,.1f,1000);}
            var hit=SurfaceProjection.hit(projection(d,c),d,0,0);
            assertNotNull(hit,shape+" must keep its closed cut face");
            assertEquals(0,hit.depth(),1e-7);assertEquals(.2f,hit.proxyPoint().z,1e-4);
        }
    }
    @Test void nearPlaneCutDoesNotExposeContainedFaceAtTiltedCamera(){
        var inner=new VolumeDefinition();inner.setDimensions(32,32,32);
        var outer=new VolumeDefinition();outer.setDimensions(60,120,60);
        float pitch=20*FastMath.DEG_TO_RAD;
        var c=camera(new Vector3f(0,FastMath.sin(pitch)*1.5f-.4f,FastMath.cos(pitch)*1.5f),Vector3f.UNIT_Y);
        c.lookAt(new Vector3f(0,-.4f,0),Vector3f.UNIT_Y);c.setFrustum(.1f,1000,-1.8f,1.8f,1.8f,-1.8f);
        var innerProjection=SurfaceProjection.create(inner,new Transform(new Vector3f(-.0625f,0,.03125f)),c,c.getLeft().negate(),TriStripSampler.layout(inner.getStrip3D(),0,20));
        var outerProjection=SurfaceProjection.create(outer,new Transform(new Vector3f(0,-1,.15625f)),c,c.getLeft().negate(),TriStripSampler.layout(outer.getStrip3D(),0,20));
        float x=341f/720*2-1,y=493f/720*2-1;
        var a=SurfaceProjection.hit(innerProjection,inner,x,y);var b=SurfaceProjection.hit(outerProjection,outer,x,y);
        assertNotNull(a);assertNotNull(b);assertEquals(0,b.depth());assertTrue(b.depth()<a.depth(),"Contained face must stay behind the clipped outer shell");
    }
    @Test void frontAndTopExtentsUsePhysicalWidthHeightAndDepth(){
        var d=new VolumeDefinition();d.setDimensions(64,96,32);
        var front=projection(d,camera(new Vector3f(0,0,5),Vector3f.UNIT_Y)).bounds();
        assertEquals(2f/3,front.z-front.x,1e-5);assertEquals(1,front.w-front.y,1e-5);
        var top=projection(d,camera(new Vector3f(0,5,0),Vector3f.UNIT_Z)).bounds();
        assertEquals(2f/3,top.z-top.x,1e-5);assertEquals(1f/3,top.w-top.y,1e-5);
        d.setDimensions(64,96,64);
        var angle=projection(d,camera(new Vector3f(5,0,5),Vector3f.UNIT_Y)).bounds();
        assertEquals(2f/3,angle.z-angle.x,1e-5,"Cylinder must not gain box-corner width at 45 degrees");
    }
    @Test void raysHitClosedSidesCapsConesAndBoxes(){
        var d=new VolumeDefinition();d.setDimensions(64,64,64);var cam=camera(new Vector3f(0,0,5),Vector3f.UNIT_Y);
        var p=projection(d,cam);assertEquals(1,SurfaceProjection.hit(p,d,0,0).proxyPoint().z,1e-4);
        assertEquals(Math.sqrt(.75),SurfaceProjection.hit(p,d,1f/6,0).proxyPoint().z,1e-4);
        assertNull(SurfaceProjection.hit(p,d,.5f,0));
        assertEquals(1,SurfaceProjection.hit(projection(d,camera(new Vector3f(0,5,0),Vector3f.UNIT_Z)),d,0,0).proxyPoint().y,1e-4);
        assertEquals(-1,SurfaceProjection.hit(projection(d,camera(new Vector3f(0,-5,0),Vector3f.UNIT_Z)),d,0,0).proxyPoint().y,1e-4);
        d.setShape(VolumeDefinition.Shape.CONE);assertEquals(.5,SurfaceProjection.hit(projection(d,cam),d,0,0).proxyPoint().z,1e-4);
        d.setShape(VolumeDefinition.Shape.FRUSTUM);d.setTopRatio(.5f);assertEquals(.75,SurfaceProjection.hit(projection(d,cam),d,0,0).proxyPoint().z,1e-4);
        d.setShape(VolumeDefinition.Shape.BOX);assertEquals(1,SurfaceProjection.hit(projection(d,cam),d,1f/6,0).proxyPoint().z,1e-4);
    }
    @Test void projectedArtKeepsAllRequestedColumnsAndRoundTripsTheMode(){
        var d=new VolumeDefinition();d.setDimensions(64,64,64);d.getStrip3D().setFrameSize(9,12);var c=camera(new Vector3f(0,0,5),Vector3f.UNIT_Y);
        for(int width:new int[]{9,11,13,16,36}){
            d.getStrip3D().setSampleWidth(width);var layout=TriStripSampler.layout(d.getStrip3D(),0,0);var p=projection(d,c);
            var left=p.screenToArt().mult(new Vector3f(p.bounds().x,0,1));var right=p.screenToArt().mult(new Vector3f(p.bounds().z,0,1));
            assertEquals(width,(right.x-left.x)*layout.canvasWidth(),1e-4);
        }
        var gson=new com.google.gson.Gson();assertTrue(gson.fromJson("{}",VolumeDefinition.class).isSurfaceDepthEnabled());
        d.setSurfaceDepthEnabled(false);assertFalse(d.copy().isSurfaceDepthEnabled());assertFalse(gson.fromJson(gson.toJson(d),VolumeDefinition.class).isSurfaceDepthEnabled());
    }
    @Test void pickingUsesTransformedVolumeAndPassesThroughSelectionHoles()throws Exception{
        var image=new BufferedImage(32,8,BufferedImage.TYPE_INT_ARGB);for(int y=0;y<8;y++)for(int x=0;x<32;x++)image.setRGB(x,y,0xffabcdef);
        var path=temp.resolve("art.png");ImageIO.write(image,"png",path.toFile());var d=new VolumeDefinition();d.setDimensions(64,64,64);var strip=d.getStrip3D();strip.setEnabled(true);strip.setFrameSize(8,8);strip.setOutlineEnabled(false);
        for(var face:Strip3DDefinition.Surface.values())strip.layer(face).setColorPath(path.toString());
        var parent=new Node();parent.setLocalTranslation(.3f,.2f,0);parent.updateGeometricState();var view=new Strip3DView(new DesktopAssetManager(true),parent,"pick");var cam=camera(new Vector3f(0,0,5),Vector3f.UNIT_Y);
        var position=new Vector3f(.7f,0,0);var rotation=new Quaternion().fromAngles(0,.3f,0);var scale=new Vector3f(-1,1,1);
        view.update(d,position,rotation,scale,cam,true,false);
        Vector3f centre=new Vector3f(1,.2f,0);Ray ray=new Ray(new Vector3f(centre.x,centre.y,4.9f),new Vector3f(0,0,-1));var results=new CollisionResults();
        assertEquals(1,view.getGeometry().collideWith(ray,results));assertEquals(1,results.getClosestCollision().getContactPoint().z,.01);
        view.setSelectionClips(java.util.List.of(new java.awt.geom.Area(new java.awt.geom.Rectangle2D.Double(-1,-1,1,2))),cam);
        assertEquals(0,view.getGeometry().collideWith(ray,new CollisionResults()),"Hidden inner pixels must not remain clickable");
        view.setSelectionClips(java.util.List.of(),cam);assertEquals(1,view.getGeometry().collideWith(ray,new CollisionResults()));
        boolean[] mask=new boolean[64];java.util.Arrays.fill(mask,true);for(int y=2;y<6;y++)for(int x=2;x<6;x++)mask[y*8+x]=false;strip.setSelectionMask(8,8,mask);
        view.update(d,position,rotation,scale,cam,true,false);assertEquals(0,view.getGeometry().collideWith(ray,new CollisionResults()));
        view.update(d,position,rotation,scale,cam,false,false);assertEquals(0,view.getGeometry().collideWith(ray,new CollisionResults()));view.cleanup();
    }
    @Test void editorAndGameUseSurfaceDepthWithoutManualRelations(){
        var app=new VolumeFeatureTest.TestApp();app.getCamera().setLocation(new Vector3f(0,0,5));app.getCamera().lookAt(Vector3f.ZERO,Vector3f.UNIT_Y);
        var skeleton=new com.Hecate.puppet.editor.core.EditorSkeleton("surface");var bone=new com.Hecate.puppet.editor.core.EditorBone("part");skeleton.setRootBone(bone);var d=new VolumeDefinition();d.getStrip3D().setEnabled(true);d.setCoverTarget("part");bone.setVolumeDefinition(d);
        var editor=new com.Hecate.puppet.editor.core.EditorPuppetRenderer(app,skeleton);editor.initialize();editor.update(0);var material=editor.getPartRenderer("part").getGeometry().getMaterial();
        assertEquals(true,material.getParam("SurfaceDepth").getValue());assertNotNull(material.getParam("ClipToProxy"));assertEquals(0,material.getParam("CoverCount").getValue());assertNull(editor.getVolumeOcclusionError());editor.cleanup();
        var gameSkeleton=new com.Hecate.puppet.core.Skeleton("game");var gameBone=new com.Hecate.puppet.core.Bone("part");gameBone.setVolumeDefinition(d);gameSkeleton.setRootBone(gameBone);var game=new com.Hecate.puppet.core.PuppetRenderer(app,gameSkeleton);game.initialize();
        try{var f=com.jme3.app.LegacyApplication.class.getDeclaredField("timer");f.setAccessible(true);f.set(app,new com.jme3.system.NanoTimer());}catch(Exception e){throw new RuntimeException(e);}
        game.update(0);assertEquals(true,game.getPartRenderer("part").getGeometry().getMaterial().getParam("SurfaceDepth").getValue());assertNull(game.getVolumeOcclusionError());game.cleanup();
    }
}
