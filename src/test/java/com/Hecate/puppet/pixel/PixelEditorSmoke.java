package com.Hecate.puppet.pixel;

import com.Hecate.puppet.editor.*;
import com.Hecate.puppet.editor.core.*;
import com.Hecate.puppet.volume.*;
import com.jme3.math.*;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.*;
import com.jme3.scene.shape.Box;
import com.jme3.system.*;
import com.jme3.util.BufferUtils;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.*;
import java.nio.file.*;
import java.lang.reflect.*;
import java.util.concurrent.*;

/** Full editor integration: empty start, Blender parts, hierarchy, undo/redo, timeline and rigid animation. */
public final class PixelEditorSmoke extends PuppetEditorApp {
    final CountDownLatch done=new CountDownLatch(1);Throwable failure;int frames;EditorSkeleton skeleton;EditorPuppetRenderer renderer;PuppetEditorUI ui;long builds,bytes;
    public static void main(String[] args){try{run();}catch(Exception e){throw new RuntimeException(e);}}
    private static void run()throws Exception{var app=new PixelEditorSmoke();Field mode=PuppetEditorApp.class.getDeclaredField("volumeMode");mode.setAccessible(true);mode.setBoolean(app,true);AppSettings settings=new AppSettings(true);settings.setResolution(1600,900);settings.setSamples(0);settings.setAudioRenderer(null);settings.setUseInput(true);app.setSettings(settings);app.setShowSettings(false);app.start(JmeContext.Type.OffscreenSurface);boolean finished=app.done.await(45,TimeUnit.SECONDS);app.stop(true);if(!finished)throw new AssertionError("Editor timed out");if(app.failure!=null)throw new AssertionError("Editor failed",app.failure);System.out.println("Full pixel editor smoke passed: Blender-only start, import, hierarchy, undo/redo, shared timeline, saved pose keys, rigid cached animation.");System.exit(0);}
    @Override public void handleError(String message,Throwable t){failure=t==null?new AssertionError(message):t;done.countDown();}
    Object field(String name)throws Exception{Field f=PuppetEditorApp.class.getDeclaredField(name);f.setAccessible(true);return f.get(this);}
    void method(String name)throws Exception{Method m=PuppetEditorApp.class.getDeclaredMethod(name);m.setAccessible(true);m.invoke(this);}
    @Override public void simpleRender(RenderManager manager){super.simpleRender(manager);if(failure!=null)return;try{frames++;
        if(frames==6){Object scene=field("puppetTestScene");skeleton=(EditorSkeleton)scene.getClass().getMethod("getTestSkeleton").invoke(scene);renderer=(EditorPuppetRenderer)scene.getClass().getMethod("getPuppetRenderer").invoke(scene);ui=(PuppetEditorUI)field("editorUI");if(skeleton.getBoneCount()!=0)throw new AssertionError("New mode must start with imported geometry");save("empty");
            Node model=new Node("Scene"),body=new Node("Body"),head=new Node("Head"),arm=new Node("UpperArm"),forearm=new Node("Forearm");model.attachChild(body);body.attachChild(new Geometry("BodyMesh",new Box(.4f,.6f,.15f)));body.attachChild(head);head.setLocalTranslation(0,.9f,0);head.attachChild(new Geometry("HeadMesh",new Box(.3f,.3f,.25f)));body.attachChild(arm);arm.setLocalTranslation(.55f,.45f,0);Geometry upper=new Geometry("UpperMesh",new Box(.12f,.35f,.12f));upper.setLocalTranslation(0,-.35f,0);arm.attachChild(upper);arm.attachChild(forearm);forearm.setLocalTranslation(0,-.7f,0);Geometry lower=new Geometry("LowerMesh",new Box(.12f,.35f,.12f));lower.setLocalTranslation(0,-.35f,0);forearm.attachChild(lower);
            var parts=BlenderPixelImporter.fromScene(model);Path dir=Path.of("target/pixel-editor");Files.createDirectories(dir);BufferedImage strip=new BufferedImage(96,24,BufferedImage.TYPE_INT_ARGB);for(int y=0;y<24;y++)for(int x=0;x<96;x++)strip.setRGB(x,y,0xff000000|((80+x)%255)<<16|((110+y*4)%255)<<8|180);Path png=dir.resolve("strip.png").toAbsolutePath();ImageIO.write(strip,"png",png.toFile());for(var p:parts)for(var s:Strip3DDefinition.Surface.values())p.definition().getStrip3D().layer(s).setColorPath(png.toString());
            Method apply=PuppetEditorApp.class.getDeclaredMethod("applyBlenderParts",java.util.List.class,EditorBone.class);apply.setAccessible(true);apply.invoke(this,parts,null);if(skeleton.getBoneCount()!=5||skeleton.findBone("Forearm").getParent()!=skeleton.findBone("UpperArm"))throw new AssertionError("Import lost object hierarchy");
        }
        if(frames==12){if(renderer.getVolumeViewMode()!=VolumeReferenceView.Mode.APPEARANCE)throw new AssertionError("Import must show pixel silhouette immediately");renderer.setVolumeViewMode(VolumeReferenceView.Mode.SOLID);save("model");getCommandManager().undo();if(skeleton.getBoneCount()!=0)throw new AssertionError("Import undo left geometry");getCommandManager().redo();if(skeleton.getBoneCount()!=5)throw new AssertionError("Import redo lost model");if(renderer.getVolumeViewMode()!=VolumeReferenceView.Mode.APPEARANCE)throw new AssertionError("Redo must restore silhouette preview");}
        if(frames==18){save("pixels");method("addKeyframe");var clip=(com.Hecate.puppet.animation.AnimationClip)field("currentClip");if(clip.getAllKeyframes().isEmpty())throw new AssertionError("Existing keyframe action lost");ui.reshapeUI(1280,720);if(ui.getTimelinePanel().getAnimationClip()!=clip)throw new AssertionError("Timeline lost on resize");ui.reshapeUI(1600,900);builds=PixelAtlasCache.SHARED.builds();bytes=PixelAtlasCache.SHARED.bytes();
            EditorBone arm=skeleton.findBone("UpperArm");for(int i=0;i<1000;i++){arm.setLocalRotation(new Quaternion().fromAngles(i*.001f,i*.002f,i*.003f));renderer.update(1/60f);}if(PixelAtlasCache.SHARED.builds()!=builds||PixelAtlasCache.SHARED.bytes()!=bytes)throw new AssertionError("Bone poses recaptured");
        }
        if(frames==22){save("animated");done.countDown();}
    }catch(Throwable t){failure=t;done.countDown();}}
    void save(String name)throws Exception{Path dir=Path.of("target/pixel-editor");Files.createDirectories(dir);ByteBuffer data=BufferUtils.createByteBuffer(1600*900*4);getRenderer().readFrameBuffer(null,data);BufferedImage image=new BufferedImage(1600,900,BufferedImage.TYPE_INT_RGB);for(int y=0;y<900;y++)for(int x=0;x<1600;x++){int i=(y*1600+x)*4;image.setRGB(x,899-y,(data.get(i)&255)<<16|(data.get(i+1)&255)<<8|(data.get(i+2)&255));}ImageIO.write(image,"png",dir.resolve(name+".png").toFile());}
}
