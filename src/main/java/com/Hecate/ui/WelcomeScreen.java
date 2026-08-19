package com.Hecate.ui;

import com.jme3.app.SimpleApplication;
import com.jme3.math.ColorRGBA;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 欢迎屏幕 - 显示大大的 TTF 字体 "欢迎！"
 */
public class WelcomeScreen {
    private static final Logger logger = LoggerFactory.getLogger(WelcomeScreen.class);

    private final SimpleApplication app;
    private TrueTypeTextUI textUI;

    public WelcomeScreen(SimpleApplication app) {
        this.app = app;
    }

    /**
     * 显示欢迎文本
     */
    public void show() {
        try {
            textUI = new TrueTypeTextUI(app);

            // 加载中文像素字体
            textUI.loadFont("Interface/Fonts/ZLabsBitmap_12px_CN（简体中文）.ttf", 48);

            // 屏幕居中显示
            int screenWidth = app.getCamera().getWidth();
            int screenHeight = app.getCamera().getHeight();

            float x = screenWidth / 2f - 100f;
            float y = screenHeight / 2f + 50f;

            textUI.showText("欢迎！", ColorRGBA.White, x, y);
            logger.info("Welcome screen displayed with TTF font!");

        } catch (Exception e) {
            logger.error("Error showing welcome screen", e);
        }
    }

    /**
     * 隐藏欢迎文本
     */
    public void hide() {
        if (textUI != null) {
            textUI.hideText();
            logger.info("Welcome screen hidden");
        }
    }
}
