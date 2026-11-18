package com.Hecate.puppet.volume;
import com.Hecate.puppet.editor.*;
import com.Hecate.puppet.editor.core.*;
import com.Hecate.puppet.config.*;
import com.jme3.renderer.RenderManager;
import com.jme3.system.*;
import com.jme3.util.BufferUtils;
import java.awt.*;
import java.nio.*;
import java.nio.file.*;
import java.util.concurrent.*;
import javax.swing.*;
import javax.imageio.ImageIO;

/** Exercise the actual Save/Load dialogs, remove the originals, and compare the rendered viewport. */
public final class PuppetPersistenceSmoke extends PuppetEditorApp {
    private final CountDownLatch done=new CountDownLatch(1);private volatile Throwable failure;private int frames,stage,settle;private volatile boolean saved;private VolumeDefinition expected;private String boneName;private byte[] before;private final Path file=Path.of("target/roundtrip-ui.puppet").toAbsolutePath();private EditorBone original;
    public static void main(String[] args){try{var app=new PuppetPersistenceSmoke();var mode=PuppetEditorApp.class.getDeclaredField("volumeMode");mode.setAccessible(true);mode.setBoolean(app,true);var settings=new AppSettings(true);settings.setResolution(1600,900);settings.setAudioRenderer(null);settings.setUseInput(true);app.setSettings(settings);app.setShowSettings(false);app.start(JmeContext.Type.OffscreenSurface);boolean ok=app.done.await(50,TimeUnit.SECONDS);app.stop(false);if(!ok||app.failure!=null){if(app.failure!=null)app.failure.printStackTrace();else System.err.println("Persistence timeout stage "+app.stage);System.exit(1);}System.out.println("Actual Save/Load UI round trip passed: source PNGs removed, weights/settings preserved, viewport pixels identical");System.exit(0);}catch(Exception ex){throw new RuntimeException(ex);}}
    @Override public void simpleRender(RenderManager manager){super.simpleRender(manager);if(done.getCount()==0)return;try{
        if(++frames==8){original=getVolumeSelectedBone();boneName=original.getName();expected=original.getVolumeDefinition().copy();expected.setDimensions(36,48,28);expected.setLightingEnabled(true);expected.setLight(25,40,.8f);var strip=expected.getStrip3D();strip.setHorizontal(Strip3DDefinition.Sampling.PERSPECTIVE);int[] rows=strip.getImportance();for(int i=0;i<rows.length;i++)rows[i]=i%3+1;strip.setImportance(rows);
            for(var face:Strip3DDefinition.Surface.values()){Path color=Path.of("target/roundtrip-source-"+face+".png").toAbsolutePath(),normal=Path.of("target/roundtrip-normal-"+face+".png").toAbsolutePath();ImageIO.write(Strip3DTextures.exampleImage(face),"png",color.toFile());var image=new java.awt.image.BufferedImage(96,24,java.awt.image.BufferedImage.TYPE_INT_ARGB);for(int y=0;y<24;y++)for(int x=0;x<96;x++)image.setRGB(x,y,0xff8080ff);ImageIO.write(image,"png",normal.toFile());strip.layer(face).setColorPath(color.toString());strip.layer(face).setNormalPath(normal.toString());}
            original.setVolumeDefinition(expected);if(isVolumeModelDisplay())toggleVolumeDisplay();setVolumeCameraView(1);startDialogDriver();
        }
        if(frames==16){before=viewportPixels();invoke("savePuppet");stage=1;}
        if(stage==1&&saved){if(!PuppetPackageIO.isPackageFile(file.toString()))throw new AssertionError("Default export is not portable");for(var face:Strip3DDefinition.Surface.values()){Files.delete(Path.of(expected.getStrip3D().layer(face).getColorPath()));Files.delete(Path.of(expected.getStrip3D().layer(face).getNormalPath()));}stage=2;invoke("loadPuppet");}
        if(stage==2&&getVolumeSelectedBone()!=original&&getVolumeSelectedBone()!=null){stage=3;settle=frames+5;}
        if(stage==3&&frames>=settle){var loaded=getVolumeSelectedBone();if(!boneName.equals(loaded.getName()))throw new AssertionError("Selection changed");var d=loaded.getVolumeDefinition();if(!d.sameGeometry(expected)||!java.util.Arrays.equals(d.getStrip3D().getImportance(),expected.getStrip3D().getImportance()))throw new AssertionError("Settings lost");if(isVolumeModelDisplay())throw new AssertionError("Loaded puppet is not in image display");for(var face:Strip3DDefinition.Surface.values()){if(!Files.isRegularFile(Path.of(d.getStrip3D().layer(face).getColorPath()))||!Files.isRegularFile(Path.of(d.getStrip3D().layer(face).getNormalPath())))throw new AssertionError("Embedded images missing");}byte[] after=viewportPixels();if(!java.util.Arrays.equals(before,after)){int count=0;for(int i=0;i<before.length;i++)if(before[i]!=after[i])count++;throw new AssertionError("Viewport changed after load: "+count+" channels");}done.countDown();}
    }catch(Throwable e){failure=e;done.countDown();}}
    private void invoke(String name)throws Exception{var method=PuppetEditorApp.class.getDeclaredMethod(name);method.setAccessible(true);method.invoke(this);}
    private byte[] viewportPixels(){ByteBuffer bytes=BufferUtils.createByteBuffer(1600*900*4);getRenderer().readFrameBuffer(null,bytes);byte[] out=new byte[(1250-420)*(750-180)*3];int i=0;for(int y=180;y<750;y++)for(int x=420;x<1250;x++){int p=(y*1600+x)*4;out[i++]=bytes.get(p);out[i++]=bytes.get(p+1);out[i++]=bytes.get(p+2);}return out;}
    private void startDialogDriver(){SwingUtilities.invokeLater(()->{javax.swing.Timer timer=new javax.swing.Timer(150,null);timer.addActionListener(e->{try{if(done.getCount()==0){timer.stop();return;}for(Window window:Window.getWindows())if(window.isShowing()){
                JFileChooser chooser=find(window,JFileChooser.class);if(chooser!=null){chooser.setSelectedFile(file.toFile());chooser.approveSelection();return;}
                JOptionPane pane=find(window,JOptionPane.class);if(pane!=null){if(pane.getSelectionValues()!=null){pane.setInputValue(pane.getSelectionValues()[0]);pane.setValue(JOptionPane.OK_OPTION);}else{String message=String.valueOf(pane.getMessage());pane.setValue(JOptionPane.OK_OPTION);if(message.contains("失败"))throw new AssertionError(message);if(message.contains("导出完成"))SwingUtilities.invokeLater(()->saved=true);}return;}
            }}catch(Throwable ex){failure=ex;done.countDown();timer.stop();}});timer.start();});}
    private static <T extends Component>T find(Component c,Class<T> type){if(type.isInstance(c))return type.cast(c);if(c instanceof Container)for(Component child:((Container)c).getComponents()){T out=find(child,type);if(out!=null)return out;}return null;}
    @Override public void handleError(String message,Throwable e){failure=e;done.countDown();}
}

