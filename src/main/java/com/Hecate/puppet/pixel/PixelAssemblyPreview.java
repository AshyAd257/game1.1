package com.Hecate.puppet.pixel;

import com.Hecate.puppet.volume.VolumeDefinition;
import com.jme3.math.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Arrays;

/** Explicit 20-view assembly check from approved cached cells; no model capture or pose cache. */
public final class PixelAssemblyPreview {
    private PixelAssemblyPreview(){}
    public record Part(VolumeDefinition definition,PixelAtlas atlas,Transform world,int priority,BufferedImage[] sources){
        Vector3f photoCentre(){
            Vector3f scale=world.getScale(),centre=atlas.centre();
            centre.multLocal(Math.signum(scale.x),Math.signum(scale.y),Math.signum(scale.z));
            return world.getRotation().mult(centre).addLocal(world.getTranslation());
        }
    }
    public static BufferedImage contactSheet(List<Part> parts){
        if(parts.isEmpty())throw new IllegalArgumentException("先导入人物部件");
        Vector3f center=new Vector3f();for(var p:parts)center.addLocal(p.photoCentre());center.divideLocal(parts.size());float radius=0;for(var p:parts)radius=Math.max(radius,p.photoCentre().distance(center)+p.atlas().radius());
        int n=(int)Math.ceil(radius*2*PixelMetrics.PIXELS_PER_METRE)+4;if(n>512)throw new IllegalArgumentException("整人检查超过 512×512 像素，请减少人物整体尺寸");
        BufferedImage sheet=new BufferedImage(n*5,(n+20)*4,BufferedImage.TYPE_INT_ARGB);Graphics2D g=sheet.createGraphics();
        for(int yaw=0;yaw<PixelAtlas.YAW_VIEWS;yaw++){BufferedImage frame=render(parts,center,n,yaw*PixelAtlas.YAW_DEGREES);g.drawImage(frame,yaw%5*n,yaw/5*(n+20),null);g.setColor(new Color(210,215,225));g.drawString(yaw*PixelAtlas.YAW_DEGREES+"°",yaw%5*n+4,yaw/5*(n+20)+n+15);}g.dispose();return sheet;
    }
    public static BufferedImage render(List<Part> parts,Vector3f center,int n,float yaw){
        return render(parts,center,n,yaw,0);
    }
    public static BufferedImage render(List<Part> parts,Vector3f center,int n,float yaw,float pitch){
        BufferedImage image=new BufferedImage(n,n,BufferedImage.TYPE_INT_ARGB);float[] depths=new float[n*n];int[] priorities=new int[n*n];Arrays.fill(depths,Float.NEGATIVE_INFINITY);Arrays.fill(priorities,Integer.MIN_VALUE);
        Vector3f right=PixelViewSelector.right(yaw),toward=PixelViewSelector.direction(yaw,pitch),up=toward.cross(right).normalizeLocal();
        for(var part:parts){var d=part.definition();var atlas=part.atlas();int size=atlas.size();float ppu=atlas.pixelsPerUnit();Quaternion rotation=part.world().getRotation();Vector3f scale=part.world().getScale(),sign=new Vector3f(Math.signum(scale.x),Math.signum(scale.y),Math.signum(scale.z));if(sign.x*sign.y*sign.z==0)continue;
            Vector3f local=rotation.inverse().mult(toward).multLocal(sign).normalizeLocal();float localYaw=PixelViewSelector.localYaw(local,rotation.inverse().mult(right).multLocal(sign)),localPitch=FastMath.asin(FastMath.clamp(local.y,-1,1))*FastMath.RAD_TO_DEG;var selection=PixelViewSelector.select(d.getBakedPixels(),localYaw,localPitch,size);
            PixelBillboardLayout layout=PixelBillboardLayout.create(d.getBakedPixels(),localYaw,localPitch,selection,ppu);
            Vector3f r=rotation.mult(layout.right().mult(sign)),origin=part.world().getTranslation().subtract(center);
            Vector3f photoOrigin=part.photoCentre().subtractLocal(center);
            BufferedImage colors=PixelArtwork.selected(atlas,d,localYaw,localPitch,part.sources());byte[] low=PixelEdits.resolve(atlas,d,selection.lowerBand()*PixelAtlas.YAW_VIEWS+selection.yawStep()),high=selection.upperBand()==selection.lowerBand()?low:PixelEdits.resolve(atlas,d,selection.upperBand()*PixelAtlas.YAW_VIEWS+selection.yawStep());
            int priority=part.priority()+d.getBakedPixels().getPriority();
            for(int piece=0;piece<2;piece++){
            if(piece==0&&selection.revealedRows()==size||piece==1&&selection.revealedRows()==0)continue;
            Vector3f u=rotation.mult((piece==0?layout.lowerUp():layout.upperUp()).mult(sign));
            Vector3f planeCentre=rotation.mult((piece==0?layout.lowerCentre():layout.upperCentre()).mult(sign)).addLocal(photoOrigin);
            float ax=r.dot(right),ay=r.dot(up),bx=u.dot(right),by=u.dot(up),det=ax*by-bx*ay;if(Math.abs(det)<1e-7)continue;float ox=planeCentre.dot(right),oy=planeCentre.dot(up);
            for(int sy=0;sy<n;sy++)for(int sx=0;sx<n;sx++){float x=(sx+.5f-n*.5f)/PixelMetrics.PIXELS_PER_METRE-ox,y=(n*.5f-sy-.5f)/PixelMetrics.PIXELS_PER_METRE-oy;int cx=(int)Math.floor((by*x-bx*y)/det*ppu+size*.5f+.0001f),cy=size-1-(int)Math.floor((ax*y-ay*x)/det*ppu+size*.5f+.0001f);if(cx<0||cy<0||cx>=size||cy>=size||(cy<selection.revealedRows())!=(piece==1))continue;int color=colors.getRGB(cx,cy);if((color>>>24)<128)continue;
                int view=selection.viewForRow(cy),o=(cy*size+cx)*4;byte[] cells=cy<selection.revealedRows()?high:low;if(cells[o+3]==0)continue;int encoded=((cells[o]&255)<<16)|((cells[o+1]&255)<<8)|(cells[o+2]&255);float z=(encoded/16777215f*2-1)*atlas.radius();Vector3f point=atlas.point(view,cx,cy,z);float depth=rotation.mult(point.mult(sign)).addLocal(origin).dot(toward);int i=sy*n+sx;if(depth>depths[i]+1e-5f||Math.abs(depth-depths[i])<=1e-5f&&priority>priorities[i]){depths[i]=depth;priorities[i]=priority;image.setRGB(sx,sy,color);}
            }
            }
        }
        return image;
    }
}
