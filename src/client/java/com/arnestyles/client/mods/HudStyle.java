package com.arnestyles.client.mods;

import com.arnestyles.client.config.ArneStylesConfig;
import com.arnestyles.client.config.ConfigManager;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

/**
 * Gemeinsames Aussehen aller Info-Anzeigen (Seite Mods, Abschnitt "Aussehen der Anzeigen"):
 * Textfarbe, Hintergrundfarbe und -deckkraft, Text-Schatten, Rahmen und Akzentfarbe.
 */
public final class HudStyle {
    private HudStyle() {
    }

    /** Hintergrundfarbe mit eingestellter Deckkraft (ARGB). */
    public static int bg() {
        ArneStylesConfig c = ConfigManager.get();
        int alpha = Math.round(255 * Math.max(0.0f, Math.min(1.0f, c.hudBgOpacity)));
        return (alpha << 24) | (c.hudBgRgb & 0xFFFFFF);
    }

    /** Textfarbe (ARGB). */
    public static int text() {
        return 0xFF000000 | (ConfigManager.get().hudTextRgb & 0xFFFFFF);
    }

    /** Akzentfarbe (ARGB): gedrueckte Tasten, Balken. */
    public static int accent() {
        return 0xFF000000 | (ConfigManager.get().hudAccentRgb & 0xFFFFFF);
    }

    public static boolean shadow() {
        return ConfigManager.get().hudTextShadow;
    }

    /** Hintergrund (und optional Rahmen) einer Anzeige. */
    public static void background(DrawContext context, int x1, int y1, int x2, int y2) {
        background(context, x1, y1, x2, y2, bg());
    }

    public static void background(DrawContext context, int x1, int y1, int x2, int y2, int color) {
        if ((color >>> 24) != 0) {
            context.fill(x1, y1, x2, y2, color);
        }
        ArneStylesConfig c = ConfigManager.get();
        if (c.hudBorder) {
            int border = 0xFF000000 | (c.hudBorderRgb & 0xFFFFFF);
            context.fill(x1, y1, x2, y1 + 1, border);
            context.fill(x1, y2 - 1, x2, y2, border);
            context.fill(x1, y1 + 1, x1 + 1, y2 - 1, border);
            context.fill(x2 - 1, y1 + 1, x2, y2 - 1, border);
        }
    }

    public static void text(DrawContext context, TextRenderer font, String text, int x, int y, int color) {
        context.drawText(font, text, x, y, color, shadow());
    }
}
