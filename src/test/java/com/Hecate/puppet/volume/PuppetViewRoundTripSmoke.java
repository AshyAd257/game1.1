package com.Hecate.puppet.volume;

import com.Hecate.puppet.config.*;
import com.Hecate.puppet.editor.core.*;
import com.Hecate.puppet.export.*;
import com.jme3.app.SimpleApplication;
import com.jme3.math.*;
import com.jme3.renderer.RenderManager;
import com.jme3.system.*;
import com.jme3.util.BufferUtils;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.concurrent.*;
import javax.imageio.ImageIO;

/** Read-only user fixture; render, save a separate copy, reload and compare all views exactly. */
public final class PuppetViewRoundTripSmoke extends SimpleApplication {
    private static String source;private static boolean perspective;private final CountDownLatch done=new CountDownLatch(1);private Throwable failure;
    private final BufferedImage[] before=new BufferedImage[6];private EditorSkeleton skeleton;private EditorPuppetRenderer puppet;
    private int frame,view,pass;private static final int SIZE=600;private final int[] angles={0,45,90,135,180,270};
    public PuppetViewRoundTripSmoke(){super(new com.jme3.app.StatsAppState());}
    public static void main(String[] args)throws Exception{source=args[0];perspective=java.util.Arrays.asList(args).contains("--perspective");var app=new PuppetViewRoundTripSmoke();var s=new AppSettings(true);s.setResolution(SIZE,SIZE);s.setAudioRenderer(null);s.setUseInput(false);s.setSamples(0);s.setGammaCorrection(false);app.setSettings(s);app.setShowSettings(false);app.start(JmeContext.Type.OffscreenSurface);boolean ok=app.done.await(60,TimeUnit.SECONDS);app.stop(false);if(!ok||app.failure!=null){if(app.failure!=null)app.failure.printStackTrace();System.exit(1);}System.out.println("Six camera angles preserved pixel-for-pixel after editor export and reload");System.exit(0);}
    public void simpleInitApp(){try{setDisplayFps(false);setDisplayStatView(false);viewPort.setBackgroundColor(new ColorRGBA(.15f,.17f,.2f,1));skeleton=new EditorSkeleton("roundtrip");puppet=new EditorPuppetRenderer(this,skeleton);PuppetIO.applyConfig(PuppetIO.loadFromFile(source),skeleton,puppet);puppet.attachToScene(rootNode);setup();}catch(Throwable e){failure=e;done.countDown();}}
    private void setup(){float angle=angles[view]*FastMath.DEG_TO_RAD;var target=new Vector3f(-.484375f,0,0);cam.setParallelProjection(true);cam.setFrustum(.1f,1000,-1.8f,1.8f,1.8f,-1.8f);if(perspective){cam.setParallelProjection(false);cam.setFrustumPerspective(40,1,.1f,1000);}cam.setLocation(target.add(new Vector3f(FastMath.sin(angle)*6,0,FastMath.cos(angle)*6)));cam.lookAt(target,Vector3f.UNIT_Y);}
    public void simpleUpdate(float tpf){if(done.getCount()>0&&puppet!=null)puppet.update(0);}
    public void simpleRender(RenderManager rm){if(done.getCount()==0||++frame%4!=0)return;try{
        var data=BufferUtils.createByteBuffer(SIZE*SIZE*4);renderer.readFrameBuffer(null,data);var image=new BufferedImage(SIZE,SIZE,BufferedImage.TYPE_INT_RGB);
        for(int y=0;y<SIZE;y++)for(int x=0;x<SIZE;x++){int i=(y*SIZE+x)*4;image.setRGB(x,SIZE-1-y,(data.get(i)&255)<<16|(data.get(i+1)&255)<<8|data.get(i+2)&255);}
        if(pass==0){before[view]=image;ImageIO.write(image,"png",Path.of("target/legs-"+(perspective?"perspective-":"")+"view-"+angles[view]+".png").toFile());}
        else for(int y=0;y<SIZE;y++)for(int x=0;x<SIZE;x++)if(image.getRGB(x,y)!=before[view].getRGB(x,y))throw new AssertionError("Reload differs at yaw "+angles[view]+" pixel "+x+","+y);
        if(++view<angles.length){setup();return;}
        if(pass++==0){int count=skeleton.getBoneCount();var copy=Path.of("target/legs-roundtrip.puppet").toAbsolutePath();ExportManager.export(ExportFormat.PUPPET,skeleton,puppet,copy.toString());PuppetIO.applyConfig(PuppetIO.loadFromFile(copy.toString()),skeleton,puppet);if(count!=skeleton.getBoneCount())throw new AssertionError("Lost parts");view=0;setup();return;}
        var sheet=new BufferedImage(SIZE*3,SIZE*2,BufferedImage.TYPE_INT_RGB);var g=sheet.createGraphics();for(int i=0;i<6;i++){g.drawImage(before[i],i%3*SIZE,i/3*SIZE,null);g.setColor(java.awt.Color.WHITE);g.drawString("YAW "+angles[i],i%3*SIZE+15,i/3*SIZE+25);}g.dispose();ImageIO.write(sheet,"png",Path.of(perspective?"target/legs-perspective-angles.png":"target/legs-angles.png").toFile());done.countDown();
    }catch(Throwable e){failure=e;done.countDown();}}
    public void handleError(String message,Throwable e){failure=e;done.countDown();}
}
