package com.Hecate.puppet.editor;

import com.Hecate.puppet.volume.*;
import com.jme3.asset.AssetManager;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

/** All edits, including row priorities, are staged and committed as one undoable operation. */
public final class Strip3DTextureDialog {
    private Strip3DTextureDialog(){}
    public static VolumeDefinition show(VolumeDefinition source,AssetManager assets){
        VolumeDefinition draft=source.copy();var d=draft.getStrip3D();JPanel root=new JPanel(new BorderLayout(8,8));JTabbedPane tabs=new JTabbedPane();JTextField[][] paths=new JTextField[3][2];
        String[] names={"上层 · 顶面","中层 · 侧面","下层 · 底面"};int index=0;
        for(var face:Strip3DDefinition.Surface.values()){
            JPanel form=new JPanel(new GridLayout(0,1,6,10));form.setBorder(BorderFactory.createEmptyBorder(16,16,16,16));var layer=d.layer(face);
            paths[index][0]=fileRow(form,"颜色 PNG（宽度 = 四个视角的宽度）",layer.getColorPath());paths[index][1]=fileRow(form,"法线 PNG（RGB = 部件局部 XYZ，与颜色图同尺寸）",layer.getNormalPath());
            form.add(new JLabel("<html>三张条带尺寸一致，横向循环取整列，纵向选择整行。<br>顶面、底面的四段绘制方向请参考导出的示例图。<br>未导入或清空路径的层保持透明，不自动添加贴图。</html>"));tabs.addTab(names[index++],form);
        }
        Preview preview=new Preview(d,assets);JPanel controls=new JPanel(new GridLayout(0,2,6,6));
        JComboBox<String> v=new JComboBox<>(new String[]{"纵向：透视挑行","纵向：固定窗口滑动"}),h=new JComboBox<>(new String[]{"横向：滑动","横向：透视挑列"});
        v.setSelectedIndex(d.getVertical()==Strip3DDefinition.Sampling.PERSPECTIVE?0:1);h.setSelectedIndex(d.getHorizontal()==Strip3DDefinition.Sampling.SLIDE?0:1);
        JCheckBox priority=new JCheckBox("按行重要度保留五官",d.isImportanceEnabled());
        v.addActionListener(e->{d.setVertical(v.getSelectedIndex()==0?Strip3DDefinition.Sampling.PERSPECTIVE:Strip3DDefinition.Sampling.SLIDE);preview.repaint();});
        h.addActionListener(e->{d.setHorizontal(h.getSelectedIndex()==0?Strip3DDefinition.Sampling.SLIDE:Strip3DDefinition.Sampling.PERSPECTIVE);preview.repaint();});
        priority.addActionListener(e->{d.setImportanceEnabled(priority.isSelected());preview.repaint();});controls.add(v);controls.add(h);controls.add(priority);
        JButton reset=new JButton("恢复原型行重要度");reset.addActionListener(e->{d.resetImportance();preview.repaint();});controls.add(reset);
        JSlider yaw=new JSlider(-180,180,0),pitch=new JSlider(-90,90,0);yaw.setMajorTickSpacing(90);yaw.setPaintTicks(true);yaw.setPaintLabels(true);pitch.setMajorTickSpacing(45);pitch.setPaintTicks(true);pitch.setPaintLabels(true);
        controls.add(new JLabel("预览水平角"));controls.add(new JLabel("预览俯仰角"));controls.add(yaw);controls.add(pitch);
        yaw.addChangeListener(e->{preview.yaw=yaw.getValue();preview.repaint();});pitch.addChangeListener(e->{preview.pitch=pitch.getValue();preview.repaint();});
        JButton refresh=new JButton("应用上方路径到预览");refresh.addActionListener(e->{try{readFields(d,paths,assets);preview.reload();}catch(Exception ex){JOptionPane.showMessageDialog(root,ex.getMessage());}});controls.add(refresh);
        JButton uniform=new JButton("全部行重要度设为 2");uniform.addActionListener(e->{int[] a=d.getImportance();java.util.Arrays.fill(a,2);d.setImportance(a);preview.repaint();});controls.add(uniform);
        JPanel sampling=new JPanel(new BorderLayout(6,6));sampling.add(controls,BorderLayout.NORTH);sampling.add(new JScrollPane(preview),BorderLayout.CENTER);tabs.addTab("取样与行重要度",sampling);root.add(tabs,BorderLayout.CENTER);
        JButton examples=new JButton("导出原型的上／中／下三张示例 PNG");examples.addActionListener(e->{JFileChooser picker=new JFileChooser();picker.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);if(picker.showSaveDialog(root)==JFileChooser.APPROVE_OPTION)try{Strip3DTextures.exportExamples(picker.getSelectedFile().toPath());JOptionPane.showMessageDialog(root,"三张示例已导出");}catch(Exception ex){JOptionPane.showMessageDialog(root,ex.getMessage());}});root.add(examples,BorderLayout.SOUTH);root.setPreferredSize(new Dimension(780,620));
        while(EditorDialogs.confirm(root,"三条带贴图与取样")==JOptionPane.OK_OPTION)try{readFields(d,paths,assets);d.setEnabled(true);return draft;}catch(Exception e){JOptionPane.showMessageDialog(root,e.getMessage());}
        return null;
    }
    private static void readFields(Strip3DDefinition d,JTextField[][] fields,AssetManager assets)throws Exception{
        var candidate=d.copy();for(int i=0;i<3;i++){var l=candidate.layer(Strip3DDefinition.Surface.values()[i]);l.setColorPath(fields[i][0].getText());l.setNormalPath(fields[i][1].getText());}
        Strip3DTextures.validateSources(candidate,assets);d.copyOptionsFrom(candidate);
        for(var face:Strip3DDefinition.Surface.values()){d.layer(face).setColorPath(candidate.layer(face).getColorPath());d.layer(face).setNormalPath(candidate.layer(face).getNormalPath());}
    }
    private static JTextField fileRow(JPanel form,String label,String initial){
        JPanel row=new JPanel(new BorderLayout(6,4));row.add(new JLabel(label),BorderLayout.NORTH);JTextField field=new JTextField(initial==null?"":initial);row.add(field,BorderLayout.CENTER);JButton browse=new JButton("导入 PNG…");browse.addActionListener(e->{JFileChooser picker=new JFileChooser();picker.setFileFilter(new FileNameExtensionFilter("PNG","png"));if(picker.showOpenDialog(form)==JFileChooser.APPROVE_OPTION)field.setText(picker.getSelectedFile().getAbsolutePath());});row.add(browse,BorderLayout.EAST);form.add(row);return field;
    }
    private static final class Preview extends JPanel {
        private final Strip3DDefinition d;private final AssetManager assets;private final BufferedImage[] images=new BufferedImage[3];private double yaw,pitch;private int zoom=4;private String error;
        Preview(Strip3DDefinition d,AssetManager assets){this.d=d;this.assets=assets;reload();setBackground(new Color(32,35,43));addMouseListener(new MouseAdapter(){public void mousePressed(MouseEvent e){int row=(e.getY()-36)/zoom;if(e.getY()>=36&&e.getX()>=0&&e.getX()<42&&row>=0&&row<d.getFrameHeight()*3){int[] a=d.getImportance();a[row]=a[row]%3+1;d.setImportance(a);repaint();}}});}
        void reload(){error=null;zoom=Math.max(1,Math.min(4,384/(d.getFrameWidth()*4)));for(var f:Strip3DDefinition.Surface.values())try{String path=d.layer(f).getColorPath();images[f.ordinal()]=path!=null?Strip3DTextures.readImage(assets,path):Strip3DTextures.blankStrip(d);}catch(Exception e){error=e.getMessage();images[f.ordinal()]=Strip3DTextures.blankStrip(d);}setPreferredSize(new Dimension(740,Math.max(320,d.getFrameHeight()*3*zoom+60)));revalidate();repaint();}
        protected void paintComponent(Graphics graphics){super.paintComponent(graphics);Graphics2D g=(Graphics2D)graphics;g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);g.setColor(Color.WHITE);g.drawString("左侧色条点击循环 1 / 2 / 3（3 优先保留）；暗色为未选取的行、列",10,20);
            var l=TriStripSampler.layout(d,yaw,pitch);int fh=d.getFrameHeight(),w=d.getFrameWidth()*4;int[] priority=d.getImportance();boolean[] rows=new boolean[fh*3],cols=new boolean[w];for(int r:l.rows())rows[r]=true;for(int c:l.columns())cols[c]=true;
            for(int i=0;i<3;i++)g.drawImage(images[i],42,36+i*fh*zoom,w*zoom,fh*zoom,null);
            Color[] tier={new Color(110,120,145),new Color(224,173,65),new Color(76,214,170)};
            for(int r=0;r<fh*3;r++){g.setColor(tier[priority[r]-1]);g.fillRect(8,36+r*zoom,24,zoom);if(!rows[r]){g.setColor(new Color(0,0,0,160));g.fillRect(42,36+r*zoom,w*zoom,zoom);}}
            for(int c=0;c<w;c++)if(!cols[c]){g.setColor(new Color(0,0,0,100));g.fillRect(42+c*zoom,36,zoom,fh*3*zoom);}
            var card=TriStripSampler.compose(d,l,images,false);int cx=60+w*zoom,cz=Math.max(1,Math.min(5,220/l.canvasWidth()));g.drawImage(card,cx,50,card.getWidth()*cz,card.getHeight()*cz,null);g.setColor(Color.WHITE);g.drawString("选取 "+l.columns().length+" 列 × "+l.rows().length+" 行",cx,40);if(error!=null)g.drawString(error,10,fh*3*zoom+55);
        }
    }
}
