package com.Hecate.ui.inventory;

/**
 * 单个槛位({@link InventorySlotPanel})向宿主容器({@link InventoryGridPanel})转发的鼠标事件。
 * <p>press/release带屏幕坐标（来自CursorButtonEvent.getX()/getY()）：Lemur的拾取捕获语义
 * 保证release事件总是回调到"按下时的那个槛位"，而不是鼠标当前实际悬停的槛位——所以
 * onSlotReleased里的slotIndex是拖拽起点，不是拖拽终点；宿主需要用screenX/screenY自己
 * 命中测试出真正的落点（见InventoryGridPanel.hitTestSlot）。
 */
public interface SlotInteractionListener {
    void onSlotPressed(int slotIndex, float screenX, float screenY);
    void onSlotReleased(int slotIndex, float screenX, float screenY);
    void onSlotHovered(int slotIndex, float screenX, float screenY);
    void onSlotUnhovered(int slotIndex);
}
