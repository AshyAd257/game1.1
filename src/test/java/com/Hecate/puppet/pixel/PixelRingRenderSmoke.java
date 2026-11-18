package com.Hecate.puppet.pixel;

import com.Hecate.puppet.volume.*;
import com.jme3.app.SimpleApplication;
import com.jme3.math.*;
import com.jme3.renderer.RenderManager;
import com.jme3.system.*;
import com.jme3.util.BufferUtils;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.util.concurrent.*;
import javax.imageio.ImageIO;

/** Compare GPU texture phases with the editor reference at every 18 degrees and at the seam. */
public final class PixelRingRenderSmoke extends SimpleApplication {
    final CountDownLatch done=new CountDownLatch(1);Throwable failure;
    BakedPixelView part;VolumeDefinition definition;BufferedImage ring;int frame,step;long builds,bytes;int first;
    public static void main(String[] args)throws Exception{
        var app=new PixelRingRenderSmoke();var settings=new AppSettings(true);settings.setResolution(640,480);settings.setSamples(0);settings.setAudioRenderer(null);settings.setUseInput(true);
        app.setSettings(settings);app.setShowSettings(false);app.start(JmeContext.Type.OffscreenSurface);
        if(!app.done.await(45,TimeUnit.SECONDS)||app.failure!=null){if(app.failure!=null)app.failure.printStackTrace();app.stop(false);System.exit(1);}
        app.stop(true);System.out.println("GPU ring phases passed: all 20 directions match CPU preview, full-turn seam closes, no recapture.");System.exit(0);
    }
    @Override public void handleError(String message,Throwable error){failure=error==null?new AssertionError(message):error;done.countDown();}
    @Override public void simpleInitApp(){try{
        setDisplayFps(false);setDisplayStatView(false);cam.setParallelProjection(true);cam.setFrustum(.1f,100,-2.2f,2.2f,1.65f,-1.65f);
        definition=new VolumeDefinition();definition.setShape(VolumeDefinition.Shape.BOX);definition.setDimensions(50,50,50);definition.setPixelsPerUnit(25);definition.getBakedPixels().setEnabled(true);definition.getStrip3D().setEnabled(true);definition.getStrip3D().setOutlineEnabled(false);
        PixelRingTexture.Requirement required;try(var lease=PixelAtlasCache.SHARED.acquire(definition)){required=PixelRingTexture.requirement(lease.atlas());}
        if(required.width()!=20||required.columnsPerStep()!=1)throw new AssertionError("Unexpected cube texture size: "+required);
        ring=new BufferedImage(37,required.recommendedHeight(),BufferedImage.TYPE_INT_ARGB);int[] colors={0xffff3030,0xff30ff30,0xff3030ff};
        for(int y=0;y<ring.getHeight();y++)for(int x=0;x<ring.getWidth();x++)ring.setRGB(x,y,colors[(x/required.columnsPerStep())%3]);
        Path path=Path.of("target/pixel-ring-smoke/ring.png").toAbsolutePath();Files.createDirectories(path.getParent());ImageIO.write(ring,"png",path.toFile());
        for(var surface:Strip3DDefinition.Surface.values())PixelRingTexture.assign(definition,surface,path.toString(),ring);
        part=new BakedPixelView(assetManager,rootNode,"Ring");part.prepare(definition);if(part.error()!=null)throw new AssertionError(part.error());
        builds=PixelAtlasCache.SHARED.builds();bytes=PixelAtlasCache.SHARED.bytes();
    }catch(Throwable error){failure=error;done.countDown();}}
    @Override public void simpleUpdate(float delta){if(part==null)return;float yaw=step*PixelAtlas.YAW_DEGREES;cam.setLocation(PixelViewSelector.direction(yaw,0).mult(6));cam.lookAt(Vector3f.ZERO,Vector3f.UNIT_Y);part.update(definition,Vector3f.ZERO,Quaternion.IDENTITY,Vector3f.UNIT_XYZ,cam,true,false);}
    @Override public void simpleRender(RenderManager manager){if(failure!=null||part==null)return;try{
        if(++frame%3!=0)return;
        ByteBuffer pixels=BufferUtils.createByteBuffer(640*480*4);renderer.readFrameBuffer(null,pixels);int i=(240*640+320)*4;
        int actual=(pixels.get(i)&255)<<16|(pixels.get(i+1)&255)<<8|(pixels.get(i+2)&255);
        var image=PixelArtwork.selected(part.atlas(),definition,step*PixelAtlas.YAW_DEGREES,0,new BufferedImage[]{ring,ring,ring});int n=part.atlas().size();int expected=image.getRGB(n/2,n/2);
        if(dominant(actual)!=dominant(expected))throw new AssertionError("GPU and preview differ at "+step*PixelAtlas.YAW_DEGREES+" degrees: "+Integer.toHexString(actual)+" / "+Integer.toHexString(expected));
        if(step==0)first=actual;
        if(step==PixelAtlas.YAW_VIEWS){if(actual!=first)throw new AssertionError("Full-turn seam changed texture");if(PixelAtlasCache.SHARED.builds()!=builds||PixelAtlasCache.SHARED.bytes()!=bytes)throw new AssertionError("Camera turn recaptured");part.cleanup();done.countDown();}else step++;
    }catch(Throwable error){failure=error;done.countDown();}}
    static int dominant(int rgb){int r=rgb>>16&255,g=rgb>>8&255,b=rgb&255;return r>g&&r>b?0:g>r&&g>b?1:2;}
}
