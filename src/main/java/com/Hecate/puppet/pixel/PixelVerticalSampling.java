package com.Hecate.puppet.pixel;

import com.Hecate.puppet.volume.*;
import java.awt.image.BufferedImage;
import java.util.*;

/** Whole source rows only. Camera/pose changes never capture a model or alter silhouette/depth. */
public final class PixelVerticalSampling {
    private PixelVerticalSampling(){}
    @FunctionalInterface public interface Presence {int layers(int view,int row);}
    public record Rows(int[][] sourceRows){public int sourceY(int layer,int row){return sourceRows[layer][row];}}
    public static int frameHeight(PixelAtlas atlas,VolumeDefinition d){
        int configured=d.getBakedPixels().getVerticalFrameHeight();
        return configured>0?configured:atlas.photoHeight(BakedPixelDefinition.LEVEL*PixelAtlas.YAW_VIEWS);
    }
    public static int shift(PixelAtlas atlas,VolumeDefinition d,float pitch){
        if(!d.getBakedPixels().isVerticalTextureEnabled())return 0;
        int rows=Math.round(Math.min(90,Math.abs(pitch))*frameHeight(atlas,d)/90f+.0001f);
        return pitch<0?-rows:rows;
    }
    public static Rows build(PixelAtlas atlas,VolumeDefinition d,PixelViewSelector.Selection choice,float pitch,BufferedImage[] sources){
        boolean edits=d.getStrip3D().hasSelectionMask();
        int low=choice.lowerBand()*PixelAtlas.YAW_VIEWS+choice.yawStep(),high=choice.upperBand()*PixelAtlas.YAW_VIEWS+choice.yawStep();
        edits|=d.getBakedPixels().getEdit(low)!=null||d.getBakedPixels().getEdit(high)!=null;
        if(!edits)return build(atlas,d,choice,pitch,sources,atlas::rowLayers);
        int n=atlas.size();byte[][] presence=new byte[2][n];int[] views={low,high};
        for(int i=0;i<2;i++){byte[] cells=PixelEdits.resolve(atlas,d,views[i]);for(int y=0;y<n;y++)for(int x=0;x<n;x++){int layer=(cells[(y*n+x)*4+3]&255)/64-1;if(layer>=0)presence[i][y]|=(byte)(1<<layer);}}
        return build(atlas,d,choice,pitch,sources,(view,row)->presence[view==low?0:1][row]&255);
    }
    public static Rows build(PixelAtlas atlas,VolumeDefinition d,PixelViewSelector.Selection choice,float pitch,BufferedImage[] sources,Presence presence){
        int n=atlas.size(),frame=frameHeight(atlas,d),shift=shift(atlas,d,pitch);int[][] result=new int[3][n];
        boolean sliding=d.getBakedPixels().isVerticalTextureEnabled();
        for(int layer=0;layer<3;layer++){
            int height=sources[layer].getHeight();var surface=Strip3DDefinition.Surface.values()[layer];
            int anchor=PixelRingTexture.originY(height)+d.getBakedPixels().getTextureOriginY()+d.getStrip3D().layer(surface).getOffsetY();
            int start=anchor-frame/2;
            Map<Integer,List<Integer>> groups=new TreeMap<>();
            for(int y=0;y<n;y++){
                int view=choice.viewForRow(y),raw=y-atlas.sampleCentreY(view)+anchor-shift;
                if(!sliding||d.getStrip3D().layer(surface).getColorPath()==null){result[layer][y]=Math.floorMod(raw,height);continue;}
                result[layer][y]=raw>=0&&raw<height?raw:-1;
                if(raw>=0&&raw<height&&d.getStrip3D().isImportanceEnabled()&&(presence.layers(view,y)&1<<layer)!=0)
                    groups.computeIfAbsent(Math.floorDiv(raw-start,frame),k->new ArrayList<>()).add(y);
            }
            for(var entry:groups.entrySet()){
                int bankStart=start+entry.getKey()*frame,first=Math.max(0,bankStart),last=Math.min(height,bankStart+frame),available=last-first;
                List<Integer> output=entry.getValue();
                if(available<=0){for(int y:output)result[layer][y]=-1;continue;}
                int count=Math.min(available,output.size()),weights[]=new int[available];
                for(int i=0;i<available;i++)weights[i]=d.getBakedPixels().rowImportance(layer,first+i);
                int[] drop=TriStripSampler.dropOrder(weights,0,available);boolean[] removed=new boolean[available];
                for(int i=0;i<available-count;i++)removed[drop[i]]=true;
                int[] kept=new int[count];for(int i=0,j=0;i<available;i++)if(!removed[i])kept[j++]=first+i;
                for(int i=0;i<output.size();i++)result[layer][output.get(i)]=kept[Math.min(count-1,(int)((i+.5)*count/output.size()))];
            }
        }
        return new Rows(result);
    }
}
