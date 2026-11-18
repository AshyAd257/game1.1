package com.Hecate.puppet.volume;

/** Three independently painted surfaces. All source-image offsets are integer pixels. */
public final class Strip3DDefinition {
    public enum Surface { TOP, SIDE, BOTTOM }
    public static final class Layer {
        private String colorPath, normalPath;
        private int offsetX, offsetY, travelX=16, travelY=8;
        public String getColorPath(){return colorPath;}
        public String getNormalPath(){return normalPath;}
        public void setColorPath(String p){colorPath=clean(p);}
        public void setNormalPath(String p){normalPath=clean(p);}
        public int getOffsetX(){return offsetX;} public int getOffsetY(){return offsetY;}
        public int getTravelX(){return travelX;} public int getTravelY(){return travelY;}
        public void setSampling(int x,int y,int tx,int ty){
            if(Math.abs((long)x)>65536||Math.abs((long)y)>65536||tx<0||tx>4096||ty<0||ty>4096)
                throw new IllegalArgumentException("偏移须在 ±65536 内，滑动范围须在 0–4096 内");
            offsetX=x;offsetY=y;travelX=tx;travelY=ty;
        }
        private static String clean(String p){return p==null||p.isBlank()?null:p;}
        public Layer copy(){Layer l=new Layer();l.colorPath=colorPath;l.normalPath=normalPath;l.setSampling(offsetX,offsetY,travelX,travelY);return l;}
    }
    public enum Sampling { SLIDE, PERSPECTIVE }
    private Sampling vertical=Sampling.PERSPECTIVE,horizontal=Sampling.SLIDE;
    private boolean importanceEnabled=true;
    // Preserve the previous appearance when loading files saved before the outline option existed.
    private boolean outlineEnabled=true;
    public boolean isOutlineEnabled(){return outlineEnabled;}
    public void setOutlineEnabled(boolean value){outlineEnabled=value;}
    private int frameWidth=24,frameHeight=24;
    // Zero keeps legacy files and newly imported textures at one face's width.
    private int sampleWidth;
    public int getSampleWidth(){return sampleWidth<=0?getFrameWidth():Math.min(sampleWidth,getFrameWidth()*4);}
    public void setSampleWidth(int value){
        if(value<1||value>getFrameWidth()*4)throw new IllegalArgumentException("选框宽度须为 1–"+getFrameWidth()*4+" 个整数像素（不超过贴图宽度）");
        sampleWidth=value;
    }
    private int[] importance;
    // Null is the legacy rectangular window. A mask belongs to the moving window, not the PNG.
    private int selectionWidth, selectionHeight;
    private boolean[] selectionMask;
    private boolean selectionFollowsTurn=true;
    private transient int[][] selectionRuns;
    public boolean isSelectionFollowingTurn(){return selectionFollowsTurn;}
    public void setSelectionFollowingTurn(boolean enabled){selectionFollowsTurn=enabled;}
    public boolean hasSelectionMask(){return selectionMask!=null&&selectionWidth>0&&selectionHeight>0&&selectionMask.length==(long)selectionWidth*selectionHeight;}
    public int getSelectionWidth(){return hasSelectionMask()?selectionWidth:getSampleWidth();}
    public int getSelectionHeight(){return hasSelectionMask()?selectionHeight:getFrameHeight();}
    public boolean[] getSelectionMask(){if(hasSelectionMask())return selectionMask.clone();boolean[] full=new boolean[getSelectionWidth()*getSelectionHeight()];java.util.Arrays.fill(full,true);return full;}
    public void setSelectionMask(int width,int height,boolean[] pixels){
        if(width<1||width>getFrameWidth()*4||height<1||height>getFrameHeight()||pixels==null||pixels.length!=(long)width*height)
            throw new IllegalArgumentException("选框形状的宽高必须为整数，且不能超过导入贴图的宽高");
        selectionWidth=width;selectionHeight=height;selectionMask=pixels.clone();selectionRuns=null;
    }
    public void resetSelectionMask(){selectionMask=null;selectionWidth=selectionHeight=0;selectionRuns=null;}
    public void copySelectionFrom(Strip3DDefinition other){selectionFollowsTurn=other.selectionFollowsTurn;if(other.hasSelectionMask())setSelectionMask(other.selectionWidth,other.selectionHeight,other.selectionMask);else resetSelectionMask();}
    /** Copy both the sampled extent and its silhouette, preserving the legacy automatic width. */
    public void copyWindowFrom(Strip3DDefinition other){sampleWidth=other.sampleWidth<=0?0:Math.min(other.getSampleWidth(),getFrameWidth()*4);copySelectionFrom(other);}
    /** Quantize the shape in window coordinates; source texels are never filtered or stretched. */
    public boolean selectsPixel(int x,int y,int windowWidth,int windowHeight){
        if(!hasSelectionMask())return true;
        int mx=(int)((2L*x+1)*selectionWidth/(2L*windowWidth)),my=(int)((2L*y+1)*selectionHeight/(2L*windowHeight));
        return selectionMask[Math.min(my,selectionHeight-1)*selectionWidth+Math.min(mx,selectionWidth-1)];
    }
    /** Approximate each front-view run as a round cross-section, centred in part space.
     * Preserve its width while projecting its lateral offset; back view mirrors exactly.
     * This is a silhouette approximation, not a reconstructed shoe mesh. */
    public boolean selectsPixel(int x,int y,int windowWidth,int windowHeight,double cosine){
        if(!hasSelectionMask())return true;
        if(!selectionFollowsTurn||cosine>1-1e-8)return selectsPixel(x,y,windowWidth,windowHeight);
        if(cosine<-1+1e-8)return selectsPixel(windowWidth-1-x,y,windowWidth,windowHeight);
        if(selectionRuns==null){
            selectionRuns=new int[selectionHeight][];
            for(int row=0;row<selectionHeight;row++){
                var runs=new java.util.ArrayList<Integer>();
                for(int col=0;col<selectionWidth;){if(!selectionMask[row*selectionWidth+col]){col++;continue;}runs.add(col++);while(col<selectionWidth&&selectionMask[row*selectionWidth+col])col++;runs.add(col);}
                selectionRuns[row]=runs.stream().mapToInt(Integer::intValue).toArray();
            }
        }
        int row=Math.min(selectionHeight-1,(int)((2L*y+1)*selectionHeight/(2L*windowHeight)));
        double px=(x+.5)/windowWidth-.5;int[] runs=selectionRuns[row];
        for(int i=0;i<runs.length;i+=2){double centre=((runs[i]+runs[i+1])/(2.0*selectionWidth)-.5)*cosine,radius=(runs[i+1]-runs[i])/(2.0*selectionWidth);
            if(px>=centre-radius-1e-9&&px<centre+radius-1e-9)return true;}
        return false;
    }
    private transient int[][] cachedDropOrders;
    int[] dropOrder(int face){if(cachedDropOrders==null)cachedDropOrders=new int[3][];if(cachedDropOrders[face]==null)cachedDropOrders[face]=TriStripSampler.dropOrder(getImportance(),face*getFrameHeight(),getFrameHeight());return cachedDropOrders[face];}
    public Sampling getVertical(){return vertical==null?Sampling.PERSPECTIVE:vertical;}
    public Sampling getHorizontal(){return horizontal==null?Sampling.SLIDE:horizontal;}
    public void setVertical(Sampling v){vertical=java.util.Objects.requireNonNull(v);}
    public void setHorizontal(Sampling v){horizontal=java.util.Objects.requireNonNull(v);}
    public boolean isImportanceEnabled(){return importanceEnabled;}
    public void setImportanceEnabled(boolean v){importanceEnabled=v;}
    public int getFrameWidth(){return frameWidth>0?frameWidth:24;}
    public int getFrameHeight(){return frameHeight>0?frameHeight:24;}
    public void setFrameSize(int w,int h){
        if(w<1||w>512||h<1||h>512)throw new IllegalArgumentException("单面尺寸须为 1–512 像素（条带宽度为单面宽度的 4 倍）");
        if(h!=getFrameHeight()){importance=null;cachedDropOrders=null;}frameWidth=w;frameHeight=h;sampleWidth=Math.min(sampleWidth,w*4);
        // Replacing a texture with a smaller one crops the mask without resampling its cells.
        if(hasSelectionMask()&&(selectionWidth>w*4||selectionHeight>h)){
            int nw=Math.min(selectionWidth,w*4),nh=Math.min(selectionHeight,h);boolean[] cropped=new boolean[nw*nh];
            for(int y=0;y<nh;y++)System.arraycopy(selectionMask,y*selectionWidth,cropped,y*nw,nw);
            setSelectionMask(nw,nh,cropped);
        }
    }
    public int[] getImportance(){
        int h=getFrameHeight();if(importance==null||importance.length!=h*3){importance=new int[h*3];java.util.Arrays.fill(importance,2);
            if(h==24){int[] sideRows={1,1,1,1,3,2,2,2,1,1,3,1,3,3,3,1,2,2,1,3,1,1,2,2};System.arraycopy(sideRows,0,importance,h,h);}}
        return importance.clone();
    }
    public void setImportance(int[] values){if(values==null||values.length!=getFrameHeight()*3)throw new IllegalArgumentException("行重要度数量与贴图高度不符");for(int v:values)if(v<1||v>3)throw new IllegalArgumentException("行重要度为 1、2 或 3");importance=values.clone();cachedDropOrders=null;}
    public void resetImportance(){importance=null;cachedDropOrders=null;}
    public void copyOptionsFrom(Strip3DDefinition other){setFrameSize(other.getFrameWidth(),other.getFrameHeight());sampleWidth=other.sampleWidth<=0?0:other.getSampleWidth();vertical=other.getVertical();horizontal=other.getHorizontal();importanceEnabled=other.importanceEnabled;outlineEnabled=other.outlineEnabled;setImportance(other.getImportance());copySelectionFrom(other);}
    private boolean enabled;
    private Layer top=new Layer(),side=new Layer(),bottom=new Layer();
    public boolean isEnabled(){return enabled;}
    public void setEnabled(boolean value){enabled=value;}
    public Layer layer(Surface s){switch(s){case TOP:if(top==null)top=new Layer();return top;case BOTTOM:if(bottom==null)bottom=new Layer();return bottom;default:if(side==null)side=new Layer();return side;}}
    public boolean sameSampling(Strip3DDefinition other){
        if(other==null||getSampleWidth()!=other.getSampleWidth()||outlineEnabled!=other.outlineEnabled||getVertical()!=other.getVertical()||getHorizontal()!=other.getHorizontal()||importanceEnabled!=other.importanceEnabled||getFrameWidth()!=other.getFrameWidth()||getFrameHeight()!=other.getFrameHeight()||!java.util.Arrays.equals(getImportance(),other.getImportance()))return false;
        if(selectionFollowsTurn!=other.selectionFollowsTurn||hasSelectionMask()!=other.hasSelectionMask()||hasSelectionMask()&&(selectionWidth!=other.selectionWidth||selectionHeight!=other.selectionHeight||!java.util.Arrays.equals(selectionMask,other.selectionMask)))return false;
        for(Surface s:Surface.values()){
            Layer a=layer(s),b=other.layer(s);
            if(!java.util.Objects.equals(a.colorPath,b.colorPath)||!java.util.Objects.equals(a.normalPath,b.normalPath)
                ||a.offsetX!=b.offsetX||a.offsetY!=b.offsetY||a.travelX!=b.travelX||a.travelY!=b.travelY)return false;
        }return true;
    }
    public Strip3DDefinition copy(){Strip3DDefinition d=new Strip3DDefinition();d.enabled=enabled;d.copyOptionsFrom(this);d.top=layer(Surface.TOP).copy();d.side=layer(Surface.SIDE).copy();d.bottom=layer(Surface.BOTTOM).copy();return d;}
}
