# Add debug logging to PuppetPartRenderer.java
$file = "src\main\java\com\Hecate\puppet\PuppetPartRenderer.java"
$content = Get-Content $file -Raw -Encoding UTF8

# Add debug logging to createMaterial method
$content = $content -replace '(private void createMaterial\(\) \{)', '$1
        System.out.println("[DEBUG] PuppetPartRenderer.createMaterial() - Bone: " + bone.getName() + ", editorMode=" + editorMode);'

$content = $content -replace '(if \(editorMode\) \{[\r\n\s]+// 【编辑器模式】使用Unshaded材质)', 'if (editorMode) {
            System.out.println("[DEBUG] " + bone.getName() + " - 使用Unshaded材质（编辑器模式）");
            // 【编辑器模式】使用Unshaded材质'

$content = $content -replace '(} else \{[\r\n\s]+// 【游戏模式】使用Lighting材质)', '} else {
            System.out.println("[DEBUG] " + bone.getName() + " - 使用Lighting材质（游戏模式）");
            // 【游戏模式】使用Lighting材质'

# Add debug logging to initialize method
$content = $content -replace '(public void initialize\(\) \{[\r\n\s]+if \(initialized\) \{)', 'public void initialize() {
        System.out.println("[DEBUG] PuppetPartRenderer.initialize() - Bone: " + bone.getName() + ", editorMode=" + editorMode);
        if (initialized) {'

# Add debug logging to loadTexture method
$content = $content -replace '(public void loadTexture\(String texturePath\) \{[\r\n\s]+try \{)', 'public void loadTexture(String texturePath) {
        System.out.println("[DEBUG] PuppetPartRenderer.loadTexture() - Bone: " + bone.getName() + ", Path: " + texturePath + ", editorMode=" + editorMode);
        try {'

# Add debug logging after texture is set
$content = $content -replace '(if \(editorMode\) \{[\r\n\s]+partMaterial\.setTexture\("ColorMap", texture\);)', 'if (editorMode) {
                partMaterial.setTexture("ColorMap", texture);
                System.out.println("[DEBUG] " + bone.getName() + " - 设置ColorMap纹理（编辑器模式）");'

$content = $content -replace '(} else \{[\r\n\s]+partMaterial\.setTexture\("DiffuseMap", texture\);)', '} else {
                partMaterial.setTexture("DiffuseMap", texture);
                System.out.println("[DEBUG] " + bone.getName() + " - 设置DiffuseMap纹理（游戏模式）");'

Set-Content $file -Value $content -Encoding UTF8 -NoNewline
Write-Host "Debug logging added successfully to PuppetPartRenderer.java"
