package com.Hecate.puppet.editor;

import com.Hecate.puppet.pixel.*;
import com.Hecate.puppet.volume.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.BitSet;

/** The original PNG with the same whole-pixel sampling mask used by the part renderer. */
final class PixelTextureCanvas extends JPanel {
    static final int MARGIN=12;
    final PixelStripTextureDialog.Editor editor;
    int zoom=2;boolean editingCentre,editingRows,dragging;int importanceBrush=3;int pressX,pressY,startX,startY;
    PixelTextureCanvas(PixelStripTextureDialog.Editor editor){
        this.editor=editor;setBackground(new Color(40,46,55));setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        var mouse=new MouseAdapter(){
            @Override public void mousePressed(MouseEvent event){
                if(!SwingUtilities.isLeftMouseButton(event)||editor.paths[editor.activeSurface].getText().isBlank())return;
                int x=columnAt(event.getX()),y=rowAt(event.getY());var image=image();
                if(x<0||x>=image.getWidth()||y<0||y>=image.getHeight())return;
                if(editingRows){editor.setTextureRowImportance(y,importanceBrush,false);pressY=y;dragging=true;return;}
                if(editingCentre){editor.setTextureCentre(x,y,false);startX=editor.draft.getBakedPixels().getTextureOriginX();startY=editor.draft.getBakedPixels().getTextureOriginY();}
                else{
                    if(!coverage().get(y*image.getWidth()+x))editor.placeTextureWindow(x,y,false);
                    startX=offsetX();startY=offsetY();
                }
                pressX=x;pressY=y;dragging=true;
            }
            @Override public void mouseDragged(MouseEvent event){
                if(!dragging)return;if(editingRows){int row=rowAt(event.getY());if(row>=0&&row<image().getHeight()){for(int y=Math.min(row,pressY);y<=Math.max(row,pressY);y++)editor.setTextureRowImportance(y,importanceBrush,false);pressY=row;}return;}int dx=columnAt(event.getX())-pressX,dy=rowAt(event.getY())-pressY;
                if(editingCentre)editor.setTextureCentre(PixelRingTexture.originX(image().getWidth())+startX+dx,PixelRingTexture.originY(image().getHeight())+startY+dy,false);
                else editor.setWindowOffset(startX+dx,startY+dy,false);
            }
            @Override public void mouseReleased(MouseEvent event){if(dragging){dragging=false;editor.publish();}}
        };
        addMouseListener(mouse);addMouseMotionListener(mouse);
    }
    BufferedImage image(){var image=editor.images[editor.activeSurface];return image==null?PixelRingTexture.silhouette():image;}
    int columnAt(int screenX){return Math.floorDiv(screenX-MARGIN,zoom);}
    int rowAt(int screenY){return Math.floorDiv(screenY-MARGIN,zoom);}
    int screenX(int column){return MARGIN+column*zoom+zoom/2;}
    int screenY(int row){return MARGIN+row*zoom+zoom/2;}
    int offsetX(){return ((Number)editor.xs[editor.activeSurface].getValue()).intValue();}
    int offsetY(){return ((Number)editor.ys[editor.activeSurface].getValue()).intValue();}
    int centreX(){return PixelRingTexture.column(PixelRingTexture.originX(image().getWidth())+editor.draft.getBakedPixels().getTextureOriginX(),image().getWidth());}
    int centreY(){int y=PixelRingTexture.originY(image().getHeight())+editor.draft.getBakedPixels().getTextureOriginY();return editor.draft.getBakedPixels().isVerticalTextureEnabled()?y:Math.floorMod(y,image().getHeight());}
    int windowX(){return PixelRingTexture.column(centreX()+offsetX()+PixelRingTexture.phase(choice().yawStep(),image().getWidth()),image().getWidth());}
    int windowY(){int y=centreY()+offsetY()-PixelVerticalSampling.shift(editor.atlas,editor.draft,editor.pitch);return editor.draft.getBakedPixels().isVerticalTextureEnabled()?y:Math.floorMod(y,image().getHeight());}
    PixelViewSelector.Selection choice(){return PixelViewSelector.select(editor.draft.getBakedPixels(),editor.yaw,editor.pitch,editor.atlas.size());}
    BitSet coverage(){
        var image=image();int w=image.getWidth(),h=image.getHeight(),n=editor.atlas.size();BitSet mask=new BitSet(w*h);
        var choice=choice();var rowMap=PixelVerticalSampling.build(editor.atlas,editor.draft,choice,editor.pitch,editor.images);int lowView=choice.lowerBand()*PixelAtlas.YAW_VIEWS+choice.yawStep(),highView=choice.upperBand()*PixelAtlas.YAW_VIEWS+choice.yawStep();
        byte[] low=PixelEdits.resolve(editor.atlas,editor.draft,lowView),high=highView==lowView?low:PixelEdits.resolve(editor.atlas,editor.draft,highView);
        for(int y=0;y<n;y++)for(int x=0;x<n;x++){
            boolean upper=y<choice.revealedRows();int layer=((upper?high:low)[(y*n+x)*4+3]&255)/64-1;
            if(layer<0||!editor.shared.isSelected()&&layer!=editor.activeSurface)continue;
            int view=upper?highView:lowView;
            int px=PixelRingTexture.sourceX(editor.atlas,editor.draft,view,layer,x,w),py=rowMap.sourceY(layer,y);if(py<0)continue;
            mask.set(py*w+px);
        }
        return mask;
    }
    @Override public Dimension getPreferredSize(){return new Dimension(Math.max(320,image().getWidth()*zoom+MARGIN*2),Math.max(160,image().getHeight()*zoom+MARGIN*2));}
    @Override protected void paintComponent(Graphics graphics){
        super.paintComponent(graphics);Graphics2D g=(Graphics2D)graphics.create();
        if(editor.paths[editor.activeSurface].getText().isBlank()){g.setColor(Color.LIGHT_GRAY);g.drawString("导入贴图后，点击设置中心或拖动黄色选框。",20,40);g.dispose();return;}
        var image=image();int w=image.getWidth(),h=image.getHeight();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        for(int y=0;y<h;y+=8)for(int x=0;x<w;x+=8){g.setColor(((x/8+y/8)&1)==0?new Color(85,90,100):new Color(65,70,80));g.fillRect(MARGIN+x*zoom,MARGIN+y*zoom,Math.min(8,w-x)*zoom,Math.min(8,h-y)*zoom);}
        g.drawImage(image,MARGIN,MARGIN,w*zoom,h*zoom,null);
        var mask=coverage();g.setColor(new Color(255,235,80,35));
        for(int cell=mask.nextSetBit(0);cell>=0;cell=mask.nextSetBit(cell+1))g.fillRect(MARGIN+(cell%w)*zoom,MARGIN+(cell/w)*zoom,zoom,zoom);
        g.setColor(new Color(255,235,50));
        for(int cell=mask.nextSetBit(0);cell>=0;cell=mask.nextSetBit(cell+1)){
            int x=cell%w,y=cell/w,px=MARGIN+x*zoom,py=MARGIN+y*zoom;
            if(x==0||!mask.get(cell-1))g.drawLine(px,py,px,py+zoom);
            if(x==w-1||!mask.get(cell+1))g.drawLine(px+zoom,py,px+zoom,py+zoom);
            if(y==0||!mask.get(cell-w))g.drawLine(px,py,px+zoom,py);
            if(y==h-1||!mask.get(cell+w))g.drawLine(px,py+zoom,px+zoom,py+zoom);
        }
        if(zoom>=6){g.setColor(new Color(255,255,255,35));for(int x=0;x<=w;x++)g.drawLine(MARGIN+x*zoom,MARGIN,MARGIN+x*zoom,MARGIN+h*zoom);for(int y=0;y<=h;y++)g.drawLine(MARGIN,MARGIN+y*zoom,MARGIN+w*zoom,MARGIN+y*zoom);}
        if(editor.verticalSlide.isSelected()||editingRows)for(int y=0;y<h;y++){
            int value=editor.draft.getBakedPixels().rowImportance(editor.activeSurface,y);
            g.setColor(new Color[]{new Color(145,150,160),new Color(235,190,65),new Color(75,215,145)}[value-1]);g.fillRect(MARGIN-9,MARGIN+y*zoom,7,zoom);
        }
        cross(g,windowX(),windowY(),new Color(255,235,50),7);cross(g,centreX(),centreY(),new Color(255,70,80),10);
        g.setColor(Color.LIGHT_GRAY);g.drawRect(MARGIN-1,MARGIN-1,w*zoom+1,h*zoom+1);g.dispose();
    }
    void cross(Graphics2D g,int x,int y,Color color,int arm){int px=screenX(x),py=screenY(y);g.setColor(Color.BLACK);g.setStroke(new BasicStroke(3));g.drawLine(px-arm,py,px+arm,py);g.drawLine(px,py-arm,px,py+arm);g.setColor(color);g.setStroke(new BasicStroke(1));g.drawLine(px-arm,py,px+arm,py);g.drawLine(px,py-arm,px,py+arm);}
}
