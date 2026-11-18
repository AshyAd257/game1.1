package com.Hecate.puppet.volume;

import com.Hecate.puppet.config.*;
import com.Hecate.puppet.core.*;
import com.Hecate.puppet.editor.core.*;
import com.Hecate.puppet.editor.command.*;
import com.jme3.app.SimpleApplication;
import com.jme3.asset.DesktopAssetManager;
import com.jme3.bounding.BoundingBox;
import com.jme3.math.*;
import com.jme3.renderer.Camera;
import com.jme3.scene.*;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.*;
import com.jme3.texture.plugins.AWTLoader;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipFile;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class VolumeFeatureTest {
    @TempDir Path temp;
    static final class TestApp extends SimpleApplication {
        TestApp() { assetManager=new DesktopAssetManager(true);cam=new Camera(640,480);cam.setLocation(new Vector3f(0,0,10)); }
        public void simpleInitApp() {}
    }
    @Test void rejectsInvalidDimensionsAndLight() {
        VolumeDefinition d=new VolumeDefinition();
        assertThrows(IllegalArgumentException.class,()->d.setDimensions(0,64,32));
        assertThrows(IllegalArgumentException.class,()->d.setPixelsPerUnit(Float.NaN));
        assertThrows(IllegalArgumentException.class,()->d.setLight(0,91,1));
        assertThrows(IllegalArgumentException.class,()->d.setTopRatio(Float.POSITIVE_INFINITY));
    }
    @Test void allShapesHaveExpectedBoundsAndNonDegenerateOutwardFaces() {
        for(VolumeDefinition.Shape shape:VolumeDefinition.Shape.values()) {
            VolumeDefinition d=new VolumeDefinition();d.setShape(shape);d.setDimensions(64,96,32);
            Mesh mesh=VolumeMeshFactory.create(d);BoundingBox b=(BoundingBox)mesh.getBound();
            assertEquals(1,b.getXExtent(),1e-6);assertEquals(1.5,b.getYExtent(),1e-6);assertEquals(.5,b.getZExtent(),1e-6);
            Vector3f a=new Vector3f(),c=new Vector3f(),e=new Vector3f();
            for(int i=0;i<mesh.getTriangleCount();i++) {
                mesh.getTriangle(i,a,c,e);Vector3f normal=c.subtract(a).cross(e.subtract(a));
                assertTrue(normal.lengthSquared()>1e-10,"degenerate "+shape+" face "+i);
                assertTrue(normal.dot(a.add(c).addLocal(e))>0,"inward "+shape+" face "+i);
            }
        }
    }
    @Test void editsUndoRedoWithoutAliasing() {
        EditorBone b=new EditorBone("arm");VolumeDefinition d=new VolumeDefinition();
        AtomicInteger refreshed=new AtomicInteger();CommandManager commands=new CommandManager();
        commands.executeCommand(new SetVolumeDefinitionCommand(b,d,refreshed::incrementAndGet));
        d.setDimensions(99,99,99);assertEquals(32,b.getVolumeDefinition().getWidthPx());
        commands.undo();assertNull(b.getVolumeDefinition());commands.redo();
        assertEquals(32,b.getVolumeDefinition().getWidthPx());assertEquals(3,refreshed.get());
        commands.executeCommand(new SetVolumeDefinitionCommand(b,null,()->{}));
        assertNull(b.getVolumeDefinition());commands.undo();assertNotNull(b.getVolumeDefinition());
    }
    @Test void editorConversionAndJsonRoundTripRetainVolumeAndOldFilesStayOptional() throws Exception {
        EditorBone b=new EditorBone("arm");VolumeDefinition d=new VolumeDefinition();d.setShape(VolumeDefinition.Shape.FRUSTUM);
        d.setNormalTexturePath("normal.png");d.setLightingEnabled(true);d.setDimensions(24,64,16);b.setVolumeDefinition(d);
        EditorSkeleton editor=new EditorSkeleton("test");editor.setRootBone(b);
        Skeleton runtime=editor.getBaseSkeleton();assertNotSame(b.getVolumeDefinition(),runtime.getRootBone().getVolumeDefinition());
        PuppetRenderer renderer=new PuppetRenderer(new TestApp(),runtime);
        PuppetConfig config=PuppetIO.createConfig(runtime,renderer);Path json=temp.resolve("test.json");PuppetIO.saveToFile(config,json.toString());
        PuppetConfig restored=PuppetIO.loadFromFile(json.toString());VolumeDefinition actual=restored.getBones().get(0).getVolumeDefinition();
        assertEquals(24,actual.getWidthPx());assertEquals(VolumeDefinition.Shape.FRUSTUM,actual.getShape());assertTrue(actual.isLightingEnabled());
        assertEquals("normal.png",actual.getNormalTexturePath());assertEquals("2.0",restored.getVersion());
        TestApp app=new TestApp();EditorSkeleton destination=new EditorSkeleton("loaded");
        EditorPuppetRenderer editorRenderer=new EditorPuppetRenderer(app,destination);
        PuppetIO.applyConfig(restored,destination,editorRenderer);
        assertEquals(24,destination.findBone("arm").getVolumeDefinition().getWidthPx());
        assertNull(new com.google.gson.Gson().fromJson("{\"name\":\"old\"}",BoneConfig.class).getVolumeDefinition());
    }
    @Test void portablePackageContainsAndRemapsNormalTexture() throws Exception {
        Path png=writeNormal("normal.png",32,64,0xff8080ff);
        Bone b=new Bone("arm");VolumeDefinition d=new VolumeDefinition();d.setNormalTexturePath(png.toString());b.setVolumeDefinition(d);
        Skeleton skeleton=new Skeleton("pack");skeleton.setRootBone(b);TestApp app=new TestApp();
        PuppetRenderer renderer=new PuppetRenderer(app,skeleton);Path pack=temp.resolve("volume.ppkg");
        PuppetPackageIO.savePackage(skeleton,renderer,pack.toString());
        try(ZipFile zip=new ZipFile(pack.toFile())) {
            String json=new String(zip.getInputStream(zip.getEntry("puppet.json")).readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
            PuppetConfig data=new com.google.gson.Gson().fromJson(json,PuppetConfig.class);
            String path=data.getBones().get(0).getVolumeDefinition().getNormalTexturePath();
            assertTrue(path.startsWith("textures/"));assertNotNull(zip.getEntry(path));
        }
        assertEquals(png.toString(),b.getVolumeDefinition().getNormalTexturePath());
        EditorSkeleton loaded=new EditorSkeleton("loaded");EditorPuppetRenderer editorRenderer=new EditorPuppetRenderer(app,loaded);
        PuppetPackageIO.loadPackage(pack.toString(),loaded,editorRenderer,app.getAssetManager());
        Path restored=Path.of(loaded.findBone("arm").getVolumeDefinition().getNormalTexturePath());
        assertTrue(Files.isRegularFile(restored));assertArrayEquals(Files.readAllBytes(png),Files.readAllBytes(restored));
    }
    @Test void referenceLayerFollowsTransformsAndVisibilityWithoutRebuildingEveryFrame() {
        Node parent=new Node();VolumeReferenceView view=new VolumeReferenceView(new DesktopAssetManager(true),parent,"arm");
        VolumeDefinition d=new VolumeDefinition();Quaternion r=new Quaternion().fromAngleAxis(1,Vector3f.UNIT_Z);
        view.update(d,new Vector3f(1,2,3),r,new Vector3f(2,1,1),VolumeReferenceView.Mode.SOLID,true);
        Geometry g=view.getGeometry();Mesh mesh=g.getMesh();assertEquals(r,g.getLocalRotation());assertEquals(new Vector3f(1,2,3),g.getLocalTranslation());
        view.update(d,new Vector3f(),r,Vector3f.UNIT_XYZ,VolumeReferenceView.Mode.OVERLAY,true);
        assertSame(mesh,g.getMesh());assertTrue(g.getMaterial().getAdditionalRenderState().isWireframe());
        view.update(d,new Vector3f(),r,Vector3f.UNIT_XYZ,VolumeReferenceView.Mode.SOLID,false);assertEquals(Spatial.CullHint.Always,g.getLocalCullHint());
        view.cleanup();assertEquals(0,parent.getQuantity());
    }
    @Test void normalMaterialLoadsRefreshesAndRejectsMismatchedSize() throws Exception {
        DesktopAssetManager assets=new DesktopAssetManager(true);VolumeAppearance appearance=new VolumeAppearance(assets);
        Path normal=writeNormal("normal.png",32,64,0xff8080ff);VolumeDefinition d=new VolumeDefinition();d.setNormalTexturePath(normal.toString());
        Geometry g=new Geometry("sprite",new Quad(1,2));Texture color=texture(32,64);
        appearance.update(g,color,d,new Quaternion(),false);assertNull(appearance.getError());assertNotNull(g.getMaterial().getParam("NormalMap"));
        Texture old=(Texture)g.getMaterial().getParam("NormalMap").getValue();
        writeNormal("normal.png",32,64,0xffff8080);appearance.invalidate();appearance.update(g,color,d,new Quaternion(),false);
        assertNotSame(old,g.getMaterial().getParam("NormalMap").getValue());
        appearance.update(g,texture(16,16),d,new Quaternion(),false);assertNotNull(appearance.getError());assertNull(g.getMaterial().getParam("NormalMap"));
        appearance.update(g,color,d,new Quaternion(),false);assertNull(appearance.getError());
    }
    @Test void editorReferenceFollowsParentPoseAndRemainsHiddenWhenPartIsHidden() {
        TestApp app=new TestApp();EditorBone parent=new EditorBone("parent"),child=new EditorBone("child");parent.addChild(child);
        parent.setLocalPosition(new Vector3f(3,2,1));parent.setLocalRotation(new Quaternion().fromAngleAxis(.7f,Vector3f.UNIT_Z));
        child.setLocalPosition(new Vector3f(0,2,0));child.setVolumeDefinition(new VolumeDefinition());
        EditorSkeleton skeleton=new EditorSkeleton("rig");skeleton.setRootBone(parent);
        EditorPuppetRenderer renderer=new EditorPuppetRenderer(app,skeleton);renderer.initialize();renderer.setAutoDirectionSwitch(false);
        renderer.setVolumeViewMode(VolumeReferenceView.Mode.SOLID);EditorPuppetPartRenderer part=renderer.getPartRenderer("child");
        part.updateTransform(.016f);Vector3f p=new Vector3f(),scale=new Vector3f();Quaternion r=new Quaternion();child.getWorldTransform(p,r,scale);
        assertSame(part.getReferenceGeometry(),part.getGeometry());assertEquals(p,part.getReferenceGeometry().getLocalTranslation());assertEquals(r,part.getReferenceGeometry().getLocalRotation());
        child.setModelEnabled(true);part.updateTransform(.016f);assertEquals(Spatial.CullHint.Always,part.getReferenceGeometry().getLocalCullHint());child.setModelEnabled(false);
        part.setVisible(false);renderer.setVolumeViewMode(VolumeReferenceView.Mode.OVERLAY);part.updateTransform(.016f);
        assertEquals(Spatial.CullHint.Always,part.getReferenceGeometry().getLocalCullHint());assertEquals(Spatial.CullHint.Always,part.getGeometry().getLocalCullHint());
        part.setVisible(true);renderer.setVolumeViewMode(VolumeReferenceView.Mode.APPEARANCE);part.updateTransform(.016f);
        assertEquals("PuppetVolume",part.getGeometry().getMaterial().getMaterialDef().getName());
        child.setVolumeDefinition(null);part.updateTransform(.016f);
        assertNotEquals("PuppetVolume",part.getGeometry().getMaterial().getMaterialDef().getName());renderer.cleanup();
    }
    @Test void stripNormalsUseRepeatAndIntegerSizedRingAndRefreshFromDisk() throws Exception {
        DesktopAssetManager assets=new DesktopAssetManager(true);Path png=writeNormal("strip.png",36,16,0xff8080ff);
        VolumeDefinition d=new VolumeDefinition();d.setNormalTexturePath(png.toString());Geometry g=new Geometry("strip",new Quad(1,1));
        VolumeAppearance appearance=new VolumeAppearance(assets);appearance.update(g,texture(36,16),d,new Quaternion(),true);
        Texture first=(Texture)g.getMaterial().getParam("NormalMap").getValue();assertEquals(Texture.WrapMode.Repeat,first.getWrap(Texture.WrapAxis.S));
        assertEquals(Texture.MagFilter.Nearest,first.getMagFilter());assertEquals(36,first.getImage().getWidth());
        writeNormal("strip.png",36,16,0xffff8080);appearance.invalidate();appearance.update(g,texture(36,16),d,new Quaternion(),true);
        Texture second=(Texture)g.getMaterial().getParam("NormalMap").getValue();
        assertFalse(first.getImage().getData(0).duplicate().rewind().equals(second.getImage().getData(0).duplicate().rewind()));
    }
    @Test void integratedResizePreservesIntegerFramingAndUndoRestoresCustomCrop() {
        EditorBone b=new EditorBone("strip");VolumeDefinition d=new VolumeDefinition();d.setPixelsPerUnit(16);
        b.setVolumeDefinition(d);b.setRotationStripEnabled(true);b.setStripFrameWidthPx(23);b.setStripFrameHeightPx(47);
        VolumeDefinition changed=d.copy();changed.setDimensions(40,80,32);
        CommandManager commands=new CommandManager();commands.executeCommand(new SetVolumeDefinitionCommand(b,changed,()->{},true));
        assertEquals(40,b.getStripFrameWidthPx());assertEquals(80,b.getStripFrameHeightPx());assertEquals(2.5f,b.getStripWidth());assertEquals(5f,b.getStripHeight());
        commands.undo();assertEquals(23,b.getStripFrameWidthPx());assertEquals(47,b.getStripFrameHeightPx());assertEquals(23/16f,b.getStripWidth());
        commands.redo();assertEquals(2.5f,b.getStripWidth());
        VolumeDefinition density=changed.copy();density.setPixelsPerUnit(32);
        commands.executeCommand(new SetVolumeDefinitionCommand(b,density,()->{},true));
        assertEquals(40,b.getStripFrameWidthPx());assertEquals(1.25f,b.getStripWidth());commands.undo();assertEquals(2.5f,b.getStripWidth());
        b.setStripFrameWidthPx(19);VolumeDefinition lighting=b.getVolumeDefinition().copy();lighting.setLightingEnabled(true);
        commands.executeCommand(new SetVolumeDefinitionCommand(b,lighting,()->{},true));assertEquals(19,b.getStripFrameWidthPx());
        Bone runtime=new Bone("runtime");runtime.setVolumeDefinition(d);runtime.setStripFrameWidthPx(23);assertEquals(23/16f,runtime.getStripWidth());
    }
    @Test void threeLayerDefinitionsCopyAndUndoIndependently() {
        EditorBone bone=new EditorBone("new");VolumeDefinition d=new VolumeDefinition();d.getStrip3D().setEnabled(true);
        d.getStrip3D().layer(Strip3DDefinition.Surface.TOP).setColorPath("top.png");
        d.getStrip3D().layer(Strip3DDefinition.Surface.SIDE).setSampling(3,7,0,12);
        CommandManager commands=new CommandManager();commands.executeCommand(new SetVolumeDefinitionCommand(bone,d,()->{}));
        d.getStrip3D().layer(Strip3DDefinition.Surface.TOP).setColorPath("changed.png");
        assertEquals("top.png",bone.getVolumeDefinition().getStrip3D().layer(Strip3DDefinition.Surface.TOP).getColorPath());
        commands.undo();assertNull(bone.getVolumeDefinition());commands.redo();assertEquals(7,bone.getVolumeDefinition().getStrip3D().layer(Strip3DDefinition.Surface.SIDE).getOffsetY());
        assertFalse(new com.google.gson.Gson().fromJson("{\"widthPx\":32}",VolumeDefinition.class).isStrip3DEnabled());
    }
    @Test void portablePackageRetainsAllThreeColorsAndNormals() throws Exception {
        VolumeDefinition d=new VolumeDefinition();d.getStrip3D().setEnabled(true);
        for(var surface:Strip3DDefinition.Surface.values()){
            var layer=d.getStrip3D().layer(surface);layer.setColorPath(writeNormal(surface+"-color.png",80,96,0xff887755).toString());
            layer.setNormalPath(writeNormal(surface+"-normal.png",80,96,0xff8080ff).toString());layer.setSampling(3,5,13,17);
        }
        Bone bone=new Bone("part");bone.setVolumeDefinition(d);Skeleton skeleton=new Skeleton("three");skeleton.setRootBone(bone);
        TestApp app=new TestApp();PuppetRenderer renderer=new PuppetRenderer(app,skeleton);Path pack=temp.resolve("three.ppkg");PuppetPackageIO.savePackage(skeleton,renderer,pack.toString());
        EditorSkeleton loaded=new EditorSkeleton("loaded");EditorPuppetRenderer editorRenderer=new EditorPuppetRenderer(app,loaded);PuppetPackageIO.loadPackage(pack.toString(),loaded,editorRenderer,app.getAssetManager());
        VolumeDefinition actual=loaded.findBone("part").getVolumeDefinition();assertTrue(actual.isStrip3DEnabled());
        for(var surface:Strip3DDefinition.Surface.values()){
            var expected=d.getStrip3D().layer(surface);var layer=actual.getStrip3D().layer(surface);
            assertArrayEquals(Files.readAllBytes(Path.of(expected.getColorPath())),Files.readAllBytes(Path.of(layer.getColorPath())));
            assertArrayEquals(Files.readAllBytes(Path.of(expected.getNormalPath())),Files.readAllBytes(Path.of(layer.getNormalPath())));assertEquals(17,layer.getTravelY());
        }
        EditorSkeleton converted=new EditorSkeleton("converted");EditorBone eb=new EditorBone("part");eb.setVolumeDefinition(actual);converted.setRootBone(eb);
        assertTrue(converted.getBaseSkeleton().getRootBone().getVolumeDefinition().isStrip3DEnabled());
    }
    @Test void threeDimensionalRuntimeAndEditorFollowBonePoseAndVisibility() {
        TestApp app=new TestApp();VolumeDefinition d=new VolumeDefinition();d.getStrip3D().setEnabled(true);
        Bone parent=new Bone("root"),child=new Bone("part");parent.addChild(child);child.setVolumeDefinition(d);child.setLocalPosition(new Vector3f(0,2,0));
        parent.setLocalPosition(new Vector3f(3,0,0));parent.setLocalRotation(new Quaternion().fromAngleAxis(.5f,Vector3f.UNIT_Z));
        Skeleton skeleton=new Skeleton("three");skeleton.setRootBone(parent);PuppetRenderer renderer=new PuppetRenderer(app,skeleton);renderer.initialize();
        PuppetPartRenderer part=renderer.getPartRenderer("part");part.updateTransform();Geometry image=part.getGeometry();
        assertEquals("PuppetStrip3D",image.getMaterial().getMaterialDef().getName());
        Matrix4f world=(Matrix4f)image.getMaterial().getParam("ArtToWorld").getValue();Vector3f pos=new Vector3f(),scale=new Vector3f();Quaternion rotation=new Quaternion();child.getWorldTransform(pos,rotation,scale);
        assertTrue(world.toTranslationVector().distance(pos)<.001f);part.setVisible(false);part.updateTransform();assertEquals(Spatial.CullHint.Always,image.getLocalCullHint());
        part.setVisible(true);renderer.setManualRotationAngle(.7f);part.updateTransform();
        Matrix4f manual=(Matrix4f)image.getMaterial().getParam("ArtToWorld").getValue();assertTrue(manual.toTranslationVector().distance(new Quaternion().fromAngleAxis(.7f,Vector3f.UNIT_Y).mult(pos))<.001f);
        renderer.cleanup();assertNull(image.getParent());
    }
    private Path writeNormal(String name,int w,int h,int argb)throws Exception {
        BufferedImage image=new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<h;y++)for(int x=0;x<w;x++)image.setRGB(x,y,argb);
        Path path=temp.resolve(name);ImageIO.write(image,"png",path.toFile());return path;
    }
    private Texture texture(int w,int h){return new Texture2D(new AWTLoader().load(new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB),true));}
}
