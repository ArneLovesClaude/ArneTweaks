package com.arnestyles.client.mods;

import com.arnestyles.client.config.ArneStylesConfig;
import com.arnestyles.client.config.ConfigManager;
import net.minecraft.util.math.MathHelper;

import java.util.Locale;

/** Optik (Version 2.8): Block-Umrandung, eigene Zeit, eigene Himmelsfarbe und Item-Physik. Alles nur bei dir sichtbar. */
public final class WorldVisuals {
    /** Farben zur Auswahl fuer den Himmel. */
    public static final int[] SKY_COLORS = {
        0x78A7FF, 0xFF9A5C, 0xFF6EC7, 0x9B6BFF, 0x2B2F77, 0x7CF5C8, 0xFFD27A, 0xFFFFFF, 0x111111
    };

    private WorldVisuals() {
    }

    private static ArneStylesConfig config() {
        return ConfigManager.get();
    }

    // ---- Block-Umrandung ----

    public static boolean outlineActive() {
        return ModSetting.Toggle.BLOCK_OUTLINE.isActive();
    }

    /** Farbe der Umrandung als ARGB (Regenbogen dreht den Farbton langsam). */
    public static int outlineColor() {
        int rgb = config().outlineColorRgb & 0xFFFFFF;
        if (ModSetting.Toggle.OUTLINE_RAINBOW.value()) {
            float hue = (System.currentTimeMillis() % 4000L) / 4000.0f;
            rgb = MathHelper.hsvToRgb(hue, 0.8f, 1.0f) & 0xFFFFFF;
        }
        return 0xFF000000 | rgb;
    }

    public static float outlineWidth(float vanilla) {
        return vanilla * config().outlineWidth;
    }

    // ---- Zeit und Himmel ----

    public static boolean customTime() {
        return ModSetting.Toggle.CUSTOM_TIME.isActive();
    }

    public static long time() {
        return Math.round(config().customTime);
    }

    /** Minecraft-Zeit als Uhrzeit: 0 = 06:00, 6000 = 12:00, 18000 = 00:00. */
    public static String clock(float ticks) {
        int minutes = Math.round(ticks / 1000.0f * 60.0f) + 6 * 60;
        minutes = Math.floorMod(minutes, 24 * 60);
        return String.format(Locale.ROOT, "%02d:%02d", minutes / 60, minutes % 60);
    }

    public static boolean customSky() {
        return ModSetting.Toggle.SKY_COLOR.isActive();
    }

    public static int skyColor() {
        return 0xFF000000 | (config().skyColorRgb & 0xFFFFFF);
    }

    // ---- Item-Physik ----

    public static boolean itemPhysics() {
        return ModSetting.Toggle.ITEM_PHYSICS.isActive();
    }
}
