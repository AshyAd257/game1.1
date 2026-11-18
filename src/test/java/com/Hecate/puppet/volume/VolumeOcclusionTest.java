package com.Hecate.puppet.volume;
import java.util.*;
import com.jme3.math.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class VolumeOcclusionTest {
    @Test void containmentUsesActualShapesAndTransformsNotBoneParentage(){
        var outer=new VolumeDefinition();outer.setDimensions(60,120,60);
        var inner=new VolumeDefinition();inner.setDimensions(32,32,32);
        var op=VolumeContainment.Proxy.of(outer,new Transform(new Vector3f(0,-1,.15625f)));
        var ip=VolumeContainment.Proxy.of(inner,new Transform(new Vector3f(-.0625f,0,.03125f)));
        assertTrue(VolumeContainment.contains(op,ip));assertFalse(VolumeContainment.contains(ip,op));assertFalse(VolumeContainment.contains(op,op));
        assertFalse(VolumeContainment.contains(op,VolumeContainment.Proxy.of(inner,new Transform(new Vector3f(.7f,0,0)))));
        var common=new Transform(new Vector3f(3,2,-1),new Quaternion().fromAngles(.5f,.8f,.2f),new Vector3f(-1.2f,.8f,1.4f));
        assertTrue(VolumeContainment.contains(VolumeContainment.Proxy.of(outer,new Transform(new Vector3f(0,-1,.15625f)).combineWithParent(common)),VolumeContainment.Proxy.of(inner,new Transform(new Vector3f(-.0625f,0,.03125f)).combineWithParent(common))));
        outer.setShape(VolumeDefinition.Shape.BOX);assertTrue(VolumeContainment.contains(VolumeContainment.Proxy.of(outer,new Transform()),VolumeContainment.Proxy.of(inner,new Transform())));
        outer.setShape(VolumeDefinition.Shape.CONE);assertFalse(VolumeContainment.contains(VolumeContainment.Proxy.of(outer,new Transform()),VolumeContainment.Proxy.of(inner,new Transform(new Vector3f(0,1,0)))));
    }
    @Test void outerSelectionClipsOnlyContainedVisiblePartsAndClearsWhenMovedOrHidden(){
        var app=new VolumeFeatureTest.TestApp();var cam=app.getCamera();cam.setParallelProjection(true);cam.setFrustum(.1f,100,-2,2,2,-2);cam.lookAt(Vector3f.ZERO,Vector3f.UNIT_Y);
        var parent=new com.jme3.scene.Node();var outer=new Strip3DView(app.getAssetManager(),parent,"outer");var inner=new Strip3DView(app.getAssetManager(),parent,"inner");
        var a=new VolumeDefinition();a.setDimensions(64,64,64);a.getStrip3D().setEnabled(true);a.getStrip3D().setFrameSize(8,8);
        var b=new VolumeDefinition();b.setDimensions(32,32,32);b.getStrip3D().setEnabled(true);
        boolean[] mask=new boolean[64];for(int y=0;y<8;y++)for(int x=4;x<8;x++)mask[y*8+x]=true;a.getStrip3D().setSelectionMask(8,8,mask);
        var entries=List.of(new VolumeOcclusion.Entry("outer",a,outer),new VolumeOcclusion.Entry("inner",b,inner));var resolver=new VolumeOcclusion();
        outer.update(a,Vector3f.ZERO,Quaternion.IDENTITY,Vector3f.UNIT_XYZ,cam,true,false);inner.update(b,Vector3f.ZERO,Quaternion.IDENTITY,Vector3f.UNIT_XYZ,cam,true,false);resolver.apply(entries,cam);
        assertFalse(inner.allowsSelection(-.1f,0));assertTrue(inner.allowsSelection(.1f,0));
        var cached=inner.getGeometry().getMesh();resolver.apply(entries,cam);assertSame(cached,inner.getGeometry().getMesh());
        outer.setVisible(false);resolver.apply(entries,cam);assertTrue(inner.allowsSelection(-.1f,0));assertNotSame(cached,inner.getGeometry().getMesh());
        outer.update(a,Vector3f.ZERO,Quaternion.IDENTITY,Vector3f.UNIT_XYZ,cam,true,false);inner.update(b,new Vector3f(1,0,0),Quaternion.IDENTITY,Vector3f.UNIT_XYZ,cam,true,false);resolver.apply(entries,cam);assertTrue(inner.allowsSelection(-.1f,0));
        inner.update(b,Vector3f.ZERO,Quaternion.IDENTITY,Vector3f.UNIT_XYZ,cam,true,false);resolver.apply(entries,cam);assertFalse(inner.allowsSelection(-.1f,0));
        a.getStrip3D().resetSelectionMask();outer.update(a,Vector3f.ZERO,Quaternion.IDENTITY,Vector3f.UNIT_XYZ,cam,true,false);resolver.apply(entries,cam);assertTrue(inner.allowsSelection(-.1f,0));outer.cleanup();inner.cleanup();
    }
    @Test void clippedMeshKeepsScreenCellsWholeIncludingRotatedMasksAndHoles(){
        var area=new java.awt.geom.Area(new java.awt.geom.Rectangle2D.Double(-.7,-.8,1.4,1.6));
        area.subtract(new java.awt.geom.Area(new java.awt.geom.Rectangle2D.Double(-.2,-.3,.4,.6)));
        area.transform(java.awt.geom.AffineTransform.getRotateInstance(.3));
        var bounds=new Vector4f(-1,-1,1,1);int size=64,step=2;
        var mesh=VolumeSelectionClip.mesh(area,bounds,size,size,step,null);var uv=mesh.getFloatBuffer(com.jme3.scene.VertexBuffer.Type.TexCoord);var raster=new boolean[size/step][size/step];
        for(int i=0;i<mesh.getVertexCount()/4;i++){
            int l=Math.round(uv.get(i*8)*size/step),b=Math.round(uv.get(i*8+1)*size/step),r=Math.round(uv.get(i*8+4)*size/step),t=Math.round(uv.get(i*8+5)*size/step);
            for(int k=0;k<8;k++)assertEquals(Math.rint(uv.get(i*8+k)*size/step),uv.get(i*8+k)*size/step,1e-5);
            for(int y=b;y<t;y++)for(int x=l;x<r;x++)raster[y][x]=true;
        }
        for(int y=0;y<size/step;y++)for(int x=0;x<size/step;x++)assertEquals(area.contains((x+.5)*step/size*2-1,(y+.5)*step/size*2-1),raster[y][x]);
    }
    @Test void resolvesNestedCoversWithoutAffectingUnrelatedParts(){
        var map=new LinkedHashMap<String,String>();map.put("护甲","衣服");map.put("衣服","皮肤");map.put("皮肤",null);map.put("手",null);
        var r=VolumeOcclusion.resolve(map);assertEquals(List.of("衣服","护甲"),r.get("皮肤"));assertEquals(List.of("护甲"),r.get("衣服"));assertTrue(r.get("手").isEmpty());
        map.remove("衣服");assertTrue(VolumeOcclusion.resolve(map).get("皮肤").isEmpty());
    }
    @Test void rejectsCyclesAndOversizedChainsOrBranches(){
        assertThrows(IllegalArgumentException.class,()->VolumeOcclusion.resolve(Map.of("A","A")));
        assertThrows(IllegalArgumentException.class,()->VolumeOcclusion.resolve(Map.of("A","B","B","C","C","A")));
        var map=new LinkedHashMap<String,String>();map.put("0",null);for(int i=1;i<=8;i++)map.put(""+i,""+(i-1));assertEquals(8,VolumeOcclusion.resolve(map).get("0").size());
        map.put("9","8");assertThrows(IllegalArgumentException.class,()->VolumeOcclusion.resolve(map));
        for(int i=1;i<=9;i++)map.put(""+i,"0");assertThrows(IllegalArgumentException.class,()->VolumeOcclusion.resolve(map));
    }
    @Test void editorAndRuntimeRenderersBothBindCoverTextures(){
        var app=new VolumeFeatureTest.TestApp();app.getCamera().setFrustum(.1f,100,-2,2,2,-2);
        var outer=new VolumeDefinition();outer.setSurfaceDepthEnabled(false);outer.getStrip3D().setEnabled(true);outer.setCoverTarget("inner");var inner=new VolumeDefinition();inner.setSurfaceDepthEnabled(false);inner.getStrip3D().setEnabled(true);
        var editorSkeleton=new com.Hecate.puppet.editor.core.EditorSkeleton("editor");var a=new com.Hecate.puppet.editor.core.EditorBone("outer");var b=new com.Hecate.puppet.editor.core.EditorBone("inner");a.addChild(b);a.setVolumeDefinition(outer);b.setVolumeDefinition(inner);editorSkeleton.setRootBone(a);
        var editor=new com.Hecate.puppet.editor.core.EditorPuppetRenderer(app,editorSkeleton);editor.initialize();editor.update(0);
        assertEquals(1,editor.getPartRenderer("inner").getStrip3DView().getGeometry().getMaterial().getParam("CoverCount").getValue());
        editor.getPartRenderer("outer").setVisible(false);editor.update(0);assertEquals(0,editor.getPartRenderer("inner").getStrip3DView().getGeometry().getMaterial().getParam("CoverCount").getValue());editor.cleanup();
        var runtimeSkeleton=new com.Hecate.puppet.core.Skeleton("runtime");var ra=new com.Hecate.puppet.core.Bone("outer");var rb=new com.Hecate.puppet.core.Bone("inner");ra.addChild(rb);ra.setVolumeDefinition(outer);rb.setVolumeDefinition(inner);runtimeSkeleton.setRootBone(ra);
        var runtime=new com.Hecate.puppet.core.PuppetRenderer(app,runtimeSkeleton);runtime.initialize();
        try{var field=com.jme3.app.LegacyApplication.class.getDeclaredField("timer");field.setAccessible(true);field.set(app,new com.jme3.system.NanoTimer());}catch(Exception e){throw new RuntimeException(e);}
        runtime.update(0);assertEquals(1,runtime.getPartRenderer("inner").getStrip3DView().getGeometry().getMaterial().getParam("CoverCount").getValue());
        ra.getVolumeDefinition().setCoverTarget(null);runtime.update(0);assertEquals(0,runtime.getPartRenderer("inner").getStrip3DView().getGeometry().getMaterial().getParam("CoverCount").getValue());runtime.cleanup();
    }
    @Test void definitionCopiesAndLegacyFilesPreserveTheirSemantics(){
        var d=new VolumeDefinition();d.setCoverTarget("头部");var copy=d.copy();assertEquals("头部",copy.getCoverTarget());copy.setCoverTarget(null);assertEquals("头部",d.getCoverTarget());
        var gson=new com.google.gson.Gson();assertEquals("头部",gson.fromJson(gson.toJson(d),VolumeDefinition.class).getCoverTarget());assertNull(gson.fromJson("{}",VolumeDefinition.class).getCoverTarget());
    }
}
