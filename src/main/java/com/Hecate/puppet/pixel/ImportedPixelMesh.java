package com.Hecate.puppet.pixel;

import com.jme3.math.*;
import com.jme3.scene.*;
import java.nio.*;
import java.security.*;

/** Portable rigid source geometry in metres, relative to its original Blender object origin. */
public final class ImportedPixelMesh {
    private float[] positions;
    private int[] indices;
    private int widthPx,heightPx,depthPx;
    private String digest,sourceName;
    private ImportedPixelMesh() {}
    public ImportedPixelMesh(String name,float[] positions,int[] indices){
        if(positions==null||positions.length<9||positions.length%3!=0||indices==null||indices.length<3||indices.length%3!=0||positions.length>600000||indices.length>600000)throw new IllegalArgumentException("部件网格无效或超过 20 万顶点／三角形索引上限");
        this.positions=positions.clone();this.indices=indices.clone();sourceName=name;
        float[] min={Float.POSITIVE_INFINITY,Float.POSITIVE_INFINITY,Float.POSITIVE_INFINITY},max={Float.NEGATIVE_INFINITY,Float.NEGATIVE_INFINITY,Float.NEGATIVE_INFINITY};
        for(int i=0;i<positions.length;i++){float v=positions[i];if(!Float.isFinite(v))throw new IllegalArgumentException("网格坐标无效");min[i%3]=Math.min(min[i%3],v);max[i%3]=Math.max(max[i%3],v);}
        for(int index:indices)if(index<0||index>=positions.length/3)throw new IllegalArgumentException("网格索引无效");
        widthPx=Math.max(1,Math.round((max[0]-min[0])*PixelMetrics.PIXELS_PER_METRE));heightPx=Math.max(1,Math.round((max[1]-min[1])*PixelMetrics.PIXELS_PER_METRE));depthPx=Math.max(1,Math.round((max[2]-min[2])*PixelMetrics.PIXELS_PER_METRE));
        try{MessageDigest hash=MessageDigest.getInstance("SHA-256");ByteBuffer buffer=ByteBuffer.allocate(4);for(float v:positions){buffer.clear();buffer.putFloat(v);hash.update(buffer.array());}for(int i:indices){buffer.clear();buffer.putInt(i);hash.update(buffer.array());}digest=java.util.HexFormat.of().formatHex(hash.digest());}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}
    }
    public String key(){return digest;}public String sourceName(){return sourceName;}
    public int widthPx(){return widthPx;}public int heightPx(){return heightPx;}public int depthPx(){return depthPx;}
    public Vector3f sizeScale(int width,int height,int depth){return new Vector3f(width/(float)widthPx,height/(float)heightPx,depth/(float)depthPx);}
    public float radius(Vector3f scale){float radius=0;for(int i=0;i<positions.length;i+=3){float x=positions[i]*scale.x,y=positions[i+1]*scale.y,z=positions[i+2]*scale.z;radius=Math.max(radius,(float)Math.sqrt(x*x+y*y+z*z));}return radius;}
    float captureRadius(Vector3f scale){
        Vector3f min=new Vector3f(Float.POSITIVE_INFINITY,Float.POSITIVE_INFINITY,Float.POSITIVE_INFINITY);
        Vector3f max=min.negate(),vertex=new Vector3f();
        for(int i=0;i<positions.length;i+=3){
            vertex.set(positions[i],positions[i+1],positions[i+2]);
            min.minLocal(vertex);
            max.maxLocal(vertex);
        }
        Vector3f centre=min.addLocal(max).multLocal(.5f);
        float squared=0;
        for(int i=0;i<positions.length;i+=3){
            vertex.set(positions[i],positions[i+1],positions[i+2]).subtractLocal(centre).multLocal(scale);
            squared=Math.max(squared,vertex.lengthSquared());
        }
        return FastMath.sqrt(squared);
    }
    public Mesh mesh(Vector3f scale){float[] values=positions.clone();for(int i=0;i<values.length;i++)values[i]*=scale.get(i%3);Mesh mesh=new Mesh();mesh.setBuffer(VertexBuffer.Type.Position,3,values);mesh.setBuffer(VertexBuffer.Type.Index,3,indices.clone());mesh.updateBound();mesh.setStatic();return mesh;}
    // Geometry is immutable; authoring snapshots safely share it and save it only as source data.
    public ImportedPixelMesh copy(){return this;}
    public ImportedPixelMesh atCurrentDensity(){return new ImportedPixelMesh(sourceName,positions,indices);}
}
