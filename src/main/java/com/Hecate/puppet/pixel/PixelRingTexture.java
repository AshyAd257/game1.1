package com.Hecate.puppet.pixel;

import com.Hecate.puppet.volume.*;
import java.awt.image.BufferedImage;
import com.jme3.asset.*;
import java.io.*;
import javax.imageio.ImageIO;

/** Texture length is independent of mask size. A ring closes in 20 whole-column steps. */
public final class PixelRingTexture {
    public static final long MAX_IMAGE_BYTES=32L*1024*1024;
    private PixelRingTexture(){}
    public static BufferedImage readImage(AssetManager assets,String path)throws IOException{
        BufferedImage image;
        if(new File(path).isAbsolute())image=ImageIO.read(new File(path));
        else{var info=assets.locateAsset(new AssetKey<Object>(path));if(info==null)throw new IOException("找不到图片："+path);try(var stream=info.openStream()){image=ImageIO.read(stream);}}
        if(image==null)throw new IOException("无法读取 PNG："+path);
        try{validate(image);}catch(IllegalArgumentException error){throw new IOException(error.getMessage(),error);}return image;
    }
    /** Centre the silhouette in the first quarter of the ring and in the PNG's actual height. */
    public static int originX(int width){return width/8;}
    public static int originY(int height){return height/2;}
    public static int sourceX(PixelAtlas atlas,VolumeDefinition d,int view,int layer,int cellX,int width){
        var l=d.getStrip3D().layer(Strip3DDefinition.Surface.values()[layer]);
        return column(cellX-atlas.sampleCentreX(view)+originX(width)+d.getBakedPixels().getTextureOriginX()+l.getOffsetX()+phase(view%PixelAtlas.YAW_VIEWS,width),width);
    }
    public static int sourceY(PixelAtlas atlas,VolumeDefinition d,int view,int layer,int cellY,int height){
        var l=d.getStrip3D().layer(Strip3DDefinition.Surface.values()[layer]);
        return Math.floorMod(cellY-atlas.sampleCentreY(view)+originY(height)+d.getBakedPixels().getTextureOriginY()+l.getOffsetY(),height);
    }
    public static void validate(BufferedImage image){
        if(image==null||image.getWidth()<1||image.getHeight()<1)throw new IllegalArgumentException("贴图必须包含完整的像素格");
        if(image.getWidth()>16384||image.getHeight()>16384)throw new IllegalArgumentException("贴图单边超过 16384 像素，请缩小图片");
        if((long)image.getWidth()*image.getHeight()*8>MAX_IMAGE_BYTES)throw new IllegalArgumentException("条带图片超过 32 MB，请缩小图片");
    }
    public record Requirement(int silhouetteWidth,int silhouetteHeight,int width,int recommendedHeight,int columnsPerStep){
        public String description(){return "贴图宽度自由；20 的倍数可等步移动；建议高度："+recommendedHeight+" px；360° 回到起点";}
    }
    public static Requirement requirement(PixelAtlas atlas){
        return new Requirement(atlas.silhouetteWidth(),atlas.silhouetteHeight(),PixelAtlas.YAW_VIEWS,atlas.silhouetteHeight(),1);
    }
    public static void validate(BufferedImage image,Requirement requirement){
        validate(image);
    }
    public static void assign(VolumeDefinition d,Strip3DDefinition.Surface surface,String path,BufferedImage image){
        validate(image);
        d.getStrip3D().layer(surface).setColorPath(path);d.getStrip3D().setEnabled(true);
    }
    /** Older arbitrary-width sources also close after a full turn, with integer per-view sampling. */
    public static int phase(int yawStep,int width){if(width<1)throw new IllegalArgumentException("环长必须至少为一列");return Math.floorMod(yawStep,PixelAtlas.YAW_VIEWS)*width/PixelAtlas.YAW_VIEWS;}
    public static String stepDescription(int width){
        if(width<1)throw new IllegalArgumentException("环长必须至少为一列");
        return "每 18° 移动 "+(width%PixelAtlas.YAW_VIEWS==0?Integer.toString(width/PixelAtlas.YAW_VIEWS):width/PixelAtlas.YAW_VIEWS+"–"+(width/PixelAtlas.YAW_VIEWS+1))+" 整列；360° 回到起点";
    }
    public static int column(int x,int width){if(width<1)throw new IllegalArgumentException("环长必须至少为一列");return Math.floorMod(x,width);}
    public static BufferedImage silhouette(){BufferedImage image=new BufferedImage(1,1,BufferedImage.TYPE_INT_ARGB);image.setRGB(0,0,0xffe1e8ef);return image;}
}
