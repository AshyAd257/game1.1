package com.Hecate.puppet.editor;

import com.Hecate.puppet.pixel.*;
import com.Hecate.puppet.volume.*;
import com.jme3.asset.AssetManager;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Objects;
import java.util.function.Consumer;

/** A texture edit previews on the actual part and closes with the last valid settings. */
final class PixelStripTextureDialog {
    private PixelStripTextureDialog(){}
    static VolumeDefinition show(VolumeDefinition source,AssetManager assets,Consumer<VolumeDefinition> apply){
        try(var lease=PixelAtlasCache.SHARED.acquire(source)){
            Editor panel=new Editor(source,lease.atlas(),assets,apply);
            while(true){
                int choice=EditorDialogs.confirm(panel,"条带贴图 · 即时应用到部件","完成","取消本次修改");
                try{return panel.finish(choice);}catch(Exception error){EditorDialogs.message(error.getMessage());}
            }
        }catch(Exception error){EditorDialogs.message(error.getMessage());return null;}
    }
    /** Swing controls are kept independent of a native window, so the actual edit flow can be tested. */
    static final class Editor extends JPanel {
        final VolumeDefinition draft;final PixelAtlas atlas;final AssetManager assets;
        final PixelRingTexture.Requirement required;final Consumer<VolumeDefinition> apply;
        final JTextField[] paths=new JTextField[3];final JSpinner[] xs=new JSpinner[3],ys=new JSpinner[3];
        final BufferedImage[] images=new BufferedImage[3];final JLabel[] sizes=new JLabel[3];
        final JCheckBox shared=new JCheckBox("整个部件使用同一张贴图");
        final JLabel status=new JLabel();final Preview preview=new Preview();
        final PixelTextureCanvas textureCanvas=new PixelTextureCanvas(this);
        final JCheckBox verticalSlide=new JCheckBox("上下移动选框（以平视为基准）"),priorityRows=new JCheckBox("按行重要度保留");
        final JSpinner frameRows;final JLabel verticalLabel=new JLabel();
        final JLabel centreLabel=new JLabel(),windowLabel=new JLabel(),stepLabel=new JLabel();
        final String[] imageKeys=new String[3];int activeSurface=1;boolean syncing;
        VolumeDefinition lastApplied;String invalid;float yaw,pitch;
        Editor(VolumeDefinition source,PixelAtlas atlas,AssetManager assets,Consumer<VolumeDefinition> apply){
            super(new BorderLayout(8,8));this.draft=source.copy();this.atlas=atlas;this.assets=assets;this.apply=apply;
            required=PixelRingTexture.requirement(atlas);frameRows=new JSpinner(new SpinnerNumberModel(PixelVerticalSampling.frameHeight(atlas,draft),1,256,1));
            JPanel rows=new JPanel(new GridLayout(0,1,8,8));
            String initial=source.getStrip3D().layer(Strip3DDefinition.Surface.SIDE).getColorPath();
            boolean same=true;for(var surface:Strip3DDefinition.Surface.values())same&=Objects.equals(initial,source.getStrip3D().layer(surface).getColorPath());shared.setSelected(same);
            for(var surface:Strip3DDefinition.Surface.values()){
                int i=surface.ordinal();var layer=draft.getStrip3D().layer(surface);
                JPanel form=new JPanel(new GridLayout(0,1,6,6));form.setBorder(BorderFactory.createTitledBorder(new String[]{"材质区域：上表面","材质区域：侧面（正面在这里）","材质区域：下表面"}[i]));
                JPanel pathRow=new JPanel(new BorderLayout(6,6));paths[i]=new JTextField(layer.getColorPath()==null?"":layer.getColorPath(),24);
                JButton browse=new JButton("选择 PNG");
                browse.addActionListener(event->{var chooser=new JFileChooser();chooser.setDialogTitle("贴图宽度自由；建议高度 "+required.recommendedHeight()+" px");chooser.setFileFilter(new FileNameExtensionFilter("条带 PNG","png"));if(chooser.showOpenDialog(this)==JFileChooser.APPROVE_OPTION)choose(surface,chooser.getSelectedFile().toString());});
                pathRow.add(paths[i],BorderLayout.CENTER);pathRow.add(browse,BorderLayout.EAST);form.add(pathRow);
                sizes[i]=new JLabel("未贴图：显示剪影");form.add(sizes[i]);
                JPanel offsets=new JPanel(new GridLayout(1,4,6,6));
                xs[i]=new JSpinner(new SpinnerNumberModel(layer.getOffsetX(),-65536,65536,1));ys[i]=new JSpinner(new SpinnerNumberModel(layer.getOffsetY(),-65536,65536,1));
                offsets.add(new JLabel("左右偏移（列）"));offsets.add(xs[i]);offsets.add(new JLabel("上下偏移（行）"));offsets.add(ys[i]);form.add(offsets);rows.add(form);
                images[i]=PixelRingTexture.silhouette();
            }
            for(var surface:Strip3DDefinition.Surface.values()){
                int i=surface.ordinal();paths[i].addActionListener(event->choose(surface,paths[i].getText().trim()));
                xs[i].addChangeListener(event->{if(!syncing)publish();});ys[i].addChangeListener(event->{if(!syncing)publish();});
            }
            shared.addActionListener(event->{if(shared.isSelected()){String path=paths[1].getText().trim();if(!path.isEmpty())choose(Strip3DDefinition.Surface.SIDE,path);}});
            JPanel left=new JPanel(new BorderLayout(8,8));left.add(shared,BorderLayout.NORTH);left.add(rows,BorderLayout.CENTER);
            JButton update=new JButton("应用到部件并更新预览");update.addActionListener(event->publish());left.add(update,BorderLayout.SOUTH);add(left,BorderLayout.WEST);
            JPanel right=new JPanel(new BorderLayout(4,4));JPanel previews=new JPanel(new BorderLayout(4,4));
            JPanel texture=new JPanel(new BorderLayout(4,4));texture.setBorder(BorderFactory.createTitledBorder("原始贴图与选框 · 红十字为中心，黄轮廓为选框"));
            JPanel controls=new JPanel(new GridLayout(0,1,4,4));JPanel modeRow=new JPanel(new FlowLayout(FlowLayout.LEFT,4,0));
            JComboBox<String> surfaceMenu=new JComboBox<>(new String[]{"材质：上表面","材质：侧面","材质：下表面"});surfaceMenu.setSelectedIndex(1);
            surfaceMenu.addActionListener(event->{activeSurface=surfaceMenu.getSelectedIndex();textureCanvas.revalidate();updatePositionLabels();textureCanvas.repaint();});
            JToggleButton setCentre=new JToggleButton("设置中心"),moveWindow=new JToggleButton("拖动选框",true);JToggleButton rowMode=new JToggleButton("行重要度");ButtonGroup modes=new ButtonGroup();modes.add(setCentre);modes.add(moveWindow);modes.add(rowMode);
            setCentre.addActionListener(event->{textureCanvas.editingCentre=true;textureCanvas.editingRows=false;});moveWindow.addActionListener(event->{textureCanvas.editingCentre=false;textureCanvas.editingRows=false;});rowMode.addActionListener(event->{textureCanvas.editingRows=true;textureCanvas.editingCentre=false;});
            JSpinner zoom=new JSpinner(new SpinnerNumberModel(2,1,12,1));zoom.addChangeListener(event->{textureCanvas.zoom=(Integer)zoom.getValue();textureCanvas.revalidate();textureCanvas.repaint();});
            JButton reset=new JButton("重置位置");reset.addActionListener(event->{draft.getBakedPixels().setTextureOrigin(0,0);setWindowOffset(0,0,true);});
            modeRow.add(surfaceMenu);modeRow.add(setCentre);modeRow.add(moveWindow);modeRow.add(rowMode);modeRow.add(new JLabel("放大"));modeRow.add(zoom);modeRow.add(reset);
            controls.add(modeRow);
            JPanel verticalTools=new JPanel(new FlowLayout(FlowLayout.LEFT,4,0));verticalSlide.setSelected(draft.getBakedPixels().isVerticalTextureEnabled());priorityRows.setSelected(draft.getStrip3D().isImportanceEnabled());
            JComboBox<String> brush=new JComboBox<>(new String[]{"1 先消失","2 普通","3 最后消失"});brush.setSelectedIndex(2);brush.addActionListener(event->textureCanvas.importanceBrush=brush.getSelectedIndex()+1);
            verticalSlide.addActionListener(event->{draft.getBakedPixels().setVerticalTextureEnabled(verticalSlide.isSelected());publish();});priorityRows.addActionListener(event->{draft.getStrip3D().setImportanceEnabled(priorityRows.isSelected());publish();});frameRows.addChangeListener(event->{draft.getBakedPixels().setVerticalFrameHeight((Integer)frameRows.getValue());publish();});
            verticalTools.add(verticalSlide);verticalTools.add(new JLabel("平视高度"));verticalTools.add(frameRows);verticalTools.add(priorityRows);verticalTools.add(brush);
            controls.add(verticalTools);controls.add(verticalLabel);controls.add(centreLabel);controls.add(windowLabel);texture.add(controls,BorderLayout.NORTH);
            JScrollPane scroll=new JScrollPane(textureCanvas);scroll.setPreferredSize(new Dimension(620,230));texture.add(scroll,BorderLayout.CENTER);
            previews.add(texture,BorderLayout.NORTH);JPanel rendered=new JPanel(new BorderLayout());rendered.setBorder(BorderFactory.createTitledBorder("剪影效果 · 与实际部件使用同一取图规则"));rendered.add(preview);previews.add(rendered,BorderLayout.CENTER);right.add(previews,BorderLayout.CENTER);
            JPanel angles=new JPanel(new GridLayout(0,1));JSlider yawSlider=new JSlider(-180,180,0),pitchSlider=new JSlider(-90,90,0);
            yawSlider.addChangeListener(event->{yaw=yawSlider.getValue();preview.repaint();textureCanvas.repaint();updatePositionLabels();});pitchSlider.addChangeListener(event->{pitch=pitchSlider.getValue();preview.repaint();textureCanvas.repaint();updatePositionLabels();});
            angles.add(stepLabel);angles.add(yawSlider);JPanel directions=new JPanel(new FlowLayout(FlowLayout.LEFT,4,0));
            int[] presets={90,45,0,-45,-90};String[] names={"正顶端 +90°","上斜 +45°","平视 0°","下斜 −45°","正底端 −90°"};
            JLabel angleLabel=new JLabel("当前观察仰角：0° · 平视；上下角度按整行拼接");
            for(int i=0;i<presets.length;i++){final int value=presets[i];JButton button=new JButton(names[i]);button.addActionListener(event->pitchSlider.setValue(value));directions.add(button);}
            pitchSlider.addChangeListener(event->angleLabel.setText("当前观察仰角："+pitchSlider.getValue()+"° · "+(pitchSlider.getValue()==90?"正顶端":pitchSlider.getValue()==-90?"正底端":pitchSlider.getValue()>0?"上斜视":pitchSlider.getValue()<0?"下斜视":"平视")+"；上下角度按整行拼接"));
            angles.add(directions);angles.add(angleLabel);angles.add(pitchSlider);right.add(angles,BorderLayout.SOUTH);add(right,BorderLayout.CENTER);
            JPanel info=new JPanel(new GridLayout(0,1,4,4));
            info.add(new JLabel(required.description()));
            info.add(new JLabel("最大剪影："+required.silhouetteWidth()+" × "+required.silhouetteHeight()+" px；在原始贴图上点击设置中心、拖动选框；所有位置保存为整数像素。"));
            info.add(new JLabel("选择有效贴图即应用；完成或关闭窗口会保留，取消才恢复原设置。"));add(info,BorderLayout.NORTH);add(status,BorderLayout.SOUTH);
            refresh();lastApplied=source.copy();
        }
        void choose(Strip3DDefinition.Surface surface,String path){
            if(shared.isSelected())for(var field:paths)field.setText(path);else paths[surface.ordinal()].setText(path);
            publish();
        }
        void refresh(){
            invalid=null;java.util.Map<String,BufferedImage> loaded=new java.util.HashMap<>();
            for(var surface:Strip3DDefinition.Surface.values()){
                int i=surface.ordinal();String path=paths[i].getText().trim();
                try{
                    java.io.File file=new java.io.File(path);String key=path+"/"+file.lastModified()+"/"+file.length();
                    if(path.isEmpty()){images[i]=PixelRingTexture.silhouette();imageKeys[i]=null;}
                    else if(!key.equals(imageKeys[i])){images[i]=loaded.containsKey(key)?loaded.get(key):PixelRingTexture.readImage(assets,path);imageKeys[i]=key;}
                    if(!path.isEmpty())loaded.put(key,images[i]);
                    if(!path.isEmpty())PixelRingTexture.validate(images[i],required);
                    sizes[i].setText(path.isEmpty()?"未贴图：显示剪影":"当前："+images[i].getWidth()+" × "+images[i].getHeight()+" px · "+PixelRingTexture.stepDescription(images[i].getWidth()));sizes[i].setForeground(new Color(30,100,60));
                }catch(Exception error){
                    if(images[i]==null)images[i]=PixelRingTexture.silhouette();
                    invalid=error.getMessage();
                    sizes[i].setText("未应用："+error.getMessage());
                    sizes[i].setToolTipText(error.getMessage());sizes[i].setForeground(new Color(170,55,35));
                }
                var layer=draft.getStrip3D().layer(surface);layer.setColorPath(path.isEmpty()?null:path);
                layer.setSampling(((Number)xs[i].getValue()).intValue(),((Number)ys[i].getValue()).intValue(),layer.getTravelX(),layer.getTravelY());
            }
            preview.repaint();textureCanvas.revalidate();textureCanvas.repaint();updatePositionLabels();
            if(invalid!=null){status.setText("当前选择尚未应用：请修正红色提示。部件仍保留上一次有效贴图。");status.setForeground(new Color(170,55,35));}
        }
        void updatePositionLabels(){
            verticalLabel.setText("45° 移 "+Math.round(PixelVerticalSampling.frameHeight(atlas,draft)/2f)+" 行；90° 移 "+PixelVerticalSampling.frameHeight(atlas,draft)+" 行；包含上／平／下的贴图建议高 "+PixelVerticalSampling.frameHeight(atlas,draft)*3+" 行。重要度：1 灰先消失，3 绿最后消失。");
            centreLabel.setText("平视中心：("+textureCanvas.centreX()+", "+textureCanvas.centreY()+") · 点击“设置中心”后在图上点选。");
            stepLabel.setText(imageKeys[activeSurface]==null?"选贴图后按实际宽度移动；360° 回到起点":PixelRingTexture.stepDescription(textureCanvas.image().getWidth()));
            windowLabel.setText("当前选框中心：("+textureCanvas.windowX()+", "+textureCanvas.windowY()+") · 偏移：("+textureCanvas.offsetX()+", "+textureCanvas.offsetY()+")");
        }
        void setTextureRowImportance(int row,int value,boolean applyNow){
            for(int i=0;i<3;i++)if((shared.isSelected()||i==activeSurface)&&row<images[i].getHeight())draft.getBakedPixels().setRowImportance(i,row,images[i].getHeight(),value);
            if(applyNow)publish();else {textureCanvas.repaint();preview.repaint();}
        }
        void setTextureCentre(int x,int y,boolean applyNow){
            var image=textureCanvas.image();x=Math.floorMod(x,image.getWidth());y=Math.floorMod(y,image.getHeight());
            draft.getBakedPixels().setTextureOrigin(x-PixelRingTexture.originX(image.getWidth()),y-PixelRingTexture.originY(image.getHeight()));
            if(applyNow)publish();else refresh();
        }
        void placeTextureWindow(int x,int y,boolean applyNow){
            int phase=PixelRingTexture.phase(textureCanvas.choice().yawStep(),textureCanvas.image().getWidth());
            setWindowOffset(x-textureCanvas.centreX()-phase,y-textureCanvas.centreY()+PixelVerticalSampling.shift(atlas,draft,pitch),applyNow);
        }
        void setWindowOffset(int x,int y,boolean applyNow){
            int dx=x-textureCanvas.offsetX(),dy=y-textureCanvas.offsetY();syncing=true;
            try{for(int i=0;i<3;i++)if(shared.isSelected()||i==activeSurface){
                xs[i].setValue(Math.max(-65536,Math.min(65536,((Number)xs[i].getValue()).intValue()+dx)));
                ys[i].setValue(Math.max(-65536,Math.min(65536,((Number)ys[i].getValue()).intValue()+dy)));
            }}finally{syncing=false;}
            if(applyNow)publish();else refresh();
        }
        VolumeDefinition applyNow(){
            refresh();if(invalid!=null)throw new IllegalArgumentException(invalid);
            draft.getStrip3D().setEnabled(true);lastApplied=draft.copy();
            apply.accept(lastApplied.copy());status.setText("已应用到部件；关闭窗口后保留。保存木偶文件可保留贴图。");status.setForeground(new Color(30,100,60));return lastApplied.copy();
        }
        void publish(){try{applyNow();}catch(IllegalArgumentException ignored){ /* Invalid input stays visible, but cannot replace the last valid part. */ }}
        VolumeDefinition finish(int choice){
            if(choice==JOptionPane.CANCEL_OPTION)return null;
            try{return applyNow();}catch(IllegalArgumentException error){if(choice==JOptionPane.CLOSED_OPTION)return lastApplied.copy();throw error;}
        }
        final class Preview extends JPanel {
            Preview(){setPreferredSize(new Dimension(590,210));setBackground(new Color(40,46,55));}
            protected void paintComponent(Graphics graphics){
                super.paintComponent(graphics);if(atlas==null)return;
                BufferedImage image=PixelAssemblyPreview.render(java.util.List.of(new PixelAssemblyPreview.Part(draft,atlas,new com.jme3.math.Transform(),0,images)),atlas.centre(),atlas.size(),yaw,pitch);
                int left=image.getWidth(),top=image.getHeight(),right=-1,bottom=-1;
                for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++)if((image.getRGB(x,y)>>>24)>=128){left=Math.min(left,x);top=Math.min(top,y);right=Math.max(right,x);bottom=Math.max(bottom,y);}
                if(right<left)return;
                left=Math.max(0,left-3);top=Math.max(0,top-3);right=Math.min(image.getWidth(),right+4);bottom=Math.min(image.getHeight(),bottom+4);
                int width=right-left,height=bottom-top,scale=Math.max(1,Math.min((getWidth()-16)/width,(getHeight()-16)/height));
                int x=(getWidth()-width*scale)/2,y=(getHeight()-height*scale)/2;
                graphics.drawImage(image,x,y,x+width*scale,y+height*scale,left,top,right,bottom,null);
            }
        }
    }
}
