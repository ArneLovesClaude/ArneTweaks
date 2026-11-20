# ArneTweaks

Clientseitige Fabric-Mod für Minecraft 1.21.11: einzelne Texturen aus verschiedenen Texture-Packs als „Styles“ auswählen, mit Live-Vorschau im Menü (Taste K).

- Anleitung zum Einfügen eigener Texturen: [ANLEITUNG.md](ANLEITUNG.md)
- Bauen: `.\gradlew.bat build` → `build\libs\arne-tweaks-<version>.jar`
- Testen: `.\gradlew.bat runClient`
- Selbsttest mit Screenshots (nur Entwicklung): Umgebungsvariable `ARNESTYLES_SELFTEST=1` setzen, dann `runClient`. Die Bilder landen in `run\screenshots\`.

## Packs einbauen (bundled-styles)

Die Texturen fremder Packs liegen in `bundled-styles/` (nicht im Git) und werden beim Bauen in die Mod-Datei gepackt.
Reihenfolge zum Neu-Erzeugen:

1. `python tools/sort_packs.py bundled-styles <entpacktes-pack> <id> "<Name>" ...`
   (mit `ARNE_VANILLA_ITEMS` = Ordner `items/` und `ARNE_VANILLA_MODELS` = Ordner `models/` der Vanilla-Assets;
   gemeinsame Grund-Modelle wie `item/generated` bekommt jeder Style als eigene Kopie unter `models/arnestyles/`)
2. `tools/convert_sky.ps1 -StylesRoot bundled-styles` (Sonne/Mond auf 1.21.11-Format)
3. `java tools/RemoveVanillaCopies.java bundled-styles <vanilla assets/minecraft>` (Dateien entfernen, die genau wie in Minecraft sind)
4. `tools/compress_styles.ps1 -StylesRoot bundled-styles -Pngquant <pngquant.exe> -Oxipng <oxipng.exe>`
   (Himmel auf 2048 px, Himmel auf 256 Farben, alles verlustfrei nachpacken; Werkzeuge: pngquant.org, github.com/oxipng/oxipng)
5. `python tools/dedupe_styles.py bundled-styles` (gleiche Styles aus mehreren Packs zusammenlegen)

6. `python tools/pack_cloud.py bundled-styles build/cloud-styles` (ZIPs pro Kategorie + `index.json`)
7. Alle Dateien aus `build/cloud-styles` als neues Release in `ArneLovesClaude/arnetweaks-styles` hochladen:
   `gh release create styles-vN build/cloud-styles/* --repo ArneLovesClaude/arnetweaks-styles`

Seit 3.0.0 sind die Packs **nicht** mehr in der Mod-Datei (sie ist dadurch unter 1 MB). Die Mod lädt
`index.json` aus dem neuesten Release und holt eine Kategorie erst, wenn sie gebraucht wird
(Cache: `config/arnestyles/cloud/`). Zum Testen mit einem lokalen Server: Umgebungsvariable
`ARNESTYLES_CLOUD_URL=http://127.0.0.1:8765/` setzen und `python -m http.server 8765 --directory build/cloud-styles` starten.
Wichtig: Das Repo muss öffentlich sein, sonst können Spieler nichts herunterladen.

## Lizenz / License

**Alle Rechte vorbehalten.** ArneTweaks darfst du kostenlos herunterladen und spielen, aber nicht verändern,
weiterverbreiten, neu hochladen oder in eigenen Projekten benutzen. Details: [LICENSE](LICENSE).

**All rights reserved.** You may download and play ArneTweaks for free, but you may not modify, redistribute,
re-upload or reuse it in your own projects. Details: [LICENSE](LICENSE).

Versionen bis 3.0.0 wurden unter CC0 veröffentlicht und bleiben für genau diese Versionen CC0.
