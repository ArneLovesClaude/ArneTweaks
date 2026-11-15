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
    public static final int MAX_NAME_LENGTH = 24;

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

    /** Laedt ein Preset in die aktuelle Config. Der Hauptschalter und die offene Kategorie bleiben, wie sie sind. */
    public static boolean load(Preset preset) {
        try (Reader reader = Files.newBufferedReader(preset.file(), StandardCharsets.UTF_8)) {
            Snapshot snapshot = GSON.fromJson(reader, Snapshot.class);
            if (snapshot == null) {
                return false;
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
            byte[] json = Files.readAllBytes(preset.file());
            java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
            try (java.util.zip.GZIPOutputStream gzip = new java.util.zip.GZIPOutputStream(bytes)) {
                gzip.write(json);
            }
            return CODE_PREFIX + java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes.toByteArray());
        } catch (IOException e) {
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
