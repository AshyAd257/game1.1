package com.Hecate.puppet.editor;

import com.Hecate.puppet.volume.VolumeDefinition;
import javax.swing.*;
import java.awt.*;

/** The creation form is shared by the integrated mode and its tests. No scene access on the EDT. */
public final class VolumePartDialog {
    private VolumePartDialog() {}
    public static VolumeDefinition show(VolumeDefinition initial) {
        JComboBox<String> shape=new JComboBox<>(new String[]{"圆柱", "锥台", "圆锥", "方块"});
        shape.setSelectedIndex(initial.getShape().ordinal());
        JSpinner w=pixels(initial.getWidthPx()),h=pixels(initial.getHeightPx()),d=pixels(initial.getDepthPx());
        JSpinner ratio=new JSpinner(new SpinnerNumberModel((double)initial.getTopRatio(),0,1,.05));
        JSpinner density=new JSpinner(new SpinnerNumberModel((double)initial.getPixelsPerUnit(),.01,4096,1));
        JPanel panel=new JPanel(new GridLayout(0,2,12,10));
        panel.add(new JLabel("部件类型"));panel.add(shape);
        panel.add(new JLabel("宽度（整数像素）"));panel.add(w);panel.add(new JLabel("高度（整数像素）"));panel.add(h);
        panel.add(new JLabel("深度（整数像素）"));panel.add(d);panel.add(new JLabel("顶部 / 底部比例"));panel.add(ratio);
        panel.add(new JLabel("每世界单位像素数"));panel.add(density);
        ratio.setEnabled(shape.getSelectedIndex()==1);shape.addActionListener(e->ratio.setEnabled(shape.getSelectedIndex()==1));
        while(JOptionPane.showConfirmDialog(null,panel,"新增部件",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)==JOptionPane.OK_OPTION) {
            try {
                for(JSpinner s:new JSpinner[]{w,h,d})if(!((JSpinner.DefaultEditor)s.getEditor()).getTextField().getText().trim().matches("[0-9]+"))throw new IllegalArgumentException("尺寸必须是整数像素");
                for(JSpinner s:new JSpinner[]{w,h,d,ratio,density})s.commitEdit();
                VolumeDefinition result=initial.copy();result.setShape(VolumeDefinition.Shape.values()[shape.getSelectedIndex()]);
                result.setDimensions(((Number)w.getValue()).intValue(),((Number)h.getValue()).intValue(),((Number)d.getValue()).intValue());
                result.setTopRatio(((Number)ratio.getValue()).floatValue());result.setPixelsPerUnit(((Number)density.getValue()).floatValue());return result;
            } catch(Exception e){JOptionPane.showMessageDialog(null,"请检查尺寸："+e.getMessage());}
        }
        return null;
    }
    private static JSpinner pixels(int value){JSpinner s=new JSpinner(new SpinnerNumberModel(value,1,4096,1));s.setEditor(new JSpinner.NumberEditor(s,"0"));return s;}
}
