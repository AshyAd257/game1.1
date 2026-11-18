package com.Hecate.puppet.volume;

import com.jme3.math.*;

/** Conservative containment of closed convex proxies, independent of the camera and bone hierarchy. */
final class VolumeContainment {
    record Proxy(Matrix4f world, VolumeDefinition.Shape shape, float topRatio) {
        static Proxy of(VolumeDefinition d, Transform transform) {
            Matrix4f size=new Matrix4f();size.setScale(new Vector3f(d.width()/2,d.height()/2,d.depth()/2));
            return new Proxy(transform.toTransformMatrix().mult(size),d.getShape(),d.effectiveTopRatio());
        }
        double volume() {
            return Math.abs(world.determinant())*(shape==VolumeDefinition.Shape.BOX?8:2*Math.PI/3*(1+topRatio+topRatio*topRatio));
        }
    }
    static boolean contains(Proxy outer, Proxy inner) {
        // Coincident/equal parts must not become each other's enclosing silhouettes.
        if(outer.volume()<=inner.volume()*(1+1e-6))return false;
        Matrix4f relative=outer.world.invert().mult(inner.world);
        if(inner.shape==VolumeDefinition.Shape.BOX){
            for(int x=-1;x<=1;x+=2)for(int y=-1;y<=1;y+=2)for(int z=-1;z<=1;z+=2)
                if(!inside(outer,relative.mult(new Vector3f(x,y,z))))return false;
        }else{
            // A circumscribed 128-gon contains each exact elliptical cap. Testing its
            // vertices against a convex outer proxy cannot clip a protruding inner part.
            int n=128;double expansion=1/Math.cos(Math.PI/n);
            for(int cap=-1;cap<=1;cap+=2)for(int i=0;i<n;i++){
                double a=2*Math.PI*i/n,r=(cap<0?1:inner.topRatio)*expansion;
                if(!inside(outer,relative.mult(new Vector3f((float)(r*Math.cos(a)),cap,(float)(r*Math.sin(a))))))return false;
            }
        }
        return true;
    }
    private static boolean inside(Proxy outer,Vector3f p){
        if(outer.shape==VolumeDefinition.Shape.BOX)return Math.max(Math.abs(p.x),Math.max(Math.abs(p.y),Math.abs(p.z)))<=1.000001;
        double r=(outer.topRatio+1)/2.0+(outer.topRatio-1)/2.0*p.y;
        return Math.abs(p.y)<=1.000001&&p.x*p.x+p.z*p.z<=r*r+1e-6;
    }
}
