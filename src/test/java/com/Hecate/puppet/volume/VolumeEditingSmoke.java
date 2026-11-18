package com.Hecate.puppet.volume;
import com.Hecate.puppet.editor.*;
import com.jme3.renderer.RenderManager;
import com.jme3.system.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.concurrent.*;
import javax.swing.*;
import javax.imageio.ImageIO;

/** Full-editor interaction checks for foreground size fields and click-on-image row priorities. */
public final class VolumeEditingSmoke extends PuppetEditorApp {
    private final CountDownLatch done=new CountDownLatch(1);private volatile Throwable failure;private int frame,stage,closedFrame,seenClosed=-1;private volatile int closed=-1;private VolumeDefinition before;private String coverName;
    public static void main(String[] args){try{
        var app=new VolumeEditingSmoke();var mode=PuppetEditorApp.class.getDeclaredField("volumeMode");mode.setAccessible(true);mode.setBoolean(app,true);var settings=new AppSettings(true);settings.setResolution(1280,720);settings.setAudioRenderer(null);settings.setUseInput(true);app.setSettings(settings);app.setShowSettings(false);app.start(JmeContext.Type.OffscreenSurface);
        boolean ok=app.done.await(40,TimeUnit.SECONDS);app.stop(false);if(!ok||app.failure!=null){if(app.failure!=null)app.failure.printStackTrace();else System.err.println("Editing smoke timed out at stage "+app.stage);System.exit(1);}System.out.println("Size/importance editing passed: actual buttons, foreground forms, dimensions, image-row clicks on all 3 layers, cancel, undo/redo, JSON persistence, selection shape apply/cancel/undo/reset, outline toggle/undo/redo, sample width bounds/apply/cancel/undo/redo at 1280x720");System.exit(0);}catch(Exception e){throw new RuntimeException(e);}
    }
    @Override public void simpleRender(RenderManager manager){super.simpleRender(manager);if(done.getCount()==0)return;try{
        if(++frame==8){
            clickXrayTool();if(!isVolumeModelDisplay()||!isVolumeXrayEnabled())throw new AssertionError("Xray toolbar on");
            toggleVolumeDisplay();if(isVolumeModelDisplay()||!isVolumeXrayEnabled())throw new AssertionError("Xray preference lost");
            clickXrayTool();if(!isVolumeModelDisplay()||!isVolumeXrayEnabled())throw new AssertionError("Xray toolbar from artwork");
            clickXrayTool();if(isVolumeXrayEnabled())throw new AssertionError("Xray toolbar off");
            clickControl("实体");clickControl("轮廓：");
            if(getVolumeSelectedBone().getVolumeDefinition().isSurfaceContourSnapped())throw new AssertionError("Contour toggle");
            getCommandManager().undo();if(!getVolumeSelectedBone().getVolumeDefinition().isSurfaceContourSnapped())throw new AssertionError("Contour undo");
            getCommandManager().redo();if(getVolumeSelectedBone().getVolumeDefinition().isSurfaceContourSnapped())throw new AssertionError("Contour redo");
            getCommandManager().undo();clickControl("遮挡：");
            if(getVolumeSelectedBone().getVolumeDefinition().isSurfaceDepthEnabled())throw new AssertionError("Surface toggle");
            getCommandManager().undo();if(!getVolumeSelectedBone().getVolumeDefinition().isSurfaceDepthEnabled())throw new AssertionError("Surface undo");
            getCommandManager().redo();if(getVolumeSelectedBone().getVolumeDefinition().isSurfaceDepthEnabled())throw new AssertionError("Surface redo");
            before=getVolumeSelectedBone().getVolumeDefinition().copy();var d=before.copy();for(var face:Strip3DDefinition.Surface.values())d.getStrip3D().layer(face).setColorPath("textures/TriStripPrototype/"+face.name().toLowerCase(java.util.Locale.ROOT)+".png");getVolumeSelectedBone().setVolumeDefinition(d);clickControl("图像");clickControl("像素描边：");if(getVolumeSelectedBone().getVolumeDefinition().getStrip3D().isOutlineEnabled())throw new AssertionError("Outline toggle");getCommandManager().undo();if(!getVolumeSelectedBone().getVolumeDefinition().getStrip3D().isOutlineEnabled())throw new AssertionError("Outline undo");getCommandManager().redo();if(getVolumeSelectedBone().getVolumeDefinition().getStrip3D().isOutlineEnabled())throw new AssertionError("Outline redo");getCommandManager().undo();start(0);}
        if(frame<=8)return;if(closed!=seenClosed){seenClosed=closed;closedFrame=frame;}if(closed==stage&&frame-closedFrame<2)return;var d=getVolumeSelectedBone().getVolumeDefinition();
        if(stage==0&&d.getWidthPx()==48&&d.getHeightPx()==72&&d.getDepthPx()==40){if(d.getStrip3D().getFrameWidth()!=24||d.getStrip3D().getFrameHeight()!=24)throw new AssertionError("Resize changed source sampling dimensions");getCommandManager().undo();if(!getVolumeSelectedBone().getVolumeDefinition().sameGeometry(before))throw new AssertionError("Size undo");getCommandManager().redo();start(1);}
        else if(stage==1&&closed==1){if(d.getWidthPx()!=48||d.getHeightPx()!=72||d.getDepthPx()!=40)throw new AssertionError("Cancel changed size");start(2);}
        else if(stage==2){int[] a=d.getStrip3D().getImportance();if(a[5]==1&&a[24]==2&&a[53]==3){getCommandManager().undo();if(getVolumeSelectedBone().getVolumeDefinition().getStrip3D().getImportance()[5]!=2)throw new AssertionError("Priority undo");getCommandManager().redo();var gson=new com.google.gson.Gson();var restored=gson.fromJson(gson.toJson(getVolumeSelectedBone().getVolumeDefinition()),VolumeDefinition.class);if(restored.getStrip3D().getImportance()[53]!=3)throw new AssertionError("Save priority");start(3);}}
        else if(stage==3&&closed==3){int[] a=d.getStrip3D().getImportance();if(a[5]!=1||a[24]!=2||a[53]!=3)throw new AssertionError("Cancel changed priorities");start(4);}
        else if(stage==4&&closed==4){if(!d.getStrip3D().hasSelectionMask()||d.getStrip3D().getSelectionMask()[0])throw new AssertionError("Shape apply");getCommandManager().undo();if(getVolumeSelectedBone().getVolumeDefinition().getStrip3D().hasSelectionMask())throw new AssertionError("Shape undo");getCommandManager().redo();start(5);}
        else if(stage==5&&closed==5){if(!d.getStrip3D().hasSelectionMask()||d.getStrip3D().getSelectionMask()[0])throw new AssertionError("Shape cancel");start(6);}
        else if(stage==6&&closed==6){if(d.getStrip3D().hasSelectionMask())throw new AssertionError("Shape reset");start(7);}
        else if(stage==7&&closed==7){if(d.getStrip3D().getSampleWidth()!=96)throw new AssertionError("Width apply maximum");getCommandManager().undo();if(getVolumeSelectedBone().getVolumeDefinition().getStrip3D().getSampleWidth()!=24)throw new AssertionError("Width undo");getCommandManager().redo();start(8);}
        else if(stage==8&&closed==8){if(d.getStrip3D().getSampleWidth()!=96)throw new AssertionError("Width cancel");start(9);}
        else if(stage==9&&closed==9){if(d.getStrip3D().getSampleWidth()!=1)throw new AssertionError("Width apply minimum");start(10);}
        else if(stage==10&&closed==10){if(d.getStrip3D().getSampleWidth()!=9)throw new AssertionError("Typed width 9");start(11);}
        else if(stage==11&&closed==11){if(d.getStrip3D().getSampleWidth()!=13)throw new AssertionError("Shape editor typed width 13");getCommandManager().undo();if(getVolumeSelectedBone().getVolumeDefinition().getStrip3D().getSampleWidth()!=9)throw new AssertionError("Shape width undo");getCommandManager().redo();start(12);}
        else if(stage==12&&closed==12){if(d.getStrip3D().getSampleWidth()!=13)throw new AssertionError("Shape width cancel");start(13);}
        else if(stage==13&&closed==13){if(d.getStrip3D().getSampleWidth()!=16||d.getWidthPx()!=48||d.getHeightPx()!=72||d.getDepthPx()!=40)throw new AssertionError("Width 16 must preserve body dimensions");start(14);}
        else if(stage==14&&closed==14){if(!coverName.equals(d.getCoverTarget()))throw new AssertionError("Cover apply");getCommandManager().undo();if(getVolumeSelectedBone().getVolumeDefinition().getCoverTarget()!=null)throw new AssertionError("Cover undo");getCommandManager().redo();start(15);}
        else if(stage==15&&closed==15){if(!coverName.equals(d.getCoverTarget()))throw new AssertionError("Cover cancel");start(16);}
        else if(stage==16&&closed==16){if(d.getCoverTarget()!=null)throw new AssertionError("Cover clear");done.countDown();}
    }catch(Throwable e){failure=e;done.countDown();}}
    private void start(int n)throws Exception{
        stage=n;SwingUtilities.invokeLater(()->{final int[] tries={0};javax.swing.Timer timer=new javax.swing.Timer(120,null);timer.addActionListener(e->{try{
            for(Window window:Window.getWindows())if(window.isShowing()){JOptionPane pane=find(window,JOptionPane.class,null);if(pane==null)continue;if(!window.isAlwaysOnTop())throw new AssertionError("Form can hide behind editor");
                if(n>=14){JComboBox<?> choice=find(window,JComboBox.class,"coverTarget");if(choice==null)continue;
                    if(n==14){if(choice.getItemCount()<2)throw new AssertionError("Missing cover candidates");coverName=choice.getItemAt(1).toString();choice.setSelectedIndex(1);savePanel(window,"target/volume-cover-form.png");}
                    else choice.setSelectedIndex(0);pane.setValue(n==15?JOptionPane.CANCEL_OPTION:JOptionPane.OK_OPTION);
                }
                else if(n<2){JSpinner width=find(window,JSpinner.class,"width"),height=find(window,JSpinner.class,"height"),depth=find(window,JSpinner.class,"depth");if(width==null)continue;width.setValue(n==0?48:100);height.setValue(72);depth.setValue(40);if(n==0)savePanel(window,"target/volume-size-form.png");pane.setValue(n==0?JOptionPane.OK_OPTION:JOptionPane.CANCEL_OPTION);}
                else if(n<4){JComboBox<?> layers=find(window,JComboBox.class,"importanceLayer");JPanel rows=find(window,JPanel.class,"importanceRows");if(layers==null||rows==null)continue;
                    if(n==2){layers.setSelectedIndex(0);clickRow(rows,5);clickRow(rows,5);layers.setSelectedIndex(1);clickRow(rows,0);layers.setSelectedIndex(2);clickRow(rows,5);layers.setSelectedIndex(1);savePanel(window,"target/row-importance-editor.png");}else{layers.setSelectedIndex(0);clickRow(rows,5);}pane.setValue(n==2?JOptionPane.OK_OPTION:JOptionPane.CANCEL_OPTION);
                }
                else if(n>=7){JSpinner width=find(window,JSpinner.class,"sampleWidth");if(width==null)continue;
                    var model=(SpinnerNumberModel)width.getModel();if(!Integer.valueOf(96).equals(model.getMaximum())||!Integer.valueOf(1).equals(model.getMinimum()))throw new AssertionError("Width bounds");
                    var input=((JSpinner.DefaultEditor)width.getEditor()).getTextField();
                    for(String invalid:new String[]{"0","97","12.5","NaN"}){try{input.getFormatter().stringToValue(invalid);throw new AssertionError("Invalid width accepted: "+invalid);}catch(java.text.ParseException expected){}}
                    if(n<=9)width.setValue(n==7?96:1);else input.setText(n==10?"9":n==11?"13":"16");
                    if(n==11){input.postActionEvent();if(!Integer.valueOf(13).equals(width.getValue()))throw new AssertionError("Enter must preview typed width");savePanel(window,"target/selection-width-shape-form.png");}
                    if(n==7)savePanel(window,"target/selection-width-form.png");pane.setValue(n==8||n==12?JOptionPane.CANCEL_OPTION:JOptionPane.OK_OPTION);
                }
                else{JPanel grid=find(window,JPanel.class,"selectionGrid");if(grid==null)continue;
                    if(n==4){grid.dispatchEvent(new MouseEvent(grid,MouseEvent.MOUSE_PRESSED,System.currentTimeMillis(),0,14,14,1,false,MouseEvent.BUTTON3));grid.dispatchEvent(new MouseEvent(grid,MouseEvent.MOUSE_RELEASED,System.currentTimeMillis(),0,14,14,1,false,MouseEvent.BUTTON3));savePanel(window,"target/selection-shape-editor.png");}
                    else clickSwingButton(window,"恢复默认方框");pane.setValue(n==5?JOptionPane.CANCEL_OPTION:JOptionPane.OK_OPTION);
                }
                timer.stop();SwingUtilities.invokeLater(()->closed=n);return;
            }
            if(++tries[0]>160)throw new AssertionError("Editing dialog did not open at stage "+n);
        }catch(Throwable ex){failure=ex;timer.stop();done.countDown();}});timer.start();});
        clickControl(n<2||n>=14?"实体":"图像");clickControl(n>=14?"包覆目标：":n==0?"宽度：":n==1?"高度：":n<4?"编辑行重要度":n>=7&&n<=10?"选框宽度：":"绘制选框形状");
    }
    private static boolean clickSwingButton(Component c,String text){if(c instanceof JButton b&&text.equals(b.getText())){b.doClick();return true;}if(c instanceof Container container)for(Component child:container.getComponents())if(clickSwingButton(child,text))return true;return false;}
    private static void clickRow(JPanel rows,int row){rows.dispatchEvent(new MouseEvent(rows,MouseEvent.MOUSE_PRESSED,System.currentTimeMillis(),0,64+20,24+row*8+4,1,false,MouseEvent.BUTTON1));}
    private void clickXrayTool()throws Exception{
        var field=PuppetEditorApp.class.getDeclaredField("editorUI");field.setAccessible(true);var ui=field.get(this);
        var list=ui.getClass().getDeclaredField("volumeToolButtons");list.setAccessible(true);
        var b=(com.Hecate.puppet.editor.Button)((java.util.List<?>)list.get(ui)).get(7);
        var x=b.getClass().getDeclaredField("x");var y=b.getClass().getDeclaredField("y");x.setAccessible(true);y.setAccessible(true);
        int bx=x.getInt(b),by=y.getInt(b);if(bx<0||bx+76>getCamera().getWidth()||by<0)throw new AssertionError("Xray button clipped");
        b.handleMouseClick(bx+8,by+8);
    }
    private void clickControl(String prefix)throws Exception{
        var uiField=PuppetEditorApp.class.getDeclaredField("editorUI");uiField.setAccessible(true);Object ui=uiField.get(this);var panelField=ui.getClass().getDeclaredField("volumeInspector");panelField.setAccessible(true);Object panel=panelField.get(ui);var buttonsField=panel.getClass().getDeclaredField("buttons");buttonsField.setAccessible(true);
        for(Object object:(java.util.List<?>)buttonsField.get(panel)){var b=(com.Hecate.puppet.editor.Button)object;if(b.getText().startsWith(prefix)){var x=b.getClass().getDeclaredField("x");var y=b.getClass().getDeclaredField("y");x.setAccessible(true);y.setAccessible(true);if(y.getInt(b)<0)throw new AssertionError("Control outside window: "+prefix);b.handleMouseClick(x.getInt(b)+8,y.getInt(b)+8);return;}}throw new AssertionError("Missing control "+prefix);
    }
    private static <T extends Component> T find(Component c,Class<T> type,String name){if(type.isInstance(c)&&(name==null||name.equals(c.getName())))return type.cast(c);if(c instanceof Container)for(Component child:((Container)c).getComponents()){T found=find(child,type,name);if(found!=null)return found;}return null;}
    private static void savePanel(Window window,String path)throws Exception{var image=new BufferedImage(window.getWidth(),window.getHeight(),BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();window.paint(g);g.dispose();ImageIO.write(image,"png",Path.of(path).toFile());}
    @Override public void handleError(String message,Throwable e){failure=e;done.countDown();}
}
