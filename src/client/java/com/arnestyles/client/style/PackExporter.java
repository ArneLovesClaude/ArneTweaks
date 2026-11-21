package com.arnestyles.client.style;

import com.arnestyles.ArneStyles;
import com.arnestyles.client.config.ArneStylesConfig;
import com.arnestyles.client.config.ConfigManager;
import com.arnestyles.client.config.StyleCategory;
import com.arnestyles.client.essentials.Essential;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Exportiert das aktuelle Setup (alle aktiven Styles, Essentials-Packs und eigene Farben) als ein normales
 * Texture-Pack (.zip). Das kann man ohne ArneTweaks benutzen oder an Freunde schicken.
 * Reihenfolge wie im Spiel: spaetere Quellen ueberschreiben fruehere Dateien.
 */
public final class PackExporter {
    private static final Set<String> ALLOWED = Set.of("png", "json", "mcmeta", "properties", "ogg", "txt", "jem", "jpm");

    private PackExporter() {
    }

    /** Schreibt das Pack und gibt die Datei zurueck. Laeuft im Hintergrund (Dateien kopieren). */
    public static Path export(Path resourcePackDir) throws IOException {
        ArneStylesConfig config = ConfigManager.get();
        List<Path> roots = new ArrayList<>();
        List<String> names = new ArrayList<>();
        for (StyleCategory category : StyleCategory.values()) {
            if (category == StyleCategory.ESSENTIALS || !config.isEnabled(category)) {
                continue;
            }
            StyleDefinition style = StyleRegistry.getSelected(category);
            if (style.isVanilla() || style.root() == null || !Files.isDirectory(style.root().resolve("assets")) || !CloudStyles.isReady(style)) {
                continue;
            }
            roots.add(style.root());
            names.add(style.name().getString());
        }
        if (config.isEnabled(StyleCategory.ESSENTIALS)) {
            for (Essential essential : Essential.values()) {
                if (essential.kind() == Essential.Kind.PACK && config.isEssentialOn(essential.id())) {
                    StyleRegistry.essentialRoot(essential).ifPresent(roots::add);
                }
            }
        }
        Path tints = TintedTextures.generatedRoot();
        if (Files.isDirectory(tints.resolve("assets"))) {
            roots.add(tints);
        }

        // Pfad im Pack -> Quelldatei (spaetere gewinnen)
        Map<String, Path> files = new LinkedHashMap<>();
        for (Path root : roots) {
            Path assets = root.resolve("assets");
            if (!Files.isDirectory(assets)) {
                continue;
            }
            try (Stream<Path> walk = Files.walk(assets)) {
                for (Path p : (Iterable<Path>) walk::iterator) {
                    if (!Files.isRegularFile(p)) {
                        continue;
                    }
                    String rel = root.relativize(p).toString().replace('\\', '/');
                    int dot = rel.lastIndexOf('.');
                    if (dot < 0 || !ALLOWED.contains(rel.substring(dot + 1).toLowerCase(Locale.ROOT)) || rel.contains("..")) {
                        continue;
                    }
                    files.put(rel, p);
                }
            }
        }
        if (files.isEmpty()) {
            throw new IOException("Kein aktiver Style zum Exportieren");
        }

        Files.createDirectories(resourcePackDir);
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm"));
        Path out = resourcePackDir.resolve("ArneTweaks-Export_" + stamp + ".zip");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(out))) {
            JsonObject inner = new JsonObject();
            inner.addProperty("description", "§dArneTweaks Export §7" + stamp.replace('_', ' '));
            JsonArray format = new JsonArray();
            format.add(75);
            format.add(0);
            inner.add("min_format", format);
            inner.add("max_format", format.deepCopy());
            JsonObject pack = new JsonObject();
            pack.add("pack", inner);
            put(zip, "pack.mcmeta", pack.toString().getBytes(StandardCharsets.UTF_8));
            try (InputStream icon = PackExporter.class.getResourceAsStream("/assets/arnestyles/icon.png")) {
                if (icon != null) {
                    put(zip, "pack.png", icon.readAllBytes());
                }
            }
            String readme = "Exportiert mit ArneTweaks am " + stamp.replace('_', ' ') + "\n\nEnthaltene Styles:\n- "
                + String.join("\n- ", names) + "\n\nDie einzelnen Styles gehoeren ihren Erstellern.\n";
            put(zip, "ArneTweaks-Export.txt", readme.getBytes(StandardCharsets.UTF_8));
            for (Map.Entry<String, Path> entry : files.entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey()));
                Files.copy(entry.getValue(), zip);
                zip.closeEntry();
            }
        }
        ArneStyles.LOGGER.info("Pack exportiert: {} ({} Dateien aus {} Quellen)", out, files.size(), roots.size());
        return out;
    }

    private static void put(ZipOutputStream zip, String name, byte[] data) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(data);
        zip.closeEntry();
    }
}
