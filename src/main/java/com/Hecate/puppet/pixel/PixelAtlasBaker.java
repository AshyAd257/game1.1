package com.Hecate.puppet.pixel;

import com.Hecate.puppet.volume.*;
import com.jme3.math.*;
import com.jme3.scene.Mesh;
import com.jme3.util.BufferUtils;
import java.nio.ByteBuffer;
import java.util.Arrays;

/** Authoring-only mild-perspective triangle rasterizer. This class is never called by pose updates. */
public final class PixelAtlasBaker {
    private PixelAtlasBaker() {}
    public static String key(VolumeDefinition d) {
        String key=jointCentredKey(d);
        return d.getImportedMesh()==null?key:key+"/mesh-centre";
    }
    static String jointCentredKey(VolumeDefinition d) {
        var p=d.getBakedPixels();return "pixel-v6/"+(d.getImportedMesh()==null?d.getShape():d.getImportedMesh().key())+"/"+d.getWidthPx()+"/"+d.getHeightPx()+"/"+d.getDepthPx()+"/"+d.effectiveTopRatio()+"/"+d.getPixelsPerUnit()+"/"+p.elevation(0)+"/"+p.elevation(2)+"/"+p.getPerspectiveStrength();
    }
    public static int size(VolumeDefinition d) {
        int size=(int)Math.ceil(Math.sqrt((double)d.getWidthPx()*d.getWidthPx()+(double)d.getHeightPx()*d.getHeightPx()+(double)d.getDepthPx()*d.getDepthPx()))+4;
        if(d.getImportedMesh()!=null)size=2*(int)Math.ceil(d.getImportedMesh().captureRadius(d.getImportedMesh().sizeScale(d.getWidthPx(),d.getHeightPx(),d.getDepthPx()))*d.getPixelsPerUnit())+4;
        // Never silently resample, which would violate the character's shared artwork grid.
        size=(int)Math.ceil((size-4)/(1-d.getBakedPixels().getPerspectiveStrength()))+4;
        if(size>256)throw new IllegalArgumentException("剪影包围网格超过 256×256；请减小部件像素尺寸（不会自动缩小像素）");
        return size;
    }
    public static long estimateBytes(VolumeDefinition d){int n=size(d);return (long)n*n*PixelAtlas.VIEWS*4;}
    public static PixelAtlas bake(VolumeDefinition d) { return bakeMesh(VolumeMeshFactory.create(d),d); }
    /** Also usable by asset importers for a rigid triangle mesh in the agreed local coordinates. */
    public static PixelAtlas bakeMesh(Mesh mesh, VolumeDefinition d) {
        d.validate();int n=size(d);float ppu=d.getPixelsPerUnit(),radius=n/(2*ppu);
        ByteBuffer pixels=BufferUtils.createByteBuffer(n*n*PixelAtlas.VIEWS*4);
        float[] depths=new float[n*n];int[] layers=new int[n*n];
        Vector4f[] planes=new Vector4f[PixelAtlas.VIEWS],perspectives=new Vector4f[PixelAtlas.VIEWS];
        Vector2f[] oldGridOffsets=new Vector2f[PixelAtlas.VIEWS];
        Vector3f a=new Vector3f(),b=new Vector3f(),c=new Vector3f();
        Vector3f min=new Vector3f(Float.POSITIVE_INFINITY,Float.POSITIVE_INFINITY,Float.POSITIVE_INFINITY),max=min.negate();
        for(int tri=0;tri<mesh.getTriangleCount();tri++){mesh.getTriangle(tri,a,b,c);for(var v:new Vector3f[]{a,b,c}){min.minLocal(v);max.maxLocal(v);}}
        Vector3f centre=min.add(max).multLocal(.5f);float captureRadius=0;
        for(int tri=0;tri<mesh.getTriangleCount();tri++){mesh.getTriangle(tri,a,b,c);captureRadius=Math.max(captureRadius,Math.max(a.distance(centre),Math.max(b.distance(centre),c.distance(centre))));}
        float inverseDistance=captureRadius>1e-7f?d.getBakedPixels().getPerspectiveStrength()/captureRadius:0;
        for(int view=0;view<PixelAtlas.VIEWS;view++) {
            Arrays.fill(depths,Float.NEGATIVE_INFINITY);Arrays.fill(layers,0);
            Vector3f toward=PixelViewSelector.direction(view%PixelAtlas.YAW_VIEWS*PixelAtlas.YAW_DEGREES,d.getBakedPixels().elevation(view/PixelAtlas.YAW_VIEWS));
            Vector3f right=PixelViewSelector.right(view%PixelAtlas.YAW_VIEWS*PixelAtlas.YAW_DEGREES),up=toward.cross(right).normalizeLocal();
            perspectives[view]=new Vector4f(0,0,0,inverseDistance);
            float left=Float.POSITIVE_INFINITY,top=left,rightEdge=Float.NEGATIVE_INFINITY,bottom=rightEdge;
            for(int tri=0;tri<mesh.getTriangleCount();tri++){
                mesh.getTriangle(tri,a,b,c);
                for(Vector3f v:new Vector3f[]{a,b,c}){
                    v.subtractLocal(centre);
                    float magnification=1/(1-v.dot(toward)*inverseDistance);
                    float x=v.dot(right)*magnification*ppu+n*.5f;
                    float y=n*.5f-v.dot(up)*magnification*ppu;
                    left=Math.min(left,x);rightEdge=Math.max(rightEdge,x);
                    top=Math.min(top,y);bottom=Math.max(bottom,y);
                }
            }
            float width=rightEdge-left,height=bottom-top;int columns=Math.round(width+.00001f),rows=Math.round(height+.00001f);
            float gridX=(left+rightEdge-columns)*.5f,gridY=(top+bottom-rows)*.5f;
            float startX=Math.round(gridX),startY=Math.round(gridY);
            oldGridOffsets[view]=new Vector2f(gridX-startX+centre.dot(right)*ppu-n*.5f,
                                               gridY-startY-centre.dot(up)*ppu-n*.5f);
            planes[view]=new Vector4f(1,1,0,0);if(columns<1||rows<1)continue;
            float scaleX=columns/width,scaleY=rows/height;
            // Only the projected photo is rounded. The inverse preserves original 3D depth positions.
            float inverseX=1/scaleX,inverseY=1/scaleY;
            planes[view]=new Vector4f(inverseX,inverseY,(left-startX*inverseX+n*.5f*(inverseX-1))/ppu,(n*.5f*(1-inverseY)-top+startY*inverseY)/ppu);
            for(int tri=0;tri<mesh.getTriangleCount();tri++) {
                mesh.getTriangle(tri,a,b,c);
                a.subtractLocal(centre);b.subtractLocal(centre);c.subtractLocal(centre);
                Vector3f normal=b.subtract(a).cross(c.subtract(a)).normalizeLocal();
                int layer=normal.y>.7f?1:normal.y<-.7f?3:2;
                float az=a.dot(toward),bz=b.dot(toward),cz=c.dot(toward);
                float ia=1/(1-az*inverseDistance),ib=1/(1-bz*inverseDistance),ic=1/(1-cz*inverseDistance);
                float ax=a.dot(right)*ia*ppu+n*.5f,ay=n*.5f-a.dot(up)*ia*ppu;
                float bx=b.dot(right)*ib*ppu+n*.5f,by=n*.5f-b.dot(up)*ib*ppu;
                float cx=c.dot(right)*ic*ppu+n*.5f,cy=n*.5f-c.dot(up)*ic*ppu;
                ax=startX+(ax-left)*scaleX;ay=startY+(ay-top)*scaleY;bx=startX+(bx-left)*scaleX;by=startY+(by-top)*scaleY;cx=startX+(cx-left)*scaleX;cy=startY+(cy-top)*scaleY;
                float denominator=(by-cy)*(ax-cx)+(cx-bx)*(ay-cy);if(Math.abs(denominator)<1e-7f)continue;
                boolean edgeA=topLeft(bx,by,cx,cy,denominator),edgeB=topLeft(cx,cy,ax,ay,denominator),edgeC=topLeft(ax,ay,bx,by,denominator);
                int minX=Math.max(0,(int)Math.floor(Math.min(ax,Math.min(bx,cx)))),maxX=Math.min(n-1,(int)Math.ceil(Math.max(ax,Math.max(bx,cx))));
                int minY=Math.max(0,(int)Math.floor(Math.min(ay,Math.min(by,cy)))),maxY=Math.min(n-1,(int)Math.ceil(Math.max(ay,Math.max(by,cy))));
                for(int y=minY;y<=maxY;y++)for(int x=minX;x<=maxX;x++) {
                    float wa=((by-cy)*(x+.5f-cx)+(cx-bx)*(y+.5f-cy))/denominator;
                    float wb=((cy-ay)*(x+.5f-cx)+(ax-cx)*(y+.5f-cy))/denominator,wc=1-wa-wb;
                    if(wa<-.00001f||wb<-.00001f||wc<-.00001f)continue;
                    // Half-open edges keep odd pixel widths exact and shared diagonals covered once.
                    if(wa<=.00001f&&!edgeA||wb<=.00001f&&!edgeB||wc<=.00001f&&!edgeC)continue;
                    float z=(wa*az*ia+wb*bz*ib+wc*cz*ic)/(wa*ia+wb*ib+wc*ic);int i=y*n+x;if(z>depths[i]){depths[i]=z;layers[i]=layer;}
                }
            }
            for(int y=0;y<n;y++)for(int x=0;x<n;x++) {
                int i=y*n+x,o=(((view/PixelAtlas.COLUMNS)*n+(n-1-y))*(n*PixelAtlas.COLUMNS)+(view%PixelAtlas.COLUMNS)*n+x)*4;
                if(layers[i]==0)continue;
                int encoded=Math.round(Math.max(0,Math.min(1,(depths[i]/radius+1)*.5f))*16777215);
                pixels.put(o,(byte)(encoded>>16));pixels.put(o+1,(byte)(encoded>>8));pixels.put(o+2,(byte)encoded);pixels.put(o+3,(byte)(layers[i]*64));
            }
        }
        return new PixelAtlas(key(d),n,ppu,d.getBakedPixels(),pixels,planes,perspectives,centre,oldGridOffsets);
    }
    private static boolean topLeft(float ax,float ay,float bx,float by,float area){float dx=bx-ax,dy=by-ay;if(area<0){dx=-dx;dy=-dy;}return dy<0||dy==0&&dx>0;}
}
