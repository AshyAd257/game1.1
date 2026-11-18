package com.Hecate.puppet.volume;
import com.Hecate.puppet.editor.*;
import com.Hecate.puppet.editor.core.*;
import com.Hecate.puppet.volume.*;
import com.jme3.math.*;
import com.jme3.renderer.RenderManager;
import com.jme3.system.*;
import com.jme3.texture.Texture;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import javax.swing.*;
import javax.imageio.ImageIO;

public class EditorWidthApplySmoke extends PuppetEditorApp {
 private final CountDownLatch done=new CountDownLatch(1);private Throwable failure;private int frame,applied;private EditorPuppetPartRenderer part;private final Set<Integer> sourceColors=new HashSet<>();
 public static void main(String[] args){try{
  var a=new EditorWidthApplySmoke();var f=PuppetEditorApp.class.getDeclaredField("volumeMode");f.setAccessible(true);f.setBoolean(a,true);var s=new AppSettings(true);s.setResolution(1280,720);s.setAudioRenderer(null);s.setUseInput(true);s.setSamples(0);s.setGammaCorrection(false);a.setSettings(s);a.setShowSettings(false);a.start(JmeContext.Type.OffscreenSurface);boolean ok=a.done.await(35,TimeUnit.SECONDS);a.stop(false);if(!ok||a.failure!=null){if(a.failure!=null)a.failure.printStackTrace();System.exit(1);}System.out.println("Actual editor: dialog apply 9 -> 11, uploaded texture AND viewport contain 11 distinct columns");System.exit(0);
 }catch(Exception e){throw new RuntimeException(e);}}
 public void simpleRender(RenderManager rm){super.simpleRender(rm);if(done.getCount()==0)return;try{
  if(++frame==8){
   var field=PuppetEditorApp.class.getDeclaredField("puppetTestScene");field.setAccessible(true);var scene=(EditorPuppetTestScene)field.get(this);var r=scene.getPuppetRenderer();var bone=getVolumeSelectedBone();part=r.getPartRenderer(bone.getName());
   var image=new BufferedImage(36,12,BufferedImage.TYPE_INT_ARGB);for(int x=0;x<36;x++){int c=0xff000000|((x*67)%200+40)<<16|((x*37)%200+40)<<8|((x*13)%200+40);sourceColors.add(c);for(int y=0;y<12;y++)image.setRGB(x,y,c);}var path=Path.of("target/editor-width-apply-source.png").toAbsolutePath();ImageIO.write(image,"png",path.toFile());
   var d=bone.getVolumeDefinition().copy();var strip=d.getStrip3D();strip.setEnabled(true);strip.setFrameSize(9,12);strip.setSampleWidth(9);strip.setHorizontal(Strip3DDefinition.Sampling.SLIDE);strip.setVertical(Strip3DDefinition.Sampling.SLIDE);strip.setOutlineEnabled(false);boolean[] mask=new boolean[144];Arrays.fill(mask,true);strip.setSelectionMask(12,12,mask);for(var face:Strip3DDefinition.Surface.values())strip.layer(face).setColorPath(path.toString());bone.setVolumeDefinition(d);
   for(var other:r.getAllPartRenderers())other.setVisible(other==part);if(isVolumeModelDisplay())toggleVolumeDisplay();if(isVolumeBonesVisible())toggleVolumeBones();setVolumeCameraView(1);
  }
  if(frame==15){checkColumns(9);SwingUtilities.invokeLater(()->{javax.swing.Timer timer=new javax.swing.Timer(100,null);timer.addActionListener(e->{for(Window w:Window.getWindows())if(w.isShowing()){var spinner=find(w,JSpinner.class,"sampleWidth");var pane=find(w,JOptionPane.class,null);if(spinner!=null&&pane!=null){((JSpinner.DefaultEditor)spinner.getEditor()).getTextField().setText("11");pane.setValue(JOptionPane.OK_OPTION);timer.stop();}}});timer.start();});editSelectionShape();}
  if(frame>15&&getVolumeSelectedBone().getVolumeDefinition().getStrip3D().getSampleWidth()==11&&++applied==4){checkColumns(11);done.countDown();}
 }catch(Throwable e){failure=e;done.countDown();}}
 private void checkColumns(int expected)throws Exception{
  var field=part.getClass().getDeclaredField("strip3DView");field.setAccessible(true);var v=(Strip3DView)field.get(part);var texture=(Texture)v.getGeometry().getMaterial().getParam("ColorMap").getValue();var img=texture.getImage();var bytes=img.getData(0);Set<Integer> found=new HashSet<>();for(int i=0;i<bytes.limit();i+=4){int c=(bytes.get(i+3)&255)<<24|(bytes.get(i)&255)<<16|(bytes.get(i+1)&255)<<8|bytes.get(i+2)&255;if(sourceColors.contains(c))found.add(c);}System.out.println("Saved "+getVolumeSelectedBone().getVolumeDefinition().getStrip3D().getSampleWidth()+", uploaded "+found.size()+" columns");if(found.size()!=expected)throw new AssertionError("Uploaded "+found.size()+", expected "+expected);
  var screen=com.jme3.util.BufferUtils.createByteBuffer(1280*720*4);renderer.readFrameBuffer(null,screen);var image=new BufferedImage(1280,720,BufferedImage.TYPE_INT_RGB);found.clear();for(int y=0;y<720;y++)for(int x=0;x<1280;x++){int p=(y*1280+x)*4;int c=0xff000000|(screen.get(p)&255)<<16|(screen.get(p+1)&255)<<8|screen.get(p+2)&255;image.setRGB(x,719-y,c);if(sourceColors.contains(c))found.add(c);}ImageIO.write(image,"png",Path.of("target/editor-width-applied-"+expected+".png").toFile());System.out.println("Viewport "+found.size()+" columns");if(found.size()!=expected)throw new AssertionError("Viewport "+found.size()+", expected "+expected);
 }
 private static <T extends Component>T find(Component c,Class<T> type,String name){if(type.isInstance(c)&&(name==null||name.equals(c.getName())))return type.cast(c);if(c instanceof Container container)for(Component child:container.getComponents()){T v=find(child,type,name);if(v!=null)return v;}return null;}
 public void handleError(String message,Throwable e){failure=e;done.countDown();}
}
