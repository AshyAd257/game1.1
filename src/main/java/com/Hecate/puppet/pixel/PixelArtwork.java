package com.Hecate.puppet.pixel;

import com.Hecate.puppet.volume.*;
import java.awt.image.BufferedImage;

/** Shared CPU reference for the mask painter and render verification; no mesh projection here. */
public final class PixelArtwork {
    private PixelArtwork() {}
    public static BufferedImage view(PixelAtlas atlas,VolumeDefinition d,int view,BufferedImage[] sources) {
        return view(atlas,d,view,sources,PixelVerticalSampling.build(atlas,d,PixelViewSelector.select(d.getBakedPixels(),view%PixelAtlas.YAW_VIEWS*PixelAtlas.YAW_DEGREES,d.getBakedPixels().elevation(view/PixelAtlas.YAW_VIEWS),atlas.size()),d.getBakedPixels().elevation(view/PixelAtlas.YAW_VIEWS),sources));
    }
    private static BufferedImage view(PixelAtlas atlas,VolumeDefinition d,int view,BufferedImage[] sources,PixelVerticalSampling.Rows rows){
        int n=atlas.size();BufferedImage result=new BufferedImage(n,n,BufferedImage.TYPE_INT_ARGB);
        byte[] cells=PixelEdits.resolve(atlas,d,view);
        for(int y=0;y<n;y++)for(int x=0;x<n;x++) {
            int layer=(cells[(y*n+x)*4+3]&255)/64-1;if(layer<0)continue;
            var s=d.getStrip3D();var l=s.layer(Strip3DDefinition.Surface.values()[layer]);BufferedImage source=sources[layer];if(source==null)continue;
            int px=PixelRingTexture.sourceX(atlas,d,view,layer,x,source.getWidth());
            int py=rows.sourceY(layer,y);if(py<0)continue;
            int c=source.getRGB(px,py);if((c>>>24)<128)continue;c=d.getBakedPixels().recolor(c);float[] tint=d.getBakedPixels().getTint();
            int r=tint((c>>16)&255,tint[0]),g=tint((c>>8)&255,tint[1]),b=tint(c&255,tint[2]);result.setRGB(x,y,0xff000000|r<<16|g<<8|b);
        }
        return result;
    }
    private static int tint(int channel,float multiplier){double s=channel/255.0,linear=s<=.04045?s/12.92:Math.pow((s+.055)/1.055,2.4);linear*=multiplier;double encoded=linear<=.0031308?linear*12.92:1.055*Math.pow(linear,1/2.4)-.055;return (int)Math.round(encoded*255);}
    public static BufferedImage selected(PixelAtlas atlas,VolumeDefinition d,float yaw,float pitch,BufferedImage[] sources){
        int n=atlas.size();var s=PixelViewSelector.select(d.getBakedPixels(),yaw,pitch,n);var rows=PixelVerticalSampling.build(atlas,d,s,pitch,sources);BufferedImage low=view(atlas,d,s.lowerBand()*PixelAtlas.YAW_VIEWS+s.yawStep(),sources,rows),high=view(atlas,d,s.upperBand()*PixelAtlas.YAW_VIEWS+s.yawStep(),sources,rows),out=new BufferedImage(n,n,BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<n;y++)for(int x=0;x<n;x++)out.setRGB(x,y,(y<s.revealedRows()?high:low).getRGB(x,y));
        if(d.getStrip3D().isOutlineEnabled()){BufferedImage outlined=new BufferedImage(n,n,BufferedImage.TYPE_INT_ARGB);for(int y=0;y<n;y++)for(int x=0;x<n;x++){int c=out.getRGB(x,y);if((c>>>24)==0)for(int[] delta:new int[][]{{-1,0},{1,0},{0,-1},{0,1}}){int xx=x+delta[0],yy=y+delta[1];if(xx>=0&&yy>=0&&xx<n&&yy<n&&(out.getRGB(xx,yy)>>>24)>0){c=0xff14121a;break;}}outlined.setRGB(x,y,c);}return outlined;}return out;
    }
}
