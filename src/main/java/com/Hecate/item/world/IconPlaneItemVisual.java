package com.Hecate.item.world;

import com.Hecate.item.ItemDef;
import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.BillboardControl;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Texture;

/**
 * {@link ItemVisual} 的默认实现：用物品图标贴图做一个始终面向摄像机的平面
 * （类似MC掉落物图标的效果）。这是当前唯一的实现，但不是唯一允许存在的实现——
 * 换成3D模型时新写一个类实现 {@link ItemVisual} 即可，不需要改这个类或调用方之外的任何东西。
 */
public class IconPlaneItemVisual implements ItemVisual {
    // 地面图标平面的边长（世界单位/米），比背包UI里的图标小一些，避免铺满整个方块格子
    private static final float PLANE_SIZE = 0.4f;

    private final Geometry geometry;

    public IconPlaneItemVisual(AssetManager assetManager, ItemDef def) {
        Quad quad = new Quad(PLANE_SIZE, PLANE_SIZE);
        this.geometry = new Geometry("WorldItem_" + def.getId(), quad);

        Material material = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        if (def.getIconPath() != null) {
            Texture texture = assetManager.loadTexture(def.getIconPath());
            texture.setMagFilter(Texture.MagFilter.Nearest);
            texture.setMinFilter(Texture.MinFilter.NearestNoMipMaps);
            material.setTexture("ColorMap", texture);
        } else {
            // 没有图标贴图的物品（如尚未画图标的武器）用一块纯色占位，不阻断掉落物生成
            material.setColor("Color", ColorRGBA.Magenta);
        }
        material.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        material.setTransparent(true);
        material.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);
        geometry.setMaterial(material);

        // 始终面向摄像机（不随视角旋转变形，与MC掉落物图标的效果一致）
        geometry.addControl(new BillboardControl());
    }

    @Override
    public Spatial getSpatial() {
        return geometry;
    }

    @Override
    public void setPosition(Vector3f position) {
        geometry.setLocalTranslation(position);
    }

    @Override
    public void dispose() {
        if (geometry.getParent() != null) {
            geometry.removeFromParent();
        }
    }
}
