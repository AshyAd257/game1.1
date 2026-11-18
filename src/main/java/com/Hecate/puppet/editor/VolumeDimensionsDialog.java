package com.Hecate.puppet.editor;
import com.Hecate.puppet.volume.VolumeDefinition;
import javax.swing.*;
import java.awt.*;

final class VolumeDimensionsDialog {
    private VolumeDimensionsDialog(){}
    static int[] show(VolumeDefinition source){
        JPanel form=new JPanel(new GridLayout(0,2,12,12));int[] initial={source.getWidthPx(),source.getHeightPx(),source.getDepthPx()};String[] names={"width","height","depth"},labels={"宽度 X（像素）","高度 Y（像素）","长度／深度 Z（像素）"};JSpinner[] fields=new JSpinner[3];
        for(int i=0;i<3;i++){fields[i]=new JSpinner(new SpinnerNumberModel(initial[i],1,4096,1));fields[i].setName(names[i]);fields[i].setEditor(new JSpinner.NumberEditor(fields[i],"0"));form.add(new JLabel(labels[i]));form.add(fields[i]);}
        JPanel root=new JPanel(new BorderLayout(10,14));root.add(form,BorderLayout.CENTER);root.add(new JLabel("<html>输入 1–4096 的整数；确定后立即应用，可撤销。<br>修改部件尺寸不会重采样源贴图。</html>"),BorderLayout.SOUTH);root.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        while(EditorDialogs.confirm(root,"修改部件长宽高")==JOptionPane.OK_OPTION){try{int[] result=new int[3];for(int i=0;i<3;i++){String text=((JSpinner.DefaultEditor)fields[i].getEditor()).getTextField().getText().trim();if(!text.matches("[0-9]+"))throw new IllegalArgumentException("长宽高必须是整数");int value=Integer.parseInt(text);if(value<1||value>4096)throw new IllegalArgumentException("长宽高必须在 1–4096 之间");fields[i].commitEdit();result[i]=value;}return result;}catch(Exception ex){EditorDialogs.message("请检查尺寸："+ex.getMessage());}}
        return null;
    }
}
