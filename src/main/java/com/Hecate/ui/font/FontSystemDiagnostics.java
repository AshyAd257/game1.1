package com.Hecate.ui.font;

import com.jme3.asset.AssetManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 字体系统诊断工具
 * <p>检测所有字体渲染库的可用性和状态
 */
public class FontSystemDiagnostics {
    private static final Logger logger = LoggerFactory.getLogger(FontSystemDiagnostics.class);

    public static class DiagnosticResult {
        public String name;
        public boolean available;
        public String version;
        public String status;
        public List<String> issues = new ArrayList<>();

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("\n  [").append(available ? "✓" : "✗").append("] ")
              .append(name);
            if (version != null && !version.isEmpty()) {
                sb.append(" (").append(version).append(")");
            }
            sb.append("\n      Status: ").append(status);
            if (!issues.isEmpty()) {
                sb.append("\n      Issues:");
                for (String issue : issues) {
                    sb.append("\n        - ").append(issue);
                }
            }
            return sb.toString();
        }
    }

    /**
     * 运行完整诊断
     */
    public static List<DiagnosticResult> runDiagnostics(AssetManager assetManager) {
        logger.info("========================================");
        logger.info("  Font System Diagnostics");
        logger.info("========================================");

        List<DiagnosticResult> results = new ArrayList<>();

        results.add(checkJmeBitmapFont(assetManager));
        results.add(checkJmeTtf());
        results.add(checkLwjglFreetype());
        results.add(checkLwjglStb());
        results.add(checkLemurFont());
        results.add(checkFontFiles(assetManager));

        logger.info("\n========== Diagnostic Results ==========");
        for (DiagnosticResult result : results) {
            logger.info(result.toString());
        }
        logger.info("========================================\n");

        return results;
    }

    /**
     * 检查 JME3 默认位图字体
     */
    private static DiagnosticResult checkJmeBitmapFont(AssetManager assetManager) {
        DiagnosticResult result = new DiagnosticResult();
        result.name = "JME3 Bitmap Font";

        try {
            assetManager.loadFont("Interface/Fonts/Default.fnt");
            result.available = true;
            result.status = "Default font loaded successfully";
        } catch (Exception e) {
            result.available = false;
            result.status = "Failed to load default font";
            result.issues.add("Error: " + e.getMessage());
        }

        return result;
    }

    /**
     * 检查 jme-ttf
     */
    private static DiagnosticResult checkJmeTtf() {
        DiagnosticResult result = new DiagnosticResult();
        result.name = "jme-ttf (Stephen Gold)";

        try {
            Class.forName("com.jme3x.jfx.injme.TrueTypeFont");
            result.available = true;
            result.version = "3.0.1+";
            result.status = "Library found but not integrated";
            result.issues.add("Loader implementation incomplete");
        } catch (ClassNotFoundException e) {
            result.available = false;
            result.status = "Library not found in classpath";
            result.issues.add("Add dependency or check Maven installation");
        }

        return result;
    }

    /**
     * 检查 LWJGL FreeType
     */
    private static DiagnosticResult checkLwjglFreetype() {
        DiagnosticResult result = new DiagnosticResult();
        result.name = "LWJGL FreeType";

        try {
            Class.forName("org.lwjgl.util.freetype.FreeType");
            result.available = true;
            result.version = "3.3.1";
            result.status = "Library available, loader partially implemented";

            // 检查原生库
            // TODO: FreetypeFontLoader 尚未实现
            // try {
            //     FreetypeFontLoader.initialize();
            //     result.issues.add("Native library loaded successfully");
            // } catch (Exception e) {
            //     result.issues.add("Native library error: " + e.getMessage());
            // }

        } catch (ClassNotFoundException e) {
            result.available = false;
            result.status = "Library not found";
            result.issues.add("Check pom.xml for lwjgl-freetype dependency");
        }

        return result;
    }

    /**
     * 检查 LWJGL STB TrueType
     */
    private static DiagnosticResult checkLwjglStb() {
        DiagnosticResult result = new DiagnosticResult();
        result.name = "LWJGL STB TrueType";

        try {
            Class.forName("org.lwjgl.stb.STBTruetype");
            result.available = true;
            result.version = "3.3.1";
            result.status = "Library available, loader partially implemented";
        } catch (ClassNotFoundException e) {
            result.available = false;
            result.status = "Library not found";
            result.issues.add("Check pom.xml for lwjgl-stb dependency");
        }

        return result;
    }

    /**
     * 检查 Lemur 字体系统
     */
    private static DiagnosticResult checkLemurFont() {
        DiagnosticResult result = new DiagnosticResult();
        result.name = "Lemur GUI Font System";

        try {
            Class.forName("com.simsilica.lemur.GuiGlobals");
            result.available = true;
            result.version = "1.16.0";
            result.status = "Lemur GUI available";
            result.issues.add("Lemur has its own font rendering system");
        } catch (ClassNotFoundException e) {
            result.available = false;
            result.status = "Lemur not found";
        }

        return result;
    }

    /**
     * 检查字体文件
     */
    private static DiagnosticResult checkFontFiles(AssetManager assetManager) {
        DiagnosticResult result = new DiagnosticResult();
        result.name = "Font Files";

        String[] fontPaths = {
            "Interface/Fonts/ZLabsBitmap_12px_CN（简体中文）.ttf",
            "Interface/Fonts/ZLabsBitmap_12px_HC（香港繁体）.ttf",
            "Interface/Fonts/ZLabsBitmap_12px_JP（日文）.ttf",
            "Interface/Fonts/Default.fnt"
        };

        int foundCount = 0;
        for (String path : fontPaths) {
            try {
                assetManager.locateAsset(new com.jme3.asset.AssetKey<>(path));
                foundCount++;
            } catch (Exception e) {
                result.issues.add("Missing: " + path);
            }
        }

        result.available = foundCount > 0;
        result.status = foundCount + "/" + fontPaths.length + " font files found";

        return result;
    }

    /**
     * 生成诊断报告（用于调试）
     */
    public static String generateReport(List<DiagnosticResult> results) {
        StringBuilder report = new StringBuilder();
        report.append("\n╔════════════════════════════════════════╗\n");
        report.append("║   Font System Diagnostic Report       ║\n");
        report.append("╚════════════════════════════════════════╝\n");

        int available = 0;
        int total = results.size();

        for (DiagnosticResult result : results) {
            if (result.available) available++;
            report.append(result.toString()).append("\n");
        }

        report.append("\n═══════════════════════════════════════\n");
        report.append(String.format("Summary: %d/%d components available\n", available, total));
        report.append("═══════════════════════════════════════\n");

        return report.toString();
    }
}
