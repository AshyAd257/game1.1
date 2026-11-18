package com.Hecate.puppet.pixel;

import java.util.*;

/** Saved authoring parameters only. Animation poses and generated buffers never enter this data. */
public final class BakedPixelDefinition {
    private boolean enabled;
    private float upperElevation = 45, lowerElevation = -45;
    private float transitionDegrees = 8;
    private float perspectiveStrength = .1f;
    private int textureOriginX, textureOriginY;
    private boolean verticalTextureEnabled;
    private int verticalFrameHeight;
    private Map<Integer,int[]> textureRowImportance=new HashMap<>();
    private int priority;
    private float red = 1, green = 1, blue = 1;
    private Map<Integer, MaskEdit> viewEdits = new HashMap<>();
    private Map<Integer, MaskEdit> legacyViewEdits = new HashMap<>();
    private transient boolean editsMigrated;
    private List<PaletteEntry> palette = new ArrayList<>();
    public static final class PaletteEntry {
        private String name;private int source,target;
        private PaletteEntry(){}
        public PaletteEntry(String name,int source,int target){this.name=name==null?"颜色":name;this.source=source&0xffffff;this.target=target&0xffffff;}
        public String name(){return name;}public int source(){return source;}public int target(){return target;}
    }
    public List<PaletteEntry> getPalette(){return palette==null?List.of():List.copyOf(palette);}
    public void setPalette(List<PaletteEntry> values){if(values.size()>16)throw new IllegalArgumentException("每个部件最多 16 项调色板");Set<Integer> seen=new HashSet<>();for(var e:values)if(!seen.add(e.source()))throw new IllegalArgumentException("调色板里的原颜色不能重复");palette=new ArrayList<>(values);}
    public int recolor(int rgb){for(var e:getPalette())if(e.source()==(rgb&0xffffff))return (rgb&0xff000000)|e.target();return rgb;}
    public static final class MaskEdit {
        private int size;
        private boolean[] mask;
        private String geometryKey;
        private MaskEdit() {}
        public MaskEdit(int size, boolean[] mask, String key) {
            if (size < 1 || mask == null || mask.length != (long) size * size)
                throw new IllegalArgumentException("剪影网格尺寸不符");
            this.size = size; this.mask = mask.clone(); geometryKey = key;
        }
        public int size() { return size; }
        public boolean[] mask() { return mask.clone(); }
        public boolean applies(String key) {
            return Objects.equals(key,geometryKey);
        }
        public MaskEdit copy() { return new MaskEdit(size, mask, geometryKey); }
    }
    public boolean isEnabled() { return enabled; }
    public boolean isVerticalTextureEnabled(){return verticalTextureEnabled;}
    public void setVerticalTextureEnabled(boolean value){verticalTextureEnabled=value;}
    public int getVerticalFrameHeight(){return verticalFrameHeight;}
    public void setVerticalFrameHeight(int value){if(value<0||value>256)throw new IllegalArgumentException("平视取图高度为 1–256 行，0 使用平视剪影高度");verticalFrameHeight=value;}
    public int rowImportance(int layer,int row){
        int[] values=textureRowImportance==null?null:textureRowImportance.get(layer);
        return values!=null&&row>=0&&row<values.length?values[row]:2;
    }
    public void copyTextureRowsFrom(BakedPixelDefinition other){
        setVerticalTextureEnabled(other.isVerticalTextureEnabled());setVerticalFrameHeight(other.getVerticalFrameHeight());
        textureRowImportance=new HashMap<>();if(other.textureRowImportance!=null)other.textureRowImportance.forEach((k,v)->textureRowImportance.put(k,v.clone()));
    }
    public void setRowImportance(int layer,int row,int imageHeight,int value){
        if(layer<0||layer>2||imageHeight<1||imageHeight>16384||row<0||row>=imageHeight||value<1||value>3)throw new IllegalArgumentException("行重要度必须为 1、2 或 3，并对应图片中的完整行");
        if(textureRowImportance==null)textureRowImportance=new HashMap<>();
        int[] values=textureRowImportance.get(layer);
        if(values==null||values.length!=imageHeight){int[] next=new int[imageHeight];Arrays.fill(next,2);if(values!=null)System.arraycopy(values,0,next,0,Math.min(values.length,imageHeight));values=next;textureRowImportance.put(layer,values);}
        values[row]=value;
    }
    public float getPerspectiveStrength(){return perspectiveStrength;}
    public void setPerspectiveStrength(float value){
        if(!Float.isFinite(value)||value<0||value>.2f)throw new IllegalArgumentException("透视强度为 0–20%，0 为正交拍照");
        perspectiveStrength=value;
    }
    public void setEnabled(boolean value) { enabled = value; }
    // Keep the original three indices so saved hand edits retain their directions.
    public static final int LOWER=0, LEVEL=1, UPPER=2, TOP=3, BOTTOM=4;
    public static int orderedBand(int index) { return switch(index) { case 0 -> BOTTOM; case 1 -> LOWER; case 2 -> LEVEL; case 3 -> UPPER; case 4 -> TOP; default -> throw new IllegalArgumentException("方向编号无效"); }; }
    public float elevation(int band) { return switch(band) { case LOWER -> lowerElevation; case LEVEL -> 0; case UPPER -> upperElevation; case TOP -> 90; case BOTTOM -> -90; default -> throw new IllegalArgumentException("方向编号无效"); }; }
    public void setElevations(float lower, float upper) {
        if (!Float.isFinite(lower) || !Float.isFinite(upper) || lower < -89 || lower > -1 || upper < 1 || upper > 89)
            throw new IllegalArgumentException("下圈仰角为 -89 到 -1，上圈为 1 到 89 度");
        lowerElevation = lower; upperElevation = upper;
    }
    public float getTransitionDegrees() { return transitionDegrees; }
    public void setTransitionDegrees(float value) {
        if (!Float.isFinite(value) || value < 1 || value > 20) throw new IllegalArgumentException("逐行切换范围为 1–20 度");
        transitionDegrees = value;
    }
    public int getTextureOriginX() { return textureOriginX; }
    public int getTextureOriginY() { return textureOriginY; }
    public void setTextureOrigin(int x, int y) {
        if (Math.abs((long)x) > 65536 || Math.abs((long)y) > 65536) throw new IllegalArgumentException("贴图起点超出范围");
        textureOriginX = x; textureOriginY = y;
    }
    public int getPriority() { return priority; }
    public void setPriority(int value) { if (value < -100 || value > 100) throw new IllegalArgumentException("接缝优先级为 -100–100"); priority = value; }
    public float[] getTint() { return new float[]{red, green, blue}; }
    public void setTint(float r, float g, float b) {
        for (float v : new float[]{r,g,b}) if (!Float.isFinite(v) || v < 0 || v > 1) throw new IllegalArgumentException("颜色倍率为 0–1");
        red = r; green = g; blue = b;
    }
    private void migrateEdits(){
        if(editsMigrated)return;
        if(viewEdits==null)viewEdits=new HashMap<>();
        if(legacyViewEdits==null)legacyViewEdits=new HashMap<>();
        var iterator=viewEdits.entrySet().iterator();
        while(iterator.hasNext()){
            var e=iterator.next();
            if(e.getValue().geometryKey==null||!e.getValue().geometryKey.startsWith("pixel-v6/")){
                legacyViewEdits.put(e.getKey(),e.getValue());iterator.remove();
            }
        }
        editsMigrated=true;
    }
    public MaskEdit getLegacyEdit(int originalView) {migrateEdits();return legacyViewEdits.get(originalView);}
    public MaskEdit getEdit(int view) {migrateEdits();return viewEdits.get(view);}
    public void setEdit(int view, MaskEdit edit) {
        migrateEdits();
        if(edit!=null&&(edit.geometryKey==null||!edit.geometryKey.startsWith("pixel-v6/"))){
            if(view<0||view>=120)throw new IllegalArgumentException("旧视角编号为 0–119");
            legacyViewEdits.put(view,edit.copy());return;
        }
        if (view < 0 || view >= PixelAtlas.VIEWS) throw new IllegalArgumentException("视角编号为 0–99");
        if (edit == null) viewEdits.remove(view); else viewEdits.put(view, edit.copy());
    }
    public int staleEdits(String key) {migrateEdits();return legacyViewEdits.size()+(int)viewEdits.values().stream().filter(e -> !e.applies(key)).count();}
    public BakedPixelDefinition copy() {
        BakedPixelDefinition d = new BakedPixelDefinition(); d.enabled = enabled;
        d.setPerspectiveStrength(perspectiveStrength);
        d.setVerticalTextureEnabled(verticalTextureEnabled);d.setVerticalFrameHeight(verticalFrameHeight);
        if(textureRowImportance!=null)textureRowImportance.forEach((k,v)->d.textureRowImportance.put(k,v.clone()));
        d.setElevations(lowerElevation, upperElevation); d.setTransitionDegrees(transitionDegrees);
        d.setTextureOrigin(textureOriginX, textureOriginY); d.setPriority(priority); d.setTint(red,green,blue);
        d.setPalette(getPalette());
        migrateEdits();viewEdits.forEach((k,v) -> d.setEdit(k,v));legacyViewEdits.forEach((k,v)->d.legacyViewEdits.put(k,v.copy()));return d;
    }
}
