package com.Hecate.puppet.editor;

import com.Hecate.puppet.editor.core.*;
import com.Hecate.puppet.volume.VolumeDefinition;
import com.jme3.font.*;
import com.jme3.material.*;
import com.jme3.math.*;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.*;
import java.util.*;

/** Editor-only axis handles. A drag previews edits and commits exactly one existing editor command. */
public final class VolumeViewportTools {
    private static final ColorRGBA[] COLORS={new ColorRGBA(1,.28f,.25f,1),new ColorRGBA(.35f,1,.4f,1),new ColorRGBA(.3f,.65f,1,1)};
    private static final Vector3f[] AXES={Vector3f.UNIT_X,Vector3f.UNIT_Y,Vector3f.UNIT_Z};
    private final PuppetEditorApp app;
    private final Node handles=new Node("VolumeAxisHandles"),grid=new Node("VolumeSpaceGrid");
    private final Geometry skeletonLines,skeletonSelected,uniformHandle;
    private Vector2f uniformTip;private final Vector2f[][] rings=new Vector2f[3][];
    private Quaternion beforeRotation,beforeRestRotation;private Vector3f beforeScale,beforeRestScale;
    private Vector3f rotationStart;private float lastRotationAngle,accumulatedAngle;
    private int pressX,pressY;private Vector2f scaleDirection;
    private final Geometry[] arrows=new Geometry[3];private final BitmapText[] labels=new BitmapText[3];
    private final Vector2f[] starts=new Vector2f[3],tips=new Vector2f[3];private final boolean[] available=new boolean[3];
    private final Geometry gridLines,majorLines;private Node gridLabel;private String previousHint="";private final Geometry[] gridAxes=new Geometry[3];
    private int mode,activeAxis=-1,gridPlane=-1;private boolean gridVisible=true;private float gridStep=-1;private int gridA,gridB;
    private EditorBone dragBone;private VolumeDefinition beforeVolume;private Vector3f beforePosition,beforeRest;
    private Vector3f dragOrigin,dragAxis,planeNormal;private float startParameter,axisUnits;
    public VolumeViewportTools(PuppetEditorApp app){
        this.app=app;app.getGuiNode().attachChild(handles);app.getRootNode().attachChild(grid);
        skeletonLines=geometry("VisibleSkeleton",new ColorRGBA(.8f,.9f,1,1),true);skeletonSelected=geometry("SelectedSkeleton",new ColorRGBA(1,.75f,.2f,1),true);handles.attachChild(skeletonLines);handles.attachChild(skeletonSelected);
        uniformHandle=geometry("UniformScale",ColorRGBA.White,true);handles.attachChild(uniformHandle);
        BitmapFont font=app.getAssetManager().loadFont("Interface/Fonts/Default.fnt");
        for(int i=0;i<3;i++){arrows[i]=geometry("Axis"+i,COLORS[i],true);handles.attachChild(arrows[i]);labels[i]=new BitmapText(font);labels[i].setSize(16);labels[i].setColor(COLORS[i]);handles.attachChild(labels[i]);}
        gridLines=geometry("GridMinor",new ColorRGBA(.27f,.31f,.37f,1),false);majorLines=geometry("GridMajor",new ColorRGBA(.4f,.45f,.53f,1),false);grid.attachChild(gridLines);grid.attachChild(majorLines);
        gridLabel=new Node("GridHint");handles.attachChild(gridLabel);
        for(int i=0;i<3;i++){gridAxes[i]=geometry("GridAxis"+i,COLORS[i].mult(.65f),false);grid.attachChild(gridAxes[i]);}
    }
    public int getMode(){return mode;}
    public void setMode(int mode){cancel();this.mode=Math.max(0,Math.min(4,mode));update();}
    public boolean isGridVisible(){return gridVisible;}
    public void toggleGrid(){gridVisible=!gridVisible;update();}
    public boolean isDragging(){return dragBone!=null;}
    private Geometry geometry(String name,ColorRGBA color,boolean overlay){
        Geometry g=new Geometry(name,new Mesh());Material m=new Material(app.getAssetManager(),"Common/MatDefs/Misc/Unshaded.j3md");m.setColor("Color",color);m.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);m.getAdditionalRenderState().setDepthWrite(!overlay);m.getAdditionalRenderState().setDepthTest(!overlay);g.setMaterial(m);g.setShadowMode(RenderQueue.ShadowMode.Off);return g;
    }
    private Transform boneTransform(EditorBone bone){
        Vector3f p=new Vector3f(),scale=new Vector3f();Quaternion rotation=new Quaternion();bone.getWorldTransform(p,rotation,scale);
        var renderer=app.volumeSceneRenderer();Quaternion manual=new Quaternion().fromAngleAxis(renderer.getManualRotationAngle(),Vector3f.UNIT_Y);
        return new Transform(manual.mult(p),manual.mult(rotation),scale).combineWithParent(renderer.getPuppetNode().getWorldTransform());
    }
    /** Derivative of the rendered center with respect to a parent-space position coordinate. */
    private Vector3f movementAxis(EditorBone bone,int axis){
        Vector3f p=new Vector3f(),scale=new Vector3f(1,1,1);Quaternion rotation=new Quaternion();if(bone.getParent()!=null)bone.getParent().getWorldTransform(p,rotation,scale);
        Vector3f v=rotation.mult(AXES[axis].mult(scale.get(axis)));var renderer=app.volumeSceneRenderer();v=new Quaternion().fromAngleAxis(renderer.getManualRotationAngle(),Vector3f.UNIT_Y).mult(v);
        Transform outer=renderer.getPuppetNode().getWorldTransform();return outer.getRotation().mult(v.multLocal(outer.getScale()));
    }
    private Vector3f axis(EditorBone bone,Transform world,int i){return mode==0?movementAxis(bone,i):world.getRotation().mult(AXES[i].mult(world.getScale().get(i)));}
    private boolean allowed(Vector3f screen){return screen.z>=0&&screen.z<=1&&app.volumeViewportAllows(Math.round(screen.x),Math.round(screen.y));}
    public void update(){
        EditorBone bone=app.getVolumeSelectedBone();if(isDragging()&&(bone!=dragBone||!app.volumeBoneEditable(dragBone)||!app.volumeToolsVisible()))cancel();
        boolean show=app.volumeToolsVisible();grid.setCullHint(show&&gridVisible?Spatial.CullHint.Inherit:Spatial.CullHint.Always);updateGrid();
        updateSkeleton(show);uniformHandle.setCullHint(Spatial.CullHint.Always);uniformTip=null;
        Arrays.fill(available,false);for(int i=0;i<3;i++){arrows[i].setCullHint(Spatial.CullHint.Always);labels[i].setCullHint(Spatial.CullHint.Always);}
        gridLabel.setCullHint(show?Spatial.CullHint.Inherit:Spatial.CullHint.Always);gridLabel.setLocalTranslation(290,140,4);
        String hint=(mode==0?"移动 · 整像素":mode==1?"尺寸 · 中心对称伸缩":mode==3?"旋转 · 拖动圆环":mode==4?"缩放 · 中心方块等比":"箭头已隐藏")+(gridVisible?"  |  网格 "+new String[]{"YZ","XZ","XY"}[Math.max(0,gridPlane)]+"  "+String.format(Locale.ROOT,"%.4g",gridStep)+"单位/格":"")+"  |  左键拖动看旋转"+(isDragging()?"  |  Esc 取消":"");
        if(!hint.equals(previousHint)){gridLabel.detachAllChildren();gridLabel.attachChild(app.createVolumeHint(hint));previousHint=hint;}

        if(!show||mode==2||!app.volumeBoneEditable(bone))return;
        Transform world=boneTransform(bone);Vector3f center=world.getTranslation(),sc=app.getCamera().getScreenCoordinates(center);if(!allowed(sc))return;
        if(mode==3){updateRings(world,sc);return;}
        if(mode==4){uniformTip=new Vector2f(sc.x,sc.y);ArrayList<Vector3f> vertices=new ArrayList<>();quad(vertices,uniformTip.add(new Vector2f(-6,-6)),uniformTip.add(new Vector2f(6,-6)),uniformTip.add(new Vector2f(6,6)),uniformTip.add(new Vector2f(-6,6)));setTriangles(uniformHandle,vertices);uniformHandle.setCullHint(Spatial.CullHint.Inherit);uniformHandle.getMaterial().setColor("Color",activeAxis==3?ColorRGBA.Yellow:ColorRGBA.White);}
        for(int i=0;i<3;i++){
            Vector3f vector=axis(bone,world,i);float length=vector.length();if(length<1e-5)continue;
            Vector3f unit=vector.divide(length),projected=app.getCamera().getScreenCoordinates(center.add(unit));Vector2f direction=new Vector2f(projected.x-sc.x,projected.y-sc.y);
            float pixelsPerUnit=direction.length();if(pixelsPerUnit<3||Math.abs(unit.dot(app.getCamera().getDirection()))>.995f)continue;
            direction.normalizeLocal();float handleLength=88;
            if(mode==1){float half=dimension(bone.getVolumeDefinition(),i)/bone.getVolumeDefinition().getPixelsPerUnit()*.5f;Vector3f face=app.getCamera().getScreenCoordinates(center.add(vector.mult(half)));handleLength=Math.max(45,new Vector2f(face.x-sc.x,face.y-sc.y).length()+24);}
            Vector2f start=new Vector2f(sc.x,sc.y).add(direction.mult(14)),tip=new Vector2f(sc.x,sc.y).add(direction.mult(handleLength));
            if(!app.volumeViewportAllows(Math.round(tip.x),Math.round(tip.y)))continue;
            starts[i]=start;tips[i]=tip;available[i]=true;drawHandle(arrows[i],start,tip,mode==1||mode==4);arrows[i].setCullHint(Spatial.CullHint.Inherit);
            labels[i].setText("XYZ".substring(i,i+1)+(mode==1?" "+dimension(bone.getVolumeDefinition(),i):mode==4?String.format(Locale.ROOT," %.2f",bone.getLocalScale().get(i)):""));labels[i].setLocalTranslation(tip.x+10,tip.y+17,4);labels[i].setCullHint(Spatial.CullHint.Inherit);
            arrows[i].getMaterial().setColor("Color",isDragging()&&i==activeAxis?ColorRGBA.Yellow:COLORS[i]);
        }
    }
    private static int dimension(VolumeDefinition d,int axis){return axis==0?d.getWidthPx():axis==1?d.getHeightPx():d.getDepthPx();}
    private static void dimensions(VolumeDefinition d,int axis,int value){d.setDimensions(axis==0?value:d.getWidthPx(),axis==1?value:d.getHeightPx(),axis==2?value:d.getDepthPx());}
    private void drawHandle(Geometry geometry,Vector2f start,Vector2f tip,boolean box){
        Vector2f dir=tip.subtract(start).normalizeLocal(),normal=new Vector2f(-dir.y,dir.x),end=tip.subtract(dir.mult(box?6:12));
        ArrayList<Vector3f> vertices=new ArrayList<>();quad(vertices,start.add(normal.mult(2)),start.subtract(normal.mult(2)),end.subtract(normal.mult(2)),end.add(normal.mult(2)));
        if(box)quad(vertices,tip.add(new Vector2f(-6,-6)),tip.add(new Vector2f(6,-6)),tip.add(new Vector2f(6,6)),tip.add(new Vector2f(-6,6)));
        else{vertices.add(new Vector3f(tip.x,tip.y,3));vertices.add(new Vector3f(end.x+normal.x*7,end.y+normal.y*7,3));vertices.add(new Vector3f(end.x-normal.x*7,end.y-normal.y*7,3));}
        Mesh mesh=geometry.getMesh();mesh.setBuffer(VertexBuffer.Type.Position,3,com.jme3.util.BufferUtils.createFloatBuffer(vertices.toArray(Vector3f[]::new)));mesh.updateBound();
    }
    private static void quad(List<Vector3f> list,Vector2f a,Vector2f b,Vector2f c,Vector2f d){for(Vector2f p:new Vector2f[]{a,b,c,a,c,d})list.add(new Vector3f(p.x,p.y,3));}
    static float segmentDistance(Vector2f p,Vector2f a,Vector2f b){Vector2f v=b.subtract(a);float t=FastMath.clamp(p.subtract(a).dot(v)/Math.max(.0001f,v.lengthSquared()),0,1);return p.distance(a.add(v.mult(t)));}
    public boolean press(int x,int y){
        if(mode==2||!app.volumeViewportAllows(x,y))return false;update();int hit=-1;float best=10;Vector2f mouse=new Vector2f(x,y);
        if(mode==4&&uniformTip!=null&&uniformTip.distance(mouse)<10)hit=3;
        if(hit<0)for(int i=0;i<3;i++)if(available[i]){
            float distance=mode==3?ringDistance(mouse,rings[i]):segmentDistance(mouse,starts[i],tips[i]);if(distance<best){best=distance;hit=i;}
        }
        if(hit<0)return false;EditorBone bone=app.getVolumeSelectedBone();if(!app.volumeBoneEditable(bone))return false;
        Transform world=boneTransform(bone);dragOrigin=world.getTranslation().clone();pressX=x;pressY=y;
        if(mode==3){dragAxis=world.getRotation().mult(AXES[hit]).normalizeLocal();rotationStart=rotationVector(x,y);if(rotationStart==null)return false;lastRotationAngle=accumulatedAngle=0;}
        else if(mode==4){scaleDirection=hit==3?new Vector2f(1,1).normalizeLocal():tips[hit].subtract(starts[hit]).normalizeLocal();}
        else{
            Vector3f vector=axis(bone,world,hit);axisUnits=vector.length();dragAxis=vector.divide(axisUnits);
            Vector3f view=app.getCamera().isParallelProjection()?app.getCamera().getDirection().clone():dragOrigin.subtract(app.getCamera().getLocation()).normalizeLocal();planeNormal=view.subtract(dragAxis.mult(view.dot(dragAxis))).normalizeLocal();
            Float parameter=parameter(x,y);if(parameter==null)return false;startParameter=parameter;
        }
        dragBone=bone;activeAxis=hit;beforeVolume=bone.getVolumeDefinition().copy();beforePosition=bone.getLocalPosition().clone();beforeRest=bone.getRestPosition().clone();
        beforeRotation=bone.getLocalRotation().clone();beforeRestRotation=bone.getRestRotation().clone();beforeScale=bone.getLocalScale().clone();beforeRestScale=bone.getRestScale().clone();app.beginVolumeDrag();return true;
    }
    private Float parameter(int x,int y){Vector2f mouse=new Vector2f(x,y);Vector3f near=app.getCamera().getWorldCoordinates(mouse,0),far=app.getCamera().getWorldCoordinates(mouse,1),ray=far.subtract(near).normalizeLocal();float denominator=planeNormal.dot(ray);if(Math.abs(denominator)<.00001f)return null;float t=planeNormal.dot(dragOrigin.subtract(near))/denominator;if(t<0)return null;return near.add(ray.mult(t)).subtract(dragOrigin).dot(dragAxis)/axisUnits;}
    public void drag(int x,int y){
        if(!isDragging())return;if(!app.volumeBoneEditable(dragBone)||app.getVolumeSelectedBone()!=dragBone){cancel();return;}
        if(mode==3){Vector3f current=rotationVector(x,y);if(current==null)return;
            float angle=FastMath.atan2(dragAxis.dot(rotationStart.cross(current)),rotationStart.dot(current));float delta=angle-lastRotationAngle;
            if(delta>FastMath.PI)delta-=FastMath.TWO_PI;else if(delta<-FastMath.PI)delta+=FastMath.TWO_PI;accumulatedAngle+=delta;lastRotationAngle=angle;
            float snapped=Math.round(accumulatedAngle*FastMath.RAD_TO_DEG)*FastMath.DEG_TO_RAD;
            Quaternion rotation=snapped==0?beforeRotation.clone():beforeRotation.mult(new Quaternion().fromAngleAxis(snapped,AXES[activeAxis])).normalizeLocal();dragBone.setLocalRotation(rotation);dragBone.setRestRotation(rotation.clone());return;
        }
        if(mode==4){float movement=new Vector2f(x-pressX,y-pressY).dot(scaleDirection);float factor=(float)Math.exp(Math.max(-12,Math.min(12,movement*.01f)));
            float min=.000001f,max=Float.MAX_VALUE;for(int i=0;i<3;i++)if(activeAxis==3||activeAxis==i){float magnitude=Math.max(.001f,Math.abs(beforeScale.get(i)));min=Math.max(min,.001f/magnitude);max=Math.min(max,1000f/magnitude);}factor=FastMath.clamp(factor,min,max);
            Vector3f next=beforeScale.clone();for(int i=0;i<3;i++)if(activeAxis==3||activeAxis==i){float original=beforeScale.get(i);next.set(i,(original<0?-1:1)*Math.max(.001f,Math.abs(original))*factor);}dragBone.setLocalScale(next);dragBone.setRestScale(next.clone());return;
        }
        Float value=parameter(x,y);if(value==null||!Float.isFinite(value))return;float delta=value-startParameter,ppu=beforeVolume.getPixelsPerUnit();
        if(mode==0){Vector3f next=beforePosition.clone();next.set(activeAxis,beforePosition.get(activeAxis)+Math.round(delta*ppu)/ppu);dragBone.setLocalPosition(next);dragBone.setRestPosition(next.clone());}
        else{var next=beforeVolume.copy();int size=(int)Math.max(1,Math.min(4096,dimension(beforeVolume,activeAxis)+(long)Math.round(delta*ppu*2)));dimensions(next,activeAxis,size);dragBone.setVolumeDefinition(next);}
    }
    private void restore(){dragBone.setLocalPosition(beforePosition.clone());dragBone.setRestPosition(beforeRest.clone());dragBone.setLocalRotation(beforeRotation.clone());dragBone.setRestRotation(beforeRestRotation.clone());dragBone.setLocalScale(beforeScale.clone());dragBone.setRestScale(beforeRestScale.clone());dragBone.setVolumeDefinition(beforeVolume.copy());}
    public void cancel(){if(!isDragging())return;restore();dragBone=null;activeAxis=-1;}
    public void finish(){
        if(!isDragging())return;EditorBone bone=dragBone;int axis=activeAxis;Vector3f position=bone.getLocalPosition().clone(),scale=bone.getLocalScale().clone();Quaternion rotation=bone.getLocalRotation().clone();var volume=bone.getVolumeDefinition().copy();restore();dragBone=null;activeAxis=-1;
        if(!app.volumeBoneEditable(bone))return;
        if(mode==0){if(!position.equals(beforePosition))app.commitVolumeMove(bone,axis,position.get(axis));}
        else if(mode==3||mode==4){if(!rotation.equals(beforeRotation)||!scale.equals(beforeScale))app.commitVolumeTransform(bone,rotation,scale);}
        else if(dimension(volume,axis)!=dimension(beforeVolume,axis))app.commitVolumeSize(bone,volume);
    }
    private Vector3f rotationVector(int x,int y){
        Vector2f mouse=new Vector2f(x,y);Vector3f near=app.getCamera().getWorldCoordinates(mouse,0),ray=app.getCamera().getWorldCoordinates(mouse,1).subtract(near).normalizeLocal();
        float denominator=dragAxis.dot(ray);if(Math.abs(denominator)<.001f)return null;float t=dragAxis.dot(dragOrigin.subtract(near))/denominator;if(t<0)return null;
        Vector3f vector=near.add(ray.mult(t)).subtract(dragOrigin);return vector.lengthSquared()<1e-8f?null:vector.normalizeLocal();
    }
    private static float ringDistance(Vector2f mouse,Vector2f[] points){float distance=Float.MAX_VALUE;if(points!=null)for(int i=1;i<points.length;i++)if(points[i-1]!=null&&points[i]!=null)distance=Math.min(distance,segmentDistance(mouse,points[i-1],points[i]));return distance;}
    private void updateRings(Transform world,Vector3f screen){
        Vector3f center=world.getTranslation();float radius=app.getCamera().getWorldCoordinates(new Vector2f(screen.x+75,screen.y),screen.z).distance(center);
        Vector3f view=app.getCamera().isParallelProjection()?app.getCamera().getDirection():center.subtract(app.getCamera().getLocation()).normalizeLocal();
        for(int axis=0;axis<3;axis++){
            Vector3f normal=world.getRotation().mult(AXES[axis]);if(Math.abs(normal.dot(view))<.08f){rings[axis]=null;continue;}
            Vector3f u=world.getRotation().mult(AXES[(axis+1)%3]),v=world.getRotation().mult(AXES[(axis+2)%3]);Vector2f[] points=new Vector2f[97];ArrayList<Vector3f> vertices=new ArrayList<>();
            for(int k=0;k<=96;k++){float angle=k*FastMath.TWO_PI/96;Vector3f projected=app.getCamera().getScreenCoordinates(center.add(u.mult(radius*FastMath.cos(angle))).addLocal(v.mult(radius*FastMath.sin(angle))));if(allowed(projected))points[k]=new Vector2f(projected.x,projected.y);}
            for(int k=1;k<points.length;k++)if(points[k-1]!=null&&points[k]!=null)thickLine(vertices,points[k-1],points[k],2);
            rings[axis]=points;available[axis]=!vertices.isEmpty();setTriangles(arrows[axis],vertices);arrows[axis].getMaterial().setColor("Color",isDragging()&&activeAxis==axis?ColorRGBA.Yellow:COLORS[axis]);
            for(Vector2f point:points)if(point!=null){labels[axis].setText("XYZ".substring(axis,axis+1)+(isDragging()&&activeAxis==axis?" "+Math.round(accumulatedAngle*FastMath.RAD_TO_DEG)+" deg":""));labels[axis].setLocalTranslation(point.x+8,point.y+16,4);labels[axis].setCullHint(Spatial.CullHint.Inherit);break;}
        }
    }
    private static void thickLine(List<Vector3f> out,Vector2f a,Vector2f b,float halfWidth){Vector2f delta=b.subtract(a);if(delta.lengthSquared()<.001)return;delta.normalizeLocal();Vector2f normal=new Vector2f(-delta.y,delta.x).multLocal(halfWidth);quad(out,a.add(normal),a.subtract(normal),b.subtract(normal),b.add(normal));}
    private static void setTriangles(Geometry geometry,List<Vector3f> vertices){geometry.setCullHint(vertices.isEmpty()?Spatial.CullHint.Always:Spatial.CullHint.Inherit);if(vertices.isEmpty())return;Mesh mesh=geometry.getMesh();mesh.setMode(Mesh.Mode.Triangles);mesh.setBuffer(VertexBuffer.Type.Position,3,com.jme3.util.BufferUtils.createFloatBuffer(vertices.toArray(Vector3f[]::new)));mesh.updateBound();}
    private void updateSkeleton(boolean show){
        if(!show||!app.isVolumeBonesVisible()||app.volumeSceneRenderer()==null){skeletonLines.setCullHint(Spatial.CullHint.Always);skeletonSelected.setCullHint(Spatial.CullHint.Always);return;}
        List<Vector3f> normal=new ArrayList<>(),selected=new ArrayList<>();
        for(EditorBone bone:app.volumeSceneRenderer().getSkeleton().getAllBones()){
            if(!app.volumeBoneVisible(bone))continue;Vector3f end=app.getCamera().getScreenCoordinates(boneTransform(bone).getTranslation());List<Vector3f> vertices=bone==app.getVolumeSelectedBone()?selected:normal;
            if(allowed(end)){Vector2f c=new Vector2f(end.x,end.y);quad(vertices,c.add(new Vector2f(-5,0)),c.add(new Vector2f(0,-5)),c.add(new Vector2f(5,0)),c.add(new Vector2f(0,5)));}
            if(bone.getParent()!=null&&app.volumeBoneVisible(bone.getParent())){Vector3f start=app.getCamera().getScreenCoordinates(boneTransform(bone.getParent()).getTranslation());
                if(start.z>=0&&start.z<=1&&end.z>=0&&end.z<=1){Vector2f[] line=clipBoneLine(new Vector2f(start.x,start.y),new Vector2f(end.x,end.y));if(line!=null){Vector2f a=line[0],b=line[1],dir=b.subtract(a);if(dir.lengthSquared()>16){Vector2f shoulder=a.add(dir.mult(.22f)),side=new Vector2f(-dir.y,dir.x).normalizeLocal().multLocal(4);quad(vertices,a,shoulder.subtract(side),b,shoulder.add(side));}}}
            }
        }
        for(Vector3f vertex:normal)vertex.z=1;for(Vector3f vertex:selected)vertex.z=2;setTriangles(skeletonLines,normal);setTriangles(skeletonSelected,selected);
    }
    private Vector2f[] clipBoneLine(Vector2f a,Vector2f b){
        float dx=b.x-a.x,dy=b.y-a.y,low=0,high=1;float[] p={-dx,dx,-dy,dy},q={a.x-286,app.getCamera().getWidth()-336-a.x,a.y-126,app.getCamera().getHeight()-76-a.y};
        for(int i=0;i<4;i++){if(Math.abs(p[i])<1e-6){if(q[i]<0)return null;}else{float t=q[i]/p[i];if(p[i]<0)low=Math.max(low,t);else high=Math.min(high,t);if(low>high)return null;}}
        return new Vector2f[]{new Vector2f(a.x+low*dx,a.y+low*dy),new Vector2f(a.x+high*dx,a.y+high*dy)};
    }
    private void updateGrid(){
        if(!gridVisible||!app.volumeToolsVisible())return;
        Vector3f direction=app.getCamera().getDirection(),pivot=app.volumeViewPivot();int plane=Math.abs(direction.z)>.98f?2:Math.abs(direction.x)>.98f?0:1;
        float worldPerPixel=app.getCamera().getWorldCoordinates(new Vector2f(1,1),app.getCamera().getScreenCoordinates(pivot).z).distance(app.getCamera().getWorldCoordinates(new Vector2f(2,1),app.getCamera().getScreenCoordinates(pivot).z));
        float step=(float)Math.pow(2,Math.floor(Math.log(Math.max(.03125f,worldPerPixel*36))/Math.log(2)));step=Math.max(.03125f,Math.min(128,step));
        int a=plane==0?1:0,b=plane==2?1:2,ca=Math.round(pivot.get(a)/step),cb=Math.round(pivot.get(b)/step);
        if(plane==gridPlane&&step==gridStep&&ca==gridA&&cb==gridB)return;gridPlane=plane;gridStep=step;gridA=ca;gridB=cb;
        List<Vector3f> minor=new ArrayList<>(),major=new ArrayList<>();int count=48;
        for(int i=-count;i<=count;i++){
            List<Vector3f> first=Math.floorMod(ca+i,8)==0?major:minor,second=Math.floorMod(cb+i,8)==0?major:minor;
            Vector3f p=new Vector3f(),q=new Vector3f();p.set(a,(ca+i)*step);q.set(a,(ca+i)*step);p.set(b,(cb-count)*step);q.set(b,(cb+count)*step);first.add(p);first.add(q);
            p=new Vector3f();q=new Vector3f();p.set(b,(cb+i)*step);q.set(b,(cb+i)*step);p.set(a,(ca-count)*step);q.set(a,(ca+count)*step);second.add(p);second.add(q);
        }
        lineMesh(gridLines,minor);lineMesh(majorLines,major);
        for(int i=0;i<3;i++){gridAxes[i].setCullHint(i==plane?Spatial.CullHint.Always:Spatial.CullHint.Inherit);if(i!=plane){int center=i==a?ca:cb;Vector3f low=new Vector3f(),high=new Vector3f();low.set(i,(center-count)*step);high.set(i,(center+count)*step);lineMesh(gridAxes[i],List.of(low,high));}}
    }
    private static void lineMesh(Geometry geometry,List<Vector3f> points){Mesh mesh=geometry.getMesh();mesh.setMode(Mesh.Mode.Lines);mesh.setBuffer(VertexBuffer.Type.Position,3,com.jme3.util.BufferUtils.createFloatBuffer(points.toArray(Vector3f[]::new)));mesh.updateBound();}
}
