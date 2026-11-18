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

/** Regression: 9, 13, 16 source columns must occupy exactly the same world/screen width. */
public final class SelectionWidthRenderSmoke extends SimpleApplication {
    public SelectionWidthRenderSmoke(){super(new com.jme3.app.StatsAppState());}
    private final CountDownLatch done=new CountDownLatch(1);private volatile Throwable failure;
    private final int[] widths={9,13,16,9};private int frame,stage;
    private final BufferedImage[] shots=new BufferedImage[4];private BufferedImage source;private VolumeDefinition d;private Strip3DView view;
    public static void main(String[] args)throws Exception{
        var app=new SelectionWidthRenderSmoke();var settings=new AppSettings(true);settings.setResolution(512,512);settings.setAudioRenderer(null);settings.setUseInput(false);settings.setSamples(0);settings.setGammaCorrection(false);app.setSettings(settings);app.setShowSettings(false);app.start(JmeContext.Type.OffscreenSurface);
        boolean ok=app.done.await(30,TimeUnit.SECONDS);app.stop(false);if(!ok||app.failure!=null){if(app.failure!=null)app.failure.printStackTrace();System.exit(1);}System.out.println("9/13/16 columns rendered at identical 256-pixel width and centre, exact source colors, live edits and restored width passed");System.exit(0);
    }
    public void simpleInitApp(){try{
        setDisplayFps(false);setDisplayStatView(false);cam.setParallelProjection(true);cam.setFrustum(.1f,100,-1,1,1,-1);cam.setLocation(new Vector3f(0,0,5));cam.lookAt(Vector3f.ZERO,Vector3f.UNIT_Y);viewPort.setBackgroundColor(ColorRGBA.Black);
        source=new BufferedImage(36,9,BufferedImage.TYPE_INT_ARGB);for(int y=0;y<9;y++)for(int x=0;x<36;x++)source.setRGB(x,y,0xff000000|((x*67)%200+40)<<16|((x*37)%200+40)<<8|((x*13)%200+40));
        Path path=Path.of("target/selection-width-regression-source.png").toAbsolutePath();ImageIO.write(source,"png",path.toFile());
        d=new VolumeDefinition();d.setDimensions(32,32,32);d.setPixelsPerUnit(32);d.getStrip3D().setEnabled(true);d.getStrip3D().setFrameSize(9,9);d.getStrip3D().setOutlineEnabled(false);d.getStrip3D().setHorizontal(Strip3DDefinition.Sampling.SLIDE);d.getStrip3D().setVertical(Strip3DDefinition.Sampling.SLIDE);d.getStrip3D().layer(Strip3DDefinition.Surface.SIDE).setColorPath(path.toString());
        view=new Strip3DView(assetManager,rootNode,"width-regression");d.getStrip3D().setSampleWidth(widths[0]);
    }catch(Throwable e){failure=e;done.countDown();}}
    public void simpleUpdate(float tpf){if(view!=null&&done.getCount()>0)view.update(d,Vector3f.ZERO,new Quaternion(),Vector3f.UNIT_XYZ,cam,true,false);}
    public void simpleRender(RenderManager manager){if(done.getCount()==0||++frame%8!=0)return;try{
        var bytes=BufferUtils.createByteBuffer(512*512*4);renderer.readFrameBuffer(null,bytes);var image=new BufferedImage(512,512,BufferedImage.TYPE_INT_RGB);
        for(int y=0;y<512;y++)for(int x=0;x<512;x++){int p=(y*512+x)*4;image.setRGB(x,511-y,(bytes.get(p)&255)<<16|(bytes.get(p+1)&255)<<8|bytes.get(p+2)&255);}
        shots[stage]=image;int first=512,last=-1,runs=0,previous=0xff000000;
        for(int x=0;x<512;x++){int rgb=image.getRGB(x,256);if(rgb!=0xff000000){first=Math.min(first,x);last=x;if(rgb!=previous)runs++;}previous=rgb;}
        if(first!=128||last!=383||runs!=widths[stage])throw new AssertionError("Width "+widths[stage]+": bounds "+first+".."+last+", columns "+runs);
        int start=Math.floorMod((int)Math.round(18-widths[stage]/2.0),36);
        for(int col=0;col<widths[stage];col++){int x=(int)Math.floor(128+(col+.5)*256/widths[stage]);if(image.getRGB(x,256)!=source.getRGB((start+col)%36,4))throw new AssertionError("Wrong source column "+col);}
        if(d.getWidthPx()!=32||d.getHeightPx()!=32||d.getDepthPx()!=32)throw new AssertionError("Changed physical dimensions");
        if(++stage<widths.length){d.getStrip3D().setSampleWidth(widths[stage]);return;}
        var sheet=new BufferedImage(1536,512,BufferedImage.TYPE_INT_RGB);var g=sheet.createGraphics();for(int i=0;i<3;i++){g.drawImage(shots[i],i*512,0,null);g.setColor(java.awt.Color.WHITE);g.drawString(widths[i]+" source columns / same body width",i*512+140,50);}g.dispose();ImageIO.write(sheet,"png",Path.of("target/selection-width-fixed-body.png").toFile());view.cleanup();done.countDown();
    }catch(Throwable e){failure=e;done.countDown();}}
    public void handleError(String message,Throwable e){failure=e;done.countDown();}
}
