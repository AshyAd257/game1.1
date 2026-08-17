package com.Hecate.world;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.texture.Texture;

/**
 * 地形材质工厂
 * 创建使用世界坐标着色器的拼布风格材质
 */
public class TerrainMaterialFactory {

    private final AssetManager assetManager;
    private Material dirtMaterial;
    private Material sandMaterial;
    private Material waterMaterial;

    public TerrainMaterialFactory(AssetManager assetManager) {
        this.assetManager = assetManager;
        initializeMaterials();
    }

    /**
     * 初始化所有地形材质
     */
    private void initializeMaterials() {
        // 土材质
        dirtMaterial = createTerrainMaterial(TerrainMaterial.DIRT);

        // 沙材质
        sandMaterial = createTerrainMaterial(TerrainMaterial.SAND);

        // 水材质
        waterMaterial = createTerrainMaterial(TerrainMaterial.WATER);
    }

    /**
     * 创建地形材质
     */
    private Material createTerrainMaterial(TerrainMaterial terrainType) {
        Material mat = new Material(assetManager, "MatDefs/TerrainPatchwork.j3md");

        // 设置材质类型
        switch (terrainType) {
            case DIRT:
                mat.setInt("MaterialType", 0);

                // 加载13张地表纹理（3张dirt + 10张grs）
                String[] textureFiles = {
                    "dirt1.JPG", "dirt2.JPG", "dirt3.JPG",
                    "grs1.JPG", "grs2.JPG", "grs3.JPG", "grs4.JPG", "grs5.JPG",
                    "grs6.JPG", "grs7.JPG", "grs8.JPG", "grs9.JPG", "grs10.JPG"
                };

                String[] textureParams = {
                    "Dirt1Texture", "Dirt2Texture", "Dirt3Texture",
                    "Grs1Texture", "Grs2Texture", "Grs3Texture", "Grs4Texture", "Grs5Texture",
                    "Grs6Texture", "Grs7Texture", "Grs8Texture", "Grs9Texture", "Grs10Texture"
                };

                for (int i = 0; i < textureFiles.length; i++) {
                    try {
                        Texture tex = assetManager.loadTexture("textures/blocks/" + textureFiles[i]);
                        tex.setWrap(Texture.WrapMode.Repeat);
                        mat.setTexture(textureParams[i], tex);
                    } catch (Exception e) {
                        // 纹理加载失败，静默处理
                    }
                }
                break;

            case SAND:
                mat.setInt("MaterialType", 1);
                try {
                    Texture sandTex = assetManager.loadTexture("textures/blocks/sand.png");
                    sandTex.setWrap(Texture.WrapMode.Repeat);
                    mat.setTexture("SandTexture", sandTex);
                } catch (Exception e) {
                    // 纹理加载失败，静默处理
                }
                break;

            case WATER:
                mat.setInt("MaterialType", 2);
                try {
                    Texture waterTex = assetManager.loadTexture("textures/blocks/water.png");
                    waterTex.setWrap(Texture.WrapMode.Repeat);
                    mat.setTexture("WaterTexture", waterTex);
                } catch (Exception e) {
                    // 纹理加载失败，静默处理
                }
                break;

            default:
                mat.setInt("MaterialType", 0);
        }

        // 设置拼布参数（调整为更明显的效果）
        mat.setFloat("PatchScale", 8.0f);           // 拼块大小（增大）
        mat.setFloat("DecorationDensity", 0.6f);    // 装饰密度（增加）
        mat.setFloat("DecorationScale", 12.0f);     // 装饰比例（减小，让装饰更密集）

        // 水流动画参数（初始值）
        mat.setFloat("Time", 0.0f);
        mat.setFloat("WaterFlowSpeed", 0.1f);

        // 配置渲染状态：禁用混合，启用深度测试（确保不透明）
        RenderState rs = mat.getAdditionalRenderState();
        rs.setBlendMode(RenderState.BlendMode.Off);  // 禁用混合
        rs.setDepthTest(true);                        // 启用深度测试
        rs.setDepthWrite(true);                       // 启用深度写入
        rs.setFaceCullMode(RenderState.FaceCullMode.Off);  // 暂时禁用剔除来测试

        return mat;
    }

    /**
     * 根据地形材质类型获取材质
     */
    public Material getMaterial(TerrainMaterial terrainType) {
        switch (terrainType) {
            case DIRT:
                return dirtMaterial;
            case SAND:
                return sandMaterial;
            case WATER:
                return waterMaterial;
            default:
                return dirtMaterial; // 默认返回土材质
        }
    }

    /**
     * 更新水材质的时间参数（用于流动动画）
     */
    public void updateWaterAnimation(float time) {
        if (waterMaterial != null) {
            waterMaterial.setFloat("Time", time);
        }
    }

    /**
     * 设置拼块参数（用于调试和定制）
     */
    public void setPatchParameters(float patchScale, float decorationDensity, float decorationScale) {
        if (dirtMaterial != null) {
            dirtMaterial.setFloat("PatchScale", patchScale);
            dirtMaterial.setFloat("DecorationDensity", decorationDensity);
            dirtMaterial.setFloat("DecorationScale", decorationScale);
        }
        if (sandMaterial != null) {
            sandMaterial.setFloat("PatchScale", patchScale);
            sandMaterial.setFloat("DecorationDensity", decorationDensity);
            sandMaterial.setFloat("DecorationScale", decorationScale);
        }
        if (waterMaterial != null) {
            waterMaterial.setFloat("PatchScale", patchScale);
            waterMaterial.setFloat("DecorationDensity", decorationDensity);
            waterMaterial.setFloat("DecorationScale", decorationScale);
        }
    }
}
