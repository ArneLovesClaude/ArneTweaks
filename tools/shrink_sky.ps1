# Verkleinert riesige Himmel-Bilder, damit die Mod-Datei verschickbar bleibt.
# - Himmel-Texturen breiter als MaxWidth werden proportional verkleinert.
# - preview.png (nur fuers Menue) wird auf PreviewWidth verkleinert.
param([string]$StylesRoot, [int]$MaxWidth = 3072, [int]$PreviewWidth = 256)
Add-Type -AssemblyName System.Drawing

function Resize([string]$path, [int]$width) {
    $img = [Drawing.Image]::FromFile($path)
    try {
        if ($img.Width -le $width) { return $false }
        $height = [int][math]::Round($img.Height * $width / $img.Width)
        $bmp = New-Object Drawing.Bitmap $width, $height, ([Drawing.Imaging.PixelFormat]::Format32bppArgb)
        $g = [Drawing.Graphics]::FromImage($bmp)
        $g.InterpolationMode = [Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
        $g.PixelOffsetMode = [Drawing.Drawing2D.PixelOffsetMode]::HighQuality
        $g.CompositingMode = [Drawing.Drawing2D.CompositingMode]::SourceCopy
        $g.DrawImage($img, 0, 0, $width, $height)
        $g.Dispose()
    } finally { $img.Dispose() }
    $bmp.Save($path, [Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
    return $true
}

$changed = 0
foreach ($f in Get-ChildItem "$StylesRoot\sky" -Recurse -File -Filter *.png) {
    $target = if ($f.Name -eq "preview.png" -and $f.Directory.Parent.Name -eq "sky") { $PreviewWidth } else { $MaxWidth }
    if (Resize $f.FullName $target) { $changed++ }
}
"$changed Bilder verkleinert"
