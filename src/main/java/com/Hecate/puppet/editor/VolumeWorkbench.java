package com.Hecate.puppet.editor;

import com.Hecate.puppet.volume.VolumeDefinition;
import com.Hecate.puppet.volume.VolumeReferenceView.Mode;
import java.awt.*;
import java.awt.event.*;
import java.text.ParseException;
import java.util.Arrays;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

/** Modeless workbench. The controller queues all scene access onto the render thread. */
public final class VolumeWorkbench {
    public interface Controller {
        void requestSnapshot(Consumer<Snapshot> receiver);
        void select(String name);
        void create(VolumeDefinition definition);
        void apply(Object token, VolumeDefinition definition);
        void mode(Mode mode);
        void action(String action, Object token);
    }
    public static final class Snapshot {
        public final String[] names;
        public final String selected, error;
        public final Object token, revision;
        public final VolumeDefinition definition;
        public final Mode mode;
        public final boolean editable;
        public Snapshot(String[] names, String selected, Object token, Object revision,
                        VolumeDefinition definition, Mode mode, boolean editable, String error) {
            this.names=names; this.selected=selected; this.token=token; this.revision=revision;
            this.definition=definition; this.mode=mode; this.editable=editable; this.error=error;
        }
    }
    private final Controller controller;
    private final JFrame frame = new JFrame("木偶 · 体积工作台");
    private final JComboBox<String> parts = new JComboBox<>();
    private final JComboBox<String> shapes = new JComboBox<>(new String[]{"圆柱", "锥台", "圆锥", "方块"});
    private final JComboBox<String> views = new JComboBox<>(new String[]{"外观", "实体", "叠加"});
    private final JSpinner width=integer(32), height=integer(64), depth=integer(32);
    private final JSpinner ratio=decimal(.5,0,1,.05), density=decimal(32, .01,4096,1);
    private final JSpinner yaw=decimal(-35,-180,180,5), elevation=decimal(45,-90,90,5), strength=decimal(.8,0,2,.1);
    private final JTextField normal=new JTextField();
    private final JCheckBox lighting=new JCheckBox("启用简易光照（关闭时保留原画颜色）");
    private final JLabel status=new JLabel("正在连接编辑器…");
    private final JButton apply=new JButton("应用到所选部件"), detach=new JButton("移除实体设置");
    private final Timer timer;
    private Snapshot current;
    private boolean updating, pending;

    public VolumeWorkbench(Controller controller) {
        this.controller=controller;
        frame.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        JPanel root=new JPanel(new BorderLayout(10,10)); root.setBorder(BorderFactory.createEmptyBorder(14,14,14,14));
        JPanel top=new JPanel(new GridLayout(0,1,4,4));
        top.add(new JLabel("选择部件后设置空间体积；骨骼和动画沿用主编辑器。"));
        top.add(row("当前部件",parts)); top.add(row("预览方式",views));
        JPanel history=new JPanel(new FlowLayout(FlowLayout.LEFT,4,0));
        for(String[] entry:new String[][]{{"撤销","undo"},{"重做","redo"},{"播放 / 暂停","play"},{"记录关键帧","key"},{"保存 / 导出","save"}})
            history.add(button(entry[0],()->controller.action(entry[1],token())));
        top.add(history); root.add(top,BorderLayout.NORTH);
        JTabbedPane tabs=new JTabbedPane();
        JPanel body=form();
        body.add(row("基本形状",shapes)); body.add(row("宽度（像素）",width));
        body.add(row("高度（像素）",height)); body.add(row("深度（像素）",depth));
        body.add(row("顶部 / 底部比例",ratio)); body.add(row("每世界单位像素数",density));
        body.add(new JLabel("尺寸控制参考实体，不会自动拉伸已有贴图。"));
        body.add(new JLabel("实体继承骨骼的位移、旋转和缩放。"));
        body.add(button("新建此形状的部件",()->withDefinition(controller::create)));
        tabs.addTab("实体",body);
        JPanel art=form();
        art.add(button("导入普通颜色图",()->controller.action("color",token())));
        art.add(button("导入环绕条带颜色图",()->controller.action("strip",token())));
        art.add(button("打开整数像素选框",()->controller.action("crop",token())));
        art.add(new JLabel("沿用现有条带模式：每 10° 移动 1 像素，一圈 36 列。"));
        art.add(new JLabel("颜色图和法线图使用同一组选框坐标。"));
        art.add(new JLabel("上 / 中 / 下三层联动取样尚未接入。"));
        tabs.addTab("外观",art);
        JPanel light=form();
        light.add(new JLabel("法线图使用部件局部坐标：RGB 对应 XYZ。"));
        light.add(new JLabel("中性正面为 RGB(128,128,255)，尺寸与颜色图一致。"));
        light.add(row("法线图片",normal));
        JPanel files=new JPanel(new FlowLayout(FlowLayout.LEFT));
        files.add(button("选择 PNG…",this::chooseNormal)); files.add(button("清除",()->normal.setText("")));
        files.add(button("刷新已应用的图片",()->controller.action("refresh",token()))); light.add(files);
        light.add(lighting); light.add(row("灯光水平角",yaw)); light.add(row("灯光仰角",elevation));
        light.add(row("灯光强度",strength));
        light.add(new JLabel("仅模拟一束方向光，无阴影、无灯光场景编辑。"));
        tabs.addTab("法线与光照",light); root.add(tabs,BorderLayout.CENTER);
        JPanel bottom=new JPanel(new BorderLayout(4,8));
        JPanel actions=new JPanel(new FlowLayout(FlowLayout.LEFT)); actions.add(apply); actions.add(detach);
        bottom.add(actions,BorderLayout.NORTH); bottom.add(status,BorderLayout.SOUTH); root.add(bottom,BorderLayout.SOUTH);
        frame.setContentPane(root); frame.setSize(650,590); frame.setMinimumSize(new Dimension(620,550)); frame.setLocationByPlatform(true);
        parts.addActionListener(e->{if(!updating && parts.getSelectedItem()!=null)controller.select((String)parts.getSelectedItem());});
        views.addActionListener(e->{if(!updating)controller.mode(Mode.values()[views.getSelectedIndex()]);});
        shapes.addActionListener(e->ratio.setEnabled(shapes.getSelectedIndex()==1));
        apply.addActionListener(e->withDefinition(d->controller.apply(token(),d)));
        detach.addActionListener(e->controller.apply(token(),null));
        timer=new Timer(300,e->refresh());
        frame.addWindowListener(new WindowAdapter(){public void windowClosing(WindowEvent e){timer.stop();}});
    }
    public void show() { frame.setVisible(true); frame.toFront(); timer.start(); refresh(); }
    public void close() { timer.stop(); frame.dispose(); }
    private Object token(){return current==null?null:current.token;}
    private void refresh() {
        if(pending || !frame.isVisible())return; pending=true;
        controller.requestSnapshot(s->SwingUtilities.invokeLater(()->{pending=false;if(frame.isVisible())accept(s);}));
    }
    private void accept(Snapshot s) {
        boolean changed=current==null || current.token!=s.token || current.revision!=s.revision;
        updating=true;
        if(current==null || !Arrays.equals(current.names,s.names))parts.setModel(new DefaultComboBoxModel<>(s.names));
        parts.setSelectedItem(s.selected); views.setSelectedIndex(s.mode.ordinal());
        current=s;
        if(changed)fill(s.definition==null?new VolumeDefinition():s.definition);
        apply.setEnabled(s.editable); detach.setEnabled(s.editable && s.definition!=null);
        status.setText(s.error!=null?s.error:!s.editable?"请选择一个贴图部件；导入的网格不支持参考实体。":s.definition==null?"此部件尚无实体设置。设置参数后点击应用。":"修改参数后点击应用；支持撤销和保存。");
        status.setToolTipText(status.getText()); updating=false;
    }
    private void fill(VolumeDefinition d) {
        shapes.setSelectedIndex(d.getShape().ordinal()); width.setValue(d.getWidthPx());height.setValue(d.getHeightPx());depth.setValue(d.getDepthPx());
        ratio.setValue((double)d.getTopRatio());density.setValue((double)d.getPixelsPerUnit()); normal.setText(d.getNormalTexturePath()==null?"":d.getNormalTexturePath());
        lighting.setSelected(d.isLightingEnabled());yaw.setValue((double)d.getLightYaw());elevation.setValue((double)d.getLightElevation());strength.setValue((double)d.getLightStrength());
        ratio.setEnabled(d.getShape()==VolumeDefinition.Shape.FRUSTUM);
    }
    private void withDefinition(Consumer<VolumeDefinition> operation) {
        try {
            for(JSpinner s:new JSpinner[]{width,height,depth}) {
                String value=((JSpinner.DefaultEditor)s.getEditor()).getTextField().getText().trim();
                if(!value.matches("[0-9]+"))throw new IllegalArgumentException("实体尺寸必须为整数像素");
            }
            for(JSpinner s:new JSpinner[]{width,height,depth,ratio,density,yaw,elevation,strength})s.commitEdit();
            VolumeDefinition d=new VolumeDefinition(); d.setShape(VolumeDefinition.Shape.values()[shapes.getSelectedIndex()]);
            d.setDimensions(number(width).intValue(),number(height).intValue(),number(depth).intValue());
            d.setTopRatio(number(ratio).floatValue());d.setPixelsPerUnit(number(density).floatValue());d.setNormalTexturePath(normal.getText().trim());
            d.setLightingEnabled(lighting.isSelected());d.setLight(number(yaw).floatValue(),number(elevation).floatValue(),number(strength).floatValue());d.validate();operation.accept(d);
        } catch(IllegalArgumentException|ParseException e){JOptionPane.showMessageDialog(frame,"请检查参数："+e.getMessage(),"未应用",JOptionPane.WARNING_MESSAGE);}
    }
    private void chooseNormal(){JFileChooser chooser=new JFileChooser();chooser.setFileFilter(new FileNameExtensionFilter("PNG 法线图片","png"));if(chooser.showOpenDialog(frame)==JFileChooser.APPROVE_OPTION)normal.setText(chooser.getSelectedFile().getAbsolutePath());}
    private static Number number(JSpinner s){return (Number)s.getValue();}
    private static JSpinner integer(int value){JSpinner s=new JSpinner(new SpinnerNumberModel(value,1,4096,1));s.setEditor(new JSpinner.NumberEditor(s,"0"));return s;}
    private static JSpinner decimal(double value,double min,double max,double step){return new JSpinner(new SpinnerNumberModel(value,min,max,step));}
    private static JPanel form(){JPanel p=new JPanel();p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));p.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));return p;}
    private static JPanel row(String label,JComponent field){JPanel p=new JPanel(new BorderLayout(8,4));p.add(new JLabel(label),BorderLayout.WEST);p.add(field,BorderLayout.CENTER);p.setMaximumSize(new Dimension(Integer.MAX_VALUE,34));return p;}
    private static JButton button(String text,Runnable action){JButton b=new JButton(text);b.addActionListener(e->action.run());return b;}
}
