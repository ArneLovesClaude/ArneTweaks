package com.arnestyles.client.gui;

import com.arnestyles.ArneStyles;
import com.arnestyles.client.config.ConfigManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Farben des ArneTweaks-Menues. Es gibt fertige Designs (Standard, Weihnachten, Halloween, ...) und eigene,
 * die als JSON in config/arnestyles/themes liegen und sich so auch mit anderen teilen lassen.
 */
public final class GuiTheme {
    /** Alle einstellbaren Farben. Alpha ist fest pro Farbe, eingestellt wird nur RGB. */
    public enum Slot {
        BACKDROP(0xC0),
        PANEL(0xF5),
        EDGE(0xFF),
        HEADER_TOP(0xFF),
        HEADER_BOTTOM(0xFF),
        SIDEBAR(0xFF),
        FIELD(0xFF),
        CARD(0xFF),
        CARD_HOVER(0xFF),
        ACCENT(0xFF),
        TEXT(0xFF),
        MUTED(0xFF),
        OK(0xFF),
        WARN(0xFF),
        DANGER(0xFF),
        OFF(0xFF),
        /** Zweite Akzentfarbe (kreative Bereiche: Texturen, Zeichnen, Farben). Fehlt sie, gilt ACCENT. */
        ACCENT2(0xFF);

        public final int alpha;

        Slot(int alpha) {
            this.alpha = alpha;
        }

        public String key() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /**
     * Ein Design. colors sind RGB ohne Alpha, Reihenfolge wie {@link Slot}.
     * base = fertiges Design, aus dem ein eigenes entstanden ist (fuer "Zuruecksetzen").
     */
    public record Theme(String id, String name, boolean builtIn, int[] colors, String base) {
        public int rgb(Slot slot) {
            int i = slot.ordinal() < colors.length ? slot.ordinal() : Slot.ACCENT.ordinal();
            return colors[i] & 0xFFFFFF;
        }

        public int argb(Slot slot) {
            return (slot.alpha << 24) | rgb(slot);
        }
    }

    public static final String STANDARD = "standard";
    private static final String CUSTOM_PREFIX = "custom:";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final String ARNE = "arne";
    private static final List<Theme> BUILT_IN = List.of(
        //                                   backdrop  panel     edge      headTop   headBot   sidebar   field     card      cardHov   accent    text      muted     ok        warn      danger    off       accent2
        builtIn(ARNE, "Arne Tweaks", 0x080B12, 0x0D1320, 0x1E2A40, 0x0D1320, 0x0A0F1A, 0x0A0F19, 0x080B12, 0x111A29, 0x182235, 0x35D6E8, 0xF0F3FA, 0x929BAD, 0x4ADE80, 0xFBBF24, 0xF87171, 0x606B7D, 0xA66AFF),
        builtIn(STANDARD, "Klassisch", 0x080A10, 0x121420, 0x2A2E45, 0x1D2036, 0x141727, 0x0E1019, 0x0B0D15, 0x1A1D2B, 0x242842, 0x8B5CF6, 0xEDEFF7, 0x8A90A8, 0x4ADE80, 0xFBBF24, 0xF87171, 0x3A3F58),
        builtIn("christmas", "Weihnachten", 0x05100A, 0x0F1F17, 0x2D5A3D, 0x9E1B1B, 0x5E1010, 0x0B1812, 0x08120D, 0x15291E, 0x1F3D2C, 0xF2C14E, 0xF5F7FA, 0x9DB5A6, 0x6EE7A0, 0xF2C14E, 0xFF5A5A, 0x34503F),
        builtIn("halloween", "Halloween", 0x0A0604, 0x140D08, 0x4A2A12, 0x3B1E4A, 0x1E0F26, 0x0E0906, 0x0A0604, 0x1F140C, 0x33200F, 0xFF7A1A, 0xFFEBD6, 0xA88B73, 0x9BE15D, 0xFFC23D, 0xFF4D4D, 0x4A3A2E),
        builtIn("easter", "Ostern", 0x403048, 0xFFF8EC, 0xE3C9E6, 0xFFC9DF, 0xD6F0E0, 0xF6EDF8, 0xFFFFFF, 0xF8EEF4, 0xEEDDF2, 0xB46CD9, 0x4A3A55, 0x8F7F99, 0x3FAE6E, 0xD9901E, 0xE0506A, 0xD5C8DC),
        builtIn("ocean", "Ozean", 0x020A12, 0x0A1724, 0x1C3E5A, 0x0F3554, 0x0A2238, 0x07131E, 0x050E17, 0x0F2233, 0x163450, 0x2DD4BF, 0xE6F6FF, 0x7FA3BD, 0x4ADE80, 0xFBBF24, 0xF87171, 0x2A4660),
        builtIn("light", "Hell", 0x202430, 0xF4F5F9, 0xD3D7E2, 0xFFFFFF, 0xE9ECF3, 0xECEEF4, 0xFFFFFF, 0xFFFFFF, 0xE8EAF3, 0x6D4AE0, 0x1E2130, 0x6B7086, 0x18A957, 0xC98A0B, 0xD93A3A, 0xC7CBD8),
        builtIn("glass", "Glas", 0x06060A, 0x14131C, 0x2C2A3D, 0x1A1926, 0x14131C, 0x1A1926, 0x100F17, 0x1F1D2D, 0x26233A, 0x8B5CF6, 0xE6E4F0, 0x77748F, 0x4ADE80, 0xFBBF24, 0xF87171, 0x3A3850),
        builtIn("compact", "Kompakt", 0x050508, 0x111018, 0x2A2838, 0x111018, 0x111018, 0x0C0B12, 0x0C0B12, 0x17161F, 0x22202E, 0x8B5CF6, 0xE6E4F0, 0x6D6A85, 0x4ADE80, 0xFBBF24, 0xF87171, 0x2A2838),
        builtIn("fire", "Feuer", 0x050303, 0x0D0A0A, 0x3A1D1D, 0x2A0E0E, 0x140A0A, 0x140C0C, 0x0A0707, 0x1A1212, 0x2A1616, 0xEF4444, 0xF5E6E6, 0xA37B7B, 0x4ADE80, 0xFBBF24, 0xFF6B6B, 0x3A2626),
        builtIn("mint", "Mint", 0x1C2A24, 0xF2FAF6, 0xCFE9DD, 0xFFFFFF, 0xE3F4EC, 0xE8F5EF, 0xFFFFFF, 0xFFFFFF, 0xE3F4EC, 0x10B981, 0x14352A, 0x5E8A79, 0x0E9F6E, 0xC98A0B, 0xD93A3A, 0xC5DDD2),
        builtIn("gold", "Gold", 0x050403, 0x0F0E0B, 0x3B3424, 0x1F1B12, 0x0F0E0B, 0x0C0B08, 0x0A0907, 0x1A1813, 0x26221A, 0xE8B44C, 0xF3EAD6, 0x9A8C6A, 0x9BE15D, 0xE8B44C, 0xF87171, 0x3B3424),
        builtIn("sakura", "Sakura", 0x0E0812, 0x1E1424, 0x4A3157, 0x3A1F48, 0x1E1424, 0x1A1020, 0x140D18, 0x2B1D33, 0x3A2745, 0xF472B6, 0xFBE7F3, 0xB48AA8, 0x86EFAC, 0xFCD34D, 0xFB7185, 0x4A3157),
        builtIn("minecraft", "Minecraft", 0x000000, 0x2B2B2B, 0x000000, 0x3C3C3C, 0x2B2B2B, 0x1E1E1E, 0x000000, 0x6B6B6B, 0x7E7E9E, 0xFFFF55, 0xFFFFFF, 0xAAAAAA, 0x55FF55, 0xFFAA00, 0xFF5555, 0x555555)
    );

    private static @Nullable List<Theme> customCache;
    private static @Nullable Theme activeCache;
    private static String activeCacheId = "";

    private GuiTheme() {
    }

    private static Theme builtIn(String id, String name, int... colors) {
        return new Theme(id, name, true, full(colors), id);
    }

    /** Fuellt fehlende Farben auf (aeltere Designs haben keine zweite Akzentfarbe: dann wie ACCENT). */
    private static int[] full(int[] colors) {
        int n = Slot.values().length;
        if (colors.length >= n) {
            return colors;
        }
        int[] out = java.util.Arrays.copyOf(colors, n);
        for (int i = colors.length; i < n; i++) {
            out[i] = colors[Slot.ACCENT.ordinal()];
        }
        return out;
    }

    /** Design per Id (fertig oder eigen), sonst das Standard-Design. */
    public static Theme find(String id) {
        return byId(id, BUILT_IN.get(0));
    }

    public static List<Theme> builtIns() {
        return BUILT_IN;
    }

    public static Path dir() {
        return FabricLoader.getInstance().getConfigDir().resolve("arnestyles").resolve("themes");
    }

    /** Eigene Designs aus dem Ordner (wird zwischengespeichert, {@link #reload()} liest neu). */
    public static List<Theme> custom() {
        if (customCache != null) {
            return customCache;
        }
        List<Theme> list = new ArrayList<>();
        Path dir = dir();
        if (Files.isDirectory(dir)) {
            try (var files = Files.list(dir)) {
                for (Path file : files.filter(f -> f.getFileName().toString().endsWith(".json")).sorted().toList()) {
                    Theme theme = read(file);
                    if (theme != null) {
                        list.add(theme);
                    }
                }
            } catch (IOException e) {
                ArneStyles.LOGGER.warn("Designs konnten nicht gelesen werden", e);
            }
        }
        customCache = list;
        return list;
    }

    public static void reload() {
        customCache = null;
        activeCache = null;
    }

    private static @Nullable Theme read(Path file) {
        String fileName = file.getFileName().toString();
        String id = CUSTOM_PREFIX + fileName.substring(0, fileName.length() - 5);
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);
            if (json == null) {
                return null;
            }
            String base = json.has("base") ? json.get("base").getAsString() : STANDARD;
            Theme fallback = byId(base, BUILT_IN.get(0));
            int[] colors = full(fallback.colors().clone());
            if (!colorJson(json).has(Slot.ACCENT2.key())) {
                // Aelteres eigenes Design: zweiter Akzent wie der erste
                colors[Slot.ACCENT2.ordinal()] = -1;
            }
            JsonObject colorJson = json.has("colors") ? json.getAsJsonObject("colors") : new JsonObject();
            for (Slot slot : Slot.values()) {
                if (colorJson.has(slot.key())) {
                    Integer rgb = parseHex(colorJson.get(slot.key()).getAsString());
                    if (rgb != null) {
                        colors[slot.ordinal()] = rgb;
                    }
                }
            }
            if (colors[Slot.ACCENT2.ordinal()] == -1) {
                colors[Slot.ACCENT2.ordinal()] = colors[Slot.ACCENT.ordinal()];
            }
            String name = json.has("name") ? json.get("name").getAsString() : fileName.substring(0, fileName.length() - 5);
            return new Theme(id, name, false, colors, base);
        } catch (Exception e) {
            ArneStyles.LOGGER.warn("Design {} ist kaputt: {}", fileName, e.getMessage());
            return null;
        }
    }

    private static JsonObject colorJson(JsonObject json) {
        return json.has("colors") && json.get("colors").isJsonObject() ? json.getAsJsonObject("colors") : new JsonObject();
    }

    private static @Nullable Integer parseHex(String text) {
        String hex = text.trim();
        if (hex.startsWith("#")) {
            hex = hex.substring(1);
        }
        try {
            return hex.length() == 6 ? Integer.parseInt(hex, 16) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Speichert ein eigenes Design in seine Datei. */
    public static void save(Theme theme) {
        if (theme.builtIn()) {
            return;
        }
        JsonObject json = new JsonObject();
        json.addProperty("name", theme.name());
        json.addProperty("base", theme.base());
        JsonObject colors = new JsonObject();
        for (Slot slot : Slot.values()) {
            colors.addProperty(slot.key(), String.format(Locale.ROOT, "#%06X", theme.rgb(slot)));
        }
        json.add("colors", colors);
        try {
            Files.createDirectories(dir());
            try (Writer writer = Files.newBufferedWriter(file(theme.id()), StandardCharsets.UTF_8)) {
                GSON.toJson(json, writer);
            }
        } catch (IOException e) {
            ArneStyles.LOGGER.warn("Design {} konnte nicht gespeichert werden", theme.name(), e);
        }
    }

    private static Path file(String id) {
        return dir().resolve(id.substring(CUSTOM_PREFIX.length()) + ".json");
    }

    /** Neues eigenes Design als Kopie von from. Wird gespeichert und aktiviert. */
    public static Theme createFrom(Theme from) {
        int n = 1;
        while (Files.exists(dir().resolve("design_" + n + ".json"))) {
            n++;
        }
        Theme theme = new Theme(CUSTOM_PREFIX + "design_" + n, "Mein Design " + n, false, from.colors().clone(), from.base());
        save(theme);
        reload();
        select(theme.id());
        return theme;
    }

    public static void delete(Theme theme) {
        if (theme.builtIn()) {
            return;
        }
        try {
            Files.deleteIfExists(file(theme.id()));
        } catch (IOException e) {
            ArneStyles.LOGGER.warn("Design {} konnte nicht geloescht werden", theme.name(), e);
        }
        if (theme.id().equals(ConfigManager.get().guiTheme)) {
            select(STANDARD);
        }
        reload();
    }

    /** Neuer Name fuer ein eigenes Design (die Datei behaelt ihren Namen). */
    public static Theme rename(Theme theme, String name) {
        Theme renamed = new Theme(theme.id(), name, false, theme.colors(), theme.base());
        save(renamed);
        reload();
        return renamed;
    }

    /** Aendert eine Farbe des eigenen Designs (nur im Speicher, {@link #save} schreibt sie). */
    public static void setColor(Theme theme, Slot slot, int rgb) {
        theme.colors()[slot.ordinal()] = rgb & 0xFFFFFF;
    }

    public static void select(String id) {
        ConfigManager.get().guiTheme = id;
        ConfigManager.save();
        activeCache = null;
    }

    private static Theme byId(String id, Theme fallback) {
        for (Theme theme : BUILT_IN) {
            if (theme.id().equals(id)) {
                return theme;
            }
        }
        if (id.startsWith(CUSTOM_PREFIX)) {
            for (Theme theme : custom()) {
                if (theme.id().equals(id)) {
                    return theme;
                }
            }
        }
        return fallback;
    }

    /** Das gewaehlte Design (Standard, wenn es nicht mehr existiert). */
    public static Theme active() {
        String id = ConfigManager.get().guiTheme;
        String season = ConfigManager.get().seasonalThemes ? seasonTheme(java.time.LocalDate.now()) : null;
        if (season != null) {
            id = season;
        }
        if (activeCache == null || !activeCacheId.equals(id)) {
            activeCache = byId(id == null ? STANDARD : id, BUILT_IN.get(0));
            activeCacheId = id == null ? STANDARD : id;
        }
        return activeCache;
    }

    /**
     * Design zur Jahreszeit oder null: 1. bis 26. Dezember Weihnachten, Karfreitag bis eine Woche nach Ostern Ostern,
     * 15. Oktober bis 1. November Halloween.
     */
    public static String seasonTheme(java.time.LocalDate date) {
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();
        if (month == 12 && day <= 26) {
            return "christmas";
        }
        if ((month == 10 && day >= 15) || (month == 11 && day == 1)) {
            return "halloween";
        }
        java.time.LocalDate easter = easterSunday(date.getYear());
        if (!date.isBefore(easter.minusDays(2)) && !date.isAfter(easter.plusDays(7))) {
            return "easter";
        }
        return null;
    }

    /** Ostersonntag (gregorianisch, Algorithmus nach Gauss / Meeus). */
    static java.time.LocalDate easterSunday(int year) {
        int a = year % 19;
        int b = year / 100;
        int c = year % 100;
        int d = b / 4;
        int e = b % 4;
        int f = (b + 8) / 25;
        int g = (b - f + 1) / 3;
        int h = (19 * a + b - d - g + 15) % 30;
        int i = c / 4;
        int k = c % 4;
        int l = (32 + 2 * e + 2 * i - h - k) % 7;
        int m = (a + 11 * h + 22 * l) / 451;
        int month = (h + l - 7 * m + 114) / 31;
        int day = (h + l - 7 * m + 114) % 31 + 1;
        return java.time.LocalDate.of(year, month, day);
    }

    /** Fertiges Design, aus dem theme entstanden ist. */
    public static Theme baseOf(Theme theme) {
        return byId(theme.base(), BUILT_IN.get(0));
    }

    /** Mischt zwei Farben (t = 0 nur a, t = 1 nur b), Alpha von a. */
    public static int mix(int a, int b, float t) {
        int r = Math.round(((a >> 16) & 0xFF) * (1 - t) + ((b >> 16) & 0xFF) * t);
        int g = Math.round(((a >> 8) & 0xFF) * (1 - t) + ((b >> 8) & 0xFF) * t);
        int bl = Math.round((a & 0xFF) * (1 - t) + (b & 0xFF) * t);
        return (a & 0xFF000000) | (r << 16) | (g << 8) | bl;
    }

    /** RGB in Farbton, Saettigung, Helligkeit (je 0..1). */
    public static float[] toHsv(int rgb) {
        float r = ((rgb >> 16) & 0xFF) / 255.0f;
        float g = ((rgb >> 8) & 0xFF) / 255.0f;
        float b = (rgb & 0xFF) / 255.0f;
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float delta = max - min;
        float hue = 0.0f;
        if (delta > 0.0f) {
            if (max == r) {
                hue = ((g - b) / delta) % 6.0f;
            } else if (max == g) {
                hue = (b - r) / delta + 2.0f;
            } else {
                hue = (r - g) / delta + 4.0f;
            }
            hue /= 6.0f;
            if (hue < 0.0f) {
                hue += 1.0f;
            }
        }
        return new float[] {hue, max == 0.0f ? 0.0f : delta / max, max};
    }
}
