package com.Hecate.ui;

import com.jme3.app.SimpleApplication;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Sphere;

/**
 * 顶点高亮显示
 * 在目标顶点位置显示一个小球
 */
public class VertexHighlight {

    private final SimpleApplication app;
    private Geometry highlightGeometry;
    private boolean isVisible = false;

    public VertexHighlight(SimpleApplication app) {
        this.app = app;
        createHighlight();
    }

    /**
     * 创建高亮几何体
     */
    private void createHighlight() {
        // 创建一个小球体
        Sphere sphere = new Sphere(8, 8, 0.15f);
        highlightGeometry = new Geometry("VertexHighlight", sphere);

        // 创建发光材质
        Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", new ColorRGBA(1.0f, 1.0f, 0.0f, 0.8f)); // 黄色半透明
        highlightGeometry.setMaterial(mat);

        // 设置为透明渲染
        highlightGeometry.setQueueBucket(RenderQueue.Bucket.Transparent);

        // 初始隐藏
        highlightGeometry.setCullHint(com.jme3.scene.Spatial.CullHint.Always);

        app.getRootNode().attachChild(highlightGeometry);
    }

    /**
     * 显示高亮在指定位置
     */
    public void show(Vector3f position) {
        if (highlightGeometry != null && position != null) {
            highlightGeometry.setLocalTranslation(position);
            highlightGeometry.setCullHint(com.jme3.scene.Spatial.CullHint.Never);
            isVisible = true;
        }
    }

    /**
     * 隐藏高亮
     */
    public void hide() {
        if (highlightGeometry != null) {
            highlightGeometry.setCullHint(com.jme3.scene.Spatial.CullHint.Always);
            isVisible = false;
        }
    }

    /**
     * 检查是否可见
     */
    public boolean isVisible() {
        return isVisible;
    }

    /**
     * 清理
     */
    public void cleanup() {
        if (highlightGeometry != null) {
            app.getRootNode().detachChild(highlightGeometry);
            highlightGeometry = null;
        }
    }
}
