package com.Hecate.ui.inventory;

import com.Hecate.item.ItemDef;
import com.Hecate.utils.LogUtils;
import com.jme3.asset.AssetManager;
import com.jme3.asset.AssetNotFoundException;
import com.jme3.math.Vector2f;
import com.jme3.texture.Texture;
import com.simsilica.lemur.HAlignment;
import com.simsilica.lemur.VAlignment;
import com.simsilica.lemur.component.IconComponent;

/**
 * 构建物品图标的共享逻辑，供 {@link InventorySlotPanel}（格子内静态显示）和拖拽中的
 * 幽灵图标（跟随鼠标的浮动预览）共用，避免同一套"贴图加载+最近邻过滤+跨格尺寸计算"
 * 逻辑维护两份。
 */
public final class ItemIconFactory {
    private ItemIconFactory() {
    }

    /**
     * 按物品定义创建一个尺寸为 cellWidth*slotSize x cellHeight*slotSize 的图标组件。
     * @param assetManager 资源管理器（用于加载贴图）
     * @param def 物品定义（提供图标路径与跨格尺寸）
     * @param slotSize 单格像素尺寸
     * @return 图标组件；若贴图文件缺失（美术资源尚未补齐）返回null，调用方按"空图标"处理，
     *         不会因为缺一张png就让整个游戏初始化失败崩溃。
     */
    public static IconComponent createIcon(AssetManager assetManager, ItemDef def, float slotSize) {
        Texture iconTexture;
        try {
            iconTexture = assetManager.loadTexture(def.getIconPath());
        } catch (AssetNotFoundException e) {
            LogUtils.warning(ItemIconFactory.class, "Icon texture not found for item '" + def.getId()
                    + "': " + def.getIconPath());
            return null;
        }
        iconTexture.setMagFilter(Texture.MagFilter.Nearest);
        iconTexture.setMinFilter(Texture.MinFilter.NearestNoMipMaps);

        // 注意：IconComponent(Texture, Vector2f, ...)构造函数里的Vector2f参数是
        // iconScale（相对原图的缩放倍数），不是目标像素尺寸！缩放倍数固定传(1,1)
        // （不额外缩放原图），目标像素尺寸用setIconSize()单独设置——这才是
        // "绝对尺寸"的正确写法（历史教训：曾经把目标像素尺寸误当缩放倍数传入，
        // 导致图标被放大了几十倍）。
        IconComponent icon = new IconComponent(iconTexture, new Vector2f(1f, 1f), 0f, 0f, 0f, false);
        icon.setIconSize(new Vector2f(def.getCellWidth() * slotSize, def.getCellHeight() * slotSize));
        icon.setHAlignment(HAlignment.Left);
        icon.setVAlignment(VAlignment.Top);
        return icon;
    }
}
