package com.arnestyles.client.mods;

import com.arnestyles.ArneStyles;
import com.arnestyles.client.config.ArneStylesConfig;
import com.arnestyles.client.config.ConfigManager;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.ptr.IntByReference;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.jspecify.annotations.Nullable;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Spotify-Anzeige ohne Login. Quelle 1: Windows-Medien-Schnittstelle ({@link WinMedia}): Song, Kuenstler, Cover,
 * genaue Position, Laenge und Pause, rein lokal. Quelle 2 (falls die fehlt): Fenstertitel der Spotify-App.
 * Lyrics optional von lrclib.net (schickt Kuenstler und Titel dorthin).
 */
public final class SpotifyHud {
    public static final String[] MODES = {"compact", "song_artist", "song_line", "lyrics", "card"};
    public static final int GREEN = 0xFF1DB954;
    private static final int CARD_BG = 0xD8121212;
    private static final int GREY = 0xFFB3B3B3;
    private static final int DIM = 0x80FFFFFF;
    private static final Pattern LRC_TAG = Pattern.compile("\\[(\\d+):(\\d+(?:\\.\\d+)?)]");
    private static final int CACHE_SIZE = 32;
    private static final boolean WINDOWS = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).startsWith("windows");
    /** Cover-Textur (64x64, auf dem Render-Thread hochgeladen). */
    private static final int COVER = 64;
    private static final Identifier COVER_ID = Identifier.of(ArneStyles.MOD_ID, "spotify_cover");
    private static final int MAX_COVER_SIDE = 4096;

    /** Was gerade laeuft. Wird nur vom Spotify-Thread ersetzt, die Anzeige liest nur. durationMs 0 = unbekannt. */
    private record Song(String artist, String title, boolean playing, boolean positionKnown, long startNanos, long pausedMs,
                        long durationMs) {
        long positionMs() {
            return playing ? (System.nanoTime() - startNanos) / 1_000_000L : pausedMs;
        }

        String key() {
            return artist + "\n" + title;
        }
    }

    /** Lyrics mit Zeitstempeln in ms. lines[i] gilt ab times[i]. */
    private record Lyrics(long[] times, String[] lines, long durationMs) {
        int lineAt(long ms) {
            int lo = 0;
            int hi = times.length - 1;
            int found = -1;
            while (lo <= hi) {
                int mid = (lo + hi) >>> 1;
                if (times[mid] <= ms) {
                    found = mid;
                    lo = mid + 1;
                } else {
                    hi = mid - 1;
                }
            }
            return found;
        }
    }

    /** Fertig dekodiertes Cover (ARGB, COVER x COVER) fuer einen Song. */
    private record Cover(String key, int[] argb) {
    }

    private static final Lyrics NONE = new Lyrics(new long[0], new String[0], 0);

    private static volatile @Nullable Song song;
    private static volatile @Nullable Lyrics lyrics;
    private static volatile @Nullable String lyricsKey;
    private static volatile boolean lyricsLoading;
    private static volatile @Nullable Cover pendingCover;
    private static final Map<String, Lyrics> CACHE = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Lyrics> eldest) {
            return size() > CACHE_SIZE;
        }
    };
    private static @Nullable HttpClient http;
    private static @Nullable Thread thread;
    /** Wartender Steuer-Befehl (WinMedia.TOGGLE/NEXT/PREVIOUS), 0 = keiner. Weckt den Spotify-Thread sofort auf. */
    private static volatile int pendingControl;
    private static final Object WAKE = new Object();
    private static Set<Integer> spotifyPids = new HashSet<>();
    private static long pidsCheckedAt;
    /** Windows-Medien-Schnittstelle nicht verfuegbar (dann nur Fenstertitel). */
    private static boolean mediaBroken;
    private static @Nullable String coverRequestedFor;
    /** Fuer welchen Song das Cover geladen ist, wie oft schon gefragt wurde, und Pruefsummen gegen alte Cover. */
    private static @Nullable String coverLoadedKey;
    private static int coverTries;
    private static int previousCoverHash;
    private static int currentCoverHash;

    // Nur Render-Thread: Cache fuer die Anzeige, damit pro Frame keine Strings neu gebaut werden
    private static @Nullable Song drawnSong;
    private static int drawnStamp = Integer.MIN_VALUE;
    private static String[] drawnText = new String[0];
    private static @Nullable Cover uploadedCover;
    private static int timeShownSecond = -1;
    private static String timeText = "";

    private SpotifyHud() {
    }

    public static boolean supported() {
        return WINDOWS;
    }

    private static boolean enabled() {
        return ModSetting.Toggle.SPOTIFY_HUD.isActive();
    }

    /** Startet den Hintergrund-Thread (einmal). Er schlaeft, solange die Anzeige aus ist. */
    public static void register() {
        if (!WINDOWS || thread != null) {
            return;
        }
        Thread t = new Thread(SpotifyHud::loop, "ArneTweaks-Spotify");
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY);
        thread = t;
        t.start();
    }

    private static void loop() {
        boolean fresh = true;
        while (true) {
            try {
                if (!enabled()) {
                    song = null;
                    fresh = true;
                    Thread.sleep(2000);
                    continue;
                }
                int command = pendingControl;
                if (command != 0) {
                    pendingControl = 0;
                    if (WinMedia.control(command)) {
                        // Windows braucht einen Moment, bis Pause/Song-Wechsel in der Abfrage stehen
                        Thread.sleep(250);
                    }
                }
                poll(fresh);
                fresh = false;
                synchronized (WAKE) {
                    if (pendingControl == 0) {
                        WAKE.wait(1000);
                    }
                }
            } catch (InterruptedException e) {
                return;
            } catch (Throwable e) {
                ArneStyles.LOGGER.warn("Spotify-Anzeige: {}", e.toString());
                song = null;
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException ie) {
                    return;
                }
            }
        }
    }

    /**
     * Play/Pause, naechster oder vorheriger Song (Tasten in der Steuerung). Wirkt auf die Session, die gerade
     * angezeigt wird. Laeuft auf dem Spotify-Thread, der Aufruf hier wartet nicht.
     */
    public static void control(int command) {
        if (!WINDOWS || mediaBroken || thread == null) {
            return;
        }
        pendingControl = command;
        synchronized (WAKE) {
            WAKE.notifyAll();
        }
    }

    /** Fragt zuerst die Windows-Medien-Schnittstelle, sonst den Fenstertitel. */
    private static void poll(boolean fresh) throws InterruptedException {
        if (!mediaBroken) {
            WinMedia.Info info;
            try {
                info = WinMedia.poll(!ConfigManager.get().spotifyAnyPlayer);
            } catch (InterruptedException e) {
                throw e;
            } catch (Throwable e) {
                mediaBroken = true;
                ArneStyles.LOGGER.info("Windows-Medien-Schnittstelle nicht verfuegbar, nutze Fenstertitel: {}", e.toString());
                info = null;
            }
            if (info != null && !info.title().isEmpty() && (info.status() == 4 || info.status() == 5)) {
                applyMedia(info);
                return;
            }
            if (!mediaBroken && !ConfigManager.get().spotifyAnyPlayer && findSpotifyTitle() == null) {
                song = null;
                return;
            }
        }
        pollWindowTitle(fresh);
    }

    private static void applyMedia(WinMedia.Info info) throws InterruptedException {
        Song old = song;
        String artist = info.artist().isEmpty() ? info.album() : info.artist();
        long pos = info.positionNow();
        long start = System.nanoTime() - pos * 1_000_000L;
        Song next = new Song(artist, info.title(), info.playing(), true, start, pos, info.durationMs());
        song = next;
        boolean changed = old == null || !old.key().equals(next.key());
        if (changed) {
            requestLyrics(next);
        } else if (ConfigManager.get().spotifyLyrics && lyrics == null && !lyricsLoading) {
            requestLyrics(next);
        }
        if (!next.key().equals(coverRequestedFor)) {
            // Neuer Song: Windows liefert das neue Cover oft erst nach einem Moment (direkt nach dem Wechsel
            // kommt nichts oder noch das alte). Deshalb erst beim naechsten Abfragen holen und ein paar Mal nachfassen.
            coverRequestedFor = next.key();
            coverTries = 0;
            coverLoadedKey = null;
            previousCoverHash = currentCoverHash;
            return;
        }
        if (next.key().equals(coverLoadedKey) || coverTries >= 8) {
            return;
        }
        coverTries++;
        byte[] bytes = null;
        try {
            bytes = WinMedia.thumbnail();
        } catch (InterruptedException e) {
            throw e;
        } catch (Throwable e) {
            ArneStyles.LOGGER.debug("Cover nicht lesbar: {}", e.toString());
        }
        if (bytes == null) {
            return;
        }
        int hash = java.util.Arrays.hashCode(bytes);
        if (hash == previousCoverHash && coverTries < 6) {
            // Noch das Cover vom vorigen Song: spaeter nochmal fragen
            return;
        }
        int[] argb = decodeCover(bytes);
        if (argb != null) {
            pendingCover = new Cover(next.key(), argb);
            coverLoadedKey = next.key();
            currentCoverHash = hash;
        }
    }

    /** Fallback: Titel des Spotify-Fensters ("Kuenstler - Song", pausiert nur "Spotify ..."). */
    private static void pollWindowTitle(boolean fresh) {
        String windowTitle = findSpotifyTitle();
        Song old = song;
        if (windowTitle == null) {
            song = null;
            return;
        }
        int dash = windowTitle.indexOf(" - ");
        if (dash < 0) {
            if (old != null && old.playing()) {
                song = new Song(old.artist(), old.title(), false, old.positionKnown(), old.startNanos(), old.positionMs(), old.durationMs());
            }
            return;
        }
        String artist = windowTitle.substring(0, dash).trim();
        String title = windowTitle.substring(dash + 3).trim();
        long now = System.nanoTime();
        if (old != null && old.artist().equals(artist) && old.title().equals(title)) {
            if (!old.playing()) {
                song = new Song(artist, title, true, old.positionKnown(), now - old.pausedMs() * 1_000_000L, 0, old.durationMs());
            }
            if (ConfigManager.get().spotifyLyrics && lyrics == null && !lyricsLoading) {
                requestLyrics(old);
            }
            return;
        }
        // Lief der Song schon beim Einschalten, ist die Stelle unbekannt. Im Mittel kommt der Wechsel 0,5 s spaet an.
        Song next = new Song(artist, title, true, !fresh, now - 500_000_000L, 0, 0);
        song = next;
        pendingCover = null;
        requestLyrics(next);
    }

    private static @Nullable String findSpotifyTitle() {
        long now = System.currentTimeMillis();
        if (now - pidsCheckedAt > 5000) {
            Set<Integer> pids = new HashSet<>();
            ProcessHandle.allProcesses().forEach(p -> {
                String cmd = p.info().command().orElse("");
                if (cmd.toLowerCase(Locale.ROOT).endsWith("\\spotify.exe")) {
                    pids.add((int) p.pid());
                }
            });
            spotifyPids = pids;
            pidsCheckedAt = now;
        }
        if (spotifyPids.isEmpty()) {
            return null;
        }
        String[] result = new String[1];
        char[] buf = new char[512];
        IntByReference pid = new IntByReference();
        User32.INSTANCE.EnumWindows((WinDef.HWND hwnd, com.sun.jna.Pointer data) -> {
            if (!User32.INSTANCE.IsWindowVisible(hwnd)) {
                return true;
            }
            User32.INSTANCE.GetWindowThreadProcessId(hwnd, pid);
            if (!spotifyPids.contains(pid.getValue())) {
                return true;
            }
            int len = User32.INSTANCE.GetWindowText(hwnd, buf, buf.length);
            if (len <= 0) {
                return true;
            }
            String t = new String(buf, 0, len);
            if (t.contains(" - ") || t.startsWith("Spotify")) {
                result[0] = t;
                return false;
            }
            return true;
        }, null);
        return result[0];
    }

    // ---- Cover ----

    /** PNG/JPEG mit stb_image dekodieren und auf COVER x COVER verkleinern (Mittelwert je Block). */
    private static int @Nullable [] decodeCover(byte[] bytes) {
        ByteBuffer input = MemoryUtil.memAlloc(bytes.length);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            input.put(bytes).flip();
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer c = stack.mallocInt(1);
            if (!STBImage.stbi_info_from_memory(input, w, h, c) || w.get(0) <= 0 || h.get(0) <= 0
                || w.get(0) > MAX_COVER_SIDE || h.get(0) > MAX_COVER_SIDE) {
                return null;
            }
            ByteBuffer pixels = STBImage.stbi_load_from_memory(input, w, h, c, 4);
            if (pixels == null) {
                return null;
            }
            try {
                int sw = w.get(0);
                int sh = h.get(0);
                int[] out = new int[COVER * COVER];
                for (int ty = 0; ty < COVER; ty++) {
                    int y0 = ty * sh / COVER;
                    int y1 = Math.max(y0 + 1, (ty + 1) * sh / COVER);
                    for (int tx = 0; tx < COVER; tx++) {
                        int x0 = tx * sw / COVER;
                        int x1 = Math.max(x0 + 1, (tx + 1) * sw / COVER);
                        long r = 0;
                        long g = 0;
                        long b = 0;
                        int n = 0;
                        for (int y = y0; y < y1; y++) {
                            for (int x = x0; x < x1; x++) {
                                int i = (y * sw + x) * 4;
                                r += pixels.get(i) & 0xFF;
                                g += pixels.get(i + 1) & 0xFF;
                                b += pixels.get(i + 2) & 0xFF;
                                n++;
                            }
                        }
                        out[ty * COVER + tx] = 0xFF000000 | (int) (r / n) << 16 | (int) (g / n) << 8 | (int) (b / n);
                    }
                }
                return out;
            } finally {
                STBImage.stbi_image_free(pixels);
            }
        } catch (RuntimeException | LinkageError e) {
            return null;
        } finally {
            MemoryUtil.memFree(input);
        }
    }

    /** Neues Cover auf dem Render-Thread als Textur hochladen. true, wenn das Cover zum Song passt. */
    private static boolean coverReady(@Nullable Song s) {
        Cover pending = pendingCover;
        if (pending != null && pending != uploadedCover) {
            NativeImage image = new NativeImage(COVER, COVER, false);
            for (int y = 0; y < COVER; y++) {
                for (int x = 0; x < COVER; x++) {
                    image.setColorArgb(x, y, pending.argb()[y * COVER + x]);
                }
            }
            MinecraftClient.getInstance().getTextureManager()
                .registerTexture(COVER_ID, new NativeImageBackedTexture(() -> "arnestyles spotify cover", image));
            uploadedCover = pending;
        }
        return s != null && uploadedCover != null && pending == uploadedCover && uploadedCover.key().equals(s.key());
    }

    private static void drawCover(DrawContext context, int x, int y, int size) {
        context.drawTexture(RenderPipelines.GUI_TEXTURED, COVER_ID, x, y, 0.0f, 0.0f, size, size, COVER, COVER, COVER, COVER);
    }

    /** Fuer die Live-Vorschau im Menue: zeichnet das echte Cover, falls eins geladen ist. */
    public static boolean drawCurrentCover(DrawContext context, int x, int y, int size) {
        Song s = song;
        if (!coverReady(s)) {
            return false;
        }
        drawCover(context, x, y, size);
        return true;
    }

    // ---- Lyrics (lrclib.net) ----

    private static void requestLyrics(Song s) {
        String key = s.key();
        lyricsKey = key;
        Lyrics cached;
        synchronized (CACHE) {
            cached = CACHE.get(key);
        }
        if (cached != null) {
            lyrics = cached;
            lyricsLoading = false;
            return;
        }
        lyrics = null;
        if (!ConfigManager.get().spotifyLyrics) {
            lyricsLoading = false;
            return;
        }
        lyricsLoading = true;
        Thread t = new Thread(() -> {
            Lyrics found = fetch(s.artist(), s.title(), s.durationMs());
            if (found == null && s.artist().contains(", ")) {
                found = fetch(s.artist().substring(0, s.artist().indexOf(", ")), s.title(), s.durationMs());
            }
            Lyrics result = found == null ? NONE : found;
            synchronized (CACHE) {
                CACHE.put(key, result);
            }
            if (key.equals(lyricsKey)) {
                lyrics = result;
                lyricsLoading = false;
            }
        }, "ArneTweaks-Lyrics");
        t.setDaemon(true);
        t.start();
    }

    private static @Nullable Lyrics fetch(String artist, String title, long durationMs) {
        try {
            if (http == null) {
                http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NORMAL).build();
            }
            String url = "https://lrclib.net/api/search?track_name=" + URLEncoder.encode(title, StandardCharsets.UTF_8)
                + "&artist_name=" + URLEncoder.encode(artist, StandardCharsets.UTF_8);
            String version = FabricLoader.getInstance().getModContainer(ArneStyles.MOD_ID)
                .map(m -> m.getMetadata().getVersion().getFriendlyString()).orElse("dev");
            HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(8))
                .header("User-Agent", "ArneTweaks/" + version).GET().build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200 || response.body().length() > 4_000_000) {
                return null;
            }
            return pick(JsonParser.parseString(response.body()), durationMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (Exception e) {
            ArneStyles.LOGGER.debug("Lyrics nicht geladen: {}", e.toString());
            return null;
        }
    }

    private static long durationOf(JsonObject o) {
        return o.has("duration") && !o.get("duration").isJsonNull() ? Math.round(o.get("duration").getAsDouble()) : 0;
    }

    /**
     * Von mehreren Treffern den mit Zeitstempeln nehmen: bei bekannter Songlaenge den mit der passendsten Laenge,
     * sonst die Laenge, die am haeufigsten vorkommt (normale Album-Version).
     */
    private static @Nullable Lyrics pick(JsonElement json, long knownMs) {
        if (!json.isJsonArray()) {
            return null;
        }
        JsonArray array = json.getAsJsonArray();
        Map<Long, Integer> votes = new HashMap<>();
        List<JsonObject> synced = new ArrayList<>();
        for (JsonElement e : array) {
            if (!e.isJsonObject()) {
                continue;
            }
            JsonObject o = e.getAsJsonObject();
            if (!o.has("syncedLyrics") || o.get("syncedLyrics").isJsonNull()) {
                continue;
            }
            synced.add(o);
            votes.merge(durationOf(o), 1, Integer::sum);
        }
        if (synced.isEmpty()) {
            return null;
        }
        if (knownMs > 0) {
            JsonObject best = null;
            long bestDiff = Long.MAX_VALUE;
            for (JsonObject o : synced) {
                long diff = Math.abs(durationOf(o) * 1000L - knownMs);
                if (diff < bestDiff) {
                    bestDiff = diff;
                    best = o;
                }
            }
            if (best != null && bestDiff <= 4000) {
                return parse(best.get("syncedLyrics").getAsString(), durationOf(best) * 1000L);
            }
        }
        long best = votes.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(0L);
        for (JsonObject o : synced) {
            if (durationOf(o) == best) {
                return parse(o.get("syncedLyrics").getAsString(), best * 1000L);
            }
        }
        return parse(synced.get(0).get("syncedLyrics").getAsString(), 0);
    }

    /** LRC-Text "[mm:ss.xx] Zeile" in sortierte Zeiten und Zeilen umwandeln. */
    static @Nullable Lyrics parse(String lrc, long durationMs) {
        List<long[]> order = new ArrayList<>();
        List<String> texts = new ArrayList<>();
        for (String raw : lrc.split("\\r?\\n")) {
            Matcher m = LRC_TAG.matcher(raw);
            List<Long> stamps = new ArrayList<>();
            int end = 0;
            while (m.find() && m.start() == end) {
                stamps.add(Long.parseLong(m.group(1)) * 60_000L + Math.round(Double.parseDouble(m.group(2)) * 1000.0));
                end = m.end();
            }
            if (stamps.isEmpty()) {
                continue;
            }
            String text = raw.substring(end).trim();
            if (text.length() > 200) {
                text = text.substring(0, 200);
            }
            for (long stamp : stamps) {
                order.add(new long[]{stamp, texts.size()});
                texts.add(text.isEmpty() ? "♪" : text);
            }
        }
        if (order.isEmpty()) {
            return null;
        }
        order.sort((a, b) -> Long.compare(a[0], b[0]));
        long[] times = new long[order.size()];
        String[] lines = new String[order.size()];
        for (int i = 0; i < order.size(); i++) {
            times[i] = order.get(i)[0];
            lines[i] = texts.get((int) order.get(i)[1]);
        }
        return new Lyrics(times, lines, durationMs);
    }

    // ---- Anzeige ----

    public static int mode() {
        return MathHelper.clamp(Math.round(ConfigManager.get().spotifyMode), 0, MODES.length - 1);
    }

    public static String modeName(int mode) {
        return Text.translatable("arnestyles.spotify.mode." + MODES[MathHelper.clamp(mode, 0, MODES.length - 1)]).getString();
    }

    private static int cardWidth() {
        return MathHelper.clamp(Math.round(ConfigManager.get().spotifyWidth), 150, 320);
    }

    private static long durationOf(Song s, @Nullable Lyrics l) {
        if (s.durationMs() > 0) {
            return s.durationMs();
        }
        return l != null && l != NONE ? l.durationMs() : 0;
    }

    private static String clock(long ms) {
        long sec = Math.max(0, ms / 1000L);
        return (sec / 60) + ":" + (sec % 60 < 10 ? "0" : "") + (sec % 60);
    }

    public static void render(DrawContext context, MinecraftClient client, int x, int y) {
        if (!enabled() && !HudLayout.editing) {
            return;
        }
        Song s = song;
        if (s == null && HudLayout.editing) {
            s = new Song("Artist", "Song", true, true, System.nanoTime() - 42_000_000_000L, 0, 180_000L);
        }
        if (s == null) {
            return;
        }
        ArneStylesConfig config = ConfigManager.get();
        if (!s.playing() && config.spotifyHidePaused && !HudLayout.editing) {
            return;
        }
        Lyrics l = s == song ? lyrics : null;
        long pos = s.positionMs() + Math.round(config.spotifyOffset * 1000.0f);
        int line = l != null && l != NONE && s.positionKnown() ? l.lineAt(pos) : -1;
        int mode = mode();
        boolean cover = config.spotifyCover && mode != 3 && coverReady(s);
        TextRenderer font = client.textRenderer;
        int width = mode == 0 ? 0 : cardWidth();
        updateText(s, l, line, mode, cover, width, font);

        String[] t = drawnText;
        int lines = t.length;
        boolean card = mode == 4;
        long duration = durationOf(s, l);
        boolean bar = card && duration > 0 && s.positionKnown();
        int coverSize = !cover ? 0 : card ? 38 : mode == 0 ? 9 : 20;
        int textX = coverSize > 0 ? coverSize + (card ? 9 : 6) : (card ? 7 : 4);
        int w = mode == 0 ? textX + font.getWidth(t[0]) + 4 : width;
        int textH = 4 + lines * 11;
        int h = Math.max(textH, coverSize + (card ? 8 : 4)) + (bar ? 11 : 0) + (card ? 2 : 0);
        HudLayout.begin(context, HudLayout.Element.SPOTIFY, x, y, w, h);
        try {
            int alpha = Math.round(255 * MathHelper.clamp(config.spotifyBgOpacity, 0.0f, 1.0f));
            context.fill(x, y, x + w, y + h, (alpha << 24) | (card ? CARD_BG & 0xFFFFFF : 0));
            int accent = accent();
            if (card) {
                context.fill(x, y, x + 2, y + h, accent);
            }
            if (coverSize > 0) {
                int cy = card ? y + 4 : y + (mode == 0 ? 2 : 2);
                drawCover(context, x + (card ? 5 : 2), cy, coverSize);
            }
            int ty = y + (card ? 5 : 3);
            for (int i = 0; i < lines; i++) {
                context.drawText(font, t[i], x + textX, ty + i * 11, colorOf(mode, i, lines, accent), config.spotifyShadow);
            }
            if (bar) {
                long now = Math.min(duration, Math.max(0, s.positionMs()));
                int second = (int) (now / 1000L);
                if (second != timeShownSecond || drawnSong != s) {
                    timeShownSecond = second;
                    timeText = clock(now) + " / " + clock(duration);
                }
                int by = y + h - 9;
                int tw = font.getWidth(timeText);
                int bx1 = x + 7;
                int bx2 = x + w - 10 - tw;
                float f = MathHelper.clamp(now / (float) duration, 0.0f, 1.0f);
                context.fill(bx1, by + 2, bx2, by + 4, 0x40FFFFFF);
                context.fill(bx1, by + 2, bx1 + Math.round((bx2 - bx1) * f), by + 4, accent);
                context.drawText(font, timeText, x + w - 5 - tw, by - 1, GREY, config.spotifyShadow);
            }
        } finally {
            HudLayout.end(context);
        }
    }

    /** Akzentfarbe (Balken, Lyrics, Kartenrand), einstellbar, Standard Spotify-Gruen. */
    public static int accent() {
        return 0xFF000000 | (ConfigManager.get().spotifyAccentRgb & 0xFFFFFF);
    }

    private static int colorOf(int mode, int i, int lines, int accent) {
        return switch (mode) {
            case 0 -> 0xFFFFFFFF;
            case 1 -> i == 0 ? 0xFFFFFFFF : GREY;
            case 2 -> i == 0 ? 0xFFFFFFFF : accent;
            case 3 -> lines == 3 ? (i == 1 ? accent : DIM) : (i == 0 ? 0xFFFFFFFF : GREY);
            default -> i == 0 ? 0xFFFFFFFF : i == 1 ? GREY : accent;
        };
    }

    /** Baut die Zeilen nur neu, wenn sich Song, Lyrics-Zeile, Pause, Modus, Breite oder Cover geaendert haben. */
    private static void updateText(Song s, @Nullable Lyrics l, int line, int mode, boolean cover, int width, TextRenderer font) {
        int stamp = ((line * 8 + mode) * 2 + (cover ? 1 : 0)) * 31 + width + (l == null ? 0 : 50000) + (lyricsLoading ? 90000 : 0);
        if (s == drawnSong && stamp == drawnStamp) {
            return;
        }
        drawnSong = s;
        drawnStamp = stamp;
        int coverSpace = !cover ? 0 : mode == 4 ? 47 : mode == 0 ? 15 : 26;
        int max = Math.max(40, width - (coverSpace > 0 ? coverSpace : 11) - 6);
        String pause = s.playing() ? "" : "❚❚ ";
        String title = font.trimToWidth(pause + s.title(), max);
        String artist = font.trimToWidth(s.artist(), max);
        String current = lyricLine(s, l, line);
        List<String> out = new ArrayList<>(4);
        switch (mode) {
            case 0 -> out.add((cover ? "" : "♪ ") + font.trimToWidth(pause + s.title() + " – " + s.artist(), 260));
            case 1 -> {
                out.add(title);
                out.add(artist);
            }
            case 2 -> {
                out.add(font.trimToWidth(pause + s.title() + " – " + s.artist(), max));
                out.add(current != null ? font.trimToWidth(current, max) : artist);
            }
            case 3 -> {
                if (l != null && l != NONE && line >= 0 && s.positionKnown()) {
                    out.add(font.trimToWidth(line > 0 ? l.lines()[line - 1] : "", max));
                    out.add(font.trimToWidth(l.lines()[line], max));
                    out.add(font.trimToWidth(line + 1 < l.lines().length ? l.lines()[line + 1] : "", max));
                } else {
                    out.add(title);
                    out.add(current != null ? font.trimToWidth(current, max) : artist);
                }
            }
            default -> {
                out.add(title);
                out.add(artist);
                if (current != null) {
                    out.add(font.trimToWidth(current, max));
                }
            }
        }
        drawnText = out.toArray(new String[0]);
    }

    /** Aktuelle Lyrics-Zeile oder ein Hinweis, warum keine da ist (null = nichts anzeigen). */
    private static @Nullable String lyricLine(Song s, @Nullable Lyrics l, int line) {
        if (HudLayout.editing && song == null) {
            return "♪ Lyrics";
        }
        if (!ConfigManager.get().spotifyLyrics) {
            return null;
        }
        if (lyricsLoading && l == null) {
            return Text.translatable("arnestyles.spotify.loading").getString();
        }
        if (l == null || l == NONE) {
            return Text.translatable("arnestyles.spotify.no_lyrics").getString();
        }
        if (!s.positionKnown()) {
            return Text.translatable("arnestyles.spotify.next_song").getString();
        }
        return line >= 0 ? l.lines()[line] : "♪";
    }
}
