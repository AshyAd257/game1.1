package com.Hecate.puppet.pixel;

import com.Hecate.puppet.volume.VolumeDefinition;
import com.Hecate.puppet.volume.VolumeReferenceView;
import com.Hecate.puppet.editor.core.EditorBone;
import com.jme3.asset.DesktopAssetManager;
import com.jme3.collision.CollisionResults;
import com.jme3.math.*;
import com.jme3.renderer.Camera;
import com.jme3.scene.*;
import com.jme3.scene.shape.Box;
import java.awt.image.BufferedImage;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class OffsetPixelOrbitTest {
    private static VolumeDefinition boxAt(Vector3f centre){
        Node object=new Node("Head");
        Geometry mesh=new Geometry("Mesh",new Box(.25f,.375f,.125f));
        mesh.setLocalTranslation(centre);
        object.attachChild(mesh);
        return BlenderPixelImporter.fromScene(object).get(0).definition();
    }

    @Test void movingTheJointDoesNotMoveThePhotosDuringAnOrbit(){
        Vector3f offset=new Vector3f(0,0,-2.5f);
        var centred=boxAt(Vector3f.ZERO);
        var offsetPart=boxAt(offset);
        Quaternion rotation=new Quaternion().fromAngleAxis(FastMath.HALF_PI,Vector3f.UNIT_X);
        var solid=PixelRingTexture.silhouette();
        var sources=new BufferedImage[]{solid,solid,solid};
        try(var a=PixelAtlasCache.SHARED.acquire(centred);var b=PixelAtlasCache.SHARED.acquire(offsetPart)){
            int n=a.atlas().size();
            assertEquals(n,b.atlas().size());
            // Same world geometry, represented once with the joint inside and once outside.
            var worldCentre=rotation.mult(offset);
            var reference=new PixelAssemblyPreview.Part(centred,a.atlas(),new Transform(worldCentre,rotation),0,sources);
            var actual=new PixelAssemblyPreview.Part(offsetPart,b.atlas(),new Transform(Vector3f.ZERO,rotation),0,sources);
            for(int yaw:new int[]{0,9,23,89,135,181,269,359}){
                for(float pitch:new float[]{-90,-67.5f,-22.5f,0,22.5f,67.5f,90}){
                    var expected=PixelAssemblyPreview.render(List.of(reference),worldCentre,n,yaw,pitch);
                    var rendered=PixelAssemblyPreview.render(List.of(actual),worldCentre,n,yaw,pitch);
                    assertTrue((expected.getRGB(n/2,n/2)>>>24)>0);
                    assertArrayEquals(expected.getRGB(0,0,n,n,null,0,n),rendered.getRGB(0,0,n,n,null,0,n));
                }
            }
        }
    }

    @Test void cullingAndPickingUseTheVisiblePartUnderATranslatedParent(){
        Node parent=new Node();
        parent.setLocalTranslation(4,5,0);
        parent.updateGeometricState();
        var part=boxAt(new Vector3f(0,0,-2.5f));
        var view=new BakedPixelView(new DesktopAssetManager(true),parent,"Head");
        var camera=new Camera(400,400);
        camera.setParallelProjection(true);
        camera.setFrustum(.1f,20,-.7f,.7f,.7f,-.7f);
        Quaternion rotation=new Quaternion().fromAngleAxis(FastMath.HALF_PI,Vector3f.UNIT_X);
        try{
            view.prepare(part);
            for(float x:new float[]{0,1}){
                var joint=new Vector3f(x,0,0);
                var centre=parent.getWorldTranslation().add(joint).addLocal(rotation.mult(view.atlas().centre()));
                camera.setLocation(centre.add(0,0,2));
                camera.lookAt(centre,Vector3f.UNIT_Y);
                view.update(part,joint,rotation,Vector3f.UNIT_XYZ,camera,true,false);
                parent.updateGeometricState();
                assertTrue(parent.getWorldBound().contains(centre));
                CollisionResults hits=new CollisionResults();
                assertEquals(1,parent.collideWith(new Ray(camera.getLocation(),camera.getDirection()),hits));
                assertEquals(centre.z+.375f,hits.getClosestCollision().getContactPoint().z,.0001f);
            }
        }finally{view.cleanup();}
    }

    @Test void pixelsSolidAndXrayFollowAnOffCentreChildWhenItsParentTurns(){
        var definition=boxAt(new Vector3f(.5f,-.75f,.25f));
        var puppet=new Node("Puppet");
        puppet.setLocalTranslation(6,-2,4);
        puppet.setLocalRotation(new Quaternion().fromAngles(.2f,-.4f,.1f));
        puppet.setLocalScale(-2,.5f,3);
        puppet.updateGeometricState();
        var assets=new DesktopAssetManager(true);
        var reference=new VolumeReferenceView(assets,puppet,"Hand");
        var pixels=new BakedPixelView(assets,puppet,"Hand");
        var camera=new Camera(640,480);
        camera.setFrustumPerspective(50,640f/480,.1f,40);
        var arm=new EditorBone("Arm");
        var hand=new EditorBone("Hand");
        arm.addChild(hand);
        arm.setLocalPosition(new Vector3f(1,.25f,-.5f));
        hand.setLocalPosition(new Vector3f(.75f,0,.125f));
        hand.setLocalRotation(new Quaternion().fromAngles(.1f,.3f,-.2f));
        reference.setXray(true);
        try{
            pixels.prepare(definition);
            assertNull(pixels.error());
            for(int angle=0;angle<360;angle+=15){
                float handScaleX=angle<180?-3:3;
                hand.setLocalScale(new Vector3f(handScaleX,2,.25f));
                arm.setLocalRotation(new Quaternion().fromAngleAxis(angle*FastMath.DEG_TO_RAD,Vector3f.UNIT_Y));
                var position=new Vector3f();var rotation=new Quaternion();var scale=new Vector3f();
                hand.getWorldTransform(position,rotation,scale);
                reference.update(definition,position,rotation,scale,VolumeReferenceView.Mode.SOLID,true);
                puppet.updateGeometricState();
                // Joint spacing inherits scale. The rigid mesh inherits only its combined sign.
                var joint=arm.getLocalRotation().mult(hand.getLocalPosition()).addLocal(arm.getLocalPosition());
                var worldJoint=puppet.localToWorld(joint,null);
                var worldRotation=puppet.getWorldRotation().mult(arm.getLocalRotation()).multLocal(hand.getLocalRotation());
                float mirrorX=-Math.signum(handScaleX);
                var expectedCentre=worldRotation.mult(new Vector3f(.5f*mirrorX,-.75f,.25f)).addLocal(worldJoint);
                var target=puppet.getWorldTranslation();
                camera.setLocation(PixelViewSelector.direction(angle,25).mult(10).addLocal(target));
                camera.lookAt(target,Vector3f.UNIT_Y);
                pixels.update(definition,position,rotation,scale,camera,true,false);
                var screen=camera.getScreenCoordinates(expectedCentre);
                var xy=new Vector2f(screen.x,screen.y);
                var start=camera.getWorldCoordinates(xy,0);
                var ray=new Ray(start,camera.getWorldCoordinates(xy,1).subtractLocal(start).normalizeLocal());
                assertEquals(1,pixels.geometry().collideWith(ray,new CollisionResults()));
                for(Geometry mesh:new Geometry[]{reference.getGeometry(),reference.getXrayWireGeometry()}){
                    mesh.computeWorldMatrix();
                    for(Vector3f vertex:new Vector3f[]{mesh.getModelBound().getCenter(),new Vector3f(.75f,-.375f,.375f)}){
                        var expected=worldRotation.mult(new Vector3f(mirrorX*vertex.x,vertex.y,vertex.z)).addLocal(worldJoint);
                        assertEquals(0,mesh.getWorldMatrix().mult(vertex).distance(expected),.00002f);
                    }
                    assertEquals(0,mesh.getWorldBound().getCenter().distance(expectedCentre),.00002f);
                }
            }
        }finally{reference.cleanup();pixels.cleanup();}
    }
}
