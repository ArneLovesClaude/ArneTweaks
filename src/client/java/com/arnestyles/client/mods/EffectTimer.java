package com.arnestyles.client.mods;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.effect.StatusEffectInstance;

import java.util.Locale;

/**
 * Status Effect Timer: Restzeit (unten) und Stufe (oben rechts) direkt auf den Effekt-Symbolen oben rechts.
 * Fuer ArneTweaks selbst geschrieben.
 */
public final class EffectTimer {
    private static final String[] ROMAN = {"I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};

    private EffectTimer() {
    }

    /** x und y sind die obere linke Ecke des 24x24-Feldes. */
    public static void draw(DrawContext context, StatusEffectInstance effect, int x, int y) {
        if (!ModSetting.Toggle.EFFECT_TIMER.isActive()) {
            return;
        }
        TextRenderer font = MinecraftClient.getInstance().textRenderer;
        context.drawCenteredTextWithShadow(font, duration(effect), x + 12, y + 14, 0xFFFFFFFF);
        int amplifier = effect.getAmplifier();
        if (amplifier > 0 && ModSetting.Toggle.EFFECT_TIMER_LEVEL.isActive()) {
            String level = amplifier < ROMAN.length ? ROMAN[amplifier] : String.valueOf(amplifier + 1);
            context.drawTextWithShadow(font, level, x + 22 - font.getWidth(level), y + 2, 0xFFFFFFFF);
        }
    }

    private static String duration(StatusEffectInstance effect) {
        if (effect.isInfinite()) {
            return "∞";
        }
        int seconds = Math.max(0, effect.getDuration() / 20);
        if (seconds >= 3600) {
            return seconds / 3600 + "h";
        }
        if (seconds >= 60) {
            return String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
        }
        return seconds + "s";
    }
}
