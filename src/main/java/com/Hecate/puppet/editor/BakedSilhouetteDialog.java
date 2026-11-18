package com.Hecate.puppet.editor;

import com.Hecate.puppet.pixel.*;
import com.Hecate.puppet.volume.*;
import com.jme3.asset.AssetManager;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.*;

/** Per-capture hand edits. PNG/strip edits remain separate and survive geometry changes. */
final class BakedSilhouetteDialog {
    private BakedSilhouetteDialog() {}
    static VolumeDefinition show(VolumeDefinition source,PixelAtlas atlas,BufferedImage[] images) {
        Editor editor=new Editor(source.copy(),atlas,images);
        return EditorDialogs.confirm(editor,"手改剪影 · 五组方向 × 20 张") == JOptionPane.OK_OPTION ? editor.d : null;
    }
    static BufferedImage[] loadSources(VolumeDefinition d,AssetManager assets) throws java.io.IOException {
        BufferedImage[] images=new BufferedImage[3];for(var s:Strip3DDefinition.Surface.values()){String p=d.getStrip3D().layer(s).getColorPath();images[s.ordinal()]=p==null?PixelRingTexture.silhouette():com.Hecate.puppet.pixel.PixelRingTexture.readImage(assets,p);}return images;
    }
    static final class Editor extends JPanel {
        final VolumeDefinition d;final PixelAtlas atlas;final BufferedImage[] sources;
        final Grid grid=new Grid();final JLabel status=new JLabel();
        final JComboBox<String> band=new JComboBox<>(new String[]{"正底端 −90°","下斜视","平视 0°","上斜视","正顶端 +90°"});
        final JSpinner angle=new JSpinner(new SpinnerNumberModel(0,0,PixelAtlas.YAW_VIEWS-1,1));
        final ArrayDeque<VolumeDefinition> undo=new ArrayDeque<>(),redo=new ArrayDeque<>();
        boolean erase;int zoom=8;boolean[] mask;
        Editor(VolumeDefinition d,PixelAtlas atlas,BufferedImage[] sources){
            super(new BorderLayout(8,8));this.d=d;this.atlas=atlas;this.sources=sources;
            JPanel tools=new JPanel(new FlowLayout(FlowLayout.LEFT));band.setSelectedIndex(2);tools.add(band);tools.add(new JLabel("角度编号 × 18°"));tools.add(angle);
            band.addActionListener(e->refresh());angle.addChangeListener(e->refresh());
            JToggleButton keep=new JToggleButton("画上／保留",true),remove=new JToggleButton("擦除");ButtonGroup group=new ButtonGroup();group.add(keep);group.add(remove);tools.add(keep);tools.add(remove);keep.addActionListener(e->erase=false);remove.addActionListener(e->erase=true);
            JButton reset=new JButton("恢复这张自动剪影"),back=new JButton("撤销一笔"),forward=new JButton("重做");tools.add(reset);tools.add(back);tools.add(forward);
            reset.addActionListener(e->{remember();d.getBakedPixels().setEdit(view(),null);refresh();});back.addActionListener(e->history(undo,redo));forward.addActionListener(e->history(redo,undo));
            add(tools,BorderLayout.NORTH);add(new JScrollPane(grid),BorderLayout.CENTER);add(status,BorderLayout.SOUTH);setPreferredSize(new Dimension(900,650));
            getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("control Z"),"undo");getActionMap().put("undo",new AbstractAction(){public void actionPerformed(ActionEvent e){history(undo,redo);}});
            getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("control Y"),"redo");getActionMap().put("redo",new AbstractAction(){public void actionPerformed(ActionEvent e){history(redo,undo);}});refresh();
        }
        int view(){return BakedPixelDefinition.orderedBand(band.getSelectedIndex())*PixelAtlas.YAW_VIEWS+(int)angle.getValue();}
        void remember(){undo.push(d.copy());while(undo.size()>32)undo.removeLast();redo.clear();}
        void history(ArrayDeque<VolumeDefinition> from,ArrayDeque<VolumeDefinition> to){if(from.isEmpty())return;to.push(d.copy());var next=from.pop();for(int i=0;i<PixelAtlas.VIEWS;i++)d.getBakedPixels().setEdit(i,next.getBakedPixels().getEdit(i));refresh();}
        void refresh(){int n=atlas.size();var edit=d.getBakedPixels().getEdit(view());mask=edit!=null&&edit.applies(atlas.key())?edit.mask():new boolean[n*n];if(edit==null||!edit.applies(atlas.key()))for(int y=0;y<n;y++)for(int x=0;x<n;x++)mask[y*n+x]=atlas.occupied(view(),x,y);
            grid.setPreferredSize(new Dimension(n*zoom+16,n*zoom+16));grid.revalidate();grid.repaint();status.setText("仰角 "+d.getBakedPixels().elevation(view()/PixelAtlas.YAW_VIEWS)+"° · 横向观察角 "+((int)angle.getValue()*PixelAtlas.YAW_DEGREES)+"° · "+n+" × "+n+" 整格；新增像素沿用最近原像素的深度。换形状后需检查这张修改。");}
        final class Grid extends JPanel {
            boolean drawing,remove;int lastX=-1,lastY=-1;
            Grid(){setName("bakedSilhouetteGrid");MouseAdapter mouse=new MouseAdapter(){public void mousePressed(MouseEvent e){if(!inside(e))return;remember();drawing=true;remove=erase||SwingUtilities.isRightMouseButton(e);lastX=lastY=-1;stroke(e);}public void mouseDragged(MouseEvent e){if(drawing)stroke(e);}public void mouseReleased(MouseEvent e){drawing=false;lastX=lastY=-1;}};addMouseListener(mouse);addMouseMotionListener(mouse);}
            boolean inside(MouseEvent e){return e.getX()>=8&&e.getY()>=8&&e.getX()<8+atlas.size()*zoom&&e.getY()<8+atlas.size()*zoom;}
            void stroke(MouseEvent e){if(!inside(e)){lastX=lastY=-1;return;}int n=atlas.size(),x=(e.getX()-8)/zoom,y=(e.getY()-8)/zoom;
                int ax=lastX<0?x:lastX,ay=lastY<0?y:lastY,dx=Math.abs(x-ax),dy=-Math.abs(y-ay),sx=ax<x?1:-1,sy=ay<y?1:-1,err=dx+dy;
                while(true){mask[ay*n+ax]=!remove;if(ax==x&&ay==y)break;int twice=2*err;if(twice>=dy){err+=dy;ax+=sx;}if(twice<=dx){err+=dx;ay+=sy;}}
                d.getBakedPixels().setEdit(view(),new BakedPixelDefinition.MaskEdit(n,mask,atlas.key()));lastX=x;lastY=y;repaint();
            }
            @Override protected void paintComponent(Graphics graphics){super.paintComponent(graphics);Graphics2D g=(Graphics2D)graphics;int n=atlas.size();BufferedImage image=PixelArtwork.view(atlas,d,view(),sources);Rectangle clip=g.getClipBounds();
                for(int y=Math.max(0,(clip.y-8)/zoom);y<Math.min(n,(clip.y+clip.height-8)/zoom+1);y++)for(int x=Math.max(0,(clip.x-8)/zoom);x<Math.min(n,(clip.x+clip.width-8)/zoom+1);x++){
                    int px=8+x*zoom,py=8+y*zoom;g.setColor(((x+y)&1)==0?new Color(75,82,90):new Color(93,100,109));g.fillRect(px,py,zoom,zoom);g.setColor(new Color(image.getRGB(x,y),true));g.fillRect(px,py,zoom,zoom);
                    if(atlas.occupied(view(),x,y)){g.setColor(mask[y*n+x]?new Color(60,230,180,70):new Color(0,0,0,220));g.fillRect(px,py,zoom,zoom);}g.setColor(new Color(25,30,35,140));g.drawRect(px,py,zoom,zoom);
                }
            }
        }
    }
}
