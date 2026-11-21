package com.arnestyles.client.config;

import com.arnestyles.ArneStyles;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Presets: komplette Einstellungen (Styles, aktive Kategorien, Essentials, Regler) unter einem Namen
 * speichern und wieder laden. Jedes Preset ist eine Datei in config/arnestyles/presets/<name>.json
 * und kann so auch an andere weitergegeben werden.
 */
public final class PresetManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Gson GSON_COMPACT = new Gson();
    public static final int MAX_NAME_LENGTH = 24;

    /**
     * Felder, die nicht mit einem Preset/Code mitgehen: persoenliche Sachen (Wegpunkte, Server-Zuordnungen),
     * Menue-Zustand und Felder, die das Preset sowieso einzeln speichert.
     */
    private static final java.util.Set<String> NOT_SHARED = java.util.Set.of(
        "configVersion", "modEnabled", "expandedGroups", "activeTab", "activeCategory", "sidebarOrder",
        "waypoints", "serverPresets", "lastPreset", "customHudSetupDone", "introShown", "guiDesignV2",
        "selectedStyles", "enabledCategories", "essentials", "lowFireOffset", "totemPopScale", "totemParticles", "heldItemScale");

    /** Inhalt einer Preset-Datei. */
    private static final class Snapshot {
        String name;
        Map<String, String> selectedStyles = new LinkedHashMap<>();
        Map<String, Boolean> enabledCategories = new LinkedHashMap<>();
        Map<String, Boolean> essentials = new LinkedHashMap<>();
        float lowFireOffset = 0.25f;
        float totemPopScale = 1.0f;
        float totemParticles = 1.0f;
        float heldItemScale = 1.0f;
        /** Alle anderen Einstellungen (Mods, Farben, HUD-Positionen ...), nur was vom Standard abweicht. Fehlt bei alten Presets. */
        com.google.gson.JsonObject settings;
    }

    /** Einstellungen, die vom Standard abweichen (ohne persoenliche Felder). */
    private static com.google.gson.JsonObject settingsDiff(ArneStylesConfig config) {
        ArneStylesConfig defaults = new ArneStylesConfig();
        defaults.sanitize();
        com.google.gson.JsonObject current = GSON_COMPACT.toJsonTree(config).getAsJsonObject();
        com.google.gson.JsonObject standard = GSON_COMPACT.toJsonTree(defaults).getAsJsonObject();
        com.google.gson.JsonObject diff = new com.google.gson.JsonObject();
        for (Map.Entry<String, com.google.gson.JsonElement> entry : current.entrySet()) {
            if (!NOT_SHARED.contains(entry.getKey()) && !entry.getValue().equals(standard.get(entry.getKey()))) {
                diff.add(entry.getKey(), entry.getValue());
            }
        }
        return diff;
    }

    /** Neue Config: Standardwerte + Einstellungen aus dem Preset, persoenliche Felder bleiben wie bisher. */
    private static ArneStylesConfig withSettings(ArneStylesConfig current, com.google.gson.JsonObject settings) {
        ArneStylesConfig defaults = new ArneStylesConfig();
        defaults.sanitize();
        com.google.gson.JsonObject merged = GSON_COMPACT.toJsonTree(defaults).getAsJsonObject();
        for (Map.Entry<String, com.google.gson.JsonElement> entry : settings.entrySet()) {
            if (!NOT_SHARED.contains(entry.getKey()) && merged.has(entry.getKey())) {
                merged.add(entry.getKey(), entry.getValue());
            }
        }
        com.google.gson.JsonObject mine = GSON_COMPACT.toJsonTree(current).getAsJsonObject();
        for (String key : NOT_SHARED) {
            if (mine.has(key)) {
                merged.add(key, mine.get(key));
            }
        }
        ArneStylesConfig result = GSON_COMPACT.fromJson(merged, ArneStylesConfig.class);
        result.sanitize();
        return result;
    }

    private static Snapshot snapshot(String name) {
        ArneStylesConfig config = ConfigManager.get();
        Snapshot snapshot = new Snapshot();
        snapshot.name = name;
        snapshot.selectedStyles.putAll(config.selectedStyles);
        snapshot.enabledCategories.putAll(config.enabledCategories);
        snapshot.essentials.putAll(config.essentials);
        snapshot.lowFireOffset = config.lowFireOffset;
        snapshot.totemPopScale = config.totemPopScale;
        snapshot.totemParticles = config.totemParticles;
        snapshot.heldItemScale = config.heldItemScale;
        snapshot.settings = settingsDiff(config);
        return snapshot;
    }

    private static String pack(String json) throws IOException {
        java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
        try (java.util.zip.GZIPOutputStream gzip = new java.util.zip.GZIPOutputStream(bytes)) {
            gzip.write(json.getBytes(StandardCharsets.UTF_8));
        }
        return CODE_PREFIX + java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes.toByteArray());
    }

    /** Die aktuellen Einstellungen direkt als Code (ohne vorher ein Preset zu speichern). */
    public static @org.jspecify.annotations.Nullable String currentCode(String rawName) {
        String name = cleanName(rawName);
        try {
            return pack(GSON_COMPACT.toJson(snapshot(name.isEmpty() ? "Import" : name)));
        } catch (IOException | RuntimeException e) {
            ArneStyles.LOGGER.warn("Einstellungen konnten nicht als Code exportiert werden.", e);
            return null;
        }
    }

    public record Preset(String name, Path file) {
    }

    private PresetManager() {
    }

    public static Path dir() {
        return FabricLoader.getInstance().getConfigDir().resolve("arnestyles").resolve("presets");
    }

    /** Erlaubte Zeichen fuer Namen (auch Umlaute), maximal 24 Zeichen. */
    public static String cleanName(String raw) {
        String cleaned = raw.replaceAll("[^A-Za-z0-9 _\\-äöüÄÖÜß]", "").trim();
        return cleaned.length() > MAX_NAME_LENGTH ? cleaned.substring(0, MAX_NAME_LENGTH) : cleaned;
    }

    public static List<Preset> list() {
        List<Preset> result = new ArrayList<>();
        Path dir = dir();
        if (!Files.isDirectory(dir)) {
            return result;
        }
        try (Stream<Path> files = Files.list(dir)) {
            files.filter(p -> p.getFileName().toString().endsWith(".json"))
                .sorted(Comparator.comparing(p -> p.getFileName().toString().toLowerCase(Locale.ROOT)))
                .forEach(p -> {
                    String file = p.getFileName().toString();
                    result.add(new Preset(file.substring(0, file.length() - 5), p));
                });
        } catch (IOException e) {
            ArneStyles.LOGGER.warn("Presets konnten nicht gelesen werden.", e);
        }
        return result;
    }

    /** Speichert die aktuellen Einstellungen. Ein Preset mit gleichem Namen wird ueberschrieben. */
    public static boolean save(String rawName) {
        String name = cleanName(rawName);
        if (name.isEmpty()) {
            return false;
        }
        Snapshot snapshot = snapshot(name);
        try {
            Files.createDirectories(dir());
            try (Writer writer = Files.newBufferedWriter(dir().resolve(name + ".json"), StandardCharsets.UTF_8)) {
                GSON.toJson(snapshot, writer);
            }
            return true;
        } catch (IOException e) {
            ArneStyles.LOGGER.error("Preset {} konnte nicht gespeichert werden.", name, e);
            return false;
        }
    }

    /** Benennt ein Preset um. false, wenn der Name leer ist oder schon vergeben. */
    public static boolean rename(Preset preset, String rawName) {
        String name = cleanName(rawName);
        if (name.isEmpty() || name.equals(preset.name())) {
            return false;
        }
        Path target = dir().resolve(name + ".json");
        if (Files.exists(target)) {
            return false;
        }
        try {
            com.google.gson.JsonObject json;
            try (java.io.Reader reader = Files.newBufferedReader(preset.file(), StandardCharsets.UTF_8)) {
                json = GSON.fromJson(reader, com.google.gson.JsonObject.class);
            }
            if (json == null) {
                return false;
            }
            json.addProperty("name", name);
            try (Writer writer = Files.newBufferedWriter(target, StandardCharsets.UTF_8)) {
                GSON.toJson(json, writer);
            }
            Files.deleteIfExists(preset.file());
            return true;
        } catch (IOException | RuntimeException e) {
            ArneStyles.LOGGER.warn("Preset {} konnte nicht umbenannt werden.", preset.name(), e);
            return false;
        }
    }

    /** Laedt ein Preset in die aktuelle Config. Der Hauptschalter und die offene Kategorie bleiben, wie sie sind. */
    public static boolean load(Preset preset) {
        try (Reader reader = Files.newBufferedReader(preset.file(), StandardCharsets.UTF_8)) {
            Snapshot snapshot = GSON.fromJson(reader, Snapshot.class);
            if (snapshot == null) {
                return false;
            }
            if (snapshot.settings != null) {
                // Neueres Preset: alle Einstellungen uebernehmen (Rest auf Standard)
                ConfigManager.replace(withSettings(ConfigManager.get(), snapshot.settings));
            }
            ArneStylesConfig config = ConfigManager.get();
            config.selectedStyles = new LinkedHashMap<>(snapshot.selectedStyles != null ? snapshot.selectedStyles : Map.of());
            config.enabledCategories = new LinkedHashMap<>(snapshot.enabledCategories != null ? snapshot.enabledCategories : Map.of());
            config.essentials = new LinkedHashMap<>(snapshot.essentials != null ? snapshot.essentials : Map.of());
            config.lowFireOffset = snapshot.lowFireOffset;
            config.totemPopScale = snapshot.totemPopScale;
            config.totemParticles = snapshot.totemParticles;
            config.heldItemScale = snapshot.heldItemScale;
            config.sanitize();
            com.arnestyles.client.mods.CustomCrosshair.invalidate();
            ConfigManager.save();
            return true;
        } catch (IOException | RuntimeException e) {
            ArneStyles.LOGGER.error("Preset {} konnte nicht geladen werden.", preset.name(), e);
            return false;
        }
    }

    /** Vorsilbe eines Preset-Codes (Version 1). */
    public static final String CODE_PREFIX = "ARNE1:";

    /** Preset als kurzer Text zum Teilen: "ARNE1:" + Base64 der gepackten Preset-Datei. */
    public static @org.jspecify.annotations.Nullable String exportCode(Preset preset) {
        try {
            String json = Files.readString(preset.file(), StandardCharsets.UTF_8);
            // Kompakt (ohne Einrueckung), damit der Code kuerzer wird
            return pack(GSON_COMPACT.toJson(com.google.gson.JsonParser.parseString(json)));
        } catch (IOException | RuntimeException e) {
            ArneStyles.LOGGER.warn("Preset {} konnte nicht als Code exportiert werden.", preset.name(), e);
            return null;
        }
    }

    /** Preset aus einem Code speichern. Gibt den Namen zurueck oder null, wenn der Code ungueltig ist. */
    public static @org.jspecify.annotations.Nullable String importCode(String raw) {
        String code = raw == null ? "" : raw.trim();
        if (!code.startsWith(CODE_PREFIX)) {
            return null;
        }
        try {
            byte[] packed = java.util.Base64.getUrlDecoder().decode(code.substring(CODE_PREFIX.length()));
            byte[] json;
            try (java.util.zip.GZIPInputStream gzip = new java.util.zip.GZIPInputStream(new java.io.ByteArrayInputStream(packed))) {
                json = gzip.readNBytes(1 << 20);
            }
            Snapshot snapshot = GSON.fromJson(new String(json, StandardCharsets.UTF_8), Snapshot.class);
            if (snapshot == null) {
                return null;
            }
            String base = cleanName(snapshot.name == null ? "" : snapshot.name);
            if (base.isEmpty()) {
                base = "Import";
            }
            String name = base;
            for (int n = 2; Files.exists(dir().resolve(name + ".json")); n++) {
                String suffix = " " + n;
                name = (base.length() + suffix.length() > MAX_NAME_LENGTH ? base.substring(0, MAX_NAME_LENGTH - suffix.length()) : base) + suffix;
            }
            snapshot.name = name;
            Files.createDirectories(dir());
            try (Writer writer = Files.newBufferedWriter(dir().resolve(name + ".json"), StandardCharsets.UTF_8)) {
                GSON.toJson(snapshot, writer);
            }
            return name;
        } catch (IOException | RuntimeException e) {
            ArneStyles.LOGGER.warn("Preset-Code ist ungueltig: {}", e.getMessage());
            return null;
        }
    }

    public static void delete(Preset preset) {
        try {
            Files.deleteIfExists(preset.file());
        } catch (IOException e) {
            ArneStyles.LOGGER.warn("Preset {} konnte nicht geloescht werden.", preset.name(), e);
        }
    }
}
