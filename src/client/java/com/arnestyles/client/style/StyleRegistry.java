package com.arnestyles.client.style;

import com.arnestyles.ArneStyles;
import com.arnestyles.client.config.ArneStylesConfig;
import com.arnestyles.client.config.ConfigManager;
import com.arnestyles.client.config.StyleCategory;
import com.arnestyles.client.essentials.Essential;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resource.DirectoryResourcePack;
import net.minecraft.resource.ResourcePack;
import net.minecraft.resource.ResourcePackInfo;
import net.minecraft.resource.ResourcePackSource;
import net.minecraft.text.Text;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Liest die Style-Ordner aus config/arnestyles/styles/<kategorie>/<style>/ ein.
 * Jeder Style-Ordner ist wie ein kleines Texture-Pack aufgebaut (assets/minecraft/...).
 * Die aktiven Styles werden beim Laden der Ressourcen als zusaetzliche Packs ganz oben eingehaengt.
 */
public final class StyleRegistry {
    private static final Gson GSON = new Gson();
    private static final String README_NAME = "LIESMICH.txt";
    private static final Map<StyleCategory, List<StyleDefinition>> STYLES = new EnumMap<>(StyleCategory.class);

    private StyleRegistry() {
    }

    public static Path stylesRoot() {
        return FabricLoader.getInstance().getConfigDir().resolve("arnestyles").resolve("styles");
    }

    public static Path categoryDir(StyleCategory category) {
        return stylesRoot().resolve(category.folderName());
    }

    /**
     * Liest alle Styles neu ein: zuerst die eingebauten (aus GitHub geladen, siehe {@link CloudStyles},
     * liegen in config/arnestyles/cloud/<kategorie>/<id>), dann die eigenen aus config/arnestyles/styles.
     * Ein eigener Ordner mit gleichem Namen ersetzt den eingebauten.
     */
    public static synchronized void reload() {
        STYLES.clear();
        Optional<Path> builtInRoot = Optional.of(CloudStyles.root()).filter(Files::isDirectory);
        int builtInCount = 0;
        int customCount = 0;
        for (StyleCategory category : StyleCategory.values()) {
            if (category.previewKind() == StyleCategory.PreviewKind.SETTINGS) {
                // Reine Einstellungs-Seite: keine Styles und kein Ordner.
                STYLES.put(category, List.of(StyleDefinition.vanilla(category)));
                continue;
            }
            Map<String, StyleDefinition> byId = new java.util.TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            if (builtInRoot.isPresent()) {
                for (StyleDefinition style : scan(category, builtInRoot.get().resolve(category.folderName()), true)) {
                    byId.put(style.id(), style);
                    builtInCount++;
                }
            }
            if (category == StyleCategory.PERFORMANCE) {
                // Performance-Packs sind klein und liegen direkt in der Mod-Jar (kein Download noetig).
                Optional<Path> jarPacks = modPath(PERFORMANCE_FOLDER);
                if (jarPacks.isPresent()) {
                    for (StyleDefinition style : scan(category, jarPacks.get(), true)) {
                        byId.put(style.id(), style);
                        builtInCount++;
                    }
                }
            }
            Path dir = categoryDir(category);
            try {
                Files.createDirectories(dir);
            } catch (IOException e) {
                ArneStyles.LOGGER.error("Style-Ordner {} konnte nicht angelegt werden.", dir, e);
            }
            for (StyleDefinition style : scan(category, dir, false)) {
                byId.put(style.id(), style);
                customCount++;
            }
            List<StyleDefinition> list = new ArrayList<>();
            list.add(StyleDefinition.vanilla(category));
            list.addAll(byId.values());
            STYLES.put(category, List.copyOf(list));
        }
        writeReadmeIfMissing();
        ArneStyles.LOGGER.info("Arne Styles: {} eingebaute und {} eigene Styles gefunden.", builtInCount, customCount);
    }

    private static List<StyleDefinition> scan(StyleCategory category, Path dir, boolean builtIn) {
        List<StyleDefinition> result = new ArrayList<>();
        if (!Files.isDirectory(dir)) {
            return result;
        }
        try (Stream<Path> children = Files.list(dir)) {
            children.filter(Files::isDirectory)
                .sorted(Comparator.comparing(path -> fileName(path).toLowerCase(Locale.ROOT)))
                .forEach(path -> readStyle(category, path, builtIn).ifPresent(result::add));
        } catch (IOException e) {
            ArneStyles.LOGGER.error("Style-Ordner {} konnte nicht gelesen werden.", dir, e);
        }
        return result;
    }

    /** Ordnername ohne abschliessenden Schraegstrich (in der Jar-Datei enden Ordner auf "/"). */
    private static String fileName(Path path) {
        String name = path.getFileName().toString();
        return name.endsWith("/") ? name.substring(0, name.length() - 1) : name;
    }

    private static Optional<StyleDefinition> readStyle(StyleCategory category, Path dir, boolean builtIn) {
        String id = fileName(dir);
        if (id.equalsIgnoreCase(ArneStylesConfig.DEFAULT_STYLE)) {
            ArneStyles.LOGGER.warn("Style-Ordner {} heisst wie der Standard-Style und wird ignoriert.", dir);
            return Optional.empty();
        }
        // Eingebaute grosse Styles (Himmel) haben vor ihrem eigenen Download nur die Vorschau.
        if (!builtIn && !Files.isDirectory(dir.resolve("assets"))) {
            ArneStyles.LOGGER.warn("Style-Ordner {} hat keinen Unterordner 'assets' und wird ignoriert.", dir);
            return Optional.empty();
        }

        String name = id;
        List<String> aliases = new ArrayList<>();
        Path json = dir.resolve("style.json");
        if (Files.isRegularFile(json)) {
            try (Reader reader = Files.newBufferedReader(json, StandardCharsets.UTF_8)) {
                JsonObject object = GSON.fromJson(reader, JsonObject.class);
                JsonElement value = object == null ? null : object.get("name");
                if (value != null && value.isJsonPrimitive() && !value.getAsString().isBlank()) {
                    name = value.getAsString();
                }
                // Zweitnamen von zusammengelegten Styles (gleiche Dateien in mehreren Packs).
                JsonElement alias = object == null ? null : object.get("aliases");
                if (alias != null && alias.isJsonArray()) {
                    alias.getAsJsonArray().forEach(a -> aliases.add(a.getAsString()));
                }
            } catch (IOException | RuntimeException e) {
                ArneStyles.LOGGER.warn("style.json in {} ist ungueltig, der Ordnername wird verwendet.", dir, e);
            }
        }

        Path preview = dir.resolve("preview.png");
        return Optional.of(new StyleDefinition(id, category, Text.literal(name), dir, Files.isRegularFile(preview) ? preview : null, builtIn,
            List.copyOf(aliases)));
    }

    public static synchronized List<StyleDefinition> getStyles(StyleCategory category) {
        List<StyleDefinition> styles = STYLES.get(category);
        return styles != null ? styles : List.of(StyleDefinition.vanilla(category));
    }

    /** Der in der Config gewaehlte Style. Existiert der Ordner nicht mehr, ist es der Standard-Style. */
    public static StyleDefinition getSelected(StyleCategory category) {
        String id = ConfigManager.get().getSelectedStyle(category);
        for (StyleDefinition style : getStyles(category)) {
            if (style.id().equals(id) || style.aliases().contains(id)) {
                return style;
            }
        }
        return getStyles(category).get(0);
    }

    /** Packs fuer alle aktiven Kategorien mit eigenem Style. Spaetere Eintraege haben Vorrang. */
    public static List<ResourcePack> createActivePacks() {
        ArneStylesConfig config = ConfigManager.get();
        List<ResourcePack> packs = new ArrayList<>();
        if (!config.modEnabled) {
            // Hauptschalter aus: Minecraft sieht keinen einzigen Style.
            return packs;
        }
        for (StyleCategory category : StyleCategory.values()) {
            if (category == StyleCategory.ESSENTIALS || !config.isEnabled(category)) {
                continue;
            }
            StyleDefinition style = getSelected(category);
            if (style.isVanilla() || !Files.isDirectory(style.root().resolve("assets")) || !CloudStyles.isReady(style)) {
                continue;
            }
            packs.add(pack(category.folderName() + "/" + style.id(), style.name(), style.root()));
        }

        // Essentials zuletzt, damit z. B. Small Totem auch ueber einem Tools-Style gilt.
        if (config.isEnabled(StyleCategory.ESSENTIALS)) {
            for (Essential essential : Essential.values()) {
                if (essential.kind() == Essential.Kind.PACK && config.isEssentialOn(essential.id())) {
                    essentialRoot(essential).ifPresent(root ->
                        packs.add(pack("essentials/" + essential.id(), Text.translatable(essential.nameKey()), root)));
                }
            }
            for (StyleDefinition custom : getStyles(StyleCategory.ESSENTIALS)) {
                if (!custom.isVanilla() && config.isEssentialOn(customEssentialKey(custom)) && Files.isDirectory(custom.root())) {
                    packs.add(pack("essentials/folder/" + custom.id(), custom.name(), custom.root()));
                }
            }
        }
        return packs;
    }

    private static ResourcePack pack(String id, Text name, Path root) {
        ResourcePackInfo info = new ResourcePackInfo("arnestyles/" + id, Text.literal("ArneTweaks: ").append(name), ResourcePackSource.BUILTIN, Optional.empty());
        return new DirectoryResourcePack(info, root);
    }

    /** Ordner mit den mitgelieferten Dateien eines Essentials (in der Mod-Jar oder im Entwicklungsordner). */
    public static Optional<Path> essentialRoot(Essential essential) {
        return modPath(essential.resourceFolder());
    }

    /** Ordner der mitgelieferten Performance-Packs in der Mod-Jar. */
    private static final String PERFORMANCE_FOLDER = "performance-packs";

    /** Ordner in der Mod-Jar (oder im Entwicklungsordner). */
    private static Optional<Path> modPath(String folder) {
        return FabricLoader.getInstance().getModContainer(ArneStyles.MOD_ID)
            .flatMap(mod -> mod.findPath(folder))
            .filter(Files::isDirectory);
    }

    /** Config-Schluessel fuer ein eigenes Essential aus styles/essentials/<ordner>. */
    public static String customEssentialKey(StyleDefinition style) {
        return "folder:" + style.id();
    }

    private static void writeReadmeIfMissing() {
        Path readme = stylesRoot().resolve(README_NAME);
        if (Files.exists(readme)) {
            return;
        }
        String text = """
            Arne Styles - eigene Styles
            ===========================

            Jeder Unterordner hier ist eine Kategorie (crystal, fire, obsidian, inventory, ...).
            In eine Kategorie legst du pro Style einen eigenen Ordner, zum Beispiel:

              crystal/
                bluxy/
                  style.json      (optional)  {"name": "BluxyCrystal"}
                  preview.png     (optional)  nur fuer Kategorien ohne eingebaute Vorschau, z. B. sky
                  assets/minecraft/textures/entity/end_crystal/end_crystal.png

            Der Ordner "assets" ist genau so aufgebaut wie in einem Texture-Pack.
            Kopiere einfach die Dateien, die du willst, mit demselben Pfad hinein.

            Im Spiel: Styles-Menue oeffnen (Standard: K), Kategorie waehlen, Style anklicken.
            Neue Ordner werden mit F5 oder "Neu laden" im Menue erkannt.

            Sonderfall essentials/: Hier ist jeder Ordner ein eigener Schalter. Du kannst
            beliebig viele gleichzeitig anschalten (z. B. kleine Items aus einem Pack).
            """;
        try {
            Files.writeString(readme, text, StandardCharsets.UTF_8);
        } catch (IOException e) {
            ArneStyles.LOGGER.warn("{} konnte nicht geschrieben werden.", readme, e);
        }
    }
}
