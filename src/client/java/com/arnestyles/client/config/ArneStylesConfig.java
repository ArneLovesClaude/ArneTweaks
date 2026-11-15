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

        public CategoryTint copy() {
            CategoryTint copy = new CategoryTint();
            copy.on = on;
            copy.hue = hue;
            copy.saturation = saturation;
            copy.brightness = brightness;
            copy.mode = mode;
            copy.oreSpots = oreSpots;
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
    /** Zuletzt mit der Taste geladenes Preset. */
    public String lastPreset = "";

    /** Design des Menues: "standard", ein anderes fertiges Design oder "custom:<datei>". */
    public String guiTheme = "standard";

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
    public int guiGlass = 0;

    /** Bringt geladene Werte in einen gueltigen Zustand und fuellt fehlende Eintraege auf. */
    public void sanitize() {
        if (guiTheme == null || guiTheme.isBlank()) {
            guiTheme = "standard";
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
        lowFireOffset = Math.max(0.0f, Math.min(0.5f, lowFireOffset));
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
