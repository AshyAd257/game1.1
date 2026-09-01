package com.Hecate.ui.inventory;

import com.Hecate.item.ItemDef;
import com.Hecate.item.Inventory;
import com.Hecate.item.ItemRegistry;
import com.Hecate.item.ItemStack;
import com.jme3.asset.AssetManager;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.texture.Texture;
import com.simsilica.lemur.Axis;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.FillMode;
import com.simsilica.lemur.component.QuadBackgroundComponent;
import com.simsilica.lemur.component.SpringGridLayout;

/**
 * 通用格子容器面板：把一个 {@link Inventory} 渲染成N个格子（{@link InventorySlotPanel}），
 * 支持鼠标按住拖拽移动/交换物品位置。背景使用整张预先画好网格线的贴图（如backpack.png，
 * 4x4格），格子按钮本身透明，叠在这张背景上方对齐每一格。
 * <p>背包、箱子、战利品、镶嵌界面共用这一个类——差异只是构造时传入的 Inventory 实例、
 * columns/slotSize和背景/高亮贴图路径，不为任何具体用途派生子类。
 *
 * <p><b>拖拽实现方式</b>：按住某格(onSlotPressed)记录拖拽起点、隐藏该格图标、创建跟随
 * 鼠标的幽灵图标；每帧(update)把幽灵图标移到当前鼠标位置，并高亮鼠标下方的落点格；
 * 松开(onSlotReleased)时用鼠标当前屏幕坐标手工命中测试出落点格（而不是用回调参数里的
 * slotIndex——Lemur的拾取捕获语义保证release事件总是回调到"按下时的槛位"，不是鼠标当前
 * 悬停的槛位，见 {@link SlotInteractionListener}），调用 {@link Inventory#moveItem} 完成
 * 搬移/交换，成功或失败都清除拖拽状态并用真实数据刷新显示。
 */
public class InventoryGridPanel implements SlotInteractionListener {
    /**
     * 鼠标悬停在物品图标上/移出时的通知（用于外部驱动说明面板等提示UI）。
     * 与拖拽/旋转逻辑无关，本类自己不渲染任何提示文本，只负责告知"现在该显示/隐藏什么"。
     */
    public interface ItemHoverListener {
        void onItemHovered(ItemDef def, float screenX, float screenY);
        void onItemUnhovered();
    }

    private final Inventory inventory;
    private final ItemRegistry itemRegistry;
    private final AssetManager assetManager;
    private final Container rootContainer; // 背景层(整张贴图) + 格子层(透明按钮网格) 的容器
    private final Container gridContainer;
    private final InventorySlotPanel[] slotPanels;
    private final int columns;
    private final int rows;
    private final float slotSize;

    private ItemHoverListener itemHoverListener;

    // 拖拽状态：draggingAnchor<0表示当前未在拖拽
    private int draggingAnchor = -1;
    private final com.simsilica.lemur.Label ghostIcon; // 跟随鼠标的浮动图标（Label自带setIcon，不需要额外包一层容器）
    private int currentDropTarget = -1; // 当前鼠标悬停的落点格，-1表示不在网格范围内

    /**
     * @param guiNode 场景GUI根节点（用于挂载跟随鼠标的幽灵图标，与rootContainer所在的节点一致）
     * @param backgroundTexturePath 整张背包/箱子背景贴图路径（已经画好网格线，如backpack.png），
     *                               传null则不显示背景（格子按钮保持透明，退化为无边框纯交互层）
     * @param highlightTexturePath 单格高亮贴图路径（如blockhighlight.png，尺寸应与单格一致），
     *                              传null则悬停/拖拽落点不显示任何视觉反馈
     */
    public InventoryGridPanel(Inventory inventory, ItemRegistry itemRegistry, AssetManager assetManager,
                               Node guiNode, int columns, float slotSize,
                               String backgroundTexturePath, String highlightTexturePath) {
        this.inventory = inventory;
        this.itemRegistry = itemRegistry;
        this.assetManager = assetManager;
        this.columns = columns;
        this.slotSize = slotSize;
        this.rows = (int) Math.ceil(inventory.getSize() / (double) columns);
        this.slotPanels = new InventorySlotPanel[inventory.getSize()];

        this.rootContainer = new Container();
        this.rootContainer.setPreferredSize(new Vector3f(columns * slotSize, rows * slotSize, 0));

        if (backgroundTexturePath != null) {
            Texture bgTexture = assetManager.loadTexture(backgroundTexturePath);
            bgTexture.setMagFilter(Texture.MagFilter.Nearest);
            bgTexture.setMinFilter(Texture.MinFilter.NearestNoMipMaps);
            rootContainer.setBackground(new QuadBackgroundComponent(bgTexture));
        }

        // 主轴Y=行与行之间纵向堆叠，次轴X=行内部由rowContainer自己横向排列——
        // 若主轴写反(X, Y)，行会横向并排而不是纵向堆叠，整个网格会挤成一条横线
        this.gridContainer = new Container(new SpringGridLayout(Axis.Y, Axis.X, FillMode.None, FillMode.None));
        for (int row = 0; row < rows; row++) {
            Container rowContainer = new Container(new SpringGridLayout(Axis.X, Axis.Y, FillMode.None, FillMode.None));
            for (int col = 0; col < columns; col++) {
                int index = row * columns + col;
                if (index >= inventory.getSize()) {
                    break;
                }
                InventorySlotPanel slotPanel = new InventorySlotPanel(index, slotSize, itemRegistry, assetManager, this);
                if (highlightTexturePath != null) {
                    slotPanel.setHighlightPath(highlightTexturePath);
                }
                slotPanels[index] = slotPanel;
                rowContainer.addChild(slotPanel.getButton());
            }
            gridContainer.addChild(rowContainer);
        }
        // 格子层叠在背景层正上方，两者共享同一个左上角原点（0,0），逐格对齐；
        // 仅在Z轴上抬一点，确保格子按钮渲染在背景贴图前面而不是被背景盖住
        rootContainer.addChild(gridContainer);
        gridContainer.setLocalTranslation(0, 0, 1);

        this.ghostIcon = new com.simsilica.lemur.Label("");
        this.ghostIcon.setBackground(null);
        this.ghostIcon.setLocalTranslation(0, 0, 2000); // 拖拽时应始终盖在所有UI最上层
        guiNode.attachChild(ghostIcon);
        ghostIcon.setCullHint(com.jme3.scene.Spatial.CullHint.Always); // 默认隐藏，拖拽时才显示

        refreshAll();
    }

    /**
     * 根容器（背景+格子叠在一起），挂到GUI节点上时只需要挂这一个。
     */
    public Container getContainer() {
        return rootContainer;
    }

    /**
     * 设置悬停通知监听器（用于外部驱动说明面板等提示UI）。传null取消监听。
     */
    public void setItemHoverListener(ItemHoverListener listener) {
        this.itemHoverListener = listener;
    }

    /**
     * 将容器内容与Inventory的当前状态重新同步（外部通过 {@link #addItem}/命令行修改了
     * inventory后调用，保证显示不落后于数据）。
     */
    public void refreshAll() {
        for (int i = 0; i < slotPanels.length; i++) {
            slotPanels[i].updateDisplay(inventory.getSlot(i));
        }
    }

    /**
     * 每帧调用：拖拽进行中时，把幽灵图标移动到当前鼠标位置，并更新落点格高亮。
     * 非拖拽状态下是no-op。
     * @param cursorScreenPos 当前鼠标屏幕坐标（app.getInputManager().getCursorPosition()）
     */
    public void update(Vector2f cursorScreenPos) {
        if (draggingAnchor < 0) {
            return;
        }

        Vector3f iconSize = ghostIcon.getPreferredSize();
        ghostIcon.setLocalTranslation(
                cursorScreenPos.x - iconSize.x / 2f,
                cursorScreenPos.y + iconSize.y / 2f,
                2000);

        int hitIndex = hitTestSlot(cursorScreenPos);
        if (hitIndex != currentDropTarget) {
            setDropTargetHighlight(currentDropTarget, false);
            currentDropTarget = hitIndex;
            setDropTargetHighlight(currentDropTarget, true);
        }
    }

    /**
     * 高亮/取消高亮anchorIndex处摆放当前拖拽物品时会覆盖的整片格子（不是只高亮anchorIndex
     * 这一格）——预览用的footprint按当前拖拽物品的尺寸计算，不检查目标位置是否被占用
     * （拖拽悬停允许预览到一个稍后可能因空间不够而失败的位置，真正校验在松手时发生）。
     */
    private void setDropTargetHighlight(int anchorIndex, boolean active) {
        if (anchorIndex < 0 || draggingAnchor < 0) {
            return;
        }
        ItemStack draggingStack = inventory.getSlot(draggingAnchor);
        ItemDef draggingDef = itemRegistry.getItemDef(draggingStack.getItemId());
        if (draggingDef == null) {
            return;
        }
        int[] footprint = inventory.previewFootprint(anchorIndex, draggingDef.getCellWidth(), draggingDef.getCellHeight());
        if (footprint == null) {
            return; // 预览位置连边界都放不下（会跨行/超出总格数），没有格子可高亮
        }
        for (int cell : footprint) {
            slotPanels[cell].setDropTarget(active);
        }
    }

    @Override
    public void onSlotPressed(int slotIndex, float screenX, float screenY) {
        if (draggingAnchor >= 0 || inventory.isEmpty(slotIndex)) {
            return;
        }

        int anchor = inventory.getAnchorIndex(slotIndex);
        ItemStack stack = inventory.getSlot(anchor);
        ItemDef def = itemRegistry.getItemDef(stack.getItemId());
        if (def == null) {
            return;
        }

        // 按下前的静态悬停高亮（onSlotHovered点亮的整个footprint）必须先清掉，再切换到
        // 拖拽预览高亮体系——否则从非锚点格（如Gun1的第2格）按下时，静态悬停高亮的区域
        // 是[锚点,锚点+1]，但拖拽预览一开始是以slotIndex本身为锚点去算的（见下方注释），
        // 两者不重合，会导致锚点格的悬停高亮永远没人清理、卡在格子里（此前的bug）。
        for (int cell : inventory.getItemFootprint(slotIndex)) {
            slotPanels[cell].setHighlighted(false);
        }

        draggingAnchor = anchor;
        slotPanels[anchor].hideIconTemporarily();

        ghostIcon.setPreferredSize(new Vector3f(def.getCellWidth() * slotSize, def.getCellHeight() * slotSize, 0));
        ghostIcon.setIcon(ItemIconFactory.createIcon(assetManager, def, slotSize));
        ghostIcon.setCullHint(com.jme3.scene.Spatial.CullHint.Never);
    }

    @Override
    public void onSlotReleased(int slotIndex, float screenX, float screenY) {
        if (draggingAnchor < 0) {
            return;
        }

        int dropIndex = hitTestSlot(new Vector2f(screenX, screenY));
        if (dropIndex >= 0) {
            inventory.moveItem(draggingAnchor, dropIndex);
        }
        // dropIndex<0（拖出网格外）或moveItem返回false（目标放不下）：什么都不做，
        // 下面的refreshAll()会把原本临时隐藏的图标按真实数据显示回原位，等同于弹回。

        endDrag();
    }

    @Override
    public void onSlotHovered(int slotIndex, float screenX, float screenY) {
        if (draggingAnchor < 0) {
            for (int cell : inventory.getItemFootprint(slotIndex)) {
                slotPanels[cell].setHighlighted(true);
            }
            notifyItemHovered(slotIndex, screenX, screenY);
        }
    }

    @Override
    public void onSlotUnhovered(int slotIndex) {
        if (draggingAnchor < 0) {
            for (int cell : inventory.getItemFootprint(slotIndex)) {
                slotPanels[cell].setHighlighted(false);
            }
            if (itemHoverListener != null) {
                itemHoverListener.onItemUnhovered();
            }
        }
    }

    /**
     * 空格子不触发悬停通知——说明面板只在鼠标停在真正的物品图标上时才该出现。
     */
    private void notifyItemHovered(int slotIndex, float screenX, float screenY) {
        if (itemHoverListener == null || inventory.isEmpty(slotIndex)) {
            return;
        }
        int anchor = inventory.getAnchorIndex(slotIndex);
        ItemStack stack = inventory.getSlot(anchor);
        ItemDef def = itemRegistry.getItemDef(stack.getItemId());
        if (def != null) {
            itemHoverListener.onItemHovered(def, screenX, screenY);
        }
    }

    private void endDrag() {
        // 必须在draggingAnchor置为-1之前清除落点高亮：setDropTargetHighlight需要用
        // draggingAnchor查询被拖拽物品的尺寸来算完整footprint，顺序颠倒会导致只清掉
        // currentDropTarget这一格、其余被跨格覆盖的格子高亮残留（此前的bug）。
        setDropTargetHighlight(currentDropTarget, false);
        currentDropTarget = -1;
        draggingAnchor = -1;
        ghostIcon.setCullHint(com.jme3.scene.Spatial.CullHint.Always);
        refreshAll();
    }

    /**
     * 用鼠标屏幕坐标手工命中测试落在哪个格子上，而不是依赖Lemur自己的拾取——
     * 拖拽中幽灵图标会挡在按钮上方，若靠Lemur拾取会拾到幽灵图标本身而不是下方的槛位按钮。
     * <p>Lemur面板以左上角为锚点、Y轴朝上（与 {@code getCursorPosition()} 的屏幕坐标系
     * 一致，行随行号增加向下延伸即世界Y减小）。
     * @return 命中的格子index；未命中任何格子（拖出网格范围外）返回-1
     */
    private int hitTestSlot(Vector2f screenPos) {
        Vector3f topLeft = rootContainer.getWorldTranslation();
        float localX = screenPos.x - topLeft.x;
        float localY = topLeft.y - screenPos.y;

        if (localX < 0 || localY < 0 || localX >= columns * slotSize || localY >= rows * slotSize) {
            return -1;
        }

        int col = (int) (localX / slotSize);
        int row = (int) (localY / slotSize);
        int index = row * columns + col;

        return index < inventory.getSize() ? index : -1;
    }
}
