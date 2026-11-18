package com.Hecate.puppet.volume;
import com.google.gson.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class TriStripPrototypeTest {
    @Test void updatedHtmlMatchesExactlyAcross96ViewsAndModes()throws Exception{
        JsonObject fixture=new JsonParser().parse(Files.readString(Path.of("src/test/resources/tri-strip/reference-v2.json"))).getAsJsonObject();
        BufferedImage[] art=new BufferedImage[3];for(var f:Strip3DDefinition.Surface.values())art[f.ordinal()]=Strip3DTextures.exampleImage(f);
        for(JsonElement item:fixture.getAsJsonArray("cases")){JsonObject c=item.getAsJsonObject();Strip3DDefinition d=new Strip3DDefinition();
            d.setVertical(c.get("v").getAsString().equals("persp")?Strip3DDefinition.Sampling.PERSPECTIVE:Strip3DDefinition.Sampling.SLIDE);
            d.setHorizontal(c.get("h").getAsString().equals("persp")?Strip3DDefinition.Sampling.PERSPECTIVE:Strip3DDefinition.Sampling.SLIDE);d.setImportanceEnabled(c.get("impMode").getAsString().equals("imp"));
            var l=TriStripSampler.layout(d,c.get("yaw").getAsDouble(),c.get("pitch").getAsDouble());String desc=c.get("yaw")+" / "+c.get("pitch")+" / "+c.get("v")+" / "+c.get("h")+" / "+c.get("impMode");
            assertArrayEquals(ints(c.getAsJsonArray("rows")),l.rows(),desc+" rows");assertArrayEquals(ints(c.getAsJsonArray("cols")),l.columns(),desc+" columns");
            BufferedImage image=TriStripSampler.compose(d,l,art,false);byte[] expected=Base64.getDecoder().decode(c.get("pixels").getAsString()),actual=new byte[image.getWidth()*image.getHeight()*4];int pos=0;
            for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++){int rgb=image.getRGB(x,y);actual[pos++]=(byte)(rgb>>16);actual[pos++]=(byte)(rgb>>8);actual[pos++]=(byte)rgb;actual[pos++]=(byte)(rgb>>>24);}assertArrayEquals(expected,actual,desc+" RGBA");
        }
    }
    private static int[] ints(JsonArray a){int[] r=new int[a.size()];for(int i=0;i<r.length;i++)r[i]=a.get(i).getAsInt();return r;}
    @Test void priorityDropsAreNestedAndCopiesAndJsonAreIsolated(){
        Strip3DDefinition d=new Strip3DDefinition();for(int face=0;face<3;face++)for(int count=1;count<=24;count++){
            int[] larger=TriStripSampler.keepRows(d,face,count),smaller=TriStripSampler.keepRows(d,face,count-1);for(int row:smaller)assertTrue(Arrays.stream(larger).anyMatch(r->r==row));}
        var copy=d.copy();int[] values=copy.getImportance();values[24]=3;copy.setImportance(values);copy.setHorizontal(Strip3DDefinition.Sampling.PERSPECTIVE);
        assertEquals(1,d.getImportance()[24]);assertFalse(copy.sameSampling(d));Gson gson=new Gson();var restored=gson.fromJson(gson.toJson(copy),Strip3DDefinition.class);assertTrue(copy.sameSampling(restored));
    }
    @Test void sourceTransparencyAndNormalsUseSameLookup(){
        var d=new Strip3DDefinition();d.setImportanceEnabled(false);var l=TriStripSampler.layout(d,0,0);BufferedImage[] art=new BufferedImage[3];for(int i=0;i<3;i++)art[i]=new BufferedImage(96,24,BufferedImage.TYPE_INT_ARGB);
        art[1].setRGB(40,12,0xff123456);var image=TriStripSampler.compose(d,l,art,false);var normal=TriStripSampler.compose(d,l,art,true);
        assertEquals(0xff123456,image.getRGB(12,20));assertEquals(0xff123456,normal.getRGB(12,20));assertEquals(0,image.getRGB(20,20));assertEquals(0xff17121c,image.getRGB(11,20));assertEquals(0,image.getRGB(11,19));
    }
    @Test void importsInferDimensionsRejectMixedSizesAndUndoSettings()throws Exception{
        Path dir=Files.createTempDirectory("tri-strip-import");var assets=new com.jme3.asset.DesktopAssetManager(true);var d=new Strip3DDefinition();
        Path color=dir.resolve("color.png"),bad=dir.resolve("bad.png");javax.imageio.ImageIO.write(new BufferedImage(128,32,BufferedImage.TYPE_INT_ARGB),"png",color.toFile());javax.imageio.ImageIO.write(new BufferedImage(100,24,BufferedImage.TYPE_INT_ARGB),"png",bad.toFile());
        d.layer(Strip3DDefinition.Surface.SIDE).setColorPath(color.toString());Strip3DTextures.validateSources(d,assets);assertEquals(32,d.getFrameWidth());assertEquals(32,d.getFrameHeight());
        d.layer(Strip3DDefinition.Surface.TOP).setColorPath(bad.toString());assertThrows(java.io.IOException.class,()->Strip3DTextures.validateSources(d,assets));assertEquals(32,d.getFrameWidth());
        var bone=new com.Hecate.puppet.editor.core.EditorBone("sample");var volume=new VolumeDefinition();bone.setVolumeDefinition(volume);var next=volume.copy();next.getStrip3D().setHorizontal(Strip3DDefinition.Sampling.PERSPECTIVE);int[] rows=next.getStrip3D().getImportance();rows[24]=3;next.getStrip3D().setImportance(rows);
        var commands=new com.Hecate.puppet.editor.command.CommandManager();commands.executeCommand(new com.Hecate.puppet.editor.command.SetVolumeDefinitionCommand(bone,next,()->{}));commands.undo();assertEquals(1,bone.getVolumeDefinition().getStrip3D().getImportance()[24]);commands.redo();assertEquals(3,bone.getVolumeDefinition().getStrip3D().getImportance()[24]);
    }
    @Test void importancePreviewRespondsToClicksAndPaints()throws Exception{
        javax.swing.SwingUtilities.invokeAndWait(()->{try{
            var d=new Strip3DDefinition();Class<?> type=Class.forName("com.Hecate.puppet.editor.Strip3DTextureDialog$Preview");var ctor=type.getDeclaredConstructors()[0];ctor.setAccessible(true);
            var panel=(javax.swing.JPanel)ctor.newInstance(d,new com.jme3.asset.DesktopAssetManager(true));panel.setSize(740,360);
            for(String key:new String[]{"yaw","pitch"}){var field=type.getDeclaredField(key);field.setAccessible(true);field.setDouble(panel,45);}
            var click=new java.awt.event.MouseEvent(panel,java.awt.event.MouseEvent.MOUSE_PRESSED,0,0,15,36+24*4+2,1,false);for(var listener:panel.getMouseListeners())listener.mousePressed(click);assertEquals(2,d.getImportance()[24]);
            var image=new BufferedImage(740,360,BufferedImage.TYPE_INT_RGB);var graphics=image.createGraphics();panel.paint(graphics);graphics.dispose();javax.imageio.ImageIO.write(image,"png",Path.of("target/tri-strip-priority-preview.png").toFile());
        }catch(Exception e){throw new RuntimeException(e);}});
    }
}
