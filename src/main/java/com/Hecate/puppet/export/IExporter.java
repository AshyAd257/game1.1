package com.Hecate.puppet.export;

import com.Hecate.puppet.core.Skeleton;
import com.Hecate.puppet.core.PuppetRenderer;

import java.io.IOException;

/**
 * 导出器接口
 * 定义导出木偶数据的标准接口
 */
public interface IExporter {

    /**
     * 导出木偶数据到指定路径
     *
     * @param skeleton 骨架数据
     * @param renderer 渲染器（包含部件信息）
     * @param filePath 目标文件路径
     * @throws IOException 如果导出失败
     */
    void export(Skeleton skeleton, PuppetRenderer renderer, String filePath) throws IOException;

    /**
     * 获取导出格式
     */
    ExportFormat getFormat();
}
