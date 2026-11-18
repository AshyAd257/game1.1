package com.Hecate.puppet.volume;

import com.jme3.math.*;
import com.jme3.scene.*;
import java.awt.geom.*;
import java.util.*;

/** Selection silhouettes only: source-image alpha remains a window into enclosed parts. */
final class VolumeSelectionClip {
    static Area silhouette(Strip3DDefinition d,TriStripSampler.Layout layout,Matrix3f screenToArt){
        if(!d.hasSelectionMask())return null;
        boolean edited=false;for(boolean b:d.getSelectionMask())if(!b){edited=true;break;}
        if(!edited)return null;
        int cols=layout.columns().length,rows=layout.rows().length;
        int left=(layout.canvasWidth()-cols)/2,top=(layout.canvasHeight()-rows)/2;
        Matrix3f toScreen=screenToArt.invert();Path2D.Double path=new Path2D.Double();
        for(int y=0;y<rows;y++)for(int x=0;x<cols;){
            if(!d.selectsPixel(x,y,cols,rows,layout.selectionCosine())){x++;continue;}
            int start=x++;while(x<cols&&d.selectsPixel(x,y,cols,rows,layout.selectionCosine()))x++;
            float u0=(left+start)/(float)layout.canvasWidth(),u1=(left+x)/(float)layout.canvasWidth();
            float v0=1-(top+y+1)/(float)layout.canvasHeight(),v1=1-(top+y)/(float)layout.canvasHeight();
            boolean first=true;
            for(float[] uv:new float[][]{{u0,v0},{u1,v0},{u1,v1},{u0,v1}}){
                Vector3f p=toScreen.mult(new Vector3f(uv[0],uv[1],1));
                if(first){path.moveTo(p.x/p.z,p.y/p.z);first=false;}else path.lineTo(p.x/p.z,p.y/p.z);
            }
            path.closePath();
        }
        return new Area(path);
    }
    private record Edge(double x0,double y0,double x1,double y1){
        double x(double y){return x0+(x1-x0)*(y-y0)/(y1-y0);}
    }
    private record Span(int left,int right){}
    private static final class Rect {int left,right,bottom,top;Rect(Span s,int y){left=s.left;right=s.right;bottom=y;top=y+1;}}

    /** Rasterize polygon intersections on the shared display grid, then merge equal spans.
     * This avoids per-part full-screen textures and keeps every displayed pixel cell whole. */
    static Mesh mesh(Area area,Vector4f bounds,float width,float height,int step,Mesh mesh){
        var edges=new ArrayList<Edge>();double[] p=new double[6];double x=0,y=0,firstX=0,firstY=0;
        for(PathIterator it=area.getPathIterator(null);!it.isDone();it.next()){
            int kind=it.currentSegment(p);
            if(kind==PathIterator.SEG_MOVETO){x=firstX=p[0];y=firstY=p[1];}
            else if(kind==PathIterator.SEG_LINETO){if(y!=p[1])edges.add(new Edge(x,y,p[0],p[1]));x=p[0];y=p[1];}
            else if(kind==PathIterator.SEG_CLOSE){if(y!=firstY)edges.add(new Edge(x,y,firstX,firstY));x=firstX;y=firstY;}
        }
        var rectangles=new ArrayList<Rect>();Map<Span,Rect> active=new HashMap<>();
        int nx=(int)Math.ceil(width/step),ny=(int)Math.ceil(height/step);
        double[] intersections=new double[edges.size()];
        for(int row=0;row<ny;row++){
            double cy=(row+.5)*step/height*2-1;int n=0;
            for(Edge e:edges)if(cy>=Math.min(e.y0,e.y1)&&cy<Math.max(e.y0,e.y1))intersections[n++]=e.x(cy);
            Arrays.sort(intersections,0,n);Map<Span,Rect> next=new HashMap<>();
            for(int i=0;i+1<n;i+=2){
                int l=Math.max(0,(int)Math.ceil((intersections[i]+1)*width/(2*step)-.5));
                int r=Math.min(nx,(int)Math.ceil((intersections[i+1]+1)*width/(2*step)-.5));
                if(r<=l)continue;Span span=new Span(l,r);Rect rectangle=active.get(span);
                if(rectangle==null){rectangle=new Rect(span,row);rectangles.add(rectangle);}else rectangle.top=row+1;
                next.put(span,rectangle);
            }
            active=next;
        }
        int count=rectangles.size();float[] positions=new float[Math.max(3,count*12)],uv=new float[Math.max(2,count*8)];int[] indices=new int[count*6];
        for(int i=0;i<count;i++){
            Rect r=rectangles.get(i);float l=(r.left*step/width*2-1-bounds.x)/(bounds.z-bounds.x),right=(r.right*step/width*2-1-bounds.x)/(bounds.z-bounds.x);
            float bottom=(r.bottom*step/height*2-1-bounds.y)/(bounds.w-bounds.y),top=(r.top*step/height*2-1-bounds.y)/(bounds.w-bounds.y);
            System.arraycopy(new float[]{l,bottom,right,bottom,right,top,l,top},0,uv,i*8,8);
            System.arraycopy(new int[]{i*4,i*4+1,i*4+2,i*4,i*4+2,i*4+3},0,indices,i*6,6);
        }
        if(mesh==null)mesh=new Mesh();mesh.setBuffer(VertexBuffer.Type.Position,3,positions);mesh.setBuffer(VertexBuffer.Type.TexCoord,2,uv);mesh.setBuffer(VertexBuffer.Type.Index,3,indices);mesh.setDynamic();return mesh;
    }
}
