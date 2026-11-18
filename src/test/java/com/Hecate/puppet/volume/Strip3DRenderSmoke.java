package com.Hecate.puppet.volume;
import com.jme3.app.SimpleApplication;
import com.jme3.math.*;
import com.jme3.renderer.RenderManager;
import com.jme3.system.*;
import com.jme3.util.BufferUtils;
import java.awt.image.BufferedImage;
import java.nio.*;
import java.nio.file.*;
import java.util.concurrent.*;
import javax.imageio.ImageIO;

/** GPU billboard output must preserve the prototype's exact source pixels, including near poles. */
public final class Strip3DRenderSmoke extends SimpleApplication {
    public Strip3DRenderSmoke(){super(new com.jme3.app.StatsAppState());}
    private final CountDownLatch closed=new CountDownLatch(1),done=new CountDownLatch(1);private volatile Throwable failure;
    private Strip3DView view;private VolumeDefinition definition;private int frames,stage;private final BufferedImage[] shots=new BufferedImage[17];
    private final double[][] angles={{0,0},{45,30},{0,90},{0,-90},{45,45},{45,45},{45,45},{45,45},{0,0},{0,0},{45,30},{0,0},{0,0},{179,0},{0,0},{45,30},{-37,-45}};
    public static void main(String[] args)throws Exception{
        Strip3DRenderSmoke app=new Strip3DRenderSmoke();AppSettings s=new AppSettings(true);s.setResolution(512,512);s.setAudioRenderer(null);s.setUseInput(false);s.setSamples(0);s.setGammaCorrection(false);app.setSettings(s);app.setShowSettings(false);app.setPauseOnLostFocus(false);app.start(JmeContext.Type.OffscreenSurface);
        boolean ok=app.done.await(50,TimeUnit.SECONDS);app.stop(false);app.closed.await(3,TimeUnit.SECONDS);if(!ok||app.failure!=null){if(app.failure!=null)app.failure.printStackTrace();else System.err.println("Timeout");System.exit(1);}System.out.println("Prototype GPU checks passed: exact pixels in 8 views/modes, normal refresh, custom selection shape, outline on/off, selected artwork RGB, perspective visibility, live sample widths 1/37/96/24, wrapping and custom masks");System.exit(0);
    }
    @Override public void destroy(){try{super.destroy();}catch(Throwable e){failure=e;}finally{closed.countDown();}}
    public void simpleInitApp(){
        Thread.currentThread().setUncaughtExceptionHandler((t,e)->{e.printStackTrace();failure=e;done.countDown();closed.countDown();});setDisplayFps(false);setDisplayStatView(false);viewPort.setBackgroundColor(new ColorRGBA(.035f,.05f,.075f,1));cam.setParallelProjection(true);cam.setFrustum(.1f,100,-2,2,2,-2);
        definition=new VolumeDefinition();definition.setSurfaceDepthEnabled(false);definition.setDimensions(24,24,24);definition.getStrip3D().setEnabled(true);for(var face:Strip3DDefinition.Surface.values())definition.getStrip3D().layer(face).setColorPath("textures/TriStripPrototype/"+face.name().toLowerCase(java.util.Locale.ROOT)+".png");view=new Strip3DView(assetManager,rootNode,"test");setStage(0);
    }
    private void setStage(int n)throws RuntimeException{
        stage=n;var d=definition.getStrip3D();d.setOutlineEnabled(n!=6);d.setHorizontal(n>=4&&n<=7?Strip3DDefinition.Sampling.PERSPECTIVE:Strip3DDefinition.Sampling.SLIDE);d.setVertical(n==5?Strip3DDefinition.Sampling.SLIDE:Strip3DDefinition.Sampling.PERSPECTIVE);d.setImportanceEnabled(n!=6);definition.setShape(n==7?VolumeDefinition.Shape.CONE:VolumeDefinition.Shape.CYLINDER);
        d.resetSelectionMask();if(n==7){boolean[] shape=new boolean[24*24];for(int y=0;y<24;y++)for(int x=0;x<24;x++)shape[y*24+x]=Math.abs(x-12)+Math.abs(y-12)<13;d.setSelectionMask(24,24,shape);}
        if(n>=11){
            int[] widths={1,37,96,24,96,37};d.setSampleWidth(widths[n-11]);definition.setLightingEnabled(false);
            cam.setParallelProjection(true);cam.setFrustum(.1f,100,-1,1,1,-1);
            if(n==15)d.setHorizontal(Strip3DDefinition.Sampling.PERSPECTIVE);
            if(n==16){boolean[] mask=new boolean[8*8];for(int i=0;i<mask.length;i++)mask[i]=(i%8+i/8)%3!=0;d.setSelectionMask(8,8,mask);}
        }
        double yaw=Math.toRadians(angles[n][0]),pitch=Math.toRadians(angles[n][1]);Vector3f eye=new Vector3f((float)(Math.sin(yaw)*Math.cos(pitch)),(float)Math.sin(pitch),(float)(Math.cos(yaw)*Math.cos(pitch))).multLocal(7);Vector3f up=Math.abs(pitch)>1.56?(pitch>0?new Vector3f(0,0,-1):Vector3f.UNIT_Z):Vector3f.UNIT_Y;
        cam.setLocation(eye);cam.lookAt(Vector3f.ZERO,up);
        if(n==8||n==9)try{definition.setLightingEnabled(true);definition.setLight(0,0,1);BufferedImage normal=new BufferedImage(96,24,BufferedImage.TYPE_INT_ARGB);for(int y=0;y<24;y++)for(int x=0;x<96;x++)normal.setRGB(x,y,n==8?0xff8080ff:0xff008080);Path path=Path.of("target/strip3d-test-normal.png").toAbsolutePath();ImageIO.write(normal,"png",path.toFile());d.layer(Strip3DDefinition.Surface.SIDE).setNormalPath(path.toString());view.invalidate();}catch(Exception e){throw new RuntimeException(e);}
        if(n==10){definition.setLightingEnabled(false);cam.setParallelProjection(false);cam.setFrustumPerspective(30,1,.1f,100);}
    }
    @Override public void simpleUpdate(float tpf){if(done.getCount()>0&&view!=null)view.update(definition,Vector3f.ZERO,new Quaternion(),Vector3f.UNIT_XYZ,cam,true,stage==7);}
    @Override public void simpleRender(RenderManager manager){
        if(done.getCount()==0||++frames%8!=0)return;try{
            ByteBuffer bytes=BufferUtils.createByteBuffer(512*512*4);renderer.readFrameBuffer(null,bytes);BufferedImage image=new BufferedImage(512,512,BufferedImage.TYPE_INT_RGB);for(int y=0;y<512;y++)for(int x=0;x<512;x++){int p=(y*512+x)*4;image.setRGB(x,511-y,((bytes.get(p)&255)<<16)|((bytes.get(p+1)&255)<<8)|(bytes.get(p+2)&255));}shots[stage]=image;ImageIO.write(image,"png",Path.of("target/strip3d-stage-"+stage+".png").toFile());
            if(view.getError()!=null)throw new AssertionError(view.getError());int bg=image.getRGB(0,0);
            if(stage<8||stage>=11){var d=definition.getStrip3D();var layout=TriStripSampler.layout(d,angles[stage][0],angles[stage][1]);BufferedImage[] art=new BufferedImage[3];for(var f:Strip3DDefinition.Surface.values())art[f.ordinal()]=Strip3DTextures.exampleImage(f);var expected=TriStripSampler.compose(d,layout,art,false);
                double pixelScaleX=stage>=11?192.0/d.getSampleWidth():4,pixelScaleY=stage>=11?8:4;
                double originX=256-layout.canvasWidth()*pixelScaleX/2+((layout.canvasWidth()-layout.columns().length)&1)*pixelScaleX/2,originY=256-layout.canvasHeight()*pixelScaleY/2;
                for(int y=0;y<layout.canvasHeight();y++)for(int x=0;x<layout.canvasWidth();x++){int wanted=expected.getRGB(x,y);if(stage==7&&(wanted>>>24)==0)continue;if((wanted>>>24)==0)wanted=bg;int screenX=(int)Math.floor(originX+(x+.5)*pixelScaleX),screenY=(int)Math.floor(originY+(y+.5)*pixelScaleY);if(screenX<0||screenX>=512||screenY<0||screenY>=512)continue;int actual=image.getRGB(screenX,screenY);if(actual!=wanted)throw new AssertionError("Stage "+stage+" pixel "+x+","+y+": expected "+Integer.toHexString(wanted)+" actual "+Integer.toHexString(actual));}
            }
            if(stage<16){setStage(stage+1);return;}
            int lit=shots[8].getRGB(250,265),dark=shots[9].getRGB(250,265);if(((lit>>8)&255)<=((dark>>8)&255)+20)throw new AssertionError("Normal refresh must alter lighting");
            int count=0;for(int y=0;y<512;y++)for(int x=0;x<512;x++)if(shots[10].getRGB(x,y)!=bg)count++;if(count<4000)throw new AssertionError("Perspective card missing");
            BufferedImage sheet=new BufferedImage(1536,3072,BufferedImage.TYPE_INT_RGB);var g=sheet.createGraphics();String[] labels={"FRONT","YAW 45 / PITCH 30","TOP","BOTTOM","BOTH PERSPECTIVE","VERTICAL SLIDE","UNIFORM ROWS","CONE PROXY / SAME ART","NORMAL +Z","REFRESH NORMAL -X","PERSPECTIVE CAMERA","WIDTH 1","WIDTH 37","WIDTH 96 WRAP","WIDTH 24 RESTORED","WIDTH 96 PERSPECTIVE","WIDTH 37 CUSTOM SHAPE"};for(int i=0;i<shots.length;i++){int x=i%3*512,y=i/3*512;g.drawImage(shots[i],x,y,null);g.setColor(java.awt.Color.WHITE);g.setFont(new java.awt.Font("SansSerif",0,20));g.drawString(labels[i],x+16,y+28);}g.dispose();ImageIO.write(sheet,"png",Path.of("target/strip3d-views.png").toFile());view.cleanup();done.countDown();
        }catch(Throwable e){failure=e;done.countDown();}
    }
    @Override public void handleError(String message,Throwable e){System.err.println(message);failure=e;done.countDown();}
}
