package com.arnestyles.client.config;

import com.arnestyles.ArneStyles;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Laedt, speichert und setzt die lokale Konfiguration zurueck.
 * Alles passiert lokal in config/arnestyles.json, es gibt keine Netzwerkzugriffe.
 */
public final class ConfigManager {
    private static final String FILE_NAME = "arnestyles.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static ArneStylesConfig config = createDefaults();

    private ConfigManager() {
    }

    public static ArneStylesConfig get() {
        return config;
    }

    private static Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
    }

    private static ArneStylesConfig createDefaults() {
        ArneStylesConfig defaults = new ArneStylesConfig();
        defaults.sanitize();
        return defaults;
    }

    /** Laedt die Datei. Fehlt sie, wird sie mit Standardwerten angelegt. Ist sie beschaedigt, wird sie gesichert und neu erzeugt. */
    public static synchronized void load() {
        Path path = configPath();

        if (!Files.exists(path)) {
            config = createDefaults();
            save();
            return;
        }

        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            ArneStylesConfig loaded = GSON.fromJson(reader, ArneStylesConfig.class);
            if (loaded == null) {
                throw new JsonParseException("Konfigurationsdatei ist leer");
            }
            loaded.sanitize();
            config = loaded;
            // Speichert ergaenzte Eintraege und Umstellungen aus aelteren Versionen sofort.
            save();
        } catch (IOException | RuntimeException e) {
            ArneStyles.LOGGER.error("Konfiguration konnte nicht gelesen werden, Standardwerte werden verwendet.", e);
            backupBrokenFile(path);
            config = createDefaults();
            save();
        }
    }

    /** Schreibt die Konfiguration zuerst in eine Temp-Datei und ersetzt dann die echte Datei. */
    public static synchronized void save() {
        Path path = configPath();
        Path temp = path.resolveSibling(FILE_NAME + ".tmp");

        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
                GSON.toJson(config, writer);
            }
            try {
                Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                // Windows verweigert das Ersetzen manchmal (z. B. Virenscanner oder andere Dateirechte): dann normal kopieren.
                Files.copy(temp, path, StandardCopyOption.REPLACE_EXISTING);
                Files.deleteIfExists(temp);
            }
        } catch (IOException e) {
            ArneStyles.LOGGER.error("Konfiguration konnte nicht gespeichert werden.", e);
        }
    }

    /** Ersetzt die ganze Config (z. B. beim Laden eines Presets mit allen Einstellungen) und speichert sofort. */
    public static synchronized void replace(ArneStylesConfig next) {
        config = next;
        save();
    }

    /** Setzt alles auf Standardwerte zurueck und speichert sofort. */
    public static synchronized void resetToDefaults() {
        config = createDefaults();
        save();
    }

    private static void backupBrokenFile(Path path) {
        Path backup = path.resolveSibling(FILE_NAME + ".broken-" + System.currentTimeMillis());
        try {
            Files.move(path, backup, StandardCopyOption.REPLACE_EXISTING);
            ArneStyles.LOGGER.warn("Beschaedigte Konfiguration gesichert als {}", backup.getFileName());
        } catch (IOException e) {
            ArneStyles.LOGGER.error("Beschaedigte Konfiguration konnte nicht gesichert werden.", e);
        }
    }
}