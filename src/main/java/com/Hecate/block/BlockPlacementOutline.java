package com.Hecate.block;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.BufferUtils;

/**
 * 放置方块预览框 - 手持方块瞄准有效位置时，用空心正方体线框标出将要放置的那一格
 * （效果参考Minecraft的方块选取框）
 */
public class BlockPlacementOutline {

    private static final float HALF_SIZE = 0.505f; // 略大于0.5，让线框贴着方块表面外侧，避免和方块面重叠闪烁（Z-fighting）

    private final Node rootNode;
    private Geometry outlineGeometry;
    private Material wireframeMaterial;
    private boolean visible = false;

    public BlockPlacementOutline(AssetManager assetManager, Node rootNode) {
        this.rootNode = rootNode;

        wireframeMaterial = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        wireframeMaterial.setColor("Color", new ColorRGBA(0f, 0f, 0f, 0.8f));
        wireframeMaterial.getAdditionalRenderState().setWireframe(true);
        wireframeMaterial.getAdditionalRenderState().setLineWidth(2f);
        // 深度测试保持开启，让线框被前方的方块正常遮挡，而不是永远画在最上面
        wireframeMaterial.getAdditionalRenderState().setDepthTest(true);
        wireframeMaterial.getAdditionalRenderState().setDepthWrite(false);

        outlineGeometry = new Geometry("BlockPlacementOutline", createWireframeCube());
        outlineGeometry.setMaterial(wireframeMaterial);
        outlineGeometry.setQueueBucket(RenderQueue.Bucket.Transparent);
    }

    /**
     * 在指定方块坐标显示预览框；传入null则隐藏
     */
    public void update(Vector3f blockPosition) {
        if (blockPosition == null) {
            hide();
            return;
        }

        outlineGeometry.setLocalTranslation(blockPosition);

        if (!visible) {
            rootNode.attachChild(outlineGeometry);
            visible = true;
        }
    }

    public void hide() {
        if (visible) {
            outlineGeometry.removeFromParent();
            visible = false;
        }
    }

    public void cleanup() {
        hide();
        wireframeMaterial = null;
    }

    /**
     * 生成正方体12条边的线框网格（8个顶点，12条线段）
     */
    private Mesh createWireframeCube() {
        Vector3f[] vertices = new Vector3f[]{
                new Vector3f(-HALF_SIZE, -HALF_SIZE, -HALF_SIZE),
                new Vector3f(HALF_SIZE, -HALF_SIZE, -HALF_SIZE),
                new Vector3f(HALF_SIZE, -HALF_SIZE, HALF_SIZE),
                new Vector3f(-HALF_SIZE, -HALF_SIZE, HALF_SIZE),
                new Vector3f(-HALF_SIZE, HALF_SIZE, -HALF_SIZE),
                new Vector3f(HALF_SIZE, HALF_SIZE, -HALF_SIZE),
                new Vector3f(HALF_SIZE, HALF_SIZE, HALF_SIZE),
                new Vector3f(-HALF_SIZE, HALF_SIZE, HALF_SIZE),
        };

        int[] indices = new int[]{
                // 底面四条边
                0, 1, 1, 2, 2, 3, 3, 0,
                // 顶面四条边
                4, 5, 5, 6, 6, 7, 7, 4,
                // 四条竖边
                0, 4, 1, 5, 2, 6, 3, 7,
        };

        Mesh mesh = new Mesh();
        mesh.setMode(Mesh.Mode.Lines);
        mesh.setBuffer(VertexBuffer.Type.Position, 3, BufferUtils.createFloatBuffer(vertices));
        mesh.setBuffer(VertexBuffer.Type.Index, 2, BufferUtils.createIntBuffer(indices));
        mesh.updateBound();

        return mesh;
    }
}
