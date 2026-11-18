package com.Hecate.puppet.pixel;

import com.Hecate.puppet.volume.VolumeDefinition;
import java.util.*;

/** Process-wide bounded, reference-counted cache shared by editor and game renderers. */
public final class PixelAtlasCache {
    public static final PixelAtlasCache SHARED=new PixelAtlasCache(64L*1024*1024);
    private final long budget;
    private long bytes, builds;
    private final LinkedHashMap<String,Entry> entries=new LinkedHashMap<>(16,.75f,true);
    private static final class Entry { final PixelAtlas atlas; int refs; Entry(PixelAtlas a){atlas=a;} }
    public PixelAtlasCache(long budget){if(budget<1)throw new IllegalArgumentException();this.budget=budget;}
    public final class Lease implements AutoCloseable {
        private Entry entry; private Lease(Entry e){entry=e;e.refs++;}
        public PixelAtlas atlas(){if(entry==null)throw new IllegalStateException("剪影已释放");return entry.atlas;}
        @Override public void close(){synchronized(PixelAtlasCache.this){if(entry!=null){entry.refs--;entry=null;}}}
    }
    public synchronized Lease acquire(VolumeDefinition d) {
        String key=PixelAtlasBaker.key(d);Entry entry=entries.get(key);
        if(entry==null){
            long needed=PixelAtlasBaker.estimateBytes(d);
            if(needed>budget)throw new IllegalArgumentException("单个部件超过剪影内存上限");
            Iterator<Entry> it=entries.values().iterator();
            while(bytes+needed>budget&&it.hasNext()){Entry e=it.next();if(e.refs==0){bytes-=e.atlas.bytes();e.atlas.dispose();it.remove();}}
            if(bytes+needed>budget)throw new IllegalStateException("当前人物剪影已达到内存上限；请减少同时显示的部件或像素尺寸");
            entry=new Entry(PixelAtlasBaker.bake(d));entries.put(key,entry);bytes+=entry.atlas.bytes();builds++;
        }
        if(d.getImportedMesh()!=null)entry.atlas.restoreJointCentredEdits(d.getBakedPixels(),PixelAtlasBaker.jointCentredKey(d));
        return new Lease(entry);
    }
    public synchronized long bytes(){return bytes;} public synchronized long builds(){return builds;}
    public long budget(){return budget;}
    public synchronized void clearUnused(){Iterator<Entry> it=entries.values().iterator();while(it.hasNext()){Entry e=it.next();if(e.refs==0){bytes-=e.atlas.bytes();e.atlas.dispose();it.remove();}}}
}
