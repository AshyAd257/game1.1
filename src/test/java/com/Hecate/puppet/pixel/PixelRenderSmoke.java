package com.Hecate.puppet.pixel;

import com.Hecate.puppet.volume.*;
import com.jme3.app.SimpleApplication;
import com.jme3.math.*;
import com.jme3.renderer.RenderManager;
import com.jme3.system.*;
import com.jme3.util.BufferUtils;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.*;
import java.nio.file.*;
import java.util.concurrent.*;

/** Real GPU shader + depth/alpha test, and 10,000 pose updates without capture/texture allocation. */
public final class PixelRenderSmoke extends SimpleApplication {
    @Override public void handleError(String message,Throwable t){failure=t==null?new AssertionError(message):t;done.countDown();}
    final CountDownLatch done=new CountDownLatch(1);Throwable failure;int frames;BakedPixelView front,back;VolumeDefinition a,b;long builds,bytes;int addedX,addedY;final BufferedImage[] shots=new BufferedImage[5];
    public static void main(String[] args)throws Exception{
        var app=new PixelRenderSmoke();var settings=new AppSettings(true);settings.setResolution(640,480);settings.setSamples(0);settings.setAudioRenderer(null);settings.setUseInput(true);app.setSettings(settings);app.setShowSettings(false);app.start(JmeContext.Type.OffscreenSurface);
        boolean ok=app.done.await(45,TimeUnit.SECONDS);if(!ok||app.failure!=null){if(app.failure!=null)app.failure.printStackTrace();app.stop(false);System.exit(1);}app.stop(true);System.out.println("GPU pixel smoke passed: cached pose updates, transparency, per-pixel depth, priority, shared screen grid.");System.exit(0);
    }
    @Override public void simpleInitApp(){
        try{
            setDisplayFps(false);setDisplayStatView(false);cam.setLocation(new Vector3f(0,0,4));cam.lookAt(Vector3f.ZERO,Vector3f.UNIT_Y);cam.setParallelProjection(true);cam.setFrustum(.1f,100,-1.4f,1.4f,1.05f,-1.05f);viewPort.setBackgroundColor(new ColorRGBA(.08f,.1f,.14f,1));
            Path dir=Path.of("target/pixel-smoke");Files.createDirectories(dir);
            BufferedImage red=new BufferedImage(96,24,BufferedImage.TYPE_INT_ARGB),blue=new BufferedImage(96,24,BufferedImage.TYPE_INT_ARGB);
            for(int y=0;y<24;y++)for(int x=0;x<96;x++){red.setRGB(x,y,x==12?0:0xffff3030);blue.setRGB(x,y,0xff3060ff);}ImageIO.write(red,"png",dir.resolve("red.png").toFile());ImageIO.write(blue,"png",dir.resolve("blue.png").toFile());
            a=definition(dir.resolve("red.png").toAbsolutePath().toString());b=definition(dir.resolve("blue.png").toAbsolutePath().toString());front=new BakedPixelView(assetManager,rootNode,"Front");back=new BakedPixelView(assetManager,rootNode,"Back");front.prepare(a);back.prepare(b);
            if(front.error()!=null||back.error()!=null)throw new AssertionError(front.error()+" / "+back.error());
            builds=PixelAtlasCache.SHARED.builds();bytes=PixelAtlasCache.SHARED.bytes();
            for(int i=0;i<10000;i++){Quaternion pose=new Quaternion().fromAngles((float)Math.sin(i)*.4f,i*.003f,(float)Math.cos(i)*.2f);front.update(a,new Vector3f(),pose,Vector3f.UNIT_XYZ,cam,true,false);}
            if(PixelAtlasCache.SHARED.builds()!=builds||PixelAtlasCache.SHARED.bytes()!=bytes)throw new AssertionError("Animation generated more captures");
        }catch(Throwable t){failure=t;done.countDown();}
    }
    static VolumeDefinition definition(String path){var d=new VolumeDefinition();d.setShape(VolumeDefinition.Shape.BOX);d.setDimensions(25,25,25);d.setPixelsPerUnit(25);d.getStrip3D().setEnabled(true);d.getStrip3D().setOutlineEnabled(false);d.getBakedPixels().setEnabled(true);for(var s:Strip3DDefinition.Surface.values())d.getStrip3D().layer(s).setColorPath(path);return d;}
    @Override public void simpleUpdate(float tpf){if(front==null)return;int stage=frames/4;Vector3f pos=stage==1?new Vector3f(0,0,-.5f):stage==2?Vector3f.ZERO:new Vector3f(0,0,.5f);if(stage==2){front.setPriority(0);back.setPriority(100);}front.update(a,pos,new Quaternion(),Vector3f.UNIT_XYZ,cam,true,false);back.update(b,Vector3f.ZERO,new Quaternion(),Vector3f.UNIT_XYZ,cam,true,false);if(stage==3)front.update(a,new Vector3f(-.35f,.2f,.5f),new Quaternion().fromAngles(.4f,.65f,.8f),Vector3f.UNIT_XYZ,cam,true,false);}
    @Override public void simpleRender(RenderManager manager){if(failure!=null||front==null)return;try{frames++;if(frames%4==0&&frames<=20){int stage=frames/4-1;ByteBuffer data=BufferUtils.createByteBuffer(640*480*4);renderer.readFrameBuffer(null,data);BufferedImage image=new BufferedImage(640,480,BufferedImage.TYPE_INT_RGB);for(int y=0;y<480;y++)for(int x=0;x<640;x++){int i=(y*640+x)*4;image.setRGB(x,479-y,(data.get(i)&255)<<16|(data.get(i+1)&255)<<8|(data.get(i+2)&255));}shots[stage]=image;ImageIO.write(image,"png",Path.of("target/pixel-smoke/view-"+stage+".png").toFile());
            if(stage==0){int hole=image.getRGB(320,240)&0xffffff,solid=image.getRGB(340,240)&0xffffff;if((hole&255)<((hole>>16)&255))throw new AssertionError("Transparent strip pixel hides back part");if(((solid>>16)&255)<(solid&255))throw new AssertionError("Near pixels fail depth");}
            if(stage==1){int color=image.getRGB(340,240)&0xffffff;if((color&255)<((color>>16)&255))throw new AssertionError("Far part covers near part");}
            if(stage==2){int color=image.getRGB(340,240)&0xffffff;if((color&255)<((color>>16)&255))throw new AssertionError("Coplanar joint priority failed");}
            if(stage==3){
                a.getBakedPixels().setPalette(java.util.List.of(new BakedPixelDefinition.PaletteEntry("衣服",0xff3030,0x30ff30)));
                var atlas=front.atlas();int n=atlas.size();boolean[] mask=new boolean[n*n];for(int yy=0;yy<n;yy++)for(int xx=0;xx<n;xx++)mask[yy*n+xx]=atlas.occupied(20,xx,yy);int last=-1;for(int xx=0;xx<n;xx++)if(atlas.occupied(20,xx,n/2))last=xx;int cell=last+1;mask[(n/2)*n+cell]=true;var screen=cam.getScreenCoordinates(new Vector3f((cell+.5f-n*.5f)/atlas.pixelsPerUnit(),-.5f/atlas.pixelsPerUnit(),.5f));addedX=(int)Math.floor(screen.x);addedY=479-(int)Math.floor(screen.y);a.getBakedPixels().setEdit(20,new BakedPixelDefinition.MaskEdit(n,mask,atlas.key()));front.prepare(a);if(front.error()!=null)throw new AssertionError(front.error());
            }
            if(stage==4){int green=image.getRGB(340,240)&0xffffff,added=image.getRGB(addedX,addedY)&0xffffff;if(((green>>8)&255)<((green>>16)&255))throw new AssertionError("GPU palette remap failed");if(((added>>8)&255)<100||((added>>8)&255)<((added>>16)&255))throw new AssertionError("Added silhouette pixel missing: "+Integer.toHexString(added));if(PixelAtlasCache.SHARED.builds()!=builds)throw new AssertionError("Colour, mask, camera or pose recaptured");front.cleanup();back.cleanup();done.countDown();}
        }}catch(Throwable t){failure=t;done.countDown();}}
}
