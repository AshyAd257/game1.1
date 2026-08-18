package com.Hecate.ui.test;

import com.Hecate.ui.FontManager;
import com.Hecate.ui.font.FontSystemDiagnostics;
import com.jme3.app.SimpleApplication;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.math.ColorRGBA;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 字体系统测试应用
 * <p>用于测试和验证所有字体渲染库
 *
 * <p>运行方式：
 * <pre>
 * mvn exec:java -Dexec.mainClass="com.Hecate.ui.test.FontSystemTest"
 * </pre>
 */
public class FontSystemTest extends SimpleApplication {
    private static final Logger logger = LoggerFactory.getLogger(FontSystemTest.class);

    private FontManager fontManager;

    public static void main(String[] args) {
        FontSystemTest app = new FontSystemTest();
        app.start();
    }

    @Override
    public void simpleInitApp() {
        logger.info("\n\n");
        logger.info("╔══════════════════════════════════════════╗");
        logger.info("║   Font System Integration Test          ║");
        logger.info("╚══════════════════════════════════════════╝");
        logger.info("\n");

        // 初始化字体管理器
        fontManager = new FontManager(assetManager);

        // 运行诊断
        logger.info("\n▶ Running diagnostics...\n");
        var results = FontSystemDiagnostics.runDiagnostics(assetManager);
        String report = FontSystemDiagnostics.generateReport(results);
        System.out.println(report);

        // 测试各种字体加载方式
        testDefaultFont();
        testTtfFonts();
        testBackendSwitching();

        // 显示测试结果
        displayTestResults();

        logger.info("\n");
        logger.info("╔══════════════════════════════════════════╗");
        logger.info("║   Test Complete - Check Visual Output   ║");
        logger.info("╚══════════════════════════════════════════╝");
        logger.info("\n");
    }

    /**
     * 测试默认字体
     */
    private void testDefaultFont() {
        logger.info("\n▶ Testing default JME3 bitmap font...");
        try {
            BitmapFont defaultFont = fontManager.getDefaultFont();
            if (defaultFont != null) {
                logger.info("  ✓ Default font loaded successfully");

                // 显示文本
                BitmapText text = new BitmapText(defaultFont);
                text.setText("Default JME3 Font - OK");
                text.setSize(20);
                text.setColor(ColorRGBA.Green);
                text.setLocalTranslation(10, settings.getHeight() - 10, 0);
                guiNode.attachChild(text);
            } else {
                logger.error("  ✗ Failed to load default font");
            }
        } catch (Exception e) {
            logger.error("  ✗ Error loading default font: " + e.getMessage());
        }
    }

    /**
     * 测试 TTF 字体加载
     */
    private void testTtfFonts() {
        logger.info("\n▶ Testing TTF font loading...");

        String[] ttfFonts = {
            "Interface/Fonts/ZLabsBitmap_12px_CN（简体中文）.ttf",
            "Interface/Fonts/ZLabsBitmap_12px_HC（香港繁体）.ttf",
            "Interface/Fonts/ZLabsBitmap_12px_JP（日文）.ttf"
        };

        int yPos = settings.getHeight() - 50;
        for (String fontPath : ttfFonts) {
            try {
                logger.info("  Testing: {}", fontPath);
                BitmapFont font = fontManager.loadFont(fontPath, 16);

                if (font != null) {
                    logger.info("    ✓ Loaded (fallback to default for now)");

                    BitmapText text = new BitmapText(font);
                    text.setText("TTF: " + fontPath.substring(fontPath.lastIndexOf('/') + 1));
                    text.setSize(14);
                    text.setColor(ColorRGBA.Yellow);
                    text.setLocalTranslation(10, yPos, 0);
                    guiNode.attachChild(text);
                    yPos -= 25;
                } else {
                    logger.warn("    ✗ Failed to load");
                }
            } catch (Exception e) {
                logger.error("    ✗ Error: " + e.getMessage());
            }
        }
    }

    /**
     * 测试后端切换
     */
    private void testBackendSwitching() {
        logger.info("\n▶ Testing backend switching...");

        FontManager.FontBackend[] backends = FontManager.FontBackend.values();

        for (FontManager.FontBackend backend : backends) {
            try {
                logger.info("  Switching to: {}", backend);
                fontManager.setBackend(backend);
                logger.info("    ✓ Backend set successfully");
            } catch (Exception e) {
                logger.error("    ✗ Error: " + e.getMessage());
            }
        }

        // 切回默认
        fontManager.setBackend(FontManager.FontBackend.JME_BITMAP);
        logger.info("  Restored to: JME_BITMAP");
    }

    /**
     * 显示测试结果摘要
     */
    private void displayTestResults() {
        BitmapFont defaultFont = fontManager.getDefaultFont();

        // 标题
        BitmapText title = new BitmapText(defaultFont);
        title.setText("Font System Integration Test");
        title.setSize(24);
        title.setColor(ColorRGBA.Cyan);
        title.setLocalTranslation(
            settings.getWidth() / 2 - 150,
            settings.getHeight() - 150,
            0
        );
        guiNode.attachChild(title);

        // 说明
        BitmapText info = new BitmapText(defaultFont);
        info.setText("Check console for detailed diagnostic report");
        info.setSize(14);
        info.setColor(ColorRGBA.White);
        info.setLocalTranslation(
            settings.getWidth() / 2 - 150,
            settings.getHeight() - 180,
            0
        );
        guiNode.attachChild(info);

        // 状态指示
        String[] statusLines = {
            "JME3 Bitmap: WORKING",
            "jme-ttf: PARTIAL (needs implementation)",
            "LWJGL FreeType: PARTIAL (needs atlas generation)",
            "LWJGL STB: PARTIAL (needs atlas generation)",
            "",
            "Press ESC to exit"
        };

        int yPos = settings.getHeight() - 220;
        for (String line : statusLines) {
            BitmapText text = new BitmapText(defaultFont);
            text.setText(line);
            text.setSize(12);
            text.setColor(line.contains("WORKING") ? ColorRGBA.Green :
                         line.contains("PARTIAL") ? ColorRGBA.Yellow :
                         ColorRGBA.White);
            text.setLocalTranslation(
                settings.getWidth() / 2 - 150,
                yPos,
                0
            );
            guiNode.attachChild(text);
            yPos -= 20;
        }
    }

    @Override
    public void simpleUpdate(float tpf) {
        // 空实现
    }
}
