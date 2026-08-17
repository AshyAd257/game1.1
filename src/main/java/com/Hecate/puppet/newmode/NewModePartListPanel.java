package com.Hecate.puppet.newmode;

import com.jme3.app.SimpleApplication;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Quad;

import java.util.ArrayList;
import java.util.List;

/**
 * 新模式卡片列表面板 - 显示8张卡片
 *
 * 核心功能：
 * 1. 显示8张卡片的列表（Card 0 - Card 7）
 * 2. 支持点击选择卡片
 * 3. 可拖动面板
 * 4. 半透明背景
 */
public class NewModePartListPanel {

    private final SimpleApplication app;
    private final BitmapFont font;
    private final Node rootNode;

    private BitmapText titleText;
    private List<CardListItem> cardItems;
    private NewModeSkeleton skeleton;
    private NewModeBone selectedBone;
    private int selectedCardIndex = -1;
    private Geometry backgroundGeometry;
    private Geometry titleBarGeometry;

    // 回调接口
    public interface CardListCallbacks {
        void onCardSelected(NewModeBone bone, int cardIndex);
    }
    private CardListCallbacks callbacks;

    private int x, y;  // 可变，支持拖动
    private final int width, height;

    // 拖动相关
    private boolean isDragging = false;
    private int dragStartX = 0;
    private int dragStartY = 0;
    private int panelStartX = 0;
    private int panelStartY = 0;
    private final int titleBarHeight = 50;

    // 卡片列表项
    private static class CardListItem {
        BitmapText text;
        int cardIndex;
        Geometry background;

        CardListItem(BitmapText text, int cardIndex, Geometry background) {
            this.text = text;
            this.cardIndex = cardIndex;
            this.background = background;
        }
    }

    public NewModePartListPanel(SimpleApplication app, BitmapFont font, int x, int y, int width, int height) {
        this.app = app;
        this.font = font;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        this.rootNode = new Node("NewModePartListPanel");
        this.cardItems = new ArrayList<>();
        initializePanel();
    }

    /**
     * 初始化面板 - 所有子元素使用相对坐标
     */
    private void initializePanel() {
        // 创建背景（相对坐标）
        Quad bgQuad = new Quad(width, height);
        backgroundGeometry = new Geometry("PartListBackground", bgQuad);
        Material bgMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        bgMat.setColor("Color", new ColorRGBA(0.15f, 0.15f, 0.15f, 0.9f));
        bgMat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        backgroundGeometry.setMaterial(bgMat);
        backgroundGeometry.setLocalTranslation(0, 0, -1);
        rootNode.attachChild(backgroundGeometry);

        // 创建标题栏背景（相对坐标）
        Quad titleBarQuad = new Quad(width, titleBarHeight);
        titleBarGeometry = new Geometry("PartListTitleBar", titleBarQuad);
        Material titleBarMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        titleBarMat.setColor("Color", new ColorRGBA(0.08f, 0.08f, 0.1f, 0.95f));
        titleBarMat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        titleBarGeometry.setMaterial(titleBarMat);
        titleBarGeometry.setLocalTranslation(0, height - titleBarHeight, -0.5f);
        rootNode.attachChild(titleBarGeometry);

        // 标题（相对坐标）
        titleText = new BitmapText(font);
        titleText.setText("=== 8 Cards ===");
        titleText.setSize(font.getCharSet().getRenderedSize() * 1.5f);
        titleText.setColor(ColorRGBA.Yellow);
        titleText.setLocalTranslation(10, height - 20, 0);
        rootNode.attachChild(titleText);

        // 设置 rootNode 的屏幕绝对位置（唯一的坐标转换点）
        updateRootNodePosition();
    }

    /**
     * 设置骨架并刷新列表
     */
    public void setSkeleton(NewModeSkeleton skeleton) {
        this.skeleton = skeleton;
        refreshCardList();
    }

    /**
     * 刷新卡片列表 - 使用相对坐标
     */
    public void refreshCardList() {
        // 清除旧的列表项
        for (CardListItem item : cardItems) {
            item.text.removeFromParent();
            item.background.removeFromParent();
        }
        cardItems.clear();

        if (skeleton == null || selectedBone == null) {
            return;
        }

        float currentY = height - titleBarHeight - 10;

        // 创建8张卡片的列表项（相对坐标）
        for (int i = 0; i < 8; i++) {
            float itemHeight = 40;
            float itemTopY = currentY;
            float itemBottomY = currentY - itemHeight + 5;

            // 创建背景（相对坐标）
            Geometry itemBackground = new Geometry("CardItem_" + i,
                    new Quad(width - 10, itemHeight));
            Material bgMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
            bgMat.setColor("Color", new ColorRGBA(0.2f, 0.2f, 0.2f, 0.5f));
            bgMat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
            itemBackground.setMaterial(bgMat);
            itemBackground.setLocalTranslation(2, itemBottomY, -0.5f);
            rootNode.attachChild(itemBackground);

            // 创建文本（相对坐标）
            BitmapText text = new BitmapText(font);

            // 检查是否有贴图，显示贴图状态
            String texturePath = selectedBone.getCardTexture(i);
            String displayText = "Card " + i;
            if (texturePath != null && !texturePath.isEmpty()) {
                displayText += " [T]"; // T表示有贴图
            }

            text.setText(displayText);
            text.setSize(font.getCharSet().getRenderedSize() * 1.5f);
            text.setColor(ColorRGBA.White);
            text.setLocalTranslation(10, currentY, 0);
            rootNode.attachChild(text);

            // 创建列表项
            CardListItem item = new CardListItem(text, i, itemBackground);
            cardItems.add(item);

            currentY -= 50;
        }

        updateHighlight();
    }

    /**
     * 设置选中的骨骼
     */
    public void setSelectedBone(NewModeBone bone) {
        this.selectedBone = bone;
        this.selectedCardIndex = 0;  // 默认选择第一张卡片
        refreshCardList();
    }

    /**
     * 设置选中的卡片
     */
    public void setSelectedCard(NewModeBone bone, int cardIndex) {
        this.selectedBone = bone;
        this.selectedCardIndex = cardIndex;
        updateHighlight();
    }

    /**
     * 更新高亮显示
     */
    private void updateHighlight() {
        for (CardListItem item : cardItems) {
            if (item.cardIndex == selectedCardIndex) {
                // 选中的项：黄色文本 + 亮色背景
                item.text.setColor(ColorRGBA.Yellow);
                Material bgMat = item.background.getMaterial();
                bgMat.setColor("Color", new ColorRGBA(0.4f, 0.4f, 0.1f, 0.8f));
            } else {
                // 未选中的项：白色文本 + 暗色背景
                item.text.setColor(ColorRGBA.White);
                Material bgMat = item.background.getMaterial();
                bgMat.setColor("Color", new ColorRGBA(0.2f, 0.2f, 0.2f, 0.5f));
            }
        }
    }

    /**
     * 处理鼠标点击 - 使用绝对坐标判定
     */
    public boolean handleMouseClick(int mouseX, int mouseY) {
        // mouseX, mouseY 是GUI坐标（Y轴向上，原点在左下角）
        float mouseGuiX = mouseX;
        float mouseGuiY = mouseY;

        // 检查是否点击了某个卡片项
        for (CardListItem item : cardItems) {
            // 获取背景的绝对位置（rootNode的translation + 背景的相对位置）
            float rootX = rootNode.getLocalTranslation().x;
            float rootY = rootNode.getLocalTranslation().y;
            float bgRelativeX = item.background.getLocalTranslation().x;
            float bgRelativeY = item.background.getLocalTranslation().y;

            // 背景的绝对GUI坐标
            float bgAbsoluteX = rootX + bgRelativeX;
            float bgAbsoluteY = rootY + bgRelativeY;

            Quad bgQuad = (Quad) item.background.getMesh();
            float bgWidth = bgQuad.getWidth();
            float bgHeight = bgQuad.getHeight();

            // 背景边界框（GUI坐标系）
            float bgLeft = bgAbsoluteX;
            float bgRight = bgAbsoluteX + bgWidth;
            float bgBottom = bgAbsoluteY;
            float bgTop = bgAbsoluteY + bgHeight;

            // 检查鼠标是否在背景边界框内
            if (mouseGuiX >= bgLeft && mouseGuiX <= bgRight &&
                    mouseGuiY >= bgBottom && mouseGuiY <= bgTop) {

                // 点击选择卡片
                if (callbacks != null && selectedBone != null) {
                    callbacks.onCardSelected(selectedBone, item.cardIndex);
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
        int screenHeight = app.getCamera().getHeight();

        if (mouseX < x || mouseX > x + width) {
            return false;
        }

        int titleBarTopGuiY = screenHeight - y;
        int titleBarBottomGuiY = screenHeight - y - titleBarHeight;

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
     * 处理鼠标拖动 - 只更新 x, y 并移动 rootNode
     * @return 如果正在拖动面板返回true，否则返回false
     */
    public boolean handleMouseDrag(int mouseX, int mouseY) {
        if (isDragging) {
            int deltaX = mouseX - dragStartX;
            int deltaY = mouseY - dragStartY;

            x = panelStartX + deltaX;
            y = panelStartY - deltaY;

            // 增大拖动范围15%
            int screenWidth = app.getCamera().getWidth();
            int screenHeight = app.getCamera().getHeight();
            int extraRange = (int)(Math.max(width, height) * 0.15);

            // 限制在屏幕范围内（允许15%超出）
            x = Math.max(-extraRange, Math.min(x, screenWidth - width + extraRange));
            y = Math.max(-extraRange, Math.min(y, screenHeight - height + extraRange));

            updateRootNodePosition();
            return true;
        }
        return false;
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
     * 更新 rootNode 位置 - 唯一的坐标转换点
     */
    private void updateRootNodePosition() {
        int screenHeight = app.getCamera().getHeight();
        float panelBottomGuiY = screenHeight - y - height;
        rootNode.setLocalTranslation(x, panelBottomGuiY, 0);
    }

    /**
     * 清除显示
     */
    public void clear() {
        for (CardListItem item : cardItems) {
            item.text.removeFromParent();
            item.background.removeFromParent();
        }
        cardItems.clear();
        skeleton = null;
        selectedBone = null;
        selectedCardIndex = -1;
    }

    // ========== Getters and Setters ==========

    public Node getRootNode() {
        return rootNode;
    }

    public void setCallbacks(CardListCallbacks callbacks) {
        this.callbacks = callbacks;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }
}
