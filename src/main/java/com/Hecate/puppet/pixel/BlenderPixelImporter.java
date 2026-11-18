package com.Hecate.puppet.pixel;

import com.Hecate.puppet.volume.VolumeDefinition;
import com.jme3.asset.*;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.math.*;
import com.jme3.scene.*;
import java.nio.file.*;
import java.util.*;

/** GLB/glTF metre geometry. Object origins become joints; transforms are preserved in the hierarchy. */
public final class BlenderPixelImporter {
    private BlenderPixelImporter() {}
    public record Part(String name,int parent,Vector3f position,Quaternion rotation,VolumeDefinition definition) {}
    public static List<Part> load(Path file){
        String ext=file.getFileName().toString().toLowerCase(Locale.ROOT);
        if(!ext.endsWith(".glb")&&!ext.endsWith(".gltf"))throw new IllegalArgumentException("请从 Blender 导出 glTF 2.0（.glb 或 .gltf）");
        try{return fromScene(RigidGltfReader.read(file));}catch(java.io.IOException e){throw new IllegalArgumentException(e.getMessage(),e);}
    }
    public static List<Part> fromScene(Spatial scene){
        scene.updateGeometricState();LinkedHashMap<Spatial,List<Geometry>> groups=new LinkedHashMap<>();collect(scene,groups);
        if(groups.isEmpty())throw new IllegalArgumentException("模型里没有可用的三角形网格");if(groups.size()>64)throw new IllegalArgumentException("单次最多导入 64 个刚性部件，请拆分文件");
        List<Spatial> owners=new ArrayList<>(groups.keySet());List<Part> result=new ArrayList<>();long estimate=0;Set<String> names=new HashSet<>();
        for(int i=0;i<owners.size();i++){
            Spatial owner=owners.get(i);int parent=-1;Spatial ancestor=owner.getParent();while(ancestor!=null){parent=owners.indexOf(ancestor);if(parent>=0)break;ancestor=ancestor.getParent();}
            Vector3f origin=owner.getWorldTranslation();Quaternion rotation=owner.getWorldRotation();
            ArrayList<Float> vertices=new ArrayList<>();ArrayList<Integer> triangles=new ArrayList<>();Vector3f a=new Vector3f(),b=new Vector3f(),c=new Vector3f();
            for(Geometry g:groups.get(owner)){
                // Skinning would violate rigid-parts animation. Split limbs into objects in Blender.
                if(g.getMesh().getBuffer(VertexBuffer.Type.BoneWeight)!=null)throw new IllegalArgumentException("部件含蒙皮权重。请在 Blender 中把上臂、前臂等拆成独立刚性物体后导出。");
                Mesh mesh=g.getMesh();for(int t=0;t<mesh.getTriangleCount();t++){
                    mesh.getTriangle(t,a,b,c);for(Vector3f v:new Vector3f[]{a,b,c}){Vector3f local=rotation.inverse().mult(g.getWorldTransform().transformVector(v,null).subtractLocal(origin));triangles.add(vertices.size()/3);vertices.add(local.x);vertices.add(local.y);vertices.add(local.z);}
                    if(vertices.size()>600000)throw new IllegalArgumentException("单个部件三角形过多，请先简化网格");
                }
            }
            float[] positions=new float[vertices.size()];for(int j=0;j<positions.length;j++)positions[j]=vertices.get(j);int[] indices=triangles.stream().mapToInt(Integer::intValue).toArray();
            String base=owner.getName()==null||owner.getName().isBlank()?"Part_"+(i+1):owner.getName();String name=base;int suffix=2;while(!names.add(name))name=base+"_"+(suffix++);
            ImportedPixelMesh source=new ImportedPixelMesh(name,positions,indices);VolumeDefinition d=new VolumeDefinition();d.setPixelsPerUnit(PixelMetrics.PIXELS_PER_METRE);d.setDimensions(source.widthPx(),source.heightPx(),source.depthPx());d.setImportedMesh(source);d.getBakedPixels().setEnabled(true);d.getStrip3D().setEnabled(true);d.getStrip3D().setFrameSize(d.getWidthPx(),d.getHeightPx());
            estimate+=PixelAtlasBaker.estimateBytes(d);if(estimate>PixelAtlasCache.SHARED.budget())throw new IllegalArgumentException("导入人物的剪影超过 64 MB 上限，请减少像素尺寸或拆分部件");
            Vector3f position=origin.clone();Quaternion rest=rotation.clone();if(parent>=0){Spatial p=owners.get(parent);Quaternion inv=p.getWorldRotation().inverse();position=inv.mult(origin.subtract(p.getWorldTranslation()));rest=inv.mult(rotation);}
            result.add(new Part(name,parent,position,rest,d));
        }
        return List.copyOf(result);
    }
    private static void collect(Spatial spatial,LinkedHashMap<Spatial,List<Geometry>> groups){
        if(spatial instanceof Geometry g){if(g.getMesh().getMode()!=Mesh.Mode.Triangles)throw new IllegalArgumentException("请将部件转为三角形网格再导出");Spatial owner=g.getParent()==null?g:g.getParent();groups.computeIfAbsent(owner,k->new ArrayList<>()).add(g);}
        else if(spatial instanceof Node node)for(Spatial child:node.getChildren())collect(child,groups);
    }
}
