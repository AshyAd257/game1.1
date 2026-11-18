package com.Hecate.puppet.pixel;

import com.Hecate.puppet.volume.*;
import com.jme3.app.SimpleApplication;
import com.jme3.math.*;
import com.jme3.renderer.RenderManager;
import com.jme3.system.*;
import com.jme3.util.BufferUtils;
import java.awt.image.BufferedImage;
import java.nio.*;
import java.nio.file.*;
import java.util.List;
import java.util.concurrent.*;
import javax.imageio.ImageIO;

/** GPU/CPU full-image comparisons across four joins, both real poles, and pole yaw phases. */
public final class PixelVerticalRenderSmoke extends SimpleApplication {
    final CountDownLatch done=new CountDownLatch(1);Throwable failure;
    BakedPixelView part;VolumeDefinition definition;BufferedImage[] sources=new BufferedImage[3];
    int frames,stage;long builds,bytes;
    static final float[] PITCHES={-90,-75,-71.5f,-69.5f,-67.5f,-65.5f,-63.5f,-55,-45,-26.5f,-24.5f,-22.5f,-20.5f,-18.5f,-10,0,10,18.5f,20.5f,22.5f,24.5f,26.5f,45,55,63.5f,65.5f,67.5f,69.5f,71.5f,75,90,90,-90,-90};
    public static void main(String[] args)throws Exception{
        var app=new PixelVerticalRenderSmoke();var settings=new AppSettings(true);settings.setResolution(400,400);settings.setSamples(0);settings.setAudioRenderer(null);settings.setUseInput(true);settings.setGammaCorrection(false);
        app.setSettings(settings);app.setShowSettings(false);app.start(JmeContext.Type.OffscreenSurface);
        if(!app.done.await(55,TimeUnit.SECONDS)||app.failure!=null){if(app.failure!=null)app.failure.printStackTrace();app.stop(false);System.exit(1);}
        app.stop(true);System.out.println("Vertical row sampling GPU checks passed: 34 full-image comparisons, half/full shifts, all joins, row priority, transparent row picking, no recapture.");System.exit(0);
    }
    @Override public void handleError(String message,Throwable error){failure=error==null?new AssertionError(message):error;done.countDown();}
    @Override public void simpleInitApp(){try{
        setDisplayFps(false);setDisplayStatView(false);cam.setParallelProjection(true);cam.setFrustum(.1f,100,-8,8,8,-8);viewPort.setBackgroundColor(ColorRGBA.Black);
        definition=PixelPhotoAndRingTest.imported(new com.jme3.scene.shape.Box(1,1,1));definition.getBakedPixels().setPerspectiveStrength(.1f);definition.getBakedPixels().setVerticalTextureEnabled(true);definition.getBakedPixels().setVerticalFrameHeight(50);definition.getStrip3D().setEnabled(true);definition.getStrip3D().setOutlineEnabled(false);
        Path dir=Path.of("target/vertical-render-smoke").toAbsolutePath();Files.createDirectories(dir);
        PixelRingTexture.Requirement required;try(var lease=PixelAtlasCache.SHARED.acquire(definition)){required=PixelRingTexture.requirement(lease.atlas());}
        int[] colors={0xffff3030,0xff30ff30,0xff3030ff};
        for(int i=0;i<3;i++){
            var image=new BufferedImage(37,150,BufferedImage.TYPE_INT_ARGB);
            for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++)image.setRGB(x,y,y==77?0:0xff000000|((y<50?220:y<100?80:160)<<16)|((y%50*4)<<8)|(30+(x*5+y)%200));definition.getBakedPixels().setRowImportance(i,95,150,3);
            sources[i]=image;Path p=dir.resolve("surface"+i+".png");ImageIO.write(image,"png",p.toFile());definition.getStrip3D().layer(Strip3DDefinition.Surface.values()[i]).setColorPath(p.toString());
        }
        part=new BakedPixelView(assetManager,rootNode,"FiveDirections");part.prepare(definition);if(part.error()!=null)throw new AssertionError(part.error());builds=PixelAtlasCache.SHARED.builds();bytes=PixelAtlasCache.SHARED.bytes();
    }catch(Throwable error){failure=error;done.countDown();}}
    float yaw(){return stage>=31?(stage%2==0?90:15):0;}
    @Override public void simpleUpdate(float delta){if(part==null||stage>=PITCHES.length)return;float pitch=PITCHES[stage],yaw=yaw();Vector3f right=PixelViewSelector.right(yaw),toward=PixelViewSelector.direction(yaw,pitch);cam.setLocation(toward.mult(6));cam.lookAt(Vector3f.ZERO,toward.cross(right));part.update(definition,Vector3f.ZERO,Quaternion.IDENTITY,Vector3f.UNIT_XYZ,cam,true,false);}
    @Override public void simpleRender(RenderManager manager){if(failure!=null||part==null||stage>=PITCHES.length)return;try{
        if(++frames%3!=0)return;
        ByteBuffer pixels=BufferUtils.createByteBuffer(400*400*4);renderer.readFrameBuffer(null,pixels);
        BufferedImage cpu=PixelAssemblyPreview.render(List.of(new PixelAssemblyPreview.Part(definition,part.atlas(),new Transform(),0,sources)),Vector3f.ZERO,400,yaw(),PITCHES[stage]);
        BufferedImage shot=new BufferedImage(400,400,BufferedImage.TYPE_INT_RGB);int mismatches=0,occupied=0;
        for(int y=0;y<400;y++)for(int x=0;x<400;x++){
            int i=((399-y)*400+x)*4;int actual=(pixels.get(i)&255)<<16|(pixels.get(i+1)&255)<<8|(pixels.get(i+2)&255);
            shot.setRGB(x,y,actual);int expected=cpu.getRGB(x,y)&0xffffff;if(expected!=0)occupied++;
            if(actual!=expected)mismatches++;
        }
        ImageIO.write(shot,"png",Path.of("target/vertical-render-smoke/view-"+stage+".png").toFile());
        if(occupied<200||mismatches>3){ImageIO.write(cpu,"png",Path.of("target/vertical-render-smoke/cpu-failure.png").toFile());throw new AssertionError("GPU/CPU differ at pitch "+PITCHES[stage]+" yaw "+yaw()+": "+mismatches+" cells, "+occupied+" occupied; GPU "+part.geometry().getMaterial().getParam("ViewChoice").getValue()+" / CPU "+PixelViewSelector.select(definition.getBakedPixels(),yaw(),PITCHES[stage],part.atlas().size()));}
        var hits=new com.jme3.collision.CollisionResults();part.geometry().collideWith(new Ray(cam.getWorldCoordinates(new Vector2f(200.5f,200.5f),0),cam.getDirection()),hits);
        boolean visible=(cpu.getRGB(200,199)>>>24)>=128;if(hits.size()!=(visible?1:0))throw new AssertionError("Pixel picking disagrees with row-sampled alpha at "+PITCHES[stage]);
        if(++stage==PITCHES.length){
            for(int i=0;i<10000;i++)part.update(definition,Vector3f.ZERO,new Quaternion().fromAngles(i*.001f,i*.002f,i*.003f),Vector3f.UNIT_XYZ,cam,true,false);
            if(builds!=PixelAtlasCache.SHARED.builds()||bytes!=PixelAtlasCache.SHARED.bytes())throw new AssertionError("Camera or animation recaptured model");part.cleanup();done.countDown();
        }
    }catch(Throwable error){failure=error;done.countDown();}}
}
