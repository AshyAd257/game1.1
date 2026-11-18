package com.Hecate.puppet.volume;

import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.shape.Box;
import java.util.ArrayList;
import java.util.List;

/** Closed, Y-aligned reference shapes, centred on the existing bone origin. */
public final class VolumeMeshFactory {
    private VolumeMeshFactory() {}
    public static Mesh create(VolumeDefinition d) {
        d.validate();
        if(d.getImportedMesh()!=null)return withReferenceShading(d.getImportedMesh().mesh(d.getImportedMesh().sizeScale(d.getWidthPx(),d.getHeightPx(),d.getDepthPx())));
        if (d.getShape() == VolumeDefinition.Shape.BOX) return withReferenceShading(new Box(d.width()/2,d.height()/2,d.depth()/2));
        int n=32;
        float[] positions=new float[(n*2+2)*3];
        float top=d.effectiveTopRatio();
        for(int i=0;i<n;i++) {
            double a=2*Math.PI*i/n;
            put(positions,i,(float)Math.cos(a)*d.width()/2,-d.height()/2,(float)Math.sin(a)*d.depth()/2);
            put(positions,n+i,(float)Math.cos(a)*d.width()/2*top,d.height()/2,(float)Math.sin(a)*d.depth()/2*top);
        }
        put(positions,2*n,0,-d.height()/2,0); put(positions,2*n+1,0,d.height()/2,0);
        List<Integer> indices=new ArrayList<>();
        for(int i=0;i<n;i++) {
            int j=(i+1)%n;
            tri(indices,i,n+i,j);
            if(top>0) { tri(indices,j,n+i,n+j); tri(indices,2*n+1,n+j,n+i); }
            tri(indices,2*n,i,j);
        }
        Mesh mesh=new Mesh();
        mesh.setBuffer(VertexBuffer.Type.Position,3,positions);
        mesh.setBuffer(VertexBuffer.Type.Index,3,indices.stream().mapToInt(Integer::intValue).toArray());
        mesh.updateBound(); mesh.setStatic(); return withReferenceShading(mesh);
    }
    /** Baked neutral shading makes the editor shape readable without adding scene lights. */
    private static Mesh withReferenceShading(Mesh mesh) {
        com.jme3.math.Vector3f[] normals=new com.jme3.math.Vector3f[mesh.getVertexCount()];
        for(int i=0;i<normals.length;i++)normals[i]=new com.jme3.math.Vector3f();
        int[] indices=new int[3];com.jme3.math.Vector3f a=new com.jme3.math.Vector3f(),b=new com.jme3.math.Vector3f(),c=new com.jme3.math.Vector3f();
        for(int i=0;i<mesh.getTriangleCount();i++){
            mesh.getTriangle(i,indices);mesh.getTriangle(i,a,b,c);com.jme3.math.Vector3f n=b.subtract(a).cross(c.subtract(a));
            for(int index:indices)normals[index].addLocal(n);
        }
        float[] colors=new float[normals.length*4];com.jme3.math.Vector3f light=new com.jme3.math.Vector3f(-.5f,.65f,.7f).normalizeLocal();
        for(int i=0;i<normals.length;i++){float value=.42f+.58f*Math.max(0,normals[i].normalizeLocal().dot(light));colors[i*4]=value;colors[i*4+1]=value;colors[i*4+2]=value;colors[i*4+3]=1;}
        mesh.setBuffer(VertexBuffer.Type.Color,4,colors);return mesh;
    }
    private static void put(float[] p,int i,float x,float y,float z) { p[i*3]=x;p[i*3+1]=y;p[i*3+2]=z; }
    private static void tri(List<Integer> a,int x,int y,int z) { a.add(x);a.add(y);a.add(z); }
}
