package com.Hecate.puppet.volume;

import com.jme3.math.*;
import com.jme3.renderer.Camera;

/** Screen-space artwork fitted to a real, closed volume. No cylindrical UV compression. */
public final class SurfaceProjection {
    private SurfaceProjection() {}
    public record Result(Matrix4f proxyToClip, Matrix4f clipToProxy, Matrix3f screenToArt,
                         Vector4f bounds, Vector2f artPixelSize) {}
    public record Hit(Vector3f proxyPoint,float depth) {}

    /** Round the silhouette to artwork cells; preserve physical depth wherever the ray hits. */
    public static Hit hitArtwork(Result projection,VolumeDefinition d,float x,float y,int width,int height){
        if(!d.isSurfaceContourSnapped())return hit(projection,d,x,y);
        Vector3f uv=projection.screenToArt.mult(new Vector3f(x,y,1));
        float u=((float)Math.floor(uv.x/uv.z*width+.0001f)+.5f)/width;
        float v=((float)Math.floor(uv.y/uv.z*height+.0001f)+.5f)/height;
        Vector3f centre=projection.screenToArt.invert().mult(new Vector3f(u,v,1));
        Hit centreHit=hit(projection,d,centre.x/centre.z,centre.y/centre.z);
        if(centreHit==null)return null;
        Hit physical=hit(projection,d,x,y);
        return physical==null?centreHit:physical;
    }

    /** CPU counterpart used for editor picking. The ray starts on the near plane, just like rendering. */
    public static Hit hit(Result projection,VolumeDefinition d,float screenX,float screenY) {
        Vector4f near=projection.clipToProxy.mult(new Vector4f(screenX,screenY,-1,1));
        Vector4f far=projection.clipToProxy.mult(new Vector4f(screenX,screenY,1,1));
        if(Math.abs(near.w)<1e-12||Math.abs(far.w)<1e-12)return null;
        Vector3f o=new Vector3f(near.x,near.y,near.z).divideLocal(near.w);
        Vector3f ray=new Vector3f(far.x,far.y,far.z).divideLocal(far.w).subtractLocal(o).normalizeLocal();
        // Match the GPU's closed-volume cap at the near plane, including editor picking.
        double nearRadius=(d.effectiveTopRatio()+1)/2.0+(d.effectiveTopRatio()-1)/2.0*o.y;
        boolean nearInside=d.getShape()==VolumeDefinition.Shape.BOX
            ? Math.abs(o.x)<=1&&Math.abs(o.y)<=1&&Math.abs(o.z)<=1
            : Math.abs(o.y)<=1&&o.x*o.x+o.z*o.z<=nearRadius*nearRadius;
        if(nearInside)return new Hit(o,0);
        double t=Double.POSITIVE_INFINITY;
        if(d.getShape()==VolumeDefinition.Shape.BOX){
            double lo=Double.NEGATIVE_INFINITY,hi=Double.POSITIVE_INFINITY;
            for(int i=0;i<3;i++){
                if(Math.abs(ray.get(i))<1e-8){if(Math.abs(o.get(i))>1)return null;}
                else {double a=(-1-o.get(i))/ray.get(i),b=(1-o.get(i))/ray.get(i);lo=Math.max(lo,Math.min(a,b));hi=Math.min(hi,Math.max(a,b));}
            }
            if(hi<Math.max(lo,0))return null;t=lo>=0?lo:hi;
        }else{
            double k=(d.effectiveTopRatio()-1)/2.0,r=(d.effectiveTopRatio()+1)/2.0+k*o.y;
            double a=ray.x*ray.x+ray.z*ray.z-k*k*ray.y*ray.y;
            double b=2*(o.x*ray.x+o.z*ray.z-r*k*ray.y),c=o.x*o.x+o.z*o.z-r*r;
            double discriminant=b*b-4*a*c;
            if(Math.abs(a)<1e-7){if(Math.abs(b)>1e-8)t=side(-c/b,o,ray,t);}
            else if(discriminant>=0){double q=-.5*(b+Math.copySign(Math.sqrt(discriminant),b));
                if(Math.abs(q)>1e-12){t=side(q/a,o,ray,t);t=side(c/q,o,ray,t);}else t=side(-b/(2*a),o,ray,t);
            }
            if(Math.abs(ray.y)>1e-8)for(int cap=-1;cap<=1;cap+=2){
                double candidate=(cap-o.y)/ray.y,x=o.x+candidate*ray.x,z=o.z+candidate*ray.z,radius=cap<0?1:d.effectiveTopRatio();
                if(candidate>=0&&x*x+z*z<=radius*radius+1e-7)t=Math.min(t,candidate);
            }
        }
        if(!Double.isFinite(t))return null;
        Vector3f point=o.add(ray.mult((float)t));Vector4f clip=projection.proxyToClip.mult(new Vector4f(point.x,point.y,point.z,1));
        float depth=clip.z/clip.w*.5f+.5f;
        return clip.w>0&&depth>=0&&depth<=1?new Hit(point,depth):null;
    }
    private static double side(double t,Vector3f o,Vector3f ray,double best){
        double y=o.y+t*ray.y;return t>=0&&y>=-1.000001&&y<=1.000001?Math.min(best,t):best;
    }

    /** All puppets in a viewport use the same integer screen grid, independent of part size. */
    public static int pixelStep(Camera camera) {
        return Math.max(1, Math.round(camera.getHeight() * (camera.getViewPortTop()-camera.getViewPortBottom()) / 360f));
    }

    public static Result create(VolumeDefinition d, Transform world, Camera camera,
                                Vector3f artRight, TriStripSampler.Layout layout) {
        Matrix4f dimensions=new Matrix4f();
        dimensions.setScale(new Vector3f(d.width()/2,d.height()/2,d.depth()/2));
        Matrix4f proxy=camera.getViewProjectionMatrix().mult(world.toTransformMatrix()).mult(dimensions);
        float vw=camera.getWidth()*(camera.getViewPortRight()-camera.getViewPortLeft());
        float vh=camera.getHeight()*(camera.getViewPortTop()-camera.getViewPortBottom());
        float c=artRight.dot(camera.getLeft().negate()), s=artRight.dot(camera.getUp());
        float length=FastMath.sqrt(c*c+s*s);c/=length;s/=length;
        // Rotated artwork axes measured in viewport pixels (aspect ratio must not skew roll).
        Matrix4f axes=new Matrix4f(c*vw/2,s*vh/2,0,0,-s*vw/2,c*vh/2,0,0,0,0,1,0,0,0,0,1);
        Vector4f range=bounds(axes.mult(proxy),d.getShape(),d.effectiveTopRatio());
        if(range==null)range=new Vector4f(-vw,-vh,vw,vh); // Camera inside / near-plane crossing.
        float width=Math.max(.0001f,range.z-range.x),height=Math.max(.0001f,range.w-range.y);
        int cols=layout.columns().length,rows=layout.rows().length;
        float sx=cols/(layout.canvasWidth()*width),sy=rows/(layout.canvasHeight()*height);
        float left=(layout.canvasWidth()-cols)/2;
        // BufferedImage is top-down; upload flips it to the GL bottom-left origin.
        float bottom=layout.canvasHeight()-(layout.canvasHeight()-rows)/2-rows;
        Matrix3f art=new Matrix3f(sx*c*vw/2,sx*s*vh/2,left/layout.canvasWidth()-sx*range.x,
                                 -sy*s*vw/2,sy*c*vh/2,bottom/layout.canvasHeight()-sy*range.y,0,0,1);
        Vector4f screen=bounds(proxy,d.getShape(),d.effectiveTopRatio());
        if(screen==null)screen=new Vector4f(-1,-1,1,1);
        return new Result(proxy,proxy.invert(),art,screen,new Vector2f(width/cols,height/rows));
    }

    /** Exact projected extrema of the two cap circles, or eight box corners. Null crosses eye plane. */
    static Vector4f bounds(Matrix4f m,VolumeDefinition.Shape shape,float ratio) {
        float[] result={Float.POSITIVE_INFINITY,Float.POSITIVE_INFINITY,Float.NEGATIVE_INFINITY,Float.NEGATIVE_INFINITY};
        if(shape==VolumeDefinition.Shape.BOX){
            for(int x=-1;x<=1;x+=2)for(int y=-1;y<=1;y+=2)for(int z=-1;z<=1;z+=2){
                Vector4f p=m.mult(new Vector4f(x,y,z,1));if(p.w<=1e-5f)return null;
                result[0]=Math.min(result[0],p.x/p.w);result[2]=Math.max(result[2],p.x/p.w);
                result[1]=Math.min(result[1],p.y/p.w);result[3]=Math.max(result[3],p.y/p.w);
            }
        }else for(int cap=-1;cap<=1;cap+=2){
            double radius=cap<0?1:ratio,C=m.m31*cap+m.m33,A=m.m30*radius,B=m.m32*radius;
            if(C-Math.hypot(A,B)<=1e-5)return null;
            for(int axis=0;axis<2;axis++){
                double a=m.get(axis,0)*radius,b=m.get(axis,2)*radius,k=m.get(axis,1)*cap+m.get(axis,3);
                double qa=C*C-A*A-B*B,qb=2*(a*A+b*B-k*C),qc=k*k-a*a-b*b;
                double root=Math.sqrt(Math.max(0,qb*qb-4*qa*qc));
                result[axis]=Math.min(result[axis],(float)((-qb-root)/(2*qa)));
                result[axis+2]=Math.max(result[axis+2],(float)((-qb+root)/(2*qa)));
            }
        }
        return new Vector4f(result[0],result[1],result[2],result[3]);
    }
}
