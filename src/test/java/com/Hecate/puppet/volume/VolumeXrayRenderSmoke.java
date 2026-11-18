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

/** Enclosed models must remain legible in X-ray, with no leaked helpers in other modes. */
public final class VolumeXrayRenderSmoke extends SimpleApplication {
    private final CountDownLatch done=new CountDownLatch(1);private Throwable failure;private int frame,stage;
    private final VolumeReferenceView[] views=new VolumeReferenceView[3];private final VolumeDefinition[] defs=new VolumeDefinition[3];
    private final ArrayList<BufferedImage> shots=new ArrayList<>();
    public VolumeXrayRenderSmoke(){super(new com.jme3.app.StatsAppState());}
    public static void main(String[] args)throws Exception{
        var app=new VolumeXrayRenderSmoke();var s=new AppSettings(true);s.setResolution(640,640);s.setAudioRenderer(null);s.setUseInput(false);s.setSamples(0);s.setGammaCorrection(false);app.setSettings(s);app.setShowSettings(false);app.start(JmeContext.Type.OffscreenSurface);
        boolean ok=app.done.await(30,TimeUnit.SECONDS);app.stop(false);if(!ok||app.failure!=null){if(app.failure!=null)app.failure.printStackTrace();System.exit(1);}System.out.println("X-ray GPU passed: enclosed inner model visible, opaque restore exact, artwork mode clean, top view, rotated/mirrored shapes, hidden cleanup");System.exit(0);
    }
    public void simpleInitApp(){
        setDisplayFps(false);setDisplayStatView(false);viewPort.setBackgroundColor(new ColorRGBA(.08f,.1f,.14f,1));cam.setParallelProjection(true);cam.setFrustum(.1f,100,-1.6f,1.6f,1.6f,-1.6f);
        for(int i=0;i<3;i++){views[i]=new VolumeReferenceView(assetManager,rootNode,new String[]{"outer","middle","inner"}[i]);defs[i]=new VolumeDefinition();int size=64-i*18;defs[i].setDimensions(size,size,size);}
    }
    public void simpleUpdate(float tpf){if(done.getCount()==0)return;
        cam.setLocation(stage==4?new Vector3f(0,5,0):stage>=5?new Vector3f(3,2,5):new Vector3f(0,.8f,5));cam.lookAt(Vector3f.ZERO,stage==4?Vector3f.UNIT_Z:Vector3f.UNIT_Y);
        for(int i=0;i<3;i++){
            defs[i].setShape(stage>=5?(i==0?VolumeDefinition.Shape.BOX:i==1?VolumeDefinition.Shape.FRUSTUM:VolumeDefinition.Shape.CONE):VolumeDefinition.Shape.CYLINDER);
            views[i].setXray(stage!=0&&stage!=2);views[i].setSelected(i==2&&stage!=6);
            views[i].update(defs[i],Vector3f.ZERO,stage>=5?new Quaternion().fromAngles(.2f,.3f,.4f):new Quaternion(),stage>=5?new Vector3f(-1,1,.85f):Vector3f.UNIT_XYZ,stage==3?VolumeReferenceView.Mode.APPEARANCE:VolumeReferenceView.Mode.SOLID,stage!=7);
        }
    }
    public void simpleRender(RenderManager rm){if(done.getCount()==0||++frame%5!=0)return;try{
        var bytes=BufferUtils.createByteBuffer(640*640*4);renderer.readFrameBuffer(null,bytes);var image=new BufferedImage(640,640,BufferedImage.TYPE_INT_RGB);int cyan=0,drawn=0;
        for(int y=0;y<640;y++)for(int x=0;x<640;x++){int p=(y*640+x)*4,r=bytes.get(p)&255,g=bytes.get(p+1)&255,b=bytes.get(p+2)&255;image.setRGB(x,639-y,r<<16|g<<8|b);if(r<100&&g>200&&b>230)cyan++;}
        int bg=image.getRGB(0,0);for(int y=0;y<640;y++)for(int x=0;x<640;x++)if(image.getRGB(x,y)!=bg)drawn++;
        if((stage==0||stage==2)&&cyan!=0)throw new AssertionError("Solid outer should conceal cyan inner");
        if((stage==1||stage==4||stage==5)&&cyan<100)throw new AssertionError("Internal edges not visible in X-ray: "+cyan);
        if((stage==3||stage==7)&&drawn!=0)throw new AssertionError("Hidden references left stray pixels");
        if(stage==2&&!Arrays.equals(shots.get(0).getRGB(0,0,640,640,null,0,640),image.getRGB(0,0,640,640,null,0,640)))throw new AssertionError("Opaque mode did not restore exactly");
        shots.add(image);if(++stage<8)return;
        var sheet=new BufferedImage(2560,1280,BufferedImage.TYPE_INT_RGB);var g=sheet.createGraphics();String[] labels={"SOLID","X-RAY: INNER SELECTED","SOLID RESTORED","ARTWORK MODE","TOP X-RAY","ROTATED / MIRRORED","PART COLORS","HIDDEN"};for(int i=0;i<8;i++){g.drawImage(shots.get(i),i%4*640,i/4*640,null);g.setColor(java.awt.Color.WHITE);g.drawString(labels[i],i%4*640+16,i/4*640+24);}g.dispose();ImageIO.write(sheet,"png",Path.of("target/volume-xray-regression.png").toFile());done.countDown();
    }catch(Throwable e){failure=e;done.countDown();}}
    public void handleError(String message,Throwable e){failure=e;done.countDown();}
}
