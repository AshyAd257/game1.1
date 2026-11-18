package com.Hecate.puppet.volume;

import com.jme3.app.SimpleApplication;
import com.jme3.math.*;
import com.jme3.renderer.RenderManager;
import com.jme3.system.*;
import com.jme3.util.BufferUtils;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import javax.imageio.ImageIO;

/** Every source texel must be either completely present or absent after silhouette rounding. */
public final class SurfaceContourRenderSmoke extends SimpleApplication {
    private static final int SIZE=768,CASES=14;
    private final CountDownLatch done=new CountDownLatch(1);private Throwable failure;
    private Strip3DView view,inner;private VolumeDefinition d,inside;private int stage,frame;
    private final ArrayList<BufferedImage> shots=new ArrayList<>();
    public SurfaceContourRenderSmoke(){super(new com.jme3.app.StatsAppState());}
    public static void main(String[] args)throws Exception{
        var app=new SurfaceContourRenderSmoke();var s=new AppSettings(true);s.setResolution(SIZE,SIZE);s.setSamples(0);s.setGammaCorrection(false);s.setAudioRenderer(null);s.setUseInput(false);app.setSettings(s);app.setShowSettings(false);app.start(JmeContext.Type.OffscreenSurface);
        boolean ok=app.done.await(40,TimeUnit.SECONDS);app.stop(false);if(!ok||app.failure!=null){if(app.failure!=null)app.failure.printStackTrace();System.exit(1);}System.out.println("Contour GPU: reproduced sub-texel cuts in fine mode; zero partial artwork texels in snapped views; widths, masks, roll, top, cones, nested front/back passed");System.exit(0);
    }
    public void simpleInitApp(){try{
        setDisplayFps(false);setDisplayStatView(false);viewPort.setBackgroundColor(ColorRGBA.Black);
        d=new VolumeDefinition();d.setDimensions(48,48,48);d.getStrip3D().setEnabled(true);d.getStrip3D().setFrameSize(12,12);d.getStrip3D().setOutlineEnabled(false);
        for(int index=0;index<2;index++){
            var image=new BufferedImage(48,12,BufferedImage.TYPE_INT_ARGB);
            for(int y=0;y<12;y++)for(int x=0;x<48;x++)image.setRGB(x,y,index==1?0xff3366ee:(x+y)%2==0?0xffdeb6a0:0xff6083c0);
            var path=Path.of("target/contour-source-"+index+".png").toAbsolutePath();ImageIO.write(image,"png",path.toFile());
            var target=index==0?d:(inside=d.copy());if(index==1)target.setDimensions(28,28,28);
            for(var face:Strip3DDefinition.Surface.values())target.getStrip3D().layer(face).setColorPath(path.toString());
        }
        view=new Strip3DView(assetManager,rootNode,"outer");inner=new Strip3DView(assetManager,rootNode,"inner");setup();
    }catch(Throwable e){failure=e;done.countDown();}}
    private void setup(){
        boolean fine=stage<8&&stage%2==0;d.setSurfaceContourSnapped(!fine);d.setShape(stage==6||stage==7?VolumeDefinition.Shape.CONE:stage==9?VolumeDefinition.Shape.FRUSTUM:VolumeDefinition.Shape.CYLINDER);
        var strip=d.getStrip3D();strip.resetSelectionMask();strip.setSampleWidth(stage==10?11:stage==11?16:12);
        if(stage==8){boolean[] mask=new boolean[144];for(int y=0;y<12;y++)for(int x=0;x<12;x++)mask[y*12+x]=x<4||x>7||y<4||y>7;strip.setSelectionMask(12,12,mask);}
        double yaw=stage==13?Math.PI:stage==2||stage==3?.6:0,pitch=stage==4||stage==5?Math.PI/2:stage==2||stage==3?.35:.08;
        cam.setParallelProjection(true);cam.setFrustum(.1f,100,-1.2f,1.2f,1.2f,-1.2f);
        if(stage==2||stage==3){cam.setParallelProjection(false);cam.setFrustumPerspective(35,1,.1f,100);}
        cam.setLocation(new Vector3f((float)(Math.sin(yaw)*Math.cos(pitch)),(float)Math.sin(pitch),(float)(Math.cos(yaw)*Math.cos(pitch))).multLocal(4));cam.lookAt(Vector3f.ZERO,pitch>1.56?Vector3f.UNIT_Z:Vector3f.UNIT_Y);
    }
    public void simpleUpdate(float tpf){if(done.getCount()==0||view==null)return;
        view.update(d,new Vector3f(.013f,.019f,0),stage==9?new Quaternion().fromAngles(.3f,.4f,.43f):new Quaternion(),Vector3f.UNIT_XYZ,cam,true,false);
        inner.update(inside,new Vector3f(.08f,0,.1f),new Quaternion(),Vector3f.UNIT_XYZ,cam,stage>=12,false);
    }
    public void simpleRender(RenderManager rm){if(done.getCount()==0||++frame%4!=0)return;try{
        var bytes=BufferUtils.createByteBuffer(SIZE*SIZE*4);renderer.readFrameBuffer(null,bytes);var image=new BufferedImage(SIZE,SIZE,BufferedImage.TYPE_INT_RGB);
        for(int y=0;y<SIZE;y++)for(int x=0;x<SIZE;x++){int p=(y*SIZE+x)*4;image.setRGB(x,SIZE-1-y,(bytes.get(p)&255)<<16|(bytes.get(p+1)&255)<<8|bytes.get(p+2)&255);}
        int grid=SurfaceProjection.pixelStep(cam),present=0,blue=0;
        var texture=(com.jme3.texture.Texture)view.getGeometry().getMaterial().getParam("ColorMap").getValue();int w=texture.getImage().getWidth(),h=texture.getImage().getHeight();
        int[] states=new int[w*h];Matrix3f toArt=view.getSurfaceProjection().screenToArt();
        for(int y=0;y<SIZE;y+=grid)for(int x=0;x<SIZE;x+=grid){
            int rgb=image.getRGB(x,SIZE-1-y);boolean filled=rgb!=0xff000000;if(filled)present++;if(rgb==0xff3366ee)blue++;
            Vector3f uv=toArt.mult(new Vector3f((x+grid*.5f)/SIZE*2-1,(y+grid*.5f)/SIZE*2-1,1));
            int tx=(int)Math.floor(uv.x/uv.z*w+.0001f),ty=(int)Math.floor(uv.y/uv.z*h+.0001f);
            if(tx>=0&&tx<w&&ty>=0&&ty<h)states[ty*w+tx]|=filled?1:2;
        }
        int partial=0;for(int state:states)if(state==3)partial++;
        ImageIO.write(image,"png",Path.of("target/contour-case-"+stage+".png").toFile());
        if(present<1000)throw new AssertionError("Missing artwork at "+stage);
        if(d.isSurfaceContourSnapped()&&partial!=0)throw new AssertionError("Stage "+stage+": "+partial+" source cells are cut into small steps");
        if(!d.isSurfaceContourSnapped()&&partial==0)throw new AssertionError("Did not reproduce original silhouette cuts at "+stage);
        if(stage>=12&&blue!=0)throw new AssertionError("Inner part leaks through solid enclosing volume");
        System.out.println("Contour stage "+stage+" partial source cells="+partial);shots.add(image);
        if(++stage<CASES){setup();return;}
        var sheet=new BufferedImage(1536,3072,BufferedImage.TYPE_INT_RGB);var g=sheet.createGraphics();for(int i=0;i<8;i++){g.drawImage(shots.get(i),i%2*768,i/2*768,null);g.setColor(java.awt.Color.WHITE);g.drawString(i%2==0?"BEFORE: FINE CONTOUR":"AFTER: WHOLE ART PIXELS",i%2*768+16,i/2*768+24);}g.dispose();ImageIO.write(sheet,"png",Path.of("target/contour-before-after.png").toFile());done.countDown();
    }catch(Throwable e){failure=e;done.countDown();}}
    public void handleError(String message,Throwable e){failure=e;done.countDown();}
}
