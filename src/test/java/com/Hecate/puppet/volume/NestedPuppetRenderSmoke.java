package com.Hecate.puppet.volume;
import com.Hecate.puppet.config.PuppetIO;
import com.Hecate.puppet.editor.core.*;
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

/** Optional real-file regression. The input is read only and is never copied into the repository. */
public final class NestedPuppetRenderSmoke extends SimpleApplication {
    private final CountDownLatch done=new CountDownLatch(1);private Throwable failure;
    private static String file;private static boolean diagnose,near;
    private EditorPuppetRenderer puppet;private EditorSkeleton skeleton;
    private int frame,scenario,phase,totalBad,failedViews;private BufferedImage outer,inner;
    private final ArrayList<BufferedImage> failures=new ArrayList<>();
    private final ArrayList<String> labels=new ArrayList<>();
    private Matrix3f referenceProjection;private TriStripSampler.Layout referenceLayout;private Strip3DDefinition referenceSelection;
    private static final int SIZE=720,CASES=288;
    public NestedPuppetRenderSmoke(){super(new com.jme3.app.StatsAppState());}
    public static void main(String[] args)throws Exception{
        file=args[0];diagnose=Arrays.asList(args).contains("--diagnose");near=Arrays.asList(args).contains("--near");
        var app=new NestedPuppetRenderSmoke();var s=new AppSettings(true);s.setResolution(SIZE,SIZE);s.setAudioRenderer(null);s.setUseInput(false);s.setSamples(0);s.setGammaCorrection(false);app.setSettings(s);app.setShowSettings(false);app.start(JmeContext.Type.OffscreenSurface);
        boolean ok=app.done.await(150,TimeUnit.SECONDS);app.stop(false);if(!ok||app.failure!=null){if(app.failure!=null)app.failure.printStackTrace();System.exit(1);}System.out.println("Nested file check: "+app.failedViews+" failing views, "+app.totalBad+" incorrectly ordered pixels / "+CASES+" views");if(app.totalBad>0&&!diagnose)System.exit(1);System.exit(0);
    }
    public void simpleInitApp(){try{
        setDisplayFps(false);setDisplayStatView(false);viewPort.setBackgroundColor(new ColorRGBA(.15f,.17f,.2f,1));
        skeleton=new EditorSkeleton("regression");puppet=new EditorPuppetRenderer(this,skeleton);PuppetIO.applyConfig(PuppetIO.loadFromFile(file),skeleton,puppet);puppet.attachToScene(rootNode);puppet.setVolumeViewMode(VolumeReferenceView.Mode.APPEARANCE);puppet.setAutoDirectionSwitch(false);
        for(var b:skeleton.getAllBones()){var p=new Vector3f();b.getWorldTransform(p,new Quaternion(),new Vector3f());System.out.println(b.getName()+" world="+p);}
        setup();
    }catch(Throwable e){failure=e;done.countDown();}}
    private void setup(){
        double yaw=Math.toRadians(scenario%24*15),pitch=Math.toRadians((near?new int[]{0,20,-20,45,-45,70}:new int[]{0,10,20,45,80,-45})[scenario/24%6]);
        Vector3f target=new Vector3f(0,-.4f,0),eye=new Vector3f((float)(Math.sin(yaw)*Math.cos(pitch)),(float)Math.sin(pitch),(float)(Math.cos(yaw)*Math.cos(pitch))).multLocal(near?1.5f:6).addLocal(target);
        cam.setParallelProjection(true);cam.setFrustum(.1f,1000,-1.8f,1.8f,1.8f,-1.8f);
        if(scenario>=144){cam.setParallelProjection(false);cam.setFrustumPerspective(35,1,.1f,1000);}
        cam.setLocation(eye);cam.lookAt(target,Vector3f.UNIT_Y);
    }
    public void simpleUpdate(float tpf){if(done.getCount()==0||puppet==null)return;
        puppet.getPartRenderer("Volume_1").setVisible(phase!=0);puppet.getPartRenderer("Volume_2").setVisible(phase!=1);puppet.update(0);
    }
    public void simpleRender(RenderManager rm){if(done.getCount()==0||++frame%3!=0)return;try{
        var b=BufferUtils.createByteBuffer(SIZE*SIZE*4);renderer.readFrameBuffer(null,b);var image=new BufferedImage(SIZE,SIZE,BufferedImage.TYPE_INT_RGB);
        for(int y=0;y<SIZE;y++)for(int x=0;x<SIZE;x++){int p=(y*SIZE+x)*4;image.setRGB(x,SIZE-1-y,(b.get(p)&255)<<16|(b.get(p+1)&255)<<8|b.get(p+2)&255);}
        if(phase==0){outer=image;phase=1;return;}if(phase==1){inner=image;phase=2;return;}
        prepareSelectionReference();
        int bad=0,bg=0xff262b33;for(int y=0;y<SIZE;y++)for(int x=0;x<SIZE;x++){
            int expected=outer.getRGB(x,y)==bg?(inner.getRGB(x,y)!=bg&&insideOuterSelection(x,y)?inner.getRGB(x,y):bg):outer.getRGB(x,y);
            if(image.getRGB(x,y)!=expected){
                if(failedViews==0&&bad<4){
                    float sx=(x/2*2+1f)/SIZE*2-1,sy=((SIZE-1-y)/2*2+1f)/SIZE*2-1;
                    System.out.println("Mismatch @"+x+","+y+" expected="+Integer.toHexString(expected)+" actual="+Integer.toHexString(image.getRGB(x,y)));
                    for(String name:List.of("Volume_1","Volume_2")){
                        var view=puppet.getPartRenderer(name).getStrip3DView();var def=skeleton.findBone(name).getVolumeDefinition();
                        var texture=(com.jme3.texture.Texture)view.getGeometry().getMaterial().getParam("ColorMap").getValue();
                        System.out.println(name+" physical="+SurfaceProjection.hit(view.getSurfaceProjection(),def,sx,sy)+" art="+SurfaceProjection.hitArtwork(view.getSurfaceProjection(),def,sx,sy,texture.getImage().getWidth(),texture.getImage().getHeight()));
                    }
                }bad++;
            }
        }
        if(bad>0){failedViews++;totalBad+=bad;System.out.println("View "+scenario+" wrong="+bad);
            if(failures.size()<24){failures.add(image);labels.add("VIEW "+scenario+" / WRONG "+bad);}
            if(failedViews==1){ImageIO.write(outer,"png",Path.of("target/nested-outer.png").toFile());ImageIO.write(inner,"png",Path.of("target/nested-inner.png").toFile());ImageIO.write(image,"png",Path.of("target/nested-failed.png").toFile());}
        }
        if(scenario%12==0){ImageIO.write(image,"png",Path.of("target/nested-view-"+scenario+".png").toFile());}
        if(scenario==44){ImageIO.write(image,"png",Path.of("target/nested-selection-side.png").toFile());}
        if(++scenario<CASES){phase=0;setup();return;}
        if(!failures.isEmpty()){var sheet=new BufferedImage(1440,360*((failures.size()+3)/4),BufferedImage.TYPE_INT_RGB);var g=sheet.createGraphics();for(int i=0;i<failures.size();i++){g.drawImage(failures.get(i),i%4*360,i/4*360,360,360,null);g.setColor(java.awt.Color.WHITE);g.drawString(labels.get(i),i%4*360+10,i/4*360+20);}g.dispose();ImageIO.write(sheet,"png",Path.of("target/nested-failures.png").toFile());}done.countDown();
    }catch(Throwable e){failure=e;done.countDown();}}
    /** Independent oracle for the uploaded fixture's unrotated outer part: sample the
     * original selection grid, without using the polygon intersection or clipping mesh. */
    private void prepareSelectionReference(){
        var view=puppet.getPartRenderer("Volume_2").getStrip3DView();
        referenceSelection=skeleton.findBone("Volume_2").getVolumeDefinition().getStrip3D();
        referenceProjection=view.getSurfaceProjection().screenToArt();
        Vector3f toward=cam.getDirection().negate();
        referenceLayout=TriStripSampler.layout(referenceSelection,Math.toDegrees(Math.atan2(toward.x,toward.z)),Math.toDegrees(Math.asin(toward.y)));
    }
    private boolean insideOuterSelection(int x,int y){
        float sx=(x/2*2+1f)/SIZE*2-1,sy=((SIZE-1-y)/2*2+1f)/SIZE*2-1;
        var m=referenceProjection;var l=referenceLayout;
        float u=m.get(0,0)*sx+m.get(0,1)*sy+m.get(0,2),v=m.get(1,0)*sx+m.get(1,1)*sy+m.get(1,2);
        int col=(int)Math.floor(u*l.canvasWidth()+.0001f)-(l.canvasWidth()-l.columns().length)/2;
        int row=l.canvasHeight()-1-(int)Math.floor(v*l.canvasHeight()+.0001f)-(l.canvasHeight()-l.rows().length)/2;
        return col>=0&&col<l.columns().length&&row>=0&&row<l.rows().length&&referenceSelection.selectsPixel(col,row,l.columns().length,l.rows().length,l.selectionCosine());
    }
    public void handleError(String message,Throwable e){failure=e;done.countDown();}
}
