package com.Hecate.ui;

import com.Hecate.event.AmmoChangedEvent;
import com.Hecate.event.EventBus;
import com.Hecate.event.WeaponEquippedEvent;
import com.Hecate.event.WeaponUnequippedEvent;
import com.Hecate.localization.Localization;
import com.Hecate.weapon.WeaponKind;
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
 * <p>文字统一走 {@link Localization}，不额外维护文本表。
 * <p>显示/隐藏时机由 {@link EventBus} 事件驱动：
 * <ul>
 *   <li>{@link WeaponEquippedEvent} / {@link WeaponUnequippedEvent} - 控制枪械仪表盘（GunPanel）</li>
 *   <li>{@link AmmoChangedEvent} - 更新弹药条</li>
 * </ul>
 * IntroduceWordPanel（说明面板）目前只提供 {@link #showIntroduceWordPanel(String)} /
 * {@link #hideIntroduceWordPanel()} 接口，尚未接入"鼠标悬停物品"的自动触发（背包物品系统未实现）。
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
    private static final float INTRODUCE_PANEL_SCALE = 2.0f;
    private static final float INTRODUCE_PANEL_MARGIN_Y = 24f;
    private static final float Z_INTRODUCE_PANEL = 1007f;

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
        float width = texWidth * INTRODUCE_PANEL_SCALE;
        float height = texHeight * INTRODUCE_PANEL_SCALE;

        introduceWordPanel = createQuadGeometry("IntroduceWordPanel", texture, width, height);

        int screenWidth = app.getCamera().getWidth();
        float x = (screenWidth - width) / 2f;
        introduceWordPanel.setLocalTranslation(x, INTRODUCE_PANEL_MARGIN_Y, Z_INTRODUCE_PANEL);
        introduceWordPanel.setCullHint(com.jme3.scene.Spatial.CullHint.Always);

        app.getGuiNode().attachChild(introduceWordPanel);
    }

    /**
     * 显示说明面板并展示对应文本
     * @param localizationKey 文本键名，走 {@link Localization#get(String)} 查询
     */
    public void showIntroduceWordPanel(String localizationKey) {
        introducePanelVisible = true;
        introduceWordPanel.setCullHint(com.jme3.scene.Spatial.CullHint.Never);
        // 文本内容当前只用于占位查询，尚未挂接文字渲染节点（背包/物品系统未实现）
        Localization.get(localizationKey);
    }

    public void hideIntroduceWordPanel() {
        introducePanelVisible = false;
        introduceWordPanel.setCullHint(com.jme3.scene.Spatial.CullHint.Always);
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
