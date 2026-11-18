package com.Hecate.puppet.pixel;

import com.jme3.math.*;
import com.jme3.texture.*;
import com.jme3.texture.image.ColorSpace;
import com.jme3.util.BufferUtils;
import com.Hecate.puppet.volume.Strip3DTextures;
import java.nio.ByteBuffer;

/** Immutable shared mask + 24-bit depth. No baked colours, animation frames, or per-pose copies. */
public final class PixelAtlas {
    public static final int YAW_VIEWS=20, BANDS=5, VIEWS=YAW_VIEWS*BANDS, COLUMNS=10, ROWS=10;
    public static final float YAW_DEGREES=360f/YAW_VIEWS;
    private final String key;
    private final int size;
    private final int silhouetteWidth,silhouetteHeight;
    private final float pixelsPerUnit, radius;
    private final Vector3f centre;
    private final Vector2f[] oldGridOffsets;
    private final ByteBuffer data;
    private final Texture2D texture;
    private final Vector4f[] photoPlanes;
    private final Vector4f[] photoPerspectives;
    private final int[] photoHeights=new int[VIEWS];
    private final byte[][] rowLayers=new byte[VIEWS][];
    private final Vector2f[] sampleCentres=new Vector2f[VIEWS];
    private final Vector3f[] directions=new Vector3f[VIEWS], rights=new Vector3f[VIEWS], ups=new Vector3f[VIEWS];
    PixelAtlas(String key, int size, float pixelsPerUnit, BakedPixelDefinition settings, ByteBuffer bytes,Vector4f[] planes,Vector4f[] perspectives,Vector3f centre,Vector2f[] oldGridOffsets) {
        this.centre=centre.clone();
        this.oldGridOffsets=oldGridOffsets;
        this.key=key;this.size=size;this.pixelsPerUnit=pixelsPerUnit;radius=size/(2*pixelsPerUnit);data=bytes;
        photoPlanes=new Vector4f[VIEWS];for(int i=0;i<VIEWS;i++)photoPlanes[i]=planes[i].clone();
        photoPerspectives=new Vector4f[VIEWS];for(int i=0;i<VIEWS;i++)photoPerspectives[i]=perspectives[i].clone();
        texture=new Texture2D(new Image(Image.Format.RGBA8,size*COLUMNS,size*ROWS,bytes,ColorSpace.Linear));
        Strip3DTextures.nearest(texture);
        for(int i=0;i<VIEWS;i++) { directions[i]=PixelViewSelector.direction(i%YAW_VIEWS*YAW_DEGREES,settings.elevation(i/YAW_VIEWS));rights[i]=PixelViewSelector.right(i%YAW_VIEWS*YAW_DEGREES);ups[i]=directions[i].cross(rights[i]).normalizeLocal(); }
        int maxWidth=1,maxHeight=1;
        for(int view=0;view<VIEWS;view++){
            rowLayers[view]=new byte[size];
            int left=size,right=-1,top=size,bottom=-1;
            for(int y=0;y<size;y++)for(int x=0;x<size;x++)if(occupied(view,x,y)){
                rowLayers[view][y]|=(byte)(1<<(layer(view,x,y)-1));
                left=Math.min(left,x);right=Math.max(right,x);top=Math.min(top,y);bottom=Math.max(bottom,y);
            }
            maxWidth=Math.max(maxWidth,right-left+1);maxHeight=Math.max(maxHeight,bottom-top+1);
            photoHeights[view]=Math.max(1,bottom-top+1);
            sampleCentres[view]=right<left?new Vector2f(size/2,size/2):new Vector2f((left+right+1)/2,(top+bottom+1)/2);
        }
        silhouetteWidth=maxWidth;silhouetteHeight=maxHeight;
    }
    public String key(){return key;} public int size(){return size;} public float radius(){return radius;}
    public Vector3f centre(){return centre.clone();}
    /** Re-register saved edits by the same whole-cell translation as the captured triangles. */
    void restoreJointCentredEdits(BakedPixelDefinition settings,String oldKey){
        for(int view=0;view<VIEWS;view++){
            var edit=settings.getEdit(view);
            if(edit==null||!edit.applies(oldKey))continue;
            int oldSize=edit.size();
            int dx=Math.round(oldGridOffsets[view].x+oldSize*.5f);
            int dy=Math.round(oldGridOffsets[view].y+oldSize*.5f);
            boolean[] original=edit.mask(),moved=new boolean[size*size];
            boolean fits=true;
            for(int i=0;i<original.length;i++){
                if(!original[i])continue;
                int x=i%oldSize-dx,y=i/oldSize-dy;
                if(x<0||x>=size||y<0||y>=size){fits=false;break;}
                moved[y*size+x]=true;
            }
            // Keep unusually distant hand-painted cells intact for the existing stale-edit UI.
            if(fits)settings.setEdit(view,new BakedPixelDefinition.MaskEdit(size,moved,key));
        }
    }
    public float pixelsPerUnit(){return pixelsPerUnit;} public long bytes(){return (long)size*size*VIEWS*4;}
    public int silhouetteWidth(){return silhouetteWidth;}
    public int silhouetteHeight(){return silhouetteHeight;}
    public int photoHeight(int view){return photoHeights[view];}
    public int rowLayers(int view,int row){return rowLayers[view][row]&255;}
    /** Integer photo centre is independent of a Blender object's joint origin. */
    public Vector2f sampleCentre(int view){return sampleCentres[view].clone();}
    public int sampleCentreX(int view){return (int)sampleCentres[view].x;}
    public int sampleCentreY(int view){return (int)sampleCentres[view].y;}
    public Texture2D texture(){return texture;}
    public Vector3f direction(int view){return directions[view].clone();}
    public Vector3f right(int view){return rights[view].clone();}
    public Vector3f up(int view){return ups[view].clone();}
    public Vector4f photoPlane(int view){return photoPlanes[view].clone();}
    public Vector4f photoPerspective(int view){return photoPerspectives[view].clone();}
    public int offset(int view,int x,int y){return (((view/COLUMNS)*size+(size-1-y))*(size*COLUMNS)+(view%COLUMNS)*size+x)*4;}
    public int layer(int view,int x,int y){return (data.get(offset(view,x,y)+3)&255)/64;}
    public boolean occupied(int view,int x,int y){return layer(view,x,y)>0;}
    public void copyCell(int view,int x,int y,byte[] target,int start){int o=offset(view,x,y);for(int i=0;i<4;i++)target[start+i]=data.get(o+i);}
    public float depth(int view,int x,int y){int o=offset(view,x,y);int n=((data.get(o)&255)<<16)|((data.get(o+1)&255)<<8)|(data.get(o+2)&255);return (n/16777215f*2-1)*radius;}
    public Vector3f point(int view,int x,int y){return point(view,x,y,depth(view,x,y));}
    public Vector3f point(int view,int x,int y,float depth){
        Vector4f p=photoPlanes[view],q=photoPerspectives[view];float shrink=1-(depth-q.z)*q.w;
        float px=(x+.5f-size*.5f)/pixelsPerUnit*p.x+p.z,py=(size*.5f-y-.5f)/pixelsPerUnit*p.y+p.w;
        return rights[view].mult(q.x+(px-q.x)*shrink).addLocal(ups[view].mult(q.y+(py-q.y)*shrink)).addLocal(directions[view].mult(depth)).addLocal(centre);
    }
    void dispose(){texture.getImage().dispose();}
}
