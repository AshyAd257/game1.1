package com.Hecate.ui;

import com.Hecate.event.AmmoChangedEvent;
import com.Hecate.event.EventBus;
import com.Hecate.event.WeaponEquippedEvent;
import com.Hecate.event.WeaponUnequippedEvent;
import com.Hecate.weapon.WeaponKind;
import com.Hecate.ui.common.TTFontLoader;
import com.jme3.app.SimpleApplication;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Texture;

import java.util.HashMap;
import java.util.Map;

/**
 * 面板管理器 - 统一管理表盘类HUD面板的贴图映射与出现时机
 * <p>物品/方块/武器的显示名称在这个项目里统一是直接存字面字符串（见ItemRegistry等注册代码），
 * 不走本地化查询——说明面板显示的就是这些字面字符串本身。
 * <p>显示/隐藏时机由 {@link EventBus} 事件驱动：
 * <ul>
 *   <li>{@link WeaponEquippedEvent} / {@link WeaponUnequippedEvent} - 控制枪械仪表盘（GunPanel）</li>
 *   <li>{@link AmmoChangedEvent} - 更新弹药条</li>
 * </ul>
 * IntroduceWordPanel（说明面板）通过 {@link #showIntroduceWordPanel(String)} /
 * {@link #hideIntroduceWordPanel()} 接口，由背包UI在鼠标悬停物品图标时调用
 * （见InventoryGridPanel/InventorySlotPanel）。
 */
public class PanelManager {

    // ==================== 贴图路径 ====================
    private static final String GUN_PANEL_TOP_TEXTURE = "textures/panel/GunPanelTop.png";
    private static final String GUN_PANEL_BOTTOM_TEXTURE = "textures/panel/GunPanelBottom.png";
    private static final String INTRODUCE_WORD_PANEL_TEXTURE = "textures/panel/IntroduceWordPanel.png";
    private static final String GUN_ICON_TEXTURE_DIR = "textures/panel/icons/";

    // ==================== 枪械仪表盘布局（GUI层，屏幕左下角锚定） ====================
    // 曾短暂改为3D世界空间挂在角色身上跟随移动，但会被地形遮挡，改回屏幕固定HUD。
    // 注意：GUI层缩放是直接的像素倍数，不能套用3D世界坐标（米）的缩放比例——
    // 之前误将3D方案的"放大倍数"按比例套到这里，算出scale=25，导致209x199px的贴图
    // 被放大到5225x4975像素（远超屏幕分辨率），屏幕左下角锚点只能看到画布边缘的
    // 透明留白，表现为"面板不显示"。这里scale=6是像素倍数，直接决定屏幕上的实际大小。
    private static final float GUN_PANEL_SCALE = 0.7f;
    private static final float GUN_PANEL_MARGIN_X = 34f;    // 左边距
    private static final float GUN_PANEL_MARGIN_Y = 24f;    // 下边距

    // GUI层Z深度（crosshair在1000，见PointerSystem）
    private static final float Z_GUN_PANEL_BOTTOM = 1005f;
    private static final float Z_GUN_PANEL_ICON = 1005.5f;
    private static final float Z_GUN_PANEL_TOP = 1006f;

    // ==================== 说明面板布局（GUI层，屏幕锚定） ====================
    // 面板贴图源文件是48x48的正方形；放大到6倍(288x288px)才够装下"蒸汽朋克枪"这种5字
    // 物品名——之前2倍(96x96px)在18px字号下连一行都装不满，绝大部分文字溢出到面板外。
    private static final float INTRODUCE_PANEL_SCALE = 6.0f;
    // 面板紧挨鼠标显示时，与鼠标指针之间留一点间隙，避免面板正好挡住鼠标下方的物品格子
    private static final float INTRODUCE_PANEL_CURSOR_OFFSET_X = 16f;
    private static final float INTRODUCE_PANEL_CURSOR_OFFSET_Y = 16f;
    // 屏幕边缘留白：面板紧挨鼠标显示时，若鼠标靠近屏幕边缘，面板要整体钳制在屏幕内，
    // 不能被裁切到看不全
    private static final float INTRODUCE_PANEL_SCREEN_MARGIN = 8f;
    private static final float Z_INTRODUCE_PANEL = 1007f;
    private static final float Z_INTRODUCE_PANEL_TEXT = 1007.5f;
    private static final String CJK_FONT_PATH = "Interface/Fonts/ZLabsBitmap_12px_CN（简体中文）.ttf";
    private static final float INTRODUCE_PANEL_FONT_SIZE = 18f;
    // 文字在面板内的留白（四周留白，避免文字贴着面板边框——尤其是换行后的多行文本
    // 需要保证最后一行也不会顶到面板底边）
    private static final float INTRODUCE_PANEL_TEXT_PADDING_X = 20f;
    private static final float INTRODUCE_PANEL_TEXT_PADDING_Y = 20f;

    private final SimpleApplication app;
    private final EventBus eventBus;

    private final Map<String, Texture> textureCache = new HashMap<>();

    // 枪械仪表盘（GUI层节点，固定贴在屏幕左下角）
    private final Node gunPanelNode = new Node("GunPanelNode");
    private Geometry gunPanelBottom;
    private Geometry gunPanelTop;
    private Geometry gunPanelIcon;
    private boolean gunPanelVisible = false;
    private float gunPanelBaseWidth;
    private float gunPanelHeight;
    private float ammoPercentage = 1.0f;

    // 说明面板
    private Geometry introduceWordPanel;
    private boolean introducePanelVisible = false;
    private TTFontLoader introducePanelFontLoader;
    private Node introducePanelTextNode; // 当前显示的文字节点，切换文本时先摘除旧的再挂新的
    private float introducePanelWidth;
    private float introducePanelHeight;

    public PanelManager(SimpleApplication app, EventBus eventBus) {
        this.app = app;
        this.eventBus = eventBus;

        createGunPanel();
        createIntroduceWordPanel();
        subscribeEvents();
    }

    private void subscribeEvents() {
        if (eventBus == null) {
            return;
        }
        eventBus.subscribe(WeaponEquippedEvent.class, this::onWeaponEquipped);
        eventBus.subscribe(WeaponUnequippedEvent.class, this::onWeaponUnequipped);
        eventBus.subscribe(AmmoChangedEvent.class, this::onAmmoChanged);
    }

    private void onWeaponEquipped(WeaponEquippedEvent event) {
        showGunPanel(event.getKind());
        updateAmmoBar(event.getAmmoCurrent(), event.getAmmoMax());
    }

    private void onWeaponUnequipped(WeaponUnequippedEvent event) {
        hideGunPanel();
    }

    private void onAmmoChanged(AmmoChangedEvent event) {
        if (gunPanelVisible) {
            updateAmmoBar(event.getAmmoCurrent(), event.getAmmoMax());
        }
    }

    // ==================== 枪械仪表盘 ====================

    private void createGunPanel() {
        Texture bottomTexture = loadTexture(GUN_PANEL_BOTTOM_TEXTURE);
        Texture topTexture = loadTexture(GUN_PANEL_TOP_TEXTURE);

        int texWidth = bottomTexture.getImage().getWidth();
        int texHeight = bottomTexture.getImage().getHeight();
        gunPanelBaseWidth = texWidth * GUN_PANEL_SCALE;
        gunPanelHeight = texHeight * GUN_PANEL_SCALE;

        gunPanelBottom = createQuadGeometry("GunPanelBottom", bottomTexture, gunPanelBaseWidth, gunPanelHeight);
        gunPanelBottom.setLocalTranslation(0, 0, Z_GUN_PANEL_BOTTOM);
        gunPanelNode.attachChild(gunPanelBottom);

        gunPanelTop = createQuadGeometry("GunPanelTop", topTexture, gunPanelBaseWidth, gunPanelHeight);
        gunPanelTop.setLocalTranslation(0, 0, Z_GUN_PANEL_TOP);
        gunPanelNode.attachChild(gunPanelTop);

        // 武器图标位置预留：图标资源尚未绘制，默认不可见，装备武器后尝试按种类加载
        float iconWidth = gunPanelBaseWidth * 0.4f;
        float iconHeight = gunPanelHeight * 0.4f;
        gunPanelIcon = createQuadGeometry("GunPanelIcon", topTexture, iconWidth, iconHeight);
        gunPanelIcon.setLocalTranslation(gunPanelBaseWidth * 0.3f, gunPanelHeight * 0.3f, Z_GUN_PANEL_ICON);
        gunPanelIcon.setCullHint(com.jme3.scene.Spatial.CullHint.Always);
        gunPanelNode.attachChild(gunPanelIcon);

        gunPanelNode.setLocalTranslation(GUN_PANEL_MARGIN_X, GUN_PANEL_MARGIN_Y, 0);
        gunPanelNode.setCullHint(com.jme3.scene.Spatial.CullHint.Always);
        app.getGuiNode().attachChild(gunPanelNode);
    }

    /**
     * 显示枪械仪表盘（装备Gun1/Gun2时调用）
     * @param kind 装备的武器种类，用于尝试加载对应图标；为null或图标资源不存在时图标位保持隐藏
     */
    public void showGunPanel(WeaponKind kind) {
        gunPanelVisible = true;
        gunPanelNode.setCullHint(com.jme3.scene.Spatial.CullHint.Never);
        updateGunIcon(kind);
    }

    /**
     * 隐藏枪械仪表盘（卸下武器时调用）
     */
    public void hideGunPanel() {
        gunPanelVisible = false;
        gunPanelNode.setCullHint(com.jme3.scene.Spatial.CullHint.Always);
    }

    private void updateGunIcon(WeaponKind kind) {
        if (kind == null) {
            gunPanelIcon.setCullHint(com.jme3.scene.Spatial.CullHint.Always);
            return;
        }

        String iconPath = GUN_ICON_TEXTURE_DIR + kind.name().toLowerCase() + ".png";
        Texture iconTexture = tryLoadTexture(iconPath);
        if (iconTexture == null) {
            // 图标资源尚未绘制，保持隐藏
            gunPanelIcon.setCullHint(com.jme3.scene.Spatial.CullHint.Always);
            return;
        }

        iconTexture.setMagFilter(Texture.MagFilter.Nearest);
        iconTexture.setMinFilter(Texture.MinFilter.NearestNoMipMaps);
        gunPanelIcon.getMaterial().setTexture("ColorMap", iconTexture);
        gunPanelIcon.setCullHint(com.jme3.scene.Spatial.CullHint.Never);
    }

    /**
     * 更新弹药条显示。底部面板贴图从右向左收缩来表现弹药消耗：
     * 左边缘固定，右边缘随弹药减少向左收缩；恢复弹药时反向变宽。
     * 贴图本身不做拉伸，只裁切UV与几何宽度，保持像素密度一致。
     * <p>收缩速度做了非线性缓动（见{@link #easeAmmoPercentage}）：弹药充足时收缩更快，
     * 弹药将尽时收缩更慢，但满/空两端严格对齐真实弹药百分比。
     */
    public void updateAmmoBar(float current, float max) {
        float rawPercentage = (max > 0) ? Math.max(0f, Math.min(1f, current / max)) : 0f;
        ammoPercentage = easeAmmoPercentage(rawPercentage);

        float visibleWidth = gunPanelBaseWidth * ammoPercentage;
        if (visibleWidth <= 0.01f) {
            gunPanelBottom.setCullHint(com.jme3.scene.Spatial.CullHint.Always);
            return;
        }
        gunPanelBottom.setCullHint(com.jme3.scene.Spatial.CullHint.Never);

        Quad clippedQuad = new Quad(visibleWidth, gunPanelHeight);
        clippedQuad.setBuffer(VertexBuffer.Type.TexCoord, 2, new float[]{
                0f, 0f,
                ammoPercentage, 0f,
                ammoPercentage, 1f,
                0f, 1f
        });
        gunPanelBottom.setMesh(clippedQuad);
    }

    /**
     * 弹药条收缩缓动曲线：弹药充足时收缩更快（约1.5倍线性速度），
     * 弹药将尽时收缩更慢（约0.5倍线性速度），两端与真实弹药百分比严格对齐（0对0，1对1），
     * 保证墨水耗尽时血条视觉上恰好清空，不会出现"墨水已耗尽但条还没到底"的不一致。
     * <p>曲线：f(p) = 0.5*p^2 + 0.5*p，其导数 f'(p) = p + 0.5，
     * 在p=1（满弹药）处斜率1.5，在p=0（弹药耗尽）处斜率0.5。
     */
    private static float easeAmmoPercentage(float p) {
        return 0.7f * p * p + 0.3f * p;
    }

    // ==================== 说明面板 ====================

    private void createIntroduceWordPanel() {
        Texture texture = loadTexture(INTRODUCE_WORD_PANEL_TEXTURE);
        int texWidth = texture.getImage().getWidth();
        int texHeight = texture.getImage().getHeight();
        introducePanelWidth = texWidth * INTRODUCE_PANEL_SCALE;
        introducePanelHeight = texHeight * INTRODUCE_PANEL_SCALE;

        introduceWordPanel = createQuadGeometry("IntroduceWordPanel", texture, introducePanelWidth, introducePanelHeight);
        // 初始位置无意义（每次showIntroduceWordPanel都会按鼠标位置重新定位），
        // 这里只是给个安全的默认值避免未显示前坐标是(0,0)导致意外闪现在屏幕角落
        introduceWordPanel.setLocalTranslation(0, 0, Z_INTRODUCE_PANEL);
        introduceWordPanel.setCullHint(com.jme3.scene.Spatial.CullHint.Always);

        app.getGuiNode().attachChild(introduceWordPanel);

        // 与BuffSelectUI/InventoryUI一致的中文TTF字体（jME默认BitmapFont不含CJK字形，
        // 直接用jME的BitmapText渲染中文物品名会整行空白，见BuffSelectUI.java同款注释）
        introducePanelFontLoader = TTFontLoader.loadFontFromResource(app.getAssetManager(), CJK_FONT_PATH, INTRODUCE_PANEL_FONT_SIZE);
    }

    /**
     * 显示说明面板并展示指定文本（如物品名称），紧挨鼠标当前位置显示（右下方偏移一点，
     * 避免面板正好挡住鼠标下方的物品格子）。文本是要显示的字面内容，不是本地化key——
     * 物品/方块/武器的显示名称在这个项目里统一是直接存字面字符串（见ItemRegistry/BlockRegistry
     * 的注册代码），不走Localization查询。
     * @param cursorX 鼠标屏幕坐标X（app.getInputManager().getCursorPosition()的坐标系）
     * @param cursorY 鼠标屏幕坐标Y
     */
    public void showIntroduceWordPanel(String text, float cursorX, float cursorY) {
        introducePanelVisible = true;
        introduceWordPanel.setCullHint(com.jme3.scene.Spatial.CullHint.Never);
        positionPanelNearCursor(cursorX, cursorY);
        updateIntroducePanelText(text);
    }

    /**
     * 把面板定位到鼠标右下方，并整体钳制在屏幕范围内（靠近屏幕右/下边缘时改为贴在
     * 鼠标左/上方，而不是被裁切到屏幕外看不全）。
     */
    private void positionPanelNearCursor(float cursorX, float cursorY) {
        int screenWidth = app.getCamera().getWidth();
        int screenHeight = app.getCamera().getHeight();

        float x = cursorX + INTRODUCE_PANEL_CURSOR_OFFSET_X;
        if (x + introducePanelWidth > screenWidth - INTRODUCE_PANEL_SCREEN_MARGIN) {
            x = cursorX - INTRODUCE_PANEL_CURSOR_OFFSET_X - introducePanelWidth; // 改贴到鼠标左侧
        }
        x = Math.max(INTRODUCE_PANEL_SCREEN_MARGIN, x);

        // GUI层Y轴朝上，鼠标下方=Y更小；面板的localTranslation是左下角原点，
        // 所以"面板顶部紧贴鼠标下方"意味着 y + panelHeight = cursorY - offset
        float y = cursorY - INTRODUCE_PANEL_CURSOR_OFFSET_Y - introducePanelHeight;
        if (y < INTRODUCE_PANEL_SCREEN_MARGIN) {
            y = cursorY + INTRODUCE_PANEL_CURSOR_OFFSET_Y; // 改贴到鼠标上方
        }
        y = Math.min(screenHeight - INTRODUCE_PANEL_SCREEN_MARGIN - introducePanelHeight, y);

        introduceWordPanel.setLocalTranslation(x, y, Z_INTRODUCE_PANEL);
    }

    public void hideIntroduceWordPanel() {
        introducePanelVisible = false;
        introduceWordPanel.setCullHint(com.jme3.scene.Spatial.CullHint.Always);
        clearIntroducePanelText();
    }

    private void updateIntroducePanelText(String text) {
        clearIntroducePanelText();
        if (introducePanelFontLoader == null || text == null || text.isEmpty()) {
            return;
        }

        float availableWidth = introducePanelWidth - INTRODUCE_PANEL_TEXT_PADDING_X * 2;
        String wrapped = wrapText(text, availableWidth);

        introducePanelTextNode = introducePanelFontLoader.createText(wrapped, ColorRGBA.White);
        // 面板本身的localTranslation已经是左下角原点；createText()把第一行的字符基线放在
        // 节点局部坐标y=0附近（略微偏上，是字符上方的一点空间），后续行依次向下排布。
        // 所以只需把节点Y对齐到"面板顶部减去顶部留白"，第一行就会紧贴着这条线往下写，
        // 后续行自然向下延伸，不需要额外减去行高。
        float startY = introduceWordPanel.getLocalTranslation().y + introducePanelHeight
                - INTRODUCE_PANEL_TEXT_PADDING_Y;
        introducePanelTextNode.setLocalTranslation(
                introduceWordPanel.getLocalTranslation().x + INTRODUCE_PANEL_TEXT_PADDING_X,
                startY,
                Z_INTRODUCE_PANEL_TEXT);
        app.getGuiNode().attachChild(introducePanelTextNode);
    }

    /**
     * 按可用宽度贪心换行：逐字符累加，一旦当前行加上下一个字符会超出可用宽度就换行。
     * 逐字符而不是按空格分词——中文物品名通常没有空格分隔，按词分行对中文文本没有意义。
     */
    private String wrapText(String text, float availableWidth) {
        if (availableWidth <= 0) {
            return text;
        }
        StringBuilder result = new StringBuilder();
        StringBuilder currentLine = new StringBuilder();

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\n') {
                result.append(currentLine).append('\n');
                currentLine.setLength(0);
                continue;
            }
            String candidate = currentLine.toString() + c;
            if (currentLine.length() > 0 && introducePanelFontLoader.getTextWidth(candidate) > availableWidth) {
                result.append(currentLine).append('\n');
                currentLine.setLength(0);
            }
            currentLine.append(c);
        }
        result.append(currentLine);
        return result.toString();
    }

    private void clearIntroducePanelText() {
        if (introducePanelTextNode != null && introducePanelTextNode.getParent() != null) {
            introducePanelTextNode.removeFromParent();
        }
        introducePanelTextNode = null;
    }

    public boolean isGunPanelVisible() {
        return gunPanelVisible;
    }

    public boolean isIntroducePanelVisible() {
        return introducePanelVisible;
    }

    // ==================== 通用辅助 ====================

    private Geometry createQuadGeometry(String name, Texture texture, float width, float height) {
        Quad quad = new Quad(width, height);
        Geometry geometry = new Geometry(name, quad);

        Material material = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        material.setTexture("ColorMap", texture);
        material.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        material.setTransparent(true);
        material.setColor("Color", ColorRGBA.White);
        geometry.setMaterial(material);

        return geometry;
    }

    private Texture loadTexture(String path) {
        if (textureCache.containsKey(path)) {
            return textureCache.get(path);
        }

        Texture texture = app.getAssetManager().loadTexture(path);
        texture.setMagFilter(Texture.MagFilter.Nearest);
        texture.setMinFilter(Texture.MinFilter.NearestNoMipMaps);
        textureCache.put(path, texture);
        return texture;
    }

    /**
     * 尝试加载贴图，资源不存在时返回null而不抛异常（用于武器图标等尚未绘制的资源）
     */
    private Texture tryLoadTexture(String path) {
        if (textureCache.containsKey(path)) {
            return textureCache.get(path);
        }
        try {
            Texture texture = app.getAssetManager().loadTexture(path);
            textureCache.put(path, texture);
            return texture;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 清理资源
     */
    public void cleanup() {
        if (eventBus != null) {
            eventBus.unsubscribe(WeaponEquippedEvent.class, this::onWeaponEquipped);
            eventBus.unsubscribe(WeaponUnequippedEvent.class, this::onWeaponUnequipped);
            eventBus.unsubscribe(AmmoChangedEvent.class, this::onAmmoChanged);
        }
        if (gunPanelNode.getParent() != null) {
            gunPanelNode.removeFromParent();
        }
        if (introduceWordPanel != null && introduceWordPanel.getParent() != null) {
            introduceWordPanel.removeFromParent();
        }
        textureCache.clear();
    }
}
