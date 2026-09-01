package com.Hecate.ui.inventory;

import com.Hecate.item.ItemDef;
import com.Hecate.item.ItemRegistry;
import com.Hecate.item.ItemStack;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.texture.Texture;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.component.QuadBackgroundComponent;
import com.simsilica.lemur.event.CursorButtonEvent;
import com.simsilica.lemur.event.CursorListener;
import com.simsilica.lemur.event.CursorMotionEvent;
import com.jme3.scene.Spatial;
import com.jme3.asset.AssetManager;

/**
 * 通用格子容器里的单个槛位（Lemur版本）：一个方形按钮，显示物品图标+堆叠数量。
 * <p>格子本身默认透明——网格线/边框由宿主 {@link InventoryGridPanel} 整张背景贴图
 * （如backpack.png）绘制，槛位按钮只负责悬停/拖拽高亮（叠加blockhighlight.png）和图标。
 * <p>拖拽交互统一由 {@link InventoryGridPanel} 处理（它知道"当前拖拽中的是哪个物品、
 * 幽灵图标位置"这些跨槛位状态），本类只负责单个槛位的外观和把鼠标事件转发给宿主容器。
 */
public class InventorySlotPanel {
    private final int slotIndex;
    private final Button button;
    private final ItemRegistry itemRegistry;
    private final AssetManager assetManager;
    private final float slotSize;

    private Texture highlightTexture; // blockhighlight.png，悬停/拖拽预览共用同一张贴图，靠色调区分状态

    private static final ColorRGBA HOVER_TINT = new ColorRGBA(1f, 1f, 1f, 0.6f);
    private static final ColorRGBA DROP_TARGET_TINT = new ColorRGBA(1f, 0.7f, 0.2f, 0.85f);

    public InventorySlotPanel(int slotIndex, float slotSize, ItemRegistry itemRegistry, AssetManager assetManager,
                               SlotInteractionListener listener) {
        this.slotIndex = slotIndex;
        this.slotSize = slotSize;
        this.itemRegistry = itemRegistry;
        this.assetManager = assetManager;
        this.button = new Button("");
        button.setPreferredSize(new Vector3f(slotSize, slotSize, 0));
        button.setColor(ColorRGBA.White);
        button.setBackground(null); // 透明：让宿主容器的整张背包背景贴图透出来

        com.simsilica.lemur.event.CursorEventControl.addListenersToSpatial(button, new CursorListener() {
            @Override
            public void cursorButtonEvent(CursorButtonEvent event, Spatial target, Spatial capture) {
                if (event.isPressed()) {
                    listener.onSlotPressed(slotIndex, event.getX(), event.getY());
                } else {
                    listener.onSlotReleased(slotIndex, event.getX(), event.getY());
                }
                event.setConsumed();
            }

            @Override
            public void cursorEntered(CursorMotionEvent event, Spatial target, Spatial capture) {
                listener.onSlotHovered(slotIndex, event.getX(), event.getY());
            }

            @Override
            public void cursorExited(CursorMotionEvent event, Spatial target, Spatial capture) {
                listener.onSlotUnhovered(slotIndex);
            }

            @Override
            public void cursorMoved(CursorMotionEvent event, Spatial target, Spatial capture) {
                listener.onSlotHovered(slotIndex, event.getX(), event.getY());
            }
        });
    }

    public Button getButton() {
        return button;
    }

    public int getSlotIndex() {
        return slotIndex;
    }

    /**
     * 根据物品堆刷新显示的图标+数量文字。跨格物品（如Gun1横向占2格）的图标尺寸按
     * {@link ItemDef#getCellWidth()}/{@link ItemDef#getCellHeight()} 乘以单格像素尺寸计算，
     * 会视觉上覆盖到相邻槛位——这要求相邻的被占用槛位不渲染自己的图标（由
     * {@link com.Hecate.item.Inventory#getSlot} 对非锚点格返回EMPTY保证），且本槛位在
     * 场景图中的绘制顺序晚于相邻槛位（{@link InventoryGridPanel}按index顺序添加，
     * 后添加的默认覆盖在前面，锚点格总是左上角、index最小，天然满足这个顺序）。
     */
    public void updateDisplay(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            button.setIcon(null);
            button.setText("");
            return;
        }

        ItemDef def = itemRegistry.getItemDef(stack.getItemId());
        if (def != null && def.getIconPath() != null) {
            button.setIcon(ItemIconFactory.createIcon(assetManager, def, slotSize));
        } else {
            button.setIcon(null);
        }

        button.setText(stack.getCount() > 1 ? String.valueOf(stack.getCount()) : "");
    }

    /**
     * 拖拽中隐藏本槛位的图标（物品被"拿在手上"跟随鼠标显示，原槛位应该显示为空），
     * 但不清空底层数据——只是临时视觉隐藏，拖拽结束后 {@link InventoryGridPanel} 会调用
     * {@link #updateDisplay} 用真实数据刷新回来。
     */
    public void hideIconTemporarily() {
        button.setIcon(null);
        button.setText("");
    }

    public void setHighlightPath(String highlightTexturePath) {
        Texture texture = assetManager.loadTexture(highlightTexturePath);
        texture.setMagFilter(Texture.MagFilter.Nearest);
        texture.setMinFilter(Texture.MinFilter.NearestNoMipMaps);
        this.highlightTexture = texture;
    }

    public void setHighlighted(boolean highlighted) {
        applyHighlightState(highlighted, HOVER_TINT);
    }

    /**
     * 拖拽悬停在本槛位上方时的落点预览高亮（与普通hover共用同一张贴图，色调更醒目）。
     */
    public void setDropTarget(boolean active) {
        applyHighlightState(active, DROP_TARGET_TINT);
    }

    private void applyHighlightState(boolean active, ColorRGBA tint) {
        if (!active || highlightTexture == null) {
            button.setBackground(null);
            return;
        }
        QuadBackgroundComponent bg = new QuadBackgroundComponent(highlightTexture);
        bg.setColor(tint);
        button.setBackground(bg);
    }
}
