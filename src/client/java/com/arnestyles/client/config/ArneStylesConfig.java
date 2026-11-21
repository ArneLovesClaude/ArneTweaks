package com.arnestyles.client.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Datenmodell der Konfigurationsdatei config/arnestyles.json.
 * Felder sind oeffentlich, damit Gson sie direkt lesen und schreiben kann.
 */
public final class ArneStylesConfig {
    public static final int CONFIG_VERSION = 8;
    public static final String DEFAULT_STYLE = "default";
    private static final String OLD_LOW_FIRE_STYLE = "low_fire";
    private static final String LOW_FIRE_ESSENTIAL = "low_fire";

    public int configVersion = CONFIG_VERSION;

    /** Hauptschalter: aus = die Mod aendert gar nichts (keine Styles, keine Essentials). */
    public boolean modEnabled = true;

    /** Aufgeklappte Ordner in der Seitenleiste (Namen aus CategoryGroup). */
    public List<String> expandedGroups = new ArrayList<>();

    /** Gewaehlter Spielmodus-Tab oben im Menue ("G:VANILLA", "T:OTHER" ...). Leer = passend zur Kategorie. */
    public String activeTab = "";

    /** Zuletzt geoeffnete Kategorie im GUI (Name aus StyleCategory). */
    public String activeCategory = StyleCategory.CRYSTAL.name();

    /** Pro Kategorie die ausgewaehlte Style-ID. "default" bedeutet: normales Minecraft. */
    public Map<String, String> selectedStyles = new LinkedHashMap<>();

    /** Pro Kategorie, ob der Style aktiv ist. */
    public Map<String, Boolean> enabledCategories = new LinkedHashMap<>();

    /** Essentials, die an sind. Eingebaute ueber ihre ID, eigene Ordner als "folder:<name>". */
    public Map<String, Boolean> essentials = new LinkedHashMap<>();

    /** Farben als 0xRRGGBB, Schluessel z. B. "crystal.glow". */
    public Map<String, Integer> colors = new LinkedHashMap<>();

    /** Visuelle Rotationsgeschwindigkeit der Endkristalle (Faktor, 1.0 = normal). */
    public float crystalRotationSpeed = 1.0f;

    /** Nur noch zum Umstellen alter Configs (Version 2). null wird von Gson nicht geschrieben. */
    public Boolean lowFire = null;

    /** Low Fire: wie weit das Feuer-Overlay nach unten verschoben und verkleinert wird (0.0 bis 0.5). */
    public float lowFireOffset = 0.25f;

    /** Groesse der Totem-Pop-Animation auf dem Bildschirm (0.0 = aus, 1.0 = normal). */
    public float totemPopScale = 1.0f;

    /** Menge der Totem-Pop-Partikel (0.0 = keine, 1.0 = normal). */
    public float totemParticles = 1.0f;

    /** Groesse der Items in der Hand (Egoperspektive), 1.0 = normal. */
    public float heldItemScale = 1.0f;

    /** Lautstaerke der Sounds aus eingebauten Packs (0.0 bis 2.0, 1.0 = normal). */
    public float packSoundVolume = 1.0f;

    // ---- Mods (Seite "Mods") ----

    /** Totem Pop Chams: farbige Silhouette an der Stelle, an der ein Spieler einen Totem poppt. */
    public boolean popChams = true;
    public boolean popChamsOwn = false;
    public boolean popChamsFade = true;
    public boolean popChamsDisperse = false;
    /** Wie lange die Silhouette sichtbar ist, in Sekunden. */
    public float popChamsLifetime = 1.5f;
    /** Deckkraft der Silhouette (0.1 bis 1.0). */
    public float popChamsOpacity = 0.6f;
    /** Farbe als 0xRRGGBB. */
    public int popChamsColor = 0xB794F6;

    /** Shield Fixes: andere Spieler werden sicher blockend angezeigt (MC-238293). */
    public boolean shieldFix = true;
    /** Erst nach den 5 Ticks Schild-Verzoegerung als blockend zeigen. */
    public boolean shieldFixDelay = false;
    /** Schild-Geraeusch sofort beim eigenen Treffer abspielen (fuer Server, die es nicht schicken). */
    public boolean shieldSounds = false;

    /** Shield Status: Schild gruen, wenn es blocken kann, rot, wenn es durch eine Axt ausgeschaltet ist. */
    public boolean shieldStatus = true;
    public boolean shieldStatusOthers = true;
    /** Rot geht langsam in Gruen ueber, statt sofort umzuspringen. */
    public boolean shieldStatusFade = false;

    /** TotemCounter: Pops im Namensschild der Spieler und eigene Totems im HUD. */
    public boolean totemCounter = true;
    public boolean totemCounterHud = true;

    /** Armor HUD: Ruestung neben der Hotbar. */
    public boolean armorHud = true;
    public boolean armorHudRight = false;
    public boolean armorHudPercent = false;

    /** Status Effect Timer: Restzeit und Stufe auf den Effekt-Symbolen. */
    public boolean effectTimer = true;
    public boolean effectTimerLevel = true;

    /** Inventory Scale: Groesse von Inventar und Kisten, unabhaengig von der GUI-Groesse (1.0 = normal). */
    public float inventoryScale = 1.0f;

    /** Motion Blur: Bilder werden ineinander gemischt. Standardmaessig aus. */
    public boolean motionBlur = false;
    /** Anteil des alten Bildes bei 60 FPS (0.1 bis 0.9). */
    public float motionBlurStrength = 0.5f;

    /** Hand-Editor: Position, Drehung und Groesse pro Item in der Hand (Egoperspektive), Schluessel ist die Item-ID. */
    public Map<String, HandTransform> handTransforms = new LinkedHashMap<>();

    /** Verschiebung in Bloecken, Drehung in Grad, Groesse als Faktor (1.0 = normal). */
    public static final class HandTransform {
        public float x = 0.0f;
        public float y = 0.0f;
        public float z = 0.0f;
        public float rotX = 0.0f;
        public float rotY = 0.0f;
        public float rotZ = 0.0f;
        public float scale = 1.0f;

        public boolean isDefault() {
            return x == 0 && y == 0 && z == 0 && rotX == 0 && rotY == 0 && rotZ == 0 && scale == 1.0f;
        }
    }

    /** Saison-Designs: im Dezember Weihnachten, um Ostern Ostern, im Oktober Halloween (das eigene Design bleibt gespeichert). */
    public boolean seasonalThemes = false;

    /** Position und Groesse der eigenen HUD-Anzeigen (HUD-Editor), Schluessel ist die Anzeige (armor_hud, totem_counter). */
    public Map<String, HudPosition> hudPositions = new LinkedHashMap<>();

    /** Verschiebung in GUI-Pixeln gegenueber der normalen Position und Groesse (1.0 = normal). */
    public static final class HudPosition {
        public float x = 0.0f;
        public float y = 0.0f;
        public float scale = 1.0f;
    }

    /** Eigene Farbe pro Kategorie (Stone, Deepslate, Gras, Feuer), Schluessel ist der Kategorie-Name. */
    public Map<String, CategoryTint> tints = new LinkedHashMap<>();

    /**
     * Farbe fuer eine Kategorie oder eine einzelne Textur. Werte von 0 bis 1.
     * mode 0 = Farbe ersetzen (hue/saturation sind die neue Farbe), mode 1 = Farbton drehen
     * (hue = Drehung, saturation und brightness 0.5 = unveraendert), damit bunte Texturen bunt bleiben.
     */
    public static final class CategoryTint {
        public boolean on = false;
        public float hue = 0.58f;
        public float saturation = 0.6f;
        public float brightness = 0.5f;
        public int mode = 0;
        /** Nur bei Erzen: nur die Erz-Flecken einfaerben, der Stein-Hintergrund bleibt. */
        public boolean oreSpots = true;
        /** Regenbogen: die Farbe laeuft im Kreis (nur Items, Bloecke und HUD, Minecraft animiert keine Entity-Texturen). */
        public boolean rainbow = false;
        /** Sekunden fuer eine volle Runde durch alle Farben. */
        public float rainbowSeconds = 3.0f;

        public CategoryTint copy() {
            CategoryTint copy = new CategoryTint();
            copy.on = on;
            copy.hue = hue;
            copy.saturation = saturation;
            copy.brightness = brightness;
            copy.mode = mode;
            copy.oreSpots = oreSpots;
            copy.rainbow = rainbow;
            copy.rainbowSeconds = rainbowSeconds;
            return copy;
        }
    }

    /** Farbe pro einzelner Textur (Customize-Tab), Schluessel "KATEGORIE|style|textures/...png". Geht vor der Kategorie-Farbe. */
    public Map<String, CategoryTint> textureTints = new LinkedHashMap<>();

    /** Erze: Stein-Hintergrund bekommt automatisch die eigene Farbe von Stone bzw. Deepslate, damit Erze nicht herausstechen. */
    public boolean oresFollowStone = true;

    /** ArneTweaks-Shader (ohne Iris): "builtin:<id>", "folder:<ordner>" oder "" fuer aus. */
    public String arneShader = "";

    /** Cubes Without Borders: Vollbild als rahmenloses Fenster (Alt+Tab ohne Flackern). */
    public boolean borderlessFullscreen = true;

    /** No Chat Restrictions: Chat, Server und Realms auch dann, wenn das Konto sie sperrt. */
    public boolean noChatRestrictions = true;

    /** AppleSkin: Saettigung im HUD, Vorschau beim Halten von Essen, Werte im Tooltip. */
    public boolean appleSkinSaturation = true;
    public boolean appleSkinPreview = true;
    public boolean appleSkinHealth = true;
    public boolean appleSkinTooltip = true;

    /** Eigene Reihenfolge der Seitenleiste (Schluessel wie "G:VANILLA", "C:HUD"). Leer = Standard. */
    public List<String> sidebarOrder = new ArrayList<>();

    /** Info-HUD (Version 2.6): alles aus, bis man es auf der Mods-Seite einschaltet. */
    public boolean keystrokes = false;
    public boolean keystrokesCps = true;
    public boolean cpsHud = false;
    public boolean fpsHud = false;
    public boolean pingHud = false;
    public boolean coordsHud = false;
    public boolean comboHud = false;
    public boolean pearlCooldown = false;
    public boolean armorWarning = false;
    public boolean armorWarningSound = true;
    /** Gegner-Info: auf manchen Servern nicht erlaubt, deshalb aus. */
    public boolean targetHud = false;

    /** PvP-Optik (Version 2.7). */
    /** Sound-Mixer: Gruppe -> Lautstaerke (0 bis 2, fehlt = 100 %). */
    public Map<String, Float> soundVolumes = new LinkedHashMap<>();
    /** Fadenkreuz faerbt sich, wenn man auf ein Lebewesen in Reichweite zielt. */
    public boolean crosshairTarget = false;
    public int crosshairTargetRgb = 0xFF4D4D;
    /** Treffer-Partikel: immer Krit- bzw. Schaerfe-Partikel, Menge 1 bis 5. */
    public boolean hitCrits = false;
    public boolean hitSharpness = false;
    public float hitParticleAmount = 1.0f;
    /** Wegpunkte im HUD anzeigen. */
    public boolean waypointsHud = true;
    public List<Waypoint> waypoints = new ArrayList<>();

    /** Gemerkte Position. world: "sp:<Weltname>" oder "mp:<Server>", dimension z. B. minecraft:overworld. */
    public static final class Waypoint {
        public String world = "";
        public String dimension = "";
        public String name = "";
        public int x;
        public int y;
        public int z;
    }

    /** Totem-Pop: Stil (0 normal, 1 ohne Drehung, 2 Mitte, 3 Mitte ohne Drehung), Tempo, Tonhoehe, Partikel-Farbe. */
    public float totemPopStyle = 0.0f;
    public float totemPopSpeed = 1.0f;
    public float totemPopPitch = 1.0f;
    public float totemParticleColor = 0.0f;
    /** Spotify-Anzeige (ohne Login, aus dem Fenstertitel). Lyrics nur wenn eingeschaltet (fragt lrclib.net). */
    /** CustomHud (mitgeliefert) wurde beim ersten Start eingerichtet (aus, Taste B frei). */
    public boolean customHudSetupDone = false;
    public boolean spotifyHud = false;
    public float spotifyMode = 4.0f;
    public boolean spotifyLyrics = false;
    public float spotifyOffset = 0.0f;
    public boolean spotifyHidePaused = false;
    /** Cover anzeigen, Kartenbreite, auch andere Player (Browser, ...) statt nur Spotify. */
    public boolean spotifyCover = true;
    public float spotifyWidth = 210.0f;
    public boolean spotifyAnyPlayer = false;
    public boolean hitColor = false;
    public int hitColorRgb = 0x8B5CF6;
    public float crystalSize = 1.0f;
    public boolean crystalBounce = true;
    public float playerFireOffset = 0.0f;
    public boolean killEffects = false;
    public boolean killOnlyPlayers = true;
    public boolean killLightning = true;
    public boolean killFirework = true;
    public boolean killTotem = false;
    public boolean killSound = true;
    /** 0 = normal, sonst Index in PvpVisuals.POP_SOUNDS. */
    public float totemPopSound = 0.0f;
    public float totemPopVolume = 1.0f;

    /** Optik (Version 2.8). */
    public boolean blockOutline = false;
    public int outlineColorRgb = 0x8B5CF6;
    public boolean outlineRainbow = false;
    public float outlineWidth = 2.0f;
    public boolean customTimeOn = false;
    public float customTime = 6000.0f;
    public boolean skyColorOn = false;
    public int skyColorRgb = 0xFF9A5C;
    public boolean itemPhysics = false;
    /** Customize-Tab: false = Einfach (eine Kachel pro Textur), true = Erweitert (jeder Zustand einzeln). */
    public boolean customizeAdvanced = false;

    // ---- Combat Hitboxes (F3+B) ----
    public boolean hitboxes = true;
    public boolean hitboxTargetColor = true;
    public boolean hitboxHurtColor = false;
    public boolean hitboxEyes = true;
    public boolean hitboxLook = true;
    public boolean hitboxLookArrow = false;
    public boolean hitboxOutline = false;
    public boolean hitboxHideFireworks = false;
    public boolean hitboxHideItems = false;
    public boolean hideStuckArrows = false;
    public int hitboxRgb = 0xFFFFFF;
    public int hitboxTargetRgb = 0xFF5555;
    public int hitboxHurtRgb = 0xFF55FF;
    public int hitboxEyeRgb = 0xFF5555;
    public int hitboxLookRgb = 0x5555FF;
    public int hitboxOutlineRgb = 0x000000;
    public float hitboxWidth = 2.5f;
    public float hitboxFarWidth = 2.5f;
    public float hitboxFarDistance = 32.0f;
    public float hitboxOutlineSize = 2.0f;

    /** Komfort (Version 2.9). */
    public boolean zoom = true;
    public float zoomLevel = 4.0f;
    public boolean zoomSmooth = true;
    public boolean zoomScroll = true;
    public boolean zoomSlowMouse = true;
    /** Freelook: auf manchen Servern nicht erlaubt, deshalb aus. */
    public boolean freelook = false;
    public boolean freelookThirdPerson = true;
    public boolean autoGg = false;
    public String autoGgMessage = "gg";
    /** Chat: Uhrzeit vor jeder Nachricht. */
    /** Eigene Trank-Effekt-Partikel in der Egoperspektive ausblenden (sonst bunte Flecken vor der Kamera). */
    public boolean hideOwnEffectParticles = true;
    public boolean chatTimestamps = true;
    /** Chat: gleiche Nachrichten hintereinander zu einer Zeile mit (x2), (x3) ... zusammenfassen. */
    public boolean chatStack = true;
    /** Todespunkt: Position im Chat und Anzeige mit Entfernung und Richtung. */
    public boolean deathPoint = true;
    /** Auto-Preset: Server-Adresse (klein geschrieben) -> Preset, das beim Betreten geladen wird. */
    public Map<String, String> serverPresets = new LinkedHashMap<>();
    /** Zuletzt mit der Taste geladenes Preset. */
    public String lastPreset = "";

    /** Design des Menues: "standard", ein anderes fertiges Design oder "custom:<datei>". */
    public String guiTheme = "arne";

    /** Groesse des Menues (1 = normal). */
    public float guiScale = 1.0f;
    /** Aufbau des Menues (siehe GuiLayout.Option), 0 = normaler Look. */
    public int guiCorners = 0;
    public int guiHeader = 0;
    public int guiSidebar = 0;
    public int guiActive = 0;
    public int guiButtons = 0;
    public int guiSwitches = 0;
    public int guiModsView = 0;
    public int guiGlass = 2;
    public int guiInspector = 0;
    public int guiSize = 1;
    /** true, sobald das neue Standard-Design (Arne Tweaks) einmal eingestellt wurde. */
    public boolean guiDesignV2 = false;


    // ---- 3.3: Spotify-Steuerung, Item-Zaehler, Tab-Ping, Fadenkreuz, Animationen, HUD-Stil ----
    public boolean spotifyShadow = true;
    public boolean itemCounter = false;
    public boolean itemCounterVertical = false;
    public boolean itemCounterHideZero = false;
    public boolean countTotem = true;
    public boolean countPearl = true;
    public boolean countCrystal = true;
    public boolean countObsidian = true;
    public boolean countAnchor = false;
    public boolean countGlowstone = false;
    public boolean countGapple = true;
    public boolean countXp = true;
    public boolean countArrow = false;
    public boolean countCobweb = false;
    public boolean countWindCharge = false;
    public boolean countSplash = false;
    public boolean tabPing = false;
    public boolean tabPingColors = true;
    public boolean tabPingSuffix = true;
    public boolean customCrosshair = false;
    public boolean crosshairOutline = false;
    public boolean animNoCooldown = false;
    public boolean animEatBob = true;
    public boolean hudTextShadow = true;
    public boolean hudBorder = false;
    public boolean keystrokesMouse = true;
    public boolean keystrokesSpace = true;
    public boolean coordsDirection = true;
    public boolean chatSeconds = false;
    public float spotifyBgOpacity = 0.85f;
    public float itemCounterWarn = 2.0f;
    public float tabPingGood = 80.0f;
    public float tabPingBad = 200.0f;
    public float crosshairOpacity = 1.0f;
    public float crosshairScale = 1.0f;
    public float animEquip = 1.0f;
    public float animSwingSpeed = 1.0f;
    public float animSwingMove = 1.0f;
    public float animSwingRotation = 1.0f;
    public float animEat = 1.0f;
    public float handX = 0.0f;
    public float handY = 0.0f;
    public float handZ = 0.0f;
    public float hudBgOpacity = 0.56f;
    public float armorWarningPercent = 0.1f;
    public float comboTimeout = 3.0f;
    public float waypointCount = 3.0f;
    public float deathPointMinutes = 15.0f;
    public int spotifyAccentRgb = 0x1DB954;
    public int itemCounterWarnRgb = 0xFF5555;
    public int crosshairRgb = 0xFFFFFF;
    public int crosshairOutlineRgb = 0x000000;
    public int hudTextRgb = 0xFFFFFF;
    public int hudBgRgb = 0x000000;
    public int hudBorderRgb = 0x35D6E8;
    public int hudAccentRgb = 0x2DD4BF;
    public int chatTimeRgb = 0x555555;
    /** Eigenes Fadenkreuz: 15 Zeilen mit je 15 Zeichen, # = Pixel an. */
    public List<String> crosshairPixels = new ArrayList<>(com.arnestyles.client.mods.CustomCrosshair.preset("cross"));

    // ---- Extra-Anzeigen (Uhr, Elytra, Server, Tage) ----
    public boolean clockHud = false;
    public boolean clockSeconds = false;
    public boolean clock12h = false;
    public boolean clockDate = true;
    public boolean clockYear = true;
    public boolean elytraHud = false;
    public boolean elytraOnlyFlying = false;
    public boolean elytraKmh = false;
    public boolean serverHud = false;
    public boolean dayHud = false;
    public boolean dayTime = false;

    // ---- Vanilla-HUD ----
    public boolean scoreboardHide = false;
    public boolean scoreboardHideNumbers = false;
    public boolean scoreboardShadow = false;
    public float scoreboardBgOpacity = 0.3f;
    public boolean bossbarHide = false;
    public boolean heldItemNameHide = false;
    public boolean effectIconsHide = false;
    public boolean hotbarSmooth = false;
    public float hotbarOpacity = 1.0f;
    public int hotbarSelectRgb = 0xFFFFFF;
    public boolean cooldownIndicator = false;
    public float cooldownStyle = 0.0f;
    public int cooldownRgb = 0xFFFFFF;

    // ---- Nametags ----
    public boolean nametags = false;
    public boolean nametagHealth = true;
    public boolean nametagPing = true;
    public boolean nametagShadow = false;
    public float nametagScale = 1.0f;
    public float nametagBgOpacity = 0.25f;

    /** Einfuehrung beim ersten Start schon gezeigt (oder uebersprungen). */
    public boolean introShown = false;
    /** Menue-Taste wegen Iris schon einmal von K auf Rechts-Shift verlegt (nur einmal, danach entscheidet der Spieler). */
    public boolean irisKeyMoved = false;

    /** Bringt geladene Werte in einen gueltigen Zustand und fuellt fehlende Eintraege auf. */
    public void sanitize() {
        if (guiTheme == null || guiTheme.isBlank()) {
            guiTheme = "arne";
        }
        if (!guiDesignV2) {
            // Einmalig: wer noch das alte Standard-Design ohne Aenderungen hatte, bekommt das neue Standard-Design
            boolean oldStandard = "standard".equals(guiTheme) && guiCorners == 0 && guiHeader == 0 && guiSidebar == 0
                && guiActive == 0 && guiButtons == 0 && guiSwitches == 0 && guiModsView == 0 && guiGlass == 0;
            boolean fresh = "arne".equals(guiTheme);
            if (oldStandard || fresh) {
                guiTheme = "arne";
                guiCorners = 0;
                guiHeader = 1;
                guiSidebar = 0;
                guiActive = 1;
                guiButtons = 0;
                guiSwitches = 0;
                guiModsView = 3;
                guiGlass = 2;
                guiInspector = 0;
                guiSize = 1;
            }
            guiDesignV2 = true;
        }
        if (!(guiScale >= 0.5f && guiScale <= 2.0f)) {
            guiScale = 1.0f;
        }
        com.arnestyles.client.gui.GuiLayout.sanitize(this);
        if (selectedStyles == null) {
            selectedStyles = new LinkedHashMap<>();
        }
        if (enabledCategories == null) {
            enabledCategories = new LinkedHashMap<>();
        }
        if (essentials == null) {
            essentials = new LinkedHashMap<>();
        }
        if (colors == null) {
            colors = new LinkedHashMap<>();
        }
        if (waypoints == null) {
            waypoints = new ArrayList<>();
        }
        waypoints.removeIf(w -> w == null || w.world == null || w.dimension == null || w.name == null);
        com.arnestyles.client.mods.ModSetting.Slider.clampAll(this);
        crosshairPixels = com.arnestyles.client.mods.CustomCrosshair.sanitize(crosshairPixels);
        hitParticleAmount = clamp(hitParticleAmount, 1.0f, 5.0f, 1.0f);
        totemPopStyle = clamp(totemPopStyle, 0.0f, 3.0f, 0.0f);
        spotifyMode = clamp(spotifyMode, 0.0f, 4.0f, 4.0f);
        spotifyOffset = clamp(spotifyOffset, -5.0f, 5.0f, 0.0f);
        spotifyWidth = clamp(spotifyWidth, 150.0f, 320.0f, 210.0f);
        totemPopSpeed = clamp(totemPopSpeed, 0.5f, 2.0f, 1.0f);
        totemPopPitch = clamp(totemPopPitch, 0.5f, 2.0f, 1.0f);
        totemParticleColor = clamp(totemParticleColor, 0.0f, 10.0f, 0.0f);
        if (soundVolumes == null) {
            soundVolumes = new LinkedHashMap<>();
        }
        soundVolumes.values().removeIf(v -> v == null || v.isNaN());
        soundVolumes.replaceAll((k, v) -> Math.max(0.0f, Math.min(2.0f, v)));
        if (serverPresets == null) {
            serverPresets = new LinkedHashMap<>();
        }
        if (handTransforms == null) {
            handTransforms = new LinkedHashMap<>();
        }
        handTransforms.values().removeIf(java.util.Objects::isNull);
        for (HandTransform t : handTransforms.values()) {
            t.x = clamp(t.x, -1.0f, 1.0f, 0.0f);
            t.y = clamp(t.y, -1.0f, 1.0f, 0.0f);
            t.z = clamp(t.z, -1.0f, 1.0f, 0.0f);
            t.rotX = clamp(t.rotX, -180.0f, 180.0f, 0.0f);
            t.rotY = clamp(t.rotY, -180.0f, 180.0f, 0.0f);
            t.rotZ = clamp(t.rotZ, -180.0f, 180.0f, 0.0f);
            t.scale = clamp(t.scale, 0.2f, 2.5f, 1.0f);
        }

        // Version 1: Low Fire war ein Style der Kategorie Fire. Version 2: eigener Schalter. Jetzt: ein Essential.
        if (OLD_LOW_FIRE_STYLE.equals(selectedStyles.get(StyleCategory.FIRE.name()))) {
            lowFire = true;
            selectedStyles.put(StyleCategory.FIRE.name(), DEFAULT_STYLE);
        }
        if (Boolean.TRUE.equals(lowFire)) {
            essentials.put(LOW_FIRE_ESSENTIAL, true);
            enabledCategories.put(StyleCategory.ESSENTIALS.name(), true);
        }
        lowFire = null;

        // Version 3: Totem-Pop und Totem-Partikel waren Schalter, jetzt Regler.
        if (Boolean.TRUE.equals(essentials.remove("small_totem_pop"))) {
            totemPopScale = 0.45f;
        }
        if (Boolean.TRUE.equals(essentials.remove("fewer_totem_particles"))) {
            totemParticles = 0.15f;
        }

        // Version 4: Terrain war eine Kategorie, jetzt gibt es Stone, Deepslate, Grass und Snow einzeln.
        String terrain = selectedStyles.remove("TERRAIN");
        Boolean terrainOn = enabledCategories.remove("TERRAIN");
        if (terrain != null) {
            for (StyleCategory split : new StyleCategory[] {StyleCategory.STONE, StyleCategory.DEEPSLATE, StyleCategory.GRASS, StyleCategory.SNOW}) {
                selectedStyles.putIfAbsent(split.name(), terrain);
                if (terrainOn != null) {
                    enabledCategories.putIfAbsent(split.name(), terrainOn);
                }
            }
        }

        // Version 5: Tools enthielt auch Schwerter, Aexte und Streitkolben. Jetzt sind das eigene Kategorien.
        String tools = selectedStyles.get(StyleCategory.TOOLS.name());
        Boolean toolsOn = enabledCategories.get(StyleCategory.TOOLS.name());
        if (configVersion < 6 && tools != null && !DEFAULT_STYLE.equals(tools)) {
            for (StyleCategory split : new StyleCategory[] {StyleCategory.SWORDS, StyleCategory.AXES, StyleCategory.MACE}) {
                selectedStyles.putIfAbsent(split.name(), tools);
                if (toolsOn != null) {
                    enabledCategories.putIfAbsent(split.name(), toolsOn);
                }
            }
        }
        if (expandedGroups == null) {
            expandedGroups = new ArrayList<>();
        }
        expandedGroups.replaceAll(name -> CategoryGroup.RENAMED.getOrDefault(name, name));

        // Version 6: No Water Overlay ist jetzt NoFog (kein Nebel und kein Wasser-Schleier).
        if (Boolean.TRUE.equals(essentials.remove("no_water_overlay"))) {
            essentials.put("no_fog", true);
        }

        // Version 7: Erze waren Teil von Stone und Deepslate, jetzt eigene Kategorie. Gleichen Pack auch fuer Erze nehmen.
        if (configVersion < 8 && !selectedStyles.containsKey(StyleCategory.ORES.name())) {
            for (StyleCategory old : new StyleCategory[] {StyleCategory.STONE, StyleCategory.DEEPSLATE}) {
                String style = selectedStyles.get(old.name());
                if (style != null && !DEFAULT_STYLE.equals(style)) {
                    selectedStyles.put(StyleCategory.ORES.name(), style);
                    enabledCategories.put(StyleCategory.ORES.name(), Boolean.TRUE.equals(enabledCategories.get(old.name())));
                    break;
                }
            }
        }

        // Eintraege fuer Kategorien, die es nicht mehr gibt (z. B. Cosmetics), entfernen.
        selectedStyles.keySet().removeIf(key -> !isCategory(key));
        enabledCategories.keySet().removeIf(key -> !isCategory(key));

        for (StyleCategory category : StyleCategory.values()) {
            String key = category.name();
            String style = selectedStyles.get(key);
            if (style == null || style.isBlank()) {
                selectedStyles.put(key, DEFAULT_STYLE);
            }
            if (enabledCategories.get(key) == null) {
                enabledCategories.put(key, false);
            }
        }
        essentials.values().removeIf(value -> value == null);
        colors.values().removeIf(value -> value == null);
        colors.replaceAll((key, value) -> value & 0xFFFFFF);

        if (!isCategory(activeCategory)) {
            activeCategory = StyleCategory.CRYSTAL.name();
        }

        if (Float.isNaN(crystalRotationSpeed)) {
            crystalRotationSpeed = 1.0f;
        }
        crystalRotationSpeed = Math.max(0.0f, Math.min(5.0f, crystalRotationSpeed));

        if (Float.isNaN(lowFireOffset)) {
            lowFireOffset = 0.25f;
        }
        lowFireOffset = clamp(lowFireOffset, 0.0f, 0.5f, 0.25f);
        totemPopScale = clamp(totemPopScale, 0.0f, 1.0f, 1.0f);
        totemParticles = clamp(totemParticles, 0.0f, 1.0f, 1.0f);
        heldItemScale = clamp(heldItemScale, 0.3f, 1.5f, 1.0f);
        packSoundVolume = clamp(packSoundVolume, 0.0f, 2.0f, 1.0f);
        popChamsLifetime = clamp(popChamsLifetime, 0.5f, 5.0f, 1.5f);
        popChamsOpacity = clamp(popChamsOpacity, 0.1f, 1.0f, 0.6f);
        popChamsColor &= 0xFFFFFF;
        inventoryScale = clamp(inventoryScale, 0.5f, 2.0f, 1.0f);
        motionBlurStrength = clamp(motionBlurStrength, 0.1f, 0.9f, 0.5f);
        if (arneShader == null) {
            arneShader = "";
        }
        if (hudPositions == null) {
            hudPositions = new LinkedHashMap<>();
        }
        hudPositions.values().removeIf(pos -> pos == null);
        for (HudPosition pos : hudPositions.values()) {
            pos.x = clamp(pos.x, -4000.0f, 4000.0f, 0.0f);
            pos.y = clamp(pos.y, -4000.0f, 4000.0f, 0.0f);
            pos.scale = clamp(pos.scale, 0.5f, 3.0f, 1.0f);
        }
        if (tints == null) {
            tints = new LinkedHashMap<>();
        }
        tints.values().removeIf(tint -> tint == null);
        if (textureTints == null) {
            textureTints = new LinkedHashMap<>();
        }
        textureTints.values().removeIf(tint -> tint == null);
        List<CategoryTint> allTints = new ArrayList<>(tints.values());
        allTints.addAll(textureTints.values());
        for (CategoryTint tint : allTints) {
            tint.mode = tint.mode == 1 ? 1 : 0;
            tint.hue = clamp(tint.hue, 0.0f, 1.0f, 0.58f);
            tint.saturation = clamp(tint.saturation, 0.0f, 1.0f, 0.6f);
            tint.brightness = clamp(tint.brightness, 0.0f, 1.0f, 0.5f);
            tint.rainbowSeconds = clamp(tint.rainbowSeconds, 0.5f, 20.0f, 3.0f);
        }
        textureTints.values().removeIf(tint -> !tint.on);
        if (sidebarOrder == null) {
            sidebarOrder = new ArrayList<>();
        }

        configVersion = CONFIG_VERSION;
    }

    private static float clamp(float value, float min, float max, float fallback) {
        if (Float.isNaN(value)) {
            return fallback;
        }
        return Math.max(min, Math.min(max, value));
    }

    private static boolean isCategory(String name) {
        if (name == null) {
            return false;
        }
        for (StyleCategory category : StyleCategory.values()) {
            if (category.name().equals(name)) {
                return true;
            }
        }
        return false;
    }

    public String getSelectedStyle(StyleCategory category) {
        return selectedStyles.getOrDefault(category.name(), DEFAULT_STYLE);
    }

    public void setSelectedStyle(StyleCategory category, String styleId) {
        selectedStyles.put(category.name(), styleId);
    }

    public boolean isEnabled(StyleCategory category) {
        return enabledCategories.getOrDefault(category.name(), false);
    }

    public void setEnabled(StyleCategory category, boolean enabled) {
        enabledCategories.put(category.name(), enabled);
    }

    public boolean isEssentialOn(String id) {
        return essentials.getOrDefault(id, false);
    }

    public void setEssentialOn(String id, boolean on) {
        if (on) {
            essentials.put(id, true);
        } else {
            essentials.remove(id);
        }
    }

    public int getColor(String key, int fallback) {
        Integer value = colors.get(key);
        return value == null ? fallback : value;
    }

    public void setColor(String key, int rgb) {
        colors.put(key, rgb & 0xFFFFFF);
    }
}
