package com.Hecate.puppet.core;

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

/**
 * 木偶部件渲染�?
 * 负责渲染单个骨骼部件
 */
public class PuppetPartRenderer {

    private final SimpleApplication app;
    private final Bone bone;
    private final Node parentNode;
    private PuppetRenderer parentRenderer;  // 鐖舵覆鏌撳櫒引用（精敤浜庣粺一billboard�?

    private Geometry partGeometry;
    private Material partMaterial;
    private Quad partQuad;
    private com.jme3.scene.control.BillboardControl billboardControl;

    // 选中高光锛堟敼涓鸿竟妗嗙嚎鏉★級
    private Node highlightNode;
    private Geometry topLine, bottomLine, leftLine, rightLine;
    private Material highlightMaterial;
    private boolean isSelected = false;

    // 部件尺寸
    private float width;
    private float height;

    // 纹理
    private Texture texture;
    private String texturePath;  // 纹理文件璺�?

    // UV坐标（精汗鐞嗗浘闆嗗垏鐗囷級
    private float uvOffsetX = 0.0f;
    private float uvOffsetY = 0.0f;
    private float uvScaleX = 1.0f;
    private float uvScaleY = 1.0f;

    // 网格系统配置
    private boolean gridEnabled = false;
    private boolean gridHorizontal = true;
    private boolean gridVertical = true;
    private float gridSize = 32f;  // 网格大小锛堝儚绱狅級
    private boolean snapToGrid = false;

    //
    private boolean initialized = false;

    //
    private final Vector3f offset = new Vector3f(0f, 0f, 0f);

    //
    private final Vector3f pivotPoint = new Vector3f(0f, 0f, 0f);

    //
    private Geometry pivotMarker;
    private boolean showPivotMarker = false;

    // 鑷畾涔夋棆杞度
    private float customRotationX = 0f; // X杞存棆杞紙璺疯贩鏉夸笂涓嬫憜鍔級
    private float customRotationY = 0f; // Y杞存棆杞紙左右杞悜（
    private float customRotationZ = 0f; // Z杞存棆杞紙平面鍐呮棆杞級

    // 贴图旋转角度
    private float textureRotation = 0f;

    // 优先度Z偏移（用于分层渲染）
    private float priorityZOffset = 0f;

    // 标记使用方向旋转
    private boolean useAnimationRotation = false;

    // 固定的调试颜色（避免闪烁�?
    private final ColorRGBA debugColor;

    // ==================== 3D模型骨骼系统 ====================
    // 模型骨骼是与Quad部件完全独立的一条渲染路径：不创建Quad/Material/billboard/高光/
    // 中心点标记，只加载一个外部模型Spatial并挂到parentNode下，按bone的世界变换摆放。
    // 是否走这条路径由bone.isModelEnabled()在initialize()时决定一次，本渲染器实例生命周期内
    // 不支持中途切换模式（要切换请重新创建部件——与现有代码里"新建部件决定类型"的惯例一致）。
    private com.jme3.scene.Spatial modelSpatial;
    private String loadedModelPath;  // 记录已加载的模型路径，路径不变时避免重复loadModel()

    public PuppetPartRenderer(SimpleApplication app, Bone bone, Node parentNode, float width, float height) {
        this.app = app;
        this.bone = bone;
        this.parentNode = parentNode;
        this.width = width;
        this.height = height;
        // 为每个部件生成一个固定的随机调试颜色
        this.debugColor = ColorRGBA.randomColor();
    }

    /**
     * 鍒濆鍖栨覆鏌撳�?
     */
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

        // 【阴槾褰变紭鍖栥€戞湪鍋堕儴浠跺彧鎺ユ敹闃村奖锛屼笉鎶曞皠锛堥伩鍏嶈В浣撻槾褰憋級
        // 统一鐨勯槾褰辩敱PuppetRenderer涓殑shadowCaster鎶曞�?
        // 【阴影优化】木偶部件只接收阴影，不投射（避免解体阴影）
        // 统一的阴影由PuppetRenderer中的shadowCaster投射
        partGeometry.setShadowMode(com.jme3.renderer.queue.RenderQueue.ShadowMode.Receive);

        // 鍒涘缓鏉愯川
        createMaterial();

        // 璁剧疆渲染灞炴€?
        setupRenderProperties();

        // 娣诲姞billboard鎺у埗锛岃部件濮嬬粓闈㈠悜鎽勫儚鏈?
        billboardControl = new com.jme3.scene.control.BillboardControl();
        billboardControl.setAlignment(com.jme3.scene.control.BillboardControl.Alignment.Screen);
        partGeometry.addControl(billboardControl);

        // 娣诲姞鍒扮埗鑺傜�?
        parentNode.attachChild(partGeometry);

        // 鍒涘缓选中高光
        createHighlight();

        // 鍒涘缓涓績鐐规爣设
        createPivotMarker();

        // 鍔犺浇纹理锛堜紭鍏堜娇鐢ㄦ柟鍚戣创鍥撅級
        updateTextureFromBone();

        // 灏嗗垵濮嬪搴﹀拰楂樺害淇濆瓨鍒癇one鐨勫綋鍓嶆柟鍚戯紙瑕嗙洊浠讳綍鏃у€硷�?
        if (bone.isRotationStripEnabled()) {
            bone.setStripWidth(width);
            bone.setStripHeight(height);
        } else {
            bone.setDirectionWidth(bone.getCurrentDirection(), width);
            bone.setDirectionHeight(bone.getCurrentDirection(), height);
        }

        initialized = true;
    }

    /**
     * 从Bone加载3D模型（仅modelEnabled=true时使用）。
     * 加载失败时留空并打印错误，不影响其他部件渲染。
     */
    private void updateModelFromBone() {
        String modelPath = bone.getModelFilePath();
        if (modelPath == null || modelPath.isEmpty()) {
            return;
        }

        if (modelSpatial != null && modelPath.equals(loadedModelPath)) {
            return; // 路径没变，不重复加载
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
            System.err.println("[PuppetPartRenderer] 加载3D模型失败: " + modelPath);
            System.err.println("[PuppetPartRenderer] 错误信息: " + e.getMessage());
            modelSpatial = null;
            loadedModelPath = null;
        }
    }

    /**
     * 把bone的世界变换套到模型Spatial上（仅modelEnabled=true时使用）。
     * 不做billboard/高光/优先级分层——模型骨骼固定朝向，跟普通2D纸片部件是两套逻辑。
     */
    private void updateModelTransform() {
        if (modelSpatial == null) {
            return;
        }

        Vector3f worldPos = new Vector3f();
        Quaternion worldRot = new Quaternion();
        Vector3f worldScale = new Vector3f();
        bone.getWorldTransform(worldPos, worldRot, worldScale);

        // 叠加模型自身的额外旋转（用于修正模型朝向，比如导出时坐标轴习惯不同）
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

    /**
     * 鍒涘缓鏉愯川
     */
    private void createMaterial() {
        // 使用Lighting材质以支持阴影
        partMaterial = new Material(app.getAssetManager(), "Common/MatDefs/Light/Lighting.j3md");
        partMaterial.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        partMaterial.setTransparent(true);
        partMaterial.setBoolean("UseMaterialColors", true);
        partMaterial.setColor("Diffuse", ColorRGBA.White);
        partMaterial.setColor("Ambient", ColorRGBA.White);
        partMaterial.setColor("GlowColor", ColorRGBA.White);
        partMaterial.setFloat("Shininess", 0f);
        // 降低到0.1以支持半透明显示（10%以下的透明度会被丢弃，避免幽灵影子）
        partMaterial.setFloat("AlphaDiscardThreshold", 0.1f);
        partGeometry.setMaterial(partMaterial);
    }

    /**
     *
     */
    private void setupRenderProperties() {
        //
        partGeometry.setQueueBucket(RenderQueue.Bucket.Transparent);

        // 【阴影优化】木偶部件只接收阴影，不投射
        partGeometry.setShadowMode(RenderQueue.ShadowMode.Receive);

        // 启用深度写入（用于解决穿模问题）
        partMaterial.getAdditionalRenderState().setDepthWrite(true);

        // 鍚敤娣卞害娴嬭瘯锛堜慨澶嶇┛妯￠棶棰橈�?
        // 閰嶅悎priority偏移系统锛岀‘淇濋珮priority部件鎬绘槸鍦ㄥ墠�?
        partMaterial.getAdditionalRenderState().setDepthTest(true);

        // 【修复闪烁】启用背面剔除，只渲染正�?
        // Billboard总是面向摄像机，只需要渲染正面，避免正反面冲突导致闪�?
        partMaterial.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Back);

        // 鍚敤閫忔槑娣峰�?
        partMaterial.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);

        // 【25段分层多边形偏移系统】
        // 初始化为0，将在updateTransform()中根据bone.getCurrentDirectionPriority()动态设置
        // 每层间隔100.0f，层内每个priority间隔1.0f，实现priority 1-25, 26-50, 51-75, 76-100的绝对分层
    }

    /**
     * 鍔犺浇纹�?
     */
    public void loadTexture(String texturePath) {
        try {
            texture = app.getAssetManager().loadTexture(texturePath);
            texture.setMagFilter(Texture.MagFilter.Nearest);
            texture.setMinFilter(Texture.MinFilter.NearestNoMipMaps);
            partMaterial.setTexture("DiffuseMap", texture);  // Lighting.j3md uses DiffuseMap
            partMaterial.setColor("Diffuse", ColorRGBA.White);
            partMaterial.setColor("Ambient", ColorRGBA.White);
            this.texturePath = texturePath;

        } catch (Exception e) {
            System.err.println("[PuppetPartRenderer] 鍔犺浇纹理澶辫触: " + texturePath);
            System.err.println("[PuppetPartRenderer] 閿欒淇℃伅: " + e.getMessage());
            e.printStackTrace();
            setDebugColor(ColorRGBA.Magenta);
            texture = null;
        }
    }

    /**
     * 浠嶣one鏇存柊纹理锛堟牴鎹綋鍓嶆柟鍚戝姞杞藉搴旇创鍥撅級
     * 鍚屾椂鎭㈠璇ユ柟鍚戠殑鎵€鏈夊睘鎬э紙UV銆佸昂瀵搞€佸亸绉汇€佹棆杞€佷紭鍏堢骇�?
     */
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
                partQuad.setBuffer(com.jme3.scene.VertexBuffer.Type.Position, 3, new float[]{
                    -width/2, -height/2, 0,
                    width/2, -height/2, 0,
                    width/2, height/2, 0,
                    -width/2, height/2, 0
                });
                partQuad.updateBound();
                updateHighlightBorderSize();
            }

            Vector3f stripOffset = bone.getStripOffset();
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
            // 榛樿棰滆壊锛堣皟璇曠敤�?
            setDebugColor(debugColor);
        }

        // 鎭㈠褰撳墠方向鐨刄V坐标
        float[] directionUV = bone.getCurrentDirectionUV();
        if (directionUV != null && directionUV.length == 4) {
            // 涓嶈Е鍙憇ave锛岀洿鎺ヨ缃�?
            this.uvOffsetX = directionUV[0];
            this.uvOffsetY = directionUV[1];
            this.uvScaleX = directionUV[2];
            this.uvScaleY = directionUV[3];
            updateTexCoords();
        }

        // 鎭㈠褰撳墠方向鐨勫昂对
        Float dirWidth = bone.getCurrentDirectionWidth();
        Float dirHeight = bone.getCurrentDirectionHeight();
        if (dirWidth != null) {
            this.width = dirWidth;
            if (partQuad != null) {
                partQuad.setBuffer(com.jme3.scene.VertexBuffer.Type.Position, 3, new float[]{
                    -width/2, -height/2, 0,
                    width/2, -height/2, 0,
                    width/2, height/2, 0,
                    -width/2, height/2, 0
                });
                partQuad.updateBound();
                updateHighlightBorderSize();
            }
        }
        if (dirHeight != null) {
            this.height = dirHeight;
            if (partQuad != null) {
                partQuad.setBuffer(com.jme3.scene.VertexBuffer.Type.Position, 3, new float[]{
                    -width/2, -height/2, 0,
                    width/2, -height/2, 0,
                    width/2, height/2, 0,
                    -width/2, height/2, 0
                });
                partQuad.updateBound();
                updateHighlightBorderSize();
            }
        }

        // 鎭㈠褰撳墠方向鐨勪綅缃亸�?
        float[] dirOffset = bone.getCurrentDirectionOffset();
        if (dirOffset != null && dirOffset.length == 3) {
            this.offset.set(dirOffset[0], dirOffset[1], dirOffset[2]);
        }

        // 鎭㈠褰撳墠方向鐨勬棆轴
        float[] dirRotation = bone.getCurrentDirectionRotation();
        if (dirRotation != null && dirRotation.length >= 3) {
            this.customRotationX = dirRotation[0];
            this.customRotationY = dirRotation[1];
            this.customRotationZ = dirRotation[2];
        } else if (dirRotation != null && dirRotation.length == 2) {
            // 鍚戝悗鍏煎鏃ф暟鎹紙鍙湁X鍜孼�?
            this.customRotationX = dirRotation[0];
            this.customRotationY = 0f;
            this.customRotationZ = dirRotation[1];
        }

        // 娉ㄦ剰锛歱riority涓嶉渶瑕佸湪杩欓噷鎭㈠锛屽洜涓哄畠瀛樺偍鍦˙one涓�?
        // updateTransform()浼氱洿鎺ヤ粠bone.getCurrentDirectionPriority()璇诲�?
    }

    /**
     * 璁剧疆褰撳墠方向骞舵洿鏂拌创�?
     * 鍦ㄥ垏鎹㈡柟鍚戝墠淇濆瓨褰撳墠方向鐨勬墍鏈夊睘鎬?
     */
    public void setDirection(Bone.Direction direction) {
        // 淇濆瓨褰撳墠方向鐨勬墍鏈夊睘�?
        saveCurrentDirectionProperties();

        // 鍒囨崲方�?
        bone.setCurrentDirection(direction.getKey());

        // 鍔犺浇鏂版柟鍚戠殑纹理鍜屾墍鏈夊睘鎬?
        updateTextureFromBone();
    }

    /**
     * 璁剧疆褰撳墠方向骞舵洿鏂拌创鍥撅紙瀛楃涓瞜ey�?
     * 鍦ㄥ垏鎹㈡柟鍚戝墠淇濆瓨褰撳墠方向鐨勬墍鏈夊睘鎬?
     */
    public void setDirection(String directionKey) {
        // 淇濆瓨褰撳墠方向鐨勬墍鏈夊睘�?
        saveCurrentDirectionProperties();

        // 鍒囨崲方�?
        bone.setCurrentDirection(directionKey);

        // 鍔犺浇鏂版柟鍚戠殑纹理鍜屾墍鏈夊睘鎬?
        updateTextureFromBone();
    }

    /**
     * 淇濆瓨褰撳墠方向鐨勬墍鏈夊睘鎬у埌Bone
     * 鍖呮嫭UV坐标銆佸昂瀵搞€佷綅缃亸绉汇€佹棆杞€佷紭鍏堢�?
     */
    private void saveCurrentDirectionProperties() {
        // 旋转条状贴图模式不使用6方向系统，不应该按方向存储任何属性
        if (bone.isRotationStripEnabled()) {
            return;
        }

        String currentDirection = bone.getCurrentDirection();

        // 淇濆瓨UV坐标
        bone.setDirectionUV(currentDirection, uvOffsetX, uvOffsetY, uvScaleX, uvScaleY);

        // 淇濆瓨尺寸
        bone.setDirectionWidth(currentDirection, width);
        bone.setDirectionHeight(currentDirection, height);

        // 淇濆瓨浣嶇疆偏移
        bone.setDirectionOffset(currentDirection, offset.x, offset.y, offset.z);

        // 淇濆瓨旋�?
        bone.setDirectionRotation(currentDirection, customRotationX, customRotationY, customRotationZ);

        // 淇濆瓨浼樺厛绾э紙鏂板（
        bone.setDirectionPriority(currentDirection, bone.getPriority());
    }

    /**
     * 淇濆瓨褰撳墠方向鐨刄V坐标鍒癇one锛堜繚鐣欑敤浜庡悜鍚庡吋瀹癸�?
     */
    private void saveCurrentDirectionUV() {
        String currentDirection = bone.getCurrentDirection();
        bone.setDirectionUV(currentDirection, uvOffsetX, uvOffsetY, uvScaleX, uvScaleY);
    }

    /**
     * 鑾峰彇褰撳墠方向
     */
    public String getCurrentDirection() {
        return bone.getCurrentDirection();
    }

    /**
     * 鑾峰彇纹理璺緞
     */
    public String getTexturePath() {
        return texturePath;
    }

    /**
     * 鑾峰彇纹理瀵硅�?
     */
    public Texture getTexture() {
        return texture;
    }

    /**
     * 璁剧疆璋冭瘯棰滆壊锛堟棤纹理鏃朵娇鐢級
     */
    public void setDebugColor(ColorRGBA color) {
        partMaterial.clearParam("DiffuseMap");  // Lighting.j3md uses DiffuseMap
        partMaterial.setBoolean("UseMaterialColors", true);
        partMaterial.setColor("Diffuse", color);
        partMaterial.setColor("Ambient", color);
    }

    /**
     * 设置优先度Z偏移
     */
    public void setPriorityZOffset(float offset) {
        this.priorityZOffset = offset;
    }

    /**
     * 获取优先度Z偏移
     */
    public float getPriorityZOffset() {
        return priorityZOffset;
    }

    /**
     * 鏇存柊部件鐨勪笘鐣屽彉据
     */
    public void updateTransform() {
        if (!initialized) {
            return;
        }

        // 3D模型骨骼：固定朝向，不走billboard/高光/优先级分层这套2D纸片逻辑，
        // 只是简单地把bone的世界变换（含父骨骼递归合成的结果）套到模型Spatial上，
        // 再叠加模型自身的额外旋转和缩放（用于修正模型朝向/单位不一致）
        if (bone.isModelEnabled()) {
            updateModelTransform();
            return;
        }


        // 浠庨楠艰幏鍙栦笘鐣屽彉�?
        Vector3f worldPos = new Vector3f();
        Quaternion worldRot = new Quaternion();
        Vector3f worldScale = new Vector3f();
        bone.getWorldTransform(worldPos, worldRot, worldScale);

        // 旋转条状贴图模式：按相机水平角度动态取样UV（只影响UV，宽高/位置/billboard走原有逻辑）
        if (bone.isRotationStripEnabled()) {
            applyRotationStripUV(worldPos);
        }

        // 浠嶣one璇诲彇褰撳墠方向鐨勫搴﹀拰楂樺害锛堝鏋滄湁璁剧疆（
        // 娉ㄦ剰锛氬彧鍦ㄥ€肩湡姝ｆ敼鍙樻椂鎵嶆洿鏂帮紙閬垮厤瑕嗙洊鐢ㄦ埛鎵嬪姩璁剧疆鐨勫€硷�?
        // 旋转条状贴图模式：读单一的stripWidth/stripHeight，不走6方向的Map
        Float dirWidth = bone.isRotationStripEnabled() ? bone.getStripWidth() : bone.getCurrentDirectionWidth();
        Float dirHeight = bone.isRotationStripEnabled() ? bone.getStripHeight() : bone.getCurrentDirectionHeight();

        // 鍙湁褰揃one涓瓨鍌ㄧ殑鍊间笌褰撳墠鍊间笉鍚屾椂鎵嶆洿方
        // 杩欏厑璁哥敤鎴锋墜鍔ㄤ慨鏀瑰度楂樺害锛岃€屼笉浼氳姣忓抚閲嶇疆
        if (dirWidth != null && Math.abs(dirWidth - width) > 0.001f) {
            // 鍐呴儴鏇存柊锛屼笉鍐嶄繚瀛樺洖Bone锛堥伩鍏嶅惊鐜�?
            this.width = dirWidth;
            if (partQuad != null) {
                partQuad.setBuffer(com.jme3.scene.VertexBuffer.Type.Position, 3, new float[]{
                    -width/2, -height/2, 0,
                    width/2, -height/2, 0,
                    width/2, height/2, 0,
                    -width/2, height/2, 0
                });
                partQuad.updateBound();
                updateHighlightBorderSize();
            }
        }
        if (dirHeight != null && Math.abs(dirHeight - height) > 0.001f) {
            // 鍐呴儴鏇存柊锛屼笉鍐嶄繚瀛樺洖Bone锛堥伩鍏嶅惊鐜�?
            this.height = dirHeight;
            if (partQuad != null) {
                partQuad.setBuffer(com.jme3.scene.VertexBuffer.Type.Position, 3, new float[]{
                    -width/2, -height/2, 0,
                    width/2, -height/2, 0,
                    width/2, height/2, 0,
                    -width/2, height/2, 0
                });
                partQuad.updateBound();
                updateHighlightBorderSize();
            }
        }

        // 浠嶣one璇诲彇褰撳墠方向鐨勬棆杞紙濡傛灉鏈夎缃�?
        float[] dirRotation = bone.isRotationStripEnabled() ? new float[]{bone.getStripRotationX(), bone.getStripRotationY(), bone.getStripRotationZ()} : bone.getCurrentDirectionRotation();
        float currentRotX = customRotationX;
        float currentRotY = customRotationY;
        float currentRotZ = customRotationZ;

        if (dirRotation != null) {
            if (dirRotation.length >= 3) {
                currentRotX = dirRotation[0];
                currentRotY = dirRotation[1];
                currentRotZ = dirRotation[2];
            } else if (dirRotation.length == 2) {
                // 鍚戝悗鍏煎锛氭棫鏁版嵁鍙湁X鍜孼
                currentRotX = dirRotation[0];
                currentRotY = 0f;
                currentRotZ = dirRotation[1];
            }
        }

        // 1. 璁＄畻鑷畾涔夋棆杞紙鍦ㄩ儴浠剁殑灞€閮ㄥ潗鏍囩郴涓�?
        Quaternion customRot = new Quaternion();
        boolean hasCustomRotation = (currentRotX != 0f || currentRotY != 0f || currentRotZ != 0f);

        if (hasCustomRotation) {
            // 璁＄畻部件鐨勫眬閮ㄥ潗标记酱锛堣窡闅忕焊鐨勬湞鍚戯級
            Vector3f localAxisX = worldRot.mult(Vector3f.UNIT_X); // 灞€閮ㄦí杞达紙璺疯贩鏉胯酱（
            Vector3f localAxisY = worldRot.mult(Vector3f.UNIT_Y); // 灞€閮ㄧ珫杞达紙左右杞悜杞达級
            Vector3f localAxisZ = worldRot.mult(Vector3f.UNIT_Z); // 灞€閮ㄦ硶绾胯酱锛堝瀭鐩寸焊闈�?

            // 鍏堝簲鐢╔杞存棆杞紙跷跷板- 缁曞眬閮ㄦí杞达級
            Quaternion rotX = new Quaternion();
            if (currentRotX != 0f) {
                float radiansX = currentRotX * com.jme3.math.FastMath.DEG_TO_RAD;
                rotX.fromAngleAxis(radiansX, localAxisX);
            }

            // 鍐嶅簲鐢╕杞存棆杞紙左右杞�?- 缁曞眬閮ㄧ珫杞达�?
            Quaternion rotY = new Quaternion();
            if (currentRotY != 0f) {
                float radiansY = currentRotY * com.jme3.math.FastMath.DEG_TO_RAD;
                rotY.fromAngleAxis(radiansY, localAxisY);
            }

            // 鏈€鍚庡簲鐢╖杞存棆杞紙平面鍐呰浆�?- 缁曞眬閮ㄦ硶绾胯酱（
            Quaternion rotZ = new Quaternion();
            if (currentRotZ != 0f) {
                float radiansZ = currentRotZ * com.jme3.math.FastMath.DEG_TO_RAD;
                rotZ.fromAngleAxis(radiansZ, localAxisZ);
            }

            // 缁勫悎旋转锛氬厛X鍚嶻鍚嶼
            customRot = rotX.mult(rotY).mult(rotZ);
        }

        // 2. 缁撳悎骨骼旋转鍜岃嚜瀹氫箟旋�?
        Quaternion finalRot = customRot.mult(worldRot);

        // 3. 璁＄畻鏈€缁堜綅�? 鍏堝簲鐢ㄦ墜鍔ㄦ棆杞埌骨骼鐨勫師濮嬩笘鐣屼綅�?
        Vector3f finalPos = worldPos.clone();

        // 銆愬叧閿慨澶嶃€戝厛搴旂敤鎵嬪姩旋转鍒伴楠肩殑鍘熷浣嶇疆锛屽啀搴旂敤鍏朵粬偏移
        // bone.getWorldTransform() 杩斿洖鐨勬槸相对浜庢湪鍋惰妭鐐圭殑浣嶇疆锛岄渶瑕佽浆鎹㈠埌涓栫晫绌洪棿
        if (parentRenderer != null) {
            float manualRotation = parentRenderer.getManualRotationAngle();
            if (Math.abs(manualRotation) > 0.001f) {
                // 骨骼的worldPos是相对于木偶节点的位置，直接作为偏移向量使用
                // 不需要减去puppetCenter，因为它们在不同的坐标系
                Vector3f boneOffset = worldPos.clone();

                // 创建绕Y轴旋转的四元�?
                Quaternion rotation = new Quaternion();
                rotation.fromAngleAxis(manualRotation, Vector3f.UNIT_Y);

                // 旋转偏移向量
                Vector3f rotatedOffset = rotation.mult(boneOffset);

                // 计算旋转后的骨骼位置（在世界空间中）
                finalPos = rotatedOffset;

                // 同时旋转骨骼的朝向，使偏移方向正�?
                worldRot = rotation.mult(worldRot);
            }
        }

        // 浠嶣one璇诲彇褰撳墠方向鐨勪綅缃亸绉伙紙濡傛灉鏈夎缃級
        float[] dirOffset = bone.isRotationStripEnabled() ? new float[]{bone.getStripOffset().x, bone.getStripOffset().y, bone.getStripOffset().z} : bone.getCurrentDirectionOffset();
        Vector3f currentOffset = (dirOffset != null) ?
            new Vector3f(dirOffset[0], dirOffset[1], dirOffset[2]) : offset;

        // 搴旂敤offset
        if (!currentOffset.equals(Vector3f.ZERO)) {
            Vector3f rotatedOffset = worldRot.mult(currentOffset);
            finalPos.addLocal(rotatedOffset);
        }

        // 搴旂敤鍐呭涓績偏移（精敤浜庡榻愯创鍥句腑闈炲眳涓殑瀹為檯鍐呭�?
        float[] contentCenter = bone.getCurrentDirectionContentCenter();
        if (contentCenter != null && (contentCenter[0] != 0f || contentCenter[1] != 0f)) {
            // 鍐呭涓績偏移相对浜巕uad鐨勫昂瀵革紝闇€瑕佷箻浠ュ疄闄呭�?
            // 偏移鍊艰寖四-0.5 �?0.5锛岄渶瑕佷箻浠idth鍜宧eight杞崲涓轰笘鐣岀┖闂磋窛�?
            Vector3f contentOffset = new Vector3f(
                contentCenter[0] * width,  // X方向偏移
                contentCenter[1] * height, // Y方向偏移
                0f                          // Z方向涓嶅亸绉伙紙quad是D平面�?
            );

            // 灏嗗眬閮ㄥ潗鏍囩郴涓殑偏移杞崲鍒颁笘鐣屽潗鏍囩郴
            Vector3f rotatedContentOffset = worldRot.mult(contentOffset);
            finalPos.addLocal(rotatedContentOffset);
        }

        // 搴旂敤浼樺厛绾у亸绉伙紙濮嬬粓娌挎憚鍍忔満瑙嗙嚎方向（
        // 【优先级渲染顺序控制】
        // 已移除位置偏移系统，改用多边形偏移（Polygon Offset）实现25段分层
        // 优势：
        // 1. 保持所有部件在同一位置，阴影完美对齐
        // 2. 通过深度缓冲操作强制渲染顺序，不受视角影响
        // 3. 1-25层永远不会超过26-50层，实现绝对分层
        // 详见setPolyOffset()调用处的实现

        // 濡傛灉鏈夎嚜瀹氫箟旋转涓旀湁涓績鐐癸紝闇€瑕佸洿缁曚腑蹇冪偣旋转部件
        if (hasCustomRotation && !pivotPoint.equals(Vector3f.ZERO)) {
            // 涓績鐐瑰湪涓栫晫绌洪棿鐨勪綅置
            Vector3f rotatedPivot = worldRot.mult(pivotPoint);
            Vector3f pivotWorldPos = worldPos.add(rotatedPivot);

            // 部件相对浜庝腑蹇冪偣鐨勫亸�?
            Vector3f toPart = finalPos.subtract(pivotWorldPos);

            // 将世界坐标系向量转换到局部坐标系
            Quaternion worldRotInv = worldRot.inverse();
            Vector3f toPartLocal = worldRotInv.mult(toPart);

            // 在局部坐标系中应用自定义旋转
            Vector3f rotatedToPartLocal = customRot.mult(toPartLocal);

            // 转换回世界坐标系
            Vector3f rotatedToPart = worldRot.mult(rotatedToPartLocal);

            // 鏈€缁堜綅置= 涓績点+ 旋转鍚庣殑偏�?
            finalPos = pivotWorldPos.add(rotatedToPart);
        }

        // 鏍规嵁Billboard妯″紡鍜岃嚜瀹氫箟旋转鏉ュ喅瀹氭棆杞柟�?
        // 浼樺厛使用Bone鐨勭嫭绔媌illboard璁剧疆锛岀劧鍚庢墠鏄叏灞€Billboard妯″紡
        PuppetRenderer.BillboardMode billboardMode = (parentRenderer != null) ?
            parentRenderer.getBillboardMode() : PuppetRenderer.BillboardMode.UNIFIED;

        // 妫€鏌one鐨勭嫭绔媌illboard璁剧�?
        boolean useBillboard = bone.isBillboardEnabled();

        if (hasCustomRotation) {
            // 鏈夎嚜瀹氫箟旋转鏃讹紝绂佺敤billboard鎺у埗锛屼娇鐢ㄦ垜浠殑旋�?
            billboardControl.setEnabled(false);
            partGeometry.setLocalRotation(finalRot);
        } else if (!useBillboard) {
            // 濡傛灉Bone绂佺敤浜哹illboard锛屽垯淇濇寔鍥哄畾鏈濆悜（D妯″紡�?
            billboardControl.setEnabled(false);
            // 使用骨骼鐨勪笘鐣屾棆杞紝涓嶅仛浠讳綍billboard璋冩�?
            partGeometry.setLocalRotation(worldRot);
        } else if (billboardMode == PuppetRenderer.BillboardMode.DISABLED) {
            // 鍏ㄥ眬绂佺敤Billboard妯″紡锛氶儴浠朵繚鎸佸浐瀹氭湞鍚戯紝閫傚�?D绔嬩綋妯″�?
            billboardControl.setEnabled(false);
            // 使用骨骼鐨勪笘鐣屾棆杞紝涓嶅仛浠讳綍billboard璋冩�?
            partGeometry.setLocalRotation(worldRot);
        } else if (billboardMode == PuppetRenderer.BillboardMode.UNIFIED) {
            // 统一billboard模式：所有部件使用相同的旋转，像纸人一样整体朝向相机
            // 俯仰角范围内平滑过渡：中间正常billboard，接近顶/底时逐渐过渡到自然竖直朝向，
            // 不再依赖离散的"up"/"down"方向key判断（旋转条状贴图模式没有这些key，也要能正常工作）
            billboardControl.setEnabled(false);

            Quaternion baseBillboardRot = parentRenderer.getUnifiedBillboardRotation();
            Vector3f camPos = app.getCamera().getLocation();
            Vector3f partPos = partGeometry.getWorldTranslation();
            Vector3f toCam = camPos.subtract(partPos);

            Quaternion finalBillboardRot;
            if (toCam.lengthSquared() < 0.0001f) {
                finalBillboardRot = baseBillboardRot;
            } else {
                toCam.normalizeLocal();

                float fullRangeDeg = bone.getBillboardPitchFullRangeDeg();
                float lockDeg = bone.getBillboardPitchLockDeg();
                // 俯仰角：toCam与水平面的夹角，0°=水平看，90°=从正上方/正下方看
                float pitchDeg = FastMath.asin(FastMath.clamp(toCam.y, -1f, 1f)) * FastMath.RAD_TO_DEG;
                float absPitch = Math.abs(pitchDeg);

                if (absPitch <= fullRangeDeg) {
                    // 中间区域：完全billboard，面向摄像机
                    finalBillboardRot = baseBillboardRot;
                } else {
                    // 接近顶/底：计算"自然竖直"朝向（只绕世界Y轴对齐视线水平分量，不跟随俯仰）
                    Quaternion uprightRot = calculateUprightRotation(toCam);

                    if (absPitch >= lockDeg) {
                        // 完全锁定竖直，不再billboard
                        finalBillboardRot = uprightRot;
                    } else {
                        // 过渡区：在billboard和竖直朝向之间平滑插值
                        float t = (absPitch - fullRangeDeg) / (lockDeg - fullRangeDeg);
                        finalBillboardRot = new Quaternion();
                        finalBillboardRot.slerp(baseBillboardRot, uprightRot, t);
                    }
                }
            }

            partGeometry.setLocalRotation(finalBillboardRot);
        } else {
            // 鐙珛billboard妯″紡锛氭瘡涓儴浠剁嫭绔嬫湞鍚戞憚鍍忔満
            billboardControl.setEnabled(true);
            // billboard浼氳嚜鍔ㄨ缃棆杞紝涓嶉渶瑕佹墜鍔ㄨ置
        }

        // 搴旂敤浣嶇疆鍜岀缉改
        // 应用优先度Z偏移（用于分层渲染）
        Vector3f finalPosWithZOffset = finalPos.clone();
        finalPosWithZOffset.z += priorityZOffset;
        partGeometry.setLocalTranslation(finalPosWithZOffset);
        partGeometry.setLocalScale(worldScale);

        // 【25段分层多边形偏移】
        // 根据priority设置多边形偏移，实现绝对分层
        // priority 1-100分为4层，每层25个值
        int currentPriority = bone.isRotationStripEnabled() ? bone.getStripPriority() : bone.getCurrentDirectionPriority();
        int layer = currentPriority / 25;           // 层号：0-3（对应1-25, 26-50, 51-75, 76-100）
        int layerOffset = currentPriority % 25;     // 层内偏移：0-24

        // 基础偏移：每层间隔100.0f，确保层间绝对分离
        // 微调偏移：层内每个priority间隔1.0f，确保层内顺序正确
        // 负值表示向摄像机方向偏移（优先级高的更接近摄像机）
        float baseOffset = -layer * 100.0f;
        float microOffset = -layerOffset * 1.0f;

        // 应用多边形偏移（factor=0表示不使用slope-based offset，只用units）
        partMaterial.getAdditionalRenderState().setPolyOffset(0f, baseOffset + microOffset);

        // 鏇存柊高光浣嶇疆鍜屾棆轴
        updateHighlightTransform(finalPos, finalRot, worldScale, hasCustomRotation);

        // 鏇存柊涓績鐐规爣璁颁綅�?
        updatePivotMarkerPosition();
    }

    /**
     * 鏇存柊高光鐨勪綅缃拰旋�?
     */
    /**
     * 计算"自然竖直"朝向：只绕世界Y轴对齐视线的水平分量，忽略俯仰角。
     * 效果类似jME内置的BillboardControl.Alignment.AxialY——贴图始终保持竖直站立，
     * 转头看着摄像机的水平方向，但不会因为俯仰角而歪倒。
     * 用于Billboard俯仰角范围过渡：接近顶/底视角时，部件不再面向摄像机翻转，而是自然竖立。
     */
    private Quaternion calculateUprightRotation(Vector3f toCam) {
        Vector3f up = Vector3f.UNIT_Y;
        Vector3f horizontalDir = new Vector3f(toCam.x, 0f, toCam.z);

        Vector3f left;
        if (horizontalDir.lengthSquared() < 0.0001f) {
            // 摄像机几乎正上方/正下方，水平方向不可判定，使用固定参考轴保持稳定
            Vector3f referenceAxis = Vector3f.UNIT_Z;
            left = referenceAxis.cross(toCam);
            if (left.lengthSquared() < 0.0001f) {
                referenceAxis = Vector3f.UNIT_X;
                left = referenceAxis.cross(toCam);
                if (left.lengthSquared() < 0.0001f) {
                    left = Vector3f.UNIT_Z;
                } else {
                    left.normalizeLocal();
                }
            } else {
                left.normalizeLocal();
            }
        } else {
            horizontalDir.normalizeLocal();
            left = up.cross(horizontalDir);
            if (left.lengthSquared() < 0.0001f) {
                left = Vector3f.UNIT_X;
            } else {
                left.normalizeLocal();
            }
        }

        Vector3f forward = left.cross(up).normalizeLocal();
        Quaternion uprightRot = new Quaternion();
        uprightRot.fromAxes(left, up, forward);
        return uprightRot;
    }

    private void updateHighlightTransform(Vector3f position, Quaternion rotation, Vector3f scale, boolean hasCustomRotation) {
        if (!initialized || highlightNode == null) {
            return;
        }

        // 高光璺熼殢部件鐨勫彉�?
        highlightNode.setLocalTranslation(position);
        highlightNode.setLocalScale(scale);

        // 鏍规嵁鏄惁鏈夎嚜瀹氫箟旋转鏉ュ喅瀹氶珮鍏夌殑billboard鎺у埗
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

    /**
     * 璁剧疆选中鐘舵�?
     */
    public void setSelected(boolean selected) {
        this.isSelected = selected;
        if (highlightNode != null) {
            highlightNode.setCullHint(selected ?
                Geometry.CullHint.Never :
                Geometry.CullHint.Always);
        }
    }

    /**
     * 鑾峰彇选中鐘舵�?
     */
    public boolean isSelected() {
        return isSelected;
    }

    /**
     * 璁剧疆鍙�?
     */
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

            // 高光鍙湪选中涓斿彲瑙佹椂鏄剧�?
            if (highlightNode != null) {
                highlightNode.setCullHint((visible && isSelected) ?
                    Geometry.CullHint.Never :
                    Geometry.CullHint.Always);
            }

            // 鍚屾椂闅愯棌/鏄剧ず涓績鐐规爣设
            if (pivotMarker != null) {
                pivotMarker.setCullHint(visible ?
                    (showPivotMarker ? Geometry.CullHint.Never : Geometry.CullHint.Always) :
                    Geometry.CullHint.Always);
            }
        }
    }

    /**
     * 娓呯悊璧勬簮
     */
    public void cleanup() {
        if (partGeometry != null) {
            partGeometry.removeFromParent();
        }
        if (modelSpatial != null) {
            modelSpatial.removeFromParent();
        }
        if (highlightNode != null) {
            // 鏇磋缁嗙殑鏃ュ�?

            // 鍏堝己鍒堕殣钘忥紙闃叉浠嶇劧琚覆鏌擄�?
            highlightNode.setCullHint(Spatial.CullHint.Always);

            // 寮哄埗detach鎵€鏈夊瓙鑺傜偣
            highlightNode.detachAllChildren();

            // 浠庣埗鑺傜偣绉婚�?
            highlightNode.removeFromParent();

        }
        if (pivotMarker != null) {
            pivotMarker.removeFromParent();
        }

        // 娓呯┖引�?
        partGeometry = null;
        modelSpatial = null;
        loadedModelPath = null;
        highlightNode = null;
        pivotMarker = null;

        initialized = false;
    }

    /**
     * 鍔ㄦ€佷慨鏀归儴浠跺昂对
     */
    public void setSize(float newWidth, float newHeight) {
        if (!initialized) {
            return;
        }

        this.width = newWidth;
        this.height = newHeight;


        // 閲嶆柊璁剧疆椤剁偣浣嶇疆锛堝眳涓級
        partQuad.setBuffer(com.jme3.scene.VertexBuffer.Type.Position, 3, new float[]{
            -width/2, -height/2, 0,
            width/2, -height/2, 0,
            width/2, height/2, 0,
            -width/2, height/2, 0
        });

        // 鏇存柊杈圭晫
        partQuad.updateBound();

        // 鍚屾椂鏇存柊高光边框大�?
        updateHighlightBorderSize();
    }

    /**
     * 璋冩暣瀹藉�?
     */
    public void adjustWidth(float delta) {
        setSize(Math.max(0.1f, width + delta), height);
    }

    /**
     * 璋冩暣楂樺害
     */
    public void adjustHeight(float delta) {
        setSize(width, Math.max(0.1f, height + delta));
    }

    // ========== Getters ==========

    public Bone getBone() {
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
        return width;
    }

    public void setWidth(float width) {
        this.width = width;

        // 鏇存柊鍑犱綍�?
        if (partQuad != null) {
            partQuad.setBuffer(com.jme3.scene.VertexBuffer.Type.Position, 3, new float[]{
                -width/2, -height/2, 0,
                width/2, -height/2, 0,
                width/2, height/2, 0,
                -width/2, height/2, 0
            });
            partQuad.updateBound();
            updateHighlightBorderSize();
        }
        // 淇濆瓨鍒板綋鍓嶆柟向
        if (bone.isRotationStripEnabled()) {
            bone.setStripWidth(width);
        } else {
            bone.setDirectionWidth(bone.getCurrentDirection(), width);
        }
    }

    public float getHeight() {
        return height;
    }

    public void setHeight(float height) {
        this.height = height;

        // 鏇存柊鍑犱綍�?
        if (partQuad != null) {
            partQuad.setBuffer(com.jme3.scene.VertexBuffer.Type.Position, 3, new float[]{
                -width/2, -height/2, 0,
                width/2, -height/2, 0,
                width/2, height/2, 0,
                -width/2, height/2, 0
            });
            partQuad.updateBound();
            updateHighlightBorderSize();
        }
        // 淇濆瓨鍒板綋鍓嶆柟向
        if (bone.isRotationStripEnabled()) {
            bone.setStripHeight(height);
        } else {
            bone.setDirectionHeight(bone.getCurrentDirection(), height);
        }
    }

    public void setOffset(float offsetX, float offsetY) {
        this.offset.set(offsetX, offsetY, 0f);
        // 淇濆瓨鍒板綋鍓嶆柟向
        if (bone.isRotationStripEnabled()) {
            bone.setStripOffset(offset.x, offset.y, offset.z);
        } else {
            bone.setDirectionOffset(bone.getCurrentDirection(), offset.x, offset.y, offset.z);
        }
    }

    public void setOffsetX(float offsetX) {
        this.offset.x = offsetX;
        // 淇濆瓨鍒板綋鍓嶆柟向
        if (bone.isRotationStripEnabled()) {
            bone.setStripOffset(offset.x, offset.y, offset.z);
        } else {
            bone.setDirectionOffset(bone.getCurrentDirection(), offset.x, offset.y, offset.z);
        }
    }

    public void setOffsetY(float offsetY) {
        this.offset.y = offsetY;
        // 淇濆瓨鍒板綋鍓嶆柟向
        if (bone.isRotationStripEnabled()) {
            bone.setStripOffset(offset.x, offset.y, offset.z);
        } else {
            bone.setDirectionOffset(bone.getCurrentDirection(), offset.x, offset.y, offset.z);
        }
    }

    public void setOffsetZ(float offsetZ) {
        this.offset.z = offsetZ;
        // 淇濆瓨鍒板綋鍓嶆柟向
        if (bone.isRotationStripEnabled()) {
            bone.setStripOffset(offset.x, offset.y, offset.z);
        } else {
            bone.setDirectionOffset(bone.getCurrentDirection(), offset.x, offset.y, offset.z);
        }
    }

    public Vector3f getOffset() {
        return offset.clone();
    }

    // 网格配置鐨刧etter鍜宻etter
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

    /**
     * 鑾峰彇璇ラ儴浠剁殑Billboard鍚敤鐘舵€侊紙浠嶣one璇诲彇（
     */
    public boolean isBillboardEnabled() {
        return bone.isBillboardEnabled();
    }

    /**
     * 璁剧疆璇ラ儴浠剁殑Billboard鍚敤鐘舵€侊紙鍐欏叆Bone�?
     */
    public void setBillboardEnabled(boolean enabled) {
        bone.setBillboardEnabled(enabled);
    }

    /**
     * 鑾峰彇部件鐨勬渶缁堜笘鐣屼綅缃紙鍖呮嫭骨骼鍙樻崲鍜屽亸绉伙級
     */
    public Vector3f getFinalWorldPosition() {
        if (partGeometry == null) {
            // 濡傛灉杩樻病鍒濆鍖栵紝杩斿洖骨骼鐨勪笘鐣屼綅置
            Vector3f worldPos = new Vector3f();
            Quaternion tempRot = new Quaternion();
            Vector3f tempScale = new Vector3f();
            bone.getWorldTransform(worldPos, tempRot, tempScale);
            return worldPos;
        }
        return partGeometry.getWorldTranslation();
    }

    /**
     * 鍒涘缓选中高光锛堜腑绌鸿竟妗嗭�?
     */
    private void createHighlight() {
        // 高光姣斿師濮嬮儴浠剁◢澶?
        float highlightPadding = 0.15f;
        float highlightWidth = width + highlightPadding * 2;
        float highlightHeight = height + highlightPadding * 2;
        float lineThickness = 0.1f;

        // 鍒涘缓高光鑺傜偣
        highlightNode = new Node(bone.getName() + "_Highlight");

        // 鍒涘缓边框鏉愯川（精櫧鑹诧紝涓嶉€忔槑�?
        highlightMaterial = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        highlightMaterial.setColor("Color", new ColorRGBA(1f, 1f, 1f, 1.0f)); // 鐧借壊
        highlightMaterial.getAdditionalRenderState().setDepthWrite(false);
        highlightMaterial.getAdditionalRenderState().setDepthTest(false);

        // 鍒涘�?鏉¤竟妗嗙�?
        // 椤惰�?
        Quad topQuad = new Quad(highlightWidth, lineThickness);
        topLine = new Geometry("TopLine", topQuad);
        topLine.setMaterial(highlightMaterial);
        topLine.setLocalTranslation(-highlightWidth/2, highlightHeight/2 - lineThickness, 0.01f);
        highlightNode.attachChild(topLine);

        // 搴曡�?
        Quad bottomQuad = new Quad(highlightWidth, lineThickness);
        bottomLine = new Geometry("BottomLine", bottomQuad);
        bottomLine.setMaterial(highlightMaterial);
        bottomLine.setLocalTranslation(-highlightWidth/2, -highlightHeight/2, 0.01f);
        highlightNode.attachChild(bottomLine);

        // 宸﹁�?
        Quad leftQuad = new Quad(lineThickness, highlightHeight);
        leftLine = new Geometry("LeftLine", leftQuad);
        leftLine.setMaterial(highlightMaterial);
        leftLine.setLocalTranslation(-highlightWidth/2, -highlightHeight/2, 0.01f);
        highlightNode.attachChild(leftLine);

        // 鍙宠�?
        Quad rightQuad = new Quad(lineThickness, highlightHeight);
        rightLine = new Geometry("RightLine", rightQuad);
        rightLine.setMaterial(highlightMaterial);
        rightLine.setLocalTranslation(highlightWidth/2 - lineThickness, -highlightHeight/2, 0.01f);
        highlightNode.attachChild(rightLine);

        // 璁剧疆渲染闃熷垪
        highlightNode.setQueueBucket(RenderQueue.Bucket.Transparent);

        // 娣诲姞billboard鎺у埗鍒拌妭点
        com.jme3.scene.control.BillboardControl highlightBillboard =
            new com.jme3.scene.control.BillboardControl();
        highlightBillboard.setAlignment(com.jme3.scene.control.BillboardControl.Alignment.Screen);
        highlightNode.addControl(highlightBillboard);

        // 娣诲姞鍒扮埗鑺傜�?
        parentNode.attachChild(highlightNode);

        // 榛樿闅愯棌
        highlightNode.setCullHint(Spatial.CullHint.Always);
    }

    /**
     * 鍒涘缓涓績鐐硅瑙夋爣�?
     */
    private void createPivotMarker() {
        // 鍒涘缓灏忓崄瀛楁爣设
        float markerSize = 0.15f;
        Quad markerQuad = new Quad(markerSize, markerSize);

        // 灞呬�?
        markerQuad.setBuffer(com.jme3.scene.VertexBuffer.Type.Position, 3, new float[]{
            -markerSize/2, -markerSize/2, 0,
            markerSize/2, -markerSize/2, 0,
            markerSize/2, markerSize/2, 0,
            -markerSize/2, markerSize/2, 0
        });

        pivotMarker = new Geometry(bone.getName() + "_Pivot", markerQuad);

        // 鍒涘缓鏉愯川 - 绾㈣壊鍗佸瓧
        Material markerMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        markerMat.setColor("Color", new ColorRGBA(1f, 0f, 0f, 1f)); // 绾㈣�?
        markerMat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        markerMat.getAdditionalRenderState().setDepthTest(false); // 鎬绘槸鏄剧ず鍦ㄥ墠闈?
        pivotMarker.setMaterial(markerMat);

        // 璁剧疆渲染闃熷垪
        pivotMarker.setQueueBucket(RenderQueue.Bucket.Translucent);

        // 娣诲姞billboard鎺у埗
        com.jme3.scene.control.BillboardControl pivotBillboard =
            new com.jme3.scene.control.BillboardControl();
        pivotBillboard.setAlignment(com.jme3.scene.control.BillboardControl.Alignment.Screen);
        pivotMarker.addControl(pivotBillboard);

        // 娣诲姞鍒扮埗鑺傜�?
        parentNode.attachChild(pivotMarker);

        // 榛樿闅愯棌
        pivotMarker.setCullHint(showPivotMarker ?
            Geometry.CullHint.Never : Geometry.CullHint.Always);
    }

    /**
     * 璁剧疆涓績鐐逛綅缃紙相对浜庨儴浠朵腑蹇冿�?
     */
    public void setPivotPoint(float x, float y) {
        this.pivotPoint.set(x, y, 0f);
        updatePivotMarkerPosition();
    }

    /**
     * 璁剧疆涓績鐐逛綅缃紙Vector3f�?
     */
    public void setPivotPoint(Vector3f pivot) {
        this.pivotPoint.set(pivot);
        updatePivotMarkerPosition();
    }

    /**
     * 鑾峰彇涓績鐐逛綅�?
     */
    public Vector3f getPivotPoint() {
        return pivotPoint.clone();
    }

    /**
     * 璁剧疆鏄惁鏄剧ず涓績鐐规爣设
     */
    public void setShowPivotMarker(boolean show) {
        this.showPivotMarker = show;
        if (pivotMarker != null) {
            pivotMarker.setCullHint(show ?
                Geometry.CullHint.Never : Geometry.CullHint.Always);
        }
    }

    /**
     * 璁剧疆鑷畾涔塜杞存棆杞搴︼紙搴︽暟�?璺疯贩鏉夸笂涓嬫憜鍔?
     */
    public void setCustomRotationX(float degrees) {
        this.customRotationX = degrees;
        // 淇濆瓨鍒板綋鍓嶆柟向
        if (bone.isRotationStripEnabled()) {
            bone.setStripRotation(customRotationX, customRotationY, customRotationZ);
        } else {
            bone.setDirectionRotation(bone.getCurrentDirection(), customRotationX, customRotationY, customRotationZ);
        }
    }

    /**
     * 鑾峰彇鑷畾涔塜杞存棆杞搴︼紙搴︽暟�?
     */
    public float getCustomRotationX() {
        return customRotationX;
    }

    /**
     * 璁剧疆鑷畾涔塝杞存棆杞搴︼紙搴︽暟�?左右杞�?
     */
    public void setCustomRotationY(float degrees) {
        this.customRotationY = degrees;
        // 淇濆瓨鍒板綋鍓嶆柟向
        if (bone.isRotationStripEnabled()) {
            bone.setStripRotation(customRotationX, customRotationY, customRotationZ);
        } else {
            bone.setDirectionRotation(bone.getCurrentDirection(), customRotationX, customRotationY, customRotationZ);
        }
    }

    /**
     * 鑾峰彇鑷畾涔塝杞存棆杞搴︼紙搴︽暟�?
     */
    public float getCustomRotationY() {
        return customRotationY;
    }

    /**
     * 璁剧疆鑷畾涔塟杞存棆杞搴︼紙搴︽暟�?平面鍐呮棆轴
     */
    public void setCustomRotationZ(float degrees) {
        this.customRotationZ = degrees;
        // 淇濆瓨鍒板綋鍓嶆柟向
        if (bone.isRotationStripEnabled()) {
            bone.setStripRotation(customRotationX, customRotationY, customRotationZ);
        } else {
            bone.setDirectionRotation(bone.getCurrentDirection(), customRotationX, customRotationY, customRotationZ);
        }
    }

    /**
     * 鑾峰彇鑷畾涔塟杞存棆杞搴︼紙搴︽暟�?
     */
    public float getCustomRotationZ() {
        return customRotationZ;
    }

    /**
     * 璁剧疆贴图旋转角度锛堝害鏁帮紝鏀寔多圈旋转�?
     */
    public void setTextureRotation(float degrees) {
        this.textureRotation = degrees;
        this.useAnimationRotation = true;  // 标记涓轰娇鐢ㄥ姩鐢绘棆�?
        updateTexCoords();
    }

    /**
     * 璁剧疆贴图旋转角度锛堝唴閮ㄦ柟娉曪紝涓嶆敼鍙榰seAnimationRotation鏍囧織（
     * 用于动画系统鍦ㄩ潪鎾斁鐘舵€佷笅搴旂敤鍏抽敭甯у�?
     */
    public void setTextureRotationInternal(float degrees) {
        this.textureRotation = degrees;
        // 涓嶄慨鏀箄seAnimationRotation鏍囧織锛屼繚鎸乁I鎺у埗�?
        updateTexCoords();
    }

    /**
     * 鑾峰彇贴图旋转角度锛堝害鏁帮�?
     */
    public float getTextureRotation() {
        return textureRotation;
    }

    /**
     * 鑾峰彇鏄惁姝ｅ湪使用动画旋�?
     * @return true=使用动画旋转锛宖alse=使用方向旋转
     */
    public boolean isUsingAnimationRotation() {
        return useAnimationRotation;
    }

    /**
     * 璁剧疆鏄惁使用动画旋转
     * @param useAnimationRotation true=使用textureRotation锛宖alse=使用方向旋转
     */
    public void setUseAnimationRotation(boolean useAnimationRotation) {
        this.useAnimationRotation = useAnimationRotation;
        updateTexCoords();
    }

    /**
     * 閲嶇疆动画旋转鏍囧織（精敤浜庡垏鎹㈠洖方向旋转（
     */
    public void resetAnimationRotation() {
        this.useAnimationRotation = false;
        updateTexCoords();
    }

    /**
     * 鏇存柊涓績鐐规爣璁颁綅�?
     */
    private void updatePivotMarkerPosition() {
        if (!initialized || pivotMarker == null) {
            return;
        }

        // 鑾峰彇部件鐨勪笘鐣屽彉据
        Vector3f worldPos = new Vector3f();
        Quaternion worldRot = new Quaternion();
        Vector3f worldScale = new Vector3f();
        bone.getWorldTransform(worldPos, worldRot, worldScale);

        // 璁＄畻涓績鐐圭殑涓栫晫浣嶇�?
        // 鍏堝簲鐢╫ffset锛屽啀搴旂敤pivotPoint
        Vector3f totalOffset = offset.add(pivotPoint);
        Vector3f rotatedOffset = worldRot.mult(totalOffset);
        Vector3f pivotWorldPos = worldPos.add(rotatedOffset);

        pivotMarker.setLocalTranslation(pivotWorldPos);
    }

    /**
     * 鏇存柊纹理坐标锛圲V坐标�?
     *
     * **閲嶈閫昏緫**�?
     * - 鐩存帴使用鐢ㄦ埛鍦ㄩ瑙堜腑閫夋嫨鐨刄V坐标
     * - 涓嶅仛浠讳綍鑷姩璋冩暣锛岀‘淇濋瑙堝拰瀹為檯渲染瀹屽叏一�?
     * - 濡傛灉部件瀹介珮姣斿拰UV閫夋嫨鐨勫儚绱犲尯鍩熷楂樻瘮涓嶄竴鑷达紝纹理浼氱浉搴旀媺浼?鍘嬬�?
     *   锛堣繖鏄敤鎴风殑閫夋嫨锛屼粬浠彲浠ラ€氳繃璋冩暣部件瀹介珮鎴朥V閫夋嫨鏉ユ帶鍒讹�?
     * - 鏀寔贴图旋转锛堟瘡涓柟鍚戝彲浠ユ湁鐙珛鐨勬棆杞搴︼級
     */
    /**
     * 旋转条状贴图：按相机相对该部件的水平夹角(yaw)，从环绕360°的条状贴图上
     * 取样出当前应该显示的那一格，实现"伪3D棱柱"的转身错觉。
     *
     * 三条规矩：
     * 1. 取景框只能停在整数像素格线上（不做半像素插值）
     * 2. 取样是纯拷贝，不做混合/缩放（配合Nearest过滤）
     * 3. 贴图内容宽度必须恰好等于一圈的步数（STEPS_PER_REVOLUTION），不足时右侧
     *    用透明像素在内存中补齐（不修改原文件），保证贴图开启Repeat环绕后能
     *    无缝绕回起点
     *
     * 环形寻址原理：atan2算出的yaw在±180°处会有一次数学上的跳变（从+180°瞬间
     * 变成-180°），对应stepIndex会跳变整整一个周期（STEPS_PER_REVOLUTION步）。
     * 这里不对stepIndex做取模归一化，而是让贴图的S轴开启WrapMode.Repeat——
     * U坐标每变化1.0正好对应贴图绕一圈，跳变前后的UV正好相差整数倍的1.0，
     * GPU按小数部分采样，两个UV落在完全相同的像素上，视觉上是无缝的连续过渡，
     * 不会出现"整张图突然切换"的跳变。
     */
    // 固定规则：摄像机每转DEGREES_PER_STEP度，取景框挪1个像素。360°正好整除成
    // STEPS_PER_REVOLUTION个固定位置，不可配置，不考虑档数/连续模式
    private static final float DEGREES_PER_STEP = 10f;
    private static final int STEPS_PER_REVOLUTION = 360 / (int) DEGREES_PER_STEP; // 36

    private void applyRotationStripUV(Vector3f worldPos) {
        String stripPath = bone.getStripTexturePath();
        if (stripPath == null || stripPath.isEmpty()) {
            return;
        }

        int frameWidthPx = bone.getStripFrameWidthPx();
        int calibrationOffsetPx = bone.getStripCalibrationOffsetPx();

        RotationStripTextureUtil.RingStrip strip =
                RotationStripTextureUtil.getOrCreateRingStrip(app.getAssetManager(), stripPath, STEPS_PER_REVOLUTION);
        if (strip == null) {
            return;
        }

        // 确保材质上贴的是这张（可能是补齐后的）环形贴图，而不是loadTexture()原样加载的那份
        if (texture != strip.texture) {
            texture = strip.texture;
            partMaterial.setTexture("DiffuseMap", texture);
            partMaterial.setColor("Diffuse", ColorRGBA.White);
            partMaterial.setColor("Ambient", ColorRGBA.White);
        }

        // 计算相机相对该部件的水平夹角（yaw），复用PuppetRenderer里同款的水平投影+atan2算法
        Vector3f camPos = app.getCamera().getLocation();
        Vector3f toCam = camPos.subtract(worldPos);
        Vector3f horizontalDir = new Vector3f(toCam.x, 0, toCam.z);
        if (horizontalDir.lengthSquared() < 0.0001f) {
            return; // 相机在部件正上/正下方，水平角度不可判定，保持上一帧的取样
        }
        horizontalDir.normalizeLocal();
        float yawRad = FastMath.atan2(horizontalDir.x, horizontalDir.z);
        float yawDeg = yawRad * FastMath.RAD_TO_DEG;

        int ringWidthPx = strip.widthPx;

        // 每转DEGREES_PER_STEP度挪1个像素，一步只走一个像素。
        // 【关键】不做取模归一化——保留atan2在±180°处的原始跳变（正好是±STEPS_PER_REVOLUTION），
        // 交给贴图的Repeat环绕模式在UV层面无缝吸收这个跳变。
        int stepIndex = Math.round(yawDeg / DEGREES_PER_STEP);
///
        // 校准偏移：把"摄像机朝向0° -> 取景框从像素0开始采样"这条固定规则整体平移
        // calibrationOffsetPx像素。用户在选区面板对准某个朝向手动拖好取景框后点击"校准"
        // 写入这个值，换贴图也不受影响（存在Bone上，不跟着贴图文件走）。偏移和stepIndex
        // 都可能是任意整数（正负不限，超出一圈范围也没关系），Repeat环绕会自动按
        // 周期折算，不需要在CPU侧夹紧或取模。
        int pixelStart = calibrationOffsetPx + stepIndex;

        float u0 = pixelStart / (float) ringWidthPx;
        float u1 = (pixelStart + frameWidthPx) / (float) ringWidthPx;

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
                u0, v0,  // 左下
                u1, v0,  // 右下
                u1, v1,  // 右上
                u0, v1   // 左上
            };
            partQuad.setBuffer(com.jme3.scene.VertexBuffer.Type.TexCoord, 2, texCoords);
        }
    }

    private void updateTexCoords() {
        if (partQuad == null) {
            return;
        }

        // 鐩存帴使用鐢ㄦ埛閫夋嫨鐨刄V坐标锛屼笉鍋氫换浣曡皟数
        float u0 = uvOffsetX;
        float v0 = uvOffsetY;
        float u1 = uvOffsetX + uvScaleX;
        float v1 = uvOffsetY + uvScaleY;

        // 鑾峰彇贴图旋转角�?
        // 濡傛灉标记涓轰娇鐢ㄥ姩鐢绘棆杞紝使用textureRotation锛涘惁鍒欎娇鐢ㄦ柟鍚戣创鍥炬棆轴
        float rotationDegrees;
        if (useAnimationRotation) {
            rotationDegrees = textureRotation;
        } else {
            rotationDegrees = bone.getCurrentDirectionTextureRotation();
        }

        // 濡傛灉鏈夋棆杞紝搴旂敤旋转鍙樻�?
        float[] texCoords;
        if (Math.abs(rotationDegrees) > 0.001f) {
            // 璁＄畻UV涓績点
            float centerU = (u0 + u1) / 2.0f;
            float centerV = (v0 + v1) / 2.0f;

            // 杞崲涓哄姬搴︼紙椤烘椂閽堟棆杞紝鎵€浠ュ彇璐燂級
            float angleRad = (float) Math.toRadians(-rotationDegrees);
            float cos = (float) Math.cos(angleRad);
            float sin = (float) Math.sin(angleRad);

            // 旋转4涓《鐐圭殑UV坐标
            // 宸︿笅觉
            float u0_rot = centerU + (u0 - centerU) * cos - (v0 - centerV) * sin;
            float v0_rot = centerV + (u0 - centerU) * sin + (v0 - centerV) * cos;

            // 鍙充笅觉
            float u1_rot = centerU + (u1 - centerU) * cos - (v0 - centerV) * sin;
            float v1_rot = centerV + (u1 - centerU) * sin + (v0 - centerV) * cos;

            // 鍙充笂觉
            float u2_rot = centerU + (u1 - centerU) * cos - (v1 - centerV) * sin;
            float v2_rot = centerV + (u1 - centerU) * sin + (v1 - centerV) * cos;

            // 宸︿笂觉
            float u3_rot = centerU + (u0 - centerU) * cos - (v1 - centerV) * sin;
            float v3_rot = centerV + (u0 - centerU) * sin + (v1 - centerV) * cos;

            texCoords = new float[]{
                u0_rot, v0_rot,  // 宸︿�?
                u1_rot, v1_rot,  // 鍙充�?
                u2_rot, v2_rot,  // 鍙充�?
                u3_rot, v3_rot   // 宸︿�?
            };
        } else {
            // 鏃犳棆杞紝使用鍘熷UV坐标
            texCoords = new float[]{
                u0, v0,  // 宸︿�?
                u1, v0,  // 鍙充�?
                u1, v1,  // 鍙充�?
                u0, v1   // 宸︿�?
            };
        }

        // 璁剧疆纹理坐标锛堝度涓《鐐癸細宸︿笅銆佸彸涓嬨€佸彸涓娿€佸乏涓婏�?
        partQuad.setBuffer(com.jme3.scene.VertexBuffer.Type.TexCoord, 2, texCoords);
    }

    /**
     * 璁剧疆UV坐标
     * 鍚屾椂淇濆瓨鍒板綋鍓嶆柟鍚戠殑UV鏁版�?
     */
    public void setUV(float offsetX, float offsetY, float scaleX, float scaleY) {
        this.uvOffsetX = offsetX;
        this.uvOffsetY = offsetY;
        this.uvScaleX = scaleX;
        this.uvScaleY = scaleY;
        updateTexCoords();

        // 淇濆瓨鍒板綋鍓嶆柟向
        saveCurrentDirectionUV();
    }

    /**
     * 鑾峰彇UV偏移X
     */
    public float getUvOffsetX() {
        return uvOffsetX;
    }

    /**
     * 璁剧疆UV偏移X
     * 鍚屾椂淇濆瓨鍒板綋鍓嶆柟�?
     */
    public void setUvOffsetX(float uvOffsetX) {
        this.uvOffsetX = uvOffsetX;
        updateTexCoords();
        saveCurrentDirectionUV();
    }

    /**
     * 鑾峰彇UV偏移Y
     */
    public float getUvOffsetY() {
        return uvOffsetY;
    }

    /**
     * 璁剧疆UV偏移Y
     * 鍚屾椂淇濆瓨鍒板綋鍓嶆柟�?
     */
    public void setUvOffsetY(float uvOffsetY) {
        this.uvOffsetY = uvOffsetY;
        updateTexCoords();
        saveCurrentDirectionUV();
    }

    /**
     * 鑾峰彇UV缂╂斁X
     */
    public float getUvScaleX() {
        return uvScaleX;
    }

    /**
     * 璁剧疆UV缂╂斁X
     * 鍚屾椂淇濆瓨鍒板綋鍓嶆柟�?
     */
    public void setUvScaleX(float uvScaleX) {
        this.uvScaleX = uvScaleX;
        updateTexCoords();
        saveCurrentDirectionUV();
    }

    /**
     * 鑾峰彇UV缂╂斁Y
     */
    public float getUvScaleY() {
        return uvScaleY;
    }

    /**
     * 璁剧疆UV缂╂斁Y
     * 鍚屾椂淇濆瓨鍒板綋鍓嶆柟�?
     */
    public void setUvScaleY(float uvScaleY) {
        this.uvScaleY = uvScaleY;
        updateTexCoords();
        saveCurrentDirectionUV();
    }

    /**
     * 鏇存柊高光边框大小
     */
    private void updateHighlightBorderSize() {
        if (topLine == null || bottomLine == null || leftLine == null || rightLine == null) {
            return;
        }

        float highlightPadding = 0.15f;
        float highlightWidth = width + highlightPadding * 2;
        float highlightHeight = height + highlightPadding * 2;
        float lineThickness = 0.1f;

        // 鏇存柊椤惰竟
        ((Quad)topLine.getMesh()).updateGeometry(highlightWidth, lineThickness);
        topLine.setLocalTranslation(-highlightWidth/2, highlightHeight/2 - lineThickness, 0.01f);

        // 鏇存柊搴曡竟
        ((Quad)bottomLine.getMesh()).updateGeometry(highlightWidth, lineThickness);
        bottomLine.setLocalTranslation(-highlightWidth/2, -highlightHeight/2, 0.01f);

        // 鏇存柊宸﹁竟
        ((Quad)leftLine.getMesh()).updateGeometry(lineThickness, highlightHeight);
        leftLine.setLocalTranslation(-highlightWidth/2, -highlightHeight/2, 0.01f);

        // 鏇存柊鍙宠竟
        ((Quad)rightLine.getMesh()).updateGeometry(lineThickness, highlightHeight);
        rightLine.setLocalTranslation(highlightWidth/2 - lineThickness, -highlightHeight/2, 0.01f);
    }

    /**
     * 鍒涘缓一涓眳涓殑Quad mesh锛堝畬鍏ㄦ墜鍔ㄥ垱寤猴紝纭繚椤剁偣鍜孶V瀹屽叏瀵瑰簲（
     *
     * **閲嶈锛歫ME3鐨凲uad椤剁偣椤哄簭鍜岀储引*
     * 使用鏍囧噯鐨勫洓杈瑰舰椤剁偣甯冨眬（
     * 椤剁偣椤哄簭锛歔0]宸︿�? [1]鍙充�? [2]鍙充�? [3]宸︿�?
     * 涓夎褰㈢储寮曪細[0,1,2] �?[0,2,3] 锛堥€嗘椂閽堢粫搴忥�?
     *
     * **OpenGL UV坐标�?�?
     * - U杞达紙妯悜锛夛�?=宸︼�?=�?
     * - V杞达紙绾靛悜锛夛�?=涓嬶�?=�?
     *
     * **部件鍑犱綍坐标系*�?
     * - X杞达紙妯悜锛夛細璐?宸︼紝姝?�?
     * - Y杞达紙绾靛悜锛夛細璐?涓嬶紝姝?�?
     * - Z杞达紙娣卞害锛夛�?=平面
     */
    private Quad createCenteredQuad(float w, float h) {
        // 涓嶄娇鐢≦uad鏋勯€犲嚱鏁帮紝鐩存帴鍒涘缓Mesh
        com.jme3.scene.Mesh mesh = new com.jme3.scene.Mesh();

        // 璁剧疆椤剁偣浣嶇疆锛堝眳涓紝X-Y平面�?
        // 椤剁偣椤哄簭锛歔0]宸︿�? [1]鍙充�? [2]鍙充�? [3]宸︿�?
        float[] positions = new float[]{
            -w/2, -h/2, 0,  // [0] 宸︿笅觉
            w/2, -h/2, 0,   // [1] 鍙充笅觉
            w/2, h/2, 0,    // [2] 鍙充笂觉
            -w/2, h/2, 0    // [3] 宸︿笂觉
        };
        mesh.setBuffer(com.jme3.scene.VertexBuffer.Type.Position, 3, positions);

        // 璁剧疆纹理坐标锛堜弗鏍煎搴擯osition椤剁偣（
        // OpenGL UV: U=0鏄乏锛�?1鏄彸锛�?0鏄笅锛�?1鏄�?
        float[] texCoords = new float[]{
            uvOffsetX, uvOffsetY,                       // [0] 宸︿笅觉→UV宸︿�?
            uvOffsetX + uvScaleX, uvOffsetY,            // [1] 鍙充笅觉→UV鍙充�?
            uvOffsetX + uvScaleX, uvOffsetY + uvScaleY, // [2] 鍙充笂觉→UV鍙充�?
            uvOffsetX, uvOffsetY + uvScaleY             // [3] 宸︿笂觉→UV宸︿�?
        };
        mesh.setBuffer(com.jme3.scene.VertexBuffer.Type.TexCoord, 2, texCoords);

        // 璁剧疆娉曠嚎锛堟湞鍚慫杞存方向锛岄潰鍚戣瀵熻€咃�?
        float[] normals = new float[]{
            0, 0, 1,  // [0]
            0, 0, 1,  // [1]
            0, 0, 1,  // [2]
            0, 0, 1   // [3]
        };
        mesh.setBuffer(com.jme3.scene.VertexBuffer.Type.Normal, 3, normals);

        // 璁剧疆绱㈠紩缂撳啿鍖猴紙瀹氫箟涓や釜涓夎褰級
        // 涓夎当: [0,1,2] 锛堝乏涓嬧啋鍙充笅鈫掑彸涓婏�?
        // 涓夎当: [0,2,3] 锛堝乏涓嬧啋鍙充笂鈫掑乏涓婏�?
        // 閫嗘椂閽堢粫搴忥紙姝ｉ潰鏈濆悜瑙傚療鑰咃�?
        short[] indices = new short[]{
            0, 1, 2,  // 绗竴涓笁瑙掑�?
            0, 2, 3   // 绗簩涓笁瑙掑�?
        };
        mesh.setBuffer(com.jme3.scene.VertexBuffer.Type.Index, 3, indices);

        // 鏇存柊杈圭晫
        mesh.updateBound();

        // 灏哅esh杞崲涓篞uad锛堜负浜嗕繚鎸佺被鍨嬪吋瀹规€э級
        // 瀹為檯涓婃垜浠洿鎺ヨ繑鍥濵esh锛屼絾闇€瑕佷慨鏀筽artQuad鐨勭被鍨?
        // 杩欓噷鎴戜滑鍒涘缓一涓猀uad骞跺鍒舵墍鏈夌紦鍐插尯
        Quad quad = new Quad(1, 1);
        quad.setBuffer(com.jme3.scene.VertexBuffer.Type.Position, 3, positions);
        quad.setBuffer(com.jme3.scene.VertexBuffer.Type.TexCoord, 2, texCoords);
        quad.setBuffer(com.jme3.scene.VertexBuffer.Type.Normal, 3, normals);
        quad.setBuffer(com.jme3.scene.VertexBuffer.Type.Index, 3, indices);
        quad.updateBound();

        return quad;
    }

    /**
     * 璁剧疆鐖舵覆鏌撳櫒引用（精敤浜庣粺一billboard�?
     */
    public void setParentRenderer(PuppetRenderer renderer) {
        this.parentRenderer = renderer;
    }
}

