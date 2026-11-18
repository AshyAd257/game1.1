package com.Hecate.puppet.editor;

import com.Hecate.puppet.editor.core.*;
import com.Hecate.puppet.pixel.*;
import com.Hecate.puppet.volume.*;
import com.Hecate.puppet.config.PuppetIO;
import com.Hecate.puppet.export.*;
import com.jme3.math.*;
import com.jme3.renderer.RenderManager;
import com.jme3.system.*;
import com.jme3.util.BufferUtils;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.*;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.util.concurrent.*;
import javax.imageio.ImageIO;
import javax.swing.*;

/** Actual settings controls -> actual part -> close/undo/cancel -> portable save -> reopen. */
public final class PixelVerticalCommitSmoke extends PuppetEditorApp {
    final CountDownLatch done=new CountDownLatch(1);Throwable failure;int frame,settledFrames;volatile int readyStage;
    EditorSkeleton skeleton;EditorPuppetRenderer renderer;EditorBone cube;
    PixelStripTextureDialog.Editor editor,cancelEditor;VolumeDefinition original,cancelOriginal;
    Path model,output,source,other,saved;long builds,bytes;
    public static void main(String[] args){try{run(args);}catch(Exception error){error.printStackTrace();System.exit(1);}}
    static void run(String[] args)throws Exception{
        var app=new PixelVerticalCommitSmoke();app.model=Path.of(args[0]);app.output=Path.of(args[1]);Files.createDirectories(app.output);
        Field mode=PuppetEditorApp.class.getDeclaredField("volumeMode");mode.setAccessible(true);mode.setBoolean(app,true);
        AppSettings settings=new AppSettings(true);settings.setResolution(1600,900);settings.setSamples(0);settings.setAudioRenderer(null);settings.setUseInput(true);
        app.setSettings(settings);app.setShowSettings(false);app.start(JmeContext.Type.OffscreenSurface);
        if(!app.done.await(55,TimeUnit.SECONDS)||app.failure!=null){if(app.failure!=null)app.failure.printStackTrace();app.stop(false);System.exit(1);}
        app.stop(true);System.out.println("Vertical texture settings passed: brush on actual PNG rows, half/full sliding parameters, immediate part display, undo/cancel, embedded portable save without original image, row priorities restored, no recapture.");System.exit(0);
    }
    @Override public void handleError(String message,Throwable error){failure=error==null?new AssertionError(message):error;done.countDown();}
    Object field(String name)throws Exception{Field f=PuppetEditorApp.class.getDeclaredField(name);f.setAccessible(true);return f.get(this);}
    void checkPaths(String expected){for(var surface:Strip3DDefinition.Surface.values())if(!java.util.Objects.equals(expected,cube.getVolumeDefinition().getStrip3D().layer(surface).getColorPath()))throw new AssertionError("Texture lost on "+surface);}
    void writeSource(Path path,int width,int height,boolean reversed)throws Exception{
        BufferedImage image=new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<height;y++)for(int x=0;x<width;x++)image.setRGB(x,y,Math.abs(x-50)<=1?0xfff4cf33:(y<height/2)^reversed?0xffcc5533:0xff3377dd);
        ImageIO.write(image,"png",path.toFile());
    }
    @Override public void simpleRender(RenderManager manager){super.simpleRender(manager);if(failure!=null)return;try{frame++;
        if((frame==14&&readyStage<1)||(frame==20&&readyStage<2)||(frame==26&&readyStage<3)||(frame==31&&readyStage<4)){frame--;return;}
        if(frame==14&&++settledFrames<3){frame--;return;}
        if(frame==6){
            Object scene=field("puppetTestScene");skeleton=(EditorSkeleton)scene.getClass().getMethod("getTestSkeleton").invoke(scene);renderer=(EditorPuppetRenderer)scene.getClass().getMethod("getPuppetRenderer").invoke(scene);
            Method importParts=PuppetEditorApp.class.getDeclaredMethod("applyBlenderParts",java.util.List.class,EditorBone.class);importParts.setAccessible(true);importParts.invoke(this,BlenderPixelImporter.load(model),null);cube=skeleton.findBone("Cube");
            original=cube.getVolumeDefinition().copy();renderer.setVolumeViewMode(VolumeReferenceView.Mode.SOLID);builds=PixelAtlasCache.SHARED.builds();bytes=PixelAtlasCache.SHARED.bytes();
            var r=getPixelTextureRequirement();if(r.width()!=20)throw new AssertionError("Unexpected strip size");
            Path work=Path.of("target/vertical-commit-smoke").toAbsolutePath();Files.createDirectories(work);source=work.resolve("strip.png");other=work.resolve("other.png");writeSource(source,288,r.recommendedHeight(),false);writeSource(other,288,r.recommendedHeight(),true);
        }
        if(frame==8){var atlas=renderer.getPartRenderer(cube.getName()).getStrip3DView().getBakedView().atlas();
            SwingUtilities.invokeLater(()->{try{editor=new PixelStripTextureDialog.Editor(original,atlas,getAssetManager(),edit->enqueue(()->{previewPixelTextureSettings(cube,edit);var origin=edit.getBakedPixels();var layer=edit.getStrip3D().layer(Strip3DDefinition.Surface.SIDE);if(origin.getTextureOriginX()==8&&origin.getTextureOriginY()==-4&&layer.getOffsetX()==6&&layer.getOffsetY()==4&&origin.isVerticalTextureEnabled()&&origin.getVerticalFrameHeight()==50&&origin.rowImportance(1,60)==3)readyStage=1;}));editor.choose(Strip3DDefinition.Surface.TOP,source.toString());
                editor.textureCanvas.editingCentre=true;PixelTextureSettingsTest.mouse(editor.textureCanvas,java.awt.event.MouseEvent.MOUSE_PRESSED,44,38);PixelTextureSettingsTest.mouse(editor.textureCanvas,java.awt.event.MouseEvent.MOUSE_RELEASED,44,38);
                editor.textureCanvas.editingCentre=false;PixelTextureSettingsTest.mouse(editor.textureCanvas,java.awt.event.MouseEvent.MOUSE_PRESSED,44,38);PixelTextureSettingsTest.mouse(editor.textureCanvas,java.awt.event.MouseEvent.MOUSE_DRAGGED,50,42);PixelTextureSettingsTest.mouse(editor.textureCanvas,java.awt.event.MouseEvent.MOUSE_RELEASED,50,42);
                editor.frameRows.setValue(50);editor.verticalSlide.doClick();editor.textureCanvas.editingRows=true;editor.textureCanvas.importanceBrush=3;PixelTextureSettingsTest.mouse(editor.textureCanvas,java.awt.event.MouseEvent.MOUSE_PRESSED,50,60);PixelTextureSettingsTest.mouse(editor.textureCanvas,java.awt.event.MouseEvent.MOUSE_RELEASED,50,60);editor.textureCanvas.editingRows=false;
                editor.setSize(editor.getPreferredSize());layout(editor);BufferedImage image=new BufferedImage(editor.getWidth(),editor.getHeight(),BufferedImage.TYPE_INT_RGB);var graphics=image.createGraphics();editor.paint(graphics);graphics.dispose();ImageIO.write(image,"png",output.resolve("texture-settings-preview.png").toFile());}catch(Throwable error){failure=error;done.countDown();}});
        }
        if(frame==14){checkPaths(source.toString());checkPositions();if(renderer.getVolumeViewMode()!=VolumeReferenceView.Mode.APPEARANCE)throw new AssertionError("Part remained in model view");styled("texture-applied-live");}
        if(frame==15)SwingUtilities.invokeLater(()->{var result=editor.finish(JOptionPane.CLOSED_OPTION);enqueue(()->{finishPixelTextureSettings(cube,original,result);readyStage=2;});});
        if(frame==20){checkPaths(source.toString());checkPositions();styled("texture-applied-after-close");getCommandManager().undo();checkPaths(null);if(cube.getVolumeDefinition().getBakedPixels().getTextureOriginX()!=0||cube.getVolumeDefinition().getStrip3D().layer(Strip3DDefinition.Surface.SIDE).getOffsetX()!=0)throw new AssertionError("Undo lost original positions");getCommandManager().redo();checkPaths(source.toString());checkPositions();}
        if(frame==22){cancelOriginal=cube.getVolumeDefinition().copy();var atlas=renderer.getPartRenderer(cube.getName()).getStrip3DView().getBakedView().atlas();SwingUtilities.invokeLater(()->{cancelEditor=new PixelStripTextureDialog.Editor(cancelOriginal,atlas,getAssetManager(),edit->enqueue(()->{previewPixelTextureSettings(cube,edit);readyStage=3;}));cancelEditor.choose(Strip3DDefinition.Surface.SIDE,other.toString());});}
        if(frame==26){checkPaths(other.toString());SwingUtilities.invokeLater(()->{var result=cancelEditor.finish(JOptionPane.CANCEL_OPTION);enqueue(()->{finishPixelTextureSettings(cube,cancelOriginal,result);readyStage=4;});});}
        if(frame==31){checkPaths(source.toString());styled("texture-after-cancel");saved=output.resolve("texture-settings-saved.puppet");ExportManager.export(ExportFormat.PUPPET,skeleton,renderer,saved.toString());}
        if(frame==32){Files.delete(source);var config=PuppetIO.loadFromFile(saved.toString());PuppetIO.applyConfig(config,skeleton,renderer);cube=skeleton.findBone("Cube");
            for(var surface:Strip3DDefinition.Surface.values()){String path=cube.getVolumeDefinition().getStrip3D().layer(surface).getColorPath();if(path==null||!Files.isRegularFile(Path.of(path)))throw new AssertionError("Saved puppet lost embedded texture");var image=PixelRingTexture.readImage(getAssetManager(),path);if(image.getRGB(36,10)!=0xffcc5533||image.getRGB(36,70)!=0xff3377dd)throw new AssertionError("Embedded artwork changed");}
            Method refresh=PuppetEditorApp.class.getDeclaredMethod("refreshVolumeStructure",EditorBone.class);refresh.setAccessible(true);refresh.invoke(this,cube);
        }
        if(frame==38){checkPositions();styled("texture-reopened-from-save");if(PixelAtlasCache.SHARED.builds()!=builds||PixelAtlasCache.SHARED.bytes()!=bytes)throw new AssertionError("Texture edits recaptured geometry");setPixelCameraElevation(45);}
        if(frame==43){styled("vertical-up45");checkPositions();done.countDown();}
    }catch(Throwable error){failure=error;done.countDown();}}
    static void layout(Container container){container.doLayout();for(Component child:container.getComponents())if(child instanceof Container nested)layout(nested);}
    void checkPositions(){var state=cube.getVolumeDefinition().getBakedPixels();if(!state.isVerticalTextureEnabled()||state.getVerticalFrameHeight()!=50||state.rowImportance(1,60)!=3)throw new AssertionError("Vertical sampling/row priority were lost");var p=cube.getVolumeDefinition().getBakedPixels();var l=cube.getVolumeDefinition().getStrip3D().layer(Strip3DDefinition.Surface.SIDE);if(p.getTextureOriginX()!=8||p.getTextureOriginY()!=-4||l.getOffsetX()!=6||l.getOffsetY()!=4)throw new AssertionError("Mouse-edited positions were lost");}
    void styled(String name)throws Exception{
        ByteBuffer data=BufferUtils.createByteBuffer(1600*900*4);getRenderer().readFrameBuffer(null,data);BufferedImage image=new BufferedImage(1600,900,BufferedImage.TYPE_INT_RGB);
        for(int y=0;y<900;y++)for(int x=0;x<1600;x++){int i=(y*1600+x)*4;image.setRGB(x,899-y,(data.get(i)&255)<<16|(data.get(i+1)&255)<<8|(data.get(i+2)&255));}
        if(!name.equals("vertical-up45")){int top=image.getRGB(850,330),bottom=image.getRGB(850,580);if((top>>16&255)<(top&255)||(bottom&255)<(bottom>>16&255))throw new AssertionError("Actual part texture missing or vertically misplaced: "+Integer.toHexString(top)+" / "+Integer.toHexString(bottom));
        int marker=image.getRGB(802,580);if((marker>>16&255)<180||(marker>>8&255)<150||(marker&255)>90)throw new AssertionError("Mouse-edited PNG position does not match the actual part: "+Integer.toHexString(marker));}
        ImageIO.write(image,"png",output.resolve(name+".png").toFile());
    }
}
