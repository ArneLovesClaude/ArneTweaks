package com.arnestyles.client.style;

import com.arnestyles.ArneStyles;
import com.arnestyles.client.config.ArneStylesConfig;
import com.arnestyles.client.config.StyleCategory;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.resource.InputSupplier;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Laedt Texturen fuer die Vorschau im Menue direkt aus den Style-Ordnern,
 * ohne die Ressourcen des Spiels neu zu laden. Fehlt eine Datei im Style,
 * wird die normale Minecraft-Textur verwendet.
 */
public final class PreviewTextures {
    /** Eine hochgeladene Vorschau-Textur. frames > 1 bei animierten Texturen (Frames untereinander). */
    public record Entry(Identifier id, int width, int height, int frames, int frameTime) {
        /** Aktueller Frame, Minecraft-Animationen laufen mit 20 Ticks pro Sekunde. */
        public int currentFrame() {
            if (frames <= 1) {
                return 0;
            }
            long ticks = System.currentTimeMillis() / 50L;
            return (int) ((ticks / frameTime) % frames);
        }

        public int frameHeight() {
            return frames > 1 ? width : height;
        }
    }

    private static final Map<String, Optional<Entry>> CACHE = new HashMap<>();
    private static int counter;

    private PreviewTextures() {
    }

    /** Textur aus dem Style (Pfad relativ zu assets/minecraft/), sonst die Minecraft-Textur. */
    public static @Nullable Entry forStyle(StyleDefinition style, String assetPath) {
        String key = style.category().name() + "/" + style.id() + "/" + assetPath;
        return CACHE.computeIfAbsent(key, k -> Optional.ofNullable(load(style.root(), assetPath))).orElse(null);
    }

    /** Wie forStyle, aber mit beliebigem Pack-Ordner (z. B. mitgelieferte Essentials). root darf null sein. */
    public static @Nullable Entry forRoot(String cacheKey, @Nullable Path root, String assetPath) {
        String key = "root:" + cacheKey + "/" + assetPath;
        return CACHE.computeIfAbsent(key, k -> Optional.ofNullable(load(root, assetPath))).orElse(null);
    }

    /** Ein einzelnes Bild, z. B. preview.png. */
    public static @Nullable Entry forFile(Path file) {
        String key = "file:" + file.toAbsolutePath();
        return CACHE.computeIfAbsent(key, k -> Optional.ofNullable(loadFile(file, 0))).orElse(null);
    }

    /** Eingefaerbte Vorschauen: pro Karte nur die aktuelle Farbe, beim Schieben wird die alte freigegeben. */
    private record Tinted(String tintKey, @Nullable Entry entry) {
    }

    private static final Map<String, Tinted> TINTED = new HashMap<>();

    /**
     * Vorschau einer Textur mit allen Farben, die fuer sie gelten (Kategorie, einzelne Textur, Erz-Hintergrund).
     * Ohne Farbe: die normale Vorschau des Styles. Wird genauso berechnet wie spaeter im Spiel (Live-Vorschau).
     */
    public static @Nullable Entry colored(StyleDefinition style, String assetPath) {
        String styleKey = TintedTextures.styleKey(style);
        return colored(style.category(), styleKey, TintedTextures.previewRoot(style), assetPath, () -> forStyle(style, assetPath));
    }

    public static @Nullable Entry colored(StyleCategory category, String styleKey, @Nullable Path root, String assetPath,
                                          java.util.function.Supplier<@Nullable Entry> plain) {
        String signature = TintedTextures.signature(category, styleKey, assetPath);
        String key = category.name() + "/" + styleKey + "/" + assetPath;
        Tinted cached = TINTED.get(key);
        if (signature.isEmpty()) {
            if (cached != null && cached.entry() != null) {
                MinecraftClient.getInstance().getTextureManager().destroyTexture(cached.entry().id());
                TINTED.remove(key);
            }
            return plain.get();
        }
        if (cached != null && cached.tintKey().equals(signature)) {
            return cached.entry();
        }
        if (cached != null && cached.entry() != null) {
            MinecraftClient.getInstance().getTextureManager().destroyTexture(cached.entry().id());
        }
        Entry entry = null;
        try {
            NativeImage image = TintedTextures.readImage(root, assetPath);
            if (image != null) {
                TintedTextures.apply(category, styleKey, root, assetPath, image);
                if (TintedTextures.rainbowFor(category, styleKey, assetPath, image)) {
                    // Regenbogen: dieselbe Bildfolge wie im Spiel, damit die Vorschau mitlaeuft
                    NativeImage strip = TintedTextures.rainbowStrip(image);
                    image.close();
                    entry = upload(strip, TintedTextures.rainbowFrameTime(category, styleKey, assetPath));
                    TINTED.put(key, new Tinted(signature, entry));
                    return entry;
                }
                Path styleFile = root == null ? null : root.resolve("assets").resolve("minecraft").resolve(assetPath);
                int frameTime = styleFile != null && Files.isRegularFile(styleFile)
                    ? readFrameTime(styleFile.resolveSibling(styleFile.getFileName() + ".mcmeta"))
                    : vanillaFrameTime(assetPath);
                entry = upload(image, frameTime);
            }
        } catch (IOException | RuntimeException e) {
            ArneStyles.LOGGER.warn("Eingefaerbte Vorschau {} konnte nicht erstellt werden.", assetPath, e);
        }
        TINTED.put(key, new Tinted(signature, entry));
        return entry;
    }

    /** Vorschau einer Textur ohne Farbe aus einem bestimmten Ordner (Customize-Tab). */
    public static @Nullable Entry plain(String cacheKey, @Nullable Path root, String assetPath) {
        return forRoot(cacheKey, root, assetPath);
    }

    /** Gibt alle Vorschau-Texturen frei (beim Schliessen des Menues). */
    public static void clear() {
        MinecraftClient client = MinecraftClient.getInstance();
        for (Optional<Entry> entry : CACHE.values()) {
            entry.ifPresent(e -> client.getTextureManager().destroyTexture(e.id()));
        }
        CACHE.clear();
        for (Tinted tinted : TINTED.values()) {
            if (tinted.entry() != null) {
                client.getTextureManager().destroyTexture(tinted.entry().id());
            }
        }
        TINTED.clear();
    }

    private static @Nullable Entry load(@Nullable Path root, String assetPath) {
        if (root != null) {
            Path file = root.resolve("assets").resolve("minecraft").resolve(assetPath);
            if (Files.isRegularFile(file)) {
                return loadFile(file, readFrameTime(file.resolveSibling(file.getFileName() + ".mcmeta")));
            }
        }
        return loadVanilla(assetPath);
    }

    private static @Nullable Entry loadFile(Path file, int frameTime) {
        try (InputStream in = Files.newInputStream(file)) {
            return upload(NativeImage.read(in), frameTime);
        } catch (IOException | RuntimeException e) {
            ArneStyles.LOGGER.warn("Vorschau {} konnte nicht geladen werden.", file, e);
            return null;
        }
    }

    private static @Nullable Entry loadVanilla(String assetPath) {
        if (!Identifier.isPathValid(assetPath)) {
            // z. B. "textures/Desktop/1.png" aus einem Pack-Modell: Minecraft erlaubt nur Kleinbuchstaben, also keine Vorschau
            return null;
        }
        var pack = MinecraftClient.getInstance().getDefaultResourcePack();
        InputSupplier<InputStream> image = pack.open(ResourceType.CLIENT_RESOURCES, Identifier.ofVanilla(assetPath));
        if (image == null) {
            return null;
        }
        int frameTime = vanillaFrameTime(assetPath);
        try (InputStream in = image.get()) {
            return upload(NativeImage.read(in), frameTime);
        } catch (IOException | RuntimeException e) {
            ArneStyles.LOGGER.warn("Minecraft-Textur {} konnte nicht geladen werden.", assetPath, e);
            return null;
        }
    }

    /** Wie readFrameTime, aber fuer die Minecraft-Textur selbst. */
    private static int vanillaFrameTime(String assetPath) {
        if (!Identifier.isPathValid(assetPath + ".mcmeta")) {
            return 0;
        }
        InputSupplier<InputStream> meta = MinecraftClient.getInstance().getDefaultResourcePack()
            .open(ResourceType.CLIENT_RESOURCES, Identifier.ofVanilla(assetPath + ".mcmeta"));
        if (meta == null) {
            return 0;
        }
        try (InputStream in = meta.get()) {
            return parseFrameTime(in);
        } catch (IOException | RuntimeException e) {
            return 0;
        }
    }

    /** Ticks pro Bild aus der .mcmeta, oder 0 wenn die Textur nicht animiert ist. */
    private static int readFrameTime(Path mcmeta) {
        if (!Files.isRegularFile(mcmeta)) {
            return 0;
        }
        try (InputStream in = Files.newInputStream(mcmeta)) {
            return parseFrameTime(in);
        } catch (IOException | RuntimeException e) {
            return 0;
        }
    }

    private static int parseFrameTime(InputStream in) {
        try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            JsonObject animation = root.getAsJsonObject("animation");
            if (animation != null) {
                JsonElement frameTime = animation.get("frametime");
                if (frameTime != null && frameTime.isJsonPrimitive()) {
                    return Math.max(1, frameTime.getAsInt());
                }
                return 1;
            }
        } catch (IOException | RuntimeException e) {
            // Ungueltige .mcmeta: wie Minecraft als nicht animiert behandeln.
        }
        return 0;
    }

    private static Entry upload(NativeImage image, int frameTime) {
        Identifier id = Identifier.of(ArneStyles.MOD_ID, "preview/" + counter++);
        MinecraftClient.getInstance().getTextureManager().registerTexture(id, new NativeImageBackedTexture(() -> "arnestyles preview " + id, image));
        int width = image.getWidth();
        int height = image.getHeight();
        // Nur Texturen mit "animation" in der .mcmeta sind Bildfolgen. Hohe Bilder ohne (z. B. Regen 64x256) sind ein Bild.
        int frames = frameTime > 0 && width > 0 && height > width && height % width == 0 ? height / width : 1;
        return new Entry(id, width, height, frames, Math.max(1, frameTime));
    }
}
