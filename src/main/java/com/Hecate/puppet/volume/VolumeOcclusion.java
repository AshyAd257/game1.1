package com.Hecate.puppet.volume;

import com.jme3.renderer.Camera;
import java.util.*;

/** Explicit cover graph shared by the editor and game. No global depth/order overrides. */
public final class VolumeOcclusion {
    public static final int MAX_COVERS=8;
    public record Entry(String name,VolumeDefinition definition,Strip3DView view){}
    private Map<String,String> previous;
    private Map<String,List<String>> resolved=Map.of();
    private String error;
    private record Pair(VolumeContainment.Proxy outer,VolumeContainment.Proxy inner){}
    private final Map<Pair,Boolean> contained=new HashMap<>();
    public String getError(){return error;}

    /** Returns all enclosing parts for each target, including transitive ancestors. */
    public static Map<String,List<String>> resolve(Map<String,String> targets){
        Map<String,List<String>> incoming=new LinkedHashMap<>();targets.keySet().forEach(n->incoming.put(n,new ArrayList<>()));
        targets.forEach((outer,inner)->{if(inner!=null&&incoming.containsKey(inner))incoming.get(inner).add(outer);});
        Map<String,List<String>> result=new LinkedHashMap<>();
        for(String name:incoming.keySet())visit(name,incoming,result,new HashSet<>());
        return result;
    }
    private static List<String> visit(String name,Map<String,List<String>> incoming,Map<String,List<String>> result,Set<String> visiting){
        if(result.containsKey(name))return result.get(name);
        if(!visiting.add(name))throw new IllegalArgumentException("不能互相包覆或形成循环包覆关系。");
        if(visiting.size()>MAX_COVERS+1)throw new IllegalArgumentException("单个部件最多支持 8 个外层包覆部件。");
        Set<String> ancestors=new LinkedHashSet<>();
        for(String parent:incoming.get(name)){ancestors.add(parent);ancestors.addAll(visit(parent,incoming,result,visiting));}
        visiting.remove(name);
        if(ancestors.size()>MAX_COVERS)throw new IllegalArgumentException("单个部件最多支持 8 个外层包覆部件。");
        List<String> value=List.copyOf(ancestors);result.put(name,value);return value;
    }
    public void apply(List<Entry> entries,Camera camera){
        if(contained.size()>Math.max(16,entries.size()*entries.size()))contained.clear();
        Map<String,String> targets=new LinkedHashMap<>();Map<String,Strip3DView> views=new HashMap<>();
        for(Entry entry:entries){
            if(entry.view()!=null)entry.view().prepareCoverProjection(camera);
            if(entry.definition()!=null&&entry.definition().isStrip3DEnabled()){
                targets.put(entry.name(),entry.definition().isSurfaceDepthEnabled()?null:entry.definition().getCoverTarget());views.put(entry.name(),entry.view());
            }
        }
        if(!targets.equals(previous)){
            previous=targets;error=null;
            try{resolved=resolve(targets);}catch(IllegalArgumentException e){error=e.getMessage();resolved=Map.of();}
        }
        for(Entry entry:entries)if(entry.view()!=null){
            List<Strip3DView> covers=new ArrayList<>();
            for(String name:resolved.getOrDefault(entry.name(),List.of())){var view=views.get(name);if(view!=null&&view.isCoverVisible())covers.add(view);}
            entry.view().setCovers(covers);
            var silhouettes=new ArrayList<java.awt.geom.Area>();
            if(entry.view().isSurfaceVisible())for(Entry outer:entries){
                if(outer.view()==null||outer.view()==entry.view()||!outer.view().isSurfaceVisible())continue;
                var area=outer.view().selectionArea();if(area==null)continue;
                var pair=new Pair(outer.view().containmentProxy(),entry.view().containmentProxy());
                if(contained.computeIfAbsent(pair,p->VolumeContainment.contains(p.outer(),p.inner())))silhouettes.add(area);
            }
            entry.view().setSelectionClips(silhouettes,camera);
        }
    }
}
