# Macht bundled-styles klein, damit die Mod-Datei verschickbar bleibt.
# 1. Himmel-Bilder auf MaxWidth verkleinern (shrink_sky.ps1)
# 2. Himmel-Bilder mit pngquant auf 256 Farben reduzieren (mit Dithering, kaum sichtbar)
# 3. Alle PNGs mit oxipng verlustfrei nachpacken
# Werkzeuge: pngquant (pngquant.org) und oxipng (github.com/oxipng/oxipng).
param(
    [string]$StylesRoot,
    [string]$Pngquant,
    [string]$Oxipng,
    [int]$MaxWidth = 1536
)
$before = (Get-ChildItem $StylesRoot -Recurse -File | Measure-Object Length -Sum).Sum

& (Join-Path $PSScriptRoot "shrink_sky.ps1") -StylesRoot $StylesRoot -MaxWidth $MaxWidth

# pngquant in Paketen, damit die Befehlszeile nicht zu lang wird.
# Exit-Code 98/99 = Bild wuerde groesser bzw. zu schlecht -> bleibt unveraendert, das ist in Ordnung.
$skyPngs = @(Get-ChildItem "$StylesRoot\sky" -Recurse -File -Filter *.png | ForEach-Object { $_.FullName })
for ($i = 0; $i -lt $skyPngs.Count; $i += 40) {
    $chunk = $skyPngs[$i..([Math]::Min($i + 39, $skyPngs.Count - 1))]
    & $Pngquant --quality=60-95 --speed 1 --skip-if-larger --force --strip --ext .png -- @chunk 2>$null
}
"pngquant: $($skyPngs.Count) Himmel-Bilder bearbeitet"

& $Oxipng -o 2 --strip safe -r -q $StylesRoot
"oxipng: fertig"

$after = (Get-ChildItem $StylesRoot -Recurse -File | Measure-Object Length -Sum).Sum
"{0:N0} MB -> {1:N0} MB" -f ($before / 1MB), ($after / 1MB)
