package com.arnestyles.client.mods;

import com.arnestyles.client.ArneKeybinds;
import com.arnestyles.client.config.ConfigManager;
import com.arnestyles.client.config.PresetManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Komfort (Version 2.9): Zoom (Verhalten nach Zoomify von isXander, LGPL-3.0, Code komplett neu geschrieben),
 * Freelook, Auto-GG und Preset-Schnellwechsel.
 */
public final class Comfort {
    public static final float MIN_ZOOM = 1.5f;
    public static final float MAX_ZOOM = 50.0f;
    private static final long AUTO_GG_COOLDOWN_MS = 5000L;
    private static final int AUTO_GG_DELAY_TICKS = 15;

    // ---- Zoom ----
    private static boolean zooming;
    /** Zoom-Stufe, solange die Taste gehalten wird (Mausrad aendert sie, beim Loslassen zurueck auf die Einstellung). */
    private static float scrollZoom;
    private static float currentZoom = 1.0f;
    private static long lastFrame;

    // ---- Freelook ----
    private static boolean freelook;
    private static float freeYaw;
    private static float freePitch;
    private static @Nullable Perspective perspectiveBefore;

    // ---- Auto-GG ----
    private static int ggTicks = -1;
    private static long lastGg;

    private Comfort() {
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(Comfort::tick);
    }

    private static void tick(MinecraftClient client) {
        boolean wantZoom = client.player != null && client.currentScreen == null && ModSetting.Toggle.ZOOM.isActive()
            && ArneKeybinds.ZOOM.isPressed();
        if (wantZoom && !zooming) {
            scrollZoom = ConfigManager.get().zoomLevel;
        }
        zooming = wantZoom;

        boolean wantFree = client.player != null && ModSetting.Toggle.FREELOOK.isActive() && ArneKeybinds.FREELOOK.isPressed();
        if (wantFree && !freelook) {
            freeYaw = client.player.getYaw();
            freePitch = client.player.getPitch();
            if (ModSetting.Toggle.FREELOOK_THIRD_PERSON.value() && client.options.getPerspective().isFirstPerson()) {
                perspectiveBefore = client.options.getPerspective();
                client.options.setPerspective(Perspective.THIRD_PERSON_BACK);
            }
        } else if (!wantFree && freelook && perspectiveBefore != null) {
            client.options.setPerspective(perspectiveBefore);
            perspectiveBefore = null;
        }
        freelook = wantFree;

        while (ArneKeybinds.NEXT_PRESET.wasPressed()) {
            nextPreset(client);
        }

        if (ggTicks >= 0 && --ggTicks < 0 && client.player != null) {
            client.player.networkHandler.sendChatMessage(ConfigManager.get().autoGgMessage);
        }
    }

    // ---- Zoom ----

    /** Teilt das Sichtfeld durch den aktuellen Zoom (weich ein- und ausgeblendet). */
    public static float applyZoom(float fov) {
        long now = System.nanoTime();
        float dt = lastFrame == 0 ? 0.0f : Math.min(0.1f, (now - lastFrame) / 1.0e9f);
        lastFrame = now;
        float target = zooming ? scrollZoom : 1.0f;
        if (ModSetting.Toggle.ZOOM_SMOOTH.value()) {
            currentZoom += (target - currentZoom) * (1.0f - (float) Math.exp(-dt * 14.0f));
            if (Math.abs(currentZoom - target) < 0.001f) {
                currentZoom = target;
            }
        } else {
            currentZoom = target;
        }
        return currentZoom <= 1.0001f ? fov : fov / currentZoom;
    }

    /** Mausrad beim Zoomen: naeher ran oder weiter weg. true = Mausrad verbraucht. */
    public static boolean onScroll(double vertical) {
        if (!zooming || !ModSetting.Toggle.ZOOM_SCROLL.value() || vertical == 0.0) {
            return false;
        }
        float factor = vertical > 0 ? 1.25f : 0.8f;
        scrollZoom = MathHelper.clamp(scrollZoom * factor, MIN_ZOOM, MAX_ZOOM);
        return true;
    }

    /** Beim Zoomen wird die Maus langsamer, damit man genau zielen kann. */
    public static double mouseFactor() {
        return currentZoom > 1.0001f && ModSetting.Toggle.ZOOM_SLOW_MOUSE.value() ? 1.0 / currentZoom : 1.0;
    }

    public static boolean zooming() {
        return zooming;
    }

    // ---- Freelook ----

    public static boolean freelook() {
        return freelook;
    }

    /** Mausbewegung beim Freelook dreht nur die Kamera (gleiche Empfindlichkeit wie normal). */
    public static void freelookTurn(double dx, double dy) {
        freeYaw += (float) dx * 0.15f;
        freePitch = MathHelper.clamp(freePitch + (float) dy * 0.15f, -90.0f, 90.0f);
    }

    public static float freeYaw() {
        return freeYaw;
    }

    public static float freePitch() {
        return freePitch;
    }

    // ---- Auto-GG ----

    /** Nach einem Kill an einem Spieler (aus PvpVisuals). */
    public static void onPlayerKill() {
        long now = System.currentTimeMillis();
        if (ModSetting.Toggle.AUTO_GG.isActive() && now - lastGg > AUTO_GG_COOLDOWN_MS) {
            lastGg = now;
            ggTicks = AUTO_GG_DELAY_TICKS;
        }
    }

    // ---- Presets ----

    /** Naechstes Preset laden (Taste), Texturen neu laden und den Namen ueber der Hotbar zeigen. */
    private static void nextPreset(MinecraftClient client) {
        List<PresetManager.Preset> presets = PresetManager.list();
        if (presets.isEmpty()) {
            client.inGameHud.setOverlayMessage(Text.translatable("arnestyles.preset.none"), false);
            return;
        }
        String last = ConfigManager.get().lastPreset;
        int index = 0;
        for (int i = 0; i < presets.size(); i++) {
            if (presets.get(i).name().equals(last)) {
                index = (i + 1) % presets.size();
                break;
            }
        }
        PresetManager.Preset preset = presets.get(index);
        if (PresetManager.load(preset)) {
            ConfigManager.get().lastPreset = preset.name();
            ConfigManager.save();
            client.reloadResources();
            client.inGameHud.setOverlayMessage(Text.translatable("arnestyles.preset.switched", preset.name()), false);
        }
    }
}
