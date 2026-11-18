package com.Hecate.puppet.volume;

import com.jme3.app.SimpleApplication;
import com.jme3.material.*;
import com.jme3.math.*;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.*;
import com.jme3.system.*;
import com.jme3.util.BufferUtils;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import javax.imageio.ImageIO;

/** Independent GPU oracle: rasterized triangle meshes versus analytic surface depth. */
public final class SurfaceDepthRenderSmoke extends SimpleApplication {
    private static final int SIZE=768,CASES=15;
    private final CountDownLatch done=new CountDownLatch(1);
    private Throwable failure;private int frame,scenario,phase;
    private final Strip3DView[] views=new Strip3DView[3];
    private final Geometry[] reference=new Geometry[3];
    private Geometry nearCap;
    private final VolumeDefinition[] defs=new VolumeDefinition[3];
    private final VolumeOcclusion occlusion=new VolumeOcclusion();
    private BufferedImage expected;private final ArrayList<BufferedImage> shots=new ArrayList<>();
    public SurfaceDepthRenderSmoke(){super(new com.jme3.app.StatsAppState());}
    public static void main(String[] args)throws Exception{
        var app=new SurfaceDepthRenderSmoke();var settings=new AppSettings(true);settings.setResolution(SIZE,SIZE);settings.setAudioRenderer(null);settings.setUseInput(false);settings.setSamples(0);settings.setGammaCorrection(false);
        app.setSettings(settings);app.setShowSettings(false);app.start(JmeContext.Type.OffscreenSurface);
        boolean ok=app.done.await(60,TimeUnit.SECONDS);app.stop(false);
        if(!ok||app.failure!=null){if(app.failure!=null)app.failure.printStackTrace();else System.err.println("Timeout");System.exit(1);}
        System.out.println("Surface depth GPU: 15 mesh-reference comparisons passed; nested/intersecting volumes, caps, perspective, nonuniform/mirrored transforms, masks, alpha, hidden/blank layers, near plane, whole pixel cells");System.exit(0);
    }
    public void simpleInitApp(){try{
        setDisplayFps(false);setDisplayStatView(false);viewPort.setBackgroundColor(ColorRGBA.Black);
        for(int i=0;i<3;i++){views[i]=new Strip3DView(assetManager,rootNode,"surface"+i);reference[i]=new Geometry("reference"+i);var m=new Material(assetManager,"MatDefs/SurfaceReference.j3md");m.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);reference[i].setMaterial(m);rootNode.attachChild(reference[i]);}
        nearCap=new Geometry("referenceNearCap",new com.jme3.scene.shape.Quad(1,1));nearCap.setMaterial(reference[0].getMaterial());rootNode.attachChild(nearCap);nearCap.setCullHint(Spatial.CullHint.Always);
        setup();
    }catch(Throwable e){failure=e;done.countDown();}}
    private void setup()throws Exception{
        cam.setViewPort(0,1,0,1);cam.setParallelProjection(true);cam.setFrustum(.1f,100,-2,2,2,-2);
        double yaw=scenario==1?Math.PI:scenario==2?.8:scenario==3?.6:scenario>=6?.65:0;
        double pitch=scenario==4?Math.PI/2:scenario==5?-Math.PI/2:scenario>=6?.45:0;
        cam.setLocation(new Vector3f((float)(Math.sin(yaw)*Math.cos(pitch)),(float)Math.sin(pitch),(float)(Math.cos(yaw)*Math.cos(pitch))).multLocal(5));
        cam.lookAt(Vector3f.ZERO,Math.abs(pitch)>1.56?Vector3f.UNIT_Z:Vector3f.UNIT_Y);
        if(scenario==3||scenario==7||scenario==12||scenario==14){cam.setParallelProjection(false);cam.setFrustumPerspective(45,1,.1f,100);}
        if(scenario==13){cam.setViewPort(.125f,.875f,.125f,.875f);}
        if(scenario==14){cam.setLocation(new Vector3f(0,.2f,.85f));cam.lookAt(new Vector3f(0,.2f,-1),Vector3f.UNIT_Y);}
        if(scenario==14){
            // Independent mesh cap: this fixture's outer unit cylinder is cut by z=.75.
            // The intersection is a rectangle, x=+/-sqrt(1-z*z), y=+/-1.
            float z=.75f-.00001f,w=(float)Math.sqrt(1-z*z);var cap=new Mesh();
            cap.setBuffer(VertexBuffer.Type.Position,3,new float[]{-w,-1,z,w,-1,z,w,1,z,-w,1,z});
            cap.setBuffer(VertexBuffer.Type.Index,3,new int[]{0,1,2,0,2,3});cap.updateBound();nearCap.setMesh(cap);
        }
        for(int i=0;i<3;i++){
            var d=defs[i]=new VolumeDefinition();d.setSurfaceContourSnapped(false);d.setDimensions(i==0?64:i==1?40:12,i==0?64:i==1?40:75,i==0?64:i==1?40:12);
            if(scenario==6)d.setShape(i==0?VolumeDefinition.Shape.FRUSTUM:VolumeDefinition.Shape.CONE);
            if(scenario==7)d.setShape(i==0?VolumeDefinition.Shape.BOX:VolumeDefinition.Shape.FRUSTUM);
            if(scenario==8)d.setDimensions(i==0?64:40,i==0?32:24,i==0?40:30);
            // An obsolete manual relation must not force wrong ordering in surface mode.
            d.setCoverTarget(i==0?"surface1":null);
            var strip=d.getStrip3D();strip.setEnabled(true);strip.setFrameSize(12,12);strip.setOutlineEnabled(false);
            strip.setVertical(Strip3DDefinition.Sampling.PERSPECTIVE);strip.setHorizontal(Strip3DDefinition.Sampling.SLIDE);
            var image=new BufferedImage(48,12,BufferedImage.TYPE_INT_ARGB);
            for(int y=0;y<12;y++)for(int x=0;x<48;x++){
                int color=i==0?0xffdd4433:i==1?0xff4488ee:0xffeecc33;
                if(scenario==9&&i==0&&x%12>=3&&x%12<9&&y>=3&&y<9)color=0;
                image.setRGB(x,y,color);
            }
            var path=Path.of("target/surface-source-"+i+".png").toAbsolutePath();ImageIO.write(image,"png",path.toFile());
            if(scenario!=11)for(var face:Strip3DDefinition.Surface.values())strip.layer(face).setColorPath(path.toString());
            if(scenario==10&&i==0){boolean[] mask=new boolean[144];for(int y=0;y<12;y++)for(int x=0;x<12;x++)mask[y*12+x]=x<3||x>8||y<3||y>8;strip.setSelectionMask(12,12,mask);}
            reference[i].setMesh(denseMesh(d));views[i].invalidate();
        }
    }
    public void simpleUpdate(float tpf){if(done.getCount()==0||defs[0]==null)return;
        nearCap.setCullHint(scenario==14&&phase==0?Spatial.CullHint.Never:Spatial.CullHint.Always);
        var entries=new ArrayList<VolumeOcclusion.Entry>();
        for(int i=0;i<3;i++){
            Vector3f position=i==1?new Vector3f(0,0,.15f):i==2?new Vector3f(.65f,0,1.1f):new Vector3f();
            if(scenario==2||scenario==3||scenario==6||scenario==7||scenario==12)position=i==1?new Vector3f(.55f,.3f,.7f):i==2?new Vector3f(-.6f,0,.7f):new Vector3f();
            Quaternion rotation=scenario>=6&&scenario<=8?new Quaternion().fromAngles(.3f,.4f,i==0?.65f:-.25f):new Quaternion();
            Vector3f scale=scenario==8?new Vector3f(i==0?-1.2f:1,.8f,1.3f):Vector3f.UNIT_XYZ;
            boolean visible=!(scenario==12&&i==0);
            views[i].update(defs[i],position,rotation,scale,cam,visible,false);
            var projection=views[i].getSurfaceProjection();var material=reference[i].getMaterial();
            material.setBoolean("UseEnclosingSelection",scenario==10&&i==1);
            if(scenario==10&&i==1){
                material.setParam("EnclosingProjection",com.jme3.shader.VarType.Matrix3,views[0].getSurfaceProjection().screenToArt());
                material.setTexture("EnclosingSelection",(com.jme3.texture.Texture)views[0].getGeometry().getMaterial().getParam("ColorMap").getValue());
            }
            if(projection!=null){material.setParam("ArtScreenToUV",com.jme3.shader.VarType.Matrix3,projection.screenToArt());material.setTexture("ColorMap",(com.jme3.texture.Texture)views[i].getGeometry().getMaterial().getParam("ColorMap").getValue());}
            material.setVector4("Viewport",new Vector4f(cam.getWidth()*cam.getViewPortLeft(),cam.getHeight()*cam.getViewPortBottom(),cam.getWidth()*(cam.getViewPortRight()-cam.getViewPortLeft()),cam.getHeight()*(cam.getViewPortTop()-cam.getViewPortBottom())));
            reference[i].setLocalTransform(new Transform(position,rotation,scale));reference[i].setCullHint(visible&&phase==0?Spatial.CullHint.Never:Spatial.CullHint.Always);
            if(phase==0)views[i].setVisible(false);
            entries.add(new VolumeOcclusion.Entry("surface"+i,defs[i],views[i]));
        }occlusion.apply(entries,cam);
    }
    public void simpleRender(RenderManager rm){if(done.getCount()==0||++frame%4!=0)return;try{
        var buffer=BufferUtils.createByteBuffer(SIZE*SIZE*4);renderer.readFrameBuffer(null,buffer);var image=new BufferedImage(SIZE,SIZE,BufferedImage.TYPE_INT_RGB);
        for(int y=0;y<SIZE;y++)for(int x=0;x<SIZE;x++){int p=(y*SIZE+x)*4;image.setRGB(x,SIZE-1-y,(buffer.get(p)&255)<<16|(buffer.get(p+1)&255)<<8|buffer.get(p+2)&255);}
        if(phase==0){expected=image;phase=1;return;}
        ImageIO.write(image,"png",Path.of("target/surface-case-"+scenario+".png").toFile());
        int step=SurfaceProjection.pixelStep(cam),bad=0,occupied=0,exact=0;
        int vx=Math.round(cam.getViewPortLeft()*SIZE),vy=Math.round(cam.getViewPortBottom()*SIZE),vw=Math.round((cam.getViewPortRight()-cam.getViewPortLeft())*SIZE),vh=Math.round((cam.getViewPortTop()-cam.getViewPortBottom())*SIZE);
        for(int gy=0;gy<vh;gy+=step)for(int gx=0;gx<vw;gx+=step){
            int x=vx+gx,y=SIZE-1-vy-gy,actual=image.getRGB(x,y);
            for(int a=0;a<step&&gx+a<vw;a++)for(int b=0;b<step&&gy+b<vh;b++)if(image.getRGB(x+a,y-b)!=actual)throw new AssertionError("Partial cell at "+x+","+y+" case "+scenario);
            int cx=Math.min(SIZE-1,x+step/2),cy=Math.max(0,y-step/2),want=expected.getRGB(cx,cy);
            if(want!=0xff000000||actual!=0xff000000)occupied++;
            if(want==actual){if(actual!=0xff000000)exact++;continue;}
            // Raster sample centres and analytic cell centres differ by <= half a screen pixel.
            // A 256-sided mesh approximates curved surfaces; only boundary-adjacent differences are allowed.
            boolean boundary=false;for(int dy=-1;dy<=1;dy++)for(int dx=-1;dx<=1;dx++)if(expected.getRGB(Math.max(0,Math.min(SIZE-1,cx+dx)),Math.max(0,Math.min(SIZE-1,cy+dy)))==actual)boundary=true;
            if(!boundary){if(bad<6)System.out.println("case="+scenario+" "+x+","+y+" expected="+Integer.toHexString(want)+" actual="+Integer.toHexString(actual));bad++;}
        }
        if(bad>0||occupied>0&&exact<occupied*.9){ImageIO.write(expected,"png",Path.of("target/surface-reference-failed.png").toFile());throw new AssertionError("case "+scenario+": "+bad+" non-boundary mismatches; exact "+exact+" / "+occupied);}
        if(scenario==11&&occupied!=0)throw new AssertionError("Blank parts acquired default art");
        if(scenario!=11&&occupied<300)throw new AssertionError("Missing artwork, case "+scenario);
        System.out.println("Surface case "+scenario+" OK, exact occupied cells "+exact+"/"+occupied);shots.add(image);
        if(++scenario<CASES){phase=0;setup();return;}
        var sheet=new BufferedImage(1920,1152,BufferedImage.TYPE_INT_RGB);var g=sheet.createGraphics();for(int i=0;i<shots.size();i++){g.drawImage(shots.get(i),i%5*384,i/5*384,384,384,null);g.setColor(java.awt.Color.WHITE);g.drawString("CASE "+i,i%5*384+12,i/5*384+20);}g.dispose();ImageIO.write(sheet,"png",Path.of("target/surface-depth-regression.png").toFile());done.countDown();
    }catch(Throwable e){failure=e;done.countDown();}}
    /** High-resolution closed reference mesh, rendered by the ordinary triangle depth pipeline. */
    private static Mesh denseMesh(VolumeDefinition d){
        if(d.getShape()==VolumeDefinition.Shape.BOX)return VolumeMeshFactory.create(d);
        int n=256;float[] p=new float[(n*2+2)*3];int[] idx=new int[n*12];
        for(int i=0;i<n;i++)for(int cap=0;cap<2;cap++){double angle=2*Math.PI*i/n;float r=cap==0?1:d.effectiveTopRatio();int k=(i+n*cap)*3;p[k]=(float)Math.cos(angle)*d.width()/2*r;p[k+1]=(cap==0?-1:1)*d.height()/2;p[k+2]=(float)Math.sin(angle)*d.depth()/2*r;}
        p[n*6+1]=-d.height()/2;p[n*6+4]=d.height()/2;
        for(int i=0;i<n;i++){int j=(i+1)%n;int[] triangles={i,n+i,j,j,n+i,n+j,n*2,i,j,n*2+1,n+j,n+i};System.arraycopy(triangles,0,idx,i*12,12);}
        var mesh=new Mesh();mesh.setBuffer(VertexBuffer.Type.Position,3,p);mesh.setBuffer(VertexBuffer.Type.Index,3,idx);mesh.updateBound();return mesh;
    }
    public void handleError(String message,Throwable e){failure=e;done.countDown();}
}
