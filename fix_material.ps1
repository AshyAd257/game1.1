$file = "src\main\java\com\Hecate\puppet\PuppetPartRenderer.java"
$content = Get-Content $file -Raw -Encoding UTF8

# 替换 createMaterial 方法
$pattern = '(?s)(private void createMaterial\(\) \{)(.*?)(partGeometry\.setMaterial\(partMaterial\);[\r\n\s]+\})'
$replacement = @'
$1
        if (editorMode) {
            // 【编辑器模式】使用Unshaded材质，完全不受光照影响，始终明亮
            partMaterial = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
            partMaterial.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
            partMaterial.setTransparent(true);
            partMaterial.setColor("Color", ColorRGBA.White);
            partMaterial.setFloat("AlphaDiscardThreshold", 0.5f);
        } else {
            // 【游戏模式】使用Lighting材质以支持阴影
            partMaterial = new Material(app.getAssetManager(), "Common/MatDefs/Light/Lighting.j3md");
            partMaterial.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
            partMaterial.setTransparent(true);
            partMaterial.setBoolean("UseMaterialColors", true);
            partMaterial.setColor("Diffuse", ColorRGBA.White);
            partMaterial.setColor("Ambient", ColorRGBA.White);
            partMaterial.setColor("GlowColor", ColorRGBA.White);
            partMaterial.setFloat("Shininess", 0f);
            partMaterial.setFloat("AlphaDiscardThreshold", 0.5f);
        }
        $3
'@

$newContent = $content -replace $pattern, $replacement
Set-Content $file -Value $newContent -Encoding UTF8
Write-Host "createMaterial method updated successfully"
