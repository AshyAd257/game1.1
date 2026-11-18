package com.Hecate.puppet.pixel;

import com.Hecate.puppet.volume.*;
import com.jme3.asset.AssetManager;
import com.jme3.texture.*;
import com.jme3.texture.image.ColorSpace;
import com.jme3.util.BufferUtils;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.*;
import java.util.*;

/** Shared source PNGs; colour changes and many characters never duplicate the same strip. */
final class PixelSourceCache {
    static final PixelSourceCache SHARED=new PixelSourceCache();
    private static final long BUDGET=32L*1024*1024;
    private long bytes;
    private final LinkedHashMap<String,Entry> entries=new LinkedHashMap<>(16,.75f,true);
    private static final class Entry {final BufferedImage image;final Texture2D texture;final long bytes;int refs;Entry(BufferedImage image,Texture2D texture){this.image=image;this.texture=texture;bytes=(long)image.getWidth()*image.getHeight()*8;}}
    final class Lease implements AutoCloseable {private Entry entry;Lease(Entry e){entry=e;e.refs++;}BufferedImage image(){return entry.image;}Texture2D texture(){return entry.texture;}public void close(){synchronized(PixelSourceCache.this){if(entry!=null){entry.refs--;entry=null;}}}}
    synchronized Lease acquire(AssetManager assets,String path,Strip3DDefinition strip)throws IOException{
        File file=path==null?null:new File(path);String key=path==null?"untextured-silhouette":file.isAbsolute()?file.getAbsolutePath()+"/"+file.lastModified()+"/"+file.length():System.identityHashCode(assets)+"/"+path;
        Entry entry=entries.get(key);if(entry==null){
            BufferedImage source=path==null?PixelRingTexture.silhouette():com.Hecate.puppet.pixel.PixelRingTexture.readImage(assets,path);PixelRingTexture.validate(source);long needed=(long)source.getWidth()*source.getHeight()*8;
            Iterator<Entry> it=entries.values().iterator();while(bytes+needed>BUDGET&&it.hasNext()){Entry e=it.next();if(e.refs==0){bytes-=e.bytes;e.texture.getImage().dispose();it.remove();}}
            if(bytes+needed>BUDGET)throw new IOException("当前条带图片已达到 32 MB 共享内存上限");
            ByteBuffer data=BufferUtils.createByteBuffer(source.getWidth()*source.getHeight()*4);for(int y=source.getHeight()-1;y>=0;y--)for(int x=0;x<source.getWidth();x++){int c=source.getRGB(x,y);data.put((byte)(c>>16)).put((byte)(c>>8)).put((byte)c).put((byte)(c>>>24));}data.flip();
            Texture2D texture=new Texture2D(new Image(Image.Format.RGBA8,source.getWidth(),source.getHeight(),data,ColorSpace.sRGB));Strip3DTextures.nearest(texture);entry=new Entry(source,texture);entries.put(key,entry);bytes+=entry.bytes;
        }return new Lease(entry);
    }
}
