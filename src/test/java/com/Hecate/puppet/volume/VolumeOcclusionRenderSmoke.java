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

/** Compare every composite pixel with independently rendered layers, including an unrelated foreground part. */
public final class VolumeOcclusionRenderSmoke extends SimpleApplication {
    public VolumeOcclusionRenderSmoke(){super(new com.jme3.app.StatsAppState());}
    private final CountDownLatch done=new CountDownLatch(1);private Throwable failure;private int frame,scenario,phase;
    private final Strip3DView[] views=new Strip3DView[4];private final VolumeDefinition[] defs=new VolumeDefinition[4];
    private final BufferedImage[] isolated=new BufferedImage[4];private final VolumeOcclusion covers=new VolumeOcclusion();
    private final ArrayList<BufferedImage> results=new ArrayList<>();
    private final String[] names={"outer","middle","inner","unrelated"};
    private final double[][] angles={{0,0},{180,0},{35,28},{0,90},{-37,-89},{52,31},{0,0},{0,0},{0,0},{0,0}};
    public static void main(String[] args)throws Exception{
        var app=new VolumeOcclusionRenderSmoke();var s=new AppSettings(true);s.setResolution(512,512);s.setAudioRenderer(null);s.setUseInput(false);s.setSamples(0);s.setGammaCorrection(false);app.setSettings(s);app.setShowSettings(false);app.start(JmeContext.Type.OffscreenSurface);
        boolean ok=app.done.await(40,TimeUnit.SECONDS);app.stop(false);if(!ok||app.failure!=null){if(app.failure!=null)app.failure.printStackTrace();System.exit(1);}System.out.println("Cover GPU checks passed: all pixels in 10 scenarios, nested alpha holes, reverse depth, perspective, top/bottom, roll, hidden wrapper, remove relation, unrelated foreground");System.exit(0);
    }
    public void simpleInitApp(){try{
        setDisplayFps(false);setDisplayStatView(false);viewPort.setBackgroundColor(ColorRGBA.Black);
        int[] colors={0xffe53f32,0xff43bd65,0xff367bea,0xffffd438};
        for(int i=0;i<4;i++){
            var source=new BufferedImage(32,8,BufferedImage.TYPE_INT_ARGB);
            for(int y=0;y<8;y++)for(int x=0;x<32;x++){boolean hole=i==0&&x%8>=2&&x%8<=5&&y>=2&&y<=5||i==1&&x%8>=3&&x%8<=4&&y>=3&&y<=4;source.setRGB(x,y,hole?0:colors[i]);}
            var path=Path.of("target/cover-test-"+i+".png").toAbsolutePath();ImageIO.write(source,"png",path.toFile());
            var d=defs[i]=new VolumeDefinition();d.setSurfaceDepthEnabled(false);d.setDimensions(i==3?10:48-i*5,i==3?70:48-i*5,32);var strip=d.getStrip3D();strip.setEnabled(true);strip.setFrameSize(8,8);strip.setOutlineEnabled(false);strip.setHorizontal(Strip3DDefinition.Sampling.SLIDE);strip.setVertical(Strip3DDefinition.Sampling.SLIDE);for(var face:Strip3DDefinition.Surface.values())strip.layer(face).setColorPath(path.toString());
            views[i]=new Strip3DView(assetManager,rootNode,names[i]);
        }
        setScenario();
    }catch(Throwable e){failure=e;done.countDown();}}
    private void setScenario(){
        double yaw=Math.toRadians(angles[scenario][0]),pitch=Math.toRadians(angles[scenario][1]);Vector3f eye=new Vector3f((float)(Math.sin(yaw)*Math.cos(pitch)),(float)Math.sin(pitch),(float)(Math.cos(yaw)*Math.cos(pitch))).multLocal(5);
        cam.setParallelProjection(true);cam.setFrustum(.1f,100,-2,2,2,-2);if(scenario==2||scenario==5){cam.setParallelProjection(false);cam.setFrustumPerspective(45,1,.1f,100);}cam.setLocation(eye);cam.lookAt(Vector3f.ZERO,Math.abs(pitch)>1.56?Vector3f.UNIT_Z:Vector3f.UNIT_Y);
        defs[0].setCoverTarget(scenario==7||scenario==9?null:names[1]);defs[1].setCoverTarget(scenario==9?null:names[2]);
    }
    public void simpleUpdate(float tpf){if(done.getCount()==0||views[0]==null)return;
        var entries=new ArrayList<VolumeOcclusion.Entry>();
        for(int i=0;i<4;i++){
            boolean visible=(phase==4||phase==i)&&!(scenario==6&&i==0)&&scenario!=8;
            Vector3f position=i==3?cam.getDirection().negate().mult(1.2f).addLocal(cam.getLeft().negate().mult(.65f)):new Vector3f(0,0,(i-1)*.25f);
            Quaternion rotation=scenario==5?new Quaternion().fromAngles(.2f,.4f,.7f):new Quaternion();
            views[i].update(defs[i],position,rotation,Vector3f.UNIT_XYZ,cam,visible,false);entries.add(new VolumeOcclusion.Entry(names[i],defs[i],views[i]));
        }covers.apply(entries,cam);
    }
    public void simpleRender(RenderManager rm){if(done.getCount()==0||++frame%4!=0)return;try{
        var bytes=BufferUtils.createByteBuffer(512*512*4);renderer.readFrameBuffer(null,bytes);var image=new BufferedImage(512,512,BufferedImage.TYPE_INT_RGB);
        for(int y=0;y<512;y++)for(int x=0;x<512;x++){int p=(y*512+x)*4;image.setRGB(x,511-y,(bytes.get(p)&255)<<16|(bytes.get(p+1)&255)<<8|bytes.get(p+2)&255);}
        if(phase<4){isolated[phase++]=image;return;}
        int[] order=scenario==7?new int[]{3,1,2,0}:scenario==9?new int[]{3,2,1,0}:new int[]{3,0,1,2};
        int mismatches=0;for(int y=0;y<512;y++)for(int x=0;x<512;x++){
            int expected=0xff000000;for(int i:order)if(isolated[i].getRGB(x,y)!=0xff000000){expected=isolated[i].getRGB(x,y);break;}
            if(expected!=image.getRGB(x,y)){if(mismatches<12)System.out.println("Mismatch "+x+","+y+" expected "+Integer.toHexString(expected)+" actual "+Integer.toHexString(image.getRGB(x,y)));mismatches++;}
        }
        ImageIO.write(image,"png",Path.of("target/cover-scenario-"+scenario+".png").toFile());if(mismatches!=0){
            for(int i=0;i<4;i++)ImageIO.write(isolated[i],"png",Path.of("target/cover-isolated-"+i+".png").toFile());
            throw new AssertionError("Scenario "+scenario+": "+mismatches+" unexpected pixels");}results.add(image);
        if(++scenario<angles.length){phase=0;setScenario();return;}
        var sheet=new BufferedImage(2560,1024,BufferedImage.TYPE_INT_RGB);var g=sheet.createGraphics();for(int i=0;i<results.size();i++){g.drawImage(results.get(i),i%5*512,i/5*512,null);g.setColor(java.awt.Color.WHITE);g.drawString("CASE "+i,i%5*512+16,i/5*512+24);}g.dispose();ImageIO.write(sheet,"png",Path.of("target/cover-regression.png").toFile());for(var view:views)view.cleanup();done.countDown();
    }catch(Throwable e){failure=e;done.countDown();}}
    public void handleError(String message,Throwable e){failure=e;done.countDown();}
}
