package com.Hecate.puppet.editor.core;

import com.jme3.app.SimpleApplication;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Texture;

public class EditorPuppetPartRenderer {

    private final SimpleApplication app;
    private final EditorBone bone;
    private final Node parentNode;
    private EditorPuppetRenderer parentRenderer;

    private Geometry partGeometry;
    private Material partMaterial;
    private Quad partQuad;
    private com.jme3.scene.control.BillboardControl billboardControl;

    private Node highlightNode;
    private Geometry topLine, bottomLine, leftLine, rightLine;
    private Material highlightMaterial;
    private boolean isSelected = false;

    private float width;
    private float height;

    private Texture texture;
    private String texturePath;

    private float uvOffsetX = 0.0f;
    private float uvOffsetY = 0.0f;
    private float uvScaleX = 1.0f;
    private float uvScaleY = 1.0f;

    private boolean gridEnabled = false;
    private boolean gridHorizontal = true;
    private boolean gridVertical = true;
    private float gridSize = 32f;
    private boolean snapToGrid = false;

    private boolean initialized = false;

    private final Vector3f offset = new Vector3f(0f, 0f, 0f);

    private final Vector3f pivotPoint = new Vector3f(0f, 0f, 0f);

    private Geometry pivotMarker;
    private boolean showPivotMarker = false;

    private float customRotationX = 0f;
    private float customRotationY = 0f;
    private float customRotationZ = 0f;

    private float textureRotation = 0f;

    private boolean useAnimationRotation = false;

    private final ColorRGBA debugColor;

    // ==================== 3D模型骨骼系统（与core.PuppetPartRenderer保持一致） ====================
    private com.jme3.scene.Spatial modelSpatial;
    private String loadedModelPath;

    private String lastDirection = null;

    // 方向切换冷却时间（秒）- 防止频繁切换
    private static final float DIRECTION_SWITCH_COOLDOWN = 0.1f;
    private float timeSinceLastDirectionSwitch = 0f;

    // 可复用的位置缓冲区数组（避免频繁分配）
    // 四边形顶点：左下、右下、右上、左上，每个3个float (x, y, z)
    private final float[] reusablePositionArray = new float[12];

    public EditorPuppetPartRenderer(SimpleApplication app, EditorBone bone, Node parentNode, float width, float height) {
        this.app = app;
        this.bone = bone;
        this.parentNode = parentNode;
        this.width = width;
        this.height = height;

        this.debugColor = ColorRGBA.randomColor();
    }

    
    public void initialize() {
        if (initialized) {
            return;
        }

        // 3D模型骨骼：完全跳过Quad/Material/billboard/高光/中心点标记，只加载模型
        if (bone.isModelEnabled()) {
            updateModelFromBone();
            initialized = true;
            return;
        }

        partQuad = createCenteredQuad(width, height);
        partGeometry = new Geometry(bone.getName() + "_Part", partQuad);

        partGeometry.setShadowMode(com.jme3.renderer.queue.RenderQueue.ShadowMode.Receive);

        createMaterial();

        setupRenderProperties();

        billboardControl = new com.jme3.scene.control.BillboardControl();
        billboardControl.setAlignment(com.jme3.scene.control.BillboardControl.Alignment.Screen);
        partGeometry.addControl(billboardControl);

        parentNode.attachChild(partGeometry);

        createHighlight();

        createPivotMarker();

        updateTextureFromBone();

        if (bone.isRotationStripEnabled()) {
            bone.setStripWidth(width);
            bone.setStripHeight(height);
        } else {
            // 只有当bone的当前方向没有宽度/高度时，才保存默认值
            // 这样可以确保：
            // 1. 新建部件时，front方向有一个初始值作为其他方向的继承源
            // 2. 加载已有模型时，不会覆盖文件中的值
            if (bone.getDirectionWidth(bone.getCurrentDirection()) == null) {
                bone.setDirectionWidth(bone.getCurrentDirection(), width);
            }
            if (bone.getDirectionHeight(bone.getCurrentDirection()) == null) {
                bone.setDirectionHeight(bone.getCurrentDirection(), height);
            }
        }

        initialized = true;
    }

    /**
     * 从Bone加载3D模型（仅modelEnabled=true时使用），与core.PuppetPartRenderer同款逻辑
     */
    private void updateModelFromBone() {
        String modelPath = bone.getModelFilePath();
        if (modelPath == null || modelPath.isEmpty()) {
            return;
        }

        if (modelSpatial != null && modelPath.equals(loadedModelPath)) {
            return;
        }

        if (modelSpatial != null) {
            modelSpatial.removeFromParent();
            modelSpatial = null;
        }

        try {
            modelSpatial = app.getAssetManager().loadModel(modelPath);
            modelSpatial.setName(bone.getName() + "_Model");
            parentNode.attachChild(modelSpatial);
            loadedModelPath = modelPath;
        } catch (Exception e) {
            System.err.println("[EditorPuppetPartRenderer] 加载3D模型失败: " + modelPath);
            System.err.println("[EditorPuppetPartRenderer] 错误信息: " + e.getMessage());
            modelSpatial = null;
            loadedModelPath = null;
        }
    }

    /**
     * 把bone的世界变换套到模型Spatial上（仅modelEnabled=true时使用），
     * 与core.PuppetPartRenderer同款逻辑
     */
    private void updateModelTransform() {
        if (modelSpatial == null) {
            return;
        }

        Vector3f worldPos = new Vector3f();
        Quaternion worldRot = new Quaternion();
        Vector3f worldScale = new Vector3f();
        bone.getWorldTransform(worldPos, worldRot, worldScale);

        float rotX = bone.getModelRotationX();
        float rotY = bone.getModelRotationY();
        float rotZ = bone.getModelRotationZ();
        Quaternion finalRot = worldRot;
        if (rotX != 0f || rotY != 0f || rotZ != 0f) {
            Quaternion modelRot = new Quaternion().fromAngles(
                rotX * FastMath.DEG_TO_RAD,
                rotY * FastMath.DEG_TO_RAD,
                rotZ * FastMath.DEG_TO_RAD
            );
            finalRot = worldRot.mult(modelRot);
        }

        modelSpatial.setLocalTranslation(worldPos);
        modelSpatial.setLocalRotation(finalRot);
        modelSpatial.setLocalScale(worldScale.mult(bone.getModelScale()));
    }

    private void createMaterial() {
        partMaterial = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        partMaterial.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        partMaterial.setTransparent(true);
        partMaterial.setColor("Color", ColorRGBA.White);
        // 降低到0.1以支持半透明显示（10%以下的透明度会被丢弃，避免幽灵影子）
        partMaterial.setFloat("AlphaDiscardThreshold", 0.1f);
        partGeometry.setMaterial(partMaterial);
    }

    private void setupRenderProperties() {
        // 使用 Transparent 渲染队列，支持透明度混合
        partGeometry.setQueueBucket(RenderQueue.Bucket.Transparent);

        partGeometry.setShadowMode(RenderQueue.ShadowMode.Receive);

        // 启用深度写入和深度测试
        partMaterial.getAdditionalRenderState().setDepthWrite(true);
        partMaterial.getAdditionalRenderState().setDepthTest(true);

        partMaterial.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Back);

        partMaterial.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);

        // 使用 PolyOffset 来控制渲染顺序，避免 Z-fighting
        // 负值会让几何体更靠近相机（渲染在前面）
        // 我们将根据优先级动态设置这个值
        partMaterial.getAdditionalRenderState().setPolyOffset(0f, 0f);
    }

    
public void loadTexture(String texturePath) {
        if (texturePath != null && texturePath.equals(this.texturePath) && texture != null) {
            return;
        }

        try {
            if (texturePath.startsWith("file:///") || new java.io.File(texturePath).isAbsolute()) {
                String filePath = texturePath;
                if (filePath.startsWith("file:///")) {
                    filePath = filePath.substring(8);
                }

                java.io.File imageFile = new java.io.File(filePath);
                if (!imageFile.exists()) {
                    throw new java.io.FileNotFoundException("File not found: " + filePath);
                }

                java.awt.image.BufferedImage bufferedImage = javax.imageio.ImageIO.read(imageFile);
                if (bufferedImage == null) {
                    throw new java.io.IOException("Cannot read image: " + filePath);
                }

                com.jme3.texture.Image jmeImage = convertBufferedImageToJmeImage(bufferedImage);

                texture = new com.jme3.texture.Texture2D(jmeImage);
                texture.setMagFilter(Texture.MagFilter.Nearest);
                texture.setMinFilter(Texture.MinFilter.NearestNoMipMaps);
                texture.setWrap(Texture.WrapMode.Clamp);

            } else {

                texture = app.getAssetManager().loadTexture(texturePath);
                texture.setMagFilter(Texture.MagFilter.Nearest);
                texture.setMinFilter(Texture.MinFilter.NearestNoMipMaps);

            }

            partMaterial.setTexture("ColorMap", texture);
            partMaterial.setColor("Color", ColorRGBA.White);
            this.texturePath = texturePath;

        } catch (Exception e) {

            e.printStackTrace();
            setDebugColor(ColorRGBA.Magenta);
            texture = null;
        }
    }

    
    private com.jme3.texture.Image convertBufferedImageToJmeImage(java.awt.image.BufferedImage bufferedImage) {
        int width = bufferedImage.getWidth();
        int height = bufferedImage.getHeight();

        java.nio.ByteBuffer byteBuffer = java.nio.ByteBuffer.allocateDirect(width * height * 4);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = bufferedImage.getRGB(x, y);

                int alpha = (argb >> 24) & 0xFF;
                int red = (argb >> 16) & 0xFF;
                int green = (argb >> 8) & 0xFF;
                int blue = argb & 0xFF;

                byteBuffer.put((byte) red);
                byteBuffer.put((byte) green);
                byteBuffer.put((byte) blue);
                byteBuffer.put((byte) alpha);
            }
        }

        byteBuffer.flip();

        return new com.jme3.texture.Image(
            com.jme3.texture.Image.Format.RGBA8,
            width,
            height,
            byteBuffer,
            com.jme3.texture.image.ColorSpace.sRGB
        );
    }

    
    public void updateTextureFromBone() {
        // 3D模型骨骼没有partMaterial/partQuad，纹理系统对它完全不适用
        if (bone.isModelEnabled()) {
            updateModelFromBone();
            return;
        }

        // 旋转条状贴图模式：完全跳过6方向系统，改由updateTransform()里的
        // applyRotationStripUV()按相机角度动态取样。这里除了加载贴图，还要恢复
        // 单一的宽高/偏移/旋转（不是按方向存储的，只有一份数据）
        if (bone.isRotationStripEnabled()) {
            String stripPath = bone.getStripTexturePath();
            if (stripPath != null && !stripPath.isEmpty()) {
                loadTexture(stripPath);
            } else {
                setDebugColor(debugColor);
            }

            this.width = bone.getStripWidth();
            this.height = bone.getStripHeight();
            if (partQuad != null) {
                updateReusablePositionArray();
                partQuad.setBuffer(com.jme3.scene.VertexBuffer.Type.Position, 3, reusablePositionArray);
                partQuad.updateBound();
                partQuad.getBuffer(com.jme3.scene.VertexBuffer.Type.Position).setUpdateNeeded();
                updateHighlightBorderSize();
            }

            com.jme3.math.Vector3f stripOffset = bone.getStripOffset();
            this.offset.set(stripOffset);

            this.customRotationX = bone.getStripRotationX();
            this.customRotationY = bone.getStripRotationY();
            this.customRotationZ = bone.getStripRotationZ();

            return;
        }

        String textureToLoad = bone.getCurrentDirectionTexture();

        if (textureToLoad != null && !textureToLoad.isEmpty()) {
            loadTexture(textureToLoad);
        } else {

            setDebugColor(debugColor);
        }

        float[] directionUV = bone.getCurrentDirectionUV();
        if (directionUV != null && directionUV.length == 4) {

            this.uvOffsetX = directionUV[0];
            this.uvOffsetY = directionUV[1];
            this.uvScaleX = directionUV[2];
            this.uvScaleY = directionUV[3];
            updateTexCoords();
        }

        Float dirWidth = bone.getCurrentDirectionWidth();
        Float dirHeight = bone.getCurrentDirectionHeight();
        if (dirWidth != null) {
            this.width = dirWidth;
            if (partQuad != null) {
                // 使用可复用数组更新位置缓冲区
                updateReusablePositionArray();
                partQuad.setBuffer(com.jme3.scene.VertexBuffer.Type.Position, 3, reusablePositionArray);
                partQuad.updateBound();
                partQuad.getBuffer(com.jme3.scene.VertexBuffer.Type.Position).setUpdateNeeded();
                updateHighlightBorderSize();
            }
        }
        if (dirHeight != null) {
            this.height = dirHeight;
            if (partQuad != null) {
                // 使用可复用数组更新位置缓冲区
                updateReusablePositionArray();
                partQuad.setBuffer(com.jme3.scene.VertexBuffer.Type.Position, 3, reusablePositionArray);
                partQuad.updateBound();
                partQuad.getBuffer(com.jme3.scene.VertexBuffer.Type.Position).setUpdateNeeded();
                updateHighlightBorderSize();
            }
        }

        float[] dirOffset = bone.getCurrentDirectionOffset();
        if (dirOffset != null && dirOffset.length == 3) {
            this.offset.set(dirOffset[0], dirOffset[1], dirOffset[2]);
        }

        float[] dirRotation = bone.getCurrentDirectionRotation();
        if (dirRotation != null && dirRotation.length >= 3) {
            this.customRotationX = dirRotation[0];
            this.customRotationY = dirRotation[1];
            this.customRotationZ = dirRotation[2];
        } else if (dirRotation != null && dirRotation.length == 2) {

            this.customRotationX = dirRotation[0];
            this.customRotationY = 0f;
            this.customRotationZ = dirRotation[1];
        }

    }

    public void setDirection(EditorBone.Direction direction) {
        // Note: No need to save properties here because setter methods
        // (setWidth, setHeight, etc.) already save to bone's direction map
        // when user modifies values via UI

        bone.setCurrentDirection(direction.getKey());

        updateTextureFromBone();
    }

    
    public void setDirection(String directionKey) {

        saveCurrentDirectionProperties();

        bone.setCurrentDirection(directionKey);

        updateTextureFromBone();
    }

    
    private void saveCurrentDirectionProperties() {
        String currentDirection = bone.getCurrentDirection();

        // 只保存当前方向已经有值的属性
        // 如果当前方向没有值，说明是继承来的，不应该保存（避免"固化"继承值）

        // UV坐标：如果当前方向有UV设置，才保存
        if (bone.hasDirectionUV(currentDirection)) {
            bone.setDirectionUV(currentDirection, uvOffsetX, uvOffsetY, uvScaleX, uvScaleY);
        }

        // 宽度：如果当前方向有宽度设置，才保存
        if (bone.getDirectionWidth(currentDirection) != null) {
            bone.setDirectionWidth(currentDirection, width);
        }

        // 高度：如果当前方向有高度设置，才保存
        if (bone.getDirectionHeight(currentDirection) != null) {
            bone.setDirectionHeight(currentDirection, height);
        }

        // 位置偏移：如果当前方向有偏移设置，才保存
        if (bone.getDirectionOffset(currentDirection) != null) {
            bone.setDirectionOffset(currentDirection, offset.x, offset.y, offset.z);
        }

        // 旋转：如果当前方向有旋转设置，才保存
        if (bone.getDirectionRotation(currentDirection) != null) {
            bone.setDirectionRotation(currentDirection, customRotationX, customRotationY, customRotationZ);
        }

        // 优先级：如果当前方向有优先级设置，才保存
        if (bone.hasDirectionPriority(currentDirection)) {
            bone.setDirectionPriority(currentDirection, bone.getPriority());
        }
    }

    
    private void saveCurrentDirectionUV() {
        String currentDirection = bone.getCurrentDirection();
        bone.setDirectionUV(currentDirection, uvOffsetX, uvOffsetY, uvScaleX, uvScaleY);
    }

    
    public String getCurrentDirection() {
        return bone.getCurrentDirection();
    }

    
    public String getTexturePath() {
        return texturePath;
    }

    
    public Texture getTexture() {
        return texture;
    }

    
    public void setDebugColor(ColorRGBA color) {
        if (partMaterial == null) {
            return;
        }
        partMaterial.clearParam("ColorMap");
        partMaterial.setColor("Color", color);
    }

    public void updateTransform(float tpf) {
        if (!initialized) {
            return;
        }

        // 3D模型骨骼：固定朝向，不走billboard/高光/优先级分层这套2D纸片逻辑，
        // 也不受parentRenderer是否就绪影响（不需要统一billboard参数）
        if (bone.isModelEnabled()) {
            updateModelTransform();
            return;
        }

        // 【修复】如果 parentRenderer 还未设置，跳过更新
        // 这在初始化阶段可能发生，因为 setParentRenderer() 在 initialize() 之后才调用
        if (parentRenderer == null) {
            return;
        }

        // 更新方向切换冷却计时器
        timeSinceLastDirectionSwitch += tpf;

        // 检测方向变化并同步所有属性（带冷却防抖）
        String detectedDirection = bone.getCurrentDirection();
        if (lastDirection == null || !lastDirection.equals(detectedDirection)) {
            // 只有在冷却时间到了才允许方向切换
            if (timeSinceLastDirectionSwitch >= DIRECTION_SWITCH_COOLDOWN) {
                updateTextureFromBone();
                lastDirection = detectedDirection;
                timeSinceLastDirectionSwitch = 0f; // 重置计时器
            }
        }

        Vector3f worldPos = new Vector3f();
        Quaternion worldRot = new Quaternion();
        Vector3f worldScale = new Vector3f();
        bone.getWorldTransform(worldPos, worldRot, worldScale);

        // 旋转条状贴图模式：按相机水平角度动态取样UV（只影响UV，宽高/位置/billboard走原有逻辑）
        if (bone.isRotationStripEnabled()) {
            applyRotationStripUV(worldPos);
        }

        // 旋转条状贴图模式：读单一的stripWidth/stripHeight，不走6方向的Map
        Float dirWidth = bone.isRotationStripEnabled() ? bone.getStripWidth() : bone.getCurrentDirectionWidth();
        Float dirHeight = bone.isRotationStripEnabled() ? bone.getStripHeight() : bone.getCurrentDirectionHeight();

        if (dirWidth != null && Math.abs(dirWidth - width) > 0.001f) {
            this.width = dirWidth;
            if (partQuad != null) {
                // 使用可复用数组更新位置缓冲区
                updateReusablePositionArray();
                partQuad.setBuffer(com.jme3.scene.VertexBuffer.Type.Position, 3, reusablePositionArray);
                partQuad.updateBound();
                partQuad.getBuffer(com.jme3.scene.VertexBuffer.Type.Position).setUpdateNeeded();
                updateHighlightBorderSize();
            }
        }
        if (dirHeight != null && Math.abs(dirHeight - height) > 0.001f) {
            this.height = dirHeight;
            if (partQuad != null) {
                // 使用可复用数组更新位置缓冲区
                updateReusablePositionArray();
                partQuad.setBuffer(com.jme3.scene.VertexBuffer.Type.Position, 3, reusablePositionArray);
                partQuad.updateBound();
                partQuad.getBuffer(com.jme3.scene.VertexBuffer.Type.Position).setUpdateNeeded();
                updateHighlightBorderSize();
            }
        }

        float[] dirRotation = bone.isRotationStripEnabled()
            ? new float[]{bone.getStripRotationX(), bone.getStripRotationY(), bone.getStripRotationZ()}
            : bone.getCurrentDirectionRotation();
        float currentRotX = customRotationX;
        float currentRotY = customRotationY;
        float currentRotZ = customRotationZ;

        if (dirRotation != null) {
            if (dirRotation.length >= 3) {
                currentRotX = dirRotation[0];
                currentRotY = dirRotation[1];
                currentRotZ = dirRotation[2];
            } else if (dirRotation.length == 2) {

                currentRotX = dirRotation[0];
                currentRotY = 0f;
                currentRotZ = dirRotation[1];
            }
        }

        Quaternion customRot = new Quaternion();
        boolean hasCustomRotation = (currentRotX != 0f || currentRotY != 0f || currentRotZ != 0f);

        if (hasCustomRotation) {

            Vector3f localAxisX = worldRot.mult(Vector3f.UNIT_X);
            Vector3f localAxisY = worldRot.mult(Vector3f.UNIT_Y);
            Vector3f localAxisZ = worldRot.mult(Vector3f.UNIT_Z);

            Quaternion rotX = new Quaternion();
            if (currentRotX != 0f) {
                float radiansX = currentRotX * com.jme3.math.FastMath.DEG_TO_RAD;
                rotX.fromAngleAxis(radiansX, localAxisX);
            }

            Quaternion rotY = new Quaternion();
            if (currentRotY != 0f) {
                float radiansY = currentRotY * com.jme3.math.FastMath.DEG_TO_RAD;
                rotY.fromAngleAxis(radiansY, localAxisY);
            }

            Quaternion rotZ = new Quaternion();
            if (currentRotZ != 0f) {
                float radiansZ = currentRotZ * com.jme3.math.FastMath.DEG_TO_RAD;
                rotZ.fromAngleAxis(radiansZ, localAxisZ);
            }

            customRot = rotX.mult(rotY).mult(rotZ);
        }

        Quaternion finalRot = customRot.mult(worldRot);

        Vector3f finalPos = worldPos.clone();

        if (parentRenderer != null) {
            float manualRotation = parentRenderer.getManualRotationAngle();
            if (Math.abs(manualRotation) > 0.001f) {

                Vector3f boneOffset = worldPos.clone();

                Quaternion rotation = new Quaternion();
                rotation.fromAngleAxis(manualRotation, Vector3f.UNIT_Y);

                Vector3f rotatedOffset = rotation.mult(boneOffset);

                finalPos = rotatedOffset;

                worldRot = rotation.mult(worldRot);
            }
        }

        float[] dirOffset = bone.isRotationStripEnabled()
            ? new float[]{bone.getStripOffset().x, bone.getStripOffset().y, bone.getStripOffset().z}
            : bone.getCurrentDirectionOffset();
        Vector3f currentOffset = (dirOffset != null) ?
            new Vector3f(dirOffset[0], dirOffset[1], dirOffset[2]) : offset;

        if (!currentOffset.equals(Vector3f.ZERO)) {
            Vector3f rotatedOffset = worldRot.mult(currentOffset);
            finalPos.addLocal(rotatedOffset);
        }

        float[] contentCenter = bone.getCurrentDirectionContentCenter();
        if (contentCenter != null && (contentCenter[0] != 0f || contentCenter[1] != 0f)) {

            Vector3f contentOffset = new Vector3f(
                contentCenter[0] * width,
                contentCenter[1] * height,
                0f
            );

            Vector3f rotatedContentOffset = worldRot.mult(contentOffset);
            finalPos.addLocal(rotatedContentOffset);
        }

        int currentPriority = bone.isRotationStripEnabled() ? bone.getStripPriority() : bone.getCurrentDirectionPriority();

        Vector3f offsetDirection;

        boolean isBillboardEnabled = bone.isBillboardEnabled();
        EditorPuppetRenderer.BillboardMode currentBillboardMode = (parentRenderer != null) ?
            parentRenderer.getBillboardMode() : EditorPuppetRenderer.BillboardMode.UNIFIED;

        if (isBillboardEnabled && currentBillboardMode == EditorPuppetRenderer.BillboardMode.UNIFIED) {

            Quaternion baseBillboardRot = parentRenderer.getUnifiedBillboardRotation();
            offsetDirection = baseBillboardRot.mult(Vector3f.UNIT_Z);
        } else {

            offsetDirection = worldRot.mult(Vector3f.UNIT_Z);
        }

        // 根据优先级计算深度偏移
        // 优先级越高，越靠近相机（Z值越小）
        // 每个优先级单位对应 0.01 的深度偏移
        float offsetAmount = currentPriority * 0.01f;

        // 将深度偏移应用到最终位置
        // offsetDirection 指向相机方向，所以减去偏移量会让部件更靠近相机
        finalPos.addLocal(offsetDirection.mult(-offsetAmount));

        

        float cameraFollowFreedomX = bone.getCameraFollowFreedomX();
        float cameraFollowFreedomY = bone.getCameraFollowFreedomY();

        if ((cameraFollowFreedomX > 0.001f || cameraFollowFreedomY > 0.001f) && parentRenderer != null) {
            Vector3f camPos = app.getCamera().getLocation();
            Vector3f puppetCenter = parentRenderer.getPuppetNode().getWorldTranslation();

            Vector3f toCam = camPos.subtract(puppetCenter);
            float distance = toCam.length();

            if (distance > 0.001f) {
                toCam.normalizeLocal();

                Vector3f camHorizontal = new Vector3f(toCam.x, 0, toCam.z);
                float horizontalDist = camHorizontal.length();

                if (horizontalDist > 0.001f) {
                    camHorizontal.normalizeLocal();

                    float horizontalFactor = camHorizontal.x;

                    Vector3f horizontalOffset = new Vector3f(
                        horizontalFactor * cameraFollowFreedomX * 2.0f,
                        0,
                        0
                    );

                    finalPos.addLocal(horizontalOffset);
                }

                float verticalFactor = toCam.y;

                Vector3f verticalOffset = new Vector3f(
                    0,
                    verticalFactor * cameraFollowFreedomY * 2.0f,
                    0
                );

                finalPos.addLocal(verticalOffset);
            }
        }

        if (hasCustomRotation && !pivotPoint.equals(Vector3f.ZERO)) {

            Vector3f rotatedPivot = worldRot.mult(pivotPoint);
            Vector3f pivotWorldPos = worldPos.add(rotatedPivot);

            Vector3f toPart = finalPos.subtract(pivotWorldPos);

            Vector3f rotatedToPart = customRot.mult(toPart);

            finalPos = pivotWorldPos.add(rotatedToPart);
        }

        EditorPuppetRenderer.BillboardMode billboardMode = (parentRenderer != null) ?
            parentRenderer.getBillboardMode() : EditorPuppetRenderer.BillboardMode.UNIFIED;

        boolean useBillboard = bone.isBillboardEnabled();

        if (hasCustomRotation) {

            billboardControl.setEnabled(false);
            partGeometry.setLocalRotation(finalRot);
        } else if (!useBillboard) {

            billboardControl.setEnabled(false);

            partGeometry.setLocalRotation(worldRot);
        } else if (billboardMode == EditorPuppetRenderer.BillboardMode.DISABLED) {

            billboardControl.setEnabled(false);

            partGeometry.setLocalRotation(worldRot);
        } else if (billboardMode == EditorPuppetRenderer.BillboardMode.UNIFIED) {

            billboardControl.setEnabled(false);

            Quaternion baseBillboardRot = parentRenderer.getUnifiedBillboardRotation();

            String currentDirection = bone.getCurrentDirection();
            Quaternion finalBillboardRot;

            if ("up".equals(currentDirection) || "down".equals(currentDirection)) {

                Vector3f camPos = app.getCamera().getLocation();
                Vector3f partPos = partGeometry.getWorldTranslation();

                Vector3f toCam = camPos.subtract(partPos);
                if (toCam.lengthSquared() < 0.0001f) {
                    finalBillboardRot = baseBillboardRot;
                } else {
                    toCam.normalizeLocal();

                    Vector3f up;
                    if ("up".equals(currentDirection)) {

                        up = Vector3f.UNIT_Y.negate();
                    } else {

                        up = Vector3f.UNIT_Y;
                    }

                    Vector3f left = up.cross(toCam);
                    if (left.lengthSquared() < 0.0001f) {

                        left = Vector3f.UNIT_X;
                    } else {
                        left.normalizeLocal();
                    }
                    Vector3f realUp = toCam.cross(left).normalizeLocal();

                    finalBillboardRot = new Quaternion();
                    finalBillboardRot.fromAxes(left, realUp, toCam);
                }
            } else {

                finalBillboardRot = baseBillboardRot;
            }

            partGeometry.setLocalRotation(finalBillboardRot);
        } else {

            billboardControl.setEnabled(true);

        }

        partGeometry.setLocalTranslation(finalPos);
        partGeometry.setLocalScale(worldScale);

        updateHighlightTransform(finalPos, finalRot, worldScale, hasCustomRotation);

        updatePivotMarkerPosition();
    }

    
    private void updateHighlightTransform(Vector3f position, Quaternion rotation, Vector3f scale, boolean hasCustomRotation) {
        if (!initialized || highlightNode == null) {
            return;
        }

        highlightNode.setLocalTranslation(position);
        highlightNode.setLocalScale(scale);

        com.jme3.scene.control.BillboardControl highlightBillboard =
            highlightNode.getControl(com.jme3.scene.control.BillboardControl.class);

        if (highlightBillboard != null) {
            if (hasCustomRotation) {
                highlightBillboard.setEnabled(false);
                highlightNode.setLocalRotation(rotation);
            } else {
                highlightBillboard.setEnabled(true);
            }
        }
    }

    
    public void setSelected(boolean selected) {
        this.isSelected = selected;
        if (highlightNode != null) {
            highlightNode.setCullHint(selected ?
                Geometry.CullHint.Never :
                Geometry.CullHint.Always);
        }
    }

    
    public boolean isSelected() {
        return isSelected;
    }

    
    public void setVisible(boolean visible) {
        if (initialized) {
            if (partGeometry != null) {
                partGeometry.setCullHint(visible ?
                        Geometry.CullHint.Never :
                        Geometry.CullHint.Always);
            }

            // 3D模型骨骼：直接控制模型Spatial的显隐
            if (modelSpatial != null) {
                modelSpatial.setCullHint(visible ?
                        Geometry.CullHint.Never :
                        Geometry.CullHint.Always);
            }

            if (highlightNode != null) {
                highlightNode.setCullHint((visible && isSelected) ?
                    Geometry.CullHint.Never :
                    Geometry.CullHint.Always);
            }

            if (pivotMarker != null) {
                pivotMarker.setCullHint(visible ?
                    (showPivotMarker ? Geometry.CullHint.Never : Geometry.CullHint.Always) :
                    Geometry.CullHint.Always);
            }
        }
    }


    public void cleanup() {

        if (partGeometry != null) {
            partGeometry.removeFromParent();

        }
        if (modelSpatial != null) {
            modelSpatial.removeFromParent();
        }
        if (highlightNode != null) {

            highlightNode.setCullHint(Spatial.CullHint.Always);

            highlightNode.detachAllChildren();

            highlightNode.removeFromParent();

        }
        if (pivotMarker != null) {
            pivotMarker.removeFromParent();

        }

        partGeometry = null;
        modelSpatial = null;
        loadedModelPath = null;
        highlightNode = null;
        pivotMarker = null;

        initialized = false;
    }

    /**
     * 更新可复用的位置数组缓冲区
     * 避免每次尺寸变化时都分配新数组
     */
    private void updateReusablePositionArray() {
        // 左下角
        reusablePositionArray[0] = -width / 2;
        reusablePositionArray[1] = -height / 2;
        reusablePositionArray[2] = 0;
        // 右下角
        reusablePositionArray[3] = width / 2;
        reusablePositionArray[4] = -height / 2;
        reusablePositionArray[5] = 0;
        // 右上角
        reusablePositionArray[6] = width / 2;
        reusablePositionArray[7] = height / 2;
        reusablePositionArray[8] = 0;
        // 左上角
        reusablePositionArray[9] = -width / 2;
        reusablePositionArray[10] = height / 2;
        reusablePositionArray[11] = 0;
    }

    public void setSize(float newWidth, float newHeight) {
        if (!initialized) {
            return;
        }

        this.width = newWidth;
        this.height = newHeight;

        // 使用可复用数组更新位置缓冲区
        updateReusablePositionArray();
        partQuad.setBuffer(com.jme3.scene.VertexBuffer.Type.Position, 3, reusablePositionArray);

        partQuad.updateBound();
        partQuad.getBuffer(com.jme3.scene.VertexBuffer.Type.Position).setUpdateNeeded();

        updateHighlightBorderSize();
    }

    
    public void adjustWidth(float delta) {
        setSize(Math.max(0.1f, width + delta), height);
    }

    
    public void adjustHeight(float delta) {
        setSize(width, Math.max(0.1f, height + delta));
    }

    public EditorBone getBone() {
        return bone;
    }

    /**
     * @return 部件的Quad几何体。3D模型骨骼没有Quad，返回null（3D射线选中暂不支持模型骨骼，
     *         这类骨骼仍可以通过部件列表面板按名字选中——见PartListPanel）
     */
    public Geometry getGeometry() {
        return partGeometry;
    }

    /**
     * @return 3D模型骨骼加载的模型Spatial，非模型骨骼返回null
     */
    public com.jme3.scene.Spatial getModelSpatial() {
        return modelSpatial;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public float getWidth() {
        // 旋转条状贴图模式：读单一的stripWidth，不走6方向的继承逻辑
        if (bone.isRotationStripEnabled()) {
            return bone.getStripWidth();
        }
        // 优先返回EditorBone的当前方向宽度（包含继承逻辑）
        Float dirWidth = bone.getCurrentDirectionWidth();
        if (dirWidth != null) {
            return dirWidth;
        }
        // 如果没有设置任何方向的宽度，返回本地默认值
        return width;
    }

    public void setWidth(float width) {
        this.width = width;

        if (partQuad != null) {
            // 使用可复用数组更新位置缓冲区
            updateReusablePositionArray();
            partQuad.setBuffer(com.jme3.scene.VertexBuffer.Type.Position, 3, reusablePositionArray);
            partQuad.updateBound();
            // 强制标记缓冲区为已修改，确保渲染更新
            partQuad.getBuffer(com.jme3.scene.VertexBuffer.Type.Position).setUpdateNeeded();
            updateHighlightBorderSize();
        }

        if (bone.isRotationStripEnabled()) {
            bone.setStripWidth(width);
        } else {
            bone.setDirectionWidth(bone.getCurrentDirection(), width);
        }
    }

    public float getHeight() {
        // 旋转条状贴图模式：读单一的stripHeight，不走6方向的继承逻辑
        if (bone.isRotationStripEnabled()) {
            return bone.getStripHeight();
        }
        // 优先返回EditorBone的当前方向高度（包含继承逻辑）
        Float dirHeight = bone.getCurrentDirectionHeight();
        if (dirHeight != null) {
            return dirHeight;
        }
        // 如果没有设置任何方向的高度，返回本地默认值
        return height;
    }

    public void setHeight(float height) {
        this.height = height;

        if (partQuad != null) {
            // 使用可复用数组更新位置缓冲区
            updateReusablePositionArray();
            partQuad.setBuffer(com.jme3.scene.VertexBuffer.Type.Position, 3, reusablePositionArray);
            partQuad.updateBound();
            // 强制标记缓冲区为已修改，确保渲染更新
            partQuad.getBuffer(com.jme3.scene.VertexBuffer.Type.Position).setUpdateNeeded();
            updateHighlightBorderSize();
        }

        if (bone.isRotationStripEnabled()) {
            bone.setStripHeight(height);
        } else {
            bone.setDirectionHeight(bone.getCurrentDirection(), height);
        }
    }

    public void setOffset(float offsetX, float offsetY) {
        this.offset.set(offsetX, offsetY, 0f);

        if (bone.isRotationStripEnabled()) {
            bone.setStripOffset(offset.x, offset.y, offset.z);
        } else {
            bone.setDirectionOffset(bone.getCurrentDirection(), offset.x, offset.y, offset.z);
        }
    }

    public void setOffsetX(float offsetX) {
        this.offset.x = offsetX;

        if (bone.isRotationStripEnabled()) {
            bone.setStripOffset(offset.x, offset.y, offset.z);
        } else {
            bone.setDirectionOffset(bone.getCurrentDirection(), offset.x, offset.y, offset.z);
        }
    }

    public void setOffsetY(float offsetY) {
        this.offset.y = offsetY;

        if (bone.isRotationStripEnabled()) {
            bone.setStripOffset(offset.x, offset.y, offset.z);
        } else {
            bone.setDirectionOffset(bone.getCurrentDirection(), offset.x, offset.y, offset.z);
        }
    }

    public void setOffsetZ(float offsetZ) {
        this.offset.z = offsetZ;

        if (bone.isRotationStripEnabled()) {
            bone.setStripOffset(offset.x, offset.y, offset.z);
        } else {
            bone.setDirectionOffset(bone.getCurrentDirection(), offset.x, offset.y, offset.z);
        }
    }

    public Vector3f getOffset() {
        return offset.clone();
    }

    public boolean isGridEnabled() {
        return gridEnabled;
    }

    public void setGridEnabled(boolean gridEnabled) {
        this.gridEnabled = gridEnabled;
    }

    public boolean isGridHorizontal() {
        return gridHorizontal;
    }

    public void setGridHorizontal(boolean gridHorizontal) {
        this.gridHorizontal = gridHorizontal;
    }

    public boolean isGridVertical() {
        return gridVertical;
    }

    public void setGridVertical(boolean gridVertical) {
        this.gridVertical = gridVertical;
    }

    public float getGridSize() {
        return gridSize;
    }

    public void setGridSize(float gridSize) {
        this.gridSize = gridSize;
    }

    public boolean isSnapToGrid() {
        return snapToGrid;
    }

    public void setSnapToGrid(boolean snapToGrid) {
        this.snapToGrid = snapToGrid;
    }

    
    public boolean isBillboardEnabled() {
        return bone.isBillboardEnabled();
    }

    
    public void setBillboardEnabled(boolean enabled) {
        bone.setBillboardEnabled(enabled);
    }

    
    public Vector3f getFinalWorldPosition() {
        if (partGeometry == null) {

            Vector3f worldPos = new Vector3f();
            Quaternion tempRot = new Quaternion();
            Vector3f tempScale = new Vector3f();
            bone.getWorldTransform(worldPos, tempRot, tempScale);
            return worldPos;
        }
        return partGeometry.getWorldTranslation();
    }

    
    private void createHighlight() {

        float highlightPadding = 0.15f;
        float highlightWidth = width + highlightPadding * 2;
        float highlightHeight = height + highlightPadding * 2;
        float lineThickness = 0.1f;

        highlightNode = new Node(bone.getName() + "_Highlight");

        highlightMaterial = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        highlightMaterial.setColor("Color", new ColorRGBA(1f, 1f, 1f, 1.0f));
        highlightMaterial.getAdditionalRenderState().setDepthWrite(false);
        highlightMaterial.getAdditionalRenderState().setDepthTest(false);

        Quad topQuad = new Quad(highlightWidth, lineThickness);
        topLine = new Geometry("TopLine", topQuad);
        topLine.setMaterial(highlightMaterial);
        topLine.setLocalTranslation(-highlightWidth/2, highlightHeight/2 - lineThickness, 0.01f);
        highlightNode.attachChild(topLine);

        Quad bottomQuad = new Quad(highlightWidth, lineThickness);
        bottomLine = new Geometry("BottomLine", bottomQuad);
        bottomLine.setMaterial(highlightMaterial);
        bottomLine.setLocalTranslation(-highlightWidth/2, -highlightHeight/2, 0.01f);
        highlightNode.attachChild(bottomLine);

        Quad leftQuad = new Quad(lineThickness, highlightHeight);
        leftLine = new Geometry("LeftLine", leftQuad);
        leftLine.setMaterial(highlightMaterial);
        leftLine.setLocalTranslation(-highlightWidth/2, -highlightHeight/2, 0.01f);
        highlightNode.attachChild(leftLine);

        Quad rightQuad = new Quad(lineThickness, highlightHeight);
        rightLine = new Geometry("RightLine", rightQuad);
        rightLine.setMaterial(highlightMaterial);
        rightLine.setLocalTranslation(highlightWidth/2 - lineThickness, -highlightHeight/2, 0.01f);
        highlightNode.attachChild(rightLine);

        highlightNode.setQueueBucket(RenderQueue.Bucket.Transparent);

        com.jme3.scene.control.BillboardControl highlightBillboard =
            new com.jme3.scene.control.BillboardControl();
        highlightBillboard.setAlignment(com.jme3.scene.control.BillboardControl.Alignment.Screen);
        highlightNode.addControl(highlightBillboard);

        parentNode.attachChild(highlightNode);

        highlightNode.setCullHint(Spatial.CullHint.Always);
    }

    
    private void createPivotMarker() {

        float markerSize = 0.15f;
        Quad markerQuad = new Quad(markerSize, markerSize);

        markerQuad.setBuffer(com.jme3.scene.VertexBuffer.Type.Position, 3, new float[]{
            -markerSize/2, -markerSize/2, 0,
            markerSize/2, -markerSize/2, 0,
            markerSize/2, markerSize/2, 0,
            -markerSize/2, markerSize/2, 0
        });

        pivotMarker = new Geometry(bone.getName() + "_Pivot", markerQuad);

        Material markerMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        markerMat.setColor("Color", new ColorRGBA(1f, 0f, 0f, 1f));
        markerMat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        markerMat.getAdditionalRenderState().setDepthTest(false);
        pivotMarker.setMaterial(markerMat);

        pivotMarker.setQueueBucket(RenderQueue.Bucket.Translucent);

        com.jme3.scene.control.BillboardControl pivotBillboard =
            new com.jme3.scene.control.BillboardControl();
        pivotBillboard.setAlignment(com.jme3.scene.control.BillboardControl.Alignment.Screen);
        pivotMarker.addControl(pivotBillboard);

        parentNode.attachChild(pivotMarker);

        pivotMarker.setCullHint(showPivotMarker ?
            Geometry.CullHint.Never : Geometry.CullHint.Always);
    }

    
    public void setPivotPoint(float x, float y) {
        this.pivotPoint.set(x, y, 0f);
        updatePivotMarkerPosition();
    }

    
    public void setPivotPoint(Vector3f pivot) {
        this.pivotPoint.set(pivot);
        updatePivotMarkerPosition();
    }

    
    public Vector3f getPivotPoint() {
        return pivotPoint.clone();
    }

    
    public void setShowPivotMarker(boolean show) {
        this.showPivotMarker = show;
        if (pivotMarker != null) {
            pivotMarker.setCullHint(show ?
                Geometry.CullHint.Never : Geometry.CullHint.Always);
        }
    }

    
    public void setCustomRotationX(float degrees) {
        this.customRotationX = degrees;

        if (bone.isRotationStripEnabled()) {
            bone.setStripRotation(customRotationX, customRotationY, customRotationZ);
        } else {
            bone.setDirectionRotation(bone.getCurrentDirection(), customRotationX, customRotationY, customRotationZ);
        }
    }

    
    public float getCustomRotationX() {
        return customRotationX;
    }

    
    public void setCustomRotationY(float degrees) {
        this.customRotationY = degrees;

        if (bone.isRotationStripEnabled()) {
            bone.setStripRotation(customRotationX, customRotationY, customRotationZ);
        } else {
            bone.setDirectionRotation(bone.getCurrentDirection(), customRotationX, customRotationY, customRotationZ);
        }
    }

    
    public float getCustomRotationY() {
        return customRotationY;
    }

    
    public void setCustomRotationZ(float degrees) {
        this.customRotationZ = degrees;

        if (bone.isRotationStripEnabled()) {
            bone.setStripRotation(customRotationX, customRotationY, customRotationZ);
        } else {
            bone.setDirectionRotation(bone.getCurrentDirection(), customRotationX, customRotationY, customRotationZ);
        }
    }

    
    public float getCustomRotationZ() {
        return customRotationZ;
    }

    
    public void setTextureRotation(float degrees) {
        this.textureRotation = degrees;
        this.useAnimationRotation = true;
        updateTexCoords();
    }

    
    public void setTextureRotationInternal(float degrees) {
        this.textureRotation = degrees;

        updateTexCoords();
    }

    
    public float getTextureRotation() {
        return textureRotation;
    }

    
    public boolean isUsingAnimationRotation() {
        return useAnimationRotation;
    }

    
    public void setUseAnimationRotation(boolean useAnimationRotation) {
        this.useAnimationRotation = useAnimationRotation;
        updateTexCoords();
    }

    
    public void resetAnimationRotation() {
        this.useAnimationRotation = false;
        updateTexCoords();
    }

    
    private void updatePivotMarkerPosition() {
        if (!initialized || pivotMarker == null) {
            return;
        }

        Vector3f worldPos = new Vector3f();
        Quaternion worldRot = new Quaternion();
        Vector3f worldScale = new Vector3f();
        bone.getWorldTransform(worldPos, worldRot, worldScale);

        Vector3f totalOffset = offset.add(pivotPoint);
        Vector3f rotatedOffset = worldRot.mult(totalOffset);
        Vector3f pivotWorldPos = worldPos.add(rotatedOffset);

        pivotMarker.setLocalTranslation(pivotWorldPos);
    }

    
    /**
     * 旋转条状贴图：按相机相对该部件的水平夹角(yaw)，从环绕360°的条状贴图上
     * 取样出当前应该显示的那一格，实现"伪3D棱柱"的转身错觉。
     * 与核心运行时版本(PuppetPartRenderer)逻辑保持一致。
     */
    // 固定规则：摄像机每转DEGREES_PER_STEP度，取景框挪1个像素。360°正好整除成15个固定位置，
    // 不可配置，不考虑档数/连续模式——与核心运行时版本(PuppetPartRenderer)保持一致
    private static final float DEGREES_PER_STEP = 10f;
    private static final int STEPS_PER_REVOLUTION = 360 / (int) DEGREES_PER_STEP; // 15

    private void applyRotationStripUV(Vector3f worldPos) {
        String stripPath = bone.getStripTexturePath();
        if (stripPath == null || stripPath.isEmpty()) {
            return;
        }

        int frameWidthPx = bone.getStripFrameWidthPx();
        int calibrationOffsetPx = bone.getStripCalibrationOffsetPx();

        // 左侧留白：校准偏移理论上最负也只会是-(STEPS_PER_REVOLUTION-1)（selPixelX=0时），
        // 固定预留这么多像素的透明留白，保证校准偏移为负数时取景框仍有合法像素可采样，
        // 不需要在下面夹紧坐标——夹紧会导致转到某些角度时取景框卡住不动，达不到"按原规则移动"的要求。
        int leftMarginPx = STEPS_PER_REVOLUTION - 1;
        // 右侧（原图+右侧补齐）所需宽度：正的校准偏移量 + 15个固定位置最后一个的起点 + 取景框宽度
        int requiredWidthPx = Math.max(0, calibrationOffsetPx) + (STEPS_PER_REVOLUTION - 1) + frameWidthPx;

        com.Hecate.puppet.core.RotationStripTextureUtil.PaddedStrip strip =
                com.Hecate.puppet.core.RotationStripTextureUtil.getOrCreatePaddedStrip(
                        app.getAssetManager(), stripPath, leftMarginPx, requiredWidthPx);
        if (strip == null) {
            return;
        }

        if (texture != strip.texture) {
            texture = strip.texture;
            partMaterial.setTexture("ColorMap", texture);
            partMaterial.setColor("Color", ColorRGBA.White);
        }

        Vector3f camPos = app.getCamera().getLocation();
        Vector3f toCam = camPos.subtract(worldPos);
        Vector3f horizontalDir = new Vector3f(toCam.x, 0, toCam.z);
        if (horizontalDir.lengthSquared() < 0.0001f) {
            return;
        }
        horizontalDir.normalizeLocal();
        float yawRad = FastMath.atan2(horizontalDir.x, horizontalDir.z);
        float yawDeg = yawRad * FastMath.RAD_TO_DEG;

        int paddedWidthPx = strip.paddedWidthPx;

        // 每转DEGREES_PER_STEP度挪1个像素，一步只走一个像素
        int stepIndex = Math.round(yawDeg / DEGREES_PER_STEP);
        stepIndex = ((stepIndex % STEPS_PER_REVOLUTION) + STEPS_PER_REVOLUTION) % STEPS_PER_REVOLUTION;

        // 校准偏移：把"摄像机朝向0° -> 取景框从像素0开始采样"这条固定规则整体平移
        // calibrationOffsetPx像素。用户在选区面板对准某个朝向手动拖好取景框后点击"校准"
        // 写入这个值，换贴图也不受影响（存在Bone上，不跟着贴图文件走）。加上leftMarginPx
        // 换算到这张贴图的实际像素坐标——偏移无论正负，贴图左侧留白都保证了坐标始终合法，
        // 取景框能按固定1像素/步连续滑动，不会在任何朝向卡住。
        int pixelStart = leftMarginPx + calibrationOffsetPx + stepIndex;

        float u0 = pixelStart / (float) paddedWidthPx;
        float u1 = (pixelStart + frameWidthPx) / (float) paddedWidthPx;

        // 【关键修复】V范围必须按取景框高度裁剪，不能永远取贴图整张高度。
        // 之前v0/v1硬编码为0/1，导致无论取景框选多高，都把整张贴图的高度塞进
        // 按stripFrameHeightPx算出来的四边形里，两者高宽比不一致时贴图就被拉伸/压扁。
        // 取景框从贴图顶部往下框选（与选区面板"像素从上到下数"的约定一致），
        // 顶部对应V=1，取景框底部对应V = 1 - frameHeightPx/纹理实际高度。
        int frameHeightPx = bone.getStripFrameHeightPx();
        int texHeightPx = strip.heightPx;
        float v1 = 1f;
        float v0 = (texHeightPx > 0) ? Math.max(0f, 1f - frameHeightPx / (float) texHeightPx) : 0f;

        if (partQuad != null) {
            float[] texCoords = new float[]{
                u0, v0,
                u1, v0,
                u1, v1,
                u0, v1
            };
            partQuad.setBuffer(com.jme3.scene.VertexBuffer.Type.TexCoord, 2, texCoords);
        }
    }

    private void updateTexCoords() {
        if (partQuad == null) {
            return;
        }

        float u0 = uvOffsetX;
        float v0 = uvOffsetY;
        float u1 = uvOffsetX + uvScaleX;
        float v1 = uvOffsetY + uvScaleY;

        float rotationDegrees;
        if (useAnimationRotation) {
            rotationDegrees = textureRotation;
        } else {
            rotationDegrees = bone.getCurrentDirectionTextureRotation();
        }

        float[] texCoords;
        if (Math.abs(rotationDegrees) > 0.001f) {

            float centerU = (u0 + u1) / 2.0f;
            float centerV = (v0 + v1) / 2.0f;

            float angleRad = (float) Math.toRadians(-rotationDegrees);
            float cos = (float) Math.cos(angleRad);
            float sin = (float) Math.sin(angleRad);

            float u0_rot = centerU + (u0 - centerU) * cos - (v0 - centerV) * sin;
            float v0_rot = centerV + (u0 - centerU) * sin + (v0 - centerV) * cos;

            float u1_rot = centerU + (u1 - centerU) * cos - (v0 - centerV) * sin;
            float v1_rot = centerV + (u1 - centerU) * sin + (v0 - centerV) * cos;

            float u2_rot = centerU + (u1 - centerU) * cos - (v1 - centerV) * sin;
            float v2_rot = centerV + (u1 - centerU) * sin + (v1 - centerV) * cos;

            float u3_rot = centerU + (u0 - centerU) * cos - (v1 - centerV) * sin;
            float v3_rot = centerV + (u0 - centerU) * sin + (v1 - centerV) * cos;

            texCoords = new float[]{
                u0_rot, v0_rot,
                u1_rot, v1_rot,
                u2_rot, v2_rot,
                u3_rot, v3_rot
            };
        } else {

            texCoords = new float[]{
                u0, v0,
                u1, v0,
                u1, v1,
                u0, v1
            };
        }

        partQuad.setBuffer(com.jme3.scene.VertexBuffer.Type.TexCoord, 2, texCoords);
        partQuad.getBuffer(com.jme3.scene.VertexBuffer.Type.TexCoord).setUpdateNeeded();
    }

    
    public void setUV(float offsetX, float offsetY, float scaleX, float scaleY) {
        this.uvOffsetX = offsetX;
        this.uvOffsetY = offsetY;
        this.uvScaleX = scaleX;
        this.uvScaleY = scaleY;
        updateTexCoords();

        saveCurrentDirectionUV();
    }

    
    public float getUvOffsetX() {
        return uvOffsetX;
    }

    
    public void setUvOffsetX(float uvOffsetX) {
        this.uvOffsetX = uvOffsetX;
        updateTexCoords();
        saveCurrentDirectionUV();
    }

    
    public float getUvOffsetY() {
        return uvOffsetY;
    }

    
    public void setUvOffsetY(float uvOffsetY) {
        this.uvOffsetY = uvOffsetY;
        updateTexCoords();
        saveCurrentDirectionUV();
    }

    
    public float getUvScaleX() {
        return uvScaleX;
    }

    
    public void setUvScaleX(float uvScaleX) {
        this.uvScaleX = uvScaleX;
        updateTexCoords();
        saveCurrentDirectionUV();
    }

    
    public float getUvScaleY() {
        return uvScaleY;
    }

    
    public void setUvScaleY(float uvScaleY) {
        this.uvScaleY = uvScaleY;
        updateTexCoords();
        saveCurrentDirectionUV();
    }

    
    private void updateHighlightBorderSize() {
        if (topLine == null || bottomLine == null || leftLine == null || rightLine == null) {
            return;
        }

        float highlightPadding = 0.15f;
        float highlightWidth = width + highlightPadding * 2;
        float highlightHeight = height + highlightPadding * 2;
        float lineThickness = 0.1f;

        ((Quad)topLine.getMesh()).updateGeometry(highlightWidth, lineThickness);
        topLine.setLocalTranslation(-highlightWidth/2, highlightHeight/2 - lineThickness, 0.01f);

        ((Quad)bottomLine.getMesh()).updateGeometry(highlightWidth, lineThickness);
        bottomLine.setLocalTranslation(-highlightWidth/2, -highlightHeight/2, 0.01f);

        ((Quad)leftLine.getMesh()).updateGeometry(lineThickness, highlightHeight);
        leftLine.setLocalTranslation(-highlightWidth/2, -highlightHeight/2, 0.01f);

        ((Quad)rightLine.getMesh()).updateGeometry(lineThickness, highlightHeight);
        rightLine.setLocalTranslation(highlightWidth/2 - lineThickness, -highlightHeight/2, 0.01f);
    }

    
    private Quad createCenteredQuad(float w, float h) {

        com.jme3.scene.Mesh mesh = new com.jme3.scene.Mesh();

        float[] positions = new float[]{
            -w/2, -h/2, 0,
            w/2, -h/2, 0,
            w/2, h/2, 0,
            -w/2, h/2, 0
        };
        mesh.setBuffer(com.jme3.scene.VertexBuffer.Type.Position, 3, positions);

        float[] texCoords = new float[]{
            uvOffsetX, uvOffsetY,
            uvOffsetX + uvScaleX, uvOffsetY,
            uvOffsetX + uvScaleX, uvOffsetY + uvScaleY,
            uvOffsetX, uvOffsetY + uvScaleY
        };
        mesh.setBuffer(com.jme3.scene.VertexBuffer.Type.TexCoord, 2, texCoords);

        float[] normals = new float[]{
            0, 0, 1,
            0, 0, 1,
            0, 0, 1,
            0, 0, 1
        };
        mesh.setBuffer(com.jme3.scene.VertexBuffer.Type.Normal, 3, normals);

        short[] indices = new short[]{
            0, 1, 2,
            0, 2, 3
        };
        mesh.setBuffer(com.jme3.scene.VertexBuffer.Type.Index, 3, indices);

        mesh.updateBound();

        Quad quad = new Quad(1, 1);
        quad.setBuffer(com.jme3.scene.VertexBuffer.Type.Position, 3, positions);
        quad.setBuffer(com.jme3.scene.VertexBuffer.Type.TexCoord, 2, texCoords);
        quad.setBuffer(com.jme3.scene.VertexBuffer.Type.Normal, 3, normals);
        quad.setBuffer(com.jme3.scene.VertexBuffer.Type.Index, 3, indices);
        quad.updateBound();

        return quad;
    }

    
    public void setParentRenderer(EditorPuppetRenderer renderer) {
        this.parentRenderer = renderer;
    }
}

