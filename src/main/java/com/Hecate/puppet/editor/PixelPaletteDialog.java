package com.Hecate.puppet.editor;

import com.Hecate.puppet.pixel.BakedPixelDefinition;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

final class PixelPaletteDialog {
    private PixelPaletteDialog(){}
    static List<BakedPixelDefinition.PaletteEntry> show(BakedPixelDefinition d){
        var existing=d.getPalette();JPanel rows=new JPanel(new GridLayout(0,4,8,8));
        JCheckBox[] enabled=new JCheckBox[16];JTextField[] names=new JTextField[16];JButton[] originals=new JButton[16],targets=new JButton[16];
        rows.add(new JLabel("启用"));rows.add(new JLabel("区域名称"));rows.add(new JLabel("条带中的原颜色"));rows.add(new JLabel("替换后的颜色"));
        for(int i=0;i<16;i++){var e=i<existing.size()?existing.get(i):null;enabled[i]=new JCheckBox("使用",e!=null);names[i]=new JTextField(e!=null?e.name():i==0?"皮肤":i==1?"头发":i==2?"衣服":"颜色 "+(i+1));originals[i]=colorButton("原颜色",e==null?0xffffff:e.source());targets[i]=colorButton("新颜色",e==null?0xffffff:e.target());rows.add(enabled[i]);rows.add(names[i]);rows.add(originals[i]);rows.add(targets[i]);}
        JPanel panel=new JPanel(new BorderLayout(8,8));panel.add(new JLabel("条带使用相同原颜色的整格一起换色；剪影和深度不会重新生成。"),BorderLayout.NORTH);JScrollPane scroll=new JScrollPane(rows);scroll.setPreferredSize(new Dimension(650,450));panel.add(scroll,BorderLayout.CENTER);
        while(EditorDialogs.confirm(panel,"皮肤／头发／衣服调色板")==JOptionPane.OK_OPTION){List<BakedPixelDefinition.PaletteEntry> result=new ArrayList<>();for(int i=0;i<16;i++)if(enabled[i].isSelected())result.add(new BakedPixelDefinition.PaletteEntry(names[i].getText(),originals[i].getBackground().getRGB(),targets[i].getBackground().getRGB()));try{var validate=d.copy();validate.setPalette(result);return result;}catch(IllegalArgumentException e){EditorDialogs.message(e.getMessage());}}return null;
    }
    private static JButton colorButton(String label,int rgb){JButton button=new JButton(label);button.setOpaque(true);button.setBackground(new Color(rgb));button.addActionListener(e->{Color selected=JColorChooser.showDialog(null,label,button.getBackground());if(selected!=null)button.setBackground(selected);});return button;}
}
