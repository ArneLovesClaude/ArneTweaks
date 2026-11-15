# Rechnet altes Himmel-Format (sun.png, moon_phases.png) auf 1.21.11 um (environment/celestial/...).
param([string]$StylesRoot)
Add-Type -AssemblyName System.Drawing
$names = "full_moon","waning_gibbous","third_quarter","waning_crescent","new_moon","waxing_crescent","first_quarter","waxing_gibbous"
foreach ($env in Get-ChildItem "$StylesRoot\sky\*\assets\minecraft\textures\environment" -Directory -ErrorAction SilentlyContinue) {
    $d = $env.FullName; $c = "$d\celestial"
    if (Test-Path "$d\sun.png") { New-Item -ItemType Directory -Force $c | Out-Null; Move-Item "$d\sun.png" "$c\sun.png" -Force; "sun: $d" }
    if (Test-Path "$d\moon_phases.png") {
        New-Item -ItemType Directory -Force "$c\moon" | Out-Null
        $m = [Drawing.Bitmap]::FromFile("$d\moon_phases.png"); $w = $m.Width / 4; $h = $m.Height / 2
        for ($i = 0; $i -lt 8; $i++) {
            $r = New-Object Drawing.Rectangle (($i % 4) * $w), ([math]::Floor($i / 4) * $h), $w, $h
            $part = $m.Clone($r, $m.PixelFormat); $part.Save("$c\moon\$($names[$i]).png", [Drawing.Imaging.ImageFormat]::Png); $part.Dispose()
        }
        $m.Dispose(); Remove-Item "$d\moon_phases.png"; "moon: $d"
    }
}
