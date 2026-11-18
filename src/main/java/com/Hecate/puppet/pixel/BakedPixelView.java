package com.Hecate.puppet.pixel;

import com.Hecate.puppet.volume.*;
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

/** Runtime lookup only. prepare() is the separate appearance-change boundary; update() cannot bake. */
public final class BakedPixelView {
    private final AssetManager assets;
    private final Node parent;
    private final Geometry geometry;
    private final Material material;
    private PixelAtlasCache.Lease lease;
    private VolumeDefinition prepared;
    private String error;
    private boolean visible;
    private int priority;
    private Camera camera;
    private Transform world;
    private final Matrix3f[] screenToUV=new Matrix3f[2];
    private PixelViewSelector.Selection selection;
    private final BufferedImage[] sources=new BufferedImage[3];
    private final Texture2D[] colors=new Texture2D[3];
    private final PixelSourceCache.Lease[] sourceLeases=new PixelSourceCache.Lease[3];
    private static long totalEditBytes;
    private long editBytes;
    private Texture2D editTexture,paletteTexture;
    private boolean hasEditedAtlas;
    private byte[][] editedRows;private PixelVerticalSampling.Rows sampledRows;private String rowKey;private Texture2D rowTexture;
    public BakedPixelView(AssetManager assets,Node parent,String name) {
        this.assets=assets;this.parent=parent;
        Mesh mesh=new Mesh();mesh.setBuffer(VertexBuffer.Type.Position,3,new float[]{0,0,0,1,0,0,1,1,0,0,1,0});mesh.setBuffer(VertexBuffer.Type.Index,3,new int[]{0,1,2,0,2,3});mesh.updateBound();
        geometry=new Geometry(name+"_BakedPixels",mesh){
            @Override public int collideWith(com.jme3.collision.Collidable target,com.jme3.collision.CollisionResults results){return target instanceof Ray ray?pick(ray,results):0;}
        };
        geometry.setIgnoreTransform(true);geometry.setCullHint(Spatial.CullHint.Always);geometry.setShadowMode(com.jme3.renderer.queue.RenderQueue.ShadowMode.Off);
        material=new Material(assets,"MatDefs/PuppetBakedPixels.j3md");geometry.setMaterial(material);
        var state=material.getAdditionalRenderState();state.setFaceCullMode(RenderState.FaceCullMode.Off);state.setDepthTest(true);state.setDepthWrite(true);
        geometry.setQueueBucket(com.jme3.renderer.queue.RenderQueue.Bucket.Opaque);parent.attachChild(geometry);
    }
    public Geometry geometry(){return geometry;} public String error(){return error;}
    public PixelAtlas atlas(){return lease==null?null:lease.atlas();}
    public void setPriority(int value){priority=value;}
    public void setVisible(boolean value){visible=value;geometry.setCullHint(value&&lease!=null&&error==null?Spatial.CullHint.Never:Spatial.CullHint.Always);}
    /** Called on creation, shape edits, imports and undo/redo; never from animation pose changes. */
    public void prepare(VolumeDefinition d) {
        error=null;
        try {
            PixelMetrics.normalize(d);
            if(lease==null||!lease.atlas().key().equals(PixelAtlasBaker.key(d))){
                // Release the old lease before acquiring, so shape edits can evict obsolete data at the cap.
                if(lease!=null){lease.close();lease=null;}
                lease=PixelAtlasCache.SHARED.acquire(d);material.setTexture("DepthAtlas",lease.atlas().texture());
            }else if(d.getImportedMesh()!=null){
                lease.atlas().restoreJointCentredEdits(d.getBakedPixels(),PixelAtlasBaker.jointCentredKey(d));
            }
            int n=lease.atlas().size();material.setFloat("AtlasSize",n);material.setFloat("YawViews",PixelAtlas.YAW_VIEWS);material.setVector2("AtlasTiles",new Vector2f(PixelAtlas.COLUMNS,PixelAtlas.ROWS));material.setFloat("Radius",lease.atlas().radius());
            for(var surface:Strip3DDefinition.Surface.values()) {
                int i=surface.ordinal();var layer=d.getStrip3D().layer(surface);
                if(sourceLeases[i]!=null){sourceLeases[i].close();sourceLeases[i]=null;}
                sourceLeases[i]=PixelSourceCache.SHARED.acquire(assets,layer.getColorPath(),d.getStrip3D());sources[i]=sourceLeases[i].image();colors[i]=sourceLeases[i].texture();material.setTexture("Color"+i,colors[i]);
                material.setVector4("Source"+i,new Vector4f(sources[i].getWidth(),sources[i].getHeight(),layer.getOffsetX(),layer.getOffsetY()));
            }
            uploadEdits(d);uploadPalette(d);prepared=d.copy();rowKey=null;
            int stale=d.getBakedPixels().staleEdits(lease.atlas().key());
            // A stale hand edit is never silently applied to a different body shape.
            if(stale>0)warning="有 "+stale+" 张手改剪影来自旧方向或与当前形状不符，已保留原修改，请重新检查。";else warning=null;

        }catch(Exception e){error=e.getMessage();}
        setVisible(visible);
    }
    private String warning;
    public String status(){return error!=null?error:warning;}
    private void uploadEdits(VolumeDefinition d){
        if(editTexture!=null){editTexture.getImage().dispose();editTexture=null;}releaseEditBytes();
        PixelAtlas atlas=lease.atlas();int n=atlas.size();boolean hasEdits=d.getStrip3D().hasSelectionMask();for(int i=0;i<PixelAtlas.VIEWS;i++)hasEdits|=d.getBakedPixels().getEdit(i)!=null;
        editedRows=hasEdits?new byte[PixelAtlas.VIEWS][n]:null;hasEditedAtlas=hasEdits;material.setBoolean("HasEditedAtlas",hasEdits);
        if(!hasEdits){material.setTexture("EditAtlas",atlas.texture());return;}
        reserveEditBytes((long)n*n*PixelAtlas.VIEWS*4);ByteBuffer bytes=BufferUtils.createByteBuffer(n*n*PixelAtlas.VIEWS*4);
        for(int view=0;view<PixelAtlas.VIEWS;view++){
            byte[] cells=PixelEdits.resolve(atlas,d,view);
            for(int y=0;y<n;y++)for(int x=0;x<n;x++){int o=atlas.offset(view,x,y),i=(y*n+x)*4;for(int c=0;c<4;c++)bytes.put(o+c,cells[i+c]);int layer=(cells[i+3]&255)/64-1;if(layer>=0)editedRows[view][y]|=(byte)(1<<layer);}
        }
        editTexture=new Texture2D(new Image(Image.Format.RGBA8,n*PixelAtlas.COLUMNS,n*PixelAtlas.ROWS,bytes,ColorSpace.Linear));Strip3DTextures.nearest(editTexture);material.setTexture("EditAtlas",editTexture);
    }
    private void uploadPalette(VolumeDefinition d){
        if(paletteTexture!=null)paletteTexture.getImage().dispose();
        ByteBuffer data=BufferUtils.createByteBuffer(16*2*4);var palette=d.getBakedPixels().getPalette();
        for(int i=0;i<palette.size();i++)for(int row=0;row<2;row++){int rgb=row==0?palette.get(i).source():palette.get(i).target(),o=(row*16+i)*4;data.put(o,(byte)(rgb>>16));data.put(o+1,(byte)(rgb>>8));data.put(o+2,(byte)rgb);data.put(o+3,(byte)255);}
        paletteTexture=new Texture2D(new Image(Image.Format.RGBA8,16,2,data,ColorSpace.sRGB));Strip3DTextures.nearest(paletteTexture);material.setTexture("Palette",paletteTexture);material.setFloat("PaletteCount",palette.size());
    }
    private void reserveEditBytes(long value){synchronized(BakedPixelView.class){if(totalEditBytes+value>16L*1024*1024)throw new IllegalStateException("当前手改剪影达到 16 MB 上限，请减少同时显示的编辑部件");totalEditBytes+=value;editBytes=value;}}
    private void releaseEditBytes(){synchronized(BakedPixelView.class){totalEditBytes-=editBytes;editBytes=0;}}
    public void update(VolumeDefinition d,Vector3f position,Quaternion rotation,Vector3f scale,Camera camera,boolean shown,boolean selected) {
        setVisible(shown);if(!shown||lease==null||error!=null)return;
        if(!lease.atlas().key().equals(PixelAtlasBaker.key(d))){error="形状已改变，请点击重新生成剪影";setVisible(false);return;}
        this.camera=camera;
        // Rigid parts retain their pixel size; signed scale only preserves intentional mirroring.
        world=new Transform(position,rotation,new Vector3f(Math.signum(scale.x),Math.signum(scale.y),Math.signum(scale.z))).combineWithParent(parent.getWorldTransform());
        Vector3f ws=world.getScale();ws.set(Math.signum(ws.x),Math.signum(ws.y),Math.signum(ws.z));world.setScale(ws);
        if(ws.x*ws.y*ws.z==0){setVisible(false);return;}
        Vector3f local=world.getRotation().inverse().mult(camera.getDirection().negate()).multLocal(ws).normalizeLocal();
        Vector3f cameraRight=world.getRotation().inverse().mult(camera.getLeft().negate()).multLocal(ws);
        float yaw=PixelViewSelector.localYaw(local,cameraRight);
        float pitch=FastMath.asin(FastMath.clamp(local.y,-1,1))*FastMath.RAD_TO_DEG;
        PixelAtlas atlas=lease.atlas();int n=atlas.size();selection=PixelViewSelector.select(d.getBakedPixels(),yaw,pitch,n);updateTextureRows(pitch);
        PixelBillboardLayout layout=PixelBillboardLayout.create(d.getBakedPixels(),yaw,pitch,selection,atlas.pixelsPerUnit());
        Vector3f right=world.getRotation().mult(layout.right().mult(ws));
        Matrix4f vp=camera.getViewProjectionMatrix();float r=atlas.radius();
        Transform photoWorld=new Transform(world.transformVector(atlas.centre(),null),world.getRotation(),ws);
        // The shader draws a screen quad. Give ancestor nodes a world bound enclosing
        // both tilted halves and their shared seam, even when the joint is off screen.
        var bound=(com.jme3.bounding.BoundingBox)geometry.getMesh().getBound();
        float extent=2*r;
        if(!bound.getCenter().equals(photoWorld.getTranslation())||bound.getZExtent()!=extent){
            geometry.setModelBound(new com.jme3.bounding.BoundingBox(photoWorld.getTranslation(),extent,extent,extent));
        }
        float left=1,bottom=1,top=-1,rightEdge=-1;
        for(int piece=0;piece<2;piece++){
            Vector3f up=world.getRotation().mult((piece==0?layout.lowerUp():layout.upperUp()).mult(ws));
            Vector3f centre=photoWorld.transformVector(piece==0?layout.lowerCentre():layout.upperCentre(),null);
            Vector3f c=centre.subtract(right.mult(r)).subtractLocal(up.mult(r)),a=right.mult(2*r),b=up.mult(2*r);
            Vector4f cc=vp.mult(new Vector4f(c.x,c.y,c.z,1)),aa=vp.mult(new Vector4f(a.x,a.y,a.z,0)),bb=vp.mult(new Vector4f(b.x,b.y,b.z,0));
            Matrix3f projection=new Matrix3f(aa.x,bb.x,cc.x,aa.y,bb.y,cc.y,aa.w,bb.w,cc.w);
            if(!Float.isFinite(projection.determinant())||Math.abs(projection.determinant())<1e-10f){setVisible(false);return;}
            screenToUV[piece]=projection.invert();
            material.setParam("ScreenToUV"+piece,com.jme3.shader.VarType.Matrix3,screenToUV[piece]);
            if(piece==0&&selection.revealedRows()==n||piece==1&&selection.revealedRows()==0)continue;
            float seam=1-selection.revealedRows()/(float)n;
            for(int i=0;i<4;i++){
                float y=piece==0?(i>=2?seam:0):(i>=2?1:seam);
                Vector4f p=cc.add(aa.mult(i==1||i==2?1:0)).addLocal(bb.mult(y));
                if(p.w<=0){setVisible(false);return;}left=Math.min(left,p.x/p.w);rightEdge=Math.max(rightEdge,p.x/p.w);bottom=Math.min(bottom,p.y/p.w);top=Math.max(top,p.y/p.w);
            }
        }
        float w=camera.getWidth()*(camera.getViewPortRight()-camera.getViewPortLeft()),h=camera.getHeight()*(camera.getViewPortTop()-camera.getViewPortBottom());int step=SurfaceProjection.pixelStep(camera);
        material.setVector4("Bounds",new Vector4f(Math.max(-1,left-4*step/w),Math.max(-1,bottom-4*step/h),Math.min(1,rightEdge+4*step/w),Math.min(1,top+4*step/h)));
        material.setVector4("Viewport",new Vector4f(camera.getWidth()*camera.getViewPortLeft(),camera.getHeight()*camera.getViewPortBottom(),w,h));material.setFloat("PixelStep",step);
        material.setMatrix4("LocalToClip",vp.mult(photoWorld.toTransformMatrix()));
        material.setVector4("ViewChoice",new Vector4f(selection.yawStep(),selection.lowerBand(),selection.upperBand(),selection.revealedRows()));
        for(int i=0;i<2;i++){int view=(i==0?selection.lowerBand():selection.upperBand())*PixelAtlas.YAW_VIEWS+selection.yawStep();material.setVector3("ViewRight"+i,atlas.right(view));material.setVector3("ViewUp"+i,atlas.up(view));material.setVector3("ViewToward"+i,atlas.direction(view));material.setVector4("ViewPlane"+i,atlas.photoPlane(view));material.setVector4("ViewPerspective"+i,atlas.photoPerspective(view));material.setVector2("ViewSampleCentre"+i,atlas.sampleCentre(view));}
        float[] tint=d.getBakedPixels().getTint();material.setColor("Tint",new ColorRGBA(tint[0],tint[1],tint[2],1));
        material.setVector2("TextureOrigin",new Vector2f(d.getBakedPixels().getTextureOriginX(),d.getBakedPixels().getTextureOriginY()));
        material.setFloat("Priority",FastMath.clamp(priority+d.getBakedPixels().getPriority(),-1000,1000));material.setBoolean("Selected",selected);material.setBoolean("Outline",d.getStrip3D().isOutlineEnabled());
        setVisible(true);
    }
    private void updateTextureRows(float pitch){
        int n=lease.atlas().size();String next=selection+"/"+PixelVerticalSampling.shift(lease.atlas(),prepared,pitch);
        if(next.equals(rowKey))return;
        rowKey=next;sampledRows=PixelVerticalSampling.build(lease.atlas(),prepared,selection,pitch,sources,(view,row)->editedRows==null?lease.atlas().rowLayers(view,row):editedRows[view][row]&255);
        if(rowTexture==null||rowTexture.getImage().getWidth()!=n){
            if(rowTexture!=null)rowTexture.getImage().dispose();
            rowTexture=new Texture2D(new Image(Image.Format.RGBA8,n,3,BufferUtils.createByteBuffer(n*3*4),ColorSpace.Linear));Strip3DTextures.nearest(rowTexture);material.setTexture("TextureRows",rowTexture);
        }
        ByteBuffer data=rowTexture.getImage().getData(0);
        for(int layer=0;layer<3;layer++)for(int y=0;y<n;y++){int value=sampledRows.sourceY(layer,y),o=(layer*n+y)*4;data.put(o,(byte)(value>>8));data.put(o+1,(byte)value);data.put(o+2,(byte)0);data.put(o+3,(byte)(value<0?0:255));}
        rowTexture.getImage().setUpdateNeeded();
    }
    private int pick(Ray ray,com.jme3.collision.CollisionResults results){
        if(!visible||camera==null||selection==null||lease==null||error!=null)return 0;
        Vector3f screen=camera.getScreenCoordinates(ray.getOrigin().add(ray.getDirection().mult(10)));
        float w=camera.getWidth()*(camera.getViewPortRight()-camera.getViewPortLeft()),h=camera.getHeight()*(camera.getViewPortTop()-camera.getViewPortBottom());int step=SurfaceProjection.pixelStep(camera);
        float sx=((float)Math.floor((screen.x-camera.getWidth()*camera.getViewPortLeft())/step)+.5f)*step/w*2-1;
        float sy=((float)Math.floor((screen.y-camera.getHeight()*camera.getViewPortBottom())/step)+.5f)*step/h*2-1;
        PixelAtlas atlas=lease.atlas();int n=atlas.size(),x=-1,y=-1;
        for(int piece=0;piece<2;piece++){
            Vector3f uv=screenToUV[piece].mult(new Vector3f(sx,sy,1));
            if(!Float.isFinite(uv.z)||Math.abs(uv.z)<1e-10f)continue;
            int cx=(int)Math.floor(uv.x/uv.z*n+.0001f),cy=n-1-(int)Math.floor(uv.y/uv.z*n+.0001f);
            if(cx>=0&&cy>=0&&cx<n&&cy<n&&(cy<selection.revealedRows())==(piece==1)){x=cx;y=cy;break;}
        }
        if(x<0)return 0;int view=selection.viewForRow(y);
        int o=atlas.offset(view,x,y);ByteBuffer cells=hasEditedAtlas?editTexture.getImage().getData(0):atlas.texture().getImage().getData(0);
        int layer=(cells.get(o+3)&255)/64-1;if(layer<0)return 0;
        BufferedImage source=sources[layer];var l=prepared.getStrip3D().layer(Strip3DDefinition.Surface.values()[layer]);
        int px=PixelRingTexture.sourceX(atlas,prepared,view,layer,x,source.getWidth());
        int py=sampledRows.sourceY(layer,y);if(py<0)return 0;
        if((source.getRGB(px,py)>>>24)<128)return 0;
        int encoded=((cells.get(o)&255)<<16)|((cells.get(o+1)&255)<<8)|(cells.get(o+2)&255);float depth=(encoded/16777215f*2-1)*atlas.radius();
        Vector3f point=atlas.point(view,x,y,depth);
        Vector3f hit=world.transformVector(point,null);float distance=hit.distance(ray.getOrigin());if(distance>ray.getLimit())return 0;
        results.addCollision(new com.jme3.collision.CollisionResult(geometry,hit,distance,0));return 1;
    }
    public void cleanup(){geometry.removeFromParent();if(rowTexture!=null)rowTexture.getImage().dispose();if(lease!=null){lease.close();lease=null;}for(var source:sourceLeases)if(source!=null)source.close();if(editTexture!=null)editTexture.getImage().dispose();if(paletteTexture!=null)paletteTexture.getImage().dispose();releaseEditBytes();}
}
