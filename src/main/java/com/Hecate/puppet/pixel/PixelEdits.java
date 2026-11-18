package com.Hecate.puppet.pixel;

import com.Hecate.puppet.volume.VolumeDefinition;
import java.util.Arrays;

/** Authoring-only edits to cached cells. Added pixels inherit nearest captured depth and surface. */
public final class PixelEdits {
    private PixelEdits() {}
    public static byte[] resolve(PixelAtlas atlas,VolumeDefinition d,int view){
        int n=atlas.size(),cells=n*n;byte[] result=new byte[cells*4];
        var edit=d.getBakedPixels().getEdit(view);boolean[] mask=edit!=null&&edit.size()==n&&edit.applies(atlas.key())?edit.mask():null;
        int[] nearest=new int[cells],queue=new int[cells];Arrays.fill(nearest,-1);int tail=0;
        for(int y=0;y<n;y++)for(int x=0;x<n;x++)if(atlas.occupied(view,x,y)){int i=y*n+x;nearest[i]=i;queue[tail++]=i;}
        if(mask!=null){for(int head=0;head<tail;head++){int i=queue[head],x=i%n,y=i/n;int[] next={x>0?i-1:-1,x<n-1?i+1:-1,y>0?i-n:-1,y<n-1?i+n:-1};for(int j:next)if(j>=0&&nearest[j]<0){nearest[j]=nearest[i];queue[tail++]=j;}}}
        var s=d.getStrip3D();
        for(int y=0;y<n;y++)for(int x=0;x<n;x++){
            int i=y*n+x,source=nearest[i];if(source<0||mask!=null&&!mask[i])continue;
            if(s.hasSelectionMask()){int mx=(int)Math.floor((x+.5f-n*.5f)/d.getWidthPx()*s.getSampleWidth()+s.getSampleWidth()*.5f),my=(int)Math.floor((y+.5f-n*.5f)/d.getHeightPx()*s.getFrameHeight()+s.getFrameHeight()*.5f);if(mx<0||my<0||mx>=s.getSampleWidth()||my>=s.getFrameHeight()||!s.selectsPixel(mx,my,s.getSampleWidth(),s.getFrameHeight()))continue;}
            int o=i*4;atlas.copyCell(view,source%n,source/n,result,o);
        }
        return result;
    }
}
