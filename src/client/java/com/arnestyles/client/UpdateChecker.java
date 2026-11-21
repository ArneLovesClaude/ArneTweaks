package com.arnestyles.client;

import com.arnestyles.ArneStyles;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Update-Hinweis: fragt beim Start einmal das neueste Release auf GitHub ab.
 * Ist es neuer als die laufende Version, kommt im Hauptmenue ein Toast und beim Betreten
 * einer Welt eine Chat-Nachricht mit Link zur Download-Seite. Installiert wird von Hand (oder ueber den Launcher).
 */
public final class UpdateChecker {
    private static final String API = "https://api.github.com/repos/ArneLovesClaude/ArneTweaks/releases/latest";
    public static final String RELEASES = "https://github.com/ArneLovesClaude/ArneTweaks/releases/latest";

    private static volatile @Nullable String newer;
    private static boolean toastShown;
    private static boolean chatShown;

    private UpdateChecker() {
    }

    public static void register() {
        String current = currentVersion();
        CompletableFuture.runAsync(() -> check(current));
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            String version = newer;
            if (version != null && !toastShown && screen instanceof TitleScreen) {
                toastShown = true;
                SystemToast.add(client.getToastManager(), SystemToast.Type.PERIODIC_NOTIFICATION,
                    Text.translatable("arnestyles.update.title", version),
                    Text.translatable("arnestyles.update.toast"));
            }
        });
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            String version = newer;
            if (version == null || chatShown) {
                return;
            }
            chatShown = true;
            Text link = Text.translatable("arnestyles.update.link").styled(style -> style
                .withColor(Formatting.AQUA).withUnderline(true)
                .withClickEvent(new ClickEvent.OpenUrl(URI.create(RELEASES))));
            Text message = Text.translatable("arnestyles.update.chat", version, currentVersion()).formatted(Formatting.GOLD)
                .append(" ").append(link);
            client.execute(() -> client.inGameHud.getChatHud().addMessage(message));
        });
    }

    private static String currentVersion() {
        return FabricLoader.getInstance().getModContainer(ArneStyles.MOD_ID)
            .map(mod -> mod.getMetadata().getVersion().getFriendlyString()).orElse("0");
    }

    private static void check(String current) {
        try {
            HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(6)).build();
            HttpRequest request = HttpRequest.newBuilder(URI.create(API))
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "ArneTweaks")
                .GET().build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                ArneStyles.LOGGER.info("Update-Pruefung: GitHub antwortet mit {}", response.statusCode());
                return;
            }
            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            String latest = json.get("tag_name").getAsString().replaceFirst("^[vV]", "");
            // Nur eine echte Versionsnummer anzeigen (kein beliebiger Text im Chat)
            if (!latest.matches("[0-9]+([.][0-9]+){0,3}")) {
                return;
            }
            if (isNewer(latest, current)) {
                newer = latest;
                ArneStyles.LOGGER.info("Neue ArneTweaks-Version verfuegbar: {} (installiert: {})", latest, current);
            }
        } catch (Exception e) {
            ArneStyles.LOGGER.info("Update-Pruefung nicht moeglich: {}", e.toString());
        }
    }

    /** Vergleicht Versionen wie 3.0.0 und 3.1.2 Zahl fuer Zahl. Alles nach "+" oder "-" wird ignoriert. */
    static boolean isNewer(String latest, String current) {
        String[] a = latest.split("[+-]")[0].split("\\.");
        String[] b = current.split("[+-]")[0].split("\\.");
        for (int i = 0; i < Math.max(a.length, b.length); i++) {
            int x = i < a.length ? parse(a[i]) : 0;
            int y = i < b.length ? parse(b[i]) : 0;
            if (x != y) {
                return x > y;
            }
        }
        return false;
    }

    private static int parse(String part) {
        try {
            return Integer.parseInt(part.replaceAll("\\D", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
