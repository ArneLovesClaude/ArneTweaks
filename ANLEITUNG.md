# ArneTweaks – Texturen aus Texture-Packs einfügen

Mit ArneTweaks kannst du dir aus verschiedenen Texture-Packs einzelne Teile aussuchen, zum Beispiel den Kristall aus Pack A und das Feuer aus Pack B. Im Spiel siehst du für jeden Style eine Vorschau und schaltest ihn mit einem Klick an.

---

## 1. Mod ins echte Minecraft einbauen

Du brauchst **Fabric Loader** für Minecraft **1.21.11** und **Fabric API**. Wenn du schon andere Fabric-Mods nutzt, hast du beides wahrscheinlich.

1. Im Projektordner ein Terminal öffnen und die Mod bauen:
   ```
   .\gradlew.bat build
   ```
2. Die fertige Datei liegt danach in `build\libs\arne-tweaks-<version>.jar`.
   (Die Datei mit `-sources` im Namen brauchst du nicht.)
3. Diese Datei nach `%appdata%\.minecraft\mods\` kopieren.
4. Minecraft mit dem Fabric-Profil starten.

Beim ersten Start legt die Mod diesen Ordner an:

```
%appdata%\.minecraft\config\arnestyles\styles\
```

Darin gibt es für jede Kategorie einen Unterordner: `crystal`, `obsidian`, `glowstone`, `anchor`, `fire`, `stone`, `deepslate`, `grass`, `snow`, `inventory`, `hud`, `crosshair`, `items`, `totems`, `gaps`, `xp_bottles`, `shield`, `tools`, `armor`, `particles`, `sky`, `sounds`, `essentials`.

> Tipp: Im Spiel kommst du direkt dorthin. Menü mit **K** öffnen, Kategorie wählen, oben rechts auf **„Ordner öffnen“** klicken.

---

## 2. Texture-Pack auspacken

Texture-Packs sind ZIP-Dateien und liegen meistens in `%appdata%\.minecraft\resourcepacks\`.

1. Das Pack **kopieren** (nicht verschieben) und die Kopie entpacken, am besten mit **7-Zip**.
   Manche Packs (auch BluxyCrystal) sind absichtlich so gepackt, dass der Windows-Explorer sie nicht öffnen kann. 7-Zip schafft das.
2. Im entpackten Ordner findest du `assets\minecraft\textures\...`. Da liegen alle Bilder.

---

## 3. Einen Style anlegen

Ein Style ist einfach ein Ordner. Beispiel für den Kristall aus BluxyCrystal:

```
config\arnestyles\styles\
└── crystal\
    └── bluxy\                          ← Name frei wählbar (keine Leerzeichen nötig)
        ├── style.json                  ← optional: Anzeigename
        └── assets\
            └── minecraft\
                └── textures\
                    └── entity\
                        └── end_crystal\
                            └── end_crystal.png
```

**Wichtig:** Der Pfad ab `assets\` muss **genau gleich** sein wie im Texture-Pack. Du kopierst also die Datei mitsamt ihren Ordnern.

`style.json` (optional, sonst wird der Ordnername angezeigt):

```json
{"name": "BluxyCrystal"}
```

Du kannst in einer Kategorie beliebig viele Styles haben, zum Beispiel `crystal\bluxy\`, `crystal\anderes_pack\` usw. Im Menü erscheint dann für jeden Style eine eigene Karte mit Vorschau.

---

## 4. Welche Datei gehört wohin?

Alle Pfade beginnen mit `assets\minecraft\`.

| Was | Kategorie-Ordner | Dateien im Pack |
|---|---|---|
| End-Kristall | `crystal` | `textures\entity\end_crystal\end_crystal.png` (optional `end_crystal_beam.png`) |
| Feuer | `fire` | `textures\block\fire_0.png`, `fire_1.png` **und** die passenden `.png.mcmeta`-Dateien |
| Seelenfeuer | `fire` | `textures\block\soul_fire_0.png`, `soul_fire_1.png` + `.mcmeta` |
| Obsidian | `obsidian` | `textures\block\obsidian.png` (optional `crying_obsidian.png`) |
| Inventar | `inventory` | `textures\gui\container\inventory.png` |
| Kreativ-Inventar | `inventory` | `textures\gui\container\creative_inventory\*.png` |
| Truhen / Shulker | `inventory` | `textures\gui\container\generic_54.png`, `shulker_box.png` |
| Schwerter | `tools` | `textures\item\diamond_sword.png`, `netherite_sword.png` usw. |
| Rüstung | `armor` | `textures\entity\equipment\...` und `textures\item\..._chestplate.png` usw. (das alte `textures\models\armor` funktioniert in 1.21.11 nicht mehr) |
| Respawn Anchor | `anchor` | `textures\block\respawn_anchor_*.png` |
| Hotbar, Herzen, Fadenkreuz | `hud` | `textures\gui\sprites\hud\...` |
| Perle, Gaps, Tränke + Haltung | `items` | `textures\item\...` und `models\item\*.json` |
| Totem (Textur, Modell, Pop-Partikel, Sound) | `totems` | `textures\item\totem_of_undying.png`, `models\item\totem_of_undying.json`, `textures\particle\glitter_*.png`, `sounds\item\totem\...` |
| Partikel | `particles` | `textures\particle\...` |
| Sonne, Mond, Wolken, Regen | `sky` | `textures\environment\...` (Sonne und Mond liegen in 1.21.11 unter `celestial\`) |

**`.mcmeta`-Dateien immer mitkopieren.** Sie gehören zu animierten Texturen wie dem Feuer. Ohne sie sieht die Animation kaputt aus.

### Wie Items in der Hand gehalten werden

Wie groß ein Item ist und wie du es hältst, steht nicht in den Bildern, sondern in Modell-Dateien: `assets\minecraft\models\item\*.json`. Wenn dir die Haltung aus einem Pack gefällt, kopier die passenden `.json`-Dateien mit in den Style, zum Beispiel nach `tools\...\assets\minecraft\models\item\diamond_sword.json`.
In BluxyCrystal stecken solche Dateien zum Beispiel für das Totem (`totem_of_undying.json` und `smaller_util.json`). Manche Modelle verweisen auf ein anderes Modell (`"parent": "item/smaller_util"`). Dann musst du das auch mitkopieren.

---

## 5. Im Spiel auswählen

1. **K** drücken (in den Steuerungs-Einstellungen änderbar).
2. Links die Kategorie wählen, rechts die Karte des Styles anklicken. Die Kategorie wird dabei automatisch aktiv (grüner Punkt in der Liste).
3. Menü schließen. Die Texturen werden jetzt neu geladen, das dauert ein paar Sekunden.

Weitere Bedienung:

- **Standard**-Karte = normales Minecraft für diese Kategorie.
- **„Kategorie aktiv: Ja/Nein“** schaltet die ganze Kategorie aus, ohne deine Auswahl zu vergessen.
- **F5** oder **„Neu laden“**: neue Style-Ordner erkennen, ohne das Spiel neu zu starten.
- Pfeiltasten ↑↓ wechseln die Kategorie, ←→ den Style.

### Essentials

In der Kategorie **Essentials** gibt es keine Styles, sondern Regler und Schalter. Du kannst beliebig viele gleichzeitig anmachen. Fährst du mit der Maus über einen Eintrag, steht oben, was er macht.

**Regler** (ziehen zum Einstellen, Rechtsklick setzt auf normal zurück):

| Regler | Was er macht |
|---|---|
| Totem-Pop-Größe | Größe der Totem-Animation auf dem Bildschirm, 0 % = ganz aus |
| Totem-Partikel | Menge der Partikel beim Totem-Pop, 0 % = keine |
| Item-Größe in der Hand | alle Items in der Hand größer oder kleiner (30–150 %) |
| Low-Fire-Tiefe | wie tief und klein das Feuer ist, wenn Low Fire an ist |

**Schalter:**

| Essential | Was es macht |
|---|---|
| Fullbright | alles hell, auch in Höhlen und nachts |
| Low Fire | Feuer vor den Augen tiefer und kleiner |
| No Explosion Particles | keine Explosions-Wolken bei Kristallen, Ankern, TNT |
| No Smoke | kein Rauch von Feuer, Fackeln, Lagerfeuern und Lava |
| Kein Kürbis-Rahmen | kein Rahmen beim Tragen eines geschnitzten Kürbisses |
| NoFog | kein Nebel in Wasser, Lava, Pulverschnee und in der Ferne, kein Schleier unter Wasser (Blindheit bleibt) |
| Small Totem | Totem kleiner in der Hand |
| Small Gap | goldene Äpfel kleiner in der Hand und beim Essen |
| Small Mace | Streitkolben kleiner in der Hand |
| Kleine Items | Perlen, Tränke, Essen, Eimer, Raketen, Wind Charges usw. kleiner |
| Low Shield | Schild tiefer, auch beim Blocken |
| ObvPowderSnow | Pulverschnee mit rotem Rand, damit du ihn sofort siehst |

Alles davon ist rein optisch, am Spiel selbst ändert sich nichts. Ausnahme: NoFog (wie Fullbright) lässt dich weiter sehen als normal. Manche Server verbieten das.

**Eigene Essentials:** Leg in `styles\essentials\` einen Ordner an, der wie ein Style aufgebaut ist (mit `assets\...`). Er taucht dann unten in der Essentials-Liste als eigener Schalter auf. Damit kannst du z. B. die kleinen Item-Modelle aus einem Pack übernehmen.

### Vorschau

| Kategorie | Vorschau |
|---|---|
| Crystal | echter, drehender Kristall mit der Textur des Styles |
| Fire, Obsidian, Tools, Armor, Anchor | die Textur groß, Feuer animiert |
| Inventory | das Inventar-Fenster |
| HUD, Sky | die Hotbar bzw. die Sonne (ein eigenes `preview.png` im Style-Ordner hat Vorrang) |
| Items, Tools, Armor, Partikel | die erste typische Textur, die das Pack mitbringt (z. B. Totem, Schwert) |
| Essentials | kleines Symbol vor jedem Schalter (bei eigenen Ordnern das `preview.png`) |

---

## Eigene Farbe (Kategorie)

Bei **Stone, Deepslate, Erze, Gras, Snow, Feuer, Obsidian, Glowstone, Anchor und Crystal** gibt es über den Karten den Schalter **„Eigene Farbe“** mit Farbton (Farbspektrum), Sättigung und Helligkeit. Alle Karten zeigen die Farbe sofort als Vorschau. Im Spiel gilt sie, sobald du das Menü schließt.

- Eingefärbt wird der gewählte Style. Ist „Default“ gewählt (oder die Kategorie aus), nimmt ArneTweaks als Basis **Bauloo Privat** (Stone, Deepslate) bzw. die normale Minecraft-Textur (alle anderen).
- Die Helligkeits-Unterschiede der Textur bleiben erhalten, nur die Farbe wird ersetzt.
- Gras hat mit eigener Farbe überall dieselbe Farbe, unabhängig vom Biom.
- Rechtsklick auf einen Regler setzt ihn zurück.
- **Erze:** „Nur Erz-Flecken“ färbt nur die Erz-Stücke. „Stein wie Stone-Farbe“ gibt dem Stein um die Erze automatisch die eigene Farbe von Stone bzw. Deepslate, damit Erze in eingefärbtem Stein nicht herausstechen.

## Customize: jede einzelne Textur einfärben

Der Tab **„Customize“** (ganz rechts in der Leiste) zeigt links alle Kategorien. Rechts wählst du oben mit ‹ › den Style, darunter siehst du alle seine Texturen als Kacheln. Kachel anklicken, dann im **Farbkreis** die Farbe wählen (Winkel = Farbton, Abstand zur Mitte = Sättigung), darunter die Helligkeit.

- Die Farbe gilt nur für diese eine Textur dieses Styles und geht vor der Farbe der Kategorie. Ein lila Punkt auf der Kachel zeigt, dass sie eine eigene Farbe hat.
- **„Farbton drehen“**: statt einer einzigen Farbe werden alle Farben der Textur zusammen gedreht, so bleiben bunte Texturen (Totems, Tränke) bunt.
- **„Für alle“** gibt allen Texturen dieses Styles dieselbe Farbe, **„Zurücksetzen“** entfernt die Farbe der gewählten Textur.
- Die kleinen Farb-Bereiche in den einzelnen Kategorien bleiben und gelten weiterhin für die ganze Kategorie.

## Menü: Spielmodi oben

Oben unter „Mod an“ und „Ordner öffnen“ stehen die Spielmodi (Vanilla, UHC, Pot, ...) als Leiste, ganz rechts „Allgemein“ für Inventar, HUD, Fadenkreuz usw. Links siehst du die Items des gewählten Spielmodus, darüber Presets, Mods, Shader, Design und Pack-Import. Passen nicht alle Namen in die Leiste, zeigen die übrigen Tabs nur ihr Symbol (Maus drüber zeigt den Namen). Die Reihenfolge der Tabs änderst du mit dem Sortier-Knopf neben der Suche.

## Design: Farben und Größe des Menüs

Links in der Seitenleiste unter Shader steht **„Design“**.
- **Menü-Größe** (50 bis 150 %) macht das ganze ArneTweaks-Menü kleiner oder größer. Die neue Größe gilt, sobald du den Regler loslässt.
- **Fertige Designs:** Standard, Weihnachten, Halloween, Ostern, Ozean und Hell. Anklicken genügt.
- **Eigene Designs:** „+ Neues Design“ kopiert das gerade aktive Design. Dann kannst du rechts den Namen eintippen, jede einzelne Farbe anklicken (Fenster, Rand, Kopfzeile, Akzent, Text, Schalter ...) und sie mit Farbkreis und Helligkeit ändern. Das Menü zeigt jede Änderung sofort. „Zurücksetzen“ holt die Farbe aus dem Design zurück, von dem du kopiert hast.
- Eigene Designs liegen als JSON in `config/arnestyles/themes` („Ordner öffnen“ auf der Design-Seite). Die Datei kannst du Freunden schicken, sie legen sie einfach in denselben Ordner.

## Pack-Import: ganze Packs automatisch einsortieren

Links in der Seitenleiste steht **„Pack-Import“**.
- Zieh ein Texture-Pack (.zip oder Ordner) einfach ins Minecraft-Fenster, während das ArneTweaks-Menü offen ist. Oder leg es in den Ordner `config/arnestyles/import` („Ordner öffnen“ auf der Import-Seite) und klick auf „Importieren“ bzw. „Alle importieren“.
- Jede Datei kommt automatisch in ihre Kategorie: Totems, Schwerter, Erze, Stone, Gras, Rüstung, Himmel, Sounds usw. Was in keine Kategorie passt, wird weggelassen.
- Alte Packs werden angepasst: alte Rüstung (bis 1.21.1) wird ins neue Format umgerechnet, alte Dateinamen wie `grass.png` werden umbenannt.
- Danach zeigt jede Zeile, in welchen Kategorien das Pack jetzt ist. Ein Klick auf eine Kategorie öffnet sie und wählt den neuen Style direkt aus.
- Der Style heißt wie das Pack. Wenn du dasselbe Pack noch einmal importierst, wird der alte Style ersetzt.

## HUD bearbeiten

Auf der Seite **Mods** unter „HUD-Positionen“ öffnet **„HUD bearbeiten“** den Editor (nur in einer Welt). Dort ziehst du die Rüstungsanzeige und den Totem-Zähler mit der Maus an eine andere Stelle, das Mausrad macht sie größer oder kleiner (50 bis 300 %), Rechtsklick setzt eine Anzeige zurück. Damit man etwas sieht, zeigen die Anzeigen im Editor Beispiel-Rüstung und -Totems.

## Info-HUD und PvP-HUD

Auf der Seite **Mods** gibt es zwei neue Bereiche. Alles ist erst aus und lässt sich einzeln einschalten. Jede Anzeige kannst du im HUD-Editor verschieben und mit dem Mausrad größer oder kleiner machen.
- **Info-HUD:** Keystrokes (W, A, S, D, Maustasten mit CPS, Leertaste), CPS-Zähler (links | rechts und dein Maximum), FPS, Ping, Koordinaten mit Himmelsrichtung.
- **PvP-HUD:** Combo-Zähler, Pearl-Cooldown als Balken unter dem Fadenkreuz, Rüstungs-Warnung (blinkt unter 10 % Haltbarkeit, mit Warnton) und Gegner-Info (Name, Leben, Rüstung, Items). Die Gegner-Info ist auf manchen Servern nicht erlaubt.

Im Customize-Tab sind Zustände einer Textur jetzt eine Kachel (zum Beispiel die fünf Ladestufen des Anchors). Die kleine Zahl auf der Kachel zeigt, wie viele es sind, die Farbe gilt für alle.

## PvP-Optik

Ebenfalls auf der Seite **Mods**, alles nur bei dir sichtbar:
- **Eigene Treffer-Farbe:** Getroffene Spieler und Mobs blinken in deiner Farbe statt rot.
- **Feuer auf Spielern tiefer:** Brennende Gegner sieht man besser.
- **Endkristall:** Größe (30 bis 150 %), Drehtempo (bis Stillstand) und Hüpfen an/aus. Die Trefferfläche bleibt gleich.
- **Kill-Effekte:** Blitz, Feuerwerk, Totem-Partikel und/oder Sound, wenn jemand stirbt, den du in den letzten 5 Sekunden getroffen hast (wahlweise nur bei Spielern).
- **Totem-Pop:** anderer Sound (Glocke, Amboss, Level-Up, Pling) und Lautstärke bis stumm.

## Optik: Umrandung, Zeit, Himmel, Items

Auch auf der Seite **Mods**, alles nur bei dir sichtbar:
- **Block-Umrandung:** eigene Farbe oder Regenbogen und Dicke (1x bis 6x) für die Linie um den Block, auf den du schaust.
- **Eigene Zeit:** Immer dieselbe Tageszeit (Regler mit Uhrzeit, z. B. 12:00 oder 18:30). Der Server merkt davon nichts.
- **Eigene Himmelsfarbe:** Der Himmel in der Oberwelt in einer Farbe deiner Wahl.
- **Item-Physik:** Gedroppte Items liegen flach auf dem Boden, Blöcke stehen darauf, nichts dreht oder wippt.

## Komfort: Zoom, Freelook, Auto-GG, Presets teilen

- **Zoom:** Taste **C** halten (in den Minecraft-Steuerungs-Einstellungen unter „ArneTweaks“ änderbar). Mit dem Mausrad zoomst du währenddessen näher ran oder weiter weg (1,5x bis 50x). Auf der Mods-Seite stellst du Zoom-Stufe, weichen Zoom und langsamere Maus ein. Ist die Mod Zoomify installiert, bleibt der eingebaute Zoom aus.
- **Freelook:** linke **Alt**-Taste halten: umschauen, während du weiter geradeaus läufst, wahlweise in der Ansicht von hinten. Erst auf der Mods-Seite einschalten, auf manchen Servern nicht erlaubt.
- **Auto-GG:** schreibt „gg“ in den Chat, wenn ein Spieler stirbt, den du gerade getroffen hast.
- **Nächstes Preset:** eigene Taste (in den Steuerungs-Einstellungen belegen), lädt reihum deine Presets.
- **Presets teilen:** Auf der Presets-Seite kopiert „Code“ ein Preset als Text (beginnt mit `ARNE1:`). „Code einfügen“ speichert einen Code aus der Zwischenablage als neues Preset. Styles, die der andere nicht hat, bleiben bei ihm einfach auf Standard.

## Erze

Erze haben seit Version 2.1 eine eigene Kategorie **„Erze“** (unter Terrain). Vorher steckten sie in Stone und Deepslate, deshalb zeigten manche Packs dort ein Erz statt Stein. Wer vorher einen Stone- oder Deepslate-Style gewählt hatte, bekommt dieselben Erze automatisch unter „Erze“.

## Shader

Die Seite **„Shader“** im Menü hat zwei Teile. „Ordner öffnen“ öffnet auf dieser Seite den Shader-Ordner `config\arnestyles\styles\shaders\`.

**ArneTweaks-Shader (ohne Iris):** Bild-Shader, die das fertige Bild verändern (Farben, Kontrast, Glühen, Schärfe). Eingebaut sind Kräftig, Warm, Kalt, Kino, Noir, Scharf und Glühen. Eigene kommen als Ordner mit einer Datei `shader.fsh` in den Shader-Ordner (Anleitung und Beispiel: `LIES MICH.txt` im Ordner). Nach Änderungen den Shader einfach nochmal anklicken.

**Iris-Shaderpacks (BSL, Complementary, ...):** normale Shader-Packs als .zip in denselben Ordner. Die gehen nur, wenn die Mod **Iris** installiert ist (zusammen mit Sodium), weil sie Schatten, Wasser und Licht komplett neu zeichnen. Ohne Iris steht dort nur ein Hinweis.

## Hauptschalter

Oben rechts im Menü schaltet **„Mod an/aus“** die ganze Mod ab: keine Styles, keine Essentials, Minecraft sieht normal aus. Deine Auswahl bleibt dabei gespeichert.

## Eigene Himmel (Sky)

Viele Himmel-Packs nutzen das OptiFine-Format (`optifine\sky\...`). Das kann Minecraft nicht selbst anzeigen. Installiere dafür zusätzlich die Mod **Skyboxify** (Fabric, 1.21.11). ArneTweaks liefert die Himmel-Dateien mit, Skyboxify zeichnet sie. Sehr große Himmel-Bilder werden beim Einbauen auf 3072 Pixel Breite verkleinert.

## 6. Was nicht geht

- **OptiFine-Funktionen** wie eigener Himmel (`optifine\sky\`), Texturen je nach Item-Name (`optifine\cit\`), leuchtende Texturen (`_e.png`) oder verbundene Glas-Texturen (`ctm`). Normales Minecraft kann das nicht. Solche Ordner im Pack einfach ignorieren.
- **Sounds** gehen technisch auch (`assets\minecraft\sounds\...`), haben aber keine eigene Kategorie und keine Vorschau.

## 7. Wenn etwas nicht klappt

- **Style taucht nicht im Menü auf:** Im Style-Ordner fehlt der Unterordner `assets`, oder du hast noch nicht F5 gedrückt.
- **Textur ändert sich nicht:** Pfad prüfen. Er muss Buchstabe für Buchstabe wie im Pack sein, auch die Kleinschreibung. Dann im Menü prüfen, ob die Kategorie aktiv ist.
- **Feuer flackert komisch:** Die `.mcmeta`-Dateien fehlen.
- Hinweise stehen auch in `%appdata%\.minecraft\logs\latest.log`, wenn du nach `arnestyles` suchst.

## Rechtliches

Die Texturen gehören den Erstellern der Packs. Für dich selbst ist das kein Problem. Wenn du Styles oder die Mod mit fremden Texturen weitergibst, frag vorher die Ersteller.

## Packs automatisch einsortieren

Am einfachsten geht das im Spiel über **Pack-Import** (siehe oben). Im Ordner `tools\` liegt außerdem das Skript, mit dem ich die eingebauten Packs einsortiert habe:

```
python tools\sort_packs.py <styles-ordner> <entpacktes-pack> <style-id> "<Name>" [weitere Packs ...]
powershell -File tools\convert_sky.ps1 -StylesRoot <styles-ordner>
```

Das erste Skript verteilt die Dateien auf die Kategorien und rechnet alte Rüstungen um. Das zweite stellt Sonne und Mond auf das 1.21.11-Format um.
