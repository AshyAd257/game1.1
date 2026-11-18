package com.Hecate.puppet.volume;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.*;
import com.jme3.scene.Geometry;
import com.jme3.texture.*;
import com.jme3.texture.image.ColorSpace;
import com.jme3.texture.plugins.AWTLoader;
import com.Hecate.puppet.core.RotationStripTextureUtil;
import java.io.File;
import java.util.Objects;
import javax.imageio.ImageIO;

/** Shared unlit/normal-lit sprite material. Normal RGB encodes part-local XYZ. */
public final class VolumeAppearance {
    private final AssetManager assets;
    private final Material material;
    private String loadedPath;
    private boolean loadedStrip;
    private Texture normal;
    private String error;
    private boolean invalidated=true;
    public VolumeAppearance(AssetManager assets) {
        this.assets=assets;
        material=new Material(assets,"MatDefs/PuppetVolume.j3md");
        material.setColor("Color",ColorRGBA.White);
        material.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        material.getAdditionalRenderState().setDepthWrite(true);
        material.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);
    }
    public void invalidate() {
        invalidated=true;
        RotationStripTextureUtil.invalidate(assets, loadedPath);
    }
    public String getError() { return error; }
    public void inheritStyle(Material original, String tintParameter) {
        com.jme3.material.MatParam tint=original.getParam(tintParameter);
        material.setColor("Color",tint==null?ColorRGBA.White:(ColorRGBA)tint.getValue());
        RenderState state=original.getAdditionalRenderState();
        material.getAdditionalRenderState().setPolyOffset(state.getPolyOffsetFactor(),state.getPolyOffsetUnits());
    }
    public void update(Geometry geometry,Texture color,VolumeDefinition d,Quaternion partWorldRotation,boolean strip) {
        if(invalidated || !Objects.equals(loadedPath,d.getNormalTexturePath()) || loadedStrip!=strip) {
            loadedPath=d.getNormalTexturePath();loadedStrip=strip;normal=null;error=null;invalidated=false;
            if(loadedPath!=null) try {
                if(strip) {
                    // The legacy strip renderer currently uses a 36-pixel cycle.
                    RotationStripTextureUtil.RingStrip ring=RotationStripTextureUtil.getOrCreateRingStrip(assets,loadedPath,36);
                    if(ring==null) throw new IllegalArgumentException("无法加载条带法线图");
                    normal=ring.texture;
                } else if(new File(loadedPath).isAbsolute()) {
                    java.awt.image.BufferedImage image=ImageIO.read(new File(loadedPath));
                    if(image==null)throw new IllegalArgumentException("无法读取法线图片");
                    normal=new Texture2D(new AWTLoader().load(image,true));
                } else {
                    com.jme3.asset.TextureKey key=new com.jme3.asset.TextureKey(loadedPath,true);
                    assets.deleteFromCache(key); normal=assets.loadTexture(key);
                }
                // Do not change the color space of an image also cached as a color texture.
                normal = normal.clone();
                normal.setImage(normal.getImage().clone());
                normal.getImage().setColorSpace(ColorSpace.Linear);
                normal.setMagFilter(Texture.MagFilter.Nearest);normal.setMinFilter(Texture.MinFilter.NearestNoMipMaps);
            } catch(Exception e) { normal=null;error="法线加载失败："+e.getMessage(); }
        }
        if(color!=null)material.setTexture("ColorMap",color);else material.clearParam("ColorMap");
        boolean match=normal!=null && (color==null || (normal.getImage().getWidth()==color.getImage().getWidth() && normal.getImage().getHeight()==color.getImage().getHeight()));
        if(match)material.setTexture("NormalMap",normal);else material.clearParam("NormalMap");
        if(normal!=null&&!match)error="颜色图和法线图尺寸不一致；当前使用基础法线";
        if(match)error=null;
        material.setBoolean("UseLighting",d.isLightingEnabled());
        material.setParam("PartRotation",com.jme3.shader.VarType.Matrix3,partWorldRotation.toRotationMatrix());
        float yaw=d.getLightYaw()*FastMath.DEG_TO_RAD, elevation=d.getLightElevation()*FastMath.DEG_TO_RAD;
        material.setVector3("LightDirection",new Vector3f(FastMath.sin(yaw)*FastMath.cos(elevation),FastMath.sin(elevation),FastMath.cos(yaw)*FastMath.cos(elevation)));
        material.setFloat("LightStrength",d.getLightStrength());
        geometry.setMaterial(material);
    }
}
