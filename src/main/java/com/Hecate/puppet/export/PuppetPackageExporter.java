package com.Hecate.puppet.export;

import com.Hecate.puppet.core.Skeleton;
import com.Hecate.puppet.core.PuppetRenderer;
import com.Hecate.puppet.config.PuppetPackageIO;

import java.io.IOException;

/**
 * Puppet打包格式导出器
 * 将木偶配置和所有纹理图片打包成一个.puppet文件
 */
public class PuppetPackageExporter implements IExporter {

    @Override
    public void export(Skeleton skeleton, PuppetRenderer renderer, String filePath) throws IOException {
        PuppetPackageIO.savePackage(skeleton, renderer, filePath);
    }

    @Override
    public ExportFormat getFormat() {
        return ExportFormat.PUPPET_PACKAGE;
    }
}
