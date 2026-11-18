package com.Hecate.puppet.editor;

import com.Hecate.puppet.volume.*;
import com.jme3.asset.AssetManager;
import java.awt.*;
import javax.swing.*;

/** Changes the source window, independently of the silhouette drawing grid. */
public final class SelectionWidthDialog {
    private SelectionWidthDialog(){}
    public static Integer show(Strip3DDefinition source,AssetManager assets){
        boolean loaded=false;
        for(var face:Strip3DDefinition.Surface.values()){
            String path=source.layer(face).getColorPath();if(path==null)continue;
            try{var image=Strip3DTextures.readImage(assets,path);
                if(image.getWidth()==source.getFrameWidth()*4&&image.getHeight()==source.getFrameHeight()){loaded=true;break;}
            }catch(Exception ignored){ /* Only a successfully imported texture establishes the limit. */ }
        }
        if(!loaded){EditorDialogs.message("请先成功导入至少一层颜色贴图，再设置选框宽度。");return null;}
        int max=source.getFrameWidth()*4;
        JSpinner width=createSpinner(source);
        JPanel panel=new JPanel(new GridLayout(0,1,6,8));JPanel row=new JPanel(new FlowLayout(FlowLayout.LEFT));
        row.add(new JLabel("一次显示"));row.add(width);row.add(new JLabel("列像素（1–"+max+"）"));panel.add(row);
        panel.add(new JLabel("上限为整张贴图的宽度；三层共用，取样始终对齐整数像素。"));
        panel.add(new JLabel("横向滑动时，填 13 就在部件原有宽度内显示 13 列像素。"));
        panel.add(new JLabel("透视挑列以此为单面列数，转动时显示列数随角度变化。"));
        while(EditorDialogs.confirm(panel,"选框宽度 · 一次显示的像素列数")==JOptionPane.OK_OPTION){
            try{return readWidth(width,max);}catch(IllegalArgumentException e){EditorDialogs.message(e.getMessage());}
        }
        return null;
    }
    static JSpinner createSpinner(Strip3DDefinition source){
        int max=source.getFrameWidth()*4;
        JSpinner width=new JSpinner(new SpinnerNumberModel(source.getSampleWidth(),1,max,1));width.setName("sampleWidth");
        width.setEditor(new JSpinner.NumberEditor(width,"0"));
        var input=((JSpinner.DefaultEditor)width.getEditor()).getTextField();
        var formatter=new javax.swing.text.DefaultFormatter(){
            @Override public Object stringToValue(String text)throws java.text.ParseException{
                try{int value=Integer.parseInt(text.trim());if(value<1||value>max)throw new NumberFormatException();return value;}
                catch(NumberFormatException e){throw new java.text.ParseException("请输入范围内的整数像素数",0);}
            }
        };
        // Keep the spinner model (and therefore the preview) current while typing.
        // Empty, fractional, and out-of-range intermediate text must not replace the last valid value.
        formatter.setCommitsOnValidEdit(true);
        formatter.setOverwriteMode(false);
        input.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(formatter));
        input.setFocusLostBehavior(JFormattedTextField.PERSIST);
        input.addActionListener(e->{try{width.commitEdit();}catch(java.text.ParseException ex){java.awt.Toolkit.getDefaultToolkit().beep();}});
        return width;
    }
    static int readWidth(JSpinner width,int max){
        try{
            String text=((JSpinner.DefaultEditor)width.getEditor()).getTextField().getText().trim();
            int value=Integer.parseInt(text);if(value<1||value>max)throw new NumberFormatException();return value;
        }catch(NumberFormatException e){throw new IllegalArgumentException("请输入 1–"+max+" 之间的整数像素数。");}
    }
}
