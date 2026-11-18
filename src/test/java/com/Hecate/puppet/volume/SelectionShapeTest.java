package com.Hecate.puppet.volume;
import com.google.gson.Gson;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SelectionShapeTest {
    @Test void asymmetricLegSelectionFollowsTurnWithoutChangingSourcePixels(){
        var d=new Strip3DDefinition();d.setFrameSize(6,13);d.setSampleWidth(5);d.setOutlineEnabled(false);
        boolean[] mask=new boolean[6*13];for(int y=0;y<13;y++)for(int x=2;x<(y==0?4:y<6?5:6);x++)mask[y*6+x]=true;d.setSelectionMask(6,13,mask);
        var image=new BufferedImage(24,13,BufferedImage.TYPE_INT_ARGB);for(int y=0;y<13;y++)for(int x=0;x<24;x++)image.setRGB(x,y,0xff000000|x<<16|y<<8|23);
        for(int yaw:new int[]{0,45,90,135,180,270,360}){
            var layout=TriStripSampler.layout(d,yaw,0);var out=TriStripSampler.compose(d,layout,new BufferedImage[]{image,image,image},false);
            int ox=(layout.canvasWidth()-5)/2,oy=(layout.canvasHeight()-13)/2;
            for(int y=0;y<13;y++)for(int x=0;x<5;x++){
                boolean selected=(out.getRGB(ox+x,oy+y)>>>24)!=0;
                if(yaw==0||yaw==360)assertEquals(d.selectsPixel(x,y,5,13),selected);
                if(yaw==180)assertEquals(d.selectsPixel(4-x,y,5,13),selected);
                if(yaw==90||yaw==270)assertEquals(d.selectsPixel(4-x,y,5,13,0),selected,"Side profile must stay centred");
                if(selected)assertEquals(image.getRGB(layout.columns()[x],y),out.getRGB(ox+x,oy+y));
            }
        }
        assertTrue(d.copy().isSelectionFollowingTurn());assertTrue(new Gson().fromJson("{}",Strip3DDefinition.class).isSelectionFollowingTurn());
        d.setSelectionFollowingTurn(false);var copy=new Gson().fromJson(new Gson().toJson(d),Strip3DDefinition.class);assertFalse(copy.isSelectionFollowingTurn());
        for(int x=0;x<5;x++)assertEquals(d.selectsPixel(x,8,5,13),d.selectsPixel(x,8,5,13,-1));
    }
    @Test void sampleWidthBoundsCopiesLegacyAndReplacement(){
        var d=new Gson().fromJson("{\"frameWidth\":16,\"frameHeight\":20}",Strip3DDefinition.class);
        assertEquals(16,d.getSampleWidth());d.setFrameSize(24,20);assertEquals(24,d.getSampleWidth());
        var legacy=d.copy();d.setSampleWidth(73);assertFalse(d.sameSampling(legacy));assertEquals(73,d.getSelectionWidth());
        assertTrue(d.copy().sameSampling(d));assertTrue(new Gson().fromJson(new Gson().toJson(d),Strip3DDefinition.class).sameSampling(d));
        assertThrows(IllegalArgumentException.class,()->d.setSampleWidth(0));assertThrows(IllegalArgumentException.class,()->d.setSampleWidth(97));
        d.setFrameSize(8,20);assertEquals(32,d.getSampleWidth());d.setFrameSize(24,20);assertEquals(32,d.getSampleWidth());
    }
    @Test void sampleWindowUsesExactContiguousPixelsAndWrapsAtFullTextureWidth(){
        var d=new Strip3DDefinition();d.setOutlineEnabled(false);d.setVertical(Strip3DDefinition.Sampling.SLIDE);
        BufferedImage[] sources=new BufferedImage[3];for(int f=0;f<3;f++){sources[f]=new BufferedImage(96,24,BufferedImage.TYPE_INT_ARGB);
            for(int y=0;y<24;y++)for(int x=0;x<96;x++)sources[f].setRGB(x,y,0xff000000|x<<16|y<<8|f+1);}
        for(int width:new int[]{1,2,23,24,37,95,96})for(int yaw:new int[]{-360,-179,0,37,90,179,360}){
            d.setSampleWidth(width);var l=TriStripSampler.layout(d,yaw,0);assertEquals(width,l.columns().length);
            assertEquals(Math.floorMod((int)Math.round(48+yaw*96/360.0-width/2.0),96),l.columns()[0]);
            assertArrayEquals(l.columns(),TriStripSampler.layout(d,yaw+360,0).columns());
            assertEquals(width,java.util.Arrays.stream(l.columns()).distinct().count());
            for(int i=1;i<width;i++)assertEquals((l.columns()[i-1]+1)%96,l.columns()[i]);
            for(boolean normal:new boolean[]{false,true}){var image=TriStripSampler.compose(d,l,sources,normal);
                int ox=(l.canvasWidth()-width)/2,oy=(l.canvasHeight()-24)/2;
                for(int y=0;y<24;y++)for(int x=0;x<width;x++)assertEquals(sources[1].getRGB(l.columns()[x],y),image.getRGB(ox+x,oy+y));
            }
        }
        d.setHorizontal(Strip3DDefinition.Sampling.PERSPECTIVE);
        for(int width:new int[]{1,24,37,96})for(int yaw:new int[]{-179,0,37,45,90}){
            d.setSampleWidth(width);var l=TriStripSampler.layout(d,yaw,0);
            double a=Math.toRadians(Math.floorMod(yaw,90));assertEquals(Math.round(width*Math.cos(a))+Math.round(width*Math.sin(a)),l.columns().length);
            assertTrue(l.canvasWidth()>=l.columns().length+2);for(int col:l.columns())assertTrue(col>=0&&col<96);
            assertArrayEquals(l.columns(),TriStripSampler.layout(d,yaw+360,0).columns());
        }
    }

    @Test void rectangularDefaultAndMaskCopyBoundsPersistence(){
        var d=new Strip3DDefinition();assertFalse(d.hasSelectionMask());assertTrue(d.selectsPixel(0,0,24,34));
        boolean[] pixels=new boolean[24*24];Arrays.fill(pixels,true);pixels[0]=false;d.setSelectionMask(24,24,pixels);pixels[0]=true;assertFalse(d.getSelectionMask()[0]);
        var copy=d.copy();assertTrue(copy.sameSampling(d));boolean[] changed=copy.getSelectionMask();changed[1]=false;copy.setSelectionMask(24,24,changed);assertFalse(copy.sameSampling(d));assertTrue(d.getSelectionMask()[1]);
        var restored=new Gson().fromJson(new Gson().toJson(d),Strip3DDefinition.class);assertTrue(restored.sameSampling(d));
        assertThrows(IllegalArgumentException.class,()->d.setSelectionMask(97,24,new boolean[97*24]));assertThrows(IllegalArgumentException.class,()->d.setSelectionMask(24,25,new boolean[24*25]));
        d.setFrameSize(2,5);assertEquals(8,d.getSelectionWidth());assertEquals(5,d.getSelectionHeight());assertFalse(d.getSelectionMask()[0]);
        d.resetSelectionMask();assertFalse(d.hasSelectionMask());assertTrue(d.selectsPixel(0,0,2,5));
    }
    @Test void fullShapeMatchesLegacyAndEmptyShapeClipsColorAndNormalsAtEveryAngle(){
        var d=new Strip3DDefinition();BufferedImage[] images=new BufferedImage[3];for(int i=0;i<3;i++){images[i]=new BufferedImage(96,24,BufferedImage.TYPE_INT_ARGB);for(int y=0;y<24;y++)for(int x=0;x<96;x++)images[i].setRGB(x,y,0xff000000|x<<16|y<<8|i+1);}
        for(var vertical:Strip3DDefinition.Sampling.values())for(var horizontal:Strip3DDefinition.Sampling.values())for(int yaw:new int[]{0,37,90,179})for(int pitch:new int[]{-90,-45,0,45,90}){
            d.setVertical(vertical);d.setHorizontal(horizontal);d.resetSelectionMask();var l=TriStripSampler.layout(d,yaw,pitch);var legacy=TriStripSampler.compose(d,l,images,false);
            boolean[] full=new boolean[24*24];Arrays.fill(full,true);d.setSelectionMask(24,24,full);assertArrayEquals(pixels(legacy),pixels(TriStripSampler.compose(d,l,images,false)));
            d.setSelectionMask(24,24,new boolean[24*24]);for(boolean normal:new boolean[]{false,true})for(int pixel:pixels(TriStripSampler.compose(d,l,images,normal)))assertEquals(0,pixel);
        }
        d.setSelectionFollowingTurn(false);d.setHorizontal(Strip3DDefinition.Sampling.SLIDE);d.setVertical(Strip3DDefinition.Sampling.SLIDE);boolean[] left=new boolean[24*24];for(int y=0;y<24;y++)for(int x=0;x<12;x++)left[y*24+x]=true;d.setSelectionMask(24,24,left);
        var layout=TriStripSampler.layout(d,90,0);var color=TriStripSampler.compose(d,layout,images,false);var normal=TriStripSampler.compose(d,layout,images,true);int ox=(layout.canvasWidth()-24)/2,oy=(layout.canvasHeight()-24)/2;
        boolean[] hole=new boolean[24*24];Arrays.fill(hole,true);hole[12*24+12]=false;d.setSelectionMask(24,24,hole);assertEquals(0xff17121c,TriStripSampler.compose(d,layout,images,false).getRGB(ox+12,oy+12));d.setOutlineEnabled(false);assertEquals(0,TriStripSampler.compose(d,layout,images,false).getRGB(ox+12,oy+12));
        assertEquals(images[1].getRGB(layout.columns()[3],5),color.getRGB(ox+3,oy+5));assertEquals(0,color.getRGB(ox+20,oy+5));assertEquals(0,normal.getRGB(ox+20,oy+5));
    }
    @Test void outlineFollowsFinalMaskWithoutChangingArtworkOrNormalMap(){
        var d=new Strip3DDefinition();boolean[] mask=new boolean[24*24];
        for(int y=0;y<24;y++)for(int x=0;x<24;x++)mask[y*24+x]=Math.abs(x-12)+Math.abs(y-12)<12&&!(x>=10&&x<=14&&y>=10&&y<=14);
        d.setSelectionMask(24,24,mask);BufferedImage[] sources=new BufferedImage[3];
        for(int f=0;f<3;f++){sources[f]=new BufferedImage(96,24,BufferedImage.TYPE_INT_ARGB);for(int y=0;y<24;y++)for(int x=0;x<96;x++)sources[f].setRGB(x,y,x%11==0?0:0xffdc8040);}
        int added=0;for(var v:Strip3DDefinition.Sampling.values())for(var h:Strip3DDefinition.Sampling.values())for(int yaw:new int[]{0,37,90})for(int pitch:new int[]{-90,-40,0,40,90}){
            d.setVertical(v);d.setHorizontal(h);var l=TriStripSampler.layout(d,yaw,pitch);d.setOutlineEnabled(false);var raw=TriStripSampler.compose(d,l,sources,false);var normals=TriStripSampler.compose(d,l,sources,true);d.setOutlineEnabled(true);var outlined=TriStripSampler.compose(d,l,sources,false);
            assertArrayEquals(pixels(normals),pixels(TriStripSampler.compose(d,l,sources,true)));
            for(int y=0;y<raw.getHeight();y++)for(int x=0;x<raw.getWidth();x++){
                int expected=raw.getRGB(x,y);if((expected>>>24)==0){boolean edge=false;if(x>0)edge|=(raw.getRGB(x-1,y)>>>24)!=0;if(x+1<raw.getWidth())edge|=(raw.getRGB(x+1,y)>>>24)!=0;if(y>0)edge|=(raw.getRGB(x,y-1)>>>24)!=0;if(y+1<raw.getHeight())edge|=(raw.getRGB(x,y+1)>>>24)!=0;if(edge){expected=0xff17121c;added++;}}
                assertEquals(expected,outlined.getRGB(x,y),"Final silhouette edge at "+x+","+y);
            }
        }assertTrue(added>0);
    }
    @Test void outlineOptionCopiesInvalidatesAndLoadsOldFiles(){
        var d=new Strip3DDefinition();assertTrue(d.isOutlineEnabled());assertTrue(new Gson().fromJson("{}",Strip3DDefinition.class).isOutlineEnabled());var original=d.copy();d.setOutlineEnabled(false);assertFalse(d.sameSampling(original));assertFalse(d.copy().isOutlineEnabled());assertFalse(new Gson().fromJson(new Gson().toJson(d),Strip3DDefinition.class).isOutlineEnabled());var options=new Strip3DDefinition();options.copyOptionsFrom(d);assertFalse(options.isOutlineEnabled());
    }
    @Test void gridDragUndoResizeAndDefaultWorkOnDraftOnly()throws Exception{
        SwingUtilities.invokeAndWait(()->{try{
            var original=new Strip3DDefinition();var draft=original.copy();var type=Class.forName("com.Hecate.puppet.editor.SelectionShapeDialog$Editor");var ctor=type.getDeclaredConstructor(Strip3DDefinition.class,BufferedImage[].class,int.class);ctor.setAccessible(true);JPanel panel=(JPanel)ctor.newInstance(draft,new BufferedImage[3],1);
            button(panel,"全部清空").doClick();assertTrue(draft.hasSelectionMask());JPanel grid=find(panel,JPanel.class,"selectionGrid");
            mouse(grid,MouseEvent.MOUSE_PRESSED,8+6,8+6,MouseEvent.BUTTON1);mouse(grid,MouseEvent.MOUSE_DRAGGED,8+12*8+6,8+12*8+6,MouseEvent.NOBUTTON);mouse(grid,MouseEvent.MOUSE_RELEASED,110,110,MouseEvent.BUTTON1);
            for(int i=0;i<=8;i++)assertTrue(draft.getSelectionMask()[i*24+i]);button(panel,"撤销一笔").doClick();assertFalse(draft.getSelectionMask()[4*24+4]);button(panel,"重做").doClick();assertTrue(draft.getSelectionMask()[4*24+4]);
            boolean[] before=draft.getSelectionMask();mouse(grid,MouseEvent.MOUSE_PRESSED,1,1,MouseEvent.BUTTON3);assertArrayEquals(before,draft.getSelectionMask());
            var width=find(panel,JSpinner.class,"selectionWidth");var height=find(panel,JSpinner.class,"selectionHeight");assertEquals(96,((SpinnerNumberModel)width.getModel()).getMaximum());assertEquals(24,((SpinnerNumberModel)height.getModel()).getMaximum());width.setValue(30);assertTrue(draft.getSelectionMask()[4*30+4]);assertFalse(draft.getSelectionMask()[29]);
            button(panel,"恢复默认方框").doClick();assertFalse(draft.hasSelectionMask());assertEquals(24,draft.getSelectionWidth());assertFalse(original.hasSelectionMask());
            JSpinner sample=find(panel,JSpinner.class,"sampleWidth");sample.setValue(9);assertEquals(9,draft.getSampleWidth());assertEquals(9,draft.getSelectionWidth());
            sample.setValue(13);assertEquals(13,TriStripSampler.layout(draft,0,0).columns().length);
            button(panel,"撤销一笔").doClick();assertEquals(9,draft.getSampleWidth());button(panel,"重做").doClick();assertEquals(13,draft.getSampleWidth());
            width.setValue(16);assertEquals(16,draft.getSelectionWidth());assertEquals(13,draft.getSampleWidth());assertEquals(24,original.getSampleWidth());
        }catch(Exception ex){throw new RuntimeException(ex);}});
    }
    @Test void typingWidthImmediatelyChangesActualPreviewWithAnExistingDifferentSizeMask()throws Exception{
        SwingUtilities.invokeAndWait(()->{try{
            var original=new Strip3DDefinition();original.setFrameSize(9,12);original.setSampleWidth(11);original.setOutlineEnabled(false);
            boolean[] mask=new boolean[12*12];Arrays.fill(mask,true);original.setSelectionMask(12,12,mask);var draft=original.copy();
            BufferedImage source=new BufferedImage(36,12,BufferedImage.TYPE_INT_ARGB);var colors=new java.util.HashSet<Integer>();
            for(int x=0;x<36;x++){int color=0xff000000|((x*67)%200+40)<<16|((x*37)%200+40)<<8|((x*13)%200+40);colors.add(color);for(int y=0;y<12;y++)source.setRGB(x,y,color);}
            var type=Class.forName("com.Hecate.puppet.editor.SelectionShapeDialog$Editor");var ctor=type.getDeclaredConstructor(Strip3DDefinition.class,BufferedImage[].class,int.class);ctor.setAccessible(true);
            JPanel panel=(JPanel)ctor.newInstance(draft,new BufferedImage[]{source,source,source},1);JSpinner spinner=find(panel,JSpinner.class,"sampleWidth");
            var input=((JSpinner.DefaultEditor)spinner.getEditor()).getTextField();var previewField=type.getDeclaredField("preview");previewField.setAccessible(true);JPanel preview=(JPanel)previewField.get(panel);preview.setSize(300,360);
            for(int width:new int[]{13,16,9,36,11}){
                // No Enter, focus change, arrow button, or confirmation: just edit the text.
                input.setText(Integer.toString(width));assertEquals(width,spinner.getValue());assertEquals(width,draft.getSampleWidth());assertEquals(12,draft.getSelectionWidth());
                var image=new BufferedImage(300,360,BufferedImage.TYPE_INT_ARGB);var g=image.createGraphics();preview.paint(g);g.dispose();
                var seen=new java.util.HashSet<Integer>();for(int x=0;x<300;x++)if(colors.contains(image.getRGB(x,180)))seen.add(image.getRGB(x,180));assertEquals(width,seen.size(),"Actual painted source columns");
            }
            for(String invalid:new String[]{"","0","37","12.5","abc"}){input.setText(invalid);assertEquals(11,draft.getSampleWidth());}
            input.setText("13");button(panel,"撤销一笔").doClick();assertEquals(11,draft.getSampleWidth());assertEquals("11",input.getText());button(panel,"重做").doClick();assertEquals(13,draft.getSampleWidth());assertEquals("13",input.getText());
            assertEquals(11,original.getSampleWidth());assertArrayEquals(mask,draft.getSelectionMask());
        }catch(Exception e){throw new RuntimeException(e);}});
    }
    @Test void missingOrUnreadableTexturesDoNotOpenPaintingCanvas()throws Exception{
        SwingUtilities.invokeAndWait(()->{for(String path:new String[]{null,"Z:/missing-texture.png"}){
            var source=new Strip3DDefinition();source.layer(Strip3DDefinition.Surface.SIDE).setColorPath(path);final boolean[] sawMessage={false};Timer timer=new Timer(80,e->{for(Window w:Window.getWindows())if(w.isShowing()){assertNull(find(w,JPanel.class,"selectionGrid"));JOptionPane pane=find(w,JOptionPane.class,null);if(pane!=null){sawMessage[0]=true;pane.setValue(JOptionPane.OK_OPTION);}}});timer.start();
            try{assertNull(com.Hecate.puppet.editor.SelectionShapeDialog.show(source,null));assertTrue(sawMessage[0]);}finally{timer.stop();}
        }});
    }
    static int[] pixels(BufferedImage image){return image.getRGB(0,0,image.getWidth(),image.getHeight(),null,0,image.getWidth());}
    static void mouse(Component c,int event,int x,int y,int button){c.dispatchEvent(new MouseEvent(c,event,System.currentTimeMillis(),0,x,y,1,false,button));}
    static JButton button(Component c,String text){if(c instanceof JButton b&&text.equals(b.getText()))return b;if(c instanceof Container container)for(Component child:container.getComponents()){JButton found=button(child,text);if(found!=null)return found;}return null;}
    static <T extends Component>T find(Component c,Class<T> type,String name){if(type.isInstance(c)&&(name==null||name.equals(c.getName())))return type.cast(c);if(c instanceof Container container)for(Component child:container.getComponents()){T found=find(child,type,name);if(found!=null)return found;}return null;}
}
