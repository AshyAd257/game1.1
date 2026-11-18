package com.Hecate.puppet.volume;
import com.jme3.app.SimpleApplication;
import com.jme3.math.*;
import com.jme3.renderer.RenderManager;
import com.jme3.system.*;
import com.jme3.util.BufferUtils;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.concurrent.*;
import javax.imageio.ImageIO;

/** Surface-mode artwork, cutouts, outline and normal-map regression with real pixel-art fixtures. */
public final class SurfaceAppearanceSmoke extends SimpleApplication {
    private final CountDownLatch done=new CountDownLatch(1);private Throwable failure;
    private Strip3DView view;private VolumeDefinition d;private int stage,frame;
    private final BufferedImage[] shots=new BufferedImage[8];
    public SurfaceAppearanceSmoke(){super(new com.jme3.app.StatsAppState());}
    public static void main(String[] args)throws Exception{
        var app=new SurfaceAppearanceSmoke();var s=new AppSettings(true);s.setResolution(720,720);s.setAudioRenderer(null);s.setUseInput(false);s.setSamples(0);s.setGammaCorrection(false);app.setSettings(s);app.setShowSettings(false);app.start(JmeContext.Type.OffscreenSurface);
        boolean ok=app.done.await(30,TimeUnit.SECONDS);app.stop(false);if(!ok||app.failure!=null){if(app.failure!=null)app.failure.printStackTrace();System.exit(1);}System.out.println("Surface appearance: outline toggle, masked-hole outline, normal-map refresh, cone silhouette and top-view artwork passed");System.exit(0);
    }
    public void simpleInitApp(){try{
        setDisplayFps(false);setDisplayStatView(false);viewPort.setBackgroundColor(new ColorRGBA(.12f,.14f,.18f,1));cam.setParallelProjection(true);cam.setFrustum(.1f,100,-.7f,.7f,.7f,-.7f);
        d=new VolumeDefinition();d.setDimensions(24,24,24);d.getStrip3D().setEnabled(true);for(var face:Strip3DDefinition.Surface.values())d.getStrip3D().layer(face).setColorPath("textures/TriStripPrototype/"+face.name().toLowerCase(java.util.Locale.ROOT)+".png");
        view=new Strip3DView(assetManager,rootNode,"art");setup();
    }catch(Throwable e){failure=e;done.countDown();}}
    private void setup()throws Exception{
        cam.setLocation(stage==7?new Vector3f(0,5,0):new Vector3f(0,0,5));cam.lookAt(Vector3f.ZERO,stage==7?Vector3f.UNIT_Z:Vector3f.UNIT_Y);
        var strip=d.getStrip3D();strip.setOutlineEnabled(stage==1||stage==3||stage>=6);strip.resetSelectionMask();
        if(stage==2||stage==3){boolean[] mask=new boolean[576];for(int y=0;y<24;y++)for(int x=0;x<24;x++)mask[y*24+x]=x<9||x>14||y<9||y>14;strip.setSelectionMask(24,24,mask);}
        d.setShape(stage==6?VolumeDefinition.Shape.CONE:VolumeDefinition.Shape.CYLINDER);d.setLightingEnabled(stage==4||stage==5);d.setLight(0,0,1);
        if(stage==4||stage==5){var normal=new BufferedImage(96,24,BufferedImage.TYPE_INT_ARGB);for(int y=0;y<24;y++)for(int x=0;x<96;x++)normal.setRGB(x,y,stage==4?0xff8080ff:0xff008080);var path=Path.of("target/surface-normal.png").toAbsolutePath();ImageIO.write(normal,"png",path.toFile());strip.layer(Strip3DDefinition.Surface.SIDE).setNormalPath(path.toString());view.invalidate();}
    }
    public void simpleUpdate(float tpf){if(done.getCount()>0&&view!=null)view.update(d,Vector3f.ZERO,new Quaternion(),Vector3f.UNIT_XYZ,cam,true,false);}
    public void simpleRender(RenderManager rm){if(done.getCount()==0||++frame%5!=0)return;try{
        var b=BufferUtils.createByteBuffer(720*720*4);var image=new BufferedImage(720,720,BufferedImage.TYPE_INT_RGB);renderer.readFrameBuffer(null,b);
        for(int y=0;y<720;y++)for(int x=0;x<720;x++){int p=(y*720+x)*4;image.setRGB(x,719-y,(b.get(p)&255)<<16|(b.get(p+1)&255)<<8|b.get(p+2)&255);}shots[stage]=image;
        if(view.getError()!=null)throw new AssertionError(view.getError());
        if(stage==1&&count(image,0xff17121c)<=count(shots[0],0xff17121c))throw new AssertionError("Outline did not appear");
        if((stage==2||stage==3)&&image.getRGB(360,360)!=image.getRGB(0,0))throw new AssertionError("Mask centre must stay transparent");
        if(stage==3&&count(image,0xff17121c)<=count(shots[1],0xff17121c))throw new AssertionError("Mask hole lost its outline");
        if(stage==5&&brightness(shots[4])<=brightness(image)*1.4)throw new AssertionError("Normal refresh did not change lighting");
        if(stage<7){stage++;setup();return;}
        var sheet=new BufferedImage(1440,2880,BufferedImage.TYPE_INT_RGB);var g=sheet.createGraphics();String[] labels={"SURFACE / NO OUTLINE","SURFACE / OUTLINE","MASK HOLE","MASK HOLE + OUTLINE","NORMAL +Z","NORMAL -X","CONE","TOP"};for(int i=0;i<8;i++){g.drawImage(shots[i],i%2*720,i/2*720,null);g.setColor(java.awt.Color.WHITE);g.drawString(labels[i],i%2*720+16,i/2*720+24);}g.dispose();ImageIO.write(sheet,"png",Path.of("target/surface-appearance.png").toFile());done.countDown();
    }catch(Throwable e){failure=e;done.countDown();}}
    private static int count(BufferedImage image,int color){int count=0;for(int y=0;y<720;y++)for(int x=0;x<720;x++)if(image.getRGB(x,y)==color)count++;return count;}
    private static long brightness(BufferedImage image){long value=0;int bg=image.getRGB(0,0);for(int y=0;y<720;y++)for(int x=0;x<720;x++){int c=image.getRGB(x,y);if(c!=bg)value+=(c>>16&255)+(c>>8&255)+(c&255);}return value;}
    public void handleError(String message,Throwable e){failure=e;done.countDown();}
}
