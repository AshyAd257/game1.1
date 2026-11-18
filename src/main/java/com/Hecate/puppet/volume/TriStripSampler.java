package com.Hecate.puppet.volume;

import java.awt.image.BufferedImage;
import java.util.*;

/** Integer row/column selection ported from the user's updated tri-strip-head prototype. */
public final class TriStripSampler {
    private TriStripSampler(){}
    public record Layout(int[] rows,int[] columns,int canvasWidth,int canvasHeight,double selectionCosine){
        public boolean samePixels(Layout b){return b!=null&&selectionCosine==b.selectionCosine&&canvasWidth==b.canvasWidth&&canvasHeight==b.canvasHeight&&Arrays.equals(rows,b.rows)&&Arrays.equals(columns,b.columns);}
    }
    public static int[] pick(int n,int count){int[] out=new int[count];for(int i=0;i<count;i++)out[i]=(int)Math.floor((i+.5)*n/count);return out;}
    public static int[] dropOrder(int[] importance,int start,int n){
        int[] order=new int[n],nearest=new int[n];boolean[] gone=new boolean[n];Arrays.fill(nearest,100);int count=0;
        for(int tier=1;tier<=3;tier++)while(true){
            int best=-1;double bestScore=-Double.MAX_VALUE;
            for(int r=0;r<n;r++)if(!gone[r]&&importance[start+r]==tier){double score=nearest[r]*100-Math.abs(r-(n-1)/2.0);if(score>bestScore){best=r;bestScore=score;}}
            if(best<0)break;gone[best]=true;order[count++]=best;
            for(int r=0;r<n;r++)nearest[r]=count==1?Math.abs(r-best):Math.min(nearest[r],Math.abs(r-best));
        }return order;
    }
    public static int[] keepRows(Strip3DDefinition d,int face,int count){
        int n=d.getFrameHeight();if(!d.isImportanceEnabled())return pick(n,count);
        boolean[] gone=new boolean[n];int[] order=d.dropOrder(face);for(int i=0;i<n-count;i++)gone[order[i]]=true;
        int[] out=new int[count];for(int r=0,i=0;r<n;r++)if(!gone[r])out[i++]=r;return out;
    }
    public static Layout layout(Strip3DDefinition d,double yaw,double pitch){
        if(!Double.isFinite(yaw)||!Double.isFinite(pitch))throw new IllegalArgumentException("视角必须是有限数值");
        int fw=d.getFrameWidth(),fh=d.getFrameHeight(),w=fw*4,sw=d.getSampleWidth();double pq=Math.round(Math.max(-90,Math.min(90,pitch))*2)/2.0,yq=Math.round(yaw*2)/2.0;
        List<Integer> rows=new ArrayList<>(),cols=new ArrayList<>();
        if(d.getVertical()==Strip3DDefinition.Sampling.SLIDE){int off=Math.max(0,Math.min(2*fh,(int)Math.round(fh-pq/(90.0/fh))));for(int i=0;i<fh;i++)rows.add(off+i);}
        else{double a=Math.toRadians(Math.abs(pq));int cap=(int)Math.round(fh*Math.sin(a)),side=(int)Math.round(fh*Math.cos(a));
            if(pq>=0)for(int r:keepRows(d,0,cap))rows.add(r);for(int r:keepRows(d,1,side))rows.add(fh+r);if(pq<0)for(int r:keepRows(d,2,cap))rows.add(2*fh+r);}
        if(d.getHorizontal()==Strip3DDefinition.Sampling.SLIDE){int off=Math.floorMod((int)Math.round(w/2.0+yaw/(360.0/w)-sw/2.0),w);for(int c=0;c<sw;c++)cols.add((off+c)%w);}
        else{double y=((yq%360)+360)%360;int k=(int)Math.floor(y/90);double a=Math.toRadians(y-k*90);int left=(int)Math.round(sw*Math.cos(a)),right=(int)Math.round(sw*Math.sin(a));
            // Widen each face's integer sampling window about the same face centre.
            // Face centres and the 360-degree period still follow the imported strip dimensions.
            int origin=(int)Math.round(2.0*fw-sw/2.0);for(int c:pick(sw,left))cols.add((origin+k*fw+c)%w);for(int c:pick(sw,right))cols.add((origin+(k+1)*fw+c)%w);}
        double cosine=d.hasSelectionMask()&&d.isSelectionFollowingTurn()?Math.cos(Math.toRadians(yq)):1;
        return new Layout(rows.stream().mapToInt(Integer::intValue).toArray(),cols.stream().mapToInt(Integer::intValue).toArray(),(int)Math.ceil(sw*Math.sqrt(2))+6,(int)Math.ceil(fh*Math.sqrt(2))+6,cosine);
    }
    public static BufferedImage compose(Strip3DDefinition d,Layout l,BufferedImage[] sources,boolean normal){
        BufferedImage out=new BufferedImage(l.canvasWidth,l.canvasHeight,BufferedImage.TYPE_INT_ARGB);int fh=d.getFrameHeight();
        int x0=(l.canvasWidth-l.columns.length)/2,y0=(l.canvasHeight-l.rows.length)/2;
        for(int y=0;y<l.rows.length;y++){int row=l.rows[y],face=row/fh;var layer=d.layer(Strip3DDefinition.Surface.values()[face]);BufferedImage source=sources[face];
            for(int x=0;x<l.columns.length;x++){if(!d.selectsPixel(x,y,l.columns.length,l.rows.length,l.selectionCosine))continue;int c=Math.floorMod(l.columns[x]+layer.getOffsetX(),4*d.getFrameWidth()),r=row%fh+layer.getOffsetY();
                int pixel=normal?(face==0?0xff80ff80:face==2?0xff800080:0xff8080ff):0;
                if(source!=null&&r>=0&&r<source.getHeight())pixel=source.getRGB(c,r);else if(!normal)pixel=0;
                out.setRGB(x0+x,y0+y,pixel);
            }}
        // Outline the final visible silhouette, after selection masking. Do not mask the border again:
        // its pixels intentionally lie just outside the edited shape, including concave cutouts.
        if(!normal&&d.isOutlineEnabled())return outline(out);
        return out;
    }
    private static BufferedImage outline(BufferedImage source){
        int w=source.getWidth(),h=source.getHeight();
        BufferedImage result=new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);
        int[][] neighbors={{-1,0},{1,0},{0,-1},{0,1}};
        for(int y=0;y<h;y++)for(int x=0;x<w;x++){
            int color=source.getRGB(x,y);
            if((color>>>24)==0)for(int[] neighbor:neighbors){
                int xx=x+neighbor[0],yy=y+neighbor[1];
                if(xx>=0&&yy>=0&&xx<w&&yy<h&&(source.getRGB(xx,yy)>>>24)!=0){color=0xff17121c;break;}
            }
            result.setRGB(x,y,color);
        }
        return result;
    }
}
