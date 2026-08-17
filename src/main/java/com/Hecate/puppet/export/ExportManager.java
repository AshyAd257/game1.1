package com.Hecate.puppet.export;

import com.Hecate.puppet.editor.core.EditorSkeleton;
import com.Hecate.puppet.editor.core.EditorPuppetRenderer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 导出管理器
 * 管理所有导出格式和导出器
 */
public class ExportManager {

    private static final Map<ExportFormat, IExporter> exporters = new HashMap<>();

    static {
        // 注册所有导出器
        registerExporter(new PuppetExporter());
        registerExporter(new PuppetPackageExporter());
        registerExporter(new JsonExporter());
        registerExporter(new DragonBonesExporter());
    }

    /**
     * 注册导出器
     */
    private static void registerExporter(IExporter exporter) {
        exporters.put(exporter.getFormat(), exporter);
    }

    /**
     * 获取导出器
     */
    public static IExporter getExporter(ExportFormat format) {
        return exporters.get(format);
    }

    /**
     * 导出木偶数据
     */
    public static void export(ExportFormat format, com.Hecate.puppet.core.Skeleton skeleton, com.Hecate.puppet.core.PuppetRenderer renderer, String filePath) throws IOException {
        IExporter exporter = getExporter(format);
        if (exporter == null) {
            throw new IllegalArgumentException("未找到导出格式: " + format);
        }
        exporter.export(skeleton, renderer, filePath);
    }

    /**
     * 导出木偶数据（编辑器版本）
     * 将 EditorSkeleton 和 EditorPuppetRenderer 转换为基础类型后导出
     */
    public static void export(ExportFormat format, EditorSkeleton editorSkeleton, EditorPuppetRenderer editorRenderer, String filePath) throws IOException {
        // 从 EditorSkeleton 获取基础 Skeleton
        com.Hecate.puppet.core.Skeleton skeleton = editorSkeleton.getBaseSkeleton();

        // 从 EditorPuppetRenderer 获取基础 PuppetRenderer
        com.Hecate.puppet.core.PuppetRenderer renderer = editorRenderer.getBaseRenderer();

        // 调用原始导出方法
        export(format, skeleton, renderer, filePath);
    }

    /**
     * 获取所有支持的导出格式
     */
    public static ExportFormat[] getSupportedFormats() {
        return ExportFormat.values();
    }
}
