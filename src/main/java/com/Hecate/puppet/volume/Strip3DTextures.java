package com.Hecate.puppet.volume;

import com.jme3.asset.*;
import com.jme3.texture.*;
import com.jme3.texture.plugins.AWTLoader;
import com.jme3.texture.image.ColorSpace;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import javax.imageio.ImageIO;

/** Full-resolution assets: no resampling to the old 36-column ring. */
public final class Strip3DTextures {
    private static final java.util.Map<Strip3DDefinition.Surface,Texture> EXAMPLES=new java.util.EnumMap<>(Strip3DDefinition.Surface.class);
    private Strip3DTextures(){}
    public static Texture load(AssetManager assets,String path,boolean normal) throws IOException {
        Texture texture;
        if(new File(path).isAbsolute()){
            BufferedImage image=ImageIO.read(new File(path));if(image==null)throw new IOException("无法读取 PNG");
            if(image.getWidth()>8192||image.getHeight()>8192)throw new IOException("贴图边长不能超过 8192");
            texture=new Texture2D(new AWTLoader().load(image,true));
        } else {TextureKey key=new TextureKey(path,true);texture=assets.loadTexture(key).clone();texture.setImage(texture.getImage().clone());}
        texture.getImage().setColorSpace(normal?ColorSpace.Linear:ColorSpace.sRGB);nearest(texture);return texture;
    }
    public static void nearest(Texture t){t.setMagFilter(Texture.MagFilter.Nearest);t.setMinFilter(Texture.MinFilter.NearestNoMipMaps);t.setWrap(Texture.WrapMode.EdgeClamp);}
    public static synchronized Texture example(Strip3DDefinition.Surface surface){return EXAMPLES.computeIfAbsent(surface,s->{Texture t=new Texture2D(new AWTLoader().load(exampleImage(s),true));nearest(t);return t;});}
    public static BufferedImage exampleImage(Strip3DDefinition.Surface surface){
        try(var in=Strip3DTextures.class.getResourceAsStream("/textures/TriStripPrototype/"+surface.name().toLowerCase(java.util.Locale.ROOT)+".png")){
            if(in==null)throw new IOException("找不到内置三条带示例");return ImageIO.read(in);
        }catch(IOException e){throw new IllegalStateException(e);}
    }
    public static BufferedImage readImage(AssetManager assets,String path)throws IOException{
        BufferedImage image;
        if(new File(path).isAbsolute())image=ImageIO.read(new File(path));
        else{var info=assets.locateAsset(new AssetKey<Object>(path));if(info==null)throw new IOException("找不到图片："+path);try(var in=info.openStream()){image=ImageIO.read(in);}}
        if(image==null)throw new IOException("无法读取 PNG："+path);
        if(image.getWidth()>2048||image.getHeight()>512)throw new IOException("条带最大为 2048×512 像素");return image;
    }
    public static BufferedImage blankStrip(Strip3DDefinition d){return new BufferedImage(d.getFrameWidth()*4,d.getFrameHeight(),BufferedImage.TYPE_INT_ARGB);}
    /** Validate together before committing an import; never rescale the artist's source pixels. */
    public static void validateSources(Strip3DDefinition d,AssetManager assets)throws IOException{
        int w=0,h=0;
        for(var face:Strip3DDefinition.Surface.values()){var layer=d.layer(face);for(String path:new String[]{layer.getColorPath(),layer.getNormalPath()})if(path!=null){
            BufferedImage image=readImage(assets,path);if(image.getWidth()%4!=0)throw new IOException("条带宽度必须是 4 的倍数");
            if(w==0){w=image.getWidth();h=image.getHeight();}else if(w!=image.getWidth()||h!=image.getHeight())throw new IOException("三层颜色图和法线图必须同尺寸；当前需要 "+w+"×"+h);
        }}if(w!=0)d.setFrameSize(w/4,h);
    }
    public static void exportExamples(Path directory) throws IOException {
        Files.createDirectories(directory);
        for(Strip3DDefinition.Surface s:Strip3DDefinition.Surface.values()){
            Path destination=directory.resolve("strip3d-"+s.name().toLowerCase()+".png");
            if(Files.exists(destination))throw new IOException("文件已存在，请选择空文件夹："+destination.getFileName());
        }
        for(Strip3DDefinition.Surface s:Strip3DDefinition.Surface.values())ImageIO.write(exampleImage(s),"png",directory.resolve("strip3d-"+s.name().toLowerCase()+".png").toFile());
    }
}
