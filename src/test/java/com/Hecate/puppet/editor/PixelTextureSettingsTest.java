package com.Hecate.puppet.editor;

import com.Hecate.puppet.pixel.*;
import com.Hecate.puppet.volume.*;
import com.jme3.asset.DesktopAssetManager;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import java.awt.event.MouseEvent;
import com.google.gson.Gson;
import javax.imageio.ImageIO;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

final class PixelTextureSettingsTest {
    @TempDir Path directory;
    VolumeDefinition definition(){var d=new VolumeDefinition();d.setShape(VolumeDefinition.Shape.BOX);d.setDimensions(50,50,50);d.setPixelsPerUnit(25);d.getStrip3D().setFrameSize(50,50);d.getStrip3D().setEnabled(true);d.getStrip3D().setOutlineEnabled(false);d.getBakedPixels().setEnabled(true);return d;}
    String png(int width,int height)throws Exception{
        BufferedImage image=new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<height;y++)for(int x=0;x<width;x++)image.setRGB(x,y,0xff000000|x%256<<16|y%256<<8|96);
        Path path=directory.resolve(width+"x"+height+".png");ImageIO.write(image,"png",path.toFile());return path.toString();
    }
    @Test void choosingTextureAppliesImmediatelyAndWindowCloseKeepsIt()throws Exception{
        var source=definition();try(var lease=PixelAtlasCache.SHARED.acquire(source)){
            String path=png(288,85);var applied=new ArrayList<VolumeDefinition>();
            SwingUtilities.invokeAndWait(()->{
                var editor=new PixelStripTextureDialog.Editor(source,lease.atlas(),new DesktopAssetManager(true),applied::add);
                assertTrue(editor.shared.isSelected());editor.choose(Strip3DDefinition.Surface.TOP,path);
                assertEquals(1,applied.size());assertNull(source.getStrip3D().layer(Strip3DDefinition.Surface.SIDE).getColorPath());
                var result=editor.finish(JOptionPane.CLOSED_OPTION);
                for(var surface:Strip3DDefinition.Surface.values())assertEquals(path,result.getStrip3D().layer(surface).getColorPath());
                assertTrue(editor.status.getText().contains("已应用到部件"));
            });
        }
    }
    @Test void wrongImageCannotReplaceTheLastValidAppliedTexture()throws Exception{
        var source=definition();try(var lease=PixelAtlasCache.SHARED.acquire(source)){
            String good=png(288,85),bad=directory.resolve("not-an-image.png").toString();var applied=new ArrayList<VolumeDefinition>();
            SwingUtilities.invokeAndWait(()->{
                var editor=new PixelStripTextureDialog.Editor(source,lease.atlas(),new DesktopAssetManager(true),applied::add);
                editor.choose(Strip3DDefinition.Surface.SIDE,good);editor.choose(Strip3DDefinition.Surface.SIDE,bad);
                assertEquals(1,applied.size());assertNotNull(editor.invalid);assertTrue(editor.status.getText().contains("尚未应用"));
                assertThrows(IllegalArgumentException.class,()->editor.finish(JOptionPane.OK_OPTION));
                assertEquals(good,editor.finish(JOptionPane.CLOSED_OPTION).getStrip3D().layer(Strip3DDefinition.Surface.SIDE).getColorPath());
            });
        }
    }
    @Test void cancellationIsExplicitAndIndividualSurfacesRemainAvailable()throws Exception{
        var source=definition();try(var lease=PixelAtlasCache.SHARED.acquire(source)){
            String path=png(288,85);SwingUtilities.invokeAndWait(()->{
                var editor=new PixelStripTextureDialog.Editor(source,lease.atlas(),new DesktopAssetManager(true),edit->{});
                editor.shared.setSelected(false);editor.choose(Strip3DDefinition.Surface.TOP,path);
                var applied=editor.lastApplied;assertEquals(path,applied.getStrip3D().layer(Strip3DDefinition.Surface.TOP).getColorPath());
                assertNull(applied.getStrip3D().layer(Strip3DDefinition.Surface.SIDE).getColorPath());
                assertNull(editor.finish(JOptionPane.CANCEL_OPTION));
            });
        }
    }
    @Test void offsetsAreAppliedAndCopiedOnFinish()throws Exception{
        var source=definition();try(var lease=PixelAtlasCache.SHARED.acquire(source)){
            String path=png(288,85);SwingUtilities.invokeAndWait(()->{
                var editor=new PixelStripTextureDialog.Editor(source,lease.atlas(),new DesktopAssetManager(true),edit->{});
                editor.choose(Strip3DDefinition.Surface.SIDE,path);editor.xs[1].setValue(7);editor.ys[1].setValue(-3);
                var result=editor.finish(JOptionPane.OK_OPTION);assertEquals(7,result.getStrip3D().layer(Strip3DDefinition.Surface.SIDE).getOffsetX());assertEquals(-3,result.getStrip3D().layer(Strip3DDefinition.Surface.SIDE).getOffsetY());
                editor.paths[1].setText("");assertEquals(path,result.getStrip3D().layer(Strip3DDefinition.Surface.SIDE).getColorPath());
            });
        }
    }
    @Test void samplingUsesActualImageCentreInsteadOfOldWindowDimensions()throws Exception{
        var d=definition();String path=png(288,85);var source=ImageIO.read(Path.of(path).toFile());
        try(var lease=PixelAtlasCache.SHARED.acquire(d)){
            int n=lease.atlas().size();for(int window:new int[]{10,50,200}){
                d.getStrip3D().setFrameSize(window,window);
                var result=PixelArtwork.view(lease.atlas(),d,20,new BufferedImage[]{source,source,source});
                assertEquals(source.getRGB(36,42),result.getRGB(lease.atlas().sampleCentreX(20),lease.atlas().sampleCentreY(20)));
                assertEquals(source.getRGB(50,42),PixelArtwork.view(lease.atlas(),d,21,new BufferedImage[]{source,source,source}).getRGB(lease.atlas().sampleCentreX(21),lease.atlas().sampleCentreY(21)));
            }
        }
    }
    @Test void pixelSourcesUseTheirOwnSizeLimits()throws Exception{
        String path=png(2304,600);assertEquals(2304,PixelRingTexture.readImage(new DesktopAssetManager(true),path).getWidth());
        assertThrows(java.io.IOException.class,()->Strip3DTextures.readImage(new DesktopAssetManager(true),path));
    }
    @Test void mouseEditedCentreAndWindowSurviveCopyAndSerializationWithoutRecapture()throws Exception{
        var source=definition();try(var lease=PixelAtlasCache.SHARED.acquire(source)){
            String path=png(288,85);long builds=PixelAtlasCache.SHARED.builds();String key=lease.atlas().key();
            SwingUtilities.invokeAndWait(()->{
                var editor=new PixelStripTextureDialog.Editor(source,lease.atlas(),new DesktopAssetManager(true),edit->{});editor.choose(Strip3DDefinition.Surface.SIDE,path);
                var canvas=editor.textureCanvas;canvas.editingCentre=true;
                mouse(canvas,MouseEvent.MOUSE_PRESSED,50,30);mouse(canvas,MouseEvent.MOUSE_RELEASED,50,30);
                assertEquals(14,editor.draft.getBakedPixels().getTextureOriginX());assertEquals(-12,editor.draft.getBakedPixels().getTextureOriginY());
                canvas.editingCentre=false;mouse(canvas,MouseEvent.MOUSE_PRESSED,50,30);mouse(canvas,MouseEvent.MOUSE_DRAGGED,57,35);mouse(canvas,MouseEvent.MOUSE_RELEASED,57,35);
                assertEquals(57,canvas.windowX());assertEquals(35,canvas.windowY());assertEquals(50,canvas.centreX());assertEquals(30,canvas.centreY());
                var result=editor.finish(JOptionPane.CLOSED_OPTION);var actual=source.copy();PuppetEditorApp.copyPixelTextureSettings(actual,result);
                actual=new Gson().fromJson(new Gson().toJson(actual),VolumeDefinition.class);
                assertEquals(14,actual.getBakedPixels().getTextureOriginX());assertEquals(-12,actual.getBakedPixels().getTextureOriginY());
                for(var surface:Strip3DDefinition.Surface.values()){assertEquals(7,actual.getStrip3D().layer(surface).getOffsetX());assertEquals(5,actual.getStrip3D().layer(surface).getOffsetY());}
                assertEquals(key,PixelAtlasBaker.key(actual));assertEquals(builds,PixelAtlasCache.SHARED.builds());
                var picture=PixelArtwork.view(lease.atlas(),actual,20,editor.images);assertEquals(editor.images[1].getRGB(57,35),picture.getRGB(lease.atlas().sampleCentreX(20),lease.atlas().sampleCentreY(20)));
            });
        }
    }
    @Test void textureOverlayShowsWrappedWindowAtBothRingEnds()throws Exception{
        var source=definition();try(var lease=PixelAtlasCache.SHARED.acquire(source)){
            String path=png(288,85);SwingUtilities.invokeAndWait(()->{
                var editor=new PixelStripTextureDialog.Editor(source,lease.atlas(),new DesktopAssetManager(true),edit->{});editor.choose(Strip3DDefinition.Surface.SIDE,path);editor.setTextureCentre(286,42,true);
                var mask=editor.textureCanvas.coverage();assertTrue(mask.get(42*288));assertTrue(mask.get(42*288+287));assertEquals(286,editor.textureCanvas.windowX());
                for(int y=0;y<lease.atlas().size();y++)for(int x=0;x<lease.atlas().size();x++)if(lease.atlas().occupied(20,x,y)){
                    int layer=lease.atlas().layer(20,x,y)-1;int sx=PixelRingTexture.sourceX(lease.atlas(),editor.draft,20,layer,x,288),sy=PixelRingTexture.sourceY(lease.atlas(),editor.draft,20,layer,y,85);
                    assertTrue(mask.get(sy*288+sx));
                }
            });
        }
    }
    @Test void jointAtTheEndOfALimbDoesNotMoveTheTextureCentre()throws Exception{
        var node=new com.jme3.scene.Node("Arm");var geometry=new com.jme3.scene.Geometry("Mesh",new com.jme3.scene.shape.Box(.2f,.5f,.2f));geometry.setLocalTranslation(0,-.5f,0);node.attachChild(geometry);
        var d=BlenderPixelImporter.fromScene(node).get(0).definition();try(var lease=PixelAtlasCache.SHARED.acquire(d)){
            var r=PixelRingTexture.requirement(lease.atlas());var image=ImageIO.read(Path.of(png(r.width(),r.recommendedHeight())).toFile());
            assertNotEquals(lease.atlas().size()/2,lease.atlas().sampleCentreY(20));
            for(int view:new int[]{0,20,40,60,80}){
                int x=lease.atlas().sampleCentreX(view),y=lease.atlas().sampleCentreY(view);
                assertEquals(image.getRGB(PixelRingTexture.originX(image.getWidth()),PixelRingTexture.originY(image.getHeight())),PixelArtwork.view(lease.atlas(),d,view,new BufferedImage[]{image,image,image}).getRGB(x,y));
            }
        }
    }
    @Test void arbitraryRingWidthsApplyAndKeepWholeColumnClosureWithoutNewCaptures()throws Exception{
        var source=definition();try(var lease=PixelAtlasCache.SHARED.acquire(source)){
            long builds=PixelAtlasCache.SHARED.builds();String narrow=png(37,85),tiny=png(7,11);
            SwingUtilities.invokeAndWait(()->{
                var editor=new PixelStripTextureDialog.Editor(source,lease.atlas(),new DesktopAssetManager(true),edit->{});
                for(String path:new String[]{narrow,tiny}){
                    editor.choose(Strip3DDefinition.Surface.SIDE,path);assertNull(editor.invalid);
                    assertEquals(path,editor.finish(JOptionPane.CLOSED_OPTION).getStrip3D().layer(Strip3DDefinition.Surface.SIDE).getColorPath());
                    assertTrue(editor.stepLabel.getText().contains(path.equals(narrow)?"1–2":"0–1"));
                    assertEquals(0,PixelRingTexture.phase(20,editor.images[1].getWidth()));
                }
            });
            assertEquals(builds,PixelAtlasCache.SHARED.builds());
        }
    }
    @Test void fiveViewButtonsSelectExactAnglesWithoutTouchingTheMaterialRegion()throws Exception{
        var source=definition();try(var lease=PixelAtlasCache.SHARED.acquire(source)){
            SwingUtilities.invokeAndWait(()->{
                var editor=new PixelStripTextureDialog.Editor(source,lease.atlas(),new DesktopAssetManager(true),edit->{});
                String[] labels={"正顶端 +90°","上斜 +45°","平视 0°","下斜 −45°","正底端 −90°"};int[] angles={90,45,0,-45,-90},bands={3,2,1,0,4};
                for(int i=0;i<5;i++){
                    button(editor,labels[i]).doClick();assertEquals(angles[i],editor.pitch);assertEquals(1,editor.activeSurface);
                    var s=editor.textureCanvas.choice();for(int row=0;row<lease.atlas().size();row++)assertEquals(bands[i]*PixelAtlas.YAW_VIEWS,s.viewForRow(row));
                }
            });
        }
    }
    @Test void silhouetteEditorOrdersTheFiveGroupsWithoutReassigningSavedSlantEdits()throws Exception{
        var source=definition();try(var lease=PixelAtlasCache.SHARED.acquire(source)){
            var image=PixelRingTexture.silhouette();SwingUtilities.invokeAndWait(()->{
                var editor=new BakedSilhouetteDialog.Editor(source,lease.atlas(),new BufferedImage[]{image,image,image});
                assertEquals(5,editor.band.getItemCount());assertEquals(20,editor.view());
                int[] groups={4,0,1,2,3};for(int i=0;i<5;i++){editor.band.setSelectedIndex(i);assertEquals(groups[i]*PixelAtlas.YAW_VIEWS,editor.view());}
                assertTrue(editor.band.getItemAt(0).contains("−90°"));assertTrue(editor.band.getItemAt(4).contains("+90°"));
            });
        }
    }
    @Test void verticalSlidingAndMousePaintedRowPriorityApplyTogetherAndSaveOnClose()throws Exception{
        var d=definition();try(var lease=PixelAtlasCache.SHARED.acquire(d)){
            String path=png(37,150);long builds=PixelAtlasCache.SHARED.builds();String key=lease.atlas().key();
            SwingUtilities.invokeAndWait(()->{
                var editor=new PixelStripTextureDialog.Editor(d,lease.atlas(),new DesktopAssetManager(true),edit->{});editor.choose(Strip3DDefinition.Surface.SIDE,path);
                editor.frameRows.setValue(50);editor.verticalSlide.doClick();editor.pitch=45;assertEquals(50,editor.textureCanvas.windowY());
                var canvas=editor.textureCanvas;canvas.editingRows=true;canvas.importanceBrush=3;mouse(canvas,MouseEvent.MOUSE_PRESSED,10,95);mouse(canvas,MouseEvent.MOUSE_DRAGGED,10,97);mouse(canvas,MouseEvent.MOUSE_RELEASED,10,97);
                var result=editor.finish(JOptionPane.CLOSED_OPTION);assertTrue(result.getBakedPixels().isVerticalTextureEnabled());assertEquals(50,result.getBakedPixels().getVerticalFrameHeight());
                for(int layer=0;layer<3;layer++)for(int row=95;row<=97;row++)assertEquals(3,result.getBakedPixels().rowImportance(layer,row));
                var target=d.copy();PuppetEditorApp.copyPixelTextureSettings(target,result);assertEquals(3,target.getBakedPixels().rowImportance(1,95));assertTrue(target.getBakedPixels().isVerticalTextureEnabled());
                var saved=new Gson().fromJson(new Gson().toJson(target),VolumeDefinition.class);assertEquals(3,saved.getBakedPixels().rowImportance(1,95));assertEquals(key,PixelAtlasBaker.key(saved));
            });
            assertEquals(builds,PixelAtlasCache.SHARED.builds());
        }
    }
    static JButton button(java.awt.Container root,String text){
        for(var child:root.getComponents()){
            if(child instanceof JButton b&&text.equals(b.getText()))return b;
            if(child instanceof java.awt.Container container){var found=button(container,text);if(found!=null)return found;}
        }
        return null;
    }
    static void mouse(PixelTextureCanvas canvas,int type,int column,int row){
        canvas.dispatchEvent(new MouseEvent(canvas,type,System.currentTimeMillis(),type==MouseEvent.MOUSE_DRAGGED?MouseEvent.BUTTON1_DOWN_MASK:0,canvas.screenX(column),canvas.screenY(row),1,false,type==MouseEvent.MOUSE_DRAGGED?MouseEvent.NOBUTTON:MouseEvent.BUTTON1));
    }
}
