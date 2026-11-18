package com.Hecate.puppet.export;

import com.Hecate.puppet.core.Skeleton;
import com.Hecate.puppet.core.PuppetRenderer;
import com.Hecate.puppet.config.PuppetConfig;
import com.Hecate.puppet.config.PuppetIO;

import java.io.IOException;

/**
 * Puppet格式导出器
 * 导出包含配置与贴图的完整 .puppet 资源包
 */
public class PuppetExporter implements IExporter {

    @Override
    public void export(Skeleton skeleton, PuppetRenderer renderer, String filePath) throws IOException {
        com.Hecate.puppet.config.PuppetPackageIO.savePackage(skeleton,renderer,filePath);
    }

    @Override
    public ExportFormat getFormat() {
        return ExportFormat.PUPPET;
    }
}
