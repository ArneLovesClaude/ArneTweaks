package com.arnestyles.client.config;

import com.arnestyles.ArneStyles;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Keybind-Sets: die ganze Tastenbelegung (Minecraft und alle Mods) unter einem Namen speichern, laden und als Code teilen.
 * Gespeichert wird nur, was vom Standard abweicht. Beim Laden geht alles andere auf Standard.
 */
public final class KeybindSets {
    public static final String CODE_PREFIX = "ARNEKEYS1:";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** Inhalt einer Set-Datei. */
    private static final class Snapshot {
        String name;
        Map<String, String> keys = new LinkedHashMap<>();
    }

    public record KeySet(String name, Path file) {
    }

    private KeybindSets() {
    }

    public static Path dir() {
        return FabricLoader.getInstance().getConfigDir().resolve("arnestyles").resolve("keybinds");
    }

    public static List<KeySet> list() {
        List<KeySet> sets = new ArrayList<>();
        if (!Files.isDirectory(dir())) {
            return sets;
        }
        try (var files = Files.list(dir())) {
            for (Path file : files.filter(f -> f.getFileName().toString().endsWith(".json")).sorted().toList()) {
                String name = file.getFileName().toString();
                sets.add(new KeySet(name.substring(0, name.length() - 5), file));
            }
        } catch (IOException e) {
            ArneStyles.LOGGER.warn("Keybind-Sets konnten nicht gelesen werden.", e);
        }
        return sets;
    }

    private static Snapshot capture(String name) {
        Snapshot snapshot = new Snapshot();
        snapshot.name = name;
        for (KeyBinding binding : MinecraftClient.getInstance().options.allKeys) {
            if (!binding.isDefault()) {
                snapshot.keys.put(binding.getId(), binding.getBoundKeyTranslationKey());
            }
        }
        return snapshot;
    }

    /** Aktuelle Tastenbelegung unter diesem Namen speichern (ueberschreibt ein Set mit gleichem Namen). */
    public static boolean save(String rawName) {
        String name = PresetManager.cleanName(rawName);
        if (name.isEmpty()) {
            return false;
        }
        return write(capture(name), dir().resolve(name + ".json"));
    }

    private static boolean write(Snapshot snapshot, Path file) {
        try {
            Files.createDirectories(dir());
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(snapshot, writer);
            }
            return true;
        } catch (IOException e) {
            ArneStyles.LOGGER.warn("Keybind-Set {} konnte nicht gespeichert werden.", snapshot.name, e);
            return false;
        }
    }

    private static @Nullable Snapshot read(Path file) {
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            Snapshot snapshot = GSON.fromJson(reader, Snapshot.class);
            if (snapshot != null && snapshot.keys == null) {
                snapshot.keys = new LinkedHashMap<>();
            }
            return snapshot;
        } catch (IOException | RuntimeException e) {
            ArneStyles.LOGGER.warn("Keybind-Set {} ist kaputt: {}", file.getFileName(), e.getMessage());
            return null;
        }
    }

    /** Set laden: alles auf Standard, dann die gespeicherten Tasten. Tasten von Mods, die fehlen, werden uebersprungen. */
    public static boolean load(KeySet set) {
        Snapshot snapshot = read(set.file());
        if (snapshot == null) {
            return false;
        }
        apply(snapshot.keys);
        return true;
    }

    /** Alle Tasten auf Minecraft-Standard. */
    public static void resetAll() {
        apply(Map.of());
    }

    private static void apply(Map<String, String> keys) {
        MinecraftClient client = MinecraftClient.getInstance();
        for (KeyBinding binding : client.options.allKeys) {
            String wanted = keys.get(binding.getId());
            InputUtil.Key key = binding.getDefaultKey();
            if (wanted != null) {
                try {
                    key = InputUtil.fromTranslationKey(wanted);
                } catch (RuntimeException e) {
                    ArneStyles.LOGGER.warn("Unbekannte Taste {} fuer {}", wanted, binding.getId());
                }
            }
            binding.setBoundKey(key);
        }
        KeyBinding.updateKeysByCode();
        client.options.write();
    }

    /** Wie viele Tasten im Set vom Standard abweichen. */
    public static int changedCount(KeySet set) {
        Snapshot snapshot = read(set.file());
        return snapshot == null ? 0 : snapshot.keys.size();
    }

    public static void delete(KeySet set) {
        try {
            Files.deleteIfExists(set.file());
        } catch (IOException e) {
            ArneStyles.LOGGER.warn("Keybind-Set {} konnte nicht geloescht werden.", set.name(), e);
        }
    }

    /** Set als kurzer Code (gzip + Base64) zum Teilen. */
    public static @Nullable String exportCode(KeySet set) {
        try {
            byte[] json = Files.readAllBytes(set.file());
            java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
            try (java.util.zip.GZIPOutputStream gzip = new java.util.zip.GZIPOutputStream(bytes)) {
                gzip.write(json);
            }
            return CODE_PREFIX + java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes.toByteArray());
        } catch (IOException e) {
            ArneStyles.LOGGER.warn("Keybind-Set {} konnte nicht als Code exportiert werden.", set.name(), e);
            return null;
        }
    }

    /** Code als neues Set speichern. Gibt den Namen zurueck oder null bei ungueltigem Code. */
    public static @Nullable String importCode(@Nullable String raw) {
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
            if (snapshot == null || snapshot.keys == null) {
                return null;
            }
            String base = PresetManager.cleanName(snapshot.name == null ? "" : snapshot.name);
            if (base.isEmpty()) {
                base = "Import";
            }
            String name = base;
            for (int n = 2; Files.exists(dir().resolve(name + ".json")); n++) {
                String suffix = " " + n;
                name = (base.length() + suffix.length() > PresetManager.MAX_NAME_LENGTH
                    ? base.substring(0, PresetManager.MAX_NAME_LENGTH - suffix.length()) : base) + suffix;
            }
            snapshot.name = name;
            return write(snapshot, dir().resolve(name + ".json")) ? name : null;
        } catch (IOException | RuntimeException e) {
            ArneStyles.LOGGER.warn("Keybind-Code ist ungueltig: {}", e.getMessage());
            return null;
        }
    }
}
