package com.arnestyles.client.style;

import com.arnestyles.ArneStyles;
import com.arnestyles.client.config.StyleCategory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.ResourceType;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Importiert ganze Texture-Packs (.zip oder Ordner): jede Datei wird anhand ihres Pfads einer Kategorie zugeordnet
 * und landet als Style in config/arnestyles/styles/<kategorie>/<pack>. Gleiche Regeln wie tools/sort_packs.py.
 */
public final class PackImporter {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    /** Nur Dateitypen, die ein Texture-Pack wirklich braucht. Alles andere (z. B. .exe, .jar, .bat) wird nie kopiert. */
    private static final Set<String> ALLOWED = Set.of("png", "json", "mcmeta", "properties", "ogg", "txt", "jem", "jpm");
    /** Schutz vor riesigen oder praeparierten Packs (ZIP-Bomben). */
    private static final long MAX_PACK_BYTES = 512L * 1024 * 1024;
    private static final int MAX_PACK_FILES = 60_000;

    private static final String MAT = "(wooden|stone|iron|golden|diamond|netherite|copper)";
    private static final String ARMOR_ITEM = "\\w+_(helmet|chestplate|leggings|boots)";
    private static final String ORES = "(coal|copper|iron|gold|redstone|lapis|diamond|emerald)_ore";
    private static final String SNOW_BLOCKS = "snow|snow_block|powder_snow|grass_block_snow|ice|packed_ice|blue_ice";
    private static final String DEEPSLATE_BLOCKS = "deepslate|deepslate_top|cobbled_deepslate|polished_deepslate|deepslate_bricks|deepslate_tiles"
        + "|cracked_deepslate_\\w+|tuff";
    private static final String STONE_BLOCKS = "stone|smooth_stone|smooth_stone_slab_side|stone_bricks|cracked_stone_bricks|mossy_stone_bricks"
        + "|mossy_cobblestone|andesite|diorite|granite|polished_(andesite|diorite|granite)|calcite|gravel|sand|sandstone(_top|_bottom)?|bedrock|clay";
    private static final String ORE_FILES = "(deepslate_)?" + ORES + "(/\\w+)?";
    private static final String GRASS_BLOCKS = "grass_block(_top|_side|_side_overlay)?|short_grass|tall_grass(_top|_bottom)?|fern|large_fern_(top|bottom)"
        + "|dirt|coarse_dirt|rooted_dirt|dirt_path(_top|_side)?|podzol(_top|_side)?|mycelium(_top|_side)?|farmland(_moist)?";
    private static final String FOOD = "cooked_\\w+|beef|porkchop|chicken|mutton|rabbit|cod|salmon|bread|golden_carrot|carrot|baked_potato|potato|apple"
        + "|cookie|melon_slice|sweet_berries|glow_berries|pumpkin_pie|mushroom_stew|rabbit_stew|beetroot_soup|suspicious_stew|dried_kelp|honey_bottle";
    private static final String BUCKETS = "bucket|\\w+_bucket";
    private static final String BOWS = "bow(_pulling_\\d)?|crossbow\\w*|arrow|spectral_arrow|tipped_arrow\\w*";
    private static final String BLOCKS = "cobblestone|\\w+_planks";
    private static final String ITEM_MODEL = "(models/item|items)/";

    private record Rule(String category, Pattern pattern) {
    }

    /** Reihenfolge zaehlt: die erste passende Regel gewinnt. Pfade relativ zu assets/minecraft/. */
    private static final List<Rule> RULES = new ArrayList<>();

    private static void rule(String category, String regex) {
        RULES.add(new Rule(category, Pattern.compile(regex)));
    }

    static {
        rule("crystal", "textures/entity/end_crystal/.+\\.png");
        rule("totems", "textures/item/totem_of_undying\\.png");
        rule("totems", ITEM_MODEL + "totem_of_undying\\.json");
        rule("totems", "textures/particle/glitter_[0-7]\\.png");
        rule("totems", "particles/totem_of_undying\\.json");
        rule("totems", "sounds/item/totem/.+\\.ogg");
        rule("glowstone", "textures/block/glowstone\\.png");
        rule("gaps", "textures/item/(enchanted_)?golden_apple\\.png");
        rule("gaps", ITEM_MODEL + "(enchanted_)?golden_apple\\.json");
        rule("xp_bottles", "textures/item/experience_bottle\\.png");
        rule("xp_bottles", "textures/entity/experience_orb\\.png");
        rule("xp_bottles", ITEM_MODEL + "experience_bottle\\.json");
        rule("pearls", "textures/item/ender_pearl\\.png");
        rule("pearls", ITEM_MODEL + "ender_pearl\\.json");
        rule("potions", "textures/item/(potion|splash_potion|lingering_potion|potion_overlay)\\.png");
        rule("potions", ITEM_MODEL + "(potion|splash_potion|lingering_potion)\\.json");
        rule("wind_charges", "textures/item/wind_charge\\.png");
        rule("wind_charges", "textures/entity/projectiles/wind_charge\\.png");
        rule("wind_charges", ITEM_MODEL + "wind_charge\\.json");
        rule("shield", "textures/entity/(shield/.+|shield_base\\w*)\\.png");
        rule("shield", ITEM_MODEL + "shield(_blocking)?\\.json");
        rule("elytra", "textures/entity/equipment/wings/.+\\.png");
        rule("elytra", "textures/item/elytra(_broken)?\\.png");
        rule("elytra", ITEM_MODEL + "elytra(_broken)?\\.json");
        rule("glint", "textures/misc/enchanted_glint_(item|armor)\\.png");
        rule("fire", "textures/block/(soul_)?fire_[01]\\.png");
        rule("obsidian", "textures/block/(crying_)?obsidian(_e)?\\.png");
        rule("anchor", "textures/block/respawn_anchor_\\w+\\.png");
        rule("inventory", "textures/gui/(container|sprites/container)/.+\\.png");
        rule("crosshair", "textures/gui/sprites/hud/crosshair\\w*\\.png");
        rule("hud", "textures/gui/sprites/hud/.+\\.png");
        rule("swords", "textures/item/" + MAT + "_sword\\.png");
        rule("swords", ITEM_MODEL + MAT + "_sword\\.json");
        rule("axes", "textures/item/" + MAT + "_axe\\.png");
        rule("axes", ITEM_MODEL + MAT + "_axe\\.json");
        rule("mace", "textures/item/mace\\.png");
        rule("mace", ITEM_MODEL + "(mace|handheld_mace)\\.json");
        rule("spears", "textures/item/\\w+_spear(_in_hand)?\\.png");
        rule("spears", ITEM_MODEL + "(\\w+_spear(_in_hand)?|spear_in_hand)\\.json");
        rule("bows", "textures/item/(" + BOWS + ")\\.png");
        rule("bows", "textures/entity/projectiles/(arrow|spectral_arrow|tipped_arrow)\\.png");
        rule("bows", ITEM_MODEL + "(" + BOWS + ")\\.json");
        rule("tools", "textures/item/(" + MAT + "_(pickaxe|shovel|hoe)|trident)\\.png");
        rule("armor", "textures/entity/equipment/.+\\.png");
        rule("armor", "textures/item/" + ARMOR_ITEM + "\\.png");
        rule("sky", "textures/environment/.+\\.png");
        rule("sky", "(optifine|mcpatcher)/sky/.+");
        rule("particles", "textures/particle/.+\\.png");
        rule("cobwebs", "textures/block/cobweb\\.png");
        rule("cobwebs", "(blockstates|models/item|items)/cobweb\\.json");
        rule("blocks", "textures/block/(" + BLOCKS + ")\\.png");
        rule("blocks", "blockstates/(" + BLOCKS + ")\\.json");
        rule("shulker", "textures/entity/shulker/.+\\.png");
        rule("shulker", "textures/block/\\w*shulker_box\\.png");
        rule("snow", "textures/block/(" + SNOW_BLOCKS + ")\\.png");
        rule("snow", "blockstates/(" + SNOW_BLOCKS + ")\\.json");
        rule("ores", "textures/block/(" + ORE_FILES + ")\\.png");
        rule("ores", "(blockstates|models/block)/(" + ORE_FILES + ")\\.json");
        rule("deepslate", "textures/block/(" + DEEPSLATE_BLOCKS + ")\\.png");
        rule("deepslate", "blockstates/(" + DEEPSLATE_BLOCKS + ")\\.json");
        rule("stone", "textures/block/(" + STONE_BLOCKS + ")\\.png");
        rule("stone", "blockstates/(" + STONE_BLOCKS + ")\\.json");
        rule("grass", "textures/block/(" + GRASS_BLOCKS + ")\\.png");
        rule("grass", "textures/colormap/(grass|foliage)\\.png");
        rule("grass", "blockstates/(" + GRASS_BLOCKS + ")\\.json");
        rule("buckets", "textures/item/(" + BUCKETS + ")\\.png");
        rule("buckets", ITEM_MODEL + "(" + BUCKETS + ")\\.json");
        rule("food", "textures/item/(" + FOOD + ")\\.png");
        rule("food", ITEM_MODEL + "(" + FOOD + ")\\.json");
        rule("sounds", "sounds/(?!.*/\\._)(?!\\._).+\\.ogg");
        rule("sounds", "sounds\\.json");
        rule("items", "textures/(item|custom/item)/.+\\.png");
        rule("items", ITEM_MODEL + ".+\\.json");
    }

    /** Kategorien, deren Modelle/Blockstates auf weitere Dateien im Pack verweisen koennen. */
    private static final Set<String> WITH_REFERENCES = Set.of("items", "totems", "gaps", "xp_bottles", "pearls", "potions", "wind_charges",
        "shield", "elytra", "swords", "axes", "mace", "spears", "bows", "cobwebs", "blocks", "buckets", "food", "snow", "deepslate", "stone",
        "ores", "grass");

    /** Eigene Himmel-Formate (brauchen eine Himmel-Mod wie Skyboxify oder Nuit). */
    private static final List<String> SKY_NAMESPACES = List.of("fabricskyboxes", "celestial", "nuit");

    /** Alte Dateinamen -> aktuelle Namen. */
    private static final Map<String, String> RENAMES = Map.of(
        "textures/block/grass.png", "textures/block/short_grass.png",
        "textures/misc/enchanted_item_glint.png", "textures/misc/enchanted_glint_item.png",
        "textures/misc/enchanted_glint_entity.png", "textures/misc/enchanted_glint_armor.png",
        "textures/entity/elytra.png", "textures/entity/equipment/wings/elytra.png");

    /** Altes Ruestungsformat (bis 1.21.1) -> neues Format (ab 1.21.2). */
    private static final Pattern OLD_ARMOR = Pattern.compile("textures/models/armor/(\\w+?)_layer_([12])(_overlay)?\\.png");
    private static final Map<String, String> ARMOR_NAMES = Map.of("gold", "gold", "chainmail", "chainmail", "diamond", "diamond", "iron", "iron",
        "leather", "leather", "netherite", "netherite", "turtle", "turtle_scute");

    private static final Pattern ITEM_DEF = Pattern.compile("items/(.+)\\.json");
    private static final Pattern REF_PATH = Pattern.compile("[a-z0-9_./-]+");
    private static final Pattern REF_NS = Pattern.compile("[a-z0-9_.-]+");
    private static final Pattern NS_PREFIX = Pattern.compile("([a-z0-9_.-]+):.*");

    /** Was Minecraft selbst mitbringt (einmal gelesen). */
    private static @Nullable Set<String> vanillaItems;
    private static @Nullable Set<String> vanillaModels;
    private static @Nullable Set<String> vanillaTextures;
    private static @Nullable Set<String> sharedModels;

    /** Ergebnis eines Imports: Dateien pro Kategorie (Ordnername) und wie viele nirgends hineinpassten. */
    public record Result(String name, String styleId, Map<String, Integer> categories, int unsorted, @Nullable String error) {
    }

    private PackImporter() {
    }

    // ---- Warteschlange: Importe laufen nacheinander im Hintergrund ----

    private static final java.util.concurrent.ExecutorService EXECUTOR = java.util.concurrent.Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "ArneTweaks Pack-Import");
        thread.setDaemon(true);
        return thread;
    });
    private static final Map<Path, Result> RESULTS = new java.util.concurrent.ConcurrentHashMap<>();
    private static final Set<Path> QUEUED = java.util.concurrent.ConcurrentHashMap.newKeySet();
    /** Packs, die ins Fenster gezogen wurden (liegen nicht im Import-Ordner). */
    private static final Set<Path> DROPPED = new java.util.concurrent.CopyOnWriteArraySet<>();
    private static volatile @Nullable Path running;
    private static volatile boolean finished;

    /** Pack zum Importieren einreihen. Nach dem Import werden die Styles neu eingelesen. */
    public static void enqueue(Path pack) {
        if (!QUEUED.add(pack)) {
            return;
        }
        RESULTS.remove(pack);
        EXECUTOR.execute(() -> {
            running = pack;
            try {
                RESULTS.put(pack, importPack(pack));
            } finally {
                running = null;
                QUEUED.remove(pack);
                StyleRegistry.reload();
                finished = true;
            }
        });
    }

    public static void addDropped(Path pack) {
        DROPPED.add(pack);
    }

    public static @Nullable Result result(Path pack) {
        return RESULTS.get(pack);
    }

    public static boolean isQueued(Path pack) {
        return QUEUED.contains(pack);
    }

    public static @Nullable Path running() {
        return running;
    }

    /** true (einmal), wenn seit dem letzten Aufruf ein Import fertig geworden ist. */
    public static boolean pollFinished() {
        boolean value = finished;
        finished = false;
        return value;
    }

    /** Alle bekannten Packs: Import-Ordner und ins Fenster gezogene. */
    public static List<Path> packs() {
        List<Path> list = new ArrayList<>(listImportable());
        for (Path pack : DROPPED) {
            if (!list.contains(pack) && Files.exists(pack)) {
                list.add(pack);
            }
        }
        return list;
    }

    /** Entfernte Packs (zum Wiederherstellen), unsichtbar im Import-Ordner. */
    public static Path removedDir() {
        return importDir().resolve(".removed");
    }

    /**
     * Pack aus der Liste nehmen. Liegt es im Import-Ordner, wird es nach .removed verschoben (wiederherstellbar),
     * ins Fenster gezogene Packs bleiben, wo sie sind, und werden nur aus der Liste genommen.
     */
    public static void remove(Path pack) {
        if (QUEUED.contains(pack) || pack.equals(running)) {
            return;
        }
        DROPPED.remove(pack);
        RESULTS.remove(pack);
        if (pack.getParent() != null && pack.getParent().equals(importDir())) {
            try {
                Files.createDirectories(removedDir());
                Files.move(pack, removedDir().resolve(pack.getFileName().toString()), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                ArneStyles.LOGGER.warn("Pack {} konnte nicht entfernt werden", pack, e);
            }
        }
    }

    public static int removedCount() {
        Path dir = removedDir();
        if (!Files.isDirectory(dir)) {
            return 0;
        }
        try (Stream<Path> files = Files.list(dir)) {
            return (int) files.count();
        } catch (IOException e) {
            return 0;
        }
    }

    /** Alle entfernten Packs zurueck in den Import-Ordner (vorhandene gleichnamige bleiben). */
    public static void restoreAll() {
        Path dir = removedDir();
        if (!Files.isDirectory(dir)) {
            return;
        }
        try (Stream<Path> files = Files.list(dir)) {
            for (Path file : files.toList()) {
                Path target = importDir().resolve(file.getFileName().toString());
                if (!Files.exists(target)) {
                    Files.move(file, target);
                }
            }
        } catch (IOException e) {
            ArneStyles.LOGGER.warn("Packs konnten nicht wiederhergestellt werden", e);
        }
    }

    /** Ordner, in den man Packs zum Importieren legt. */
    public static Path importDir() {
        return FabricLoader.getInstance().getConfigDir().resolve("arnestyles").resolve("import");
    }

    /** Packs im Import-Ordner: .zip-Dateien und Ordner. */
    public static List<Path> listImportable() {
        Path dir = importDir();
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(dir)) {
            return files.filter(PackImporter::isPack)
                .sorted(Comparator.comparing(p -> p.getFileName().toString().toLowerCase(Locale.ROOT)))
                .toList();
        } catch (IOException e) {
            return List.of();
        }
    }

    public static boolean isPack(Path path) {
        String name = path.getFileName().toString();
        if (name.startsWith(".") || name.equalsIgnoreCase("LIES MICH.txt")) {
            return false;
        }
        return Files.isDirectory(path) || name.toLowerCase(Locale.ROOT).endsWith(".zip");
    }

    /** Anzeigename aus dem Datei-/Ordnernamen: ohne .zip und ohne Farbcodes. */
    public static String displayName(Path pack) {
        String name = pack.getFileName().toString();
        if (name.toLowerCase(Locale.ROOT).endsWith(".zip")) {
            name = name.substring(0, name.length() - 4);
        }
        name = name.replaceAll("§.", "").replace('_', ' ').trim();
        return name.isEmpty() ? "Pack" : name;
    }

    /** Ordnername des Styles: klein, nur Buchstaben, Zahlen und Unterstriche. */
    public static String styleId(String name) {
        String id = name.toLowerCase(Locale.ROOT)
            .replace("ä", "ae").replace("ö", "oe").replace("ü", "ue").replace("ß", "ss")
            .replaceAll("[^a-z0-9]+", "_")
            .replaceAll("^_+|_+$", "");
        if (id.isEmpty() || id.equals("default")) {
            id = "pack_" + id;
        }
        return id;
    }

    /** Importiert ein Pack. Laeuft lange (Dateien kopieren), deshalb nicht im Render-Thread aufrufen. */
    public static Result importPack(Path pack) {
        String name = displayName(pack);
        String id = styleId(name);
        try {
            loadVanilla();
            if (Files.isDirectory(pack)) {
                String tooBig = checkSize(pack);
                return tooBig != null ? new Result(name, id, Map.of(), 0, tooBig) : sort(pack, StyleRegistry.stylesRoot(), id, name);
            }
            try (FileSystem zip = FileSystems.newFileSystem(pack)) {
                String tooBig = checkSize(zip.getPath("/"));
                return tooBig != null ? new Result(name, id, Map.of(), 0, tooBig) : sort(zip.getPath("/"), StyleRegistry.stylesRoot(), id, name);
            }
        } catch (Exception e) {
            ArneStyles.LOGGER.warn("Pack {} konnte nicht importiert werden", pack, e);
            return new Result(name, id, Map.of(), 0, e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    /** Fehlertext, wenn das Pack zu gross ist oder zu viele Dateien hat, sonst null. */
    private static @Nullable String checkSize(Path root) throws IOException {
        long bytes = 0;
        int count = 0;
        try (Stream<Path> walk = Files.walk(root)) {
            for (Path p : (Iterable<Path>) walk::iterator) {
                if (!Files.isRegularFile(p, java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
                    continue;
                }
                bytes += Files.size(p);
                if (++count > MAX_PACK_FILES || bytes > MAX_PACK_BYTES) {
                    return "Pack zu gross (hoechstens " + (MAX_PACK_BYTES >> 20) + " MB und " + MAX_PACK_FILES + " Dateien)";
                }
            }
        }
        return null;
    }

    private static boolean allowedFile(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot > 0 && ALLOWED.contains(fileName.substring(dot + 1).toLowerCase(Locale.ROOT));
    }

    /** Ziel muss im Zielordner bleiben (kein "../" aus einem praeparierten Pack). */
    private static Path inside(Path base, Path target) throws IOException {
        Path b = base.toAbsolutePath().normalize();
        Path t = target.toAbsolutePath().normalize();
        if (!t.startsWith(b)) {
            throw new IOException("Ungueltiger Pfad im Pack: " + target);
        }
        return t;
    }

    // ---- Vanilla-Infos ----

    /** Liest einmal, welche Items und Modelle Minecraft hat und welche Modelle Eltern mehrerer anderer sind. */
    private static synchronized void loadVanilla() {
        if (sharedModels != null) {
            return;
        }
        Set<String> items = new HashSet<>();
        Set<String> models = new HashSet<>();
        Set<String> textures = new HashSet<>();
        Map<String, Integer> parents = new HashMap<>();
        var pack = MinecraftClient.getInstance().getDefaultResourcePack();
        pack.findResources(ResourceType.CLIENT_RESOURCES, "minecraft", "textures", (id, supplier) -> textures.add(id.getPath()));
        pack.findResources(ResourceType.CLIENT_RESOURCES, "minecraft", "items", (id, supplier) -> {
            String path = id.getPath();
            if (path.endsWith(".json")) {
                items.add(path.substring("items/".length(), path.length() - 5));
            }
        });
        pack.findResources(ResourceType.CLIENT_RESOURCES, "minecraft", "models", (id, supplier) -> {
            String path = id.getPath();
            if (!path.endsWith(".json")) {
                return;
            }
            models.add(path.substring("models/".length(), path.length() - 5));
            try (InputStream in = supplier.get()) {
                JsonElement json = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                if (json.isJsonObject() && json.getAsJsonObject().get("parent") instanceof JsonPrimitive parent) {
                    String pid = stripMinecraft(parent.getAsString());
                    parents.merge(pid, 1, Integer::sum);
                }
            } catch (Exception e) {
                // einzelnes kaputtes Modell ist egal
            }
        });
        Set<String> shared = new HashSet<>();
        parents.forEach((pid, n) -> {
            if (n >= 2 && !pid.startsWith("builtin/")) {
                shared.add(pid);
            }
        });
        vanillaItems = items;
        vanillaModels = models;
        vanillaTextures = textures;
        sharedModels = shared;
        ArneStyles.LOGGER.info("Pack-Import: {} Items, {} Modelle, {} geteilte Eltern-Modelle aus Minecraft", items.size(), models.size(), shared.size());
    }

    private static String stripMinecraft(String value) {
        return value.startsWith("minecraft:") ? value.substring("minecraft:".length()) : value;
    }

    // ---- Sortieren ----

    public static @Nullable String categoryOf(String rel) {
        for (Rule rule : RULES) {
            if (rule.pattern().matcher(rel).matches()) {
                return rule.category();
            }
        }
        return null;
    }

    private static @Nullable String modelId(String rel) {
        return rel.startsWith("models/") && rel.endsWith(".json") ? rel.substring("models/".length(), rel.length() - 5) : null;
    }

    private static String isolatedId(String category, String modelId) {
        return "arnestyles/" + category + "/" + modelId;
    }

    /** Ordner mit assets/ finden (ZIPs haben das Pack oft in einem Unterordner, Mac-Reste ignorieren). */
    private static @Nullable Path findRoot(Path pack) throws IOException {
        if (Files.isDirectory(pack.resolve("assets"))) {
            return pack;
        }
        try (Stream<Path> walk = Files.walk(pack, 3)) {
            return walk.filter(p -> Files.isDirectory(p) && name(p).equals("assets") && !p.toString().contains("__MACOSX"))
                .sorted(Comparator.comparingInt(Path::getNameCount))
                .map(Path::getParent)
                .findFirst()
                .orElse(null);
        }
    }

    private static String name(Path path) {
        Path file = path.getFileName();
        if (file == null) {
            return "";
        }
        String name = file.toString();
        return name.endsWith("/") ? name.substring(0, name.length() - 1) : name;
    }

    private static Path at(Path mc, String rel) {
        return mc.resolve(rel).normalize();
    }

    private static Result sort(Path pack, Path stylesRoot, String styleId, String name) throws IOException {
        Path root = findRoot(pack);
        if (root == null) {
            return new Result(name, styleId, Map.of(), 0, "kein assets-Ordner");
        }
        Path mc = root.resolve("assets").resolve("minecraft");
        List<String> files = new ArrayList<>();
        if (Files.isDirectory(mc)) {
            try (Stream<Path> walk = Files.walk(mc)) {
                walk.filter(Files::isRegularFile).forEach(p -> {
                    String rel = mc.relativize(p).toString().replace('\\', '/');
                    if (!rel.endsWith(".mcmeta")) {
                        files.add(rel);
                    }
                });
            }
        }
        java.util.Collections.sort(files);
        Set<String> fileSet = new HashSet<>(files);

        Map<String, Set<String>> byCat = new TreeMap<>();
        int unsorted = 0;
        boolean hasOptifineSky = Files.isDirectory(mc.resolve("optifine").resolve("sky"));
        for (String rel : files) {
            String fileName = rel.substring(rel.lastIndexOf('/') + 1);
            if ((rel.startsWith("mcpatcher/sky/") && hasOptifineSky) || fileName.equals(".DS_Store") || fileName.equals("Thumbs.db")
                || fileName.startsWith("._")) {
                continue;
            }
            Matcher item = ITEM_DEF.matcher(rel);
            if (item.matches() && vanillaItems != null && !vanillaItems.contains(item.group(1))) {
                continue;
            }
            if (sharedModels.contains(String.valueOf(modelId(rel)))) {
                continue;
            }
            String cat = categoryOf(rel);
            if (cat != null) {
                byCat.computeIfAbsent(cat, k -> new TreeSet<>()).add(rel);
            } else {
                unsorted++;
            }
        }

        // Alte Dateinamen auf die aktuellen umstellen
        Map<String, String> renamed = new LinkedHashMap<>();
        RENAMES.forEach((old, now) -> {
            if (fileSet.contains(old) && !fileSet.contains(now)) {
                byCat.computeIfAbsent(categoryOf(now), k -> new TreeSet<>());
                renamed.put(now, old);
            }
        });

        // Alles mitnehmen, worauf Modelle/Blockstates verweisen (eigene Eltern-Modelle, eigene Texturen)
        Map<String, Set<String>> isolated = new HashMap<>();
        for (String cat : new ArrayList<>(byCat.keySet())) {
            if (!WITH_REFERENCES.contains(cat)) {
                continue;
            }
            Set<String> own = byCat.get(cat);
            List<String> todo = new ArrayList<>(own.stream().filter(r -> r.endsWith(".json")).toList());
            Set<String> seen = new HashSet<>(todo);
            while (!todo.isEmpty()) {
                for (String ref : references(mc, todo.remove(todo.size() - 1))) {
                    if (!seen.add(ref)) {
                        continue;
                    }
                    String mid = modelId(ref);
                    if (mid != null && sharedModels.contains(mid)) {
                        isolated.computeIfAbsent(cat, k -> new TreeSet<>()).add(ref);
                    } else {
                        String other = categoryOf(ref);
                        if (other != null && !other.equals(cat) && !ref.endsWith(".json") && vanillaTextures.contains(ref)) {
                            // Minecraft-Textur einer anderen Kategorie: nicht mitnehmen, sonst aendert dieser Style auch die andere.
                            // Eigene Texturen des Packs (gibt es in Minecraft nicht) kommen mit, sonst fehlen sie dem Modell.
                            continue;
                        }
                        own.add(ref);
                    }
                    if (ref.endsWith(".json")) {
                        todo.add(ref);
                    }
                }
            }
        }

        // Item-Definitionen, deren Modelle es nirgends gibt, weglassen: Minecraft nimmt dann seine eigene
        for (Set<String> rels : byCat.values()) {
            rels.removeIf(rel -> rel.startsWith("items/") && !unresolvedModels(mc, rel).isEmpty());
        }

        // Alte Ruestung umrechnen
        Map<String, String> converted = new LinkedHashMap<>();
        for (String rel : files) {
            Matcher m = OLD_ARMOR.matcher(rel);
            if (!m.matches() || !ARMOR_NAMES.containsKey(m.group(1))) {
                continue;
            }
            String folder = m.group(2).equals("1") ? "humanoid" : "humanoid_leggings";
            String now = "textures/entity/equipment/" + folder + "/" + ARMOR_NAMES.get(m.group(1)) + (m.group(3) == null ? "" : m.group(3)) + ".png";
            if (!byCat.getOrDefault("armor", Set.of()).contains(now) && !Files.isRegularFile(at(mc, now))) {
                converted.put(now, rel);
            }
        }

        List<String> namespaces = skyNamespaces(root);
        Set<String> cats = new TreeSet<>(byCat.keySet());
        if (!converted.isEmpty()) {
            cats.add("armor");
        }
        if (!namespaces.isEmpty()) {
            cats.add("sky");
        }
        boolean skyOnly = Set.of("sky", "sounds").containsAll(cats);

        Map<String, Integer> summary = new LinkedHashMap<>();
        for (String cat : cats) {
            if (category(cat) == null) {
                continue;
            }
            Path dest = stylesRoot.resolve(cat).resolve(styleId);
            deleteTree(dest);
            Map<String, String> mapping = new HashMap<>();
            for (String r : isolated.getOrDefault(cat, Set.of())) {
                mapping.put(modelId(r), isolatedId(cat, modelId(r)));
            }
            for (String rel : byCat.getOrDefault(cat, Set.of())) {
                copy(mc, rel, dest, rel, mapping);
            }
            for (String rel : isolated.getOrDefault(cat, Set.of())) {
                copy(mc, rel, dest, "models/" + isolatedId(cat, modelId(rel)) + ".json", mapping);
            }
            if (cat.equals("armor")) {
                for (var entry : converted.entrySet()) {
                    copy(mc, entry.getValue(), dest, entry.getKey(), null);
                }
            }
            for (var entry : renamed.entrySet()) {
                if (cat.equals(categoryOf(entry.getKey()))) {
                    copy(mc, entry.getValue(), dest, entry.getKey(), null);
                }
            }
            if (cat.equals("sky")) {
                for (String ns : namespaces) {
                    copyTree(root.resolve("assets").resolve(ns), dest.resolve("assets").resolve(ns));
                }
                Files.createDirectories(dest.resolve("assets").resolve("minecraft"));
                skyPreview(dest, root, skyOnly);
            }
            JsonObject style = new JsonObject();
            style.addProperty("name", name);
            style.addProperty("imported", true);
            Files.writeString(dest.resolve("style.json"), GSON.toJson(style), StandardCharsets.UTF_8);
            try (Stream<Path> walk = Files.walk(dest)) {
                summary.put(cat, (int) walk.filter(Files::isRegularFile).count() - 1);
            }
        }
        ArneStyles.LOGGER.info("Pack-Import {}: {} ({} Dateien ohne Kategorie)", styleId, summary, unsorted);
        return new Result(name, styleId, summary, unsorted, null);
    }

    public static @Nullable StyleCategory category(String folder) {
        for (StyleCategory category : StyleCategory.values()) {
            if (category.folderName().equals(folder)) {
                return category;
            }
        }
        return null;
    }

    private static void strings(@Nullable JsonElement value, List<String> out) {
        if (value == null) {
            return;
        }
        if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
            out.add(value.getAsString());
        } else if (value.isJsonObject()) {
            for (var entry : value.getAsJsonObject().entrySet()) {
                strings(entry.getValue(), out);
            }
        } else if (value.isJsonArray()) {
            for (JsonElement element : value.getAsJsonArray()) {
                strings(element, out);
            }
        }
    }

    private static @Nullable JsonElement loadJson(Path file) {
        try {
            String text = Files.readString(file, StandardCharsets.UTF_8);
            if (text.startsWith("﻿")) {
                text = text.substring(1);
            }
            return JsonParser.parseString(text);
        } catch (Exception e) {
            return null;
        }
    }

    /** Modelle und Texturen aus dem Pack, auf die eine JSON-Datei verweist. */
    private static List<String> references(Path mc, String rel) {
        List<String> values = new ArrayList<>();
        strings(loadJson(at(mc, rel)), values);
        List<String> found = new ArrayList<>();
        for (String s : values) {
            String ns = "minecraft";
            String path = s;
            int colon = s.indexOf(':');
            if (colon >= 0) {
                ns = s.substring(0, colon);
                path = s.substring(colon + 1);
            }
            if (!REF_PATH.matcher(path).matches() || !REF_NS.matcher(ns).matches()) {
                continue;
            }
            String prefix = ns.equals("minecraft") ? "" : "../" + ns + "/";
            for (String candidate : new String[] {prefix + "models/" + path + ".json", prefix + "textures/" + path + ".png"}) {
                if (Files.isRegularFile(at(mc, candidate))) {
                    found.add(candidate);
                }
            }
        }
        return found;
    }

    private static void modelRefs(@Nullable JsonElement value, List<String> out) {
        if (value == null) {
            return;
        }
        if (value.isJsonObject()) {
            for (var entry : value.getAsJsonObject().entrySet()) {
                JsonElement v = entry.getValue();
                if ((entry.getKey().equals("model") || entry.getKey().equals("base")) && v.isJsonPrimitive() && v.getAsJsonPrimitive().isString()) {
                    out.add(v.getAsString());
                } else {
                    modelRefs(v, out);
                }
            }
        } else if (value.isJsonArray()) {
            for (JsonElement element : value.getAsJsonArray()) {
                modelRefs(element, out);
            }
        }
    }

    /** Modell-Verweise einer items/*.json, die weder im Pack noch in Minecraft existieren. */
    private static List<String> unresolvedModels(Path mc, String rel) {
        List<String> refs = new ArrayList<>();
        modelRefs(loadJson(at(mc, rel)), refs);
        List<String> missing = new ArrayList<>();
        for (String value : refs) {
            String ns = "minecraft";
            String path = value;
            int colon = value.indexOf(':');
            if (colon >= 0) {
                ns = value.substring(0, colon);
                path = value.substring(colon + 1);
            }
            String prefix = ns.equals("minecraft") ? "" : "../" + ns + "/";
            boolean inPack = Files.isRegularFile(at(mc, prefix + "models/" + path + ".json"));
            boolean inVanilla = ns.equals("minecraft") && vanillaModels != null && vanillaModels.contains(path);
            if (!inPack && !inVanilla) {
                missing.add(value);
            }
        }
        return missing;
    }

    private static JsonElement rewriteRefs(JsonElement value, Map<String, String> mapping) {
        if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
            String key = stripMinecraft(value.getAsString());
            return mapping.containsKey(key) ? new JsonPrimitive("minecraft:" + mapping.get(key)) : value;
        }
        if (value.isJsonObject()) {
            JsonObject copy = new JsonObject();
            for (var entry : value.getAsJsonObject().entrySet()) {
                copy.add(entry.getKey(), rewriteRefs(entry.getValue(), mapping));
            }
            return copy;
        }
        if (value.isJsonArray()) {
            JsonArray copy = new JsonArray();
            for (JsonElement element : value.getAsJsonArray()) {
                copy.add(rewriteRefs(element, mapping));
            }
            return copy;
        }
        return value;
    }

    private static void copy(Path mc, String rel, Path destRoot, String destRel, @Nullable Map<String, String> mapping) throws IOException {
        if (!allowedFile(destRel) || !allowedFile(rel)) {
            return;
        }
        Path source = inside(mc, at(mc, rel));
        Path target = inside(destRoot, destRoot.resolve("assets").resolve("minecraft").resolve(destRel));
        if (!Files.isRegularFile(source, java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
            return;
        }
        Files.createDirectories(target.getParent());
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        if (rel.endsWith(".json")) {
            JsonElement data = loadJson(target);
            boolean changed = false;
            // "builtin/entity" gibt es seit 1.21.4 nicht mehr: Verweis entfernen, Anzeige-Werte behalten
            if (data != null && data.isJsonObject() && data.getAsJsonObject().get("parent") instanceof JsonPrimitive parent
                && stripMinecraft(parent.getAsString()).equals("builtin/entity")) {
                data.getAsJsonObject().remove("parent");
                changed = true;
            }
            if (mapping != null && !mapping.isEmpty() && data != null) {
                JsonElement rewritten = rewriteRefs(data, mapping);
                changed = changed || !rewritten.equals(data);
                data = rewritten;
            }
            if (changed) {
                Files.writeString(target, GSON.toJson(data), StandardCharsets.UTF_8);
            }
        }
        Path meta = source.resolveSibling(source.getFileName().toString() + ".mcmeta");
        if (Files.isRegularFile(meta)) {
            Files.copy(meta, target.resolveSibling(target.getFileName().toString() + ".mcmeta"), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /** Ordner kopieren (auch aus einer ZIP), Mac-Reste weglassen. */
    private static void copyTree(Path from, Path to) throws IOException {
        try (Stream<Path> walk = Files.walk(from)) {
            for (Path p : walk.toList()) {
                String fileName = name(p);
                if (fileName.startsWith("._") || fileName.equals(".DS_Store") || fileName.equals("Thumbs.db")) {
                    continue;
                }
                if (Files.isSymbolicLink(p)) {
                    continue;
                }
                Path target = inside(to, to.resolve(from.relativize(p).toString().replace('\\', '/')));
                if (Files.isDirectory(p)) {
                    Files.createDirectories(target);
                } else if (allowedFile(fileName)) {
                    Files.createDirectories(target.getParent());
                    Files.copy(p, target, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private static void deleteTree(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(dir)) {
            for (Path p : walk.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(p);
            }
        }
    }

    /** Himmel-Namespaces (fabricskyboxes, celestial ...) und alle Namespaces, auf die deren JSON verweist. */
    private static List<String> skyNamespaces(Path root) throws IOException {
        Path assets = root.resolve("assets");
        Set<String> found = new TreeSet<>();
        for (String ns : SKY_NAMESPACES) {
            if (Files.isDirectory(assets.resolve(ns))) {
                found.add(ns);
            }
        }
        for (String ns : List.copyOf(found)) {
            try (Stream<Path> walk = Files.walk(assets.resolve(ns))) {
                for (Path f : walk.filter(p -> p.toString().endsWith(".json")).toList()) {
                    List<String> values = new ArrayList<>();
                    strings(loadJson(f), values);
                    for (String s : values) {
                        Matcher m = NS_PREFIX.matcher(s);
                        if (m.matches() && !m.group(1).equals("minecraft") && !m.group(1).equals("realms") && Files.isDirectory(assets.resolve(m.group(1)))) {
                            found.add(m.group(1));
                        }
                    }
                }
            }
        }
        return new ArrayList<>(found);
    }

    /** preview.png fuer Himmel: bei reinen Himmel-Packs das pack.png, sonst das groesste Himmel-Bild. */
    private static void skyPreview(Path dest, Path root, boolean skyOnly) throws IOException {
        Path target = dest.resolve("preview.png");
        Path packPng = root.resolve("pack.png");
        if (skyOnly && Files.isRegularFile(packPng)) {
            Files.copy(packPng, target, StandardCopyOption.REPLACE_EXISTING);
            return;
        }
        List<Path> candidates = new ArrayList<>();
        List<String> subs = new ArrayList<>(List.of("assets/minecraft/optifine/sky", "assets/minecraft/mcpatcher/sky"));
        for (String ns : SKY_NAMESPACES) {
            subs.add("assets/" + ns);
        }
        for (String sub : subs) {
            Path folder = dest.resolve(sub);
            if (Files.isDirectory(folder)) {
                try (Stream<Path> walk = Files.walk(folder)) {
                    candidates.addAll(walk.filter(p -> p.toString().endsWith(".png")).toList());
                }
            }
        }
        candidates.sort(Comparator.comparingLong(p -> {
            try {
                return -Files.size(p);
            } catch (IOException e) {
                return 0L;
            }
        }));
        if (!candidates.isEmpty()) {
            Files.copy(candidates.get(0), target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
