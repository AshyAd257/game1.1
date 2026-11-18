package com.Hecate.puppet.volume;

import com.Hecate.puppet.editor.PuppetEditorApp;
import com.Hecate.puppet.editor.core.*;
import com.jme3.renderer.RenderManager;
import com.jme3.system.*;
import com.jme3.util.BufferUtils;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.util.concurrent.*;
import javax.imageio.ImageIO;

/** Hidden full-editor startup, create/undo/redo and reference-view smoke check. */
public final class EditorVolumeSmoke extends PuppetEditorApp {
    private final CountDownLatch done=new CountDownLatch(1);
    private volatile Throwable failure;
    private int frames,oldCount,importStep;
    private volatile boolean chooserCancelled,importErrorDismissed;
    private EditorSkeleton skeleton;
    private EditorPuppetRenderer renderer;
    public static void main(String[] args) {
        EditorVolumeSmoke app=new EditorVolumeSmoke();AppSettings settings=new AppSettings(true);
        if(java.util.Arrays.asList(args).contains("--new-mode")) {
            try {var field=PuppetEditorApp.class.getDeclaredField("volumeMode");field.setAccessible(true);field.setBoolean(app,true);}
            catch(Exception e){throw new RuntimeException(e);}
        }
        settings.setResolution(1600,900);settings.setAudioRenderer(null);settings.setUseInput(true);settings.setSamples(0);
        app.setSettings(settings);app.setShowSettings(false);app.start(JmeContext.Type.OffscreenSurface);
        boolean finished=false;
        try { finished=app.done.await(40,TimeUnit.SECONDS); } catch(InterruptedException e) { Thread.currentThread().interrupt(); }
        app.stop(false);
        if(!finished){System.err.println("Editor smoke timed out");System.exit(1);}
        if(app.failure!=null){app.failure.printStackTrace();System.exit(1);}
        System.out.println("Full editor smoke passed: create, undo, redo, reference display, sequential three-layer imports, cancel and reopen, error recovery");System.exit(0);
    }
    @Override public void simpleRender(RenderManager manager) {
        super.simpleRender(manager);
        try {
            int frame=++frames;
            if(frame==8) {
                var field=PuppetEditorApp.class.getDeclaredField("puppetTestScene");field.setAccessible(true);Object scene=field.get(this);
                skeleton=(EditorSkeleton)scene.getClass().getMethod("getTestSkeleton").invoke(scene);
                renderer=(EditorPuppetRenderer)scene.getClass().getMethod("getPuppetRenderer").invoke(scene);oldCount=skeleton.getBoneCount();
                if(isVolumeMode()) {
                    if(!isVolumeModelDisplay())throw new AssertionError("New mode must start with models");
                    if(skeleton.getAllBones().stream().anyMatch(b->b.getVolumeDefinition()==null))throw new AssertionError("Legacy sprite in new-mode sample");
                    var uiField=PuppetEditorApp.class.getDeclaredField("editorUI");uiField.setAccessible(true);
                    var ui=(com.Hecate.puppet.editor.PuppetEditorUI)uiField.get(this);
                    var selected=getVolumeSelectedBone();var position=selected.getLocalPosition().clone();
                    ui.handleMouseClick(1210,880);
                    if(isVolumeModelDisplay()||!position.equals(selected.getLocalPosition()))throw new AssertionError("Display toggle changed pose or failed");
                    toggleVolumeDisplay();
                }
                var create=PuppetEditorApp.class.getDeclaredMethod("createVolumePart",VolumeDefinition.class);create.setAccessible(true);create.invoke(this,new VolumeDefinition());
                if(skeleton.getBoneCount()!=oldCount+1)throw new AssertionError("Create did not add a bone");
                renderer.setVolumeViewMode(VolumeReferenceView.Mode.SOLID);
            }
            if(frame==14) {
                ByteBuffer bytes=BufferUtils.createByteBuffer(1600*900*4);getRenderer().readFrameBuffer(null,bytes);
                BufferedImage image=new BufferedImage(1600,900,BufferedImage.TYPE_INT_RGB);
                for(int y=0;y<900;y++)for(int x=0;x<1600;x++){int p=(y*1600+x)*4;image.setRGB(x,899-y,((bytes.get(p)&255)<<16)|((bytes.get(p+1)&255)<<8)|(bytes.get(p+2)&255));}
                ImageIO.write(image,"png",Path.of(isVolumeMode()?"target/volume-mode-smoke.png":"target/volume-editor-smoke.png").toFile());
                getCommandManager().undo();if(skeleton.getBoneCount()!=oldCount)throw new AssertionError("Undo failed");
                getCommandManager().redo();if(skeleton.getBoneCount()!=oldCount+1)throw new AssertionError("Redo failed");
                if(isVolumeMode()) {
                    var selected=getVolumeSelectedBone();setVolumeShape(VolumeDefinition.Shape.CONE);
                    if(!selected.isRotationStripEnabled()||Math.abs(selected.getStripHeight()-selected.getVolumeDefinition().height())>.001f)throw new AssertionError("New part appearance dimensions not initialized");
                    if(selected.getVolumeDefinition().getShape()!=VolumeDefinition.Shape.CONE)throw new AssertionError("Inspector shape change failed");
                    getCommandManager().undo();if(selected.getVolumeDefinition().getShape()!=VolumeDefinition.Shape.CYLINDER)throw new AssertionError("Shape undo failed");
                    var pose=PuppetEditorApp.class.getDeclaredMethod("applyVolumePose",EditorBone.class,String.class,float.class);pose.setAccessible(true);pose.invoke(this,selected,"rx",30f);
                    if(Math.abs(selected.getLocalRotation().toAngles(null)[0]-(float)Math.PI/6)>.001)throw new AssertionError("Pose editor rotates sprite instead of skeleton");
                    getCommandManager().undo();
                    var addKey=PuppetEditorApp.class.getDeclaredMethod("addKeyframe");addKey.setAccessible(true);addKey.invoke(this);
                    var clipField=PuppetEditorApp.class.getDeclaredField("currentClip");clipField.setAccessible(true);
                    var clip=(com.Hecate.puppet.animation.AnimationClip)clipField.get(this);
                    if(clip.getAllKeyframes().isEmpty())throw new AssertionError("Shared keyframe creation failed");
                    var uiField=PuppetEditorApp.class.getDeclaredField("editorUI");uiField.setAccessible(true);var ui=(com.Hecate.puppet.editor.PuppetEditorUI)uiField.get(this);
                    ui.reshapeUI(1280,720);
                    if(ui.getTimelinePanel().getAnimationClip()!=clip)throw new AssertionError("Resize lost timeline");
                    ui.reshapeUI(1600,900);
                    var copy=PuppetEditorApp.class.getDeclaredMethod("copyBoneRecursive",EditorBone.class);copy.setAccessible(true);
                    selected.setStripCalibrationOffsetPx(7);selected.setStripFrameWidthPx(23);
                    var data=(com.Hecate.puppet.editor.BoneClipboardData.BoneData)copy.invoke(this,selected);
                    var paste=PuppetEditorApp.class.getDeclaredMethod("createBoneFromData",com.Hecate.puppet.editor.BoneClipboardData.BoneData.class);paste.setAccessible(true);
                    EditorBone pasted=(EditorBone)paste.invoke(this,data);
                    if(!pasted.isRotationStripEnabled()||pasted.getStripFrameWidthPx()!=23||pasted.getStripCalibrationOffsetPx()!=7||pasted.getVolumeDefinition()==null)throw new AssertionError("Copy lost volume strip data");
                }
            }
            if(isVolumeMode() && (frame==18 || frame==24)) {
                var uiField=PuppetEditorApp.class.getDeclaredField("editorUI");uiField.setAccessible(true);
                var ui=(com.Hecate.puppet.editor.PuppetEditorUI)uiField.get(this);
                if(frame==18){
                    var sphere=PuppetEditorApp.class.getDeclaredField("directionSphere");sphere.setAccessible(true);if(sphere.get(this)==null)throw new AssertionError("Orientation globe missing");
                    setVolumeCameraView(1);if(!getCamera().isParallelProjection()||getCamera().getDirection().z>-.999f)throw new AssertionError("Front preset");
                    setVolumeCameraView(3);if(getCamera().getDirection().x>-.999f)throw new AssertionError("Side preset");
                    setVolumeCameraView(7);if(getCamera().getDirection().y>-.999f)throw new AssertionError("Top preset");
                    setVolumeCameraView(5);if(getCamera().isParallelProjection())throw new AssertionError("Projection toggle");
                    setVolumeCameraView(1);toggleVolumeDisplay();
                    ui.handleMouseClick(1420,785);ui.handleMouseClick(135,485);
                }
                else {ui.handleMouseClick(1470,785);ui.handleMouseClick(50,810);}
            }
            if(isVolumeMode() && (frame==22 || frame==28)) {
                ByteBuffer bytes=BufferUtils.createByteBuffer(1600*900*4);getRenderer().readFrameBuffer(null,bytes);
                BufferedImage image=new BufferedImage(1600,900,BufferedImage.TYPE_INT_RGB);
                for(int y=0;y<900;y++)for(int x=0;x<1600;x++){int p=(y*1600+x)*4;image.setRGB(x,899-y,((bytes.get(p)&255)<<16)|((bytes.get(p+1)&255)<<8)|(bytes.get(p+2)&255));}
                ImageIO.write(image,"png",Path.of(frame==22?"target/volume-pose-page.png":"target/volume-image-page.png").toFile());
            }
            if(isVolumeMode()&&frame==30)startImport(0);
            if(isVolumeMode()&&frame>30&&done.getCount()>0){
                var selected=getVolumeSelectedBone();var strip=selected.getVolumeDefinition().getStrip3D();
                if(importStep<3){var face=Strip3DDefinition.Surface.values()[importStep];String path=strip.layer(face).getColorPath();
                    if(path!=null){if(!path.endsWith(face.name().toLowerCase(java.util.Locale.ROOT)+".png")||isVolumeModelDisplay())throw new AssertionError("Sequential import applied to wrong layer");
                        for(int i=0;i<=importStep;i++)if(strip.layer(Strip3DDefinition.Surface.values()[i]).getColorPath()==null)throw new AssertionError("Import erased an earlier layer");
                        if(++importStep<3)startImport(importStep);else{getCommandManager().undo();if(selected.getVolumeDefinition().getStrip3D().layer(Strip3DDefinition.Surface.BOTTOM).getColorPath()!=null)throw new AssertionError("Bottom import undo failed");getCommandManager().redo();startImport(3);}
                    }
                }else if(importStep==3&&chooserCancelled){importStep=4;startImport(4);}
                else if(importStep==4&&strip.layer(Strip3DDefinition.Surface.SIDE).getColorPath().endsWith("side-reimport.png")){importStep=5;startImport(5);}
                else if(importStep==5&&importErrorDismissed){if(!strip.layer(Strip3DDefinition.Surface.SIDE).getColorPath().endsWith("side-reimport.png"))throw new AssertionError("Invalid import overwrote existing image");importStep=6;startImport(6);}
                else if(importStep==6&&strip.layer(Strip3DDefinition.Surface.SIDE).getColorPath().endsWith("side.png"))done.countDown();
            }
            if(!isVolumeMode()&&frame==18)done.countDown();
        }catch(Throwable e){failure=e;done.countDown();}
    }
    private void startImport(int step)throws Exception{
        if(step==4)Files.copy(Path.of("src/main/resources/textures/TriStripPrototype/side.png"),Path.of("target/side-reimport.png"),StandardCopyOption.REPLACE_EXISTING);
        if(step==5)ImageIO.write(new BufferedImage(100,24,BufferedImage.TYPE_INT_ARGB),"png",Path.of("target/side-invalid-size.png").toFile());
        java.awt.EventQueue.invokeLater(()->{final int[] attempts={0};final boolean[] chosen={false};javax.swing.Timer timer=new javax.swing.Timer(150,null);timer.addActionListener(e->{
            for(java.awt.Window window:java.awt.Window.getWindows()){
                if(step==5&&chosen[0]&&window.isShowing()){javax.swing.JOptionPane error=findOptionPane(window);if(error!=null){if(!window.isAlwaysOnTop())failure=new AssertionError("Import error hidden behind editor");error.setValue(javax.swing.JOptionPane.OK_OPTION);java.awt.EventQueue.invokeLater(()->importErrorDismissed=true);timer.stop();return;}}
                javax.swing.JFileChooser chooser=findChooser(window);if(!chosen[0]&&window.isShowing()&&chooser!=null){chosen[0]=true;
                if(!window.isAlwaysOnTop()){failure=new AssertionError("Texture chooser can hide behind the native editor");chooser.cancelSelection();timer.stop();done.countDown();return;}
                if(step==3){chooser.cancelSelection();java.awt.EventQueue.invokeLater(()->chooserCancelled=true);}
                else{String name=step==4?"target/side-reimport.png":step==5?"target/side-invalid-size.png":"src/main/resources/textures/TriStripPrototype/"+(step==6?"side":Strip3DDefinition.Surface.values()[step].name().toLowerCase(java.util.Locale.ROOT))+".png";chooser.setSelectedFile(Path.of(name).toAbsolutePath().toFile());chooser.approveSelection();}if(step!=5)timer.stop();return;
            }}
            if(++attempts[0]>100){failure=new AssertionError("Import chooser missing at step "+step);timer.stop();done.countDown();}
        });timer.start();});
        var uiField=PuppetEditorApp.class.getDeclaredField("editorUI");uiField.setAccessible(true);var ui=(com.Hecate.puppet.editor.PuppetEditorUI)uiField.get(this);ui.handleMouseClick(1400,step<3?660-step*38:622);
    }
    private static javax.swing.JOptionPane findOptionPane(java.awt.Component c){if(c instanceof javax.swing.JOptionPane)return (javax.swing.JOptionPane)c;if(c instanceof java.awt.Container)for(var child:((java.awt.Container)c).getComponents()){var found=findOptionPane(child);if(found!=null)return found;}return null;}
    private static javax.swing.JFileChooser findChooser(java.awt.Component component){
        if(component instanceof javax.swing.JFileChooser)return (javax.swing.JFileChooser)component;
        if(component instanceof java.awt.Container)for(java.awt.Component child:((java.awt.Container)component).getComponents()){var found=findChooser(child);if(found!=null)return found;}return null;
    }
    @Override public void handleError(String message,Throwable e){System.err.println(message);failure=e;done.countDown();}
}
