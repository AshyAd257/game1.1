package com.Hecate.puppet.volume;

import com.jme3.asset.AssetManager;
import com.jme3.material.*;
import com.jme3.math.*;
import com.jme3.renderer.Camera;
import com.jme3.scene.*;
import com.jme3.texture.*;
import com.jme3.texture.image.ColorSpace;
import com.jme3.util.BufferUtils;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

/** Integer-composed pixel art with analytic volume depth; legacy billboard rendering remains available. */
public final class Strip3DView {
    private com.Hecate.puppet.pixel.BakedPixelView bakedView;
    public com.Hecate.puppet.pixel.BakedPixelView getBakedView(){return bakedView;}
    public void prepareBakedPixels(VolumeDefinition d){
        if(!d.isBakedPixelsEnabled())return;
        if(bakedView==null)bakedView=new com.Hecate.puppet.pixel.BakedPixelView(assets,parent,geometry.getName());
        bakedView.prepare(d);
    }
    public void setPixelPriority(int value){if(bakedView!=null)bakedView.setPriority(value);}

    private final AssetManager assets;private final Node parent;private final Geometry geometry;private final Material material;private final Mesh mesh;
    private final BufferedImage[] colors=new BufferedImage[3],normals=new BufferedImage[3];
    private final Texture2D colorTexture=new Texture2D(),normalTexture=new Texture2D();
    private Strip3DDefinition loaded;private TriStripSampler.Layout previous;private boolean invalidated=true;private String error;private float lastYaw;
    private final java.nio.FloatBuffer positions=BufferUtils.createFloatBuffer(12);private final float[] vertices=new float[12];
    public Strip3DView(AssetManager assets,Node parent,String name){
        this.assets=assets;this.parent=parent;mesh=new Mesh();mesh.setBuffer(VertexBuffer.Type.Position,3,positions);mesh.setBuffer(VertexBuffer.Type.TexCoord,2,new float[]{0,0,1,0,1,1,0,1});
        mesh.setBuffer(VertexBuffer.Type.Index,3,new int[]{0,1,2,0,2,3});mesh.setDynamic();geometry=new Geometry(name+"_Strip3D",mesh){
            @Override public int collideWith(com.jme3.collision.Collidable object,com.jme3.collision.CollisionResults results){
                if(surfaceDepth&&object instanceof Ray ray)return pickSurface(ray,results);
                return super.collideWith(object,results);
            }
        };geometry.setIgnoreTransform(true);geometry.setShadowMode(com.jme3.renderer.queue.RenderQueue.ShadowMode.Off);
        material=new Material(assets,"MatDefs/PuppetStrip3D.j3md");geometry.setMaterial(material);material.setInt("CoverCount",0);material.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);material.getAdditionalRenderState().setDepthWrite(true);
        Strip3DTextures.nearest(colorTexture);Strip3DTextures.nearest(normalTexture);parent.attachChild(geometry);
    }
    private Matrix3f screenToUV;
    private Vector3f coverDepth;
    private int coverCount;
    private boolean surfaceDepth;
    public boolean usesSurfaceDepth(){return bakedView!=null||surfaceDepth;}
    public SurfaceProjection.Result getSurfaceProjection(){return surfaceProjection;}
    private SurfaceProjection.Result surfaceProjection;
    private Camera surfaceCamera;
    private VolumeDefinition surfaceDefinition;
    private Transform surfaceWorld;
    private BufferedImage composedColor;
    private VolumeContainment.Proxy containmentProxy;
    private java.awt.geom.Area selectionArea, inheritedSelection;
    private Strip3DDefinition selectionSource;
    private TriStripSampler.Layout selectionLayout;
    private Matrix3f selectionProjection;
    private java.util.List<java.awt.geom.Area> inheritedAreas=java.util.List.of();
    private Vector4f inheritedBounds;
    private float inheritedWidth,inheritedHeight;
    private int inheritedStep;
    private Mesh clippedMesh;
    VolumeContainment.Proxy containmentProxy(){return containmentProxy;}
    boolean isSurfaceVisible(){return bakedView==null&&surfaceDepth&&surfaceProjection!=null&&geometry.getCullHint()!=Spatial.CullHint.Always;}
    java.awt.geom.Area selectionArea(){
        if(!isSurfaceVisible())return null;
        Matrix3f projection=surfaceProjection.screenToArt();
        if(selectionSource!=loaded||!previous.samePixels(selectionLayout)||!projection.equals(selectionProjection)){
            selectionArea=VolumeSelectionClip.silhouette(loaded,previous,projection);
            selectionSource=loaded;selectionLayout=previous;selectionProjection=projection.clone();
        }
        return selectionArea;
    }
    /** Called after every part's current world transform and artwork projection are available. */
    void setSelectionClips(java.util.List<java.awt.geom.Area> areas,Camera camera){
        if(bakedView!=null)return;
        if(areas.isEmpty()){
            inheritedSelection=null;inheritedAreas=java.util.List.of();inheritedBounds=null;geometry.setMesh(mesh);return;
        }
        Vector4f bounds=(Vector4f)material.getParam("ScreenBounds").getValue();
        float width=camera.getWidth()*(camera.getViewPortRight()-camera.getViewPortLeft());
        float height=camera.getHeight()*(camera.getViewPortTop()-camera.getViewPortBottom());int step=SurfaceProjection.pixelStep(camera);
        boolean same=areas.size()==inheritedAreas.size();
        for(int i=0;same&&i<areas.size();i++)same=areas.get(i)==inheritedAreas.get(i);
        if(!same||!bounds.equals(inheritedBounds)||width!=inheritedWidth||height!=inheritedHeight||step!=inheritedStep){
            var intersection=new java.awt.geom.Area(new java.awt.geom.Rectangle2D.Float(bounds.x,bounds.y,Math.max(0,bounds.z-bounds.x),Math.max(0,bounds.w-bounds.y)));
            for(var area:areas)intersection.intersect(area);
            inheritedSelection=intersection;inheritedAreas=java.util.List.copyOf(areas);inheritedBounds=bounds.clone();inheritedWidth=width;inheritedHeight=height;inheritedStep=step;
            clippedMesh=VolumeSelectionClip.mesh(intersection,bounds,width,height,step,clippedMesh);
        }
        clippedMesh.setBound(mesh.getBound().clone());geometry.setMesh(clippedMesh);
    }
    boolean allowsSelection(float screenX,float screenY){return inheritedSelection==null||inheritedSelection.contains(screenX,screenY);}
    private int pickSurface(Ray ray,com.jme3.collision.CollisionResults results){
        if(surfaceProjection==null||composedColor==null||geometry.getCullHint()==Spatial.CullHint.Always)return 0;
        Vector3f screen=surfaceCamera.getScreenCoordinates(ray.getOrigin().add(ray.getDirection()));
        float vx=surfaceCamera.getWidth()*surfaceCamera.getViewPortLeft(),vy=surfaceCamera.getHeight()*surfaceCamera.getViewPortBottom();
        float vw=surfaceCamera.getWidth()*(surfaceCamera.getViewPortRight()-surfaceCamera.getViewPortLeft()),vh=surfaceCamera.getHeight()*(surfaceCamera.getViewPortTop()-surfaceCamera.getViewPortBottom());
        int grid=SurfaceProjection.pixelStep(surfaceCamera);
        float sx=((float)Math.floor((screen.x-vx)/grid)+.5f)*grid/vw*2-1;
        float sy=((float)Math.floor((screen.y-vy)/grid)+.5f)*grid/vh*2-1;
        if(!allowsSelection(sx,sy))return 0;
        var hit=SurfaceProjection.hitArtwork(surfaceProjection,surfaceDefinition,sx,sy,composedColor.getWidth(),composedColor.getHeight());if(hit==null)return 0;
        Vector3f uv=surfaceProjection.screenToArt().mult(new Vector3f(sx,sy,1));
        int x=(int)Math.floor(uv.x/uv.z*composedColor.getWidth()+.0001f);
        int y=composedColor.getHeight()-1-(int)Math.floor(uv.y/uv.z*composedColor.getHeight()+.0001f);
        if(x<0||x>=composedColor.getWidth()||y<0||y>=composedColor.getHeight()||(composedColor.getRGB(x,y)>>>24)<128)return 0;
        Vector3f local=hit.proxyPoint().mult(new Vector3f(surfaceDefinition.width()/2,surfaceDefinition.height()/2,surfaceDefinition.depth()/2));
        Vector3f point=surfaceWorld.transformVector(local,null);float distance=point.distance(ray.getOrigin());
        if(distance>ray.getLimit())return 0;
        results.addCollision(new com.jme3.collision.CollisionResult(geometry,point,distance,0));return 1;
    }
    public boolean isCoverVisible(){return screenToUV!=null&&colorTexture.getImage()!=null&&geometry.getCullHint()!=Spatial.CullHint.Always;}
    /** Invert the card's projective UV-to-screen mapping after all part transforms are current. */
    public void prepareCoverProjection(Camera camera){
        if(bakedView!=null)return;
        if(surfaceDepth)return;
        screenToUV=null;
        material.setBoolean("UseArtProjection",false);
        material.setVector4("CoverViewport",new Vector4f(camera.getWidth()*camera.getViewPortLeft(),camera.getHeight()*camera.getViewPortBottom(),camera.getWidth()*(camera.getViewPortRight()-camera.getViewPortLeft()),camera.getHeight()*(camera.getViewPortTop()-camera.getViewPortBottom())));
        if(geometry.getCullHint()==Spatial.CullHint.Always||colorTexture.getImage()==null)return;
        Matrix4f vp=camera.getViewProjectionMatrix();
        Vector4f c=vp.mult(new Vector4f(vertices[0],vertices[1],vertices[2],1));
        Vector4f a=vp.mult(new Vector4f(vertices[3],vertices[4],vertices[5],1)).subtractLocal(c);
        Vector4f b=vp.mult(new Vector4f(vertices[9],vertices[10],vertices[11],1)).subtractLocal(c);
        Matrix3f projection=new Matrix3f(a.x,b.x,c.x,a.y,b.y,c.y,a.w,b.w,c.w);
        if(!Float.isFinite(projection.determinant())||Math.abs(projection.determinant())<1e-12f)return;
        screenToUV=projection.invert();coverDepth=screenToUV.transposeNew().mult(new Vector3f(a.z,b.z,c.z));
        material.setParam("ArtScreenToUV",com.jme3.shader.VarType.Matrix3,screenToUV);material.setBoolean("UseArtProjection",true);
    }
    public void setCovers(java.util.List<Strip3DView> covers){
        if(bakedView!=null)return;
        int count=0;
        if(surfaceDepth)covers=java.util.List.of();
        for(Strip3DView cover:covers){
            if(cover==this||cover.usesSurfaceDepth()||!cover.isCoverVisible())continue;
            if(count>=VolumeOcclusion.MAX_COVERS)break;
            material.setTexture("CoverMap"+count,cover.colorTexture);
            material.setParam("CoverScreenToUV"+count,com.jme3.shader.VarType.Matrix3,cover.screenToUV);
            material.setVector3("CoverDepth"+count,cover.coverDepth);count++;
        }
        for(int i=count;i<coverCount;i++){material.clearParam("CoverMap"+i);material.clearParam("CoverScreenToUV"+i);material.clearParam("CoverDepth"+i);}
        if(count!=coverCount)material.setInt("CoverCount",count);coverCount=count;
    }
    public Geometry getGeometry(){return bakedView==null?geometry:bakedView.geometry();}
    public void setVisible(boolean v){if(bakedView!=null)bakedView.setVisible(v);geometry.setCullHint(v&&bakedView==null?Spatial.CullHint.Inherit:Spatial.CullHint.Always);}
    public void invalidate(){invalidated=true;}
    public String getError(){return bakedView==null?error:bakedView.status();}
    public void cleanup(){if(bakedView!=null){bakedView.cleanup();bakedView=null;}geometry.removeFromParent();if(colorTexture.getImage()!=null)colorTexture.getImage().dispose();if(normalTexture.getImage()!=null)normalTexture.getImage().dispose();}
    private void load(Strip3DDefinition d){
        if(!invalidated&&d.sameSampling(loaded))return;error=null;previous=null;
        for(var face:Strip3DDefinition.Surface.values()){int i=face.ordinal();var layer=d.layer(face);colors[i]=null;normals[i]=null;
            try{colors[i]=layer.getColorPath()==null?Strip3DTextures.blankStrip(d):Strip3DTextures.readImage(assets,layer.getColorPath());
                if(colors[i].getWidth()!=4*d.getFrameWidth()||colors[i].getHeight()!=d.getFrameHeight())throw new java.io.IOException("颜色图应为 "+4*d.getFrameWidth()+"×"+d.getFrameHeight());
            }catch(Exception e){error=face+"："+e.getMessage();colors[i]=Strip3DTextures.blankStrip(d);}
            try{if(layer.getNormalPath()!=null){normals[i]=Strip3DTextures.readImage(assets,layer.getNormalPath());if(normals[i].getWidth()!=colors[i].getWidth()||normals[i].getHeight()!=colors[i].getHeight())throw new java.io.IOException("法线与颜色图尺寸不一致");}}
            catch(Exception e){error=face+"："+e.getMessage();normals[i]=null;}
        }loaded=d.copy();invalidated=false;
    }
    private void upload(Texture2D texture,BufferedImage source,boolean normal){
        if(!normal)composedColor=source;
        Image image=texture.getImage();int w=source.getWidth(),h=source.getHeight();
        if(image==null||image.getWidth()!=w||image.getHeight()!=h){if(image!=null)image.dispose();image=new Image(Image.Format.RGBA8,w,h,BufferUtils.createByteBuffer(w*h*4),normal?ColorSpace.Linear:ColorSpace.sRGB);texture.setImage(image);}
        ByteBuffer data=image.getData(0);data.clear();for(int y=h-1;y>=0;y--)for(int x=0;x<w;x++){int c=source.getRGB(x,y);data.put((byte)(c>>16)).put((byte)(c>>8)).put((byte)c).put((byte)(c>>>24));}data.flip();image.setUpdateNeeded();
    }
    public void update(VolumeDefinition d,Vector3f position,Quaternion rotation,Vector3f scale,Camera camera,boolean visible,boolean selected){
        if(d.isBakedPixelsEnabled()){
            geometry.setCullHint(Spatial.CullHint.Always);
            if(bakedView!=null)bakedView.update(d,position,rotation,scale,camera,visible,selected);
            return;
        }
        if(bakedView!=null){bakedView.cleanup();bakedView=null;}

        if(surfaceDepth!=d.isSurfaceDepthEnabled()){surfaceDepth=d.isSurfaceDepthEnabled();invalidated=true;}
        material.setBoolean("SurfaceDepth",surfaceDepth);
        setVisible(visible);if(!visible)return;var strip=d.getStrip3D();load(strip);
        if(surfaceDepth)geometry.setCullHint(Spatial.CullHint.Never);
        Transform world=new Transform(position,rotation,scale).combineWithParent(parent.getWorldTransform());Vector3f ws=world.getScale();if(Math.abs(ws.x*ws.y*ws.z)<1e-9f){setVisible(false);return;}
        containmentProxy=VolumeContainment.Proxy.of(d,world);
        // One viewing direction for every part, even in perspective. Per-part eye rays
        // make adjacent parts sample different faces and make panning spin their artwork.
        Vector3f center=world.getTranslation();Quaternion q=world.getRotation();Vector3f toward=camera.getDirection().negate();
        Vector3f local=q.inverse().mult(toward).normalizeLocal();double pitch=Math.toDegrees(Math.asin(Math.max(-1,Math.min(1,local.y))));
        if(local.x*local.x+local.z*local.z>1e-8)lastYaw=(float)Math.toDegrees(Math.atan2(local.x,local.z));
        else {Vector3f poleUp=q.inverse().mult(camera.getUp());double sign=local.y>=0?-1:1;lastYaw=(float)Math.toDegrees(Math.atan2(poleUp.x*sign,poleUp.z*sign));}
        TriStripSampler.Layout layout=TriStripSampler.layout(strip,lastYaw,pitch);
        if(!layout.samePixels(previous)){var artwork=strip;
            if(surfaceDepth){artwork=strip.copy();artwork.setOutlineEnabled(false);}
            upload(colorTexture,TriStripSampler.compose(artwork,layout,colors,false),false);upload(normalTexture,TriStripSampler.compose(strip,layout,normals,true),true);material.setTexture("ColorMap",colorTexture);material.setTexture("NormalMap",normalTexture);material.setVector2("TexelSize",new Vector2f(1f/layout.canvasWidth(),1f/layout.canvasHeight()));previous=layout;}
        Vector3f right=camera.getLeft().negate(),up=camera.getUp();
        // Apply only the bone's twist around the viewing axis to the billboard (limb roll).
        float projection=q.getX()*toward.x+q.getY()*toward.y+q.getZ()*toward.z;
        if(Math.abs(projection)+Math.abs(q.getW())>1e-6){Quaternion roll=new Quaternion().fromAngleAxis(2*FastMath.atan2(projection,q.getW()),toward);right=roll.mult(right);up=roll.mult(up);}
        // A source window of N columns always fills the part's reference width.
        // Canvas padding is transparent space, not additional body width.
        float pixelWidth=d.width()/strip.getSampleWidth()*Math.abs(ws.x);
        float halfW=layout.canvasWidth()*.5f*pixelWidth,halfH=layout.canvasHeight()*.5f*d.height()/strip.getFrameHeight()*Math.abs(ws.y);
        // Integer centring can leave one more padding column on the right. Compensate
        // geometrically so odd/even window sizes stay centred on the same bone.
        float centerOffset=((layout.canvasWidth()-layout.columns().length)&1)*.5f*pixelWidth;
        boolean moved=false;for(int i=0;i<4;i++){float x=(i==0||i==3?-halfW:halfW)+centerOffset,y=i<2?-halfH:halfH;Vector3f v=center.add(right.mult(x)).addLocal(up.mult(y));for(int k=0;k<3;k++){float n=v.get(k);moved|=vertices[i*3+k]!=n;vertices[i*3+k]=n;}}
        if(surfaceDepth){
            surfaceProjection=SurfaceProjection.create(d,world,camera,right,layout);
            surfaceCamera=camera;surfaceDefinition=d;surfaceWorld=world;
            material.setMatrix4("ProxyToClip",surfaceProjection.proxyToClip());material.setMatrix4("ClipToProxy",surfaceProjection.clipToProxy());
            material.setInt("ProxyShape",d.getShape()==VolumeDefinition.Shape.BOX?1:0);material.setFloat("TopRatio",d.effectiveTopRatio());
            material.setBoolean("OutlineEnabled",strip.isOutlineEnabled());
            material.setBoolean("SnapSurfaceContour",d.isSurfaceContourSnapped());
            material.setParam("ArtToScreen",com.jme3.shader.VarType.Matrix3,surfaceProjection.screenToArt().invert());
            material.setParam("ArtScreenToUV",com.jme3.shader.VarType.Matrix3,surfaceProjection.screenToArt());material.setBoolean("UseArtProjection",true);
            float vw=camera.getWidth()*(camera.getViewPortRight()-camera.getViewPortLeft()),vh=camera.getHeight()*(camera.getViewPortTop()-camera.getViewPortBottom());
            material.setVector4("CoverViewport",new Vector4f(camera.getWidth()*camera.getViewPortLeft(),camera.getHeight()*camera.getViewPortBottom(),vw,vh));
            int grid=SurfaceProjection.pixelStep(camera);material.setFloat("PixelStep",grid);
            // Include a complete border cell; raster coverage must never cut a shared pixel cell.
            float extension=d.isSurfaceContourSnapped()?.5f*surfaceProjection.artPixelSize().length():0;
            float padding=(float)Math.ceil(extension/grid)*grid+2*grid;
            Vector4f b=surfaceProjection.bounds();float padx=2*padding/vw,pady=2*padding/vh;
            float l=Math.max(-1,(float)Math.floor((b.x+1)*vw/(2*grid))*2*grid/vw-1-padx);
            float r=Math.min(1,(float)Math.ceil((b.z+1)*vw/(2*grid))*2*grid/vw-1+padx);
            float bottom=Math.max(-1,(float)Math.floor((b.y+1)*vh/(2*grid))*2*grid/vh-1-pady);
            float top=Math.min(1,(float)Math.ceil((b.w+1)*vh/(2*grid))*2*grid/vh-1+pady);
            material.setVector4("ScreenBounds",new Vector4f(l,bottom,r,top));
        }else surfaceProjection=null;
        if(moved){positions.clear();positions.put(vertices).flip();mesh.getBuffer(VertexBuffer.Type.Position).updateData(positions);mesh.updateBound();}
        if(surfaceDepth){
            // Keep world-space bounds meaningful for scene queries; only the shader draws the screen rectangle.
            var bound=new com.jme3.bounding.BoundingBox(Vector3f.ZERO,d.width()/2,d.height()/2,d.depth()/2);
            mesh.setBound(bound.transform(world,null));
        }
        prepareCoverProjection(camera);
        material.setMatrix4("ArtToWorld",world.toTransformMatrix());material.setParam("NormalRotation",com.jme3.shader.VarType.Matrix3,q.toRotationMatrix());material.setBoolean("UseLighting",d.isLightingEnabled());material.setBoolean("Selected",selected);
        float yaw=d.getLightYaw()*FastMath.DEG_TO_RAD,elev=d.getLightElevation()*FastMath.DEG_TO_RAD;material.setVector3("LightDirection",new Vector3f(FastMath.sin(yaw)*FastMath.cos(elev),FastMath.sin(elev),FastMath.cos(yaw)*FastMath.cos(elev)));material.setFloat("LightStrength",d.getLightStrength());
    }
}
