package com.Hecate.puppet.volume;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.*;
import com.jme3.scene.*;
import com.jme3.renderer.queue.RenderQueue;
import java.util.ArrayList;

/** Editor-only reference layer. Its rotation never comes from a billboard. */
public final class VolumeReferenceView {
    public enum Mode { APPEARANCE, SOLID, OVERLAY }
    private final Geometry geometry, xrayWire;
    private final Material material, wireMaterial;
    private final ColorRGBA tint;
    private final Node parent;
    private VolumeDefinition previous;
    private boolean xrayRequested, xray, selected;
    public VolumeReferenceView(AssetManager assets, Node parent, String boneName) {
        this.parent=parent;
        geometry=new Geometry(boneName+"_ReferenceVolume");
        material=new Material(assets,"Common/MatDefs/Misc/Unshaded.j3md");
        material.setBoolean("VertexColor",true);
        material.setColor("Color",new ColorRGBA(.78f,.65f,.32f,1));
        material.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);
        geometry.setMaterial(material); geometry.setShadowMode(RenderQueue.ShadowMode.Off);
        ColorRGBA[] palette={new ColorRGBA(.92f,.64f,.32f,1),new ColorRGBA(.6f,.78f,.98f,1),
            new ColorRGBA(.75f,.56f,.92f,1),new ColorRGBA(.47f,.88f,.64f,1),new ColorRGBA(.98f,.57f,.6f,1)};
        tint=palette[Math.floorMod(boneName.hashCode(),palette.length)];
        xrayWire=new Geometry(boneName+"_XrayEdges");
        wireMaterial=new Material(assets,"Common/MatDefs/Misc/Unshaded.j3md");
        var state=wireMaterial.getAdditionalRenderState();
        state.setBlendMode(RenderState.BlendMode.Alpha);state.setDepthTest(false);state.setDepthWrite(false);
        xrayWire.setMaterial(wireMaterial);xrayWire.setQueueBucket(RenderQueue.Bucket.Translucent);
        xrayWire.setShadowMode(RenderQueue.ShadowMode.Off);xrayWire.setCullHint(Spatial.CullHint.Always);
        updateColors();parent.attachChild(geometry);parent.attachChild(xrayWire);
    }
    public void setXray(boolean enabled){xrayRequested=enabled;}
    public void update(VolumeDefinition d,Vector3f position,Quaternion rotation,Vector3f scale,Mode mode,boolean visible) {
        if(d==null){setVisible(false);return;}
        if(!d.sameGeometry(previous)){
            geometry.setMesh(VolumeMeshFactory.create(d));xrayWire.setMesh(edges(d));previous=d.copy();
        }
        Vector3f meshScale=scale;
        if(d.isBakedPixelsEnabled()){
            // Scene transforms place the joint normally. Cancel inherited scale magnitudes
            // on the rigid mesh itself, preserving each mirror axis just like the pixel view.
            Vector3f inherited=parent.getWorldScale();
            meshScale=new Vector3f(rigidLocalScale(scale.x,inherited.x),
                                   rigidLocalScale(scale.y,inherited.y),
                                   rigidLocalScale(scale.z,inherited.z));
            visible=visible && meshScale.x!=0 && meshScale.y!=0 && meshScale.z!=0;
        }
        geometry.setLocalTranslation(position);
        geometry.setLocalRotation(rotation);
        geometry.setLocalScale(meshScale);
        xrayWire.setLocalTransform(geometry.getLocalTransform());
        xray=xrayRequested&&mode==Mode.SOLID;
        var state=material.getAdditionalRenderState();
        state.setWireframe(mode==Mode.OVERLAY);state.setDepthTest(true);state.setDepthWrite(!xray);
        state.setBlendMode(xray?RenderState.BlendMode.Alpha:RenderState.BlendMode.Off);
        state.setPolyOffset(mode==Mode.OVERLAY?-1:0,mode==Mode.OVERLAY?-1:0);
        geometry.setQueueBucket(xray?RenderQueue.Bucket.Transparent:RenderQueue.Bucket.Opaque);
        setVisible(visible&&mode!=Mode.APPEARANCE);updateColors();
    }
    private static float rigidLocalScale(float boneScale,float parentScale){
        return parentScale==0 ? 0 : Math.signum(boneScale)/Math.abs(parentScale);
    }
    public void setVisible(boolean visible){
        geometry.setCullHint(visible?Spatial.CullHint.Inherit:Spatial.CullHint.Always);
        xrayWire.setCullHint(visible&&xray?Spatial.CullHint.Inherit:Spatial.CullHint.Always);
    }
    public Geometry getGeometry() { return geometry; }
    public Geometry getXrayWireGeometry(){return xrayWire;}
    public void setSelected(boolean selected) {
        this.selected=selected;updateColors();
    }
    private void updateColors(){
        ColorRGBA color=selected?new ColorRGBA(.25f,.8f,1,1):xray?tint.clone():new ColorRGBA(.78f,.65f,.32f,1);
        color.a=xray?(selected?.22f:.12f):1;material.setColor("Color",color);
        color=selected?new ColorRGBA(.25f,.9f,1,1):tint.clone();color.a=selected?1:.72f;
        wireMaterial.setColor("Color",color);wireMaterial.getAdditionalRenderState().setLineWidth(selected?2:1);
    }
    public void cleanup(){geometry.removeFromParent();xrayWire.removeFromParent();}

    /** Cap rings and four generators, without triangulation diagonals cluttering the view. */
    private static Mesh edges(VolumeDefinition d){
        var points=new ArrayList<Vector3f>();float w=d.width()/2,h=d.height()/2,z=d.depth()/2;
        if(d.getImportedMesh()!=null){
            Mesh source=d.getImportedMesh().mesh(d.getImportedMesh().sizeScale(d.getWidthPx(),d.getHeightPx(),d.getDepthPx()));Vector3f a=new Vector3f(),b=new Vector3f(),c=new Vector3f();
            for(int i=0;i<source.getTriangleCount();i++){source.getTriangle(i,a,b,c);line(points,a.clone(),b.clone());line(points,b.clone(),c.clone());line(points,c.clone(),a.clone());}
        }else if(d.getShape()==VolumeDefinition.Shape.BOX){
            for(int a=-1;a<=1;a+=2)for(int b=-1;b<=1;b+=2){
                line(points,new Vector3f(-w,a*h,b*z),new Vector3f(w,a*h,b*z));
                line(points,new Vector3f(a*w,-h,b*z),new Vector3f(a*w,h,b*z));
                line(points,new Vector3f(a*w,b*h,-z),new Vector3f(a*w,b*h,z));
            }
        }else{
            float ratio=d.effectiveTopRatio();int count=32;
            for(int i=0;i<count;i++){
                double a=2*Math.PI*i/count,b=2*Math.PI*(i+1)/count;
                Vector3f bottom=ring(w,h,z,1,-1,a),top=ring(w,h,z,ratio,1,a);
                line(points,bottom,ring(w,h,z,1,-1,b));
                if(ratio>0)line(points,top,ring(w,h,z,ratio,1,b));
                if(i%(count/4)==0)line(points,bottom,top);
            }
        }
        float[] positions=new float[points.size()*3];for(int i=0;i<points.size();i++){Vector3f p=points.get(i);positions[i*3]=p.x;positions[i*3+1]=p.y;positions[i*3+2]=p.z;}
        Mesh mesh=new Mesh();mesh.setMode(Mesh.Mode.Lines);mesh.setBuffer(VertexBuffer.Type.Position,3,positions);mesh.updateBound();mesh.setStatic();return mesh;
    }
    private static Vector3f ring(float w,float h,float z,float ratio,int cap,double angle){return new Vector3f((float)Math.cos(angle)*w*ratio,cap*h,(float)Math.sin(angle)*z*ratio);}
    private static void line(ArrayList<Vector3f> points,Vector3f a,Vector3f b){points.add(a);points.add(b);}
}
