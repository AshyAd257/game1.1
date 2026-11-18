package com.Hecate.puppet.editor;

import com.Hecate.puppet.volume.*;
import com.jme3.asset.AssetManager;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.Arrays;
import javax.swing.*;

/** Edits a copy of the moving selection's silhouette. Original texture files are untouched. */
public final class SelectionShapeDialog {
    private SelectionShapeDialog(){}
    public static Strip3DDefinition show(Strip3DDefinition source,AssetManager assets){
        return show(source,assets,null,null);
    }
    public static Strip3DDefinition showBaked(VolumeDefinition source,com.Hecate.puppet.pixel.PixelAtlas atlas,AssetManager assets){return show(source.getStrip3D(),assets,source,atlas);}
    private static Strip3DDefinition show(Strip3DDefinition source,AssetManager assets,VolumeDefinition baked,com.Hecate.puppet.pixel.PixelAtlas atlas){
        BufferedImage[] images=new BufferedImage[3];int first=-1;
        for(var face:Strip3DDefinition.Surface.values()){
            String path=source.layer(face).getColorPath();if(path==null)continue;
            try{BufferedImage image=baked==null?Strip3DTextures.readImage(assets,path):com.Hecate.puppet.pixel.PixelRingTexture.readImage(assets,path);
                if(baked!=null||image.getWidth()==source.getFrameWidth()*4&&image.getHeight()==source.getFrameHeight()){
                    images[face.ordinal()]=image;if(first<0||face==Strip3DDefinition.Surface.SIDE)first=face.ordinal();
                }
            }catch(Exception ignored){ /* A missing or unreadable path does not unlock the editor. */ }
        }
        if(baked!=null){if(first<0)first=1;for(int i=0;i<3;i++)if(images[i]==null)images[i]=com.Hecate.puppet.pixel.PixelRingTexture.silhouette();}
        if(first<0){EditorDialogs.message("请先成功导入至少一层颜色贴图，再绘制选框形状。");return null;}
        var draft=source.copy();Editor panel=new Editor(draft,images,first);
        panel.baked=baked;panel.atlas=atlas;if(baked!=null)panel.followTurn.setVisible(false);
        while(EditorDialogs.confirm(panel,"绘制选框形状 · 保留 / 擦除像素格")==JOptionPane.OK_OPTION){
            try{int value=SelectionWidthDialog.readWidth(panel.sampleWidth,draft.getFrameWidth()*4);if(value!=draft.getSampleWidth())draft.setSampleWidth(value);return draft;}
            catch(IllegalArgumentException e){EditorDialogs.message(e.getMessage());}
        }
        return null;
    }
    static final class Editor extends JPanel {
        private VolumeDefinition baked;private com.Hecate.puppet.pixel.PixelAtlas atlas;
        private final Strip3DDefinition d;private final BufferedImage[] images;
        private final ArrayDeque<Strip3DDefinition> undo=new ArrayDeque<>(),redo=new ArrayDeque<>();
        private final Grid grid=new Grid();private final Preview preview=new Preview();private final JLabel status=new JLabel();
        private final JSpinner width,height,sampleWidth;private final JButton undoButton=new JButton("撤销一笔"),redoButton=new JButton("重做");
        private final JCheckBox followTurn=new JCheckBox("轮廓随转向（背面镜像，侧面自动推算）");
        private boolean syncing,erase;private int zoom=12,face;private double yaw,pitch;
        Editor(Strip3DDefinition draft,BufferedImage[] images,int first){
            super(new BorderLayout(8,8));d=draft;this.images=images;face=first;pitch=first==0?90:first==2?-90:0;
            JPanel tools=new JPanel(new GridLayout(0,1));JPanel sizes=new JPanel(new FlowLayout(FlowLayout.LEFT));
            sampleWidth=SelectionWidthDialog.createSpinner(d);
            JPanel sampling=new JPanel(new FlowLayout(FlowLayout.LEFT));sampling.add(new JLabel("选框宽度：一次显示"));sampling.add(sampleWidth);
            sampling.add(new JLabel("列像素（上限 "+d.getFrameWidth()*4+"）；只编辑遮罩，不改变模型尺寸"));tools.add(sampling);
            sampleWidth.addChangeListener(e->{if(syncing)return;remember();d.setSampleWidth((int)sampleWidth.getValue());refresh();});
            followTurn.setName("selectionFollowTurn");followTurn.setSelected(d.isSelectionFollowingTurn());followTurn.addActionListener(e->{if(syncing)return;remember();d.setSelectionFollowingTurn(followTurn.isSelected());refresh();});tools.add(followTurn);
            width=new JSpinner(new SpinnerNumberModel(d.getSelectionWidth(),1,d.getFrameWidth()*4,1));width.setName("selectionWidth");
            height=new JSpinner(new SpinnerNumberModel(d.getSelectionHeight(),1,d.getFrameHeight(),1));height.setName("selectionHeight");
            sizes.add(new JLabel("轮廓绘制网格 宽"));sizes.add(width);sizes.add(new JLabel("高"));sizes.add(height);
            sizes.add(new JLabel("上限 "+d.getFrameWidth()*4+" × "+d.getFrameHeight()+"（与贴图尺寸独立）"));
            JSpinner scale=new JSpinner(new SpinnerNumberModel(12,2,32,1));scale.addChangeListener(e->{zoom=(int)scale.getValue();refresh();});sizes.add(new JLabel("放大"));sizes.add(scale);tools.add(sizes);
            width.addChangeListener(e->resizeMask());height.addChangeListener(e->resizeMask());
            JPanel brushes=new JPanel(new FlowLayout(FlowLayout.LEFT));ButtonGroup group=new ButtonGroup();
            for(String label:new String[]{"保留","擦除"}){JToggleButton b=new JToggleButton(label,label.equals("保留"));group.add(b);brushes.add(b);b.addActionListener(e->erase=label.equals("擦除"));}
            for(String label:new String[]{"全部填满","全部清空","恢复默认方框"}){JButton b=new JButton(label);b.addActionListener(e->{remember();if(label.equals("恢复默认方框"))d.resetSelectionMask();else{boolean[] cells=d.getSelectionMask();Arrays.fill(cells,label.equals("全部填满"));d.setSelectionMask(d.getSelectionWidth(),d.getSelectionHeight(),cells);}refresh();});brushes.add(b);}
            undoButton.addActionListener(e->history(undo,redo));redoButton.addActionListener(e->history(redo,undo));brushes.add(undoButton);brushes.add(redoButton);tools.add(brushes);
            tools.add(new JLabel("左键拖画，右键擦除。亮格保留，暗格排除；形状跟随整个选框，原图不变。"));add(tools,BorderLayout.NORTH);
            JPanel right=new JPanel(new BorderLayout(4,4));right.setPreferredSize(new Dimension(270,360));right.add(new JLabel("固定宽度预览（跟随描边开关）"),BorderLayout.NORTH);right.add(preview,BorderLayout.CENTER);
            JPanel angles=new JPanel(new GridLayout(0,1));JSlider horizontal=new JSlider(-180,180,0),vertical=new JSlider(-90,90,(int)pitch);
            horizontal.setName("selectionYaw");vertical.setName("selectionPitch");horizontal.addChangeListener(e->{yaw=horizontal.getValue();preview.repaint();grid.repaint();});vertical.addChangeListener(e->{pitch=vertical.getValue();preview.repaint();grid.repaint();});
            angles.add(new JLabel("左右转动"));angles.add(horizontal);angles.add(new JLabel("俯仰"));angles.add(vertical);right.add(angles,BorderLayout.SOUTH);
            JPanel left=new JPanel(new BorderLayout());JComboBox<String> layer=new JComboBox<>(new String[]{"上层背景","中层背景","下层背景"});layer.setSelectedIndex(face);layer.addActionListener(e->{face=layer.getSelectedIndex();grid.repaint();});left.add(layer,BorderLayout.NORTH);left.add(new JScrollPane(grid),BorderLayout.CENTER);
            JSplitPane split=new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,left,right);split.setResizeWeight(.7);split.setDividerLocation(650);add(split,BorderLayout.CENTER);add(status,BorderLayout.SOUTH);setPreferredSize(new Dimension(960,580));
            getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("control Z"),"undoShape");getActionMap().put("undoShape",new AbstractAction(){public void actionPerformed(ActionEvent e){history(undo,redo);}});
            getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("control Y"),"redoShape");getActionMap().put("redoShape",new AbstractAction(){public void actionPerformed(ActionEvent e){history(redo,undo);}});refresh();
        }
        private void remember(){undo.push(d.copy());while(undo.size()>32)undo.removeLast();redo.clear();}
        private void history(ArrayDeque<Strip3DDefinition> from,ArrayDeque<Strip3DDefinition> to){if(from.isEmpty())return;to.push(d.copy());d.copyWindowFrom(from.pop());refresh();}
        private void resizeMask(){if(syncing)return;remember();int w=(int)width.getValue(),h=(int)height.getValue(),oldW=d.getSelectionWidth(),oldH=d.getSelectionHeight();boolean[] old=d.getSelectionMask(),next=new boolean[w*h];
            for(int y=0;y<Math.min(h,oldH);y++)System.arraycopy(old,y*oldW,next,y*w,Math.min(w,oldW));d.setSelectionMask(w,h,next);refresh();}
        private void refresh(){syncing=true;sampleWidth.setValue(d.getSampleWidth());width.setValue(d.getSelectionWidth());height.setValue(d.getSelectionHeight());followTurn.setSelected(d.isSelectionFollowingTurn());syncing=false;
            grid.setPreferredSize(new Dimension(d.getSelectionWidth()*zoom+16,d.getSelectionHeight()*zoom+16));grid.revalidate();grid.repaint();preview.repaint();undoButton.setEnabled(!undo.isEmpty());redoButton.setEnabled(!redo.isEmpty());
            status.setText((d.hasSelectionMask()?"自定义形状":"默认方形／矩形选框")+" · "+d.getSelectionWidth()+" × "+d.getSelectionHeight()+" 格 · 取样 "+d.getSampleWidth()+" 列 · 确定后应用；取消不修改部件");}
        private final class Grid extends JPanel {
            private boolean drawing,strokeErase;private int lastX=-1,lastY=-1;
            Grid(){setName("selectionGrid");setBackground(new Color(34,38,45));MouseAdapter mouse=new MouseAdapter(){
                public void mousePressed(MouseEvent e){if(!SwingUtilities.isLeftMouseButton(e)&&!SwingUtilities.isRightMouseButton(e))return;if(!inside(e))return;remember();drawing=true;strokeErase=erase||SwingUtilities.isRightMouseButton(e);lastX=lastY=-1;paintStroke(e);}
                public void mouseDragged(MouseEvent e){if(drawing)paintStroke(e);}
                public void mouseReleased(MouseEvent e){drawing=false;lastX=lastY=-1;}
            };addMouseListener(mouse);addMouseMotionListener(mouse);}
            private boolean inside(MouseEvent e){return e.getX()>=8&&e.getY()>=8&&e.getX()<8+d.getSelectionWidth()*zoom&&e.getY()<8+d.getSelectionHeight()*zoom;}
            private void paintStroke(MouseEvent e){if(!inside(e)){lastX=lastY=-1;return;}int x=(e.getX()-8)/zoom,y=(e.getY()-8)/zoom,w=d.getSelectionWidth();boolean[] cells=d.getSelectionMask();
                int ax=lastX<0?x:lastX,ay=lastY<0?y:lastY,dx=Math.abs(x-ax),dy=-Math.abs(y-ay),sx=ax<x?1:-1,sy=ay<y?1:-1,err=dx+dy;
                while(true){cells[ay*w+ax]=!strokeErase;if(ax==x&&ay==y)break;int twice=2*err;if(twice>=dy){err+=dy;ax+=sx;}if(twice<=dx){err+=dx;ay+=sy;}}
                lastX=x;lastY=y;d.setSelectionMask(w,d.getSelectionHeight(),cells);refresh();}
            @Override protected void paintComponent(Graphics graphics){var textureRows=baked==null?null:com.Hecate.puppet.pixel.PixelVerticalSampling.build(atlas,baked,com.Hecate.puppet.pixel.PixelViewSelector.select(baked.getBakedPixels(),(float)yaw,(float)pitch,atlas.size()),(float)pitch,images);super.paintComponent(graphics);Graphics2D g=(Graphics2D)graphics;boolean[] cells=d.getSelectionMask();int w=d.getSelectionWidth(),h=d.getSelectionHeight();BufferedImage source=images[face];
                Rectangle clip=g.getClipBounds();int yMin=Math.max(0,(clip.y-8)/zoom),yMax=Math.min(h,(clip.y+clip.height-8)/zoom+1),xMin=Math.max(0,(clip.x-8)/zoom),xMax=Math.min(w,(clip.x+clip.width-8)/zoom+1);
                for(int y=yMin;y<yMax;y++)for(int x=xMin;x<xMax;x++){int px=8+x*zoom,py=8+y*zoom;g.setColor(((x+y)&1)==0?new Color(75,82,90):new Color(93,100,109));g.fillRect(px,py,zoom,zoom);
                    if(source!=null){int sx,sy;if(baked!=null){var layer=d.layer(Strip3DDefinition.Surface.values()[face]);var choice=com.Hecate.puppet.pixel.PixelViewSelector.select(baked.getBakedPixels(),(float)yaw,(float)pitch,atlas.size());int view=(choice.revealedRows()>atlas.size()/2?choice.upperBand():choice.lowerBand())*com.Hecate.puppet.pixel.PixelAtlas.YAW_VIEWS+choice.yawStep();int cellX=(int)Math.floor((x+.5f)*baked.getWidthPx()/w-baked.getWidthPx()*.5f)+atlas.size()/2,cellY=(int)Math.floor((y+.5f)*baked.getHeightPx()/h-baked.getHeightPx()*.5f)+atlas.size()/2;sx=com.Hecate.puppet.pixel.PixelRingTexture.sourceX(atlas,baked,view,face,cellX,source.getWidth());sy=textureRows.sourceY(face,Math.max(0,Math.min(atlas.size()-1,cellY)));}else{sx=Math.floorMod((source.getWidth()-w)/2+x,source.getWidth());sy=Math.floorMod((source.getHeight()-h)/2+y,source.getHeight());}g.setColor(new Color(sy<0?0:source.getRGB(sx,sy),true));g.fillRect(px,py,zoom,zoom);}
                    g.setColor(cells[y*w+x]?new Color(68,225,190,75):new Color(0,0,0,220));g.fillRect(px,py,zoom,zoom);if(zoom>=5){g.setColor(new Color(25,30,35,150));g.drawRect(px,py,zoom,zoom);}
                }
            }
        }
        private final class Preview extends JPanel {
            Preview(){setBackground(new Color(42,46,55));}
            @Override protected void paintComponent(Graphics graphics){
                if(atlas!=null){super.paintComponent(graphics);var current=baked.copy();current.getStrip3D().copyOptionsFrom(d);BufferedImage image=com.Hecate.puppet.pixel.PixelArtwork.selected(atlas,current,(float)yaw,(float)pitch,images);
                    int scale=Math.max(1,Math.min((getWidth()-16)/image.getWidth(),(getHeight()-16)/image.getHeight()));graphics.drawImage(image,(getWidth()-image.getWidth()*scale)/2,(getHeight()-image.getHeight()*scale)/2,image.getWidth()*scale,image.getHeight()*scale,null);return;}
                super.paintComponent(graphics);var layout=TriStripSampler.layout(d,yaw,pitch);BufferedImage image=TriStripSampler.compose(d,layout,images,false);
                double extent=Math.max(1,Math.min((getWidth()-16)/2.0,(getHeight()-16)/2.0));
                double sx=extent/d.getSampleWidth(),sy=extent/d.getFrameHeight();
                double x=(getWidth()-image.getWidth()*sx)/2+((layout.canvasWidth()-layout.columns().length)&1)*sx/2;
                var transform=new java.awt.geom.AffineTransform();transform.translate(x,(getHeight()-image.getHeight()*sy)/2);transform.scale(sx,sy);
                Graphics2D g=(Graphics2D)graphics.create();g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);g.drawImage(image,transform,null);g.dispose();
            }
        }
    }
}
