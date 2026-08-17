$file = "src\main\java\com\Hecate\puppet\PuppetPartRenderer.java"
$content = Get-Content $file -Raw -Encoding UTF8

# 修复 loadTexture 方法中的颜色设置
$pattern = '(?s)(if \(editorMode\) \{[\r\n\s]+partMaterial\.setTexture\("ColorMap", texture\);.*?\} else \{[\r\n\s]+partMaterial\.setTexture\("DiffuseMap", texture\);.*?\})[\r\n\s]+.*?// Lighting\.j3md 使用 DiffuseMap[\r\n\s]+partMaterial\.setColor\("Diffuse", ColorRGBA\.White\);[\r\n\s]+partMaterial\.setColor\("Ambient", ColorRGBA\.White\);'

$replacement = 'if (editorMode) {
                partMaterial.setTexture("ColorMap", texture);  // Unshaded.j3md uses ColorMap
            } else {
                partMaterial.setTexture("DiffuseMap", texture);  // Lighting.j3md uses DiffuseMap
                partMaterial.setColor("Diffuse", ColorRGBA.White);
                partMaterial.setColor("Ambient", ColorRGBA.White);
            }'

$newContent = $content -replace $pattern, $replacement
Set-Content $file -Value $newContent -Encoding UTF8
Write-Host "loadTexture color settings fixed successfully"
