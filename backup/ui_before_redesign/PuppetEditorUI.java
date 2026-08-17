package com.Hecate.puppet.editor;

import com.jme3.app.SimpleApplication;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Node;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Quad;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.Hecate.puppet.Skeleton;
import com.Hecate.puppet.Bone;
import java.io.File;

/**
 * 木偶编辑器UI主界面
 * 管理Timeline、Inspector等面板
 */
public class PuppetEditorUI {

    private final SimpleApplication app;
    private final Node guiNode;
    private BitmapFont guiFont;
    private TTFontLoader ttfLoader; // TTF字体加载器

    // UI组件
    private Node editorRootNode;
    private TimelinePanel timelinePanel;
    private InspectorPanel inspectorPanel;
    private PartListPanel partListPanel;
    private DirectionTexturePanel directionTexturePanel;
    private BitmapText currentDirectionText;

    // 编辑器状态
    private Skeleton currentSkeleton;
    private Bone selectedBone;
    private float currentTime = 0f;
    private boolean visible = false;

    // 布局参数
    private int screenWidth;
    private int screenHeight;
    private final int timelineHeight = 150;
    private final int partListWidth = 300;  // 左侧第一栏：部件列表宽度
    private final int inspectorWidth = 450;  // 左侧第二栏：Inspector宽度（包含所有控件）
    private final int directionPanelWidth = 300;  // 左侧第三栏：方向纹理面板宽度

    public PuppetEditorUI(SimpleApplication app) {
        this.app = app;
        this.guiNode = app.getGuiNode();

        // 尝试加载TTF字体
        // 优先级1: 从resources加载自定义字体
        String[] resourceFonts = {
            "Interface/Fonts/ZLabsBitmap_12px_CN（简体中文）.ttf",  // ZLabs像素字体
            "Fonts/CustomFont.ttf",
            "Fonts/ChineseFont.ttf"
        };

        for (String resourcePath : resourceFonts) {
            this.ttfLoader = TTFontLoader.loadFontFromResource(app.getAssetManager(), resourcePath, 16f);
            if (this.ttfLoader != null) {
                break;
            }
        }

        // 优先级2: 从系统字体目录加载（Windows）
        if (this.ttfLoader == null) {
            String[] systemFontPaths = {
                "C:/Windows/Fonts/msyh.ttc",      // 微软雅黑
                "C:/Windows/Fonts/simhei.ttf",    // 黑体
                "C:/Windows/Fonts/simsun.ttc"     // 宋体
            };

            for (String fontPath : systemFontPaths) {
                File fontFile = new File(fontPath);
                if (fontFile.exists()) {
                    this.ttfLoader = TTFontLoader.loadFont(app.getAssetManager(), fontPath, 16f);
                    if (this.ttfLoader != null) {
                        break;
                    }
                }
            }
        }

        // Fallback to BitmapFont if TTF loading fails
        try {
            this.guiFont = app.getAssetManager().loadFont("Interface/Fonts/ChineseFont.fnt");
        } catch (Exception e) {
            this.guiFont = app.getAssetManager().loadFont("Interface/Fonts/Default.fnt");
        }

        this.screenWidth = app.getCamera().getWidth();
        this.screenHeight = app.getCamera().getHeight();

        initializeUI();
    }

    /**
     * 初始化UI
     */
    private void initializeUI() {
        // 创建编辑器根节点
        editorRootNode = new Node("PuppetEditorUI");

        // 创建背景
        createBackground();

        // 创建标题
        createTitle();

        // 创建方向显示
        createDirectionDisplay();

        // 创建Timeline面板
        timelinePanel = new TimelinePanel(app, guiFont, 0, 0, screenWidth, timelineHeight);
        editorRootNode.attachChild(timelinePanel.getRootNode());

        // 创建部件列表面板（左侧第一栏）
        partListPanel = new PartListPanel(app, guiFont, 0, timelineHeight, partListWidth, screenHeight - timelineHeight);
        editorRootNode.attachChild(partListPanel.getRootNode());

        // 创建Inspector面板（左侧第二栏，包含所有滑条和按钮）
        inspectorPanel = new InspectorPanel(app, guiFont, partListWidth, timelineHeight,
                                           inspectorWidth, screenHeight - timelineHeight);
        editorRootNode.attachChild(inspectorPanel.getRootNode());

        // 创建多方向纹理管理面板（左侧第三栏）
        createDirectionTexturePanel();

        // 默认隐藏
        editorRootNode.setCullHint(com.jme3.scene.Spatial.CullHint.Always);
    }

    /**
     * 创建背景
     */
    private void createBackground() {
        // Timeline背景
        Quad timelineQuad = new Quad(screenWidth, timelineHeight);
        Geometry timelineBg = new Geometry("TimelineBackground", timelineQuad);
        Material timelineMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        timelineMat.setColor("Color", new ColorRGBA(0.2f, 0.2f, 0.2f, 0.9f));
        timelineMat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        timelineBg.setMaterial(timelineMat);
        timelineBg.setLocalTranslation(0, screenHeight - timelineHeight, 0);
        editorRootNode.attachChild(timelineBg);

        // Inspector背景（左侧第二栏）
        Quad inspectorQuad = new Quad(inspectorWidth, screenHeight - timelineHeight);
        Geometry inspectorBg = new Geometry("InspectorBackground", inspectorQuad);
        Material inspectorMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        inspectorMat.setColor("Color", new ColorRGBA(0.15f, 0.15f, 0.15f, 0.9f));
        inspectorMat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        inspectorBg.setMaterial(inspectorMat);
        inspectorBg.setLocalTranslation(partListWidth, 0, 0);
        editorRootNode.attachChild(inspectorBg);

        // 部件列表背景 - 由PartListPanel自己管理
    }

    /**
     * 创建标题
     */
    private void createTitle() {
        if (ttfLoader != null) {
            Node titleNode = ttfLoader.createText("木偶编辑器 (Press I to toggle)", ColorRGBA.White);
            titleNode.setLocalTranslation(10, screenHeight - 10, 0);
            titleNode.setLocalScale(1.2f);
            editorRootNode.attachChild(titleNode);
        } else {
            BitmapText titleText = new BitmapText(guiFont);
            titleText.setText("Puppet Editor (Press I to toggle)");
            titleText.setSize(guiFont.getCharSet().getRenderedSize() * 3.0f);
            titleText.setColor(ColorRGBA.White);
            titleText.setLocalTranslation(10, screenHeight - 10, 0);
            editorRootNode.attachChild(titleText);
        }
    }

    /**
     * 创建方向显示
     */
    private void createDirectionDisplay() {
        currentDirectionText = new BitmapText(guiFont);
        currentDirectionText.setText("Current View: FRONT");
        currentDirectionText.setSize(guiFont.getCharSet().getRenderedSize() * 2.5f);
        currentDirectionText.setColor(new ColorRGBA(1.0f, 0.8f, 0.2f, 1.0f)); // 金黄色
        // 放在屏幕顶部中央，z坐标设置得高一些确保显示在最上层
        currentDirectionText.setLocalTranslation(screenWidth / 2 - 180, screenHeight - 50, 10);
        editorRootNode.attachChild(currentDirectionText);
    }

    /**
     * 设置当前编辑的骨架
     */
    public void setSkeleton(Skeleton skeleton) {
        this.currentSkeleton = skeleton;
        inspectorPanel.clear();
        if (partListPanel != null) {
            partListPanel.setSkeleton(skeleton);
        }
    }

    /**
     * 选择骨骼
     */
    public void selectBone(Bone bone) {
        this.selectedBone = bone;
        inspectorPanel.setBone(bone, null);
    }

    /**
     * 选择骨骼（带渲染器）
     */
    public void selectBone(Bone bone, com.Hecate.puppet.PuppetPartRenderer partRenderer) {
        this.selectedBone = bone;
        inspectorPanel.setBone(bone, partRenderer);
        if (partListPanel != null) {
            partListPanel.setSelectedBone(bone);
        }
        if (directionTexturePanel != null) {
            directionTexturePanel.setBone(bone);
        }
    }

    /**
     * 更新Inspector显示
     */
    public void updateInspector() {
        if (inspectorPanel != null) {
            inspectorPanel.updateDisplay();
        }
    }

    /**
     * 更新编辑器
     */
    public void update(float tpf) {
        if (!visible) return;

        currentTime += tpf;
        timelinePanel.setTime(currentTime);
    }

    /**
     * 更新按钮状态（独立于时间轴）
     */
    public void updateButtons(float tpf) {
        if (!visible) return;

        // 更新Inspector面板中的按钮
        inspectorPanel.update(tpf);

        // 更新Timeline的锁定计时器
        timelinePanel.update(tpf);

        // 更新多方向纹理管理面板
        if (directionTexturePanel != null) {
            directionTexturePanel.update(tpf);
        }
    }

    /**
     * 回调接口，用于通知编辑器状态变化
     */
    public interface EditorCallbacks {
        void onEditorOpened();
    }
    private EditorCallbacks editorCallbacks;

    public void setEditorCallbacks(EditorCallbacks callbacks) {
        this.editorCallbacks = callbacks;
    }

    /**
     * 切换显示/隐藏
     */
    public void toggleVisibility() {
        visible = !visible;

        if (visible) {
            // 打开编辑器时，检查窗口大小是否变化（例如全屏切换）
            int currentWidth = app.getCamera().getWidth();
            int currentHeight = app.getCamera().getHeight();
            if (currentWidth != screenWidth || currentHeight != screenHeight) {
                reshapeUI(currentWidth, currentHeight);
            }

            editorRootNode.setCullHint(com.jme3.scene.Spatial.CullHint.Never);
            guiNode.attachChild(editorRootNode);

            // 通知编辑器已打开（用于重新设置回调）
            if (editorCallbacks != null) {
                editorCallbacks.onEditorOpened();
            }
        } else {
            editorRootNode.setCullHint(com.jme3.scene.Spatial.CullHint.Always);
            editorRootNode.removeFromParent();
        }
    }

    /**
     * 设置可见性
     */
    public void setVisible(boolean visible) {
        if (this.visible != visible) {
            toggleVisibility();
        }
    }

    /**
     * 创建多方向纹理管理面板（左侧第三栏）
     */
    private void createDirectionTexturePanel() {
        int panelHeight = screenHeight - timelineHeight - 20;
        int panelX = partListWidth + inspectorWidth + 10; // 在Inspector面板右侧
        int panelY = 10;

        directionTexturePanel = new DirectionTexturePanel(app, guiFont, panelX, panelY, directionPanelWidth, panelHeight);
        editorRootNode.attachChild(directionTexturePanel.getRootNode());
    }


    /**
     * 响应窗口大小变化
     */
    public void reshapeUI(int newWidth, int newHeight) {
        this.screenWidth = newWidth;
        this.screenHeight = newHeight;

        // 保存当前选中的骨骼和渲染器
        Bone oldSelectedBone = selectedBone;
        com.Hecate.puppet.PuppetPartRenderer oldPartRenderer =
            (inspectorPanel != null) ? inspectorPanel.getCurrentPartRenderer() : null;

        // 保存部件列表面板的位置（如果被拖动过）
        int oldPartListX = (partListPanel != null) ? partListPanel.getX() : 0;
        int oldPartListY = (partListPanel != null) ? partListPanel.getY() : timelineHeight;

        // 清除旧的UI
        editorRootNode.detachAllChildren();

        // 重新创建UI
        createBackground();
        createTitle();

        // 重新创建Timeline面板
        timelinePanel = new TimelinePanel(app, guiFont, 0, 0, screenWidth, timelineHeight);
        editorRootNode.attachChild(timelinePanel.getRootNode());

        // 重新创建部件列表面板（保持之前的位置）
        partListPanel = new PartListPanel(app, guiFont, oldPartListX, oldPartListY, partListWidth, screenHeight - timelineHeight);
        editorRootNode.attachChild(partListPanel.getRootNode());
        if (currentSkeleton != null) {
            partListPanel.setSkeleton(currentSkeleton);
        }

        // 重新创建Inspector面板（左侧第二栏）
        inspectorPanel = new InspectorPanel(app, guiFont, partListWidth, timelineHeight,
                                           inspectorWidth, screenHeight - timelineHeight);
        editorRootNode.attachChild(inspectorPanel.getRootNode());

        // 重新创建多方向纹理管理面板（左侧第三栏）
        createDirectionTexturePanel();

        // 恢复选中的骨骼
        if (oldSelectedBone != null) {
            selectBone(oldSelectedBone, oldPartRenderer);
        }

        // 重新设置回调（因为创建了新的 InspectorPanel）
        if (editorCallbacks != null) {
            editorCallbacks.onEditorOpened();
        }
    }

    /**
     * 清理资源
     */
    public void cleanup() {
        if (editorRootNode != null) {
            editorRootNode.removeFromParent();
        }
    }

    // ========== Getters ==========

    public boolean isVisible() {
        return visible;
    }

    public Skeleton getCurrentSkeleton() {
        return currentSkeleton;
    }

    public Bone getSelectedBone() {
        return selectedBone;
    }

    public float getCurrentTime() {
        return currentTime;
    }

    public void setCurrentTime(float time) {
        this.currentTime = time;
    }

    public InspectorPanel getInspectorPanel() {
        return inspectorPanel;
    }

    public TimelinePanel getTimelinePanel() {
        return timelinePanel;
    }

    public PartListPanel getPartListPanel() {
        return partListPanel;
    }

    public DirectionTexturePanel getDirectionTexturePanel() {
        return directionTexturePanel;
    }

    /**
     * 更新当前方向显示
     */
    public void updateCurrentDirection(Bone.Direction direction) {
        if (currentDirectionText != null) {
            currentDirectionText.setText("Current View: " + direction.name());
        }
    }
}
