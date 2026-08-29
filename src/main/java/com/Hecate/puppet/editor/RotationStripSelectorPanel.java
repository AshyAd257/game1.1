package com.Hecate.puppet.editor;

import com.jme3.app.SimpleApplication;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Texture;

/**
 * 旋转条状贴图选区框面板
 *
 * 用于在条状贴图（环绕360°的一整条脸部姿势）上，以整数像素为单位框选出
 * 一格"取景框"的宽高，同时提示当前设置的方向档数是否需要贴图自动补齐。
 *
 * 三条像素画规矩：
 * 1. 缩放只能整数倍（1x/2x/3x...），保证显示时不产生半像素糊边
 * 2. 选区框永远吸附到整数像素边界，不允许停在半个像素上
 * 3. 档数（方向数）与贴图像素宽度的关系在这里实时提示
 */
public class RotationStripSelectorPanel {

    private final SimpleApplication app;
    private final BitmapFont font;
    private final Node rootNode;

    private final int width = 900;
    private final int height = 700;
    private int x, y;

    // 纹理预览
    private Geometry texturePreview;
    private Texture currentTexture;
    private int texWidthPx = 1;
    private int texHeightPx = 1;

    // 整数倍缩放（1x, 2x, 3x, 4x...）
    private int zoomLevel = 1;
    private static final int MIN_ZOOM = 1;
    private static final int MAX_ZOOM = 8;

    // 显示区域（贴图预览实际占用的屏幕像素范围）
    private float displayX, displayY, displayWidth, displayHeight;

    // 选区框（像素单位，永远是整数）
    private int selPixelX = 0;
    private int selPixelY = 0;
    private int selPixelWidth = 32;
    private int selPixelHeight = 32;

    // 固定规则：摄像机每转DEGREES_PER_STEP度，取景框挪1个像素，不可配置
    private static final float DEGREES_PER_STEP = 10f;
    private static final int STEPS_PER_REVOLUTION = 360 / (int) DEGREES_PER_STEP; // 36

    // 拖拽状态
    private boolean isDragging = false;
    private int dragStartMouseX, dragStartMouseY;
    private int dragStartSelX, dragStartSelY;

    // UI元素
    private Geometry modalOverlay;
    private Geometry background;
    private BitmapText titleText;
    private BitmapText infoText;
    private BitmapText paddingWarningText;
    private Node selectionBoxNode;
    private Geometry topLine, bottomLine, leftLine, rightLine;

    private Button closeButton;
    private Button zoomInButton;
    private Button zoomOutButton;
    private Button widthButton;
    private Button heightButton;
    private Button livePreviewButton;
    private Button calibrateButton;
    private BitmapText calibrationInfoText;

    /**
     * 校准回调：用户对准某个摄像机朝向手动拖好取景框后点击"校准"，
     * 面板算出对应的像素偏移，回调方负责把它写回当前部件的Bone
     */
    public interface CalibrationListener {
        void onCalibrate(int calibrationOffsetPx);
    }

    private CalibrationListener calibrationListener;

    // 实时预览：跟随相机高亮当前正被采样的那一格（用青色区分手动选区的黄色框）
    private boolean livePreviewEnabled = false;
    private com.Hecate.puppet.editor.core.EditorPuppetPartRenderer livePreviewPartRenderer;
    private Node livePreviewBoxNode;
    private Geometry liveTopLine, liveBottomLine, liveLeftLine, liveRightLine;
    private BitmapText livePreviewInfoText;

    /**
     * 选区变化回调：像素单位的取景框位置和尺寸
     */
    public interface SelectionChangeListener {
        void onSelectionChanged(int pixelX, int pixelY, int pixelWidth, int pixelHeight);
    }

    private SelectionChangeListener selectionChangeListener;

    public RotationStripSelectorPanel(SimpleApplication app, BitmapFont font) {
        this.app = app;
        this.font = font;
        this.rootNode = new Node("RotationStripSelectorPanel");

        this.x = (app.getCamera().getWidth() - width) / 2;
        this.y = (app.getCamera().getHeight() - height) / 2;

        initialize();
        rootNode.setCullHint(Spatial.CullHint.Always);
    }

    private void initialize() {
        // 模态遮罩
        Quad fullScreenQuad = new Quad(app.getCamera().getWidth(), app.getCamera().getHeight());
        modalOverlay = new Geometry("StripModalOverlay", fullScreenQuad);
        Material overlayMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        overlayMat.setColor("Color", new ColorRGBA(0.0f, 0.0f, 0.0f, 0.7f));
        overlayMat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        modalOverlay.setMaterial(overlayMat);
        modalOverlay.setLocalTranslation(0, 0, 5.0f);
        rootNode.attachChild(modalOverlay);

        // 窗口背景
        Quad bgQuad = new Quad(width, height);
        background = new Geometry("StripSelectorBackground", bgQuad);
        Material bgMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        bgMat.setColor("Color", new ColorRGBA(0.15f, 0.15f, 0.15f, 1.0f));
        background.setMaterial(bgMat);
        background.setLocalTranslation(x, y, 5.1f);
        rootNode.attachChild(background);

        // 标题
        titleText = new BitmapText(font);
        titleText.setText("旋转条状贴图 - 取景框选区（像素单位，整数倍缩放）");
        titleText.setSize(font.getCharSet().getRenderedSize());
        titleText.setColor(ColorRGBA.White);
        titleText.setLocalTranslation(x + 10, y + height - 10, 5.3f);
        rootNode.attachChild(titleText);

        // 关闭按钮
        closeButton = new Button(app, font, "Close", x + width - 110, y + height - 40, 100, 30);
        closeButton.setClickListener(this::hide);
        closeButton.getRootNode().setLocalTranslation(0, 0, 5.3f);
        rootNode.attachChild(closeButton.getRootNode());

        // 缩放按钮
        zoomInButton = new Button(app, font, "Zoom +", x + width - 220, y + height - 40, 100, 30);
        zoomInButton.setClickListener(() -> setZoomLevel(zoomLevel + 1));
        zoomInButton.getRootNode().setLocalTranslation(0, 0, 5.3f);
        rootNode.attachChild(zoomInButton.getRootNode());

        zoomOutButton = new Button(app, font, "Zoom -", x + width - 330, y + height - 40, 100, 30);
        zoomOutButton.setClickListener(() -> setZoomLevel(zoomLevel - 1));
        zoomOutButton.getRootNode().setLocalTranslation(0, 0, 5.3f);
        rootNode.attachChild(zoomOutButton.getRootNode());

        // 实时预览开关：跟随相机高亮当前正被采样的那一格
        livePreviewButton = new Button(app, font, "实时预览: 关", x + width - 460, y + height - 40, 120, 30);
        livePreviewButton.setClickListener(() -> {
            livePreviewEnabled = !livePreviewEnabled;
            livePreviewButton.setText(livePreviewEnabled ? "实时预览: 开" : "实时预览: 关");
            livePreviewBoxNode.setCullHint(livePreviewEnabled ? Spatial.CullHint.Never : Spatial.CullHint.Always);
            livePreviewInfoText.setCullHint(livePreviewEnabled ? Spatial.CullHint.Never : Spatial.CullHint.Always);
        });
        livePreviewButton.getRootNode().setLocalTranslation(0, 0, 5.3f);
        rootNode.attachChild(livePreviewButton.getRootNode());

        // 校准按钮：把当前取景框位置和当前摄像机朝向的对应关系写入部件（换贴图也保留这个关系）
        calibrateButton = new Button(app, font, "校准当前朝向", x + width - 600, y + height - 40, 130, 30);
        calibrateButton.setClickListener(this::performCalibration);
        calibrateButton.getRootNode().setLocalTranslation(0, 0, 5.3f);
        rootNode.attachChild(calibrateButton.getRootNode());

        calibrationInfoText = new BitmapText(font);
        calibrationInfoText.setSize(font.getCharSet().getRenderedSize() * 0.8f);
        calibrationInfoText.setColor(new ColorRGBA(1.0f, 0.85f, 0.2f, 1.0f));
        calibrationInfoText.setLocalTranslation(x + 10, y + height - 115, 5.3f);
        rootNode.attachChild(calibrationInfoText);

        // 宽/高数字输入按钮（点击弹出JOptionPane）。旋转规则固定为"每转24度挪1像素"，不可配置
        widthButton = new Button(app, font, "框宽: " + selPixelWidth + "px", x + 10, y + 10, 150, 30);
        widthButton.setClickListener(() -> openNumericInputDialog("取景框宽度(像素)", selPixelWidth, 1, 4096, value -> {
            selPixelWidth = Math.max(1, (int) value);
            widthButton.setText("框宽: " + selPixelWidth + "px");
            clampSelectionToTexture();
            updateSelectionBox();
            updateInfoText();
            notifySelectionChanged();
        }));
        widthButton.getRootNode().setLocalTranslation(0, 0, 5.3f);
        rootNode.attachChild(widthButton.getRootNode());

        heightButton = new Button(app, font, "框高: " + selPixelHeight + "px", x + 170, y + 10, 150, 30);
        heightButton.setClickListener(() -> openNumericInputDialog("取景框高度(像素)", selPixelHeight, 1, 4096, value -> {
            selPixelHeight = Math.max(1, (int) value);
            heightButton.setText("框高: " + selPixelHeight + "px");
            clampSelectionToTexture();
            updateSelectionBox();
            updateInfoText();
            notifySelectionChanged();
        }));
        heightButton.getRootNode().setLocalTranslation(0, 0, 5.3f);
        rootNode.attachChild(heightButton.getRootNode());

        // 贴图预览
        Quad texQuad = new Quad(1, 1);
        texturePreview = new Geometry("StripTexturePreview", texQuad);
        Material texMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        texMat.setColor("Color", ColorRGBA.White);
        texMat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        texturePreview.setMaterial(texMat);
        texturePreview.setLocalTranslation(x + 10, y + 50, 5.2f);
        rootNode.attachChild(texturePreview);

        // 选区框（4条线）
        selectionBoxNode = new Node("StripSelectionBox");
        Material lineMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        lineMat.setColor("Color", new ColorRGBA(1.0f, 0.8f, 0.0f, 1.0f)); // 醒目的黄色
        int lineThickness = 2;
        topLine = new Geometry("StripTopLine", new Quad(1, lineThickness));
        topLine.setMaterial(lineMat);
        selectionBoxNode.attachChild(topLine);
        bottomLine = new Geometry("StripBottomLine", new Quad(1, lineThickness));
        bottomLine.setMaterial(lineMat);
        selectionBoxNode.attachChild(bottomLine);
        leftLine = new Geometry("StripLeftLine", new Quad(lineThickness, 1));
        leftLine.setMaterial(lineMat);
        selectionBoxNode.attachChild(leftLine);
        rightLine = new Geometry("StripRightLine", new Quad(lineThickness, 1));
        rightLine.setMaterial(lineMat);
        selectionBoxNode.attachChild(rightLine);
        selectionBoxNode.setLocalTranslation(0, 0, 5.25f);
        rootNode.attachChild(selectionBoxNode);

        // 实时预览框（4条线，青色，跟随相机高亮当前采样格，与手动选区的黄色框区分）
        livePreviewBoxNode = new Node("StripLivePreviewBox");
        Material liveLineMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        liveLineMat.setColor("Color", new ColorRGBA(0.2f, 1.0f, 1.0f, 1.0f)); // 醒目的青色
        liveTopLine = new Geometry("StripLiveTopLine", new Quad(1, lineThickness));
        liveTopLine.setMaterial(liveLineMat);
        livePreviewBoxNode.attachChild(liveTopLine);
        liveBottomLine = new Geometry("StripLiveBottomLine", new Quad(1, lineThickness));
        liveBottomLine.setMaterial(liveLineMat);
        livePreviewBoxNode.attachChild(liveBottomLine);
        liveLeftLine = new Geometry("StripLiveLeftLine", new Quad(lineThickness, 1));
        liveLeftLine.setMaterial(liveLineMat);
        livePreviewBoxNode.attachChild(liveLeftLine);
        liveRightLine = new Geometry("StripLiveRightLine", new Quad(lineThickness, 1));
        liveRightLine.setMaterial(liveLineMat);
        livePreviewBoxNode.attachChild(liveRightLine);
        livePreviewBoxNode.setLocalTranslation(0, 0, 5.26f);
        livePreviewBoxNode.setCullHint(Spatial.CullHint.Always); // 默认隐藏，开关控制
        rootNode.attachChild(livePreviewBoxNode);

        livePreviewInfoText = new BitmapText(font);
        livePreviewInfoText.setSize(font.getCharSet().getRenderedSize() * 0.85f);
        livePreviewInfoText.setColor(new ColorRGBA(0.2f, 1.0f, 1.0f, 1.0f));
        livePreviewInfoText.setLocalTranslation(x + 10, y + height - 90, 5.3f);
        livePreviewInfoText.setCullHint(Spatial.CullHint.Always);
        rootNode.attachChild(livePreviewInfoText);

        // 信息文本
        infoText = new BitmapText(font);
        infoText.setSize(font.getCharSet().getRenderedSize() * 0.85f);
        infoText.setColor(ColorRGBA.White);
        infoText.setLocalTranslation(x + 10, y + height - 40, 5.3f);
        rootNode.attachChild(infoText);

        // 补齐提示文本（贴图不够宽时用醒目颜色提示）
        paddingWarningText = new BitmapText(font);
        paddingWarningText.setSize(font.getCharSet().getRenderedSize() * 0.85f);
        paddingWarningText.setColor(new ColorRGBA(1.0f, 0.6f, 0.2f, 1.0f));
        paddingWarningText.setLocalTranslation(x + 10, y + height - 65, 5.3f);
        rootNode.attachChild(paddingWarningText);

        updateInfoText();
    }

    /**
     * 设置实时预览要跟踪的部件渲染器（用于读取骨骼世界位置，计算相机相对它的实时夹角）
     */
    public void setLivePreviewTarget(com.Hecate.puppet.editor.core.EditorPuppetPartRenderer partRenderer) {
        this.livePreviewPartRenderer = partRenderer;
    }

    /**
     * 每帧调用：如果实时预览开启，跟随相机重新计算当前正被采样的那一格并高亮显示
     */
    public void update(float tpf) {
        closeButton.update(tpf);
        zoomInButton.update(tpf);
        zoomOutButton.update(tpf);
        livePreviewButton.update(tpf);
        widthButton.update(tpf);
        heightButton.update(tpf);
        calibrateButton.update(tpf);

        if (!livePreviewEnabled || !isVisible() || livePreviewPartRenderer == null || texWidthPx <= 0) {
            return;
        }

        com.jme3.math.Vector3f partPos = livePreviewPartRenderer.getFinalWorldPosition();
        com.jme3.math.Vector3f camPos = app.getCamera().getLocation();
        com.jme3.math.Vector3f toCam = camPos.subtract(partPos);
        com.jme3.math.Vector3f horizontalDir = new com.jme3.math.Vector3f(toCam.x, 0f, toCam.z);
        if (horizontalDir.lengthSquared() < 0.0001f) {
            return;
        }
        horizontalDir.normalizeLocal();
        float yawRad = com.jme3.math.FastMath.atan2(horizontalDir.x, horizontalDir.z);
        float yawDeg = yawRad * com.jme3.math.FastMath.RAD_TO_DEG;

        // 用与运行时applyRotationStripUV()相同的固定公式：每转DEGREES_PER_STEP度挪1像素。
        // 环形寻址：贴图宽度固定为STEPS_PER_REVOLUTION（配合Repeat环绕），实际采样时
        // stepIndex不取模，交给贴图环绕在UV层面无缝吸收±180°处的跳变。但这里是给人看的
        // 调试高亮框，需要落在贴图预览区域的可见范围内，所以单独对显示坐标做一次
        // 周期折算（不影响运行时真正的采样公式）。
        int ringWidthPx = STEPS_PER_REVOLUTION;

        int stepIndex = Math.round(yawDeg / DEGREES_PER_STEP);
        int pixelStart = currentCalibrationOffsetPx + stepIndex; // 步长恒为1像素，叠加已保存的校准偏移
        int displayPixelStart = ((pixelStart % ringWidthPx) + ringWidthPx) % ringWidthPx; // 仅用于显示定位，折算回[0, ringWidthPx)

        updateLivePreviewBox(displayPixelStart, ringWidthPx, yawDeg);
    }

    // 当前部件已保存的校准偏移（打开面板时由调用方通过setCalibrationOffsetPx()同步进来，
    // 用于让实时预览框的位置和运行时applyRotationStripUV()保持一致）
    private int currentCalibrationOffsetPx = 0;

    /**
     * 设置当前部件已保存的校准偏移（供实时预览框计算使用，不影响手动选区selPixelX/Y）
     */
    public void setCalibrationOffsetPx(int calibrationOffsetPx) {
        this.currentCalibrationOffsetPx = calibrationOffsetPx;
    }

    public void setCalibrationListener(CalibrationListener listener) {
        this.calibrationListener = listener;
    }

    /**
     * 校准：把"当前摄像机对该部件的水平朝向"和"当前手动选区selPixelX"的对应关系算出来，
     * 即 calibrationOffsetPx = selPixelX - stepIndex(当前朝向)。
     * 之后运行时采样公式 pixelStart = calibrationOffsetPx + stepIndex(朝向) 在这个朝向下
     * 正好等于selPixelX，也就是校准时用户手动摆好的位置。
     */
    private void performCalibration() {
        if (livePreviewPartRenderer == null) {
            calibrationInfoText.setText("校准失败：未指定要跟踪的部件");
            return;
        }

        com.jme3.math.Vector3f partPos = livePreviewPartRenderer.getFinalWorldPosition();
        com.jme3.math.Vector3f camPos = app.getCamera().getLocation();
        com.jme3.math.Vector3f toCam = camPos.subtract(partPos);
        com.jme3.math.Vector3f horizontalDir = new com.jme3.math.Vector3f(toCam.x, 0f, toCam.z);
        if (horizontalDir.lengthSquared() < 0.0001f) {
            calibrationInfoText.setText("校准失败：摄像机正上/正下方，水平朝向不可判定");
            return;
        }
        horizontalDir.normalizeLocal();
        float yawRad = com.jme3.math.FastMath.atan2(horizontalDir.x, horizontalDir.z);
        float yawDeg = yawRad * com.jme3.math.FastMath.RAD_TO_DEG;

        int stepIndex = Math.round(yawDeg / DEGREES_PER_STEP);
        stepIndex = ((stepIndex % STEPS_PER_REVOLUTION) + STEPS_PER_REVOLUTION) % STEPS_PER_REVOLUTION;

        int newCalibrationOffsetPx = selPixelX - stepIndex;
        currentCalibrationOffsetPx = newCalibrationOffsetPx;

        calibrationInfoText.setText(String.format(
            "已校准：当前朝向 %.1f° <-> 取景框起点 %dpx（偏移量 %dpx）",
            yawDeg, selPixelX, newCalibrationOffsetPx
        ));

        if (calibrationListener != null) {
            calibrationListener.onCalibrate(newCalibrationOffsetPx);
        }
    }

    /**
     * 根据当前实时采样的像素起点，更新青色高亮框的屏幕位置
     * 注意：如果补齐后的贴图比预览面板里显示的原图更宽，高亮框的像素坐标系是"补齐后"的，
     * 超出原图宽度部分（透明补齐区）会显示在贴图预览区域之外，这是预期行为，用于提示补齐范围。
     */
    private void updateLivePreviewBox(int pixelStart, int paddedWidthPx, float yawDeg) {
        float boxX = displayX + pixelStart * zoomLevel;
        float boxY = displayY + displayHeight - selPixelHeight * zoomLevel;
        float boxWidth = selPixelWidth * zoomLevel;
        float boxHeight = selPixelHeight * zoomLevel;

        int lineThickness = 2;
        float z = 5.27f;

        liveTopLine.setMesh(new Quad(boxWidth, lineThickness));
        liveTopLine.setLocalTranslation(boxX, boxY + boxHeight - lineThickness, z);

        liveBottomLine.setMesh(new Quad(boxWidth, lineThickness));
        liveBottomLine.setLocalTranslation(boxX, boxY, z);

        liveLeftLine.setMesh(new Quad(lineThickness, boxHeight));
        liveLeftLine.setLocalTranslation(boxX, boxY, z);

        liveRightLine.setMesh(new Quad(lineThickness, boxHeight));
        liveRightLine.setLocalTranslation(boxX + boxWidth - lineThickness, boxY, z);

        livePreviewInfoText.setText(String.format(
            "实时预览: 相机夹角 %.1f° | 采样起点 %dpx（补齐后总宽 %dpx）",
            yawDeg, pixelStart, paddedWidthPx
        ));
    }

    /**
     * 设置要预览的贴图，读取真实像素尺寸
     */
    public void setTexture(Texture texture) {
        this.currentTexture = texture;
        if (texture == null || texture.getImage() == null) {
            return;
        }

        texWidthPx = texture.getImage().getWidth();
        texHeightPx = texture.getImage().getHeight();

        Material mat = texturePreview.getMaterial();
        mat.setTexture("ColorMap", texture);
        mat.setColor("Color", ColorRGBA.White);

        // 自动选一个能撑满显示区域的整数缩放倍数，避免像素画贴图显示得过小
        zoomLevel = calculateBestFitZoom();

        clampSelectionToTexture();
        rebuildTexturePreviewQuad();
        updateSelectionBox();
        updateInfoText();
    }

    /**
     * 计算能让贴图尽量撑满可用显示区域、同时保持整数倍（不糊边）的缩放级别
     */
    private int calculateBestFitZoom() {
        // 可用显示区域：面板宽度减边距，面板高度减去顶部按钮/标题栏和底部信息文本占用的空间
        int availableWidth = width - 20;
        int availableHeight = height - 140;

        if (texWidthPx <= 0 || texHeightPx <= 0) {
            return 1;
        }

        int maxZoomByWidth = Math.max(1, availableWidth / texWidthPx);
        int maxZoomByHeight = Math.max(1, availableHeight / texHeightPx);
        int bestZoom = Math.min(maxZoomByWidth, maxZoomByHeight);

        return Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, bestZoom));
    }

    /**
     * 设置初始选区（像素单位）和当前档数，供打开面板时从EditorBone拉取当前值
     */
    public void setSelection(int pixelX, int pixelY, int pixelWidth, int pixelHeight) {
        this.selPixelX = Math.max(0, pixelX);
        this.selPixelY = Math.max(0, pixelY);
        this.selPixelWidth = Math.max(1, pixelWidth);
        this.selPixelHeight = Math.max(1, pixelHeight);

        clampSelectionToTexture();
        widthButton.setText("框宽: " + selPixelWidth + "px");
        heightButton.setText("框高: " + selPixelHeight + "px");
        updateSelectionBox();
        updateInfoText();
    }

    private void rebuildTexturePreviewQuad() {
        float dispWidth = texWidthPx * zoomLevel;
        float dispHeight = texHeightPx * zoomLevel;

        Quad newQuad = new Quad(dispWidth, dispHeight);
        newQuad.setBuffer(com.jme3.scene.VertexBuffer.Type.TexCoord, 2, new float[]{
            0, 0,
            1, 0,
            1, 1,
            0, 1
        });
        texturePreview.setMesh(newQuad);

        this.displayX = x + 10;
        this.displayY = y + 50;
        this.displayWidth = dispWidth;
        this.displayHeight = dispHeight;
        texturePreview.setLocalTranslation(displayX, displayY, 5.2f);
    }

    /**
     * 设置整数倍缩放（1x/2x/3x/4x...），保证像素显示不糊边
     */
    private void setZoomLevel(int newZoom) {
        this.zoomLevel = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, newZoom));
        rebuildTexturePreviewQuad();
        updateSelectionBox();
    }

    /**
     * 确保选区永远落在贴图像素范围内（不做任何裁剪之外的调整，只夹紧边界）
     */
    private void clampSelectionToTexture() {
        selPixelWidth = Math.max(1, Math.min(selPixelWidth, Math.max(1, texWidthPx)));
        selPixelHeight = Math.max(1, Math.min(selPixelHeight, Math.max(1, texHeightPx)));
        selPixelX = Math.max(0, Math.min(selPixelX, Math.max(0, texWidthPx - selPixelWidth)));
        selPixelY = Math.max(0, Math.min(selPixelY, Math.max(0, texHeightPx - selPixelHeight)));
    }

    /**
     * 更新选区框的屏幕位置（像素坐标 * 整数缩放 + 显示区域偏移）
     */
    private void updateSelectionBox() {
        float boxX = displayX + selPixelX * zoomLevel;
        // 贴图纹理坐标V轴从下到上，屏幕Y轴也是从下到上，但像素坐标习惯从上到下数
        // 这里选区框的Y以贴图左上角为原点向下量度，转换为屏幕坐标时要翻转
        float boxYFromTop = selPixelY * zoomLevel;
        float boxY = displayY + displayHeight - boxYFromTop - selPixelHeight * zoomLevel;
        float boxWidth = selPixelWidth * zoomLevel;
        float boxHeight = selPixelHeight * zoomLevel;

        int lineThickness = 2;
        float z = 5.25f;

        topLine.setMesh(new Quad(boxWidth, lineThickness));
        topLine.setLocalTranslation(boxX, boxY + boxHeight - lineThickness, z);

        bottomLine.setMesh(new Quad(boxWidth, lineThickness));
        bottomLine.setLocalTranslation(boxX, boxY, z);

        leftLine.setMesh(new Quad(lineThickness, boxHeight));
        leftLine.setLocalTranslation(boxX, boxY, z);

        rightLine.setMesh(new Quad(lineThickness, boxHeight));
        rightLine.setLocalTranslation(boxX + boxWidth - lineThickness, boxY, z);
    }

    private void updateInfoText() {
        // 环形寻址：贴图开启Repeat环绕后无缝转一圈，要求贴图内容宽度恰好等于
        // STEPS_PER_REVOLUTION（一步一像素）。不是"至少多宽"，宽了会浪费未使用的列，
        // 窄了会被透明像素补齐——两种情况都不是"正好绕回原点"，都要提示。
        int requiredWidth = STEPS_PER_REVOLUTION;

        infoText.setText(String.format(
            "贴图: %dx%d px | 缩放: %dx | 取景框: (%d, %d) %dx%d px | 每转%.0f°挪1像素，一圈%d个位置",
            texWidthPx, texHeightPx, zoomLevel, selPixelX, selPixelY, selPixelWidth, selPixelHeight,
            DEGREES_PER_STEP, STEPS_PER_REVOLUTION
        ));

        if (texWidthPx != requiredWidth) {
            if (texWidthPx < requiredWidth) {
                int missing = requiredWidth - texWidthPx;
                paddingWarningText.setText(String.format(
                    "贴图宽 %d px，环形无缝旋转要求正好 %d px，右侧将用透明像素自动填充 %d px（会出现拼接痕迹）",
                    texWidthPx, requiredWidth, missing
                ));
            } else {
                int extra = texWidthPx - requiredWidth;
                paddingWarningText.setText(String.format(
                    "贴图宽 %d px，环形无缝旋转要求正好 %d px，右侧多出的 %d px 不会被使用",
                    texWidthPx, requiredWidth, extra
                ));
            }
            paddingWarningText.setCullHint(Spatial.CullHint.Never);
        } else {
            paddingWarningText.setCullHint(Spatial.CullHint.Always);
        }
    }

    private void notifySelectionChanged() {
        if (selectionChangeListener != null) {
            selectionChangeListener.onSelectionChanged(selPixelX, selPixelY, selPixelWidth, selPixelHeight);
        }
    }

    /**
     * 打开数值输入对话框（复用EnlargedTexturePreviewPanel里的既有约定：Swing对话框 + app.enqueue回主线程）
     */
    private interface NumericInputCallback {
        void onValueEntered(float value);
    }

    private void openNumericInputDialog(String title, float currentValue, float minValue, float maxValue, NumericInputCallback callback) {
        app.enqueue(() -> {
            try {
                String input = javax.swing.JOptionPane.showInputDialog(
                    null,
                    String.format("请输入数值 (%.0f - %.0f):", minValue, maxValue),
                    title,
                    javax.swing.JOptionPane.PLAIN_MESSAGE
                );

                if (input != null && !input.trim().isEmpty()) {
                    try {
                        float value = Float.parseFloat(input.trim());
                        value = Math.max(minValue, Math.min(maxValue, value));
                        final float finalValue = value;

                        app.enqueue(() -> {
                            callback.onValueEntered(finalValue);
                            return null;
                        });
                    } catch (NumberFormatException e) {
                        javax.swing.JOptionPane.showMessageDialog(
                            null,
                            "输入格式无效，请输入有效的数字。",
                            "错误",
                            javax.swing.JOptionPane.ERROR_MESSAGE
                        );
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        });
    }

    // ========== 鼠标交互：拖拽选区框，永远吸附到整数像素 ==========

    public boolean handleMousePress(int mouseX, int mouseY) {
        if (closeButton.handleMouseClick(mouseX, mouseY)) return true;
        if (zoomInButton.handleMouseClick(mouseX, mouseY)) return true;
        if (zoomOutButton.handleMouseClick(mouseX, mouseY)) return true;
        if (livePreviewButton.handleMouseClick(mouseX, mouseY)) return true;
        if (widthButton.handleMouseClick(mouseX, mouseY)) return true;
        if (heightButton.handleMouseClick(mouseX, mouseY)) return true;
        if (calibrateButton.handleMouseClick(mouseX, mouseY)) return true;

        if (isPointInDisplay(mouseX, mouseY)) {
            isDragging = true;
            dragStartMouseX = mouseX;
            dragStartMouseY = mouseY;
            dragStartSelX = selPixelX;
            dragStartSelY = selPixelY;
            return true;
        }
        return false;
    }

    public boolean handleMouseDrag(int mouseX, int mouseY) {
        if (!isDragging) {
            return false;
        }

        // 屏幕像素差 / 缩放倍数 = 贴图像素差，四舍五入后永远是整数像素
        int deltaScreenX = mouseX - dragStartMouseX;
        int deltaScreenY = mouseY - dragStartMouseY;
        int deltaPixelX = Math.round(deltaScreenX / (float) zoomLevel);
        // Y轴屏幕坐标向上为正，像素坐标向下为正，取反
        int deltaPixelY = -Math.round(deltaScreenY / (float) zoomLevel);

        selPixelX = dragStartSelX + deltaPixelX;
        selPixelY = dragStartSelY + deltaPixelY;
        clampSelectionToTexture();

        updateSelectionBox();
        updateInfoText();
        return true;
    }

    public boolean handleMouseRelease(int mouseX, int mouseY) {
        if (isDragging) {
            isDragging = false;
            notifySelectionChanged();
            return true;
        }
        return false;
    }

    /**
     * 滚轮缩放（整数步进，不做任意浮点缩放）
     */
    public boolean handleMouseScroll(int mouseX, int mouseY, float scrollAmount) {
        if (!isPointInDisplay(mouseX, mouseY)) {
            return false;
        }
        if (scrollAmount < 0) {
            setZoomLevel(zoomLevel + 1);
        } else {
            setZoomLevel(zoomLevel - 1);
        }
        return true;
    }

    private boolean isPointInDisplay(int mouseX, int mouseY) {
        return mouseX >= displayX && mouseX <= displayX + displayWidth &&
               mouseY >= displayY && mouseY <= displayY + displayHeight;
    }

    // ========== 显示/隐藏 ==========

    public void show() {
        rootNode.setCullHint(Spatial.CullHint.Never);
    }

    public void hide() {
        rootNode.setCullHint(Spatial.CullHint.Always);
        isDragging = false;
    }

    public boolean isVisible() {
        return rootNode.getCullHint() == Spatial.CullHint.Never;
    }

    // ========== Getters/Setters ==========

    public Node getRootNode() {
        return rootNode;
    }

    public void setSelectionChangeListener(SelectionChangeListener listener) {
        this.selectionChangeListener = listener;
    }

    public int getSelPixelX() {
        return selPixelX;
    }

    public int getSelPixelY() {
        return selPixelY;
    }

    public int getSelPixelWidth() {
        return selPixelWidth;
    }

    public int getSelPixelHeight() {
        return selPixelHeight;
    }
}
