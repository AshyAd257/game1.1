package com.Hecate.puppet.volume;

import com.jme3.app.SimpleApplication;
import com.jme3.math.*;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Quad;
import com.jme3.system.*;
import com.jme3.texture.*;
import com.jme3.texture.plugins.AWTLoader;
import com.jme3.util.BufferUtils;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.util.concurrent.*;
import javax.imageio.ImageIO;

/** Optional GPU smoke check, launched manually; no visible window or game assets needed. */
public final class VolumeRenderSmoke extends SimpleApplication {
    private final CountDownLatch finished=new CountDownLatch(1);
    private volatile Throwable failure;
    private int frames;
    public static void main(String[] args)throws Exception {
        VolumeRenderSmoke app=new VolumeRenderSmoke();AppSettings settings=new AppSettings(true);
        settings.setResolution(512,256);settings.setAudioRenderer(null);settings.setUseInput(false);settings.setSamples(0);
        app.setSettings(settings);app.setShowSettings(false);app.setPauseOnLostFocus(false);
        app.start(JmeContext.Type.OffscreenSurface);
        if(!app.finished.await(40,TimeUnit.SECONDS)){app.stop();throw new AssertionError("GPU smoke check timed out");}
        app.stop(false);
        if(app.failure!=null){app.failure.printStackTrace();System.exit(1);}
        System.out.println("GPU shader smoke passed: target/volume-render-smoke.png");
        System.exit(0);
    }
    @Override public void simpleInitApp() {
        setDisplayStatView(false);setDisplayFps(false);viewPort.setBackgroundColor(new ColorRGBA(.06f,.08f,.12f,1));
        cam.setParallelProjection(true);cam.setFrustum(1,20,-2,2,1,-1);cam.setLocation(new Vector3f(0,0,5));cam.lookAt(Vector3f.ZERO,Vector3f.UNIT_Y);
        try {
            BufferedImage color=new BufferedImage(32,32,BufferedImage.TYPE_INT_ARGB), normal=new BufferedImage(32,32,BufferedImage.TYPE_INT_ARGB);
            for(int y=0;y<32;y++)for(int x=0;x<32;x++){
                color.setRGB(x,y,0xffd29458);float nx=(x-15.5f)/16;float nz=(float)Math.sqrt(1-nx*nx);
                normal.setRGB(x,y,0xff000000|((int)((nx+1)*127.5f)<<16)|(128<<8)|(int)((nz+1)*127.5f));
            }
            Path normalPath=Path.of("target/volume-smoke-normal.png").toAbsolutePath();ImageIO.write(normal,"png",normalPath.toFile());
            for(int i=0;i<2;i++) {
                Geometry g=new Geometry("volume"+i,new Quad(1.4f,1.4f));g.setLocalTranslation(-1.7f+i*2,-.7f,0);
                VolumeDefinition d=new VolumeDefinition();d.setNormalTexturePath(normalPath.toString());d.setLightingEnabled(i==1);d.setLight(-45,0,1);
                VolumeAppearance material=new VolumeAppearance(assetManager);material.update(g,new Texture2D(new AWTLoader().load(color,true)),d,new Quaternion(),false);
                rootNode.attachChild(g);
            }
        }catch(Exception e){handleError("Setup",e);}
    }
    @Override public void simpleRender(RenderManager manager) {
        if(++frames!=8)return;
        try {
            ByteBuffer bytes=BufferUtils.createByteBuffer(512*256*4);renderer.readFrameBuffer(null,bytes);
            BufferedImage image=new BufferedImage(512,256,BufferedImage.TYPE_INT_ARGB);
            for(int y=0;y<256;y++)for(int x=0;x<512;x++){int p=(y*512+x)*4;image.setRGB(x,255-y,0xff000000|((bytes.get(p)&255)<<16)|((bytes.get(p+1)&255)<<8)|(bytes.get(p+2)&255));}
            ImageIO.write(image,"png",Path.of("target/volume-render-smoke.png").toFile());
            // Both quads must render, with a uniform unlit side and a varying normal-lit side.
            int a=image.getRGB(80,128),b=image.getRGB(160,128),c=image.getRGB(335,128),d=image.getRGB(415,128);
            if(a!=b || c==d || (a&0xffffff)==(image.getRGB(1,1)&0xffffff))throw new AssertionError("Unexpected shader output");
        }catch(Throwable e){failure=e;}finally{finished.countDown();}
    }
    @Override public void handleError(String message,Throwable e){failure=e;finished.countDown();}
}
