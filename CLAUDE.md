# ArneTweaks – Hinweise für Claude

Clientseitige Fabric-Mod (Mod-ID `arnestyles`, Jar `arne-tweaks-<version>.jar`). Antworten und Kommentare auf Deutsch.

## Versionen (Quelle: `gradle.properties`)

- Minecraft **1.21.11**, Fabric Loader 0.20.1, Fabric API 0.141.6+1.21.11
- Fabric Loom 1.19 (Plugin `net.fabricmc.fabric-loom-remap`, das ist **kein** Tippfehler), Gradle 9.8.2
- Java: Code wird mit `release = 21` gebaut, Mixins mit `compatibilityLevel: JAVA_21`. Keine Sprachfeatures über Java 21 verwenden.
- Mappings: **Yarn** `1.21.11+build.6`

## Yarn statt Mojang-Namen

Dieses Projekt nutzt Yarn. Immer Yarn-Namen verwenden, nie Mojang-Namen. Beispiele:
`MinecraftClient` (nicht `Minecraft`), `World`/`ClientWorld` (nicht `Level`), `DrawContext` (nicht `GuiGraphics`),
`Identifier` (nicht `ResourceLocation`), `Text` (nicht `Component`), `PlayerEntity` (nicht `Player`).
Viele Tutorials und KI-Wissen nutzen Mojang-Namen. Vor dem Schreiben von Mixin-Zielen (Klasse, Methode, Descriptor)
den Namen in den Yarn-Quellen prüfen, z. B. über `.\gradlew.bat genSources` oder die Fabric-Doku.

## Doku nachschlagen (Context7)

Bei Fragen zu Fabric API, Events, Networking, Rendering, GUI oder Registries **zuerst Context7 nutzen**, nicht aus dem Gedächtnis antworten.
Die APIs ändern sich zwischen Minecraft-Versionen stark. Passende Bibliotheken:
- `/websites/fabricmc_net` (Fabric-Doku, Version 1.21.11)
- `/fabricmc/fabric-api`

Hinweis: Fabric-Doku zeigt Code oft mit Mojang-Namen. Beim Übernehmen in Yarn übersetzen.

## Projektaufbau

- `splitEnvironmentSourceSets()`: `src/main/java` (gemeinsam, kaum Code) und `src/client/java` (fast alles).
  Client-Klassen (Rendering, Screens, `MinecraftClient`) gehören nach `src/client/`.
- `com.arnestyles.client.mixin` – alle Mixins, registriert in `src/client/resources/arnestyles.client.mixins.json`.
  Neue Mixins dort eintragen, sonst werden sie nicht geladen.
- `client/mods/` – einzelne Features (HUDs, PvP-Visuals, Performance …), `client/gui/` – eigene Screens,
  `client/config/` – Einstellungen, `client/style/` – Texture-Styles, `updater/` – signierter Auto-Updater.
- `tools/` – Python/PowerShell-Hilfsskripte für die Texture-Packs (siehe README). Nicht ohne Rückfrage ausführen.
- Mod Menu, Sodium und Iris sind nur zum Kompilieren/Testen eingebunden, nicht in der fertigen Mod.

## Bauen und prüfen

- Zum Prüfen von Änderungen: `.\gradlew.bat build -x installToMinecraft -x signUpdate`
  - Wichtig: Das normale `build` startet danach `installToMinecraft` (kopiert die Jar nach `%appdata%\.minecraft\mods`
    und **löscht alte ArneTweaks-Versionen dort**) und `signUpdate` (signiert mit dem privaten Schlüssel).
    Beides nur ausführen, wenn Arne es ausdrücklich möchte.
- Nach jeder Code-Änderung bauen und Fehler beheben, bevor du fertig meldest.
- Im Spiel testen: `.\gradlew.bat runClient` (mit Iris/Sodium: `-PwithIris`).
- CI (`.github/workflows/build.yml`) baut mit JDK 25 auf Ubuntu.

## Regeln

- Den privaten Update-Schlüssel (`%USERPROFILE%\.arnetweaks\update-signing.key`) nie lesen, ausgeben oder committen.
- `updateKeygen` nie ausführen (überschreibt sonst den Schlüssel für Updates).
- Performance: In Render-Mixins und pro Frame/Tick aufgerufenem Code keine Objekte unnötig neu anlegen,
  keine Datei- oder Netzwerkzugriffe. Teure Arbeit cachen.
- Lizenz ist „Alle Rechte vorbehalten“. Keinen fremden Code ohne Lizenzprüfung übernehmen; fremde Assets in `THIRD_PARTY_NOTICES.txt` vermerken.
