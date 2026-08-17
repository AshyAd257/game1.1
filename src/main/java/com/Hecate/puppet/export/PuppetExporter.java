package com.Hecate.puppet.export;

import com.Hecate.puppet.core.Skeleton;
import com.Hecate.puppet.core.PuppetRenderer;
import com.Hecate.puppet.config.PuppetConfig;
import com.Hecate.puppet.config.PuppetIO;

import java.io.IOException;

/**
 * Puppet格式导出器
 * 使用现有的PuppetIO保存为.puppet文件
 */
public class PuppetExporter implements IExporter {

    @Override
    public void export(Skeleton skeleton, PuppetRenderer renderer, String filePath) throws IOException {
        PuppetConfig config = PuppetIO.createConfig(skeleton, renderer);
        PuppetIO.saveToFile(config, filePath);
    }

    @Override
    public ExportFormat getFormat() {
        return ExportFormat.PUPPET;
    }
}
