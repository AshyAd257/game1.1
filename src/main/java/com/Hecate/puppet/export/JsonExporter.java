package com.Hecate.puppet.export;

import com.Hecate.puppet.core.Skeleton;
import com.Hecate.puppet.core.PuppetRenderer;
import com.Hecate.puppet.config.PuppetConfig;
import com.Hecate.puppet.config.PuppetIO;

import java.io.IOException;

/**
 * 纯JSON格式导出器
 * 仅导出配置与资源路径，不嵌入图片
 */
public class JsonExporter implements IExporter {

    @Override
    public void export(Skeleton skeleton, PuppetRenderer renderer, String filePath) throws IOException {
        PuppetConfig config = PuppetIO.createConfig(skeleton, renderer);
        PuppetIO.saveToFile(config, filePath);
    }

    @Override
    public ExportFormat getFormat() {
        return ExportFormat.JSON;
    }
}
