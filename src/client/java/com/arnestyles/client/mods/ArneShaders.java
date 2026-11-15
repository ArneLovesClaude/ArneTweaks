package com.arnestyles.client.mods;

import com.arnestyles.ArneStyles;
import com.arnestyles.client.config.ConfigManager;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.client.render.DefaultFramebufferSet;
import net.minecraft.client.util.memory.ObjectAllocator;
import net.minecraft.resource.DirectoryResourcePack;
import net.minecraft.resource.ResourcePack;
import net.minecraft.resource.ResourcePackInfo;
import net.minecraft.resource.ResourcePackSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Eigene ArneTweaks-Shader, laufen ohne Iris.
 * Es sind Bild-Shader: sie bekommen das fertige Bild der Welt und veraendern es (Farben, Kontrast,
 * Gluehen, Schaerfe ...). Minecraft fuehrt sie mit seinen eigenen Post-Effekten aus, so wie den Motion Blur.
 *
 * Eingebaute Shader liegen in der Mod (assets/arnestyles/shaders/post/look). Eigene Shader sind Ordner in
 * config/arnestyles/styles/shaders mit einer Datei shader.fsh (und optional shader.json mit {"name": "..."}).
 * Zu jedem Shader wird beim Laden der Ressourcen eine kleine Effekt-Beschreibung erzeugt und als Pack eingespielt.
 */
public final class ArneShaders {
    public static final String SHADER_FILE = "shader.fsh";
    private static final String INFO_FILE = "shader.json";
    private static final String BUILTIN = "builtin:";
    private static final String FOLDER = "folder:";

    /** Eingebaute Shader in der Reihenfolge der Seite. */
    public static final String[] BUILTINS = {"vibrant", "warm", "cold", "cinematic", "noir", "sharp", "glow"};

    /** Fehler beim letzten Laden des ausgewaehlten Shaders (Shader kaputt), wird auf der Seite gezeigt. */
    private static @Nullable String lastError;
    private static @Nullable String failedFor;

    private ArneShaders() {
    }

    /** Ein Shader auf der Seite. key ist das, was in der Config gespeichert wird. */
    public record Entry(String key, Text name, boolean builtIn) {
    }

    public static List<Entry> list() {
        List<Entry> entries = new ArrayList<>();
        for (String id : BUILTINS) {
            entries.add(new Entry(BUILTIN + id, Text.translatable("arnestyles.shader." + id), true));
        }
        for (Path dir : folderShaders()) {
            String folder = dir.getFileName().toString();
            entries.add(new Entry(FOLDER + folder, Text.literal(displayName(dir)), false));
        }
        return entries;
    }

    public static String selected() {
        String key = ConfigManager.get().arneShader;
        return key == null ? "" : key;
    }

    /** Shader auswaehlen ("" = aus). Eigene Shader brauchen ein Neuladen der Ressourcen, damit Aenderungen greifen. */
    public static void select(String key) {
        ConfigManager.get().arneShader = key;
        ConfigManager.save();
        lastError = null;
        failedFor = null;
        if (key.startsWith(FOLDER)) {
            MinecraftClient.getInstance().reloadResources();
        }
    }

    public static @Nullable String lastError() {
        return selected().equals(failedFor) ? lastError : null;
    }

    /** Wird nach dem Zeichnen der Welt aufgerufen, vor HUD und Menues (vor dem Motion Blur). */
    public static void apply(MinecraftClient client, ObjectAllocator pool) {
        String key = selected();
        if (key.isEmpty() || !ConfigManager.get().modEnabled || key.equals(failedFor)) {
            return;
        }
        Identifier effect = effectId(key);
        if (effect == null) {
            return;
        }
        PostEffectProcessor processor = client.getShaderLoader().loadPostEffect(effect, DefaultFramebufferSet.MAIN_ONLY);
        if (processor == null) {
            // Shader laesst sich nicht kompilieren oder fehlt: nicht jedes Bild neu versuchen, Grund steht im Log
            failedFor = key;
            lastError = "Details im Log (latest.log)";
            ArneStyles.LOGGER.warn("ArneTweaks-Shader {} konnte nicht geladen werden.", key);
            return;
        }
        processor.render(client.getFramebuffer(), pool);
    }

    private static @Nullable Identifier effectId(String key) {
        if (key.startsWith(BUILTIN)) {
            return Identifier.of(ArneStyles.MOD_ID, "look/" + key.substring(BUILTIN.length()));
        }
        if (key.startsWith(FOLDER)) {
            return Identifier.of(ArneStyles.MOD_ID, "user/" + resourceId(key.substring(FOLDER.length())));
        }
        return null;
    }

    // ---- Ressourcen ----

    private static Path generatedRoot() {
        return FabricLoader.getInstance().getConfigDir().resolve("arnestyles").resolve(".generated-shaders");
    }

    /**
     * Schreibt die Effekt-Beschreibungen fuer alle Shader und gibt sie als Pack zurueck.
     * Wird bei jedem Laden der Client-Ressourcen aufgerufen (ReloadableResourceManagerImplMixin).
     */
    public static @Nullable ResourcePack createPack() {
        // Nach dem Neuladen einen vorher kaputten Shader nochmal versuchen (er wurde vielleicht repariert)
        failedFor = null;
        lastError = null;
        Path root = generatedRoot();
        Path assets = root.resolve("assets").resolve(ArneStyles.MOD_ID);
        try {
            deleteTree(root);
            Path effects = assets.resolve("post_effect");
            for (String id : BUILTINS) {
                write(effects.resolve("look").resolve(id + ".json"), effectJson("look/" + id));
            }
            for (Path dir : folderShaders()) {
                String id = resourceId(dir.getFileName().toString());
                write(effects.resolve("user").resolve(id + ".json"), effectJson("user/" + id));
                Path target = assets.resolve("shaders").resolve("post").resolve("user").resolve(id + ".fsh");
                Files.createDirectories(target.getParent());
                Files.copy(dir.resolve(SHADER_FILE), target);
            }
        } catch (IOException e) {
            ArneStyles.LOGGER.error("ArneTweaks-Shader konnten nicht vorbereitet werden.", e);
            return null;
        }
        ResourcePackInfo info = new ResourcePackInfo("arnestyles/shaders", Text.literal("ArneTweaks: Shader"), ResourcePackSource.BUILTIN, Optional.empty());
        return new DirectoryResourcePack(info, root);
    }

    /** Ein Durchgang mit dem Shader in ein Zwischenbild, dann zurueck ins Hauptbild (wie die Effekte von Minecraft). */
    private static String effectJson(String shaderPath) {
        return """
            {
                "targets": {
                    "swap": {}
                },
                "passes": [
                    {
                        "vertex_shader": "minecraft:core/screenquad",
                        "fragment_shader": "arnestyles:post/%s",
                        "inputs": [
                            { "sampler_name": "In", "target": "minecraft:main", "bilinear": true }
                        ],
                        "output": "swap"
                    },
                    {
                        "vertex_shader": "minecraft:core/screenquad",
                        "fragment_shader": "minecraft:post/blit",
                        "inputs": [
                            { "sampler_name": "In", "target": "swap" }
                        ],
                        "output": "minecraft:main",
                        "uniforms": {
                            "BlitConfig": [
                                { "name": "ColorModulate", "type": "vec4", "value": [ 1.0, 1.0, 1.0, 1.0 ] }
                            ]
                        }
                    }
                ]
            }
            """.formatted(shaderPath);
    }

    /** Ordner in config/arnestyles/styles/shaders, die eine shader.fsh enthalten. */
    private static List<Path> folderShaders() {
        Path dir = ShaderPacks.dir();
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        try (Stream<Path> entries = Files.list(dir)) {
            return entries.filter(path -> Files.isRegularFile(path.resolve(SHADER_FILE)))
                .sorted(Comparator.comparing(path -> path.getFileName().toString().toLowerCase(Locale.ROOT)))
                .toList();
        } catch (IOException e) {
            ArneStyles.LOGGER.warn("Shader-Ordner {} konnte nicht gelesen werden.", dir, e);
            return List.of();
        }
    }

    public static boolean isArneShaderFolder(Path path) {
        return Files.isRegularFile(path.resolve(SHADER_FILE));
    }

    private static String displayName(Path dir) {
        Path info = dir.resolve(INFO_FILE);
        if (Files.isRegularFile(info)) {
            try (Reader reader = Files.newBufferedReader(info, StandardCharsets.UTF_8)) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                if (json.has("name")) {
                    return json.get("name").getAsString();
                }
            } catch (IOException | RuntimeException e) {
                ArneStyles.LOGGER.warn("{} konnte nicht gelesen werden.", info, e);
            }
        }
        return dir.getFileName().toString();
    }

    /** Ordnername als gueltiger Ressourcen-Name (nur a-z, 0-9, _), mit Kurz-Hash gegen gleiche Namen. */
    private static String resourceId(String folder) {
        String clean = folder.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_");
        return clean + "_" + Integer.toHexString(folder.hashCode() & 0xFFFFFF);
    }

    private static void write(Path file, String text) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, text, StandardCharsets.UTF_8);
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

    // ---- Anleitung im Shader-Ordner ----

    private static final String README = """
        ArneTweaks - Shader
        ===================

        Hier kommen zwei Arten von Shadern rein:

        1) ArneTweaks-Shader (laufen OHNE Iris)
           Ein Ordner mit einer Datei shader.fsh, zum Beispiel:

             MeinLook/
               shader.fsh
               shader.json   (optional)  {"name": "Mein Look"}

           shader.fsh ist ein Bild-Shader (GLSL 330). Er bekommt das fertige Bild und gibt die neue Farbe aus:

             #version 330
             uniform sampler2D InSampler;
             layout(std140) uniform SamplerInfo { vec2 OutSize; vec2 InSize; };
             in vec2 texCoord;
             out vec4 fragColor;
             void main() {
                 vec3 c = texture(InSampler, texCoord).rgb;
                 fragColor = vec4(c.bgr, 1.0);   // Beispiel: Rot und Blau tauschen
             }

           Mit "#moj_import <minecraft:globals.glsl>" gibt es ausserdem GameTime (fuer bewegte Effekte).
           Nach Aenderungen den Shader im Menue einfach nochmal anklicken.

        2) Normale Shader-Packs (BSL, Complementary, ...) als .zip
           Die brauchen die Mod Iris. Ist Iris installiert, erscheinen sie im Menue unter "Iris-Shaderpacks".
        """;

    public static void writeReadmeIfMissing() {
        Path readme = ShaderPacks.dir().resolve("LIES MICH.txt");
        if (Files.exists(readme)) {
            return;
        }
        try {
            Files.createDirectories(readme.getParent());
            Files.writeString(readme, README, StandardCharsets.UTF_8);
        } catch (IOException e) {
            ArneStyles.LOGGER.warn("Shader-Anleitung konnte nicht geschrieben werden.", e);
        }
    }
}
