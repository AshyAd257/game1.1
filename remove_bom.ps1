$filePath = "C:\Users\29232\OneDrive\Desktop\game1(1)\src\main\java\com\Hecate\puppet\PuppetPartRenderer.java"
$content = Get-Content $filePath -Raw
$utf8NoBom = New-Object System.Text.UTF8Encoding $false
[System.IO.File]::WriteAllText($filePath, $content, $utf8NoBom)
Write-Host "BOM removed successfully"
