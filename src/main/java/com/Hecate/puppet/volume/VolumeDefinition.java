package com.Hecate.puppet.volume;

/** Shared editor/runtime data. Dimensions are art pixels, not screen pixels. */
public final class VolumeDefinition {
    private boolean skeletonOnly;
    public boolean isSkeletonOnly(){return skeletonOnly;}
    public void setSkeletonOnly(boolean value){skeletonOnly=value;}
    private com.Hecate.puppet.pixel.ImportedPixelMesh importedMesh;
    public com.Hecate.puppet.pixel.ImportedPixelMesh getImportedMesh(){return importedMesh;}
    public void setImportedMesh(com.Hecate.puppet.pixel.ImportedPixelMesh value){importedMesh=value;}
    private com.Hecate.puppet.pixel.BakedPixelDefinition bakedPixels = new com.Hecate.puppet.pixel.BakedPixelDefinition();
    public com.Hecate.puppet.pixel.BakedPixelDefinition getBakedPixels(){if(bakedPixels==null)bakedPixels=new com.Hecate.puppet.pixel.BakedPixelDefinition();return bakedPixels;}
    public boolean isBakedPixelsEnabled(){return bakedPixels!=null&&bakedPixels.isEnabled();}
    public enum Shape { CYLINDER, FRUSTUM, CONE, BOX }
    private Strip3DDefinition strip3D = new Strip3DDefinition();
    public Strip3DDefinition getStrip3D(){if(strip3D==null)strip3D=new Strip3DDefinition();return strip3D;}
    public boolean isStrip3DEnabled(){return strip3D!=null&&strip3D.isEnabled();}
    private Shape shape = Shape.CYLINDER;
    private boolean surfaceDepthEnabled = true;
    private boolean snapSurfaceContour = true;
    public boolean isSurfaceContourSnapped(){return snapSurfaceContour;}
    public void setSurfaceContourSnapped(boolean value){snapSurfaceContour=value;}
    public boolean isSurfaceDepthEnabled(){return surfaceDepthEnabled;}
    public void setSurfaceDepthEnabled(boolean value){surfaceDepthEnabled=value;}
    private int widthPx = 32, heightPx = 64, depthPx = 32;
    private float topRatio = 0.5f;
    private float pixelsPerUnit = 32f;
    private String normalTexturePath;
    // The named part lies inside this part. Independent of the animation/bone hierarchy.
    private String coverTarget;
    public String getCoverTarget(){return coverTarget;}
    public void setCoverTarget(String name){coverTarget=name==null||name.isBlank()?null:name;}
    private boolean lightingEnabled;
    private float lightYaw = -35f, lightElevation = 45f, lightStrength = 0.8f;

    public VolumeDefinition copy() {
        VolumeDefinition d = new VolumeDefinition();
        d.bakedPixels = getBakedPixels().copy();
        d.importedMesh = importedMesh;
        d.skeletonOnly = skeletonOnly;
        d.strip3D = getStrip3D().copy();d.coverTarget=coverTarget;
        d.snapSurfaceContour=snapSurfaceContour;d.surfaceDepthEnabled=surfaceDepthEnabled;d.shape = shape; d.widthPx = widthPx; d.heightPx = heightPx; d.depthPx = depthPx;
        d.topRatio = topRatio; d.pixelsPerUnit = pixelsPerUnit;
        d.normalTexturePath = normalTexturePath; d.lightingEnabled = lightingEnabled;
        d.lightYaw = lightYaw; d.lightElevation = lightElevation; d.lightStrength = lightStrength;
        d.validate();
        return d;
    }
    public void validate() {
        if (shape == null) throw new IllegalArgumentException("请选择实体形状");
        checkPixels(widthPx); checkPixels(heightPx); checkPixels(depthPx);
        if (!Float.isFinite(pixelsPerUnit) || pixelsPerUnit <= 0 || pixelsPerUnit > 4096)
            throw new IllegalArgumentException("像素比例必须在 0 到 4096 之间");
        check(topRatio, 0, 1, "顶部比例");
        check(lightYaw, -180, 180, "灯光水平角");
        check(lightElevation, -90, 90, "灯光仰角");
        check(lightStrength, 0, 2, "灯光强度");
    }
    private static void checkPixels(int value) {
        if (value < 1 || value > 4096) throw new IllegalArgumentException("尺寸必须是 1–4096 的整数像素");
    }
    private static void check(float v, float min, float max, String label) {
        if (!Float.isFinite(v) || v < min || v > max) throw new IllegalArgumentException(label + "超出范围");
    }
    public Shape getShape() { return shape; }
    public void setShape(Shape value) { shape = value; }
    public int getWidthPx() { return widthPx; }
    public int getHeightPx() { return heightPx; }
    public int getDepthPx() { return depthPx; }
    public void setDimensions(int w, int h, int d) { checkPixels(w); checkPixels(h); checkPixels(d); widthPx=w; heightPx=h; depthPx=d; }
    public float getTopRatio() { return topRatio; }
    public void setTopRatio(float value) { check(value,0,1,"顶部比例"); topRatio=value; }
    public float effectiveTopRatio() { return shape == Shape.CONE ? 0 : shape == Shape.FRUSTUM ? topRatio : 1; }
    public float getPixelsPerUnit() { return pixelsPerUnit; }
    public void setPixelsPerUnit(float value) { if (!Float.isFinite(value) || value <= 0 || value > 4096) throw new IllegalArgumentException("像素比例无效"); pixelsPerUnit=value; }
    public float width() { return widthPx/pixelsPerUnit; }
    public float height() { return heightPx/pixelsPerUnit; }
    public float depth() { return depthPx/pixelsPerUnit; }
    public String getNormalTexturePath() { return normalTexturePath; }
    public void setNormalTexturePath(String value) { normalTexturePath = value == null || value.isBlank() ? null : value; }
    public boolean isLightingEnabled() { return lightingEnabled; }
    public void setLightingEnabled(boolean value) { lightingEnabled=value; }
    public float getLightYaw() { return lightYaw; }
    public float getLightElevation() { return lightElevation; }
    public float getLightStrength() { return lightStrength; }
    public void setLight(float yaw, float elevation, float strength) {
        check(yaw,-180,180,"灯光水平角"); check(elevation,-90,90,"灯光仰角"); check(strength,0,2,"灯光强度");
        lightYaw=yaw; lightElevation=elevation; lightStrength=strength;
    }
    public boolean sameGeometry(VolumeDefinition other) {
        return other != null && java.util.Objects.equals(importedMesh==null?null:importedMesh.key(),other.importedMesh==null?null:other.importedMesh.key()) && shape == other.shape && widthPx == other.widthPx && heightPx == other.heightPx
            && depthPx == other.depthPx && topRatio == other.topRatio && pixelsPerUnit == other.pixelsPerUnit;
    }
}
