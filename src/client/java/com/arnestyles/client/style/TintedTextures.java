package com.arnestyles.client.style;

import com.arnestyles.ArneStyles;
import com.arnestyles.client.config.ArneStylesConfig;
import com.arnestyles.client.config.ArneStylesConfig.CategoryTint;
import com.arnestyles.client.config.ConfigManager;
import com.arnestyles.client.config.StyleCategory;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Blocks;
import net.minecraft.block.TallPlantBlock;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.color.world.BiomeColors;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.resource.DirectoryResourcePack;
import net.minecraft.resource.InputSupplier;
import net.minecraft.resource.ResourcePack;
import net.minecraft.resource.ResourcePackInfo;
import net.minecraft.resource.ResourcePackSource;
import net.minecraft.resource.ResourceType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.biome.GrassColors;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Eigene Farben fuer Texturen.
 * - Kategorie-Farbe (kleiner Farb-Bereich ueber den Karten): gilt fuer alle passenden Texturen einer Kategorie.
 * - Textur-Farbe (Customize-Tab): gilt fuer genau eine Textur eines Styles und geht vor.
 * Die Texturen werden beim Laden der Ressourcen umgefaerbt und als oberstes Pack eingespielt.
 * Zwei Modi: Farbe ersetzen (Helligkeit bleibt) oder Farbton drehen (bunte Texturen bleiben bunt).
 * Erze: die Flecken werden durch Vergleich mit der Stein-Textur erkannt, der Stein-Hintergrund kann der
 * Stone-/Deepslate-Farbe folgen.
 */
public final class TintedTextures {
    /** Kategorien mit kleinem Farb-Bereich ueber den Karten. */
    public static final List<StyleCategory> CATEGORIES = List.of(StyleCategory.STONE, StyleCategory.DEEPSLATE, StyleCategory.ORES, StyleCategory.GRASS,
        StyleCategory.SNOW, StyleCategory.FIRE, StyleCategory.OBSIDIAN, StyleCategory.GLOWSTONE, StyleCategory.ANCHOR, StyleCategory.CRYSTAL);
    /** Basis fuer Stone und Deepslate, wenn kein eigener Style gewaehlt ist. */
    private static final String STONE_BASE = "bauloo_privat";
    public static final String DEFAULT_KEY = "default";
    public static final int MODE_REPLACE = 0;
    public static final int MODE_SHIFT = 1;

    private static final List<String> VANILLA_GRASS = List.of("grass_block_top", "grass_block_side_overlay", "short_grass", "tall_grass_top",
        "tall_grass_bottom", "fern", "large_fern_top", "large_fern_bottom");
    private static final List<String> VANILLA_FIRE = List.of("fire_0", "fire_1");
    private static final List<String> VANILLA_STONE = List.of("stone", "andesite", "diorite", "granite", "polished_andesite", "polished_diorite",
        "polished_granite", "stone_bricks", "cracked_stone_bricks", "smooth_stone");
    private static final List<String> VANILLA_DEEPSLATE = List.of("deepslate", "deepslate_top", "cobbled_deepslate", "polished_deepslate",
        "deepslate_bricks", "deepslate_tiles");
    private static final List<String> VANILLA_OBSIDIAN = List.of("obsidian", "crying_obsidian");
    private static final List<String> VANILLA_GLOWSTONE = List.of("glowstone");
    private static final List<String> VANILLA_ANCHOR = List.of("respawn_anchor_bottom", "respawn_anchor_side0", "respawn_anchor_side1",
        "respawn_anchor_side2", "respawn_anchor_side3", "respawn_anchor_side4", "respawn_anchor_top", "respawn_anchor_top_off");
    private static final List<String> VANILLA_ORES = List.of("coal_ore", "copper_ore", "iron_ore", "gold_ore", "redstone_ore", "lapis_ore",
        "diamond_ore", "emerald_ore", "deepslate_coal_ore", "deepslate_copper_ore", "deepslate_iron_ore", "deepslate_gold_ore",
        "deepslate_redstone_ore", "deepslate_lapis_ore", "deepslate_diamond_ore", "deepslate_emerald_ore");
    private static final List<String> VANILLA_SNOW = List.of("snow", "powder_snow", "ice", "packed_ice", "blue_ice");

    private TintedTextures() {
    }

    // ---- Welche Kategorien ----

    /** Kleiner Farb-Bereich ueber den Karten. */
    public static boolean supports(StyleCategory category) {
        return CATEGORIES.contains(category);
    }

    /** Im Customize-Tab bearbeitbar: alle Kategorien mit Texturen (nicht Himmel, Sounds, Inventar, HUD, Fadenkreuz, Essentials). */
    public static boolean customizable(StyleCategory category) {
        return switch (category) {
            case SKY, SOUNDS, INVENTORY, HUD, CROSSHAIR, ESSENTIALS -> false;
            default -> true;
        };
    }

    public static List<StyleCategory> customizableCategories() {
        List<StyleCategory> result = new ArrayList<>();
        for (StyleCategory category : StyleCategory.values()) {
            if (customizable(category)) {
                result.add(category);
            }
        }
        return result;
    }

    // ---- Farben lesen ----

    public static CategoryTint get(StyleCategory category) {
        return ConfigManager.get().tints.computeIfAbsent(category.name(), key -> new CategoryTint());
    }

    /** Kategorie-Farbe ist an (und die Mod an). */
    public static boolean active(StyleCategory category) {
        ArneStylesConfig config = ConfigManager.get();
        if (!config.modEnabled || !supports(category)) {
            return false;
        }
        CategoryTint tint = config.tints.get(category.name());
        return tint != null && tint.on;
    }

    /**
     * Zustaende einer Textur gehoeren zusammen: Ziffern am Ende und _on/_off fallen weg
     * (respawn_anchor_side0..4, fire_0/1, glitter_0..7, respawn_anchor_top_off). Im Customize-Tab ist das eine Kachel.
     */
    public static String stateGroup(String asset) {
        String base = asset.endsWith(".png") ? asset.substring(0, asset.length() - 4) : asset;
        String group = base.replaceAll("_?\\d+$", "").replaceAll("_(on|off)$", "");
        // Nur innerhalb des Dateinamens kuerzen und nie auf leer
        int slash = base.lastIndexOf('/');
        if (group.length() <= slash + 1) {
            return base;
        }
        return group;
    }

    /** Eine Kachel pro Zustands-Gruppe (die erste Datei steht fuer die Gruppe). */
    public static List<String> groupStates(List<String> textures) {
        java.util.Map<String, String> first = new java.util.LinkedHashMap<>();
        for (String asset : textures) {
            first.putIfAbsent(stateGroup(asset), asset);
        }
        return new ArrayList<>(first.values());
    }

    /** Wie viele Dateien zur Gruppe dieser Textur gehoeren. */
    public static int stateCount(List<String> textures, String asset) {
        String group = stateGroup(asset);
        int n = 0;
        for (String other : textures) {
            if (stateGroup(other).equals(group)) {
                n++;
            }
        }
        return n;
    }

    /** Schluessel der eigenen Farbe: gilt fuer alle Zustaende der Textur. */
    public static String textureKey(StyleCategory category, String styleKey, String asset) {
        return category.name() + "|" + styleKey + "|" + stateGroup(asset);
    }

    /** Schluessel aus Version 2.3 (eine Farbe pro Datei). */
    private static String fileKey(StyleCategory category, String styleKey, String asset) {
        return category.name() + "|" + styleKey + "|" + asset;
    }

    private static final String STATE_SUFFIX = "#state";

    /** Schluessel fuer genau einen Zustand (Erweitert-Modus im Customize-Tab), geht vor der Farbe der ganzen Textur. */
    public static String stateKey(StyleCategory category, String styleKey, String asset) {
        return category.name() + "|" + styleKey + "|" + asset + STATE_SUFFIX;
    }

    /** Eigene Farbe genau dieses Zustands oder null. */
    public static @Nullable CategoryTint stateTint(StyleCategory category, String styleKey, String asset) {
        CategoryTint tint = ConfigManager.get().textureTints.get(stateKey(category, styleKey, asset));
        return tint != null && tint.on ? tint : null;
    }

    /** Farbe fuer einen einzelnen Zustand zum Bearbeiten (startet mit der Farbe der ganzen Textur, noch aus). */
    public static CategoryTint editStateTint(StyleCategory category, String styleKey, String asset) {
        return ConfigManager.get().textureTints.computeIfAbsent(stateKey(category, styleKey, asset), key -> {
            CategoryTint group = groupTint(category, styleKey, asset);
            CategoryTint start = group != null ? group.copy() : active(category) ? get(category).copy() : new CategoryTint();
            start.on = false;
            return start;
        });
    }

    public static void clearStateTint(StyleCategory category, String styleKey, String asset) {
        ConfigManager.get().textureTints.remove(stateKey(category, styleKey, asset));
    }

    /** Entfernt die Einzel-Farben aller Zustaende dieser Textur (die Farbe der ganzen Textur bleibt). */
    public static void clearStateTints(StyleCategory category, String styleKey, String asset) {
        String prefix = category.name() + "|" + styleKey + "|";
        String group = stateGroup(asset);
        ConfigManager.get().textureTints.keySet().removeIf(key -> key.startsWith(prefix) && key.endsWith(STATE_SUFFIX)
            && stateGroup(key.substring(prefix.length(), key.length() - STATE_SUFFIX.length())).equals(group));
    }

    /** Eigene Farbe dieser Textur (bzw. ihres Zustands oder ihrer Zustands-Gruppe) oder null. */
    public static @Nullable CategoryTint textureTint(StyleCategory category, String styleKey, String asset) {
        CategoryTint state = stateTint(category, styleKey, asset);
        return state != null ? state : groupTint(category, styleKey, asset);
    }

    /** Farbe der ganzen Textur (alle Zustaende) oder null. */
    private static @Nullable CategoryTint groupTint(StyleCategory category, String styleKey, String asset) {
        var tints = ConfigManager.get().textureTints;
        CategoryTint tint = tints.get(textureKey(category, styleKey, asset));
        if (tint == null) {
            tint = tints.get(fileKey(category, styleKey, asset));
        }
        return tint != null && tint.on ? tint : null;
    }

    /** Farbe fuer eine Textur zum Bearbeiten im Customize-Tab (legt sie an, noch aus). */
    public static CategoryTint editTextureTint(StyleCategory category, String styleKey, String asset) {
        return ConfigManager.get().textureTints.computeIfAbsent(textureKey(category, styleKey, asset), key -> {
            CategoryTint old = ConfigManager.get().textureTints.get(fileKey(category, styleKey, asset));
            if (old != null) {
                return old.copy();
            }
            CategoryTint start = active(category) ? get(category).copy() : new CategoryTint();
            start.on = false;
            return start;
        });
    }

    public static void clearTextureTint(StyleCategory category, String styleKey, String asset) {
        String prefix = category.name() + "|" + styleKey + "|";
        String group = stateGroup(asset);
        ConfigManager.get().textureTints.keySet().removeIf(key -> {
            if (!key.startsWith(prefix)) {
                return false;
            }
            String rest = key.substring(prefix.length());
            if (rest.endsWith(STATE_SUFFIX)) {
                rest = rest.substring(0, rest.length() - STATE_SUFFIX.length());
            }
            return rest.equals(group) || stateGroup(rest).equals(group);
        });
    }

    /** Welche Farbe gilt fuer diese Textur: eigene Textur-Farbe, sonst Kategorie-Farbe (nur fuer passende Texturen). */
    public static @Nullable CategoryTint effective(StyleCategory category, String styleKey, String asset) {
        if (!ConfigManager.get().modEnabled) {
            return null;
        }
        CategoryTint own = textureTint(category, styleKey, asset);
        if (own != null) {
            return own;
        }
        String file = asset.substring(asset.lastIndexOf('/') + 1);
        return active(category) && belongs(category, file) ? get(category) : null;
    }

    /** Die gewaehlte Farbe als 0xRRGGBB (volle Helligkeit). Beim Farbton-Drehen: Farbe der Drehung, nur zur Anzeige. */
    public static int color(CategoryTint tint) {
        return MathHelper.hsvToRgb(tint.hue, tint.saturation, 1.0f) & 0xFFFFFF;
    }

    // ---- Styles und Texturen ----

    /** Schluessel eines Styles fuer Textur-Farben ("default" fuer den Minecraft-/Basis-Style). */
    public static String styleKey(StyleDefinition style) {
        return style.isVanilla() ? DEFAULT_KEY : style.id();
    }

    /**
     * Ordner des Styles, dessen Texturen im Spiel eingefaerbt werden, oder null fuer die Minecraft-Texturen.
     * Ist die Kategorie aktiv und ein eigener Style gewaehlt, wird dieser genommen.
     */
    public static @Nullable Path sourceRoot(StyleCategory category) {
        StyleDefinition selected = StyleRegistry.getSelected(category);
        if (ConfigManager.get().isEnabled(category) && !selected.isVanilla()) {
            return selected.root();
        }
        return baseRoot(category);
    }

    private static String sourceStyleKey(StyleCategory category) {
        StyleDefinition selected = StyleRegistry.getSelected(category);
        return ConfigManager.get().isEnabled(category) && !selected.isVanilla() ? selected.id() : DEFAULT_KEY;
    }

    /** Ordner, der fuer die Vorschau eines Styles eingefaerbt wird (Default = Basis). */
    public static @Nullable Path previewRoot(StyleDefinition style) {
        return style.isVanilla() ? baseRoot(style.category()) : style.root();
    }

    private static @Nullable Path baseRoot(StyleCategory category) {
        if (category == StyleCategory.STONE || category == StyleCategory.DEEPSLATE) {
            for (StyleDefinition style : StyleRegistry.getStyles(category)) {
                if (style.id().equalsIgnoreCase(STONE_BASE)) {
                    return style.root();
                }
            }
        }
        return null;
    }

    /** Alle Texturen eines Styles (Pfad relativ zu assets/minecraft/), fuer den Customize-Tab und zum Einfaerben. */
    public static List<String> textures(StyleDefinition style) {
        return texturesOf(style.category(), previewRoot(style));
    }

    private static List<String> texturesOf(StyleCategory category, @Nullable Path root) {
        List<String> result = new ArrayList<>();
        if (root != null) {
            Path textures = root.resolve("assets").resolve("minecraft").resolve("textures");
            if (Files.isDirectory(textures)) {
                try (Stream<Path> files = Files.walk(textures)) {
                    files.filter(path -> path.getFileName().toString().endsWith(".png"))
                        .sorted(Comparator.comparing(path -> textures.relativize(path).toString()))
                        .forEach(path -> result.add("textures/" + textures.relativize(path).toString().replace('\\', '/')));
                } catch (IOException e) {
                    ArneStyles.LOGGER.warn("Texturen in {} konnten nicht gelesen werden.", textures, e);
                }
            }
            if (!result.isEmpty()) {
                return result;
            }
        }
        List<String> names = switch (category) {
            case GRASS -> VANILLA_GRASS;
            case FIRE -> VANILLA_FIRE;
            case STONE -> VANILLA_STONE;
            case DEEPSLATE -> VANILLA_DEEPSLATE;
            case OBSIDIAN -> VANILLA_OBSIDIAN;
            case GLOWSTONE -> VANILLA_GLOWSTONE;
            case ANCHOR -> VANILLA_ANCHOR;
            case ORES -> VANILLA_ORES;
            case SNOW -> VANILLA_SNOW;
            default -> List.of();
        };
        for (String name : names) {
            result.add("textures/block/" + name + ".png");
        }
        if (result.isEmpty()) {
            for (String asset : category.previewAssets()) {
                if (asset.endsWith(".png")) {
                    result.add(asset);
                }
            }
        }
        return result;
    }

    /** Fuer die Kategorie-Farbe: bei Gras nur die gruenen Teile (nicht Erde), bei Feuer nur Flammen usw. */
    private static boolean belongs(StyleCategory category, String fileName) {
        String name = fileName.toLowerCase(Locale.ROOT);
        return switch (category) {
            case GRASS -> name.startsWith("grass_block_top") || name.startsWith("grass_block_side_overlay") || name.startsWith("short_grass")
                || name.startsWith("tall_grass") || name.startsWith("fern") || name.startsWith("large_fern");
            case FIRE -> name.startsWith("fire_") || name.startsWith("soul_fire_");
            case OBSIDIAN -> name.contains("obsidian");
            case GLOWSTONE -> name.startsWith("glowstone");
            case ANCHOR -> name.startsWith("respawn_anchor");
            case ORES -> name.contains("_ore");
            case CRYSTAL -> name.startsWith("end_crystal");
            default -> true;
        };
    }

    private static boolean isOre(String asset) {
        String file = asset.substring(asset.lastIndexOf('/') + 1);
        return asset.startsWith("textures/block/") && file.contains("_ore");
    }

    // ---- Einfaerben einer Textur ----

    /**
     * Wendet alle Farben an, die fuer diese Textur gelten. Gibt false zurueck, wenn nichts zu tun war.
     * root ist der Style-Ordner, aus dem die Textur kommt (fuer Erze: zum Finden der passenden Stein-Textur).
     */
    public static boolean apply(StyleCategory category, String styleKey, @Nullable Path root, String asset, NativeImage image) {
        CategoryTint tint = effective(category, styleKey, asset);
        if (category == StyleCategory.ORES && isOre(asset)) {
            return applyOre(styleKey, asset, image, tint);
        }
        if (tint == null) {
            return false;
        }
        recolor(image, tint, null, Float.NaN);
        return true;
    }

    /** Kurzer Text, der alle Farben dieser Textur beschreibt (fuer den Vorschau-Cache). Leer = keine Farbe. */
    public static String signature(StyleCategory category, String styleKey, String asset) {
        StringBuilder out = new StringBuilder();
        append(out, effective(category, styleKey, asset));
        if (category == StyleCategory.ORES && isOre(asset)) {
            CategoryTint background = oreBackgroundTint(asset);
            if (background != null) {
                out.append("|bg");
                append(out, background);
            }
        }
        return out.toString();
    }

    private static void append(StringBuilder out, @Nullable CategoryTint tint) {
        if (tint != null) {
            out.append(Math.round(tint.hue * 1000)).append(':').append(Math.round(tint.saturation * 1000)).append(':')
                .append(Math.round(tint.brightness * 1000)).append(':').append(tint.mode).append(tint.oreSpots ? 's' : 'w');
        }
    }

    private static @Nullable CategoryTint oreBackgroundTint(String asset) {
        if (!ConfigManager.get().oresFollowStone) {
            return null;
        }
        StyleCategory stone = asset.contains("deepslate_") ? StyleCategory.DEEPSLATE : StyleCategory.STONE;
        return active(stone) ? get(stone) : null;
    }

    /** Erz: Flecken und Stein-Hintergrund getrennt einfaerben. */
    private static boolean applyOre(String styleKey, String asset, NativeImage image, @Nullable CategoryTint spotTint) {
        CategoryTint background = oreBackgroundTint(asset);
        if (spotTint == null && background == null) {
            return false;
        }
        if (spotTint != null && !spotTint.oreSpots) {
            // Ganzes Erz in der Erz-Farbe
            recolor(image, spotTint, null, Float.NaN);
            return true;
        }
        boolean deepslate = asset.contains("deepslate_");
        String baseAsset = "textures/block/" + (deepslate ? "deepslate" : "stone") + ".png";
        NativeImage base = null;
        try {
            base = readImage(stoneRootFor(deepslate ? StyleCategory.DEEPSLATE : StyleCategory.STONE, styleKey), baseAsset);
            boolean[] spots = oreMask(image, base);
            if (background != null) {
                boolean[] stone = new boolean[spots.length];
                for (int i = 0; i < spots.length; i++) {
                    stone[i] = !spots[i];
                }
                // Gleiche Helligkeit wie die eingefaerbte Stein-Textur, damit Erz und Stein zusammenpassen
                recolor(image, background, stone, base == null ? Float.NaN : averageLuma(base, null));
            }
            if (spotTint != null) {
                recolor(image, spotTint, spots, Float.NaN);
            }
        } catch (IOException | RuntimeException e) {
            ArneStyles.LOGGER.warn("Erz {} konnte nicht eingefaerbt werden.", asset, e);
        } finally {
            if (base != null) {
                base.close();
            }
        }
        return true;
    }

    /** Stein-Textur, mit der ein Erz verglichen wird: gleicher Style in Stone/Deepslate, sonst Minecraft. */
    private static @Nullable Path stoneRootFor(StyleCategory stone, String styleKey) {
        if (!DEFAULT_KEY.equals(styleKey)) {
            for (StyleDefinition style : StyleRegistry.getStyles(stone)) {
                if (style.id().equalsIgnoreCase(styleKey) && Files.isRegularFile(style.root().resolve("assets/minecraft/textures/block/"
                    + (stone == StyleCategory.DEEPSLATE ? "deepslate" : "stone") + ".png"))) {
                    return style.root();
                }
            }
        }
        return null;
    }

    /**
     * true fuer Pixel, die zum Erz gehoeren: sie weichen deutlich vom Stein an derselben Stelle ab.
     * Ohne Stein-Textur: farbige Pixel (Stein ist grau).
     */
    private static boolean[] oreMask(NativeImage ore, @Nullable NativeImage base) {
        int w = ore.getWidth();
        int h = ore.getHeight();
        boolean[] mask = new boolean[w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb = ore.getColorArgb(x, y);
                if ((argb >>> 24) == 0) {
                    continue;
                }
                if (base != null) {
                    int bx = x * base.getWidth() / w;
                    int by = (y % w) * base.getHeight() / w;
                    int b = base.getColorArgb(Math.min(bx, base.getWidth() - 1), Math.min(by, base.getHeight() - 1));
                    int diff = Math.abs(((argb >> 16) & 0xFF) - ((b >> 16) & 0xFF)) + Math.abs(((argb >> 8) & 0xFF) - ((b >> 8) & 0xFF))
                        + Math.abs((argb & 0xFF) - (b & 0xFF));
                    mask[y * w + x] = diff > 60;
                } else {
                    mask[y * w + x] = saturation(argb) > 0.2f;
                }
            }
        }
        return mask;
    }

    /**
     * Faerbt die Pixel (alle oder nur die mit mask = true) um.
     * Farbe ersetzen: Helligkeit des Pixels bleibt, durchschnittliche Helligkeit wird auf einen festen Wert gebracht
     * (average = NaN: aus den Pixeln selbst berechnet). Farbton drehen: Farbton verschieben, Saettigung und Helligkeit skalieren.
     */
    public static void recolor(NativeImage image, CategoryTint tint, boolean @Nullable [] mask, float average) {
        int w = image.getWidth();
        int h = image.getHeight();
        if (tint.mode == MODE_SHIFT) {
            float satScale = tint.saturation * 2.0f;
            float valScale = tint.brightness * 2.0f;
            float[] hsv = new float[3];
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    if (mask != null && !mask[y * w + x]) {
                        continue;
                    }
                    int argb = image.getColorArgb(x, y);
                    int alpha = argb >>> 24;
                    if (alpha == 0) {
                        continue;
                    }
                    rgbToHsv(argb, hsv);
                    float hue = (hsv[0] + tint.hue) % 1.0f;
                    float sat = MathHelper.clamp(hsv[1] * satScale, 0.0f, 1.0f);
                    float val = MathHelper.clamp(hsv[2] * valScale, 0.0f, 1.0f);
                    int rgb = MathHelper.hsvToRgb(hue, sat, val) & 0xFFFFFF;
                    image.setColorArgb(x, y, (alpha << 24) | rgb);
                }
            }
            return;
        }
        int rgb = color(tint);
        float tr = ((rgb >> 16) & 0xFF) / 255.0f;
        float tg = ((rgb >> 8) & 0xFF) / 255.0f;
        float tb = (rgb & 0xFF) / 255.0f;
        float avg = Float.isNaN(average) ? averageLuma(image, mask) : average;
        // Helligkeit 0..1 -> mittlere Helligkeit 0.25..0.95
        float gain = (0.25f + 0.7f * tint.brightness) / Math.max(0.05f, avg);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (mask != null && !mask[y * w + x]) {
                    continue;
                }
                int argb = image.getColorArgb(x, y);
                int alpha = argb >>> 24;
                if (alpha == 0) {
                    continue;
                }
                float l = luma(argb) * gain;
                int r = Math.round(MathHelper.clamp(tr * l, 0.0f, 1.0f) * 255.0f);
                int g = Math.round(MathHelper.clamp(tg * l, 0.0f, 1.0f) * 255.0f);
                int b = Math.round(MathHelper.clamp(tb * l, 0.0f, 1.0f) * 255.0f);
                image.setColorArgb(x, y, (alpha << 24) | (r << 16) | (g << 8) | b);
            }
        }
    }

    /** Alte Schnittstelle: ganze Textur in einer Farbe (Kategorie-Vorschau). */
    public static void recolor(NativeImage image, CategoryTint tint) {
        recolor(image, tint, null, Float.NaN);
    }

    /** Farbe in Farbton, Saettigung, Helligkeit (je 0 bis 1). */
    private static void rgbToHsv(int argb, float[] out) {
        float r = ((argb >> 16) & 0xFF) / 255.0f;
        float g = ((argb >> 8) & 0xFF) / 255.0f;
        float b = (argb & 0xFF) / 255.0f;
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float d = max - min;
        float hue;
        if (d == 0.0f) {
            hue = 0.0f;
        } else if (max == r) {
            hue = ((g - b) / d) / 6.0f;
        } else if (max == g) {
            hue = ((b - r) / d + 2.0f) / 6.0f;
        } else {
            hue = ((r - g) / d + 4.0f) / 6.0f;
        }
        out[0] = hue < 0.0f ? hue + 1.0f : hue;
        out[1] = max == 0.0f ? 0.0f : d / max;
        out[2] = max;
    }

    private static float averageLuma(NativeImage image, boolean @Nullable [] mask) {
        int w = image.getWidth();
        int h = image.getHeight();
        double sum = 0.0;
        int count = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (mask != null && !mask[y * w + x]) {
                    continue;
                }
                int argb = image.getColorArgb(x, y);
                if ((argb >>> 24) > 8) {
                    sum += luma(argb);
                    count++;
                }
            }
        }
        return count == 0 ? 0.5f : (float) Math.max(0.05, sum / count);
    }

    private static float luma(int argb) {
        float r = ((argb >> 16) & 0xFF) / 255.0f;
        float g = ((argb >> 8) & 0xFF) / 255.0f;
        float b = (argb & 0xFF) / 255.0f;
        return 0.2126f * r + 0.7152f * g + 0.0722f * b;
    }

    private static float saturation(int argb) {
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        int max = Math.max(r, Math.max(g, b));
        int min = Math.min(r, Math.min(g, b));
        return max == 0 ? 0.0f : (max - min) / (float) max;
    }

    // ---- Pack beim Laden der Ressourcen ----

    private static Path generatedRoot() {
        return FabricLoader.getInstance().getConfigDir().resolve("arnestyles").resolve(".generated-tints");
    }

    /** Hat diese Kategorie irgendeine Farbe (Kategorie, einzelne Textur oder Erz-Hintergrund)? */
    private static boolean hasAnyTint(StyleCategory category) {
        if (active(category)) {
            return true;
        }
        String prefix = category.name() + "|";
        for (var entry : ConfigManager.get().textureTints.entrySet()) {
            if (entry.getKey().startsWith(prefix) && entry.getValue().on) {
                return true;
            }
        }
        return category == StyleCategory.ORES && ConfigManager.get().oresFollowStone
            && (active(StyleCategory.STONE) || active(StyleCategory.DEEPSLATE));
    }

    /** Schreibt alle eingefaerbten Texturen und gibt sie als Pack zurueck (oder null, wenn nichts eingefaerbt ist). */
    public static @Nullable ResourcePack createPack() {
        Path root = generatedRoot();
        try {
            deleteTree(root);
        } catch (IOException e) {
            ArneStyles.LOGGER.warn("Alte eingefaerbte Texturen konnten nicht geloescht werden.", e);
        }
        grassTinted = ConfigManager.get().modEnabled && hasAnyTint(StyleCategory.GRASS);
        if (!ConfigManager.get().modEnabled) {
            return null;
        }
        boolean any = false;
        for (StyleCategory category : customizableCategories()) {
            if (!hasAnyTint(category)) {
                continue;
            }
            Path source = sourceRoot(category);
            String styleKey = sourceStyleKey(category);
            for (String asset : texturesOf(category, source)) {
                try (NativeImage image = readImage(source, asset)) {
                    if (image == null || !apply(category, styleKey, source, asset, image)) {
                        continue;
                    }
                    Path target = root.resolve("assets").resolve("minecraft").resolve(asset);
                    Files.createDirectories(target.getParent());
                    image.writeTo(target);
                    copyMeta(source, asset, target);
                    any = true;
                } catch (IOException | RuntimeException e) {
                    ArneStyles.LOGGER.warn("Textur {} konnte nicht eingefaerbt werden.", asset, e);
                }
            }
        }
        if (!any) {
            return null;
        }
        ResourcePackInfo info = new ResourcePackInfo("arnestyles/tints", Text.literal("ArneTweaks: Farben"), ResourcePackSource.BUILTIN, Optional.empty());
        return new DirectoryResourcePack(info, root);
    }

    /** Bild aus dem Style-Ordner oder (root == null bzw. Datei fehlt) aus Minecraft. */
    public static @Nullable NativeImage readImage(@Nullable Path root, String asset) throws IOException {
        if (root != null) {
            Path file = root.resolve("assets").resolve("minecraft").resolve(asset);
            if (Files.isRegularFile(file)) {
                try (InputStream in = Files.newInputStream(file)) {
                    return NativeImage.read(in);
                }
            }
        }
        InputSupplier<InputStream> vanilla = MinecraftClient.getInstance().getDefaultResourcePack()
            .open(ResourceType.CLIENT_RESOURCES, Identifier.ofVanilla(asset));
        if (vanilla == null) {
            return null;
        }
        try (InputStream in = vanilla.get()) {
            return NativeImage.read(in);
        }
    }

    /** Animations-Datei (.mcmeta) mitnehmen, sonst laeuft z. B. das Feuer nicht. */
    private static void copyMeta(@Nullable Path root, String asset, Path target) throws IOException {
        Path out = target.resolveSibling(target.getFileName() + ".mcmeta");
        if (root != null) {
            Path meta = root.resolve("assets").resolve("minecraft").resolve(asset + ".mcmeta");
            if (Files.isRegularFile(meta)) {
                Files.copy(meta, out);
                return;
            }
            if (Files.isRegularFile(root.resolve("assets").resolve("minecraft").resolve(asset))) {
                return;
            }
        }
        InputSupplier<InputStream> vanilla = MinecraftClient.getInstance().getDefaultResourcePack()
            .open(ResourceType.CLIENT_RESOURCES, Identifier.ofVanilla(asset + ".mcmeta"));
        if (vanilla != null) {
            try (InputStream in = vanilla.get()) {
                Files.copy(in, out);
            }
        }
    }

    private static void deleteTree(Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(root)) {
            for (Path path : walk.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(path);
            }
        }
    }

    // ---- Gras: Biom-Farbe aus, wenn eine eigene Farbe gewaehlt ist ----

    /**
     * Gras hat eine eigene Farbe (Kategorie oder einzelne Gras-Textur): dann keine Biom-Einfaerbung.
     * Wird beim Laden der Ressourcen gesetzt, weil die Abfrage fuer jeden Grasblock laeuft.
     */
    private static volatile boolean grassTinted;

    private static boolean grassTinted() {
        return grassTinted;
    }

    /**
     * Minecraft faerbt Gras je nach Biom ein. Mit eigener Farbe ist die Textur schon fertig gefaerbt,
     * deshalb gibt es dann Weiss (keine Veraenderung) statt der Biom-Farbe.
     */
    public static void registerGrassColors() {
        ColorProviderRegistry.BLOCK.register((state, world, pos, tintIndex) -> {
            if (grassTinted()) {
                return 0xFFFFFF;
            }
            if (world == null || pos == null) {
                return GrassColors.getDefaultColor();
            }
            return BiomeColors.getGrassColor(world, state.get(TallPlantBlock.HALF) == DoubleBlockHalf.UPPER ? pos.down() : pos);
        }, Blocks.TALL_GRASS, Blocks.LARGE_FERN);
        ColorProviderRegistry.BLOCK.register((state, world, pos, tintIndex) -> {
            if (grassTinted()) {
                return 0xFFFFFF;
            }
            return world != null && pos != null ? BiomeColors.getGrassColor(world, pos) : GrassColors.getDefaultColor();
        }, Blocks.GRASS_BLOCK, Blocks.SHORT_GRASS, Blocks.FERN, Blocks.POTTED_FERN, Blocks.BUSH);
    }
}
