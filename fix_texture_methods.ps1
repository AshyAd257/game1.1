$file = "src\main\java\com\Hecate\puppet\PuppetPartRenderer.java"
$content = Get-Content $file -Raw -Encoding UTF8

# 修改 loadTexture 方法
$pattern = '(?s)(public void loadTexture\(String texturePath\) \{.*?)(partMaterial\.setTexture\("DiffuseMap", texture\);)(.*?)(catch \(Exception e\))'
$replacement = '$1if (editorMode) {
                partMaterial.setTexture("ColorMap", texture);  // Unshaded.j3md uses ColorMap
            } else {
                partMaterial.setTexture("DiffuseMap", texture);  // Lighting.j3md uses DiffuseMap
            }$3$4'

$newContent = $content -replace $pattern, $replacement

# 修改 setDebugColor 方法
$pattern2 = '(?s)(public void setDebugColor\(ColorRGBA color\) \{.*?)(partMaterial\.clearParam\("DiffuseMap"\);)(.*?\})'
$replacement2 = '$1if (editorMode) {
            partMaterial.clearParam("ColorMap");  // Unshaded.j3md uses ColorMap
            partMaterial.setColor("Color", color);
        } else {
            partMaterial.clearParam("DiffuseMap");  // Lighting.j3md uses DiffuseMap
            partMaterial.setBoolean("UseMaterialColors", true);
            partMaterial.setColor("Diffuse", color);
            partMaterial.setColor("Ambient", color);
        }
    }'

$newContent = $newContent -replace $pattern2, $replacement2
Set-Content $file -Value $newContent -Encoding UTF8
Write-Host "loadTexture and setDebugColor methods updated successfully"
