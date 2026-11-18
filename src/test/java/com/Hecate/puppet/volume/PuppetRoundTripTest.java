package com.Hecate.puppet.volume;
import com.Hecate.puppet.config.*;
import com.Hecate.puppet.editor.core.*;
import com.Hecate.puppet.export.*;
import com.jme3.math.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class PuppetRoundTripTest {
    @TempDir Path dir;
    @Test void independentPartsAndTheirChildrenSurviveEditorExportAndBothImporters()throws Exception{
        var app=new VolumeFeatureTest.TestApp();var s=new EditorSkeleton("forest");
        var torso=new EditorBone("torso");torso.setLocalPosition(new Vector3f(4,5,6));s.setRootBone(torso);
        var leg=new EditorBone("independentLeg");leg.setLocalPosition(new Vector3f(-2,1,0));s.addBone(leg);
        var shoe=new EditorBone("shoe");shoe.setLocalPosition(new Vector3f(0,-1,0));leg.addChild(shoe);s.addBone(shoe);
        for(var b:s.getAllBones()){var d=new VolumeDefinition();d.getStrip3D().setEnabled(true);d.getStrip3D().setFrameSize(2,2);d.getStrip3D().setSelectionMask(2,2,new boolean[]{true,false,true,true});b.setVolumeDefinition(d);}
        var image=new BufferedImage(8,2,BufferedImage.TYPE_INT_ARGB);image.setRGB(3,1,0xff123456);var png=dir.resolve("shoe.png");ImageIO.write(image,"png",png.toFile());
        shoe.getVolumeDefinition().getStrip3D().layer(Strip3DDefinition.Surface.SIDE).setColorPath(png.toString());
        var r=new EditorPuppetRenderer(app,s);r.initialize();var path=dir.resolve("forest.puppet");ExportManager.export(ExportFormat.PUPPET,s,r,path.toString());
        var config=PuppetIO.loadFromFile(path.toString());assertEquals(3,config.getBones().size());
        var loaded=new EditorSkeleton("loaded");var lr=new EditorPuppetRenderer(app,loaded);PuppetIO.applyConfig(config,loaded,lr);
        assertEquals(3,loaded.getBoneCount());assertNull(loaded.findBone("independentLeg").getParent());assertEquals("independentLeg",loaded.findBone("shoe").getParent().getName());
        assertEquals(leg.getLocalPosition(),loaded.findBone("independentLeg").getLocalPosition());assertArrayEquals(new boolean[]{true,false,true,true},loaded.findBone("shoe").getVolumeDefinition().getStrip3D().getSelectionMask());
        assertEquals(0xff123456,ImageIO.read(Path.of(loaded.findBone("shoe").getVolumeDefinition().getStrip3D().layer(Strip3DDefinition.Surface.SIDE).getColorPath()).toFile()).getRGB(3,1));
        var world=new Vector3f();loaded.findBone("shoe").getWorldTransform(world,new Quaternion(),new Vector3f());assertEquals(new Vector3f(-2,0,0),world);
        var game=new com.Hecate.puppet.core.Skeleton("runtime");var gr=new com.Hecate.puppet.core.PuppetRenderer(app,game);PuppetIO.applyConfig(config,game,gr);
        assertEquals(3,game.getAllBones().size());assertNull(game.findBone("independentLeg").getParent());assertEquals("independentLeg",game.findBone("shoe").getParent().getName());
        ExportManager.export(ExportFormat.PUPPET,loaded,lr,path.toString());assertEquals(3,PuppetIO.loadFromFile(path.toString()).getBones().size());r.cleanup();lr.cleanup();gr.cleanup();
    }
    @Test void portablePuppetKeepsArtworkWeightsAndPoseAfterOriginalsAreRemoved()throws Exception{
        var app=new VolumeFeatureTest.TestApp();var skeleton=new EditorSkeleton("中文木偶");var root=new EditorBone("根");var bone=new EditorBone("头部");root.addChild(bone);skeleton.setRootBone(root);
        root.setLocalPosition(new Vector3f(1,2,3));bone.setLocalRotation(new Quaternion().fromAngles(.1f,.4f,.2f));bone.setLocalScale(new Vector3f(1.2f,.9f,1));
        var d=new VolumeDefinition();d.setSurfaceContourSnapped(false);d.setShape(VolumeDefinition.Shape.FRUSTUM);d.setDimensions(40,52,28);d.setPixelsPerUnit(24);d.setTopRatio(.3f);d.setLightingEnabled(true);d.setLight(37,28,.7f);var strip=d.getStrip3D();strip.setEnabled(true);strip.setFrameSize(16,20);strip.setSampleWidth(27);strip.setHorizontal(Strip3DDefinition.Sampling.PERSPECTIVE);strip.setVertical(Strip3DDefinition.Sampling.PERSPECTIVE);
        int[] weights=new int[60];for(int i=0;i<60;i++)weights[i]=i%3+1;strip.setImportance(weights);BufferedImage[] artwork=new BufferedImage[3],normals=new BufferedImage[3];Map<String,byte[]> original=new HashMap<>();
        for(var face:Strip3DDefinition.Surface.values()){int index=face.ordinal();var layer=strip.layer(face);layer.setSampling(index,0,7,9);for(int k=0;k<2;k++){BufferedImage image=new BufferedImage(64,20,BufferedImage.TYPE_INT_ARGB);for(int y=0;y<20;y++)for(int x=0;x<64;x++)image.setRGB(x,y,k==1?0xff8080ff:0xff000000|(40+index*55)<<16|(x*3)<<8|y*8);Path path=dir.resolve(face+"-"+k+".png");ImageIO.write(image,"png",path.toFile());original.put(path.toString(),Files.readAllBytes(path));if(k==0){artwork[index]=image;layer.setColorPath(path.toString());}else{normals[index]=image;layer.setNormalPath(path.toString());}}}
        boolean[] mask=new boolean[16*20];for(int y=0;y<20;y++)for(int x=0;x<16;x++)mask[y*16+x]=x>Math.abs(y-10)/2&&x<15-Math.abs(y-10)/2;strip.setSelectionMask(16,20,mask);strip.setOutlineEnabled(false);
        bone.setVolumeDefinition(d);var wrapper=new VolumeDefinition();wrapper.getStrip3D().setEnabled(true);wrapper.setCoverTarget("头部");root.setVolumeDefinition(wrapper);var renderer=new EditorPuppetRenderer(app,skeleton);renderer.initialize();Path pack=dir.resolve("complete.puppet");ExportManager.export(ExportFormat.PUPPET,skeleton,renderer,pack.toString());assertTrue(PuppetPackageIO.isPackageFile(pack.toString()));
        try(ZipFile zip=new ZipFile(pack.toFile())){assertEquals(7,zip.size());String json=new String(zip.getInputStream(zip.getEntry("puppet.json")).readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);assertTrue(json.contains("importance"));assertFalse(json.contains("cachedDropOrders"));}
        for(String path:original.keySet())Files.delete(Path.of(path));
        var config=PuppetIO.loadFromFile(pack.toString());var loaded=new EditorSkeleton("loaded");var loadedRenderer=new EditorPuppetRenderer(app,loaded);PuppetIO.applyConfig(config,loaded,loadedRenderer);var actual=loaded.findBone("头部").getVolumeDefinition();assertEquals("头部",loaded.findBone("根").getVolumeDefinition().getCoverTarget());assertEquals("根",loaded.findBone("头部").getParent().getName());assertEquals(root.getLocalPosition(),loaded.findBone("根").getLocalPosition());assertEquals(bone.getLocalRotation(),loaded.findBone("头部").getLocalRotation());assertEquals(bone.getLocalScale(),loaded.findBone("头部").getLocalScale());assertTrue(d.sameGeometry(actual));assertTrue(actual.isSurfaceDepthEnabled());assertFalse(actual.isSurfaceContourSnapped());assertEquals(d.getLightYaw(),actual.getLightYaw());assertEquals(d.getLightElevation(),actual.getLightElevation());assertEquals(d.getLightStrength(),actual.getLightStrength());assertTrue(actual.isLightingEnabled());assertArrayEquals(weights,actual.getStrip3D().getImportance());assertTrue(actual.getStrip3D().isImportanceEnabled());assertEquals(27,actual.getStrip3D().getSampleWidth());assertEquals(16,actual.getStrip3D().getSelectionWidth());assertEquals(20,actual.getStrip3D().getSelectionHeight());assertArrayEquals(mask,actual.getStrip3D().getSelectionMask());assertFalse(actual.getStrip3D().isOutlineEnabled());
        BufferedImage[] restoredArt=new BufferedImage[3],restoredNormals=new BufferedImage[3];for(var face:Strip3DDefinition.Surface.values()){var from=d.getStrip3D().layer(face);var to=actual.getStrip3D().layer(face);assertArrayEquals(original.get(from.getColorPath()),Files.readAllBytes(Path.of(to.getColorPath())));assertArrayEquals(original.get(from.getNormalPath()),Files.readAllBytes(Path.of(to.getNormalPath())));restoredArt[face.ordinal()]=ImageIO.read(Path.of(to.getColorPath()).toFile());restoredNormals[face.ordinal()]=ImageIO.read(Path.of(to.getNormalPath()).toFile());assertEquals(from.getOffsetX(),to.getOffsetX());}
        for(double yaw:new double[]{-179,0,37,90,180})for(double pitch:new double[]{-90,-45,0,32,89,90}){var before=TriStripSampler.layout(strip,yaw,pitch);var after=TriStripSampler.layout(actual.getStrip3D(),yaw,pitch);assertArrayEquals(before.rows(),after.rows());assertArrayEquals(before.columns(),after.columns());for(boolean normal:new boolean[]{false,true}){var a=TriStripSampler.compose(strip,before,normal?normals:artwork,normal);var b=TriStripSampler.compose(actual.getStrip3D(),after,normal?restoredNormals:restoredArt,normal);assertArrayEquals(a.getRGB(0,0,a.getWidth(),a.getHeight(),null,0,a.getWidth()),b.getRGB(0,0,b.getWidth(),b.getHeight(),null,0,b.getWidth()));}}
        Path second=dir.resolve("resaved.puppet");ExportManager.export(ExportFormat.PUPPET,loaded,loadedRenderer,second.toString());assertTrue(PuppetIO.loadFromFile(second.toString()).getBones().stream().anyMatch(b->"头部".equals(b.getName())));renderer.cleanup();loadedRenderer.cleanup();
    }
    @Test void legacyJsonAndClasspathAssetsStillLoadAndMissingImagesCannotReplaceAGoodExport()throws Exception{
        var app=new VolumeFeatureTest.TestApp();var root=new com.Hecate.puppet.core.Bone("部件");var skeleton=new com.Hecate.puppet.core.Skeleton("旧木偶");skeleton.setRootBone(root);var renderer=new com.Hecate.puppet.core.PuppetRenderer(app,skeleton);var d=new VolumeDefinition();d.getStrip3D().setEnabled(true);d.getStrip3D().setImportanceEnabled(false);d.getStrip3D().setVertical(Strip3DDefinition.Sampling.SLIDE);d.getStrip3D().layer(Strip3DDefinition.Surface.SIDE).setColorPath("textures/TriStripPrototype/side.png");root.setVolumeDefinition(d);
        var config=PuppetIO.createConfig(skeleton,renderer);Path json=dir.resolve("old.puppet");PuppetIO.saveToFile(config,json.toString());assertFalse(PuppetPackageIO.isPackageFile(json.toString()));assertEquals("旧木偶",PuppetIO.loadFromFile(json.toString()).getName());Path pack=dir.resolve("new.ppkg");PuppetPackageIO.savePackage(skeleton,renderer,pack.toString());var actual=PuppetIO.loadFromFile(pack.toString()).getBones().get(0).getVolumeDefinition();assertFalse(actual.getStrip3D().isImportanceEnabled());assertEquals(Strip3DDefinition.Sampling.SLIDE,actual.getStrip3D().getVertical());assertTrue(Files.exists(Path.of(actual.getStrip3D().layer(Strip3DDefinition.Surface.SIDE).getColorPath())));
        String resourceName="roundtrip-"+java.util.UUID.randomUUID()+".puppet";Path resource=Path.of("target/test-classes").resolve(resourceName);Files.copy(pack,resource);try{var fromResource=PuppetIO.loadFromResource(resourceName);assertEquals("旧木偶",fromResource.getName());assertTrue(Files.isRegularFile(Path.of(fromResource.getBones().get(0).getVolumeDefinition().getStrip3D().layer(Strip3DDefinition.Surface.SIDE).getColorPath())));}finally{Files.delete(resource);}
        byte[] previous=Files.readAllBytes(pack);d.getStrip3D().layer(Strip3DDefinition.Surface.TOP).setColorPath(dir.resolve("missing.png").toString());root.setVolumeDefinition(d);assertThrows(java.io.IOException.class,()->PuppetPackageIO.savePackage(skeleton,renderer,pack.toString()));assertArrayEquals(previous,Files.readAllBytes(pack));
        Path broken=dir.resolve("broken.puppet");try(ZipFile good=new ZipFile(pack.toFile());ZipOutputStream output=new ZipOutputStream(Files.newOutputStream(broken))){output.putNextEntry(new ZipEntry("puppet.json"));good.getInputStream(good.getEntry("puppet.json")).transferTo(output);output.closeEntry();}assertThrows(java.io.IOException.class,()->PuppetIO.loadFromFile(broken.toString()));
    }
}
