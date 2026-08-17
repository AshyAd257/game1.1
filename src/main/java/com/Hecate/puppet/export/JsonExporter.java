package com.Hecate.puppet.export;

import com.Hecate.puppet.core.Skeleton;
import com.Hecate.puppet.core.PuppetRenderer;
import com.Hecate.puppet.config.PuppetConfig;
import com.Hecate.puppet.config.PuppetIO;

import java.io.IOException;

/**
 * 纯JSON格式导出器
 * 导出为标准JSON文件（和puppet格式相同，但扩展名为.json）
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
