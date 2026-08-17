package com.Hecate.ui;

import com.jme3.app.SimpleApplication;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.math.ColorRGBA;
import com.Hecate.world.TerrainInventory;

/**
 * 地形背包 HUD
 * 在屏幕上显示土方数量
 */
public class TerrainInventoryHUD {

    private final SimpleApplication app;
    private final TerrainInventory inventory;
    private BitmapText inventoryText;

    public TerrainInventoryHUD(SimpleApplication app, TerrainInventory inventory) {
        this.app = app;
        this.inventory = inventory;
        initializeHUD();
    }

    /**
     * 初始化 HUD
     */
    private void initializeHUD() {
        BitmapFont guiFont = app.getAssetManager().loadFont("Interface/Fonts/Default.fnt");

        inventoryText = new BitmapText(guiFont, false);
        inventoryText.setSize(guiFont.getCharSet().getRenderedSize() * 1.5f);
        inventoryText.setColor(ColorRGBA.White);

        // 位置：屏幕右上角
        int screenWidth = app.getContext().getSettings().getWidth();
        inventoryText.setLocalTranslation(screenWidth - 200, app.getContext().getSettings().getHeight() - 20, 0);

        app.getGuiNode().attachChild(inventoryText);

        updateDisplay();
    }

    /**
     * 更新显示
     */
    public void update() {
        updateDisplay();
    }

    /**
     * 更新文本内容
     */
    private void updateDisplay() {
        if (inventoryText != null && inventory != null) {
            String text = String.format("土方: %.1f m³", inventory.getDirtVolume());
            inventoryText.setText(text);
        }
    }

    /**
     * 显示提示信息（例如土方不足）
     */
    public void showMessage(String message, ColorRGBA color) {
        if (inventoryText != null) {
            // 暂时改变颜色显示提示
            ColorRGBA originalColor = inventoryText.getColor().clone();
            inventoryText.setColor(color);

            // 1秒后恢复
            app.enqueue(() -> {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    // ignore
                }
                inventoryText.setColor(originalColor);
                return null;
            });
        }
    }

    /**
     * 清理 HUD
     */
    public void cleanup() {
        if (inventoryText != null) {
            app.getGuiNode().detachChild(inventoryText);
            inventoryText = null;
        }
    }
}
