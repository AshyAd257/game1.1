package com.Hecate.puppet.editor;

import com.jme3.app.SimpleApplication;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Node;
import com.Hecate.puppet.editor.core.EditorBone;
import com.Hecate.puppet.editor.core.EditorSkeleton;

import java.util.ArrayList;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

/**
 * 部件列表面板
 * 显示所有部件的层级结构
 */
public class PartListPanel {

    private final SimpleApplication app;
    private final BitmapFont font;
    private final Node rootNode;

    private BitmapText titleText;
    private List<PartListItem> partItems;
    private EditorSkeleton skeleton;
    private EditorBone selectedBone;
    private com.jme3.scene.Geometry backgroundGeometry;
    private com.jme3.scene.Geometry titleBarGeometry;  // 标题栏背景（可拖动区域提示）

    // 镜像管理器引用
    private MirrorManager mirrorManager;

    // Shift键状态（用于多选）
    private boolean shiftPressed = false;

    // 回调接口
    public interface PartListCallbacks {
        void onPartSelected(EditorBone bone);
        void onPartSelected(EditorBone bone, boolean shiftPressed);  // 带Shift状态的版本
        void onPartRenamed(EditorBone bone, String newName);
    }
    private PartListCallbacks callbacks;

    private int x, y;  // 改为可变，支持拖动
    private final int width, height;

    // 拖动相关
    private boolean isDragging = false;
    private int dragStartX = 0;
    private int dragStartY = 0;
    private int panelStartX = 0;
    private int panelStartY = 0;
    private final int titleBarHeight = 30;

    // 双击检测
    private long lastClickTime = 0;
    private EditorBone lastClickedBone = null;
    private static final long DOUBLE_CLICK_INTERVAL = 300; // 300毫秒

    // 滚动相关
    private int scrollOffset = 0;  // 滚动偏移量（像素）
    private int totalContentHeight = 0;  // 内容总高度
    private int visibleHeight;  // 可见区域高度
    private com.jme3.scene.Geometry scrollbarTrack;  // 滚动条轨道
    private com.jme3.scene.Geometry scrollbarThumb;  // 滚动条滑块
    private boolean isDraggingScrollbar = false;  // 是否正在拖动滚动条
    private int scrollbarDragStartY = 0;
    private int scrollbarDragStartOffset = 0;
    private static final int SCROLLBAR_WIDTH = 12;  // 滚动条宽度
    private static final int ITEM_HEIGHT = 50;  // 每个列表项的高度

    // 部件列表项
    private static class PartListItem {
        BitmapText text;
        EditorBone bone;
        int depth; // 层级深度
        com.jme3.scene.Geometry background; // 可点击的背景
        Node mirrorBorderNode; // 镜像彩色边框节点（可选）

        PartListItem(BitmapText text, EditorBone bone, int depth, com.jme3.scene.Geometry background, Node mirrorBorderNode) {
            this.text = text;
            this.bone = bone;
            this.depth = depth;
            this.background = background;
            this.mirrorBorderNode = mirrorBorderNode;
        }
    }

    public PartListPanel(SimpleApplication app, BitmapFont font, int x, int y, int width, int height) {
        this.app = app;
        this.font = font;
        this.x = x;
        // y是从屏幕顶部开始的偏移（timelineHeight），需要转换为GUI坐标
        // GUI坐标系：原点在左下角，Y轴向上
        // 面板底部应该在GUI坐标的0，顶部在screenHeight-y
        this.y = y;  // 暂时保存为屏幕坐标（从顶部算）
        this.width = width;
        this.height = height;

        this.rootNode = new Node("PartListPanel");
        this.partItems = new ArrayList<>();
        initializePanel();
    }

    /**
     * 初始化面板
     */
    private void initializePanel() {
        int screenHeight = app.getCamera().getHeight();

        // 计算可见区域高度（面板高度减去标题栏）
        visibleHeight = height - titleBarHeight - 10;

        // 创建背景
        // 面板高度固定，位置根据y值调整
        com.jme3.scene.shape.Quad bgQuad = new com.jme3.scene.shape.Quad(width, height);
        backgroundGeometry = new com.jme3.scene.Geometry("PartListBackground", bgQuad);
        com.jme3.material.Material bgMat = new com.jme3.material.Material(
            app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        bgMat.setColor("Color", new ColorRGBA(0.15f, 0.15f, 0.15f, 0.9f));
        bgMat.getAdditionalRenderState().setBlendMode(com.jme3.material.RenderState.BlendMode.Alpha);
        backgroundGeometry.setMaterial(bgMat);

        // 计算面板底部的GUI坐标
        // 面板顶部在GUI坐标 (screenHeight - y)，底部在 (screenHeight - y - height)
        float panelBottomGuiY = Math.max(0, (screenHeight - y) - height);
        backgroundGeometry.setLocalTranslation(x, panelBottomGuiY, -1);
        rootNode.attachChild(backgroundGeometry);

        // 创建标题栏背景（浅色，提示可拖动）
        com.jme3.scene.shape.Quad titleBarQuad = new com.jme3.scene.shape.Quad(width, titleBarHeight);
        titleBarGeometry = new com.jme3.scene.Geometry("PartListTitleBar", titleBarQuad);
        com.jme3.material.Material titleBarMat = new com.jme3.material.Material(
            app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        titleBarMat.setColor("Color", new ColorRGBA(0.08f, 0.08f, 0.1f, 0.95f));  // 深色背景
        titleBarMat.getAdditionalRenderState().setBlendMode(com.jme3.material.RenderState.BlendMode.Alpha);
        titleBarGeometry.setMaterial(titleBarMat);

        // 标题栏位于面板顶部
        float titleBarBottomGuiY = (screenHeight - y) - titleBarHeight;
        titleBarGeometry.setLocalTranslation(x, titleBarBottomGuiY, -0.5f);
        rootNode.attachChild(titleBarGeometry);

        int currentY = screenHeight - y - 20;

        // 标题
        titleText = new BitmapText(font);
        titleText.setSize(font.getCharSet().getRenderedSize() * 1.5f);
        titleText.setColor(ColorRGBA.Yellow);
        titleText.setLocalTranslation(x + 10, currentY, 0);
        rootNode.attachChild(titleText);

        // 创建滚动条
        createScrollbar();
    }

    /**
     * 创建滚动条
     */
    private void createScrollbar() {
        int screenHeight = app.getCamera().getHeight();
        float panelBottomGuiY = Math.max(0, (screenHeight - y) - height);

        // 滚动条轨道（右侧边缘）
        com.jme3.scene.shape.Quad trackQuad = new com.jme3.scene.shape.Quad(SCROLLBAR_WIDTH, visibleHeight);
        scrollbarTrack = new com.jme3.scene.Geometry("ScrollbarTrack", trackQuad);
        com.jme3.material.Material trackMat = new com.jme3.material.Material(
            app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        trackMat.setColor("Color", new ColorRGBA(0.3f, 0.3f, 0.35f, 0.9f));
        trackMat.getAdditionalRenderState().setBlendMode(com.jme3.material.RenderState.BlendMode.Alpha);
        scrollbarTrack.setMaterial(trackMat);
        scrollbarTrack.setLocalTranslation(x + width - SCROLLBAR_WIDTH - 2, panelBottomGuiY + 5, 2f);
        rootNode.attachChild(scrollbarTrack);

        // 滚动条滑块（初始大小，后续会更新）
        com.jme3.scene.shape.Quad thumbQuad = new com.jme3.scene.shape.Quad(SCROLLBAR_WIDTH - 4, 50);
        scrollbarThumb = new com.jme3.scene.Geometry("ScrollbarThumb", thumbQuad);
        com.jme3.material.Material thumbMat = new com.jme3.material.Material(
            app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        thumbMat.setColor("Color", new ColorRGBA(0.6f, 0.6f, 0.7f, 1.0f));
        scrollbarThumb.setMaterial(thumbMat);
        scrollbarThumb.setLocalTranslation(x + width - SCROLLBAR_WIDTH, panelBottomGuiY + visibleHeight - 50, 3f);
        rootNode.attachChild(scrollbarThumb);
    }

    /**
     * 设置骨架并刷新列表
     */
    public void setSkeleton(EditorSkeleton skeleton) {
        this.skeleton = skeleton;
        refreshPartList();
    }

    /**
     * 刷新部件列表
     */
    public void refreshPartList() {
        // 清除旧的列表项
        for (PartListItem item : partItems) {
            item.text.removeFromParent();
            item.background.removeFromParent();
            if (item.mirrorBorderNode != null) {
                item.mirrorBorderNode.removeFromParent();
            }
        }
        partItems.clear();

        if (skeleton == null) {
            totalContentHeight = 0;
            updateScrollbar();
            return;
        }

        // 先计算总内容高度（不创建UI）
        int itemCount = countAllBones(skeleton);
        totalContentHeight = itemCount * ITEM_HEIGHT;

        int screenHeight = app.getCamera().getHeight();
        // 内容区域顶部（标题栏下方）
        int contentTopY = screenHeight - y - titleBarHeight - 10;
        // 应用滚动偏移
        int currentY = contentTopY + scrollOffset;

        // 从根骨骼开始递归构建列表（有层级关系的骨骼）
        EditorBone rootBone = skeleton.getRootBone();
        if (rootBone != null) {
            currentY = buildPartListRecursive(rootBone, 0, currentY, contentTopY);
        }

        // 添加独立骨骼（没有父骨骼，且不是根骨骼的）
        java.util.Set<EditorBone> processedBones = new java.util.HashSet<>();
        if (rootBone != null) {
            collectProcessedBones(rootBone, processedBones);
        }

        for (EditorBone bone : skeleton.getAllBones()) {
            if (!processedBones.contains(bone) && bone.getParent() == null) {
                currentY = buildPartListRecursive(bone, 0, currentY, contentTopY);
            }
        }

        // 更新滚动条
        updateScrollbar();
    }

    /**
     * 计算所有骨骼数量
     */
    private int countAllBones(EditorSkeleton skel) {
        return skel.getAllBones().size();
    }

    /**
     * 收集已处理的骨骼（在骨骼树中的）
     */
    private void collectProcessedBones(EditorBone bone, java.util.Set<EditorBone> processed) {
        processed.add(bone);
        for (EditorBone child : bone.getChildren()) {
            collectProcessedBones(child, processed);
        }
    }

    /**
     * 递归构建部件列表（带内容裁剪）
     */
    private int buildPartListRecursive(EditorBone bone, int depth, int startY, int contentTopY) {
        int currentY = startY;
        int screenHeight = app.getCamera().getHeight();
        float panelBottomGuiY = Math.max(0, (screenHeight - y) - height);

        // 计算可见区域边界
        float visibleTop = contentTopY;
        float visibleBottom = panelBottomGuiY + 5;

        // 创建可点击的背景条
        float itemHeight = 40;
        float itemTopY = currentY;
        float itemBottomY = currentY - itemHeight + 5;

        // 只有在可见区域内才创建UI元素
        boolean isVisible = (itemTopY > visibleBottom && itemBottomY < visibleTop);

        com.jme3.scene.Geometry itemBackground = new com.jme3.scene.Geometry("ItemBg_" + bone.getName(),
            new com.jme3.scene.shape.Quad(width - SCROLLBAR_WIDTH - 10, itemHeight));
        com.jme3.material.Material bgMat = new com.jme3.material.Material(
            app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        bgMat.setColor("Color", new ColorRGBA(0.2f, 0.2f, 0.2f, isVisible ? 0.5f : 0f));
        bgMat.getAdditionalRenderState().setBlendMode(com.jme3.material.RenderState.BlendMode.Alpha);
        itemBackground.setMaterial(bgMat);
        itemBackground.setLocalTranslation(x + 2, itemBottomY, -0.5f);
        rootNode.attachChild(itemBackground);

        // 创建文本
        BitmapText text = new BitmapText(font);

        // 根据深度添加缩进
        String indent = "";
        for (int i = 0; i < depth; i++) {
            indent += "  "; // 两个空格作为缩进
        }

        text.setText(indent + bone.getName());
        text.setSize(font.getCharSet().getRenderedSize() * 1.5f);
        text.setColor(isVisible ? ColorRGBA.White : new ColorRGBA(1, 1, 1, 0));
        text.setLocalTranslation(x + 10, currentY, 0);
        rootNode.attachChild(text);

        // 检查是否有镜像关系，如果有则创建彩色边框
        Node mirrorBorderNode = null;
        if (mirrorManager != null && mirrorManager.hasMirror(bone)) {
            ColorRGBA mirrorColor = mirrorManager.getMirrorColor(bone);

            // 创建边框节点
            mirrorBorderNode = new Node("MirrorBorder_" + bone.getName());

            // 创建边框几何体（只包围文本区域，不要太大）
            float textWidth = text.getLineWidth();
            float textHeight = text.getLineHeight();
            float borderThickness = 2f;  // 边框厚度

            // 创建一个简单的边框（使用半透明背景色）
            // 外框（彩色）
            com.jme3.scene.shape.Quad outerQuad = new com.jme3.scene.shape.Quad(textWidth + 6, textHeight + 4);
            com.jme3.scene.Geometry outerBorder = new com.jme3.scene.Geometry("MirrorBorderOuter", outerQuad);
            com.jme3.material.Material outerMat = new com.jme3.material.Material(
                app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
            outerMat.setColor("Color", new ColorRGBA(mirrorColor.r, mirrorColor.g, mirrorColor.b, 0.6f));
            outerMat.getAdditionalRenderState().setBlendMode(com.jme3.material.RenderState.BlendMode.Alpha);
            outerBorder.setMaterial(outerMat);
            outerBorder.setLocalTranslation(x + 7, currentY - textHeight - 1, 0.05f);
            mirrorBorderNode.attachChild(outerBorder);

            // 内框（透明，形成边框效果）
            com.jme3.scene.shape.Quad innerQuad = new com.jme3.scene.shape.Quad(textWidth + 2, textHeight);
            com.jme3.scene.Geometry innerBorder = new com.jme3.scene.Geometry("MirrorBorderInner", innerQuad);
            com.jme3.material.Material innerMat = new com.jme3.material.Material(
                app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
            innerMat.setColor("Color", new ColorRGBA(0.2f, 0.2f, 0.2f, 0.5f));
            innerMat.getAdditionalRenderState().setBlendMode(com.jme3.material.RenderState.BlendMode.Alpha);
            innerBorder.setMaterial(innerMat);
            innerBorder.setLocalTranslation(x + 9, currentY - textHeight + 1, 0.06f);
            mirrorBorderNode.attachChild(innerBorder);

            // 将边框节点添加到根节点
            rootNode.attachChild(mirrorBorderNode);
        }

        // 创建列表项
        PartListItem item = new PartListItem(text, bone, depth, itemBackground, mirrorBorderNode);
        partItems.add(item);

        currentY -= ITEM_HEIGHT;

        // 递归处理子骨骼
        for (EditorBone child : bone.getChildren()) {
            currentY = buildPartListRecursive(child, depth + 1, currentY, contentTopY);
        }

        return currentY;
    }

    /**
     * 更新滚动条大小和位置
     */
    private void updateScrollbar() {
        if (scrollbarThumb == null || scrollbarTrack == null) return;

        int screenHeight = app.getCamera().getHeight();
        float panelBottomGuiY = Math.max(0, (screenHeight - y) - height);

        // 如果内容不需要滚动，隐藏滚动条滑块
        if (totalContentHeight <= visibleHeight) {
            scrollbarThumb.setCullHint(com.jme3.scene.Spatial.CullHint.Always);
            return;
        }

        scrollbarThumb.setCullHint(com.jme3.scene.Spatial.CullHint.Never);

        // 计算滑块大小（与可见区域成比例）
        float thumbHeight = Math.max(30, (float) visibleHeight / totalContentHeight * visibleHeight);

        // 计算滑块位置（根据滚动偏移）
        int maxScrollOffset = totalContentHeight - visibleHeight;
        float scrollRatio = (float) scrollOffset / maxScrollOffset;
        float thumbY = panelBottomGuiY + 5 + (visibleHeight - thumbHeight) * (1 - scrollRatio);

        // 重建滑块几何体
        scrollbarThumb.removeFromParent();
        com.jme3.scene.shape.Quad thumbQuad = new com.jme3.scene.shape.Quad(SCROLLBAR_WIDTH - 4, thumbHeight);
        scrollbarThumb = new com.jme3.scene.Geometry("ScrollbarThumb", thumbQuad);
        com.jme3.material.Material thumbMat = new com.jme3.material.Material(
            app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        thumbMat.setColor("Color", new ColorRGBA(0.5f, 0.5f, 0.6f, 1.0f));
        scrollbarThumb.setMaterial(thumbMat);
        scrollbarThumb.setLocalTranslation(x + width - SCROLLBAR_WIDTH, thumbY, 1f);
        rootNode.attachChild(scrollbarThumb);
    }

    /**
     * 处理鼠标滚轮滚动
     */
    public boolean handleMouseScroll(int mouseX, int mouseY, int scrollAmount) {
        // 检查鼠标是否在面板区域内
        int screenHeight = app.getCamera().getHeight();
        float panelBottomGuiY = Math.max(0, (screenHeight - y) - height);
        float panelTopGuiY = screenHeight - y;

        if (mouseX >= x && mouseX <= x + width &&
            mouseY >= panelBottomGuiY && mouseY <= panelTopGuiY) {

            // 滚动（每次滚动30像素）
            scroll(scrollAmount * 30);
            return true;
        }
        return false;
    }

    /**
     * 滚动指定像素量
     */
    private void scroll(int delta) {
        int maxScrollOffset = Math.max(0, totalContentHeight - visibleHeight);
        scrollOffset = Math.max(0, Math.min(maxScrollOffset, scrollOffset + delta));
        refreshPartList();
    }

    /**
     * 检查是否点击在滚动条上
     */
    public boolean handleScrollbarClick(int mouseX, int mouseY) {
        if (scrollbarThumb == null || totalContentHeight <= visibleHeight) return false;

        int screenHeight = app.getCamera().getHeight();
        float panelBottomGuiY = Math.max(0, (screenHeight - y) - height);

        // 检查是否点击在滚动条轨道区域
        float trackLeft = x + width - SCROLLBAR_WIDTH - 2;
        float trackRight = x + width;
        float trackBottom = panelBottomGuiY + 5;
        float trackTop = trackBottom + visibleHeight;

        if (mouseX >= trackLeft && mouseX <= trackRight &&
            mouseY >= trackBottom && mouseY <= trackTop) {

            isDraggingScrollbar = true;
            scrollbarDragStartY = mouseY;
            scrollbarDragStartOffset = scrollOffset;
            return true;
        }
        return false;
    }

    /**
     * 处理滚动条拖动
     */
    public void handleScrollbarDrag(int mouseX, int mouseY) {
        if (!isDraggingScrollbar) return;

        int deltaY = mouseY - scrollbarDragStartY;
        int maxScrollOffset = Math.max(0, totalContentHeight - visibleHeight);

        // 滑块向上移动（deltaY为正）对应内容向下滚动（scrollOffset增加）
        float scrollRatio = -(float) deltaY / visibleHeight;
        int newOffset = scrollbarDragStartOffset + (int) (scrollRatio * maxScrollOffset);

        scrollOffset = Math.max(0, Math.min(maxScrollOffset, newOffset));
        refreshPartList();
    }

    /**
     * 处理滚动条释放
     */
    public void handleScrollbarRelease() {
        isDraggingScrollbar = false;
    }

    /**
     * 是否正在拖动滚动条
     */
    public boolean isDraggingScrollbar() {
        return isDraggingScrollbar;
    }

    /**
     * 设置选中的骨骼（高亮显示）
     */
    public void setSelectedBone(EditorBone bone) {
        this.selectedBone = bone;
        updateHighlight();
    }

    /**
     * 更新高亮显示
     */
    private void updateHighlight() {
        for (PartListItem item : partItems) {
            if (item.bone == selectedBone) {
                // 选中的项：黄色文本 + 亮色背景
                item.text.setColor(ColorRGBA.Yellow);
                com.jme3.material.Material bgMat = item.background.getMaterial();
                bgMat.setColor("Color", new ColorRGBA(0.4f, 0.4f, 0.1f, 0.8f)); // 黄色高亮背景
            } else {
                // 未选中的项：白色文本 + 暗色背景
                item.text.setColor(ColorRGBA.White);
                com.jme3.material.Material bgMat = item.background.getMaterial();
                bgMat.setColor("Color", new ColorRGBA(0.2f, 0.2f, 0.2f, 0.5f)); // 默认灰色背景
            }
        }
    }

    /**
     * 处理鼠标点击
     */
    public boolean handleMouseClick(int mouseX, int mouseY) {
        // RawInputListener已经提供GUI坐标（Y轴向上，原点在左下角），直接使用
        float mouseGuiX = mouseX;
        float mouseGuiY = mouseY;

        // 检查是否点击了某个部件的背景区域（更大的点击范围）
        for (int i = 0; i < partItems.size(); i++) {
            PartListItem item = partItems.get(i);

            // 获取背景的GUI位置和尺寸
            float bgX = item.background.getLocalTranslation().x;
            float bgY = item.background.getLocalTranslation().y;
            com.jme3.scene.shape.Quad bgQuad = (com.jme3.scene.shape.Quad) item.background.getMesh();
            float bgWidth = bgQuad.getWidth();
            float bgHeight = bgQuad.getHeight();

            // 背景边界框（GUI坐标系）
            float bgLeft = bgX;
            float bgRight = bgX + bgWidth;
            float bgBottom = bgY;
            float bgTop = bgY + bgHeight;

            // 检查鼠标是否在背景边界框内
            if (mouseGuiX >= bgLeft && mouseGuiX <= bgRight &&
                mouseGuiY >= bgBottom && mouseGuiY <= bgTop) {

                // 检测双击
                long currentTime = System.currentTimeMillis();
                if (lastClickedBone == item.bone &&
                    currentTime - lastClickTime < DOUBLE_CLICK_INTERVAL) {
                    // 双击 - 打开重命名对话框
                    openRenameDialog(item.bone);
                    lastClickTime = 0;
                    lastClickedBone = null;
                } else {
                    // 单击 - 选择部件
                    lastClickTime = currentTime;
                    lastClickedBone = item.bone;

                    if (callbacks != null) {
                        // 使用带Shift状态的版本，以支持多选
                        callbacks.onPartSelected(item.bone, shiftPressed);
                    }
                }
                return true;
            }
        }

        return false;
    }

    /**
     * 检查是否点击在标题栏（用于拖动）
     */
    public boolean handleTitleBarClick(int mouseX, int mouseY) {
        // 鼠标坐标是GUI坐标（Y从底部向上），需要转换
        int screenHeight = app.getCamera().getHeight();

        // 检查X范围
        if (mouseX < x || mouseX > x + width) {
            return false;
        }

        // 计算标题栏在GUI坐标系中的Y范围
        // 标题栏顶部（GUI坐标）：screenHeight - y
        // 标题栏底部（GUI坐标）：screenHeight - y - titleBarHeight
        int titleBarTopGuiY = screenHeight - y;
        int titleBarBottomGuiY = screenHeight - y - titleBarHeight;

        // 检查Y范围（标题栏区域）
        if (mouseY >= titleBarBottomGuiY && mouseY <= titleBarTopGuiY) {
            isDragging = true;
            dragStartX = mouseX;
            dragStartY = mouseY;
            panelStartX = x;
            panelStartY = y;
            return true;
        }

        return false;
    }

    /**
     * 处理鼠标拖动
     */
    public void handleMouseDrag(int mouseX, int mouseY) {
        if (isDragging) {
            // 鼠标坐标是GUI坐标（Y从底部向上）
            // 但 y 是从屏幕顶部开始的偏移，需要特殊处理
            int deltaX = mouseX - dragStartX;
            int deltaY = mouseY - dragStartY;

            x = panelStartX + deltaX;
            // deltaY是正值表示向上移动（GUI坐标系），对应y减小（屏幕坐标系）
            y = panelStartY - deltaY;

            // 限制在屏幕范围内
            x = Math.max(0, Math.min(x, app.getCamera().getWidth() - width));
            y = Math.max(0, Math.min(y, app.getCamera().getHeight() - height));

            // 更新所有UI元素的位置
            updatePanelPosition();
        }
    }

    /**
     * 处理鼠标释放
     */
    public void handleMouseRelease() {
        if (isDragging) {
            isDragging = false;
        }
    }

    /**
     * 更新面板位置（用于拖动时）
     */
    private void updatePanelPosition() {
        int screenHeight = app.getCamera().getHeight();

        // 更新背景位置
        if (backgroundGeometry != null) {
            float panelBottomGuiY = Math.max(0, (screenHeight - y) - height);
            backgroundGeometry.setLocalTranslation(x, panelBottomGuiY, -1);
        }

        // 更新标题栏位置
        if (titleBarGeometry != null) {
            float titleBarBottomGuiY = (screenHeight - y) - titleBarHeight;
            titleBarGeometry.setLocalTranslation(x, titleBarBottomGuiY, -0.5f);
        }

        // 更新标题
        int currentY = screenHeight - y - 20;
        titleText.setLocalTranslation(x + 10, currentY, 0);

        // 重新刷新列表（这会更新所有部件的位置）
        refreshPartList();
    }

    /**
     * 清除显示
     */
    public void clear() {
        for (PartListItem item : partItems) {
            item.text.removeFromParent();
            item.background.removeFromParent();
        }
        partItems.clear();
        skeleton = null;
        selectedBone = null;
    }

    /**
     * 获取根节点
     */
    public Node getRootNode() {
        return rootNode;
    }

    /**
     * 设置回调接口
     */
    public void setCallbacks(PartListCallbacks callbacks) {
        this.callbacks = callbacks;
    }

    /**
     * 获取面板X位置
     */
    public int getX() {
        return x;
    }

    /**
     * 获取面板Y位置
     */
    public int getY() {
        return y;
    }

    /**
     * 打开重命名对话框
     */
    private void openRenameDialog(EditorBone bone) {
        // 在独立线程中显示输入对话框
        new Thread(() -> {
            // 创建置顶的父窗口
            JFrame parentFrame = new JFrame();
            parentFrame.setAlwaysOnTop(true);
            parentFrame.setUndecorated(true);
            parentFrame.setSize(0, 0);
            parentFrame.setVisible(false);

            String input = JOptionPane.showInputDialog(
                parentFrame,
                "Rename part:",
                bone.getName()
            );

            parentFrame.dispose();

            if (input != null && !input.trim().isEmpty()) {
                String newName = input.trim();

                // 检查名称是否有效（不为空，不含特殊字符）
                if (newName.matches("^[a-zA-Z0-9_\\-]+$")) {
                    // 检查名称是否已存在
                    if (skeleton.findBone(newName) != null && !newName.equals(bone.getName())) {
                        // 名称已存在，显示错误
                        JFrame errorFrame = new JFrame();
                        errorFrame.setAlwaysOnTop(true);
                        errorFrame.setUndecorated(true);
                        errorFrame.setSize(0, 0);
                        errorFrame.setVisible(false);

                        JOptionPane.showMessageDialog(
                            errorFrame,
                            "Part name '" + newName + "' already exists!",
                            "Error",
                            JOptionPane.ERROR_MESSAGE
                        );

                        errorFrame.dispose();
                    } else {
                        // 在渲染线程中执行重命名
                        app.enqueue(() -> {
                            if (callbacks != null) {
                                callbacks.onPartRenamed(bone, newName);
                            }
                            return null;
                        });
                    }
                } else {
                    // 名称格式无效
                    JFrame errorFrame = new JFrame();
                    errorFrame.setAlwaysOnTop(true);
                    errorFrame.setUndecorated(true);
                    errorFrame.setSize(0, 0);
                    errorFrame.setVisible(false);

                    JOptionPane.showMessageDialog(
                        errorFrame,
                        "Invalid name! Use only letters, numbers, '_' and '-'.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                    );

                    errorFrame.dispose();
                }
            }
        }).start();
    }

    /**
     * 设置镜像管理器引用
     */
    public void setMirrorManager(MirrorManager mirrorManager) {
        this.mirrorManager = mirrorManager;
    }

    /**
     * 设置 Shift 键状态（用于多选）
     */
    public void setShiftPressed(boolean pressed) {
        this.shiftPressed = pressed;
    }

    /**
     * 刷新列表显示（用于更新镜像边框）
     */
    public void refreshDisplay() {
        if (skeleton != null) {
            setSkeleton(skeleton);
        }
    }
}
