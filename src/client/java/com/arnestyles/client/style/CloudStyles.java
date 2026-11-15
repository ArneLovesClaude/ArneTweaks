package com.arnestyles.client.style;

import com.arnestyles.ArneStyles;
import com.arnestyles.client.config.ArneStylesConfig;
import com.arnestyles.client.config.ConfigManager;
import com.arnestyles.client.config.StyleCategory;
import com.arnestyles.client.gui.StylesScreen;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Die eingebauten Styles liegen nicht mehr in der Mod-Datei, sondern als ZIPs in einem GitHub-Release.
 * index.json listet pro Kategorie eine ZIP-Datei (mit SHA-256). Eine Kategorie wird erst geladen, wenn
 * sie gebraucht wird (Menue geoeffnet oder Style aktiv), und danach in config/arnestyles/cloud/ aufgehoben.
 * Sehr grosse Kategorien (Himmel) haben im Kategorie-ZIP nur die Vorschau; jeder Style hat ein eigenes ZIP.
 *
 * Sicherheit: Es werden nur Dateien mit bekannter SHA-256 angenommen, nur harmlose Dateitypen entpackt
 * und nie ausserhalb des Cache-Ordners geschrieben.
 */
public final class CloudStyles {
    /** Standard-Quelle. Zum Testen mit der Umgebungsvariable ARNESTYLES_CLOUD_URL ueberschreiben. */
    private static final String DEFAULT_URL = "https://github.com/ArneLovesClaude/arnetweaks-styles/releases/latest/download/";
    private static final Set<String> ALLOWED = Set.of("png", "json", "mcmeta", "properties", "ogg", "txt");
    private static final long MAX_DOWNLOAD = 64L * 1024 * 1024;
    private static final long MAX_UNPACKED = 256L * 1024 * 1024;
    private static final String BUNDLE_MARKER = ".bundle";
    private static final String FULL_MARKER = ".full";

    private enum State { LOADING, FAILED }

    private static final Map<String, State> STATES = new ConcurrentHashMap<>();
    private static final Map<String, Integer> PROGRESS = new ConcurrentHashMap<>();
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "ArneTweaks Style-Download");
        thread.setDaemon(true);
        return thread;
    });
    private static final HttpClient HTTP = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NORMAL)
        .connectTimeout(Duration.ofSeconds(6))
        .build();

    private static volatile @Nullable JsonObject index;
    private static volatile int generation;
    private static volatile boolean activeChanged;
    private static int seenGeneration;

    private CloudStyles() {
    }

    public static Path root() {
        return FabricLoader.getInstance().getConfigDir().resolve("arnestyles").resolve("cloud");
    }

    private static String baseUrl() {
        String url = System.getenv("ARNESTYLES_CLOUD_URL");
        if (url == null || url.isBlank()) {
            url = DEFAULT_URL;
        }
        return url.endsWith("/") ? url : url + "/";
    }

    /**
     * Beim Spielstart (vor dem ersten Laden der Ressourcen): Index holen und die Kategorien der aktiven Styles
     * direkt laden, damit sie sofort greifen. Ohne Internet wird mit dem bisherigen Cache weitergemacht.
     */
    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(CloudStyles::tick);
        loadIndex();
        ArneStylesConfig config = ConfigManager.get();
        for (StyleCategory category : StyleCategory.values()) {
            String selected = config.getSelectedStyle(category);
            if (category == StyleCategory.ESSENTIALS || ArneStylesConfig.DEFAULT_STYLE.equals(selected)) {
                continue;
            }
            try {
                ensureCategory(category);
                ensureStyle(category, selected);
            } catch (IOException | RuntimeException e) {
                ArneStyles.LOGGER.warn("Style {} / {} konnte beim Start nicht geladen werden: {}", category.folderName(), selected, e.toString());
            }
        }
        activeChanged = false;
        seenGeneration = generation;
    }

    /** Laedt eine Kategorie im Hintergrund, falls sie fehlt oder veraltet ist. */
    public static void request(StyleCategory category) {
        if (category == StyleCategory.ESSENTIALS || !needsCategory(category)) {
            return;
        }
        start(category.folderName(), () -> ensureCategory(category));
    }

    /** Laedt einen einzelnen grossen Style (Himmel) im Hintergrund. */
    public static void requestStyle(StyleDefinition style) {
        if (isReady(style)) {
            return;
        }
        start(style.category().folderName() + "/" + style.id(), () -> ensureStyle(style.category(), style.id()));
    }

    /** Erneut versuchen (F5 im Menue): fehlgeschlagene Downloads vergessen und Index neu holen. */
    public static void retry(StyleCategory category) {
        STATES.values().removeIf(state -> state == State.FAILED);
        WORKER.submit(CloudStyles::loadIndex);
        request(category);
    }

    private interface Job {
        void run() throws IOException;
    }

    private static void start(String key, Job job) {
        if (STATES.putIfAbsent(key, State.LOADING) != null) {
            return;
        }
        PROGRESS.put(key, 0);
        WORKER.submit(() -> {
            try {
                if (index == null) {
                    loadIndex();
                }
                job.run();
                STATES.remove(key);
            } catch (IOException | RuntimeException e) {
                ArneStyles.LOGGER.warn("Download {} fehlgeschlagen: {}", key, e.toString());
                STATES.put(key, State.FAILED);
            }
            PROGRESS.remove(key);
            generation++;
        });
    }

    /** Statuszeile fuer das Menue, null wenn nichts laeuft. */
    public static @Nullable Text status(StyleCategory category) {
        String prefix = category.folderName();
        for (Map.Entry<String, State> entry : STATES.entrySet()) {
            String key = entry.getKey();
            if (!key.equals(prefix) && !key.startsWith(prefix + "/")) {
                continue;
            }
            if (entry.getValue() == State.FAILED) {
                return Text.translatable("arnestyles.cloud.failed");
            }
            return Text.translatable("arnestyles.cloud.loading", PROGRESS.getOrDefault(key, 0));
        }
        return null;
    }

    /** Wird hochgezaehlt, sobald ein Download fertig ist. Das Menue liest dann die Styles neu ein. */
    public static int generation() {
        return generation;
    }

    /** true, wenn seit dem letzten Aufruf ein gerade aktiver Style fertig geladen wurde (Ressourcen neu laden). */
    public static boolean consumeActiveChanged() {
        boolean changed = activeChanged;
        activeChanged = false;
        return changed;
    }

    /** Ist der Style vollstaendig da? Eigene Styles immer, Himmel erst nach seinem eigenen Download. */
    public static boolean isReady(StyleDefinition style) {
        if (!style.builtIn() || style.root() == null) {
            return true;
        }
        if (perStyle(style.category()) == null) {
            return true;
        }
        return expectedStyleSha(style.category(), style.id()) == null
            ? Files.isDirectory(style.root().resolve("assets"))
            : Files.isRegularFile(style.root().resolve(FULL_MARKER));
    }

    /** Ausserhalb des Menues: fertige Downloads uebernehmen und bei Bedarf die Ressourcen neu laden. */
    private static void tick(MinecraftClient client) {
        if (seenGeneration == generation || client.currentScreen instanceof StylesScreen) {
            return;
        }
        seenGeneration = generation;
        StyleRegistry.reload();
        if (consumeActiveChanged()) {
            client.reloadResources();
        }
    }

    // ---- Laden ----

    private static synchronized void loadIndex() {
        Path cached = root().resolve("index.json");
        try {
            byte[] data = download(baseUrl() + "index.json", null, 2L * 1024 * 1024, null);
            JsonObject fresh = JsonParser.parseString(new String(data, StandardCharsets.UTF_8)).getAsJsonObject();
            Files.createDirectories(root());
            Files.write(cached, data);
            index = fresh;
            return;
        } catch (IOException | RuntimeException e) {
            ArneStyles.LOGGER.warn("Style-Index konnte nicht geladen werden ({}), nutze den Cache.", e.toString());
        }
        if (Files.isRegularFile(cached)) {
            try {
                index = JsonParser.parseString(Files.readString(cached, StandardCharsets.UTF_8)).getAsJsonObject();
            } catch (IOException | RuntimeException e) {
                ArneStyles.LOGGER.warn("Gespeicherter Style-Index ist kaputt.", e);
            }
        }
    }

    private static @Nullable JsonObject categoryInfo(StyleCategory category) {
        JsonObject current = index;
        if (current == null || !current.has("categories")) {
            return null;
        }
        JsonElement info = current.getAsJsonObject("categories").get(category.folderName());
        return info != null && info.isJsonObject() ? info.getAsJsonObject() : null;
    }

    private static @Nullable JsonObject perStyle(StyleCategory category) {
        JsonObject info = categoryInfo(category);
        return info != null && info.has("styles") ? info.getAsJsonObject("styles") : null;
    }

    private static @Nullable String expectedStyleSha(StyleCategory category, String id) {
        JsonObject styles = perStyle(category);
        JsonElement style = styles == null ? null : styles.get(id);
        return style != null && style.isJsonObject() ? style.getAsJsonObject().get("sha256").getAsString() : null;
    }

    private static boolean needsCategory(StyleCategory category) {
        JsonObject info = categoryInfo(category);
        if (info == null) {
            // Noch kein Index (z. B. offline): nur laden, wenn gar nichts da ist.
            return !Files.isDirectory(root().resolve(category.folderName()));
        }
        return !info.get("sha256").getAsString().equals(readMarker(root().resolve(category.folderName()).resolve(BUNDLE_MARKER)));
    }

    private static void ensureCategory(StyleCategory category) throws IOException {
        JsonObject info = categoryInfo(category);
        if (info == null || !needsCategory(category)) {
            return;
        }
        String sha = info.get("sha256").getAsString();
        String key = category.folderName();
        byte[] zip = download(baseUrl() + info.get("file").getAsString(), sha, MAX_DOWNLOAD, key);
        Path target = root().resolve(category.folderName());
        Path temp = root().resolve(".tmp-" + category.folderName());
        deleteTree(temp);
        unzip(zip, temp);
        Files.writeString(temp.resolve(BUNDLE_MARKER), sha, StandardCharsets.UTF_8);
        deleteTree(target);
        Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE);
        if (!ArneStylesConfig.DEFAULT_STYLE.equals(ConfigManager.get().getSelectedStyle(category))) {
            activeChanged = true;
        }
        ArneStyles.LOGGER.info("Styles {} geladen ({} KB).", category.folderName(), zip.length / 1024);
    }

    private static void ensureStyle(StyleCategory category, String id) throws IOException {
        String sha = expectedStyleSha(category, id);
        if (sha == null || !isSafeName(id)) {
            return;
        }
        Path target = root().resolve(category.folderName()).resolve(id);
        if (sha.equals(readMarker(target.resolve(FULL_MARKER)))) {
            return;
        }
        String file = perStyle(category).getAsJsonObject(id).get("file").getAsString();
        byte[] zip = download(baseUrl() + file, sha, MAX_DOWNLOAD, category.folderName() + "/" + id);
        Files.createDirectories(target);
        unzip(zip, target);
        Files.writeString(target.resolve(FULL_MARKER), sha, StandardCharsets.UTF_8);
        if (id.equals(ConfigManager.get().getSelectedStyle(category))) {
            activeChanged = true;
        }
        ArneStyles.LOGGER.info("Style {}/{} geladen ({} KB).", category.folderName(), id, zip.length / 1024);
    }

    private static byte[] download(String url, @Nullable String sha, long limit, @Nullable String progressKey) throws IOException {
        if (!url.startsWith("https://") && !url.startsWith("http://")) {
            throw new IOException("Ungueltige Adresse " + url);
        }
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
            .timeout(Duration.ofSeconds(90))
            .header("User-Agent", "ArneTweaks")
            .GET().build();
        HttpResponse<InputStream> response;
        try {
            response = HTTP.send(request, HttpResponse.BodyHandlers.ofInputStream());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Abgebrochen", e);
        }
        if (response.statusCode() != 200) {
            response.body().close();
            throw new IOException("HTTP " + response.statusCode() + " fuer " + url);
        }
        long total = response.headers().firstValueAsLong("Content-Length").orElse(-1L);
        if (total > limit) {
            response.body().close();
            throw new IOException("Datei zu gross: " + url);
        }
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream(total > 0 ? (int) total : 64 * 1024);
        try (InputStream in = response.body()) {
            byte[] buffer = new byte[64 * 1024];
            int read;
            while ((read = in.read(buffer)) >= 0) {
                out.write(buffer, 0, read);
                if (out.size() > limit) {
                    throw new IOException("Datei zu gross: " + url);
                }
                if (progressKey != null && total > 0) {
                    PROGRESS.put(progressKey, (int) (out.size() * 100L / total));
                }
            }
        }
        byte[] data = out.toByteArray();
        if (sha != null && !sha.equalsIgnoreCase(sha256(data))) {
            throw new IOException("Pruefsumme stimmt nicht: " + url);
        }
        return data;
    }

    private static void unzip(byte[] zip, Path target) throws IOException {
        Files.createDirectories(target);
        Path base = target.toAbsolutePath().normalize();
        long unpacked = 0;
        try (ZipInputStream in = new ZipInputStream(new java.io.ByteArrayInputStream(zip))) {
            ZipEntry entry;
            byte[] buffer = new byte[64 * 1024];
            while ((entry = in.getNextEntry()) != null) {
                String name = entry.getName();
                if (entry.isDirectory()) {
                    continue;
                }
                if (name.contains("\\") || name.startsWith("/") || name.contains("..") || name.contains(":")) {
                    throw new IOException("Ungueltiger Pfad im ZIP: " + name);
                }
                int dot = name.lastIndexOf('.');
                String ext = dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
                if (!ALLOWED.contains(ext)) {
                    ArneStyles.LOGGER.warn("Datei {} im Style-ZIP wird uebersprungen.", name);
                    continue;
                }
                Path file = base.resolve(name).normalize();
                if (!file.startsWith(base)) {
                    throw new IOException("Ungueltiger Pfad im ZIP: " + name);
                }
                Files.createDirectories(file.getParent());
                try (OutputStream out = Files.newOutputStream(file)) {
                    int read;
                    while ((read = in.read(buffer)) >= 0) {
                        unpacked += read;
                        if (unpacked > MAX_UNPACKED) {
                            throw new IOException("ZIP entpackt zu gross");
                        }
                        out.write(buffer, 0, read);
                    }
                }
            }
        }
    }

    private static boolean isSafeName(String id) {
        return id.matches("[a-z0-9_.-]+") && !id.contains("..");
    }

    private static @Nullable String readMarker(Path marker) {
        try {
            return Files.isRegularFile(marker) ? Files.readString(marker, StandardCharsets.UTF_8).trim() : null;
        } catch (IOException e) {
            return null;
        }
    }

    private static String sha256(byte[] data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void deleteTree(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(dir)) {
            for (Path path : walk.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(path);
            }
        }
    }
}
