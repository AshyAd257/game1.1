package com.Hecate.puppet.editor;
import com.Hecate.puppet.volume.*;
import com.jme3.asset.AssetManager;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import javax.swing.*;

/** Click the actual source image to edit per-row importance; no source PNG is modified. */
public final class RowImportanceDialog {
    private RowImportanceDialog(){}
    public static Strip3DDefinition show(Strip3DDefinition source,AssetManager assets){
        Strip3DDefinition draft=source.copy();Editor panel=new Editor(draft,assets);
        return EditorDialogs.confirm(panel,"贴图行重要度 · 点击行修改 1–3")==JOptionPane.OK_OPTION?draft:null;
    }
    static final class Editor extends JPanel {
        private final Strip3DDefinition d;private final BufferedImage[] images=new BufferedImage[3];private final String[] errors=new String[3];
        private final JLabel info=new JLabel("点击贴图上的一行：1 → 2 → 3 → 1");private final Rows rows;private final Card card;private int face=1,zoom=8,brush,row=-1;private double pitch=45;
        Editor(Strip3DDefinition d,AssetManager assets){
            super(new BorderLayout(8,8));this.d=d;int first=-1;
            for(var surface:Strip3DDefinition.Surface.values()){int i=surface.ordinal();String path=d.layer(surface).getColorPath();try{images[i]=path==null?Strip3DTextures.blankStrip(d):Strip3DTextures.readImage(assets,path);if(images[i].getWidth()!=d.getFrameWidth()*4||images[i].getHeight()!=d.getFrameHeight())throw new IllegalArgumentException("贴图尺寸与部件设置不一致");if(path!=null&&first<0)first=i;}catch(Exception ex){errors[i]=ex.getMessage();images[i]=Strip3DTextures.blankStrip(d);}}
            if(d.layer(Strip3DDefinition.Surface.SIDE).getColorPath()==null&&first>=0)face=first;
            rows=new Rows();card=new Card();JPanel tools=new JPanel(new GridLayout(0,1,4,4)),line=new JPanel(new FlowLayout(FlowLayout.LEFT));
            JComboBox<String> layer=new JComboBox<>(new String[]{"上层贴图","中层贴图","下层贴图"});layer.setName("importanceLayer");layer.setSelectedIndex(face);layer.addActionListener(e->{face=layer.getSelectedIndex();row=-1;changed();});line.add(layer);
            JCheckBox enabled=new JCheckBox("启用行重要度",d.isImportanceEnabled());enabled.addActionListener(e->{d.setImportanceEnabled(enabled.isSelected());changed();});line.add(enabled);
            JSpinner size=new JSpinner(new SpinnerNumberModel(8,4,24,1));size.setName("importanceZoom");size.addChangeListener(e->{zoom=(int)size.getValue();rows.resize();});line.add(new JLabel("放大倍数"));line.add(size);tools.add(line);
            JPanel brushes=new JPanel(new FlowLayout(FlowLayout.LEFT));ButtonGroup group=new ButtonGroup();String[] labels={"点击循环 1→2→3","设为 1","设为 2","设为 3"};for(int i=0;i<4;i++){final int value=i;JToggleButton button=new JToggleButton(labels[i],i==0);button.addActionListener(e->brush=value);group.add(button);brushes.add(button);}tools.add(brushes);
            tools.add(new JLabel("1 灰：先舍弃　 2 黄：普通　 3 绿：优先保留；整行共用一个重要度。"));add(tools,BorderLayout.NORTH);
            JScrollPane scroller=new JScrollPane(rows);scroller.getVerticalScrollBar().setUnitIncrement(24);JPanel preview=new JPanel(new BorderLayout());preview.setPreferredSize(new Dimension(210,260));preview.add(new JLabel("挑行效果预览（正面）"),BorderLayout.NORTH);preview.add(card,BorderLayout.CENTER);
            JSlider angle=new JSlider(-90,90,45);angle.setMajorTickSpacing(45);angle.setPaintTicks(true);angle.setPaintLabels(true);angle.addChangeListener(e->{pitch=angle.getValue();card.repaint();});preview.add(angle,BorderLayout.SOUTH);
            JSplitPane split=new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,scroller,preview);split.setResizeWeight(.8);split.setDividerLocation(810);add(split,BorderLayout.CENTER);add(info,BorderLayout.SOUTH);setPreferredSize(new Dimension(1060,570));rows.resize();changed();
        }
        private void changed(){rows.repaint();card.repaint();String name=new String[]{"上层","中层","下层"}[face];info.setText(errors[face]!=null?name+"："+errors[face]:d.layer(Strip3DDefinition.Surface.values()[face]).getColorPath()==null?name+"尚未导入贴图；请先在图像面板导入 PNG。":row<0?name+"：点击贴图或左侧数值修改；3 优先保留。":name+" · 第 "+(row+1)+" 行 · 重要度 "+d.getImportance()[face*d.getFrameHeight()+row]+"（确定后保存，取消不修改）");}
        private final class Rows extends JPanel {
            private static final int LEFT=64,TOP=24;
            Rows(){setName("importanceRows");setBackground(new Color(35,38,46));setToolTipText("点击行切换重要度");addMouseListener(new MouseAdapter(){public void mousePressed(MouseEvent e){if(!SwingUtilities.isLeftMouseButton(e)||d.layer(Strip3DDefinition.Surface.values()[face]).getColorPath()==null||errors[face]!=null)return;int r=(e.getY()-TOP)/zoom;if(e.getY()<TOP||e.getX()<0||e.getX()>=LEFT+d.getFrameWidth()*4*zoom||r<0||r>=d.getFrameHeight())return;row=r;int[] values=d.getImportance();int index=face*d.getFrameHeight()+row;values[index]=brush==0?values[index]%3+1:brush;d.setImportance(values);changed();}});}
            void resize(){setPreferredSize(new Dimension(LEFT+d.getFrameWidth()*4*zoom+8,TOP+d.getFrameHeight()*zoom+8));revalidate();repaint();}
            @Override public String getToolTipText(MouseEvent e){int r=(e.getY()-TOP)/zoom;return e.getY()>=TOP&&r>=0&&r<d.getFrameHeight()?"第 "+(r+1)+" 行 · 重要度 "+d.getImportance()[face*d.getFrameHeight()+r]:"点击图片中的任意一行";}
            @Override protected void paintComponent(Graphics graphics){super.paintComponent(graphics);Graphics2D g=(Graphics2D)graphics;g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);g.setColor(Color.WHITE);g.drawString("行号   重要度",3,16);int w=d.getFrameWidth()*4*zoom,h=d.getFrameHeight()*zoom;for(int y=0;y<h;y+=zoom)for(int x=0;x<w;x+=zoom){g.setColor(((x/zoom+y/zoom)&1)==0?new Color(52,55,62):new Color(65,68,76));g.fillRect(LEFT+x,TOP+y,zoom,zoom);}g.drawImage(images[face],LEFT,TOP,w,h,null);
                int[] values=d.getImportance();Color[] colors={new Color(140,149,164),new Color(235,190,75),new Color(80,218,152)};g.setFont(new Font(Font.MONOSPACED,Font.PLAIN,Math.max(7,Math.min(13,zoom))));for(int r=0;r<d.getFrameHeight();r++){int value=values[face*d.getFrameHeight()+r];g.setColor(colors[value-1]);g.fillRect(36,TOP+r*zoom,24,zoom);g.setColor(Color.BLACK);g.drawString(Integer.toString(value),43,TOP+(r+1)*zoom-1);g.setColor(Color.LIGHT_GRAY);g.drawString(Integer.toString(r+1),3,TOP+(r+1)*zoom-1);}
                if(row>=0){g.setColor(Color.WHITE);g.drawRect(LEFT,TOP+row*zoom,w-1,zoom-1);}
            }
        }
        private final class Card extends JPanel {
            Card(){setBackground(new Color(28,31,39));}
            @Override protected void paintComponent(Graphics g){super.paintComponent(g);var layout=TriStripSampler.layout(d,0,pitch);var image=TriStripSampler.compose(d,layout,images,false);int scale=Math.max(1,Math.min((getWidth()-12)/image.getWidth(),(getHeight()-12)/image.getHeight()));g.drawImage(image,(getWidth()-image.getWidth()*scale)/2,(getHeight()-image.getHeight()*scale)/2,image.getWidth()*scale,image.getHeight()*scale,null);}
        }
    }
}
