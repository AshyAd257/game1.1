$file = "src\main\java\com\Hecate\puppet\PuppetPartRenderer.java"
$content = Get-Content $file -Raw
$utf8NoBom = New-Object System.Text.UTF8Encoding $false
[System.IO.File]::WriteAllText((Resolve-Path $file).Path, $content, $utf8NoBom)
Write-Host "BOM removed successfully"
